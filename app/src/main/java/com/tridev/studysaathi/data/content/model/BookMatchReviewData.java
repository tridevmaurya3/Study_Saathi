package com.tridev.studysaathi.data.content.model;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.scanner.BookCoverMetadataExtractor;
import com.tridev.studysaathi.data.content.search.BookDiscoveryCoordinator;
import com.tridev.studysaathi.data.content.search.BookScanDiscoveryCoordinator;
import com.tridev.studysaathi.data.content.search.OnlineBookMatchEvaluator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class BookMatchReviewData
        implements Serializable {

    private static final long serialVersionUID =
            1L;

    @NonNull
    private final String selectedImageUri;

    @NonNull
    private final String privateImagePath;

    @NonNull
    private final String scanSourceName;

    @NonNull
    private final String detectedBookTitle;

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
    private final String detectedIsbn;

    @NonNull
    private final String detectedBarcodeValue;

    private final float detectedOverallConfidence;

    private final boolean bestMatchAvailable;

    @NonNull
    private final String onlineBookTitle;

    @NonNull
    private final String onlineBookSubtitle;

    @NonNull
    private final String onlineBookAuthors;

    @NonNull
    private final String onlinePublisherName;

    @NonNull
    private final String onlinePublicationDate;

    @NonNull
    private final String onlineEditionName;

    @NonNull
    private final String onlineSubjectName;

    @NonNull
    private final String onlineClassName;

    @NonNull
    private final String onlineEducationBoard;

    @NonNull
    private final String onlineStudyMedium;

    @NonNull
    private final String onlineIsbn;

    @NonNull
    private final String onlineProviderName;

    @NonNull
    private final String onlineAccessType;

    @NonNull
    private final String onlineLicenseType;

    @NonNull
    private final String onlineMatchStatus;

    @NonNull
    private final String onlineInformationUrl;

    @NonNull
    private final String onlinePreviewUrl;

    @NonNull
    private final String onlineOfficialSourceUrl;

    @NonNull
    private final String onlineAuthorizedDownloadUrl;

    @NonNull
    private final String onlineCoverImageUrl;

    private final float overallMatchScore;

    private final float titleMatchScore;

    private final float isbnMatchScore;

    private final float subjectMatchScore;

    private final float classMatchScore;

    private final float publisherMatchScore;

    private final float boardMatchScore;

    private final float authorMatchScore;

    private final boolean highConfidenceMatch;

    private final boolean automaticSelectionRecommended;

    private final boolean parentReviewRequired;

    private final boolean bookMetadataCanBeAdded;

    private final boolean previewAvailable;

    private final boolean authorizedDownloadAvailable;

    private final boolean officialSourceVerified;

    @NonNull
    private final ArrayList<String> matchReasons;

    @NonNull
    private final ArrayList<String> reviewWarnings;

    private final long searchedAt;

    private final long operationDurationMilliseconds;

    private BookMatchReviewData(
            @NonNull Builder builder
    ) {
        selectedImageUri =
                safeText(
                        builder.selectedImageUri
                );

        privateImagePath =
                safeText(
                        builder.privateImagePath
                );

        scanSourceName =
                safeText(
                        builder.scanSourceName
                );

        detectedBookTitle =
                safeText(
                        builder.detectedBookTitle
                );

        detectedSubjectName =
                safeText(
                        builder.detectedSubjectName
                );

        detectedClassName =
                safeText(
                        builder.detectedClassName
                );

        detectedEducationBoard =
                safeText(
                        builder.detectedEducationBoard
                );

        detectedPublisherName =
                safeText(
                        builder.detectedPublisherName
                );

        detectedAuthorName =
                safeText(
                        builder.detectedAuthorName
                );

        detectedEditionName =
                safeText(
                        builder.detectedEditionName
                );

        detectedPublicationYear =
                safeText(
                        builder.detectedPublicationYear
                );

        detectedStudyMedium =
                safeText(
                        builder.detectedStudyMedium
                );

        detectedIsbn =
                safeText(
                        builder.detectedIsbn
                );

        detectedBarcodeValue =
                safeText(
                        builder.detectedBarcodeValue
                );

        detectedOverallConfidence =
                normalizePercentage(
                        builder.detectedOverallConfidence
                );

        bestMatchAvailable =
                builder.bestMatchAvailable;

        onlineBookTitle =
                safeText(
                        builder.onlineBookTitle
                );

        onlineBookSubtitle =
                safeText(
                        builder.onlineBookSubtitle
                );

        onlineBookAuthors =
                safeText(
                        builder.onlineBookAuthors
                );

        onlinePublisherName =
                safeText(
                        builder.onlinePublisherName
                );

        onlinePublicationDate =
                safeText(
                        builder.onlinePublicationDate
                );

        onlineEditionName =
                safeText(
                        builder.onlineEditionName
                );

        onlineSubjectName =
                safeText(
                        builder.onlineSubjectName
                );

        onlineClassName =
                safeText(
                        builder.onlineClassName
                );

        onlineEducationBoard =
                safeText(
                        builder.onlineEducationBoard
                );

        onlineStudyMedium =
                safeText(
                        builder.onlineStudyMedium
                );

        onlineIsbn =
                safeText(
                        builder.onlineIsbn
                );

        onlineProviderName =
                safeText(
                        builder.onlineProviderName
                );

        onlineAccessType =
                safeText(
                        builder.onlineAccessType
                );

        onlineLicenseType =
                safeText(
                        builder.onlineLicenseType
                );

        onlineMatchStatus =
                safeText(
                        builder.onlineMatchStatus
                );

        onlineInformationUrl =
                safeText(
                        builder.onlineInformationUrl
                );

        onlinePreviewUrl =
                safeText(
                        builder.onlinePreviewUrl
                );

        onlineOfficialSourceUrl =
                safeText(
                        builder.onlineOfficialSourceUrl
                );

        onlineAuthorizedDownloadUrl =
                safeText(
                        builder.onlineAuthorizedDownloadUrl
                );

        onlineCoverImageUrl =
                safeText(
                        builder.onlineCoverImageUrl
                );

        overallMatchScore =
                normalizePercentage(
                        builder.overallMatchScore
                );

        titleMatchScore =
                normalizePercentage(
                        builder.titleMatchScore
                );

        isbnMatchScore =
                normalizePercentage(
                        builder.isbnMatchScore
                );

        subjectMatchScore =
                normalizePercentage(
                        builder.subjectMatchScore
                );

        classMatchScore =
                normalizePercentage(
                        builder.classMatchScore
                );

        publisherMatchScore =
                normalizePercentage(
                        builder.publisherMatchScore
                );

        boardMatchScore =
                normalizePercentage(
                        builder.boardMatchScore
                );

        authorMatchScore =
                normalizePercentage(
                        builder.authorMatchScore
                );

        highConfidenceMatch =
                builder.highConfidenceMatch;

        automaticSelectionRecommended =
                builder.automaticSelectionRecommended;

        parentReviewRequired =
                builder.parentReviewRequired;

        bookMetadataCanBeAdded =
                builder.bookMetadataCanBeAdded;

        previewAvailable =
                builder.previewAvailable
                        && !onlinePreviewUrl.isEmpty();

        authorizedDownloadAvailable =
                builder.authorizedDownloadAvailable
                        && !onlineAuthorizedDownloadUrl
                        .isEmpty();

        officialSourceVerified =
                builder.officialSourceVerified;

        matchReasons =
                createSafeTextList(
                        builder.matchReasons
                );

        reviewWarnings =
                createSafeTextList(
                        builder.reviewWarnings
                );

        searchedAt =
                builder.searchedAt > 0L
                        ? builder.searchedAt
                        : System.currentTimeMillis();

        operationDurationMilliseconds =
                Math.max(
                        0L,
                        builder.operationDurationMilliseconds
                );
    }

    @NonNull
    public static BookMatchReviewData
    fromDiscoveryResult(
            @NonNull BookScanDiscoveryCoordinator
                    .CompleteDiscoveryResult completeResult,
            @Nullable Uri selectedImageUri,
            @Nullable String privateImagePath
    ) {
        BookDiscoveryCoordinator.BookDiscoveryResult
                discoveryResult =
                completeResult.getDiscoveryResult();

        BookCoverMetadataExtractor.DetectedBookMetadata
                detectedMetadata =
                discoveryResult.getDetectedMetadata();

        Builder builder =
                new Builder();

        builder.selectedImageUri =
                selectedImageUri == null
                        ? ""
                        : selectedImageUri.toString();

        builder.privateImagePath =
                privateImagePath;

        builder.scanSourceName =
                completeResult
                        .getScanResult()
                        .getScanSource()
                        .name();

        builder.detectedBookTitle =
                detectedMetadata.getBookTitle();

        builder.detectedSubjectName =
                detectedMetadata.getSubjectName();

        builder.detectedClassName =
                detectedMetadata.getClassName();

        builder.detectedEducationBoard =
                detectedMetadata.getEducationBoard();

        builder.detectedPublisherName =
                detectedMetadata.getPublisherName();

        builder.detectedAuthorName =
                detectedMetadata.getAuthorName();

        builder.detectedEditionName =
                detectedMetadata.getEditionName();

        builder.detectedPublicationYear =
                detectedMetadata.getPublicationYear();

        builder.detectedStudyMedium =
                detectedMetadata.getStudyMedium();

        builder.detectedIsbn =
                detectedMetadata.getPreferredIsbn();

        builder.detectedBarcodeValue =
                detectedMetadata.getBarcodeValue();

        builder.detectedOverallConfidence =
                detectedMetadata.getOverallConfidence();

        builder.highConfidenceMatch =
                completeResult.hasHighConfidenceMatch();

        builder.parentReviewRequired =
                completeResult.requiresParentReview();

        builder.bookMetadataCanBeAdded =
                completeResult.canAddBookMetadata();

        builder.authorizedDownloadAvailable =
                completeResult.canDownloadAuthorizedBook();

        builder.reviewWarnings.addAll(
                discoveryResult.getWarnings()
        );

        builder.searchedAt =
                discoveryResult.getSearchedAt();

        builder.operationDurationMilliseconds =
                completeResult
                        .getTotalDurationMilliseconds();

        OnlineBookMatchEvaluator.RankedBookResult
                bestMatch =
                discoveryResult.getBestMatch();

        if (bestMatch == null) {
            builder.bestMatchAvailable =
                    false;

            return new BookMatchReviewData(
                    builder
            );
        }

        builder.bestMatchAvailable =
                true;

        OnlineBookSearchResult onlineBook =
                bestMatch.getBookResult();

        OnlineBookMatchEvaluator.MatchEvaluation
                evaluation =
                bestMatch.getEvaluation();

        builder.onlineBookTitle =
                onlineBook.getBookTitle();

        builder.onlineBookSubtitle =
                onlineBook.getBookSubtitle();

        builder.onlineBookAuthors =
                onlineBook.getAuthorsDisplayText();

        builder.onlinePublisherName =
                onlineBook.getPublisherName();

        builder.onlinePublicationDate =
                onlineBook.getPublicationDate();

        builder.onlineEditionName =
                onlineBook.getEditionName();

        builder.onlineSubjectName =
                onlineBook.getSubjectName();

        builder.onlineClassName =
                onlineBook.getClassName();

        builder.onlineEducationBoard =
                onlineBook.getEducationBoard();

        builder.onlineStudyMedium =
                onlineBook.getStudyMedium();

        builder.onlineIsbn =
                onlineBook.getPreferredIsbn();

        builder.onlineProviderName =
                onlineBook
                        .getProvider()
                        .getEnglishLabel();

        builder.onlineAccessType =
                onlineBook
                        .getAccessType()
                        .getEnglishLabel();

        builder.onlineLicenseType =
                onlineBook
                        .getLicenseType()
                        .getEnglishLabel();

        builder.onlineMatchStatus =
                evaluation
                        .getMatchStatus()
                        .getEnglishLabel();

        builder.onlineInformationUrl =
                onlineBook.getInformationUrl();

        builder.onlinePreviewUrl =
                onlineBook.getPreviewUrl();

        builder.onlineOfficialSourceUrl =
                onlineBook.getOfficialSourceUrl();

        builder.onlineAuthorizedDownloadUrl =
                onlineBook.getAuthorizedDownloadUrl();

        builder.onlineCoverImageUrl =
                onlineBook.getPreferredCoverImageUrl();

        builder.overallMatchScore =
                evaluation.getOverallMatchScore();

        builder.titleMatchScore =
                evaluation.getTitleMatchScore();

        builder.isbnMatchScore =
                evaluation.getIsbnMatchScore();

        builder.subjectMatchScore =
                evaluation.getSubjectMatchScore();

        builder.classMatchScore =
                evaluation.getClassMatchScore();

        builder.publisherMatchScore =
                evaluation.getPublisherMatchScore();

        builder.boardMatchScore =
                evaluation.getBoardMatchScore();

        builder.authorMatchScore =
                evaluation.getAuthorMatchScore();

        builder.highConfidenceMatch =
                evaluation.isHighConfidence();

        builder.automaticSelectionRecommended =
                evaluation
                        .isAutomaticSelectionRecommended();

        builder.parentReviewRequired =
                evaluation.requiresParentReview()
                        || !builder.reviewWarnings.isEmpty();

        builder.previewAvailable =
                onlineBook.hasPreview();

        builder.authorizedDownloadAvailable =
                onlineBook.hasAuthorizedDownload();

        builder.officialSourceVerified =
                onlineBook.isOfficialSourceVerified();

        builder.matchReasons.addAll(
                evaluation.getMatchReasons()
        );

        addUniqueTexts(
                builder.reviewWarnings,
                evaluation.getWarnings()
        );

        return new BookMatchReviewData(
                builder
        );
    }

    @NonNull
    public String getSelectedImageUri() {
        return selectedImageUri;
    }

    @NonNull
    public String getPrivateImagePath() {
        return privateImagePath;
    }

    @NonNull
    public String getScanSourceName() {
        return scanSourceName;
    }

    @NonNull
    public String getDetectedBookTitle() {
        return detectedBookTitle;
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
    public String getDetectedIsbn() {
        return detectedIsbn;
    }

    @NonNull
    public String getDetectedBarcodeValue() {
        return detectedBarcodeValue;
    }

    public float getDetectedOverallConfidence() {
        return detectedOverallConfidence;
    }

    public boolean isBestMatchAvailable() {
        return bestMatchAvailable;
    }

    @NonNull
    public String getOnlineBookTitle() {
        return onlineBookTitle;
    }

    @NonNull
    public String getOnlineBookSubtitle() {
        return onlineBookSubtitle;
    }

    @NonNull
    public String getOnlineBookAuthors() {
        return onlineBookAuthors;
    }

    @NonNull
    public String getOnlinePublisherName() {
        return onlinePublisherName;
    }

    @NonNull
    public String getOnlinePublicationDate() {
        return onlinePublicationDate;
    }

    @NonNull
    public String getOnlineEditionName() {
        return onlineEditionName;
    }

    @NonNull
    public String getOnlineSubjectName() {
        return onlineSubjectName;
    }

    @NonNull
    public String getOnlineClassName() {
        return onlineClassName;
    }

    @NonNull
    public String getOnlineEducationBoard() {
        return onlineEducationBoard;
    }

    @NonNull
    public String getOnlineStudyMedium() {
        return onlineStudyMedium;
    }

    @NonNull
    public String getOnlineIsbn() {
        return onlineIsbn;
    }

    @NonNull
    public String getOnlineProviderName() {
        return onlineProviderName;
    }

    @NonNull
    public String getOnlineAccessType() {
        return onlineAccessType;
    }

    @NonNull
    public String getOnlineLicenseType() {
        return onlineLicenseType;
    }

    @NonNull
    public String getOnlineMatchStatus() {
        return onlineMatchStatus;
    }

    @NonNull
    public String getOnlineInformationUrl() {
        return onlineInformationUrl;
    }

    @NonNull
    public String getOnlinePreviewUrl() {
        return onlinePreviewUrl;
    }

    @NonNull
    public String getOnlineOfficialSourceUrl() {
        return onlineOfficialSourceUrl;
    }

    @NonNull
    public String getOnlineAuthorizedDownloadUrl() {
        return onlineAuthorizedDownloadUrl;
    }

    @NonNull
    public String getOnlineCoverImageUrl() {
        return onlineCoverImageUrl;
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

    public boolean isHighConfidenceMatch() {
        return highConfidenceMatch;
    }

    public boolean isAutomaticSelectionRecommended() {
        return automaticSelectionRecommended;
    }

    public boolean isParentReviewRequired() {
        return parentReviewRequired;
    }

    public boolean canBookMetadataBeAdded() {
        return bookMetadataCanBeAdded;
    }

    public boolean isPreviewAvailable() {
        return previewAvailable;
    }

    public boolean isAuthorizedDownloadAvailable() {
        return authorizedDownloadAvailable;
    }

    public boolean isOfficialSourceVerified() {
        return officialSourceVerified;
    }

    @NonNull
    public ArrayList<String> getMatchReasons() {
        return new ArrayList<>(
                matchReasons
        );
    }

    @NonNull
    public ArrayList<String> getReviewWarnings() {
        return new ArrayList<>(
                reviewWarnings
        );
    }

    public long getSearchedAt() {
        return searchedAt;
    }

    public long getOperationDurationMilliseconds() {
        return operationDurationMilliseconds;
    }

    public boolean hasSelectedImageUri() {
        return !selectedImageUri.isEmpty();
    }

    public boolean hasDetectedBookTitle() {
        return !detectedBookTitle.isEmpty();
    }

    public boolean hasOnlineBookTitle() {
        return !onlineBookTitle.isEmpty();
    }

    public boolean hasOnlinePreviewUrl() {
        return previewAvailable
                && !onlinePreviewUrl.isEmpty();
    }

    public boolean hasOnlineInformationUrl() {
        return !onlineInformationUrl.isEmpty();
    }

    public boolean hasOfficialSourceUrl() {
        return !onlineOfficialSourceUrl.isEmpty();
    }

    public boolean hasAuthorizedDownloadUrl() {
        return authorizedDownloadAvailable
                && !onlineAuthorizedDownloadUrl.isEmpty();
    }

    public boolean hasWarnings() {
        return !reviewWarnings.isEmpty();
    }

    public boolean canConfirmBook() {
        return bestMatchAvailable
                && bookMetadataCanBeAdded
                && !onlineBookTitle.isEmpty();
    }

    @NonNull
    public String getPreferredBookTitle() {
        if (!onlineBookTitle.isEmpty()) {
            return onlineBookTitle;
        }

        if (!detectedBookTitle.isEmpty()) {
            return detectedBookTitle;
        }

        return "Unknown School Book";
    }

    @NonNull
    public String getPreferredSubjectName() {
        if (!onlineSubjectName.isEmpty()) {
            return onlineSubjectName;
        }

        return detectedSubjectName;
    }

    @NonNull
    public String getPreferredClassName() {
        if (!onlineClassName.isEmpty()) {
            return onlineClassName;
        }

        return detectedClassName;
    }

    @NonNull
    public String getPreferredEducationBoard() {
        if (!onlineEducationBoard.isEmpty()) {
            return onlineEducationBoard;
        }

        return detectedEducationBoard;
    }

    @NonNull
    public String getPreferredPublisherName() {
        if (!onlinePublisherName.isEmpty()) {
            return onlinePublisherName;
        }

        return detectedPublisherName;
    }

    @NonNull
    public String getPreferredIsbn() {
        if (!onlineIsbn.isEmpty()) {
            return onlineIsbn;
        }

        return detectedIsbn;
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private static float normalizePercentage(
            float value
    ) {
        if (Float.isNaN(value)
                || Float.isInfinite(value)) {

            return 0f;
        }

        return Math.max(
                0f,
                Math.min(
                        100f,
                        value
                )
        );
    }

    @NonNull
    private static ArrayList<String>
    createSafeTextList(
            @Nullable List<String> source
    ) {
        ArrayList<String> safeList =
                new ArrayList<>();

        addUniqueTexts(
                safeList,
                source
        );

        return safeList;
    }

    private static void addUniqueTexts(
            @NonNull ArrayList<String> target,
            @Nullable List<String> source
    ) {
        if (source == null) {
            return;
        }

        for (String sourceValue : source) {
            String safeValue =
                    safeText(
                            sourceValue
                    );

            if (safeValue.isEmpty()) {
                continue;
            }

            boolean alreadyAdded =
                    false;

            for (String existingValue : target) {
                if (existingValue.equalsIgnoreCase(
                        safeValue
                )) {
                    alreadyAdded =
                            true;

                    break;
                }
            }

            if (!alreadyAdded) {
                target.add(
                        safeValue
                );
            }
        }
    }

    private static final class Builder {

        @Nullable
        private String selectedImageUri;

        @Nullable
        private String privateImagePath;

        @Nullable
        private String scanSourceName;

        @Nullable
        private String detectedBookTitle;

        @Nullable
        private String detectedSubjectName;

        @Nullable
        private String detectedClassName;

        @Nullable
        private String detectedEducationBoard;

        @Nullable
        private String detectedPublisherName;

        @Nullable
        private String detectedAuthorName;

        @Nullable
        private String detectedEditionName;

        @Nullable
        private String detectedPublicationYear;

        @Nullable
        private String detectedStudyMedium;

        @Nullable
        private String detectedIsbn;

        @Nullable
        private String detectedBarcodeValue;

        private float detectedOverallConfidence;

        private boolean bestMatchAvailable;

        @Nullable
        private String onlineBookTitle;

        @Nullable
        private String onlineBookSubtitle;

        @Nullable
        private String onlineBookAuthors;

        @Nullable
        private String onlinePublisherName;

        @Nullable
        private String onlinePublicationDate;

        @Nullable
        private String onlineEditionName;

        @Nullable
        private String onlineSubjectName;

        @Nullable
        private String onlineClassName;

        @Nullable
        private String onlineEducationBoard;

        @Nullable
        private String onlineStudyMedium;

        @Nullable
        private String onlineIsbn;

        @Nullable
        private String onlineProviderName;

        @Nullable
        private String onlineAccessType;

        @Nullable
        private String onlineLicenseType;

        @Nullable
        private String onlineMatchStatus;

        @Nullable
        private String onlineInformationUrl;

        @Nullable
        private String onlinePreviewUrl;

        @Nullable
        private String onlineOfficialSourceUrl;

        @Nullable
        private String onlineAuthorizedDownloadUrl;

        @Nullable
        private String onlineCoverImageUrl;

        private float overallMatchScore;

        private float titleMatchScore;

        private float isbnMatchScore;

        private float subjectMatchScore;

        private float classMatchScore;

        private float publisherMatchScore;

        private float boardMatchScore;

        private float authorMatchScore;

        private boolean highConfidenceMatch;

        private boolean automaticSelectionRecommended;

        private boolean parentReviewRequired;

        private boolean bookMetadataCanBeAdded;

        private boolean previewAvailable;

        private boolean authorizedDownloadAvailable;

        private boolean officialSourceVerified;

        @NonNull
        private final ArrayList<String> matchReasons =
                new ArrayList<>();

        @NonNull
        private final ArrayList<String> reviewWarnings =
                new ArrayList<>();

        private long searchedAt;

        private long operationDurationMilliseconds;
    }
}