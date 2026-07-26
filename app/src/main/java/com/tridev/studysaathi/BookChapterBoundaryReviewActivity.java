package com.tridev.studysaathi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.content.parser
        .BookChapterBoundaryDetector;
import com.tridev.studysaathi.databinding
        .ActivityBookChapterBoundaryReviewBinding;
import com.tridev.studysaathi.ui.adapter
        .BookChapterBoundaryReviewAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class BookChapterBoundaryReviewActivity
        extends AppCompatActivity {

    public static final String EXTRA_DETECTION_RESULT =
            "extra_detection_result";

    public static final String EXTRA_BOOK_TITLE =
            "extra_book_title";

    public static final String EXTRA_APPROVED_BOUNDARIES =
            "extra_approved_boundaries";

    private ActivityBookChapterBoundaryReviewBinding binding;

    private BookChapterBoundaryReviewAdapter adapter;

    private BookChapterBoundaryDetector.DetectionResult
            detectionResult;

    @NonNull
    private String bookTitle = "";

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            @NonNull BookChapterBoundaryDetector
                    .DetectionResult detectionResult,
            @Nullable String bookTitle
    ) {
        Intent intent =
                new Intent(
                        context,
                        BookChapterBoundaryReviewActivity.class
                );

        intent.putExtra(
                EXTRA_DETECTION_RESULT,
                detectionResult
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
                ActivityBookChapterBoundaryReviewBinding
                        .inflate(
                                getLayoutInflater()
                        );

        setContentView(
                binding.getRoot()
        );

        readArguments();

        if (detectionResult == null
                || !detectionResult.isSuccessful()) {
            showMessage(
                    "Detected chapter details are missing."
            );
            finish();
            return;
        }

        setupToolbar();
        setupList();
        setupButtons();
        showDetectionSummary();
    }

    @SuppressWarnings("deprecation")
    private void readArguments() {
        Serializable serializable =
                getIntent().getSerializableExtra(
                        EXTRA_DETECTION_RESULT
                );

        if (serializable
                instanceof BookChapterBoundaryDetector
                .DetectionResult) {

            detectionResult =
                    (BookChapterBoundaryDetector
                            .DetectionResult) serializable;
        }

        bookTitle =
                safeText(
                        getIntent().getStringExtra(
                                EXTRA_BOOK_TITLE
                        )
                );
    }

    private void setupToolbar() {
        binding.chapterBoundaryReviewToolbar
                .setNavigationOnClickListener(
                        ignored -> finish()
                );

        if (!bookTitle.isEmpty()) {
            binding.chapterBoundaryReviewToolbar
                    .setSubtitle(bookTitle);
        }
    }

    private void setupList() {
        adapter =
                new BookChapterBoundaryReviewAdapter(
                        hasCandidates ->
                                binding
                                        .approveChapterBoundariesButton
                                        .setEnabled(
                                                hasCandidates
                                        )
                );

        binding.chapterBoundaryReviewRecyclerView
                .setLayoutManager(
                        new LinearLayoutManager(this)
                );

        binding.chapterBoundaryReviewRecyclerView
                .setHasFixedSize(false);

        binding.chapterBoundaryReviewRecyclerView
                .setAdapter(adapter);

        adapter.submitCandidates(
                detectionResult
                        .getChapterCandidates()
        );
    }

    private void setupButtons() {
        binding.approveChapterBoundariesButton
                .setOnClickListener(
                        ignored ->
                                approveBoundaries()
                );

        binding.cancelChapterBoundaryReviewButton
                .setOnClickListener(
                        ignored -> finish()
                );
    }

    private void showDetectionSummary() {
        int totalCandidates =
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

        binding.chapterBoundaryReviewSummaryTextView
                .setText(
                        totalCandidates
                                + " chapters detected"
                );

        binding.chapterBoundaryReviewBookTitleTextView
                .setText(
                        bookTitle.isEmpty()
                                ? "Exact school book"
                                : bookTitle
                );

        binding.chapterBoundaryReviewWarningTextView
                .setText(
                        "हर chapter का title, start page और "
                                + "end page जाँचें। "
                                + carefulReviewCount
                                + " low-confidence chapters को "
                                + "विशेष रूप से सुधारें।"
                );
    }

    private void approveBoundaries() {
        BookChapterBoundaryReviewAdapter
                .ReviewValidationResult validationResult =
                adapter.validateAndCollect(
                        detectionResult
                                .getScannedPageCount()
                );

        if (!validationResult.isValid()) {
            showMessage(
                    validationResult.getMessage()
            );
            return;
        }

        showLoading(true);

        ArrayList<ApprovedChapterBoundary>
                approvedBoundaries =
                new ArrayList<>();

        List<BookChapterBoundaryReviewAdapter
                .ReviewInput> includedInputs =
                validationResult.getIncludedInputs();

        for (int index = 0;
             index < includedInputs.size();
             index++) {

            BookChapterBoundaryReviewAdapter
                    .ReviewInput input =
                    includedInputs.get(index);

            approvedBoundaries.add(
                    new ApprovedChapterBoundary(
                            index + 1,
                            input.getDetectedOrder(),
                            input.getDetectedHeading(),
                            input.getTitle(),
                            input.getStartPage(),
                            input.getEndPage(),
                            input.getConfidence(),
                            input.getDetectionReason()
                    )
            );
        }

        Intent resultIntent =
                new Intent();

        resultIntent.putExtra(
                EXTRA_APPROVED_BOUNDARIES,
                approvedBoundaries
        );

        setResult(
                Activity.RESULT_OK,
                resultIntent
        );

        finish();
    }

    private void showLoading(
            boolean loading
    ) {
        binding.chapterBoundaryReviewLoadingContainer
                .setVisibility(
                        loading
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.chapterBoundaryReviewRecyclerView
                .setVisibility(
                        loading
                                ? View.GONE
                                : View.VISIBLE
                );

        binding.chapterBoundaryReviewActionsContainer
                .setVisibility(
                        loading
                                ? View.GONE
                                : View.VISIBLE
                );
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

    public static final class ApprovedChapterBoundary
            implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int approvedOrder;
        private final int detectedOrder;

        @NonNull
        private final String detectedHeading;

        @NonNull
        private final String approvedTitle;

        private final int startPage;
        private final int endPage;
        private final float originalConfidence;

        @NonNull
        private final String detectionReason;

        private ApprovedChapterBoundary(
                int approvedOrder,
                int detectedOrder,
                @NonNull String detectedHeading,
                @NonNull String approvedTitle,
                int startPage,
                int endPage,
                float originalConfidence,
                @NonNull String detectionReason
        ) {
            this.approvedOrder = approvedOrder;
            this.detectedOrder = detectedOrder;
            this.detectedHeading = detectedHeading;
            this.approvedTitle = approvedTitle;
            this.startPage = startPage;
            this.endPage = endPage;
            this.originalConfidence =
                    originalConfidence;
            this.detectionReason =
                    detectionReason;
        }

        public int getApprovedOrder() {
            return approvedOrder;
        }

        public int getDetectedOrder() {
            return detectedOrder;
        }

        @NonNull
        public String getDetectedHeading() {
            return detectedHeading;
        }

        @NonNull
        public String getApprovedTitle() {
            return approvedTitle;
        }

        public int getStartPage() {
            return startPage;
        }

        public int getEndPage() {
            return endPage;
        }

        public float getOriginalConfidence() {
            return originalConfidence;
        }

        @NonNull
        public String getDetectionReason() {
            return detectionReason;
        }
    }
}
