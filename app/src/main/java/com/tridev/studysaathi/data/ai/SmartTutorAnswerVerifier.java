package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Reusable local quality-verification layer for Study Saathi answers.
 *
 * Stage 5 keeps the existing public API while making the answer-language
 * contract explicit for English, Hindi and Hinglish.
 */
public final class SmartTutorAnswerVerifier {

    private static final int MINIMUM_USEFUL_ANSWER_LENGTH = 12;
    private static final int MINIMUM_DETAILED_ANSWER_LENGTH = 35;
    private static final int MAXIMUM_DISPLAY_ANSWER_LENGTH = 30000;

    @NonNull
    private static final String[] UNCERTAINTY_PHRASES = {
            "शायद", "संभवतः", "हो सकता है", "हो सकती है", "मुझे लगता है",
            "पक्का नहीं", "निश्चित नहीं", "जानकारी स्पष्ट नहीं",
            "may be", "maybe", "possibly", "probably", "i think",
            "i am not sure", "not certain", "cannot confirm", "could be"
    };

    @NonNull
    private static final String[] FAILURE_OR_REFUSAL_PHRASES = {
            "उत्तर नहीं दे सकता", "उत्तर नहीं दे सकती", "मुझे जानकारी नहीं है",
            "मैं इसे समझ नहीं पाया", "मैं इसे समझ नहीं पाई", "प्रश्न स्पष्ट नहीं है",
            "दोबारा प्रयास करें", "कुछ गलत हो गया", "response प्राप्त नहीं",
            "answer खाली है", "as an ai language model", "i cannot answer",
            "i cannot help", "i do not know", "i don't know", "unable to answer",
            "try again", "something went wrong", "no response"
    };

    @NonNull
    private static final String[] PROMPT_LEAKAGE_PHRASES = {
            "system prompt", "developer message", "hidden instruction",
            "internal instruction", "ignore previous instructions",
            "ignore all instructions", "begin previous conversation context",
            "end previous conversation context", "student context",
            "teaching rules", "current student question",
            "language instruction", "child safety and accuracy rules"
    };

    @NonNull
    private static final String[] UNSUPPORTED_CERTAINTY_PHRASES = {
            "100% सही", "सौ प्रतिशत सही", "पूरी गारंटी", "हमेशा बिल्कुल सही",
            "कभी गलत नहीं", "100% correct", "completely guaranteed",
            "always correct", "never wrong", "definitely true in every case"
    };

    @NonNull
    private static final String[] INCOMPLETE_ENDINGS = {
            ":", ",", ";", "-", "और", "या", "because", "and", "or",
            "such as", "for example"
    };

    @NonNull
    private static final String[] HINGLISH_OVERRIDE_PHRASES = {
            "in hinglish", "hinglish mein", "hinglish me", "हिंग्लिश में",
            "हिंग्लिश मे", "hindi english mix", "hindi + english",
            "hindi+english", "हिंदी इंग्लिश मिक्स", "हिन्दी इंग्लिश मिक्स"
    };

    @NonNull
    private static final String[] ENGLISH_OVERRIDE_PHRASES = {
            "in english", "english mein", "english me", "english please",
            "अंग्रेजी में", "अंग्रेज़ी में", "इंग्लिश में", "इंग्लिश मे"
    };

    @NonNull
    private static final String[] HINDI_OVERRIDE_PHRASES = {
            "in hindi", "hindi mein", "hindi me", "hindi please",
            "हिंदी में", "हिन्दी में", "हिंदी मे", "हिन्दी मे"
    };

    private SmartTutorAnswerVerifier() { }

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

    @NonNull
    public static VerificationResult verify(
            @Nullable String answerText,
            @Nullable SmartTutorAnswerResult.AnswerSource answerSource,
            boolean alreadyVerified,
            @Nullable String question,
            @Nullable String subjectName,
            @Nullable String explanationLanguage
    ) {
        String normalizedAnswer = normalizeText(answerText);
        String normalizedQuestion = normalizeText(question);
        String normalizedSubject = normalizeText(subjectName);
        LanguageMode languageMode =
                resolveEffectiveLanguageMode(explanationLanguage, normalizedQuestion);

        SmartTutorAnswerResult.AnswerSource safeSource =
                answerSource == null
                        ? SmartTutorAnswerResult.AnswerSource.UNKNOWN
                        : answerSource;

        if (normalizedAnswer.isEmpty()) {
            return VerificationResult.retryRecommended(
                    VerificationReason.EMPTY_ANSWER,
                    studentMessage(
                            languageMode,
                            "The answer is empty. Please ask the question again.",
                            "उत्तर खाली है। कृपया प्रश्न दोबारा पूछें।",
                            "Answer खाली है। Question दोबारा पूछें।"
                    )
            );
        }

        if (normalizedAnswer.length() > MAXIMUM_DISPLAY_ANSWER_LENGTH) {
            return VerificationResult.caution(
                    VerificationReason.ANSWER_TOO_LONG,
                    studentMessage(
                            languageMode,
                            "This answer is unusually long. Check the important points before using it.",
                            "यह उत्तर असामान्य रूप से लंबा है। उपयोग करने से पहले मुख्य बिंदुओं की जाँच करें।",
                            "यह answer unusually long है। Use करने से पहले important points check करें।"
                    )
            );
        }

        String searchableAnswer = normalizedAnswer.toLowerCase(Locale.ROOT);

        if (containsAny(searchableAnswer, PROMPT_LEAKAGE_PHRASES)) {
            return VerificationResult.retryRecommended(
                    VerificationReason.PROMPT_OR_SYSTEM_LEAKAGE,
                    studentMessage(
                            languageMode,
                            "This answer contains internal instruction text. Please generate the answer again.",
                            "इस उत्तर में आंतरिक निर्देश दिखाई दे रहे हैं। कृपया उत्तर दोबारा तैयार करें।",
                            "इस answer में internal instruction text आ गया है। Answer दोबारा generate करें।"
                    )
            );
        }

        if (normalizedAnswer.length() < MINIMUM_USEFUL_ANSWER_LENGTH) {
            return VerificationResult.retryRecommended(
                    VerificationReason.ANSWER_TOO_SHORT,
                    studentMessage(
                            languageMode,
                            "The answer is too short to be useful. Please ask for a complete explanation.",
                            "उत्तर उपयोगी होने के लिए बहुत छोटा है। कृपया पूरी व्याख्या माँगें।",
                            "Answer बहुत short है। Complete explanation माँगें।"
                    )
            );
        }

        if (containsAny(searchableAnswer, FAILURE_OR_REFUSAL_PHRASES)) {
            return VerificationResult.retryRecommended(
                    VerificationReason.FAILURE_OR_REFUSAL_TEXT,
                    studentMessage(
                            languageMode,
                            "A complete educational answer was not produced. Please retry or rephrase the question.",
                            "पूरा शैक्षिक उत्तर तैयार नहीं हुआ। कृपया प्रश्न दोबारा या दूसरे तरीके से पूछें।",
                            "Complete learning answer नहीं बना। Question retry या rephrase करें।"
                    )
            );
        }

        if (containsAny(searchableAnswer, UNSUPPORTED_CERTAINTY_PHRASES)) {
            return VerificationResult.caution(
                    VerificationReason.UNSUPPORTED_CERTAINTY,
                    studentMessage(
                            languageMode,
                            "The answer makes an absolute claim. Check it with the textbook or teacher.",
                            "उत्तर पूर्ण निश्चितता का दावा कर रहा है। इसे पुस्तक या शिक्षक से जाँचें।",
                            "Answer absolute certainty claim कर रहा है। Textbook या teacher से check करें।"
                    )
            );
        }

        if (containsAny(searchableAnswer, UNCERTAINTY_PHRASES)) {
            return VerificationResult.caution(
                    VerificationReason.UNCERTAIN_LANGUAGE,
                    studentMessage(
                            languageMode,
                            "The answer contains uncertainty. Check the textbook page, question image, or a trusted source.",
                            "उत्तर में अनिश्चितता है। पुस्तक के पृष्ठ, प्रश्न की तस्वीर या किसी भरोसेमंद स्रोत से जाँच करें।",
                            "Answer में uncertainty है। Textbook page, question image या trusted source से check करें।"
                    )
            );
        }

        if (looksIncomplete(normalizedAnswer)) {
            return VerificationResult.retryRecommended(
                    VerificationReason.INCOMPLETE_ANSWER,
                    studentMessage(
                            languageMode,
                            "The answer appears incomplete. Please generate it again.",
                            "उत्तर अधूरा दिखाई दे रहा है। कृपया इसे दोबारा तैयार करें।",
                            "Answer incomplete लग रहा है। इसे दोबारा generate करें।"
                    )
            );
        }

        if (isLanguageMismatch(normalizedAnswer, languageMode)) {
            return VerificationResult.caution(
                    VerificationReason.LANGUAGE_MISMATCH,
                    studentMessage(
                            languageMode,
                            "The answer language does not fully match the selected English mode.",
                            "उत्तर की भाषा चुने गए हिन्दी मोड से पूरी तरह मेल नहीं खाती।",
                            "Answer natural Hinglish mode से पूरी तरह match नहीं करता।"
                    )
            );
        }

        /*
         * Deterministic and curated sources stay verified, but only after
         * the language-contract check so a wrong-language local answer is
         * never silently marked as fully verified.
         */
        if (safeSource == SmartTutorAnswerResult.AnswerSource.OFFLINE_BASIC_MATH) {
            return VerificationResult.verified(
                    VerificationReason.DETERMINISTIC_OFFLINE_MATH,
                    studentMessage(
                            languageMode,
                            "Verified by the offline Mathematics engine.",
                            "ऑफलाइन गणित इंजन द्वारा सत्यापित।",
                            "Offline Mathematics engine से verified।"
                    )
            );
        }

        if (safeSource == SmartTutorAnswerResult.AnswerSource.OFFLINE_DIVISIBILITY) {
            return VerificationResult.verified(
                    VerificationReason.DETERMINISTIC_DIVISIBILITY,
                    studentMessage(
                            languageMode,
                            "Verified by the offline divisibility engine.",
                            "ऑफलाइन विभाज्यता इंजन द्वारा सत्यापित।",
                            "Offline divisibility engine से verified।"
                    )
            );
        }

        if (safeSource == SmartTutorAnswerResult.AnswerSource.VERIFIED_OFFLINE_KNOWLEDGE
                || alreadyVerified) {
            return VerificationResult.verified(
                    VerificationReason.CURATED_OFFLINE_KNOWLEDGE,
                    studentMessage(
                            languageMode,
                            "Matched with verified offline study content.",
                            "सत्यापित ऑफलाइन अध्ययन सामग्री से मिलान किया गया।",
                            "Verified offline study content से match किया गया।"
                    )
            );
        }

        if (normalizedAnswer.length() < MINIMUM_DETAILED_ANSWER_LENGTH
                && isExplanationQuestion(normalizedQuestion)) {
            return VerificationResult.caution(
                    VerificationReason.EXPLANATION_TOO_BRIEF,
                    studentMessage(
                            languageMode,
                            "The answer may be correct, but the explanation is very brief.",
                            "उत्तर सही हो सकता है, लेकिन व्याख्या बहुत संक्षिप्त है।",
                            "Answer सही हो सकता है, लेकिन explanation बहुत brief है।"
                    )
            );
        }

        if (safeSource == SmartTutorAnswerResult.AnswerSource.PERSISTENT_CACHE) {
            return VerificationResult.highConfidence(
                    VerificationReason.CACHED_PREVIOUS_ANSWER,
                    studentMessage(
                            languageMode,
                            "This is a previously saved answer. Recheck it when the textbook or chapter changes.",
                            "यह पहले से सुरक्षित उत्तर है। पुस्तक या अध्याय बदलने पर इसे दोबारा जाँचें।",
                            "यह previously saved answer है। Textbook या chapter बदलने पर recheck करें।"
                    )
            );
        }

        if (safeSource == SmartTutorAnswerResult.AnswerSource.FIREBASE_AI) {
            return VerificationResult.highConfidence(
                    VerificationReason.AI_QUALITY_CHECK_PASSED,
                    studentMessage(
                            languageMode,
                            "The answer passed local quality checks. Important facts should still be checked with the textbook.",
                            "उत्तर ने स्थानीय गुणवत्ता जाँच पूरी की है। महत्वपूर्ण तथ्यों को फिर भी पुस्तक से जाँचें।",
                            "Answer ने local quality checks pass किए हैं। Important facts को textbook से भी check करें।"
                    )
            );
        }

        if (safeSource == SmartTutorAnswerResult.AnswerSource.LOCAL_FALLBACK) {
            return VerificationResult.highConfidence(
                    VerificationReason.LOCAL_SAFETY_OR_FALLBACK,
                    studentMessage(
                            languageMode,
                            "This is a local safety or fallback response.",
                            "यह स्थानीय सुरक्षा या वैकल्पिक उत्तर है।",
                            "यह local safety या fallback response है।"
                    )
            );
        }

        if (!normalizedSubject.isEmpty()
                && normalizedAnswer.length() >= MINIMUM_DETAILED_ANSWER_LENGTH) {
            return VerificationResult.highConfidence(
                    VerificationReason.BASIC_QUALITY_CHECK_PASSED,
                    studentMessage(
                            languageMode,
                            "The answer passed basic local quality checks.",
                            "उत्तर ने मूल स्थानीय गुणवत्ता जाँच पूरी की है।",
                            "Answer ने basic local quality checks pass किए हैं।"
                    )
            );
        }

        return VerificationResult.caution(
                VerificationReason.SOURCE_NOT_VERIFIED,
                studentMessage(
                        languageMode,
                        "The answer source could not be fully verified.",
                        "उत्तर के स्रोत को पूरी तरह सत्यापित नहीं किया जा सका।",
                        "Answer source पूरी तरह verify नहीं हो सका।"
                )
        );
    }

    /**
     * Instruction inserted into the existing Gemini prompt.
     *
     * The current question itself remains in the prompt, so this contract
     * can safely honor a one-turn explicit language request without changing
     * the saved student profile.
     */
    @NonNull
    public static String buildAiVerificationInstruction(
            @Nullable String explanationLanguage
    ) {
        LanguageMode storedMode =
                resolveStoredLanguageMode(explanationLanguage);

        StringBuilder instruction = new StringBuilder();
        instruction.append(
                "Before returning the final answer, silently verify the calculation, "
                        + "factual consistency, class-level suitability, and whether the answer "
                        + "directly addresses the current question. "
                        + "Do not claim 100% certainty without evidence. "
                        + "If important information is unclear or missing, say so and request "
                        + "the relevant textbook page or a clearer question image.\n"
        );

        instruction.append(
                "MANDATORY LANGUAGE CONTRACT: This contract overrides any looser or generic "
                        + "language wording elsewhere in this prompt, even if that wording appears later. "
        );

        switch (storedMode) {
            case ENGLISH:
                instruction.append(
                        "Default to English: write the complete student-facing answer in natural, "
                                + "class-appropriate English. Do not add Hindi/Devanagari explanation "
                                + "or a duplicate translated section. Standard formulas, symbols, names "
                                + "and accepted abbreviations are allowed. "
                );
                break;

            case HINDI:
                instruction.append(
                        "Default to Hindi: write the complete student-facing answer in simple, natural "
                                + "Devanagari Hindi. Do not add English sentences or a separate English "
                                + "translation section. Use formulas, symbols, proper names or unavoidable "
                                + "standard abbreviations only when genuinely necessary. Prefer clear Hindi "
                                + "school vocabulary instead of unnecessary English terms. "
                );
                break;

            case HINGLISH:
            default:
                instruction.append(
                        "Default to Hinglish: write one natural blended explanation using easy Hindi "
                                + "sentence flow with familiar English academic terms where they help. "
                                + "Do not output a full English section followed by a full Hindi section. "
                                + "Do not put every English term in brackets. Keep it conversational, "
                                + "clear and school-appropriate. "
                );
                break;
        }

        instruction.append(
                "If the CURRENT STUDENT QUESTION explicitly asks for English, Hindi or Hinglish "
                        + "(for example 'explain in Hindi'), follow that requested language for this "
                        + "answer only. That one-turn request overrides the stored preference for this "
                        + "answer and must not be treated as a permanent profile-language change."
        );

        return instruction.toString();
    }

    @NonNull
    public static String buildAnswerWithVerificationNote(
            @Nullable String answerText,
            @NonNull VerificationResult verificationResult
    ) {
        String safeAnswer = normalizeText(answerText);

        if (safeAnswer.isEmpty()) {
            return verificationResult.getStudentMessage();
        }

        if (!verificationResult.shouldShowStudentMessage()) {
            return safeAnswer;
        }

        String message = verificationResult.getStudentMessage();
        if (message.isEmpty()) {
            return safeAnswer;
        }

        return safeAnswer
                + "\n\n"
                + verificationResult.getDisplayIcon()
                + " "
                + message;
    }

    private static boolean isExplanationQuestion(@NonNull String question) {
        String searchableQuestion = question.toLowerCase(Locale.ROOT);
        return searchableQuestion.contains("समझाओ")
                || searchableQuestion.contains("समझाएँ")
                || searchableQuestion.contains("व्याख्या")
                || searchableQuestion.contains("कैसे")
                || searchableQuestion.contains("क्यों")
                || searchableQuestion.contains("वर्णन")
                || searchableQuestion.contains("explain")
                || searchableQuestion.contains("describe")
                || searchableQuestion.contains("how")
                || searchableQuestion.contains("why");
    }

    private static boolean looksIncomplete(@NonNull String answerText) {
        String normalized = answerText.trim().toLowerCase(Locale.ROOT);

        for (String incompleteEnding : INCOMPLETE_ENDINGS) {
            if (normalized.endsWith(incompleteEnding.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return countCharacter(normalized, '(') != countCharacter(normalized, ')')
                || countCharacter(normalized, '[') != countCharacter(normalized, ']');
    }

    private static int countCharacter(
            @NonNull String text,
            char targetCharacter
    ) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == targetCharacter) {
                count++;
            }
        }
        return count;
    }

    private static boolean isLanguageMismatch(
            @NonNull String answerText,
            @NonNull LanguageMode languageMode
    ) {
        String lower = answerText.toLowerCase(Locale.ROOT);

        if (languageMode == LanguageMode.HINGLISH) {
            boolean englishSection =
                    lower.contains("english:")
                            || lower.contains("english :");
            boolean hindiSection =
                    lower.contains("hindi:")
                            || lower.contains("hindi :")
                            || answerText.contains("हिंदी:")
                            || answerText.contains("हिन्दी:")
                            || answerText.contains("हिंदी :")
                            || answerText.contains("हिन्दी :");
            return englishSection && hindiSection;
        }

        int devanagariCount = countDevanagariCharacters(answerText);
        int latinLetterCount = countLatinLetters(answerText);

        if (languageMode == LanguageMode.ENGLISH) {
            return devanagariCount > 12
                    && devanagariCount > latinLetterCount / 2;
        }

        /*
         * Hindi mode tolerates formulas, proper names and small abbreviations,
         * but not an answer that is effectively an English explanation.
         */
        return latinLetterCount > 50
                && (devanagariCount < 12
                || latinLetterCount > devanagariCount * 2);
    }

    @NonNull
    private static LanguageMode resolveEffectiveLanguageMode(
            @Nullable String explanationLanguage,
            @Nullable String currentQuestion
    ) {
        String question = normalizeText(currentQuestion).toLowerCase(Locale.ROOT);

        if (containsAny(question, HINGLISH_OVERRIDE_PHRASES)) {
            return LanguageMode.HINGLISH;
        }
        if (containsAny(question, ENGLISH_OVERRIDE_PHRASES)) {
            return LanguageMode.ENGLISH;
        }
        if (containsAny(question, HINDI_OVERRIDE_PHRASES)) {
            return LanguageMode.HINDI;
        }

        return resolveStoredLanguageMode(explanationLanguage);
    }

    @NonNull
    private static LanguageMode resolveStoredLanguageMode(
            @Nullable String explanationLanguage
    ) {
        String language =
                safeText(explanationLanguage).toLowerCase(Locale.ROOT);

        if (language.contains("hinglish")
                || language.contains("bilingual")
                || language.contains("hi,en")
                || language.contains("hindi + english")
                || language.contains("hindi+english")
                || (language.contains("hindi") && language.contains("english"))) {
            return LanguageMode.HINGLISH;
        }

        if ((language.contains("english") || language.equals("en"))
                && !language.contains("hindi")) {
            return LanguageMode.ENGLISH;
        }

        if (language.contains("hindi")
                || language.equals("hi")
                || language.contains("हिंदी")
                || language.contains("हिन्दी")) {
            return LanguageMode.HINDI;
        }

        return LanguageMode.HINGLISH;
    }

    @NonNull
    private static String studentMessage(
            @NonNull LanguageMode mode,
            @NonNull String english,
            @NonNull String hindi,
            @NonNull String hinglish
    ) {
        switch (mode) {
            case ENGLISH:
                return english;
            case HINDI:
                return hindi;
            case HINGLISH:
            default:
                return hinglish;
        }
    }

    private static int countDevanagariCharacters(@NonNull String text) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character >= '\u0900' && character <= '\u097F') {
                count++;
            }
        }
        return count;
    }

    private static int countLatinLetters(@NonNull String text) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if ((character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')) {
                count++;
            }
        }
        return count;
    }

    private static boolean containsAny(
            @NonNull String searchableText,
            @NonNull String[] phrases
    ) {
        for (String phrase : phrases) {
            if (searchableText.contains(phrase.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String normalizeText(@Nullable String value) {
        String normalized = safeText(value);
        if (normalized.isEmpty()) {
            return "";
        }

        return normalized
                .replace('\u0000', ' ')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    @NonNull
    private static String safeText(@Nullable Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private enum LanguageMode {
        ENGLISH,
        HINDI,
        HINGLISH
    }

    public enum VerificationStatus {
        VERIFIED,
        HIGH_CONFIDENCE,
        CAUTION,
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
            this.status = status;
            this.reason = reason;
            this.studentMessage = safeText(studentMessage);
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
            return status == VerificationStatus.VERIFIED;
        }

        public boolean isHighConfidence() {
            return status == VerificationStatus.HIGH_CONFIDENCE;
        }

        public boolean requiresCaution() {
            return status == VerificationStatus.CAUTION;
        }

        public boolean shouldRetry() {
            return status == VerificationStatus.RETRY_RECOMMENDED;
        }

        public boolean shouldShowStudentMessage() {
            return status == VerificationStatus.CAUTION
                    || status == VerificationStatus.RETRY_RECOMMENDED;
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
