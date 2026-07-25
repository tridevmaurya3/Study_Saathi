package com.tridev.studysaathi.data.content.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model.BookMatchReviewData;
import com.tridev.studysaathi.data.local.entity.SchoolBookEntity;

import java.util.Locale;

public final class BookMatchReviewMapper {

    private BookMatchReviewMapper() {
        /*
         * Utility class.
         */
    }

    /**
     * Parent-confirmed review result को SchoolBookEntity
     * में बदलता है।
     *
     * @param reviewData     Scan और online search का review snapshot.
     * @param subjectRowId   वह database subject जिसमें book जोड़ी जाएगी.
     * @param academicSession Student का academic session.
     * @param sortOrder      Subject की book list में position.
     * @param primaryBook    क्या यह subject की primary book होगी.
     */
    @NonNull
    public static SchoolBookEntity
    createConfirmedBookEntity(
            @NonNull BookMatchReviewData reviewData,
            long subjectRowId,
            @Nullable String academicSession,
            int sortOrder,
            boolean primaryBook
    ) {
        if (subjectRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school subject row ID is required."
            );
        }

        long currentTime =
                System.currentTimeMillis();

        String preferredBookTitle =
                valueOrFallback(
                        reviewData.getPreferredBookTitle(),
                        "Unknown School Book"
                );

        String preferredIsbn =
                normalizeIsbn(
                        reviewData.getPreferredIsbn()
                );

        String isbn10 =
                preferredIsbn.length() == 10
                        ? preferredIsbn
                        : "";

        String isbn13 =
                preferredIsbn.length() == 13
                        ? preferredIsbn
                        : "";

        String barcodeValue =
                safeText(
                        reviewData.getDetectedBarcodeValue()
                );

        String scannedImageLocation =
                findScannedImageLocation(
                        reviewData
                );

        SchoolBookEntity schoolBook =
                new SchoolBookEntity();

        schoolBook.setSubjectRowId(
                subjectRowId
        );

        schoolBook.setBookId(
                createStableBookId(
                        preferredBookTitle,
                        preferredIsbn,
                        subjectRowId,
                        currentTime
                )
        );

        schoolBook.setBookTitle(
                preferredBookTitle
        );

        schoolBook.setBookSubtitle(
                reviewData.getOnlineBookSubtitle()
        );

        schoolBook.setAuthorName(
                firstNonBlank(
                        reviewData.getOnlineBookAuthors(),
                        reviewData.getDetectedAuthorName()
                )
        );

        schoolBook.setPublisherName(
                reviewData.getPreferredPublisherName()
        );

        schoolBook.setEditionName(
                firstNonBlank(
                        reviewData.getOnlineEditionName(),
                        reviewData.getDetectedEditionName()
                )
        );

        schoolBook.setPublicationYear(
                firstNonBlank(
                        reviewData.getOnlinePublicationDate(),
                        reviewData.getDetectedPublicationYear()
                )
        );

        schoolBook.setAcademicSession(
                safeText(
                        academicSession
                )
        );

        schoolBook.setClassName(
                reviewData.getPreferredClassName()
        );

        schoolBook.setEducationBoard(
                reviewData.getPreferredEducationBoard()
        );

        schoolBook.setStudyMedium(
                firstNonBlank(
                        reviewData.getOnlineStudyMedium(),
                        reviewData.getDetectedStudyMedium()
                )
        );

        schoolBook.setIsbn10(
                isbn10
        );

        schoolBook.setIsbn13(
                isbn13
        );

        schoolBook.setBookCode(
                firstNonBlank(
                        preferredIsbn,
                        barcodeValue
                )
        );

        schoolBook.setBarcodeValue(
                barcodeValue
        );

        schoolBook.setBarcodeFormat(
                inferBarcodeFormat(
                        barcodeValue
                )
        );

        schoolBook.setCoverImageUrl(
                reviewData.getOnlineCoverImageUrl()
        );

        schoolBook.setLocalCoverImagePath(
                scannedImageLocation
        );

        schoolBook.setScannedCoverImagePath(
                scannedImageLocation
        );

        schoolBook.setDetectedCoverText(
                createDetectedInformationSummary(
                        reviewData
                )
        );

        schoolBook.setCoverMatchConfidence(
                reviewData.isBestMatchAvailable()
                        ? reviewData.getOverallMatchScore()
                        : reviewData
                        .getDetectedOverallConfidence()
        );

        schoolBook.setBookSource(
                determineBookSource(
                        reviewData
                )
        );

        schoolBook.setOnlineProvider(
                reviewData.getOnlineProviderName()
        );

        /*
         * Current BookMatchReviewData में provider volume ID
         * उपलब्ध नहीं है। इसे future search-result expansion
         * में जोड़ा जाएगा।
         */
        schoolBook.setOnlineVolumeId(
                ""
        );

        schoolBook.setOnlineInformationUrl(
                reviewData.getOnlineInformationUrl()
        );

        schoolBook.setOfficialSourceUrl(
                reviewData.getOnlineOfficialSourceUrl()
        );

        schoolBook.setAuthorizedDownloadUrl(
                reviewData.getOnlineAuthorizedDownloadUrl()
        );

        schoolBook.setAccessType(
                convertAccessTypeToDatabaseValue(
                        reviewData.getOnlineAccessType()
                )
        );

        schoolBook.setLicenseType(
                convertLicenseTypeToDatabaseValue(
                        reviewData.getOnlineLicenseType()
                )
        );

        schoolBook.setDownloadAllowed(
                reviewData
                        .hasAuthorizedDownloadUrl()
        );

        schoolBook.setPreviewAllowed(
                reviewData
                        .hasOnlinePreviewUrl()
        );

        schoolBook.setParentConfirmedMatch(
                true
        );

        schoolBook.setOfficialSourceVerified(
                reviewData
                        .isOfficialSourceVerified()
        );

        schoolBook.setDownloadStatus(
                reviewData.hasAuthorizedDownloadUrl()
                        ? "NOT_DOWNLOADED"
                        : "NOT_ALLOWED"
        );

        schoolBook.setDownloadProgress(
                0
        );

        schoolBook.setLocalBookFilePath(
                ""
        );

        schoolBook.setLocalContentFolderPath(
                ""
        );

        schoolBook.setDownloadedFileName(
                ""
        );

        schoolBook.setDownloadedFileMimeType(
                ""
        );

        schoolBook.setDownloadedFileSizeBytes(
                0L
        );

        schoolBook.setDownloadedFileChecksumSha256(
                ""
        );

        schoolBook.setContentProcessingStatus(
                "NOT_STARTED"
        );

        schoolBook.setChapterCount(
                0
        );

        schoolBook.setProcessedChapterCount(
                0
        );

        schoolBook.setGeneratedLessonCount(
                0
        );

        schoolBook.setGeneratedQuizQuestionCount(
                0
        );

        schoolBook.setOfflineAvailable(
                false
        );

        schoolBook.setAiTutorEnabled(
                true
        );

        schoolBook.setActive(
                true
        );

        schoolBook.setPrimaryBook(
                primaryBook
        );

        schoolBook.setSortOrder(
                Math.max(
                        0,
                        sortOrder
                )
        );

        schoolBook.setLastOnlineSearchAt(
                reviewData.getSearchedAt()
        );

        schoolBook.setLastDownloadAttemptAt(
                0L
        );

        schoolBook.setDownloadCompletedAt(
                0L
        );

        schoolBook.setLastContentProcessedAt(
                0L
        );

        schoolBook.setCreatedAt(
                currentTime
        );

        schoolBook.setUpdatedAt(
                currentTime
        );

        return schoolBook;
    }

    @NonNull
    private static String findScannedImageLocation(
            @NonNull BookMatchReviewData reviewData
    ) {
        String privateImagePath =
                safeText(
                        reviewData.getPrivateImagePath()
                );

        if (!privateImagePath.isEmpty()) {
            return privateImagePath;
        }

        return safeText(
                reviewData.getSelectedImageUri()
        );
    }

    @NonNull
    private static String createStableBookId(
            @NonNull String bookTitle,
            @NonNull String isbn,
            long subjectRowId,
            long createdAt
    ) {
        if (!isbn.isEmpty()) {
            return "isbn_"
                    + isbn.toLowerCase(
                    Locale.ROOT
            );
        }

        String normalizedTitle =
                bookTitle.trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9]+",
                                "_"
                        )
                        .replaceAll(
                                "_+",
                                "_"
                        );

        normalizedTitle =
                trimUnderscores(
                        normalizedTitle
                );

        if (!normalizedTitle.isEmpty()) {
            return normalizedTitle
                    + "_"
                    + subjectRowId;
        }

        return "school_book_"
                + subjectRowId
                + "_"
                + createdAt;
    }

    @NonNull
    private static String trimUnderscores(
            @NonNull String value
    ) {
        String result =
                value;

        while (result.startsWith(
                "_"
        )) {
            result =
                    result.substring(
                            1
                    );
        }

        while (result.endsWith(
                "_"
        )) {
            result =
                    result.substring(
                            0,
                            result.length() - 1
                    );
        }

        return result;
    }

    @NonNull
    private static String normalizeIsbn(
            @Nullable String isbn
    ) {
        String normalizedIsbn =
                safeText(
                        isbn
                )
                        .replaceAll(
                                "[^0-9Xx]",
                                ""
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (normalizedIsbn.length() == 10
                || normalizedIsbn.length() == 13) {

            return normalizedIsbn;
        }

        return "";
    }

    @NonNull
    private static String inferBarcodeFormat(
            @Nullable String barcodeValue
    ) {
        String normalizedBarcode =
                safeText(
                        barcodeValue
                )
                        .replaceAll(
                                "[^0-9Xx]",
                                ""
                        );

        if (normalizedBarcode.length() == 13) {
            return "EAN_13";
        }

        if (normalizedBarcode.length() == 10) {
            return "ISBN_10";
        }

        if (!normalizedBarcode.isEmpty()) {
            return "UNKNOWN";
        }

        return "";
    }

    @NonNull
    private static String determineBookSource(
            @NonNull BookMatchReviewData reviewData
    ) {
        String combinedBookInformation =
                (
                        reviewData.getPreferredBookTitle()
                                + " "
                                + reviewData
                                .getPreferredPublisherName()
                                + " "
                                + reviewData
                                .getOnlineProviderName()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (combinedBookInformation.contains(
                "ncert"
        )
                || combinedBookInformation.contains(
                "national council of educational research"
        )) {

            return "NCERT";
        }

        return "SCHOOL_BOOK";
    }

    @NonNull
    private static String
    convertAccessTypeToDatabaseValue(
            @Nullable String accessTypeLabel
    ) {
        String normalizedValue =
                normalizeEnumLabel(
                        accessTypeLabel
                );

        switch (normalizedValue) {
            case "FULL_DOWNLOAD":
                return "FULL_DOWNLOAD";

            case "FULL_ONLINE":
            case "FULL_ONLINE_ACCESS":
                return "FULL_ONLINE";

            case "PARTIAL_PREVIEW":
            case "PREVIEW":
                return "PARTIAL_PREVIEW";

            case "USER_IMPORTED":
                return "USER_IMPORTED";

            case "NOT_AVAILABLE":
                return "NOT_AVAILABLE";

            case "METADATA_ONLY":
            default:
                return "METADATA_ONLY";
        }
    }

    @NonNull
    private static String
    convertLicenseTypeToDatabaseValue(
            @Nullable String licenseTypeLabel
    ) {
        String normalizedValue =
                normalizeEnumLabel(
                        licenseTypeLabel
                );

        switch (normalizedValue) {
            case "PUBLIC_DOMAIN":
                return "PUBLIC_DOMAIN";

            case "OPEN_LICENSE":
                return "OPEN_LICENSE";

            case "OFFICIAL_FREE_ACCESS":
                return "OFFICIAL_FREE_ACCESS";

            case "PURCHASED_OR_OWNED":
                return "PURCHASED_OR_OWNED";

            case "PRIVATE_COPYRIGHT":
                return "PRIVATE_COPYRIGHT";

            case "RESTRICTED":
                return "RESTRICTED";

            case "UNKNOWN":
            default:
                return "UNKNOWN";
        }
    }

    @NonNull
    private static String normalizeEnumLabel(
            @Nullable String value
    ) {
        return safeText(
                value
        )
                .toUpperCase(
                        Locale.ROOT
                )
                .replace(
                        "-",
                        "_"
                )
                .replaceAll(
                        "[^A-Z0-9]+",
                        "_"
                )
                .replaceAll(
                        "_+",
                        "_"
                );
    }

    @NonNull
    private static String
    createDetectedInformationSummary(
            @NonNull BookMatchReviewData reviewData
    ) {
        StringBuilder summary =
                new StringBuilder();

        appendSummaryValue(
                summary,
                "Title",
                reviewData.getDetectedBookTitle()
        );

        appendSummaryValue(
                summary,
                "Subject",
                reviewData.getDetectedSubjectName()
        );

        appendSummaryValue(
                summary,
                "Class",
                reviewData.getDetectedClassName()
        );

        appendSummaryValue(
                summary,
                "Board",
                reviewData.getDetectedEducationBoard()
        );

        appendSummaryValue(
                summary,
                "Publisher",
                reviewData.getDetectedPublisherName()
        );

        appendSummaryValue(
                summary,
                "Author",
                reviewData.getDetectedAuthorName()
        );

        appendSummaryValue(
                summary,
                "Edition",
                reviewData.getDetectedEditionName()
        );

        appendSummaryValue(
                summary,
                "ISBN",
                reviewData.getDetectedIsbn()
        );

        appendSummaryValue(
                summary,
                "Barcode",
                reviewData.getDetectedBarcodeValue()
        );

        return summary.toString();
    }

    private static void appendSummaryValue(
            @NonNull StringBuilder summary,
            @NonNull String label,
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (summary.length() > 0) {
            summary.append(
                    '\n'
            );
        }

        summary.append(
                label
        );

        summary.append(
                ": "
        );

        summary.append(
                safeValue
        );
    }

    @NonNull
    private static String firstNonBlank(
            @Nullable String firstValue,
            @Nullable String secondValue
    ) {
        String safeFirstValue =
                safeText(
                        firstValue
                );

        if (!safeFirstValue.isEmpty()) {
            return safeFirstValue;
        }

        return safeText(
                secondValue
        );
    }

    @NonNull
    private static String valueOrFallback(
            @Nullable String value,
            @NonNull String fallback
    ) {
        String safeValue =
                safeText(
                        value
                );

        return safeValue.isEmpty()
                ? fallback
                : safeValue;
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
}