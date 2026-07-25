package com.tridev.studysaathi.data.content.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class OnlineBookSearchResult {

    private static final float MINIMUM_MATCH_SCORE =
            0f;

    private static final float MAXIMUM_MATCH_SCORE =
            100f;

    @NonNull
    private final String resultId;

    @NonNull
    private final BookProvider provider;

    @NonNull
    private final String providerBookId;

    @NonNull
    private final String bookTitle;

    @NonNull
    private final String bookSubtitle;

    @NonNull
    private final List<String> authors;

    @NonNull
    private final String publisherName;

    @NonNull
    private final String publicationDate;

    @NonNull
    private final String publicationYear;

    @NonNull
    private final String editionName;

    @NonNull
    private final String description;

    @NonNull
    private final String subjectName;

    @NonNull
    private final String className;

    @NonNull
    private final String educationBoard;

    @NonNull
    private final String studyMedium;

    @NonNull
    private final String isbn10;

    @NonNull
    private final String isbn13;

    @NonNull
    private final String industryIdentifier;

    @NonNull
    private final List<String> categories;

    @NonNull
    private final String languageCode;

    private final int pageCount;

    @NonNull
    private final String smallCoverImageUrl;

    @NonNull
    private final String largeCoverImageUrl;

    @NonNull
    private final String informationUrl;

    @NonNull
    private final String previewUrl;

    @NonNull
    private final String officialSourceUrl;

    @NonNull
    private final String authorizedDownloadUrl;

    @NonNull
    private final String downloadMimeType;

    @NonNull
    private final AccessType accessType;

    @NonNull
    private final LicenseType licenseType;

    private final boolean previewAllowed;

    private final boolean downloadAllowed;

    private final boolean officialSource;

    private final boolean officialSourceVerified;

    private final boolean publicDomain;

    private final boolean openEducationalResource;

    private final float overallMatchScore;

    private final float titleMatchScore;

    private final float isbnMatchScore;

    private final float subjectMatchScore;

    private final float classMatchScore;

    private final float publisherMatchScore;

    private final float boardMatchScore;

    @NonNull
    private final MatchStatus matchStatus;

    @NonNull
    private final List<String> matchReasons;

    @NonNull
    private final List<String> warnings;

    private final long searchedAt;

    private final boolean parentConfirmed;

    private OnlineBookSearchResult(
            @NonNull Builder builder
    ) {
        resultId =
                requireIdentifier(
                        builder.resultId,
                        "Result ID"
                );

        provider =
                builder.provider;

        providerBookId =
                normalizeOptionalText(
                        builder.providerBookId
                );

        bookTitle =
                requireText(
                        builder.bookTitle,
                        "Book title"
                );

        bookSubtitle =
                normalizeOptionalText(
                        builder.bookSubtitle
                );

        authors =
                createImmutableTextList(
                        builder.authors
                );

        publisherName =
                normalizeOptionalText(
                        builder.publisherName
                );

        publicationDate =
                normalizeOptionalText(
                        builder.publicationDate
                );

        publicationYear =
                normalizeOptionalText(
                        builder.publicationYear
                );

        editionName =
                normalizeOptionalText(
                        builder.editionName
                );

        description =
                normalizeOptionalText(
                        builder.description
                );

        subjectName =
                normalizeOptionalText(
                        builder.subjectName
                );

        className =
                normalizeOptionalText(
                        builder.className
                );

        educationBoard =
                normalizeOptionalText(
                        builder.educationBoard
                );

        studyMedium =
                normalizeOptionalText(
                        builder.studyMedium
                );

        isbn10 =
                normalizeIsbn(
                        builder.isbn10,
                        10
                );

        isbn13 =
                normalizeIsbn(
                        builder.isbn13,
                        13
                );

        industryIdentifier =
                normalizeOptionalText(
                        builder.industryIdentifier
                );

        categories =
                createImmutableTextList(
                        builder.categories
                );

        languageCode =
                normalizeLanguageCode(
                        builder.languageCode
                );

        pageCount =
                Math.max(
                        0,
                        builder.pageCount
                );

        smallCoverImageUrl =
                normalizeOptionalText(
                        builder.smallCoverImageUrl
                );

        largeCoverImageUrl =
                normalizeOptionalText(
                        builder.largeCoverImageUrl
                );

        informationUrl =
                normalizeOptionalText(
                        builder.informationUrl
                );

        previewUrl =
                normalizeOptionalText(
                        builder.previewUrl
                );

        officialSourceUrl =
                normalizeOptionalText(
                        builder.officialSourceUrl
                );

        authorizedDownloadUrl =
                normalizeOptionalText(
                        builder.authorizedDownloadUrl
                );

        downloadMimeType =
                normalizeOptionalText(
                        builder.downloadMimeType
                );

        accessType =
                builder.accessType;

        licenseType =
                builder.licenseType;

        previewAllowed =
                builder.previewAllowed;

        downloadAllowed =
                builder.downloadAllowed;

        officialSource =
                builder.officialSource;

        officialSourceVerified =
                builder.officialSourceVerified;

        publicDomain =
                builder.publicDomain;

        openEducationalResource =
                builder.openEducationalResource;

        overallMatchScore =
                normalizeMatchScore(
                        builder.overallMatchScore
                );

        titleMatchScore =
                normalizeMatchScore(
                        builder.titleMatchScore
                );

        isbnMatchScore =
                normalizeMatchScore(
                        builder.isbnMatchScore
                );

        subjectMatchScore =
                normalizeMatchScore(
                        builder.subjectMatchScore
                );

        classMatchScore =
                normalizeMatchScore(
                        builder.classMatchScore
                );

        publisherMatchScore =
                normalizeMatchScore(
                        builder.publisherMatchScore
                );

        boardMatchScore =
                normalizeMatchScore(
                        builder.boardMatchScore
                );

        matchStatus =
                builder.matchStatus;

        matchReasons =
                createImmutableTextList(
                        builder.matchReasons
                );

        warnings =
                createImmutableTextList(
                        builder.warnings
                );

        searchedAt =
                builder.searchedAt > 0L
                        ? builder.searchedAt
                        : System.currentTimeMillis();

        parentConfirmed =
                builder.parentConfirmed;
    }

    @NonNull
    public static Builder builder(
            @NonNull String resultId,
            @NonNull BookProvider provider,
            @NonNull String bookTitle
    ) {
        return new Builder(
                resultId,
                provider,
                bookTitle
        );
    }

    @NonNull
    public String getResultId() {
        return resultId;
    }

    @NonNull
    public BookProvider getProvider() {
        return provider;
    }

    @NonNull
    public String getProviderBookId() {
        return providerBookId;
    }

    @NonNull
    public String getBookTitle() {
        return bookTitle;
    }

    @NonNull
    public String getBookSubtitle() {
        return bookSubtitle;
    }

    @NonNull
    public List<String> getAuthors() {
        return authors;
    }

    @NonNull
    public String getPublisherName() {
        return publisherName;
    }

    @NonNull
    public String getPublicationDate() {
        return publicationDate;
    }

    @NonNull
    public String getPublicationYear() {
        return publicationYear;
    }

    @NonNull
    public String getEditionName() {
        return editionName;
    }

    @NonNull
    public String getDescription() {
        return description;
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
        if (!isbn13.isEmpty()) {
            return isbn13;
        }

        return isbn10;
    }

    @NonNull
    public String getIndustryIdentifier() {
        return industryIdentifier;
    }

    @NonNull
    public List<String> getCategories() {
        return categories;
    }

    @NonNull
    public String getLanguageCode() {
        return languageCode;
    }

    public int getPageCount() {
        return pageCount;
    }

    @NonNull
    public String getSmallCoverImageUrl() {
        return smallCoverImageUrl;
    }

    @NonNull
    public String getLargeCoverImageUrl() {
        return largeCoverImageUrl;
    }

    @NonNull
    public String getPreferredCoverImageUrl() {
        if (!largeCoverImageUrl.isEmpty()) {
            return largeCoverImageUrl;
        }

        return smallCoverImageUrl;
    }

    @NonNull
    public String getInformationUrl() {
        return informationUrl;
    }

    @NonNull
    public String getPreviewUrl() {
        return previewUrl;
    }

    @NonNull
    public String getOfficialSourceUrl() {
        return officialSourceUrl;
    }

    @NonNull
    public String getAuthorizedDownloadUrl() {
        return authorizedDownloadUrl;
    }

    @NonNull
    public String getDownloadMimeType() {
        return downloadMimeType;
    }

    @NonNull
    public AccessType getAccessType() {
        return accessType;
    }

    @NonNull
    public LicenseType getLicenseType() {
        return licenseType;
    }

    public boolean isPreviewAllowed() {
        return previewAllowed;
    }

    public boolean isDownloadAllowed() {
        return downloadAllowed;
    }

    public boolean isOfficialSource() {
        return officialSource;
    }

    public boolean isOfficialSourceVerified() {
        return officialSourceVerified;
    }

    public boolean isPublicDomain() {
        return publicDomain;
    }

    public boolean isOpenEducationalResource() {
        return openEducationalResource;
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

    @NonNull
    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    @NonNull
    public List<String> getMatchReasons() {
        return matchReasons;
    }

    @NonNull
    public List<String> getWarnings() {
        return warnings;
    }

    public long getSearchedAt() {
        return searchedAt;
    }

    public boolean isParentConfirmed() {
        return parentConfirmed;
    }

    public boolean hasIsbn() {
        return !isbn10.isEmpty()
                || !isbn13.isEmpty();
    }

    public boolean hasAuthorInformation() {
        return !authors.isEmpty();
    }

    public boolean hasCoverImage() {
        return !smallCoverImageUrl.isEmpty()
                || !largeCoverImageUrl.isEmpty();
    }

    public boolean hasInformationPage() {
        return !informationUrl.isEmpty()
                || !officialSourceUrl.isEmpty();
    }

    public boolean hasPreview() {
        return previewAllowed
                && !previewUrl.isEmpty();
    }

    public boolean hasAuthorizedDownload() {
        return downloadAllowed
                && officialSourceVerified
                && !authorizedDownloadUrl.isEmpty();
    }

    public boolean canBeAddedAsMetadataOnly() {
        return !bookTitle.isEmpty()
                && matchStatus != MatchStatus.REJECTED;
    }

    public boolean isHighConfidenceMatch() {
        return matchStatus == MatchStatus.HIGH_CONFIDENCE
                || overallMatchScore >= 85f;
    }

    public boolean requiresParentConfirmation() {
        return !parentConfirmed
                && (matchStatus
                == MatchStatus.MANUAL_REVIEW_REQUIRED
                || overallMatchScore < 85f
                || !warnings.isEmpty());
    }

    public boolean isSafeForAutomaticDownload() {
        return parentConfirmed
                && officialSourceVerified
                && downloadAllowed
                && !authorizedDownloadUrl.isEmpty()
                && (licenseType == LicenseType.PUBLIC_DOMAIN
                || licenseType == LicenseType.OPEN_LICENSE
                || licenseType == LicenseType.OFFICIAL_FREE_ACCESS
                || licenseType == LicenseType.PURCHASED_OR_OWNED);
    }

    @NonNull
    public String getAuthorsDisplayText() {
        if (authors.isEmpty()) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (String author : authors) {
            if (builder.length() > 0) {
                builder.append(
                        ", "
                );
            }

            builder.append(
                    author
            );
        }

        return builder.toString();
    }

    @NonNull
    public String createDisplaySummary() {
        StringBuilder summaryBuilder =
                new StringBuilder();

        appendSummaryLine(
                summaryBuilder,
                "Book",
                bookTitle
        );

        appendSummaryLine(
                summaryBuilder,
                "Author",
                getAuthorsDisplayText()
        );

        appendSummaryLine(
                summaryBuilder,
                "Publisher",
                publisherName
        );

        appendSummaryLine(
                summaryBuilder,
                "Subject",
                subjectName
        );

        appendSummaryLine(
                summaryBuilder,
                "Class",
                className
        );

        appendSummaryLine(
                summaryBuilder,
                "Board",
                educationBoard
        );

        appendSummaryLine(
                summaryBuilder,
                "ISBN",
                getPreferredIsbn()
        );

        appendSummaryLine(
                summaryBuilder,
                "Source",
                provider.getEnglishLabel()
        );

        appendSummaryLine(
                summaryBuilder,
                "Match",
                String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        overallMatchScore
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
        String safeValue =
                value.trim();

        if (safeValue.isEmpty()) {
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
                safeValue
        );
    }

    public enum BookProvider {

        GOOGLE_BOOKS(
                "Google Books",
                "गूगल बुक्स"
        ),

        OPEN_LIBRARY(
                "Open Library",
                "ओपन लाइब्रेरी"
        ),

        NCERT(
                "NCERT",
                "एनसीईआरटी"
        ),

        OFFICIAL_PUBLISHER(
                "Official Publisher",
                "आधिकारिक प्रकाशक"
        ),

        SCHOOL_SOURCE(
                "School Source",
                "स्कूल स्रोत"
        ),

        PARENT_IMPORTED(
                "Parent Imported",
                "अभिभावक द्वारा इम्पोर्ट"
        ),

        MANUAL_ENTRY(
                "Manual Entry",
                "मैनुअल एंट्री"
        ),

        CUSTOM(
                "Custom",
                "कस्टम"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        BookProvider(
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

    public enum AccessType {

        FULL_DOWNLOAD(
                "Full Download",
                "पूरा डाउनलोड"
        ),

        FULL_ONLINE(
                "Full Online Access",
                "पूरा ऑनलाइन एक्सेस"
        ),

        PARTIAL_PREVIEW(
                "Partial Preview",
                "आंशिक प्रीव्यू"
        ),

        METADATA_ONLY(
                "Metadata Only",
                "केवल पुस्तक जानकारी"
        ),

        USER_IMPORTED(
                "User Imported",
                "यूजर द्वारा इम्पोर्ट"
        ),

        NOT_AVAILABLE(
                "Not Available",
                "उपलब्ध नहीं"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        AccessType(
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

    public enum LicenseType {

        PUBLIC_DOMAIN(
                "Public Domain",
                "पब्लिक डोमेन"
        ),

        OPEN_LICENSE(
                "Open License",
                "ओपन लाइसेंस"
        ),

        OFFICIAL_FREE_ACCESS(
                "Official Free Access",
                "आधिकारिक निःशुल्क एक्सेस"
        ),

        PURCHASED_OR_OWNED(
                "Purchased or Owned",
                "खरीदी या स्वामित्व वाली प्रति"
        ),

        PRIVATE_COPYRIGHT(
                "Private Copyright",
                "निजी कॉपीराइट"
        ),

        RESTRICTED(
                "Restricted",
                "प्रतिबंधित"
        ),

        UNKNOWN(
                "Unknown",
                "अज्ञात"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        LicenseType(
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

    public enum MatchStatus {

        HIGH_CONFIDENCE(
                "High Confidence",
                "उच्च विश्वसनीयता"
        ),

        POSSIBLE_MATCH(
                "Possible Match",
                "संभावित मिलान"
        ),

        MANUAL_REVIEW_REQUIRED(
                "Manual Review Required",
                "मैनुअल जाँच आवश्यक"
        ),

        NO_CONFIDENT_MATCH(
                "No Confident Match",
                "विश्वसनीय मिलान नहीं"
        ),

        REJECTED(
                "Rejected",
                "अस्वीकृत"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        MatchStatus(
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

        @NonNull
        public static MatchStatus fromScore(
                float matchScore
        ) {
            float safeScore =
                    normalizeMatchScore(
                            matchScore
                    );

            if (safeScore >= 85f) {
                return HIGH_CONFIDENCE;
            }

            if (safeScore >= 65f) {
                return POSSIBLE_MATCH;
            }

            if (safeScore >= 40f) {
                return MANUAL_REVIEW_REQUIRED;
            }

            return NO_CONFIDENT_MATCH;
        }
    }

    public static final class Builder {

        @NonNull
        private final String resultId;

        @NonNull
        private final BookProvider provider;

        @NonNull
        private final String bookTitle;

        @NonNull
        private String providerBookId = "";

        @NonNull
        private String bookSubtitle = "";

        @NonNull
        private final List<String> authors =
                new ArrayList<>();

        @NonNull
        private String publisherName = "";

        @NonNull
        private String publicationDate = "";

        @NonNull
        private String publicationYear = "";

        @NonNull
        private String editionName = "";

        @NonNull
        private String description = "";

        @NonNull
        private String subjectName = "";

        @NonNull
        private String className = "";

        @NonNull
        private String educationBoard = "";

        @NonNull
        private String studyMedium = "";

        @NonNull
        private String isbn10 = "";

        @NonNull
        private String isbn13 = "";

        @NonNull
        private String industryIdentifier = "";

        @NonNull
        private final List<String> categories =
                new ArrayList<>();

        @NonNull
        private String languageCode = "";

        private int pageCount;

        @NonNull
        private String smallCoverImageUrl = "";

        @NonNull
        private String largeCoverImageUrl = "";

        @NonNull
        private String informationUrl = "";

        @NonNull
        private String previewUrl = "";

        @NonNull
        private String officialSourceUrl = "";

        @NonNull
        private String authorizedDownloadUrl = "";

        @NonNull
        private String downloadMimeType = "";

        @NonNull
        private AccessType accessType =
                AccessType.METADATA_ONLY;

        @NonNull
        private LicenseType licenseType =
                LicenseType.UNKNOWN;

        private boolean previewAllowed;

        private boolean downloadAllowed;

        private boolean officialSource;

        private boolean officialSourceVerified;

        private boolean publicDomain;

        private boolean openEducationalResource;

        private float overallMatchScore;

        private float titleMatchScore;

        private float isbnMatchScore;

        private float subjectMatchScore;

        private float classMatchScore;

        private float publisherMatchScore;

        private float boardMatchScore;

        @NonNull
        private MatchStatus matchStatus =
                MatchStatus.MANUAL_REVIEW_REQUIRED;

        @NonNull
        private final List<String> matchReasons =
                new ArrayList<>();

        @NonNull
        private final List<String> warnings =
                new ArrayList<>();

        private long searchedAt;

        private boolean parentConfirmed;

        private Builder(
                @NonNull String resultId,
                @NonNull BookProvider provider,
                @NonNull String bookTitle
        ) {
            this.resultId =
                    resultId;

            this.provider =
                    provider;

            this.bookTitle =
                    bookTitle;
        }

        @NonNull
        public Builder setProviderBookId(
                @Nullable String providerBookId
        ) {
            this.providerBookId =
                    normalizeOptionalText(
                            providerBookId
                    );

            return this;
        }

        @NonNull
        public Builder setBookSubtitle(
                @Nullable String bookSubtitle
        ) {
            this.bookSubtitle =
                    normalizeOptionalText(
                            bookSubtitle
                    );

            return this;
        }

        @NonNull
        public Builder addAuthor(
                @Nullable String author
        ) {
            String safeAuthor =
                    normalizeOptionalText(
                            author
                    );

            if (!safeAuthor.isEmpty()) {
                authors.add(
                        safeAuthor
                );
            }

            return this;
        }

        @NonNull
        public Builder addAuthors(
                @NonNull List<String> authors
        ) {
            for (String author : authors) {
                addAuthor(
                        author
                );
            }

            return this;
        }

        @NonNull
        public Builder setPublisherName(
                @Nullable String publisherName
        ) {
            this.publisherName =
                    normalizeOptionalText(
                            publisherName
                    );

            return this;
        }

        @NonNull
        public Builder setPublicationDate(
                @Nullable String publicationDate
        ) {
            this.publicationDate =
                    normalizeOptionalText(
                            publicationDate
                    );

            return this;
        }

        @NonNull
        public Builder setPublicationYear(
                @Nullable String publicationYear
        ) {
            this.publicationYear =
                    normalizeOptionalText(
                            publicationYear
                    );

            return this;
        }

        @NonNull
        public Builder setEditionName(
                @Nullable String editionName
        ) {
            this.editionName =
                    normalizeOptionalText(
                            editionName
                    );

            return this;
        }

        @NonNull
        public Builder setDescription(
                @Nullable String description
        ) {
            this.description =
                    normalizeOptionalText(
                            description
                    );

            return this;
        }

        @NonNull
        public Builder setSubjectName(
                @Nullable String subjectName
        ) {
            this.subjectName =
                    normalizeOptionalText(
                            subjectName
                    );

            return this;
        }

        @NonNull
        public Builder setClassName(
                @Nullable String className
        ) {
            this.className =
                    normalizeOptionalText(
                            className
                    );

            return this;
        }

        @NonNull
        public Builder setEducationBoard(
                @Nullable String educationBoard
        ) {
            this.educationBoard =
                    normalizeOptionalText(
                            educationBoard
                    );

            return this;
        }

        @NonNull
        public Builder setStudyMedium(
                @Nullable String studyMedium
        ) {
            this.studyMedium =
                    normalizeOptionalText(
                            studyMedium
                    );

            return this;
        }

        @NonNull
        public Builder setIsbn10(
                @Nullable String isbn10
        ) {
            this.isbn10 =
                    normalizeOptionalText(
                            isbn10
                    );

            return this;
        }

        @NonNull
        public Builder setIsbn13(
                @Nullable String isbn13
        ) {
            this.isbn13 =
                    normalizeOptionalText(
                            isbn13
                    );

            return this;
        }

        @NonNull
        public Builder setIndustryIdentifier(
                @Nullable String industryIdentifier
        ) {
            this.industryIdentifier =
                    normalizeOptionalText(
                            industryIdentifier
                    );

            return this;
        }

        @NonNull
        public Builder addCategory(
                @Nullable String category
        ) {
            String safeCategory =
                    normalizeOptionalText(
                            category
                    );

            if (!safeCategory.isEmpty()) {
                categories.add(
                        safeCategory
                );
            }

            return this;
        }

        @NonNull
        public Builder setLanguageCode(
                @Nullable String languageCode
        ) {
            this.languageCode =
                    normalizeOptionalText(
                            languageCode
                    );

            return this;
        }

        @NonNull
        public Builder setPageCount(
                int pageCount
        ) {
            this.pageCount =
                    pageCount;

            return this;
        }

        @NonNull
        public Builder setSmallCoverImageUrl(
                @Nullable String smallCoverImageUrl
        ) {
            this.smallCoverImageUrl =
                    normalizeOptionalText(
                            smallCoverImageUrl
                    );

            return this;
        }

        @NonNull
        public Builder setLargeCoverImageUrl(
                @Nullable String largeCoverImageUrl
        ) {
            this.largeCoverImageUrl =
                    normalizeOptionalText(
                            largeCoverImageUrl
                    );

            return this;
        }

        @NonNull
        public Builder setInformationUrl(
                @Nullable String informationUrl
        ) {
            this.informationUrl =
                    normalizeOptionalText(
                            informationUrl
                    );

            return this;
        }

        @NonNull
        public Builder setPreviewUrl(
                @Nullable String previewUrl
        ) {
            this.previewUrl =
                    normalizeOptionalText(
                            previewUrl
                    );

            return this;
        }

        @NonNull
        public Builder setOfficialSourceUrl(
                @Nullable String officialSourceUrl
        ) {
            this.officialSourceUrl =
                    normalizeOptionalText(
                            officialSourceUrl
                    );

            return this;
        }

        @NonNull
        public Builder setAuthorizedDownloadUrl(
                @Nullable String authorizedDownloadUrl
        ) {
            this.authorizedDownloadUrl =
                    normalizeOptionalText(
                            authorizedDownloadUrl
                    );

            return this;
        }

        @NonNull
        public Builder setDownloadMimeType(
                @Nullable String downloadMimeType
        ) {
            this.downloadMimeType =
                    normalizeOptionalText(
                            downloadMimeType
                    );

            return this;
        }

        @NonNull
        public Builder setAccessType(
                @NonNull AccessType accessType
        ) {
            this.accessType =
                    accessType;

            return this;
        }

        @NonNull
        public Builder setLicenseType(
                @NonNull LicenseType licenseType
        ) {
            this.licenseType =
                    licenseType;

            return this;
        }

        @NonNull
        public Builder setPreviewAllowed(
                boolean previewAllowed
        ) {
            this.previewAllowed =
                    previewAllowed;

            return this;
        }

        @NonNull
        public Builder setDownloadAllowed(
                boolean downloadAllowed
        ) {
            this.downloadAllowed =
                    downloadAllowed;

            return this;
        }

        @NonNull
        public Builder setOfficialSource(
                boolean officialSource
        ) {
            this.officialSource =
                    officialSource;

            return this;
        }

        @NonNull
        public Builder setOfficialSourceVerified(
                boolean officialSourceVerified
        ) {
            this.officialSourceVerified =
                    officialSourceVerified;

            return this;
        }

        @NonNull
        public Builder setPublicDomain(
                boolean publicDomain
        ) {
            this.publicDomain =
                    publicDomain;

            return this;
        }

        @NonNull
        public Builder setOpenEducationalResource(
                boolean openEducationalResource
        ) {
            this.openEducationalResource =
                    openEducationalResource;

            return this;
        }

        @NonNull
        public Builder setOverallMatchScore(
                float overallMatchScore
        ) {
            this.overallMatchScore =
                    overallMatchScore;

            return this;
        }

        @NonNull
        public Builder setTitleMatchScore(
                float titleMatchScore
        ) {
            this.titleMatchScore =
                    titleMatchScore;

            return this;
        }

        @NonNull
        public Builder setIsbnMatchScore(
                float isbnMatchScore
        ) {
            this.isbnMatchScore =
                    isbnMatchScore;

            return this;
        }

        @NonNull
        public Builder setSubjectMatchScore(
                float subjectMatchScore
        ) {
            this.subjectMatchScore =
                    subjectMatchScore;

            return this;
        }

        @NonNull
        public Builder setClassMatchScore(
                float classMatchScore
        ) {
            this.classMatchScore =
                    classMatchScore;

            return this;
        }

        @NonNull
        public Builder setPublisherMatchScore(
                float publisherMatchScore
        ) {
            this.publisherMatchScore =
                    publisherMatchScore;

            return this;
        }

        @NonNull
        public Builder setBoardMatchScore(
                float boardMatchScore
        ) {
            this.boardMatchScore =
                    boardMatchScore;

            return this;
        }

        @NonNull
        public Builder setMatchStatus(
                @NonNull MatchStatus matchStatus
        ) {
            this.matchStatus =
                    matchStatus;

            return this;
        }

        @NonNull
        public Builder applyMatchStatusFromScore() {
            matchStatus =
                    MatchStatus.fromScore(
                            overallMatchScore
                    );

            return this;
        }

        @NonNull
        public Builder addMatchReason(
                @Nullable String matchReason
        ) {
            String safeReason =
                    normalizeOptionalText(
                            matchReason
                    );

            if (!safeReason.isEmpty()) {
                matchReasons.add(
                        safeReason
                );
            }

            return this;
        }

        @NonNull
        public Builder addWarning(
                @Nullable String warning
        ) {
            String safeWarning =
                    normalizeOptionalText(
                            warning
                    );

            if (!safeWarning.isEmpty()) {
                warnings.add(
                        safeWarning
                );
            }

            return this;
        }

        @NonNull
        public Builder setSearchedAt(
                long searchedAt
        ) {
            this.searchedAt =
                    searchedAt;

            return this;
        }

        @NonNull
        public Builder setParentConfirmed(
                boolean parentConfirmed
        ) {
            this.parentConfirmed =
                    parentConfirmed;

            return this;
        }

        @NonNull
        public OnlineBookSearchResult build() {
            return new OnlineBookSearchResult(
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

    @NonNull
    private static String normalizeLanguageCode(
            @Nullable String value
    ) {
        String normalizedLanguage =
                normalizeOptionalText(
                        value
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (normalizedLanguage.length() > 10) {
            return normalizedLanguage.substring(
                    0,
                    10
            );
        }

        return normalizedLanguage;
    }

    private static float normalizeMatchScore(
            float matchScore
    ) {
        return Math.max(
                MINIMUM_MATCH_SCORE,
                Math.min(
                        MAXIMUM_MATCH_SCORE,
                        matchScore
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
            String safeItem =
                    normalizeOptionalText(
                            item
                    );

            if (!safeItem.isEmpty()
                    && !preparedList.contains(
                    safeItem
            )) {
                preparedList.add(
                        safeItem
                );
            }
        }

        return Collections.unmodifiableList(
                preparedList
        );
    }
}