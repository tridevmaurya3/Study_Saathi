package com.tridev.studysaathi.data.schooldirectory.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.tridev.studysaathi.data.schooldirectory.entity.DistrictDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.SchoolDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.StateDirectoryEntity;

import java.util.List;

@Dao
public abstract class SchoolDirectoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertStates(
            @NonNull List<StateDirectoryEntity> states
    );

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertDistricts(
            @NonNull List<DistrictDirectoryEntity> districts
    );

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertSchools(
            @NonNull List<SchoolDirectoryEntity> schools
    );

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertSchool(
            @NonNull SchoolDirectoryEntity school
    );

    @Query(
            "SELECT * FROM directory_states "
                    + "WHERE is_active = 1 "
                    + "ORDER BY sort_order ASC, state_name ASC"
    )
    @NonNull
    public abstract List<StateDirectoryEntity>
    getActiveStates();

    @Query(
            "SELECT * FROM directory_districts "
                    + "WHERE state_code = :stateCode "
                    + "AND is_active = 1 "
                    + "ORDER BY sort_order ASC, district_name ASC"
    )
    @NonNull
    public abstract List<DistrictDirectoryEntity>
    getActiveDistrictsForState(
            @NonNull String stateCode
    );

    @Query(
            "SELECT DISTINCT education_board "
                    + "FROM directory_schools "
                    + "WHERE district_code = :districtCode "
                    + "AND is_active = 1 "
                    + "AND education_board != '' "
                    + "ORDER BY education_board ASC"
    )
    @NonNull
    public abstract List<String>
    getEducationBoardsForDistrict(
            @NonNull String districtCode
    );

    @Query(
            "SELECT * FROM directory_schools "
                    + "WHERE district_code = :districtCode "
                    + "AND education_board = :educationBoard "
                    + "AND is_active = 1 "
                    + "ORDER BY school_name ASC "
                    + "LIMIT :resultLimit"
    )
    @NonNull
    public abstract List<SchoolDirectoryEntity>
    getSchools(
            @NonNull String districtCode,
            @NonNull String educationBoard,
            int resultLimit
    );

    @Query(
            "SELECT * FROM directory_schools "
                    + "WHERE district_code = :districtCode "
                    + "AND education_board = :educationBoard "
                    + "AND is_active = 1 "
                    + "AND ("
                    + "school_name LIKE '%' || :searchText || '%' "
                    + "OR school_name_hindi LIKE '%' || :searchText || '%' "
                    + "OR udise_code LIKE '%' || :searchText || '%' "
                    + "OR board_affiliation_number "
                    + "LIKE '%' || :searchText || '%' "
                    + "OR school_internal_code "
                    + "LIKE '%' || :searchText || '%'"
                    + ") "
                    + "ORDER BY "
                    + "CASE WHEN school_name = :searchText "
                    + "THEN 0 ELSE 1 END, "
                    + "school_name ASC "
                    + "LIMIT :resultLimit"
    )
    @NonNull
    public abstract List<SchoolDirectoryEntity>
    searchSchools(
            @NonNull String districtCode,
            @NonNull String educationBoard,
            @NonNull String searchText,
            int resultLimit
    );

    @Query(
            "SELECT * FROM directory_schools "
                    + "WHERE school_directory_id = :schoolDirectoryId "
                    + "LIMIT 1"
    )
    @Nullable
    public abstract SchoolDirectoryEntity
    getSchoolByDirectoryId(
            @NonNull String schoolDirectoryId
    );

    @Query(
            "SELECT * FROM directory_schools "
                    + "WHERE udise_code = :udiseCode "
                    + "LIMIT 1"
    )
    @Nullable
    public abstract SchoolDirectoryEntity
    getSchoolByUdiseCode(
            @NonNull String udiseCode
    );

    @Query(
            "SELECT COUNT(*) FROM directory_states"
    )
    public abstract int getStateCount();

    @Query(
            "SELECT COUNT(*) FROM directory_districts"
    )
    public abstract int getDistrictCount();

    @Query(
            "SELECT COUNT(*) FROM directory_schools"
    )
    public abstract int getSchoolCount();

    @Query(
            "DELETE FROM directory_schools"
    )
    public abstract void deleteAllSchools();

    @Query(
            "DELETE FROM directory_districts"
    )
    public abstract void deleteAllDistricts();

    @Query(
            "DELETE FROM directory_states"
    )
    public abstract void deleteAllStates();

    @Transaction
    public void replaceCompleteDirectory(
            @NonNull List<StateDirectoryEntity> states,
            @NonNull List<DistrictDirectoryEntity> districts,
            @NonNull List<SchoolDirectoryEntity> schools
    ) {
        deleteAllSchools();
        deleteAllDistricts();
        deleteAllStates();

        insertStates(
                states
        );

        insertDistricts(
                districts
        );

        insertSchools(
                schools
        );
    }
}