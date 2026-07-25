package com.tridev.studysaathi.data.schooldirectory.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "directory_districts",
        foreignKeys = {
                @ForeignKey(
                        entity = StateDirectoryEntity.class,
                        parentColumns = "state_code",
                        childColumns = "state_code",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(
                        value = {"state_code"}
                ),
                @Index(
                        value = {
                                "state_code",
                                "district_name"
                        },
                        unique = true
                ),
                @Index(
                        value = {
                                "state_code",
                                "is_active",
                                "sort_order"
                        }
                )
        }
)
public class DistrictDirectoryEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "district_code")
    private String districtCode = "";

    @NonNull
    @ColumnInfo(name = "state_code")
    private String stateCode = "";

    @NonNull
    @ColumnInfo(name = "district_name")
    private String districtName = "";

    @NonNull
    @ColumnInfo(name = "district_name_hindi")
    private String districtNameHindi = "";

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

    public DistrictDirectoryEntity() {
        /*
         * Required empty constructor for Room.
         */
    }

    @NonNull
    public String getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(
            @NonNull String districtCode
    ) {
        this.districtCode =
                normalizeCode(
                        districtCode,
                        "District code"
                );
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
                        stateCode,
                        "State code"
                );
    }

    @NonNull
    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(
            @NonNull String districtName
    ) {
        this.districtName =
                normalizeRequiredText(
                        districtName,
                        "District name"
                );
    }

    @NonNull
    public String getDistrictNameHindi() {
        return districtNameHindi;
    }

    public void setDistrictNameHindi(
            @NonNull String districtNameHindi
    ) {
        this.districtNameHindi =
                normalizeOptionalText(
                        districtNameHindi
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
                && !districtNameHindi.isEmpty()) {

            return districtNameHindi;
        }

        return districtName;
    }

    @NonNull
    private static String normalizeCode(
            @NonNull String value,
            @NonNull String fieldName
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
                    fieldName
                            + " is required."
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