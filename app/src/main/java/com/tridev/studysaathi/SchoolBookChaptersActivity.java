package com.tridev.studysaathi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterRepository;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterPageRepository;
import com.tridev.studysaathi.databinding
        .ActivitySchoolBookChaptersBinding;
import com.tridev.studysaathi.ui.adapter
        .SchoolBookChapterAdapter;

import java.util.Collections;
import java.util.List;

public final class SchoolBookChaptersActivity
        extends AppCompatActivity
        implements SchoolBookChapterAdapter
        .ChapterActionListener {

    public static final String EXTRA_BOOK_ROW_ID =
            "extra_book_row_id";

    public static final String EXTRA_BOOK_TITLE =
            "extra_book_title";

    public static final String EXTRA_FOCUSED_CONTENT_REVIEW =
            "extra_focused_content_review";

    private static final int REQUEST_MANUAL_CHAPTER =
            6801;

    private static final long INVALID_ROW_ID =
            0L;

    private ActivitySchoolBookChaptersBinding binding;

    private SchoolBookChapterRepository chapterRepository;

    private SchoolBookChapterPageRepository chapterPageRepository;

    private SchoolBookChapterAdapter chapterAdapter;

    private long bookRowId =
            INVALID_ROW_ID;

    @NonNull
    private String bookTitle =
            "";

    private boolean loading;

    private boolean changingChapterState;

    private boolean focusedContentReview;

    @NonNull
    private final ActivityResultLauncher<Intent> contentsScanLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    activityResult -> {
                        if (activityResult.getResultCode()
                                != Activity.RESULT_OK) {

                            return;
                        }

                        Intent data =
                                activityResult.getData();

                        boolean chaptersChanged =
                                data == null
                                        || data.getBooleanExtra(
                                        BookContentsScanActivity
                                                .EXTRA_CHAPTERS_CHANGED,
                                        true
                                );

                        int importedCount =
                                data == null
                                        ? 0
                                        : data.getIntExtra(
                                        BookContentsScanActivity
                                        .EXTRA_IMPORTED_COUNT,
                                        0
                                );

                        int skippedCount =
                                data == null
                                        ? 0
                                        : data.getIntExtra(
                                        BookContentsScanActivity
                                        .EXTRA_SKIPPED_COUNT,
                                        0
                                );

                        if (chaptersChanged) {
                            showImportResultMessage(
                                    importedCount,
                                    skippedCount
                            );

                            loadChapters();
                        }
                    }
            );

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            long bookRowId,
            @Nullable String bookTitle
    ) {
        Intent intent =
                new Intent(
                        context,
                        SchoolBookChaptersActivity.class
                );

        intent.putExtra(
                EXTRA_BOOK_ROW_ID,
                bookRowId
        );

        intent.putExtra(
                EXTRA_BOOK_TITLE,
                safeText(
                        bookTitle
                )
        );

        return intent;
    }

    @NonNull
    public static Intent createContentReviewIntent(
            @NonNull Context context,
            long bookRowId,
            @Nullable String bookTitle
    ) {
        Intent intent = createIntent(context, bookRowId, bookTitle);
        intent.putExtra(EXTRA_FOCUSED_CONTENT_REVIEW, true);
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
                ActivitySchoolBookChaptersBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        readIntentValues();

        chapterRepository =
                new SchoolBookChapterRepository(
                        getApplicationContext()
                );

        chapterPageRepository =
                new SchoolBookChapterPageRepository(
                        getApplicationContext()
                );

        configureRecyclerView();
        configureButtons();
        bindBookInformation();

        if (!hasValidBook()) {
            showFatalError(
                    "A valid school book was not provided."
            );

            return;
        }

        loadChapters();
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

        bookTitle =
                safeText(
                        intent.getStringExtra(
                                EXTRA_BOOK_TITLE
                        )
                );

        focusedContentReview =
                intent.getBooleanExtra(
                        EXTRA_FOCUSED_CONTENT_REVIEW,
                        false
                );
    }

    private void configureRecyclerView() {
        chapterAdapter =
                new SchoolBookChapterAdapter(
                        this
                );

        binding.chaptersRecyclerView
                .setLayoutManager(
                        new LinearLayoutManager(
                                this
                        )
                );

        binding.chaptersRecyclerView
                .setHasFixedSize(
                        false
                );

        binding.chaptersRecyclerView
                .setAdapter(
                        chapterAdapter
                );
    }

    private void configureButtons() {
        binding.backButton
                .setOnClickListener(view ->
                        finish()
                );

        binding.addChapterButton
                .setOnClickListener(view ->
                        openAddChapterScreen()
                );

        binding.emptyAddChapterButton
                .setOnClickListener(view ->
                        openAddChapterScreen()
                );

        binding.confirmAllChaptersButton
                .setOnClickListener(view ->
                        confirmAllPendingChapters()
                );

        binding.scanContentsPageButton
                .setOnClickListener(view ->
                        openContentsScanScreen()
                );

        binding.importFullBookContentButton
                .setOnClickListener(view ->
                        openFullBookLearningImportScreen()
                );
    }

    private void bindBookInformation() {
        binding.screenTitleTextView
                .setText(
                        focusedContentReview
                                ? "Step 3 • Review Saved Chapters"
                                : "School Book Chapters"
                );

        binding.bookTitleTextView
                .setText(
                        bookTitle.isEmpty()
                                ? "Exact School Book"
                                : bookTitle
                );

        int setupVisibility =
                focusedContentReview
                        ? View.GONE
                        : View.VISIBLE;

        binding.chapterSetupQuickActionsContainer
                .setVisibility(setupVisibility);

        binding.importFullBookContentButton
                .setVisibility(setupVisibility);

        binding.importFullBookContentHintTextView
                .setVisibility(setupVisibility);
    }

    private void loadChapters() {
        if (loading
                || !hasValidBook()) {

            return;
        }

        loading =
                true;

        hideError();
        showLoadingState();

        chapterRepository.getChaptersForBook(
                bookRowId,
                new SchoolBookChapterRepository
                        .ChaptersCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolBookChapterEntity> chapters
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        loading =
                                false;

                        List<SchoolBookChapterEntity> safeChapters =
                                chapters == null
                                        ? Collections.emptyList()
                                        : chapters;

                        chapterAdapter.submitChapters(
                                safeChapters
                        );

                        updateSummary(
                                safeChapters
                        );

                        showChapterContentState(
                                safeChapters
                        );

                        setActionsEnabled(
                                true
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        loading =
                                false;

                        chapterAdapter.clearChapters();

                        updateSummary(
                                Collections.emptyList()
                        );

                        showEmptyState();

                        showError(
                                getErrorMessage(
                                        exception,
                                        "The chapter list could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    private void updateSummary(
            @NonNull List<SchoolBookChapterEntity> chapters
    ) {
        int confirmedCount =
                0;

        int pendingReviewCount =
                0;

        for (SchoolBookChapterEntity chapter : chapters) {
            if (chapter.isParentConfirmed()) {
                confirmedCount++;
            }

            if (isPendingParentReview(
                    chapter
            )) {
                pendingReviewCount++;
            }
        }

        String summary =
                chapters.size()
                        + (chapters.size() == 1
                        ? " chapter"
                        : " chapters")
                        + " â€¢ "
                        + confirmedCount
                        + " confirmed";

        binding.chapterSummaryTextView
                .setText(
                        summary
                );

        updatePendingReviewState(
                pendingReviewCount
        );
    }

    private void updatePendingReviewState(
            int pendingReviewCount
    ) {
        if (pendingReviewCount <= 0) {
            binding.pendingReviewContainer
                    .setVisibility(
                            View.GONE
                    );

            return;
        }

        binding.pendingReviewContainer
                .setVisibility(
                        View.VISIBLE
                );

        String message =
                pendingReviewCount
                        + (pendingReviewCount == 1
                        ? " scanned chapter requires"
                        : " scanned chapters require")
                        + " Parent review before "
                        + (pendingReviewCount == 1
                        ? "it appears"
                        : "they appear")
                        + " in Child Mode.";

        binding.pendingReviewMessageTextView
                .setText(
                        message
                );

        binding.confirmAllChaptersButton
                .setText(
                        pendingReviewCount == 1
                                ? "Confirm Pending Chapter"
                                : "Confirm All Pending Chapters"
                );
    }

    private boolean isPendingParentReview(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        if (chapter.isParentConfirmed()) {
            return false;
        }

        String contentSource =
                chapter.getContentSource();

        return SchoolBookChapterEntity
                .CONTENT_SOURCE_BOOK_TOC_SCAN
                .equals(
                        contentSource
                )
                || SchoolBookChapterEntity
                .CONTENT_SOURCE_AI_EXTRACTED
                .equals(
                        contentSource
                );
    }

    private void showChapterContentState(
            @NonNull List<SchoolBookChapterEntity> chapters
    ) {
        binding.chapterLoadingContainer
                .setVisibility(
                        View.GONE
                );

        if (chapters.isEmpty()) {
            binding.chaptersRecyclerView
                    .setVisibility(
                            View.GONE
                    );

            binding.emptyChaptersContainer
                    .setVisibility(
                            View.VISIBLE
                    );

        } else {
            binding.emptyChaptersContainer
                    .setVisibility(
                            View.GONE
                    );

            binding.chaptersRecyclerView
                    .setVisibility(
                            View.VISIBLE
                    );
        }
    }

    private void showLoadingState() {
        binding.chapterLoadingContainer
                .setVisibility(
                        View.VISIBLE
                );

        binding.chaptersRecyclerView
                .setVisibility(
                        View.GONE
                );

        binding.emptyChaptersContainer
                .setVisibility(
                        View.GONE
                );

        setActionsEnabled(
                false
        );
    }

    private void showEmptyState() {
        binding.chapterLoadingContainer
                .setVisibility(
                        View.GONE
                );

        binding.chaptersRecyclerView
                .setVisibility(
                        View.GONE
                );

        binding.emptyChaptersContainer
                .setVisibility(
                        View.VISIBLE
                );

        setActionsEnabled(
                true
        );
    }

    private void setActionsEnabled(
            boolean enabled
    ) {
        boolean actionsAvailable =
                enabled
                        && hasValidBook()
                        && !changingChapterState;

        binding.addChapterButton
                .setEnabled(
                        actionsAvailable
                );

        binding.emptyAddChapterButton
                .setEnabled(
                        actionsAvailable
                );

        binding.scanContentsPageButton
                .setEnabled(
                        actionsAvailable
                );

        binding.importFullBookContentButton
                .setEnabled(
                        actionsAvailable
                );

        binding.confirmAllChaptersButton
                .setEnabled(
                        actionsAvailable
                );
    }

    private void openAddChapterScreen() {
        if (!hasValidBook()
                || changingChapterState) {

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        ManualSchoolBookChapterActivity.class
                );

        intent.putExtra(
                ManualSchoolBookChapterActivity
                        .EXTRA_BOOK_ROW_ID,
                bookRowId
        );

        startActivityForResult(
                intent,
                REQUEST_MANUAL_CHAPTER
        );
    }

    private void openEditChapterScreen(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        if (chapter.getChapterRowId()
                <= 0L
                || changingChapterState) {

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        ManualSchoolBookChapterActivity.class
                );

        intent.putExtra(
                ManualSchoolBookChapterActivity
                        .EXTRA_BOOK_ROW_ID,
                bookRowId
        );

        intent.putExtra(
                ManualSchoolBookChapterActivity
                        .EXTRA_CHAPTER_ROW_ID,
                chapter.getChapterRowId()
        );

        startActivityForResult(
                intent,
                REQUEST_MANUAL_CHAPTER
        );
    }

    private void openContentsScanScreen() {
        if (!hasValidBook()
                || changingChapterState) {

            return;
        }

        Intent intent =
                BookContentsScanActivity.createIntent(
                        this,
                        bookRowId,
                        bookTitle
                );

        try {
            contentsScanLauncher.launch(
                    intent
            );

        } catch (RuntimeException exception) {
            showError(
                    getErrorMessage(
                            exception,
                            "The contents scanner could not be opened."
                    )
            );
        }
    }

    private void openFullBookLearningImportScreen() {
        if (!hasValidBook()
                || changingChapterState) {

            return;
        }

        Intent intent =
                BookLearningImportActivity.createIntent(
                        this,
                        bookRowId,
                        bookTitle
                );

        try {
            startActivity(intent);
        } catch (RuntimeException exception) {
            showError(
                    getErrorMessage(
                            exception,
                            "The full-book content importer "
                                    + "could not be opened."
                    )
            );
        }
    }

    @Override
    public void onOpenChapter(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        if (chapter.getChapterRowId() <= 0L
                || changingChapterState) {

            return;
        }

        changingChapterState = true;
        setActionsEnabled(false);

        chapterPageRepository.getPagesForChapter(
                chapter.getChapterRowId(),
                new SchoolBookChapterPageRepository
                        .PagesCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<com.tridev.studysaathi
                                    .data.local.entity
                                    .SchoolBookChapterPageEntity> pages
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        changingChapterState = false;
                        setActionsEnabled(true);

                        if (pages.isEmpty()) {
                            openManualChapterContentEditor(
                                    chapter
                            );
                            return;
                        }

                        Intent intent =
                                SchoolBookChapterPageReviewActivity
                                        .createIntent(
                                                SchoolBookChaptersActivity
                                                        .this,
                                                chapter.getChapterRowId(),
                                                getChapterReviewTitle(
                                                        chapter
                                                )
                                        );

                        try {
                            startActivity(intent);
                        } catch (RuntimeException exception) {
                            showError(
                                    getErrorMessage(
                                            exception,
                                            "The scanned page review "
                                                    + "screen could not be opened."
                                    )
                            );
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        changingChapterState = false;
                        setActionsEnabled(true);

                        showError(
                                getErrorMessage(
                                        exception,
                                        "Chapter pages could not be checked."
                                )
                        );
                    }
                }
        );
    }

    private void openManualChapterContentEditor(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        Intent intent =
                new Intent(
                        this,
                        SchoolBookChapterContentEditorActivity.class
                );

        intent.putExtra(
                SchoolBookChapterContentEditorActivity
                        .EXTRA_CHAPTER_ROW_ID,
                chapter.getChapterRowId()
        );

        startActivity(
                intent
        );
    }

    @NonNull
    private static String getChapterReviewTitle(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        String englishTitle =
                safeText(
                        chapter.getChapterTitleEnglish()
                );

        String hindiTitle =
                safeText(
                        chapter.getChapterTitleHindi()
                );

        if (englishTitle.isEmpty()) {
            return hindiTitle.isEmpty()
                    ? "Chapter"
                    : hindiTitle;
        }

        if (hindiTitle.isEmpty()
                || englishTitle.equals(hindiTitle)) {
            return englishTitle;
        }

        return englishTitle
                + " / "
                + hindiTitle;
    }

    @Override
    public void onEditChapter(
            @NonNull SchoolBookChapterEntity chapter
    ) {
        openEditChapterScreen(
                chapter
        );
    }

    @Override
    public void onChapterEnabledChanged(
            @NonNull SchoolBookChapterEntity chapter,
            boolean enabled
    ) {
        if (changingChapterState
                || chapter.getChapterRowId() <= 0L) {

            loadChapters();
            return;
        }

        changingChapterState =
                true;

        setActionsEnabled(
                false
        );

        chapterRepository.setChapterEnabled(
                chapter.getChapterRowId(),
                enabled,
                new SchoolBookChapterRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        changingChapterState =
                                false;

                        Toast.makeText(
                                SchoolBookChaptersActivity.this,
                                enabled
                                        ? "Chapter enabled."
                                        : "Chapter disabled.",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadChapters();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        changingChapterState =
                                false;

                        showError(
                                getErrorMessage(
                                        exception,
                                        "The chapter status could not be updated."
                                )
                        );

                        loadChapters();
                    }
                }
        );
    }

    private void confirmAllPendingChapters() {
        if (changingChapterState
                || !hasValidBook()) {

            return;
        }

        changingChapterState =
                true;

        setActionsEnabled(
                false
        );

        chapterRepository.confirmAllChaptersForBook(
                bookRowId,
                new SchoolBookChapterRepository
                        .CountOperationCallback() {

                    @Override
                    public void onSuccess(
                            int affectedChapterCount
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        changingChapterState =
                                false;

                        String message =
                                affectedChapterCount <= 0
                                        ? "No pending chapters were found."
                                        : affectedChapterCount
                                          + (affectedChapterCount == 1
                                             ? " chapter confirmed."
                                             : " chapters confirmed.");

                        Toast.makeText(
                                SchoolBookChaptersActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        loadChapters();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        changingChapterState =
                                false;

                        setActionsEnabled(
                                true
                        );

                        showError(
                                getErrorMessage(
                                        exception,
                                        "The pending chapters "
                                                + "could not be confirmed."
                                )
                        );
                    }
                }
        );
    }

    private void showImportResultMessage(
            int importedCount,
            int skippedCount
    ) {
        if (importedCount <= 0
                && skippedCount <= 0) {

            return;
        }

        String message =
                importedCount
                        + (importedCount == 1
                        ? " chapter imported"
                        : " chapters imported");

        if (skippedCount > 0) {
            message =
                    message
                            + "; "
                            + skippedCount
                            + (skippedCount == 1
                            ? " duplicate skipped."
                            : " duplicates skipped.");

        } else {
            message =
                    message
                            + ".";
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void showFatalError(
            @NonNull String errorMessage
    ) {
        loading =
                false;

        chapterAdapter.clearChapters();

        binding.chapterLoadingContainer
                .setVisibility(
                        View.GONE
                );

        binding.chaptersRecyclerView
                .setVisibility(
                        View.GONE
                );

        binding.emptyChaptersContainer
                .setVisibility(
                        View.VISIBLE
                );

        binding.emptyChaptersMessageTextView
                .setText(
                        errorMessage
                );

        showError(
                errorMessage
        );

        setActionsEnabled(
                false
        );
    }

    private void showError(
            @NonNull String errorMessage
    ) {
        binding.chapterScreenErrorTextView
                .setText(
                        errorMessage
                );

        binding.chapterScreenErrorTextView
                .setVisibility(
                        View.VISIBLE
                );
    }

    private void hideError() {
        binding.chapterScreenErrorTextView
                .setText(
                        ""
                );

        binding.chapterScreenErrorTextView
                .setVisibility(
                        View.GONE
                );
    }

    private boolean hasValidBook() {
        return bookRowId
                > INVALID_ROW_ID;
    }

    private boolean canUpdateScreen() {
        return !isFinishing()
                && !isDestroyed();
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

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != REQUEST_MANUAL_CHAPTER
                || resultCode != Activity.RESULT_OK) {

            return;
        }

        boolean chapterChanged =
                data == null
                        || data.getBooleanExtra(
                        ManualSchoolBookChapterActivity
                                .EXTRA_CHAPTER_CHANGED,
                        true
                );

        if (chapterChanged) {
            loadChapters();
        }
    }
}
