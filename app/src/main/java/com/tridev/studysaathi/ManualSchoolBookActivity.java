package com.tridev.studysaathi;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.content.mapper
        .ManualSchoolBookEntityMapper;
import com.tridev.studysaathi.data.content.validation
        .ManualSchoolBookFormValidator;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolSubjectEntity;
import com.tridev.studysaathi.data.repository
        .SchoolBookRepository;
import com.tridev.studysaathi.data.repository
        .SchoolCurriculumProfileRepository;
import com.tridev.studysaathi.data.repository
        .SchoolSubjectRepository;
import com.tridev.studysaathi.databinding
        .ActivityManualSchoolBookBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ManualSchoolBookActivity
        extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_ROW_ID =
            "extra_subject_row_id";

    public static final String RESULT_BOOK_ROW_ID =
            "result_book_row_id";

    public static final String RESULT_SUBJECT_ROW_ID =
            "result_subject_row_id";

    public static final String RESULT_BOOK_TITLE =
            "result_book_title";

    private static final String STATE_SELECTED_COVER_PATH =
            "state_selected_cover_path";

    private static final String STATE_SELECTED_COVER_SOURCE =
            "state_selected_cover_source";

    private static final String PRIVATE_COVER_FOLDER =
            "book_covers";

    private ActivityManualSchoolBookBinding binding;

    private SchoolSubjectRepository
            schoolSubjectRepository;

    private SchoolCurriculumProfileRepository
            curriculumProfileRepository;

    private SchoolBookRepository
            schoolBookRepository;

    private ActivityResultLauncher<Uri>
            cameraLauncher;

    private ActivityResultLauncher<String[]>
            galleryLauncher;

    private final Handler mainThreadHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final ExecutorService fileExecutor =
            Executors.newSingleThreadExecutor();

    @Nullable
    private SchoolSubjectEntity selectedSubject;

    @Nullable
    private SchoolCurriculumProfileEntity
            curriculumProfile;

    @Nullable
    private File pendingCameraFile;

    @Nullable
    private Uri pendingCameraUri;

    @Nullable
    private File selectedCoverFile;

    @NonNull
    private String selectedCoverSource =
            "";

    private long subjectRowId;

    private boolean operationInProgress;

    private boolean saveAttempted;

    private boolean bookSaved;

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            long subjectRowId
    ) {
        Intent intent =
                new Intent(
                        context,
                        ManualSchoolBookActivity.class
                );

        intent.putExtra(
                EXTRA_SUBJECT_ROW_ID,
                subjectRowId
        );

        return intent;
    }

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivityManualSchoolBookBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        schoolSubjectRepository =
                new SchoolSubjectRepository(
                        this
                );

        curriculumProfileRepository =
                new SchoolCurriculumProfileRepository(
                        this
                );

        schoolBookRepository =
                new SchoolBookRepository(
                        this
                );

        registerActivityResultLaunchers();
        setupToolbar();
        setupBackHandling();
        setupClickListeners();
        setupFormWatchers();

        restoreSavedState(
                savedInstanceState
        );

        subjectRowId =
                getIntent().getLongExtra(
                        EXTRA_SUBJECT_ROW_ID,
                        0L
                );

        if (subjectRowId <= 0L) {
            showInvalidSubjectError();

            return;
        }

        loadSelectedSubject();
    }

    private void registerActivityResultLaunchers() {
        cameraLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .TakePicture(),
                        pictureSaved -> {
                            File cameraFile =
                                    pendingCameraFile;

                            pendingCameraFile =
                                    null;

                            pendingCameraUri =
                                    null;

                            if (!Boolean.TRUE.equals(
                                    pictureSaved
                            )
                                    || cameraFile == null
                                    || !cameraFile.exists()
                                    || cameraFile.length() <= 0L) {

                                deleteFileQuietly(
                                        cameraFile
                                );

                                setCoverOperationState(
                                        false
                                );

                                showMessage(
                                        "Book cover capture नहीं हुआ।"
                                );

                                return;
                            }

                            replaceSelectedCover(
                                    cameraFile,
                                    "Camera"
                            );

                            setCoverOperationState(
                                    false
                            );
                        }
                );

        galleryLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .OpenDocument(),
                        selectedUri -> {
                            if (selectedUri == null) {
                                return;
                            }

                            copyGalleryImageToPrivateStorage(
                                    selectedUri
                            );
                        }
                );
    }

    private void setupToolbar() {
        binding.toolbarManualSchoolBook
                .setNavigationOnClickListener(
                        view ->
                                cancelManualBookCreation()
                );
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(
                                true
                        ) {
                            @Override
                            public void handleOnBackPressed() {
                                cancelManualBookCreation();
                            }
                        }
                );
    }

    private void setupClickListeners() {
        binding.buttonCaptureManualBookCover
                .setOnClickListener(
                        view ->
                                captureBookCover()
                );

        binding.buttonChooseManualBookCover
                .setOnClickListener(
                        view ->
                                chooseBookCoverFromGallery()
                );

        binding.buttonRemoveManualBookCover
                .setOnClickListener(
                        view ->
                                removeSelectedCover()
                );

        binding.buttonSaveManualBook
                .setOnClickListener(
                        view ->
                                saveManualBook()
                );

        binding.buttonCancelManualBook
                .setOnClickListener(
                        view ->
                                cancelManualBookCreation()
                );
    }

    private void setupFormWatchers() {
        addFormWatcher(
                binding.inputManualBookTitle
        );

        addFormWatcher(
                binding.inputManualBookSubtitle
        );

        addFormWatcher(
                binding.inputManualBookAuthor
        );

        addFormWatcher(
                binding.inputManualBookPublisher
        );

        addFormWatcher(
                binding.inputManualBookEdition
        );

        addFormWatcher(
                binding.inputManualBookPublicationYear
        );

        addFormWatcher(
                binding.inputManualBookIsbn
        );

        addFormWatcher(
                binding.inputManualBookCode
        );

        binding.switchManualBookPrimary
                .setOnCheckedChangeListener(
                        (buttonView, isChecked) ->
                                refreshFormState(
                                        saveAttempted
                                )
                );

        binding.switchManualBookAiTutor
                .setOnCheckedChangeListener(
                        (buttonView, isChecked) ->
                                refreshFormState(
                                        saveAttempted
                                )
                );

        refreshFormState(
                false
        );
    }

    private void addFormWatcher(
            @NonNull EditText editText
    ) {
        editText.addTextChangedListener(
                new SimpleTextWatcher() {
                    @Override
                    public void afterTextChanged(
                            @NonNull String text
                    ) {
                        refreshFormState(
                                saveAttempted
                        );
                    }
                }
        );
    }

    private void restoreSavedState(
            @Nullable Bundle savedInstanceState
    ) {
        if (savedInstanceState == null) {
            return;
        }

        String restoredCoverPath =
                safeText(
                        savedInstanceState.getString(
                                STATE_SELECTED_COVER_PATH
                        )
                );

        selectedCoverSource =
                safeText(
                        savedInstanceState.getString(
                                STATE_SELECTED_COVER_SOURCE
                        )
                );

        if (restoredCoverPath.isEmpty()) {
            return;
        }

        File restoredCover =
                new File(
                        restoredCoverPath
                );

        if (restoredCover.exists()
                && restoredCover.length() > 0L) {

            selectedCoverFile =
                    restoredCover;

            displaySelectedCover();
        }
    }

    private void loadSelectedSubject() {
        setOperationState(
                true,
                "School subject load किया जा रहा है"
        );

        schoolSubjectRepository
                .getSubjectByRowId(
                        subjectRowId,
                        new SchoolSubjectRepository
                                .SingleSubjectCallback() {

                            @Override
                            public void onSuccess(
                                    @Nullable
                                    SchoolSubjectEntity
                                            schoolSubject
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                if (schoolSubject == null) {
                                    setOperationState(
                                            false,
                                            ""
                                    );

                                    showInvalidSubjectError();

                                    return;
                                }

                                selectedSubject =
                                        schoolSubject;

                                displaySelectedSubject(
                                        schoolSubject
                                );

                                loadCurriculumProfile(
                                        schoolSubject
                                                .getProfileId()
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

                                showErrorDialog(
                                        "Subject load नहीं हुआ",
                                        "Selected school subject database से load नहीं हो सका।"
                                );
                            }
                        }
                );
    }

    private void loadCurriculumProfile(
            long profileId
    ) {
        curriculumProfileRepository
                .getCurriculumProfile(
                        profileId,
                        new SchoolCurriculumProfileRepository
                                .SingleProfileCallback() {

                            @Override
                            public void onSuccess(
                                    @Nullable
                                    SchoolCurriculumProfileEntity
                                            profile
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                if (profile == null) {
                                    setOperationState(
                                            false,
                                            ""
                                    );

                                    showErrorDialog(
                                            "Curriculum उपलब्ध नहीं है",
                                            "Manual school book जोड़ने से पहले school curriculum setup पूरा करें।"
                                    );

                                    return;
                                }

                                curriculumProfile =
                                        profile;

                                displayCurriculumDetails(
                                        profile
                                );

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

                                setOperationState(
                                        false,
                                        ""
                                );

                                showErrorDialog(
                                        "Curriculum load नहीं हुआ",
                                        "Student का school curriculum profile load नहीं हो सका।"
                                );
                            }
                        }
                );
    }

    private void displaySelectedSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String englishName =
                safeText(
                        schoolSubject
                                .getSubjectNameEnglish()
                );

        String hindiName =
                safeText(
                        schoolSubject
                                .getSubjectNameHindi()
                );

        String displayName =
                !englishName.isEmpty()
                        ? englishName
                        : hindiName;

        binding.textManualBookSubjectName
                .setText(
                        valueOrFallback(
                                displayName,
                                "School Subject"
                        )
                );

        String subjectCode =
                safeText(
                        schoolSubject.getSubjectCode()
                );

        if (!hindiName.isEmpty()
                && !hindiName.equalsIgnoreCase(
                displayName
        )) {

            binding.textManualBookSubjectDetails
                    .setText(
                            hindiName
                                    + (subjectCode.isEmpty()
                                    ? ""
                                    : "  •  " + subjectCode)
                    );

        } else if (!subjectCode.isEmpty()) {
            binding.textManualBookSubjectDetails
                    .setText(
                            subjectCode
                    );
        }
    }

    private void displayCurriculumDetails(
            @NonNull SchoolCurriculumProfileEntity profile
    ) {
        StringBuilder details =
                new StringBuilder();

        appendDetail(
                details,
                profile.getClassNumber() > 0
                        ? "Class "
                          + profile.getClassNumber()
                        : ""
        );

        appendDetail(
                details,
                profile.getEducationBoard()
        );

        appendDetail(
                details,
                profile.getStudyMedium()
        );

        appendDetail(
                details,
                profile.getAcademicSession()
        );

        if (details.length() > 0) {
            binding.textManualBookSubjectDetails
                    .setText(
                            details.toString()
                    );
        }
    }

    private void captureBookCover() {
        if (operationInProgress) {
            return;
        }

        try {
            File cameraFile =
                    createPrivateCoverFile(
                            "camera"
                    );

            Uri cameraUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            cameraFile
                    );

            pendingCameraFile =
                    cameraFile;

            pendingCameraUri =
                    cameraUri;

            setCoverOperationState(
                    true
            );

            cameraLauncher.launch(
                    cameraUri
            );

        } catch (RuntimeException
                 | IOException exception) {

            deleteFileQuietly(
                    pendingCameraFile
            );

            pendingCameraFile =
                    null;

            pendingCameraUri =
                    null;

            setCoverOperationState(
                    false
            );

            showMessage(
                    "Camera cover file तैयार नहीं हो सकी।"
            );
        }
    }

    private void chooseBookCoverFromGallery() {
        if (operationInProgress) {
            return;
        }

        galleryLauncher.launch(
                new String[]{
                        "image/*"
                }
        );
    }

    private void copyGalleryImageToPrivateStorage(
            @NonNull Uri sourceUri
    ) {
        setCoverOperationState(
                true
        );

        fileExecutor.execute(() -> {
            File destinationFile =
                    null;

            try {
                destinationFile =
                        createPrivateCoverFile(
                                determineGalleryExtension(
                                        sourceUri
                                )
                        );

                copyUriToFile(
                        sourceUri,
                        destinationFile
                );

                File finalDestinationFile =
                        destinationFile;

                mainThreadHandler.post(() -> {
                    if (!isActivityAvailable()) {
                        return;
                    }

                    replaceSelectedCover(
                            finalDestinationFile,
                            createGallerySourceLabel(
                                    sourceUri
                            )
                    );

                    setCoverOperationState(
                            false
                    );
                });

            } catch (Exception exception) {
                deleteFileQuietly(
                        destinationFile
                );

                mainThreadHandler.post(() -> {
                    if (!isActivityAvailable()) {
                        return;
                    }

                    setCoverOperationState(
                            false
                    );

                    showMessage(
                            "Gallery image private storage में copy नहीं हो सकी।"
                    );
                });
            }
        });
    }

    @NonNull
    private File createPrivateCoverFile(
            @NonNull String sourceOrExtension
    ) throws IOException {
        File coverDirectory =
                new File(
                        getFilesDir(),
                        PRIVATE_COVER_FOLDER
                );

        if (!coverDirectory.exists()
                && !coverDirectory.mkdirs()) {

            throw new IOException(
                    "Book cover directory could not be created."
            );
        }

        String extension =
                sourceOrExtension
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .contains(
                                "png"
                        )
                        ? ".png"
                        : ".jpg";

        File coverFile =
                new File(
                        coverDirectory,
                        "manual_book_cover_"
                                + System.currentTimeMillis()
                                + "_"
                                + Math.abs(
                                System.nanoTime()
                        )
                                + extension
                );

        if (!coverFile.createNewFile()) {
            throw new IOException(
                    "Book cover file could not be created."
            );
        }

        return coverFile;
    }

    private void copyUriToFile(
            @NonNull Uri sourceUri,
            @NonNull File destinationFile
    ) throws IOException {
        try (
                InputStream inputStream =
                        getContentResolver()
                                .openInputStream(
                                        sourceUri
                                );

                FileOutputStream outputStream =
                        new FileOutputStream(
                                destinationFile
                        )
        ) {
            if (inputStream == null) {
                throw new IOException(
                        "Selected image could not be opened."
                );
            }

            byte[] buffer =
                    new byte[8192];

            int bytesRead;

            while ((bytesRead =
                    inputStream.read(
                            buffer
                    )) != -1) {

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            outputStream.flush();
        }

        if (!destinationFile.exists()
                || destinationFile.length() <= 0L) {

            throw new IOException(
                    "Copied image file is empty."
            );
        }
    }

    @NonNull
    private String determineGalleryExtension(
            @NonNull Uri sourceUri
    ) {
        String mimeType =
                safeText(
                        getContentResolver()
                                .getType(
                                        sourceUri
                                )
                );

        if (mimeType.equalsIgnoreCase(
                "image/png"
        )) {
            return "png";
        }

        return "jpg";
    }

    @NonNull
    private String createGallerySourceLabel(
            @NonNull Uri sourceUri
    ) {
        String displayName =
                "";

        try (
                android.database.Cursor cursor =
                        getContentResolver()
                                .query(
                                        sourceUri,
                                        new String[]{
                                                OpenableColumns
                                                        .DISPLAY_NAME
                                        },
                                        null,
                                        null,
                                        null
                                )
        ) {
            if (cursor != null
                    && cursor.moveToFirst()) {

                int nameColumn =
                        cursor.getColumnIndex(
                                OpenableColumns
                                        .DISPLAY_NAME
                        );

                if (nameColumn >= 0) {
                    displayName =
                            safeText(
                                    cursor.getString(
                                            nameColumn
                                    )
                            );
                }
            }

        } catch (RuntimeException ignored) {
            /*
             * Display name optional है।
             */
        }

        return displayName.isEmpty()
                ? "Gallery"
                : "Gallery • " + displayName;
    }

    private void replaceSelectedCover(
            @NonNull File newCoverFile,
            @NonNull String coverSource
    ) {
        File oldCoverFile =
                selectedCoverFile;

        selectedCoverFile =
                newCoverFile;

        selectedCoverSource =
                coverSource;

        if (oldCoverFile != null
                && !oldCoverFile.equals(
                newCoverFile
        )) {
            deleteFileQuietly(
                    oldCoverFile
            );
        }

        displaySelectedCover();

        refreshFormState(
                saveAttempted
        );
    }

    private void displaySelectedCover() {
        File coverFile =
                selectedCoverFile;

        boolean coverAvailable =
                coverFile != null
                        && coverFile.exists()
                        && coverFile.length() > 0L;

        binding.imageManualBookCover
                .setVisibility(
                        coverAvailable
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.containerManualBookCoverPlaceholder
                .setVisibility(
                        coverAvailable
                                ? View.GONE
                                : View.VISIBLE
                );

        binding.buttonRemoveManualBookCover
                .setVisibility(
                        coverAvailable
                                ? View.VISIBLE
                                : View.GONE
                );

        if (!coverAvailable) {
            binding.imageManualBookCover
                    .setImageDrawable(
                            null
                    );

            binding.textManualBookCoverSource
                    .setText(
                            "No cover selected"
                    );

            return;
        }

        binding.imageManualBookCover
                .setImageURI(
                        Uri.fromFile(
                                coverFile
                        )
                );

        binding.textManualBookCoverSource
                .setText(
                        valueOrFallback(
                                selectedCoverSource,
                                "Private book cover"
                        )
                );
    }

    private void removeSelectedCover() {
        if (operationInProgress) {
            return;
        }

        deleteFileQuietly(
                selectedCoverFile
        );

        selectedCoverFile =
                null;

        selectedCoverSource =
                "";

        displaySelectedCover();

        refreshFormState(
                saveAttempted
        );
    }

    private void saveManualBook() {
        if (operationInProgress) {
            return;
        }

        saveAttempted =
                true;

        ManualSchoolBookFormValidator.ValidationResult
                validationResult =
                createValidationResult();

        applyValidationErrors(
                validationResult
        );

        refreshFormState(
                true
        );

        if (!validationResult.isValid()) {
            showValidationError(
                    validationResult
            );

            return;
        }

        SchoolSubjectEntity subject =
                selectedSubject;

        SchoolCurriculumProfileEntity profile =
                curriculumProfile;

        if (subject == null
                || profile == null) {

            showMessage(
                    "Subject और curriculum details अभी तैयार नहीं हैं।"
            );

            return;
        }

        setOperationState(
                true,
                "Book details तैयार की जा रही हैं"
        );

        schoolBookRepository
                .getNextSortOrder(
                        subject.getSubjectRowId(),
                        new SchoolBookRepository
                                .SortOrderCallback() {

                            @Override
                            public void onSuccess(
                                    int nextSortOrder
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                createAndInsertBook(
                                        validationResult,
                                        subject,
                                        profile,
                                        nextSortOrder
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

                                showMessage(
                                        "Book का sort order तैयार नहीं हो सका।"
                                );
                            }
                        }
                );
    }

    private void createAndInsertBook(
            @NonNull ManualSchoolBookFormValidator
                    .ValidationResult validationResult,
            @NonNull SchoolSubjectEntity subject,
            @NonNull SchoolCurriculumProfileEntity profile,
            int nextSortOrder
    ) {
        final SchoolBookEntity schoolBook;

        try {
            schoolBook =
                    ManualSchoolBookEntityMapper
                            .createEntity(
                                    validationResult,
                                    subject,
                                    profile,
                                    nextSortOrder
                            );

        } catch (RuntimeException exception) {
            setOperationState(
                    false,
                    ""
            );

            showMessage(
                    "Manual book details database format में तैयार नहीं हो सकीं।"
            );

            return;
        }

        setOperationState(
                true,
                "Exact school book save की जा रही है"
        );

        schoolBookRepository
                .insertBook(
                        schoolBook,
                        new SchoolBookRepository
                                .InsertBookCallback() {

                            @Override
                            public void onSuccess(
                                    long insertedBookRowId
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                schoolBook.setBookRowId(
                                        insertedBookRowId
                                );

                                updateSubjectBookSummary(
                                        subject,
                                        schoolBook
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

                                showErrorDialog(
                                        "Book save नहीं हुई",
                                        "संभव है कि यही exact school book इस subject में पहले से मौजूद हो। Book title, publisher, edition, ISBN या book code जाँचें।"
                                );
                            }
                        }
                );
    }

    private void updateSubjectBookSummary(
            @NonNull SchoolSubjectEntity subject,
            @NonNull SchoolBookEntity schoolBook
    ) {
        schoolSubjectRepository
                .updateSubjectBookInformation(
                        subject.getSubjectRowId(),
                        schoolBook.getBookTitle(),
                        schoolBook.getBookCode(),
                        schoolBook.getPublisherName(),
                        new SchoolSubjectRepository
                                .OperationCallback() {

                            @Override
                            public void onSuccess() {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                finishSuccessfulSave(
                                        schoolBook,
                                        false
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                /*
                                 * Book entity save हो चुकी है। केवल subject
                                 * summary update fail हुई है, इसलिए save
                                 * को rollback नहीं किया जाएगा।
                                 */
                                finishSuccessfulSave(
                                        schoolBook,
                                        true
                                );
                            }
                        }
                );
    }

    private void finishSuccessfulSave(
            @NonNull SchoolBookEntity schoolBook,
            boolean subjectSummaryWarning
    ) {
        bookSaved =
                true;

        saveAttempted =
                false;

        setOperationState(
                false,
                ""
        );

        Intent resultIntent =
                new Intent();

        resultIntent.putExtra(
                RESULT_BOOK_ROW_ID,
                schoolBook.getBookRowId()
        );

        resultIntent.putExtra(
                RESULT_SUBJECT_ROW_ID,
                schoolBook.getSubjectRowId()
        );

        resultIntent.putExtra(
                RESULT_BOOK_TITLE,
                schoolBook.getBookTitle()
        );

        setResult(
                RESULT_OK,
                resultIntent
        );

        StringBuilder message =
                new StringBuilder();

        message.append(
                schoolBook.getBookTitle()
        );

        message.append(
                " को इस subject की confirmed school book के रूप में save कर दिया गया है।"
        );

        if (subjectSummaryWarning) {
            message.append(
                    "\n\nSubject card की book summary अभी update नहीं हो सकी। Screen दोबारा खोलने पर database से refresh की जाएगी।"
            );
        }

        message.append(
                "\n\nअगले चरण में इस book के Contents/Index pages scan करके exact chapters जोड़े जाएँगे।"
        );

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "School book save हो गई"
                )
                .setMessage(
                        message.toString()
                )
                .setCancelable(
                        false
                )
                .setPositiveButton(
                        "ठीक है",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    @NonNull
    private ManualSchoolBookFormValidator
            .ValidationResult createValidationResult() {
        String coverPath =
                selectedCoverFile == null
                        ? ""
                        : selectedCoverFile
                        .getAbsolutePath();

        return ManualSchoolBookFormValidator
                .validate(
                        subjectRowId,
                        binding.inputManualBookTitle
                                .getText(),
                        binding.inputManualBookSubtitle
                                .getText(),
                        binding.inputManualBookAuthor
                                .getText(),
                        binding.inputManualBookPublisher
                                .getText(),
                        binding.inputManualBookEdition
                                .getText(),
                        binding.inputManualBookPublicationYear
                                .getText(),
                        binding.inputManualBookIsbn
                                .getText(),
                        binding.inputManualBookCode
                                .getText(),
                        coverPath,
                        binding.switchManualBookPrimary
                                .isChecked(),
                        binding.switchManualBookAiTutor
                                .isChecked()
                );
    }

    private void refreshFormState(
            boolean showErrors
    ) {
        if (binding == null) {
            return;
        }

        ManualSchoolBookFormValidator.ValidationResult
                validationResult =
                createValidationResult();

        if (showErrors) {
            applyValidationErrors(
                    validationResult
            );

        } else {
            clearValidationErrors();
        }

        boolean dataReady =
                selectedSubject != null
                        && curriculumProfile != null;

        binding.buttonSaveManualBook
                .setEnabled(
                        !operationInProgress
                                && dataReady
                                && validationResult
                                .isValid()
                );
    }

    private void applyValidationErrors(
            @NonNull ManualSchoolBookFormValidator
                    .ValidationResult validationResult
    ) {
        binding.layoutManualBookTitle
                .setError(
                        emptyToNull(
                                validationResult
                                        .getBookTitleError()
                        )
                );

        binding.layoutManualBookPublicationYear
                .setError(
                        emptyToNull(
                                validationResult
                                        .getPublicationYearError()
                        )
                );

        binding.layoutManualBookIsbn
                .setError(
                        emptyToNull(
                                validationResult
                                        .getIsbnError()
                        )
                );

        if (validationResult.hasGeneralErrors()) {
            binding.textManualBookFormError
                    .setText(
                            validationResult
                                    .getGeneralErrors()
                                    .get(
                                            0
                                    )
                    );

            binding.textManualBookFormError
                    .setVisibility(
                            View.VISIBLE
                    );

        } else {
            binding.textManualBookFormError
                    .setText(
                            ""
                    );

            binding.textManualBookFormError
                    .setVisibility(
                            View.GONE
                    );
        }
    }

    private void clearValidationErrors() {
        binding.layoutManualBookTitle
                .setError(
                        null
                );

        binding.layoutManualBookPublicationYear
                .setError(
                        null
                );

        binding.layoutManualBookIsbn
                .setError(
                        null
                );

        binding.textManualBookFormError
                .setText(
                        ""
                );

        binding.textManualBookFormError
                .setVisibility(
                        View.GONE
                );
    }

    private void showValidationError(
            @NonNull ManualSchoolBookFormValidator
                    .ValidationResult validationResult
    ) {
        if (!validationResult
                .getBookTitleError()
                .isEmpty()) {

            requestInputFocus(
                    binding.inputManualBookTitle
            );

            showMessage(
                    validationResult
                            .getBookTitleError()
            );

            return;
        }

        if (!validationResult
                .getPublicationYearError()
                .isEmpty()) {

            requestInputFocus(
                    binding.inputManualBookPublicationYear
            );

            showMessage(
                    validationResult
                            .getPublicationYearError()
            );

            return;
        }

        if (!validationResult
                .getIsbnError()
                .isEmpty()) {

            requestInputFocus(
                    binding.inputManualBookIsbn
            );

            showMessage(
                    validationResult
                            .getIsbnError()
            );

            return;
        }

        if (validationResult.hasGeneralErrors()) {
            showMessage(
                    validationResult
                            .getGeneralErrors()
                            .get(
                                    0
                            )
            );
        }
    }

    private void requestInputFocus(
            @NonNull EditText input
    ) {
        input.requestFocus();

        binding.scrollManualSchoolBook
                .post(() ->
                        binding.scrollManualSchoolBook
                                .smoothScrollTo(
                                        0,
                                        Math.max(
                                                0,
                                                input.getTop()
                                                        - 80
                                        )
                                )
                );
    }

    private void setOperationState(
            boolean inProgress,
            @Nullable String loadingMessage
    ) {
        operationInProgress =
                inProgress;

        binding.containerManualBookLoading
                .setVisibility(
                        inProgress
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.buttonCaptureManualBookCover
                .setEnabled(
                        !inProgress
                );

        binding.buttonChooseManualBookCover
                .setEnabled(
                        !inProgress
                );

        binding.buttonRemoveManualBookCover
                .setEnabled(
                        !inProgress
                );

        binding.buttonCancelManualBook
                .setEnabled(
                        !inProgress
                );

        String safeLoadingMessage =
                safeText(
                        loadingMessage
                );

        if (!safeLoadingMessage.isEmpty()) {
            binding.textManualBookLoadingMessage
                    .setText(
                            safeLoadingMessage
                    );
        }

        refreshFormState(
                saveAttempted
        );
    }

    private void setCoverOperationState(
            boolean inProgress
    ) {
        binding.progressManualBookCover
                .setVisibility(
                        inProgress
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.buttonCaptureManualBookCover
                .setEnabled(
                        !inProgress
                                && !operationInProgress
                );

        binding.buttonChooseManualBookCover
                .setEnabled(
                        !inProgress
                                && !operationInProgress
                );

        binding.buttonRemoveManualBookCover
                .setEnabled(
                        !inProgress
                                && !operationInProgress
                );
    }

    private void cancelManualBookCreation() {
        if (operationInProgress) {
            showMessage(
                    "Book save operation पूरी होने तक प्रतीक्षा करें।"
            );

            return;
        }

        boolean hasEnteredData =
                !safeText(
                        binding.inputManualBookTitle
                                .getText()
                ).isEmpty()
                        || !safeText(
                        binding.inputManualBookPublisher
                                .getText()
                ).isEmpty()
                        || selectedCoverFile != null;

        if (!hasEnteredData) {
            finish();

            return;
        }

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "Manual book setup छोड़ें?"
                )
                .setMessage(
                        "भरी गई unsaved book details और selected cover हटा दिए जाएँगे।"
                )
                .setNegativeButton(
                        "वापस जाएँ",
                        null
                )
                .setPositiveButton(
                        "Discard",
                        (dialog, which) -> {
                            if (!bookSaved) {
                                deleteFileQuietly(
                                        selectedCoverFile
                                );
                            }

                            selectedCoverFile =
                                    null;

                            finish();
                        }
                )
                .show();
    }

    private void showInvalidSubjectError() {
        showErrorDialog(
                "Valid subject उपलब्ध नहीं है",
                "Manual school book जोड़ने के लिए पहले Parent Curriculum Setup में actual school subject जोड़ें।"
        );
    }

    private void showErrorDialog(
            @NonNull String title,
            @NonNull String message
    ) {
        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        title
                )
                .setMessage(
                        message
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

    private void showMessage(
            @NonNull String message
    ) {
        Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void appendDetail(
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

    private void deleteFileQuietly(
            @Nullable File file
    ) {
        if (file == null
                || !file.exists()) {
            return;
        }

        try {
            file.delete();

        } catch (SecurityException ignored) {
            /*
             * Cleanup failure app flow को नहीं रोकेगी।
             */
        }
    }

    private boolean isActivityAvailable() {
        return binding != null
                && !isFinishing()
                && !isDestroyed();
    }

    @Nullable
    private String emptyToNull(
            @Nullable Object value
    ) {
        String safeValue =
                safeText(
                        value
                );

        return safeValue.isEmpty()
                ? null
                : safeValue;
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
    private String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString().trim();
    }

    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {
        super.onSaveInstanceState(
                outState
        );

        outState.putString(
                STATE_SELECTED_COVER_PATH,
                selectedCoverFile == null
                        ? ""
                        : selectedCoverFile
                        .getAbsolutePath()
        );

        outState.putString(
                STATE_SELECTED_COVER_SOURCE,
                selectedCoverSource
        );
    }

    @Override
    protected void onDestroy() {
        deleteFileQuietly(
                pendingCameraFile
        );

        pendingCameraFile =
                null;

        pendingCameraUri =
                null;

        fileExecutor.shutdown();

        binding =
                null;

        super.onDestroy();
    }

    private abstract static class SimpleTextWatcher
            implements android.text.TextWatcher {

        @Override
        public final void beforeTextChanged(
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
        public final void onTextChanged(
                CharSequence text,
                int start,
                int before,
                int count
        ) {
            /*
             * No action required.
             */
        }

        @Override
        public final void afterTextChanged(
                android.text.Editable editable
        ) {
            afterTextChanged(
                    editable == null
                            ? ""
                            : editable.toString()
            );
        }

        public abstract void afterTextChanged(
                @NonNull String text
        );
    }
}