package com.tridev.studysaathi.data.catalog;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.model.LessonContent;

import java.util.Locale;

public final class LessonCatalog {

    private LessonCatalog() {
        // Utility class.
    }

    @NonNull
    public static LessonContent getLessonContent(
            String subjectName,
            String chapterTitle,
            String chapterDescription
    ) {
        String safeSubject = getSafeText(
                subjectName,
                "Subject"
        );

        String safeChapter = getSafeText(
                chapterTitle,
                "Chapter"
        );

        String safeDescription = getSafeText(
                chapterDescription,
                "This lesson introduces the main ideas of the chapter."
        );

        String normalizedTitle = safeChapter
                .trim()
                .toLowerCase(Locale.ROOT);

        switch (normalizedTitle) {
            case "fractions":
                return createFractionsLesson();

            case "food and nutrition":
                return createFoodAndNutritionLesson();

            case "reading skills":
                return createReadingSkillsLesson();

            case "हिंदी व्याकरण":
                return createHindiGrammarLesson();

            case "understanding history":
                return createHistoryLesson();

            case "computer fundamentals":
                return createComputerFundamentalsLesson();

            default:
                return createGenericLesson(
                        safeSubject,
                        safeChapter,
                        safeDescription
                );
        }
    }

    @NonNull
    private static LessonContent createFractionsLesson() {
        return new LessonContent(
                "Understanding Fractions",
                "A fraction represents a part of a whole. It is written using two numbers. The top number is called the numerator and tells us how many parts are selected. The bottom number is called the denominator and tells us how many equal parts make the whole.",
                "भिन्न किसी पूरी वस्तु के एक हिस्से को दर्शाता है। इसे दो संख्याओं की सहायता से लिखा जाता है। ऊपर की संख्या को अंश कहते हैं, जो बताती है कि कितने भाग लिए गए हैं। नीचे की संख्या को हर कहते हैं, जो बताती है कि पूरी वस्तु को कुल कितने बराबर भागों में बाँटा गया है।",
                "• Numerator is written above the fraction line.\n• Denominator is written below the fraction line.\n• All parts of the whole must be equal.\n• Fractions with the same denominator are called like fractions.",
                "• अंश भिन्न रेखा के ऊपर लिखा जाता है।\n• हर भिन्न रेखा के नीचे लिखा जाता है।\n• पूरी वस्तु के सभी भाग बराबर होने चाहिए।\n• समान हर वाली भिन्नों को समहर भिन्न कहते हैं।",
                "A pizza is divided into 8 equal slices. If Riya eats 3 slices, she has eaten 3/8 of the pizza. Here, 3 is the numerator and 8 is the denominator.",
                "एक पिज़्ज़ा को 8 बराबर टुकड़ों में बाँटा गया। यदि रिया 3 टुकड़े खाती है, तो उसने पिज़्ज़ा का 3/8 भाग खाया। यहाँ 3 अंश और 8 हर है।",
                "A chocolate is divided into 6 equal pieces. Aman eats 2 pieces. What fraction of the chocolate did Aman eat?",
                "एक चॉकलेट को 6 बराबर भागों में बाँटा गया। अमन ने 2 भाग खाए। अमन ने चॉकलेट का कितना भाग खाया?"
        );
    }

    @NonNull
    private static LessonContent createFoodAndNutritionLesson() {
        return new LessonContent(
                "Food and Nutrition",
                "Food gives our body energy, helps us grow and protects us from illness. Different foods contain different nutrients. The main nutrients are carbohydrates, proteins, fats, vitamins and minerals. Water and roughage are also important for a healthy body.",
                "भोजन हमारे शरीर को ऊर्जा देता है, वृद्धि में सहायता करता है और बीमारियों से बचाता है। अलग-अलग खाद्य पदार्थों में अलग-अलग पोषक तत्त्व होते हैं। मुख्य पोषक तत्त्व कार्बोहाइड्रेट, प्रोटीन, वसा, विटामिन और खनिज हैं। स्वस्थ शरीर के लिए पानी और रेशेदार भोजन भी आवश्यक हैं।",
                "• Carbohydrates provide energy.\n• Proteins help growth and repair.\n• Fats store energy.\n• Vitamins and minerals protect the body.\n• A balanced diet contains all nutrients in proper amounts.",
                "• कार्बोहाइड्रेट ऊर्जा देते हैं।\n• प्रोटीन वृद्धि और शरीर की मरम्मत में मदद करते हैं।\n• वसा ऊर्जा को संचित करती है।\n• विटामिन और खनिज शरीर की रक्षा करते हैं।\n• संतुलित आहार में सभी पोषक तत्त्व उचित मात्रा में होते हैं।",
                "Rice provides carbohydrates, pulses provide proteins, and fruits provide vitamins and minerals. Eating these in suitable amounts makes the meal more balanced.",
                "चावल से कार्बोहाइड्रेट, दाल से प्रोटीन और फलों से विटामिन तथा खनिज मिलते हैं। इन्हें उचित मात्रा में खाने से भोजन अधिक संतुलित बनता है।",
                "Name one food rich in protein and explain why protein is important.",
                "प्रोटीन से भरपूर किसी एक खाद्य पदार्थ का नाम लिखिए और बताइए कि प्रोटीन क्यों आवश्यक है।"
        );
    }

    @NonNull
    private static LessonContent createReadingSkillsLesson() {
        return new LessonContent(
                "Reading Skills",
                "Good reading means understanding the message, not only speaking the words. Read the title first, notice important words and then read each sentence carefully. After reading, ask yourself who, what, when, where, why and how.",
                "अच्छा पठन केवल शब्दों को बोलना नहीं, बल्कि उनके संदेश को समझना है। पहले शीर्षक पढ़ें, महत्वपूर्ण शब्दों पर ध्यान दें और फिर हर वाक्य को सावधानी से पढ़ें। पढ़ने के बाद स्वयं से कौन, क्या, कब, कहाँ, क्यों और कैसे जैसे प्रश्न पूछें।",
                "• Read the title before the passage.\n• Underline unfamiliar words.\n• Identify the main idea.\n• Find supporting details.\n• Answer using information from the passage.",
                "• गद्यांश से पहले उसका शीर्षक पढ़ें।\n• कठिन शब्दों को चिन्हित करें।\n• मुख्य विचार पहचानें।\n• सहायक जानकारियाँ खोजें।\n• उत्तर गद्यांश में दी गई जानकारी के आधार पर दें।",
                "Passage: Ravi planted a small mango tree near his house. He watered it every morning. The main idea is that Ravi cared for a newly planted tree.",
                "गद्यांश: रवि ने अपने घर के पास आम का एक छोटा पौधा लगाया। वह उसे हर सुबह पानी देता था। इसका मुख्य विचार है कि रवि नए लगाए गए पौधे की देखभाल करता था।",
                "Read a short paragraph from your book and write its main idea in one sentence.",
                "अपनी पुस्तक का कोई छोटा अनुच्छेद पढ़िए और उसका मुख्य विचार एक वाक्य में लिखिए।"
        );
    }

    @NonNull
    private static LessonContent createHindiGrammarLesson() {
        return new LessonContent(
                "हिंदी व्याकरण का परिचय",
                "Hindi grammar helps us use words and sentences correctly. Nouns name people, places, animals or things. Pronouns are used in place of nouns. Verbs tell us about an action or state.",
                "हिंदी व्याकरण हमें शब्दों और वाक्यों का सही प्रयोग करना सिखाता है। संज्ञा किसी व्यक्ति, स्थान, पशु या वस्तु का नाम बताती है। सर्वनाम का प्रयोग संज्ञा के स्थान पर किया जाता है। क्रिया किसी कार्य या अवस्था का बोध कराती है।",
                "• Noun: name of a person, place, animal or thing.\n• Pronoun: used in place of a noun.\n• Adjective: describes a noun.\n• Verb: shows action or state.",
                "• संज्ञा: व्यक्ति, स्थान, पशु या वस्तु का नाम।\n• सर्वनाम: संज्ञा के स्थान पर प्रयुक्त शब्द।\n• विशेषण: संज्ञा की विशेषता बताने वाला शब्द।\n• क्रिया: कार्य या अवस्था बताने वाला शब्द।",
                "Sentence: सीता विद्यालय जाती है। सीता is a noun, विद्यालय is also a noun, and जाती है is the verb.",
                "वाक्य: सीता विद्यालय जाती है। इसमें ‘सीता’ और ‘विद्यालय’ संज्ञा हैं तथा ‘जाती है’ क्रिया है।",
                "Identify the noun and verb in this sentence: राम पुस्तक पढ़ता है।",
                "इस वाक्य में संज्ञा और क्रिया पहचानिए: राम पुस्तक पढ़ता है।"
        );
    }

    @NonNull
    private static LessonContent createHistoryLesson() {
        return new LessonContent(
                "Understanding History",
                "History is the study of the past. Historians use different sources to understand how people lived in earlier times. These sources include inscriptions, coins, buildings, tools, manuscripts and oral traditions.",
                "इतिहास अतीत का अध्ययन है। इतिहासकार यह समझने के लिए विभिन्न स्रोतों का उपयोग करते हैं कि पुराने समय में लोग किस प्रकार रहते थे। इन स्रोतों में अभिलेख, सिक्के, भवन, औजार, पांडुलिपियाँ और मौखिक परंपराएँ शामिल हैं।",
                "• History studies past events and societies.\n• Archaeological sources include objects and buildings.\n• Literary sources include written records.\n• A timeline arranges events in chronological order.",
                "• इतिहास अतीत की घटनाओं और समाजों का अध्ययन करता है।\n• पुरातात्विक स्रोतों में वस्तुएँ और भवन आते हैं।\n• साहित्यिक स्रोतों में लिखित अभिलेख आते हैं।\n• समयरेखा घटनाओं को कालक्रम में व्यवस्थित करती है।",
                "An old coin can tell us the name of a ruler, symbols used at that time and sometimes the language or script of the period.",
                "एक पुराना सिक्का हमें शासक का नाम, उस समय प्रयोग किए गए प्रतीक और कभी-कभी उस काल की भाषा या लिपि के बारे में बता सकता है।",
                "Write the names of any two sources used by historians.",
                "इतिहासकारों द्वारा उपयोग किए जाने वाले किन्हीं दो स्रोतों के नाम लिखिए।"
        );
    }

    @NonNull
    private static LessonContent createComputerFundamentalsLesson() {
        return new LessonContent(
                "Computer Fundamentals",
                "A computer is an electronic machine that accepts data, processes it and produces useful information. The main parts of a desktop computer include the monitor, keyboard, mouse and system unit.",
                "कंप्यूटर एक इलेक्ट्रॉनिक मशीन है, जो डेटा ग्रहण करती है, उसे संसाधित करती है और उपयोगी जानकारी प्रदान करती है। डेस्कटॉप कंप्यूटर के मुख्य भाग मॉनिटर, कीबोर्ड, माउस और सिस्टम यूनिट हैं।",
                "• Input devices send data to the computer.\n• Output devices show the result.\n• The CPU processes instructions.\n• Storage devices keep data for future use.",
                "• इनपुट डिवाइस कंप्यूटर को डेटा भेजते हैं।\n• आउटपुट डिवाइस परिणाम दिखाते हैं।\n• CPU निर्देशों को संसाधित करता है।\n• स्टोरेज डिवाइस डेटा को भविष्य के लिए सुरक्षित रखते हैं।",
                "Keyboard and mouse are input devices. A monitor is an output device because it displays information produced by the computer.",
                "कीबोर्ड और माउस इनपुट डिवाइस हैं। मॉनिटर आउटपुट डिवाइस है, क्योंकि यह कंप्यूटर द्वारा तैयार की गई जानकारी दिखाता है।",
                "Classify these as input or output devices: keyboard, printer, mouse and monitor.",
                "इन उपकरणों को इनपुट या आउटपुट डिवाइस के रूप में वर्गीकृत कीजिए: कीबोर्ड, प्रिंटर, माउस और मॉनिटर।"
        );
    }

    @NonNull
    private static LessonContent createGenericLesson(
            @NonNull String subjectName,
            @NonNull String chapterTitle,
            @NonNull String chapterDescription
    ) {
        return new LessonContent(
                chapterTitle,
                "This lesson belongs to " + subjectName + ". "
                        + chapterDescription
                        + " Start by understanding the meaning of the chapter title. Then divide the topic into small ideas and study one idea at a time.",
                "यह पाठ " + subjectName + " विषय से संबंधित है। "
                        + chapterDescription
                        + " पहले अध्याय के शीर्षक का अर्थ समझें। इसके बाद विषय को छोटे-छोटे भागों में बाँटकर एक समय में एक विचार पढ़ें।",
                "• Read the chapter title carefully.\n• Identify important words.\n• Learn one small concept at a time.\n• Connect the concept with daily life.\n• Revise the lesson after studying.",
                "• अध्याय का शीर्षक ध्यान से पढ़ें।\n• महत्वपूर्ण शब्द पहचानें।\n• एक समय में एक छोटा concept सीखें।\n• concept को दैनिक जीवन से जोड़ें।\n• पढ़ने के बाद पाठ का revision करें।",
                "Choose one important word from this chapter. Write its meaning and use it in a simple sentence.",
                "इस अध्याय से एक महत्वपूर्ण शब्द चुनिए। उसका अर्थ लिखिए और उसे एक सरल वाक्य में प्रयोग कीजिए।",
                "Explain the main idea of this chapter in two simple sentences.",
                "इस अध्याय के मुख्य विचार को दो सरल वाक्यों में समझाइए।"
        );
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