package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.tridev.studysaathi.databinding.ActivityLearningLabBinding;

/** One launch point for revision, assessment, creative and progress tools. */
public final class LearningLabActivity extends AppCompatActivity {
    private ActivityLearningLabBinding binding;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLearningLabBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.buttonLabRevision.setOnClickListener(v -> open(RevisionActivity.class));
        binding.buttonLabMockExam.setOnClickListener(v -> tutor(
                "मेरे current subject और chapter का timed mock test लो। पहले marks और समय बताओ, answers अभी मत दिखाओ।"));
        binding.buttonLabFlashcards.setOnClickListener(v -> tutor(
                "मेरे current chapter के concise flashcards बनाओ। हर card को Front | Back format में दो।"));
        binding.buttonLabMistakes.setOnClickListener(v -> open(MistakeNotebookActivity.class));
        binding.buttonLabWhiteboard.setOnClickListener(v -> open(WhiteboardActivity.class));
        binding.buttonLabReminders.setOnClickListener(v -> open(ReminderSettingsActivity.class));
        binding.buttonLabParentReport.setOnClickListener(v -> open(ParentDashboardActivity.class));
        binding.buttonLabOffline.setOnClickListener(v -> open(SubjectsActivity.class));
        binding.buttonLabOnboarding.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpAboutActivity.class);
            intent.putExtra(HelpAboutActivity.EXTRA_MODE, HelpAboutActivity.MODE_STUDENT);
            startActivity(intent);
        });
        binding.buttonLabAchievements.setOnClickListener(v -> open(LearningProgressActivity.class));
    }

    private void open(Class<?> destination) { startActivity(new Intent(this, destination)); }

    private void tutor(String prompt) {
        Intent intent = new Intent(this, AskStudySaathiActivity.class);
        intent.putExtra(AskStudySaathiActivity.EXTRA_PREFILL_QUESTION, prompt);
        startActivity(intent);
    }
}
