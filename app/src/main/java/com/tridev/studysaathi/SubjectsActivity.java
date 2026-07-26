package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.SubjectAdapter;
import com.tridev.studysaathi.data.content.policy
        .ChildSubjectVisibilityPolicy;
import com.tridev.studysaathi.data.local.entity
        .SchoolSubjectEntity;
import com.tridev.studysaathi.data.local.entity
        .StudentProfileEntity;
import com.tridev.studysaathi.data.repository
        .SchoolSubjectRepository;
import com.tridev.studysaathi.data.repository
        .StudentProfileRepository;
import com.tridev.studysaathi.databinding
        .ActivitySubjectsBinding;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SubjectsActivity
        extends AppCompatActivity {

    private ActivitySubjectsBinding binding;

    private StudentProfileRepository studentProfileRepository;

    private SchoolSubjectRepository schoolSubjectRepository;

    private SubjectAdapter subjectAdapter;

    private ActivityResultLauncher<Intent> bookScanLauncher;

    @NonNull
    private List<SchoolSubjectEntity> visibleSchoolSubjects =
            new ArrayList<>();

    @NonNull
    private String activeEducationBoard =
            "CBSE";

    @NonNull
    private String activeStudentClass =
            "Class 6";

    private long activeProfileId =
            -1L;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivitySubjectsBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        studentProfileRepository =
                new StudentProfileRepository(
                        this
                );

        schoolSubjectRepository =
                new SchoolSubjectRepository(
                        this
                );

        registerBookScanLauncher();
        setupRecyclerView();
        setupClickListeners();
        loadActiveStudentProfile();
    }

    private void registerBookScanLauncher() {
        bookScanLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .StartActivityForResult(),
                        result -> {
                            if (!isActivityAvailable()
                                    || result.getResultCode()
                                    != RESULT_OK
                                    || activeProfileId <= 0L) {

                                return;
                            }

                            loadConfirmedSchoolSubjects(
                                    activeProfileId
                            );
                        }
                );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (binding != null
                && activeProfileId > 0L) {

            loadConfirmedSchoolSubjects(
                    activeProfileId
            );
        }
    }

    private void setupRecyclerView() {
        subjectAdapter =
                new SubjectAdapter(
                        new ArrayList<>(),
                        this::handleSubjectSelection
                );

        binding.recyclerSubjects.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        binding.recyclerSubjects.setAdapter(
                subjectAdapter
        );

        binding.recyclerSubjects.setHasFixedSize(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.cardScanSchoolBook
                .setOnClickListener(view ->
                        openBookCoverScanner()
                );
    }

    private void openBookCoverScanner() {
        if (!isActivityAvailable()) {
            return;
        }

        if (activeProfileId <= 0L) {
            Snackbar.make(
                    binding.getRoot(),
                    "पहले active student profile तैयार करें।",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        Intent bookScannerIntent =
                new Intent(
                        SubjectsActivity.this,
                        BookCoverScanActivity.class
                );

        bookScannerIntent.putExtra(
                BookCoverScanActivity
                        .EXTRA_TARGET_PROFILE_ID,
                activeProfileId
        );

        bookScanLauncher.launch(
                bookScannerIntent
        );
    }

    private void loadActiveStudentProfile() {
        showLoadingState(
                true
        );

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository
                        .SingleProfileCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable StudentProfileEntity studentProfile
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        if (studentProfile == null
                                || studentProfile.getProfileId() <= 0L) {

                            showLoadingState(
                                    false
                            );

                            showNoProfileState();

                            return;
                        }

                        displayStudentProfile(
                                studentProfile
                        );

                        activeProfileId =
                                studentProfile.getProfileId();

                        loadConfirmedSchoolSubjects(
                                activeProfileId
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        showLoadingState(
                                false
                        );

                        showNoProfileState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.profile_loading_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void displayStudentProfile(
            @NonNull StudentProfileEntity studentProfile
    ) {
        activeEducationBoard =
                valueOrFallback(
                        studentProfile.getEducationBoard(),
                        "School Board"
                );

        activeStudentClass =
                valueOrFallback(
                        studentProfile.getStudentClass(),
                        "Current Class"
                );

        binding.textSubjectProfile.setText(
                activeEducationBoard
                        + "  •  "
                        + activeStudentClass
        );

        binding.textSubjectStudentName.setText(
                getString(
                        R.string.subjects_for_student_format,
                        valueOrFallback(
                                studentProfile.getStudentName(),
                                "Student"
                        )
                )
        );
    }

    private void loadConfirmedSchoolSubjects(
            long profileId
    ) {
        if (profileId <= 0L) {
            showLoadingState(
                    false
            );

            showNoProfileState();

            return;
        }

        showLoadingState(
                true
        );

        /*
         * Repository से सभी subjects लेकर Child policy स्वयं
         * enabled/book-confirmed filtering करेगी।
         */
        schoolSubjectRepository.getSubjectsForProfile(
                profileId,
                false,
                new SchoolSubjectRepository.SubjectsCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolSubjectEntity> schoolSubjects
                    ) {
                        if (!isActivityAvailable()
                                || profileId != activeProfileId) {

                            return;
                        }

                        ChildSubjectVisibilityPolicy
                                .VisibilitySummary summary =
                                ChildSubjectVisibilityPolicy
                                        .createVisibilitySummary(
                                                schoolSubjects
                                        );

                        List<SchoolSubjectEntity> filteredSubjects =
                                ChildSubjectVisibilityPolicy
                                        .filterVisibleSubjects(
                                                schoolSubjects
                                        );

                        visibleSchoolSubjects =
                                new ArrayList<>(
                                        filteredSubjects
                                );

                        List<SubjectItem> subjectItems =
                                createSubjectItems(
                                        filteredSubjects
                                );

                        subjectAdapter.submitList(
                                subjectItems
                        );

                        updateSubjectListState(
                                subjectItems.size()
                        );

                        showLoadingState(
                                false
                        );

                        if (subjectItems.isEmpty()) {
                            Snackbar.make(
                                    binding.getRoot(),
                                    summary.createStatusMessage(),
                                    Snackbar.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()
                                || profileId != activeProfileId) {

                            return;
                        }

                        visibleSchoolSubjects =
                                new ArrayList<>();

                        subjectAdapter.submitList(
                                new ArrayList<>()
                        );

                        updateSubjectListState(
                                0
                        );

                        showLoadingState(
                                false
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                "Confirmed school subjects "
                                        + "load नहीं हो सके।",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    @NonNull
    private List<SubjectItem> createSubjectItems(
            @NonNull List<SchoolSubjectEntity> schoolSubjects
    ) {
        List<SubjectItem> subjectItems =
                new ArrayList<>();

        for (int index = 0;
             index < schoolSubjects.size();
             index++) {

            SchoolSubjectEntity schoolSubject =
                    schoolSubjects.get(
                            index
                    );

            subjectItems.add(
                    createSubjectItem(
                            schoolSubject,
                            index
                    )
            );
        }

        return subjectItems;
    }

    @NonNull
    private SubjectItem createSubjectItem(
            @NonNull SchoolSubjectEntity schoolSubject,
            int position
    ) {
        String subjectName =
                ChildSubjectVisibilityPolicy
                        .getSubjectDisplayName(
                                schoolSubject
                        );

        String subjectDescription =
                createSubjectDescription(
                        schoolSubject
                );

        String subjectIcon =
                createSubjectIcon(
                        schoolSubject
                );

        int paletteIndex =
                Math.floorMod(
                        position,
                        6
                );

        int backgroundColorRes;
        int borderColorRes;
        int accentColorRes;

        switch (paletteIndex) {
            case 1:
                backgroundColorRes =
                        R.color.ss_yellow_soft;

                borderColorRes =
                        R.color.ss_yellow_border;

                accentColorRes =
                        R.color.ss_warning;
                break;

            case 2:
                backgroundColorRes =
                        R.color.ss_green_soft;

                borderColorRes =
                        R.color.ss_green_border;

                accentColorRes =
                        R.color.ss_success;
                break;

            case 3:
                backgroundColorRes =
                        R.color.ss_teal_soft;

                borderColorRes =
                        R.color.ss_teal_border;

                accentColorRes =
                        R.color.ss_secondary;
                break;

            case 4:
                backgroundColorRes =
                        R.color.ss_red_soft;

                borderColorRes =
                        R.color.ss_red_border;

                accentColorRes =
                        R.color.ss_error;
                break;

            case 5:
                backgroundColorRes =
                        R.color.ss_purple_soft;

                borderColorRes =
                        R.color.ss_purple_border;

                accentColorRes =
                        R.color.ss_primary_dark;
                break;

            case 0:
            default:
                backgroundColorRes =
                        R.color.ss_blue_soft;

                borderColorRes =
                        R.color.ss_blue_border;

                accentColorRes =
                        R.color.ss_primary;
                break;
        }

        return new SubjectItem(
                subjectName,
                subjectDescription,
                subjectIcon,
                backgroundColorRes,
                borderColorRes,
                accentColorRes
        );
    }

    @NonNull
    private String createSubjectDescription(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        StringBuilder description =
                new StringBuilder();

        String bookName =
                safeText(
                        schoolSubject.getBookName()
                );

        if (!bookName.isEmpty()) {
            description.append(
                    "Exact Book: "
            );

            description.append(
                    bookName
            );
        }

        appendDescriptionPart(
                description,
                schoolSubject.getPublisherName()
        );

        if (schoolSubject.getChapterCount() > 0) {
            int chapterCount =
                    schoolSubject.getChapterCount();

            appendDescriptionPart(
                    description,
                    chapterCount
                            + (chapterCount == 1
                            ? " Chapter"
                            : " Chapters")
            );

        } else {
            appendDescriptionPart(
                    description,
                    "Open to view confirmed chapters"
            );
        }

        return description.length() == 0
                ? "Exact school book confirmed"
                : description.toString();
    }

    private void appendDescriptionPart(
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
    private String createSubjectIcon(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String subjectName =
                ChildSubjectVisibilityPolicy
                        .getSubjectDisplayName(
                                schoolSubject
                        );

        String normalizedName =
                normalizeText(
                        subjectName
                );

        if (normalizedName.contains(
                "mathematics"
        )
                || normalizedName.equals(
                "math"
        )
                || normalizedName.contains(
                "गणित"
        )) {
            return "∑";
        }

        if (normalizedName.contains(
                "hindi"
        )
                || normalizedName.contains(
                "हिंदी"
        )) {
            return "अ";
        }

        if (normalizedName.contains(
                "english"
        )
                || normalizedName.contains(
                "अंग्रेज"
        )) {
            return "A";
        }

        if (normalizedName.contains(
                "science"
        )
                || normalizedName.contains(
                "विज्ञान"
        )) {
            return "⚗";
        }

        if (normalizedName.contains(
                "social"
        )
                || normalizedName.contains(
                "history"
        )
                || normalizedName.contains(
                "geography"
        )
                || normalizedName.contains(
                "civics"
        )
                || normalizedName.contains(
                "सामाजिक"
        )) {
            return "◎";
        }

        if (normalizedName.contains(
                "computer"
        )
                || normalizedName.contains(
                "information technology"
        )
                || normalizedName.contains(
                "कंप्यूटर"
        )) {
            return "</>";
        }

        if (normalizedName.contains(
                "sanskrit"
        )
                || normalizedName.contains(
                "संस्कृत"
        )) {
            return "सं";
        }

        String trimmedName =
                safeText(
                        subjectName
                );

        if (trimmedName.isEmpty()) {
            return "S";
        }

        int firstCodePoint =
                trimmedName.codePointAt(
                        0
                );

        return new String(
                Character.toChars(
                        firstCodePoint
                )
        ).toUpperCase(
                Locale.ROOT
        );
    }

    private void updateSubjectListState(
            int visibleSubjectCount
    ) {
        binding.textSubjectCount.setText(
                getString(
                        R.string.subject_count_format,
                        Math.max(
                                0,
                                visibleSubjectCount
                        )
                )
        );

        boolean subjectsAvailable =
                visibleSubjectCount > 0;

        binding.recyclerSubjects.setVisibility(
                subjectsAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptySubjects.setVisibility(
                subjectsAvailable
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private void showNoProfileState() {
        activeProfileId =
                -1L;

        activeEducationBoard =
                "School Board";

        activeStudentClass =
                "Current Class";

        visibleSchoolSubjects =
                new ArrayList<>();

        subjectAdapter.submitList(
                new ArrayList<>()
        );

        binding.textSubjectProfile.setText(
                R.string.no_active_profile
        );

        binding.textSubjectStudentName.setText(
                R.string.create_profile_to_continue
        );

        updateSubjectListState(
                0
        );
    }

    /**
     * Child द्वारा subject card tap करने पर exact school-book
     * chapter mode खोला जाता है।
     */
    private void handleSubjectSelection(
            @NonNull SubjectItem subjectItem
    ) {
        SchoolSubjectEntity selectedSubject =
                findVisibleSchoolSubject(
                        subjectItem.getSubjectName()
                );

        if (selectedSubject == null
                || selectedSubject.getSubjectRowId() <= 0L) {

            Snackbar.make(
                    binding.getRoot(),
                    "Selected school subject उपलब्ध नहीं है।",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        String selectedSubjectName =
                ChildSubjectVisibilityPolicy
                        .getSubjectDisplayName(
                                selectedSubject
                        );

        Intent chaptersIntent =
                new Intent(
                        SubjectsActivity.this,
                        ChaptersActivity.class
                );

        chaptersIntent.putExtra(
                ChaptersActivity.EXTRA_SCHOOL_SUBJECT_ROW_ID,
                selectedSubject.getSubjectRowId()
        );

        chaptersIntent.putExtra(
                ChaptersActivity.EXTRA_SUBJECT_NAME,
                selectedSubjectName
        );

        chaptersIntent.putExtra(
                ChaptersActivity.EXTRA_STUDENT_CLASS,
                activeStudentClass
        );

        chaptersIntent.putExtra(
                ChaptersActivity.EXTRA_EDUCATION_BOARD,
                activeEducationBoard
        );

        startActivity(
                chaptersIntent
        );
    }

    @Nullable
    private SchoolSubjectEntity findVisibleSchoolSubject(
            @Nullable String selectedSubjectName
    ) {
        String normalizedSelectedName =
                normalizeText(
                        selectedSubjectName
                );

        if (normalizedSelectedName.isEmpty()) {
            return null;
        }

        for (SchoolSubjectEntity schoolSubject :
                visibleSchoolSubjects) {

            String displayName =
                    ChildSubjectVisibilityPolicy
                            .getSubjectDisplayName(
                                    schoolSubject
                            );

            if (normalizedSelectedName.equals(
                    normalizeText(
                            displayName
                    )
            )) {
                return schoolSubject;
            }
        }

        return null;
    }

    private void showLoadingState(
            boolean loading
    ) {
        binding.progressSubjects.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.contentSubjects.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }

    private boolean isActivityAvailable() {
        return binding != null
                && !isFinishing()
                && !isDestroyed();
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