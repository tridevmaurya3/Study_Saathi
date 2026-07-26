package com.tridev.studysaathi.ui.adapter;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.data.content.parser
        .BookChapterBoundaryDetector;
import com.tridev.studysaathi.databinding
        .ItemBookChapterBoundaryCandidateBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BookChapterBoundaryReviewAdapter
        extends RecyclerView.Adapter<
        BookChapterBoundaryReviewAdapter.BoundaryViewHolder> {

    @NonNull
    private final ArrayList<ReviewInput> reviewInputs =
            new ArrayList<>();

    @NonNull
    private final ChangeListener changeListener;

    public BookChapterBoundaryReviewAdapter(
            @NonNull ChangeListener changeListener
    ) {
        this.changeListener = changeListener;
        setHasStableIds(true);
    }

    public void submitCandidates(
            @NonNull List<BookChapterBoundaryDetector
                    .ChapterBoundaryCandidate> candidates
    ) {
        reviewInputs.clear();

        for (BookChapterBoundaryDetector
                .ChapterBoundaryCandidate candidate
                : candidates) {

            reviewInputs.add(
                    ReviewInput.from(candidate)
            );
        }

        notifyDataSetChanged();
        changeListener.onReviewChanged(
                !reviewInputs.isEmpty()
        );
    }

    @NonNull
    public ReviewValidationResult validateAndCollect(
            int maximumPageNumber
    ) {
        ArrayList<ReviewInput> included =
                new ArrayList<>();

        boolean valid = true;
        int previousEndPage = 0;

        for (ReviewInput input : reviewInputs) {
            input.validationError = "";

            if (!input.included) {
                continue;
            }

            included.add(input);

            if (input.title.trim().isEmpty()) {
                input.validationError =
                        "Chapter title is required.";
                valid = false;
                continue;
            }

            if (input.startPage <= 0) {
                input.validationError =
                        "Enter a valid start page.";
                valid = false;
                continue;
            }

            if (input.endPage < input.startPage) {
                input.validationError =
                        "End page cannot be before start page.";
                valid = false;
                continue;
            }

            if (maximumPageNumber > 0
                    && input.endPage > maximumPageNumber) {
                input.validationError =
                        "End page exceeds the book page count.";
                valid = false;
                continue;
            }

            if (previousEndPage > 0
                    && input.startPage <= previousEndPage) {
                input.validationError =
                        "This chapter overlaps the previous chapter.";
                valid = false;
                continue;
            }

            previousEndPage =
                    input.endPage;
        }

        if (included.isEmpty()) {
            valid = false;
        }

        notifyDataSetChanged();

        return new ReviewValidationResult(
                valid,
                included.isEmpty()
                        ? "Select at least one chapter."
                        : valid
                          ? ""
                          : "Correct the highlighted chapter details.",
                included
        );
    }

    public int getIncludedCount() {
        int count = 0;

        for (ReviewInput input : reviewInputs) {
            if (input.included) {
                count++;
            }
        }

        return count;
    }

    @Override
    public long getItemId(int position) {
        return reviewInputs.get(position)
                .detectedOrder;
    }

    @NonNull
    @Override
    public BoundaryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemBookChapterBoundaryCandidateBinding binding =
                ItemBookChapterBoundaryCandidateBinding
                        .inflate(
                                LayoutInflater.from(
                                        parent.getContext()
                                ),
                                parent,
                                false
                        );

        return new BoundaryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BoundaryViewHolder holder,
            int position
    ) {
        holder.bind(
                reviewInputs.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return reviewInputs.size();
    }

    public interface ChangeListener {

        void onReviewChanged(
                boolean hasCandidates
        );
    }

    public static final class ReviewInput {

        private final int detectedOrder;

        @NonNull
        private final String detectedHeading;

        private final float confidence;

        @NonNull
        private final String detectionReason;

        private boolean included;

        @NonNull
        private String title;

        private int startPage;
        private int endPage;

        @NonNull
        private String validationError;

        private ReviewInput(
                int detectedOrder,
                @NonNull String detectedHeading,
                float confidence,
                @NonNull String detectionReason,
                boolean included,
                @NonNull String title,
                int startPage,
                int endPage
        ) {
            this.detectedOrder = detectedOrder;
            this.detectedHeading = detectedHeading;
            this.confidence = confidence;
            this.detectionReason = detectionReason;
            this.included = included;
            this.title = title;
            this.startPage = startPage;
            this.endPage = endPage;
            this.validationError = "";
        }

        @NonNull
        private static ReviewInput from(
                @NonNull BookChapterBoundaryDetector
                        .ChapterBoundaryCandidate candidate
        ) {
            return new ReviewInput(
                    candidate.getDetectedOrder(),
                    candidate.getDetectedHeading(),
                    candidate.getConfidence(),
                    candidate.getDetectionReason(),
                    true,
                    candidate.getSuggestedTitle(),
                    candidate.getStartPage(),
                    candidate.getEndPage()
            );
        }

        public int getDetectedOrder() {
            return detectedOrder;
        }

        @NonNull
        public String getDetectedHeading() {
            return detectedHeading;
        }

        public float getConfidence() {
            return confidence;
        }

        @NonNull
        public String getDetectionReason() {
            return detectionReason;
        }

        public boolean isIncluded() {
            return included;
        }

        @NonNull
        public String getTitle() {
            return title.trim();
        }

        public int getStartPage() {
            return startPage;
        }

        public int getEndPage() {
            return endPage;
        }
    }

    public static final class ReviewValidationResult {

        private final boolean valid;

        @NonNull
        private final String message;

        @NonNull
        private final List<ReviewInput> includedInputs;

        private ReviewValidationResult(
                boolean valid,
                @NonNull String message,
                @NonNull List<ReviewInput> includedInputs
        ) {
            this.valid = valid;
            this.message = message;
            this.includedInputs =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    includedInputs
                            )
                    );
        }

        public boolean isValid() {
            return valid;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @NonNull
        public List<ReviewInput> getIncludedInputs() {
            return includedInputs;
        }
    }

    final class BoundaryViewHolder
            extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemBookChapterBoundaryCandidateBinding
                binding;

        private TextWatcher titleWatcher;
        private TextWatcher startPageWatcher;
        private TextWatcher endPageWatcher;

        private BoundaryViewHolder(
                @NonNull ItemBookChapterBoundaryCandidateBinding
                        binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(
                @NonNull ReviewInput input
        ) {
            removeWatchers();

            binding.includeChapterBoundaryCheckBox
                    .setOnCheckedChangeListener(null);

            binding.chapterBoundaryOrderTextView
                    .setText(
                            "Detected chapter "
                                    + input.detectedOrder
                    );

            binding.detectedChapterHeadingTextView
                    .setText(
                            "OCR: "
                                    + input.detectedHeading
                    );

            binding.includeChapterBoundaryCheckBox
                    .setChecked(
                            input.included
                    );

            binding.chapterBoundaryTitleEditText
                    .setText(input.title);

            binding.chapterBoundaryStartPageEditText
                    .setText(
                            String.valueOf(
                                    input.startPage
                            )
                    );

            binding.chapterBoundaryEndPageEditText
                    .setText(
                            String.valueOf(
                                    input.endPage
                            )
                    );

            int confidencePercent =
                    Math.round(
                            input.confidence * 100f
                    );

            binding.chapterBoundaryConfidenceTextView
                    .setText(
                            String.format(
                                    Locale.getDefault(),
                                    "Confidence: %d%%",
                                    confidencePercent
                            )
                    );

            binding.chapterBoundaryConfidenceTextView
                    .setTextColor(
                            input.confidence < 0.80f
                                    ? Color.parseColor(
                                    "#B54708"
                            )
                                    : Color.parseColor(
                                    "#157347"
                            )
                    );

            binding.chapterBoundaryReasonTextView
                    .setText(
                            input.detectionReason
                    );

            showValidationError(
                    input.validationError
            );

            setEditorEnabled(
                    input.included
            );

            binding.includeChapterBoundaryCheckBox
                    .setOnCheckedChangeListener(
                            (button, checked) -> {
                                input.included = checked;
                                input.validationError = "";
                                setEditorEnabled(checked);
                                showValidationError("");
                                notifyReviewChanged();
                            }
                    );

            titleWatcher =
                    new SimpleTextWatcher() {
                        @Override
                        public void afterTextChanged(
                                Editable editable
                        ) {
                            input.title =
                                    editable == null
                                            ? ""
                                            : editable.toString();
                            input.validationError = "";
                            showValidationError("");
                            notifyReviewChanged();
                        }
                    };

            startPageWatcher =
                    new SimpleTextWatcher() {
                        @Override
                        public void afterTextChanged(
                                Editable editable
                        ) {
                            input.startPage =
                                    parsePageNumber(
                                            editable
                                    );
                            input.validationError = "";
                            showValidationError("");
                            notifyReviewChanged();
                        }
                    };

            endPageWatcher =
                    new SimpleTextWatcher() {
                        @Override
                        public void afterTextChanged(
                                Editable editable
                        ) {
                            input.endPage =
                                    parsePageNumber(
                                            editable
                                    );
                            input.validationError = "";
                            showValidationError("");
                            notifyReviewChanged();
                        }
                    };

            binding.chapterBoundaryTitleEditText
                    .addTextChangedListener(
                            titleWatcher
                    );

            binding.chapterBoundaryStartPageEditText
                    .addTextChangedListener(
                            startPageWatcher
                    );

            binding.chapterBoundaryEndPageEditText
                    .addTextChangedListener(
                            endPageWatcher
                    );
        }

        private void setEditorEnabled(
                boolean enabled
        ) {
            binding.chapterBoundaryTitleInputLayout
                    .setEnabled(enabled);

            binding.chapterBoundaryStartPageInputLayout
                    .setEnabled(enabled);

            binding.chapterBoundaryEndPageInputLayout
                    .setEnabled(enabled);

            binding.getRoot().setAlpha(
                    enabled
                            ? 1.0f
                            : 0.60f
            );
        }

        private void showValidationError(
                @NonNull String message
        ) {
            binding.chapterBoundaryItemErrorTextView
                    .setText(message);

            binding.chapterBoundaryItemErrorTextView
                    .setVisibility(
                            message.isEmpty()
                                    ? View.GONE
                                    : View.VISIBLE
                    );
        }

        private void removeWatchers() {
            if (titleWatcher != null) {
                binding.chapterBoundaryTitleEditText
                        .removeTextChangedListener(
                                titleWatcher
                        );
            }

            if (startPageWatcher != null) {
                binding.chapterBoundaryStartPageEditText
                        .removeTextChangedListener(
                                startPageWatcher
                        );
            }

            if (endPageWatcher != null) {
                binding.chapterBoundaryEndPageEditText
                        .removeTextChangedListener(
                                endPageWatcher
                        );
            }
        }
    }

    private void notifyReviewChanged() {
        changeListener.onReviewChanged(
                getIncludedCount() > 0
        );
    }

    private static int parsePageNumber(
            Editable editable
    ) {
        if (editable == null) {
            return 0;
        }

        String value =
                editable.toString().trim();

        if (value.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private abstract static class SimpleTextWatcher
            implements TextWatcher {

        @Override
        public void beforeTextChanged(
                CharSequence sequence,
                int start,
                int count,
                int after
        ) {
        }

        @Override
        public void onTextChanged(
                CharSequence sequence,
                int start,
                int before,
                int count
        ) {
        }
    }
}
