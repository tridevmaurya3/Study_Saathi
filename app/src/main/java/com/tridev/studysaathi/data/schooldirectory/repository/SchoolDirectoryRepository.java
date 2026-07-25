package com.tridev.studysaathi.data.schooldirectory.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.schooldirectory.dao.SchoolDirectoryDao;
import com.tridev.studysaathi.data.schooldirectory.database.SchoolDirectoryDatabase;
import com.tridev.studysaathi.data.schooldirectory.entity.DistrictDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.SchoolDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.StateDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.seed.SchoolDirectorySeedProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SchoolDirectoryRepository {

    private static final int DEFAULT_SEARCH_LIMIT =
            50;

    private static final int MAXIMUM_SEARCH_LIMIT =
            100;

    private static final Object SEED_LOCK =
            new Object();

    private final SchoolDirectoryDao
            schoolDirectoryDao;

    private final Handler mainThreadHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    public SchoolDirectoryRepository(
            @NonNull Context context
    ) {
        SchoolDirectoryDatabase database =
                SchoolDirectoryDatabase.getInstance(
                        context
                );

        schoolDirectoryDao =
                database.schoolDirectoryDao();
    }

    /**
     * Starter State और District directory को केवल जरूरत पड़ने पर seed करता है।
     *
     * Existing school records को यह method delete या replace नहीं करेगा।
     */
    public void ensureStarterDirectory(
            @NonNull StarterDirectoryCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        boolean statesSeeded =
                                false;

                        boolean districtsSeeded =
                                false;

                        boolean schoolsSeeded =
                                false;

                        synchronized (SEED_LOCK) {
                            int currentStateCount =
                                    schoolDirectoryDao
                                            .getStateCount();

                            int currentDistrictCount =
                                    schoolDirectoryDao
                                            .getDistrictCount();

                            int currentSchoolCount =
                                    schoolDirectoryDao
                                            .getSchoolCount();

                            SchoolDirectorySeedProvider
                                    .StarterDirectoryData starterData =
                                    SchoolDirectorySeedProvider
                                            .createStarterDirectoryData();

                            if (currentStateCount <= 0) {
                                schoolDirectoryDao
                                        .insertStates(
                                                starterData
                                                        .getStates()
                                        );

                                statesSeeded =
                                        true;
                            }

                            if (currentDistrictCount <= 0) {
                                schoolDirectoryDao
                                        .insertDistricts(
                                                starterData
                                                        .getDistricts()
                                        );

                                districtsSeeded =
                                        true;
                            }

                            /*
                             * Starter build में fabricated schools नहीं हैं।
                             * भविष्य में verified starter schools उपलब्ध होने
                             * पर ही वे यहाँ insert होंगी।
                             *
                             * Existing imported school records कभी delete
                             * या overwrite नहीं किए जाएँगे।
                             */
                            if (currentSchoolCount <= 0
                                    && starterData
                                    .hasSchoolRecords()) {

                                schoolDirectoryDao
                                        .insertSchools(
                                                starterData
                                                        .getSchools()
                                        );

                                schoolsSeeded =
                                        true;
                            }
                        }

                        DirectoryStatistics statistics =
                                readDirectoryStatistics();

                        StarterDirectoryResult result =
                                new StarterDirectoryResult(
                                        statesSeeded,
                                        districtsSeeded,
                                        schoolsSeeded,
                                        statistics
                                );

                        postToMainThread(() ->
                                callback.onReady(
                                        result
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    public void getActiveStates(
            @NonNull StatesCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        List<StateDirectoryEntity> states =
                                schoolDirectoryDao
                                        .getActiveStates();

                        postToMainThread(() ->
                                callback.onSuccess(
                                        states
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    public void getDistrictsForState(
            @NonNull String stateCode,
            @NonNull DistrictsCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        String safeStateCode =
                                normalizeRequiredCode(
                                        stateCode,
                                        "State code"
                                );

                        List<DistrictDirectoryEntity>
                                districts =
                                schoolDirectoryDao
                                        .getActiveDistrictsForState(
                                                safeStateCode
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        districts
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    public void getEducationBoardsForDistrict(
            @NonNull String districtCode,
            @NonNull BoardsCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        String safeDistrictCode =
                                normalizeRequiredCode(
                                        districtCode,
                                        "District code"
                                );

                        List<String> directoryBoards =
                                schoolDirectoryDao
                                        .getEducationBoardsForDistrict(
                                                safeDistrictCode
                                        );

                        List<String> finalBoards =
                                mergeEducationBoards(
                                        directoryBoards
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        finalBoards
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Standard supported board list देता है।
     *
     * School records खाली होने पर भी Board dropdown काम करेगी।
     */
    public void getSupportedEducationBoards(
            @NonNull BoardsCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        List<String> supportedBoards =
                                new ArrayList<>(
                                        SchoolDirectorySeedProvider
                                                .createSupportedEducationBoards()
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        supportedBoards
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    public void getSchools(
            @NonNull String districtCode,
            @NonNull String educationBoard,
            @NonNull SchoolsCallback callback
    ) {
        searchSchools(
                districtCode,
                educationBoard,
                "",
                DEFAULT_SEARCH_LIMIT,
                callback
        );
    }

    public void searchSchools(
            @NonNull String districtCode,
            @NonNull String educationBoard,
            @Nullable String searchText,
            int resultLimit,
            @NonNull SchoolsCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        String safeDistrictCode =
                                normalizeRequiredCode(
                                        districtCode,
                                        "District code"
                                );

                        String safeEducationBoard =
                                normalizeRequiredBoard(
                                        educationBoard
                                );

                        String safeSearchText =
                                safeText(
                                        searchText
                                );

                        int safeResultLimit =
                                Math.max(
                                        1,
                                        Math.min(
                                                MAXIMUM_SEARCH_LIMIT,
                                                resultLimit
                                        )
                                );

                        List<SchoolDirectoryEntity> schools;

                        if (safeSearchText.isEmpty()) {
                            schools =
                                    schoolDirectoryDao
                                            .getSchools(
                                                    safeDistrictCode,
                                                    safeEducationBoard,
                                                    safeResultLimit
                                            );

                        } else {
                            schools =
                                    schoolDirectoryDao
                                            .searchSchools(
                                                    safeDistrictCode,
                                                    safeEducationBoard,
                                                    safeSearchText,
                                                    safeResultLimit
                                            );
                        }

                        postToMainThread(() ->
                                callback.onSuccess(
                                        schools
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    public void getSchoolByDirectoryId(
            @NonNull String schoolDirectoryId,
            @NonNull SingleSchoolCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        String safeSchoolDirectoryId =
                                safeText(
                                        schoolDirectoryId
                                );

                        if (safeSchoolDirectoryId.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "School directory ID is required."
                            );
                        }

                        SchoolDirectoryEntity school =
                                schoolDirectoryDao
                                        .getSchoolByDirectoryId(
                                                safeSchoolDirectoryId
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        school
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    public void getSchoolByUdiseCode(
            @NonNull String udiseCode,
            @NonNull SingleSchoolCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        String safeUdiseCode =
                                normalizeOfficialCode(
                                        udiseCode
                                );

                        if (safeUdiseCode.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "UDISE code is required."
                            );
                        }

                        SchoolDirectoryEntity school =
                                schoolDirectoryDao
                                        .getSchoolByUdiseCode(
                                                safeUdiseCode
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        school
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    public void insertOrUpdateSchool(
            @NonNull SchoolDirectoryEntity school,
            @NonNull OperationCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        validateSchool(
                                school
                        );

                        schoolDirectoryDao
                                .insertSchool(
                                        school
                                );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * पूरा verified/imported directory replace करता है।
     *
     * इसे केवल explicit Admin/Import flow से call करना चाहिए।
     */
    public void replaceCompleteDirectory(
            @NonNull List<StateDirectoryEntity> states,
            @NonNull List<DistrictDirectoryEntity> districts,
            @NonNull List<SchoolDirectoryEntity> schools,
            @NonNull OperationCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        List<StateDirectoryEntity> safeStates =
                                states == null
                                        ? Collections.emptyList()
                                        : states;

                        List<DistrictDirectoryEntity>
                                safeDistricts =
                                districts == null
                                        ? Collections.emptyList()
                                        : districts;

                        List<SchoolDirectoryEntity> safeSchools =
                                schools == null
                                        ? Collections.emptyList()
                                        : schools;

                        schoolDirectoryDao
                                .replaceCompleteDirectory(
                                        safeStates,
                                        safeDistricts,
                                        safeSchools
                                );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    public void getDirectoryStatistics(
            @NonNull DirectoryStatisticsCallback callback
    ) {
        SchoolDirectoryDatabase
                .directoryExecutor
                .execute(() -> {
                    try {
                        DirectoryStatistics statistics =
                                readDirectoryStatistics();

                        postToMainThread(() ->
                                callback.onSuccess(
                                        statistics
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    @NonNull
    private DirectoryStatistics readDirectoryStatistics() {
        int stateCount =
                schoolDirectoryDao
                        .getStateCount();

        int districtCount =
                schoolDirectoryDao
                        .getDistrictCount();

        int schoolCount =
                schoolDirectoryDao
                        .getSchoolCount();

        return new DirectoryStatistics(
                stateCount,
                districtCount,
                schoolCount
        );
    }

    @NonNull
    private List<String> mergeEducationBoards(
            @Nullable List<String> directoryBoards
    ) {
        List<String> mergedBoards =
                new ArrayList<>();

        if (directoryBoards != null) {
            for (String board :
                    directoryBoards) {

                String normalizedBoard =
                        normalizeOptionalBoard(
                                board
                        );

                if (!normalizedBoard.isEmpty()
                        && !mergedBoards.contains(
                        normalizedBoard
                )) {
                    mergedBoards.add(
                            normalizedBoard
                    );
                }
            }
        }

        for (String supportedBoard :
                SchoolDirectorySeedProvider
                        .createSupportedEducationBoards()) {

            String normalizedBoard =
                    normalizeOptionalBoard(
                            supportedBoard
                    );

            if (!normalizedBoard.isEmpty()
                    && !mergedBoards.contains(
                    normalizedBoard
            )) {
                mergedBoards.add(
                        normalizedBoard
                );
            }
        }

        return Collections.unmodifiableList(
                mergedBoards
        );
    }

    private void validateSchool(
            @NonNull SchoolDirectoryEntity school
    ) {
        if (safeText(
                school.getSchoolDirectoryId()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "School directory ID is required."
            );
        }

        if (safeText(
                school.getStateCode()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "State code is required."
            );
        }

        if (safeText(
                school.getDistrictCode()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "District code is required."
            );
        }

        if (safeText(
                school.getSchoolName()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "School name is required."
            );
        }

        if (safeText(
                school.getEducationBoard()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Education board is required."
            );
        }
    }

    @NonNull
    private String normalizeRequiredCode(
            @Nullable String value,
            @NonNull String fieldName
    ) {
        String normalizedValue =
                safeText(
                        value
                )
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
    private String normalizeRequiredBoard(
            @Nullable String value
    ) {
        String normalizedBoard =
                normalizeOptionalBoard(
                        value
                );

        if (normalizedBoard.isEmpty()) {
            throw new IllegalArgumentException(
                    "Education board is required."
            );
        }

        return normalizedBoard;
    }

    @NonNull
    private String normalizeOptionalBoard(
            @Nullable String value
    ) {
        String normalizedBoard =
                safeText(
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
    private String normalizeOfficialCode(
            @Nullable String value
    ) {
        return safeText(
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
    private String safeText(
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

    private void postToMainThread(
            @NonNull Runnable runnable
    ) {
        mainThreadHandler.post(
                runnable
        );
    }

    private void postError(
            @NonNull ErrorCallback callback,
            @NonNull Exception exception
    ) {
        postToMainThread(() ->
                callback.onError(
                        exception
                )
        );
    }

    public interface ErrorCallback {

        void onError(
                @NonNull Exception exception
        );
    }

    public interface StarterDirectoryCallback
            extends ErrorCallback {

        void onReady(
                @NonNull StarterDirectoryResult result
        );
    }

    public interface StatesCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<StateDirectoryEntity> states
        );
    }

    public interface DistrictsCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<DistrictDirectoryEntity> districts
        );
    }

    public interface BoardsCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<String> educationBoards
        );
    }

    public interface SchoolsCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<SchoolDirectoryEntity> schools
        );
    }

    public interface SingleSchoolCallback
            extends ErrorCallback {

        void onSuccess(
                @Nullable SchoolDirectoryEntity school
        );
    }

    public interface DirectoryStatisticsCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull DirectoryStatistics statistics
        );
    }

    public interface OperationCallback
            extends ErrorCallback {

        void onSuccess();
    }

    public static final class StarterDirectoryResult {

        private final boolean statesSeeded;

        private final boolean districtsSeeded;

        private final boolean schoolsSeeded;

        @NonNull
        private final DirectoryStatistics statistics;

        private StarterDirectoryResult(
                boolean statesSeeded,
                boolean districtsSeeded,
                boolean schoolsSeeded,
                @NonNull DirectoryStatistics statistics
        ) {
            this.statesSeeded =
                    statesSeeded;

            this.districtsSeeded =
                    districtsSeeded;

            this.schoolsSeeded =
                    schoolsSeeded;

            this.statistics =
                    statistics;
        }

        public boolean wereStatesSeeded() {
            return statesSeeded;
        }

        public boolean wereDistrictsSeeded() {
            return districtsSeeded;
        }

        public boolean wereSchoolsSeeded() {
            return schoolsSeeded;
        }

        public boolean wasAnyDataSeeded() {
            return statesSeeded
                    || districtsSeeded
                    || schoolsSeeded;
        }

        @NonNull
        public DirectoryStatistics getStatistics() {
            return statistics;
        }
    }

    public static final class DirectoryStatistics {

        private final int stateCount;

        private final int districtCount;

        private final int schoolCount;

        private DirectoryStatistics(
                int stateCount,
                int districtCount,
                int schoolCount
        ) {
            this.stateCount =
                    Math.max(
                            0,
                            stateCount
                    );

            this.districtCount =
                    Math.max(
                            0,
                            districtCount
                    );

            this.schoolCount =
                    Math.max(
                            0,
                            schoolCount
                    );
        }

        public int getStateCount() {
            return stateCount;
        }

        public int getDistrictCount() {
            return districtCount;
        }

        public int getSchoolCount() {
            return schoolCount;
        }

        public boolean hasStateData() {
            return stateCount > 0;
        }

        public boolean hasDistrictData() {
            return districtCount > 0;
        }

        public boolean hasSchoolData() {
            return schoolCount > 0;
        }

        public boolean hasDirectoryFoundation() {
            return stateCount > 0
                    && districtCount > 0;
        }

        public boolean hasCompleteDirectoryData() {
            return stateCount > 0
                    && districtCount > 0
                    && schoolCount > 0;
        }
    }
}