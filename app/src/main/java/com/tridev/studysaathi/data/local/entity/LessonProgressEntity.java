package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Locale;

@Entity(
        tableName = "lesson_progress",
        indices = {
                @Index(value = {"profile_id"}),
                @Index(
                        value = {
                                "profile_id",
                                "subject_name",
                                "chapter_title"
                        }
                )
        }
)
public class LessonProgressEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "progress_key")
    private String progressKey = "";

    @ColumnInfo(name = "profile_id")
    private long profileId;

    @NonNull
    @ColumnInfo(name = "education_board")
    private String educationBoard = "";

    @NonNull
    @ColumnInfo(name = "student_class")
    private String studentClass = "";

    @NonNull
    @ColumnInfo(name = "subject_name")
    private String subjectName = "";

    @NonNull
    @ColumnInfo(name = "chapter_title")
    private String chapterTitle = "";

    @ColumnInfo(name = "progress_percent")
    private int progressPercent;

    @ColumnInfo(name = "is_completed")
    private boolean completed;

    @ColumnInfo(name = "last_studied_at")
    private long lastStudiedAt;

    @ColumnInfo(name = "completed_at")
    private long completedAt;

    @ColumnInfo(
            name = "revision_count",
            defaultValue = "0"
    )
    private int revisionCount;

    @ColumnInfo(
            name = "last_revised_at",
            defaultValue = "0"
    )
    private long lastRevisedAt;

    public LessonProgressEntity() {
        // Required empty constructor for Room.
    }

    @NonNull
    public static String createProgressKey(
            long profileId,
            String educationBoard,
            String studentClass,
            String subjectName,
            String chapterTitle
    ) {
        return profileId
                + "|"
                + normalizeKeyPart(educationBoard)
                + "|"
                + normalizeKeyPart(studentClass)
                + "|"
                + normalizeKeyPart(subjectName)
                + "|"
                + normalizeKeyPart(chapterTitle);
    }

    @NonNull
    private static String normalizeKeyPart(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("|", "_")
                .replaceAll("\\s+", "_");
    }

    @NonNull
    public String getProgressKey() {
        return progressKey;
    }

    public void setProgressKey(
            @NonNull String progressKey
    ) {
        this.progressKey = progressKey;
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    @NonNull
    public String getEducationBoard() {
        return educationBoard;
    }

    public void setEducationBoard(
            @NonNull String educationBoard
    ) {
        this.educationBoard = educationBoard;
    }

    @NonNull
    public String getStudentClass() {
        return studentClass;
    }

    public void setStudentClass(
            @NonNull String studentClass
    ) {
        this.studentClass = studentClass;
    }

    @NonNull
    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(
            @NonNull String subjectName
    ) {
        this.subjectName = subjectName;
    }

    @NonNull
    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(
            @NonNull String chapterTitle
    ) {
        this.chapterTitle = chapterTitle;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(
            int progressPercent
    ) {
        this.progressPercent = Math.max(
                0,
                Math.min(100, progressPercent)
        );
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getLastStudiedAt() {
        return lastStudiedAt;
    }

    public void setLastStudiedAt(
            long lastStudiedAt
    ) {
        this.lastStudiedAt = lastStudiedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public void setRevisionCount(
            int revisionCount
    ) {
        this.revisionCount = Math.max(
                0,
                revisionCount
        );
    }

    public long getLastRevisedAt() {
        return lastRevisedAt;
    }

    public void setLastRevisedAt(
            long lastRevisedAt
    ) {
        this.lastRevisedAt = lastRevisedAt;
    }
}