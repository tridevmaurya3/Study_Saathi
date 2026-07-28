package com.tridev.studysaathi.data.ai;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OfflineSmartAnswerRouterTest {

    @Test
    public void arithmeticQuestion_isAnsweredOffline() {
        OfflineSmartAnswerRouter.RouteResult result =
                OfflineSmartAnswerRouter.tryCreateAnswer(
                        "Mathematics",
                        "12 + 8",
                        "English",
                        false
                );

        assertTrue(result.isHandled());
        assertTrue(result.getAnswerText().contains("20"));
    }

    @Test
    public void imageQuestion_isNotGuessedOffline() {
        OfflineSmartAnswerRouter.RouteResult result =
                OfflineSmartAnswerRouter.tryCreateAnswer(
                        "Mathematics",
                        "12 + 8",
                        "English",
                        true
                );

        assertFalse(result.isHandled());
    }

    @Test
    public void nonMathQuestion_isNotRoutedToMathSolver() {
        OfflineSmartAnswerRouter.RouteResult result =
                OfflineSmartAnswerRouter.tryCreateAnswer(
                        "Science",
                        "12 + 8",
                        "English",
                        false
                );

        assertFalse(result.isHandled());
    }
}
