package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.dao.SchoolCurriculumProfileDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SchoolCurriculumProfileRepository {

    private static final String DEFAULT_SCHOOL_NAME =
            "School details pending";

    private static final String DEFAULT_SCHOOL_PATTERN =
            "SCHOOL_SPECIFIC";

    private static final String DEFAULT_AI_LANGUAGE =
            "BILINGUAL";

    private static final String DEFAULT_AI_ANSWER_MODE =
            "SIMPLE";

    private static final int DEFAULT_MAXIMUM_ANSWER_WORDS =
            300;

    @NonNull
    private final SchoolCurriculumProfileDao
            curriculumProfileDao;

    @NonNull
    private final Handler mainThreadHandler;

    public SchoolCurriculumProfileRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(
                        context.getApplicationContext()
                );

        curriculumProfileDao =
                database.schoolCurriculumProfileDao();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Student profile ID से curriculum profile पढ़ता है।
     */
    public void getCurriculumProfile(
            long profileId,
            @NonNull SingleProfileCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        SchoolCurriculumProfileEntity profile =
                                curriculumProfileDao
                                        .getCurriculumProfileByProfileId(
                                                profileId
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        profile
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
     * केवल पूरी तरह configured curriculum profile पढ़ता है।
     */
    public void getConfiguredCurriculumProfile(
            long profileId,
            @NonNull SingleProfileCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        SchoolCurriculumProfileEntity profile =
                                curriculumProfileDao
                                        .getConfiguredProfile(
                                                profileId
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        profile
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
     * Curriculum ID से profile पढ़ता है।
     */
    public void getCurriculumProfileByCurriculumId(
            @NonNull String curriculumId,
            @NonNull SingleProfileCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        String safeCurriculumId =
                                safeText(
                                        curriculumId
                                );

                        if (safeCurriculumId.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Curriculum ID is required."
                            );
                        }

                        SchoolCurriculumProfileEntity profile =
                                curriculumProfileDao
                                        .getCurriculumProfileByCurriculumId(
                                                safeCurriculumId
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        profile
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
     * सभी curriculum profiles पढ़ता है।
     */
    public void getAllCurriculumProfiles(
            boolean configuredProfilesOnly,
            @NonNull ProfilesCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        List<SchoolCurriculumProfileEntity> profiles;

                        if (configuredProfilesOnly) {
                            profiles =
                                    curriculumProfileDao
                                            .getAllConfiguredProfiles();

                        } else {
                            profiles =
                                    curriculumProfileDao
                                            .getAllCurriculumProfiles();
                        }

                        List<SchoolCurriculumProfileEntity>
                                safeProfiles =
                                profiles == null
                                        ? Collections.emptyList()
                                        : profiles;

                        postToMainThread(() ->
                                callback.onSuccess(
                                        safeProfiles
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
     * Student के लिए curriculum profile उपलब्ध कराता है।
     *
     * Existing profile मिलने पर वही लौटेगी।
     *
     * Profile न मिलने पर StudentProfileEntity की board,
     * class और medium information से incomplete profile
     * बनाई जाएगी।
     */
    public void ensureBasicCurriculumProfile(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull EnsureProfileCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateStudentProfile(
                                studentProfile
                        );

                        long profileId =
                                studentProfile.getProfileId();

                        SchoolCurriculumProfileEntity
                                existingProfile =
                                curriculumProfileDao
                                        .getCurriculumProfileByProfileId(
                                                profileId
                                        );

                        if (existingProfile != null) {
                            postToMainThread(() ->
                                    callback.onReady(
                                            existingProfile,
                                            false
                                    )
                            );

                            return;
                        }

                        SchoolCurriculumProfileEntity
                                newCurriculumProfile =
                                createBasicCurriculumProfile(
                                        studentProfile
                                );

                        curriculumProfileDao
                                .insertCurriculumProfile(
                                        newCurriculumProfile
                                );

                        postToMainThread(() ->
                                callback.onReady(
                                        newCurriculumProfile,
                                        true
                                )
                        );

                    } catch (Exception exception) {
                        /*
                         * किसी rare duplicate/race condition में
                         * insert fail होने पर existing row दोबारा
                         * पढ़ने की कोशिश की जाएगी।
                         */
                        try {
                            long profileId =
                                    studentProfile.getProfileId();

                            SchoolCurriculumProfileEntity
                                    existingProfile =
                                    curriculumProfileDao
                                            .getCurriculumProfileByProfileId(
                                                    profileId
                                            );

                            if (existingProfile != null) {
                                postToMainThread(() ->
                                        callback.onReady(
                                                existingProfile,
                                                false
                                        )
                                );

                                return;
                            }

                        } catch (Exception ignored) {
                            /*
                             * Original error नीचे callback
                             * में लौटाई जाएगी।
                             */
                        }

                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Curriculum profile insert या update करता है।
     */
    public void insertOrUpdateCurriculumProfile(
            @NonNull SchoolCurriculumProfileEntity profile,
            @NonNull SaveProfileCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateCurriculumProfile(
                                profile
                        );

                        long currentTime =
                                System.currentTimeMillis();

                        if (profile.getCreatedAt() <= 0L) {
                            profile.setCreatedAt(
                                    currentTime
                            );
                        }

                        profile.setUpdatedAt(
                                currentTime
                        );

                        long savedProfileId =
                                curriculumProfileDao
                                        .insertOrUpdateCurriculumProfile(
                                                profile
                                        );

                        if (savedProfileId <= 0L) {
                            throw new IllegalStateException(
                                    "Curriculum profile could not be saved."
                            );
                        }

                        postToMainThread(() ->
                                callback.onSuccess(
                                        savedProfileId
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
     * School identity information update करता है।
     */
    public void updateSchoolInformation(
            long profileId,
            @Nullable String schoolName,
            @Nullable String schoolCode,
            @Nullable String educationBoard,
            @Nullable String schoolPattern,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        String safeSchoolName =
                                safeText(
                                        schoolName
                                );

                        String safeEducationBoard =
                                safeText(
                                        educationBoard
                                );

                        if (safeSchoolName.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "School name is required."
                            );
                        }

                        if (safeEducationBoard.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Education board is required."
                            );
                        }

                        int updatedRows =
                                curriculumProfileDao
                                        .updateSchoolInformation(
                                                profileId,
                                                safeSchoolName,
                                                safeText(
                                                        schoolCode
                                                ),
                                                safeEducationBoard,
                                                valueOrFallback(
                                                        schoolPattern,
                                                        DEFAULT_SCHOOL_PATTERN
                                                ),
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
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
     * Class, section, session और study medium update करता है।
     */
    public void updateAcademicInformation(
            long profileId,
            int classNumber,
            @Nullable String section,
            @Nullable String academicSession,
            @Nullable String studyMedium,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        if (classNumber < 1
                                || classNumber > 12) {

                            throw new IllegalArgumentException(
                                    "Class number must be between 1 and 12."
                            );
                        }

                        String safeAcademicSession =
                                valueOrFallback(
                                        academicSession,
                                        createCurrentAcademicSession()
                                );

                        String safeStudyMedium =
                                safeText(
                                        studyMedium
                                );

                        if (safeStudyMedium.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Study medium is required."
                            );
                        }

                        int updatedRows =
                                curriculumProfileDao
                                        .updateAcademicInformation(
                                                profileId,
                                                classNumber,
                                                safeText(
                                                        section
                                                ),
                                                safeAcademicSession,
                                                safeStudyMedium,
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
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
     * Curriculum profile को configured या incomplete करता है।
     */
    public void setProfileConfigured(
            long profileId,
            boolean configured,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        int updatedRows =
                                curriculumProfileDao
                                        .setProfileConfigured(
                                                profileId,
                                                configured,
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
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
     * AI Tutor enable या disable करता है।
     */
    public void setAiTutorEnabled(
            long profileId,
            boolean enabled,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        int updatedRows =
                                curriculumProfileDao
                                        .setAiTutorEnabled(
                                                profileId,
                                                enabled,
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
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
     * AI Tutor language, answer mode और word limit update करता है।
     */
    public void updateAiTutorPreferences(
            long profileId,
            @Nullable String defaultLanguage,
            @Nullable String defaultAnswerMode,
            int maximumAnswerWords,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        int safeMaximumWords =
                                Math.max(
                                        50,
                                        Math.min(
                                                1000,
                                                maximumAnswerWords
                                        )
                                );

                        int updatedRows =
                                curriculumProfileDao
                                        .updateAiTutorPreferences(
                                                profileId,
                                                valueOrFallback(
                                                        defaultLanguage,
                                                        DEFAULT_AI_LANGUAGE
                                                ),
                                                valueOrFallback(
                                                        defaultAnswerMode,
                                                        DEFAULT_AI_ANSWER_MODE
                                                ),
                                                safeMaximumWords,
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
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
     * Voice, image और read-aloud settings update करता है।
     */
    public void updateAiInputAndSpeechSettings(
            long profileId,
            boolean voiceQuestionEnabled,
            boolean imageQuestionEnabled,
            boolean readAnswerAloudEnabled,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        int updatedRows =
                                curriculumProfileDao
                                        .updateAiInputAndSpeechSettings(
                                                profileId,
                                                voiceQuestionEnabled,
                                                imageQuestionEnabled,
                                                readAnswerAloudEnabled,
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
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
     * Child-safe answers और doubt-history settings update करता है।
     */
    public void updateSafetyAndHistorySettings(
            long profileId,
            boolean childSafeAnswersEnabled,
            boolean saveDoubtHistoryEnabled,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        int updatedRows =
                                curriculumProfileDao
                                        .updateSafetyAndHistorySettings(
                                                profileId,
                                                childSafeAnswersEnabled,
                                                saveDoubtHistoryEnabled,
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
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
     * Student profile ID से curriculum profile delete करता है।
     */
    public void deleteCurriculumProfile(
            long profileId,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        int deletedRows =
                                curriculumProfileDao
                                        .deleteCurriculumProfileByProfileId(
                                                profileId
                                        );

                        if (deletedRows <= 0) {
                            throw new IllegalStateException(
                                    "Curriculum profile was not found."
                            );
                        }

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

    @NonNull
    private SchoolCurriculumProfileEntity
    createBasicCurriculumProfile(
            @NonNull StudentProfileEntity studentProfile
    ) {
        long currentTime =
                System.currentTimeMillis();

        long profileId =
                studentProfile.getProfileId();

        String educationBoard =
                valueOrFallback(
                        studentProfile.getEducationBoard(),
                        "School Board"
                );

        SchoolCurriculumProfileEntity curriculumProfile =
                new SchoolCurriculumProfileEntity();

        curriculumProfile.setProfileId(
                profileId
        );

        curriculumProfile.setCurriculumId(
                createCurriculumId(
                        profileId
                )
        );

        curriculumProfile.setSchoolName(
                DEFAULT_SCHOOL_NAME
        );

        curriculumProfile.setSchoolCode(
                ""
        );

        curriculumProfile.setEducationBoard(
                educationBoard
        );

        curriculumProfile.setSchoolPattern(
                educationBoard
        );

        curriculumProfile.applyClassDefaults(
                extractClassNumber(
                        studentProfile.getStudentClass()
                )
        );

        curriculumProfile.setSection(
                ""
        );

        curriculumProfile.setAcademicSession(
                createCurrentAcademicSession()
        );

        curriculumProfile.setStudyMedium(
                valueOrFallback(
                        studentProfile.getStudyMedium(),
                        "English"
                )
        );

        curriculumProfile.setAiTutorEnabled(
                true
        );

        curriculumProfile.setAiDefaultLanguage(
                DEFAULT_AI_LANGUAGE
        );

        curriculumProfile.setAiDefaultAnswerMode(
                DEFAULT_AI_ANSWER_MODE
        );

        curriculumProfile.setVoiceQuestionEnabled(
                true
        );

        curriculumProfile.setImageQuestionEnabled(
                true
        );

        curriculumProfile.setReadAnswerAloudEnabled(
                true
        );

        curriculumProfile.setChildSafeAnswersEnabled(
                true
        );

        curriculumProfile.setSaveDoubtHistoryEnabled(
                true
        );

        /*
         * School name, section और complete curriculum
         * अभी parent से confirm नहीं हुए हैं।
         */
        curriculumProfile.setConfigured(
                false
        );

        curriculumProfile.setCreatedAt(
                currentTime
        );

        curriculumProfile.setUpdatedAt(
                currentTime
        );

        return curriculumProfile;
    }

    @NonNull
    private String createCurriculumId(
            long profileId
    ) {
        return "student_"
                + profileId
                + "_curriculum";
    }

    private int extractClassNumber(
            @Nullable String studentClass
    ) {
        String normalizedClass =
                safeText(
                        studentClass
                )
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replace(
                                "class",
                                ""
                        )
                        .replace(
                                "grade",
                                ""
                        )
                        .replaceAll(
                                "[^0-9]",
                                ""
                        );

        if (!normalizedClass.isEmpty()) {
            try {
                int classNumber =
                        Integer.parseInt(
                                normalizedClass
                        );

                if (classNumber >= 1
                        && classNumber <= 12) {

                    return classNumber;
                }

            } catch (NumberFormatException ignored) {
                /*
                 * Default Class 6 नीचे लौटेगी।
                 */
            }
        }

        return 6;
    }

    @NonNull
    private String createCurrentAcademicSession() {
        Calendar calendar =
                Calendar.getInstance();

        int currentYear =
                calendar.get(
                        Calendar.YEAR
                );

        int currentMonth =
                calendar.get(
                        Calendar.MONTH
                );

        /*
         * Indian school session सामान्यतः April से
         * अगले वर्ष March तक माना गया है।
         */
        int sessionStartYear;

        if (currentMonth
                >= Calendar.APRIL) {

            sessionStartYear =
                    currentYear;

        } else {
            sessionStartYear =
                    currentYear - 1;
        }

        return sessionStartYear
                + "-"
                + String.valueOf(
                sessionStartYear + 1
        ).substring(
                2
        );
    }

    private void validateStudentProfile(
            @NonNull StudentProfileEntity studentProfile
    ) {
        validateProfileId(
                studentProfile.getProfileId()
        );
    }

    private void validateCurriculumProfile(
            @NonNull SchoolCurriculumProfileEntity profile
    ) {
        validateProfileId(
                profile.getProfileId()
        );

        if (safeText(
                profile.getCurriculumId()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Curriculum ID is required."
            );
        }

        if (safeText(
                profile.getEducationBoard()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Education board is required."
            );
        }

        if (profile.getClassNumber() < 1
                || profile.getClassNumber() > 12) {

            throw new IllegalArgumentException(
                    "Class number must be between 1 and 12."
            );
        }
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

    private void requireUpdatedRow(
            int updatedRows
    ) {
        if (updatedRows <= 0) {
            throw new IllegalStateException(
                    "The curriculum profile was not found."
            );
        }
    }

    @NonNull
    private String valueOrFallback(
            @Nullable String value,
            @NonNull String fallback
    ) {
        String safeValue =
                safeText(
                        value
                );

        return safeValue.isEmpty()
                ? fallback
                : safeValue;
    }

    @NonNull
    private String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
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

    public interface SingleProfileCallback
            extends ErrorCallback {

        void onSuccess(
                @Nullable SchoolCurriculumProfileEntity profile
        );
    }

    public interface ProfilesCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<SchoolCurriculumProfileEntity> profiles
        );
    }

    public interface EnsureProfileCallback
            extends ErrorCallback {

        void onReady(
                @NonNull SchoolCurriculumProfileEntity profile,
                boolean newlyCreated
        );
    }

    public interface SaveProfileCallback
            extends ErrorCallback {

        void onSuccess(
                long savedProfileId
        );
    }

    public interface OperationCallback
            extends ErrorCallback {

        void onSuccess();
    }
}
