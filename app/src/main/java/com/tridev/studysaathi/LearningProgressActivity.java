package com.tridev.studysaathi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.AchievementAdapter;
import com.tridev.studysaathi.adapter.QuizHistoryAdapter;
import com.tridev.studysaathi.adapter.SubjectProgressAdapter;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.catalog.SubjectCatalog;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.LessonProgressRepository;
import com.tridev.studysaathi.data.repository.QuizAttemptRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityLearningProgressBinding;
import com.tridev.studysaathi.model.AchievementItem;
import com.tridev.studysaathi.model.SubjectItem;
import com.tridev.studysaathi.model.SubjectProgressItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LearningProgressActivity
        extends AppCompatActivity {

    private static final String GOAL_PREFERENCES_NAME =
            "study_saathi_daily_goals";

    private static final String GOAL_KEY_PREFIX =
            "daily_goal_profile_";

    private static final int DEFAULT_DAILY_GOAL = 3;

    private ActivityLearningProgressBinding binding;

    private StudentProfileRepository studentProfileRepository;
    private LessonProgressRepository lessonProgressRepository;
    private QuizAttemptRepository quizAttemptRepository;

    private AchievementAdapter achievementAdapter;
    private SubjectProgressAdapter subjectProgressAdapter;
    private QuizHistoryAdapter quizHistoryAdapter;

    private SharedPreferences goalPreferences;

    private StudentProfileEntity activeStudentProfile;

    private int selectedDailyGoal =
            DEFAULT_DAILY_GOAL;

    private TodayActivitySummary todayActivitySummary =
            new TodayActivitySummary(0, 0, 0);

    private boolean updatingGoalSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLearningProgressBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        lessonProgressRepository =
                new LessonProgressRepository(this);

        quizAttemptRepository =
                new QuizAttemptRepository(this);

        goalPreferences = getSharedPreferences(
                GOAL_PREFERENCES_NAME,
                MODE_PRIVATE
        );

        setupRecyclerViews();
        setupClickListeners();
        setupDailyGoalControls();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadLearningProgress();
    }

    private void setupRecyclerViews() {
        achievementAdapter =
                new AchievementAdapter(
                        new ArrayList<>()
                );

        binding.recyclerAchievements.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerAchievements.setAdapter(
                achievementAdapter
        );

        binding.recyclerAchievements.setHasFixedSize(
                false
        );

        subjectProgressAdapter =
                new SubjectProgressAdapter(
                        new ArrayList<>()
                );

        binding.recyclerSubjectProgress.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerSubjectProgress.setAdapter(
                subjectProgressAdapter
        );

        binding.recyclerSubjectProgress.setHasFixedSize(
                false
        );

        quizHistoryAdapter =
                new QuizHistoryAdapter(
                        new ArrayList<>(),
                        this::openQuizAttempt
                );

        binding.recyclerQuizHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerQuizHistory.setAdapter(
                quizHistoryAdapter
        );

        binding.recyclerQuizHistory.setHasFixedSize(
                false
        );
    }
    private void openSmartStudyPlan() {
        Intent smartPlanIntent = new Intent(
                LearningProgressActivity.this,
                SmartStudyPlanActivity.class
        );

        startActivity(smartPlanIntent);
    }
    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.buttonBrowseSubjects.setOnClickListener(view ->
                openSubjects()
        );
        binding.cardDailyStudyGoal.setOnClickListener(view ->
                openSmartStudyPlan()
        );
    }

    private void setupDailyGoalControls() {
        binding.toggleDailyGoal.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked
                            || updatingGoalSelection
                            || activeStudentProfile == null) {
                        return;
                    }

                    int selectedGoal =
                            getGoalForButton(checkedId);

                    if (selectedGoal <= 0) {
                        return;
                    }

                    selectedDailyGoal =
                            selectedGoal;

                    saveDailyGoal(
                            activeStudentProfile.getProfileId(),
                            selectedDailyGoal
                    );

                    updateDailyGoalCard(
                            todayActivitySummary
                    );
                }
        );

        setGoalControlsEnabled(false);
    }

    private void loadLearningProgress() {
        showLoadingState(true);

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
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

                        loadDailyGoal(
                                studentProfile.getProfileId()
                        );

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
                                R.string.learning_progress_profile_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void loadDailyGoal(long profileId) {
        int savedGoal =
                goalPreferences.getInt(
                        GOAL_KEY_PREFIX + profileId,
                        DEFAULT_DAILY_GOAL
                );

        selectedDailyGoal =
                sanitizeDailyGoal(savedGoal);

        applyDailyGoalSelection();
        setGoalControlsEnabled(true);
    }

    private int sanitizeDailyGoal(int goal) {
        if (goal == 1 || goal == 3 || goal == 5) {
            return goal;
        }

        return DEFAULT_DAILY_GOAL;
    }

    private void saveDailyGoal(
            long profileId,
            int dailyGoal
    ) {
        goalPreferences.edit()
                .putInt(
                        GOAL_KEY_PREFIX + profileId,
                        sanitizeDailyGoal(dailyGoal)
                )
                .apply();
    }

    private void applyDailyGoalSelection() {
        updatingGoalSelection = true;

        binding.toggleDailyGoal.check(
                getButtonForGoal(selectedDailyGoal)
        );

        updatingGoalSelection = false;
    }

    @IdRes
    private int getButtonForGoal(int goal) {
        if (goal == 1) {
            return R.id.buttonGoalOne;
        }

        if (goal == 5) {
            return R.id.buttonGoalFive;
        }

        return R.id.buttonGoalThree;
    }

    private int getGoalForButton(
            @IdRes int checkedButtonId
    ) {
        if (checkedButtonId == R.id.buttonGoalOne) {
            return 1;
        }

        if (checkedButtonId == R.id.buttonGoalFive) {
            return 5;
        }

        if (checkedButtonId == R.id.buttonGoalThree) {
            return 3;
        }

        return -1;
    }

    private void setGoalControlsEnabled(boolean enabled) {
        binding.buttonGoalOne.setEnabled(enabled);
        binding.buttonGoalThree.setEnabled(enabled);
        binding.buttonGoalFive.setEnabled(enabled);
    }

    private void showStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textProgressStudent.setText(
                getString(
                        R.string.learning_progress_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );
    }

    private void loadLessonProgress(
            @NonNull StudentProfileEntity studentProfile
    ) {
        lessonProgressRepository.getProgressForProfile(
                studentProfile.getProfileId(),
                new LessonProgressRepository.ProgressListCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<LessonProgressEntity> progressList
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

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.learning_progress_lessons_failed,
                                Snackbar.LENGTH_LONG
                        ).show();

                        loadQuizAttempts(
                                studentProfile,
                                new ArrayList<>()
                        );
                    }
                }
        );
    }

    private void loadQuizAttempts(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> lessonProgressList
    ) {
        quizAttemptRepository.getProfileAttempts(
                studentProfile.getProfileId(),
                new QuizAttemptRepository.QuizAttemptListCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<QuizAttemptEntity> quizAttempts
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);

                        showProgressSummary(
                                lessonProgressList,
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

                        showProgressSummary(
                                lessonProgressList,
                                new ArrayList<>()
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.learning_progress_quiz_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showProgressSummary(
            @NonNull List<LessonProgressEntity> lessonProgressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        int completedLessons =
                calculateCompletedLessons(
                        lessonProgressList
                );

        int totalRevisions =
                calculateTotalRevisions(
                        lessonProgressList
                );

        int totalAttempts =
                quizAttempts.size();

        int averageScore =
                calculateAverageScore(
                        quizAttempts
                );

        int bestScore =
                calculateBestScore(
                        quizAttempts
                );

        binding.textCompletedLessonsValue.setText(
                String.valueOf(completedLessons)
        );

        binding.textRevisionCountValue.setText(
                String.valueOf(totalRevisions)
        );

        binding.textQuizAttemptsValue.setText(
                String.valueOf(totalAttempts)
        );

        binding.textAverageScoreValue.setText(
                getString(
                        R.string.progress_percentage_format,
                        averageScore
                )
        );

        binding.textBestScoreValue.setText(
                getString(
                        R.string.progress_percentage_format,
                        bestScore
                )
        );

        todayActivitySummary =
                calculateTodayActivitySummary(
                        lessonProgressList,
                        quizAttempts
                );

        updateDailyGoalCard(
                todayActivitySummary
        );

        showCoachInsight(
                completedLessons,
                totalAttempts,
                averageScore
        );

        showAchievements(
                buildAchievements(
                        completedLessons,
                        totalRevisions,
                        totalAttempts,
                        bestScore
                )
        );

        showSubjectAnalytics(
                buildSubjectProgressItems(
                        lessonProgressList,
                        quizAttempts
                )
        );

        showQuizHistory(quizAttempts);
    }

    @NonNull
    private TodayActivitySummary calculateTodayActivitySummary(
            @NonNull List<LessonProgressEntity> lessonProgressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        int lessonsToday = 0;
        int revisionsToday = 0;
        int quizzesToday = 0;

        for (LessonProgressEntity progress
                : lessonProgressList) {

            if (progress.isCompleted()
                    && isTimestampToday(
                    progress.getCompletedAt()
            )) {
                lessonsToday++;
            }

            if (progress.getRevisionCount() > 0
                    && isTimestampToday(
                    progress.getLastRevisedAt()
            )) {
                revisionsToday++;
            }
        }

        for (QuizAttemptEntity quizAttempt
                : quizAttempts) {

            if (isTimestampToday(
                    quizAttempt.getAttemptedAt()
            )) {
                quizzesToday++;
            }
        }

        return new TodayActivitySummary(
                lessonsToday,
                revisionsToday,
                quizzesToday
        );
    }

    private boolean isTimestampToday(long timestamp) {
        if (timestamp <= 0L) {
            return false;
        }

        ZoneId zoneId =
                ZoneId.systemDefault();

        LocalDate activityDate =
                Instant.ofEpochMilli(timestamp)
                        .atZone(zoneId)
                        .toLocalDate();

        return activityDate.equals(
                LocalDate.now(zoneId)
        );
    }

    private void updateDailyGoalCard(
            @NonNull TodayActivitySummary summary
    ) {
        int completedActions =
                summary.getTotalActions();

        int progressValue =
                Math.min(
                        completedActions,
                        selectedDailyGoal
                );

        binding.progressTodayGoal.setMax(
                selectedDailyGoal
        );

        binding.progressTodayGoal.setProgressCompat(
                progressValue,
                true
        );

        binding.textTodayGoalValue.setText(
                getString(
                        R.string.daily_goal_value_format,
                        completedActions,
                        selectedDailyGoal
                )
        );

        binding.textTodayGoalBreakdown.setText(
                getString(
                        R.string.daily_goal_breakdown_format,
                        summary.getLessonsCompleted(),
                        summary.getRevisionsCompleted(),
                        summary.getQuizAttempts()
                )
        );

        if (completedActions == 0) {
            binding.textTodayGoalStatus.setText(
                    R.string.daily_goal_start_message
            );

            return;
        }

        if (completedActions < selectedDailyGoal) {
            binding.textTodayGoalStatus.setText(
                    getString(
                            R.string.daily_goal_remaining_format,
                            selectedDailyGoal
                                    - completedActions
                    )
            );

            return;
        }

        if (completedActions == selectedDailyGoal) {
            binding.textTodayGoalStatus.setText(
                    R.string.daily_goal_completed_message
            );

            return;
        }

        binding.textTodayGoalStatus.setText(
                getString(
                        R.string.daily_goal_exceeded_format,
                        completedActions
                                - selectedDailyGoal
                )
        );
    }

    @NonNull
    private List<AchievementItem> buildAchievements(
            int completedLessons,
            int totalRevisions,
            int totalAttempts,
            int bestScore
    ) {
        List<AchievementItem> achievements =
                new ArrayList<>();

        achievements.add(
                new AchievementItem(
                        "1",
                        getString(
                                R.string.achievement_first_step_title
                        ),
                        getString(
                                R.string.achievement_first_step_description
                        ),
                        completedLessons >= 1,
                        completedLessons,
                        1
                )
        );

        achievements.add(
                new AchievementItem(
                        "L",
                        getString(
                                R.string.achievement_lesson_explorer_title
                        ),
                        getString(
                                R.string.achievement_lesson_explorer_description
                        ),
                        completedLessons >= 5,
                        completedLessons,
                        5
                )
        );

        achievements.add(
                new AchievementItem(
                        "R",
                        getString(
                                R.string.achievement_revision_starter_title
                        ),
                        getString(
                                R.string.achievement_revision_starter_description
                        ),
                        totalRevisions >= 1,
                        totalRevisions,
                        1
                )
        );

        achievements.add(
                new AchievementItem(
                        "Q",
                        getString(
                                R.string.achievement_quiz_challenger_title
                        ),
                        getString(
                                R.string.achievement_quiz_challenger_description
                        ),
                        totalAttempts >= 3,
                        totalAttempts,
                        3
                )
        );

        achievements.add(
                new AchievementItem(
                        "★",
                        getString(
                                R.string.achievement_high_scorer_title
                        ),
                        getString(
                                R.string.achievement_high_scorer_description
                        ),
                        bestScore >= 80,
                        bestScore,
                        80
                )
        );

        achievements.add(
                new AchievementItem(
                        "C",
                        getString(
                                R.string.achievement_revision_champion_title
                        ),
                        getString(
                                R.string.achievement_revision_champion_description
                        ),
                        totalRevisions >= 5,
                        totalRevisions,
                        5
                )
        );

        return achievements;
    }

    private void showAchievements(
            @NonNull List<AchievementItem> achievements
    ) {
        achievementAdapter.submitList(achievements);

        boolean achievementsAvailable =
                !achievements.isEmpty();

        binding.recyclerAchievements.setVisibility(
                achievementsAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptyAchievements.setVisibility(
                achievementsAvailable
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    @NonNull
    private List<SubjectProgressItem> buildSubjectProgressItems(
            @NonNull List<LessonProgressEntity> lessonProgressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        List<SubjectProgressItem> subjectItems =
                new ArrayList<>();

        if (activeStudentProfile == null) {
            return subjectItems;
        }

        Set<String> subjectNames =
                new LinkedHashSet<>();

        List<SubjectItem> catalogSubjects =
                SubjectCatalog.getSubjectsForClass(
                        activeStudentProfile.getStudentClass()
                );

        for (SubjectItem subjectItem : catalogSubjects) {
            String subjectName =
                    subjectItem.getSubjectName();

            if (subjectName != null
                    && !subjectName.trim().isEmpty()) {

                subjectNames.add(
                        subjectName.trim()
                );
            }
        }

        for (LessonProgressEntity progress
                : lessonProgressList) {

            if (progress.getSubjectName() != null
                    && !progress.getSubjectName()
                    .trim()
                    .isEmpty()) {

                addSubjectIfMissing(
                        subjectNames,
                        progress.getSubjectName()
                );
            }
        }

        for (QuizAttemptEntity quizAttempt
                : quizAttempts) {

            if (quizAttempt.getSubjectName() != null
                    && !quizAttempt.getSubjectName()
                    .trim()
                    .isEmpty()) {

                addSubjectIfMissing(
                        subjectNames,
                        quizAttempt.getSubjectName()
                );
            }
        }

        for (String subjectName : subjectNames) {
            int completedLessons = 0;
            int revisionCount = 0;
            int quizAttemptCount = 0;
            int totalQuizPercentage = 0;

            for (LessonProgressEntity progress
                    : lessonProgressList) {

                if (!sameSubject(
                        subjectName,
                        progress.getSubjectName()
                )) {
                    continue;
                }

                if (progress.isCompleted()) {
                    completedLessons++;
                }

                revisionCount += Math.max(
                        0,
                        progress.getRevisionCount()
                );
            }

            for (QuizAttemptEntity quizAttempt
                    : quizAttempts) {

                if (!sameSubject(
                        subjectName,
                        quizAttempt.getSubjectName()
                )) {
                    continue;
                }

                quizAttemptCount++;

                totalQuizPercentage +=
                        quizAttempt.getPercentage();
            }

            int averageQuizScore =
                    quizAttemptCount == 0
                            ? 0
                            : Math.round(
                            totalQuizPercentage
                            / (float) quizAttemptCount
                    );

            int totalLessons =
                    ChapterCatalog.getChapters(
                            activeStudentProfile
                                    .getEducationBoard(),
                            activeStudentProfile
                                    .getStudentClass(),
                            subjectName
                    ).size();

            if (totalLessons < completedLessons) {
                totalLessons = completedLessons;
            }

            subjectItems.add(
                    new SubjectProgressItem(
                            subjectName,
                            completedLessons,
                            totalLessons,
                            revisionCount,
                            quizAttemptCount,
                            averageQuizScore
                    )
            );
        }

        return subjectItems;
    }

    private void addSubjectIfMissing(
            @NonNull Set<String> subjectNames,
            @NonNull String candidateSubject
    ) {
        String trimmedCandidate =
                candidateSubject.trim();

        for (String existingSubject : subjectNames) {
            if (sameSubject(
                    existingSubject,
                    trimmedCandidate
            )) {
                return;
            }
        }

        subjectNames.add(trimmedCandidate);
    }

    private boolean sameSubject(
            String firstSubject,
            String secondSubject
    ) {
        return normalizeText(firstSubject)
                .equals(
                        normalizeText(secondSubject)
                );
    }

    private int calculateCompletedLessons(
            @NonNull List<LessonProgressEntity> progressList
    ) {
        int completedCount = 0;

        for (LessonProgressEntity progress : progressList) {
            if (progress.isCompleted()) {
                completedCount++;
            }
        }

        return completedCount;
    }

    private int calculateTotalRevisions(
            @NonNull List<LessonProgressEntity> progressList
    ) {
        int revisionCount = 0;

        for (LessonProgressEntity progress : progressList) {
            revisionCount += Math.max(
                    0,
                    progress.getRevisionCount()
            );
        }

        return revisionCount;
    }

    private int calculateAverageScore(
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        if (quizAttempts.isEmpty()) {
            return 0;
        }

        int totalPercentage = 0;

        for (QuizAttemptEntity attempt : quizAttempts) {
            totalPercentage += attempt.getPercentage();
        }

        return Math.round(
                totalPercentage
                        / (float) quizAttempts.size()
        );
    }

    private int calculateBestScore(
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        int bestScore = 0;

        for (QuizAttemptEntity attempt : quizAttempts) {
            bestScore = Math.max(
                    bestScore,
                    attempt.getPercentage()
            );
        }

        return bestScore;
    }

    private void showCoachInsight(
            int completedLessons,
            int totalAttempts,
            int averageScore
    ) {
        if (activeStudentProfile == null) {
            binding.textCoachInsight.setText(
                    R.string.learning_progress_default_insight
            );

            return;
        }

        String studentName =
                activeStudentProfile.getStudentName();

        if (completedLessons == 0) {
            binding.textCoachInsight.setText(
                    getString(
                            R.string.learning_insight_start_format,
                            studentName
                    )
            );

            return;
        }

        if (totalAttempts == 0) {
            binding.textCoachInsight.setText(
                    getString(
                            R.string.learning_insight_quiz_format,
                            studentName,
                            completedLessons
                    )
            );

            return;
        }

        if (averageScore >= 80) {
            binding.textCoachInsight.setText(
                    getString(
                            R.string.learning_insight_excellent_format,
                            studentName,
                            averageScore
                    )
            );
        } else if (averageScore >= 50) {
            binding.textCoachInsight.setText(
                    getString(
                            R.string.learning_insight_good_format,
                            studentName,
                            averageScore
                    )
            );
        } else {
            binding.textCoachInsight.setText(
                    getString(
                            R.string.learning_insight_practise_format,
                            studentName,
                            averageScore
                    )
            );
        }
    }

    private void showSubjectAnalytics(
            @NonNull List<SubjectProgressItem> subjectItems
    ) {
        subjectProgressAdapter.submitList(
                subjectItems
        );

        boolean subjectsAvailable =
                !subjectItems.isEmpty();

        binding.recyclerSubjectProgress.setVisibility(
                subjectsAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptySubjectProgress.setVisibility(
                subjectsAvailable
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private void showQuizHistory(
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        quizHistoryAdapter.submitList(quizAttempts);

        boolean historyAvailable =
                !quizAttempts.isEmpty();

        binding.recyclerQuizHistory.setVisibility(
                historyAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptyQuizHistory.setVisibility(
                historyAvailable
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private void showNoProfileState() {
        activeStudentProfile = null;

        selectedDailyGoal =
                DEFAULT_DAILY_GOAL;

        todayActivitySummary =
                new TodayActivitySummary(0, 0, 0);

        binding.textProgressStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.textCompletedLessonsValue.setText("0");
        binding.textRevisionCountValue.setText("0");
        binding.textQuizAttemptsValue.setText("0");
        binding.textAverageScoreValue.setText("0%");
        binding.textBestScoreValue.setText("0%");

        binding.textCoachInsight.setText(
                R.string.learning_progress_default_insight
        );

        updatingGoalSelection = true;
        binding.toggleDailyGoal.clearChecked();
        updatingGoalSelection = false;

        setGoalControlsEnabled(false);

        binding.textTodayGoalValue.setText(
                getString(
                        R.string.daily_goal_value_format,
                        0,
                        DEFAULT_DAILY_GOAL
                )
        );

        binding.textTodayGoalBreakdown.setText(
                getString(
                        R.string.daily_goal_breakdown_format,
                        0,
                        0,
                        0
                )
        );

        binding.textTodayGoalStatus.setText(
                R.string.daily_goal_unavailable
        );

        binding.progressTodayGoal.setMax(
                DEFAULT_DAILY_GOAL
        );

        binding.progressTodayGoal.setProgressCompat(
                0,
                false
        );

        showAchievements(new ArrayList<>());
        showSubjectAnalytics(new ArrayList<>());
        showQuizHistory(new ArrayList<>());
    }

    private void openQuizAttempt(
            @NonNull QuizAttemptEntity quizAttempt
    ) {
        Intent practiceIntent = new Intent(
                LearningProgressActivity.this,
                PracticeActivity.class
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_SUBJECT_NAME,
                quizAttempt.getSubjectName()
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_CHAPTER_TITLE,
                quizAttempt.getChapterTitle()
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_STUDENT_CLASS,
                quizAttempt.getStudentClass()
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_EDUCATION_BOARD,
                quizAttempt.getEducationBoard()
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_LANGUAGE_MODE,
                "BILINGUAL"
        );

        startActivity(practiceIntent);
    }

    private void openSubjects() {
        Intent subjectsIntent = new Intent(
                LearningProgressActivity.this,
                SubjectsActivity.class
        );

        startActivity(subjectsIntent);
    }

    @NonNull
    private String normalizeText(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void showLoadingState(boolean loading) {
        binding.progressLearningProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.contentLearningProgress.setVisibility(
                loading ? View.INVISIBLE : View.VISIBLE
        );
    }

    private static class TodayActivitySummary {

        private final int lessonsCompleted;
        private final int revisionsCompleted;
        private final int quizAttempts;

        TodayActivitySummary(
                int lessonsCompleted,
                int revisionsCompleted,
                int quizAttempts
        ) {
            this.lessonsCompleted =
                    Math.max(0, lessonsCompleted);

            this.revisionsCompleted =
                    Math.max(0, revisionsCompleted);

            this.quizAttempts =
                    Math.max(0, quizAttempts);
        }

        int getLessonsCompleted() {
            return lessonsCompleted;
        }

        int getRevisionsCompleted() {
            return revisionsCompleted;
        }

        int getQuizAttempts() {
            return quizAttempts;
        }

        int getTotalActions() {
            return lessonsCompleted
                    + revisionsCompleted
                    + quizAttempts;
        }
    }
}