package com.tridev.studysaathi.data.learning;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only learning-memory view built on top of StudentKnowledgeGraphStore.
 *
 * No second database is created. Existing answer, quiz and misconception
 * signals are converted into actionable student states:
 * Needs Practice, Needs Revision, Learning and Mastered.
 */
public final class WeakTopicLearningMemory {

    private static final int REVISION_READINESS_THRESHOLD = 68;
    private static final int MASTERED_READINESS_THRESHOLD = 82;
    private static final int REPEAT_ASK_THRESHOLD = 3;

    @NonNull
    private final StudentKnowledgeGraphStore knowledgeGraphStore;

    public WeakTopicLearningMemory(@NonNull Context context) {
        knowledgeGraphStore = new StudentKnowledgeGraphStore(
                context.getApplicationContext()
        );
    }

    @NonNull
    public MemorySummary buildSummary(long profileId) {
        if (profileId <= 0L) return MemorySummary.empty();

        StudentKnowledgeGraphStore.ProfileSummary graphSummary =
                knowledgeGraphStore.getProfileSummary(profileId);

        List<TopicMemory> all = new ArrayList<>();
        int mastered = 0;
        int needsRevision = 0;
        int needsPractice = 0;
        int learning = 0;

        for (StudentKnowledgeGraphStore.KnowledgeNode node
                : graphSummary.getRecentNodes()) {
            TopicState state = classify(node);
            TopicMemory memory = new TopicMemory(
                    node.getSubject(),
                    node.getChapter(),
                    node.getConceptLabel(),
                    state,
                    node.getReadinessScore(),
                    node.getAverageConfidence(),
                    node.getAverageQuizPercentage(),
                    node.getAnswerInteractions(),
                    node.getQuizAttempts(),
                    node.getMisconceptionReviewCount(),
                    node.getLastUpdatedAt()
            );
            all.add(memory);

            switch (state) {
                case MASTERED:
                    mastered++;
                    break;
                case NEEDS_REVISION:
                    needsRevision++;
                    break;
                case NEEDS_PRACTICE:
                    needsPractice++;
                    break;
                case LEARNING:
                default:
                    learning++;
                    break;
            }
        }

        all.sort(Comparator
                .comparingInt((TopicMemory memory) -> memory.getState().priority)
                .thenComparingInt(TopicMemory::getReadinessScore)
                .thenComparing(
                        Comparator.comparingLong(TopicMemory::getLastUpdatedAt)
                                .reversed()
                ));

        return new MemorySummary(
                all,
                mastered,
                needsRevision,
                needsPractice,
                learning,
                graphSummary.getAverageConfidence(),
                graphSummary.getMisconceptionReviewCount()
        );
    }

    @NonNull
    public List<TopicMemory> getWeakTopics(long profileId, int limit) {
        MemorySummary summary = buildSummary(profileId);
        List<TopicMemory> weak = new ArrayList<>();
        for (TopicMemory memory : summary.getTopics()) {
            if (memory.state == TopicState.NEEDS_REVISION
                    || memory.state == TopicState.NEEDS_PRACTICE) {
                weak.add(memory);
            }
        }
        weak.sort(Comparator
                .comparingInt((TopicMemory memory) -> memory.state.priority)
                .thenComparingInt(TopicMemory::getReadinessScore));
        int safeLimit = Math.max(0, Math.min(limit, weak.size()));
        return Collections.unmodifiableList(
                new ArrayList<>(weak.subList(0, safeLimit))
        );
    }

    @NonNull
    private TopicState classify(
            @NonNull StudentKnowledgeGraphStore.KnowledgeNode node
    ) {
        int readiness = node.getReadinessScore();
        int quizAverage = node.getAverageQuizPercentage();
        int quizAttempts = node.getQuizAttempts();
        int misconceptions = node.getMisconceptionReviewCount();
        int asks = node.getAnswerInteractions();

        if (node.getStage() == StudentKnowledgeGraphStore.LearningStage.MASTERED
                && readiness >= MASTERED_READINESS_THRESHOLD
                && misconceptions == 0) {
            return TopicState.MASTERED;
        }

        if (misconceptions > 0
                || (quizAttempts > 0 && quizAverage < 70)
                || (quizAttempts > 0 && readiness < REVISION_READINESS_THRESHOLD)) {
            return TopicState.NEEDS_REVISION;
        }

        if (asks >= REPEAT_ASK_THRESHOLD && quizAttempts == 0) {
            return TopicState.NEEDS_PRACTICE;
        }

        if (quizAttempts > 0 && quizAverage < 80) {
            return TopicState.NEEDS_PRACTICE;
        }

        return TopicState.LEARNING;
    }

    public enum TopicState {
        NEEDS_REVISION("Needs Revision", 0),
        NEEDS_PRACTICE("Needs Practice", 1),
        LEARNING("Learning", 2),
        MASTERED("Mastered", 3);

        @NonNull private final String displayLabel;
        private final int priority;

        TopicState(@NonNull String displayLabel, int priority) {
            this.displayLabel = displayLabel;
            this.priority = priority;
        }

        @NonNull public String getDisplayLabel() { return displayLabel; }
    }

    public static final class TopicMemory {
        @NonNull private final String subject;
        @NonNull private final String chapter;
        @NonNull private final String conceptLabel;
        @NonNull private final TopicState state;
        private final int readinessScore;
        private final int averageConfidence;
        private final int averageQuizPercentage;
        private final int askCount;
        private final int quizAttempts;
        private final int misconceptionCount;
        private final long lastUpdatedAt;

        private TopicMemory(
                @NonNull String subject,
                @NonNull String chapter,
                @NonNull String conceptLabel,
                @NonNull TopicState state,
                int readinessScore,
                int averageConfidence,
                int averageQuizPercentage,
                int askCount,
                int quizAttempts,
                int misconceptionCount,
                long lastUpdatedAt
        ) {
            this.subject = subject;
            this.chapter = chapter;
            this.conceptLabel = conceptLabel;
            this.state = state;
            this.readinessScore = readinessScore;
            this.averageConfidence = averageConfidence;
            this.averageQuizPercentage = averageQuizPercentage;
            this.askCount = askCount;
            this.quizAttempts = quizAttempts;
            this.misconceptionCount = misconceptionCount;
            this.lastUpdatedAt = lastUpdatedAt;
        }

        @NonNull public String getSubject() { return subject; }
        @NonNull public String getChapter() { return chapter; }
        @NonNull public String getConceptLabel() { return conceptLabel; }
        @NonNull public TopicState getState() { return state; }
        public int getReadinessScore() { return readinessScore; }
        public int getAverageConfidence() { return averageConfidence; }
        public int getAverageQuizPercentage() { return averageQuizPercentage; }
        public int getAskCount() { return askCount; }
        public int getQuizAttempts() { return quizAttempts; }
        public int getMisconceptionCount() { return misconceptionCount; }
        public long getLastUpdatedAt() { return lastUpdatedAt; }
    }

    public static final class MemorySummary {
        @NonNull private final List<TopicMemory> topics;
        private final int masteredCount;
        private final int needsRevisionCount;
        private final int needsPracticeCount;
        private final int learningCount;
        private final int averageConfidence;
        private final int misconceptionCount;

        private MemorySummary(
                @NonNull List<TopicMemory> topics,
                int masteredCount,
                int needsRevisionCount,
                int needsPracticeCount,
                int learningCount,
                int averageConfidence,
                int misconceptionCount
        ) {
            this.topics = Collections.unmodifiableList(new ArrayList<>(topics));
            this.masteredCount = masteredCount;
            this.needsRevisionCount = needsRevisionCount;
            this.needsPracticeCount = needsPracticeCount;
            this.learningCount = learningCount;
            this.averageConfidence = averageConfidence;
            this.misconceptionCount = misconceptionCount;
        }

        @NonNull
        static MemorySummary empty() {
            return new MemorySummary(
                    Collections.emptyList(), 0, 0, 0, 0, 0, 0
            );
        }

        @NonNull public List<TopicMemory> getTopics() { return topics; }
        public int getMasteredCount() { return masteredCount; }
        public int getNeedsRevisionCount() { return needsRevisionCount; }
        public int getNeedsPracticeCount() { return needsPracticeCount; }
        public int getLearningCount() { return learningCount; }
        public int getAverageConfidence() { return averageConfidence; }
        public int getMisconceptionCount() { return misconceptionCount; }
    }
}
