package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.databinding.ItemChapterBinding;
import com.tridev.studysaathi.model.ChapterItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChapterAdapter extends RecyclerView.Adapter<
        ChapterAdapter.ChapterViewHolder> {

    private final List<ChapterItem> chapterItems;
    private final OnChapterClickListener onChapterClickListener;

    public ChapterAdapter(
            @NonNull List<ChapterItem> chapterItems,
            @NonNull OnChapterClickListener onChapterClickListener
    ) {
        this.chapterItems = new ArrayList<>(chapterItems);
        this.onChapterClickListener = onChapterClickListener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemChapterBinding binding =
                ItemChapterBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ChapterViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ChapterViewHolder holder,
            int position
    ) {
        holder.bind(chapterItems.get(position));
    }

    @Override
    public int getItemCount() {
        return chapterItems.size();
    }

    public void submitList(
            @NonNull List<ChapterItem> updatedChapters
    ) {
        chapterItems.clear();
        chapterItems.addAll(updatedChapters);
        notifyDataSetChanged();
    }

    public interface OnChapterClickListener {

        void onChapterClicked(
                @NonNull ChapterItem chapterItem
        );
    }

    class ChapterViewHolder extends RecyclerView.ViewHolder {

        private final ItemChapterBinding binding;

        ChapterViewHolder(
                @NonNull ItemChapterBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ChapterItem chapterItem) {
            binding.textChapterNumber.setText(
                    String.format(
                            Locale.getDefault(),
                            "%02d",
                            chapterItem.getChapterNumber()
                    )
            );

            binding.textChapterTitle.setText(
                    chapterItem.getChapterTitle()
            );

            binding.textChapterDescription.setText(
                    chapterItem.getChapterDescription()
            );

            binding.textLessonCount.setText(
                    binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.lesson_count_format,
                                    chapterItem.getLessonCount()
                            )
            );

            int progress = Math.max(
                    0,
                    Math.min(100, chapterItem.getProgressPercent())
            );

            binding.progressChapter.setProgressCompat(
                    progress,
                    false
            );

            binding.textProgressPercent.setText(
                    binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.progress_percent_format,
                                    progress
                            )
            );

            applyProgressState(progress);

            binding.cardChapter.setOnClickListener(view ->
                    onChapterClickListener.onChapterClicked(
                            chapterItem
                    )
            );
        }

        private void applyProgressState(int progress) {
            int statusText;
            int statusTextColor;
            int statusBackgroundColor;

            if (progress >= 100) {
                statusText = R.string.chapter_status_completed;
                statusTextColor = R.color.ss_success;
                statusBackgroundColor = R.color.ss_green_soft;
            } else if (progress > 0) {
                statusText = R.string.chapter_status_continue;
                statusTextColor = R.color.ss_primary;
                statusBackgroundColor = R.color.ss_blue_soft;
            } else {
                statusText = R.string.chapter_status_start;
                statusTextColor = R.color.ss_warning;
                statusBackgroundColor = R.color.ss_yellow_soft;
            }

            binding.textChapterStatus.setText(statusText);

            binding.textChapterStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusTextColor
                    )
            );

            binding.cardChapterStatus.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusBackgroundColor
                    )
            );
        }
    }
}