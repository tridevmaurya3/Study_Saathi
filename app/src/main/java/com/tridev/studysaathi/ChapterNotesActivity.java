package com.tridev.studysaathi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.catalog.SubjectCatalog;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityChapterNotesBinding;
import com.tridev.studysaathi.model.ChapterItem;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChapterNotesActivity
        extends AppCompatActivity {

    public static final String EXTRA_PREFILL_SUBJECT =
            "extra_notes_prefill_subject";

    public static final String EXTRA_PREFILL_CHAPTER =
            "extra_notes_prefill_chapter";

    private static final String NOTES_PREFERENCES_NAME =
            "study_saathi_chapter_notes_v1";

    private static final int MAX_NOTE_LENGTH =
            3000;

    private ActivityChapterNotesBinding binding;

    private StudentProfileRepository
            studentProfileRepository;

    private SharedPreferences notesPreferences;

    private StudentProfileEntity activeStudentProfile;

    private final List<String> subjectOptions =
            new ArrayList<>();

    private final List<ChapterItem> chapterItems =
            new ArrayList<>();

    private final List<String> chapterOptions =
            new ArrayList<>();

    private String selectedSubject = "";
    private String selectedChapter = "";
    private String selectedChapterDescription = "";

    private String loadedNoteText = "";

    private String prefillSubject = "";
    private String prefillChapter = "";

    private boolean updatingDropdowns;
    private boolean loadingNoteText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityChapterNotesBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        notesPreferences =
                getSharedPreferences(
                        NOTES_PREFERENCES_NAME,
                        MODE_PRIVATE
                );

        readPrefillInformation();
        setupClickListeners();
        setupNoteTextListener();
        loadActiveProfile();
    }

    @Override
    protected void onPause() {
        saveCurrentDraftIfChanged();
        super.onPause();
    }

    private void readPrefillInformation() {
        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        prefillSubject =
                safeText(
                        intent.getStringExtra(
                                EXTRA_PREFILL_SUBJECT
                        )
                );

        prefillChapter =
                safeText(
                        intent.getStringExtra(
                                EXTRA_PREFILL_CHAPTER
                        )
                );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view -> {
            saveCurrentDraftIfChanged();

            getOnBackPressedDispatcher()
                    .onBackPressed();
        });

        binding.buttonViewAllNotes
                .setOnClickListener(view ->
                        openAllNotesLibrary()
                );

        binding.buttonSaveNote.setOnClickListener(view ->
                saveCurrentNote(true)
        );

        binding.buttonClearNote.setOnClickListener(view ->
                confirmClearCurrentNote()
        );

        binding.buttonOpenLesson.setOnClickListener(view ->
                openSelectedLesson()
        );

        binding.dropdownNoteSubject.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (updatingDropdowns
                            || position < 0
                            || position >= subjectOptions.size()) {
                        return;
                    }

                    saveCurrentDraftIfChanged();

                    selectedSubject =
                            subjectOptions.get(position);

                    setupChapterDropdown("");
                }
        );

        binding.dropdownNoteChapter.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (updatingDropdowns
                            || position < 0
                            || position >= chapterItems.size()) {
                        return;
                    }

                    saveCurrentDraftIfChanged();

                    applySelectedChapter(position);
                    loadCurrentNote();
                }
        );
    }

    private void setupNoteTextListener() {
        binding.editChapterNote.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after
                    ) {
                        // No action required.
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count
                    ) {
                        updateCharacterCount();

                        if (loadingNoteText) {
                            return;
                        }

                        updateActionButtons();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        // No action required.
                    }
                }
        );
    }

    private void loadActiveProfile() {
        showLoadingState(true);

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository
                        .SingleProfileCallback() {
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

                        showStudentInformation(
                                studentProfile
                        );

                        setupSubjectDropdown();
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
                                R.string.chapter_notes_profile_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textNotesStudent.setText(
                getString(
                        R.string.chapter_notes_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );

        binding.buttonViewAllNotes.setEnabled(
                true
        );
    }

    private void setupSubjectDropdown() {
        subjectOptions.clear();

        if (activeStudentProfile == null) {
            showUnavailableState(
                    R.string.chapter_notes_no_subjects
            );

            return;
        }

        List<SubjectItem> subjectItems =
                SubjectCatalog.getSubjectsForClass(
                        activeStudentProfile
                                .getStudentClass()
                );

        for (SubjectItem subjectItem : subjectItems) {
            String subjectName =
                    safeText(
                            subjectItem.getSubjectName()
                    );

            if (!subjectName.isEmpty()
                    && !containsIgnoreCase(
                    subjectOptions,
                    subjectName
            )) {
                subjectOptions.add(subjectName);
            }
        }

        if (subjectOptions.isEmpty()) {
            showUnavailableState(
                    R.string.chapter_notes_no_subjects
            );

            return;
        }

        ArrayAdapter<String> subjectAdapter =
                new ArrayAdapter<>(
                        this,
                        R.layout.item_professional_dropdown,
                        subjectOptions
                );

        binding.dropdownNoteSubject.setAdapter(
                subjectAdapter
        );

        selectedSubject =
                findMatchingValue(
                        subjectOptions,
                        prefillSubject
                );

        if (selectedSubject.isEmpty()) {
            selectedSubject =
                    subjectOptions.get(0);
        }

        updatingDropdowns = true;

        binding.dropdownNoteSubject.setText(
                selectedSubject,
                false
        );

        updatingDropdowns = false;

        binding.dropdownNoteSubject.setEnabled(true);

        String requestedChapter =
                prefillChapter;

        prefillSubject = "";
        prefillChapter = "";

        setupChapterDropdown(
                requestedChapter
        );
    }

    private void setupChapterDropdown(
            @NonNull String requestedChapter
    ) {
        chapterItems.clear();
        chapterOptions.clear();

        if (activeStudentProfile == null
                || selectedSubject.isEmpty()) {

            showUnavailableState(
                    R.string.chapter_notes_no_chapters
            );

            return;
        }

        List<ChapterItem> availableChapters =
                ChapterCatalog.getChapters(
                        activeStudentProfile
                                .getEducationBoard(),
                        activeStudentProfile
                                .getStudentClass(),
                        selectedSubject
                );

        chapterItems.addAll(
                availableChapters
        );

        for (ChapterItem chapterItem : chapterItems) {
            chapterOptions.add(
                    safeText(
                            chapterItem.getChapterTitle()
                    )
            );
        }

        if (chapterItems.isEmpty()) {
            selectedChapter = "";
            selectedChapterDescription = "";

            updatingDropdowns = true;

            binding.dropdownNoteChapter.setText(
                    "",
                    false
            );

            updatingDropdowns = false;

            binding.dropdownNoteChapter.setEnabled(false);

            showUnavailableState(
                    R.string.chapter_notes_no_chapters
            );

            return;
        }

        ArrayAdapter<String> chapterAdapter =
                new ArrayAdapter<>(
                        this,
                        R.layout.item_professional_dropdown,
                        chapterOptions
                );

        binding.dropdownNoteChapter.setAdapter(
                chapterAdapter
        );

        int selectedPosition =
                findMatchingPosition(
                        chapterOptions,
                        requestedChapter
                );

        if (selectedPosition < 0) {
            selectedPosition = 0;
        }

        applySelectedChapter(
                selectedPosition
        );

        updatingDropdowns = true;

        binding.dropdownNoteChapter.setText(
                selectedChapter,
                false
        );

        updatingDropdowns = false;

        binding.dropdownNoteChapter.setEnabled(true);

        binding.cardNoteEditor.setVisibility(
                View.VISIBLE
        );

        binding.cardNotesUnavailable.setVisibility(
                View.GONE
        );

        loadCurrentNote();
    }

    private void applySelectedChapter(int position) {
        if (position < 0
                || position >= chapterItems.size()) {
            selectedChapter = "";
            selectedChapterDescription = "";
            return;
        }

        ChapterItem selectedItem =
                chapterItems.get(position);

        selectedChapter =
                safeText(
                        selectedItem.getChapterTitle()
                );

        selectedChapterDescription =
                safeText(
                        selectedItem.getChapterDescription()
                );

        updatingDropdowns = true;

        binding.dropdownNoteChapter.setText(
                selectedChapter,
                false
        );

        updatingDropdowns = false;
    }

    private void loadCurrentNote() {
        if (!hasValidSelection()) {
            showUnavailableState(
                    R.string.chapter_notes_no_chapters
            );

            return;
        }

        String savedNote =
                notesPreferences.getString(
                        createCurrentNoteKey(),
                        ""
                );

        if (savedNote == null) {
            savedNote = "";
        }

        loadedNoteText = savedNote;

        loadingNoteText = true;

        binding.editChapterNote.setText(
                savedNote
        );

        binding.editChapterNote.setSelection(
                binding.editChapterNote.length()
        );

        loadingNoteText = false;

        updateCharacterCount();
        updateActionButtons();

        binding.cardNoteEditor.setVisibility(
                View.VISIBLE
        );

        binding.cardNotesUnavailable.setVisibility(
                View.GONE
        );
    }

    private void saveCurrentDraftIfChanged() {
        if (!hasValidSelection()) {
            return;
        }

        String currentText =
                getCurrentNoteText();

        if (currentText.equals(
                loadedNoteText
        )) {
            return;
        }

        saveCurrentNote(false);
    }

    private void saveCurrentNote(
            boolean showConfirmation
    ) {
        if (!hasValidSelection()) {
            if (showConfirmation) {
                Snackbar.make(
                        binding.getRoot(),
                        R.string.chapter_notes_select_chapter_first,
                        Snackbar.LENGTH_SHORT
                ).show();
            }

            return;
        }

        String currentNote =
                getCurrentNoteText();

        SharedPreferences.Editor editor =
                notesPreferences.edit();

        if (currentNote.trim().isEmpty()) {
            editor.remove(
                    createCurrentNoteKey()
            );
        } else {
            editor.putString(
                    createCurrentNoteKey(),
                    currentNote
            );
        }

        editor.apply();

        loadedNoteText = currentNote;

        updateActionButtons();

        if (showConfirmation) {
            Snackbar.make(
                    binding.getRoot(),
                    currentNote.trim().isEmpty()
                            ? R.string.chapter_notes_empty_removed
                            : R.string.chapter_notes_saved,
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }

    private void confirmClearCurrentNote() {
        if (!hasValidSelection()) {
            return;
        }

        String currentNote =
                getCurrentNoteText();

        if (currentNote.trim().isEmpty()) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.chapter_notes_already_empty,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.chapter_notes_clear_title
                )
                .setMessage(
                        getString(
                                R.string.chapter_notes_clear_message_format,
                                selectedChapter
                        )
                )
                .setNegativeButton(
                        R.string.doubt_action_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.chapter_notes_clear_action,
                        (dialog, which) ->
                                clearCurrentNote()
                )
                .show();
    }

    private void clearCurrentNote() {
        if (!hasValidSelection()) {
            return;
        }

        notesPreferences.edit()
                .remove(
                        createCurrentNoteKey()
                )
                .apply();

        loadedNoteText = "";

        loadingNoteText = true;
        binding.editChapterNote.setText("");
        loadingNoteText = false;

        updateCharacterCount();
        updateActionButtons();

        Snackbar.make(
                binding.getRoot(),
                R.string.chapter_notes_cleared,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void openSelectedLesson() {
        saveCurrentDraftIfChanged();

        if (activeStudentProfile == null
                || !hasValidSelection()) {

            Snackbar.make(
                    binding.getRoot(),
                    R.string.chapter_notes_select_chapter_first,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        Intent lessonIntent = new Intent(
                ChapterNotesActivity.this,
                LessonActivity.class
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                selectedSubject
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                selectedChapter
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                selectedChapterDescription
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

    private void openAllNotesLibrary() {
        saveCurrentDraftIfChanged();

        if (activeStudentProfile == null) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.chapter_notes_profile_required,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        Intent libraryIntent = new Intent(
                ChapterNotesActivity.this,
                AllChapterNotesActivity.class
        );

        startActivity(libraryIntent);
    }

    private void updateCharacterCount() {
        int characterCount =
                binding.editChapterNote.length();

        binding.textNoteCharacterCount.setText(
                getString(
                        R.string.chapter_notes_character_count_format,
                        characterCount,
                        MAX_NOTE_LENGTH
                )
        );
    }

    private void updateActionButtons() {
        boolean selectionAvailable =
                hasValidSelection();

        String currentText =
                getCurrentNoteText();

        binding.buttonSaveNote.setEnabled(
                selectionAvailable
        );

        binding.buttonClearNote.setEnabled(
                selectionAvailable
                        && !currentText.trim().isEmpty()
        );

        binding.buttonOpenLesson.setEnabled(
                selectionAvailable
        );
    }

    private void showUnavailableState(
            int messageResource
    ) {
        binding.cardNoteEditor.setVisibility(
                View.GONE
        );

        binding.cardNotesUnavailable.setVisibility(
                View.VISIBLE
        );

        binding.textNotesUnavailable.setText(
                messageResource
        );

        binding.buttonSaveNote.setEnabled(false);
        binding.buttonClearNote.setEnabled(false);
        binding.buttonOpenLesson.setEnabled(false);
    }

    private void showNoProfileState() {
        activeStudentProfile = null;

        selectedSubject = "";
        selectedChapter = "";
        selectedChapterDescription = "";
        loadedNoteText = "";

        binding.textNotesStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.buttonViewAllNotes.setEnabled(
                false
        );

        binding.dropdownNoteSubject.setText(
                "",
                false
        );

        binding.dropdownNoteChapter.setText(
                "",
                false
        );

        binding.dropdownNoteSubject.setEnabled(false);
        binding.dropdownNoteChapter.setEnabled(false);

        showUnavailableState(
                R.string.chapter_notes_profile_required
        );
    }

    private boolean hasValidSelection() {
        return activeStudentProfile != null
                && activeStudentProfile.getProfileId() > 0L
                && !selectedSubject.isEmpty()
                && !selectedChapter.isEmpty();
    }

    @NonNull
    private String createCurrentNoteKey() {
        if (activeStudentProfile == null) {
            return "invalid_note";
        }

        return "profile_"
                + activeStudentProfile.getProfileId()
                + "_subject_"
                + normalizeForKey(selectedSubject)
                + "_chapter_"
                + normalizeForKey(selectedChapter);
    }

    @NonNull
    private String getCurrentNoteText() {
        Editable editable =
                binding.editChapterNote.getText();

        if (editable == null) {
            return "";
        }

        return editable.toString();
    }

    private int findMatchingPosition(
            @NonNull List<String> values,
            String requestedValue
    ) {
        String normalizedRequested =
                normalizeText(requestedValue);

        if (normalizedRequested.isEmpty()) {
            return -1;
        }

        for (int position = 0;
             position < values.size();
             position++) {

            if (normalizeText(
                    values.get(position)
            ).equals(normalizedRequested)) {

                return position;
            }
        }

        return -1;
    }

    @NonNull
    private String findMatchingValue(
            @NonNull List<String> values,
            String requestedValue
    ) {
        int matchingPosition =
                findMatchingPosition(
                        values,
                        requestedValue
                );

        if (matchingPosition < 0) {
            return "";
        }

        return values.get(matchingPosition);
    }

    private boolean containsIgnoreCase(
            @NonNull List<String> values,
            @NonNull String requiredValue
    ) {
        String normalizedRequired =
                normalizeText(requiredValue);

        for (String value : values) {
            if (normalizeText(value)
                    .equals(normalizedRequired)) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private String normalizeForKey(String value) {
        return normalizeText(value)
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        "_"
                );
    }

    @NonNull
    private String normalizeText(String value) {
        return safeText(value)
                .toLowerCase(Locale.ROOT);
    }

    @NonNull
    private String safeText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private void showLoadingState(boolean loading) {
        binding.progressChapterNotes.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.contentChapterNotes.setVisibility(
                loading ? View.INVISIBLE : View.VISIBLE
        );
    }
}