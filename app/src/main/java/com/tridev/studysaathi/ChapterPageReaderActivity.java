package com.tridev.studysaathi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterPageEntity;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterPageRepository;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterProgressRepository;
import com.tridev.studysaathi.databinding
        .ActivityChapterPageReaderBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Child Mode के लिए one-approved-page-at-a-time Kinder reader.
 */
public final class ChapterPageReaderActivity
        extends AppCompatActivity {

    public static final String EXTRA_CHAPTER_ROW_ID =
            "extra_reader_chapter_row_id";

    public static final String EXTRA_CHAPTER_TITLE =
            "extra_reader_chapter_title";

    private static final long INVALID_ROW_ID = 0L;
    private static final int MAX_IMAGE_SIDE = 1600;
    private static final String READER_PREFERENCES =
            "chapter_page_reader_preferences";
    private static final String KEY_READER_TEXT_SIZE =
            "reader_text_size_sp";
    private static final float DEFAULT_TEXT_SIZE_SP = 18f;
    private static final float MIN_TEXT_SIZE_SP = 14f;
    private static final float MAX_TEXT_SIZE_SP = 30f;
    private static final float TEXT_SIZE_STEP_SP = 2f;

    private ActivityChapterPageReaderBinding binding;
    private SchoolBookChapterPageRepository pageRepository;
    private SchoolBookChapterProgressRepository progressRepository;

    @NonNull
    private final ArrayList<SchoolBookChapterPageEntity> pages =
            new ArrayList<>();

    private long chapterRowId = INVALID_ROW_ID;

    @NonNull
    private String chapterTitle = "Chapter";

    private int currentPageIndex = 0;
    private boolean finishingChapter = false;
    private float readerTextSizeSp =
            DEFAULT_TEXT_SIZE_SP;

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            long chapterRowId,
            @Nullable String chapterTitle
    ) {
        Intent intent =
                new Intent(
                        context,
                        ChapterPageReaderActivity.class
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
                ActivityChapterPageReaderBinding
                        .inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        pageRepository =
                new SchoolBookChapterPageRepository(this);

        progressRepository =
                new SchoolBookChapterProgressRepository(this);

        readArguments();
        loadReaderTextSize();
        setupToolbar();
        setupButtons();
        applyReaderTextSize();

        if (chapterRowId <= INVALID_ROW_ID) {
            showMessage(
                    "A valid chapter is required."
            );
            finish();
            return;
        }

        loadApprovedPages();
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
        binding.chapterPageReaderToolbar
                .setNavigationOnClickListener(
                        ignored -> finish()
                );
    }

    private void setupButtons() {
        binding.chapterPageReaderPreviousButton
                .setOnClickListener(
                        ignored -> moveToPage(
                                currentPageIndex - 1
                        )
                );

        binding.chapterPageReaderNextButton
                .setOnClickListener(ignored -> {
                    if (currentPageIndex
                            >= pages.size() - 1) {
                        completeChapter();
                        return;
                    }

                    moveToPage(
                            currentPageIndex + 1
                    );
                });

        binding.chapterPageReaderDecreaseTextButton
                .setOnClickListener(
                        ignored -> changeReaderTextSize(
                                -TEXT_SIZE_STEP_SP
                        )
                );

        binding.chapterPageReaderIncreaseTextButton
                .setOnClickListener(
                        ignored -> changeReaderTextSize(
                                TEXT_SIZE_STEP_SP
                        )
                );
    }

    private void loadReaderTextSize() {
        SharedPreferences preferences =
                getSharedPreferences(
                        READER_PREFERENCES,
                        MODE_PRIVATE
                );

        readerTextSizeSp =
                clampReaderTextSize(
                        preferences.getFloat(
                                KEY_READER_TEXT_SIZE,
                                DEFAULT_TEXT_SIZE_SP
                        )
                );
    }

    private void changeReaderTextSize(
            float changeSp
    ) {
        float newSize =
                clampReaderTextSize(
                        readerTextSizeSp + changeSp
                );

        if (newSize == readerTextSizeSp) {
            return;
        }

        readerTextSizeSp = newSize;

        getSharedPreferences(
                READER_PREFERENCES,
                MODE_PRIVATE
        ).edit()
                .putFloat(
                        KEY_READER_TEXT_SIZE,
                        readerTextSizeSp
                )
                .apply();

        applyReaderTextSize();
    }

    private void applyReaderTextSize() {
        binding.chapterPageReaderMatterTextView
                .setTextSize(readerTextSizeSp);

        binding.chapterPageReaderExamplesTextView
                .setTextSize(readerTextSizeSp);

        binding.chapterPageReaderExercisesTextView
                .setTextSize(readerTextSizeSp);

        binding.chapterPageReaderDecreaseTextButton
                .setEnabled(
                        readerTextSizeSp
                                > MIN_TEXT_SIZE_SP
                );

        binding.chapterPageReaderIncreaseTextButton
                .setEnabled(
                        readerTextSizeSp
                                < MAX_TEXT_SIZE_SP
                );
    }

    private static float clampReaderTextSize(
            float textSizeSp
    ) {
        return Math.max(
                MIN_TEXT_SIZE_SP,
                Math.min(
                        MAX_TEXT_SIZE_SP,
                        textSizeSp
                )
        );
    }

    private void loadApprovedPages() {
        setNavigationEnabled(false);

        binding.chapterPageReaderProgressIndicator
                .setIndeterminate(true);

        pageRepository.getApprovedPagesForChapter(
                chapterRowId,
                new SchoolBookChapterPageRepository
                        .PagesCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolBookChapterPageEntity>
                                    approvedPages
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        binding.chapterPageReaderProgressIndicator
                                .setIndeterminate(false);

                        pages.clear();
                        pages.addAll(approvedPages);

                        if (pages.isEmpty()) {
                            showEmptyState();
                            return;
                        }

                        loadSavedStartPage();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        binding.chapterPageReaderProgressIndicator
                                .setIndeterminate(false);

                        showEmptyState();

                        showMessage(
                                readableMessage(
                                        exception,
                                        "Approved chapter pages "
                                                + "could not be loaded."
                                )
                        );
                    }
                }
        );
    }

    private void loadSavedStartPage() {
        progressRepository.getChapterProgressPercent(
                chapterRowId,
                new SchoolBookChapterProgressRepository
                        .ProgressCallback() {

                    @Override
                    public void onSuccess(
                            int progressPercent
                    ) {
                        if (!canUpdateScreen()
                                || pages.isEmpty()) {
                            return;
                        }

                        currentPageIndex =
                                pageIndexFromProgress(
                                        progressPercent,
                                        pages.size()
                                );

                        showCurrentPage();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()
                                || pages.isEmpty()) {
                            return;
                        }

                        currentPageIndex = 0;
                        showCurrentPage();

                        showMessage(
                                "Saved page could not be restored. "
                                        + "Starting from page 1."
                        );
                    }
                }
        );
    }

    private static int pageIndexFromProgress(
            int progressPercent,
            int totalPages
    ) {
        if (totalPages <= 0
                || progressPercent <= 0) {
            return 0;
        }

        int completedPageCount =
                (int) Math.ceil(
                        (progressPercent
                                * totalPages)
                                / 100.0
                );

        return Math.max(
                0,
                Math.min(
                        totalPages - 1,
                        completedPageCount - 1
                )
        );
    }

    private void moveToPage(
            int requestedIndex
    ) {
        if (requestedIndex < 0
                || requestedIndex >= pages.size()) {
            return;
        }

        currentPageIndex = requestedIndex;
        showCurrentPage();

        binding.chapterPageReaderScrollView
                .smoothScrollTo(0, 0);
    }

    private void showCurrentPage() {
        SchoolBookChapterPageEntity page =
                getCurrentPage();

        if (page == null) {
            showEmptyState();
            return;
        }

        showReaderContent(true);

        binding.chapterPageReaderChapterTitleTextView
                .setText(chapterTitle);

        binding.chapterPageReaderCounterTextView
                .setText(
                        "Page "
                                + (currentPageIndex + 1)
                                + " of "
                                + pages.size()
                );

        binding.chapterPageReaderProgressIndicator
                .setMax(100);

        binding.chapterPageReaderProgressIndicator
                .setProgressCompat(
                        calculateProgressPercent(),
                        true
                );

        binding.chapterPageReaderPageTypeTextView
                .setText(
                        readablePageType(
                                page.getPageType()
                        )
                );

        binding.chapterPageReaderPageTitleTextView
                .setText(
                        firstNonEmpty(
                                page.getPageTitle(),
                                "Learning Page "
                                        + (currentPageIndex + 1)
                        )
                );

        binding.chapterPageReaderMatterTextView
                .setText(
                        createPageMatter(page)
                );

        showOptionalCard(
                binding.chapterPageReaderExamplesCard,
                binding.chapterPageReaderExamplesTextView,
                combineLanguages(
                        page.getExamplesEnglish(),
                        page.getExamplesHindi()
                )
        );

        showOptionalCard(
                binding.chapterPageReaderExercisesCard,
                binding.chapterPageReaderExercisesTextView,
                formatExercises(
                        page.getExercisesJson()
                )
        );

        showPageImage(
                page.getPersistentPageImagePath()
        );

        setNavigationEnabled(true);

        binding.chapterPageReaderNextButton
                .setText(
                        currentPageIndex
                                == pages.size() - 1
                                ? "Finish"
                                : "Next"
                );

        saveCurrentPageProgress();
    }

    private void saveCurrentPageProgress() {
        if (pages.isEmpty()
                || chapterRowId <= INVALID_ROW_ID
                || finishingChapter) {
            return;
        }

        progressRepository.updateChapterProgress(
                chapterRowId,
                calculateProgressPercent(),
                new SchoolBookChapterProgressRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        // Progress is reflected by the reader indicator.
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        showMessage(
                                "Reading progress could not be saved."
                        );
                    }
                }
        );
    }

    private void completeChapter() {
        if (finishingChapter
                || pages.isEmpty()) {
            return;
        }

        finishingChapter = true;
        setNavigationEnabled(false);

        binding.chapterPageReaderNextButton
                .setText("Completing…");

        binding.chapterPageReaderProgressIndicator
                .setProgressCompat(100, true);

        progressRepository.markChapterCompleted(
                chapterRowId,
                new SchoolBookChapterProgressRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        showMessage(
                                "Chapter completed — 100%"
                        );

                        binding.getRoot().postDelayed(
                                ChapterPageReaderActivity
                                        .this::finish,
                                500L
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (!canUpdateScreen()) {
                            return;
                        }

                        finishingChapter = false;
                        setNavigationEnabled(true);

                        binding.chapterPageReaderNextButton
                                .setText("Finish");

                        showMessage(
                                readableMessage(
                                        exception,
                                        "Chapter completion "
                                                + "could not be saved."
                                )
                        );
                    }
                }
        );
    }

    private int calculateProgressPercent() {
        if (pages.isEmpty()) {
            return 0;
        }

        return Math.min(
                100,
                Math.max(
                        0,
                        Math.round(
                                ((currentPageIndex + 1)
                                        * 100f)
                                        / pages.size()
                        )
                )
        );
    }

    private void setNavigationEnabled(
            boolean enabled
    ) {
        binding.chapterPageReaderPreviousButton
                .setEnabled(
                        enabled
                                && currentPageIndex > 0
                );

        binding.chapterPageReaderNextButton
                .setEnabled(
                        enabled && !pages.isEmpty()
                );
    }

    private void showEmptyState() {
        pages.clear();

        binding.chapterPageReaderChapterTitleTextView
                .setText(chapterTitle);

        binding.chapterPageReaderCounterTextView
                .setText("Page 0 of 0");

        binding.chapterPageReaderProgressIndicator
                .setProgressCompat(0, false);

        showReaderContent(false);
        setNavigationEnabled(false);
    }

    private void showReaderContent(
            boolean visible
    ) {
        int contentVisibility =
                visible
                        ? View.VISIBLE
                        : View.GONE;

        binding.chapterPageReaderImageCard
                .setVisibility(contentVisibility);

        binding.chapterPageReaderMatterCard
                .setVisibility(contentVisibility);

        if (!visible) {
            binding.chapterPageReaderExamplesCard
                    .setVisibility(View.GONE);

            binding.chapterPageReaderExercisesCard
                    .setVisibility(View.GONE);
        }

        binding.chapterPageReaderEmptyTextView
                .setVisibility(
                        visible
                                ? View.GONE
                                : View.VISIBLE
                );
    }

    private void showPageImage(
            @Nullable String imagePath
    ) {
        binding.chapterPageReaderImageView
                .setImageDrawable(null);

        String path = safeText(imagePath);

        if (path.isEmpty()
                || !new File(path).isFile()) {
            binding.chapterPageReaderImageCard
                    .setVisibility(View.GONE);
            return;
        }

        Bitmap bitmap =
                decodeSampledBitmap(path);

        if (bitmap == null) {
            binding.chapterPageReaderImageCard
                    .setVisibility(View.GONE);
            return;
        }

        binding.chapterPageReaderImageCard
                .setVisibility(View.VISIBLE);

        binding.chapterPageReaderImageView
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
                Math.max(1, sampleSize);

        return BitmapFactory.decodeFile(
                imagePath,
                options
        );
    }

    private static void showOptionalCard(
            @NonNull View card,
            @NonNull TextView textView,
            @Nullable String content
    ) {
        String value = safeText(content);

        card.setVisibility(
                value.isEmpty()
                        ? View.GONE
                        : View.VISIBLE
        );

        textView.setText(value);
    }

    @NonNull
    private static String createPageMatter(
            @NonNull SchoolBookChapterPageEntity page
    ) {
        ArrayList<String> parts =
                new ArrayList<>();

        addUniquePart(
                parts,
                combineLanguages(
                        page.getIntroductionEnglish(),
                        page.getIntroductionHindi()
                )
        );

        addUniquePart(
                parts,
                combineLanguages(
                        page.getExplanationEnglish(),
                        page.getExplanationHindi()
                )
        );

        addUniquePart(
                parts,
                combineLanguages(
                        page.getKeyPointsEnglish(),
                        page.getKeyPointsHindi()
                )
        );

        addUniquePart(
                parts,
                combineLanguages(
                        page.getSummaryEnglish(),
                        page.getSummaryHindi()
                )
        );

        if (parts.isEmpty()) {
            return firstNonEmpty(
                    page.getRawOcrText(),
                    "No readable learning matter "
                            + "is available on this page."
            );
        }

        return android.text.TextUtils.join(
                "\n\n",
                parts
        );
    }

    private static void addUniquePart(
            @NonNull List<String> parts,
            @Nullable String value
    ) {
        String safeValue = safeText(value);

        if (!safeValue.isEmpty()
                && !parts.contains(safeValue)) {
            parts.add(safeValue);
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
            // Preserve OCR text when structured JSON is incomplete.
        }

        return value;
    }

    @NonNull
    private static String readablePageType(
            @Nullable String pageType
    ) {
        String value =
                safeText(pageType)
                        .replace('_', ' ')
                        .toUpperCase(Locale.getDefault());

        return value.isEmpty()
                ? "LEARN"
                : value;
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
        return binding != null
                && !isFinishing()
                && !isDestroyed();
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
