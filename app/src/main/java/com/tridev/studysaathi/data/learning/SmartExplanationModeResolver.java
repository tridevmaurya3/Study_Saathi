package com.tridev.studysaathi.data.learning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Resolves one answer-level explanation mode without changing the student's
 * saved language preference. The returned instruction is intentionally
 * bounded so a quick-action or typed request changes only the current answer.
 */
public final class SmartExplanationModeResolver {

    public enum Mode {
        DEFAULT,
        QUICK_ANSWER,
        UNDERSTAND,
        SIMPLER,
        EXAMPLE,
        DIAGRAM,
        EXAM_ANSWER,
        TEST_ME,
        HINDI_OVERRIDE,
        HINGLISH_OVERRIDE,
        ENGLISH_OVERRIDE
    }

    private SmartExplanationModeResolver() { }

    @NonNull
    public static Decision resolve(@Nullable String question) {
        String text = safe(question).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) return Decision.none();

        if (has(text,
                "explain in hinglish", "hinglish mein", "hinglish me",
                "हिंग्लिश", "हिंगलिश")) {
            return Decision.of(Mode.HINGLISH_OVERRIDE,
                    "CURRENT ANSWER LANGUAGE OVERRIDE: answer this turn in natural Hinglish. "
                            + "Mix easy Hindi with familiar English academic terms naturally; "
                            + "do not change the student's saved language preference.");
        }

        if (has(text,
                "explain in hindi", "hindi mein", "hindi me",
                "हिन्दी में", "हिंदी में")) {
            return Decision.of(Mode.HINDI_OVERRIDE,
                    "CURRENT ANSWER LANGUAGE OVERRIDE: answer this turn in clear, easy Hindi. "
                            + "Avoid unnecessary English words; preserve formulas, symbols and proper nouns. "
                            + "Do not change the student's saved language preference.");
        }

        if (has(text,
                "explain in english", "answer in english", "english mein",
                "अंग्रेजी में", "अंग्रेज़ी में")) {
            return Decision.of(Mode.ENGLISH_OVERRIDE,
                    "CURRENT ANSWER LANGUAGE OVERRIDE: answer this turn fully in simple English. "
                            + "Do not mix Hindi except where the student explicitly asks for a translation. "
                            + "Do not change the student's saved language preference.");
        }

        if (has(text,
                "exam answer", "exam में", "exam me", "परीक्षा उत्तर",
                "परीक्षा में लिख", "उत्तर लिखने लायक")) {
            return Decision.of(Mode.EXAM_ANSWER,
                    "EXAM ANSWER MODE: give a syllabus-bound, marks-friendly answer with a short opening, "
                            + "well-ordered points/steps, required formula or keywords, and a concise conclusion. "
                            + "Do not add unsupported book facts or page numbers.");
        }

        if (has(text,
                "test me", "मुझे टेस्ट करो", "quiz me", "क्विज",
                "एक सवाल पूछो", "practice question")) {
            return Decision.of(Mode.TEST_ME,
                    "TEST ME MODE: ask exactly one short concept-check question first and do not reveal "
                            + "the answer until the student responds. Keep it aligned to the supplied context.");
        }

        if (has(text,
                "diagram", "चित्र से", "आरेख", "flowchart", "visual")) {
            return Decision.of(Mode.DIAGRAM,
                    "DIAGRAM MODE: explain using a compact labelled text-safe diagram, flow or table when useful. "
                            + "Never invent labels that are not supported by the question or verified evidence.");
        }

        if (has(text,
                "example", "उदाहरण", "रोजमर्रा", "real life")) {
            return Decision.of(Mode.EXAMPLE,
                    "EXAMPLE MODE: explain the idea briefly, then give one simple age-appropriate example "
                            + "and explicitly connect the example back to the concept.");
        }

        if (has(text,
                "simpler", "और आसान", "easy way", "बहुत आसान",
                "simple language", "आसान भाषा")) {
            return Decision.of(Mode.SIMPLER,
                    "SIMPLER MODE: use shorter sentences, one idea at a time, minimal jargon, and one tiny check "
                            + "for understanding. Keep factual meaning unchanged.");
        }

        if (has(text,
                "let's understand", "lets understand", "समझते हैं",
                "concept समझाओ", "detail में समझ")) {
            return Decision.of(Mode.UNDERSTAND,
                    "UNDERSTAND MODE: teach from the core idea to the reason, then an example, then a one-line recap. "
                            + "Keep the level appropriate for the student's class.");
        }

        if (has(text,
                "quick answer", "short answer", "brief answer", "जल्दी बताओ",
                "संक्षेप में", "छोटा उत्तर")) {
            return Decision.of(Mode.QUICK_ANSWER,
                    "QUICK ANSWER MODE: answer directly in at most 3 concise points or 4 short sentences, "
                            + "while preserving any essential formula, unit or verified citation.");
        }

        return Decision.none();
    }

    private static boolean has(@NonNull String text, @NonNull String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    @NonNull
    private static String safe(@Nullable Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public static final class Decision {
        @NonNull private final Mode mode;
        @NonNull private final String promptInstruction;

        private Decision(@NonNull Mode mode, @NonNull String promptInstruction) {
            this.mode = mode;
            this.promptInstruction = promptInstruction;
        }

        @NonNull
        static Decision none() {
            return new Decision(Mode.DEFAULT, "");
        }

        @NonNull
        static Decision of(@NonNull Mode mode, @NonNull String instruction) {
            return new Decision(mode, instruction);
        }

        @NonNull public Mode getMode() { return mode; }
        @NonNull public String getPromptInstruction() { return promptInstruction; }
        public boolean hasMode() { return mode != Mode.DEFAULT && !promptInstruction.isEmpty(); }
    }
}
