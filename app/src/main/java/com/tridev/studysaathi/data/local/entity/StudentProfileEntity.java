package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Locale;

@Entity(tableName = "student_profiles")
public class StudentProfileEntity {

    public static final String STUDY_MEDIUM_ENGLISH =
            "English Medium";
    public static final String STUDY_MEDIUM_HINDI =
            "Hindi Medium";
    public static final String STUDY_MEDIUM_OTHER =
            "Other Medium";

    public static final String EXPLANATION_LANGUAGE_ENGLISH =
            "English";
    public static final String EXPLANATION_LANGUAGE_HINDI =
            "Hindi";
    public static final String EXPLANATION_LANGUAGE_HINGLISH =
            "Hinglish";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "profile_id")
    private long profileId;

    @NonNull
    @ColumnInfo(name = "student_name")
    private String studentName = "";

    @NonNull
    @ColumnInfo(name = "education_board")
    private String educationBoard = "";

    @NonNull
    @ColumnInfo(name = "student_class")
    private String studentClass = "";

    @NonNull
    @ColumnInfo(name = "study_medium")
    private String studyMedium = "";

    @NonNull
    @ColumnInfo(name = "explanation_language")
    private String explanationLanguage = "";

    @ColumnInfo(name = "is_active")
    private boolean active;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public StudentProfileEntity() {
        // Required empty constructor for Room.
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    @NonNull
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(@NonNull String studentName) {
        this.studentName = studentName;
    }

    @NonNull
    public String getEducationBoard() {
        return educationBoard;
    }

    public void setEducationBoard(@NonNull String educationBoard) {
        this.educationBoard = educationBoard;
    }

    @NonNull
    public String getStudentClass() {
        return studentClass;
    }

    public void setStudentClass(@NonNull String studentClass) {
        this.studentClass = studentClass;
    }

    @NonNull
    public String getStudyMedium() {
        return normalizeStudyMedium(studyMedium);
    }

    public void setStudyMedium(@NonNull String studyMedium) {
        this.studyMedium = normalizeStudyMedium(studyMedium);
    }

    @NonNull
    public String getExplanationLanguage() {
        return normalizeExplanationLanguage(explanationLanguage);
    }

    public void setExplanationLanguage(@NonNull String explanationLanguage) {
        this.explanationLanguage = normalizeExplanationLanguage(explanationLanguage);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @NonNull
    public static String normalizeStudyMedium(String value) {
        String safeValue = safeText(value);
        if (safeValue.isEmpty()) {
            return "";
        }

        String normalized = safeValue.toLowerCase(Locale.ROOT);
        if (normalized.equals("english medium")
                || normalized.equals("english")
                || safeValue.equals("अंग्रेज़ी माध्यम")
                || safeValue.equals("अंग्रेजी माध्यम")) {
            return STUDY_MEDIUM_ENGLISH;
        }
        if (normalized.equals("hindi medium")
                || normalized.equals("hindi")
                || safeValue.equals("हिन्दी माध्यम")
                || safeValue.equals("हिंदी माध्यम")) {
            return STUDY_MEDIUM_HINDI;
        }
        if (normalized.equals("other medium")
                || normalized.equals("other")
                || safeValue.equals("अन्य माध्यम")) {
            return STUDY_MEDIUM_OTHER;
        }
        return safeValue;
    }

    @NonNull
    public static String normalizeExplanationLanguage(String value) {
        String safeValue = safeText(value);
        if (safeValue.isEmpty()) {
            return "";
        }

        String normalized = safeValue.toLowerCase(Locale.ROOT);
        if (normalized.equals("english") || normalized.equals("en")) {
            return EXPLANATION_LANGUAGE_ENGLISH;
        }
        if (normalized.equals("hindi")
                || normalized.equals("hi")
                || safeValue.equals("हिन्दी")
                || safeValue.equals("हिंदी")) {
            return EXPLANATION_LANGUAGE_HINDI;
        }
        if (normalized.equals("hinglish")
                || normalized.equals("bilingual")
                || normalized.equals("hindi + english")
                || normalized.equals("hindi+english")
                || normalized.equals("hi,en")) {
            return EXPLANATION_LANGUAGE_HINGLISH;
        }

        // Preserve unknown future/custom values instead of guessing.
        return safeValue;
    }

    @NonNull
    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
