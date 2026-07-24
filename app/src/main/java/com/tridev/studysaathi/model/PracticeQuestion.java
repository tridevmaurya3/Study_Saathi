package com.tridev.studysaathi.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PracticeQuestion {

    @NonNull
    private final String englishQuestion;

    @NonNull
    private final String hindiQuestion;

    @NonNull
    private final List<String> englishOptions;

    @NonNull
    private final List<String> hindiOptions;

    private final int correctOptionIndex;

    @NonNull
    private final String englishExplanation;

    @NonNull
    private final String hindiExplanation;

    public PracticeQuestion(
            @NonNull String englishQuestion,
            @NonNull String hindiQuestion,
            @NonNull List<String> englishOptions,
            @NonNull List<String> hindiOptions,
            int correctOptionIndex,
            @NonNull String englishExplanation,
            @NonNull String hindiExplanation
    ) {
        if (englishOptions.size() != 4
                || hindiOptions.size() != 4) {
            throw new IllegalArgumentException(
                    "Each practice question must have exactly four options."
            );
        }

        if (correctOptionIndex < 0
                || correctOptionIndex > 3) {
            throw new IllegalArgumentException(
                    "Correct option index must be between 0 and 3."
            );
        }

        this.englishQuestion = englishQuestion;
        this.hindiQuestion = hindiQuestion;

        this.englishOptions = Collections.unmodifiableList(
                new ArrayList<>(englishOptions)
        );

        this.hindiOptions = Collections.unmodifiableList(
                new ArrayList<>(hindiOptions)
        );

        this.correctOptionIndex = correctOptionIndex;
        this.englishExplanation = englishExplanation;
        this.hindiExplanation = hindiExplanation;
    }

    @NonNull
    public String getEnglishQuestion() {
        return englishQuestion;
    }

    @NonNull
    public String getHindiQuestion() {
        return hindiQuestion;
    }

    @NonNull
    public List<String> getEnglishOptions() {
        return englishOptions;
    }

    @NonNull
    public List<String> getHindiOptions() {
        return hindiOptions;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    @NonNull
    public String getEnglishExplanation() {
        return englishExplanation;
    }

    @NonNull
    public String getHindiExplanation() {
        return hindiExplanation;
    }
}