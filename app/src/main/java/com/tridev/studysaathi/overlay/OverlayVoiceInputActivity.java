package com.tridev.studysaathi.overlay;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.tridev.studysaathi.data.ai.StudyVoiceLanguageHelper;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;

/**
 * Transparent bridge that keeps the floating Study Saathi panel in place
 * while system speech recognition collects a voice question.
 */
public final class OverlayVoiceInputActivity extends ComponentActivity {

    private final ActivityResultLauncher<Intent> voiceLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            String spokenQuestion =
                                    StudyVoiceLanguageHelper.extractBestSpeechResult(
                                            result.getData()
                                    );

                            if (!spokenQuestion.isEmpty()) {
                                ContextCompat.startForegroundService(
                                        this,
                                        new Intent(this, StudyOverlayBubbleService.class)
                                                .setAction(
                                                        StudyOverlayBubbleService.ACTION_VOICE_RESULT
                                                )
                                                .putExtra(
                                                        StudyOverlayBubbleService.EXTRA_VOICE_TEXT,
                                                        spokenQuestion
                                                )
                                );
                            }
                        }
                        finish();
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            loadProfileAndLaunchVoice();
        }
    }

    private void loadProfileAndLaunchVoice() {
        new StudentProfileRepository(this).getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            @Nullable StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        launchVoiceRecognition(
                                studentProfile == null
                                        ? ""
                                        : studentProfile.getExplanationLanguage()
                        );
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (!isFinishing() && !isDestroyed()) {
                            launchVoiceRecognition("");
                        }
                    }
                }
        );
    }

    private void launchVoiceRecognition(
            @Nullable String explanationLanguage
    ) {
        Intent intent = StudyVoiceLanguageHelper.createRecognitionIntent(
                explanationLanguage,
                ""
        );

        try {
            voiceLauncher.launch(intent);
        } catch (RuntimeException error) {
            finish();
        }
    }
}
