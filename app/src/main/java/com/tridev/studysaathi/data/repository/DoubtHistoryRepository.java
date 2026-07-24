package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.dao.DoubtHistoryDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;

import java.util.List;

public class DoubtHistoryRepository {

    private final DoubtHistoryDao doubtHistoryDao;
    private final Handler mainThreadHandler;

    public DoubtHistoryRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(context);

        doubtHistoryDao =
                database.doubtHistoryDao();

        mainThreadHandler =
                new Handler(Looper.getMainLooper());
    }

    public void saveHistory(
            @NonNull DoubtHistoryEntity doubtHistory,
            @NonNull SaveHistoryCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long historyId =
                        doubtHistoryDao.insertHistory(
                                doubtHistory
                        );

                mainThreadHandler.post(() ->
                        callback.onSuccess(historyId)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void getHistoryForProfile(
            long profileId,
            @NonNull HistoryListCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<DoubtHistoryEntity> historyList =
                        doubtHistoryDao.getHistoryForProfile(
                                profileId
                        );

                mainThreadHandler.post(() ->
                        callback.onSuccess(historyList)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void deleteHistoryById(
            long historyId,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                doubtHistoryDao.deleteHistoryById(
                        historyId
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

    public void deleteHistoryForProfile(
            long profileId,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                doubtHistoryDao.deleteHistoryForProfile(
                        profileId
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

    public interface SaveHistoryCallback {

        void onSuccess(long historyId);

        void onError(
                @NonNull Exception exception
        );
    }

    public interface HistoryListCallback {

        void onSuccess(
                @NonNull List<DoubtHistoryEntity> historyList
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