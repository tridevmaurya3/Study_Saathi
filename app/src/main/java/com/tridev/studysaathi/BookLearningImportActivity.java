package com.tridev.studysaathi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.content.importer
        .BookDocumentPageExtractor;
import com.tridev.studysaathi.data.content.importer
        .BookLearningSourcePicker;
import com.tridev.studysaathi.data.content.model
        .BookLearningImportRequest;
import com.tridev.studysaathi.data.content.parser
        .BookChapterBoundaryDetector;
import com.tridev.studysaathi.data.content.parser
        .BookChapterSectionExtractor;
import com.tridev.studysaathi.data.content.scanner
        .BookPageOcrScanner;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.data.repository
        .BookExtractedContentSaveCoordinator;
import com.tridev.studysaathi.data.repository
        .BookChapterPageImportCoordinator;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterRepository;
import com.tridev.studysaathi.databinding
        .ActivityBookLearningImportBinding;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parent द्वारा full-book PDF/image चुनने की entry screen.
 */
public final class BookLearningImportActivity
        extends AppCompatActivity {

    public static final String EXTRA_BOOK_ROW_ID =
            "extra_book_row_id";

    public static final String EXTRA_BOOK_TITLE =
            "extra_book_title";

    private static final long INVALID_BOOK_ROW_ID =
            0L;

    private ActivityBookLearningImportBinding binding;

    private long bookRowId =
            INVALID_BOOK_ROW_ID;

    @NonNull
    private String bookTitle = "";

    @Nullable
    private BookLearningImportRequest importRequest;

    private BookDocumentPageExtractor pageExtractor;

    private BookPageOcrScanner pageOcrScanner;

    private BookExtractedContentSaveCoordinator
            extractedContentSaveCoordinator;

    private BookChapterPageImportCoordinator
            pageImportCoordinator;

    private SchoolBookChapterRepository
            chapterRepository;

    @Nullable
    private BookPageOcrScanner.BookOcrResult
            latestOcrResult;

    @Nullable
    private BookChapterBoundaryDetector.DetectionResult
            latestBoundaryResult;

    @NonNull
    private final ArrayList<BookChapterBoundaryReviewActivity
            .ApprovedChapterBoundary> approvedBoundaries =
            new ArrayList<>();

    @NonNull
    private final ArrayList<BookChapterSectionExtractor
            .ExtractedChapterContent> extractedChapterContents =
            new ArrayList<>();

    private boolean extractingPages;

    private boolean draftsReadyForReview;

    @NonNull
    private final ActivityResultLauncher<Intent>
            sourcePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),
                    result -> {
                        if (result.getResultCode()
                                != Activity.RESULT_OK) {
                            return;
                        }

                        Intent resultData =
                                result.getData();

                        Uri sourceUri =
                                resultData == null
                                        ? null
                                        : resultData.getData();

                        if (sourceUri == null) {
                            showMessage(
                                    "Selected book file "
                                            + "could not be read."
                            );
                            return;
                        }

                        handleSelectedSource(
                                sourceUri
                        );
                    }
            );

    @NonNull
    private final ActivityResultLauncher<Intent>
            boundaryReviewLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),
                    result ->
                            handleBoundaryReviewResult(
                                    result.getResultCode(),
                                    result.getData()
                            )
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
                        BookLearningImportActivity.class
                );

        intent.putExtra(
                EXTRA_BOOK_ROW_ID,
                bookRowId
        );

        intent.putExtra(
                EXTRA_BOOK_TITLE,
                safeText(bookTitle)
        );

        return intent;
    }

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityBookLearningImportBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        pageExtractor =
                new BookDocumentPageExtractor(
                        this
                );

        pageOcrScanner =
                new BookPageOcrScanner(
                        this
                );

        extractedContentSaveCoordinator =
                new BookExtractedContentSaveCoordinator(
                        this
                );

        pageImportCoordinator =
                new BookChapterPageImportCoordinator(
                        this
                );

        chapterRepository =
                new SchoolBookChapterRepository(
                        this
                );

        readArguments();

        if (bookRowId <= INVALID_BOOK_ROW_ID) {
            showMessage(
                    "Exact book details are missing."
            );
            finish();
            return;
        }

        setupToolbar();
        setupClickListeners();
        showEmptySelection();
        showBookTitle();
    }

    @Override
    protected void onDestroy() {
        if (pageExtractor != null) {
            pageExtractor.close();
        }

        if (pageOcrScanner != null) {
            pageOcrScanner.close();
        }

        if (pageImportCoordinator != null) {
            pageImportCoordinator.close();
        }

        super.onDestroy();
    }

    private void readArguments() {
        bookRowId =
                getIntent().getLongExtra(
                        EXTRA_BOOK_ROW_ID,
                        INVALID_BOOK_ROW_ID
                );

        bookTitle =
                safeText(
                        getIntent().getStringExtra(
                                EXTRA_BOOK_TITLE
                        )
                );
    }

    private void setupToolbar() {
        binding.bookLearningImportToolbar
                .setNavigationOnClickListener(
                        ignored -> finish()
                );
    }

    private void setupClickListeners() {
        binding.chooseBookLearningSourceButton
                .setOnClickListener(
                        ignored ->
                                sourcePickerLauncher.launch(
                                        BookLearningSourcePicker
                                                .createPickerIntent()
                                )
                );

        binding.startBookLearningImportButton
                .setOnClickListener(
                        ignored -> {
                            if (draftsReadyForReview) {
                                openSavedChaptersForReview();
                                return;
                            }

                            if (latestBoundaryResult != null
                                    && latestBoundaryResult
                                    .isSuccessful()) {
                                openBoundaryReview();
                                return;
                            }

                            prepareChapterDetection();
                        }
                );

        binding.cancelBookLearningImportButton
                .setOnClickListener(
                        ignored -> finish()
                );
    }

    private void showBookTitle() {
        if (bookTitle.isEmpty()) {
            return;
        }

        binding.bookLearningImportToolbar
                .setSubtitle(bookTitle);
    }

    private void handleSelectedSource(
            @NonNull Uri sourceUri
    ) {
        latestOcrResult = null;
        latestBoundaryResult = null;
        draftsReadyForReview = false;
        approvedBoundaries.clear();
        extractedChapterContents.clear();

        setSelectionControlsEnabled(false);

        try {
            importRequest =
                    BookLearningSourcePicker
                            .createImportRequest(
                                    this,
                                    bookRowId,
                                    sourceUri
                            );

            showSelectedSource(
                    importRequest
            );
        } catch (IOException
                 | IllegalArgumentException exception) {
            importRequest = null;
            showEmptySelection();
            showMessage(
                    readableMessage(
                            exception,
                            "Please select a readable "
                                    + "PDF or image file."
                    )
            );
        } finally {
            setSelectionControlsEnabled(true);
        }
    }

    private void showSelectedSource(
            @NonNull BookLearningImportRequest request
    ) {
        binding.selectedBookLearningSourceCard
                .setVisibility(View.VISIBLE);

        binding.noBookLearningSourceTextView
                .setVisibility(View.GONE);

        binding.selectedBookLearningSourceNameTextView
                .setText(
                        request.getDisplayName()
                );

        binding.selectedBookLearningSourceDetailsTextView
                .setText(
                        createSourceDetails(request)
                );

        binding.bookLearningImportProgressCard
                .setVisibility(View.GONE);

        binding.startBookLearningImportButton
                .setText(
                        "Continue to Chapter Review"
                );

        binding.startBookLearningImportButton
                .setEnabled(true);
    }

    private void showEmptySelection() {
        binding.selectedBookLearningSourceCard
                .setVisibility(View.GONE);

        binding.noBookLearningSourceTextView
                .setVisibility(View.VISIBLE);

        binding.bookLearningImportProgressCard
                .setVisibility(View.GONE);

        binding.startBookLearningImportButton
                .setEnabled(false);
    }

    private void prepareChapterDetection() {
        BookLearningImportRequest request =
                importRequest;

        if (request == null
                || extractingPages) {
            showMessage(
                    "पहले PDF या image book file चुनें।"
            );
            return;
        }

        request.markReadingDocument();
        extractingPages = true;

        binding.bookLearningImportProgressCard
                .setVisibility(View.VISIBLE);

        binding.bookLearningImportProgressTitleTextView
                .setText("Preparing book pages");

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        "PDF/image पढ़ी जा रही है…"
                );

        binding.bookLearningImportProgressIndicator
                .setIndeterminate(true);

        setSelectionControlsEnabled(false);

        pageExtractor.extractPages(
                request,
                new BookDocumentPageExtractor
                        .ExtractionCallback() {

                    @Override
                    public void onProgress(
                            int completedPages,
                            int totalPages
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        binding.bookLearningImportProgressIndicator
                                .setIndeterminate(false);

                        binding.bookLearningImportProgressIndicator
                                .setMax(
                                        Math.max(
                                                1,
                                                totalPages
                                        )
                                );

                        binding.bookLearningImportProgressIndicator
                                .setProgressCompat(
                                        completedPages,
                                        true
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(
                                        completedPages
                                                + " / "
                                                + totalPages
                                                + " pages prepared"
                                );
                    }

                    @Override
                    public void onSuccess(
                            @NonNull BookDocumentPageExtractor
                                    .ExtractionResult result
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        request.markDetectingChapters();
                        startBookPageOcr(
                                request,
                                result
                        );
                    }

                    @Override
                    public void onCancelled() {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        extractingPages = false;
                        setSelectionControlsEnabled(true);

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Page preparation cancelled"
                                );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        extractingPages = false;
                        setSelectionControlsEnabled(true);

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Book pages could not be prepared"
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(
                                        readableMessage(
                                                exception,
                                                "Please choose the book "
                                                        + "file again."
                                        )
                                );

                        showMessage(
                                readableMessage(
                                        exception,
                                        "Book page extraction failed."
                                )
                        );
                    }
                }
        );
    }

    private void startBookPageOcr(
            @NonNull BookLearningImportRequest request,
            @NonNull BookDocumentPageExtractor
                    .ExtractionResult extractionResult
    ) {
        latestOcrResult = null;
        latestBoundaryResult = null;

        binding.bookLearningImportProgressTitleTextView
                .setText(
                        "Reading book text"
                );

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        "OCR 0 / "
                                + extractionResult.getPageCount()
                                + " pages"
                );

        binding.bookLearningImportProgressIndicator
                .setIndeterminate(false);

        binding.bookLearningImportProgressIndicator
                .setMax(
                        Math.max(
                                1,
                                extractionResult.getPageCount()
                        )
                );

        binding.bookLearningImportProgressIndicator
                .setProgressCompat(
                        0,
                        false
                );

        pageOcrScanner.scanPages(
                extractionResult,
                new BookPageOcrScanner.ScanCallback() {

                    @Override
                    public void onProgress(
                            int completedPages,
                            int totalPages
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        binding.bookLearningImportProgressIndicator
                                .setMax(
                                        Math.max(
                                                1,
                                                totalPages
                                        )
                                );

                        binding.bookLearningImportProgressIndicator
                                .setProgressCompat(
                                        completedPages,
                                        true
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(
                                        "OCR "
                                                + completedPages
                                                + " / "
                                                + totalPages
                                                + " pages"
                                );
                    }

                    @Override
                    public void onSuccess(
                            @NonNull BookPageOcrScanner
                                    .BookOcrResult result
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        latestOcrResult = result;
                        detectChapterBoundaries(
                                result
                        );
                    }

                    @Override
                    public void onCancelled() {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        extractingPages = false;
                        setSelectionControlsEnabled(true);

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Book OCR cancelled"
                                );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        extractingPages = false;
                        request.markFailed(
                                readableMessage(
                                        exception,
                                        "Book OCR failed."
                                )
                        );

                        setSelectionControlsEnabled(true);

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Book text could not be read"
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(
                                        readableMessage(
                                                exception,
                                                "Please try another "
                                                        + "clear PDF/image."
                                        )
                                );

                        showMessage(
                                readableMessage(
                                        exception,
                                        "Book OCR failed."
                                )
                        );
                    }
                }
        );
    }

    private void detectChapterBoundaries(
            @NonNull BookPageOcrScanner.BookOcrResult
                    ocrResult
    ) {
        binding.bookLearningImportProgressTitleTextView
                .setText(
                        "Detecting chapter boundaries"
                );

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        "Chapter headings और page ranges "
                                + "पहचाने जा रहे हैं…"
                );

        BookChapterBoundaryDetector.DetectionResult
                detectionResult =
                BookChapterBoundaryDetector.detect(
                        ocrResult
                );

        latestBoundaryResult = detectionResult;
        extractingPages = false;
        setSelectionControlsEnabled(true);

        binding.startBookLearningImportButton
                .setEnabled(false);

        if (!detectionResult.isSuccessful()) {
            binding.bookLearningImportProgressTitleTextView
                    .setText(
                            "Using existing chapter ranges"
                    );

            binding.bookLearningImportProgressMessageTextView
                    .setText(
                            "OCR heading स्पष्ट नहीं है। "
                                    + "Parent द्वारा पहले से तय "
                                    + "chapters लोड किए जा रहे हैं…"
                    );

            loadExistingChapterBoundaryFallback(
                    ocrResult,
                    detectionResult.getErrorMessage()
            );
            return;
        }

        int detectedChapterCount =
                detectionResult
                        .getChapterCandidates()
                        .size();

        int carefulReviewCount = 0;

        for (BookChapterBoundaryDetector
                .ChapterBoundaryCandidate candidate
                : detectionResult
                .getChapterCandidates()) {

            if (candidate.requiresCarefulReview()) {
                carefulReviewCount++;
            }
        }

        binding.bookLearningImportProgressTitleTextView
                .setText(
                        "Chapter boundaries detected"
                );

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        detectedChapterCount
                                + " chapters मिले। "
                                + carefulReviewCount
                                + " chapters को careful Parent "
                                + "review चाहिए। अगले चरण में "
                                + "पूरी list edit/approve होगी।"
                );

        binding.startBookLearningImportButton
                .setText(
                        "Review Detected Chapters"
                );

        binding.startBookLearningImportButton
                .setEnabled(true);

        showMessage(
                detectedChapterCount
                        + " chapter boundaries मिलीं।"
        );

        openBoundaryReview();
    }

    private void loadExistingChapterBoundaryFallback(
            @NonNull BookPageOcrScanner.BookOcrResult
                    ocrResult,
            @NonNull String originalDetectionError
    ) {
        chapterRepository.getChaptersForBook(
                bookRowId,
                new SchoolBookChapterRepository
                        .ChaptersCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolBookChapterEntity>
                                    chapters
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        BookChapterBoundaryDetector
                                .DetectionResult fallbackResult =
                                BookChapterBoundaryDetector
                                        .fromExistingChapters(
                                                ocrResult.getRequestId(),
                                                bookRowId,
                                                ocrResult.getPageCount(),
                                                chapters
                                        );

                        if (!fallbackResult.isSuccessful()) {
                            showBoundaryFallbackFailure(
                                    originalDetectionError,
                                    fallbackResult
                                            .getErrorMessage()
                            );
                            return;
                        }

                        latestBoundaryResult =
                                fallbackResult;

                        showTrustedExistingBoundaries(
                                fallbackResult
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        showBoundaryFallbackFailure(
                                originalDetectionError,
                                readableMessage(
                                        exception,
                                        "Existing chapters "
                                                + "could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    private void showTrustedExistingBoundaries(
            @NonNull BookChapterBoundaryDetector
                    .DetectionResult detectionResult
    ) {
        int chapterCount =
                detectionResult
                        .getChapterCandidates()
                        .size();

        extractingPages = false;
        setSelectionControlsEnabled(true);

        binding.bookLearningImportProgressTitleTextView
                .setText(
                        "Existing chapter ranges loaded"
                );

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        chapterCount
                                + " Parent-defined chapters मिले। "
                                + "OCR heading की जगह इन्हीं trusted "
                                + "page ranges का उपयोग होगा।"
                );

        binding.startBookLearningImportButton
                .setText(
                        "Review Existing Chapters"
                );

        binding.startBookLearningImportButton
                .setEnabled(true);

        showMessage(
                chapterCount
                        + " trusted chapter ranges loaded."
        );

        openBoundaryReview();
    }

    private void showBoundaryFallbackFailure(
            @NonNull String originalError,
            @NonNull String fallbackError
    ) {
        latestBoundaryResult = null;
        extractingPages = false;
        setSelectionControlsEnabled(true);

        binding.bookLearningImportProgressTitleTextView
                .setText(
                        "Chapter ranges need correction"
                );

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        originalError
                                + "\n"
                                + fallbackError
                );

        binding.startBookLearningImportButton
                .setText(
                        "No Valid Chapter Ranges"
                );

        binding.startBookLearningImportButton
                .setEnabled(false);

        showMessage(
                "Existing chapter page ranges जाँचें।"
        );
    }

    private void openBoundaryReview() {
        BookChapterBoundaryDetector.DetectionResult
                detectionResult =
                latestBoundaryResult;

        if (detectionResult == null
                || !detectionResult.isSuccessful()) {
            showMessage(
                    "No detected chapter list is available."
            );
            return;
        }

        Intent intent =
                BookChapterBoundaryReviewActivity
                        .createIntent(
                                this,
                                detectionResult,
                                bookTitle
                        );

        try {
            boundaryReviewLauncher.launch(intent);
        } catch (RuntimeException exception) {
            showMessage(
                    readableMessage(
                            exception,
                            "Chapter review screen "
                                    + "could not be opened."
                    )
            );
        }
    }

    @SuppressWarnings("deprecation")
    private void handleBoundaryReviewResult(
            int resultCode,
            @Nullable Intent resultData
    ) {
        if (resultCode != Activity.RESULT_OK
                || resultData == null) {

            if (latestBoundaryResult != null
                    && latestBoundaryResult
                    .isSuccessful()) {
                binding.startBookLearningImportButton
                        .setText(
                                "Review Detected Chapters"
                        );
                binding.startBookLearningImportButton
                        .setEnabled(true);
            }

            return;
        }

        Serializable serializable =
                resultData.getSerializableExtra(
                        BookChapterBoundaryReviewActivity
                                .EXTRA_APPROVED_BOUNDARIES
                );

        if (!(serializable instanceof ArrayList<?>)) {
            showMessage(
                    "Approved chapter ranges could not be read."
            );
            return;
        }

        ArrayList<?> rawList =
                (ArrayList<?>) serializable;

        approvedBoundaries.clear();

        for (Object item : rawList) {
            if (item instanceof
                    BookChapterBoundaryReviewActivity
                            .ApprovedChapterBoundary) {

                approvedBoundaries.add(
                        (BookChapterBoundaryReviewActivity
                                .ApprovedChapterBoundary) item
                );
            }
        }

        if (approvedBoundaries.isEmpty()) {
            showMessage(
                    "At least one approved chapter is required."
            );
            return;
        }

        BookLearningImportRequest request =
                importRequest;

        if (request != null) {
            request.markExtractingContent();
        }

        importPageWiseChapterDrafts();
    }

    private void importPageWiseChapterDrafts() {
        BookPageOcrScanner.BookOcrResult ocrResult =
                latestOcrResult;

        if (ocrResult == null) {
            showMessage(
                    "Book OCR result is no longer available. "
                            + "Please run the import again."
            );
            return;
        }

        if (approvedBoundaries.isEmpty()) {
            showMessage(
                    "At least one approved chapter is required."
            );
            return;
        }

        ArrayList<BookChapterPageImportCoordinator
                .BoundaryInput> boundaryInputs =
                new ArrayList<>();

        try {
            for (BookChapterBoundaryReviewActivity
                    .ApprovedChapterBoundary boundary
                    : approvedBoundaries) {

                boundaryInputs.add(
                        new BookChapterPageImportCoordinator
                                .BoundaryInput(
                                boundary.getApprovedOrder(),
                                boundary.getApprovedTitle(),
                                boundary.getStartPage(),
                                boundary.getEndPage()
                        )
                );
            }
        } catch (IllegalArgumentException exception) {
            showMessage(
                    readableMessage(
                            exception,
                            "Approved chapter ranges are invalid."
                    )
            );
            return;
        }

        binding.bookLearningImportProgressCard
                .setVisibility(View.VISIBLE);

        binding.bookLearningImportProgressTitleTextView
                .setText(
                        "Saving page-wise chapter drafts"
                );

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        "हर PDF page को अलग Kinder reader page "
                                + "बनाया जा रहा है…"
                );

        binding.bookLearningImportProgressIndicator
                .setIndeterminate(true);

        setSelectionControlsEnabled(false);

        pageImportCoordinator.importPageDrafts(
                bookRowId,
                ocrResult,
                boundaryInputs,
                new BookChapterPageImportCoordinator
                        .ImportCallback() {

                    @Override
                    public void onProgress(
                            int completedChapters,
                            int totalChapters,
                            @NonNull String chapterTitle,
                            int savedPageCount
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        binding.bookLearningImportProgressIndicator
                                .setIndeterminate(false);

                        binding.bookLearningImportProgressIndicator
                                .setMax(
                                        Math.max(
                                                totalChapters,
                                                1
                                        )
                                );

                        binding.bookLearningImportProgressIndicator
                                .setProgressCompat(
                                        completedChapters,
                                        true
                                );

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Saving page-wise chapter drafts"
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(
                                        completedChapters
                                                + " / "
                                                + totalChapters
                                                + " chapters • "
                                                + chapterTitle
                                                + " • "
                                                + savedPageCount
                                                + " pages saved"
                                );
                    }

                    @Override
                    public void onSuccess(
                            @NonNull BookChapterPageImportCoordinator
                                    .ImportResult result
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        BookLearningImportRequest request =
                                importRequest;

                        if (request != null
                                && result.getImportedPageCount()
                                > 0) {
                            request.markPendingParentReview();
                        }

                        binding.bookLearningImportProgressIndicator
                                .setIndeterminate(false);

                        binding.bookLearningImportProgressIndicator
                                .setMax(1);

                        binding.bookLearningImportProgressIndicator
                                .setProgressCompat(
                                        1,
                                        false
                                );

                        setSelectionControlsEnabled(true);

                        binding.startBookLearningImportButton
                                .setEnabled(true);

                        binding.startBookLearningImportButton
                                .setText(
                                        "Step 3 • Review Saved Chapters"
                                );

                        draftsReadyForReview = true;

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Page-wise drafts ready"
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(
                                        result.getImportedChapterCount()
                                                + " chapters and "
                                                + result.getImportedPageCount()
                                                + " pages saved for Parent review. "
                                                + result.getProtectedChapterCount()
                                                + " chapters kept unchanged. "
                                                + result.getUnmatchedChapterCount()
                                                + " chapters could not be matched."
                                );

                        showMessage(
                                result.getImportedPageCount()
                                        + " page-wise drafts saved."
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        binding.bookLearningImportProgressIndicator
                                .setIndeterminate(false);

                        setSelectionControlsEnabled(true);

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Page-wise draft save failed"
                                );

                        String message =
                                readableMessage(
                                        exception,
                                        "Page-wise chapter drafts "
                                                + "could not be saved."
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(message);

                        showMessage(message);
                    }
                }
        );
    }

    private void extractApprovedChapterContents() {
        BookPageOcrScanner.BookOcrResult ocrResult =
                latestOcrResult;

        if (ocrResult == null) {
            showMessage(
                    "Book OCR result is no longer available. "
                            + "Please run the import again."
            );
            return;
        }

        extractedChapterContents.clear();

        try {
            for (BookChapterBoundaryReviewActivity
                    .ApprovedChapterBoundary boundary
                    : approvedBoundaries) {

                extractedChapterContents.add(
                        BookChapterSectionExtractor.extract(
                                ocrResult,
                                boundary.getApprovedOrder(),
                                boundary.getApprovedTitle(),
                                boundary.getStartPage(),
                                boundary.getEndPage()
                        )
                );
            }
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            extractedChapterContents.clear();

            showMessage(
                    readableMessage(
                            exception,
                            "Chapter content extraction failed."
                    )
            );
            return;
        }

        int chaptersWithExamples = 0;
        int chaptersWithExercises = 0;
        int carefulReviewCount = 0;

        for (BookChapterSectionExtractor
                .ExtractedChapterContent content
                : extractedChapterContents) {

            if (content.hasExamples()) {
                chaptersWithExamples++;
            }

            if (content.hasExercises()) {
                chaptersWithExercises++;
            }

            if (content.requiresCarefulReview()) {
                carefulReviewCount++;
            }
        }

        binding.bookLearningImportProgressTitleTextView
                .setText(
                        "Chapter content extracted"
                );

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        extractedChapterContents.size()
                                + " chapters तैयार हैं। "
                                + chaptersWithExamples
                                + " में examples, "
                                + chaptersWithExercises
                                + " में exercises मिलीं। "
                                + carefulReviewCount
                                + " chapters को careful content "
                                + "review चाहिए।"
                );

        binding.startBookLearningImportButton
                .setText(
                        "Content Extracted"
                );

        binding.startBookLearningImportButton
                .setEnabled(false);

        showMessage(
                extractedChapterContents.size()
                        + " chapters का content अलग हो गया।"
        );

        saveExtractedContentDrafts();
    }

    private void saveExtractedContentDrafts() {
        if (extractedChapterContents.isEmpty()) {
            showMessage(
                    "No extracted chapter content is available."
            );
            return;
        }

        binding.bookLearningImportProgressTitleTextView
                .setText(
                        "Saving Parent review drafts"
                );

        binding.bookLearningImportProgressMessageTextView
                .setText(
                        "Extracted content को सही existing "
                                + "chapters से safely match किया "
                                + "जा रहा है…"
                );

        binding.bookLearningImportProgressIndicator
                .setIndeterminate(true);

        setSelectionControlsEnabled(false);

        extractedContentSaveCoordinator.saveDrafts(
                bookRowId,
                extractedChapterContents,
                new BookExtractedContentSaveCoordinator
                        .SaveCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull BookExtractedContentSaveCoordinator
                                    .SaveResult result
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        binding.bookLearningImportProgressIndicator
                                .setIndeterminate(false);

                        binding.bookLearningImportProgressIndicator
                                .setMax(1);

                        binding.bookLearningImportProgressIndicator
                                .setProgressCompat(
                                        1,
                                        false
                                );

                        BookLearningImportRequest request =
                                importRequest;

                        if (request != null
                                && result.getSavedDraftCount() > 0) {
                            request.markPendingParentReview();
                        }

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Parent review drafts ready"
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(
                                        result.getSavedDraftCount()
                                                + " drafts saved, "
                                                + result
                                                .getExistingContentSkippedCount()
                                                + " existing content "
                                                + "protected, "
                                                + result
                                                .getUnmatchedChapterCount()
                                                + " unmatched chapters। "
                                                + "Chapter cards खोलकर "
                                                + "content review/approve करें।"
                                );

                        setSelectionControlsEnabled(true);

                        binding.startBookLearningImportButton
                                .setText(
                                        "Step 3 • Review Saved Chapters"
                                );

                        binding.startBookLearningImportButton
                                .setEnabled(true);

                        draftsReadyForReview = true;

                        showMessage(
                                result.getSavedDraftCount()
                                        + " chapter drafts safely saved."
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        binding.bookLearningImportProgressIndicator
                                .setIndeterminate(false);

                        setSelectionControlsEnabled(true);

                        binding.bookLearningImportProgressTitleTextView
                                .setText(
                                        "Drafts could not be saved"
                                );

                        binding.bookLearningImportProgressMessageTextView
                                .setText(
                                        readableMessage(
                                                exception,
                                                "Please review the exact "
                                                        + "chapter list "
                                                        + "and try again."
                                        )
                                );

                        showMessage(
                                readableMessage(
                                        exception,
                                        "Chapter drafts could not be saved."
                                )
                        );
                    }
                }
        );
    }

    private void openSavedChaptersForReview() {
        Intent reviewIntent =
                SchoolBookChaptersActivity.createIntent(
                        this,
                        bookRowId,
                        bookTitle
                );

        try {
            startActivity(reviewIntent);
            setResult(Activity.RESULT_OK);
            finish();
        } catch (RuntimeException exception) {
            showMessage(
                    readableMessage(
                            exception,
                            "Saved chapters अभी नहीं खुल सके।"
                    )
            );
        }
    }

    private boolean canUpdateScreen() {
        return binding != null
                && !isFinishing()
                && !isDestroyed();
    }

    private void setSelectionControlsEnabled(
            boolean enabled
    ) {
        binding.chooseBookLearningSourceButton
                .setEnabled(enabled);

        binding.cancelBookLearningImportButton
                .setEnabled(
                        enabled
                                || extractingPages
                );

        binding.startBookLearningImportButton
                .setEnabled(
                        enabled
                                && importRequest != null
                );
    }

    @NonNull
    private static String createSourceDetails(
            @NonNull BookLearningImportRequest request
    ) {
        String sourceLabel =
                request.isPdf()
                        ? "PDF"
                        : "Image";

        return sourceLabel
                + " • "
                + formatFileSize(
                request.getSourceSizeBytes()
        );
    }

    @NonNull
    private static String formatFileSize(
            long sizeBytes
    ) {
        if (sizeBytes <= 0L) {
            return "Size unavailable";
        }

        if (sizeBytes < 1024L) {
            return sizeBytes + " B";
        }

        double sizeKb =
                sizeBytes / 1024.0;

        if (sizeKb < 1024.0) {
            return String.format(
                    Locale.getDefault(),
                    "%.1f KB",
                    sizeKb
            );
        }

        double sizeMb =
                sizeKb / 1024.0;

        return String.format(
                Locale.getDefault(),
                "%.1f MB",
                sizeMb
        );
    }

    @NonNull
    private static String readableMessage(
            @NonNull Exception exception,
            @NonNull String fallback
    ) {
        String message =
                safeText(
                        exception.getMessage()
                );

        return message.isEmpty()
                ? fallback
                : message;
    }

    private void showMessage(
            @NonNull String message
    ) {
        if (binding == null) {
            return;
        }

        Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
}
