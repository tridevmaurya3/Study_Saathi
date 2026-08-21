package com.tridev.studysaathi.data.learning;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Converts Knowledge Graph readiness into a stable teaching level. */
public final class AdaptiveLearningLevelResolver {
    @NonNull private final StudentKnowledgeGraphStore graphStore;

    public AdaptiveLearningLevelResolver(@NonNull Context context) {
        graphStore = new StudentKnowledgeGraphStore(context);
    }

    @NonNull
    public AdaptiveLevel resolve(long profileId,
                                 @Nullable String subject,
                                 @Nullable String chapter) {
        return resolve(profileId, subject, chapter, "");
    }

    @NonNull
    public AdaptiveLevel resolve(long profileId,
                                 @Nullable String subject,
                                 @Nullable String chapter,
                                 @Nullable String question) {
        String request = question == null ? "" : question.toLowerCase();
        if (request.contains("और आसान") || request.contains("सरल भाषा")
                || request.contains("simple words") || request.contains("easier")) {
            return AdaptiveLevel.FOUNDATION;
        }
        StudentKnowledgeGraphStore.ContextReadiness readiness =
                graphStore.getContextReadiness(profileId, subject, chapter);
        if (readiness.getConceptCount() == 0 || readiness.getReadinessScore() < 35) {
            return AdaptiveLevel.FOUNDATION;
        }
        if (readiness.getReadinessScore() >= 70 && readiness.getQuizAttempts() > 0) {
            return AdaptiveLevel.ADVANCED;
        }
        return AdaptiveLevel.STANDARD;
    }

    public enum AdaptiveLevel {
        FOUNDATION("Foundation", "आसान शुरुआत",
                "Use very simple words, one idea at a time, extra steps and one familiar example."),
        STANDARD("Standard", "सामान्य स्तर",
                "Use class-appropriate detail, clear steps and one useful example."),
        ADVANCED("Advanced", "उन्नत स्तर",
                "Be concise but deeper, connect related concepts and include one challenge question when useful.");

        @NonNull private final String requestValue;
        @NonNull private final String displayLabel;
        @NonNull private final String promptInstruction;

        AdaptiveLevel(@NonNull String requestValue, @NonNull String displayLabel,
                      @NonNull String promptInstruction) {
            this.requestValue = requestValue;
            this.displayLabel = displayLabel;
            this.promptInstruction = promptInstruction;
        }

        @NonNull public String getRequestValue() { return requestValue; }
        @NonNull public String getDisplayLabel() { return displayLabel; }
        @NonNull public String getPromptInstruction() { return promptInstruction; }

        @NonNull
        public static AdaptiveLevel fromRequestValue(@Nullable String value) {
            if (value != null) {
                for (AdaptiveLevel level : values()) {
                    if (level.requestValue.equalsIgnoreCase(value.trim())) return level;
                }
            }
            return STANDARD;
        }
    }
}
