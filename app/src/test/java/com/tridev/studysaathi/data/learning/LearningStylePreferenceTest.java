package com.tridev.studysaathi.data.learning;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LearningStylePreferenceTest {
    @Test public void ordinaryQuestionIsBalanced() {
        assertEquals(LearningStylePreference.Style.BALANCED,
                LearningStylePreference.detect("प्रकाश संश्लेषण क्या है?"));
    }

    @Test public void detectsHindiStepRequest() {
        assertEquals(LearningStylePreference.Style.STEP_BY_STEP,
                LearningStylePreference.detect("इसे स्टेप बाय स्टेप समझाओ"));
    }

    @Test public void detectsExampleAndVisualRequests() {
        assertEquals(LearningStylePreference.Style.EXAMPLE_DRIVEN,
                LearningStylePreference.detect("उदाहरण से समझाओ"));
        assertEquals(LearningStylePreference.Style.VISUAL,
                LearningStylePreference.detect("Explain this with a diagram"));
    }

    @Test public void detectsConciseAndBilingualRequests() {
        assertEquals(LearningStylePreference.Style.CONCISE,
                LearningStylePreference.detect("Give me a short answer"));
        assertEquals(LearningStylePreference.Style.BILINGUAL,
                LearningStylePreference.detect("हिंदी और अंग्रेजी दोनों भाषा में बताओ"));
    }
}
