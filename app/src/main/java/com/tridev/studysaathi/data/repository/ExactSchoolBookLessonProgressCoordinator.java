package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.navigation
        .ExactSchoolBookLessonContract;

public final class ExactSchoolBookLessonProgressCoordinator {

    private final long exactChapterRowId;

    @NonNull
    private final SchoolBookChapterProgressRepository
            chapterProgressRepository;

    public ExactSchoolBookLessonProgressCoordinator(
            @NonNull Context context,
            @NonNull Intent lessonIntent
    ) {
        exactChapterRowId =
                ExactSchoolBookLessonContract
                        .readExactChapterRowId(
                                lessonIntent
                        );

        chapterProgressRepository =
                new SchoolBookChapterProgressRepository(
                        context.getApplicationContext()
                );
    }

    public boolean isExactSchoolBookLesson() {
        return exactChapterRowId
                > ExactSchoolBookLessonContract
                .INVALID_CHAPTER_ROW_ID;
    }

    public long getExactChapterRowId() {
        return exactChapterRowId;
    }

    public void markLessonOpened(
            @NonNull OperationCallback callback
    ) {
        if (!isExactSchoolBookLesson()) {
            callback.onSkipped();

            return;
        }

        chapterProgressRepository.markChapterOpened(
                exactChapterRowId,
                new SchoolBookChapterProgressRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        callback.onError(
                                exception
                        );
                    }
                }
        );
    }

    public void updateLessonProgress(
            int progressPercent,
            @NonNull OperationCallback callback
    ) {
        if (!isExactSchoolBookLesson()) {
            callback.onSkipped();

            return;
        }

        chapterProgressRepository.updateChapterProgress(
                exactChapterRowId,
                progressPercent,
                new SchoolBookChapterProgressRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        callback.onError(
                                exception
                        );
                    }
                }
        );
    }

    public void markLessonCompleted(
            @NonNull OperationCallback callback
    ) {
        if (!isExactSchoolBookLesson()) {
            callback.onSkipped();

            return;
        }

        chapterProgressRepository.markChapterCompleted(
                exactChapterRowId,
                new SchoolBookChapterProgressRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        callback.onError(
                                exception
                        );
                    }
                }
        );
    }

    public interface OperationCallback {

        void onSuccess();

        void onSkipped();

        void onError(
                @NonNull Exception exception
        );
    }
}