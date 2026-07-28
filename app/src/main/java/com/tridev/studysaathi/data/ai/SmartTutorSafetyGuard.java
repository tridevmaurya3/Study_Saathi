package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Study Saathi Hero Part की reusable child-safety layer।
 *
 * यह class दो स्तरों पर सुरक्षा देती है:
 *
 * 1. Question Safety:
 *    Unsafe अथवा dangerous question को Firebase AI तक जाने से
 *    पहले पहचानकर सुरक्षित educational response देती है।
 *
 * 2. Answer Safety:
 *    Firebase AI अथवा किसी अन्य source से मिले answer में
 *    dangerous procedural instructions दिखाई देने पर उन्हें
 *    safe response से replace करती है।
 *
 * यह class सामान्य educational topics को block नहीं करती।
 * उदाहरण:
 *
 * - "अम्ल क्या है?" एक सामान्य educational question है।
 * - "खतरनाक chemicals को कैसे मिलाएँ?" unsafe procedural request है।
 * - "मानव प्रजनन क्या है?" biology का educational question है।
 * - Explicit अथवा inappropriate request unsafe मानी जाएगी।
 */
public final class SmartTutorSafetyGuard {

    private static final int MAX_SAFE_QUESTION_LENGTH =
            5000;

    @NonNull
    private static final String[] MIXING_ACTION_WORDS = {
            "मिला",
            "मिलाना",
            "मिक्स",
            "mix",
            "combine",
            "together",
            "एक साथ",
            "डाल दें",
            "डालना"
    };

    @NonNull
    private static final String[] DANGEROUS_CHEMICAL_WORDS = {
            "bleach",
            "ब्लीच",
            "toilet cleaner",
            "टॉयलेट क्लीनर",
            "acid",
            "एसिड",
            "ammonia",
            "अमोनिया",
            "chemical",
            "केमिकल",
            "cleaning liquid",
            "सफाई वाला तरल",
            "pesticide",
            "कीटनाशक"
    };

    @NonNull
    private static final String[] WEAPON_WORDS = {
            "weapon",
            "हथियार",
            "gun",
            "बंदूक",
            "bomb",
            "बम",
            "explosive",
            "विस्फोटक",
            "grenade",
            "गोला",
            "ammunition",
            "गोला बारूद"
    };

    @NonNull
    private static final String[] WEAPON_ACTION_WORDS = {
            "बनाना",
            "कैसे बनाएं",
            "कैसे बनाए",
            "तैयार",
            "assemble",
            "build",
            "make",
            "use",
            "इस्तेमाल",
            "चलाना",
            "खरीदना",
            "कहाँ मिलेगा",
            "access"
    };

    @NonNull
    private static final String[] POISON_OR_DRUG_WORDS = {
            "poison",
            "जहर",
            "ज़हर",
            "overdose",
            "ओवरडोज",
            "dangerous drug",
            "खतरनाक दवा",
            "नशीला पदार्थ",
            "recreational drug",
            "toxic substance",
            "विषैला पदार्थ"
    };

    @NonNull
    private static final String[] DANGEROUS_ACTION_WORDS = {
            "कैसे करें",
            "कैसे बनाएं",
            "कैसे बनाए",
            "कैसे इस्तेमाल",
            "कितना लें",
            "कितनी मात्रा",
            "make",
            "build",
            "prepare",
            "use",
            "consume",
            "dose",
            "instructions",
            "steps",
            "method",
            "तरीका",
            "विधि"
    };

    @NonNull
    private static final String[] SELF_HARM_WORDS = {
            "suicide",
            "self harm",
            "खुद को नुकसान",
            "अपने आप को नुकसान",
            "जान देना",
            "मर जाना चाहता",
            "मरना चाहता",
            "मरना चाहती"
    };

    @NonNull
    private static final String[] EXPLICIT_CONTENT_WORDS = {
            "porn",
            "पोर्न",
            "pornography",
            "अश्लील वीडियो",
            "explicit sexual",
            "sex video",
            "सेक्स वीडियो"
    };

    @NonNull
    private static final String[] ACCESS_ACTION_WORDS = {
            "कहाँ मिलेगा",
            "कैसे देखें",
            "कैसे खोलें",
            "डाउनलोड",
            "download",
            "watch",
            "access",
            "website",
            "site",
            "link",
            "लिंक"
    };

    @NonNull
    private static final String[] PERSONAL_DATA_WORDS = {
            "otp",
            "ओटीपी",
            "password",
            "पासवर्ड",
            "pin",
            "पिन",
            "aadhaar",
            "आधार नंबर",
            "bank account",
            "बैंक अकाउंट",
            "card number",
            "कार्ड नंबर",
            "cvv",
            "phone number",
            "फोन नंबर",
            "home address",
            "घर का पता"
    };

    @NonNull
    private static final String[] PERSONAL_DATA_ACTION_WORDS = {
            "share",
            "send",
            "बताओ",
            "भेजो",
            "दे दो",
            "दिखाओ",
            "publish",
            "पोस्ट"
    };

    @NonNull
    private static final String[] ANSWER_INSTRUCTION_MARKERS = {
            "step 1",
            "चरण 1",
            "पहला कदम",
            "सामग्री चाहिए",
            "आपको चाहिए",
            "ingredients",
            "materials required",
            "instructions",
            "विधि",
            "तरीका",
            "फिर इसमें",
            "इसके बाद डालें",
            "mix it",
            "combine it",
            "assemble"
    };

    private SmartTutorSafetyGuard() {
        /*
         * Utility class है।
         */
    }

    /**
     * Student के question की safety जाँचता है।
     */
    @NonNull
    public static SafetyDecision inspectQuestion(
            @Nullable String question,
            @Nullable String explanationLanguage
    ) {
        String normalizedQuestion =
                normalizeText(
                        question
                );

        boolean englishPreferred =
                prefersEnglish(
                        explanationLanguage,
                        normalizedQuestion
                );

        if (normalizedQuestion.isEmpty()) {
            return SafetyDecision.blocked(
                    RiskCategory.EMPTY_QUESTION,
                    englishPreferred
                            ? "Please enter or speak a question first."
                            : "कृपया पहले अपना प्रश्न लिखें या बोलें।",
                    normalizedQuestion
            );
        }

        if (normalizedQuestion.length()
                > MAX_SAFE_QUESTION_LENGTH) {

            return SafetyDecision.blocked(
                    RiskCategory.QUESTION_TOO_LONG,
                    englishPreferred
                            ? "This question is too long. Please ask it in a shorter form."
                            : "यह प्रश्न बहुत लंबा है। कृपया इसे थोड़ा छोटा करके पूछें।",
                    normalizedQuestion.substring(
                            0,
                            MAX_SAFE_QUESTION_LENGTH
                    )
            );
        }

        String searchableQuestion =
                normalizedQuestion.toLowerCase(
                        Locale.ROOT
                );

        if (containsAny(
                searchableQuestion,
                SELF_HARM_WORDS
        )) {
            return SafetyDecision.blocked(
                    RiskCategory.SELF_HARM_RISK,
                    buildSelfHarmSafetyResponse(
                            englishPreferred
                    ),
                    normalizedQuestion
            );
        }

        if (containsAny(
                searchableQuestion,
                EXPLICIT_CONTENT_WORDS
        )
                && containsAny(
                searchableQuestion,
                ACCESS_ACTION_WORDS
        )) {

            return SafetyDecision.blocked(
                    RiskCategory.EXPLICIT_CONTENT,
                    englishPreferred
                            ? "I cannot help access explicit content. I can help with age-appropriate health, biology, or online-safety questions."
                            : "मैं अश्लील सामग्री तक पहुँचने में सहायता नहीं कर सकता। मैं उम्र के अनुसार स्वास्थ्य, जीवविज्ञान या ऑनलाइन सुरक्षा के प्रश्न समझा सकता हूँ।",
                    normalizedQuestion
            );
        }

        if (containsAny(
                searchableQuestion,
                DANGEROUS_CHEMICAL_WORDS
        )
                && containsAny(
                searchableQuestion,
                MIXING_ACTION_WORDS
        )) {

            return SafetyDecision.blocked(
                    RiskCategory.DANGEROUS_CHEMICAL_MIXING,
                    buildChemicalSafetyResponse(
                            englishPreferred
                    ),
                    normalizedQuestion
            );
        }

        if (containsAny(
                searchableQuestion,
                WEAPON_WORDS
        )
                && containsAny(
                searchableQuestion,
                WEAPON_ACTION_WORDS
        )) {

            return SafetyDecision.blocked(
                    RiskCategory.WEAPON_OR_EXPLOSIVE,
                    englishPreferred
                            ? "I cannot provide instructions for making, obtaining, or using weapons or explosives. I can explain their risks, laws, history, or safety at a general educational level."
                            : "मैं हथियार या विस्फोटक बनाने, प्राप्त करने या उपयोग करने की विधि नहीं बता सकता। मैं इनके खतरे, कानून, इतिहास या सामान्य सुरक्षा जानकारी समझा सकता हूँ।",
                    normalizedQuestion
            );
        }

        if (containsAny(
                searchableQuestion,
                POISON_OR_DRUG_WORDS
        )
                && containsAny(
                searchableQuestion,
                DANGEROUS_ACTION_WORDS
        )) {

            return SafetyDecision.blocked(
                    RiskCategory.POISON_OR_DRUG_MISUSE,
                    englishPreferred
                            ? "I cannot provide instructions for using poisons, dangerous drugs, or toxic substances. Move away from the substance and tell a trusted adult immediately if exposure may have occurred."
                            : "मैं जहर, खतरनाक दवा या विषैले पदार्थ के उपयोग की विधि नहीं बता सकता। किसी पदार्थ के संपर्क की आशंका हो तो उससे दूर जाएँ और तुरंत किसी भरोसेमंद बड़े व्यक्ति को बताएँ।",
                    normalizedQuestion
            );
        }

        if (containsAny(
                searchableQuestion,
                PERSONAL_DATA_WORDS
        )
                && containsAny(
                searchableQuestion,
                PERSONAL_DATA_ACTION_WORDS
        )) {

            return SafetyDecision.blocked(
                    RiskCategory.PERSONAL_DATA,
                    englishPreferred
                            ? "Never share passwords, OTPs, PINs, card details, identity numbers, phone numbers, or home addresses in a chat. Ask a trusted adult before sharing personal information."
                            : "चैट में पासवर्ड, OTP, PIN, कार्ड विवरण, पहचान संख्या, फोन नंबर या घर का पता साझा न करें। व्यक्तिगत जानकारी देने से पहले किसी भरोसेमंद बड़े व्यक्ति से पूछें।",
                    normalizedQuestion
            );
        }

        return SafetyDecision.allowed(
                normalizedQuestion
        );
    }

    /**
     * AI अथवा किसी अन्य source से मिले answer की safety जाँचता है।
     *
     * Unsafe procedural content मिलने पर पूरा answer safe message
     * से replace किया जाता है।
     */
    @NonNull
    public static GuardedAnswer inspectAnswer(
            @Nullable String answerText,
            @Nullable String explanationLanguage
    ) {
        String normalizedAnswer =
                normalizeText(
                        answerText
                );

        boolean englishPreferred =
                prefersEnglish(
                        explanationLanguage,
                        normalizedAnswer
                );

        if (normalizedAnswer.isEmpty()) {
            return GuardedAnswer.replaced(
                    englishPreferred
                            ? "The answer could not be prepared. Please try asking the question again."
                            : "उत्तर तैयार नहीं हो सका। कृपया प्रश्न दोबारा पूछें।",
                    RiskCategory.EMPTY_ANSWER
            );
        }

        String searchableAnswer =
                normalizedAnswer.toLowerCase(
                        Locale.ROOT
                );

        boolean containsProceduralInstructions =
                containsAny(
                        searchableAnswer,
                        ANSWER_INSTRUCTION_MARKERS
                );

        if (containsProceduralInstructions
                && containsAny(
                searchableAnswer,
                DANGEROUS_CHEMICAL_WORDS
        )) {

            return GuardedAnswer.replaced(
                    buildChemicalSafetyResponse(
                            englishPreferred
                    ),
                    RiskCategory.DANGEROUS_CHEMICAL_MIXING
            );
        }

        if (containsProceduralInstructions
                && containsAny(
                searchableAnswer,
                WEAPON_WORDS
        )) {

            return GuardedAnswer.replaced(
                    englishPreferred
                            ? "For safety, I cannot provide procedural instructions involving weapons or explosives. I can explain the topic only at a general educational and safety level."
                            : "सुरक्षा के कारण मैं हथियार या विस्फोटक से जुड़ी चरणबद्ध विधि नहीं बता सकता। मैं विषय को केवल सामान्य शैक्षिक और सुरक्षा स्तर पर समझा सकता हूँ।",
                    RiskCategory.WEAPON_OR_EXPLOSIVE
            );
        }

        if (containsProceduralInstructions
                && containsAny(
                searchableAnswer,
                POISON_OR_DRUG_WORDS
        )) {

            return GuardedAnswer.replaced(
                    englishPreferred
                            ? "For safety, I cannot provide instructions involving poisons, dangerous drugs, or toxic substances. Please ask a trusted adult or qualified professional for help."
                            : "सुरक्षा के कारण मैं जहर, खतरनाक दवा या विषैले पदार्थ से जुड़ी विधि नहीं बता सकता। सहायता के लिए किसी भरोसेमंद बड़े व्यक्ति या योग्य विशेषज्ञ से बात करें।",
                    RiskCategory.POISON_OR_DRUG_MISUSE
            );
        }

        if (containsAny(
                searchableAnswer,
                EXPLICIT_CONTENT_WORDS
        )
                && containsAny(
                searchableAnswer,
                ACCESS_ACTION_WORDS
        )) {

            return GuardedAnswer.replaced(
                    englishPreferred
                            ? "This content is not appropriate for a school learning assistant. I can provide age-appropriate health, biology, and online-safety information."
                            : "यह सामग्री school learning assistant के लिए उपयुक्त नहीं है। मैं उम्र के अनुसार स्वास्थ्य, जीवविज्ञान और ऑनलाइन सुरक्षा जानकारी दे सकता हूँ।",
                    RiskCategory.EXPLICIT_CONTENT
            );
        }

        return GuardedAnswer.allowed(
                normalizedAnswer
        );
    }

    /**
     * Firebase AI prompt में जोड़ने के लिए child-safe instruction।
     */
    @NonNull
    public static String buildAiSafetyInstruction(
            @Nullable String explanationLanguage
    ) {
        String normalizedLanguage =
                safeText(
                        explanationLanguage
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (normalizedLanguage.contains(
                "english"
        )) {
            return "Give an age-appropriate school-level answer. "
                    + "Do not provide dangerous, illegal, explicit, or harmful procedural instructions. "
                    + "Correct false assumptions politely. "
                    + "Clearly state uncertainty when the answer cannot be verified. "
                    + "Never request passwords, OTPs, financial details, identity numbers, phone numbers, or home addresses.";
        }

        return "उत्तर बच्चे की उम्र और school level के अनुसार दें। "
                + "खतरनाक, अवैध, अश्लील या नुकसान पहुँचाने वाली चरणबद्ध विधि न दें। "
                + "गलत धारणा को विनम्रता से सुधारें। "
                + "उत्तर सत्यापित न हो सके तो अनिश्चितता साफ बताएँ। "
                + "पासवर्ड, OTP, वित्तीय विवरण, पहचान संख्या, फोन नंबर या घर का पता कभी न माँगें।";
    }

    @NonNull
    private static String buildChemicalSafetyResponse(
            boolean englishPreferred
    ) {
        if (englishPreferred) {
            return "Do not mix these substances. Mixing household cleaners can create harmful fumes. Move away from the area, tell a trusted adult immediately, and keep the area safely ventilated without touching or testing the mixture.";
        }

        return "इन पदार्थों को कभी न मिलाएँ। घरेलू cleaners को मिलाने से हानिकारक गैस बन सकती है। वहाँ से दूर जाएँ, तुरंत किसी भरोसेमंद बड़े व्यक्ति को बताएँ और मिश्रण को छुएँ या जाँचें बिना जगह को सुरक्षित रूप से हवादार रखें।";
    }

    @NonNull
    private static String buildSelfHarmSafetyResponse(
            boolean englishPreferred
    ) {
        if (englishPreferred) {
            return "Your safety matters most. Tell a trusted adult such as a parent, guardian, teacher, or school counsellor right now, and do not stay alone. If there is immediate danger, contact local emergency services.";
        }

        return "आपकी सुरक्षा सबसे जरूरी है। अभी माता-पिता, अभिभावक, शिक्षक या school counsellor जैसे किसी भरोसेमंद बड़े व्यक्ति को बताएँ और अकेले न रहें। तत्काल खतरा हो तो स्थानीय emergency service से संपर्क करें।";
    }

    private static boolean prefersEnglish(
            @Nullable String explanationLanguage,
            @NonNull String text
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
                        || language.contains(
                        "अंग्रेज"
                );

        boolean explicitlyHindi =
                language.contains(
                        "hindi"
                )
                        || language.contains(
                        "हिंदी"
                );

        if (explicitlyEnglish
                && !explicitlyHindi) {

            return true;
        }

        if (explicitlyHindi
                && !explicitlyEnglish) {

            return false;
        }

        return !containsDevanagari(
                text
        );
    }

    private static boolean containsDevanagari(
            @NonNull String text
    ) {
        for (int index = 0;
             index < text.length();
             index++) {

            char character =
                    text.charAt(
                            index
                    );

            if (character >= '\u0900'
                    && character <= '\u097F') {

                return true;
            }
        }

        return false;
    }

    private static boolean containsAny(
            @NonNull String searchableText,
            @NonNull String[] keywords
    ) {
        for (String keyword : keywords) {
            if (searchableText.contains(
                    keyword.toLowerCase(
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
            @Nullable String text
    ) {
        String normalized =
                safeText(
                        text
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

    /**
     * Question/answer का detected risk type।
     */
    public enum RiskCategory {
        SAFE,
        EMPTY_QUESTION,
        QUESTION_TOO_LONG,
        EMPTY_ANSWER,
        DANGEROUS_CHEMICAL_MIXING,
        WEAPON_OR_EXPLOSIVE,
        POISON_OR_DRUG_MISUSE,
        SELF_HARM_RISK,
        EXPLICIT_CONTENT,
        PERSONAL_DATA
    }

    /**
     * Question Firebase AI तक भेजना है या local safe response देना है।
     */
    public static final class SafetyDecision {

        private final boolean allowed;

        @NonNull
        private final RiskCategory riskCategory;

        @NonNull
        private final String safeResponse;

        @NonNull
        private final String normalizedQuestion;

        private SafetyDecision(
                boolean allowed,
                @NonNull RiskCategory riskCategory,
                @NonNull String safeResponse,
                @NonNull String normalizedQuestion
        ) {
            this.allowed =
                    allowed;

            this.riskCategory =
                    riskCategory;

            this.safeResponse =
                    safeResponse;

            this.normalizedQuestion =
                    normalizedQuestion;
        }

        @NonNull
        private static SafetyDecision allowed(
                @NonNull String normalizedQuestion
        ) {
            return new SafetyDecision(
                    true,
                    RiskCategory.SAFE,
                    "",
                    normalizedQuestion
            );
        }

        @NonNull
        private static SafetyDecision blocked(
                @NonNull RiskCategory riskCategory,
                @NonNull String safeResponse,
                @NonNull String normalizedQuestion
        ) {
            return new SafetyDecision(
                    false,
                    riskCategory,
                    safeResponse,
                    normalizedQuestion
            );
        }

        public boolean isAllowed() {
            return allowed;
        }

        public boolean shouldBypassRemoteAi() {
            return !allowed;
        }

        @NonNull
        public RiskCategory getRiskCategory() {
            return riskCategory;
        }

        @NonNull
        public String getSafeResponse() {
            return safeResponse;
        }

        @NonNull
        public String getNormalizedQuestion() {
            return normalizedQuestion;
        }
    }

    /**
     * Final answer दिखाया जा सकता है या safety response से बदला गया है।
     */
    public static final class GuardedAnswer {

        private final boolean replacedForSafety;

        @NonNull
        private final String answerText;

        @NonNull
        private final RiskCategory riskCategory;

        private GuardedAnswer(
                boolean replacedForSafety,
                @NonNull String answerText,
                @NonNull RiskCategory riskCategory
        ) {
            this.replacedForSafety =
                    replacedForSafety;

            this.answerText =
                    answerText;

            this.riskCategory =
                    riskCategory;
        }

        @NonNull
        private static GuardedAnswer allowed(
                @NonNull String answerText
        ) {
            return new GuardedAnswer(
                    false,
                    answerText,
                    RiskCategory.SAFE
            );
        }

        @NonNull
        private static GuardedAnswer replaced(
                @NonNull String safeAnswer,
                @NonNull RiskCategory riskCategory
        ) {
            return new GuardedAnswer(
                    true,
                    safeAnswer,
                    riskCategory
            );
        }

        public boolean wasReplacedForSafety() {
            return replacedForSafety;
        }

        @NonNull
        public String getAnswerText() {
            return answerText;
        }

        @NonNull
        public RiskCategory getRiskCategory() {
            return riskCategory;
        }
    }
}