package com.tridev.studysaathi.data.learning;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmartExplanationModeResolverTest {

    @Test
    public void hinglishOverrideIsOneTurnMode() {
        SmartExplanationModeResolver.Decision decision =
                SmartExplanationModeResolver.resolve(
                        "Explain in Hinglish please"
                );

        assertTrue(decision.hasMode());
        assertEquals(
                SmartExplanationModeResolver.Mode.HINGLISH_OVERRIDE,
                decision.getMode()
        );
        assertTrue(decision.getPromptInstruction()
                .contains("do not change the student's saved language preference"));
    }

    @Test
    public void examAnswerModeIsDetected() {
        SmartExplanationModeResolver.Decision decision =
                SmartExplanationModeResolver.resolve(
                        "इसे exam में लिखने लायक answer बनाओ"
                );

        assertEquals(
                SmartExplanationModeResolver.Mode.EXAM_ANSWER,
                decision.getMode()
        );
        assertTrue(decision.hasMode());
    }

    @Test
    public void genericQuestionKeepsDefaultMode() {
        SmartExplanationModeResolver.Decision decision =
                SmartExplanationModeResolver.resolve(
                        "What is photosynthesis?"
                );

        assertEquals(
                SmartExplanationModeResolver.Mode.DEFAULT,
                decision.getMode()
        );
        assertFalse(decision.hasMode());
    }
}
