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

import com.tridev.studysaathi.data.local.entity.SchoolBookEntity;

import java.util.List;

@Dao
public interface SchoolBookDao {

    /*
     * नई school book insert करता है।
     *
     * Unique subject_row_id + book_id conflict होने पर
     * existing book अपने-आप replace नहीं होगी।
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertBook(
            @NonNull SchoolBookEntity schoolBook
    );

    /*
     * Multiple books को एक transaction में insert करता है।
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertBooks(
            @NonNull List<SchoolBookEntity> schoolBooks
    );

    /*
     * Existing book के सभी fields update करता है।
     */
    @Update
    int updateBook(
            @NonNull SchoolBookEntity schoolBook
    );

    /*
     * Book को database से permanently delete करता है।
     *
     * सामान्य app flow में permanent delete की जगह
     * setBookActive(false) उपयोग करना बेहतर होगा।
     */
    @Delete
    int deleteBook(
            @NonNull SchoolBookEntity schoolBook
    );

    @Nullable
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE book_row_id = :bookRowId "
                    + "LIMIT 1"
    )
    SchoolBookEntity getBookByRowId(
            long bookRowId
    );

    @Nullable
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "AND book_id = :bookId "
                    + "LIMIT 1"
    )
    SchoolBookEntity getBookByBookId(
            long subjectRowId,
            @NonNull String bookId
    );

    /*
     * किसी subject की सभी books देता है।
     *
     * Primary book पहले, फिर sort order और title के अनुसार।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "ORDER BY is_primary_book DESC, "
                    + "sort_order ASC, "
                    + "book_title COLLATE NOCASE ASC"
    )
    List<SchoolBookEntity> getBooksForSubject(
            long subjectRowId
    );

    /*
     * केवल active books देता है।
     */
    @NonNull
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "AND is_active = 1 "
                    + "ORDER BY is_primary_book DESC, "
                    + "sort_order ASC, "
                    + "book_title COLLATE NOCASE ASC"
    )
    List<SchoolBookEntity> getActiveBooksForSubject(
            long subjectRowId
    );

    @Nullable
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "AND is_primary_book = 1 "
                    + "AND is_active = 1 "
                    + "LIMIT 1"
    )
    SchoolBookEntity getPrimaryBookForSubject(
            long subjectRowId
    );

    /*
     * ISBN-13 के आधार पर duplicate book खोजता है।
     *
     * Blank ISBN search नहीं किया जाएगा।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE isbn_13 = :isbn13 "
                    + "AND isbn_13 != '' "
                    + "LIMIT 1"
    )
    SchoolBookEntity findBookByIsbn13(
            @NonNull String isbn13
    );

    /*
     * ISBN-10 के आधार पर duplicate book खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE isbn_10 = :isbn10 "
                    + "AND isbn_10 != '' "
                    + "LIMIT 1"
    )
    SchoolBookEntity findBookByIsbn10(
            @NonNull String isbn10
    );

    /*
     * किसी particular subject के अंदर ISBN-13 match खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "AND isbn_13 = :isbn13 "
                    + "AND isbn_13 != '' "
                    + "LIMIT 1"
    )
    SchoolBookEntity findSubjectBookByIsbn13(
            long subjectRowId,
            @NonNull String isbn13
    );

    /*
     * किसी particular subject के अंदर ISBN-10 match खोजता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "AND isbn_10 = :isbn10 "
                    + "AND isbn_10 != '' "
                    + "LIMIT 1"
    )
    SchoolBookEntity findSubjectBookByIsbn10(
            long subjectRowId,
            @NonNull String isbn10
    );

    /*
     * Exact normalized title और publisher से duplicate जाँचता है।
     */
    @Nullable
    @Query(
            "SELECT * FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "AND LOWER(TRIM(book_title)) "
                    + "= LOWER(TRIM(:bookTitle)) "
                    + "AND LOWER(TRIM(publisher_name)) "
                    + "= LOWER(TRIM(:publisherName)) "
                    + "LIMIT 1"
    )
    SchoolBookEntity findBookByTitleAndPublisher(
            long subjectRowId,
            @NonNull String bookTitle,
            @NonNull String publisherName
    );

    @Query(
            "SELECT COUNT(*) FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int getBookCountForSubject(
            long subjectRowId
    );

    @Query(
            "SELECT COUNT(*) FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId "
                    + "AND is_active = 1"
    )
    int getActiveBookCountForSubject(
            long subjectRowId
    );

    @Query(
            "SELECT COALESCE(MAX(sort_order), 0) "
                    + "FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int getMaximumSortOrder(
            long subjectRowId
    );

    /*
     * Book को active या inactive करता है।
     */
    @Query(
            "UPDATE school_books "
                    + "SET is_active = :active, "
                    + "updated_at = :updatedAt "
                    + "WHERE book_row_id = :bookRowId"
    )
    int setBookActive(
            long bookRowId,
            boolean active,
            long updatedAt
    );

    /*
     * Parent द्वारा book match confirm किए जाने की स्थिति save करता है।
     */
    @Query(
            "UPDATE school_books "
                    + "SET parent_confirmed_match = :confirmed, "
                    + "updated_at = :updatedAt "
                    + "WHERE book_row_id = :bookRowId"
    )
    int setParentConfirmedMatch(
            long bookRowId,
            boolean confirmed,
            long updatedAt
    );

    /*
     * किसी subject की सभी books से primary status हटाता है।
     */
    @Query(
            "UPDATE school_books "
                    + "SET is_primary_book = 0, "
                    + "updated_at = :updatedAt "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int clearPrimaryBookForSubject(
            long subjectRowId,
            long updatedAt
    );

    /*
     * चुनी गई book को primary बनाता है।
     */
    @Query(
            "UPDATE school_books "
                    + "SET is_primary_book = 1, "
                    + "is_active = 1, "
                    + "updated_at = :updatedAt "
                    + "WHERE book_row_id = :bookRowId "
                    + "AND subject_row_id = :subjectRowId"
    )
    int markBookAsPrimary(
            long subjectRowId,
            long bookRowId,
            long updatedAt
    );

    /*
     * पहले पुराना primary status हटाता है और फिर नई book को
     * primary बनाता है। दोनों operations एक transaction में होंगे।
     */
    @Transaction
    default boolean setPrimaryBook(
            long subjectRowId,
            long bookRowId
    ) {
        long currentTime =
                System.currentTimeMillis();

        clearPrimaryBookForSubject(
                subjectRowId,
                currentTime
        );

        return markBookAsPrimary(
                subjectRowId,
                bookRowId,
                currentTime
        ) > 0;
    }

    /*
     * किसी subject की books की sort order update करता है।
     */
    @Query(
            "UPDATE school_books "
                    + "SET sort_order = :sortOrder, "
                    + "updated_at = :updatedAt "
                    + "WHERE book_row_id = :bookRowId"
    )
    int updateBookSortOrder(
            long bookRowId,
            int sortOrder,
            long updatedAt
    );

    /*
     * Scan/online search का latest timestamp update करता है।
     */
    @Query(
            "UPDATE school_books "
                    + "SET last_online_search_at = :searchedAt, "
                    + "updated_at = :updatedAt "
                    + "WHERE book_row_id = :bookRowId"
    )
    int updateLastOnlineSearchTime(
            long bookRowId,
            long searchedAt,
            long updatedAt
    );

    /*
     * किसी subject की सभी books permanently delete करता है।
     *
     * यह मुख्य रूप से profile cleanup या testing के लिए है।
     */
    @Query(
            "DELETE FROM school_books "
                    + "WHERE subject_row_id = :subjectRowId"
    )
    int deleteBooksForSubject(
            long subjectRowId
    );
}