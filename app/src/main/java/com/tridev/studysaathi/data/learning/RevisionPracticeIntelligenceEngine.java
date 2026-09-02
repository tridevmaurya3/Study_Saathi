package com.tridev.studysaathi.data.learning;

import android.content.Context;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.ai.RecommendedRevisionProgressStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts existing learning-memory signals into a small revision/practice queue.
 *
 * It does not create a new database or scheduler. WeakTopicLearningMemory remains
 * the source of learning state, while RecommendedRevisionProgressStore remains
 * the source of revision start/completion history.
 */
public final class RevisionPracticeIntelligenceEngine {

    private static final int DEFAULT_PLAN_LIMIT = 5;

    @NonNull private final WeakTopicLearningMemory learningMemory;
    @NonNull private final RecommendedRevisionProgressStore revisionProgressStore;

    public RevisionPracticeIntelligenceEngine(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        learningMemory = new WeakTopicLearningMemory(applicationContext);
        revisionProgressStore = new RecommendedRevisionProgressStore(applicationContext);
    }

    @NonNull
    public Plan buildPlan(long profileId) {
        return buildPlan(profileId, DEFAULT_PLAN_LIMIT);
    }

    @NonNull
    public Plan buildPlan(long profileId, int limit) {
        if (profileId <= 0L || limit <= 0) return Plan.empty();

        List<WeakTopicLearningMemory.TopicMemory> candidates =
                learningMemory.getWeakTopics(profileId, Math.max(limit * 2, limit));

        List<NextAction> actions = new ArrayList<>();
        for (WeakTopicLearningMemory.TopicMemory memory : candidates) {
            if (actions.size() >= limit) break;

            RecommendedRevisionProgressStore.Summary progress =
                    revisionProgressStore.getSummary(
                            memory.getSubject(),
                            memory.getChapter()
                    );

            ActionType type = chooseAction(memory, progress);
            actions.add(new NextAction(
                    type,
                    memory.getSubject(),
                    memory.getChapter(),
                    memory.getConceptLabel(),
                    memory.getReadinessScore(),
                    memory.getState(),
                    progress.getPending(),
                    buildPrompt(type, memory)
            ));
        }

        int urgent = 0;
        for (NextAction action : actions) {
            if (action.type == ActionType.EASIER_RETEACH
                    || action.type == ActionType.TWO_MINUTE_REVISION) {
                urgent++;
            }
        }

        return new Plan(actions, urgent);
    }

    @NonNull
    public NextAction getNextAction(long profileId) {
        Plan plan = buildPlan(profileId, 1);
        return plan.actions.isEmpty()
                ? NextAction.none()
                : plan.actions.get(0);
    }

    public void recordStarted(@NonNull NextAction action) {
        if (!action.isActionable()) return;
        revisionProgressStore.recordStarted(action.subject, action.chapter);
    }

    public void recordCompleted(@NonNull NextAction action) {
        if (!action.isActionable()) return;
        revisionProgressStore.recordCompleted(action.subject, action.chapter);
    }

    @NonNull
    private ActionType chooseAction(
            @NonNull WeakTopicLearningMemory.TopicMemory memory,
            @NonNull RecommendedRevisionProgressStore.Summary progress
    ) {
        if (memory.getMisconceptionCount() > 0
                || (memory.getQuizAttempts() > 0
                && memory.getAverageQuizPercentage() < 50)) {
            return ActionType.EASIER_RETEACH;
        }

        if (memory.getState()
                == WeakTopicLearningMemory.TopicState.NEEDS_REVISION) {
            return ActionType.TWO_MINUTE_REVISION;
        }

        if (progress.getPending() > 0) {
            return ActionType.COMPLETE_PENDING_REVISION;
        }

        return ActionType.QUICK_PRACTICE_CHECK;
    }

    @NonNull
    private String buildPrompt(
            @NonNull ActionType type,
            @NonNull WeakTopicLearningMemory.TopicMemory memory
    ) {
        String concept = memory.getConceptLabel().isEmpty()
                ? memory.getChapter()
                : memory.getConceptLabel();

        switch (type) {
            case EASIER_RETEACH:
                return "इस topic को foundation level पर बहुत आसान तरीके से दोबारा समझाओ: "
                        + concept
                        + ". पहले common mistake ठीक करो, फिर एक छोटा example और अंत में एक check question दो।";
            case TWO_MINUTE_REVISION:
                return "2-minute revision कराओ: " + concept
                        + ". केवल core idea, 3 key points, एक common mistake और एक quick check दो।";
            case COMPLETE_PENDING_REVISION:
                return "मेरी pending revision पूरी कराओ: " + concept
                        + ". पहले बहुत छोटा recap दो और फिर एक बिना-answer वाला check question पूछो।";
            case QUICK_PRACTICE_CHECK:
                return "इस topic पर एक छोटा practice check दो: " + concept
                        + ". एक question पूछो, अभी answer मत बताओ; मेरे जवाब के बाद difficulty adjust करना।";
            case NONE:
            default:
                return "";
        }
    }

    public enum ActionType {
        EASIER_RETEACH,
        TWO_MINUTE_REVISION,
        COMPLETE_PENDING_REVISION,
        QUICK_PRACTICE_CHECK,
        NONE
    }

    public static final class NextAction {
        @NonNull private final ActionType type;
        @NonNull private final String subject;
        @NonNull private final String chapter;
        @NonNull private final String conceptLabel;
        private final int readinessScore;
        @NonNull private final WeakTopicLearningMemory.TopicState topicState;
        private final int pendingRevisionCount;
        @NonNull private final String askPrompt;

        private NextAction(
                @NonNull ActionType type,
                @NonNull String subject,
                @NonNull String chapter,
                @NonNull String conceptLabel,
                int readinessScore,
                @NonNull WeakTopicLearningMemory.TopicState topicState,
                int pendingRevisionCount,
                @NonNull String askPrompt
        ) {
            this.type = type;
            this.subject = subject;
            this.chapter = chapter;
            this.conceptLabel = conceptLabel;
            this.readinessScore = readinessScore;
            this.topicState = topicState;
            this.pendingRevisionCount = Math.max(0, pendingRevisionCount);
            this.askPrompt = askPrompt;
        }

        @NonNull
        static NextAction none() {
            return new NextAction(
                    ActionType.NONE,
                    "",
                    "",
                    "",
                    0,
                    WeakTopicLearningMemory.TopicState.LEARNING,
                    0,
                    ""
            );
        }

        public boolean isActionable() {
            return type != ActionType.NONE && !askPrompt.isEmpty();
        }

        @NonNull public ActionType getType() { return type; }
        @NonNull public String getSubject() { return subject; }
        @NonNull public String getChapter() { return chapter; }
        @NonNull public String getConceptLabel() { return conceptLabel; }
        public int getReadinessScore() { return readinessScore; }
        @NonNull public WeakTopicLearningMemory.TopicState getTopicState() { return topicState; }
        public int getPendingRevisionCount() { return pendingRevisionCount; }
        @NonNull public String getAskPrompt() { return askPrompt; }
    }

    public static final class Plan {
        @NonNull private final List<NextAction> actions;
        private final int urgentCount;

        private Plan(@NonNull List<NextAction> actions, int urgentCount) {
            this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
            this.urgentCount = Math.max(0, urgentCount);
        }

        @NonNull
        static Plan empty() {
            return new Plan(Collections.emptyList(), 0);
        }

        @NonNull public List<NextAction> getActions() { return actions; }
        public int getUrgentCount() { return urgentCount; }
        public boolean hasRecommendedWork() { return !actions.isEmpty(); }
    }
}
