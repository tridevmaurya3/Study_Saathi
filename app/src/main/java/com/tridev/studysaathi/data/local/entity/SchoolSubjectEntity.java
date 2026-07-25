package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Locale;

@Entity(
        tableName = "school_subjects",
        foreignKeys = {
                @ForeignKey(
                        entity = SchoolCurriculumProfileEntity.class,
                        parentColumns = "profile_id",
                        childColumns = "profile_id",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(
                        value = {"profile_id"}
                ),
                @Index(
                        value = {
                                "profile_id",
                                "subject_id"
                        },
                        unique = true
                ),
                @Index(
                        value = {
                                "profile_id",
                                "is_enabled",
                                "sort_order"
                        }
                )
        }
)
public class SchoolSubjectEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "subject_row_id")
    private long subjectRowId;

    @ColumnInfo(name = "profile_id")
    private long profileId;

    @NonNull
    @ColumnInfo(name = "subject_id")
    private String subjectId = "";

    @NonNull
    @ColumnInfo(name = "subject_name_english")
    private String subjectNameEnglish = "";

    @NonNull
    @ColumnInfo(name = "subject_name_hindi")
    private String subjectNameHindi = "";

    @NonNull
    @ColumnInfo(name = "subject_code")
    private String subjectCode = "";

    @NonNull
    @ColumnInfo(name = "book_name")
    private String bookName = "";

    @NonNull
    @ColumnInfo(name = "book_code")
    private String bookCode = "";

    @NonNull
    @ColumnInfo(name = "publisher_name")
    private String publisherName = "";

    @NonNull
    @ColumnInfo(
            name = "subject_category",
            defaultValue = "'SCHOOL_SPECIFIC'"
    )
    private String subjectCategory =
            "SCHOOL_SPECIFIC";

    @NonNull
    @ColumnInfo(
            name = "content_source",
            defaultValue = "'SCHOOL_BOOK'"
    )
    private String contentSource =
            "SCHOOL_BOOK";

    @NonNull
    @ColumnInfo(name = "content_pack_id")
    private String contentPackId = "";

    @ColumnInfo(
            name = "is_enabled",
            defaultValue = "1"
    )
    private boolean enabled = true;

    @ColumnInfo(
            name = "ai_tutor_enabled",
            defaultValue = "1"
    )
    private boolean aiTutorEnabled = true;

    @ColumnInfo(
            name = "is_official_core_subject",
            defaultValue = "0"
    )
    private boolean officialCoreSubject;

    @ColumnInfo(
            name = "allow_parent_content_editing",
            defaultValue = "1"
    )
    private boolean allowParentContentEditing = true;

    @ColumnInfo(
            name = "sort_order",
            defaultValue = "0"
    )
    private int sortOrder;

    @ColumnInfo(
            name = "chapter_count",
            defaultValue = "0"
    )
    private int chapterCount;

    @ColumnInfo(
            name = "lesson_count",
            defaultValue = "0"
    )
    private int lessonCount;

    @ColumnInfo(
            name = "quiz_question_count",
            defaultValue = "0"
    )
    private int quizQuestionCount;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public SchoolSubjectEntity() {
        // Required empty constructor for Room.
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

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(
            long profileId
    ) {
        this.profileId =
                Math.max(
                        0L,
                        profileId
                );
    }

    @NonNull
    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(
            @NonNull String subjectId
    ) {
        this.subjectId =
                normalizeIdentifier(
                        subjectId
                );
    }

    @NonNull
    public String getSubjectNameEnglish() {
        return subjectNameEnglish;
    }

    public void setSubjectNameEnglish(
            @NonNull String subjectNameEnglish
    ) {
        this.subjectNameEnglish =
                normalizeRequiredText(
                        subjectNameEnglish
                );
    }

    @NonNull
    public String getSubjectNameHindi() {
        return subjectNameHindi;
    }

    public void setSubjectNameHindi(
            @NonNull String subjectNameHindi
    ) {
        this.subjectNameHindi =
                normalizeRequiredText(
                        subjectNameHindi
                );
    }

    @NonNull
    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(
            @NonNull String subjectCode
    ) {
        this.subjectCode =
                normalizeOptionalText(
                        subjectCode
                );
    }

    @NonNull
    public String getBookName() {
        return bookName;
    }

    public void setBookName(
            @NonNull String bookName
    ) {
        this.bookName =
                normalizeOptionalText(
                        bookName
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
    public String getSubjectCategory() {
        return subjectCategory;
    }

    public void setSubjectCategory(
            @NonNull String subjectCategory
    ) {
        this.subjectCategory =
                normalizeSubjectCategory(
                        subjectCategory
                );
    }

    @NonNull
    public String getContentSource() {
        return contentSource;
    }

    public void setContentSource(
            @NonNull String contentSource
    ) {
        this.contentSource =
                normalizeContentSource(
                        contentSource
                );
    }

    @NonNull
    public String getContentPackId() {
        return contentPackId;
    }

    public void setContentPackId(
            @NonNull String contentPackId
    ) {
        String normalizedContentPackId =
                normalizeOptionalText(
                        contentPackId
                );

        if (normalizedContentPackId.isEmpty()) {
            this.contentPackId = "";
        } else {
            this.contentPackId =
                    normalizeIdentifier(
                            normalizedContentPackId
                    );
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

    public boolean isAiTutorEnabled() {
        return aiTutorEnabled;
    }

    public void setAiTutorEnabled(
            boolean aiTutorEnabled
    ) {
        this.aiTutorEnabled =
                aiTutorEnabled;
    }

    public boolean isOfficialCoreSubject() {
        return officialCoreSubject;
    }

    public void setOfficialCoreSubject(
            boolean officialCoreSubject
    ) {
        this.officialCoreSubject =
                officialCoreSubject;
    }

    public boolean isAllowParentContentEditing() {
        return allowParentContentEditing;
    }

    public void setAllowParentContentEditing(
            boolean allowParentContentEditing
    ) {
        this.allowParentContentEditing =
                allowParentContentEditing;
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

    @NonNull
    public String getDisplayName(
            boolean useHindi
    ) {
        if (useHindi
                && !subjectNameHindi.isEmpty()) {

            return subjectNameHindi;
        }

        return subjectNameEnglish;
    }

    @NonNull
    public String getBilingualDisplayName() {
        if (subjectNameHindi.isEmpty()
                || subjectNameHindi.equalsIgnoreCase(
                subjectNameEnglish
        )) {
            return subjectNameEnglish;
        }

        return subjectNameEnglish
                + " / "
                + subjectNameHindi;
    }

    public boolean isCoreAcademicSubject() {
        return "CORE_ACADEMIC".equals(
                subjectCategory
        );
    }

    public boolean isLanguageSubject() {
        return "LANGUAGE".equals(
                subjectCategory
        );
    }

    public boolean isSchoolSpecificSubject() {
        return "SCHOOL_SPECIFIC".equals(
                subjectCategory
        );
    }

    public boolean isSkillBasedSubject() {
        return "SKILL_BASED".equals(
                subjectCategory
        );
    }

    public boolean isActivityBasedSubject() {
        return "ACTIVITY_BASED".equals(
                subjectCategory
        );
    }

    public boolean isNcertContent() {
        return "NCERT".equals(
                contentSource
        );
    }

    public boolean hasBookInformation() {
        return !bookName.isEmpty()
                || !bookCode.isEmpty()
                || !publisherName.isEmpty();
    }

    public boolean hasInstalledContentPack() {
        return !contentPackId.isEmpty();
    }

    public boolean hasMinimumRequiredInformation() {
        return profileId > 0L
                && !subjectId.isEmpty()
                && !subjectNameEnglish.isEmpty()
                && !subjectNameHindi.isEmpty();
    }

    public void applyCoreSubjectDefaults() {
        subjectCategory =
                "CORE_ACADEMIC";

        contentSource =
                "NCERT";

        enabled =
                true;

        aiTutorEnabled =
                true;

        officialCoreSubject =
                true;

        allowParentContentEditing =
                false;
    }

    public void applySchoolSpecificDefaults() {
        subjectCategory =
                "SCHOOL_SPECIFIC";

        contentSource =
                "SCHOOL_BOOK";

        enabled =
                true;

        aiTutorEnabled =
                true;

        officialCoreSubject =
                false;

        allowParentContentEditing =
                true;
    }

    @NonNull
    public static String createSubjectId(
            @NonNull String subjectNameEnglish
    ) {
        return normalizeIdentifier(
                subjectNameEnglish
        );
    }

    @NonNull
    private static String normalizeRequiredText(
            @NonNull String value
    ) {
        String normalizedValue =
                value.trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    "Required subject information cannot be empty."
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
                    "Subject identifier is invalid."
            );
        }

        return normalizedValue;
    }

    @NonNull
    private static String normalizeSubjectCategory(
            @NonNull String subjectCategory
    ) {
        String normalizedCategory =
                subjectCategory.trim()
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

        switch (normalizedCategory) {
            case "CORE_ACADEMIC":
            case "LANGUAGE":
            case "SCHOOL_SPECIFIC":
            case "SKILL_BASED":
            case "ACTIVITY_BASED":
                return normalizedCategory;

            default:
                return "SCHOOL_SPECIFIC";
        }
    }

    @NonNull
    private static String normalizeContentSource(
            @NonNull String contentSource
    ) {
        String normalizedSource =
                contentSource.trim()
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

        switch (normalizedSource) {
            case "NCERT":
            case "SCHOOL_BOOK":
            case "PRIVATE_PUBLISHER":
            case "TEACHER_NOTES":
            case "PARENT_CREATED":
            case "AI_ASSISTED":
            case "CUSTOM":
                return normalizedSource;

            default:
                return "SCHOOL_BOOK";
        }
    }
}