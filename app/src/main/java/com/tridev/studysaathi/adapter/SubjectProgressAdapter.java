package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.databinding.ItemSubjectProgressBinding;
import com.tridev.studysaathi.model.SubjectProgressItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubjectProgressAdapter extends RecyclerView.Adapter<
        SubjectProgressAdapter.SubjectProgressViewHolder> {

    private final List<SubjectProgressItem> subjectProgressItems;

    public SubjectProgressAdapter(
            @NonNull List<SubjectProgressItem> subjectProgressItems
    ) {
        this.subjectProgressItems =
                new ArrayList<>(subjectProgressItems);
    }

    @NonNull
    @Override
    public SubjectProgressViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSubjectProgressBinding binding =
                ItemSubjectProgressBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new SubjectProgressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SubjectProgressViewHolder holder,
            int position
    ) {
        holder.bind(
                subjectProgressItems.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return subjectProgressItems.size();
    }

    public void submitList(
            @NonNull List<SubjectProgressItem> updatedItems
    ) {
        subjectProgressItems.clear();
        subjectProgressItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    class SubjectProgressViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemSubjectProgressBinding binding;

        SubjectProgressViewHolder(
                @NonNull ItemSubjectProgressBinding binding
        ) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(
                @NonNull SubjectProgressItem subjectProgress
        ) {
            binding.textSubjectIcon.setText(
                    getSubjectInitial(
                            subjectProgress.getSubjectName()
                    )
            );

            binding.textSubjectName.setText(
                    subjectProgress.getSubjectName()
            );

            binding.textSubjectLessons.setText(
                    binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.analytics_subject_lessons_format,
                                    subjectProgress.getCompletedLessons(),
                                    subjectProgress.getTotalLessons()
                            )
            );

            binding.textSubjectRevisions.setText(
                    binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.analytics_subject_revisions_format,
                                    subjectProgress.getRevisionCount()
                            )
            );

            if (subjectProgress.getQuizAttempts() > 0) {
                binding.textSubjectQuiz.setText(
                        binding.getRoot()
                                .getContext()
                                .getString(
                                        R.string.analytics_subject_quiz_format,
                                        subjectProgress.getQuizAttempts(),
                                        subjectProgress.getAverageQuizScore()
                                )
                );
            } else {
                binding.textSubjectQuiz.setText(
                        R.string.analytics_subject_no_quiz
                );
            }

            binding.textSubjectPercent.setText(
                    binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.analytics_subject_percent_format,
                                    subjectProgress.getCompletionPercent()
                            )
            );

            binding.progressSubjectCompletion.setProgressCompat(
                    subjectProgress.getCompletionPercent(),
                    false
            );

            applyLearningStatus(subjectProgress);
        }

        private void applyLearningStatus(
                @NonNull SubjectProgressItem subjectProgress
        ) {
            int statusText;
            int statusBackground;
            int statusBorder;
            int statusTextColor;

            if (subjectProgress.getQuizAttempts() > 0
                    && subjectProgress.getAverageQuizScore() >= 80) {

                statusText =
                        R.string.analytics_status_strong;

                statusBackground =
                        R.color.ss_green_soft;

                statusBorder =
                        R.color.ss_green_border;

                statusTextColor =
                        R.color.ss_success;

            } else if (subjectProgress.hasLearningActivity()) {

                statusText =
                        R.string.analytics_status_growing;

                statusBackground =
                        R.color.ss_yellow_soft;

                statusBorder =
                        R.color.ss_yellow_border;

                statusTextColor =
                        R.color.ss_warning;

            } else {

                statusText =
                        R.string.analytics_status_start;

                statusBackground =
                        R.color.ss_blue_soft;

                statusBorder =
                        R.color.ss_blue_border;

                statusTextColor =
                        R.color.ss_primary;
            }

            binding.textSubjectStatus.setText(
                    statusText
            );

            binding.cardSubjectStatus.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusBackground
                    )
            );

            binding.cardSubjectStatus.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusBorder
                    )
            );

            binding.textSubjectStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusTextColor
                    )
            );

            binding.cardSubjectIcon.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusBackground
                    )
            );

            binding.cardSubjectIcon.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusBorder
                    )
            );

            binding.textSubjectIcon.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            statusTextColor
                    )
            );
        }

        @NonNull
        private String getSubjectInitial(
                String subjectName
        ) {
            if (subjectName == null
                    || subjectName.trim().isEmpty()) {
                return "S";
            }

            String trimmedName =
                    subjectName.trim();

            if (trimmedName.length() <= 2) {
                return trimmedName.toUpperCase(
                        Locale.getDefault()
                );
            }

            return trimmedName
                    .substring(0, 1)
                    .toUpperCase(Locale.getDefault());
        }
    }
}