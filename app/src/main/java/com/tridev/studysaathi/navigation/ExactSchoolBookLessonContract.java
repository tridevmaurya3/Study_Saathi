package com.tridev.studysaathi.navigation;

import android.content.Intent;

import androidx.annotation.NonNull;

public final class ExactSchoolBookLessonContract {

    public static final String EXTRA_EXACT_CHAPTER_ROW_ID =
            "com.tridev.studysaathi.extra.EXACT_CHAPTER_ROW_ID";

    public static final long INVALID_CHAPTER_ROW_ID =
            0L;

    private ExactSchoolBookLessonContract() {
        throw new AssertionError(
                "ExactSchoolBookLessonContract cannot be instantiated."
        );
    }

    public static void putExactChapterRowId(
            @NonNull Intent intent,
            long chapterRowId
    ) {
        if (chapterRowId <= INVALID_CHAPTER_ROW_ID) {
            throw new IllegalArgumentException(
                    "A valid exact chapter row ID is required."
            );
        }

        intent.putExtra(
                EXTRA_EXACT_CHAPTER_ROW_ID,
                chapterRowId
        );
    }

    public static long readExactChapterRowId(
            @NonNull Intent intent
    ) {
        return intent.getLongExtra(
                EXTRA_EXACT_CHAPTER_ROW_ID,
                INVALID_CHAPTER_ROW_ID
        );
    }

    public static boolean hasValidExactChapterRowId(
            @NonNull Intent intent
    ) {
        return readExactChapterRowId(
                intent
        ) > INVALID_CHAPTER_ROW_ID;
    }
}