package com.tridev.studysaathi.data.learning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/** Pure, conservative detector for an explicitly requested explanation style. */
public final class LearningStylePreference {
    private LearningStylePreference() { }

    @NonNull
    public static Style detect(@Nullable String question) {
        String value = question == null ? ""
                : question.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (containsAny(value, "छोटा जवाब", "संक्षेप", "कम शब्द", "short answer",
                "briefly", "concise")) return Style.CONCISE;
        if (containsAny(value, "हिंदी और अंग्रेजी", "दोनों भाषा", "bilingual",
                "hindi and english", "english and hindi")) return Style.BILINGUAL;
        if (containsAny(value, "चित्र से", "डायग्राम", "visual", "diagram", "flowchart",
                "चार्ट से")) return Style.VISUAL;
        if (containsAny(value, "उदाहरण से", "example से", "example देकर",
                "with an example", "real life example")) return Style.EXAMPLE_DRIVEN;
        if (containsAny(value, "step by step", "स्टेप बाय स्टेप", "एक-एक कदम",
                "हर स्टेप", "पूरा तरीका")) return Style.STEP_BY_STEP;
        return Style.BALANCED;
    }

    private static boolean containsAny(@NonNull String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }

    public enum Style {
        BALANCED("Balanced", "संतुलित शैली",
                "Use a balanced class-level explanation with clear structure."),
        STEP_BY_STEP("StepByStep", "स्टेप-बाय-स्टेप",
                "Break the explanation into numbered, small sequential steps."),
        EXAMPLE_DRIVEN("ExampleDriven", "उदाहरण से सीखना",
                "Lead with one familiar real-life example, then connect it to the concept."),
        VISUAL("Visual", "दृश्य शैली",
                "Use a compact text diagram, spatial description, table, or visual analogy when useful; never claim an image was shown if none exists."),
        CONCISE("Concise", "संक्षिप्त उत्तर",
                "Keep the answer brief while retaining the essential reasoning and safety details."),
        BILINGUAL("Bilingual", "हिंदी + English",
                "Explain mainly in the preferred language and include key terms in both Hindi and English." );

        @NonNull private final String requestValue;
        @NonNull private final String displayLabel;
        @NonNull private final String promptInstruction;

        Style(String requestValue, String displayLabel, String promptInstruction) {
            this.requestValue = requestValue;
            this.displayLabel = displayLabel;
            this.promptInstruction = promptInstruction;
        }

        @NonNull public String getRequestValue() { return requestValue; }
        @NonNull public String getDisplayLabel() { return displayLabel; }
        @NonNull public String getPromptInstruction() { return promptInstruction; }

        @NonNull public static Style fromRequestValue(@Nullable String value) {
            if (value != null) for (Style style : values()) {
                if (style.requestValue.equalsIgnoreCase(value.trim())) return style;
            }
            return BALANCED;
        }
    }
}
