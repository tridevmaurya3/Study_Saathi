package com.tridev.studysaathi.navigation;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.LessonActivity;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;

public final class ExactSchoolBookLessonIntentFactory {

    private ExactSchoolBookLessonIntentFactory() {
        throw new AssertionError(
                "ExactSchoolBookLessonIntentFactory "
                        + "cannot be instantiated."
        );
    }

    @NonNull
    public static Intent create(
            @NonNull Context context,
            @NonNull SchoolBookChapterEntity chapter,
            @NonNull String subjectName,
            @NonNull String studentClass,
            @NonNull String educationBoard
    ) {
        validateChapter(
                chapter
        );

        Intent lessonIntent =
                new Intent(
                        context,
                        LessonActivity.class
                );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                safeText(
                        subjectName
                )
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                chapter.getDisplayTitle()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                safeText(
                        chapter.getChapterDescription()
                )
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_STUDENT_CLASS,
                safeText(
                        studentClass
                )
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_EDUCATION_BOARD,
                safeText(
                        educationBoard
                )
        );

        ExactSchoolBookLessonContract.putExactChapterRowId(
                lessonIntent,
                chapter.getChapterRowId()
        );

        return lessonIntent;
    }

    private static void validateChapter(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        if (chapter.getChapterRowId()
                <= ExactSchoolBookLessonContract
                .INVALID_CHAPTER_ROW_ID) {

            throw new IllegalArgumentException(
                    "The exact chapter does not have "
                            + "a valid database row ID."
            );
        }

        if (!chapter.isReadyForChildMode()) {
            throw new IllegalStateException(
                    "The exact chapter is not available "
                            + "in Child Mode."
            );
        }

        if (safeText(
                chapter.getDisplayTitle()
        ).isEmpty()) {

            throw new IllegalStateException(
                    "The exact chapter title is missing."
            );
        }
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString().trim();
    }
}