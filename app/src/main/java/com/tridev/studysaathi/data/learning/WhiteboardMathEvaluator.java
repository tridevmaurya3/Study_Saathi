package com.tridev.studysaathi.data.learning;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Safe offline arithmetic preview for handwritten +, -, ×, ÷ and parentheses. */
public final class WhiteboardMathEvaluator {
    private WhiteboardMathEvaluator() { }

    public static String evaluate(String input) {
        if (input == null) return "";
        String value = input.trim().replace('×', '*').replace('÷', '/').replace(" ", "");
        if (value.endsWith("=")) value = value.substring(0, value.length() - 1);
        if (value.isEmpty() || !value.matches("[0-9.+\\-*/()]+")) return "";
        try {
            Parser parser = new Parser(value); double answer = parser.expression();
            if (!parser.finished() || !Double.isFinite(answer)) return "";
            return BigDecimal.valueOf(answer).setScale(8, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString();
        } catch (RuntimeException ignored) { return ""; }
    }

    private static final class Parser {
        private final String text; private int index;
        Parser(String text) { this.text = text; }
        boolean finished() { return index == text.length(); }
        double expression() {
            double value = term();
            while (index < text.length()) {
                char op = text.charAt(index);
                if (op != '+' && op != '-') break;
                index++; double right = term(); value = op == '+' ? value + right : value - right;
            }
            return value;
        }
        double term() {
            double value = factor();
            while (index < text.length()) {
                char op = text.charAt(index);
                if (op != '*' && op != '/') break;
                index++; double right = factor();
                if (op == '/' && right == 0d) throw new ArithmeticException();
                value = op == '*' ? value * right : value / right;
            }
            return value;
        }
        double factor() {
            if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-'))
                return text.charAt(index++) == '-' ? -factor() : factor();
            if (index < text.length() && text.charAt(index) == '(') {
                index++; double value = expression();
                if (index >= text.length() || text.charAt(index++) != ')') throw new IllegalArgumentException();
                return value;
            }
            int start = index; boolean dot = false;
            while (index < text.length()) {
                char c = text.charAt(index);
                if (c == '.' && !dot) { dot = true; index++; }
                else if (Character.isDigit(c)) index++; else break;
            }
            if (start == index) throw new IllegalArgumentException();
            return Double.parseDouble(text.substring(start, index));
        }
    }
}
