package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.SchoolCurriculumSubjectAdapter;
import com.tridev.studysaathi.data.content.bootstrap.SchoolCurriculumBootstrapper;
import com.tridev.studysaathi.data.content.validation.SchoolCurriculumSetupValidator;
import com.tridev.studysaathi.data.local.entity.SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.SchoolCurriculumProfileRepository;
import com.tridev.studysaathi.data.repository.SchoolSubjectRepository;
import com.tridev.studysaathi.data.repository.SchoolSubjectRemovalRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.data.schooldirectory.adapter.SchoolDirectorySchoolAdapter;
import com.tridev.studysaathi.data.schooldirectory.entity.DistrictDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.SchoolDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.StateDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.repository.SchoolDirectoryRepository;
import com.tridev.studysaathi.data.schooldirectory.storage.SchoolDirectorySelectionStore;
import com.tridev.studysaathi.databinding.ActivitySchoolCurriculumSetupBinding;
import com.tridev.studysaathi.databinding.DialogAddSchoolSubjectBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SchoolCurriculumSetupActivity extends AppCompatActivity implements SchoolCurriculumSubjectAdapter.SubjectActionListener {
    public static final String EXTRA_TARGET_SUBJECT_ROW_ID = "extra_target_subject_row_id";
    public static final String EXTRA_TARGET_PROFILE_ID = "extra_target_profile_id";
    public static final String EXTRA_TARGET_SUBJECT_NAME = "extra_target_subject_name";
    private static final String DEFAULT_SCHOOL_NAME = "School details pending";
    private static final String CONTENT_SOURCE_SCHOOL_BOOK = "SCHOOL_BOOK";
    private static final int SCHOOL_SEARCH_LIMIT = 100;
    private ActivitySchoolCurriculumSetupBinding binding;
    private StudentProfileRepository studentProfileRepository;
    private SchoolCurriculumProfileRepository curriculumProfileRepository;
    private SchoolSubjectRepository schoolSubjectRepository;
    private SchoolSubjectRemovalRepository schoolSubjectRemovalRepository;
    private SchoolDirectoryRepository schoolDirectoryRepository;
    private SchoolDirectorySelectionStore schoolDirectorySelectionStore;
    private SchoolCurriculumSubjectAdapter subjectAdapter;
    private SchoolDirectorySchoolAdapter schoolAdapter;
    private ActivityResultLauncher<Intent> bookSetupLauncher;
    @Nullable
    private StudentProfileEntity activeStudentProfile;
    @Nullable
    private SchoolCurriculumProfileEntity curriculumProfile;
    @Nullable
    private StateDirectoryEntity selectedState;
    @Nullable
    private DistrictDirectoryEntity selectedDistrict;
    @Nullable
    private SchoolDirectoryEntity selectedDirectorySchool;
    @NonNull
    private String selectedEducationBoard = "";
    @NonNull
    private List<StateDirectoryEntity> availableStates = new ArrayList<>();
    @NonNull
    private List<DistrictDirectoryEntity> availableDistricts = new ArrayList<>();
    private boolean operationInProgress;
    private boolean directoryOperationInProgress;
    private boolean formBindingInProgress;
    private boolean directoryBindingInProgress;
    private boolean saveAttempted;
    private boolean manualSchoolEntryMode;
    private boolean schoolDirectoryStatesLoaded;
    private boolean restoringSavedSchoolSelection;
    @Nullable
    private SchoolDirectorySelectionStore.SelectionData pendingSavedSchoolSelection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySchoolCurriculumSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        studentProfileRepository = new StudentProfileRepository(this);
        curriculumProfileRepository = new SchoolCurriculumProfileRepository(this);
        schoolSubjectRepository = new SchoolSubjectRepository(this);
        schoolSubjectRemovalRepository = new SchoolSubjectRemovalRepository(this);
        schoolDirectoryRepository = new SchoolDirectoryRepository(this);
        schoolDirectorySelectionStore = new SchoolDirectorySelectionStore(this);
        registerBookSetupLauncher();
        setupRecyclerView();
        setupSchoolDirectoryAdapters();
        setupToolbar();
        setupClickListeners();
        setupFormValidation();
        setupSchoolDirectoryListeners();
        resetSchoolDirectorySelection();
        loadActiveStudent();
        initializeSchoolDirectory();
    }

    private void registerBookSetupLauncher() {
        bookSetupLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (!isActivityAvailable()) {
                return;
            }
            if (result.getResultCode() != RESULT_OK) {
                return;
            }
            Intent resultData = result.getData();
            String savedBookTitle = "";
            if (resultData != null) {
                savedBookTitle = safeText(resultData.getStringExtra(ManualSchoolBookActivity.RESULT_BOOK_TITLE));
            }
            SchoolCurriculumProfileEntity profile = curriculumProfile;
            if (profile != null && profile.getProfileId() > 0L) {
                loadSchoolSubjects(profile.getProfileId());
            }
            String message = savedBookTitle.isEmpty() ? "Exact school book save हो गई है।" : savedBookTitle + " save हो गई है।";
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            setResult(RESULT_OK);
        });
    }

    private void setupRecyclerView() {
        subjectAdapter = new SchoolCurriculumSubjectAdapter(this);
        binding.recyclerCurriculumSubjects.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCurriculumSubjects.setAdapter(subjectAdapter);
        binding.recyclerCurriculumSubjects.setHasFixedSize(false);
        updateSubjectListState();
    }

    private void setupSchoolDirectoryAdapters() {
        schoolAdapter = new SchoolDirectorySchoolAdapter(this);
        binding.inputDirectorySchoolSearch.setAdapter(schoolAdapter);
        binding.inputDirectorySchoolSearch.setThreshold(0);
    }

    private void setupToolbar() {
        binding.toolbarSchoolCurriculumSetup.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupClickListeners() {
        binding.buttonAddSchoolSubject.setOnClickListener(view -> showAddSubjectDialog());
        binding.buttonImportSubjectList.setOnClickListener(view -> showImportSubjectMessage());
        binding.buttonSaveSchoolCurriculum.setOnClickListener(view -> saveSchoolCurriculum());
        binding.buttonCancelCurriculumSetup.setOnClickListener(view -> finish());
        binding.buttonEnterSchoolManually.setOnClickListener(view -> enableManualSchoolEntryMode());
        binding.buttonChangeSelectedSchool.setOnClickListener(view -> changeSelectedSchool());
    }

    private void setupFormValidation() {
        addValidationWatcher(binding.inputSchoolName);
        addValidationWatcher(binding.inputSchoolCode);
        addValidationWatcher(binding.inputEducationBoard);
        addValidationWatcher(binding.inputClassNumber);
        addValidationWatcher(binding.inputSection);
        addValidationWatcher(binding.inputAcademicSession);
        addValidationWatcher(binding.inputStudyMedium);
        refreshValidationState(false);
    }

    private void addValidationWatcher(@NonNull EditText input) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (formBindingInProgress) {
                    return;
                }
                refreshValidationState(saveAttempted);
            }
        });
    }

    private void setupSchoolDirectoryListeners() {
        binding.inputDirectoryState.setOnItemClickListener((parent, view, position, id) -> {
            if (directoryBindingInProgress) {
                return;
            }
            if (position < 0 || position >= availableStates.size()) {
                return;
            }
            StateDirectoryEntity state = availableStates.get(position);
            cancelPendingSavedSelectionRestore();
            selectState(state);
        });
        binding.inputDirectoryDistrict.setOnItemClickListener((parent, view, position, id) -> {
            if (directoryBindingInProgress) {
                return;
            }
            if (position < 0 || position >= availableDistricts.size()) {
                return;
            }
            DistrictDirectoryEntity district = availableDistricts.get(position);
            cancelPendingSavedSelectionRestore();
            selectDistrict(district);
        });
        binding.inputDirectoryEducationBoard.setOnItemClickListener((parent, view, position, id) -> {
            if (directoryBindingInProgress) {
                return;
            }
            String board = safeText(parent.getItemAtPosition(position));
            cancelPendingSavedSelectionRestore();
            selectEducationBoard(board);
        });
        binding.inputDirectorySchoolSearch.setOnItemClickListener((parent, view, position, id) -> {
            SchoolDirectoryEntity school = schoolAdapter.getSchoolAt(position);
            if (school != null) {
                cancelPendingSavedSelectionRestore();
                selectDirectorySchool(school);
            }
        });
        binding.inputDirectorySchoolSearch.setOnClickListener(view -> {
            if (binding.inputDirectorySchoolSearch.isEnabled() && schoolAdapter.hasSchools()) {
                binding.inputDirectorySchoolSearch.showDropDown();
            }
        });
        binding.inputDirectorySchoolSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (directoryBindingInProgress || selectedDistrict == null || selectedEducationBoard.isEmpty()) {
                    return;
                }
                String searchText = safeText(editable);
                searchSchools(searchText);
            }
        });
    }

    private void initializeSchoolDirectory() {
        setDirectoryLoadingState(true, "School directory तैयार की जा रही है");
        schoolDirectoryRepository.ensureStarterDirectory(new SchoolDirectoryRepository.StarterDirectoryCallback() {
            @Override
            public void onReady(@NonNull SchoolDirectoryRepository.StarterDirectoryResult result) {
                if (!isActivityAvailable()) {
                    return;
                }
                loadStates();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setDirectoryLoadingState(false, "");
                showDirectoryError("School directory तैयार नहीं हो सकी। Manual Entry का उपयोग करें।");
                enableManualSchoolEntryMode();
            }
        });
    }

    private void loadStates() {
        setDirectoryLoadingState(true, "States load किए जा रहे हैं");
        schoolDirectoryRepository.getActiveStates(new SchoolDirectoryRepository.StatesCallback() {
            @Override
            public void onSuccess(@NonNull List<StateDirectoryEntity> states) {
                if (!isActivityAvailable()) {
                    return;
                }
                availableStates = new ArrayList<>(states);
                List<String> stateNames = new ArrayList<>();
                for (StateDirectoryEntity state : states) {
                    stateNames.add(state.getStateName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(SchoolCurriculumSetupActivity.this, android.R.layout.simple_list_item_1, stateNames);
                binding.inputDirectoryState.setAdapter(adapter);
                binding.layoutDirectoryState.setEnabled(!states.isEmpty());
                setDirectoryLoadingState(false, "");
                if (states.isEmpty()) {
                    showDirectoryError("State directory उपलब्ध नहीं है।");
                    enableManualSchoolEntryMode();
                    return;
                }
                schoolDirectoryStatesLoaded = true;
                if (!attemptRestoreSavedSchoolSelection()) {
                    selectPreferredDefaultState(states);
                }
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setDirectoryLoadingState(false, "");
                showDirectoryError("State list load नहीं हो सकी।");
                enableManualSchoolEntryMode();
            }
        });
    }

    private void selectPreferredDefaultState(@NonNull List<StateDirectoryEntity> states) {
        for (StateDirectoryEntity state : states) {
            if ("UP".equalsIgnoreCase(state.getStateCode())) {
                directoryBindingInProgress = true;
                binding.inputDirectoryState.setText(state.getStateName(), false);
                directoryBindingInProgress = false;
                selectState(state);
                return;
            }
        }
    }

    private void selectState(@NonNull StateDirectoryEntity state) {
        selectedState = state;
        selectedDistrict = null;
        selectedDirectorySchool = null;
        selectedEducationBoard = "";
        availableDistricts = new ArrayList<>();
        directoryBindingInProgress = true;
        binding.inputDirectoryState.setText(state.getStateName(), false);
        binding.inputDirectoryDistrict.setText("", false);
        binding.inputDirectoryEducationBoard.setText("", false);
        binding.inputDirectorySchoolSearch.setText("", false);
        directoryBindingInProgress = false;
        binding.layoutDirectoryDistrict.setEnabled(false);
        binding.layoutDirectoryEducationBoard.setEnabled(false);
        binding.layoutDirectorySchoolSearch.setEnabled(false);
        schoolAdapter.clearSchools();
        hideSelectedSchoolCard();
        hideDirectoryNoResult();
        loadDistricts(state.getStateCode());
    }

    private void loadDistricts(@NonNull String stateCode) {
        final String requestedStateCode = safeText(stateCode);
        setDirectoryLoadingState(true, "Districts load किए जा रहे हैं");
        schoolDirectoryRepository.getDistrictsForState(requestedStateCode, new SchoolDirectoryRepository.DistrictsCallback() {
            @Override
            public void onSuccess(@NonNull List<DistrictDirectoryEntity> districts) {
                if (!isActivityAvailable() || !isCurrentSelectedState(requestedStateCode)) {
                    return;
                }
                availableDistricts = new ArrayList<>(districts);
                List<String> districtNames = new ArrayList<>();
                for (DistrictDirectoryEntity district : districts) {
                    districtNames.add(district.getDistrictName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(SchoolCurriculumSetupActivity.this, android.R.layout.simple_list_item_1, districtNames);
                binding.inputDirectoryDistrict.setAdapter(adapter);
                binding.layoutDirectoryDistrict.setEnabled(!districts.isEmpty());
                setDirectoryLoadingState(false, "");
                if (restoringSavedSchoolSelection && pendingSavedSchoolSelection != null) {
                    restoreSavedDistrictAfterLoad(districts);
                    return;
                }
                if (districts.isEmpty()) {
                    showDirectoryNoResult("इस State के districts अभी directory में उपलब्ध नहीं हैं। Manual Entry का उपयोग करें।");
                }
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable() || !isCurrentSelectedState(requestedStateCode)) {
                    return;
                }
                setDirectoryLoadingState(false, "");
                if (restoringSavedSchoolSelection && pendingSavedSchoolSelection != null) {
                    restoreSavedSelectionWithoutDirectory();
                    return;
                }
                showDirectoryNoResult("District list load नहीं हो सकी। Manual Entry का उपयोग करें।");
            }
        });
    }

    private void selectDistrict(@NonNull DistrictDirectoryEntity district) {
        selectedDistrict = district;
        selectedDirectorySchool = null;
        selectedEducationBoard = "";
        directoryBindingInProgress = true;
        binding.inputDirectoryDistrict.setText(district.getDistrictName(), false);
        binding.inputDirectoryEducationBoard.setText("", false);
        binding.inputDirectorySchoolSearch.setText("", false);
        directoryBindingInProgress = false;
        binding.layoutDirectoryEducationBoard.setEnabled(false);
        binding.layoutDirectorySchoolSearch.setEnabled(false);
        schoolAdapter.clearSchools();
        hideSelectedSchoolCard();
        hideDirectoryNoResult();
        loadEducationBoards(district.getDistrictCode());
    }

    private void loadEducationBoards(@NonNull String districtCode) {
        final String requestedDistrictCode = safeText(districtCode);
        setDirectoryLoadingState(true, "Education Boards load किए जा रहे हैं");
        schoolDirectoryRepository.getEducationBoardsForDistrict(requestedDistrictCode, new SchoolDirectoryRepository.BoardsCallback() {
            @Override
            public void onSuccess(@NonNull List<String> educationBoards) {
                if (!isActivityAvailable() || !isCurrentSelectedDistrict(requestedDistrictCode)) {
                    return;
                }
                List<String> boardLabels = new ArrayList<>();
                for (String board : educationBoards) {
                    boardLabels.add(formatBoardName(board));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(SchoolCurriculumSetupActivity.this, android.R.layout.simple_list_item_1, boardLabels);
                binding.inputDirectoryEducationBoard.setAdapter(adapter);
                binding.inputDirectoryEducationBoard.setTag(new ArrayList<>(educationBoards));
                binding.layoutDirectoryEducationBoard.setEnabled(!educationBoards.isEmpty());
                setDirectoryLoadingState(false, "");
                if (restoringSavedSchoolSelection && pendingSavedSchoolSelection != null) {
                    restoreSavedBoardAfterLoad(educationBoards);
                    return;
                }
                if (educationBoards.isEmpty()) {
                    showDirectoryNoResult("Education Board list उपलब्ध नहीं है। Manual Entry का उपयोग करें।");
                }
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable() || !isCurrentSelectedDistrict(requestedDistrictCode)) {
                    return;
                }
                setDirectoryLoadingState(false, "");
                if (restoringSavedSchoolSelection && pendingSavedSchoolSelection != null) {
                    restoreSavedSelectionWithoutDirectory();
                    return;
                }
                showDirectoryNoResult("Education Board list load नहीं हो सकी।");
            }
        });
    }

    private void selectEducationBoard(@NonNull String displayedBoard) {
        String normalizedBoard = normalizeBoard(displayedBoard);
        selectedEducationBoard = normalizedBoard;
        selectedDirectorySchool = null;
        directoryBindingInProgress = true;
        binding.inputDirectoryEducationBoard.setText(formatBoardName(normalizedBoard), false);
        binding.inputDirectorySchoolSearch.setText("", false);
        directoryBindingInProgress = false;
        binding.layoutDirectorySchoolSearch.setEnabled(selectedDistrict != null && !normalizedBoard.isEmpty());
        schoolAdapter.clearSchools();
        hideSelectedSchoolCard();
        hideDirectoryNoResult();
        loadSchoolsForSelectedFilters();
    }

    private void loadSchoolsForSelectedFilters() {
        DistrictDirectoryEntity district = selectedDistrict;
        if (district == null || selectedEducationBoard.isEmpty()) {
            return;
        }
        final String requestedDistrictCode = district.getDistrictCode();
        final String requestedBoard = selectedEducationBoard;
        setDirectoryLoadingState(true, "Schools खोजे जा रहे हैं");
        schoolDirectoryRepository.getSchools(requestedDistrictCode, requestedBoard, new SchoolDirectoryRepository.SchoolsCallback() {
            @Override
            public void onSuccess(@NonNull List<SchoolDirectoryEntity> schools) {
                if (!isActivityAvailable() || !isCurrentSchoolFilter(requestedDistrictCode, requestedBoard)) {
                    return;
                }
                schoolAdapter.submitSchools(schools);
                setDirectoryLoadingState(false, "");
                if (restoringSavedSchoolSelection && pendingSavedSchoolSelection != null) {
                    restoreSavedSchoolAfterLoad(schools);
                    return;
                }
                if (schools.isEmpty()) {
                    showDirectoryNoResult("इस District और Board के verified school records अभी import नहीं हुए हैं। School details manually भरें।");
                } else {
                    hideDirectoryNoResult();
                    binding.inputDirectorySchoolSearch.showDropDown();
                }
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable() || !isCurrentSchoolFilter(requestedDistrictCode, requestedBoard)) {
                    return;
                }
                setDirectoryLoadingState(false, "");
                if (restoringSavedSchoolSelection && pendingSavedSchoolSelection != null) {
                    restoreSavedSchoolAfterLoad(new ArrayList<>());
                    return;
                }
                showDirectoryNoResult("School list load नहीं हो सकी। Manual Entry का उपयोग करें।");
            }
        });
    }

    private void searchSchools(@NonNull String searchText) {
        DistrictDirectoryEntity district = selectedDistrict;
        if (district == null || selectedEducationBoard.isEmpty()) {
            return;
        }
        schoolDirectoryRepository.searchSchools(district.getDistrictCode(), selectedEducationBoard, searchText, SCHOOL_SEARCH_LIMIT, new SchoolDirectoryRepository.SchoolsCallback() {
            @Override
            public void onSuccess(@NonNull List<SchoolDirectoryEntity> schools) {
                if (!isActivityAvailable()) {
                    return;
                }
                schoolAdapter.submitSchools(schools);
                if (schools.isEmpty()) {
                    showDirectoryNoResult("कोई matching school नहीं मिला। नाम या code जाँचें अथवा Manual Entry करें।");
                } else {
                    hideDirectoryNoResult();
                    if (binding.inputDirectorySchoolSearch.hasFocus()) {
                        binding.inputDirectorySchoolSearch.showDropDown();
                    }
                }
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                showDirectoryNoResult("School search पूरी नहीं हो सकी।");
            }
        });
    }

    private void selectDirectorySchool(@NonNull SchoolDirectoryEntity school) {
        selectedDirectorySchool = school;
        manualSchoolEntryMode = false;
        directoryBindingInProgress = true;
        binding.inputDirectorySchoolSearch.setText(school.getSchoolName(), false);
        directoryBindingInProgress = false;
        formBindingInProgress = true;
        binding.inputSchoolName.setText(school.getSchoolName());
        binding.inputSchoolCode.setText(school.getPreferredSchoolCode());
        binding.inputEducationBoard.setText(formatBoardName(school.getEducationBoard()));
        formBindingInProgress = false;
        binding.inputSchoolName.setEnabled(false);
        binding.inputSchoolCode.setEnabled(false);
        binding.inputEducationBoard.setEnabled(false);
        binding.textSchoolEntryMode.setText("School Directory Mode");
        binding.textSelectedDirectorySchoolName.setText(school.getSchoolName());
        binding.textSelectedDirectorySchoolDetails.setText(createSelectedSchoolDetails(school));
        binding.textSelectedSchoolVerificationStatus.setText(school.getVerificationLabel());
        binding.textSelectedDirectorySchoolIcon.setText(school.isOfficiallyVerified() ? "✓" : "i");
        binding.cardSelectedDirectorySchool.setVisibility(View.VISIBLE);
        hideDirectoryNoResult();
        refreshValidationState(saveAttempted);
    }

    private void enableManualSchoolEntryMode() {
        cancelPendingSavedSelectionRestore();
        manualSchoolEntryMode = true;
        selectedDirectorySchool = null;
        hideSelectedSchoolCard();
        binding.inputSchoolName.setEnabled(true);
        binding.inputSchoolCode.setEnabled(true);
        binding.inputEducationBoard.setEnabled(true);
        binding.textSchoolEntryMode.setText("Manual School Entry");
        binding.textSchoolDetailsFormDescription.setText("School directory में school न मिलने पर सही School Name और Education Board manually भरें। School Code optional है।");
        if (!selectedEducationBoard.isEmpty() && safeText(binding.inputEducationBoard.getText()).isEmpty()) {
            binding.inputEducationBoard.setText(formatBoardName(selectedEducationBoard));
        }
        binding.inputSchoolName.requestFocus();
        binding.scrollSchoolCurriculumSetup.post(() -> binding.scrollSchoolCurriculumSetup.smoothScrollTo(0, binding.cardSchoolDetailsForm.getTop()));
        Snackbar.make(binding.getRoot(), "Manual School Entry चालू है।", Snackbar.LENGTH_LONG).show();
    }

    private void changeSelectedSchool() {
        cancelPendingSavedSelectionRestore();
        selectedDirectorySchool = null;
        manualSchoolEntryMode = false;
        hideSelectedSchoolCard();
        formBindingInProgress = true;
        binding.inputSchoolName.setText("");
        binding.inputSchoolCode.setText("");
        binding.inputEducationBoard.setText(selectedEducationBoard.isEmpty() ? "" : formatBoardName(selectedEducationBoard));
        formBindingInProgress = false;
        binding.inputSchoolName.setEnabled(true);
        binding.inputSchoolCode.setEnabled(true);
        binding.inputEducationBoard.setEnabled(true);
        binding.textSchoolEntryMode.setText("School Directory Mode");
        binding.textSchoolDetailsFormDescription.setText("Directory से school चुनने पर School Name, Code और Board अपने-आप भरेंगे।");
        binding.inputDirectorySchoolSearch.requestFocus();
        if (schoolAdapter.hasSchools()) {
            binding.inputDirectorySchoolSearch.showDropDown();
        }
        refreshValidationState(saveAttempted);
    }

    private void resetSchoolDirectorySelection() {
        selectedState = null;
        selectedDistrict = null;
        selectedDirectorySchool = null;
        selectedEducationBoard = "";
        manualSchoolEntryMode = false;
        schoolDirectoryStatesLoaded = false;
        restoringSavedSchoolSelection = false;
        pendingSavedSchoolSelection = null;
        binding.layoutDirectoryState.setEnabled(false);
        binding.layoutDirectoryDistrict.setEnabled(false);
        binding.layoutDirectoryEducationBoard.setEnabled(false);
        binding.layoutDirectorySchoolSearch.setEnabled(false);
        hideSelectedSchoolCard();
        hideDirectoryNoResult();
    }

    private void setDirectoryLoadingState(boolean loading, @Nullable String message) {
        directoryOperationInProgress = loading;
        binding.containerSchoolDirectoryLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        String safeMessage = safeText(message);
        if (!safeMessage.isEmpty()) {
            binding.textSchoolDirectoryLoading.setText(safeMessage);
        }
        binding.buttonEnterSchoolManually.setEnabled(!loading);
    }

    private void showDirectoryNoResult(@NonNull String message) {
        binding.textSchoolDirectoryNoResultDescription.setText(message);
        binding.cardSchoolDirectoryNoResult.setVisibility(View.VISIBLE);
    }

    private void hideDirectoryNoResult() {
        binding.cardSchoolDirectoryNoResult.setVisibility(View.GONE);
    }

    private void hideSelectedSchoolCard() {
        binding.cardSelectedDirectorySchool.setVisibility(View.GONE);
    }

    private void showDirectoryError(@NonNull String message) {
        showDirectoryNoResult(message);
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void loadActiveStudent() {
        setOperationState(true, "Student profile load किया जा रहा है");
        studentProfileRepository.getActiveProfile(new StudentProfileRepository.SingleProfileCallback() {
            @Override
            public void onSuccess(@Nullable StudentProfileEntity studentProfile) {
                if (!isActivityAvailable()) {
                    return;
                }
                if (studentProfile == null) {
                    setOperationState(false, "");
                    showMissingStudentProfileError();
                    return;
                }
                activeStudentProfile = studentProfile;
                displayStudentInformation(studentProfile);
                ensureCurriculumProfile(studentProfile);
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setOperationState(false, "");
                showError("Active student profile load नहीं हो सकी।");
            }
        });
    }

    private void ensureCurriculumProfile(@NonNull StudentProfileEntity studentProfile) {
        setOperationState(true, "School curriculum तैयार किया जा रहा है");
        curriculumProfileRepository.ensureBasicCurriculumProfile(studentProfile, new SchoolCurriculumProfileRepository.EnsureProfileCallback() {
            @Override
            public void onReady(@NonNull SchoolCurriculumProfileEntity profile, boolean newlyCreated) {
                if (!isActivityAvailable()) {
                    return;
                }
                curriculumProfile = profile;
                displayCurriculumProfile(profile);
                loadSchoolSubjects(profile.getProfileId());
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setOperationState(false, "");
                showError("School curriculum profile तैयार नहीं हो सकी।");
            }
        });
    }

    private void loadSchoolSubjects(long profileId) {
        setOperationState(true, "Confirmed school subjects load किए जा रहे हैं");
        schoolSubjectRepository.getSubjectsForProfile(profileId, false, new SchoolSubjectRepository.SubjectsCallback() {
            @Override
            public void onSuccess(@NonNull List<SchoolSubjectEntity> schoolSubjects) {
                if (!isActivityAvailable()) {
                    return;
                }
                subjectAdapter.submitList(schoolSubjects);
                updateSubjectListState();
                setOperationState(false, "");
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                subjectAdapter.submitList(new ArrayList<>());
                updateSubjectListState();
                setOperationState(false, "");
                showError("School subjects load नहीं हो सके।");
            }
        });
    }

    private void displayStudentInformation(@NonNull StudentProfileEntity studentProfile) {
        binding.textCurriculumStudentName.setText(valueOrFallback(studentProfile.getStudentName(), "Active Student"));
        StringBuilder summaryBuilder = new StringBuilder();
        appendSummaryPart(summaryBuilder, studentProfile.getEducationBoard());
        appendSummaryPart(summaryBuilder, studentProfile.getStudentClass());
        appendSummaryPart(summaryBuilder, studentProfile.getStudyMedium());
        binding.textCurriculumStudentSummary.setText(summaryBuilder.length() > 0 ? summaryBuilder.toString() : "Student academic details");
    }

    private void displayCurriculumProfile(@NonNull SchoolCurriculumProfileEntity profile) {
        formBindingInProgress = true;
        String schoolName = safeText(profile.getSchoolName());
        if (schoolName.equalsIgnoreCase(DEFAULT_SCHOOL_NAME)) {
            schoolName = "";
        }
        binding.inputSchoolName.setText(schoolName);
        binding.inputSchoolCode.setText(safeText(profile.getSchoolCode()));
        binding.inputEducationBoard.setText(safeText(profile.getEducationBoard()));
        binding.inputClassNumber.setText(String.valueOf(profile.getClassNumber()));
        binding.inputSection.setText(safeText(profile.getSection()));
        binding.inputAcademicSession.setText(safeText(profile.getAcademicSession()));
        binding.inputStudyMedium.setText(safeText(profile.getStudyMedium()));
        formBindingInProgress = false;
        binding.inputSchoolName.setEnabled(true);
        binding.inputSchoolCode.setEnabled(true);
        binding.inputEducationBoard.setEnabled(true);
        if (!schoolName.isEmpty()) {
            manualSchoolEntryMode = true;
            binding.textSchoolEntryMode.setText("Saved / Manual School Details");
        }
        loadSavedSchoolSelection(profile.getProfileId());
        refreshValidationState(false);
    }

    private void loadSavedSchoolSelection(long profileId) {
        try {
            SchoolDirectorySelectionStore.SelectionData savedSelection = schoolDirectorySelectionStore.getSelection(profileId);
            if (!savedSelection.hasSchoolSelection()) {
                pendingSavedSchoolSelection = null;
                restoringSavedSchoolSelection = false;
                return;
            }
            pendingSavedSchoolSelection = savedSelection;
            restoringSavedSchoolSelection = false;
            attemptRestoreSavedSchoolSelection();
        } catch (Exception exception) {
            pendingSavedSchoolSelection = null;
            restoringSavedSchoolSelection = false;
            Snackbar.make(binding.getRoot(), "Saved State/District selection पढ़ी नहीं जा सकी। School details फिर से चुनें।", Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean attemptRestoreSavedSchoolSelection() {
        SchoolDirectorySelectionStore.SelectionData savedSelection = pendingSavedSchoolSelection;
        if (savedSelection == null || !savedSelection.hasSchoolSelection() || !schoolDirectoryStatesLoaded || availableStates.isEmpty() || restoringSavedSchoolSelection) {
            return false;
        }
        restoringSavedSchoolSelection = true;
        StateDirectoryEntity savedState = findMatchingState(availableStates, savedSelection);
        if (savedState == null) {
            selectedState = null;
            selectedDistrict = null;
            restoreSavedSelectionWithoutDirectory();
            return true;
        }
        selectState(savedState);
        return true;
    }

    private void restoreSavedDistrictAfterLoad(@NonNull List<DistrictDirectoryEntity> districts) {
        SchoolDirectorySelectionStore.SelectionData savedSelection = pendingSavedSchoolSelection;
        if (!restoringSavedSchoolSelection || savedSelection == null) {
            return;
        }
        DistrictDirectoryEntity savedDistrict = findMatchingDistrict(districts, savedSelection);
        if (savedDistrict == null) {
            restoreSavedSelectionWithoutDirectory();
            return;
        }
        selectDistrict(savedDistrict);
    }

    private void restoreSavedBoardAfterLoad(@NonNull List<String> educationBoards) {
        SchoolDirectorySelectionStore.SelectionData savedSelection = pendingSavedSchoolSelection;
        if (!restoringSavedSchoolSelection || savedSelection == null) {
            return;
        }
        String savedBoard = normalizeBoard(savedSelection.getEducationBoard());
        if (savedBoard.isEmpty()) {
            restoreSavedSelectionWithoutDirectory();
            return;
        }
        boolean boardAvailable = educationBoards.isEmpty();
        for (String board : educationBoards) {
            if (savedBoard.equals(normalizeBoard(board))) {
                boardAvailable = true;
                break;
            }
        }
        selectedEducationBoard = savedBoard;
        directoryBindingInProgress = true;
        binding.inputDirectoryEducationBoard.setText(formatBoardName(savedBoard), false);
        binding.inputDirectorySchoolSearch.setText("", false);
        directoryBindingInProgress = false;
        binding.layoutDirectorySchoolSearch.setEnabled(selectedDistrict != null && boardAvailable);
        schoolAdapter.clearSchools();
        hideSelectedSchoolCard();
        hideDirectoryNoResult();
        if (savedSelection.isManualSelection()) {
            applySavedManualSelection(savedSelection);
            return;
        }
        if (!boardAvailable || selectedDistrict == null) {
            restoreSavedSelectionWithoutDirectory();
            return;
        }
        loadSchoolsForSelectedFilters();
    }

    private void restoreSavedSchoolAfterLoad(@NonNull List<SchoolDirectoryEntity> schools) {
        SchoolDirectorySelectionStore.SelectionData savedSelection = pendingSavedSchoolSelection;
        if (!restoringSavedSchoolSelection || savedSelection == null) {
            return;
        }
        SchoolDirectoryEntity matchingSchool = findMatchingSchool(schools, savedSelection);
        if (matchingSchool != null) {
            selectDirectorySchool(matchingSchool);
            completeSavedSchoolSelectionRestore();
            return;
        }
        if (savedSelection.hasDirectoryIdentity()) {
            schoolDirectoryRepository.getSchoolByDirectoryId(savedSelection.getSchoolDirectoryId(), new SchoolDirectoryRepository.SingleSchoolCallback() {
                @Override
                public void onSuccess(@Nullable SchoolDirectoryEntity school) {
                    if (!isActivityAvailable() || !restoringSavedSchoolSelection || pendingSavedSchoolSelection == null) {
                        return;
                    }
                    if (school != null) {
                        selectDirectorySchool(school);
                        completeSavedSchoolSelectionRestore();
                        return;
                    }
                    restoreDirectorySelectionFromStoredData();
                }

                @Override
                public void onError(@NonNull Exception exception) {
                    if (!isActivityAvailable() || !restoringSavedSchoolSelection) {
                        return;
                    }
                    restoreDirectorySelectionFromStoredData();
                }
            });
            return;
        }
        restoreDirectorySelectionFromStoredData();
    }

    private void restoreDirectorySelectionFromStoredData() {
        SchoolDirectorySelectionStore.SelectionData savedSelection = pendingSavedSchoolSelection;
        if (!restoringSavedSchoolSelection || savedSelection == null) {
            return;
        }
        try {
            String schoolDirectoryId = safeText(savedSelection.getSchoolDirectoryId());
            String stateCode = safeText(savedSelection.getStateCode());
            String districtCode = safeText(savedSelection.getDistrictCode());
            String schoolName = safeText(savedSelection.getSchoolName());
            String board = normalizeBoard(savedSelection.getEducationBoard());
            if (schoolDirectoryId.isEmpty() || stateCode.isEmpty() || districtCode.isEmpty() || schoolName.isEmpty() || board.isEmpty()) {
                applySavedManualSelection(savedSelection);
                return;
            }
            SchoolDirectoryEntity restoredSchool = new SchoolDirectoryEntity();
            restoredSchool.setSchoolDirectoryId(schoolDirectoryId);
            restoredSchool.setStateCode(stateCode);
            restoredSchool.setDistrictCode(districtCode);
            restoredSchool.setSchoolName(schoolName);
            restoredSchool.setSchoolNameHindi("");
            restoredSchool.setEducationBoard(board);
            restoredSchool.setSchoolType("");
            restoredSchool.setManagementType("");
            restoredSchool.setUdiseCode(savedSelection.getUdiseCode());
            restoredSchool.setBoardAffiliationNumber(savedSelection.getBoardAffiliationNumber());
            restoredSchool.setSchoolInternalCode(savedSelection.getSchoolInternalCode());
            restoredSchool.setAddressLine(savedSelection.getAddress());
            restoredSchool.setPostalCode("");
            restoredSchool.setDirectorySource(savedSelection.getDirectorySource());
            restoredSchool.setOfficiallyVerified(savedSelection.isOfficiallyVerified());
            restoredSchool.setActive(true);
            restoredSchool.setSourceUpdatedAt(savedSelection.getSavedAt());
            restoredSchool.setCreatedAt(savedSelection.getSavedAt());
            restoredSchool.setUpdatedAt(savedSelection.getSavedAt());
            selectDirectorySchool(restoredSchool);
            completeSavedSchoolSelectionRestore();
        } catch (Exception exception) {
            applySavedManualSelection(savedSelection);
        }
    }

    private void restoreSavedSelectionWithoutDirectory() {
        SchoolDirectorySelectionStore.SelectionData savedSelection = pendingSavedSchoolSelection;
        if (!restoringSavedSchoolSelection || savedSelection == null) {
            return;
        }
        directoryBindingInProgress = true;
        binding.inputDirectoryState.setText(valueOrFallback(savedSelection.getStateName(), savedSelection.getStateCode()), false);
        binding.inputDirectoryDistrict.setText(valueOrFallback(savedSelection.getDistrictName(), savedSelection.getDistrictCode()), false);
        selectedEducationBoard = normalizeBoard(savedSelection.getEducationBoard());
        binding.inputDirectoryEducationBoard.setText(formatBoardName(selectedEducationBoard), false);
        binding.inputDirectorySchoolSearch.setText(savedSelection.isDirectorySelection() ? savedSelection.getSchoolName() : "", false);
        directoryBindingInProgress = false;
        binding.layoutDirectoryEducationBoard.setEnabled(true);
        binding.layoutDirectorySchoolSearch.setEnabled(false);
        if (savedSelection.isDirectorySelection()) {
            restoreDirectorySelectionFromStoredData();
        } else {
            applySavedManualSelection(savedSelection);
        }
    }

    private void applySavedManualSelection(@NonNull SchoolDirectorySelectionStore.SelectionData savedSelection) {
        manualSchoolEntryMode = true;
        selectedDirectorySchool = null;
        selectedEducationBoard = normalizeBoard(savedSelection.getEducationBoard());
        formBindingInProgress = true;
        binding.inputSchoolName.setText(savedSelection.getSchoolName());
        binding.inputSchoolCode.setText(savedSelection.getPreferredSchoolCode());
        binding.inputEducationBoard.setText(formatBoardName(selectedEducationBoard));
        formBindingInProgress = false;
        binding.inputSchoolName.setEnabled(true);
        binding.inputSchoolCode.setEnabled(true);
        binding.inputEducationBoard.setEnabled(true);
        binding.textSchoolEntryMode.setText("Saved Manual School Details");
        binding.textSchoolDetailsFormDescription.setText("यह school Parent द्वारा manually enter किया गया है। School Name और Board को जरूरत पड़ने पर बदला जा सकता है।");
        showSavedManualSelectionCard(savedSelection.getSchoolName(), savedSelection.getPreferredSchoolCode(), savedSelection.getEducationBoard(), savedSelection.getStateName(), savedSelection.getDistrictName(), savedSelection.getVerificationLabel());
        hideDirectoryNoResult();
        completeSavedSchoolSelectionRestore();
        refreshValidationState(false);
    }

    private void completeSavedSchoolSelectionRestore() {
        restoringSavedSchoolSelection = false;
        pendingSavedSchoolSelection = null;
    }

    private void cancelPendingSavedSelectionRestore() {
        restoringSavedSchoolSelection = false;
        pendingSavedSchoolSelection = null;
    }

    @Nullable
    private StateDirectoryEntity findMatchingState(@NonNull List<StateDirectoryEntity> states, @NonNull SchoolDirectorySelectionStore.SelectionData savedSelection) {
        String savedStateCode = normalizeText(savedSelection.getStateCode());
        String savedStateName = normalizeText(savedSelection.getStateName());
        for (StateDirectoryEntity state : states) {
            if (!savedStateCode.isEmpty() && savedStateCode.equals(normalizeText(state.getStateCode()))) {
                return state;
            }
            if (!savedStateName.isEmpty() && savedStateName.equals(normalizeText(state.getStateName()))) {
                return state;
            }
        }
        return null;
    }

    @Nullable
    private DistrictDirectoryEntity findMatchingDistrict(@NonNull List<DistrictDirectoryEntity> districts, @NonNull SchoolDirectorySelectionStore.SelectionData savedSelection) {
        String savedDistrictCode = normalizeText(savedSelection.getDistrictCode());
        String savedDistrictName = normalizeText(savedSelection.getDistrictName());
        for (DistrictDirectoryEntity district : districts) {
            if (!savedDistrictCode.isEmpty() && savedDistrictCode.equals(normalizeText(district.getDistrictCode()))) {
                return district;
            }
            if (!savedDistrictName.isEmpty() && savedDistrictName.equals(normalizeText(district.getDistrictName()))) {
                return district;
            }
        }
        return null;
    }

    @Nullable
    private SchoolDirectoryEntity findMatchingSchool(@NonNull List<SchoolDirectoryEntity> schools, @NonNull SchoolDirectorySelectionStore.SelectionData savedSelection) {
        String savedDirectoryId = normalizeText(savedSelection.getSchoolDirectoryId());
        String savedUdiseCode = normalizeText(savedSelection.getUdiseCode());
        String savedSchoolName = normalizeText(savedSelection.getSchoolName());
        for (SchoolDirectoryEntity school : schools) {
            if (!savedDirectoryId.isEmpty() && savedDirectoryId.equals(normalizeText(school.getSchoolDirectoryId()))) {
                return school;
            }
            if (!savedUdiseCode.isEmpty() && savedUdiseCode.equals(normalizeText(school.getUdiseCode()))) {
                return school;
            }
            if (!savedSchoolName.isEmpty() && savedSchoolName.equals(normalizeText(school.getSchoolName()))) {
                return school;
            }
        }
        return null;
    }

    private boolean isCurrentSelectedState(@NonNull String stateCode) {
        return selectedState != null && safeText(selectedState.getStateCode()).equalsIgnoreCase(safeText(stateCode));
    }

    private boolean isCurrentSelectedDistrict(@NonNull String districtCode) {
        return selectedDistrict != null && safeText(selectedDistrict.getDistrictCode()).equalsIgnoreCase(safeText(districtCode));
    }

    private boolean isCurrentSchoolFilter(@NonNull String districtCode, @NonNull String educationBoard) {
        return isCurrentSelectedDistrict(districtCode) && normalizeBoard(selectedEducationBoard).equals(normalizeBoard(educationBoard));
    }

    private boolean saveCurrentSchoolSelection(long profileId, @NonNull SchoolCurriculumSetupValidator.ValidationResult validationResult) {
        try {
            SchoolDirectoryEntity directorySchool = selectedDirectorySchool;
            String stateCode;
            String stateName;
            if (selectedState != null) {
                stateCode = selectedState.getStateCode();
                stateName = selectedState.getStateName();
            } else {
                stateCode = directorySchool == null ? "" : directorySchool.getStateCode();
                stateName = safeText(binding.inputDirectoryState.getText());
            }
            String districtCode;
            String districtName;
            if (selectedDistrict != null) {
                districtCode = selectedDistrict.getDistrictCode();
                districtName = selectedDistrict.getDistrictName();
            } else {
                districtCode = directorySchool == null ? "" : directorySchool.getDistrictCode();
                districtName = safeText(binding.inputDirectoryDistrict.getText());
            }
            boolean saved;
            if (directorySchool != null && !manualSchoolEntryMode) {
                saved = schoolDirectorySelectionStore.saveDirectorySelection(profileId, stateCode, stateName, districtCode, districtName, directorySchool.getEducationBoard(), directorySchool.getSchoolDirectoryId(), directorySchool.getSchoolName(), directorySchool.getPreferredSchoolCode(), directorySchool.getUdiseCode(), directorySchool.getBoardAffiliationNumber(), directorySchool.getSchoolInternalCode(), directorySchool.getAddressLine(), directorySchool.getDirectorySource(), directorySchool.isOfficiallyVerified());
            } else {
                saved = schoolDirectorySelectionStore.saveManualSelection(profileId, stateCode, stateName, districtCode, districtName, validationResult.getEducationBoard(), validationResult.getSchoolName(), validationResult.getSchoolCode());
                if (saved) {
                    manualSchoolEntryMode = true;
                    selectedDirectorySchool = null;
                    selectedEducationBoard = normalizeBoard(validationResult.getEducationBoard());
                    showSavedManualSelectionCard(validationResult.getSchoolName(), validationResult.getSchoolCode(), validationResult.getEducationBoard(), stateName, districtName, "Parent Entered • Not Officially Verified");
                }
            }
            return saved;
        } catch (Exception exception) {
            return false;
        }
    }

    private void showSavedManualSelectionCard(@Nullable String schoolName, @Nullable String schoolCode, @Nullable String educationBoard, @Nullable String stateName, @Nullable String districtName, @NonNull String verificationLabel) {
        binding.textSelectedDirectorySchoolName.setText(valueOrFallback(schoolName, "Saved School"));
        StringBuilder detailsBuilder = new StringBuilder();
        appendSummaryPart(detailsBuilder, stateName);
        appendSummaryPart(detailsBuilder, districtName);
        appendSummaryPart(detailsBuilder, formatBoardName(educationBoard));
        appendSummaryPart(detailsBuilder, schoolCode);
        binding.textSelectedDirectorySchoolDetails.setText(detailsBuilder.length() == 0 ? "Parent entered school details" : detailsBuilder.toString());
        binding.textSelectedSchoolVerificationStatus.setText(verificationLabel);
        binding.textSelectedDirectorySchoolIcon.setText("i");
        binding.cardSelectedDirectorySchool.setVisibility(View.VISIBLE);
    }

    private void showAddSubjectDialog() {
        if (operationInProgress) {
            return;
        }
        SchoolCurriculumProfileEntity profile = curriculumProfile;
        if (profile == null || profile.getProfileId() <= 0L) {
            showError("पहले curriculum profile तैयार होना आवश्यक है।");
            return;
        }
        DialogAddSchoolSubjectBinding dialogBinding = DialogAddSchoolSubjectBinding.inflate(getLayoutInflater());
        setupSubjectCategoryDropdown(dialogBinding);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogBinding.getRoot()).setNegativeButton("Cancel", null).setPositiveButton("Add Subject", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> validateAndAddSubject(dialog, dialogBinding, profile)));
        dialog.show();
    }

    private void showEditSubjectDialog(@NonNull SchoolSubjectEntity schoolSubject) {
        if (operationInProgress) {
            return;
        }
        if (schoolSubject.getSubjectRowId() <= 0L) {
            showError("Valid school subject उपलब्ध नहीं है।");
            return;
        }
        DialogAddSchoolSubjectBinding dialogBinding = DialogAddSchoolSubjectBinding.inflate(getLayoutInflater());
        setupSubjectCategoryDropdown(dialogBinding);
        dialogBinding.textSubjectDialogIcon.setText("✏️");
        dialogBinding.textSubjectDialogTitle.setText("School Subject Edit करें");
        dialogBinding.textSubjectDialogDescription.setText("Subject का नाम, code, category और Child Mode visibility बदलें।");
        String currentBookName = safeText(schoolSubject.getBookName());
        if (currentBookName.isEmpty()) {
            dialogBinding.textSubjectDialogInformation.setText("Subject changes save होने के बाद इसकी exact school book scan या manually add की जा सकती है।");
        } else {
            dialogBinding.textSubjectDialogInformation.setText("जुड़ी exact school book सुरक्षित रहेगी: " + currentBookName + ". Subject edit करने से book, chapters या progress delete नहीं होंगे।");
        }
        dialogBinding.inputSubjectNameEnglish.setText(schoolSubject.getSubjectNameEnglish());
        dialogBinding.inputSubjectNameHindi.setText(schoolSubject.getSubjectNameHindi());
        dialogBinding.inputSubjectCode.setText(schoolSubject.getSubjectCode());
        dialogBinding.inputSubjectCategory.setText(convertDatabaseCategoryToLabel(schoolSubject.getSubjectCategory()), false);
        dialogBinding.switchSubjectEnabled.setChecked(schoolSubject.isEnabled());
        dialogBinding.switchSubjectAiTutorEnabled.setChecked(schoolSubject.isAiTutorEnabled());
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogBinding.getRoot()).setNegativeButton("Cancel", null).setPositiveButton("Save Changes", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> validateAndUpdateSubject(dialog, dialogBinding, schoolSubject)));
        dialog.show();
    }

    private void validateAndUpdateSubject(@NonNull androidx.appcompat.app.AlertDialog dialog, @NonNull DialogAddSchoolSubjectBinding dialogBinding, @NonNull SchoolSubjectEntity originalSubject) {
        clearSubjectDialogErrors(dialogBinding);
        String englishName = safeText(dialogBinding.inputSubjectNameEnglish.getText());
        String hindiName = safeText(dialogBinding.inputSubjectNameHindi.getText());
        String enteredSubjectCode = safeText(dialogBinding.inputSubjectCode.getText()).toUpperCase(Locale.ROOT);
        String finalSubjectCode = enteredSubjectCode.isEmpty() ? createSubjectCode(englishName) : enteredSubjectCode;
        String categoryLabel = safeText(dialogBinding.inputSubjectCategory.getText());
        if (englishName.isEmpty()) {
            dialogBinding.layoutSubjectNameEnglish.setError("Subject name आवश्यक है");
            return;
        }
        if (isDuplicateSubjectForEdit(originalSubject.getSubjectRowId(), englishName, hindiName, finalSubjectCode)) {
            dialogBinding.textSubjectDialogError.setText("इसी नाम या code वाला दूसरा subject curriculum में मौजूद है।");
            dialogBinding.textSubjectDialogError.setVisibility(View.VISIBLE);
            return;
        }
        String subjectCategory = convertCategoryToDatabaseValue(categoryLabel);
        SchoolSubjectEntity updatedSubject = copySchoolSubject(originalSubject);
        updatedSubject.setSubjectNameEnglish(englishName);
        updatedSubject.setSubjectNameHindi(hindiName);
        updatedSubject.setSubjectCode(finalSubjectCode);
        updatedSubject.setSubjectCategory(subjectCategory);
        updatedSubject.setEnabled(dialogBinding.switchSubjectEnabled.isChecked());
        updatedSubject.setAiTutorEnabled(dialogBinding.switchSubjectAiTutorEnabled.isChecked());
        updatedSubject.setOfficialCoreSubject(subjectCategory.equals("CORE_ACADEMIC") || subjectCategory.equals("LANGUAGE"));
        updatedSubject.setAllowParentContentEditing(true);
        updatedSubject.setContentSource(CONTENT_SOURCE_SCHOOL_BOOK);
        updatedSubject.setUpdatedAt(System.currentTimeMillis());
        updateSubjectInDatabase(dialog, dialogBinding, updatedSubject);
    }

    private void updateSubjectInDatabase(@NonNull androidx.appcompat.app.AlertDialog dialog, @NonNull DialogAddSchoolSubjectBinding dialogBinding, @NonNull SchoolSubjectEntity updatedSubject) {
        android.widget.Button saveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        setOperationState(true, "Subject changes save किए जा रहे हैं");
        schoolSubjectRepository.updateSubject(updatedSubject, new SchoolSubjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isActivityAvailable()) {
                    return;
                }
                dialog.dismiss();
                subjectAdapter.updateSubject(updatedSubject);
                updateSubjectListState();
                setOperationState(false, "");
                setResult(RESULT_OK);
                Snackbar.make(binding.getRoot(), "Subject changes save हो गए। Exact school book सुरक्षित है।", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setOperationState(false, "");
                saveButton.setEnabled(true);
                saveButton.setText("Save Changes");
                dialogBinding.textSubjectDialogError.setText("Subject changes save नहीं हो सके। नाम और code दोबारा जाँचें।");
                dialogBinding.textSubjectDialogError.setVisibility(View.VISIBLE);
                showError("Subject update नहीं हो सका।");
            }
        });
    }

    @NonNull
    private SchoolSubjectEntity copySchoolSubject(@NonNull SchoolSubjectEntity source) {
        SchoolSubjectEntity copy = new SchoolSubjectEntity();
        copy.setSubjectRowId(source.getSubjectRowId());
        copy.setProfileId(source.getProfileId());
        copy.setSubjectId(source.getSubjectId());
        copy.setSubjectNameEnglish(source.getSubjectNameEnglish());
        copy.setSubjectNameHindi(source.getSubjectNameHindi());
        copy.setSubjectCode(source.getSubjectCode());
        copy.setBookName(source.getBookName());
        copy.setBookCode(source.getBookCode());
        copy.setPublisherName(source.getPublisherName());
        copy.setSubjectCategory(source.getSubjectCategory());
        copy.setContentSource(source.getContentSource());
        copy.setContentPackId(source.getContentPackId());
        copy.setEnabled(source.isEnabled());
        copy.setAiTutorEnabled(source.isAiTutorEnabled());
        copy.setOfficialCoreSubject(source.isOfficialCoreSubject());
        copy.setAllowParentContentEditing(source.isAllowParentContentEditing());
        copy.setSortOrder(source.getSortOrder());
        copy.setChapterCount(source.getChapterCount());
        copy.setLessonCount(source.getLessonCount());
        copy.setQuizQuestionCount(source.getQuizQuestionCount());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private void setupSubjectCategoryDropdown(@NonNull DialogAddSchoolSubjectBinding dialogBinding) {
        String[] categories = new String[]{"Core Academic", "Language", "Skill Based", "Activity Based", "School Specific"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        dialogBinding.inputSubjectCategory.setAdapter(categoryAdapter);
        dialogBinding.inputSubjectCategory.setText(categories[0], false);
    }

    private void validateAndAddSubject(@NonNull androidx.appcompat.app.AlertDialog dialog, @NonNull DialogAddSchoolSubjectBinding dialogBinding, @NonNull SchoolCurriculumProfileEntity profile) {
        clearSubjectDialogErrors(dialogBinding);
        String englishName = safeText(dialogBinding.inputSubjectNameEnglish.getText());
        String hindiName = safeText(dialogBinding.inputSubjectNameHindi.getText());
        String subjectCode = safeText(dialogBinding.inputSubjectCode.getText()).toUpperCase(Locale.ROOT);
        String categoryLabel = safeText(dialogBinding.inputSubjectCategory.getText());
        if (englishName.isEmpty()) {
            dialogBinding.layoutSubjectNameEnglish.setError("Subject name आवश्यक है");
            return;
        }
        if (isDuplicateSubject(englishName, hindiName, subjectCode)) {
            dialogBinding.textSubjectDialogError.setText("यह subject पहले से curriculum में मौजूद है।");
            dialogBinding.textSubjectDialogError.setVisibility(View.VISIBLE);
            return;
        }
        dialog.dismiss();
        addSubjectToDatabase(profile, englishName, hindiName, subjectCode, convertCategoryToDatabaseValue(categoryLabel), dialogBinding.switchSubjectEnabled.isChecked(), dialogBinding.switchSubjectAiTutorEnabled.isChecked());
    }

    private void addSubjectToDatabase(@NonNull SchoolCurriculumProfileEntity profile, @NonNull String englishName, @NonNull String hindiName, @NonNull String subjectCode, @NonNull String subjectCategory, boolean enabled, boolean aiTutorEnabled) {
        setOperationState(true, "School subject save किया जा रहा है");
        schoolSubjectRepository.getNextSortOrder(profile.getProfileId(), new SchoolSubjectRepository.SortOrderCallback() {
            @Override
            public void onSuccess(int nextSortOrder) {
                SchoolSubjectEntity schoolSubject = createSchoolSubject(profile.getProfileId(), englishName, hindiName, subjectCode, subjectCategory, enabled, aiTutorEnabled, nextSortOrder);
                insertSchoolSubject(schoolSubject);
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setOperationState(false, "");
                showError("Subject का sort order तैयार नहीं हो सका।");
            }
        });
    }

    private void insertSchoolSubject(@NonNull SchoolSubjectEntity schoolSubject) {
        schoolSubjectRepository.insertSubject(schoolSubject, new SchoolSubjectRepository.InsertSubjectCallback() {
            @Override
            public void onSuccess(long insertedSubjectRowId) {
                if (!isActivityAvailable()) {
                    return;
                }
                schoolSubject.setSubjectRowId(insertedSubjectRowId);
                subjectAdapter.addSubject(schoolSubject);
                updateSubjectListState();
                setOperationState(false, "");
                binding.getRoot().postDelayed(
                        () -> {
                            if (isActivityAvailable()
                                    && !operationInProgress) {
                                showBookAddMethodDialog(
                                        schoolSubject
                                );
                            }
                        },
                        250L
                );
                Snackbar.make(binding.getRoot(), schoolSubject.getSubjectNameEnglish() + " curriculum में जोड़ दिया गया है।", Snackbar.LENGTH_LONG).show();
                setResult(RESULT_OK);
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setOperationState(false, "");
                showError("Subject save नहीं हो सका। संभव है कि यह पहले से मौजूद हो।");
            }
        });
    }

    @NonNull
    private SchoolSubjectEntity createSchoolSubject(long profileId, @NonNull String englishName, @NonNull String hindiName, @NonNull String subjectCode, @NonNull String subjectCategory, boolean enabled, boolean aiTutorEnabled, int sortOrder) {
        long currentTime = System.currentTimeMillis();
        SchoolSubjectEntity schoolSubject = new SchoolSubjectEntity();
        schoolSubject.setProfileId(profileId);
        schoolSubject.setSubjectId(SchoolSubjectEntity.createSubjectId(englishName));
        schoolSubject.setSubjectNameEnglish(englishName);
        schoolSubject.setSubjectNameHindi(hindiName);
        schoolSubject.setSubjectCode(subjectCode.isEmpty() ? createSubjectCode(englishName) : subjectCode);
        schoolSubject.setBookName("");
        schoolSubject.setBookCode("");
        schoolSubject.setPublisherName("");
        schoolSubject.setSubjectCategory(subjectCategory);
        schoolSubject.setContentSource(CONTENT_SOURCE_SCHOOL_BOOK);
        schoolSubject.setContentPackId("");
        schoolSubject.setEnabled(enabled);
        schoolSubject.setAiTutorEnabled(aiTutorEnabled);
        schoolSubject.setOfficialCoreSubject(subjectCategory.equals("CORE_ACADEMIC") || subjectCategory.equals("LANGUAGE"));
        schoolSubject.setAllowParentContentEditing(true);
        schoolSubject.setSortOrder(Math.max(1, sortOrder));
        schoolSubject.setChapterCount(0);
        schoolSubject.setLessonCount(0);
        schoolSubject.setQuizQuestionCount(0);
        schoolSubject.setCreatedAt(currentTime);
        schoolSubject.setUpdatedAt(currentTime);
        return schoolSubject;
    }

    private void saveSchoolCurriculum() {
        if (operationInProgress) {
            return;
        }
        saveAttempted = true;
        SchoolCurriculumSetupValidator.ValidationResult validationResult = createValidationResult();
        applyValidationErrors(validationResult);
        updateSaveButtonState(validationResult);
        if (!validationResult.canSaveSchoolSetup()) {
            focusFirstInvalidField(validationResult);
            showError(createValidationErrorMessage(validationResult));
            return;
        }
        SchoolCurriculumProfileEntity profile = curriculumProfile;
        if (profile == null || profile.getProfileId() <= 0L) {
            showError("Curriculum profile उपलब्ध नहीं है।");
            return;
        }
        updateCurriculumProfileFromForm(profile, validationResult);
        setOperationState(true, "School curriculum save किया जा रहा है");
        curriculumProfileRepository.insertOrUpdateCurriculumProfile(profile, new SchoolCurriculumProfileRepository.SaveProfileCallback() {
            @Override
            public void onSuccess(long savedProfileId) {
                if (!isActivityAvailable()) {
                    return;
                }
                curriculumProfile = profile;
                boolean selectionMetadataSaved = saveCurrentSchoolSelection(profile.getProfileId(), validationResult);
                saveAttempted = false;
                setOperationState(false, "");
                setResult(RESULT_OK);
                if (!selectionMetadataSaved) {
                    Snackbar.make(binding.getRoot(), "Curriculum save हो गया, लेकिन State/District selection सुरक्षित नहीं हो सकी।", Snackbar.LENGTH_LONG).show();
                }
                showCurriculumSaveResult(validationResult);
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setOperationState(false, "");
                showError("School curriculum save नहीं हो सका।");
            }
        });
    }

    private void updateCurriculumProfileFromForm(@NonNull SchoolCurriculumProfileEntity profile, @NonNull SchoolCurriculumSetupValidator.ValidationResult validationResult) {
        profile.setSchoolName(validationResult.getSchoolName());
        profile.setSchoolCode(validationResult.getSchoolCode());
        profile.setEducationBoard(validationResult.getEducationBoard());
        profile.setSchoolPattern(validationResult.getEducationBoard());
        profile.setClassNumber(validationResult.getClassNumber());
        profile.setSection(validationResult.getSection());
        profile.setAcademicSession(validationResult.getAcademicSession());
        profile.setStudyMedium(validationResult.getStudyMedium());
        profile.setConfigured(validationResult.isReadyForChildMode());
        profile.setUpdatedAt(System.currentTimeMillis());
    }

    private void showCurriculumSaveResult(@NonNull SchoolCurriculumSetupValidator.ValidationResult validationResult) {
        String schoolSelectionStatus;
        if (selectedDirectorySchool != null) {
            schoolSelectionStatus = selectedDirectorySchool.getVerificationLabel();
        } else {
            schoolSelectionStatus = "Parent Entered • Not Officially Verified";
        }
        if (validationResult.isReadyForChildMode()) {
            new MaterialAlertDialogBuilder(this).setTitle("Curriculum तैयार है").setMessage(validationResult.getSetupStatusMessage() + "\n\nSchool Status: " + schoolSelectionStatus + "\n\nअब बच्चे को केवल confirmed school subjects और उनकी exact books दिखाई जाएँगी।").setPositiveButton("ठीक है", null).show();
            return;
        }
        int pendingBookCount = validationResult.getPendingBookCount();
        String bookWord = pendingBookCount == 1 ? "book" : "books";
        new MaterialAlertDialogBuilder(this).setTitle("School setup save हो गया").setMessage(validationResult.getSetupStatusMessage() + "\n\nSchool Status: " + schoolSelectionStatus + "\n\n" + pendingBookCount + " exact school " + bookWord + " confirm होने तक Child Mode curriculum locked रहेगा।").setPositiveButton("ठीक है", null).show();
    }

    private void showBookAddMethodDialog(@NonNull SchoolSubjectEntity schoolSubject) {
        if (operationInProgress) {
            return;
        }
        if (schoolSubject.getSubjectRowId() <= 0L) {
            showError("Valid school subject उपलब्ध नहीं है।");
            return;
        }
        String subjectName = getSubjectDisplayName(schoolSubject);
        String existingBookName = safeText(schoolSubject.getBookName());
        String message;
        if (existingBookName.isEmpty()) {
            message = subjectName + " में पढ़ाई जा रही exact school book जोड़ें।";
        } else {
            message = "अभी जुड़ी book: " + existingBookName + "\n\nनई book confirm होने तक मौजूदा book सुरक्षित रहेगी।";
        }
        String[] options = new String[]{"Scan / Search Book Online", "Add Book Manually"};
        new MaterialAlertDialogBuilder(this).setTitle(existingBookName.isEmpty() ? "Add Exact School Book" : "Change Exact School Book").setMessage(message).setItems(options, (dialog, selectedOption) -> {
            if (selectedOption == 0) {
                launchOnlineBookScan(schoolSubject);
            } else {
                launchManualBookEntry(schoolSubject);
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void launchOnlineBookScan(@NonNull SchoolSubjectEntity schoolSubject) {
        Intent intent = new Intent(this, BookCoverScanActivity.class);
        intent.putExtra(EXTRA_TARGET_SUBJECT_ROW_ID, schoolSubject.getSubjectRowId());
        intent.putExtra(EXTRA_TARGET_PROFILE_ID, schoolSubject.getProfileId());
        intent.putExtra(EXTRA_TARGET_SUBJECT_NAME, getSubjectDisplayName(schoolSubject));
        bookSetupLauncher.launch(intent);
    }

    private void launchManualBookEntry(@NonNull SchoolSubjectEntity schoolSubject) {
        Intent intent = ManualSchoolBookActivity.createIntent(this, schoolSubject.getSubjectRowId());
        bookSetupLauncher.launch(intent);
    }

    @NonNull
    private SchoolCurriculumSetupValidator.ValidationResult createValidationResult() {
        return SchoolCurriculumSetupValidator.validate(binding.inputSchoolName.getText(), binding.inputSchoolCode.getText(), binding.inputEducationBoard.getText(), binding.inputClassNumber.getText(), binding.inputSection.getText(), binding.inputAcademicSession.getText(), binding.inputStudyMedium.getText(), subjectAdapter.getCurrentSubjects());
    }

    private void refreshValidationState(boolean showErrors) {
        if (binding == null || subjectAdapter == null) {
            return;
        }
        SchoolCurriculumSetupValidator.ValidationResult validationResult = createValidationResult();
        if (showErrors) {
            applyValidationErrors(validationResult);
        } else {
            clearValidationErrors();
        }
        updateSaveButtonState(validationResult);
    }

    private void updateSaveButtonState(@NonNull SchoolCurriculumSetupValidator.ValidationResult validationResult) {
        boolean saveButtonEnabled = !operationInProgress && validationResult.canSaveSchoolSetup();
        binding.buttonSaveSchoolCurriculum.setEnabled(saveButtonEnabled);
        if (!validationResult.isSchoolDetailsValid()) {
            binding.buttonSaveSchoolCurriculum.setText("Complete School Details");
            return;
        }
        if (validationResult.getEnabledSubjectCount() <= 0) {
            binding.buttonSaveSchoolCurriculum.setText("Add School Subjects");
            return;
        }
        if (validationResult.getPendingBookCount() > 0) {
            binding.buttonSaveSchoolCurriculum.setText("Save Setup • " + validationResult.getPendingBookCount() + " Books Pending");
            return;
        }
        binding.buttonSaveSchoolCurriculum.setText("Save & Activate Curriculum");
    }

    private void applyValidationErrors(@NonNull SchoolCurriculumSetupValidator.ValidationResult validationResult) {
        binding.layoutSchoolName.setError(emptyToNull(validationResult.getSchoolNameError()));
        binding.layoutEducationBoard.setError(emptyToNull(validationResult.getEducationBoardError()));
        binding.layoutClassNumber.setError(emptyToNull(validationResult.getClassNumberError()));
        binding.layoutAcademicSession.setError(emptyToNull(validationResult.getAcademicSessionError()));
        binding.layoutStudyMedium.setError(emptyToNull(validationResult.getStudyMediumError()));
    }

    private void clearValidationErrors() {
        binding.layoutSchoolName.setError(null);
        binding.layoutEducationBoard.setError(null);
        binding.layoutClassNumber.setError(null);
        binding.layoutAcademicSession.setError(null);
        binding.layoutStudyMedium.setError(null);
    }

    private void focusFirstInvalidField(@NonNull SchoolCurriculumSetupValidator.ValidationResult validationResult) {
        if (!validationResult.getSchoolNameError().isEmpty()) {
            requestInputFocus(binding.inputSchoolName);
            return;
        }
        if (!validationResult.getEducationBoardError().isEmpty()) {
            requestInputFocus(binding.inputEducationBoard);
            return;
        }
        if (!validationResult.getClassNumberError().isEmpty()) {
            requestInputFocus(binding.inputClassNumber);
            return;
        }
        if (!validationResult.getAcademicSessionError().isEmpty()) {
            requestInputFocus(binding.inputAcademicSession);
            return;
        }
        if (!validationResult.getStudyMediumError().isEmpty()) {
            requestInputFocus(binding.inputStudyMedium);
            return;
        }
        if (validationResult.getEnabledSubjectCount() <= 0) {
            binding.buttonAddSchoolSubject.requestFocus();
        }
    }

    private void requestInputFocus(@NonNull EditText input) {
        input.requestFocus();
        binding.scrollSchoolCurriculumSetup.smoothScrollTo(0, Math.max(0, input.getTop() - 80));
    }

    @NonNull
    private String createValidationErrorMessage(@NonNull SchoolCurriculumSetupValidator.ValidationResult validationResult) {
        if (!validationResult.getSchoolNameError().isEmpty()) {
            return validationResult.getSchoolNameError();
        }
        if (!validationResult.getEducationBoardError().isEmpty()) {
            return validationResult.getEducationBoardError();
        }
        if (!validationResult.getClassNumberError().isEmpty()) {
            return validationResult.getClassNumberError();
        }
        if (!validationResult.getAcademicSessionError().isEmpty()) {
            return validationResult.getAcademicSessionError();
        }
        if (!validationResult.getStudyMediumError().isEmpty()) {
            return validationResult.getStudyMediumError();
        }
        if (validationResult.hasGeneralErrors()) {
            return validationResult.getGeneralErrors().get(0);
        }
        return validationResult.getSetupStatusMessage();
    }

    @Nullable
    private String emptyToNull(@Nullable String value) {
        String safeValue = safeText(value);
        return safeValue.isEmpty() ? null : safeValue;
    }

    private boolean isDuplicateSubject(@NonNull String englishName, @NonNull String hindiName, @NonNull String subjectCode) {
        return isDuplicateSubjectForEdit(0L, englishName, hindiName, subjectCode);
    }

    private boolean isDuplicateSubjectForEdit(long ignoredSubjectRowId, @NonNull String englishName, @NonNull String hindiName, @NonNull String subjectCode) {
        String normalizedEnglishName = normalizeText(englishName);
        String normalizedHindiName = normalizeText(hindiName);
        String normalizedCode = normalizeText(subjectCode);
        for (SchoolSubjectEntity existingSubject : subjectAdapter.getCurrentSubjects()) {
            if (ignoredSubjectRowId > 0L && existingSubject.getSubjectRowId() == ignoredSubjectRowId) {
                continue;
            }
            if (!normalizedEnglishName.isEmpty() && normalizedEnglishName.equals(normalizeText(existingSubject.getSubjectNameEnglish()))) {
                return true;
            }
            if (!normalizedHindiName.isEmpty() && normalizedHindiName.equals(normalizeText(existingSubject.getSubjectNameHindi()))) {
                return true;
            }
            if (!normalizedCode.isEmpty() && normalizedCode.equals(normalizeText(existingSubject.getSubjectCode()))) {
                return true;
            }
        }
        return false;
    }

    private void clearSubjectDialogErrors(@NonNull DialogAddSchoolSubjectBinding dialogBinding) {
        dialogBinding.layoutSubjectNameEnglish.setError(null);
        dialogBinding.layoutSubjectNameHindi.setError(null);
        dialogBinding.layoutSubjectCode.setError(null);
        dialogBinding.textSubjectDialogError.setText("");
        dialogBinding.textSubjectDialogError.setVisibility(View.GONE);
    }

    @NonNull
    private String convertDatabaseCategoryToLabel(@Nullable String databaseCategory) {
        String normalizedCategory = safeText(databaseCategory).toUpperCase(Locale.ROOT);
        switch (normalizedCategory) {
            case "LANGUAGE":
                return "Language";
            case "SKILL_BASED":
                return "Skill Based";
            case "ACTIVITY_BASED":
                return "Activity Based";
            case "SCHOOL_SPECIFIC":
                return "School Specific";
            case "CORE_ACADEMIC":
            default:
                return "Core Academic";
        }
    }

    @NonNull
    private String convertCategoryToDatabaseValue(@Nullable String categoryLabel) {
        String normalizedCategory = normalizeText(categoryLabel);
        switch (normalizedCategory) {
            case "language":
                return "LANGUAGE";
            case "skill based":
                return "SKILL_BASED";
            case "activity based":
                return "ACTIVITY_BASED";
            case "school specific":
                return "SCHOOL_SPECIFIC";
            case "core academic":
            default:
                return "CORE_ACADEMIC";
        }
    }

    @NonNull
    private String createSubjectCode(@NonNull String subjectName) {
        String normalizedName = subjectName.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (normalizedName.isEmpty()) {
            return "SUB";
        }
        return normalizedName.substring(0, Math.min(6, normalizedName.length()));
    }

    private void updateSubjectListState() {
        boolean hasSubjects = subjectAdapter.hasSubjects();
        binding.recyclerCurriculumSubjects.setVisibility(hasSubjects ? View.VISIBLE : View.GONE);
        binding.containerEmptyCurriculumSubjects.setVisibility(hasSubjects ? View.GONE : View.VISIBLE);
        int selectedSubjectCount = subjectAdapter.getSelectedSubjectCount();
        binding.textSelectedSubjectCount.setText(selectedSubjectCount + (selectedSubjectCount == 1 ? " Subject" : " Subjects"));
        refreshValidationState(saveAttempted);
    }

    @Override
    public void onSubjectSelected(@NonNull SchoolSubjectEntity schoolSubject) {
        showBookAddMethodDialog(schoolSubject);
    }

    @Override
    public void onScanBookClicked(@NonNull SchoolSubjectEntity schoolSubject) {
        showBookAddMethodDialog(schoolSubject);
    }

    @Override
    public void onEditSubjectClicked(@NonNull SchoolSubjectEntity schoolSubject) {
        showEditSubjectDialog(schoolSubject);
    }

    @Override
    public void onRemoveSubjectClicked(@NonNull SchoolSubjectEntity schoolSubject) {
        showSubjectRemovalOptions(schoolSubject);
    }

    private void showSubjectRemovalOptions(@NonNull SchoolSubjectEntity schoolSubject) {
        if (operationInProgress) {
            return;
        }
        if (schoolSubject.getSubjectRowId() <= 0L) {
            showError("Valid school subject उपलब्ध नहीं है।");
            return;
        }
        String subjectName = getSubjectDisplayName(schoolSubject);
        SchoolSubjectRemovalRepository.RemovalImpact removalImpact = SchoolSubjectRemovalRepository.inspectRemovalImpact(schoolSubject);
        String protectedDataSummary = removalImpact.createProtectedDataSummary();
        if (!schoolSubject.isEnabled()) {
            new MaterialAlertDialogBuilder(this).setTitle(subjectName + " पहले से hidden है").setMessage("यह subject अभी Child Mode में दिखाई नहीं देता। Subject और उससे जुड़ा data database में सुरक्षित है।\n\nसुरक्षित data:\n" + protectedDataSummary + "\n\nइसे दोबारा दिखाने के लिए Edit में जाकर ‘बच्चे को यह subject दिखाएँ’ switch चालू करें।").setNegativeButton("Cancel", null).setPositiveButton("Permanently Delete", (dialog, which) -> showPermanentDeleteConfirmation(schoolSubject, removalImpact)).show();
            return;
        }
        String[] removalOptions = new String[]{"Hide from Child Mode", "Permanently Delete"};
        new MaterialAlertDialogBuilder(this).setTitle(subjectName + " हटाएँ या छिपाएँ?").setMessage("Hide करने पर subject, exact school book और content सुरक्षित रहेगा। Permanent Delete करने पर subject और linked data वापस नहीं आएगा.\n\nअभी जुड़ा data:\n" + protectedDataSummary).setItems(removalOptions, (dialog, selectedOption) -> {
            if (selectedOption == 0) {
                showHideSubjectConfirmation(schoolSubject);
            } else {
                showPermanentDeleteConfirmation(schoolSubject, removalImpact);
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void showHideSubjectConfirmation(@NonNull SchoolSubjectEntity schoolSubject) {
        String subjectName = getSubjectDisplayName(schoolSubject);
        new MaterialAlertDialogBuilder(this).setTitle("Child Mode से hide करें?").setMessage(subjectName + " बच्चे की My Subjects screen से छिप जाएगा।\n\nExact school book, chapters, lessons, quiz data और parent settings सुरक्षित रहेंगी।\n\nबाद में Edit Subject में switch चालू करके इसे दोबारा दिखाया जा सकेगा।").setNegativeButton("Cancel", null).setPositiveButton("Hide Subject", (dialog, which) -> hideSubjectFromChildMode(schoolSubject)).show();
    }

    private void hideSubjectFromChildMode(@NonNull SchoolSubjectEntity schoolSubject) {
        if (operationInProgress) {
            return;
        }
        String subjectName = getSubjectDisplayName(schoolSubject);
        setOperationState(true, subjectName + " Child Mode से hide किया जा रहा है");
        schoolSubjectRemovalRepository.hideSubjectFromChildMode(schoolSubject.getSubjectRowId(), new SchoolSubjectRemovalRepository.RemovalCallback() {
            @Override
            public void onSuccess(@NonNull SchoolSubjectRemovalRepository.RemovalResult result) {
                if (!isActivityAvailable()) {
                    return;
                }
                schoolSubject.setEnabled(false);
                schoolSubject.setUpdatedAt(System.currentTimeMillis());
                subjectAdapter.updateSubject(schoolSubject);
                updateSubjectListState();
                setOperationState(false, "");
                setResult(RESULT_OK);
                Snackbar.make(binding.getRoot(), subjectName + " Child Mode से hide हो गया। Exact school book सुरक्षित है।", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setOperationState(false, "");
                showError("Subject hide नहीं हो सका। दोबारा प्रयास करें।");
            }
        });
    }

    private void showPermanentDeleteConfirmation(@NonNull SchoolSubjectEntity schoolSubject, @NonNull SchoolSubjectRemovalRepository.RemovalImpact removalImpact) {
        String subjectName = getSubjectDisplayName(schoolSubject);
        String warningMessage;
        if (removalImpact.hasProtectedData()) {
            warningMessage = "यह action वापस नहीं होगा। " + subjectName + " के साथ निम्न data permanently delete हो सकता है:\n\n" + removalImpact.createProtectedDataSummary() + "\n\nकेवल तभी Delete करें जब यह subject बच्चे के वर्तमान school curriculum में बिल्कुल नहीं है।";
        } else {
            warningMessage = "यह action वापस नहीं होगा। " + subjectName + " curriculum और database से permanently हट जाएगा.\n\nकेवल तभी Delete करें जब यह subject बच्चे के वर्तमान school curriculum में नहीं है।";
        }
        new MaterialAlertDialogBuilder(this).setTitle("Permanently Delete Subject?").setMessage(warningMessage).setNegativeButton("Cancel", null).setPositiveButton("Delete Permanently", (dialog, which) -> deleteSubjectPermanently(schoolSubject)).show();
    }

    private void deleteSubjectPermanently(@NonNull SchoolSubjectEntity schoolSubject) {
        if (operationInProgress) {
            return;
        }
        long subjectRowId = schoolSubject.getSubjectRowId();
        String subjectName = getSubjectDisplayName(schoolSubject);
        setOperationState(true, subjectName + " permanently delete किया जा रहा है");
        schoolSubjectRemovalRepository.deleteSubjectPermanently(schoolSubject, new SchoolSubjectRemovalRepository.RemovalCallback() {
            @Override
            public void onSuccess(@NonNull SchoolSubjectRemovalRepository.RemovalResult result) {
                if (!isActivityAvailable()) {
                    return;
                }
                subjectAdapter.removeSubject(subjectRowId);
                updateSubjectListState();
                setOperationState(false, "");
                setResult(RESULT_OK);
                Snackbar.make(binding.getRoot(), subjectName + " permanently delete हो गया।", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isActivityAvailable()) {
                    return;
                }
                setOperationState(false, "");
                showError("Subject permanently delete नहीं हो सका।");
            }
        });
    }

    private void showImportSubjectMessage() {
        StudentProfileEntity studentProfile =
                activeStudentProfile;

        if (studentProfile == null
                || operationInProgress) {
            showError(
                    "Active student profile तैयार होने के बाद दोबारा कोशिश करें।"
            );
            return;
        }

        setOperationState(
                true,
                studentProfile.getStudentClass()
                        + " के suggested subjects जोड़े जा रहे हैं"
        );

        SchoolCurriculumBootstrapper bootstrapper =
                new SchoolCurriculumBootstrapper(
                        this
                );

        bootstrapper.ensureCurriculumReady(
                studentProfile,
                new SchoolCurriculumBootstrapper.BootstrapCallback() {
                    @Override
                    public void onReady(
                            @NonNull SchoolCurriculumBootstrapper
                                    .BootstrapResult result
                    ) {
                        bootstrapper.close();

                        if (!isActivityAvailable()) {
                            return;
                        }

                        curriculumProfile =
                                result.getCurriculumProfile();

                        subjectAdapter.submitList(
                                result.getAvailableSubjects()
                        );

                        updateSubjectListState();
                        setOperationState(
                                false,
                                ""
                        );
                        setResult(
                                RESULT_OK
                        );

                        String message =
                                result.getInsertedSubjectCount() > 0
                                        ? result.getInsertedSubjectCount()
                                        + " suggested subjects जोड़ दिए गए। अब actual school books जोड़ें।"
                                        : "इस class के suggested subjects पहले से मौजूद हैं।";

                        Snackbar.make(
                                binding.getRoot(),
                                message,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onError(
                            @NonNull SchoolCurriculumBootstrapper
                                    .BootstrapException exception
                    ) {
                        bootstrapper.close();

                        if (!isActivityAvailable()) {
                            return;
                        }

                        setOperationState(
                                false,
                                ""
                        );
                        showError(
                                "Suggested subjects नहीं जुड़ सके। Subject manually जोड़ें।"
                        );
                    }
                }
        );
    }

    private void showMissingStudentProfileError() {
        new MaterialAlertDialogBuilder(this).setTitle("Active student उपलब्ध नहीं है").setMessage("School curriculum setup से पहले student profile बनाकर active करें।").setCancelable(false).setPositiveButton("वापस जाएँ", (dialog, which) -> finish()).show();
    }

    private void showError(@NonNull String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void setOperationState(boolean inProgress, @Nullable String progressMessage) {
        operationInProgress = inProgress;
        binding.containerCurriculumSetupLoading.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        binding.buttonAddSchoolSubject.setEnabled(!inProgress);
        binding.buttonImportSubjectList.setEnabled(!inProgress);
        binding.buttonCancelCurriculumSetup.setEnabled(!inProgress);
        String safeProgressMessage = safeText(progressMessage);
        if (!safeProgressMessage.isEmpty()) {
            binding.textCurriculumSetupProgress.setText(safeProgressMessage);
        }
        refreshValidationState(saveAttempted);
    }

    @NonNull
    private String getSubjectDisplayName(@NonNull SchoolSubjectEntity schoolSubject) {
        String englishName = safeText(schoolSubject.getSubjectNameEnglish());
        if (!englishName.isEmpty()) {
            return englishName;
        }
        String hindiName = safeText(schoolSubject.getSubjectNameHindi());
        return hindiName.isEmpty() ? "School Subject" : hindiName;
    }

    @NonNull
    private String createSelectedSchoolDetails(@NonNull SchoolDirectoryEntity school) {
        StringBuilder details = new StringBuilder();
        if (selectedDistrict != null) {
            appendSummaryPart(details, selectedDistrict.getDistrictName());
        }
        appendSummaryPart(details, formatBoardName(school.getEducationBoard()));
        appendSummaryPart(details, school.getPreferredSchoolCode());
        appendSummaryPart(details, school.getAddressLine());
        return details.length() == 0 ? "School directory record" : details.toString();
    }

    @NonNull
    private String normalizeBoard(@Nullable Object value) {
        String board = safeText(value).toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        if (board.equals("CISCE_/_ICSE_/_ISC") || board.equals("ICSE") || board.equals("ISC")) {
            return "CISCE";
        }
        if (board.equals("UP_BOARD") || board.equals("UPMSP")) {
            return "UPMSP";
        }
        if (board.equals("STATE_BOARD")) {
            return "STATE_BOARD";
        }
        return board;
    }

    @NonNull
    private String formatBoardName(@Nullable Object value) {
        String board = normalizeBoard(value);
        switch (board) {
            case "CBSE":
                return "CBSE";
            case "CISCE":
                return "CISCE / ICSE / ISC";
            case "UPMSP":
                return "UP Board";
            case "STATE_BOARD":
                return "State Board";
            case "IB":
                return "IB";
            case "CAMBRIDGE":
                return "Cambridge";
            case "NIOS":
                return "NIOS";
            case "OTHER":
                return "Other";
            default:
                return board.replace("_", " ");
        }
    }

    private boolean isActivityAvailable() {
        return !isFinishing() && !isDestroyed() && binding != null;
    }

    private void appendSummaryPart(@NonNull StringBuilder builder, @Nullable Object value) {
        String safeValue = safeText(value);
        if (safeValue.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("  •  ");
        }
        builder.append(safeValue);
    }

    @NonNull
    private String valueOrFallback(@Nullable Object value, @NonNull String fallback) {
        String safeValue = safeText(value);
        return safeValue.isEmpty() ? fallback : safeValue;
    }

    @NonNull
    private String normalizeText(@Nullable Object value) {
        return safeText(value).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").replaceAll("\\s+", " ").trim();
    }

    @NonNull
    private String safeText(@Nullable Object value) {
        return value == null ? "" : value.toString().trim().replaceAll("\\s+", " ");
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
