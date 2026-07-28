package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Study Saathi Hero Part का reusable Text-to-Speech manager।
 *
 * मुख्य कार्य:
 *
 * 1. Hindi, English और bilingual answers को आवाज में पढ़ना।
 * 2. Answer source label को speech से हटाना।
 * 3. TextToSpeech initialization safely handle करना।
 * 4. Play, stop और replay support देना।
 * 5. लंबे उत्तर को सुरक्षित speech chunks में बाँटना।
 * 6. सभी chunks को क्रम से अपने-आप पढ़ना।
 * 7. नई speech शुरू होने पर पुरानी speech रोकना।
 * 8. Activity/Fragment destroy होने पर resources release करना।
 *
 * इस class में कोई Activity अथवा View reference नहीं रखा जाता।
 * इसलिए इसे Hero, Revision, History और Lesson screens में
 * दोबारा उपयोग किया जा सकता है।
 */
public final class SmartTutorTextToSpeechManager {

    private static final float DEFAULT_SPEECH_RATE =
            0.92f;

    private static final float DEFAULT_PITCH =
            1.0f;

    private static final String UTTERANCE_PREFIX =
            "study_saathi_answer_";

    /**
     * Android TTS की maximum सीमा से इतनी जगह सुरक्षित रखी जाती है।
     */
    private static final int TTS_INPUT_SAFETY_MARGIN =
            200;

    /**
     * किसी भी स्थिति में chunk इससे छोटा limit नहीं रखेगा।
     */
    private static final int MIN_SAFE_CHUNK_LENGTH =
            200;

    @NonNull
    private static final Locale HINDI_INDIA_LOCALE =
            new Locale(
                    "hi",
                    "IN"
            );

    @NonNull
    private static final Locale ENGLISH_INDIA_LOCALE =
            new Locale(
                    "en",
                    "IN"
            );

    @NonNull
    private static final String[] SOURCE_BADGE_LINES = {
            "✓ ऑफलाइन गणित",
            "✓ ऑफलाइन विभाज्यता",
            "✓ सत्यापित ऑफलाइन सामग्री",
            "↻ सेव किया गया उत्तर",
            "✦ Smart AI",
            "● ऑफलाइन सहायता",
            "● उत्तर",
            "ऑफलाइन गणित",
            "ऑफलाइन विभाज्यता",
            "सत्यापित ऑफलाइन सामग्री",
            "सेव किया गया उत्तर",
            "Smart AI",
            "ऑफलाइन सहायता"
    };

    @NonNull
    private final Context applicationContext;

    @NonNull
    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    @Nullable
    private TextToSpeech textToSpeech;

    @Nullable
    private SpeechRequest pendingSpeechRequest;

    @Nullable
    private SpeechRequest lastSpeechRequest;

    @Nullable
    private SpeechCallback currentSpeechCallback;

    /**
     * वर्तमान उत्तर के सभी speech chunks।
     */
    @NonNull
    private List<String> currentSpeechChunks =
            Collections.emptyList();

    /**
     * वर्तमान में पढ़े जा रहे chunk का index।
     */
    private int currentChunkIndex =
            -1;

    /**
     * एक पूरे answer-reading session की unique ID।
     */
    @NonNull
    private String currentSpeechSessionId =
            "";

    /**
     * वर्तमान individual chunk की utterance ID।
     */
    @NonNull
    private String currentUtteranceId =
            "";

    @NonNull
    private String lastErrorMessage =
            "";

    private boolean initialized;

    private boolean initializationFailed;

    private boolean shutdown;

    /**
     * onSpeechStarted callback पूरे उत्तर में केवल एक बार भेजने के लिए।
     */
    private boolean speechStartedNotified;

    public SmartTutorTextToSpeechManager(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        initializeTextToSpeech();
    }

    /**
     * Android TextToSpeech engine initialize करता है।
     */
    private void initializeTextToSpeech() {
        runOnMainThread(
                () -> {
                    if (shutdown) {
                        return;
                    }

                    textToSpeech =
                            new TextToSpeech(
                                    applicationContext,
                                    this::handleInitializationResult
                            );
                }
        );
    }

    /**
     * TTS initialization result handle करता है।
     */
    private void handleInitializationResult(
            int status
    ) {
        runOnMainThread(
                () -> {
                    if (shutdown) {
                        releaseTextToSpeechEngine();

                        return;
                    }

                    if (status
                            != TextToSpeech.SUCCESS) {

                        initialized =
                                false;

                        initializationFailed =
                                true;

                        lastErrorMessage =
                                "Text-to-Speech शुरू नहीं हो सका। "
                                        + "फोन की speech service जाँचें।";

                        SpeechRequest failedRequest =
                                pendingSpeechRequest;

                        pendingSpeechRequest =
                                null;

                        if (failedRequest != null) {
                            notifyError(
                                    failedRequest.callback,
                                    lastErrorMessage
                            );
                        }

                        return;
                    }

                    TextToSpeech engine =
                            textToSpeech;

                    if (engine == null) {
                        initialized =
                                false;

                        initializationFailed =
                                true;

                        lastErrorMessage =
                                "Text-to-Speech engine उपलब्ध नहीं है।";

                        SpeechRequest failedRequest =
                                pendingSpeechRequest;

                        pendingSpeechRequest =
                                null;

                        if (failedRequest != null) {
                            notifyError(
                                    failedRequest.callback,
                                    lastErrorMessage
                            );
                        }

                        return;
                    }

                    configureTextToSpeechEngine(
                            engine
                    );

                    initialized =
                            true;

                    initializationFailed =
                            false;

                    lastErrorMessage =
                            "";

                    SpeechRequest queuedRequest =
                            pendingSpeechRequest;

                    pendingSpeechRequest =
                            null;

                    if (queuedRequest != null) {
                        speakRequestInternal(
                                queuedRequest
                        );
                    }
                }
        );
    }

    /**
     * Speech rate, pitch, audio type और progress listener configure करता है।
     */
    private void configureTextToSpeechEngine(
            @NonNull TextToSpeech engine
    ) {
        engine.setSpeechRate(
                DEFAULT_SPEECH_RATE
        );

        engine.setPitch(
                DEFAULT_PITCH
        );

        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(
                                AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                        )
                        .setContentType(
                                AudioAttributes.CONTENT_TYPE_SPEECH
                        )
                        .build();

        engine.setAudioAttributes(
                audioAttributes
        );

        engine.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {

                    @Override
                    public void onStart(
                            String utteranceId
                    ) {
                        runOnMainThread(
                                () -> handleChunkStarted(
                                        utteranceId
                                )
                        );
                    }

                    @Override
                    public void onDone(
                            String utteranceId
                    ) {
                        runOnMainThread(
                                () -> handleChunkCompleted(
                                        utteranceId
                                )
                        );
                    }

                    @Override
                    public void onError(
                            String utteranceId
                    ) {
                        handleSpeechError(
                                utteranceId,
                                "उत्तर को आवाज में पढ़ते समय समस्या हुई।"
                        );
                    }

                    @Override
                    public void onError(
                            String utteranceId,
                            int errorCode
                    ) {
                        handleSpeechError(
                                utteranceId,
                                "उत्तर को आवाज में पढ़ते समय समस्या हुई। "
                                        + "Error code: "
                                        + errorCode
                        );
                    }

                    @Override
                    public void onStop(
                            String utteranceId,
                            boolean interrupted
                    ) {
                        runOnMainThread(
                                () -> handleEngineSpeechStopped(
                                        utteranceId
                                )
                        );
                    }
                }
        );
    }

    /**
     * पहला chunk शुरू होने पर UI को एक बार callback देता है।
     */
    private void handleChunkStarted(
            @Nullable String utteranceId
    ) {
        if (!isCurrentUtterance(
                utteranceId
        )) {
            return;
        }

        if (speechStartedNotified) {
            return;
        }

        speechStartedNotified =
                true;

        SpeechCallback callback =
                currentSpeechCallback;

        if (callback != null) {
            callback.onSpeechStarted();
        }
    }

    /**
     * एक chunk पूरा होने पर अगला chunk शुरू करता है।
     *
     * अंतिम chunk पूरा होने पर पूरे answer का completion callback देता है।
     */
    private void handleChunkCompleted(
            @Nullable String utteranceId
    ) {
        if (!isCurrentUtterance(
                utteranceId
        )) {
            return;
        }

        int nextChunkIndex =
                currentChunkIndex + 1;

        if (nextChunkIndex
                < currentSpeechChunks.size()) {

            currentChunkIndex =
                    nextChunkIndex;

            TextToSpeech engine =
                    textToSpeech;

            if (engine == null
                    || !speakCurrentChunk(
                    engine
            )) {

                SpeechCallback callback =
                        currentSpeechCallback;

                clearCurrentSpeechState();

                notifyError(
                        callback,
                        "लंबे उत्तर का अगला भाग शुरू नहीं हो सका।"
                );
            }

            return;
        }

        SpeechCallback callback =
                currentSpeechCallback;

        clearCurrentSpeechState();

        if (callback != null) {
            callback.onSpeechCompleted();
        }
    }

    /**
     * Android speech engine द्वारा speech रोक दिए जाने की स्थिति।
     */
    private void handleEngineSpeechStopped(
            @Nullable String utteranceId
    ) {
        if (!isCurrentUtterance(
                utteranceId
        )) {
            return;
        }

        SpeechCallback callback =
                currentSpeechCallback;

        clearCurrentSpeechState();

        if (callback != null) {
            callback.onSpeechStopped();
        }
    }

    /**
     * SmartTutorAnswerResult का केवल raw educational answer पढ़ता है।
     *
     * इससे "Smart AI", "ऑफलाइन गणित" जैसे source labels
     * आवाज में नहीं पढ़े जाएँगे।
     */
    public void speakAnswer(
            @NonNull SmartTutorAnswerResult answerResult,
            @Nullable String explanationLanguage,
            @Nullable SpeechCallback callback
    ) {
        speakAnswer(
                answerResult.getRawAnswerText(),
                explanationLanguage,
                callback
        );
    }

    /**
     * String answer को आवाज में पढ़ता है।
     *
     * Legacy Hero screen से display-ready answer मिलने पर भी
     * यह method source badge remove करता है।
     */
    public void speakAnswer(
            @Nullable String answerText,
            @Nullable String explanationLanguage,
            @Nullable SpeechCallback callback
    ) {
        String speechText =
                cleanAnswerForSpeech(
                        answerText
                );

        if (speechText.isEmpty()) {
            notifyError(
                    callback,
                    "सुनाने के लिए कोई उत्तर उपलब्ध नहीं है।"
            );

            return;
        }

        SpeechRequest speechRequest =
                new SpeechRequest(
                        speechText,
                        safeText(
                                explanationLanguage
                        ),
                        callback
                );

        runOnMainThread(
                () -> {
                    if (shutdown) {
                        notifyError(
                                callback,
                                "Text-to-Speech बंद किया जा चुका है।"
                        );

                        return;
                    }

                    lastSpeechRequest =
                            speechRequest;

                    if (initializationFailed) {
                        notifyError(
                                callback,
                                lastErrorMessage.isEmpty()
                                        ? "Text-to-Speech उपलब्ध नहीं है।"
                                        : lastErrorMessage
                        );

                        return;
                    }

                    if (!initialized
                            || textToSpeech == null) {

                        pendingSpeechRequest =
                                speechRequest;

                        return;
                    }

                    speakRequestInternal(
                            speechRequest
                    );
                }
        );
    }

    /**
     * पिछला answer दोबारा पढ़ता है।
     */
    public void replayLastAnswer(
            @Nullable SpeechCallback callback
    ) {
        runOnMainThread(
                () -> {
                    if (shutdown) {
                        notifyError(
                                callback,
                                "Text-to-Speech बंद किया जा चुका है।"
                        );

                        return;
                    }

                    SpeechRequest previousRequest =
                            lastSpeechRequest;

                    if (previousRequest == null) {
                        notifyError(
                                callback,
                                "दोबारा सुनाने के लिए पिछला उत्तर उपलब्ध नहीं है।"
                        );

                        return;
                    }

                    SpeechRequest replayRequest =
                            new SpeechRequest(
                                    previousRequest.speechText,
                                    previousRequest.explanationLanguage,
                                    callback
                            );

                    lastSpeechRequest =
                            replayRequest;

                    if (initializationFailed) {
                        notifyError(
                                callback,
                                lastErrorMessage.isEmpty()
                                        ? "Text-to-Speech उपलब्ध नहीं है।"
                                        : lastErrorMessage
                        );

                        return;
                    }

                    if (!initialized
                            || textToSpeech == null) {

                        pendingSpeechRequest =
                                replayRequest;

                        return;
                    }

                    speakRequestInternal(
                            replayRequest
                    );
                }
        );
    }

    /**
     * Actual speech request को chunks में बदलकर पढ़ना शुरू करता है।
     */
    private void speakRequestInternal(
            @NonNull SpeechRequest speechRequest
    ) {
        TextToSpeech engine =
                textToSpeech;

        if (engine == null
                || !initialized
                || shutdown) {

            notifyError(
                    speechRequest.callback,
                    "Text-to-Speech अभी तैयार नहीं है।"
            );

            return;
        }

        /*
         * नया answer शुरू होने से पहले पुरानी speech रोकें।
         * पुराने callback को Stop event नहीं भेजा जाएगा।
         */
        stopCurrentSpeechWithoutCallback();

        Locale preferredLocale =
                resolvePreferredLocale(
                        speechRequest.explanationLanguage,
                        speechRequest.speechText
                );

        Locale selectedLocale =
                selectSupportedLocale(
                        engine,
                        preferredLocale
                );

        if (selectedLocale == null) {
            lastErrorMessage =
                    "इस उत्तर की भाषा के लिए speech voice उपलब्ध नहीं है।";

            notifyError(
                    speechRequest.callback,
                    lastErrorMessage
            );

            return;
        }

        int languageResult =
                engine.setLanguage(
                        selectedLocale
                );

        if (languageResult
                == TextToSpeech.LANG_MISSING_DATA
                || languageResult
                == TextToSpeech.LANG_NOT_SUPPORTED) {

            lastErrorMessage =
                    "फोन में आवश्यक Hindi/English speech language उपलब्ध नहीं है।";

            notifyError(
                    speechRequest.callback,
                    lastErrorMessage
            );

            return;
        }

        int safeChunkLength =
                calculateSafeChunkLength();

        List<String> speechChunks =
                SmartTutorSpeechChunker
                        .createSpeechChunks(
                                speechRequest.speechText,
                                safeChunkLength
                        );

        if (speechChunks.isEmpty()) {
            notifyError(
                    speechRequest.callback,
                    "सुनाने के लिए उत्तर उपलब्ध नहीं है।"
            );

            return;
        }

        currentSpeechChunks =
                speechChunks;

        currentChunkIndex =
                0;

        currentSpeechSessionId =
                UUID.randomUUID()
                        .toString();

        currentSpeechCallback =
                speechRequest.callback;

        speechStartedNotified =
                false;

        if (!speakCurrentChunk(
                engine
        )) {
            SpeechCallback callback =
                    currentSpeechCallback;

            clearCurrentSpeechState();

            lastErrorMessage =
                    "उत्तर को आवाज में शुरू नहीं किया जा सका।";

            notifyError(
                    callback,
                    lastErrorMessage
            );
        }
    }

    /**
     * वर्तमान chunk को Android TTS engine से पढ़ता है।
     */
    private boolean speakCurrentChunk(
            @NonNull TextToSpeech engine
    ) {
        if (currentChunkIndex < 0
                || currentChunkIndex
                >= currentSpeechChunks.size()
                || currentSpeechSessionId.isEmpty()) {

            return false;
        }

        String chunkText =
                currentSpeechChunks.get(
                        currentChunkIndex
                );

        if (chunkText == null
                || chunkText.trim().isEmpty()) {

            return false;
        }

        String utteranceId =
                UTTERANCE_PREFIX
                        + currentSpeechSessionId
                        + "_chunk_"
                        + currentChunkIndex;

        currentUtteranceId =
                utteranceId;

        Bundle speechParameters =
                new Bundle();

        /*
         * प्रत्येक नया chunk पिछले पूर्ण chunk के बाद शुरू होता है।
         * QUEUE_FLUSH किसी stale speech को queue में बचने नहीं देता।
         */
        int speakResult =
                engine.speak(
                        chunkText,
                        TextToSpeech.QUEUE_FLUSH,
                        speechParameters,
                        utteranceId
                );

        return speakResult
                != TextToSpeech.ERROR;
    }

    /**
     * Device TTS maximum input limit के अनुसार safe chunk size निकालता है।
     */
    private int calculateSafeChunkLength() {
        int androidMaximumLength =
                TextToSpeech.getMaxSpeechInputLength();

        int maximumAfterSafetyMargin =
                androidMaximumLength
                        - TTS_INPUT_SAFETY_MARGIN;

        int preferredLength =
                Math.min(
                        SmartTutorSpeechChunker
                                .DEFAULT_MAX_CHUNK_LENGTH,
                        maximumAfterSafetyMargin
                );

        return Math.max(
                MIN_SAFE_CHUNK_LENGTH,
                preferredLength
        );
    }

    /**
     * Current अथवा initialization के लिए pending speech रोकता है।
     */
    public void stop() {
        runOnMainThread(
                () -> {
                    boolean hadActiveSpeech =
                            !currentSpeechSessionId.isEmpty()
                                    || pendingSpeechRequest != null;

                    SpeechCallback callback =
                            currentSpeechCallback;

                    if (callback == null
                            && pendingSpeechRequest != null) {

                        callback =
                                pendingSpeechRequest.callback;
                    }

                    pendingSpeechRequest =
                            null;

                    /*
                     * State पहले clear करें ताकि engine का asynchronous
                     * onStop callback duplicate event न भेजे।
                     */
                    clearCurrentSpeechState();

                    TextToSpeech engine =
                            textToSpeech;

                    if (engine != null) {
                        engine.stop();
                    }

                    if (hadActiveSpeech
                            && callback != null) {

                        callback.onSpeechStopped();
                    }
                }
        );
    }

    /**
     * Activity या Fragment destroy होने पर call करें।
     */
    public void shutdown() {
        runOnMainThread(
                () -> {
                    if (shutdown) {
                        return;
                    }

                    shutdown =
                            true;

                    pendingSpeechRequest =
                            null;

                    lastSpeechRequest =
                            null;

                    clearCurrentSpeechState();

                    releaseTextToSpeechEngine();

                    initialized =
                            false;
                }
        );
    }

    /**
     * Android TextToSpeech resources release करता है।
     */
    private void releaseTextToSpeechEngine() {
        TextToSpeech engine =
                textToSpeech;

        textToSpeech =
                null;

        if (engine != null) {
            engine.stop();
            engine.shutdown();
        }
    }

    /**
     * पूरा answer अथवा उसका कोई chunk सक्रिय है या नहीं।
     */
    public boolean isSpeaking() {
        if (!currentSpeechSessionId.isEmpty()) {
            return true;
        }

        TextToSpeech engine =
                textToSpeech;

        return engine != null
                && initialized
                && engine.isSpeaking();
    }

    /**
     * TTS engine request लेने के लिए तैयार है या नहीं।
     */
    public boolean isReady() {
        return initialized
                && !initializationFailed
                && !shutdown
                && textToSpeech != null;
    }

    /**
     * पिछला answer replay के लिए उपलब्ध है या नहीं।
     */
    public boolean hasReplayableAnswer() {
        return lastSpeechRequest != null;
    }

    /**
     * वर्तमान chunk number।
     *
     * Speech active न होने पर 0 return होगा।
     */
    public int getCurrentChunkNumber() {
        if (currentChunkIndex < 0
                || currentSpeechSessionId.isEmpty()) {

            return 0;
        }

        return currentChunkIndex + 1;
    }

    /**
     * वर्तमान answer के कुल speech chunks।
     */
    public int getTotalChunkCount() {
        if (currentSpeechSessionId.isEmpty()) {
            return 0;
        }

        return currentSpeechChunks.size();
    }

    @NonNull
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /**
     * Display-ready answer से source label और formatting symbols हटाता है।
     */
    @NonNull
    public static String cleanAnswerForSpeech(
            @Nullable String answerText
    ) {
        String cleanedText =
                safeText(
                        answerText
                );

        if (cleanedText.isEmpty()) {
            return "";
        }

        cleanedText =
                removeSourceBadgeFromBeginning(
                        cleanedText
                );

        cleanedText =
                cleanedText
                        .replace(
                                "```",
                                ""
                        )
                        .replace(
                                "`",
                                ""
                        )
                        .replace(
                                "###",
                                ""
                        )
                        .replace(
                                "##",
                                ""
                        )
                        .replace(
                                "#",
                                ""
                        )
                        .replace(
                                "•",
                                ". "
                        )
                        .replace(
                                "→",
                                " फिर "
                        )
                        .replace(
                                "⇒",
                                " इसलिए "
                        )
                        .replace(
                                "=",
                                " बराबर "
                        )
                        .replace(
                                "÷",
                                " भाग "
                        )
                        .replace(
                                "×",
                                " गुणा "
                        );

        cleanedText =
                cleanedText.replaceAll(
                        "(?m)^\\s*[-*]\\s+",
                        ""
                );

        cleanedText =
                cleanedText.replaceAll(
                        "[\\r\\n]+",
                        ". "
                );

        cleanedText =
                cleanedText.replaceAll(
                        "\\s+",
                        " "
                );

        cleanedText =
                cleanedText.replaceAll(
                        "(\\.\\s*){2,}",
                        ". "
                );

        return cleanedText.trim();
    }

    /**
     * Answer की पहली line source badge हो तो उसे हटाता है।
     */
    @NonNull
    private static String removeSourceBadgeFromBeginning(
            @NonNull String answerText
    ) {
        String normalizedAnswer =
                answerText.trim();

        for (String sourceBadgeLine :
                SOURCE_BADGE_LINES) {

            if (normalizedAnswer.equals(
                    sourceBadgeLine
            )) {
                return "";
            }

            if (normalizedAnswer.startsWith(
                    sourceBadgeLine + "\n"
            )
                    || normalizedAnswer.startsWith(
                    sourceBadgeLine + "\r\n"
            )) {

                return normalizedAnswer
                        .substring(
                                sourceBadgeLine.length()
                        )
                        .trim();
            }
        }

        return normalizedAnswer;
    }

    /**
     * Selected explanation language और answer script से preferred locale चुनता है।
     */
    @NonNull
    private Locale resolvePreferredLocale(
            @Nullable String explanationLanguage,
            @NonNull String speechText
    ) {
        String normalizedLanguage =
                safeText(
                        explanationLanguage
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        boolean hindiRequested =
                normalizedLanguage.contains(
                        "hindi"
                )
                        || normalizedLanguage.contains(
                        "हिंदी"
                );

        boolean englishRequested =
                normalizedLanguage.contains(
                        "english"
                )
                        || normalizedLanguage.contains(
                        "अंग्रेज"
                );

        if (hindiRequested
                && !englishRequested) {

            return HINDI_INDIA_LOCALE;
        }

        if (englishRequested
                && !hindiRequested) {

            return ENGLISH_INDIA_LOCALE;
        }

        if (containsDevanagari(
                speechText
        )) {
            return HINDI_INDIA_LOCALE;
        }

        return ENGLISH_INDIA_LOCALE;
    }

    /**
     * Preferred locale उपलब्ध न होने पर सुरक्षित fallback चुनता है।
     */
    @Nullable
    private Locale selectSupportedLocale(
            @NonNull TextToSpeech engine,
            @NonNull Locale preferredLocale
    ) {
        if (isLocaleSupported(
                engine,
                preferredLocale
        )) {
            return preferredLocale;
        }

        if (!preferredLocale.getLanguage()
                .equals(
                        HINDI_INDIA_LOCALE.getLanguage()
                )
                && isLocaleSupported(
                engine,
                HINDI_INDIA_LOCALE
        )) {

            return HINDI_INDIA_LOCALE;
        }

        if (isLocaleSupported(
                engine,
                ENGLISH_INDIA_LOCALE
        )) {
            return ENGLISH_INDIA_LOCALE;
        }

        if (isLocaleSupported(
                engine,
                Locale.US
        )) {
            return Locale.US;
        }

        return null;
    }

    private boolean isLocaleSupported(
            @NonNull TextToSpeech engine,
            @NonNull Locale locale
    ) {
        int availability =
                engine.isLanguageAvailable(
                        locale
                );

        return availability
                != TextToSpeech.LANG_MISSING_DATA
                && availability
                != TextToSpeech.LANG_NOT_SUPPORTED;
    }

    /**
     * Text में Devanagari characters हैं या नहीं।
     */
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

            if (character >= '\u0900'
                    && character <= '\u097F') {

                return true;
            }
        }

        return false;
    }

    /**
     * Utterance error callback handle करता है।
     */
    private void handleSpeechError(
            @Nullable String utteranceId,
            @NonNull String errorMessage
    ) {
        runOnMainThread(
                () -> {
                    if (!isCurrentUtterance(
                            utteranceId
                    )) {
                        return;
                    }

                    SpeechCallback callback =
                            currentSpeechCallback;

                    clearCurrentSpeechState();

                    lastErrorMessage =
                            errorMessage;

                    notifyError(
                            callback,
                            errorMessage
                    );
                }
        );
    }

    /**
     * पुरानी speech रोकता है, लेकिन पुराने UI callback को Stop नहीं भेजता।
     */
    private void stopCurrentSpeechWithoutCallback() {
        boolean hadActiveSpeech =
                !currentSpeechSessionId.isEmpty();

        /*
         * State पहले clear करने से asynchronous onStop ignored रहेगा।
         */
        clearCurrentSpeechState();

        TextToSpeech engine =
                textToSpeech;

        if (engine != null
                && (hadActiveSpeech
                || engine.isSpeaking())) {

            engine.stop();
        }
    }

    /**
     * पूरे current answer-reading session की state साफ करता है।
     */
    private void clearCurrentSpeechState() {
        currentSpeechCallback =
                null;

        currentSpeechChunks =
                Collections.emptyList();

        currentChunkIndex =
                -1;

        currentSpeechSessionId =
                "";

        currentUtteranceId =
                "";

        speechStartedNotified =
                false;
    }

    private boolean isCurrentUtterance(
            @Nullable String utteranceId
    ) {
        return utteranceId != null
                && !currentUtteranceId.isEmpty()
                && currentUtteranceId.equals(
                utteranceId
        );
    }

    private void notifyError(
            @Nullable SpeechCallback callback,
            @NonNull String errorMessage
    ) {
        lastErrorMessage =
                errorMessage;

        if (callback != null) {
            runOnMainThread(
                    () -> callback.onSpeechError(
                            errorMessage
                    )
            );
        }
    }

    private void runOnMainThread(
            @NonNull Runnable runnable
    ) {
        if (Looper.myLooper()
                == Looper.getMainLooper()) {

            runnable.run();

            return;
        }

        mainHandler.post(
                runnable
        );
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

    /**
     * एक pending अथवा active speech request।
     */
    private static final class SpeechRequest {

        @NonNull
        private final String speechText;

        @NonNull
        private final String explanationLanguage;

        @Nullable
        private final SpeechCallback callback;

        private SpeechRequest(
                @NonNull String speechText,
                @NonNull String explanationLanguage,
                @Nullable SpeechCallback callback
        ) {
            this.speechText =
                    speechText;

            this.explanationLanguage =
                    explanationLanguage;

            this.callback =
                    callback;
        }
    }

    /**
     * Hero UI को speech status देने वाला callback।
     */
    public interface SpeechCallback {

        /**
         * पूरे answer का पहला chunk बोलना शुरू हुआ।
         */
        void onSpeechStarted();

        /**
         * पूरे answer के सभी chunks बोल दिए गए।
         */
        void onSpeechCompleted();

        /**
         * Speech manually अथवा system द्वारा रोकी गई।
         */
        void onSpeechStopped();

        /**
         * Speech शुरू अथवा पूरी नहीं हो सकी।
         */
        void onSpeechError(
                @NonNull String errorMessage
        );
    }
}
