package com.tridev.studysaathi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private static final String GOAL_PREFERENCES_NAME =
            "study_saathi_daily_goals";

    private static final String GOAL_KEY_PREFIX =
            "daily_goal_profile_";

    private static final int DEFAULT_DAILY_GOAL = 3;

    private ActivitySettingsBinding binding;

    private StudentProfileRepository studentProfileRepository;
    private SharedPreferences goalPreferences;

    private StudentProfileEntity activeStudentProfile;

    private int selectedDailyGoal =
            DEFAULT_DAILY_GOAL;

    private boolean updatingGoalSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        goalPreferences = getSharedPreferences(
                GOAL_PREFERENCES_NAME,
                MODE_PRIVATE
        );

        setupClickListeners();
        setupAppearanceControls();
        setupDailyGoalControls();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadActiveProfile();
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonManageProfiles.setOnClickListener(view ->
                openStudentProfiles()
        );

        binding.buttonOpenCloudAccount.setOnClickListener(view ->
                openCloudAccount()
        );

        binding.buttonOpenParentDashboard.setOnClickListener(view ->
                openParentDashboard()
        );

        binding.buttonOpenBackupExport.setOnClickListener(view ->
                openBackupExport()
        );

        binding.buttonOpenBackupRestore.setOnClickListener(view ->
                openBackupRestore()
        );

        binding.buttonOpenWeeklyStudy.setOnClickListener(view ->
                openWeeklyStudy()
        );

        binding.buttonOpenGlobalSearch.setOnClickListener(view ->
                openGlobalSearch()
        );

        binding.buttonOpenBookmarks.setOnClickListener(view ->
                openBookmarks()
        );

        binding.buttonOpenReminderSettings.setOnClickListener(view ->
                openReminderSettings()
        );

        binding.buttonOpenNotesLibrary.setOnClickListener(view ->
                openNotesLibrary()
        );

        binding.buttonOpenDoubtHistory.setOnClickListener(view ->
                openDoubtHistory()
        );
    }

    private void setupDailyGoalControls() {
        binding.toggleSettingsDailyGoal
                .addOnButtonCheckedListener(
                        (group, checkedId, isChecked) -> {
                            if (!isChecked
                                    || updatingGoalSelection
                                    || activeStudentProfile == null) {
                                return;
                            }

                            int newDailyGoal =
                                    getGoalForButton(
                                            checkedId
                                    );

                            if (newDailyGoal <= 0) {
                                return;
                            }

                            selectedDailyGoal =
                                    newDailyGoal;

                            saveDailyGoalForActiveProfile();
                            updateDailyGoalStatus();

                            Snackbar.make(
                                    binding.getRoot(),
                                    getString(
                                            R.string.settings_daily_goal_saved_format,
                                            selectedDailyGoal
                                    ),
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                );

        setDailyGoalControlsEnabled(false);
    }

    private void setupAppearanceControls() {
        String savedTheme =
                AppAppearancePreferences.getTheme(
                        this
                );

        if (AppAppearancePreferences.THEME_LIGHT
                .equals(savedTheme)) {
            binding.toggleSettingsTheme.check(
                    R.id.buttonThemeLight
            );
        } else if (AppAppearancePreferences.THEME_DARK
                .equals(savedTheme)) {
            binding.toggleSettingsTheme.check(
                    R.id.buttonThemeDark
            );
        } else {
            binding.toggleSettingsTheme.check(
                    R.id.buttonThemeSystem
            );
        }

        String savedLanguage =
                AppAppearancePreferences.getLanguage(
                        this
                );

        if (AppAppearancePreferences.LANGUAGE_HINDI
                .equals(savedLanguage)) {
            binding.toggleSettingsLanguage.check(
                    R.id.buttonLanguageHindi
            );
        } else if (AppAppearancePreferences.LANGUAGE_ENGLISH
                .equals(savedLanguage)) {
            binding.toggleSettingsLanguage.check(
                    R.id.buttonLanguageEnglish
            );
        } else {
            binding.toggleSettingsLanguage.check(
                    R.id.buttonLanguageBilingual
            );
        }

        binding.toggleSettingsTheme
                .addOnButtonCheckedListener(
                        (group, checkedId, isChecked) -> {
                            if (!isChecked) {
                                return;
                            }

                            String theme =
                                    AppAppearancePreferences
                                            .THEME_SYSTEM;

                            if (checkedId
                                    == R.id.buttonThemeLight) {
                                theme =
                                        AppAppearancePreferences
                                                .THEME_LIGHT;
                            } else if (checkedId
                                    == R.id.buttonThemeDark) {
                                theme =
                                        AppAppearancePreferences
                                                .THEME_DARK;
                            }

                            AppAppearancePreferences
                                    .saveAndApplyTheme(
                                            this,
                                            theme
                                    );
                        }
                );

        binding.toggleSettingsLanguage
                .addOnButtonCheckedListener(
                        (group, checkedId, isChecked) -> {
                            if (!isChecked) {
                                return;
                            }

                            String language =
                                    AppAppearancePreferences
                                            .LANGUAGE_BILINGUAL;

                            if (checkedId
                                    == R.id.buttonLanguageHindi) {
                                language =
                                        AppAppearancePreferences
                                                .LANGUAGE_HINDI;
                            } else if (checkedId
                                    == R.id.buttonLanguageEnglish) {
                                language =
                                        AppAppearancePreferences
                                                .LANGUAGE_ENGLISH;
                            }

                            AppAppearancePreferences
                                    .saveAndApplyLanguage(
                                            this,
                                            language
                                    );
                        }
                );
    }

    private void loadActiveProfile() {
        showLoadingState(true);

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository
                        .SingleProfileCallback() {

                    @Override
                    public void onSuccess(
                            StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);

                        if (studentProfile == null) {
                            showNoActiveProfileState();
                            return;
                        }

                        showActiveProfileState(
                                studentProfile
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);
                        showNoActiveProfileState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.settings_profile_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showActiveProfileState(
            @NonNull StudentProfileEntity studentProfile
    ) {
        activeStudentProfile =
                studentProfile;

        binding.textSettingsStudentName.setText(
                studentProfile.getStudentName()
        );

        binding.textSettingsStudentDetails.setText(
                getString(
                        R.string.settings_profile_detail_format,
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );

        binding.textSettingsProfileStatus.setText(
                R.string.settings_active_profile_available
        );

        binding.cardSettingsProfile.setAlpha(1f);

        selectedDailyGoal =
                sanitizeDailyGoal(
                        goalPreferences.getInt(
                                GOAL_KEY_PREFIX
                                        + studentProfile.getProfileId(),
                                DEFAULT_DAILY_GOAL
                        )
                );

        applyDailyGoalSelection();
        setDailyGoalControlsEnabled(true);
        updateDailyGoalStatus();

        binding.buttonOpenCloudAccount.setEnabled(true);
        binding.buttonOpenParentDashboard.setEnabled(true);
        binding.buttonOpenBackupExport.setEnabled(true);
        binding.buttonOpenBackupRestore.setEnabled(true);
        binding.buttonOpenWeeklyStudy.setEnabled(true);
        binding.buttonOpenGlobalSearch.setEnabled(true);
        binding.buttonOpenBookmarks.setEnabled(true);
        binding.buttonOpenNotesLibrary.setEnabled(true);
        binding.buttonOpenDoubtHistory.setEnabled(true);
    }

    private void showNoActiveProfileState() {
        activeStudentProfile = null;

        selectedDailyGoal =
                DEFAULT_DAILY_GOAL;

        binding.textSettingsStudentName.setText(
                R.string.settings_no_active_profile
        );

        binding.textSettingsStudentDetails.setText(
                R.string.settings_no_active_profile_description
        );

        binding.textSettingsProfileStatus.setText(
                R.string.settings_profile_required_status
        );

        binding.cardSettingsProfile.setAlpha(0.72f);

        applyDailyGoalSelection();
        setDailyGoalControlsEnabled(false);

        binding.textSettingsDailyGoalStatus.setText(
                R.string.settings_daily_goal_unavailable
        );

        binding.buttonOpenCloudAccount.setEnabled(true);
        binding.buttonOpenParentDashboard.setEnabled(true);
        binding.buttonOpenBackupExport.setEnabled(true);
        binding.buttonOpenBackupRestore.setEnabled(true);
        binding.buttonOpenWeeklyStudy.setEnabled(false);
        binding.buttonOpenGlobalSearch.setEnabled(false);
        binding.buttonOpenBookmarks.setEnabled(false);
        binding.buttonOpenNotesLibrary.setEnabled(false);
        binding.buttonOpenDoubtHistory.setEnabled(false);
    }

    private void saveDailyGoalForActiveProfile() {
        if (activeStudentProfile == null) {
            return;
        }

        goalPreferences.edit()
                .putInt(
                        GOAL_KEY_PREFIX
                                + activeStudentProfile
                                .getProfileId(),
                        selectedDailyGoal
                )
                .apply();
    }

    private void applyDailyGoalSelection() {
        updatingGoalSelection = true;

        binding.toggleSettingsDailyGoal.check(
                getButtonForGoal(
                        selectedDailyGoal
                )
        );

        updatingGoalSelection = false;
    }

    private void updateDailyGoalStatus() {
        if (activeStudentProfile == null) {
            binding.textSettingsDailyGoalStatus.setText(
                    R.string.settings_daily_goal_unavailable
            );

            return;
        }

        binding.textSettingsDailyGoalStatus.setText(
                getString(
                        R.string.settings_daily_goal_status_format,
                        activeStudentProfile.getStudentName(),
                        selectedDailyGoal
                )
        );
    }

    @IdRes
    private int getButtonForGoal(int goal) {
        if (goal == 1) {
            return R.id.buttonSettingsGoalOne;
        }

        if (goal == 5) {
            return R.id.buttonSettingsGoalFive;
        }

        return R.id.buttonSettingsGoalThree;
    }

    private int getGoalForButton(
            @IdRes int buttonId
    ) {
        if (buttonId
                == R.id.buttonSettingsGoalOne) {
            return 1;
        }

        if (buttonId
                == R.id.buttonSettingsGoalThree) {
            return 3;
        }

        if (buttonId
                == R.id.buttonSettingsGoalFive) {
            return 5;
        }

        return -1;
    }

    private int sanitizeDailyGoal(int goal) {
        if (goal == 1
                || goal == 3
                || goal == 5) {
            return goal;
        }

        return DEFAULT_DAILY_GOAL;
    }

    private void setDailyGoalControlsEnabled(
            boolean enabled
    ) {
        binding.buttonSettingsGoalOne.setEnabled(
                enabled
        );

        binding.buttonSettingsGoalThree.setEnabled(
                enabled
        );

        binding.buttonSettingsGoalFive.setEnabled(
                enabled
        );

        binding.cardSettingsDailyGoal.setAlpha(
                enabled ? 1f : 0.60f
        );
    }

    private void openStudentProfiles() {
        startActivity(
                new Intent(
                        SettingsActivity.this,
                        StudentProfilesActivity.class
                )
        );
    }

    private void openCloudAccount() {
        startActivity(
                new Intent(
                        SettingsActivity.this,
                        CloudAccountActivity.class
                )
        );
    }

    private void openParentDashboard() {
        startActivity(
                new Intent(
                        SettingsActivity.this,
                        ParentDashboardActivity.class
                )
        );
    }

    private void openBackupExport() {
        startActivity(
                new Intent(
                        SettingsActivity.this,
                        BackupExportActivity.class
                )
        );
    }

    private void openBackupRestore() {
        startActivity(
                new Intent(
                        SettingsActivity.this,
                        BackupRestoreActivity.class
                )
        );
    }

    private void openWeeklyStudy() {
        if (activeStudentProfile == null) {
            showProfileRequiredMessage();
            return;
        }

        startActivity(
                new Intent(
                        SettingsActivity.this,
                        WeeklyStudyActivity.class
                )
        );
    }

    private void openGlobalSearch() {
        if (activeStudentProfile == null) {
            showProfileRequiredMessage();
            return;
        }

        startActivity(
                new Intent(
                        SettingsActivity.this,
                        GlobalSearchActivity.class
                )
        );
    }

    private void openBookmarks() {
        if (activeStudentProfile == null) {
            showProfileRequiredMessage();
            return;
        }

        startActivity(
                new Intent(
                        SettingsActivity.this,
                        BookmarksActivity.class
                )
        );
    }

    private void openReminderSettings() {
        startActivity(
                new Intent(
                        SettingsActivity.this,
                        ReminderSettingsActivity.class
                )
        );
    }

    private void openNotesLibrary() {
        if (activeStudentProfile == null) {
            showProfileRequiredMessage();
            return;
        }

        startActivity(
                new Intent(
                        SettingsActivity.this,
                        AllChapterNotesActivity.class
                )
        );
    }

    private void openDoubtHistory() {
        if (activeStudentProfile == null) {
            showProfileRequiredMessage();
            return;
        }

        startActivity(
                new Intent(
                        SettingsActivity.this,
                        DoubtHistoryActivity.class
                )
        );
    }

    private void showProfileRequiredMessage() {
        Snackbar.make(
                binding.getRoot(),
                R.string.settings_profile_required_message,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void showLoadingState(boolean loading) {
        binding.progressSettings.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.contentSettings.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }
}
