package com.tridev.studysaathi.data.schooldirectory.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "directory_states",
        indices = {
                @Index(
                        value = {"state_name"},
                        unique = true
                ),
                @Index(
                        value = {
                                "is_active",
                                "sort_order"
                        }
                )
        }
)
public class StateDirectoryEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "state_code")
    private String stateCode = "";

    @NonNull
    @ColumnInfo(name = "state_name")
    private String stateName = "";

    @NonNull
    @ColumnInfo(name = "state_name_hindi")
    private String stateNameHindi = "";

    @ColumnInfo(
            name = "is_active",
            defaultValue = "1"
    )
    private boolean active = true;

    @ColumnInfo(
            name = "sort_order",
            defaultValue = "0"
    )
    private int sortOrder;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public StateDirectoryEntity() {
        /*
         * Required empty constructor for Room.
         */
    }

    @NonNull
    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(
            @NonNull String stateCode
    ) {
        this.stateCode =
                normalizeCode(
                        stateCode
                );
    }

    @NonNull
    public String getStateName() {
        return stateName;
    }

    public void setStateName(
            @NonNull String stateName
    ) {
        this.stateName =
                normalizeRequiredText(
                        stateName,
                        "State name"
                );
    }

    @NonNull
    public String getStateNameHindi() {
        return stateNameHindi;
    }

    public void setStateNameHindi(
            @NonNull String stateNameHindi
    ) {
        this.stateNameHindi =
                normalizeOptionalText(
                        stateNameHindi
                );
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(
            boolean active
    ) {
        this.active =
                active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(
            int sortOrder
    ) {
        this.sortOrder =
                Math.max(
                        0,
                        sortOrder
                );
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            long createdAt
    ) {
        this.createdAt =
                Math.max(
                        0L,
                        createdAt
                );
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            long updatedAt
    ) {
        this.updatedAt =
                Math.max(
                        0L,
                        updatedAt
                );
    }

    @NonNull
    public String getDisplayName(
            boolean useHindi
    ) {
        if (useHindi
                && !stateNameHindi.isEmpty()) {

            return stateNameHindi;
        }

        return stateName;
    }

    @NonNull
    private static String normalizeCode(
            @NonNull String value
    ) {
        String normalizedValue =
                value.trim()
                        .toUpperCase()
                        .replaceAll(
                                "[^A-Z0-9_-]",
                                ""
                        );

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    "State code is required."
            );
        }

        return normalizedValue;
    }

    @NonNull
    private static String normalizeRequiredText(
            @NonNull String value,
            @NonNull String fieldName
    ) {
        String normalizedValue =
                value.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName
                            + " is required."
            );
        }

        return normalizedValue;
    }

    @NonNull
    private static String normalizeOptionalText(
            @NonNull String value
    ) {
        return value.trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }
}