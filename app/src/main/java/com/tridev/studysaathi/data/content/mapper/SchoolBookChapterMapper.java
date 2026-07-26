package com.tridev.studysaathi.data.content.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.validation
        .SchoolBookChapterFormValidator;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;

public final class SchoolBookChapterMapper {

    private SchoolBookChapterMapper() {
        // Utility class.
    }

    /**
     * Parent द्वारा manually भरे गए validated chapter form को
     * नई database entity में बदलता है।
     *
     * Manual chapter को Parent-confirmed माना जाएगा।
     */
    @NonNull
    public static SchoolBookChapterEntity fromManualForm(
            @NonNull SchoolBookChapterFormValidator
                    .ValidatedChapterForm form,
            int sortOrder,
            boolean optionalChapter,
            boolean revisionChapter
    ) {
        return fromManualForm(
                form,
                sortOrder,
                optionalChapter,
                revisionChapter,
                ""
        );
    }

    /**
     * Parent द्वारा manually भरे गए validated chapter form को
     * optional source reference के साथ entity में बदलता है।
     */
    @NonNull
    public static SchoolBookChapterEntity fromManualForm(
            @NonNull SchoolBookChapterFormValidator
                    .ValidatedChapterForm form,
            int sortOrder,
            boolean optionalChapter,
            boolean revisionChapter,
            @Nullable String sourceReference
    ) {
        validateManualForm(
                form
        );

        long currentTime =
                System.currentTimeMillis();

        SchoolBookChapterEntity chapter =
                new SchoolBookChapterEntity();

        chapter.setBookRowId(
                form.getBookRowId()
        );

        chapter.setChapterId(
                SchoolBookChapterEntity.createChapterId()
        );

        applyValidatedForm(
                chapter,
                form
        );

        chapter.setContentSource(
                SchoolBookChapterEntity
                        .CONTENT_SOURCE_PARENT_MANUAL
        );

        chapter.setSourceReference(
                safeText(
                        sourceReference
                )
        );

        chapter.setExtractionConfidence(
                1F
        );

        /*
         * Parent ने स्वयं chapter भरा है, इसलिए यह तुरंत
         * confirmed और Child Mode के लिए eligible हो सकता है।
         */
        chapter.setContentProcessingStatus(
                SchoolBookChapterEntity
                        .PROCESSING_STATUS_CONFIRMED
        );

        chapter.setParentConfirmed(
                true
        );

        chapter.setEnabled(
                true
        );

        chapter.setOptionalChapter(
                optionalChapter
        );

        chapter.setRevisionChapter(
                revisionChapter
        );

        chapter.setSortOrder(
                Math.max(
                        0,
                        sortOrder
                )
        );

        initializeProgressFields(
                chapter
        );

        chapter.setLastOpenedAt(
                0L
        );

        chapter.setLastContentProcessedAt(
                currentTime
        );

        chapter.setCreatedAt(
                currentTime
        );

        chapter.setUpdatedAt(
                currentTime
        );

        requireValidMappedEntity(
                chapter
        );

        return chapter;
    }

    /**
     * Existing chapter को edited validated form से update करता है।
     *
     * Database row ID, chapter ID, content source, progress,
     * timestamps और confirmation state सुरक्षित रहते हैं।
     */
    @NonNull
    public static SchoolBookChapterEntity applyManualEdit(
            @NonNull SchoolBookChapterEntity existingChapter,
            @NonNull SchoolBookChapterFormValidator
                    .ValidatedChapterForm form,
            int sortOrder,
            boolean optionalChapter,
            boolean revisionChapter
    ) {
        if (existingChapter.getChapterRowId()
                <= 0L) {

            throw new IllegalArgumentException(
                    "A valid existing chapter row ID is required."
            );
        }

        if (existingChapter.getBookRowId()
                <= 0L) {

            throw new IllegalArgumentException(
                    "The existing chapter is not linked to a valid book."
            );
        }

        validateManualForm(
                form
        );

        if (existingChapter.getBookRowId()
                != form.getBookRowId()) {

            throw new IllegalArgumentException(
                    "A chapter cannot be moved to another school book."
            );
        }

        applyValidatedForm(
                existingChapter,
                form
        );

        existingChapter.setSortOrder(
                Math.max(
                        0,
                        sortOrder
                )
        );

        existingChapter.setOptionalChapter(
                optionalChapter
        );

        existingChapter.setRevisionChapter(
                revisionChapter
        );

        existingChapter.setUpdatedAt(
                System.currentTimeMillis()
        );

        requireValidMappedEntity(
                existingChapter
        );

        return existingChapter;
    }

    /**
     * Validated form की editable values entity में लगाता है।
     */
    private static void applyValidatedForm(
            @NonNull SchoolBookChapterEntity chapter,
            @NonNull SchoolBookChapterFormValidator
                    .ValidatedChapterForm form
    ) {
        chapter.setChapterNumber(
                form.getChapterNumber()
        );

        chapter.setChapterTitleEnglish(
                form.getChapterTitleEnglish()
        );

        chapter.setChapterTitleHindi(
                form.getChapterTitleHindi()
        );

        chapter.setChapterSubtitle(
                form.getChapterSubtitle()
        );

        chapter.setUnitName(
                form.getUnitName()
        );

        chapter.setChapterType(
                form.getChapterType()
        );

        chapter.setStartPageNumber(
                form.getStartPageNumber()
        );

        chapter.setEndPageNumber(
                form.getEndPageNumber()
        );

        chapter.setChapterDescription(
                form.getChapterDescription()
        );

        chapter.setLearningObjectives(
                form.getLearningObjectives()
        );

        chapter.setImportantTopics(
                form.getImportantTopics()
        );
    }

    /**
     * नई manual chapter entity के progress-related fields को
     * सुरक्षित initial values देता है।
     */
    private static void initializeProgressFields(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        chapter.setLessonCount(
                0
        );

        chapter.setCompletedLessonCount(
                0
        );

        chapter.setQuizQuestionCount(
                0
        );

        chapter.setNoteCount(
                0
        );

        chapter.setBookmarkCount(
                0
        );

        chapter.setProgressPercent(
                0
        );
    }

    private static void validateManualForm(
            @NonNull SchoolBookChapterFormValidator
                    .ValidatedChapterForm form
    ) {
        if (form.getBookRowId()
                <= 0L) {

            throw new IllegalArgumentException(
                    "A valid school book row ID is required."
            );
        }

        if (form.getChapterTitleEnglish()
                .isEmpty()
                && form.getChapterTitleHindi()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Chapter title is required in English or Hindi."
            );
        }

        if (form.getStartPageNumber()
                > 0
                && form.getEndPageNumber()
                > 0
                && form.getEndPageNumber()
                < form.getStartPageNumber()) {

            throw new IllegalArgumentException(
                    "End page cannot be before the start page."
            );
        }
    }

    private static void requireValidMappedEntity(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        if (!chapter.hasMinimumRequiredInformation()) {
            throw new IllegalStateException(
                    "The mapped chapter does not contain "
                            + "the required information."
            );
        }

        if (!chapter.hasValidPageRange()) {
            throw new IllegalStateException(
                    "The mapped chapter has an invalid page range."
            );
        }

        if (!chapter.isParentConfirmed()) {
            throw new IllegalStateException(
                    "A manually entered chapter must be "
                            + "Parent-confirmed."
            );
        }
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
}