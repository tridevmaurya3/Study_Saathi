package com.tridev.studysaathi.data.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class LowCoverageChapterRecommendationEngineTest {
    @Test public void lowestCoverageWithAttentionGetsPriority() {
        List<CitationCoverageHistoryStore.ScopeSummary> scopes = Arrays.asList(
                scope("Science", "Light", 10, 8, 2, 0),
                scope("Math", "Fractions", 10, 4, 4, 2));
        List<LowCoverageChapterRecommendationEngine.Recommendation> result =
                LowCoverageChapterRecommendationEngine.recommend(scopes, 2);
        assertEquals("Fractions", result.get(0).getChapter());
        assertTrue(result.get(0).buildParentMessage().contains("revision"));
    }

    @Test public void healthyChapterWithoutAttentionIsExcluded() {
        List<LowCoverageChapterRecommendationEngine.Recommendation> result =
                LowCoverageChapterRecommendationEngine.recommend(
                        Arrays.asList(scope("Science", "Plants", 5, 5, 0, 0)), 3);
        assertTrue(result.isEmpty());
    }

    @Test public void singleAnswerDoesNotCreatePrematureRecommendation() {
        List<LowCoverageChapterRecommendationEngine.Recommendation> result =
                LowCoverageChapterRecommendationEngine.recommend(
                        Arrays.asList(scope("Math", "Numbers", 1, 0, 1, 0)), 3);
        assertTrue(result.isEmpty());
    }

    private static CitationCoverageHistoryStore.ScopeSummary scope(
            String subject, String chapter, int reviewed, int grounded,
            int missing, int blocked) {
        return CitationCoverageHistoryStore.ScopeSummary.fromCounts(
                subject, chapter, reviewed, grounded, missing, blocked);
    }
}
