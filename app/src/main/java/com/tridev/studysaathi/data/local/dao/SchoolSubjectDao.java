package com.tridev.studysaathi.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;

import java.util.List;

@Dao
public interface SchoolSubjectDao {

    /*
     * नई subject entry insert करता है।
     *
     * एक curriculum profile में समान subject_id
     * पहले से होने पर insert fail होगा।
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    );

    /*
     * कई subjects को एक साथ insert करता है।
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertSubjects(
            @NonNull List<SchoolSubjectEntity> schoolSubjects
    );

    /*
     * Existing subject की पूरी जानकारी update करता है।
     */
    @Update
    int updateSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    );

    /*
     * Subject को permanently delete करता है।
     *
     * Subject delete होने पर उससे जुड़ी school_books
     * ForeignKey CASCADE के कारण delete हो जाएँगी।
     */
    @Delete
    int deleteSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    );

    @Nullable
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "LIMIT 1"
    )
    SchoolSubjectEntity getSubjectByRowId(
            long subjectRowId
    );

    @Nullable
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND subject_id = :subjectId "
                    + "LIMIT 1"
    )
    SchoolSubjectEntity getSubjectBySubjectId(
            long profileId,
            @NonNull String subjectId
    );

    /*
     * Curriculum profile के सभी subjects देता है।
     *
     * Enabled subjects पहले और फिर sort order के अनुसार।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "ORDER BY is_enabled DESC, "
                    + "sort_order ASC, "
                    + "subject_name_english COLLATE NOCASE ASC"
    )
    List<SchoolSubjectEntity> getSubjectsForProfile(
            long profileId
    );

    /*
     * केवल enabled subjects देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND is_enabled = 1 "
                    + "ORDER BY sort_order ASC, "
                    + "subject_name_english COLLATE NOCASE ASC"
    )
    List<SchoolSubjectEntity> getEnabledSubjectsForProfile(
            long profileId
    );

    /*
     * केवल official core subjects देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND is_official_core_subject = 1 "
                    + "ORDER BY sort_order ASC, "
                    + "subject_name_english COLLATE NOCASE ASC"
    )
    List<SchoolSubjectEntity> getOfficialCoreSubjects(
            long profileId
    );

    /*
     * School-specific subjects देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND subject_category = 'SCHOOL_SPECIFIC' "
                    + "ORDER BY sort_order ASC, "
                    + "subject_name_english COLLATE NOCASE ASC"
    )
    List<SchoolSubjectEntity> getSchoolSpecificSubjects(
            long profileId
    );

    /*
     * English या Hindi subject name से subject खोजता है।
     *
     * Book scan result को सही curriculum subject से
     * जोड़ने में इसका उपयोग होगा।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND ("
                    + "LOWER(TRIM(subject_name_english)) "
                    + "= LOWER(TRIM(:subjectName)) "
                    + "OR LOWER(TRIM(subject_name_hindi)) "
                    + "= LOWER(TRIM(:subjectName))"
                    + ") "
                    + "LIMIT 1"
    )
    SchoolSubjectEntity findSubjectByName(
            long profileId,
            @NonNull String subjectName
    );

    /*
     * केवल enabled subject में name match खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND is_enabled = 1 "
                    + "AND ("
                    + "LOWER(TRIM(subject_name_english)) "
                    + "= LOWER(TRIM(:subjectName)) "
                    + "OR LOWER(TRIM(subject_name_hindi)) "
                    + "= LOWER(TRIM(:subjectName))"
                    + ") "
                    + "LIMIT 1"
    )
    SchoolSubjectEntity findEnabledSubjectByName(
            long profileId,
            @NonNull String subjectName
    );

    /*
     * Subject code से subject खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND subject_code != '' "
                    + "AND LOWER(TRIM(subject_code)) "
                    + "= LOWER(TRIM(:subjectCode)) "
                    + "LIMIT 1"
    )
    SchoolSubjectEntity findSubjectByCode(
            long profileId,
            @NonNull String subjectCode
    );

    /*
     * Book name से संबंधित subject खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND book_name != '' "
                    + "AND LOWER(TRIM(book_name)) "
                    + "= LOWER(TRIM(:bookName)) "
                    + "LIMIT 1"
    )
    SchoolSubjectEntity findSubjectByBookName(
            long profileId,
            @NonNull String bookName
    );

    @Query(
            "SELECT COUNT(*) FROM school_subjects "
                    + "WHERE profile_id = :profileId"
    )
    int getSubjectCountForProfile(
            long profileId
    );

    @Query(
            "SELECT COUNT(*) FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND is_enabled = 1"
    )
    int getEnabledSubjectCountForProfile(
            long profileId
    );

    @Query(
            "SELECT COALESCE(MAX(sort_order), 0) "
                    + "FROM school_subjects "
                    + "WHERE profile_id = :profileId"
    )
    int getMaximumSortOrder(
            long profileId
    );

    /*
     * Subject को enable या disable करता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET is_enabled = :enabled, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int setSubjectEnabled(
            long subjectRowId,
            boolean enabled,
            long updatedAt
    );

    /*
     * Subject-level AI Tutor setting बदलता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET ai_tutor_enabled = :aiTutorEnabled, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int setAiTutorEnabled(
            long subjectRowId,
            boolean aiTutorEnabled,
            long updatedAt
    );

    /*
     * Parent को content editing की अनुमति बदलता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET allow_parent_content_editing = :allowed, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int setParentContentEditingAllowed(
            long subjectRowId,
            boolean allowed,
            long updatedAt
    );

    /*
     * Scan या manual entry के बाद subject की मुख्य
     * book summary information update करता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET book_name = :bookName, "
                    + "book_code = :bookCode, "
                    + "publisher_name = :publisherName, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int updateSubjectBookInformation(
            long subjectRowId,
            @NonNull String bookName,
            @NonNull String bookCode,
            @NonNull String publisherName,
            long updatedAt
    );

    /*
     * Subject का content source और installed pack update करता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET content_source = :contentSource, "
                    + "content_pack_id = :contentPackId, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int updateContentSource(
            long subjectRowId,
            @NonNull String contentSource,
            @NonNull String contentPackId,
            long updatedAt
    );

    /*
     * Subject की category update करता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET subject_category = :subjectCategory, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int updateSubjectCategory(
            long subjectRowId,
            @NonNull String subjectCategory,
            long updatedAt
    );

    /*
     * Subject की list position update करता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET sort_order = :sortOrder, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int updateSubjectSortOrder(
            long subjectRowId,
            int sortOrder,
            long updatedAt
    );

    /*
     * Generated curriculum content की संख्या update करता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET chapter_count = :chapterCount, "
                    + "lesson_count = :lessonCount, "
                    + "quiz_question_count = :quizQuestionCount, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int updateContentCounts(
            long subjectRowId,
            int chapterCount,
            int lessonCount,
            int quizQuestionCount,
            long updatedAt
    );

    /*
     * Subject के नाम update करता है।
     */
    @Query(
            "UPDATE school_subjects "
                    + "SET subject_name_english = :englishName, "
                    + "subject_name_hindi = :hindiName, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int updateSubjectNames(
            long subjectRowId,
            @NonNull String englishName,
            @NonNull String hindiName,
            long updatedAt
    );

    /*
     * Subject ID या subject code के आधार पर duplicate
     * entry जाँचने के लिए उपयोग होगा।
     */
    @Query(
            "SELECT COUNT(*) FROM school_subjects "
                    + "WHERE profile_id = :profileId "
                    + "AND ("
                    + "subject_id = :subjectId "
                    + "OR (subject_code != '' "
                    + "AND LOWER(TRIM(subject_code)) "
                    + "= LOWER(TRIM(:subjectCode)))"
                    + ")"
    )
    int countMatchingSubjectIdentifiers(
            long profileId,
            @NonNull String subjectId,
            @NonNull String subjectCode
    );

    /*
     * किसी profile के सभी subjects permanently delete करता है।
     *
     * यह profile cleanup या reset operation में उपयोग होगा।
     */
    @Query(
            "DELETE FROM school_subjects "
                    + "WHERE profile_id = :profileId"
    )
    int deleteSubjectsForProfile(
            long profileId
    );

    /*
     * किसी subject को row ID के आधार पर permanently delete करता है।
     */
    @Query(
            "DELETE FROM school_subjects "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int deleteSubjectByRowId(
            long subjectRowId
    );
}