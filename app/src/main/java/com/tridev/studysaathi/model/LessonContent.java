package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class LessonContent {

    @NonNull
    private final String lessonTitle;

    @NonNull
    private final String englishExplanation;

    @NonNull
    private final String hindiExplanation;

    @NonNull
    private final String englishKeyPoints;

    @NonNull
    private final String hindiKeyPoints;

    @NonNull
    private final String englishExample;

    @NonNull
    private final String hindiExample;

    @NonNull
    private final String englishPracticeQuestion;

    @NonNull
    private final String hindiPracticeQuestion;

    public LessonContent(
            @NonNull String lessonTitle,
            @NonNull String englishExplanation,
            @NonNull String hindiExplanation,
            @NonNull String englishKeyPoints,
            @NonNull String hindiKeyPoints,
            @NonNull String englishExample,
            @NonNull String hindiExample,
            @NonNull String englishPracticeQuestion,
            @NonNull String hindiPracticeQuestion
    ) {
        this.lessonTitle = lessonTitle;
        this.englishExplanation = englishExplanation;
        this.hindiExplanation = hindiExplanation;
        this.englishKeyPoints = englishKeyPoints;
        this.hindiKeyPoints = hindiKeyPoints;
        this.englishExample = englishExample;
        this.hindiExample = hindiExample;
        this.englishPracticeQuestion = englishPracticeQuestion;
        this.hindiPracticeQuestion = hindiPracticeQuestion;
    }

    @NonNull
    public String getLessonTitle() {
        return lessonTitle;
    }

    @NonNull
    public String getEnglishExplanation() {
        return englishExplanation;
    }

    @NonNull
    public String getHindiExplanation() {
        return hindiExplanation;
    }

    @NonNull
    public String getEnglishKeyPoints() {
        return englishKeyPoints;
    }

    @NonNull
    public String getHindiKeyPoints() {
        return hindiKeyPoints;
    }

    @NonNull
    public String getEnglishExample() {
        return englishExample;
    }

    @NonNull
    public String getHindiExample() {
        return hindiExample;
    }

    @NonNull
    public String getEnglishPracticeQuestion() {
        return englishPracticeQuestion;
    }

    @NonNull
    public String getHindiPracticeQuestion() {
        return hindiPracticeQuestion;
    }
}