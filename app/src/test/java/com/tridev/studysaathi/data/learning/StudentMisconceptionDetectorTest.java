package com.tridev.studysaathi.data.learning;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StudentMisconceptionDetectorTest {
    @Test
    public void ordinaryQuestion_isNotFlagged() {
        StudentMisconceptionDetector.Detection detection =
                StudentMisconceptionDetector.inspect("Science", "प्रकाश संश्लेषण क्या है?");
        assertFalse(detection.shouldReview());
    }

    @Test
    public void studentClaim_isMarkedForReasoningReview() {
        StudentMisconceptionDetector.Detection detection =
                StudentMisconceptionDetector.inspect("Maths", "मेरा उत्तर 24 है, क्या यह सही है?");
        assertTrue(detection.shouldReview());
        assertEquals(StudentMisconceptionDetector.Type.STUDENT_REASONING,
                detection.getType());
    }

    @Test
    public void divisionByZeroClaim_isHighConfidenceReview() {
        StudentMisconceptionDetector.Detection detection =
                StudentMisconceptionDetector.inspect("Maths", "10 ÷ 0 = 0 है");
        assertTrue(detection.shouldReview());
        assertTrue(detection.isHighConfidence());
        assertEquals(StudentMisconceptionDetector.Type.DIVISION_BY_ZERO,
                detection.getType());
    }
}
