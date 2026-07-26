package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.dao
        .SchoolBookChapterDao;
import com.tridev.studysaathi.data.local.dao
        .SchoolBookDao;
import com.tridev.studysaathi.data.local.database
        .StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChildSchoolBookChapterRepository {

    @NonNull
    private final SchoolBookDao schoolBookDao;

    @NonNull
    private final SchoolBookChapterDao chapterDao;

    @NonNull
    private final Handler mainThreadHandler;

    public ChildSchoolBookChapterRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(
                        context.getApplicationContext()
                );

        schoolBookDao =
                database.schoolBookDao();

        chapterDao =
                database.schoolBookChapterDao();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Child Mode के लिए selected subject की exact confirmed
     * chapter list load करता है।
     */
    public void getChildChaptersForSubject(
            long subjectRowId,
            @NonNull ChildChaptersCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        if (subjectRowId <= 0L) {
                            throw new IllegalArgumentException(
                                    "A valid school subject row ID is required."
                            );
                        }

                        ChildChapterResult result =
                                loadChildChapterResult(
                                        subjectRowId
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
    private ChildChapterResult loadChildChapterResult(
            long subjectRowId
    ) {
        /*
         * DAO query केवल active primary book लौटाती है।
         */
        SchoolBookEntity primaryBook =
                schoolBookDao.getPrimaryBookForSubject(
                        subjectRowId
                );

        if (primaryBook == null) {
            return ChildChapterResult.unavailable(
                    subjectRowId,
                    UnavailableReason
                            .PRIMARY_BOOK_NOT_FOUND
            );
        }

        if (primaryBook.getBookRowId()
                <= 0L) {

            return ChildChapterResult.unavailable(
                    subjectRowId,
                    UnavailableReason
                            .INVALID_PRIMARY_BOOK
            );
        }

        /*
         * केवल active/primary होना पर्याप्त नहीं है।
         * Parent confirmation भी आवश्यक है।
         */
        if (!primaryBook.isParentConfirmedMatch()) {
            return ChildChapterResult.unavailable(
                    subjectRowId,
                    primaryBook,
                    UnavailableReason
                            .BOOK_NOT_PARENT_CONFIRMED
            );
        }

        List<SchoolBookChapterEntity> chapters =
                chapterDao.getChildModeChaptersForBook(
                        primaryBook.getBookRowId()
                );

        List<SchoolBookChapterEntity> safeChapters =
                chapters == null
                        ? Collections.emptyList()
                        : new ArrayList<>(
                        chapters
                );

        if (safeChapters.isEmpty()) {
            return ChildChapterResult.unavailable(
                    subjectRowId,
                    primaryBook,
                    UnavailableReason
                            .NO_CONFIRMED_CHAPTERS
            );
        }

        /*
         * DAO पहले से enabled + Parent-confirmed + valid title
         * filter लगाती है। फिर भी repository boundary पर result
         * दोबारा verify किया जाता है।
         */
        List<SchoolBookChapterEntity> verifiedChapters =
                new ArrayList<>();

        for (SchoolBookChapterEntity chapter : safeChapters) {
            if (chapter == null) {
                continue;
            }

            if (chapter.getBookRowId()
                    != primaryBook.getBookRowId()) {

                continue;
            }

            if (!chapter.isReadyForChildMode()) {
                continue;
            }

            verifiedChapters.add(
                    chapter
            );
        }

        if (verifiedChapters.isEmpty()) {
            return ChildChapterResult.unavailable(
                    subjectRowId,
                    primaryBook,
                    UnavailableReason
                            .NO_CONFIRMED_CHAPTERS
            );
        }

        return ChildChapterResult.available(
                subjectRowId,
                primaryBook,
                verifiedChapters
        );
    }

    private void postToMainThread(
            @NonNull Runnable runnable
    ) {
        mainThreadHandler.post(
                runnable
        );
    }

    public enum UnavailableReason {

        NONE,

        PRIMARY_BOOK_NOT_FOUND,

        INVALID_PRIMARY_BOOK,

        BOOK_NOT_PARENT_CONFIRMED,

        NO_CONFIRMED_CHAPTERS
    }

    public static final class ChildChapterResult {

        private final long subjectRowId;

        @Nullable
        private final SchoolBookEntity schoolBook;

        @NonNull
        private final List<SchoolBookChapterEntity> chapters;

        private final boolean available;

        @NonNull
        private final UnavailableReason unavailableReason;

        private ChildChapterResult(
                long subjectRowId,
                @Nullable SchoolBookEntity schoolBook,
                @NonNull List<SchoolBookChapterEntity> chapters,
                boolean available,
                @NonNull UnavailableReason unavailableReason
        ) {
            this.subjectRowId =
                    Math.max(
                            0L,
                            subjectRowId
                    );

            this.schoolBook =
                    schoolBook;

            this.chapters =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    chapters
                            )
                    );

            this.available =
                    available;

            this.unavailableReason =
                    unavailableReason;
        }

        @NonNull
        private static ChildChapterResult available(
                long subjectRowId,
                @NonNull SchoolBookEntity schoolBook,
                @NonNull List<SchoolBookChapterEntity> chapters
        ) {
            return new ChildChapterResult(
                    subjectRowId,
                    schoolBook,
                    chapters,
                    true,
                    UnavailableReason.NONE
            );
        }

        @NonNull
        private static ChildChapterResult unavailable(
                long subjectRowId,
                @NonNull UnavailableReason reason
        ) {
            return new ChildChapterResult(
                    subjectRowId,
                    null,
                    Collections.emptyList(),
                    false,
                    reason
            );
        }

        @NonNull
        private static ChildChapterResult unavailable(
                long subjectRowId,
                @NonNull SchoolBookEntity schoolBook,
                @NonNull UnavailableReason reason
        ) {
            return new ChildChapterResult(
                    subjectRowId,
                    schoolBook,
                    Collections.emptyList(),
                    false,
                    reason
            );
        }

        public long getSubjectRowId() {
            return subjectRowId;
        }

        @Nullable
        public SchoolBookEntity getSchoolBook() {
            return schoolBook;
        }

        @NonNull
        public List<SchoolBookChapterEntity> getChapters() {
            return chapters;
        }

        public boolean isAvailable() {
            return available;
        }

        @NonNull
        public UnavailableReason getUnavailableReason() {
            return unavailableReason;
        }

        public int getChapterCount() {
            return chapters.size();
        }

        @NonNull
        public String getUnavailableMessage() {
            switch (unavailableReason) {
                case PRIMARY_BOOK_NOT_FOUND:
                    return "Your exact school book has not been selected.";

                case INVALID_PRIMARY_BOOK:
                    return "The selected school book is invalid.";

                case BOOK_NOT_PARENT_CONFIRMED:
                    return "Your exact school book is waiting "
                            + "for Parent confirmation.";

                case NO_CONFIRMED_CHAPTERS:
                    return "No Parent-confirmed chapters "
                            + "are available for this book.";

                case NONE:
                default:
                    return "";
            }
        }
    }

    public interface ChildChaptersCallback {

        void onSuccess(
                @NonNull ChildChapterResult result
        );

        void onError(
                @NonNull Exception exception
        );
    }
}