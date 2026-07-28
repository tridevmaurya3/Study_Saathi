package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Study Saathi Hero Part के प्रत्येक सफल answer का
 * सामान्य immutable result model।
 *
 * यह model Activity को answer text के साथ:
 *
 * 1. Answer source
 * 2. Offline/online status
 * 3. Verification status
 * 4. Cache status
 * 5. Source reference
 * 6. Firebase model name
 *
 * उपलब्ध कराता है।
 *
 * वर्तमान legacy Hero screen getAnswerText() उपयोग करती है।
 * इसलिए getAnswerText() source label और original answer को
 * मिलाकर display-ready text लौटाता है।
 *
 * केवल वास्तविक answer चाहिए तो getRawAnswerText() उपयोग करें।
 * भविष्य में Text-to-Speech और answer verification के लिए
 * raw answer उपयोग किया जाएगा, ताकि source label पढ़ा न जाए।
 */
public final class SmartTutorAnswerResult {

    /**
     * Answer किस engine अथवा source से आया।
     */
    public enum AnswerSource {

        /**
         * Addition, subtraction, multiplication और division
         * जैसे deterministic local Mathematics answers।
         */
        OFFLINE_BASIC_MATH(
                "ऑफलाइन गणित",
                "✓",
                true,
                false,
                true
        ),

        /**
         * Divisibility rules और divisibility checking।
         */
        OFFLINE_DIVISIBILITY(
                "ऑफलाइन विभाज्यता",
                "✓",
                true,
                false,
                true
        ),

        /**
         * Curated और verified JSON knowledge content।
         */
        VERIFIED_OFFLINE_KNOWLEDGE(
                "सत्यापित ऑफलाइन सामग्री",
                "✓",
                true,
                false,
                true
        ),

        /**
         * Persistent cache से प्राप्त answer।
         */
        PERSISTENT_CACHE(
                "सेव किया गया उत्तर",
                "↻",
                true,
                true,
                false
        ),

        /**
         * Firebase AI Logic / Gemini से प्राप्त remote answer।
         */
        FIREBASE_AI(
                "Smart AI",
                "✦",
                false,
                false,
                false
        ),

        /**
         * Internet या verified content उपलब्ध न होने पर
         * general local fallback answer।
         */
        LOCAL_FALLBACK(
                "ऑफलाइन सहायता",
                "●",
                true,
                false,
                false
        ),

        /**
         * Source की स्पष्ट पहचान उपलब्ध नहीं है।
         */
        UNKNOWN(
                "उत्तर",
                "●",
                false,
                false,
                false
        );

        @NonNull
        private final String displayLabel;

        @NonNull
        private final String displayIcon;

        private final boolean offline;

        private final boolean cached;

        private final boolean verifiedByDefault;

        AnswerSource(
                @NonNull String displayLabel,
                @NonNull String displayIcon,
                boolean offline,
                boolean cached,
                boolean verifiedByDefault
        ) {
            this.displayLabel =
                    displayLabel;

            this.displayIcon =
                    displayIcon;

            this.offline =
                    offline;

            this.cached =
                    cached;

            this.verifiedByDefault =
                    verifiedByDefault;
        }

        @NonNull
        public String getDisplayLabel() {
            return displayLabel;
        }

        @NonNull
        public String getDisplayIcon() {
            return displayIcon;
        }

        public boolean isOffline() {
            return offline;
        }

        public boolean isCached() {
            return cached;
        }

        public boolean isVerifiedByDefault() {
            return verifiedByDefault;
        }
    }

    /**
     * केवल वास्तविक educational answer।
     *
     * इसमें source label शामिल नहीं है।
     */
    @NonNull
    private final String rawAnswerText;

    @NonNull
    private final AnswerSource answerSource;

    /**
     * अतिरिक्त source जानकारी।
     *
     * उदाहरण:
     *
     * Study Saathi Curated Foundation
     * FIREBASE_AI
     * OFFLINE_BASIC_MATH
     */
    @NonNull
    private final String sourceDetails;

    /**
     * Knowledge entry ID अथवा अन्य internal reference।
     */
    @NonNull
    private final String referenceId;

    /**
     * Firebase AI answer के लिए model name।
     */
    @NonNull
    private final String modelName;

    /**
     * Answer verified माना जा सकता है या नहीं।
     */
    private final boolean verified;

    /**
     * Result बनने का समय।
     */
    private final long createdAtEpochMillis;

    private SmartTutorAnswerResult(
            @NonNull String answerText,
            @NonNull AnswerSource answerSource,
            @Nullable String sourceDetails,
            @Nullable String referenceId,
            @Nullable String modelName,
            boolean verified,
            long createdAtEpochMillis
    ) {
        this.rawAnswerText =
                safeText(
                        answerText
                );

        this.answerSource =
                answerSource;

        this.sourceDetails =
                safeText(
                        sourceDetails
                );

        this.referenceId =
                safeText(
                        referenceId
                );

        this.modelName =
                safeText(
                        modelName
                );

        this.verified =
                verified;

        this.createdAtEpochMillis =
                createdAtEpochMillis > 0L
                        ? createdAtEpochMillis
                        : System.currentTimeMillis();
    }

    /**
     * Basic arithmetic offline answer result।
     */
    @NonNull
    public static SmartTutorAnswerResult
    fromOfflineBasicMath(
            @NonNull String answerText
    ) {
        return new SmartTutorAnswerResult(
                answerText,
                AnswerSource.OFFLINE_BASIC_MATH,
                OfflineSmartAnswerRouter
                        .SOURCE_OFFLINE_BASIC_MATH,
                "",
                "",
                true,
                System.currentTimeMillis()
        );
    }

    /**
     * Divisibility offline answer result।
     */
    @NonNull
    public static SmartTutorAnswerResult
    fromOfflineDivisibility(
            @NonNull String answerText
    ) {
        return new SmartTutorAnswerResult(
                answerText,
                AnswerSource.OFFLINE_DIVISIBILITY,
                OfflineSmartAnswerRouter
                        .SOURCE_OFFLINE_DIVISIBILITY,
                "",
                "",
                true,
                System.currentTimeMillis()
        );
    }

    /**
     * OfflineSmartAnswerRouter result से structured answer बनाता है।
     */
    @NonNull
    public static SmartTutorAnswerResult
    fromOfflineMathematicsRoute(
            @NonNull String answerText,
            @Nullable String routerSource
    ) {
        String safeRouterSource =
                safeText(
                        routerSource
                );

        if (OfflineSmartAnswerRouter
                .SOURCE_OFFLINE_DIVISIBILITY
                .equals(
                        safeRouterSource
                )) {

            return fromOfflineDivisibility(
                    answerText
            );
        }

        return fromOfflineBasicMath(
                answerText
        );
    }

    /**
     * Curated verified knowledge JSON answer result।
     */
    @NonNull
    public static SmartTutorAnswerResult
    fromVerifiedOfflineKnowledge(
            @NonNull String answerText,
            @Nullable String sourceLabel,
            @Nullable String knowledgeEntryId
    ) {
        return new SmartTutorAnswerResult(
                answerText,
                AnswerSource.VERIFIED_OFFLINE_KNOWLEDGE,
                sourceLabel,
                knowledgeEntryId,
                "",
                true,
                System.currentTimeMillis()
        );
    }

    /**
     * Persistent cache answer result।
     */
    @NonNull
    public static SmartTutorAnswerResult
    fromPersistentCache(
            @NonNull String answerText,
            @Nullable String originalAnswerSource
    ) {
        return new SmartTutorAnswerResult(
                answerText,
                AnswerSource.PERSISTENT_CACHE,
                originalAnswerSource,
                "",
                "",
                false,
                System.currentTimeMillis()
        );
    }

    /**
     * Firebase AI / Gemini answer result।
     */
    @NonNull
    public static SmartTutorAnswerResult
    fromFirebaseAi(
            @NonNull String answerText,
            @Nullable String modelName
    ) {
        return new SmartTutorAnswerResult(
                answerText,
                AnswerSource.FIREBASE_AI,
                "Firebase AI Logic",
                "",
                modelName,
                false,
                System.currentTimeMillis()
        );
    }

    /**
     * General local fallback result।
     */
    @NonNull
    public static SmartTutorAnswerResult
    fromLocalFallback(
            @NonNull String answerText
    ) {
        return new SmartTutorAnswerResult(
                answerText,
                AnswerSource.LOCAL_FALLBACK,
                "",
                "",
                "",
                false,
                System.currentTimeMillis()
        );
    }

    /**
     * Backward-compatible unknown-source result।
     */
    @NonNull
    public static SmartTutorAnswerResult
    fromUnknownSource(
            @NonNull String answerText
    ) {
        return new SmartTutorAnswerResult(
                answerText,
                AnswerSource.UNKNOWN,
                "",
                "",
                "",
                false,
                System.currentTimeMillis()
        );
    }

    /**
     * Answer Hero screen पर दिखाने योग्य है या नहीं।
     */
    public boolean isValid() {
        return !rawAnswerText.isEmpty();
    }

    /**
     * Existing Hero screen के लिए display-ready answer।
     *
     * Output example:
     *
     * ✓ सत्यापित ऑफलाइन सामग्री
     *
     * पौधे सूर्य के प्रकाश की सहायता से...
     */
    @NonNull
    public String buildDisplayAnswerText() {
        if (rawAnswerText.isEmpty()) {
            return "";
        }

        StringBuilder displayBuilder =
                new StringBuilder();

        displayBuilder.append(
                buildSourceBadgeText()
        );

        displayBuilder.append(
                "\n\n"
        );

        displayBuilder.append(
                rawAnswerText
        );

        return displayBuilder.toString()
                .trim();
    }

    /**
     * Hero screen के source label में दिखाने योग्य text।
     */
    @NonNull
    public String buildSourceBadgeText() {
        return answerSource.getDisplayIcon()
                + " "
                + answerSource.getDisplayLabel();
    }

    /**
     * Source की detailed debugging information।
     *
     * यह student को सामान्य रूप से नहीं दिखानी चाहिए।
     */
    @NonNull
    public String buildTechnicalSourceDescription() {
        StringBuilder descriptionBuilder =
                new StringBuilder();

        descriptionBuilder.append(
                answerSource.name()
        );

        if (!sourceDetails.isEmpty()) {
            descriptionBuilder.append(
                    " | Details: "
            );

            descriptionBuilder.append(
                    sourceDetails
            );
        }

        if (!referenceId.isEmpty()) {
            descriptionBuilder.append(
                    " | Reference: "
            );

            descriptionBuilder.append(
                    referenceId
            );
        }

        if (!modelName.isEmpty()) {
            descriptionBuilder.append(
                    " | Model: "
            );

            descriptionBuilder.append(
                    modelName
            );
        }

        descriptionBuilder.append(
                " | Offline: "
        );

        descriptionBuilder.append(
                isOffline()
        );

        descriptionBuilder.append(
                " | Cached: "
        );

        descriptionBuilder.append(
                isCached()
        );

        descriptionBuilder.append(
                " | Verified: "
        );

        descriptionBuilder.append(
                isVerified()
        );

        return descriptionBuilder.toString();
    }

    /**
     * Answer device पर local route से मिला है।
     */
    public boolean isOffline() {
        return answerSource.isOffline();
    }

    /**
     * Answer persistent cache से मिला है।
     */
    public boolean isCached() {
        return answerSource.isCached();
    }

    /**
     * Answer Firebase/Gemini network route से मिला है।
     */
    public boolean isRemoteAiAnswer() {
        return answerSource
                == AnswerSource.FIREBASE_AI;
    }

    /**
     * Deterministic अथवा curated verified answer।
     */
    public boolean isVerified() {
        return verified
                || answerSource.isVerifiedByDefault();
    }

    /**
     * Backward-compatible Hero display text।
     *
     * वर्तमान FirebaseStudyTutorClient का legacy callback
     * यही method उपयोग करता है। इसलिए source label screen
     * पर बिना Activity बदले दिखाई देगा।
     */
    @NonNull
    public String getAnswerText() {
        return buildDisplayAnswerText();
    }

    /**
     * केवल वास्तविक answer text।
     *
     * Source label इसमें शामिल नहीं है।
     *
     * Text-to-Speech, cache, answer verification और export
     * जैसे future features में इसका उपयोग करना चाहिए।
     */
    @NonNull
    public String getRawAnswerText() {
        return rawAnswerText;
    }

    @NonNull
    public AnswerSource getAnswerSource() {
        return answerSource;
    }

    @NonNull
    public String getSourceDetails() {
        return sourceDetails;
    }

    @NonNull
    public String getReferenceId() {
        return referenceId;
    }

    @NonNull
    public String getModelName() {
        return modelName;
    }

    public long getCreatedAtEpochMillis() {
        return createdAtEpochMillis;
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }
}