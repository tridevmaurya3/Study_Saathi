package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class StudyRecommendation {

    public enum RecommendationType {
        LOW_QUIZ_SCORE,
        PRACTICE_AFTER_LESSON,
        NEXT_LESSON,
        SMART_REVISION,
        NO_CONTENT
    }

    @NonNull
    private final String subjectName;

    @NonNull
    private final String chapterTitle;

    @NonNull
    private final String chapterDescription;

    @NonNull
    private final RecommendationType recommendationType;

    private final int quizAverageScore;

    public StudyRecommendation(
            @NonNull String subjectName,
            @NonNull String chapterTitle,
            @NonNull String chapterDescription,
            @NonNull RecommendationType recommendationType,
            int quizAverageScore
    ) {
        this.subjectName = subjectName;
        this.chapterTitle = chapterTitle;
        this.chapterDescription = chapterDescription;
        this.recommendationType = recommendationType;

        this.quizAverageScore = Math.max(
                0,
                Math.min(100, quizAverageScore)
        );
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

    @NonNull
    public RecommendationType getRecommendationType() {
        return recommendationType;
    }

    public int getQuizAverageScore() {
        return quizAverageScore;
    }

    public boolean hasRecommendation() {
        return recommendationType
                != RecommendationType.NO_CONTENT
                && !subjectName.trim().isEmpty()
                && !chapterTitle.trim().isEmpty();
    }

    @NonNull
    public static StudyRecommendation empty() {
        return new StudyRecommendation(
                "",
                "",
                "",
                RecommendationType.NO_CONTENT,
                0
        );
    }
}