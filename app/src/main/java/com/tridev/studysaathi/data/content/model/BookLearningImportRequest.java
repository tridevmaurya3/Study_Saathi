package com.tridev.studysaathi.data.content.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;

/**
 * Full book PDF/image import की एक सुरक्षित, process-independent request.
 *
 * <p>Android Uri को String के रूप में रखा गया है ताकि request को Intent,
 * savedInstanceState या background processing में भेजा जा सके। चुनी गई Uri
 * पर persistable read permission लेने की जिम्मेदारी file-picker screen की है।
 */
public final class BookLearningImportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SOURCE_TYPE_PDF = "PDF";
    public static final String SOURCE_TYPE_IMAGE = "IMAGE";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_READING_DOCUMENT =
            "READING_DOCUMENT";
    public static final String STATUS_DETECTING_CHAPTERS =
            "DETECTING_CHAPTERS";
    public static final String STATUS_EXTRACTING_CONTENT =
            "EXTRACTING_CONTENT";
    public static final String STATUS_PENDING_PARENT_REVIEW =
            "PENDING_PARENT_REVIEW";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @NonNull
    private final String requestId;

    private final long schoolBookRowId;

    @NonNull
    private final String sourceUri;

    @NonNull
    private final String displayName;

    @NonNull
    private final String mimeType;

    @NonNull
    private final String sourceType;

    private final long sourceSizeBytes;
    private final long createdAt;

    @NonNull
    private String processingStatus;

    @NonNull
    private String failureMessage;

    private BookLearningImportRequest(
            @NonNull String requestId,
            long schoolBookRowId,
            @NonNull String sourceUri,
            @NonNull String displayName,
            @NonNull String mimeType,
            @NonNull String sourceType,
            long sourceSizeBytes,
            long createdAt,
            @NonNull String processingStatus,
            @NonNull String failureMessage
    ) {
        this.requestId = requireText(
                requestId,
                "Request ID"
        );

        if (schoolBookRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid exact school book row ID is required."
            );
        }

        this.schoolBookRowId = schoolBookRowId;
        this.sourceUri = requireText(
                sourceUri,
                "Source Uri"
        );
        this.displayName = requireText(
                displayName,
                "Display name"
        );
        this.mimeType = normalizeMimeType(
                mimeType
        );
        this.sourceType = normalizeSourceType(
                sourceType,
                this.mimeType
        );
        this.sourceSizeBytes =
                Math.max(0L, sourceSizeBytes);
        this.createdAt =
                createdAt > 0L
                        ? createdAt
                        : System.currentTimeMillis();
        this.processingStatus =
                normalizeStatus(processingStatus);
        this.failureMessage =
                safeText(failureMessage);
    }

    @NonNull
    public static BookLearningImportRequest create(
            long schoolBookRowId,
            @NonNull String sourceUri,
            @NonNull String displayName,
            @NonNull String mimeType,
            long sourceSizeBytes
    ) {
        String normalizedMimeType =
                normalizeMimeType(mimeType);

        return new BookLearningImportRequest(
                UUID.randomUUID().toString(),
                schoolBookRowId,
                sourceUri,
                displayName,
                normalizedMimeType,
                detectSourceType(normalizedMimeType),
                sourceSizeBytes,
                System.currentTimeMillis(),
                STATUS_PENDING,
                ""
        );
    }

    @NonNull
    public String getRequestId() {
        return requestId;
    }

    public long getSchoolBookRowId() {
        return schoolBookRowId;
    }

    @NonNull
    public String getSourceUri() {
        return sourceUri;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    @NonNull
    public String getSourceType() {
        return sourceType;
    }

    public long getSourceSizeBytes() {
        return sourceSizeBytes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @NonNull
    public String getProcessingStatus() {
        return processingStatus;
    }

    @NonNull
    public String getFailureMessage() {
        return failureMessage;
    }

    public boolean isPdf() {
        return SOURCE_TYPE_PDF.equals(sourceType);
    }

    public boolean isImage() {
        return SOURCE_TYPE_IMAGE.equals(sourceType);
    }

    public boolean isTerminal() {
        return STATUS_COMPLETED.equals(processingStatus)
                || STATUS_FAILED.equals(processingStatus);
    }

    public void markReadingDocument() {
        updateStatus(
                STATUS_READING_DOCUMENT,
                ""
        );
    }

    public void markDetectingChapters() {
        updateStatus(
                STATUS_DETECTING_CHAPTERS,
                ""
        );
    }

    public void markExtractingContent() {
        updateStatus(
                STATUS_EXTRACTING_CONTENT,
                ""
        );
    }

    public void markPendingParentReview() {
        updateStatus(
                STATUS_PENDING_PARENT_REVIEW,
                ""
        );
    }

    public void markCompleted() {
        updateStatus(
                STATUS_COMPLETED,
                ""
        );
    }

    public void markFailed(
            @NonNull String message
    ) {
        String safeMessage =
                requireText(
                        message,
                        "Failure message"
                );

        updateStatus(
                STATUS_FAILED,
                safeMessage
        );
    }

    private void updateStatus(
            @NonNull String status,
            @NonNull String message
    ) {
        processingStatus =
                normalizeStatus(status);
        failureMessage =
                safeText(message);
    }

    @NonNull
    private static String detectSourceType(
            @NonNull String mimeType
    ) {
        if ("application/pdf".equals(mimeType)) {
            return SOURCE_TYPE_PDF;
        }

        if (mimeType.startsWith("image/")) {
            return SOURCE_TYPE_IMAGE;
        }

        throw new IllegalArgumentException(
                "Only PDF or image book files are supported."
        );
    }

    @NonNull
    private static String normalizeSourceType(
            @NonNull String sourceType,
            @NonNull String mimeType
    ) {
        String normalized =
                safeText(sourceType)
                        .toUpperCase(Locale.US);

        String detected =
                detectSourceType(mimeType);

        if (!detected.equals(normalized)) {
            throw new IllegalArgumentException(
                    "Source type does not match the selected file."
            );
        }

        return normalized;
    }

    @NonNull
    private static String normalizeMimeType(
            @NonNull String mimeType
    ) {
        String normalized =
                safeText(mimeType)
                        .toLowerCase(Locale.US);

        if (!"application/pdf".equals(normalized)
                && !normalized.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Only PDF or image book files are supported."
            );
        }

        return normalized;
    }

    @NonNull
    private static String normalizeStatus(
            @NonNull String status
    ) {
        String normalized =
                safeText(status)
                        .toUpperCase(Locale.US);

        switch (normalized) {
            case STATUS_PENDING:
            case STATUS_READING_DOCUMENT:
            case STATUS_DETECTING_CHAPTERS:
            case STATUS_EXTRACTING_CONTENT:
            case STATUS_PENDING_PARENT_REVIEW:
            case STATUS_COMPLETED:
            case STATUS_FAILED:
                return normalized;

            default:
                throw new IllegalArgumentException(
                        "Unknown learning import status: "
                                + normalized
                );
        }
    }

    @NonNull
    private static String requireText(
            @NonNull String value,
            @NonNull String fieldName
    ) {
        String normalized =
                safeText(value);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    @NonNull
    private static String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
}
