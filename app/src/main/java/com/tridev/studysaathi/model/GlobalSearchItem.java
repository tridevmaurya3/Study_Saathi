package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class GlobalSearchItem {

    public enum ResultType {
        SUBJECT,
        CHAPTER,
        NOTE
    }

    @NonNull
    private final ResultType resultType;

    @NonNull
    private final String title;

    @NonNull
    private final String subtitle;

    @NonNull
    private final String description;

    @NonNull
    private final String subjectName;

    @NonNull
    private final String chapterTitle;

    @NonNull
    private final String chapterDescription;

    public GlobalSearchItem(
            @NonNull ResultType resultType,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String description,
            @NonNull String subjectName,
            @NonNull String chapterTitle,
            @NonNull String chapterDescription
    ) {
        this.resultType = resultType;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.subjectName = subjectName;
        this.chapterTitle = chapterTitle;
        this.chapterDescription = chapterDescription;
    }

    @NonNull
    public ResultType getResultType() {
        return resultType;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getSubtitle() {
        return subtitle;
    }

    @NonNull
    public String getDescription() {
        return description;
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