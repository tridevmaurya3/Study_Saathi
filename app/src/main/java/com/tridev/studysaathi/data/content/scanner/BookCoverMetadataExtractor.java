package com.tridev.studysaathi.data.content.scanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model.BookCoverScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BookCoverMetadataExtractor {

    private static final Pattern CLASS_PATTERN =
            Pattern.compile(
                    "(?i)(?:class|grade|standard|std\\.?|कक्षा)"
                            + "\\s*[-:]?\\s*"
                            + "(1[0-2]|[1-9]|VI{0,3}|IV|IX|XI{0,2})"
            );

    private static final Pattern EDITION_PATTERN =
            Pattern.compile(
                    "(?i)(\\d{1,2}(?:st|nd|rd|th)?\\s+edition"
                            + "|revised\\s+edition"
                            + "|new\\s+edition"
                            + "|संशोधित\\s+संस्करण"
                            + "|नया\\s+संस्करण"
                            + "|प्रथम\\s+संस्करण"
                            + "|द्वितीय\\s+संस्करण)"
            );

    private static final Pattern PUBLICATION_YEAR_PATTERN =
            Pattern.compile(
                    "(?<!\\d)(20\\d{2}|19\\d{2})(?!\\d)"
            );

    private static final Pattern AUTHOR_PATTERN =
            Pattern.compile(
                    "(?i)(?:author|authors|written\\s+by|by|लेखक|लेखिका)"
                            + "\\s*[:\\-]?\\s*(.+)"
            );

    private static final Pattern PUBLISHER_PATTERN =
            Pattern.compile(
                    "(?i)(?:published\\s+by|publisher|publishers"
                            + "|publication|publications"
                            + "|प्रकाशक|प्रकाशन)"
                            + "\\s*[:\\-]?\\s*(.+)"
            );

    private static final int MAXIMUM_SEARCH_QUERY_LENGTH =
            350;

    @NonNull
    private final Map<String, SubjectDefinition>
            subjectDefinitions;

    @NonNull
    private final Map<String, BoardDefinition>
            boardDefinitions;

    public BookCoverMetadataExtractor() {
        subjectDefinitions =
                createSubjectDefinitions();

        boardDefinitions =
                createBoardDefinitions();
    }

    /**
     * Extracts structured book metadata from a raw
     * OCR and barcode scan result.
     */
    @NonNull
    public DetectedBookMetadata extract(
            @NonNull BookCoverScanResult scanResult
    ) {
        return extract(
                scanResult,
                ExtractionContext.empty()
        );
    }

    /**
     * Extracts structured book metadata while using
     * optional school-profile information to improve
     * matching accuracy.
     */
    @NonNull
    public DetectedBookMetadata extract(
            @NonNull BookCoverScanResult scanResult,
            @NonNull ExtractionContext context
    ) {
        List<String> textLines =
                prepareTextLines(
                        scanResult
                );

        String completeText =
                joinLines(
                        textLines
                );

        DetectedValue subject =
                detectSubject(
                        completeText,
                        context
                );

        DetectedValue className =
                detectClass(
                        completeText,
                        context
                );

        DetectedValue educationBoard =
                detectBoard(
                        completeText,
                        context
                );

        DetectedValue publisher =
                detectPublisher(
                        textLines
                );

        DetectedValue author =
                detectAuthor(
                        textLines
                );

        DetectedValue edition =
                detectEdition(
                        completeText
                );

        DetectedValue publicationYear =
                detectPublicationYear(
                        completeText
                );

        DetectedValue studyMedium =
                detectStudyMedium(
                        completeText,
                        context
                );

        DetectedValue title =
                detectTitle(
                        textLines,
                        subject.getValue(),
                        className.getValue(),
                        educationBoard.getValue(),
                        publisher.getValue()
                );

        float overallConfidence =
                calculateOverallConfidence(
                        scanResult,
                        title,
                        subject,
                        className,
                        educationBoard,
                        publisher
                );

        List<String> warnings =
                createWarnings(
                        scanResult,
                        title,
                        subject,
                        className,
                        publisher
                );

        String onlineSearchQuery =
                createOnlineSearchQuery(
                        scanResult,
                        title.getValue(),
                        subject.getValue(),
                        className.getValue(),
                        educationBoard.getValue(),
                        publisher.getValue(),
                        author.getValue()
                );

        return new DetectedBookMetadata(
                title.getValue(),
                subject.getValue(),
                className.getValue(),
                educationBoard.getValue(),
                publisher.getValue(),
                author.getValue(),
                edition.getValue(),
                publicationYear.getValue(),
                studyMedium.getValue(),
                scanResult.getDetectedIsbn10(),
                scanResult.getDetectedIsbn13(),
                scanResult.getDetectedBarcodeValue(),
                onlineSearchQuery,
                title.getConfidence(),
                subject.getConfidence(),
                className.getConfidence(),
                educationBoard.getConfidence(),
                publisher.getConfidence(),
                overallConfidence,
                warnings
        );
    }

    @NonNull
    private DetectedValue detectSubject(
            @NonNull String completeText,
            @NonNull ExtractionContext context
    ) {
        String normalizedText =
                normalizeForComparison(
                        completeText
                );

        SubjectDefinition bestMatch =
                null;

        int bestScore =
                0;

        for (SubjectDefinition definition :
                subjectDefinitions.values()) {

            int score =
                    0;

            for (String keyword :
                    definition.getKeywords()) {

                if (containsWholeKeyword(
                        normalizedText,
                        keyword
                )) {
                    score +=
                            keyword.length() >= 8
                                    ? 4
                                    : 2;
                }
            }

            if (context.containsExpectedSubject(
                    definition.getSubjectId()
            )) {
                score +=
                        3;
            }

            if (score > bestScore) {
                bestScore =
                        score;

                bestMatch =
                        definition;
            }
        }

        if (bestMatch == null) {
            return DetectedValue.empty();
        }

        float confidence =
                Math.min(
                        98f,
                        55f + bestScore * 6f
                );

        return new DetectedValue(
                bestMatch.getDisplayName(),
                confidence
        );
    }

    @NonNull
    private DetectedValue detectClass(
            @NonNull String completeText,
            @NonNull ExtractionContext context
    ) {
        Matcher matcher =
                CLASS_PATTERN.matcher(
                        completeText
                );

        if (matcher.find()) {
            String rawClassValue =
                    safeText(
                            matcher.group(1)
                    );

            int classNumber =
                    convertClassValueToNumber(
                            rawClassValue
                    );

            if (classNumber >= 1
                    && classNumber <= 12) {

                float confidence =
                        classNumber
                                == context.getExpectedClassNumber()
                                ? 98f
                                : 90f;

                return new DetectedValue(
                        "Class " + classNumber,
                        confidence
                );
            }
        }

        int expectedClass =
                context.getExpectedClassNumber();

        if (expectedClass >= 1
                && expectedClass <= 12) {

            return new DetectedValue(
                    "Class " + expectedClass,
                    45f
            );
        }

        return DetectedValue.empty();
    }

    @NonNull
    private DetectedValue detectBoard(
            @NonNull String completeText,
            @NonNull ExtractionContext context
    ) {
        String normalizedText =
                normalizeForComparison(
                        completeText
                );

        BoardDefinition bestMatch =
                null;

        int bestScore =
                0;

        for (BoardDefinition definition :
                boardDefinitions.values()) {

            int score =
                    0;

            for (String keyword :
                    definition.getKeywords()) {

                if (containsWholeKeyword(
                        normalizedText,
                        keyword
                )) {
                    score +=
                            3;
                }
            }

            if (definition.matches(
                    context.getExpectedBoard()
            )) {
                score +=
                        2;
            }

            if (score > bestScore) {
                bestScore =
                        score;

                bestMatch =
                        definition;
            }
        }

        if (bestMatch != null) {
            return new DetectedValue(
                    bestMatch.getDisplayName(),
                    Math.min(
                            98f,
                            65f + bestScore * 7f
                    )
            );
        }

        if (!context.getExpectedBoard()
                .isEmpty()) {

            return new DetectedValue(
                    context.getExpectedBoard(),
                    40f
            );
        }

        return DetectedValue.empty();
    }

    @NonNull
    private DetectedValue detectPublisher(
            @NonNull List<String> textLines
    ) {
        for (String line : textLines) {
            Matcher matcher =
                    PUBLISHER_PATTERN.matcher(
                            line
                    );

            if (matcher.find()) {
                String detectedPublisher =
                        cleanDetectedValue(
                                matcher.group(1)
                        );

                if (!detectedPublisher.isEmpty()) {
                    return new DetectedValue(
                            detectedPublisher,
                            92f
                    );
                }
            }
        }

        for (String line : textLines) {
            String normalizedLine =
                    normalizeForComparison(
                            line
                    );

            if (normalizedLine.contains("ncert")
                    || normalizedLine.contains(
                    "national council of educational research"
            )) {
                return new DetectedValue(
                        "NCERT",
                        98f
                );
            }

            if (normalizedLine.contains("publication")
                    || normalizedLine.contains("publications")
                    || normalizedLine.contains("publishers")
                    || normalizedLine.contains("books")
                    || normalizedLine.contains("प्रकाशन")
                    || normalizedLine.contains("प्रकाशक")) {

                if (line.length() <= 100) {
                    return new DetectedValue(
                            cleanDetectedValue(line),
                            72f
                    );
                }
            }
        }

        return DetectedValue.empty();
    }

    @NonNull
    private DetectedValue detectAuthor(
            @NonNull List<String> textLines
    ) {
        for (String line : textLines) {
            Matcher matcher =
                    AUTHOR_PATTERN.matcher(
                            line
                    );

            if (matcher.find()) {
                String detectedAuthor =
                        cleanDetectedValue(
                                matcher.group(1)
                        );

                if (!detectedAuthor.isEmpty()) {
                    return new DetectedValue(
                            detectedAuthor,
                            88f
                    );
                }
            }
        }

        return DetectedValue.empty();
    }

    @NonNull
    private DetectedValue detectEdition(
            @NonNull String completeText
    ) {
        Matcher matcher =
                EDITION_PATTERN.matcher(
                        completeText
                );

        if (matcher.find()) {
            return new DetectedValue(
                    cleanDetectedValue(
                            matcher.group(1)
                    ),
                    90f
            );
        }

        return DetectedValue.empty();
    }

    @NonNull
    private DetectedValue detectPublicationYear(
            @NonNull String completeText
    ) {
        Matcher matcher =
                PUBLICATION_YEAR_PATTERN.matcher(
                        completeText
                );

        int currentYear =
                java.util.Calendar.getInstance()
                        .get(
                                java.util.Calendar.YEAR
                        );

        while (matcher.find()) {
            String yearText =
                    safeText(
                            matcher.group(1)
                    );

            try {
                int year =
                        Integer.parseInt(
                                yearText
                        );

                if (year >= 1950
                        && year <= currentYear + 1) {

                    return new DetectedValue(
                            yearText,
                            80f
                    );
                }

            } catch (NumberFormatException ignored) {
                // Continue checking other year values.
            }
        }

        return DetectedValue.empty();
    }

    @NonNull
    private DetectedValue detectStudyMedium(
            @NonNull String completeText,
            @NonNull ExtractionContext context
    ) {
        String normalizedText =
                normalizeForComparison(
                        completeText
                );

        boolean hasHindi =
                normalizedText.contains("hindi medium")
                        || normalizedText.contains("हिंदी माध्यम")
                        || normalizedText.contains("हिन्दी माध्यम");

        boolean hasEnglish =
                normalizedText.contains("english medium")
                        || normalizedText.contains("अंग्रेजी माध्यम");

        if (hasHindi
                && hasEnglish) {

            return new DetectedValue(
                    "Bilingual",
                    92f
            );
        }

        if (hasHindi) {
            return new DetectedValue(
                    "Hindi",
                    94f
            );
        }

        if (hasEnglish) {
            return new DetectedValue(
                    "English",
                    94f
            );
        }

        if (!context.getExpectedMedium()
                .isEmpty()) {

            return new DetectedValue(
                    context.getExpectedMedium(),
                    42f
            );
        }

        return DetectedValue.empty();
    }

    @NonNull
    private DetectedValue detectTitle(
            @NonNull List<String> textLines,
            @NonNull String detectedSubject,
            @NonNull String detectedClass,
            @NonNull String detectedBoard,
            @NonNull String detectedPublisher
    ) {
        String bestTitle =
                "";

        int bestScore =
                Integer.MIN_VALUE;

        int maximumLines =
                Math.min(
                        12,
                        textLines.size()
                );

        for (int index = 0;
             index < maximumLines;
             index++) {

            String line =
                    textLines.get(index);

            int score =
                    scoreTitleCandidate(
                            line,
                            index,
                            detectedSubject,
                            detectedClass,
                            detectedBoard,
                            detectedPublisher
                    );

            if (score > bestScore) {
                bestScore =
                        score;

                bestTitle =
                        line;
            }
        }

        if (bestScore < 3
                || bestTitle.isEmpty()) {

            return DetectedValue.empty();
        }

        float confidence =
                Math.min(
                        96f,
                        52f + bestScore * 5f
                );

        return new DetectedValue(
                cleanDetectedValue(
                        bestTitle
                ),
                confidence
        );
    }

    private int scoreTitleCandidate(
            @NonNull String line,
            int lineIndex,
            @NonNull String subject,
            @NonNull String className,
            @NonNull String board,
            @NonNull String publisher
    ) {
        String normalizedLine =
                normalizeForComparison(
                        line
                );

        int length =
                line.length();

        if (length < 3
                || length > 100) {

            return Integer.MIN_VALUE;
        }

        int score =
                0;

        if (lineIndex <= 2) {
            score +=
                    5;

        } else if (lineIndex <= 5) {
            score +=
                    3;
        }

        if (length >= 5
                && length <= 55) {

            score +=
                    4;
        }

        if (containsLetters(line)) {
            score +=
                    3;
        }

        if (containsMostlyUpperCaseLetters(
                line
        )) {
            score +=
                    2;
        }

        if (!subject.isEmpty()
                && normalizeForComparison(subject)
                .equals(normalizedLine)) {

            score +=
                    3;
        }

        if (containsMetadataLabel(
                normalizedLine
        )) {
            score -=
                    8;
        }

        if (!className.isEmpty()
                && normalizedLine.equals(
                normalizeForComparison(className)
        )) {
            score -=
                    7;
        }

        if (!board.isEmpty()
                && normalizedLine.equals(
                normalizeForComparison(board)
        )) {
            score -=
                    7;
        }

        if (!publisher.isEmpty()
                && normalizedLine.equals(
                normalizeForComparison(publisher)
        )) {
            score -=
                    7;
        }

        if (normalizedLine.matches(
                ".*\\b(?:isbn|class|grade|edition|publisher|author)\\b.*"
        )) {
            score -=
                    6;
        }

        if (normalizedLine.matches(
                "^[0-9\\s\\-:/.]+$"
        )) {
            score -=
                    10;
        }

        return score;
    }

    private float calculateOverallConfidence(
            @NonNull BookCoverScanResult scanResult,
            @NonNull DetectedValue title,
            @NonNull DetectedValue subject,
            @NonNull DetectedValue className,
            @NonNull DetectedValue educationBoard,
            @NonNull DetectedValue publisher
    ) {
        float weightedTotal =
                title.getConfidence() * 0.30f
                        + subject.getConfidence() * 0.22f
                        + className.getConfidence() * 0.18f
                        + educationBoard.getConfidence() * 0.12f
                        + publisher.getConfidence() * 0.08f
                        + scanResult.getOverallConfidence() * 0.10f;

        if (scanResult.hasIsbn()) {
            weightedTotal +=
                    5f;
        }

        return Math.max(
                0f,
                Math.min(
                        100f,
                        weightedTotal
                )
        );
    }

    @NonNull
    private List<String> createWarnings(
            @NonNull BookCoverScanResult scanResult,
            @NonNull DetectedValue title,
            @NonNull DetectedValue subject,
            @NonNull DetectedValue className,
            @NonNull DetectedValue publisher
    ) {
        List<String> warnings =
                new ArrayList<>(
                        scanResult.getWarnings()
                );

        if (title.getValue().isEmpty()) {
            warnings.add(
                    "Book title could not be identified confidently."
            );
        }

        if (subject.getValue().isEmpty()) {
            warnings.add(
                    "Subject could not be identified from the cover."
            );
        }

        if (className.getValue().isEmpty()) {
            warnings.add(
                    "Class or grade information was not found."
            );
        }

        if (publisher.getValue().isEmpty()) {
            warnings.add(
                    "Publisher information was not found."
            );
        }

        if (!scanResult.hasIsbn()) {
            warnings.add(
                    "ISBN was not found; online matching may be less accurate."
            );
        }

        return Collections.unmodifiableList(
                warnings
        );
    }

    @NonNull
    private String createOnlineSearchQuery(
            @NonNull BookCoverScanResult scanResult,
            @NonNull String title,
            @NonNull String subject,
            @NonNull String className,
            @NonNull String board,
            @NonNull String publisher,
            @NonNull String author
    ) {
        Set<String> queryParts =
                new LinkedHashSet<>();

        String preferredIsbn =
                scanResult.getPreferredIsbn();

        if (!preferredIsbn.isEmpty()) {
            queryParts.add(
                    preferredIsbn
            );
        }

        addSearchPart(
                queryParts,
                title
        );

        addSearchPart(
                queryParts,
                subject
        );

        addSearchPart(
                queryParts,
                className
        );

        addSearchPart(
                queryParts,
                board
        );

        addSearchPart(
                queryParts,
                publisher
        );

        addSearchPart(
                queryParts,
                author
        );

        StringBuilder searchQuery =
                new StringBuilder();

        for (String queryPart : queryParts) {
            if (searchQuery.length() > 0) {
                searchQuery.append(
                        ' '
                );
            }

            searchQuery.append(
                    queryPart
            );

            if (searchQuery.length()
                    >= MAXIMUM_SEARCH_QUERY_LENGTH) {

                break;
            }
        }

        String finalQuery =
                searchQuery.toString()
                        .trim();

        if (finalQuery.length()
                > MAXIMUM_SEARCH_QUERY_LENGTH) {

            finalQuery =
                    finalQuery.substring(
                                    0,
                                    MAXIMUM_SEARCH_QUERY_LENGTH
                            )
                            .trim();
        }

        return finalQuery;
    }

    private void addSearchPart(
            @NonNull Set<String> queryParts,
            @Nullable String value
    ) {
        String normalizedValue =
                safeText(
                        value
                );

        if (!normalizedValue.isEmpty()) {
            queryParts.add(
                    normalizedValue
            );
        }
    }

    @NonNull
    private List<String> prepareTextLines(
            @NonNull BookCoverScanResult scanResult
    ) {
        Map<String, String> uniqueLines =
                new LinkedHashMap<>();

        for (String textLine :
                scanResult.getDetectedTextLines()) {

            addUniqueLine(
                    uniqueLines,
                    textLine
            );
        }

        if (uniqueLines.isEmpty()) {
            String fullText =
                    scanResult.getDetectedFullText();

            if (!fullText.isEmpty()) {
                String[] lines =
                        fullText.split(
                                "\\r?\\n"
                        );

                for (String line : lines) {
                    addUniqueLine(
                            uniqueLines,
                            line
                    );
                }
            }
        }

        return new ArrayList<>(
                uniqueLines.values()
        );
    }

    private void addUniqueLine(
            @NonNull Map<String, String> uniqueLines,
            @Nullable String line
    ) {
        String safeLine =
                cleanDetectedValue(
                        line
                );

        if (safeLine.length() < 2) {
            return;
        }

        String comparisonKey =
                normalizeForComparison(
                        safeLine
                );

        if (!comparisonKey.isEmpty()
                && !uniqueLines.containsKey(
                comparisonKey
        )) {
            uniqueLines.put(
                    comparisonKey,
                    safeLine
            );
        }
    }

    @NonNull
    private Map<String, SubjectDefinition>
    createSubjectDefinitions() {
        Map<String, SubjectDefinition> definitions =
                new LinkedHashMap<>();

        addSubject(
                definitions,
                "mathematics",
                "Mathematics",
                "mathematics",
                "maths",
                "math",
                "गणित",
                "ganita",
                "ganit"
        );

        addSubject(
                definitions,
                "science",
                "Science",
                "science",
                "विज्ञान",
                "curiosity"
        );

        addSubject(
                definitions,
                "english",
                "English",
                "english",
                "अंग्रेजी",
                "poorvi",
                "grammar",
                "literature"
        );

        addSubject(
                definitions,
                "hindi",
                "Hindi",
                "hindi",
                "हिंदी",
                "हिन्दी",
                "मल्हार",
                "malhar"
        );

        addSubject(
                definitions,
                "social_science",
                "Social Science",
                "social science",
                "social studies",
                "history",
                "geography",
                "civics",
                "सामाजिक विज्ञान",
                "इतिहास",
                "भूगोल",
                "नागरिक शास्त्र"
        );

        addSubject(
                definitions,
                "sanskrit",
                "Sanskrit",
                "sanskrit",
                "संस्कृत",
                "deepakam",
                "दीपकम्"
        );

        addSubject(
                definitions,
                "computer",
                "Computer",
                "computer",
                "computer science",
                "information technology",
                "coding",
                "कंप्यूटर",
                "कम्प्यूटर"
        );

        addSubject(
                definitions,
                "general_knowledge",
                "General Knowledge",
                "general knowledge",
                "gk",
                "सामान्य ज्ञान"
        );

        addSubject(
                definitions,
                "moral_science",
                "Moral Science",
                "moral science",
                "value education",
                "moral values",
                "नैतिक शिक्षा"
        );

        addSubject(
                definitions,
                "reasoning",
                "Reasoning",
                "reasoning",
                "mental ability",
                "logical reasoning",
                "तर्कशक्ति",
                "मानसिक योग्यता"
        );

        addSubject(
                definitions,
                "art",
                "Art",
                "art",
                "drawing",
                "craft",
                "कला",
                "चित्रकला"
        );

        addSubject(
                definitions,
                "environmental_studies",
                "Environmental Studies",
                "environmental studies",
                "evs",
                "पर्यावरण अध्ययन"
        );

        return Collections.unmodifiableMap(
                definitions
        );
    }

    private void addSubject(
            @NonNull Map<String, SubjectDefinition> definitions,
            @NonNull String subjectId,
            @NonNull String displayName,
            @NonNull String... keywords
    ) {
        definitions.put(
                subjectId,
                new SubjectDefinition(
                        subjectId,
                        displayName,
                        keywords
                )
        );
    }

    @NonNull
    private Map<String, BoardDefinition>
    createBoardDefinitions() {
        Map<String, BoardDefinition> definitions =
                new LinkedHashMap<>();

        addBoard(
                definitions,
                "cbse",
                "CBSE",
                "cbse",
                "central board of secondary education"
        );

        addBoard(
                definitions,
                "ncert",
                "NCERT",
                "ncert",
                "national council of educational research and training"
        );

        addBoard(
                definitions,
                "up_board",
                "UP Board",
                "up board",
                "upmsp",
                "uttar pradesh madhyamik shiksha parishad",
                "उत्तर प्रदेश माध्यमिक शिक्षा परिषद"
        );

        addBoard(
                definitions,
                "icse",
                "ICSE",
                "icse",
                "cisce",
                "council for the indian school certificate examinations"
        );

        return Collections.unmodifiableMap(
                definitions
        );
    }

    private void addBoard(
            @NonNull Map<String, BoardDefinition> definitions,
            @NonNull String boardId,
            @NonNull String displayName,
            @NonNull String... keywords
    ) {
        definitions.put(
                boardId,
                new BoardDefinition(
                        boardId,
                        displayName,
                        keywords
                )
        );
    }

    private int convertClassValueToNumber(
            @NonNull String classValue
    ) {
        String normalizedValue =
                classValue.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        switch (normalizedValue) {
            case "I":
                return 1;

            case "II":
                return 2;

            case "III":
                return 3;

            case "IV":
                return 4;

            case "V":
                return 5;

            case "VI":
                return 6;

            case "VII":
                return 7;

            case "VIII":
                return 8;

            case "IX":
                return 9;

            case "X":
                return 10;

            case "XI":
                return 11;

            case "XII":
                return 12;

            default:
                try {
                    return Integer.parseInt(
                            normalizedValue
                    );

                } catch (NumberFormatException exception) {
                    return 0;
                }
        }
    }

    private boolean containsMetadataLabel(
            @NonNull String normalizedLine
    ) {
        return normalizedLine.contains("isbn")
                || normalizedLine.contains("publisher")
                || normalizedLine.contains("publication")
                || normalizedLine.contains("author")
                || normalizedLine.contains("edition")
                || normalizedLine.contains("class ")
                || normalizedLine.contains("grade ")
                || normalizedLine.contains("प्रकाशक")
                || normalizedLine.contains("प्रकाशन")
                || normalizedLine.contains("लेखक")
                || normalizedLine.contains("संस्करण")
                || normalizedLine.contains("कक्षा");
    }

    private boolean containsLetters(
            @NonNull String value
    ) {
        for (int index = 0;
             index < value.length();
             index++) {

            if (Character.isLetter(
                    value.charAt(index)
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean containsMostlyUpperCaseLetters(
            @NonNull String value
    ) {
        int letterCount =
                0;

        int upperCaseCount =
                0;

        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(index);

            if (Character.isLetter(character)) {
                letterCount++;

                if (Character.isUpperCase(
                        character
                )) {
                    upperCaseCount++;
                }
            }
        }

        return letterCount >= 3
                && upperCaseCount
                >= Math.ceil(letterCount * 0.70);
    }

    private boolean containsWholeKeyword(
            @NonNull String normalizedText,
            @NonNull String keyword
    ) {
        String normalizedKeyword =
                normalizeForComparison(
                        keyword
                );

        if (normalizedKeyword.isEmpty()) {
            return false;
        }

        return normalizedText.contains(
                normalizedKeyword
        );
    }

    @NonNull
    private String normalizeForComparison(
            @Nullable String value
    ) {
        return safeText(value)
                .toLowerCase(
                        Locale.ROOT
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
    private String cleanDetectedValue(
            @Nullable String value
    ) {
        return safeText(value)
                .replaceAll(
                        "\\s+",
                        " "
                )
                .replaceAll(
                        "^[\\-:|,.;]+",
                        ""
                )
                .replaceAll(
                        "[\\-:|,.;]+$",
                        ""
                )
                .trim();
    }

    @NonNull
    private String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    @NonNull
    private String joinLines(
            @NonNull List<String> lines
    ) {
        StringBuilder builder =
                new StringBuilder();

        for (String line : lines) {
            if (builder.length() > 0) {
                builder.append(
                        '\n'
                );
            }

            builder.append(
                    line
            );
        }

        return builder.toString()
                .trim();
    }

    public static final class ExtractionContext {

        private final int expectedClassNumber;

        @NonNull
        private final String expectedBoard;

        @NonNull
        private final String expectedMedium;

        @NonNull
        private final Set<String> expectedSubjectIds;

        public ExtractionContext(
                int expectedClassNumber,
                @Nullable String expectedBoard,
                @Nullable String expectedMedium,
                @NonNull List<String> expectedSubjectIds
        ) {
            this.expectedClassNumber =
                    Math.max(
                            0,
                            Math.min(
                                    12,
                                    expectedClassNumber
                            )
                    );

            this.expectedBoard =
                    expectedBoard == null
                            ? ""
                            : expectedBoard.trim();

            this.expectedMedium =
                    expectedMedium == null
                            ? ""
                            : expectedMedium.trim();

            Set<String> preparedSubjectIds =
                    new LinkedHashSet<>();

            for (String subjectId :
                    expectedSubjectIds) {

                if (subjectId == null
                        || subjectId.trim().isEmpty()) {

                    continue;
                }

                preparedSubjectIds.add(
                        subjectId.trim()
                                .toLowerCase(
                                        Locale.ROOT
                                )
                );
            }

            this.expectedSubjectIds =
                    Collections.unmodifiableSet(
                            preparedSubjectIds
                    );
        }

        @NonNull
        public static ExtractionContext empty() {
            return new ExtractionContext(
                    0,
                    "",
                    "",
                    Collections.emptyList()
            );
        }

        public int getExpectedClassNumber() {
            return expectedClassNumber;
        }

        @NonNull
        public String getExpectedBoard() {
            return expectedBoard;
        }

        @NonNull
        public String getExpectedMedium() {
            return expectedMedium;
        }

        public boolean containsExpectedSubject(
                @NonNull String subjectId
        ) {
            return expectedSubjectIds.contains(
                    subjectId.toLowerCase(
                            Locale.ROOT
                    )
            );
        }
    }

    public static final class DetectedBookMetadata {

        @NonNull
        private final String bookTitle;

        @NonNull
        private final String subjectName;

        @NonNull
        private final String className;

        @NonNull
        private final String educationBoard;

        @NonNull
        private final String publisherName;

        @NonNull
        private final String authorName;

        @NonNull
        private final String editionName;

        @NonNull
        private final String publicationYear;

        @NonNull
        private final String studyMedium;

        @NonNull
        private final String isbn10;

        @NonNull
        private final String isbn13;

        @NonNull
        private final String barcodeValue;

        @NonNull
        private final String onlineSearchQuery;

        private final float titleConfidence;

        private final float subjectConfidence;

        private final float classConfidence;

        private final float boardConfidence;

        private final float publisherConfidence;

        private final float overallConfidence;

        @NonNull
        private final List<String> warnings;

        private DetectedBookMetadata(
                @NonNull String bookTitle,
                @NonNull String subjectName,
                @NonNull String className,
                @NonNull String educationBoard,
                @NonNull String publisherName,
                @NonNull String authorName,
                @NonNull String editionName,
                @NonNull String publicationYear,
                @NonNull String studyMedium,
                @NonNull String isbn10,
                @NonNull String isbn13,
                @NonNull String barcodeValue,
                @NonNull String onlineSearchQuery,
                float titleConfidence,
                float subjectConfidence,
                float classConfidence,
                float boardConfidence,
                float publisherConfidence,
                float overallConfidence,
                @NonNull List<String> warnings
        ) {
            this.bookTitle =
                    bookTitle;

            this.subjectName =
                    subjectName;

            this.className =
                    className;

            this.educationBoard =
                    educationBoard;

            this.publisherName =
                    publisherName;

            this.authorName =
                    authorName;

            this.editionName =
                    editionName;

            this.publicationYear =
                    publicationYear;

            this.studyMedium =
                    studyMedium;

            this.isbn10 =
                    isbn10;

            this.isbn13 =
                    isbn13;

            this.barcodeValue =
                    barcodeValue;

            this.onlineSearchQuery =
                    onlineSearchQuery;

            this.titleConfidence =
                    titleConfidence;

            this.subjectConfidence =
                    subjectConfidence;

            this.classConfidence =
                    classConfidence;

            this.boardConfidence =
                    boardConfidence;

            this.publisherConfidence =
                    publisherConfidence;

            this.overallConfidence =
                    overallConfidence;

            this.warnings =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    warnings
                            )
                    );
        }

        @NonNull
        public String getBookTitle() {
            return bookTitle;
        }

        @NonNull
        public String getSubjectName() {
            return subjectName;
        }

        @NonNull
        public String getClassName() {
            return className;
        }

        @NonNull
        public String getEducationBoard() {
            return educationBoard;
        }

        @NonNull
        public String getPublisherName() {
            return publisherName;
        }

        @NonNull
        public String getAuthorName() {
            return authorName;
        }

        @NonNull
        public String getEditionName() {
            return editionName;
        }

        @NonNull
        public String getPublicationYear() {
            return publicationYear;
        }

        @NonNull
        public String getStudyMedium() {
            return studyMedium;
        }

        @NonNull
        public String getIsbn10() {
            return isbn10;
        }

        @NonNull
        public String getIsbn13() {
            return isbn13;
        }

        @NonNull
        public String getPreferredIsbn() {
            return !isbn13.isEmpty()
                    ? isbn13
                    : isbn10;
        }

        @NonNull
        public String getBarcodeValue() {
            return barcodeValue;
        }

        @NonNull
        public String getOnlineSearchQuery() {
            return onlineSearchQuery;
        }

        public float getTitleConfidence() {
            return titleConfidence;
        }

        public float getSubjectConfidence() {
            return subjectConfidence;
        }

        public float getClassConfidence() {
            return classConfidence;
        }

        public float getBoardConfidence() {
            return boardConfidence;
        }

        public float getPublisherConfidence() {
            return publisherConfidence;
        }

        public float getOverallConfidence() {
            return overallConfidence;
        }

        @NonNull
        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasBookTitle() {
            return !bookTitle.isEmpty();
        }

        public boolean hasIsbn() {
            return !isbn10.isEmpty()
                    || !isbn13.isEmpty();
        }

        public boolean isReadyForOnlineSearch() {
            return !onlineSearchQuery.isEmpty()
                    && (hasBookTitle()
                    || hasIsbn());
        }

        public boolean requiresParentConfirmation() {
            return overallConfidence < 80f
                    || !warnings.isEmpty()
                    || !hasBookTitle();
        }
    }

    private static final class DetectedValue {

        @NonNull
        private final String value;

        private final float confidence;

        private DetectedValue(
                @NonNull String value,
                float confidence
        ) {
            this.value =
                    value.trim();

            this.confidence =
                    Math.max(
                            0f,
                            Math.min(
                                    100f,
                                    confidence
                            )
                    );
        }

        @NonNull
        private static DetectedValue empty() {
            return new DetectedValue(
                    "",
                    0f
            );
        }

        @NonNull
        private String getValue() {
            return value;
        }

        private float getConfidence() {
            return confidence;
        }
    }

    private static final class SubjectDefinition {

        @NonNull
        private final String subjectId;

        @NonNull
        private final String displayName;

        @NonNull
        private final List<String> keywords;

        private SubjectDefinition(
                @NonNull String subjectId,
                @NonNull String displayName,
                @NonNull String... keywords
        ) {
            this.subjectId =
                    subjectId;

            this.displayName =
                    displayName;

            List<String> preparedKeywords =
                    new ArrayList<>();

            Collections.addAll(
                    preparedKeywords,
                    keywords
            );

            this.keywords =
                    Collections.unmodifiableList(
                            preparedKeywords
                    );
        }

        @NonNull
        private String getSubjectId() {
            return subjectId;
        }

        @NonNull
        private String getDisplayName() {
            return displayName;
        }

        @NonNull
        private List<String> getKeywords() {
            return keywords;
        }
    }

    private static final class BoardDefinition {

        @NonNull
        private final String boardId;

        @NonNull
        private final String displayName;

        @NonNull
        private final List<String> keywords;

        private BoardDefinition(
                @NonNull String boardId,
                @NonNull String displayName,
                @NonNull String... keywords
        ) {
            this.boardId =
                    boardId;

            this.displayName =
                    displayName;

            List<String> preparedKeywords =
                    new ArrayList<>();

            Collections.addAll(
                    preparedKeywords,
                    keywords
            );

            this.keywords =
                    Collections.unmodifiableList(
                            preparedKeywords
                    );
        }

        @NonNull
        private String getDisplayName() {
            return displayName;
        }

        @NonNull
        private List<String> getKeywords() {
            return keywords;
        }

        private boolean matches(
                @Nullable String boardValue
        ) {
            if (boardValue == null
                    || boardValue.trim().isEmpty()) {

                return false;
            }

            String normalizedBoard =
                    boardValue.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            return boardId.equals(
                    normalizedBoard
            )
                    || displayName.equalsIgnoreCase(
                    boardValue.trim()
            );
        }
    }
}