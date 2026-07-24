package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class ParentProfileSummary {

    private final long profileId;

    @NonNull
    private final String studentName;

    @NonNull
    private final String educationBoard;

    @NonNull
    private final String studentClass;

    private final boolean activeProfile;

    private final int completedLessons;
    private final int revisionCount;
    private final int quizAttemptCount;
    private final int latestQuizScore;
    private final int bestQuizScore;
    private final int currentStreak;

    private final int todayActions;
    private final int dailyGoal;

    private final long lastActivityAt;

    @NonNull
    private final String weakSubject;

    @NonNull
    private final String weakChapter;

    private final int weakChapterScore;

    public ParentProfileSummary(
            long profileId,
            @NonNull String studentName,
            @NonNull String educationBoard,
            @NonNull String studentClass,
            boolean activeProfile,
            int completedLessons,
            int revisionCount,
            int quizAttemptCount,
            int latestQuizScore,
            int bestQuizScore,
            int currentStreak,
            int todayActions,
            int dailyGoal,
            long lastActivityAt,
            @NonNull String weakSubject,
            @NonNull String weakChapter,
            int weakChapterScore
    ) {
        this.profileId = profileId;
        this.studentName = studentName;
        this.educationBoard = educationBoard;
        this.studentClass = studentClass;
        this.activeProfile = activeProfile;
        this.completedLessons = completedLessons;
        this.revisionCount = revisionCount;
        this.quizAttemptCount = quizAttemptCount;
        this.latestQuizScore = latestQuizScore;
        this.bestQuizScore = bestQuizScore;
        this.currentStreak = currentStreak;
        this.todayActions = todayActions;
        this.dailyGoal = dailyGoal;
        this.lastActivityAt = lastActivityAt;
        this.weakSubject = weakSubject;
        this.weakChapter = weakChapter;
        this.weakChapterScore = weakChapterScore;
    }

    public long getProfileId() {
        return profileId;
    }

    @NonNull
    public String getStudentName() {
        return studentName;
    }

    @NonNull
    public String getEducationBoard() {
        return educationBoard;
    }

    @NonNull
    public String getStudentClass() {
        return studentClass;
    }

    public boolean isActiveProfile() {
        return activeProfile;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public int getQuizAttemptCount() {
        return quizAttemptCount;
    }

    public int getLatestQuizScore() {
        return latestQuizScore;
    }

    public int getBestQuizScore() {
        return bestQuizScore;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getTodayActions() {
        return todayActions;
    }

    public int getDailyGoal() {
        return dailyGoal;
    }

    public long getLastActivityAt() {
        return lastActivityAt;
    }

    @NonNull
    public String getWeakSubject() {
        return weakSubject;
    }

    @NonNull
    public String getWeakChapter() {
        return weakChapter;
    }

    public int getWeakChapterScore() {
        return weakChapterScore;
    }

    public boolean hasQuizData() {
        return quizAttemptCount > 0
                && bestQuizScore >= 0;
    }

    public boolean hasWeakChapter() {
        return weakChapterScore >= 0
                && !weakChapter.isEmpty();
    }
}