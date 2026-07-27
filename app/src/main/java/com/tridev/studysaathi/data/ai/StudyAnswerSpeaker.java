package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Study Saathi Smart AI answers को आवाज में पढ़ने वाला
 * offline-first Text-to-Speech controller।
 *
 * मुख्य सुविधाएँ:
 *
 * 1. उपलब्ध installed voices में से offline voice चुनना।
 * 2. Network-required TTS voice को उपयोग न करना।
 * 3. Hindi, English और Sanskrit text support।
 * 4. Sanskrit के लिए Hindi offline voice fallback।
 * 5. लंबे AI answer को छोटे TTS-safe chunks में बाँटना।
 * 6. Markdown और सामान्य LaTeX markers हटाकर साफ text बोलना।
 * 7. Speech start, complete, stop और friendly error callbacks।
 * 8. Network timeout आने पर एक सुरक्षित automatic retry।
 * 9. Activity बंद होने पर TTS resources release करना।
 */
public final class StudyAnswerSpeaker
        implements TextToSpeech.OnInitListener {

    private static final float DEFAULT_SPEECH_RATE =
            0.90f;

    private static final float DEFAULT_SPEECH_PITCH =
            1.00f;

    /**
     * छोटे chunks offline TTS engines पर अधिक stable रहते हैं।
     */
    private static final int MAXIMUM_SPEECH_CHUNK_LENGTH =
            Math.max(
                    600,
                    Math.min(
                            1200,
                            TextToSpeech.getMaxSpeechInputLength()
                                    - 200
                    )
            );

    private static final int MAXIMUM_CHUNK_RETRY_COUNT =
            1;

    @NonNull
    private final Context applicationContext;

    @NonNull
    private final Executor mainExecutor;

    @Nullable
    private TextToSpeech textToSpeech;

    @Nullable
    private PendingSpeech pendingSpeech;

    @Nullable
    private SpeechCallback activeCallback;

    @Nullable
    private Voice activeOfflineVoice;

    @NonNull
    private List<String> activeSpeechChunks =
            Collections.emptyList();

    @Nullable
    private String activeSessionId;

    @Nullable
    private String activeUtteranceId;

    private int activeChunkIndex;

    private int currentChunkRetryCount;

    private boolean initialized;

    private boolean released;

    private boolean speaking;

    private boolean speechStartCallbackSent;

    public StudyAnswerSpeaker(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        mainExecutor =
                ContextCompat.getMainExecutor(
                        applicationContext
                );

        textToSpeech =
                new TextToSpeech(
                        applicationContext,
                        this
                );
    }

    /**
     * Android TextToSpeech initialization result।
     */
    @Override
    public void onInit(
            int status
    ) {
        if (released) {
            return;
        }

        TextToSpeech currentTextToSpeech =
                textToSpeech;

        if (currentTextToSpeech == null) {
            notifyInitializationError(
                    "Text-to-Speech engine उपलब्ध नहीं है।"
            );

            return;
        }

        if (status
                != TextToSpeech.SUCCESS) {

            notifyInitializationError(
                    "Text-to-Speech शुरू नहीं हो सकी।"
            );

            return;
        }

        initialized =
                true;

        currentTextToSpeech.setSpeechRate(
                DEFAULT_SPEECH_RATE
        );

        currentTextToSpeech.setPitch(
                DEFAULT_SPEECH_PITCH
        );

        currentTextToSpeech.setOnUtteranceProgressListener(
                createUtteranceProgressListener()
        );

        PendingSpeech waitingSpeech =
                pendingSpeech;

        pendingSpeech =
                null;

        if (waitingSpeech != null) {
            startPreparedSpeech(
                    waitingSpeech
            );
        }
    }

    /**
     * Smart AI answer को आवाज में पढ़ना शुरू करता है।
     */
    public void speak(
            @NonNull String answerText,
            @Nullable String preferredLanguage,
            @NonNull SpeechCallback callback
    ) {
        if (released) {
            callback.onError(
                    "Answer speaker बंद हो चुका है।"
            );

            return;
        }

        String preparedText =
                prepareTextForSpeech(
                        answerText
                );

        if (preparedText.isEmpty()) {
            callback.onError(
                    "बोलने के लिए answer उपलब्ध नहीं है।"
            );

            return;
        }

        stopCurrentSpeech(
                false
        );

        PendingSpeech newSpeech =
                new PendingSpeech(
                        preparedText,
                        safeText(
                                preferredLanguage
                        ),
                        callback
                );

        if (!initialized) {
            pendingSpeech =
                    newSpeech;

            callback.onPreparing();

            return;
        }

        startPreparedSpeech(
                newSpeech
        );
    }

    /**
     * Offline voice और answer chunks तैयार करके speech शुरू करता है।
     */
    private void startPreparedSpeech(
            @NonNull PendingSpeech speech
    ) {
        TextToSpeech currentTextToSpeech =
                textToSpeech;

        if (released
                || currentTextToSpeech == null) {

            speech.callback.onError(
                    "Text-to-Speech engine उपलब्ध नहीं है।"
            );

            return;
        }

        Voice selectedOfflineVoice =
                selectBestOfflineVoice(
                        currentTextToSpeech,
                        speech.preferredLanguage,
                        speech.text
                );

        if (selectedOfflineVoice == null) {
            speech.callback.onError(
                    createOfflineVoiceMissingMessage(
                            speech.text
                    )
            );

            return;
        }

        int voiceResult =
                currentTextToSpeech.setVoice(
                        selectedOfflineVoice
                );

        if (voiceResult
                == TextToSpeech.ERROR) {

            speech.callback.onError(
                    "Installed offline voice को चालू नहीं किया जा सका।"
            );

            return;
        }

        currentTextToSpeech.setSpeechRate(
                DEFAULT_SPEECH_RATE
        );

        currentTextToSpeech.setPitch(
                DEFAULT_SPEECH_PITCH
        );

        List<String> speechChunks =
                splitTextForSpeech(
                        speech.text
                );

        if (speechChunks.isEmpty()) {
            speech.callback.onError(
                    "Answer को पढ़ने योग्य हिस्सों में तैयार नहीं किया जा सका।"
            );

            return;
        }

        activeOfflineVoice =
                selectedOfflineVoice;

        activeSessionId =
                "study_saathi_"
                        + UUID.randomUUID();

        activeCallback =
                speech.callback;

        activeSpeechChunks =
                speechChunks;

        activeChunkIndex =
                0;

        currentChunkRetryCount =
                0;

        speechStartCallbackSent =
                false;

        speaking =
                true;

        speakCurrentChunk();
    }

    /**
     * उपलब्ध voices में से केवल offline voice चुनता है।
     */
    @Nullable
    private Voice selectBestOfflineVoice(
            @NonNull TextToSpeech currentTextToSpeech,
            @NonNull String preferredLanguage,
            @NonNull String answerText
    ) {
        Set<Voice> availableVoices;

        try {
            availableVoices =
                    currentTextToSpeech.getVoices();

        } catch (RuntimeException exception) {
            return null;
        }

        if (availableVoices == null
                || availableVoices.isEmpty()) {

            return null;
        }

        List<Locale> preferredLocales =
                createPreferredLocales(
                        preferredLanguage,
                        answerText
                );

        Voice bestVoice =
                null;

        long bestScore =
                Long.MIN_VALUE;

        for (Voice voice :
                availableVoices) {

            if (voice == null) {
                continue;
            }

            /*
             * यही मुख्य Error -7 fix है।
             * Network voice को पूरी तरह छोड़ दिया जाता है।
             */
            if (voice.isNetworkConnectionRequired()) {
                continue;
            }

            Locale voiceLocale =
                    voice.getLocale();

            if (voiceLocale == null) {
                continue;
            }

            int localeScore =
                    calculateLocaleScore(
                            voiceLocale,
                            preferredLocales
                    );

            if (localeScore < 0) {
                continue;
            }

            long totalScore =
                    localeScore
                            + (
                            voice.getQuality()
                                    * 10L
                    )
                            - voice.getLatency();

            if (bestVoice == null
                    || totalScore > bestScore) {

                bestVoice =
                        voice;

                bestScore =
                        totalScore;
            }
        }

        return bestVoice;
    }

    /**
     * Answer language के अनुसार preferred locale क्रम तैयार करता है।
     */
    @NonNull
    private List<Locale> createPreferredLocales(
            @NonNull String preferredLanguage,
            @NonNull String answerText
    ) {
        List<Locale> preferredLocales =
                new ArrayList<>();

        String normalizedLanguage =
                preferredLanguage.toLowerCase(
                        Locale.ROOT
                );

        boolean sanskritPreferred =
                normalizedLanguage.contains(
                        "sanskrit"
                )
                        || normalizedLanguage.contains(
                        "संस्कृत"
                );

        boolean devanagariText =
                containsDevanagari(
                        answerText
                );

        boolean englishOnly =
                normalizedLanguage.contains(
                        "english"
                )
                        && !normalizedLanguage.contains(
                        "hindi"
                )
                        && !normalizedLanguage.contains(
                        "हिंदी"
                )
                        && !normalizedLanguage.contains(
                        "हिन्दी"
                )
                        && !normalizedLanguage.contains(
                        "bilingual"
                )
                        && !normalizedLanguage.contains(
                        "hinglish"
                )
                        && !devanagariText;

        if (sanskritPreferred) {
            addLocaleIfMissing(
                    preferredLocales,
                    new Locale(
                            "sa",
                            "IN"
                    )
            );

            addLocaleIfMissing(
                    preferredLocales,
                    new Locale(
                            "sa"
                    )
            );

            /*
             * अधिकांश phones में Sanskrit की dedicated offline voice
             * नहीं होती। इसलिए Hindi voice fallback।
             */
            addLocaleIfMissing(
                    preferredLocales,
                    new Locale(
                            "hi",
                            "IN"
                    )
            );

            addLocaleIfMissing(
                    preferredLocales,
                    new Locale(
                            "hi"
                    )
            );

            return preferredLocales;
        }

        if (devanagariText
                || !englishOnly) {

            addLocaleIfMissing(
                    preferredLocales,
                    new Locale(
                            "hi",
                            "IN"
                    )
            );

            addLocaleIfMissing(
                    preferredLocales,
                    new Locale(
                            "hi"
                    )
            );

            return preferredLocales;
        }

        addLocaleIfMissing(
                preferredLocales,
                new Locale(
                        "en",
                        "IN"
                )
        );

        addLocaleIfMissing(
                preferredLocales,
                Locale.US
        );

        addLocaleIfMissing(
                preferredLocales,
                new Locale(
                        "en"
                )
        );

        return preferredLocales;
    }

    private void addLocaleIfMissing(
            @NonNull List<Locale> locales,
            @NonNull Locale newLocale
    ) {
        for (Locale existingLocale :
                locales) {

            if (existingLocale.toLanguageTag()
                    .equalsIgnoreCase(
                            newLocale.toLanguageTag()
                    )) {

                return;
            }
        }

        locales.add(
                newLocale
        );
    }

    /**
     * Voice locale को preferred locale list के अनुसार score देता है।
     */
    private int calculateLocaleScore(
            @NonNull Locale voiceLocale,
            @NonNull List<Locale> preferredLocales
    ) {
        String voiceLanguage =
                safeText(
                        voiceLocale.getLanguage()
                );

        String voiceCountry =
                safeText(
                        voiceLocale.getCountry()
                );

        if (voiceLanguage.isEmpty()) {
            return -1;
        }

        for (int index = 0;
             index < preferredLocales.size();
             index++) {

            Locale preferredLocale =
                    preferredLocales.get(
                            index
                    );

            String preferredLanguage =
                    safeText(
                            preferredLocale.getLanguage()
                    );

            if (!voiceLanguage.equalsIgnoreCase(
                    preferredLanguage
            )) {
                continue;
            }

            int score =
                    100000
                            - (
                            index
                                    * 10000
                    );

            String preferredCountry =
                    safeText(
                            preferredLocale.getCountry()
                    );

            if (!preferredCountry.isEmpty()
                    && voiceCountry.equalsIgnoreCase(
                    preferredCountry
            )) {

                score +=
                        5000;

            } else if (preferredCountry.isEmpty()) {
                score +=
                        2500;
            }

            return score;
        }

        return -1;
    }

    /**
     * Offline voice उपलब्ध न होने का स्पष्ट message।
     */
    @NonNull
    private String createOfflineVoiceMissingMessage(
            @NonNull String answerText
    ) {
        if (containsDevanagari(
                answerText
        )) {

            return "Hindi offline Text-to-Speech voice इस device में installed नहीं है। "
                    + "Phone Settings में Text-to-Speech की Hindi (India) voice download करें।";
        }

        return "English offline Text-to-Speech voice इस device में installed नहीं है। "
                + "Phone Settings में English (India) voice download करें।";
    }

    /**
     * वर्तमान chunk को TTS engine में भेजता है।
     */
    private void speakCurrentChunk() {
        TextToSpeech currentTextToSpeech =
                textToSpeech;

        String currentSessionId =
                activeSessionId;

        SpeechCallback callback =
                activeCallback;

        if (released
                || currentTextToSpeech == null
                || currentSessionId == null
                || callback == null
                || activeChunkIndex < 0
                || activeChunkIndex
                >= activeSpeechChunks.size()) {

            handleActiveSpeechError(
                    "Answer बोलना जारी नहीं रखा जा सका।"
            );

            return;
        }

        Voice selectedVoice =
                activeOfflineVoice;

        if (selectedVoice == null
                || selectedVoice
                .isNetworkConnectionRequired()) {

            handleActiveSpeechError(
                    "Offline Text-to-Speech voice उपलब्ध नहीं है।"
            );

            return;
        }

        /*
         * प्रत्येक chunk से पहले offline voice दोबारा set करने से
         * engine किसी default network voice पर वापस नहीं जाता।
         */
        int voiceResult =
                currentTextToSpeech.setVoice(
                        selectedVoice
                );

        if (voiceResult
                == TextToSpeech.ERROR) {

            handleActiveSpeechError(
                    "Offline voice को उपयोग नहीं किया जा सका।"
            );

            return;
        }

        String currentChunk =
                activeSpeechChunks.get(
                        activeChunkIndex
                );

        String utteranceId =
                currentSessionId
                        + "_chunk_"
                        + activeChunkIndex
                        + "_try_"
                        + currentChunkRetryCount;

        activeUtteranceId =
                utteranceId;

        Bundle speechParameters =
                new Bundle();

        int speakResult =
                currentTextToSpeech.speak(
                        currentChunk,
                        TextToSpeech.QUEUE_FLUSH,
                        speechParameters,
                        utteranceId
                );

        if (speakResult
                == TextToSpeech.ERROR) {

            handleActiveSpeechError(
                    "Answer बोलना शुरू नहीं हो सका।"
            );
        }
    }

    /**
     * TTS progress callbacks।
     */
    @NonNull
    private UtteranceProgressListener
    createUtteranceProgressListener() {

        return new UtteranceProgressListener() {

            @Override
            public void onStart(
                    String utteranceId
            ) {
                mainExecutor.execute(() ->
                        handleUtteranceStarted(
                                utteranceId
                        )
                );
            }

            @Override
            public void onDone(
                    String utteranceId
            ) {
                mainExecutor.execute(() ->
                        handleUtteranceCompleted(
                                utteranceId
                        )
                );
            }

            @Override
            public void onError(
                    String utteranceId
            ) {
                mainExecutor.execute(() ->
                        handleUtteranceError(
                                utteranceId,
                                TextToSpeech.ERROR
                        )
                );
            }

            @Override
            public void onError(
                    String utteranceId,
                    int errorCode
            ) {
                mainExecutor.execute(() ->
                        handleUtteranceError(
                                utteranceId,
                                errorCode
                        )
                );
            }

            @Override
            public void onStop(
                    String utteranceId,
                    boolean interrupted
            ) {
                /*
                 * Manual stop को stop() method स्वयं manage करती है।
                 */
            }
        };
    }

    private void handleUtteranceStarted(
            @Nullable String utteranceId
    ) {
        if (!isActiveUtterance(
                utteranceId
        )) {
            return;
        }

        if (speechStartCallbackSent) {
            return;
        }

        speechStartCallbackSent =
                true;

        SpeechCallback callback =
                activeCallback;

        if (callback != null) {
            callback.onStarted();
        }
    }

    private void handleUtteranceCompleted(
            @Nullable String utteranceId
    ) {
        if (!isActiveUtterance(
                utteranceId
        )) {
            return;
        }

        int nextChunkIndex =
                activeChunkIndex + 1;

        if (nextChunkIndex
                < activeSpeechChunks.size()) {

            activeChunkIndex =
                    nextChunkIndex;

            currentChunkRetryCount =
                    0;

            speakCurrentChunk();

            return;
        }

        SpeechCallback callback =
                activeCallback;

        clearActiveSpeechState();

        if (callback != null) {
            callback.onCompleted();
        }
    }

    private void handleUtteranceError(
            @Nullable String utteranceId,
            int errorCode
    ) {
        if (!isActiveUtterance(
                utteranceId
        )) {
            return;
        }

        boolean networkRelatedError =
                errorCode
                        == TextToSpeech.ERROR_NETWORK
                        || errorCode
                        == TextToSpeech.ERROR_NETWORK_TIMEOUT;

        /*
         * Engine ने offline voice के बावजूद temporary timeout दिया,
         * तो उसी छोटे chunk को केवल एक बार दोबारा चलाएँ।
         */
        if (networkRelatedError
                && currentChunkRetryCount
                < MAXIMUM_CHUNK_RETRY_COUNT) {

            currentChunkRetryCount++;

            TextToSpeech currentTextToSpeech =
                    textToSpeech;

            if (currentTextToSpeech != null) {
                currentTextToSpeech.stop();
            }

            speakCurrentChunk();

            return;
        }

        handleActiveSpeechError(
                createReadableTtsErrorMessage(
                        errorCode
                )
        );
    }

    @NonNull
    private String createReadableTtsErrorMessage(
            int errorCode
    ) {
        switch (errorCode) {
            case TextToSpeech.ERROR_NETWORK_TIMEOUT:
                return "Text-to-Speech voice का network timeout हुआ। "
                        + "Hindi offline voice data download करके दोबारा सुनें।";

            case TextToSpeech.ERROR_NETWORK:
                return "Text-to-Speech network सेवा उपलब्ध नहीं है। "
                        + "Offline voice data install करें।";

            case TextToSpeech.ERROR_NOT_INSTALLED_YET:
                return "Text-to-Speech voice अभी पूरी तरह download नहीं हुई है।";

            case TextToSpeech.ERROR_OUTPUT:
                return "Phone audio output में समस्या के कारण answer नहीं बोला जा सका।";

            case TextToSpeech.ERROR_SERVICE:
                return "Phone की Text-to-Speech service अभी उपलब्ध नहीं है।";

            case TextToSpeech.ERROR_SYNTHESIS:
                return "इस answer को आवाज में बदलने में समस्या आई।";

            case TextToSpeech.ERROR_INVALID_REQUEST:
                return "Text-to-Speech request valid नहीं थी।";

            case TextToSpeech.ERROR:
            default:
                return "Answer बोलते समय Text-to-Speech error आया।";
        }
    }

    private void handleActiveSpeechError(
            @NonNull String errorMessage
    ) {
        SpeechCallback callback =
                activeCallback;

        clearActiveSpeechState();

        if (callback != null) {
            callback.onError(
                    errorMessage
            );
        }
    }

    private boolean isActiveUtterance(
            @Nullable String utteranceId
    ) {
        return utteranceId != null
                && utteranceId.equals(
                activeUtteranceId
        );
    }

    /**
     * वर्तमान answer speech रोकता है।
     */
    public void stop() {
        stopCurrentSpeech(
                true
        );
    }

    private void stopCurrentSpeech(
            boolean notifyCallback
    ) {
        PendingSpeech waitingSpeech =
                pendingSpeech;

        pendingSpeech =
                null;

        SpeechCallback callback =
                activeCallback;

        boolean hadActiveSpeech =
                speaking
                        || waitingSpeech != null;

        if (callback == null
                && waitingSpeech != null) {

            callback =
                    waitingSpeech.callback;
        }

        clearActiveSpeechState();

        TextToSpeech currentTextToSpeech =
                textToSpeech;

        if (currentTextToSpeech != null) {
            currentTextToSpeech.stop();
        }

        if (notifyCallback
                && hadActiveSpeech
                && callback != null) {

            callback.onStopped();
        }
    }

    private void clearActiveSpeechState() {
        speaking =
                false;

        speechStartCallbackSent =
                false;

        activeChunkIndex =
                0;

        currentChunkRetryCount =
                0;

        activeSessionId =
                null;

        activeUtteranceId =
                null;

        activeCallback =
                null;

        activeOfflineVoice =
                null;

        activeSpeechChunks =
                Collections.emptyList();
    }

    public boolean isSpeaking() {
        return speaking
                || pendingSpeech != null;
    }

    /**
     * Activity या Answer View destroy होने पर resources release करें।
     */
    public void shutdown() {
        if (released) {
            return;
        }

        released =
                true;

        pendingSpeech =
                null;

        clearActiveSpeechState();

        TextToSpeech currentTextToSpeech =
                textToSpeech;

        textToSpeech =
                null;

        initialized =
                false;

        if (currentTextToSpeech != null) {
            currentTextToSpeech.stop();
            currentTextToSpeech.shutdown();
        }
    }

    private void notifyInitializationError(
            @NonNull String errorMessage
    ) {
        initialized =
                false;

        PendingSpeech waitingSpeech =
                pendingSpeech;

        pendingSpeech =
                null;

        if (waitingSpeech != null) {
            mainExecutor.execute(
                    () -> waitingSpeech.callback.onError(
                            errorMessage
                    )
            );
        }
    }

    /**
     * AI answer को speech-friendly text में बदलता है।
     */
    @NonNull
    private String prepareTextForSpeech(
            @Nullable String originalText
    ) {
        String text =
                safeText(
                        originalText
                );

        if (text.isEmpty()) {
            return "";
        }

        return text
                /*
                 * सामान्य LaTeX Mathematics।
                 */
                .replace(
                        "\\times",
                        " गुणा "
                )
                .replace(
                        "\\div",
                        " भाग "
                )
                .replace(
                        "\\pm",
                        " प्लस या माइनस "
                )
                .replace(
                        "\\sqrt",
                        " वर्गमूल "
                )
                .replace(
                        "\\leq",
                        " से छोटा या बराबर "
                )
                .replace(
                        "\\geq",
                        " से बड़ा या बराबर "
                )
                .replace(
                        "$",
                        ""
                )

                /*
                 * Markdown code और emphasis markers।
                 */
                .replace(
                        "```",
                        ""
                )
                .replace(
                        "**",
                        ""
                )
                .replace(
                        "__",
                        ""
                )
                .replace(
                        "`",
                        ""
                )

                /*
                 * Markdown links में केवल visible title।
                 */
                .replaceAll(
                        "\\[([^\\]]+)]\\([^)]+\\)",
                        "$1"
                )

                /*
                 * Markdown headings।
                 */
                .replaceAll(
                        "(?m)^\\s*#{1,6}\\s*",
                        ""
                )

                /*
                 * Horizontal divider।
                 */
                .replaceAll(
                        "(?m)^\\s*[-_]{3,}\\s*$",
                        ""
                )

                /*
                 * Quote marker।
                 */
                .replaceAll(
                        "(?m)^\\s*>\\s*",
                        ""
                )

                /*
                 * Bullet marker को pause में बदलना।
                 */
                .replaceAll(
                        "(?m)^\\s*[-*•]\\s+",
                        ". "
                )

                /*
                 * बहुत अधिक blank lines कम करना।
                 */
                .replaceAll(
                        "\\n{3,}",
                        "\n\n"
                )

                /*
                 * Extra spaces साफ करना।
                 */
                .replaceAll(
                        "[ \\t]{2,}",
                        " "
                )
                .trim();
    }

    /**
     * लंबे answer को TTS-safe chunks में बाँटता है।
     */
    @NonNull
    private List<String> splitTextForSpeech(
            @NonNull String text
    ) {
        List<String> chunks =
                new ArrayList<>();

        String normalizedText =
                text
                        .replace(
                                "\r\n",
                                "\n"
                        )
                        .replace(
                                '\r',
                                '\n'
                        )
                        .replaceAll(
                                "\\n{3,}",
                                "\n\n"
                        )
                        .trim();

        if (normalizedText.isEmpty()) {
            return chunks;
        }

        String[] speechPieces =
                normalizedText.split(
                        "(?<=[।.!?])\\s+|\\n+"
                );

        StringBuilder currentChunk =
                new StringBuilder();

        for (String rawPiece :
                speechPieces) {

            String piece =
                    safeText(
                            rawPiece
                    );

            if (piece.isEmpty()) {
                continue;
            }

            appendSpeechPiece(
                    chunks,
                    currentChunk,
                    piece
            );
        }

        flushCurrentChunk(
                chunks,
                currentChunk
        );

        return chunks;
    }

    private void appendSpeechPiece(
            @NonNull List<String> chunks,
            @NonNull StringBuilder currentChunk,
            @NonNull String piece
    ) {
        if (piece.length()
                > MAXIMUM_SPEECH_CHUNK_LENGTH) {

            flushCurrentChunk(
                    chunks,
                    currentChunk
            );

            splitOversizedPiece(
                    chunks,
                    piece
            );

            return;
        }

        int requiredLength =
                currentChunk.length()
                        + (
                        currentChunk.length() > 0
                                ? 1
                                : 0
                )
                        + piece.length();

        if (requiredLength
                > MAXIMUM_SPEECH_CHUNK_LENGTH) {

            flushCurrentChunk(
                    chunks,
                    currentChunk
            );
        }

        if (currentChunk.length() > 0) {
            currentChunk.append(
                    ' '
            );
        }

        currentChunk.append(
                piece
        );
    }

    private void splitOversizedPiece(
            @NonNull List<String> chunks,
            @NonNull String oversizedPiece
    ) {
        int startPosition =
                0;

        while (startPosition
                < oversizedPiece.length()) {

            int maximumEndPosition =
                    Math.min(
                            oversizedPiece.length(),
                            startPosition
                                    + MAXIMUM_SPEECH_CHUNK_LENGTH
                    );

            int safeEndPosition =
                    maximumEndPosition;

            if (maximumEndPosition
                    < oversizedPiece.length()) {

                int whitespacePosition =
                        oversizedPiece.lastIndexOf(
                                ' ',
                                maximumEndPosition
                        );

                int minimumAcceptablePosition =
                        startPosition
                                + (
                                MAXIMUM_SPEECH_CHUNK_LENGTH
                                        / 2
                        );

                if (whitespacePosition
                        > minimumAcceptablePosition) {

                    safeEndPosition =
                            whitespacePosition;
                }
            }

            String chunk =
                    oversizedPiece.substring(
                                    startPosition,
                                    safeEndPosition
                            )
                            .trim();

            if (!chunk.isEmpty()) {
                chunks.add(
                        chunk
                );
            }

            startPosition =
                    safeEndPosition;

            while (startPosition
                    < oversizedPiece.length()
                    && Character.isWhitespace(
                    oversizedPiece.charAt(
                            startPosition
                    )
            )) {
                startPosition++;
            }
        }
    }

    private void flushCurrentChunk(
            @NonNull List<String> chunks,
            @NonNull StringBuilder currentChunk
    ) {
        if (currentChunk.length() == 0) {
            return;
        }

        String chunk =
                currentChunk.toString()
                        .trim();

        if (!chunk.isEmpty()) {
            chunks.add(
                    chunk
            );
        }

        currentChunk.setLength(
                0
        );
    }

    private boolean containsDevanagari(
            @NonNull String text
    ) {
        for (int index = 0;
             index < text.length();
             index++) {

            char character =
                    text.charAt(
                            index
                    );

            if ((character >= '\u0900'
                    && character <= '\u097F')
                    || (character >= '\uA8E0'
                    && character <= '\uA8FF')) {

                return true;
            }
        }

        return false;
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

    public interface SpeechCallback {

        void onPreparing();

        void onStarted();

        void onCompleted();

        void onStopped();

        void onError(
                @NonNull String errorMessage
        );
    }

    private static final class PendingSpeech {

        @NonNull
        private final String text;

        @NonNull
        private final String preferredLanguage;

        @NonNull
        private final SpeechCallback callback;

        private PendingSpeech(
                @NonNull String text,
                @NonNull String preferredLanguage,
                @NonNull SpeechCallback callback
        ) {
            this.text =
                    text;

            this.preferredLanguage =
                    preferredLanguage;

            this.callback =
                    callback;
        }
    }
}