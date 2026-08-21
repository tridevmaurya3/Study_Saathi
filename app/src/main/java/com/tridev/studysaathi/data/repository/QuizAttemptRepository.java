package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.dao.QuizAttemptDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.learning.StudentKnowledgeGraphStore;

import java.util.List;

public class QuizAttemptRepository {

    private final Context applicationContext;
    private final QuizAttemptDao quizAttemptDao;
    private final Handler mainThreadHandler;

    public QuizAttemptRepository(
            @NonNull Context context
    ) {
        applicationContext = context.getApplicationContext();
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(applicationContext);

        quizAttemptDao = database.quizAttemptDao();

        mainThreadHandler = new Handler(
                Looper.getMainLooper()
        );
    }

    public void saveAttempt(
            @NonNull QuizAttemptEntity quizAttempt,
            @NonNull InsertAttemptCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long attemptId =
                        quizAttemptDao.insertAttempt(
                                quizAttempt
                        );

                try {
                    new StudentKnowledgeGraphStore(applicationContext)
                            .recordAssessment(quizAttempt);
                } catch (RuntimeException ignored) {
                    // Knowledge graph telemetry must never block the saved quiz result.
                }

                mainThreadHandler.post(() ->
                        callback.onSuccess(attemptId)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void getChapterStats(
            long profileId,
            @NonNull String educationBoard,
            @NonNull String studentClass,
            @NonNull String subjectName,
            @NonNull String chapterTitle,
            @NonNull QuizStatsCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Integer bestPercentageValue =
                        quizAttemptDao.getBestPercentage(
                                profileId,
                                educationBoard,
                                studentClass,
                                subjectName,
                                chapterTitle
                        );

                int bestPercentage =
                        bestPercentageValue == null
                                ? 0
                                : bestPercentageValue;

                int attemptCount =
                        quizAttemptDao.getAttemptCount(
                                profileId,
                                educationBoard,
                                studentClass,
                                subjectName,
                                chapterTitle
                        );

                QuizStats quizStats = new QuizStats(
                        bestPercentage,
                        attemptCount
                );

                mainThreadHandler.post(() ->
                        callback.onSuccess(quizStats)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void getProfileAttempts(
            long profileId,
            @NonNull QuizAttemptListCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<QuizAttemptEntity> quizAttempts =
                        quizAttemptDao.getProfileAttempts(
                                profileId
                        );

                mainThreadHandler.post(() ->
                        callback.onSuccess(quizAttempts)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public static class QuizStats {

        private final int bestPercentage;
        private final int attemptCount;

        public QuizStats(
                int bestPercentage,
                int attemptCount
        ) {
            this.bestPercentage = Math.max(
                    0,
                    Math.min(100, bestPercentage)
            );

            this.attemptCount = Math.max(
                    0,
                    attemptCount
            );
        }

        public int getBestPercentage() {
            return bestPercentage;
        }

        public int getAttemptCount() {
            return attemptCount;
        }
    }

    public interface InsertAttemptCallback {

        void onSuccess(long attemptId);

        void onError(
                @NonNull Exception exception
        );
    }

    public interface QuizStatsCallback {

        void onSuccess(
                @NonNull QuizStats quizStats
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public interface QuizAttemptListCallback {

        void onSuccess(
                @NonNull List<QuizAttemptEntity> quizAttempts
        );

        void onError(
                @NonNull Exception exception
        );
    }
}
