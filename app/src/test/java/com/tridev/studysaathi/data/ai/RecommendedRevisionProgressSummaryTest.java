package com.tridev.studysaathi.data.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RecommendedRevisionProgressSummaryTest {
    @Test public void calculatesCompletionAndPendingCounts() {
        RecommendedRevisionProgressStore.Summary summary =
                RecommendedRevisionProgressStore.Summary.fromCounts(5, 3);
        assertEquals(3, summary.getCompleted());
        assertEquals(2, summary.getPending());
        assertEquals(60, summary.getCompletionPercent());
    }

    @Test public void completionCannotExceedStarts() {
        RecommendedRevisionProgressStore.Summary summary =
                RecommendedRevisionProgressStore.Summary.fromCounts(2, 8);
        assertEquals(2, summary.getCompleted());
        assertEquals(100, summary.getCompletionPercent());
    }
}
