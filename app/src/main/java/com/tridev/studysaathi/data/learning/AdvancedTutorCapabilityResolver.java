package com.tridev.studysaathi.data.learning;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Detects advanced learning requests and supplies strict, bounded tutor instructions. */
public final class AdvancedTutorCapabilityResolver {

    public enum Capability {
        DIAGRAM_TABLE,
        CHAPTER_SUMMARY,
        QUESTION_GENERATOR,
        ADAPTIVE_PRACTICE,
        HANDWRITTEN_CHECK,
        STEPWISE_EVALUATION,
        EXAM_SIMULATION,
        AUTOMATIC_RETEST,
        FLASHCARDS,
        READ_ALONG,
        VISUAL_ANSWER,
        PRONUNCIATION,
        EXACT_BOOK_CONTEXT,
        PHOTO_BOOK_CONTEXT
    }

    public static final class Decision {
        private final List<Capability> capabilities;
        private final String promptInstruction;

        private Decision(List<Capability> capabilities, String promptInstruction) {
            this.capabilities = Collections.unmodifiableList(capabilities);
            this.promptInstruction = promptInstruction;
        }

        @NonNull public List<Capability> getCapabilities() { return capabilities; }
        @NonNull public String getPromptInstruction() { return promptInstruction; }
        public boolean hasCapabilities() { return !capabilities.isEmpty(); }
        public boolean includes(Capability capability) { return capabilities.contains(capability); }
    }

    private AdvancedTutorCapabilityResolver() { }

    @NonNull
    public static Decision resolve(String question, boolean imageAttached) {
        String text = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
        List<Capability> found = new ArrayList<>();
        StringBuilder rules = new StringBuilder();

        ExactStudyContextResolver.Decision exactContext =
                ExactStudyContextResolver.resolve(question, imageAttached);
        addIf(found, rules, Capability.EXACT_BOOK_CONTEXT,
                exactContext.hasVerifiedExactContext(),
                exactContext.getPromptInstruction());

        addIf(found, rules, Capability.DIAGRAM_TABLE,
                imageAttached || has(text, "diagram", "चित्र", "आरेख", "table", "तालिका", "graph", "चार्ट"),
                "DIAGRAM/TABLE: inspect visible structure, headings, labels, units and relationships; never invent unreadable content.");
        addIf(found, rules, Capability.CHAPTER_SUMMARY,
                has(text, "summary", "summarise", "summarize", "सारांश", "chapter notes", "अध्याय का सार"),
                "CHAPTER SUMMARY: return key ideas, important terms, formula/facts, common mistakes and a 5-point revision recap.");
        addIf(found, rules, Capability.QUESTION_GENERATOR,
                has(text, "questions बनाओ", "प्रश्न बनाओ", "question generate", "practice questions", "quiz बनाओ"),
                "QUESTION GENERATOR: create syllabus-bound questions with an answer key kept after the questions.");
        addIf(found, rules, Capability.ADAPTIVE_PRACTICE,
                has(text, "practice", "अभ्यास", "difficulty", "कठिनाई", "level बढ़ाओ", "level घटाओ"),
                "ADAPTIVE PRACTICE: begin at the supplied learning level, ask one item at a time, and adjust the next item from the student's response.");
        addIf(found, rules, Capability.HANDWRITTEN_CHECK,
                imageAttached && has(text, "check", "जाँच", "जांच", "handwriting", "handwritten", "मेरी कॉपी", "उत्तर सही"),
                "HANDWRITTEN CHECK: transcribe only legible writing, mark uncertain words, compare method and result separately, and do not guess cropped work.");
        addIf(found, rules, Capability.STEPWISE_EVALUATION,
                has(text, "step", "चरण", "steps check", "solution check", "हल जाँच", "कहाँ गलती"),
                "STEP-WISE EVALUATION: label each step correct/needs-fix, identify the first error, explain why, then show only the corrected continuation.");
        addIf(found, rules, Capability.EXAM_SIMULATION,
                has(text, "mock test", "exam simulation", "परीक्षा", "टेस्ट लो", "test लो", "sample paper"),
                "EXAM SIMULATION: state marks and suggested time, present questions without answers, and evaluate only after submission.");
        addIf(found, rules, Capability.AUTOMATIC_RETEST,
                has(text, "retest", "re-test", "दोबारा टेस्ट", "फिर से पूछो", "गलत वाले प्रश्न"),
                "AUTOMATIC RE-TEST: retest missed concepts with changed numbers/wording; do not repeat the memorisable answer verbatim.");
        addIf(found, rules, Capability.FLASHCARDS,
                has(text, "flashcard", "फ्लैशकार्ड", "cards बनाओ", "कार्ड बनाओ"),
                "FLASHCARDS: output compact Front | Back pairs; one fact or concept per card, including misconception cards when useful.");
        addIf(found, rules, Capability.READ_ALONG,
                has(text, "read along", "साथ पढ़ो", "पढ़कर सुनाओ", "line by line", "लाइन बाय लाइन"),
                "READ-ALONG: split into short speakable lines, pause with a comprehension check after each small section.");
        addIf(found, rules, Capability.VISUAL_ANSWER,
                has(text, "visual answer", "visual card", "दृश्य", "flowchart", "mind map", "माइंड मैप"),
                "VISUAL ANSWER: use a text-safe flow, comparison table or labelled ASCII layout; preserve accessibility in plain text.");
        addIf(found, rules, Capability.PRONUNCIATION,
                has(text, "pronunciation", "उच्चारण", "कैसे बोलें", "कैसे पढ़ें", "speak this"),
                "PRONUNCIATION: provide syllable breaks, simple Hindi sound guidance, stress cue and one short repeat-after-me practice line.");

        return new Decision(found, rules.toString().trim());
    }

    private static void addIf(List<Capability> found, StringBuilder rules,
                              Capability capability, boolean condition, String instruction) {
        if (!condition) return;
        found.add(capability);
        if (rules.length() > 0) rules.append('\n');
        rules.append("- ").append(instruction);
    }

    private static boolean has(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }
}
