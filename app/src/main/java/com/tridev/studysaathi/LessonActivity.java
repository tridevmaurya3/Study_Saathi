package com.tridev.studysaathi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterContentEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository
        .ExactSchoolBookLessonProgressCoordinator;
import com.tridev.studysaathi.data.repository.LessonProgressRepository;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterContentRepository;
import com.tridev.studysaathi.data.repository
        .SchoolBookChapterPageRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityLessonBinding;
import com.tridev.studysaathi.databinding.DialogLessonCompletedBinding;
import com.tridev.studysaathi.mapper
        .SchoolBookChapterContentLessonMapper;
import com.tridev.studysaathi.model.LessonContent;

import java.util.Locale;

public class LessonActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_NAME =
            "extra_lesson_subject_name";

    public static final String EXTRA_CHAPTER_TITLE =
            "extra_lesson_chapter_title";

    public static final String EXTRA_CHAPTER_DESCRIPTION =
            "extra_lesson_chapter_description";

    public static final String EXTRA_STUDENT_CLASS =
            "extra_lesson_student_class";

    public static final String EXTRA_EDUCATION_BOARD =
            "extra_lesson_education_board";

    public static final String EXTRA_REVISION_MODE =
            "extra_revision_mode";

    private static final String PREFERENCES_NAME =
            "study_saathi_reading_preferences";

    private static final String KEY_READING_SIZE =
            "reading_text_size";

    private static final String BOOKMARK_PREFERENCES_NAME =
            "study_saathi_bookmarks_v1";

    private static final float DEFAULT_READING_SIZE = 18f;
    private static final float MIN_READING_SIZE = 15f;
    private static final float MAX_READING_SIZE = 26f;

    private ActivityLessonBinding binding;

    private SharedPreferences readingPreferences;
    private SharedPreferences bookmarkPreferences;

    private StudentProfileRepository studentProfileRepository;
    private LessonProgressRepository lessonProgressRepository;
    private ExactSchoolBookLessonProgressCoordinator
            exactLessonProgressCoordinator;
    private SchoolBookChapterContentRepository
            exactChapterContentRepository;
    private SchoolBookChapterPageRepository
            exactChapterPageRepository;

    private LessonContent lessonContent;
    private LessonProgressEntity currentLessonProgress;

    private String subjectName;
    private String chapterTitle;
    private String chapterDescription;
    private String studentClass;
    private String educationBoard;

    private long activeProfileId = -1L;
    private String activeStudentName = "Student";

    private float readingSizeSp;

    private boolean revisionMode;
    private boolean lessonCompleted;
    private boolean revisionCompletedThisSession;
    private boolean progressSaveInProgress;
    private boolean chapterBookmarked;

    private LanguageMode selectedLanguageMode =
            LanguageMode.BILINGUAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLessonBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        readingPreferences = getSharedPreferences(
                PREFERENCES_NAME,
                MODE_PRIVATE
        );

        bookmarkPreferences = getSharedPreferences(
                BOOKMARK_PREFERENCES_NAME,
                MODE_PRIVATE
        );

        studentProfileRepository =
                new StudentProfileRepository(this);

        lessonProgressRepository =
                new LessonProgressRepository(this);

        exactLessonProgressCoordinator =
                new ExactSchoolBookLessonProgressCoordinator(
                        this,
                        getIntent()
                );

        exactChapterContentRepository =
                new SchoolBookChapterContentRepository(
                        this
                );

        exactChapterPageRepository =
                new SchoolBookChapterPageRepository(
                        this
                );

        readScreenArguments();

        if (subjectName.isEmpty()
                || chapterTitle.isEmpty()) {
            Snackbar.make(
                    binding.getRoot(),
                    "Verified subject और chapter उपलब्ध नहीं है। Parent से curriculum confirm कराएँ।",
                    Snackbar.LENGTH_LONG
            ).addCallback(
                    new Snackbar.Callback() {
                        @Override
                        public void onDismissed(
                                Snackbar transientBottomBar,
                                int event
                        ) {
                            finish();
                        }
                    }
            ).show();
            return;
        }

        loadLessonContent();
        loadSavedReadingSize();

        setupClickListeners();
        setupLanguageToggle();
        showLessonHeader();
        showLessonContent();
        showApprovedContentLoadingState();
        applyReadingTextSize();
        showBookmarkLoadingState();

        loadApprovedExactChapterContent();
        openPageReaderIfAvailable();
        markExactChapterOpened();
        loadActiveStudentAndProgress();
    }

    private void openPageReaderIfAvailable() {
        if (!exactLessonProgressCoordinator
                .isExactSchoolBookLesson()) {
            return;
        }

        long exactChapterRowId =
                exactLessonProgressCoordinator
                        .getExactChapterRowId();

        if (exactChapterRowId <= 0L) {
            return;
        }

        exactChapterPageRepository
                .getApprovedPagesForChapter(
                        exactChapterRowId,
                        new SchoolBookChapterPageRepository
                                .PagesCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull java.util.List<com.tridev
                                            .studysaathi.data.local.entity
                                            .SchoolBookChapterPageEntity>
                                            pages
                            ) {
                                if (isFinishing()
                                        || isDestroyed()
                                        || pages.isEmpty()) {
                                    return;
                                }

                                Intent readerIntent =
                                        ChapterPageReaderActivity
                                                .createIntent(
                                                        LessonActivity.this,
                                                        exactChapterRowId,
                                                        chapterTitle
                                                );

                                try {
                                    startActivity(readerIntent);
                                    finish();
                                } catch (RuntimeException exception) {
                                    Snackbar.make(
                                            binding.getRoot(),
                                            "Approved chapter pages could "
                                                    + "not be opened. "
                                                    + "Only approved saved "
                                                    + "content will be shown.",
                                            Snackbar.LENGTH_LONG
                                    ).show();
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

                                Snackbar.make(
                                        binding.getRoot(),
                                        "Approved pages could not be "
                                                + "checked. Unverified "
                                                + "fallback content will "
                                                + "not be shown.",
                                        Snackbar.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    private void loadApprovedExactChapterContent() {
        if (!exactLessonProgressCoordinator
                .isExactSchoolBookLesson()) {
            showApprovedContentUnavailableState();
            return;
        }

        long exactChapterRowId =
                exactLessonProgressCoordinator
                        .getExactChapterRowId();

        exactChapterContentRepository
                .getApprovedContentForChapter(
                        exactChapterRowId,
                        new SchoolBookChapterContentRepository
                                .SingleContentCallback() {

                            @Override
                            public void onSuccess(
                                    SchoolBookChapterContentEntity
                                            content
                            ) {
                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                if (content == null) {
                                    showApprovedContentUnavailableState();
                                    return;
                                }

                                lessonContent =
                                        SchoolBookChapterContentLessonMapper
                                                .toLessonContent(
                                                        chapterTitle,
                                                        content
                                                );

                                showApprovedContentAvailableState();
                                showLessonHeader();
                                showLessonContent();
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                showApprovedContentUnavailableState();
                            }
                        }
                );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (activeProfileId > 0L) {
            loadCurrentBookmarkState();
        }
    }

    private void readScreenArguments() {
        subjectName = getSafeExtra(
                EXTRA_SUBJECT_NAME,
                ""
        );

        chapterTitle = getSafeExtra(
                EXTRA_CHAPTER_TITLE,
                ""
        );

        chapterDescription = getSafeExtra(
                EXTRA_CHAPTER_DESCRIPTION,
                ""
        );

        studentClass = getSafeExtra(
                EXTRA_STUDENT_CLASS,
                ""
        );

        educationBoard = getSafeExtra(
                EXTRA_EDUCATION_BOARD,
                ""
        );

        revisionMode = getIntent().getBooleanExtra(
                EXTRA_REVISION_MODE,
                false
        );
    }

    private String getSafeExtra(
            String key,
            String fallback
    ) {
        String value =
                getIntent().getStringExtra(key);

        if (value == null
                || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private void loadLessonContent() {
        lessonContent =
                new LessonContent(
                        chapterTitle,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                );
    }

    private void showApprovedContentLoadingState() {
        binding.textExplanation.setText(
                R.string.lesson_approved_content_loading
        );
        setOptionalLessonSectionsVisible(false);
        binding.buttonPractice.setVisibility(View.GONE);
        binding.buttonCompleteLesson.setEnabled(false);
    }

    private void showApprovedContentUnavailableState() {
        lessonContent =
                new LessonContent(
                        chapterTitle,
                        getString(
                                R.string.lesson_approved_content_missing_en
                        ),
                        getString(
                                R.string.lesson_approved_content_missing_hi
                        ),
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                );

        showLessonHeader();
        showLessonContent();
        setOptionalLessonSectionsVisible(false);
        binding.buttonPractice.setVisibility(View.GONE);
        binding.buttonCompleteLesson.setEnabled(false);
        binding.buttonCompleteLesson.setVisibility(View.GONE);
    }

    private void showApprovedContentAvailableState() {
        setOptionalLessonSectionsVisible(true);
        binding.buttonPractice.setVisibility(View.VISIBLE);
        binding.buttonCompleteLesson.setVisibility(View.VISIBLE);
    }

    private void setOptionalLessonSectionsVisible(
            boolean visible
    ) {
        int visibility =
                visible ? View.VISIBLE : View.GONE;

        binding.textKeyPointsHeading.setVisibility(visibility);
        binding.cardKeyPoints.setVisibility(visibility);
        binding.textExampleHeading.setVisibility(visibility);
        binding.cardExample.setVisibility(visibility);
        binding.textPracticeHeading.setVisibility(visibility);
        binding.cardPracticeQuestion.setVisibility(visibility);
    }

    private void loadSavedReadingSize() {
        readingSizeSp =
                readingPreferences.getFloat(
                        KEY_READING_SIZE,
                        DEFAULT_READING_SIZE
                );

        readingSizeSp = Math.max(
                MIN_READING_SIZE,
                Math.min(
                        MAX_READING_SIZE,
                        readingSizeSp
                )
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonBookmarkChapter.setOnClickListener(view ->
                handleBookmarkButtonClick()
        );

        binding.buttonDecreaseFont.setOnClickListener(view ->
                changeReadingSize(-1f)
        );

        binding.buttonIncreaseFont.setOnClickListener(view ->
                changeReadingSize(1f)
        );

        binding.buttonPractice.setOnClickListener(view ->
                openPracticeQuiz()
        );

        binding.buttonCompleteLesson.setOnClickListener(view -> {
            if (progressSaveInProgress) {
                return;
            }

            if (revisionMode) {
                if (!revisionCompletedThisSession) {
                    completeRevision();
                }

                return;
            }

            if (!lessonCompleted) {
                completeLesson();
            }
        });
    }

    private void handleBookmarkButtonClick() {
        if (activeProfileId <= 0L) {
            showBookmarkUnavailable();

            Snackbar.make(
                    binding.getRoot(),
                    R.string.lesson_bookmark_profile_required,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        if (chapterBookmarked) {
            confirmRemoveCurrentBookmark();
        } else {
            addCurrentChapterBookmark();
        }
    }

    private void addCurrentChapterBookmark() {
        if (activeProfileId <= 0L) {
            showBookmarkUnavailable();
            return;
        }

        bookmarkPreferences.edit()
                .putBoolean(
                        createCurrentBookmarkKey(),
                        true
                )
                .apply();

        chapterBookmarked = true;
        updateBookmarkButtonState();

        Snackbar.make(
                binding.getRoot(),
                R.string.lesson_bookmark_saved,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void confirmRemoveCurrentBookmark() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.lesson_bookmark_remove_title
                )
                .setMessage(
                        getString(
                                R.string.lesson_bookmark_remove_message_format,
                                chapterTitle
                        )
                )
                .setNegativeButton(
                        R.string.lesson_bookmark_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.lesson_bookmark_remove_action,
                        (dialog, which) ->
                                removeCurrentChapterBookmark()
                )
                .show();
    }

    private void removeCurrentChapterBookmark() {
        if (activeProfileId <= 0L) {
            showBookmarkUnavailable();
            return;
        }

        bookmarkPreferences.edit()
                .remove(
                        createCurrentBookmarkKey()
                )
                .apply();

        chapterBookmarked = false;
        updateBookmarkButtonState();

        Snackbar.make(
                binding.getRoot(),
                R.string.lesson_bookmark_removed,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void loadCurrentBookmarkState() {
        if (activeProfileId <= 0L) {
            showBookmarkUnavailable();
            return;
        }

        chapterBookmarked =
                bookmarkPreferences.getBoolean(
                        createCurrentBookmarkKey(),
                        false
                );

        updateBookmarkButtonState();
    }

    private void showBookmarkLoadingState() {
        chapterBookmarked = false;

        binding.buttonBookmarkChapter.setEnabled(false);

        binding.buttonBookmarkChapter.setText(
                R.string.lesson_bookmark_loading
        );

        binding.buttonBookmarkChapter.setBackgroundTintList(
                ColorStateList.valueOf(
                        getColor(
                                R.color.ss_surface
                        )
                )
        );

        binding.buttonBookmarkChapter.setStrokeColor(
                ColorStateList.valueOf(
                        getColor(
                                R.color.ss_outline
                        )
                )
        );

        binding.buttonBookmarkChapter.setTextColor(
                getColor(
                        R.color.ss_text_muted
                )
        );
    }

    private void showBookmarkUnavailable() {
        chapterBookmarked = false;

        binding.buttonBookmarkChapter.setEnabled(false);

        binding.buttonBookmarkChapter.setText(
                R.string.lesson_bookmark_button
        );

        binding.buttonBookmarkChapter.setBackgroundTintList(
                ColorStateList.valueOf(
                        getColor(
                                R.color.ss_surface
                        )
                )
        );

        binding.buttonBookmarkChapter.setStrokeColor(
                ColorStateList.valueOf(
                        getColor(
                                R.color.ss_outline
                        )
                )
        );

        binding.buttonBookmarkChapter.setTextColor(
                getColor(
                        R.color.ss_text_muted
                )
        );
    }

    private void updateBookmarkButtonState() {
        if (activeProfileId <= 0L) {
            showBookmarkUnavailable();
            return;
        }

        binding.buttonBookmarkChapter.setEnabled(true);

        if (chapterBookmarked) {
            binding.buttonBookmarkChapter.setText(
                    R.string.lesson_bookmarked_button
            );

            binding.buttonBookmarkChapter.setBackgroundTintList(
                    ColorStateList.valueOf(
                            getColor(
                                    R.color.ss_purple_soft
                            )
                    )
            );

            binding.buttonBookmarkChapter.setStrokeColor(
                    ColorStateList.valueOf(
                            getColor(
                                    R.color.ss_purple_border
                            )
                    )
            );

            binding.buttonBookmarkChapter.setTextColor(
                    getColor(
                            R.color.ss_primary
                    )
            );

            return;
        }

        binding.buttonBookmarkChapter.setText(
                R.string.lesson_bookmark_button
        );

        binding.buttonBookmarkChapter.setBackgroundTintList(
                ColorStateList.valueOf(
                        getColor(
                                R.color.ss_surface
                        )
                )
        );

        binding.buttonBookmarkChapter.setStrokeColor(
                ColorStateList.valueOf(
                        getColor(
                                R.color.ss_blue_border
                        )
                )
        );

        binding.buttonBookmarkChapter.setTextColor(
                getColor(
                        R.color.ss_primary
                )
        );
    }

    @NonNull
    private String createCurrentBookmarkKey() {
        return "profile_"
                + activeProfileId
                + "_subject_"
                + normalizeForBookmarkKey(subjectName)
                + "_chapter_"
                + normalizeForBookmarkKey(chapterTitle);
    }

    @NonNull
    private String normalizeForBookmarkKey(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        "_"
                );
    }

    private void openPracticeQuiz() {
        Intent practiceIntent = new Intent(
                LessonActivity.this,
                PracticeActivity.class
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_SUBJECT_NAME,
                subjectName
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_CHAPTER_TITLE,
                chapterTitle
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_STUDENT_CLASS,
                studentClass
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_EDUCATION_BOARD,
                educationBoard
        );

        practiceIntent.putExtra(
                PracticeActivity.EXTRA_LANGUAGE_MODE,
                selectedLanguageMode.name()
        );

        startActivity(practiceIntent);
    }

    private void setupLanguageToggle() {
        binding.toggleLessonLanguage.check(
                R.id.buttonBilingual
        );

        binding.toggleLessonLanguage
                .addOnButtonCheckedListener(
                        (group, checkedId, isChecked) -> {
                            if (!isChecked) {
                                return;
                            }

                            selectedLanguageMode =
                                    getLanguageMode(
                                            checkedId
                                    );

                            showLessonContent();
                        }
                );
    }

    private LanguageMode getLanguageMode(
            @IdRes int checkedButtonId
    ) {
        if (checkedButtonId
                == R.id.buttonHindi) {
            return LanguageMode.HINDI;
        }

        if (checkedButtonId
                == R.id.buttonEnglish) {
            return LanguageMode.ENGLISH;
        }

        return LanguageMode.BILINGUAL;
    }

    private void showLessonHeader() {
        binding.textLessonChapterTitle.setText(
                chapterTitle
        );

        binding.textLessonCurriculum.setText(
                joinCurriculumDetails(
                        educationBoard,
                        studentClass,
                        subjectName
                )
        );

        binding.textLessonTitle.setText(
                lessonContent.getLessonTitle()
        );
    }

    @NonNull
    private String joinCurriculumDetails(
            String board,
            String classValue,
            String subject
    ) {
        StringBuilder details = new StringBuilder();
        appendCurriculumPart(details, board);
        appendCurriculumPart(details, classValue);
        appendCurriculumPart(details, subject);
        return details.length() == 0
                ? "Verified lesson"
                : details.toString();
    }

    private void appendCurriculumPart(
            @NonNull StringBuilder details,
            String value
    ) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.isEmpty()) {
            return;
        }
        if (details.length() > 0) {
            details.append("  •  ");
        }
        details.append(safeValue);
    }

    private void showLessonContent() {
        switch (selectedLanguageMode) {
            case HINDI:
                showHindiContent();
                break;

            case ENGLISH:
                showEnglishContent();
                break;

            case BILINGUAL:
            default:
                showBilingualContent();
                break;
        }

        applyReadingTextSize();
    }

    private void showHindiContent() {
        binding.textExplanation.setText(
                lessonContent.getHindiExplanation()
        );

        binding.textKeyPoints.setText(
                lessonContent.getHindiKeyPoints()
        );

        binding.textExample.setText(
                lessonContent.getHindiExample()
        );

        binding.textPracticeQuestion.setText(
                lessonContent
                        .getHindiPracticeQuestion()
        );
    }

    private void showEnglishContent() {
        binding.textExplanation.setText(
                lessonContent.getEnglishExplanation()
        );

        binding.textKeyPoints.setText(
                lessonContent.getEnglishKeyPoints()
        );

        binding.textExample.setText(
                lessonContent.getEnglishExample()
        );

        binding.textPracticeQuestion.setText(
                lessonContent
                        .getEnglishPracticeQuestion()
        );
    }

    private void showBilingualContent() {
        binding.textExplanation.setText(
                getString(
                        R.string.bilingual_content_format,
                        lessonContent.getEnglishExplanation(),
                        lessonContent.getHindiExplanation()
                )
        );

        binding.textKeyPoints.setText(
                getString(
                        R.string.bilingual_content_format,
                        lessonContent.getEnglishKeyPoints(),
                        lessonContent.getHindiKeyPoints()
                )
        );

        binding.textExample.setText(
                getString(
                        R.string.bilingual_content_format,
                        lessonContent.getEnglishExample(),
                        lessonContent.getHindiExample()
                )
        );

        binding.textPracticeQuestion.setText(
                getString(
                        R.string.bilingual_content_format,
                        lessonContent
                                .getEnglishPracticeQuestion(),
                        lessonContent
                                .getHindiPracticeQuestion()
                )
        );
    }

    private void loadActiveStudentAndProgress() {
        binding.buttonCompleteLesson.setEnabled(false);

        binding.buttonCompleteLesson.setText(
                R.string.lesson_progress_loading
        );

        showBookmarkLoadingState();

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository
                        .SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (studentProfile == null) {
                            activeProfileId = -1L;

                            showBookmarkUnavailable();
                            showProgressUnavailable();
                            return;
                        }

                        activeProfileId =
                                studentProfile.getProfileId();

                        activeStudentName =
                                studentProfile.getStudentName();

                        if (studentClass.isEmpty()) {
                            studentClass =
                                    studentProfile.getStudentClass();
                        }

                        if (educationBoard.isEmpty()) {
                            educationBoard =
                                    studentProfile.getEducationBoard();
                        }

                        showLessonHeader();

                        loadCurrentBookmarkState();
                        loadExistingLessonProgress();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        activeProfileId = -1L;

                        showBookmarkUnavailable();
                        showProgressUnavailable();
                    }
                }
        );
    }

    private void loadExistingLessonProgress() {
        String progressKey =
                LessonProgressEntity.createProgressKey(
                        activeProfileId,
                        educationBoard,
                        studentClass,
                        subjectName,
                        chapterTitle
                );

        lessonProgressRepository.getProgress(
                progressKey,
                new LessonProgressRepository
                        .SingleProgressCallback() {
                    @Override
                    public void onSuccess(
                            LessonProgressEntity lessonProgress
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        currentLessonProgress =
                                lessonProgress;

                        if (revisionMode) {
                            if (lessonProgress != null
                                    && lessonProgress
                                    .isCompleted()) {

                                showReadyToReviseState();
                            } else {
                                showRevisionUnavailable();
                            }

                            return;
                        }

                        if (lessonProgress != null
                                && lessonProgress
                                .isCompleted()) {

                            showCompletedState();
                        } else {
                            showReadyToCompleteState();
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (revisionMode) {
                            showRevisionUnavailable();
                        } else {
                            showReadyToCompleteState();
                        }

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.lesson_progress_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void completeLesson() {
        if (activeProfileId <= 0L) {
            showProgressUnavailable();
            return;
        }

        showLessonSavingState();

        long currentTime =
                System.currentTimeMillis();

        LessonProgressEntity lessonProgress =
                new LessonProgressEntity();

        lessonProgress.setProgressKey(
                LessonProgressEntity.createProgressKey(
                        activeProfileId,
                        educationBoard,
                        studentClass,
                        subjectName,
                        chapterTitle
                )
        );

        lessonProgress.setProfileId(
                activeProfileId
        );

        lessonProgress.setEducationBoard(
                educationBoard
        );

        lessonProgress.setStudentClass(
                studentClass
        );

        lessonProgress.setSubjectName(
                subjectName
        );

        lessonProgress.setChapterTitle(
                chapterTitle
        );

        lessonProgress.setProgressPercent(100);
        lessonProgress.setCompleted(true);
        lessonProgress.setLastStudiedAt(currentTime);
        lessonProgress.setCompletedAt(currentTime);
        lessonProgress.setRevisionCount(0);
        lessonProgress.setLastRevisedAt(0L);

        lessonProgressRepository.saveProgress(
                lessonProgress,
                new LessonProgressRepository
                        .OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        currentLessonProgress =
                                lessonProgress;

                        syncExactChapterCompletion();
                        showCompletedState();
                        showAchievementDialog(false);
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showReadyToCompleteState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.lesson_progress_save_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void completeRevision() {
        if (activeProfileId <= 0L
                || currentLessonProgress == null
                || !currentLessonProgress
                .isCompleted()) {

            showRevisionUnavailable();
            return;
        }

        showRevisionSavingState();

        long currentTime =
                System.currentTimeMillis();

        LessonProgressEntity updatedProgress =
                createUpdatedRevisionProgress(
                        currentLessonProgress,
                        currentTime
                );

        lessonProgressRepository.saveProgress(
                updatedProgress,
                new LessonProgressRepository
                        .OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        currentLessonProgress =
                                updatedProgress;

                        showRevisionCompletedState();
                        showAchievementDialog(true);
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showReadyToReviseState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.revision_progress_save_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    @NonNull
    private LessonProgressEntity createUpdatedRevisionProgress(
            @NonNull LessonProgressEntity existingProgress,
            long currentTime
    ) {
        LessonProgressEntity updatedProgress =
                new LessonProgressEntity();

        updatedProgress.setProgressKey(
                existingProgress.getProgressKey()
        );

        updatedProgress.setProfileId(
                existingProgress.getProfileId()
        );

        updatedProgress.setEducationBoard(
                existingProgress.getEducationBoard()
        );

        updatedProgress.setStudentClass(
                existingProgress.getStudentClass()
        );

        updatedProgress.setSubjectName(
                existingProgress.getSubjectName()
        );

        updatedProgress.setChapterTitle(
                existingProgress.getChapterTitle()
        );

        updatedProgress.setProgressPercent(100);
        updatedProgress.setCompleted(true);

        updatedProgress.setCompletedAt(
                existingProgress.getCompletedAt()
        );

        updatedProgress.setLastStudiedAt(
                currentTime
        );

        updatedProgress.setRevisionCount(
                existingProgress.getRevisionCount() + 1
        );

        updatedProgress.setLastRevisedAt(
                currentTime
        );

        return updatedProgress;
    }

    private void showReadyToCompleteState() {
        lessonCompleted = false;
        revisionCompletedThisSession = false;
        progressSaveInProgress = false;

        binding.buttonCompleteLesson.setEnabled(true);

        binding.buttonCompleteLesson.setText(
                R.string.mark_lesson_complete
        );
    }

    private void showCompletedState() {
        lessonCompleted = true;
        revisionCompletedThisSession = false;
        progressSaveInProgress = false;

        binding.buttonCompleteLesson.setEnabled(false);

        binding.buttonCompleteLesson.setText(
                R.string.lesson_completed_button
        );
    }

    private void showReadyToReviseState() {
        lessonCompleted = true;
        revisionCompletedThisSession = false;
        progressSaveInProgress = false;

        binding.buttonCompleteLesson.setEnabled(true);

        binding.buttonCompleteLesson.setText(
                R.string.mark_revision_complete
        );
    }

    private void showRevisionCompletedState() {
        lessonCompleted = true;
        revisionCompletedThisSession = true;
        progressSaveInProgress = false;

        binding.buttonCompleteLesson.setEnabled(false);

        binding.buttonCompleteLesson.setText(
                R.string.revision_completed_button
        );
    }

    private void showLessonSavingState() {
        progressSaveInProgress = true;

        binding.buttonCompleteLesson.setEnabled(false);

        binding.buttonCompleteLesson.setText(
                R.string.lesson_progress_saving
        );
    }

    private void showRevisionSavingState() {
        progressSaveInProgress = true;

        binding.buttonCompleteLesson.setEnabled(false);

        binding.buttonCompleteLesson.setText(
                R.string.revision_progress_saving
        );
    }

    private void showRevisionUnavailable() {
        lessonCompleted = false;
        revisionCompletedThisSession = false;
        progressSaveInProgress = false;

        binding.buttonCompleteLesson.setEnabled(false);

        binding.buttonCompleteLesson.setText(
                R.string.mark_revision_complete
        );

        Snackbar.make(
                binding.getRoot(),
                R.string.revision_progress_unavailable,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showProgressUnavailable() {
        lessonCompleted = false;
        revisionCompletedThisSession = false;
        progressSaveInProgress = false;

        binding.buttonCompleteLesson.setEnabled(false);

        binding.buttonCompleteLesson.setText(
                revisionMode
                        ? R.string.mark_revision_complete
                        : R.string.mark_lesson_complete
        );

        Snackbar.make(
                binding.getRoot(),
                R.string.lesson_progress_unavailable,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showAchievementDialog(
            boolean revisionAchievement
    ) {
        DialogLessonCompletedBinding dialogBinding =
                DialogLessonCompletedBinding.inflate(
                        getLayoutInflater()
                );

        if (revisionAchievement) {
            dialogBinding.textCelebrationTitle.setText(
                    getString(
                            R.string.revision_celebration_title_format,
                            activeStudentName
                    )
            );

            dialogBinding.textCelebrationSubtitle.setText(
                    R.string.revision_celebration_subtitle
            );

            dialogBinding.textCelebrationBadge.setText(
                    R.string.revision_completed_badge
            );

            dialogBinding.textCelebrationMessage.setText(
                    R.string.revision_celebration_message
            );

            dialogBinding.buttonBackToChapters.setText(
                    R.string.back_to_revision_plan
            );
        } else {
            dialogBinding.textCelebrationTitle.setText(
                    getString(
                            R.string.lesson_celebration_title_format,
                            activeStudentName
                    )
            );

            dialogBinding.textCelebrationSubtitle.setText(
                    R.string.lesson_celebration_subtitle
            );

            dialogBinding.textCelebrationBadge.setText(
                    R.string.lesson_completed_badge
            );

            dialogBinding.textCelebrationMessage.setText(
                    getEncouragementMessage()
            );

            dialogBinding.buttonBackToChapters.setText(
                    R.string.back_to_chapters
            );
        }

        dialogBinding.textCelebrationChapter.setText(
                chapterTitle
        );

        AlertDialog celebrationDialog =
                new MaterialAlertDialogBuilder(this)
                        .setView(
                                dialogBinding.getRoot()
                        )
                        .setCancelable(false)
                        .create();

        dialogBinding.buttonKeepLearning
                .setOnClickListener(
                        view ->
                                celebrationDialog.dismiss()
                );

        dialogBinding.buttonBackToChapters
                .setOnClickListener(
                        view -> {
                            celebrationDialog.dismiss();
                            finish();
                        }
                );

        celebrationDialog.show();

        dialogBinding.celebrationContent.setAlpha(0f);
        dialogBinding.celebrationContent.setScaleX(0.92f);
        dialogBinding.celebrationContent.setScaleY(0.92f);

        dialogBinding.celebrationContent.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350L)
                .start();

        dialogBinding.textCelebrationStar.animate()
                .rotationBy(360f)
                .setDuration(650L)
                .start();
    }

    private String getEncouragementMessage() {
        int messageIndex =
                Math.floorMod(
                        chapterTitle.hashCode(),
                        4
                );

        switch (messageIndex) {
            case 1:
                return getString(
                        R.string.lesson_celebration_message_two
                );

            case 2:
                return getString(
                        R.string.lesson_celebration_message_three
                );

            case 3:
                return getString(
                        R.string.lesson_celebration_message_four
                );

            case 0:
            default:
                return getString(
                        R.string.lesson_celebration_message_one
                );
        }
    }

    private void changeReadingSize(
            float changeAmount
    ) {
        float updatedSize =
                readingSizeSp + changeAmount;

        updatedSize = Math.max(
                MIN_READING_SIZE,
                Math.min(
                        MAX_READING_SIZE,
                        updatedSize
                )
        );

        if (updatedSize == readingSizeSp) {
            Snackbar.make(
                    binding.getRoot(),
                    updatedSize == MIN_READING_SIZE
                            ? R.string.minimum_font_size_reached
                            : R.string.maximum_font_size_reached,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        readingSizeSp = updatedSize;

        readingPreferences.edit()
                .putFloat(
                        KEY_READING_SIZE,
                        readingSizeSp
                )
                .apply();

        applyReadingTextSize();
    }

    private void applyReadingTextSize() {
        binding.textExplanation.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                readingSizeSp
        );

        binding.textKeyPoints.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                readingSizeSp
        );

        binding.textExample.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                readingSizeSp
        );

        binding.textPracticeQuestion.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                readingSizeSp
        );

        binding.textFontSize.setText(
                String.format(
                        Locale.getDefault(),
                        "%.0fsp",
                        readingSizeSp
                )
        );

        binding.buttonDecreaseFont.setEnabled(
                readingSizeSp > MIN_READING_SIZE
        );

        binding.buttonIncreaseFont.setEnabled(
                readingSizeSp < MAX_READING_SIZE
        );
    }

    private void markExactChapterOpened() {
        exactLessonProgressCoordinator.markLessonOpened(
                new ExactSchoolBookLessonProgressCoordinator
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        // Exact chapter open time saved successfully.
                    }

                    @Override
                    public void onSkipped() {
                        // Generic catalog lessons do not require exact sync.
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        // Opening the lesson must remain available even if
                        // the non-critical timestamp update fails.
                    }
                }
        );
    }

    private void syncExactChapterCompletion() {
        exactLessonProgressCoordinator.markLessonCompleted(
                new ExactSchoolBookLessonProgressCoordinator
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        // Exact chapter progress is now synchronized.
                    }

                    @Override
                    public void onSkipped() {
                        // Generic catalog progress was already saved.
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Snackbar.make(
                                binding.getRoot(),
                                "Lesson completed, but exact chapter "
                                        + "progress could not be synchronized.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private enum LanguageMode {
        BILINGUAL,
        HINDI,
        ENGLISH
    }
}
