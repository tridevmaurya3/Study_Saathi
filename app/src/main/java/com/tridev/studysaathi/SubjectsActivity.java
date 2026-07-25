package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.SubjectAdapter;
import com.tridev.studysaathi.data.catalog.SubjectCatalog;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivitySubjectsBinding;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.List;

public class SubjectsActivity extends AppCompatActivity {

    private ActivitySubjectsBinding binding;

    private StudentProfileRepository
            studentProfileRepository;

    private SubjectAdapter subjectAdapter;

    private String activeEducationBoard =
            "CBSE";

    private String activeStudentClass =
            "Class 6";

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivitySubjectsBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        studentProfileRepository =
                new StudentProfileRepository(
                        this
                );

        setupRecyclerView();
        setupClickListeners();
        loadActiveStudentProfile();
    }

    private void setupRecyclerView() {
        subjectAdapter =
                new SubjectAdapter(
                        new ArrayList<>(),
                        this::handleSubjectSelection
                );

        binding.recyclerSubjects.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        binding.recyclerSubjects.setAdapter(
                subjectAdapter
        );

        binding.recyclerSubjects.setHasFixedSize(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(
                view ->
                        getOnBackPressedDispatcher()
                                .onBackPressed()
        );

        binding.cardScanSchoolBook
                .setOnClickListener(
                        view ->
                                openBookCoverScanScreen()
                );
    }

    private void openBookCoverScanScreen() {
        if (isFinishing()
                || isDestroyed()) {

            return;
        }

        Intent scanBookIntent =
                new Intent(
                        SubjectsActivity.this,
                        BookCoverScanActivity.class
                );

        startActivity(
                scanBookIntent
        );
    }

    private void loadActiveStudentProfile() {
        showLoadingState(
                true
        );

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository
                        .SingleProfileCallback() {

                    @Override
                    public void onSuccess(
                            StudentProfileEntity
                                    studentProfile
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        showLoadingState(
                                false
                        );

                        if (studentProfile == null) {
                            showNoProfileState();

                            return;
                        }

                        showSubjectsForProfile(
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

                        showLoadingState(
                                false
                        );

                        showNoProfileState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string
                                        .subject_profile_loading_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showSubjectsForProfile(
            @NonNull StudentProfileEntity
                    studentProfile
    ) {
        activeEducationBoard =
                studentProfile
                        .getEducationBoard();

        activeStudentClass =
                studentProfile
                        .getStudentClass();

        String profileSummary =
                activeEducationBoard
                        + "  •  "
                        + activeStudentClass;

        binding.textSubjectProfile.setText(
                profileSummary
        );

        binding.textSubjectStudentName.setText(
                getString(
                        R.string
                                .subjects_for_student_format,
                        studentProfile
                                .getStudentName()
                )
        );

        List<SubjectItem> subjects =
                SubjectCatalog
                        .getSubjectsForClass(
                                activeStudentClass
                        );

        subjectAdapter.submitList(
                subjects
        );

        binding.textSubjectCount.setText(
                getString(
                        R.string
                                .subject_count_format,
                        subjects.size()
                )
        );

        boolean subjectsAvailable =
                !subjects.isEmpty();

        binding.recyclerSubjects.setVisibility(
                subjectsAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptySubjects.setVisibility(
                subjectsAvailable
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private void showNoProfileState() {
        binding.textSubjectProfile.setText(
                R.string.no_active_profile
        );

        binding.textSubjectStudentName.setText(
                R.string.create_profile_to_continue
        );

        binding.textSubjectCount.setText(
                getString(
                        R.string.subject_count_format,
                        0
                )
        );

        binding.recyclerSubjects.setVisibility(
                View.GONE
        );

        binding.cardEmptySubjects.setVisibility(
                View.VISIBLE
        );
    }

    private void handleSubjectSelection(
            @NonNull SubjectItem subjectItem
    ) {
        Intent chaptersIntent =
                new Intent(
                        SubjectsActivity.this,
                        ChaptersActivity.class
                );

        chaptersIntent.putExtra(
                ChaptersActivity.EXTRA_SUBJECT_NAME,
                subjectItem.getSubjectName()
        );

        chaptersIntent.putExtra(
                ChaptersActivity.EXTRA_STUDENT_CLASS,
                activeStudentClass
        );

        chaptersIntent.putExtra(
                ChaptersActivity.EXTRA_EDUCATION_BOARD,
                activeEducationBoard
        );

        startActivity(
                chaptersIntent
        );
    }

    private void showLoadingState(
            boolean loading
    ) {
        binding.progressSubjects.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.contentSubjects.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }
}