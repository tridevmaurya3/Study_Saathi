package com.tridev.studysaathi.adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.BookCoverScanActivity;
import com.tridev.studysaathi.BookLearningImportActivity;
import com.tridev.studysaathi.SchoolBookChaptersActivity;
import com.tridev.studysaathi.data.content.setup
        .SchoolBookContentProgressRepository;
import com.tridev.studysaathi.data.content.setup
        .SubjectContentSetupStatus;
import com.tridev.studysaathi.data.local.entity.SchoolBookEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.repository.SchoolBookRepository;
import com.tridev.studysaathi.data.repository.SchoolSubjectRepository;
import com.tridev.studysaathi.databinding
        .ItemSchoolCurriculumSubjectBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SchoolCurriculumSubjectAdapter
        extends RecyclerView.Adapter<
        SchoolCurriculumSubjectAdapter.SubjectViewHolder> {

    @NonNull
    private final List<SchoolSubjectEntity> subjects;

    @NonNull
    private final SubjectActionListener actionListener;

    @Nullable
    private final ComponentActivity hostActivity;

    @Nullable
    private final SchoolSubjectRepository schoolSubjectRepository;

    @Nullable
    private final SchoolBookRepository schoolBookRepository;

    @Nullable
    private final SchoolBookContentProgressRepository contentProgressRepository;

    @Nullable
    private final ActivityResultLauncher<Intent> bookScanLauncher;

    @Nullable
    private final ActivityResultLauncher<Intent> contentImportLauncher;

    private long pendingBookScanSubjectRowId =
            -1L;

    private boolean openingChapterScreen;

    public SchoolCurriculumSubjectAdapter(
            @NonNull SubjectActionListener actionListener
    ) {
        subjects =
                new ArrayList<>();

        this.actionListener =
                actionListener;

        ComponentActivity resolvedHostActivity =
                null;

        SchoolSubjectRepository resolvedSubjectRepository =
                null;

        SchoolBookRepository resolvedBookRepository =
                null;

        SchoolBookContentProgressRepository resolvedProgressRepository =
                null;

        ActivityResultLauncher<Intent> resolvedBookScanLauncher =
                null;

        ActivityResultLauncher<Intent> resolvedContentImportLauncher =
                null;

        if (actionListener instanceof ComponentActivity) {
            resolvedHostActivity =
                    (ComponentActivity) actionListener;

            resolvedSubjectRepository =
                    new SchoolSubjectRepository(
                            resolvedHostActivity
                    );

            resolvedBookRepository =
                    new SchoolBookRepository(
                            resolvedHostActivity
                    );

            resolvedProgressRepository =
                    new SchoolBookContentProgressRepository(
                            resolvedHostActivity
                    );

            resolvedBookScanLauncher =
                    resolvedHostActivity
                            .getActivityResultRegistry()
                            .register(
                                    "school_curriculum_subject_book_scan",
                                    resolvedHostActivity,
                                    new ActivityResultContracts
                                            .StartActivityForResult(),
                                    this::handleBookScanResult
                            );

            resolvedContentImportLauncher =
                    resolvedHostActivity
                            .getActivityResultRegistry()
                            .register(
                                    "school_curriculum_content_import",
                                    resolvedHostActivity,
                                    new ActivityResultContracts
                                            .StartActivityForResult(),
                                    ignored -> notifyDataSetChanged()
                            );
        }

        hostActivity =
                resolvedHostActivity;

        schoolSubjectRepository =
                resolvedSubjectRepository;

        schoolBookRepository =
                resolvedBookRepository;

        contentProgressRepository =
                resolvedProgressRepository;

        bookScanLauncher =
                resolvedBookScanLauncher;

        contentImportLauncher =
                resolvedContentImportLauncher;

        setHasStableIds(
                true
        );
    }

    public void submitList(
            @NonNull List<SchoolSubjectEntity> newSubjects
    ) {
        subjects.clear();

        subjects.addAll(
                newSubjects
        );

        notifyDataSetChanged();
    }

    public void addSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        int existingPosition =
                findSubjectPosition(
                        schoolSubject.getSubjectRowId()
                );

        if (existingPosition >= 0) {
            subjects.set(
                    existingPosition,
                    schoolSubject
            );

            notifyItemChanged(
                    existingPosition
            );

            return;
        }

        subjects.add(
                schoolSubject
        );

        notifyItemInserted(
                subjects.size() - 1
        );
    }

    public void updateSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        int subjectPosition =
                findSubjectPosition(
                        schoolSubject.getSubjectRowId()
                );

        if (subjectPosition < 0) {
            addSubject(
                    schoolSubject
            );

            return;
        }

        subjects.set(
                subjectPosition,
                schoolSubject
        );

        notifyItemChanged(
                subjectPosition
        );
    }

    public void removeSubject(
            long subjectRowId
    ) {
        int subjectPosition =
                findSubjectPosition(
                        subjectRowId
                );

        if (subjectPosition < 0) {
            return;
        }

        subjects.remove(
                subjectPosition
        );

        notifyItemRemoved(
                subjectPosition
        );
    }

    @NonNull
    public List<SchoolSubjectEntity> getCurrentSubjects() {
        return new ArrayList<>(
                subjects
        );
    }

    public int getSelectedSubjectCount() {
        int selectedCount =
                0;

        for (SchoolSubjectEntity schoolSubject : subjects) {
            if (schoolSubject.isEnabled()) {
                selectedCount++;
            }
        }

        return selectedCount;
    }

    public boolean hasSubjects() {
        return !subjects.isEmpty();
    }

    @Override
    public long getItemId(
            int position
    ) {
        SchoolSubjectEntity schoolSubject =
                subjects.get(
                        position
                );

        long subjectRowId =
                schoolSubject.getSubjectRowId();

        if (subjectRowId > 0L) {
            return subjectRowId;
        }

        String subjectId =
                safeText(
                        schoolSubject.getSubjectId()
                );

        return subjectId.isEmpty()
                ? position
                : subjectId.hashCode();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSchoolCurriculumSubjectBinding binding =
                ItemSchoolCurriculumSubjectBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new SubjectViewHolder(
                binding
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull SubjectViewHolder holder,
            int position
    ) {
        holder.bind(
                subjects.get(
                        position
                )
        );
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    private int findSubjectPosition(
            long subjectRowId
    ) {
        if (subjectRowId <= 0L) {
            return -1;
        }

        for (int index = 0;
             index < subjects.size();
             index++) {

            SchoolSubjectEntity schoolSubject =
                    subjects.get(
                            index
                    );

            if (schoolSubject.getSubjectRowId()
                    == subjectRowId) {

                return index;
            }
        }

        return -1;
    }

    /**
     * Selected subject के लिए scanner सीधे खोलता है।
     */
    private void openBookScannerDirectly(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        long subjectRowId =
                schoolSubject.getSubjectRowId();

        ComponentActivity activity =
                hostActivity;

        ActivityResultLauncher<Intent> launcher =
                bookScanLauncher;

        if (subjectRowId <= 0L
                || activity == null
                || launcher == null) {

            actionListener.onScanBookClicked(
                    schoolSubject
            );

            return;
        }

        pendingBookScanSubjectRowId =
                subjectRowId;

        Intent scannerIntent =
                new Intent(
                        activity,
                        BookCoverScanActivity.class
                );

        scannerIntent.putExtra(
                BookCoverScanActivity
                        .EXTRA_TARGET_SUBJECT_ROW_ID,
                subjectRowId
        );

        scannerIntent.putExtra(
                BookCoverScanActivity
                        .EXTRA_TARGET_PROFILE_ID,
                schoolSubject.getProfileId()
        );

        scannerIntent.putExtra(
                BookCoverScanActivity
                        .EXTRA_TARGET_SUBJECT_NAME,
                getSubjectDisplayName(
                        schoolSubject
                )
        );

        try {
            launcher.launch(
                    scannerIntent
            );

        } catch (RuntimeException exception) {
            pendingBookScanSubjectRowId =
                    -1L;

            Toast.makeText(
                    activity,
                    "Book scanner नहीं खुल सका। "
                            + "दोबारा प्रयास करें।",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Subject की primary exact book load करके chapter-management
     * screen खोलता है।
     */
    private void openManageChapters(
            @NonNull SchoolSubjectEntity schoolSubject,
            boolean focusedReview
    ) {
        if (openingChapterScreen) {
            return;
        }

        long subjectRowId =
                schoolSubject.getSubjectRowId();

        ComponentActivity activity =
                hostActivity;

        SchoolBookRepository repository =
                schoolBookRepository;

        if (subjectRowId <= 0L
                || activity == null
                || repository == null) {

            showHostMessage(
                    "Exact school book की पहचान उपलब्ध नहीं है।"
            );

            return;
        }

        openingChapterScreen =
                true;

        repository.getPrimaryBookForSubject(
                subjectRowId,
                new SchoolBookRepository.SingleBookCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable SchoolBookEntity schoolBook
                    ) {
                        openingChapterScreen =
                                false;

                        if (activity.isFinishing()
                                || activity.isDestroyed()) {

                            return;
                        }

                        if (schoolBook == null
                                || schoolBook.getBookRowId() <= 0L) {

                            showHostMessage(
                                    "इस subject की primary exact book "
                                            + "नहीं मिली। पहले book को "
                                            + "दोबारा confirm करें।"
                            );

                            return;
                        }

                        Intent chapterIntent =
                                focusedReview
                                        ? SchoolBookChaptersActivity
                                        .createContentReviewIntent(
                                                activity,
                                                schoolBook.getBookRowId(),
                                                schoolBook.getBookTitle()
                                        )
                                        : SchoolBookChaptersActivity
                                        .createIntent(
                                                activity,
                                                schoolBook.getBookRowId(),
                                                schoolBook.getBookTitle()
                                        );

                        try {
                            activity.startActivity(
                                    chapterIntent
                            );

                        } catch (RuntimeException exception) {
                            showHostMessage(
                                    "Chapter screen नहीं खुल सकी। "
                                            + "दोबारा प्रयास करें।"
                            );
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        openingChapterScreen =
                                false;

                        if (activity.isFinishing()
                                || activity.isDestroyed()) {

                            return;
                        }

                        showHostMessage(
                                getErrorMessage(
                                        exception,
                                        "Exact school book load नहीं हो सकी।"
                                )
                        );
                    }
                }
        );
    }

    /**
     * Scanner या Manual Book Entry के बाद subject card refresh करता है।
     */
    private void handleBookScanResult(
            @NonNull ActivityResult result
    ) {
        long subjectRowId =
                pendingBookScanSubjectRowId;

        pendingBookScanSubjectRowId =
                -1L;

        if (result.getResultCode()
                != Activity.RESULT_OK
                || subjectRowId <= 0L) {

            return;
        }

        ComponentActivity activity =
                hostActivity;

        SchoolSubjectRepository repository =
                schoolSubjectRepository;

        if (activity == null
                || repository == null
                || activity.isFinishing()
                || activity.isDestroyed()) {

            return;
        }

        repository.getSubjectByRowId(
                subjectRowId,
                new SchoolSubjectRepository
                        .SingleSubjectCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable SchoolSubjectEntity schoolSubject
                    ) {
                        if (activity.isFinishing()
                                || activity.isDestroyed()) {

                            return;
                        }

                        if (schoolSubject == null) {
                            Toast.makeText(
                                    activity,
                                    "Saved subject दोबारा "
                                            + "load नहीं हो सका।",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        updateSubject(
                                schoolSubject
                        );

                        activity.setResult(
                                Activity.RESULT_OK
                        );

                        String bookName =
                                safeText(
                                        schoolSubject.getBookName()
                                );

                        String message =
                                bookName.isEmpty()
                                        ? "Exact school book "
                                          + "update हो गई है।"
                                        : bookName
                                          + " subject के साथ "
                                          + "save हो गई है।";

                        Toast.makeText(
                                activity,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (activity.isFinishing()
                                || activity.isDestroyed()) {

                            return;
                        }

                        Toast.makeText(
                                activity,
                                "Book save हुई, लेकिन subject "
                                        + "card refresh नहीं हो सका। "
                                        + "Screen दोबारा खोलें।",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showHostMessage(
            @NonNull String message
    ) {
        ComponentActivity activity =
                hostActivity;

        if (activity == null
                || activity.isFinishing()
                || activity.isDestroyed()) {

            return;
        }

        Toast.makeText(
                activity,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @NonNull
    private static String getSubjectDisplayName(
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
    private static String createSubjectSecondaryText(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String hindiName =
                safeText(
                        schoolSubject.getSubjectNameHindi()
                );

        String subjectCode =
                safeText(
                        schoolSubject.getSubjectCode()
                );

        StringBuilder secondaryText =
                new StringBuilder();

        if (!hindiName.isEmpty()
                && !hindiName.equalsIgnoreCase(
                safeText(
                        schoolSubject
                                .getSubjectNameEnglish()
                )
        )) {
            secondaryText.append(
                    hindiName
            );
        }

        if (!subjectCode.isEmpty()) {
            if (secondaryText.length() > 0) {
                secondaryText.append(
                        "  •  "
                );
            }

            secondaryText.append(
                    subjectCode
            );
        }

        if (secondaryText.length() == 0) {
            secondaryText.append(
                    "School Subject"
            );
        }

        return secondaryText.toString();
    }

    @NonNull
    private static String createSubjectIconText(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String englishName =
                safeText(
                        schoolSubject.getSubjectNameEnglish()
                );

        String hindiName =
                safeText(
                        schoolSubject.getSubjectNameHindi()
                );

        String preferredName =
                !englishName.isEmpty()
                        ? englishName
                        : hindiName;

        if (preferredName.isEmpty()) {
            return "S";
        }

        int firstCodePoint =
                preferredName.codePointAt(
                        0
                );

        return new String(
                Character.toChars(
                        firstCodePoint
                )
        ).toUpperCase(
                Locale.getDefault()
        );
    }

    @NonNull
    private static String createBookDetailsText(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String publisherName =
                safeText(
                        schoolSubject.getPublisherName()
                );

        String bookCode =
                safeText(
                        schoolSubject.getBookCode()
                );

        StringBuilder bookDetails =
                new StringBuilder();

        if (!publisherName.isEmpty()) {
            bookDetails.append(
                    publisherName
            );
        }

        if (!bookCode.isEmpty()) {
            if (bookDetails.length() > 0) {
                bookDetails.append(
                        "  •  "
                );
            }

            bookDetails.append(
                    bookCode
            );
        }

        if (bookDetails.length() == 0) {
            return "Cover या ISBN scan करके actual book जोड़ें";
        }

        return bookDetails.toString();
    }

    @NonNull
    private static String getErrorMessage(
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
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    public interface SubjectActionListener {

        void onSubjectSelected(
                @NonNull SchoolSubjectEntity schoolSubject
        );

        void onScanBookClicked(
                @NonNull SchoolSubjectEntity schoolSubject
        );

        void onEditSubjectClicked(
                @NonNull SchoolSubjectEntity schoolSubject
        );

        void onRemoveSubjectClicked(
                @NonNull SchoolSubjectEntity schoolSubject
        );
    }

    final class SubjectViewHolder
            extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemSchoolCurriculumSubjectBinding binding;

        private long boundSubjectRowId = -1L;

        @Nullable
        private SchoolBookEntity boundPrimaryBook;

        @Nullable
        private SubjectContentSetupStatus.Result boundSetupStatus;

        private SubjectViewHolder(
                @NonNull ItemSchoolCurriculumSubjectBinding binding
        ) {
            super(
                    binding.getRoot()
            );

            this.binding =
                    binding;
        }

        private void bind(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            boundSubjectRowId =
                    schoolSubject.getSubjectRowId();

            boundPrimaryBook = null;
            boundSetupStatus = null;

            String englishSubjectName =
                    safeText(
                            schoolSubject
                                    .getSubjectNameEnglish()
                    );

            String hindiSubjectName =
                    safeText(
                            schoolSubject
                                    .getSubjectNameHindi()
                    );

            String displaySubjectName =
                    !englishSubjectName.isEmpty()
                            ? englishSubjectName
                            : hindiSubjectName;

            if (displaySubjectName.isEmpty()) {
                displaySubjectName =
                        "Unnamed Subject";
            }

            binding.textCurriculumSubjectIcon
                    .setText(
                            createSubjectIconText(
                                    schoolSubject
                            )
                    );

            binding.textCurriculumSubjectName
                    .setText(
                            displaySubjectName
                    );

            binding.textCurriculumSubjectSecondaryName
                    .setText(
                            createSubjectSecondaryText(
                                    schoolSubject
                            )
                    );

            bindSubjectStatus(
                    schoolSubject
            );

            bindBookStatus(
                    schoolSubject
            );

            bindContentSetupStatus(
                    schoolSubject
            );

            loadExactBookProgress(
                    schoolSubject
            );

            binding.cardCurriculumSubjectItem
                    .setOnClickListener(view ->
                            actionListener
                                    .onSubjectSelected(
                                            schoolSubject
                                    )
                    );

            binding.buttonScanCurriculumSubjectBook
                    .setOnClickListener(view ->
                            openBookScannerDirectly(
                                    schoolSubject
                            )
                    );

            binding.buttonManageCurriculumSubjectChapters
                    .setOnClickListener(view ->
                            openContentSetupNextStep(
                                    schoolSubject
                            )
                    );

            binding.buttonEditCurriculumSubject
                    .setOnClickListener(view ->
                            actionListener
                                    .onEditSubjectClicked(
                                            schoolSubject
                                    )
                    );

            binding.buttonRemoveCurriculumSubject
                    .setOnClickListener(view ->
                            actionListener
                                    .onRemoveSubjectClicked(
                                            schoolSubject
                                    )
                    );
        }

        private void bindSubjectStatus(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            boolean subjectEnabled =
                    schoolSubject.isEnabled();

            binding.textCurriculumSubjectStatus
                    .setText(
                            subjectEnabled
                                    ? "Active"
                                    : "Hidden"
                    );

            binding.cardCurriculumSubjectItem
                    .setAlpha(
                            subjectEnabled
                                    ? 1.0f
                                    : 0.65f
                    );
        }

        private void bindBookStatus(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            String bookName =
                    safeText(
                            schoolSubject.getBookName()
                    );

            boolean exactBookAvailable =
                    !bookName.isEmpty();

            if (!exactBookAvailable) {
                binding.textCurriculumSubjectBookName
                        .setText(
                                "Exact school book अभी नहीं जोड़ी गई"
                        );

                binding.textCurriculumSubjectBookDetails
                        .setText(
                                "Cover या ISBN scan करके actual book जोड़ें"
                        );

                binding.textCurriculumSubjectBookStatus
                        .setText(
                                "Pending"
                        );

                binding.buttonScanCurriculumSubjectBook
                        .setText(
                                "Scan Book"
                        );

                binding.buttonManageCurriculumSubjectChapters
                        .setVisibility(
                                View.GONE
                        );

                return;
            }

            binding.textCurriculumSubjectBookName
                    .setText(
                            bookName
                    );

            binding.textCurriculumSubjectBookDetails
                    .setText(
                            createBookDetailsText(
                                    schoolSubject
                            )
                    );

            binding.textCurriculumSubjectBookStatus
                    .setText(
                            "Confirmed"
                    );

            binding.buttonScanCurriculumSubjectBook
                    .setText(
                            "Change Book"
                    );

            binding.buttonManageCurriculumSubjectChapters
                    .setVisibility(
                            View.VISIBLE
                    );
        }

        private void bindContentSetupStatus(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            SubjectContentSetupStatus.Result setupStatus =
                    SubjectContentSetupStatus.resolve(
                            schoolSubject.isEnabled(),
                            schoolSubject.getBookName(),
                            schoolSubject.getChapterCount()
                    );

            applyContentSetupStatus(
                    setupStatus
            );
        }

        private void loadExactBookProgress(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            SchoolBookRepository repository =
                    schoolBookRepository;

            long subjectRowId =
                    schoolSubject.getSubjectRowId();

            if (repository == null
                    || subjectRowId <= 0L
                    || safeText(schoolSubject.getBookName()).isEmpty()) {
                return;
            }

            repository.getPrimaryBookForSubject(
                    subjectRowId,
                    new SchoolBookRepository.SingleBookCallback() {
                        @Override
                        public void onSuccess(
                                @Nullable SchoolBookEntity schoolBook
                        ) {
                            if (boundSubjectRowId != subjectRowId
                                    || schoolBook == null) {
                                return;
                            }

                            boundPrimaryBook = schoolBook;

                            loadActualContentProgress(
                                    schoolSubject,
                                    schoolBook
                            );
                        }

                        @Override
                        public void onError(
                                @NonNull Exception exception
                        ) {
                            // Existing subject summary remains visible.
                        }
                    }
            );
        }

        private void loadActualContentProgress(
                @NonNull SchoolSubjectEntity schoolSubject,
                @NonNull SchoolBookEntity schoolBook
        ) {
            SchoolBookContentProgressRepository repository =
                    contentProgressRepository;

            long subjectRowId = schoolSubject.getSubjectRowId();

            if (repository == null) {
                return;
            }

            repository.getProgress(
                    schoolBook.getBookRowId(),
                    new SchoolBookContentProgressRepository.Callback() {
                        @Override
                        public void onSuccess(
                                int chapterCount,
                                int contentCount
                        ) {
                            if (boundSubjectRowId != subjectRowId) {
                                return;
                            }

                            applyContentSetupStatus(
                                    SubjectContentSetupStatus
                                            .resolveBookProgress(
                                                    schoolSubject.isEnabled(),
                                                    schoolBook.getBookTitle(),
                                                    chapterCount,
                                                    contentCount
                                            )
                            );
                        }

                        @Override
                        public void onError(
                                @NonNull Exception exception
                        ) {
                            // Existing safe summary remains visible.
                        }
                    }
            );
        }

        private void applyContentSetupStatus(
                @NonNull SubjectContentSetupStatus.Result setupStatus
        ) {
            boundSetupStatus = setupStatus;

            binding.textCurriculumSubjectSetupTitle
                    .setText(
                            setupStatus.getTitle()
                    );

            binding.textCurriculumSubjectSetupDescription
                    .setText(
                            setupStatus.getDescription()
                    );

            if (setupStatus.getStep()
                    == SubjectContentSetupStatus.Step.HIDDEN) {
                return;
            }

            boolean exactBookAvailable =
                    !safeText(
                            schoolSubject.getBookName()
                    ).isEmpty();

            if (!exactBookAvailable) {
                binding.buttonScanCurriculumSubjectBook
                        .setText(
                                setupStatus.getPrimaryActionLabel()
                        );

                return;
            }

            binding.buttonManageCurriculumSubjectChapters
                    .setText(
                            setupStatus.getPrimaryActionLabel()
                    );
        }

        private void openContentSetupNextStep(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            SubjectContentSetupStatus.Result setupStatus =
                    boundSetupStatus;

            SchoolBookEntity primaryBook =
                    boundPrimaryBook;

            ComponentActivity activity =
                    hostActivity;

            ActivityResultLauncher<Intent> launcher =
                    contentImportLauncher;

            if (setupStatus == null
                    || setupStatus.getStep()
                    != SubjectContentSetupStatus.Step.ADD_MATERIAL
                    || primaryBook == null
                    || primaryBook.getBookRowId() <= 0L
                    || activity == null
                    || launcher == null) {

                boolean focusedReview =
                        setupStatus != null
                                && setupStatus.getStep()
                                == SubjectContentSetupStatus.Step.REVIEW_CONTENT;

                openManageChapters(schoolSubject, focusedReview);
                return;
            }

            Intent importIntent =
                    BookLearningImportActivity.createIntent(
                            activity,
                            primaryBook.getBookRowId(),
                            primaryBook.getBookTitle()
                    );

            try {
                launcher.launch(importIntent);

            } catch (RuntimeException exception) {
                showHostMessage(
                        "Bulk material setup नहीं खुल सका। "
                                + "Chapter list खोली जा रही है।"
                );

                openManageChapters(schoolSubject, false);
            }
        }
    }
}
