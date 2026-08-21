package com.tridev.studysaathi.data.learning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/** Conservative local detector: flags reasoning for review without declaring the child wrong. */
public final class StudentMisconceptionDetector {
    private static final Pattern FRACTION_ADD = Pattern.compile(
            ".*\\d+\\s*/\\s*\\d+\\s*\\+\\s*\\d+\\s*/\\s*\\d+\\s*(=|is|है).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DIVIDE_ZERO = Pattern.compile(
            ".*(/|÷|divided by|भाग)\\s*0\\s*(=|is|है).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STUDENT_ASSERTION = Pattern.compile(
            ".*(i think|i believe|my answer|i got|मुझे लगता|मेरा उत्तर|मेरा जवाब|मैंने निकाला|क्या यह सही).*",
            Pattern.CASE_INSENSITIVE);

    private StudentMisconceptionDetector() { }

    @NonNull
    public static Detection inspect(@Nullable String subject, @NonNull String question) {
        String normalized = question.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) return Detection.none();

        if (DIVIDE_ZERO.matcher(normalized).matches()) {
            return Detection.review(Type.DIVISION_BY_ZERO,
                    "शून्य से भाग की सोच जाँचें",
                    "Check the student's claimed result involving division by zero. Explain why division by zero is undefined, without shaming the student.",
                    true);
        }
        if (FRACTION_ADD.matcher(normalized).matches()) {
            return Detection.review(Type.FRACTION_OPERATION,
                    "भिन्न जोड़ने का तरीका जाँचें",
                    "Verify the student's fraction-addition method. If denominators differ, explicitly show the common-denominator step and identify the exact step needing correction.",
                    false);
        }
        if (containsAny(normalized, "sun revolves around earth", "सूर्य पृथ्वी के चारों ओर")) {
            return Detection.review(Type.SCIENCE_MODEL,
                    "विज्ञान concept की जाँच",
                    "Check the stated Sun-Earth motion model and gently correct the exact misconception with a simple spatial example.",
                    true);
        }
        if (containsAny(normalized, "plants get food from soil", "पौधे भोजन मिट्टी से")) {
            return Detection.review(Type.SCIENCE_MODEL,
                    "पौधों के भोजन की सोच जाँचें",
                    "Distinguish minerals/water absorbed from soil from food made by photosynthesis. Correct only if the student actually made that claim.",
                    true);
        }
        if (STUDENT_ASSERTION.matcher(normalized).matches()) {
            return Detection.review(Type.STUDENT_REASONING,
                    "विद्यार्थी के reasoning की जाँच",
                    "The student supplied a possible answer or belief. Verify it step-by-step. If incorrect, name the first incorrect step, explain why, then show the corrected step.",
                    false);
        }
        return Detection.none();
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }

    public enum Type {
        NONE, STUDENT_REASONING, FRACTION_OPERATION, DIVISION_BY_ZERO, SCIENCE_MODEL
    }

    public static final class Detection {
        @NonNull private final Type type;
        @NonNull private final String displayLabel;
        @NonNull private final String promptInstruction;
        private final boolean highConfidence;

        private Detection(@NonNull Type type, @NonNull String displayLabel,
                          @NonNull String promptInstruction, boolean highConfidence) {
            this.type = type;
            this.displayLabel = displayLabel;
            this.promptInstruction = promptInstruction;
            this.highConfidence = highConfidence;
        }

        @NonNull static Detection none() {
            return new Detection(Type.NONE, "", "", false);
        }

        @NonNull static Detection review(Type type, String label,
                                         String instruction, boolean highConfidence) {
            return new Detection(type, label, instruction, highConfidence);
        }

        public boolean shouldReview() { return type != Type.NONE; }
        public boolean isHighConfidence() { return highConfidence; }
        @NonNull public Type getType() { return type; }
        @NonNull public String getDisplayLabel() { return displayLabel; }
        @NonNull public String getPromptInstruction() { return promptInstruction; }
        @NonNull public String getRequestContext() {
            if (!shouldReview()) return "";
            return type.name() + " | " + promptInstruction;
        }
    }
}
