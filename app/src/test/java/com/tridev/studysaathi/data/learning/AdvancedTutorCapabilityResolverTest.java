package com.tridev.studysaathi.data.learning;

import org.junit.Test;

import static org.junit.Assert.*;

public class AdvancedTutorCapabilityResolverTest {
    @Test public void detectsCombinedExamAndAdaptivePractice() {
        AdvancedTutorCapabilityResolver.Decision decision =
                AdvancedTutorCapabilityResolver.resolve("मेरा adaptive practice mock test लो", false);
        assertTrue(decision.includes(AdvancedTutorCapabilityResolver.Capability.ADAPTIVE_PRACTICE));
        assertTrue(decision.includes(AdvancedTutorCapabilityResolver.Capability.EXAM_SIMULATION));
        assertTrue(decision.getPromptInstruction().contains("without answers"));
    }

    @Test public void imageEnablesDiagramButNotHandwritingWithoutCheckIntent() {
        AdvancedTutorCapabilityResolver.Decision decision =
                AdvancedTutorCapabilityResolver.resolve("इसे समझाओ", true);
        assertTrue(decision.includes(AdvancedTutorCapabilityResolver.Capability.DIAGRAM_TABLE));
        assertFalse(decision.includes(AdvancedTutorCapabilityResolver.Capability.HANDWRITTEN_CHECK));
    }

    @Test public void detectsRevisionContentModes() {
        AdvancedTutorCapabilityResolver.Decision decision =
                AdvancedTutorCapabilityResolver.resolve("chapter summary और flashcards बनाओ", false);
        assertTrue(decision.includes(AdvancedTutorCapabilityResolver.Capability.CHAPTER_SUMMARY));
        assertTrue(decision.includes(AdvancedTutorCapabilityResolver.Capability.FLASHCARDS));
    }
}
