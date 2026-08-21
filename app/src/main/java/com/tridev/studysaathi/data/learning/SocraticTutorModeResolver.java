package com.tridev.studysaathi.data.learning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Selects guided-discovery teaching only when the learner clearly asks to practise or for a hint.
 * Direct-answer requests remain unchanged so existing tutor behaviour stays backward-compatible.
 */
public final class SocraticTutorModeResolver {
    private static final Pattern GUIDED_INTENT = Pattern.compile(
            ".*(quiz me|test me|ask me|give me a hint|hint please|help me solve|"
                    + "do not tell.*answer|don't tell.*answer|without.*answer|"
                    + "मेरा टेस्ट|मुझसे सवाल|मुझे सवाल|क्विज|संकेत दो|हिंट|"
                    + "हल करने में मदद|उत्तर मत बताओ|जवाब मत बताओ|अभ्यास कराओ).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DIRECT_INTENT = Pattern.compile(
            ".*(direct answer|just answer|tell me the answer|show.*solution|solve it|"
                    + "सीधा उत्तर|सीधा जवाब|उत्तर बता|जवाब बता|पूरा हल|हल करके).*",
            Pattern.CASE_INSENSITIVE);

    private SocraticTutorModeResolver() { }

    @NonNull
    public static Decision resolve(@Nullable String question) {
        return resolve(question, "");
    }

    /** Keeps an active guided conversation going while allowing a current direct-answer request to exit. */
    @NonNull
    public static Decision resolve(@Nullable String question, @Nullable String conversationContext) {
        String normalized = normalize(question);
        if (normalized.isEmpty() || DIRECT_INTENT.matcher(normalized).matches()) {
            return Decision.direct();
        }
        String normalizedContext = normalize(conversationContext);
        return (GUIDED_INTENT.matcher(normalized).matches()
                || GUIDED_INTENT.matcher(normalizedContext).matches())
                ? Decision.guided()
                : Decision.direct();
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public enum Mode { DIRECT_ANSWER, GUIDED_DISCOVERY }

    public static final class Decision {
        @NonNull private final Mode mode;

        private Decision(@NonNull Mode mode) { this.mode = mode; }

        @NonNull static Decision direct() { return new Decision(Mode.DIRECT_ANSWER); }
        @NonNull static Decision guided() { return new Decision(Mode.GUIDED_DISCOVERY); }

        public boolean isGuided() { return mode == Mode.GUIDED_DISCOVERY; }
        @NonNull public Mode getMode() { return mode; }
        @NonNull public String getDisplayLabel() {
            return isGuided() ? "प्रश्नों से सीखें" : "सीधा उत्तर";
        }

        @NonNull
        public String getPromptInstruction() {
            if (!isGuided()) return "";
            return "Use Socratic guided-discovery mode. Do not reveal the final answer immediately. "
                    + "Start by briefly acknowledging the goal, then ask exactly one short, class-level "
                    + "guiding question or give one small hint. Wait for the student's next response before "
                    + "the next step. Use the student's answer to choose the next question. If they struggle, "
                    + "reduce the step and give a concrete hint; after two unsuccessful attempts, explain that "
                    + "step and continue. Praise effort specifically, never shame guessing, and never withhold "
                    + "urgent safety information. If the student explicitly asks for the direct answer later, "
                    + "leave Socratic mode and answer normally.";
        }
    }
}
