package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.ChapterAdapter;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.LessonProgressRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityChaptersBinding;
import com.tridev.studysaathi.model.ChapterItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChaptersActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_NAME =
            "extra_subject_name";

    public static final String EXTRA_STUDENT_CLASS =
            "extra_student_class";

    public static final String EXTRA_EDUCATION_BOARD =
            "extra_education_board";

    private ActivityChaptersBinding binding;
    private ChapterAdapter chapterAdapter;

    private StudentProfileRepository studentProfileRepository;
    private LessonProgressRepository lessonProgressRepository;

    private String subjectName;
    private String studentClass;
    private String educationBoard;

    private long activeProfileId = -1L;

    private List<ChapterItem> baseChapterList =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChaptersBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        lessonProgressRepository =
                new LessonProgressRepository(this);

        readScreenArguments();
        setupRecyclerView();
        setupClickListeners();
        prepareChapterCatalog();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Lesson screen से वापस आने पर progress तुरंत refresh होगी।
         */
        loadActiveProfileAndProgress();
    }

    private void readScreenArguments() {
        subjectName = getIntent().getStringExtra(
                EXTRA_SUBJECT_NAME
        );

        studentClass = getIntent().getStringExtra(
                EXTRA_STUDENT_CLASS
        );

        educationBoard = getIntent().getStringExtra(
                EXTRA_EDUCATION_BOARD
        );

        if (subjectName == null
                || subjectName.trim().isEmpty()) {
            subjectName = getString(
                    R.string.default_subject_name
            );
        }

        if (studentClass == null
                || studentClass.trim().isEmpty()) {
            studentClass = "Class 6";
        }

        if (educationBoard == null
                || educationBoard.trim().isEmpty()) {
            educationBoard = "CBSE";
        }
    }

    private void setupRecyclerView() {
        chapterAdapter = new ChapterAdapter(
                new ArrayList<>(),
                this::handleChapterSelection
        );

        binding.recyclerChapters.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerChapters.setAdapter(
                chapterAdapter
        );

        binding.recyclerChapters.setHasFixedSize(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );
    }

    private void prepareChapterCatalog() {
        binding.textSubjectName.setText(
                subjectName
        );

        binding.textCurriculumDetails.setText(
                educationBoard
                        + "  •  "
                        + studentClass
        );

        baseChapterList = ChapterCatalog.getChapters(
                educationBoard,
                studentClass,
                subjectName
        );

        binding.textChapterCount.setText(
                getString(
                        R.string.chapter_count_format,
                        baseChapterList.size()
                )
        );

        showChapterList(baseChapterList);
    }

    private void loadActiveProfileAndProgress() {
        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (studentProfile == null) {
                            activeProfileId = -1L;
                            showChapterList(baseChapterList);
                            return;
                        }

                        activeProfileId =
                                studentProfile.getProfileId();

                        loadSavedSubjectProgress();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        activeProfileId = -1L;
                        showChapterList(baseChapterList);

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.profile_loading_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void loadSavedSubjectProgress() {
        if (activeProfileId <= 0L) {
            showChapterList(baseChapterList);
            return;
        }

        lessonProgressRepository.getProgressForSubject(
                activeProfileId,
                educationBoard,
                studentClass,
                subjectName,
                new LessonProgressRepository.ProgressListCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<LessonProgressEntity> progressList
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        List<ChapterItem> updatedChapters =
                                applySavedProgress(
                                        baseChapterList,
                                        progressList
                                );

                        showChapterList(updatedChapters);
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showChapterList(baseChapterList);

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.lesson_progress_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    @NonNull
    private List<ChapterItem> applySavedProgress(
            @NonNull List<ChapterItem> chapters,
            @NonNull List<LessonProgressEntity> progressList
    ) {
        Map<String, Integer> savedProgressMap =
                new HashMap<>();

        for (LessonProgressEntity lessonProgress
                : progressList) {

            String normalizedChapterTitle =
                    normalizeChapterTitle(
                            lessonProgress.getChapterTitle()
                    );

            int savedProgress =
                    lessonProgress.getProgressPercent();

            Integer existingProgress =
                    savedProgressMap.get(
                            normalizedChapterTitle
                    );

            if (existingProgress == null
                    || savedProgress > existingProgress) {
                savedProgressMap.put(
                        normalizedChapterTitle,
                        savedProgress
                );
            }
        }

        List<ChapterItem> updatedChapters =
                new ArrayList<>();

        for (ChapterItem chapterItem : chapters) {
            String normalizedChapterTitle =
                    normalizeChapterTitle(
                            chapterItem.getChapterTitle()
                    );

            int savedProgress =
                    savedProgressMap.containsKey(
                            normalizedChapterTitle
                    )
                            ? savedProgressMap.get(
                            normalizedChapterTitle
                    )
                            : 0;

            updatedChapters.add(
                    chapterItem.copyWithProgress(
                            savedProgress
                    )
            );
        }

        return updatedChapters;
    }

    @NonNull
    private String normalizeChapterTitle(
            String chapterTitle
    ) {
        if (chapterTitle == null) {
            return "";
        }

        return chapterTitle
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void showChapterList(
            @NonNull List<ChapterItem> chapters
    ) {
        chapterAdapter.submitList(chapters);

        boolean chaptersAvailable =
                !chapters.isEmpty();

        binding.recyclerChapters.setVisibility(
                chaptersAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptyChapters.setVisibility(
                chaptersAvailable
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private void handleChapterSelection(
            @NonNull ChapterItem chapterItem
    ) {
        Intent lessonIntent = new Intent(
                ChaptersActivity.this,
                LessonActivity.class
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                subjectName
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                chapterItem.getChapterTitle()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                chapterItem.getChapterDescription()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_STUDENT_CLASS,
                studentClass
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_EDUCATION_BOARD,
                educationBoard
        );

        startActivity(lessonIntent);
    }
}