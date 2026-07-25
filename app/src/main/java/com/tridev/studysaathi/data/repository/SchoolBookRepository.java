package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.dao.SchoolBookDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.SchoolBookEntity;

import java.util.Collections;
import java.util.List;

public final class SchoolBookRepository {

    @NonNull
    private final SchoolBookDao schoolBookDao;

    @NonNull
    private final Handler mainThreadHandler;

    public SchoolBookRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(
                        context.getApplicationContext()
                );

        schoolBookDao =
                database.schoolBookDao();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * नई school book database में insert करता है।
     */
    public void insertBook(
            @NonNull SchoolBookEntity schoolBook,
            @NonNull InsertBookCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookForInsert(
                                schoolBook
                        );

                        long insertedBookRowId =
                                schoolBookDao.insertBook(
                                        schoolBook
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        insertedBookRowId
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Existing school book update करता है।
     */
    public void updateBook(
            @NonNull SchoolBookEntity schoolBook,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        if (schoolBook.getBookRowId()
                                <= 0L) {

                            throw new IllegalArgumentException(
                                    "A valid book row ID is required."
                            );
                        }

                        schoolBook.setUpdatedAt(
                                System.currentTimeMillis()
                        );

                        int updatedRows =
                                schoolBookDao.updateBook(
                                        schoolBook
                                );

                        if (updatedRows <= 0) {
                            throw new IllegalStateException(
                                    "The selected school book was not found."
                            );
                        }

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Database row ID से book प्राप्त करता है।
     */
    public void getBookByRowId(
            long bookRowId,
            @NonNull SingleBookCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        SchoolBookEntity schoolBook =
                                schoolBookDao
                                        .getBookByRowId(
                                                bookRowId
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        schoolBook
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * किसी subject की सभी books प्राप्त करता है।
     */
    public void getBooksForSubject(
            long subjectRowId,
            boolean activeBooksOnly,
            @NonNull BooksCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        List<SchoolBookEntity> books;

                        if (activeBooksOnly) {
                            books =
                                    schoolBookDao
                                            .getActiveBooksForSubject(
                                                    subjectRowId
                                            );

                        } else {
                            books =
                                    schoolBookDao
                                            .getBooksForSubject(
                                                    subjectRowId
                                            );
                        }

                        List<SchoolBookEntity> safeBooks =
                                books == null
                                        ? Collections.emptyList()
                                        : books;

                        postToMainThread(() ->
                                callback.onSuccess(
                                        safeBooks
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * किसी subject की primary book प्राप्त करता है।
     */
    public void getPrimaryBookForSubject(
            long subjectRowId,
            @NonNull SingleBookCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        SchoolBookEntity primaryBook =
                                schoolBookDao
                                        .getPrimaryBookForSubject(
                                                subjectRowId
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        primaryBook
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * ISBN या title/publisher से duplicate book जाँचता है।
     *
     * Match priority:
     *
     * 1. ISBN-13
     * 2. ISBN-10
     * 3. Exact title और publisher
     */
    public void findDuplicateBook(
            long subjectRowId,
            @Nullable String isbn10,
            @Nullable String isbn13,
            @Nullable String bookTitle,
            @Nullable String publisherName,
            @NonNull DuplicateBookCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        String safeIsbn13 =
                                normalizeIsbn(
                                        isbn13
                                );

                        String safeIsbn10 =
                                normalizeIsbn(
                                        isbn10
                                );

                        String safeBookTitle =
                                safeText(
                                        bookTitle
                                );

                        String safePublisherName =
                                safeText(
                                        publisherName
                                );

                        SchoolBookEntity duplicateBook =
                                null;

                        DuplicateMatchType matchType =
                                DuplicateMatchType.NONE;

                        if (!safeIsbn13.isEmpty()) {
                            duplicateBook =
                                    schoolBookDao
                                            .findSubjectBookByIsbn13(
                                                    subjectRowId,
                                                    safeIsbn13
                                            );

                            if (duplicateBook != null) {
                                matchType =
                                        DuplicateMatchType.ISBN_13;
                            }
                        }

                        if (duplicateBook == null
                                && !safeIsbn10.isEmpty()) {

                            duplicateBook =
                                    schoolBookDao
                                            .findSubjectBookByIsbn10(
                                                    subjectRowId,
                                                    safeIsbn10
                                            );

                            if (duplicateBook != null) {
                                matchType =
                                        DuplicateMatchType.ISBN_10;
                            }
                        }

                        if (duplicateBook == null
                                && !safeBookTitle.isEmpty()
                                && !safePublisherName.isEmpty()) {

                            duplicateBook =
                                    schoolBookDao
                                            .findBookByTitleAndPublisher(
                                                    subjectRowId,
                                                    safeBookTitle,
                                                    safePublisherName
                                            );

                            if (duplicateBook != null) {
                                matchType =
                                        DuplicateMatchType
                                                .TITLE_AND_PUBLISHER;
                            }
                        }

                        SchoolBookEntity finalDuplicateBook =
                                duplicateBook;

                        DuplicateMatchType finalMatchType =
                                matchType;

                        postToMainThread(() ->
                                callback.onResult(
                                        finalDuplicateBook,
                                        finalMatchType
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Book को active या inactive करता है।
     */
    public void setBookActive(
            long bookRowId,
            boolean active,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        int updatedRows =
                                schoolBookDao.setBookActive(
                                        bookRowId,
                                        active,
                                        System.currentTimeMillis()
                                );

                        requireUpdatedRow(
                                updatedRows
                        );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Parent confirmation status update करता है।
     */
    public void setParentConfirmedMatch(
            long bookRowId,
            boolean confirmed,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        int updatedRows =
                                schoolBookDao
                                        .setParentConfirmedMatch(
                                                bookRowId,
                                                confirmed,
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
                        );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * चुनी गई book को subject की primary book बनाता है।
     */
    public void setPrimaryBook(
            long subjectRowId,
            long bookRowId,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        validateBookRowId(
                                bookRowId
                        );

                        boolean primaryBookUpdated =
                                schoolBookDao.setPrimaryBook(
                                        subjectRowId,
                                        bookRowId
                                );

                        if (!primaryBookUpdated) {
                            throw new IllegalStateException(
                                    "The selected book could not be made primary."
                            );
                        }

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Book की sort order update करता है।
     */
    public void updateBookSortOrder(
            long bookRowId,
            int sortOrder,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        int updatedRows =
                                schoolBookDao
                                        .updateBookSortOrder(
                                                bookRowId,
                                                Math.max(
                                                        0,
                                                        sortOrder
                                                ),
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
                        );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Subject की next available sort order देता है।
     */
    public void getNextSortOrder(
            long subjectRowId,
            @NonNull SortOrderCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        int maximumSortOrder =
                                schoolBookDao
                                        .getMaximumSortOrder(
                                                subjectRowId
                                        );

                        int nextSortOrder =
                                Math.max(
                                        0,
                                        maximumSortOrder
                                ) + 1;

                        postToMainThread(() ->
                                callback.onSuccess(
                                        nextSortOrder
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Book को permanently delete करता है।
     */
    public void deleteBook(
            @NonNull SchoolBookEntity schoolBook,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        if (schoolBook.getBookRowId()
                                <= 0L) {

                            throw new IllegalArgumentException(
                                    "A valid book row ID is required."
                            );
                        }

                        int deletedRows =
                                schoolBookDao.deleteBook(
                                        schoolBook
                                );

                        if (deletedRows <= 0) {
                            throw new IllegalStateException(
                                    "The selected school book was not found."
                            );
                        }

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    private void validateBookForInsert(
            @NonNull SchoolBookEntity schoolBook
    ) {
        validateSubjectRowId(
                schoolBook.getSubjectRowId()
        );

        if (schoolBook.getBookId()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Book ID is required."
            );
        }

        if (schoolBook.getBookTitle()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Book title is required."
            );
        }

        long currentTime =
                System.currentTimeMillis();

        if (schoolBook.getCreatedAt()
                <= 0L) {

            schoolBook.setCreatedAt(
                    currentTime
            );
        }

        schoolBook.setUpdatedAt(
                currentTime
        );
    }

    private void validateSubjectRowId(
            long subjectRowId
    ) {
        if (subjectRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school subject row ID is required."
            );
        }
    }

    private void validateBookRowId(
            long bookRowId
    ) {
        if (bookRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school book row ID is required."
            );
        }
    }

    private void requireUpdatedRow(
            int updatedRows
    ) {
        if (updatedRows <= 0) {
            throw new IllegalStateException(
                    "The selected school book was not found."
            );
        }
    }

    @NonNull
    private String normalizeIsbn(
            @Nullable String isbn
    ) {
        return safeText(
                isbn
        )
                .replace(
                        "-",
                        ""
                )
                .replace(
                        " ",
                        ""
                )
                .toUpperCase();
    }

    @NonNull
    private String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private void postToMainThread(
            @NonNull Runnable runnable
    ) {
        mainThreadHandler.post(
                runnable
        );
    }

    private void postError(
            @NonNull ErrorCallback callback,
            @NonNull Exception exception
    ) {
        postToMainThread(() ->
                callback.onError(
                        exception
                )
        );
    }

    public enum DuplicateMatchType {

        NONE,

        ISBN_13,

        ISBN_10,

        TITLE_AND_PUBLISHER
    }

    public interface ErrorCallback {

        void onError(
                @NonNull Exception exception
        );
    }

    public interface InsertBookCallback
            extends ErrorCallback {

        void onSuccess(
                long insertedBookRowId
        );
    }

    public interface SingleBookCallback
            extends ErrorCallback {

        void onSuccess(
                @Nullable SchoolBookEntity schoolBook
        );
    }

    public interface BooksCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<SchoolBookEntity> schoolBooks
        );
    }

    public interface DuplicateBookCallback
            extends ErrorCallback {

        void onResult(
                @Nullable SchoolBookEntity duplicateBook,
                @NonNull DuplicateMatchType matchType
        );
    }

    public interface SortOrderCallback
            extends ErrorCallback {

        void onSuccess(
                int nextSortOrder
        );
    }

    public interface OperationCallback
            extends ErrorCallback {

        void onSuccess();
    }
}