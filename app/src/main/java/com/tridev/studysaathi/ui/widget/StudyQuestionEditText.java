package com.tridev.studysaathi.ui.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tridev.studysaathi.R;

/**
 * Study Saathi Hero Question Box।
 *
 * यह custom EditText:
 *
 * 1. Question text के अनुसार अपने-आप बड़ा होता है।
 * 2. सामान्य अवस्था में अधिकतम 8 lines दिखाता है।
 * 3. Expanded अवस्था में अधिकतम 16 lines दिखाता है।
 * 4. Question box के अंदर swipe करने पर उसी text को scroll करता है।
 * 5. Text के ऊपर या नीचे पहुँचने पर parent screen को scroll करने देता है।
 * 6. Clear और Expand/Collapse buttons को स्वयं manage करता है।
 * 7. Clear करने पर पुराना displayed answer भी हटाता है।
 * 8. Screen rotation के बाद expanded state सुरक्षित रखता है।
 */
public final class StudyQuestionEditText
        extends TextInputEditText {

    private static final int DEFAULT_MINIMUM_LINES =
            4;

    private static final int DEFAULT_MAXIMUM_LINES =
            8;

    private static final int EXPANDED_MAXIMUM_LINES =
            16;

    private static final float MINIMUM_SCROLL_GESTURE_DISTANCE =
            2f;

    private boolean expanded;

    private boolean parentInterceptBlocked;

    private float previousTouchY;

    @Nullable
    private MaterialButton clearQuestionButton;

    @Nullable
    private MaterialButton toggleQuestionSizeButton;

    public StudyQuestionEditText(
            @NonNull Context context
    ) {
        super(
                context
        );

        initializeQuestionBox();
    }

    public StudyQuestionEditText(
            @NonNull Context context,
            @Nullable AttributeSet attributes
    ) {
        super(
                context,
                attributes
        );

        initializeQuestionBox();
    }

    public StudyQuestionEditText(
            @NonNull Context context,
            @Nullable AttributeSet attributes,
            int defaultStyleAttribute
    ) {
        super(
                context,
                attributes,
                defaultStyleAttribute
        );

        initializeQuestionBox();
    }

    /**
     * Question box की default behaviour तैयार करता है।
     */
    private void initializeQuestionBox() {
        setSingleLine(
                false
        );

        setHorizontallyScrolling(
                false
        );

        setGravity(
                Gravity.TOP
                        | Gravity.START
        );

        setMinLines(
                DEFAULT_MINIMUM_LINES
        );

        setMaxLines(
                DEFAULT_MAXIMUM_LINES
        );

        setVerticalScrollBarEnabled(
                true
        );

        setHorizontalScrollBarEnabled(
                false
        );

        setScrollbarFadingEnabled(
                false
        );

        setScrollBarStyle(
                SCROLLBARS_INSIDE_INSET
        );

        setOverScrollMode(
                OVER_SCROLL_IF_CONTENT_SCROLLS
        );

        setNestedScrollingEnabled(
                true
        );

        setMovementMethod(
                ScrollingMovementMethod.getInstance()
        );

        /*
         * Cursor जिस position पर है, focus मिलने पर उसे visible रखें।
         */
        setOnFocusChangeListener(
                (view, hasFocus) -> {
                    if (hasFocus) {
                        post(
                                this::keepCursorVisible
                        );
                    }
                }
        );

        addTextChangedListener(
                createQuestionTextWatcher()
        );
    }

    @NonNull
    private TextWatcher createQuestionTextWatcher() {
        return new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence sequence,
                    int start,
                    int count,
                    int after
            ) {
                // No action required.
            }

            @Override
            public void onTextChanged(
                    CharSequence sequence,
                    int start,
                    int before,
                    int count
            ) {
                post(
                        StudyQuestionEditText.this
                                ::updateClearButtonState
                );
            }

            @Override
            public void afterTextChanged(
                    Editable editable
            ) {
                /*
                 * Text बढ़ने या घटने के बाद cursor वाली line
                 * को visible रखने की कोशिश करें।
                 */
                if (hasFocus()) {
                    post(
                            StudyQuestionEditText.this
                                    ::keepCursorVisible
                    );
                }
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        /*
         * XML के सभी views attach हो जाने के बाद action buttons खोजें।
         */
        post(
                this::connectQuestionActionButtons
        );
    }

    /**
     * XML में मौजूद Clear और Expand buttons को इस Question Box से जोड़ता है।
     */
    private void connectQuestionActionButtons() {
        View rootView =
                getRootView();

        clearQuestionButton =
                rootView.findViewById(
                        R.id.buttonClearQuestion
                );

        toggleQuestionSizeButton =
                rootView.findViewById(
                        R.id.buttonToggleQuestionSize
                );

        MaterialButton availableClearButton =
                clearQuestionButton;

        if (availableClearButton != null) {
            availableClearButton.setOnClickListener(
                    view -> handleClearQuestionAction()
            );
        }

        MaterialButton availableToggleButton =
                toggleQuestionSizeButton;

        if (availableToggleButton != null) {
            availableToggleButton.setOnClickListener(
                    view -> handleToggleQuestionSizeAction()
            );
        }

        updateClearButtonState();
        updateToggleButtonText();
    }

    /**
     * Typed, Voice और OCR question text साफ करता है।
     *
     * Subject, Chapter और selected photo नहीं हटाए जाते।
     */
    private void handleClearQuestionAction() {
        if (!isQuestionActionAllowed()) {
            Snackbar.make(
                    this,
                    "Smart answer तैयार होते समय सवाल साफ नहीं किया जा सकता।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        boolean hadQuestionText =
                getText() != null
                        && getText().length() > 0;

        boolean hadDisplayedAnswer =
                isAnswerCurrentlyVisible();

        if (!hadQuestionText
                && !hadDisplayedAnswer) {

            Snackbar.make(
                    this,
                    "साफ करने के लिए कोई सवाल उपलब्ध नहीं है।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        clearQuestionText();
        clearQuestionInputMessage();
        clearDisplayedAnswer();

        Snackbar.make(
                this,
                "सवाल और पिछला answer साफ कर दिया गया है।",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    /**
     * AI request के दौरान Ask button disabled रहता है।
     * उसी संकेत से Clear और Toggle actions को सुरक्षित रखा जाता है।
     */
    private boolean isQuestionActionAllowed() {
        View askButton =
                getRootView()
                        .findViewById(
                                R.id.buttonAskSaathi
                        );

        return isEnabled()
                && (
                askButton == null
                        || askButton.isEnabled()
        );
    }

    /**
     * Question Box को बड़ा या छोटा करता है।
     */
    private void handleToggleQuestionSizeAction() {
        if (!isQuestionActionAllowed()) {
            Snackbar.make(
                    this,
                    "Smart answer तैयार होते समय box का आकार नहीं बदला जा सकता।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        boolean nowExpanded =
                toggleExpanded();

        Snackbar.make(
                this,
                nowExpanded
                        ? "Question box बड़ा कर दिया गया है।"
                        : "Question box सामान्य आकार में कर दिया गया है।",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    /**
     * Box को सामान्य और expanded आकार के बीच बदलता है।
     *
     * @return true जब box expanded हो।
     */
    public boolean toggleExpanded() {
        setExpanded(
                !expanded
        );

        return expanded;
    }

    /**
     * Question box का expanded state बदलता है।
     */
    public void setExpanded(
            boolean shouldExpand
    ) {
        expanded =
                shouldExpand;

        setMaxLines(
                expanded
                        ? EXPANDED_MAXIMUM_LINES
                        : DEFAULT_MAXIMUM_LINES
        );

        requestLayout();

        updateToggleButtonText();

        post(
                this::keepCursorVisible
        );
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void updateToggleButtonText() {
        MaterialButton availableToggleButton =
                toggleQuestionSizeButton;

        if (availableToggleButton == null) {
            return;
        }

        availableToggleButton.setText(
                expanded
                        ? "छोटा करें"
                        : "बड़ा करें"
        );

        availableToggleButton.setContentDescription(
                expanded
                        ? "Question box छोटा करें"
                        : "Question box बड़ा करें"
        );
    }

    private void updateClearButtonState() {
        MaterialButton availableClearButton =
                clearQuestionButton;

        if (availableClearButton == null) {
            return;
        }

        boolean hasQuestionText =
                getText() != null
                        && getText().length() > 0;

        boolean hasDisplayedAnswer =
                isAnswerCurrentlyVisible();

        availableClearButton.setAlpha(
                hasQuestionText
                        || hasDisplayedAnswer
                        ? 1.0f
                        : 0.60f
        );
    }

    /**
     * Question text को साफ करता है और internal scroll को शुरुआत पर लाता है।
     */
    public void clearQuestionText() {
        setText(
                ""
        );

        scrollTo(
                0,
                0
        );

        setSelection(
                0
        );

        requestFocus();

        post(
                this::keepCursorVisible
        );
    }

    private void clearQuestionInputMessage() {
        TextInputLayout questionInputLayout =
                getRootView()
                        .findViewById(
                                R.id.inputQuestion
                        );

        if (questionInputLayout == null) {
            return;
        }

        questionInputLayout.setError(
                null
        );

        questionInputLayout.setHelperText(
                null
        );
    }

    private boolean isAnswerCurrentlyVisible() {
        View answerCard =
                getRootView()
                        .findViewById(
                                R.id.cardAnswer
                        );

        return answerCard != null
                && answerCard.getVisibility()
                == View.VISIBLE;
    }

    /**
     * पुराने answer card का content और visibility साफ करता है।
     */
    private void clearDisplayedAnswer() {
        View rootView =
                getRootView();

        View answerCard =
                rootView.findViewById(
                        R.id.cardAnswer
                );

        TextView displayedQuestion =
                rootView.findViewById(
                        R.id.textUserQuestion
                );

        TextView displayedAnswer =
                rootView.findViewById(
                        R.id.textSaathiAnswer
                );

        View openLessonButton =
                rootView.findViewById(
                        R.id.buttonOpenLesson
                );

        if (displayedQuestion != null) {
            displayedQuestion.setText(
                    ""
            );
        }

        if (displayedAnswer != null) {
            displayedAnswer.setText(
                    ""
            );
        }

        if (openLessonButton != null) {
            openLessonButton.setVisibility(
                    View.GONE
            );
        }

        if (answerCard != null) {
            answerCard.setVisibility(
                    View.GONE
            );
        }

        updateClearButtonState();
    }

    /**
     * Cursor वाली position को Question Box में visible रखता है।
     */
    private void keepCursorVisible() {
        Editable editableText =
                getText();

        if (editableText == null) {
            return;
        }

        int safeSelection =
                Math.max(
                        0,
                        Math.min(
                                getSelectionStart(),
                                editableText.length()
                        )
                );

        if (getSelectionStart()
                != safeSelection) {

            setSelection(
                    safeSelection
            );
        }

        bringPointIntoView(
                safeSelection
        );
    }

    /**
     * Question box के अंदर touch होने पर:
     *
     * - Text scroll हो सकता है तो gesture Question Box को मिलेगा।
     * - Text ऊपर/नीचे के अंतिम point पर है तो parent screen scroll करेगी।
     */
    @Override
    public boolean onTouchEvent(
            @NonNull MotionEvent event
    ) {
        int action =
                event.getActionMasked();

        if (action
                == MotionEvent.ACTION_DOWN) {

            previousTouchY =
                    event.getY();

            blockParentTouchInterception();

        } else if (action
                == MotionEvent.ACTION_MOVE) {

            float currentTouchY =
                    event.getY();

            float movementDistance =
                    currentTouchY
                            - previousTouchY;

            if (Math.abs(
                    movementDistance
            )
                    >= MINIMUM_SCROLL_GESTURE_DISTANCE) {

                /*
                 * उँगली ऊपर जाने पर content नीचे की ओर scroll होगा।
                 * उँगली नीचे आने पर content ऊपर की ओर scroll होगा।
                 */
                int scrollDirection =
                        movementDistance < 0
                                ? 1
                                : -1;

                if (canScrollVertically(
                        scrollDirection
                )) {

                    blockParentTouchInterception();

                } else {
                    allowParentTouchInterception();
                }

                previousTouchY =
                        currentTouchY;
            }

        } else if (action
                == MotionEvent.ACTION_UP
                || action
                == MotionEvent.ACTION_CANCEL) {

            post(
                    this::allowParentTouchInterception
            );
        }

        boolean handled =
                super.onTouchEvent(
                        event
                );

        if (action
                == MotionEvent.ACTION_UP) {

            performClick();
        }

        return handled;
    }

    private void blockParentTouchInterception() {
        if (parentInterceptBlocked) {
            return;
        }

        ViewParent parent =
                getParent();

        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(
                    true
            );

            parent =
                    parent.getParent();
        }

        parentInterceptBlocked =
                true;
    }

    private void allowParentTouchInterception() {
        ViewParent parent =
                getParent();

        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(
                    false
            );

            parent =
                    parent.getParent();
        }

        parentInterceptBlocked =
                false;
    }

    @Override
    public boolean performClick() {
        super.performClick();

        return true;
    }

    /**
     * Expanded state को screen recreation के बाद सुरक्षित रखता है।
     *
     * TextView में यह method public है, इसलिए override भी public होना चाहिए।
     */
    @Nullable
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable parentState =
                super.onSaveInstanceState();

        SavedState savedState =
                new SavedState(
                        parentState
                );

        savedState.expanded =
                expanded;

        return savedState;
    }

    /**
     * Saved expanded/collapsed state दोबारा लागू करता है।
     *
     * TextView में यह method public है, इसलिए override भी public होना चाहिए।
     */
    @Override
    public void onRestoreInstanceState(
            @Nullable Parcelable state
    ) {
        if (!(state
                instanceof SavedState)) {

            super.onRestoreInstanceState(
                    state
            );

            return;
        }

        SavedState savedState =
                (SavedState) state;

        super.onRestoreInstanceState(
                savedState.getSuperState()
        );

        setExpanded(
                savedState.expanded
        );
    }

    @Override
    protected void onDetachedFromWindow() {
        allowParentTouchInterception();

        MaterialButton availableClearButton =
                clearQuestionButton;

        if (availableClearButton != null) {
            availableClearButton.setOnClickListener(
                    null
            );
        }

        MaterialButton availableToggleButton =
                toggleQuestionSizeButton;

        if (availableToggleButton != null) {
            availableToggleButton.setOnClickListener(
                    null
            );
        }

        clearQuestionButton =
                null;

        toggleQuestionSizeButton =
                null;

        super.onDetachedFromWindow();
    }

    private static final class SavedState
            extends BaseSavedState {

        private boolean expanded;

        private SavedState(
                @Nullable Parcelable parentState
        ) {
            super(
                    parentState
            );
        }

        private SavedState(
                @NonNull Parcel input
        ) {
            super(
                    input
            );

            expanded =
                    input.readInt()
                            == 1;
        }

        @Override
        public void writeToParcel(
                @NonNull Parcel destination,
                int flags
        ) {
            super.writeToParcel(
                    destination,
                    flags
            );

            destination.writeInt(
                    expanded
                            ? 1
                            : 0
            );
        }

        public static final Parcelable.Creator<SavedState>
                CREATOR =
                new Parcelable.Creator<SavedState>() {

                    @NonNull
                    @Override
                    public SavedState createFromParcel(
                            @NonNull Parcel input
                    ) {
                        return new SavedState(
                                input
                        );
                    }

                    @NonNull
                    @Override
                    public SavedState[] newArray(
                            int size
                    ) {
                        return new SavedState[
                                size
                                ];
                    }
                };
    }
}