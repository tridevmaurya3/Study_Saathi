package com.tridev.studysaathi.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.AskStudySaathiActivity;
import com.tridev.studysaathi.BackupExportActivity;
import com.tridev.studysaathi.BackupRestoreActivity;
import com.tridev.studysaathi.BookChapterBoundaryReviewActivity;
import com.tridev.studysaathi.BookContentsReviewActivity;
import com.tridev.studysaathi.BookContentsScanActivity;
import com.tridev.studysaathi.BookCoverScanActivity;
import com.tridev.studysaathi.BookLearningImportActivity;
import com.tridev.studysaathi.BookMatchReviewActivity;
import com.tridev.studysaathi.CloudAccountActivity;
import com.tridev.studysaathi.CloudBackupDiagnosticActivity;
import com.tridev.studysaathi.DoubtHistoryActivity;
import com.tridev.studysaathi.MainActivity;
import com.tridev.studysaathi.ManualSchoolBookActivity;
import com.tridev.studysaathi.ManualSchoolBookChapterActivity;
import com.tridev.studysaathi.ParentDashboardActivity;
import com.tridev.studysaathi.R;
import com.tridev.studysaathi.SchoolBookChapterContentEditorActivity;
import com.tridev.studysaathi.SchoolBookChapterPageReviewActivity;
import com.tridev.studysaathi.SchoolCurriculumSetupActivity;
import com.tridev.studysaathi.StudentProfileActivity;
import com.tridev.studysaathi.StudentProfilesActivity;
import com.tridev.studysaathi.data.ai.FirebaseStudyTutorClient;
import com.tridev.studysaathi.data.ai.QuestionImageBitmapLoader;
import com.tridev.studysaathi.data.ai.SmartCompanionConversationStore;
import com.tridev.studysaathi.data.ai.SmartTutorAnswerResult;
import com.tridev.studysaathi.data.catalog.DoubtAssistantEngine;
import com.tridev.studysaathi.data.local.entity.DoubtHistoryEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterContentEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.DoubtHistoryRepository;
import com.tridev.studysaathi.data.repository.SchoolBookChapterContentRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.mapper.SchoolBookChapterContentLessonMapper;
import com.tridev.studysaathi.model.LessonContent;
import com.tridev.studysaathi.navigation.ExactSchoolBookLessonContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Student-learning screens पर स्थिर, inline और persistent Study Saathi chat।
 */
public final class SmartAiCompanionController {

    public static final String EXTRA_OPEN_INPUT_MODE =
            "extra_open_input_mode";
    public static final String INPUT_MODE_VOICE = "voice";
    public static final String INPUT_MODE_PHOTO = "photo";

    private static final int VIEW_TAG_KEY =
            R.id.smartAiCompanionRoot;

    @NonNull
    private static final Map<Activity, CompanionSession> SESSIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SmartAiCompanionController() {
    }

    public static void attach(@NonNull Activity activity) {
        if (!isStudentLearningScreen(activity)
                || activity.isFinishing()
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && activity.isDestroyed())) {
            return;
        }

        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) {
            return;
        }

        if (content.getTag(VIEW_TAG_KEY) != null) {
            CompanionSession existingSession = SESSIONS.get(activity);
            if (existingSession != null) {
                existingSession.refreshFromStore();
            }
            return;
        }

        FrameLayout host = (FrameLayout) content;
        View companion = LayoutInflater.from(activity).inflate(
                R.layout.view_smart_ai_companion,
                host,
                false
        );

        int margin = dp(activity, 12);
        int availableWidth = activity.getResources()
                .getDisplayMetrics().widthPixels - (margin * 2);
        int panelWidth = Math.min(dp(activity, 360), availableWidth);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        host.addView(companion, params);
        content.setTag(VIEW_TAG_KEY, companion);

        View panel = companion.findViewById(
                R.id.smartAiCompanionPanel
        );
        FrameLayout.LayoutParams panelParams =
                (FrameLayout.LayoutParams) panel.getLayoutParams();
        panelParams.width = panelWidth;
        panelParams.gravity = Gravity.END | Gravity.BOTTOM;
        panel.setLayoutParams(panelParams);

        CompanionSession session = new CompanionSession(
                activity,
                companion
        );
        SESSIONS.put(activity, session);
        session.start();

        if (activity instanceof AppCompatActivity) {
            SmartAiCompanionInputFragment.ensureAttached(
                    (AppCompatActivity) activity
            );
        }
    }

    public static void detach(@NonNull Activity activity) {
        CompanionSession session = SESSIONS.remove(activity);
        if (session != null) {
            session.close();
        }

        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) {
            return;
        }

        Object tagged = content.getTag(VIEW_TAG_KEY);
        if (tagged instanceof View) {
            ((FrameLayout) content).removeView((View) tagged);
        }
        content.setTag(VIEW_TAG_KEY, null);
    }

    static void deliverVoiceQuestion(
            @NonNull Activity activity,
            @NonNull String spokenQuestion
    ) {
        CompanionSession session = SESSIONS.get(activity);
        if (session != null) {
            session.submitQuestion(spokenQuestion, null);
        }
    }

    static void deliverQuestionPhoto(
            @NonNull Activity activity,
            @NonNull Uri imageUri
    ) {
        CompanionSession session = SESSIONS.get(activity);
        if (session != null) {
            session.submitPhoto(imageUri);
        }
    }

    static void deliverCameraBitmap(
            @NonNull Activity activity,
            @NonNull Bitmap bitmap
    ) {
        CompanionSession session = SESSIONS.get(activity);
        if (session != null) {
            session.submitQuestion(
                    activity.getString(
                            R.string.smart_companion_photo_question
                    ),
                    bitmap
            );
        } else if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private static final class CompanionSession {

        @NonNull
        private final Activity activity;
        @NonNull
        private final View companion;
        @NonNull
        private final MaterialCardView card;
        @NonNull
        private final View dismissArea;
        @NonNull
        private final EditText questionInput;
        @NonNull
        private final LinearLayout conversationContainer;
        @NonNull
        private final NestedScrollView conversationScroll;
        @NonNull
        private final TextView welcomeText;
        @NonNull
        private final TextView statusText;
        @NonNull
        private final TextView aiButton;
        @NonNull
        private final FirebaseStudyTutorClient tutorClient;
        @NonNull
        private final SmartCompanionConversationStore conversationStore;
        @NonNull
        private final DoubtHistoryRepository doubtHistoryRepository;
        @NonNull
        private final QuestionImageBitmapLoader imageLoader;
        @NonNull
        private final List<SmartCompanionConversationStore.Turn> turns =
                new ArrayList<>();

        @Nullable
        private StudentProfileEntity profile;
        @Nullable
        private LessonContent approvedLessonContent;
        @Nullable
        private ObjectAnimator pulseAnimator;

        @NonNull
        private final String subjectName;
        @NonNull
        private final String chapterTitle;
        @NonNull
        private String approvedChapterReference = "";

        private boolean requestInProgress;
        private boolean closed;

        private CompanionSession(
                @NonNull Activity activity,
                @NonNull View companion
        ) {
            this.activity = activity;
            this.companion = companion;
            card = companion.findViewById(R.id.cardSmartAiCompanion);
            dismissArea = companion.findViewById(
                    R.id.smartAiDismissArea
            );
            questionInput = companion.findViewById(
                    R.id.editSmartAiQuestion
            );
            conversationContainer = companion.findViewById(
                    R.id.containerSmartAiConversation
            );
            conversationScroll = companion.findViewById(
                    R.id.scrollSmartAiConversation
            );
            welcomeText = companion.findViewById(
                    R.id.textSmartAiWelcome
            );
            statusText = companion.findViewById(
                    R.id.textSmartAiStatus
            );
            aiButton = companion.findViewById(
                    R.id.buttonSmartAiCompanion
            );
            tutorClient = new FirebaseStudyTutorClient(activity);
            conversationStore =
                    new SmartCompanionConversationStore(activity);
            doubtHistoryRepository =
                    new DoubtHistoryRepository(activity);
            imageLoader = new QuestionImageBitmapLoader(activity);
            subjectName = firstNonEmptyExtra(
                    activity,
                    AskStudySaathiActivity.EXTRA_PREFILL_SUBJECT,
                    "extra_lesson_subject_name",
                    "extra_subject_name",
                    "extra_practice_subject_name",
                    "subject_name"
            );
            chapterTitle = firstNonEmptyExtra(
                    activity,
                    AskStudySaathiActivity.EXTRA_PREFILL_CHAPTER,
                    "extra_reader_chapter_title",
                    "extra_lesson_chapter_title",
                    "extra_chapter_title",
                    "extra_practice_chapter_title",
                    "chapter_title"
            );
        }

        private void start() {
            TextView contextText = companion.findViewById(
                    R.id.textSmartAiContext
            );
            contextText.setText(activity.getString(
                    R.string.smart_companion_context_format,
                    resolvePageContext(activity)
            ));

            aiButton.setOnClickListener(view -> toggleCard());
            dismissArea.setOnClickListener(view -> collapseCard());
            companion.findViewById(R.id.buttonSmartAiSend)
                    .setOnClickListener(view ->
                            submitQuestion(
                                    safe(questionInput.getText()),
                                    null
                            ));
            companion.findViewById(R.id.buttonSmartAiVoice)
                    .setOnClickListener(view -> launchVoice());
            companion.findViewById(R.id.buttonSmartAiPhoto)
                    .setOnClickListener(view -> choosePhotoSource());
            companion.findViewById(R.id.buttonSmartAiSimpler)
                    .setOnClickListener(view ->
                            submitFollowUp(
                                    "पिछले उत्तर को और आसान भाषा में समझाओ।"
                            ));
            companion.findViewById(R.id.buttonSmartAiExample)
                    .setOnClickListener(view ->
                            submitFollowUp(
                                    "पिछले उत्तर का एक आसान रोज़मर्रा का उदाहरण दो।"
                            ));
            companion.findViewById(R.id.buttonSmartAiVisual)
                    .setOnClickListener(view ->
                            submitFollowUp(
                                    "पिछले उत्तर को एक सरल text diagram या step-by-step चित्र जैसी रचना से समझाओ।"
                            ));
            companion.findViewById(R.id.buttonSmartAiQuiz)
                    .setOnClickListener(view ->
                            submitFollowUp(
                                    "पिछले विषय पर कक्षा 6 का एक छोटा सवाल पूछो। अभी उसका उत्तर मत बताना।"
                            ));
            companion.findViewById(R.id.buttonSmartAiNewChat)
                    .setOnClickListener(view -> clearConversation());

            questionInput.setOnEditorActionListener(
                    (view, actionId, event) -> {
                        if (actionId != EditorInfo.IME_ACTION_SEND) {
                            return false;
                        }
                        submitQuestion(safe(questionInput.getText()), null);
                        return true;
                    }
            );

            startPulse();
            showStatus(R.string.smart_companion_profile_loading);
            loadProfile();
            loadApprovedChapterContent();
        }

        private void loadProfile() {
            new StudentProfileRepository(activity).getActiveProfile(
                    new StudentProfileRepository.SingleProfileCallback() {
                        @Override
                        public void onSuccess(
                                @Nullable StudentProfileEntity loadedProfile
                        ) {
                            if (closed) {
                                return;
                            }
                            profile = loadedProfile;
                            hideStatus();
                            if (loadedProfile == null) {
                                return;
                            }
                            turns.clear();
                            turns.addAll(
                                    conversationStore.load(
                                            loadedProfile.getProfileId()
                                    )
                            );
                            renderAllTurns();
                            if (!turns.isEmpty()) {
                                showTemporaryStatus(
                                        R.string.smart_companion_restored
                                );
                            }
                        }

                        @Override
                        public void onError(@NonNull Exception exception) {
                            if (!closed) {
                                hideStatus();
                            }
                        }
                    }
            );
        }

        private void refreshFromStore() {
            StudentProfileEntity activeProfile = profile;
            if (closed || activeProfile == null || requestInProgress) {
                return;
            }
            List<SmartCompanionConversationStore.Turn> saved =
                    conversationStore.load(activeProfile.getProfileId());
            if (saved.size() == turns.size()) {
                return;
            }
            turns.clear();
            turns.addAll(saved);
            renderAllTurns();
        }

        private void loadApprovedChapterContent() {
            long chapterRowId = firstPositiveLongExtra(
                    activity,
                    ExactSchoolBookLessonContract
                            .EXTRA_EXACT_CHAPTER_ROW_ID,
                    "extra_reader_chapter_row_id",
                    "extra_chapter_row_id",
                    "chapter_row_id"
            );
            if (chapterRowId <= 0L) {
                return;
            }

            new SchoolBookChapterContentRepository(activity)
                    .getApprovedContentForChapter(
                            chapterRowId,
                            new SchoolBookChapterContentRepository
                                    .SingleContentCallback() {
                                @Override
                                public void onSuccess(
                                        @Nullable SchoolBookChapterContentEntity
                                                content
                                ) {
                                    if (!closed && content != null) {
                                        approvedChapterReference =
                                                buildChapterReference(content);
                                        approvedLessonContent =
                                                SchoolBookChapterContentLessonMapper
                                                        .toLessonContent(
                                                                chapterTitle,
                                                                content
                                                        );
                                    }
                                }

                                @Override
                                public void onError(
                                        @NonNull Exception exception
                                ) {
                                    // No approved source means normal verified routing.
                                }
                            }
                    );
        }

        private void toggleCard() {
            boolean opening = card.getVisibility() != View.VISIBLE;
            if (!opening) {
                collapseCard();
                return;
            }
            dismissArea.setVisibility(View.VISIBLE);
            card.setAlpha(0f);
            card.setScaleX(0.94f);
            card.setScaleY(0.94f);
            card.setVisibility(View.VISIBLE);
            card.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180L)
                    .start();
            scrollToBottom();
        }

        private void collapseCard() {
            card.animate().cancel();
            card.setVisibility(View.GONE);
            dismissArea.setVisibility(View.GONE);
        }

        private void submitFollowUp(@NonNull String prompt) {
            if (turns.isEmpty()) {
                Snackbar.make(
                        companion,
                        R.string.smart_companion_no_previous_answer,
                        Snackbar.LENGTH_SHORT
                ).show();
                return;
            }
            submitQuestion(prompt, null);
        }

        private void submitQuestion(
                @NonNull String question,
                @Nullable Bitmap questionImage
        ) {
            if (requestInProgress) {
                recycle(questionImage);
                return;
            }
            if (question.isEmpty()) {
                recycle(questionImage);
                Snackbar.make(
                        companion,
                        R.string.smart_companion_empty_question,
                        Snackbar.LENGTH_SHORT
                ).show();
                return;
            }
            StudentProfileEntity activeProfile = profile;
            if (activeProfile == null) {
                recycle(questionImage);
                showTemporaryStatus(
                        R.string.smart_companion_profile_loading
                );
                return;
            }

            requestInProgress = true;
            questionInput.setText("");
            showStatus(R.string.smart_companion_thinking);

            String effectiveSubject = subjectName.isEmpty()
                    ? "General Studies"
                    : subjectName;
            FirebaseStudyTutorClient.TutorRequest request =
                    new FirebaseStudyTutorClient.TutorRequest(
                            activeProfile.getStudentName(),
                            activeProfile.getEducationBoard(),
                            activeProfile.getStudentClass(),
                            activeProfile.getExplanationLanguage(),
                            effectiveSubject,
                            chapterTitle,
                            question,
                            conversationStore.buildConversationContext(turns),
                            approvedChapterReference
                    );

            tutorClient.askQuestionWithResult(
                    request,
                    questionImage,
                    new FirebaseStudyTutorClient.TutorResultCallback() {
                        @Override
                        public void onSuccess(
                                @NonNull SmartTutorAnswerResult result
                        ) {
                            recycle(questionImage);
                            if (closed) {
                                return;
                            }
                            requestInProgress = false;
                            hideStatus();
                            addSuccessfulTurn(
                                    question,
                                    result,
                                    effectiveSubject
                            );
                        }

                        @Override
                        public void onError(@NonNull Throwable throwable) {
                            recycle(questionImage);
                            if (closed) {
                                return;
                            }
                            requestInProgress = false;

                            LessonContent localLesson =
                                    approvedLessonContent;
                            if (questionImage == null
                                    && localLesson != null) {
                                String localAnswer =
                                        DoubtAssistantEngine.createAnswer(
                                                question,
                                                effectiveSubject,
                                                chapterTitle,
                                                localLesson,
                                                activeProfile
                                                        .getExplanationLanguage()
                                        );
                                SmartTutorAnswerResult localResult =
                                        SmartTutorAnswerResult
                                                .fromVerifiedOfflineKnowledge(
                                                        localAnswer,
                                                        "Parent-approved chapter",
                                                        "chapter-local"
                                                );
                                hideStatus();
                                addSuccessfulTurn(
                                        question,
                                        localResult,
                                        effectiveSubject
                                );
                                return;
                            }

                            showTemporaryStatus(
                                    R.string.smart_companion_answer_failed
                            );
                        }
                    }
            );
        }

        private void addSuccessfulTurn(
                @NonNull String question,
                @NonNull SmartTutorAnswerResult result,
                @NonNull String effectiveSubject
        ) {
            String answer = formatForClassSix(
                    result.getRawAnswerText()
            );
            String source = result.getAnswerSource().getDisplayLabel();
            SmartCompanionConversationStore.Turn turn =
                    new SmartCompanionConversationStore.Turn(
                            question,
                            answer,
                            source,
                            result.isVerified(),
                            System.currentTimeMillis()
                    );
            turns.add(turn);
            conversationStore.append(profile.getProfileId(), turn);
            renderTurn(turn);
            scrollToBottom();
            saveParentInsight(
                    question,
                    answer,
                    effectiveSubject
            );
        }

        private void renderAllTurns() {
            while (conversationContainer.getChildCount() > 1) {
                conversationContainer.removeViewAt(1);
            }
            welcomeText.setVisibility(
                    turns.isEmpty() ? View.VISIBLE : View.GONE
            );
            for (SmartCompanionConversationStore.Turn turn : turns) {
                renderTurn(turn);
            }
            scrollToBottom();
        }

        private void renderTurn(
                @NonNull SmartCompanionConversationStore.Turn turn
        ) {
            welcomeText.setVisibility(View.GONE);
            View turnView = LayoutInflater.from(activity).inflate(
                    R.layout.item_study_conversation_turn,
                    conversationContainer,
                    false
            );
            TextView questionView = turnView.findViewById(
                    R.id.textConversationQuestion
            );
            TextView answerView = turnView.findViewById(
                    R.id.textConversationAnswer
            );
            questionView.setText(turn.getQuestion());

            String verification = turn.isVerified()
                    ? activity.getString(R.string.smart_companion_verified)
                    : activity.getString(R.string.smart_companion_ai_checked);
            String sourceLine = verification;
            if (!turn.getSource().isEmpty()) {
                sourceLine += " • " + turn.getSource();
            }
            answerView.setText(
                    sourceLine + "\n\n" + turn.getAnswer()
            );
            conversationContainer.addView(turnView);
        }

        private void saveParentInsight(
                @NonNull String question,
                @NonNull String answer,
                @NonNull String effectiveSubject
        ) {
            StudentProfileEntity activeProfile = profile;
            if (activeProfile == null) {
                return;
            }
            DoubtHistoryEntity history = new DoubtHistoryEntity();
            history.setProfileId(activeProfile.getProfileId());
            history.setEducationBoard(activeProfile.getEducationBoard());
            history.setStudentClass(activeProfile.getStudentClass());
            history.setSubjectName(effectiveSubject);
            history.setChapterTitle(chapterTitle);
            history.setQuestionText(question);
            history.setAnswerText(answer);
            history.setExplanationLanguage(
                    activeProfile.getExplanationLanguage()
            );
            history.setCreatedAt(System.currentTimeMillis());
            doubtHistoryRepository.saveHistory(
                    history,
                    new DoubtHistoryRepository.SaveHistoryCallback() {
                        @Override
                        public void onSuccess(long historyId) {
                            // Parent insight saved.
                        }

                        @Override
                        public void onError(@NonNull Exception exception) {
                            // The answer remains usable even if analytics fail.
                        }
                    }
            );
        }

        private void clearConversation() {
            StudentProfileEntity activeProfile = profile;
            if (activeProfile == null) {
                return;
            }
            turns.clear();
            conversationStore.clear(activeProfile.getProfileId());
            FirebaseStudyTutorClient.clearSharedConversation();
            renderAllTurns();
        }

        private void launchVoice() {
            SmartAiCompanionInputFragment fragment =
                    SmartAiCompanionInputFragment.find(activity);
            if (fragment != null) {
                fragment.launchVoice();
            }
        }

        private void choosePhotoSource() {
            SmartAiCompanionInputFragment fragment =
                    SmartAiCompanionInputFragment.find(activity);
            if (fragment == null) {
                return;
            }
            new AlertDialog.Builder(activity)
                    .setTitle("सवाल की फोटो")
                    .setItems(
                            new String[]{
                                    "Camera से फोटो लें",
                                    "Gallery से चुनें"
                            },
                            (DialogInterface dialog, int which) -> {
                                if (which == 0) {
                                    fragment.launchCamera();
                                } else {
                                    fragment.launchGallery();
                                }
                            }
                    )
                    .show();
        }

        private void submitPhoto(@NonNull Uri imageUri) {
            showStatus(R.string.smart_companion_thinking);
            imageLoader.loadForAi(
                    imageUri,
                    new QuestionImageBitmapLoader.ImageLoadCallback() {
                        @Override
                        public void onSuccess(@NonNull Bitmap bitmap) {
                            if (closed) {
                                recycle(bitmap);
                                return;
                            }
                            hideStatus();
                            submitQuestion(
                                    activity.getString(
                                            R.string
                                                    .smart_companion_photo_question
                                    ),
                                    bitmap
                            );
                        }

                        @Override
                        public void onError(@NonNull Throwable throwable) {
                            if (!closed) {
                                showTemporaryStatus(
                                        R.string
                                                .smart_companion_answer_failed
                                );
                            }
                        }
                    }
            );
        }

        private void startPulse() {
            pulseAnimator = ObjectAnimator.ofFloat(
                    aiButton,
                    View.SCALE_X,
                    1f,
                    1.06f,
                    1f
            );
            pulseAnimator.setDuration(2200L);
            pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnimator.start();
        }

        private void showStatus(int stringResource) {
            statusText.setText(stringResource);
            statusText.setVisibility(View.VISIBLE);
        }

        private void hideStatus() {
            statusText.setVisibility(View.GONE);
        }

        private void showTemporaryStatus(int stringResource) {
            showStatus(stringResource);
            statusText.removeCallbacks(this::hideStatus);
            statusText.postDelayed(this::hideStatus, 3200L);
        }

        private void scrollToBottom() {
            conversationScroll.post(() ->
                    conversationScroll.fullScroll(View.FOCUS_DOWN)
            );
        }

        private void close() {
            closed = true;
            imageLoader.close();
            if (pulseAnimator != null) {
                pulseAnimator.cancel();
            }
        }
    }

    @NonNull
    private static String formatForClassSix(@NonNull String rawAnswer) {
        String answer = safe(rawAnswer);
        if (answer.isEmpty()) {
            return answer;
        }
        String lower = answer.toLowerCase();
        StringBuilder formatted = new StringBuilder();
        if (!lower.contains("सरल उत्तर")
                && !lower.contains("simple answer")) {
            formatted.append("सरल उत्तर\n");
        }
        formatted.append(answer);
        if (!lower.contains("उदाहरण")
                && !lower.contains("example")) {
            formatted.append(
                    "\n\nआसान उदाहरण\n"
                            + "इसी नियम या विचार को अपनी किताब के ऐसे ही एक सवाल पर लगाकर देखो।"
            );
        }
        if (!lower.contains("अभ्यास")
                && !lower.contains("practice")) {
            formatted.append(
                    "\n\nछोटा अभ्यास प्रश्न\n"
                            + "क्या तुम इस उत्तर को अपने शब्दों में एक वाक्य में बता सकती हो?"
            );
        }
        return formatted.toString();
    }

    @NonNull
    private static String buildChapterReference(
            @NonNull SchoolBookChapterContentEntity content
    ) {
        StringBuilder reference = new StringBuilder();
        appendReference(reference, content.getChapterIntroductionHindi());
        appendReference(reference, content.getChapterIntroductionEnglish());
        appendReference(reference, content.getDetailedExplanationHindi());
        appendReference(reference, content.getDetailedExplanationEnglish());
        appendReference(reference, content.getKeyPointsHindi());
        appendReference(reference, content.getKeyPointsEnglish());
        appendReference(reference, content.getWorkedExamplesHindi());
        appendReference(reference, content.getWorkedExamplesEnglish());
        appendReference(reference, content.getChapterSummaryHindi());
        appendReference(reference, content.getChapterSummaryEnglish());
        if (reference.length() <= 12000) {
            return reference.toString().trim();
        }
        return reference.substring(0, 12000).trim();
    }

    private static void appendReference(
            @NonNull StringBuilder target,
            @Nullable String value
    ) {
        String text = safe(value);
        if (!text.isEmpty() && target.length() < 12000) {
            target.append(text).append("\n\n");
        }
    }

    @NonNull
    private static String resolvePageContext(@NonNull Activity activity) {
        CharSequence title = activity.getTitle();
        if (!TextUtils.isEmpty(title)
                && !activity.getString(R.string.app_name)
                .contentEquals(title)) {
            return title.toString().trim();
        }
        return activity.getClass().getSimpleName()
                .replace("Activity", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();
    }

    @NonNull
    private static String firstNonEmptyExtra(
            @NonNull Activity activity,
            @NonNull String... keys
    ) {
        for (String key : keys) {
            String value = safe(
                    activity.getIntent().getStringExtra(key)
            );
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static long firstPositiveLongExtra(
            @NonNull Activity activity,
            @NonNull String... keys
    ) {
        for (String key : keys) {
            long value = activity.getIntent().getLongExtra(key, 0L);
            if (value > 0L) {
                return value;
            }
        }
        return 0L;
    }

    @NonNull
    private static String safe(@Nullable Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(
                value * activity.getResources()
                        .getDisplayMetrics().density
        );
    }

    private static void recycle(@Nullable Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private static boolean isStudentLearningScreen(
            @NonNull Activity activity
    ) {
        return !(activity instanceof MainActivity)
                && !(activity instanceof StudentProfileActivity)
                && !(activity instanceof StudentProfilesActivity)
                && !(activity instanceof AskStudySaathiActivity)
                && !(activity instanceof DoubtHistoryActivity)
                && !(activity instanceof ParentDashboardActivity)
                && !(activity instanceof SchoolCurriculumSetupActivity)
                && !(activity instanceof BookCoverScanActivity)
                && !(activity instanceof BookContentsScanActivity)
                && !(activity instanceof BookContentsReviewActivity)
                && !(activity instanceof BookLearningImportActivity)
                && !(activity instanceof BookChapterBoundaryReviewActivity)
                && !(activity instanceof BookMatchReviewActivity)
                && !(activity instanceof ManualSchoolBookActivity)
                && !(activity instanceof ManualSchoolBookChapterActivity)
                && !(activity instanceof SchoolBookChapterContentEditorActivity)
                && !(activity instanceof SchoolBookChapterPageReviewActivity)
                && !(activity instanceof BackupRestoreActivity)
                && !(activity instanceof BackupExportActivity)
                && !(activity instanceof CloudAccountActivity)
                && !(activity instanceof CloudBackupDiagnosticActivity);
    }
}
