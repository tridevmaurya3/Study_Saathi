package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityStudentProfileBinding;

public class StudentProfileActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_PROFILE_ID = "extra_edit_profile_id";

    private ActivityStudentProfileBinding binding;
    private StudentProfileRepository studentProfileRepository;
    private boolean profileSaveInProgress;
    private long editProfileId;
    private StudentProfileEntity editingProfile;

    private final String[] boardOptions = {
            "CBSE",
            "CISCE – ICSE / ISC",
            "State Board / SCERT",
            "NIOS",
            "IB - International Baccalaureate",
            "Cambridge - CAIE",
            "Other Board / Custom School"
    };

    private final String[] classOptions = {
            "Class 1", "Class 2", "Class 3", "Class 4",
            "Class 5", "Class 6", "Class 7", "Class 8",
            "Class 9", "Class 10", "Class 11", "Class 12"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studentProfileRepository = new StudentProfileRepository(this);
        setupDropdowns();
        setupClickListeners();

        editProfileId = getIntent().getLongExtra(EXTRA_EDIT_PROFILE_ID, 0L);
        if (editProfileId > 0L) {
            loadProfileForEditing();
        }
    }

    private void setupDropdowns() {
        String[] mediumOptions = {
                getString(R.string.profile_medium_english),
                getString(R.string.profile_medium_hindi),
                getString(R.string.profile_medium_other)
        };
        String[] explanationOptions = {
                getString(R.string.profile_explanation_english),
                getString(R.string.profile_explanation_hindi),
                getString(R.string.profile_explanation_hinglish)
        };

        binding.dropdownBoard.setAdapter(new ArrayAdapter<>(
                this, R.layout.item_professional_dropdown, boardOptions));
        binding.dropdownClass.setAdapter(new ArrayAdapter<>(
                this, R.layout.item_professional_dropdown, classOptions));
        binding.dropdownMedium.setAdapter(new ArrayAdapter<>(
                this, R.layout.item_professional_dropdown, mediumOptions));
        binding.dropdownExplanationLanguage.setAdapter(new ArrayAdapter<>(
                this, R.layout.item_professional_dropdown, explanationOptions));

        // Preserve the old bilingual-first default under its final name.
        binding.dropdownExplanationLanguage.setText(
                getExplanationDisplayLabel(
                        StudentProfileEntity.EXPLANATION_LANGUAGE_HINGLISH),
                false);
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view -> {
            if (!profileSaveInProgress) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        binding.buttonContinue.setOnClickListener(view -> {
            if (!profileSaveInProgress) {
                validateAndSaveProfile();
            }
        });
    }

    private void validateAndSaveProfile() {
        clearErrors();

        String studentName = getStudentName();
        String selectedBoard = textOf(binding.dropdownBoard.getText());
        String selectedClass = textOf(binding.dropdownClass.getText());
        String selectedMedium = StudentProfileEntity.normalizeStudyMedium(
                textOf(binding.dropdownMedium.getText()));
        String selectedExplanationLanguage =
                StudentProfileEntity.normalizeExplanationLanguage(
                        textOf(binding.dropdownExplanationLanguage.getText()));

        boolean valid = true;
        if (TextUtils.isEmpty(studentName)) {
            binding.inputLayoutStudentName.setError(
                    getString(R.string.error_student_name_required));
            valid = false;
        }
        if (TextUtils.isEmpty(selectedBoard)) {
            binding.inputLayoutBoard.setError(
                    getString(R.string.error_board_required));
            valid = false;
        }
        if (TextUtils.isEmpty(selectedClass)) {
            binding.inputLayoutClass.setError(
                    getString(R.string.error_class_required));
            valid = false;
        }
        if (TextUtils.isEmpty(selectedMedium)) {
            binding.inputLayoutMedium.setError(
                    getString(R.string.error_medium_required));
            valid = false;
        }
        if (TextUtils.isEmpty(selectedExplanationLanguage)) {
            binding.inputLayoutExplanationLanguage.setError(
                    getString(R.string.error_language_required));
            valid = false;
        }

        if (!valid) {
            Snackbar.make(binding.getRoot(),
                    R.string.complete_required_information,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        showSavingState(true);
        if (editingProfile != null) {
            editingProfile.setStudentName(studentName);
            editingProfile.setEducationBoard(selectedBoard);
            editingProfile.setStudentClass(selectedClass);
            editingProfile.setStudyMedium(selectedMedium);
            editingProfile.setExplanationLanguage(selectedExplanationLanguage);
            updateStudentProfile(editingProfile);
            return;
        }

        insertStudentProfile(createStudentProfileEntity(
                studentName,
                selectedBoard,
                selectedClass,
                selectedMedium,
                selectedExplanationLanguage));
    }

    private void loadProfileForEditing() {
        showSavingState(true);
        studentProfileRepository.getProfileById(
                editProfileId,
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(StudentProfileEntity profile) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (profile == null) {
                            finish();
                            return;
                        }

                        editingProfile = profile;
                        binding.editStudentName.setText(profile.getStudentName());
                        binding.dropdownBoard.setText(
                                profile.getEducationBoard(), false);
                        binding.dropdownClass.setText(
                                profile.getStudentClass(), false);
                        binding.dropdownMedium.setText(
                                getMediumDisplayLabel(profile.getStudyMedium()), false);
                        binding.dropdownExplanationLanguage.setText(
                                getExplanationDisplayLabel(
                                        profile.getExplanationLanguage()), false);
                        showSavingState(false);
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (!isFinishing() && !isDestroyed()) {
                            finish();
                        }
                    }
                });
    }

    private void updateStudentProfile(@NonNull StudentProfileEntity profile) {
        studentProfileRepository.updateProfile(
                profile,
                new StudentProfileRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        showSavingState(false);
                        Snackbar.make(binding.getRoot(),
                                R.string.profile_update_failed,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private StudentProfileEntity createStudentProfileEntity(
            @NonNull String studentName,
            @NonNull String educationBoard,
            @NonNull String studentClass,
            @NonNull String studyMedium,
            @NonNull String explanationLanguage
    ) {
        long now = System.currentTimeMillis();
        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setStudentName(studentName);
        profile.setEducationBoard(educationBoard);
        profile.setStudentClass(studentClass);
        profile.setStudyMedium(studyMedium);
        profile.setExplanationLanguage(explanationLanguage);
        profile.setActive(false);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        return profile;
    }

    private void insertStudentProfile(@NonNull StudentProfileEntity profile) {
        studentProfileRepository.insertProfile(
                profile,
                new StudentProfileRepository.InsertProfileCallback() {
                    @Override
                    public void onSuccess(long insertedProfileId) {
                        activateInsertedProfile(insertedProfileId);
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        showSavingState(false);
                        Snackbar.make(binding.getRoot(),
                                R.string.profile_save_failed,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void activateInsertedProfile(long insertedProfileId) {
        studentProfileRepository.activateProfile(
                insertedProfileId,
                new StudentProfileRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        showSavingState(false);
                        openDashboard();
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        showSavingState(false);
                        Snackbar.make(binding.getRoot(),
                                R.string.profile_activation_failed,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void openDashboard() {
        Intent intent = new Intent(
                StudentProfileActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showSavingState(boolean saving) {
        profileSaveInProgress = saving;
        binding.buttonContinue.setEnabled(!saving);
        binding.buttonBack.setEnabled(!saving);
        binding.editStudentName.setEnabled(!saving);
        binding.dropdownBoard.setEnabled(!saving);
        binding.dropdownClass.setEnabled(!saving);
        binding.dropdownMedium.setEnabled(!saving);
        binding.dropdownExplanationLanguage.setEnabled(!saving);

        if (saving) {
            binding.buttonContinue.setText(R.string.saving_profile);
        } else if (editProfileId > 0L || editingProfile != null) {
            binding.buttonContinue.setText(R.string.profile_save_changes);
        } else {
            binding.buttonContinue.setText(R.string.continue_button);
        }
    }

    private void clearErrors() {
        binding.inputLayoutStudentName.setError(null);
        binding.inputLayoutBoard.setError(null);
        binding.inputLayoutClass.setError(null);
        binding.inputLayoutMedium.setError(null);
        binding.inputLayoutExplanationLanguage.setError(null);
    }

    @NonNull
    private String getMediumDisplayLabel(String storedValue) {
        String normalized = StudentProfileEntity.normalizeStudyMedium(storedValue);
        if (StudentProfileEntity.STUDY_MEDIUM_HINDI.equals(normalized)) {
            return getString(R.string.profile_medium_hindi);
        }
        if (StudentProfileEntity.STUDY_MEDIUM_OTHER.equals(normalized)) {
            return getString(R.string.profile_medium_other);
        }
        if (StudentProfileEntity.STUDY_MEDIUM_ENGLISH.equals(normalized)) {
            return getString(R.string.profile_medium_english);
        }
        return storedValue == null ? "" : storedValue.trim();
    }

    @NonNull
    private String getExplanationDisplayLabel(String storedValue) {
        String normalized =
                StudentProfileEntity.normalizeExplanationLanguage(storedValue);
        if (StudentProfileEntity.EXPLANATION_LANGUAGE_HINDI.equals(normalized)) {
            return getString(R.string.profile_explanation_hindi);
        }
        if (StudentProfileEntity.EXPLANATION_LANGUAGE_ENGLISH.equals(normalized)) {
            return getString(R.string.profile_explanation_english);
        }
        if (StudentProfileEntity.EXPLANATION_LANGUAGE_HINGLISH.equals(normalized)) {
            return getString(R.string.profile_explanation_hinglish);
        }
        return storedValue == null ? "" : storedValue.trim();
    }

    private String getStudentName() {
        return binding.editStudentName.getText() == null
                ? ""
                : binding.editStudentName.getText().toString().trim();
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
