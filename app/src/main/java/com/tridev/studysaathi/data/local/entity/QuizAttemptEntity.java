package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "quiz_attempts",
        indices = {
                @Index(value = {"profile_id"}),
                @Index(
                        value = {
                                "profile_id",
                                "education_board",
                                "student_class",
                                "subject_name",
                                "chapter_title"
                        }
                )
        }
)
public class QuizAttemptEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attempt_id")
    private long attemptId;

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

    @ColumnInfo(name = "correct_answers")
    private int correctAnswers;

    @ColumnInfo(name = "total_questions")
    private int totalQuestions;

    @ColumnInfo(name = "percentage")
    private int percentage;

    @ColumnInfo(name = "attempted_at")
    private long attemptedAt;

    public QuizAttemptEntity() {
        // Required empty constructor for Room.
    }

    public long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(long attemptId) {
        this.attemptId = attemptId;
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

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = Math.max(
                0,
                correctAnswers
        );
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = Math.max(
                0,
                totalQuestions
        );
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = Math.max(
                0,
                Math.min(100, percentage)
        );
    }

    public long getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(long attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}