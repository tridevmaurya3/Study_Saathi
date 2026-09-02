package com.tridev.studysaathi.data.content.setup;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class SubjectContentSetupStatusTest {

    @Test
    public void hiddenSubjectKeepsSetupPaused() {
        SubjectContentSetupStatus.Result result =
                SubjectContentSetupStatus.resolve(
                        false,
                        "Mathematics",
                        12
                );

        assertEquals(
                SubjectContentSetupStatus.Step.HIDDEN,
                result.getStep()
        );
    }

    @Test
    public void missingBookRequestsBookSetup() {
        SubjectContentSetupStatus.Result result =
                SubjectContentSetupStatus.resolve(
                        true,
                        "  ",
                        0
                );

        assertEquals(
                SubjectContentSetupStatus.Step.ADD_BOOK,
                result.getStep()
        );
        assertEquals(
                "Add Book",
                result.getPrimaryActionLabel()
        );
    }

    @Test
    public void confirmedBookWithoutFreshCountContinuesSafely() {
        SubjectContentSetupStatus.Result result =
                SubjectContentSetupStatus.resolve(
                        true,
                        "Science Book",
                        0
                );

        assertEquals(
                SubjectContentSetupStatus.Step.CONTINUE_BOOK_SETUP,
                result.getStep()
        );
        assertEquals(
                "Continue Setup",
                result.getPrimaryActionLabel()
        );
    }

    @Test
    public void existingChaptersContinueMaterialSetup() {
        SubjectContentSetupStatus.Result result =
                SubjectContentSetupStatus.resolve(
                        true,
                        "Science Book",
                        8
                );

        assertEquals(
                SubjectContentSetupStatus.Step.CONTINUE_BOOK_SETUP,
                result.getStep()
        );
        assertEquals(
                "Continue Setup",
                result.getPrimaryActionLabel()
        );
        assertEquals(
                "8 chapters summary उपलब्ध है",
                result.getDescription()
        );
    }

    @Test
    public void negativeChapterCountStillUsesSafeContinuation() {
        assertEquals(
                SubjectContentSetupStatus.Step.CONTINUE_BOOK_SETUP,
                SubjectContentSetupStatus.resolve(
                        true,
                        "English Book",
                        -1
                ).getStep()
        );
    }
}
