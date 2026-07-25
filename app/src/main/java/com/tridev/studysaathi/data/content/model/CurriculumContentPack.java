package com.tridev.studysaathi.data.content.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class CurriculumContentPack {

    private final String packId;

    private final int packVersion;

    private final String educationBoard;

    private final int classNumber;

    private final String academicSession;

    private final String studyMedium;

    private final BilingualText packTitle;

    private final List<SubjectContent> subjects;

    public CurriculumContentPack(
            @NonNull String packId,
            int packVersion,
            @NonNull String educationBoard,
            int classNumber,
            @NonNull String academicSession,
            @NonNull String studyMedium,
            @NonNull BilingualText packTitle,
            @NonNull List<SubjectContent> subjects
    ) {
        this.packId =
                requireText(
                        packId,
                        "Pack ID"
                );

        if (packVersion < 1) {
            throw new IllegalArgumentException(
                    "Pack version must be at least 1."
            );
        }

        this.packVersion =
                packVersion;

        this.educationBoard =
                requireText(
                        educationBoard,
                        "Education board"
                );

        if (classNumber < 1
                || classNumber > 12) {

            throw new IllegalArgumentException(
                    "Class number must be between 1 and 12."
            );
        }

        this.classNumber =
                classNumber;

        this.academicSession =
                requireText(
                        academicSession,
                        "Academic session"
                );

        this.studyMedium =
                requireText(
                        studyMedium,
                        "Study medium"
                );

        this.packTitle =
                packTitle;

        this.subjects =
                createImmutableList(
                        subjects
                );
    }

    @NonNull
    public String getPackId() {
        return packId;
    }

    public int getPackVersion() {
        return packVersion;
    }

    @NonNull
    public String getEducationBoard() {
        return educationBoard;
    }

    public int getClassNumber() {
        return classNumber;
    }

    @NonNull
    public String getAcademicSession() {
        return academicSession;
    }

    @NonNull
    public String getStudyMedium() {
        return studyMedium;
    }

    @NonNull
    public BilingualText getPackTitle() {
        return packTitle;
    }

    @NonNull
    public List<SubjectContent> getSubjects() {
        return subjects;
    }

    public int getSubjectCount() {
        return subjects.size();
    }

    public int getTotalChapterCount() {
        int chapterCount = 0;

        for (SubjectContent subject :
                subjects) {

            chapterCount +=
                    subject.getChapterCount();
        }

        return chapterCount;
    }

    public int getTotalLessonCount() {
        int lessonCount = 0;

        for (SubjectContent subject :
                subjects) {

            lessonCount +=
                    subject.getTotalLessonCount();
        }

        return lessonCount;
    }

    public int getTotalQuizQuestionCount() {
        int quizQuestionCount = 0;

        for (SubjectContent subject :
                subjects) {

            quizQuestionCount +=
                    subject.getTotalQuizQuestionCount();
        }

        return quizQuestionCount;
    }

    @Nullable
    public SubjectContent findSubjectById(
            @NonNull String subjectId
    ) {
        String requiredSubjectId =
                normalizeIdentifier(
                        subjectId
                );

        for (SubjectContent subject :
                subjects) {

            if (subject.getSubjectId()
                    .equals(requiredSubjectId)) {

                return subject;
            }
        }

        return null;
    }

    @Nullable
    public ChapterContent findChapterById(
            @NonNull String chapterId
    ) {
        String requiredChapterId =
                normalizeIdentifier(
                        chapterId
                );

        for (SubjectContent subject :
                subjects) {

            ChapterContent chapter =
                    subject.findChapterById(
                            requiredChapterId
                    );

            if (chapter != null) {
                return chapter;
            }
        }

        return null;
    }

    @Nullable
    public LessonItem findLessonById(
            @NonNull String lessonId
    ) {
        String requiredLessonId =
                normalizeIdentifier(
                        lessonId
                );

        for (SubjectContent subject :
                subjects) {

            LessonItem lesson =
                    subject.findLessonById(
                            requiredLessonId
                    );

            if (lesson != null) {
                return lesson;
            }
        }

        return null;
    }

    public static final class SubjectContent {

        private final String subjectId;

        private final BilingualText subjectName;

        private final BilingualText bookName;

        private final boolean officialNcertBook;

        private final List<ChapterContent> chapters;

        public SubjectContent(
                @NonNull String subjectId,
                @NonNull BilingualText subjectName,
                @NonNull BilingualText bookName,
                boolean officialNcertBook,
                @NonNull List<ChapterContent> chapters
        ) {
            this.subjectId =
                    normalizeIdentifier(
                            subjectId
                    );

            this.subjectName =
                    subjectName;

            this.bookName =
                    bookName;

            this.officialNcertBook =
                    officialNcertBook;

            this.chapters =
                    createImmutableList(
                            chapters
                    );
        }

        @NonNull
        public String getSubjectId() {
            return subjectId;
        }

        @NonNull
        public BilingualText getSubjectName() {
            return subjectName;
        }

        @NonNull
        public BilingualText getBookName() {
            return bookName;
        }

        public boolean isOfficialNcertBook() {
            return officialNcertBook;
        }

        @NonNull
        public List<ChapterContent> getChapters() {
            return chapters;
        }

        public int getChapterCount() {
            return chapters.size();
        }

        public int getTotalLessonCount() {
            int lessonCount = 0;

            for (ChapterContent chapter :
                    chapters) {

                lessonCount +=
                        chapter.getLessonCount();
            }

            return lessonCount;
        }

        public int getTotalQuizQuestionCount() {
            int questionCount = 0;

            for (ChapterContent chapter :
                    chapters) {

                questionCount +=
                        chapter.getQuizQuestionCount();
            }

            return questionCount;
        }

        @Nullable
        public ChapterContent findChapterById(
                @NonNull String chapterId
        ) {
            String requiredChapterId =
                    normalizeIdentifier(
                            chapterId
                    );

            for (ChapterContent chapter :
                    chapters) {

                if (chapter.getChapterId()
                        .equals(requiredChapterId)) {

                    return chapter;
                }
            }

            return null;
        }

        @Nullable
        public LessonItem findLessonById(
                @NonNull String lessonId
        ) {
            String requiredLessonId =
                    normalizeIdentifier(
                            lessonId
                    );

            for (ChapterContent chapter :
                    chapters) {

                LessonItem lesson =
                        chapter.findLessonById(
                                requiredLessonId
                        );

                if (lesson != null) {
                    return lesson;
                }
            }

            return null;
        }
    }

    public static final class ChapterContent {

        private final String chapterId;

        private final int chapterNumber;

        private final BilingualText chapterTitle;

        private final BilingualText chapterDescription;

        private final List<LessonItem> lessons;

        private final List<QuizQuestionItem>
                quizQuestions;

        public ChapterContent(
                @NonNull String chapterId,
                int chapterNumber,
                @NonNull BilingualText chapterTitle,
                @NonNull BilingualText chapterDescription,
                @NonNull List<LessonItem> lessons,
                @NonNull List<QuizQuestionItem> quizQuestions
        ) {
            this.chapterId =
                    normalizeIdentifier(
                            chapterId
                    );

            if (chapterNumber < 1) {
                throw new IllegalArgumentException(
                        "Chapter number must be at least 1."
                );
            }

            this.chapterNumber =
                    chapterNumber;

            this.chapterTitle =
                    chapterTitle;

            this.chapterDescription =
                    chapterDescription;

            this.lessons =
                    createImmutableList(
                            lessons
                    );

            this.quizQuestions =
                    createImmutableList(
                            quizQuestions
                    );
        }

        @NonNull
        public String getChapterId() {
            return chapterId;
        }

        public int getChapterNumber() {
            return chapterNumber;
        }

        @NonNull
        public BilingualText getChapterTitle() {
            return chapterTitle;
        }

        @NonNull
        public BilingualText getChapterDescription() {
            return chapterDescription;
        }

        @NonNull
        public List<LessonItem> getLessons() {
            return lessons;
        }

        @NonNull
        public List<QuizQuestionItem>
        getQuizQuestions() {

            return quizQuestions;
        }

        public int getLessonCount() {
            return lessons.size();
        }

        public int getQuizQuestionCount() {
            return quizQuestions.size();
        }

        public int getEstimatedStudyMinutes() {
            int totalMinutes = 0;

            for (LessonItem lesson :
                    lessons) {

                totalMinutes +=
                        lesson.getEstimatedMinutes();
            }

            return totalMinutes;
        }

        @Nullable
        public LessonItem findLessonById(
                @NonNull String lessonId
        ) {
            String requiredLessonId =
                    normalizeIdentifier(
                            lessonId
                    );

            for (LessonItem lesson :
                    lessons) {

                if (lesson.getLessonId()
                        .equals(requiredLessonId)) {

                    return lesson;
                }
            }

            return null;
        }
    }

    public static final class LessonItem {

        private final String lessonId;

        private final int lessonNumber;

        private final BilingualText lessonTitle;

        private final BilingualText explanation;

        private final List<BilingualText> keyPoints;

        private final BilingualText example;

        private final BilingualText practicePrompt;

        private final int estimatedMinutes;

        public LessonItem(
                @NonNull String lessonId,
                int lessonNumber,
                @NonNull BilingualText lessonTitle,
                @NonNull BilingualText explanation,
                @NonNull List<BilingualText> keyPoints,
                @NonNull BilingualText example,
                @NonNull BilingualText practicePrompt,
                int estimatedMinutes
        ) {
            this.lessonId =
                    normalizeIdentifier(
                            lessonId
                    );

            if (lessonNumber < 1) {
                throw new IllegalArgumentException(
                        "Lesson number must be at least 1."
                );
            }

            this.lessonNumber =
                    lessonNumber;

            this.lessonTitle =
                    lessonTitle;

            this.explanation =
                    explanation;

            this.keyPoints =
                    createImmutableList(
                            keyPoints
                    );

            this.example =
                    example;

            this.practicePrompt =
                    practicePrompt;

            if (estimatedMinutes < 1) {
                throw new IllegalArgumentException(
                        "Estimated lesson time must "
                                + "be at least 1 minute."
                );
            }

            this.estimatedMinutes =
                    estimatedMinutes;
        }

        @NonNull
        public String getLessonId() {
            return lessonId;
        }

        public int getLessonNumber() {
            return lessonNumber;
        }

        @NonNull
        public BilingualText getLessonTitle() {
            return lessonTitle;
        }

        @NonNull
        public BilingualText getExplanation() {
            return explanation;
        }

        @NonNull
        public List<BilingualText> getKeyPoints() {
            return keyPoints;
        }

        @NonNull
        public BilingualText getExample() {
            return example;
        }

        @NonNull
        public BilingualText getPracticePrompt() {
            return practicePrompt;
        }

        public int getEstimatedMinutes() {
            return estimatedMinutes;
        }
    }

    public static final class QuizQuestionItem {

        private static final int REQUIRED_OPTION_COUNT =
                4;

        private final String questionId;

        private final BilingualText question;

        private final List<BilingualText> options;

        private final int correctOptionIndex;

        private final BilingualText explanation;

        private final Difficulty difficulty;

        public QuizQuestionItem(
                @NonNull String questionId,
                @NonNull BilingualText question,
                @NonNull List<BilingualText> options,
                int correctOptionIndex,
                @NonNull BilingualText explanation,
                @NonNull Difficulty difficulty
        ) {
            this.questionId =
                    normalizeIdentifier(
                            questionId
                    );

            this.question =
                    question;

            if (options.size()
                    != REQUIRED_OPTION_COUNT) {

                throw new IllegalArgumentException(
                        "Every quiz question must "
                                + "have exactly 4 options."
                );
            }

            this.options =
                    createImmutableList(
                            options
                    );

            if (correctOptionIndex < 0
                    || correctOptionIndex
                    >= REQUIRED_OPTION_COUNT) {

                throw new IllegalArgumentException(
                        "Correct option index must "
                                + "be between 0 and 3."
                );
            }

            this.correctOptionIndex =
                    correctOptionIndex;

            this.explanation =
                    explanation;

            this.difficulty =
                    difficulty;
        }

        @NonNull
        public String getQuestionId() {
            return questionId;
        }

        @NonNull
        public BilingualText getQuestion() {
            return question;
        }

        @NonNull
        public List<BilingualText> getOptions() {
            return options;
        }

        public int getCorrectOptionIndex() {
            return correctOptionIndex;
        }

        @NonNull
        public BilingualText getExplanation() {
            return explanation;
        }

        @NonNull
        public Difficulty getDifficulty() {
            return difficulty;
        }

        @NonNull
        public BilingualText getCorrectOption() {
            return options.get(
                    correctOptionIndex
            );
        }
    }

    public static final class BilingualText {

        private final String english;

        private final String hindi;

        public BilingualText(
                @NonNull String english,
                @NonNull String hindi
        ) {
            this.english =
                    requireText(
                            english,
                            "English text"
                    );

            this.hindi =
                    requireText(
                            hindi,
                            "Hindi text"
                    );
        }

        @NonNull
        public String getEnglish() {
            return english;
        }

        @NonNull
        public String getHindi() {
            return hindi;
        }

        @NonNull
        public String getText(
                boolean useHindi
        ) {
            return useHindi
                    ? hindi
                    : english;
        }

        @NonNull
        public String getBilingualText() {
            return english
                    + "\n\n"
                    + hindi;
        }
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD;

        @NonNull
        public static Difficulty fromText(
                String value
        ) {
            if (value == null
                    || value.trim().isEmpty()) {

                return MEDIUM;
            }

            String normalizedValue =
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );

            try {
                return Difficulty.valueOf(
                        normalizedValue
                );

            } catch (IllegalArgumentException exception) {
                return MEDIUM;
            }
        }
    }

    @NonNull
    private static String requireText(
            String value,
            @NonNull String fieldName
    ) {
        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be empty."
            );
        }

        return value.trim();
    }

    @NonNull
    private static String normalizeIdentifier(
            String identifier
    ) {
        String safeIdentifier =
                requireText(
                        identifier,
                        "Content identifier"
                )
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9_-]",
                                "_"
                        )
                        .replaceAll(
                                "_+",
                                "_"
                        );

        while (safeIdentifier.startsWith("_")) {
            safeIdentifier =
                    safeIdentifier.substring(1);
        }

        while (safeIdentifier.endsWith("_")) {
            safeIdentifier =
                    safeIdentifier.substring(
                            0,
                            safeIdentifier.length() - 1
                    );
        }

        if (safeIdentifier.isEmpty()) {
            throw new IllegalArgumentException(
                    "Content identifier is invalid."
            );
        }

        return safeIdentifier;
    }

    @NonNull
    private static <T> List<T> createImmutableList(
            @NonNull List<T> sourceList
    ) {
        if (sourceList.contains(null)) {
            throw new IllegalArgumentException(
                    "Content lists cannot contain null items."
            );
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        sourceList
                )
        );
    }
}