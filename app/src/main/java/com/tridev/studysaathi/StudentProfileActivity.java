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

    public static final String EXTRA_EDIT_PROFILE_ID =
            "extra_edit_profile_id";

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
            "Class 1",
            "Class 2",
            "Class 3",
            "Class 4",
            "Class 5",
            "Class 6",
            "Class 7",
            "Class 8",
            "Class 9",
            "Class 10",
            "Class 11",
            "Class 12"
    };

    private final String[] mediumOptions = {
            "English Medium",
            "Hindi Medium",
            "Other Medium"
    };

    private final String[] explanationOptions = {
            "Hindi + English",
            "Hindi",
            "English"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStudentProfileBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        setupDropdowns();
        setupClickListeners();
        editProfileId = getIntent().getLongExtra(
                EXTRA_EDIT_PROFILE_ID,
                0L
        );
        if (editProfileId > 0L) {
            loadProfileForEditing();
        }
    }

    private void setupDropdowns() {
        ArrayAdapter<String> boardAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                boardOptions
        );

        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                classOptions
        );

        ArrayAdapter<String> mediumAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                mediumOptions
        );

        ArrayAdapter<String> explanationAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                explanationOptions
        );

        binding.dropdownBoard.setAdapter(boardAdapter);
        binding.dropdownClass.setAdapter(classAdapter);
        binding.dropdownMedium.setAdapter(mediumAdapter);

        binding.dropdownExplanationLanguage.setAdapter(
                explanationAdapter
        );

        binding.dropdownExplanationLanguage.setText(
                explanationOptions[0],
                false
        );
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

        String selectedBoard =
                getSelectedValue(
                        binding.dropdownBoard.getText()
                );

        String selectedClass =
                getSelectedValue(
                        binding.dropdownClass.getText()
                );

        String selectedMedium =
                getSelectedValue(
                        binding.dropdownMedium.getText()
                );

        String selectedExplanationLanguage =
                getSelectedValue(
                        binding.dropdownExplanationLanguage.getText()
                );

        boolean isValid = true;

        if (TextUtils.isEmpty(studentName)) {
            binding.inputLayoutStudentName.setError(
                    getString(
                            R.string.error_student_name_required
                    )
            );

            isValid = false;
        }

        if (TextUtils.isEmpty(selectedBoard)) {
            binding.inputLayoutBoard.setError(
                    getString(
                            R.string.error_board_required
                    )
            );

            isValid = false;
        }

        if (TextUtils.isEmpty(selectedClass)) {
            binding.inputLayoutClass.setError(
                    getString(
                            R.string.error_class_required
                    )
            );

            isValid = false;
        }

        if (TextUtils.isEmpty(selectedMedium)) {
            binding.inputLayoutMedium.setError(
                    getString(
                            R.string.error_medium_required
                    )
            );

            isValid = false;
        }

        if (TextUtils.isEmpty(selectedExplanationLanguage)) {
            binding.inputLayoutExplanationLanguage.setError(
                    getString(
                            R.string.error_language_required
                    )
            );

            isValid = false;
        }

        if (!isValid) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.complete_required_information,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        StudentProfileEntity studentProfile =
                createStudentProfileEntity(
                        studentName,
                        selectedBoard,
                        selectedClass,
                        selectedMedium,
                        selectedExplanationLanguage
                );

        showSavingState(true);
        if (editingProfile != null) {
            editingProfile.setStudentName(studentName);
            editingProfile.setEducationBoard(selectedBoard);
            editingProfile.setStudentClass(selectedClass);
            editingProfile.setStudyMedium(selectedMedium);
            editingProfile.setExplanationLanguage(
                    selectedExplanationLanguage
            );
            updateStudentProfile(editingProfile);
        } else {
            insertStudentProfile(studentProfile);
        }
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
                        binding.dropdownBoard.setText(profile.getEducationBoard(), false);
                        binding.dropdownClass.setText(profile.getStudentClass(), false);
                        binding.dropdownMedium.setText(profile.getStudyMedium(), false);
                        binding.dropdownExplanationLanguage.setText(
                                profile.getExplanationLanguage(),
                                false
                        );
                        binding.buttonContinue.setText("Save Changes");
                        showSavingState(false);
                        binding.buttonContinue.setText("Save Changes");
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (!isFinishing() && !isDestroyed()) {
                            finish();
                        }
                    }
                }
        );
    }

    private void updateStudentProfile(
            @NonNull StudentProfileEntity profile
    ) {
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
                        binding.buttonContinue.setText("Save Changes");
                        Snackbar.make(
                                binding.getRoot(),
                                "Profile update नहीं हो सका।",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private StudentProfileEntity createStudentProfileEntity(
            @NonNull String studentName,
            @NonNull String educationBoard,
            @NonNull String studentClass,
            @NonNull String studyMedium,
            @NonNull String explanationLanguage
    ) {
        long currentTime = System.currentTimeMillis();

        StudentProfileEntity studentProfile =
                new StudentProfileEntity();

        studentProfile.setStudentName(studentName);
        studentProfile.setEducationBoard(educationBoard);
        studentProfile.setStudentClass(studentClass);
        studentProfile.setStudyMedium(studyMedium);

        studentProfile.setExplanationLanguage(
                explanationLanguage
        );

        studentProfile.setActive(false);
        studentProfile.setCreatedAt(currentTime);
        studentProfile.setUpdatedAt(currentTime);

        return studentProfile;
    }

    private void insertStudentProfile(
            @NonNull StudentProfileEntity studentProfile
    ) {
        studentProfileRepository.insertProfile(
                studentProfile,
                new StudentProfileRepository.InsertProfileCallback() {
                    @Override
                    public void onSuccess(long insertedProfileId) {
                        activateInsertedProfile(insertedProfileId);
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showSavingState(false);

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.profile_save_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void activateInsertedProfile(
            long insertedProfileId
    ) {
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
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showSavingState(false);

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.profile_activation_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void openDashboard() {
        Intent dashboardIntent = new Intent(
                StudentProfileActivity.this,
                DashboardActivity.class
        );

        /*
         * Profile बनने के बाद Welcome और Profile screens को
         * back stack से हटाकर Dashboard को नई root screen बनाता है।
         */
        dashboardIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(dashboardIntent);
    }

    private void showSavingState(boolean saving) {
        profileSaveInProgress = saving;

        binding.buttonContinue.setEnabled(!saving);
        binding.buttonBack.setEnabled(!saving);

        binding.editStudentName.setEnabled(!saving);
        binding.dropdownBoard.setEnabled(!saving);
        binding.dropdownClass.setEnabled(!saving);
        binding.dropdownMedium.setEnabled(!saving);

        binding.dropdownExplanationLanguage.setEnabled(
                !saving
        );

        binding.buttonContinue.setText(
                saving
                        ? R.string.saving_profile
                        : R.string.continue_button
        );
    }

    private void clearErrors() {
        binding.inputLayoutStudentName.setError(null);
        binding.inputLayoutBoard.setError(null);
        binding.inputLayoutClass.setError(null);
        binding.inputLayoutMedium.setError(null);

        binding.inputLayoutExplanationLanguage.setError(
                null
        );
    }

    private String getStudentName() {
        if (binding.editStudentName.getText() == null) {
            return "";
        }

        return binding.editStudentName
                .getText()
                .toString()
                .trim();
    }

    private String getSelectedValue(
            CharSequence value
    ) {
        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }
}
