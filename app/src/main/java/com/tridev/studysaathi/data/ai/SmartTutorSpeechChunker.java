package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Study Saathi Text-to-Speech के लिए long answer chunking utility।
 *
 * Android Text-to-Speech engine को बहुत बड़ा text एक साथ देने के बजाय
 * यह class उत्तर को छोटे और प्राकृतिक speech chunks में बाँटती है।
 *
 * Chunking priority:
 *
 * 1. Paragraph boundary
 * 2. Sentence boundary
 * 3. Comma / semicolon boundary
 * 4. Word boundary
 * 5. Hard character split
 *
 * यह class:
 *
 * - Answer का meaning नहीं बदलती।
 * - Words को सामान्य स्थिति में बीच से नहीं तोड़ती।
 * - Hindi और English punctuation दोनों समझती है।
 * - Empty chunks return नहीं करती।
 * - Original chunk order सुरक्षित रखती है।
 */
public final class SmartTutorSpeechChunker {

    /**
     * Speech के लिए सुरक्षित default chunk size।
     *
     * यह Android TTS की maximum limit से काफी नीचे रखा गया है,
     * ताकि अलग-अलग devices और speech engines पर stability बनी रहे।
     */
    public static final int DEFAULT_MAX_CHUNK_LENGTH =
            3000;

    /**
     * बहुत छोटा chunk size देने से बचने के लिए minimum सीमा।
     */
    private static final int MIN_ALLOWED_CHUNK_LENGTH =
            200;

    /**
     * एक chunk के अंत के पास natural split खोजने की सीमा।
     */
    private static final int NATURAL_BOUNDARY_SEARCH_RANGE =
            700;

    private SmartTutorSpeechChunker() {
        /*
         * Utility class है। Object बनाने की आवश्यकता नहीं है।
         */
    }

    /**
     * Default safe size के अनुसार speech chunks बनाता है।
     */
    @NonNull
    public static List<String> createSpeechChunks(
            @Nullable String answerText
    ) {
        return createSpeechChunks(
                answerText,
                DEFAULT_MAX_CHUNK_LENGTH
        );
    }

    /**
     * दिए गए maximum chunk size के अनुसार speech chunks बनाता है।
     *
     * @param answerText     साफ किया हुआ educational answer
     * @param maxChunkLength एक chunk की अधिकतम character length
     */
    @NonNull
    public static List<String> createSpeechChunks(
            @Nullable String answerText,
            int maxChunkLength
    ) {
        String normalizedText =
                normalizeText(
                        answerText
                );

        if (normalizedText.isEmpty()) {
            return Collections.emptyList();
        }

        int safeMaxLength =
                Math.max(
                        MIN_ALLOWED_CHUNK_LENGTH,
                        maxChunkLength
                );

        if (normalizedText.length()
                <= safeMaxLength) {

            return Collections.singletonList(
                    normalizedText
            );
        }

        List<String> chunks =
                new ArrayList<>();

        splitTextRecursively(
                normalizedText,
                safeMaxLength,
                chunks
        );

        removeEmptyChunks(
                chunks
        );

        return Collections.unmodifiableList(
                chunks
        );
    }

    /**
     * Text को recursively सुरक्षित boundaries पर split करता है।
     */
    private static void splitTextRecursively(
            @NonNull String text,
            int maxChunkLength,
            @NonNull List<String> outputChunks
    ) {
        String safeText =
                text.trim();

        if (safeText.isEmpty()) {
            return;
        }

        if (safeText.length()
                <= maxChunkLength) {

            outputChunks.add(
                    safeText
            );

            return;
        }

        int splitPosition =
                findBestSplitPosition(
                        safeText,
                        maxChunkLength
                );

        if (splitPosition <= 0
                || splitPosition
                >= safeText.length()) {

            splitPosition =
                    Math.min(
                            maxChunkLength,
                            safeText.length()
                    );
        }

        String firstPart =
                safeText.substring(
                                0,
                                splitPosition
                        )
                        .trim();

        String remainingPart =
                safeText.substring(
                                splitPosition
                        )
                        .trim();

        if (!firstPart.isEmpty()) {
            if (firstPart.length()
                    <= maxChunkLength) {

                outputChunks.add(
                        firstPart
                );

            } else {
                splitTextRecursively(
                        firstPart,
                        maxChunkLength,
                        outputChunks
                );
            }
        }

        if (!remainingPart.isEmpty()) {
            splitTextRecursively(
                    remainingPart,
                    maxChunkLength,
                    outputChunks
            );
        }
    }

    /**
     * Maximum length के पास सबसे प्राकृतिक split point खोजता है।
     */
    private static int findBestSplitPosition(
            @NonNull String text,
            int maxChunkLength
    ) {
        int preferredEnd =
                Math.min(
                        maxChunkLength,
                        text.length()
                );

        int searchStart =
                Math.max(
                        0,
                        preferredEnd
                                - NATURAL_BOUNDARY_SEARCH_RANGE
                );

        /*
         * सबसे पहले paragraph boundary खोजें।
         */
        int paragraphBoundary =
                findLastBoundary(
                        text,
                        preferredEnd,
                        searchStart,
                        "\n\n"
                );

        if (paragraphBoundary > 0) {
            return paragraphBoundary;
        }

        /*
         * फिर line boundary खोजें।
         */
        int lineBoundary =
                findLastBoundary(
                        text,
                        preferredEnd,
                        searchStart,
                        "\n"
                );

        if (lineBoundary > 0) {
            return lineBoundary;
        }

        /*
         * Hindi और English sentence endings।
         */
        int sentenceBoundary =
                findLastSentenceBoundary(
                        text,
                        preferredEnd,
                        searchStart
                );

        if (sentenceBoundary > 0) {
            return sentenceBoundary;
        }

        /*
         * Comma या semicolon जैसे छोटे pause points।
         */
        int pauseBoundary =
                findLastPauseBoundary(
                        text,
                        preferredEnd,
                        searchStart
                );

        if (pauseBoundary > 0) {
            return pauseBoundary;
        }

        /*
         * कम से कम word boundary पर split करें।
         */
        int wordBoundary =
                findLastWhitespaceBoundary(
                        text,
                        preferredEnd,
                        searchStart
                );

        if (wordBoundary > 0) {
            return wordBoundary;
        }

        /*
         * बहुत लंबे continuous word/URL जैसी स्थिति में hard split।
         */
        return preferredEnd;
    }

    /**
     * दिए गए separator का अंतिम सुरक्षित स्थान खोजता है।
     */
    private static int findLastBoundary(
            @NonNull String text,
            int preferredEnd,
            int searchStart,
            @NonNull String separator
    ) {
        int searchEnd =
                Math.min(
                        preferredEnd,
                        text.length()
                );

        String searchablePart =
                text.substring(
                        searchStart,
                        searchEnd
                );

        int relativePosition =
                searchablePart.lastIndexOf(
                        separator
                );

        if (relativePosition < 0) {
            return -1;
        }

        return searchStart
                + relativePosition
                + separator.length();
    }

    /**
     * Sentence-ending punctuation के बाद split point खोजता है।
     */
    private static int findLastSentenceBoundary(
            @NonNull String text,
            int preferredEnd,
            int searchStart
    ) {
        for (int index = preferredEnd - 1;
             index >= searchStart;
             index--) {

            char character =
                    text.charAt(
                            index
                    );

            if (isSentenceEndingCharacter(
                    character
            )) {
                return index + 1;
            }
        }

        return -1;
    }

    /**
     * Comma, semicolon और colon जैसे natural pause points खोजता है।
     */
    private static int findLastPauseBoundary(
            @NonNull String text,
            int preferredEnd,
            int searchStart
    ) {
        for (int index = preferredEnd - 1;
             index >= searchStart;
             index--) {

            char character =
                    text.charAt(
                            index
                    );

            if (character == ','
                    || character == '،'
                    || character == ';'
                    || character == ':') {

                return index + 1;
            }
        }

        return -1;
    }

    /**
     * अंतिम whitespace boundary खोजता है।
     */
    private static int findLastWhitespaceBoundary(
            @NonNull String text,
            int preferredEnd,
            int searchStart
    ) {
        for (int index = preferredEnd - 1;
             index >= searchStart;
             index--) {

            if (Character.isWhitespace(
                    text.charAt(
                            index
                    )
            )) {
                return index + 1;
            }
        }

        return -1;
    }

    /**
     * Hindi और English sentence endings।
     */
    private static boolean isSentenceEndingCharacter(
            char character
    ) {
        return character == '.'
                || character == '।'
                || character == '॥'
                || character == '?'
                || character == '!'
                || character == '…';
    }

    /**
     * Input text को speech chunking के लिए normalize करता है।
     *
     * Paragraphs सुरक्षित रहते हैं, लेकिन unnecessary spaces हटते हैं।
     */
    @NonNull
    private static String normalizeText(
            @Nullable String text
    ) {
        if (text == null) {
            return "";
        }

        String normalized =
                text.replace(
                                "\r\n",
                                "\n"
                        )
                        .replace(
                                '\r',
                                '\n'
                        )
                        .trim();

        if (normalized.isEmpty()) {
            return "";
        }

        /*
         * Line के अंदर multiple spaces को single space में बदलें।
         */
        normalized =
                normalized.replaceAll(
                        "[\\t ]+",
                        " "
                );

        /*
         * तीन या अधिक blank lines को एक paragraph gap में बदलें।
         */
        normalized =
                normalized.replaceAll(
                        "\\n[\\t ]*\\n(?:[\\t ]*\\n)+",
                        "\n\n"
                );

        /*
         * प्रत्येक line के आगे-पीछे की spaces हटाएँ।
         */
        String[] lines =
                normalized.split(
                        "\\n",
                        -1
                );

        StringBuilder builder =
                new StringBuilder();

        for (String line : lines) {
            String cleanLine =
                    line.trim();

            if (builder.length() > 0) {
                builder.append(
                        '\n'
                );
            }

            builder.append(
                    cleanLine
            );
        }

        return builder.toString()
                .trim();
    }

    /**
     * Defensive cleanup: blank chunks हटाता है।
     */
    private static void removeEmptyChunks(
            @NonNull List<String> chunks
    ) {
        for (int index = chunks.size() - 1;
             index >= 0;
             index--) {

            String chunk =
                    chunks.get(
                            index
                    );

            if (chunk == null
                    || chunk.trim().isEmpty()) {

                chunks.remove(
                        index
                );

            } else {
                chunks.set(
                        index,
                        chunk.trim()
                );
            }
        }
    }
}