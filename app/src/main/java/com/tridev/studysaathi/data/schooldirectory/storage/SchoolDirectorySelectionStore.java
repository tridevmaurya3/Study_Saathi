package com.tridev.studysaathi.data.schooldirectory.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class SchoolDirectorySelectionStore {

    private static final String PREFERENCES_NAME =
            "school_directory_selections";

    private static final String KEY_SELECTION_MODE =
            "selection_mode";

    private static final String KEY_STATE_CODE =
            "state_code";

    private static final String KEY_STATE_NAME =
            "state_name";

    private static final String KEY_DISTRICT_CODE =
            "district_code";

    private static final String KEY_DISTRICT_NAME =
            "district_name";

    private static final String KEY_EDUCATION_BOARD =
            "education_board";

    private static final String KEY_SCHOOL_DIRECTORY_ID =
            "school_directory_id";

    private static final String KEY_SCHOOL_NAME =
            "school_name";

    private static final String KEY_SCHOOL_CODE =
            "school_code";

    private static final String KEY_UDISE_CODE =
            "udise_code";

    private static final String KEY_BOARD_AFFILIATION_NUMBER =
            "board_affiliation_number";

    private static final String KEY_SCHOOL_INTERNAL_CODE =
            "school_internal_code";

    private static final String KEY_ADDRESS =
            "address";

    private static final String KEY_DIRECTORY_SOURCE =
            "directory_source";

    private static final String KEY_OFFICIALLY_VERIFIED =
            "officially_verified";

    private static final String KEY_SAVED_AT =
            "saved_at";

    private static final String MODE_DIRECTORY =
            "DIRECTORY";

    private static final String MODE_MANUAL =
            "MANUAL";

    private final SharedPreferences preferences;

    public SchoolDirectorySelectionStore(
            @NonNull Context context
    ) {
        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFERENCES_NAME,
                                Context.MODE_PRIVATE
                        );
    }

    /**
     * Directory से चुने गए school की पूरी identity save करता है।
     */
    public boolean saveDirectorySelection(
            long profileId,
            @Nullable String stateCode,
            @Nullable String stateName,
            @Nullable String districtCode,
            @Nullable String districtName,
            @Nullable String educationBoard,
            @Nullable String schoolDirectoryId,
            @Nullable String schoolName,
            @Nullable String schoolCode,
            @Nullable String udiseCode,
            @Nullable String boardAffiliationNumber,
            @Nullable String schoolInternalCode,
            @Nullable String address,
            @Nullable String directorySource,
            boolean officiallyVerified
    ) {
        validateProfileId(
                profileId
        );

        String safeSchoolName =
                normalizeText(
                        schoolName
                );

        if (safeSchoolName.isEmpty()) {
            throw new IllegalArgumentException(
                    "School name is required."
            );
        }

        String safeEducationBoard =
                normalizeBoard(
                        educationBoard
                );

        if (safeEducationBoard.isEmpty()) {
            throw new IllegalArgumentException(
                    "Education board is required."
            );
        }

        SharedPreferences.Editor editor =
                preferences.edit();

        String prefix =
                createProfilePrefix(
                        profileId
                );

        editor.putString(
                prefix + KEY_SELECTION_MODE,
                MODE_DIRECTORY
        );

        editor.putString(
                prefix + KEY_STATE_CODE,
                normalizeCode(
                        stateCode
                )
        );

        editor.putString(
                prefix + KEY_STATE_NAME,
                normalizeText(
                        stateName
                )
        );

        editor.putString(
                prefix + KEY_DISTRICT_CODE,
                normalizeCode(
                        districtCode
                )
        );

        editor.putString(
                prefix + KEY_DISTRICT_NAME,
                normalizeText(
                        districtName
                )
        );

        editor.putString(
                prefix + KEY_EDUCATION_BOARD,
                safeEducationBoard
        );

        editor.putString(
                prefix + KEY_SCHOOL_DIRECTORY_ID,
                normalizeIdentifier(
                        schoolDirectoryId
                )
        );

        editor.putString(
                prefix + KEY_SCHOOL_NAME,
                safeSchoolName
        );

        editor.putString(
                prefix + KEY_SCHOOL_CODE,
                normalizeOfficialCode(
                        schoolCode
                )
        );

        editor.putString(
                prefix + KEY_UDISE_CODE,
                normalizeOfficialCode(
                        udiseCode
                )
        );

        editor.putString(
                prefix + KEY_BOARD_AFFILIATION_NUMBER,
                normalizeOfficialCode(
                        boardAffiliationNumber
                )
        );

        editor.putString(
                prefix + KEY_SCHOOL_INTERNAL_CODE,
                normalizeOfficialCode(
                        schoolInternalCode
                )
        );

        editor.putString(
                prefix + KEY_ADDRESS,
                normalizeText(
                        address
                )
        );

        editor.putString(
                prefix + KEY_DIRECTORY_SOURCE,
                normalizeDirectorySource(
                        directorySource
                )
        );

        editor.putBoolean(
                prefix + KEY_OFFICIALLY_VERIFIED,
                officiallyVerified
        );

        editor.putLong(
                prefix + KEY_SAVED_AT,
                System.currentTimeMillis()
        );

        return editor.commit();
    }

    /**
     * Parent द्वारा manually भरे गए school details save करता है।
     *
     * Manual school को officially verified नहीं माना जाएगा।
     */
    public boolean saveManualSelection(
            long profileId,
            @Nullable String stateCode,
            @Nullable String stateName,
            @Nullable String districtCode,
            @Nullable String districtName,
            @Nullable String educationBoard,
            @Nullable String schoolName,
            @Nullable String schoolCode
    ) {
        validateProfileId(
                profileId
        );

        String safeSchoolName =
                normalizeText(
                        schoolName
                );

        if (safeSchoolName.isEmpty()) {
            throw new IllegalArgumentException(
                    "School name is required."
            );
        }

        String safeEducationBoard =
                normalizeBoard(
                        educationBoard
                );

        if (safeEducationBoard.isEmpty()) {
            throw new IllegalArgumentException(
                    "Education board is required."
            );
        }

        String prefix =
                createProfilePrefix(
                        profileId
                );

        SharedPreferences.Editor editor =
                preferences.edit();

        editor.putString(
                prefix + KEY_SELECTION_MODE,
                MODE_MANUAL
        );

        editor.putString(
                prefix + KEY_STATE_CODE,
                normalizeCode(
                        stateCode
                )
        );

        editor.putString(
                prefix + KEY_STATE_NAME,
                normalizeText(
                        stateName
                )
        );

        editor.putString(
                prefix + KEY_DISTRICT_CODE,
                normalizeCode(
                        districtCode
                )
        );

        editor.putString(
                prefix + KEY_DISTRICT_NAME,
                normalizeText(
                        districtName
                )
        );

        editor.putString(
                prefix + KEY_EDUCATION_BOARD,
                safeEducationBoard
        );

        editor.putString(
                prefix + KEY_SCHOOL_DIRECTORY_ID,
                ""
        );

        editor.putString(
                prefix + KEY_SCHOOL_NAME,
                safeSchoolName
        );

        editor.putString(
                prefix + KEY_SCHOOL_CODE,
                normalizeOfficialCode(
                        schoolCode
                )
        );

        editor.putString(
                prefix + KEY_UDISE_CODE,
                ""
        );

        editor.putString(
                prefix + KEY_BOARD_AFFILIATION_NUMBER,
                ""
        );

        editor.putString(
                prefix + KEY_SCHOOL_INTERNAL_CODE,
                ""
        );

        editor.putString(
                prefix + KEY_ADDRESS,
                ""
        );

        editor.putString(
                prefix + KEY_DIRECTORY_SOURCE,
                "PARENT_ENTERED"
        );

        editor.putBoolean(
                prefix + KEY_OFFICIALLY_VERIFIED,
                false
        );

        editor.putLong(
                prefix + KEY_SAVED_AT,
                System.currentTimeMillis()
        );

        return editor.commit();
    }

    /**
     * किसी student profile की saved school directory selection पढ़ता है।
     */
    @NonNull
    public SelectionData getSelection(
            long profileId
    ) {
        validateProfileId(
                profileId
        );

        String prefix =
                createProfilePrefix(
                        profileId
                );

        String selectionMode =
                safePreferenceValue(
                        prefix + KEY_SELECTION_MODE
                );

        return new SelectionData(
                profileId,
                selectionMode,
                safePreferenceValue(
                        prefix + KEY_STATE_CODE
                ),
                safePreferenceValue(
                        prefix + KEY_STATE_NAME
                ),
                safePreferenceValue(
                        prefix + KEY_DISTRICT_CODE
                ),
                safePreferenceValue(
                        prefix + KEY_DISTRICT_NAME
                ),
                safePreferenceValue(
                        prefix + KEY_EDUCATION_BOARD
                ),
                safePreferenceValue(
                        prefix + KEY_SCHOOL_DIRECTORY_ID
                ),
                safePreferenceValue(
                        prefix + KEY_SCHOOL_NAME
                ),
                safePreferenceValue(
                        prefix + KEY_SCHOOL_CODE
                ),
                safePreferenceValue(
                        prefix + KEY_UDISE_CODE
                ),
                safePreferenceValue(
                        prefix
                                + KEY_BOARD_AFFILIATION_NUMBER
                ),
                safePreferenceValue(
                        prefix + KEY_SCHOOL_INTERNAL_CODE
                ),
                safePreferenceValue(
                        prefix + KEY_ADDRESS
                ),
                safePreferenceValue(
                        prefix + KEY_DIRECTORY_SOURCE
                ),
                preferences.getBoolean(
                        prefix + KEY_OFFICIALLY_VERIFIED,
                        false
                ),
                Math.max(
                        0L,
                        preferences.getLong(
                                prefix + KEY_SAVED_AT,
                                0L
                        )
                )
        );
    }

    public boolean hasSelection(
            long profileId
    ) {
        return getSelection(
                profileId
        ).hasSchoolSelection();
    }

    /**
     * केवल दिए गए profile की directory selection हटाता है।
     *
     * Main curriculum और subjects/books इससे प्रभावित नहीं होंगे।
     */
    public boolean clearSelection(
            long profileId
    ) {
        validateProfileId(
                profileId
        );

        String prefix =
                createProfilePrefix(
                        profileId
                );

        SharedPreferences.Editor editor =
                preferences.edit();

        editor.remove(
                prefix + KEY_SELECTION_MODE
        );

        editor.remove(
                prefix + KEY_STATE_CODE
        );

        editor.remove(
                prefix + KEY_STATE_NAME
        );

        editor.remove(
                prefix + KEY_DISTRICT_CODE
        );

        editor.remove(
                prefix + KEY_DISTRICT_NAME
        );

        editor.remove(
                prefix + KEY_EDUCATION_BOARD
        );

        editor.remove(
                prefix + KEY_SCHOOL_DIRECTORY_ID
        );

        editor.remove(
                prefix + KEY_SCHOOL_NAME
        );

        editor.remove(
                prefix + KEY_SCHOOL_CODE
        );

        editor.remove(
                prefix + KEY_UDISE_CODE
        );

        editor.remove(
                prefix + KEY_BOARD_AFFILIATION_NUMBER
        );

        editor.remove(
                prefix + KEY_SCHOOL_INTERNAL_CODE
        );

        editor.remove(
                prefix + KEY_ADDRESS
        );

        editor.remove(
                prefix + KEY_DIRECTORY_SOURCE
        );

        editor.remove(
                prefix + KEY_OFFICIALLY_VERIFIED
        );

        editor.remove(
                prefix + KEY_SAVED_AT
        );

        return editor.commit();
    }

    @NonNull
    private String safePreferenceValue(
            @NonNull String key
    ) {
        return normalizeText(
                preferences.getString(
                        key,
                        ""
                )
        );
    }

    private void validateProfileId(
            long profileId
    ) {
        if (profileId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid student profile ID is required."
            );
        }
    }

    @NonNull
    private String createProfilePrefix(
            long profileId
    ) {
        return "profile_"
                + profileId
                + "_";
    }

    @NonNull
    private String normalizeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    private String normalizeCode(
            @Nullable Object value
    ) {
        return normalizeText(
                value
        )
                .toUpperCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^A-Z0-9_-]",
                        ""
                );
    }

    @NonNull
    private String normalizeOfficialCode(
            @Nullable Object value
    ) {
        return normalizeText(
                value
        )
                .toUpperCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "\\s+",
                        ""
                );
    }

    @NonNull
    private String normalizeIdentifier(
            @Nullable Object value
    ) {
        return normalizeText(
                value
        )
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
                )
                .replaceAll(
                        "^_+|_+$",
                        ""
                );
    }

    @NonNull
    private String normalizeBoard(
            @Nullable Object value
    ) {
        String normalizedBoard =
                normalizeText(
                        value
                )
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

        if (normalizedBoard.equals(
                "ICSE"
        )
                || normalizedBoard.equals(
                "ISC"
        )
                || normalizedBoard.equals(
                "CISCE_/_ICSE_/_ISC"
        )) {

            return "CISCE";
        }

        if (normalizedBoard.equals(
                "UP_BOARD"
        )
                || normalizedBoard.equals(
                "UPMSP"
        )) {

            return "UPMSP";
        }

        return normalizedBoard;
    }

    @NonNull
    private String normalizeDirectorySource(
            @Nullable Object value
    ) {
        String normalizedSource =
                normalizeText(
                        value
                )
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

    public static final class SelectionData {

        private final long profileId;

        @NonNull
        private final String selectionMode;

        @NonNull
        private final String stateCode;

        @NonNull
        private final String stateName;

        @NonNull
        private final String districtCode;

        @NonNull
        private final String districtName;

        @NonNull
        private final String educationBoard;

        @NonNull
        private final String schoolDirectoryId;

        @NonNull
        private final String schoolName;

        @NonNull
        private final String schoolCode;

        @NonNull
        private final String udiseCode;

        @NonNull
        private final String boardAffiliationNumber;

        @NonNull
        private final String schoolInternalCode;

        @NonNull
        private final String address;

        @NonNull
        private final String directorySource;

        private final boolean officiallyVerified;

        private final long savedAt;

        private SelectionData(
                long profileId,
                @NonNull String selectionMode,
                @NonNull String stateCode,
                @NonNull String stateName,
                @NonNull String districtCode,
                @NonNull String districtName,
                @NonNull String educationBoard,
                @NonNull String schoolDirectoryId,
                @NonNull String schoolName,
                @NonNull String schoolCode,
                @NonNull String udiseCode,
                @NonNull String boardAffiliationNumber,
                @NonNull String schoolInternalCode,
                @NonNull String address,
                @NonNull String directorySource,
                boolean officiallyVerified,
                long savedAt
        ) {
            this.profileId =
                    Math.max(
                            0L,
                            profileId
                    );

            this.selectionMode =
                    selectionMode;

            this.stateCode =
                    stateCode;

            this.stateName =
                    stateName;

            this.districtCode =
                    districtCode;

            this.districtName =
                    districtName;

            this.educationBoard =
                    educationBoard;

            this.schoolDirectoryId =
                    schoolDirectoryId;

            this.schoolName =
                    schoolName;

            this.schoolCode =
                    schoolCode;

            this.udiseCode =
                    udiseCode;

            this.boardAffiliationNumber =
                    boardAffiliationNumber;

            this.schoolInternalCode =
                    schoolInternalCode;

            this.address =
                    address;

            this.directorySource =
                    directorySource;

            this.officiallyVerified =
                    officiallyVerified;

            this.savedAt =
                    Math.max(
                            0L,
                            savedAt
                    );
        }

        public long getProfileId() {
            return profileId;
        }

        @NonNull
        public String getSelectionMode() {
            return selectionMode;
        }

        @NonNull
        public String getStateCode() {
            return stateCode;
        }

        @NonNull
        public String getStateName() {
            return stateName;
        }

        @NonNull
        public String getDistrictCode() {
            return districtCode;
        }

        @NonNull
        public String getDistrictName() {
            return districtName;
        }

        @NonNull
        public String getEducationBoard() {
            return educationBoard;
        }

        @NonNull
        public String getSchoolDirectoryId() {
            return schoolDirectoryId;
        }

        @NonNull
        public String getSchoolName() {
            return schoolName;
        }

        @NonNull
        public String getSchoolCode() {
            return schoolCode;
        }

        @NonNull
        public String getUdiseCode() {
            return udiseCode;
        }

        @NonNull
        public String getBoardAffiliationNumber() {
            return boardAffiliationNumber;
        }

        @NonNull
        public String getSchoolInternalCode() {
            return schoolInternalCode;
        }

        @NonNull
        public String getAddress() {
            return address;
        }

        @NonNull
        public String getDirectorySource() {
            return directorySource;
        }

        public boolean isOfficiallyVerified() {
            return officiallyVerified;
        }

        public long getSavedAt() {
            return savedAt;
        }

        public boolean hasSchoolSelection() {
            return profileId > 0L
                    && !schoolName.isEmpty()
                    && !educationBoard.isEmpty();
        }

        public boolean isDirectorySelection() {
            return MODE_DIRECTORY.equals(
                    selectionMode
            );
        }

        public boolean isManualSelection() {
            return MODE_MANUAL.equals(
                    selectionMode
            );
        }

        public boolean hasState() {
            return !stateCode.isEmpty()
                    || !stateName.isEmpty();
        }

        public boolean hasDistrict() {
            return !districtCode.isEmpty()
                    || !districtName.isEmpty();
        }

        public boolean hasUdiseCode() {
            return !udiseCode.isEmpty();
        }

        public boolean hasDirectoryIdentity() {
            return !schoolDirectoryId.isEmpty();
        }

        @NonNull
        public String getPreferredSchoolCode() {
            if (!udiseCode.isEmpty()) {
                return udiseCode;
            }

            if (!boardAffiliationNumber.isEmpty()) {
                return boardAffiliationNumber;
            }

            if (!schoolInternalCode.isEmpty()) {
                return schoolInternalCode;
            }

            return schoolCode;
        }

        @NonNull
        public String getVerificationLabel() {
            if (isManualSelection()) {
                return "Parent Entered • Not Officially Verified";
            }

            if (officiallyVerified) {
                return "Verified Directory Match";
            }

            return "Directory Record";
        }
    }
}