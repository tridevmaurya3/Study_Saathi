package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class BookmarkItem {

    @NonNull
    private final String subjectName;

    @NonNull
    private final String chapterTitle;

    @NonNull
    private final String chapterDescription;

    public BookmarkItem(
            @NonNull String subjectName,
            @NonNull String chapterTitle,
            @NonNull String chapterDescription
    ) {
        this.subjectName = subjectName;
        this.chapterTitle = chapterTitle;
        this.chapterDescription = chapterDescription;
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
    public String getChapterDescription() {
        return chapterDescription;
    }
}