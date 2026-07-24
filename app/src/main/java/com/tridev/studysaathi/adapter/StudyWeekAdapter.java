package com.tridev.studysaathi.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.databinding.ItemStudyDayBinding;
import com.tridev.studysaathi.model.StudyDayItem;

import java.util.ArrayList;
import java.util.List;

public class StudyWeekAdapter
        extends RecyclerView.Adapter<
        StudyWeekAdapter.StudyDayViewHolder> {

    private final List<StudyDayItem> studyDays =
            new ArrayList<>();

    public StudyWeekAdapter(
            @NonNull List<StudyDayItem> initialItems
    ) {
        studyDays.addAll(initialItems);
    }

    @NonNull
    @Override
    public StudyDayViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemStudyDayBinding binding =
                ItemStudyDayBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new StudyDayViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull StudyDayViewHolder holder,
            int position
    ) {
        holder.bind(
                studyDays.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return studyDays.size();
    }

    public void submitList(
            @NonNull List<StudyDayItem> updatedItems
    ) {
        studyDays.clear();
        studyDays.addAll(updatedItems);
        notifyDataSetChanged();
    }

    static class StudyDayViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemStudyDayBinding binding;

        StudyDayViewHolder(
                @NonNull ItemStudyDayBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull StudyDayItem studyDay
        ) {
            Context context =
                    binding.getRoot().getContext();

            binding.textStudyDayName.setText(
                    studyDay.isToday()
                            ? context.getString(
                            R.string.weekly_study_today_format,
                            studyDay.getDayName()
                    )
                            : studyDay.getDayName()
            );

            binding.textStudyDayDate.setText(
                    studyDay.getDateText()
            );

            binding.textStudyDayLessons.setText(
                    context.getString(
                            R.string.weekly_study_lessons_format,
                            studyDay.getLessonCount()
                    )
            );

            binding.textStudyDayRevisions.setText(
                    context.getString(
                            R.string.weekly_study_revisions_format,
                            studyDay.getRevisionCount()
                    )
            );

            binding.textStudyDayQuizzes.setText(
                    context.getString(
                            R.string.weekly_study_quizzes_format,
                            studyDay.getQuizCount()
                    )
            );

            binding.textStudyDayTotal.setText(
                    context.getString(
                            R.string.weekly_study_actions_format,
                            studyDay.getTotalActions(),
                            studyDay.getDailyGoal()
                    )
            );

            binding.progressStudyDay.setMax(
                    studyDay.getDailyGoal()
            );

            binding.progressStudyDay.setProgressCompat(
                    Math.min(
                            studyDay.getTotalActions(),
                            studyDay.getDailyGoal()
                    ),
                    false
            );

            showStudyStatus(
                    context,
                    studyDay
            );
        }

        private void showStudyStatus(
                @NonNull Context context,
                @NonNull StudyDayItem studyDay
        ) {
            if (studyDay.isGoalCompleted()) {
                binding.textStudyDayStatus.setText(
                        R.string.weekly_study_goal_completed
                );

                binding.textStudyDayStatus.setTextColor(
                        context.getColor(
                                R.color.ss_success
                        )
                );

                binding.cardStudyDay.setStrokeColor(
                        context.getColor(
                                R.color.ss_green_border
                        )
                );

                return;
            }

            if (studyDay.hasStudyActivity()) {
                binding.textStudyDayStatus.setText(
                        context.getString(
                                R.string.weekly_study_goal_remaining_format,
                                Math.max(
                                        0,
                                        studyDay.getDailyGoal()
                                                - studyDay.getTotalActions()
                                )
                        )
                );

                binding.textStudyDayStatus.setTextColor(
                        context.getColor(
                                R.color.ss_warning
                        )
                );

                binding.cardStudyDay.setStrokeColor(
                        context.getColor(
                                R.color.ss_yellow_border
                        )
                );

                return;
            }

            binding.textStudyDayStatus.setText(
                    studyDay.isToday()
                            ? R.string.weekly_study_start_today
                            : R.string.weekly_study_no_activity
            );

            binding.textStudyDayStatus.setTextColor(
                    context.getColor(
                            R.color.ss_text_muted
                    )
            );

            binding.cardStudyDay.setStrokeColor(
                    context.getColor(
                            R.color.ss_outline
                    )
            );
        }
    }
}