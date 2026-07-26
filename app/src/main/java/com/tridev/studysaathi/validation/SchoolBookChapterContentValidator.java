package com.tridev.studysaathi.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterContentEntity;

public final class SchoolBookChapterContentValidator {

    private static final int MINIMUM_EXPLANATION_LENGTH =
            40;

    private static final int MINIMUM_SUMMARY_LENGTH =
            20;

    private SchoolBookChapterContentValidator() {
        throw new AssertionError(
                "SchoolBookChapterContentValidator "
                        + "cannot be instantiated."
        );
    }

    @NonNull
    public static ValidationResult validateDraft(
            long chapterRowId,
            @Nullable SchoolBookChapterContentEntity content
    ) {
        if (chapterRowId <= 0L) {
            return ValidationResult.invalid(
                    "A valid exact chapter is required."
            );
        }

        if (content == null) {
            return ValidationResult.invalid(
                    "Chapter learning content is missing."
            );
        }

        if (content.getChapterRowId() > 0L
                && content.getChapterRowId()
                != chapterRowId) {

            return ValidationResult.invalid(
                    "This learning content belongs "
                            + "to another chapter."
            );
        }

        if (!isSupportedLanguageMode(
                content.getLanguageMode()
        )) {
            return ValidationResult.invalid(
                    "Select English, Hindi or Bilingual "
                            + "as the content language."
            );
        }

        if (!content.hasReadableContent()) {
            return ValidationResult.invalid(
                    "Add an introduction, explanation "
                            + "or chapter summary."
            );
        }

        return ValidationResult.valid();
    }

    @NonNull
    public static ValidationResult validateForReview(
            long chapterRowId,
            @Nullable SchoolBookChapterContentEntity content
    ) {
        ValidationResult draftResult =
                validateDraft(
                        chapterRowId,
                        content
                );

        if (!draftResult.isValid()) {
            return draftResult;
        }

        if (content == null) {
            return ValidationResult.invalid(
                    "Chapter learning content is missing."
            );
        }

        String languageMode =
                content.getLanguageMode();

        if (SchoolBookChapterContentEntity
                .LANGUAGE_MODE_ENGLISH.equals(
                        languageMode
                )) {

            return validateEnglishContent(
                    content
            );
        }

        if (SchoolBookChapterContentEntity
                .LANGUAGE_MODE_HINDI.equals(
                        languageMode
                )) {

            return validateHindiContent(
                    content
            );
        }

        ValidationResult englishResult =
                validateEnglishContent(
                        content
                );

        if (!englishResult.isValid()) {
            return ValidationResult.invalid(
                    "English content: "
                            + englishResult.getMessage()
            );
        }

        ValidationResult hindiResult =
                validateHindiContent(
                        content
                );

        if (!hindiResult.isValid()) {
            return ValidationResult.invalid(
                    "Hindi content: "
                            + hindiResult.getMessage()
            );
        }

        return ValidationResult.valid();
    }

    @NonNull
    public static ValidationResult validateForApproval(
            long chapterRowId,
            @Nullable SchoolBookChapterContentEntity content
    ) {
        ValidationResult reviewResult =
                validateForReview(
                        chapterRowId,
                        content
                );

        if (!reviewResult.isValid()) {
            return reviewResult;
        }

        if (content == null) {
            return ValidationResult.invalid(
                    "Chapter learning content is missing."
            );
        }

        if (!SchoolBookChapterContentEntity
                .REVIEW_STATUS_PENDING_REVIEW.equals(
                        content.getReviewStatus()
                )
                && !SchoolBookChapterContentEntity
                .REVIEW_STATUS_APPROVED.equals(
                        content.getReviewStatus()
                )) {

            return ValidationResult.invalid(
                    "Submit this content for Parent review "
                            + "before approving it."
            );
        }

        return ValidationResult.valid();
    }

    @NonNull
    private static ValidationResult validateEnglishContent(
            @NonNull SchoolBookChapterContentEntity content
    ) {
        if (content.getDetailedExplanationEnglish()
                .length()
                < MINIMUM_EXPLANATION_LENGTH) {

            return ValidationResult.invalid(
                    "Detailed explanation must contain "
                            + "at least "
                            + MINIMUM_EXPLANATION_LENGTH
                            + " characters."
            );
        }

        if (content.getChapterSummaryEnglish()
                .length()
                < MINIMUM_SUMMARY_LENGTH) {

            return ValidationResult.invalid(
                    "Chapter summary must contain "
                            + "at least "
                            + MINIMUM_SUMMARY_LENGTH
                            + " characters."
            );
        }

        if (content.getKeyPointsEnglish().isEmpty()) {
            return ValidationResult.invalid(
                    "Add at least one key point."
            );
        }

        return ValidationResult.valid();
    }

    @NonNull
    private static ValidationResult validateHindiContent(
            @NonNull SchoolBookChapterContentEntity content
    ) {
        if (content.getDetailedExplanationHindi()
                .length()
                < MINIMUM_EXPLANATION_LENGTH) {

            return ValidationResult.invalid(
                    "विस्तृत व्याख्या कम-से-कम "
                            + MINIMUM_EXPLANATION_LENGTH
                            + " अक्षरों की होनी चाहिए।"
            );
        }

        if (content.getChapterSummaryHindi()
                .length()
                < MINIMUM_SUMMARY_LENGTH) {

            return ValidationResult.invalid(
                    "अध्याय सारांश कम-से-कम "
                            + MINIMUM_SUMMARY_LENGTH
                            + " अक्षरों का होना चाहिए।"
            );
        }

        if (content.getKeyPointsHindi().isEmpty()) {
            return ValidationResult.invalid(
                    "कम-से-कम एक मुख्य बिंदु जोड़ें।"
            );
        }

        return ValidationResult.valid();
    }

    private static boolean isSupportedLanguageMode(
            @Nullable String languageMode
    ) {
        return SchoolBookChapterContentEntity
                .LANGUAGE_MODE_ENGLISH.equals(
                        languageMode
                )
                || SchoolBookChapterContentEntity
                .LANGUAGE_MODE_HINDI.equals(
                        languageMode
                )
                || SchoolBookChapterContentEntity
                .LANGUAGE_MODE_BILINGUAL.equals(
                        languageMode
                );
    }

    public static final class ValidationResult {

        private final boolean valid;

        @NonNull
        private final String message;

        private ValidationResult(
                boolean valid,
                @NonNull String message
        ) {
            this.valid =
                    valid;

            this.message =
                    message.trim();
        }

        @NonNull
        private static ValidationResult valid() {
            return new ValidationResult(
                    true,
                    ""
            );
        }

        @NonNull
        private static ValidationResult invalid(
                @NonNull String message
        ) {
            return new ValidationResult(
                    false,
                    message
            );
        }

        public boolean isValid() {
            return valid;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }
}