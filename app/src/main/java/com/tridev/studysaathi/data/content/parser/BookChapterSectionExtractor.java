package com.tridev.studysaathi.data.content.parser;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.content.scanner
        .BookPageOcrScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parent-approved chapter page range के OCR text को learning sections में
 * अलग करता है। यह draft extraction है; publication से पहले Parent review
 * आवश्यक है।
 */
public final class BookChapterSectionExtractor {

    private static final int MAX_SECTION_HEADING_LENGTH = 80;

    private static final Pattern INTRODUCTION_HEADING =
            headingPattern(
                    "introduction|overview|getting started|"
                            + "परिचय|भूमिका|प्रस्तावना"
            );

    private static final Pattern KEY_POINTS_HEADING =
            headingPattern(
                    "key points?|important points?|"
                            + "remember|things to remember|"
                            + "मुख्य बिंदु|महत्वपूर्ण बिंदु|"
                            + "याद रखें"
            );

    private static final Pattern EXAMPLE_HEADING =
            headingPattern(
                    "examples?|worked examples?|"
                            + "solved examples?|illustrations?|"
                            + "उदाहरण|हल उदाहरण|सुलझाए गए उदाहरण"
            );

    private static final Pattern EXERCISE_HEADING =
            headingPattern(
                    "exercises?|practice|questions?|"
                            + "assessment|activities|worksheet|"
                            + "अभ्यास|अभ्यास प्रश्न|प्रश्न|"
                            + "गतिविधि|मूल्यांकन"
            );

    private static final Pattern SUMMARY_HEADING =
            headingPattern(
                    "summary|recap|chapter review|"
                            + "what we have learnt|"
                            + "सारांश|पुनरावृत्ति|"
                            + "हमने क्या सीखा"
            );

    private static final Pattern EXAMPLE_LINE =
            Pattern.compile(
                    "^(example|उदाहरण)"
                            + "\\s*[0-9०-९ivxlcdm]*"
                            + "\\s*[:.\\-–—]?",
                    Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE
            );

    private static final Pattern QUESTION_LINE =
            Pattern.compile(
                    "^((q(uestion)?\\s*)?[0-9०-९]+"
                            + "\\s*[.):\\-]|"
                            + "प्रश्न\\s*[0-9०-९]*"
                            + "\\s*[:.\\-–—]?)",
                    Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE
            );

    private BookChapterSectionExtractor() {
        throw new AssertionError(
                "BookChapterSectionExtractor "
                        + "cannot be instantiated."
        );
    }

    @NonNull
    public static ExtractedChapterContent extract(
            @NonNull BookPageOcrScanner.BookOcrResult
                    ocrResult,
            int chapterOrder,
            @NonNull String chapterTitle,
            int startPage,
            int endPage
    ) {
        validateArguments(
                ocrResult,
                chapterOrder,
                chapterTitle,
                startPage,
                endPage
        );

        SectionBuffers buffers =
                new SectionBuffers();

        Section currentSection =
                Section.EXPLANATION;

        ArrayList<Integer> sourcePages =
                new ArrayList<>();

        for (BookPageOcrScanner.PageOcrText page
                : ocrResult.getPages()) {

            if (page.getPageNumber() < startPage
                    || page.getPageNumber() > endPage) {
                continue;
            }

            String pageText =
                    safeText(
                            page.getCombinedText()
                    );

            if (pageText.isEmpty()) {
                continue;
            }

            sourcePages.add(
                    page.getPageNumber()
            );

            String[] lines =
                    pageText.split("\\R");

            for (String rawLine : lines) {
                String line =
                        normalizeLine(rawLine);

                if (line.isEmpty()
                        || isLikelyPageNumber(line)) {
                    continue;
                }

                Section detectedSection =
                        detectSectionHeading(line);

                if (detectedSection != null) {
                    currentSection =
                            detectedSection;
                    continue;
                }

                if (EXAMPLE_LINE.matcher(line).find()) {
                    currentSection =
                            Section.EXAMPLES;
                } else if (QUESTION_LINE
                        .matcher(line).find()) {
                    currentSection =
                            Section.EXERCISES;
                }

                buffers.append(
                        currentSection,
                        line
                );
            }
        }

        String explanation =
                buffers.explanation.toString()
                        .trim();

        if (explanation.isEmpty()) {
            explanation =
                    firstNonEmpty(
                            buffers.introduction
                                    .toString(),
                            buffers.summary
                                    .toString(),
                            "Readable detailed explanation "
                                    + "was not detected."
                    );
        }

        return new ExtractedChapterContent(
                chapterOrder,
                chapterTitle.trim(),
                startPage,
                endPage,
                buffers.introduction
                        .toString().trim(),
                explanation,
                buffers.keyPoints
                        .toString().trim(),
                buffers.examples
                        .toString().trim(),
                buffers.exercises
                        .toString().trim(),
                buffers.summary
                        .toString().trim(),
                sourcePages,
                calculateConfidence(buffers)
        );
    }

    private static void validateArguments(
            @NonNull BookPageOcrScanner.BookOcrResult
                    ocrResult,
            int chapterOrder,
            @NonNull String chapterTitle,
            int startPage,
            int endPage
    ) {
        if (chapterOrder <= 0) {
            throw new IllegalArgumentException(
                    "A valid chapter order is required."
            );
        }

        if (safeText(chapterTitle).isEmpty()) {
            throw new IllegalArgumentException(
                    "Chapter title is required."
            );
        }

        if (startPage <= 0
                || endPage < startPage) {
            throw new IllegalArgumentException(
                    "A valid chapter page range is required."
            );
        }

        if (ocrResult.getPages().isEmpty()) {
            throw new IllegalArgumentException(
                    "Book OCR pages are required."
            );
        }
    }

    private static Section detectSectionHeading(
            @NonNull String line
    ) {
        if (line.length() > MAX_SECTION_HEADING_LENGTH) {
            return null;
        }

        String normalized =
                normalizeHeading(line);

        if (INTRODUCTION_HEADING
                .matcher(normalized).matches()) {
            return Section.INTRODUCTION;
        }

        if (KEY_POINTS_HEADING
                .matcher(normalized).matches()) {
            return Section.KEY_POINTS;
        }

        if (EXAMPLE_HEADING
                .matcher(normalized).matches()) {
            return Section.EXAMPLES;
        }

        if (EXERCISE_HEADING
                .matcher(normalized).matches()) {
            return Section.EXERCISES;
        }

        if (SUMMARY_HEADING
                .matcher(normalized).matches()) {
            return Section.SUMMARY;
        }

        return null;
    }

    @NonNull
    private static Pattern headingPattern(
            @NonNull String alternatives
    ) {
        return Pattern.compile(
                "^(chapter\\s+)?(" + alternatives + ")"
                        + "\\s*[:.\\-–—]?$",
                Pattern.CASE_INSENSITIVE
                        | Pattern.UNICODE_CASE
        );
    }

    @NonNull
    private static String normalizeHeading(
            @NonNull String line
    ) {
        return line.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isLikelyPageNumber(
            @NonNull String line
    ) {
        return line.matches(
                "^[\\-–—]?[0-9०-९]{1,4}[\\-–—]?$"
        );
    }

    private static float calculateConfidence(
            @NonNull SectionBuffers buffers
    ) {
        int detectedSections = 0;

        if (buffers.introduction.length() > 0) {
            detectedSections++;
        }

        if (buffers.explanation.length() > 0) {
            detectedSections++;
        }

        if (buffers.keyPoints.length() > 0) {
            detectedSections++;
        }

        if (buffers.examples.length() > 0) {
            detectedSections++;
        }

        if (buffers.exercises.length() > 0) {
            detectedSections++;
        }

        if (buffers.summary.length() > 0) {
            detectedSections++;
        }

        float confidence =
                0.35f
                        + detectedSections * 0.10f;

        return Math.min(
                0.95f,
                confidence
        );
    }

    @NonNull
    private static String firstNonEmpty(
            @NonNull String first,
            @NonNull String second,
            @NonNull String fallback
    ) {
        String safeFirst =
                safeText(first);

        if (!safeFirst.isEmpty()) {
            return safeFirst;
        }

        String safeSecond =
                safeText(second);

        return safeSecond.isEmpty()
                ? fallback
                : safeSecond;
    }

    @NonNull
    private static String normalizeLine(
            String value
    ) {
        return safeText(value)
                .replaceAll("[\\t ]+", " ")
                .trim();
    }

    @NonNull
    private static String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private enum Section {
        INTRODUCTION,
        EXPLANATION,
        KEY_POINTS,
        EXAMPLES,
        EXERCISES,
        SUMMARY
    }

    private static final class SectionBuffers {

        @NonNull
        private final StringBuilder introduction =
                new StringBuilder();

        @NonNull
        private final StringBuilder explanation =
                new StringBuilder();

        @NonNull
        private final StringBuilder keyPoints =
                new StringBuilder();

        @NonNull
        private final StringBuilder examples =
                new StringBuilder();

        @NonNull
        private final StringBuilder exercises =
                new StringBuilder();

        @NonNull
        private final StringBuilder summary =
                new StringBuilder();

        private void append(
                @NonNull Section section,
                @NonNull String line
        ) {
            StringBuilder target;

            switch (section) {
                case INTRODUCTION:
                    target = introduction;
                    break;

                case KEY_POINTS:
                    target = keyPoints;
                    break;

                case EXAMPLES:
                    target = examples;
                    break;

                case EXERCISES:
                    target = exercises;
                    break;

                case SUMMARY:
                    target = summary;
                    break;

                case EXPLANATION:
                default:
                    target = explanation;
                    break;
            }

            if (target.length() > 0) {
                target.append('\n');
            }

            target.append(line);
        }
    }

    public static final class ExtractedChapterContent {

        private final int chapterOrder;

        @NonNull
        private final String chapterTitle;

        private final int startPage;
        private final int endPage;

        @NonNull
        private final String introduction;

        @NonNull
        private final String detailedExplanation;

        @NonNull
        private final String keyPoints;

        @NonNull
        private final String examples;

        @NonNull
        private final String exercises;

        @NonNull
        private final String summary;

        @NonNull
        private final List<Integer> sourcePageNumbers;

        private final float extractionConfidence;

        private ExtractedChapterContent(
                int chapterOrder,
                @NonNull String chapterTitle,
                int startPage,
                int endPage,
                @NonNull String introduction,
                @NonNull String detailedExplanation,
                @NonNull String keyPoints,
                @NonNull String examples,
                @NonNull String exercises,
                @NonNull String summary,
                @NonNull List<Integer> sourcePageNumbers,
                float extractionConfidence
        ) {
            this.chapterOrder = chapterOrder;
            this.chapterTitle = chapterTitle;
            this.startPage = startPage;
            this.endPage = endPage;
            this.introduction = introduction;
            this.detailedExplanation =
                    detailedExplanation;
            this.keyPoints = keyPoints;
            this.examples = examples;
            this.exercises = exercises;
            this.summary = summary;
            this.sourcePageNumbers =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    sourcePageNumbers
                            )
                    );
            this.extractionConfidence =
                    extractionConfidence;
        }

        public int getChapterOrder() {
            return chapterOrder;
        }

        @NonNull
        public String getChapterTitle() {
            return chapterTitle;
        }

        public int getStartPage() {
            return startPage;
        }

        public int getEndPage() {
            return endPage;
        }

        @NonNull
        public String getIntroduction() {
            return introduction;
        }

        @NonNull
        public String getDetailedExplanation() {
            return detailedExplanation;
        }

        @NonNull
        public String getKeyPoints() {
            return keyPoints;
        }

        @NonNull
        public String getExamples() {
            return examples;
        }

        @NonNull
        public String getExercises() {
            return exercises;
        }

        @NonNull
        public String getSummary() {
            return summary;
        }

        @NonNull
        public List<Integer> getSourcePageNumbers() {
            return sourcePageNumbers;
        }

        public float getExtractionConfidence() {
            return extractionConfidence;
        }

        public boolean hasExamples() {
            return !examples.isEmpty();
        }

        public boolean hasExercises() {
            return !exercises.isEmpty();
        }

        public boolean requiresCarefulReview() {
            return extractionConfidence < 0.75f;
        }
    }
}
