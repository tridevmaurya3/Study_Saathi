package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class SubjectProgressItem {

    @NonNull
    private final String subjectName;

    private final int completedLessons;
    private final int totalLessons;
    private final int revisionCount;
    private final int quizAttempts;
    private final int averageQuizScore;
    private final int completionPercent;

    public SubjectProgressItem(
            @NonNull String subjectName,
            int completedLessons,
            int totalLessons,
            int revisionCount,
            int quizAttempts,
            int averageQuizScore
    ) {
        this.subjectName = subjectName;

        this.completedLessons = Math.max(
                0,
                completedLessons
        );

        this.totalLessons = Math.max(
                0,
                totalLessons
        );

        this.revisionCount = Math.max(
                0,
                revisionCount
        );

        this.quizAttempts = Math.max(
                0,
                quizAttempts
        );

        this.averageQuizScore = Math.max(
                0,
                Math.min(100, averageQuizScore)
        );

        if (this.totalLessons <= 0) {
            completionPercent = 0;
        } else {
            completionPercent = Math.max(
                    0,
                    Math.min(
                            100,
                            Math.round(
                                    this.completedLessons
                                            * 100f
                                            / this.totalLessons
                            )
                    )
            );
        }
    }

    @NonNull
    public String getSubjectName() {
        return subjectName;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public int getQuizAttempts() {
        return quizAttempts;
    }

    public int getAverageQuizScore() {
        return averageQuizScore;
    }

    public int getCompletionPercent() {
        return completionPercent;
    }

    public boolean hasLearningActivity() {
        return completedLessons > 0
                || revisionCount > 0
                || quizAttempts > 0;
    }
}