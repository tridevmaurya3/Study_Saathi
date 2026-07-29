package com.tridev.studysaathi.cloud;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.backup
        .BackupDatabaseTablePolicy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EncryptedBackupDatabaseValidator {

    private EncryptedBackupDatabaseValidator() {
        throw new AssertionError(
                "EncryptedBackupDatabaseValidator "
                        + "cannot be instantiated."
        );
    }

    @NonNull
    public static Map<String, JSONArray> validate(
            int schemaVersion,
            @NonNull JSONArray tableArray
    ) throws JSONException,
            ValidationException {

        if (!BackupDatabaseTablePolicy
                .isSupportedSchemaVersion(
                        schemaVersion
                )) {

            throw new ValidationException(
                    "Unsupported encrypted backup "
                            + "database schema version "
                            + schemaVersion
                            + "."
            );
        }

        List<String> requiredTables =
                BackupDatabaseTablePolicy
                        .getRequiredTables(
                                schemaVersion
                        );

        Map<String, JSONArray> tableRows =
                new HashMap<>();

        for (int tableIndex = 0;
             tableIndex < tableArray.length();
             tableIndex++) {

            JSONObject tableObject =
                    tableArray.getJSONObject(
                            tableIndex
                    );

            String tableName =
                    tableObject.getString(
                            "table_name"
                    ).trim();

            if (!BackupDatabaseTablePolicy
                    .isSupportedTable(
                            schemaVersion,
                            tableName
                    )) {

                throw new ValidationException(
                        "Encrypted backup contains "
                                + "unsupported table "
                                + tableName
                                + "."
                );
            }

            if (tableRows.containsKey(
                    tableName
            )) {
                throw new ValidationException(
                        "Encrypted backup contains "
                                + "duplicate table "
                                + tableName
                                + "."
                );
            }

            JSONArray rows =
                    tableObject.getJSONArray(
                            "rows"
                    );

            int declaredRowCount =
                    tableObject.getInt(
                            "row_count"
                    );

            if (declaredRowCount
                    != rows.length()) {

                throw new ValidationException(
                        "Table "
                                + tableName
                                + " has an invalid row count."
                );
            }

            validateCriticalColumns(
                    tableName,
                    rows
            );

            tableRows.put(
                    tableName,
                    rows
            );
        }

        for (String requiredTable
                : requiredTables) {

            if (!tableRows.containsKey(
                    requiredTable
            )) {
                throw new ValidationException(
                        "Encrypted backup is missing "
                                + requiredTable
                                + "."
                );
            }
        }

        return tableRows;
    }

    private static void validateCriticalColumns(
            @NonNull String tableName,
            @NonNull JSONArray rows
    ) throws JSONException,
            ValidationException {

        String[] requiredColumns =
                getCriticalColumns(
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

                if (!row.has(
                        requiredColumn
                ) || row.isNull(
                        requiredColumn
                )) {

                    throw new ValidationException(
                            "Table "
                                    + tableName
                                    + " row "
                                    + rowIndex
                                    + " is missing column "
                                    + requiredColumn
                                    + "."
                    );
                }
            }
        }
    }

    @NonNull
    private static String[] getCriticalColumns(
            @NonNull String tableName
    ) throws ValidationException {

        switch (tableName) {
            case "student_profiles":
                return new String[]{
                        "profile_id",
                        "student_name",
                        "education_board",
                        "student_class",
                        "is_active",
                        "created_at",
                        "updated_at"
                };

            case "lesson_progress":
                return new String[]{
                        "progress_key",
                        "profile_id",
                        "subject_name",
                        "chapter_title",
                        "progress_percent",
                        "is_completed"
                };

            case "quiz_attempts":
                return new String[]{
                        "attempt_id",
                        "profile_id",
                        "subject_name",
                        "chapter_title",
                        "correct_answers",
                        "total_questions",
                        "percentage"
                };

            case "doubt_history":
                return new String[]{
                        "history_id",
                        "profile_id",
                        "subject_name",
                        "chapter_title",
                        "question_text",
                        "answer_text"
                };

            case "school_curriculum_profiles":
                return new String[]{
                        "profile_id",
                        "curriculum_id",
                        "education_board",
                        "class_number",
                        "academic_session",
                        "created_at",
                        "updated_at"
                };

            case "school_subjects":
                return new String[]{
                        "subject_row_id",
                        "profile_id",
                        "subject_id",
                        "subject_name_english",
                        "is_enabled",
                        "sort_order",
                        "created_at",
                        "updated_at"
                };

            case "school_books":
                return new String[]{
                        "book_row_id",
                        "subject_row_id",
                        "book_id",
                        "book_title",
                        "parent_confirmed_match",
                        "is_active",
                        "is_primary_book",
                        "created_at",
                        "updated_at"
                };

            case "school_book_chapters":
                return new String[]{
                        "chapter_row_id",
                        "book_row_id",
                        "chapter_id",
                        "chapter_title_english",
                        "parent_confirmed",
                        "is_enabled",
                        "progress_percent",
                        "sort_order",
                        "created_at",
                        "updated_at"
                };

            case "school_book_chapter_contents":
                return new String[]{
                        "content_row_id",
                        "chapter_row_id",
                        "content_id",
                        "language_mode",
                        "content_source",
                        "review_status",
                        "parent_approved",
                        "created_at",
                        "updated_at"
                };

            case "school_book_chapter_pages":
                return new String[]{
                        "chapter_page_row_id",
                        "chapter_row_id",
                        "chapter_page_id",
                        "page_order",
                        "page_type",
                        "parent_approved",
                        "created_at",
                        "updated_at"
                };

            default:
                throw new ValidationException(
                        "Unsupported encrypted backup table "
                                + tableName
                                + "."
                );
        }
    }

    public static final class ValidationException
            extends Exception {

        public ValidationException(
                @NonNull String message
        ) {
            super(
                    message
            );
        }
    }
}
