package com.tridev.studysaathi.overlay;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

/** Transparent bridge that returns system speech recognition text to the overlay service. */
public final class OverlayVoiceInputActivity extends ComponentActivity {
    private final ActivityResultLauncher<Intent> voiceLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            ArrayList<String> results = result.getData().getStringArrayListExtra(
                                    RecognizerIntent.EXTRA_RESULTS);
                            if (results != null && !results.isEmpty()) {
                                ContextCompat.startForegroundService(this,
                                        new Intent(this, StudyOverlayBubbleService.class)
                                                .setAction(StudyOverlayBubbleService.ACTION_VOICE_RESULT)
                                                .putExtra(StudyOverlayBubbleService.EXTRA_VOICE_TEXT,
                                                        results.get(0)));
                            }
                        }
                        finish();
                    });

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "अपना सवाल बोलिए");
            voiceLauncher.launch(intent);
        } catch (ActivityNotFoundException error) {
            finish();
        }
    }
}
