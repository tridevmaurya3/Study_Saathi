package com.tridev.studysaathi.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "doubt_history",
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
public class DoubtHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "history_id")
    private long historyId;

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

    @NonNull
    @ColumnInfo(name = "question_text")
    private String questionText = "";

    @NonNull
    @ColumnInfo(name = "answer_text")
    private String answerText = "";

    @NonNull
    @ColumnInfo(name = "explanation_language")
    private String explanationLanguage = "";

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public DoubtHistoryEntity() {
        // Required empty constructor for Room.
    }

    public long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(long historyId) {
        this.historyId = historyId;
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

    @NonNull
    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(
            @NonNull String questionText
    ) {
        this.questionText = questionText;
    }

    @NonNull
    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(
            @NonNull String answerText
    ) {
        this.answerText = answerText;
    }

    @NonNull
    public String getExplanationLanguage() {
        return explanationLanguage;
    }

    public void setExplanationLanguage(
            @NonNull String explanationLanguage
    ) {
        this.explanationLanguage = explanationLanguage;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}