package com.tridev.studysaathi;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.cloud.CloudBackupRestoreCoordinator;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.databinding.ActivityBackupRestoreBinding;
import com.tridev.studysaathi.reminder.StudyReminderScheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BackupRestoreActivity
        extends AppCompatActivity {

    public static final String EXTRA_INTERNAL_BACKUP_PATH =
            "com.tridev.studysaathi.extra.INTERNAL_BACKUP_PATH";

    public static final String EXTRA_INTERNAL_BACKUP_DISPLAY_NAME =
            "com.tridev.studysaathi.extra.INTERNAL_BACKUP_DISPLAY_NAME";

    private static final String BACKUP_FORMAT =
            "study_saathi_backup";

    private static final int BACKUP_FORMAT_VERSION = 1;
    private static final int DATABASE_SCHEMA_VERSION = 5;

    private static final long MAX_BACKUP_FILE_SIZE =
            25L * 1024L * 1024L;

    private static final String BACKUP_STATE_PREFERENCES =
            "study_saathi_backup_state";

    private static final String KEY_LAST_RESTORE_AT =
            "last_restore_at";

    private static final String SAFETY_BACKUP_FOLDER =
            "safety_backups";

    private static final int MAX_SAFETY_BACKUPS = 3;

    private static final String[] DATABASE_TABLES = {
            "student_profiles",
            "lesson_progress",
            "quiz_attempts",
            "doubt_history"
    };

    private ActivityBackupRestoreBinding binding;

    private StudySaathiDatabase database;

    private CloudBackupRestoreCoordinator
            cloudBackupRestoreCoordinator;

    private ActivityResultLauncher<String[]>
            openBackupDocumentLauncher;

    private JSONObject selectedBackupJson;
    private BackupPreview selectedBackupPreview;

    private String selectedBackupFileName = "";

    private String selectedManagedCloudBackupPath = "";

    private boolean operationInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityBackupRestoreBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        database =
                StudySaathiDatabase.getInstance(this);

        cloudBackupRestoreCoordinator =
                new CloudBackupRestoreCoordinator(this);

        registerOpenDocumentLauncher();
        setupClickListeners();

        showNoSelectedFileState();
        showLastRestoreStatus();

        handleIncomingManagedCloudBackup();
    }

    private void registerOpenDocumentLauncher() {
        openBackupDocumentLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .OpenDocument(),
                        selectedUri -> {
                            if (selectedUri == null) {
                                Snackbar.make(
                                        binding.getRoot(),
                                        R.string.backup_restore_selection_cancelled,
                                        Snackbar.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            deleteSelectedManagedCloudBackup();

                            try {
                                getContentResolver()
                                        .takePersistableUriPermission(
                                                selectedUri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        );

                            } catch (SecurityException ignored) {
                                // Immediate read permission is sufficient.
                            }

                            readAndValidateBackup(
                                    selectedUri
                            );
                        }
                );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonChooseBackupFile.setOnClickListener(view -> {
            if (operationInProgress) {
                return;
            }

            openBackupDocumentLauncher.launch(
                    new String[]{
                            "application/json",
                            "text/plain",
                            "application/octet-stream"
                    }
            );
        });

        binding.buttonRestoreBackup.setOnClickListener(view -> {
            if (operationInProgress
                    || selectedBackupJson == null
                    || selectedBackupPreview == null) {
                return;
            }

            showRestoreConfirmation();
        });
    }

    private void handleIncomingManagedCloudBackup() {
        Intent incomingIntent =
                getIntent();

        if (incomingIntent == null) {
            return;
        }

        String internalBackupPath =
                incomingIntent.getStringExtra(
                        EXTRA_INTERNAL_BACKUP_PATH
                );

        if (internalBackupPath == null
                || internalBackupPath
                .trim()
                .isEmpty()) {
            return;
        }

        String displayFileName =
                incomingIntent.getStringExtra(
                        EXTRA_INTERNAL_BACKUP_DISPLAY_NAME
                );

        if (displayFileName == null
                || displayFileName
                .trim()
                .isEmpty()) {

            displayFileName =
                    new File(
                            internalBackupPath
                    ).getName();
        }

        readAndValidateManagedCloudBackup(
                internalBackupPath.trim(),
                displayFileName.trim()
        );
    }

    private void readAndValidateManagedCloudBackup(
            @NonNull String internalBackupPath,
            @NonNull String displayFileName
    ) {
        selectedBackupJson = null;
        selectedBackupPreview = null;
        selectedManagedCloudBackupPath = "";

        showOperationState(
                true,
                R.string.backup_restore_validating_button
        );

        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    File managedBackupFile =
                            new File(
                                    internalBackupPath
                            );

                    try {
                        if (!cloudBackupRestoreCoordinator
                                .isManagedCacheFile(
                                        managedBackupFile
                                )) {

                            throw new BackupValidationException(
                                    getString(
                                            R.string.backup_restore_invalid_file
                                    )
                            );
                        }

                        String backupText =
                                readTextFromManagedFile(
                                        managedBackupFile
                                );

                        JSONObject backupJson =
                                new JSONObject(
                                        backupText
                                );

                        BackupPreview backupPreview =
                                validateAndCreatePreview(
                                        backupJson
                                );

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            selectedBackupJson =
                                    backupJson;

                            selectedBackupPreview =
                                    backupPreview;

                            selectedBackupFileName =
                                    displayFileName;

                            selectedManagedCloudBackupPath =
                                    managedBackupFile
                                            .getAbsolutePath();

                            showOperationState(
                                    false,
                                    R.string.backup_restore_restore_button
                            );

                            showBackupPreview(
                                    displayFileName,
                                    backupPreview
                            );
                        });

                    } catch (BackupValidationException exception) {
                        cloudBackupRestoreCoordinator
                                .deletePreparedBackup(
                                        internalBackupPath
                                );

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    R.string.backup_restore_restore_button
                            );

                            showNoSelectedFileState();

                            Snackbar.make(
                                    binding.getRoot(),
                                    exception.getMessage() == null
                                            ? getString(
                                            R.string.backup_restore_invalid_file
                                    )
                                            : exception.getMessage(),
                                    Snackbar.LENGTH_LONG
                            ).show();
                        });

                    } catch (Exception exception) {
                        cloudBackupRestoreCoordinator
                                .deletePreparedBackup(
                                        internalBackupPath
                                );

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    R.string.backup_restore_restore_button
                            );

                            showNoSelectedFileState();

                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.backup_restore_read_failed,
                                    Snackbar.LENGTH_LONG
                            ).show();
                        });
                    }
                });
    }

    @NonNull
    private String readTextFromManagedFile(
            @NonNull File backupFile
    ) throws IOException {

        if (!backupFile.exists()
                || !backupFile.isFile()) {

            throw new IOException(
                    "Cloud backup cache file is unavailable."
            );
        }

        if (backupFile.length() <= 0L) {
            throw new BackupValidationException(
                    getString(
                            R.string.backup_restore_invalid_file
                    )
            );
        }

        if (backupFile.length()
                > MAX_BACKUP_FILE_SIZE) {

            throw new BackupValidationException(
                    getString(
                            R.string.backup_restore_file_too_large
                    )
            );
        }

        try (InputStream inputStream =
                     new FileInputStream(
                             backupFile
                     );

             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;
            long totalBytes = 0L;

            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {

                totalBytes += bytesRead;

                if (totalBytes
                        > MAX_BACKUP_FILE_SIZE) {

                    throw new BackupValidationException(
                            getString(
                                    R.string.backup_restore_file_too_large
                            )
                    );
                }

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return outputStream.toString(
                    StandardCharsets.UTF_8.name()
            );
        }
    }

    private void readAndValidateBackup(
            @NonNull Uri selectedUri
    ) {
        selectedBackupJson = null;
        selectedBackupPreview = null;
        selectedManagedCloudBackupPath = "";

        showOperationState(
                true,
                R.string.backup_restore_validating_button
        );

        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        String backupText =
                                readTextFromUri(
                                        selectedUri
                                );

                        JSONObject backupJson =
                                new JSONObject(
                                        backupText
                                );

                        BackupPreview backupPreview =
                                validateAndCreatePreview(
                                        backupJson
                                );

                        String displayName =
                                getDisplayName(
                                        selectedUri
                                );

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            selectedBackupJson =
                                    backupJson;

                            selectedBackupPreview =
                                    backupPreview;

                            selectedBackupFileName =
                                    displayName;

                            selectedManagedCloudBackupPath =
                                    "";

                            showOperationState(
                                    false,
                                    R.string.backup_restore_restore_button
                            );

                            showBackupPreview(
                                    displayName,
                                    backupPreview
                            );
                        });

                    } catch (BackupValidationException exception) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    R.string.backup_restore_restore_button
                            );

                            showNoSelectedFileState();

                            Snackbar.make(
                                    binding.getRoot(),
                                    exception.getMessage() == null
                                            ? getString(
                                            R.string.backup_restore_invalid_file
                                    )
                                            : exception.getMessage(),
                                    Snackbar.LENGTH_LONG
                            ).show();
                        });

                    } catch (Exception exception) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    R.string.backup_restore_restore_button
                            );

                            showNoSelectedFileState();

                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.backup_restore_read_failed,
                                    Snackbar.LENGTH_LONG
                            ).show();
                        });
                    }
                });
    }

    @NonNull
    private String readTextFromUri(
            @NonNull Uri selectedUri
    ) throws IOException {

        InputStream inputStream =
                getContentResolver()
                        .openInputStream(
                                selectedUri
                        );

        if (inputStream == null) {
            throw new IOException(
                    "Unable to open backup file."
            );
        }

        try (InputStream safeInputStream =
                     inputStream;

             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;
            long totalBytes = 0L;

            while ((bytesRead =
                    safeInputStream.read(buffer)) != -1) {

                totalBytes += bytesRead;

                if (totalBytes
                        > MAX_BACKUP_FILE_SIZE) {

                    throw new BackupValidationException(
                            getString(
                                    R.string.backup_restore_file_too_large
                            )
                    );
                }

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return outputStream.toString(
                    StandardCharsets.UTF_8.name()
            );
        }
    }

    @NonNull
    private BackupPreview validateAndCreatePreview(
            @NonNull JSONObject backupJson
    ) throws JSONException,
            BackupValidationException {

        String backupFormat =
                backupJson.optString(
                        "backup_format",
                        ""
                );

        if (!BACKUP_FORMAT.equals(
                backupFormat
        )) {
            throw new BackupValidationException(
                    getString(
                            R.string.backup_restore_invalid_format
                    )
            );
        }

        int backupFormatVersion =
                backupJson.optInt(
                        "backup_format_version",
                        -1
                );

        if (backupFormatVersion
                != BACKUP_FORMAT_VERSION) {

            throw new BackupValidationException(
                    getString(
                            R.string.backup_restore_unsupported_format
                    )
            );
        }

        int schemaVersion =
                backupJson.optInt(
                        "database_schema_version",
                        -1
                );

        if (schemaVersion
                != DATABASE_SCHEMA_VERSION) {

            throw new BackupValidationException(
                    getString(
                            R.string.backup_restore_schema_mismatch_format,
                            schemaVersion,
                            DATABASE_SCHEMA_VERSION
                    )
            );
        }

        String packageName =
                backupJson.optString(
                        "package_name",
                        ""
                );

        if (!getPackageName().equals(
                packageName
        )) {
            throw new BackupValidationException(
                    getString(
                            R.string.backup_restore_different_app
                    )
            );
        }

        JSONObject databaseObject =
                backupJson.getJSONObject(
                        "database"
                );

        JSONArray tableArray =
                databaseObject.getJSONArray(
                        "tables"
                );

        Map<String, JSONObject> tableObjects =
                createTableMap(
                        tableArray
                );

        for (String requiredTable
                : DATABASE_TABLES) {

            if (!tableObjects.containsKey(
                    requiredTable
            )) {
                throw new BackupValidationException(
                        getString(
                                R.string.backup_restore_missing_table_format,
                                requiredTable
                        )
                );
            }
        }

        validateTableRows(
                "student_profiles",
                getRows(
                        tableObjects,
                        "student_profiles"
                )
        );

        validateTableRows(
                "lesson_progress",
                getRows(
                        tableObjects,
                        "lesson_progress"
                )
        );

        validateTableRows(
                "quiz_attempts",
                getRows(
                        tableObjects,
                        "quiz_attempts"
                )
        );

        validateTableRows(
                "doubt_history",
                getRows(
                        tableObjects,
                        "doubt_history"
                )
        );

        JSONArray preferencesArray =
                backupJson.getJSONArray(
                        "shared_preferences"
                );

        validatePreferences(
                preferencesArray
        );

        JSONArray profileRows =
                getRows(
                        tableObjects,
                        "student_profiles"
                );

        int activeProfileCount = 0;

        for (int index = 0;
             index < profileRows.length();
             index++) {

            JSONObject profileRow =
                    profileRows.getJSONObject(
                            index
                    );

            if (profileRow.optInt(
                    "is_active",
                    0
            ) == 1) {
                activeProfileCount++;
            }
        }

        if (activeProfileCount > 1) {
            throw new BackupValidationException(
                    getString(
                            R.string.backup_restore_multiple_active_profiles
                    )
            );
        }

        int preferenceEntryCount =
                countPreferenceEntries(
                        preferencesArray
                );

        long createdAt =
                backupJson.optLong(
                        "created_at",
                        0L
                );

        return new BackupPreview(
                profileRows.length(),
                getRows(
                        tableObjects,
                        "lesson_progress"
                ).length(),
                getRows(
                        tableObjects,
                        "quiz_attempts"
                ).length(),
                getRows(
                        tableObjects,
                        "doubt_history"
                ).length(),
                preferenceEntryCount,
                createdAt
        );
    }

    @NonNull
    private Map<String, JSONObject> createTableMap(
            @NonNull JSONArray tableArray
    ) throws JSONException,
            BackupValidationException {

        Map<String, JSONObject> tableObjects =
                new HashMap<>();

        for (int index = 0;
             index < tableArray.length();
             index++) {

            JSONObject tableObject =
                    tableArray.getJSONObject(
                            index
                    );

            String tableName =
                    tableObject.optString(
                            "table_name",
                            ""
                    );

            if (tableName.isEmpty()) {
                throw new BackupValidationException(
                        getString(
                                R.string.backup_restore_invalid_file
                        )
                );
            }

            if (tableObjects.containsKey(
                    tableName
            )) {
                throw new BackupValidationException(
                        getString(
                                R.string.backup_restore_duplicate_table_format,
                                tableName
                        )
                );
            }

            tableObjects.put(
                    tableName,
                    tableObject
            );
        }

        return tableObjects;
    }

    @NonNull
    private JSONArray getRows(
            @NonNull Map<String, JSONObject> tableObjects,
            @NonNull String tableName
    ) throws JSONException {

        JSONObject tableObject =
                tableObjects.get(
                        tableName
                );

        if (tableObject == null) {
            throw new JSONException(
                    "Missing table: "
                            + tableName
            );
        }

        return tableObject.getJSONArray(
                "rows"
        );
    }

    private void validateTableRows(
            @NonNull String tableName,
            @NonNull JSONArray rows
    ) throws JSONException,
            BackupValidationException {

        String[] requiredColumns =
                getRequiredColumns(
                        tableName
                );

        for (int rowIndex = 0;
             rowIndex < rows.length();
             rowIndex++) {

            JSONObject row =
                    rows.getJSONObject(
                            rowIndex
                    );

            for (String requiredColumn
                    : requiredColumns) {

                if (!row.has(requiredColumn)
                        || row.isNull(
                        requiredColumn
                )) {
                    throw new BackupValidationException(
                            getString(
                                    R.string.backup_restore_missing_column_format,
                                    tableName,
                                    requiredColumn
                            )
                    );
                }
            }
        }
    }

    @NonNull
    private String[] getRequiredColumns(
            @NonNull String tableName
    ) throws BackupValidationException {

        switch (tableName) {
            case "student_profiles":
                return new String[]{
                        "profile_id",
                        "student_name",
                        "education_board",
                        "student_class",
                        "study_medium",
                        "explanation_language",
                        "is_active",
                        "created_at",
                        "updated_at"
                };

            case "lesson_progress":
                return new String[]{
                        "progress_key",
                        "profile_id",
                        "education_board",
                        "student_class",
                        "subject_name",
                        "chapter_title",
                        "progress_percent",
                        "is_completed",
                        "last_studied_at",
                        "completed_at",
                        "revision_count",
                        "last_revised_at"
                };

            case "quiz_attempts":
                return new String[]{
                        "attempt_id",
                        "profile_id",
                        "education_board",
                        "student_class",
                        "subject_name",
                        "chapter_title",
                        "correct_answers",
                        "total_questions",
                        "percentage",
                        "attempted_at"
                };

            case "doubt_history":
                return new String[]{
                        "history_id",
                        "profile_id",
                        "education_board",
                        "student_class",
                        "subject_name",
                        "chapter_title",
                        "question_text",
                        "answer_text",
                        "explanation_language",
                        "created_at"
                };

            default:
                throw new BackupValidationException(
                        getString(
                                R.string.backup_restore_invalid_file
                        )
                );
        }
    }

    private void validatePreferences(
            @NonNull JSONArray preferencesArray
    ) throws JSONException,
            BackupValidationException {

        for (int fileIndex = 0;
             fileIndex < preferencesArray.length();
             fileIndex++) {

            JSONObject preferenceFile =
                    preferencesArray.getJSONObject(
                            fileIndex
                    );

            String preferenceName =
                    preferenceFile.optString(
                            "preference_name",
                            ""
                    );

            if (preferenceName.isEmpty()
                    || !preferenceFile.has(
                    "entries"
            )) {
                throw new BackupValidationException(
                        getString(
                                R.string.backup_restore_invalid_preferences
                        )
                );
            }

            JSONObject entries =
                    preferenceFile.getJSONObject(
                            "entries"
                    );

            JSONArray keys =
                    entries.names();

            if (keys == null) {
                continue;
            }

            for (int keyIndex = 0;
                 keyIndex < keys.length();
                 keyIndex++) {

                String key =
                        keys.getString(
                                keyIndex
                        );

                JSONObject valueObject =
                        entries.getJSONObject(
                                key
                        );

                if (!valueObject.has("type")
                        || !valueObject.has(
                        "value"
                )) {
                    throw new BackupValidationException(
                            getString(
                                    R.string.backup_restore_invalid_preferences
                            )
                    );
                }
            }
        }
    }

    private int countPreferenceEntries(
            @NonNull JSONArray preferencesArray
    ) throws JSONException {

        int entryCount = 0;

        for (int index = 0;
             index < preferencesArray.length();
             index++) {

            JSONObject preferenceFile =
                    preferencesArray.getJSONObject(
                            index
                    );

            if (!isRestorablePreferenceName(
                    preferenceFile.optString(
                            "preference_name",
                            ""
                    )
            )) {
                continue;
            }

            JSONObject entries =
                    preferenceFile.getJSONObject(
                            "entries"
                    );

            entryCount += entries.length();
        }

        return entryCount;
    }

    private void showRestoreConfirmation() {
        BackupPreview preview =
                selectedBackupPreview;

        if (preview == null) {
            return;
        }

        String confirmationMessage =
                getString(
                        R.string.backup_restore_confirmation_message,
                        preview.profileCount,
                        preview.lessonProgressCount,
                        preview.quizAttemptCount,
                        preview.doubtCount,
                        preview.preferenceEntryCount
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.backup_restore_confirmation_title
                )
                .setMessage(
                        confirmationMessage
                )
                .setNegativeButton(
                        R.string.backup_restore_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.backup_restore_confirm_action,
                        (dialog, which) ->
                                restoreSelectedBackup()
                )
                .show();
    }

    private void restoreSelectedBackup() {
        JSONObject backupJson =
                selectedBackupJson;

        if (backupJson == null) {
            return;
        }

        showOperationState(
                true,
                R.string.backup_restore_restoring_button
        );

        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {

                    JSONObject safetyBackup = null;
                    File safetyBackupFile = null;

                    try {
                        safetyBackup =
                                createCompleteBackup();

                        safetyBackupFile =
                                writeInternalSafetyBackup(
                                        safetyBackup
                                );

                        applyCompleteBackup(
                                backupJson
                        );

                        applyRestoredReminderSchedule();

                        long restoredAt =
                                System.currentTimeMillis();

                        getSharedPreferences(
                                BACKUP_STATE_PREFERENCES,
                                MODE_PRIVATE
                        )
                                .edit()
                                .putLong(
                                        KEY_LAST_RESTORE_AT,
                                        restoredAt
                                )
                                .commit();

                        deleteSelectedManagedCloudBackup();

                        File finalSafetyBackupFile =
                                safetyBackupFile;

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    R.string.backup_restore_restore_button
                            );

                            showLastRestoreStatus(
                                    restoredAt
                            );

                            showRestoreSuccessDialog(
                                    finalSafetyBackupFile
                            );
                        });

                    } catch (Exception restoreException) {

                        boolean rollbackSuccessful =
                                false;

                        if (safetyBackup != null) {
                            try {
                                applyCompleteBackup(
                                        safetyBackup
                                );

                                applyRestoredReminderSchedule();

                                rollbackSuccessful = true;

                            } catch (Exception ignored) {
                                rollbackSuccessful = false;
                            }
                        }

                        boolean finalRollbackSuccessful =
                                rollbackSuccessful;

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    R.string.backup_restore_restore_button
                            );

                            Snackbar.make(
                                    binding.getRoot(),
                                    finalRollbackSuccessful
                                            ? R.string.backup_restore_failed_rolled_back
                                            : R.string.backup_restore_failed_manual_recovery,
                                    Snackbar.LENGTH_LONG
                            ).show();
                        });
                    }
                });
    }

    private void deleteSelectedManagedCloudBackup() {
        if (selectedManagedCloudBackupPath
                == null
                || selectedManagedCloudBackupPath
                .trim()
                .isEmpty()) {

            selectedManagedCloudBackupPath = "";
            return;
        }

        cloudBackupRestoreCoordinator
                .deletePreparedBackup(
                        selectedManagedCloudBackupPath
                );

        selectedManagedCloudBackupPath = "";
    }

    private void applyCompleteBackup(
            @NonNull JSONObject backupJson
    ) throws JSONException,
            IOException {

        JSONObject databaseObject =
                backupJson.getJSONObject(
                        "database"
                );

        JSONArray preferencesArray =
                backupJson.getJSONArray(
                        "shared_preferences"
                );

        restoreDatabase(
                databaseObject
        );

        restoreSharedPreferences(
                preferencesArray
        );
    }

    private void restoreDatabase(
            @NonNull JSONObject databaseObject
    ) throws JSONException {

        JSONArray tableArray =
                databaseObject.getJSONArray(
                        "tables"
                );

        Map<String, JSONObject> tableObjects =
                createTableMapForRestore(
                        tableArray
                );

        SupportSQLiteDatabase writableDatabase =
                database.getOpenHelper()
                        .getWritableDatabase();

        writableDatabase.beginTransaction();

        try {
            writableDatabase.execSQL(
                    "DELETE FROM `doubt_history`"
            );

            writableDatabase.execSQL(
                    "DELETE FROM `quiz_attempts`"
            );

            writableDatabase.execSQL(
                    "DELETE FROM `lesson_progress`"
            );

            writableDatabase.execSQL(
                    "DELETE FROM `student_profiles`"
            );

            insertTableRows(
                    writableDatabase,
                    "student_profiles",
                    tableObjects
            );

            insertTableRows(
                    writableDatabase,
                    "lesson_progress",
                    tableObjects
            );

            insertTableRows(
                    writableDatabase,
                    "quiz_attempts",
                    tableObjects
            );

            insertTableRows(
                    writableDatabase,
                    "doubt_history",
                    tableObjects
            );

            resetAutoIncrementSequence(
                    writableDatabase,
                    "student_profiles",
                    "profile_id"
            );

            resetAutoIncrementSequence(
                    writableDatabase,
                    "quiz_attempts",
                    "attempt_id"
            );

            resetAutoIncrementSequence(
                    writableDatabase,
                    "doubt_history",
                    "history_id"
            );

            writableDatabase.setTransactionSuccessful();

        } finally {
            writableDatabase.endTransaction();
        }
    }

    @NonNull
    private Map<String, JSONObject> createTableMapForRestore(
            @NonNull JSONArray tableArray
    ) throws JSONException {

        Map<String, JSONObject> tableObjects =
                new HashMap<>();

        for (int index = 0;
             index < tableArray.length();
             index++) {

            JSONObject tableObject =
                    tableArray.getJSONObject(
                            index
                    );

            tableObjects.put(
                    tableObject.getString(
                            "table_name"
                    ),
                    tableObject
            );
        }

        return tableObjects;
    }

    private void insertTableRows(
            @NonNull SupportSQLiteDatabase writableDatabase,
            @NonNull String tableName,
            @NonNull Map<String, JSONObject> tableObjects
    ) throws JSONException {

        JSONObject tableObject =
                tableObjects.get(
                        tableName
                );

        if (tableObject == null) {
            throw new JSONException(
                    "Missing table: "
                            + tableName
            );
        }

        JSONArray rows =
                tableObject.getJSONArray(
                        "rows"
                );

        for (int index = 0;
             index < rows.length();
             index++) {

            JSONObject row =
                    rows.getJSONObject(
                            index
                    );

            ContentValues contentValues =
                    createContentValues(
                            tableName,
                            row
                    );

            long insertedRow =
                    writableDatabase.insert(
                            tableName,
                            SQLiteDatabase.CONFLICT_ABORT,
                            contentValues
                    );

            if (insertedRow == -1L) {
                throw new JSONException(
                        "Unable to restore "
                                + tableName
                                + " row "
                                + index
                );
            }
        }
    }

    @NonNull
    private ContentValues createContentValues(
            @NonNull String tableName,
            @NonNull JSONObject row
    ) throws JSONException {

        ContentValues values =
                new ContentValues();

        switch (tableName) {
            case "student_profiles":
                values.put(
                        "profile_id",
                        row.getLong("profile_id")
                );

                values.put(
                        "student_name",
                        row.getString("student_name")
                );

                values.put(
                        "education_board",
                        row.getString("education_board")
                );

                values.put(
                        "student_class",
                        row.getString("student_class")
                );

                values.put(
                        "study_medium",
                        row.getString("study_medium")
                );

                values.put(
                        "explanation_language",
                        row.getString(
                                "explanation_language"
                        )
                );

                values.put(
                        "is_active",
                        row.getInt("is_active")
                );

                values.put(
                        "created_at",
                        row.getLong("created_at")
                );

                values.put(
                        "updated_at",
                        row.getLong("updated_at")
                );
                break;

            case "lesson_progress":
                values.put(
                        "progress_key",
                        row.getString("progress_key")
                );

                values.put(
                        "profile_id",
                        row.getLong("profile_id")
                );

                values.put(
                        "education_board",
                        row.getString("education_board")
                );

                values.put(
                        "student_class",
                        row.getString("student_class")
                );

                values.put(
                        "subject_name",
                        row.getString("subject_name")
                );

                values.put(
                        "chapter_title",
                        row.getString("chapter_title")
                );

                values.put(
                        "progress_percent",
                        row.getInt("progress_percent")
                );

                values.put(
                        "is_completed",
                        row.getInt("is_completed")
                );

                values.put(
                        "last_studied_at",
                        row.getLong("last_studied_at")
                );

                values.put(
                        "completed_at",
                        row.getLong("completed_at")
                );

                values.put(
                        "revision_count",
                        row.getInt("revision_count")
                );

                values.put(
                        "last_revised_at",
                        row.getLong("last_revised_at")
                );
                break;

            case "quiz_attempts":
                values.put(
                        "attempt_id",
                        row.getLong("attempt_id")
                );

                values.put(
                        "profile_id",
                        row.getLong("profile_id")
                );

                values.put(
                        "education_board",
                        row.getString("education_board")
                );

                values.put(
                        "student_class",
                        row.getString("student_class")
                );

                values.put(
                        "subject_name",
                        row.getString("subject_name")
                );

                values.put(
                        "chapter_title",
                        row.getString("chapter_title")
                );

                values.put(
                        "correct_answers",
                        row.getInt("correct_answers")
                );

                values.put(
                        "total_questions",
                        row.getInt("total_questions")
                );

                values.put(
                        "percentage",
                        row.getInt("percentage")
                );

                values.put(
                        "attempted_at",
                        row.getLong("attempted_at")
                );
                break;

            case "doubt_history":
                values.put(
                        "history_id",
                        row.getLong("history_id")
                );

                values.put(
                        "profile_id",
                        row.getLong("profile_id")
                );

                values.put(
                        "education_board",
                        row.getString("education_board")
                );

                values.put(
                        "student_class",
                        row.getString("student_class")
                );

                values.put(
                        "subject_name",
                        row.getString("subject_name")
                );

                values.put(
                        "chapter_title",
                        row.getString("chapter_title")
                );

                values.put(
                        "question_text",
                        row.getString("question_text")
                );

                values.put(
                        "answer_text",
                        row.getString("answer_text")
                );

                values.put(
                        "explanation_language",
                        row.getString(
                                "explanation_language"
                        )
                );

                values.put(
                        "created_at",
                        row.getLong("created_at")
                );
                break;

            default:
                throw new JSONException(
                        "Unsupported table: "
                                + tableName
                );
        }

        return values;
    }

    private void resetAutoIncrementSequence(
            @NonNull SupportSQLiteDatabase writableDatabase,
            @NonNull String tableName,
            @NonNull String idColumn
    ) {
        writableDatabase.execSQL(
                "DELETE FROM sqlite_sequence "
                        + "WHERE name = '"
                        + tableName
                        + "'"
        );

        writableDatabase.execSQL(
                "INSERT INTO sqlite_sequence(name, seq) "
                        + "SELECT '"
                        + tableName
                        + "', COALESCE(MAX(`"
                        + idColumn
                        + "`), 0) FROM `"
                        + tableName
                        + "`"
        );
    }

    private void restoreSharedPreferences(
            @NonNull JSONArray preferencesArray
    ) throws JSONException,
            IOException {

        clearCurrentStudySaathiPreferences();

        for (int fileIndex = 0;
             fileIndex < preferencesArray.length();
             fileIndex++) {

            JSONObject preferenceFile =
                    preferencesArray.getJSONObject(
                            fileIndex
                    );

            String preferenceName =
                    preferenceFile.getString(
                            "preference_name"
                    );

            if (!isRestorablePreferenceName(
                    preferenceName
            )) {
                continue;
            }

            JSONObject entries =
                    preferenceFile.getJSONObject(
                            "entries"
                    );

            SharedPreferences.Editor editor =
                    getSharedPreferences(
                            preferenceName,
                            MODE_PRIVATE
                    ).edit();

            JSONArray keys =
                    entries.names();

            if (keys != null) {
                for (int keyIndex = 0;
                     keyIndex < keys.length();
                     keyIndex++) {

                    String key =
                            keys.getString(
                                    keyIndex
                            );

                    JSONObject valueObject =
                            entries.getJSONObject(
                                    key
                            );

                    restorePreferenceValue(
                            editor,
                            key,
                            valueObject
                    );
                }
            }

            if (!editor.commit()) {
                throw new IOException(
                        "Unable to restore preference: "
                                + preferenceName
                );
            }
        }
    }

    private void clearCurrentStudySaathiPreferences()
            throws IOException {

        for (File preferenceFile
                : getSharedPreferenceFiles()) {

            String preferenceName =
                    removeXmlExtension(
                            preferenceFile.getName()
                    );

            if (!isRestorablePreferenceName(
                    preferenceName
            )) {
                continue;
            }

            boolean cleared =
                    getSharedPreferences(
                            preferenceName,
                            MODE_PRIVATE
                    )
                            .edit()
                            .clear()
                            .commit();

            if (!cleared) {
                throw new IOException(
                        "Unable to clear preference: "
                                + preferenceName
                );
            }
        }
    }

    private void restorePreferenceValue(
            @NonNull SharedPreferences.Editor editor,
            @NonNull String key,
            @NonNull JSONObject valueObject
    ) throws JSONException {

        String valueType =
                valueObject.getString(
                        "type"
                );

        Object rawValue =
                valueObject.get(
                        "value"
                );

        switch (valueType) {
            case "string":
                editor.putString(
                        key,
                        rawValue == JSONObject.NULL
                                ? ""
                                : String.valueOf(
                                rawValue
                        )
                );
                break;

            case "integer":
                editor.putInt(
                        key,
                        ((Number) rawValue)
                                .intValue()
                );
                break;

            case "long":
                editor.putLong(
                        key,
                        ((Number) rawValue)
                                .longValue()
                );
                break;

            case "float":
                editor.putFloat(
                        key,
                        ((Number) rawValue)
                                .floatValue()
                );
                break;

            case "boolean":
                editor.putBoolean(
                        key,
                        (Boolean) rawValue
                );
                break;

            case "string_set":
                JSONArray stringSetArray =
                        (JSONArray) rawValue;

                Set<String> stringSet =
                        new HashSet<>();

                for (int index = 0;
                     index < stringSetArray.length();
                     index++) {

                    stringSet.add(
                            stringSetArray.getString(
                                    index
                            )
                    );
                }

                editor.putStringSet(
                        key,
                        stringSet
                );
                break;

            case "null":
                editor.remove(key);
                break;

            default:
                throw new JSONException(
                        "Unsupported preference type: "
                                + valueType
                );
        }
    }

    private void applyRestoredReminderSchedule() {
        boolean reminderEnabled =
                StudyReminderScheduler
                        .isReminderEnabled(this);

        if (reminderEnabled) {
            StudyReminderScheduler.scheduleReminder(
                    this,
                    StudyReminderScheduler
                            .getReminderHour(this),
                    StudyReminderScheduler
                            .getReminderMinute(this),
                    StudyReminderScheduler
                            .getReminderDaysMask(this)
            );

        } else {
            StudyReminderScheduler.cancelReminder(
                    this
            );
        }
    }

    @NonNull
    private JSONObject createCompleteBackup()
            throws JSONException {

        JSONObject rootObject =
                new JSONObject();

        long createdAt =
                System.currentTimeMillis();

        rootObject.put(
                "backup_format",
                BACKUP_FORMAT
        );

        rootObject.put(
                "backup_format_version",
                BACKUP_FORMAT_VERSION
        );

        rootObject.put(
                "database_schema_version",
                DATABASE_SCHEMA_VERSION
        );

        rootObject.put(
                "created_at",
                createdAt
        );

        rootObject.put(
                "created_at_iso",
                Instant.ofEpochMilli(createdAt)
                        .toString()
        );

        rootObject.put(
                "package_name",
                getPackageName()
        );

        rootObject.put(
                "database",
                exportDatabase()
        );

        rootObject.put(
                "shared_preferences",
                exportAllSharedPreferences()
        );

        return rootObject;
    }

    @NonNull
    private JSONObject exportDatabase()
            throws JSONException {

        JSONObject databaseObject =
                new JSONObject();

        databaseObject.put(
                "database_name",
                "study_saathi_database"
        );

        databaseObject.put(
                "schema_version",
                DATABASE_SCHEMA_VERSION
        );

        JSONArray tableArray =
                new JSONArray();

        for (String tableName
                : DATABASE_TABLES) {

            tableArray.put(
                    exportDatabaseTable(
                            tableName
                    )
            );
        }

        databaseObject.put(
                "tables",
                tableArray
        );

        return databaseObject;
    }

    @NonNull
    private JSONObject exportDatabaseTable(
            @NonNull String tableName
    ) throws JSONException {

        JSONObject tableObject =
                new JSONObject();

        tableObject.put(
                "table_name",
                tableName
        );

        JSONArray rowsArray =
                new JSONArray();

        try (Cursor cursor =
                     database.getOpenHelper()
                             .getReadableDatabase()
                             .query(
                                     "SELECT * FROM `"
                                             + tableName
                                             + "`"
                             )) {

            String[] columnNames =
                    cursor.getColumnNames();

            while (cursor.moveToNext()) {
                JSONObject rowObject =
                        new JSONObject();

                for (int columnIndex = 0;
                     columnIndex < columnNames.length;
                     columnIndex++) {

                    rowObject.put(
                            columnNames[columnIndex],
                            getCursorValue(
                                    cursor,
                                    columnIndex
                            )
                    );
                }

                rowsArray.put(
                        rowObject
                );
            }
        }

        tableObject.put(
                "row_count",
                rowsArray.length()
        );

        tableObject.put(
                "rows",
                rowsArray
        );

        return tableObject;
    }

    private Object getCursorValue(
            @NonNull Cursor cursor,
            int columnIndex
    ) {
        switch (cursor.getType(columnIndex)) {
            case Cursor.FIELD_TYPE_NULL:
                return JSONObject.NULL;

            case Cursor.FIELD_TYPE_INTEGER:
                return cursor.getLong(
                        columnIndex
                );

            case Cursor.FIELD_TYPE_FLOAT:
                return cursor.getDouble(
                        columnIndex
                );

            case Cursor.FIELD_TYPE_BLOB:
                return Base64.encodeToString(
                        cursor.getBlob(
                                columnIndex
                        ),
                        Base64.NO_WRAP
                );

            case Cursor.FIELD_TYPE_STRING:
            default:
                return cursor.getString(
                        columnIndex
                );
        }
    }

    @NonNull
    private JSONArray exportAllSharedPreferences()
            throws JSONException {

        JSONArray preferenceFilesArray =
                new JSONArray();

        for (File preferenceFile
                : getSharedPreferenceFiles()) {

            String preferenceName =
                    removeXmlExtension(
                            preferenceFile.getName()
                    );

            SharedPreferences sharedPreferences =
                    getSharedPreferences(
                            preferenceName,
                            MODE_PRIVATE
                    );

            JSONObject preferenceFileObject =
                    new JSONObject();

            preferenceFileObject.put(
                    "preference_name",
                    preferenceName
            );

            JSONObject entriesObject =
                    new JSONObject();

            Map<String, ?> preferenceValues =
                    sharedPreferences.getAll();

            List<String> sortedKeys =
                    new ArrayList<>(
                            preferenceValues.keySet()
                    );

            Collections.sort(
                    sortedKeys
            );

            for (String key : sortedKeys) {
                entriesObject.put(
                        key,
                        createPreferenceValueObject(
                                preferenceValues.get(
                                        key
                                )
                        )
                );
            }

            preferenceFileObject.put(
                    "entry_count",
                    entriesObject.length()
            );

            preferenceFileObject.put(
                    "entries",
                    entriesObject
            );

            preferenceFilesArray.put(
                    preferenceFileObject
            );
        }

        return preferenceFilesArray;
    }

    @NonNull
    private JSONObject createPreferenceValueObject(
            Object value
    ) throws JSONException {

        JSONObject valueObject =
                new JSONObject();

        if (value == null) {
            valueObject.put(
                    "type",
                    "null"
            );

            valueObject.put(
                    "value",
                    JSONObject.NULL
            );

            return valueObject;
        }

        if (value instanceof String) {
            valueObject.put(
                    "type",
                    "string"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Integer) {
            valueObject.put(
                    "type",
                    "integer"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Long) {
            valueObject.put(
                    "type",
                    "long"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Float) {
            valueObject.put(
                    "type",
                    "float"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Boolean) {
            valueObject.put(
                    "type",
                    "boolean"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Set<?>) {
            valueObject.put(
                    "type",
                    "string_set"
            );

            JSONArray setArray =
                    new JSONArray();

            List<String> sortedValues =
                    new ArrayList<>();

            for (Object setValue
                    : (Set<?>) value) {

                sortedValues.add(
                        String.valueOf(
                                setValue
                        )
                );
            }

            Collections.sort(
                    sortedValues
            );

            for (String setValue
                    : sortedValues) {

                setArray.put(
                        setValue
                );
            }

            valueObject.put(
                    "value",
                    setArray
            );

            return valueObject;
        }

        valueObject.put(
                "type",
                "string"
        );

        valueObject.put(
                "value",
                String.valueOf(value)
        );

        return valueObject;
    }

    @NonNull
    private File writeInternalSafetyBackup(
            @NonNull JSONObject safetyBackup
    ) throws IOException,
            JSONException {

        File safetyDirectory =
                new File(
                        getFilesDir(),
                        SAFETY_BACKUP_FOLDER
                );

        if (!safetyDirectory.exists()
                && !safetyDirectory.mkdirs()) {

            throw new IOException(
                    "Unable to create safety backup folder."
            );
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                );

        File safetyBackupFile =
                new File(
                        safetyDirectory,
                        "StudySaathi_PreRestore_"
                                + LocalDateTime.now()
                                .format(formatter)
                                + ".json"
                );

        try (OutputStreamWriter writer =
                     new OutputStreamWriter(
                             new FileOutputStream(
                                     safetyBackupFile
                             ),
                             StandardCharsets.UTF_8
                     )) {

            writer.write(
                    safetyBackup.toString(2)
            );

            writer.flush();
        }

        pruneOldSafetyBackups(
                safetyDirectory
        );

        return safetyBackupFile;
    }

    private void pruneOldSafetyBackups(
            @NonNull File safetyDirectory
    ) {
        File[] safetyFiles =
                safetyDirectory.listFiles(
                        (directory, fileName) ->
                                fileName != null
                                        && fileName.endsWith(
                                        ".json"
                                )
                );

        if (safetyFiles == null
                || safetyFiles.length
                <= MAX_SAFETY_BACKUPS) {
            return;
        }

        Arrays.sort(
                safetyFiles,
                (firstFile, secondFile) ->
                        Long.compare(
                                secondFile.lastModified(),
                                firstFile.lastModified()
                        )
        );

        for (int index = MAX_SAFETY_BACKUPS;
             index < safetyFiles.length;
             index++) {

            safetyFiles[index].delete();
        }
    }

    @NonNull
    private List<File> getSharedPreferenceFiles() {
        File sharedPreferencesDirectory =
                new File(
                        getApplicationInfo().dataDir,
                        "shared_prefs"
                );

        File[] preferenceFiles =
                sharedPreferencesDirectory.listFiles(
                        (directory, fileName) ->
                                fileName != null
                                        && fileName.endsWith(
                                        ".xml"
                                )
                );

        if (preferenceFiles == null
                || preferenceFiles.length == 0) {

            return new ArrayList<>();
        }

        Arrays.sort(
                preferenceFiles,
                Comparator.comparing(
                        File::getName
                )
        );

        return new ArrayList<>(
                Arrays.asList(
                        preferenceFiles
                )
        );
    }

    private boolean isRestorablePreferenceName(
            @NonNull String preferenceName
    ) {
        return preferenceName.startsWith(
                "study_saathi_"
        );
    }

    @NonNull
    private String removeXmlExtension(
            @NonNull String fileName
    ) {
        if (fileName.endsWith(".xml")
                && fileName.length() > 4) {

            return fileName.substring(
                    0,
                    fileName.length() - 4
            );
        }

        return fileName;
    }

    @NonNull
    private String getDisplayName(
            @NonNull Uri selectedUri
    ) {
        try (Cursor cursor =
                     getContentResolver().query(
                             selectedUri,
                             new String[]{
                                     OpenableColumns.DISPLAY_NAME
                             },
                             null,
                             null,
                             null
                     )) {

            if (cursor != null
                    && cursor.moveToFirst()) {

                int nameColumnIndex =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );

                if (nameColumnIndex >= 0) {
                    String displayName =
                            cursor.getString(
                                    nameColumnIndex
                            );

                    if (displayName != null
                            && !displayName.trim()
                            .isEmpty()) {

                        return displayName.trim();
                    }
                }
            }

        } catch (Exception ignored) {
            // Use URI fallback.
        }

        String lastPathSegment =
                selectedUri.getLastPathSegment();

        return lastPathSegment == null
                ? getString(
                R.string.backup_restore_unknown_file
        )
                : lastPathSegment;
    }

    private void showBackupPreview(
            @NonNull String fileName,
            @NonNull BackupPreview preview
    ) {
        binding.textSelectedBackupFile.setText(
                getString(
                        R.string.backup_restore_selected_file_format,
                        fileName
                )
        );

        binding.cardBackupPreview.setVisibility(
                View.VISIBLE
        );

        binding.buttonRestoreBackup.setVisibility(
                View.VISIBLE
        );

        binding.buttonRestoreBackup.setEnabled(
                true
        );

        binding.textRestoreProfiles.setText(
                String.valueOf(
                        preview.profileCount
                )
        );

        binding.textRestoreLessons.setText(
                String.valueOf(
                        preview.lessonProgressCount
                )
        );

        binding.textRestoreQuizzes.setText(
                String.valueOf(
                        preview.quizAttemptCount
                )
        );

        binding.textRestoreDoubts.setText(
                String.valueOf(
                        preview.doubtCount
                )
        );

        binding.textRestorePreferences.setText(
                String.valueOf(
                        preview.preferenceEntryCount
                )
        );

        binding.textBackupCreatedAt.setText(
                createBackupCreatedText(
                        preview.createdAt
                )
        );
    }

    @NonNull
    private String createBackupCreatedText(
            long timestamp
    ) {
        if (timestamp <= 0L) {
            return getString(
                    R.string.backup_restore_created_unknown
            );
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                );

        String formattedTime =
                Instant.ofEpochMilli(timestamp)
                        .atZone(
                                ZoneId.systemDefault()
                        )
                        .format(formatter);

        return getString(
                R.string.backup_restore_created_format,
                formattedTime
        );
    }

    private void showNoSelectedFileState() {
        selectedBackupJson = null;
        selectedBackupPreview = null;
        selectedBackupFileName = "";
        selectedManagedCloudBackupPath = "";

        binding.textSelectedBackupFile.setText(
                R.string.backup_restore_no_file_selected
        );

        binding.cardBackupPreview.setVisibility(
                View.GONE
        );

        binding.buttonRestoreBackup.setVisibility(
                View.GONE
        );
    }

    private void showLastRestoreStatus() {
        long lastRestoreAt =
                getSharedPreferences(
                        BACKUP_STATE_PREFERENCES,
                        MODE_PRIVATE
                )
                        .getLong(
                                KEY_LAST_RESTORE_AT,
                                0L
                        );

        showLastRestoreStatus(
                lastRestoreAt
        );
    }

    private void showLastRestoreStatus(
            long timestamp
    ) {
        if (timestamp <= 0L) {
            binding.textLastRestore.setText(
                    R.string.backup_restore_last_none
            );

            return;
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                );

        String formattedTime =
                Instant.ofEpochMilli(timestamp)
                        .atZone(
                                ZoneId.systemDefault()
                        )
                        .format(formatter);

        binding.textLastRestore.setText(
                getString(
                        R.string.backup_restore_last_format,
                        formattedTime
                )
        );
    }

    private void showRestoreSuccessDialog(
            File safetyBackupFile
    ) {
        String safetyFileName =
                safetyBackupFile == null
                        ? getString(
                        R.string.backup_restore_safety_unknown
                )
                        : safetyBackupFile.getName();

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.backup_restore_success_title
                )
                .setMessage(
                        getString(
                                R.string.backup_restore_success_message,
                                safetyFileName
                        )
                )
                .setCancelable(false)
                .setPositiveButton(
                        R.string.backup_restore_restart_action,
                        (dialog, which) ->
                                restartApplication()
                )
                .show();
    }

    private void restartApplication() {
        Intent restartIntent =
                new Intent(
                        BackupRestoreActivity.this,
                        MainActivity.class
                );

        restartIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                restartIntent
        );

        finish();
    }

    private void showOperationState(
            boolean inProgress,
            int restoreButtonText
    ) {
        operationInProgress =
                inProgress;

        binding.progressBackupRestore.setVisibility(
                inProgress
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.buttonChooseBackupFile.setEnabled(
                !inProgress
        );

        binding.buttonRestoreBackup.setEnabled(
                !inProgress
                        && selectedBackupJson != null
        );

        binding.buttonRestoreBackup.setText(
                restoreButtonText
        );

        binding.contentBackupRestore.setAlpha(
                inProgress
                        ? 0.68f
                        : 1f
        );
    }

    private static class BackupPreview {

        private final int profileCount;
        private final int lessonProgressCount;
        private final int quizAttemptCount;
        private final int doubtCount;
        private final int preferenceEntryCount;
        private final long createdAt;

        BackupPreview(
                int profileCount,
                int lessonProgressCount,
                int quizAttemptCount,
                int doubtCount,
                int preferenceEntryCount,
                long createdAt
        ) {
            this.profileCount =
                    profileCount;

            this.lessonProgressCount =
                    lessonProgressCount;

            this.quizAttemptCount =
                    quizAttemptCount;

            this.doubtCount =
                    doubtCount;

            this.preferenceEntryCount =
                    preferenceEntryCount;

            this.createdAt =
                    createdAt;
        }
    }

    private static class BackupValidationException
            extends IOException {

        BackupValidationException(
                @NonNull String message
        ) {
            super(message);
        }
    }
}