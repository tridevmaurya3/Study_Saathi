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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.BookmarksAdapter;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.catalog.SubjectCatalog;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityBookmarksBinding;
import com.tridev.studysaathi.model.BookmarkItem;
import com.tridev.studysaathi.model.ChapterItem;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BookmarksActivity
        extends AppCompatActivity {

    private static final String BOOKMARK_PREFERENCES_NAME =
            "study_saathi_bookmarks_v1";

    private ActivityBookmarksBinding binding;

    private StudentProfileRepository
            studentProfileRepository;

    private SharedPreferences bookmarkPreferences;

    private BookmarksAdapter bookmarksAdapter;

    private StudentProfileEntity activeStudentProfile;

    private final List<String> subjectOptions =
            new ArrayList<>();

    private final List<ChapterItem> chapterItems =
            new ArrayList<>();

    private final List<String> chapterOptions =
            new ArrayList<>();

    private final List<BookmarkItem> allBookmarks =
            new ArrayList<>();

    private String selectedSubject = "";
    private String selectedChapter = "";
    private String selectedChapterDescription = "";

    private boolean updatingDropdowns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityBookmarksBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        bookmarkPreferences =
                getSharedPreferences(
                        BOOKMARK_PREFERENCES_NAME,
                        MODE_PRIVATE
                );

        setupRecyclerView();
        setupClickListeners();
        setupSearchListener();
        setupDropdownListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadActiveStudentProfile();
    }

    private void setupRecyclerView() {
        bookmarksAdapter =
                new BookmarksAdapter(
                        new ArrayList<>(),
                        this::openBookmarkedLesson,
                        this::confirmRemoveBookmark
                );

        binding.recyclerBookmarks.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerBookmarks.setAdapter(
                bookmarksAdapter
        );

        binding.recyclerBookmarks.setHasFixedSize(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonAddBookmark.setOnClickListener(view ->
                addSelectedBookmark()
        );

        binding.buttonBrowseSubjects.setOnClickListener(view ->
                openSubjects()
        );
    }

    private void setupSearchListener() {
        binding.editSearchBookmarks.addTextChangedListener(
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

    private void setupDropdownListeners() {
        binding.dropdownBookmarkSubject
                .setOnItemClickListener(
                        (parent, view, position, id) -> {
                            if (updatingDropdowns
                                    || position < 0
                                    || position
                                    >= subjectOptions.size()) {
                                return;
                            }

                            selectedSubject =
                                    subjectOptions.get(
                                            position
                                    );

                            setupChapterDropdown();
                        }
                );

        binding.dropdownBookmarkChapter
                .setOnItemClickListener(
                        (parent, view, position, id) -> {
                            if (updatingDropdowns
                                    || position < 0
                                    || position
                                    >= chapterItems.size()) {
                                return;
                            }

                            applySelectedChapter(
                                    position
                            );

                            updateAddBookmarkButton();
                        }
                );
    }

    private void loadActiveStudentProfile() {
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
                        loadProfileBookmarks();
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
                                R.string.bookmarks_profile_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textBookmarksStudent.setText(
                getString(
                        R.string.bookmarks_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );

        binding.editSearchBookmarks.setEnabled(true);
        binding.buttonBrowseSubjects.setEnabled(true);
    }

    private void setupSubjectDropdown() {
        subjectOptions.clear();

        if (activeStudentProfile == null) {
            disableBookmarkSelection();
            return;
        }

        List<SubjectItem> subjects =
                SubjectCatalog.getSubjectsForClass(
                        activeStudentProfile
                                .getStudentClass()
                );

        for (SubjectItem subjectItem : subjects) {
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
            disableBookmarkSelection();

            Snackbar.make(
                    binding.getRoot(),
                    R.string.bookmarks_no_subjects,
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        ArrayAdapter<String> subjectAdapter =
                new ArrayAdapter<>(
                        this,
                        R.layout.item_professional_dropdown,
                        subjectOptions
                );

        binding.dropdownBookmarkSubject.setAdapter(
                subjectAdapter
        );

        selectedSubject =
                subjectOptions.get(0);

        updatingDropdowns = true;

        binding.dropdownBookmarkSubject.setText(
                selectedSubject,
                false
        );

        updatingDropdowns = false;

        binding.dropdownBookmarkSubject.setEnabled(true);

        setupChapterDropdown();
    }

    private void setupChapterDropdown() {
        chapterItems.clear();
        chapterOptions.clear();

        selectedChapter = "";
        selectedChapterDescription = "";

        if (activeStudentProfile == null
                || selectedSubject.isEmpty()) {
            disableChapterSelection();
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
            disableChapterSelection();

            Snackbar.make(
                    binding.getRoot(),
                    R.string.bookmarks_no_chapters,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        ArrayAdapter<String> chapterAdapter =
                new ArrayAdapter<>(
                        this,
                        R.layout.item_professional_dropdown,
                        chapterOptions
                );

        binding.dropdownBookmarkChapter.setAdapter(
                chapterAdapter
        );

        applySelectedChapter(0);

        updatingDropdowns = true;

        binding.dropdownBookmarkChapter.setText(
                selectedChapter,
                false
        );

        updatingDropdowns = false;

        binding.dropdownBookmarkChapter.setEnabled(true);

        updateAddBookmarkButton();
    }

    private void applySelectedChapter(int position) {
        if (position < 0
                || position >= chapterItems.size()) {

            selectedChapter = "";
            selectedChapterDescription = "";
            return;
        }

        ChapterItem selectedChapterItem =
                chapterItems.get(position);

        selectedChapter =
                safeText(
                        selectedChapterItem
                                .getChapterTitle()
                );

        selectedChapterDescription =
                safeText(
                        selectedChapterItem
                                .getChapterDescription()
                );

        updatingDropdowns = true;

        binding.dropdownBookmarkChapter.setText(
                selectedChapter,
                false
        );

        updatingDropdowns = false;
    }

    private void addSelectedBookmark() {
        if (!hasValidSelection()) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.bookmarks_select_chapter_first,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        String bookmarkKey =
                createBookmarkKey(
                        activeStudentProfile.getProfileId(),
                        selectedSubject,
                        selectedChapter
                );

        if (bookmarkPreferences.getBoolean(
                bookmarkKey,
                false
        )) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.bookmarks_already_saved,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        bookmarkPreferences.edit()
                .putBoolean(
                        bookmarkKey,
                        true
                )
                .apply();

        Snackbar.make(
                binding.getRoot(),
                R.string.bookmarks_saved,
                Snackbar.LENGTH_SHORT
        ).show();

        loadProfileBookmarks();
        updateAddBookmarkButton();
    }

    private void loadProfileBookmarks() {
        allBookmarks.clear();

        if (activeStudentProfile == null) {
            applySearchFilter();
            return;
        }

        List<SubjectItem> subjects =
                SubjectCatalog.getSubjectsForClass(
                        activeStudentProfile
                                .getStudentClass()
                );

        for (SubjectItem subjectItem : subjects) {
            String subjectName =
                    safeText(
                            subjectItem.getSubjectName()
                    );

            if (subjectName.isEmpty()) {
                continue;
            }

            List<ChapterItem> chapters =
                    ChapterCatalog.getChapters(
                            activeStudentProfile
                                    .getEducationBoard(),
                            activeStudentProfile
                                    .getStudentClass(),
                            subjectName
                    );

            for (ChapterItem chapterItem : chapters) {
                String chapterTitle =
                        safeText(
                                chapterItem.getChapterTitle()
                        );

                if (chapterTitle.isEmpty()) {
                    continue;
                }

                String bookmarkKey =
                        createBookmarkKey(
                                activeStudentProfile
                                        .getProfileId(),
                                subjectName,
                                chapterTitle
                        );

                if (!bookmarkPreferences.getBoolean(
                        bookmarkKey,
                        false
                )) {
                    continue;
                }

                allBookmarks.add(
                        new BookmarkItem(
                                subjectName,
                                chapterTitle,
                                safeText(
                                        chapterItem
                                                .getChapterDescription()
                                )
                        )
                );
            }
        }

        Collections.sort(
                allBookmarks,
                (firstBookmark, secondBookmark) -> {
                    int subjectComparison =
                            firstBookmark
                                    .getSubjectName()
                                    .compareToIgnoreCase(
                                            secondBookmark
                                                    .getSubjectName()
                                    );

                    if (subjectComparison != 0) {
                        return subjectComparison;
                    }

                    return firstBookmark
                            .getChapterTitle()
                            .compareToIgnoreCase(
                                    secondBookmark
                                            .getChapterTitle()
                            );
                }
        );

        applySearchFilter();
    }

    private void applySearchFilter() {
        String searchText =
                binding.editSearchBookmarks.getText()
                        == null
                        ? ""
                        : binding.editSearchBookmarks
                        .getText()
                        .toString();

        String normalizedSearch =
                normalizeText(searchText);

        List<BookmarkItem> filteredBookmarks =
                new ArrayList<>();

        for (BookmarkItem bookmarkItem
                : allBookmarks) {

            if (normalizedSearch.isEmpty()
                    || matchesSearch(
                    bookmarkItem,
                    normalizedSearch
            )) {
                filteredBookmarks.add(
                        bookmarkItem
                );
            }
        }

        bookmarksAdapter.submitList(
                filteredBookmarks
        );

        showBookmarksState(
                filteredBookmarks.size(),
                allBookmarks.size(),
                !normalizedSearch.isEmpty()
        );
    }

    private boolean matchesSearch(
            @NonNull BookmarkItem bookmarkItem,
            @NonNull String normalizedSearch
    ) {
        return normalizeText(
                bookmarkItem.getSubjectName()
        ).contains(normalizedSearch)

                || normalizeText(
                bookmarkItem.getChapterTitle()
        ).contains(normalizedSearch)

                || normalizeText(
                bookmarkItem.getChapterDescription()
        ).contains(normalizedSearch);
    }

    private void showBookmarksState(
            int visibleCount,
            int totalCount,
            boolean searchApplied
    ) {
        binding.textBookmarksCount.setText(
                getString(
                        R.string.bookmarks_count_format,
                        visibleCount,
                        totalCount
                )
        );

        boolean bookmarksAvailable =
                visibleCount > 0;

        binding.recyclerBookmarks.setVisibility(
                bookmarksAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardBookmarksEmpty.setVisibility(
                bookmarksAvailable
                        ? View.GONE
                        : View.VISIBLE
        );

        if (bookmarksAvailable) {
            return;
        }

        if (totalCount > 0 && searchApplied) {
            binding.textBookmarksEmptyTitle.setText(
                    R.string.bookmarks_no_matching_title
            );

            binding.textBookmarksEmptyDescription.setText(
                    R.string.bookmarks_no_matching_description
            );

            return;
        }

        binding.textBookmarksEmptyTitle.setText(
                R.string.bookmarks_empty_title
        );

        binding.textBookmarksEmptyDescription.setText(
                R.string.bookmarks_empty_description
        );
    }

    private void confirmRemoveBookmark(
            @NonNull BookmarkItem bookmarkItem
    ) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.bookmarks_remove_title
                )
                .setMessage(
                        getString(
                                R.string.bookmarks_remove_message_format,
                                bookmarkItem.getChapterTitle()
                        )
                )
                .setNegativeButton(
                        R.string.bookmarks_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.bookmarks_remove_action,
                        (dialog, which) ->
                                removeBookmark(
                                        bookmarkItem
                                )
                )
                .show();
    }

    private void removeBookmark(
            @NonNull BookmarkItem bookmarkItem
    ) {
        if (activeStudentProfile == null) {
            return;
        }

        bookmarkPreferences.edit()
                .remove(
                        createBookmarkKey(
                                activeStudentProfile
                                        .getProfileId(),
                                bookmarkItem.getSubjectName(),
                                bookmarkItem.getChapterTitle()
                        )
                )
                .apply();

        Snackbar.make(
                binding.getRoot(),
                R.string.bookmarks_removed,
                Snackbar.LENGTH_SHORT
        ).show();

        loadProfileBookmarks();
        updateAddBookmarkButton();
    }

    private void openBookmarkedLesson(
            @NonNull BookmarkItem bookmarkItem
    ) {
        if (activeStudentProfile == null) {
            return;
        }

        Intent lessonIntent = new Intent(
                BookmarksActivity.this,
                LessonActivity.class
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                bookmarkItem.getSubjectName()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                bookmarkItem.getChapterTitle()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                bookmarkItem.getChapterDescription()
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

    private void openSubjects() {
        if (activeStudentProfile == null) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.bookmarks_profile_required,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        Intent subjectsIntent = new Intent(
                BookmarksActivity.this,
                SubjectsActivity.class
        );

        startActivity(subjectsIntent);
    }

    private void updateAddBookmarkButton() {
        if (!hasValidSelection()) {
            binding.buttonAddBookmark.setEnabled(false);
            binding.buttonAddBookmark.setText(
                    R.string.bookmarks_add_button
            );
            return;
        }

        boolean alreadyBookmarked =
                bookmarkPreferences.getBoolean(
                        createBookmarkKey(
                                activeStudentProfile
                                        .getProfileId(),
                                selectedSubject,
                                selectedChapter
                        ),
                        false
                );

        binding.buttonAddBookmark.setEnabled(
                !alreadyBookmarked
        );

        binding.buttonAddBookmark.setText(
                alreadyBookmarked
                        ? R.string.bookmarks_saved_button
                        : R.string.bookmarks_add_button
        );
    }

    private void disableBookmarkSelection() {
        selectedSubject = "";
        selectedChapter = "";
        selectedChapterDescription = "";

        binding.dropdownBookmarkSubject.setText(
                "",
                false
        );

        binding.dropdownBookmarkChapter.setText(
                "",
                false
        );

        binding.dropdownBookmarkSubject.setEnabled(false);
        binding.dropdownBookmarkChapter.setEnabled(false);
        binding.buttonAddBookmark.setEnabled(false);
    }

    private void disableChapterSelection() {
        selectedChapter = "";
        selectedChapterDescription = "";

        binding.dropdownBookmarkChapter.setText(
                "",
                false
        );

        binding.dropdownBookmarkChapter.setEnabled(false);
        binding.buttonAddBookmark.setEnabled(false);
    }

    private void showNoProfileState() {
        activeStudentProfile = null;

        allBookmarks.clear();

        bookmarksAdapter.submitList(
                new ArrayList<>()
        );

        disableBookmarkSelection();

        binding.textBookmarksStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.editSearchBookmarks.setText("");
        binding.editSearchBookmarks.setEnabled(false);

        binding.textBookmarksCount.setText(
                getString(
                        R.string.bookmarks_count_format,
                        0,
                        0
                )
        );

        binding.recyclerBookmarks.setVisibility(
                View.GONE
        );

        binding.cardBookmarksEmpty.setVisibility(
                View.VISIBLE
        );

        binding.textBookmarksEmptyTitle.setText(
                R.string.bookmarks_profile_required_title
        );

        binding.textBookmarksEmptyDescription.setText(
                R.string.bookmarks_profile_required
        );

        binding.buttonBrowseSubjects.setEnabled(false);
    }

    private boolean hasValidSelection() {
        return activeStudentProfile != null
                && activeStudentProfile.getProfileId() > 0L
                && !selectedSubject.isEmpty()
                && !selectedChapter.isEmpty();
    }

    @NonNull
    private String createBookmarkKey(
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
        binding.progressBookmarks.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.contentBookmarks.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }
}