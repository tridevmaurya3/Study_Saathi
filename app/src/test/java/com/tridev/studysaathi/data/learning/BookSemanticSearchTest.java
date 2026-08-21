package com.tridev.studysaathi.data.learning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class BookSemanticSearchTest {
    @Test public void ranksMeaningMatchAheadOfUnrelatedPage() {
        List<ExactBookPageCitationBuilder.PageReference> pages = Arrays.asList(
                page(7, "पौधों में भोजन", "प्रकाश संश्लेषण में सूर्य का प्रकाश उपयोग होता है"),
                page(8, "भिन्न", "अंश और हर से भिन्न बनती है")
        );

        List<ExactBookPageCitationBuilder.PageReference> result =
                BookSemanticSearch.findRelevantPages("प्रकाश संश्लेषण समझाओ", pages, 2);

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getPageNumber());
    }

    @Test public void titleMatchGetsPriorityAndLimitIsHonored() {
        List<ExactBookPageCitationBuilder.PageReference> pages = Arrays.asList(
                page(11, "Fractions", "A fraction represents equal parts"),
                page(12, "Practice", "Practice questions about fractions"),
                page(13, "Geometry", "Lines and angles")
        );

        List<ExactBookPageCitationBuilder.PageReference> result =
                BookSemanticSearch.findRelevantPages("What are fractions", pages, 1);

        assertEquals(1, result.size());
        assertEquals(11, result.get(0).getPageNumber());
    }

    @Test public void unrelatedQuestionReturnsNoPageInsteadOfGuessing() {
        List<ExactBookPageCitationBuilder.PageReference> result =
                BookSemanticSearch.findRelevantPages(
                        "भारत का संविधान", Arrays.asList(page(4, "Water", "Water cycle")), 3);
        assertTrue(result.isEmpty());
    }

    private static ExactBookPageCitationBuilder.PageReference page(
            int number, String title, String content) {
        return new ExactBookPageCitationBuilder.PageReference(number, title, content);
    }
}
