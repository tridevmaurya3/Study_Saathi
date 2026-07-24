package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class AchievementItem {

    @NonNull
    private final String iconText;

    @NonNull
    private final String title;

    @NonNull
    private final String description;

    private final boolean unlocked;
    private final int currentValue;
    private final int targetValue;

    public AchievementItem(
            @NonNull String iconText,
            @NonNull String title,
            @NonNull String description,
            boolean unlocked,
            int currentValue,
            int targetValue
    ) {
        this.iconText = iconText;
        this.title = title;
        this.description = description;
        this.unlocked = unlocked;

        this.currentValue = Math.max(
                0,
                currentValue
        );

        this.targetValue = Math.max(
                1,
                targetValue
        );
    }

    @NonNull
    public String getIconText() {
        return iconText;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public int getProgressPercent() {
        return Math.max(
                0,
                Math.min(
                        100,
                        Math.round(
                                currentValue
                                        * 100f
                                        / targetValue
                        )
                )
        );
    }
}