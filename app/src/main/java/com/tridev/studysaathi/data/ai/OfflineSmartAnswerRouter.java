package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

/**
 * Offline-first Mathematics answer router.
 *
 * Stage 5 preserves the existing deterministic solvers and public API while
 * replacing the old duplicated bilingual output with one natural Hinglish
 * explanation when Hinglish is selected.
 */
public final class OfflineSmartAnswerRouter {

    public static final String SOURCE_OFFLINE_DIVISIBILITY =
            "OFFLINE_DIVISIBILITY";

    public static final String SOURCE_OFFLINE_BASIC_MATH =
            "OFFLINE_BASIC_MATH";

    public static final String SOURCE_NOT_HANDLED =
            "NOT_HANDLED";

    @NonNull
    private static final String[] HINGLISH_OVERRIDE_PHRASES = {
            "in hinglish", "hinglish mein", "hinglish me", "हिंग्लिश में",
            "हिंग्लिश मे", "hindi english mix", "hindi + english",
            "hindi+english", "हिंदी इंग्लिश मिक्स", "हिन्दी इंग्लिश मिक्स"
    };

    @NonNull
    private static final String[] ENGLISH_OVERRIDE_PHRASES = {
            "in english", "english mein", "english me", "english please",
            "अंग्रेजी में", "अंग्रेज़ी में", "इंग्लिश में", "इंग्लिश मे"
    };

    @NonNull
    private static final String[] HINDI_OVERRIDE_PHRASES = {
            "in hindi", "hindi mein", "hindi me", "hindi please",
            "हिंदी में", "हिन्दी में", "हिंदी मे", "हिन्दी मे"
    };

    private OfflineSmartAnswerRouter() { }

    @NonNull
    public static RouteResult tryCreateAnswer(
            @Nullable String subjectName,
            @Nullable String question,
            @Nullable String explanationLanguage,
            boolean imageAttached
    ) {
        String safeSubjectName = safeText(subjectName);
        String safeQuestion = safeText(question);

        if (safeQuestion.isEmpty()) {
            return RouteResult.notHandled("Question is empty.");
        }

        /*
         * Image questions continue through the existing Gemini/image path.
         * We never guess visual information in the deterministic router.
         */
        if (imageAttached) {
            return RouteResult.notHandled(
                    "An original question image is attached."
            );
        }

        if (!isMathematicsSubject(safeSubjectName)) {
            return RouteResult.notHandled(
                    "Selected subject is not Mathematics."
            );
        }

        LanguageMode languageMode =
                resolveLanguageMode(explanationLanguage, safeQuestion);

        /*
         * Keep the existing deterministic divisibility solver. For Hinglish,
         * ask it for a deterministic Hindi form and rebuild only the display
         * wording from its structured result; the calculation is untouched.
         */
        String divisibilitySolverLanguage =
                languageMode == LanguageMode.ENGLISH
                        ? "English"
                        : "Hindi";

        OfflineDivisibilitySolver.SolveResult divisibilitySolveResult =
                OfflineDivisibilitySolver.trySolve(
                        safeQuestion,
                        divisibilitySolverLanguage
                );

        if (divisibilitySolveResult.isSolved()) {
            String divisibilityAnswer;

            if (languageMode == LanguageMode.HINGLISH) {
                divisibilityAnswer =
                        createHinglishDivisibilityAnswer(
                                divisibilitySolveResult
                        );
            } else {
                divisibilityAnswer =
                        safeText(divisibilitySolveResult.getAnswerText());
            }

            if (!divisibilityAnswer.isEmpty()) {
                return RouteResult.handled(
                        divisibilityAnswer,
                        SOURCE_OFFLINE_DIVISIBILITY,
                        null
                );
            }
        }

        OfflineBasicMathSolver.SolveResult basicMathSolveResult =
                OfflineBasicMathSolver.trySolve(safeQuestion);

        if (!basicMathSolveResult.isSolved()) {
            return RouteResult.notHandled(
                    createCombinedDiagnosticReason(
                            divisibilitySolveResult.getDiagnosticReason(),
                            basicMathSolveResult.getDiagnosticReason()
                    )
            );
        }

        String finalAnswer;
        switch (languageMode) {
            case ENGLISH:
                finalAnswer =
                        createEnglishBasicMathAnswer(basicMathSolveResult);
                break;

            case HINDI:
                finalAnswer =
                        safeText(basicMathSolveResult.getAnswerText());
                break;

            case HINGLISH:
            default:
                finalAnswer =
                        createHinglishBasicMathAnswer(basicMathSolveResult);
                break;
        }

        return RouteResult.handled(
                finalAnswer,
                SOURCE_OFFLINE_BASIC_MATH,
                basicMathSolveResult.getConfidence()
        );
    }

    private static boolean isMathematicsSubject(@NonNull String subjectName) {
        String normalizedSubjectName = normalizeText(subjectName);

        return normalizedSubjectName.contains("mathematics")
                || normalizedSubjectName.contains("maths")
                || normalizedSubjectName.equals("math")
                || normalizedSubjectName.contains("गणित")
                || normalizedSubjectName.contains("अंकगणित");
    }

    @NonNull
    private static String createEnglishBasicMathAnswer(
            @NonNull OfflineBasicMathSolver.SolveResult solveResult
    ) {
        String expression = solveResult.getExpression();
        String resultDisplayValue =
                formatBasicMathValue(
                        solveResult.getResultValue(),
                        solveResult.getCurrencySymbol()
                );

        StringBuilder answerBuilder = new StringBuilder();
        if (!safeText(expression).isEmpty()) {
            answerBuilder.append(expression).append("\n\n");
        }

        OfflineBasicMathSolver.Operation operation = solveResult.getOperation();

        if (operation == null) {
            return answerBuilder
                    .append("Therefore, the answer is ")
                    .append(resultDisplayValue)
                    .append(".")
                    .toString();
        }

        switch (operation) {
            case ADDITION:
                answerBuilder.append("Therefore, the total is ")
                        .append(resultDisplayValue)
                        .append(".");
                break;

            case SUBTRACTION:
                if (!solveResult.getCurrencySymbol().isEmpty()) {
                    answerBuilder.append("Therefore, ")
                            .append(resultDisplayValue)
                            .append(" remains.");
                } else {
                    answerBuilder.append("Therefore, the remaining value is ")
                            .append(resultDisplayValue)
                            .append(".");
                }
                break;

            case MULTIPLICATION:
                answerBuilder.append("Therefore, the product is ")
                        .append(resultDisplayValue)
                        .append(".");
                break;

            case DIVISION:
                answerBuilder.append("Therefore, the quotient is ")
                        .append(resultDisplayValue)
                        .append(".");
                break;

            default:
                answerBuilder.append("Therefore, the answer is ")
                        .append(resultDisplayValue)
                        .append(".");
                break;
        }

        return answerBuilder.toString();
    }

    @NonNull
    private static String createHinglishBasicMathAnswer(
            @NonNull OfflineBasicMathSolver.SolveResult solveResult
    ) {
        String expression = safeText(solveResult.getExpression());
        String resultDisplayValue =
                formatBasicMathValue(
                        solveResult.getResultValue(),
                        solveResult.getCurrencySymbol()
                );

        StringBuilder answer = new StringBuilder();
        if (!expression.isEmpty()) {
            answer.append(expression).append("\n\n");
        }

        OfflineBasicMathSolver.Operation operation = solveResult.getOperation();
        if (operation == null) {
            return answer.append("इसलिए final answer ")
                    .append(resultDisplayValue)
                    .append(" है.")
                    .toString();
        }

        switch (operation) {
            case ADDITION:
                answer.append("इस calculation में total ")
                        .append(resultDisplayValue)
                        .append(" है.");
                break;

            case SUBTRACTION:
                answer.append("इस calculation के बाद ")
                        .append(resultDisplayValue)
                        .append(" remaining है.");
                break;

            case MULTIPLICATION:
                answer.append("इसलिए product ")
                        .append(resultDisplayValue)
                        .append(" है.");
                break;

            case DIVISION:
                answer.append("इसलिए quotient ")
                        .append(resultDisplayValue)
                        .append(" है.");
                break;

            default:
                answer.append("इसलिए final answer ")
                        .append(resultDisplayValue)
                        .append(" है.");
                break;
        }

        return answer.toString();
    }

    @NonNull
    private static String createHinglishDivisibilityAnswer(
            @NonNull OfflineDivisibilitySolver.SolveResult solveResult
    ) {
        BigInteger divisor = solveResult.getDivisor();

        if (solveResult.isRuleAnswer()) {
            if (divisor == null) {
                return "";
            }
            return createHinglishDivisibilityRule(divisor);
        }

        BigInteger dividend = solveResult.getDividend();
        BigInteger remainder = solveResult.getRemainder();

        if (dividend == null || divisor == null || remainder == null) {
            return "";
        }

        StringBuilder answer = new StringBuilder();
        answer.append(dividend)
                .append(" को ")
                .append(divisor)
                .append(" से divide करने पर remainder ")
                .append(remainder)
                .append(" आता है.\n\n");

        if (solveResult.isDivisible()) {
            answer.append("Remainder 0 है, इसलिए यह number ")
                    .append(divisor)
                    .append(" से completely divisible है.");
        } else {
            answer.append("Remainder 0 नहीं है, इसलिए यह number ")
                    .append(divisor)
                    .append(" से completely divisible नहीं है.");
        }

        return answer.toString();
    }

    @NonNull
    private static String createHinglishDivisibilityRule(
            @NonNull BigInteger divisor
    ) {
        int divisorValue =
                divisor.bitLength() < 31
                        ? divisor.intValue()
                        : -1;

        switch (divisorValue) {
            case 2:
                return "2 का divisibility rule:\nLast digit 0, 2, 4, 6 या 8 हो, तो number 2 से divisible होता है.";

            case 3:
                return "3 का divisibility rule:\nसभी digits का sum निकालें. अगर digit sum 3 से divisible है, तो पूरा number भी 3 से divisible होगा.";

            case 4:
                return "4 का divisibility rule:\nLast two digits से बने number को check करें. वह 4 से divisible हो, तो पूरा number भी 4 से divisible होगा.";

            case 5:
                return "5 का divisibility rule:\nLast digit 0 या 5 होना चाहिए.";

            case 6:
                return "6 का divisibility rule:\nNumber को 2 और 3 दोनों से divisible होना चाहिए.";

            case 8:
                return "8 का divisibility rule:\nLast three digits से बने number को check करें. वह 8 से divisible हो, तो पूरा number भी 8 से divisible होगा.";

            case 9:
                return "9 का divisibility rule:\nसभी digits का sum निकालें. अगर digit sum 9 से divisible है, तो पूरा number भी 9 से divisible होगा.";

            case 10:
                return "10 का divisibility rule:\nLast digit 0 होना चाहिए.";

            case 11:
                return "11 का divisibility rule:\nAlternate-place digits के दोनों sums का difference निकालें. Difference 0 या 11 का multiple हो, तो number 11 से divisible होता है.";

            default:
                return divisor
                        + " से divisibility check करने के लिए number को "
                        + divisor
                        + " से divide करें. Remainder 0 हो, तो number divisible है.";
        }
    }

    @NonNull
    private static LanguageMode resolveLanguageMode(
            @Nullable String explanationLanguage,
            @Nullable String currentQuestion
    ) {
        String question = normalizeText(currentQuestion);

        if (containsAny(question, HINGLISH_OVERRIDE_PHRASES)) {
            return LanguageMode.HINGLISH;
        }
        if (containsAny(question, ENGLISH_OVERRIDE_PHRASES)) {
            return LanguageMode.ENGLISH;
        }
        if (containsAny(question, HINDI_OVERRIDE_PHRASES)) {
            return LanguageMode.HINDI;
        }

        String language = normalizeText(explanationLanguage);

        if (language.contains("hinglish")
                || language.contains("bilingual")
                || language.contains("hi,en")
                || language.contains("hindi + english")
                || language.contains("hindi+english")
                || (language.contains("hindi") && language.contains("english"))) {
            return LanguageMode.HINGLISH;
        }

        if ((language.contains("english") || language.equals("en"))
                && !language.contains("hindi")) {
            return LanguageMode.ENGLISH;
        }

        if (language.contains("hindi")
                || language.equals("hi")
                || language.contains("हिंदी")
                || language.contains("हिन्दी")) {
            return LanguageMode.HINDI;
        }

        return LanguageMode.HINGLISH;
    }

    private static boolean containsAny(
            @NonNull String source,
            @NonNull String[] keywords
    ) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String formatBasicMathValue(
            @Nullable BigDecimal value,
            @NonNull String currencySymbol
    ) {
        if (value == null) {
            return currencySymbol + "0";
        }

        BigDecimal normalizedValue = value.stripTrailingZeros();
        if (normalizedValue.scale() < 0) {
            normalizedValue = normalizedValue.setScale(0);
        }

        return currencySymbol + normalizedValue.toPlainString();
    }

    @NonNull
    private static String createCombinedDiagnosticReason(
            @Nullable String divisibilityReason,
            @Nullable String basicMathReason
    ) {
        String safeDivisibilityReason = safeText(divisibilityReason);
        String safeBasicMathReason = safeText(basicMathReason);

        if (safeDivisibilityReason.isEmpty()
                && safeBasicMathReason.isEmpty()) {
            return "No supported offline Mathematics route matched.";
        }

        if (safeDivisibilityReason.isEmpty()) {
            return safeBasicMathReason;
        }

        if (safeBasicMathReason.isEmpty()) {
            return safeDivisibilityReason;
        }

        return "Divisibility route: "
                + safeDivisibilityReason
                + " | Basic Mathematics route: "
                + safeBasicMathReason;
    }

    @NonNull
    private static String normalizeText(@Nullable String value) {
        return safeText(value)
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    @NonNull
    private static String safeText(@Nullable Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private enum LanguageMode {
        ENGLISH,
        HINDI,
        HINGLISH
    }

    public static final class RouteResult {

        private final boolean handled;

        @NonNull
        private final String answerText;

        @NonNull
        private final String answerSource;

        @NonNull
        private final String diagnosticReason;

        @Nullable
        private final OfflineBasicMathSolver.Confidence confidence;

        private RouteResult(
                boolean handled,
                @NonNull String answerText,
                @NonNull String answerSource,
                @NonNull String diagnosticReason,
                @Nullable OfflineBasicMathSolver.Confidence confidence
        ) {
            this.handled = handled;
            this.answerText = answerText;
            this.answerSource = answerSource;
            this.diagnosticReason = diagnosticReason;
            this.confidence = confidence;
        }

        @NonNull
        private static RouteResult handled(
                @NonNull String answerText,
                @NonNull String answerSource,
                @Nullable OfflineBasicMathSolver.Confidence confidence
        ) {
            return new RouteResult(
                    true,
                    answerText,
                    answerSource,
                    "",
                    confidence
            );
        }

        @NonNull
        private static RouteResult notHandled(
                @NonNull String diagnosticReason
        ) {
            return new RouteResult(
                    false,
                    "",
                    SOURCE_NOT_HANDLED,
                    diagnosticReason,
                    null
            );
        }

        public boolean isHandled() {
            return handled;
        }

        @NonNull
        public String getAnswerText() {
            return answerText;
        }

        @NonNull
        public String getAnswerSource() {
            return answerSource;
        }

        @NonNull
        public String getDiagnosticReason() {
            return diagnosticReason;
        }

        @Nullable
        public OfflineBasicMathSolver.Confidence getConfidence() {
            return confidence;
        }
    }
}
