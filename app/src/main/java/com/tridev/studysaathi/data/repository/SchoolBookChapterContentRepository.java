package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.dao
        .SchoolBookChapterContentDao;
import com.tridev.studysaathi.data.local.database
        .StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterContentEntity;

import java.util.Collections;
import java.util.List;

public final class SchoolBookChapterContentRepository {

    @NonNull
    private final SchoolBookChapterContentDao contentDao;

    @NonNull
    private final Handler mainThreadHandler;

    public SchoolBookChapterContentRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(
                        context.getApplicationContext()
                );

        contentDao =
                database.schoolBookChapterContentDao();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    public void getContentForChapter(
            long chapterRowId,
            @NonNull SingleContentCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(
                    chapterRowId
            );

            SchoolBookChapterContentEntity content =
                    contentDao.getContentForChapter(
                            chapterRowId
                    );

            postSuccess(
                    callback,
                    content
            );
        }, callback::onError);
    }

    public void getApprovedContentForChapter(
            long chapterRowId,
            @NonNull SingleContentCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(
                    chapterRowId
            );

            SchoolBookChapterContentEntity content =
                    contentDao
                            .getApprovedContentForChapter(
                                    chapterRowId
                            );

            if (content != null
                    && !content.isReadyForChildMode()) {
                content =
                        null;
            }

            postSuccess(
                    callback,
                    content
            );
        }, callback::onError);
    }

    public void getContentsForBook(
            long bookRowId,
            @NonNull ContentsCallback callback
    ) {
        execute(() -> {
            if (bookRowId <= 0L) {
                throw new IllegalArgumentException(
                        "A valid book row ID is required."
                );
            }

            List<SchoolBookChapterContentEntity> contents =
                    contentDao.getContentsForBook(bookRowId);

            List<SchoolBookChapterContentEntity> safeContents =
                    contents == null
                            ? Collections.emptyList()
                            : contents;

            mainThreadHandler.post(() ->
                    callback.onSuccess(safeContents)
            );
        }, callback::onError);
    }

    public void saveDraft(
            long chapterRowId,
            @NonNull SchoolBookChapterContentEntity content,
            @NonNull SaveContentCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(
                    chapterRowId
            );

            SchoolBookChapterContentEntity existingContent =
                    contentDao.getContentForChapter(
                            chapterRowId
                    );

            long currentTime =
                    System.currentTimeMillis();

            content.setChapterRowId(
                    chapterRowId
            );

            content.setParentApproved(
                    false
            );

            content.setApprovedAt(
                    0L
            );

            if (!SchoolBookChapterContentEntity
                    .REVIEW_STATUS_PROCESSING.equals(
                            content.getReviewStatus()
                    )
                    && !SchoolBookChapterContentEntity
                    .REVIEW_STATUS_PENDING_REVIEW.equals(
                            content.getReviewStatus()
                    )) {

                content.setReviewStatus(
                        SchoolBookChapterContentEntity
                                .REVIEW_STATUS_DRAFT
                );
            }

            if (existingContent == null) {
                content.prepareForNewDraft(
                        chapterRowId
                );

                long insertedRowId =
                        contentDao.insertContent(
                                content
                        );

                if (insertedRowId <= 0L) {
                    throw new IllegalStateException(
                            "Chapter learning content "
                                    + "could not be created."
                    );
                }

                content.setContentRowId(
                        insertedRowId
                );

                postSaveSuccess(
                        callback,
                        insertedRowId,
                        true
                );

                return;
            }

            content.setContentRowId(
                    existingContent.getContentRowId()
            );

            if (content.getContentId().isEmpty()) {
                content.setContentId(
                        existingContent.getContentId()
                );
            }

            if (content.getCreatedAt() <= 0L) {
                content.setCreatedAt(
                        existingContent.getCreatedAt()
                );
            }

            content.setUpdatedAt(
                    currentTime
            );

            int updatedRowCount =
                    contentDao.updateContent(
                            content
                    );

            ensureUpdated(
                    updatedRowCount
            );

            postSaveSuccess(
                    callback,
                    content.getContentRowId(),
                    false
            );
        }, callback::onError);
    }

    public void submitForParentReview(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        execute(() -> {
            SchoolBookChapterContentEntity content =
                    requireContent(
                            chapterRowId
                    );

            if (!content.hasReadableContent()) {
                throw new IllegalStateException(
                        "Add chapter explanation or summary "
                                + "before submitting it for review."
                );
            }

            long currentTime =
                    System.currentTimeMillis();

            int updatedRowCount =
                    contentDao.updateReviewState(
                            chapterRowId,
                            SchoolBookChapterContentEntity
                                    .REVIEW_STATUS_PENDING_REVIEW,
                            false,
                            0L,
                            currentTime
                    );

            ensureUpdated(
                    updatedRowCount
            );

            postOperationSuccess(
                    callback
            );
        }, callback::onError);
    }

    public void approveContent(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        execute(() -> {
            SchoolBookChapterContentEntity content =
                    requireContent(
                            chapterRowId
                    );

            if (!content.hasReadableContent()) {
                throw new IllegalStateException(
                        "Empty chapter content cannot be approved."
                );
            }

            long currentTime =
                    System.currentTimeMillis();

            int updatedRowCount =
                    contentDao.updateReviewState(
                            chapterRowId,
                            SchoolBookChapterContentEntity
                                    .REVIEW_STATUS_APPROVED,
                            true,
                            currentTime,
                            currentTime
                    );

            ensureUpdated(
                    updatedRowCount
            );

            postOperationSuccess(
                    callback
            );
        }, callback::onError);
    }

    public void rejectContent(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        execute(() -> {
            requireContent(
                    chapterRowId
            );

            long currentTime =
                    System.currentTimeMillis();

            int updatedRowCount =
                    contentDao.updateReviewState(
                            chapterRowId,
                            SchoolBookChapterContentEntity
                                    .REVIEW_STATUS_REJECTED,
                            false,
                            0L,
                            currentTime
                    );

            ensureUpdated(
                    updatedRowCount
            );

            postOperationSuccess(
                    callback
            );
        }, callback::onError);
    }

    public void markProcessing(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        execute(() -> {
            requireContent(
                    chapterRowId
            );

            int updatedRowCount =
                    contentDao.markProcessing(
                            chapterRowId,
                            System.currentTimeMillis()
                    );

            ensureUpdated(
                    updatedRowCount
            );

            postOperationSuccess(
                    callback
            );
        }, callback::onError);
    }

    public void markFailed(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        execute(() -> {
            requireContent(
                    chapterRowId
            );

            int updatedRowCount =
                    contentDao.markFailed(
                            chapterRowId,
                            System.currentTimeMillis()
                    );

            ensureUpdated(
                    updatedRowCount
            );

            postOperationSuccess(
                    callback
            );
        }, callback::onError);
    }

    public void deleteContentForChapter(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        execute(() -> {
            validateChapterRowId(
                    chapterRowId
            );

            contentDao.deleteContentForChapter(
                    chapterRowId
            );

            postOperationSuccess(
                    callback
            );
        }, callback::onError);
    }

    @NonNull
    private SchoolBookChapterContentEntity requireContent(
            long chapterRowId
    ) {
        validateChapterRowId(
                chapterRowId
        );

        SchoolBookChapterContentEntity content =
                contentDao.getContentForChapter(
                        chapterRowId
                );

        if (content == null) {
            throw new IllegalStateException(
                    "Chapter learning content was not found."
            );
        }

        return content;
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

    private void ensureUpdated(
            int updatedRowCount
    ) {
        if (updatedRowCount <= 0) {
            throw new IllegalStateException(
                    "Chapter learning content "
                            + "could not be updated."
            );
        }
    }

    private void execute(
            @NonNull RepositoryOperation operation,
            @NonNull ErrorCallback errorCallback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                operation.run();

            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        errorCallback.onError(
                                exception
                        )
                );
            }
        });
    }

    private void postSuccess(
            @NonNull SingleContentCallback callback,
            @Nullable SchoolBookChapterContentEntity content
    ) {
        mainThreadHandler.post(() ->
                callback.onSuccess(
                        content
                )
        );
    }

    private void postSaveSuccess(
            @NonNull SaveContentCallback callback,
            long contentRowId,
            boolean created
    ) {
        mainThreadHandler.post(() ->
                callback.onSuccess(
                        contentRowId,
                        created
                )
        );
    }

    private void postOperationSuccess(
            @NonNull OperationCallback callback
    ) {
        mainThreadHandler.post(
                callback::onSuccess
        );
    }

    private interface RepositoryOperation {

        void run() throws Exception;
    }

    private interface ErrorCallback {

        void onError(
                @NonNull Exception exception
        );
    }

    public interface SingleContentCallback {

        void onSuccess(
                @Nullable SchoolBookChapterContentEntity content
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public interface ContentsCallback {
        void onSuccess(
                @NonNull List<SchoolBookChapterContentEntity> contents
        );

        void onError(@NonNull Exception exception);
    }

    public interface SaveContentCallback {

        void onSuccess(
                long contentRowId,
                boolean created
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public interface OperationCallback {

        void onSuccess();

        void onError(
                @NonNull Exception exception
        );
    }
}
