package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Study Saathi का proactive Firebase AI request rate limiter।
 *
 * उद्देश्य:
 *
 * 1. बहुत कम समय में अत्यधिक Gemini requests रोकना।
 * 2. Firebase free-tier request-per-minute quota पर दबाव कम करना।
 * 3. App restart होने पर भी recent request timestamps याद रखना।
 * 4. Offline और cached answers को rate limit से प्रभावित न करना।
 * 5. अगली remote AI request कब भेजी जा सकती है, यह बताना।
 *
 * यह class केवल remote Gemini requests को नियंत्रित करेगी।
 * Basic offline Mathematics और cached answers पर इसका प्रभाव नहीं होगा।
 */
public final class FirebaseAiRequestRateLimiter {

    private static final String PREFERENCES_NAME =
            "study_saathi_firebase_ai_rate_limiter";

    private static final String KEY_REQUEST_TIMESTAMPS =
            "remote_request_timestamps";

    /**
     * Firebase error में 20 free-tier requests की सीमा दिखाई गई थी।
     *
     * सुरक्षा के लिए app अधिकतम 18 remote requests प्रति rolling
     * 60-second window स्वीकार करेगा।
     */
    private static final int MAXIMUM_REMOTE_REQUESTS_PER_WINDOW =
            18;

    /**
     * Rolling rate-limit window।
     */
    private static final long REQUEST_WINDOW_MILLIS =
            60_000L;

    /**
     * दो remote requests के बीच minimum सुरक्षित अंतर।
     *
     * 3.5 seconds रखने पर लगातार requests लगभग
     * 17 requests/minute तक सीमित रहती हैं।
     */
    private static final long MINIMUM_REQUEST_INTERVAL_MILLIS =
            3_500L;

    /**
     * Preferences में malformed या बहुत अधिक timestamps save होने से
     * बचाने के लिए hard safety limit।
     */
    private static final int MAXIMUM_STORED_TIMESTAMPS =
            40;

    @NonNull
    private final SharedPreferences preferences;

    public FirebaseAiRequestRateLimiter(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        preferences =
                applicationContext.getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE
                );

        removeExpiredRequestTimestamps();
    }

    /**
     * जाँचता है कि इस समय नई remote Gemini request भेजी जा सकती है या नहीं।
     *
     * यह method request को record नहीं करता।
     * Allowed result मिलने के बाद recordRemoteRequest() call करें।
     */
    @NonNull
    public synchronized RateLimitDecision canSendRemoteRequest() {
        long currentTimeMillis =
                System.currentTimeMillis();

        List<Long> validTimestamps =
                readValidRequestTimestamps(
                        currentTimeMillis
                );

        persistRequestTimestamps(
                validTimestamps
        );

        if (validTimestamps.isEmpty()) {
            return RateLimitDecision.allowed();
        }

        long latestRequestTimestamp =
                validTimestamps.get(
                        validTimestamps.size() - 1
                );

        long intervalReadyAt =
                latestRequestTimestamp
                        + MINIMUM_REQUEST_INTERVAL_MILLIS;

        if (currentTimeMillis < intervalReadyAt) {
            long retryAfterMilliseconds =
                    intervalReadyAt
                            - currentTimeMillis;

            return RateLimitDecision.blocked(
                    calculateRemainingSeconds(
                            retryAfterMilliseconds
                    ),
                    intervalReadyAt,
                    BlockReason.MINIMUM_INTERVAL
            );
        }

        if (validTimestamps.size()
                >= MAXIMUM_REMOTE_REQUESTS_PER_WINDOW) {

            long oldestRequestTimestamp =
                    validTimestamps.get(0);

            long rollingWindowReadyAt =
                    oldestRequestTimestamp
                            + REQUEST_WINDOW_MILLIS;

            if (currentTimeMillis
                    < rollingWindowReadyAt) {

                long retryAfterMilliseconds =
                        rollingWindowReadyAt
                                - currentTimeMillis;

                return RateLimitDecision.blocked(
                        calculateRemainingSeconds(
                                retryAfterMilliseconds
                        ),
                        rollingWindowReadyAt,
                        BlockReason.ROLLING_WINDOW
                );
            }
        }

        return RateLimitDecision.allowed();
    }

    /**
     * Gemini network request वास्तव में भेजने से ठीक पहले timestamp save करें।
     *
     * Failed request भी Firebase rate-limit usage में गिनी जा सकती है,
     * इसलिए request भेजने से पहले इसे record किया जाना चाहिए।
     */
    public synchronized void recordRemoteRequest() {
        long currentTimeMillis =
                System.currentTimeMillis();

        List<Long> validTimestamps =
                readValidRequestTimestamps(
                        currentTimeMillis
                );

        validTimestamps.add(
                currentTimeMillis
        );

        Collections.sort(
                validTimestamps
        );

        trimStoredTimestamps(
                validTimestamps
        );

        persistRequestTimestamps(
                validTimestamps
        );
    }

    /**
     * अगली remote request भेजे जाने से पहले शेष seconds।
     *
     * Request अभी allowed हो तो 0 लौटाता है।
     */
    public synchronized long getRemainingSeconds() {
        RateLimitDecision decision =
                canSendRemoteRequest();

        return decision.isAllowed()
                ? 0L
                : decision.getRetryAfterSeconds();
    }

    /**
     * Current rolling window में record की गई valid remote requests।
     */
    public synchronized int getCurrentWindowRequestCount() {
        List<Long> validTimestamps =
                readValidRequestTimestamps(
                        System.currentTimeMillis()
                );

        persistRequestTimestamps(
                validTimestamps
        );

        return validTimestamps.size();
    }

    /**
     * Expired request timestamps preferences से हटाता है।
     */
    public synchronized void removeExpiredRequestTimestamps() {
        List<Long> validTimestamps =
                readValidRequestTimestamps(
                        System.currentTimeMillis()
                );

        persistRequestTimestamps(
                validTimestamps
        );
    }

    /**
     * Testing या maintenance के लिए पूरी local request history साफ करता है।
     *
     * सामान्य app flow में इसे बार-बार call नहीं करना चाहिए।
     */
    public synchronized void clearAll() {
        preferences.edit()
                .remove(
                        KEY_REQUEST_TIMESTAMPS
                )
                .apply();
    }

    @NonNull
    private List<Long> readValidRequestTimestamps(
            long currentTimeMillis
    ) {
        String savedTimestamps =
                preferences.getString(
                        KEY_REQUEST_TIMESTAMPS,
                        ""
                );

        List<Long> validTimestamps =
                new ArrayList<>();

        if (savedTimestamps == null
                || savedTimestamps.trim().isEmpty()) {

            return validTimestamps;
        }

        String[] timestampParts =
                savedTimestamps.split(
                        ","
                );

        long earliestValidTimestamp =
                currentTimeMillis
                        - REQUEST_WINDOW_MILLIS;

        for (String timestampPart : timestampParts) {
            if (timestampPart == null
                    || timestampPart.trim().isEmpty()) {

                continue;
            }

            try {
                long timestamp =
                        Long.parseLong(
                                timestampPart.trim()
                        );

                if (timestamp
                        > earliestValidTimestamp
                        && timestamp
                        <= currentTimeMillis) {

                    validTimestamps.add(
                            timestamp
                    );
                }

            } catch (NumberFormatException ignored) {
                /*
                 * Corrupted timestamp को safely ignore किया जाएगा।
                 */
            }
        }

        Collections.sort(
                validTimestamps
        );

        trimStoredTimestamps(
                validTimestamps
        );

        return validTimestamps;
    }

    private void persistRequestTimestamps(
            @NonNull List<Long> requestTimestamps
    ) {
        if (requestTimestamps.isEmpty()) {
            preferences.edit()
                    .remove(
                            KEY_REQUEST_TIMESTAMPS
                    )
                    .apply();

            return;
        }

        StringBuilder savedValueBuilder =
                new StringBuilder();

        for (Long timestamp : requestTimestamps) {
            if (timestamp == null
                    || timestamp <= 0L) {

                continue;
            }

            if (savedValueBuilder.length() > 0) {
                savedValueBuilder.append(
                        ','
                );
            }

            savedValueBuilder.append(
                    timestamp
            );
        }

        preferences.edit()
                .putString(
                        KEY_REQUEST_TIMESTAMPS,
                        savedValueBuilder.toString()
                )
                .apply();
    }

    private void trimStoredTimestamps(
            @NonNull List<Long> requestTimestamps
    ) {
        while (requestTimestamps.size()
                > MAXIMUM_STORED_TIMESTAMPS) {

            requestTimestamps.remove(0);
        }
    }

    private long calculateRemainingSeconds(
            long remainingMilliseconds
    ) {
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

    public enum BlockReason {

        /**
         * पिछली remote request के बाद minimum interval अभी पूरा नहीं हुआ।
         */
        MINIMUM_INTERVAL,

        /**
         * Rolling 60-second window की local safety limit पूरी हो गई।
         */
        ROLLING_WINDOW
    }

    /**
     * Local rate-limit check का immutable result।
     */
    public static final class RateLimitDecision {

        private final boolean allowed;

        private final long retryAfterSeconds;

        private final long retryAllowedAtEpochMillis;

        private final BlockReason blockReason;

        private RateLimitDecision(
                boolean allowed,
                long retryAfterSeconds,
                long retryAllowedAtEpochMillis,
                BlockReason blockReason
        ) {
            this.allowed =
                    allowed;

            this.retryAfterSeconds =
                    Math.max(
                            0L,
                            retryAfterSeconds
                    );

            this.retryAllowedAtEpochMillis =
                    Math.max(
                            0L,
                            retryAllowedAtEpochMillis
                    );

            this.blockReason =
                    blockReason;
        }

        @NonNull
        private static RateLimitDecision allowed() {
            return new RateLimitDecision(
                    true,
                    0L,
                    0L,
                    null
            );
        }

        @NonNull
        private static RateLimitDecision blocked(
                long retryAfterSeconds,
                long retryAllowedAtEpochMillis,
                @NonNull BlockReason blockReason
        ) {
            return new RateLimitDecision(
                    false,
                    retryAfterSeconds,
                    retryAllowedAtEpochMillis,
                    blockReason
            );
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }

        public long getRetryAllowedAtEpochMillis() {
            return retryAllowedAtEpochMillis;
        }

        public BlockReason getBlockReason() {
            return blockReason;
        }

        /**
         * Student को दिखाने योग्य message।
         */
        @NonNull
        public String createUserMessage() {
            if (allowed) {
                return "Smart AI request भेजी जा सकती है।";
            }

            if (blockReason
                    == BlockReason.MINIMUM_INTERVAL) {

                return "Smart AI को पिछला प्रश्न अभी भेजा गया है। "
                        + retryAfterSeconds
                        + " सेकंड बाद अगला नया प्रश्न भेजें।";
            }

            return "Smart AI को बहुत सारे नए प्रश्न भेजे गए हैं। "
                    + retryAfterSeconds
                    + " सेकंड बाद अगला नया प्रश्न भेजें।";
        }
    }
}