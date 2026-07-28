package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tridev.studysaathi.adapter.StudentProfileAdapter;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityStudentProfilesBinding;

import java.util.ArrayList;
import java.util.List;

public class StudentProfilesActivity extends AppCompatActivity {

    private ActivityStudentProfilesBinding binding;
    private StudentProfileRepository studentProfileRepository;
    private StudentProfileAdapter studentProfileAdapter;

    private boolean profileActivationInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStudentProfilesBinding.inflate(
                getLayoutInflater()
        );
        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        setupRecyclerView();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        studentProfileAdapter = new StudentProfileAdapter(
                new ArrayList<>(),
                this::handleProfileSelection,
                new StudentProfileAdapter.OnProfileActionListener() {
                    @Override
                    public void onEdit(@NonNull StudentProfileEntity profile) {
                        openEditStudentProfile(profile);
                    }

                    @Override
                    public void onDelete(@NonNull StudentProfileEntity profile) {
                        confirmProfileDeletion(profile);
                    }
                }
        );

        binding.recyclerStudentProfiles.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerStudentProfiles.setAdapter(
                studentProfileAdapter
        );

        binding.recyclerStudentProfiles.setHasFixedSize(false);
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view -> {
            if (!profileActivationInProgress) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        binding.buttonAddStudent.setOnClickListener(view -> {
            if (!profileActivationInProgress) {
                openCreateStudentProfile();
            }
        });
    }

    private void loadStudentProfiles() {
        showLoadingState(true);

        studentProfileRepository.getAllProfiles(
                new StudentProfileRepository.ProfilesCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<StudentProfileEntity> profiles
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);
                        showStudentProfiles(profiles);
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);
                        showEmptyState(true);

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.student_profiles_loading_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentProfiles(
            @NonNull List<StudentProfileEntity> profiles
    ) {
        studentProfileAdapter.submitList(profiles);

        boolean profilesAvailable = !profiles.isEmpty();

        binding.recyclerStudentProfiles.setVisibility(
                profilesAvailable ? View.VISIBLE : View.GONE
        );

        showEmptyState(!profilesAvailable);
    }

    private void handleProfileSelection(
            @NonNull StudentProfileEntity studentProfile
    ) {
        if (profileActivationInProgress) {
            return;
        }

        if (studentProfile.isActive()) {
            finish();
            return;
        }

        activateStudentProfile(studentProfile.getProfileId());
    }

    private void activateStudentProfile(long profileId) {
        showActivationState(true);

        studentProfileRepository.activateProfile(
                profileId,
                new StudentProfileRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showActivationState(false);
                        openDashboardAsRoot();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showActivationState(false);

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.student_profile_activation_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void openCreateStudentProfile() {
        Intent profileIntent = new Intent(
                StudentProfilesActivity.this,
                StudentProfileActivity.class
        );

        startActivity(profileIntent);
    }

    private void openEditStudentProfile(
            @NonNull StudentProfileEntity profile
    ) {
        Intent profileIntent = new Intent(
                this,
                StudentProfileActivity.class
        );
        profileIntent.putExtra(
                StudentProfileActivity.EXTRA_EDIT_PROFILE_ID,
                profile.getProfileId()
        );
        startActivity(profileIntent);
    }

    private void confirmProfileDeletion(
            @NonNull StudentProfileEntity profile
    ) {
        if (profileActivationInProgress) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + profile.getStudentName() + "?")
                .setMessage("इस profile की progress, doubts, quiz, subjects और books इस device से permanently delete होंगे।")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete Permanently", (dialog, which) ->
                        deleteProfile(profile.getProfileId()))
                .show();
    }

    private void deleteProfile(long profileId) {
        showActivationState(true);
        studentProfileRepository.permanentlyDeleteProfile(
                profileId,
                new StudentProfileRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        showActivationState(false);
                        loadStudentProfiles();
                        Snackbar.make(
                                binding.getRoot(),
                                "Student profile और उससे जुड़ा data permanently deleted.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        showActivationState(false);
                        Snackbar.make(
                                binding.getRoot(),
                                "Profile delete नहीं हो सका।",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (studentProfileRepository != null && !profileActivationInProgress) {
            loadStudentProfiles();
        }
    }

    private void openDashboardAsRoot() {
        Intent dashboardIntent = new Intent(
                StudentProfilesActivity.this,
                DashboardActivity.class
        );

        dashboardIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(dashboardIntent);
    }

    private void showLoadingState(boolean loading) {
        binding.progressProfiles.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        if (loading) {
            binding.recyclerStudentProfiles.setVisibility(
                    View.INVISIBLE
            );

            binding.cardEmptyProfiles.setVisibility(View.GONE);
        }
    }

    private void showActivationState(boolean activating) {
        profileActivationInProgress = activating;

        binding.buttonBack.setEnabled(!activating);
        binding.buttonAddStudent.setEnabled(!activating);
        binding.recyclerStudentProfiles.setEnabled(!activating);

        binding.progressProfiles.setVisibility(
                activating ? View.VISIBLE : View.GONE
        );
    }

    private void showEmptyState(boolean show) {
        binding.cardEmptyProfiles.setVisibility(
                show ? View.VISIBLE : View.GONE
        );
    }
}
