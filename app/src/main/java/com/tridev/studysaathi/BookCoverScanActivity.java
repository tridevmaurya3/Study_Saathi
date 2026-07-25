package com.tridev.studysaathi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.content.model.BookCoverScanResult;
import com.tridev.studysaathi.data.content.model.BookMatchReviewData;
import com.tridev.studysaathi.data.content.scanner.BookCoverMetadataExtractor;
import com.tridev.studysaathi.data.content.search.BookScanDiscoveryCoordinator;
import com.tridev.studysaathi.databinding.ActivityBookCoverScanBinding;

import java.io.File;
import java.io.IOException;

public final class BookCoverScanActivity
        extends AppCompatActivity {

    private static final String STATE_SELECTED_IMAGE_URI =
            "selected_book_cover_uri";

    private static final String STATE_SELECTED_SCAN_SOURCE =
            "selected_book_scan_source";

    private static final String STATE_SELECTED_PRIVATE_IMAGE_PATH =
            "selected_private_book_cover_path";

    private static final String STATE_PENDING_CAMERA_URI =
            "pending_camera_book_cover_uri";

    private static final String STATE_PENDING_CAMERA_FILE_PATH =
            "pending_camera_book_cover_path";

    private static final String BOOK_COVER_DIRECTORY_NAME =
            "book_covers";

    private ActivityBookCoverScanBinding binding;

    private ActivityResultLauncher<String[]>
            openBookCoverLauncher;

    private ActivityResultLauncher<Uri>
            takeBookCoverPictureLauncher;

    private BookScanDiscoveryCoordinator
            scanDiscoveryCoordinator;

    @Nullable
    private Uri selectedBookCoverUri;

    @Nullable
    private String selectedPrivateImagePath;

    @Nullable
    private Uri pendingCameraImageUri;

    @Nullable
    private String pendingCameraImagePath;

    @NonNull
    private BookCoverScanResult.ScanSource
            selectedScanSource =
            BookCoverScanResult.ScanSource.GALLERY;

    private boolean operationInProgress;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivityBookCoverScanBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        scanDiscoveryCoordinator =
                new BookScanDiscoveryCoordinator(
                        this
                );

        registerActivityResultLaunchers();
        setupToolbar();
        setupClickListeners();

        restoreActivityState(
                savedInstanceState
        );

        updateSelectedImageState();

        setOperationState(
                false
        );
    }

    private void registerActivityResultLaunchers() {
        openBookCoverLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .OpenDocument(),
                        this::handleSelectedGalleryImage
                );

        takeBookCoverPictureLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .TakePicture(),
                        this::handleCameraCaptureResult
                );
    }

    private void setupToolbar() {
        binding.toolbarBookCoverScan
                .setNavigationOnClickListener(
                        view ->
                                getOnBackPressedDispatcher()
                                        .onBackPressed()
                );
    }

    private void setupClickListeners() {
        binding.buttonCaptureBookCover
                .setOnClickListener(
                        view ->
                                captureBookCoverWithCamera()
                );

        binding.buttonChooseBookCover
                .setOnClickListener(
                        view ->
                                openBookCoverGallery()
                );

        binding.buttonRemoveSelectedBookCover
                .setOnClickListener(
                        view ->
                                removeSelectedBookCover()
                );

        binding.buttonScanAndFindBook
                .setOnClickListener(
                        view ->
                                startBookDiscovery()
                );

        binding.buttonEnterBookManually
                .setOnClickListener(
                        view ->
                                showManualEntryComingNextMessage()
                );
    }

    private void captureBookCoverWithCamera() {
        if (operationInProgress) {
            return;
        }

        try {
            CameraFileTarget cameraFileTarget =
                    createCameraFileTarget();

            pendingCameraImageUri =
                    cameraFileTarget.getContentUri();

            pendingCameraImagePath =
                    cameraFileTarget.getAbsoluteFilePath();

            takeBookCoverPictureLauncher.launch(
                    pendingCameraImageUri
            );

        } catch (IOException exception) {
            clearPendingCameraTarget();

            Snackbar.make(
                    binding.getRoot(),
                    "Camera photo के लिए सुरक्षित file नहीं बन सकी।",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (RuntimeException exception) {
            deletePrivateCameraFileSafely(
                    pendingCameraImagePath
            );

            clearPendingCameraTarget();

            Snackbar.make(
                    binding.getRoot(),
                    "Camera app नहीं खोली जा सकी।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @NonNull
    private CameraFileTarget createCameraFileTarget()
            throws IOException {

        File bookCoverDirectory =
                new File(
                        getFilesDir(),
                        BOOK_COVER_DIRECTORY_NAME
                );

        if (!bookCoverDirectory.exists()
                && !bookCoverDirectory.mkdirs()) {

            throw new IOException(
                    "Book-cover directory could not be created."
            );
        }

        if (!bookCoverDirectory.isDirectory()) {
            throw new IOException(
                    "Book-cover path is not a directory."
            );
        }

        File cameraImageFile =
                File.createTempFile(
                        "book_cover_"
                                + System.currentTimeMillis()
                                + "_",
                        ".jpg",
                        bookCoverDirectory
                );

        Uri cameraContentUri =
                FileProvider.getUriForFile(
                        this,
                        getPackageName()
                                + ".fileprovider",
                        cameraImageFile
                );

        return new CameraFileTarget(
                cameraContentUri,
                cameraImageFile.getAbsolutePath()
        );
    }

    private void handleCameraCaptureResult(
            @Nullable Boolean pictureSaved
    ) {
        Uri completedCameraUri =
                pendingCameraImageUri;

        String completedCameraPath =
                safeNullableText(
                        pendingCameraImagePath
                );

        if (!Boolean.TRUE.equals(
                pictureSaved
        )) {
            deletePrivateCameraFileSafely(
                    completedCameraPath
            );

            clearPendingCameraTarget();

            Snackbar.make(
                    binding.getRoot(),
                    "Camera photo capture cancel कर दिया गया।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (completedCameraUri == null
                || completedCameraPath == null) {

            deletePrivateCameraFileSafely(
                    completedCameraPath
            );

            clearPendingCameraTarget();

            Snackbar.make(
                    binding.getRoot(),
                    "Camera photo सुरक्षित नहीं हो सकी।",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        deletePreviousSelectedCameraFile();

        selectedBookCoverUri =
                completedCameraUri;

        selectedPrivateImagePath =
                completedCameraPath;

        selectedScanSource =
                BookCoverScanResult
                        .ScanSource
                        .CAMERA;

        clearPendingCameraTarget();

        updateSelectedImageState();

        Snackbar.make(
                binding.getRoot(),
                "Book cover photo तैयार है। अब Scan करके Book खोजें।",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void clearPendingCameraTarget() {
        pendingCameraImageUri =
                null;

        pendingCameraImagePath =
                null;
    }

    private void openBookCoverGallery() {
        if (operationInProgress) {
            return;
        }

        openBookCoverLauncher.launch(
                new String[]{
                        "image/*"
                }
        );
    }

    private void handleSelectedGalleryImage(
            @Nullable Uri selectedUri
    ) {
        if (selectedUri == null) {
            return;
        }

        tryTakePersistableReadPermission(
                selectedUri
        );

        deletePreviousSelectedCameraFile();

        selectedBookCoverUri =
                selectedUri;

        selectedPrivateImagePath =
                null;

        selectedScanSource =
                BookCoverScanResult
                        .ScanSource
                        .GALLERY;

        updateSelectedImageState();

        Snackbar.make(
                binding.getRoot(),
                "Book cover selected है। अब Scan करके Book खोजें।",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void tryTakePersistableReadPermission(
            @NonNull Uri selectedUri
    ) {
        try {
            getContentResolver()
                    .takePersistableUriPermission(
                            selectedUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

        } catch (SecurityException ignored) {
            /*
             * कुछ document providers persistent
             * permission support नहीं करते।
             *
             * Current activity session में प्राप्त
             * read permission फिर भी उपयोग की जाएगी।
             */
        }
    }

    private void removeSelectedBookCover() {
        if (operationInProgress) {
            return;
        }

        deletePreviousSelectedCameraFile();

        selectedBookCoverUri =
                null;

        selectedPrivateImagePath =
                null;

        selectedScanSource =
                BookCoverScanResult
                        .ScanSource
                        .GALLERY;

        binding.imageSelectedBookCover
                .setImageDrawable(
                        null
                );

        updateSelectedImageState();
    }

    private void deletePreviousSelectedCameraFile() {
        if (selectedScanSource
                != BookCoverScanResult
                .ScanSource.CAMERA) {

            selectedPrivateImagePath =
                    null;

            return;
        }

        deletePrivateCameraFileSafely(
                selectedPrivateImagePath
        );

        selectedPrivateImagePath =
                null;
    }

    private void deletePrivateCameraFileSafely(
            @Nullable String filePath
    ) {
        String safeFilePath =
                safeNullableText(
                        filePath
                );

        if (safeFilePath == null) {
            return;
        }

        try {
            File allowedDirectory =
                    new File(
                            getFilesDir(),
                            BOOK_COVER_DIRECTORY_NAME
                    );

            File requestedFile =
                    new File(
                            safeFilePath
                    );

            String allowedDirectoryPath =
                    allowedDirectory
                            .getCanonicalPath();

            String requestedFilePath =
                    requestedFile
                            .getCanonicalPath();

            boolean fileInsideAllowedDirectory =
                    requestedFilePath.startsWith(
                            allowedDirectoryPath
                                    + File.separator
                    );

            if (!fileInsideAllowedDirectory) {
                return;
            }

            if (requestedFile.exists()
                    && requestedFile.isFile()) {

                //noinspection ResultOfMethodCallIgnored
                requestedFile.delete();
            }

        } catch (IOException
                 | SecurityException ignored) {

            /*
             * Cleanup failure app flow को नहीं रोकेगी।
             */
        }
    }

    private void updateSelectedImageState() {
        boolean hasSelectedImage =
                selectedBookCoverUri != null;

        binding.containerBookCoverPlaceholder
                .setVisibility(
                        hasSelectedImage
                                ? View.GONE
                                : View.VISIBLE
                );

        binding.imageSelectedBookCover
                .setVisibility(
                        hasSelectedImage
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.buttonRemoveSelectedBookCover
                .setVisibility(
                        hasSelectedImage
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.buttonScanAndFindBook
                .setEnabled(
                        hasSelectedImage
                                && !operationInProgress
                );

        if (!hasSelectedImage) {
            binding.textSelectedImageSource.setText(
                    "Camera या Gallery से cover चुनें"
            );

            return;
        }

        try {
            binding.imageSelectedBookCover
                    .setImageURI(
                            null
                    );

            binding.imageSelectedBookCover
                    .setImageURI(
                            selectedBookCoverUri
                    );

            binding.textSelectedImageSource.setText(
                    selectedScanSource
                            == BookCoverScanResult
                            .ScanSource.CAMERA
                            ? "Camera से लिया गया cover"
                            : "Gallery से चुना गया cover"
            );

        } catch (RuntimeException exception) {
            deletePreviousSelectedCameraFile();

            selectedBookCoverUri =
                    null;

            selectedPrivateImagePath =
                    null;

            selectedScanSource =
                    BookCoverScanResult
                            .ScanSource
                            .GALLERY;

            binding.imageSelectedBookCover
                    .setImageDrawable(
                            null
                    );

            binding.containerBookCoverPlaceholder
                    .setVisibility(
                            View.VISIBLE
                    );

            binding.imageSelectedBookCover
                    .setVisibility(
                            View.GONE
                    );

            binding.buttonRemoveSelectedBookCover
                    .setVisibility(
                            View.GONE
                    );

            binding.buttonScanAndFindBook
                    .setEnabled(
                            false
                    );

            binding.textSelectedImageSource.setText(
                    "Selected image खोली नहीं जा सकी"
            );

            Snackbar.make(
                    binding.getRoot(),
                    "यह image खोली नहीं जा सकी। दूसरी image चुनें।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private void startBookDiscovery() {
        if (operationInProgress) {
            return;
        }

        Uri imageUri =
                selectedBookCoverUri;

        if (imageUri == null) {
            Snackbar.make(
                    binding.getRoot(),
                    "पहले book cover चुनें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        setOperationState(
                true
        );

        updateProgressStage(
                BookScanDiscoveryCoordinator
                        .DiscoveryStage
                        .PREPARING_IMAGE
        );

        BookCoverMetadataExtractor.ExtractionContext
                extractionContext =
                BookCoverMetadataExtractor
                        .ExtractionContext
                        .empty();

        scanDiscoveryCoordinator.scanAndDiscover(
                imageUri,
                selectedPrivateImagePath,
                selectedScanSource,
                extractionContext,
                new BookScanDiscoveryCoordinator
                        .ScanDiscoveryCallback() {

                    @Override
                    public void onStageChanged(
                            @NonNull BookScanDiscoveryCoordinator
                                    .DiscoveryStage stage
                    ) {
                        updateProgressStage(
                                stage
                        );
                    }

                    @Override
                    public void onCoverScanCompleted(
                            @NonNull BookCoverScanResult
                                    scanResult
                    ) {
                        /*
                         * OCR और barcode scanning
                         * पूरा हो चुका है।
                         *
                         * अभी online matching और
                         * result ranking पूरी होगी।
                         */
                    }

                    @Override
                    public void onDiscoveryCompleted(
                            @NonNull BookScanDiscoveryCoordinator
                                    .CompleteDiscoveryResult
                                    result
                    ) {
                        setOperationState(
                                false
                        );

                        openBookMatchReviewScreen(
                                result
                        );
                    }

                    @Override
                    public void onDiscoveryFailed(
                            @NonNull BookScanDiscoveryCoordinator
                                    .BookScanDiscoveryException
                                    exception
                    ) {
                        setOperationState(
                                false
                        );

                        showDiscoveryFailure(
                                exception
                        );
                    }
                }
        );
    }

    private void openBookMatchReviewScreen(
            @NonNull BookScanDiscoveryCoordinator
                    .CompleteDiscoveryResult completeResult
    ) {
        try {
            BookMatchReviewData reviewData =
                    BookMatchReviewData
                            .fromDiscoveryResult(
                                    completeResult,
                                    selectedBookCoverUri,
                                    selectedPrivateImagePath
                            );

            Intent reviewIntent =
                    BookMatchReviewActivity
                            .createIntent(
                                    BookCoverScanActivity.this,
                                    reviewData
                            );

            startActivity(
                    reviewIntent
            );

        } catch (RuntimeException exception) {
            Snackbar.make(
                    binding.getRoot(),
                    "Book review screen तैयार नहीं की जा सकी।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private void setOperationState(
            boolean inProgress
    ) {
        operationInProgress =
                inProgress;

        binding.cardBookScanProgress
                .setVisibility(
                        inProgress
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.progressBookCoverImage
                .setVisibility(
                        inProgress
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.buttonCaptureBookCover
                .setEnabled(
                        !inProgress
                );

        binding.buttonChooseBookCover
                .setEnabled(
                        !inProgress
                );

        binding.buttonRemoveSelectedBookCover
                .setEnabled(
                        !inProgress
                );

        binding.buttonEnterBookManually
                .setEnabled(
                        !inProgress
                );

        binding.buttonScanAndFindBook
                .setEnabled(
                        !inProgress
                                && selectedBookCoverUri != null
                );

        if (!inProgress) {
            binding.textBookScanProgressTitle
                    .setText(
                            "Book cover scan हो रहा है"
                    );

            binding.textBookScanProgressDescription
                    .setText(
                            "कृपया process पूरा होने तक इसी screen पर रहें।"
                    );
        }
    }

    private void updateProgressStage(
            @NonNull BookScanDiscoveryCoordinator
                    .DiscoveryStage stage
    ) {
        if (stage
                == BookScanDiscoveryCoordinator
                .DiscoveryStage.COMPLETED
                || stage
                == BookScanDiscoveryCoordinator
                .DiscoveryStage.FAILED) {

            return;
        }

        binding.cardBookScanProgress
                .setVisibility(
                        View.VISIBLE
                );

        binding.textBookScanProgressTitle
                .setText(
                        stage.getHindiMessage()
                );

        binding.textBookScanProgressDescription
                .setText(
                        stage.getEnglishMessage()
                );
    }

    private void showDiscoveryFailure(
            @NonNull BookScanDiscoveryCoordinator
                    .BookScanDiscoveryException exception
    ) {
        String hindiMessage =
                safeText(
                        exception.getHindiMessage()
                );

        String englishMessage =
                safeText(
                        exception.getMessage()
                );

        StringBuilder messageBuilder =
                new StringBuilder();

        if (!hindiMessage.isEmpty()) {
            messageBuilder.append(
                    hindiMessage
            );
        }

        if (!englishMessage.isEmpty()
                && !englishMessage.equalsIgnoreCase(
                hindiMessage
        )) {
            if (messageBuilder.length() > 0) {
                messageBuilder.append(
                        "\n\n"
                );
            }

            messageBuilder.append(
                    englishMessage
            );
        }

        if (exception.hasHttpStatusCode()) {
            messageBuilder.append(
                    "\n\nHTTP Status: "
            );

            messageBuilder.append(
                    exception.getHttpStatusCode()
            );
        }

        MaterialAlertDialogBuilder dialogBuilder =
                new MaterialAlertDialogBuilder(
                        this
                )
                        .setTitle(
                                "Book search पूरी नहीं हुई"
                        )
                        .setMessage(
                                messageBuilder.length() > 0
                                        ? messageBuilder.toString()
                                        : "पुस्तक खोज के दौरान समस्या हुई।"
                        )
                        .setNegativeButton(
                                "बंद करें",
                                null
                        );

        if (exception.canRetry()
                && selectedBookCoverUri != null) {

            dialogBuilder.setPositiveButton(
                    "फिर से कोशिश करें",
                    (dialog, which) ->
                            startBookDiscovery()
            );
        }

        if (exception.shouldOfferManualEntry()) {
            dialogBuilder.setNeutralButton(
                    "Manual Entry",
                    (dialog, which) ->
                            showManualEntryComingNextMessage()
            );
        }

        dialogBuilder.show();
    }

    private void showManualEntryComingNextMessage() {
        Snackbar.make(
                binding.getRoot(),
                "Manual Book Entry screen आगे के step में जोड़ी जाएगी।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void restoreActivityState(
            @Nullable Bundle savedInstanceState
    ) {
        if (savedInstanceState == null) {
            return;
        }

        selectedBookCoverUri =
                parseUriSafely(
                        savedInstanceState.getString(
                                STATE_SELECTED_IMAGE_URI
                        )
                );

        pendingCameraImageUri =
                parseUriSafely(
                        savedInstanceState.getString(
                                STATE_PENDING_CAMERA_URI
                        )
                );

        selectedPrivateImagePath =
                safeNullableText(
                        savedInstanceState.getString(
                                STATE_SELECTED_PRIVATE_IMAGE_PATH
                        )
                );

        pendingCameraImagePath =
                safeNullableText(
                        savedInstanceState.getString(
                                STATE_PENDING_CAMERA_FILE_PATH
                        )
                );

        String savedSource =
                safeText(
                        savedInstanceState.getString(
                                STATE_SELECTED_SCAN_SOURCE
                        )
                );

        if (!savedSource.isEmpty()) {
            try {
                selectedScanSource =
                        BookCoverScanResult
                                .ScanSource
                                .valueOf(
                                        savedSource
                                );

            } catch (IllegalArgumentException ignored) {
                selectedScanSource =
                        BookCoverScanResult
                                .ScanSource
                                .GALLERY;
            }
        }
    }

    @Nullable
    private Uri parseUriSafely(
            @Nullable String uriValue
    ) {
        String safeUriValue =
                safeNullableText(
                        uriValue
                );

        if (safeUriValue == null) {
            return null;
        }

        try {
            return Uri.parse(
                    safeUriValue
            );

        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {
        super.onSaveInstanceState(
                outState
        );

        if (selectedBookCoverUri != null) {
            outState.putString(
                    STATE_SELECTED_IMAGE_URI,
                    selectedBookCoverUri.toString()
            );
        }

        if (selectedPrivateImagePath != null) {
            outState.putString(
                    STATE_SELECTED_PRIVATE_IMAGE_PATH,
                    selectedPrivateImagePath
            );
        }

        if (pendingCameraImageUri != null) {
            outState.putString(
                    STATE_PENDING_CAMERA_URI,
                    pendingCameraImageUri.toString()
            );
        }

        if (pendingCameraImagePath != null) {
            outState.putString(
                    STATE_PENDING_CAMERA_FILE_PATH,
                    pendingCameraImagePath
            );
        }

        outState.putString(
                STATE_SELECTED_SCAN_SOURCE,
                selectedScanSource.name()
        );
    }

    @NonNull
    private String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    @Nullable
    private String safeNullableText(
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        return safeValue.isEmpty()
                ? null
                : safeValue;
    }

    @Override
    protected void onDestroy() {
        if (scanDiscoveryCoordinator != null) {
            scanDiscoveryCoordinator.close();
        }

        binding =
                null;

        super.onDestroy();
    }

    private static final class CameraFileTarget {

        @NonNull
        private final Uri contentUri;

        @NonNull
        private final String absoluteFilePath;

        private CameraFileTarget(
                @NonNull Uri contentUri,
                @NonNull String absoluteFilePath
        ) {
            this.contentUri =
                    contentUri;

            this.absoluteFilePath =
                    absoluteFilePath;
        }

        @NonNull
        private Uri getContentUri() {
            return contentUri;
        }

        @NonNull
        private String getAbsoluteFilePath() {
            return absoluteFilePath;
        }
    }
}