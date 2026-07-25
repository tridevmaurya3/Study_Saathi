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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.content.model.BookCoverScanResult;
import com.tridev.studysaathi.data.content.model.OnlineBookSearchResult;
import com.tridev.studysaathi.data.content.scanner.BookCoverMetadataExtractor;
import com.tridev.studysaathi.data.content.search.BookDiscoveryCoordinator;
import com.tridev.studysaathi.data.content.search.BookScanDiscoveryCoordinator;
import com.tridev.studysaathi.data.content.search.OnlineBookMatchEvaluator;
import com.tridev.studysaathi.databinding.ActivityBookCoverScanBinding;

import java.util.List;
import java.util.Locale;

public final class BookCoverScanActivity
        extends AppCompatActivity {

    private static final String STATE_SELECTED_IMAGE_URI =
            "selected_book_cover_uri";

    private static final String STATE_SELECTED_SCAN_SOURCE =
            "selected_book_scan_source";

    private static final int MAXIMUM_WARNING_COUNT =
            3;

    private ActivityBookCoverScanBinding binding;

    private ActivityResultLauncher<String[]>
            openBookCoverLauncher;

    private BookScanDiscoveryCoordinator
            scanDiscoveryCoordinator;

    @Nullable
    private Uri selectedBookCoverUri;

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
    }

    private void setupToolbar() {
        binding.toolbarBookCoverScan
                .setNavigationOnClickListener(view ->
                        getOnBackPressedDispatcher()
                                .onBackPressed()
                );
    }

    private void setupClickListeners() {
        binding.buttonCaptureBookCover
                .setOnClickListener(view ->
                        showCameraComingNextMessage()
                );

        binding.buttonChooseBookCover
                .setOnClickListener(view ->
                        openBookCoverGallery()
                );

        binding.buttonRemoveSelectedBookCover
                .setOnClickListener(view ->
                        removeSelectedBookCover()
                );

        binding.buttonScanAndFindBook
                .setOnClickListener(view ->
                        startBookDiscovery()
                );

        binding.buttonEnterBookManually
                .setOnClickListener(view ->
                        showManualEntryComingNextMessage()
                );
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

        selectedBookCoverUri =
                selectedUri;

        selectedScanSource =
                BookCoverScanResult.ScanSource.GALLERY;

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

        selectedBookCoverUri =
                null;

        selectedScanSource =
                BookCoverScanResult.ScanSource.GALLERY;

        binding.imageSelectedBookCover
                .setImageDrawable(
                        null
                );

        updateSelectedImageState();
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
            selectedBookCoverUri =
                    null;

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
                imageUri.toString(),
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
                         * Raw OCR और barcode result
                         * मिल चुका है।
                         *
                         * अभी final online result का
                         * इंतजार किया जाएगा।
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

                        showDiscoveryResult(
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

    private void showDiscoveryResult(
            @NonNull BookScanDiscoveryCoordinator
                    .CompleteDiscoveryResult completeResult
    ) {
        BookDiscoveryCoordinator.BookDiscoveryResult
                discoveryResult =
                completeResult.getDiscoveryResult();

        BookCoverMetadataExtractor.DetectedBookMetadata
                detectedMetadata =
                discoveryResult.getDetectedMetadata();

        StringBuilder messageBuilder =
                new StringBuilder();

        appendResultLine(
                messageBuilder,
                "Detected Book",
                detectedMetadata.getBookTitle()
        );

        appendResultLine(
                messageBuilder,
                "Subject",
                detectedMetadata.getSubjectName()
        );

        appendResultLine(
                messageBuilder,
                "Class",
                detectedMetadata.getClassName()
        );

        appendResultLine(
                messageBuilder,
                "Board",
                detectedMetadata.getEducationBoard()
        );

        appendResultLine(
                messageBuilder,
                "Publisher",
                detectedMetadata.getPublisherName()
        );

        appendResultLine(
                messageBuilder,
                "ISBN",
                detectedMetadata.getPreferredIsbn()
        );

        OnlineBookMatchEvaluator.RankedBookResult
                bestMatch =
                discoveryResult.getBestMatch();

        if (bestMatch != null) {
            OnlineBookSearchResult bookResult =
                    bestMatch.getBookResult();

            OnlineBookMatchEvaluator.MatchEvaluation
                    evaluation =
                    bestMatch.getEvaluation();

            appendSectionHeading(
                    messageBuilder,
                    "Best Online Match"
            );

            appendResultLine(
                    messageBuilder,
                    "Title",
                    bookResult.getBookTitle()
            );

            appendResultLine(
                    messageBuilder,
                    "Author",
                    bookResult.getAuthorsDisplayText()
            );

            appendResultLine(
                    messageBuilder,
                    "Publisher",
                    bookResult.getPublisherName()
            );

            appendResultLine(
                    messageBuilder,
                    "Online ISBN",
                    bookResult.getPreferredIsbn()
            );

            appendResultLine(
                    messageBuilder,
                    "Source",
                    bookResult.getProvider()
                            .getEnglishLabel()
            );

            appendResultLine(
                    messageBuilder,
                    "Match",
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            evaluation.getOverallMatchScore()
                    )
            );

            appendResultLine(
                    messageBuilder,
                    "Access",
                    bookResult.getAccessType()
                            .getEnglishLabel()
            );

            if (bookResult.hasAuthorizedDownload()) {
                appendResultLine(
                        messageBuilder,
                        "Download",
                        "Authorized download available"
                );

            } else if (bookResult.hasPreview()) {
                appendResultLine(
                        messageBuilder,
                        "Download",
                        "Preview available; full download not verified"
                );

            } else {
                appendResultLine(
                        messageBuilder,
                        "Download",
                        "Only book information is available"
                );
            }

        } else {
            appendSectionHeading(
                    messageBuilder,
                    "Online Result"
            );

            messageBuilder.append(
                    "कोई विश्वसनीय online match नहीं मिला।"
            );
        }

        appendWarnings(
                messageBuilder,
                discoveryResult.getWarnings()
        );

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        discoveryResult
                                .hasHighConfidenceMatch()
                                ? "Book match मिल गया"
                                : "Parent review आवश्यक है"
                )
                .setMessage(
                        messageBuilder.toString()
                )
                .setPositiveButton(
                        "ठीक है",
                        null
                )
                .setNeutralButton(
                        "दूसरा cover चुनें",
                        (dialog, which) ->
                                removeSelectedBookCover()
                )
                .show();
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

    private void appendResultLine(
            @NonNull StringBuilder builder,
            @NonNull String label,
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(
                    '\n'
            );
        }

        builder.append(
                label
        );

        builder.append(
                ": "
        );

        builder.append(
                safeValue
        );
    }

    private void appendSectionHeading(
            @NonNull StringBuilder builder,
            @NonNull String heading
    ) {
        if (builder.length() > 0) {
            builder.append(
                    "\n\n"
            );
        }

        builder.append(
                heading
        );

        builder.append(
                "\n"
        );
    }

    private void appendWarnings(
            @NonNull StringBuilder builder,
            @NonNull List<String> warnings
    ) {
        if (warnings.isEmpty()) {
            return;
        }

        appendSectionHeading(
                builder,
                "Review Notes"
        );

        int warningCount =
                Math.min(
                        MAXIMUM_WARNING_COUNT,
                        warnings.size()
                );

        for (int index = 0;
             index < warningCount;
             index++) {

            if (index > 0) {
                builder.append(
                        '\n'
                );
            }

            builder.append(
                    "• "
            );

            builder.append(
                    warnings.get(
                            index
                    )
            );
        }

        if (warnings.size()
                > warningCount) {

            builder.append(
                    "\n• "
            );

            builder.append(
                    warnings.size()
                            - warningCount
            );

            builder.append(
                    " additional review notes"
            );
        }
    }

    private void showCameraComingNextMessage() {
        Snackbar.make(
                binding.getRoot(),
                "High-quality Camera capture अगले step में जोड़ा जाएगा।",
                Snackbar.LENGTH_LONG
        ).show();
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

        String savedUri =
                safeText(
                        savedInstanceState.getString(
                                STATE_SELECTED_IMAGE_URI
                        )
                );

        if (!savedUri.isEmpty()) {
            try {
                selectedBookCoverUri =
                        Uri.parse(
                                savedUri
                        );

            } catch (RuntimeException ignored) {
                selectedBookCoverUri =
                        null;
            }
        }

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

    @Override
    protected void onDestroy() {
        if (scanDiscoveryCoordinator != null) {
            scanDiscoveryCoordinator.close();
        }

        binding =
                null;

        super.onDestroy();
    }
}