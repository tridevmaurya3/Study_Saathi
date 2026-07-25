package com.tridev.studysaathi.data.schooldirectory.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Locale;

@Entity(
        tableName = "directory_schools",
        foreignKeys = {
                @ForeignKey(
                        entity = StateDirectoryEntity.class,
                        parentColumns = "state_code",
                        childColumns = "state_code",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = DistrictDirectoryEntity.class,
                        parentColumns = "district_code",
                        childColumns = "district_code",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(
                        value = {"state_code"}
                ),
                @Index(
                        value = {"district_code"}
                ),
                @Index(
                        value = {"education_board"}
                ),
                @Index(
                        value = {"school_name"}
                ),
                @Index(
                        value = {"udise_code"},
                        unique = true
                ),
                @Index(
                        value = {
                                "district_code",
                                "education_board",
                                "is_active"
                        }
                )
        }
)
public class SchoolDirectoryEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "school_directory_id")
    private String schoolDirectoryId = "";

    @NonNull
    @ColumnInfo(name = "state_code")
    private String stateCode = "";

    @NonNull
    @ColumnInfo(name = "district_code")
    private String districtCode = "";

    @NonNull
    @ColumnInfo(name = "school_name")
    private String schoolName = "";

    @NonNull
    @ColumnInfo(name = "school_name_hindi")
    private String schoolNameHindi = "";

    @NonNull
    @ColumnInfo(name = "education_board")
    private String educationBoard = "";

    @NonNull
    @ColumnInfo(name = "school_type")
    private String schoolType = "";

    @NonNull
    @ColumnInfo(name = "management_type")
    private String managementType = "";

    @NonNull
    @ColumnInfo(name = "udise_code")
    private String udiseCode = "";

    @NonNull
    @ColumnInfo(name = "board_affiliation_number")
    private String boardAffiliationNumber = "";

    @NonNull
    @ColumnInfo(name = "school_internal_code")
    private String schoolInternalCode = "";

    @NonNull
    @ColumnInfo(name = "address_line")
    private String addressLine = "";

    @NonNull
    @ColumnInfo(name = "postal_code")
    private String postalCode = "";

    @NonNull
    @ColumnInfo(name = "directory_source")
    private String directorySource =
            "LOCAL_DIRECTORY";

    @ColumnInfo(
            name = "officially_verified",
            defaultValue = "0"
    )
    private boolean officiallyVerified;

    @ColumnInfo(
            name = "is_active",
            defaultValue = "1"
    )
    private boolean active = true;

    @ColumnInfo(name = "source_updated_at")
    private long sourceUpdatedAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public SchoolDirectoryEntity() {
        /*
         * Required empty constructor for Room.
         */
    }

    @NonNull
    public String getSchoolDirectoryId() {
        return schoolDirectoryId;
    }

    public void setSchoolDirectoryId(
            @NonNull String schoolDirectoryId
    ) {
        this.schoolDirectoryId =
                normalizeIdentifier(
                        schoolDirectoryId
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
    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(
            @NonNull String schoolName
    ) {
        this.schoolName =
                normalizeRequiredText(
                        schoolName,
                        "School name"
                );
    }

    @NonNull
    public String getSchoolNameHindi() {
        return schoolNameHindi;
    }

    public void setSchoolNameHindi(
            @NonNull String schoolNameHindi
    ) {
        this.schoolNameHindi =
                normalizeOptionalText(
                        schoolNameHindi
                );
    }

    @NonNull
    public String getEducationBoard() {
        return educationBoard;
    }

    public void setEducationBoard(
            @NonNull String educationBoard
    ) {
        this.educationBoard =
                normalizeBoard(
                        educationBoard
                );
    }

    @NonNull
    public String getSchoolType() {
        return schoolType;
    }

    public void setSchoolType(
            @NonNull String schoolType
    ) {
        this.schoolType =
                normalizeOptionalText(
                        schoolType
                );
    }

    @NonNull
    public String getManagementType() {
        return managementType;
    }

    public void setManagementType(
            @NonNull String managementType
    ) {
        this.managementType =
                normalizeOptionalText(
                        managementType
                );
    }

    @NonNull
    public String getUdiseCode() {
        return udiseCode;
    }

    public void setUdiseCode(
            @NonNull String udiseCode
    ) {
        this.udiseCode =
                normalizeOfficialCode(
                        udiseCode
                );
    }

    @NonNull
    public String getBoardAffiliationNumber() {
        return boardAffiliationNumber;
    }

    public void setBoardAffiliationNumber(
            @NonNull String boardAffiliationNumber
    ) {
        this.boardAffiliationNumber =
                normalizeOfficialCode(
                        boardAffiliationNumber
                );
    }

    @NonNull
    public String getSchoolInternalCode() {
        return schoolInternalCode;
    }

    public void setSchoolInternalCode(
            @NonNull String schoolInternalCode
    ) {
        this.schoolInternalCode =
                normalizeOfficialCode(
                        schoolInternalCode
                );
    }

    @NonNull
    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(
            @NonNull String addressLine
    ) {
        this.addressLine =
                normalizeOptionalText(
                        addressLine
                );
    }

    @NonNull
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(
            @NonNull String postalCode
    ) {
        this.postalCode =
                normalizeOfficialCode(
                        postalCode
                );
    }

    @NonNull
    public String getDirectorySource() {
        return directorySource;
    }

    public void setDirectorySource(
            @NonNull String directorySource
    ) {
        this.directorySource =
                normalizeDirectorySource(
                        directorySource
                );
    }

    public boolean isOfficiallyVerified() {
        return officiallyVerified;
    }

    public void setOfficiallyVerified(
            boolean officiallyVerified
    ) {
        this.officiallyVerified =
                officiallyVerified;
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

    public long getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public void setSourceUpdatedAt(
            long sourceUpdatedAt
    ) {
        this.sourceUpdatedAt =
                Math.max(
                        0L,
                        sourceUpdatedAt
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
                && !schoolNameHindi.isEmpty()) {

            return schoolNameHindi;
        }

        return schoolName;
    }

    @NonNull
    public String getPreferredSchoolCode() {
        if (!udiseCode.isEmpty()) {
            return udiseCode;
        }

        if (!boardAffiliationNumber.isEmpty()) {
            return boardAffiliationNumber;
        }

        return schoolInternalCode;
    }

    @NonNull
    public String getVerificationLabel() {
        return officiallyVerified
                ? "Verified Directory Match"
                : "Directory Record";
    }

    @NonNull
    private static String normalizeIdentifier(
            @NonNull String value
    ) {
        String normalizedValue =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9_-]",
                                "_"
                        )
                        .replaceAll(
                                "_+",
                                "_"
                        );

        while (normalizedValue.startsWith("_")) {
            normalizedValue =
                    normalizedValue.substring(
                            1
                    );
        }

        while (normalizedValue.endsWith("_")) {
            normalizedValue =
                    normalizedValue.substring(
                            0,
                            normalizedValue.length() - 1
                    );
        }

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    "School directory ID is required."
            );
        }

        return normalizedValue;
    }

    @NonNull
    private static String normalizeCode(
            @NonNull String value,
            @NonNull String fieldName
    ) {
        String normalizedValue =
                value.trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
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
                normalizeOptionalText(
                        value
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

    @NonNull
    private static String normalizeOfficialCode(
            @NonNull String value
    ) {
        return value.trim()
                .toUpperCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "\\s+",
                        ""
                );
    }

    @NonNull
    private static String normalizeBoard(
            @NonNull String value
    ) {
        String normalizedBoard =
                value.trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replace(
                                "-",
                                "_"
                        )
                        .replace(
                                " ",
                                "_"
                        );

        if (normalizedBoard.equals("ICSE")
                || normalizedBoard.equals("ISC")) {

            return "CISCE";
        }

        if (normalizedBoard.equals("UP_BOARD")
                || normalizedBoard.equals("UPMSP")) {

            return "UPMSP";
        }

        if (normalizedBoard.isEmpty()) {
            return "OTHER";
        }

        return normalizedBoard;
    }

    @NonNull
    private static String normalizeDirectorySource(
            @NonNull String value
    ) {
        String normalizedSource =
                value.trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replace(
                                "-",
                                "_"
                        )
                        .replace(
                                " ",
                                "_"
                        );

        switch (normalizedSource) {
            case "UDISE":
            case "CBSE":
            case "CISCE":
            case "STATE_BOARD":
            case "LOCAL_DIRECTORY":
            case "PARENT_IMPORTED":
                return normalizedSource;

            default:
                return "LOCAL_DIRECTORY";
        }
    }
}