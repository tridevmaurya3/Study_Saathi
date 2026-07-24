package com.tridev.studysaathi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.ChapterNotesLibraryAdapter;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.catalog.SubjectCatalog;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityAllChapterNotesBinding;
import com.tridev.studysaathi.model.ChapterItem;
import com.tridev.studysaathi.model.ChapterNoteItem;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AllChapterNotesActivity
        extends AppCompatActivity {

    private static final String NOTES_PREFERENCES_NAME =
            "study_saathi_chapter_notes_v1";

    private ActivityAllChapterNotesBinding binding;

    private StudentProfileRepository
            studentProfileRepository;

    private SharedPreferences notesPreferences;

    private ChapterNotesLibraryAdapter notesAdapter;

    private StudentProfileEntity activeStudentProfile;

    private final List<ChapterNoteItem> allNotes =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityAllChapterNotesBinding.inflate(
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

        setupRecyclerView();
        setupClickListeners();
        setupSearchListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadNotesLibrary();
    }

    private void setupRecyclerView() {
        notesAdapter =
                new ChapterNotesLibraryAdapter(
                        new ArrayList<>(),
                        this::openSavedNote,
                        this::confirmDeleteNote
                );

        binding.recyclerAllChapterNotes.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerAllChapterNotes.setAdapter(
                notesAdapter
        );

        binding.recyclerAllChapterNotes.setHasFixedSize(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonCreateChapterNote.setOnClickListener(
                view -> openNewNote()
        );
    }

    private void setupSearchListener() {
        binding.editSearchChapterNotes
                .addTextChangedListener(
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
                                applySearchFilter();
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

    private void loadNotesLibrary() {
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

                        loadProfileNotes();
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
        binding.textAllNotesStudent.setText(
                getString(
                        R.string.all_notes_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );

        binding.editSearchChapterNotes.setEnabled(
                true
        );

        binding.buttonCreateChapterNote.setEnabled(
                true
        );
    }

    private void loadProfileNotes() {
        allNotes.clear();

        if (activeStudentProfile == null) {
            applySearchFilter();
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

            if (subjectName.isEmpty()) {
                continue;
            }

            List<ChapterItem> chapterItems =
                    ChapterCatalog.getChapters(
                            activeStudentProfile
                                    .getEducationBoard(),
                            activeStudentProfile
                                    .getStudentClass(),
                            subjectName
                    );

            for (ChapterItem chapterItem
                    : chapterItems) {

                String chapterTitle =
                        safeText(
                                chapterItem
                                        .getChapterTitle()
                        );

                if (chapterTitle.isEmpty()) {
                    continue;
                }

                String noteKey =
                        createNoteKey(
                                activeStudentProfile
                                        .getProfileId(),
                                subjectName,
                                chapterTitle
                        );

                String savedNote =
                        notesPreferences.getString(
                                noteKey,
                                ""
                        );

                if (savedNote == null
                        || savedNote.trim().isEmpty()) {
                    continue;
                }

                allNotes.add(
                        new ChapterNoteItem(
                                subjectName,
                                chapterTitle,
                                savedNote
                        )
                );
            }
        }

        Collections.sort(
                allNotes,
                (firstNote, secondNote) -> {
                    int subjectComparison =
                            firstNote
                                    .getSubjectName()
                                    .compareToIgnoreCase(
                                            secondNote
                                                    .getSubjectName()
                                    );

                    if (subjectComparison != 0) {
                        return subjectComparison;
                    }

                    return firstNote
                            .getChapterTitle()
                            .compareToIgnoreCase(
                                    secondNote
                                            .getChapterTitle()
                            );
                }
        );

        applySearchFilter();
    }

    private void applySearchFilter() {
        String searchText =
                binding.editSearchChapterNotes
                        .getText() == null
                        ? ""
                        : binding.editSearchChapterNotes
                        .getText()
                        .toString();

        String normalizedSearch =
                normalizeText(searchText);

        List<ChapterNoteItem> filteredNotes =
                new ArrayList<>();

        for (ChapterNoteItem noteItem : allNotes) {
            if (normalizedSearch.isEmpty()
                    || containsSearchText(
                    noteItem,
                    normalizedSearch
            )) {
                filteredNotes.add(noteItem);
            }
        }

        notesAdapter.submitList(
                filteredNotes
        );

        showNotesState(
                filteredNotes.size(),
                allNotes.size(),
                !normalizedSearch.isEmpty()
        );
    }

    private boolean containsSearchText(
            @NonNull ChapterNoteItem noteItem,
            @NonNull String normalizedSearch
    ) {
        return normalizeText(
                noteItem.getSubjectName()
        ).contains(normalizedSearch)

                || normalizeText(
                noteItem.getChapterTitle()
        ).contains(normalizedSearch)

                || normalizeText(
                noteItem.getNoteText()
        ).contains(normalizedSearch);
    }

    private void showNotesState(
            int visibleCount,
            int totalCount,
            boolean searchApplied
    ) {
        binding.textAllNotesCount.setText(
                getString(
                        R.string.all_notes_count_format,
                        visibleCount,
                        totalCount
                )
        );

        boolean visibleNotesAvailable =
                visibleCount > 0;

        binding.recyclerAllChapterNotes.setVisibility(
                visibleNotesAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptyAllNotes.setVisibility(
                visibleNotesAvailable
                        ? View.GONE
                        : View.VISIBLE
        );

        if (visibleNotesAvailable) {
            return;
        }

        if (totalCount > 0 && searchApplied) {
            binding.textEmptyAllNotesTitle.setText(
                    R.string.all_notes_no_matching_title
            );

            binding.textEmptyAllNotesDescription.setText(
                    R.string.all_notes_no_matching_description
            );

            binding.buttonCreateChapterNote.setVisibility(
                    View.GONE
            );

            return;
        }

        binding.textEmptyAllNotesTitle.setText(
                R.string.all_notes_empty_title
        );

        binding.textEmptyAllNotesDescription.setText(
                R.string.all_notes_empty_description
        );

        binding.buttonCreateChapterNote.setVisibility(
                View.VISIBLE
        );
    }

    private void confirmDeleteNote(
            @NonNull ChapterNoteItem noteItem
    ) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.all_notes_delete_title
                )
                .setMessage(
                        getString(
                                R.string.all_notes_delete_message_format,
                                noteItem.getChapterTitle()
                        )
                )
                .setNegativeButton(
                        R.string.doubt_action_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.all_notes_delete_action,
                        (dialog, which) ->
                                deleteNote(noteItem)
                )
                .show();
    }

    private void deleteNote(
            @NonNull ChapterNoteItem noteItem
    ) {
        if (activeStudentProfile == null) {
            return;
        }

        String noteKey =
                createNoteKey(
                        activeStudentProfile
                                .getProfileId(),
                        noteItem.getSubjectName(),
                        noteItem.getChapterTitle()
                );

        notesPreferences.edit()
                .remove(noteKey)
                .apply();

        Snackbar.make(
                binding.getRoot(),
                R.string.all_notes_deleted,
                Snackbar.LENGTH_SHORT
        ).show();

        loadProfileNotes();
    }

    private void openSavedNote(
            @NonNull ChapterNoteItem noteItem
    ) {
        Intent notesIntent = new Intent(
                AllChapterNotesActivity.this,
                ChapterNotesActivity.class
        );

        notesIntent.putExtra(
                ChapterNotesActivity.EXTRA_PREFILL_SUBJECT,
                noteItem.getSubjectName()
        );

        notesIntent.putExtra(
                ChapterNotesActivity.EXTRA_PREFILL_CHAPTER,
                noteItem.getChapterTitle()
        );

        startActivity(notesIntent);
    }

    private void openNewNote() {
        if (activeStudentProfile == null) {
            return;
        }

        Intent notesIntent = new Intent(
                AllChapterNotesActivity.this,
                ChapterNotesActivity.class
        );

        startActivity(notesIntent);
    }

    private void showNoProfileState() {
        activeStudentProfile = null;

        allNotes.clear();
        notesAdapter.submitList(
                new ArrayList<>()
        );

        binding.textAllNotesStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.editSearchChapterNotes.setText("");
        binding.editSearchChapterNotes.setEnabled(false);

        binding.buttonCreateChapterNote.setEnabled(false);
        binding.buttonCreateChapterNote.setVisibility(
                View.VISIBLE
        );

        binding.textAllNotesCount.setText(
                getString(
                        R.string.all_notes_count_format,
                        0,
                        0
                )
        );

        binding.recyclerAllChapterNotes.setVisibility(
                View.GONE
        );

        binding.cardEmptyAllNotes.setVisibility(
                View.VISIBLE
        );

        binding.textEmptyAllNotesTitle.setText(
                R.string.all_notes_profile_required_title
        );

        binding.textEmptyAllNotesDescription.setText(
                R.string.all_notes_profile_required_description
        );
    }

    @NonNull
    private String createNoteKey(
            long profileId,
            @NonNull String subjectName,
            @NonNull String chapterTitle
    ) {
        return "profile_"
                + profileId
                + "_subject_"
                + normalizeForKey(subjectName)
                + "_chapter_"
                + normalizeForKey(chapterTitle);
    }

    @NonNull
    private String normalizeForKey(
            String value
    ) {
        return normalizeText(value)
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        "_"
                );
    }

    @NonNull
    private String normalizeText(
            String value
    ) {
        return safeText(value)
                .toLowerCase(Locale.ROOT);
    }

    @NonNull
    private String safeText(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private void showLoadingState(boolean loading) {
        binding.progressAllChapterNotes.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.contentAllChapterNotes.setVisibility(
                loading ? View.INVISIBLE : View.VISIBLE
        );
    }
}