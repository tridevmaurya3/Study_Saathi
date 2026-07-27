package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;

import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.ThinkingConfig;
import com.google.firebase.ai.type.ThinkingLevel;

/**
 * Study Saathi के Gemini requests के लिए centralized
 * token-saving generation configuration।
 *
 * मुख्य उद्देश्य:
 *
 * 1. सामान्य school questions में unnecessary thinking कम करना।
 * 2. बहुत लंबे answers से token usage बचाना।
 * 3. केवल एक final answer generate करना।
 * 4. Thought summaries को response में शामिल न करना।
 * 5. भविष्य में AI configuration को एक ही स्थान से नियंत्रित करना।
 */
public final class StudyAiGenerationConfig {

    /**
     * एक सामान्य child-friendly answer के लिए पर्याप्त output limit।
     *
     * 1400 output tokens सामान्यतः:
     *
     * - Direct answer
     * - Step-by-step explanation
     * - Example
     * - Understanding-check question
     *
     * के लिए पर्याप्त हैं।
     */
    private static final int STANDARD_MAXIMUM_OUTPUT_TOKENS =
            1400;

    /**
     * केवल एक answer candidate चाहिए।
     */
    private static final int STANDARD_CANDIDATE_COUNT =
            1;

    private StudyAiGenerationConfig() {
        /*
         * Utility class का object नहीं बनाया जाना चाहिए।
         */
    }

    /**
     * सामान्य Study Saathi questions के लिए token-saving
     * Firebase AI GenerationConfig बनाता है।
     */
    @NonNull
    public static GenerationConfig createStandardConfig() {
        ThinkingConfig thinkingConfig =
                new ThinkingConfig.Builder()
                        /*
                         * Gemini 3.x models में thinking पूरी तरह बंद
                         * नहीं होती। LOW सामान्य educational questions
                         * में कम thinking और कम latency देता है।
                         */
                        .setThinkingLevel(
                                ThinkingLevel.LOW
                        )

                        /*
                         * Internal thought summary user को नहीं चाहिए।
                         * केवल final educational answer प्राप्त होगा।
                         */
                        .setIncludeThoughts(
                                false
                        )
                        .build();

        return GenerationConfig.builder()
                .setThinkingConfig(
                        thinkingConfig
                )

                /*
                 * एक request में केवल एक final answer।
                 */
                .setCandidateCount(
                        STANDARD_CANDIDATE_COUNT
                )

                /*
                 * बहुत लंबे और token-heavy answers को रोकता है।
                 */
                .setMaxOutputTokens(
                        STANDARD_MAXIMUM_OUTPUT_TOKENS
                )
                .build();
    }

    /**
     * Parent/Admin usage screen में configuration की जानकारी
     * दिखाने के लिए readable description।
     */
    @NonNull
    public static String getConfigurationDescription() {
        return "Thinking: LOW"
                + " • Output limit: "
                + STANDARD_MAXIMUM_OUTPUT_TOKENS
                + " tokens"
                + " • Candidates: "
                + STANDARD_CANDIDATE_COUNT;
    }

    public static int getMaximumOutputTokens() {
        return STANDARD_MAXIMUM_OUTPUT_TOKENS;
    }
}