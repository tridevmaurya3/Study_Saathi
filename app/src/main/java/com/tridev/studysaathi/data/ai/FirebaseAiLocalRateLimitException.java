package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;

/**
 * Study Saathi के proactive local Firebase AI rate limiter की
 * structured exception।
 *
 * यह Firebase server का quota error नहीं है।
 *
 * यह exception तब दी जाएगी जब app स्वयं किसी remote Gemini
 * request को भेजने से पहले थोड़े समय के लिए रोकता है, ताकि:
 *
 * 1. Firebase free-tier request limit पर दबाव कम हो।
 * 2. बहुत तेजी से लगातार requests न भेजी जाएँ।
 * 3. बच्चे को स्पष्ट retry time बताया जा सके।
 * 4. Local limiter और Firebase quota error अलग पहचाने जा सकें।
 */
public final class FirebaseAiLocalRateLimitException
        extends RuntimeException {

    private static final long MINIMUM_RETRY_SECONDS =
            1L;

    @NonNull
    private final String userMessage;

    private final long retryAfterSeconds;

    private final long retryAllowedAtEpochMillis;

    @NonNull
    private final FirebaseAiRequestRateLimiter.BlockReason
            blockReason;

    public FirebaseAiLocalRateLimitException(
            @NonNull String userMessage,
            long retryAfterSeconds,
            long retryAllowedAtEpochMillis,
            @NonNull FirebaseAiRequestRateLimiter.BlockReason blockReason
    ) {
        super(
                createTechnicalMessage(
                        userMessage,
                        retryAfterSeconds,
                        retryAllowedAtEpochMillis,
                        blockReason
                )
        );

        this.userMessage =
                normalizeUserMessage(
                        userMessage
                );

        this.retryAfterSeconds =
                Math.max(
                        MINIMUM_RETRY_SECONDS,
                        retryAfterSeconds
                );

        this.retryAllowedAtEpochMillis =
                Math.max(
                        System.currentTimeMillis(),
                        retryAllowedAtEpochMillis
                );

        this.blockReason =
                blockReason;
    }

    /**
     * FirebaseAiRequestRateLimiter के blocked decision से
     * exception तैयार करता है।
     */
    @NonNull
    public static FirebaseAiLocalRateLimitException
    fromDecision(
            @NonNull FirebaseAiRequestRateLimiter
                    .RateLimitDecision decision
    ) {
        if (decision.isAllowed()) {
            throw new IllegalArgumentException(
                    "Allowed rate-limit decision से "
                            + "FirebaseAiLocalRateLimitException "
                            + "नहीं बनाई जा सकती।"
            );
        }

        FirebaseAiRequestRateLimiter.BlockReason reason =
                decision.getBlockReason();

        if (reason == null) {
            throw new IllegalArgumentException(
                    "Blocked rate-limit decision में "
                            + "block reason उपलब्ध नहीं है।"
            );
        }

        long retryAfterSeconds =
                Math.max(
                        MINIMUM_RETRY_SECONDS,
                        decision.getRetryAfterSeconds()
                );

        long retryAllowedAtEpochMillis =
                decision.getRetryAllowedAtEpochMillis();

        if (retryAllowedAtEpochMillis
                <= System.currentTimeMillis()) {

            retryAllowedAtEpochMillis =
                    System.currentTimeMillis()
                            + retryAfterSeconds * 1000L;
        }

        return new FirebaseAiLocalRateLimitException(
                decision.createUserMessage(),
                retryAfterSeconds,
                retryAllowedAtEpochMillis,
                reason
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
     * अगली remote Gemini request से पहले शेष seconds।
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * अगली remote request allowed होने का epoch timestamp।
     */
    public long getRetryAllowedAtEpochMillis() {
        return retryAllowedAtEpochMillis;
    }

    /**
     * Request किस local rule के कारण रोकी गई।
     */
    @NonNull
    public FirebaseAiRequestRateLimiter.BlockReason
    getBlockReason() {
        return blockReason;
    }

    /**
     * Current समय के अनुसार वास्तविक शेष seconds।
     */
    public long calculateCurrentRemainingSeconds() {
        long remainingMilliseconds =
                retryAllowedAtEpochMillis
                        - System.currentTimeMillis();

        if (remainingMilliseconds <= 0L) {
            return 0L;
        }

        return Math.max(
                MINIMUM_RETRY_SECONDS,
                (long) Math.ceil(
                        remainingMilliseconds
                                / 1000.0d
                )
        );
    }

    /**
     * Local waiting period अभी active है या नहीं।
     */
    public boolean isWaitingPeriodActive() {
        return calculateCurrentRemainingSeconds()
                > 0L;
    }

    /**
     * true होने पर पिछली remote request के बाद minimum
     * interval पूरा नहीं हुआ था।
     */
    public boolean isMinimumIntervalBlock() {
        return blockReason
                == FirebaseAiRequestRateLimiter
                .BlockReason.MINIMUM_INTERVAL;
    }

    /**
     * true होने पर rolling 60-second local safety limit
     * पूरी हो गई थी।
     */
    public boolean isRollingWindowBlock() {
        return blockReason
                == FirebaseAiRequestRateLimiter
                .BlockReason.ROLLING_WINDOW;
    }

    @NonNull
    private static String normalizeUserMessage(
            String userMessage
    ) {
        if (userMessage == null
                || userMessage.trim().isEmpty()) {

            return "Smart AI को अगला प्रश्न भेजने से पहले "
                    + "थोड़ी देर प्रतीक्षा करें।";
        }

        return userMessage.trim();
    }

    @NonNull
    private static String createTechnicalMessage(
            String userMessage,
            long retryAfterSeconds,
            long retryAllowedAtEpochMillis,
            @NonNull FirebaseAiRequestRateLimiter
                    .BlockReason blockReason
    ) {
        return "Study Saathi local Firebase AI rate limit active. "
                + "Retry after approximately "
                + Math.max(
                MINIMUM_RETRY_SECONDS,
                retryAfterSeconds
        )
                + " seconds. "
                + "Retry allowed at epoch millis: "
                + retryAllowedAtEpochMillis
                + ". Block reason: "
                + blockReason.name()
                + ". User message: "
                + normalizeUserMessage(
                userMessage
        );
    }
}