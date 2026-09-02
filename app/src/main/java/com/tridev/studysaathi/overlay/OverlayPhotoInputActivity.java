package com.tridev.studysaathi.overlay;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

/**
 * Transparent camera/gallery bridge for the floating Study Saathi overlay.
 *
 * The floating panel remains alive behind this activity; after the user picks
 * or captures a question image, only the Uri is returned to the overlay
 * service. The full Study Saathi activity stack is never opened.
 */
public final class OverlayPhotoInputActivity extends ComponentActivity {

    private static final String PHOTO_DIRECTORY =
            "book_cover_cache/overlay_question_images";

    @Nullable
    private Uri pendingCameraUri;

    private final ActivityResultLauncher<String[]> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::handleGalleryResult
            );

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (Boolean.TRUE.equals(success)
                                && pendingCameraUri != null) {
                            deliverPhoto(pendingCameraUri);
                        } else {
                            finish();
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            showSourceChooser();
        }
    }

    private void showSourceChooser() {
        new AlertDialog.Builder(this)
                .setTitle("Question photo")
                .setItems(
                        new CharSequence[]{
                                "📷 Camera से photo लें",
                                "🖼 Gallery से चुनें"
                        },
                        (dialog, which) -> {
                            if (which == 0) {
                                launchCamera();
                            } else {
                                launchGallery();
                            }
                        }
                )
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void launchGallery() {
        try {
            galleryLauncher.launch(new String[]{"image/*"});
        } catch (RuntimeException error) {
            finish();
        }
    }

    private void launchCamera() {
        try {
            File directory = new File(getCacheDir(), PHOTO_DIRECTORY);
            if (!directory.exists() && !directory.mkdirs()) {
                finish();
                return;
            }

            File imageFile = File.createTempFile(
                    "overlay_question_",
                    ".jpg",
                    directory
            );

            pendingCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );

            cameraLauncher.launch(pendingCameraUri);
        } catch (IOException | RuntimeException error) {
            finish();
        }
    }

    private void handleGalleryResult(@Nullable Uri imageUri) {
        if (imageUri == null) {
            finish();
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Same-session read permission is still enough for the service.
        }

        deliverPhoto(imageUri);
    }

    private void deliverPhoto(@NonNull Uri imageUri) {
        Intent serviceIntent = new Intent(
                this,
                StudyOverlayBubbleService.class
        )
                .setAction(StudyOverlayBubbleService.ACTION_PHOTO_RESULT)
                .putExtra(
                        StudyOverlayBubbleService.EXTRA_PHOTO_URI,
                        imageUri.toString()
                );

        try {
            ContextCompat.startForegroundService(this, serviceIntent);
        } catch (RuntimeException ignored) {
            // The overlay service may have been stopped while the picker was open.
        }

        finish();
    }
}
