package com.tridev.studysaathi.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;

import java.util.List;

@Dao
public interface StudentProfileDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertProfile(StudentProfileEntity studentProfile);

    @Update
    int updateProfile(StudentProfileEntity studentProfile);

    @Delete
    int deleteProfile(StudentProfileEntity studentProfile);

    @Query(
            "SELECT * FROM student_profiles " +
                    "ORDER BY created_at DESC"
    )
    List<StudentProfileEntity> getAllProfiles();

    @Query(
            "SELECT * FROM student_profiles " +
                    "WHERE profile_id = :profileId " +
                    "LIMIT 1"
    )
    StudentProfileEntity getProfileById(long profileId);

    @Query(
            "SELECT * FROM student_profiles " +
                    "WHERE is_active = 1 " +
                    "LIMIT 1"
    )
    StudentProfileEntity getActiveProfile();

    @Query(
            "SELECT * FROM student_profiles " +
                    "ORDER BY created_at DESC " +
                    "LIMIT 1"
    )
    StudentProfileEntity getLatestProfile();

    @Query("SELECT COUNT(*) FROM student_profiles")
    int getProfileCount();

    @Query("UPDATE student_profiles SET is_active = 0")
    void deactivateAllProfiles();

    @Query(
            "UPDATE student_profiles " +
                    "SET is_active = 1, updated_at = :updatedAt " +
                    "WHERE profile_id = :profileId"
    )
    void activateProfile(long profileId, long updatedAt);

    @Query("DELETE FROM student_profiles WHERE profile_id = :profileId")
    void deleteProfileById(long profileId);
}