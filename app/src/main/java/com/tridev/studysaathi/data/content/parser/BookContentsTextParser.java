package com.tridev.studysaathi.data.content.parser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model
        .BookContentsScanResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BookContentsTextParser {

    private static final int MAX_CANDIDATE_TITLE_LENGTH =
            200;

    private static final int MAX_REASONABLE_PAGE_NUMBER =
            5000;

    /*
     * Line के अंत में मौजूद page number पहचानता है।
     *
     * उदाहरण:
     * Knowing Our Numbers ........ 1
     * Whole Numbers 25
     */
    @NonNull
    private static final Pattern TRAILING_PAGE_PATTERN =
            Pattern.compile(
                    "^(.*?)(?:\\.{2,}|\\s{2,}|\\s)"
                            + "([0-9]{1,4})\\s*$"
            );

    /*
     * Keyword + number + title या number + title.
     *
     * उदाहरण:
     * Chapter 1 Knowing Our Numbers
     * अध्याय 2 पूर्ण संख्याएँ
     * 3 Playing with Numbers
     */
    @NonNull
    private static final Pattern NUMBERED_CHAPTER_PATTERN =
            Pattern.compile(
                    "^(?:"
                            + "(chapter|unit|lesson|poem|story|"
                            + "activity|project|appendix|"
                            + "अध्याय|इकाई|पाठ|कविता|कहानी|"
                            + "गतिविधि|परियोजना|परिशिष्ट)"
                            + "\\s*"
                            + ")?"
                            + "([0-9]+(?:\\.[0-9]+)*)"
                            + "\\s*[.\\-:–—)]*\\s+"
                            + "(.+)$",
                    Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE
            );

    /*
     * Keyword मौजूद हो लेकिन chapter number न हो।
     *
     * उदाहरण:
     * Chapter Knowing Our Numbers
     * कविता वह चिड़िया जो
     */
    @NonNull
    private static final Pattern KEYWORD_TITLE_PATTERN =
            Pattern.compile(
                    "^(chapter|unit|lesson|poem|story|"
                            + "activity|project|appendix|"
                            + "अध्याय|इकाई|पाठ|कविता|कहानी|"
                            + "गतिविधि|परियोजना|परिशिष्ट)"
                            + "\\s*[:\\-–—]*\\s+"
                            + "(.+)$",
                    Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE
            );

    /*
     * सामान्य Contents/Index headings जिन्हें chapter नहीं मानना है।
     */
    @NonNull
    private static final Set<String> IGNORED_HEADINGS =
            createIgnoredHeadings();

    private BookContentsTextParser() {
        // Utility class.
    }

    /**
     * OCR text को chapter candidates में बदलता है।
     */
    @NonNull
    public static BookContentsScanResult parse(
            long bookRowId,
            @Nullable String sourceImagePath,
            @Nullable String detectedText
    ) {
        if (bookRowId <= 0L) {
            return BookContentsScanResult.failure(
                    bookRowId,
                    sourceImagePath,
                    "A valid school book is required."
            );
        }

        String normalizedText =
                normalizeMultilineText(
                        detectedText
                );

        if (normalizedText.isEmpty()) {
            return BookContentsScanResult.failure(
                    bookRowId,
                    sourceImagePath,
                    "No readable text was found on the contents page."
            );
        }

        String[] detectedLines =
                normalizedText.split(
                        "\\n"
                );

        List<BookContentsScanResult.ChapterCandidate> candidates =
                new ArrayList<>();

        Set<String> uniqueCandidateKeys =
                new HashSet<>();

        for (int lineIndex = 0;
             lineIndex < detectedLines.length;
             lineIndex++) {

            String originalLine =
                    safeSingleLineText(
                            detectedLines[lineIndex]
                    );

            if (shouldIgnoreLine(
                    originalLine
            )) {
                continue;
            }

            ParsedLine parsedLine =
                    parseLine(
                            originalLine,
                            lineIndex + 1
                    );

            if (parsedLine == null
                    || !parsedLine.isUsable()) {

                continue;
            }

            String uniqueKey =
                    createUniqueKey(
                            parsedLine
                    );

            if (!uniqueCandidateKeys.add(
                    uniqueKey
            )) {
                continue;
            }

            candidates.add(
                    BookContentsScanResult
                            .ChapterCandidate
                            .create(
                                    parsedLine.chapterNumber,
                                    parsedLine.chapterTitle,
                                    parsedLine.startPageNumber,
                                    parsedLine.sourceLineNumber,
                                    originalLine,
                                    parsedLine.confidence
                            )
            );
        }

        return BookContentsScanResult.success(
                bookRowId,
                sourceImagePath,
                normalizedText,
                candidates
        );
    }

    @Nullable
    private static ParsedLine parseLine(
            @NonNull String originalLine,
            int sourceLineNumber
    ) {
        String normalizedLine =
                normalizeDigits(
                        originalLine
                );

        PageExtraction pageExtraction =
                extractTrailingPageNumber(
                        normalizedLine
                );

        String lineWithoutPage =
                pageExtraction.textWithoutPage;

        Matcher numberedMatcher =
                NUMBERED_CHAPTER_PATTERN.matcher(
                        lineWithoutPage
                );

        if (numberedMatcher.matches()) {
            String keyword =
                    safeSingleLineText(
                            numberedMatcher.group(
                                    1
                            )
                    );

            String chapterNumber =
                    safeSingleLineText(
                            numberedMatcher.group(
                                    2
                            )
                    );

            String chapterTitle =
                    cleanChapterTitle(
                            numberedMatcher.group(
                                    3
                            )
                    );

            float confidence =
                    calculateConfidence(
                            !keyword.isEmpty(),
                            !chapterNumber.isEmpty(),
                            pageExtraction.pageNumber > 0,
                            chapterTitle
                    );

            return new ParsedLine(
                    chapterNumber,
                    chapterTitle,
                    pageExtraction.pageNumber,
                    sourceLineNumber,
                    confidence
            );
        }

        Matcher keywordMatcher =
                KEYWORD_TITLE_PATTERN.matcher(
                        lineWithoutPage
                );

        if (keywordMatcher.matches()) {
            String chapterTitle =
                    cleanChapterTitle(
                            keywordMatcher.group(
                                    2
                            )
                    );

            float confidence =
                    calculateConfidence(
                            true,
                            false,
                            pageExtraction.pageNumber > 0,
                            chapterTitle
                    );

            return new ParsedLine(
                    "",
                    chapterTitle,
                    pageExtraction.pageNumber,
                    sourceLineNumber,
                    confidence
            );
        }

        /*
         * Keyword/number न मिलने पर केवल उन lines को candidate
         * माना जाएगा जिनके अंत में valid page number मौजूद है।
         */
        if (pageExtraction.pageNumber > 0) {
            String chapterTitle =
                    cleanChapterTitle(
                            lineWithoutPage
                    );

            float confidence =
                    calculateConfidence(
                            false,
                            false,
                            true,
                            chapterTitle
                    );

            return new ParsedLine(
                    "",
                    chapterTitle,
                    pageExtraction.pageNumber,
                    sourceLineNumber,
                    confidence
            );
        }

        return null;
    }

    @NonNull
    private static PageExtraction extractTrailingPageNumber(
            @NonNull String line
    ) {
        Matcher matcher =
                TRAILING_PAGE_PATTERN.matcher(
                        line
                );

        if (!matcher.matches()) {
            return new PageExtraction(
                    line,
                    0
            );
        }

        String textWithoutPage =
                safeSingleLineText(
                        matcher.group(
                                1
                        )
                )
                        .replaceAll(
                                "[.·•\\-–—\\s]+$",
                                ""
                        )
                        .trim();

        int pageNumber =
                parsePageNumber(
                        matcher.group(
                                2
                        )
                );

        if (pageNumber <= 0) {
            return new PageExtraction(
                    line,
                    0
            );
        }

        return new PageExtraction(
                textWithoutPage,
                pageNumber
        );
    }

    private static int parsePageNumber(
            @Nullable String pageText
    ) {
        String safePageText =
                safeSingleLineText(
                        pageText
                );

        if (safePageText.isEmpty()) {
            return 0;
        }

        try {
            int pageNumber =
                    Integer.parseInt(
                            safePageText
                    );

            if (pageNumber <= 0
                    || pageNumber
                    > MAX_REASONABLE_PAGE_NUMBER) {

                return 0;
            }

            return pageNumber;

        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static boolean shouldIgnoreLine(
            @NonNull String line
    ) {
        if (line.length() < 2) {
            return true;
        }

        String normalizedHeading =
                line.toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^\\p{L}\\p{N}]+",
                                " "
                        )
                        .trim();

        if (IGNORED_HEADINGS.contains(
                normalizedHeading
        )) {
            return true;
        }

        /*
         * केवल page-number वाली line chapter नहीं है।
         */
        if (normalizedHeading.matches(
                "[0-9]+"
        )) {
            return true;
        }

        /*
         * अत्यधिक लंबी OCR paragraph line Contents entry नहीं मानी जाएगी।
         */
        return line.length()
                > MAX_CANDIDATE_TITLE_LENGTH + 40;
    }

    @NonNull
    private static String cleanChapterTitle(
            @Nullable String rawTitle
    ) {
        String title =
                safeSingleLineText(
                        rawTitle
                );

        title =
                title.replaceAll(
                        "^[.\\-:–—)\\s]+",
                        ""
                );

        title =
                title.replaceAll(
                        "[.·•\\-–—\\s]+$",
                        ""
                );

        return title.trim();
    }

    private static float calculateConfidence(
            boolean hasKeyword,
            boolean hasChapterNumber,
            boolean hasPageNumber,
            @NonNull String chapterTitle
    ) {
        float confidence =
                0.30F;

        if (hasKeyword) {
            confidence +=
                    0.25F;
        }

        if (hasChapterNumber) {
            confidence +=
                    0.20F;
        }

        if (hasPageNumber) {
            confidence +=
                    0.20F;
        }

        int titleLength =
                chapterTitle.length();

        if (titleLength >= 3
                && titleLength <= 120) {

            confidence +=
                    0.05F;
        }

        return Math.max(
                0F,
                Math.min(
                        1F,
                        confidence
                )
        );
    }

    @NonNull
    private static String createUniqueKey(
            @NonNull ParsedLine parsedLine
    ) {
        return parsedLine.chapterNumber
                .toLowerCase(
                        Locale.ROOT
                )
                + "|"
                + parsedLine.chapterTitle
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    private static String normalizeDigits(
            @NonNull String value
    ) {
        StringBuilder normalized =
                new StringBuilder(
                        value.length()
                );

        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(
                            index
                    );

            switch (character) {
                case '०':
                    normalized.append(
                            '0'
                    );
                    break;

                case '१':
                    normalized.append(
                            '1'
                    );
                    break;

                case '२':
                    normalized.append(
                            '2'
                    );
                    break;

                case '३':
                    normalized.append(
                            '3'
                    );
                    break;

                case '४':
                    normalized.append(
                            '4'
                    );
                    break;

                case '५':
                    normalized.append(
                            '5'
                    );
                    break;

                case '६':
                    normalized.append(
                            '6'
                    );
                    break;

                case '७':
                    normalized.append(
                            '7'
                    );
                    break;

                case '८':
                    normalized.append(
                            '8'
                    );
                    break;

                case '९':
                    normalized.append(
                            '9'
                    );
                    break;

                default:
                    normalized.append(
                            character
                    );
                    break;
            }
        }

        return normalized.toString();
    }

    @NonNull
    private static Set<String> createIgnoredHeadings() {
        Set<String> ignoredHeadings =
                new HashSet<>();

        ignoredHeadings.add(
                "contents"
        );

        ignoredHeadings.add(
                "table of contents"
        );

        ignoredHeadings.add(
                "index"
        );

        ignoredHeadings.add(
                "chapter"
        );

        ignoredHeadings.add(
                "chapters"
        );

        ignoredHeadings.add(
                "unit"
        );

        ignoredHeadings.add(
                "units"
        );

        ignoredHeadings.add(
                "page"
        );

        ignoredHeadings.add(
                "page no"
        );

        ignoredHeadings.add(
                "page number"
        );

        ignoredHeadings.add(
                "विषय सूची"
        );

        ignoredHeadings.add(
                "विषयसूची"
        );

        ignoredHeadings.add(
                "अनुक्रमणिका"
        );

        ignoredHeadings.add(
                "अध्याय"
        );

        ignoredHeadings.add(
                "पृष्ठ"
        );

        ignoredHeadings.add(
                "पृष्ठ संख्या"
        );

        return ignoredHeadings;
    }

    @NonNull
    private static String safeSingleLineText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    private static String normalizeMultilineText(
            @Nullable Object value
    ) {
        if (value == null) {
            return "";
        }

        return value.toString()
                .replace(
                        "\r\n",
                        "\n"
                )
                .replace(
                        "\r",
                        "\n"
                )
                .trim();
    }

    private static final class PageExtraction {

        @NonNull
        private final String textWithoutPage;

        private final int pageNumber;

        private PageExtraction(
                @NonNull String textWithoutPage,
                int pageNumber
        ) {
            this.textWithoutPage =
                    textWithoutPage;

            this.pageNumber =
                    Math.max(
                            0,
                            pageNumber
                    );
        }
    }

    private static final class ParsedLine {

        @NonNull
        private final String chapterNumber;

        @NonNull
        private final String chapterTitle;

        private final int startPageNumber;

        private final int sourceLineNumber;

        private final float confidence;

        private ParsedLine(
                @NonNull String chapterNumber,
                @NonNull String chapterTitle,
                int startPageNumber,
                int sourceLineNumber,
                float confidence
        ) {
            this.chapterNumber =
                    chapterNumber;

            this.chapterTitle =
                    chapterTitle;

            this.startPageNumber =
                    Math.max(
                            0,
                            startPageNumber
                    );

            this.sourceLineNumber =
                    Math.max(
                            0,
                            sourceLineNumber
                    );

            this.confidence =
                    Math.max(
                            0F,
                            Math.min(
                                    1F,
                                    confidence
                            )
                    );
        }

        private boolean isUsable() {
            if (chapterTitle.length() < 2
                    || chapterTitle.length()
                    > MAX_CANDIDATE_TITLE_LENGTH) {

                return false;
            }

            return confidence >= 0.45F;
        }
    }
}