package com.tridev.studysaathi;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterContentEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterContentRepository;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterRepository;
import com.tridev.studysaathi.databinding
        .ActivitySchoolBookChapterContentEditorBinding;
import com.tridev.studysaathi.validation
        .SchoolBookChapterContentValidator;

import java.util.Arrays;
import java.util.List;

public final class SchoolBookChapterContentEditorActivity
        extends AppCompatActivity {

    public static final String EXTRA_CHAPTER_ROW_ID =
            "extra_content_chapter_row_id";

    private static final long INVALID_ROW_ID =
            0L;

    private ActivitySchoolBookChapterContentEditorBinding binding;

    private SchoolBookChapterRepository chapterRepository;

    private SchoolBookChapterContentRepository contentRepository;

    private long chapterRowId =
            INVALID_ROW_ID;

    @Nullable
    private SchoolBookChapterEntity currentChapter;

    @Nullable
    private SchoolBookChapterContentEntity currentContent;

    private boolean operationInProgress;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivitySchoolBookChapterContentEditorBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        chapterRepository =
                new SchoolBookChapterRepository(
                        this
                );

        contentRepository =
                new SchoolBookChapterContentRepository(
                        this
                );

        chapterRowId =
                getIntent().getLongExtra(
                        EXTRA_CHAPTER_ROW_ID,
                        INVALID_ROW_ID
                );

        setupToolbar();
        setupLanguageDropdown();
        setupButtons();

        if (chapterRowId <= INVALID_ROW_ID) {
            showFatalError(
                    "A valid exact chapter is required."
            );

            return;
        }

        loadChapter();
    }

    private void setupToolbar() {
        binding.chapterContentToolbar
                .setNavigationOnClickListener(view ->
                        getOnBackPressedDispatcher()
                                .onBackPressed()
                );
    }

    private void setupLanguageDropdown() {
        List<String> languageItems =
                Arrays.asList(
                        "English",
                        "Hindi",
                        "Bilingual"
                );

        ArrayAdapter<String> languageAdapter =
                new ArrayAdapter<>(
                        this,
                        R.layout.item_professional_dropdown,
                        languageItems
                );

        binding.chapterContentLanguageDropdown
                .setAdapter(
                        languageAdapter
                );

        binding.chapterContentLanguageDropdown
                .setText(
                        "Bilingual",
                        false
                );
    }

    private void setupButtons() {
        binding.saveChapterContentDraftButton
                .setOnClickListener(view ->
                        saveDraft(
                                false
                        )
                );

        binding.submitChapterContentReviewButton
                .setOnClickListener(view ->
                        saveDraft(
                                true
                        )
                );

        binding.approveChapterContentButton
                .setOnClickListener(view ->
                        approveCurrentContent()
                );
    }

    private void loadChapter() {
        setLoading(
                true
        );

        chapterRepository.getChapterByRowId(
                chapterRowId,
                new SchoolBookChapterRepository
                        .SingleChapterCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable SchoolBookChapterEntity chapter
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        if (chapter == null) {
                            showFatalError(
                                    "The selected chapter was not found."
                            );

                            return;
                        }

                        currentChapter =
                                chapter;

                        binding.chapterContentTitleTextView
                                .setText(
                                        chapter.getDisplayTitle()
                                );

                        loadExistingContent();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        showFatalError(
                                getErrorMessage(
                                        exception,
                                        "Chapter could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    private void loadExistingContent() {
        contentRepository.getContentForChapter(
                chapterRowId,
                new SchoolBookChapterContentRepository
                        .SingleContentCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable SchoolBookChapterContentEntity content
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        currentContent =
                                content == null
                                        ? createEmptyContent()
                                        : content;

                        bindContent(
                                currentContent
                        );

                        setLoading(
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

                        setLoading(
                                false
                        );

                        showMessage(
                                getErrorMessage(
                                        exception,
                                        "Chapter content could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    @NonNull
    private SchoolBookChapterContentEntity createEmptyContent() {
        SchoolBookChapterContentEntity content =
                new SchoolBookChapterContentEntity();

        content.prepareForNewDraft(
                chapterRowId
        );

        content.setLanguageMode(
                SchoolBookChapterContentEntity
                        .LANGUAGE_MODE_BILINGUAL
        );

        content.setContentSource(
                SchoolBookChapterContentEntity
                        .CONTENT_SOURCE_PARENT_MANUAL
        );

        return content;
    }

    private void bindContent(
            @NonNull SchoolBookChapterContentEntity content
    ) {
        binding.chapterContentLanguageDropdown.setText(
                getLanguageDisplayName(
                        content.getLanguageMode()
                ),
                false
        );

        binding.chapterIntroductionEnglishEditText.setText(
                content.getChapterIntroductionEnglish()
        );

        binding.chapterIntroductionHindiEditText.setText(
                content.getChapterIntroductionHindi()
        );

        binding.detailedExplanationEnglishEditText.setText(
                content.getDetailedExplanationEnglish()
        );

        binding.detailedExplanationHindiEditText.setText(
                content.getDetailedExplanationHindi()
        );

        binding.keyPointsEnglishEditText.setText(
                content.getKeyPointsEnglish()
        );

        binding.keyPointsHindiEditText.setText(
                content.getKeyPointsHindi()
        );

        binding.importantTermsEnglishEditText.setText(
                content.getImportantTermsEnglish()
        );

        binding.importantTermsHindiEditText.setText(
                content.getImportantTermsHindi()
        );

        binding.workedExamplesEnglishEditText.setText(
                content.getWorkedExamplesEnglish()
        );

        binding.workedExamplesHindiEditText.setText(
                content.getWorkedExamplesHindi()
        );

        binding.chapterSummaryEnglishEditText.setText(
                content.getChapterSummaryEnglish()
        );

        binding.chapterSummaryHindiEditText.setText(
                content.getChapterSummaryHindi()
        );

        int readingMinutes =
                content.getEstimatedReadingMinutes();

        binding.estimatedReadingMinutesEditText.setText(
                readingMinutes <= 0
                        ? ""
                        : String.valueOf(
                        readingMinutes
                )
        );

        showReviewStatus(
                content
        );
    }

    private void saveDraft(
            boolean submitAfterSave
    ) {
        if (operationInProgress) {
            return;
        }

        SchoolBookChapterContentEntity content =
                collectFormContent();

        SchoolBookChapterContentValidator.ValidationResult
                validationResult =
                submitAfterSave
                        ? SchoolBookChapterContentValidator
                        .validateForReview(
                                chapterRowId,
                                content
                        )
                        : SchoolBookChapterContentValidator
                        .validateDraft(
                                chapterRowId,
                                content
                        );

        if (!validationResult.isValid()) {
            showMessage(
                    validationResult.getMessage()
            );

            return;
        }

        setLoading(
                true
        );

        contentRepository.saveDraft(
                chapterRowId,
                content,
                new SchoolBookChapterContentRepository
                        .SaveContentCallback() {

                    @Override
                    public void onSuccess(
                            long contentRowId,
                            boolean created
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        content.setContentRowId(
                                contentRowId
                        );

                        currentContent =
                                content;

                        if (submitAfterSave) {
                            submitSavedContentForReview();

                        } else {
                            setLoading(
                                    false
                            );

                            showReviewStatus(
                                    content
                            );

                            showMessage(
                                    created
                                            ? "Chapter content draft created."
                                            : "Chapter content draft saved."
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

                        setLoading(
                                false
                        );

                        showMessage(
                                getErrorMessage(
                                        exception,
                                        "Chapter content could not be saved."
                                )
                        );
                    }
                }
        );
    }

    private void submitSavedContentForReview() {
        contentRepository.submitForParentReview(
                chapterRowId,
                new SchoolBookChapterContentRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setLoading(
                                false
                        );

                        if (currentContent != null) {
                            currentContent
                                    .markPendingParentReview();

                            showReviewStatus(
                                    currentContent
                            );
                        }

                        showMessage(
                                "Content submitted for Parent review."
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setLoading(
                                false
                        );

                        showMessage(
                                getErrorMessage(
                                        exception,
                                        "Content could not be submitted."
                                )
                        );
                    }
                }
        );
    }

    private void approveCurrentContent() {
        if (operationInProgress) {
            return;
        }

        SchoolBookChapterContentEntity content =
                collectFormContent();

        SchoolBookChapterContentValidator.ValidationResult
                validationResult =
                SchoolBookChapterContentValidator
                        .validateForApproval(
                                chapterRowId,
                                content
                        );

        if (!validationResult.isValid()) {
            showMessage(
                    validationResult.getMessage()
            );

            return;
        }

        setLoading(
                true
        );

        contentRepository.approveContent(
                chapterRowId,
                new SchoolBookChapterContentRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        content.markParentApproved();

                        currentContent =
                                content;

                        showReviewStatus(
                                content
                        );

                        setLoading(
                                false
                        );

                        showMessage(
                                "Content approved for Child Mode."
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!isActivityAvailable()) {
                            return;
                        }

                        setLoading(
                                false
                        );

                        showMessage(
                                getErrorMessage(
                                        exception,
                                        "Content could not be approved."
                                )
                        );
                    }
                }
        );
    }

    @NonNull
    private SchoolBookChapterContentEntity collectFormContent() {
        SchoolBookChapterContentEntity content =
                currentContent == null
                        ? createEmptyContent()
                        : currentContent;

        content.setChapterRowId(
                chapterRowId
        );

        content.setLanguageMode(
                getSelectedLanguageMode()
        );

        content.setChapterIntroductionEnglish(
                textOf(
                        binding.chapterIntroductionEnglishEditText
                                .getText()
                )
        );

        content.setChapterIntroductionHindi(
                textOf(
                        binding.chapterIntroductionHindiEditText
                                .getText()
                )
        );

        content.setDetailedExplanationEnglish(
                textOf(
                        binding.detailedExplanationEnglishEditText
                                .getText()
                )
        );

        content.setDetailedExplanationHindi(
                textOf(
                        binding.detailedExplanationHindiEditText
                                .getText()
                )
        );

        content.setKeyPointsEnglish(
                textOf(
                        binding.keyPointsEnglishEditText
                                .getText()
                )
        );

        content.setKeyPointsHindi(
                textOf(
                        binding.keyPointsHindiEditText
                                .getText()
                )
        );

        content.setImportantTermsEnglish(
                textOf(
                        binding.importantTermsEnglishEditText
                                .getText()
                )
        );

        content.setImportantTermsHindi(
                textOf(
                        binding.importantTermsHindiEditText
                                .getText()
                )
        );

        content.setWorkedExamplesEnglish(
                textOf(
                        binding.workedExamplesEnglishEditText
                                .getText()
                )
        );

        content.setWorkedExamplesHindi(
                textOf(
                        binding.workedExamplesHindiEditText
                                .getText()
                )
        );

        content.setChapterSummaryEnglish(
                textOf(
                        binding.chapterSummaryEnglishEditText
                                .getText()
                )
        );

        content.setChapterSummaryHindi(
                textOf(
                        binding.chapterSummaryHindiEditText
                                .getText()
                )
        );

        content.setEstimatedReadingMinutes(
                parsePositiveInteger(
                        textOf(
                                binding.estimatedReadingMinutesEditText
                                        .getText()
                        )
                )
        );

        content.setUpdatedAt(
                System.currentTimeMillis()
        );

        currentContent =
                content;

        return content;
    }

    @NonNull
    private String getSelectedLanguageMode() {
        String selection =
                textOf(
                        binding.chapterContentLanguageDropdown
                                .getText()
                );

        if ("English".equalsIgnoreCase(
                selection
        )) {
            return SchoolBookChapterContentEntity
                    .LANGUAGE_MODE_ENGLISH;
        }

        if ("Hindi".equalsIgnoreCase(
                selection
        )) {
            return SchoolBookChapterContentEntity
                    .LANGUAGE_MODE_HINDI;
        }

        return SchoolBookChapterContentEntity
                .LANGUAGE_MODE_BILINGUAL;
    }

    @NonNull
    private String getLanguageDisplayName(
            @Nullable String languageMode
    ) {
        if (SchoolBookChapterContentEntity
                .LANGUAGE_MODE_ENGLISH.equals(
                        languageMode
                )) {
            return "English";
        }

        if (SchoolBookChapterContentEntity
                .LANGUAGE_MODE_HINDI.equals(
                        languageMode
                )) {
            return "Hindi";
        }

        return "Bilingual";
    }

    private void showReviewStatus(
            @NonNull SchoolBookChapterContentEntity content
    ) {
        String displayStatus =
                content.getReviewStatus()
                        .replace(
                                '_',
                                ' '
                        );

        binding.chapterContentStatusTextView.setText(
                "Status: "
                        + displayStatus
        );

        boolean pendingReview =
                SchoolBookChapterContentEntity
                        .REVIEW_STATUS_PENDING_REVIEW
                        .equals(
                                content.getReviewStatus()
                        );

        binding.approveChapterContentButton.setEnabled(
                pendingReview
        );
    }

    private void setLoading(
            boolean loading
    ) {
        operationInProgress =
                loading;

        binding.chapterContentProgressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.saveChapterContentDraftButton.setEnabled(
                !loading
        );

        binding.submitChapterContentReviewButton.setEnabled(
                !loading
        );

        binding.approveChapterContentButton.setEnabled(
                !loading
                        && currentContent != null
                        && SchoolBookChapterContentEntity
                        .REVIEW_STATUS_PENDING_REVIEW
                        .equals(
                                currentContent.getReviewStatus()
                        )
        );
    }

    private void showFatalError(
            @NonNull String message
    ) {
        setLoading(
                false
        );

        showMessage(
                message
        );

        binding.saveChapterContentDraftButton.setEnabled(
                false
        );

        binding.submitChapterContentReviewButton.setEnabled(
                false
        );

        binding.approveChapterContentButton.setEnabled(
                false
        );
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

    @NonNull
    private String getErrorMessage(
            @NonNull Exception exception,
            @NonNull String fallbackMessage
    ) {
        String message =
                exception.getMessage();

        return message == null
                || message.trim().isEmpty()
                ? fallbackMessage
                : message.trim();
    }

    @NonNull
    private String textOf(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString().trim();
    }

    private int parsePositiveInteger(
            @Nullable String value
    ) {
        String safeValue =
                textOf(
                        value
                );

        if (safeValue.isEmpty()) {
            return 0;
        }

        try {
            return Math.max(
                    0,
                    Integer.parseInt(
                            safeValue
                    )
            );

        } catch (NumberFormatException ignored) {
            return 0;
        }
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