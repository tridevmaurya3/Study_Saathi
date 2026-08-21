package com.tridev.studysaathi.data.learning;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ExactBookPageCitationBuilderTest {
    @Test public void emitsOnlySuppliedExactPageNumbers() {
        String value = ExactBookPageCitationBuilder.build(Arrays.asList(
                new ExactBookPageCitationBuilder.PageReference(12, "Fractions", "भागों का परिचय"),
                new ExactBookPageCitationBuilder.PageReference(14, "Examples", "एक सरल उदाहरण")
        ));
        assertTrue(value.contains("page=12"));
        assertTrue(value.contains("page=14"));
        assertFalse(value.contains("page=13"));
    }

    @Test public void missingPageNumberNeverCreatesCitation() {
        String value = ExactBookPageCitationBuilder.build(Collections.singletonList(
                new ExactBookPageCitationBuilder.PageReference(0, "Unknown", "कुछ सामग्री")
        ));
        assertTrue(value.isEmpty());
    }
}
