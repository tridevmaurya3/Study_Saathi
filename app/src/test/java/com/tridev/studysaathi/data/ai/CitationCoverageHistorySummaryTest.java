package com.tridev.studysaathi.data.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CitationCoverageHistorySummaryTest {
    @Test public void calculatesCoverageAndAttentionCounts() {
        CitationCoverageHistoryStore.Summary summary =
                CitationCoverageHistoryStore.Summary.fromCounts(10, 7, 2, 1);
        assertEquals(70, summary.getCoveragePercent());
        assertEquals(3, summary.getAttentionNeeded());
    }

    @Test public void emptyHistoryHasZeroCoverage() {
        CitationCoverageHistoryStore.Summary summary =
                CitationCoverageHistoryStore.Summary.fromCounts(0, 0, 0, 0);
        assertEquals(0, summary.getCoveragePercent());
        assertEquals(0, summary.getAttentionNeeded());
    }

    @Test public void scopeSummaryKeepsSubjectChapterAndCoverage() {
        CitationCoverageHistoryStore.ScopeSummary scope =
                CitationCoverageHistoryStore.ScopeSummary.fromCounts(
                        "Science", "Light", 5, 4, 1, 0);
        assertEquals("Science", scope.getSubject());
        assertEquals("Light", scope.getChapter());
        assertEquals(80, scope.getCoveragePercent());
        assertEquals(1, scope.getAttentionNeeded());
    }
}
