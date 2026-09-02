package com.tridev.studysaathi.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.tridev.studysaathi.data.ai.StudyVoiceLanguageHelper;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;

/**
 * Headless lifecycle owner for the global companion voice/camera/gallery
 * Activity Result contracts.
 */
public final class SmartAiCompanionInputFragment extends Fragment {

    private static final String TAG =
            "SmartAiCompanionInputFragment";

    private ActivityResultLauncher<Intent> voiceLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    public static void ensureAttached(
            @NonNull AppCompatActivity activity
    ) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) != null
                || manager.isStateSaved()) {
            return;
        }
        manager.beginTransaction()
                .add(new SmartAiCompanionInputFragment(), TAG)
                .commitNow();
    }

    @Nullable
    public static SmartAiCompanionInputFragment find(
            @NonNull Activity activity
    ) {
        if (!(activity instanceof AppCompatActivity)) {
            return null;
        }
        Fragment fragment = ((AppCompatActivity) activity)
                .getSupportFragmentManager()
                .findFragmentByTag(TAG);
        return fragment instanceof SmartAiCompanionInputFragment
                ? (SmartAiCompanionInputFragment) fragment
                : null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        voiceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || getActivity() == null) {
                        return;
                    }

                    String spokenQuestion =
                            StudyVoiceLanguageHelper.extractBestSpeechResult(
                                    result.getData()
                            );

                    if (!spokenQuestion.isEmpty()) {
                        SmartAiCompanionController.deliverVoiceQuestion(
                                requireActivity(),
                                spokenQuestion
                        );
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && getActivity() != null) {
                        SmartAiCompanionController.deliverQuestionPhoto(
                                requireActivity(),
                                uri
                        );
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null && getActivity() != null) {
                        SmartAiCompanionController.deliverCameraBitmap(
                                requireActivity(),
                                bitmap
                        );
                    }
                }
        );
    }

    public void launchVoice() {
        if (!isAdded()) {
            return;
        }

        new StudentProfileRepository(requireContext()).getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            @Nullable StudentProfileEntity studentProfile
                    ) {
                        if (!isAdded()) {
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
                        if (isAdded()) {
                            launchVoiceRecognition("");
                        }
                    }
                }
        );
    }

    private void launchVoiceRecognition(
            @Nullable String explanationLanguage
    ) {
        if (!isAdded()) {
            return;
        }

        Intent intent = StudyVoiceLanguageHelper.createRecognitionIntent(
                explanationLanguage,
                ""
        );

        try {
            voiceLauncher.launch(intent);
        } catch (RuntimeException error) {
            Toast.makeText(
                    requireContext(),
                    StudyVoiceLanguageHelper.isEnglishOnly(explanationLanguage)
                            ? "Voice input is unavailable on this device."
                            : "इस device पर voice input उपलब्ध नहीं है।",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    public void launchGallery() {
        galleryLauncher.launch("image/*");
    }

    public void launchCamera() {
        cameraLauncher.launch(null);
    }
}
