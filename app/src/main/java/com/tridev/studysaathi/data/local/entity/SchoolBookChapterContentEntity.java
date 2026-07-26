package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Locale;
import java.util.UUID;

@Entity(
        tableName = "school_book_chapter_contents",
        foreignKeys = {
                @ForeignKey(
                        entity = SchoolBookChapterEntity.class,
                        parentColumns = "chapter_row_id",
                        childColumns = "chapter_row_id",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(
                        value = {
                                "chapter_row_id"
                        },
                        unique = true
                ),
                @Index(
                        value = {
                                "review_status",
                                "parent_approved"
                        }
                ),
                @Index(
                        value = {
                                "content_source"
                        }
                )
        }
)
public class SchoolBookChapterContentEntity {

    public static final String LANGUAGE_MODE_ENGLISH =
            "ENGLISH";

    public static final String LANGUAGE_MODE_HINDI =
            "HINDI";

    public static final String LANGUAGE_MODE_BILINGUAL =
            "BILINGUAL";

    public static final String CONTENT_SOURCE_PARENT_MANUAL =
            "PARENT_MANUAL";

    public static final String CONTENT_SOURCE_BOOK_PAGE_OCR =
            "BOOK_PAGE_OCR";

    public static final String CONTENT_SOURCE_AI_GENERATED =
            "AI_GENERATED";

    public static final String CONTENT_SOURCE_AI_AND_OCR =
            "AI_AND_OCR";

    public static final String REVIEW_STATUS_DRAFT =
            "DRAFT";

    public static final String REVIEW_STATUS_PROCESSING =
            "PROCESSING";

    public static final String REVIEW_STATUS_PENDING_REVIEW =
            "PENDING_REVIEW";

    public static final String REVIEW_STATUS_APPROVED =
            "APPROVED";

    public static final String REVIEW_STATUS_REJECTED =
            "REJECTED";

    public static final String REVIEW_STATUS_FAILED =
            "FAILED";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "content_row_id")
    private long contentRowId;

    @ColumnInfo(name = "chapter_row_id")
    private long chapterRowId;

    @NonNull
    @ColumnInfo(name = "content_id")
    private String contentId = "";

    @NonNull
    @ColumnInfo(name = "language_mode")
    private String languageMode =
            LANGUAGE_MODE_BILINGUAL;

    @NonNull
    @ColumnInfo(name = "chapter_introduction_english")
    private String chapterIntroductionEnglish = "";

    @NonNull
    @ColumnInfo(name = "chapter_introduction_hindi")
    private String chapterIntroductionHindi = "";

    @NonNull
    @ColumnInfo(name = "detailed_explanation_english")
    private String detailedExplanationEnglish = "";

    @NonNull
    @ColumnInfo(name = "detailed_explanation_hindi")
    private String detailedExplanationHindi = "";

    @NonNull
    @ColumnInfo(name = "key_points_english")
    private String keyPointsEnglish = "";

    @NonNull
    @ColumnInfo(name = "key_points_hindi")
    private String keyPointsHindi = "";

    @NonNull
    @ColumnInfo(name = "important_terms_english")
    private String importantTermsEnglish = "";

    @NonNull
    @ColumnInfo(name = "important_terms_hindi")
    private String importantTermsHindi = "";

    @NonNull
    @ColumnInfo(name = "worked_examples_english")
    private String workedExamplesEnglish = "";

    @NonNull
    @ColumnInfo(name = "worked_examples_hindi")
    private String workedExamplesHindi = "";

    @NonNull
    @ColumnInfo(name = "real_life_examples_english")
    private String realLifeExamplesEnglish = "";

    @NonNull
    @ColumnInfo(name = "real_life_examples_hindi")
    private String realLifeExamplesHindi = "";

    @NonNull
    @ColumnInfo(name = "common_mistakes_english")
    private String commonMistakesEnglish = "";

    @NonNull
    @ColumnInfo(name = "common_mistakes_hindi")
    private String commonMistakesHindi = "";

    @NonNull
    @ColumnInfo(name = "chapter_summary_english")
    private String chapterSummaryEnglish = "";

    @NonNull
    @ColumnInfo(name = "chapter_summary_hindi")
    private String chapterSummaryHindi = "";

    @NonNull
    @ColumnInfo(name = "practice_questions_json")
    private String practiceQuestionsJson = "[]";

    @NonNull
    @ColumnInfo(name = "source_page_references_json")
    private String sourcePageReferencesJson = "[]";

    @NonNull
    @ColumnInfo(name = "content_source")
    private String contentSource =
            CONTENT_SOURCE_PARENT_MANUAL;

    @NonNull
    @ColumnInfo(name = "review_status")
    private String reviewStatus =
            REVIEW_STATUS_DRAFT;

    @ColumnInfo(
            name = "parent_approved",
            defaultValue = "0"
    )
    private boolean parentApproved;

    @ColumnInfo(
            name = "generation_version",
            defaultValue = "1"
    )
    private int generationVersion = 1;

    @ColumnInfo(
            name = "estimated_reading_minutes",
            defaultValue = "0"
    )
    private int estimatedReadingMinutes;

    @ColumnInfo(
            name = "created_at",
            defaultValue = "0"
    )
    private long createdAt;

    @ColumnInfo(
            name = "updated_at",
            defaultValue = "0"
    )
    private long updatedAt;

    @ColumnInfo(
            name = "approved_at",
            defaultValue = "0"
    )
    private long approvedAt;

    @ColumnInfo(
            name = "last_generated_at",
            defaultValue = "0"
    )
    private long lastGeneratedAt;

    public long getContentRowId() {
        return contentRowId;
    }

    public void setContentRowId(
            long contentRowId
    ) {
        this.contentRowId =
                Math.max(
                        0L,
                        contentRowId
                );
    }

    public long getChapterRowId() {
        return chapterRowId;
    }

    public void setChapterRowId(
            long chapterRowId
    ) {
        this.chapterRowId =
                Math.max(
                        0L,
                        chapterRowId
                );
    }

    @NonNull
    public String getContentId() {
        return contentId;
    }

    public void setContentId(
            @Nullable String contentId
    ) {
        this.contentId =
                safeText(
                        contentId
                );
    }

    @NonNull
    public String getLanguageMode() {
        return languageMode;
    }

    public void setLanguageMode(
            @Nullable String languageMode
    ) {
        String normalizedValue =
                normalizeConstant(
                        languageMode
                );

        if (!normalizedValue.equals(
                LANGUAGE_MODE_ENGLISH
        ) && !normalizedValue.equals(
                LANGUAGE_MODE_HINDI
        ) && !normalizedValue.equals(
                LANGUAGE_MODE_BILINGUAL
        )) {
            normalizedValue =
                    LANGUAGE_MODE_BILINGUAL;
        }

        this.languageMode =
                normalizedValue;
    }

    @NonNull
    public String getChapterIntroductionEnglish() {
        return chapterIntroductionEnglish;
    }

    public void setChapterIntroductionEnglish(
            @Nullable String value
    ) {
        chapterIntroductionEnglish =
                safeText(value);
    }

    @NonNull
    public String getChapterIntroductionHindi() {
        return chapterIntroductionHindi;
    }

    public void setChapterIntroductionHindi(
            @Nullable String value
    ) {
        chapterIntroductionHindi =
                safeText(value);
    }

    @NonNull
    public String getDetailedExplanationEnglish() {
        return detailedExplanationEnglish;
    }

    public void setDetailedExplanationEnglish(
            @Nullable String value
    ) {
        detailedExplanationEnglish =
                safeText(value);
    }

    @NonNull
    public String getDetailedExplanationHindi() {
        return detailedExplanationHindi;
    }

    public void setDetailedExplanationHindi(
            @Nullable String value
    ) {
        detailedExplanationHindi =
                safeText(value);
    }

    @NonNull
    public String getKeyPointsEnglish() {
        return keyPointsEnglish;
    }

    public void setKeyPointsEnglish(
            @Nullable String value
    ) {
        keyPointsEnglish =
                safeText(value);
    }

    @NonNull
    public String getKeyPointsHindi() {
        return keyPointsHindi;
    }

    public void setKeyPointsHindi(
            @Nullable String value
    ) {
        keyPointsHindi =
                safeText(value);
    }

    @NonNull
    public String getImportantTermsEnglish() {
        return importantTermsEnglish;
    }

    public void setImportantTermsEnglish(
            @Nullable String value
    ) {
        importantTermsEnglish =
                safeText(value);
    }

    @NonNull
    public String getImportantTermsHindi() {
        return importantTermsHindi;
    }

    public void setImportantTermsHindi(
            @Nullable String value
    ) {
        importantTermsHindi =
                safeText(value);
    }

    @NonNull
    public String getWorkedExamplesEnglish() {
        return workedExamplesEnglish;
    }

    public void setWorkedExamplesEnglish(
            @Nullable String value
    ) {
        workedExamplesEnglish =
                safeText(value);
    }

    @NonNull
    public String getWorkedExamplesHindi() {
        return workedExamplesHindi;
    }

    public void setWorkedExamplesHindi(
            @Nullable String value
    ) {
        workedExamplesHindi =
                safeText(value);
    }

    @NonNull
    public String getRealLifeExamplesEnglish() {
        return realLifeExamplesEnglish;
    }

    public void setRealLifeExamplesEnglish(
            @Nullable String value
    ) {
        realLifeExamplesEnglish =
                safeText(value);
    }

    @NonNull
    public String getRealLifeExamplesHindi() {
        return realLifeExamplesHindi;
    }

    public void setRealLifeExamplesHindi(
            @Nullable String value
    ) {
        realLifeExamplesHindi =
                safeText(value);
    }

    @NonNull
    public String getCommonMistakesEnglish() {
        return commonMistakesEnglish;
    }

    public void setCommonMistakesEnglish(
            @Nullable String value
    ) {
        commonMistakesEnglish =
                safeText(value);
    }

    @NonNull
    public String getCommonMistakesHindi() {
        return commonMistakesHindi;
    }

    public void setCommonMistakesHindi(
            @Nullable String value
    ) {
        commonMistakesHindi =
                safeText(value);
    }

    @NonNull
    public String getChapterSummaryEnglish() {
        return chapterSummaryEnglish;
    }

    public void setChapterSummaryEnglish(
            @Nullable String value
    ) {
        chapterSummaryEnglish =
                safeText(value);
    }

    @NonNull
    public String getChapterSummaryHindi() {
        return chapterSummaryHindi;
    }

    public void setChapterSummaryHindi(
            @Nullable String value
    ) {
        chapterSummaryHindi =
                safeText(value);
    }

    @NonNull
    public String getPracticeQuestionsJson() {
        return practiceQuestionsJson;
    }

    public void setPracticeQuestionsJson(
            @Nullable String value
    ) {
        practiceQuestionsJson =
                safeJsonArray(value);
    }

    @NonNull
    public String getSourcePageReferencesJson() {
        return sourcePageReferencesJson;
    }

    public void setSourcePageReferencesJson(
            @Nullable String value
    ) {
        sourcePageReferencesJson =
                safeJsonArray(value);
    }

    @NonNull
    public String getContentSource() {
        return contentSource;
    }

    public void setContentSource(
            @Nullable String contentSource
    ) {
        String normalizedValue =
                normalizeConstant(
                        contentSource
                );

        if (!normalizedValue.equals(
                CONTENT_SOURCE_PARENT_MANUAL
        ) && !normalizedValue.equals(
                CONTENT_SOURCE_BOOK_PAGE_OCR
        ) && !normalizedValue.equals(
                CONTENT_SOURCE_AI_GENERATED
        ) && !normalizedValue.equals(
                CONTENT_SOURCE_AI_AND_OCR
        )) {
            normalizedValue =
                    CONTENT_SOURCE_PARENT_MANUAL;
        }

        this.contentSource =
                normalizedValue;
    }

    @NonNull
    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(
            @Nullable String reviewStatus
    ) {
        String normalizedValue =
                normalizeConstant(
                        reviewStatus
                );

        if (!normalizedValue.equals(
                REVIEW_STATUS_DRAFT
        ) && !normalizedValue.equals(
                REVIEW_STATUS_PROCESSING
        ) && !normalizedValue.equals(
                REVIEW_STATUS_PENDING_REVIEW
        ) && !normalizedValue.equals(
                REVIEW_STATUS_APPROVED
        ) && !normalizedValue.equals(
                REVIEW_STATUS_REJECTED
        ) && !normalizedValue.equals(
                REVIEW_STATUS_FAILED
        )) {
            normalizedValue =
                    REVIEW_STATUS_DRAFT;
        }

        this.reviewStatus =
                normalizedValue;
    }

    public boolean isParentApproved() {
        return parentApproved;
    }

    public void setParentApproved(
            boolean parentApproved
    ) {
        this.parentApproved =
                parentApproved;
    }

    public int getGenerationVersion() {
        return generationVersion;
    }

    public void setGenerationVersion(
            int generationVersion
    ) {
        this.generationVersion =
                Math.max(
                        1,
                        generationVersion
                );
    }

    public int getEstimatedReadingMinutes() {
        return estimatedReadingMinutes;
    }

    public void setEstimatedReadingMinutes(
            int estimatedReadingMinutes
    ) {
        this.estimatedReadingMinutes =
                Math.max(
                        0,
                        estimatedReadingMinutes
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

    public long getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(
            long approvedAt
    ) {
        this.approvedAt =
                Math.max(
                        0L,
                        approvedAt
                );
    }

    public long getLastGeneratedAt() {
        return lastGeneratedAt;
    }

    public void setLastGeneratedAt(
            long lastGeneratedAt
    ) {
        this.lastGeneratedAt =
                Math.max(
                        0L,
                        lastGeneratedAt
                );
    }

    public boolean isReadyForChildMode() {
        return parentApproved
                && REVIEW_STATUS_APPROVED.equals(
                reviewStatus
        )
                && hasReadableContent();
    }

    public boolean hasReadableContent() {
        return !chapterIntroductionEnglish.isEmpty()
                || !chapterIntroductionHindi.isEmpty()
                || !detailedExplanationEnglish.isEmpty()
                || !detailedExplanationHindi.isEmpty()
                || !chapterSummaryEnglish.isEmpty()
                || !chapterSummaryHindi.isEmpty();
    }

    public void prepareForNewDraft(
            long targetChapterRowId
    ) {
        long currentTime =
                System.currentTimeMillis();

        setChapterRowId(
                targetChapterRowId
        );

        if (contentId.isEmpty()) {
            contentId =
                    UUID.randomUUID()
                            .toString();
        }

        reviewStatus =
                REVIEW_STATUS_DRAFT;

        parentApproved =
                false;

        approvedAt =
                0L;

        if (createdAt <= 0L) {
            createdAt =
                    currentTime;
        }

        updatedAt =
                currentTime;
    }

    public void markPendingParentReview() {
        reviewStatus =
                REVIEW_STATUS_PENDING_REVIEW;

        parentApproved =
                false;

        approvedAt =
                0L;

        updatedAt =
                System.currentTimeMillis();
    }

    public void markParentApproved() {
        long currentTime =
                System.currentTimeMillis();

        reviewStatus =
                REVIEW_STATUS_APPROVED;

        parentApproved =
                true;

        approvedAt =
                currentTime;

        updatedAt =
                currentTime;
    }

    public void markRejected() {
        reviewStatus =
                REVIEW_STATUS_REJECTED;

        parentApproved =
                false;

        approvedAt =
                0L;

        updatedAt =
                System.currentTimeMillis();
    }

    @NonNull
    private String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString().trim();
    }

    @NonNull
    private String safeJsonArray(
            @Nullable String value
    ) {
        String normalizedValue =
                safeText(value);

        if (!normalizedValue.startsWith("[")
                || !normalizedValue.endsWith("]")) {
            return "[]";
        }

        return normalizedValue;
    }

    @NonNull
    private String normalizeConstant(
            @Nullable String value
    ) {
        return safeText(value)
                .toUpperCase(
                        Locale.ROOT
                );
    }
}