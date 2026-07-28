package com.tridev.studysaathi.data.ai;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tridev.studysaathi.AskStudySaathiActivity;
import com.tridev.studysaathi.R;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Ask Study Saathi screen पर Firebase AI quota cooldown को
 * Activity की बड़ी file बदले बिना दिखाने वाला lifecycle observer।
 *
 * यह observer:
 *
 * 1. केवल AskStudySaathiActivity को observe करता है।
 * 2. FirebaseAiQuotaCooldownManager की persistent state जाँचता है।
 * 3. Active cooldown में Ask और Quick Action buttons रोकता है।
 * 4. Ask button पर live MM:SS countdown दिखाता है।
 * 5. Question input के नीचे exact cooldown helper message दिखाता है।
 * 6. Activity बंद होने पर callbacks और countdown सुरक्षित रूप से रोकता है।
 * 7. Cooldown पूरा होने पर योग्य controls फिर से सक्रिय करता है।
 *
 * इसे StudySaathiApplication में केवल एक बार register किया जाएगा।
 */
public final class FirebaseAiQuotaActivityObserver
        implements Application.ActivityLifecycleCallbacks {

    private static final long COOLDOWN_DISCOVERY_INTERVAL_MILLIS =
            400L;

    @Nullable
    private static FirebaseAiQuotaActivityObserver sharedInstance;

    @NonNull
    private final FirebaseAiQuotaCooldownManager cooldownManager;

    @NonNull
    private final FirebaseAiQuotaCountdownController countdownController;

    @NonNull
    private final Handler mainHandler;

    @NonNull
    private WeakReference<AskStudySaathiActivity>
            activeActivityReference =
            new WeakReference<>(
                    null
            );

    @Nullable
    private CharSequence previousQuestionHelperText;

    private long observedCooldownUntilEpochMillis;

    private boolean quotaUiApplied;

    private boolean pollingActive;

    @NonNull
    private final Runnable cooldownDiscoveryRunnable =
            this::discoverAndApplyCooldown;

    private FirebaseAiQuotaActivityObserver(
            @NonNull Application application
    ) {
        cooldownManager =
                new FirebaseAiQuotaCooldownManager(
                        application
                );

        countdownController =
                new FirebaseAiQuotaCountdownController();

        mainHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Application process में observer को केवल एक बार register करता है।
     */
    public static synchronized void register(
            @NonNull Application application
    ) {
        if (sharedInstance != null) {
            return;
        }

        FirebaseAiQuotaActivityObserver observer =
                new FirebaseAiQuotaActivityObserver(
                        application
                );

        application.registerActivityLifecycleCallbacks(
                observer
        );

        sharedInstance =
                observer;
    }

    @Override
    public void onActivityCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState
    ) {
        // No action required.
    }

    @Override
    public void onActivityStarted(
            @NonNull Activity activity
    ) {
        // No action required.
    }

    @Override
    public void onActivityResumed(
            @NonNull Activity activity
    ) {
        if (!(activity
                instanceof AskStudySaathiActivity)) {

            return;
        }

        AskStudySaathiActivity askActivity =
                (AskStudySaathiActivity) activity;

        activeActivityReference =
                new WeakReference<>(
                        askActivity
                );

        startCooldownDiscovery();
    }

    @Override
    public void onActivityPaused(
            @NonNull Activity activity
    ) {
        if (!isObservedAskActivity(
                activity
        )) {
            return;
        }

        stopCooldownDiscovery();
    }

    @Override
    public void onActivityStopped(
            @NonNull Activity activity
    ) {
        // onActivityPaused already stops active UI work.
    }

    @Override
    public void onActivitySaveInstanceState(
            @NonNull Activity activity,
            @NonNull Bundle outState
    ) {
        // Cooldown is already persistent in SharedPreferences.
    }

    @Override
    public void onActivityDestroyed(
            @NonNull Activity activity
    ) {
        if (!isObservedAskActivity(
                activity
        )) {
            return;
        }

        stopCooldownDiscovery();

        activeActivityReference =
                new WeakReference<>(
                        null
                );

        previousQuestionHelperText =
                null;

        quotaUiApplied =
                false;

        observedCooldownUntilEpochMillis =
                0L;
    }

    private void startCooldownDiscovery() {
        pollingActive =
                true;

        mainHandler.removeCallbacks(
                cooldownDiscoveryRunnable
        );

        mainHandler.post(
                cooldownDiscoveryRunnable
        );
    }

    private void stopCooldownDiscovery() {
        pollingActive =
                false;

        mainHandler.removeCallbacks(
                cooldownDiscoveryRunnable
        );

        countdownController.cancel();
    }

    private void discoverAndApplyCooldown() {
        if (!pollingActive) {
            return;
        }

        AskStudySaathiActivity activity =
                getActiveAskActivity();

        if (!isActivityUsable(
                activity
        )) {
            stopCooldownDiscovery();
            return;
        }

        if (cooldownManager.isCooldownActive()) {
            long cooldownUntilEpochMillis =
                    cooldownManager
                            .getCooldownUntilEpochMillis();

            if (cooldownUntilEpochMillis > 0L
                    && (
                    cooldownUntilEpochMillis
                            != observedCooldownUntilEpochMillis
                            || !countdownController
                            .isCountdownRunning()
            )) {

                beginObservedCountdown(
                        activity,
                        cooldownUntilEpochMillis
                );
            }

        } else if (quotaUiApplied) {
            finishQuotaUi(
                    activity,
                    false
            );
        }

        if (pollingActive) {
            mainHandler.postDelayed(
                    cooldownDiscoveryRunnable,
                    COOLDOWN_DISCOVERY_INTERVAL_MILLIS
            );
        }
    }

    private void beginObservedCountdown(
            @NonNull AskStudySaathiActivity activity,
            long cooldownUntilEpochMillis
    ) {
        observedCooldownUntilEpochMillis =
                cooldownUntilEpochMillis;

        countdownController.start(
                cooldownUntilEpochMillis,
                new FirebaseAiQuotaCountdownController
                        .CountdownListener() {

                    @Override
                    public void onCooldownTick(
                            long remainingSeconds,
                            long ignoredCooldownUntilEpochMillis
                    ) {
                        AskStudySaathiActivity currentActivity =
                                getActiveAskActivity();

                        if (!isActivityUsable(
                                currentActivity
                        )) {
                            return;
                        }

                        applyQuotaUi(
                                currentActivity,
                                remainingSeconds
                        );
                    }

                    @Override
                    public void onCooldownFinished() {
                        AskStudySaathiActivity currentActivity =
                                getActiveAskActivity();

                        cooldownManager
                                .clearExpiredCooldown();

                        observedCooldownUntilEpochMillis =
                                0L;

                        if (!isActivityUsable(
                                currentActivity
                        )) {
                            quotaUiApplied =
                                    false;

                            previousQuestionHelperText =
                                    null;

                            return;
                        }

                        finishQuotaUi(
                                currentActivity,
                                true
                        );
                    }
                }
        );

        long initialRemainingSeconds =
                calculateRemainingSeconds(
                        cooldownUntilEpochMillis
                );

        if (initialRemainingSeconds > 0L) {
            applyQuotaUi(
                    activity,
                    initialRemainingSeconds
            );
        }
    }

    private void applyQuotaUi(
            @NonNull AskStudySaathiActivity activity,
            long remainingSeconds
    ) {
        MaterialButton askButton =
                activity.findViewById(
                        R.id.buttonAskSaathi
                );

        MaterialButton quickExplainButton =
                activity.findViewById(
                        R.id.buttonQuickExplain
                );

        MaterialButton quickKeyPointsButton =
                activity.findViewById(
                        R.id.buttonQuickKeyPoints
                );

        MaterialButton quickExampleButton =
                activity.findViewById(
                        R.id.buttonQuickExample
                );

        MaterialButton quickPracticeButton =
                activity.findViewById(
                        R.id.buttonQuickPractice
                );

        TextInputLayout questionInputLayout =
                activity.findViewById(
                        R.id.inputQuestion
                );

        if (askButton == null
                || quickExplainButton == null
                || quickKeyPointsButton == null
                || quickExampleButton == null
                || quickPracticeButton == null
                || questionInputLayout == null) {

            return;
        }

        if (!quotaUiApplied) {
            previousQuestionHelperText =
                    questionInputLayout
                            .getHelperText();
        }

        quotaUiApplied =
                true;

        askButton.setEnabled(
                false
        );

        quickExplainButton.setEnabled(
                false
        );

        quickKeyPointsButton.setEnabled(
                false
        );

        quickExampleButton.setEnabled(
                false
        );

        quickPracticeButton.setEnabled(
                false
        );

        askButton.setText(
                "दोबारा प्रयास: "
                        + formatCountdownDuration(
                        remainingSeconds
                )
        );

        questionInputLayout.setHelperText(
                createCooldownMessage(
                        remainingSeconds
                )
        );
    }

    private void finishQuotaUi(
            @NonNull AskStudySaathiActivity activity,
            boolean showCompletionMessage
    ) {
        MaterialButton askButton =
                activity.findViewById(
                        R.id.buttonAskSaathi
                );

        MaterialButton quickExplainButton =
                activity.findViewById(
                        R.id.buttonQuickExplain
                );

        MaterialButton quickKeyPointsButton =
                activity.findViewById(
                        R.id.buttonQuickKeyPoints
                );

        MaterialButton quickExampleButton =
                activity.findViewById(
                        R.id.buttonQuickExample
                );

        MaterialButton quickPracticeButton =
                activity.findViewById(
                        R.id.buttonQuickPractice
                );

        TextInputLayout questionInputLayout =
                activity.findViewById(
                        R.id.inputQuestion
                );

        TextInputEditText questionInput =
                activity.findViewById(
                        R.id.editQuestion
                );

        AutoCompleteTextView subjectDropdown =
                activity.findViewById(
                        R.id.dropdownAskSubject
                );

        View questionOcrProgress =
                activity.findViewById(
                        R.id.progressQuestionImageOcr
                );

        quotaUiApplied =
                false;

        observedCooldownUntilEpochMillis =
                0L;

        if (questionInputLayout != null) {
            questionInputLayout.setHelperText(
                    previousQuestionHelperText
            );
        }

        previousQuestionHelperText =
                null;

        if (askButton == null
                || quickExplainButton == null
                || quickKeyPointsButton == null
                || quickExampleButton == null
                || quickPracticeButton == null) {

            return;
        }

        askButton.setText(
                R.string.ask_study_saathi_button
        );

        boolean screenReadyForQuestion =
                questionInput != null
                        && questionInput.isEnabled()
                        && subjectDropdown != null
                        && subjectDropdown.isEnabled()
                        && (
                        questionOcrProgress == null
                                || questionOcrProgress
                                .getVisibility()
                                != View.VISIBLE
                );

        askButton.setEnabled(
                screenReadyForQuestion
        );

        quickExplainButton.setEnabled(
                screenReadyForQuestion
        );

        quickKeyPointsButton.setEnabled(
                screenReadyForQuestion
        );

        quickExampleButton.setEnabled(
                screenReadyForQuestion
        );

        quickPracticeButton.setEnabled(
                screenReadyForQuestion
        );

        if (showCompletionMessage
                && screenReadyForQuestion) {

            Snackbar.make(
                    askButton,
                    "Smart AI अब फिर से उपलब्ध है।",
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }

    private boolean isObservedAskActivity(
            @NonNull Activity activity
    ) {
        AskStudySaathiActivity observedActivity =
                activeActivityReference.get();

        return observedActivity != null
                && observedActivity == activity;
    }

    @Nullable
    private AskStudySaathiActivity getActiveAskActivity() {
        return activeActivityReference.get();
    }

    private boolean isActivityUsable(
            @Nullable AskStudySaathiActivity activity
    ) {
        return activity != null
                && !activity.isFinishing()
                && !activity.isDestroyed();
    }

    private long calculateRemainingSeconds(
            long cooldownUntilEpochMillis
    ) {
        long remainingMilliseconds =
                cooldownUntilEpochMillis
                        - System.currentTimeMillis();

        if (remainingMilliseconds <= 0L) {
            return 0L;
        }

        return Math.max(
                1L,
                (long) Math.ceil(
                        remainingMilliseconds
                                / 1000.0d
                )
        );
    }

    @NonNull
    private String createCooldownMessage(
            long remainingSeconds
    ) {
        return "Firebase AI की request limit अभी पूरी है। "
                + formatCountdownDuration(
                remainingSeconds
        )
                + " बाद दोबारा प्रयास करें।";
    }

    @NonNull
    private String formatCountdownDuration(
            long remainingSeconds
    ) {
        long safeRemainingSeconds =
                Math.max(
                        0L,
                        remainingSeconds
                );

        long minutes =
                safeRemainingSeconds / 60L;

        long seconds =
                safeRemainingSeconds % 60L;

        return String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                seconds
        );
    }
}