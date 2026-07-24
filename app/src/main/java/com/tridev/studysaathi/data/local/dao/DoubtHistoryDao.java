package com.tridev.studysaathi.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;

import java.util.List;

@Dao
public interface DoubtHistoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertHistory(
            DoubtHistoryEntity doubtHistory
    );

    @Query(
            "SELECT * FROM doubt_history " +
                    "WHERE profile_id = :profileId " +
                    "ORDER BY created_at DESC"
    )
    List<DoubtHistoryEntity> getHistoryForProfile(
            long profileId
    );

    @Query(
            "SELECT * FROM doubt_history " +
                    "WHERE history_id = :historyId " +
                    "LIMIT 1"
    )
    DoubtHistoryEntity getHistoryById(
            long historyId
    );

    @Query(
            "DELETE FROM doubt_history " +
                    "WHERE history_id = :historyId"
    )
    void deleteHistoryById(
            long historyId
    );

    @Query(
            "DELETE FROM doubt_history " +
                    "WHERE profile_id = :profileId"
    )
    void deleteHistoryForProfile(
            long profileId
    );
}