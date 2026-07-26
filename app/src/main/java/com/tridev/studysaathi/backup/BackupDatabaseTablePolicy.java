package com.tridev.studysaathi.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BackupDatabaseTablePolicy {

    public static final int LEGACY_SCHEMA_VERSION =
            5;

    public static final int CURRENT_SCHEMA_VERSION =
            7;

    @NonNull
    private static final List<String> LEGACY_TABLES =
            immutableList(
                    "student_profiles",
                    "lesson_progress",
                    "quiz_attempts",
                    "doubt_history"
            );

    @NonNull
    private static final List<String> CURRENT_INSERT_ORDER =
            immutableList(
                    "student_profiles",
                    "lesson_progress",
                    "quiz_attempts",
                    "doubt_history",
                    "school_curriculum_profiles",
                    "school_subjects",
                    "school_books",
                    "school_book_chapters"
            );

    @NonNull
    private static final List<String> CURRENT_DELETE_ORDER =
            immutableList(
                    "school_book_chapters",
                    "school_books",
                    "school_subjects",
                    "school_curriculum_profiles",
                    "doubt_history",
                    "quiz_attempts",
                    "lesson_progress",
                    "student_profiles"
            );

    @NonNull
    private static final Map<String, String>
            AUTO_INCREMENT_ID_COLUMNS =
            createAutoIncrementIdColumns();

    private BackupDatabaseTablePolicy() {
        throw new AssertionError(
                "BackupDatabaseTablePolicy cannot be instantiated."
        );
    }

    public static boolean isSupportedSchemaVersion(
            int schemaVersion
    ) {
        return schemaVersion == LEGACY_SCHEMA_VERSION
                || schemaVersion == CURRENT_SCHEMA_VERSION;
    }

    public static boolean isCurrentSchemaVersion(
            int schemaVersion
    ) {
        return schemaVersion == CURRENT_SCHEMA_VERSION;
    }

    @NonNull
    public static List<String> getRequiredTables(
            int schemaVersion
    ) {
        validateSupportedSchemaVersion(
                schemaVersion
        );

        if (schemaVersion == LEGACY_SCHEMA_VERSION) {
            return LEGACY_TABLES;
        }

        return CURRENT_INSERT_ORDER;
    }

    @NonNull
    public static List<String> getInsertOrder(
            int schemaVersion
    ) {
        return getRequiredTables(
                schemaVersion
        );
    }

    @NonNull
    public static List<String> getDeleteOrder(
            int schemaVersion
    ) {
        validateSupportedSchemaVersion(
                schemaVersion
        );

        return CURRENT_DELETE_ORDER;
    }

    public static boolean isSupportedTable(
            int schemaVersion,
            @Nullable String tableName
    ) {
        if (tableName == null
                || !isSupportedSchemaVersion(
                schemaVersion
        )) {

            return false;
        }

        return getRequiredTables(
                schemaVersion
        ).contains(
                tableName.trim()
        );
    }

    @Nullable
    public static String getAutoIncrementIdColumn(
            @Nullable String tableName
    ) {
        if (tableName == null) {
            return null;
        }

        return AUTO_INCREMENT_ID_COLUMNS.get(
                tableName.trim()
        );
    }

    private static void validateSupportedSchemaVersion(
            int schemaVersion
    ) {
        if (!isSupportedSchemaVersion(
                schemaVersion
        )) {
            throw new IllegalArgumentException(
                    "Unsupported backup database schema version: "
                            + schemaVersion
            );
        }
    }

    @NonNull
    private static Map<String, String>
    createAutoIncrementIdColumns() {

        Map<String, String> idColumns =
                new LinkedHashMap<>();

        idColumns.put(
                "student_profiles",
                "profile_id"
        );

        idColumns.put(
                "quiz_attempts",
                "attempt_id"
        );

        idColumns.put(
                "doubt_history",
                "history_id"
        );


        idColumns.put(
                "school_subjects",
                "subject_row_id"
        );

        idColumns.put(
                "school_books",
                "book_row_id"
        );

        idColumns.put(
                "school_book_chapters",
                "chapter_row_id"
        );

        return Collections.unmodifiableMap(
                idColumns
        );
    }

    @NonNull
    private static List<String> immutableList(
            @NonNull String... values
    ) {
        return Collections.unmodifiableList(
                Arrays.asList(
                        values
                )
        );
    }
}

