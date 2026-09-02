package com.tridev.studysaathi.data.ai;

import android.content.Intent;
import android.os.Build;
import android.speech.RecognizerIntent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared speech-recognition language contract for Study Saathi.
 *
 * English and Hindi use a stable India locale. Hinglish keeps Hindi as the
 * backwards-compatible base locale and, on Android 14+, asks compatible
 * recognizers to switch automatically between Hindi and English.
 */
public final class StudyVoiceLanguageHelper {

    public static final String LANGUAGE_TAG_ENGLISH_INDIA = "en-IN";
    public static final String LANGUAGE_TAG_HINDI_INDIA = "hi-IN";

    private StudyVoiceLanguageHelper() {
        // Utility class.
    }

    @NonNull
    public static Intent createRecognitionIntent(
            @Nullable String explanationLanguage,
            @Nullable String subjectName
    ) {
        VoiceMode voiceMode = resolveVoiceMode(explanationLanguage);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                createPrompt(voiceMode, subjectName)
        );

        String baseLanguageTag = voiceMode == VoiceMode.ENGLISH
                ? LANGUAGE_TAG_ENGLISH_INDIA
                : LANGUAGE_TAG_HINDI_INDIA;

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, baseLanguageTag);

        if (voiceMode == VoiceMode.HINGLISH
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ArrayList<String> allowedLanguages = new ArrayList<>();
            allowedLanguages.add(LANGUAGE_TAG_HINDI_INDIA);
            allowedLanguages.add(LANGUAGE_TAG_ENGLISH_INDIA);

            intent.putExtra(
                    RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION,
                    true
            );
            intent.putStringArrayListExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES,
                    allowedLanguages
            );
            intent.putExtra(
                    RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                    RecognizerIntent.LANGUAGE_SWITCH_BALANCED
            );
            intent.putStringArrayListExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                    allowedLanguages
            );
        }

        return intent;
    }

    @NonNull
    public static String extractBestSpeechResult(
            @Nullable Intent resultData
    ) {
        if (resultData == null) {
            return "";
        }

        ArrayList<String> speechResults = resultData.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
        );

        if (speechResults == null || speechResults.isEmpty()) {
            return "";
        }

        float[] confidenceScores = resultData.getFloatArrayExtra(
                RecognizerIntent.EXTRA_CONFIDENCE_SCORES
        );

        if (confidenceScores != null
                && confidenceScores.length == speechResults.size()) {
            int bestIndex = -1;
            float bestScore = Float.NEGATIVE_INFINITY;

            for (int index = 0; index < speechResults.size(); index++) {
                String candidate = safeText(speechResults.get(index));
                if (candidate.isEmpty()) {
                    continue;
                }

                float score = confidenceScores[index];
                if (bestIndex < 0 || score > bestScore) {
                    bestIndex = index;
                    bestScore = score;
                }
            }

            if (bestIndex >= 0) {
                return safeText(speechResults.get(bestIndex));
            }
        }

        return firstValidResult(speechResults);
    }

    public static boolean isEnglishOnly(
            @Nullable String explanationLanguage
    ) {
        return resolveVoiceMode(explanationLanguage) == VoiceMode.ENGLISH;
    }

    public static boolean isHinglish(
            @Nullable String explanationLanguage
    ) {
        return resolveVoiceMode(explanationLanguage) == VoiceMode.HINGLISH;
    }

    @NonNull
    private static VoiceMode resolveVoiceMode(
            @Nullable String explanationLanguage
    ) {
        String language = safeText(explanationLanguage)
                .toLowerCase(Locale.ROOT);

        if (language.contains("hinglish")
                || language.contains("bilingual")
                || language.contains("hi,en")
                || language.contains("hindi + english")
                || language.contains("hindi+english")
                || (language.contains("hindi") && language.contains("english"))) {
            return VoiceMode.HINGLISH;
        }

        if ((language.equals("english") || language.equals("en"))
                && !language.contains("hindi")) {
            return VoiceMode.ENGLISH;
        }

        if (language.equals("hindi")
                || language.equals("hi")
                || language.contains("हिंदी")
                || language.contains("हिन्दी")) {
            return VoiceMode.HINDI;
        }

        return Locale.ENGLISH.getLanguage().equalsIgnoreCase(
                Locale.getDefault().getLanguage()
        ) ? VoiceMode.ENGLISH : VoiceMode.HINDI;
    }

    @NonNull
    private static String createPrompt(
            @NonNull VoiceMode voiceMode,
            @Nullable String subjectName
    ) {
        String subject = safeText(subjectName);

        switch (voiceMode) {
            case ENGLISH:
                return subject.isEmpty()
                        ? "Speak your question"
                        : "Speak your " + subject + " question";

            case HINDI:
                return subject.isEmpty()
                        ? "अपना सवाल बोलें"
                        : subject + " का सवाल बोलें";

            case HINGLISH:
            default:
                return subject.isEmpty()
                        ? "Hindi, English या Hinglish में अपना सवाल बोलें"
                        : subject + " का सवाल Hindi, English या Hinglish में बोलें";
        }
    }

    @NonNull
    private static String firstValidResult(
            @NonNull List<String> speechResults
    ) {
        for (String speechResult : speechResults) {
            String candidate = safeText(speechResult);
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return "";
    }

    @NonNull
    private static String safeText(@Nullable Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private enum VoiceMode {
        ENGLISH,
        HINDI,
        HINGLISH
    }
}
