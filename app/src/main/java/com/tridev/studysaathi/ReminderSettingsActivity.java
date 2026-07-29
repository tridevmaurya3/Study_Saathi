package com.tridev.studysaathi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.tridev.studysaathi.databinding.ActivityReminderSettingsBinding;
import com.tridev.studysaathi.reminder.StudyReminderScheduler;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReminderSettingsActivity
        extends AppCompatActivity {

    private enum PendingPermissionAction {
        NONE,
        SAVE_REMINDER,
        SEND_TEST
    }

    private ActivityReminderSettingsBinding binding;

    private ActivityResultLauncher<String>
            notificationPermissionLauncher;

    private int selectedHour =
            StudyReminderScheduler.DEFAULT_REMINDER_HOUR;

    private int selectedMinute =
            StudyReminderScheduler.DEFAULT_REMINDER_MINUTE;

    private int selectedDaysMask =
            StudyReminderScheduler.ALL_DAYS_MASK;

    private boolean reminderEnabled;

    private boolean updatingReminderSwitch;
    private boolean updatingDaySelection;

    private PendingPermissionAction pendingPermissionAction =
            PendingPermissionAction.NONE;
    private long activeProfileId = -1L;
    private StudentProfileRepository studentProfileRepository;
    private final List<StudentProfileEntity> availableProfiles =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityReminderSettingsBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        registerNotificationPermissionLauncher();
        setupClickListeners();
        setupDaySelectionListener();
        studentProfileRepository = new StudentProfileRepository(this);
        loadActiveStudentReminder();
    }

    private void loadActiveStudentReminder() {
        studentProfileRepository.getAllProfiles(
                new StudentProfileRepository.ProfilesCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<StudentProfileEntity> profiles
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (profiles.isEmpty()) {
                            binding.textReminderStudent.setText(
                                    "पहले Student Profile चुनें");
                            binding.buttonSaveReminder.setEnabled(false);
                            binding.buttonTestReminder.setEnabled(false);
                            return;
                        }
                        availableProfiles.clear();
                        availableProfiles.addAll(profiles);
                        List<String> labels = new ArrayList<>();
                        int activePosition = 0;
                        for (int index = 0; index < profiles.size(); index++) {
                            StudentProfileEntity profile = profiles.get(index);
                            labels.add(profile.getStudentName()
                                    + " • " + profile.getStudentClass());
                            if (profile.isActive()) {
                                activePosition = index;
                            }
                        }
                        binding.spinnerReminderStudent.setAdapter(
                                new ArrayAdapter<>(
                                        ReminderSettingsActivity.this,
                                        android.R.layout.simple_spinner_dropdown_item,
                                        labels));
                        binding.spinnerReminderStudent.setSelection(
                                activePosition,
                                false);
                        binding.spinnerReminderStudent.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(
                                            AdapterView<?> parent,
                                            View view,
                                            int position,
                                            long id
                                    ) {
                                        if (position >= 0
                                                && position < availableProfiles.size()) {
                                            showReminderForStudent(
                                                    availableProfiles.get(position));
                                        }
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {
                                    }
                                });
                        showReminderForStudent(profiles.get(activePosition));
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (!isFinishing() && !isDestroyed()) {
                            binding.textReminderStudent.setText(
                                    "Student reminder load नहीं हो सका");
                        }
                    }
                });
    }

    private void showReminderForStudent(
            @NonNull StudentProfileEntity profile
    ) {
                        activeProfileId = profile.getProfileId();
                        binding.textReminderStudent.setText(
                                "Reminder for: " + profile.getStudentName()
                                        + " • " + profile.getStudentClass());
                        loadSavedSettings();
                        updateScreen();
                        showSavedReminderStatus();
    }

    private void registerNotificationPermissionLauncher() {
        notificationPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .RequestPermission(),
                        permissionGranted -> {
                            PendingPermissionAction completedAction =
                                    pendingPermissionAction;

                            pendingPermissionAction =
                                    PendingPermissionAction.NONE;

                            if (permissionGranted) {
                                continuePendingAction(
                                        completedAction
                                );
                            } else {
                                handlePermissionDenied(
                                        completedAction
                                );
                            }
                        }
                );
    }

    private void continuePendingAction(
            @NonNull PendingPermissionAction action
    ) {
        switch (action) {
            case SAVE_REMINDER:
                saveAndApplyReminder();
                break;

            case SEND_TEST:
                sendTestReminder();
                break;

            case NONE:
            default:
                break;
        }
    }

    private void handlePermissionDenied(
            @NonNull PendingPermissionAction action
    ) {
        if (action
                == PendingPermissionAction.SAVE_REMINDER
                && reminderEnabled) {

            reminderEnabled = false;

            updatingReminderSwitch = true;

            binding.switchStudyReminder.setChecked(
                    false
            );

            updatingReminderSwitch = false;

            updateScreen();
        }

        Snackbar.make(
                binding.getRoot(),
                R.string.study_reminder_permission_denied,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void loadSavedSettings() {
        reminderEnabled =
                StudyReminderScheduler
                        .isReminderEnabled(this, activeProfileId);

        selectedHour =
                StudyReminderScheduler
                        .getReminderHour(this, activeProfileId);

        selectedMinute =
                StudyReminderScheduler
                        .getReminderMinute(this, activeProfileId);

        selectedDaysMask =
                StudyReminderScheduler
                        .getReminderDaysMask(this, activeProfileId);

        updatingReminderSwitch = true;

        binding.switchStudyReminder.setChecked(
                reminderEnabled
        );

        updatingReminderSwitch = false;

        applySelectedDaysToButtons();
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.switchStudyReminder
                .setOnCheckedChangeListener(
                        (buttonView, isChecked) -> {
                            if (updatingReminderSwitch) {
                                return;
                            }

                            reminderEnabled = isChecked;
                            updateScreen();
                        }
                );

        binding.buttonChooseReminderTime
                .setOnClickListener(view ->
                        openTimePicker()
                );

        binding.buttonSaveReminder
                .setOnClickListener(view ->
                        validatePermissionAndSave()
                );

        binding.buttonTestReminder
                .setOnClickListener(view ->
                        validatePermissionAndSendTest()
                );
    }

    private void setupDaySelectionListener() {
        binding.toggleReminderDays
                .addOnButtonCheckedListener(
                        (group, checkedId, isChecked) -> {
                            if (updatingDaySelection) {
                                return;
                            }

                            int selectedDayBit =
                                    getDayBitForButton(
                                            checkedId
                                    );

                            if (selectedDayBit == 0) {
                                return;
                            }

                            if (isChecked) {
                                selectedDaysMask |=
                                        selectedDayBit;
                            } else {
                                selectedDaysMask &=
                                        ~selectedDayBit;
                            }

                            updateScreen();
                        }
                );
    }

    private void applySelectedDaysToButtons() {
        updatingDaySelection = true;

        binding.toggleReminderDays.clearChecked();

        checkDayButton(
                StudyReminderScheduler.DAY_MONDAY,
                R.id.buttonDayMonday
        );

        checkDayButton(
                StudyReminderScheduler.DAY_TUESDAY,
                R.id.buttonDayTuesday
        );

        checkDayButton(
                StudyReminderScheduler.DAY_WEDNESDAY,
                R.id.buttonDayWednesday
        );

        checkDayButton(
                StudyReminderScheduler.DAY_THURSDAY,
                R.id.buttonDayThursday
        );

        checkDayButton(
                StudyReminderScheduler.DAY_FRIDAY,
                R.id.buttonDayFriday
        );

        checkDayButton(
                StudyReminderScheduler.DAY_SATURDAY,
                R.id.buttonDaySaturday
        );

        checkDayButton(
                StudyReminderScheduler.DAY_SUNDAY,
                R.id.buttonDaySunday
        );

        updatingDaySelection = false;
    }

    private void checkDayButton(
            int dayBit,
            @IdRes int buttonId
    ) {
        if ((selectedDaysMask & dayBit) != 0) {
            binding.toggleReminderDays.check(
                    buttonId
            );
        }
    }

    private int getDayBitForButton(
            @IdRes int buttonId
    ) {
        if (buttonId == R.id.buttonDayMonday) {
            return StudyReminderScheduler.DAY_MONDAY;
        }

        if (buttonId == R.id.buttonDayTuesday) {
            return StudyReminderScheduler.DAY_TUESDAY;
        }

        if (buttonId == R.id.buttonDayWednesday) {
            return StudyReminderScheduler.DAY_WEDNESDAY;
        }

        if (buttonId == R.id.buttonDayThursday) {
            return StudyReminderScheduler.DAY_THURSDAY;
        }

        if (buttonId == R.id.buttonDayFriday) {
            return StudyReminderScheduler.DAY_FRIDAY;
        }

        if (buttonId == R.id.buttonDaySaturday) {
            return StudyReminderScheduler.DAY_SATURDAY;
        }

        if (buttonId == R.id.buttonDaySunday) {
            return StudyReminderScheduler.DAY_SUNDAY;
        }

        return 0;
    }

    private void openTimePicker() {
        int timeFormat =
                DateFormat.is24HourFormat(this)
                        ? TimeFormat.CLOCK_24H
                        : TimeFormat.CLOCK_12H;

        MaterialTimePicker timePicker =
                new MaterialTimePicker.Builder()
                        .setTimeFormat(timeFormat)
                        .setHour(selectedHour)
                        .setMinute(selectedMinute)
                        .setTitleText(
                                R.string.study_reminder_choose_time
                        )
                        .setInputMode(
                                MaterialTimePicker.INPUT_MODE_CLOCK
                        )
                        .build();

        timePicker.addOnPositiveButtonClickListener(
                view -> {
                    selectedHour =
                            timePicker.getHour();

                    selectedMinute =
                            timePicker.getMinute();

                    updateScreen();
                }
        );

        timePicker.show(
                getSupportFragmentManager(),
                "study_reminder_time_picker"
        );
    }

    private void validatePermissionAndSave() {
        if (reminderEnabled
                && selectedDaysMask == 0) {

            Snackbar.make(
                    binding.getRoot(),
                    R.string.study_reminder_choose_day_required,
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        if (!reminderEnabled) {
            saveAndApplyReminder();
            return;
        }

        if (needsNotificationPermission()) {
            pendingPermissionAction =
                    PendingPermissionAction.SAVE_REMINDER;

            notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            );

            return;
        }

        saveAndApplyReminder();
    }

    private void validatePermissionAndSendTest() {
        if (needsNotificationPermission()) {
            pendingPermissionAction =
                    PendingPermissionAction.SEND_TEST;

            notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            );

            return;
        }

        sendTestReminder();
    }

    private boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU

                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED;
    }

    private void sendTestReminder() {
        StudyReminderScheduler.sendTestReminder(
                this
        );

        Snackbar.make(
                binding.getRoot(),
                R.string.study_reminder_test_queued,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void saveAndApplyReminder() {
        int safeDaysMask =
                selectedDaysMask == 0
                        ? StudyReminderScheduler.ALL_DAYS_MASK
                        : selectedDaysMask;

        StudyReminderScheduler.saveSettings(
                this,
                activeProfileId,
                reminderEnabled,
                selectedHour,
                selectedMinute,
                safeDaysMask
        );

        if (reminderEnabled) {
            selectedDaysMask = safeDaysMask;

            StudyReminderScheduler.scheduleReminder(
                    this,
                    activeProfileId,
                    selectedHour,
                    selectedMinute,
                    selectedDaysMask
            );

            updateScreen();
            showSavedReminderStatus();

            Snackbar.make(
                    binding.getRoot(),
                    getString(
                            R.string.study_reminder_saved_days_format,
                            getFormattedTime(),
                            getSelectedDaysSummary()
                    ),
                    Snackbar.LENGTH_LONG
            ).show();

        } else {
            StudyReminderScheduler.cancelReminder(
                    this,
                    activeProfileId
            );

            updateScreen();
            showSavedReminderStatus();

            Snackbar.make(
                    binding.getRoot(),
                    R.string.study_reminder_disabled_message,
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }

    private void updateScreen() {
        binding.textSelectedReminderTime.setText(
                getFormattedTime()
        );

        binding.textSelectedDays.setText(
                getString(
                        R.string.study_reminder_selected_days_format,
                        getSelectedDaysSummary()
                )
        );

        binding.buttonChooseReminderTime.setEnabled(
                reminderEnabled
        );

        setDayButtonsEnabled(
                reminderEnabled
        );

        binding.cardReminderTime.setAlpha(
                reminderEnabled ? 1f : 0.55f
        );

        binding.cardReminderDays.setAlpha(
                reminderEnabled ? 1f : 0.55f
        );

        binding.buttonTestReminder.setEnabled(true);

        binding.buttonSaveReminder.setText(
                R.string.study_reminder_save_button
        );

        if (reminderEnabled) {
            binding.textReminderCurrentStatus.setText(
                    getString(
                            R.string.study_reminder_ready_days_status_format,
                            getFormattedTime(),
                            getSelectedDaysSummary()
                    )
            );
        } else {
            binding.textReminderCurrentStatus.setText(
                    R.string.study_reminder_disabled_status
            );
        }
    }

    private void showSavedReminderStatus() {
        if (reminderEnabled) {
            binding.textReminderCurrentStatus.setText(
                    getString(
                            R.string.study_reminder_enabled_days_status_format,
                            getFormattedTime(),
                            getSelectedDaysSummary()
                    )
            );
        } else {
            binding.textReminderCurrentStatus.setText(
                    R.string.study_reminder_disabled_status
            );
        }
    }

    private void setDayButtonsEnabled(
            boolean enabled
    ) {
        binding.buttonDayMonday.setEnabled(enabled);
        binding.buttonDayTuesday.setEnabled(enabled);
        binding.buttonDayWednesday.setEnabled(enabled);
        binding.buttonDayThursday.setEnabled(enabled);
        binding.buttonDayFriday.setEnabled(enabled);
        binding.buttonDaySaturday.setEnabled(enabled);
        binding.buttonDaySunday.setEnabled(enabled);
    }

    @NonNull
    private String getSelectedDaysSummary() {
        List<String> selectedDays =
                new ArrayList<>();

        if ((selectedDaysMask
                & StudyReminderScheduler.DAY_MONDAY) != 0) {
            selectedDays.add(
                    getString(R.string.reminder_day_monday)
            );
        }

        if ((selectedDaysMask
                & StudyReminderScheduler.DAY_TUESDAY) != 0) {
            selectedDays.add(
                    getString(R.string.reminder_day_tuesday)
            );
        }

        if ((selectedDaysMask
                & StudyReminderScheduler.DAY_WEDNESDAY) != 0) {
            selectedDays.add(
                    getString(R.string.reminder_day_wednesday)
            );
        }

        if ((selectedDaysMask
                & StudyReminderScheduler.DAY_THURSDAY) != 0) {
            selectedDays.add(
                    getString(R.string.reminder_day_thursday)
            );
        }

        if ((selectedDaysMask
                & StudyReminderScheduler.DAY_FRIDAY) != 0) {
            selectedDays.add(
                    getString(R.string.reminder_day_friday)
            );
        }

        if ((selectedDaysMask
                & StudyReminderScheduler.DAY_SATURDAY) != 0) {
            selectedDays.add(
                    getString(R.string.reminder_day_saturday)
            );
        }

        if ((selectedDaysMask
                & StudyReminderScheduler.DAY_SUNDAY) != 0) {
            selectedDays.add(
                    getString(R.string.reminder_day_sunday)
            );
        }

        if (selectedDays.isEmpty()) {
            return getString(
                    R.string.study_reminder_no_days_selected
            );
        }

        if (selectedDaysMask
                == StudyReminderScheduler.ALL_DAYS_MASK) {

            return getString(
                    R.string.study_reminder_every_day
            );
        }

        return String.join(
                ", ",
                selectedDays
        );
    }

    @NonNull
    private String getFormattedTime() {
        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                selectedHour
        );

        calendar.set(
                Calendar.MINUTE,
                selectedMinute
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        return DateFormat
                .getTimeFormat(this)
                .format(
                        calendar.getTime()
                );
    }
}
