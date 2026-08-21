package com.tridev.studysaathi;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tridev.studysaathi.adapter.ParentProfileSummaryAdapter;
import com.tridev.studysaathi.data.ai.CitationCoverageHistoryStore;
import com.tridev.studysaathi.data.ai.LowCoverageChapterRecommendationEngine;
import com.tridev.studysaathi.data.ai.RecommendedRevisionProgressStore;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.LessonProgressRepository;
import com.tridev.studysaathi.data.repository.QuizAttemptRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityParentDashboardBinding;
import com.tridev.studysaathi.model.ParentProfileSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ParentDashboardActivity
        extends AppCompatActivity {

    private static final String GOAL_PREFERENCES_NAME =
            "study_saathi_daily_goals";

    private static final String GOAL_KEY_PREFIX =
            "daily_goal_profile_";

    private static final int DEFAULT_DAILY_GOAL = 3;

    private ActivityParentDashboardBinding binding;

    private StudentProfileRepository studentProfileRepository;
    private LessonProgressRepository lessonProgressRepository;
    private QuizAttemptRepository quizAttemptRepository;
    private CitationCoverageHistoryStore citationCoverageHistoryStore;
    private RecommendedRevisionProgressStore recommendedRevisionProgressStore;

    private SharedPreferences goalPreferences;

    private ParentProfileSummaryAdapter profileSummaryAdapter;

    private final List<ParentProfileSummary> loadedSummaries =
            new ArrayList<>();

    private int pendingProfileCount;
    private int loadGeneration;

    private boolean partialLoadFailure;
    private boolean profileActivationInProgress;
    private boolean parentAccessVerified;
    private boolean securityPromptInProgress;
    private LowCoverageChapterRecommendationEngine.Recommendation
            topCitationRecommendation;

    private ActivityResultLauncher<Intent>
            deviceCredentialLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE
        );

        registerParentSecurityLauncher();

        binding =
                ActivityParentDashboardBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        returnToModeSelection();
                    }
                }
        );

        studentProfileRepository =
                new StudentProfileRepository(this);

        lessonProgressRepository =
                new LessonProgressRepository(this);

        quizAttemptRepository =
                new QuizAttemptRepository(this);

        citationCoverageHistoryStore =
                new CitationCoverageHistoryStore(this);
        recommendedRevisionProgressStore =
                new RecommendedRevisionProgressStore(this);

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

        profileActivationInProgress = false;

        if (parentAccessVerified) {
            loadParentDashboard();
        } else {
            verifyParentAccess();
        }
    }

    private void registerParentSecurityLauncher() {
        deviceCredentialLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .StartActivityForResult(),
                        result -> {
                            securityPromptInProgress = false;

                            if (result.getResultCode()
                                    == RESULT_OK) {

                                parentAccessVerified = true;
                                loadParentDashboard();
                                return;
                            }

                            Snackbar.make(
                                    binding.getRoot(),
                                    "Parent verification पूरी नहीं हुई।",
                                    Snackbar.LENGTH_LONG
                            ).show();

                            finish();
                        }
                );
    }

    private void verifyParentAccess() {
        if (securityPromptInProgress
                || isFinishing()
                || isDestroyed()) {

            return;
        }

        FirebaseUser currentUser =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (currentUser == null) {
            showCloudAccountRequiredDialog();
            return;
        }

        String email =
                currentUser.getEmail();

        if (email != null
                && !email.trim().isEmpty()
                && !currentUser.isEmailVerified()) {

            showVerifiedEmailRequiredDialog();
            return;
        }

        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(
                        KEYGUARD_SERVICE
                );

        if (keyguardManager == null
                || !keyguardManager.isDeviceSecure()) {

            showSecureLockRequiredDialog();
            return;
        }

        Intent confirmationIntent =
                keyguardManager
                        .createConfirmDeviceCredentialIntent(
                                "Parent Mode verification",
                                "Parent Dashboard खोलने के लिए device PIN, pattern या password confirm करें।"
                        );

        if (confirmationIntent == null) {
            showSecureLockRequiredDialog();
            return;
        }

        securityPromptInProgress = true;
        deviceCredentialLauncher.launch(
                confirmationIntent
        );
    }

    private void showCloudAccountRequiredDialog() {
        securityPromptInProgress = true;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Secure Parent Mode")
                .setMessage(
                        "Parent Dashboard के लिए पहले verified cloud account से sign in करें। इसके बाद device lock दूसरा security factor होगा।"
                )
                .setPositiveButton(
                        "Cloud Account खोलें",
                        (dialog, which) -> {
                            securityPromptInProgress = false;
                            startActivity(
                                    new Intent(
                                            this,
                                            CloudAccountActivity.class
                                    )
                            );
                        }
                )
                .setNegativeButton(
                        "वापस",
                        (dialog, which) -> finish()
                )
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void showVerifiedEmailRequiredDialog() {
        securityPromptInProgress = true;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Email verification जरूरी है")
                .setMessage(
                        "Parent Mode खोलने से पहले cloud account की email verify करें।"
                )
                .setPositiveButton(
                        "Account खोलें",
                        (dialog, which) -> {
                            securityPromptInProgress = false;
                            startActivity(
                                    new Intent(
                                            this,
                                            CloudAccountActivity.class
                                    )
                            );
                        }
                )
                .setNegativeButton(
                        "वापस",
                        (dialog, which) -> finish()
                )
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void showSecureLockRequiredDialog() {
        securityPromptInProgress = true;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Device lock जरूरी है")
                .setMessage(
                        "Parent Dashboard के 2-step protection के लिए phone Settings में PIN, pattern या password चालू करें।"
                )
                .setPositiveButton(
                        "ठीक है",
                        (dialog, which) -> finish()
                )
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void setupRecyclerView() {
        profileSummaryAdapter =
                new ParentProfileSummaryAdapter(
                        new ArrayList<>(),
                        this::openProfileProgress
                );

        binding.recyclerParentProfiles.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerParentProfiles.setAdapter(
                profileSummaryAdapter
        );

        binding.recyclerParentProfiles.setHasFixedSize(
                false
        );

        binding.recyclerParentProfiles.setNestedScrollingEnabled(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                returnToModeSelection()
        );

        binding.buttonManageParentProfiles.setOnClickListener(view ->
                openProfileManager()
        );

        binding.buttonSwitchToStudentMode.setOnClickListener(view -> {
            Intent modeIntent = new Intent(
                    ParentDashboardActivity.this,
                    UserModeSelectionActivity.class
            );
            modeIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );
            startActivity(modeIntent);
        });

        binding.buttonParentDoubtInsights.setOnClickListener(view ->
                startActivity(
                        new Intent(
                                ParentDashboardActivity.this,
                                DoubtHistoryActivity.class
                        )
                )
        );

        binding.buttonOpenRecommendedRevision.setOnClickListener(view ->
                openRecommendedRevision());

        binding.buttonParentSettingsGoals.setOnClickListener(view ->
                openParentControl(SettingsActivity.class)
        );

        binding.buttonParentReminders.setOnClickListener(view ->
                openParentControl(ReminderSettingsActivity.class)
        );

        binding.buttonParentLocalBackup.setOnClickListener(view ->
                openParentControl(BackupExportActivity.class)
        );

        binding.buttonParentRestoreImport.setOnClickListener(view ->
                openParentControl(BackupRestoreActivity.class)
        );

        binding.buttonParentCloudAccount.setOnClickListener(view ->
                openParentControl(CloudAccountActivity.class)
        );

        binding.buttonParentHelp.setOnClickListener(view -> {
            Intent helpIntent = new Intent(
                    ParentDashboardActivity.this,
                    HelpAboutActivity.class
            );
            helpIntent.putExtra(
                    HelpAboutActivity.EXTRA_MODE,
                    HelpAboutActivity.MODE_PARENT
            );
            startActivity(helpIntent);
        });
    }

    private void returnToModeSelection() {
        Intent modeIntent = new Intent(
                ParentDashboardActivity.this,
                UserModeSelectionActivity.class
        );
        modeIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(modeIntent);
    }

    private void openParentControl(@NonNull Class<?> destination) {
        if (!parentAccessVerified) {
            verifyParentAccess();
            return;
        }

        startActivity(new Intent(this, destination));
    }

    private void loadParentDashboard() {
        renderCitationCoverageInsights();
        int currentGeneration =
                ++loadGeneration;

        showLoadingState(true);

        loadedSummaries.clear();
        profileSummaryAdapter.submitList(
                new ArrayList<>()
        );

        studentProfileRepository.getAllProfiles(
                new StudentProfileRepository.ProfilesCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<StudentProfileEntity> profiles
                    ) {
                        if (!isValidGeneration(
                                currentGeneration
                        )) {
                            return;
                        }

                        if (profiles.isEmpty()) {
                            showLoadingState(false);
                            showEmptyProfilesState();
                            return;
                        }

                        binding.cardParentEmpty.setVisibility(
                                View.GONE
                        );

                        binding.recyclerParentProfiles.setVisibility(
                                View.VISIBLE
                        );

                        pendingProfileCount =
                                profiles.size();

                        partialLoadFailure = false;

                        for (StudentProfileEntity profile
                                : profiles) {

                            loadProfileProgress(
                                    profile,
                                    currentGeneration
                            );
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isValidGeneration(
                                currentGeneration
                        )) {
                            return;
                        }

                        showLoadingState(false);
                        showEmptyProfilesState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.parent_profiles_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void renderCitationCoverageInsights() {
        CitationCoverageHistoryStore.Summary summary =
                citationCoverageHistoryStore.getSummary();
        binding.textParentCitationCoverage.setText(summary.getCoveragePercent() + "%");
        binding.textParentCitationReviewed.setText(String.valueOf(summary.getReviewed()));
        binding.textParentCitationAttention.setText(
                String.valueOf(summary.getAttentionNeeded()));
        binding.textParentCitationInsight.setText(summary.getReviewed() == 0
                ? "Approved book citation वाले answers आने पर यहाँ insight दिखाई जाएगी।"
                : summary.getAttentionNeeded() == 0
                ? "सभी reviewed answers approved book evidence से grounded रहे।"
                : summary.getMissing() + " answers में citation missing था और "
                + summary.getBlocked() + " unsupported citations block किए गए।");

        List<CitationCoverageHistoryStore.ScopeSummary> scopes =
                citationCoverageHistoryStore.getTopScopeSummaries(3);
        if (scopes.isEmpty()) {
            binding.textParentCitationScopeInsights.setText(
                    "Subject और chapter breakdown अभी उपलब्ध नहीं है।");
            topCitationRecommendation = null;
            binding.buttonOpenRecommendedRevision.setEnabled(false);
            binding.textParentRevisionProgress.setText("Revision progress अभी उपलब्ध नहीं है।");
            return;
        }
        StringBuilder scopeText = new StringBuilder();
        for (CitationCoverageHistoryStore.ScopeSummary scope : scopes) {
            if (scopeText.length() > 0) scopeText.append("\n");
            scopeText.append("• ").append(scope.getSubject()).append(" — ")
                    .append(scope.getChapter()).append(": ")
                    .append(scope.getCoveragePercent()).append("% grounded")
                    .append(" • ").append(scope.getAttentionNeeded()).append(" attention");
        }
        binding.textParentCitationScopeInsights.setText(scopeText.toString());

        List<LowCoverageChapterRecommendationEngine.Recommendation> recommendations =
                LowCoverageChapterRecommendationEngine.recommend(
                        citationCoverageHistoryStore.getTopScopeSummaries(60), 3);
        if (recommendations.isEmpty()) {
            binding.textParentCitationRecommendations.setText(
                    "अभी कोई low-coverage chapter recommendation नहीं है।");
            topCitationRecommendation = null;
            binding.buttonOpenRecommendedRevision.setEnabled(false);
            binding.textParentRevisionProgress.setText("Revision progress अभी उपलब्ध नहीं है।");
            return;
        }
        topCitationRecommendation = recommendations.get(0);
        binding.buttonOpenRecommendedRevision.setEnabled(true);
        RecommendedRevisionProgressStore.Summary revisionSummary =
                recommendedRevisionProgressStore.getSummary(
                        topCitationRecommendation.getSubject(),
                        topCitationRecommendation.getChapter());
        binding.textParentRevisionProgress.setText(
                "Revision progress: " + revisionSummary.getCompleted() + "/"
                        + revisionSummary.getStarted() + " completed • "
                        + revisionSummary.getCompletionPercent() + "%");
        StringBuilder recommendationText = new StringBuilder();
        for (LowCoverageChapterRecommendationEngine.Recommendation recommendation
                : recommendations) {
            if (recommendationText.length() > 0) recommendationText.append("\n");
            recommendationText.append("→ ").append(recommendation.buildParentMessage());
        }
        binding.textParentCitationRecommendations.setText(recommendationText.toString());
    }

    private void openRecommendedRevision() {
        LowCoverageChapterRecommendationEngine.Recommendation recommendation =
                topCitationRecommendation;
        if (recommendation == null) return;
        Intent revisionIntent = new Intent(this, AskStudySaathiActivity.class);
        revisionIntent.putExtra(AskStudySaathiActivity.EXTRA_PREFILL_SUBJECT,
                recommendation.getSubject());
        revisionIntent.putExtra(AskStudySaathiActivity.EXTRA_PREFILL_CHAPTER,
                recommendation.getChapter());
        revisionIntent.putExtra(AskStudySaathiActivity.EXTRA_PREFILL_QUESTION,
                recommendation.buildRevisionQuestion());
        revisionIntent.putExtra(AskStudySaathiActivity.EXTRA_RECOMMENDED_REVISION, true);
        recommendedRevisionProgressStore.recordStarted(
                recommendation.getSubject(), recommendation.getChapter());
        startActivity(revisionIntent);
    }

    private void loadProfileProgress(
            @NonNull StudentProfileEntity profile,
            int generation
    ) {
        lessonProgressRepository.getProgressForProfile(
                profile.getProfileId(),
                new LessonProgressRepository.ProgressListCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<LessonProgressEntity> progressList
                    ) {
                        if (!isValidGeneration(generation)) {
                            return;
                        }

                        loadProfileQuizAttempts(
                                profile,
                                progressList,
                                generation,
                                false
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isValidGeneration(generation)) {
                            return;
                        }

                        loadProfileQuizAttempts(
                                profile,
                                new ArrayList<>(),
                                generation,
                                true
                        );
                    }
                }
        );
    }

    private void loadProfileQuizAttempts(
            @NonNull StudentProfileEntity profile,
            @NonNull List<LessonProgressEntity> progressList,
            int generation,
            boolean progressLoadFailed
    ) {
        quizAttemptRepository.getProfileAttempts(
                profile.getProfileId(),
                new QuizAttemptRepository.QuizAttemptListCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<QuizAttemptEntity> quizAttempts
                    ) {
                        if (!isValidGeneration(generation)) {
                            return;
                        }

                        onProfileSummaryReady(
                                createProfileSummary(
                                        profile,
                                        progressList,
                                        quizAttempts
                                ),
                                generation,
                                progressLoadFailed
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isValidGeneration(generation)) {
                            return;
                        }

                        onProfileSummaryReady(
                                createProfileSummary(
                                        profile,
                                        progressList,
                                        new ArrayList<>()
                                ),
                                generation,
                                true
                        );
                    }
                }
        );
    }

    private void onProfileSummaryReady(
            @NonNull ParentProfileSummary profileSummary,
            int generation,
            boolean loadFailed
    ) {
        if (!isValidGeneration(generation)) {
            return;
        }

        loadedSummaries.add(profileSummary);

        if (loadFailed) {
            partialLoadFailure = true;
        }

        pendingProfileCount--;

        if (pendingProfileCount > 0) {
            return;
        }

        showLoadingState(false);

        sortProfileSummaries();
        showLoadedProfileSummaries();

        if (partialLoadFailure) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.parent_partial_data_failed,
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @NonNull
    private ParentProfileSummary createProfileSummary(
            @NonNull StudentProfileEntity profile,
            @NonNull List<LessonProgressEntity> progressList,
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        int completedLessons = 0;
        int revisionCount = 0;
        int todayLessonCount = 0;
        int todayRevisionCount = 0;

        long lastActivityAt = 0L;

        ZoneId zoneId =
                ZoneId.systemDefault();

        LocalDate today =
                LocalDate.now(zoneId);

        Set<LocalDate> activityDates =
                new HashSet<>();

        for (LessonProgressEntity progress
                : progressList) {

            if (progress.isCompleted()) {
                completedLessons++;

                if (isTimestampOnDate(
                        progress.getCompletedAt(),
                        today,
                        zoneId
                )) {
                    todayLessonCount++;
                }
            }

            revisionCount += Math.max(
                    0,
                    progress.getRevisionCount()
            );

            if (progress.getRevisionCount() > 0
                    && isTimestampOnDate(
                    progress.getLastRevisedAt(),
                    today,
                    zoneId
            )) {
                todayRevisionCount++;
            }

            lastActivityAt = Math.max(
                    lastActivityAt,
                    progress.getCompletedAt()
            );

            lastActivityAt = Math.max(
                    lastActivityAt,
                    progress.getLastStudiedAt()
            );

            lastActivityAt = Math.max(
                    lastActivityAt,
                    progress.getLastRevisedAt()
            );

            addActivityDate(
                    activityDates,
                    progress.getCompletedAt(),
                    zoneId
            );

            addActivityDate(
                    activityDates,
                    progress.getLastStudiedAt(),
                    zoneId
            );

            addActivityDate(
                    activityDates,
                    progress.getLastRevisedAt(),
                    zoneId
            );
        }

        int latestQuizScore = -1;
        int bestQuizScore = -1;
        int todayQuizCount = 0;

        long latestQuizTime = 0L;

        for (QuizAttemptEntity quizAttempt
                : quizAttempts) {

            int percentage =
                    quizAttempt.getPercentage();

            bestQuizScore = Math.max(
                    bestQuizScore,
                    percentage
            );

            if (quizAttempt.getAttemptedAt()
                    >= latestQuizTime) {

                latestQuizTime =
                        quizAttempt.getAttemptedAt();

                latestQuizScore =
                        percentage;
            }

            if (isTimestampOnDate(
                    quizAttempt.getAttemptedAt(),
                    today,
                    zoneId
            )) {
                todayQuizCount++;
            }

            lastActivityAt = Math.max(
                    lastActivityAt,
                    quizAttempt.getAttemptedAt()
            );

            addActivityDate(
                    activityDates,
                    quizAttempt.getAttemptedAt(),
                    zoneId
            );
        }

        WeakChapterResult weakChapterResult =
                findWeakChapter(
                        quizAttempts
                );

        int dailyGoal =
                getDailyGoal(
                        profile.getProfileId()
                );

        int todayActions =
                todayLessonCount
                        + todayRevisionCount
                        + todayQuizCount;

        return new ParentProfileSummary(
                profile.getProfileId(),
                profile.getStudentName(),
                profile.getEducationBoard(),
                profile.getStudentClass(),
                profile.isActive(),
                completedLessons,
                revisionCount,
                quizAttempts.size(),
                latestQuizScore,
                bestQuizScore,
                calculateCurrentStreak(
                        activityDates,
                        today
                ),
                todayActions,
                dailyGoal,
                lastActivityAt,
                weakChapterResult.subjectName,
                weakChapterResult.chapterTitle,
                weakChapterResult.averageScore
        );
    }

    @NonNull
    private WeakChapterResult findWeakChapter(
            @NonNull List<QuizAttemptEntity> quizAttempts
    ) {
        Map<String, ChapterScoreAccumulator> chapterScores =
                new HashMap<>();

        for (QuizAttemptEntity quizAttempt
                : quizAttempts) {

            String subjectName =
                    safeText(
                            quizAttempt.getSubjectName()
                    );

            String chapterTitle =
                    safeText(
                            quizAttempt.getChapterTitle()
                    );

            if (chapterTitle.isEmpty()) {
                continue;
            }

            String scoreKey =
                    subjectName
                            + "\u0000"
                            + chapterTitle;

            ChapterScoreAccumulator accumulator =
                    chapterScores.get(scoreKey);

            if (accumulator == null) {
                accumulator =
                        new ChapterScoreAccumulator(
                                subjectName,
                                chapterTitle
                        );

                chapterScores.put(
                        scoreKey,
                        accumulator
                );
            }

            accumulator.addScore(
                    quizAttempt.getPercentage()
            );
        }

        WeakChapterResult weakResult =
                new WeakChapterResult(
                        "",
                        "",
                        -1
                );

        for (ChapterScoreAccumulator accumulator
                : chapterScores.values()) {

            int averageScore =
                    accumulator.getAverageScore();

            if (weakResult.averageScore < 0
                    || averageScore
                    < weakResult.averageScore) {

                weakResult =
                        new WeakChapterResult(
                                accumulator.subjectName,
                                accumulator.chapterTitle,
                                averageScore
                        );
            }
        }

        return weakResult;
    }

    private int calculateCurrentStreak(
            @NonNull Set<LocalDate> activityDates,
            @NonNull LocalDate today
    ) {
        if (activityDates.isEmpty()) {
            return 0;
        }

        LocalDate streakDate;

        if (activityDates.contains(today)) {
            streakDate = today;
        } else if (activityDates.contains(
                today.minusDays(1)
        )) {
            streakDate =
                    today.minusDays(1);
        } else {
            return 0;
        }

        int streakCount = 0;

        while (activityDates.contains(streakDate)) {
            streakCount++;
            streakDate =
                    streakDate.minusDays(1);
        }

        return streakCount;
    }

    private void addActivityDate(
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

    private void sortProfileSummaries() {
        Collections.sort(
                loadedSummaries,
                (firstProfile, secondProfile) -> {
                    if (firstProfile.isActiveProfile()
                            && !secondProfile.isActiveProfile()) {
                        return -1;
                    }

                    if (!firstProfile.isActiveProfile()
                            && secondProfile.isActiveProfile()) {
                        return 1;
                    }

                    return firstProfile
                            .getStudentName()
                            .compareToIgnoreCase(
                                    secondProfile
                                            .getStudentName()
                            );
                }
        );
    }

    private void showLoadedProfileSummaries() {
        profileSummaryAdapter.submitList(
                new ArrayList<>(
                        loadedSummaries
                )
        );

        binding.recyclerParentProfiles.setVisibility(
                View.VISIBLE
        );

        binding.cardParentEmpty.setVisibility(
                View.GONE
        );

        showOverallSummary();
    }

    private void showOverallSummary() {
        int totalStudents =
                loadedSummaries.size();

        int profilesWithActivity = 0;
        int totalCompletedLessons = 0;

        int totalBestScores = 0;
        int profilesWithQuizScores = 0;

        ParentProfileSummary selectedStudent = null;
        for (ParentProfileSummary summary : loadedSummaries) {
            if (summary.isActiveProfile()) {
                selectedStudent = summary;
                break;
            }
        }
        if (selectedStudent == null && !loadedSummaries.isEmpty()) {
            selectedStudent = loadedSummaries.get(0);
        }
        if (selectedStudent != null) {
            profilesWithActivity =
                    selectedStudent.getLastActivityAt() > 0L ? 1 : 0;
            totalCompletedLessons =
                    selectedStudent.getCompletedLessons();
            if (selectedStudent.hasQuizData()) {
                totalBestScores = selectedStudent.getBestQuizScore();
                profilesWithQuizScores = 1;
            }
        }

        binding.textParentTotalStudents.setText(
                String.valueOf(totalStudents)
        );

        binding.textParentProfilesWithActivity.setText(
                String.valueOf(profilesWithActivity)
        );

        binding.textParentCompletedLessons.setText(
                String.valueOf(totalCompletedLessons)
        );

        if (profilesWithQuizScores == 0) {
            binding.textParentAverageBestScore.setText(
                    "—"
            );
        } else {
            int averageBestScore =
                    Math.round(
                            totalBestScores
                                    * 1f
                                    / profilesWithQuizScores
                    );

            binding.textParentAverageBestScore.setText(
                    getString(
                            R.string.parent_percentage_format,
                            averageBestScore
                    )
            );
        }

        binding.textParentDashboardSubtitle.setText(
                getString(
                        R.string.parent_dashboard_profiles_format,
                        totalStudents
                )
        );
    }

    private void openProfileProgress(
            @NonNull ParentProfileSummary profileSummary
    ) {
        if (profileActivationInProgress) {
            return;
        }

        if (profileSummary.isActiveProfile()) {
            openLearningProgress();
            return;
        }

        profileActivationInProgress = true;

        Snackbar.make(
                binding.getRoot(),
                getString(
                        R.string.parent_activating_profile_format,
                        profileSummary.getStudentName()
                ),
                Snackbar.LENGTH_SHORT
        ).show();

        studentProfileRepository.activateProfile(
                profileSummary.getProfileId(),
                new StudentProfileRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        profileActivationInProgress = false;
                        openLearningProgress();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        profileActivationInProgress = false;

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.parent_profile_activation_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void openLearningProgress() {
        Intent progressIntent = new Intent(
                ParentDashboardActivity.this,
                LearningProgressActivity.class
        );

        startActivity(progressIntent);
    }

    private void openProfileManager() {
        Intent profilesIntent = new Intent(
                ParentDashboardActivity.this,
                StudentProfilesActivity.class
        );

        startActivity(profilesIntent);
    }

    private void showEmptyProfilesState() {
        loadedSummaries.clear();

        profileSummaryAdapter.submitList(
                new ArrayList<>()
        );

        binding.recyclerParentProfiles.setVisibility(
                View.GONE
        );

        binding.cardParentEmpty.setVisibility(
                View.VISIBLE
        );

        binding.textParentTotalStudents.setText("0");
        binding.textParentProfilesWithActivity.setText("0");
        binding.textParentCompletedLessons.setText("0");
        binding.textParentAverageBestScore.setText("—");

        binding.textParentDashboardSubtitle.setText(
                R.string.parent_dashboard_no_profiles_subtitle
        );
    }

    private boolean isValidGeneration(
            int requiredGeneration
    ) {
        return !isFinishing()
                && !isDestroyed()
                && loadGeneration
                == requiredGeneration;
    }

    @NonNull
    private String safeText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private void showLoadingState(boolean loading) {
        binding.progressParentDashboard.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.contentParentDashboard.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }

    private static class ChapterScoreAccumulator {

        @NonNull
        private final String subjectName;

        @NonNull
        private final String chapterTitle;

        private int totalScore;
        private int attemptCount;

        ChapterScoreAccumulator(
                @NonNull String subjectName,
                @NonNull String chapterTitle
        ) {
            this.subjectName = subjectName;
            this.chapterTitle = chapterTitle;
        }

        void addScore(int score) {
            totalScore += score;
            attemptCount++;
        }

        int getAverageScore() {
            if (attemptCount <= 0) {
                return 0;
            }

            return Math.round(
                    totalScore
                            * 1f
                            / attemptCount
            );
        }
    }

    private static class WeakChapterResult {

        @NonNull
        private final String subjectName;

        @NonNull
        private final String chapterTitle;

        private final int averageScore;

        WeakChapterResult(
                @NonNull String subjectName,
                @NonNull String chapterTitle,
                int averageScore
        ) {
            this.subjectName = subjectName;
            this.chapterTitle = chapterTitle;
            this.averageScore = averageScore;
        }
    }
}
