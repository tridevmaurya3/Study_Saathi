package com.tridev.studysaathi.model;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

public class SubjectItem {

    @NonNull
    private final String subjectName;

    @NonNull
    private final String subjectDescription;

    @NonNull
    private final String iconText;

    @ColorRes
    private final int backgroundColorRes;

    @ColorRes
    private final int borderColorRes;

    @ColorRes
    private final int accentColorRes;

    public SubjectItem(
            @NonNull String subjectName,
            @NonNull String subjectDescription,
            @NonNull String iconText,
            @ColorRes int backgroundColorRes,
            @ColorRes int borderColorRes,
            @ColorRes int accentColorRes
    ) {
        this.subjectName = subjectName;
        this.subjectDescription = subjectDescription;
        this.iconText = iconText;
        this.backgroundColorRes = backgroundColorRes;
        this.borderColorRes = borderColorRes;
        this.accentColorRes = accentColorRes;
    }

    @NonNull
    public String getSubjectName() {
        return subjectName;
    }

    @NonNull
    public String getSubjectDescription() {
        return subjectDescription;
    }

    @NonNull
    public String getIconText() {
        return iconText;
    }

    @ColorRes
    public int getBackgroundColorRes() {
        return backgroundColorRes;
    }

    @ColorRes
    public int getBorderColorRes() {
        return borderColorRes;
    }

    @ColorRes
    public int getAccentColorRes() {
        return accentColorRes;
    }
}