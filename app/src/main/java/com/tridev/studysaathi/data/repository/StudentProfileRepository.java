package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.dao.StudentProfileDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;

import java.util.List;

public class StudentProfileRepository {

    private final StudentProfileDao studentProfileDao;
    private final Handler mainThreadHandler;

    public StudentProfileRepository(@NonNull Context context) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(context);

        studentProfileDao = database.studentProfileDao();
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public void insertProfile(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull InsertProfileCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long insertedProfileId =
                        studentProfileDao.insertProfile(studentProfile);

                mainThreadHandler.post(() ->
                        callback.onSuccess(insertedProfileId)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void getAllProfiles(
            @NonNull ProfilesCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<StudentProfileEntity> profiles =
                        studentProfileDao.getAllProfiles();

                mainThreadHandler.post(() ->
                        callback.onSuccess(profiles)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void getActiveProfile(
            @NonNull SingleProfileCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                StudentProfileEntity activeProfile =
                        studentProfileDao.getActiveProfile();

                mainThreadHandler.post(() ->
                        callback.onSuccess(activeProfile)
                );
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public void activateProfile(
            long profileId,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                studentProfileDao.deactivateAllProfiles();
                studentProfileDao.activateProfile(
                        profileId,
                        System.currentTimeMillis()
                );

                mainThreadHandler.post(callback::onSuccess);
            } catch (Exception exception) {
                mainThreadHandler.post(() ->
                        callback.onError(exception)
                );
            }
        });
    }

    public interface InsertProfileCallback {

        void onSuccess(long insertedProfileId);

        void onError(@NonNull Exception exception);
    }

    public interface ProfilesCallback {

        void onSuccess(
                @NonNull List<StudentProfileEntity> profiles
        );

        void onError(@NonNull Exception exception);
    }

    public interface SingleProfileCallback {

        void onSuccess(StudentProfileEntity studentProfile);

        void onError(@NonNull Exception exception);
    }

    public interface OperationCallback {

        void onSuccess();

        void onError(@NonNull Exception exception);
    }
}