package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.databinding.ItemAchievementBinding;
import com.tridev.studysaathi.model.AchievementItem;

import java.util.ArrayList;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<
        AchievementAdapter.AchievementViewHolder> {

    private final List<AchievementItem> achievementItems;

    public AchievementAdapter(
            @NonNull List<AchievementItem> achievementItems
    ) {
        this.achievementItems =
                new ArrayList<>(achievementItems);
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemAchievementBinding binding =
                ItemAchievementBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new AchievementViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AchievementViewHolder holder,
            int position
    ) {
        holder.bind(
                achievementItems.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return achievementItems.size();
    }

    public void submitList(
            @NonNull List<AchievementItem> updatedItems
    ) {
        achievementItems.clear();
        achievementItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    class AchievementViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemAchievementBinding binding;

        AchievementViewHolder(
                @NonNull ItemAchievementBinding binding
        ) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(
                @NonNull AchievementItem achievement
        ) {
            binding.textAchievementIcon.setText(
                    achievement.getIconText()
            );

            binding.textAchievementTitle.setText(
                    achievement.getTitle()
            );

            binding.textAchievementDescription.setText(
                    achievement.getDescription()
            );

            binding.progressAchievement.setProgressCompat(
                    achievement.getProgressPercent(),
                    false
            );

            if (achievement.isUnlocked()) {
                showUnlockedState();
            } else {
                showLockedState(achievement);
            }
        }

        private void showUnlockedState() {
            binding.textAchievementStatus.setText(
                    R.string.achievement_unlocked
            );

            binding.textAchievementProgress.setText(
                    R.string.achievement_completed
            );

            binding.cardAchievement.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_green_soft
                    )
            );

            binding.cardAchievement.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_green_border
                    )
            );

            binding.cardAchievementIcon.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_surface
                    )
            );

            binding.cardAchievementIcon.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_green_border
                    )
            );

            binding.textAchievementIcon.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_success
                    )
            );

            binding.cardAchievementStatus.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_surface
                    )
            );

            binding.cardAchievementStatus.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_green_border
                    )
            );

            binding.textAchievementStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_success
                    )
            );

            binding.progressAchievement.setIndicatorColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_success
                    )
            );

            binding.progressAchievement.setTrackColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_surface
                    )
            );
        }

        private void showLockedState(
                @NonNull AchievementItem achievement
        ) {
            binding.textAchievementStatus.setText(
                    R.string.achievement_locked
            );

            binding.textAchievementProgress.setText(
                    binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.achievement_progress_format,
                                    Math.min(
                                            achievement.getCurrentValue(),
                                            achievement.getTargetValue()
                                    ),
                                    achievement.getTargetValue()
                            )
            );

            binding.cardAchievement.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_surface
                    )
            );

            binding.cardAchievement.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_outline
                    )
            );

            binding.cardAchievementIcon.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_surface_muted
                    )
            );

            binding.cardAchievementIcon.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_outline
                    )
            );

            binding.textAchievementIcon.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_text_muted
                    )
            );

            binding.cardAchievementStatus.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_surface_muted
                    )
            );

            binding.cardAchievementStatus.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_outline
                    )
            );

            binding.textAchievementStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_text_muted
                    )
            );

            binding.progressAchievement.setIndicatorColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_primary
                    )
            );

            binding.progressAchievement.setTrackColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            R.color.ss_blue_soft
                    )
            );
        }
    }
}