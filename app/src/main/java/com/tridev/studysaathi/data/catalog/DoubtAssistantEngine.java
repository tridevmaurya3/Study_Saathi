package com.tridev.studysaathi.data.catalog;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.model.LessonContent;

import java.util.Locale;

public final class DoubtAssistantEngine {

    private DoubtAssistantEngine() {
        // Utility class.
    }

    @NonNull
    public static String createAnswer(
            String studentQuestion,
            String subjectName,
            String chapterTitle,
            @NonNull LessonContent lessonContent,
            String explanationLanguage
    ) {
        String safeQuestion = getSafeText(
                studentQuestion,
                "Explain this chapter"
        );

        String safeSubject = getSafeText(
                subjectName,
                "Subject"
        );

        String safeChapter = getSafeText(
                chapterTitle,
                "Chapter"
        );

        String normalizedQuestion = safeQuestion
                .trim()
                .toLowerCase(Locale.ROOT);

        LanguageMode languageMode =
                getLanguageMode(explanationLanguage);

        if (containsAny(
                normalizedQuestion,
                "key point",
                "important point",
                "main point",
                "मुख्य बिंदु",
                "जरूरी बिंदु",
                "महत्वपूर्ण बिंदु",
                "याद करने"
        )) {
            return combineAnswer(
                    lessonContent.getEnglishKeyPoints(),
                    lessonContent.getHindiKeyPoints(),
                    languageMode
            );
        }

        if (containsAny(
                normalizedQuestion,
                "example",
                "उदाहरण",
                "मिसाल"
        )) {
            return combineAnswer(
                    lessonContent.getEnglishExample(),
                    lessonContent.getHindiExample(),
                    languageMode
            );
        }

        if (containsAny(
                normalizedQuestion,
                "practice",
                "question",
                "quiz",
                "test me",
                "सवाल",
                "प्रश्न",
                "अभ्यास"
        )) {
            return combineAnswer(
                    lessonContent.getEnglishPracticeQuestion(),
                    lessonContent.getHindiPracticeQuestion(),
                    languageMode
            );
        }

        if (containsAny(
                normalizedQuestion,
                "summary",
                "summarise",
                "summarize",
                "short",
                "brief",
                "सारांश",
                "छोटा",
                "संक्षेप"
        )) {
            String englishSummary =
                    createShortSummary(
                            lessonContent.getEnglishExplanation()
                    );

            String hindiSummary =
                    createShortSummary(
                            lessonContent.getHindiExplanation()
                    );

            return combineAnswer(
                    englishSummary,
                    hindiSummary,
                    languageMode
            );
        }

        if (containsAny(
                normalizedQuestion,
                "explain",
                "what is",
                "what are",
                "meaning",
                "define",
                "definition",
                "understand",
                "समझाओ",
                "क्या है",
                "क्या होता",
                "अर्थ",
                "परिभाषा"
        )) {
            return combineAnswer(
                    lessonContent.getEnglishExplanation(),
                    lessonContent.getHindiExplanation(),
                    languageMode
            );
        }

        String englishFallback =
                "Your question is related to "
                        + safeSubject
                        + " — "
                        + safeChapter
                        + ".\n\n"
                        + lessonContent.getEnglishExplanation()
                        + "\n\n"
                        + "For a more focused answer, try asking: "
                        + "“Explain simply”, “Show key points”, "
                        + "“Give an example” or “Give a practice question”.";

        String hindiFallback =
                "आपका प्रश्न "
                        + safeSubject
                        + " के अध्याय “"
                        + safeChapter
                        + "” से संबंधित है।\n\n"
                        + lessonContent.getHindiExplanation()
                        + "\n\n"
                        + "अधिक स्पष्ट उत्तर के लिए आप पूछ सकते हैं: "
                        + "“आसान भाषा में समझाओ”, “मुख्य बिंदु बताओ”, "
                        + "“उदाहरण दो” या “अभ्यास प्रश्न दो”।";

        return combineAnswer(
                englishFallback,
                hindiFallback,
                languageMode
        );
    }

    @NonNull
    private static LanguageMode getLanguageMode(
            String explanationLanguage
    ) {
        String normalizedLanguage =
                getSafeText(
                        explanationLanguage,
                        "Hindi + English"
                )
                        .toLowerCase(Locale.ROOT);

        boolean containsHindi =
                normalizedLanguage.contains("hindi")
                        || normalizedLanguage.contains("हिंदी");

        boolean containsEnglish =
                normalizedLanguage.contains("english")
                        || normalizedLanguage.contains("अंग्रेज");

        if (containsHindi && !containsEnglish) {
            return LanguageMode.HINDI;
        }

        if (containsEnglish && !containsHindi) {
            return LanguageMode.ENGLISH;
        }

        return LanguageMode.BILINGUAL;
    }

    @NonNull
    private static String combineAnswer(
            @NonNull String englishAnswer,
            @NonNull String hindiAnswer,
            @NonNull LanguageMode languageMode
    ) {
        switch (languageMode) {
            case HINDI:
                return hindiAnswer;

            case ENGLISH:
                return englishAnswer;

            case BILINGUAL:
            default:
                return "English:\n"
                        + englishAnswer
                        + "\n\nहिंदी:\n"
                        + hindiAnswer;
        }
    }

    private static boolean containsAny(
            @NonNull String source,
            @NonNull String... keywords
    ) {
        for (String keyword : keywords) {
            if (source.contains(
                    keyword.toLowerCase(Locale.ROOT)
            )) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private static String createShortSummary(
            @NonNull String explanation
    ) {
        String trimmedText = explanation.trim();

        if (trimmedText.length() <= 230) {
            return trimmedText;
        }

        int sentenceEnd =
                trimmedText.indexOf('.', 120);

        if (sentenceEnd > 0 && sentenceEnd <= 300) {
            return trimmedText.substring(
                    0,
                    sentenceEnd + 1
            );
        }

        return trimmedText.substring(0, 230)
                .trim()
                + "…";
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

    private enum LanguageMode {
        BILINGUAL,
        HINDI,
        ENGLISH
    }
}