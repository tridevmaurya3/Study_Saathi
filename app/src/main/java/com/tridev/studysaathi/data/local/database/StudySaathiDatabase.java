package com.tridev.studysaathi.data.local.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tridev.studysaathi.data.local.dao.DoubtHistoryDao;
import com.tridev.studysaathi.data.local.dao.LessonProgressDao;
import com.tridev.studysaathi.data.local.dao.QuizAttemptDao;
import com.tridev.studysaathi.data.local.dao.SchoolBookChapterContentDao;
import com.tridev.studysaathi.data.local.dao.SchoolBookChapterDao;
import com.tridev.studysaathi.data.local.dao.SchoolBookChapterPageDao;
import com.tridev.studysaathi.data.local.dao.SchoolBookDao;
import com.tridev.studysaathi.data.local.dao.SchoolCurriculumProfileDao;
import com.tridev.studysaathi.data.local.dao.SchoolSubjectDao;
import com.tridev.studysaathi.data.local.dao.StudentProfileDao;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterContentEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterPageEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookEntity;
import com.tridev.studysaathi.data.local.entity.SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                StudentProfileEntity.class,
                LessonProgressEntity.class,
                QuizAttemptEntity.class,
                DoubtHistoryEntity.class,
                SchoolCurriculumProfileEntity.class,
                SchoolSubjectEntity.class,
                SchoolBookEntity.class,
                SchoolBookChapterEntity.class,
                SchoolBookChapterContentEntity.class,
                SchoolBookChapterPageEntity.class
        },
        version = 11,
        exportSchema = false
)
public abstract class StudySaathiDatabase extends RoomDatabase {

    private static final String DATABASE_NAME =
            "study_saathi_database";

    private static volatile StudySaathiDatabase INSTANCE;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(2);

    public static final Migration MIGRATION_1_2 =
            new Migration(1, 2) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS "
                                    + "`lesson_progress` ("
                                    + "`progress_key` TEXT NOT NULL, "
                                    + "`profile_id` INTEGER NOT NULL, "
                                    + "`education_board` TEXT NOT NULL, "
                                    + "`student_class` TEXT NOT NULL, "
                                    + "`subject_name` TEXT NOT NULL, "
                                    + "`chapter_title` TEXT NOT NULL, "
                                    + "`progress_percent` INTEGER NOT NULL, "
                                    + "`is_completed` INTEGER NOT NULL, "
                                    + "`last_studied_at` INTEGER NOT NULL, "
                                    + "`completed_at` INTEGER NOT NULL, "
                                    + "PRIMARY KEY(`progress_key`)"
                                    + ")"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "`index_lesson_progress_profile_id` "
                                    + "ON `lesson_progress` (`profile_id`)"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "`index_lesson_progress_profile_id_"
                                    + "subject_name_chapter_title` "
                                    + "ON `lesson_progress` ("
                                    + "`profile_id`, "
                                    + "`subject_name`, "
                                    + "`chapter_title`"
                                    + ")"
                    );
                }
            };

    public static final Migration MIGRATION_2_3 =
            new Migration(2, 3) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    database.execSQL(
                            "ALTER TABLE `lesson_progress` "
                                    + "ADD COLUMN `revision_count` "
                                    + "INTEGER NOT NULL DEFAULT 0"
                    );

                    database.execSQL(
                            "ALTER TABLE `lesson_progress` "
                                    + "ADD COLUMN `last_revised_at` "
                                    + "INTEGER NOT NULL DEFAULT 0"
                    );
                }
            };

    public static final Migration MIGRATION_3_4 =
            new Migration(3, 4) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS "
                                    + "`quiz_attempts` ("
                                    + "`attempt_id` INTEGER PRIMARY KEY "
                                    + "AUTOINCREMENT NOT NULL, "
                                    + "`profile_id` INTEGER NOT NULL, "
                                    + "`education_board` TEXT NOT NULL, "
                                    + "`student_class` TEXT NOT NULL, "
                                    + "`subject_name` TEXT NOT NULL, "
                                    + "`chapter_title` TEXT NOT NULL, "
                                    + "`correct_answers` INTEGER NOT NULL, "
                                    + "`total_questions` INTEGER NOT NULL, "
                                    + "`percentage` INTEGER NOT NULL, "
                                    + "`attempted_at` INTEGER NOT NULL"
                                    + ")"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "`index_quiz_attempts_profile_id` "
                                    + "ON `quiz_attempts` (`profile_id`)"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "`index_quiz_attempts_profile_id_"
                                    + "education_board_student_class_"
                                    + "subject_name_chapter_title` "
                                    + "ON `quiz_attempts` ("
                                    + "`profile_id`, "
                                    + "`education_board`, "
                                    + "`student_class`, "
                                    + "`subject_name`, "
                                    + "`chapter_title`"
                                    + ")"
                    );
                }
            };

    public static final Migration MIGRATION_4_5 =
            new Migration(4, 5) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS "
                                    + "`doubt_history` ("
                                    + "`history_id` INTEGER PRIMARY KEY "
                                    + "AUTOINCREMENT NOT NULL, "
                                    + "`profile_id` INTEGER NOT NULL, "
                                    + "`education_board` TEXT NOT NULL, "
                                    + "`student_class` TEXT NOT NULL, "
                                    + "`subject_name` TEXT NOT NULL, "
                                    + "`chapter_title` TEXT NOT NULL, "
                                    + "`question_text` TEXT NOT NULL, "
                                    + "`answer_text` TEXT NOT NULL, "
                                    + "`explanation_language` TEXT NOT NULL, "
                                    + "`created_at` INTEGER NOT NULL"
                                    + ")"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "`index_doubt_history_profile_id` "
                                    + "ON `doubt_history` (`profile_id`)"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "`index_doubt_history_profile_id_"
                                    + "subject_name_chapter_title` "
                                    + "ON `doubt_history` ("
                                    + "`profile_id`, "
                                    + "`subject_name`, "
                                    + "`chapter_title`"
                                    + ")"
                    );
                }
            };

    public static final Migration MIGRATION_5_6 =
            new Migration(5, 6) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    createSchoolCurriculumProfilesTable(database);
                    createSchoolSubjectsTable(database);
                    createSchoolBooksTable(database);
                }
            };

    public static final Migration MIGRATION_6_7 =
            new Migration(6, 7) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    createSchoolBookChaptersTable(database);
                }
            };

    public static final Migration MIGRATION_7_8 =
            new Migration(7, 8) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    createSchoolBookChapterContentsTable(database);
                }
            };

    public static final Migration MIGRATION_8_9 =
            new Migration(8, 9) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    createSchoolBookChapterPagesTable(database);
                }
            };

    /**
     * Additive metadata used by Family Workspace realtime synchronization.
     * App records and their existing primary/foreign keys are not changed.
     */
    public static final Migration MIGRATION_9_10 =
            new Migration(9, 10) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {
                    createFamilySyncMetadata(database);
                }
            };

    /**
     * Preserve the nationwide State/UT selection in the curriculum record so
     * the existing family record sync carries it to every linked parent.
     */
    public static final Migration MIGRATION_10_11 =
            new Migration(10, 11) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {
                    database.execSQL(
                            "ALTER TABLE `school_curriculum_profiles` "
                                    + "ADD COLUMN `school_state_code` TEXT NOT NULL DEFAULT ''"
                    );
                    database.execSQL(
                            "ALTER TABLE `school_curriculum_profiles` "
                                    + "ADD COLUMN `school_state_name` TEXT NOT NULL DEFAULT ''"
                    );
                }
            };

    private static final Callback FAMILY_SYNC_CALLBACK = new Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase database) {
            super.onOpen(database);
            createFamilySyncMetadata(database);
        }
    };

    private static void createFamilySyncMetadata(@NonNull SupportSQLiteDatabase database) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `family_sync_record_map` ("
                            + "`table_name` TEXT NOT NULL, `local_id` TEXT NOT NULL, "
                            + "`sync_id` TEXT NOT NULL, PRIMARY KEY(`table_name`,`local_id`))");
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                            + "`index_family_sync_record_map_sync_id` ON "
                            + "`family_sync_record_map` (`sync_id`)");
                    database.execSQL("CREATE TABLE IF NOT EXISTS `family_sync_outbox` ("
                            + "`table_name` TEXT NOT NULL, `local_id` TEXT NOT NULL, "
                            + "`sync_id` TEXT NOT NULL, `operation` TEXT NOT NULL, "
                            + "`changed_at` INTEGER NOT NULL, PRIMARY KEY(`table_name`,`sync_id`))");
                    database.execSQL("CREATE TABLE IF NOT EXISTS `family_sync_runtime` ("
                            + "`singleton_id` INTEGER NOT NULL PRIMARY KEY CHECK(`singleton_id`=1), "
                            + "`suppress_triggers` INTEGER NOT NULL DEFAULT 0)");
                    database.execSQL("INSERT OR IGNORE INTO `family_sync_runtime` "
                            + "(`singleton_id`,`suppress_triggers`) VALUES (1,0)");

                    addFamilySyncTriggers(database, "student_profiles", "profile_id");
                    addFamilySyncTriggers(database, "school_curriculum_profiles", "profile_id");
                    addFamilySyncTriggers(database, "school_subjects", "subject_row_id");
                    addFamilySyncTriggers(database, "school_books", "book_row_id");
                    addFamilySyncTriggers(database, "school_book_chapters", "chapter_row_id");
                    addFamilySyncTriggers(database, "school_book_chapter_contents", "content_row_id");
                    addFamilySyncTriggers(database, "school_book_chapter_pages", "chapter_page_row_id");
                    addFamilySyncTriggers(database, "lesson_progress", "progress_key");
                    addFamilySyncTriggers(database, "quiz_attempts", "attempt_id");
                    addFamilySyncTriggers(database, "doubt_history", "history_id");
    }

    private static void addFamilySyncTriggers(@NonNull SupportSQLiteDatabase database,
                                               @NonNull String table,
                                               @NonNull String primaryKey) {
        String prefix = "family_sync_" + table;
        String newId = "CAST(NEW.`" + primaryKey + "` AS TEXT)";
        String oldId = "CAST(OLD.`" + primaryKey + "` AS TEXT)";
        String enabled = "(SELECT `suppress_triggers` FROM `family_sync_runtime` "
                + "WHERE `singleton_id`=1)=0";
        database.execSQL("CREATE TRIGGER IF NOT EXISTS `" + prefix + "_insert` "
                + "AFTER INSERT ON `" + table + "` WHEN " + enabled + " BEGIN "
                + "INSERT OR IGNORE INTO `family_sync_record_map` "
                + "(`table_name`,`local_id`,`sync_id`) VALUES ('" + table + "'," + newId
                + ",lower(hex(randomblob(16)))); "
                + "INSERT OR REPLACE INTO `family_sync_outbox` "
                + "(`table_name`,`local_id`,`sync_id`,`operation`,`changed_at`) "
                + "SELECT '" + table + "'," + newId + ",`sync_id`,'UPSERT',"
                + "CAST(strftime('%s','now') AS INTEGER)*1000 FROM `family_sync_record_map` "
                + "WHERE `table_name`='" + table + "' AND `local_id`=" + newId + "; END");
        database.execSQL("CREATE TRIGGER IF NOT EXISTS `" + prefix + "_update` "
                + "AFTER UPDATE ON `" + table + "` WHEN " + enabled + " BEGIN "
                + "INSERT OR IGNORE INTO `family_sync_record_map` "
                + "(`table_name`,`local_id`,`sync_id`) VALUES ('" + table + "'," + newId
                + ",lower(hex(randomblob(16)))); "
                + "INSERT OR REPLACE INTO `family_sync_outbox` "
                + "(`table_name`,`local_id`,`sync_id`,`operation`,`changed_at`) "
                + "SELECT '" + table + "'," + newId + ",`sync_id`,'UPSERT',"
                + "CAST(strftime('%s','now') AS INTEGER)*1000 FROM `family_sync_record_map` "
                + "WHERE `table_name`='" + table + "' AND `local_id`=" + newId + "; END");
        database.execSQL("CREATE TRIGGER IF NOT EXISTS `" + prefix + "_delete` "
                + "BEFORE DELETE ON `" + table + "` WHEN " + enabled + " BEGIN "
                + "INSERT OR REPLACE INTO `family_sync_outbox` "
                + "(`table_name`,`local_id`,`sync_id`,`operation`,`changed_at`) "
                + "SELECT '" + table + "'," + oldId + ",`sync_id`,'DELETE',"
                + "CAST(strftime('%s','now') AS INTEGER)*1000 FROM `family_sync_record_map` "
                + "WHERE `table_name`='" + table + "' AND `local_id`=" + oldId + "; "
                + "DELETE FROM `family_sync_record_map` WHERE `table_name`='" + table
                + "' AND `local_id`=" + oldId + "; END");
    }

    private static void createSchoolCurriculumProfilesTable(
            @NonNull SupportSQLiteDatabase database
    ) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + "`school_curriculum_profiles` ("
                        + "`profile_id` INTEGER NOT NULL, "
                        + "`curriculum_id` TEXT NOT NULL, "
                        + "`school_state_code` TEXT NOT NULL DEFAULT '', "
                        + "`school_state_name` TEXT NOT NULL DEFAULT '', "
                        + "`school_name` TEXT NOT NULL, "
                        + "`school_code` TEXT NOT NULL, "
                        + "`education_board` TEXT NOT NULL, "
                        + "`school_pattern` TEXT NOT NULL, "
                        + "`class_number` INTEGER NOT NULL DEFAULT 6, "
                        + "`section` TEXT NOT NULL, "
                        + "`academic_session` TEXT NOT NULL, "
                        + "`study_medium` TEXT NOT NULL, "
                        + "`ai_tutor_enabled` INTEGER NOT NULL DEFAULT 1, "
                        + "`ai_default_language` TEXT NOT NULL "
                        + "DEFAULT 'BILINGUAL', "
                        + "`ai_default_answer_mode` TEXT NOT NULL "
                        + "DEFAULT 'SIMPLE', "
                        + "`voice_question_enabled` INTEGER NOT NULL "
                        + "DEFAULT 1, "
                        + "`image_question_enabled` INTEGER NOT NULL "
                        + "DEFAULT 1, "
                        + "`read_answer_aloud_enabled` INTEGER NOT NULL "
                        + "DEFAULT 1, "
                        + "`child_safe_answers_enabled` INTEGER NOT NULL "
                        + "DEFAULT 1, "
                        + "`save_doubt_history_enabled` INTEGER NOT NULL "
                        + "DEFAULT 1, "
                        + "`preferred_maximum_answer_words` INTEGER "
                        + "NOT NULL DEFAULT 300, "
                        + "`is_configured` INTEGER NOT NULL DEFAULT 0, "
                        + "`created_at` INTEGER NOT NULL, "
                        + "`updated_at` INTEGER NOT NULL, "
                        + "PRIMARY KEY(`profile_id`), "
                        + "FOREIGN KEY(`profile_id`) "
                        + "REFERENCES `student_profiles`(`profile_id`) "
                        + "ON UPDATE CASCADE ON DELETE CASCADE"
                        + ")"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS "
                        + "`index_school_curriculum_profiles_curriculum_id` "
                        + "ON `school_curriculum_profiles` "
                        + "(`curriculum_id`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_curriculum_profiles_"
                        + "school_name_education_board_class_number` "
                        + "ON `school_curriculum_profiles` ("
                        + "`school_name`, "
                        + "`education_board`, "
                        + "`class_number`"
                        + ")"
        );
    }

    private static void createSchoolSubjectsTable(
            @NonNull SupportSQLiteDatabase database
    ) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + "`school_subjects` ("
                        + "`subject_row_id` INTEGER PRIMARY KEY "
                        + "AUTOINCREMENT NOT NULL, "
                        + "`profile_id` INTEGER NOT NULL, "
                        + "`subject_id` TEXT NOT NULL, "
                        + "`subject_name_english` TEXT NOT NULL, "
                        + "`subject_name_hindi` TEXT NOT NULL, "
                        + "`subject_code` TEXT NOT NULL, "
                        + "`book_name` TEXT NOT NULL, "
                        + "`book_code` TEXT NOT NULL, "
                        + "`publisher_name` TEXT NOT NULL, "
                        + "`subject_category` TEXT NOT NULL "
                        + "DEFAULT 'SCHOOL_SPECIFIC', "
                        + "`content_source` TEXT NOT NULL "
                        + "DEFAULT 'SCHOOL_BOOK', "
                        + "`content_pack_id` TEXT NOT NULL, "
                        + "`is_enabled` INTEGER NOT NULL DEFAULT 1, "
                        + "`ai_tutor_enabled` INTEGER NOT NULL DEFAULT 1, "
                        + "`is_official_core_subject` INTEGER NOT NULL "
                        + "DEFAULT 0, "
                        + "`allow_parent_content_editing` INTEGER NOT NULL "
                        + "DEFAULT 1, "
                        + "`sort_order` INTEGER NOT NULL DEFAULT 0, "
                        + "`chapter_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`lesson_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`quiz_question_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`created_at` INTEGER NOT NULL, "
                        + "`updated_at` INTEGER NOT NULL, "
                        + "FOREIGN KEY(`profile_id`) "
                        + "REFERENCES `school_curriculum_profiles`"
                        + "(`profile_id`) "
                        + "ON UPDATE CASCADE ON DELETE CASCADE"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_subjects_profile_id` "
                        + "ON `school_subjects` (`profile_id`)"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS "
                        + "`index_school_subjects_profile_id_subject_id` "
                        + "ON `school_subjects` ("
                        + "`profile_id`, "
                        + "`subject_id`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_subjects_profile_id_"
                        + "is_enabled_sort_order` "
                        + "ON `school_subjects` ("
                        + "`profile_id`, "
                        + "`is_enabled`, "
                        + "`sort_order`"
                        + ")"
        );
    }

    private static void createSchoolBooksTable(
            @NonNull SupportSQLiteDatabase database
    ) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + "`school_books` ("
                        + "`book_row_id` INTEGER PRIMARY KEY "
                        + "AUTOINCREMENT NOT NULL, "
                        + "`subject_row_id` INTEGER NOT NULL, "
                        + "`book_id` TEXT NOT NULL, "
                        + "`book_title` TEXT NOT NULL, "
                        + "`book_subtitle` TEXT NOT NULL, "
                        + "`author_name` TEXT NOT NULL, "
                        + "`publisher_name` TEXT NOT NULL, "
                        + "`edition_name` TEXT NOT NULL, "
                        + "`publication_year` TEXT NOT NULL, "
                        + "`academic_session` TEXT NOT NULL, "
                        + "`class_name` TEXT NOT NULL, "
                        + "`education_board` TEXT NOT NULL, "
                        + "`study_medium` TEXT NOT NULL, "
                        + "`isbn_10` TEXT NOT NULL, "
                        + "`isbn_13` TEXT NOT NULL, "
                        + "`book_code` TEXT NOT NULL, "
                        + "`barcode_value` TEXT NOT NULL, "
                        + "`barcode_format` TEXT NOT NULL, "
                        + "`cover_image_url` TEXT NOT NULL, "
                        + "`local_cover_image_path` TEXT NOT NULL, "
                        + "`scanned_cover_image_path` TEXT NOT NULL, "
                        + "`detected_cover_text` TEXT NOT NULL, "
                        + "`cover_match_confidence` REAL NOT NULL DEFAULT 0, "
                        + "`book_source` TEXT NOT NULL "
                        + "DEFAULT 'SCHOOL_BOOK', "
                        + "`online_provider` TEXT NOT NULL, "
                        + "`online_volume_id` TEXT NOT NULL, "
                        + "`online_information_url` TEXT NOT NULL, "
                        + "`official_source_url` TEXT NOT NULL, "
                        + "`authorized_download_url` TEXT NOT NULL, "
                        + "`access_type` TEXT NOT NULL "
                        + "DEFAULT 'METADATA_ONLY', "
                        + "`license_type` TEXT NOT NULL DEFAULT 'UNKNOWN', "
                        + "`download_allowed` INTEGER NOT NULL DEFAULT 0, "
                        + "`preview_allowed` INTEGER NOT NULL DEFAULT 0, "
                        + "`parent_confirmed_match` INTEGER NOT NULL "
                        + "DEFAULT 0, "
                        + "`official_source_verified` INTEGER NOT NULL "
                        + "DEFAULT 0, "
                        + "`download_status` TEXT NOT NULL "
                        + "DEFAULT 'NOT_DOWNLOADED', "
                        + "`download_progress` INTEGER NOT NULL DEFAULT 0, "
                        + "`local_book_file_path` TEXT NOT NULL, "
                        + "`local_content_folder_path` TEXT NOT NULL, "
                        + "`downloaded_file_name` TEXT NOT NULL, "
                        + "`downloaded_file_mime_type` TEXT NOT NULL, "
                        + "`downloaded_file_size_bytes` INTEGER NOT NULL "
                        + "DEFAULT 0, "
                        + "`downloaded_file_checksum_sha256` TEXT NOT NULL, "
                        + "`content_processing_status` TEXT NOT NULL "
                        + "DEFAULT 'NOT_STARTED', "
                        + "`chapter_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`processed_chapter_count` INTEGER NOT NULL "
                        + "DEFAULT 0, "
                        + "`generated_lesson_count` INTEGER NOT NULL "
                        + "DEFAULT 0, "
                        + "`generated_quiz_question_count` INTEGER NOT NULL "
                        + "DEFAULT 0, "
                        + "`offline_available` INTEGER NOT NULL DEFAULT 0, "
                        + "`ai_tutor_enabled` INTEGER NOT NULL DEFAULT 1, "
                        + "`is_active` INTEGER NOT NULL DEFAULT 1, "
                        + "`is_primary_book` INTEGER NOT NULL DEFAULT 0, "
                        + "`sort_order` INTEGER NOT NULL DEFAULT 0, "
                        + "`last_online_search_at` INTEGER NOT NULL, "
                        + "`last_download_attempt_at` INTEGER NOT NULL, "
                        + "`download_completed_at` INTEGER NOT NULL, "
                        + "`last_content_processed_at` INTEGER NOT NULL, "
                        + "`created_at` INTEGER NOT NULL, "
                        + "`updated_at` INTEGER NOT NULL, "
                        + "FOREIGN KEY(`subject_row_id`) "
                        + "REFERENCES `school_subjects`(`subject_row_id`) "
                        + "ON UPDATE CASCADE ON DELETE CASCADE"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_books_subject_row_id` "
                        + "ON `school_books` (`subject_row_id`)"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS "
                        + "`index_school_books_subject_row_id_book_id` "
                        + "ON `school_books` ("
                        + "`subject_row_id`, "
                        + "`book_id`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_books_isbn_10` "
                        + "ON `school_books` (`isbn_10`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_books_isbn_13` "
                        + "ON `school_books` (`isbn_13`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_books_subject_row_id_"
                        + "is_active_sort_order` "
                        + "ON `school_books` ("
                        + "`subject_row_id`, "
                        + "`is_active`, "
                        + "`sort_order`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_books_download_status_"
                        + "content_processing_status` "
                        + "ON `school_books` ("
                        + "`download_status`, "
                        + "`content_processing_status`"
                        + ")"
        );
    }

    private static void createSchoolBookChaptersTable(
            @NonNull SupportSQLiteDatabase database
    ) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + "`school_book_chapters` ("
                        + "`chapter_row_id` INTEGER PRIMARY KEY "
                        + "AUTOINCREMENT NOT NULL, "
                        + "`book_row_id` INTEGER NOT NULL, "
                        + "`chapter_id` TEXT NOT NULL, "
                        + "`chapter_number` TEXT NOT NULL, "
                        + "`chapter_title_english` TEXT NOT NULL, "
                        + "`chapter_title_hindi` TEXT NOT NULL, "
                        + "`chapter_subtitle` TEXT NOT NULL, "
                        + "`unit_name` TEXT NOT NULL, "
                        + "`chapter_type` TEXT NOT NULL "
                        + "DEFAULT 'CHAPTER', "
                        + "`start_page_number` INTEGER NOT NULL DEFAULT 0, "
                        + "`end_page_number` INTEGER NOT NULL DEFAULT 0, "
                        + "`chapter_description` TEXT NOT NULL, "
                        + "`learning_objectives` TEXT NOT NULL, "
                        + "`important_topics` TEXT NOT NULL, "
                        + "`content_source` TEXT NOT NULL "
                        + "DEFAULT 'PARENT_MANUAL', "
                        + "`source_reference` TEXT NOT NULL, "
                        + "`extraction_confidence` REAL NOT NULL DEFAULT 0, "
                        + "`parent_confirmed` INTEGER NOT NULL DEFAULT 0, "
                        + "`is_enabled` INTEGER NOT NULL DEFAULT 1, "
                        + "`is_optional_chapter` INTEGER NOT NULL DEFAULT 0, "
                        + "`is_revision_chapter` INTEGER NOT NULL DEFAULT 0, "
                        + "`content_processing_status` TEXT NOT NULL "
                        + "DEFAULT 'NOT_STARTED', "
                        + "`lesson_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`completed_lesson_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`quiz_question_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`note_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`bookmark_count` INTEGER NOT NULL DEFAULT 0, "
                        + "`progress_percent` INTEGER NOT NULL DEFAULT 0, "
                        + "`sort_order` INTEGER NOT NULL DEFAULT 0, "
                        + "`last_opened_at` INTEGER NOT NULL, "
                        + "`last_content_processed_at` INTEGER NOT NULL, "
                        + "`created_at` INTEGER NOT NULL, "
                        + "`updated_at` INTEGER NOT NULL, "
                        + "FOREIGN KEY(`book_row_id`) "
                        + "REFERENCES `school_books`(`book_row_id`) "
                        + "ON UPDATE CASCADE ON DELETE CASCADE"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapters_book_row_id` "
                        + "ON `school_book_chapters` (`book_row_id`)"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapters_"
                        + "book_row_id_chapter_id` "
                        + "ON `school_book_chapters` ("
                        + "`book_row_id`, "
                        + "`chapter_id`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapters_"
                        + "book_row_id_is_enabled_"
                        + "parent_confirmed_sort_order` "
                        + "ON `school_book_chapters` ("
                        + "`book_row_id`, "
                        + "`is_enabled`, "
                        + "`parent_confirmed`, "
                        + "`sort_order`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapters_"
                        + "content_source_content_processing_status` "
                        + "ON `school_book_chapters` ("
                        + "`content_source`, "
                        + "`content_processing_status`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapters_chapter_type` "
                        + "ON `school_book_chapters` (`chapter_type`)"
        );
    }

    private static void createSchoolBookChapterContentsTable(
            @NonNull SupportSQLiteDatabase database
    ) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + "`school_book_chapter_contents` ("
                        + "`content_row_id` INTEGER PRIMARY KEY "
                        + "AUTOINCREMENT NOT NULL, "
                        + "`chapter_row_id` INTEGER NOT NULL, "
                        + "`content_id` TEXT NOT NULL, "
                        + "`language_mode` TEXT NOT NULL, "
                        + "`chapter_introduction_english` TEXT NOT NULL, "
                        + "`chapter_introduction_hindi` TEXT NOT NULL, "
                        + "`detailed_explanation_english` TEXT NOT NULL, "
                        + "`detailed_explanation_hindi` TEXT NOT NULL, "
                        + "`key_points_english` TEXT NOT NULL, "
                        + "`key_points_hindi` TEXT NOT NULL, "
                        + "`important_terms_english` TEXT NOT NULL, "
                        + "`important_terms_hindi` TEXT NOT NULL, "
                        + "`worked_examples_english` TEXT NOT NULL, "
                        + "`worked_examples_hindi` TEXT NOT NULL, "
                        + "`real_life_examples_english` TEXT NOT NULL, "
                        + "`real_life_examples_hindi` TEXT NOT NULL, "
                        + "`common_mistakes_english` TEXT NOT NULL, "
                        + "`common_mistakes_hindi` TEXT NOT NULL, "
                        + "`chapter_summary_english` TEXT NOT NULL, "
                        + "`chapter_summary_hindi` TEXT NOT NULL, "
                        + "`practice_questions_json` TEXT NOT NULL, "
                        + "`source_page_references_json` TEXT NOT NULL, "
                        + "`content_source` TEXT NOT NULL, "
                        + "`review_status` TEXT NOT NULL, "
                        + "`parent_approved` INTEGER NOT NULL DEFAULT 0, "
                        + "`generation_version` INTEGER NOT NULL DEFAULT 1, "
                        + "`estimated_reading_minutes` INTEGER NOT NULL DEFAULT 0, "
                        + "`created_at` INTEGER NOT NULL DEFAULT 0, "
                        + "`updated_at` INTEGER NOT NULL DEFAULT 0, "
                        + "`approved_at` INTEGER NOT NULL DEFAULT 0, "
                        + "`last_generated_at` INTEGER NOT NULL DEFAULT 0, "
                        + "FOREIGN KEY(`chapter_row_id`) "
                        + "REFERENCES `school_book_chapters`(`chapter_row_id`) "
                        + "ON UPDATE CASCADE ON DELETE CASCADE"
                        + ")"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapter_contents_chapter_row_id` "
                        + "ON `school_book_chapter_contents` "
                        + "(`chapter_row_id`)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapter_contents_"
                        + "review_status_parent_approved` "
                        + "ON `school_book_chapter_contents` ("
                        + "`review_status`, `parent_approved`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapter_contents_content_source` "
                        + "ON `school_book_chapter_contents` "
                        + "(`content_source`)"
        );
    }

    private static void createSchoolBookChapterPagesTable(
            @NonNull SupportSQLiteDatabase database
    ) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + "`school_book_chapter_pages` ("
                        + "`chapter_page_row_id` INTEGER PRIMARY KEY "
                        + "AUTOINCREMENT NOT NULL, "
                        + "`chapter_row_id` INTEGER NOT NULL, "
                        + "`chapter_page_id` TEXT NOT NULL, "
                        + "`page_order` INTEGER NOT NULL, "
                        + "`source_document_page_number` INTEGER NOT NULL, "
                        + "`page_type` TEXT NOT NULL, "
                        + "`page_title` TEXT NOT NULL, "
                        + "`introduction_english` TEXT NOT NULL, "
                        + "`introduction_hindi` TEXT NOT NULL, "
                        + "`explanation_english` TEXT NOT NULL, "
                        + "`explanation_hindi` TEXT NOT NULL, "
                        + "`key_points_english` TEXT NOT NULL, "
                        + "`key_points_hindi` TEXT NOT NULL, "
                        + "`examples_english` TEXT NOT NULL, "
                        + "`examples_hindi` TEXT NOT NULL, "
                        + "`exercises_json` TEXT NOT NULL, "
                        + "`summary_english` TEXT NOT NULL, "
                        + "`summary_hindi` TEXT NOT NULL, "
                        + "`persistent_page_image_path` TEXT NOT NULL, "
                        + "`raw_ocr_text` TEXT NOT NULL, "
                        + "`parent_approved` INTEGER NOT NULL DEFAULT 0, "
                        + "`created_at` INTEGER NOT NULL DEFAULT 0, "
                        + "`updated_at` INTEGER NOT NULL DEFAULT 0, "
                        + "FOREIGN KEY(`chapter_row_id`) "
                        + "REFERENCES `school_book_chapters`"
                        + "(`chapter_row_id`) "
                        + "ON UPDATE CASCADE ON DELETE CASCADE"
                        + ")"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapter_pages_"
                        + "chapter_row_id_page_order` "
                        + "ON `school_book_chapter_pages` ("
                        + "`chapter_row_id`, `page_order`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapter_pages_"
                        + "chapter_row_id_parent_approved` "
                        + "ON `school_book_chapter_pages` ("
                        + "`chapter_row_id`, `parent_approved`"
                        + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS "
                        + "`index_school_book_chapter_pages_"
                        + "source_document_page_number` "
                        + "ON `school_book_chapter_pages` "
                        + "(`source_document_page_number`)"
        );
    }

    public abstract StudentProfileDao studentProfileDao();

    public abstract LessonProgressDao lessonProgressDao();

    public abstract QuizAttemptDao quizAttemptDao();

    public abstract DoubtHistoryDao doubtHistoryDao();

    public abstract SchoolCurriculumProfileDao
    schoolCurriculumProfileDao();

    public abstract SchoolSubjectDao schoolSubjectDao();

    public abstract SchoolBookDao schoolBookDao();

    public abstract SchoolBookChapterDao schoolBookChapterDao();

    public abstract SchoolBookChapterContentDao
    schoolBookChapterContentDao();

    public abstract SchoolBookChapterPageDao
    schoolBookChapterPageDao();

    public static StudySaathiDatabase getInstance(
            Context context
    ) {
        if (INSTANCE == null) {
            synchronized (StudySaathiDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE =
                            Room.databaseBuilder(
                                            context.getApplicationContext(),
                                            StudySaathiDatabase.class,
                                            DATABASE_NAME
                                    )
                                    .addMigrations(
                                            MIGRATION_1_2,
                                            MIGRATION_2_3,
                                            MIGRATION_3_4,
                                            MIGRATION_4_5,
                                            MIGRATION_5_6,
                                            MIGRATION_6_7,
                                            MIGRATION_7_8,
                                            MIGRATION_8_9,
                                            MIGRATION_9_10,
                                            MIGRATION_10_11
                                    )
                                    .addCallback(FAMILY_SYNC_CALLBACK)
                                    .build();
                }
            }
        }

        return INSTANCE;
    }
}
