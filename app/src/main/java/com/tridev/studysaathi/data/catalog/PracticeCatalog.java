package com.tridev.studysaathi.data.catalog;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.model.PracticeQuestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PracticeCatalog {

    private PracticeCatalog() {
        // Utility class.
    }

    @NonNull
    public static List<PracticeQuestion> getQuestions(
            String subjectName,
            String chapterTitle
    ) {
        String normalizedTitle = normalizeText(chapterTitle);

        switch (normalizedTitle) {
            case "fractions":
                return getFractionsQuestions();

            case "food and nutrition":
                return getFoodAndNutritionQuestions();

            case "reading skills":
                return getReadingSkillsQuestions();

            case "हिंदी व्याकरण":
                return getHindiGrammarQuestions();

            case "understanding history":
                return getHistoryQuestions();

            case "computer fundamentals":
                return getComputerQuestions();

            default:
                return getGenericQuestions(
                        getSafeText(subjectName, "Subject"),
                        getSafeText(chapterTitle, "Chapter")
                );
        }
    }

    @NonNull
    private static List<PracticeQuestion>
    getFractionsQuestions() {

        List<PracticeQuestion> questions =
                new ArrayList<>();

        questions.add(
                createQuestion(
                        "What does the numerator tell us?",
                        "अंश हमें क्या बताता है?",
                        new String[]{
                                "Total equal parts",
                                "Selected parts",
                                "Size of the object",
                                "Name of the fraction"
                        },
                        new String[]{
                                "कुल बराबर भाग",
                                "चुने गए भाग",
                                "वस्तु का आकार",
                                "भिन्न का नाम"
                        },
                        1,
                        "The numerator is the upper number. It tells us how many equal parts have been selected.",
                        "अंश ऊपर लिखी संख्या है। यह बताता है कि कुल बराबर भागों में से कितने भाग लिए गए हैं।"
                )
        );

        questions.add(
                createQuestion(
                        "What is the denominator in 3/8?",
                        "3/8 में हर कौन-सी संख्या है?",
                        new String[]{
                                "3",
                                "5",
                                "8",
                                "11"
                        },
                        new String[]{
                                "3",
                                "5",
                                "8",
                                "11"
                        },
                        2,
                        "In 3/8, the lower number 8 is the denominator.",
                        "3/8 में नीचे लिखी संख्या 8 हर है।"
                )
        );

        questions.add(
                createQuestion(
                        "A pizza has 6 equal slices. Aman eats 2 slices. What fraction did he eat?",
                        "एक पिज़्ज़ा के 6 बराबर टुकड़े हैं। अमन 2 टुकड़े खाता है। उसने कितना भाग खाया?",
                        new String[]{
                                "2/6",
                                "6/2",
                                "2/4",
                                "4/6"
                        },
                        new String[]{
                                "2/6",
                                "6/2",
                                "2/4",
                                "4/6"
                        },
                        0,
                        "Two slices were selected from six equal slices, so the fraction is 2/6.",
                        "6 बराबर टुकड़ों में से 2 टुकड़े लिए गए, इसलिए भिन्न 2/6 है।"
                )
        );

        questions.add(
                createQuestion(
                        "Which pair contains like fractions?",
                        "कौन-सा जोड़ा समहर भिन्नों का है?",
                        new String[]{
                                "2/5 and 3/5",
                                "1/2 and 1/3",
                                "3/4 and 5/6",
                                "2/7 and 4/9"
                        },
                        new String[]{
                                "2/5 और 3/5",
                                "1/2 और 1/3",
                                "3/4 और 5/6",
                                "2/7 और 4/9"
                        },
                        0,
                        "Like fractions have the same denominator. Both 2/5 and 3/5 have denominator 5.",
                        "समहर भिन्नों के हर समान होते हैं। 2/5 और 3/5 दोनों का हर 5 है।"
                )
        );

        return questions;
    }

    @NonNull
    private static List<PracticeQuestion>
    getFoodAndNutritionQuestions() {

        List<PracticeQuestion> questions =
                new ArrayList<>();

        questions.add(
                createQuestion(
                        "Which nutrient mainly gives energy to the body?",
                        "कौन-सा पोषक तत्त्व शरीर को मुख्य रूप से ऊर्जा देता है?",
                        new String[]{
                                "Carbohydrates",
                                "Proteins",
                                "Vitamins",
                                "Minerals"
                        },
                        new String[]{
                                "कार्बोहाइड्रेट",
                                "प्रोटीन",
                                "विटामिन",
                                "खनिज"
                        },
                        0,
                        "Carbohydrates are the body's main source of energy.",
                        "कार्बोहाइड्रेट शरीर के लिए ऊर्जा का मुख्य स्रोत हैं।"
                )
        );

        questions.add(
                createQuestion(
                        "Which nutrient helps in growth and repair?",
                        "कौन-सा पोषक तत्त्व वृद्धि और शरीर की मरम्मत में सहायता करता है?",
                        new String[]{
                                "Fats",
                                "Proteins",
                                "Water",
                                "Carbohydrates"
                        },
                        new String[]{
                                "वसा",
                                "प्रोटीन",
                                "पानी",
                                "कार्बोहाइड्रेट"
                        },
                        1,
                        "Proteins help the body grow and repair damaged tissues.",
                        "प्रोटीन शरीर की वृद्धि और क्षतिग्रस्त ऊतकों की मरम्मत में सहायता करते हैं।"
                )
        );

        questions.add(
                createQuestion(
                        "What is a balanced diet?",
                        "संतुलित आहार क्या है?",
                        new String[]{
                                "Only fruits",
                                "Only proteins",
                                "All nutrients in proper amounts",
                                "Only carbohydrates"
                        },
                        new String[]{
                                "केवल फल",
                                "केवल प्रोटीन",
                                "सभी पोषक तत्त्व उचित मात्रा में",
                                "केवल कार्बोहाइड्रेट"
                        },
                        2,
                        "A balanced diet contains all required nutrients in suitable amounts.",
                        "संतुलित आहार में सभी आवश्यक पोषक तत्त्व उचित मात्रा में होते हैं।"
                )
        );

        questions.add(
                createQuestion(
                        "Why is roughage important?",
                        "रेशेदार भोजन क्यों आवश्यक है?",
                        new String[]{
                                "It improves digestion",
                                "It changes blood colour",
                                "It makes bones shorter",
                                "It replaces water"
                        },
                        new String[]{
                                "यह पाचन में सहायता करता है",
                                "यह रक्त का रंग बदलता है",
                                "यह हड्डियाँ छोटी करता है",
                                "यह पानी का स्थान लेता है"
                        },
                        0,
                        "Roughage supports digestion and helps the body remove waste.",
                        "रेशेदार भोजन पाचन में सहायता करता है और शरीर से अपशिष्ट निकालने में मदद करता है।"
                )
        );

        return questions;
    }

    @NonNull
    private static List<PracticeQuestion>
    getReadingSkillsQuestions() {

        List<PracticeQuestion> questions =
                new ArrayList<>();

        questions.add(
                createQuestion(
                        "What should you read first before a passage?",
                        "गद्यांश पढ़ने से पहले क्या पढ़ना चाहिए?",
                        new String[]{
                                "Last sentence",
                                "Title",
                                "Answer key",
                                "Page number"
                        },
                        new String[]{
                                "अंतिम वाक्य",
                                "शीर्षक",
                                "उत्तर पुस्तिका",
                                "पृष्ठ संख्या"
                        },
                        1,
                        "The title gives an early idea about the topic of the passage.",
                        "शीर्षक से गद्यांश के विषय का प्रारम्भिक संकेत मिलता है।"
                )
        );

        questions.add(
                createQuestion(
                        "What is the main idea?",
                        "मुख्य विचार क्या होता है?",
                        new String[]{
                                "The central message",
                                "The longest word",
                                "The first punctuation mark",
                                "The page number"
                        },
                        new String[]{
                                "मुख्य संदेश",
                                "सबसे लंबा शब्द",
                                "पहला विराम चिह्न",
                                "पृष्ठ संख्या"
                        },
                        0,
                        "The main idea is the central message or most important point of the passage.",
                        "मुख्य विचार गद्यांश का केंद्रीय संदेश या सबसे महत्वपूर्ण बात होती है।"
                )
        );

        questions.add(
                createQuestion(
                        "What should you do with an unfamiliar word?",
                        "किसी कठिन या अनजान शब्द के साथ क्या करना चाहिए?",
                        new String[]{
                                "Ignore the whole passage",
                                "Underline it and understand from context",
                                "Stop reading permanently",
                                "Delete the word"
                        },
                        new String[]{
                                "पूरा गद्यांश छोड़ दें",
                                "उसे चिन्हित करके संदर्भ से अर्थ समझें",
                                "पढ़ना हमेशा के लिए रोक दें",
                                "शब्द मिटा दें"
                        },
                        1,
                        "Marking unfamiliar words helps you return to them and understand their meaning from context.",
                        "कठिन शब्दों को चिन्हित करने से उनका अर्थ संदर्भ के आधार पर समझने में सहायता मिलती है।"
                )
        );

        questions.add(
                createQuestion(
                        "A passage-based answer should mainly use:",
                        "गद्यांश पर आधारित उत्तर में मुख्य रूप से किसका उपयोग होना चाहिए?",
                        new String[]{
                                "Information from the passage",
                                "A random guess",
                                "An unrelated story",
                                "Only the title"
                        },
                        new String[]{
                                "गद्यांश में दी गई जानकारी",
                                "अनुमान",
                                "असंबंधित कहानी",
                                "केवल शीर्षक"
                        },
                        0,
                        "The answer should be supported by information given in the passage.",
                        "उत्तर गद्यांश में दी गई जानकारी पर आधारित होना चाहिए।"
                )
        );

        return questions;
    }

    @NonNull
    private static List<PracticeQuestion>
    getHindiGrammarQuestions() {

        List<PracticeQuestion> questions =
                new ArrayList<>();

        questions.add(
                createQuestion(
                        "Identify the noun in: Ram reads a book.",
                        "वाक्य में संज्ञा पहचानिए: राम पुस्तक पढ़ता है।",
                        new String[]{
                                "राम",
                                "पढ़ता है",
                                "तेज़",
                                "लेकिन"
                        },
                        new String[]{
                                "राम",
                                "पढ़ता है",
                                "तेज़",
                                "लेकिन"
                        },
                        0,
                        "Ram is the name of a person, so it is a noun.",
                        "राम एक व्यक्ति का नाम है, इसलिए यह संज्ञा है।"
                )
        );

        questions.add(
                createQuestion(
                        "Identify the verb in: Ram reads a book.",
                        "वाक्य में क्रिया पहचानिए: राम पुस्तक पढ़ता है।",
                        new String[]{
                                "राम",
                                "पुस्तक",
                                "पढ़ता है",
                                "का"
                        },
                        new String[]{
                                "राम",
                                "पुस्तक",
                                "पढ़ता है",
                                "का"
                        },
                        2,
                        "The words 'पढ़ता है' show an action, so they form the verb.",
                        "‘पढ़ता है’ कार्य का बोध कराता है, इसलिए यह क्रिया है।"
                )
        );

        questions.add(
                createQuestion(
                        "What is used in place of a noun?",
                        "संज्ञा के स्थान पर किसका प्रयोग किया जाता है?",
                        new String[]{
                                "Pronoun",
                                "Verb",
                                "Adjective",
                                "Punctuation"
                        },
                        new String[]{
                                "सर्वनाम",
                                "क्रिया",
                                "विशेषण",
                                "विराम चिह्न"
                        },
                        0,
                        "A pronoun is used in place of a noun.",
                        "संज्ञा के स्थान पर सर्वनाम का प्रयोग किया जाता है।"
                )
        );

        questions.add(
                createQuestion(
                        "Which word describes a noun?",
                        "संज्ञा की विशेषता बताने वाला शब्द क्या कहलाता है?",
                        new String[]{
                                "Adjective",
                                "Pronoun",
                                "Verb",
                                "Conjunction"
                        },
                        new String[]{
                                "विशेषण",
                                "सर्वनाम",
                                "क्रिया",
                                "समुच्चयबोधक"
                        },
                        0,
                        "An adjective describes the quality or feature of a noun.",
                        "विशेषण संज्ञा की विशेषता या गुण बताता है।"
                )
        );

        return questions;
    }

    @NonNull
    private static List<PracticeQuestion>
    getHistoryQuestions() {

        List<PracticeQuestion> questions =
                new ArrayList<>();

        questions.add(
                createQuestion(
                        "What does history study?",
                        "इतिहास किसका अध्ययन करता है?",
                        new String[]{
                                "The past",
                                "Only the future",
                                "Only weather",
                                "Only mathematics"
                        },
                        new String[]{
                                "अतीत",
                                "केवल भविष्य",
                                "केवल मौसम",
                                "केवल गणित"
                        },
                        0,
                        "History studies past events, people and societies.",
                        "इतिहास अतीत की घटनाओं, लोगों और समाजों का अध्ययन करता है।"
                )
        );

        questions.add(
                createQuestion(
                        "Coins and old buildings are examples of:",
                        "सिक्के और पुराने भवन किस प्रकार के स्रोत हैं?",
                        new String[]{
                                "Archaeological sources",
                                "Future predictions",
                                "Mathematical formulas",
                                "Weather reports"
                        },
                        new String[]{
                                "पुरातात्विक स्रोत",
                                "भविष्यवाणी",
                                "गणितीय सूत्र",
                                "मौसम रिपोर्ट"
                        },
                        0,
                        "Objects, coins, tools and buildings are archaeological sources.",
                        "वस्तुएँ, सिक्के, औजार और भवन पुरातात्विक स्रोत हैं।"
                )
        );

        questions.add(
                createQuestion(
                        "What does a timeline do?",
                        "समयरेखा का क्या कार्य है?",
                        new String[]{
                                "Arranges events in chronological order",
                                "Measures temperature",
                                "Shows only maps",
                                "Counts money"
                        },
                        new String[]{
                                "घटनाओं को कालक्रम में व्यवस्थित करती है",
                                "तापमान मापती है",
                                "केवल नक्शे दिखाती है",
                                "धन गिनती है"
                        },
                        0,
                        "A timeline places events according to when they happened.",
                        "समयरेखा घटनाओं को उनके घटित होने के क्रम में व्यवस्थित करती है।"
                )
        );

        questions.add(
                createQuestion(
                        "A manuscript is mainly a:",
                        "पांडुलिपि मुख्य रूप से क्या है?",
                        new String[]{
                                "Written source",
                                "Modern machine",
                                "Weather instrument",
                                "Type of food"
                        },
                        new String[]{
                                "लिखित स्रोत",
                                "आधुनिक मशीन",
                                "मौसम यंत्र",
                                "भोजन का प्रकार"
                        },
                        0,
                        "A manuscript is an old handwritten document and is a literary source.",
                        "पांडुलिपि पुराना हस्तलिखित दस्तावेज़ है और साहित्यिक स्रोत होती है।"
                )
        );

        return questions;
    }

    @NonNull
    private static List<PracticeQuestion>
    getComputerQuestions() {

        List<PracticeQuestion> questions =
                new ArrayList<>();

        questions.add(
                createQuestion(
                        "Which is an input device?",
                        "इनमें से कौन-सा इनपुट डिवाइस है?",
                        new String[]{
                                "Keyboard",
                                "Monitor",
                                "Speaker",
                                "Printer"
                        },
                        new String[]{
                                "कीबोर्ड",
                                "मॉनिटर",
                                "स्पीकर",
                                "प्रिंटर"
                        },
                        0,
                        "A keyboard sends data and commands to the computer.",
                        "कीबोर्ड कंप्यूटर को डेटा और निर्देश भेजता है।"
                )
        );

        questions.add(
                createQuestion(
                        "Which is an output device?",
                        "इनमें से कौन-सा आउटपुट डिवाइस है?",
                        new String[]{
                                "Mouse",
                                "Keyboard",
                                "Monitor",
                                "Scanner"
                        },
                        new String[]{
                                "माउस",
                                "कीबोर्ड",
                                "मॉनिटर",
                                "स्कैनर"
                        },
                        2,
                        "A monitor displays information produced by the computer.",
                        "मॉनिटर कंप्यूटर द्वारा तैयार की गई जानकारी प्रदर्शित करता है।"
                )
        );

        questions.add(
                createQuestion(
                        "What does the CPU mainly do?",
                        "CPU का मुख्य कार्य क्या है?",
                        new String[]{
                                "Processes instructions",
                                "Prints paper",
                                "Cleans the keyboard",
                                "Provides internet automatically"
                        },
                        new String[]{
                                "निर्देशों को संसाधित करता है",
                                "कागज़ प्रिंट करता है",
                                "कीबोर्ड साफ करता है",
                                "अपने-आप इंटरनेट देता है"
                        },
                        0,
                        "The CPU processes instructions and controls computer operations.",
                        "CPU निर्देशों को संसाधित करता है और कंप्यूटर के कार्यों को नियंत्रित करता है।"
                )
        );

        questions.add(
                createQuestion(
                        "What is the purpose of a storage device?",
                        "स्टोरेज डिवाइस का उद्देश्य क्या है?",
                        new String[]{
                                "Keep data for future use",
                                "Increase room temperature",
                                "Write on paper",
                                "Replace the monitor"
                        },
                        new String[]{
                                "डेटा को भविष्य के लिए सुरक्षित रखना",
                                "कमरे का तापमान बढ़ाना",
                                "कागज़ पर लिखना",
                                "मॉनिटर का स्थान लेना"
                        },
                        0,
                        "Storage devices save data so it can be used later.",
                        "स्टोरेज डिवाइस डेटा को सुरक्षित रखते हैं ताकि उसका बाद में उपयोग किया जा सके।"
                )
        );

        return questions;
    }

    @NonNull
    private static List<PracticeQuestion> getGenericQuestions(
            @NonNull String subjectName,
            @NonNull String chapterTitle
    ) {
        List<PracticeQuestion> questions =
                new ArrayList<>();

        questions.add(
                createQuestion(
                        "What is the best first step while studying "
                                + chapterTitle + "?",
                        chapterTitle
                                + " पढ़ते समय पहला सही कदम क्या है?",
                        new String[]{
                                "Understand the chapter title",
                                "Skip every explanation",
                                "Memorise without understanding",
                                "Close the book"
                        },
                        new String[]{
                                "अध्याय के शीर्षक को समझना",
                                "हर explanation छोड़ देना",
                                "बिना समझे याद करना",
                                "पुस्तक बंद कर देना"
                        },
                        0,
                        "Understanding the title gives you an early idea of the chapter topic.",
                        "शीर्षक समझने से अध्याय के विषय का प्रारम्भिक विचार मिलता है।"
                )
        );

        questions.add(
                createQuestion(
                        "What should you identify while reading a lesson?",
                        "पाठ पढ़ते समय क्या पहचानना चाहिए?",
                        new String[]{
                                "Important words and ideas",
                                "Only page colour",
                                "Only chapter number",
                                "Nothing"
                        },
                        new String[]{
                                "महत्वपूर्ण शब्द और विचार",
                                "केवल पृष्ठ का रंग",
                                "केवल अध्याय संख्या",
                                "कुछ भी नहीं"
                        },
                        0,
                        "Important words and ideas help you understand the main concept.",
                        "महत्वपूर्ण शब्द और विचार मुख्य concept को समझने में सहायता करते हैं।"
                )
        );

        questions.add(
                createQuestion(
                        "How can a concept become easier to remember?",
                        "किसी concept को आसानी से कैसे याद रखा जा सकता है?",
                        new String[]{
                                "Connect it with daily life",
                                "Avoid examples",
                                "Never revise it",
                                "Ignore its meaning"
                        },
                        new String[]{
                                "उसे दैनिक जीवन से जोड़कर",
                                "उदाहरणों से बचकर",
                                "कभी revision न करके",
                                "उसका अर्थ अनदेखा करके"
                        },
                        0,
                        "Connecting a concept with daily life makes it meaningful and easier to remember.",
                        "Concept को दैनिक जीवन से जोड़ने पर वह अर्थपूर्ण और याद रखने में आसान बनता है।"
                )
        );

        questions.add(
                createQuestion(
                        "What should you do after completing a lesson?",
                        "पाठ पूरा करने के बाद क्या करना चाहिए?",
                        new String[]{
                                "Revise the key points",
                                "Forget it immediately",
                                "Avoid all practice",
                                "Delete your notes"
                        },
                        new String[]{
                                "मुख्य बिंदुओं का revision",
                                "उसे तुरंत भूल जाना",
                                "सभी practice से बचना",
                                "अपने notes मिटा देना"
                        },
                        0,
                        "Revision strengthens memory and understanding.",
                        "Revision से memory और understanding मजबूत होती है।"
                )
        );

        return questions;
    }

    @NonNull
    private static PracticeQuestion createQuestion(
            @NonNull String englishQuestion,
            @NonNull String hindiQuestion,
            @NonNull String[] englishOptions,
            @NonNull String[] hindiOptions,
            int correctOptionIndex,
            @NonNull String englishExplanation,
            @NonNull String hindiExplanation
    ) {
        return new PracticeQuestion(
                englishQuestion,
                hindiQuestion,
                Arrays.asList(englishOptions),
                Arrays.asList(hindiOptions),
                correctOptionIndex,
                englishExplanation,
                hindiExplanation
        );
    }

    @NonNull
    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    @NonNull
    private static String getSafeText(
            String value,
            @NonNull String fallback
    ) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }
}