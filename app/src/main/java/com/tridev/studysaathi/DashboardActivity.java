package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.LessonProgressRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.data.repository.SchoolCurriculumProfileRepository;
import com.tridev.studysaathi.data.local.entity.SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.databinding.ActivityDashboardBinding;
import com.tridev.studysaathi.databinding.BottomSheetStudyToolsBinding;
import com.tridev.studysaathi.model.ChapterItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;

    private StudentProfileRepository studentProfileRepository;
    private SchoolCurriculumProfileRepository curriculumProfileRepository;
    private LessonProgressRepository lessonProgressRepository;

    private StudentProfileEntity activeStudentProfile;

    private boolean continueLessonAvailable;

    private String continueSubjectName = "";
    private String continueChapterTitle = "";
    private String continueChapterDescription = "";
    private String continueStudentClass = "";
    private String continueEducationBoard = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDashboardBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);
        curriculumProfileRepository =
                new SchoolCurriculumProfileRepository(this);

        lessonProgressRepository =
                new LessonProgressRepository(this);

        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Dashboard पर वापस आने पर active profile को दोबारा load किया
         * जाता है। इससे Settings या Profiles screen में profile बदलने
         * के बाद Dashboard तुरंत नए student की जानकारी दिखाता है।
         */
        loadActiveStudentProfile();
    }

    private void setupClickListeners() {
        binding.buttonContinueLearning.setOnClickListener(view ->
                openContinueLearning()
        );

        binding.buttonSubjects.setOnClickListener(view ->
                openSubjects()
        );

        binding.buttonRevision.setOnClickListener(view ->
                openRevision()
        );

        binding.buttonAskSaathi.setOnClickListener(view ->
                openAskStudySaathi()
        );

        binding.buttonSmartStudyPlan.setOnClickListener(view ->
                openSmartStudyPlan()
        );

        binding.buttonStudyTools.setOnClickListener(view ->
                showStudyToolsBottomSheet()
        );

        /*
         * Dashboard का profile icon/card अब सीधे Settings & Profiles
         * screen खोलेगा।
         */
        binding.cardProfileShortcut.setOnClickListener(view ->
                binding.dashboardRoot.openDrawer(
                        GravityCompat.END
                )
        );

        binding.cardDashboardMenu.setOnClickListener(view ->
                binding.dashboardRoot.openDrawer(
                        GravityCompat.START
                )
        );

        setupProfileDrawerActions();
        setupStudyMenuActions();
    }

    private void setupStudyMenuActions() {
        binding.studyMenuDrawerPanel.textMenuAppVersion.setText(
                "Study Saathi • Version " + BuildConfig.VERSION_NAME
        );

        binding.studyMenuDrawerPanel.buttonMenuDashboard
                .setOnClickListener(view ->
                        binding.dashboardRoot.closeDrawer(
                                GravityCompat.START
                        )
                );

        binding.studyMenuDrawerPanel.buttonMenuSubjects
                .setOnClickListener(view ->
                        closeStudyMenuAndOpen(
                                SubjectsActivity.class
                        )
                );

        binding.studyMenuDrawerPanel.buttonMenuSmartStudyPlan
                .setOnClickListener(view ->
                        closeStudyMenuAndOpen(
                                SmartStudyPlanActivity.class
                        )
                );

        binding.studyMenuDrawerPanel.buttonMenuRevision
                .setOnClickListener(view ->
                        closeStudyMenuAndOpen(
                                RevisionActivity.class
                        )
                );

        binding.studyMenuDrawerPanel.buttonMenuWeeklyStudy
                .setOnClickListener(view ->
                        closeStudyMenuAndOpen(
                                WeeklyStudyActivity.class
                        )
                );

        binding.studyMenuDrawerPanel.buttonMenuLearningProgress
                .setOnClickListener(view ->
                        closeStudyMenuAndOpen(
                                LearningProgressActivity.class
                        )
                );

        binding.studyMenuDrawerPanel.buttonMenuAskSaathi
                .setOnClickListener(view ->
                        closeStudyMenuAndOpen(
                                AskStudySaathiActivity.class
                        )
                );

        binding.studyMenuDrawerPanel.buttonMenuLearningLab
                .setOnClickListener(view ->
                        closeStudyMenuAndOpen(
                                LearningLabActivity.class
                        )
                );

        binding.studyMenuDrawerPanel.buttonMenuChapterNotes
                .setOnClickListener(view ->
                        closeStudyMenuAndOpen(
                                ChapterNotesActivity.class
                        )
                );

    }

    private void closeStudyMenuAndOpen(
            @NonNull Class<?> destinationActivity
    ) {
        binding.dashboardRoot.closeDrawer(
                GravityCompat.START
        );

        startActivity(
                new Intent(
                        DashboardActivity.this,
                        destinationActivity
                )
        );
    }

    private void setupProfileDrawerActions() {
        binding.profileDrawerPanel.buttonDrawerStudentMode
                .setOnClickListener(view ->
                        binding.dashboardRoot.closeDrawer(
                                GravityCompat.END
                        )
                );

        binding.profileDrawerPanel.buttonDrawerParentMode
                .setOnClickListener(view ->
                        closeProfileDrawerAndOpen(
                                ParentDashboardActivity.class
                        )
                );

        binding.profileDrawerPanel.buttonDrawerHelp
                .setOnClickListener(view -> {
                    binding.dashboardRoot.closeDrawer(GravityCompat.END);
                    Intent helpIntent = new Intent(
                            DashboardActivity.this,
                            HelpAboutActivity.class
                    );
                    helpIntent.putExtra(
                            HelpAboutActivity.EXTRA_MODE,
                            HelpAboutActivity.MODE_STUDENT
                    );
                    startActivity(helpIntent);
                });

    }

    private void closeProfileDrawerAndOpen(
            @NonNull Class<?> destinationActivity
    ) {
        binding.dashboardRoot.closeDrawer(
                GravityCompat.END
        );

        startActivity(
                new Intent(
                        DashboardActivity.this,
                        destinationActivity
                )
        );
    }

    private void openSmartStudyPlan() {
        Intent planIntent =
                new Intent(
                        DashboardActivity.this,
                        SmartStudyPlanActivity.class
                );

        startActivity(planIntent);
    }

    private void showStudyToolsBottomSheet() {
        BottomSheetStudyToolsBinding toolsBinding =
                BottomSheetStudyToolsBinding.inflate(
                        getLayoutInflater()
                );

        BottomSheetDialog toolsDialog =
                new BottomSheetDialog(this);

        toolsDialog.setContentView(
                toolsBinding.getRoot()
        );

        toolsBinding.cardToolRevisionPlanner
                .setOnClickListener(view -> {
                    toolsDialog.dismiss();
                    openRevision();
                });

        toolsBinding.cardToolChapterNotes
                .setOnClickListener(view -> {
                    toolsDialog.dismiss();

                    startActivity(
                            new Intent(
                                    DashboardActivity.this,
                                    ChapterNotesActivity.class
                            )
                    );
                });

        toolsDialog.show();
    }

    private void loadActiveStudentProfile() {
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

                        showLoadingState(false);

                        if (studentProfile == null) {
                            activeStudentProfile = null;
                            showProfileNotFoundState();
                            return;
                        }

                        activeStudentProfile = studentProfile;

                        showStudentProfile(
                                studentProfile
                        );

                        loadDashboardProgress(
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
                        activeStudentProfile = null;

                        showProfileNotFoundState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.profile_loading_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentProfile(
            @NonNull StudentProfileEntity studentProfile
    ) {
        String studentName =
                studentProfile.getStudentName();

        binding.textGreeting.setText(
                getString(
                        R.string.dashboard_greeting_format,
                        studentName
                )
        );

        String profileDetails =
                studentProfile.getEducationBoard()
                        + "  •  "
                        + studentProfile.getStudentClass()
                        + "  •  "
                        + studentProfile.getStudyMedium();

        binding.textStudentDetails.setText(
                profileDetails
        );

        binding.textExplanationLanguage.setText(
                getString(
                        R.string.explanation_language_format,
                        studentProfile.getExplanationLanguage()
                )
        );

        binding.textProfileInitial.setText(
                getStudentInitial(studentName)
        );

        binding.profileDrawerPanel.textDrawerProfileInitial
                .setText(
                        getStudentInitial(studentName)
                );

        binding.profileDrawerPanel.textDrawerProfileName
                .setText(studentName);

        binding.profileDrawerPanel.textDrawerProfileDetails
                .setText(profileDetails);

        loadStudentSchoolDetails(studentProfile.getProfileId());
        showProgressLoadingState();
    }

    private void loadStudentSchoolDetails(long profileId) {
        curriculumProfileRepository.getCurriculumProfile(
                profileId,
                new SchoolCurriculumProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            SchoolCurriculumProfileEntity profile
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (profile == null
                                || profile.getSchoolName().trim().isEmpty()
                                || "School details pending".equalsIgnoreCase(
                                profile.getSchoolName().trim())) {
                            binding.textStudentSchoolDetails.setVisibility(
                                    View.GONE
                            );
                            return;
                        }
                        String details = "🏫 " + profile.getSchoolName();
                        if (!profile.getSchoolCode().trim().isEmpty()) {
                            details += " • Code " + profile.getSchoolCode();
                        }
                        if (!profile.getSection().trim().isEmpty()) {
                            details += " • Section " + profile.getSection();
                        }
                        binding.textStudentSchoolDetails.setText(details);
                        binding.textStudentSchoolDetails.setVisibility(
                                View.VISIBLE
                        );
                        binding.profileDrawerPanel.textDrawerProfileDetails
                                .setText(
                                        binding.profileDrawerPanel
                                                .textDrawerProfileDetails
                                                .getText()
                                                + "\n"
                                                + details
                                );
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (!isFinishing() && !isDestroyed()) {
                            binding.textStudentSchoolDetails.setVisibility(
                                    View.GONE
                            );
                        }
                    }
                }
        );
    }

    private void loadDashboardProgress(
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

                        /*
                         * Async result किसी पुराने profile का हो सकता है।
                         * इसलिए result दिखाने से पहले active profile ID
                         * verify की जाती है।
                         */
                        if (activeStudentProfile == null
                                || activeStudentProfile.getProfileId()
                                != studentProfile.getProfileId()) {
                            return;
                        }

                        showDashboardProgress(
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

                        if (activeStudentProfile == null
                                || activeStudentProfile.getProfileId()
                                != studentProfile.getProfileId()) {
                            return;
                        }

                        showEmptyDashboardProgress(
                                studentProfile
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.dashboard_progress_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showDashboardProgress(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull List<LessonProgressEntity> progressList
    ) {
        int completedLessonCount =
                getCompletedLessonCount(progressList);

        int studyStreak =
                calculateStudyStreak(progressList);

        binding.textCompletedLessonsValue.setText(
                String.valueOf(completedLessonCount)
        );

        if (completedLessonCount == 0) {
            binding.textCompletedLessonsDetail.setText(
                    R.string.lessons_done_empty_detail
            );
        } else {
            binding.textCompletedLessonsDetail.setText(
                    getString(
                            R.string.lessons_done_detail_format,
                            completedLessonCount
                    )
            );
        }

        binding.textStreakValue.setText(
                String.valueOf(studyStreak)
        );

        if (studyStreak == 0) {
            binding.textStreakDetail.setText(
                    R.string.streak_empty_detail
            );
        } else {
            binding.textStreakDetail.setText(
                    getString(
                            R.string.streak_detail_format,
                            studyStreak
                    )
            );
        }

        if (completedLessonCount == 0
                || progressList.isEmpty()) {

            showEmptyContinueLearningState(
                    studentProfile
            );

            return;
        }

        binding.textCoachMessage.setText(
                getString(
                        R.string.dashboard_progress_coach_format,
                        studentProfile.getStudentName(),
                        completedLessonCount
                )
        );

        prepareContinueLearning(
                progressList.get(0)
        );
    }

    private int getCompletedLessonCount(
            @NonNull List<LessonProgressEntity> progressList
    ) {
        int completedCount = 0;

        for (LessonProgressEntity lessonProgress
                : progressList) {

            if (lessonProgress.isCompleted()) {
                completedCount++;
            }
        }

        return completedCount;
    }

    private int calculateStudyStreak(
            @NonNull List<LessonProgressEntity> progressList
    ) {
        Set<LocalDate> studyDates =
                new HashSet<>();

        ZoneId currentZone =
                ZoneId.systemDefault();

        for (LessonProgressEntity lessonProgress
                : progressList) {

            if (!lessonProgress.isCompleted()) {
                continue;
            }

            long activityTime =
                    lessonProgress.getCompletedAt() > 0L
                            ? lessonProgress.getCompletedAt()
                            : lessonProgress.getLastStudiedAt();

            if (activityTime <= 0L) {
                continue;
            }

            LocalDate studyDate =
                    Instant.ofEpochMilli(activityTime)
                            .atZone(currentZone)
                            .toLocalDate();

            studyDates.add(studyDate);
        }

        if (studyDates.isEmpty()) {
            return 0;
        }

        LocalDate today =
                LocalDate.now(currentZone);

        LocalDate streakDate;

        if (studyDates.contains(today)) {
            streakDate = today;
        } else if (studyDates.contains(
                today.minusDays(1)
        )) {
            streakDate = today.minusDays(1);
        } else {
            return 0;
        }

        int streakCount = 0;

        while (studyDates.contains(streakDate)) {
            streakCount++;
            streakDate = streakDate.minusDays(1);
        }

        return streakCount;
    }

    private void prepareContinueLearning(
            @NonNull LessonProgressEntity latestProgress
    ) {
        continueEducationBoard =
                latestProgress.getEducationBoard();

        continueStudentClass =
                latestProgress.getStudentClass();

        continueSubjectName =
                latestProgress.getSubjectName();

        List<ChapterItem> subjectChapters =
                ChapterCatalog.getChapters(
                        continueEducationBoard,
                        continueStudentClass,
                        continueSubjectName
                );

        ChapterItem selectedChapter =
                findContinueChapter(
                        subjectChapters,
                        latestProgress
                );

        if (selectedChapter == null) {
            continueChapterTitle =
                    latestProgress.getChapterTitle();

            continueChapterDescription =
                    getString(
                            R.string.continue_learning_default_detail
                    );
        } else {
            continueChapterTitle =
                    selectedChapter.getChapterTitle();

            continueChapterDescription =
                    selectedChapter.getChapterDescription();
        }

        continueLessonAvailable = true;

        binding.buttonContinueLearning.setText(
                R.string.continue_learning_button
        );

        binding.textContinueLearningDetail.setText(
                getString(
                        R.string.continue_learning_detail_format,
                        continueSubjectName,
                        continueChapterTitle
                )
        );
    }

    private ChapterItem findContinueChapter(
            @NonNull List<ChapterItem> subjectChapters,
            @NonNull LessonProgressEntity latestProgress
    ) {
        if (subjectChapters.isEmpty()) {
            return null;
        }

        String completedChapterTitle =
                normalizeText(
                        latestProgress.getChapterTitle()
                );

        for (int index = 0;
             index < subjectChapters.size();
             index++) {

            ChapterItem chapterItem =
                    subjectChapters.get(index);

            if (!normalizeText(
                    chapterItem.getChapterTitle()
            ).equals(completedChapterTitle)) {
                continue;
            }

            if (latestProgress.isCompleted()
                    && index + 1
                    < subjectChapters.size()) {

                return subjectChapters.get(
                        index + 1
                );
            }

            return chapterItem;
        }

        return null;
    }

    private void showProgressLoadingState() {
        continueLessonAvailable = false;

        binding.textCompletedLessonsValue.setText("—");
        binding.textStreakValue.setText("—");

        binding.textCompletedLessonsDetail.setText(
                R.string.dashboard_progress_loading
        );

        binding.textStreakDetail.setText(
                R.string.dashboard_progress_loading
        );

        binding.textContinueLearningDetail.setText(
                R.string.dashboard_progress_loading
        );

        binding.buttonContinueLearning.setText(
                R.string.start_learning
        );
    }

    private void showEmptyDashboardProgress(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textCompletedLessonsValue.setText("0");
        binding.textStreakValue.setText("0");

        binding.textCompletedLessonsDetail.setText(
                R.string.lessons_done_empty_detail
        );

        binding.textStreakDetail.setText(
                R.string.streak_empty_detail
        );

        showEmptyContinueLearningState(
                studentProfile
        );
    }

    private void showEmptyContinueLearningState(
            @NonNull StudentProfileEntity studentProfile
    ) {
        continueLessonAvailable = false;

        continueSubjectName = "";
        continueChapterTitle = "";
        continueChapterDescription = "";
        continueStudentClass = "";
        continueEducationBoard = "";

        binding.buttonContinueLearning.setText(
                R.string.start_learning
        );

        binding.textContinueLearningDetail.setText(
                R.string.continue_learning_default_detail
        );

        binding.textCoachMessage.setText(
                getString(
                        R.string.dashboard_first_lesson_coach_format,
                        studentProfile.getStudentName()
                )
        );
    }

    private void openContinueLearning() {
        if (!continueLessonAvailable) {
            openSubjects();
            return;
        }

        Intent lessonIntent = new Intent(
                DashboardActivity.this,
                LessonActivity.class
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                continueSubjectName
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                continueChapterTitle
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                continueChapterDescription
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_STUDENT_CLASS,
                continueStudentClass
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_EDUCATION_BOARD,
                continueEducationBoard
        );

        startActivity(lessonIntent);
    }

    private void showProfileNotFoundState() {
        continueLessonAvailable = false;

        continueSubjectName = "";
        continueChapterTitle = "";
        continueChapterDescription = "";
        continueStudentClass = "";
        continueEducationBoard = "";

        binding.textGreeting.setText(
                R.string.dashboard_default_greeting
        );

        binding.textStudentDetails.setText(
                R.string.no_active_profile
        );

        binding.textExplanationLanguage.setText(
                R.string.create_profile_to_continue
        );

        binding.textProfileInitial.setText("S");

        binding.textCompletedLessonsValue.setText("0");
        binding.textStreakValue.setText("0");

        binding.textCompletedLessonsDetail.setText(
                R.string.lessons_done_empty_detail
        );

        binding.textStreakDetail.setText(
                R.string.streak_empty_detail
        );

        binding.textContinueLearningDetail.setText(
                R.string.continue_learning_default_detail
        );

        binding.buttonContinueLearning.setText(
                R.string.start_learning
        );

        binding.textCoachMessage.setText(
                R.string.create_profile_to_continue
        );

        Snackbar.make(
                binding.getRoot(),
                R.string.no_active_profile,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showLoadingState(boolean loading) {
        binding.progressDashboard.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.contentDashboard.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }

    private void openSubjects() {
        Intent subjectsIntent = new Intent(
                DashboardActivity.this,
                SubjectsActivity.class
        );

        startActivity(subjectsIntent);
    }

    private void openRevision() {
        Intent revisionIntent = new Intent(
                DashboardActivity.this,
                RevisionActivity.class
        );

        startActivity(revisionIntent);
    }

    private void openAskStudySaathi() {
        Intent askSaathiIntent = new Intent(
                DashboardActivity.this,
                AskStudySaathiActivity.class
        );

        startActivity(askSaathiIntent);
    }

    private void openLearningProgress() {
        Intent progressIntent = new Intent(
                DashboardActivity.this,
                LearningProgressActivity.class
        );

        startActivity(progressIntent);
    }

    private void openSettingsCenter() {
        Intent settingsIntent = new Intent(
                DashboardActivity.this,
                SettingsActivity.class
        );

        startActivity(settingsIntent);
    }

    private String getStudentInitial(
            String studentName
    ) {
        if (studentName == null
                || studentName.trim().isEmpty()) {
            return "S";
        }

        return studentName
                .trim()
                .substring(0, 1)
                .toUpperCase(Locale.getDefault());
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
}
