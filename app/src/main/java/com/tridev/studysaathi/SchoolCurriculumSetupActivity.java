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
import com.tridev.studysaathi.data.content.validation.SchoolCurriculumSetupValidator;
import com.tridev.studysaathi.data.local.entity.SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.SchoolCurriculumProfileRepository;
import com.tridev.studysaathi.data.repository.SchoolSubjectRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.data.schooldirectory.adapter.SchoolDirectorySchoolAdapter;
import com.tridev.studysaathi.data.schooldirectory.entity.DistrictDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.SchoolDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity.StateDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.repository.SchoolDirectoryRepository;
import com.tridev.studysaathi.databinding.ActivitySchoolCurriculumSetupBinding;
import com.tridev.studysaathi.databinding.DialogAddSchoolSubjectBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SchoolCurriculumSetupActivity
        extends AppCompatActivity
        implements SchoolCurriculumSubjectAdapter.SubjectActionListener {

    public static final String EXTRA_TARGET_SUBJECT_ROW_ID =
            "extra_target_subject_row_id";

    public static final String EXTRA_TARGET_PROFILE_ID =
            "extra_target_profile_id";

    public static final String EXTRA_TARGET_SUBJECT_NAME =
            "extra_target_subject_name";

    private static final String DEFAULT_SCHOOL_NAME =
            "School details pending";

    private static final String CONTENT_SOURCE_SCHOOL_BOOK =
            "SCHOOL_BOOK";

    private static final int SCHOOL_SEARCH_LIMIT =
            100;

    private ActivitySchoolCurriculumSetupBinding binding;

    private StudentProfileRepository studentProfileRepository;

    private SchoolCurriculumProfileRepository
            curriculumProfileRepository;

    private SchoolSubjectRepository schoolSubjectRepository;

    private SchoolDirectoryRepository schoolDirectoryRepository;

    private SchoolCurriculumSubjectAdapter subjectAdapter;

    private SchoolDirectorySchoolAdapter schoolAdapter;

    private ActivityResultLauncher<Intent>
            bookSetupLauncher;

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
    private String selectedEducationBoard =
            "";

    @NonNull
    private List<StateDirectoryEntity> availableStates =
            new ArrayList<>();

    @NonNull
    private List<DistrictDirectoryEntity> availableDistricts =
            new ArrayList<>();

    private boolean operationInProgress;

    private boolean directoryOperationInProgress;

    private boolean formBindingInProgress;

    private boolean directoryBindingInProgress;

    private boolean saveAttempted;

    private boolean manualSchoolEntryMode;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivitySchoolCurriculumSetupBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        studentProfileRepository =
                new StudentProfileRepository(
                        this
                );

        curriculumProfileRepository =
                new SchoolCurriculumProfileRepository(
                        this
                );

        schoolSubjectRepository =
                new SchoolSubjectRepository(
                        this
                );

        schoolDirectoryRepository =
                new SchoolDirectoryRepository(
                        this
                );

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
        bookSetupLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .StartActivityForResult(),
                        result -> {
                            if (!isActivityAvailable()) {
                                return;
                            }

                            if (result.getResultCode()
                                    != RESULT_OK) {
                                return;
                            }

                            Intent resultData =
                                    result.getData();

                            String savedBookTitle =
                                    "";

                            if (resultData != null) {
                                savedBookTitle =
                                        safeText(
                                                resultData.getStringExtra(
                                                        ManualSchoolBookActivity
                                                                .RESULT_BOOK_TITLE
                                                )
                                        );
                            }

                            SchoolCurriculumProfileEntity profile =
                                    curriculumProfile;

                            if (profile != null
                                    && profile.getProfileId() > 0L) {

                                loadSchoolSubjects(
                                        profile.getProfileId()
                                );
                            }

                            String message =
                                    savedBookTitle.isEmpty()
                                            ? "Exact school book save हो गई है।"
                                            : savedBookTitle
                                              + " save हो गई है।";

                            Snackbar.make(
                                    binding.getRoot(),
                                    message,
                                    Snackbar.LENGTH_LONG
                            ).show();

                            setResult(
                                    RESULT_OK
                            );
                        }
                );
    }

    private void setupRecyclerView() {
        subjectAdapter =
                new SchoolCurriculumSubjectAdapter(
                        this
                );

        binding.recyclerCurriculumSubjects
                .setLayoutManager(
                        new LinearLayoutManager(
                                this
                        )
                );

        binding.recyclerCurriculumSubjects
                .setAdapter(
                        subjectAdapter
                );

        binding.recyclerCurriculumSubjects
                .setHasFixedSize(
                        false
                );

        updateSubjectListState();
    }

    private void setupSchoolDirectoryAdapters() {
        schoolAdapter =
                new SchoolDirectorySchoolAdapter(
                        this
                );

        binding.inputDirectorySchoolSearch
                .setAdapter(
                        schoolAdapter
                );

        binding.inputDirectorySchoolSearch
                .setThreshold(
                        0
                );
    }

    private void setupToolbar() {
        binding.toolbarSchoolCurriculumSetup
                .setNavigationOnClickListener(
                        view ->
                                getOnBackPressedDispatcher()
                                        .onBackPressed()
                );
    }

    private void setupClickListeners() {
        binding.buttonAddSchoolSubject
                .setOnClickListener(
                        view ->
                                showAddSubjectDialog()
                );

        binding.buttonImportSubjectList
                .setOnClickListener(
                        view ->
                                showImportSubjectMessage()
                );

        binding.buttonSaveSchoolCurriculum
                .setOnClickListener(
                        view ->
                                saveSchoolCurriculum()
                );

        binding.buttonCancelCurriculumSetup
                .setOnClickListener(
                        view ->
                                finish()
                );

        binding.buttonEnterSchoolManually
                .setOnClickListener(
                        view ->
                                enableManualSchoolEntryMode()
                );

        binding.buttonChangeSelectedSchool
                .setOnClickListener(
                        view ->
                                changeSelectedSchool()
                );
    }

    private void setupFormValidation() {
        addValidationWatcher(
                binding.inputSchoolName
        );

        addValidationWatcher(
                binding.inputSchoolCode
        );

        addValidationWatcher(
                binding.inputEducationBoard
        );

        addValidationWatcher(
                binding.inputClassNumber
        );

        addValidationWatcher(
                binding.inputSection
        );

        addValidationWatcher(
                binding.inputAcademicSession
        );

        addValidationWatcher(
                binding.inputStudyMedium
        );

        refreshValidationState(
                false
        );
    }

    private void addValidationWatcher(
            @NonNull EditText input
    ) {
        input.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after
                    ) {
                        /*
                         * No action required.
                         */
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count
                    ) {
                        /*
                         * Validation afterTextChanged में होगी।
                         */
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        if (formBindingInProgress) {
                            return;
                        }

                        refreshValidationState(
                                saveAttempted
                        );
                    }
                }
        );
    }

    private void setupSchoolDirectoryListeners() {
        binding.inputDirectoryState
                .setOnItemClickListener(
                        (parent, view, position, id) -> {
                            if (directoryBindingInProgress) {
                                return;
                            }

                            if (position < 0
                                    || position
                                    >= availableStates.size()) {
                                return;
                            }

                            StateDirectoryEntity state =
                                    availableStates.get(
                                            position
                                    );

                            selectState(
                                    state
                            );
                        }
                );

        binding.inputDirectoryDistrict
                .setOnItemClickListener(
                        (parent, view, position, id) -> {
                            if (directoryBindingInProgress) {
                                return;
                            }

                            if (position < 0
                                    || position
                                    >= availableDistricts.size()) {
                                return;
                            }

                            DistrictDirectoryEntity district =
                                    availableDistricts.get(
                                            position
                                    );

                            selectDistrict(
                                    district
                            );
                        }
                );

        binding.inputDirectoryEducationBoard
                .setOnItemClickListener(
                        (parent, view, position, id) -> {
                            if (directoryBindingInProgress) {
                                return;
                            }

                            String board =
                                    safeText(
                                            parent.getItemAtPosition(
                                                    position
                                            )
                                    );

                            selectEducationBoard(
                                    board
                            );
                        }
                );

        binding.inputDirectorySchoolSearch
                .setOnItemClickListener(
                        (parent, view, position, id) -> {
                            SchoolDirectoryEntity school =
                                    schoolAdapter.getSchoolAt(
                                            position
                                    );

                            if (school != null) {
                                selectDirectorySchool(
                                        school
                                );
                            }
                        }
                );

        binding.inputDirectorySchoolSearch
                .setOnClickListener(
                        view -> {
                            if (binding.inputDirectorySchoolSearch
                                    .isEnabled()
                                    && schoolAdapter.hasSchools()) {

                                binding.inputDirectorySchoolSearch
                                        .showDropDown();
                            }
                        }
                );

        binding.inputDirectorySchoolSearch
                .addTextChangedListener(
                        new TextWatcher() {

                            @Override
                            public void beforeTextChanged(
                                    CharSequence text,
                                    int start,
                                    int count,
                                    int after
                            ) {
                                /*
                                 * No action required.
                                 */
                            }

                            @Override
                            public void onTextChanged(
                                    CharSequence text,
                                    int start,
                                    int before,
                                    int count
                            ) {
                                /*
                                 * Search afterTextChanged में होगी।
                                 */
                            }

                            @Override
                            public void afterTextChanged(
                                    Editable editable
                            ) {
                                if (directoryBindingInProgress
                                        || selectedDistrict == null
                                        || selectedEducationBoard
                                        .isEmpty()) {

                                    return;
                                }

                                String searchText =
                                        safeText(
                                                editable
                                        );

                                searchSchools(
                                        searchText
                                );
                            }
                        }
                );
    }

    private void initializeSchoolDirectory() {
        setDirectoryLoadingState(
                true,
                "School directory तैयार की जा रही है"
        );

        schoolDirectoryRepository
                .ensureStarterDirectory(
                        new SchoolDirectoryRepository
                                .StarterDirectoryCallback() {

                            @Override
                            public void onReady(
                                    @NonNull SchoolDirectoryRepository
                                            .StarterDirectoryResult result
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                loadStates();
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                showDirectoryError(
                                        "School directory तैयार नहीं हो सकी। Manual Entry का उपयोग करें।"
                                );

                                enableManualSchoolEntryMode();
                            }
                        }
                );
    }

    private void loadStates() {
        setDirectoryLoadingState(
                true,
                "States load किए जा रहे हैं"
        );

        schoolDirectoryRepository
                .getActiveStates(
                        new SchoolDirectoryRepository
                                .StatesCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull List<StateDirectoryEntity>
                                            states
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                availableStates =
                                        new ArrayList<>(
                                                states
                                        );

                                List<String> stateNames =
                                        new ArrayList<>();

                                for (StateDirectoryEntity state :
                                        states) {

                                    stateNames.add(
                                            state.getStateName()
                                    );
                                }

                                ArrayAdapter<String> adapter =
                                        new ArrayAdapter<>(
                                                SchoolCurriculumSetupActivity
                                                        .this,
                                                android.R.layout
                                                        .simple_list_item_1,
                                                stateNames
                                        );

                                binding.inputDirectoryState
                                        .setAdapter(
                                                adapter
                                        );

                                binding.layoutDirectoryState
                                        .setEnabled(
                                                !states.isEmpty()
                                        );

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                if (states.isEmpty()) {
                                    showDirectoryError(
                                            "State directory उपलब्ध नहीं है।"
                                    );

                                    enableManualSchoolEntryMode();

                                    return;
                                }

                                selectPreferredDefaultState(
                                        states
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                showDirectoryError(
                                        "State list load नहीं हो सकी।"
                                );

                                enableManualSchoolEntryMode();
                            }
                        }
                );
    }

    private void selectPreferredDefaultState(
            @NonNull List<StateDirectoryEntity> states
    ) {
        for (StateDirectoryEntity state :
                states) {

            if ("UP".equalsIgnoreCase(
                    state.getStateCode()
            )) {
                directoryBindingInProgress =
                        true;

                binding.inputDirectoryState
                        .setText(
                                state.getStateName(),
                                false
                        );

                directoryBindingInProgress =
                        false;

                selectState(
                        state
                );

                return;
            }
        }
    }

    private void selectState(
            @NonNull StateDirectoryEntity state
    ) {
        selectedState =
                state;

        selectedDistrict =
                null;

        selectedDirectorySchool =
                null;

        selectedEducationBoard =
                "";

        availableDistricts =
                new ArrayList<>();

        directoryBindingInProgress =
                true;

        binding.inputDirectoryState
                .setText(
                        state.getStateName(),
                        false
                );

        binding.inputDirectoryDistrict
                .setText(
                        "",
                        false
                );

        binding.inputDirectoryEducationBoard
                .setText(
                        "",
                        false
                );

        binding.inputDirectorySchoolSearch
                .setText(
                        "",
                        false
                );

        directoryBindingInProgress =
                false;

        binding.layoutDirectoryDistrict
                .setEnabled(
                        false
                );

        binding.layoutDirectoryEducationBoard
                .setEnabled(
                        false
                );

        binding.layoutDirectorySchoolSearch
                .setEnabled(
                        false
                );

        schoolAdapter.clearSchools();

        hideSelectedSchoolCard();
        hideDirectoryNoResult();

        loadDistricts(
                state.getStateCode()
        );
    }

    private void loadDistricts(
            @NonNull String stateCode
    ) {
        setDirectoryLoadingState(
                true,
                "Districts load किए जा रहे हैं"
        );

        schoolDirectoryRepository
                .getDistrictsForState(
                        stateCode,
                        new SchoolDirectoryRepository
                                .DistrictsCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull List<DistrictDirectoryEntity>
                                            districts
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                availableDistricts =
                                        new ArrayList<>(
                                                districts
                                        );

                                List<String> districtNames =
                                        new ArrayList<>();

                                for (DistrictDirectoryEntity district :
                                        districts) {

                                    districtNames.add(
                                            district.getDistrictName()
                                    );
                                }

                                ArrayAdapter<String> adapter =
                                        new ArrayAdapter<>(
                                                SchoolCurriculumSetupActivity
                                                        .this,
                                                android.R.layout
                                                        .simple_list_item_1,
                                                districtNames
                                        );

                                binding.inputDirectoryDistrict
                                        .setAdapter(
                                                adapter
                                        );

                                binding.layoutDirectoryDistrict
                                        .setEnabled(
                                                !districts.isEmpty()
                                        );

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                if (districts.isEmpty()) {
                                    showDirectoryNoResult(
                                            "इस State के districts अभी directory में उपलब्ध नहीं हैं। Manual Entry का उपयोग करें।"
                                    );
                                }
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                showDirectoryNoResult(
                                        "District list load नहीं हो सकी। Manual Entry का उपयोग करें।"
                                );
                            }
                        }
                );
    }

    private void selectDistrict(
            @NonNull DistrictDirectoryEntity district
    ) {
        selectedDistrict =
                district;

        selectedDirectorySchool =
                null;

        selectedEducationBoard =
                "";

        directoryBindingInProgress =
                true;

        binding.inputDirectoryDistrict
                .setText(
                        district.getDistrictName(),
                        false
                );

        binding.inputDirectoryEducationBoard
                .setText(
                        "",
                        false
                );

        binding.inputDirectorySchoolSearch
                .setText(
                        "",
                        false
                );

        directoryBindingInProgress =
                false;

        binding.layoutDirectoryEducationBoard
                .setEnabled(
                        false
                );

        binding.layoutDirectorySchoolSearch
                .setEnabled(
                        false
                );

        schoolAdapter.clearSchools();

        hideSelectedSchoolCard();
        hideDirectoryNoResult();

        loadEducationBoards(
                district.getDistrictCode()
        );
    }

    private void loadEducationBoards(
            @NonNull String districtCode
    ) {
        setDirectoryLoadingState(
                true,
                "Education Boards load किए जा रहे हैं"
        );

        schoolDirectoryRepository
                .getEducationBoardsForDistrict(
                        districtCode,
                        new SchoolDirectoryRepository
                                .BoardsCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull List<String>
                                            educationBoards
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                List<String> boardLabels =
                                        new ArrayList<>();

                                for (String board :
                                        educationBoards) {

                                    boardLabels.add(
                                            formatBoardName(
                                                    board
                                            )
                                    );
                                }

                                ArrayAdapter<String> adapter =
                                        new ArrayAdapter<>(
                                                SchoolCurriculumSetupActivity
                                                        .this,
                                                android.R.layout
                                                        .simple_list_item_1,
                                                boardLabels
                                        );

                                binding.inputDirectoryEducationBoard
                                        .setAdapter(
                                                adapter
                                        );

                                binding.inputDirectoryEducationBoard
                                        .setTag(
                                                new ArrayList<>(
                                                        educationBoards
                                                )
                                        );

                                binding.layoutDirectoryEducationBoard
                                        .setEnabled(
                                                !educationBoards.isEmpty()
                                        );

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                if (educationBoards.isEmpty()) {
                                    showDirectoryNoResult(
                                            "Education Board list उपलब्ध नहीं है। Manual Entry का उपयोग करें।"
                                    );
                                }
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                showDirectoryNoResult(
                                        "Education Board list load नहीं हो सकी।"
                                );
                            }
                        }
                );
    }

    private void selectEducationBoard(
            @NonNull String displayedBoard
    ) {
        String normalizedBoard =
                normalizeBoard(
                        displayedBoard
                );

        selectedEducationBoard =
                normalizedBoard;

        selectedDirectorySchool =
                null;

        directoryBindingInProgress =
                true;

        binding.inputDirectoryEducationBoard
                .setText(
                        formatBoardName(
                                normalizedBoard
                        ),
                        false
                );

        binding.inputDirectorySchoolSearch
                .setText(
                        "",
                        false
                );

        directoryBindingInProgress =
                false;

        binding.layoutDirectorySchoolSearch
                .setEnabled(
                        selectedDistrict != null
                                && !normalizedBoard.isEmpty()
                );

        schoolAdapter.clearSchools();

        hideSelectedSchoolCard();
        hideDirectoryNoResult();

        loadSchoolsForSelectedFilters();
    }

    private void loadSchoolsForSelectedFilters() {
        DistrictDirectoryEntity district =
                selectedDistrict;

        if (district == null
                || selectedEducationBoard.isEmpty()) {

            return;
        }

        setDirectoryLoadingState(
                true,
                "Schools खोजे जा रहे हैं"
        );

        schoolDirectoryRepository
                .getSchools(
                        district.getDistrictCode(),
                        selectedEducationBoard,
                        new SchoolDirectoryRepository
                                .SchoolsCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull List<SchoolDirectoryEntity>
                                            schools
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                schoolAdapter.submitSchools(
                                        schools
                                );

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                if (schools.isEmpty()) {
                                    showDirectoryNoResult(
                                            "इस District और Board के verified school records अभी import नहीं हुए हैं। School details manually भरें।"
                                    );

                                } else {
                                    hideDirectoryNoResult();

                                    binding.inputDirectorySchoolSearch
                                            .showDropDown();
                                }
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                setDirectoryLoadingState(
                                        false,
                                        ""
                                );

                                showDirectoryNoResult(
                                        "School list load नहीं हो सकी। Manual Entry का उपयोग करें।"
                                );
                            }
                        }
                );
    }

    private void searchSchools(
            @NonNull String searchText
    ) {
        DistrictDirectoryEntity district =
                selectedDistrict;

        if (district == null
                || selectedEducationBoard.isEmpty()) {

            return;
        }

        schoolDirectoryRepository
                .searchSchools(
                        district.getDistrictCode(),
                        selectedEducationBoard,
                        searchText,
                        SCHOOL_SEARCH_LIMIT,
                        new SchoolDirectoryRepository
                                .SchoolsCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull List<SchoolDirectoryEntity>
                                            schools
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                schoolAdapter.submitSchools(
                                        schools
                                );

                                if (schools.isEmpty()) {
                                    showDirectoryNoResult(
                                            "कोई matching school नहीं मिला। नाम या code जाँचें अथवा Manual Entry करें।"
                                    );

                                } else {
                                    hideDirectoryNoResult();

                                    if (binding.inputDirectorySchoolSearch
                                            .hasFocus()) {

                                        binding.inputDirectorySchoolSearch
                                                .showDropDown();
                                    }
                                }
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                showDirectoryNoResult(
                                        "School search पूरी नहीं हो सकी।"
                                );
                            }
                        }
                );
    }

    private void selectDirectorySchool(
            @NonNull SchoolDirectoryEntity school
    ) {
        selectedDirectorySchool =
                school;

        manualSchoolEntryMode =
                false;

        directoryBindingInProgress =
                true;

        binding.inputDirectorySchoolSearch
                .setText(
                        school.getSchoolName(),
                        false
                );

        directoryBindingInProgress =
                false;

        formBindingInProgress =
                true;

        binding.inputSchoolName
                .setText(
                        school.getSchoolName()
                );

        binding.inputSchoolCode
                .setText(
                        school.getPreferredSchoolCode()
                );

        binding.inputEducationBoard
                .setText(
                        formatBoardName(
                                school.getEducationBoard()
                        )
                );

        formBindingInProgress =
                false;

        binding.inputSchoolName
                .setEnabled(
                        false
                );

        binding.inputSchoolCode
                .setEnabled(
                        false
                );

        binding.inputEducationBoard
                .setEnabled(
                        false
                );

        binding.textSchoolEntryMode
                .setText(
                        "School Directory Mode"
                );

        binding.textSelectedDirectorySchoolName
                .setText(
                        school.getSchoolName()
                );

        binding.textSelectedDirectorySchoolDetails
                .setText(
                        createSelectedSchoolDetails(
                                school
                        )
                );

        binding.textSelectedSchoolVerificationStatus
                .setText(
                        school.getVerificationLabel()
                );

        binding.textSelectedDirectorySchoolIcon
                .setText(
                        school.isOfficiallyVerified()
                                ? "✓"
                                : "i"
                );

        binding.cardSelectedDirectorySchool
                .setVisibility(
                        View.VISIBLE
                );

        hideDirectoryNoResult();

        refreshValidationState(
                saveAttempted
        );
    }

    private void enableManualSchoolEntryMode() {
        manualSchoolEntryMode =
                true;

        selectedDirectorySchool =
                null;

        hideSelectedSchoolCard();

        binding.inputSchoolName
                .setEnabled(
                        true
                );

        binding.inputSchoolCode
                .setEnabled(
                        true
                );

        binding.inputEducationBoard
                .setEnabled(
                        true
                );

        binding.textSchoolEntryMode
                .setText(
                        "Manual School Entry"
                );

        binding.textSchoolDetailsFormDescription
                .setText(
                        "School directory में school न मिलने पर सही School Name और Education Board manually भरें। School Code optional है।"
                );

        if (!selectedEducationBoard.isEmpty()
                && safeText(
                binding.inputEducationBoard
                        .getText()
        ).isEmpty()) {

            binding.inputEducationBoard
                    .setText(
                            formatBoardName(
                                    selectedEducationBoard
                            )
                    );
        }

        binding.inputSchoolName
                .requestFocus();

        binding.scrollSchoolCurriculumSetup
                .post(() ->
                        binding.scrollSchoolCurriculumSetup
                                .smoothScrollTo(
                                        0,
                                        binding.cardSchoolDetailsForm
                                                .getTop()
                                )
                );

        Snackbar.make(
                binding.getRoot(),
                "Manual School Entry चालू है।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void changeSelectedSchool() {
        selectedDirectorySchool =
                null;

        manualSchoolEntryMode =
                false;

        hideSelectedSchoolCard();

        formBindingInProgress =
                true;

        binding.inputSchoolName
                .setText(
                        ""
                );

        binding.inputSchoolCode
                .setText(
                        ""
                );

        binding.inputEducationBoard
                .setText(
                        selectedEducationBoard.isEmpty()
                                ? ""
                                : formatBoardName(
                                selectedEducationBoard
                        )
                );

        formBindingInProgress =
                false;

        binding.inputSchoolName
                .setEnabled(
                        true
                );

        binding.inputSchoolCode
                .setEnabled(
                        true
                );

        binding.inputEducationBoard
                .setEnabled(
                        true
                );

        binding.textSchoolEntryMode
                .setText(
                        "School Directory Mode"
                );

        binding.textSchoolDetailsFormDescription
                .setText(
                        "Directory से school चुनने पर School Name, Code और Board अपने-आप भरेंगे।"
                );

        binding.inputDirectorySchoolSearch
                .requestFocus();

        if (schoolAdapter.hasSchools()) {
            binding.inputDirectorySchoolSearch
                    .showDropDown();
        }

        refreshValidationState(
                saveAttempted
        );
    }

    private void resetSchoolDirectorySelection() {
        selectedState =
                null;

        selectedDistrict =
                null;

        selectedDirectorySchool =
                null;

        selectedEducationBoard =
                "";

        manualSchoolEntryMode =
                false;

        binding.layoutDirectoryState
                .setEnabled(
                        false
                );

        binding.layoutDirectoryDistrict
                .setEnabled(
                        false
                );

        binding.layoutDirectoryEducationBoard
                .setEnabled(
                        false
                );

        binding.layoutDirectorySchoolSearch
                .setEnabled(
                        false
                );

        hideSelectedSchoolCard();
        hideDirectoryNoResult();
    }

    private void setDirectoryLoadingState(
            boolean loading,
            @Nullable String message
    ) {
        directoryOperationInProgress =
                loading;

        binding.containerSchoolDirectoryLoading
                .setVisibility(
                        loading
                                ? View.VISIBLE
                                : View.GONE
                );

        String safeMessage =
                safeText(
                        message
                );

        if (!safeMessage.isEmpty()) {
            binding.textSchoolDirectoryLoading
                    .setText(
                            safeMessage
                    );
        }

        binding.buttonEnterSchoolManually
                .setEnabled(
                        !loading
                );
    }

    private void showDirectoryNoResult(
            @NonNull String message
    ) {
        binding.textSchoolDirectoryNoResultDescription
                .setText(
                        message
                );

        binding.cardSchoolDirectoryNoResult
                .setVisibility(
                        View.VISIBLE
                );
    }

    private void hideDirectoryNoResult() {
        binding.cardSchoolDirectoryNoResult
                .setVisibility(
                        View.GONE
                );
    }

    private void hideSelectedSchoolCard() {
        binding.cardSelectedDirectorySchool
                .setVisibility(
                        View.GONE
                );
    }

    private void showDirectoryError(
            @NonNull String message
    ) {
        showDirectoryNoResult(
                message
        );

        Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void loadActiveStudent() {
        setOperationState(
                true,
                "Student profile load किया जा रहा है"
        );

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository
                        .SingleProfileCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable StudentProfileEntity
                                    studentProfile
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        if (studentProfile == null) {
                            setOperationState(
                                    false,
                                    ""
                            );

                            showMissingStudentProfileError();

                            return;
                        }

                        activeStudentProfile =
                                studentProfile;

                        displayStudentInformation(
                                studentProfile
                        );

                        ensureCurriculumProfile(
                                studentProfile
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setOperationState(
                                false,
                                ""
                        );

                        showError(
                                "Active student profile load नहीं हो सकी।"
                        );
                    }
                }
        );
    }

    private void ensureCurriculumProfile(
            @NonNull StudentProfileEntity studentProfile
    ) {
        setOperationState(
                true,
                "School curriculum तैयार किया जा रहा है"
        );

        curriculumProfileRepository
                .ensureBasicCurriculumProfile(
                        studentProfile,
                        new SchoolCurriculumProfileRepository
                                .EnsureProfileCallback() {

                            @Override
                            public void onReady(
                                    @NonNull
                                    SchoolCurriculumProfileEntity
                                            profile,
                                    boolean newlyCreated
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                curriculumProfile =
                                        profile;

                                displayCurriculumProfile(
                                        profile
                                );

                                loadSchoolSubjects(
                                        profile.getProfileId()
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                setOperationState(
                                        false,
                                        ""
                                );

                                showError(
                                        "School curriculum profile तैयार नहीं हो सकी।"
                                );
                            }
                        }
                );
    }

    private void loadSchoolSubjects(
            long profileId
    ) {
        setOperationState(
                true,
                "Confirmed school subjects load किए जा रहे हैं"
        );

        schoolSubjectRepository
                .getSubjectsForProfile(
                        profileId,
                        false,
                        new SchoolSubjectRepository
                                .SubjectsCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull
                                    List<SchoolSubjectEntity>
                                            schoolSubjects
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                subjectAdapter.submitList(
                                        schoolSubjects
                                );

                                updateSubjectListState();

                                setOperationState(
                                        false,
                                        ""
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                subjectAdapter.submitList(
                                        new ArrayList<>()
                                );

                                updateSubjectListState();

                                setOperationState(
                                        false,
                                        ""
                                );

                                showError(
                                        "School subjects load नहीं हो सके।"
                                );
                            }
                        }
                );
    }

    private void displayStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textCurriculumStudentName.setText(
                valueOrFallback(
                        studentProfile.getStudentName(),
                        "Active Student"
                )
        );

        StringBuilder summaryBuilder =
                new StringBuilder();

        appendSummaryPart(
                summaryBuilder,
                studentProfile.getEducationBoard()
        );

        appendSummaryPart(
                summaryBuilder,
                studentProfile.getStudentClass()
        );

        appendSummaryPart(
                summaryBuilder,
                studentProfile.getStudyMedium()
        );

        binding.textCurriculumStudentSummary.setText(
                summaryBuilder.length() > 0
                        ? summaryBuilder.toString()
                        : "Student academic details"
        );
    }

    private void displayCurriculumProfile(
            @NonNull SchoolCurriculumProfileEntity profile
    ) {
        formBindingInProgress =
                true;

        String schoolName =
                safeText(
                        profile.getSchoolName()
                );

        if (schoolName.equalsIgnoreCase(
                DEFAULT_SCHOOL_NAME
        )) {
            schoolName =
                    "";
        }

        binding.inputSchoolName.setText(
                schoolName
        );

        binding.inputSchoolCode.setText(
                safeText(
                        profile.getSchoolCode()
                )
        );

        binding.inputEducationBoard.setText(
                safeText(
                        profile.getEducationBoard()
                )
        );

        binding.inputClassNumber.setText(
                String.valueOf(
                        profile.getClassNumber()
                )
        );

        binding.inputSection.setText(
                safeText(
                        profile.getSection()
                )
        );

        binding.inputAcademicSession.setText(
                safeText(
                        profile.getAcademicSession()
                )
        );

        binding.inputStudyMedium.setText(
                safeText(
                        profile.getStudyMedium()
                )
        );

        formBindingInProgress =
                false;

        /*
         * Existing saved profile में directory selection metadata
         * अभी उपलब्ध नहीं है, इसलिए School fields editable रहेंगी।
         */
        binding.inputSchoolName
                .setEnabled(
                        true
                );

        binding.inputSchoolCode
                .setEnabled(
                        true
                );

        binding.inputEducationBoard
                .setEnabled(
                        true
                );

        if (!schoolName.isEmpty()) {
            manualSchoolEntryMode =
                    true;

            binding.textSchoolEntryMode
                    .setText(
                            "Saved / Manual School Details"
                    );
        }

        refreshValidationState(
                false
        );
    }

    private void showAddSubjectDialog() {
        if (operationInProgress) {
            return;
        }

        SchoolCurriculumProfileEntity profile =
                curriculumProfile;

        if (profile == null
                || profile.getProfileId() <= 0L) {

            showError(
                    "पहले curriculum profile तैयार होना आवश्यक है।"
            );

            return;
        }

        DialogAddSchoolSubjectBinding dialogBinding =
                DialogAddSchoolSubjectBinding.inflate(
                        getLayoutInflater()
                );

        setupSubjectCategoryDropdown(
                dialogBinding
        );

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(
                        this
                )
                        .setView(
                                dialogBinding.getRoot()
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Add Subject",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                ignored ->
                        dialog.getButton(
                                androidx.appcompat.app
                                        .AlertDialog
                                        .BUTTON_POSITIVE
                        ).setOnClickListener(
                                view ->
                                        validateAndAddSubject(
                                                dialog,
                                                dialogBinding,
                                                profile
                                        )
                        )
        );

        dialog.show();
    }

    private void setupSubjectCategoryDropdown(
            @NonNull DialogAddSchoolSubjectBinding
                    dialogBinding
    ) {
        String[] categories =
                new String[]{
                        "Core Academic",
                        "Language",
                        "Skill Based",
                        "Activity Based",
                        "School Specific"
                };

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_list_item_1,
                        categories
                );

        dialogBinding.inputSubjectCategory
                .setAdapter(
                        categoryAdapter
                );

        dialogBinding.inputSubjectCategory
                .setText(
                        categories[0],
                        false
                );
    }

    private void validateAndAddSubject(
            @NonNull androidx.appcompat.app.AlertDialog dialog,
            @NonNull DialogAddSchoolSubjectBinding
                    dialogBinding,
            @NonNull SchoolCurriculumProfileEntity profile
    ) {
        clearSubjectDialogErrors(
                dialogBinding
        );

        String englishName =
                safeText(
                        dialogBinding
                                .inputSubjectNameEnglish
                                .getText()
                );

        String hindiName =
                safeText(
                        dialogBinding
                                .inputSubjectNameHindi
                                .getText()
                );

        String subjectCode =
                safeText(
                        dialogBinding
                                .inputSubjectCode
                                .getText()
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        String categoryLabel =
                safeText(
                        dialogBinding
                                .inputSubjectCategory
                                .getText()
                );

        if (englishName.isEmpty()) {
            dialogBinding.layoutSubjectNameEnglish
                    .setError(
                            "Subject name आवश्यक है"
                    );

            return;
        }

        if (isDuplicateSubject(
                englishName,
                hindiName,
                subjectCode
        )) {
            dialogBinding.textSubjectDialogError
                    .setText(
                            "यह subject पहले से curriculum में मौजूद है।"
                    );

            dialogBinding.textSubjectDialogError
                    .setVisibility(
                            View.VISIBLE
                    );

            return;
        }

        dialog.dismiss();

        addSubjectToDatabase(
                profile,
                englishName,
                hindiName,
                subjectCode,
                convertCategoryToDatabaseValue(
                        categoryLabel
                ),
                dialogBinding
                        .switchSubjectEnabled
                        .isChecked(),
                dialogBinding
                        .switchSubjectAiTutorEnabled
                        .isChecked()
        );
    }

    private void addSubjectToDatabase(
            @NonNull SchoolCurriculumProfileEntity profile,
            @NonNull String englishName,
            @NonNull String hindiName,
            @NonNull String subjectCode,
            @NonNull String subjectCategory,
            boolean enabled,
            boolean aiTutorEnabled
    ) {
        setOperationState(
                true,
                "School subject save किया जा रहा है"
        );

        schoolSubjectRepository.getNextSortOrder(
                profile.getProfileId(),
                new SchoolSubjectRepository
                        .SortOrderCallback() {

                    @Override
                    public void onSuccess(
                            int nextSortOrder
                    ) {
                        SchoolSubjectEntity schoolSubject =
                                createSchoolSubject(
                                        profile.getProfileId(),
                                        englishName,
                                        hindiName,
                                        subjectCode,
                                        subjectCategory,
                                        enabled,
                                        aiTutorEnabled,
                                        nextSortOrder
                                );

                        insertSchoolSubject(
                                schoolSubject
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setOperationState(
                                false,
                                ""
                        );

                        showError(
                                "Subject का sort order तैयार नहीं हो सका।"
                        );
                    }
                }
        );
    }

    private void insertSchoolSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        schoolSubjectRepository.insertSubject(
                schoolSubject,
                new SchoolSubjectRepository
                        .InsertSubjectCallback() {

                    @Override
                    public void onSuccess(
                            long insertedSubjectRowId
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        schoolSubject.setSubjectRowId(
                                insertedSubjectRowId
                        );

                        subjectAdapter.addSubject(
                                schoolSubject
                        );

                        updateSubjectListState();

                        setOperationState(
                                false,
                                ""
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                schoolSubject
                                        .getSubjectNameEnglish()
                                        + " curriculum में जोड़ दिया गया है।",
                                Snackbar.LENGTH_LONG
                        ).show();

                        setResult(
                                RESULT_OK
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setOperationState(
                                false,
                                ""
                        );

                        showError(
                                "Subject save नहीं हो सका। संभव है कि यह पहले से मौजूद हो।"
                        );
                    }
                }
        );
    }

    @NonNull
    private SchoolSubjectEntity createSchoolSubject(
            long profileId,
            @NonNull String englishName,
            @NonNull String hindiName,
            @NonNull String subjectCode,
            @NonNull String subjectCategory,
            boolean enabled,
            boolean aiTutorEnabled,
            int sortOrder
    ) {
        long currentTime =
                System.currentTimeMillis();

        SchoolSubjectEntity schoolSubject =
                new SchoolSubjectEntity();

        schoolSubject.setProfileId(
                profileId
        );

        schoolSubject.setSubjectId(
                SchoolSubjectEntity.createSubjectId(
                        englishName
                )
        );

        schoolSubject.setSubjectNameEnglish(
                englishName
        );

        schoolSubject.setSubjectNameHindi(
                hindiName
        );

        schoolSubject.setSubjectCode(
                subjectCode.isEmpty()
                        ? createSubjectCode(
                        englishName
                )
                        : subjectCode
        );

        schoolSubject.setBookName(
                ""
        );

        schoolSubject.setBookCode(
                ""
        );

        schoolSubject.setPublisherName(
                ""
        );

        schoolSubject.setSubjectCategory(
                subjectCategory
        );

        schoolSubject.setContentSource(
                CONTENT_SOURCE_SCHOOL_BOOK
        );

        schoolSubject.setContentPackId(
                ""
        );

        schoolSubject.setEnabled(
                enabled
        );

        schoolSubject.setAiTutorEnabled(
                aiTutorEnabled
        );

        schoolSubject.setOfficialCoreSubject(
                subjectCategory.equals(
                        "CORE_ACADEMIC"
                )
                        || subjectCategory.equals(
                        "LANGUAGE"
                )
        );

        schoolSubject.setAllowParentContentEditing(
                true
        );

        schoolSubject.setSortOrder(
                Math.max(
                        1,
                        sortOrder
                )
        );

        schoolSubject.setChapterCount(
                0
        );

        schoolSubject.setLessonCount(
                0
        );

        schoolSubject.setQuizQuestionCount(
                0
        );

        schoolSubject.setCreatedAt(
                currentTime
        );

        schoolSubject.setUpdatedAt(
                currentTime
        );

        return schoolSubject;
    }

    private void saveSchoolCurriculum() {
        if (operationInProgress) {
            return;
        }

        saveAttempted =
                true;

        SchoolCurriculumSetupValidator.ValidationResult
                validationResult =
                createValidationResult();

        applyValidationErrors(
                validationResult
        );

        updateSaveButtonState(
                validationResult
        );

        if (!validationResult.canSaveSchoolSetup()) {
            focusFirstInvalidField(
                    validationResult
            );

            showError(
                    createValidationErrorMessage(
                            validationResult
                    )
            );

            return;
        }

        SchoolCurriculumProfileEntity profile =
                curriculumProfile;

        if (profile == null
                || profile.getProfileId() <= 0L) {

            showError(
                    "Curriculum profile उपलब्ध नहीं है।"
            );

            return;
        }

        updateCurriculumProfileFromForm(
                profile,
                validationResult
        );

        setOperationState(
                true,
                "School curriculum save किया जा रहा है"
        );

        curriculumProfileRepository
                .insertOrUpdateCurriculumProfile(
                        profile,
                        new SchoolCurriculumProfileRepository
                                .SaveProfileCallback() {

                            @Override
                            public void onSuccess(
                                    long savedProfileId
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                curriculumProfile =
                                        profile;

                                saveAttempted =
                                        false;

                                setOperationState(
                                        false,
                                        ""
                                );

                                setResult(
                                        RESULT_OK
                                );

                                showCurriculumSaveResult(
                                        validationResult
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                setOperationState(
                                        false,
                                        ""
                                );

                                showError(
                                        "School curriculum save नहीं हो सका।"
                                );
                            }
                        }
                );
    }

    private void updateCurriculumProfileFromForm(
            @NonNull SchoolCurriculumProfileEntity profile,
            @NonNull SchoolCurriculumSetupValidator
                    .ValidationResult validationResult
    ) {
        profile.setSchoolName(
                validationResult.getSchoolName()
        );

        profile.setSchoolCode(
                validationResult.getSchoolCode()
        );

        profile.setEducationBoard(
                validationResult.getEducationBoard()
        );

        profile.setSchoolPattern(
                validationResult.getEducationBoard()
        );

        profile.setClassNumber(
                validationResult.getClassNumber()
        );

        profile.setSection(
                validationResult.getSection()
        );

        profile.setAcademicSession(
                validationResult.getAcademicSession()
        );

        profile.setStudyMedium(
                validationResult.getStudyMedium()
        );

        profile.setConfigured(
                validationResult.isReadyForChildMode()
        );

        profile.setUpdatedAt(
                System.currentTimeMillis()
        );
    }

    private void showCurriculumSaveResult(
            @NonNull SchoolCurriculumSetupValidator
                    .ValidationResult validationResult
    ) {
        String schoolSelectionStatus;

        if (selectedDirectorySchool != null) {
            schoolSelectionStatus =
                    selectedDirectorySchool
                            .getVerificationLabel();

        } else {
            schoolSelectionStatus =
                    "Parent Entered • Not Officially Verified";
        }

        if (validationResult.isReadyForChildMode()) {
            new MaterialAlertDialogBuilder(
                    this
            )
                    .setTitle(
                            "Curriculum तैयार है"
                    )
                    .setMessage(
                            validationResult
                                    .getSetupStatusMessage()
                                    + "\n\nSchool Status: "
                                    + schoolSelectionStatus
                                    + "\n\nअब बच्चे को केवल confirmed school subjects और उनकी exact books दिखाई जाएँगी।"
                    )
                    .setPositiveButton(
                            "ठीक है",
                            null
                    )
                    .show();

            return;
        }

        int pendingBookCount =
                validationResult.getPendingBookCount();

        String bookWord =
                pendingBookCount == 1
                        ? "book"
                        : "books";

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "School setup save हो गया"
                )
                .setMessage(
                        validationResult
                                .getSetupStatusMessage()
                                + "\n\nSchool Status: "
                                + schoolSelectionStatus
                                + "\n\n"
                                + pendingBookCount
                                + " exact school "
                                + bookWord
                                + " confirm होने तक Child Mode curriculum locked रहेगा।"
                )
                .setPositiveButton(
                        "ठीक है",
                        null
                )
                .show();
    }

    private void showBookAddMethodDialog(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        if (operationInProgress) {
            return;
        }

        if (schoolSubject.getSubjectRowId() <= 0L) {
            showError(
                    "Valid school subject उपलब्ध नहीं है।"
            );

            return;
        }

        String subjectName =
                getSubjectDisplayName(
                        schoolSubject
                );

        String existingBookName =
                safeText(
                        schoolSubject.getBookName()
                );

        String message;

        if (existingBookName.isEmpty()) {
            message =
                    subjectName
                            + " में पढ़ाई जा रही exact school book जोड़ें।";

        } else {
            message =
                    "अभी जुड़ी book: "
                            + existingBookName
                            + "\n\nनई book confirm होने तक मौजूदा book सुरक्षित रहेगी।";
        }

        String[] options =
                new String[]{
                        "Scan / Search Book Online",
                        "Add Book Manually"
                };

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        existingBookName.isEmpty()
                                ? "Add Exact School Book"
                                : "Change Exact School Book"
                )
                .setMessage(
                        message
                )
                .setItems(
                        options,
                        (dialog, selectedOption) -> {
                            if (selectedOption == 0) {
                                launchOnlineBookScan(
                                        schoolSubject
                                );

                            } else {
                                launchManualBookEntry(
                                        schoolSubject
                                );
                            }
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void launchOnlineBookScan(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        Intent intent =
                new Intent(
                        this,
                        BookCoverScanActivity.class
                );

        intent.putExtra(
                EXTRA_TARGET_SUBJECT_ROW_ID,
                schoolSubject.getSubjectRowId()
        );

        intent.putExtra(
                EXTRA_TARGET_PROFILE_ID,
                schoolSubject.getProfileId()
        );

        intent.putExtra(
                EXTRA_TARGET_SUBJECT_NAME,
                getSubjectDisplayName(
                        schoolSubject
                )
        );

        bookSetupLauncher.launch(
                intent
        );
    }

    private void launchManualBookEntry(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        Intent intent =
                ManualSchoolBookActivity.createIntent(
                        this,
                        schoolSubject.getSubjectRowId()
                );

        bookSetupLauncher.launch(
                intent
        );
    }

    @NonNull
    private SchoolCurriculumSetupValidator.ValidationResult
    createValidationResult() {
        return SchoolCurriculumSetupValidator.validate(
                binding.inputSchoolName.getText(),
                binding.inputSchoolCode.getText(),
                binding.inputEducationBoard.getText(),
                binding.inputClassNumber.getText(),
                binding.inputSection.getText(),
                binding.inputAcademicSession.getText(),
                binding.inputStudyMedium.getText(),
                subjectAdapter.getCurrentSubjects()
        );
    }

    private void refreshValidationState(
            boolean showErrors
    ) {
        if (binding == null
                || subjectAdapter == null) {
            return;
        }

        SchoolCurriculumSetupValidator.ValidationResult
                validationResult =
                createValidationResult();

        if (showErrors) {
            applyValidationErrors(
                    validationResult
            );

        } else {
            clearValidationErrors();
        }

        updateSaveButtonState(
                validationResult
        );
    }

    private void updateSaveButtonState(
            @NonNull SchoolCurriculumSetupValidator
                    .ValidationResult validationResult
    ) {
        boolean saveButtonEnabled =
                !operationInProgress
                        && validationResult
                        .canSaveSchoolSetup();

        binding.buttonSaveSchoolCurriculum
                .setEnabled(
                        saveButtonEnabled
                );

        if (!validationResult.isSchoolDetailsValid()) {
            binding.buttonSaveSchoolCurriculum
                    .setText(
                            "Complete School Details"
                    );

            return;
        }

        if (validationResult.getEnabledSubjectCount()
                <= 0) {

            binding.buttonSaveSchoolCurriculum
                    .setText(
                            "Add School Subjects"
                    );

            return;
        }

        if (validationResult.getPendingBookCount()
                > 0) {

            binding.buttonSaveSchoolCurriculum
                    .setText(
                            "Save Setup • "
                                    + validationResult
                                    .getPendingBookCount()
                                    + " Books Pending"
                    );

            return;
        }

        binding.buttonSaveSchoolCurriculum
                .setText(
                        "Save & Activate Curriculum"
                );
    }

    private void applyValidationErrors(
            @NonNull SchoolCurriculumSetupValidator
                    .ValidationResult validationResult
    ) {
        binding.layoutSchoolName.setError(
                emptyToNull(
                        validationResult
                                .getSchoolNameError()
                )
        );

        binding.layoutEducationBoard.setError(
                emptyToNull(
                        validationResult
                                .getEducationBoardError()
                )
        );

        binding.layoutClassNumber.setError(
                emptyToNull(
                        validationResult
                                .getClassNumberError()
                )
        );

        binding.layoutAcademicSession.setError(
                emptyToNull(
                        validationResult
                                .getAcademicSessionError()
                )
        );

        binding.layoutStudyMedium.setError(
                emptyToNull(
                        validationResult
                                .getStudyMediumError()
                )
        );
    }

    private void clearValidationErrors() {
        binding.layoutSchoolName.setError(
                null
        );

        binding.layoutEducationBoard.setError(
                null
        );

        binding.layoutClassNumber.setError(
                null
        );

        binding.layoutAcademicSession.setError(
                null
        );

        binding.layoutStudyMedium.setError(
                null
        );
    }

    private void focusFirstInvalidField(
            @NonNull SchoolCurriculumSetupValidator
                    .ValidationResult validationResult
    ) {
        if (!validationResult
                .getSchoolNameError()
                .isEmpty()) {

            requestInputFocus(
                    binding.inputSchoolName
            );

            return;
        }

        if (!validationResult
                .getEducationBoardError()
                .isEmpty()) {

            requestInputFocus(
                    binding.inputEducationBoard
            );

            return;
        }

        if (!validationResult
                .getClassNumberError()
                .isEmpty()) {

            requestInputFocus(
                    binding.inputClassNumber
            );

            return;
        }

        if (!validationResult
                .getAcademicSessionError()
                .isEmpty()) {

            requestInputFocus(
                    binding.inputAcademicSession
            );

            return;
        }

        if (!validationResult
                .getStudyMediumError()
                .isEmpty()) {

            requestInputFocus(
                    binding.inputStudyMedium
            );

            return;
        }

        if (validationResult.getEnabledSubjectCount()
                <= 0) {

            binding.buttonAddSchoolSubject
                    .requestFocus();
        }
    }

    private void requestInputFocus(
            @NonNull EditText input
    ) {
        input.requestFocus();

        binding.scrollSchoolCurriculumSetup
                .smoothScrollTo(
                        0,
                        Math.max(
                                0,
                                input.getTop() - 80
                        )
                );
    }

    @NonNull
    private String createValidationErrorMessage(
            @NonNull SchoolCurriculumSetupValidator
                    .ValidationResult validationResult
    ) {
        if (!validationResult
                .getSchoolNameError()
                .isEmpty()) {

            return validationResult
                    .getSchoolNameError();
        }

        if (!validationResult
                .getEducationBoardError()
                .isEmpty()) {

            return validationResult
                    .getEducationBoardError();
        }

        if (!validationResult
                .getClassNumberError()
                .isEmpty()) {

            return validationResult
                    .getClassNumberError();
        }

        if (!validationResult
                .getAcademicSessionError()
                .isEmpty()) {

            return validationResult
                    .getAcademicSessionError();
        }

        if (!validationResult
                .getStudyMediumError()
                .isEmpty()) {

            return validationResult
                    .getStudyMediumError();
        }

        if (validationResult.hasGeneralErrors()) {
            return validationResult
                    .getGeneralErrors()
                    .get(
                            0
                    );
        }

        return validationResult
                .getSetupStatusMessage();
    }

    @Nullable
    private String emptyToNull(
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        return safeValue.isEmpty()
                ? null
                : safeValue;
    }

    private boolean isDuplicateSubject(
            @NonNull String englishName,
            @NonNull String hindiName,
            @NonNull String subjectCode
    ) {
        String normalizedEnglishName =
                normalizeText(
                        englishName
                );

        String normalizedHindiName =
                normalizeText(
                        hindiName
                );

        String normalizedCode =
                normalizeText(
                        subjectCode
                );

        for (SchoolSubjectEntity existingSubject :
                subjectAdapter.getCurrentSubjects()) {

            if (!normalizedEnglishName.isEmpty()
                    && normalizedEnglishName.equals(
                    normalizeText(
                            existingSubject
                                    .getSubjectNameEnglish()
                    )
            )) {
                return true;
            }

            if (!normalizedHindiName.isEmpty()
                    && normalizedHindiName.equals(
                    normalizeText(
                            existingSubject
                                    .getSubjectNameHindi()
                    )
            )) {
                return true;
            }

            if (!normalizedCode.isEmpty()
                    && normalizedCode.equals(
                    normalizeText(
                            existingSubject
                                    .getSubjectCode()
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    private void clearSubjectDialogErrors(
            @NonNull DialogAddSchoolSubjectBinding
                    dialogBinding
    ) {
        dialogBinding.layoutSubjectNameEnglish
                .setError(
                        null
                );

        dialogBinding.layoutSubjectNameHindi
                .setError(
                        null
                );

        dialogBinding.layoutSubjectCode
                .setError(
                        null
                );

        dialogBinding.textSubjectDialogError
                .setText(
                        ""
                );

        dialogBinding.textSubjectDialogError
                .setVisibility(
                        View.GONE
                );
    }

    @NonNull
    private String convertCategoryToDatabaseValue(
            @Nullable String categoryLabel
    ) {
        String normalizedCategory =
                normalizeText(
                        categoryLabel
                );

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
    private String createSubjectCode(
            @NonNull String subjectName
    ) {
        String normalizedName =
                subjectName
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^A-Z0-9]",
                                ""
                        );

        if (normalizedName.isEmpty()) {
            return "SUB";
        }

        return normalizedName.substring(
                0,
                Math.min(
                        6,
                        normalizedName.length()
                )
        );
    }

    private void updateSubjectListState() {
        boolean hasSubjects =
                subjectAdapter.hasSubjects();

        binding.recyclerCurriculumSubjects
                .setVisibility(
                        hasSubjects
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.containerEmptyCurriculumSubjects
                .setVisibility(
                        hasSubjects
                                ? View.GONE
                                : View.VISIBLE
                );

        int selectedSubjectCount =
                subjectAdapter
                        .getSelectedSubjectCount();

        binding.textSelectedSubjectCount.setText(
                selectedSubjectCount
                        + (selectedSubjectCount == 1
                        ? " Subject"
                        : " Subjects")
        );

        refreshValidationState(
                saveAttempted
        );
    }

    @Override
    public void onSubjectSelected(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        showBookAddMethodDialog(
                schoolSubject
        );
    }

    @Override
    public void onScanBookClicked(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        showBookAddMethodDialog(
                schoolSubject
        );
    }

    @Override
    public void onEditSubjectClicked(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        Snackbar.make(
                binding.getRoot(),
                "Subject Edit dialog अगले step में जोड़ा जाएगा।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    @Override
    public void onRemoveSubjectClicked(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        Snackbar.make(
                binding.getRoot(),
                "Subject Remove confirmation अगले step में जोड़ी जाएगी।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showImportSubjectMessage() {
        Snackbar.make(
                binding.getRoot(),
                "School directory और subject import अगले package में जोड़ा जाएगा।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showMissingStudentProfileError() {
        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "Active student उपलब्ध नहीं है"
                )
                .setMessage(
                        "School curriculum setup से पहले student profile बनाकर active करें।"
                )
                .setCancelable(
                        false
                )
                .setPositiveButton(
                        "वापस जाएँ",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    private void showError(
            @NonNull String message
    ) {
        Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void setOperationState(
            boolean inProgress,
            @Nullable String progressMessage
    ) {
        operationInProgress =
                inProgress;

        binding.containerCurriculumSetupLoading
                .setVisibility(
                        inProgress
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.buttonAddSchoolSubject
                .setEnabled(
                        !inProgress
                );

        binding.buttonImportSubjectList
                .setEnabled(
                        !inProgress
                );

        binding.buttonCancelCurriculumSetup
                .setEnabled(
                        !inProgress
                );

        String safeProgressMessage =
                safeText(
                        progressMessage
                );

        if (!safeProgressMessage.isEmpty()) {
            binding.textCurriculumSetupProgress
                    .setText(
                            safeProgressMessage
                    );
        }

        refreshValidationState(
                saveAttempted
        );
    }

    @NonNull
    private String getSubjectDisplayName(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String englishName =
                safeText(
                        schoolSubject.getSubjectNameEnglish()
                );

        if (!englishName.isEmpty()) {
            return englishName;
        }

        String hindiName =
                safeText(
                        schoolSubject.getSubjectNameHindi()
                );

        return hindiName.isEmpty()
                ? "School Subject"
                : hindiName;
    }

    @NonNull
    private String createSelectedSchoolDetails(
            @NonNull SchoolDirectoryEntity school
    ) {
        StringBuilder details =
                new StringBuilder();

        if (selectedDistrict != null) {
            appendSummaryPart(
                    details,
                    selectedDistrict.getDistrictName()
            );
        }

        appendSummaryPart(
                details,
                formatBoardName(
                        school.getEducationBoard()
                )
        );

        appendSummaryPart(
                details,
                school.getPreferredSchoolCode()
        );

        appendSummaryPart(
                details,
                school.getAddressLine()
        );

        return details.length() == 0
                ? "School directory record"
                : details.toString();
    }

    @NonNull
    private String normalizeBoard(
            @Nullable Object value
    ) {
        String board =
                safeText(
                        value
                )
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replace(
                                "-",
                                "_"
                        )
                        .replace(
                                " ",
                                "_"
                        );

        if (board.equals(
                "CISCE_/_ICSE_/_ISC"
        )
                || board.equals(
                "ICSE"
        )
                || board.equals(
                "ISC"
        )) {

            return "CISCE";
        }

        if (board.equals(
                "UP_BOARD"
        )
                || board.equals(
                "UPMSP"
        )) {

            return "UPMSP";
        }

        if (board.equals(
                "STATE_BOARD"
        )) {

            return "STATE_BOARD";
        }

        return board;
    }

    @NonNull
    private String formatBoardName(
            @Nullable Object value
    ) {
        String board =
                normalizeBoard(
                        value
                );

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
                return board.replace(
                        "_",
                        " "
                );
        }
    }

    private boolean isActivityAvailable() {
        return !isFinishing()
                && !isDestroyed()
                && binding != null;
    }

    private void appendSummaryPart(
            @NonNull StringBuilder builder,
            @Nullable Object value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(
                    "  •  "
            );
        }

        builder.append(
                safeValue
        );
    }

    @NonNull
    private String valueOrFallback(
            @Nullable Object value,
            @NonNull String fallback
    ) {
        String safeValue =
                safeText(
                        value
                );

        return safeValue.isEmpty()
                ? fallback
                : safeValue;
    }

    @NonNull
    private String normalizeText(
            @Nullable Object value
    ) {
        return safeText(
                value
        )
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    @NonNull
    private String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @Override
    protected void onDestroy() {
        binding =
                null;

        super.onDestroy();
    }
}