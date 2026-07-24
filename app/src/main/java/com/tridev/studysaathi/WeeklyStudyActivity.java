package com.tridev.studysaathi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.StudyWeekAdapter;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.LessonProgressRepository;
import com.tridev.studysaathi.data.repository.QuizAttemptRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityWeeklyStudyBinding;
import com.tridev.studysaathi.model.StudyDayItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WeeklyStudyActivity
        extends AppCompatActivity {

    private static final String GOAL_PREFERENCES_NAME =
            "study_saathi_daily_goals";

    private static final String GOAL_KEY_PREFIX =
            "daily_goal_profile_";

    private static final int DEFAULT_DAILY_GOAL = 3;
    private static final int WEEK_DAYS = 7;

    private ActivityWeeklyStudyBinding binding;

    private StudentProfileRepository
            studentProfileRepository;

    private LessonProgressRepository
            lessonProgressRepository;

    private QuizAttemptRepository
            quizAttemptRepository;

    private SharedPreferences goalPreferences;

    private StudyWeekAdapter studyWeekAdapter;

    private StudentProfileEntity activeStudentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityWeeklyStudyBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        lessonProgressRepository =
                new LessonProgressRepository(this);

        quizAttemptRepository =
                new QuizAttemptRepository(this);

        goalPreferences =
                getSharedPreferences(
                        GOAL_PREFERENCES_NAME,
                        MODE_PRIVATE
                );

        setupRecyclerView();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadWeeklyStudyData();
    }

    private void setupRecyclerView() {
        studyWeekAdapter =
                new StudyWeekAdapter(
                        new ArrayList<>()
                );

        binding.recyclerStudyWeek.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerStudyWeek.setAdapter(
                studyWeekAdapter
        );

        binding.recyclerStudyWeek.setHasFixedSize(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonOpenSmartPlan.setOnClickListener(view ->
                openSmartStudyPlan()
        );
    }

    private void loadWeeklyStudyData() {
        showLoadingState(true);

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository
                        .SingleProfileCallback() {

                    @Override
                    public void onSuccess(
                            StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (studentProfile == null) {
                            showLoadingState(false);
                            showNoProfileState();
                            return;
                        }

                        activeStudentProfile =
                                studentProfile;

                        showStudentInformation(
                                studentProfile
                        );

                        loadLessonProgress(
                                studentProfile
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);
                        showNoProfileState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.weekly_study_profile_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textWeeklyStudyStudent.setText(
                getString(
                        R.string.weekly_study_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );

        binding.buttonOpenSmartPlan.setEnabled(true);
    }

    private void loadLessonProgress(
            @NonNull StudentProfileEntity studentProfile
    ) {
        lessonProgressRepository.getProgressForProfile(
                studentProfile.getProfileId(),
                new LessonProgressRepository
                        .ProgressListCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<LessonProgressEntity>
                                    progressList
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        loadQuizAttempts(
                                studentProfile,
                                progressList
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        loadQuizAttempts(
                                studentProfile,
                                new ArrayList<>()
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.weekly_study_progress_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void loadQuizAttempts(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> progressList
    ) {
        quizAttemptRepository.getProfileAttempts(
                studentProfile.getProfileId(),
                new QuizAttemptRepository
                        .QuizAttemptListCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<QuizAttemptEntity>
                                    quizAttempts
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);

                        showWeeklyStudyData(
                                studentProfile,
                                progressList,
                                quizAttempts
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);

                        showWeeklyStudyData(
                                studentProfile,
                                progressList,
                                new ArrayList<>()
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.weekly_study_quiz_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showWeeklyStudyData(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        int dailyGoal =
                getDailyGoal(
                        studentProfile.getProfileId()
                );

        List<StudyDayItem> studyDays =
                createStudyWeek(
                        progressList,
                        quizAttempts,
                        dailyGoal
                );

        studyWeekAdapter.submitList(
                studyDays
        );

        showWeeklySummary(
                studyDays,
                progressList,
                quizAttempts,
                dailyGoal
        );

        binding.cardWeeklyEmpty.setVisibility(
                hasAnyActivity(studyDays)
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    @NonNull
    private List<StudyDayItem> createStudyWeek(
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull List<QuizAttemptEntity> quizAttempts,
            int dailyGoal
    ) {
        List<StudyDayItem> studyDays =
                new ArrayList<>();

        ZoneId zoneId =
                ZoneId.systemDefault();

        LocalDate today =
                LocalDate.now(zoneId);

        DateTimeFormatter dayFormatter =
                DateTimeFormatter.ofPattern(
                        "EEE",
                        Locale.getDefault()
                );

        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern(
                        "dd MMM",
                        Locale.getDefault()
                );

        for (int dayOffset = WEEK_DAYS - 1;
             dayOffset >= 0;
             dayOffset--) {

            LocalDate requiredDate =
                    today.minusDays(dayOffset);

            int lessonCount =
                    countLessonsForDate(
                            progressList,
                            requiredDate,
                            zoneId
                    );

            int revisionCount =
                    countRevisionsForDate(
                            progressList,
                            requiredDate,
                            zoneId
                    );

            int quizCount =
                    countQuizzesForDate(
                            quizAttempts,
                            requiredDate,
                            zoneId
                    );

            studyDays.add(
                    new StudyDayItem(
                            requiredDate.format(
                                    dayFormatter
                            ),
                            requiredDate.format(
                                    dateFormatter
                            ),
                            lessonCount,
                            revisionCount,
                            quizCount,
                            dailyGoal,
                            requiredDate.equals(today)
                    )
            );
        }

        return studyDays;
    }

    private int countLessonsForDate(
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull LocalDate requiredDate,
            @NonNull ZoneId zoneId
    ) {
        int count = 0;

        for (LessonProgressEntity progress
                : progressList) {

            if (!progress.isCompleted()) {
                continue;
            }

            if (isTimestampOnDate(
                    progress.getCompletedAt(),
                    requiredDate,
                    zoneId
            )) {
                count++;
            }
        }

        return count;
    }

    private int countRevisionsForDate(
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull LocalDate requiredDate,
            @NonNull ZoneId zoneId
    ) {
        int count = 0;

        for (LessonProgressEntity progress
                : progressList) {

            if (progress.getRevisionCount() <= 0) {
                continue;
            }

            if (isTimestampOnDate(
                    progress.getLastRevisedAt(),
                    requiredDate,
                    zoneId
            )) {
                count++;
            }
        }

        return count;
    }

    private int countQuizzesForDate(
            @NonNull List<QuizAttemptEntity> quizAttempts,
            @NonNull LocalDate requiredDate,
            @NonNull ZoneId zoneId
    ) {
        int count = 0;

        for (QuizAttemptEntity quizAttempt
                : quizAttempts) {

            if (isTimestampOnDate(
                    quizAttempt.getAttemptedAt(),
                    requiredDate,
                    zoneId
            )) {
                count++;
            }
        }

        return count;
    }

    private boolean isTimestampOnDate(
            long timestamp,
            @NonNull LocalDate requiredDate,
            @NonNull ZoneId zoneId
    ) {
        if (timestamp <= 0L) {
            return false;
        }

        LocalDate activityDate =
                Instant.ofEpochMilli(timestamp)
                        .atZone(zoneId)
                        .toLocalDate();

        return activityDate.equals(
                requiredDate
        );
    }

    private void showWeeklySummary(
            @NonNull List<StudyDayItem> studyDays,
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull List<QuizAttemptEntity> quizAttempts,
            int dailyGoal
    ) {
        int totalActions = 0;
        int activeDays = 0;
        int completedGoalDays = 0;

        StudyDayItem bestDay = null;

        for (StudyDayItem studyDay : studyDays) {
            totalActions +=
                    studyDay.getTotalActions();

            if (studyDay.hasStudyActivity()) {
                activeDays++;
            }

            if (studyDay.isGoalCompleted()) {
                completedGoalDays++;
            }

            if (bestDay == null
                    || studyDay.getTotalActions()
                    > bestDay.getTotalActions()) {
                bestDay = studyDay;
            }
        }

        int currentStreak =
                calculateCurrentStreak(
                        progressList,
                        quizAttempts
                );

        int weeklyTarget =
                dailyGoal * WEEK_DAYS;

        binding.textWeeklyActionsValue.setText(
                String.valueOf(totalActions)
        );

        binding.textWeeklyActiveDaysValue.setText(
                getString(
                        R.string.weekly_study_active_days_value_format,
                        activeDays,
                        WEEK_DAYS
                )
        );

        binding.textWeeklyStreakValue.setText(
                getString(
                        R.string.weekly_study_streak_value_format,
                        currentStreak
                )
        );

        binding.textWeeklyGoalDaysValue.setText(
                getString(
                        R.string.weekly_study_goal_days_value_format,
                        completedGoalDays,
                        WEEK_DAYS
                )
        );

        binding.progressWeeklyTarget.setMax(
                weeklyTarget
        );

        binding.progressWeeklyTarget.setProgressCompat(
                Math.min(
                        totalActions,
                        weeklyTarget
                ),
                true
        );

        binding.textWeeklyTargetStatus.setText(
                getString(
                        R.string.weekly_study_target_status_format,
                        totalActions,
                        weeklyTarget
                )
        );

        showBestDay(
                bestDay
        );

        showCoachMessage(
                totalActions,
                activeDays,
                currentStreak,
                completedGoalDays
        );
    }

    private void showBestDay(
            StudyDayItem bestDay
    ) {
        if (bestDay == null
                || !bestDay.hasStudyActivity()) {

            binding.textWeeklyBestDay.setText(
                    R.string.weekly_study_no_best_day
            );

            return;
        }

        binding.textWeeklyBestDay.setText(
                getString(
                        R.string.weekly_study_best_day_format,
                        bestDay.getDayName(),
                        bestDay.getDateText(),
                        bestDay.getTotalActions()
                )
        );
    }

    private void showCoachMessage(
            int totalActions,
            int activeDays,
            int currentStreak,
            int completedGoalDays
    ) {
        if (totalActions == 0) {
            binding.textWeeklyCoachMessage.setText(
                    R.string.weekly_study_coach_start
            );

            return;
        }

        if (completedGoalDays >= 5) {
            binding.textWeeklyCoachMessage.setText(
                    R.string.weekly_study_coach_excellent
            );

            return;
        }

        if (currentStreak >= 3) {
            binding.textWeeklyCoachMessage.setText(
                    getString(
                            R.string.weekly_study_coach_streak_format,
                            currentStreak
                    )
            );

            return;
        }

        if (activeDays >= 4) {
            binding.textWeeklyCoachMessage.setText(
                    R.string.weekly_study_coach_consistent
            );

            return;
        }

        binding.textWeeklyCoachMessage.setText(
                R.string.weekly_study_coach_continue
        );
    }

    private int calculateCurrentStreak(
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        Set<LocalDate> activityDates =
                new HashSet<>();

        ZoneId zoneId =
                ZoneId.systemDefault();

        for (LessonProgressEntity progress
                : progressList) {

            addTimestampDate(
                    activityDates,
                    progress.getCompletedAt(),
                    zoneId
            );

            addTimestampDate(
                    activityDates,
                    progress.getLastRevisedAt(),
                    zoneId
            );
        }

        for (QuizAttemptEntity quizAttempt
                : quizAttempts) {

            addTimestampDate(
                    activityDates,
                    quizAttempt.getAttemptedAt(),
                    zoneId
            );
        }

        if (activityDates.isEmpty()) {
            return 0;
        }

        LocalDate today =
                LocalDate.now(zoneId);

        LocalDate streakDate;

        if (activityDates.contains(today)) {
            streakDate = today;
        } else if (activityDates.contains(
                today.minusDays(1)
        )) {
            streakDate = today.minusDays(1);
        } else {
            return 0;
        }

        int streakCount = 0;

        while (activityDates.contains(streakDate)) {
            streakCount++;
            streakDate = streakDate.minusDays(1);
        }

        return streakCount;
    }

    private void addTimestampDate(
            @NonNull Set<LocalDate> activityDates,
            long timestamp,
            @NonNull ZoneId zoneId
    ) {
        if (timestamp <= 0L) {
            return;
        }

        activityDates.add(
                Instant.ofEpochMilli(timestamp)
                        .atZone(zoneId)
                        .toLocalDate()
        );
    }

    private boolean hasAnyActivity(
            @NonNull List<StudyDayItem> studyDays
    ) {
        for (StudyDayItem studyDay : studyDays) {
            if (studyDay.hasStudyActivity()) {
                return true;
            }
        }

        return false;
    }

    private int getDailyGoal(long profileId) {
        int savedGoal =
                goalPreferences.getInt(
                        GOAL_KEY_PREFIX + profileId,
                        DEFAULT_DAILY_GOAL
                );

        if (savedGoal == 1
                || savedGoal == 3
                || savedGoal == 5) {
            return savedGoal;
        }

        return DEFAULT_DAILY_GOAL;
    }

    private void openSmartStudyPlan() {
        if (activeStudentProfile == null) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.weekly_study_profile_required,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        Intent smartPlanIntent = new Intent(
                WeeklyStudyActivity.this,
                SmartStudyPlanActivity.class
        );

        startActivity(smartPlanIntent);
    }

    private void showNoProfileState() {
        activeStudentProfile = null;

        studyWeekAdapter.submitList(
                new ArrayList<>()
        );

        binding.textWeeklyStudyStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.textWeeklyActionsValue.setText("0");
        binding.textWeeklyActiveDaysValue.setText("0 / 7");
        binding.textWeeklyStreakValue.setText("0 days");
        binding.textWeeklyGoalDaysValue.setText("0 / 7");

        binding.progressWeeklyTarget.setMax(21);
        binding.progressWeeklyTarget.setProgressCompat(
                0,
                false
        );

        binding.textWeeklyTargetStatus.setText(
                R.string.weekly_study_profile_required
        );

        binding.textWeeklyBestDay.setText(
                R.string.weekly_study_no_best_day
        );

        binding.textWeeklyCoachMessage.setText(
                R.string.weekly_study_profile_required
        );

        binding.cardWeeklyEmpty.setVisibility(
                View.VISIBLE
        );

        binding.textWeeklyEmptyTitle.setText(
                R.string.weekly_study_profile_required_title
        );

        binding.textWeeklyEmptyDescription.setText(
                R.string.weekly_study_profile_required
        );

        binding.buttonOpenSmartPlan.setEnabled(false);
    }

    private void showLoadingState(boolean loading) {
        binding.progressWeeklyStudy.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.contentWeeklyStudy.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }
}