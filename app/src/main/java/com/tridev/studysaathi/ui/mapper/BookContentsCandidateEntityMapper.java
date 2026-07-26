package com.tridev.studysaathi.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.ui.adapter
        .BookContentsCandidateAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BookContentsCandidateEntityMapper {

    private BookContentsCandidateEntityMapper() {
        // Utility class.
    }

    /**
     * Parent-reviewed OCR candidates को नई chapter entities में
     * बदलता है।
     */
    @NonNull
    public static List<SchoolBookChapterEntity> toPendingReviewEntities(
            long bookRowId,
            @NonNull List<BookContentsCandidateAdapter
                    .CandidateInput> candidateInputs,
            int startingSortOrder,
            @Nullable String sourceImagePath
    ) {
        if (bookRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school book row ID is required."
            );
        }

        if (candidateInputs.isEmpty()) {
            return Collections.emptyList();
        }

        List<SchoolBookChapterEntity> chapters =
                new ArrayList<>();

        int currentSortOrder =
                Math.max(
                        0,
                        startingSortOrder
                );

        for (int index = 0;
             index < candidateInputs.size();
             index++) {

            BookContentsCandidateAdapter.CandidateInput input =
                    candidateInputs.get(
                            index
                    );

            validateCandidate(
                    input,
                    index
            );

            int inferredEndPage =
                    inferEndPageNumber(
                            candidateInputs,
                            index
                    );

            SchoolBookChapterEntity chapter =
                    createChapterEntity(
                            bookRowId,
                            input,
                            currentSortOrder,
                            inferredEndPage,
                            sourceImagePath
                    );

            chapters.add(
                    chapter
            );

            if (currentSortOrder < Integer.MAX_VALUE) {
                currentSortOrder++;
            }
        }

        return Collections.unmodifiableList(
                chapters
        );
    }

    @NonNull
    private static SchoolBookChapterEntity createChapterEntity(
            long bookRowId,
            @NonNull BookContentsCandidateAdapter.CandidateInput input,
            int sortOrder,
            int inferredEndPage,
            @Nullable String sourceImagePath
    ) {
        long currentTime =
                System.currentTimeMillis();

        SchoolBookChapterEntity chapter =
                new SchoolBookChapterEntity();

        chapter.setBookRowId(
                bookRowId
        );

        chapter.setChapterId(
                SchoolBookChapterEntity.createChapterId()
        );

        chapter.setChapterNumber(
                input.getChapterNumber()
        );

        setDetectedTitle(
                chapter,
                input.getChapterTitle()
        );

        chapter.setChapterSubtitle(
                ""
        );

        chapter.setUnitName(
                ""
        );

        chapter.setChapterType(
                SchoolBookChapterEntity
                        .CHAPTER_TYPE_CHAPTER
        );

        chapter.setStartPageNumber(
                input.getStartPageNumber()
        );

        chapter.setEndPageNumber(
                inferredEndPage
        );

        chapter.setChapterDescription(
                ""
        );

        chapter.setLearningObjectives(
                ""
        );

        chapter.setImportantTopics(
                ""
        );

        chapter.setContentSource(
                SchoolBookChapterEntity
                        .CONTENT_SOURCE_BOOK_TOC_SCAN
        );

        chapter.setSourceReference(
                createSourceReference(
                        sourceImagePath,
                        input
                )
        );

        chapter.setExtractionConfidence(
                input.getConfidence()
        );

        /*
         * OCR result केवल candidate है। Parent ने import चुना है,
         * लेकिन final chapter confirmation अभी बाकी है।
         */
        chapter.setParentConfirmed(
                false
        );

        chapter.setEnabled(
                true
        );

        chapter.setOptionalChapter(
                false
        );

        chapter.setRevisionChapter(
                false
        );

        chapter.setContentProcessingStatus(
                SchoolBookChapterEntity
                        .PROCESSING_STATUS_PENDING_REVIEW
        );

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

        chapter.setSortOrder(
                sortOrder
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

        requireValidEntity(
                chapter
        );

        return chapter;
    }

    /**
     * Devanagari text को Hindi title में और बाकी को English title
     * में रखता है। Parent बाद में Edit screen से दूसरी भाषा भी
     * जोड़ सकता है।
     */
    private static void setDetectedTitle(
            @NonNull SchoolBookChapterEntity chapter,
            @NonNull String detectedTitle
    ) {
        String safeTitle =
                detectedTitle.trim();

        if (containsDevanagari(
                safeTitle
        )) {
            chapter.setChapterTitleEnglish(
                    ""
            );

            chapter.setChapterTitleHindi(
                    safeTitle
            );

        } else {
            chapter.setChapterTitleEnglish(
                    safeTitle
            );

            chapter.setChapterTitleHindi(
                    ""
            );
        }
    }

    /**
     * अगले detected chapter के start page से वर्तमान chapter का
     * probable end page निकालता है।
     *
     * यदि page order स्पष्ट नहीं है तो end page 0 रखा जाएगा।
     */
    private static int inferEndPageNumber(
            @NonNull List<BookContentsCandidateAdapter
                    .CandidateInput> candidateInputs,
            int currentIndex
    ) {
        BookContentsCandidateAdapter.CandidateInput currentInput =
                candidateInputs.get(
                        currentIndex
                );

        int currentStartPage =
                currentInput.getStartPageNumber();

        if (currentStartPage <= 0
                || currentIndex + 1
                >= candidateInputs.size()) {

            return 0;
        }

        BookContentsCandidateAdapter.CandidateInput nextInput =
                candidateInputs.get(
                        currentIndex + 1
                );

        int nextStartPage =
                nextInput.getStartPageNumber();

        if (nextStartPage
                <= currentStartPage) {

            return 0;
        }

        return nextStartPage - 1;
    }

    @NonNull
    private static String createSourceReference(
            @Nullable String sourceImagePath,
            @NonNull BookContentsCandidateAdapter.CandidateInput input
    ) {
        String safeImagePath =
                safeText(
                        sourceImagePath
                );

        String safeRawLine =
                safeText(
                        input.getRawDetectedLine()
                );

        StringBuilder reference =
                new StringBuilder();

        if (!safeImagePath.isEmpty()) {
            reference.append(
                    "TOC_IMAGE="
            );

            reference.append(
                    safeImagePath
            );
        }

        if (input.getSourceLineNumber() > 0) {
            if (reference.length() > 0) {
                reference.append(
                        "; "
                );
            }

            reference.append(
                    "OCR_LINE_NUMBER="
            );

            reference.append(
                    input.getSourceLineNumber()
            );
        }

        if (!safeRawLine.isEmpty()) {
            if (reference.length() > 0) {
                reference.append(
                        "; "
                );
            }

            reference.append(
                    "OCR_TEXT="
            );

            reference.append(
                    safeRawLine
            );
        }

        return reference.toString();
    }

    private static void validateCandidate(
            @NonNull BookContentsCandidateAdapter.CandidateInput input,
            int index
    ) {
        if (!input.hasValidTitle()) {
            throw new IllegalArgumentException(
                    "Chapter candidate "
                            + (index + 1)
                            + " requires a title."
            );
        }

        if (input.getChapterTitle()
                .length() > 200) {

            throw new IllegalArgumentException(
                    "Chapter candidate "
                            + (index + 1)
                            + " has a title longer than 200 characters."
            );
        }

        if (input.getChapterNumber()
                .length() > 40) {

            throw new IllegalArgumentException(
                    "Chapter candidate "
                            + (index + 1)
                            + " has an invalid chapter number."
            );
        }
    }

    private static void requireValidEntity(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        if (!chapter.hasMinimumRequiredInformation()) {
            throw new IllegalStateException(
                    "An OCR chapter candidate could not be mapped."
            );
        }

        if (!chapter.hasValidPageRange()) {
            throw new IllegalStateException(
                    "An OCR chapter candidate has an invalid page range."
            );
        }

        if (chapter.isParentConfirmed()) {
            throw new IllegalStateException(
                    "An imported OCR chapter cannot be "
                            + "automatically Parent-confirmed."
            );
        }

        if (!SchoolBookChapterEntity
                .PROCESSING_STATUS_PENDING_REVIEW
                .equals(
                        chapter.getContentProcessingStatus()
                )) {

            throw new IllegalStateException(
                    "An imported OCR chapter must remain "
                            + "pending Parent review."
            );
        }
    }

    private static boolean containsDevanagari(
            @NonNull String value
    ) {
        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(
                            index
                    );

            if (character >= '\u0900'
                    && character <= '\u097F') {

                return true;
            }
        }

        return false;
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }
}