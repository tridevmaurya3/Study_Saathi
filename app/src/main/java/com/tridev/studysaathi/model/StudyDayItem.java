package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class StudyDayItem {

    @NonNull
    private final String dayName;

    @NonNull
    private final String dateText;

    private final int lessonCount;
    private final int revisionCount;
    private final int quizCount;
    private final int dailyGoal;

    private final boolean today;

    public StudyDayItem(
            @NonNull String dayName,
            @NonNull String dateText,
            int lessonCount,
            int revisionCount,
            int quizCount,
            int dailyGoal,
            boolean today
    ) {
        this.dayName = dayName;
        this.dateText = dateText;
        this.lessonCount = lessonCount;
        this.revisionCount = revisionCount;
        this.quizCount = quizCount;
        this.dailyGoal = dailyGoal;
        this.today = today;
    }

    @NonNull
    public String getDayName() {
        return dayName;
    }

    @NonNull
    public String getDateText() {
        return dateText;
    }

    public int getLessonCount() {
        return lessonCount;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public int getQuizCount() {
        return quizCount;
    }

    public int getDailyGoal() {
        return dailyGoal;
    }

    public boolean isToday() {
        return today;
    }

    public int getTotalActions() {
        return lessonCount
                + revisionCount
                + quizCount;
    }

    public boolean isGoalCompleted() {
        return getTotalActions() >= dailyGoal;
    }

    public boolean hasStudyActivity() {
        return getTotalActions() > 0;
    }
}