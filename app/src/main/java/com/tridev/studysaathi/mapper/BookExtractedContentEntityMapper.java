package com.tridev.studysaathi.mapper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.content.parser
        .BookChapterSectionExtractor;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterContentEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Book OCR section extraction को Parent-reviewable chapter-content draft में
 * बदलता है। Mapper कभी भी content को approve/publish नहीं करता।
 */
public final class BookExtractedContentEntityMapper {

    private BookExtractedContentEntityMapper() {
        throw new AssertionError(
                "BookExtractedContentEntityMapper "
                        + "cannot be instantiated."
        );
    }

    @NonNull
    public static SchoolBookChapterContentEntity toDraftEntity(
            long chapterRowId,
            @NonNull BookChapterSectionExtractor
                    .ExtractedChapterContent extractedContent
    ) {
        if (chapterRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid exact chapter row ID is required."
            );
        }

        SchoolBookChapterContentEntity entity =
                new SchoolBookChapterContentEntity();

        entity.setChapterRowId(chapterRowId);

        LanguageSections introduction =
                splitByLanguage(
                        extractedContent.getIntroduction()
                );

        LanguageSections explanation =
                splitByLanguage(
                        extractedContent
                                .getDetailedExplanation()
                );

        LanguageSections keyPoints =
                splitByLanguage(
                        extractedContent.getKeyPoints()
                );

        LanguageSections examples =
                splitByLanguage(
                        extractedContent.getExamples()
                );

        LanguageSections summary =
                splitByLanguage(
                        extractedContent.getSummary()
                );

        entity.setLanguageMode(
                detectLanguageMode(
                        introduction,
                        explanation,
                        keyPoints,
                        examples,
                        summary
                )
        );

        entity.setChapterIntroductionEnglish(
                introduction.english
        );

        entity.setChapterIntroductionHindi(
                introduction.hindi
        );

        entity.setDetailedExplanationEnglish(
                explanation.english
        );

        entity.setDetailedExplanationHindi(
                explanation.hindi
        );

        entity.setKeyPointsEnglish(
                keyPoints.english
        );

        entity.setKeyPointsHindi(
                keyPoints.hindi
        );

        entity.setWorkedExamplesEnglish(
                examples.english
        );

        entity.setWorkedExamplesHindi(
                examples.hindi
        );

        entity.setChapterSummaryEnglish(
                summary.english
        );

        entity.setChapterSummaryHindi(
                summary.hindi
        );

        entity.setPracticeQuestionsJson(
                createPracticeQuestionsJson(
                        extractedContent.getExercises()
                )
        );

        entity.setSourcePageReferencesJson(
                createSourceReferencesJson(
                        extractedContent
                                .getSourcePageNumbers()
                )
        );

        entity.setContentSource(
                SchoolBookChapterContentEntity
                        .CONTENT_SOURCE_BOOK_PAGE_OCR
        );

        entity.setReviewStatus(
                SchoolBookChapterContentEntity
                        .REVIEW_STATUS_PENDING_REVIEW
        );

        entity.setParentApproved(false);
        entity.setApprovedAt(0L);
        entity.setGenerationVersion(1);

        long currentTime =
                System.currentTimeMillis();

        entity.setLastGeneratedAt(currentTime);

        entity.setEstimatedReadingMinutes(
                estimateReadingMinutes(
                        extractedContent
                )
        );

        return entity;
    }

    @NonNull
    private static String createPracticeQuestionsJson(
            @NonNull String exercises
    ) {
        JSONArray questions =
                new JSONArray();

        String safeExercises =
                safeText(exercises);

        if (safeExercises.isEmpty()) {
            return questions.toString();
        }

        String[] lines =
                safeExercises.split("\\R");

        for (String rawLine : lines) {
            String line =
                    safeText(rawLine);

            if (line.isEmpty()) {
                continue;
            }

            JSONObject question =
                    new JSONObject();

            try {
                question.put(
                        "question",
                        line
                );

                if (containsDevanagari(line)) {
                    question.put(
                            "question_hindi",
                            line
                    );
                } else {
                    question.put(
                            "question_english",
                            line
                    );
                }

                question.put(
                        "source",
                        "BOOK_PAGE_OCR"
                );
            } catch (JSONException exception) {
                throw new IllegalStateException(
                        "Practice-question JSON "
                                + "could not be created.",
                        exception
                );
            }

            questions.put(question);
        }

        return questions.toString();
    }

    @NonNull
    private static String createSourceReferencesJson(
            @NonNull List<Integer> pageNumbers
    ) {
        JSONArray references =
                new JSONArray();

        for (Integer pageNumber
                : pageNumbers) {

            if (pageNumber == null
                    || pageNumber <= 0) {
                continue;
            }

            JSONObject reference =
                    new JSONObject();

            try {
                reference.put(
                        "page_number",
                        pageNumber
                );

                reference.put(
                        "source_type",
                        "BOOK_PAGE_OCR"
                );
            } catch (JSONException exception) {
                throw new IllegalStateException(
                        "Source-page reference JSON "
                                + "could not be created.",
                        exception
                );
            }

            references.put(reference);
        }

        return references.toString();
    }

    @NonNull
    private static String detectLanguageMode(
            @NonNull LanguageSections... sections
    ) {
        boolean hasEnglish = false;
        boolean hasHindi = false;

        for (LanguageSections section
                : sections) {

            if (!section.english.isEmpty()) {
                hasEnglish = true;
            }

            if (!section.hindi.isEmpty()) {
                hasHindi = true;
            }
        }

        if (hasEnglish && hasHindi) {
            return SchoolBookChapterContentEntity
                    .LANGUAGE_MODE_BILINGUAL;
        }

        if (hasHindi) {
            return SchoolBookChapterContentEntity
                    .LANGUAGE_MODE_HINDI;
        }

        return SchoolBookChapterContentEntity
                .LANGUAGE_MODE_ENGLISH;
    }

    @NonNull
    private static LanguageSections splitByLanguage(
            @NonNull String text
    ) {
        StringBuilder english =
                new StringBuilder();

        StringBuilder hindi =
                new StringBuilder();

        String safeText =
                safeText(text);

        if (safeText.isEmpty()) {
            return new LanguageSections("", "");
        }

        String[] lines =
                safeText.split("\\R");

        for (String rawLine : lines) {
            String line =
                    safeText(rawLine);

            if (line.isEmpty()) {
                continue;
            }

            if (containsDevanagari(line)) {
                appendLine(hindi, line);
            } else {
                appendLine(english, line);
            }
        }

        return new LanguageSections(
                english.toString(),
                hindi.toString()
        );
    }

    private static boolean containsDevanagari(
            @NonNull String value
    ) {
        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(index);

            if (character >= '\u0900'
                    && character <= '\u097F') {
                return true;
            }
        }

        return false;
    }

    private static void appendLine(
            @NonNull StringBuilder builder,
            @NonNull String line
    ) {
        if (builder.length() > 0) {
            builder.append('\n');
        }

        builder.append(line);
    }

    private static int estimateReadingMinutes(
            @NonNull BookChapterSectionExtractor
                    .ExtractedChapterContent content
    ) {
        String allText =
                content.getIntroduction()
                        + " "
                        + content.getDetailedExplanation()
                        + " "
                        + content.getKeyPoints()
                        + " "
                        + content.getExamples()
                        + " "
                        + content.getSummary();

        String normalized =
                safeText(allText);

        if (normalized.isEmpty()) {
            return 0;
        }

        int wordCount =
                normalized.split("\\s+").length;

        return Math.max(
                1,
                (int) Math.ceil(
                        wordCount / 180.0
                )
        );
    }

    @NonNull
    private static String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private static final class LanguageSections {

        @NonNull
        private final String english;

        @NonNull
        private final String hindi;

        private LanguageSections(
                @NonNull String english,
                @NonNull String hindi
        ) {
            this.english = english;
            this.hindi = hindi;
        }
    }
}
