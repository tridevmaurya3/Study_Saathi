package com.tridev.studysaathi.mapper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.content.parser
        .BookChapterSectionExtractor;
import com.tridev.studysaathi.data.content.scanner
        .BookPageOcrScanner;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterPageEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * एक source PDF/scanned page को एक अलग reader page entity में बदलता है।
 */
public final class BookOcrPageEntityMapper {

    private BookOcrPageEntityMapper() {
        throw new AssertionError(
                "BookOcrPageEntityMapper cannot be instantiated."
        );
    }

    @NonNull
    public static SchoolBookChapterPageEntity toPageEntity(
            @NonNull BookPageOcrScanner.BookOcrResult ocrResult,
            @NonNull BookPageOcrScanner.PageOcrText sourcePage,
            int chapterOrder,
            @NonNull String chapterTitle,
            int pageOrderInsideChapter,
            @NonNull String persistentPageImagePath
    ) {
        if (pageOrderInsideChapter <= 0) {
            throw new IllegalArgumentException(
                    "A valid page order is required."
            );
        }

        BookChapterSectionExtractor.ExtractedChapterContent
                extractedPage =
                BookChapterSectionExtractor.extract(
                        ocrResult,
                        chapterOrder,
                        chapterTitle,
                        sourcePage.getPageNumber(),
                        sourcePage.getPageNumber()
                );

        SchoolBookChapterPageEntity entity =
                new SchoolBookChapterPageEntity();

        entity.setPageOrder(
                pageOrderInsideChapter
        );

        entity.setSourceDocumentPageNumber(
                sourcePage.getPageNumber()
        );

        entity.setPageTitle(
                chapterTitle
                        + " - Page "
                        + pageOrderInsideChapter
        );

        LanguageText introduction =
                splitLanguage(
                        extractedPage.getIntroduction()
                );

        LanguageText explanation =
                splitLanguage(
                        extractedPage
                                .getDetailedExplanation()
                );

        LanguageText keyPoints =
                splitLanguage(
                        extractedPage.getKeyPoints()
                );

        LanguageText examples =
                splitLanguage(
                        extractedPage.getExamples()
                );

        LanguageText summary =
                splitLanguage(
                        extractedPage.getSummary()
                );

        entity.setIntroductionEnglish(
                introduction.english
        );
        entity.setIntroductionHindi(
                introduction.hindi
        );
        entity.setExplanationEnglish(
                explanation.english
        );
        entity.setExplanationHindi(
                explanation.hindi
        );
        entity.setKeyPointsEnglish(
                keyPoints.english
        );
        entity.setKeyPointsHindi(
                keyPoints.hindi
        );
        entity.setExamplesEnglish(
                examples.english
        );
        entity.setExamplesHindi(
                examples.hindi
        );
        entity.setSummaryEnglish(
                summary.english
        );
        entity.setSummaryHindi(
                summary.hindi
        );

        entity.setExercisesJson(
                createExercisesJson(
                        extractedPage.getExercises()
                )
        );

        entity.setPageType(
                detectPageType(extractedPage)
        );

        entity.setPersistentPageImagePath(
                persistentPageImagePath
        );

        entity.setRawOcrText(
                sourcePage.getCombinedText()
        );

        entity.setParentApproved(false);

        return entity;
    }

    @NonNull
    private static String detectPageType(
            @NonNull BookChapterSectionExtractor
                    .ExtractedChapterContent content
    ) {
        if (content.hasExercises()) {
            return SchoolBookChapterPageEntity
                    .PAGE_TYPE_EXERCISE;
        }

        if (content.hasExamples()) {
            return SchoolBookChapterPageEntity
                    .PAGE_TYPE_EXAMPLE;
        }

        if (!content.getSummary().isEmpty()) {
            return SchoolBookChapterPageEntity
                    .PAGE_TYPE_SUMMARY;
        }

        return SchoolBookChapterPageEntity
                .PAGE_TYPE_LEARNING;
    }

    @NonNull
    private static String createExercisesJson(
            @NonNull String exercises
    ) {
        JSONArray array =
                new JSONArray();

        String safeExercises =
                safeText(exercises);

        if (safeExercises.isEmpty()) {
            return array.toString();
        }

        for (String rawLine
                : safeExercises.split("\\R")) {

            String line =
                    safeText(rawLine);

            if (line.isEmpty()) {
                continue;
            }

            JSONObject item =
                    new JSONObject();

            try {
                item.put("question", line);

                if (containsDevanagari(line)) {
                    item.put(
                            "question_hindi",
                            line
                    );
                } else {
                    item.put(
                            "question_english",
                            line
                    );
                }

                item.put(
                        "source",
                        "BOOK_PAGE_OCR"
                );
            } catch (JSONException exception) {
                throw new IllegalStateException(
                        "Page exercise JSON "
                                + "could not be created.",
                        exception
                );
            }

            array.put(item);
        }

        return array.toString();
    }

    @NonNull
    private static LanguageText splitLanguage(
            @NonNull String value
    ) {
        StringBuilder english =
                new StringBuilder();

        StringBuilder hindi =
                new StringBuilder();

        for (String rawLine : value.split("\\R")) {
            String line =
                    safeText(rawLine);

            if (line.isEmpty()) {
                continue;
            }

            appendLine(
                    containsDevanagari(line)
                            ? hindi
                            : english,
                    line
            );
        }

        return new LanguageText(
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

    @NonNull
    private static String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private static final class LanguageText {

        @NonNull
        private final String english;

        @NonNull
        private final String hindi;

        private LanguageText(
                @NonNull String english,
                @NonNull String hindi
        ) {
            this.english = english;
            this.hindi = hindi;
        }
    }
}
