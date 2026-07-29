package com.tridev.studysaathi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.catalog.SmartRecommendationEngine;
import com.tridev.studysaathi.data.content.policy.ChildSubjectVisibilityPolicy;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.ChildSchoolBookChapterRepository;
import com.tridev.studysaathi.data.repository.LessonProgressRepository;
import com.tridev.studysaathi.data.repository.QuizAttemptRepository;
import com.tridev.studysaathi.data.repository.SchoolSubjectRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivitySmartStudyPlanBinding;
import com.tridev.studysaathi.model.StudyRecommendation;
import com.tridev.studysaathi.navigation.ExactSchoolBookLessonContract;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SmartStudyPlanActivity
        extends AppCompatActivity {

    private static final String GOAL_PREFERENCES_NAME =
            "study_saathi_daily_goals";

    private static final String GOAL_KEY_PREFIX =
            "daily_goal_profile_";

    private static final int DEFAULT_DAILY_GOAL = 3;

    private ActivitySmartStudyPlanBinding binding;

    private StudentProfileRepository
            studentProfileRepository;

    private LessonProgressRepository
            lessonProgressRepository;

    private QuizAttemptRepository
            quizAttemptRepository;

    private SchoolSubjectRepository
            schoolSubjectRepository;

    private ChildSchoolBookChapterRepository
            childChapterRepository;

    private SharedPreferences goalPreferences;

    private StudentProfileEntity activeStudentProfile;

    private StudyRecommendation currentRecommendation =
            StudyRecommendation.empty();

    private long currentApprovedChapterRowId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivitySmartStudyPlanBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        lessonProgressRepository =
                new LessonProgressRepository(this);

        quizAttemptRepository =
                new QuizAttemptRepository(this);

        schoolSubjectRepository =
                new SchoolSubjectRepository(this);

        childChapterRepository =
                new ChildSchoolBookChapterRepository(this);

        goalPreferences = getSharedPreferences(
                GOAL_PREFERENCES_NAME,
                MODE_PRIVATE
        );

        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadSmartStudyPlan();
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonStartRecommendedLesson
                .setOnClickListener(view ->
                        openRecommendedLesson()
                );

        binding.buttonPracticeRecommended
                .setOnClickListener(view ->
                        openRecommendedPractice()
                );

        binding.buttonBrowseSubjects.setOnClickListener(view ->
                openSubjects()
        );
    }

    private void loadSmartStudyPlan() {
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
                                R.string.smart_plan_profile_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textPlanStudent.setText(
                getString(
                        R.string.smart_plan_student_format,
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
                                R.string.smart_plan_progress_failed,
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

                        showSmartPlan(
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

                        showSmartPlan(
                                studentProfile,
                                progressList,
                                new ArrayList<>()
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.smart_plan_quiz_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showSmartPlan(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        showDailyGoal(
                studentProfile,
                progressList,
                quizAttempts
        );

        StudyRecommendation calculatedRecommendation =
                SmartRecommendationEngine
                        .createRecommendation(
                                studentProfile,
                                progressList,
                                quizAttempts
                        );

        loadApprovedRecommendation(
                studentProfile,
                calculatedRecommendation
        );
    }

    private void loadApprovedRecommendation(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull StudyRecommendation calculatedRecommendation
    ) {
        schoolSubjectRepository.getSubjectsForProfile(
                studentProfile.getProfileId(),
                false,
                new SchoolSubjectRepository.SubjectsCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolSubjectEntity> subjects
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        List<SchoolSubjectEntity> visibleSubjects =
                                ChildSubjectVisibilityPolicy
                                        .filterVisibleSubjects(subjects);

                        findApprovedChapter(
                                visibleSubjects,
                                0,
                                calculatedRecommendation
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showNoRecommendationState();
                    }
                }
        );
    }

    private void findApprovedChapter(
            @NonNull List<SchoolSubjectEntity> subjects,
            int subjectIndex,
            @NonNull StudyRecommendation calculatedRecommendation
    ) {
        if (subjectIndex >= subjects.size()) {
            showNoRecommendationState();
            return;
        }

        SchoolSubjectEntity subject =
                subjects.get(subjectIndex);

        childChapterRepository.getChildChaptersForSubject(
                subject.getSubjectRowId(),
                new ChildSchoolBookChapterRepository
                        .ChildChaptersCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull ChildSchoolBookChapterRepository
                                    .ChildChapterResult result
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (!result.isAvailable()
                                || result.getChapters().isEmpty()) {

                            findApprovedChapter(
                                    subjects,
                                    subjectIndex + 1,
                                    calculatedRecommendation
                            );
                            return;
                        }

                        SchoolBookChapterEntity approvedChapter =
                                chooseApprovedChapter(
                                        result.getChapters(),
                                        calculatedRecommendation
                                );

                        currentRecommendation =
                                createApprovedRecommendation(
                                        subject,
                                        approvedChapter,
                                        calculatedRecommendation
                                );

                        currentApprovedChapterRowId =
                                approvedChapter.getChapterRowId();

                        showRecommendation(currentRecommendation);
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        findApprovedChapter(
                                subjects,
                                subjectIndex + 1,
                                calculatedRecommendation
                        );
                    }
                }
        );
    }

    @NonNull
    private SchoolBookChapterEntity chooseApprovedChapter(
            @NonNull List<SchoolBookChapterEntity> chapters,
            @NonNull StudyRecommendation calculatedRecommendation
    ) {
        String recommendedTitle =
                calculatedRecommendation
                        .getChapterTitle()
                        .trim();

        for (SchoolBookChapterEntity chapter : chapters) {
            if (chapter.getDisplayTitle()
                    .equalsIgnoreCase(recommendedTitle)) {

                return chapter;
            }
        }

        return chapters.get(0);
    }

    @NonNull
    private StudyRecommendation createApprovedRecommendation(
            @NonNull SchoolSubjectEntity subject,
            @NonNull SchoolBookChapterEntity chapter,
            @NonNull StudyRecommendation calculatedRecommendation
    ) {
        boolean exactRecommendation =
                subject.getSubjectNameEnglish().equalsIgnoreCase(
                        calculatedRecommendation.getSubjectName()
                )
                        && chapter.getDisplayTitle().equalsIgnoreCase(
                        calculatedRecommendation.getChapterTitle()
                );

        StudyRecommendation.RecommendationType type =
                exactRecommendation
                        ? calculatedRecommendation
                                .getRecommendationType()
                        : StudyRecommendation
                                .RecommendationType.NEXT_LESSON;

        int quizScore =
                exactRecommendation
                        ? calculatedRecommendation
                                .getQuizAverageScore()
                        : 0;

        return new StudyRecommendation(
                subject.getDisplayName(false),
                chapter.getDisplayTitle(),
                chapter.getChapterDescription(),
                type,
                quizScore
        );
    }

    private void showDailyGoal(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        int dailyGoal =
                goalPreferences.getInt(
                        GOAL_KEY_PREFIX
                                + studentProfile.getProfileId(),
                        DEFAULT_DAILY_GOAL
                );

        if (dailyGoal != 1
                && dailyGoal != 3
                && dailyGoal != 5) {

            dailyGoal = DEFAULT_DAILY_GOAL;
        }

        int lessonsToday = 0;
        int revisionsToday = 0;
        int quizzesToday = 0;

        for (LessonProgressEntity progress
                : progressList) {

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

        int totalActions =
                lessonsToday
                        + revisionsToday
                        + quizzesToday;

        binding.progressSmartPlanGoal.setMax(
                dailyGoal
        );

        binding.progressSmartPlanGoal
                .setProgressCompat(
                        Math.min(
                                totalActions,
                                dailyGoal
                        ),
                        true
                );

        binding.textSmartPlanGoalValue.setText(
                getString(
                        R.string.smart_plan_goal_format,
                        totalActions,
                        dailyGoal
                )
        );

        binding.textSmartPlanGoalBreakdown.setText(
                getString(
                        R.string.daily_goal_breakdown_format,
                        lessonsToday,
                        revisionsToday,
                        quizzesToday
                )
        );

        if (totalActions >= dailyGoal) {
            binding.textSmartPlanGoalStatus.setText(
                    R.string.smart_plan_goal_complete
            );
        } else {
            binding.textSmartPlanGoalStatus.setText(
                    getString(
                            R.string.smart_plan_goal_remaining_format,
                            dailyGoal - totalActions
                    )
            );
        }
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

    private void showRecommendation(
            @NonNull StudyRecommendation recommendation
    ) {
        if (!recommendation.hasRecommendation()) {
            showNoRecommendationState();
            return;
        }

        binding.cardRecommendation.setVisibility(
                View.VISIBLE
        );

        binding.cardNoRecommendation.setVisibility(
                View.GONE
        );

        binding.textRecommendedSubject.setText(
                recommendation.getSubjectName()
        );

        binding.textRecommendedChapter.setText(
                recommendation.getChapterTitle()
        );

        binding.textRecommendedDescription.setText(
                recommendation.getChapterDescription()
        );

        binding.buttonStartRecommendedLesson.setVisibility(
                View.VISIBLE
        );

        binding.buttonPracticeRecommended.setVisibility(
                View.VISIBLE
        );

        switch (recommendation.getRecommendationType()) {
            case LOW_QUIZ_SCORE:
                binding.textRecommendationTitle.setText(
                        R.string.smart_plan_strengthen_title
                );

                binding.textRecommendationReason.setText(
                        getString(
                                R.string.smart_plan_low_score_reason_format,
                                recommendation.getQuizAverageScore()
                        )
                );

                binding.buttonStartRecommendedLesson.setText(
                        R.string.smart_plan_review_lesson
                );

                binding.buttonPracticeRecommended.setText(
                        R.string.smart_plan_retry_quiz
                );
                break;

            case PRACTICE_AFTER_LESSON:
                binding.textRecommendationTitle.setText(
                        R.string.smart_plan_check_understanding_title
                );

                binding.textRecommendationReason.setText(
                        R.string.smart_plan_no_quiz_reason
                );

                binding.buttonStartRecommendedLesson.setText(
                        R.string.smart_plan_review_lesson
                );

                binding.buttonPracticeRecommended.setText(
                        R.string.smart_plan_start_quiz
                );
                break;

            case NEXT_LESSON:
                binding.textRecommendationTitle.setText(
                        R.string.smart_plan_next_lesson_title
                );

                binding.textRecommendationReason.setText(
                        R.string.smart_plan_next_lesson_reason
                );

                binding.buttonStartRecommendedLesson.setText(
                        R.string.smart_plan_start_lesson
                );

                binding.buttonPracticeRecommended.setText(
                        R.string.smart_plan_preview_quiz
                );
                break;

            case SMART_REVISION:
                binding.textRecommendationTitle.setText(
                        R.string.smart_plan_revision_title
                );

                binding.textRecommendationReason.setText(
                        R.string.smart_plan_revision_reason
                );

                binding.buttonStartRecommendedLesson.setText(
                        R.string.smart_plan_review_lesson
                );

                binding.buttonPracticeRecommended.setText(
                        R.string.smart_plan_practice_again
                );
                break;

            case NO_CONTENT:
            default:
                showNoRecommendationState();
                break;
        }
    }

    private void showNoRecommendationState() {
        currentRecommendation =
                StudyRecommendation.empty();
        currentApprovedChapterRowId = 0L;

        binding.cardRecommendation.setVisibility(
                View.GONE
        );

        binding.cardNoRecommendation.setVisibility(
                View.VISIBLE
        );
    }

    private void openRecommendedLesson() {
        if (activeStudentProfile == null
                || !currentRecommendation
                .hasRecommendation()
                || currentApprovedChapterRowId <= 0L) {

            Snackbar.make(
                    binding.getRoot(),
                    R.string.smart_plan_no_recommendation,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        Intent lessonIntent = new Intent(
                SmartStudyPlanActivity.this,
                LessonActivity.class
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                currentRecommendation.getSubjectName()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                currentRecommendation.getChapterTitle()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                currentRecommendation
                        .getChapterDescription()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_STUDENT_CLASS,
                activeStudentProfile.getStudentClass()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_EDUCATION_BOARD,
                activeStudentProfile.getEducationBoard()
        );

        ExactSchoolBookLessonContract.putExactChapterRowId(
                lessonIntent,
                currentApprovedChapterRowId
        );

        startActivity(lessonIntent);
    }

    private void openRecommendedPractice() {
        if (activeStudentProfile == null
                || !currentRecommendation
                .hasRecommendation()) {

            Snackbar.make(
                    binding.getRoot(),
                    R.string.smart_plan_no_recommendation,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        Intent practiceIntent = new Intent(
                SmartStudyPlanActivity.this,
                PracticeActivity.class
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_SUBJECT_NAME,
                currentRecommendation.getSubjectName()
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_CHAPTER_TITLE,
                currentRecommendation.getChapterTitle()
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_STUDENT_CLASS,
                activeStudentProfile.getStudentClass()
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_EDUCATION_BOARD,
                activeStudentProfile.getEducationBoard()
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_LANGUAGE_MODE,
                getPracticeLanguageMode(
                        activeStudentProfile
                                .getExplanationLanguage()
                )
        );

        startActivity(practiceIntent);
    }

    @NonNull
    private String getPracticeLanguageMode(
            String explanationLanguage
    ) {
        if (explanationLanguage == null) {
            return "BILINGUAL";
        }

        String normalizedLanguage =
                explanationLanguage
                        .toLowerCase(Locale.ROOT);

        boolean hindi =
                normalizedLanguage.contains("hindi")
                        || normalizedLanguage
                        .contains("हिंदी");

        boolean english =
                normalizedLanguage.contains("english")
                        || normalizedLanguage
                        .contains("अंग्रेज");

        if (hindi && !english) {
            return "HINDI";
        }

        if (english && !hindi) {
            return "ENGLISH";
        }

        return "BILINGUAL";
    }

    private void openRevisionPlanner() {
        Intent revisionIntent = new Intent(
                SmartStudyPlanActivity.this,
                RevisionActivity.class
        );

        startActivity(revisionIntent);
    }

    private void openReminderSettings() {
        Intent reminderIntent = new Intent(
                SmartStudyPlanActivity.this,
                ReminderSettingsActivity.class
        );

        startActivity(reminderIntent);
    }

    private void openChapterNotes() {
        Intent notesIntent = new Intent(
                SmartStudyPlanActivity.this,
                ChapterNotesActivity.class
        );

        if (currentRecommendation
                .hasRecommendation()) {

            notesIntent.putExtra(
                    ChapterNotesActivity
                            .EXTRA_PREFILL_SUBJECT,
                    currentRecommendation
                            .getSubjectName()
            );

            notesIntent.putExtra(
                    ChapterNotesActivity
                            .EXTRA_PREFILL_CHAPTER,
                    currentRecommendation
                            .getChapterTitle()
            );
        }

        startActivity(notesIntent);
    }

    private void openSettingsCenter() {
        Intent settingsIntent = new Intent(
                SmartStudyPlanActivity.this,
                SettingsActivity.class
        );

        startActivity(settingsIntent);
    }

    private void openSubjects() {
        Intent subjectsIntent = new Intent(
                SmartStudyPlanActivity.this,
                SubjectsActivity.class
        );

        startActivity(subjectsIntent);
    }

    private void showNoProfileState() {
        activeStudentProfile = null;

        currentRecommendation =
                StudyRecommendation.empty();

        binding.textPlanStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.textSmartPlanGoalValue.setText(
                "0 / 3"
        );

        binding.textSmartPlanGoalBreakdown.setText(
                getString(
                        R.string.daily_goal_breakdown_format,
                        0,
                        0,
                        0
                )
        );

        binding.textSmartPlanGoalStatus.setText(
                R.string.daily_goal_unavailable
        );

        binding.progressSmartPlanGoal.setMax(
                DEFAULT_DAILY_GOAL
        );

        binding.progressSmartPlanGoal
                .setProgressCompat(
                        0,
                        false
                );

        showNoRecommendationState();

        binding.buttonBrowseSubjects.setEnabled(
                false
        );
    }

    private void showLoadingState(boolean loading) {
        binding.progressSmartPlan.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.contentSmartPlan.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }
}
