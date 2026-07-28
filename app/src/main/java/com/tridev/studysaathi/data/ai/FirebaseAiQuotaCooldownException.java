package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Firebase AI / Gemini quota cooldown की structured exception।
 *
 * यह exception Activity तक निम्न जानकारी पहुँचाती है:
 *
 * 1. Quota या rate limit के कारण request रुकी।
 * 2. कितने seconds बाद request दोबारा भेजी जा सकती है।
 * 3. Cooldown समाप्त होने का exact timestamp।
 * 4. Request Gemini को भेजने से पहले block हुई या
 *    Firebase response मिलने के बाद quota error आया।
 * 5. Original Firebase exception क्या थी।
 */
public final class FirebaseAiQuotaCooldownException
        extends RuntimeException {

    private static final long MINIMUM_RETRY_AFTER_SECONDS =
            1L;

    @NonNull
    private final String userMessage;

    private final long retryAfterSeconds;

    private final long cooldownUntilEpochMillis;

    private final boolean requestBlockedBeforeSending;

    /**
     * Full constructor।
     */
    public FirebaseAiQuotaCooldownException(
            @NonNull String userMessage,
            long retryAfterSeconds,
            long cooldownUntilEpochMillis,
            boolean requestBlockedBeforeSending,
            @Nullable Throwable cause
    ) {
        super(
                createTechnicalMessage(
                        userMessage,
                        retryAfterSeconds,
                        cooldownUntilEpochMillis,
                        requestBlockedBeforeSending
                ),
                cause
        );

        this.userMessage =
                normalizeUserMessage(
                        userMessage
                );

        this.retryAfterSeconds =
                Math.max(
                        MINIMUM_RETRY_AFTER_SECONDS,
                        retryAfterSeconds
                );

        this.cooldownUntilEpochMillis =
                Math.max(
                        System.currentTimeMillis(),
                        cooldownUntilEpochMillis
                );

        this.requestBlockedBeforeSending =
                requestBlockedBeforeSending;
    }

    /**
     * Cooldown manager की current state से exception बनाता है।
     */
    @NonNull
    public static FirebaseAiQuotaCooldownException
    fromCooldownManager(
            @NonNull FirebaseAiQuotaCooldownManager cooldownManager,
            boolean requestBlockedBeforeSending,
            @Nullable Throwable cause
    ) {
        long remainingSeconds =
                cooldownManager.getRemainingSeconds();

        if (remainingSeconds <= 0L) {
            remainingSeconds =
                    MINIMUM_RETRY_AFTER_SECONDS;
        }

        long cooldownUntilEpochMillis =
                cooldownManager
                        .getCooldownUntilEpochMillis();

        if (cooldownUntilEpochMillis
                <= System.currentTimeMillis()) {

            cooldownUntilEpochMillis =
                    System.currentTimeMillis()
                            + remainingSeconds * 1000L;
        }

        return new FirebaseAiQuotaCooldownException(
                cooldownManager.createCooldownMessage(),
                remainingSeconds,
                cooldownUntilEpochMillis,
                requestBlockedBeforeSending,
                cause
        );
    }

    /**
     * Student को दिखाने योग्य message।
     */
    @NonNull
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Retry करने से पहले शेष seconds।
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * Cooldown समाप्त होने का epoch timestamp।
     */
    public long getCooldownUntilEpochMillis() {
        return cooldownUntilEpochMillis;
    }

    /**
     * true:
     * Request active cooldown के कारण Gemini को भेजी ही नहीं गई।
     *
     * false:
     * Request भेजी गई थी और Firebase ने quota error लौटाया।
     */
    public boolean wasRequestBlockedBeforeSending() {
        return requestBlockedBeforeSending;
    }

    /**
     * Activity के countdown के लिए current remaining seconds।
     *
     * समय बीतने के साथ यह value स्वतः कम होती रहेगी।
     */
    public long calculateCurrentRemainingSeconds() {
        long remainingMilliseconds =
                cooldownUntilEpochMillis
                        - System.currentTimeMillis();

        if (remainingMilliseconds <= 0L) {
            return 0L;
        }

        return Math.max(
                MINIMUM_RETRY_AFTER_SECONDS,
                (long) Math.ceil(
                        remainingMilliseconds
                                / 1000.0d
                )
        );
    }

    /**
     * Cooldown अभी active है या नहीं।
     */
    public boolean isCooldownStillActive() {
        return calculateCurrentRemainingSeconds()
                > 0L;
    }

    @NonNull
    private static String normalizeUserMessage(
            @Nullable String message
    ) {
        if (message == null) {
            return "Firebase AI की request limit अभी पूरी है। "
                    + "थोड़ी देर बाद दोबारा प्रयास करें।";
        }

        String normalizedMessage =
                message.trim();

        if (normalizedMessage.isEmpty()) {
            return "Firebase AI की request limit अभी पूरी है। "
                    + "थोड़ी देर बाद दोबारा प्रयास करें।";
        }

        return normalizedMessage;
    }

    @NonNull
    private static String createTechnicalMessage(
            @Nullable String userMessage,
            long retryAfterSeconds,
            long cooldownUntilEpochMillis,
            boolean requestBlockedBeforeSending
    ) {
        String normalizedUserMessage =
                normalizeUserMessage(
                        userMessage
                );

        return "Firebase AI quota cooldown active. "
                + "Retry after approximately "
                + Math.max(
                MINIMUM_RETRY_AFTER_SECONDS,
                retryAfterSeconds
        )
                + " seconds. "
                + "Cooldown until epoch millis: "
                + cooldownUntilEpochMillis
                + ". Request blocked before sending: "
                + requestBlockedBeforeSending
                + ". User message: "
                + normalizedUserMessage;
    }
}