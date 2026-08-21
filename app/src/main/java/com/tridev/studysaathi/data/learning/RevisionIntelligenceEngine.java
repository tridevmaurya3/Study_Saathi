package com.tridev.studysaathi.data.learning;

import androidx.annotation.NonNull;

/** Pure scheduling model for spaced repetition, forgetting risk and daily revision priority. */
public final class RevisionIntelligenceEngine {
    public static final class Result {
        private final int intervalDays;
        private final int forgettingRiskPercent;
        private final int priorityScore;
        private final boolean addToMistakeNotebook;

        private Result(int intervalDays, int forgettingRiskPercent, int priorityScore,
                       boolean addToMistakeNotebook) {
            this.intervalDays = intervalDays;
            this.forgettingRiskPercent = forgettingRiskPercent;
            this.priorityScore = priorityScore;
            this.addToMistakeNotebook = addToMistakeNotebook;
        }

        public int getIntervalDays() { return intervalDays; }
        public int getForgettingRiskPercent() { return forgettingRiskPercent; }
        public int getPriorityScore() { return priorityScore; }
        public boolean shouldAddToMistakeNotebook() { return addToMistakeNotebook; }
    }

    private RevisionIntelligenceEngine() { }

    @NonNull
    public static Result evaluate(int correctStreak, int wrongAttempts, int daysSinceReview,
                                  int confidencePercent) {
        int streak = clamp(correctStreak, 0, 20);
        int wrong = clamp(wrongAttempts, 0, 20);
        int days = clamp(daysSinceReview, 0, 365);
        int confidence = clamp(confidencePercent, 0, 100);

        int[] intervals = {1, 2, 4, 7, 14, 30, 60};
        int interval = wrong > 0 ? 1 : intervals[Math.min(streak, intervals.length - 1)];
        int timePressure = Math.min(70, (days * 70) / Math.max(1, interval));
        int risk = clamp(timePressure + (100 - confidence) / 3 + wrong * 8 - streak * 4, 0, 100);
        int priority = clamp(risk + wrong * 5 + (days >= interval ? 20 : 0), 0, 100);
        return new Result(interval, risk, priority, wrong > 0 || confidence < 45);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
