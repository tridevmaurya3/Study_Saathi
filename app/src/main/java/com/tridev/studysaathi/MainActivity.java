package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_AUTH_GATE_PASSED =
            "extra_auth_gate_passed";

    private ActivityMainBinding binding;
    private StudentProfileRepository studentProfileRepository;

    private boolean profileCheckCompleted;
    private boolean dashboardOpening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().getBooleanExtra(EXTRA_AUTH_GATE_PASSED, false)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Intent entryIntent;
            if (user == null || !user.isEmailVerified()) {
                entryIntent = new Intent(this, CloudAccountActivity.class);
                entryIntent.putExtra(
                        CloudAccountActivity.EXTRA_REQUIRE_AUTHENTICATION,
                        true
                );
            } else {
                entryIntent = new Intent(this, UserModeSelectionActivity.class);
            }
            entryIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );
            startActivity(entryIntent);
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        /*
         * Welcome screen को profile check पूरा होने तक छिपाकर रखते हैं,
         * ताकि active profile होने पर Welcome Screen एक क्षण के लिए भी
         * दिखाई न दे।
         */
        binding.getRoot().setVisibility(View.INVISIBLE);

        setupClickListeners();
        checkActiveStudentProfile();
    }

    private void setupClickListeners() {
        binding.buttonCreateProfile.setOnClickListener(view -> {
            Intent profileIntent = new Intent(
                    MainActivity.this,
                    StudentProfileActivity.class
            );

            startActivity(profileIntent);
        });

        binding.buttonExplore.setOnClickListener(view ->
                Snackbar.make(
                        binding.getRoot(),
                        R.string.explore_mode_coming_soon,
                        Snackbar.LENGTH_SHORT
                ).show()
        );
    }

    private void checkActiveStudentProfile() {
        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        profileCheckCompleted = true;

                        if (studentProfile != null) {
                            openDashboard();
                        } else {
                            showWelcomeScreen();
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        profileCheckCompleted = true;
                        showWelcomeScreen();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.profile_loading_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showWelcomeScreen() {
        dashboardOpening = false;
        binding.getRoot().setVisibility(View.VISIBLE);
    }

    private void openDashboard() {
        if (dashboardOpening) {
            return;
        }

        dashboardOpening = true;

        Intent dashboardIntent = new Intent(
                MainActivity.this,
                DashboardActivity.class
        );

        /*
         * Dashboard को app की नई root screen बनाता है।
         * Dashboard से Back दबाने पर पुरानी Welcome Screen नहीं खुलेगी।
         */
        dashboardIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(dashboardIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Student Profile screen से बिना profile बनाए वापस आने पर
         * Welcome Screen पहले की तरह दिखाई देती रहेगी।
         */
        if (profileCheckCompleted && !dashboardOpening) {
            binding.getRoot().setVisibility(View.VISIBLE);
        }
    }
}
