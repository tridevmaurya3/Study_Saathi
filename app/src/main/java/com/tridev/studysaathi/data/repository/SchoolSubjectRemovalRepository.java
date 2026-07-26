package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.dao.SchoolSubjectDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;

public final class SchoolSubjectRemovalRepository {

    public static final String ACTION_HIDDEN =
            "HIDDEN";

    public static final String ACTION_DELETED =
            "DELETED";

    @NonNull
    private final SchoolSubjectDao schoolSubjectDao;

    @NonNull
    private final Handler mainThreadHandler;

    public SchoolSubjectRemovalRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(
                        context.getApplicationContext()
                );

        schoolSubjectDao =
                database.schoolSubjectDao();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Subject को Child Mode से hide करता है।
     *
     * इस action में:
     * - Subject database में सुरक्षित रहता है।
     * - Exact school book सुरक्षित रहती है।
     * - Chapters, lessons और quiz counts सुरक्षित रहते हैं।
     * - Subject को बाद में दोबारा enable किया जा सकता है।
     */
    public void hideSubjectFromChildMode(
            long subjectRowId,
            @NonNull RemovalCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        int updatedRows =
                                schoolSubjectDao
                                        .setSubjectEnabled(
                                                subjectRowId,
                                                false,
                                                System.currentTimeMillis()
                                        );

                        requireAffectedRow(
                                updatedRows
                        );

                        RemovalResult result =
                                new RemovalResult(
                                        subjectRowId,
                                        ACTION_HIDDEN
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        result
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
     * Subject को permanently delete करता है।
     *
     * महत्वपूर्ण:
     * SchoolSubjectDao में subject और school_books के बीच
     * ForeignKey CASCADE configured है।
     *
     * इसलिए subject permanently delete होने पर उससे जुड़ी
     * exact school book भी delete हो सकती है।
     *
     * इस method को केवल final confirmation के बाद call करें।
     */
    public void deleteSubjectPermanently(
            @NonNull SchoolSubjectEntity schoolSubject,
            @NonNull RemovalCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubject(
                                schoolSubject
                        );

                        long subjectRowId =
                                schoolSubject.getSubjectRowId();

                        int deletedRows =
                                schoolSubjectDao
                                        .deleteSubject(
                                                schoolSubject
                                        );

                        requireAffectedRow(
                                deletedRows
                        );

                        RemovalResult result =
                                new RemovalResult(
                                        subjectRowId,
                                        ACTION_DELETED
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        result
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
     * Subject में book या generated curriculum information
     * जुड़ी है या नहीं, इसकी safe summary बनाता है।
     */
    @NonNull
    public static RemovalImpact inspectRemovalImpact(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String bookName =
                safeTextValue(
                        schoolSubject.getBookName()
                );

        String bookCode =
                safeTextValue(
                        schoolSubject.getBookCode()
                );

        String publisherName =
                safeTextValue(
                        schoolSubject.getPublisherName()
                );

        boolean hasBookInformation =
                !bookName.isEmpty()
                        || !bookCode.isEmpty()
                        || !publisherName.isEmpty();

        int chapterCount =
                Math.max(
                        0,
                        schoolSubject.getChapterCount()
                );

        int lessonCount =
                Math.max(
                        0,
                        schoolSubject.getLessonCount()
                );

        int quizQuestionCount =
                Math.max(
                        0,
                        schoolSubject.getQuizQuestionCount()
                );

        boolean hasGeneratedContent =
                chapterCount > 0
                        || lessonCount > 0
                        || quizQuestionCount > 0;

        return new RemovalImpact(
                hasBookInformation,
                hasGeneratedContent,
                bookName,
                bookCode,
                publisherName,
                chapterCount,
                lessonCount,
                quizQuestionCount
        );
    }

    private void validateSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        validateSubjectRowId(
                schoolSubject.getSubjectRowId()
        );

        if (schoolSubject.getProfileId() <= 0L) {
            throw new IllegalArgumentException(
                    "A valid curriculum profile ID is required."
            );
        }
    }

    private void validateSubjectRowId(
            long subjectRowId
    ) {
        if (subjectRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school subject row ID is required."
            );
        }
    }

    private void requireAffectedRow(
            int affectedRows
    ) {
        if (affectedRows <= 0) {
            throw new IllegalStateException(
                    "The selected school subject was not found."
            );
        }
    }

    private void postToMainThread(
            @NonNull Runnable runnable
    ) {
        mainThreadHandler.post(
                runnable
        );
    }

    private void postError(
            @NonNull RemovalCallback callback,
            @NonNull Exception exception
    ) {
        postToMainThread(() ->
                callback.onError(
                        exception
                )
        );
    }

    @NonNull
    private static String safeTextValue(
            Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    public interface RemovalCallback {

        void onSuccess(
                @NonNull RemovalResult result
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class RemovalResult {

        private final long subjectRowId;

        @NonNull
        private final String action;

        private RemovalResult(
                long subjectRowId,
                @NonNull String action
        ) {
            this.subjectRowId =
                    Math.max(
                            0L,
                            subjectRowId
                    );

            this.action =
                    action;
        }

        public long getSubjectRowId() {
            return subjectRowId;
        }

        @NonNull
        public String getAction() {
            return action;
        }

        public boolean wasHidden() {
            return ACTION_HIDDEN.equals(
                    action
            );
        }

        public boolean wasDeleted() {
            return ACTION_DELETED.equals(
                    action
            );
        }
    }

    public static final class RemovalImpact {

        private final boolean hasBookInformation;

        private final boolean hasGeneratedContent;

        @NonNull
        private final String bookName;

        @NonNull
        private final String bookCode;

        @NonNull
        private final String publisherName;

        private final int chapterCount;

        private final int lessonCount;

        private final int quizQuestionCount;

        private RemovalImpact(
                boolean hasBookInformation,
                boolean hasGeneratedContent,
                @NonNull String bookName,
                @NonNull String bookCode,
                @NonNull String publisherName,
                int chapterCount,
                int lessonCount,
                int quizQuestionCount
        ) {
            this.hasBookInformation =
                    hasBookInformation;

            this.hasGeneratedContent =
                    hasGeneratedContent;

            this.bookName =
                    bookName;

            this.bookCode =
                    bookCode;

            this.publisherName =
                    publisherName;

            this.chapterCount =
                    Math.max(
                            0,
                            chapterCount
                    );

            this.lessonCount =
                    Math.max(
                            0,
                            lessonCount
                    );

            this.quizQuestionCount =
                    Math.max(
                            0,
                            quizQuestionCount
                    );
        }

        public boolean hasBookInformation() {
            return hasBookInformation;
        }

        public boolean hasGeneratedContent() {
            return hasGeneratedContent;
        }

        public boolean hasProtectedData() {
            return hasBookInformation
                    || hasGeneratedContent;
        }

        @NonNull
        public String getBookName() {
            return bookName;
        }

        @NonNull
        public String getBookCode() {
            return bookCode;
        }

        @NonNull
        public String getPublisherName() {
            return publisherName;
        }

        public int getChapterCount() {
            return chapterCount;
        }

        public int getLessonCount() {
            return lessonCount;
        }

        public int getQuizQuestionCount() {
            return quizQuestionCount;
        }

        @NonNull
        public String createProtectedDataSummary() {
            StringBuilder summaryBuilder =
                    new StringBuilder();

            if (hasBookInformation) {
                summaryBuilder.append(
                        "Exact school book"
                );

                if (!bookName.isEmpty()) {
                    summaryBuilder.append(
                            ": "
                    );

                    summaryBuilder.append(
                            bookName
                    );
                }
            }

            if (chapterCount > 0) {
                appendSummaryLine(
                        summaryBuilder,
                        chapterCount
                                + (chapterCount == 1
                                ? " chapter"
                                : " chapters")
                );
            }

            if (lessonCount > 0) {
                appendSummaryLine(
                        summaryBuilder,
                        lessonCount
                                + (lessonCount == 1
                                ? " lesson"
                                : " lessons")
                );
            }

            if (quizQuestionCount > 0) {
                appendSummaryLine(
                        summaryBuilder,
                        quizQuestionCount
                                + (quizQuestionCount == 1
                                ? " quiz question"
                                : " quiz questions")
                );
            }

            if (summaryBuilder.length() == 0) {
                return "इस subject के साथ अभी कोई exact book या generated content नहीं जुड़ा है।";
            }

            return summaryBuilder.toString();
        }

        private void appendSummaryLine(
                @NonNull StringBuilder builder,
                @NonNull String value
        ) {
            if (builder.length() > 0) {
                builder.append(
                        "\n"
                );
            }

            builder.append(
                    value
            );
        }
    }
}