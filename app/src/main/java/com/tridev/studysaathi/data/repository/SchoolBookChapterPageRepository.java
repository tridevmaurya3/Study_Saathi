package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.dao
        .SchoolBookChapterPageDao;
import com.tridev.studysaathi.data.local.database
        .StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterPageEntity;

import java.util.ArrayList;
import java.util.List;

public final class SchoolBookChapterPageRepository {

    @NonNull
    private final SchoolBookChapterPageDao pageDao;

    @NonNull
    private final Handler mainThreadHandler;

    public SchoolBookChapterPageRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(
                        context.getApplicationContext()
                );

        pageDao =
                database.schoolBookChapterPageDao();

        mainThreadHandler =
                new Handler(Looper.getMainLooper());
    }

    public void getPagesForChapter(
            long chapterRowId,
            @NonNull PagesCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(chapterRowId);

            List<SchoolBookChapterPageEntity> pages =
                    pageDao.getPagesForChapter(
                            chapterRowId
                    );

            postPages(callback, pages);
        }, callback::onError);
    }

    public void getApprovedPagesForChapter(
            long chapterRowId,
            @NonNull PagesCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(chapterRowId);

            List<SchoolBookChapterPageEntity> pages =
                    pageDao.getApprovedPagesForChapter(
                            chapterRowId
                    );

            postPages(callback, pages);
        }, callback::onError);
    }

    public void getApprovedPageByOrder(
            long chapterRowId,
            int pageOrder,
            @NonNull SinglePageCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(chapterRowId);

            if (pageOrder <= 0) {
                throw new IllegalArgumentException(
                        "A valid chapter page order is required."
                );
            }

            SchoolBookChapterPageEntity page =
                    pageDao.getApprovedPageByOrder(
                            chapterRowId,
                            pageOrder
                    );

            mainThreadHandler.post(
                    () -> callback.onSuccess(page)
            );
        }, callback::onError);
    }

    /**
     * पहली बार निकले page drafts save करता है। Existing page order होने पर
     * ABORT होगा; accidental overwrite नहीं होगा।
     */
    public void saveNewPagesForParentReview(
            long chapterRowId,
            @NonNull List<SchoolBookChapterPageEntity> pages,
            @NonNull SavePagesCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(chapterRowId);

            ArrayList<SchoolBookChapterPageEntity>
                    preparedPages =
                    preparePages(
                            chapterRowId,
                            pages
                    );

            List<Long> insertedIds =
                    pageDao.insertPages(
                            preparedPages
                    );

            mainThreadHandler.post(
                    () -> callback.onSuccess(
                            insertedIds.size()
                    )
            );
        }, callback::onError);
    }

    /**
     * केवल Parent-confirmed replacement action से call करें।
     */
    public void replacePagesAfterParentConfirmation(
            long chapterRowId,
            @NonNull List<SchoolBookChapterPageEntity> pages,
            @NonNull SavePagesCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(chapterRowId);

            ArrayList<SchoolBookChapterPageEntity>
                    preparedPages =
                    preparePages(
                            chapterRowId,
                            pages
                    );

            List<Long> insertedIds =
                    pageDao.replacePagesForChapter(
                            chapterRowId,
                            preparedPages
                    );

            mainThreadHandler.post(
                    () -> callback.onSuccess(
                            insertedIds.size()
                    )
            );
        }, callback::onError);
    }

    public void setPageParentApproved(
            long pageRowId,
            boolean approved,
            @NonNull OperationCallback callback
    ) {
        execute(() -> {
            if (pageRowId <= 0L) {
                throw new IllegalArgumentException(
                        "A valid chapter page is required."
                );
            }

            int updatedRows =
                    pageDao.updateParentApproval(
                            pageRowId,
                            approved,
                            System.currentTimeMillis()
                    );

            ensureUpdated(updatedRows);
            postOperationSuccess(callback);
        }, callback::onError);
    }

    public void setAllPagesParentApproved(
            long chapterRowId,
            boolean approved,
            @NonNull CountOperationCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(chapterRowId);

            int updatedRows =
                    pageDao.updateAllPageApprovalsForChapter(
                            chapterRowId,
                            approved,
                            System.currentTimeMillis()
                    );

            mainThreadHandler.post(
                    () -> callback.onSuccess(
                            updatedRows
                    )
            );
        }, callback::onError);
    }

    public void countApprovedPages(
            long chapterRowId,
            @NonNull CountOperationCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(chapterRowId);

            int count =
                    pageDao.countApprovedPagesForChapter(
                            chapterRowId
                    );

            mainThreadHandler.post(
                    () -> callback.onSuccess(count)
            );
        }, callback::onError);
    }

    @NonNull
    private static ArrayList<SchoolBookChapterPageEntity>
    preparePages(
            long chapterRowId,
            @NonNull List<SchoolBookChapterPageEntity> pages
    ) {
        if (pages.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one chapter page is required."
            );
        }

        ArrayList<SchoolBookChapterPageEntity>
                preparedPages =
                new ArrayList<>();

        int pageOrder = 1;

        for (SchoolBookChapterPageEntity page
                : pages) {

            if (page == null
                    || !page.hasReadableContent()) {
                throw new IllegalArgumentException(
                        "Every chapter page must contain "
                                + "readable content."
                );
            }

            page.prepareForInsert(
                    chapterRowId,
                    pageOrder
            );

            page.setParentApproved(false);
            preparedPages.add(page);
            pageOrder++;
        }

        return preparedPages;
    }

    private void execute(
            @NonNull Runnable operation,
            @NonNull ErrorCallback errorCallback
    ) {
        StudySaathiDatabase.databaseWriteExecutor
                .execute(() -> {
                    try {
                        operation.run();
                    } catch (Exception exception) {
                        mainThreadHandler.post(
                                () -> errorCallback.onError(
                                        exception
                                )
                        );
                    }
                });
    }

    private void postPages(
            @NonNull PagesCallback callback,
            @NonNull List<SchoolBookChapterPageEntity> pages
    ) {
        ArrayList<SchoolBookChapterPageEntity> copy =
                new ArrayList<>(pages);

        mainThreadHandler.post(
                () -> callback.onSuccess(copy)
        );
    }

    private void postOperationSuccess(
            @NonNull OperationCallback callback
    ) {
        mainThreadHandler.post(
                callback::onSuccess
        );
    }

    private static void validateChapterRowId(
            long chapterRowId
    ) {
        if (chapterRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid exact chapter is required."
            );
        }
    }

    private static void ensureUpdated(
            int updatedRows
    ) {
        if (updatedRows <= 0) {
            throw new IllegalStateException(
                    "The chapter page was not updated."
            );
        }
    }

    public interface ErrorCallback {

        void onError(
                @NonNull Exception exception
        );
    }

    public interface PagesCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<SchoolBookChapterPageEntity> pages
        );
    }

    public interface SinglePageCallback
            extends ErrorCallback {

        void onSuccess(
                @Nullable SchoolBookChapterPageEntity page
        );
    }

    public interface SavePagesCallback
            extends ErrorCallback {

        void onSuccess(
                int savedPageCount
        );
    }

    public interface OperationCallback
            extends ErrorCallback {

        void onSuccess();
    }

    public interface CountOperationCallback
            extends ErrorCallback {

        void onSuccess(
                int count
        );
    }
}
