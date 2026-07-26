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

import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterPageEntity;

import java.util.List;

@Dao
public interface SchoolBookChapterPageDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertPage(
            @NonNull SchoolBookChapterPageEntity page
    );

    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertPages(
            @NonNull List<SchoolBookChapterPageEntity> pages
    );

    @Update
    int updatePage(
            @NonNull SchoolBookChapterPageEntity page
    );

    @Delete
    int deletePage(
            @NonNull SchoolBookChapterPageEntity page
    );

    @Nullable
    @Query(
            "SELECT * "
                    + "FROM school_book_chapter_pages "
                    + "WHERE chapter_page_row_id = :pageRowId "
                    + "LIMIT 1"
    )
    SchoolBookChapterPageEntity getPageByRowId(
            long pageRowId
    );

    @Nullable
    @Query(
            "SELECT * "
                    + "FROM school_book_chapter_pages "
                    + "WHERE chapter_row_id = :chapterRowId "
                    + "AND page_order = :pageOrder "
                    + "LIMIT 1"
    )
    SchoolBookChapterPageEntity getPageByOrder(
            long chapterRowId,
            int pageOrder
    );

    @Nullable
    @Query(
            "SELECT * "
                    + "FROM school_book_chapter_pages "
                    + "WHERE chapter_row_id = :chapterRowId "
                    + "AND page_order = :pageOrder "
                    + "AND parent_approved = 1 "
                    + "LIMIT 1"
    )
    SchoolBookChapterPageEntity getApprovedPageByOrder(
            long chapterRowId,
            int pageOrder
    );

    @NonNull
    @Query(
            "SELECT * "
                    + "FROM school_book_chapter_pages "
                    + "WHERE chapter_row_id = :chapterRowId "
                    + "ORDER BY page_order ASC"
    )
    List<SchoolBookChapterPageEntity> getPagesForChapter(
            long chapterRowId
    );

    @NonNull
    @Query(
            "SELECT * "
                    + "FROM school_book_chapter_pages "
                    + "WHERE chapter_row_id = :chapterRowId "
                    + "AND parent_approved = 1 "
                    + "ORDER BY page_order ASC"
    )
    List<SchoolBookChapterPageEntity>
    getApprovedPagesForChapter(
            long chapterRowId
    );

    @Query(
            "SELECT COUNT(*) "
                    + "FROM school_book_chapter_pages "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int countPagesForChapter(
            long chapterRowId
    );

    @Query(
            "SELECT COUNT(*) "
                    + "FROM school_book_chapter_pages "
                    + "WHERE chapter_row_id = :chapterRowId "
                    + "AND parent_approved = 1"
    )
    int countApprovedPagesForChapter(
            long chapterRowId
    );

    @Query(
            "SELECT COALESCE(MAX(page_order), 0) "
                    + "FROM school_book_chapter_pages "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int getMaximumPageOrder(
            long chapterRowId
    );

    @Query(
            "UPDATE school_book_chapter_pages "
                    + "SET parent_approved = :approved, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_page_row_id = :pageRowId"
    )
    int updateParentApproval(
            long pageRowId,
            boolean approved,
            long updatedAt
    );

    @Query(
            "UPDATE school_book_chapter_pages "
                    + "SET parent_approved = :approved, "
                    + "updated_at = :updatedAt "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int updateAllPageApprovalsForChapter(
            long chapterRowId,
            boolean approved,
            long updatedAt
    );

    @Query(
            "DELETE FROM school_book_chapter_pages "
                    + "WHERE chapter_row_id = :chapterRowId"
    )
    int deletePagesForChapter(
            long chapterRowId
    );

    /**
     * केवल explicit Parent-confirmed replacement workflow में उपयोग करें।
     * Insert fail होने पर Room transaction delete को rollback कर देगा।
     */
    @Transaction
    default List<Long> replacePagesForChapter(
            long chapterRowId,
            @NonNull List<SchoolBookChapterPageEntity> pages
    ) {
        deletePagesForChapter(chapterRowId);
        return insertPages(pages);
    }
}
