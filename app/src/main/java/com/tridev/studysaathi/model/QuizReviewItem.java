package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

public class QuizReviewItem {

    private final int questionNumber;

    @NonNull
    private final String questionText;

    @NonNull
    private final String selectedAnswer;

    @NonNull
    private final String correctAnswer;

    @NonNull
    private final String explanation;

    private final boolean correct;

    public QuizReviewItem(
            int questionNumber,
            @NonNull String questionText,
            @NonNull String selectedAnswer,
            @NonNull String correctAnswer,
            @NonNull String explanation,
            boolean correct
    ) {
        this.questionNumber = questionNumber;
        this.questionText = questionText;
        this.selectedAnswer = selectedAnswer;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.correct = correct;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    @NonNull
    public String getQuestionText() {
        return questionText;
    }

    @NonNull
    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    @NonNull
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    @NonNull
    public String getExplanation() {
        return explanation;
    }

    public boolean isCorrect() {
        return correct;
    }
}