package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.dao
        .SchoolBookChapterDao;
import com.tridev.studysaathi.data.local.database
        .StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BookContentsChapterImportCoordinator {

    @NonNull
    private final SchoolBookChapterDao chapterDao;

    @NonNull
    private final Handler mainThreadHandler;

    public BookContentsChapterImportCoordinator(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(
                        context.getApplicationContext()
                );

        chapterDao =
                database.schoolBookChapterDao();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Parent-reviewed OCR chapters को duplicate-safe तरीके से
     * database में import करता है।
     */
    public void importPendingReviewChapters(
            long bookRowId,
            @NonNull List<SchoolBookChapterEntity> chapters,
            @NonNull ImportCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateImportRequest(
                                bookRowId,
                                chapters
                        );

                        ImportResult result =
                                performSafeImport(
                                        bookRowId,
                                        chapters
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        result
                                )
                        );

                    } catch (Exception exception) {
                        postToMainThread(() ->
                                callback.onError(
                                        exception
                                )
                        );
                    }
                });
    }

    @NonNull
    private ImportResult performSafeImport(
            long bookRowId,
            @NonNull List<SchoolBookChapterEntity> chapters
    ) {
        List<Long> insertedChapterRowIds =
                new ArrayList<>();

        List<SkippedChapter> skippedChapters =
                new ArrayList<>();

        Set<String> requestChapterNumbers =
                new HashSet<>();

        Set<String> requestChapterTitles =
                new HashSet<>();

        for (SchoolBookChapterEntity chapter : chapters) {
            String chapterNumber =
                    safeText(
                            chapter.getChapterNumber()
                    );

            String chapterTitle =
                    getComparableTitle(
                            chapter
                    );

            String normalizedNumber =
                    normalizeComparisonText(
                            chapterNumber
                    );

            String normalizedTitle =
                    normalizeComparisonText(
                            chapterTitle
                    );

            DuplicateReason requestDuplicate =
                    findRequestDuplicate(
                            normalizedNumber,
                            normalizedTitle,
                            requestChapterNumbers,
                            requestChapterTitles
                    );

            if (requestDuplicate
                    != DuplicateReason.NONE) {

                skippedChapters.add(
                        new SkippedChapter(
                                chapterNumber,
                                chapterTitle,
                                requestDuplicate
                        )
                );

                continue;
            }

            DuplicateReason databaseDuplicate =
                    findDatabaseDuplicate(
                            bookRowId,
                            chapterNumber,
                            chapterTitle
                    );

            if (databaseDuplicate
                    != DuplicateReason.NONE) {

                skippedChapters.add(
                        new SkippedChapter(
                                chapterNumber,
                                chapterTitle,
                                databaseDuplicate
                        )
                );

                rememberCandidateIdentity(
                        normalizedNumber,
                        normalizedTitle,
                        requestChapterNumbers,
                        requestChapterTitles
                );

                continue;
            }

            long insertedRowId =
                    chapterDao.insertChapter(
                            chapter
                    );

            insertedChapterRowIds.add(
                    insertedRowId
            );

            rememberCandidateIdentity(
                    normalizedNumber,
                    normalizedTitle,
                    requestChapterNumbers,
                    requestChapterTitles
            );
        }

        return new ImportResult(
                chapters.size(),
                insertedChapterRowIds,
                skippedChapters
        );
    }

    @NonNull
    private DuplicateReason findRequestDuplicate(
            @NonNull String normalizedNumber,
            @NonNull String normalizedTitle,
            @NonNull Set<String> requestChapterNumbers,
            @NonNull Set<String> requestChapterTitles
    ) {
        if (!normalizedNumber.isEmpty()
                && requestChapterNumbers.contains(
                normalizedNumber
        )) {
            return DuplicateReason
                    .DUPLICATE_NUMBER_IN_SCAN;
        }

        if (!normalizedTitle.isEmpty()
                && requestChapterTitles.contains(
                normalizedTitle
        )) {
            return DuplicateReason
                    .DUPLICATE_TITLE_IN_SCAN;
        }

        return DuplicateReason.NONE;
    }

    @NonNull
    private DuplicateReason findDatabaseDuplicate(
            long bookRowId,
            @NonNull String chapterNumber,
            @NonNull String chapterTitle
    ) {
        if (!chapterNumber.isEmpty()) {
            SchoolBookChapterEntity numberMatch =
                    chapterDao.findChapterByNumber(
                            bookRowId,
                            chapterNumber
                    );

            if (numberMatch != null) {
                return DuplicateReason
                        .NUMBER_ALREADY_IN_DATABASE;
            }
        }

        if (!chapterTitle.isEmpty()) {
            SchoolBookChapterEntity titleMatch =
                    chapterDao.findChapterByTitle(
                            bookRowId,
                            chapterTitle
                    );

            if (titleMatch != null) {
                return DuplicateReason
                        .TITLE_ALREADY_IN_DATABASE;
            }
        }

        return DuplicateReason.NONE;
    }

    private void rememberCandidateIdentity(
            @NonNull String normalizedNumber,
            @NonNull String normalizedTitle,
            @NonNull Set<String> requestChapterNumbers,
            @NonNull Set<String> requestChapterTitles
    ) {
        if (!normalizedNumber.isEmpty()) {
            requestChapterNumbers.add(
                    normalizedNumber
            );
        }

        if (!normalizedTitle.isEmpty()) {
            requestChapterTitles.add(
                    normalizedTitle
            );
        }
    }

    private void validateImportRequest(
            long bookRowId,
            @NonNull List<SchoolBookChapterEntity> chapters
    ) {
        if (bookRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school book row ID is required."
            );
        }

        if (chapters.isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one chapter to import."
            );
        }

        for (int index = 0;
             index < chapters.size();
             index++) {

            SchoolBookChapterEntity chapter =
                    chapters.get(
                            index
                    );

            if (chapter.getBookRowId()
                    != bookRowId) {

                throw new IllegalArgumentException(
                        "Chapter candidate "
                                + (index + 1)
                                + " belongs to a different school book."
                );
            }

            if (!chapter.hasMinimumRequiredInformation()) {
                throw new IllegalArgumentException(
                        "Chapter candidate "
                                + (index + 1)
                                + " is missing required information."
                );
            }

            if (chapter.isParentConfirmed()) {
                throw new IllegalArgumentException(
                        "OCR chapter candidate "
                                + (index + 1)
                                + " cannot be automatically confirmed."
                );
            }

            if (!SchoolBookChapterEntity
                    .CONTENT_SOURCE_BOOK_TOC_SCAN
                    .equals(
                            chapter.getContentSource()
                    )) {

                throw new IllegalArgumentException(
                        "Chapter candidate "
                                + (index + 1)
                                + " does not have a valid scan source."
                );
            }

            if (!SchoolBookChapterEntity
                    .PROCESSING_STATUS_PENDING_REVIEW
                    .equals(
                            chapter.getContentProcessingStatus()
                    )) {

                throw new IllegalArgumentException(
                        "Chapter candidate "
                                + (index + 1)
                                + " must remain pending Parent review."
                );
            }
        }
    }

    @NonNull
    private String getComparableTitle(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        String englishTitle =
                safeText(
                        chapter.getChapterTitleEnglish()
                );

        if (!englishTitle.isEmpty()) {
            return englishTitle;
        }

        return safeText(
                chapter.getChapterTitleHindi()
        );
    }

    @NonNull
    private String normalizeComparisonText(
            @Nullable String value
    ) {
        return safeText(
                value
        )
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        " "
                )
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    private String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }

    private void postToMainThread(
            @NonNull Runnable runnable
    ) {
        mainThreadHandler.post(
                runnable
        );
    }

    public enum DuplicateReason {

        NONE,

        DUPLICATE_NUMBER_IN_SCAN,

        DUPLICATE_TITLE_IN_SCAN,

        NUMBER_ALREADY_IN_DATABASE,

        TITLE_ALREADY_IN_DATABASE
    }

    public static final class SkippedChapter {

        @NonNull
        private final String chapterNumber;

        @NonNull
        private final String chapterTitle;

        @NonNull
        private final DuplicateReason reason;

        private SkippedChapter(
                @NonNull String chapterNumber,
                @NonNull String chapterTitle,
                @NonNull DuplicateReason reason
        ) {
            this.chapterNumber =
                    chapterNumber;

            this.chapterTitle =
                    chapterTitle;

            this.reason =
                    reason;
        }

        @NonNull
        public String getChapterNumber() {
            return chapterNumber;
        }

        @NonNull
        public String getChapterTitle() {
            return chapterTitle;
        }

        @NonNull
        public DuplicateReason getReason() {
            return reason;
        }

        @NonNull
        public String getDisplayLabel() {
            if (!chapterNumber.isEmpty()) {
                return "Chapter "
                        + chapterNumber
                        + " — "
                        + chapterTitle;
            }

            return chapterTitle;
        }
    }

    public static final class ImportResult {

        private final int requestedCount;

        @NonNull
        private final List<Long> insertedChapterRowIds;

        @NonNull
        private final List<SkippedChapter> skippedChapters;

        private ImportResult(
                int requestedCount,
                @NonNull List<Long> insertedChapterRowIds,
                @NonNull List<SkippedChapter> skippedChapters
        ) {
            this.requestedCount =
                    Math.max(
                            0,
                            requestedCount
                    );

            this.insertedChapterRowIds =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    insertedChapterRowIds
                            )
                    );

            this.skippedChapters =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    skippedChapters
                            )
                    );
        }

        public int getRequestedCount() {
            return requestedCount;
        }

        public int getInsertedCount() {
            return insertedChapterRowIds.size();
        }

        public int getSkippedCount() {
            return skippedChapters.size();
        }

        @NonNull
        public List<Long> getInsertedChapterRowIds() {
            return insertedChapterRowIds;
        }

        @NonNull
        public List<SkippedChapter> getSkippedChapters() {
            return skippedChapters;
        }

        public boolean hasInsertedChapters() {
            return !insertedChapterRowIds.isEmpty();
        }

        public boolean hasSkippedChapters() {
            return !skippedChapters.isEmpty();
        }

        public boolean isCompleteSuccess() {
            return requestedCount > 0
                    && getInsertedCount()
                    == requestedCount
                    && !hasSkippedChapters();
        }
    }

    public interface ImportCallback {

        void onSuccess(
                @NonNull ImportResult result
        );

        void onError(
                @NonNull Exception exception
        );
    }
}