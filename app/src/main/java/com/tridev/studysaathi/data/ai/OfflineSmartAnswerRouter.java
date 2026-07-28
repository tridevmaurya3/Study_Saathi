package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Study Saathi का offline-first answer router।
 *
 * यह class Gemini request भेजने से पहले जाँचती है कि
 * वर्तमान question को सुरक्षित और भरोसेमंद तरीके से
 * local device पर हल किया जा सकता है या नहीं।
 *
 * वर्तमान supported local routes:
 *
 * 1. Mathematics divisibility questions और rules।
 * 2. Direct addition, subtraction, multiplication और division।
 * 3. दो संख्याओं वाले आसान Hindi या English word problems।
 *
 * यह router अस्पष्ट या कठिन question का अनुमान नहीं लगाता।
 * ऐसी स्थिति में handled=false देता है, जिससे question आगे
 * cache या Firebase AI / Gemini को भेजा जा सके।
 */
public final class OfflineSmartAnswerRouter {

    public static final String SOURCE_OFFLINE_DIVISIBILITY =
            "OFFLINE_DIVISIBILITY";

    public static final String SOURCE_OFFLINE_BASIC_MATH =
            "OFFLINE_BASIC_MATH";

    public static final String SOURCE_NOT_HANDLED =
            "NOT_HANDLED";

    private OfflineSmartAnswerRouter() {
        /*
         * Utility class.
         * Object creation is not required.
         */
    }

    /**
     * Current student question का offline answer बनाने की कोशिश करता है।
     *
     * Routing order:
     *
     * 1. Request validation
     * 2. Image safety check
     * 3. Mathematics subject check
     * 4. Divisibility solver
     * 5. Basic arithmetic solver
     *
     * @param subjectName selected school subject
     * @param question current student question
     * @param explanationLanguage selected answer language
     * @param imageAttached current question के साथ image लगी है या नहीं
     *
     * @return handled result या not-handled result
     */
    @NonNull
    public static RouteResult tryCreateAnswer(
            @Nullable String subjectName,
            @Nullable String question,
            @Nullable String explanationLanguage,
            boolean imageAttached
    ) {
        String safeSubjectName =
                safeText(
                        subjectName
                );

        String safeQuestion =
                safeText(
                        question
                );

        if (safeQuestion.isEmpty()) {
            return RouteResult.notHandled(
                    "Question is empty."
            );
        }

        /*
         * Original image वाले questions को अभी Gemini तक जाने दिया जाएगा।
         *
         * कारण:
         * Image में diagram, handwritten symbols, fractions,
         * extra numbers या visual information हो सकती है।
         */
        if (imageAttached) {
            return RouteResult.notHandled(
                    "An original question image is attached."
            );
        }

        if (!isMathematicsSubject(
                safeSubjectName
        )) {
            return RouteResult.notHandled(
                    "Selected subject is not Mathematics."
            );
        }

        /*
         * सबसे पहले divisibility question जाँचें।
         *
         * उदाहरण:
         *
         * क्या 403403 संख्या 11 से विभाज्य है?
         * 3 से विभाज्यता का नियम बताओ।
         * Is 826 divisible by 7?
         *
         * Divisibility solver language selection स्वयं संभालता है।
         */
        OfflineDivisibilitySolver.SolveResult
                divisibilitySolveResult =
                OfflineDivisibilitySolver.trySolve(
                        safeQuestion,
                        explanationLanguage
                );

        if (divisibilitySolveResult.isSolved()) {
            String divisibilityAnswer =
                    safeText(
                            divisibilitySolveResult
                                    .getAnswerText()
                    );

            if (!divisibilityAnswer.isEmpty()) {
                return RouteResult.handled(
                        divisibilityAnswer,
                        SOURCE_OFFLINE_DIVISIBILITY,
                        null
                );
            }
        }

        /*
         * Divisibility question न होने पर basic arithmetic
         * और आसान word-problem solver चलाएँ।
         */
        OfflineBasicMathSolver.SolveResult
                basicMathSolveResult =
                OfflineBasicMathSolver.trySolve(
                        safeQuestion
                );

        if (!basicMathSolveResult.isSolved()) {
            return RouteResult.notHandled(
                    createCombinedDiagnosticReason(
                            divisibilitySolveResult
                                    .getDiagnosticReason(),
                            basicMathSolveResult
                                    .getDiagnosticReason()
                    )
            );
        }

        LanguageMode languageMode =
                detectLanguageMode(
                        explanationLanguage
                );

        String englishAnswer =
                createEnglishBasicMathAnswer(
                        basicMathSolveResult
                );

        String hindiAnswer =
                basicMathSolveResult
                        .getAnswerText();

        String finalAnswer =
                combineAnswer(
                        englishAnswer,
                        hindiAnswer,
                        languageMode
                );

        return RouteResult.handled(
                finalAnswer,
                SOURCE_OFFLINE_BASIC_MATH,
                basicMathSolveResult
                        .getConfidence()
        );
    }

    /**
     * Different school subject naming formats में Mathematics पहचानता है।
     *
     * Supported examples:
     *
     * Mathematics
     * Maths
     * Math
     * गणित
     * अंकगणित
     */
    private static boolean isMathematicsSubject(
            @NonNull String subjectName
    ) {
        String normalizedSubjectName =
                normalizeText(
                        subjectName
                );

        return normalizedSubjectName.contains(
                "mathematics"
        )
                || normalizedSubjectName.contains(
                "maths"
        )
                || normalizedSubjectName.equals(
                "math"
        )
                || normalizedSubjectName.contains(
                "गणित"
        )
                || normalizedSubjectName.contains(
                "अंकगणित"
        );
    }

    /**
     * Basic Mathematics structured result से English answer बनाता है।
     */
    @NonNull
    private static String createEnglishBasicMathAnswer(
            @NonNull OfflineBasicMathSolver.SolveResult solveResult
    ) {
        String expression =
                solveResult.getExpression();

        String resultDisplayValue =
                formatBasicMathValue(
                        solveResult.getResultValue(),
                        solveResult.getCurrencySymbol()
                );

        StringBuilder answerBuilder =
                new StringBuilder();

        answerBuilder.append(
                expression
        );

        answerBuilder.append(
                "\n\n"
        );

        OfflineBasicMathSolver.Operation operation =
                solveResult.getOperation();

        if (operation == null) {
            answerBuilder.append(
                    "Therefore, the answer is "
            );

            answerBuilder.append(
                    resultDisplayValue
            );

            answerBuilder.append(
                    "."
            );

            return answerBuilder.toString();
        }

        switch (operation) {
            case ADDITION:
                answerBuilder.append(
                        "Therefore, the total is "
                );

                answerBuilder.append(
                        resultDisplayValue
                );

                answerBuilder.append(
                        "."
                );
                break;

            case SUBTRACTION:
                if (!solveResult
                        .getCurrencySymbol()
                        .isEmpty()) {

                    answerBuilder.append(
                            "Therefore, "
                    );

                    answerBuilder.append(
                            resultDisplayValue
                    );

                    answerBuilder.append(
                            " remains."
                    );

                } else {
                    answerBuilder.append(
                            "Therefore, the remaining value is "
                    );

                    answerBuilder.append(
                            resultDisplayValue
                    );

                    answerBuilder.append(
                            "."
                    );
                }
                break;

            case MULTIPLICATION:
                answerBuilder.append(
                        "Therefore, the product is "
                );

                answerBuilder.append(
                        resultDisplayValue
                );

                answerBuilder.append(
                        "."
                );
                break;

            case DIVISION:
                answerBuilder.append(
                        "Therefore, the quotient is "
                );

                answerBuilder.append(
                        resultDisplayValue
                );

                answerBuilder.append(
                        "."
                );
                break;

            default:
                answerBuilder.append(
                        "Therefore, the answer is "
                );

                answerBuilder.append(
                        resultDisplayValue
                );

                answerBuilder.append(
                        "."
                );
                break;
        }

        return answerBuilder.toString();
    }

    /**
     * Student की selected explanation language के अनुसार
     * basic Mathematics final answer बनाता है।
     */
    @NonNull
    private static String combineAnswer(
            @NonNull String englishAnswer,
            @NonNull String hindiAnswer,
            @NonNull LanguageMode languageMode
    ) {
        switch (languageMode) {
            case HINDI:
                return hindiAnswer;

            case ENGLISH:
                return englishAnswer;

            case BILINGUAL:
            default:
                return "English:\n"
                        + englishAnswer
                        + "\n\nहिंदी:\n"
                        + hindiAnswer;
        }
    }

    /**
     * Explanation language setting को internal language mode में बदलता है।
     */
    @NonNull
    private static LanguageMode detectLanguageMode(
            @Nullable String explanationLanguage
    ) {
        String normalizedLanguage =
                normalizeText(
                        explanationLanguage
                );

        boolean containsHindi =
                normalizedLanguage.contains(
                        "hindi"
                )
                        || normalizedLanguage.contains(
                        "हिंदी"
                );

        boolean containsEnglish =
                normalizedLanguage.contains(
                        "english"
                )
                        || normalizedLanguage.contains(
                        "अंग्रेज"
                );

        if (containsHindi
                && !containsEnglish) {

            return LanguageMode.HINDI;
        }

        if (containsEnglish
                && !containsHindi) {

            return LanguageMode.ENGLISH;
        }

        return LanguageMode.BILINGUAL;
    }

    /**
     * BigDecimal को student-friendly format में बदलता है।
     *
     * उदाहरण:
     *
     * 42.00 → 42
     * 7.50  → 7.5
     */
    @NonNull
    private static String formatBasicMathValue(
            @Nullable BigDecimal value,
            @NonNull String currencySymbol
    ) {
        if (value == null) {
            return currencySymbol + "0";
        }

        BigDecimal normalizedValue =
                value.stripTrailingZeros();

        if (normalizedValue.scale() < 0) {
            normalizedValue =
                    normalizedValue.setScale(
                            0
                    );
        }

        return currencySymbol
                + normalizedValue.toPlainString();
    }

    /**
     * दोनों offline solvers के failure reasons को internal
     * diagnostic message में जोड़ता है।
     *
     * यह message student को नहीं दिखाया जाएगा।
     */
    @NonNull
    private static String createCombinedDiagnosticReason(
            @Nullable String divisibilityReason,
            @Nullable String basicMathReason
    ) {
        String safeDivisibilityReason =
                safeText(
                        divisibilityReason
                );

        String safeBasicMathReason =
                safeText(
                        basicMathReason
                );

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
    private static String normalizeText(
            @Nullable String value
    ) {
        return safeText(
                value
        )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }

    private enum LanguageMode {

        BILINGUAL,

        HINDI,

        ENGLISH
    }

    /**
     * Offline routing का immutable result।
     */
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
            this.handled =
                    handled;

            this.answerText =
                    answerText;

            this.answerSource =
                    answerSource;

            this.diagnosticReason =
                    diagnosticReason;

            this.confidence =
                    confidence;
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

        /**
         * true होने पर answer Gemini को भेजे बिना दिखाया जा सकता है।
         */
        public boolean isHandled() {
            return handled;
        }

        /**
         * Student को दिखाने योग्य final answer।
         */
        @NonNull
        public String getAnswerText() {
            return answerText;
        }

        /**
         * Answer किस local engine से आया।
         *
         * संभावित values:
         *
         * OFFLINE_DIVISIBILITY
         * OFFLINE_BASIC_MATH
         * NOT_HANDLED
         */
        @NonNull
        public String getAnswerSource() {
            return answerSource;
        }

        /**
         * handled=false होने का internal कारण।
         *
         * यह text student को दिखाने के लिए नहीं है।
         */
        @NonNull
        public String getDiagnosticReason() {
            return diagnosticReason;
        }

        /**
         * Basic Mathematics solver की confidence।
         *
         * Divisibility answer में यह null हो सकती है क्योंकि
         * divisibility solver deterministic result देता है।
         */
        @Nullable
        public OfflineBasicMathSolver.Confidence getConfidence() {
            return confidence;
        }
    }
}