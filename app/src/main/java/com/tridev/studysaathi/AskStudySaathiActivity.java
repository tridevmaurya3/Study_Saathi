package com.tridev.studysaathi;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tridev.studysaathi.data.ai.FirebaseStudyTutorClient;
import com.tridev.studysaathi.data.ai.QuestionImageBitmapLoader;
import com.tridev.studysaathi.data.ai.SmartTutorAnswerResult;
import com.tridev.studysaathi.data.learning.StudentKnowledgeGraphStore;
import com.tridev.studysaathi.data.catalog.DoubtAssistantEngine;
import com.tridev.studysaathi.data.catalog.LessonCatalog;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.ChildSchoolBookChapterRepository;
import com.tridev.studysaathi.data.repository.DoubtHistoryRepository;
import com.tridev.studysaathi.data.repository.SchoolSubjectRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityAskStudySaathiBinding;
import com.tridev.studysaathi.model.LessonContent;
import com.tridev.studysaathi.ui.SmartAiCompanionController;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AskStudySaathiActivity
        extends AppCompatActivity {

    public static final String EXTRA_PREFILL_SUBJECT =
            "extra_prefill_subject";

    public static final String EXTRA_PREFILL_CHAPTER =
            "extra_prefill_chapter";

    public static final String EXTRA_PREFILL_QUESTION =
            "extra_prefill_question";

    private static final String LOG_TAG =
            "AskStudySaathi";

    private static final String OPTIONAL_CHAPTER_LABEL =
            "Chapter नहीं चुना — Optional";

    private static final String GENERAL_QUESTION_CHAPTER_TITLE =
            "General Question";

    private static final String STATE_SELECTED_QUESTION_IMAGE_URI =
            "state_selected_question_image_uri";

    private static final String STATE_SELECTED_QUESTION_IMAGE_PATH =
            "state_selected_question_image_path";

    private static final String STATE_PENDING_CAMERA_IMAGE_URI =
            "state_pending_camera_image_uri";

    private static final String STATE_PENDING_CAMERA_IMAGE_PATH =
            "state_pending_camera_image_path";

    private static final String STATE_LAST_IMAGE_OCR_TEXT =
            "state_last_image_ocr_text";

    private static final String QUESTION_IMAGE_DIRECTORY =
            "book_cover_cache/question_images";

    private static final int MAXIMUM_PREVIEW_IMAGE_SIZE =
            1400;

    private ActivityAskStudySaathiBinding binding;

    private StudentProfileRepository studentProfileRepository;

    private DoubtHistoryRepository doubtHistoryRepository;

    private SchoolSubjectRepository schoolSubjectRepository;

    private ChildSchoolBookChapterRepository
            childSchoolBookChapterRepository;

    private FirebaseStudyTutorClient firebaseStudyTutorClient;

    private QuestionImageBitmapLoader questionImageBitmapLoader;

    private ActivityResultLauncher<Intent>
            voiceQuestionLauncher;

    private ActivityResultLauncher<String[]>
            questionGalleryLauncher;

    private ActivityResultLauncher<Uri>
            questionCameraLauncher;

    @Nullable
    private StudentProfileEntity activeStudentProfile;

    @Nullable
    private SchoolSubjectEntity selectedSchoolSubject;

    @Nullable
    private SchoolBookChapterEntity selectedChapter;

    @Nullable
    private Uri selectedQuestionImageUri;

    @Nullable
    private String selectedQuestionImagePrivatePath;

    @Nullable
    private Uri pendingCameraImageUri;

    @Nullable
    private String pendingCameraImagePath;

    @Nullable
    private TextRecognizer questionLatinRecognizer;

    @Nullable
    private TextRecognizer questionDevanagariRecognizer;

    @NonNull
    private final List<SchoolSubjectEntity> schoolSubjects =
            new ArrayList<>();

    @NonNull
    private final List<String> subjectDisplayNames =
            new ArrayList<>();

    @NonNull
    private final List<SchoolBookChapterEntity> chapterItems =
            new ArrayList<>();

    private String selectedSubjectName =
            "";

    private String prefillSubjectName =
            "";

    private String prefillChapterTitle =
            "";

    private String prefillQuestion =
            "";

    private String requestedInputMode =
            "";

    private String lastQuestionImageOcrText =
            "";

    private long latestChapterRequestSubjectRowId =
            0L;

    private int questionImageOcrGeneration;

    private int aiAnswerRequestGeneration;

    private boolean prefilledQuestionApplied;
    private boolean requestedInputModeApplied;

    private boolean questionImageOcrInProgress;

    private boolean restartImageOcrAfterSubjectLoad;

    private boolean aiAnswerRequestInProgress;
    private boolean completedTurnArchived;

    @NonNull
    private String lastCompletedQuestion =
            "";

    @Nullable
    private CharSequence lastCompletedAnswer;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivityAskStudySaathiBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        studentProfileRepository =
                new StudentProfileRepository(
                        this
                );

        doubtHistoryRepository =
                new DoubtHistoryRepository(
                        this
                );

        schoolSubjectRepository =
                new SchoolSubjectRepository(
                        this
                );

        childSchoolBookChapterRepository =
                new ChildSchoolBookChapterRepository(
                        this
                );

        firebaseStudyTutorClient =
                new FirebaseStudyTutorClient(
                        this
                );

        questionImageBitmapLoader =
                new QuestionImageBitmapLoader(
                        this
                );

        registerVoiceQuestionLauncher();
        registerQuestionImageLaunchers();
        readPrefillArguments();
        prepareInitialScreenState();
        setupVoiceQuestionInput();
        setupClickListeners();
        restoreQuestionImageState(
                savedInstanceState
        );
        loadActiveStudentProfile();
    }

    private void registerVoiceQuestionLauncher() {
        voiceQuestionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .StartActivityForResult(),
                        result -> {
                            if (!isActivityAvailable()) {
                                return;
                            }

                            if (result.getResultCode()
                                    != Activity.RESULT_OK) {

                                binding.inputQuestion
                                        .setHelperText(
                                                null
                                        );

                                return;
                            }

                            Intent resultData =
                                    result.getData();

                            if (resultData == null) {
                                showVoiceResultMissing();
                                return;
                            }

                            ArrayList<String> speechResults =
                                    resultData
                                            .getStringArrayListExtra(
                                                    RecognizerIntent
                                                            .EXTRA_RESULTS
                                            );

                            String spokenQuestion =
                                    getFirstValidSpeechResult(
                                            speechResults
                                    );

                            if (spokenQuestion.isEmpty()) {
                                showVoiceResultMissing();
                                return;
                            }

                            applySpokenQuestion(
                                    spokenQuestion
                            );
                        }
                );
    }

    private void registerQuestionImageLaunchers() {
        questionGalleryLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.OpenDocument(),
                        this::handleSelectedGalleryQuestionImage
                );

        questionCameraLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.TakePicture(),
                        this::handleCameraQuestionImageResult
                );
    }

    private void setupVoiceQuestionInput() {
        binding.inputQuestion.setEndIconMode(
                TextInputLayout.END_ICON_CUSTOM
        );

        binding.inputQuestion.setEndIconDrawable(
                android.R.drawable.ic_btn_speak_now
        );

        binding.inputQuestion.setEndIconContentDescription(
                "बोलकर सवाल पूछें"
        );

        binding.inputQuestion.setEndIconOnClickListener(
                view -> startVoiceQuestionInput()
        );

        binding.inputQuestion.setEndIconVisible(
                false
        );
    }

    private void startVoiceQuestionInput() {
        if (aiAnswerRequestInProgress) {
            Snackbar.make(
                    binding.getRoot(),
                    "पहले वर्तमान Smart AI answer पूरा होने दें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (activeStudentProfile == null) {
            Snackbar.make(
                    binding.getRoot(),
                    "पहले Student Profile चुनें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (selectedSchoolSubject == null
                || selectedSubjectName.isEmpty()) {

            Snackbar.make(
                    binding.getRoot(),
                    "पहले अपना Subject चुनें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        hideKeyboard();

        Intent speechIntent =
                new Intent(
                        RecognizerIntent
                                .ACTION_RECOGNIZE_SPEECH
                );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                selectedSubjectName
                        + " का सवाल बोलें"
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
        );

        String languageTag =
                resolveVoiceLanguageTag();

        if (!languageTag.isEmpty()) {
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    languageTag
            );

            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    languageTag
            );
        }

        binding.inputQuestion.setError(
                null
        );

        binding.inputQuestion.setHelperText(
                "अपना सवाल साफ और धीरे बोलें।"
        );

        try {
            voiceQuestionLauncher.launch(
                    speechIntent
            );

        } catch (ActivityNotFoundException exception) {
            binding.inputQuestion.setHelperText(
                    null
            );

            Snackbar.make(
                    binding.getRoot(),
                    "इस फोन में Voice Recognition सेवा उपलब्ध नहीं है।",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (RuntimeException exception) {
            binding.inputQuestion.setHelperText(
                    null
            );

            Snackbar.make(
                    binding.getRoot(),
                    "Voice input शुरू नहीं हो सका।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @NonNull
    private String resolveVoiceLanguageTag() {
        QuestionLanguageMode languageMode =
                resolveQuestionLanguageMode();

        if (languageMode
                == QuestionLanguageMode.LATIN_PREFERRED) {

            return "en-IN";
        }

        return "hi-IN";
    }

    @NonNull
    private String getFirstValidSpeechResult(
            @Nullable List<String> speechResults
    ) {
        if (speechResults == null
                || speechResults.isEmpty()) {

            return "";
        }

        for (String speechResult :
                speechResults) {

            String safeResult =
                    safeText(
                            speechResult
                    );

            if (!safeResult.isEmpty()) {
                return safeResult;
            }
        }

        return "";
    }

    private void applySpokenQuestion(
            @NonNull String spokenQuestion
    ) {
        String currentQuestion =
                getCurrentQuestionText();

        String finalQuestion;

        if (currentQuestion.isEmpty()) {
            finalQuestion =
                    spokenQuestion;

        } else {
            finalQuestion =
                    currentQuestion
                            + "\n"
                            + spokenQuestion;
        }

        setQuestionText(
                finalQuestion
        );

        binding.inputQuestion.setError(
                null
        );

        binding.inputQuestion.setHelperText(
                "Voice से आया सवाल जाँचें। जरूरत हो तो Text सुधारकर Ask दबाएँ।"
        );

        Snackbar.make(
                binding.getRoot(),
                "आपका बोला हुआ सवाल लिख दिया गया है।",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void showVoiceResultMissing() {
        binding.inputQuestion.setHelperText(
                null
        );

        Snackbar.make(
                binding.getRoot(),
                "आवाज स्पष्ट नहीं मिली। दोबारा बोलें या Text लिखें।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void readPrefillArguments() {
        prefillSubjectName =
                getSafeExtra(
                        EXTRA_PREFILL_SUBJECT
                );

        prefillChapterTitle =
                getSafeExtra(
                        EXTRA_PREFILL_CHAPTER
                );

        prefillQuestion =
                getSafeExtra(
                        EXTRA_PREFILL_QUESTION
                );

        requestedInputMode =
                getSafeExtra(
                        SmartAiCompanionController.EXTRA_OPEN_INPUT_MODE
                );
    }

    @NonNull
    private String getSafeExtra(
            @NonNull String key
    ) {
        String value =
                getIntent()
                        .getStringExtra(
                                key
                        );

        return safeText(
                value
        );
    }

    private void prepareInitialScreenState() {
        binding.dropdownAskSubject.setEnabled(
                false
        );

        binding.dropdownAskChapter.setEnabled(
                false
        );

        binding.editQuestion.setEnabled(
                false
        );

        binding.buttonAskSaathi.setEnabled(
                false
        );

        binding.buttonDoubtHistory.setEnabled(
                false
        );

        binding.buttonDoubtHistory.setVisibility(
                View.GONE
        );

        binding.buttonQuickExplain.setEnabled(
                false
        );

        binding.buttonQuickKeyPoints.setEnabled(
                false
        );

        binding.buttonQuickExample.setEnabled(
                false
        );

        binding.buttonQuickPractice.setEnabled(
                false
        );

        binding.buttonQuestionCamera.setEnabled(
                false
        );

        binding.buttonQuestionGallery.setEnabled(
                false
        );

        binding.buttonRemoveQuestionImage.setEnabled(
                false
        );

        binding.buttonOpenLesson.setVisibility(
                View.GONE
        );

        binding.cardAnswer.setVisibility(
                View.GONE
        );

        binding.cardQuestionImagePreview.setVisibility(
                View.GONE
        );

        binding.progressQuestionImageOcr.setVisibility(
                View.GONE
        );

        binding.inputQuestion.setEndIconVisible(
                false
        );

        setOptionalChapterDropdown(
                new ArrayList<>(),
                ""
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonDoubtHistory.setOnClickListener(view ->
                openDoubtHistory()
        );

        binding.buttonQuestionCamera.setOnClickListener(view ->
                openQuestionCamera()
        );

        binding.buttonQuestionGallery.setOnClickListener(view ->
                openQuestionGallery()
        );

        binding.buttonRemoveQuestionImage.setOnClickListener(view ->
                removeSelectedQuestionImage()
        );

        binding.buttonAskSaathi.setOnClickListener(view ->
                submitQuestion(
                        getCurrentQuestionText()
                )
        );

        binding.buttonSendFollowUp.setOnClickListener(view ->
                submitFollowUpQuestion()
        );

        binding.editFollowUpQuestion
                .setOnEditorActionListener(
                        (textView, actionId, event) -> {
                            if (actionId
                                    != EditorInfo.IME_ACTION_SEND) {

                                return false;
                            }

                            submitFollowUpQuestion();
                            return true;
                        }
                );

        binding.buttonQuickExplain.setOnClickListener(view ->
                submitQuickQuestion(
                        getString(
                                R.string.quick_question_explain
                        )
                )
        );

        binding.buttonQuickKeyPoints.setOnClickListener(view ->
                submitQuickQuestion(
                        getString(
                                R.string.quick_question_key_points
                        )
                )
        );

        binding.buttonQuickExample.setOnClickListener(view ->
                submitQuickQuestion(
                        getString(
                                R.string.quick_question_example
                        )
                )
        );

        binding.buttonQuickPractice.setOnClickListener(view ->
                submitQuickQuestion(
                        getString(
                                R.string.quick_question_practice
                        )
                )
        );

        binding.buttonOpenLesson.setOnClickListener(view ->
                openSelectedLesson()
        );
    }

    private void openQuestionCamera() {
        if (!isQuestionImageInputAvailable()) {
            return;
        }

        hideKeyboard();

        try {
            File imageFile =
                    createQuestionCameraFile();

            pendingCameraImagePath =
                    imageFile.getAbsolutePath();

            pendingCameraImageUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            imageFile
                    );

            questionCameraLauncher.launch(
                    pendingCameraImageUri
            );

        } catch (IOException exception) {
            clearPendingCameraImage(
                    true
            );

            Snackbar.make(
                    binding.getRoot(),
                    "Camera photo के लिए temporary file नहीं बन सकी।",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (IllegalArgumentException exception) {
            clearPendingCameraImage(
                    true
            );

            Snackbar.make(
                    binding.getRoot(),
                    "Camera photo provider तैयार नहीं हो सका।",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (ActivityNotFoundException exception) {
            clearPendingCameraImage(
                    true
            );

            Snackbar.make(
                    binding.getRoot(),
                    "इस device में Camera app उपलब्ध नहीं है।",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (RuntimeException exception) {
            clearPendingCameraImage(
                    true
            );

            Snackbar.make(
                    binding.getRoot(),
                    "Camera शुरू नहीं हो सका।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private void openQuestionGallery() {
        if (!isQuestionImageInputAvailable()) {
            return;
        }

        hideKeyboard();

        try {
            questionGalleryLauncher.launch(
                    new String[]{
                            "image/*"
                    }
            );

        } catch (ActivityNotFoundException exception) {
            Snackbar.make(
                    binding.getRoot(),
                    "Gallery या file picker उपलब्ध नहीं है।",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (RuntimeException exception) {
            Snackbar.make(
                    binding.getRoot(),
                    "Gallery नहीं खुल सकी।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private boolean isQuestionImageInputAvailable() {
        if (aiAnswerRequestInProgress) {
            Snackbar.make(
                    binding.getRoot(),
                    "पहले वर्तमान Smart AI answer पूरा होने दें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return false;
        }

        if (activeStudentProfile == null) {
            Snackbar.make(
                    binding.getRoot(),
                    "पहले Student Profile चुनें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return false;
        }

        if (selectedSchoolSubject == null
                || selectedSubjectName.isEmpty()) {

            Snackbar.make(
                    binding.getRoot(),
                    "पहले अपना Subject चुनें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return false;
        }

        if (questionImageOcrInProgress) {
            Snackbar.make(
                    binding.getRoot(),
                    "पहली फोटो अभी पढ़ी जा रही है।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    @NonNull
    private File createQuestionCameraFile()
            throws IOException {

        File imageDirectory =
                new File(
                        getCacheDir(),
                        QUESTION_IMAGE_DIRECTORY
                );

        if (!imageDirectory.exists()
                && !imageDirectory.mkdirs()) {

            throw new IOException(
                    "Question image directory could not be created."
            );
        }

        return File.createTempFile(
                "question_",
                ".jpg",
                imageDirectory
        );
    }

    private void handleCameraQuestionImageResult(
            boolean captured
    ) {
        if (!isActivityAvailable()) {
            return;
        }

        if (!captured
                || pendingCameraImageUri == null) {

            clearPendingCameraImage(
                    true
            );

            Snackbar.make(
                    binding.getRoot(),
                    "Camera photo नहीं ली गई।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        questionImageBitmapLoader.cancelCurrentLoad();

        removeOldInternalQuestionImage();

        selectedQuestionImageUri =
                pendingCameraImageUri;

        selectedQuestionImagePrivatePath =
                safeText(
                        pendingCameraImagePath
                );

        pendingCameraImageUri =
                null;

        pendingCameraImagePath =
                null;

        displaySelectedQuestionImage();

        startQuestionImageOcr(
                selectedQuestionImageUri
        );
    }

    private void handleSelectedGalleryQuestionImage(
            @Nullable Uri imageUri
    ) {
        if (!isActivityAvailable()) {
            return;
        }

        if (imageUri == null) {
            Snackbar.make(
                    binding.getRoot(),
                    "Gallery से कोई फोटो नहीं चुनी गई।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        try {
            getContentResolver()
                    .takePersistableUriPermission(
                            imageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

        } catch (SecurityException ignored) {
            // Current activity session में URI उपयोग हो सकती है।
        }

        questionImageBitmapLoader.cancelCurrentLoad();

        removeOldInternalQuestionImage();

        selectedQuestionImageUri =
                imageUri;

        selectedQuestionImagePrivatePath =
                null;

        displaySelectedQuestionImage();

        startQuestionImageOcr(
                imageUri
        );
    }

    private void displaySelectedQuestionImage() {
        if (selectedQuestionImageUri == null) {
            binding.cardQuestionImagePreview.setVisibility(
                    View.GONE
            );

            binding.imageQuestionPreview.setImageDrawable(
                    null
            );

            return;
        }

        binding.cardQuestionImagePreview.setVisibility(
                View.VISIBLE
        );

        binding.buttonRemoveQuestionImage.setEnabled(
                !questionImageOcrInProgress
                        && !aiAnswerRequestInProgress
        );

        binding.textQuestionImageStatus.setText(
                "फोटो तैयार है। सवाल पढ़ना शुरू किया जा रहा है।"
        );

        Bitmap previewBitmap =
                decodePreviewBitmap(
                        selectedQuestionImageUri
                );

        binding.imageQuestionPreview.setImageDrawable(
                null
        );

        if (previewBitmap != null) {
            binding.imageQuestionPreview.setImageBitmap(
                    previewBitmap
            );

        } else {
            binding.imageQuestionPreview.setImageURI(
                    selectedQuestionImageUri
            );
        }

        binding.askSaathiScrollView.post(() ->
                binding.askSaathiScrollView.smoothScrollTo(
                        0,
                        binding.cardQuestionImagePreview
                                .getBottom()
                )
        );
    }

    @Nullable
    private Bitmap decodePreviewBitmap(
            @NonNull Uri imageUri
    ) {
        BitmapFactory.Options boundsOptions =
                new BitmapFactory.Options();

        boundsOptions.inJustDecodeBounds =
                true;

        try (ParcelFileDescriptor descriptor =
                     getContentResolver()
                             .openFileDescriptor(
                                     imageUri,
                                     "r"
                             )) {

            if (descriptor == null) {
                return null;
            }

            BitmapFactory.decodeFileDescriptor(
                    descriptor.getFileDescriptor(),
                    null,
                    boundsOptions
            );

        } catch (IOException
                 | RuntimeException exception) {

            return null;
        }

        if (boundsOptions.outWidth <= 0
                || boundsOptions.outHeight <= 0) {

            return null;
        }

        int sampleSize =
                calculatePreviewSampleSize(
                        boundsOptions.outWidth,
                        boundsOptions.outHeight
                );

        BitmapFactory.Options bitmapOptions =
                new BitmapFactory.Options();

        bitmapOptions.inSampleSize =
                sampleSize;

        bitmapOptions.inPreferredConfig =
                Bitmap.Config.RGB_565;

        try (ParcelFileDescriptor descriptor =
                     getContentResolver()
                             .openFileDescriptor(
                                     imageUri,
                                     "r"
                             )) {

            if (descriptor == null) {
                return null;
            }

            return BitmapFactory.decodeFileDescriptor(
                    descriptor.getFileDescriptor(),
                    null,
                    bitmapOptions
            );

        } catch (IOException
                 | RuntimeException exception) {

            return null;
        }
    }

    private int calculatePreviewSampleSize(
            int imageWidth,
            int imageHeight
    ) {
        int sampleSize =
                1;

        while ((imageWidth / sampleSize)
                > MAXIMUM_PREVIEW_IMAGE_SIZE
                || (imageHeight / sampleSize)
                > MAXIMUM_PREVIEW_IMAGE_SIZE) {

            sampleSize *=
                    2;
        }

        return Math.max(
                1,
                sampleSize
        );
    }

    private void startQuestionImageOcr(
            @NonNull Uri imageUri
    ) {
        if (selectedSchoolSubject == null) {
            restartImageOcrAfterSubjectLoad =
                    true;

            return;
        }

        questionImageOcrGeneration++;

        int operationGeneration =
                questionImageOcrGeneration;

        closeQuestionTextRecognizers();

        setQuestionImageOcrState(
                true
        );

        final InputImage inputImage;

        try {
            inputImage =
                    InputImage.fromFilePath(
                            this,
                            imageUri
                    );

        } catch (IOException
                 | RuntimeException exception) {

            finishQuestionImageOcrWithError(
                    operationGeneration,
                    "फोटो पढ़ने योग्य format में नहीं है।"
            );

            return;
        }

        questionLatinRecognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                );

        questionDevanagariRecognizer =
                TextRecognition.getClient(
                        new DevanagariTextRecognizerOptions
                                .Builder()
                                .build()
                );

        recognizeLatinQuestionText(
                inputImage,
                operationGeneration
        );
    }

    private void recognizeLatinQuestionText(
            @NonNull InputImage inputImage,
            int operationGeneration
    ) {
        TextRecognizer recognizer =
                questionLatinRecognizer;

        if (recognizer == null) {
            recognizeDevanagariQuestionText(
                    inputImage,
                    "",
                    new IllegalStateException(
                            "Latin recognizer unavailable."
                    ),
                    operationGeneration
            );

            return;
        }

        recognizer.process(
                        inputImage
                )
                .addOnSuccessListener(text ->
                        recognizeDevanagariQuestionText(
                                inputImage,
                                safeText(
                                        text == null
                                                ? null
                                                : text.getText()
                                ),
                                null,
                                operationGeneration
                        )
                )
                .addOnFailureListener(exception ->
                        recognizeDevanagariQuestionText(
                                inputImage,
                                "",
                                exception,
                                operationGeneration
                        )
                );
    }

    private void recognizeDevanagariQuestionText(
            @NonNull InputImage inputImage,
            @NonNull String latinText,
            @Nullable Exception latinFailure,
            int operationGeneration
    ) {
        if (!isCurrentQuestionImageOcr(
                operationGeneration
        )) {
            return;
        }

        TextRecognizer recognizer =
                questionDevanagariRecognizer;

        if (recognizer == null) {
            if (!latinText.isEmpty()) {
                completeQuestionImageOcr(
                        latinText,
                        "",
                        operationGeneration
                );

            } else {
                finishQuestionImageOcrWithError(
                        operationGeneration,
                        "Question OCR engine उपलब्ध नहीं है।"
                );
            }

            return;
        }

        recognizer.process(
                        inputImage
                )
                .addOnSuccessListener(text -> {
                    String devanagariText =
                            safeText(
                                    text == null
                                            ? null
                                            : text.getText()
                            );

                    completeQuestionImageOcr(
                            latinText,
                            devanagariText,
                            operationGeneration
                    );
                })
                .addOnFailureListener(exception -> {
                    if (!latinText.isEmpty()) {
                        completeQuestionImageOcr(
                                latinText,
                                "",
                                operationGeneration
                        );

                        return;
                    }

                    String errorMessage =
                            latinFailure == null
                                    ? "फोटो में पढ़ने योग्य सवाल नहीं मिला।"
                                    : "Latin और Devanagari दोनों OCR text नहीं पढ़ सके।";

                    finishQuestionImageOcrWithError(
                            operationGeneration,
                            errorMessage
                    );
                });
    }

    private void completeQuestionImageOcr(
            @NonNull String latinText,
            @NonNull String devanagariText,
            int operationGeneration
    ) {
        if (!isCurrentQuestionImageOcr(
                operationGeneration
        )) {
            return;
        }

        QuestionOcrSelection selection =
                selectBestQuestionOcrText(
                        latinText,
                        devanagariText
                );

        closeQuestionTextRecognizers();

        questionImageOcrInProgress =
                false;

        updateQuestionImageButtonsEnabledState();

        binding.progressQuestionImageOcr.setVisibility(
                View.GONE
        );

        if (selection.getSelectedText()
                .isEmpty()) {

            binding.textQuestionImageStatus.setText(
                    "फोटो मिल गई, लेकिन सवाल साफ नहीं पढ़ा जा सका। "
                            + "फोटो crop करके दोबारा लें या सवाल manually लिखें।"
            );

            Snackbar.make(
                    binding.getRoot(),
                    "फोटो में पढ़ने योग्य सवाल नहीं मिला।",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * Full-page OCR is retained only as private image context. It must
         * never replace or append to the child's targeted typed question.
         */
        lastQuestionImageOcrText =
                safeText(selection.getSelectedText());

        binding.textQuestionImageStatus.setText(
                selection.createStatusMessage()
        );

        binding.inputQuestion.setError(
                null
        );

        binding.inputQuestion.setHelperText(
                "अब केवल लक्ष्य लिखें, जैसे: सवाल नं. 3 हल करो। पूरा page text यहाँ नहीं जोड़ा जाएगा।"
        );

        Snackbar.make(
                binding.getRoot(),
                "Photo तैयार है। केवल जिस question का answer चाहिए वह लिखें।",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void finishQuestionImageOcrWithError(
            int operationGeneration,
            @NonNull String errorMessage
    ) {
        if (!isCurrentQuestionImageOcr(
                operationGeneration
        )) {
            return;
        }

        closeQuestionTextRecognizers();

        questionImageOcrInProgress =
                false;

        updateQuestionImageButtonsEnabledState();

        binding.progressQuestionImageOcr.setVisibility(
                View.GONE
        );

        binding.textQuestionImageStatus.setText(
                errorMessage
                        + " साफ photo लें या सवाल manually लिखें।"
        );

        Snackbar.make(
                binding.getRoot(),
                errorMessage,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private boolean isCurrentQuestionImageOcr(
            int operationGeneration
    ) {
        return isActivityAvailable()
                && questionImageOcrInProgress
                && operationGeneration
                == questionImageOcrGeneration;
    }

    private void setQuestionImageOcrState(
            boolean processing
    ) {
        questionImageOcrInProgress =
                processing;

        binding.progressQuestionImageOcr.setVisibility(
                processing
                        ? View.VISIBLE
                        : View.GONE
        );

        if (processing) {
            binding.textQuestionImageStatus.setText(
                    createQuestionImageProcessingMessage()
            );
        }

        updateQuestionImageButtonsEnabledState();
    }

    @NonNull
    private String createQuestionImageProcessingMessage() {
        QuestionLanguageMode languageMode =
                resolveQuestionLanguageMode();

        switch (languageMode) {
            case DEVANAGARI_SANSKRIT:
                return "संस्कृत सवाल को Devanagari OCR से पढ़ा जा रहा है।";

            case DEVANAGARI_HINDI:
                return "हिन्दी सवाल को Devanagari OCR से पढ़ा जा रहा है।";

            case LATIN_PREFERRED:
                return "English text को पढ़ा जा रहा है।";

            case MIXED:
            default:
                return "फोटो से सवाल पढ़ा जा रहा है।";
        }
    }

    private void updateQuestionImageButtonsEnabledState() {
        boolean controlsAvailable =
                activeStudentProfile != null
                        && selectedSchoolSubject != null
                        && !aiAnswerRequestInProgress;

        binding.buttonQuestionCamera.setEnabled(
                controlsAvailable
                        && !questionImageOcrInProgress
        );

        binding.buttonQuestionGallery.setEnabled(
                controlsAvailable
                        && !questionImageOcrInProgress
        );

        binding.buttonRemoveQuestionImage.setEnabled(
                selectedQuestionImageUri != null
                        && !questionImageOcrInProgress
                        && !aiAnswerRequestInProgress
        );
    }

    @NonNull
    private QuestionOcrSelection selectBestQuestionOcrText(
            @NonNull String rawLatinText,
            @NonNull String rawDevanagariText
    ) {
        QuestionLanguageMode languageMode =
                resolveQuestionLanguageMode();

        String latinText =
                filterTextForLatinScript(
                        rawLatinText
                );

        String devanagariText =
                filterTextForDevanagariScript(
                        rawDevanagariText
                );

        String selectedText;

        String sourceLabel;

        boolean fallbackUsed =
                false;

        switch (languageMode) {
            case DEVANAGARI_SANSKRIT:
                if (!devanagariText.isEmpty()) {
                    selectedText =
                            devanagariText;

                    sourceLabel =
                            "Sanskrit Devanagari OCR";

                } else {
                    selectedText =
                            latinText;

                    sourceLabel =
                            "Latin fallback";

                    fallbackUsed =
                            true;
                }

                break;

            case DEVANAGARI_HINDI:
                if (!devanagariText.isEmpty()) {
                    selectedText =
                            devanagariText;

                    sourceLabel =
                            "Hindi Devanagari OCR";

                } else {
                    selectedText =
                            latinText;

                    sourceLabel =
                            "Latin fallback";

                    fallbackUsed =
                            true;
                }

                break;

            case LATIN_PREFERRED:
                if (!latinText.isEmpty()) {
                    selectedText =
                            latinText;

                    sourceLabel =
                            "English OCR";

                } else {
                    selectedText =
                            devanagariText;

                    sourceLabel =
                            "Devanagari fallback";

                    fallbackUsed =
                            true;
                }

                break;

            case MIXED:
            default:
                selectedText =
                        mergeMixedQuestionText(
                                latinText,
                                devanagariText
                        );

                sourceLabel =
                        "Mixed-language OCR";

                break;
        }

        int qualityEstimate =
                estimateQuestionOcrQuality(
                        selectedText,
                        languageMode
                );

        boolean manualReviewRequired =
                fallbackUsed
                        || qualityEstimate < 78
                        || languageMode
                        == QuestionLanguageMode
                        .DEVANAGARI_SANSKRIT;

        return new QuestionOcrSelection(
                selectedText,
                sourceLabel,
                qualityEstimate,
                manualReviewRequired,
                fallbackUsed,
                languageMode
        );
    }

    @NonNull
    private QuestionLanguageMode resolveQuestionLanguageMode() {
        String subjectText =
                normalizeText(
                        selectedSubjectName
                                + " "
                                + (
                                selectedSchoolSubject == null
                                        ? ""
                                        : selectedSchoolSubject
                                        .getSubjectNameHindi()
                        )
                );

        if (subjectText.contains(
                "sanskrit"
        )
                || subjectText.contains(
                "संस्कृत"
        )) {

            return QuestionLanguageMode
                    .DEVANAGARI_SANSKRIT;
        }

        if (subjectText.contains(
                "hindi"
        )
                || subjectText.contains(
                "हिंदी"
        )
                || subjectText.contains(
                "हिन्दी"
        )) {

            return QuestionLanguageMode
                    .DEVANAGARI_HINDI;
        }

        if (subjectText.contains(
                "english"
        )
                || subjectText.contains(
                "अंग्रेज"
        )) {

            return QuestionLanguageMode
                    .LATIN_PREFERRED;
        }

        if (activeStudentProfile != null) {
            String explanationLanguage =
                    normalizeText(
                            activeStudentProfile
                                    .getExplanationLanguage()
                    );

            if (explanationLanguage.contains(
                    "english"
            )
                    && !explanationLanguage.contains(
                    "hindi"
            )
                    && !explanationLanguage.contains(
                    "हिंदी"
            )
                    && !explanationLanguage.contains(
                    "bilingual"
            )) {

                return QuestionLanguageMode
                        .LATIN_PREFERRED;
            }
        }

        return QuestionLanguageMode.MIXED;
    }

    @NonNull
    private String filterTextForDevanagariScript(
            @Nullable String text
    ) {
        String safeValue =
                safeText(
                        text
                );

        if (safeValue.isEmpty()) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        String[] lines =
                safeValue.split(
                        "\\R"
                );

        for (String rawLine :
                lines) {

            String line =
                    normalizeOcrLine(
                            rawLine
                    );

            if (line.isEmpty()) {
                continue;
            }

            int devanagariCharacters =
                    countDevanagariCharacters(
                            line
                    );

            int latinCharacters =
                    countLatinCharacters(
                            line
                    );

            boolean numericOrMathematicalLine =
                    containsNumberOrMathSymbol(
                            line
                    );

            boolean keepLine =
                    devanagariCharacters > 0
                            || (
                            latinCharacters == 0
                                    && numericOrMathematicalLine
                    );

            if (!keepLine) {
                continue;
            }

            appendUniqueLine(
                    result,
                    line
            );
        }

        return result.toString()
                .trim();
    }

    @NonNull
    private String filterTextForLatinScript(
            @Nullable String text
    ) {
        String safeValue =
                safeText(
                        text
                );

        if (safeValue.isEmpty()) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        String[] lines =
                safeValue.split(
                        "\\R"
                );

        for (String rawLine :
                lines) {

            String line =
                    normalizeOcrLine(
                            rawLine
                    );

            if (line.isEmpty()) {
                continue;
            }

            int latinCharacters =
                    countLatinCharacters(
                            line
                    );

            int devanagariCharacters =
                    countDevanagariCharacters(
                            line
                    );

            boolean numericOrMathematicalLine =
                    containsNumberOrMathSymbol(
                            line
                    );

            boolean keepLine =
                    latinCharacters > 0
                            || (
                            devanagariCharacters == 0
                                    && numericOrMathematicalLine
                    );

            if (!keepLine) {
                continue;
            }

            appendUniqueLine(
                    result,
                    line
            );
        }

        return result.toString()
                .trim();
    }

    @NonNull
    private String mergeMixedQuestionText(
            @NonNull String latinText,
            @NonNull String devanagariText
    ) {
        Map<String, String> uniqueLines =
                new LinkedHashMap<>();

        addTextLinesToMap(
                uniqueLines,
                latinText
        );

        addTextLinesToMap(
                uniqueLines,
                devanagariText
        );

        StringBuilder result =
                new StringBuilder();

        for (String line :
                uniqueLines.values()) {

            if (result.length() > 0) {
                result.append(
                        '\n'
                );
            }

            result.append(
                    line
            );
        }

        return result.toString()
                .trim();
    }

    private void addTextLinesToMap(
            @NonNull Map<String, String> target,
            @NonNull String text
    ) {
        if (text.isEmpty()) {
            return;
        }

        String[] lines =
                text.split(
                        "\\R"
                );

        for (String rawLine :
                lines) {

            String line =
                    normalizeOcrLine(
                            rawLine
                    );

            if (line.isEmpty()) {
                continue;
            }

            String key =
                    createOcrComparisonKey(
                            line
                    );

            if (key.isEmpty()
                    || target.containsKey(
                    key
            )) {

                continue;
            }

            target.put(
                    key,
                    line
            );
        }
    }

    private void appendUniqueLine(
            @NonNull StringBuilder builder,
            @NonNull String line
    ) {
        String comparisonLine =
                createOcrComparisonKey(
                        line
                );

        if (!comparisonLine.isEmpty()) {
            String[] existingLines =
                    builder.toString()
                            .split(
                                    "\\R"
                            );

            for (String existingLine :
                    existingLines) {

                if (comparisonLine.equals(
                        createOcrComparisonKey(
                                existingLine
                        )
                )) {
                    return;
                }
            }
        }

        if (builder.length() > 0) {
            builder.append(
                    '\n'
            );
        }

        builder.append(
                line
        );
    }

    @NonNull
    private String normalizeOcrLine(
            @Nullable String line
    ) {
        return safeText(
                line
        )
                .replaceAll(
                        "[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]",
                        " "
                )
                .replaceAll(
                        "[ \\t]+",
                        " "
                )
                .trim();
    }

    @NonNull
    private String createOcrComparisonKey(
            @Nullable String value
    ) {
        return safeText(
                value
        )
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        ""
                )
                .trim();
    }

    private int estimateQuestionOcrQuality(
            @NonNull String selectedText,
            @NonNull QuestionLanguageMode languageMode
    ) {
        String safeValue =
                safeText(
                        selectedText
                );

        if (safeValue.isEmpty()) {
            return 0;
        }

        int totalLetters =
                countLetterCharacters(
                        safeValue
                );

        int devanagariCharacters =
                countDevanagariCharacters(
                        safeValue
                );

        int latinCharacters =
                countLatinCharacters(
                        safeValue
                );

        int meaningfulCharacters =
                countMeaningfulCharacters(
                        safeValue
                );

        int lineCount =
                Math.max(
                        1,
                        safeValue.split(
                                "\\R"
                        ).length
                );

        int score =
                25;

        score +=
                Math.min(
                        25,
                        meaningfulCharacters / 3
                );

        score +=
                Math.min(
                        10,
                        lineCount * 2
                );

        if (totalLetters > 0) {
            switch (languageMode) {
                case DEVANAGARI_SANSKRIT:
                case DEVANAGARI_HINDI:
                    score +=
                            Math.round(
                                    35f
                                            * devanagariCharacters
                                            / totalLetters
                            );
                    break;

                case LATIN_PREFERRED:
                    score +=
                            Math.round(
                                    35f
                                            * latinCharacters
                                            / totalLetters
                            );
                    break;

                case MIXED:
                default:
                    score +=
                            Math.min(
                                    35,
                                    (
                                            devanagariCharacters
                                                    + latinCharacters
                                    )
                                            * 35
                                            / totalLetters
                            );
                    break;
            }
        }

        if (meaningfulCharacters < 5) {
            score -=
                    25;
        }

        if (languageMode
                == QuestionLanguageMode.DEVANAGARI_SANSKRIT
                && devanagariCharacters < 3) {

            score -=
                    30;
        }

        return Math.max(
                0,
                Math.min(
                        100,
                        score
                )
        );
    }

    private int countDevanagariCharacters(
            @NonNull String value
    ) {
        int count =
                0;

        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(
                            index
                    );

            if ((character >= '\u0900'
                    && character <= '\u097F')
                    || (character >= '\uA8E0'
                    && character <= '\uA8FF')) {

                count++;
            }
        }

        return count;
    }

    private int countLatinCharacters(
            @NonNull String value
    ) {
        int count =
                0;

        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(
                            index
                    );

            if ((character >= 'A'
                    && character <= 'Z')
                    || (character >= 'a'
                    && character <= 'z')) {

                count++;
            }
        }

        return count;
    }

    private int countLetterCharacters(
            @NonNull String value
    ) {
        int count =
                0;

        for (int index = 0;
             index < value.length();
             index++) {

            if (Character.isLetter(
                    value.charAt(
                            index
                    )
            )) {
                count++;
            }
        }

        return count;
    }

    private int countMeaningfulCharacters(
            @NonNull String value
    ) {
        int count =
                0;

        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(
                            index
                    );

            if (Character.isLetterOrDigit(
                    character
            )
                    || isMathSymbol(
                    character
            )) {

                count++;
            }
        }

        return count;
    }

    private boolean containsNumberOrMathSymbol(
            @NonNull String value
    ) {
        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(
                            index
                    );

            if (Character.isDigit(
                    character
            )
                    || isMathSymbol(
                    character
            )) {

                return true;
            }
        }

        return false;
    }

    private boolean isMathSymbol(
            char character
    ) {
        return character == '+'
                || character == '-'
                || character == '='
                || character == '×'
                || character == '÷'
                || character == '/'
                || character == '%'
                || character == '<'
                || character == '>'
                || character == '√'
                || character == '^'
                || character == 'π'
                || character == '°';
    }

    private void applyQuestionImageOcrText(
            @NonNull String recognizedQuestion
    ) {
        lastQuestionImageOcrText =
                safeText(recognizedQuestion);
    }

    private void removeSelectedQuestionImage() {
        if (aiAnswerRequestInProgress) {
            return;
        }

        questionImageBitmapLoader.cancelCurrentLoad();

        questionImageOcrGeneration++;

        closeQuestionTextRecognizers();

        questionImageOcrInProgress =
                false;

        binding.progressQuestionImageOcr.setVisibility(
                View.GONE
        );

        removeOldInternalQuestionImage();

        selectedQuestionImageUri =
                null;

        selectedQuestionImagePrivatePath =
                null;

        lastQuestionImageOcrText =
                "";

        binding.imageQuestionPreview.setImageDrawable(
                null
        );

        binding.cardQuestionImagePreview.setVisibility(
                View.GONE
        );

        binding.textQuestionImageStatus.setText(
                "फोटो चुनी गई है। केवल question number या लक्ष्य लिखें; पूरा page text copy नहीं होगा।"
        );

        updateQuestionImageButtonsEnabledState();

        Snackbar.make(
                binding.getRoot(),
                "Question photo हटा दी गई है। आपका typed सवाल सुरक्षित है।",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void removeOldInternalQuestionImage() {
        String imagePath =
                safeText(
                        selectedQuestionImagePrivatePath
                );

        if (imagePath.isEmpty()) {
            return;
        }

        File imageFile =
                new File(
                        imagePath
                );

        if (imageFile.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            imageFile.delete();
        }
    }

    private void clearPendingCameraImage(
            boolean deleteFile
    ) {
        if (deleteFile) {
            String path =
                    safeText(
                            pendingCameraImagePath
                    );

            if (!path.isEmpty()) {
                File imageFile =
                        new File(
                                path
                        );

                if (imageFile.isFile()) {
                    //noinspection ResultOfMethodCallIgnored
                    imageFile.delete();
                }
            }
        }

        pendingCameraImageUri =
                null;

        pendingCameraImagePath =
                null;
    }

    private void restoreQuestionImageState(
            @Nullable Bundle savedInstanceState
    ) {
        if (savedInstanceState == null) {
            return;
        }

        String selectedUriText =
                safeText(
                        savedInstanceState.getString(
                                STATE_SELECTED_QUESTION_IMAGE_URI
                        )
                );

        if (!selectedUriText.isEmpty()) {
            selectedQuestionImageUri =
                    Uri.parse(
                            selectedUriText
                    );

            selectedQuestionImagePrivatePath =
                    safeText(
                            savedInstanceState.getString(
                                    STATE_SELECTED_QUESTION_IMAGE_PATH
                            )
                    );

            lastQuestionImageOcrText =
                    safeText(
                            savedInstanceState.getString(
                                    STATE_LAST_IMAGE_OCR_TEXT
                            )
                    );

            displaySelectedQuestionImage();

            binding.textQuestionImageStatus.setText(
                    "Question photo वापस load हो गई है। Subject load होने के बाद OCR दोबारा चलेगा।"
            );

            restartImageOcrAfterSubjectLoad =
                    true;
        }

        String pendingUriText =
                safeText(
                        savedInstanceState.getString(
                                STATE_PENDING_CAMERA_IMAGE_URI
                        )
                );

        if (!pendingUriText.isEmpty()) {
            pendingCameraImageUri =
                    Uri.parse(
                            pendingUriText
                    );
        }

        pendingCameraImagePath =
                safeText(
                        savedInstanceState.getString(
                                STATE_PENDING_CAMERA_IMAGE_PATH
                        )
                );
    }

    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {
        super.onSaveInstanceState(
                outState
        );

        if (selectedQuestionImageUri != null) {
            outState.putString(
                    STATE_SELECTED_QUESTION_IMAGE_URI,
                    selectedQuestionImageUri.toString()
            );
        }

        outState.putString(
                STATE_SELECTED_QUESTION_IMAGE_PATH,
                safeText(
                        selectedQuestionImagePrivatePath
                )
        );

        if (pendingCameraImageUri != null) {
            outState.putString(
                    STATE_PENDING_CAMERA_IMAGE_URI,
                    pendingCameraImageUri.toString()
            );
        }

        outState.putString(
                STATE_PENDING_CAMERA_IMAGE_PATH,
                safeText(
                        pendingCameraImagePath
                )
        );

        outState.putString(
                STATE_LAST_IMAGE_OCR_TEXT,
                lastQuestionImageOcrText
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

                        if (studentProfile == null) {
                            showLoadingState(
                                    false
                            );

                            showNoProfileState();
                            return;
                        }

                        activeStudentProfile =
                                studentProfile;

                        showStudentProfile(
                                studentProfile
                        );

                        loadActualSchoolSubjects(
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

                        showLoadingState(
                                false
                        );

                        showNoProfileState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.ask_saathi_profile_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentProfile(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textAskStudent.setText(
                getString(
                        R.string.ask_saathi_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass(),
                        studentProfile.getExplanationLanguage()
                )
        );
    }

    private void loadActualSchoolSubjects(
            @NonNull StudentProfileEntity studentProfile
    ) {
        schoolSubjectRepository.getSubjectsForProfile(
                studentProfile.getProfileId(),
                true,
                new SchoolSubjectRepository.SubjectsCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolSubjectEntity>
                                    loadedSubjects
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        showLoadingState(
                                false
                        );

                        populateActualSchoolSubjects(
                                loadedSubjects
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

                        showNoSubjectsState();

                        Snackbar.make(
                                binding.getRoot(),
                                "School subjects load नहीं हो सके।",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void populateActualSchoolSubjects(
            @NonNull List<SchoolSubjectEntity> loadedSubjects
    ) {
        schoolSubjects.clear();
        subjectDisplayNames.clear();

        for (SchoolSubjectEntity schoolSubject :
                loadedSubjects) {

            if (schoolSubject == null
                    || !schoolSubject.isEnabled()) {

                continue;
            }

            String primarySubjectName =
                    getPrimarySubjectName(
                            schoolSubject
                    );

            if (primarySubjectName.isEmpty()) {
                continue;
            }

            schoolSubjects.add(
                    schoolSubject
            );

            subjectDisplayNames.add(
                    getSubjectDisplayName(
                            schoolSubject
                    )
            );
        }

        ArrayAdapter<String> subjectAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        subjectDisplayNames
                );

        binding.dropdownAskSubject.setAdapter(
                subjectAdapter
        );

        binding.dropdownAskSubject.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position < 0
                            || position >= schoolSubjects.size()
                            || aiAnswerRequestInProgress) {

                        return;
                    }

                    selectSubject(
                            schoolSubjects.get(
                                    position
                            ),
                            ""
                    );
                }
        );

        if (schoolSubjects.isEmpty()) {
            showNoSubjectsState();
            return;
        }

        enableQuestionControls();

        SchoolSubjectEntity prefilledSubject =
                findMatchingSubject(
                        prefillSubjectName
                );

        if (prefilledSubject == null) {
            prefilledSubject =
                    schoolSubjects.get(
                            0
                    );
        }

        selectSubject(
                prefilledSubject,
                prefillChapterTitle
        );

        applyPrefilledQuestion();
    }

    private void selectSubject(
            @NonNull SchoolSubjectEntity schoolSubject,
            @NonNull String preferredChapterTitle
    ) {
        selectedSchoolSubject =
                schoolSubject;

        selectedSubjectName =
                getPrimarySubjectName(
                        schoolSubject
                );

        binding.dropdownAskSubject.setText(
                getSubjectDisplayName(
                        schoolSubject
                ),
                false
        );

        binding.inputQuestion.setError(
                null
        );

        binding.inputQuestion.setHelperText(
                null
        );

        clearPreviousAnswer();

        loadOptionalChapters(
                schoolSubject,
                preferredChapterTitle
        );

        updateQuestionImageButtonsEnabledState();

        if (selectedQuestionImageUri != null
                && (
                restartImageOcrAfterSubjectLoad
                        || !questionImageOcrInProgress
        )) {

            restartImageOcrAfterSubjectLoad =
                    false;

            startQuestionImageOcr(
                    selectedQuestionImageUri
            );
        }
    }

    private void loadOptionalChapters(
            @NonNull SchoolSubjectEntity schoolSubject,
            @NonNull String preferredChapterTitle
    ) {
        selectedChapter =
                null;

        chapterItems.clear();

        latestChapterRequestSubjectRowId =
                schoolSubject.getSubjectRowId();

        setOptionalChapterDropdown(
                new ArrayList<>(),
                ""
        );

        childSchoolBookChapterRepository
                .getChildChaptersForSubject(
                        schoolSubject.getSubjectRowId(),
                        new ChildSchoolBookChapterRepository
                                .ChildChaptersCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull ChildSchoolBookChapterRepository
                                            .ChildChapterResult result
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                if (!isCurrentChapterRequest(
                                        result.getSubjectRowId()
                                )) {
                                    return;
                                }

                                List<SchoolBookChapterEntity>
                                        availableChapters =
                                        result.isAvailable()
                                                ? result.getChapters()
                                                : new ArrayList<>();

                                setOptionalChapterDropdown(
                                        availableChapters,
                                        preferredChapterTitle
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                if (!isCurrentChapterRequest(
                                        schoolSubject
                                                .getSubjectRowId()
                                )) {
                                    return;
                                }

                                setOptionalChapterDropdown(
                                        new ArrayList<>(),
                                        ""
                                );
                            }
                        }
                );
    }

    private boolean isCurrentChapterRequest(
            long subjectRowId
    ) {
        return selectedSchoolSubject != null
                && selectedSchoolSubject.getSubjectRowId()
                == subjectRowId
                && latestChapterRequestSubjectRowId
                == subjectRowId;
    }

    private void setOptionalChapterDropdown(
            @NonNull List<SchoolBookChapterEntity> availableChapters,
            @NonNull String preferredChapterTitle
    ) {
        chapterItems.clear();

        for (SchoolBookChapterEntity chapter :
                availableChapters) {

            if (chapter == null
                    || !chapter.isReadyForChildMode()) {

                continue;
            }

            chapterItems.add(
                    chapter
            );
        }

        List<String> chapterDisplayNames =
                new ArrayList<>();

        chapterDisplayNames.add(
                OPTIONAL_CHAPTER_LABEL
        );

        for (SchoolBookChapterEntity chapter :
                chapterItems) {

            chapterDisplayNames.add(
                    createChapterDisplayName(
                            chapter
                    )
            );
        }

        ArrayAdapter<String> chapterAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        chapterDisplayNames
                );

        binding.dropdownAskChapter.setAdapter(
                chapterAdapter
        );

        binding.dropdownAskChapter.setEnabled(
                selectedSchoolSubject != null
                        && !aiAnswerRequestInProgress
        );

        binding.dropdownAskChapter.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (aiAnswerRequestInProgress) {
                        return;
                    }

                    if (position <= 0) {
                        selectedChapter =
                                null;

                        binding.dropdownAskChapter.setText(
                                OPTIONAL_CHAPTER_LABEL,
                                false
                        );

                        clearPreviousAnswer();
                        return;
                    }

                    int chapterIndex =
                            position - 1;

                    if (chapterIndex < 0
                            || chapterIndex >= chapterItems.size()) {

                        return;
                    }

                    selectedChapter =
                            chapterItems.get(
                                    chapterIndex
                            );

                    binding.dropdownAskChapter.setText(
                            createChapterDisplayName(
                                    selectedChapter
                            ),
                            false
                    );

                    clearPreviousAnswer();
                }
        );

        SchoolBookChapterEntity preferredChapter =
                findMatchingChapter(
                        preferredChapterTitle
                );

        if (preferredChapter == null) {
            selectedChapter =
                    null;

            binding.dropdownAskChapter.setText(
                    OPTIONAL_CHAPTER_LABEL,
                    false
            );

        } else {
            selectedChapter =
                    preferredChapter;

            binding.dropdownAskChapter.setText(
                    createChapterDisplayName(
                            preferredChapter
                    ),
                    false
            );
        }
    }

    @Nullable
    private SchoolSubjectEntity findMatchingSubject(
            @Nullable String preferredSubjectName
    ) {
        String normalizedPreferred =
                normalizeText(
                        preferredSubjectName
                );

        if (normalizedPreferred.isEmpty()) {
            return null;
        }

        for (SchoolSubjectEntity schoolSubject :
                schoolSubjects) {

            String englishName =
                    normalizeText(
                            schoolSubject
                                    .getSubjectNameEnglish()
                    );

            String hindiName =
                    normalizeText(
                            schoolSubject
                                    .getSubjectNameHindi()
                    );

            String bilingualName =
                    normalizeText(
                            schoolSubject
                                    .getBilingualDisplayName()
                    );

            if (normalizedPreferred.equals(
                    englishName
            )
                    || normalizedPreferred.equals(
                    hindiName
            )
                    || normalizedPreferred.equals(
                    bilingualName
            )) {

                return schoolSubject;
            }
        }

        return null;
    }

    @Nullable
    private SchoolBookChapterEntity findMatchingChapter(
            @Nullable String preferredChapterTitle
    ) {
        String normalizedPreferred =
                normalizeText(
                        preferredChapterTitle
                );

        if (normalizedPreferred.isEmpty()) {
            return null;
        }

        for (SchoolBookChapterEntity chapter :
                chapterItems) {

            if (normalizedPreferred.equals(
                    normalizeText(
                            chapter.getDisplayTitle()
                    )
            )
                    || normalizedPreferred.equals(
                    normalizeText(
                            chapter.getChapterTitleEnglish()
                    )
            )
                    || normalizedPreferred.equals(
                    normalizeText(
                            chapter.getChapterTitleHindi()
                    )
            )) {

                return chapter;
            }
        }

        return null;
    }

    @NonNull
    private String getPrimarySubjectName(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String englishName =
                safeText(
                        schoolSubject
                                .getSubjectNameEnglish()
                );

        if (!englishName.isEmpty()) {
            return englishName;
        }

        return safeText(
                schoolSubject
                        .getSubjectNameHindi()
        );
    }

    @NonNull
    private String getSubjectDisplayName(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String bilingualName =
                safeText(
                        schoolSubject
                                .getBilingualDisplayName()
                );

        if (!bilingualName.isEmpty()) {
            return bilingualName;
        }

        return getPrimarySubjectName(
                schoolSubject
        );
    }

    @NonNull
    private String createChapterDisplayName(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        String chapterLabel =
                safeText(
                        chapter.getChapterLabel()
                );

        String chapterTitle =
                safeText(
                        chapter.getDisplayTitle()
                );

        if (chapterLabel.isEmpty()) {
            return chapterTitle;
        }

        if (chapterTitle.isEmpty()) {
            return chapterLabel;
        }

        return chapterLabel
                + " — "
                + chapterTitle;
    }

    private void applyPrefilledQuestion() {
        if (prefilledQuestionApplied) {
            applyRequestedInputMode();
            return;
        }

        if (prefillQuestion.isEmpty()) {
            prefilledQuestionApplied =
                    true;

            applyRequestedInputMode();
            return;
        }

        setQuestionText(
                prefillQuestion
        );

        Snackbar.make(
                binding.getRoot(),
                R.string.history_question_restored,
                Snackbar.LENGTH_SHORT
        ).show();

        prefilledQuestionApplied =
                true;

        prefillSubjectName =
                "";

        prefillChapterTitle =
                "";

        prefillQuestion =
                "";

        applyRequestedInputMode();
    }

    /**
     * Global AI companion से चुना गया voice/photo रास्ता केवल तब खोलें जब
     * active profile और subject controls पूरी तरह तैयार हो चुके हों।
     */
    private void applyRequestedInputMode() {
        if (requestedInputModeApplied
                || requestedInputMode.isEmpty()) {
            return;
        }

        requestedInputModeApplied = true;
        String inputMode = requestedInputMode;
        requestedInputMode = "";

        binding.getRoot().post(() -> {
            if (!isActivityAvailable()
                    || activeStudentProfile == null
                    || selectedSchoolSubject == null) {
                return;
            }

            if (SmartAiCompanionController.INPUT_MODE_VOICE.equals(
                    inputMode
            )) {
                startVoiceQuestionInput();
                return;
            }

            if (SmartAiCompanionController.INPUT_MODE_PHOTO.equals(
                    inputMode
            )) {
                openQuestionCamera();
            }
        });
    }

    private void submitQuickQuestion(
            @NonNull String question
    ) {
        if (aiAnswerRequestInProgress) {
            return;
        }

        setQuestionText(
                question
        );

        submitQuestion(
                question
        );
    }

    private void submitQuestion(
            @NonNull String question
    ) {
        if (aiAnswerRequestInProgress) {
            Snackbar.make(
                    binding.getRoot(),
                    "Smart AI अभी पिछले सवाल का जवाब तैयार कर रही है।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (activeStudentProfile == null) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.ask_saathi_profile_required,
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        if (selectedSchoolSubject == null
                || selectedSubjectName.isEmpty()) {

            Snackbar.make(
                    binding.getRoot(),
                    "पहले अपना Subject चुनें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (questionImageOcrInProgress) {
            Snackbar.make(
                    binding.getRoot(),
                    "फोटो से सवाल अभी पढ़ा जा रहा है। थोड़ी देर रुकें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (!selectedSchoolSubject.isAiTutorEnabled()) {
            Snackbar.make(
                    binding.getRoot(),
                    "इस Subject के लिए AI Tutor Parent द्वारा बंद है।",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        String safeQuestion =
                safeText(
                        question
                );

        if (safeQuestion.isEmpty()) {
            binding.inputQuestion.setError(
                    getString(
                            R.string.ask_saathi_question_required
                    )
            );

            binding.editQuestion.requestFocus();
            return;
        }

        binding.inputQuestion.setError(
                null
        );

        binding.inputQuestion.setHelperText(
                null
        );

        hideKeyboard();

        requestSmartAiAnswer(
                safeQuestion
        );
    }

    private void submitFollowUpQuestion() {
        String followUpQuestion =
                binding.editFollowUpQuestion.getText() == null
                        ? ""
                        : safeText(
                                binding.editFollowUpQuestion
                                        .getText()
                                        .toString()
                        );

        if (followUpQuestion.isEmpty()) {
            binding.inputFollowUpQuestion.setError(
                    getString(
                            R.string.ask_saathi_follow_up_required
                    )
            );

            binding.editFollowUpQuestion.requestFocus();
            return;
        }

        binding.inputFollowUpQuestion.setError(
                null
        );

        submitQuestion(
                followUpQuestion
        );

        if (aiAnswerRequestInProgress) {
            binding.editFollowUpQuestion.setText(
                    null
            );
        }
    }

    private void requestSmartAiAnswer(
            @NonNull String question
    ) {
        if (activeStudentProfile == null
                || selectedSchoolSubject == null) {

            return;
        }

        aiAnswerRequestGeneration++;

        int requestGeneration =
                aiAnswerRequestGeneration;

        Uri questionImageUri =
                selectedQuestionImageUri;

        boolean imageSelected =
                questionImageUri != null;

        showSmartAiLoadingState(
                question,
                imageSelected
        );

        FirebaseStudyTutorClient.TutorRequest tutorRequest =
                new FirebaseStudyTutorClient.TutorRequest(
                        activeStudentProfile.getStudentName(),
                        activeStudentProfile.getEducationBoard(),
                        activeStudentProfile.getStudentClass(),
                        activeStudentProfile.getExplanationLanguage(),
                        selectedSubjectName,
                        getSelectedChapterTitle(),
                        question
                );

        if (questionImageUri == null) {
            sendSmartAiRequest(
                    question,
                    tutorRequest,
                    requestGeneration,
                    null
            );

            return;
        }

        prepareQuestionImageAndSendRequest(
                question,
                tutorRequest,
                requestGeneration,
                questionImageUri
        );
    }

    private void prepareQuestionImageAndSendRequest(
            @NonNull String question,
            @NonNull FirebaseStudyTutorClient.TutorRequest tutorRequest,
            int requestGeneration,
            @NonNull Uri questionImageUri
    ) {
        binding.progressQuestionImageOcr.setVisibility(
                View.VISIBLE
        );

        binding.textQuestionImageStatus.setText(
                "Original photo को Smart AI के लिए सुरक्षित रूप से तैयार किया जा रहा है।"
        );

        questionImageBitmapLoader.loadForAi(
                questionImageUri,
                new QuestionImageBitmapLoader.ImageLoadCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull Bitmap bitmap
                    ) {
                        if (!isCurrentAiAnswerRequest(
                                requestGeneration
                        )) {
                            recycleAiQuestionBitmap(
                                    bitmap
                            );

                            return;
                        }

                        binding.progressQuestionImageOcr.setVisibility(
                                View.GONE
                        );

                        binding.textQuestionImageStatus.setText(
                                "Original photo और corrected text Smart AI को भेजे जा रहे हैं।"
                        );

                        binding.textSaathiAnswer.setText(
                                "Study Saathi आपकी original photo और सवाल दोनों को समझ रही है...\n\n"
                                        + "चित्र, अक्षर, संख्या और symbols जाँचे जा रहे हैं।"
                        );

                        sendSmartAiRequest(
                                question,
                                tutorRequest,
                                requestGeneration,
                                bitmap
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Throwable throwable
                    ) {
                        if (!isCurrentAiAnswerRequest(
                                requestGeneration
                        )) {
                            return;
                        }

                        Log.e(
                                LOG_TAG,
                                "Original question image load failed.",
                                throwable
                        );

                        binding.progressQuestionImageOcr.setVisibility(
                                View.GONE
                        );

                        binding.textQuestionImageStatus.setText(
                                "Original photo AI request में attach नहीं हो सकी। "
                                        + "Corrected OCR text से Smart answer तैयार किया जा रहा है।"
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                "Photo attach नहीं हो सकी। Text से Smart answer जारी है।",
                                Snackbar.LENGTH_LONG
                        ).show();

                        sendSmartAiRequest(
                                question,
                                tutorRequest,
                                requestGeneration,
                                null
                        );
                    }
                }
        );
    }

    private void sendSmartAiRequest(
            @NonNull String question,
            @NonNull FirebaseStudyTutorClient.TutorRequest tutorRequest,
            int requestGeneration,
            @Nullable Bitmap questionBitmap
    ) {
        final boolean originalImageAttached =
                questionBitmap != null;

        firebaseStudyTutorClient.askQuestionWithResult(
                tutorRequest,
                questionBitmap,
                new FirebaseStudyTutorClient.TutorResultCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull SmartTutorAnswerResult result
                    ) {
                        recycleAiQuestionBitmap(
                                questionBitmap
                        );

                        if (!isCurrentAiAnswerRequest(
                                requestGeneration
                        )) {
                            return;
                        }

                        binding.progressQuestionImageOcr.setVisibility(
                                View.GONE
                        );

                        finishSmartAiLoadingState();

                        new StudentKnowledgeGraphStore(AskStudySaathiActivity.this)
                                .recordAnswer(
                                        activeStudentProfile.getProfileId(),
                                        selectedSchoolSubject.getBilingualDisplayName(),
                                        selectedChapter == null
                                                ? "General"
                                                : selectedChapter.getDisplayTitle(),
                                        question,
                                        result
                                );

                        showCompletedAnswer(
                                question,
                                result.buildDisplayAnswerText()
                        );

                        if (originalImageAttached
                                && selectedQuestionImageUri != null) {

                            binding.textQuestionImageStatus.setText(
                                    "Original photo और corrected text दोनों से Smart AI answer तैयार हुआ।"
                            );
                        }

                        saveDoubtHistory(
                                question,
                                result.buildDisplayAnswerText()
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                originalImageAttached
                                        ? "Photo देखकर Smart AI answer तैयार है।"
                                        : "Smart AI answer तैयार है।",
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onError(
                            @NonNull Throwable throwable
                    ) {
                        recycleAiQuestionBitmap(
                                questionBitmap
                        );

                        if (!isCurrentAiAnswerRequest(
                                requestGeneration
                        )) {
                            return;
                        }

                        Log.e(
                                LOG_TAG,
                                "Firebase Smart AI request failed.",
                                throwable
                        );

                        binding.progressQuestionImageOcr.setVisibility(
                                View.GONE
                        );

                        finishSmartAiLoadingState();

                        showOfflineFallbackAnswer(
                                question,
                                throwable,
                                originalImageAttached
                        );
                    }
                }
        );
    }

    private void recycleAiQuestionBitmap(
            @Nullable Bitmap bitmap
    ) {
        if (bitmap == null
                || bitmap.isRecycled()) {

            return;
        }

        bitmap.recycle();
    }

    private boolean isCurrentAiAnswerRequest(
            int requestGeneration
    ) {
        return isActivityAvailable()
                && aiAnswerRequestInProgress
                && requestGeneration
                == aiAnswerRequestGeneration;
    }

    private void showSmartAiLoadingState(
            @NonNull String question,
            boolean imageSelected
    ) {
        aiAnswerRequestInProgress =
                true;

        setCurrentTurnVisible(
                true
        );

        binding.textUserQuestion.setText(
                question
        );

        if (imageSelected) {
            binding.textSaathiAnswer.setText(
                    "Study Saathi आपकी photo और सवाल तैयार कर रही है...\n\n"
                            + "Original image को Smart AI analysis के लिए load किया जा रहा है।"
            );

        } else {
            binding.textSaathiAnswer.setText(
                    "Study Saathi आपका सवाल समझ रही है...\n\n"
                            + "कक्षा, बोर्ड और Subject के अनुसार Smart answer तैयार किया जा रहा है।"
            );
        }

        binding.cardAnswer.setVisibility(
                View.VISIBLE
        );

        binding.layoutFollowUpQuestion.setVisibility(
                View.GONE
        );

        binding.buttonOpenLesson.setVisibility(
                View.GONE
        );

        binding.buttonAskSaathi.setText(
                "Smart answer तैयार हो रहा है..."
        );

        disableControlsDuringAiRequest();

        binding.askSaathiScrollView.post(() ->
                binding.askSaathiScrollView.smoothScrollTo(
                        0,
                        binding.cardAnswer.getBottom()
                )
        );
    }

    private void disableControlsDuringAiRequest() {
        binding.dropdownAskSubject.setEnabled(
                false
        );

        binding.dropdownAskChapter.setEnabled(
                false
        );

        binding.editQuestion.setEnabled(
                false
        );

        binding.buttonAskSaathi.setEnabled(
                false
        );

        binding.buttonQuickExplain.setEnabled(
                false
        );

        binding.buttonQuickKeyPoints.setEnabled(
                false
        );

        binding.buttonQuickExample.setEnabled(
                false
        );

        binding.buttonQuickPractice.setEnabled(
                false
        );

        binding.buttonQuestionCamera.setEnabled(
                false
        );

        binding.buttonQuestionGallery.setEnabled(
                false
        );

        binding.buttonRemoveQuestionImage.setEnabled(
                false
        );

        binding.inputQuestion.setEndIconVisible(
                false
        );

        binding.editFollowUpQuestion.setEnabled(
                false
        );

        binding.buttonSendFollowUp.setEnabled(
                false
        );
    }

    private void finishSmartAiLoadingState() {
        aiAnswerRequestInProgress =
                false;

        binding.buttonAskSaathi.setText(
                R.string.ask_study_saathi_button
        );

        if (activeStudentProfile != null
                && selectedSchoolSubject != null) {

            enableQuestionControls();

        } else {
            disableQuestionControls();
        }
    }

    private void showCompletedAnswer(
            @NonNull String question,
            @NonNull String answer
    ) {
        binding.textUserQuestion.setText(
                question
        );

        binding.textSaathiAnswer.setText(
                answer
        );

        lastCompletedQuestion =
                question;

        lastCompletedAnswer =
                binding.textSaathiAnswer.getText();

        completedTurnArchived =
                false;

        archiveCompletedTurnIfNeeded();

        setCurrentTurnVisible(
                false
        );

        binding.buttonOpenLesson.setVisibility(
                selectedChapter == null
                        ? View.GONE
                        : View.VISIBLE
        );

        binding.cardAnswer.setVisibility(
                View.VISIBLE
        );

        binding.inputFollowUpQuestion.setError(
                null
        );

        binding.editFollowUpQuestion.setEnabled(
                true
        );

        binding.buttonSendFollowUp.setEnabled(
                true
        );

        binding.layoutFollowUpQuestion.setVisibility(
                View.VISIBLE
        );

        binding.askSaathiScrollView.post(() ->
                binding.askSaathiScrollView.smoothScrollTo(
                        0,
                        binding.cardAnswer.getBottom()
                )
        );
    }

    private void showOfflineFallbackAnswer(
            @NonNull String question,
            @NonNull Throwable throwable,
            boolean imageWasAttached
    ) {
        String localAnswer =
                createLocalFallbackAnswer(
                        question
                );

        String imageNotice =
                imageWasAttached
                        ? "\nOriginal photo का online analysis पूरा नहीं हुआ। "
                          + "Offline answer केवल question text के आधार पर है।\n"
                        : "";

        String displayedAnswer =
                "**Online Smart AI अभी उपलब्ध नहीं हुई**\n\n"
                        + createReadableAiErrorMessage(
                        throwable
                )
                        + imageNotice
                        + "\n\n**Offline Answer**\n\n"
                        + localAnswer;

        showCompletedAnswer(
                question,
                displayedAnswer
        );

        if (selectedQuestionImageUri != null) {
            binding.textQuestionImageStatus.setText(
                    "Online photo analysis पूरा नहीं हुआ। "
                            + "Question photo सुरक्षित है और दोबारा प्रयास किया जा सकता है।"
            );
        }

        saveDoubtHistory(
                question,
                displayedAnswer
        );

        Snackbar.make(
                binding.getRoot(),
                createReadableAiErrorMessage(
                        throwable
                ),
                Snackbar.LENGTH_LONG
        ).show();
    }

    @NonNull
    private String createLocalFallbackAnswer(
            @NonNull String question
    ) {
        if (activeStudentProfile == null) {
            return "Offline answer तैयार नहीं हो सका।";
        }

        String chapterTitle =
                getSelectedChapterTitle();

        String chapterDescription =
                getSelectedChapterDescription();

        LessonContent lessonContent =
                LessonCatalog.getLessonContent(
                        selectedSubjectName,
                        chapterTitle,
                        chapterDescription
                );

        return DoubtAssistantEngine.createAnswer(
                question,
                selectedSubjectName,
                chapterTitle,
                lessonContent,
                activeStudentProfile
                        .getExplanationLanguage()
        );
    }

    @NonNull
    private String createReadableAiErrorMessage(
            @NonNull Throwable throwable
    ) {
        String completeMessage =
                collectThrowableMessages(
                        throwable
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (completeMessage.contains(
                "app check"
        )
                || completeMessage.contains(
                "permission_denied"
        )
                || completeMessage.contains(
                "permission denied"
        )
                || completeMessage.contains(
                "403"
        )) {

            return "App Check verification स्वीकार नहीं हुई। Debug token और Firebase App Check setup जाँचें।";
        }

        if (completeMessage.contains(
                "model"
        )
                && (
                completeMessage.contains(
                        "not found"
                )
                        || completeMessage.contains(
                        "404"
                )
        )) {

            return "Selected Gemini model उपलब्ध नहीं मिला। Model configuration जाँचें।";
        }

        if (completeMessage.contains(
                "quota"
        )
                || completeMessage.contains(
                "resource_exhausted"
        )
                || completeMessage.contains(
                "rate limit"
        )
                || completeMessage.contains(
                "429"
        )) {

            return "AI request limit अभी पूरी हो गई है। थोड़ी देर बाद दोबारा प्रयास करें।";
        }

        if (completeMessage.contains(
                "network"
        )
                || completeMessage.contains(
                "internet"
        )
                || completeMessage.contains(
                "timeout"
        )
                || completeMessage.contains(
                "timed out"
        )
                || completeMessage.contains(
                "unavailable"
        )
                || completeMessage.contains(
                "socket"
        )) {

            return "Internet connection या Firebase AI service उपलब्ध नहीं है।";
        }

        return "Firebase Smart AI request पूरी नहीं हो सकी। Logcat में AskStudySaathi error जाँचें।";
    }

    @NonNull
    private String collectThrowableMessages(
            @Nullable Throwable throwable
    ) {
        StringBuilder messageBuilder =
                new StringBuilder();

        Throwable currentThrowable =
                throwable;

        int depth =
                0;

        while (currentThrowable != null
                && depth < 8) {

            String throwableMessage =
                    safeText(
                            currentThrowable.getMessage()
                    );

            if (!throwableMessage.isEmpty()) {
                if (messageBuilder.length() > 0) {
                    messageBuilder.append(
                            " | "
                    );
                }

                messageBuilder.append(
                        throwableMessage
                );
            }

            currentThrowable =
                    currentThrowable.getCause();

            depth++;
        }

        return messageBuilder.toString();
    }

    @NonNull
    private String getSelectedChapterTitle() {
        if (selectedChapter == null) {
            return GENERAL_QUESTION_CHAPTER_TITLE;
        }

        String displayTitle =
                safeText(
                        selectedChapter.getDisplayTitle()
                );

        return displayTitle.isEmpty()
                ? GENERAL_QUESTION_CHAPTER_TITLE
                : displayTitle;
    }

    @NonNull
    private String getSelectedChapterDescription() {
        if (selectedChapter != null) {
            String chapterDescription =
                    safeText(
                            selectedChapter
                                    .getChapterDescription()
                    );

            if (!chapterDescription.isEmpty()) {
                return chapterDescription;
            }

            return "This is a question from the selected school-book chapter.";
        }

        String studentClass =
                activeStudentProfile == null
                        ? ""
                        : safeText(
                        activeStudentProfile
                                .getStudentClass()
                );

        String educationBoard =
                activeStudentProfile == null
                        ? ""
                        : safeText(
                        activeStudentProfile
                                .getEducationBoard()
                );

        return "The student has asked a general question in "
                + selectedSubjectName
                + ". Explain it according to Class "
                + studentClass
                + " and "
                + educationBoard
                + " learning level.";
    }

    private void saveDoubtHistory(
            @NonNull String question,
            @NonNull String answer
    ) {
        if (activeStudentProfile == null
                || selectedSchoolSubject == null) {

            return;
        }

        DoubtHistoryEntity historyEntity =
                new DoubtHistoryEntity();

        historyEntity.setProfileId(
                activeStudentProfile.getProfileId()
        );

        historyEntity.setEducationBoard(
                activeStudentProfile.getEducationBoard()
        );

        historyEntity.setStudentClass(
                activeStudentProfile.getStudentClass()
        );

        historyEntity.setSubjectName(
                selectedSubjectName
        );

        historyEntity.setChapterTitle(
                getSelectedChapterTitle()
        );

        historyEntity.setQuestionText(
                question
        );

        historyEntity.setAnswerText(
                answer
        );

        historyEntity.setExplanationLanguage(
                activeStudentProfile
                        .getExplanationLanguage()
        );

        historyEntity.setCreatedAt(
                System.currentTimeMillis()
        );

        doubtHistoryRepository.saveHistory(
                historyEntity,
                new DoubtHistoryRepository
                        .SaveHistoryCallback() {

                    @Override
                    public void onSuccess(
                            long historyId
                    ) {
                        // Doubt History successfully saved.
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.doubt_history_save_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void openSelectedLesson() {
        if (activeStudentProfile == null
                || selectedSchoolSubject == null
                || selectedSubjectName.isEmpty()) {

            Snackbar.make(
                    binding.getRoot(),
                    "पहले अपना Subject चुनें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (selectedChapter == null) {
            Snackbar.make(
                    binding.getRoot(),
                    "Lesson खोलने के लिए optional Chapter चुनें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        Intent lessonIntent =
                new Intent(
                        AskStudySaathiActivity.this,
                        LessonActivity.class
                );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                selectedSubjectName
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                selectedChapter.getDisplayTitle()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                selectedChapter.getChapterDescription()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_STUDENT_CLASS,
                activeStudentProfile.getStudentClass()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_EDUCATION_BOARD,
                activeStudentProfile.getEducationBoard()
        );

        startActivity(
                lessonIntent
        );
    }

    private void openDoubtHistory() {
        Intent historyIntent =
                new Intent(
                        AskStudySaathiActivity.this,
                        DoubtHistoryActivity.class
                );

        startActivity(
                historyIntent
        );
    }

    private void clearPreviousAnswer() {
        if (aiAnswerRequestInProgress) {
            return;
        }

        binding.cardAnswer.setVisibility(
                View.GONE
        );

        binding.layoutConversationHistory.removeAllViews();
        binding.layoutConversationHistory.setVisibility(
                View.GONE
        );

        lastCompletedQuestion =
                "";

        lastCompletedAnswer =
                null;

        completedTurnArchived =
                false;

        binding.editFollowUpQuestion.setText(
                null
        );

        binding.inputFollowUpQuestion.setError(
                null
        );

        binding.layoutFollowUpQuestion.setVisibility(
                View.GONE
        );

        binding.buttonOpenLesson.setVisibility(
                View.GONE
        );

        binding.textUserQuestion.setText(
                ""
        );

        binding.textSaathiAnswer.setText(
                ""
        );
    }

    private void archiveCompletedTurnIfNeeded() {
        if (completedTurnArchived
                || lastCompletedQuestion.isEmpty()
                || lastCompletedAnswer == null
                || lastCompletedAnswer.length() == 0) {

            return;
        }

        View archivedTurn =
                getLayoutInflater().inflate(
                        R.layout.item_study_conversation_turn,
                        binding.layoutConversationHistory,
                        false
                );

        TextView archivedQuestion =
                archivedTurn.findViewById(
                        R.id.textConversationQuestion
                );

        TextView archivedAnswer =
                archivedTurn.findViewById(
                        R.id.textConversationAnswer
                );

        archivedQuestion.setText(
                lastCompletedQuestion
        );

        archivedAnswer.setText(
                lastCompletedAnswer
        );

        binding.layoutConversationHistory.addView(
                archivedTurn
        );

        binding.layoutConversationHistory.setVisibility(
                View.VISIBLE
        );

        completedTurnArchived =
                true;
    }

    private void setCurrentTurnVisible(
            boolean visible
    ) {
        int visibility =
                visible
                        ? View.VISIBLE
                        : View.GONE;

        binding.labelCurrentQuestion.setVisibility(
                visibility
        );

        binding.textUserQuestion.setVisibility(
                visibility
        );

        binding.dividerCurrentTurn.setVisibility(
                visibility
        );

        binding.labelCurrentAnswer.setVisibility(
                visibility
        );

        binding.textSaathiAnswer.setVisibility(
                visibility
        );
    }

    @NonNull
    private String getCurrentQuestionText() {
        return binding.editQuestion.getText() == null
                ? ""
                : binding.editQuestion
                .getText()
                .toString()
                .trim();
    }

    private void setQuestionText(
            @NonNull String questionText
    ) {
        binding.editQuestion.setText(
                questionText
        );

        binding.editQuestion.setSelection(
                questionText.length()
        );

        binding.editQuestion.requestFocus();
    }

    private void hideKeyboard() {
        View currentView =
                getCurrentFocus();

        if (currentView == null) {
            currentView =
                    binding.editQuestion;
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE
                );

        inputMethodManager.hideSoftInputFromWindow(
                currentView.getWindowToken(),
                0
        );

        currentView.clearFocus();
    }

    private void enableQuestionControls() {
        if (aiAnswerRequestInProgress) {
            disableControlsDuringAiRequest();
            return;
        }

        binding.dropdownAskSubject.setEnabled(
                true
        );

        binding.dropdownAskChapter.setEnabled(
                true
        );

        binding.editQuestion.setEnabled(
                true
        );

        binding.buttonAskSaathi.setEnabled(
                true
        );

        binding.buttonDoubtHistory.setEnabled(
                true
        );

        binding.buttonQuickExplain.setEnabled(
                true
        );

        binding.buttonQuickKeyPoints.setEnabled(
                true
        );

        binding.buttonQuickExample.setEnabled(
                true
        );

        binding.buttonQuickPractice.setEnabled(
                true
        );

        binding.inputQuestion.setEndIconVisible(
                true
        );

        updateQuestionImageButtonsEnabledState();
    }

    private void showNoProfileState() {
        activeStudentProfile =
                null;

        selectedSchoolSubject =
                null;

        selectedChapter =
                null;

        selectedSubjectName =
                "";

        schoolSubjects.clear();
        subjectDisplayNames.clear();
        chapterItems.clear();

        binding.textAskStudent.setText(
                R.string.create_profile_to_continue
        );

        disableQuestionControls();

        Snackbar.make(
                binding.getRoot(),
                R.string.ask_saathi_profile_required,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showNoSubjectsState() {
        selectedSchoolSubject =
                null;

        selectedChapter =
                null;

        selectedSubjectName =
                "";

        schoolSubjects.clear();
        subjectDisplayNames.clear();
        chapterItems.clear();

        binding.dropdownAskSubject.setText(
                "",
                false
        );

        binding.dropdownAskChapter.setText(
                OPTIONAL_CHAPTER_LABEL,
                false
        );

        disableQuestionControls();

        binding.buttonDoubtHistory.setEnabled(
                true
        );

        Snackbar.make(
                binding.getRoot(),
                "इस profile में कोई school subject नहीं जोड़ा गया है। "
                        + "Parent Curriculum Setup से subject जोड़ें।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void disableQuestionControls() {
        binding.dropdownAskSubject.setEnabled(
                false
        );

        binding.dropdownAskChapter.setEnabled(
                false
        );

        binding.editQuestion.setEnabled(
                false
        );

        binding.buttonAskSaathi.setEnabled(
                false
        );

        binding.buttonQuickExplain.setEnabled(
                false
        );

        binding.buttonQuickKeyPoints.setEnabled(
                false
        );

        binding.buttonQuickExample.setEnabled(
                false
        );

        binding.buttonQuickPractice.setEnabled(
                false
        );

        binding.buttonQuestionCamera.setEnabled(
                false
        );

        binding.buttonQuestionGallery.setEnabled(
                false
        );

        binding.buttonRemoveQuestionImage.setEnabled(
                false
        );

        binding.inputQuestion.setEndIconVisible(
                false
        );

        binding.inputQuestion.setHelperText(
                null
        );

        binding.buttonOpenLesson.setVisibility(
                View.GONE
        );

        clearPreviousAnswer();
    }

    private void showLoadingState(
            boolean loading
    ) {
        binding.progressAskSaathi.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.contentAskSaathi.setVisibility(
                loading
                        ? View.INVISIBLE
                        : View.VISIBLE
        );
    }

    private boolean isActivityAvailable() {
        return !isFinishing()
                && !isDestroyed();
    }

    private void closeQuestionTextRecognizers() {
        if (questionLatinRecognizer != null) {
            questionLatinRecognizer.close();

            questionLatinRecognizer =
                    null;
        }

        if (questionDevanagariRecognizer != null) {
            questionDevanagariRecognizer.close();

            questionDevanagariRecognizer =
                    null;
        }
    }

    @Override
    protected void onDestroy() {
        questionImageOcrGeneration++;

        aiAnswerRequestGeneration++;

        questionImageOcrInProgress =
                false;

        aiAnswerRequestInProgress =
                false;

        closeQuestionTextRecognizers();

        questionImageBitmapLoader.close();

        super.onDestroy();
    }

    @NonNull
    private String normalizeText(
            @Nullable String value
    ) {
        return safeText(
                value
        )
                .toLowerCase(
                        Locale.ROOT
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
                .trim();
    }

    private enum QuestionLanguageMode {

        DEVANAGARI_SANSKRIT,

        DEVANAGARI_HINDI,

        LATIN_PREFERRED,

        MIXED
    }

    private static final class QuestionOcrSelection {

        @NonNull
        private final String selectedText;

        @NonNull
        private final String sourceLabel;

        private final int qualityEstimate;

        private final boolean manualReviewRequired;

        private final boolean fallbackUsed;

        @NonNull
        private final QuestionLanguageMode languageMode;

        private QuestionOcrSelection(
                @NonNull String selectedText,
                @NonNull String sourceLabel,
                int qualityEstimate,
                boolean manualReviewRequired,
                boolean fallbackUsed,
                @NonNull QuestionLanguageMode languageMode
        ) {
            this.selectedText =
                    selectedText.trim();

            this.sourceLabel =
                    sourceLabel.trim();

            this.qualityEstimate =
                    Math.max(
                            0,
                            Math.min(
                                    100,
                                    qualityEstimate
                            )
                    );

            this.manualReviewRequired =
                    manualReviewRequired;

            this.fallbackUsed =
                    fallbackUsed;

            this.languageMode =
                    languageMode;
        }

        @NonNull
        private String getSelectedText() {
            return selectedText;
        }

        private boolean isManualReviewRequired() {
            return manualReviewRequired;
        }

        @NonNull
        private String createStatusMessage() {
            StringBuilder message =
                    new StringBuilder();

            message.append(
                    sourceLabel
            );

            message.append(
                    " पूरा हुआ। OCR quality estimate: "
            );

            message.append(
                    qualityEstimate
            );

            message.append(
                    "%."
            );

            if (languageMode
                    == QuestionLanguageMode.DEVANAGARI_SANSKRIT) {

                message.append(
                        " Sanskrit के संयुक्त अक्षर और मात्राएँ जरूर जाँचें।"
                );

            } else if (fallbackUsed) {
                message.append(
                        " Preferred script नहीं मिला, इसलिए fallback text लिया गया है।"
                );

            } else {
                message.append(
                        " Ask दबाने से पहले text एक बार जाँचें।"
                );
            }

            return message.toString();
        }
    }
}
