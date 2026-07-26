package com.tridev.studysaathi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tridev.studysaathi.data.content.model
        .BookContentsScanResult;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.data.repository
        .BookContentsChapterImportCoordinator;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterRepository;
import com.tridev.studysaathi.databinding
        .ActivityBookContentsReviewBinding;
import com.tridev.studysaathi.ui.adapter
        .BookContentsCandidateAdapter;
import com.tridev.studysaathi.ui.mapper
        .BookContentsCandidateEntityMapper;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public final class BookContentsReviewActivity
        extends AppCompatActivity {

    public static final String EXTRA_SCAN_RESULT =
            "extra_scan_result";

    public static final String EXTRA_IMPORTED_COUNT =
            "extra_imported_count";

    public static final String EXTRA_SKIPPED_COUNT =
            "extra_skipped_count";

    public static final String EXTRA_CHAPTERS_CHANGED =
            "extra_chapters_changed";

    private ActivityBookContentsReviewBinding binding;

    private BookContentsCandidateAdapter candidateAdapter;

    private SchoolBookChapterRepository chapterRepository;

    private BookContentsChapterImportCoordinator importCoordinator;

    @Nullable
    private BookContentsScanResult scanResult;

    private boolean importing;

    /**
     * Review Activity खोलने के लिए सुरक्षित Intent बनाता है।
     */
    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            @NonNull BookContentsScanResult scanResult
    ) {
        Intent intent =
                new Intent(
                        context,
                        BookContentsReviewActivity.class
                );

        intent.putExtra(
                EXTRA_SCAN_RESULT,
                scanResult
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
                ActivityBookContentsReviewBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        candidateAdapter =
                new BookContentsCandidateAdapter();

        chapterRepository =
                new SchoolBookChapterRepository(
                        getApplicationContext()
                );

        importCoordinator =
                new BookContentsChapterImportCoordinator(
                        getApplicationContext()
                );

        configureRecyclerView();
        configureButtons();

        scanResult =
                readScanResult();

        bindScanResult();
    }

    private void configureRecyclerView() {
        binding.chapterCandidatesRecyclerView
                .setLayoutManager(
                        new LinearLayoutManager(
                                this
                        )
                );

        binding.chapterCandidatesRecyclerView
                .setHasFixedSize(
                        false
                );

        binding.chapterCandidatesRecyclerView
                .setAdapter(
                        candidateAdapter
                );
    }

    private void configureButtons() {
        binding.backReviewButton
                .setOnClickListener(view ->
                        cancelAndClose()
                );

        binding.cancelReviewButton
                .setOnClickListener(view ->
                        cancelAndClose()
                );

        binding.selectAllCandidatesButton
                .setOnClickListener(view -> {
                    candidateAdapter.selectAll();

                    hideError();

                    showToast(
                            "All detected chapters selected."
                    );
                });

        binding.clearCandidateSelectionButton
                .setOnClickListener(view -> {
                    candidateAdapter.clearSelection();

                    hideError();

                    showToast(
                            "Chapter selection cleared."
                    );
                });

        binding.toggleRawOcrTextButton
                .setOnClickListener(view ->
                        toggleRawOcrText()
                );

        binding.importSelectedChaptersButton
                .setOnClickListener(view ->
                        importSelectedChapters()
                );
    }

    @Nullable
    private BookContentsScanResult readScanResult() {
        Intent intent =
                getIntent();

        if (intent == null) {
            return null;
        }

        Serializable serializableResult =
                intent.getSerializableExtra(
                        EXTRA_SCAN_RESULT
                );

        if (!(serializableResult
                instanceof BookContentsScanResult)) {

            return null;
        }

        return (BookContentsScanResult) serializableResult;
    }

    private void bindScanResult() {
        BookContentsScanResult result =
                scanResult;

        if (result == null) {
            showFatalError(
                    "The contents scan result is missing."
            );

            return;
        }

        if (!result.isSuccessful()) {
            showFatalError(
                    result.getErrorMessage()
            );

            return;
        }

        if (result.getBookRowId()
                <= 0L) {

            showFatalError(
                    "The contents scan is not linked "
                            + "to a valid school book."
            );

            return;
        }

        binding.rawOcrTextView
                .setText(
                        result.getDetectedFullText()
                );

        List<BookContentsScanResult.ChapterCandidate> candidates =
                result.getChapterCandidates();

        candidateAdapter.submitCandidates(
                candidates
        );

        updateCandidateSummary(
                candidates.size()
        );

        if (candidates.isEmpty()) {
            showNoCandidatesState();

        } else {
            showCandidateListState();
        }
    }

    private void updateCandidateSummary(
            int candidateCount
    ) {
        String summary =
                candidateCount
                        + (candidateCount == 1
                        ? " chapter candidate detected"
                        : " chapter candidates detected");

        binding.detectedCandidateSummaryTextView
                .setText(
                        summary
                );
    }

    private void showCandidateListState() {
        binding.reviewLoadingContainer
                .setVisibility(
                        View.GONE
                );

        binding.noCandidatesContainer
                .setVisibility(
                        View.GONE
                );

        binding.chapterCandidatesRecyclerView
                .setVisibility(
                        View.VISIBLE
                );

        setActionsEnabled(
                true
        );
    }

    private void showNoCandidatesState() {
        binding.reviewLoadingContainer
                .setVisibility(
                        View.GONE
                );

        binding.chapterCandidatesRecyclerView
                .setVisibility(
                        View.GONE
                );

        binding.noCandidatesContainer
                .setVisibility(
                        View.VISIBLE
                );

        binding.importSelectedChaptersButton
                .setEnabled(
                        false
                );

        binding.selectAllCandidatesButton
                .setEnabled(
                        false
                );

        binding.clearCandidateSelectionButton
                .setEnabled(
                        false
                );
    }

    private void importSelectedChapters() {
        if (importing) {
            return;
        }

        BookContentsScanResult result =
                scanResult;

        if (result == null
                || result.getBookRowId() <= 0L) {

            showError(
                    "A valid contents scan is required."
            );

            return;
        }

        List<BookContentsCandidateAdapter.CandidateInput> selectedInputs =
                candidateAdapter.getSelectedCandidateInputs();

        if (selectedInputs.isEmpty()) {
            showError(
                    "Select at least one chapter to import."
            );

            return;
        }

        for (int index = 0;
             index < selectedInputs.size();
             index++) {

            BookContentsCandidateAdapter.CandidateInput input =
                    selectedInputs.get(
                            index
                    );

            if (!input.hasValidTitle()) {
                showError(
                        "Selected chapter "
                                + (index + 1)
                                + " requires a title."
                );

                return;
            }
        }

        setImporting(
                true
        );

        chapterRepository.getNextSortOrder(
                result.getBookRowId(),
                new SchoolBookChapterRepository
                        .SortOrderCallback() {

                    @Override
                    public void onSuccess(
                            int nextSortOrder
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        prepareAndImportEntities(
                                result,
                                selectedInputs,
                                nextSortOrder
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        setImporting(
                                false
                        );

                        showError(
                                getErrorMessage(
                                        exception,
                                        "The chapter order "
                                                + "could not be prepared."
                                )
                        );
                    }
                }
        );
    }

    private void prepareAndImportEntities(
            @NonNull BookContentsScanResult result,
            @NonNull List<BookContentsCandidateAdapter
                    .CandidateInput> selectedInputs,
            int nextSortOrder
    ) {
        try {
            List<SchoolBookChapterEntity> chapters =
                    BookContentsCandidateEntityMapper
                            .toPendingReviewEntities(
                                    result.getBookRowId(),
                                    selectedInputs,
                                    nextSortOrder,
                                    result.getSourceImagePath()
                            );

            if (chapters.isEmpty()) {
                setImporting(
                        false
                );

                showError(
                        "No valid chapters are available to import."
                );

                return;
            }

            executeImport(
                    result.getBookRowId(),
                    chapters
            );

        } catch (Exception exception) {
            setImporting(
                    false
            );

            showError(
                    getErrorMessage(
                            exception,
                            "The selected chapters "
                                    + "could not be prepared."
                    )
            );
        }
    }

    private void executeImport(
            long bookRowId,
            @NonNull List<SchoolBookChapterEntity> chapters
    ) {
        importCoordinator.importPendingReviewChapters(
                bookRowId,
                chapters,
                new BookContentsChapterImportCoordinator
                        .ImportCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull BookContentsChapterImportCoordinator
                                    .ImportResult result
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        setImporting(
                                false
                        );

                        if (!result.hasInsertedChapters()) {
                            showError(
                                    result.hasSkippedChapters()
                                            ? "All selected chapters "
                                              + "already exist. Edit their "
                                              + "numbers or titles before retrying."
                                            : "No chapter was imported."
                            );

                            return;
                        }

                        showImportSuccess(
                                result
                        );

                        finishWithSuccess(
                                result
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        setImporting(
                                false
                        );

                        showError(
                                getErrorMessage(
                                        exception,
                                        "The selected chapters "
                                                + "could not be imported."
                                )
                        );
                    }
                }
        );
    }

    private void showImportSuccess(
            @NonNull BookContentsChapterImportCoordinator
                    .ImportResult result
    ) {
        String message =
                result.getInsertedCount()
                        + (result.getInsertedCount() == 1
                        ? " chapter imported"
                        : " chapters imported");

        if (result.getSkippedCount() > 0) {
            message =
                    message
                            + "; "
                            + result.getSkippedCount()
                            + (result.getSkippedCount() == 1
                            ? " duplicate skipped."
                            : " duplicates skipped.");

        } else {
            message =
                    message
                            + ".";
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void finishWithSuccess(
            @NonNull BookContentsChapterImportCoordinator
                    .ImportResult result
    ) {
        Intent resultIntent =
                new Intent();

        resultIntent.putExtra(
                EXTRA_IMPORTED_COUNT,
                result.getInsertedCount()
        );

        resultIntent.putExtra(
                EXTRA_SKIPPED_COUNT,
                result.getSkippedCount()
        );

        resultIntent.putExtra(
                EXTRA_CHAPTERS_CHANGED,
                result.hasInsertedChapters()
        );

        setResult(
                Activity.RESULT_OK,
                resultIntent
        );

        finish();
    }

    private void toggleRawOcrText() {
        boolean currentlyVisible =
                binding.rawOcrTextScrollView
                        .getVisibility()
                        == View.VISIBLE;

        binding.rawOcrTextScrollView
                .setVisibility(
                        currentlyVisible
                                ? View.GONE
                                : View.VISIBLE
                );

        binding.toggleRawOcrTextButton
                .setText(
                        currentlyVisible
                                ? "Show Raw OCR Text"
                                : "Hide Raw OCR Text"
                );
    }

    private void setImporting(
            boolean importing
    ) {
        this.importing =
                importing;

        binding.reviewLoadingContainer
                .setVisibility(
                        importing
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.chapterCandidatesRecyclerView
                .setVisibility(
                        importing
                                ? View.GONE
                                : candidateAdapter.hasCandidates()
                                  ? View.VISIBLE
                                  : View.GONE
                );

        setActionsEnabled(
                !importing
        );

        if (importing) {
            hideError();

            binding.reviewLoadingMessageTextView
                    .setText(
                            "Importing reviewed chapters…"
                    );
        }
    }

    private void setActionsEnabled(
            boolean enabled
    ) {
        boolean actionsAvailable =
                enabled
                        && scanResult != null
                        && candidateAdapter.hasCandidates();

        binding.selectAllCandidatesButton
                .setEnabled(
                        actionsAvailable
                );

        binding.clearCandidateSelectionButton
                .setEnabled(
                        actionsAvailable
                );

        binding.importSelectedChaptersButton
                .setEnabled(
                        actionsAvailable
                );

        binding.cancelReviewButton
                .setEnabled(
                        !importing
                );

        binding.backReviewButton
                .setEnabled(
                        !importing
                );
    }

    private void showFatalError(
            @NonNull String errorMessage
    ) {
        candidateAdapter.clearCandidates();

        updateCandidateSummary(
                0
        );

        showNoCandidatesState();

        binding.noCandidatesMessageTextView
                .setText(
                        errorMessage
                );

        showError(
                errorMessage
        );
    }

    private void showError(
            @NonNull String errorMessage
    ) {
        binding.reviewErrorTextView
                .setText(
                        errorMessage
                );

        binding.reviewErrorTextView
                .setVisibility(
                        View.VISIBLE
                );
    }

    private void hideError() {
        binding.reviewErrorTextView
                .setText(
                        ""
                );

        binding.reviewErrorTextView
                .setVisibility(
                        View.GONE
                );
    }

    private void showToast(
            @NonNull String message
    ) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void cancelAndClose() {
        if (importing) {
            return;
        }

        setResult(
                Activity.RESULT_CANCELED
        );

        finish();
    }

    private boolean canUpdateScreen() {
        return !isFinishing()
                && !isDestroyed();
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
}