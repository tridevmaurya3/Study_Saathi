package com.tridev.studysaathi.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.tridev.studysaathi.data.local.entity.SchoolCurriculumProfileEntity;

import java.util.List;

@Dao
public interface SchoolCurriculumProfileDao {

    /*
     * Student के लिए नया school curriculum profile insert करता है।
     *
     * profile_id StudentProfileEntity के profile_id के समान होगा।
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertCurriculumProfile(
            @NonNull SchoolCurriculumProfileEntity curriculumProfile
    );

    /*
     * Multiple curriculum profiles को एक transaction में insert करता है।
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertCurriculumProfiles(
            @NonNull List<SchoolCurriculumProfileEntity> curriculumProfiles
    );

    /*
     * Existing school curriculum profile की पूरी information update करता है।
     */
    @Update
    int updateCurriculumProfile(
            @NonNull SchoolCurriculumProfileEntity curriculumProfile
    );

    /*
     * Curriculum profile permanently delete करता है।
     *
     * Profile delete होने पर उससे जुड़े school_subjects और
     * school_books आगे ForeignKey CASCADE से delete होंगे।
     */
    @Delete
    int deleteCurriculumProfile(
            @NonNull SchoolCurriculumProfileEntity curriculumProfile
    );

    /*
     * Student profile ID से school curriculum profile प्राप्त करता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "WHERE profile_id = :profileId "
                    + "LIMIT 1"
    )
    SchoolCurriculumProfileEntity getCurriculumProfileByProfileId(
            long profileId
    );

    /*
     * Unique curriculum ID से profile खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "WHERE curriculum_id = :curriculumId "
                    + "LIMIT 1"
    )
    SchoolCurriculumProfileEntity getCurriculumProfileByCurriculumId(
            @NonNull String curriculumId
    );

    /*
     * केवल पूरी तरह configured curriculum profile देता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "WHERE profile_id = :profileId "
                    + "AND is_configured = 1 "
                    + "LIMIT 1"
    )
    SchoolCurriculumProfileEntity getConfiguredProfile(
            long profileId
    );

    /*
     * सभी curriculum profiles को latest update के अनुसार देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "ORDER BY updated_at DESC, "
                    + "school_name COLLATE NOCASE ASC"
    )
    List<SchoolCurriculumProfileEntity> getAllCurriculumProfiles();

    /*
     * केवल configured profiles देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "WHERE is_configured = 1 "
                    + "ORDER BY updated_at DESC, "
                    + "school_name COLLATE NOCASE ASC"
    )
    List<SchoolCurriculumProfileEntity> getAllConfiguredProfiles();

    /*
     * Board और class के आधार पर profiles खोजता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "WHERE LOWER(TRIM(education_board)) "
                    + "= LOWER(TRIM(:educationBoard)) "
                    + "AND class_number = :classNumber "
                    + "ORDER BY school_name COLLATE NOCASE ASC"
    )
    List<SchoolCurriculumProfileEntity> getProfilesByBoardAndClass(
            @NonNull String educationBoard,
            int classNumber
    );

    /*
     * School, board और class के exact combination से profile खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "WHERE LOWER(TRIM(school_name)) "
                    + "= LOWER(TRIM(:schoolName)) "
                    + "AND LOWER(TRIM(education_board)) "
                    + "= LOWER(TRIM(:educationBoard)) "
                    + "AND class_number = :classNumber "
                    + "LIMIT 1"
    )
    SchoolCurriculumProfileEntity findProfileBySchoolBoardAndClass(
            @NonNull String schoolName,
            @NonNull String educationBoard,
            int classNumber
    );

    /*
     * School code से curriculum profile खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "WHERE school_code != '' "
                    + "AND LOWER(TRIM(school_code)) "
                    + "= LOWER(TRIM(:schoolCode)) "
                    + "LIMIT 1"
    )
    SchoolCurriculumProfileEntity findProfileBySchoolCode(
            @NonNull String schoolCode
    );

    /*
     * Academic session के आधार पर profiles देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_curriculum_profiles "
                    + "WHERE LOWER(TRIM(academic_session)) "
                    + "= LOWER(TRIM(:academicSession)) "
                    + "ORDER BY school_name COLLATE NOCASE ASC"
    )
    List<SchoolCurriculumProfileEntity> getProfilesByAcademicSession(
            @NonNull String academicSession
    );

    @Query(
            "SELECT COUNT(*) FROM school_curriculum_profiles"
    )
    int getCurriculumProfileCount();

    @Query(
            "SELECT COUNT(*) FROM school_curriculum_profiles "
                    + "WHERE is_configured = 1"
    )
    int getConfiguredProfileCount();

    @Query(
            "SELECT COUNT(*) FROM school_curriculum_profiles "
                    + "WHERE profile_id = :profileId"
    )
    int countProfileByStudentId(
            long profileId
    );

    @Query(
            "SELECT COUNT(*) FROM school_curriculum_profiles "
                    + "WHERE curriculum_id = :curriculumId"
    )
    int countProfileByCurriculumId(
            @NonNull String curriculumId
    );

    /*
     * School की मुख्य identity information update करता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET school_name = :schoolName, "
                    + "school_code = :schoolCode, "
                    + "education_board = :educationBoard, "
                    + "school_pattern = :schoolPattern, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int updateSchoolInformation(
            long profileId,
            @NonNull String schoolName,
            @NonNull String schoolCode,
            @NonNull String educationBoard,
            @NonNull String schoolPattern,
            long updatedAt
    );

    /*
     * Student की class, section, session और medium update करता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET class_number = :classNumber, "
                    + "section = :section, "
                    + "academic_session = :academicSession, "
                    + "study_medium = :studyMedium, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int updateAcademicInformation(
            long profileId,
            int classNumber,
            @NonNull String section,
            @NonNull String academicSession,
            @NonNull String studyMedium,
            long updatedAt
    );

    /*
     * Curriculum profile को configured या incomplete mark करता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET is_configured = :configured, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int setProfileConfigured(
            long profileId,
            boolean configured,
            long updatedAt
    );

    /*
     * पूरे curriculum profile के AI Tutor को enable या disable करता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET ai_tutor_enabled = :enabled, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int setAiTutorEnabled(
            long profileId,
            boolean enabled,
            long updatedAt
    );

    /*
     * AI Tutor की default language और answer mode update करता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET ai_default_language = :defaultLanguage, "
                    + "ai_default_answer_mode = :defaultAnswerMode, "
                    + "preferred_maximum_answer_words = :maximumAnswerWords, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int updateAiTutorPreferences(
            long profileId,
            @NonNull String defaultLanguage,
            @NonNull String defaultAnswerMode,
            int maximumAnswerWords,
            long updatedAt
    );

    /*
     * Voice question, image question और read-aloud settings बदलता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET voice_question_enabled = :voiceQuestionEnabled, "
                    + "image_question_enabled = :imageQuestionEnabled, "
                    + "read_answer_aloud_enabled = :readAnswerAloudEnabled, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int updateAiInputAndSpeechSettings(
            long profileId,
            boolean voiceQuestionEnabled,
            boolean imageQuestionEnabled,
            boolean readAnswerAloudEnabled,
            long updatedAt
    );

    /*
     * Child safety और doubt-history settings update करता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET child_safe_answers_enabled = :childSafeAnswersEnabled, "
                    + "save_doubt_history_enabled = :saveDoubtHistoryEnabled, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int updateSafetyAndHistorySettings(
            long profileId,
            boolean childSafeAnswersEnabled,
            boolean saveDoubtHistoryEnabled,
            long updatedAt
    );

    /*
     * AI answer की preferred maximum word limit update करता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET preferred_maximum_answer_words = :maximumAnswerWords, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int updatePreferredMaximumAnswerWords(
            long profileId,
            int maximumAnswerWords,
            long updatedAt
    );

    /*
     * Curriculum ID update करता है।
     *
     * नया ID पूरे database में unique होना चाहिए।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET curriculum_id = :curriculumId, "
                    + "updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int updateCurriculumId(
            long profileId,
            @NonNull String curriculumId,
            long updatedAt
    );

    /*
     * केवल updated_at timestamp बदलता है।
     */
    @Query(
            "UPDATE school_curriculum_profiles "
                    + "SET updated_at = :updatedAt "
                    + "WHERE profile_id = :profileId"
    )
    int updateLastModifiedTime(
            long profileId,
            long updatedAt
    );

    /*
     * Existing row मिलने पर update और नहीं मिलने पर insert करता है।
     */
    @Transaction
    default long insertOrUpdateCurriculumProfile(
            @NonNull SchoolCurriculumProfileEntity curriculumProfile
    ) {
        SchoolCurriculumProfileEntity existingProfile =
                getCurriculumProfileByProfileId(
                        curriculumProfile.getProfileId()
                );

        if (existingProfile == null) {
            return insertCurriculumProfile(
                    curriculumProfile
            );
        }

        int updatedRows =
                updateCurriculumProfile(
                        curriculumProfile
                );

        return updatedRows > 0
                ? curriculumProfile.getProfileId()
                : -1L;
    }

    /*
     * Student profile ID के आधार पर curriculum profile delete करता है।
     */
    @Query(
            "DELETE FROM school_curriculum_profiles "
                    + "WHERE profile_id = :profileId"
    )
    int deleteCurriculumProfileByProfileId(
            long profileId
    );

    /*
     * Curriculum ID के आधार पर profile delete करता है।
     */
    @Query(
            "DELETE FROM school_curriculum_profiles "
                    + "WHERE curriculum_id = :curriculumId"
    )
    int deleteCurriculumProfileByCurriculumId(
            @NonNull String curriculumId
    );
}