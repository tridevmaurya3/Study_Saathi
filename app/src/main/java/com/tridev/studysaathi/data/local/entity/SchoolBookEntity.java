package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Locale;

@Entity(
        tableName = "school_books",
        foreignKeys = {
                @ForeignKey(
                        entity = SchoolSubjectEntity.class,
                        parentColumns = "subject_row_id",
                        childColumns = "subject_row_id",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(
                        value = {"subject_row_id"}
                ),
                @Index(
                        value = {
                                "subject_row_id",
                                "book_id"
                        },
                        unique = true
                ),
                @Index(
                        value = {"isbn_10"}
                ),
                @Index(
                        value = {"isbn_13"}
                ),
                @Index(
                        value = {
                                "subject_row_id",
                                "is_active",
                                "sort_order"
                        }
                ),
                @Index(
                        value = {
                                "download_status",
                                "content_processing_status"
                        }
                )
        }
)
public class SchoolBookEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "book_row_id")
    private long bookRowId;

    @ColumnInfo(name = "subject_row_id")
    private long subjectRowId;

    @NonNull
    @ColumnInfo(name = "book_id")
    private String bookId = "";

    @NonNull
    @ColumnInfo(name = "book_title")
    private String bookTitle = "";

    @NonNull
    @ColumnInfo(name = "book_subtitle")
    private String bookSubtitle = "";

    @NonNull
    @ColumnInfo(name = "author_name")
    private String authorName = "";

    @NonNull
    @ColumnInfo(name = "publisher_name")
    private String publisherName = "";

    @NonNull
    @ColumnInfo(name = "edition_name")
    private String editionName = "";

    @NonNull
    @ColumnInfo(name = "publication_year")
    private String publicationYear = "";

    @NonNull
    @ColumnInfo(name = "academic_session")
    private String academicSession = "";

    @NonNull
    @ColumnInfo(name = "class_name")
    private String className = "";

    @NonNull
    @ColumnInfo(name = "education_board")
    private String educationBoard = "";

    @NonNull
    @ColumnInfo(name = "study_medium")
    private String studyMedium = "";

    @NonNull
    @ColumnInfo(name = "isbn_10")
    private String isbn10 = "";

    @NonNull
    @ColumnInfo(name = "isbn_13")
    private String isbn13 = "";

    @NonNull
    @ColumnInfo(name = "book_code")
    private String bookCode = "";

    @NonNull
    @ColumnInfo(name = "barcode_value")
    private String barcodeValue = "";

    @NonNull
    @ColumnInfo(name = "barcode_format")
    private String barcodeFormat = "";

    @NonNull
    @ColumnInfo(name = "cover_image_url")
    private String coverImageUrl = "";

    @NonNull
    @ColumnInfo(name = "local_cover_image_path")
    private String localCoverImagePath = "";

    @NonNull
    @ColumnInfo(name = "scanned_cover_image_path")
    private String scannedCoverImagePath = "";

    @NonNull
    @ColumnInfo(name = "detected_cover_text")
    private String detectedCoverText = "";

    @ColumnInfo(
            name = "cover_match_confidence",
            defaultValue = "0"
    )
    private float coverMatchConfidence;

    @NonNull
    @ColumnInfo(
            name = "book_source",
            defaultValue = "'SCHOOL_BOOK'"
    )
    private String bookSource =
            "SCHOOL_BOOK";

    @NonNull
    @ColumnInfo(name = "online_provider")
    private String onlineProvider = "";

    @NonNull
    @ColumnInfo(name = "online_volume_id")
    private String onlineVolumeId = "";

    @NonNull
    @ColumnInfo(name = "online_information_url")
    private String onlineInformationUrl = "";

    @NonNull
    @ColumnInfo(name = "official_source_url")
    private String officialSourceUrl = "";

    @NonNull
    @ColumnInfo(name = "authorized_download_url")
    private String authorizedDownloadUrl = "";

    @NonNull
    @ColumnInfo(
            name = "access_type",
            defaultValue = "'METADATA_ONLY'"
    )
    private String accessType =
            "METADATA_ONLY";

    @NonNull
    @ColumnInfo(
            name = "license_type",
            defaultValue = "'UNKNOWN'"
    )
    private String licenseType =
            "UNKNOWN";

    @ColumnInfo(
            name = "download_allowed",
            defaultValue = "0"
    )
    private boolean downloadAllowed;

    @ColumnInfo(
            name = "preview_allowed",
            defaultValue = "0"
    )
    private boolean previewAllowed;

    @ColumnInfo(
            name = "parent_confirmed_match",
            defaultValue = "0"
    )
    private boolean parentConfirmedMatch;

    @ColumnInfo(
            name = "official_source_verified",
            defaultValue = "0"
    )
    private boolean officialSourceVerified;

    @NonNull
    @ColumnInfo(
            name = "download_status",
            defaultValue = "'NOT_DOWNLOADED'"
    )
    private String downloadStatus =
            "NOT_DOWNLOADED";

    @ColumnInfo(
            name = "download_progress",
            defaultValue = "0"
    )
    private int downloadProgress;

    @NonNull
    @ColumnInfo(name = "local_book_file_path")
    private String localBookFilePath = "";

    @NonNull
    @ColumnInfo(name = "local_content_folder_path")
    private String localContentFolderPath = "";

    @NonNull
    @ColumnInfo(name = "downloaded_file_name")
    private String downloadedFileName = "";

    @NonNull
    @ColumnInfo(name = "downloaded_file_mime_type")
    private String downloadedFileMimeType = "";

    @ColumnInfo(
            name = "downloaded_file_size_bytes",
            defaultValue = "0"
    )
    private long downloadedFileSizeBytes;

    @NonNull
    @ColumnInfo(name = "downloaded_file_checksum_sha256")
    private String downloadedFileChecksumSha256 = "";

    @NonNull
    @ColumnInfo(
            name = "content_processing_status",
            defaultValue = "'NOT_STARTED'"
    )
    private String contentProcessingStatus =
            "NOT_STARTED";

    @ColumnInfo(
            name = "chapter_count",
            defaultValue = "0"
    )
    private int chapterCount;

    @ColumnInfo(
            name = "processed_chapter_count",
            defaultValue = "0"
    )
    private int processedChapterCount;

    @ColumnInfo(
            name = "generated_lesson_count",
            defaultValue = "0"
    )
    private int generatedLessonCount;

    @ColumnInfo(
            name = "generated_quiz_question_count",
            defaultValue = "0"
    )
    private int generatedQuizQuestionCount;

    @ColumnInfo(
            name = "offline_available",
            defaultValue = "0"
    )
    private boolean offlineAvailable;

    @ColumnInfo(
            name = "ai_tutor_enabled",
            defaultValue = "1"
    )
    private boolean aiTutorEnabled = true;

    @ColumnInfo(
            name = "is_active",
            defaultValue = "1"
    )
    private boolean active = true;

    @ColumnInfo(
            name = "is_primary_book",
            defaultValue = "0"
    )
    private boolean primaryBook;

    @ColumnInfo(
            name = "sort_order",
            defaultValue = "0"
    )
    private int sortOrder;

    @ColumnInfo(name = "last_online_search_at")
    private long lastOnlineSearchAt;

    @ColumnInfo(name = "last_download_attempt_at")
    private long lastDownloadAttemptAt;

    @ColumnInfo(name = "download_completed_at")
    private long downloadCompletedAt;

    @ColumnInfo(name = "last_content_processed_at")
    private long lastContentProcessedAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public SchoolBookEntity() {
        // Required empty constructor for Room.
    }

    public long getBookRowId() {
        return bookRowId;
    }

    public void setBookRowId(
            long bookRowId
    ) {
        this.bookRowId =
                Math.max(
                        0L,
                        bookRowId
                );
    }

    public long getSubjectRowId() {
        return subjectRowId;
    }

    public void setSubjectRowId(
            long subjectRowId
    ) {
        this.subjectRowId =
                Math.max(
                        0L,
                        subjectRowId
                );
    }

    @NonNull
    public String getBookId() {
        return bookId;
    }

    public void setBookId(
            @NonNull String bookId
    ) {
        this.bookId =
                normalizeIdentifier(
                        bookId
                );
    }

    @NonNull
    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(
            @NonNull String bookTitle
    ) {
        this.bookTitle =
                normalizeRequiredText(
                        bookTitle
                );
    }

    @NonNull
    public String getBookSubtitle() {
        return bookSubtitle;
    }

    public void setBookSubtitle(
            @NonNull String bookSubtitle
    ) {
        this.bookSubtitle =
                normalizeOptionalText(
                        bookSubtitle
                );
    }

    @NonNull
    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(
            @NonNull String authorName
    ) {
        this.authorName =
                normalizeOptionalText(
                        authorName
                );
    }

    @NonNull
    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(
            @NonNull String publisherName
    ) {
        this.publisherName =
                normalizeOptionalText(
                        publisherName
                );
    }

    @NonNull
    public String getEditionName() {
        return editionName;
    }

    public void setEditionName(
            @NonNull String editionName
    ) {
        this.editionName =
                normalizeOptionalText(
                        editionName
                );
    }

    @NonNull
    public String getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(
            @NonNull String publicationYear
    ) {
        this.publicationYear =
                normalizeOptionalText(
                        publicationYear
                );
    }

    @NonNull
    public String getAcademicSession() {
        return academicSession;
    }

    public void setAcademicSession(
            @NonNull String academicSession
    ) {
        this.academicSession =
                normalizeOptionalText(
                        academicSession
                );
    }

    @NonNull
    public String getClassName() {
        return className;
    }

    public void setClassName(
            @NonNull String className
    ) {
        this.className =
                normalizeOptionalText(
                        className
                );
    }

    @NonNull
    public String getEducationBoard() {
        return educationBoard;
    }

    public void setEducationBoard(
            @NonNull String educationBoard
    ) {
        this.educationBoard =
                normalizeOptionalText(
                        educationBoard
                );
    }

    @NonNull
    public String getStudyMedium() {
        return studyMedium;
    }

    public void setStudyMedium(
            @NonNull String studyMedium
    ) {
        this.studyMedium =
                normalizeOptionalText(
                        studyMedium
                );
    }

    @NonNull
    public String getIsbn10() {
        return isbn10;
    }

    public void setIsbn10(
            @NonNull String isbn10
    ) {
        this.isbn10 =
                normalizeIsbn(
                        isbn10,
                        10
                );
    }

    @NonNull
    public String getIsbn13() {
        return isbn13;
    }

    public void setIsbn13(
            @NonNull String isbn13
    ) {
        this.isbn13 =
                normalizeIsbn(
                        isbn13,
                        13
                );
    }

    @NonNull
    public String getBookCode() {
        return bookCode;
    }

    public void setBookCode(
            @NonNull String bookCode
    ) {
        this.bookCode =
                normalizeOptionalText(
                        bookCode
                );
    }

    @NonNull
    public String getBarcodeValue() {
        return barcodeValue;
    }

    public void setBarcodeValue(
            @NonNull String barcodeValue
    ) {
        this.barcodeValue =
                normalizeOptionalText(
                        barcodeValue
                );
    }

    @NonNull
    public String getBarcodeFormat() {
        return barcodeFormat;
    }

    public void setBarcodeFormat(
            @NonNull String barcodeFormat
    ) {
        this.barcodeFormat =
                normalizeOptionalText(
                        barcodeFormat
                )
                        .toUpperCase(
                                Locale.ROOT
                        );
    }

    @NonNull
    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(
            @NonNull String coverImageUrl
    ) {
        this.coverImageUrl =
                normalizeOptionalText(
                        coverImageUrl
                );
    }

    @NonNull
    public String getLocalCoverImagePath() {
        return localCoverImagePath;
    }

    public void setLocalCoverImagePath(
            @NonNull String localCoverImagePath
    ) {
        this.localCoverImagePath =
                normalizeOptionalText(
                        localCoverImagePath
                );
    }

    @NonNull
    public String getScannedCoverImagePath() {
        return scannedCoverImagePath;
    }

    public void setScannedCoverImagePath(
            @NonNull String scannedCoverImagePath
    ) {
        this.scannedCoverImagePath =
                normalizeOptionalText(
                        scannedCoverImagePath
                );
    }

    @NonNull
    public String getDetectedCoverText() {
        return detectedCoverText;
    }

    public void setDetectedCoverText(
            @NonNull String detectedCoverText
    ) {
        this.detectedCoverText =
                normalizeOptionalText(
                        detectedCoverText
                );
    }

    public float getCoverMatchConfidence() {
        return coverMatchConfidence;
    }

    public void setCoverMatchConfidence(
            float coverMatchConfidence
    ) {
        this.coverMatchConfidence =
                Math.max(
                        0f,
                        Math.min(
                                100f,
                                coverMatchConfidence
                        )
                );
    }

    @NonNull
    public String getBookSource() {
        return bookSource;
    }

    public void setBookSource(
            @NonNull String bookSource
    ) {
        this.bookSource =
                normalizeBookSource(
                        bookSource
                );
    }

    @NonNull
    public String getOnlineProvider() {
        return onlineProvider;
    }

    public void setOnlineProvider(
            @NonNull String onlineProvider
    ) {
        this.onlineProvider =
                normalizeOptionalText(
                        onlineProvider
                );
    }

    @NonNull
    public String getOnlineVolumeId() {
        return onlineVolumeId;
    }

    public void setOnlineVolumeId(
            @NonNull String onlineVolumeId
    ) {
        this.onlineVolumeId =
                normalizeOptionalText(
                        onlineVolumeId
                );
    }

    @NonNull
    public String getOnlineInformationUrl() {
        return onlineInformationUrl;
    }

    public void setOnlineInformationUrl(
            @NonNull String onlineInformationUrl
    ) {
        this.onlineInformationUrl =
                normalizeOptionalText(
                        onlineInformationUrl
                );
    }

    @NonNull
    public String getOfficialSourceUrl() {
        return officialSourceUrl;
    }

    public void setOfficialSourceUrl(
            @NonNull String officialSourceUrl
    ) {
        this.officialSourceUrl =
                normalizeOptionalText(
                        officialSourceUrl
                );
    }

    @NonNull
    public String getAuthorizedDownloadUrl() {
        return authorizedDownloadUrl;
    }

    public void setAuthorizedDownloadUrl(
            @NonNull String authorizedDownloadUrl
    ) {
        this.authorizedDownloadUrl =
                normalizeOptionalText(
                        authorizedDownloadUrl
                );
    }

    @NonNull
    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(
            @NonNull String accessType
    ) {
        this.accessType =
                normalizeAccessType(
                        accessType
                );
    }

    @NonNull
    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(
            @NonNull String licenseType
    ) {
        this.licenseType =
                normalizeLicenseType(
                        licenseType
                );
    }

    public boolean isDownloadAllowed() {
        return downloadAllowed;
    }

    public void setDownloadAllowed(
            boolean downloadAllowed
    ) {
        this.downloadAllowed =
                downloadAllowed;
    }

    public boolean isPreviewAllowed() {
        return previewAllowed;
    }

    public void setPreviewAllowed(
            boolean previewAllowed
    ) {
        this.previewAllowed =
                previewAllowed;
    }

    public boolean isParentConfirmedMatch() {
        return parentConfirmedMatch;
    }

    public void setParentConfirmedMatch(
            boolean parentConfirmedMatch
    ) {
        this.parentConfirmedMatch =
                parentConfirmedMatch;
    }

    public boolean isOfficialSourceVerified() {
        return officialSourceVerified;
    }

    public void setOfficialSourceVerified(
            boolean officialSourceVerified
    ) {
        this.officialSourceVerified =
                officialSourceVerified;
    }

    @NonNull
    public String getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(
            @NonNull String downloadStatus
    ) {
        this.downloadStatus =
                normalizeDownloadStatus(
                        downloadStatus
                );
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(
            int downloadProgress
    ) {
        this.downloadProgress =
                Math.max(
                        0,
                        Math.min(
                                100,
                                downloadProgress
                        )
                );
    }

    @NonNull
    public String getLocalBookFilePath() {
        return localBookFilePath;
    }

    public void setLocalBookFilePath(
            @NonNull String localBookFilePath
    ) {
        this.localBookFilePath =
                normalizeOptionalText(
                        localBookFilePath
                );
    }

    @NonNull
    public String getLocalContentFolderPath() {
        return localContentFolderPath;
    }

    public void setLocalContentFolderPath(
            @NonNull String localContentFolderPath
    ) {
        this.localContentFolderPath =
                normalizeOptionalText(
                        localContentFolderPath
                );
    }

    @NonNull
    public String getDownloadedFileName() {
        return downloadedFileName;
    }

    public void setDownloadedFileName(
            @NonNull String downloadedFileName
    ) {
        this.downloadedFileName =
                normalizeOptionalText(
                        downloadedFileName
                );
    }

    @NonNull
    public String getDownloadedFileMimeType() {
        return downloadedFileMimeType;
    }

    public void setDownloadedFileMimeType(
            @NonNull String downloadedFileMimeType
    ) {
        this.downloadedFileMimeType =
                normalizeOptionalText(
                        downloadedFileMimeType
                );
    }

    public long getDownloadedFileSizeBytes() {
        return downloadedFileSizeBytes;
    }

    public void setDownloadedFileSizeBytes(
            long downloadedFileSizeBytes
    ) {
        this.downloadedFileSizeBytes =
                Math.max(
                        0L,
                        downloadedFileSizeBytes
                );
    }

    @NonNull
    public String getDownloadedFileChecksumSha256() {
        return downloadedFileChecksumSha256;
    }

    public void setDownloadedFileChecksumSha256(
            @NonNull String downloadedFileChecksumSha256
    ) {
        this.downloadedFileChecksumSha256 =
                normalizeOptionalText(
                        downloadedFileChecksumSha256
                )
                        .toLowerCase(
                                Locale.ROOT
                        );
    }

    @NonNull
    public String getContentProcessingStatus() {
        return contentProcessingStatus;
    }

    public void setContentProcessingStatus(
            @NonNull String contentProcessingStatus
    ) {
        this.contentProcessingStatus =
                normalizeContentProcessingStatus(
                        contentProcessingStatus
                );
    }

    public int getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(
            int chapterCount
    ) {
        this.chapterCount =
                Math.max(
                        0,
                        chapterCount
                );
    }

    public int getProcessedChapterCount() {
        return processedChapterCount;
    }

    public void setProcessedChapterCount(
            int processedChapterCount
    ) {
        this.processedChapterCount =
                Math.max(
                        0,
                        processedChapterCount
                );
    }

    public int getGeneratedLessonCount() {
        return generatedLessonCount;
    }

    public void setGeneratedLessonCount(
            int generatedLessonCount
    ) {
        this.generatedLessonCount =
                Math.max(
                        0,
                        generatedLessonCount
                );
    }

    public int getGeneratedQuizQuestionCount() {
        return generatedQuizQuestionCount;
    }

    public void setGeneratedQuizQuestionCount(
            int generatedQuizQuestionCount
    ) {
        this.generatedQuizQuestionCount =
                Math.max(
                        0,
                        generatedQuizQuestionCount
                );
    }

    public boolean isOfflineAvailable() {
        return offlineAvailable;
    }

    public void setOfflineAvailable(
            boolean offlineAvailable
    ) {
        this.offlineAvailable =
                offlineAvailable;
    }

    public boolean isAiTutorEnabled() {
        return aiTutorEnabled;
    }

    public void setAiTutorEnabled(
            boolean aiTutorEnabled
    ) {
        this.aiTutorEnabled =
                aiTutorEnabled;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(
            boolean active
    ) {
        this.active =
                active;
    }

    public boolean isPrimaryBook() {
        return primaryBook;
    }

    public void setPrimaryBook(
            boolean primaryBook
    ) {
        this.primaryBook =
                primaryBook;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(
            int sortOrder
    ) {
        this.sortOrder =
                Math.max(
                        0,
                        sortOrder
                );
    }

    public long getLastOnlineSearchAt() {
        return lastOnlineSearchAt;
    }

    public void setLastOnlineSearchAt(
            long lastOnlineSearchAt
    ) {
        this.lastOnlineSearchAt =
                Math.max(
                        0L,
                        lastOnlineSearchAt
                );
    }

    public long getLastDownloadAttemptAt() {
        return lastDownloadAttemptAt;
    }

    public void setLastDownloadAttemptAt(
            long lastDownloadAttemptAt
    ) {
        this.lastDownloadAttemptAt =
                Math.max(
                        0L,
                        lastDownloadAttemptAt
                );
    }

    public long getDownloadCompletedAt() {
        return downloadCompletedAt;
    }

    public void setDownloadCompletedAt(
            long downloadCompletedAt
    ) {
        this.downloadCompletedAt =
                Math.max(
                        0L,
                        downloadCompletedAt
                );
    }

    public long getLastContentProcessedAt() {
        return lastContentProcessedAt;
    }

    public void setLastContentProcessedAt(
            long lastContentProcessedAt
    ) {
        this.lastContentProcessedAt =
                Math.max(
                        0L,
                        lastContentProcessedAt
                );
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            long createdAt
    ) {
        this.createdAt =
                Math.max(
                        0L,
                        createdAt
                );
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            long updatedAt
    ) {
        this.updatedAt =
                Math.max(
                        0L,
                        updatedAt
                );
    }

    public boolean hasIsbn() {
        return !isbn10.isEmpty()
                || !isbn13.isEmpty();
    }

    public boolean hasBarcodeInformation() {
        return !barcodeValue.isEmpty();
    }

    public boolean hasScannedCover() {
        return !scannedCoverImagePath.isEmpty();
    }

    public boolean hasOnlineMatch() {
        return !onlineVolumeId.isEmpty()
                || !onlineInformationUrl.isEmpty()
                || !officialSourceUrl.isEmpty();
    }

    public boolean hasAuthorizedDownload() {
        return downloadAllowed
                && officialSourceVerified
                && !authorizedDownloadUrl.isEmpty();
    }

    public boolean isDownloaded() {
        return "DOWNLOADED".equals(
                downloadStatus
        )
                && !localBookFilePath.isEmpty();
    }

    public boolean isDownloadInProgress() {
        return "DOWNLOADING".equals(
                downloadStatus
        );
    }

    public boolean isContentReady() {
        return "COMPLETED".equals(
                contentProcessingStatus
        );
    }

    public boolean isContentProcessing() {
        return "PROCESSING".equals(
                contentProcessingStatus
        );
    }

    public boolean hasMinimumBookInformation() {
        return subjectRowId > 0L
                && !bookId.isEmpty()
                && !bookTitle.isEmpty();
    }

    public void markOnlineSearchCompleted() {
        lastOnlineSearchAt =
                System.currentTimeMillis();

        updatedAt =
                lastOnlineSearchAt;
    }

    public void markDownloadStarted() {
        downloadStatus =
                "DOWNLOADING";

        downloadProgress =
                0;

        lastDownloadAttemptAt =
                System.currentTimeMillis();

        updatedAt =
                lastDownloadAttemptAt;
    }

    public void markDownloadCompleted(
            @NonNull String filePath,
            @NonNull String fileName,
            @NonNull String mimeType,
            long fileSizeBytes,
            @NonNull String checksumSha256
    ) {
        localBookFilePath =
                normalizeOptionalText(
                        filePath
                );

        downloadedFileName =
                normalizeOptionalText(
                        fileName
                );

        downloadedFileMimeType =
                normalizeOptionalText(
                        mimeType
                );

        downloadedFileSizeBytes =
                Math.max(
                        0L,
                        fileSizeBytes
                );

        downloadedFileChecksumSha256 =
                normalizeOptionalText(
                        checksumSha256
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        downloadStatus =
                "DOWNLOADED";

        downloadProgress =
                100;

        downloadCompletedAt =
                System.currentTimeMillis();

        offlineAvailable =
                !localBookFilePath.isEmpty();

        updatedAt =
                downloadCompletedAt;
    }

    public void markDownloadFailed() {
        downloadStatus =
                "FAILED";

        downloadProgress =
                0;

        lastDownloadAttemptAt =
                System.currentTimeMillis();

        updatedAt =
                lastDownloadAttemptAt;
    }

    public void markContentProcessingStarted() {
        contentProcessingStatus =
                "PROCESSING";

        updatedAt =
                System.currentTimeMillis();
    }

    public void markContentProcessingCompleted(
            int chapterCount,
            int generatedLessonCount,
            int generatedQuizQuestionCount
    ) {
        this.chapterCount =
                Math.max(
                        0,
                        chapterCount
                );

        processedChapterCount =
                this.chapterCount;

        this.generatedLessonCount =
                Math.max(
                        0,
                        generatedLessonCount
                );

        this.generatedQuizQuestionCount =
                Math.max(
                        0,
                        generatedQuizQuestionCount
                );

        contentProcessingStatus =
                "COMPLETED";

        lastContentProcessedAt =
                System.currentTimeMillis();

        updatedAt =
                lastContentProcessedAt;
    }

    public void markContentProcessingFailed() {
        contentProcessingStatus =
                "FAILED";

        lastContentProcessedAt =
                System.currentTimeMillis();

        updatedAt =
                lastContentProcessedAt;
    }

    @NonNull
    public static String createBookId(
            @NonNull String bookTitle,
            @NonNull String isbnValue
    ) {
        String safeBookTitle =
                normalizeIdentifier(
                        bookTitle
                );

        String safeIsbn =
                isbnValue.trim()
                        .replaceAll(
                                "[^0-9Xx]",
                                ""
                        )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (safeIsbn.isEmpty()) {
            return safeBookTitle;
        }

        return safeBookTitle
                + "_"
                + safeIsbn;
    }

    @NonNull
    private static String normalizeRequiredText(
            @NonNull String value
    ) {
        String normalizedValue =
                value.trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    "Required book information cannot be empty."
            );
        }

        return normalizedValue;
    }

    @NonNull
    private static String normalizeOptionalText(
            @NonNull String value
    ) {
        return value.trim();
    }

    @NonNull
    private static String normalizeIdentifier(
            @NonNull String value
    ) {
        String normalizedValue =
                value.trim()
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

        while (normalizedValue.startsWith("_")) {
            normalizedValue =
                    normalizedValue.substring(
                            1
                    );
        }

        while (normalizedValue.endsWith("_")) {
            normalizedValue =
                    normalizedValue.substring(
                            0,
                            normalizedValue.length() - 1
                    );
        }

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    "Book identifier is invalid."
            );
        }

        return normalizedValue;
    }

    @NonNull
    private static String normalizeIsbn(
            @NonNull String value,
            int requiredLength
    ) {
        String normalizedIsbn =
                value.trim()
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
                != requiredLength) {

            return normalizedIsbn;
        }

        return normalizedIsbn;
    }

    @NonNull
    private static String normalizeBookSource(
            @NonNull String value
    ) {
        String normalizedValue =
                normalizeEnumText(
                        value
                );

        switch (normalizedValue) {
            case "NCERT":
            case "SCHOOL_BOOK":
            case "PRIVATE_PUBLISHER":
            case "OPEN_EDUCATIONAL_RESOURCE":
            case "TEACHER_NOTES":
            case "PARENT_IMPORTED":
            case "AI_ASSISTED":
            case "CUSTOM":
                return normalizedValue;

            default:
                return "SCHOOL_BOOK";
        }
    }

    @NonNull
    private static String normalizeAccessType(
            @NonNull String value
    ) {
        String normalizedValue =
                normalizeEnumText(
                        value
                );

        switch (normalizedValue) {
            case "FULL_DOWNLOAD":
            case "FULL_ONLINE":
            case "PARTIAL_PREVIEW":
            case "METADATA_ONLY":
            case "USER_IMPORTED":
            case "NOT_AVAILABLE":
                return normalizedValue;

            default:
                return "METADATA_ONLY";
        }
    }

    @NonNull
    private static String normalizeLicenseType(
            @NonNull String value
    ) {
        String normalizedValue =
                normalizeEnumText(
                        value
                );

        switch (normalizedValue) {
            case "PUBLIC_DOMAIN":
            case "OPEN_LICENSE":
            case "OFFICIAL_FREE_ACCESS":
            case "PURCHASED_OR_OWNED":
            case "PRIVATE_COPYRIGHT":
            case "RESTRICTED":
            case "UNKNOWN":
                return normalizedValue;

            default:
                return "UNKNOWN";
        }
    }

    @NonNull
    private static String normalizeDownloadStatus(
            @NonNull String value
    ) {
        String normalizedValue =
                normalizeEnumText(
                        value
                );

        switch (normalizedValue) {
            case "NOT_DOWNLOADED":
            case "QUEUED":
            case "DOWNLOADING":
            case "PAUSED":
            case "DOWNLOADED":
            case "FAILED":
            case "NOT_ALLOWED":
            case "REMOVED":
                return normalizedValue;

            default:
                return "NOT_DOWNLOADED";
        }
    }

    @NonNull
    private static String normalizeContentProcessingStatus(
            @NonNull String value
    ) {
        String normalizedValue =
                normalizeEnumText(
                        value
                );

        switch (normalizedValue) {
            case "NOT_STARTED":
            case "QUEUED":
            case "PROCESSING":
            case "COMPLETED":
            case "PARTIALLY_COMPLETED":
            case "FAILED":
            case "NOT_SUPPORTED":
                return normalizedValue;

            default:
                return "NOT_STARTED";
        }
    }

    @NonNull
    private static String normalizeEnumText(
            @NonNull String value
    ) {
        return value.trim()
                .toUpperCase(
                        Locale.ROOT
                )
                .replace(
                        "-",
                        "_"
                )
                .replace(
                        " ",
                        "_"
                );
    }
}