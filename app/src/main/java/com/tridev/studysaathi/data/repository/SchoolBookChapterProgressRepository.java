package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.dao.SchoolBookChapterDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterEntity;

public final class SchoolBookChapterProgressRepository {

    @NonNull
    private final SchoolBookChapterDao chapterDao;

    @NonNull
    private final Handler mainThreadHandler;

    public SchoolBookChapterProgressRepository(
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

    public void markChapterOpened(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                validateChapterRowId(
                        chapterRowId
                );

                long currentTime =
                        System.currentTimeMillis();

                int updatedRowCount =
                        chapterDao.updateLastOpenedTime(
                                chapterRowId,
                                currentTime,
                                currentTime
                        );

                ensureChapterWasUpdated(
                        updatedRowCount
                );

                postSuccess(
                        callback
                );

            } catch (Exception exception) {
                postError(
                        callback,
                        exception
                );
            }
        });
    }

    public void updateChapterProgress(
            long chapterRowId,
            int requestedProgressPercent,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                validateChapterRowId(
                        chapterRowId
                );

                SchoolBookChapterEntity chapter =
                        chapterDao.getChapterByRowId(
                                chapterRowId
                        );

                if (chapter == null) {
                    throw new IllegalStateException(
                            "The exact school-book chapter was not found."
                    );
                }

                int progressPercent =
                        clampProgress(
                                requestedProgressPercent
                        );

                int lessonCount =
                        Math.max(
                                1,
                                chapter.getLessonCount()
                        );

                int completedLessonCount =
                        chapter.getCompletedLessonCount();

                if (progressPercent >= 100) {
                    completedLessonCount =
                            lessonCount;
                } else {
                    completedLessonCount =
                            Math.min(
                                    completedLessonCount,
                                    lessonCount
                            );
                }

                long currentTime =
                        System.currentTimeMillis();

                int updatedRowCount =
                        chapterDao.updateChapterProgressSummary(
                                chapterRowId,
                                lessonCount,
                                completedLessonCount,
                                Math.max(
                                        0,
                                        chapter.getQuizQuestionCount()
                                ),
                                Math.max(
                                        0,
                                        chapter.getNoteCount()
                                ),
                                Math.max(
                                        0,
                                        chapter.getBookmarkCount()
                                ),
                                progressPercent,
                                currentTime
                        );

                ensureChapterWasUpdated(
                        updatedRowCount
                );

                postSuccess(
                        callback
                );

            } catch (Exception exception) {
                postError(
                        callback,
                        exception
                );
            }
        });
    }

    public void markChapterCompleted(
            long chapterRowId,
            @NonNull OperationCallback callback
    ) {
        updateChapterProgress(
                chapterRowId,
                100,
                callback
        );
    }

    private void validateChapterRowId(
            long chapterRowId
    ) {
        if (chapterRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid exact chapter row ID is required."
            );
        }
    }

    private int clampProgress(
            int progressPercent
    ) {
        return Math.max(
                0,
                Math.min(
                        100,
                        progressPercent
                )
        );
    }

    private void ensureChapterWasUpdated(
            int updatedRowCount
    ) {
        if (updatedRowCount <= 0) {
            throw new IllegalStateException(
                    "The exact school-book chapter could not be updated."
            );
        }
    }

    private void postSuccess(
            @NonNull OperationCallback callback
    ) {
        mainThreadHandler.post(
                callback::onSuccess
        );
    }

    private void postError(
            @NonNull OperationCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(() ->
                callback.onError(
                        exception
                )
        );
    }

    public interface OperationCallback {

        void onSuccess();

        void onError(
                @NonNull Exception exception
        );
    }
}