package com.tridev.studysaathi.data.content.setup;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;

/** Reads existing chapter/content tables without changing their schema or data. */
public final class SchoolBookContentProgressRepository {

    @NonNull
    private final StudySaathiDatabase database;

    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SchoolBookContentProgressRepository(@NonNull Context context) {
        database = StudySaathiDatabase.getInstance(
                context.getApplicationContext()
        );
    }

    public void getProgress(long bookRowId, @NonNull Callback callback) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (bookRowId <= 0L) {
                    throw new IllegalArgumentException(
                            "A valid book row ID is required."
                    );
                }

                int chapterCount = database.schoolBookChapterDao()
                        .getChaptersForBook(bookRowId).size();

                int contentCount = database.schoolBookChapterContentDao()
                        .getContentsForBook(bookRowId).size();

                int safeContentCount = Math.min(chapterCount, contentCount);

                mainHandler.post(() -> callback.onSuccess(
                        chapterCount,
                        safeContentCount
                ));
            } catch (Exception exception) {
                mainHandler.post(() -> callback.onError(exception));
            }
        });
    }

    public interface Callback {
        void onSuccess(int chapterCount, int contentCount);
        void onError(@NonNull Exception exception);
    }
}
