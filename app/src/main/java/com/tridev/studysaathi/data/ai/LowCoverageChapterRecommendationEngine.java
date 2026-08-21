package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Converts citation coverage summaries into bounded parent recommendations. */
public final class LowCoverageChapterRecommendationEngine {
    private static final int MINIMUM_REVIEWED_ANSWERS = 2;
    private static final int HEALTHY_COVERAGE_PERCENT = 70;

    private LowCoverageChapterRecommendationEngine() { }

    @NonNull
    public static List<Recommendation> recommend(
            @NonNull List<CitationCoverageHistoryStore.ScopeSummary> scopes,
            int limit) {
        if (limit <= 0 || scopes.isEmpty()) return Collections.emptyList();
        List<Recommendation> recommendations = new ArrayList<>();
        for (CitationCoverageHistoryStore.ScopeSummary scope : scopes) {
            if (scope == null || scope.getReviewed() < MINIMUM_REVIEWED_ANSWERS) continue;
            if (scope.getCoveragePercent() >= HEALTHY_COVERAGE_PERCENT
                    && scope.getAttentionNeeded() == 0) continue;
            int priority = (100 - scope.getCoveragePercent())
                    + Math.min(30, scope.getAttentionNeeded() * 10)
                    + Math.min(10, scope.getReviewed());
            recommendations.add(new Recommendation(
                    scope.getSubject(), scope.getChapter(), scope.getCoveragePercent(),
                    scope.getAttentionNeeded(), priority));
        }
        recommendations.sort(Comparator.comparingInt(Recommendation::getPriority).reversed()
                .thenComparingInt(Recommendation::getCoveragePercent));
        return new ArrayList<>(recommendations.subList(
                0, Math.min(limit, recommendations.size())));
    }

    public static final class Recommendation {
        @NonNull private final String subject;
        @NonNull private final String chapter;
        private final int coveragePercent;
        private final int attentionNeeded;
        private final int priority;

        private Recommendation(@NonNull String subject, @NonNull String chapter,
                               int coveragePercent, int attentionNeeded, int priority) {
            this.subject = subject;
            this.chapter = chapter;
            this.coveragePercent = coveragePercent;
            this.attentionNeeded = attentionNeeded;
            this.priority = priority;
        }

        @NonNull public String getSubject() { return subject; }
        @NonNull public String getChapter() { return chapter; }
        public int getCoveragePercent() { return coveragePercent; }
        public int getAttentionNeeded() { return attentionNeeded; }
        public int getPriority() { return priority; }

        @NonNull
        public String buildParentMessage() {
            return subject + " — " + chapter + ": approved पुस्तक पृष्ठ के साथ revision कराएँ ("
                    + coveragePercent + "% grounded, " + attentionNeeded + " attention)।";
        }
    }
}
