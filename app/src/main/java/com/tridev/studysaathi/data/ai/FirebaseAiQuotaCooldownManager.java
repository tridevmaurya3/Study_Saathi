package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Firebase AI / Gemini quota और rate-limit failures के लिए
 * persistent cooldown manager।
 *
 * मुख्य उद्देश्य:
 *
 * 1. QuotaExceededException और HTTP 429 failures पहचानना।
 * 2. Firebase error में दिए गए "Please retry in 39.6s"
 *    जैसे retry समय को निकालना।
 * 3. Cooldown को SharedPreferences में सुरक्षित रखना।
 * 4. Activity restart होने पर भी cooldown बनाए रखना।
 * 5. Cooldown समाप्त होने से पहले repeated AI requests रोकना।
 * 6. Student को सही remaining-time message देना।
 *
 * यह class quota को बढ़ाती या हटाती नहीं है।
 * यह quota समाप्त होने पर app को controlled और user-friendly
 * तरीके से व्यवहार करने में सहायता करती है।
 */
public final class FirebaseAiQuotaCooldownManager {

    private static final String PREFERENCES_NAME =
            "study_saathi_firebase_ai_quota";

    private static final String KEY_COOLDOWN_UNTIL_EPOCH_MILLIS =
            "cooldown_until_epoch_millis";

    /**
     * Firebase error में retry duration न मिले तो
     * default cooldown।
     */
    private static final long DEFAULT_RETRY_AFTER_SECONDS =
            60L;

    /**
     * किसी malformed error के कारण बहुत लंबा cooldown
     * save न हो, इसलिए अधिकतम एक घंटे की सीमा।
     */
    private static final long MAXIMUM_RETRY_AFTER_SECONDS =
            60L * 60L;

    /**
     * Firebase error का उदाहरण:
     *
     * Please retry in 39.669385135s.
     */
    @NonNull
    private static final Pattern RETRY_IN_SECONDS_PATTERN =
            Pattern.compile(
                    "retry\\s+in\\s+"
                            + "([0-9]+(?:\\.[0-9]+)?)"
                            + "\\s*"
                            + "(?:s|sec|secs|second|seconds)",
                    Pattern.CASE_INSENSITIVE
            );

    /**
     * वैकल्पिक error format:
     *
     * Retry-After: 40
     * retry after 40 seconds
     */
    @NonNull
    private static final Pattern RETRY_AFTER_SECONDS_PATTERN =
            Pattern.compile(
                    "retry(?:-|\\s*)after"
                            + "\\s*[:=]?\\s*"
                            + "([0-9]+(?:\\.[0-9]+)?)"
                            + "\\s*"
                            + "(?:s|sec|secs|second|seconds)?",
                    Pattern.CASE_INSENSITIVE
            );

    @NonNull
    private final SharedPreferences preferences;

    public FirebaseAiQuotaCooldownManager(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        preferences =
                applicationContext.getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE
                );

        clearExpiredCooldown();
    }

    /**
     * दिए गए Firebase AI failure को जाँचता है।
     *
     * यदि यह quota/rate-limit failure है तो cooldown save करता है
     * और remaining seconds लौटाता है।
     *
     * यदि यह quota failure नहीं है तो 0 लौटाता है।
     */
    public synchronized long registerFailure(
            @Nullable Throwable throwable
    ) {
        String completeFailureText =
                collectThrowableDetails(
                        throwable
                );

        if (!isQuotaFailureText(
                completeFailureText
        )) {
            return 0L;
        }

        long retryAfterSeconds =
                extractRetryAfterSeconds(
                        completeFailureText
                );

        if (retryAfterSeconds <= 0L) {
            retryAfterSeconds =
                    DEFAULT_RETRY_AFTER_SECONDS;
        }

        retryAfterSeconds =
                Math.min(
                        retryAfterSeconds,
                        MAXIMUM_RETRY_AFTER_SECONDS
                );

        long currentTimeMillis =
                System.currentTimeMillis();

        long requestedCooldownUntil =
                currentTimeMillis
                        + retryAfterSeconds * 1000L;

        long existingCooldownUntil =
                preferences.getLong(
                        KEY_COOLDOWN_UNTIL_EPOCH_MILLIS,
                        0L
                );

        long effectiveCooldownUntil =
                Math.max(
                        requestedCooldownUntil,
                        existingCooldownUntil
                );

        preferences.edit()
                .putLong(
                        KEY_COOLDOWN_UNTIL_EPOCH_MILLIS,
                        effectiveCooldownUntil
                )
                .apply();

        return calculateRemainingSeconds(
                effectiveCooldownUntil,
                currentTimeMillis
        );
    }

    /**
     * वर्तमान समय में Firebase AI cooldown active है या नहीं।
     */
    public synchronized boolean isCooldownActive() {
        long remainingSeconds =
                getRemainingSeconds();

        return remainingSeconds > 0L;
    }

    /**
     * Cooldown में शेष seconds देता है।
     *
     * Cooldown समाप्त होने पर 0 मिलता है।
     */
    public synchronized long getRemainingSeconds() {
        long currentTimeMillis =
                System.currentTimeMillis();

        long cooldownUntilMillis =
                preferences.getLong(
                        KEY_COOLDOWN_UNTIL_EPOCH_MILLIS,
                        0L
                );

        long remainingSeconds =
                calculateRemainingSeconds(
                        cooldownUntilMillis,
                        currentTimeMillis
                );

        if (remainingSeconds <= 0L
                && cooldownUntilMillis > 0L) {

            clearCooldown();
            return 0L;
        }

        return remainingSeconds;
    }

    /**
     * Cooldown समाप्त होने का epoch timestamp देता है।
     */
    public synchronized long getCooldownUntilEpochMillis() {
        clearExpiredCooldown();

        return preferences.getLong(
                KEY_COOLDOWN_UNTIL_EPOCH_MILLIS,
                0L
        );
    }

    /**
     * Student को दिखाने के लिए readable cooldown message।
     */
    @NonNull
    public synchronized String createCooldownMessage() {
        long remainingSeconds =
                getRemainingSeconds();

        if (remainingSeconds <= 0L) {
            return "अब Smart AI से दोबारा प्रश्न पूछा जा सकता है।";
        }

        if (remainingSeconds < 60L) {
            return "Firebase AI की request limit अभी पूरी है। "
                    + remainingSeconds
                    + " सेकंड बाद दोबारा प्रयास करें।";
        }

        long minutes =
                remainingSeconds / 60L;

        long seconds =
                remainingSeconds % 60L;

        if (seconds == 0L) {
            return "Firebase AI की request limit अभी पूरी है। "
                    + minutes
                    + " मिनट बाद दोबारा प्रयास करें।";
        }

        return "Firebase AI की request limit अभी पूरी है। "
                + minutes
                + " मिनट "
                + seconds
                + " सेकंड बाद दोबारा प्रयास करें।";
    }

    /**
     * Error quota/rate-limit से संबंधित है या नहीं।
     */
    public boolean isQuotaFailure(
            @Nullable Throwable throwable
    ) {
        return isQuotaFailureText(
                collectThrowableDetails(
                        throwable
                )
        );
    }

    /**
     * समाप्त हो चुका cooldown preferences से हटाता है।
     */
    public synchronized void clearExpiredCooldown() {
        long currentTimeMillis =
                System.currentTimeMillis();

        long cooldownUntilMillis =
                preferences.getLong(
                        KEY_COOLDOWN_UNTIL_EPOCH_MILLIS,
                        0L
                );

        if (cooldownUntilMillis <= currentTimeMillis) {
            clearCooldown();
        }
    }

    /**
     * Saved cooldown को तुरंत हटाता है।
     *
     * इसका उपयोग सामान्य app flow में नहीं करना चाहिए।
     * यह testing या cooldown expiry handling के लिए उपलब्ध है।
     */
    public synchronized void clearCooldown() {
        preferences.edit()
                .remove(
                        KEY_COOLDOWN_UNTIL_EPOCH_MILLIS
                )
                .apply();
    }

    private long extractRetryAfterSeconds(
            @NonNull String completeFailureText
    ) {
        long retryInSeconds =
                extractSecondsUsingPattern(
                        completeFailureText,
                        RETRY_IN_SECONDS_PATTERN
                );

        if (retryInSeconds > 0L) {
            return retryInSeconds;
        }

        return extractSecondsUsingPattern(
                completeFailureText,
                RETRY_AFTER_SECONDS_PATTERN
        );
    }

    private long extractSecondsUsingPattern(
            @NonNull String completeFailureText,
            @NonNull Pattern pattern
    ) {
        Matcher matcher =
                pattern.matcher(
                        completeFailureText
                );

        if (!matcher.find()) {
            return 0L;
        }

        String numericValue =
                matcher.group(1);

        if (numericValue == null
                || numericValue.trim().isEmpty()) {

            return 0L;
        }

        try {
            double seconds =
                    Double.parseDouble(
                            numericValue.trim()
                    );

            if (seconds <= 0.0d
                    || Double.isNaN(seconds)
                    || Double.isInfinite(seconds)) {

                return 0L;
            }

            return Math.max(
                    1L,
                    (long) Math.ceil(
                            seconds
                    )
            );

        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private boolean isQuotaFailureText(
            @NonNull String completeFailureText
    ) {
        String normalizedText =
                completeFailureText.toLowerCase(
                        Locale.ROOT
                );

        return normalizedText.contains(
                "quotaexceededexception"
        )
                || normalizedText.contains(
                "quota exceeded"
        )
                || normalizedText.contains(
                "exceeded your current quota"
        )
                || normalizedText.contains(
                "generate_content_free_tier_requests"
        )
                || normalizedText.contains(
                "resource_exhausted"
        )
                || normalizedText.contains(
                "rate limit"
        )
                || normalizedText.contains(
                "too many requests"
        )
                || normalizedText.contains(
                "http 429"
        )
                || normalizedText.contains(
                "status code 429"
        )
                || normalizedText.contains(
                " 429 "
        );
    }

    @NonNull
    private String collectThrowableDetails(
            @Nullable Throwable throwable
    ) {
        if (throwable == null) {
            return "";
        }

        StringBuilder detailsBuilder =
                new StringBuilder();

        Throwable currentThrowable =
                throwable;

        int depth =
                0;

        while (currentThrowable != null
                && depth < 10) {

            appendThrowableClassName(
                    detailsBuilder,
                    currentThrowable
            );

            appendThrowableMessage(
                    detailsBuilder,
                    currentThrowable
            );

            currentThrowable =
                    currentThrowable.getCause();

            depth++;
        }

        return detailsBuilder.toString();
    }

    private void appendThrowableClassName(
            @NonNull StringBuilder detailsBuilder,
            @NonNull Throwable throwable
    ) {
        String className =
                throwable.getClass()
                        .getName();

        if (className == null
                || className.trim().isEmpty()) {

            return;
        }

        appendDetailPart(
                detailsBuilder,
                className
        );
    }

    private void appendThrowableMessage(
            @NonNull StringBuilder detailsBuilder,
            @NonNull Throwable throwable
    ) {
        String message =
                throwable.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return;
        }

        appendDetailPart(
                detailsBuilder,
                message.trim()
        );
    }

    private void appendDetailPart(
            @NonNull StringBuilder detailsBuilder,
            @NonNull String detailPart
    ) {
        if (detailsBuilder.length() > 0) {
            detailsBuilder.append(
                    " | "
            );
        }

        detailsBuilder.append(
                detailPart
        );
    }

    private long calculateRemainingSeconds(
            long cooldownUntilMillis,
            long currentTimeMillis
    ) {
        long remainingMilliseconds =
                cooldownUntilMillis
                        - currentTimeMillis;

        if (remainingMilliseconds <= 0L) {
            return 0L;
        }

        return Math.max(
                1L,
                (long) Math.ceil(
                        remainingMilliseconds
                                / 1000.0d
                )
        );
    }
}