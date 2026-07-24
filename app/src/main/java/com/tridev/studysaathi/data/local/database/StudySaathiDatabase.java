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
import com.tridev.studysaathi.data.local.dao.StudentProfileDao;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                StudentProfileEntity.class,
                LessonProgressEntity.class,
                QuizAttemptEntity.class,
                DoubtHistoryEntity.class
        },
        version = 5,
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
                            "CREATE TABLE IF NOT EXISTS " +
                                    "`lesson_progress` (" +
                                    "`progress_key` TEXT NOT NULL, " +
                                    "`profile_id` INTEGER NOT NULL, " +
                                    "`education_board` TEXT NOT NULL, " +
                                    "`student_class` TEXT NOT NULL, " +
                                    "`subject_name` TEXT NOT NULL, " +
                                    "`chapter_title` TEXT NOT NULL, " +
                                    "`progress_percent` INTEGER NOT NULL, " +
                                    "`is_completed` INTEGER NOT NULL, " +
                                    "`last_studied_at` INTEGER NOT NULL, " +
                                    "`completed_at` INTEGER NOT NULL, " +
                                    "PRIMARY KEY(`progress_key`)" +
                                    ")"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "`index_lesson_progress_profile_id` " +
                                    "ON `lesson_progress` (`profile_id`)"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "`index_lesson_progress_profile_id_" +
                                    "subject_name_chapter_title` " +
                                    "ON `lesson_progress` (" +
                                    "`profile_id`, " +
                                    "`subject_name`, " +
                                    "`chapter_title`" +
                                    ")"
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
                            "ALTER TABLE `lesson_progress` " +
                                    "ADD COLUMN `revision_count` " +
                                    "INTEGER NOT NULL DEFAULT 0"
                    );

                    database.execSQL(
                            "ALTER TABLE `lesson_progress` " +
                                    "ADD COLUMN `last_revised_at` " +
                                    "INTEGER NOT NULL DEFAULT 0"
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
                            "CREATE TABLE IF NOT EXISTS " +
                                    "`quiz_attempts` (" +
                                    "`attempt_id` INTEGER PRIMARY KEY " +
                                    "AUTOINCREMENT NOT NULL, " +
                                    "`profile_id` INTEGER NOT NULL, " +
                                    "`education_board` TEXT NOT NULL, " +
                                    "`student_class` TEXT NOT NULL, " +
                                    "`subject_name` TEXT NOT NULL, " +
                                    "`chapter_title` TEXT NOT NULL, " +
                                    "`correct_answers` INTEGER NOT NULL, " +
                                    "`total_questions` INTEGER NOT NULL, " +
                                    "`percentage` INTEGER NOT NULL, " +
                                    "`attempted_at` INTEGER NOT NULL" +
                                    ")"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "`index_quiz_attempts_profile_id` " +
                                    "ON `quiz_attempts` (`profile_id`)"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "`index_quiz_attempts_profile_id_" +
                                    "education_board_student_class_" +
                                    "subject_name_chapter_title` " +
                                    "ON `quiz_attempts` (" +
                                    "`profile_id`, " +
                                    "`education_board`, " +
                                    "`student_class`, " +
                                    "`subject_name`, " +
                                    "`chapter_title`" +
                                    ")"
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
                            "CREATE TABLE IF NOT EXISTS " +
                                    "`doubt_history` (" +
                                    "`history_id` INTEGER PRIMARY KEY " +
                                    "AUTOINCREMENT NOT NULL, " +
                                    "`profile_id` INTEGER NOT NULL, " +
                                    "`education_board` TEXT NOT NULL, " +
                                    "`student_class` TEXT NOT NULL, " +
                                    "`subject_name` TEXT NOT NULL, " +
                                    "`chapter_title` TEXT NOT NULL, " +
                                    "`question_text` TEXT NOT NULL, " +
                                    "`answer_text` TEXT NOT NULL, " +
                                    "`explanation_language` TEXT NOT NULL, " +
                                    "`created_at` INTEGER NOT NULL" +
                                    ")"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "`index_doubt_history_profile_id` " +
                                    "ON `doubt_history` (`profile_id`)"
                    );

                    database.execSQL(
                            "CREATE INDEX IF NOT EXISTS " +
                                    "`index_doubt_history_profile_id_" +
                                    "subject_name_chapter_title` " +
                                    "ON `doubt_history` (" +
                                    "`profile_id`, " +
                                    "`subject_name`, " +
                                    "`chapter_title`" +
                                    ")"
                    );
                }
            };

    public abstract StudentProfileDao studentProfileDao();

    public abstract LessonProgressDao lessonProgressDao();

    public abstract QuizAttemptDao quizAttemptDao();

    public abstract DoubtHistoryDao doubtHistoryDao();

    public static StudySaathiDatabase getInstance(
            Context context
    ) {
        if (INSTANCE == null) {
            synchronized (StudySaathiDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    StudySaathiDatabase.class,
                                    DATABASE_NAME
                            )
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5
                            )
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}