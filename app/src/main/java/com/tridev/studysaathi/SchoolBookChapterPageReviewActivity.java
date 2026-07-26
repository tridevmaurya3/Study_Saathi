package com.tridev.studysaathi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterPageEntity;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterPageRepository;
import com.tridev.studysaathi.databinding
        .ActivitySchoolBookChapterPageReviewBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent को OCR से बनी हर chapter page अलग-अलग दिखाकर approval लेने वाली screen.
 */
public final class SchoolBookChapterPageReviewActivity
        extends AppCompatActivity {

    public static final String EXTRA_CHAPTER_ROW_ID =
            "extra_page_review_chapter_row_id";

    public static final String EXTRA_CHAPTER_TITLE =
            "extra_page_review_chapter_title";

    private static final long INVALID_ROW_ID = 0L;
    private static final int MAX_IMAGE_SIDE = 1600;

    private ActivitySchoolBookChapterPageReviewBinding binding;

    private SchoolBookChapterPageRepository pageRepository;

    @NonNull
    private final ArrayList<SchoolBookChapterPageEntity> pages =
            new ArrayList<>();

    private long chapterRowId = INVALID_ROW_ID;

    @NonNull
    private String chapterTitle = "Chapter";

    private int currentPageIndex = 0;
    private boolean savingApproval = false;

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            long chapterRowId,
            @Nullable String chapterTitle
    ) {
        Intent intent =
                new Intent(
                        context,
                        SchoolBookChapterPageReviewActivity.class
                );

        intent.putExtra(
                EXTRA_CHAPTER_ROW_ID,
                chapterRowId
        );

        intent.putExtra(
                EXTRA_CHAPTER_TITLE,
                safeText(chapterTitle)
        );

        return intent;
    }

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        binding =
                ActivitySchoolBookChapterPageReviewBinding
                        .inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        pageRepository =
                new SchoolBookChapterPageRepository(this);

        readArguments();
        setupToolbar();
        setupButtons();

        if (chapterRowId <= INVALID_ROW_ID) {
            showMessage(
                    "A valid exact chapter is required."
            );
            finish();
            return;
        }

        loadPages();
    }

    private void readArguments() {
        chapterRowId =
                getIntent().getLongExtra(
                        EXTRA_CHAPTER_ROW_ID,
                        INVALID_ROW_ID
                );

        String suppliedTitle =
                safeText(
                        getIntent().getStringExtra(
                                EXTRA_CHAPTER_TITLE
                        )
                );

        if (!suppliedTitle.isEmpty()) {
            chapterTitle = suppliedTitle;
        }
    }

    private void setupToolbar() {
        binding.chapterPageReviewToolbar
                .setNavigationOnClickListener(
                        ignored -> finish()
                );
    }

    private void setupButtons() {
        binding.chapterPageReviewPreviousButton
                .setOnClickListener(
                        ignored -> moveToPage(
                                currentPageIndex - 1
                        )
                );

        binding.chapterPageReviewNextButton
                .setOnClickListener(
                        ignored -> moveToPage(
                                currentPageIndex + 1
                        )
                );

        binding.chapterPageReviewSaveButton
                .setOnClickListener(
                        ignored -> saveCurrentApproval()
                );
    }

    private void loadPages() {
        showLoading(true);

        pageRepository.getPagesForChapter(
                chapterRowId,
                new SchoolBookChapterPageRepository
                        .PagesCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolBookChapterPageEntity>
                                    loadedPages
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        showLoading(false);
                        pages.clear();
                        pages.addAll(loadedPages);

                        if (pages.isEmpty()) {
                            showEmptyState();
                            return;
                        }

                        currentPageIndex = 0;
                        showCurrentPage();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        showLoading(false);
                        showMessage(
                                readableMessage(
                                        exception,
                                        "Scanned pages could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    private void moveToPage(
            int requestedIndex
    ) {
        if (savingApproval
                || requestedIndex < 0
                || requestedIndex >= pages.size()) {
            return;
        }

        currentPageIndex = requestedIndex;
        showCurrentPage();

        binding.chapterPageReviewScrollView
                .smoothScrollTo(0, 0);
    }

    private void showCurrentPage() {
        SchoolBookChapterPageEntity page =
                getCurrentPage();

        if (page == null) {
            showEmptyState();
            return;
        }

        binding.chapterPageReviewChapterTitleTextView
                .setText(chapterTitle);

        binding.chapterPageReviewCounterTextView
                .setText(
                        "Page "
                                + (currentPageIndex + 1)
                                + " of "
                                + pages.size()
                                + " • Source page "
                                + page.getSourceDocumentPageNumber()
                );

        binding.chapterPageReviewSourceLabelTextView
                .setText(
                        "Original scanned page "
                                + page.getSourceDocumentPageNumber()
                );

        binding.chapterPageReviewTypeTextView
                .setText(
                        "Type: "
                                + readablePageType(
                                page.getPageType()
                        )
                );

        binding.chapterPageReviewTitleTextView
                .setText(
                        firstNonEmpty(
                                page.getPageTitle(),
                                "Learning page "
                                        + page.getPageOrder()
                        )
                );

        binding.chapterPageReviewExplanationTextView
                .setText(createPageMatter(page));

        showOptionalSection(
                binding.chapterPageReviewExamplesHeadingTextView,
                binding.chapterPageReviewExamplesTextView,
                combineLanguages(
                        page.getExamplesEnglish(),
                        page.getExamplesHindi()
                )
        );

        showOptionalSection(
                binding.chapterPageReviewExercisesHeadingTextView,
                binding.chapterPageReviewExercisesTextView,
                formatExercises(
                        page.getExercisesJson()
                )
        );

        binding.chapterPageReviewApprovedCheckBox
                .setChecked(page.isParentApproved());

        showPageImage(
                page.getPersistentPageImagePath()
        );

        updateButtons();
    }

    private void saveCurrentApproval() {
        SchoolBookChapterPageEntity page =
                getCurrentPage();

        if (page == null || savingApproval) {
            return;
        }

        boolean approved =
                binding.chapterPageReviewApprovedCheckBox
                        .isChecked();

        savingApproval = true;
        updateButtons();

        pageRepository.setPageParentApproved(
                page.getChapterPageRowId(),
                approved,
                new SchoolBookChapterPageRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        savingApproval = false;
                        page.setParentApproved(approved);
                        updateButtons();

                        showMessage(
                                approved
                                        ? "Page approved for Child Mode."
                                        : "Page removed from Child Mode."
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        savingApproval = false;

                        binding.chapterPageReviewApprovedCheckBox
                                .setChecked(
                                        page.isParentApproved()
                                );

                        updateButtons();

                        showMessage(
                                readableMessage(
                                        exception,
                                        "Page approval could not be saved."
                                )
                        );
                    }
                }
        );
    }

    private void updateButtons() {
        boolean hasPages = !pages.isEmpty();

        binding.chapterPageReviewPreviousButton
                .setEnabled(
                        hasPages
                                && !savingApproval
                                && currentPageIndex > 0
                );

        binding.chapterPageReviewNextButton
                .setEnabled(
                        hasPages
                                && !savingApproval
                                && currentPageIndex
                                < pages.size() - 1
                );

        binding.chapterPageReviewSaveButton
                .setEnabled(
                        hasPages && !savingApproval
                );

        binding.chapterPageReviewApprovedCheckBox
                .setEnabled(
                        hasPages && !savingApproval
                );

        binding.chapterPageReviewSaveButton
                .setText(
                        savingApproval
                                ? "Saving…"
                                : "Save Approval"
                );
    }

    private void showEmptyState() {
        pages.clear();

        binding.chapterPageReviewChapterTitleTextView
                .setText(chapterTitle);

        binding.chapterPageReviewCounterTextView
                .setText("No scanned page drafts found");

        binding.chapterPageReviewSourceCard
                .setVisibility(View.GONE);

        binding.chapterPageReviewMatterCard
                .setVisibility(View.GONE);

        binding.chapterPageReviewApprovedCheckBox
                .setVisibility(View.GONE);

        binding.chapterPageReviewSafetyTextView
                .setText(
                        "Import and approve chapter boundaries "
                                + "before reviewing pages."
                );

        updateButtons();
    }

    private void showLoading(
            boolean loading
    ) {
        binding.chapterPageReviewLoadingIndicator
                .setVisibility(
                        loading
                                ? View.VISIBLE
                                : View.GONE
                );
    }

    private void showPageImage(
            @Nullable String imagePath
    ) {
        binding.chapterPageReviewImageView
                .setImageDrawable(null);

        String safePath = safeText(imagePath);

        if (safePath.isEmpty()) {
            binding.chapterPageReviewImageView
                    .setVisibility(View.GONE);
            return;
        }

        File imageFile = new File(safePath);

        if (!imageFile.isFile()) {
            binding.chapterPageReviewImageView
                    .setVisibility(View.GONE);
            return;
        }

        Bitmap bitmap =
                decodeSampledBitmap(
                        imageFile.getAbsolutePath()
                );

        if (bitmap == null) {
            binding.chapterPageReviewImageView
                    .setVisibility(View.GONE);
            return;
        }

        binding.chapterPageReviewImageView
                .setVisibility(View.VISIBLE);

        binding.chapterPageReviewImageView
                .setImageBitmap(bitmap);
    }

    @Nullable
    private static Bitmap decodeSampledBitmap(
            @NonNull String imagePath
    ) {
        BitmapFactory.Options bounds =
                new BitmapFactory.Options();

        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bounds);

        if (bounds.outWidth <= 0
                || bounds.outHeight <= 0) {
            return null;
        }

        int sampleSize = 1;
        int largestSide =
                Math.max(
                        bounds.outWidth,
                        bounds.outHeight
                );

        while (largestSide / sampleSize
                > MAX_IMAGE_SIDE) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options =
                new BitmapFactory.Options();

        options.inSampleSize =
                Math.max(sampleSize, 1);

        return BitmapFactory.decodeFile(
                imagePath,
                options
        );
    }

    private static void showOptionalSection(
            @NonNull View heading,
            @NonNull android.widget.TextView contentView,
            @Nullable String content
    ) {
        String safeContent = safeText(content);
        int visibility =
                safeContent.isEmpty()
                        ? View.GONE
                        : View.VISIBLE;

        heading.setVisibility(visibility);
        contentView.setVisibility(visibility);
        contentView.setText(safeContent);
    }

    @NonNull
    private static String createPageMatter(
            @NonNull SchoolBookChapterPageEntity page
    ) {
        ArrayList<String> sections =
                new ArrayList<>();

        addSection(
                sections,
                page.getIntroductionEnglish(),
                page.getIntroductionHindi()
        );

        addSection(
                sections,
                page.getExplanationEnglish(),
                page.getExplanationHindi()
        );

        addSection(
                sections,
                page.getKeyPointsEnglish(),
                page.getKeyPointsHindi()
        );

        addSection(
                sections,
                page.getSummaryEnglish(),
                page.getSummaryHindi()
        );

        if (sections.isEmpty()) {
            String rawOcr =
                    safeText(page.getRawOcrText());

            return rawOcr.isEmpty()
                    ? "No readable matter was extracted from this page."
                    : rawOcr;
        }

        return android.text.TextUtils.join(
                "\n\n",
                sections
        );
    }

    private static void addSection(
            @NonNull List<String> sections,
            @Nullable String english,
            @Nullable String hindi
    ) {
        String combined =
                combineLanguages(
                        english,
                        hindi
                );

        if (!combined.isEmpty()
                && !sections.contains(combined)) {
            sections.add(combined);
        }
    }

    @NonNull
    private static String combineLanguages(
            @Nullable String english,
            @Nullable String hindi
    ) {
        String safeEnglish = safeText(english);
        String safeHindi = safeText(hindi);

        if (safeEnglish.isEmpty()) {
            return safeHindi;
        }

        if (safeHindi.isEmpty()
                || safeHindi.equals(safeEnglish)) {
            return safeEnglish;
        }

        return safeEnglish
                + "\n\n"
                + safeHindi;
    }

    @NonNull
    private static String formatExercises(
            @Nullable String exercisesJson
    ) {
        String value = safeText(exercisesJson);

        if (value.isEmpty()
                || "[]".equals(value)
                || "{}".equals(value)) {
            return "";
        }

        try {
            if (value.startsWith("[")) {
                return new JSONArray(value)
                        .toString(2);
            }

            if (value.startsWith("{")) {
                return new JSONObject(value)
                        .toString(2);
            }
        } catch (Exception ignored) {
            // Invalid OCR JSON is still shown as readable source text.
        }

        return value;
    }

    @NonNull
    private static String readablePageType(
            @Nullable String pageType
    ) {
        String value =
                safeText(pageType)
                        .replace('_', ' ');

        if (value.isEmpty()) {
            return "Learning matter";
        }

        return Character.toUpperCase(
                value.charAt(0)
        ) + value.substring(1);
    }

    @Nullable
    private SchoolBookChapterPageEntity getCurrentPage() {
        if (currentPageIndex < 0
                || currentPageIndex >= pages.size()) {
            return null;
        }

        return pages.get(currentPageIndex);
    }

    private boolean canUpdateScreen() {
        return !isFinishing()
                && !isDestroyed()
                && binding != null;
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
    private static String readableMessage(
            @NonNull Exception exception,
            @NonNull String fallback
    ) {
        String message =
                safeText(exception.getMessage());

        return message.isEmpty()
                ? fallback
                : message;
    }

    @NonNull
    private static String firstNonEmpty(
            @Nullable String value,
            @NonNull String fallback
    ) {
        String safeValue = safeText(value);

        return safeValue.isEmpty()
                ? fallback
                : safeValue;
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
}
