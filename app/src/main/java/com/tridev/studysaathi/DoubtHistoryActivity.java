package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.DoubtHistoryAdapter;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.DoubtHistoryRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityDoubtHistoryBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DoubtHistoryActivity
        extends AppCompatActivity {

    private ActivityDoubtHistoryBinding binding;

    private StudentProfileRepository
            studentProfileRepository;

    private DoubtHistoryRepository
            doubtHistoryRepository;

    private DoubtHistoryAdapter
            doubtHistoryAdapter;

    private final List<DoubtHistoryEntity>
            allHistoryItems = new ArrayList<>();

    private final List<String>
            subjectFilterOptions = new ArrayList<>();

    private long activeProfileId = -1L;

    private String activeStudentName = "Student";

    private String selectedSubjectFilter = "";

    private boolean historyOperationInProgress;

    private boolean filterListenersReady;
    private final List<StudentProfileEntity> availableProfiles =
            new ArrayList<>();
    private boolean selectingStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDoubtHistoryBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        doubtHistoryRepository =
                new DoubtHistoryRepository(this);

        setupRecyclerView();
        setupClickListeners();
        setupSearchListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadStudentChoices();
    }

    private void loadStudentChoices() {
        studentProfileRepository.getAllProfiles(
                new StudentProfileRepository.ProfilesCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<StudentProfileEntity> profiles
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        availableProfiles.clear();
                        availableProfiles.addAll(profiles);
                        List<String> labels = new ArrayList<>();
                        int activePosition = 0;
                        for (int index = 0; index < profiles.size(); index++) {
                            StudentProfileEntity profile = profiles.get(index);
                            labels.add(profile.getStudentName()
                                    + " • " + profile.getStudentClass());
                            if (profile.isActive()) {
                                activePosition = index;
                            }
                        }
                        selectingStudent = true;
                        binding.spinnerHistoryStudent.setAdapter(
                                new ArrayAdapter<>(
                                        DoubtHistoryActivity.this,
                                        android.R.layout.simple_spinner_dropdown_item,
                                        labels));
                        binding.spinnerHistoryStudent.setSelection(
                                activePosition,
                                false);
                        selectingStudent = false;
                        binding.spinnerHistoryStudent.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(
                                            AdapterView<?> parent,
                                            View view,
                                            int position,
                                            long id
                                    ) {
                                        if (!selectingStudent
                                                && position >= 0
                                                && position < availableProfiles.size()) {
                                            loadSelectedStudentHistory(
                                                    availableProfiles.get(position));
                                        }
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {
                                    }
                                });
                        if (!profiles.isEmpty()) {
                            loadSelectedStudentHistory(
                                    profiles.get(activePosition));
                        } else {
                            showLoadingState(false);
                            showNoProfileState();
                        }
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        showLoadingState(false);
                        showNoProfileState();
                    }
                });
    }

    private void loadSelectedStudentHistory(
            @NonNull StudentProfileEntity profile
    ) {
        activeProfileId = profile.getProfileId();
        activeStudentName = profile.getStudentName();
        showStudentInformation(profile);
        loadProfileHistory(profile.getProfileId());
    }

    private void setupRecyclerView() {
        doubtHistoryAdapter =
                new DoubtHistoryAdapter(
                        new ArrayList<>(),
                        this::openHistoryQuestion,
                        this::confirmDeleteHistory
                );

        binding.recyclerDoubtHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerDoubtHistory.setAdapter(
                doubtHistoryAdapter
        );

        binding.recyclerDoubtHistory.setHasFixedSize(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.buttonStartAsking.setOnClickListener(view ->
                openAskStudySaathi()
        );

        binding.buttonClearHistory.setOnClickListener(view ->
                confirmClearCompleteHistory()
        );

        binding.buttonClearFilters.setOnClickListener(view ->
                clearHistoryFilters()
        );
    }

    private void setupSearchListener() {
        binding.editSearchHistory.addTextChangedListener(
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
                        if (!filterListenersReady
                                || historyOperationInProgress) {
                            return;
                        }

                        applyHistoryFilters();
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

    private void loadDoubtHistory() {
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

                        if (studentProfile == null) {
                            showLoadingState(false);
                            showNoProfileState();
                            return;
                        }

                        activeProfileId =
                                studentProfile.getProfileId();

                        activeStudentName =
                                studentProfile.getStudentName();

                        showStudentInformation(
                                studentProfile
                        );

                        loadProfileHistory(
                                studentProfile.getProfileId()
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
                                R.string.doubt_history_profile_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textHistoryStudent.setText(
                getString(
                        R.string.doubt_history_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );
    }

    private void loadProfileHistory(long profileId) {
        doubtHistoryRepository.getHistoryForProfile(
                profileId,
                new DoubtHistoryRepository.HistoryListCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<DoubtHistoryEntity> historyList
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        historyOperationInProgress = false;

                        allHistoryItems.clear();
                        allHistoryItems.addAll(historyList);

                        setupSubjectFilter();
                        showLoadingState(false);
                        applyHistoryFilters();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        historyOperationInProgress = false;

                        allHistoryItems.clear();

                        setupSubjectFilter();
                        showLoadingState(false);
                        applyHistoryFilters();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.doubt_history_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void setupSubjectFilter() {
        String previousSelectedSubject =
                selectedSubjectFilter;

        Set<String> uniqueSubjects =
                new LinkedHashSet<>();

        for (DoubtHistoryEntity historyItem
                : allHistoryItems) {

            String subjectName =
                    historyItem.getSubjectName();

            if (subjectName != null
                    && !subjectName.trim().isEmpty()) {

                uniqueSubjects.add(
                        subjectName.trim()
                );
            }
        }

        List<String> sortedSubjects =
                new ArrayList<>(uniqueSubjects);

        Collections.sort(
                sortedSubjects,
                String.CASE_INSENSITIVE_ORDER
        );

        subjectFilterOptions.clear();

        subjectFilterOptions.add(
                getString(R.string.all_subjects)
        );

        subjectFilterOptions.addAll(
                sortedSubjects
        );

        ArrayAdapter<String> filterAdapter =
                new ArrayAdapter<>(
                        this,
                        R.layout.item_professional_dropdown,
                        subjectFilterOptions
                );

        binding.dropdownHistorySubject.setAdapter(
                filterAdapter
        );

        selectedSubjectFilter =
                findAvailableSubjectFilter(
                        previousSelectedSubject
                );

        if (selectedSubjectFilter.isEmpty()) {
            selectedSubjectFilter =
                    getString(R.string.all_subjects);
        }

        binding.dropdownHistorySubject.setText(
                selectedSubjectFilter,
                false
        );

        binding.dropdownHistorySubject.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position < 0
                            || position
                            >= subjectFilterOptions.size()) {
                        return;
                    }

                    selectedSubjectFilter =
                            subjectFilterOptions.get(position);

                    if (filterListenersReady
                            && !historyOperationInProgress) {
                        applyHistoryFilters();
                    }
                }
        );

        filterListenersReady = true;
    }

    @NonNull
    private String findAvailableSubjectFilter(
            String previousFilter
    ) {
        String normalizedPrevious =
                normalizeText(previousFilter);

        if (normalizedPrevious.isEmpty()) {
            return "";
        }

        for (String filterOption
                : subjectFilterOptions) {

            if (normalizeText(filterOption)
                    .equals(normalizedPrevious)) {

                return filterOption;
            }
        }

        return "";
    }

    private void applyHistoryFilters() {
        String searchQuery =
                binding.editSearchHistory.getText() == null
                        ? ""
                        : binding.editSearchHistory
                        .getText()
                        .toString()
                        .trim();

        String normalizedSearch =
                normalizeText(searchQuery);

        String allSubjectsLabel =
                getString(R.string.all_subjects);

        boolean allSubjectsSelected =
                selectedSubjectFilter == null
                        || selectedSubjectFilter.trim().isEmpty()
                        || normalizeText(
                        selectedSubjectFilter
                ).equals(
                        normalizeText(allSubjectsLabel)
                );

        List<DoubtHistoryEntity> filteredItems =
                new ArrayList<>();

        for (DoubtHistoryEntity historyItem
                : allHistoryItems) {

            boolean subjectMatches =
                    allSubjectsSelected
                            || normalizeText(
                            historyItem.getSubjectName()
                    ).equals(
                            normalizeText(
                                    selectedSubjectFilter
                            )
                    );

            boolean searchMatches =
                    normalizedSearch.isEmpty()
                            || containsSearchText(
                            historyItem,
                            normalizedSearch
                    );

            if (subjectMatches && searchMatches) {
                filteredItems.add(historyItem);
            }
        }

        showFilteredHistoryList(
                filteredItems
        );
    }

    private boolean containsSearchText(
            @NonNull DoubtHistoryEntity historyItem,
            @NonNull String normalizedSearch
    ) {
        return normalizeText(
                historyItem.getQuestionText()
        ).contains(normalizedSearch)

                || normalizeText(
                historyItem.getAnswerText()
        ).contains(normalizedSearch)

                || normalizeText(
                historyItem.getChapterTitle()
        ).contains(normalizedSearch)

                || normalizeText(
                historyItem.getSubjectName()
        ).contains(normalizedSearch);
    }

    private void showFilteredHistoryList(
            @NonNull List<DoubtHistoryEntity> filteredItems
    ) {
        doubtHistoryAdapter.submitList(
                filteredItems
        );

        int totalHistoryCount =
                allHistoryItems.size();

        int filteredHistoryCount =
                filteredItems.size();

        binding.textFilterResultCount.setText(
                getString(
                        R.string.filter_result_count_format,
                        filteredHistoryCount,
                        totalHistoryCount
                )
        );

        boolean fullHistoryAvailable =
                totalHistoryCount > 0;

        boolean filteredHistoryAvailable =
                filteredHistoryCount > 0;

        boolean filterApplied =
                isHistoryFilterApplied();

        binding.cardHistoryFilters.setVisibility(
                fullHistoryAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.buttonClearHistory.setVisibility(
                fullHistoryAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.buttonClearHistory.setEnabled(
                fullHistoryAvailable
                        && !historyOperationInProgress
        );

        binding.recyclerDoubtHistory.setVisibility(
                filteredHistoryAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptyDoubtHistory.setVisibility(
                filteredHistoryAvailable
                        ? View.GONE
                        : View.VISIBLE
        );

        if (!fullHistoryAvailable) {
            binding.textEmptyHistoryTitle.setText(
                    R.string.no_doubt_history
            );

            binding.textEmptyHistoryDescription.setText(
                    R.string.no_doubt_history_description
            );

            binding.buttonStartAsking.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        if (filterApplied
                && !filteredHistoryAvailable) {

            binding.textEmptyHistoryTitle.setText(
                    R.string.no_matching_doubts
            );

            binding.textEmptyHistoryDescription.setText(
                    R.string.no_matching_doubts_description
            );

            binding.buttonStartAsking.setVisibility(
                    View.GONE
            );

            return;
        }

        binding.textEmptyHistoryTitle.setText(
                R.string.no_doubt_history
        );

        binding.textEmptyHistoryDescription.setText(
                R.string.no_doubt_history_description
        );

        binding.buttonStartAsking.setVisibility(
                View.VISIBLE
        );
    }

    private boolean isHistoryFilterApplied() {
        String searchText =
                binding.editSearchHistory.getText() == null
                        ? ""
                        : binding.editSearchHistory
                        .getText()
                        .toString()
                        .trim();

        String allSubjectsLabel =
                getString(R.string.all_subjects);

        boolean subjectFiltered =
                selectedSubjectFilter != null
                        && !selectedSubjectFilter.trim().isEmpty()
                        && !normalizeText(
                        selectedSubjectFilter
                ).equals(
                        normalizeText(allSubjectsLabel)
                );

        return !searchText.isEmpty()
                || subjectFiltered;
    }

    private void clearHistoryFilters() {
        if (historyOperationInProgress) {
            return;
        }

        filterListenersReady = false;

        binding.editSearchHistory.setText("");

        selectedSubjectFilter =
                getString(R.string.all_subjects);

        binding.dropdownHistorySubject.setText(
                selectedSubjectFilter,
                false
        );

        filterListenersReady = true;

        applyHistoryFilters();
    }

    private void confirmDeleteHistory(
            @NonNull DoubtHistoryEntity historyItem
    ) {
        if (historyOperationInProgress) {
            return;
        }

        String shortQuestion =
                createShortQuestion(
                        historyItem.getQuestionText()
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.delete_doubt_title
                )
                .setMessage(
                        getString(
                                R.string.delete_doubt_message_format,
                                shortQuestion
                        )
                )
                .setNegativeButton(
                        R.string.doubt_action_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.doubt_action_delete,
                        (dialog, which) ->
                                deleteSingleHistory(
                                        historyItem.getHistoryId()
                                )
                )
                .show();
    }

    private void deleteSingleHistory(long historyId) {
        if (historyOperationInProgress) {
            return;
        }

        historyOperationInProgress = true;

        showOperationLoadingState(
                R.string.deleting_saved_doubt
        );

        doubtHistoryRepository.deleteHistoryById(
                historyId,
                new DoubtHistoryRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.saved_doubt_deleted,
                                Snackbar.LENGTH_SHORT
                        ).show();

                        reloadCurrentProfileHistory();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        historyOperationInProgress = false;
                        showLoadingState(false);

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.delete_doubt_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void confirmClearCompleteHistory() {
        if (historyOperationInProgress
                || activeProfileId <= 0L) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.clear_history_title
                )
                .setMessage(
                        getString(
                                R.string.clear_history_message_format,
                                activeStudentName
                        )
                )
                .setNegativeButton(
                        R.string.doubt_action_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.doubt_action_clear_all,
                        (dialog, which) ->
                                clearCompleteHistory()
                )
                .show();
    }

    private void clearCompleteHistory() {
        if (historyOperationInProgress
                || activeProfileId <= 0L) {
            return;
        }

        historyOperationInProgress = true;

        showOperationLoadingState(
                R.string.clearing_doubt_history
        );

        doubtHistoryRepository.deleteHistoryForProfile(
                activeProfileId,
                new DoubtHistoryRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.complete_doubt_history_cleared,
                                Snackbar.LENGTH_SHORT
                        ).show();

                        clearHistoryFilters();
                        reloadCurrentProfileHistory();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        historyOperationInProgress = false;
                        showLoadingState(false);

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.clear_doubt_history_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void reloadCurrentProfileHistory() {
        if (activeProfileId <= 0L) {
            historyOperationInProgress = false;
            showNoProfileState();
            return;
        }

        loadProfileHistory(activeProfileId);
    }

    private void showOperationLoadingState(
            int messageRes
    ) {
        binding.progressDoubtHistory.setVisibility(
                View.VISIBLE
        );

        binding.contentDoubtHistory.setAlpha(0.55f);
        binding.contentDoubtHistory.setEnabled(false);

        binding.textFilterResultCount.setText(
                messageRes
        );

        binding.buttonClearHistory.setEnabled(
                false
        );

        binding.buttonClearFilters.setEnabled(
                false
        );
    }

    private void showNoProfileState() {
        activeProfileId = -1L;
        activeStudentName = "Student";
        selectedSubjectFilter = "";
        historyOperationInProgress = false;
        filterListenersReady = false;

        allHistoryItems.clear();
        subjectFilterOptions.clear();

        binding.textHistoryStudent.setText(
                R.string.create_profile_to_continue
        );

        binding.editSearchHistory.setText("");
        binding.editSearchHistory.setEnabled(false);

        binding.dropdownHistorySubject.setText(
                "",
                false
        );

        binding.dropdownHistorySubject.setEnabled(
                false
        );

        binding.buttonClearFilters.setEnabled(
                false
        );

        binding.buttonStartAsking.setEnabled(
                false
        );

        showFilteredHistoryList(
                new ArrayList<>()
        );
    }

    private void openHistoryQuestion(
            @NonNull DoubtHistoryEntity historyItem
    ) {
        if (historyOperationInProgress) {
            return;
        }

        Intent askIntent = new Intent(
                DoubtHistoryActivity.this,
                AskStudySaathiActivity.class
        );

        askIntent.putExtra(
                AskStudySaathiActivity.EXTRA_PREFILL_SUBJECT,
                historyItem.getSubjectName()
        );

        askIntent.putExtra(
                AskStudySaathiActivity.EXTRA_PREFILL_CHAPTER,
                historyItem.getChapterTitle()
        );

        askIntent.putExtra(
                AskStudySaathiActivity.EXTRA_PREFILL_QUESTION,
                historyItem.getQuestionText()
        );

        startActivity(askIntent);
    }

    private void openAskStudySaathi() {
        Intent askIntent = new Intent(
                DoubtHistoryActivity.this,
                AskStudySaathiActivity.class
        );

        startActivity(askIntent);
    }

    @NonNull
    private String createShortQuestion(
            String question
    ) {
        if (question == null
                || question.trim().isEmpty()) {
            return getString(
                    R.string.saved_doubt
            );
        }

        String trimmedQuestion =
                question.trim();

        if (trimmedQuestion.length() <= 90) {
            return trimmedQuestion;
        }

        return trimmedQuestion
                .substring(0, 90)
                .trim()
                + "…";
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
        binding.progressDoubtHistory.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.contentDoubtHistory.setVisibility(
                loading ? View.INVISIBLE : View.VISIBLE
        );

        binding.contentDoubtHistory.setAlpha(1f);
        binding.contentDoubtHistory.setEnabled(true);

        binding.buttonClearFilters.setEnabled(
                !loading
        );
    }
}
