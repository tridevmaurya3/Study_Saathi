package com.tridev.studysaathi.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterContentEntity;

import java.util.List;

@Dao
public interface SchoolBookChapterContentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertContent(
            @NonNull SchoolBookChapterContentEntity content
    );

    @Update
    int updateContent(
            @NonNull SchoolBookChapterContentEntity content
    );

    @Delete
    int deleteContent(
            @NonNull SchoolBookChapterContentEntity content
    );

    @Nullable
    @Query(
            "SELECT * FROM school_book_chapter_contents "
                    + "WHERE content_row_id = :contentRowId "
                    + "LIMIT 1"
    )
    SchoolBookChapterContentEntity getContentByRowId(
            long contentRowId
    );

    @Nullable
    @Query(
            "SELECT * FROM school_book_chapter_contents "
                    + "WHERE chapter_row_id = :chapterRowId "
                    + "LIMIT 1"
    )
    SchoolBookChapterContentEntity getContentForChapter(
            long chapterRowId
    );

    @Nullable
    @Query(
            "SELECT * FROM school_book_chapter_contents "
                    + "WHERE chapter_row_id = :chapterRowId "
                    + "AND parent_approved = 1 "
                    + "AND review_status = 'APPROVED' "
                    + "LIMIT 1"
    )
    SchoolBookChapterContentEntity
    getApprovedContentForChapter(
            long chapterRowId
    );

    @NonNull
    @Query(
            "SELECT school_book_chapter_contents.* "
                    + "FROM school_book_chapter_contents "
                    + "INNER JOIN school_book_chapters "
                    + "ON school_book_chapters.chapter_row_id "
                    + "= school_book_chapter_contents.chapter_row_id "
                    + "WHERE school_book_chapters.book_row_id "
                    + "= :bookRowId "
                    + "ORDER BY school_book_chapters.sort_order ASC, "
                    + "school_book_chapters.chapter_row_id ASC"
    )
    List<SchoolBookChapterContentEntity>
    getContentsForBook(
            long bookRowId
    );

    @NonNull
    @Query(
            "SELECT school_book_chapter_contents.* "
                    + "FROM school_book_chapter_contents "
                    + "INNER JOIN school_book_chapters "
                    + "ON school_book_chapters.chapter_row_id "
                    + "= school_book_chapter_contents.chapter_row_id "
                    + "WHERE school_book_chapters.book_row_id "
                    + "= :bookRowId "
                    + "AND school_book_chapter_contents.parent_approved = 1 "
                    + "AND school_book_chapter_contents.review_status "
                    + "= 'APPROVED' "
                    + "ORDER BY school_book_chapters.sort_order ASC, "
                    + "school_book_chapters.chapter_row_id ASC"
    )
    List<SchoolBookChapterContentEntity>
    getApprovedContentsForBook(
            long bookRowId
    );

    @NonNull
    @Query(
            "SELECT * FROM school_book_chapter_contents "
                    + "WHERE review_status = :reviewStatus "
                    + "ORDER BY updated_at DESC"
    )
    List<SchoolBookChapterContentEntity>
    getContentsByReviewStatus(
            @NonNull String reviewStatus
    );

    @Query(
            "UPDATE school_book_chapter_contents "
                    + "SET review_status = :reviewStatus, "
                    + "parent_approved = :parentApproved, "
                    + "approved_at = :approvedAt, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateReviewState(
            long chapterRowId,
            @NonNull String reviewStatus,
            boolean parentApproved,
            long approvedAt,
            long updatedAt
    );

    @Query(
            "UPDATE school_book_chapter_contents "
                    + "SET review_status = 'PROCESSING', "
                    + "parent_approved = 0, "
                    + "approved_at = 0, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int markProcessing(
            long chapterRowId,
            long updatedAt
    );

    @Query(
            "UPDATE school_book_chapter_contents "
                    + "SET review_status = 'FAILED', "
                    + "parent_approved = 0, "
                    + "approved_at = 0, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int markFailed(
            long chapterRowId,
            long updatedAt
    );

    @Query(
            "SELECT COUNT(*) FROM school_book_chapter_contents "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int countContentForChapter(
            long chapterRowId
    );

    @Query(
            "SELECT COUNT(*) FROM school_book_chapter_contents "
                    + "WHERE review_status = 'PENDING_REVIEW'"
    )
    int countPendingReviewContents();

    @Query(
            "DELETE FROM school_book_chapter_contents "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int deleteContentForChapter(
            long chapterRowId
    );
}