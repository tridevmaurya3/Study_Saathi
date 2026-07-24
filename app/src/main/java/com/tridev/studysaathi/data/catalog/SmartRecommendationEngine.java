package com.tridev.studysaathi.data.catalog;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.model.ChapterItem;
import com.tridev.studysaathi.model.StudyRecommendation;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SmartRecommendationEngine {

    private static final int WEAK_SCORE_LIMIT = 60;

    private SmartRecommendationEngine() {
        // Utility class.
    }

    @NonNull
    public static StudyRecommendation createRecommendation(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> lessonProgressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        Map<String, QuizScoreSummary> quizSummaryMap =
                buildQuizSummaryMap(quizAttempts);

        StudyRecommendation weakQuizRecommendation =
                findWeakQuizRecommendation(
                        studentProfile,
                        quizSummaryMap
                );

        if (weakQuizRecommendation.hasRecommendation()) {
            return weakQuizRecommendation;
        }

        StudyRecommendation practiceRecommendation =
                findCompletedLessonWithoutQuiz(
                        studentProfile,
                        lessonProgressList,
                        quizSummaryMap
                );

        if (practiceRecommendation.hasRecommendation()) {
            return practiceRecommendation;
        }

        StudyRecommendation nextLessonRecommendation =
                findNextIncompleteLesson(
                        studentProfile,
                        lessonProgressList
                );

        if (nextLessonRecommendation.hasRecommendation()) {
            return nextLessonRecommendation;
        }

        StudyRecommendation revisionRecommendation =
                findRevisionRecommendation(
                        studentProfile,
                        lessonProgressList
                );

        if (revisionRecommendation.hasRecommendation()) {
            return revisionRecommendation;
        }

        return StudyRecommendation.empty();
    }

    @NonNull
    private static Map<String, QuizScoreSummary> buildQuizSummaryMap(
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        Map<String, QuizScoreSummary> summaryMap =
                new LinkedHashMap<>();

        for (QuizAttemptEntity quizAttempt : quizAttempts) {
            String key = createChapterKey(
                    quizAttempt.getSubjectName(),
                    quizAttempt.getChapterTitle()
            );

            QuizScoreSummary summary =
                    summaryMap.get(key);

            if (summary == null) {
                summary = new QuizScoreSummary(
                        quizAttempt.getSubjectName(),
                        quizAttempt.getChapterTitle()
                );

                summaryMap.put(key, summary);
            }

            summary.addScore(
                    quizAttempt.getPercentage()
            );
        }

        return summaryMap;
    }

    @NonNull
    private static StudyRecommendation findWeakQuizRecommendation(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull Map<String, QuizScoreSummary> quizSummaryMap
    ) {
        QuizScoreSummary weakestSummary = null;
        int weakestAverage = 101;

        for (QuizScoreSummary summary
                : quizSummaryMap.values()) {

            int averageScore =
                    summary.getAverageScore();

            if (averageScore >= WEAK_SCORE_LIMIT) {
                continue;
            }

            if (averageScore < weakestAverage) {
                weakestAverage = averageScore;
                weakestSummary = summary;
            }
        }

        if (weakestSummary == null) {
            return StudyRecommendation.empty();
        }

        String description =
                findChapterDescription(
                        studentProfile,
                        weakestSummary.getSubjectName(),
                        weakestSummary.getChapterTitle()
                );

        return new StudyRecommendation(
                weakestSummary.getSubjectName(),
                weakestSummary.getChapterTitle(),
                description,
                StudyRecommendation.RecommendationType
                        .LOW_QUIZ_SCORE,
                weakestSummary.getAverageScore()
        );
    }

    @NonNull
    private static StudyRecommendation findCompletedLessonWithoutQuiz(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull Map<String, QuizScoreSummary> quizSummaryMap
    ) {
        for (LessonProgressEntity progress : progressList) {
            if (!progress.isCompleted()) {
                continue;
            }

            String key = createChapterKey(
                    progress.getSubjectName(),
                    progress.getChapterTitle()
            );

            if (quizSummaryMap.containsKey(key)) {
                continue;
            }

            String description =
                    findChapterDescription(
                            studentProfile,
                            progress.getSubjectName(),
                            progress.getChapterTitle()
                    );

            return new StudyRecommendation(
                    progress.getSubjectName(),
                    progress.getChapterTitle(),
                    description,
                    StudyRecommendation.RecommendationType
                            .PRACTICE_AFTER_LESSON,
                    0
            );
        }

        return StudyRecommendation.empty();
    }

    @NonNull
    private static StudyRecommendation findNextIncompleteLesson(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> progressList
    ) {
        List<SubjectItem> subjects =
                SubjectCatalog.getSubjectsForClass(
                        studentProfile.getStudentClass()
                );

        for (SubjectItem subject : subjects) {
            String subjectName =
                    subject.getSubjectName();

            List<ChapterItem> chapters =
                    ChapterCatalog.getChapters(
                            studentProfile.getEducationBoard(),
                            studentProfile.getStudentClass(),
                            subjectName
                    );

            for (ChapterItem chapter : chapters) {
                if (isChapterCompleted(
                        progressList,
                        subjectName,
                        chapter.getChapterTitle()
                )) {
                    continue;
                }

                return new StudyRecommendation(
                        subjectName,
                        chapter.getChapterTitle(),
                        chapter.getChapterDescription(),
                        StudyRecommendation.RecommendationType
                                .NEXT_LESSON,
                        0
                );
            }
        }

        return StudyRecommendation.empty();
    }

    @NonNull
    private static StudyRecommendation findRevisionRecommendation(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> progressList
    ) {
        LessonProgressEntity selectedProgress = null;

        for (LessonProgressEntity progress : progressList) {
            if (!progress.isCompleted()) {
                continue;
            }

            if (selectedProgress == null
                    || progress.getRevisionCount()
                    < selectedProgress.getRevisionCount()) {

                selectedProgress = progress;
            }
        }

        if (selectedProgress == null) {
            return StudyRecommendation.empty();
        }

        String description =
                findChapterDescription(
                        studentProfile,
                        selectedProgress.getSubjectName(),
                        selectedProgress.getChapterTitle()
                );

        return new StudyRecommendation(
                selectedProgress.getSubjectName(),
                selectedProgress.getChapterTitle(),
                description,
                StudyRecommendation.RecommendationType
                        .SMART_REVISION,
                0
        );
    }

    private static boolean isChapterCompleted(
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull String subjectName,
            @NonNull String chapterTitle
    ) {
        String requiredKey =
                createChapterKey(
                        subjectName,
                        chapterTitle
                );

        for (LessonProgressEntity progress : progressList) {
            if (!progress.isCompleted()) {
                continue;
            }

            String progressKey =
                    createChapterKey(
                            progress.getSubjectName(),
                            progress.getChapterTitle()
                    );

            if (progressKey.equals(requiredKey)) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private static String findChapterDescription(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull String subjectName,
            @NonNull String chapterTitle
    ) {
        List<ChapterItem> chapters =
                ChapterCatalog.getChapters(
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass(),
                        subjectName
                );

        String normalizedRequiredTitle =
                normalizeText(chapterTitle);

        for (ChapterItem chapter : chapters) {
            if (normalizeText(
                    chapter.getChapterTitle()
            ).equals(normalizedRequiredTitle)) {

                return chapter.getChapterDescription();
            }
        }

        return "Continue learning and practise this chapter.";
    }

    @NonNull
    private static String createChapterKey(
            String subjectName,
            String chapterTitle
    ) {
        return normalizeText(subjectName)
                + "|"
                + normalizeText(chapterTitle);
    }

    @NonNull
    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT);
    }

    private static class QuizScoreSummary {

        @NonNull
        private final String subjectName;

        @NonNull
        private final String chapterTitle;

        private int totalScore;
        private int attemptCount;

        QuizScoreSummary(
                @NonNull String subjectName,
                @NonNull String chapterTitle
        ) {
            this.subjectName = subjectName;
            this.chapterTitle = chapterTitle;
        }

        void addScore(int score) {
            totalScore += Math.max(
                    0,
                    Math.min(100, score)
            );

            attemptCount++;
        }

        @NonNull
        String getSubjectName() {
            return subjectName;
        }

        @NonNull
        String getChapterTitle() {
            return chapterTitle;
        }

        int getAverageScore() {
            if (attemptCount <= 0) {
                return 0;
            }

            return Math.round(
                    totalScore
                            / (float) attemptCount
            );
        }
    }
}