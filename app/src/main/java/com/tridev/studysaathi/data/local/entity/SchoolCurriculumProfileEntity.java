package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "school_curriculum_profiles",
        foreignKeys = {
                @ForeignKey(
                        entity = StudentProfileEntity.class,
                        parentColumns = "profile_id",
                        childColumns = "profile_id",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(
                        value = {"curriculum_id"},
                        unique = true
                ),
                @Index(
                        value = {
                                "school_name",
                                "education_board",
                                "class_number"
                        }
                )
        }
)
public class SchoolCurriculumProfileEntity {

    @PrimaryKey
    @ColumnInfo(name = "profile_id")
    private long profileId;

    @NonNull
    @ColumnInfo(name = "curriculum_id")
    private String curriculumId = "";

    @NonNull
    @ColumnInfo(name = "school_name")
    private String schoolName = "";

    @NonNull
    @ColumnInfo(name = "school_code")
    private String schoolCode = "";

    @NonNull
    @ColumnInfo(name = "education_board")
    private String educationBoard = "";

    @NonNull
    @ColumnInfo(name = "school_pattern")
    private String schoolPattern = "";

    @ColumnInfo(
            name = "class_number",
            defaultValue = "6"
    )
    private int classNumber = 6;

    @NonNull
    @ColumnInfo(name = "section")
    private String section = "";

    @NonNull
    @ColumnInfo(name = "academic_session")
    private String academicSession = "";

    @NonNull
    @ColumnInfo(name = "study_medium")
    private String studyMedium = "";

    @ColumnInfo(
            name = "ai_tutor_enabled",
            defaultValue = "1"
    )
    private boolean aiTutorEnabled = true;

    @NonNull
    @ColumnInfo(
            name = "ai_default_language",
            defaultValue = "'BILINGUAL'"
    )
    private String aiDefaultLanguage =
            "BILINGUAL";

    @NonNull
    @ColumnInfo(
            name = "ai_default_answer_mode",
            defaultValue = "'SIMPLE'"
    )
    private String aiDefaultAnswerMode =
            "SIMPLE";

    @ColumnInfo(
            name = "voice_question_enabled",
            defaultValue = "1"
    )
    private boolean voiceQuestionEnabled = true;

    @ColumnInfo(
            name = "image_question_enabled",
            defaultValue = "1"
    )
    private boolean imageQuestionEnabled = true;

    @ColumnInfo(
            name = "read_answer_aloud_enabled",
            defaultValue = "1"
    )
    private boolean readAnswerAloudEnabled = true;

    @ColumnInfo(
            name = "child_safe_answers_enabled",
            defaultValue = "1"
    )
    private boolean childSafeAnswersEnabled = true;

    @ColumnInfo(
            name = "save_doubt_history_enabled",
            defaultValue = "1"
    )
    private boolean saveDoubtHistoryEnabled = true;

    @ColumnInfo(
            name = "preferred_maximum_answer_words",
            defaultValue = "300"
    )
    private int preferredMaximumAnswerWords = 300;

    @ColumnInfo(
            name = "is_configured",
            defaultValue = "0"
    )
    private boolean configured;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public SchoolCurriculumProfileEntity() {
        // Required empty constructor for Room.
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId =
                Math.max(
                        0L,
                        profileId
                );
    }

    @NonNull
    public String getCurriculumId() {
        return curriculumId;
    }

    public void setCurriculumId(
            @NonNull String curriculumId
    ) {
        this.curriculumId =
                normalizeIdentifier(
                        curriculumId
                );
    }

    @NonNull
    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(
            @NonNull String schoolName
    ) {
        this.schoolName =
                normalizeText(
                        schoolName
                );
    }

    @NonNull
    public String getSchoolCode() {
        return schoolCode;
    }

    public void setSchoolCode(
            @NonNull String schoolCode
    ) {
        this.schoolCode =
                normalizeText(
                        schoolCode
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
                normalizeText(
                        educationBoard
                );
    }

    @NonNull
    public String getSchoolPattern() {
        return schoolPattern;
    }

    public void setSchoolPattern(
            @NonNull String schoolPattern
    ) {
        this.schoolPattern =
                normalizeText(
                        schoolPattern
                );
    }

    public int getClassNumber() {
        return classNumber;
    }

    public void setClassNumber(
            int classNumber
    ) {
        this.classNumber =
                Math.max(
                        1,
                        Math.min(
                                12,
                                classNumber
                        )
                );
    }

    @NonNull
    public String getSection() {
        return section;
    }

    public void setSection(
            @NonNull String section
    ) {
        this.section =
                normalizeText(
                        section
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
                normalizeText(
                        academicSession
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
                normalizeText(
                        studyMedium
                );
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

    @NonNull
    public String getAiDefaultLanguage() {
        return aiDefaultLanguage;
    }

    public void setAiDefaultLanguage(
            @NonNull String aiDefaultLanguage
    ) {
        this.aiDefaultLanguage =
                normalizeAiLanguage(
                        aiDefaultLanguage
                );
    }

    @NonNull
    public String getAiDefaultAnswerMode() {
        return aiDefaultAnswerMode;
    }

    public void setAiDefaultAnswerMode(
            @NonNull String aiDefaultAnswerMode
    ) {
        this.aiDefaultAnswerMode =
                normalizeAiAnswerMode(
                        aiDefaultAnswerMode
                );
    }

    public boolean isVoiceQuestionEnabled() {
        return voiceQuestionEnabled;
    }

    public void setVoiceQuestionEnabled(
            boolean voiceQuestionEnabled
    ) {
        this.voiceQuestionEnabled =
                voiceQuestionEnabled;
    }

    public boolean isImageQuestionEnabled() {
        return imageQuestionEnabled;
    }

    public void setImageQuestionEnabled(
            boolean imageQuestionEnabled
    ) {
        this.imageQuestionEnabled =
                imageQuestionEnabled;
    }

    public boolean isReadAnswerAloudEnabled() {
        return readAnswerAloudEnabled;
    }

    public void setReadAnswerAloudEnabled(
            boolean readAnswerAloudEnabled
    ) {
        this.readAnswerAloudEnabled =
                readAnswerAloudEnabled;
    }

    public boolean isChildSafeAnswersEnabled() {
        return childSafeAnswersEnabled;
    }

    public void setChildSafeAnswersEnabled(
            boolean childSafeAnswersEnabled
    ) {
        this.childSafeAnswersEnabled =
                childSafeAnswersEnabled;
    }

    public boolean isSaveDoubtHistoryEnabled() {
        return saveDoubtHistoryEnabled;
    }

    public void setSaveDoubtHistoryEnabled(
            boolean saveDoubtHistoryEnabled
    ) {
        this.saveDoubtHistoryEnabled =
                saveDoubtHistoryEnabled;
    }

    public int getPreferredMaximumAnswerWords() {
        return preferredMaximumAnswerWords;
    }

    public void setPreferredMaximumAnswerWords(
            int preferredMaximumAnswerWords
    ) {
        this.preferredMaximumAnswerWords =
                Math.max(
                        50,
                        Math.min(
                                1000,
                                preferredMaximumAnswerWords
                        )
                );
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(
            boolean configured
    ) {
        this.configured =
                configured;
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

    public void applyGradeSixDefaults() {
        classNumber =
                6;

        aiTutorEnabled =
                true;

        aiDefaultLanguage =
                "BILINGUAL";

        aiDefaultAnswerMode =
                "SIMPLE";

        voiceQuestionEnabled =
                true;

        imageQuestionEnabled =
                true;

        readAnswerAloudEnabled =
                true;

        childSafeAnswersEnabled =
                true;

        saveDoubtHistoryEnabled =
                true;

        preferredMaximumAnswerWords =
                300;
    }

    public boolean hasMinimumSchoolInformation() {
        return profileId > 0L
                && !curriculumId.isEmpty()
                && !schoolName.isEmpty()
                && !educationBoard.isEmpty()
                && classNumber >= 1
                && classNumber <= 12
                && !academicSession.isEmpty()
                && !studyMedium.isEmpty();
    }

    @NonNull
    private static String normalizeText(
            @NonNull String value
    ) {
        return value.trim();
    }

    @NonNull
    private static String normalizeIdentifier(
            @NonNull String value
    ) {
        return value
                .trim()
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9_-]",
                        "_"
                )
                .replaceAll(
                        "_+",
                        "_"
                );
    }

    @NonNull
    private static String normalizeAiLanguage(
            @NonNull String language
    ) {
        String normalizedLanguage =
                language.trim()
                        .toUpperCase();

        switch (normalizedLanguage) {
            case "HINDI":
            case "ENGLISH":
            case "BILINGUAL":
                return normalizedLanguage;

            default:
                return "BILINGUAL";
        }
    }

    @NonNull
    private static String normalizeAiAnswerMode(
            @NonNull String answerMode
    ) {
        String normalizedAnswerMode =
                answerMode.trim()
                        .toUpperCase()
                        .replace(
                                "-",
                                "_"
                        )
                        .replace(
                                " ",
                                "_"
                        );

        switch (normalizedAnswerMode) {
            case "SIMPLE":
            case "DETAILED":
            case "EXAM_ANSWER":
            case "STEP_BY_STEP":
            case "EXAMPLE_BASED":
            case "PRACTICE_MODE":
                return normalizedAnswerMode;

            default:
                return "SIMPLE";
        }
    }
}