package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Study Saathi का deterministic offline basic Mathematics solver।
 *
 * यह solver Firebase, Gemini, internet या किसी external API का
 * उपयोग नहीं करता।
 *
 * Supported question types:
 *
 * 1. Direct addition:
 *    25 + 17
 *
 * 2. Direct subtraction:
 *    ₹4 - ₹3
 *
 * 3. Direct multiplication:
 *    8 × 7
 *
 * 4. Direct division:
 *    36 ÷ 6
 *
 * 5. आसान Hindi और English word problems जिनमें दो संख्याएँ हों।
 *
 * यह solver केवल स्पष्ट प्रश्न हल करता है।
 * प्रश्न अस्पष्ट होने पर अनुमान लगाने के बजाय not-solved result देता है।
 */
public final class OfflineBasicMathSolver {

    private static final int MAXIMUM_QUESTION_LENGTH =
            4000;

    private static final int DIVISION_SCALE =
            10;

    @NonNull
    private static final MathContext CALCULATION_CONTEXT =
            new MathContext(
                    16,
                    RoundingMode.HALF_UP
            );

    @NonNull
    private static final String NUMBER_TOKEN =
            "[-+]?\\d+(?:,\\d{3})*(?:\\.\\d+)?";

    @NonNull
    private static final Pattern DIRECT_EXPRESSION_PATTERN =
            Pattern.compile(
                    "([₹$€£¥]?\\s*"
                            + NUMBER_TOKEN
                            + ")"
                            + "\\s*"
                            + "([+\\-−×xX*÷/])"
                            + "\\s*"
                            + "([₹$€£¥]?\\s*"
                            + NUMBER_TOKEN
                            + ")"
            );

    @NonNull
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile(
                    "[₹$€£¥]?\\s*"
                            + NUMBER_TOKEN
            );

    @NonNull
    private static final String[] ADDITION_KEYWORDS = {
            "कुल",
            "मिलाकर",
            "जोड़",
            "जोड़",
            "जोड़कर",
            "जोड़कर",
            "और मिले",
            "और मिला",
            "और आए",
            "और आया",
            "total",
            "altogether",
            "in all",
            "sum",
            "combined",
            "added"
    };

    @NonNull
    private static final String[] SUBTRACTION_KEYWORDS = {
            "बचा",
            "बचे",
            "बची",
            "बाकी",
            "शेष",
            "खर्च",
            "खरीद",
            "दिया",
            "दिए",
            "दी",
            "दे दिया",
            "दे दिए",
            "कम हुआ",
            "कम हुए",
            "अंतर",
            "difference",
            "left",
            "remaining",
            "remain",
            "spent",
            "bought",
            "purchased",
            "gave away",
            "lost",
            "how many more"
    };

    @NonNull
    private static final String[] MULTIPLICATION_KEYWORDS = {
            "गुणा",
            "हर एक",
            "प्रत्येक",
            "हर पैकेट",
            "हर डिब्बे",
            "हर डिब्बा",
            "प्रति पैकेट",
            "प्रति डिब्बा",
            "प्रति समूह",
            "each",
            "every",
            "times",
            "groups of",
            "in each",
            "per packet",
            "per box"
    };

    @NonNull
    private static final String[] DIVISION_KEYWORDS = {
            "भाग",
            "बराबर बाँट",
            "बराबर बांट",
            "समान बाँट",
            "समान बांट",
            "आपस में बाँट",
            "आपस में बांट",
            "प्रति व्यक्ति",
            "प्रति बच्चे",
            "equal share",
            "equally",
            "shared among",
            "divide",
            "divided by",
            "split equally"
    };

    private OfflineBasicMathSolver() {
        /*
         * Utility class.
         * Object creation is not required.
         */
    }

    /**
     * दिए गए प्रश्न को offline हल करने की कोशिश करता है।
     *
     * प्रश्न स्पष्ट और supported होने पर solved result मिलेगा।
     * अन्यथा not-solved result मिलेगा।
     */
    @NonNull
    public static SolveResult trySolve(
            @Nullable String question
    ) {
        String safeQuestion =
                normalizeQuestion(
                        question
                );

        if (safeQuestion.isEmpty()) {
            return SolveResult.notSolved(
                    "Question is empty."
            );
        }

        if (safeQuestion.length()
                > MAXIMUM_QUESTION_LENGTH) {

            return SolveResult.notSolved(
                    "Question exceeds the supported offline length."
            );
        }

        SolveResult directExpressionResult =
                trySolveDirectExpression(
                        safeQuestion
                );

        if (directExpressionResult.isSolved()) {
            return directExpressionResult;
        }

        return trySolveWordProblem(
                safeQuestion
        );
    }

    /**
     * प्रश्न offline basic Mathematics solver द्वारा समर्थित है या नहीं।
     */
    public static boolean canSolve(
            @Nullable String question
    ) {
        return trySolve(
                question
        ).isSolved();
    }

    @NonNull
    private static SolveResult trySolveDirectExpression(
            @NonNull String question
    ) {
        Matcher matcher =
                DIRECT_EXPRESSION_PATTERN.matcher(
                        question
                );

        if (!matcher.find()) {
            return SolveResult.notSolved(
                    "No direct arithmetic expression found."
            );
        }

        String firstToken =
                safeGroup(
                        matcher,
                        1
                );

        String operatorToken =
                safeGroup(
                        matcher,
                        2
                );

        String secondToken =
                safeGroup(
                        matcher,
                        3
                );

        BigDecimal firstValue =
                parseDecimal(
                        firstToken
                );

        BigDecimal secondValue =
                parseDecimal(
                        secondToken
                );

        if (firstValue == null
                || secondValue == null) {

            return SolveResult.notSolved(
                    "Expression contains an unsupported number."
            );
        }

        Operation operation =
                operationFromSymbol(
                        operatorToken
                );

        if (operation == null) {
            return SolveResult.notSolved(
                    "Expression operator is unsupported."
            );
        }

        String currencySymbol =
                detectCurrencySymbol(
                        firstToken + " " + secondToken
                );

        return calculateResult(
                firstValue,
                secondValue,
                operation,
                currencySymbol,
                question,
                Confidence.HIGH
        );
    }

    @NonNull
    private static SolveResult trySolveWordProblem(
            @NonNull String question
    ) {
        List<BigDecimal> numbers =
                extractNumbers(
                        question
                );

        if (numbers.size() != 2) {
            return SolveResult.notSolved(
                    "Offline word-problem solving currently requires exactly two numbers."
            );
        }

        Operation operation =
                detectWordProblemOperation(
                        question
                );

        if (operation == null) {
            return SolveResult.notSolved(
                    "The required arithmetic operation is not clear."
            );
        }

        String currencySymbol =
                detectCurrencySymbol(
                        question
                );

        return calculateResult(
                numbers.get(0),
                numbers.get(1),
                operation,
                currencySymbol,
                question,
                Confidence.MEDIUM
        );
    }

    @NonNull
    private static SolveResult calculateResult(
            @NonNull BigDecimal firstValue,
            @NonNull BigDecimal secondValue,
            @NonNull Operation operation,
            @NonNull String currencySymbol,
            @NonNull String originalQuestion,
            @NonNull Confidence confidence
    ) {
        BigDecimal calculatedValue;

        switch (operation) {
            case ADDITION:
                calculatedValue =
                        firstValue.add(
                                secondValue,
                                CALCULATION_CONTEXT
                        );
                break;

            case SUBTRACTION:
                calculatedValue =
                        firstValue.subtract(
                                secondValue,
                                CALCULATION_CONTEXT
                        );
                break;

            case MULTIPLICATION:
                calculatedValue =
                        firstValue.multiply(
                                secondValue,
                                CALCULATION_CONTEXT
                        );
                break;

            case DIVISION:
                if (secondValue.compareTo(
                        BigDecimal.ZERO
                ) == 0) {

                    return SolveResult.notSolved(
                            "Division by zero is not allowed."
                    );
                }

                calculatedValue =
                        firstValue.divide(
                                secondValue,
                                DIVISION_SCALE,
                                RoundingMode.HALF_UP
                        );
                break;

            default:
                return SolveResult.notSolved(
                        "Unsupported arithmetic operation."
                );
        }

        BigDecimal normalizedResult =
                normalizeDecimal(
                        calculatedValue
                );

        String firstDisplayValue =
                formatValue(
                        firstValue,
                        currencySymbol
                );

        String secondDisplayValue =
                formatValue(
                        secondValue,
                        currencySymbol
                );

        String resultDisplayValue =
                formatValue(
                        normalizedResult,
                        currencySymbol
                );

        String expression =
                firstDisplayValue
                        + " "
                        + operation.getDisplaySymbol()
                        + " "
                        + secondDisplayValue
                        + " = "
                        + resultDisplayValue;

        String answerText =
                createAnswerText(
                        expression,
                        resultDisplayValue,
                        operation,
                        originalQuestion,
                        !currencySymbol.isEmpty()
                );

        return SolveResult.solved(
                operation,
                firstValue,
                secondValue,
                normalizedResult,
                currencySymbol,
                expression,
                answerText,
                confidence
        );
    }

    @NonNull
    private static String createAnswerText(
            @NonNull String expression,
            @NonNull String resultDisplayValue,
            @NonNull Operation operation,
            @NonNull String originalQuestion,
            boolean moneyQuestion
    ) {
        String normalizedQuestion =
                originalQuestion.toLowerCase(
                        Locale.ROOT
                );

        StringBuilder answerBuilder =
                new StringBuilder();

        answerBuilder.append(
                expression
        );

        answerBuilder.append(
                "\n\n"
        );

        if (operation == Operation.SUBTRACTION
                && moneyQuestion
                && containsAnyKeyword(
                normalizedQuestion,
                SUBTRACTION_KEYWORDS
        )) {

            answerBuilder.append(
                    "अतः आपके पास "
            );

            answerBuilder.append(
                    resultDisplayValue
            );

            answerBuilder.append(
                    " बचा।"
            );

            return answerBuilder.toString();
        }

        if (operation == Operation.ADDITION) {
            answerBuilder.append(
                    "अतः कुल उत्तर "
            );

        } else if (operation == Operation.SUBTRACTION) {
            answerBuilder.append(
                    "अतः शेष उत्तर "
            );

        } else if (operation == Operation.MULTIPLICATION) {
            answerBuilder.append(
                    "अतः गुणनफल "
            );

        } else {
            answerBuilder.append(
                    "अतः भागफल "
            );
        }

        answerBuilder.append(
                resultDisplayValue
        );

        answerBuilder.append(
                " है।"
        );

        return answerBuilder.toString();
    }

    @Nullable
    private static Operation detectWordProblemOperation(
            @NonNull String question
    ) {
        String normalizedQuestion =
                question.toLowerCase(
                        Locale.ROOT
                );

        int additionScore =
                calculateKeywordScore(
                        normalizedQuestion,
                        ADDITION_KEYWORDS,
                        1
                );

        int subtractionScore =
                calculateKeywordScore(
                        normalizedQuestion,
                        SUBTRACTION_KEYWORDS,
                        2
                );

        int multiplicationScore =
                calculateKeywordScore(
                        normalizedQuestion,
                        MULTIPLICATION_KEYWORDS,
                        3
                );

        int divisionScore =
                calculateKeywordScore(
                        normalizedQuestion,
                        DIVISION_KEYWORDS,
                        4
                );

        int highestScore =
                Math.max(
                        Math.max(
                                additionScore,
                                subtractionScore
                        ),
                        Math.max(
                                multiplicationScore,
                                divisionScore
                        )
                );

        if (highestScore <= 0) {
            return null;
        }

        int highestScoreCount =
                0;

        Operation selectedOperation =
                null;

        if (additionScore == highestScore) {
            highestScoreCount++;
            selectedOperation =
                    Operation.ADDITION;
        }

        if (subtractionScore == highestScore) {
            highestScoreCount++;
            selectedOperation =
                    Operation.SUBTRACTION;
        }

        if (multiplicationScore == highestScore) {
            highestScoreCount++;
            selectedOperation =
                    Operation.MULTIPLICATION;
        }

        if (divisionScore == highestScore) {
            highestScoreCount++;
            selectedOperation =
                    Operation.DIVISION;
        }

        if (highestScoreCount != 1) {
            return null;
        }

        return selectedOperation;
    }

    private static int calculateKeywordScore(
            @NonNull String normalizedQuestion,
            @NonNull String[] keywords,
            int keywordWeight
    ) {
        int score =
                0;

        for (String keyword : keywords) {
            if (normalizedQuestion.contains(
                    keyword
            )) {
                score +=
                        keywordWeight;
            }
        }

        return score;
    }

    private static boolean containsAnyKeyword(
            @NonNull String normalizedQuestion,
            @NonNull String[] keywords
    ) {
        for (String keyword : keywords) {
            if (normalizedQuestion.contains(
                    keyword
            )) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private static List<BigDecimal> extractNumbers(
            @NonNull String question
    ) {
        List<BigDecimal> extractedNumbers =
                new ArrayList<>();

        Matcher matcher =
                NUMBER_PATTERN.matcher(
                        question
                );

        while (matcher.find()) {
            BigDecimal parsedNumber =
                    parseDecimal(
                            matcher.group()
                    );

            if (parsedNumber != null) {
                extractedNumbers.add(
                        parsedNumber
                );
            }
        }

        return extractedNumbers;
    }

    @Nullable
    private static BigDecimal parseDecimal(
            @Nullable String rawValue
    ) {
        if (rawValue == null) {
            return null;
        }

        String normalizedValue =
                rawValue
                        .replace(
                                "₹",
                                ""
                        )
                        .replace(
                                "$",
                                ""
                        )
                        .replace(
                                "€",
                                ""
                        )
                        .replace(
                                "£",
                                ""
                        )
                        .replace(
                                "¥",
                                ""
                        )
                        .replace(
                                ",",
                                ""
                        )
                        .replace(
                                "−",
                                "-"
                        )
                        .trim();

        if (normalizedValue.isEmpty()
                || normalizedValue.equals("+")
                || normalizedValue.equals("-")) {

            return null;
        }

        try {
            return new BigDecimal(
                    normalizedValue,
                    CALCULATION_CONTEXT
            );

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Nullable
    private static Operation operationFromSymbol(
            @Nullable String operatorSymbol
    ) {
        if (operatorSymbol == null) {
            return null;
        }

        switch (operatorSymbol.trim()) {
            case "+":
                return Operation.ADDITION;

            case "-":
            case "−":
                return Operation.SUBTRACTION;

            case "×":
            case "x":
            case "X":
            case "*":
                return Operation.MULTIPLICATION;

            case "÷":
            case "/":
                return Operation.DIVISION;

            default:
                return null;
        }
    }

    @NonNull
    private static String detectCurrencySymbol(
            @Nullable String text
    ) {
        if (text == null) {
            return "";
        }

        if (text.contains("₹")) {
            return "₹";
        }

        if (text.contains("$")) {
            return "$";
        }

        if (text.contains("€")) {
            return "€";
        }

        if (text.contains("£")) {
            return "£";
        }

        if (text.contains("¥")) {
            return "¥";
        }

        return "";
    }

    @NonNull
    private static String formatValue(
            @NonNull BigDecimal value,
            @NonNull String currencySymbol
    ) {
        String formattedNumber =
                normalizeDecimal(
                        value
                ).toPlainString();

        return currencySymbol
                + formattedNumber;
    }

    @NonNull
    private static BigDecimal normalizeDecimal(
            @NonNull BigDecimal value
    ) {
        BigDecimal strippedValue =
                value.stripTrailingZeros();

        if (strippedValue.scale() < 0) {
            return strippedValue.setScale(
                    0
            );
        }

        return strippedValue;
    }

    @NonNull
    private static String normalizeQuestion(
            @Nullable String question
    ) {
        if (question == null) {
            return "";
        }

        return question
                .replace(
                        '\u00A0',
                        ' '
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    @NonNull
    private static String safeGroup(
            @NonNull Matcher matcher,
            int groupIndex
    ) {
        String groupValue =
                matcher.group(
                        groupIndex
                );

        return groupValue == null
                ? ""
                : groupValue.trim();
    }

    public enum Operation {

        ADDITION("+"),

        SUBTRACTION("−"),

        MULTIPLICATION("×"),

        DIVISION("÷");

        @NonNull
        private final String displaySymbol;

        Operation(
                @NonNull String displaySymbol
        ) {
            this.displaySymbol =
                    displaySymbol;
        }

        @NonNull
        public String getDisplaySymbol() {
            return displaySymbol;
        }
    }

    public enum Confidence {

        HIGH,

        MEDIUM
    }

    /**
     * Offline solver का immutable result।
     */
    public static final class SolveResult {

        private final boolean solved;

        @Nullable
        private final Operation operation;

        @Nullable
        private final BigDecimal firstValue;

        @Nullable
        private final BigDecimal secondValue;

        @Nullable
        private final BigDecimal resultValue;

        @NonNull
        private final String currencySymbol;

        @NonNull
        private final String expression;

        @NonNull
        private final String answerText;

        @NonNull
        private final String diagnosticReason;

        @Nullable
        private final Confidence confidence;

        private SolveResult(
                boolean solved,
                @Nullable Operation operation,
                @Nullable BigDecimal firstValue,
                @Nullable BigDecimal secondValue,
                @Nullable BigDecimal resultValue,
                @NonNull String currencySymbol,
                @NonNull String expression,
                @NonNull String answerText,
                @NonNull String diagnosticReason,
                @Nullable Confidence confidence
        ) {
            this.solved =
                    solved;

            this.operation =
                    operation;

            this.firstValue =
                    firstValue;

            this.secondValue =
                    secondValue;

            this.resultValue =
                    resultValue;

            this.currencySymbol =
                    currencySymbol;

            this.expression =
                    expression;

            this.answerText =
                    answerText;

            this.diagnosticReason =
                    diagnosticReason;

            this.confidence =
                    confidence;
        }

        @NonNull
        private static SolveResult solved(
                @NonNull Operation operation,
                @NonNull BigDecimal firstValue,
                @NonNull BigDecimal secondValue,
                @NonNull BigDecimal resultValue,
                @NonNull String currencySymbol,
                @NonNull String expression,
                @NonNull String answerText,
                @NonNull Confidence confidence
        ) {
            return new SolveResult(
                    true,
                    operation,
                    firstValue,
                    secondValue,
                    resultValue,
                    currencySymbol,
                    expression,
                    answerText,
                    "",
                    confidence
            );
        }

        @NonNull
        private static SolveResult notSolved(
                @NonNull String diagnosticReason
        ) {
            return new SolveResult(
                    false,
                    null,
                    null,
                    null,
                    null,
                    "",
                    "",
                    "",
                    diagnosticReason,
                    null
            );
        }

        public boolean isSolved() {
            return solved;
        }

        @Nullable
        public Operation getOperation() {
            return operation;
        }

        @Nullable
        public BigDecimal getFirstValue() {
            return firstValue;
        }

        @Nullable
        public BigDecimal getSecondValue() {
            return secondValue;
        }

        @Nullable
        public BigDecimal getResultValue() {
            return resultValue;
        }

        @NonNull
        public String getCurrencySymbol() {
            return currencySymbol;
        }

        @NonNull
        public String getExpression() {
            return expression;
        }

        @NonNull
        public String getAnswerText() {
            return answerText;
        }

        @NonNull
        public String getDiagnosticReason() {
            return diagnosticReason;
        }

        @Nullable
        public Confidence getConfidence() {
            return confidence;
        }
    }
}