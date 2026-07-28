package com.tridev.studysaathi.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;

/**
 * Global companion के voice/camera/gallery Activity Result contracts का
 * headless lifecycle owner।
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
                            || result.getData() == null
                            || getActivity() == null) {
                        return;
                    }
                    ArrayList<String> results = result.getData()
                            .getStringArrayListExtra(
                                    RecognizerIntent.EXTRA_RESULTS
                            );
                    if (results != null && !results.isEmpty()
                            && results.get(0) != null
                            && !results.get(0).trim().isEmpty()) {
                        SmartAiCompanionController.deliverVoiceQuestion(
                                requireActivity(),
                                results.get(0).trim()
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
        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "hi-IN"
        );
        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "अपना सवाल बोलें"
        );
        try {
            voiceLauncher.launch(intent);
        } catch (ActivityNotFoundException ignored) {
            // Text input remains available.
        }
    }

    public void launchGallery() {
        galleryLauncher.launch("image/*");
    }

    public void launchCamera() {
        cameraLauncher.launch(null);
    }
}
