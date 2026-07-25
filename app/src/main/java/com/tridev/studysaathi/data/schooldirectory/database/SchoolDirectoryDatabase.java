package com.tridev.studysaathi.data.schooldirectory.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.tridev.studysaathi.data.schooldirectory.dao.SchoolDirectoryDao;
import com.tridev.studysaathi.data.schooldirectory.entity.DistrictDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.SchoolDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.StateDirectoryEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                StateDirectoryEntity.class,
                DistrictDirectoryEntity.class,
                SchoolDirectoryEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class SchoolDirectoryDatabase
        extends RoomDatabase {

    private static final String DATABASE_NAME =
            "school_directory_database";

    private static volatile SchoolDirectoryDatabase INSTANCE;

    public static final ExecutorService
            directoryExecutor =
            Executors.newFixedThreadPool(
                    2
            );

    public abstract SchoolDirectoryDao
    schoolDirectoryDao();

    @NonNull
    public static SchoolDirectoryDatabase getInstance(
            @NonNull Context context
    ) {
        if (INSTANCE == null) {
            synchronized (
                    SchoolDirectoryDatabase.class
            ) {
                if (INSTANCE == null) {
                    INSTANCE =
                            Room.databaseBuilder(
                                            context.getApplicationContext(),
                                            SchoolDirectoryDatabase.class,
                                            DATABASE_NAME
                                    )
                                    .build();
                }
            }
        }

        return INSTANCE;
    }
}