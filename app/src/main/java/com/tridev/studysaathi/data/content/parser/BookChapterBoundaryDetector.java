package com.tridev.studysaathi.data.content.parser;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.content.scanner
        .BookPageOcrScanner;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page-wise OCR text से संभावित chapter start/end boundaries निकालता है।
 * Result हमेशा Parent review के लिए candidate है, automatic approval नहीं।
 */
public final class BookChapterBoundaryDetector {

    private static final int MAX_HEADING_LINE_LENGTH = 120;
    private static final int MAX_LINES_CHECKED_PER_PAGE = 18;

    private static final Pattern EXPLICIT_HEADING_PATTERN =
            Pattern.compile(
                    "^(chapter|unit|lesson|"
                            + "\\u0905\\u0927\\u094d\\u092f\\u093e\\u092f|"
                            + "\\u092a\\u093e\\u0920|"
                            + "\\u0907\\u0915\\u093e\\u0908)"
                            + "\\s*[-.:\\u2013\\u2014]?\\s*"
                            + "([0-9\\u0966-\\u096fivxlcdm]+|"
                            + "one|two|three|four|five|six|seven|"
                            + "eight|nine|ten|eleven|twelve)?"
                            + "\\s*[-.:\\u2013\\u2014]?\\s*(.*)$",
                    Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE
            );

    private static final Pattern COVER_NUMBER_TITLE_PATTERN =
            Pattern.compile(
                    "^([0-9\\u0966-\\u096f]{1,2})"
                            + "\\s+(.{3,80})$"
            );

    private static final Pattern STANDALONE_CHAPTER_NUMBER_PATTERN =
            Pattern.compile(
                    "^[0-9\\u0966-\\u096f]{1,2}$"
            );

    private static final Pattern BARE_EXPLICIT_HEADING_PATTERN =
            Pattern.compile(
                    "^(chapter|unit|lesson|"
                            + "अध्याय|पाठ|इकाई)"
                            + "\\s+([0-9०-९ivxlcdm]+)$",
                    Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE
            );

    private BookChapterBoundaryDetector() {
        throw new AssertionError(
                "BookChapterBoundaryDetector "
                        + "cannot be instantiated."
        );
    }

    @NonNull
    public static DetectionResult detect(
            @NonNull BookPageOcrScanner.BookOcrResult
                    ocrResult
    ) {
        List<BookPageOcrScanner.PageOcrText> pages =
                ocrResult.getPages();

        if (pages.isEmpty()) {
            return DetectionResult.failure(
                    ocrResult.getRequestId(),
                    ocrResult.getSchoolBookRowId(),
                    "No OCR pages are available for "
                            + "chapter detection."
            );
        }

        ArrayList<StartCandidate> starts =
                new ArrayList<>();

        Set<String> acceptedHeadingKeys =
                new HashSet<>();

        for (BookPageOcrScanner.PageOcrText page
                : pages) {
            StartCandidate candidate =
                    detectPageStart(page);

            if (candidate == null) {
                continue;
            }

            String headingKey =
                    normalizeHeadingKey(
                            candidate.heading
                    );

            if (headingKey.isEmpty()
                    || acceptedHeadingKeys
                    .contains(headingKey)) {
                continue;
            }

            starts.add(candidate);
            acceptedHeadingKeys.add(headingKey);
        }

        if (starts.isEmpty()) {
            return DetectionResult.failure(
                    ocrResult.getRequestId(),
                    ocrResult.getSchoolBookRowId(),
                    "No clear chapter headings were detected. "
                            + "Parent can provide Contents pages "
                            + "or correct boundaries manually."
            );
        }

        ArrayList<ChapterBoundaryCandidate> boundaries =
                new ArrayList<>();

        int lastBookPageNumber =
                pages.get(pages.size() - 1)
                        .getPageNumber();

        for (int index = 0;
             index < starts.size();
             index++) {

            StartCandidate current =
                    starts.get(index);

            int endPage =
                    index + 1 < starts.size()
                            ? starts.get(index + 1)
                              .pageNumber - 1
                            : lastBookPageNumber;

            endPage =
                    Math.max(
                            current.pageNumber,
                            endPage
                    );

            boundaries.add(
                    new ChapterBoundaryCandidate(
                            index + 1,
                            current.heading,
                            current.title,
                            current.pageNumber,
                            endPage,
                            current.confidence,
                            current.detectionReason
                    )
            );
        }

        return DetectionResult.success(
                ocrResult.getRequestId(),
                ocrResult.getSchoolBookRowId(),
                pages.size(),
                boundaries
        );
    }

    /**
     * Parent द्वारा पहले से तय exact chapter title/page ranges को trusted
     * boundaries बनाता है। Image-based PDF में OCR heading न मिलने पर यही
     * source of truth होगा।
     */
    @NonNull
    public static DetectionResult fromExistingChapters(
            @NonNull String requestId,
            long schoolBookRowId,
            int scannedPageCount,
            @NonNull List<SchoolBookChapterEntity>
                    existingChapters
    ) {
        if (safeText(requestId).isEmpty()) {
            return DetectionResult.failure(
                    "",
                    schoolBookRowId,
                    "A valid import request is required."
            );
        }

        if (schoolBookRowId <= 0L
                || scannedPageCount <= 0) {
            return DetectionResult.failure(
                    requestId,
                    schoolBookRowId,
                    "Valid book and PDF page details are required."
            );
        }

        ArrayList<SchoolBookChapterEntity>
                usableChapters =
                new ArrayList<>();

        for (SchoolBookChapterEntity chapter
                : existingChapters) {

            if (chapter == null
                    || chapter.getBookRowId()
                    != schoolBookRowId
                    || chapter.getChapterRowId() <= 0L
                    || chapter.getStartPageNumber() <= 0
                    || chapter.getEndPageNumber()
                    < chapter.getStartPageNumber()) {
                continue;
            }

            usableChapters.add(chapter);
        }

        usableChapters.sort(
                (first, second) ->
                        Integer.compare(
                                first.getStartPageNumber(),
                                second.getStartPageNumber()
                        )
        );

        if (usableChapters.isEmpty()) {
            return DetectionResult.failure(
                    requestId,
                    schoolBookRowId,
                    "Existing chapters do not have valid "
                            + "Parent-reviewed page ranges."
            );
        }

        ArrayList<ChapterBoundaryCandidate> candidates =
                new ArrayList<>();

        int previousEndPage = 0;

        for (SchoolBookChapterEntity chapter
                : usableChapters) {

            int startPage =
                    chapter.getStartPageNumber();

            int endPage =
                    Math.min(
                            scannedPageCount,
                            chapter.getEndPageNumber()
                    );

            if (startPage > scannedPageCount
                    || endPage < startPage
                    || startPage <= previousEndPage) {
                continue;
            }

            String title =
                    firstNonEmpty(
                            chapter.getChapterTitleEnglish(),
                            chapter.getChapterTitleHindi(),
                            "Chapter "
                                    + (candidates.size() + 1)
                    );

            String chapterNumber =
                    safeText(
                            chapter.getChapterNumber()
                    );

            String heading =
                    chapterNumber.isEmpty()
                            ? title
                            : "Chapter "
                              + chapterNumber
                              + " - "
                              + title;

            candidates.add(
                    new ChapterBoundaryCandidate(
                            candidates.size() + 1,
                            heading,
                            title,
                            startPage,
                            endPage,
                            1.0f,
                            "Existing Parent-reviewed "
                                    + "chapter page range"
                    )
            );

            previousEndPage = endPage;
        }

        if (candidates.isEmpty()) {
            return DetectionResult.failure(
                    requestId,
                    schoolBookRowId,
                    "Existing chapter page ranges do not "
                            + "fit this PDF."
            );
        }

        return DetectionResult.success(
                requestId,
                schoolBookRowId,
                scannedPageCount,
                candidates
        );
    }

    private static StartCandidate detectPageStart(
            @NonNull BookPageOcrScanner.PageOcrText page
    ) {
        String text =
                safeText(page.getCombinedText());

        if (text.isEmpty()) {
            return null;
        }

        String[] lines =
                text.split("\\R");

        StartCandidate bestCandidate =
                null;

        int checkedLines =
                Math.min(
                        lines.length,
                        MAX_LINES_CHECKED_PER_PAGE
                );

        for (int lineIndex = 0;
             lineIndex < checkedLines;
             lineIndex++) {

            String line =
                    normalizeLine(lines[lineIndex]);

            if (lineIndex <= 5
                    && STANDALONE_CHAPTER_NUMBER_PATTERN
                    .matcher(line).matches()) {

                String coverTitle =
                        findNextCoverTitle(
                                lines,
                                lineIndex + 1,
                                checkedLines
                        );

                if (!coverTitle.isEmpty()) {
                    StartCandidate coverCandidate =
                            new StartCandidate(
                                    page.getPageNumber(),
                                    line + " " + coverTitle,
                                    coverTitle,
                                    0.97f,
                                    "Chapter-cover number and title"
                            );

                    if (bestCandidate == null
                            || coverCandidate.confidence
                            > bestCandidate.confidence) {
                        bestCandidate =
                                coverCandidate;
                    }
                }
            }

            if (line.length() < 3
                    || line.length()
                    > MAX_HEADING_LINE_LENGTH) {
                continue;
            }

            StartCandidate candidate =
                    parseHeading(
                            page.getPageNumber(),
                            line,
                            lineIndex
                    );

            if (candidate == null) {
                continue;
            }

            if (bestCandidate == null
                    || candidate.confidence
                    > bestCandidate.confidence) {
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    private static StartCandidate parseHeading(
            int pageNumber,
            @NonNull String line,
            int lineIndex
    ) {
        Matcher explicitMatcher =
                EXPLICIT_HEADING_PATTERN
                        .matcher(line);

        if (explicitMatcher.matches()) {
            String type =
                    safeText(
                            explicitMatcher.group(1)
                    );

            String number =
                    safeText(
                            explicitMatcher.group(2)
                    );

            String trailingTitle =
                    safeText(
                            explicitMatcher.group(3)
                    );

            String title =
                    trailingTitle.isEmpty()
                            ? joinNonEmpty(
                            type,
                            number
                    )
                            : trailingTitle;

            float confidence =
                    lineIndex <= 5
                            ? 0.94f
                            : 0.86f;

            if (number.isEmpty()) {
                confidence -= 0.12f;
            }

            if (trailingTitle.isEmpty()) {
                confidence -= 0.05f;
            }

            return new StartCandidate(
                    pageNumber,
                    line,
                    title,
                    clampConfidence(confidence),
                    "Explicit chapter/unit/lesson heading"
            );
        }

        if (lineIndex > 5) {
            return null;
        }

        Matcher numberedMatcher =
                COVER_NUMBER_TITLE_PATTERN
                        .matcher(line);

        if (!numberedMatcher.matches()) {
            return null;
        }

        String title =
                safeText(
                        numberedMatcher.group(2)
                );

        if (!looksLikeCoverTitle(title)) {
            return null;
        }

        return new StartCandidate(
                pageNumber,
                line,
                title,
                0.92f,
                "Chapter-cover number and short title"
        );
    }

    @NonNull
    private static String findNextCoverTitle(
            @NonNull String[] lines,
            int startIndex,
            int checkedLines
    ) {
        int endIndex =
                Math.min(
                        checkedLines,
                        startIndex + 3
                );

        for (int index = startIndex;
             index < endIndex;
             index++) {

            String candidate =
                    normalizeLine(lines[index]);

            if (candidate.isEmpty()) {
                continue;
            }

            return looksLikeCoverTitle(candidate)
                    ? candidate
                    : "";
        }

        return "";
    }

    private static boolean looksLikeCoverTitle(
            @NonNull String value
    ) {
        if (value.length() < 3
                || value.length() > 80) {
            return false;
        }

        String[] words =
                value.trim().split("\\s+");

        if (words.length > 6
                || value.endsWith(".")
                || value.endsWith("?")
                || value.endsWith("!")) {
            return false;
        }

        String lowerValue =
                value.toLowerCase(Locale.ROOT);

        if (lowerValue.contains("exercise")
                || lowerValue.contains("example")
                || lowerValue.contains("question")
                || lowerValue.contains("solution")
                || lowerValue.contains("answer")
                || lowerValue.contains("\u0905\u092d\u094d\u092f\u093e\u0938")
                || lowerValue.contains("\u0909\u0926\u093e\u0939\u0930\u0923")
                || lowerValue.contains("\u092a\u094d\u0930\u0936\u094d\u0928")) {
            return false;
        }

        int letters = 0;

        for (int index = 0;
             index < value.length();
             index++) {

            if (Character.isLetter(
                    value.charAt(index)
            )) {
                letters++;
            }
        }

        if (letters < 3) {
            return false;
        }

        int titleCaseWords = 0;

        for (String word : words) {
            if (!word.isEmpty()
                    && Character.isUpperCase(
                    word.charAt(0)
            )) {
                titleCaseWords++;
            }
        }

        return titleCaseWords >= 1
                || containsDevanagari(value);
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

    @NonNull
    private static String normalizeLine(
            String value
    ) {
        return safeText(value)
                .replaceAll("\\s+", " ")
                .replace('।', ' ')
                .trim();
    }

    @NonNull
    private static String normalizeHeadingKey(
            @NonNull String heading
    ) {
        String normalized =
                normalizeLine(heading)
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{N}]+", "");

        Matcher bareMatcher =
                BARE_EXPLICIT_HEADING_PATTERN
                        .matcher(
                                normalizeLine(heading)
                        );

        if (bareMatcher.matches()) {
            return normalized;
        }

        return normalized;
    }

    @NonNull
    private static String joinNonEmpty(
            @NonNull String first,
            @NonNull String second
    ) {
        if (first.isEmpty()) {
            return second;
        }

        if (second.isEmpty()) {
            return first;
        }

        return first + " " + second;
    }

    @NonNull
    private static String firstNonEmpty(
            String first,
            String second,
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

    private static float clampConfidence(
            float value
    ) {
        return Math.max(
                0.0f,
                Math.min(1.0f, value)
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

    private static final class StartCandidate {

        private final int pageNumber;

        @NonNull
        private final String heading;

        @NonNull
        private final String title;

        private final float confidence;

        @NonNull
        private final String detectionReason;

        private StartCandidate(
                int pageNumber,
                @NonNull String heading,
                @NonNull String title,
                float confidence,
                @NonNull String detectionReason
        ) {
            this.pageNumber = pageNumber;
            this.heading = heading;
            this.title = title;
            this.confidence = confidence;
            this.detectionReason = detectionReason;
        }
    }

    public static final class DetectionResult
            implements Serializable {

        private static final long serialVersionUID = 1L;

        @NonNull
        private final String requestId;

        private final long schoolBookRowId;
        private final int scannedPageCount;

        @NonNull
        private final List<ChapterBoundaryCandidate>
                chapterCandidates;

        @NonNull
        private final String errorMessage;

        private DetectionResult(
                @NonNull String requestId,
                long schoolBookRowId,
                int scannedPageCount,
                @NonNull List<ChapterBoundaryCandidate>
                        chapterCandidates,
                @NonNull String errorMessage
        ) {
            this.requestId = requestId;
            this.schoolBookRowId = schoolBookRowId;
            this.scannedPageCount = scannedPageCount;
            this.chapterCandidates =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    chapterCandidates
                            )
                    );
            this.errorMessage = errorMessage;
        }

        @NonNull
        private static DetectionResult success(
                @NonNull String requestId,
                long schoolBookRowId,
                int scannedPageCount,
                @NonNull List<ChapterBoundaryCandidate>
                        candidates
        ) {
            return new DetectionResult(
                    requestId,
                    schoolBookRowId,
                    scannedPageCount,
                    candidates,
                    ""
            );
        }

        @NonNull
        private static DetectionResult failure(
                @NonNull String requestId,
                long schoolBookRowId,
                @NonNull String errorMessage
        ) {
            return new DetectionResult(
                    requestId,
                    schoolBookRowId,
                    0,
                    Collections.emptyList(),
                    errorMessage
            );
        }

        public boolean isSuccessful() {
            return errorMessage.isEmpty()
                    && !chapterCandidates.isEmpty();
        }

        @NonNull
        public String getRequestId() {
            return requestId;
        }

        public long getSchoolBookRowId() {
            return schoolBookRowId;
        }

        public int getScannedPageCount() {
            return scannedPageCount;
        }

        @NonNull
        public List<ChapterBoundaryCandidate>
        getChapterCandidates() {
            return chapterCandidates;
        }

        @NonNull
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static final class ChapterBoundaryCandidate
            implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int detectedOrder;

        @NonNull
        private final String detectedHeading;

        @NonNull
        private final String suggestedTitle;

        private final int startPage;
        private final int endPage;
        private final float confidence;

        @NonNull
        private final String detectionReason;

        private ChapterBoundaryCandidate(
                int detectedOrder,
                @NonNull String detectedHeading,
                @NonNull String suggestedTitle,
                int startPage,
                int endPage,
                float confidence,
                @NonNull String detectionReason
        ) {
            this.detectedOrder = detectedOrder;
            this.detectedHeading = detectedHeading;
            this.suggestedTitle = suggestedTitle;
            this.startPage = startPage;
            this.endPage = endPage;
            this.confidence = confidence;
            this.detectionReason = detectionReason;
        }

        public int getDetectedOrder() {
            return detectedOrder;
        }

        @NonNull
        public String getDetectedHeading() {
            return detectedHeading;
        }

        @NonNull
        public String getSuggestedTitle() {
            return suggestedTitle;
        }

        public int getStartPage() {
            return startPage;
        }

        public int getEndPage() {
            return endPage;
        }

        public float getConfidence() {
            return confidence;
        }

        @NonNull
        public String getDetectionReason() {
            return detectionReason;
        }

        public boolean requiresCarefulReview() {
            return confidence < 0.80f;
        }
    }
}
