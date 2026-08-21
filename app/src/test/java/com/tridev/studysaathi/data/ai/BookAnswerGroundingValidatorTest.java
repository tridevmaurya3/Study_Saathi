package com.tridev.studysaathi.data.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BookAnswerGroundingValidatorTest {
    private static final String REFERENCE =
            "[[VERIFIED_BOOK_PAGE page=12 title=\"Fractions\"]]\nContent\n"
                    + "[[END_VERIFIED_BOOK_PAGE]]";

    @Test public void matchingApprovedCitationIsGrounded() {
        BookAnswerGroundingValidator.Result result =
                BookAnswerGroundingValidator.validate("उत्तर। 📖 पुस्तक पृष्ठ 12", REFERENCE);
        assertTrue(result.isGrounded());
        assertEquals(1, result.getCitedPageCount());
    }

    @Test public void inventedPageCitationIsBlocked() {
        BookAnswerGroundingValidator.Result result =
                BookAnswerGroundingValidator.validate("See book page 19", REFERENCE);
        assertTrue(result.hasUnsupportedCitation());
    }

    @Test public void availableEvidenceWithoutCitationNeedsCaution() {
        BookAnswerGroundingValidator.Result result =
                BookAnswerGroundingValidator.validate("भिन्न बराबर भाग दिखाती है।", REFERENCE);
        assertTrue(result.needsCitationCaution());
    }

    @Test public void noEvidenceDoesNotClaimGrounding() {
        BookAnswerGroundingValidator.Result result =
                BookAnswerGroundingValidator.validate("पृष्ठ 12", "general chapter content");
        assertFalse(result.isGrounded());
        assertEquals(BookAnswerGroundingValidator.Status.NO_EXACT_EVIDENCE,
                result.getStatus());
    }
}
