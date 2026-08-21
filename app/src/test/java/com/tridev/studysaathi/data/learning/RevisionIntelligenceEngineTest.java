package com.tridev.studysaathi.data.learning;

import org.junit.Test;

import static org.junit.Assert.*;

public class RevisionIntelligenceEngineTest {
    @Test public void mistakesReturnToTomorrowAndNotebook() {
        RevisionIntelligenceEngine.Result result =
                RevisionIntelligenceEngine.evaluate(0, 2, 1, 30);
        assertEquals(1, result.getIntervalDays());
        assertTrue(result.shouldAddToMistakeNotebook());
        assertTrue(result.getPriorityScore() >= 70);
    }

    @Test public void masteryExpandsIntervalAndReducesRisk() {
        RevisionIntelligenceEngine.Result beginner =
                RevisionIntelligenceEngine.evaluate(1, 0, 1, 70);
        RevisionIntelligenceEngine.Result mastered =
                RevisionIntelligenceEngine.evaluate(6, 0, 1, 95);
        assertTrue(mastered.getIntervalDays() > beginner.getIntervalDays());
        assertTrue(mastered.getForgettingRiskPercent() < beginner.getForgettingRiskPercent());
        assertFalse(mastered.shouldAddToMistakeNotebook());
    }
}
