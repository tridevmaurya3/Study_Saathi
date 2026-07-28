package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;

import java.io.File;

public final class LocalAccountDataRepository {

    private final Context context;
    private final StudySaathiDatabase database;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public LocalAccountDataRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.database = StudySaathiDatabase.getInstance(this.context);
    }

    public void permanentlyDeleteAll(@NonNull Callback callback) {
        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                database.clearAllTables();
                clearSharedPreferences();
                mainHandler.post(callback::onSuccess);
            } catch (Exception exception) {
                mainHandler.post(() -> callback.onError(exception));
            }
        });
    }

    private void clearSharedPreferences() {
        File preferencesDirectory =
                new File(context.getApplicationInfo().dataDir, "shared_prefs");
        File[] preferenceFiles = preferencesDirectory.listFiles(
                (directory, name) -> name.endsWith(".xml")
        );
        if (preferenceFiles == null) {
            return;
        }
        for (File preferenceFile : preferenceFiles) {
            String name = preferenceFile.getName();
            name = name.substring(0, name.length() - 4);
            String normalizedName = name.toLowerCase(java.util.Locale.ROOT);
            if (normalizedName.startsWith("com.google")
                    || normalizedName.contains("firebase")
                    || normalizedName.contains("google_app_measurement")) {
                continue;
            }
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit();
        }
    }

    public interface Callback {
        void onSuccess();
        void onError(@NonNull Exception exception);
    }
}
