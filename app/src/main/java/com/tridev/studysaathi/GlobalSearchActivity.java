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

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.GlobalSearchAdapter;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.catalog.SubjectCatalog;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityGlobalSearchBinding;
import com.tridev.studysaathi.model.ChapterItem;
import com.tridev.studysaathi.model.GlobalSearchItem;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GlobalSearchActivity
        extends AppCompatActivity {

    private static final String NOTES_PREFERENCES_NAME =
            "study_saathi_chapter_notes_v1";

    private ActivityGlobalSearchBinding binding;

    private StudentProfileRepository
            studentProfileRepository;

    private SharedPreferences notesPreferences;

    private GlobalSearchAdapter searchAdapter;

    private StudentProfileEntity activeStudentProfile;

    private final List<GlobalSearchItem> allSearchItems =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityGlobalSearchBinding.inflate(
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

        loadSearchContent();
    }

    private void setupRecyclerView() {
        searchAdapter =
                new GlobalSearchAdapter(
                        new ArrayList<>(),
                        this::openSearchResult
                );

        binding.recyclerGlobalSearch.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerGlobalSearch.setAdapter(
                searchAdapter
        );

        binding.recyclerGlobalSearch.setHasFixedSize(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonBrowseSubjects.setOnClickListener(view ->
                openSubjects()
        );
    }

    private void setupSearchListener() {
        binding.editGlobalSearch.addTextChangedListener(
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

    private void loadSearchContent() {
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

                        buildSearchIndex();
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
                                R.string.global_search_profile_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textGlobalSearchStudent.setText(
                getString(
                        R.string.global_search_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );

        binding.editGlobalSearch.setEnabled(true);
        binding.buttonBrowseSubjects.setEnabled(true);
    }

    private void buildSearchIndex() {
        allSearchItems.clear();

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

            allSearchItems.add(
                    new GlobalSearchItem(
                            GlobalSearchItem.ResultType.SUBJECT,
                            subjectName,
                            getString(
                                    R.string.global_search_subject_subtitle_format,
                                    activeStudentProfile
                                            .getStudentClass()
                            ),
                            getString(
                                    R.string.global_search_subject_description_format,
                                    subjectName
                            ),
                            subjectName,
                            "",
                            ""
                    )
            );

            List<ChapterItem> chapterItems =
                    ChapterCatalog.getChapters(
                            activeStudentProfile
                                    .getEducationBoard(),
                            activeStudentProfile
                                    .getStudentClass(),
                            subjectName
                    );

            for (ChapterItem chapterItem : chapterItems) {
                String chapterTitle =
                        safeText(
                                chapterItem.getChapterTitle()
                        );

                String chapterDescription =
                        safeText(
                                chapterItem
                                        .getChapterDescription()
                        );

                if (chapterTitle.isEmpty()) {
                    continue;
                }

                allSearchItems.add(
                        new GlobalSearchItem(
                                GlobalSearchItem.ResultType.CHAPTER,
                                chapterTitle,
                                subjectName,
                                chapterDescription.isEmpty()
                                        ? getString(
                                        R.string.global_search_chapter_default_description
                                )
                                        : chapterDescription,
                                subjectName,
                                chapterTitle,
                                chapterDescription
                        )
                );

                String savedNote =
                        notesPreferences.getString(
                                createNoteKey(
                                        activeStudentProfile
                                                .getProfileId(),
                                        subjectName,
                                        chapterTitle
                                ),
                                ""
                        );

                if (savedNote != null
                        && !savedNote.trim().isEmpty()) {

                    allSearchItems.add(
                            new GlobalSearchItem(
                                    GlobalSearchItem.ResultType.NOTE,
                                    chapterTitle,
                                    getString(
                                            R.string.global_search_note_subtitle_format,
                                            subjectName
                                    ),
                                    savedNote,
                                    subjectName,
                                    chapterTitle,
                                    chapterDescription
                            )
                    );
                }
            }
        }

        Collections.sort(
                allSearchItems,
                (firstItem, secondItem) -> {
                    int typeComparison =
                            firstItem.getResultType()
                                    .compareTo(
                                            secondItem
                                                    .getResultType()
                                    );

                    if (typeComparison != 0) {
                        return typeComparison;
                    }

                    return firstItem.getTitle()
                            .compareToIgnoreCase(
                                    secondItem.getTitle()
                            );
                }
        );

        applySearchFilter();
    }

    private void applySearchFilter() {
        if (activeStudentProfile == null) {
            searchAdapter.submitList(
                    new ArrayList<>()
            );

            return;
        }

        String searchQuery =
                binding.editGlobalSearch.getText() == null
                        ? ""
                        : binding.editGlobalSearch
                        .getText()
                        .toString();

        String normalizedQuery =
                normalizeText(searchQuery);

        if (normalizedQuery.isEmpty()) {
            showSearchStartState();
            return;
        }

        List<GlobalSearchItem> filteredItems =
                new ArrayList<>();

        for (GlobalSearchItem searchItem
                : allSearchItems) {

            if (matchesSearch(
                    searchItem,
                    normalizedQuery
            )) {
                filteredItems.add(searchItem);
            }
        }

        searchAdapter.submitList(
                filteredItems
        );

        showFilteredResults(
                filteredItems.size()
        );
    }

    private boolean matchesSearch(
            @NonNull GlobalSearchItem searchItem,
            @NonNull String normalizedQuery
    ) {
        return normalizeText(
                searchItem.getTitle()
        ).contains(normalizedQuery)

                || normalizeText(
                searchItem.getSubtitle()
        ).contains(normalizedQuery)

                || normalizeText(
                searchItem.getDescription()
        ).contains(normalizedQuery)

                || normalizeText(
                searchItem.getSubjectName()
        ).contains(normalizedQuery)

                || normalizeText(
                searchItem.getChapterTitle()
        ).contains(normalizedQuery);
    }

    private void showSearchStartState() {
        searchAdapter.submitList(
                new ArrayList<>()
        );

        binding.recyclerGlobalSearch.setVisibility(
                View.GONE
        );

        binding.cardGlobalSearchState.setVisibility(
                View.VISIBLE
        );

        binding.textGlobalSearchStateTitle.setText(
                R.string.global_search_start_title
        );

        binding.textGlobalSearchStateDescription.setText(
                R.string.global_search_start_description
        );

        binding.buttonBrowseSubjects.setVisibility(
                View.VISIBLE
        );

        binding.textGlobalSearchCount.setText(
                getString(
                        R.string.global_search_index_count_format,
                        allSearchItems.size()
                )
        );
    }

    private void showFilteredResults(
            int resultCount
    ) {
        binding.textGlobalSearchCount.setText(
                getString(
                        R.string.global_search_result_count_format,
                        resultCount
                )
        );

        boolean hasResults =
                resultCount > 0;

        binding.recyclerGlobalSearch.setVisibility(
                hasResults
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardGlobalSearchState.setVisibility(
                hasResults
                        ? View.GONE
                        : View.VISIBLE
        );

        if (!hasResults) {
            binding.textGlobalSearchStateTitle.setText(
                    R.string.global_search_no_results_title
            );

            binding.textGlobalSearchStateDescription.setText(
                    R.string.global_search_no_results_description
            );

            binding.buttonBrowseSubjects.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void openSearchResult(
            @NonNull GlobalSearchItem searchItem
    ) {
        switch (searchItem.getResultType()) {
            case SUBJECT:
                openSubjects();
                break;

            case NOTE:
                openChapterNote(searchItem);
                break;

            case CHAPTER:
            default:
                openChapterLesson(searchItem);
                break;
        }
    }

    private void openChapterLesson(
            @NonNull GlobalSearchItem searchItem
    ) {
        if (activeStudentProfile == null) {
            return;
        }

        Intent lessonIntent = new Intent(
                GlobalSearchActivity.this,
                LessonActivity.class
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                searchItem.getSubjectName()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                searchItem.getChapterTitle()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                searchItem.getChapterDescription()
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

    private void openChapterNote(
            @NonNull GlobalSearchItem searchItem
    ) {
        Intent notesIntent = new Intent(
                GlobalSearchActivity.this,
                ChapterNotesActivity.class
        );

        notesIntent.putExtra(
                ChapterNotesActivity.EXTRA_PREFILL_SUBJECT,
                searchItem.getSubjectName()
        );

        notesIntent.putExtra(
                ChapterNotesActivity.EXTRA_PREFILL_CHAPTER,
                searchItem.getChapterTitle()
        );

        startActivity(notesIntent);
    }

    private void openSubjects() {
        Intent subjectsIntent = new Intent(
                GlobalSearchActivity.this,
                SubjectsActivity.class
        );

        startActivity(subjectsIntent);
    }

    private void showNoProfileState() {
        activeStudentProfile = null;

        allSearchItems.clear();

        searchAdapter.submitList(
                new ArrayList<>()
        );

        binding.textGlobalSearchStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.editGlobalSearch.setText("");
        binding.editGlobalSearch.setEnabled(false);

        binding.textGlobalSearchCount.setText(
                R.string.global_search_profile_required_count
        );

        binding.recyclerGlobalSearch.setVisibility(
                View.GONE
        );

        binding.cardGlobalSearchState.setVisibility(
                View.VISIBLE
        );

        binding.textGlobalSearchStateTitle.setText(
                R.string.global_search_profile_required_title
        );

        binding.textGlobalSearchStateDescription.setText(
                R.string.global_search_profile_required_description
        );

        binding.buttonBrowseSubjects.setVisibility(
                View.VISIBLE
        );

        binding.buttonBrowseSubjects.setEnabled(false);
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
        binding.progressGlobalSearch.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.contentGlobalSearch.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }
}