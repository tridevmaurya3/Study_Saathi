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
        tableName = "school_book_chapters",
        foreignKeys = {
                @ForeignKey(
                        entity = SchoolBookEntity.class,
                        parentColumns = "book_row_id",
                        childColumns = "book_row_id",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(
                        value = {
                                "book_row_id"
                        }
                ),
                @Index(
                        value = {
                                "book_row_id",
                                "chapter_id"
                        },
                        unique = true
                ),
                @Index(
                        value = {
                                "book_row_id",
                                "is_enabled",
                                "parent_confirmed",
                                "sort_order"
                        }
                ),
                @Index(
                        value = {
                                "content_source",
                                "content_processing_status"
                        }
                ),
                @Index(
                        value = {
                                "chapter_type"
                        }
                )
        }
)
public class SchoolBookChapterEntity {

    public static final String CHAPTER_TYPE_CHAPTER =
            "CHAPTER";

    public static final String CHAPTER_TYPE_UNIT =
            "UNIT";

    public static final String CHAPTER_TYPE_LESSON =
            "LESSON";

    public static final String CHAPTER_TYPE_POEM =
            "POEM";

    public static final String CHAPTER_TYPE_STORY =
            "STORY";

    public static final String CHAPTER_TYPE_ACTIVITY =
            "ACTIVITY";

    public static final String CHAPTER_TYPE_PROJECT =
            "PROJECT";

    public static final String CHAPTER_TYPE_APPENDIX =
            "APPENDIX";

    public static final String CONTENT_SOURCE_PARENT_MANUAL =
            "PARENT_MANUAL";

    public static final String CONTENT_SOURCE_BOOK_TOC_SCAN =
            "BOOK_TOC_SCAN";

    public static final String CONTENT_SOURCE_AUTHORIZED_IMPORT =
            "AUTHORIZED_IMPORT";

    public static final String CONTENT_SOURCE_AI_EXTRACTED =
            "AI_EXTRACTED";

    public static final String PROCESSING_STATUS_NOT_STARTED =
            "NOT_STARTED";

    public static final String PROCESSING_STATUS_PENDING_REVIEW =
            "PENDING_REVIEW";

    public static final String PROCESSING_STATUS_CONFIRMED =
            "CONFIRMED";

    public static final String PROCESSING_STATUS_PROCESSING =
            "PROCESSING";

    public static final String PROCESSING_STATUS_COMPLETED =
            "COMPLETED";

    public static final String PROCESSING_STATUS_FAILED =
            "FAILED";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "chapter_row_id")
    private long chapterRowId;

    @ColumnInfo(name = "book_row_id")
    private long bookRowId;

    @NonNull
    @ColumnInfo(name = "chapter_id")
    private String chapterId = "";

    @NonNull
    @ColumnInfo(name = "chapter_number")
    private String chapterNumber = "";

    @NonNull
    @ColumnInfo(name = "chapter_title_english")
    private String chapterTitleEnglish = "";

    @NonNull
    @ColumnInfo(name = "chapter_title_hindi")
    private String chapterTitleHindi = "";

    @NonNull
    @ColumnInfo(name = "chapter_subtitle")
    private String chapterSubtitle = "";

    @NonNull
    @ColumnInfo(name = "unit_name")
    private String unitName = "";

    @NonNull
    @ColumnInfo(
            name = "chapter_type",
            defaultValue = "'CHAPTER'"
    )
    private String chapterType =
            CHAPTER_TYPE_CHAPTER;

    @ColumnInfo(
            name = "start_page_number",
            defaultValue = "0"
    )
    private int startPageNumber;

    @ColumnInfo(
            name = "end_page_number",
            defaultValue = "0"
    )
    private int endPageNumber;

    @NonNull
    @ColumnInfo(name = "chapter_description")
    private String chapterDescription = "";

    @NonNull
    @ColumnInfo(name = "learning_objectives")
    private String learningObjectives = "";

    @NonNull
    @ColumnInfo(name = "important_topics")
    private String importantTopics = "";

    @NonNull
    @ColumnInfo(
            name = "content_source",
            defaultValue = "'PARENT_MANUAL'"
    )
    private String contentSource =
            CONTENT_SOURCE_PARENT_MANUAL;

    @NonNull
    @ColumnInfo(name = "source_reference")
    private String sourceReference = "";

    @ColumnInfo(
            name = "extraction_confidence",
            defaultValue = "0"
    )
    private float extractionConfidence;

    @ColumnInfo(
            name = "parent_confirmed",
            defaultValue = "0"
    )
    private boolean parentConfirmed;

    @ColumnInfo(
            name = "is_enabled",
            defaultValue = "1"
    )
    private boolean enabled = true;

    @ColumnInfo(
            name = "is_optional_chapter",
            defaultValue = "0"
    )
    private boolean optionalChapter;

    @ColumnInfo(
            name = "is_revision_chapter",
            defaultValue = "0"
    )
    private boolean revisionChapter;

    @NonNull
    @ColumnInfo(
            name = "content_processing_status",
            defaultValue = "'NOT_STARTED'"
    )
    private String contentProcessingStatus =
            PROCESSING_STATUS_NOT_STARTED;

    @ColumnInfo(
            name = "lesson_count",
            defaultValue = "0"
    )
    private int lessonCount;

    @ColumnInfo(
            name = "completed_lesson_count",
            defaultValue = "0"
    )
    private int completedLessonCount;

    @ColumnInfo(
            name = "quiz_question_count",
            defaultValue = "0"
    )
    private int quizQuestionCount;

    @ColumnInfo(
            name = "note_count",
            defaultValue = "0"
    )
    private int noteCount;

    @ColumnInfo(
            name = "bookmark_count",
            defaultValue = "0"
    )
    private int bookmarkCount;

    @ColumnInfo(
            name = "progress_percent",
            defaultValue = "0"
    )
    private int progressPercent;

    @ColumnInfo(
            name = "sort_order",
            defaultValue = "0"
    )
    private int sortOrder;

    @ColumnInfo(name = "last_opened_at")
    private long lastOpenedAt;

    @ColumnInfo(name = "last_content_processed_at")
    private long lastContentProcessedAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public SchoolBookChapterEntity() {
        /*
         * Empty constructor required by Room.
         */
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

    @NonNull
    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(
            @Nullable String chapterId
    ) {
        this.chapterId =
                normalizeIdentifier(
                        chapterId
                );
    }

    @NonNull
    public String getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(
            @Nullable String chapterNumber
    ) {
        this.chapterNumber =
                normalizeOptionalText(
                        chapterNumber
                );
    }

    @NonNull
    public String getChapterTitleEnglish() {
        return chapterTitleEnglish;
    }

    public void setChapterTitleEnglish(
            @Nullable String chapterTitleEnglish
    ) {
        this.chapterTitleEnglish =
                normalizeOptionalText(
                        chapterTitleEnglish
                );
    }

    @NonNull
    public String getChapterTitleHindi() {
        return chapterTitleHindi;
    }

    public void setChapterTitleHindi(
            @Nullable String chapterTitleHindi
    ) {
        this.chapterTitleHindi =
                normalizeOptionalText(
                        chapterTitleHindi
                );
    }

    @NonNull
    public String getChapterSubtitle() {
        return chapterSubtitle;
    }

    public void setChapterSubtitle(
            @Nullable String chapterSubtitle
    ) {
        this.chapterSubtitle =
                normalizeOptionalText(
                        chapterSubtitle
                );
    }

    @NonNull
    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(
            @Nullable String unitName
    ) {
        this.unitName =
                normalizeOptionalText(
                        unitName
                );
    }

    @NonNull
    public String getChapterType() {
        return chapterType;
    }

    public void setChapterType(
            @Nullable String chapterType
    ) {
        this.chapterType =
                normalizeChapterType(
                        chapterType
                );
    }

    public int getStartPageNumber() {
        return startPageNumber;
    }

    public void setStartPageNumber(
            int startPageNumber
    ) {
        this.startPageNumber =
                Math.max(
                        0,
                        startPageNumber
                );

        if (endPageNumber > 0
                && endPageNumber
                < this.startPageNumber) {

            endPageNumber =
                    this.startPageNumber;
        }
    }

    public int getEndPageNumber() {
        return endPageNumber;
    }

    public void setEndPageNumber(
            int endPageNumber
    ) {
        int safeEndPage =
                Math.max(
                        0,
                        endPageNumber
                );

        if (safeEndPage > 0
                && startPageNumber > 0
                && safeEndPage < startPageNumber) {

            safeEndPage =
                    startPageNumber;
        }

        this.endPageNumber =
                safeEndPage;
    }

    @NonNull
    public String getChapterDescription() {
        return chapterDescription;
    }

    public void setChapterDescription(
            @Nullable String chapterDescription
    ) {
        this.chapterDescription =
                normalizeMultilineText(
                        chapterDescription
                );
    }

    @NonNull
    public String getLearningObjectives() {
        return learningObjectives;
    }

    public void setLearningObjectives(
            @Nullable String learningObjectives
    ) {
        this.learningObjectives =
                normalizeMultilineText(
                        learningObjectives
                );
    }

    @NonNull
    public String getImportantTopics() {
        return importantTopics;
    }

    public void setImportantTopics(
            @Nullable String importantTopics
    ) {
        this.importantTopics =
                normalizeMultilineText(
                        importantTopics
                );
    }

    @NonNull
    public String getContentSource() {
        return contentSource;
    }

    public void setContentSource(
            @Nullable String contentSource
    ) {
        this.contentSource =
                normalizeContentSource(
                        contentSource
                );
    }

    @NonNull
    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(
            @Nullable String sourceReference
    ) {
        this.sourceReference =
                normalizeOptionalText(
                        sourceReference
                );
    }

    public float getExtractionConfidence() {
        return extractionConfidence;
    }

    public void setExtractionConfidence(
            float extractionConfidence
    ) {
        if (Float.isNaN(
                extractionConfidence
        )
                || Float.isInfinite(
                extractionConfidence
        )) {

            this.extractionConfidence =
                    0F;

            return;
        }

        this.extractionConfidence =
                Math.max(
                        0F,
                        Math.min(
                                1F,
                                extractionConfidence
                        )
                );
    }

    public boolean isParentConfirmed() {
        return parentConfirmed;
    }

    public void setParentConfirmed(
            boolean parentConfirmed
    ) {
        this.parentConfirmed =
                parentConfirmed;

        if (parentConfirmed
                && PROCESSING_STATUS_PENDING_REVIEW.equals(
                contentProcessingStatus
        )) {

            contentProcessingStatus =
                    PROCESSING_STATUS_CONFIRMED;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(
            boolean enabled
    ) {
        this.enabled =
                enabled;
    }

    public boolean isOptionalChapter() {
        return optionalChapter;
    }

    public void setOptionalChapter(
            boolean optionalChapter
    ) {
        this.optionalChapter =
                optionalChapter;
    }

    public boolean isRevisionChapter() {
        return revisionChapter;
    }

    public void setRevisionChapter(
            boolean revisionChapter
    ) {
        this.revisionChapter =
                revisionChapter;
    }

    @NonNull
    public String getContentProcessingStatus() {
        return contentProcessingStatus;
    }

    public void setContentProcessingStatus(
            @Nullable String contentProcessingStatus
    ) {
        this.contentProcessingStatus =
                normalizeProcessingStatus(
                        contentProcessingStatus
                );
    }

    public int getLessonCount() {
        return lessonCount;
    }

    public void setLessonCount(
            int lessonCount
    ) {
        this.lessonCount =
                Math.max(
                        0,
                        lessonCount
                );

        if (completedLessonCount
                > this.lessonCount) {

            completedLessonCount =
                    this.lessonCount;
        }
    }

    public int getCompletedLessonCount() {
        return completedLessonCount;
    }

    public void setCompletedLessonCount(
            int completedLessonCount
    ) {
        int safeCompletedCount =
                Math.max(
                        0,
                        completedLessonCount
                );

        if (lessonCount > 0) {
            safeCompletedCount =
                    Math.min(
                            lessonCount,
                            safeCompletedCount
                    );
        }

        this.completedLessonCount =
                safeCompletedCount;
    }

    public int getQuizQuestionCount() {
        return quizQuestionCount;
    }

    public void setQuizQuestionCount(
            int quizQuestionCount
    ) {
        this.quizQuestionCount =
                Math.max(
                        0,
                        quizQuestionCount
                );
    }

    public int getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(
            int noteCount
    ) {
        this.noteCount =
                Math.max(
                        0,
                        noteCount
                );
    }

    public int getBookmarkCount() {
        return bookmarkCount;
    }

    public void setBookmarkCount(
            int bookmarkCount
    ) {
        this.bookmarkCount =
                Math.max(
                        0,
                        bookmarkCount
                );
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(
            int progressPercent
    ) {
        this.progressPercent =
                Math.max(
                        0,
                        Math.min(
                                100,
                                progressPercent
                        )
                );
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

    public long getLastOpenedAt() {
        return lastOpenedAt;
    }

    public void setLastOpenedAt(
            long lastOpenedAt
    ) {
        this.lastOpenedAt =
                Math.max(
                        0L,
                        lastOpenedAt
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

    public boolean hasMinimumRequiredInformation() {
        return bookRowId > 0L
                && !chapterId.isEmpty()
                && hasValidTitle();
    }

    public boolean hasValidTitle() {
        return !chapterTitleEnglish.isEmpty()
                || !chapterTitleHindi.isEmpty();
    }

    public boolean hasPageRange() {
        return startPageNumber > 0
                || endPageNumber > 0;
    }

    public boolean hasValidPageRange() {
        if (!hasPageRange()) {
            return true;
        }

        if (startPageNumber <= 0) {
            return false;
        }

        return endPageNumber <= 0
                || endPageNumber >= startPageNumber;
    }

    public boolean isReadyForChildMode() {
        return hasMinimumRequiredInformation()
                && enabled
                && parentConfirmed;
    }

    public boolean hasGeneratedContent() {
        return lessonCount > 0
                || quizQuestionCount > 0;
    }

    public boolean hasLearningProgress() {
        return completedLessonCount > 0
                || progressPercent > 0
                || lastOpenedAt > 0L;
    }

    @NonNull
    public String getDisplayTitle() {
        if (!chapterTitleEnglish.isEmpty()) {
            return chapterTitleEnglish;
        }

        if (!chapterTitleHindi.isEmpty()) {
            return chapterTitleHindi;
        }

        return "School Book Chapter";
    }

    @NonNull
    public String getSecondaryTitle() {
        if (!chapterTitleEnglish.isEmpty()
                && !chapterTitleHindi.isEmpty()) {

            return chapterTitleHindi;
        }

        return chapterSubtitle;
    }

    @NonNull
    public String getChapterLabel() {
        String displayNumber =
                chapterNumber;

        if (displayNumber.isEmpty()) {
            displayNumber =
                    String.valueOf(
                            Math.max(
                                    1,
                                    sortOrder
                            )
                    );
        }

        switch (chapterType) {
            case CHAPTER_TYPE_UNIT:
                return "Unit "
                        + displayNumber;

            case CHAPTER_TYPE_LESSON:
                return "Lesson "
                        + displayNumber;

            case CHAPTER_TYPE_POEM:
                return "Poem "
                        + displayNumber;

            case CHAPTER_TYPE_STORY:
                return "Story "
                        + displayNumber;

            case CHAPTER_TYPE_ACTIVITY:
                return "Activity "
                        + displayNumber;

            case CHAPTER_TYPE_PROJECT:
                return "Project "
                        + displayNumber;

            case CHAPTER_TYPE_APPENDIX:
                return "Appendix "
                        + displayNumber;

            case CHAPTER_TYPE_CHAPTER:
            default:
                return "Chapter "
                        + displayNumber;
        }
    }

    @NonNull
    public String getPageRangeLabel() {
        if (startPageNumber <= 0
                && endPageNumber <= 0) {

            return "";
        }

        if (startPageNumber > 0
                && endPageNumber > startPageNumber) {

            return "Pages "
                    + startPageNumber
                    + "–"
                    + endPageNumber;
        }

        int pageNumber =
                startPageNumber > 0
                        ? startPageNumber
                        : endPageNumber;

        return "Page "
                + pageNumber;
    }

    @NonNull
    public String getConfirmationStatusLabel() {
        if (parentConfirmed) {
            return "Parent Confirmed";
        }

        if (CONTENT_SOURCE_BOOK_TOC_SCAN.equals(
                contentSource
        )
                || CONTENT_SOURCE_AI_EXTRACTED.equals(
                contentSource
        )) {

            return "Parent Review Pending";
        }

        return "Not Confirmed";
    }

    @NonNull
    public String getProgressLabel() {
        if (lessonCount <= 0) {
            return "Lessons pending";
        }

        return completedLessonCount
                + "/"
                + lessonCount
                + " lessons completed";
    }

    @NonNull
    public static String createChapterId() {
        return "chapter_"
                + UUID.randomUUID()
                .toString()
                .replace(
                        "-",
                        ""
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }

    @NonNull
    private static String normalizeIdentifier(
            @Nullable Object value
    ) {
        return normalizeOptionalText(
                value
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
                )
                .replaceAll(
                        "^_+|_+$",
                        ""
                );
    }

    @NonNull
    private static String normalizeChapterType(
            @Nullable Object value
    ) {
        String normalizedType =
                normalizeEnumValue(
                        value
                );

        switch (normalizedType) {
            case CHAPTER_TYPE_UNIT:
            case CHAPTER_TYPE_LESSON:
            case CHAPTER_TYPE_POEM:
            case CHAPTER_TYPE_STORY:
            case CHAPTER_TYPE_ACTIVITY:
            case CHAPTER_TYPE_PROJECT:
            case CHAPTER_TYPE_APPENDIX:
            case CHAPTER_TYPE_CHAPTER:
                return normalizedType;

            default:
                return CHAPTER_TYPE_CHAPTER;
        }
    }

    @NonNull
    private static String normalizeContentSource(
            @Nullable Object value
    ) {
        String normalizedSource =
                normalizeEnumValue(
                        value
                );

        switch (normalizedSource) {
            case CONTENT_SOURCE_BOOK_TOC_SCAN:
            case CONTENT_SOURCE_AUTHORIZED_IMPORT:
            case CONTENT_SOURCE_AI_EXTRACTED:
            case CONTENT_SOURCE_PARENT_MANUAL:
                return normalizedSource;

            default:
                return CONTENT_SOURCE_PARENT_MANUAL;
        }
    }

    @NonNull
    private static String normalizeProcessingStatus(
            @Nullable Object value
    ) {
        String normalizedStatus =
                normalizeEnumValue(
                        value
                );

        switch (normalizedStatus) {
            case PROCESSING_STATUS_PENDING_REVIEW:
            case PROCESSING_STATUS_CONFIRMED:
            case PROCESSING_STATUS_PROCESSING:
            case PROCESSING_STATUS_COMPLETED:
            case PROCESSING_STATUS_FAILED:
            case PROCESSING_STATUS_NOT_STARTED:
                return normalizedStatus;

            default:
                return PROCESSING_STATUS_NOT_STARTED;
        }
    }

    @NonNull
    private static String normalizeEnumValue(
            @Nullable Object value
    ) {
        return normalizeOptionalText(
                value
        )
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
                )
                .replaceAll(
                        "_+",
                        "_"
                );
    }

    @NonNull
    private static String normalizeOptionalText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    private static String normalizeMultilineText(
            @Nullable Object value
    ) {
        if (value == null) {
            return "";
        }

        String normalizedValue =
                value.toString()
                        .replace(
                                "\r\n",
                                "\n"
                        )
                        .replace(
                                "\r",
                                "\n"
                        )
                        .trim();

        return normalizedValue.replaceAll(
                "[\\t ]+",
                " "
        );
    }
}