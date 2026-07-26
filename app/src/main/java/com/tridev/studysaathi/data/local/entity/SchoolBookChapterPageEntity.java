package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

/**
 * Kinder/ebook-style reader के लिए chapter का एक अलग learning page.
 *
 * <p>एक scanned/PDF source page को एक row में रखा जाता है। Child Mode में
 * एक समय पर केवल एक row दिखाई जाएगी और Next/Previous से navigation होगा।
 */
@Entity(
        tableName = "school_book_chapter_pages",
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
                                "chapter_row_id",
                                "page_order"
                        },
                        unique = true
                ),
                @Index(
                        value = {
                                "chapter_row_id",
                                "parent_approved"
                        }
                ),
                @Index(
                        value = {
                                "source_document_page_number"
                        }
                )
        }
)
public final class SchoolBookChapterPageEntity {

    public static final String PAGE_TYPE_LEARNING =
            "LEARNING";

    public static final String PAGE_TYPE_EXAMPLE =
            "EXAMPLE";

    public static final String PAGE_TYPE_EXERCISE =
            "EXERCISE";

    public static final String PAGE_TYPE_SUMMARY =
            "SUMMARY";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "chapter_page_row_id")
    private long chapterPageRowId;

    @ColumnInfo(name = "chapter_row_id")
    private long chapterRowId;

    @NonNull
    @ColumnInfo(name = "chapter_page_id")
    private String chapterPageId = "";

    @ColumnInfo(name = "page_order")
    private int pageOrder;

    @ColumnInfo(name = "source_document_page_number")
    private int sourceDocumentPageNumber;

    @NonNull
    @ColumnInfo(name = "page_type")
    private String pageType =
            PAGE_TYPE_LEARNING;

    @NonNull
    @ColumnInfo(name = "page_title")
    private String pageTitle = "";

    @NonNull
    @ColumnInfo(name = "introduction_english")
    private String introductionEnglish = "";

    @NonNull
    @ColumnInfo(name = "introduction_hindi")
    private String introductionHindi = "";

    @NonNull
    @ColumnInfo(name = "explanation_english")
    private String explanationEnglish = "";

    @NonNull
    @ColumnInfo(name = "explanation_hindi")
    private String explanationHindi = "";

    @NonNull
    @ColumnInfo(name = "key_points_english")
    private String keyPointsEnglish = "";

    @NonNull
    @ColumnInfo(name = "key_points_hindi")
    private String keyPointsHindi = "";

    @NonNull
    @ColumnInfo(name = "examples_english")
    private String examplesEnglish = "";

    @NonNull
    @ColumnInfo(name = "examples_hindi")
    private String examplesHindi = "";

    @NonNull
    @ColumnInfo(name = "exercises_json")
    private String exercisesJson = "[]";

    @NonNull
    @ColumnInfo(name = "summary_english")
    private String summaryEnglish = "";

    @NonNull
    @ColumnInfo(name = "summary_hindi")
    private String summaryHindi = "";

    /**
     * App के persistent internal storage में सुरक्षित page image path.
     * Cache path यहाँ save नहीं किया जाएगा।
     */
    @NonNull
    @ColumnInfo(name = "persistent_page_image_path")
    private String persistentPageImagePath = "";

    @NonNull
    @ColumnInfo(name = "raw_ocr_text")
    private String rawOcrText = "";

    @ColumnInfo(
            name = "parent_approved",
            defaultValue = "0"
    )
    private boolean parentApproved;

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

    public long getChapterPageRowId() {
        return chapterPageRowId;
    }

    public void setChapterPageRowId(
            long chapterPageRowId
    ) {
        this.chapterPageRowId =
                Math.max(0L, chapterPageRowId);
    }

    public long getChapterRowId() {
        return chapterRowId;
    }

    public void setChapterRowId(
            long chapterRowId
    ) {
        this.chapterRowId =
                Math.max(0L, chapterRowId);
    }

    @NonNull
    public String getChapterPageId() {
        return chapterPageId;
    }

    public void setChapterPageId(
            @Nullable String chapterPageId
    ) {
        this.chapterPageId =
                safeText(chapterPageId);
    }

    public int getPageOrder() {
        return pageOrder;
    }

    public void setPageOrder(
            int pageOrder
    ) {
        this.pageOrder =
                Math.max(1, pageOrder);
    }

    public int getSourceDocumentPageNumber() {
        return sourceDocumentPageNumber;
    }

    public void setSourceDocumentPageNumber(
            int sourceDocumentPageNumber
    ) {
        this.sourceDocumentPageNumber =
                Math.max(
                        1,
                        sourceDocumentPageNumber
                );
    }

    @NonNull
    public String getPageType() {
        return pageType;
    }

    public void setPageType(
            @Nullable String pageType
    ) {
        String normalized =
                safeText(pageType);

        if (!PAGE_TYPE_LEARNING.equals(normalized)
                && !PAGE_TYPE_EXAMPLE.equals(normalized)
                && !PAGE_TYPE_EXERCISE.equals(normalized)
                && !PAGE_TYPE_SUMMARY.equals(normalized)) {
            normalized =
                    PAGE_TYPE_LEARNING;
        }

        this.pageType = normalized;
    }

    @NonNull
    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(
            @Nullable String pageTitle
    ) {
        this.pageTitle =
                safeText(pageTitle);
    }

    @NonNull
    public String getIntroductionEnglish() {
        return introductionEnglish;
    }

    public void setIntroductionEnglish(
            @Nullable String value
    ) {
        introductionEnglish = safeText(value);
    }

    @NonNull
    public String getIntroductionHindi() {
        return introductionHindi;
    }

    public void setIntroductionHindi(
            @Nullable String value
    ) {
        introductionHindi = safeText(value);
    }

    @NonNull
    public String getExplanationEnglish() {
        return explanationEnglish;
    }

    public void setExplanationEnglish(
            @Nullable String value
    ) {
        explanationEnglish = safeText(value);
    }

    @NonNull
    public String getExplanationHindi() {
        return explanationHindi;
    }

    public void setExplanationHindi(
            @Nullable String value
    ) {
        explanationHindi = safeText(value);
    }

    @NonNull
    public String getKeyPointsEnglish() {
        return keyPointsEnglish;
    }

    public void setKeyPointsEnglish(
            @Nullable String value
    ) {
        keyPointsEnglish = safeText(value);
    }

    @NonNull
    public String getKeyPointsHindi() {
        return keyPointsHindi;
    }

    public void setKeyPointsHindi(
            @Nullable String value
    ) {
        keyPointsHindi = safeText(value);
    }

    @NonNull
    public String getExamplesEnglish() {
        return examplesEnglish;
    }

    public void setExamplesEnglish(
            @Nullable String value
    ) {
        examplesEnglish = safeText(value);
    }

    @NonNull
    public String getExamplesHindi() {
        return examplesHindi;
    }

    public void setExamplesHindi(
            @Nullable String value
    ) {
        examplesHindi = safeText(value);
    }

    @NonNull
    public String getExercisesJson() {
        return exercisesJson;
    }

    public void setExercisesJson(
            @Nullable String exercisesJson
    ) {
        String normalized =
                safeText(exercisesJson);

        this.exercisesJson =
                normalized.isEmpty()
                        ? "[]"
                        : normalized;
    }

    @NonNull
    public String getSummaryEnglish() {
        return summaryEnglish;
    }

    public void setSummaryEnglish(
            @Nullable String value
    ) {
        summaryEnglish = safeText(value);
    }

    @NonNull
    public String getSummaryHindi() {
        return summaryHindi;
    }

    public void setSummaryHindi(
            @Nullable String value
    ) {
        summaryHindi = safeText(value);
    }

    @NonNull
    public String getPersistentPageImagePath() {
        return persistentPageImagePath;
    }

    public void setPersistentPageImagePath(
            @Nullable String path
    ) {
        persistentPageImagePath =
                safeText(path);
    }

    @NonNull
    public String getRawOcrText() {
        return rawOcrText;
    }

    public void setRawOcrText(
            @Nullable String rawOcrText
    ) {
        this.rawOcrText =
                safeText(rawOcrText);
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            long createdAt
    ) {
        this.createdAt =
                Math.max(0L, createdAt);
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            long updatedAt
    ) {
        this.updatedAt =
                Math.max(0L, updatedAt);
    }

    public void prepareForInsert(
            long targetChapterRowId,
            int targetPageOrder
    ) {
        long currentTime =
                System.currentTimeMillis();

        setChapterRowId(targetChapterRowId);
        setPageOrder(targetPageOrder);

        if (chapterPageId.isEmpty()) {
            chapterPageId =
                    UUID.randomUUID().toString();
        }

        parentApproved = false;

        if (createdAt <= 0L) {
            createdAt = currentTime;
        }

        updatedAt = currentTime;
    }

    public boolean hasReadableContent() {
        return !introductionEnglish.isEmpty()
                || !introductionHindi.isEmpty()
                || !explanationEnglish.isEmpty()
                || !explanationHindi.isEmpty()
                || !keyPointsEnglish.isEmpty()
                || !keyPointsHindi.isEmpty()
                || !examplesEnglish.isEmpty()
                || !examplesHindi.isEmpty()
                || !"[]".equals(exercisesJson)
                || !summaryEnglish.isEmpty()
                || !summaryHindi.isEmpty()
                || !persistentPageImagePath.isEmpty();
    }

    public boolean isReadyForChildMode() {
        return parentApproved
                && hasReadableContent();
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
