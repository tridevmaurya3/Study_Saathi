package com.tridev.studysaathi.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterContentEntity;
import com.tridev.studysaathi.model.LessonContent;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SchoolBookChapterContentLessonMapper {

    private SchoolBookChapterContentLessonMapper() {
        throw new AssertionError(
                "SchoolBookChapterContentLessonMapper "
                        + "cannot be instantiated."
        );
    }

    @NonNull
    public static LessonContent toLessonContent(
            @NonNull String chapterTitle,
            @NonNull SchoolBookChapterContentEntity content
    ) {
        if (!content.isReadyForChildMode()) {
            throw new IllegalStateException(
                    "Only Parent-approved chapter content "
                            + "can be shown in Child Mode."
            );
        }

        String englishExplanation =
                joinSections(
                        content.getChapterIntroductionEnglish(),
                        content.getDetailedExplanationEnglish(),
                        labeledSection(
                                "Chapter Summary",
                                content.getChapterSummaryEnglish()
                        )
                );

        String hindiExplanation =
                joinSections(
                        content.getChapterIntroductionHindi(),
                        content.getDetailedExplanationHindi(),
                        labeledSection(
                                "अध्याय सारांश",
                                content.getChapterSummaryHindi()
                        )
                );

        String englishKeyPoints =
                joinSections(
                        content.getKeyPointsEnglish(),
                        labeledSection(
                                "Important Terms",
                                content.getImportantTermsEnglish()
                        )
                );

        String hindiKeyPoints =
                joinSections(
                        content.getKeyPointsHindi(),
                        labeledSection(
                                "महत्वपूर्ण शब्द",
                                content.getImportantTermsHindi()
                        )
                );

        String englishExamples =
                joinSections(
                        content.getWorkedExamplesEnglish(),
                        labeledSection(
                                "Real-life Examples",
                                content.getRealLifeExamplesEnglish()
                        ),
                        labeledSection(
                                "Common Mistakes",
                                content.getCommonMistakesEnglish()
                        )
                );

        String hindiExamples =
                joinSections(
                        content.getWorkedExamplesHindi(),
                        labeledSection(
                                "वास्तविक जीवन के उदाहरण",
                                content.getRealLifeExamplesHindi()
                        ),
                        labeledSection(
                                "सामान्य गलतियाँ",
                                content.getCommonMistakesHindi()
                        )
                );

        String englishPracticeQuestions =
                formatPracticeQuestions(
                        content.getPracticeQuestionsJson(),
                        false
                );

        String hindiPracticeQuestions =
                formatPracticeQuestions(
                        content.getPracticeQuestionsJson(),
                        true
                );

        return new LessonContent(
                safeText(
                        chapterTitle
                ),
                fallback(
                        englishExplanation,
                        "English explanation is not available."
                ),
                fallback(
                        hindiExplanation,
                        "हिन्दी व्याख्या उपलब्ध नहीं है।"
                ),
                fallback(
                        englishKeyPoints,
                        "Key points are not available."
                ),
                fallback(
                        hindiKeyPoints,
                        "मुख्य बिंदु उपलब्ध नहीं हैं।"
                ),
                fallback(
                        englishExamples,
                        "Examples are not available."
                ),
                fallback(
                        hindiExamples,
                        "उदाहरण उपलब्ध नहीं हैं।"
                ),
                englishPracticeQuestions,
                hindiPracticeQuestions
        );
    }

    @NonNull
    private static String formatPracticeQuestions(
            @Nullable String questionsJson,
            boolean hindi
    ) {
        String safeJson =
                safeText(
                        questionsJson
                );

        if (safeJson.isEmpty()
                || "[]".equals(
                safeJson
        )) {

            return hindi
                    ? "अभ्यास प्रश्न अभी उपलब्ध नहीं हैं।"
                    : "Practice questions are not available yet.";
        }

        try {
            JSONArray questions =
                    new JSONArray(
                            safeJson
                    );

            StringBuilder builder =
                    new StringBuilder();

            for (int index = 0;
                 index < questions.length();
                 index++) {

                Object questionValue =
                        questions.get(
                                index
                        );

                String questionText;

                if (questionValue
                        instanceof JSONObject) {

                    JSONObject questionObject =
                            (JSONObject) questionValue;

                    questionText =
                            firstNonEmpty(
                                    questionObject.optString(
                                            hindi
                                                    ? "question_hindi"
                                                    : "question_english",
                                            ""
                                    ),
                                    questionObject.optString(
                                            "question",
                                            ""
                                    ),
                                    questionObject.optString(
                                            "text",
                                            ""
                                    )
                            );

                } else {
                    questionText =
                            safeText(
                                    questionValue
                            );
                }

                if (questionText.isEmpty()) {
                    continue;
                }

                if (builder.length() > 0) {
                    builder.append(
                            "\n\n"
                    );
                }

                builder.append(
                        index + 1
                ).append(
                        ". "
                ).append(
                        questionText
                );
            }

            if (builder.length() > 0) {
                return builder.toString();
            }

        } catch (Exception ignored) {
            // Invalid draft JSON is never displayed directly.
        }

        return hindi
                ? "अभ्यास प्रश्न अभी उपलब्ध नहीं हैं।"
                : "Practice questions are not available yet.";
    }

    @NonNull
    private static String labeledSection(
            @NonNull String label,
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return "";
        }

        return label
                + ":\n"
                + safeValue;
    }

    @NonNull
    private static String joinSections(
            @Nullable String... sections
    ) {
        StringBuilder builder =
                new StringBuilder();

        if (sections == null) {
            return "";
        }

        for (String section : sections) {
            String safeSection =
                    safeText(
                            section
                    );

            if (safeSection.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(
                        "\n\n"
                );
            }

            builder.append(
                    safeSection
            );
        }

        return builder.toString();
    }

    @NonNull
    private static String firstNonEmpty(
            @Nullable String... values
    ) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            String safeValue =
                    safeText(
                            value
                    );

            if (!safeValue.isEmpty()) {
                return safeValue;
            }
        }

        return "";
    }

    @NonNull
    private static String fallback(
            @Nullable String value,
            @NonNull String fallbackValue
    ) {
        String safeValue =
                safeText(
                        value
                );

        return safeValue.isEmpty()
                ? fallbackValue
                : safeValue;
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString().trim();
    }
}