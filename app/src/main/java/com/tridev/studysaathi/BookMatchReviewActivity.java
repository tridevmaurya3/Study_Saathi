package com.tridev.studysaathi;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.content.mapper.BookMatchReviewMapper;
import com.tridev.studysaathi.data.content.model.BookMatchReviewData;
import com.tridev.studysaathi.data.local.entity.SchoolBookEntity;
import com.tridev.studysaathi.data.local.entity.SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.repository.SchoolBookRepository;
import com.tridev.studysaathi.data.repository.SchoolCurriculumProfileRepository;
import com.tridev.studysaathi.data.repository.SchoolSubjectRepository;
import com.tridev.studysaathi.databinding.ActivityBookMatchReviewBinding;

import java.util.List;
import java.util.Locale;

public final class BookMatchReviewActivity
        extends AppCompatActivity {

    public static final String EXTRA_BOOK_MATCH_REVIEW_DATA =
            "extra_book_match_review_data";

    public static final String EXTRA_TARGET_SUBJECT_ROW_ID =
            "extra_target_subject_row_id";

    public static final String EXTRA_TARGET_PROFILE_ID =
            "extra_target_profile_id";

    public static final String EXTRA_TARGET_SUBJECT_NAME =
            "extra_target_subject_name";

    private ActivityBookMatchReviewBinding binding;

    private SchoolBookRepository schoolBookRepository;

    private SchoolSubjectRepository schoolSubjectRepository;

    private SchoolCurriculumProfileRepository
            curriculumProfileRepository;

    @Nullable
    private BookMatchReviewData reviewData;

    @Nullable
    private SchoolSubjectEntity targetSubject;

    @Nullable
    private SchoolCurriculumProfileEntity curriculumProfile;

    private long targetSubjectRowId;

    private long targetProfileId;

    @NonNull
    private String targetSubjectName =
            "";

    private boolean saveOperationInProgress;

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            @NonNull BookMatchReviewData reviewData
    ) {
        Intent intent =
                new Intent(
                        context,
                        BookMatchReviewActivity.class
                );

        intent.putExtra(
                EXTRA_BOOK_MATCH_REVIEW_DATA,
                reviewData
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
                ActivityBookMatchReviewBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        schoolBookRepository =
                new SchoolBookRepository(
                        this
                );

        schoolSubjectRepository =
                new SchoolSubjectRepository(
                        this
                );

        curriculumProfileRepository =
                new SchoolCurriculumProfileRepository(
                        this
                );

        readTargetContextFromIntent();
        setupToolbar();
        setupClickListeners();

        reviewData =
                readReviewDataFromIntent();

        if (reviewData == null) {
            showMissingReviewDataError();

            return;
        }

        displayReviewData(
                reviewData
        );

        loadTargetSubjectAndCurriculum();
    }

    private void readTargetContextFromIntent() {
        Intent intent =
                getIntent();

        if (intent == null) {
            return;
        }

        targetSubjectRowId =
                intent.getLongExtra(
                        EXTRA_TARGET_SUBJECT_ROW_ID,
                        0L
                );

        targetProfileId =
                intent.getLongExtra(
                        EXTRA_TARGET_PROFILE_ID,
                        0L
                );

        targetSubjectName =
                safeText(
                        intent.getStringExtra(
                                EXTRA_TARGET_SUBJECT_NAME
                        )
                );
    }

    private void setupToolbar() {
        binding.toolbarBookMatchReview
                .setNavigationOnClickListener(
                        view -> {
                            if (saveOperationInProgress) {
                                showMessage(
                                        "Book save operation पूरी होने तक प्रतीक्षा करें।"
                                );

                                return;
                            }

                            getOnBackPressedDispatcher()
                                    .onBackPressed();
                        }
                );

        updateToolbarSubjectSubtitle();
    }

    private void updateToolbarSubjectSubtitle() {
        if (binding == null
                || targetSubjectName.isEmpty()) {
            return;
        }

        binding.toolbarBookMatchReview.setSubtitle(
                "Subject: "
                        + targetSubjectName
        );
    }

    private void setupClickListeners() {
        binding.buttonPreviewOnlineBook
                .setOnClickListener(
                        view ->
                                openOnlineBookPreview()
                );

        binding.buttonConfirmAndAddBook
                .setOnClickListener(
                        view ->
                                confirmAndAddBook()
                );

        binding.buttonEditBookInformation
                .setOnClickListener(
                        view ->
                                showBookInformationEditorMessage()
                );

        binding.buttonScanAnotherCover
                .setOnClickListener(
                        view -> {
                            if (saveOperationInProgress) {
                                showMessage(
                                        "Book save operation पूरी होने तक प्रतीक्षा करें।"
                                );

                                return;
                            }

                            finish();
                        }
                );
    }

    @Nullable
    private BookMatchReviewData
    readReviewDataFromIntent() {
        Intent intent =
                getIntent();

        if (intent == null) {
            return null;
        }

        try {
            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                return intent.getSerializableExtra(
                        EXTRA_BOOK_MATCH_REVIEW_DATA,
                        BookMatchReviewData.class
                );
            }

            Object serializableData =
                    intent.getSerializableExtra(
                            EXTRA_BOOK_MATCH_REVIEW_DATA
                    );

            if (serializableData
                    instanceof BookMatchReviewData) {

                return (BookMatchReviewData)
                        serializableData;
            }

        } catch (RuntimeException ignored) {
            /*
             * Invalid या incomplete Intent data के
             * कारण Activity crash नहीं होनी चाहिए।
             */
        }

        return null;
    }

    private void loadTargetSubjectAndCurriculum() {
        if (targetSubjectRowId <= 0L) {
            updateConfirmButtonState();
            showMissingTargetSubjectError();

            return;
        }

        schoolSubjectRepository.getSubjectByRowId(
                targetSubjectRowId,
                new SchoolSubjectRepository
                        .SingleSubjectCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable SchoolSubjectEntity
                                    schoolSubject
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        if (schoolSubject == null) {
                            updateConfirmButtonState();
                            showMissingTargetSubjectError();

                            return;
                        }

                        if (targetProfileId > 0L
                                && targetProfileId
                                != schoolSubject
                                .getProfileId()) {

                            updateConfirmButtonState();
                            showTargetProfileMismatchError();

                            return;
                        }

                        targetSubject =
                                schoolSubject;

                        targetProfileId =
                                schoolSubject.getProfileId();

                        if (targetSubjectName.isEmpty()) {
                            targetSubjectName =
                                    getSubjectDisplayName(
                                            schoolSubject
                                    );

                            updateToolbarSubjectSubtitle();
                        }

                        loadCurriculumProfile(
                                schoolSubject.getProfileId()
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        updateConfirmButtonState();

                        showMessage(
                                "Selected school subject load नहीं हो सका।"
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
                                    updateConfirmButtonState();

                                    showMessage(
                                            "Student का school curriculum profile उपलब्ध नहीं है।"
                                    );

                                    return;
                                }

                                curriculumProfile =
                                        profile;

                                updateConfirmButtonState();
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (!isActivityAvailable()) {
                                    return;
                                }

                                updateConfirmButtonState();

                                showMessage(
                                        "School curriculum profile load नहीं हो सका।"
                                );
                            }
                        }
                );
    }

    private void displayReviewData(
            @NonNull BookMatchReviewData data
    ) {
        displayReviewStatus(
                data
        );

        displayBookCover(
                data
        );

        displayDetectedInformation(
                data
        );

        displayOnlineMatch(
                data
        );

        displayReviewWarnings(
                data.getReviewWarnings()
        );

        updateConfirmButtonState();
    }

    private void updateConfirmButtonState() {
        if (binding == null) {
            return;
        }

        BookMatchReviewData data =
                reviewData;

        boolean readyToConfirm =
                !saveOperationInProgress
                        && data != null
                        && data.canConfirmBook()
                        && targetSubject != null
                        && curriculumProfile != null
                        && targetSubjectRowId > 0L;

        binding.buttonConfirmAndAddBook
                .setEnabled(
                        readyToConfirm
                );
    }

    private void displayReviewStatus(
            @NonNull BookMatchReviewData data
    ) {
        if (!data.isBestMatchAvailable()) {
            binding.textReviewStatusIcon.setText(
                    "!"
            );

            binding.textReviewStatusTitle.setText(
                    "सही online match नहीं मिला"
            );

            binding.textReviewStatusDescription.setText(
                    "Detected information जाँचें या book details manually सुधारें।"
            );

            return;
        }

        if (data.isHighConfidenceMatch()) {
            binding.textReviewStatusIcon.setText(
                    "✓"
            );

            binding.textReviewStatusTitle.setText(
                    "Book match मिल गया"
            );

            binding.textReviewStatusDescription.setText(
                    "Details जाँचकर parent confirmation दें।"
            );

            return;
        }

        binding.textReviewStatusIcon.setText(
                "?"
        );

        binding.textReviewStatusTitle.setText(
                "Parent review आवश्यक है"
        );

        binding.textReviewStatusDescription.setText(
                "Online result मिला है, लेकिन इसकी जानकारी ध्यान से जाँचें।"
        );
    }

    private void displayBookCover(
            @NonNull BookMatchReviewData data
    ) {
        binding.textReviewedBookTitle.setText(
                data.getPreferredBookTitle()
        );

        binding.textReviewedBookSubtitle.setText(
                createBookSubtitle(
                        data
                )
        );

        if (data.isBestMatchAvailable()) {
            binding.textMatchConfidenceChip.setText(
                    String.format(
                            Locale.getDefault(),
                            "Match: %.1f%%",
                            data.getOverallMatchScore()
                    )
            );

        } else {
            binding.textMatchConfidenceChip.setText(
                    "Match: Not available"
            );
        }

        if (!data.hasSelectedImageUri()) {
            showCoverPlaceholder();

            return;
        }

        try {
            Uri selectedImageUri =
                    Uri.parse(
                            data.getSelectedImageUri()
                    );

            binding.imageReviewedBookCover
                    .setImageURI(
                            null
                    );

            binding.imageReviewedBookCover
                    .setImageURI(
                            selectedImageUri
                    );

            binding.imageReviewedBookCover
                    .setVisibility(
                            View.VISIBLE
                    );

            binding.textReviewedCoverPlaceholder
                    .setVisibility(
                            View.GONE
                    );

        } catch (RuntimeException exception) {
            showCoverPlaceholder();
        }
    }

    private void showCoverPlaceholder() {
        binding.imageReviewedBookCover
                .setImageDrawable(
                        null
                );

        binding.imageReviewedBookCover
                .setVisibility(
                        View.GONE
                );

        binding.textReviewedCoverPlaceholder
                .setVisibility(
                        View.VISIBLE
                );
    }

    @NonNull
    private String createBookSubtitle(
            @NonNull BookMatchReviewData data
    ) {
        StringBuilder subtitleBuilder =
                new StringBuilder();

        appendSubtitlePart(
                subtitleBuilder,
                data.getPreferredSubjectName()
        );

        appendSubtitlePart(
                subtitleBuilder,
                data.getPreferredClassName()
        );

        appendSubtitlePart(
                subtitleBuilder,
                data.getPreferredPublisherName()
        );

        if (subtitleBuilder.length() == 0) {
            return "Book information requires review";
        }

        return subtitleBuilder.toString();
    }

    private void appendSubtitlePart(
            @NonNull StringBuilder builder,
            @Nullable String value
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

    private void displayDetectedInformation(
            @NonNull BookMatchReviewData data
    ) {
        binding.textDetectedBookName.setText(
                createLabelValue(
                        "Book Name",
                        data.getDetectedBookTitle()
                )
        );

        binding.textDetectedSubject.setText(
                createLabelValue(
                        "Subject",
                        data.getDetectedSubjectName()
                )
        );

        binding.textDetectedClass.setText(
                createLabelValue(
                        "Class",
                        data.getDetectedClassName()
                )
        );

        binding.textDetectedBoard.setText(
                createLabelValue(
                        "Board",
                        data.getDetectedEducationBoard()
                )
        );

        binding.textDetectedPublisher.setText(
                createLabelValue(
                        "Publisher",
                        data.getDetectedPublisherName()
                )
        );

        binding.textDetectedIsbn.setText(
                createLabelValue(
                        "ISBN",
                        data.getDetectedIsbn()
                )
        );
    }

    private void displayOnlineMatch(
            @NonNull BookMatchReviewData data
    ) {
        if (!data.isBestMatchAvailable()) {
            binding.textOnlineBookTitle.setText(
                    "No reliable online match available"
            );

            binding.textOnlineBookAuthor.setText(
                    "Author: Not available"
            );

            binding.textOnlineBookPublisher.setText(
                    "Publisher: Not available"
            );

            binding.textOnlineBookIsbn.setText(
                    "ISBN: Not available"
            );

            binding.textOnlineBookSource.setText(
                    "Source: Not available"
            );

            binding.textOnlineBookAccess.setText(
                    "Access: Not available"
            );

            binding.buttonPreviewOnlineBook
                    .setVisibility(
                            View.GONE
                    );

            return;
        }

        binding.textOnlineBookTitle.setText(
                valueOrFallback(
                        data.getOnlineBookTitle(),
                        "Online book title not available"
                )
        );

        binding.textOnlineBookAuthor.setText(
                createLabelValue(
                        "Author",
                        data.getOnlineBookAuthors()
                )
        );

        binding.textOnlineBookPublisher.setText(
                createLabelValue(
                        "Publisher",
                        data.getOnlinePublisherName()
                )
        );

        binding.textOnlineBookIsbn.setText(
                createLabelValue(
                        "ISBN",
                        data.getOnlineIsbn()
                )
        );

        binding.textOnlineBookSource.setText(
                createLabelValue(
                        "Source",
                        data.getOnlineProviderName()
                )
        );

        binding.textOnlineBookAccess.setText(
                createOnlineAccessText(
                        data
                )
        );

        binding.buttonPreviewOnlineBook
                .setVisibility(
                        data.hasOnlinePreviewUrl()
                                || data.hasOnlineInformationUrl()
                                || data.hasOfficialSourceUrl()
                                ? View.VISIBLE
                                : View.GONE
                );
    }

    @NonNull
    private String createOnlineAccessText(
            @NonNull BookMatchReviewData data
    ) {
        String accessType =
                valueOrFallback(
                        data.getOnlineAccessType(),
                        "Metadata only"
                );

        if (data.hasAuthorizedDownloadUrl()) {
            return "Access: "
                    + accessType
                    + " • Authorized download available";
        }

        if (data.hasOnlinePreviewUrl()) {
            return "Access: "
                    + accessType
                    + " • Preview available";
        }

        return "Access: "
                + accessType;
    }

    private void displayReviewWarnings(
            @NonNull List<String> warnings
    ) {
        if (warnings.isEmpty()) {
            binding.cardBookReviewWarnings
                    .setVisibility(
                            View.GONE
                    );

            binding.textBookReviewWarnings.setText(
                    ""
            );

            return;
        }

        StringBuilder warningBuilder =
                new StringBuilder();

        for (String warning : warnings) {
            String safeWarning =
                    safeText(
                            warning
                    );

            if (safeWarning.isEmpty()) {
                continue;
            }

            if (warningBuilder.length() > 0) {
                warningBuilder.append(
                        '\n'
                );
            }

            warningBuilder.append(
                    "• "
            );

            warningBuilder.append(
                    safeWarning
            );
        }

        if (warningBuilder.length() == 0) {
            binding.cardBookReviewWarnings
                    .setVisibility(
                            View.GONE
                    );

            return;
        }

        binding.textBookReviewWarnings.setText(
                warningBuilder.toString()
        );

        binding.cardBookReviewWarnings
                .setVisibility(
                        View.VISIBLE
                );
    }

    private void openOnlineBookPreview() {
        BookMatchReviewData data =
                reviewData;

        if (data == null) {
            return;
        }

        String previewUrl =
                findPreferredOnlineUrl(
                        data
                );

        if (previewUrl.isEmpty()) {
            showMessage(
                    "इस book का online preview उपलब्ध नहीं है।"
            );

            return;
        }

        try {
            Intent browserIntent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    previewUrl
                            )
                    );

            startActivity(
                    browserIntent
            );

        } catch (RuntimeException exception) {
            showMessage(
                    "Online book link खोला नहीं जा सका।"
            );
        }
    }

    @NonNull
    private String findPreferredOnlineUrl(
            @NonNull BookMatchReviewData data
    ) {
        if (data.hasOnlinePreviewUrl()) {
            return data.getOnlinePreviewUrl();
        }

        if (data.hasOfficialSourceUrl()) {
            return data.getOnlineOfficialSourceUrl();
        }

        if (data.hasOnlineInformationUrl()) {
            return data.getOnlineInformationUrl();
        }

        return "";
    }

    private void confirmAndAddBook() {
        BookMatchReviewData data =
                reviewData;

        SchoolSubjectEntity subject =
                targetSubject;

        SchoolCurriculumProfileEntity profile =
                curriculumProfile;

        if (data == null
                || !data.canConfirmBook()) {

            showMessage(
                    "इस result को अभी confirm नहीं किया जा सकता।"
            );

            return;
        }

        if (subject == null
                || profile == null
                || targetSubjectRowId <= 0L) {

            showMessage(
                    "Selected school subject और curriculum अभी तैयार नहीं हैं।"
            );

            return;
        }

        StringBuilder confirmationMessage =
                new StringBuilder();

        confirmationMessage.append(
                "क्या आप “"
        );

        confirmationMessage.append(
                data.getPreferredBookTitle()
        );

        confirmationMessage.append(
                "” को "
        );

        confirmationMessage.append(
                getSubjectDisplayName(
                        subject
                )
        );

        confirmationMessage.append(
                " की primary school book बनाना चाहते हैं?"
        );

        if (!data.isHighConfidenceMatch()) {
            confirmationMessage.append(
                    "\n\nMatch confidence कम है। Cover, title, publisher, class और ISBN ध्यान से जाँचें।"
            );
        }

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "Book जोड़ने की पुष्टि"
                )
                .setMessage(
                        confirmationMessage.toString()
                )
                .setNegativeButton(
                        "अभी नहीं",
                        null
                )
                .setPositiveButton(
                        "Confirm",
                        (dialog, which) ->
                                checkDuplicateBeforeSave(
                                        data,
                                        subject,
                                        profile
                                )
                )
                .show();
    }

    private void checkDuplicateBeforeSave(
            @NonNull BookMatchReviewData data,
            @NonNull SchoolSubjectEntity subject,
            @NonNull SchoolCurriculumProfileEntity profile
    ) {
        setSaveOperationState(
                true
        );

        String preferredIsbn =
                normalizeIsbn(
                        data.getPreferredIsbn()
                );

        String isbn10 =
                preferredIsbn.length() == 10
                        ? preferredIsbn
                        : "";

        String isbn13 =
                preferredIsbn.length() == 13
                        ? preferredIsbn
                        : "";

        schoolBookRepository.findDuplicateBook(
                subject.getSubjectRowId(),
                isbn10,
                isbn13,
                data.getPreferredBookTitle(),
                data.getPreferredPublisherName(),
                new SchoolBookRepository
                        .DuplicateBookCallback() {

                    @Override
                    public void onResult(
                            @Nullable SchoolBookEntity
                                    duplicateBook,
                            @NonNull SchoolBookRepository
                                    .DuplicateMatchType matchType
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        if (duplicateBook != null) {
                            setSaveOperationState(
                                    false
                            );

                            showDuplicateBookDialog(
                                    duplicateBook,
                                    matchType
                            );

                            return;
                        }

                        requestNextBookSortOrder(
                                data,
                                subject,
                                profile
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setSaveOperationState(
                                false
                        );

                        showMessage(
                                "Duplicate book check पूरी नहीं हो सकी।"
                        );
                    }
                }
        );
    }

    private void showDuplicateBookDialog(
            @NonNull SchoolBookEntity duplicateBook,
            @NonNull SchoolBookRepository
                    .DuplicateMatchType matchType
    ) {
        String matchReason;

        switch (matchType) {
            case ISBN_13:
                matchReason =
                        "ISBN-13 match";
                break;

            case ISBN_10:
                matchReason =
                        "ISBN-10 match";
                break;

            case TITLE_AND_PUBLISHER:
                matchReason =
                        "Title और publisher match";
                break;

            case NONE:
            default:
                matchReason =
                        "Same book details";
                break;
        }

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "यह book पहले से जुड़ी है"
                )
                .setMessage(
                        "“"
                                + duplicateBook.getBookTitle()
                                + "” इस subject में पहले से मौजूद है।\n\n"
                                + "Match: "
                                + matchReason
                                + "\n\nइसे primary school book बनाना चाहते हैं?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Use Existing Book",
                        (dialog, which) ->
                                makeBookPrimaryAndUpdateSubject(
                                        duplicateBook
                                )
                )
                .show();
    }

    private void requestNextBookSortOrder(
            @NonNull BookMatchReviewData data,
            @NonNull SchoolSubjectEntity subject,
            @NonNull SchoolCurriculumProfileEntity profile
    ) {
        schoolBookRepository.getNextSortOrder(
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

                        createAndInsertConfirmedBook(
                                data,
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

                        setSaveOperationState(
                                false
                        );

                        showMessage(
                                "Book का sort order तैयार नहीं हो सका।"
                        );
                    }
                }
        );
    }

    private void createAndInsertConfirmedBook(
            @NonNull BookMatchReviewData data,
            @NonNull SchoolSubjectEntity subject,
            @NonNull SchoolCurriculumProfileEntity profile,
            int nextSortOrder
    ) {
        final SchoolBookEntity schoolBook;

        try {
            schoolBook =
                    BookMatchReviewMapper
                            .createConfirmedBookEntity(
                                    data,
                                    subject.getSubjectRowId(),
                                    profile.getAcademicSession(),
                                    nextSortOrder,
                                    false
                            );

            /*
             * Online result की class/board/medium के बजाय
             * confirmed student curriculum values save होंगी।
             */
            schoolBook.setClassName(
                    createClassName(
                            profile.getClassNumber()
                    )
            );

            schoolBook.setEducationBoard(
                    profile.getEducationBoard()
            );

            schoolBook.setStudyMedium(
                    profile.getStudyMedium()
            );

            schoolBook.setAiTutorEnabled(
                    subject.isAiTutorEnabled()
            );

        } catch (RuntimeException exception) {
            setSaveOperationState(
                    false
            );

            showMessage(
                    "Confirmed book details database format में तैयार नहीं हो सकीं।"
            );

            return;
        }

        schoolBookRepository.insertBook(
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

                        makeNewBookPrimary(
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

                        setSaveOperationState(
                                false
                        );

                        showMessage(
                                "Book database में save नहीं हो सकी। संभव है कि यह पहले से मौजूद हो।"
                        );
                    }
                }
        );
    }

    private void makeNewBookPrimary(
            @NonNull SchoolBookEntity schoolBook
    ) {
        schoolBookRepository.setPrimaryBook(
                schoolBook.getSubjectRowId(),
                schoolBook.getBookRowId(),
                new SchoolBookRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        schoolBook.setPrimaryBook(
                                true
                        );

                        updateSubjectBookSummary(
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

                        rollbackInsertedBook(
                                schoolBook
                        );
                    }
                }
        );
    }

    private void rollbackInsertedBook(
            @NonNull SchoolBookEntity schoolBook
    ) {
        schoolBookRepository.deleteBook(
                schoolBook,
                new SchoolBookRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setSaveOperationState(
                                false
                        );

                        showMessage(
                                "Book primary नहीं बन सकी, इसलिए अधूरी entry हटा दी गई। दोबारा कोशिश करें।"
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setSaveOperationState(
                                false
                        );

                        showMessage(
                                "Book save हुई, लेकिन primary status पूरा नहीं हो सका। Curriculum screen दोबारा खोलें।"
                        );
                    }
                }
        );
    }

    private void makeBookPrimaryAndUpdateSubject(
            @NonNull SchoolBookEntity schoolBook
    ) {
        setSaveOperationState(
                true
        );

        schoolBookRepository.setPrimaryBook(
                schoolBook.getSubjectRowId(),
                schoolBook.getBookRowId(),
                new SchoolBookRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        schoolBook.setPrimaryBook(
                                true
                        );

                        updateSubjectBookSummary(
                                schoolBook,
                                true
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setSaveOperationState(
                                false
                        );

                        showMessage(
                                "Existing book को primary नहीं बनाया जा सका।"
                        );
                    }
                }
        );
    }

    private void updateSubjectBookSummary(
            @NonNull SchoolBookEntity schoolBook,
            boolean existingBookUsed
    ) {
        schoolSubjectRepository
                .updateSubjectBookInformation(
                        schoolBook.getSubjectRowId(),
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
                                        existingBookUsed,
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

                                finishSuccessfulSave(
                                        schoolBook,
                                        existingBookUsed,
                                        true
                                );
                            }
                        }
                );
    }

    private void finishSuccessfulSave(
            @NonNull SchoolBookEntity schoolBook,
            boolean existingBookUsed,
            boolean subjectSummaryWarning
    ) {
        setSaveOperationState(
                false
        );

        Intent resultIntent =
                new Intent();

        resultIntent.putExtra(
                ManualSchoolBookActivity.RESULT_BOOK_ROW_ID,
                schoolBook.getBookRowId()
        );

        resultIntent.putExtra(
                ManualSchoolBookActivity.RESULT_SUBJECT_ROW_ID,
                schoolBook.getSubjectRowId()
        );

        resultIntent.putExtra(
                ManualSchoolBookActivity.RESULT_BOOK_TITLE,
                schoolBook.getBookTitle()
        );

        setResult(
                RESULT_OK,
                resultIntent
        );

        StringBuilder message =
                new StringBuilder();

        message.append(
                "“"
        );

        message.append(
                schoolBook.getBookTitle()
        );

        message.append(
                "” को "
        );

        message.append(
                targetSubjectName.isEmpty()
                        ? "selected subject"
                        : targetSubjectName
        );

        if (existingBookUsed) {
            message.append(
                    " की primary school book बना दिया गया है।"
            );

        } else {
            message.append(
                    " की confirmed primary school book के रूप में save कर दिया गया है।"
            );
        }

        if (subjectSummaryWarning) {
            message.append(
                    "\n\nBook save हो गई है, लेकिन subject card summary तुरंत update नहीं हो सकी। Curriculum screen reload होने पर यह सही हो जाएगी।"
            );
        }

        message.append(
                "\n\nअगले चरण में इसी exact book के Contents/Index से chapters जोड़े जाएँगे।"
        );

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "School book confirm हो गई"
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

    private void setSaveOperationState(
            boolean inProgress
    ) {
        saveOperationInProgress =
                inProgress;

        binding.buttonPreviewOnlineBook
                .setEnabled(
                        !inProgress
                );

        binding.buttonEditBookInformation
                .setEnabled(
                        !inProgress
                );

        binding.buttonScanAnotherCover
                .setEnabled(
                        !inProgress
                );

        binding.buttonConfirmAndAddBook
                .setText(
                        inProgress
                                ? "Book save की जा रही है…"
                                : "Confirm करके Book जोड़ें"
                );

        updateConfirmButtonState();
    }

    private void showBookInformationEditorMessage() {
        if (targetSubjectRowId > 0L) {
            new MaterialAlertDialogBuilder(
                    this
            )
                    .setTitle(
                            "Book information सुधारें"
                    )
                    .setMessage(
                            "Online result सही न हो तो वापस जाकर Add Book Manually चुनें। Manual form में exact title, publisher, edition, ISBN और cover भरे जा सकते हैं।"
                    )
                    .setPositiveButton(
                            "ठीक है",
                            null
                    )
                    .show();

            return;
        }

        showMessage(
                "Book Information Editor आगे जोड़ा जाएगा।"
        );
    }

    private void showMissingTargetSubjectError() {
        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "School subject उपलब्ध नहीं है"
                )
                .setMessage(
                        "इस book result को save करने के लिए Parent Curriculum Setup से actual school subject चुनकर scan दोबारा शुरू करें।"
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

    private void showTargetProfileMismatchError() {
        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "Curriculum match नहीं हुआ"
                )
                .setMessage(
                        "Selected subject इस student curriculum से संबंधित नहीं है। Book को गलत profile में save होने से रोक दिया गया है।"
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

    private void showMissingReviewDataError() {
        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "Review data उपलब्ध नहीं है"
                )
                .setMessage(
                        "Book scan result नहीं मिला। कृपया cover दोबारा scan करें।"
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
    private String createClassName(
            int classNumber
    ) {
        if (classNumber >= 1
                && classNumber <= 12) {

            return "Class "
                    + classNumber;
        }

        return "";
    }

    @NonNull
    private String normalizeIsbn(
            @Nullable String isbn
    ) {
        return safeText(
                isbn
        )
                .replaceAll(
                        "[^0-9Xx]",
                        ""
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }

    @NonNull
    private String createLabelValue(
            @NonNull String label,
            @Nullable String value
    ) {
        return label
                + ": "
                + valueOrFallback(
                value,
                "Not detected"
        );
    }

    @NonNull
    private String valueOrFallback(
            @Nullable String value,
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

    private void showMessage(
            @NonNull String message
    ) {
        if (binding == null) {
            return;
        }

        Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }

    @NonNull
    private String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private boolean isActivityAvailable() {
        return binding != null
                && !isFinishing()
                && !isDestroyed();
    }

    @Override
    protected void onDestroy() {
        binding =
                null;

        super.onDestroy();
    }
}