package com.tridev.studysaathi.data.content.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BookCoverScanResult {

    private static final float MINIMUM_CONFIDENCE =
            0f;

    private static final float MAXIMUM_CONFIDENCE =
            100f;

    @NonNull
    private final String scanId;

    @NonNull
    private final String localImagePath;

    @NonNull
    private final ScanSource scanSource;

    @NonNull
    private final String detectedFullText;

    @NonNull
    private final String detectedBookTitle;

    @NonNull
    private final String detectedSubtitle;

    @NonNull
    private final String detectedSubjectName;

    @NonNull
    private final String detectedClassName;

    @NonNull
    private final String detectedEducationBoard;

    @NonNull
    private final String detectedPublisherName;

    @NonNull
    private final String detectedAuthorName;

    @NonNull
    private final String detectedEditionName;

    @NonNull
    private final String detectedPublicationYear;

    @NonNull
    private final String detectedStudyMedium;

    @NonNull
    private final String detectedIsbn10;

    @NonNull
    private final String detectedIsbn13;

    @NonNull
    private final String detectedBarcodeValue;

    @NonNull
    private final String detectedBarcodeFormat;

    private final float overallConfidence;

    private final float titleConfidence;

    private final float subjectConfidence;

    private final float classConfidence;

    private final float publisherConfidence;

    private final float isbnConfidence;

    private final int detectedTextBlockCount;

    private final long scannedAt;

    private final long processingDurationMilliseconds;

    @NonNull
    private final ScanStatus scanStatus;

    @NonNull
    private final List<String> detectedTextLines;

    @NonNull
    private final List<String> warnings;

    @NonNull
    private final List<DetectedBarcode> detectedBarcodes;

    private BookCoverScanResult(
            @NonNull Builder builder
    ) {
        scanId =
                requireIdentifier(
                        builder.scanId,
                        "Scan ID"
                );

        localImagePath =
                normalizeOptionalText(
                        builder.localImagePath
                );

        scanSource =
                builder.scanSource;

        detectedFullText =
                normalizeOptionalText(
                        builder.detectedFullText
                );

        detectedBookTitle =
                normalizeOptionalText(
                        builder.detectedBookTitle
                );

        detectedSubtitle =
                normalizeOptionalText(
                        builder.detectedSubtitle
                );

        detectedSubjectName =
                normalizeOptionalText(
                        builder.detectedSubjectName
                );

        detectedClassName =
                normalizeOptionalText(
                        builder.detectedClassName
                );

        detectedEducationBoard =
                normalizeOptionalText(
                        builder.detectedEducationBoard
                );

        detectedPublisherName =
                normalizeOptionalText(
                        builder.detectedPublisherName
                );

        detectedAuthorName =
                normalizeOptionalText(
                        builder.detectedAuthorName
                );

        detectedEditionName =
                normalizeOptionalText(
                        builder.detectedEditionName
                );

        detectedPublicationYear =
                normalizeOptionalText(
                        builder.detectedPublicationYear
                );

        detectedStudyMedium =
                normalizeOptionalText(
                        builder.detectedStudyMedium
                );

        detectedIsbn10 =
                normalizeIsbn(
                        builder.detectedIsbn10,
                        10
                );

        detectedIsbn13 =
                normalizeIsbn(
                        builder.detectedIsbn13,
                        13
                );

        detectedBarcodeValue =
                normalizeOptionalText(
                        builder.detectedBarcodeValue
                );

        detectedBarcodeFormat =
                normalizeOptionalText(
                        builder.detectedBarcodeFormat
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        overallConfidence =
                normalizeConfidence(
                        builder.overallConfidence
                );

        titleConfidence =
                normalizeConfidence(
                        builder.titleConfidence
                );

        subjectConfidence =
                normalizeConfidence(
                        builder.subjectConfidence
                );

        classConfidence =
                normalizeConfidence(
                        builder.classConfidence
                );

        publisherConfidence =
                normalizeConfidence(
                        builder.publisherConfidence
                );

        isbnConfidence =
                normalizeConfidence(
                        builder.isbnConfidence
                );

        detectedTextBlockCount =
                Math.max(
                        0,
                        builder.detectedTextBlockCount
                );

        scannedAt =
                builder.scannedAt > 0L
                        ? builder.scannedAt
                        : System.currentTimeMillis();

        processingDurationMilliseconds =
                Math.max(
                        0L,
                        builder.processingDurationMilliseconds
                );

        scanStatus =
                builder.scanStatus;

        detectedTextLines =
                createImmutableTextList(
                        builder.detectedTextLines
                );

        warnings =
                createImmutableTextList(
                        builder.warnings
                );

        detectedBarcodes =
                createImmutableBarcodeList(
                        builder.detectedBarcodes
                );
    }

    @NonNull
    public static Builder builder(
            @NonNull String scanId
    ) {
        return new Builder(
                scanId
        );
    }

    @NonNull
    public String getScanId() {
        return scanId;
    }

    @NonNull
    public String getLocalImagePath() {
        return localImagePath;
    }

    @NonNull
    public ScanSource getScanSource() {
        return scanSource;
    }

    @NonNull
    public String getDetectedFullText() {
        return detectedFullText;
    }

    @NonNull
    public String getDetectedBookTitle() {
        return detectedBookTitle;
    }

    @NonNull
    public String getDetectedSubtitle() {
        return detectedSubtitle;
    }

    @NonNull
    public String getDetectedSubjectName() {
        return detectedSubjectName;
    }

    @NonNull
    public String getDetectedClassName() {
        return detectedClassName;
    }

    @NonNull
    public String getDetectedEducationBoard() {
        return detectedEducationBoard;
    }

    @NonNull
    public String getDetectedPublisherName() {
        return detectedPublisherName;
    }

    @NonNull
    public String getDetectedAuthorName() {
        return detectedAuthorName;
    }

    @NonNull
    public String getDetectedEditionName() {
        return detectedEditionName;
    }

    @NonNull
    public String getDetectedPublicationYear() {
        return detectedPublicationYear;
    }

    @NonNull
    public String getDetectedStudyMedium() {
        return detectedStudyMedium;
    }

    @NonNull
    public String getDetectedIsbn10() {
        return detectedIsbn10;
    }

    @NonNull
    public String getDetectedIsbn13() {
        return detectedIsbn13;
    }

    @NonNull
    public String getDetectedBarcodeValue() {
        return detectedBarcodeValue;
    }

    @NonNull
    public String getDetectedBarcodeFormat() {
        return detectedBarcodeFormat;
    }

    public float getOverallConfidence() {
        return overallConfidence;
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

    public float getPublisherConfidence() {
        return publisherConfidence;
    }

    public float getIsbnConfidence() {
        return isbnConfidence;
    }

    public int getDetectedTextBlockCount() {
        return detectedTextBlockCount;
    }

    public long getScannedAt() {
        return scannedAt;
    }

    public long getProcessingDurationMilliseconds() {
        return processingDurationMilliseconds;
    }

    @NonNull
    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    @NonNull
    public List<String> getDetectedTextLines() {
        return detectedTextLines;
    }

    @NonNull
    public List<String> getWarnings() {
        return warnings;
    }

    @NonNull
    public List<DetectedBarcode> getDetectedBarcodes() {
        return detectedBarcodes;
    }

    public boolean hasImage() {
        return !localImagePath.isEmpty();
    }

    public boolean hasDetectedText() {
        return !detectedFullText.isEmpty()
                || !detectedTextLines.isEmpty();
    }

    public boolean hasBookTitle() {
        return !detectedBookTitle.isEmpty();
    }

    public boolean hasSubject() {
        return !detectedSubjectName.isEmpty();
    }

    public boolean hasClassInformation() {
        return !detectedClassName.isEmpty();
    }

    public boolean hasPublisherInformation() {
        return !detectedPublisherName.isEmpty();
    }

    public boolean hasIsbn() {
        return !detectedIsbn10.isEmpty()
                || !detectedIsbn13.isEmpty();
    }

    public boolean hasBarcode() {
        return !detectedBarcodeValue.isEmpty()
                || !detectedBarcodes.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean isSuccessful() {
        return scanStatus == ScanStatus.SUCCESS
                || scanStatus == ScanStatus.PARTIAL_SUCCESS;
    }

    public boolean requiresManualReview() {
        return scanStatus == ScanStatus.PARTIAL_SUCCESS
                || overallConfidence < 70f
                || !hasBookTitle()
                || hasWarnings();
    }

    public boolean isReadyForOnlineSearch() {
        return hasIsbn()
                || hasBarcode()
                || hasBookTitle()
                || hasDetectedText();
    }

    @NonNull
    public String getPreferredIsbn() {
        if (!detectedIsbn13.isEmpty()) {
            return detectedIsbn13;
        }

        return detectedIsbn10;
    }

    @NonNull
    public String createOnlineSearchQuery() {
        List<String> queryParts =
                new ArrayList<>();

        if (hasIsbn()) {
            queryParts.add(
                    getPreferredIsbn()
            );
        }

        if (hasBookTitle()) {
            queryParts.add(
                    detectedBookTitle
            );
        }

        if (hasSubject()) {
            queryParts.add(
                    detectedSubjectName
            );
        }

        if (hasClassInformation()) {
            queryParts.add(
                    detectedClassName
            );
        }

        if (!detectedEducationBoard.isEmpty()) {
            queryParts.add(
                    detectedEducationBoard
            );
        }

        if (hasPublisherInformation()) {
            queryParts.add(
                    detectedPublisherName
            );
        }

        StringBuilder queryBuilder =
                new StringBuilder();

        for (String queryPart : queryParts) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(
                        ' '
                );
            }

            queryBuilder.append(
                    queryPart
            );
        }

        return queryBuilder
                .toString()
                .trim();
    }

    @NonNull
    public String createDisplaySummary() {
        StringBuilder summaryBuilder =
                new StringBuilder();

        appendSummaryLine(
                summaryBuilder,
                "Book",
                detectedBookTitle
        );

        appendSummaryLine(
                summaryBuilder,
                "Subject",
                detectedSubjectName
        );

        appendSummaryLine(
                summaryBuilder,
                "Class",
                detectedClassName
        );

        appendSummaryLine(
                summaryBuilder,
                "Board",
                detectedEducationBoard
        );

        appendSummaryLine(
                summaryBuilder,
                "Publisher",
                detectedPublisherName
        );

        appendSummaryLine(
                summaryBuilder,
                "ISBN",
                getPreferredIsbn()
        );

        appendSummaryLine(
                summaryBuilder,
                "Confidence",
                String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        overallConfidence
                )
        );

        return summaryBuilder
                .toString()
                .trim();
    }

    private static void appendSummaryLine(
            @NonNull StringBuilder builder,
            @NonNull String label,
            @NonNull String value
    ) {
        if (value.trim().isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(
                    '\n'
            );
        }

        builder.append(
                label
        );

        builder.append(
                ": "
        );

        builder.append(
                value.trim()
        );
    }

    public enum ScanSource {

        CAMERA(
                "Camera",
                "कैमरा"
        ),

        GALLERY(
                "Gallery",
                "गैलरी"
        ),

        FILE_IMPORT(
                "File Import",
                "फाइल इम्पोर्ट"
        ),

        BARCODE_ONLY(
                "Barcode Only",
                "केवल बारकोड"
        ),

        MANUAL_ENTRY(
                "Manual Entry",
                "मैनुअल एंट्री"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        ScanSource(
                @NonNull String englishLabel,
                @NonNull String hindiLabel
        ) {
            this.englishLabel =
                    englishLabel;

            this.hindiLabel =
                    hindiLabel;
        }

        @NonNull
        public String getEnglishLabel() {
            return englishLabel;
        }

        @NonNull
        public String getHindiLabel() {
            return hindiLabel;
        }
    }

    public enum ScanStatus {

        NOT_STARTED(
                "Not Started",
                "शुरू नहीं हुआ"
        ),

        PROCESSING(
                "Processing",
                "प्रोसेस हो रहा है"
        ),

        SUCCESS(
                "Success",
                "सफल"
        ),

        PARTIAL_SUCCESS(
                "Partial Success",
                "आंशिक रूप से सफल"
        ),

        NO_TEXT_FOUND(
                "No Text Found",
                "कोई टेक्स्ट नहीं मिला"
        ),

        IMAGE_UNCLEAR(
                "Image Unclear",
                "चित्र स्पष्ट नहीं है"
        ),

        FAILED(
                "Failed",
                "असफल"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        ScanStatus(
                @NonNull String englishLabel,
                @NonNull String hindiLabel
        ) {
            this.englishLabel =
                    englishLabel;

            this.hindiLabel =
                    hindiLabel;
        }

        @NonNull
        public String getEnglishLabel() {
            return englishLabel;
        }

        @NonNull
        public String getHindiLabel() {
            return hindiLabel;
        }
    }

    public static final class DetectedBarcode {

        @NonNull
        private final String rawValue;

        @NonNull
        private final String displayValue;

        @NonNull
        private final String barcodeFormat;

        @NonNull
        private final String valueType;

        private final float confidence;

        public DetectedBarcode(
                @NonNull String rawValue,
                @Nullable String displayValue,
                @Nullable String barcodeFormat,
                @Nullable String valueType,
                float confidence
        ) {
            this.rawValue =
                    requireText(
                            rawValue,
                            "Barcode value"
                    );

            this.displayValue =
                    normalizeOptionalText(
                            displayValue
                    );

            this.barcodeFormat =
                    normalizeOptionalText(
                            barcodeFormat
                    )
                            .toUpperCase(
                                    Locale.ROOT
                            );

            this.valueType =
                    normalizeOptionalText(
                            valueType
                    )
                            .toUpperCase(
                                    Locale.ROOT
                            );

            this.confidence =
                    normalizeConfidence(
                            confidence
                    );
        }

        @NonNull
        public String getRawValue() {
            return rawValue;
        }

        @NonNull
        public String getDisplayValue() {
            return displayValue;
        }

        @NonNull
        public String getBarcodeFormat() {
            return barcodeFormat;
        }

        @NonNull
        public String getValueType() {
            return valueType;
        }

        public float getConfidence() {
            return confidence;
        }

        public boolean looksLikeIsbn() {
            String normalizedValue =
                    rawValue.replaceAll(
                            "[^0-9Xx]",
                            ""
                    );

            return normalizedValue.length() == 10
                    || normalizedValue.length() == 13;
        }
    }

    public static final class Builder {

        @NonNull
        private final String scanId;

        @NonNull
        private String localImagePath = "";

        @NonNull
        private ScanSource scanSource =
                ScanSource.CAMERA;

        @NonNull
        private String detectedFullText = "";

        @NonNull
        private String detectedBookTitle = "";

        @NonNull
        private String detectedSubtitle = "";

        @NonNull
        private String detectedSubjectName = "";

        @NonNull
        private String detectedClassName = "";

        @NonNull
        private String detectedEducationBoard = "";

        @NonNull
        private String detectedPublisherName = "";

        @NonNull
        private String detectedAuthorName = "";

        @NonNull
        private String detectedEditionName = "";

        @NonNull
        private String detectedPublicationYear = "";

        @NonNull
        private String detectedStudyMedium = "";

        @NonNull
        private String detectedIsbn10 = "";

        @NonNull
        private String detectedIsbn13 = "";

        @NonNull
        private String detectedBarcodeValue = "";

        @NonNull
        private String detectedBarcodeFormat = "";

        private float overallConfidence;

        private float titleConfidence;

        private float subjectConfidence;

        private float classConfidence;

        private float publisherConfidence;

        private float isbnConfidence;

        private int detectedTextBlockCount;

        private long scannedAt;

        private long processingDurationMilliseconds;

        @NonNull
        private ScanStatus scanStatus =
                ScanStatus.NOT_STARTED;

        @NonNull
        private final List<String> detectedTextLines =
                new ArrayList<>();

        @NonNull
        private final List<String> warnings =
                new ArrayList<>();

        @NonNull
        private final List<DetectedBarcode>
                detectedBarcodes =
                new ArrayList<>();

        private Builder(
                @NonNull String scanId
        ) {
            this.scanId =
                    scanId;
        }

        @NonNull
        public Builder setLocalImagePath(
                @Nullable String localImagePath
        ) {
            this.localImagePath =
                    normalizeOptionalText(
                            localImagePath
                    );

            return this;
        }

        @NonNull
        public Builder setScanSource(
                @NonNull ScanSource scanSource
        ) {
            this.scanSource =
                    scanSource;

            return this;
        }

        @NonNull
        public Builder setDetectedFullText(
                @Nullable String detectedFullText
        ) {
            this.detectedFullText =
                    normalizeOptionalText(
                            detectedFullText
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedBookTitle(
                @Nullable String detectedBookTitle
        ) {
            this.detectedBookTitle =
                    normalizeOptionalText(
                            detectedBookTitle
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedSubtitle(
                @Nullable String detectedSubtitle
        ) {
            this.detectedSubtitle =
                    normalizeOptionalText(
                            detectedSubtitle
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedSubjectName(
                @Nullable String detectedSubjectName
        ) {
            this.detectedSubjectName =
                    normalizeOptionalText(
                            detectedSubjectName
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedClassName(
                @Nullable String detectedClassName
        ) {
            this.detectedClassName =
                    normalizeOptionalText(
                            detectedClassName
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedEducationBoard(
                @Nullable String detectedEducationBoard
        ) {
            this.detectedEducationBoard =
                    normalizeOptionalText(
                            detectedEducationBoard
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedPublisherName(
                @Nullable String detectedPublisherName
        ) {
            this.detectedPublisherName =
                    normalizeOptionalText(
                            detectedPublisherName
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedAuthorName(
                @Nullable String detectedAuthorName
        ) {
            this.detectedAuthorName =
                    normalizeOptionalText(
                            detectedAuthorName
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedEditionName(
                @Nullable String detectedEditionName
        ) {
            this.detectedEditionName =
                    normalizeOptionalText(
                            detectedEditionName
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedPublicationYear(
                @Nullable String detectedPublicationYear
        ) {
            this.detectedPublicationYear =
                    normalizeOptionalText(
                            detectedPublicationYear
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedStudyMedium(
                @Nullable String detectedStudyMedium
        ) {
            this.detectedStudyMedium =
                    normalizeOptionalText(
                            detectedStudyMedium
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedIsbn10(
                @Nullable String detectedIsbn10
        ) {
            this.detectedIsbn10 =
                    normalizeOptionalText(
                            detectedIsbn10
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedIsbn13(
                @Nullable String detectedIsbn13
        ) {
            this.detectedIsbn13 =
                    normalizeOptionalText(
                            detectedIsbn13
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedBarcodeValue(
                @Nullable String detectedBarcodeValue
        ) {
            this.detectedBarcodeValue =
                    normalizeOptionalText(
                            detectedBarcodeValue
                    );

            return this;
        }

        @NonNull
        public Builder setDetectedBarcodeFormat(
                @Nullable String detectedBarcodeFormat
        ) {
            this.detectedBarcodeFormat =
                    normalizeOptionalText(
                            detectedBarcodeFormat
                    );

            return this;
        }

        @NonNull
        public Builder setOverallConfidence(
                float overallConfidence
        ) {
            this.overallConfidence =
                    overallConfidence;

            return this;
        }

        @NonNull
        public Builder setTitleConfidence(
                float titleConfidence
        ) {
            this.titleConfidence =
                    titleConfidence;

            return this;
        }

        @NonNull
        public Builder setSubjectConfidence(
                float subjectConfidence
        ) {
            this.subjectConfidence =
                    subjectConfidence;

            return this;
        }

        @NonNull
        public Builder setClassConfidence(
                float classConfidence
        ) {
            this.classConfidence =
                    classConfidence;

            return this;
        }

        @NonNull
        public Builder setPublisherConfidence(
                float publisherConfidence
        ) {
            this.publisherConfidence =
                    publisherConfidence;

            return this;
        }

        @NonNull
        public Builder setIsbnConfidence(
                float isbnConfidence
        ) {
            this.isbnConfidence =
                    isbnConfidence;

            return this;
        }

        @NonNull
        public Builder setDetectedTextBlockCount(
                int detectedTextBlockCount
        ) {
            this.detectedTextBlockCount =
                    detectedTextBlockCount;

            return this;
        }

        @NonNull
        public Builder setScannedAt(
                long scannedAt
        ) {
            this.scannedAt =
                    scannedAt;

            return this;
        }

        @NonNull
        public Builder setProcessingDurationMilliseconds(
                long processingDurationMilliseconds
        ) {
            this.processingDurationMilliseconds =
                    processingDurationMilliseconds;

            return this;
        }

        @NonNull
        public Builder setScanStatus(
                @NonNull ScanStatus scanStatus
        ) {
            this.scanStatus =
                    scanStatus;

            return this;
        }

        @NonNull
        public Builder addDetectedTextLine(
                @Nullable String textLine
        ) {
            String normalizedTextLine =
                    normalizeOptionalText(
                            textLine
                    );

            if (!normalizedTextLine.isEmpty()) {
                detectedTextLines.add(
                        normalizedTextLine
                );
            }

            return this;
        }

        @NonNull
        public Builder addWarning(
                @Nullable String warning
        ) {
            String normalizedWarning =
                    normalizeOptionalText(
                            warning
                    );

            if (!normalizedWarning.isEmpty()) {
                warnings.add(
                        normalizedWarning
                );
            }

            return this;
        }

        @NonNull
        public Builder addDetectedBarcode(
                @NonNull DetectedBarcode barcode
        ) {
            detectedBarcodes.add(
                    barcode
            );

            return this;
        }

        @NonNull
        public BookCoverScanResult build() {
            return new BookCoverScanResult(
                    this
            );
        }
    }

    @NonNull
    private static String requireText(
            @Nullable String value,
            @NonNull String fieldName
    ) {
        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be empty."
            );
        }

        return value.trim();
    }

    @NonNull
    private static String requireIdentifier(
            @Nullable String value,
            @NonNull String fieldName
    ) {
        String normalizedIdentifier =
                requireText(
                        value,
                        fieldName
                )
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9_-]",
                                "_"
                        )
                        .replaceAll(
                                "_+",
                                "_"
                        );

        while (normalizedIdentifier.startsWith("_")) {
            normalizedIdentifier =
                    normalizedIdentifier.substring(
                            1
                    );
        }

        while (normalizedIdentifier.endsWith("_")) {
            normalizedIdentifier =
                    normalizedIdentifier.substring(
                            0,
                            normalizedIdentifier.length() - 1
                    );
        }

        if (normalizedIdentifier.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName
                            + " is invalid."
            );
        }

        return normalizedIdentifier;
    }

    @NonNull
    private static String normalizeOptionalText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    @NonNull
    private static String normalizeIsbn(
            @Nullable String value,
            int expectedLength
    ) {
        String normalizedIsbn =
                normalizeOptionalText(
                        value
                )
                        .replaceAll(
                                "[^0-9Xx]",
                                ""
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (normalizedIsbn.isEmpty()) {
            return "";
        }

        if (normalizedIsbn.length()
                != expectedLength) {

            return normalizedIsbn;
        }

        return normalizedIsbn;
    }

    private static float normalizeConfidence(
            float confidence
    ) {
        return Math.max(
                MINIMUM_CONFIDENCE,
                Math.min(
                        MAXIMUM_CONFIDENCE,
                        confidence
                )
        );
    }

    @NonNull
    private static List<String>
    createImmutableTextList(
            @NonNull List<String> sourceList
    ) {
        List<String> preparedList =
                new ArrayList<>();

        for (String item : sourceList) {
            String normalizedItem =
                    normalizeOptionalText(
                            item
                    );

            if (!normalizedItem.isEmpty()) {
                preparedList.add(
                        normalizedItem
                );
            }
        }

        return Collections.unmodifiableList(
                preparedList
        );
    }

    @NonNull
    private static List<DetectedBarcode>
    createImmutableBarcodeList(
            @NonNull List<DetectedBarcode> sourceList
    ) {
        List<DetectedBarcode> preparedList =
                new ArrayList<>();

        for (DetectedBarcode barcode : sourceList) {
            if (barcode == null) {
                throw new IllegalArgumentException(
                        "Detected barcode list cannot contain null items."
                );
            }

            preparedList.add(
                    barcode
            );
        }

        return Collections.unmodifiableList(
                preparedList
        );
    }
}