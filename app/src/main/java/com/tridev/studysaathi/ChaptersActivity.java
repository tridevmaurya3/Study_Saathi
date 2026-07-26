package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.ChapterAdapter;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.local.entity
        .LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookEntity;
import com.tridev.studysaathi.data.local.entity
        .StudentProfileEntity;
import com.tridev.studysaathi.data.repository
        .ChildSchoolBookChapterRepository;
import com.tridev.studysaathi.data.repository
        .LessonProgressRepository;
import com.tridev.studysaathi.data.repository
        .StudentProfileRepository;
import com.tridev.studysaathi.databinding
        .ActivityChaptersBinding;
import com.tridev.studysaathi.model.ChapterItem;
import com.tridev.studysaathi.navigation
        .ExactSchoolBookLessonIntentFactory;
import com.tridev.studysaathi.ui.adapter
        .ChildSchoolBookChapterAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChaptersActivity
        extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_NAME =
            "extra_subject_name";

    public static final String EXTRA_STUDENT_CLASS =
            "extra_student_class";

    public static final String EXTRA_EDUCATION_BOARD =
            "extra_education_board";

    /**
     * à¤¯à¤¹ extra à¤®à¤¿à¤²à¤¨à¥‡ à¤ªà¤° generic catalog à¤•à¥‡ à¤¬à¤œà¤¾à¤¯ exact confirmed
     * school-book chapters load à¤¹à¥‹à¤‚à¤—à¥‡à¥¤
     */
    public static final String EXTRA_SCHOOL_SUBJECT_ROW_ID =
            "extra_school_subject_row_id";

    private static final long INVALID_ROW_ID =
            0L;

    private ActivityChaptersBinding binding;

    private ChapterAdapter genericChapterAdapter;

    private ChildSchoolBookChapterAdapter exactChapterAdapter;

    private StudentProfileRepository studentProfileRepository;

    private LessonProgressRepository lessonProgressRepository;

    private ChildSchoolBookChapterRepository
            childChapterRepository;

    @NonNull
    private String subjectName =
            "";

    @NonNull
    private String studentClass =
            "";

    @NonNull
    private String educationBoard =
            "";

    private long schoolSubjectRowId =
            INVALID_ROW_ID;

    private long activeProfileId =
            -1L;

    @NonNull
    private List<ChapterItem> baseChapterList =
            new ArrayList<>();

    private boolean exactChaptersLoading;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivityChaptersBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        studentProfileRepository =
                new StudentProfileRepository(
                        this
                );

        lessonProgressRepository =
                new LessonProgressRepository(
                        this
                );

        childChapterRepository =
                new ChildSchoolBookChapterRepository(
                        this
                );

        readScreenArguments();
        setupRecyclerView();
        setupClickListeners();
        prepareChapterSource();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isExactSchoolBookMode()) {
            loadExactSchoolBookChapters();

        } else {
            loadActiveProfileAndProgress();
        }
    }

    private void readScreenArguments() {
        Intent intent =
                getIntent();

        subjectName =
                safeText(
                        intent.getStringExtra(
                                EXTRA_SUBJECT_NAME
                        )
                );

        studentClass =
                safeText(
                        intent.getStringExtra(
                                EXTRA_STUDENT_CLASS
                        )
                );

        educationBoard =
                safeText(
                        intent.getStringExtra(
                                EXTRA_EDUCATION_BOARD
                        )
                );

        schoolSubjectRowId =
                intent.getLongExtra(
                        EXTRA_SCHOOL_SUBJECT_ROW_ID,
                        INVALID_ROW_ID
                );

        if (subjectName.isEmpty()) {
            subjectName =
                    getString(
                            R.string.default_subject_name
                    );
        }

        if (studentClass.isEmpty()) {
            studentClass =
                    "Class 6";
        }

        if (educationBoard.isEmpty()) {
            educationBoard =
                    "CBSE";
        }
    }

    private void setupRecyclerView() {
        binding.recyclerChapters.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        binding.recyclerChapters.setHasFixedSize(
                false
        );

        if (isExactSchoolBookMode()) {
            exactChapterAdapter =
                    new ChildSchoolBookChapterAdapter(
                            this::handleExactChapterSelection
                    );

            binding.recyclerChapters.setAdapter(
                    exactChapterAdapter
            );

        } else {
            genericChapterAdapter =
                    new ChapterAdapter(
                            new ArrayList<>(),
                            this::handleGenericChapterSelection
                    );

            binding.recyclerChapters.setAdapter(
                    genericChapterAdapter
            );
        }
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );
    }

    private void prepareChapterSource() {
        binding.textSubjectName.setText(
                subjectName
        );

        binding.textCurriculumDetails.setText(
                educationBoard
                        + "  â€¢  "
                        + studentClass
        );

        if (isExactSchoolBookMode()) {
            binding.textChapterCount.setText(
                    getString(
                            R.string.chapter_count_format,
                            0
                    )
            );

            showEmptyState();

            return;
        }

        prepareGenericChapterCatalog();
    }

    private void prepareGenericChapterCatalog() {
        baseChapterList =
                ChapterCatalog.getChapters(
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

        showGenericChapterList(
                baseChapterList
        );
    }

    private void loadExactSchoolBookChapters() {
        if (!isExactSchoolBookMode()
                || exactChaptersLoading) {

            return;
        }

        exactChaptersLoading =
                true;

        childChapterRepository.getChildChaptersForSubject(
                schoolSubjectRowId,
                new ChildSchoolBookChapterRepository
                        .ChildChaptersCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull ChildSchoolBookChapterRepository
                                    .ChildChapterResult result
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        exactChaptersLoading =
                                false;

                        if (!result.isAvailable()) {
                            showExactChapterUnavailableState(
                                    result
                            );

                            return;
                        }

                        bindExactBookInformation(
                                result.getSchoolBook()
                        );

                        showExactChapterList(
                                result.getChapters()
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        exactChaptersLoading =
                                false;

                        showEmptyState();

                        Snackbar.make(
                                binding.getRoot(),
                                getErrorMessage(
                                        exception,
                                        "Exact school-book chapters "
                                                + "could not be loaded."
                                ),
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void bindExactBookInformation(
            @Nullable SchoolBookEntity schoolBook
    ) {
        if (schoolBook == null) {
            return;
        }

        String bookTitle =
                safeText(
                        schoolBook.getBookTitle()
                );

        if (!bookTitle.isEmpty()) {
            binding.textCurriculumDetails.setText(
                    bookTitle
            );
        }
    }

    private void showExactChapterUnavailableState(
            @NonNull ChildSchoolBookChapterRepository
                    .ChildChapterResult result
    ) {
        if (exactChapterAdapter != null) {
            exactChapterAdapter.clearChapters();
        }

        binding.textChapterCount.setText(
                getString(
                        R.string.chapter_count_format,
                        0
                )
        );

        showEmptyState();

        String message =
                result.getUnavailableMessage();

        if (!message.isEmpty()) {
            Snackbar.make(
                    binding.getRoot(),
                    message,
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private void showExactChapterList(
            @NonNull List<SchoolBookChapterEntity> chapters
    ) {
        if (exactChapterAdapter == null) {
            return;
        }

        exactChapterAdapter.submitChapters(
                chapters
        );

        binding.textChapterCount.setText(
                getString(
                        R.string.chapter_count_format,
                        exactChapterAdapter.getItemCount()
                )
        );

        boolean chaptersAvailable =
                exactChapterAdapter.getItemCount()
                        > 0;

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

    private void loadActiveProfileAndProgress() {
        if (isExactSchoolBookMode()) {
            return;
        }

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable StudentProfileEntity studentProfile
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        if (studentProfile == null) {
                            activeProfileId =
                                    -1L;

                            showGenericChapterList(
                                    baseChapterList
                            );

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
                        if (!isActivityAvailable()) {
                            return;
                        }

                        activeProfileId =
                                -1L;

                        showGenericChapterList(
                                baseChapterList
                        );

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
        if (isExactSchoolBookMode()) {
            return;
        }

        if (activeProfileId <= 0L) {
            showGenericChapterList(
                    baseChapterList
            );

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
                        if (!isActivityAvailable()) {
                            return;
                        }

                        List<ChapterItem> updatedChapters =
                                applySavedProgress(
                                        baseChapterList,
                                        progressList
                                );

                        showGenericChapterList(
                                updatedChapters
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        showGenericChapterList(
                                baseChapterList
                        );

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

        for (LessonProgressEntity lessonProgress : progressList) {
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

            Integer savedProgress =
                    savedProgressMap.get(
                            normalizedChapterTitle
                    );

            updatedChapters.add(
                    chapterItem.copyWithProgress(
                            savedProgress == null
                                    ? 0
                                    : savedProgress
                    )
            );
        }

        return updatedChapters;
    }

    @NonNull
    private String normalizeChapterTitle(
            @Nullable String chapterTitle
    ) {
        return safeText(
                chapterTitle
        ).toLowerCase(
                Locale.ROOT
        );
    }

    private void showGenericChapterList(
            @NonNull List<ChapterItem> chapters
    ) {
        if (genericChapterAdapter == null) {
            return;
        }

        genericChapterAdapter.submitList(
                chapters
        );

        binding.textChapterCount.setText(
                getString(
                        R.string.chapter_count_format,
                        chapters.size()
                )
        );

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

    private void showEmptyState() {
        binding.recyclerChapters.setVisibility(
                View.GONE
        );

        binding.cardEmptyChapters.setVisibility(
                View.VISIBLE
        );
    }

    private void handleGenericChapterSelection(
            @NonNull ChapterItem chapterItem
    ) {
        openLesson(
                chapterItem.getChapterTitle(),
                chapterItem.getChapterDescription()
        );
    }

    private void handleExactChapterSelection(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        if (!chapter.isReadyForChildMode()) {
            Snackbar.make(
                    binding.getRoot(),
                    "This chapter is not available in Child Mode.",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        try {
            Intent lessonIntent =
                    ExactSchoolBookLessonIntentFactory.create(
                            ChaptersActivity.this,
                            chapter,
                            subjectName,
                            studentClass,
                            educationBoard
                    );

            startActivity(
                    lessonIntent
            );

        } catch (Exception exception) {
            Snackbar.make(
                    binding.getRoot(),
                    getErrorMessage(
                            exception,
                            "This exact chapter could not be opened."
                    ),
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private void openLesson(
            @NonNull String chapterTitle,
            @Nullable String chapterDescription
    ) {
        Intent lessonIntent =
                new Intent(
                        ChaptersActivity.this,
                        LessonActivity.class
                );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                subjectName
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                chapterTitle
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                safeText(
                        chapterDescription
                )
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_STUDENT_CLASS,
                studentClass
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_EDUCATION_BOARD,
                educationBoard
        );

        startActivity(
                lessonIntent
        );
    }

    private boolean isExactSchoolBookMode() {
        return schoolSubjectRowId
                > INVALID_ROW_ID;
    }

    private boolean isActivityAvailable() {
        return binding != null
                && !isFinishing()
                && !isDestroyed();
    }

    @NonNull
    private String getErrorMessage(
            @NonNull Exception exception,
            @NonNull String fallbackMessage
    ) {
        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return fallbackMessage;
        }

        return message.trim();
    }

    @NonNull
    private String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }

    @Override
    protected void onDestroy() {
        binding =
                null;

        super.onDestroy();
    }
}
