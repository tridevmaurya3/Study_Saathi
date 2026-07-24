package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.dao.LessonProgressDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;

import java.util.List;

public class LessonProgressRepository {

    private final LessonProgressDao lessonProgressDao;
    private final Handler mainThreadHandler;

    public LessonProgressRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(context);

        lessonProgressDao = database.lessonProgressDao();
        mainThreadHandler = new Handler(
                Looper.getMainLooper()
        );
    }

    public void saveProgress(
            @NonNull LessonProgressEntity lessonProgress,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                lessonProgressDao.saveProgress(
                        lessonProgress
                );

                mainThreadHandler.post(
                        callback::onSuccess
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void getProgress(
            @NonNull String progressKey,
            @NonNull SingleProgressCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                LessonProgressEntity lessonProgress =
                        lessonProgressDao.getProgressByKey(
                                progressKey
                        );

                mainThreadHandler.post(() ->
                        callback.onSuccess(lessonProgress)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void getProgressForSubject(
            long profileId,
            @NonNull String educationBoard,
            @NonNull String studentClass,
            @NonNull String subjectName,
            @NonNull ProgressListCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<LessonProgressEntity> progressList =
                        lessonProgressDao.getProgressForSubject(
                                profileId,
                                educationBoard,
                                studentClass,
                                subjectName
                        );

                mainThreadHandler.post(() ->
                        callback.onSuccess(progressList)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void getProgressForProfile(
            long profileId,
            @NonNull ProgressListCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<LessonProgressEntity> progressList =
                        lessonProgressDao.getProgressForProfile(
                                profileId
                        );

                mainThreadHandler.post(() ->
                        callback.onSuccess(progressList)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public interface OperationCallback {

        void onSuccess();

        void onError(
                @NonNull Exception exception
        );
    }

    public interface SingleProgressCallback {

        void onSuccess(
                LessonProgressEntity lessonProgress
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public interface ProgressListCallback {

        void onSuccess(
                @NonNull List<LessonProgressEntity> progressList
        );

        void onError(
                @NonNull Exception exception
        );
    }
}