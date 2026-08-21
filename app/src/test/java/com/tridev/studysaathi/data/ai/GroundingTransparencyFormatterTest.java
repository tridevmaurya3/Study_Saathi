package com.tridev.studysaathi.data.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GroundingTransparencyFormatterTest {
    private static final String TWO_PAGES =
            "[[VERIFIED_BOOK_PAGE page=12]] A [[END_VERIFIED_BOOK_PAGE]]\n"
                    + "[[VERIFIED_BOOK_PAGE page=14]] B [[END_VERIFIED_BOOK_PAGE]]";

    @Test public void groundedAnswerShowsPagesAndCoverage() {
        BookAnswerGroundingValidator.Result result =
                BookAnswerGroundingValidator.validate(
                        "उत्तर 📖 पुस्तक पृष्ठ 12 और 📖 पुस्तक पृष्ठ 14", TWO_PAGES);
        String text = GroundingTransparencyFormatter.format(result, "Hindi");
        assertTrue(text.contains("पृष्ठ 12, 14"));
        assertTrue(text.contains("2/2"));
    }

    @Test public void missingCitationShowsZeroCoverage() {
        BookAnswerGroundingValidator.Result result =
                BookAnswerGroundingValidator.validate("उत्तर", TWO_PAGES);
        String text = GroundingTransparencyFormatter.format(result, "Hindi");
        assertTrue(text.contains("exact citation नहीं मिला"));
        assertTrue(text.contains("0/2"));
    }

    @Test public void noEvidenceProducesNoDisclosure() {
        BookAnswerGroundingValidator.Result result =
                BookAnswerGroundingValidator.validate("उत्तर", "");
        assertEquals("", GroundingTransparencyFormatter.format(result, "English"));
    }
}
