package com.tridev.studysaathi;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.databinding.ActivityBackupExportBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BackupExportActivity
        extends AppCompatActivity {

    private static final String BACKUP_FORMAT =
            "study_saathi_backup";

    private static final int BACKUP_FORMAT_VERSION = 1;
    private static final int DATABASE_SCHEMA_VERSION = 5;

    private static final String BACKUP_STATE_PREFERENCES =
            "study_saathi_backup_state";

    private static final String KEY_LAST_BACKUP_AT =
            "last_backup_at";

    private static final String[] DATABASE_TABLES = {
            "student_profiles",
            "lesson_progress",
            "quiz_attempts",
            "doubt_history"
    };

    private ActivityBackupExportBinding binding;

    private StudySaathiDatabase database;

    private ActivityResultLauncher<String>
            createBackupDocumentLauncher;

    private boolean operationInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityBackupExportBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        database =
                StudySaathiDatabase.getInstance(this);

        registerCreateDocumentLauncher();
        setupClickListeners();
        showLastBackupStatus();
        loadBackupOverview();
    }

    private void registerCreateDocumentLauncher() {
        createBackupDocumentLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .CreateDocument(
                                "application/json"
                        ),
                        selectedUri -> {
                            if (selectedUri == null) {
                                Snackbar.make(
                                        binding.getRoot(),
                                        R.string.backup_export_cancelled,
                                        Snackbar.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            exportBackupToUri(
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

        binding.buttonCreateBackup.setOnClickListener(view -> {
            if (operationInProgress) {
                return;
            }

            createBackupDocumentLauncher.launch(
                    createBackupFileName()
            );
        });
    }

    @NonNull
    private String createBackupFileName() {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                );

        return "StudySaathi_Backup_"
                + LocalDateTime.now()
                .format(formatter)
                + ".json";
    }

    private void loadBackupOverview() {
        showOperationState(
                true,
                false
        );

        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        BackupOverview backupOverview =
                                new BackupOverview(
                                        countDatabaseRows(
                                                "student_profiles"
                                        ),
                                        countDatabaseRows(
                                                "lesson_progress"
                                        ),
                                        countDatabaseRows(
                                                "quiz_attempts"
                                        ),
                                        countDatabaseRows(
                                                "doubt_history"
                                        ),
                                        countSharedPreferenceItems()
                                );

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showBackupOverview(
                                    backupOverview
                            );

                            showOperationState(
                                    false,
                                    false
                            );
                        });

                    } catch (Exception exception) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    false
                            );

                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.backup_overview_load_failed,
                                    Snackbar.LENGTH_LONG
                            ).show();
                        });
                    }
                });
    }

    private int countDatabaseRows(
            @NonNull String tableName
    ) {
        try (Cursor cursor =
                     database.getOpenHelper()
                             .getReadableDatabase()
                             .query(
                                     "SELECT COUNT(*) FROM `"
                                             + tableName
                                             + "`"
                             )) {

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    private int countSharedPreferenceItems() {
        int totalItems = 0;

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

            totalItems +=
                    sharedPreferences.getAll().size();
        }

        return totalItems;
    }

    private void showBackupOverview(
            @NonNull BackupOverview backupOverview
    ) {
        binding.textBackupProfiles.setText(
                String.valueOf(
                        backupOverview.profileCount
                )
        );

        binding.textBackupLessons.setText(
                String.valueOf(
                        backupOverview.lessonProgressCount
                )
        );

        binding.textBackupQuizzes.setText(
                String.valueOf(
                        backupOverview.quizAttemptCount
                )
        );

        binding.textBackupDoubts.setText(
                String.valueOf(
                        backupOverview.doubtCount
                )
        );

        binding.textBackupPreferenceItems.setText(
                String.valueOf(
                        backupOverview.preferenceItemCount
                )
        );
    }

    private void exportBackupToUri(
            @NonNull Uri destinationUri
    ) {
        showOperationState(
                true,
                true
        );

        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        JSONObject backupJson =
                                createCompleteBackup();

                        writeBackupFile(
                                destinationUri,
                                backupJson
                        );

                        long backupTime =
                                System.currentTimeMillis();

                        getSharedPreferences(
                                BACKUP_STATE_PREFERENCES,
                                MODE_PRIVATE
                        )
                                .edit()
                                .putLong(
                                        KEY_LAST_BACKUP_AT,
                                        backupTime
                                )
                                .apply();

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    false
                            );

                            showLastBackupStatus(
                                    backupTime
                            );

                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.backup_export_success,
                                    Snackbar.LENGTH_LONG
                            ).show();

                            loadBackupOverview();
                        });

                    } catch (Exception exception) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            showOperationState(
                                    false,
                                    false
                            );

                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.backup_export_failed,
                                    Snackbar.LENGTH_LONG
                            ).show();
                        });
                    }
                });
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
                        cursor.getBlob(columnIndex),
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
                                preferenceValues.get(key)
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

    private void writeBackupFile(
            @NonNull Uri destinationUri,
            @NonNull JSONObject backupJson
    ) throws IOException, JSONException {

        OutputStream outputStream =
                getContentResolver()
                        .openOutputStream(
                                destinationUri,
                                "w"
                        );

        if (outputStream == null) {
            throw new IOException(
                    "Unable to open destination file."
            );
        }

        try (OutputStream safeOutputStream =
                     outputStream;

             OutputStreamWriter writer =
                     new OutputStreamWriter(
                             safeOutputStream,
                             StandardCharsets.UTF_8
                     )) {

            writer.write(
                    backupJson.toString(2)
            );

            writer.flush();
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

    private void showLastBackupStatus() {
        long lastBackupAt =
                getSharedPreferences(
                        BACKUP_STATE_PREFERENCES,
                        MODE_PRIVATE
                )
                        .getLong(
                                KEY_LAST_BACKUP_AT,
                                0L
                        );

        showLastBackupStatus(
                lastBackupAt
        );
    }

    private void showLastBackupStatus(
            long timestamp
    ) {
        if (timestamp <= 0L) {
            binding.textLastBackup.setText(
                    R.string.backup_last_backup_none
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

        binding.textLastBackup.setText(
                getString(
                        R.string.backup_last_backup_format,
                        formattedTime
                )
        );
    }

    private void showOperationState(
            boolean inProgress,
            boolean exporting
    ) {
        operationInProgress =
                inProgress;

        binding.progressBackupExport.setVisibility(
                inProgress
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.buttonCreateBackup.setEnabled(
                !inProgress
        );

        binding.buttonCreateBackup.setText(
                exporting
                        ? R.string.backup_creating_button
                        : R.string.backup_create_button
        );

        binding.contentBackupExport.setAlpha(
                inProgress
                        ? 0.72f
                        : 1f
        );
    }

    private static class BackupOverview {

        private final int profileCount;
        private final int lessonProgressCount;
        private final int quizAttemptCount;
        private final int doubtCount;
        private final int preferenceItemCount;

        BackupOverview(
                int profileCount,
                int lessonProgressCount,
                int quizAttemptCount,
                int doubtCount,
                int preferenceItemCount
        ) {
            this.profileCount = profileCount;
            this.lessonProgressCount =
                    lessonProgressCount;
            this.quizAttemptCount =
                    quizAttemptCount;
            this.doubtCount = doubtCount;
            this.preferenceItemCount =
                    preferenceItemCount;
        }
    }
}