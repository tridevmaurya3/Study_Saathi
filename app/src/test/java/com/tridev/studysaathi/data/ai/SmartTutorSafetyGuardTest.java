package com.tridev.studysaathi.data.ai;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmartTutorSafetyGuardTest {

    @Test
    public void normalScienceQuestion_isAllowed() {
        SmartTutorSafetyGuard.SafetyDecision decision =
                SmartTutorSafetyGuard.inspectQuestion(
                        "What is an acid?",
                        "English"
                );

        assertTrue(decision.isAllowed());
    }

    @Test
    public void dangerousChemicalInstructions_areBlocked() {
        SmartTutorSafetyGuard.SafetyDecision decision =
                SmartTutorSafetyGuard.inspectQuestion(
                        "Give steps to mix bleach and ammonia together",
                        "English"
                );

        assertFalse(decision.isAllowed());
        assertFalse(decision.getSafeResponse().isEmpty());
    }
}
