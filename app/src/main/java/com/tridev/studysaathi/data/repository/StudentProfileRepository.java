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

    private final StudySaathiDatabase database;
    private final StudentProfileDao studentProfileDao;
    private final Handler mainThreadHandler;

    public StudentProfileRepository(@NonNull Context context) {
        database =
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

    public void getProfileById(
            long profileId,
            @NonNull SingleProfileCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                StudentProfileEntity profile =
                        studentProfileDao.getProfileById(profileId);
                mainThreadHandler.post(() -> callback.onSuccess(profile));
            } catch (Exception exception) {
                mainThreadHandler.post(() -> callback.onError(exception));
            }
        });
    }

    public void updateProfile(
            @NonNull StudentProfileEntity profile,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                profile.setUpdatedAt(System.currentTimeMillis());
                studentProfileDao.updateProfile(profile);
                mainThreadHandler.post(callback::onSuccess);
            } catch (Exception exception) {
                mainThreadHandler.post(() -> callback.onError(exception));
            }
        });
    }

    public void permanentlyDeleteProfile(
            long profileId,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                database.runInTransaction(() -> {
                    androidx.sqlite.db.SupportSQLiteDatabase sql =
                            database.getOpenHelper().getWritableDatabase();
                    Object[] args = new Object[]{profileId};
                    sql.execSQL("DELETE FROM school_book_chapter_pages WHERE chapter_row_id IN (SELECT chapter_row_id FROM school_book_chapters WHERE book_row_id IN (SELECT book_row_id FROM school_books WHERE subject_row_id IN (SELECT subject_row_id FROM school_subjects WHERE profile_id = ?)))", args);
                    sql.execSQL("DELETE FROM school_book_chapter_contents WHERE chapter_row_id IN (SELECT chapter_row_id FROM school_book_chapters WHERE book_row_id IN (SELECT book_row_id FROM school_books WHERE subject_row_id IN (SELECT subject_row_id FROM school_subjects WHERE profile_id = ?)))", args);
                    sql.execSQL("DELETE FROM school_book_chapters WHERE book_row_id IN (SELECT book_row_id FROM school_books WHERE subject_row_id IN (SELECT subject_row_id FROM school_subjects WHERE profile_id = ?))", args);
                    sql.execSQL("DELETE FROM school_books WHERE subject_row_id IN (SELECT subject_row_id FROM school_subjects WHERE profile_id = ?)", args);
                    sql.execSQL("DELETE FROM school_subjects WHERE profile_id = ?", args);
                    sql.execSQL("DELETE FROM school_curriculum_profiles WHERE profile_id = ?", args);
                    sql.execSQL("DELETE FROM doubt_history WHERE profile_id = ?", args);
                    sql.execSQL("DELETE FROM quiz_attempts WHERE profile_id = ?", args);
                    sql.execSQL("DELETE FROM lesson_progress WHERE profile_id = ?", args);
                    studentProfileDao.deleteProfileById(profileId);
                    if (studentProfileDao.getActiveProfile() == null) {
                        StudentProfileEntity latest = studentProfileDao.getLatestProfile();
                        if (latest != null) {
                            studentProfileDao.activateProfile(
                                    latest.getProfileId(),
                                    System.currentTimeMillis()
                            );
                        }
                    }
                });
                mainThreadHandler.post(callback::onSuccess);
            } catch (Exception exception) {
                mainThreadHandler.post(() -> callback.onError(exception));
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
