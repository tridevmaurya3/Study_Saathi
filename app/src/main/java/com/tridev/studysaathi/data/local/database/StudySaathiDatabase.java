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
import com.tridev.studysaathi.data.local.dao.SchoolBookDao;
import com.tridev.studysaathi.data.local.dao.SchoolCurriculumProfileDao;
import com.tridev.studysaathi.data.local.dao.SchoolSubjectDao;
import com.tridev.studysaathi.data.local.dao.StudentProfileDao;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
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
                SchoolBookEntity.class
        },
        version = 6,
        exportSchema = false
)
public abstract class StudySaathiDatabase
        extends RoomDatabase {

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

    /**
     * Version 5 से 6:
     *
     * 1. Student school curriculum profile table
     * 2. Dynamic school subjects table
     * 3. School books and scanned books table
     */
    public static final Migration MIGRATION_5_6 =
            new Migration(5, 6) {
                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {
                    createSchoolCurriculumProfilesTable(
                            database
                    );

                    createSchoolSubjectsTable(
                            database
                    );

                    createSchoolBooksTable(
                            database
                    );
                }
            };

    private static void createSchoolCurriculumProfilesTable(
            @NonNull SupportSQLiteDatabase database
    ) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + "`school_curriculum_profiles` ("
                        + "`profile_id` INTEGER NOT NULL, "
                        + "`curriculum_id` TEXT NOT NULL, "
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

    public abstract StudentProfileDao studentProfileDao();

    public abstract LessonProgressDao lessonProgressDao();

    public abstract QuizAttemptDao quizAttemptDao();

    public abstract DoubtHistoryDao doubtHistoryDao();

    public abstract SchoolCurriculumProfileDao
    schoolCurriculumProfileDao();

    public abstract SchoolSubjectDao schoolSubjectDao();

    public abstract SchoolBookDao schoolBookDao();

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
                                            MIGRATION_5_6
                                    )
                                    .build();
                }
            }
        }

        return INSTANCE;
    }
}