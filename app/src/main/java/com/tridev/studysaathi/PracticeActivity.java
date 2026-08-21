package com.tridev.studysaathi;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.QuizReviewAdapter;
import com.tridev.studysaathi.data.catalog.PracticeCatalog;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.QuizAttemptRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityPracticeBinding;
import com.tridev.studysaathi.model.PracticeQuestion;
import com.tridev.studysaathi.model.QuizReviewItem;
import com.tridev.studysaathi.data.learning.MistakeNotebookStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PracticeActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_NAME =
            "extra_practice_subject_name";

    public static final String EXTRA_CHAPTER_TITLE =
            "extra_practice_chapter_title";

    public static final String EXTRA_STUDENT_CLASS =
            "extra_practice_student_class";

    public static final String EXTRA_EDUCATION_BOARD =
            "extra_practice_education_board";

    public static final String EXTRA_LANGUAGE_MODE =
            "extra_practice_language_mode";

    private ActivityPracticeBinding binding;

    private StudentProfileRepository studentProfileRepository;
    private QuizAttemptRepository quizAttemptRepository;

    private QuizReviewAdapter quizReviewAdapter;

    private final List<PracticeQuestion> allPracticeQuestions =
            new ArrayList<>();

    private final List<PracticeQuestion> activePracticeQuestions =
            new ArrayList<>();

    private final List<PracticeQuestion> wrongQuestionsFromLastAttempt =
            new ArrayList<>();

    private final List<QuizReviewItem> currentAttemptReviewItems =
            new ArrayList<>();

    private String subjectName;
    private String chapterTitle;
    private String studentClass;
    private String educationBoard;

    private LanguageMode languageMode =
            LanguageMode.BILINGUAL;

    private QuizMode quizMode =
            QuizMode.FULL;

    private int currentQuestionIndex;
    private int correctAnswerCount;
    private int attemptSequence;

    private boolean currentQuestionChecked;
    private boolean resultSavedForCurrentAttempt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPracticeBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        quizAttemptRepository =
                new QuizAttemptRepository(this);

        readScreenArguments();
        loadPracticeQuestions();
        setupReviewRecyclerView();
        setupClickListeners();
        showPracticeHeader();
        startFullQuiz();
    }

    private void readScreenArguments() {
        subjectName = getSafeExtra(
                EXTRA_SUBJECT_NAME,
                "Subject"
        );

        chapterTitle = getSafeExtra(
                EXTRA_CHAPTER_TITLE,
                "Chapter"
        );

        studentClass = getSafeExtra(
                EXTRA_STUDENT_CLASS,
                "Class 6"
        );

        educationBoard = getSafeExtra(
                EXTRA_EDUCATION_BOARD,
                "CBSE"
        );

        String languageValue = getSafeExtra(
                EXTRA_LANGUAGE_MODE,
                LanguageMode.BILINGUAL.name()
        );

        try {
            languageMode = LanguageMode.valueOf(
                    languageValue.toUpperCase(
                            Locale.ROOT
                    )
            );
        } catch (IllegalArgumentException exception) {
            languageMode = LanguageMode.BILINGUAL;
        }
    }

    @NonNull
    private String getSafeExtra(
            @NonNull String key,
            @NonNull String fallback
    ) {
        String value =
                getIntent().getStringExtra(key);

        if (value == null
                || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private void loadPracticeQuestions() {
        List<PracticeQuestion> loadedQuestions =
                PracticeCatalog.getQuestions(
                        subjectName,
                        chapterTitle
                );

        allPracticeQuestions.clear();

        if (loadedQuestions != null) {
            allPracticeQuestions.addAll(
                    loadedQuestions
            );
        }
    }

    private void setupReviewRecyclerView() {
        quizReviewAdapter =
                new QuizReviewAdapter(
                        new ArrayList<>()
                );

        binding.recyclerQuizReview.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerQuizReview.setAdapter(
                quizReviewAdapter
        );

        binding.recyclerQuizReview.setHasFixedSize(
                false
        );

        binding.recyclerQuizReview.setNestedScrollingEnabled(
                false
        );
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                finish()
        );

        binding.buttonCheckAnswer.setOnClickListener(view ->
                checkCurrentAnswer()
        );

        binding.buttonNextQuestion.setOnClickListener(view ->
                moveToNextQuestion()
        );

        binding.buttonRetryWrongQuestions.setOnClickListener(view ->
                startWrongQuestionsQuiz()
        );

        binding.buttonRetryQuiz.setOnClickListener(view ->
                startFullQuiz()
        );

        binding.buttonBackToLesson.setOnClickListener(view ->
                finish()
        );
    }

    private void showPracticeHeader() {
        binding.textPracticeChapter.setText(
                chapterTitle
        );

        binding.textPracticeCurriculum.setText(
                educationBoard
                        + "  •  "
                        + studentClass
                        + "  •  "
                        + subjectName
        );
    }

    private void startFullQuiz() {
        startQuiz(
                new ArrayList<>(
                        allPracticeQuestions
                ),
                QuizMode.FULL
        );
    }

    private void startWrongQuestionsQuiz() {
        if (wrongQuestionsFromLastAttempt.isEmpty()) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.quiz_no_wrong_questions,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        List<PracticeQuestion> retryQuestions =
                new ArrayList<>(
                        wrongQuestionsFromLastAttempt
                );

        startQuiz(
                retryQuestions,
                QuizMode.WRONG_ONLY
        );
    }

    private void startQuiz(
            @NonNull List<PracticeQuestion> questions,
            @NonNull QuizMode requestedQuizMode
    ) {
        attemptSequence++;

        quizMode = requestedQuizMode;

        activePracticeQuestions.clear();
        activePracticeQuestions.addAll(questions);

        currentAttemptReviewItems.clear();
        wrongQuestionsFromLastAttempt.clear();

        quizReviewAdapter.submitList(
                new ArrayList<>()
        );

        currentQuestionIndex = 0;
        correctAnswerCount = 0;

        currentQuestionChecked = false;
        resultSavedForCurrentAttempt = false;

        binding.quizSection.setVisibility(
                View.VISIBLE
        );

        binding.resultSection.setVisibility(
                View.GONE
        );

        binding.reviewSection.setVisibility(
                View.GONE
        );

        binding.buttonRetryWrongQuestions.setVisibility(
                View.GONE
        );

        binding.progressPractice.setMax(
                Math.max(
                        1,
                        activePracticeQuestions.size()
                )
        );

        if (quizMode == QuizMode.WRONG_ONLY) {
            binding.textQuizMode.setText(
                    getString(
                            R.string.quiz_mode_wrong_only_format,
                            activePracticeQuestions.size()
                    )
            );
        } else {
            binding.textQuizMode.setText(
                    R.string.quiz_mode_full
            );
        }

        if (activePracticeQuestions.isEmpty()) {
            showNoQuestionsState();
            return;
        }

        showCurrentQuestion();

        binding.practiceScrollView.smoothScrollTo(
                0,
                0
        );
    }

    private void showCurrentQuestion() {
        if (currentQuestionIndex < 0
                || currentQuestionIndex
                >= activePracticeQuestions.size()) {

            showQuizResult();
            return;
        }

        currentQuestionChecked = false;

        PracticeQuestion question =
                activePracticeQuestions.get(
                        currentQuestionIndex
                );

        binding.radioGroupOptions.clearCheck();

        setOptionsEnabled(true);
        resetOptionTextColors();

        binding.cardFeedback.setVisibility(
                View.GONE
        );

        binding.buttonNextQuestion.setVisibility(
                View.GONE
        );

        binding.buttonCheckAnswer.setVisibility(
                View.VISIBLE
        );

        binding.buttonCheckAnswer.setEnabled(
                true
        );

        binding.textQuestionCounter.setText(
                getString(
                        R.string.practice_question_counter_format,
                        currentQuestionIndex + 1,
                        activePracticeQuestions.size()
                )
        );

        binding.progressPractice.setProgressCompat(
                currentQuestionIndex + 1,
                true
        );

        binding.textQuestion.setText(
                getQuestionText(question)
        );

        List<String> options =
                getQuestionOptions(question);

        binding.optionOne.setText(
                getOptionText(options, 0)
        );

        binding.optionTwo.setText(
                getOptionText(options, 1)
        );

        binding.optionThree.setText(
                getOptionText(options, 2)
        );

        binding.optionFour.setText(
                getOptionText(options, 3)
        );

        binding.buttonNextQuestion.setText(
                currentQuestionIndex
                        == activePracticeQuestions.size() - 1
                        ? R.string.view_practice_result
                        : R.string.next_question
        );
    }

    private void checkCurrentAnswer() {
        if (currentQuestionChecked) {
            return;
        }

        int selectedOptionIndex =
                getSelectedOptionIndex();

        if (selectedOptionIndex < 0) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.select_practice_answer,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        currentQuestionChecked = true;

        PracticeQuestion question =
                activePracticeQuestions.get(
                        currentQuestionIndex
                );

        int correctOptionIndex =
                question.getCorrectOptionIndex();

        boolean correct =
                selectedOptionIndex
                        == correctOptionIndex;

        if (correct) {
            correctAnswerCount++;
        } else {
            wrongQuestionsFromLastAttempt.add(
                    question
            );
            new MistakeNotebookStore(this).record(
                    subjectName, chapterTitle, getQuestionText(question),
                    languageMode == LanguageMode.ENGLISH
                            ? question.getEnglishExplanation() : question.getHindiExplanation());
        }

        addQuestionReview(
                question,
                selectedOptionIndex,
                correctOptionIndex,
                correct
        );

        showAnswerFeedback(
                question,
                selectedOptionIndex,
                correct
        );

        setOptionsEnabled(false);

        binding.buttonCheckAnswer.setVisibility(
                View.GONE
        );

        binding.buttonNextQuestion.setVisibility(
                View.VISIBLE
        );
    }

    private void addQuestionReview(
            @NonNull PracticeQuestion question,
            int selectedOptionIndex,
            int correctOptionIndex,
            boolean correct
    ) {
        List<String> options =
                getQuestionOptions(question);

        currentAttemptReviewItems.add(
                new QuizReviewItem(
                        currentQuestionIndex + 1,
                        getQuestionText(question),
                        getOptionText(
                                options,
                                selectedOptionIndex
                        ),
                        getOptionText(
                                options,
                                correctOptionIndex
                        ),
                        getExplanationText(question),
                        correct
                )
        );
    }

    @NonNull
    private String getOptionText(
            @NonNull List<String> options,
            int optionIndex
    ) {
        if (optionIndex < 0
                || optionIndex >= options.size()) {
            return "";
        }

        String optionText =
                options.get(optionIndex);

        return optionText == null
                ? ""
                : optionText;
    }

    private void showAnswerFeedback(
            @NonNull PracticeQuestion question,
            int selectedOptionIndex,
            boolean correct
    ) {
        int backgroundColor;
        int borderColor;
        int titleColor;

        if (correct) {
            binding.textFeedbackTitle.setText(
                    R.string.practice_correct_answer
            );

            backgroundColor =
                    ContextCompat.getColor(
                            this,
                            R.color.ss_green_soft
                    );

            borderColor =
                    ContextCompat.getColor(
                            this,
                            R.color.ss_green_border
                    );

            titleColor =
                    ContextCompat.getColor(
                            this,
                            R.color.ss_success
                    );
        } else {
            binding.textFeedbackTitle.setText(
                    R.string.practice_incorrect_answer
            );

            backgroundColor =
                    ContextCompat.getColor(
                            this,
                            R.color.ss_red_soft
                    );

            borderColor =
                    ContextCompat.getColor(
                            this,
                            R.color.ss_red_border
                    );

            titleColor =
                    ContextCompat.getColor(
                            this,
                            R.color.ss_error
                    );
        }

        binding.cardFeedback.setCardBackgroundColor(
                backgroundColor
        );

        binding.cardFeedback.setStrokeColor(
                borderColor
        );

        binding.textFeedbackTitle.setTextColor(
                titleColor
        );

        binding.textFeedbackExplanation.setText(
                getExplanationText(question)
        );

        binding.cardFeedback.setVisibility(
                View.VISIBLE
        );

        highlightAnswers(
                question.getCorrectOptionIndex(),
                selectedOptionIndex
        );
    }

    private void highlightAnswers(
            int correctOptionIndex,
            int selectedOptionIndex
    ) {
        RadioButton[] optionButtons = {
                binding.optionOne,
                binding.optionTwo,
                binding.optionThree,
                binding.optionFour
        };

        int normalColor =
                ContextCompat.getColor(
                        this,
                        R.color.ss_text_primary
                );

        int successColor =
                ContextCompat.getColor(
                        this,
                        R.color.ss_success
                );

        int errorColor =
                ContextCompat.getColor(
                        this,
                        R.color.ss_error
                );

        for (int index = 0;
             index < optionButtons.length;
             index++) {

            RadioButton optionButton =
                    optionButtons[index];

            if (index == correctOptionIndex) {
                optionButton.setTextColor(
                        successColor
                );
            } else if (index == selectedOptionIndex) {
                optionButton.setTextColor(
                        errorColor
                );
            } else {
                optionButton.setTextColor(
                        normalColor
                );
            }
        }
    }

    private void moveToNextQuestion() {
        if (!currentQuestionChecked) {
            return;
        }

        if (currentQuestionIndex
                >= activePracticeQuestions.size() - 1) {

            showQuizResult();
            return;
        }

        currentQuestionIndex++;
        showCurrentQuestion();
    }

    private void showQuizResult() {
        binding.quizSection.setVisibility(
                View.GONE
        );

        binding.resultSection.setVisibility(
                View.VISIBLE
        );

        int totalQuestions =
                activePracticeQuestions.size();

        int incorrectAnswerCount =
                Math.max(
                        0,
                        totalQuestions
                                - correctAnswerCount
                );

        binding.textScoreValue.setText(
                getString(
                        R.string.practice_score_format,
                        correctAnswerCount,
                        totalQuestions
                )
        );

        binding.textCorrectAnswerCount.setText(
                getString(
                        R.string.quiz_result_correct_count_format,
                        correctAnswerCount
                )
        );

        binding.textIncorrectAnswerCount.setText(
                getString(
                        R.string.quiz_result_incorrect_count_format,
                        incorrectAnswerCount
                )
        );

        int percentage =
                totalQuestions == 0
                        ? 0
                        : Math.round(
                        correctAnswerCount
                        * 100f
                        / totalQuestions
                );

        binding.progressResult.setProgressCompat(
                percentage,
                true
        );

        binding.textResultPercentage.setText(
                getString(
                        R.string.quiz_score_saving_format,
                        percentage
                )
        );

        if (percentage >= 80) {
            binding.textResultTitle.setText(
                    R.string.practice_result_excellent
            );

            binding.textResultMessage.setText(
                    R.string.practice_result_excellent_message
            );
        } else if (percentage >= 50) {
            binding.textResultTitle.setText(
                    R.string.practice_result_good
            );

            binding.textResultMessage.setText(
                    R.string.practice_result_good_message
            );
        } else {
            binding.textResultTitle.setText(
                    R.string.practice_result_retry
            );

            binding.textResultMessage.setText(
                    R.string.practice_result_retry_message
            );
        }

        quizReviewAdapter.submitList(
                new ArrayList<>(
                        currentAttemptReviewItems
                )
        );

        binding.reviewSection.setVisibility(
                currentAttemptReviewItems.isEmpty()
                        ? View.GONE
                        : View.VISIBLE
        );

        if (incorrectAnswerCount > 0) {
            binding.buttonRetryWrongQuestions.setVisibility(
                    View.VISIBLE
            );

            binding.buttonRetryWrongQuestions.setText(
                    getString(
                            R.string.retry_wrong_questions_format,
                            incorrectAnswerCount
                    )
            );
        } else {
            binding.buttonRetryWrongQuestions.setVisibility(
                    View.GONE
            );
        }

        binding.buttonRetryQuiz.setVisibility(
                allPracticeQuestions.isEmpty()
                        ? View.GONE
                        : View.VISIBLE
        );

        binding.buttonRetryQuiz.setText(
                R.string.retry_full_quiz
        );

        if (!resultSavedForCurrentAttempt) {
            resultSavedForCurrentAttempt = true;

            int currentAttemptToken =
                    attemptSequence;

            saveQuizAttempt(
                    totalQuestions,
                    correctAnswerCount,
                    percentage,
                    currentAttemptToken
            );
        }

        binding.practiceScrollView.smoothScrollTo(
                0,
                0
        );
    }

    private void saveQuizAttempt(
            int totalQuestions,
            int correctAnswers,
            int percentage,
            int attemptToken
    ) {
        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository
                        .SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        if (studentProfile == null) {
                            showUnsavedResult(
                                    percentage,
                                    R.string.quiz_result_profile_unavailable,
                                    attemptToken
                            );

                            return;
                        }

                        createAndSaveQuizAttempt(
                                studentProfile.getProfileId(),
                                totalQuestions,
                                correctAnswers,
                                percentage,
                                attemptToken
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

                        showUnsavedResult(
                                percentage,
                                R.string.quiz_result_profile_unavailable,
                                attemptToken
                        );
                    }
                }
        );
    }

    private void createAndSaveQuizAttempt(
            long profileId,
            int totalQuestions,
            int correctAnswers,
            int percentage,
            int attemptToken
    ) {
        QuizAttemptEntity quizAttempt =
                new QuizAttemptEntity();

        quizAttempt.setProfileId(profileId);
        quizAttempt.setEducationBoard(educationBoard);
        quizAttempt.setStudentClass(studentClass);
        quizAttempt.setSubjectName(subjectName);
        quizAttempt.setChapterTitle(chapterTitle);
        quizAttempt.setCorrectAnswers(correctAnswers);
        quizAttempt.setTotalQuestions(totalQuestions);
        quizAttempt.setPercentage(percentage);

        quizAttempt.setAttemptedAt(
                System.currentTimeMillis()
        );

        quizAttemptRepository.saveAttempt(
                quizAttempt,
                new QuizAttemptRepository
                        .InsertAttemptCallback() {
                    @Override
                    public void onSuccess(
                            long attemptId
                    ) {
                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        loadUpdatedQuizStats(
                                profileId,
                                percentage,
                                attemptToken
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

                        showUnsavedResult(
                                percentage,
                                R.string.quiz_result_save_failed,
                                attemptToken
                        );
                    }
                }
        );
    }

    private void loadUpdatedQuizStats(
            long profileId,
            int currentPercentage,
            int attemptToken
    ) {
        quizAttemptRepository.getChapterStats(
                profileId,
                educationBoard,
                studentClass,
                subjectName,
                chapterTitle,
                new QuizAttemptRepository
                        .QuizStatsCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull QuizAttemptRepository
                                    .QuizStats quizStats
                    ) {
                        if (isFinishing()
                                || isDestroyed()
                                || !isCurrentResultVisible(
                                attemptToken
                        )) {
                            return;
                        }

                        binding.textResultPercentage.setText(
                                getString(
                                        R.string.quiz_score_summary_format,
                                        currentPercentage,
                                        quizStats.getBestPercentage(),
                                        quizStats.getAttemptCount()
                                )
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing()
                                || isDestroyed()
                                || !isCurrentResultVisible(
                                attemptToken
                        )) {
                            return;
                        }

                        binding.textResultPercentage.setText(
                                getString(
                                        R.string.quiz_score_saved_format,
                                        currentPercentage
                                )
                        );
                    }
                }
        );
    }

    private void showUnsavedResult(
            int percentage,
            int messageRes,
            int attemptToken
    ) {
        if (!isCurrentResultVisible(
                attemptToken
        )) {
            return;
        }

        binding.textResultPercentage.setText(
                getString(
                        R.string.quiz_score_unsaved_format,
                        percentage
                )
        );

        Snackbar.make(
                binding.getRoot(),
                messageRes,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private boolean isCurrentResultVisible(
            int attemptToken
    ) {
        return attemptSequence == attemptToken
                && binding.resultSection.getVisibility()
                == View.VISIBLE;
    }

    private void showNoQuestionsState() {
        binding.quizSection.setVisibility(
                View.GONE
        );

        binding.resultSection.setVisibility(
                View.VISIBLE
        );

        binding.reviewSection.setVisibility(
                View.GONE
        );

        binding.textScoreValue.setText("0/0");

        binding.textCorrectAnswerCount.setText(
                getString(
                        R.string.quiz_result_correct_count_format,
                        0
                )
        );

        binding.textIncorrectAnswerCount.setText(
                getString(
                        R.string.quiz_result_incorrect_count_format,
                        0
                )
        );

        binding.progressResult.setProgressCompat(
                0,
                false
        );

        binding.textResultPercentage.setText(
                R.string.practice_no_score
        );

        binding.textResultTitle.setText(
                R.string.practice_no_questions
        );

        binding.textResultMessage.setText(
                R.string.practice_no_questions_description
        );

        binding.buttonRetryWrongQuestions.setVisibility(
                View.GONE
        );

        binding.buttonRetryQuiz.setVisibility(
                View.GONE
        );
    }

    private int getSelectedOptionIndex() {
        int checkedId =
                binding.radioGroupOptions
                        .getCheckedRadioButtonId();

        if (checkedId == R.id.optionOne) {
            return 0;
        }

        if (checkedId == R.id.optionTwo) {
            return 1;
        }

        if (checkedId == R.id.optionThree) {
            return 2;
        }

        if (checkedId == R.id.optionFour) {
            return 3;
        }

        return -1;
    }

    private void setOptionsEnabled(
            boolean enabled
    ) {
        binding.optionOne.setEnabled(enabled);
        binding.optionTwo.setEnabled(enabled);
        binding.optionThree.setEnabled(enabled);
        binding.optionFour.setEnabled(enabled);
    }

    private void resetOptionTextColors() {
        int normalColor =
                ContextCompat.getColor(
                        this,
                        R.color.ss_text_primary
                );

        binding.optionOne.setTextColor(normalColor);
        binding.optionTwo.setTextColor(normalColor);
        binding.optionThree.setTextColor(normalColor);
        binding.optionFour.setTextColor(normalColor);
    }

    @NonNull
    private String getQuestionText(
            @NonNull PracticeQuestion question
    ) {
        switch (languageMode) {
            case HINDI:
                return question.getHindiQuestion();

            case ENGLISH:
                return question.getEnglishQuestion();

            case BILINGUAL:
            default:
                return getString(
                        R.string.practice_bilingual_format,
                        question.getEnglishQuestion(),
                        question.getHindiQuestion()
                );
        }
    }

    @NonNull
    private List<String> getQuestionOptions(
            @NonNull PracticeQuestion question
    ) {
        if (languageMode == LanguageMode.HINDI) {
            return question.getHindiOptions();
        }

        if (languageMode == LanguageMode.ENGLISH) {
            return question.getEnglishOptions();
        }

        List<String> bilingualOptions =
                new ArrayList<>();

        int optionCount = Math.min(
                question.getEnglishOptions().size(),
                question.getHindiOptions().size()
        );

        for (int index = 0;
             index < optionCount;
             index++) {

            bilingualOptions.add(
                    question.getEnglishOptions().get(index)
                            + "\n"
                            + question.getHindiOptions().get(index)
            );
        }

        return bilingualOptions;
    }

    @NonNull
    private String getExplanationText(
            @NonNull PracticeQuestion question
    ) {
        switch (languageMode) {
            case HINDI:
                return question.getHindiExplanation();

            case ENGLISH:
                return question.getEnglishExplanation();

            case BILINGUAL:
            default:
                return getString(
                        R.string.practice_bilingual_format,
                        question.getEnglishExplanation(),
                        question.getHindiExplanation()
                );
        }
    }

    private enum LanguageMode {
        BILINGUAL,
        HINDI,
        ENGLISH
    }

    private enum QuizMode {
        FULL,
        WRONG_ONLY
    }
}
