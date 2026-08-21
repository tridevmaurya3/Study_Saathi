package com.tridev.studysaathi.data.learning;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class WhiteboardMathEvaluatorTest {
    @Test public void solvesBasicAndPrecedenceExpressions() {
        assertEquals("4", WhiteboardMathEvaluator.evaluate("2+2"));
        assertEquals("14", WhiteboardMathEvaluator.evaluate("2+3×4="));
        assertEquals("20", WhiteboardMathEvaluator.evaluate("(2+3)*4"));
    }
    @Test public void rejectsUnsafeOrInvalidInput() {
        assertEquals("", WhiteboardMathEvaluator.evaluate("solve 2+2"));
        assertEquals("", WhiteboardMathEvaluator.evaluate("8/0"));
    }
}
