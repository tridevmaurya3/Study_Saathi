package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Study Saathi का deterministic offline divisibility solver।
 *
 * यह Firebase, Gemini या internet का उपयोग नहीं करता।
 *
 * Supported requests:
 *
 * 1. Divisibility check:
 *    "क्या 403403 संख्या 11 से विभाज्य है?"
 *    "Is 826 divisible by 7?"
 *
 * 2. Divisibility rule:
 *    "3 से विभाज्यता का नियम बताओ।"
 *    "Explain the divisibility rule of 11."
 *
 * स्पष्ट प्रश्न न मिलने पर यह अनुमान नहीं लगाता और
 * solved=false result लौटाता है।
 */
public final class OfflineDivisibilitySolver {

    private static final int MAXIMUM_QUESTION_LENGTH =
            3000;

    @NonNull
    private static final Pattern INTEGER_PATTERN =
            Pattern.compile(
                    "-?\\d+"
            );

    @NonNull
    private static final String[] DIVISIBILITY_KEYWORDS = {
            "divisible",
            "divisibility",
            "विभाज्य",
            "विभाज्यता",
            "भाग जाता",
            "भाग जाती",
            "भाग जाएगा",
            "भाग जायेगा",
            "पूरी तरह भाग",
            "शेषफल"
    };

    @NonNull
    private static final String[] RULE_KEYWORDS = {
            "rule",
            "नियम",
            "तरिका",
            "तरीका",
            "कैसे पता",
            "how to check",
            "how can we check",
            "test of divisibility"
    };

    private OfflineDivisibilitySolver() {
        /*
         * Utility class.
         * Object creation is not required.
         */
    }

    /**
     * Divisibility question को offline हल करने की कोशिश करता है।
     */
    @NonNull
    public static SolveResult trySolve(
            @Nullable String question,
            @Nullable String explanationLanguage
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

        String normalizedQuestion =
                safeQuestion.toLowerCase(
                        Locale.ROOT
                );

        if (!containsAny(
                normalizedQuestion,
                DIVISIBILITY_KEYWORDS
        )) {
            return SolveResult.notSolved(
                    "Question does not appear to be about divisibility."
            );
        }

        List<BigInteger> numbers =
                extractIntegers(
                        safeQuestion
                );

        LanguageMode languageMode =
                detectLanguageMode(
                        explanationLanguage
                );

        /*
         * Rule request में सामान्यतः केवल divisor दिया जाता है।
         *
         * उदाहरण:
         * "11 से विभाज्यता का नियम बताओ।"
         */
        if (containsAny(
                normalizedQuestion,
                RULE_KEYWORDS
        )
                && numbers.size() == 1) {

            BigInteger divisor =
                    numbers.get(0).abs();

            if (divisor.signum() == 0) {
                return SolveResult.notSolved(
                        "Divisor cannot be zero."
                );
            }

            String englishRule =
                    createEnglishRule(
                            divisor
                    );

            String hindiRule =
                    createHindiRule(
                            divisor
                    );

            return SolveResult.solvedRule(
                    divisor,
                    combineAnswer(
                            englishRule,
                            hindiRule,
                            languageMode
                    )
            );
        }

        /*
         * Number check में ठीक दो integers आवश्यक हैं।
         *
         * पहला number dividend और दूसरा divisor माना जाएगा।
         */
        if (numbers.size() != 2) {
            return SolveResult.notSolved(
                    "Divisibility checking requires exactly two integers."
            );
        }

        BigInteger dividend =
                numbers.get(0);

        BigInteger divisor =
                numbers.get(1).abs();

        if (divisor.signum() == 0) {
            return SolveResult.notSolved(
                    "Division by zero is not allowed."
            );
        }

        BigInteger absoluteDividend =
                dividend.abs();

        BigInteger remainder =
                absoluteDividend.mod(
                        divisor
                );

        boolean divisible =
                remainder.signum() == 0;

        String englishAnswer =
                createEnglishCheckAnswer(
                        dividend,
                        divisor,
                        remainder,
                        divisible
                );

        String hindiAnswer =
                createHindiCheckAnswer(
                        dividend,
                        divisor,
                        remainder,
                        divisible
                );

        return SolveResult.solvedCheck(
                dividend,
                divisor,
                remainder,
                divisible,
                combineAnswer(
                        englishAnswer,
                        hindiAnswer,
                        languageMode
                )
        );
    }

    @NonNull
    private static String createEnglishCheckAnswer(
            @NonNull BigInteger dividend,
            @NonNull BigInteger divisor,
            @NonNull BigInteger remainder,
            boolean divisible
    ) {
        StringBuilder answerBuilder =
                new StringBuilder();

        answerBuilder.append(
                dividend
        );

        if (divisible) {
            answerBuilder.append(
                    " is divisible by "
            );

            answerBuilder.append(
                    divisor
            );

            answerBuilder.append(
                    ".\n\n"
            );

        } else {
            answerBuilder.append(
                    " is not divisible by "
            );

            answerBuilder.append(
                    divisor
            );

            answerBuilder.append(
                    ".\n\n"
            );
        }

        answerBuilder.append(
                createEnglishAppliedRule(
                        dividend.abs(),
                        divisor
                )
        );

        answerBuilder.append(
                "\n\nRemainder = "
        );

        answerBuilder.append(
                remainder
        );

        if (divisible) {
            answerBuilder.append(
                    "\nTherefore, the number is completely divisible."
            );

        } else {
            answerBuilder.append(
                    "\nTherefore, the number is not completely divisible."
            );
        }

        return answerBuilder.toString();
    }

    @NonNull
    private static String createHindiCheckAnswer(
            @NonNull BigInteger dividend,
            @NonNull BigInteger divisor,
            @NonNull BigInteger remainder,
            boolean divisible
    ) {
        StringBuilder answerBuilder =
                new StringBuilder();

        answerBuilder.append(
                dividend
        );

        if (divisible) {
            answerBuilder.append(
                    ", "
            );

            answerBuilder.append(
                    divisor
            );

            answerBuilder.append(
                    " से विभाज्य है।\n\n"
            );

        } else {
            answerBuilder.append(
                    ", "
            );

            answerBuilder.append(
                    divisor
            );

            answerBuilder.append(
                    " से विभाज्य नहीं है।\n\n"
            );
        }

        answerBuilder.append(
                createHindiAppliedRule(
                        dividend.abs(),
                        divisor
                )
        );

        answerBuilder.append(
                "\n\nशेषफल = "
        );

        answerBuilder.append(
                remainder
        );

        if (divisible) {
            answerBuilder.append(
                    "\nअतः संख्या पूरी तरह विभाजित हो जाती है।"
            );

        } else {
            answerBuilder.append(
                    "\nअतः संख्या पूरी तरह विभाजित नहीं होती।"
            );
        }

        return answerBuilder.toString();
    }

    @NonNull
    private static String createEnglishAppliedRule(
            @NonNull BigInteger dividend,
            @NonNull BigInteger divisor
    ) {
        int divisorValue =
                safeSmallInt(
                        divisor
                );

        String digits =
                dividend.toString();

        switch (divisorValue) {
            case 2:
                return "The last digit is "
                        + lastDigits(digits, 1)
                        + ". An even last digit means divisibility by 2.";

            case 3:
                return "Sum of digits = "
                        + calculateDigitSum(digits)
                        + ". The digit sum is checked for divisibility by 3.";

            case 4:
                return "The last two digits are "
                        + lastDigits(digits, 2)
                        + ". These last two digits are checked for divisibility by 4.";

            case 5:
                return "The last digit is "
                        + lastDigits(digits, 1)
                        + ". A number ending in 0 or 5 is divisible by 5.";

            case 6:
                return "A number must be divisible by both 2 and 3 to be divisible by 6.";

            case 8:
                return "The last three digits are "
                        + lastDigits(digits, 3)
                        + ". These digits are checked for divisibility by 8.";

            case 9:
                return "Sum of digits = "
                        + calculateDigitSum(digits)
                        + ". The digit sum is checked for divisibility by 9.";

            case 10:
                return "The last digit is "
                        + lastDigits(digits, 1)
                        + ". A number ending in 0 is divisible by 10.";

            case 11:
                AlternatingSum alternatingSum =
                        calculateAlternatingSum(
                                digits
                        );

                return "Sum of alternate place digits = "
                        + alternatingSum.firstPlaceSum
                        + " and "
                        + alternatingSum.secondPlaceSum
                        + ". Their difference is "
                        + alternatingSum.difference.abs()
                        + ".";

            default:
                return "The number is checked by division and its remainder.";
        }
    }

    @NonNull
    private static String createHindiAppliedRule(
            @NonNull BigInteger dividend,
            @NonNull BigInteger divisor
    ) {
        int divisorValue =
                safeSmallInt(
                        divisor
                );

        String digits =
                dividend.toString();

        switch (divisorValue) {
            case 2:
                return "अंतिम अंक "
                        + lastDigits(digits, 1)
                        + " है। सम अंतिम अंक वाली संख्या 2 से विभाज्य होती है।";

            case 3:
                return "अंकों का योग = "
                        + calculateDigitSum(digits)
                        + "। अंकों के योग को 3 से विभाजित करके जाँचते हैं।";

            case 4:
                return "अंतिम दो अंक "
                        + lastDigits(digits, 2)
                        + " हैं। इन दो अंकों को 4 से विभाजित करके जाँचते हैं।";

            case 5:
                return "अंतिम अंक "
                        + lastDigits(digits, 1)
                        + " है। 0 या 5 पर समाप्त होने वाली संख्या 5 से विभाज्य होती है।";

            case 6:
                return "6 से विभाज्य होने के लिए संख्या का 2 और 3 दोनों से विभाज्य होना जरूरी है।";

            case 8:
                return "अंतिम तीन अंक "
                        + lastDigits(digits, 3)
                        + " हैं। इन अंकों को 8 से विभाजित करके जाँचते हैं।";

            case 9:
                return "अंकों का योग = "
                        + calculateDigitSum(digits)
                        + "। अंकों के योग को 9 से विभाजित करके जाँचते हैं।";

            case 10:
                return "अंतिम अंक "
                        + lastDigits(digits, 1)
                        + " है। 0 पर समाप्त होने वाली संख्या 10 से विभाज्य होती है।";

            case 11:
                AlternatingSum alternatingSum =
                        calculateAlternatingSum(
                                digits
                        );

                return "एकांतर स्थानों के अंकों का योग "
                        + alternatingSum.firstPlaceSum
                        + " और "
                        + alternatingSum.secondPlaceSum
                        + " है। दोनों का अंतर "
                        + alternatingSum.difference.abs()
                        + " है।";

            default:
                return "संख्या को भाग देकर प्राप्त शेषफल से विभाज्यता जाँची गई है।";
        }
    }

    @NonNull
    private static String createEnglishRule(
            @NonNull BigInteger divisor
    ) {
        switch (safeSmallInt(divisor)) {
            case 2:
                return "Divisibility rule of 2:\nA number is divisible by 2 when its last digit is even: 0, 2, 4, 6 or 8.";

            case 3:
                return "Divisibility rule of 3:\nAdd all digits. If the sum is divisible by 3, the complete number is divisible by 3.";

            case 4:
                return "Divisibility rule of 4:\nCheck the last two digits. If that two-digit number is divisible by 4, the complete number is divisible by 4.";

            case 5:
                return "Divisibility rule of 5:\nThe last digit must be 0 or 5.";

            case 6:
                return "Divisibility rule of 6:\nThe number must be divisible by both 2 and 3.";

            case 8:
                return "Divisibility rule of 8:\nCheck the last three digits. If they form a number divisible by 8, the complete number is divisible by 8.";

            case 9:
                return "Divisibility rule of 9:\nAdd all digits. If the sum is divisible by 9, the complete number is divisible by 9.";

            case 10:
                return "Divisibility rule of 10:\nThe last digit must be 0.";

            case 11:
                return "Divisibility rule of 11:\nFind the difference between the sums of alternate-place digits. The number is divisible by 11 when the difference is 0 or a multiple of 11.";

            default:
                return "To check divisibility by "
                        + divisor
                        + ", divide the number by "
                        + divisor
                        + ". A zero remainder means the number is divisible.";
        }
    }

    @NonNull
    private static String createHindiRule(
            @NonNull BigInteger divisor
    ) {
        switch (safeSmallInt(divisor)) {
            case 2:
                return "2 से विभाज्यता का नियम:\nयदि अंतिम अंक 0, 2, 4, 6 या 8 हो, तो संख्या 2 से विभाज्य होती है।";

            case 3:
                return "3 से विभाज्यता का नियम:\nसभी अंकों का योग करें। यदि योग 3 से विभाज्य है, तो पूरी संख्या भी 3 से विभाज्य होगी।";

            case 4:
                return "4 से विभाज्यता का नियम:\nअंतिम दो अंकों से बनी संख्या को देखें। वह 4 से विभाज्य हो तो पूरी संख्या 4 से विभाज्य होगी।";

            case 5:
                return "5 से विभाज्यता का नियम:\nसंख्या का अंतिम अंक 0 या 5 होना चाहिए।";

            case 6:
                return "6 से विभाज्यता का नियम:\nसंख्या का 2 और 3 दोनों से विभाज्य होना जरूरी है।";

            case 8:
                return "8 से विभाज्यता का नियम:\nअंतिम तीन अंकों से बनी संख्या को देखें। वह 8 से विभाज्य हो तो पूरी संख्या 8 से विभाज्य होगी।";

            case 9:
                return "9 से विभाज्यता का नियम:\nसभी अंकों का योग करें। यदि योग 9 से विभाज्य है, तो पूरी संख्या भी 9 से विभाज्य होगी।";

            case 10:
                return "10 से विभाज्यता का नियम:\nसंख्या का अंतिम अंक 0 होना चाहिए।";

            case 11:
                return "11 से विभाज्यता का नियम:\nएकांतर स्थानों के अंकों के योगों का अंतर निकालें। अंतर 0 या 11 का गुणज हो तो संख्या 11 से विभाज्य होती है।";

            default:
                return divisor
                        + " से विभाज्यता जाँचने के लिए संख्या को "
                        + divisor
                        + " से भाग दें। शेषफल 0 हो तो संख्या विभाज्य है।";
        }
    }

    @NonNull
    private static List<BigInteger> extractIntegers(
            @NonNull String question
    ) {
        List<BigInteger> numbers =
                new ArrayList<>();

        Matcher matcher =
                INTEGER_PATTERN.matcher(
                        question
                );

        while (matcher.find()) {
            try {
                numbers.add(
                        new BigInteger(
                                matcher.group()
                        )
                );

            } catch (NumberFormatException ignored) {
                /*
                 * Unsupported number is ignored.
                 */
            }
        }

        return numbers;
    }

    private static int calculateDigitSum(
            @NonNull String digits
    ) {
        int sum =
                0;

        for (int index = 0;
             index < digits.length();
             index++) {

            char character =
                    digits.charAt(
                            index
                    );

            if (Character.isDigit(
                    character
            )) {
                sum +=
                        character - '0';
            }
        }

        return sum;
    }

    @NonNull
    private static AlternatingSum calculateAlternatingSum(
            @NonNull String digits
    ) {
        int firstPlaceSum =
                0;

        int secondPlaceSum =
                0;

        boolean addToFirst =
                true;

        for (int index =
             digits.length() - 1;
             index >= 0;
             index--) {

            char character =
                    digits.charAt(
                            index
                    );

            if (!Character.isDigit(
                    character
            )) {
                continue;
            }

            int digit =
                    character - '0';

            if (addToFirst) {
                firstPlaceSum +=
                        digit;

            } else {
                secondPlaceSum +=
                        digit;
            }

            addToFirst =
                    !addToFirst;
        }

        return new AlternatingSum(
                firstPlaceSum,
                secondPlaceSum
        );
    }

    @NonNull
    private static String lastDigits(
            @NonNull String digits,
            int requiredLength
    ) {
        if (digits.length()
                <= requiredLength) {

            return digits;
        }

        return digits.substring(
                digits.length()
                        - requiredLength
        );
    }

    private static int safeSmallInt(
            @NonNull BigInteger value
    ) {
        if (value.compareTo(
                BigInteger.valueOf(
                        Integer.MAX_VALUE
                )
        ) > 0) {

            return -1;
        }

        return value.intValue();
    }

    private static boolean containsAny(
            @NonNull String source,
            @NonNull String[] keywords
    ) {
        for (String keyword : keywords) {
            if (source.contains(
                    keyword.toLowerCase(
                            Locale.ROOT
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private static LanguageMode detectLanguageMode(
            @Nullable String explanationLanguage
    ) {
        String normalizedLanguage =
                safeText(
                        explanationLanguage
                )
                        .toLowerCase(
                                Locale.ROOT
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

    @NonNull
    private static String normalizeQuestion(
            @Nullable String question
    ) {
        return safeText(
                question
        )
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

    private static final class AlternatingSum {

        private final int firstPlaceSum;

        private final int secondPlaceSum;

        @NonNull
        private final BigInteger difference;

        private AlternatingSum(
                int firstPlaceSum,
                int secondPlaceSum
        ) {
            this.firstPlaceSum =
                    firstPlaceSum;

            this.secondPlaceSum =
                    secondPlaceSum;

            difference =
                    BigInteger.valueOf(
                            firstPlaceSum
                                    - secondPlaceSum
                    );
        }
    }

    /**
     * Offline divisibility solver का immutable result।
     */
    public static final class SolveResult {

        private final boolean solved;

        private final boolean ruleAnswer;

        private final boolean divisible;

        @Nullable
        private final BigInteger dividend;

        @Nullable
        private final BigInteger divisor;

        @Nullable
        private final BigInteger remainder;

        @NonNull
        private final String answerText;

        @NonNull
        private final String diagnosticReason;

        private SolveResult(
                boolean solved,
                boolean ruleAnswer,
                boolean divisible,
                @Nullable BigInteger dividend,
                @Nullable BigInteger divisor,
                @Nullable BigInteger remainder,
                @NonNull String answerText,
                @NonNull String diagnosticReason
        ) {
            this.solved =
                    solved;

            this.ruleAnswer =
                    ruleAnswer;

            this.divisible =
                    divisible;

            this.dividend =
                    dividend;

            this.divisor =
                    divisor;

            this.remainder =
                    remainder;

            this.answerText =
                    answerText;

            this.diagnosticReason =
                    diagnosticReason;
        }

        @NonNull
        private static SolveResult solvedRule(
                @NonNull BigInteger divisor,
                @NonNull String answerText
        ) {
            return new SolveResult(
                    true,
                    true,
                    false,
                    null,
                    divisor,
                    null,
                    answerText,
                    ""
            );
        }

        @NonNull
        private static SolveResult solvedCheck(
                @NonNull BigInteger dividend,
                @NonNull BigInteger divisor,
                @NonNull BigInteger remainder,
                boolean divisible,
                @NonNull String answerText
        ) {
            return new SolveResult(
                    true,
                    false,
                    divisible,
                    dividend,
                    divisor,
                    remainder,
                    answerText,
                    ""
            );
        }

        @NonNull
        private static SolveResult notSolved(
                @NonNull String diagnosticReason
        ) {
            return new SolveResult(
                    false,
                    false,
                    false,
                    null,
                    null,
                    null,
                    "",
                    diagnosticReason
            );
        }

        public boolean isSolved() {
            return solved;
        }

        public boolean isRuleAnswer() {
            return ruleAnswer;
        }

        public boolean isDivisible() {
            return divisible;
        }

        @Nullable
        public BigInteger getDividend() {
            return dividend;
        }

        @Nullable
        public BigInteger getDivisor() {
            return divisor;
        }

        @Nullable
        public BigInteger getRemainder() {
            return remainder;
        }

        @NonNull
        public String getAnswerText() {
            return answerText;
        }

        @NonNull
        public String getDiagnosticReason() {
            return diagnosticReason;
        }
    }
}