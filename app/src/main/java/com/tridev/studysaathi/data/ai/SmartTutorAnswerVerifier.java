package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Study Saathi Hero Part का reusable answer-verification engine।
 *
 * यह class factual truth का झूठा दावा नहीं करती।
 *
 * इसके मुख्य कार्य:
 *
 * 1. Deterministic offline answers को verified पहचानना।
 * 2. Curated offline knowledge को verified पहचानना।
 * 3. Gemini और cached answers की quality जाँचना।
 * 4. Empty, incomplete, uncertain अथवा suspicious answer पहचानना।
 * 5. Prompt leakage और system-instruction leakage पहचानना।
 * 6. Child-friendly caution message तैयार करना।
 * 7. Retry की आवश्यकता बताना।
 *
 * वास्तविक textbook verification भविष्य में chapter/PDF grounded
 * retrieval system से जोड़ी जाएगी। यह class अभी local quality
 * और reliability inspection प्रदान करती है।
 */
public final class SmartTutorAnswerVerifier {

    private static final int MINIMUM_USEFUL_ANSWER_LENGTH =
            12;

    private static final int MINIMUM_DETAILED_ANSWER_LENGTH =
            35;

    private static final int MAXIMUM_DISPLAY_ANSWER_LENGTH =
            30000;

    @NonNull
    private static final String[] UNCERTAINTY_PHRASES = {
            "शायद",
            "संभवतः",
            "हो सकता है",
            "हो सकती है",
            "मुझे लगता है",
            "पक्का नहीं",
            "निश्चित नहीं",
            "जानकारी स्पष्ट नहीं",
            "may be",
            "maybe",
            "possibly",
            "probably",
            "i think",
            "i am not sure",
            "not certain",
            "cannot confirm",
            "could be"
    };

    @NonNull
    private static final String[] FAILURE_OR_REFUSAL_PHRASES = {
            "उत्तर नहीं दे सकता",
            "उत्तर नहीं दे सकती",
            "मुझे जानकारी नहीं है",
            "मैं इसे समझ नहीं पाया",
            "मैं इसे समझ नहीं पाई",
            "प्रश्न स्पष्ट नहीं है",
            "दोबारा प्रयास करें",
            "कुछ गलत हो गया",
            "response प्राप्त नहीं",
            "answer खाली है",
            "as an ai language model",
            "i cannot answer",
            "i cannot help",
            "i do not know",
            "i don't know",
            "unable to answer",
            "try again",
            "something went wrong",
            "no response"
    };

    @NonNull
    private static final String[] PROMPT_LEAKAGE_PHRASES = {
            "system prompt",
            "developer message",
            "hidden instruction",
            "internal instruction",
            "ignore previous instructions",
            "ignore all instructions",
            "BEGIN PREVIOUS CONVERSATION CONTEXT",
            "END PREVIOUS CONVERSATION CONTEXT",
            "STUDENT CONTEXT",
            "TEACHING RULES",
            "CURRENT STUDENT QUESTION",
            "LANGUAGE INSTRUCTION",
            "CHILD SAFETY AND ACCURACY RULES"
    };

    @NonNull
    private static final String[] UNSUPPORTED_CERTAINTY_PHRASES = {
            "100% सही",
            "सौ प्रतिशत सही",
            "पूरी गारंटी",
            "हमेशा बिल्कुल सही",
            "कभी गलत नहीं",
            "100% correct",
            "completely guaranteed",
            "always correct",
            "never wrong",
            "definitely true in every case"
    };

    @NonNull
    private static final String[] INCOMPLETE_ENDINGS = {
            ":",
            ",",
            ";",
            "-",
            "और",
            "या",
            "because",
            "and",
            "or",
            "such as",
            "for example"
    };

    private SmartTutorAnswerVerifier() {
        /*
         * Utility class है।
         */
    }

    /**
     * Structured answer result की verification करता है।
     */
    @NonNull
    public static VerificationResult verify(
            @NonNull SmartTutorAnswerResult answerResult,
            @Nullable String question,
            @Nullable String subjectName,
            @Nullable String explanationLanguage
    ) {
        return verify(
                answerResult.getRawAnswerText(),
                answerResult.getAnswerSource(),
                answerResult.isVerified(),
                question,
                subjectName,
                explanationLanguage
        );
    }

    /**
     * Raw answer और source के आधार पर verification करता है।
     */
    @NonNull
    public static VerificationResult verify(
            @Nullable String answerText,
            @Nullable SmartTutorAnswerResult.AnswerSource answerSource,
            boolean alreadyVerified,
            @Nullable String question,
            @Nullable String subjectName,
            @Nullable String explanationLanguage
    ) {
        String normalizedAnswer =
                normalizeText(
                        answerText
                );

        String normalizedQuestion =
                normalizeText(
                        question
                );

        String normalizedSubject =
                normalizeText(
                        subjectName
                );

        boolean englishPreferred =
                prefersEnglish(
                        explanationLanguage,
                        normalizedAnswer
                );

        SmartTutorAnswerResult.AnswerSource safeSource =
                answerSource == null
                        ? SmartTutorAnswerResult.AnswerSource.UNKNOWN
                        : answerSource;

        if (normalizedAnswer.isEmpty()) {
            return VerificationResult.retryRecommended(
                    VerificationReason.EMPTY_ANSWER,
                    englishPreferred
                            ? "The answer is empty. Please ask the question again."
                            : "उत्तर खाली है। कृपया प्रश्न दोबारा पूछें।"
            );
        }

        if (normalizedAnswer.length()
                > MAXIMUM_DISPLAY_ANSWER_LENGTH) {

            return VerificationResult.caution(
                    VerificationReason.ANSWER_TOO_LONG,
                    englishPreferred
                            ? "This answer is unusually long. Check the important points before using it."
                            : "यह उत्तर असामान्य रूप से लंबा है। उपयोग करने से पहले मुख्य बातों की जाँच करें।"
            );
        }

        /*
         * Deterministic Mathematics और curated verified knowledge
         * को local verified माना जा सकता है।
         */
        if (safeSource
                == SmartTutorAnswerResult.AnswerSource.OFFLINE_BASIC_MATH) {

            return VerificationResult.verified(
                    VerificationReason.DETERMINISTIC_OFFLINE_MATH,
                    englishPreferred
                            ? "Verified by the offline Mathematics engine."
                            : "ऑफलाइन Mathematics engine द्वारा सत्यापित।"
            );
        }

        if (safeSource
                == SmartTutorAnswerResult.AnswerSource.OFFLINE_DIVISIBILITY) {

            return VerificationResult.verified(
                    VerificationReason.DETERMINISTIC_DIVISIBILITY,
                    englishPreferred
                            ? "Verified by the offline divisibility engine."
                            : "ऑफलाइन divisibility engine द्वारा सत्यापित।"
            );
        }

        if (safeSource
                == SmartTutorAnswerResult.AnswerSource
                .VERIFIED_OFFLINE_KNOWLEDGE
                || alreadyVerified) {

            return VerificationResult.verified(
                    VerificationReason.CURATED_OFFLINE_KNOWLEDGE,
                    englishPreferred
                            ? "Matched with verified offline study content."
                            : "सत्यापित offline study content से मिलान किया गया।"
            );
        }

        String searchableAnswer =
                normalizedAnswer.toLowerCase(
                        Locale.ROOT
                );

        if (containsAny(
                searchableAnswer,
                PROMPT_LEAKAGE_PHRASES
        )) {
            return VerificationResult.retryRecommended(
                    VerificationReason.PROMPT_OR_SYSTEM_LEAKAGE,
                    englishPreferred
                            ? "This answer contains internal instruction text. Please generate the answer again."
                            : "इस उत्तर में internal instruction text दिखाई दे रहा है। कृपया उत्तर दोबारा तैयार करें।"
            );
        }

        if (normalizedAnswer.length()
                < MINIMUM_USEFUL_ANSWER_LENGTH) {

            return VerificationResult.retryRecommended(
                    VerificationReason.ANSWER_TOO_SHORT,
                    englishPreferred
                            ? "The answer is too short to be useful. Please ask for a complete explanation."
                            : "उत्तर उपयोगी होने के लिए बहुत छोटा है। कृपया पूरा explanation माँगें।"
            );
        }

        if (containsAny(
                searchableAnswer,
                FAILURE_OR_REFUSAL_PHRASES
        )) {
            return VerificationResult.retryRecommended(
                    VerificationReason.FAILURE_OR_REFUSAL_TEXT,
                    englishPreferred
                            ? "A complete educational answer was not produced. Please retry or rephrase the question."
                            : "पूरा शैक्षिक उत्तर तैयार नहीं हुआ। कृपया प्रश्न दोबारा या दूसरे तरीके से पूछें।"
            );
        }

        if (containsAny(
                searchableAnswer,
                UNSUPPORTED_CERTAINTY_PHRASES
        )) {
            return VerificationResult.caution(
                    VerificationReason.UNSUPPORTED_CERTAINTY,
                    englishPreferred
                            ? "The answer makes an absolute claim. Check it with the textbook or teacher."
                            : "उत्तर पूर्ण निश्चितता का दावा कर रहा है। इसे textbook या teacher से जाँचें।"
            );
        }

        if (containsAny(
                searchableAnswer,
                UNCERTAINTY_PHRASES
        )) {
            return VerificationResult.caution(
                    VerificationReason.UNCERTAIN_LANGUAGE,
                    englishPreferred
                            ? "The answer contains uncertainty. Check the textbook page, question image, or a trusted source."
                            : "उत्तर में अनिश्चितता है। Textbook page, question photo या भरोसेमंद स्रोत से जाँच करें।"
            );
        }

        if (looksIncomplete(
                normalizedAnswer
        )) {
            return VerificationResult.retryRecommended(
                    VerificationReason.INCOMPLETE_ANSWER,
                    englishPreferred
                            ? "The answer appears incomplete. Please generate it again."
                            : "उत्तर अधूरा दिखाई दे रहा है। कृपया इसे दोबारा तैयार करें।"
            );
        }

        if (isLanguageMismatch(
                normalizedAnswer,
                explanationLanguage
        )) {
            return VerificationResult.caution(
                    VerificationReason.LANGUAGE_MISMATCH,
                    englishPreferred
                            ? "The answer language does not fully match the selected language."
                            : "उत्तर की भाषा चुनी गई explanation language से पूरी तरह मेल नहीं खाती।"
            );
        }

        /*
         * बहुत छोटा Gemini/cache answer गलत होना जरूरी नहीं है,
         * लेकिन detailed learning के लिए caution दिया जाएगा।
         */
        if (normalizedAnswer.length()
                < MINIMUM_DETAILED_ANSWER_LENGTH
                && isExplanationQuestion(
                normalizedQuestion
        )) {

            return VerificationResult.caution(
                    VerificationReason.EXPLANATION_TOO_BRIEF,
                    englishPreferred
                            ? "The answer may be correct, but the explanation is very brief."
                            : "उत्तर सही हो सकता है, लेकिन explanation बहुत छोटा है।"
            );
        }

        if (safeSource
                == SmartTutorAnswerResult.AnswerSource.PERSISTENT_CACHE) {

            return VerificationResult.highConfidence(
                    VerificationReason.CACHED_PREVIOUS_ANSWER,
                    englishPreferred
                            ? "This is a previously saved answer. Recheck it when the textbook or chapter changes."
                            : "यह पहले से save किया गया उत्तर है। Textbook या chapter बदलने पर इसे दोबारा जाँचें।"
            );
        }

        if (safeSource
                == SmartTutorAnswerResult.AnswerSource.FIREBASE_AI) {

            return VerificationResult.highConfidence(
                    VerificationReason.AI_QUALITY_CHECK_PASSED,
                    englishPreferred
                            ? "The answer passed local quality checks. Important facts should still be checked with the textbook."
                            : "उत्तर ने local quality checks पास किए हैं। महत्वपूर्ण तथ्यों को फिर भी textbook से जाँचें।"
            );
        }

        if (safeSource
                == SmartTutorAnswerResult.AnswerSource.LOCAL_FALLBACK) {

            return VerificationResult.highConfidence(
                    VerificationReason.LOCAL_SAFETY_OR_FALLBACK,
                    englishPreferred
                            ? "This is a local safety or fallback response."
                            : "यह local safety या fallback response है।"
            );
        }

        if (!normalizedSubject.isEmpty()
                && normalizedAnswer.length()
                >= MINIMUM_DETAILED_ANSWER_LENGTH) {

            return VerificationResult.highConfidence(
                    VerificationReason.BASIC_QUALITY_CHECK_PASSED,
                    englishPreferred
                            ? "The answer passed basic local quality checks."
                            : "उत्तर ने basic local quality checks पास किए हैं।"
            );
        }

        return VerificationResult.caution(
                VerificationReason.SOURCE_NOT_VERIFIED,
                englishPreferred
                        ? "The answer source could not be fully verified."
                        : "उत्तर का source पूरी तरह सत्यापित नहीं हो सका।"
        );
    }

    /**
     * Gemini prompt में जोड़ने के लिए verification instruction।
     */
    @NonNull
    public static String buildAiVerificationInstruction(
            @Nullable String explanationLanguage
    ) {
        String language =
                safeText(
                        explanationLanguage
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (language.contains(
                "english"
        )
                && !language.contains(
                "hindi"
        )) {

            return "Before returning the final answer, silently verify the calculation, factual consistency, class-level suitability, and whether the answer directly addresses the question. "
                    + "Do not claim 100% certainty without evidence. "
                    + "If important information is unclear or missing, state the uncertainty and request the relevant textbook page or a clearer question image.";
        }

        return "Final answer देने से पहले calculation, factual consistency, student class level और current question से relevance को silently verify करें। "
                + "बिना प्रमाण 100% निश्चितता का दावा न करें। "
                + "जरूरी जानकारी अस्पष्ट या missing हो तो uncertainty बताएँ और relevant textbook page या clearer question image माँगें।";
    }

    /**
     * Answer के अंत में student-facing verification note जोड़ता है।
     *
     * Verified answer में सामान्यतः note जोड़ने की आवश्यकता नहीं होती।
     */
    @NonNull
    public static String buildAnswerWithVerificationNote(
            @Nullable String answerText,
            @NonNull VerificationResult verificationResult
    ) {
        String safeAnswer =
                normalizeText(
                        answerText
                );

        if (safeAnswer.isEmpty()) {
            return verificationResult
                    .getStudentMessage();
        }

        if (!verificationResult
                .shouldShowStudentMessage()) {

            return safeAnswer;
        }

        String message =
                verificationResult
                        .getStudentMessage();

        if (message.isEmpty()) {
            return safeAnswer;
        }

        return safeAnswer
                + "\n\n"
                + verificationResult.getDisplayIcon()
                + " "
                + message;
    }

    /**
     * Explanation माँगने वाले question पहचानता है।
     */
    private static boolean isExplanationQuestion(
            @NonNull String question
    ) {
        String searchableQuestion =
                question.toLowerCase(
                        Locale.ROOT
                );

        return searchableQuestion.contains(
                "समझाओ"
        )
                || searchableQuestion.contains(
                "व्याख्या"
        )
                || searchableQuestion.contains(
                "कैसे"
        )
                || searchableQuestion.contains(
                "क्यों"
        )
                || searchableQuestion.contains(
                "वर्णन"
        )
                || searchableQuestion.contains(
                "explain"
        )
                || searchableQuestion.contains(
                "describe"
        )
                || searchableQuestion.contains(
                "how"
        )
                || searchableQuestion.contains(
                "why"
        );
    }

    /**
     * Answer किसी unfinished connector पर समाप्त हो रहा है या नहीं।
     */
    private static boolean looksIncomplete(
            @NonNull String answerText
    ) {
        String normalized =
                answerText.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        for (String incompleteEnding :
                INCOMPLETE_ENDINGS) {

            if (normalized.endsWith(
                    incompleteEnding.toLowerCase(
                            Locale.ROOT
                    )
            )) {
                return true;
            }
        }

        int openingRoundBrackets =
                countCharacter(
                        normalized,
                        '('
                );

        int closingRoundBrackets =
                countCharacter(
                        normalized,
                        ')'
                );

        int openingSquareBrackets =
                countCharacter(
                        normalized,
                        '['
                );

        int closingSquareBrackets =
                countCharacter(
                        normalized,
                        ']'
                );

        return openingRoundBrackets
                != closingRoundBrackets
                || openingSquareBrackets
                != closingSquareBrackets;
    }

    private static int countCharacter(
            @NonNull String text,
            char targetCharacter
    ) {
        int count =
                0;

        for (int index = 0;
             index < text.length();
             index++) {

            if (text.charAt(
                    index
            )
                    == targetCharacter) {

                count++;
            }
        }

        return count;
    }

    /**
     * Selected Hindi/English language और answer script का basic मिलान।
     */
    private static boolean isLanguageMismatch(
            @NonNull String answerText,
            @Nullable String explanationLanguage
    ) {
        String language =
                safeText(
                        explanationLanguage
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        boolean explicitlyEnglish =
                language.contains(
                        "english"
                )
                        && !language.contains(
                        "hindi"
                );

        boolean explicitlyHindi =
                language.contains(
                        "hindi"
                )
                        && !language.contains(
                        "english"
                );

        if (!explicitlyEnglish
                && !explicitlyHindi) {

            return false;
        }

        int devanagariCount =
                countDevanagariCharacters(
                        answerText
                );

        int latinLetterCount =
                countLatinLetters(
                        answerText
                );

        if (explicitlyEnglish) {
            return devanagariCount
                    > latinLetterCount;
        }

        return latinLetterCount
                > devanagariCount * 3
                && devanagariCount < 8;
    }

    private static int countDevanagariCharacters(
            @NonNull String text
    ) {
        int count =
                0;

        for (int index = 0;
             index < text.length();
             index++) {

            char character =
                    text.charAt(
                            index
                    );

            if (character >= '\u0900'
                    && character <= '\u097F') {

                count++;
            }
        }

        return count;
    }

    private static int countLatinLetters(
            @NonNull String text
    ) {
        int count =
                0;

        for (int index = 0;
             index < text.length();
             index++) {

            char character =
                    text.charAt(
                            index
                    );

            if ((character >= 'A'
                    && character <= 'Z')
                    || (character >= 'a'
                    && character <= 'z')) {

                count++;
            }
        }

        return count;
    }

    private static boolean prefersEnglish(
            @Nullable String explanationLanguage,
            @NonNull String answerText
    ) {
        String language =
                safeText(
                        explanationLanguage
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (language.contains(
                "english"
        )
                && !language.contains(
                "hindi"
        )) {

            return true;
        }

        if (language.contains(
                "hindi"
        )
                && !language.contains(
                "english"
        )) {

            return false;
        }

        return countLatinLetters(
                answerText
        )
                > countDevanagariCharacters(
                answerText
        );
    }

    private static boolean containsAny(
            @NonNull String searchableText,
            @NonNull String[] phrases
    ) {
        for (String phrase :
                phrases) {

            if (searchableText.contains(
                    phrase.toLowerCase(
                            Locale.ROOT
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private static String normalizeText(
            @Nullable String value
    ) {
        String normalized =
                safeText(
                        value
                );

        if (normalized.isEmpty()) {
            return "";
        }

        normalized =
                normalized.replace(
                        '\u0000',
                        ' '
                );

        normalized =
                normalized.replaceAll(
                        "[\\t ]+",
                        " "
                );

        normalized =
                normalized.replaceAll(
                        "\\n{3,}",
                        "\n\n"
                );

        return normalized.trim();
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

    public enum VerificationStatus {

        /**
         * Deterministic अथवा curated local source।
         */
        VERIFIED,

        /**
         * Local checks pass हुए, लेकिन textbook-level external
         * verification अभी उपलब्ध नहीं है।
         */
        HIGH_CONFIDENCE,

        /**
         * Answer उपयोग किया जा सकता है, लेकिन caution जरूरी है।
         */
        CAUTION,

        /**
         * Answer अधूरा, suspicious अथवा unusable है।
         */
        RETRY_RECOMMENDED
    }

    public enum VerificationReason {
        DETERMINISTIC_OFFLINE_MATH,
        DETERMINISTIC_DIVISIBILITY,
        CURATED_OFFLINE_KNOWLEDGE,
        CACHED_PREVIOUS_ANSWER,
        AI_QUALITY_CHECK_PASSED,
        BASIC_QUALITY_CHECK_PASSED,
        LOCAL_SAFETY_OR_FALLBACK,
        SOURCE_NOT_VERIFIED,
        EMPTY_ANSWER,
        ANSWER_TOO_SHORT,
        ANSWER_TOO_LONG,
        EXPLANATION_TOO_BRIEF,
        UNCERTAIN_LANGUAGE,
        FAILURE_OR_REFUSAL_TEXT,
        PROMPT_OR_SYSTEM_LEAKAGE,
        UNSUPPORTED_CERTAINTY,
        INCOMPLETE_ANSWER,
        LANGUAGE_MISMATCH
    }

    public static final class VerificationResult {

        @NonNull
        private final VerificationStatus status;

        @NonNull
        private final VerificationReason reason;

        @NonNull
        private final String studentMessage;

        private VerificationResult(
                @NonNull VerificationStatus status,
                @NonNull VerificationReason reason,
                @NonNull String studentMessage
        ) {
            this.status =
                    status;

            this.reason =
                    reason;

            this.studentMessage =
                    safeText(
                            studentMessage
                    );
        }

        @NonNull
        private static VerificationResult verified(
                @NonNull VerificationReason reason,
                @NonNull String message
        ) {
            return new VerificationResult(
                    VerificationStatus.VERIFIED,
                    reason,
                    message
            );
        }

        @NonNull
        private static VerificationResult highConfidence(
                @NonNull VerificationReason reason,
                @NonNull String message
        ) {
            return new VerificationResult(
                    VerificationStatus.HIGH_CONFIDENCE,
                    reason,
                    message
            );
        }

        @NonNull
        private static VerificationResult caution(
                @NonNull VerificationReason reason,
                @NonNull String message
        ) {
            return new VerificationResult(
                    VerificationStatus.CAUTION,
                    reason,
                    message
            );
        }

        @NonNull
        private static VerificationResult retryRecommended(
                @NonNull VerificationReason reason,
                @NonNull String message
        ) {
            return new VerificationResult(
                    VerificationStatus.RETRY_RECOMMENDED,
                    reason,
                    message
            );
        }

        @NonNull
        public VerificationStatus getStatus() {
            return status;
        }

        @NonNull
        public VerificationReason getReason() {
            return reason;
        }

        @NonNull
        public String getStudentMessage() {
            return studentMessage;
        }

        public boolean isVerified() {
            return status
                    == VerificationStatus.VERIFIED;
        }

        public boolean isHighConfidence() {
            return status
                    == VerificationStatus.HIGH_CONFIDENCE;
        }

        public boolean requiresCaution() {
            return status
                    == VerificationStatus.CAUTION;
        }

        public boolean shouldRetry() {
            return status
                    == VerificationStatus.RETRY_RECOMMENDED;
        }

        public boolean shouldShowStudentMessage() {
            return status
                    == VerificationStatus.CAUTION
                    || status
                    == VerificationStatus.RETRY_RECOMMENDED;
        }

        @NonNull
        public String getDisplayIcon() {
            switch (status) {
                case VERIFIED:
                    return "✓";

                case HIGH_CONFIDENCE:
                    return "●";

                case CAUTION:
                    return "⚠";

                case RETRY_RECOMMENDED:
                default:
                    return "↻";
            }
        }

        @NonNull
        public String getDisplayLabel() {
            switch (status) {
                case VERIFIED:
                    return "सत्यापित";

                case HIGH_CONFIDENCE:
                    return "Quality check पूरा";

                case CAUTION:
                    return "दोबारा जाँचें";

                case RETRY_RECOMMENDED:
                default:
                    return "उत्तर दोबारा तैयार करें";
            }
        }
    }
}