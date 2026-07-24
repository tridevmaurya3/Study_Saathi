package com.tridev.studysaathi.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;

import java.util.List;

@Dao
public interface LessonProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long saveProgress(
            LessonProgressEntity lessonProgress
    );

    @Query(
            "SELECT * FROM lesson_progress " +
                    "WHERE progress_key = :progressKey " +
                    "LIMIT 1"
    )
    LessonProgressEntity getProgressByKey(
            String progressKey
    );

    @Query(
            "SELECT * FROM lesson_progress " +
                    "WHERE profile_id = :profileId " +
                    "ORDER BY last_studied_at DESC"
    )
    List<LessonProgressEntity> getProgressForProfile(
            long profileId
    );

    @Query(
            "SELECT * FROM lesson_progress " +
                    "WHERE profile_id = :profileId " +
                    "AND education_board = :educationBoard " +
                    "AND student_class = :studentClass " +
                    "AND subject_name = :subjectName " +
                    "ORDER BY last_studied_at DESC"
    )
    List<LessonProgressEntity> getProgressForSubject(
            long profileId,
            String educationBoard,
            String studentClass,
            String subjectName
    );

    @Query(
            "SELECT COUNT(*) FROM lesson_progress " +
                    "WHERE profile_id = :profileId " +
                    "AND is_completed = 1"
    )
    int getCompletedLessonCount(
            long profileId
    );

    @Query(
            "SELECT MAX(last_studied_at) FROM lesson_progress " +
                    "WHERE profile_id = :profileId"
    )
    Long getLastStudyTime(
            long profileId
    );

    @Query(
            "DELETE FROM lesson_progress " +
                    "WHERE profile_id = :profileId"
    )
    void deleteProgressForProfile(
            long profileId
    );
}