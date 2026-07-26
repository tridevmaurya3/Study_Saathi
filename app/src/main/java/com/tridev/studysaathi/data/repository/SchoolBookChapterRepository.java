package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.dao.SchoolBookChapterDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterEntity;

import java.util.Collections;
import java.util.List;

public final class SchoolBookChapterRepository {

    @NonNull
    private final SchoolBookChapterDao chapterDao;

    @NonNull
    private final Handler mainThreadHandler;

    public SchoolBookChapterRepository(
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
     * एक नया chapter insert करता है।
     */
    public void insertChapter(
            @NonNull SchoolBookChapterEntity chapter,
            @NonNull InsertChapterCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        long insertedChapterRowId =
                                chapterDao.insertChapter(
                                        chapter
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        insertedChapterRowId
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
     * कई chapters को एक transaction में insert करता है।
     */
    public void insertChapters(
            @NonNull List<SchoolBookChapterEntity> chapters,
            @NonNull InsertChaptersCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        if (chapters.isEmpty()) {
                            postToMainThread(() ->
                                    callback.onSuccess(
                                            Collections.emptyList()
                                    )
                            );

                            return;
                        }

                        List<Long> insertedRowIds =
                                chapterDao.insertChapters(
                                        chapters
                                );

                        List<Long> safeInsertedRowIds =
                                insertedRowIds == null
                                        ? Collections.emptyList()
                                        : insertedRowIds;

                        postToMainThread(() ->
                                callback.onSuccess(
                                        safeInsertedRowIds
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
     * Existing chapter के सभी fields update करता है।
     */
    public void updateChapter(
            @NonNull SchoolBookChapterEntity chapter,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        int updatedRows =
                                chapterDao.updateChapter(
                                        chapter
                                );

                        requireAffectedRow(
                                updatedRows,
                                "The selected chapter was not found."
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
     * Database row ID से exact chapter प्राप्त करता है।
     */
    public void getChapterByRowId(
            long chapterRowId,
            @NonNull SingleChapterCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateChapterRowId(
                                chapterRowId
                        );

                        SchoolBookChapterEntity chapter =
                                chapterDao.getChapterByRowId(
                                        chapterRowId
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        chapter
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
     * किसी exact school book के सभी chapters देता है।
     */
    public void getChaptersForBook(
            long bookRowId,
            @NonNull ChaptersCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        List<SchoolBookChapterEntity> chapters =
                                chapterDao.getChaptersForBook(
                                        bookRowId
                                );

                        postChapterList(
                                callback,
                                chapters
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
     * Parent review के लिए pending chapters देता है।
     */
    public void getChaptersPendingParentReview(
            long bookRowId,
            @NonNull ChaptersCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        List<SchoolBookChapterEntity> chapters =
                                chapterDao
                                        .getChaptersPendingParentReview(
                                                bookRowId
                                        );

                        postChapterList(
                                callback,
                                chapters
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
     * Child Mode में दिखने योग्य chapters देता है।
     */
    public void getChildModeChaptersForBook(
            long bookRowId,
            @NonNull ChaptersCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        List<SchoolBookChapterEntity> chapters =
                                chapterDao
                                        .getChildModeChaptersForBook(
                                                bookRowId
                                        );

                        postChapterList(
                                callback,
                                chapters
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
     * Parent-confirmed chapters देता है।
     */
    public void getParentConfirmedChaptersForBook(
            long bookRowId,
            @NonNull ChaptersCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        List<SchoolBookChapterEntity> chapters =
                                chapterDao
                                        .getParentConfirmedChaptersForBook(
                                                bookRowId
                                        );

                        postChapterList(
                                callback,
                                chapters
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
     * नए chapter के लिए अगला sort order देता है।
     */
    public void getNextSortOrder(
            long bookRowId,
            @NonNull SortOrderCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        int nextSortOrder =
                                chapterDao.getNextSortOrder(
                                        bookRowId
                                );

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
     * Chapter को enabled या disabled करता है।
     */
    public void setChapterEnabled(
            long chapterRowId,
            boolean enabled,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateChapterRowId(
                                chapterRowId
                        );

                        int updatedRows =
                                chapterDao.setChapterEnabled(
                                        chapterRowId,
                                        enabled,
                                        System.currentTimeMillis()
                                );

                        requireAffectedRow(
                                updatedRows,
                                "The selected chapter was not found."
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
     * एक chapter का Parent confirmation बदलता है।
     */
    public void setParentConfirmed(
            long chapterRowId,
            boolean confirmed,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateChapterRowId(
                                chapterRowId
                        );

                        int updatedRows =
                                chapterDao.setParentConfirmed(
                                        chapterRowId,
                                        confirmed,
                                        System.currentTimeMillis()
                                );

                        requireAffectedRow(
                                updatedRows,
                                "The selected chapter was not found."
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
     * पूरी book के pending chapters confirm करता है।
     */
    public void confirmAllChaptersForBook(
            long bookRowId,
            @NonNull CountOperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        int confirmedChapterCount =
                                chapterDao
                                        .confirmAllChaptersForBook(
                                                bookRowId,
                                                System.currentTimeMillis()
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        confirmedChapterCount
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
     * Chapter का sort order बदलता है।
     */
    public void updateChapterSortOrder(
            long chapterRowId,
            int sortOrder,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateChapterRowId(
                                chapterRowId
                        );

                        int updatedRows =
                                chapterDao.updateChapterSortOrder(
                                        chapterRowId,
                                        Math.max(
                                                0,
                                                sortOrder
                                        ),
                                        System.currentTimeMillis()
                                );

                        requireAffectedRow(
                                updatedRows,
                                "The selected chapter was not found."
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
     * Chapter permanently delete करता है।
     */
    public void deleteChapterByRowId(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateChapterRowId(
                                chapterRowId
                        );

                        int deletedRows =
                                chapterDao.deleteChapterByRowId(
                                        chapterRowId
                                );

                        requireAffectedRow(
                                deletedRows,
                                "The selected chapter was not found."
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
     * Parent-confirmed replacement list से पूरी chapter list बदलता है।
     */
    public void replaceChaptersForBook(
            long bookRowId,
            @NonNull List<SchoolBookChapterEntity> chapters,
            @NonNull InsertChaptersCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateBookRowId(
                                bookRowId
                        );

                        List<Long> insertedRowIds =
                                chapterDao.replaceChaptersForBook(
                                        bookRowId,
                                        chapters
                                );

                        List<Long> safeInsertedRowIds =
                                insertedRowIds == null
                                        ? Collections.emptyList()
                                        : insertedRowIds;

                        postToMainThread(() ->
                                callback.onSuccess(
                                        safeInsertedRowIds
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

    private void validateBookRowId(
            long bookRowId
    ) {
        if (bookRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school book row ID is required."
            );
        }
    }

    private void validateChapterRowId(
            long chapterRowId
    ) {
        if (chapterRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid chapter row ID is required."
            );
        }
    }

    private void requireAffectedRow(
            int affectedRows,
            @NonNull String errorMessage
    ) {
        if (affectedRows <= 0) {
            throw new IllegalStateException(
                    errorMessage
            );
        }
    }

    private void postChapterList(
            @NonNull ChaptersCallback callback,
            @Nullable List<SchoolBookChapterEntity> chapters
    ) {
        List<SchoolBookChapterEntity> safeChapters =
                chapters == null
                        ? Collections.emptyList()
                        : chapters;

        postToMainThread(() ->
                callback.onSuccess(
                        safeChapters
                )
        );
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

    public interface ErrorCallback {

        void onError(
                @NonNull Exception exception
        );
    }

    public interface InsertChapterCallback
            extends ErrorCallback {

        void onSuccess(
                long insertedChapterRowId
        );
    }

    public interface InsertChaptersCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<Long> insertedChapterRowIds
        );
    }

    public interface SingleChapterCallback
            extends ErrorCallback {

        void onSuccess(
                @Nullable SchoolBookChapterEntity chapter
        );
    }

    public interface ChaptersCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<SchoolBookChapterEntity> chapters
        );
    }

    public interface SortOrderCallback
            extends ErrorCallback {

        void onSuccess(
                int nextSortOrder
        );
    }

    public interface CountOperationCallback
            extends ErrorCallback {

        void onSuccess(
                int affectedChapterCount
        );
    }

    public interface OperationCallback
            extends ErrorCallback {

        void onSuccess();
    }
}