package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class ChapterItem {

    private final int chapterNumber;

    @NonNull
    private final String chapterTitle;

    @NonNull
    private final String chapterDescription;

    private final int lessonCount;
    private int progressPercent;

    public ChapterItem(
            int chapterNumber,
            @NonNull String chapterTitle,
            @NonNull String chapterDescription,
            int lessonCount,
            int progressPercent
    ) {
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
        this.chapterDescription = chapterDescription;
        this.lessonCount = lessonCount;

        setProgressPercent(progressPercent);
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    @NonNull
    public String getChapterTitle() {
        return chapterTitle;
    }

    @NonNull
    public String getChapterDescription() {
        return chapterDescription;
    }

    public int getLessonCount() {
        return lessonCount;
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

    @NonNull
    public ChapterItem copyWithProgress(
            int updatedProgressPercent
    ) {
        return new ChapterItem(
                chapterNumber,
                chapterTitle,
                chapterDescription,
                lessonCount,
                updatedProgressPercent
        );
    }
}