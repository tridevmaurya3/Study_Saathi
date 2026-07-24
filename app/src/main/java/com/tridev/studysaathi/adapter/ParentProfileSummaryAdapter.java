package com.tridev.studysaathi.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.databinding.ItemParentProfileSummaryBinding;
import com.tridev.studysaathi.model.ParentProfileSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParentProfileSummaryAdapter
        extends RecyclerView.Adapter<
        ParentProfileSummaryAdapter.ParentProfileViewHolder> {

    public interface OnProfileOpenListener {

        void onProfileOpen(
                @NonNull ParentProfileSummary profileSummary
        );
    }

    private final List<ParentProfileSummary> profileSummaries =
            new ArrayList<>();

    @NonNull
    private final OnProfileOpenListener profileOpenListener;

    public ParentProfileSummaryAdapter(
            @NonNull List<ParentProfileSummary> initialItems,
            @NonNull OnProfileOpenListener profileOpenListener
    ) {
        profileSummaries.addAll(initialItems);
        this.profileOpenListener = profileOpenListener;
    }

    @NonNull
    @Override
    public ParentProfileViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemParentProfileSummaryBinding binding =
                ItemParentProfileSummaryBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new ParentProfileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ParentProfileViewHolder holder,
            int position
    ) {
        holder.bind(
                profileSummaries.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return profileSummaries.size();
    }

    public void submitList(
            @NonNull List<ParentProfileSummary> updatedItems
    ) {
        profileSummaries.clear();
        profileSummaries.addAll(updatedItems);
        notifyDataSetChanged();
    }

    class ParentProfileViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemParentProfileSummaryBinding binding;

        ParentProfileViewHolder(
                @NonNull ItemParentProfileSummaryBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull ParentProfileSummary profileSummary
        ) {
            Context context =
                    binding.getRoot().getContext();

            binding.textParentInitial.setText(
                    getStudentInitial(
                            profileSummary.getStudentName()
                    )
            );

            binding.textParentName.setText(
                    profileSummary.getStudentName()
            );

            binding.textParentDetails.setText(
                    context.getString(
                            R.string.parent_profile_details_format,
                            profileSummary.getEducationBoard(),
                            profileSummary.getStudentClass()
                    )
            );

            binding.textParentActiveBadge.setVisibility(
                    profileSummary.isActiveProfile()
                            ? View.VISIBLE
                            : View.GONE
            );

            binding.textParentCompletedLessons.setText(
                    String.valueOf(
                            profileSummary.getCompletedLessons()
                    )
            );

            binding.textParentRevisions.setText(
                    String.valueOf(
                            profileSummary.getRevisionCount()
                    )
            );

            binding.textParentQuizAttempts.setText(
                    String.valueOf(
                            profileSummary.getQuizAttemptCount()
                    )
            );

            if (profileSummary.hasQuizData()) {
                binding.textParentBestScore.setText(
                        context.getString(
                                R.string.parent_percentage_format,
                                profileSummary.getBestQuizScore()
                        )
                );

                binding.textParentLatestScore.setText(
                        context.getString(
                                R.string.parent_latest_score_format,
                                profileSummary.getLatestQuizScore()
                        )
                );
            } else {
                binding.textParentBestScore.setText("—");

                binding.textParentLatestScore.setText(
                        R.string.parent_no_quiz_attempt
                );
            }

            binding.textParentStreak.setText(
                    context.getString(
                            R.string.parent_streak_format,
                            profileSummary.getCurrentStreak()
                    )
            );

            binding.progressParentDailyGoal.setMax(
                    Math.max(
                            1,
                            profileSummary.getDailyGoal()
                    )
            );

            binding.progressParentDailyGoal.setProgressCompat(
                    Math.min(
                            profileSummary.getTodayActions(),
                            profileSummary.getDailyGoal()
                    ),
                    false
            );

            binding.textParentDailyGoal.setText(
                    context.getString(
                            R.string.parent_daily_goal_format,
                            profileSummary.getTodayActions(),
                            profileSummary.getDailyGoal()
                    )
            );

            binding.textParentLastActivity.setText(
                    createLastActivityText(
                            context,
                            profileSummary.getLastActivityAt()
                    )
            );

            showWeakChapter(
                    context,
                    profileSummary
            );

            binding.buttonOpenParentProfile.setText(
                    profileSummary.isActiveProfile()
                            ? R.string.parent_view_progress
                            : R.string.parent_activate_and_view
            );

            binding.buttonOpenParentProfile.setOnClickListener(view ->
                    profileOpenListener.onProfileOpen(
                            profileSummary
                    )
            );
        }

        private void showWeakChapter(
                @NonNull Context context,
                @NonNull ParentProfileSummary profileSummary
        ) {
            if (!profileSummary.hasWeakChapter()) {
                binding.textParentWeakChapter.setText(
                        R.string.parent_weak_chapter_unavailable
                );

                return;
            }

            binding.textParentWeakChapter.setText(
                    context.getString(
                            R.string.parent_weak_chapter_format,
                            profileSummary.getWeakSubject(),
                            profileSummary.getWeakChapter(),
                            profileSummary.getWeakChapterScore()
                    )
            );
        }

        @NonNull
        private String createLastActivityText(
                @NonNull Context context,
                long timestamp
        ) {
            if (timestamp <= 0L) {
                return context.getString(
                        R.string.parent_no_study_activity
                );
            }

            ZoneId zoneId =
                    ZoneId.systemDefault();

            LocalDate activityDate =
                    Instant.ofEpochMilli(timestamp)
                            .atZone(zoneId)
                            .toLocalDate();

            LocalDate today =
                    LocalDate.now(zoneId);

            DateTimeFormatter timeFormatter =
                    DateTimeFormatter.ofPattern(
                            "hh:mm a",
                            Locale.getDefault()
                    );

            String timeText =
                    Instant.ofEpochMilli(timestamp)
                            .atZone(zoneId)
                            .format(timeFormatter);

            if (activityDate.equals(today)) {
                return context.getString(
                        R.string.parent_last_activity_today_format,
                        timeText
                );
            }

            if (activityDate.equals(
                    today.minusDays(1)
            )) {
                return context.getString(
                        R.string.parent_last_activity_yesterday_format,
                        timeText
                );
            }

            DateTimeFormatter dateFormatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy",
                            Locale.getDefault()
                    );

            return context.getString(
                    R.string.parent_last_activity_date_format,
                    activityDate.format(dateFormatter),
                    timeText
            );
        }

        @NonNull
        private String getStudentInitial(
                @NonNull String studentName
        ) {
            String cleanName =
                    studentName.trim();

            if (cleanName.isEmpty()) {
                return "S";
            }

            return cleanName.substring(0, 1)
                    .toUpperCase(Locale.getDefault());
        }
    }
}