package com.tridev.studysaathi.data.ai;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Study Saathi Hero Part का lifecycle-based Text-to-Speech observer।
 *
 * यह observer केवल AskStudySaathiActivity पर काम करता है।
 *
 * मुख्य कार्य:
 *
 * 1. Hero screen का answer TextView खोजना।
 * 2. Answer उपलब्ध होने पर "उत्तर सुनें" button जोड़ना।
 * 3. SmartTutorTextToSpeechManager से answer बोलना।
 * 4. बोलते समय button को Stop action में बदलना।
 * 5. Answer बदलने पर वर्तमान speech रोकना।
 * 6. Activity destroy होने पर TTS resources release करना।
 *
 * यह observer AskStudySaathiActivity.java अथवा उसके XML पर
 * compile-time dependency नहीं रखता। इसलिए बड़ी Hero Activity
 * को बदले बिना TTS control जोड़ा जा सकता है।
 */
public final class SmartTutorTextToSpeechActivityObserver
        implements Application.ActivityLifecycleCallbacks {

    private static final String TARGET_ACTIVITY_SIMPLE_NAME =
            "AskStudySaathiActivity";

    private static final String CONTROL_BUTTON_TAG =
            "study_saathi_smart_answer_tts_button";

    @NonNull
    private static final String BUTTON_TEXT_LISTEN =
            "🔊 उत्तर सुनें";

    @NonNull
    private static final String BUTTON_TEXT_STOP =
            "■ आवाज रोकें";

    /**
     * Hero layout में answer TextView के संभावित resource names।
     *
     * इनमें से कोई ID मिलने पर observer सीधे उसी TextView से जुड़ेगा।
     * ID न मिलने पर source-label वाले answer को recursively खोजा जाएगा।
     */
    @NonNull
    private static final String[] ANSWER_VIEW_ID_CANDIDATES = {
            "textSmartAnswer",
            "textAnswer",
            "textAnswerContent",
            "textAnswerBody",
            "textTutorAnswer",
            "textAiAnswer",
            "textDoubtAnswer",
            "textQuestionAnswer",
            "textStudySaathiAnswer",
            "textHeroAnswer"
    };

    /**
     * SmartTutorAnswerResult द्वारा answer के आरंभ में जोड़े गए
     * source labels।
     */
    @NonNull
    private static final String[] ANSWER_SOURCE_PREFIXES = {
            "✓ ऑफलाइन गणित",
            "✓ ऑफलाइन विभाज्यता",
            "✓ सत्यापित ऑफलाइन सामग्री",
            "↻ सेव किया गया उत्तर",
            "✦ Smart AI",
            "● ऑफलाइन सहायता",
            "● उत्तर"
    };

    /**
     * एक Application में observer दोबारा register होने से रोकता है।
     */
    @NonNull
    private static final Map<Application,
            SmartTutorTextToSpeechActivityObserver>
            REGISTERED_OBSERVERS =
            new WeakHashMap<>();

    @NonNull
    private final Map<Activity, ActivitySession>
            activitySessions =
            new WeakHashMap<>();

    private SmartTutorTextToSpeechActivityObserver() {
        /*
         * register() method से object बनाया जाएगा।
         */
    }

    /**
     * Application में observer safely register करता है।
     *
     * एक ही Application पर इसे कई बार call करने पर भी केवल
     * एक observer register रहेगा।
     */
    @NonNull
    public static SmartTutorTextToSpeechActivityObserver register(
            @NonNull Application application
    ) {
        synchronized (REGISTERED_OBSERVERS) {
            SmartTutorTextToSpeechActivityObserver existingObserver =
                    REGISTERED_OBSERVERS.get(
                            application
                    );

            if (existingObserver != null) {
                return existingObserver;
            }

            SmartTutorTextToSpeechActivityObserver newObserver =
                    new SmartTutorTextToSpeechActivityObserver();

            application.registerActivityLifecycleCallbacks(
                    newObserver
            );

            REGISTERED_OBSERVERS.put(
                    application,
                    newObserver
            );

            return newObserver;
        }
    }

    /**
     * Testing अथवा complete application shutdown के लिए observer
     * unregister करता है।
     */
    public static void unregister(
            @NonNull Application application
    ) {
        SmartTutorTextToSpeechActivityObserver observer;

        synchronized (REGISTERED_OBSERVERS) {
            observer =
                    REGISTERED_OBSERVERS.remove(
                            application
                    );
        }

        if (observer == null) {
            return;
        }

        application.unregisterActivityLifecycleCallbacks(
                observer
        );

        observer.releaseAllSessions();
    }

    @Override
    public void onActivityCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState
    ) {
        if (!isTargetActivity(
                activity
        )) {
            return;
        }

        ActivitySession session =
                new ActivitySession(
                        activity
                );

        activitySessions.put(
                activity,
                session
        );

        session.attach();
    }

    @Override
    public void onActivityStarted(
            @NonNull Activity activity
    ) {
        /*
         * No action required.
         */
    }

    @Override
    public void onActivityResumed(
            @NonNull Activity activity
    ) {
        if (!isTargetActivity(
                activity
        )) {
            return;
        }

        ActivitySession session =
                activitySessions.get(
                        activity
                );

        /*
         * Observer activity create होने के बाद register हुआ हो तो
         * resumed state में session तैयार किया जाएगा।
         */
        if (session == null) {
            session =
                    new ActivitySession(
                            activity
                    );

            activitySessions.put(
                    activity,
                    session
            );

            session.attach();
        }

        session.onActivityResumed();
    }

    @Override
    public void onActivityPaused(
            @NonNull Activity activity
    ) {
        ActivitySession session =
                activitySessions.get(
                        activity
                );

        if (session != null) {
            session.onActivityPaused();
        }
    }

    @Override
    public void onActivityStopped(
            @NonNull Activity activity
    ) {
        /*
         * Speech onActivityPaused में ही रोक दी जाती है।
         */
    }

    @Override
    public void onActivitySaveInstanceState(
            @NonNull Activity activity,
            @NonNull Bundle outState
    ) {
        /*
         * TTS state save करने की आवश्यकता नहीं है।
         */
    }

    @Override
    public void onActivityDestroyed(
            @NonNull Activity activity
    ) {
        ActivitySession session =
                activitySessions.remove(
                        activity
                );

        if (session != null) {
            session.release();
        }
    }

    private boolean isTargetActivity(
            @NonNull Activity activity
    ) {
        return TARGET_ACTIVITY_SIMPLE_NAME.equals(
                activity.getClass()
                        .getSimpleName()
        );
    }

    private void releaseAllSessions() {
        ActivitySession[] sessions =
                activitySessions.values()
                        .toArray(
                                new ActivitySession[0]
                        );

        activitySessions.clear();

        for (ActivitySession session : sessions) {
            if (session != null) {
                session.release();
            }
        }
    }

    /**
     * एक AskStudySaathiActivity का TTS lifecycle session।
     */
    private static final class ActivitySession {

        @NonNull
        private final Activity activity;

        @NonNull
        private final SmartTutorTextToSpeechManager
                textToSpeechManager;

        @Nullable
        private ViewGroup rootView;

        @Nullable
        private TextView answerTextView;

        @Nullable
        private TextWatcher answerTextWatcher;

        @Nullable
        private MaterialButton speechControlButton;

        @Nullable
        private ViewTreeObserver.OnGlobalLayoutListener
                globalLayoutListener;

        private boolean released;

        private boolean bindingInProgress;

        private ActivitySession(
                @NonNull Activity activity
        ) {
            this.activity =
                    activity;

            textToSpeechManager =
                    new SmartTutorTextToSpeechManager(
                            activity
                    );
        }

        private void attach() {
            View contentView =
                    activity.findViewById(
                            android.R.id.content
                    );

            if (!(contentView
                    instanceof ViewGroup)) {

                return;
            }

            rootView =
                    (ViewGroup) contentView;

            globalLayoutListener =
                    this::discoverAndBindAnswerView;

            ViewTreeObserver viewTreeObserver =
                    rootView.getViewTreeObserver();

            if (viewTreeObserver.isAlive()) {
                viewTreeObserver
                        .addOnGlobalLayoutListener(
                                globalLayoutListener
                        );
            }

            rootView.post(
                    this::discoverAndBindAnswerView
            );
        }

        private void onActivityResumed() {
            if (released) {
                return;
            }

            discoverAndBindAnswerView();
            refreshControlButton();
        }

        private void onActivityPaused() {
            if (released) {
                return;
            }

            textToSpeechManager.stop();

            updateButtonForIdleState();
        }

        /**
         * Answer TextView खोजकर TTS control उससे जोड़ता है।
         */
        private void discoverAndBindAnswerView() {
            if (released
                    || bindingInProgress
                    || activity.isFinishing()
                    || activity.isDestroyed()) {

                return;
            }

            bindingInProgress =
                    true;

            try {
                TextView discoveredAnswerView =
                        findAnswerTextView();

                if (discoveredAnswerView == null) {
                    return;
                }

                if (answerTextView
                        == discoveredAnswerView) {

                    refreshControlButton();

                    return;
                }

                detachAnswerTextWatcher();
                removeSpeechControlButton();

                answerTextView =
                        discoveredAnswerView;

                attachAnswerTextWatcher(
                        discoveredAnswerView
                );

                createSpeechControlButton(
                        discoveredAnswerView
                );

                refreshControlButton();

                /*
                 * Answer view मिल चुकी है। अब लगातार global layout
                 * listening की आवश्यकता नहीं है।
                 */
                removeGlobalLayoutListener();

            } finally {
                bindingInProgress =
                        false;
            }
        }

        /**
         * पहले resource ID candidates से और फिर source-label text
         * के आधार पर answer TextView खोजता है।
         */
        @Nullable
        private TextView findAnswerTextView() {
            for (String viewIdName :
                    ANSWER_VIEW_ID_CANDIDATES) {

                int resourceId =
                        activity.getResources()
                                .getIdentifier(
                                        viewIdName,
                                        "id",
                                        activity.getPackageName()
                                );

                if (resourceId == 0) {
                    continue;
                }

                View candidateView =
                        activity.findViewById(
                                resourceId
                        );

                if (isUsableAnswerTextView(
                        candidateView
                )) {
                    return (TextView) candidateView;
                }
            }

            ViewGroup availableRootView =
                    rootView;

            if (availableRootView == null) {
                return null;
            }

            return findAnswerTextViewRecursively(
                    availableRootView
            );
        }

        @Nullable
        private TextView findAnswerTextViewRecursively(
                @NonNull ViewGroup parentView
        ) {
            for (int index = 0;
                 index < parentView.getChildCount();
                 index++) {

                View childView =
                        parentView.getChildAt(
                                index
                        );

                if (isUsableAnswerTextView(
                        childView
                )
                        && startsWithAnswerSourceLabel(
                        ((TextView) childView)
                                .getText()
                )) {

                    return (TextView) childView;
                }

                if (childView
                        instanceof ViewGroup) {

                    TextView nestedResult =
                            findAnswerTextViewRecursively(
                                    (ViewGroup) childView
                            );

                    if (nestedResult != null) {
                        return nestedResult;
                    }
                }
            }

            return null;
        }

        private boolean isUsableAnswerTextView(
                @Nullable View view
        ) {
            return view instanceof TextView
                    && !(view instanceof EditText);
        }

        private boolean startsWithAnswerSourceLabel(
                @Nullable CharSequence text
        ) {
            String safeText =
                    text == null
                            ? ""
                            : text.toString()
                            .trim();

            if (safeText.isEmpty()) {
                return false;
            }

            for (String sourcePrefix :
                    ANSWER_SOURCE_PREFIXES) {

                if (safeText.startsWith(
                        sourcePrefix
                )) {
                    return true;
                }
            }

            return false;
        }

        private void attachAnswerTextWatcher(
                @NonNull TextView targetAnswerView
        ) {
            answerTextWatcher =
                    new TextWatcher() {

                        @Override
                        public void beforeTextChanged(
                                CharSequence text,
                                int start,
                                int count,
                                int after
                        ) {
                            /*
                             * No action required.
                             */
                        }

                        @Override
                        public void onTextChanged(
                                CharSequence text,
                                int start,
                                int before,
                                int count
                        ) {
                            /*
                             * Answer बदलते ही पुरानी speech रोकें।
                             */
                            if (textToSpeechManager
                                    .isSpeaking()) {

                                textToSpeechManager.stop();
                            }

                            refreshControlButton();
                        }

                        @Override
                        public void afterTextChanged(
                                Editable editable
                        ) {
                            /*
                             * No additional action required.
                             */
                        }
                    };

            targetAnswerView.addTextChangedListener(
                    answerTextWatcher
            );
        }

        /**
         * Answer के nearest vertical LinearLayout में button जोड़ता है।
         */
        private void createSpeechControlButton(
                @NonNull TextView targetAnswerView
        ) {
            LinearLayout targetContainer =
                    findNearestVerticalLinearLayout(
                            targetAnswerView
                    );

            if (targetContainer == null) {
                return;
            }

            MaterialButton existingButton =
                    findExistingControlButton(
                            targetContainer
                    );

            if (existingButton != null) {
                speechControlButton =
                        existingButton;

                configureSpeechControlButton(
                        existingButton
                );

                return;
            }

            MaterialButton newButton =
                    new MaterialButton(
                            activity
                    );

            newButton.setTag(
                    CONTROL_BUTTON_TAG
            );

            /*
             * MaterialButton के लिए सही method setAllCaps() है।
             */
            newButton.setAllCaps(
                    false
            );

            newButton.setText(
                    BUTTON_TEXT_LISTEN
            );

            newButton.setContentDescription(
                    "Study Saathi का उत्तर आवाज में सुनें"
            );

            newButton.setMinHeight(
                    dpToPixels(
                            48
                    )
            );

            newButton.setMinimumHeight(
                    dpToPixels(
                            48
                    )
            );

            newButton.setCornerRadius(
                    dpToPixels(
                            14
                    )
            );

            newButton.setInsetTop(
                    0
            );

            newButton.setInsetBottom(
                    0
            );

            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            layoutParams.topMargin =
                    dpToPixels(
                            12
                    );

            layoutParams.gravity =
                    Gravity.START;

            newButton.setLayoutParams(
                    layoutParams
            );

            configureSpeechControlButton(
                    newButton
            );

            View directAnswerChild =
                    findDirectChildInsideContainer(
                            targetAnswerView,
                            targetContainer
                    );

            int answerChildIndex =
                    directAnswerChild == null
                            ? -1
                            : targetContainer.indexOfChild(
                            directAnswerChild
                    );

            if (answerChildIndex >= 0
                    && answerChildIndex
                    < targetContainer.getChildCount() - 1) {

                targetContainer.addView(
                        newButton,
                        answerChildIndex + 1
                );

            } else {
                targetContainer.addView(
                        newButton
                );
            }

            speechControlButton =
                    newButton;
        }

        private void configureSpeechControlButton(
                @NonNull MaterialButton button
        ) {
            button.setOnClickListener(
                    view -> handleSpeechControlClick()
            );
        }

        private void handleSpeechControlClick() {
            if (released) {
                return;
            }

            if (textToSpeechManager.isSpeaking()) {
                textToSpeechManager.stop();

                updateButtonForIdleState();

                return;
            }

            TextView availableAnswerTextView =
                    answerTextView;

            if (availableAnswerTextView == null) {
                showMessage(
                        "सुनाने के लिए उत्तर उपलब्ध नहीं है।"
                );

                return;
            }

            String displayAnswer =
                    availableAnswerTextView
                            .getText()
                            .toString();

            String speechText =
                    SmartTutorTextToSpeechManager
                            .cleanAnswerForSpeech(
                                    displayAnswer
                            );

            if (speechText.isEmpty()) {
                showMessage(
                        "सुनाने के लिए उत्तर उपलब्ध नहीं है।"
                );

                refreshControlButton();

                return;
            }

            setButtonTemporarilyDisabled();

            textToSpeechManager.speakAnswer(
                    speechText,
                    "",
                    new SmartTutorTextToSpeechManager
                            .SpeechCallback() {

                        @Override
                        public void onSpeechStarted() {
                            MaterialButton button =
                                    speechControlButton;

                            if (button == null) {
                                return;
                            }

                            button.setEnabled(
                                    true
                            );

                            button.setText(
                                    BUTTON_TEXT_STOP
                            );

                            button.setContentDescription(
                                    "उत्तर की आवाज रोकें"
                            );
                        }

                        @Override
                        public void onSpeechCompleted() {
                            updateButtonForIdleState();
                        }

                        @Override
                        public void onSpeechStopped() {
                            updateButtonForIdleState();
                        }

                        @Override
                        public void onSpeechError(
                                @NonNull String errorMessage
                        ) {
                            updateButtonForIdleState();

                            showMessage(
                                    errorMessage
                            );
                        }
                    }
            );
        }

        private void setButtonTemporarilyDisabled() {
            MaterialButton button =
                    speechControlButton;

            if (button == null) {
                return;
            }

            button.setEnabled(
                    false
            );

            button.setText(
                    "आवाज तैयार हो रही है…"
            );
        }

        private void updateButtonForIdleState() {
            MaterialButton button =
                    speechControlButton;

            if (button == null) {
                return;
            }

            button.setEnabled(
                    true
            );

            button.setText(
                    BUTTON_TEXT_LISTEN
            );

            button.setContentDescription(
                    "Study Saathi का उत्तर आवाज में सुनें"
            );

            refreshControlButton();
        }

        /**
         * Answer available होने पर button दिखाता है।
         */
        private void refreshControlButton() {
            MaterialButton button =
                    speechControlButton;

            TextView availableAnswerTextView =
                    answerTextView;

            if (button == null
                    || availableAnswerTextView == null) {

                return;
            }

            /*
             * TextView.getText() CharSequence लौटाता है।
             * cleanAnswerForSpeech() String स्वीकार करता है,
             * इसलिए यहाँ explicit toString() conversion जरूरी है।
             */
            String speechText =
                    SmartTutorTextToSpeechManager
                            .cleanAnswerForSpeech(
                                    availableAnswerTextView
                                            .getText()
                                            .toString()
                            );

            boolean answerAvailable =
                    !speechText.isEmpty()
                            && startsWithAnswerSourceLabel(
                            availableAnswerTextView
                                    .getText()
                    );

            button.setVisibility(
                    answerAvailable
                            ? View.VISIBLE
                            : View.GONE
            );

            button.setEnabled(
                    answerAvailable
            );
        }

        @Nullable
        private MaterialButton findExistingControlButton(
                @NonNull ViewGroup parentView
        ) {
            for (int index = 0;
                 index < parentView.getChildCount();
                 index++) {

                View childView =
                        parentView.getChildAt(
                                index
                        );

                if (childView
                        instanceof MaterialButton
                        && CONTROL_BUTTON_TAG.equals(
                        childView.getTag()
                )) {

                    return (MaterialButton) childView;
                }
            }

            return null;
        }

        @Nullable
        private LinearLayout findNearestVerticalLinearLayout(
                @NonNull View startingView
        ) {
            View currentView =
                    startingView;

            int inspectedParentCount =
                    0;

            while (currentView.getParent()
                    instanceof ViewGroup
                    && inspectedParentCount < 8) {

                ViewGroup parentView =
                        (ViewGroup) currentView.getParent();

                if (parentView
                        instanceof LinearLayout
                        && ((LinearLayout) parentView)
                        .getOrientation()
                        == LinearLayout.VERTICAL) {

                    return (LinearLayout) parentView;
                }

                currentView =
                        parentView;

                inspectedParentCount++;
            }

            return null;
        }

        @Nullable
        private View findDirectChildInsideContainer(
                @NonNull View startingView,
                @NonNull ViewGroup targetContainer
        ) {
            View currentView =
                    startingView;

            while (currentView.getParent()
                    instanceof ViewGroup) {

                ViewParent parent =
                        currentView.getParent();

                if (parent
                        == targetContainer) {

                    return currentView;
                }

                currentView =
                        (View) parent;
            }

            return null;
        }

        private void detachAnswerTextWatcher() {
            TextView currentAnswerView =
                    answerTextView;

            TextWatcher currentWatcher =
                    answerTextWatcher;

            if (currentAnswerView != null
                    && currentWatcher != null) {

                currentAnswerView
                        .removeTextChangedListener(
                                currentWatcher
                        );
            }

            answerTextWatcher =
                    null;

            answerTextView =
                    null;
        }

        private void removeSpeechControlButton() {
            MaterialButton button =
                    speechControlButton;

            speechControlButton =
                    null;

            if (button == null) {
                return;
            }

            button.setOnClickListener(
                    null
            );

            ViewParent parent =
                    button.getParent();

            if (parent
                    instanceof ViewGroup) {

                ((ViewGroup) parent)
                        .removeView(
                                button
                        );
            }
        }

        private void removeGlobalLayoutListener() {
            ViewGroup availableRootView =
                    rootView;

            ViewTreeObserver.OnGlobalLayoutListener listener =
                    globalLayoutListener;

            globalLayoutListener =
                    null;

            if (availableRootView == null
                    || listener == null) {

                return;
            }

            ViewTreeObserver viewTreeObserver =
                    availableRootView
                            .getViewTreeObserver();

            if (viewTreeObserver.isAlive()) {
                viewTreeObserver
                        .removeOnGlobalLayoutListener(
                                listener
                        );
            }
        }

        private void showMessage(
                @NonNull String message
        ) {
            if (released
                    || activity.isFinishing()
                    || activity.isDestroyed()) {

                return;
            }

            Toast.makeText(
                    activity,
                    message,
                    Toast.LENGTH_LONG
            ).show();
        }

        private int dpToPixels(
                int dpValue
        ) {
            float density =
                    activity.getResources()
                            .getDisplayMetrics()
                            .density;

            return Math.round(
                    dpValue * density
            );
        }

        private void release() {
            if (released) {
                return;
            }

            released =
                    true;

            removeGlobalLayoutListener();
            detachAnswerTextWatcher();
            removeSpeechControlButton();

            textToSpeechManager.shutdown();

            rootView =
                    null;
        }
    }
}