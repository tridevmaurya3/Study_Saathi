package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

import java.time.LocalDate;

public class RevisionItem {

    public enum RevisionStatus {
        DUE_TODAY,
        OVERDUE,
        UPCOMING
    }

    @NonNull
    private final String educationBoard;

    @NonNull
    private final String studentClass;

    @NonNull
    private final String subjectName;

    @NonNull
    private final String chapterTitle;

    @NonNull
    private final LocalDate nextRevisionDate;

    @NonNull
    private final RevisionStatus revisionStatus;

    public RevisionItem(
            @NonNull String educationBoard,
            @NonNull String studentClass,
            @NonNull String subjectName,
            @NonNull String chapterTitle,
            @NonNull LocalDate nextRevisionDate,
            @NonNull RevisionStatus revisionStatus
    ) {
        this.educationBoard = educationBoard;
        this.studentClass = studentClass;
        this.subjectName = subjectName;
        this.chapterTitle = chapterTitle;
        this.nextRevisionDate = nextRevisionDate;
        this.revisionStatus = revisionStatus;
    }

    @NonNull
    public String getEducationBoard() {
        return educationBoard;
    }

    @NonNull
    public String getStudentClass() {
        return studentClass;
    }

    @NonNull
    public String getSubjectName() {
        return subjectName;
    }

    @NonNull
    public String getChapterTitle() {
        return chapterTitle;
    }

    @NonNull
    public LocalDate getNextRevisionDate() {
        return nextRevisionDate;
    }

    @NonNull
    public RevisionStatus getRevisionStatus() {
        return revisionStatus;
    }
}