package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class ChapterNoteItem {

    @NonNull
    private final String subjectName;

    @NonNull
    private final String chapterTitle;

    @NonNull
    private final String noteText;

    public ChapterNoteItem(
            @NonNull String subjectName,
            @NonNull String chapterTitle,
            @NonNull String noteText
    ) {
        this.subjectName = subjectName;
        this.chapterTitle = chapterTitle;
        this.noteText = noteText;
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
    public String getNoteText() {
        return noteText;
    }
}