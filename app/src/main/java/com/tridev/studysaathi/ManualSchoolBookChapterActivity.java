package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tridev.studysaathi.data.content.mapper
        .SchoolBookChapterMapper;
import com.tridev.studysaathi.data.content.validation
        .SchoolBookChapterFormValidator;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterRepository;
import com.tridev.studysaathi.databinding
        .ActivityManualSchoolBookChapterBinding;

import java.util.Arrays;
import java.util.List;

public final class ManualSchoolBookChapterActivity
        extends AppCompatActivity {

    public static final String EXTRA_BOOK_ROW_ID =
            "extra_book_row_id";

    public static final String EXTRA_CHAPTER_ROW_ID =
            "extra_chapter_row_id";

    public static final String EXTRA_SAVED_CHAPTER_ROW_ID =
            "extra_saved_chapter_row_id";

    public static final String EXTRA_CHAPTER_CHANGED =
            "extra_chapter_changed";

    private static final long INVALID_ROW_ID =
            0L;

    @NonNull
    private final List<String> chapterTypes =
            Arrays.asList(
                    SchoolBookChapterEntity
                            .CHAPTER_TYPE_CHAPTER,

                    SchoolBookChapterEntity
                            .CHAPTER_TYPE_UNIT,

                    SchoolBookChapterEntity
                            .CHAPTER_TYPE_LESSON,

                    SchoolBookChapterEntity
                            .CHAPTER_TYPE_POEM,

                    SchoolBookChapterEntity
                            .CHAPTER_TYPE_STORY,

                    SchoolBookChapterEntity
                            .CHAPTER_TYPE_ACTIVITY,

                    SchoolBookChapterEntity
                            .CHAPTER_TYPE_PROJECT,

                    SchoolBookChapterEntity
                            .CHAPTER_TYPE_APPENDIX
            );

    private ActivityManualSchoolBookChapterBinding binding;

    private SchoolBookChapterRepository chapterRepository;

    private long bookRowId =
            INVALID_ROW_ID;

    private long chapterRowId =
            INVALID_ROW_ID;

    @Nullable
    private SchoolBookChapterEntity chapterBeingEdited;

    private boolean saving;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivityManualSchoolBookChapterBinding
                        .inflate(
                                getLayoutInflater()
                        );

        setContentView(
                binding.getRoot()
        );

        chapterRepository =
                new SchoolBookChapterRepository(
                        getApplicationContext()
                );

        readIntentValues();
        configureChapterTypeSpinner();
        configureButtons();

        if (!hasValidBook()) {
            showFatalError(
                    "A valid school book was not provided."
            );

            return;
        }

        if (isEditMode()) {
            loadChapterForEditing();

        } else {
            configureAddMode();
        }
    }

    private void readIntentValues() {
        Intent intent =
                getIntent();

        if (intent == null) {
            return;
        }

        bookRowId =
                intent.getLongExtra(
                        EXTRA_BOOK_ROW_ID,
                        INVALID_ROW_ID
                );

        chapterRowId =
                intent.getLongExtra(
                        EXTRA_CHAPTER_ROW_ID,
                        INVALID_ROW_ID
                );
    }

    private void configureChapterTypeSpinner() {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        chapterTypes
                );

        adapter.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item
        );

        binding.chapterTypeSpinner.setAdapter(
                adapter
        );

        setSelectedChapterType(
                SchoolBookChapterEntity
                        .CHAPTER_TYPE_CHAPTER
        );
    }

    private void configureButtons() {
        binding.cancelChapterButton
                .setOnClickListener(view ->
                        cancelAndClose()
                );

        binding.saveChapterButton
                .setOnClickListener(view ->
                        validateAndSaveChapter()
                );
    }

    private void configureAddMode() {
        binding.chapterFormTitleTextView
                .setText(
                        "Add School Book Chapter"
                );

        binding.chapterFormSubtitleTextView
                .setText(
                        "Enter the chapter exactly as it "
                                + "appears in the confirmed school book."
                );

        binding.saveChapterButton
                .setText(
                        "Save Chapter"
                );

        showLoading(
                false
        );
    }

    private void loadChapterForEditing() {
        showLoading(
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
                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        if (chapter == null) {
                            showFatalError(
                                    "The selected chapter was not found."
                            );

                            return;
                        }

                        if (chapter.getBookRowId()
                                != bookRowId) {

                            showFatalError(
                                    "The selected chapter does not "
                                            + "belong to this school book."
                            );

                            return;
                        }

                        chapterBeingEdited =
                                chapter;

                        populateForm(
                                chapter
                        );

                        configureEditMode();

                        showLoading(
                                false
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        showFatalError(
                                getErrorMessage(
                                        exception,
                                        "The chapter could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    private void configureEditMode() {
        binding.chapterFormTitleTextView
                .setText(
                        "Edit School Book Chapter"
                );

        binding.chapterFormSubtitleTextView
                .setText(
                        "Update the chapter information. "
                                + "Existing learning progress will be preserved."
                );

        binding.saveChapterButton
                .setText(
                        "Update Chapter"
                );
    }

    private void populateForm(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        binding.chapterNumberEditText
                .setText(
                        chapter.getChapterNumber()
                );

        binding.chapterTitleEnglishEditText
                .setText(
                        chapter.getChapterTitleEnglish()
                );

        binding.chapterTitleHindiEditText
                .setText(
                        chapter.getChapterTitleHindi()
                );

        binding.chapterSubtitleEditText
                .setText(
                        chapter.getChapterSubtitle()
                );

        binding.unitNameEditText
                .setText(
                        chapter.getUnitName()
                );

        setSelectedChapterType(
                chapter.getChapterType()
        );

        binding.startPageNumberEditText
                .setText(
                        pageNumberToText(
                                chapter.getStartPageNumber()
                        )
                );

        binding.endPageNumberEditText
                .setText(
                        pageNumberToText(
                                chapter.getEndPageNumber()
                        )
                );

        binding.optionalChapterCheckBox
                .setChecked(
                        chapter.isOptionalChapter()
                );

        binding.revisionChapterCheckBox
                .setChecked(
                        chapter.isRevisionChapter()
                );

        binding.chapterDescriptionEditText
                .setText(
                        chapter.getChapterDescription()
                );

        binding.learningObjectivesEditText
                .setText(
                        chapter.getLearningObjectives()
                );

        binding.importantTopicsEditText
                .setText(
                        chapter.getImportantTopics()
                );
    }

    private void validateAndSaveChapter() {
        if (saving) {
            return;
        }

        clearValidationErrors();

        SchoolBookChapterFormValidator.ValidationResult result =
                SchoolBookChapterFormValidator.validate(
                        bookRowId,
                        getText(
                                binding.chapterNumberEditText
                        ),
                        getText(
                                binding.chapterTitleEnglishEditText
                        ),
                        getText(
                                binding.chapterTitleHindiEditText
                        ),
                        getText(
                                binding.chapterSubtitleEditText
                        ),
                        getText(
                                binding.unitNameEditText
                        ),
                        getSelectedChapterType(),
                        getText(
                                binding.startPageNumberEditText
                        ),
                        getText(
                                binding.endPageNumberEditText
                        ),
                        getText(
                                binding.chapterDescriptionEditText
                        ),
                        getText(
                                binding.learningObjectivesEditText
                        ),
                        getText(
                                binding.importantTopicsEditText
                        )
                );

        if (!result.isValid()) {
            showValidationError(
                    result
            );

            return;
        }

        SchoolBookChapterFormValidator
                .ValidatedChapterForm form =
                result.getValidatedForm();

        if (form == null) {
            showFormError(
                    "The chapter form could not be validated."
            );

            return;
        }

        if (isEditMode()) {
            updateExistingChapter(
                    form
            );

        } else {
            createNewChapter(
                    form
            );
        }
    }

    private void createNewChapter(
            @NonNull SchoolBookChapterFormValidator
                    .ValidatedChapterForm form
    ) {
        setSaving(
                true
        );

        chapterRepository.getNextSortOrder(
                bookRowId,
                new SchoolBookChapterRepository
                        .SortOrderCallback() {

                    @Override
                    public void onSuccess(
                            int nextSortOrder
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        try {
                            SchoolBookChapterEntity chapter =
                                    SchoolBookChapterMapper
                                            .fromManualForm(
                                                    form,
                                                    nextSortOrder,
                                                    binding
                                                            .optionalChapterCheckBox
                                                            .isChecked(),
                                                    binding
                                                            .revisionChapterCheckBox
                                                            .isChecked()
                                            );

                            insertNewChapter(
                                    chapter
                            );

                        } catch (Exception exception) {
                            setSaving(
                                    false
                            );

                            showFormError(
                                    getErrorMessage(
                                            exception,
                                            "The chapter could not be prepared."
                                    )
                            );
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        setSaving(
                                false
                        );

                        showFormError(
                                getErrorMessage(
                                        exception,
                                        "The chapter order could not be prepared."
                                )
                        );
                    }
                }
        );
    }

    private void insertNewChapter(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        chapterRepository.insertChapter(
                chapter,
                new SchoolBookChapterRepository
                        .InsertChapterCallback() {

                    @Override
                    public void onSuccess(
                            long insertedChapterRowId
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        setSaving(
                                false
                        );

                        Toast.makeText(
                                ManualSchoolBookChapterActivity.this,
                                "Chapter saved.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finishWithSuccess(
                                insertedChapterRowId
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        setSaving(
                                false
                        );

                        showFormError(
                                getErrorMessage(
                                        exception,
                                        "The chapter could not be saved."
                                )
                        );
                    }
                }
        );
    }

    private void updateExistingChapter(
            @NonNull SchoolBookChapterFormValidator
                    .ValidatedChapterForm form
    ) {
        SchoolBookChapterEntity existingChapter =
                chapterBeingEdited;

        if (existingChapter == null) {
            showFormError(
                    "The chapter is still loading."
            );

            return;
        }

        setSaving(
                true
        );

        try {
            SchoolBookChapterEntity updatedChapter =
                    SchoolBookChapterMapper
                            .applyManualEdit(
                                    existingChapter,
                                    form,
                                    existingChapter.getSortOrder(),
                                    binding
                                            .optionalChapterCheckBox
                                            .isChecked(),
                                    binding
                                            .revisionChapterCheckBox
                                            .isChecked()
                            );

            chapterRepository.updateChapter(
                    updatedChapter,
                    new SchoolBookChapterRepository
                            .OperationCallback() {

                        @Override
                        public void onSuccess() {
                            if (isFinishing()
                                    || isDestroyed()) {

                                return;
                            }

                            setSaving(
                                    false
                            );

                            Toast.makeText(
                                    ManualSchoolBookChapterActivity.this,
                                    "Chapter updated.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finishWithSuccess(
                                    updatedChapter
                                            .getChapterRowId()
                            );
                        }

                        @Override
                        public void onError(
                                @NonNull Exception exception
                        ) {
                            if (isFinishing()
                                    || isDestroyed()) {

                                return;
                            }

                            setSaving(
                                    false
                            );

                            showFormError(
                                    getErrorMessage(
                                            exception,
                                            "The chapter could not be updated."
                                    )
                            );
                        }
                    }
            );

        } catch (Exception exception) {
            setSaving(
                    false
            );

            showFormError(
                    getErrorMessage(
                            exception,
                            "The chapter could not be prepared."
                    )
            );
        }
    }

    private void showValidationError(
            @NonNull SchoolBookChapterFormValidator
                    .ValidationResult result
    ) {
        String errorMessage =
                result.getErrorMessage();

        switch (result.getField()) {
            case CHAPTER_NUMBER:
                setFieldError(
                        binding.chapterNumberEditText,
                        errorMessage
                );
                break;

            case CHAPTER_TITLE:
                setFieldError(
                        binding.chapterTitleEnglishEditText,
                        errorMessage
                );

                binding.chapterTitleHindiEditText
                        .setError(
                                errorMessage
                        );
                break;

            case CHAPTER_TITLE_ENGLISH:
                setFieldError(
                        binding.chapterTitleEnglishEditText,
                        errorMessage
                );
                break;

            case CHAPTER_TITLE_HINDI:
                setFieldError(
                        binding.chapterTitleHindiEditText,
                        errorMessage
                );
                break;

            case CHAPTER_SUBTITLE:
                setFieldError(
                        binding.chapterSubtitleEditText,
                        errorMessage
                );
                break;

            case UNIT_NAME:
                setFieldError(
                        binding.unitNameEditText,
                        errorMessage
                );
                break;

            case START_PAGE:
                setFieldError(
                        binding.startPageNumberEditText,
                        errorMessage
                );
                break;

            case END_PAGE:
                setFieldError(
                        binding.endPageNumberEditText,
                        errorMessage
                );
                break;

            case CHAPTER_DESCRIPTION:
                setFieldError(
                        binding.chapterDescriptionEditText,
                        errorMessage
                );
                break;

            case LEARNING_OBJECTIVES:
                setFieldError(
                        binding.learningObjectivesEditText,
                        errorMessage
                );
                break;

            case IMPORTANT_TOPICS:
                setFieldError(
                        binding.importantTopicsEditText,
                        errorMessage
                );
                break;

            case CHAPTER_TYPE:
            case BOOK:
            case NONE:
            default:
                showFormError(
                        errorMessage
                );
                break;
        }
    }

    private void setFieldError(
            @NonNull EditText editText,
            @NonNull String errorMessage
    ) {
        editText.setError(
                errorMessage
        );

        editText.requestFocus();

        binding.chapterFormScrollView
                .smoothScrollTo(
                        0,
                        Math.max(
                                0,
                                editText.getTop() - 40
                        )
                );
    }

    private void clearValidationErrors() {
        binding.chapterNumberEditText
                .setError(
                        null
                );

        binding.chapterTitleEnglishEditText
                .setError(
                        null
                );

        binding.chapterTitleHindiEditText
                .setError(
                        null
                );

        binding.chapterSubtitleEditText
                .setError(
                        null
                );

        binding.unitNameEditText
                .setError(
                        null
                );

        binding.startPageNumberEditText
                .setError(
                        null
                );

        binding.endPageNumberEditText
                .setError(
                        null
                );

        binding.chapterDescriptionEditText
                .setError(
                        null
                );

        binding.learningObjectivesEditText
                .setError(
                        null
                );

        binding.importantTopicsEditText
                .setError(
                        null
                );

        hideFormError();
    }

    private void setSaving(
            boolean saving
    ) {
        this.saving =
                saving;

        showLoading(
                saving
        );
    }

    private void showLoading(
            boolean loading
    ) {
        binding.chapterSaveProgressBar
                .setVisibility(
                        loading
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.saveChapterButton
                .setEnabled(
                        !loading
                );

        binding.cancelChapterButton
                .setEnabled(
                        !loading
                );

        binding.chapterTypeSpinner
                .setEnabled(
                        !loading
                );
    }

    private void showFatalError(
            @NonNull String errorMessage
    ) {
        showLoading(
                false
        );

        showFormError(
                errorMessage
        );

        binding.saveChapterButton
                .setEnabled(
                        false
                );
    }

    private void showFormError(
            @NonNull String errorMessage
    ) {
        binding.chapterFormErrorTextView
                .setText(
                        errorMessage
                );

        binding.chapterFormErrorTextView
                .setVisibility(
                        View.VISIBLE
                );

        binding.chapterFormScrollView
                .smoothScrollTo(
                        0,
                        binding.chapterFormErrorTextView
                                .getTop()
                );
    }

    private void hideFormError() {
        binding.chapterFormErrorTextView
                .setText(
                        ""
                );

        binding.chapterFormErrorTextView
                .setVisibility(
                        View.GONE
                );
    }

    private void setSelectedChapterType(
            @Nullable String chapterType
    ) {
        String safeType =
                chapterType == null
                        ? SchoolBookChapterEntity
                          .CHAPTER_TYPE_CHAPTER
                        : chapterType.trim();

        int selectedPosition =
                chapterTypes.indexOf(
                        safeType
                );

        if (selectedPosition < 0) {
            selectedPosition =
                    0;
        }

        binding.chapterTypeSpinner
                .setSelection(
                        selectedPosition
                );
    }

    @NonNull
    private String getSelectedChapterType() {
        Object selectedItem =
                binding.chapterTypeSpinner
                        .getSelectedItem();

        if (selectedItem == null) {
            return SchoolBookChapterEntity
                    .CHAPTER_TYPE_CHAPTER;
        }

        return selectedItem
                .toString()
                .trim();
    }

    @NonNull
    private String getText(
            @NonNull EditText editText
    ) {
        return editText
                .getText()
                .toString()
                .trim();
    }

    @NonNull
    private String pageNumberToText(
            int pageNumber
    ) {
        return pageNumber <= 0
                ? ""
                : String.valueOf(
                pageNumber
        );
    }

    private boolean hasValidBook() {
        return bookRowId
                > INVALID_ROW_ID;
    }

    private boolean isEditMode() {
        return chapterRowId
                > INVALID_ROW_ID;
    }

    private void cancelAndClose() {
        if (saving) {
            return;
        }

        setResult(
                RESULT_CANCELED
        );

        finish();
    }

    private void finishWithSuccess(
            long savedChapterRowId
    ) {
        Intent resultIntent =
                new Intent();

        resultIntent.putExtra(
                EXTRA_SAVED_CHAPTER_ROW_ID,
                savedChapterRowId
        );

        resultIntent.putExtra(
                EXTRA_CHAPTER_CHANGED,
                true
        );

        setResult(
                RESULT_OK,
                resultIntent
        );

        finish();
    }

    @NonNull
    private String getErrorMessage(
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
}