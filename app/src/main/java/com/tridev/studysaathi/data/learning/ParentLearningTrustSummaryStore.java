package com.tridev.studysaathi.data.learning;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Privacy-safe parent summary. It deliberately exposes aggregate learning
 * signals only and never reads or returns child chat transcripts/questions.
 */
public final class ParentLearningTrustSummaryStore {

    @NonNull private final WeakTopicLearningMemory learningMemory;
    @NonNull private final RevisionPracticeIntelligenceEngine revisionEngine;

    public ParentLearningTrustSummaryStore(@NonNull Context context) {
        Context app = context.getApplicationContext();
        learningMemory = new WeakTopicLearningMemory(app);
        revisionEngine = new RevisionPracticeIntelligenceEngine(app);
    }

    @NonNull
    public Summary getSummary(long profileId) {
        if (profileId <= 0L) return Summary.empty();

        WeakTopicLearningMemory.MemorySummary memory =
                learningMemory.buildSummary(profileId);
        RevisionPracticeIntelligenceEngine.NextAction next =
                revisionEngine.getNextAction(profileId);

        String nextLabel = "No urgent revision currently";
        if (next.isActionable()) {
            switch (next.getType()) {
                case EASIER_RETEACH:
                    nextLabel = "Needs an easier re-teach: " + safeTopic(next);
                    break;
                case TWO_MINUTE_REVISION:
                    nextLabel = "2-minute revision ready: " + safeTopic(next);
                    break;
                case COMPLETE_PENDING_REVISION:
                    nextLabel = "Pending revision: " + safeTopic(next);
                    break;
                case QUICK_PRACTICE_CHECK:
                    nextLabel = "Quick practice ready: " + safeTopic(next);
                    break;
                case NONE:
                default:
                    break;
            }
        }

        return new Summary(
                memory.getMasteredCount(),
                memory.getNeedsRevisionCount(),
                memory.getNeedsPracticeCount(),
                memory.getLearningCount(),
                memory.getAverageConfidence(),
                nextLabel
        );
    }

    @NonNull
    private String safeTopic(
            @NonNull RevisionPracticeIntelligenceEngine.NextAction action
    ) {
        String concept = action.getConceptLabel().trim();
        if (!concept.isEmpty()) return concept;
        String chapter = action.getChapter().trim();
        return chapter.isEmpty() ? action.getSubject().trim() : chapter;
    }

    public static final class Summary {
        private final int masteredCount;
        private final int needsRevisionCount;
        private final int needsPracticeCount;
        private final int learningCount;
        private final int averageConfidence;
        @NonNull private final String nextLearningAction;

        private Summary(
                int masteredCount,
                int needsRevisionCount,
                int needsPracticeCount,
                int learningCount,
                int averageConfidence,
                @NonNull String nextLearningAction
        ) {
            this.masteredCount = Math.max(0, masteredCount);
            this.needsRevisionCount = Math.max(0, needsRevisionCount);
            this.needsPracticeCount = Math.max(0, needsPracticeCount);
            this.learningCount = Math.max(0, learningCount);
            this.averageConfidence = Math.max(0, Math.min(100, averageConfidence));
            this.nextLearningAction = nextLearningAction;
        }

        @NonNull
        static Summary empty() {
            return new Summary(0, 0, 0, 0, 0, "No learning memory yet");
        }

        @NonNull
        public String buildParentDisplayText() {
            return "Learning Trust • "
                    + masteredCount + " mastered • "
                    + needsRevisionCount + " revision • "
                    + needsPracticeCount + " practice\n"
                    + nextLearningAction + "\n"
                    + "Privacy: this Parent view uses learning summaries only; raw child chats are not shown.";
        }

        public int getMasteredCount() { return masteredCount; }
        public int getNeedsRevisionCount() { return needsRevisionCount; }
        public int getNeedsPracticeCount() { return needsPracticeCount; }
        public int getLearningCount() { return learningCount; }
        public int getAverageConfidence() { return averageConfidence; }
        @NonNull public String getNextLearningAction() { return nextLearningAction; }
    }
}
