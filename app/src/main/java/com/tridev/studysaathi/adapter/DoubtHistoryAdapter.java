package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;
import com.tridev.studysaathi.databinding.ItemDoubtHistoryBinding;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoubtHistoryAdapter extends RecyclerView.Adapter<
        DoubtHistoryAdapter.DoubtHistoryViewHolder> {

    private final List<DoubtHistoryEntity> historyItems;

    private final OnHistoryClickListener
            onHistoryClickListener;

    private final OnDeleteClickListener
            onDeleteClickListener;

    public DoubtHistoryAdapter(
            @NonNull List<DoubtHistoryEntity> historyItems,
            @NonNull OnHistoryClickListener onHistoryClickListener,
            @NonNull OnDeleteClickListener onDeleteClickListener
    ) {
        this.historyItems =
                new ArrayList<>(historyItems);

        this.onHistoryClickListener =
                onHistoryClickListener;

        this.onDeleteClickListener =
                onDeleteClickListener;
    }

    @NonNull
    @Override
    public DoubtHistoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemDoubtHistoryBinding binding =
                ItemDoubtHistoryBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new DoubtHistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DoubtHistoryViewHolder holder,
            int position
    ) {
        holder.bind(historyItems.get(position));
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public void submitList(
            @NonNull List<DoubtHistoryEntity> updatedItems
    ) {
        historyItems.clear();
        historyItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    public interface OnHistoryClickListener {

        void onHistoryClicked(
                @NonNull DoubtHistoryEntity historyItem
        );
    }

    public interface OnDeleteClickListener {

        void onDeleteClicked(
                @NonNull DoubtHistoryEntity historyItem
        );
    }

    class DoubtHistoryViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemDoubtHistoryBinding binding;

        DoubtHistoryViewHolder(
                @NonNull ItemDoubtHistoryBinding binding
        ) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(
                @NonNull DoubtHistoryEntity historyItem
        ) {
            binding.textHistorySubject.setText(
                    historyItem.getSubjectName()
            );

            binding.textHistoryChapter.setText(
                    historyItem.getChapterTitle()
            );

            binding.textHistoryQuestion.setText(
                    historyItem.getQuestionText()
            );

            binding.textHistoryAnswer.setText(
                    historyItem.getAnswerText()
            );

            binding.textHistoryDate.setText(
                    formatDate(historyItem.getCreatedAt())
            );

            binding.cardDoubtHistory.setOnClickListener(
                    view ->
                            onHistoryClickListener
                                    .onHistoryClicked(
                                            historyItem
                                    )
            );

            binding.buttonDeleteHistory.setOnClickListener(
                    view ->
                            onDeleteClickListener
                                    .onDeleteClicked(
                                            historyItem
                                    )
            );
        }

        @NonNull
        private String formatDate(long createdAt) {
            if (createdAt <= 0L) {
                return binding.getRoot()
                        .getContext()
                        .getString(
                                R.string.saved_doubt
                        );
            }

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                    );

            return Instant.ofEpochMilli(createdAt)
                    .atZone(ZoneId.systemDefault())
                    .format(formatter);
        }
    }
}