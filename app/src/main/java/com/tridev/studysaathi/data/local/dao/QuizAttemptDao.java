package com.tridev.studysaathi.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;

import java.util.List;

@Dao
public interface QuizAttemptDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertAttempt(
            QuizAttemptEntity quizAttempt
    );

    @Query(
            "SELECT MAX(percentage) FROM quiz_attempts " +
                    "WHERE profile_id = :profileId " +
                    "AND education_board = :educationBoard " +
                    "AND student_class = :studentClass " +
                    "AND subject_name = :subjectName " +
                    "AND chapter_title = :chapterTitle"
    )
    Integer getBestPercentage(
            long profileId,
            String educationBoard,
            String studentClass,
            String subjectName,
            String chapterTitle
    );

    @Query(
            "SELECT COUNT(*) FROM quiz_attempts " +
                    "WHERE profile_id = :profileId " +
                    "AND education_board = :educationBoard " +
                    "AND student_class = :studentClass " +
                    "AND subject_name = :subjectName " +
                    "AND chapter_title = :chapterTitle"
    )
    int getAttemptCount(
            long profileId,
            String educationBoard,
            String studentClass,
            String subjectName,
            String chapterTitle
    );

    @Query(
            "SELECT * FROM quiz_attempts " +
                    "WHERE profile_id = :profileId " +
                    "AND education_board = :educationBoard " +
                    "AND student_class = :studentClass " +
                    "AND subject_name = :subjectName " +
                    "AND chapter_title = :chapterTitle " +
                    "ORDER BY attempted_at DESC"
    )
    List<QuizAttemptEntity> getChapterAttempts(
            long profileId,
            String educationBoard,
            String studentClass,
            String subjectName,
            String chapterTitle
    );

    @Query(
            "SELECT * FROM quiz_attempts " +
                    "WHERE profile_id = :profileId " +
                    "ORDER BY attempted_at DESC"
    )
    List<QuizAttemptEntity> getProfileAttempts(
            long profileId
    );

    @Query(
            "DELETE FROM quiz_attempts " +
                    "WHERE profile_id = :profileId"
    )
    void deleteAttemptsForProfile(
            long profileId
    );
}