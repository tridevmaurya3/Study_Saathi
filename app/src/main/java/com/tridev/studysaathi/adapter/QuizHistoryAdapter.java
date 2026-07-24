package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.databinding.ItemQuizHistoryBinding;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuizHistoryAdapter extends RecyclerView.Adapter<
        QuizHistoryAdapter.QuizHistoryViewHolder> {

    private final List<QuizAttemptEntity> quizAttempts;
    private final OnQuizAttemptClickListener onQuizAttemptClickListener;

    public QuizHistoryAdapter(
            @NonNull List<QuizAttemptEntity> quizAttempts,
            @NonNull OnQuizAttemptClickListener onQuizAttemptClickListener
    ) {
        this.quizAttempts = new ArrayList<>(quizAttempts);
        this.onQuizAttemptClickListener =
                onQuizAttemptClickListener;
    }

    @NonNull
    @Override
    public QuizHistoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemQuizHistoryBinding binding =
                ItemQuizHistoryBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new QuizHistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull QuizHistoryViewHolder holder,
            int position
    ) {
        holder.bind(quizAttempts.get(position));
    }

    @Override
    public int getItemCount() {
        return quizAttempts.size();
    }

    public void submitList(
            @NonNull List<QuizAttemptEntity> updatedAttempts
    ) {
        quizAttempts.clear();
        quizAttempts.addAll(updatedAttempts);
        notifyDataSetChanged();
    }

    public interface OnQuizAttemptClickListener {

        void onQuizAttemptClicked(
                @NonNull QuizAttemptEntity quizAttempt
        );
    }

    class QuizHistoryViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemQuizHistoryBinding binding;

        QuizHistoryViewHolder(
                @NonNull ItemQuizHistoryBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull QuizAttemptEntity quizAttempt
        ) {
            binding.textQuizSubject.setText(
                    quizAttempt.getSubjectName()
            );

            binding.textQuizChapter.setText(
                    quizAttempt.getChapterTitle()
            );

            binding.textQuizScore.setText(
                    binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.quiz_history_score_format,
                                    quizAttempt.getCorrectAnswers(),
                                    quizAttempt.getTotalQuestions(),
                                    quizAttempt.getPercentage()
                            )
            );

            binding.textQuizDate.setText(
                    formatAttemptDate(
                            quizAttempt.getAttemptedAt()
                    )
            );

            applyPerformanceState(
                    quizAttempt.getPercentage()
            );

            binding.cardQuizHistory.setOnClickListener(
                    view ->
                            onQuizAttemptClickListener
                                    .onQuizAttemptClicked(
                                            quizAttempt
                                    )
            );
        }

        private void applyPerformanceState(
                int percentage
        ) {
            int gradeText;
            int backgroundColor;
            int borderColor;
            int textColor;

            if (percentage >= 80) {
                gradeText =
                        R.string.quiz_grade_excellent;

                backgroundColor =
                        R.color.ss_green_soft;

                borderColor =
                        R.color.ss_green_border;

                textColor =
                        R.color.ss_success;
            } else if (percentage >= 50) {
                gradeText =
                        R.string.quiz_grade_good;

                backgroundColor =
                        R.color.ss_yellow_soft;

                borderColor =
                        R.color.ss_yellow_border;

                textColor =
                        R.color.ss_warning;
            } else {
                gradeText =
                        R.string.quiz_grade_practise;

                backgroundColor =
                        R.color.ss_red_soft;

                borderColor =
                        R.color.ss_red_border;

                textColor =
                        R.color.ss_error;
            }

            binding.textQuizGrade.setText(gradeText);

            binding.cardQuizGrade.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            backgroundColor
                    )
            );

            binding.cardQuizGrade.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            borderColor
                    )
            );

            binding.textQuizGrade.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            textColor
                    )
            );

            binding.textQuizArrow.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            textColor
                    )
            );
        }

        @NonNull
        private String formatAttemptDate(
                long attemptedAt
        ) {
            if (attemptedAt <= 0L) {
                return binding.getRoot()
                        .getContext()
                        .getString(
                                R.string.quiz_history_saved_attempt
                        );
            }

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                    );

            return Instant.ofEpochMilli(attemptedAt)
                    .atZone(ZoneId.systemDefault())
                    .format(formatter);
        }
    }
}