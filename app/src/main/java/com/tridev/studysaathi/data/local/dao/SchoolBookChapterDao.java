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

import com.tridev.studysaathi.data.local.entity.SchoolBookChapterEntity;

import java.util.Collections;
import java.util.List;

@Dao
public interface SchoolBookChapterDao {

    /*
     * नई chapter entry insert करता है।
     *
     * एक ही book में समान chapter_id पहले से मौजूद होने पर
     * insert ABORT होगा और existing chapter replace नहीं होगा।
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertChapter(
            @NonNull SchoolBookChapterEntity chapter
    );

    /*
     * Contents scan, authorized import या manual chapter list से
     * कई chapters को एक transaction में insert करता है।
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertChapters(
            @NonNull List<SchoolBookChapterEntity> chapters
    );

    /*
     * Existing chapter के सभी fields update करता है।
     */
    @Update
    int updateChapter(
            @NonNull SchoolBookChapterEntity chapter
    );

    /*
     * Chapter को permanently delete करता है।
     */
    @Delete
    int deleteChapter(
            @NonNull SchoolBookChapterEntity chapter
    );

    /*
     * Primary database row ID से exact chapter देता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE chapter_row_id = :chapterRowId "
                    + "LIMIT 1"
    )
    SchoolBookChapterEntity getChapterByRowId(
            long chapterRowId
    );

    /*
     * किसी exact school book के अंदर chapter_id से chapter खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND chapter_id = :chapterId "
                    + "LIMIT 1"
    )
    SchoolBookChapterEntity getChapterByChapterId(
            long bookRowId,
            @NonNull String chapterId
    );

    /*
     * Exact chapter number से chapter खोजता है।
     *
     * Chapter number blank होने पर result नहीं दिया जाएगा।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND chapter_number != '' "
                    + "AND LOWER(TRIM(chapter_number)) "
                    + "= LOWER(TRIM(:chapterNumber)) "
                    + "LIMIT 1"
    )
    SchoolBookChapterEntity findChapterByNumber(
            long bookRowId,
            @NonNull String chapterNumber
    );

    /*
     * English या Hindi title से exact chapter खोजता है।
     *
     * Contents scan के बाद duplicate chapters रोकने में इसका
     * उपयोग किया जाएगा।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND ("
                    + "("
                    + "chapter_title_english != '' "
                    + "AND LOWER(TRIM(chapter_title_english)) "
                    + "= LOWER(TRIM(:chapterTitle))"
                    + ") "
                    + "OR ("
                    + "chapter_title_hindi != '' "
                    + "AND LOWER(TRIM(chapter_title_hindi)) "
                    + "= LOWER(TRIM(:chapterTitle))"
                    + ")"
                    + ") "
                    + "LIMIT 1"
    )
    SchoolBookChapterEntity findChapterByTitle(
            long bookRowId,
            @NonNull String chapterTitle
    );

    /*
     * किसी exact school book के सभी chapters देता है।
     *
     * Parent-confirmed या enabled status की परवाह किए बिना
     * पूरी chapter list लौटाई जाएगी।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "ORDER BY sort_order ASC, "
                    + "CASE "
                    + "WHEN start_page_number > 0 "
                    + "THEN start_page_number "
                    + "ELSE 2147483647 "
                    + "END ASC, "
                    + "chapter_title_english COLLATE NOCASE ASC, "
                    + "chapter_title_hindi COLLATE NOCASE ASC"
    )
    List<SchoolBookChapterEntity> getChaptersForBook(
            long bookRowId
    );

    /*
     * केवल enabled chapters देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND is_enabled = 1 "
                    + "ORDER BY sort_order ASC, "
                    + "CASE "
                    + "WHEN start_page_number > 0 "
                    + "THEN start_page_number "
                    + "ELSE 2147483647 "
                    + "END ASC, "
                    + "chapter_title_english COLLATE NOCASE ASC, "
                    + "chapter_title_hindi COLLATE NOCASE ASC"
    )
    List<SchoolBookChapterEntity> getEnabledChaptersForBook(
            long bookRowId
    );

    /*
     * केवल Parent-confirmed chapters देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND parent_confirmed = 1 "
                    + "ORDER BY sort_order ASC, "
                    + "CASE "
                    + "WHEN start_page_number > 0 "
                    + "THEN start_page_number "
                    + "ELSE 2147483647 "
                    + "END ASC, "
                    + "chapter_title_english COLLATE NOCASE ASC, "
                    + "chapter_title_hindi COLLATE NOCASE ASC"
    )
    List<SchoolBookChapterEntity> getParentConfirmedChaptersForBook(
            long bookRowId
    );

    /*
     * Child Mode में केवल वही chapters दिखाई देंगे जो:
     *
     * 1. Exact book से जुड़े हों
     * 2. Enabled हों
     * 3. Parent द्वारा confirm किए गए हों
     * 4. Valid chapter_id रखते हों
     * 5. English या Hindi में valid title रखते हों
     */
    @NonNull
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND is_enabled = 1 "
                    + "AND parent_confirmed = 1 "
                    + "AND TRIM(chapter_id) != '' "
                    + "AND ("
                    + "TRIM(chapter_title_english) != '' "
                    + "OR TRIM(chapter_title_hindi) != ''"
                    + ") "
                    + "ORDER BY sort_order ASC, "
                    + "CASE "
                    + "WHEN start_page_number > 0 "
                    + "THEN start_page_number "
                    + "ELSE 2147483647 "
                    + "END ASC, "
                    + "chapter_title_english COLLATE NOCASE ASC, "
                    + "chapter_title_hindi COLLATE NOCASE ASC"
    )
    List<SchoolBookChapterEntity> getChildModeChaptersForBook(
            long bookRowId
    );

    /*
     * Contents/Index scan या AI extraction से मिले वे chapters
     * देता है जिनकी Parent review अभी बाकी है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND parent_confirmed = 0 "
                    + "AND ("
                    + "content_source = 'BOOK_TOC_SCAN' "
                    + "OR content_source = 'AI_EXTRACTED'"
                    + ") "
                    + "ORDER BY sort_order ASC, "
                    + "CASE "
                    + "WHEN start_page_number > 0 "
                    + "THEN start_page_number "
                    + "ELSE 2147483647 "
                    + "END ASC"
    )
    List<SchoolBookChapterEntity> getChaptersPendingParentReview(
            long bookRowId
    );

    /*
     * Processing status के अनुसार chapters देता है।
     *
     * उदाहरण:
     * PENDING_REVIEW
     * PROCESSING
     * COMPLETED
     * FAILED
     */
    @NonNull
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND content_processing_status = :processingStatus "
                    + "ORDER BY sort_order ASC, "
                    + "chapter_title_english COLLATE NOCASE ASC, "
                    + "chapter_title_hindi COLLATE NOCASE ASC"
    )
    List<SchoolBookChapterEntity> getChaptersByProcessingStatus(
            long bookRowId,
            @NonNull String processingStatus
    );

    /*
     * Chapter type के अनुसार chapters देता है।
     *
     * उदाहरण:
     * CHAPTER
     * UNIT
     * LESSON
     * POEM
     * STORY
     */
    @NonNull
    @Query(
            "SELECT * FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND chapter_type = :chapterType "
                    + "ORDER BY sort_order ASC, "
                    + "chapter_title_english COLLATE NOCASE ASC, "
                    + "chapter_title_hindi COLLATE NOCASE ASC"
    )
    List<SchoolBookChapterEntity> getChaptersByType(
            long bookRowId,
            @NonNull String chapterType
    );

    /*
     * किसी book में कुल chapters की संख्या।
     */
    @Query(
            "SELECT COUNT(*) FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId"
    )
    int getChapterCountForBook(
            long bookRowId
    );

    /*
     * Enabled chapters की संख्या।
     */
    @Query(
            "SELECT COUNT(*) FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND is_enabled = 1"
    )
    int getEnabledChapterCountForBook(
            long bookRowId
    );

    /*
     * Child Mode में दिखाई देने योग्य exact chapters की संख्या।
     */
    @Query(
            "SELECT COUNT(*) FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND is_enabled = 1 "
                    + "AND parent_confirmed = 1 "
                    + "AND TRIM(chapter_id) != '' "
                    + "AND ("
                    + "TRIM(chapter_title_english) != '' "
                    + "OR TRIM(chapter_title_hindi) != ''"
                    + ")"
    )
    int getChildModeChapterCountForBook(
            long bookRowId
    );

    /*
     * Parent review pending chapters की संख्या।
     */
    @Query(
            "SELECT COUNT(*) FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND parent_confirmed = 0 "
                    + "AND ("
                    + "content_source = 'BOOK_TOC_SCAN' "
                    + "OR content_source = 'AI_EXTRACTED'"
                    + ")"
    )
    int getPendingReviewChapterCountForBook(
            long bookRowId
    );

    /*
     * Book में सबसे बड़ा current sort order देता है।
     */
    @Query(
            "SELECT COALESCE(MAX(sort_order), 0) "
                    + "FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId"
    )
    int getMaximumSortOrder(
            long bookRowId
    );

    /*
     * नए chapter के लिए अगला sort order देता है।
     */
    default int getNextSortOrder(
            long bookRowId
    ) {
        int maximumSortOrder =
                getMaximumSortOrder(
                        bookRowId
                );

        if (maximumSortOrder == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return maximumSortOrder + 1;
    }

    /*
     * Chapter को enabled या disabled करता है।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET is_enabled = :enabled, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int setChapterEnabled(
            long chapterRowId,
            boolean enabled,
            long updatedAt
    );

    /*
     * Parent confirmation बदलता है।
     *
     * Pending review chapter confirm होने पर processing status
     * CONFIRMED हो जाएगा।
     *
     * Confirmed chapter को unconfirm करने पर status वापस
     * PENDING_REVIEW होगा।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET parent_confirmed = :confirmed, "
                    + "content_processing_status = CASE "
                    + "WHEN :confirmed = 1 "
                    + "AND content_processing_status = 'PENDING_REVIEW' "
                    + "THEN 'CONFIRMED' "
                    + "WHEN :confirmed = 0 "
                    + "AND content_processing_status = 'CONFIRMED' "
                    + "THEN 'PENDING_REVIEW' "
                    + "ELSE content_processing_status "
                    + "END, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int setParentConfirmed(
            long chapterRowId,
            boolean confirmed,
            long updatedAt
    );

    /*
     * पूरी book के pending chapters को Parent-confirmed बनाता है।
     *
     * Confirm All Chapters button में इसका उपयोग होगा।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET parent_confirmed = 1, "
                    + "content_processing_status = CASE "
                    + "WHEN content_processing_status = 'PENDING_REVIEW' "
                    + "THEN 'CONFIRMED' "
                    + "ELSE content_processing_status "
                    + "END, "
                    + "updated_at = :updatedAt "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND parent_confirmed = 0"
    )
    int confirmAllChaptersForBook(
            long bookRowId,
            long updatedAt
    );

    /*
     * Chapter का processing status update करता है।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET content_processing_status = :processingStatus, "
                    + "last_content_processed_at = :processedAt, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateContentProcessingStatus(
            long chapterRowId,
            @NonNull String processingStatus,
            long processedAt,
            long updatedAt
    );

    /*
     * Chapter का sort order बदलता है।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET sort_order = :sortOrder, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateChapterSortOrder(
            long chapterRowId,
            int sortOrder,
            long updatedAt
    );

    /*
     * Contents scan review screen से chapter title और number
     * ठीक किए जाने पर मुख्य पहचान fields update करता है।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET chapter_number = :chapterNumber, "
                    + "chapter_title_english = :chapterTitleEnglish, "
                    + "chapter_title_hindi = :chapterTitleHindi, "
                    + "chapter_subtitle = :chapterSubtitle, "
                    + "unit_name = :unitName, "
                    + "chapter_type = :chapterType, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateChapterIdentityInformation(
            long chapterRowId,
            @NonNull String chapterNumber,
            @NonNull String chapterTitleEnglish,
            @NonNull String chapterTitleHindi,
            @NonNull String chapterSubtitle,
            @NonNull String unitName,
            @NonNull String chapterType,
            long updatedAt
    );

    /*
     * Chapter का page range update करता है।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET start_page_number = :startPageNumber, "
                    + "end_page_number = :endPageNumber, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateChapterPageRange(
            long chapterRowId,
            int startPageNumber,
            int endPageNumber,
            long updatedAt
    );

    /*
     * Chapter के learning-content summary fields update करता है।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET chapter_description = :chapterDescription, "
                    + "learning_objectives = :learningObjectives, "
                    + "important_topics = :importantTopics, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateChapterLearningInformation(
            long chapterRowId,
            @NonNull String chapterDescription,
            @NonNull String learningObjectives,
            @NonNull String importantTopics,
            long updatedAt
    );

    /*
     * Chapter के lesson, quiz, note, bookmark और progress summary
     * fields एक साथ update करता है।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET lesson_count = :lessonCount, "
                    + "completed_lesson_count = :completedLessonCount, "
                    + "quiz_question_count = :quizQuestionCount, "
                    + "note_count = :noteCount, "
                    + "bookmark_count = :bookmarkCount, "
                    + "progress_percent = :progressPercent, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateChapterProgressSummary(
            long chapterRowId,
            int lessonCount,
            int completedLessonCount,
            int quizQuestionCount,
            int noteCount,
            int bookmarkCount,
            int progressPercent,
            long updatedAt
    );

    /*
     * Child द्वारा chapter खोले जाने का latest timestamp save करता है।
     */
    @Query(
            "UPDATE school_book_chapters "
                    + "SET last_opened_at = :openedAt, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateLastOpenedTime(
            long chapterRowId,
            long openedAt,
            long updatedAt
    );

    /*
     * Row ID के आधार पर chapter permanently delete करता है।
     */
    @Query(
            "DELETE FROM school_book_chapters "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int deleteChapterByRowId(
            long chapterRowId
    );

    /*
     * किसी exact school book के सभी chapters permanently delete करता है।
     *
     * इसका उपयोग केवल initial TOC re-import, book cleanup या testing
     * में किया जाना चाहिए।
     */
    @Query(
            "DELETE FROM school_book_chapters "
                    + "WHERE book_row_id = :bookRowId"
    )
    int deleteChaptersForBook(
            long bookRowId
    );

    /*
     * Existing chapter list हटाकर नई chapter list insert करता है।
     *
     * दोनों operations एक Room transaction में होंगे।
     *
     * यह method केवल तब उपयोग करें जब Parent ने पूरी replacement
     * chapter list confirm कर दी हो।
     */
    @Transaction
    default List<Long> replaceChaptersForBook(
            long bookRowId,
            @NonNull List<SchoolBookChapterEntity> chapters
    ) {
        deleteChaptersForBook(
                bookRowId
        );

        if (chapters.isEmpty()) {
            return Collections.emptyList();
        }

        return insertChapters(
                chapters
        );
    }
}