package com.tridev.studysaathi.data.content.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.entity.SchoolBookChapterEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SchoolBookChapterFormValidator {

    public static final int MAX_CHAPTER_NUMBER_LENGTH = 40;

    public static final int MAX_TITLE_LENGTH = 200;

    public static final int MAX_SUBTITLE_LENGTH = 250;

    public static final int MAX_UNIT_NAME_LENGTH = 150;

    public static final int MAX_DESCRIPTION_LENGTH = 3000;

    public static final int MAX_LEARNING_OBJECTIVES_LENGTH = 3000;

    public static final int MAX_IMPORTANT_TOPICS_LENGTH = 3000;

    /*
     * यह list SchoolBookChapterEntity के supported chapter types
     * से exactly synchronized रहनी चाहिए।
     */
    @NonNull
    private static final Set<String> SUPPORTED_CHAPTER_TYPES =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    SchoolBookChapterEntity
                                            .CHAPTER_TYPE_CHAPTER,

                                    SchoolBookChapterEntity
                                            .CHAPTER_TYPE_UNIT,

                                    SchoolBookChapterEntity
                                            .CHAPTER_TYPE_LESSON,

                                    SchoolBookChapterEntity
                                            .CHAPTER_TYPE_POEM,

                                    SchoolBookChapterEntity
                                            .CHAPTER_TYPE_STORY,

                                    SchoolBookChapterEntity
                                            .CHAPTER_TYPE_ACTIVITY,

                                    SchoolBookChapterEntity
                                            .CHAPTER_TYPE_PROJECT,

                                    SchoolBookChapterEntity
                                            .CHAPTER_TYPE_APPENDIX
                            )
                    )
            );

    private SchoolBookChapterFormValidator() {
        // Utility class.
    }

    /**
     * Manual chapter form की सभी values validate करता है।
     */
    @NonNull
    public static ValidationResult validate(
            long bookRowId,
            @Nullable String chapterNumber,
            @Nullable String chapterTitleEnglish,
            @Nullable String chapterTitleHindi,
            @Nullable String chapterSubtitle,
            @Nullable String unitName,
            @Nullable String chapterType,
            @Nullable String startPageNumber,
            @Nullable String endPageNumber,
            @Nullable String chapterDescription,
            @Nullable String learningObjectives,
            @Nullable String importantTopics
    ) {
        if (bookRowId <= 0L) {
            return ValidationResult.error(
                    Field.BOOK,
                    "A valid school book is required."
            );
        }

        String safeChapterNumber =
                safeText(
                        chapterNumber
                );

        String safeEnglishTitle =
                safeText(
                        chapterTitleEnglish
                );

        String safeHindiTitle =
                safeText(
                        chapterTitleHindi
                );

        String safeSubtitle =
                safeText(
                        chapterSubtitle
                );

        String safeUnitName =
                safeText(
                        unitName
                );

        String safeChapterType =
                normalizeChapterType(
                        chapterType
                );

        String safeDescription =
                safeText(
                        chapterDescription
                );

        String safeLearningObjectives =
                safeText(
                        learningObjectives
                );

        String safeImportantTopics =
                safeText(
                        importantTopics
                );

        if (safeEnglishTitle.isEmpty()
                && safeHindiTitle.isEmpty()) {

            return ValidationResult.error(
                    Field.CHAPTER_TITLE,
                    "Chapter title is required in English or Hindi."
            );
        }

        if (safeChapterNumber.length()
                > MAX_CHAPTER_NUMBER_LENGTH) {

            return ValidationResult.error(
                    Field.CHAPTER_NUMBER,
                    "Chapter number is too long."
            );
        }

        if (safeEnglishTitle.length()
                > MAX_TITLE_LENGTH) {

            return ValidationResult.error(
                    Field.CHAPTER_TITLE_ENGLISH,
                    "English chapter title is too long."
            );
        }

        if (safeHindiTitle.length()
                > MAX_TITLE_LENGTH) {

            return ValidationResult.error(
                    Field.CHAPTER_TITLE_HINDI,
                    "Hindi chapter title is too long."
            );
        }

        if (safeSubtitle.length()
                > MAX_SUBTITLE_LENGTH) {

            return ValidationResult.error(
                    Field.CHAPTER_SUBTITLE,
                    "Chapter subtitle is too long."
            );
        }

        if (safeUnitName.length()
                > MAX_UNIT_NAME_LENGTH) {

            return ValidationResult.error(
                    Field.UNIT_NAME,
                    "Unit name is too long."
            );
        }

        if (!SUPPORTED_CHAPTER_TYPES.contains(
                safeChapterType
        )) {
            return ValidationResult.error(
                    Field.CHAPTER_TYPE,
                    "Select a valid chapter type."
            );
        }

        PageNumberResult startPageResult =
                parsePageNumber(
                        startPageNumber,
                        Field.START_PAGE
                );

        if (!startPageResult.isValid()) {
            return startPageResult.getValidationResult();
        }

        PageNumberResult endPageResult =
                parsePageNumber(
                        endPageNumber,
                        Field.END_PAGE
                );

        if (!endPageResult.isValid()) {
            return endPageResult.getValidationResult();
        }

        int startPage =
                startPageResult.getPageNumber();

        int endPage =
                endPageResult.getPageNumber();

        if (startPage > 0
                && endPage > 0
                && endPage < startPage) {

            return ValidationResult.error(
                    Field.END_PAGE,
                    "End page cannot be before the start page."
            );
        }

        if (safeDescription.length()
                > MAX_DESCRIPTION_LENGTH) {

            return ValidationResult.error(
                    Field.CHAPTER_DESCRIPTION,
                    "Chapter description is too long."
            );
        }

        if (safeLearningObjectives.length()
                > MAX_LEARNING_OBJECTIVES_LENGTH) {

            return ValidationResult.error(
                    Field.LEARNING_OBJECTIVES,
                    "Learning objectives are too long."
            );
        }

        if (safeImportantTopics.length()
                > MAX_IMPORTANT_TOPICS_LENGTH) {

            return ValidationResult.error(
                    Field.IMPORTANT_TOPICS,
                    "Important topics are too long."
            );
        }

        ValidatedChapterForm validatedForm =
                new ValidatedChapterForm(
                        bookRowId,
                        safeChapterNumber,
                        safeEnglishTitle,
                        safeHindiTitle,
                        safeSubtitle,
                        safeUnitName,
                        safeChapterType,
                        startPage,
                        endPage,
                        safeDescription,
                        safeLearningObjectives,
                        safeImportantTopics
                );

        return ValidationResult.success(
                validatedForm
        );
    }

    /**
     * UI dropdown के लिए valid chapter types देता है।
     */
    @NonNull
    public static Set<String> getSupportedChapterTypes() {
        return SUPPORTED_CHAPTER_TYPES;
    }

    @NonNull
    private static PageNumberResult parsePageNumber(
            @Nullable String rawPageNumber,
            @NonNull Field field
    ) {
        String safePageNumber =
                safeText(
                        rawPageNumber
                );

        if (safePageNumber.isEmpty()) {
            return PageNumberResult.success(
                    0
            );
        }

        try {
            int pageNumber =
                    Integer.parseInt(
                            safePageNumber
                    );

            if (pageNumber < 0) {
                return PageNumberResult.error(
                        field,
                        "Page number cannot be negative."
                );
            }

            return PageNumberResult.success(
                    pageNumber
            );

        } catch (NumberFormatException exception) {
            return PageNumberResult.error(
                    field,
                    "Enter a valid page number."
            );
        }
    }

    @NonNull
    private static String normalizeChapterType(
            @Nullable String chapterType
    ) {
        String safeChapterType =
                safeText(
                        chapterType
                );

        if (safeChapterType.isEmpty()) {
            return SchoolBookChapterEntity
                    .CHAPTER_TYPE_CHAPTER;
        }

        return safeChapterType
                .toUpperCase(
                        Locale.US
                )
                .replace(
                        '-',
                        '_'
                )
                .replace(
                        ' ',
                        '_'
                )
                .replaceAll(
                        "_+",
                        "_"
                );
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    public enum Field {

        NONE,

        BOOK,

        CHAPTER_NUMBER,

        CHAPTER_TITLE,

        CHAPTER_TITLE_ENGLISH,

        CHAPTER_TITLE_HINDI,

        CHAPTER_SUBTITLE,

        UNIT_NAME,

        CHAPTER_TYPE,

        START_PAGE,

        END_PAGE,

        CHAPTER_DESCRIPTION,

        LEARNING_OBJECTIVES,

        IMPORTANT_TOPICS
    }

    public static final class ValidationResult {

        private final boolean valid;

        @NonNull
        private final Field field;

        @NonNull
        private final String errorMessage;

        @Nullable
        private final ValidatedChapterForm validatedForm;

        private ValidationResult(
                boolean valid,
                @NonNull Field field,
                @NonNull String errorMessage,
                @Nullable ValidatedChapterForm validatedForm
        ) {
            this.valid = valid;
            this.field = field;
            this.errorMessage = errorMessage;
            this.validatedForm = validatedForm;
        }

        @NonNull
        public static ValidationResult success(
                @NonNull ValidatedChapterForm validatedForm
        ) {
            return new ValidationResult(
                    true,
                    Field.NONE,
                    "",
                    validatedForm
            );
        }

        @NonNull
        public static ValidationResult error(
                @NonNull Field field,
                @NonNull String errorMessage
        ) {
            return new ValidationResult(
                    false,
                    field,
                    errorMessage,
                    null
            );
        }

        public boolean isValid() {
            return valid;
        }

        @NonNull
        public Field getField() {
            return field;
        }

        @NonNull
        public String getErrorMessage() {
            return errorMessage;
        }

        @Nullable
        public ValidatedChapterForm getValidatedForm() {
            return validatedForm;
        }
    }

    public static final class ValidatedChapterForm {

        private final long bookRowId;

        @NonNull
        private final String chapterNumber;

        @NonNull
        private final String chapterTitleEnglish;

        @NonNull
        private final String chapterTitleHindi;

        @NonNull
        private final String chapterSubtitle;

        @NonNull
        private final String unitName;

        @NonNull
        private final String chapterType;

        private final int startPageNumber;

        private final int endPageNumber;

        @NonNull
        private final String chapterDescription;

        @NonNull
        private final String learningObjectives;

        @NonNull
        private final String importantTopics;

        private ValidatedChapterForm(
                long bookRowId,
                @NonNull String chapterNumber,
                @NonNull String chapterTitleEnglish,
                @NonNull String chapterTitleHindi,
                @NonNull String chapterSubtitle,
                @NonNull String unitName,
                @NonNull String chapterType,
                int startPageNumber,
                int endPageNumber,
                @NonNull String chapterDescription,
                @NonNull String learningObjectives,
                @NonNull String importantTopics
        ) {
            this.bookRowId = bookRowId;
            this.chapterNumber = chapterNumber;
            this.chapterTitleEnglish = chapterTitleEnglish;
            this.chapterTitleHindi = chapterTitleHindi;
            this.chapterSubtitle = chapterSubtitle;
            this.unitName = unitName;
            this.chapterType = chapterType;
            this.startPageNumber = startPageNumber;
            this.endPageNumber = endPageNumber;
            this.chapterDescription = chapterDescription;
            this.learningObjectives = learningObjectives;
            this.importantTopics = importantTopics;
        }

        public long getBookRowId() {
            return bookRowId;
        }

        @NonNull
        public String getChapterNumber() {
            return chapterNumber;
        }

        @NonNull
        public String getChapterTitleEnglish() {
            return chapterTitleEnglish;
        }

        @NonNull
        public String getChapterTitleHindi() {
            return chapterTitleHindi;
        }

        @NonNull
        public String getChapterSubtitle() {
            return chapterSubtitle;
        }

        @NonNull
        public String getUnitName() {
            return unitName;
        }

        @NonNull
        public String getChapterType() {
            return chapterType;
        }

        public int getStartPageNumber() {
            return startPageNumber;
        }

        public int getEndPageNumber() {
            return endPageNumber;
        }

        @NonNull
        public String getChapterDescription() {
            return chapterDescription;
        }

        @NonNull
        public String getLearningObjectives() {
            return learningObjectives;
        }

        @NonNull
        public String getImportantTopics() {
            return importantTopics;
        }
    }

    private static final class PageNumberResult {

        private final boolean valid;

        private final int pageNumber;

        @Nullable
        private final ValidationResult validationResult;

        private PageNumberResult(
                boolean valid,
                int pageNumber,
                @Nullable ValidationResult validationResult
        ) {
            this.valid = valid;
            this.pageNumber = pageNumber;
            this.validationResult = validationResult;
        }

        @NonNull
        private static PageNumberResult success(
                int pageNumber
        ) {
            return new PageNumberResult(
                    true,
                    pageNumber,
                    null
            );
        }

        @NonNull
        private static PageNumberResult error(
                @NonNull Field field,
                @NonNull String errorMessage
        ) {
            return new PageNumberResult(
                    false,
                    0,
                    ValidationResult.error(
                            field,
                            errorMessage
                    )
            );
        }

        private boolean isValid() {
            return valid;
        }

        private int getPageNumber() {
            return pageNumber;
        }

        @NonNull
        private ValidationResult getValidationResult() {
            if (validationResult == null) {
                throw new IllegalStateException(
                        "Validation error is not available."
                );
            }

            return validationResult;
        }
    }
}