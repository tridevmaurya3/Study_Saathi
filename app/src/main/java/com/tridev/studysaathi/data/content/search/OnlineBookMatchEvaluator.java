package com.tridev.studysaathi.data.content.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model.OnlineBookSearchResult;
import com.tridev.studysaathi.data.content.scanner.BookCoverMetadataExtractor;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class OnlineBookMatchEvaluator {

    private static final float ISBN_WEIGHT =
            36f;

    private static final float TITLE_WEIGHT =
            25f;

    private static final float SUBJECT_WEIGHT =
            12f;

    private static final float CLASS_WEIGHT =
            9f;

    private static final float PUBLISHER_WEIGHT =
            8f;

    private static final float BOARD_WEIGHT =
            5f;

    private static final float AUTHOR_WEIGHT =
            3f;

    private static final float OFFICIAL_SOURCE_BONUS =
            2f;

    private static final float MAXIMUM_SCORE =
            100f;

    /**
     * Evaluates one online result against the
     * information detected from the scanned cover.
     */
    @NonNull
    public MatchEvaluation evaluate(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata scannedBook,
            @NonNull OnlineBookSearchResult onlineBook
    ) {
        List<String> reasons =
                new ArrayList<>();

        List<String> warnings =
                new ArrayList<>();

        float isbnSimilarity =
                calculateIsbnSimilarity(
                        scannedBook.getIsbn10(),
                        scannedBook.getIsbn13(),
                        onlineBook.getIsbn10(),
                        onlineBook.getIsbn13()
                );

        float titleSimilarity =
                calculateTextSimilarity(
                        scannedBook.getBookTitle(),
                        onlineBook.getBookTitle()
                );

        float subjectSimilarity =
                calculateTextSimilarity(
                        scannedBook.getSubjectName(),
                        createSubjectSearchText(
                                onlineBook
                        )
                );

        float classSimilarity =
                calculateClassSimilarity(
                        scannedBook.getClassName(),
                        onlineBook.getClassName(),
                        onlineBook.getBookTitle(),
                        onlineBook.getDescription(),
                        onlineBook.getCategories()
                );

        float publisherSimilarity =
                calculateTextSimilarity(
                        scannedBook.getPublisherName(),
                        onlineBook.getPublisherName()
                );

        float boardSimilarity =
                calculateBoardSimilarity(
                        scannedBook.getEducationBoard(),
                        onlineBook
                );

        float authorSimilarity =
                calculateAuthorSimilarity(
                        scannedBook.getAuthorName(),
                        onlineBook.getAuthors()
                );

        float weightedScore =
                isbnSimilarity
                        * ISBN_WEIGHT / 100f
                        + titleSimilarity
                        * TITLE_WEIGHT / 100f
                        + subjectSimilarity
                        * SUBJECT_WEIGHT / 100f
                        + classSimilarity
                        * CLASS_WEIGHT / 100f
                        + publisherSimilarity
                        * PUBLISHER_WEIGHT / 100f
                        + boardSimilarity
                        * BOARD_WEIGHT / 100f
                        + authorSimilarity
                        * AUTHOR_WEIGHT / 100f;

        if (onlineBook.isOfficialSourceVerified()) {
            weightedScore +=
                    OFFICIAL_SOURCE_BONUS;

            reasons.add(
                    "Result comes from a verified official source."
            );
        }

        if (isbnSimilarity >= 100f) {
            reasons.add(
                    "ISBN exactly matches the scanned book."
            );

        } else if (isbnSimilarity >= 80f) {
            reasons.add(
                    "ISBN strongly matches the scanned book."
            );

        } else if (scannedBook.hasIsbn()
                && onlineBook.hasIsbn()
                && isbnSimilarity <= 0f) {

            warnings.add(
                    "The online ISBN does not match the scanned ISBN."
            );

            weightedScore -=
                    18f;
        }

        if (titleSimilarity >= 90f) {
            reasons.add(
                    "Book title is an excellent match."
            );

        } else if (titleSimilarity >= 70f) {
            reasons.add(
                    "Book title is a strong match."
            );

        } else if (titleSimilarity < 40f) {
            warnings.add(
                    "Book title is significantly different."
            );
        }

        if (subjectSimilarity >= 75f) {
            reasons.add(
                    "Subject information matches."
            );

        } else if (!scannedBook.getSubjectName().isEmpty()
                && subjectSimilarity < 35f) {

            warnings.add(
                    "Subject information could not be matched confidently."
            );
        }

        if (classSimilarity >= 80f) {
            reasons.add(
                    "Class or grade information matches."
            );

        } else if (!scannedBook.getClassName().isEmpty()
                && classSimilarity <= 0f) {

            warnings.add(
                    "Class or grade may be different."
            );
        }

        if (publisherSimilarity >= 80f) {
            reasons.add(
                    "Publisher information matches."
            );

        } else if (!scannedBook.getPublisherName().isEmpty()
                && !onlineBook.getPublisherName().isEmpty()
                && publisherSimilarity < 35f) {

            warnings.add(
                    "Publisher name appears to be different."
            );
        }

        if (boardSimilarity >= 80f) {
            reasons.add(
                    "Education board information matches."
            );
        }

        if (onlineBook.getBookTitle().isEmpty()) {
            weightedScore -=
                    25f;

            warnings.add(
                    "Online result does not contain a valid title."
            );
        }

        if (!onlineBook.hasIsbn()
                && onlineBook.getPublisherName().isEmpty()
                && onlineBook.getAuthors().isEmpty()) {

            weightedScore -=
                    8f;

            warnings.add(
                    "Online result contains limited verification information."
            );
        }

        float safeOverallScore =
                normalizeScore(
                        weightedScore
                );

        OnlineBookSearchResult.MatchStatus
                matchStatus =
                determineMatchStatus(
                        safeOverallScore,
                        isbnSimilarity,
                        titleSimilarity,
                        warnings
                );

        boolean automaticSelectionRecommended =
                isAutomaticSelectionRecommended(
                        safeOverallScore,
                        isbnSimilarity,
                        titleSimilarity,
                        onlineBook,
                        warnings
                );

        return new MatchEvaluation(
                safeOverallScore,
                titleSimilarity,
                isbnSimilarity,
                subjectSimilarity,
                classSimilarity,
                publisherSimilarity,
                boardSimilarity,
                authorSimilarity,
                matchStatus,
                automaticSelectionRecommended,
                reasons,
                warnings
        );
    }

    /**
     * Evaluates and ranks all online results.
     * Best matching result appears first.
     */
    @NonNull
    public List<RankedBookResult> evaluateAndRank(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata scannedBook,
            @NonNull List<OnlineBookSearchResult> onlineBooks
    ) {
        List<RankedBookResult> rankedResults =
                new ArrayList<>();

        for (OnlineBookSearchResult onlineBook :
                onlineBooks) {

            if (onlineBook == null) {
                continue;
            }

            MatchEvaluation evaluation =
                    evaluate(
                            scannedBook,
                            onlineBook
                    );

            rankedResults.add(
                    new RankedBookResult(
                            onlineBook,
                            evaluation
                    )
            );
        }

        rankedResults.sort(
                new Comparator<RankedBookResult>() {

                    @Override
                    public int compare(
                            RankedBookResult first,
                            RankedBookResult second
                    ) {
                        int scoreComparison =
                                Float.compare(
                                        second.getEvaluation()
                                                .getOverallMatchScore(),
                                        first.getEvaluation()
                                                .getOverallMatchScore()
                                );

                        if (scoreComparison != 0) {
                            return scoreComparison;
                        }

                        boolean firstOfficial =
                                first.getBookResult()
                                        .isOfficialSourceVerified();

                        boolean secondOfficial =
                                second.getBookResult()
                                        .isOfficialSourceVerified();

                        if (firstOfficial != secondOfficial) {
                            return secondOfficial
                                    ? 1
                                    : -1;
                        }

                        return first.getBookResult()
                                .getBookTitle()
                                .compareToIgnoreCase(
                                        second.getBookResult()
                                                .getBookTitle()
                                );
                    }
                }
        );

        return Collections.unmodifiableList(
                rankedResults
        );
    }

    @Nullable
    public RankedBookResult findBestMatch(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata scannedBook,
            @NonNull List<OnlineBookSearchResult> onlineBooks
    ) {
        List<RankedBookResult> rankedResults =
                evaluateAndRank(
                        scannedBook,
                        onlineBooks
                );

        if (rankedResults.isEmpty()) {
            return null;
        }

        return rankedResults.get(
                0
        );
    }

    private float calculateIsbnSimilarity(
            @Nullable String scannedIsbn10,
            @Nullable String scannedIsbn13,
            @Nullable String onlineIsbn10,
            @Nullable String onlineIsbn13
    ) {
        String safeScannedIsbn10 =
                normalizeIsbn(
                        scannedIsbn10
                );

        String safeScannedIsbn13 =
                normalizeIsbn(
                        scannedIsbn13
                );

        String safeOnlineIsbn10 =
                normalizeIsbn(
                        onlineIsbn10
                );

        String safeOnlineIsbn13 =
                normalizeIsbn(
                        onlineIsbn13
                );

        if (safeScannedIsbn13.isEmpty()
                && safeScannedIsbn10.isEmpty()) {

            return 0f;
        }

        if (safeOnlineIsbn13.isEmpty()
                && safeOnlineIsbn10.isEmpty()) {

            return 0f;
        }

        if (!safeScannedIsbn13.isEmpty()
                && safeScannedIsbn13.equals(
                safeOnlineIsbn13
        )) {
            return 100f;
        }

        if (!safeScannedIsbn10.isEmpty()
                && safeScannedIsbn10.equals(
                safeOnlineIsbn10
        )) {
            return 100f;
        }

        String convertedScannedIsbn13 =
                convertIsbn10ToIsbn13(
                        safeScannedIsbn10
                );

        if (!convertedScannedIsbn13.isEmpty()
                && convertedScannedIsbn13.equals(
                safeOnlineIsbn13
        )) {
            return 100f;
        }

        String convertedOnlineIsbn13 =
                convertIsbn10ToIsbn13(
                        safeOnlineIsbn10
                );

        if (!safeScannedIsbn13.isEmpty()
                && safeScannedIsbn13.equals(
                convertedOnlineIsbn13
        )) {
            return 100f;
        }

        if (!safeScannedIsbn13.isEmpty()
                && !safeOnlineIsbn13.isEmpty()) {

            return calculateCharacterSimilarity(
                    safeScannedIsbn13,
                    safeOnlineIsbn13
            );
        }

        return 0f;
    }

    private float calculateAuthorSimilarity(
            @Nullable String scannedAuthor,
            @NonNull List<String> onlineAuthors
    ) {
        String safeScannedAuthor =
                safeText(
                        scannedAuthor
                );

        if (safeScannedAuthor.isEmpty()
                || onlineAuthors.isEmpty()) {

            return 0f;
        }

        float bestSimilarity =
                0f;

        for (String onlineAuthor :
                onlineAuthors) {

            float similarity =
                    calculateTextSimilarity(
                            safeScannedAuthor,
                            onlineAuthor
                    );

            bestSimilarity =
                    Math.max(
                            bestSimilarity,
                            similarity
                    );
        }

        return bestSimilarity;
    }

    private float calculateClassSimilarity(
            @Nullable String scannedClass,
            @Nullable String onlineClass,
            @Nullable String onlineTitle,
            @Nullable String onlineDescription,
            @NonNull List<String> categories
    ) {
        int scannedClassNumber =
                extractClassNumber(
                        scannedClass
                );

        if (scannedClassNumber <= 0) {
            return 0f;
        }

        int directOnlineClassNumber =
                extractClassNumber(
                        onlineClass
                );

        if (directOnlineClassNumber > 0) {
            return directOnlineClassNumber
                    == scannedClassNumber
                    ? 100f
                    : 0f;
        }

        StringBuilder searchableText =
                new StringBuilder();

        appendSearchText(
                searchableText,
                onlineTitle
        );

        appendSearchText(
                searchableText,
                onlineDescription
        );

        for (String category : categories) {
            appendSearchText(
                    searchableText,
                    category
            );
        }

        int inferredClassNumber =
                extractClassNumber(
                        searchableText.toString()
                );

        if (inferredClassNumber <= 0) {
            return 0f;
        }

        return inferredClassNumber
                == scannedClassNumber
                ? 85f
                : 0f;
    }

    private float calculateBoardSimilarity(
            @Nullable String scannedBoard,
            @NonNull OnlineBookSearchResult onlineBook
    ) {
        String safeScannedBoard =
                safeText(
                        scannedBoard
                );

        if (safeScannedBoard.isEmpty()) {
            return 0f;
        }

        StringBuilder onlineBoardText =
                new StringBuilder();

        appendSearchText(
                onlineBoardText,
                onlineBook.getEducationBoard()
        );

        appendSearchText(
                onlineBoardText,
                onlineBook.getBookTitle()
        );

        appendSearchText(
                onlineBoardText,
                onlineBook.getDescription()
        );

        appendSearchText(
                onlineBoardText,
                onlineBook.getPublisherName()
        );

        for (String category :
                onlineBook.getCategories()) {

            appendSearchText(
                    onlineBoardText,
                    category
            );
        }

        return calculateTextSimilarity(
                safeScannedBoard,
                onlineBoardText.toString()
        );
    }

    @NonNull
    private String createSubjectSearchText(
            @NonNull OnlineBookSearchResult onlineBook
    ) {
        StringBuilder builder =
                new StringBuilder();

        appendSearchText(
                builder,
                onlineBook.getSubjectName()
        );

        appendSearchText(
                builder,
                onlineBook.getBookTitle()
        );

        appendSearchText(
                builder,
                onlineBook.getBookSubtitle()
        );

        for (String category :
                onlineBook.getCategories()) {

            appendSearchText(
                    builder,
                    category
            );
        }

        return builder.toString();
    }

    private void appendSearchText(
            @NonNull StringBuilder builder,
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(
                    ' '
            );
        }

        builder.append(
                safeValue
        );
    }

    private float calculateTextSimilarity(
            @Nullable String firstValue,
            @Nullable String secondValue
    ) {
        String first =
                normalizeForComparison(
                        firstValue
                );

        String second =
                normalizeForComparison(
                        secondValue
                );

        if (first.isEmpty()
                || second.isEmpty()) {

            return 0f;
        }

        if (first.equals(second)) {
            return 100f;
        }

        if (second.contains(first)
                || first.contains(second)) {

            int shorterLength =
                    Math.min(
                            first.length(),
                            second.length()
                    );

            int longerLength =
                    Math.max(
                            first.length(),
                            second.length()
                    );

            float containmentRatio =
                    shorterLength * 100f
                            / longerLength;

            return Math.max(
                    78f,
                    containmentRatio
            );
        }

        Set<String> firstTokens =
                createTokenSet(
                        first
                );

        Set<String> secondTokens =
                createTokenSet(
                        second
                );

        if (firstTokens.isEmpty()
                || secondTokens.isEmpty()) {

            return calculateCharacterSimilarity(
                    first,
                    second
            );
        }

        Set<String> intersection =
                new LinkedHashSet<>(
                        firstTokens
                );

        intersection.retainAll(
                secondTokens
        );

        Set<String> union =
                new LinkedHashSet<>(
                        firstTokens
                );

        union.addAll(
                secondTokens
        );

        float tokenSimilarity =
                union.isEmpty()
                        ? 0f
                        : intersection.size()
                          * 100f
                          / union.size();

        float characterSimilarity =
                calculateCharacterSimilarity(
                        first,
                        second
                );

        return normalizeScore(
                tokenSimilarity * 0.70f
                        + characterSimilarity * 0.30f
        );
    }

    private float calculateCharacterSimilarity(
            @NonNull String firstValue,
            @NonNull String secondValue
    ) {
        if (firstValue.isEmpty()
                || secondValue.isEmpty()) {

            return 0f;
        }

        int distance =
                calculateLevenshteinDistance(
                        firstValue,
                        secondValue
                );

        int maximumLength =
                Math.max(
                        firstValue.length(),
                        secondValue.length()
                );

        if (maximumLength <= 0) {
            return 100f;
        }

        return normalizeScore(
                (1f - distance / (float) maximumLength)
                        * 100f
        );
    }

    private int calculateLevenshteinDistance(
            @NonNull String firstValue,
            @NonNull String secondValue
    ) {
        int[] previousRow =
                new int[secondValue.length() + 1];

        int[] currentRow =
                new int[secondValue.length() + 1];

        for (int column = 0;
             column <= secondValue.length();
             column++) {

            previousRow[column] =
                    column;
        }

        for (int row = 1;
             row <= firstValue.length();
             row++) {

            currentRow[0] =
                    row;

            for (int column = 1;
                 column <= secondValue.length();
                 column++) {

                int substitutionCost =
                        firstValue.charAt(row - 1)
                                == secondValue.charAt(column - 1)
                                ? 0
                                : 1;

                currentRow[column] =
                        Math.min(
                                Math.min(
                                        currentRow[column - 1] + 1,
                                        previousRow[column] + 1
                                ),
                                previousRow[column - 1]
                                        + substitutionCost
                        );
            }

            int[] temporaryRow =
                    previousRow;

            previousRow =
                    currentRow;

            currentRow =
                    temporaryRow;
        }

        return previousRow[
                secondValue.length()
                ];
    }

    @NonNull
    private Set<String> createTokenSet(
            @NonNull String normalizedText
    ) {
        Set<String> tokens =
                new LinkedHashSet<>();

        String[] splitValues =
                normalizedText.split(
                        "\\s+"
                );

        for (String splitValue :
                splitValues) {

            String token =
                    splitValue.trim();

            if (token.length() >= 2
                    && !isIgnoredToken(
                    token
            )) {
                tokens.add(
                        token
                );
            }
        }

        return tokens;
    }

    private boolean isIgnoredToken(
            @NonNull String token
    ) {
        switch (token) {
            case "book":
            case "textbook":
            case "edition":
            case "class":
            case "grade":
            case "the":
            case "for":
            case "and":
            case "of":
            case "का":
            case "की":
            case "के":
            case "और":
                return true;

            default:
                return false;
        }
    }

    private int extractClassNumber(
            @Nullable String sourceText
    ) {
        String normalizedText =
                normalizeForComparison(
                        sourceText
                );

        if (normalizedText.isEmpty()) {
            return 0;
        }

        for (int classNumber = 12;
             classNumber >= 1;
             classNumber--) {

            String numberText =
                    String.valueOf(
                            classNumber
                    );

            if (containsWholeToken(
                    normalizedText,
                    numberText
            )) {
                return classNumber;
            }
        }

        String[] romanValues = {
                "",
                "i",
                "ii",
                "iii",
                "iv",
                "v",
                "vi",
                "vii",
                "viii",
                "ix",
                "x",
                "xi",
                "xii"
        };

        for (int classNumber = 12;
             classNumber >= 1;
             classNumber--) {

            if (containsWholeToken(
                    normalizedText,
                    romanValues[classNumber]
            )) {
                return classNumber;
            }
        }

        return 0;
    }

    private boolean containsWholeToken(
            @NonNull String normalizedText,
            @NonNull String requiredToken
    ) {
        String paddedText =
                " "
                        + normalizedText
                        + " ";

        String paddedToken =
                " "
                        + requiredToken
                        + " ";

        return paddedText.contains(
                paddedToken
        );
    }

    @NonNull
    private OnlineBookSearchResult.MatchStatus
    determineMatchStatus(
            float overallScore,
            float isbnSimilarity,
            float titleSimilarity,
            @NonNull List<String> warnings
    ) {
        if (isbnSimilarity >= 100f
                && titleSimilarity >= 60f) {

            return OnlineBookSearchResult
                    .MatchStatus.HIGH_CONFIDENCE;
        }

        if (overallScore >= 85f
                && titleSimilarity >= 70f) {

            return OnlineBookSearchResult
                    .MatchStatus.HIGH_CONFIDENCE;
        }

        if (overallScore >= 65f) {
            return OnlineBookSearchResult
                    .MatchStatus.POSSIBLE_MATCH;
        }

        if (overallScore >= 40f
                || !warnings.isEmpty()) {

            return OnlineBookSearchResult
                    .MatchStatus.MANUAL_REVIEW_REQUIRED;
        }

        return OnlineBookSearchResult
                .MatchStatus.NO_CONFIDENT_MATCH;
    }

    private boolean isAutomaticSelectionRecommended(
            float overallScore,
            float isbnSimilarity,
            float titleSimilarity,
            @NonNull OnlineBookSearchResult onlineBook,
            @NonNull List<String> warnings
    ) {
        if (!warnings.isEmpty()) {
            return false;
        }

        if (isbnSimilarity >= 100f
                && titleSimilarity >= 75f) {

            return true;
        }

        return overallScore >= 92f
                && titleSimilarity >= 88f
                && onlineBook.isOfficialSourceVerified();
    }

    @NonNull
    private String normalizeForComparison(
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return "";
        }

        String normalizedUnicode =
                Normalizer.normalize(
                        safeValue,
                        Normalizer.Form.NFKC
                );

        return normalizedUnicode
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        '&',
                        ' '
                )
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    @NonNull
    private String normalizeIsbn(
            @Nullable String isbnValue
    ) {
        return safeText(
                isbnValue
        )
                .replaceAll(
                        "[^0-9Xx]",
                        ""
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }

    @NonNull
    private String convertIsbn10ToIsbn13(
            @Nullable String isbn10
    ) {
        String normalizedIsbn10 =
                normalizeIsbn(
                        isbn10
                );

        if (normalizedIsbn10.length() != 10) {
            return "";
        }

        String firstTwelveDigits =
                "978"
                        + normalizedIsbn10.substring(
                        0,
                        9
                );

        int checksumTotal =
                0;

        for (int index = 0;
             index < firstTwelveDigits.length();
             index++) {

            char character =
                    firstTwelveDigits.charAt(
                            index
                    );

            if (!Character.isDigit(
                    character
            )) {
                return "";
            }

            int digit =
                    character - '0';

            checksumTotal +=
                    index % 2 == 0
                            ? digit
                            : digit * 3;
        }

        int checkDigit =
                (10 - checksumTotal % 10) % 10;

        return firstTwelveDigits
                + checkDigit;
    }

    @NonNull
    private String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private float normalizeScore(
            float score
    ) {
        return Math.max(
                0f,
                Math.min(
                        MAXIMUM_SCORE,
                        score
                )
        );
    }

    public static final class MatchEvaluation {

        private final float overallMatchScore;

        private final float titleMatchScore;

        private final float isbnMatchScore;

        private final float subjectMatchScore;

        private final float classMatchScore;

        private final float publisherMatchScore;

        private final float boardMatchScore;

        private final float authorMatchScore;

        @NonNull
        private final OnlineBookSearchResult.MatchStatus
                matchStatus;

        private final boolean
                automaticSelectionRecommended;

        @NonNull
        private final List<String> matchReasons;

        @NonNull
        private final List<String> warnings;

        private MatchEvaluation(
                float overallMatchScore,
                float titleMatchScore,
                float isbnMatchScore,
                float subjectMatchScore,
                float classMatchScore,
                float publisherMatchScore,
                float boardMatchScore,
                float authorMatchScore,
                @NonNull OnlineBookSearchResult.MatchStatus
                        matchStatus,
                boolean automaticSelectionRecommended,
                @NonNull List<String> matchReasons,
                @NonNull List<String> warnings
        ) {
            this.overallMatchScore =
                    overallMatchScore;

            this.titleMatchScore =
                    titleMatchScore;

            this.isbnMatchScore =
                    isbnMatchScore;

            this.subjectMatchScore =
                    subjectMatchScore;

            this.classMatchScore =
                    classMatchScore;

            this.publisherMatchScore =
                    publisherMatchScore;

            this.boardMatchScore =
                    boardMatchScore;

            this.authorMatchScore =
                    authorMatchScore;

            this.matchStatus =
                    matchStatus;

            this.automaticSelectionRecommended =
                    automaticSelectionRecommended;

            this.matchReasons =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    matchReasons
                            )
                    );

            this.warnings =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    warnings
                            )
                    );
        }

        public float getOverallMatchScore() {
            return overallMatchScore;
        }

        public float getTitleMatchScore() {
            return titleMatchScore;
        }

        public float getIsbnMatchScore() {
            return isbnMatchScore;
        }

        public float getSubjectMatchScore() {
            return subjectMatchScore;
        }

        public float getClassMatchScore() {
            return classMatchScore;
        }

        public float getPublisherMatchScore() {
            return publisherMatchScore;
        }

        public float getBoardMatchScore() {
            return boardMatchScore;
        }

        public float getAuthorMatchScore() {
            return authorMatchScore;
        }

        @NonNull
        public OnlineBookSearchResult.MatchStatus
        getMatchStatus() {
            return matchStatus;
        }

        public boolean
        isAutomaticSelectionRecommended() {
            return automaticSelectionRecommended;
        }

        @NonNull
        public List<String> getMatchReasons() {
            return matchReasons;
        }

        @NonNull
        public List<String> getWarnings() {
            return warnings;
        }

        public boolean isHighConfidence() {
            return matchStatus
                    == OnlineBookSearchResult
                    .MatchStatus.HIGH_CONFIDENCE;
        }

        public boolean requiresParentReview() {
            return !automaticSelectionRecommended
                    || matchStatus
                    == OnlineBookSearchResult
                    .MatchStatus
                    .MANUAL_REVIEW_REQUIRED
                    || !warnings.isEmpty();
        }
    }

    public static final class RankedBookResult {

        @NonNull
        private final OnlineBookSearchResult
                bookResult;

        @NonNull
        private final MatchEvaluation evaluation;

        private RankedBookResult(
                @NonNull OnlineBookSearchResult
                        bookResult,
                @NonNull MatchEvaluation evaluation
        ) {
            this.bookResult =
                    bookResult;

            this.evaluation =
                    evaluation;
        }

        @NonNull
        public OnlineBookSearchResult
        getBookResult() {
            return bookResult;
        }

        @NonNull
        public MatchEvaluation getEvaluation() {
            return evaluation;
        }
    }
}