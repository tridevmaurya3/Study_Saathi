package com.tridev.studysaathi;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.tridev.studysaathi.data.content.model
        .BookContentsScanResult;
import com.tridev.studysaathi.data.content.scanner
        .BookContentsOcrScanner;
import com.tridev.studysaathi.databinding
        .ActivityBookContentsScanBinding;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public final class BookContentsScanActivity
        extends AppCompatActivity {

    public static final String EXTRA_BOOK_ROW_ID =
            "extra_book_row_id";

    public static final String EXTRA_BOOK_TITLE =
            "extra_book_title";

    public static final String EXTRA_CHAPTERS_CHANGED =
            "extra_chapters_changed";

    public static final String EXTRA_IMPORTED_COUNT =
            "extra_imported_count";

    public static final String EXTRA_SKIPPED_COUNT =
            "extra_skipped_count";

    private static final String STATE_SELECTED_IMAGE_URI =
            "state_selected_image_uri";

    private static final String STATE_SELECTED_IMAGE_PATH =
            "state_selected_image_path";

    private static final String STATE_SCAN_RESULT =
            "state_scan_result";

    private static final String CONTENTS_DIRECTORY_NAME =
            "book_contents";

    private static final String CONTENTS_IMAGE_PREFIX =
            "contents_page_";

    private static final String CONTENTS_IMAGE_SUFFIX =
            ".jpg";

    private static final long INVALID_ROW_ID =
            0L;

    private ActivityBookContentsScanBinding binding;

    private BookContentsOcrScanner ocrScanner;

    private long bookRowId =
            INVALID_ROW_ID;

    @NonNull
    private String bookTitle =
            "";

    @Nullable
    private Uri selectedImageUri;

    @NonNull
    private String selectedImagePath =
            "";

    @Nullable
    private BookContentsScanResult latestScanResult;

    private boolean processing;

    @NonNull
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    this::handleGalleryImage
            );

    @NonNull
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    activityResult -> {
                        if (activityResult.getResultCode()
                                == Activity.RESULT_OK) {

                            handleCameraImageCaptured();

                        } else {
                            /*
                             * Cancel होने पर temporary camera file रह सकती है।
                             * उसे अगली capture में replace किया जा सकता है।
                             */
                            setProcessing(
                                    false
                            );
                        }
                    }
            );

    @NonNull
    private final ActivityResultLauncher<Intent> reviewLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    activityResult -> {
                        if (activityResult.getResultCode()
                                != Activity.RESULT_OK) {

                            return;
                        }

                        Intent data =
                                activityResult.getData();

                        int importedCount =
                                data == null
                                        ? 0
                                        : data.getIntExtra(
                                        BookContentsReviewActivity
                                        .EXTRA_IMPORTED_COUNT,
                                        0
                                );

                        int skippedCount =
                                data == null
                                        ? 0
                                        : data.getIntExtra(
                                        BookContentsReviewActivity
                                        .EXTRA_SKIPPED_COUNT,
                                        0
                                );

                        boolean chaptersChanged =
                                data == null
                                        || data.getBooleanExtra(
                                        BookContentsReviewActivity
                                                .EXTRA_CHAPTERS_CHANGED,
                                        importedCount > 0
                                );

                        finishWithReviewResult(
                                chaptersChanged,
                                importedCount,
                                skippedCount
                        );
                    }
            );

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            long bookRowId,
            @Nullable String bookTitle
    ) {
        Intent intent =
                new Intent(
                        context,
                        BookContentsScanActivity.class
                );

        intent.putExtra(
                EXTRA_BOOK_ROW_ID,
                bookRowId
        );

        intent.putExtra(
                EXTRA_BOOK_TITLE,
                safeText(
                        bookTitle
                )
        );

        return intent;
    }

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivityBookContentsScanBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        ocrScanner =
                new BookContentsOcrScanner();

        readIntentValues();
        configureButtons();
        bindBookTitle();

        if (savedInstanceState != null) {
            restoreState(
                    savedInstanceState
            );
        }

        if (!hasValidBook()) {
            showFatalError(
                    "A valid school book was not provided."
            );

            return;
        }

        refreshImageState();
        refreshScanResultState();
    }

    private void readIntentValues() {
        Intent intent =
                getIntent();

        if (intent == null) {
            return;
        }

        bookRowId =
                intent.getLongExtra(
                        EXTRA_BOOK_ROW_ID,
                        INVALID_ROW_ID
                );

        bookTitle =
                safeText(
                        intent.getStringExtra(
                                EXTRA_BOOK_TITLE
                        )
                );
    }

    private void configureButtons() {
        binding.backFromContentsScanButton
                .setOnClickListener(view ->
                        cancelAndClose()
                );

        binding.takeContentsPhotoButton
                .setOnClickListener(view ->
                        openCamera()
                );

        binding.chooseContentsGalleryButton
                .setOnClickListener(view ->
                        openGallery()
                );

        binding.startContentsOcrButton
                .setOnClickListener(view ->
                        startOcrScan()
                );

        binding.reviewDetectedChaptersButton
                .setOnClickListener(view ->
                        openReviewScreen()
                );
    }

    private void bindBookTitle() {
        binding.contentsScanBookTitleTextView
                .setText(
                        bookTitle.isEmpty()
                                ? "Exact School Book"
                                : bookTitle
                );
    }

    private void openGallery() {
        if (processing
                || !hasValidBook()) {

            return;
        }

        hideError();

        try {
            galleryLauncher.launch(
                    "image/*"
            );

        } catch (RuntimeException exception) {
            showError(
                    getErrorMessage(
                            exception,
                            "Gallery could not be opened."
                    )
            );
        }
    }

    private void handleGalleryImage(
            @Nullable Uri imageUri
    ) {
        if (imageUri == null) {
            return;
        }

        selectedImageUri =
                imageUri;

        selectedImagePath =
                imageUri.toString();

        latestScanResult =
                null;

        refreshImageState();
        refreshScanResultState();
        hideError();
    }

    private void openCamera() {
        if (processing
                || !hasValidBook()) {

            return;
        }

        hideError();

        try {
            File imageFile =
                    createContentsImageFile();

            selectedImagePath =
                    imageFile.getAbsolutePath();

            selectedImageUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            imageFile
                    );

            Intent cameraIntent =
                    new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE
                    );

            cameraIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    selectedImageUri
            );

            cameraIntent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            cameraIntent.setClipData(
                    ClipData.newRawUri(
                            "Study Saathi contents page",
                            selectedImageUri
                    )
            );

            cameraLauncher.launch(
                    cameraIntent
            );

        } catch (IOException
                 | RuntimeException exception) {

            selectedImageUri =
                    null;

            selectedImagePath =
                    "";

            showError(
                    getErrorMessage(
                            exception,
                            "Camera image could not be prepared."
                    )
            );

            refreshImageState();
        }
    }

    @NonNull
    private File createContentsImageFile()
            throws IOException {

        File contentsDirectory =
                new File(
                        getFilesDir(),
                        CONTENTS_DIRECTORY_NAME
                );

        if (!contentsDirectory.exists()
                && !contentsDirectory.mkdirs()) {

            throw new IOException(
                    "The private contents-image folder "
                            + "could not be created."
            );
        }

        return File.createTempFile(
                CONTENTS_IMAGE_PREFIX
                        + System.currentTimeMillis()
                        + "_",
                CONTENTS_IMAGE_SUFFIX,
                contentsDirectory
        );
    }

    private void handleCameraImageCaptured() {
        Uri imageUri =
                selectedImageUri;

        if (imageUri == null
                || selectedImagePath.isEmpty()) {

            showError(
                    "The captured contents image is unavailable."
            );

            refreshImageState();
            return;
        }

        File imageFile =
                new File(
                        selectedImagePath
                );

        if (!imageFile.exists()
                || imageFile.length() <= 0L) {

            showError(
                    "The camera did not save a valid image."
            );

            selectedImageUri =
                    null;

            selectedImagePath =
                    "";

            refreshImageState();
            return;
        }

        latestScanResult =
                null;

        refreshImageState();
        refreshScanResultState();
        hideError();
    }

    private void startOcrScan() {
        Uri imageUri =
                selectedImageUri;

        if (processing
                || imageUri == null
                || !hasValidBook()) {

            return;
        }

        setProcessing(
                true
        );

        hideError();

        latestScanResult =
                null;

        refreshScanResultState();

        ocrScanner.scanFromUri(
                getApplicationContext(),
                bookRowId,
                imageUri,
                selectedImagePath,
                result -> {
                    if (!canUpdateScreen()) {
                        return;
                    }

                    setProcessing(
                            false
                    );

                    latestScanResult =
                            result;

                    if (!result.isSuccessful()) {
                        showError(
                                result.getErrorMessage()
                        );
                    }

                    refreshScanResultState();
                }
        );
    }

    private void refreshImageState() {
        Uri imageUri =
                selectedImageUri;

        boolean hasImage =
                imageUri != null;

        binding.noContentsImageContainer
                .setVisibility(
                        hasImage
                                ? View.GONE
                                : View.VISIBLE
                );

        binding.contentsImagePreviewImageView
                .setVisibility(
                        hasImage
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.selectedContentsImageTextView
                .setVisibility(
                        hasImage
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.startContentsOcrButton
                .setEnabled(
                        hasImage
                                && hasValidBook()
                                && !processing
                );

        if (!hasImage) {
            binding.contentsImagePreviewImageView
                    .setImageDrawable(
                            null
                    );

            binding.selectedContentsImageTextView
                    .setText(
                            ""
                    );

            return;
        }

        binding.contentsImagePreviewImageView
                .setImageURI(
                        imageUri
                );

        binding.selectedContentsImageTextView
                .setText(
                        selectedImagePath
                );
    }

    private void refreshScanResultState() {
        BookContentsScanResult result =
                latestScanResult;

        if (result == null
                || !result.isSuccessful()) {

            binding.contentsScanResultSummaryTextView
                    .setText(
                            ""
                    );

            binding.contentsScanResultSummaryTextView
                    .setVisibility(
                            View.GONE
                    );

            binding.reviewDetectedChaptersButton
                    .setVisibility(
                            View.GONE
                    );

            return;
        }

        int candidateCount =
                result.getCandidateCount();

        String summary =
                candidateCount
                        + (candidateCount == 1
                        ? " chapter candidate detected."
                        : " chapter candidates detected.");

        if (candidateCount <= 0) {
            summary =
                    "Text was read, but no chapter candidates "
                            + "were detected. Try a clearer image.";
        }

        binding.contentsScanResultSummaryTextView
                .setText(
                        summary
                );

        binding.contentsScanResultSummaryTextView
                .setVisibility(
                        View.VISIBLE
                );

        binding.reviewDetectedChaptersButton
                .setVisibility(
                        candidateCount > 0
                                ? View.VISIBLE
                                : View.GONE
                );
    }

    private void openReviewScreen() {
        BookContentsScanResult result =
                latestScanResult;

        if (processing
                || result == null
                || !result.isSuccessful()
                || !result.hasChapterCandidates()) {

            showError(
                    "No detected chapter candidates "
                            + "are available for review."
            );

            return;
        }

        Intent reviewIntent =
                BookContentsReviewActivity.createIntent(
                        this,
                        result
                );

        try {
            reviewLauncher.launch(
                    reviewIntent
            );

        } catch (RuntimeException exception) {
            showError(
                    getErrorMessage(
                            exception,
                            "The chapter review screen "
                                    + "could not be opened."
                    )
            );
        }
    }

    private void setProcessing(
            boolean processing
    ) {
        this.processing =
                processing;

        binding.contentsOcrProgressContainer
                .setVisibility(
                        processing
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.takeContentsPhotoButton
                .setEnabled(
                        !processing
                                && hasValidBook()
                );

        binding.chooseContentsGalleryButton
                .setEnabled(
                        !processing
                                && hasValidBook()
                );

        binding.startContentsOcrButton
                .setEnabled(
                        !processing
                                && hasValidBook()
                                && selectedImageUri != null
                );

        binding.reviewDetectedChaptersButton
                .setEnabled(
                        !processing
                );

        binding.backFromContentsScanButton
                .setEnabled(
                        !processing
                );
    }

    private void showFatalError(
            @NonNull String errorMessage
    ) {
        setProcessing(
                false
        );

        showError(
                errorMessage
        );

        binding.takeContentsPhotoButton
                .setEnabled(
                        false
                );

        binding.chooseContentsGalleryButton
                .setEnabled(
                        false
                );

        binding.startContentsOcrButton
                .setEnabled(
                        false
                );

        binding.reviewDetectedChaptersButton
                .setVisibility(
                        View.GONE
                );
    }

    private void showError(
            @NonNull String errorMessage
    ) {
        binding.contentsScanErrorTextView
                .setText(
                        errorMessage
                );

        binding.contentsScanErrorTextView
                .setVisibility(
                        View.VISIBLE
                );
    }

    private void hideError() {
        binding.contentsScanErrorTextView
                .setText(
                        ""
                );

        binding.contentsScanErrorTextView
                .setVisibility(
                        View.GONE
                );
    }

    private void finishWithReviewResult(
            boolean chaptersChanged,
            int importedCount,
            int skippedCount
    ) {
        Intent resultIntent =
                new Intent();

        resultIntent.putExtra(
                EXTRA_CHAPTERS_CHANGED,
                chaptersChanged
        );

        resultIntent.putExtra(
                EXTRA_IMPORTED_COUNT,
                importedCount
        );

        resultIntent.putExtra(
                EXTRA_SKIPPED_COUNT,
                skippedCount
        );

        setResult(
                Activity.RESULT_OK,
                resultIntent
        );

        finish();
    }

    private void cancelAndClose() {
        if (processing) {
            return;
        }

        setResult(
                Activity.RESULT_CANCELED
        );

        finish();
    }

    private boolean hasValidBook() {
        return bookRowId
                > INVALID_ROW_ID;
    }

    private boolean canUpdateScreen() {
        return !isFinishing()
                && !isDestroyed();
    }

    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {
        super.onSaveInstanceState(
                outState
        );

        Uri imageUri =
                selectedImageUri;

        if (imageUri != null) {
            outState.putString(
                    STATE_SELECTED_IMAGE_URI,
                    imageUri.toString()
            );
        }

        outState.putString(
                STATE_SELECTED_IMAGE_PATH,
                selectedImagePath
        );

        BookContentsScanResult result =
                latestScanResult;

        if (result != null) {
            outState.putSerializable(
                    STATE_SCAN_RESULT,
                    result
            );
        }
    }

    private void restoreState(
            @NonNull Bundle savedInstanceState
    ) {
        String imageUriText =
                safeText(
                        savedInstanceState.getString(
                                STATE_SELECTED_IMAGE_URI
                        )
                );

        if (!imageUriText.isEmpty()) {
            selectedImageUri =
                    Uri.parse(
                            imageUriText
                    );
        }

        selectedImagePath =
                safeText(
                        savedInstanceState.getString(
                                STATE_SELECTED_IMAGE_PATH
                        )
                );

        Serializable serializableResult =
                savedInstanceState.getSerializable(
                        STATE_SCAN_RESULT
                );

        if (serializableResult
                instanceof BookContentsScanResult) {

            latestScanResult =
                    (BookContentsScanResult) serializableResult;
        }
    }

    @NonNull
    private String getErrorMessage(
            @NonNull Exception exception,
            @NonNull String fallbackMessage
    ) {
        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return fallbackMessage;
        }

        return message.trim();
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
}