package com.tridev.studysaathi.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.databinding
        .ItemSchoolBookChapterBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SchoolBookChapterAdapter
        extends RecyclerView.Adapter<
        SchoolBookChapterAdapter.ChapterViewHolder> {

    @NonNull
    private final List<SchoolBookChapterEntity> chapters =
            new ArrayList<>();

    @NonNull
    private final ChapterActionListener actionListener;

    public SchoolBookChapterAdapter(
            @NonNull ChapterActionListener actionListener
    ) {
        this.actionListener =
                actionListener;

        setHasStableIds(
                true
        );
    }

    @Override
    public long getItemId(
            int position
    ) {
        SchoolBookChapterEntity chapter =
                chapters.get(
                        position
                );

        long chapterRowId =
                chapter.getChapterRowId();

        if (chapterRowId > 0L) {
            return chapterRowId;
        }

        return chapter.getChapterId()
                .hashCode();
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSchoolBookChapterBinding binding =
                ItemSchoolBookChapterBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new ChapterViewHolder(
                binding
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull ChapterViewHolder holder,
            int position
    ) {
        holder.bind(
                chapters.get(
                        position
                )
        );
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    /**
     * Adapter की पूरी chapter list replace करता है।
     */
    public void submitChapters(
            @NonNull List<SchoolBookChapterEntity> newChapters
    ) {
        chapters.clear();

        chapters.addAll(
                newChapters
        );

        notifyDataSetChanged();
    }

    /**
     * Adapter को empty करता है।
     */
    public void clearChapters() {
        if (chapters.isEmpty()) {
            return;
        }

        chapters.clear();

        notifyDataSetChanged();
    }

    /**
     * वर्तमान list की सुरक्षित read-only copy देता है।
     */
    @NonNull
    public List<SchoolBookChapterEntity> getCurrentChapters() {
        return Collections.unmodifiableList(
                new ArrayList<>(
                        chapters
                )
        );
    }

    public final class ChapterViewHolder
            extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemSchoolBookChapterBinding binding;

        private ChapterViewHolder(
                @NonNull ItemSchoolBookChapterBinding binding
        ) {
            super(
                    binding.getRoot()
            );

            this.binding =
                    binding;
        }

        private void bind(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            bindChapterIdentity(
                    chapter
            );

            bindPageAndProcessingInformation(
                    chapter
            );

            bindConfirmationStatus(
                    chapter
            );

            bindProgress(
                    chapter
            );

            bindOptionalAndRevisionStatus(
                    chapter
            );

            bindActions(
                    chapter
            );
        }

        private void bindChapterIdentity(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            binding.chapterLabelTextView
                    .setText(
                            chapter.getChapterLabel()
                    );

            String englishTitle =
                    chapter.getChapterTitleEnglish();

            String hindiTitle =
                    chapter.getChapterTitleHindi();

            if (!englishTitle.isEmpty()) {
                binding.chapterTitleEnglishTextView
                        .setText(
                                englishTitle
                        );

            } else if (!hindiTitle.isEmpty()) {
                /*
                 * English title खाली होने पर primary title के
                 * स्थान पर Hindi title दिखाएँ।
                 */
                binding.chapterTitleEnglishTextView
                        .setText(
                                hindiTitle
                        );

            } else {
                binding.chapterTitleEnglishTextView
                        .setText(
                                chapter.getDisplayTitle()
                        );
            }

            boolean showHindiAsSecondary =
                    !englishTitle.isEmpty()
                            && !hindiTitle.isEmpty();

            setTextOrHide(
                    binding.chapterTitleHindiTextView,
                    showHindiAsSecondary
                            ? hindiTitle
                            : ""
            );

            setTextOrHide(
                    binding.chapterSubtitleTextView,
                    chapter.getChapterSubtitle()
            );
        }

        private void bindPageAndProcessingInformation(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            setTextOrHide(
                    binding.chapterPageRangeTextView,
                    chapter.getPageRangeLabel()
            );

            String processingStatus =
                    chapter.getContentProcessingStatus();

            if (processingStatus.isEmpty()) {
                binding.chapterProcessingStatusTextView
                        .setVisibility(
                                View.GONE
                        );

            } else {
                binding.chapterProcessingStatusTextView
                        .setText(
                                processingStatus.replace(
                                        '_',
                                        ' '
                                )
                        );

                binding.chapterProcessingStatusTextView
                        .setVisibility(
                                View.VISIBLE
                        );
            }
        }

        private void bindConfirmationStatus(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            binding.parentConfirmationTextView
                    .setText(
                            chapter.getConfirmationStatusLabel()
                    );

            if (chapter.isParentConfirmed()) {
                binding.parentConfirmationTextView
                        .setTextColor(
                                Color.rgb(
                                        32,
                                        122,
                                        60
                                )
                        );

                binding.parentConfirmationTextView
                        .setBackgroundColor(
                                Color.rgb(
                                        232,
                                        245,
                                        233
                                )
                        );

                return;
            }

            boolean pendingParentReview =
                    SchoolBookChapterEntity
                            .CONTENT_SOURCE_BOOK_TOC_SCAN
                            .equals(
                                    chapter.getContentSource()
                            )
                            || SchoolBookChapterEntity
                            .CONTENT_SOURCE_AI_EXTRACTED
                            .equals(
                                    chapter.getContentSource()
                            );

            if (pendingParentReview) {
                binding.parentConfirmationTextView
                        .setTextColor(
                                Color.rgb(
                                        128,
                                        84,
                                        0
                                )
                        );

                binding.parentConfirmationTextView
                        .setBackgroundColor(
                                Color.rgb(
                                        255,
                                        244,
                                        204
                                )
                        );

            } else {
                binding.parentConfirmationTextView
                        .setTextColor(
                                Color.rgb(
                                        180,
                                        35,
                                        24
                                )
                        );

                binding.parentConfirmationTextView
                        .setBackgroundColor(
                                Color.rgb(
                                        255,
                                        241,
                                        240
                                )
                        );
            }
        }

        private void bindProgress(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            binding.chapterProgressTextView
                    .setText(
                            chapter.getProgressLabel()
                    );

            binding.chapterProgressBar
                    .setProgress(
                            chapter.getProgressPercent()
                    );
        }

        private void bindOptionalAndRevisionStatus(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            String statusText =
                    "";

            if (chapter.isOptionalChapter()
                    && chapter.isRevisionChapter()) {

                statusText =
                        "Optional • Revision chapter";

            } else if (chapter.isOptionalChapter()) {
                statusText =
                        "Optional chapter";

            } else if (chapter.isRevisionChapter()) {
                statusText =
                        "Revision chapter";
            }

            setTextOrHide(
                    binding.chapterOptionalStatusTextView,
                    statusText
            );
        }

        private void bindActions(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            /*
             * Recycled ViewHolder पर पुराने listener को हटाना जरूरी है,
             * अन्यथा setChecked() गलत callback चला सकता है।
             */
            binding.chapterEnabledCheckBox
                    .setOnCheckedChangeListener(
                            null
                    );

            binding.chapterEnabledCheckBox
                    .setChecked(
                            chapter.isEnabled()
                    );

            binding.chapterEnabledCheckBox
                    .setText(
                            chapter.isEnabled()
                                    ? "Enabled"
                                    : "Disabled"
                    );

            binding.chapterEnabledCheckBox
                    .setOnCheckedChangeListener(
                            (buttonView, checked) -> {
                                binding.chapterEnabledCheckBox
                                        .setText(
                                                checked
                                                        ? "Enabled"
                                                        : "Disabled"
                                        );

                                actionListener
                                        .onChapterEnabledChanged(
                                                chapter,
                                                checked
                                        );
                            }
                    );

            binding.editChapterButton
                    .setOnClickListener(view ->
                            actionListener.onEditChapter(
                                    chapter
                            )
                    );

            binding.chapterItemRoot
                    .setOnClickListener(view ->
                            actionListener.onOpenChapter(
                                    chapter
                            )
                    );
        }

        private void setTextOrHide(
                @NonNull android.widget.TextView textView,
                @NonNull String text
        ) {
            String safeText =
                    text.trim();

            if (safeText.isEmpty()) {
                textView.setText(
                        ""
                );

                textView.setVisibility(
                        View.GONE
                );

            } else {
                textView.setText(
                        safeText
                );

                textView.setVisibility(
                        View.VISIBLE
                );
            }
        }
    }

    public interface ChapterActionListener {

        /**
         * Chapter row पर tap होने पर।
         */
        void onOpenChapter(
                @NonNull SchoolBookChapterEntity chapter
        );

        /**
         * Edit button दबाने पर।
         */
        void onEditChapter(
                @NonNull SchoolBookChapterEntity chapter
        );

        /**
         * Enabled checkbox बदलने पर।
         */
        void onChapterEnabledChanged(
                @NonNull SchoolBookChapterEntity chapter,
                boolean enabled
        );
    }
}