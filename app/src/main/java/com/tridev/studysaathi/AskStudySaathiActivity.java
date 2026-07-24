package com.tridev.studysaathi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.catalog.DoubtAssistantEngine;
import com.tridev.studysaathi.data.catalog.LessonCatalog;
import com.tridev.studysaathi.data.catalog.SubjectCatalog;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.DoubtHistoryRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityAskStudySaathiBinding;
import com.tridev.studysaathi.model.ChapterItem;
import com.tridev.studysaathi.model.LessonContent;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AskStudySaathiActivity
        extends AppCompatActivity {

    public static final String EXTRA_PREFILL_SUBJECT =
            "extra_prefill_subject";

    public static final String EXTRA_PREFILL_CHAPTER =
            "extra_prefill_chapter";

    public static final String EXTRA_PREFILL_QUESTION =
            "extra_prefill_question";

    private ActivityAskStudySaathiBinding binding;

    private StudentProfileRepository studentProfileRepository;
    private DoubtHistoryRepository doubtHistoryRepository;

    private StudentProfileEntity activeStudentProfile;

    private final List<String> subjectNames =
            new ArrayList<>();

    private final List<ChapterItem> chapterItems =
            new ArrayList<>();

    private String selectedSubjectName = "";
    private ChapterItem selectedChapter;

    private String prefillSubjectName = "";
    private String prefillChapterTitle = "";
    private String prefillQuestion = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAskStudySaathiBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        doubtHistoryRepository =
                new DoubtHistoryRepository(this);

        readPrefillArguments();
        setupClickListeners();
        loadActiveStudentProfile();
    }

    private void readPrefillArguments() {
        prefillSubjectName = getSafeExtra(
                EXTRA_PREFILL_SUBJECT
        );

        prefillChapterTitle = getSafeExtra(
                EXTRA_PREFILL_CHAPTER
        );

        prefillQuestion = getSafeExtra(
                EXTRA_PREFILL_QUESTION
        );
    }

    @NonNull
    private String getSafeExtra(
            @NonNull String key
    ) {
        String value =
                getIntent().getStringExtra(key);

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.buttonDoubtHistory.setOnClickListener(view ->
                openDoubtHistory()
        );

        binding.buttonAskSaathi.setOnClickListener(view -> {
            String question =
                    binding.editQuestion.getText() == null
                            ? ""
                            : binding.editQuestion
                            .getText()
                            .toString()
                            .trim();

            submitQuestion(question);
        });

        binding.buttonQuickExplain.setOnClickListener(view ->
                submitQuickQuestion(
                        getString(
                                R.string.quick_question_explain
                        )
                )
        );

        binding.buttonQuickKeyPoints.setOnClickListener(view ->
                submitQuickQuestion(
                        getString(
                                R.string.quick_question_key_points
                        )
                )
        );

        binding.buttonQuickExample.setOnClickListener(view ->
                submitQuickQuestion(
                        getString(
                                R.string.quick_question_example
                        )
                )
        );

        binding.buttonQuickPractice.setOnClickListener(view ->
                submitQuickQuestion(
                        getString(
                                R.string.quick_question_practice
                        )
                )
        );

        binding.buttonOpenLesson.setOnClickListener(view ->
                openSelectedLesson()
        );
    }

    private void loadActiveStudentProfile() {
        showLoadingState(true);

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);

                        if (studentProfile == null) {
                            showNoProfileState();
                            return;
                        }

                        activeStudentProfile =
                                studentProfile;

                        showStudentProfile(
                                studentProfile
                        );

                        populateSubjects(
                                studentProfile
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);
                        showNoProfileState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.ask_saathi_profile_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentProfile(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textAskStudent.setText(
                getString(
                        R.string.ask_saathi_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass(),
                        studentProfile.getExplanationLanguage()
                )
        );
    }

    private void populateSubjects(
            @NonNull StudentProfileEntity studentProfile
    ) {
        List<SubjectItem> subjects =
                SubjectCatalog.getSubjectsForClass(
                        studentProfile.getStudentClass()
                );

        subjectNames.clear();

        for (SubjectItem subjectItem : subjects) {
            subjectNames.add(
                    subjectItem.getSubjectName()
            );
        }

        ArrayAdapter<String> subjectAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        subjectNames
                );

        binding.dropdownAskSubject.setAdapter(
                subjectAdapter
        );

        binding.dropdownAskSubject.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position < 0
                            || position >= subjectNames.size()) {
                        return;
                    }

                    selectedSubjectName =
                            subjectNames.get(position);

                    populateChapters(
                            studentProfile,
                            selectedSubjectName,
                            ""
                    );

                    clearPreviousAnswer();
                }
        );

        if (subjectNames.isEmpty()) {
            selectedSubjectName = "";
            selectedChapter = null;

            binding.dropdownAskSubject.setText(
                    "",
                    false
            );

            binding.dropdownAskChapter.setText(
                    "",
                    false
            );

            return;
        }

        selectedSubjectName =
                findMatchingSubject(
                        prefillSubjectName
                );

        if (selectedSubjectName.isEmpty()) {
            selectedSubjectName =
                    subjectNames.get(0);
        }

        binding.dropdownAskSubject.setText(
                selectedSubjectName,
                false
        );

        populateChapters(
                studentProfile,
                selectedSubjectName,
                prefillChapterTitle
        );

        applyPrefilledQuestion();
    }

    private void populateChapters(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull String subjectName,
            @NonNull String preferredChapterTitle
    ) {
        chapterItems.clear();

        chapterItems.addAll(
                ChapterCatalog.getChapters(
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass(),
                        subjectName
                )
        );

        List<String> chapterTitles =
                new ArrayList<>();

        for (ChapterItem chapterItem : chapterItems) {
            chapterTitles.add(
                    chapterItem.getChapterTitle()
            );
        }

        ArrayAdapter<String> chapterAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        chapterTitles
                );

        binding.dropdownAskChapter.setAdapter(
                chapterAdapter
        );

        binding.dropdownAskChapter.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position < 0
                            || position >= chapterItems.size()) {
                        return;
                    }

                    selectedChapter =
                            chapterItems.get(position);

                    clearPreviousAnswer();
                }
        );

        if (chapterItems.isEmpty()) {
            selectedChapter = null;

            binding.dropdownAskChapter.setText(
                    "",
                    false
            );

            return;
        }

        selectedChapter =
                findMatchingChapter(
                        preferredChapterTitle
                );

        if (selectedChapter == null) {
            selectedChapter =
                    chapterItems.get(0);
        }

        binding.dropdownAskChapter.setText(
                selectedChapter.getChapterTitle(),
                false
        );
    }

    @NonNull
    private String findMatchingSubject(
            @NonNull String preferredSubject
    ) {
        String normalizedPreferred =
                normalizeText(preferredSubject);

        if (normalizedPreferred.isEmpty()) {
            return "";
        }

        for (String subjectName : subjectNames) {
            if (normalizeText(subjectName)
                    .equals(normalizedPreferred)) {
                return subjectName;
            }
        }

        return "";
    }

    private ChapterItem findMatchingChapter(
            @NonNull String preferredChapter
    ) {
        String normalizedPreferred =
                normalizeText(preferredChapter);

        if (normalizedPreferred.isEmpty()) {
            return null;
        }

        for (ChapterItem chapterItem : chapterItems) {
            if (normalizeText(
                    chapterItem.getChapterTitle()
            ).equals(normalizedPreferred)) {
                return chapterItem;
            }
        }

        return null;
    }

    private void applyPrefilledQuestion() {
        if (prefillQuestion.isEmpty()) {
            return;
        }

        binding.editQuestion.setText(
                prefillQuestion
        );

        binding.editQuestion.setSelection(
                prefillQuestion.length()
        );

        Snackbar.make(
                binding.getRoot(),
                R.string.history_question_restored,
                Snackbar.LENGTH_SHORT
        ).show();

        prefillSubjectName = "";
        prefillChapterTitle = "";
        prefillQuestion = "";
    }

    private void submitQuickQuestion(
            @NonNull String question
    ) {
        binding.editQuestion.setText(question);

        binding.editQuestion.setSelection(
                question.length()
        );

        submitQuestion(question);
    }

    private void submitQuestion(
            @NonNull String question
    ) {
        if (activeStudentProfile == null) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.ask_saathi_profile_required,
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        if (selectedSubjectName.trim().isEmpty()
                || selectedChapter == null) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.ask_saathi_selection_required,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (question.trim().isEmpty()) {
            binding.inputQuestion.setError(
                    getString(
                            R.string.ask_saathi_question_required
                    )
            );

            binding.editQuestion.requestFocus();
            return;
        }

        binding.inputQuestion.setError(null);

        hideKeyboard();

        LessonContent lessonContent =
                LessonCatalog.getLessonContent(
                        selectedSubjectName,
                        selectedChapter.getChapterTitle(),
                        selectedChapter.getChapterDescription()
                );

        String answer =
                DoubtAssistantEngine.createAnswer(
                        question,
                        selectedSubjectName,
                        selectedChapter.getChapterTitle(),
                        lessonContent,
                        activeStudentProfile
                                .getExplanationLanguage()
                );

        binding.textUserQuestion.setText(
                question.trim()
        );

        binding.textSaathiAnswer.setText(
                answer
        );

        binding.cardAnswer.setVisibility(
                View.VISIBLE
        );

        saveDoubtHistory(
                question.trim(),
                answer
        );

        binding.askSaathiScrollView.post(() ->
                binding.askSaathiScrollView.smoothScrollTo(
                        0,
                        binding.cardAnswer.getBottom()
                )
        );
    }

    private void saveDoubtHistory(
            @NonNull String question,
            @NonNull String answer
    ) {
        if (activeStudentProfile == null
                || selectedChapter == null) {
            return;
        }

        DoubtHistoryEntity historyEntity =
                new DoubtHistoryEntity();

        historyEntity.setProfileId(
                activeStudentProfile.getProfileId()
        );

        historyEntity.setEducationBoard(
                activeStudentProfile.getEducationBoard()
        );

        historyEntity.setStudentClass(
                activeStudentProfile.getStudentClass()
        );

        historyEntity.setSubjectName(
                selectedSubjectName
        );

        historyEntity.setChapterTitle(
                selectedChapter.getChapterTitle()
        );

        historyEntity.setQuestionText(question);
        historyEntity.setAnswerText(answer);

        historyEntity.setExplanationLanguage(
                activeStudentProfile
                        .getExplanationLanguage()
        );

        historyEntity.setCreatedAt(
                System.currentTimeMillis()
        );

        doubtHistoryRepository.saveHistory(
                historyEntity,
                new DoubtHistoryRepository.SaveHistoryCallback() {
                    @Override
                    public void onSuccess(long historyId) {
                        // History saved successfully.
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.doubt_history_save_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void openSelectedLesson() {
        if (activeStudentProfile == null
                || selectedChapter == null
                || selectedSubjectName.trim().isEmpty()) {

            Snackbar.make(
                    binding.getRoot(),
                    R.string.ask_saathi_selection_required,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        Intent lessonIntent = new Intent(
                AskStudySaathiActivity.this,
                LessonActivity.class
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                selectedSubjectName
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                selectedChapter.getChapterTitle()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                selectedChapter.getChapterDescription()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_STUDENT_CLASS,
                activeStudentProfile.getStudentClass()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_EDUCATION_BOARD,
                activeStudentProfile.getEducationBoard()
        );

        startActivity(lessonIntent);
    }

    private void openDoubtHistory() {
        Intent historyIntent = new Intent(
                AskStudySaathiActivity.this,
                DoubtHistoryActivity.class
        );

        startActivity(historyIntent);
    }

    private void clearPreviousAnswer() {
        binding.cardAnswer.setVisibility(
                View.GONE
        );

        binding.textUserQuestion.setText("");
        binding.textSaathiAnswer.setText("");
    }

    private void hideKeyboard() {
        View currentView = getCurrentFocus();

        if (currentView == null) {
            currentView = binding.editQuestion;
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE
                );

        inputMethodManager.hideSoftInputFromWindow(
                currentView.getWindowToken(),
                0
        );

        currentView.clearFocus();
    }

    private void showNoProfileState() {
        activeStudentProfile = null;
        selectedChapter = null;
        selectedSubjectName = "";

        binding.textAskStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.dropdownAskSubject.setEnabled(false);
        binding.dropdownAskChapter.setEnabled(false);
        binding.editQuestion.setEnabled(false);
        binding.buttonAskSaathi.setEnabled(false);
        binding.buttonDoubtHistory.setEnabled(false);
        binding.buttonQuickExplain.setEnabled(false);
        binding.buttonQuickKeyPoints.setEnabled(false);
        binding.buttonQuickExample.setEnabled(false);
        binding.buttonQuickPractice.setEnabled(false);

        Snackbar.make(
                binding.getRoot(),
                R.string.ask_saathi_profile_required,
                Snackbar.LENGTH_LONG
        ).show();
    }

    @NonNull
    private String normalizeText(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void showLoadingState(boolean loading) {
        binding.progressAskSaathi.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.contentAskSaathi.setVisibility(
                loading ? View.INVISIBLE : View.VISIBLE
        );
    }
}