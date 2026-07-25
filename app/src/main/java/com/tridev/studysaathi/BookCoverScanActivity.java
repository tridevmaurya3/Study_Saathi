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

public final class BookCoverScanActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_SUBJECT_ROW_ID =
            "extra_target_subject_row_id";

    public static final String EXTRA_TARGET_PROFILE_ID =
            "extra_target_profile_id";

    public static final String EXTRA_TARGET_SUBJECT_NAME =
            "extra_target_subject_name";

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

    private ActivityResultLauncher<Intent>
            bookSetupResultLauncher;

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
    private BookCoverScanResult.ScanSource selectedScanSource =
            BookCoverScanResult.ScanSource.GALLERY;

    private long targetSubjectRowId;

    private long targetProfileId;

    @NonNull
    private String targetSubjectName =
            "";

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

        readTargetSubjectContext();
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

    private void readTargetSubjectContext() {
        Intent intent =
                getIntent();

        if (intent == null) {
            return;
        }

        targetSubjectRowId =
                intent.getLongExtra(
                        EXTRA_TARGET_SUBJECT_ROW_ID,
                        0L
                );

        targetProfileId =
                intent.getLongExtra(
                        EXTRA_TARGET_PROFILE_ID,
                        0L
                );

        targetSubjectName =
                safeText(
                        intent.getStringExtra(
                                EXTRA_TARGET_SUBJECT_NAME
                        )
                );
    }

    private void registerActivityResultLaunchers() {
        openBookCoverLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.OpenDocument(),
                        this::handleSelectedGalleryImage
                );

        takeBookCoverPictureLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.TakePicture(),
                        this::handleCameraCaptureResult
                );

        bookSetupResultLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .StartActivityForResult(),
                        result -> {
                            if (result.getResultCode()
                                    != RESULT_OK) {
                                return;
                            }

                            Intent resultData =
                                    result.getData();

                            if (resultData == null) {
                                setResult(
                                        RESULT_OK
                                );

                            } else {
                                setResult(
                                        RESULT_OK,
                                        resultData
                                );
                            }

                            finish();
                        }
                );
    }

    private void setupToolbar() {
        binding.toolbarBookCoverScan
                .setNavigationOnClickListener(
                        view ->
                                getOnBackPressedDispatcher()
                                        .onBackPressed()
                );

        if (!targetSubjectName.isEmpty()) {
            binding.toolbarBookCoverScan
                    .setSubtitle(
                            "Subject: "
                                    + targetSubjectName
                    );
        }
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
                                openManualBookEntry()
                );
    }

    private void captureBookCoverWithCamera() {
        if (operationInProgress) {
            return;
        }

        try {
            CameraFileTarget target =
                    createCameraFileTarget();

            pendingCameraImageUri =
                    target.contentUri;

            pendingCameraImagePath =
                    target.absoluteFilePath;

            takeBookCoverPictureLauncher.launch(
                    pendingCameraImageUri
            );

        } catch (IOException exception) {
            clearPendingCameraTarget();

            showMessage(
                    "Camera photo के लिए सुरक्षित file नहीं बन सकी।"
            );

        } catch (RuntimeException exception) {
            deletePrivateCameraFileSafely(
                    pendingCameraImagePath
            );

            clearPendingCameraTarget();

            showMessage(
                    "Camera app नहीं खोली जा सकी।"
            );
        }
    }

    @NonNull
    private CameraFileTarget createCameraFileTarget()
            throws IOException {

        File directory =
                new File(
                        getFilesDir(),
                        BOOK_COVER_DIRECTORY_NAME
                );

        if (!directory.exists()
                && !directory.mkdirs()) {

            throw new IOException(
                    "Book-cover directory could not be created."
            );
        }

        if (!directory.isDirectory()) {
            throw new IOException(
                    "Book-cover path is not a directory."
            );
        }

        File imageFile =
                File.createTempFile(
                        "book_cover_"
                                + System.currentTimeMillis()
                                + "_",
                        ".jpg",
                        directory
                );

        Uri contentUri =
                FileProvider.getUriForFile(
                        this,
                        getPackageName()
                                + ".fileprovider",
                        imageFile
                );

        return new CameraFileTarget(
                contentUri,
                imageFile.getAbsolutePath()
        );
    }

    private void handleCameraCaptureResult(
            @Nullable Boolean pictureSaved
    ) {
        Uri completedUri =
                pendingCameraImageUri;

        String completedPath =
                safeNullableText(
                        pendingCameraImagePath
                );

        if (!Boolean.TRUE.equals(
                pictureSaved
        )) {
            deletePrivateCameraFileSafely(
                    completedPath
            );

            clearPendingCameraTarget();

            showMessage(
                    "Camera photo capture cancel कर दिया गया।"
            );

            return;
        }

        if (completedUri == null
                || completedPath == null) {

            deletePrivateCameraFileSafely(
                    completedPath
            );

            clearPendingCameraTarget();

            showMessage(
                    "Camera photo सुरक्षित नहीं हो सकी।"
            );

            return;
        }

        deletePreviousSelectedCameraFile();

        selectedBookCoverUri =
                completedUri;

        selectedPrivateImagePath =
                completedPath;

        selectedScanSource =
                BookCoverScanResult
                        .ScanSource
                        .CAMERA;

        clearPendingCameraTarget();

        updateSelectedImageState();

        showMessage(
                "Book cover photo तैयार है। अब Scan करके Book खोजें।"
        );
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

        showMessage(
                "Book cover selected है। अब Scan करके Book खोजें।"
        );
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
             * कुछ Gallery/Document providers
             * permanent permission support नहीं करते।
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
                .ScanSource
                .CAMERA) {

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
        String safePath =
                safeNullableText(
                        filePath
                );

        if (safePath == null) {
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
                            safePath
                    );

            String allowedPath =
                    allowedDirectory
                            .getCanonicalPath();

            String requestedPath =
                    requestedFile
                            .getCanonicalPath();

            if (!requestedPath.startsWith(
                    allowedPath
                            + File.separator
            )) {
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
        boolean hasImage =
                selectedBookCoverUri != null;

        binding.containerBookCoverPlaceholder
                .setVisibility(
                        hasImage
                                ? View.GONE
                                : View.VISIBLE
                );

        binding.imageSelectedBookCover
                .setVisibility(
                        hasImage
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.buttonRemoveSelectedBookCover
                .setVisibility(
                        hasImage
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.buttonScanAndFindBook
                .setEnabled(
                        hasImage
                                && !operationInProgress
                );

        if (!hasImage) {
            binding.textSelectedImageSource
                    .setText(
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

            binding.textSelectedImageSource
                    .setText(
                            selectedScanSource
                                    == BookCoverScanResult
                                    .ScanSource
                                    .CAMERA
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

            binding.textSelectedImageSource
                    .setText(
                            "Selected image खोली नहीं जा सकी"
                    );

            showMessage(
                    "यह image खोली नहीं जा सकी। दूसरी image चुनें।"
            );
        }
    }

    private void startBookDiscovery() {
        if (operationInProgress) {
            return;
        }

        if (targetSubjectRowId <= 0L) {
            showSubjectRequiredDialog();

            return;
        }

        Uri imageUri =
                selectedBookCoverUri;

        if (imageUri == null) {
            showMessage(
                    "पहले book cover चुनें।"
            );

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
                            @NonNull
                            BookScanDiscoveryCoordinator
                                    .DiscoveryStage stage
                    ) {
                        updateProgressStage(
                                stage
                        );
                    }

                    @Override
                    public void onCoverScanCompleted(
                            @NonNull
                            BookCoverScanResult scanResult
                    ) {
                        /*
                         * OCR और barcode scan पूरा हो गया।
                         * Online matching इसके बाद जारी रहेगी।
                         */
                    }

                    @Override
                    public void onDiscoveryCompleted(
                            @NonNull
                            BookScanDiscoveryCoordinator
                                    .CompleteDiscoveryResult result
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
                            @NonNull
                            BookScanDiscoveryCoordinator
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
            @NonNull
            BookScanDiscoveryCoordinator
                    .CompleteDiscoveryResult result
    ) {
        try {
            BookMatchReviewData reviewData =
                    BookMatchReviewData
                            .fromDiscoveryResult(
                                    result,
                                    selectedBookCoverUri,
                                    selectedPrivateImagePath
                            );

            Intent reviewIntent =
                    BookMatchReviewActivity
                            .createIntent(
                                    this,
                                    reviewData
                            );

            reviewIntent.putExtra(
                    EXTRA_TARGET_SUBJECT_ROW_ID,
                    targetSubjectRowId
            );

            reviewIntent.putExtra(
                    EXTRA_TARGET_PROFILE_ID,
                    targetProfileId
            );

            reviewIntent.putExtra(
                    EXTRA_TARGET_SUBJECT_NAME,
                    targetSubjectName
            );

            bookSetupResultLauncher.launch(
                    reviewIntent
            );

        } catch (RuntimeException exception) {
            showMessage(
                    "Book review screen तैयार नहीं की जा सकी।"
            );
        }
    }

    private void openManualBookEntry() {
        if (operationInProgress) {
            return;
        }

        if (targetSubjectRowId <= 0L) {
            showSubjectRequiredDialog();

            return;
        }

        Intent manualBookIntent =
                ManualSchoolBookActivity
                        .createIntent(
                                this,
                                targetSubjectRowId
                        );

        bookSetupResultLauncher.launch(
                manualBookIntent
        );
    }

    private void showSubjectRequiredDialog() {
        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "School subject चुनना आवश्यक है"
                )
                .setMessage(
                        "Book जोड़ने के लिए Parent School Curriculum Setup से बच्चे का actual subject चुनें।"
                )
                .setPositiveButton(
                        "ठीक है",
                        null
                )
                .show();
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
            @NonNull
            BookScanDiscoveryCoordinator
                    .DiscoveryStage stage
    ) {
        if (stage
                == BookScanDiscoveryCoordinator
                .DiscoveryStage
                .COMPLETED
                || stage
                == BookScanDiscoveryCoordinator
                .DiscoveryStage
                .FAILED) {

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
            @NonNull
            BookScanDiscoveryCoordinator
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

        StringBuilder message =
                new StringBuilder();

        if (!hindiMessage.isEmpty()) {
            message.append(
                    hindiMessage
            );
        }

        if (!englishMessage.isEmpty()
                && !englishMessage.equalsIgnoreCase(
                hindiMessage
        )) {
            if (message.length() > 0) {
                message.append(
                        "\n\n"
                );
            }

            message.append(
                    englishMessage
            );
        }

        if (exception.hasHttpStatusCode()) {
            message.append(
                    "\n\nHTTP Status: "
            );

            message.append(
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
                                message.length() > 0
                                        ? message.toString()
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
                            openManualBookEntry()
            );
        }

        dialogBuilder.show();
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
        String safeUri =
                safeNullableText(
                        uriValue
                );

        if (safeUri == null) {
            return null;
        }

        try {
            return Uri.parse(
                    safeUri
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

    private void showMessage(
            @NonNull String message
    ) {
        Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_LONG
        ).show();
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
    }
}