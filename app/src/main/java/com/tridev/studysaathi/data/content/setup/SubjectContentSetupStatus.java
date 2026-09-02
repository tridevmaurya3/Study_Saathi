package com.tridev.studysaathi.data.content.setup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Existing subject summary fields से अगला safe content-setup step निकालता है।
 *
 * यह resolver database, Firebase या navigation को नहीं बदलता। इसका उद्देश्य
 * पुराने Subject -> Book -> Chapter flow के ऊपर एक छोटा compatibility contract
 * देना है, ताकि UI user को केवल अगला जरूरी action दिखा सके।
 */
public final class SubjectContentSetupStatus {

    private SubjectContentSetupStatus() {
        throw new IllegalStateException(
                "SubjectContentSetupStatus cannot be instantiated."
        );
    }

    @NonNull
    public static Result resolve(
            boolean subjectEnabled,
            @Nullable String bookName,
            int chapterCount
    ) {
        if (!subjectEnabled) {
            return new Result(
                    Step.HIDDEN,
                    "Setup paused",
                    "Subject और उसका पुराना data सुरक्षित है",
                    "Continue Setup"
            );
        }

        if (safeText(bookName).isEmpty()) {
            return new Result(
                    Step.ADD_BOOK,
                    "Next: Add exact school book",
                    "Book scan, search या manual entry से शुरू करें",
                    "Add Book"
            );
        }

        return new Result(
                Step.CONTINUE_BOOK_SETUP,
                "Next: Continue book setup",
                chapterCount > 0
                        ? chapterCount + (chapterCount == 1
                        ? " chapter summary उपलब्ध है"
                        : " chapters summary उपलब्ध है")
                        : "Chapters और learning material जोड़ें या review करें",
                "Continue Setup"
        );
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    public enum Step {
        HIDDEN,
        ADD_BOOK,
        CONTINUE_BOOK_SETUP
    }

    public static final class Result {

        @NonNull
        private final Step step;

        @NonNull
        private final String title;

        @NonNull
        private final String description;

        @NonNull
        private final String primaryActionLabel;

        private Result(
                @NonNull Step step,
                @NonNull String title,
                @NonNull String description,
                @NonNull String primaryActionLabel
        ) {
            this.step = step;
            this.title = title;
            this.description = description;
            this.primaryActionLabel = primaryActionLabel;
        }

        @NonNull
        public Step getStep() {
            return step;
        }

        @NonNull
        public String getTitle() {
            return title;
        }

        @NonNull
        public String getDescription() {
            return description;
        }

        @NonNull
        public String getPrimaryActionLabel() {
            return primaryActionLabel;
        }
    }
}
