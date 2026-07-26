package com.tridev.studysaathi.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.data.content.model
        .BookContentsScanResult;
import com.tridev.studysaathi.databinding
        .ItemBookContentsChapterCandidateBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BookContentsCandidateAdapter
        extends RecyclerView.Adapter<
        BookContentsCandidateAdapter.CandidateViewHolder> {

    private static final float LOW_CONFIDENCE_THRESHOLD =
            0.60F;

    private static final float MEDIUM_CONFIDENCE_THRESHOLD =
            0.80F;

    @NonNull
    private final List<EditableCandidate> editableCandidates =
            new ArrayList<>();

    public BookContentsCandidateAdapter() {
        setHasStableIds(
                true
        );
    }

    public void submitCandidates(
            @NonNull List<BookContentsScanResult.ChapterCandidate> candidates
    ) {
        editableCandidates.clear();

        for (int index = 0;
             index < candidates.size();
             index++) {

            BookContentsScanResult.ChapterCandidate candidate =
                    candidates.get(
                            index
                    );

            editableCandidates.add(
                    EditableCandidate.from(
                            candidate,
                            index
                    )
            );
        }

        notifyDataSetChanged();
    }

    public void clearCandidates() {
        if (editableCandidates.isEmpty()) {
            return;
        }

        editableCandidates.clear();

        notifyDataSetChanged();
    }

    @Override
    public long getItemId(
            int position
    ) {
        return editableCandidates
                .get(
                        position
                )
                .stableId;
    }

    @NonNull
    @Override
    public CandidateViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemBookContentsChapterCandidateBinding binding =
                ItemBookContentsChapterCandidateBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new CandidateViewHolder(
                binding
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull CandidateViewHolder holder,
            int position
    ) {
        holder.bind(
                editableCandidates.get(
                        position
                )
        );
    }

    @Override
    public int getItemCount() {
        return editableCandidates.size();
    }

    @NonNull
    public List<CandidateInput> getSelectedCandidateInputs() {
        List<CandidateInput> selectedInputs =
                new ArrayList<>();

        for (EditableCandidate candidate : editableCandidates) {
            if (!candidate.selected) {
                continue;
            }

            selectedInputs.add(
                    new CandidateInput(
                            candidate.chapterNumber,
                            candidate.chapterTitle,
                            parsePageNumber(
                                    candidate.startPageText
                            ),
                            candidate.rawDetectedLine,
                            candidate.confidence,
                            candidate.sourceLineNumber
                    )
            );
        }

        return Collections.unmodifiableList(
                selectedInputs
        );
    }

    public int getSelectedCandidateCount() {
        int selectedCount =
                0;

        for (EditableCandidate candidate : editableCandidates) {
            if (candidate.selected) {
                selectedCount++;
            }
        }

        return selectedCount;
    }

    public boolean hasCandidates() {
        return !editableCandidates.isEmpty();
    }

    public void selectAll() {
        boolean changed =
                false;

        for (EditableCandidate candidate : editableCandidates) {
            if (!candidate.selected) {
                candidate.selected =
                        true;

                changed =
                        true;
            }
        }

        if (changed) {
            notifyDataSetChanged();
        }
    }

    public void clearSelection() {
        boolean changed =
                false;

        for (EditableCandidate candidate : editableCandidates) {
            if (candidate.selected) {
                candidate.selected =
                        false;

                changed =
                        true;
            }
        }

        if (changed) {
            notifyDataSetChanged();
        }
    }

    private int parsePageNumber(
            @NonNull String pageText
    ) {
        String safePageText =
                pageText.trim();

        if (safePageText.isEmpty()) {
            return 0;
        }

        try {
            int pageNumber =
                    Integer.parseInt(
                            safePageText
                    );

            return Math.max(
                    0,
                    pageNumber
            );

        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public final class CandidateViewHolder
            extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemBookContentsChapterCandidateBinding binding;

        private boolean bindingValues;

        @NonNull
        private final TextWatcher chapterNumberWatcher =
                new SimpleTextWatcher() {

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        if (bindingValues) {
                            return;
                        }

                        EditableCandidate candidate =
                                getBoundCandidate();

                        if (candidate != null) {
                            candidate.chapterNumber =
                                    safeText(
                                            editable
                                    );

                            refreshChapterLabel(
                                    candidate
                            );
                        }
                    }
                };

        @NonNull
        private final TextWatcher chapterTitleWatcher =
                new SimpleTextWatcher() {

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        if (bindingValues) {
                            return;
                        }

                        EditableCandidate candidate =
                                getBoundCandidate();

                        if (candidate != null) {
                            candidate.chapterTitle =
                                    safeText(
                                            editable
                                    );
                        }
                    }
                };

        @NonNull
        private final TextWatcher startPageWatcher =
                new SimpleTextWatcher() {

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        if (bindingValues) {
                            return;
                        }

                        EditableCandidate candidate =
                                getBoundCandidate();

                        if (candidate != null) {
                            candidate.startPageText =
                                    safeText(
                                            editable
                                    );
                        }
                    }
                };

        private CandidateViewHolder(
                @NonNull ItemBookContentsChapterCandidateBinding binding
        ) {
            super(
                    binding.getRoot()
            );

            this.binding =
                    binding;

            this.binding
                    .candidateChapterNumberEditText
                    .addTextChangedListener(
                            chapterNumberWatcher
                    );

            this.binding
                    .candidateChapterTitleEditText
                    .addTextChangedListener(
                            chapterTitleWatcher
                    );

            this.binding
                    .candidateStartPageEditText
                    .addTextChangedListener(
                            startPageWatcher
                    );
        }

        private void bind(
                @NonNull EditableCandidate candidate
        ) {
            bindingValues =
                    true;

            binding.selectCandidateCheckBox
                    .setOnCheckedChangeListener(
                            null
                    );

            binding.selectCandidateCheckBox
                    .setChecked(
                            candidate.selected
                    );

            binding.candidateChapterNumberEditText
                    .setText(
                            candidate.chapterNumber
                    );

            binding.candidateChapterTitleEditText
                    .setText(
                            candidate.chapterTitle
                    );

            binding.candidateStartPageEditText
                    .setText(
                            candidate.startPageText
                    );

            binding.candidateRawLineTextView
                    .setText(
                            "OCR: "
                                    + candidate.rawDetectedLine
                    );

            refreshChapterLabel(
                    candidate
            );

            bindConfidence(
                    candidate
            );

            setCandidateFieldsEnabled(
                    candidate.selected
            );

            binding.selectCandidateCheckBox
                    .setOnCheckedChangeListener(
                            (buttonView, checked) -> {
                                candidate.selected =
                                        checked;

                                setCandidateFieldsEnabled(
                                        checked
                                );
                            }
                    );

            binding.candidateCard
                    .setOnClickListener(view ->
                            binding.selectCandidateCheckBox
                                    .setChecked(
                                            !binding
                                                    .selectCandidateCheckBox
                                                    .isChecked()
                                    )
                    );

            bindingValues =
                    false;
        }

        private void refreshChapterLabel(
                @NonNull EditableCandidate candidate
        ) {
            String label =
                    candidate.chapterNumber.isEmpty()
                            ? "Detected chapter"
                            : "Chapter "
                              + candidate.chapterNumber;

            binding.candidateChapterLabelTextView
                    .setText(
                            label
                    );
        }

        private void bindConfidence(
                @NonNull EditableCandidate candidate
        ) {
            int confidencePercent =
                    Math.round(
                            candidate.confidence
                                    * 100F
                    );

            binding.candidateConfidenceTextView
                    .setText(
                            String.format(
                                    Locale.getDefault(),
                                    "%d%%",
                                    confidencePercent
                            )
                    );

            if (candidate.confidence
                    < LOW_CONFIDENCE_THRESHOLD) {

                binding.candidateWarningTextView
                        .setText(
                                "Low-confidence result: "
                                        + "please verify every field."
                        );

                binding.candidateWarningTextView
                        .setVisibility(
                                View.VISIBLE
                        );

            } else if (candidate.confidence
                    < MEDIUM_CONFIDENCE_THRESHOLD) {

                binding.candidateWarningTextView
                        .setText(
                                "Please verify this detected result."
                        );

                binding.candidateWarningTextView
                        .setVisibility(
                                View.VISIBLE
                        );

            } else {
                binding.candidateWarningTextView
                        .setText(
                                ""
                        );

                binding.candidateWarningTextView
                        .setVisibility(
                                View.GONE
                        );
            }
        }

        private void setCandidateFieldsEnabled(
                boolean enabled
        ) {
            binding.candidateChapterNumberEditText
                    .setEnabled(
                            enabled
                    );

            binding.candidateChapterTitleEditText
                    .setEnabled(
                            enabled
                    );

            binding.candidateStartPageEditText
                    .setEnabled(
                            enabled
                    );

            binding.candidateCard
                    .setAlpha(
                            enabled
                                    ? 1F
                                    : 0.55F
                    );
        }

        @Nullable
        private EditableCandidate getBoundCandidate() {
            int adapterPosition =
                    getBindingAdapterPosition();

            if (adapterPosition
                    == RecyclerView.NO_POSITION
                    || adapterPosition < 0
                    || adapterPosition
                    >= editableCandidates.size()) {

                return null;
            }

            return editableCandidates.get(
                    adapterPosition
            );
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
            // No action required.
        }

        @Override
        public void onTextChanged(
                CharSequence sequence,
                int start,
                int before,
                int count
        ) {
            // No action required.
        }
    }

    private static final class EditableCandidate {

        private final long stableId;

        @NonNull
        private String chapterNumber;

        @NonNull
        private String chapterTitle;

        @NonNull
        private String startPageText;

        @NonNull
        private final String rawDetectedLine;

        private final float confidence;

        private final int sourceLineNumber;

        private boolean selected;

        private EditableCandidate(
                long stableId,
                @NonNull String chapterNumber,
                @NonNull String chapterTitle,
                @NonNull String startPageText,
                @NonNull String rawDetectedLine,
                float confidence,
                int sourceLineNumber,
                boolean selected
        ) {
            this.stableId =
                    stableId;

            this.chapterNumber =
                    chapterNumber;

            this.chapterTitle =
                    chapterTitle;

            this.startPageText =
                    startPageText;

            this.rawDetectedLine =
                    rawDetectedLine;

            this.confidence =
                    confidence;

            this.sourceLineNumber =
                    sourceLineNumber;

            this.selected =
                    selected;
        }

        @NonNull
        private static EditableCandidate from(
                @NonNull BookContentsScanResult
                        .ChapterCandidate candidate,
                int position
        ) {
            String startPageText =
                    candidate.getStartPageNumber()
                            <= 0
                            ? ""
                            : String.valueOf(
                            candidate.getStartPageNumber()
                    );

            long stableId =
                    createStableId(
                            candidate,
                            position
                    );

            return new EditableCandidate(
                    stableId,
                    candidate.getChapterNumber(),
                    candidate.getChapterTitle(),
                    startPageText,
                    candidate.getRawDetectedLine(),
                    candidate.getConfidence(),
                    candidate.getSourceLineNumber(),
                    candidate.isSelected()
            );
        }

        private static long createStableId(
                @NonNull BookContentsScanResult
                        .ChapterCandidate candidate,
                int position
        ) {
            String identity =
                    candidate.getSourceLineNumber()
                            + "|"
                            + candidate.getChapterNumber()
                            + "|"
                            + candidate.getChapterTitle()
                            + "|"
                            + position;

            return identity.hashCode();
        }
    }

    public static final class CandidateInput {

        @NonNull
        private final String chapterNumber;

        @NonNull
        private final String chapterTitle;

        private final int startPageNumber;

        @NonNull
        private final String rawDetectedLine;

        private final float confidence;

        private final int sourceLineNumber;

        private CandidateInput(
                @NonNull String chapterNumber,
                @NonNull String chapterTitle,
                int startPageNumber,
                @NonNull String rawDetectedLine,
                float confidence,
                int sourceLineNumber
        ) {
            this.chapterNumber =
                    chapterNumber.trim();

            this.chapterTitle =
                    chapterTitle.trim();

            this.startPageNumber =
                    Math.max(
                            0,
                            startPageNumber
                    );

            this.rawDetectedLine =
                    rawDetectedLine.trim();

            this.confidence =
                    Math.max(
                            0F,
                            Math.min(
                                    1F,
                                    confidence
                            )
                    );

            this.sourceLineNumber =
                    Math.max(
                            0,
                            sourceLineNumber
                    );
        }

        @NonNull
        public String getChapterNumber() {
            return chapterNumber;
        }

        @NonNull
        public String getChapterTitle() {
            return chapterTitle;
        }

        public int getStartPageNumber() {
            return startPageNumber;
        }

        @NonNull
        public String getRawDetectedLine() {
            return rawDetectedLine;
        }

        public float getConfidence() {
            return confidence;
        }

        public int getSourceLineNumber() {
            return sourceLineNumber;
        }

        public boolean hasValidTitle() {
            return !chapterTitle.isEmpty();
        }
    }

    @NonNull
    private static String safeText(
            Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }
}