package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "student_profiles")
public class StudentProfileEntity {

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
        return studyMedium;
    }

    public void setStudyMedium(@NonNull String studyMedium) {
        this.studyMedium = studyMedium;
    }

    @NonNull
    public String getExplanationLanguage() {
        return explanationLanguage;
    }

    public void setExplanationLanguage(@NonNull String explanationLanguage) {
        this.explanationLanguage = explanationLanguage;
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
}