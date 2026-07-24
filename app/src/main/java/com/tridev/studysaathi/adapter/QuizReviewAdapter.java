package com.tridev.studysaathi.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.databinding.ItemQuizReviewBinding;
import com.tridev.studysaathi.model.QuizReviewItem;

import java.util.ArrayList;
import java.util.List;

public class QuizReviewAdapter
        extends RecyclerView.Adapter<
        QuizReviewAdapter.QuizReviewViewHolder> {

    private final List<QuizReviewItem> reviewItems =
            new ArrayList<>();

    public QuizReviewAdapter(
            @NonNull List<QuizReviewItem> initialItems
    ) {
        reviewItems.addAll(initialItems);
    }

    @NonNull
    @Override
    public QuizReviewViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemQuizReviewBinding binding =
                ItemQuizReviewBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new QuizReviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull QuizReviewViewHolder holder,
            int position
    ) {
        holder.bind(
                reviewItems.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return reviewItems.size();
    }

    public void submitList(
            @NonNull List<QuizReviewItem> updatedItems
    ) {
        reviewItems.clear();
        reviewItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    static class QuizReviewViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemQuizReviewBinding binding;

        QuizReviewViewHolder(
                @NonNull ItemQuizReviewBinding binding
        ) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(
                @NonNull QuizReviewItem reviewItem
        ) {
            Context context =
                    binding.getRoot().getContext();

            binding.textReviewQuestionNumber.setText(
                    context.getString(
                            R.string.quiz_review_question_format,
                            reviewItem.getQuestionNumber()
                    )
            );

            binding.textReviewQuestion.setText(
                    reviewItem.getQuestionText()
            );

            binding.textReviewSelectedAnswer.setText(
                    context.getString(
                            R.string.quiz_review_selected_answer_format,
                            reviewItem.getSelectedAnswer()
                    )
            );

            binding.textReviewCorrectAnswer.setText(
                    context.getString(
                            R.string.quiz_review_correct_answer_format,
                            reviewItem.getCorrectAnswer()
                    )
            );

            binding.textReviewExplanation.setText(
                    context.getString(
                            R.string.quiz_review_explanation_format,
                            reviewItem.getExplanation()
                    )
            );

            if (reviewItem.isCorrect()) {
                showCorrectState(context);
            } else {
                showIncorrectState(context);
            }
        }

        private void showCorrectState(
                @NonNull Context context
        ) {
            binding.textReviewStatus.setText(
                    R.string.quiz_review_correct
            );

            binding.textReviewStatus.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ss_success
                    )
            );

            binding.textReviewSelectedAnswer.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ss_success
                    )
            );

            binding.cardQuizReview.setCardBackgroundColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ss_green_soft
                    )
            );

            binding.cardQuizReview.setStrokeColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ss_green_border
                    )
            );
        }

        private void showIncorrectState(
                @NonNull Context context
        ) {
            binding.textReviewStatus.setText(
                    R.string.quiz_review_incorrect
            );

            binding.textReviewStatus.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ss_error
                    )
            );

            binding.textReviewSelectedAnswer.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ss_error
                    )
            );

            binding.cardQuizReview.setCardBackgroundColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ss_red_soft
                    )
            );

            binding.cardQuizReview.setStrokeColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ss_red_border
                    )
            );
        }
    }
}