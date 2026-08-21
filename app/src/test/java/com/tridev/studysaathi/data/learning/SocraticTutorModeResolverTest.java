package com.tridev.studysaathi.data.learning;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SocraticTutorModeResolverTest {
    @Test public void ordinaryQuestionKeepsDirectAnswerMode() {
        assertFalse(SocraticTutorModeResolver.resolve("प्रकाश संश्लेषण क्या है?").isGuided());
    }

    @Test public void hindiPracticeRequestEnablesGuidedDiscovery() {
        assertTrue(SocraticTutorModeResolver.resolve(
                "मुझे भिन्न का अभ्यास कराओ, उत्तर मत बताओ").isGuided());
    }

    @Test public void englishHintRequestEnablesGuidedDiscovery() {
        assertTrue(SocraticTutorModeResolver.resolve("Help me solve this, give me a hint").isGuided());
    }

    @Test public void explicitDirectAnswerOverridesGuidedWords() {
        assertFalse(SocraticTutorModeResolver.resolve(
                "Quiz बाद में, अभी सीधा उत्तर बता दो").isGuided());
    }

    @Test public void guidedModeContinuesForStudentFollowUp() {
        assertTrue(SocraticTutorModeResolver.resolve(
                "मुझे लगता है उत्तर 12 है",
                "Student: मुझे यह सवाल हिंट देकर हल कराओ").isGuided());
    }

    @Test public void directAnswerRequestExitsGuidedConversation() {
        assertFalse(SocraticTutorModeResolver.resolve(
                "अब सीधा जवाब बता दो",
                "Student: मुझे यह सवाल हिंट देकर हल कराओ").isGuided());
    }
}
