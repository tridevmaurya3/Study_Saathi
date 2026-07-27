package com.tridev.studysaathi.ui.widget;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.R;
import com.tridev.studysaathi.data.ai.StudyAnswerSpeaker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Study Saathi AI Answer View।
 *
 * मुख्य सुविधाएँ:
 *
 * 1. Markdown headings को बड़े और bold headings में बदलना।
 * 2. **Bold Text** से stars हटाकर वास्तविक bold text दिखाना।
 * 3. Inline code को monospace text में दिखाना।
 * 4. सामान्य LaTeX Mathematics को readable symbols में बदलना।
 * 5. Smart AI answer को आवाज में पढ़ना।
 * 6. Answer को plain text में copy करना।
 * 7. Question और Answer को दूसरे apps में share करना।
 * 8. Loading और Clear state में answer actions छिपाना।
 */
public final class StudyMarkdownTextView
        extends AppCompatTextView {

    private static final String BOLD_MARKER_A =
            "**";

    private static final String BOLD_MARKER_B =
            "__";

    private static final String CODE_MARKER =
            "`";

    private static final String DISPLAY_DIVIDER =
            "────────────────────────";

    private static final String ANSWER_ACTION_BAR_TAG =
            "study_saathi_answer_action_bar";

    /**
     * #, ##, ### आदि headings।
     */
    private static final Pattern HEADING_PATTERN =
            Pattern.compile(
                    "^\\s*(#{1,6})\\s*(.+?)\\s*$"
            );

    /**
     * ---, ___ और *** divider।
     */
    private static final Pattern HORIZONTAL_RULE_PATTERN =
            Pattern.compile(
                    "^\\s*(?:-{3,}|_{3,}|\\*{3,})\\s*$"
            );

    /**
     * Markdown bullets।
     */
    private static final Pattern BULLET_PATTERN =
            Pattern.compile(
                    "^\\s*[-*•]\\s+(.+?)\\s*$"
            );

    /**
     * [Visible Text](URL)
     *
     * Corrected:
     * closing square bracket भी properly escaped है।
     */
    private static final Pattern MARKDOWN_LINK_PATTERN =
            Pattern.compile(
                    "\\[([^\\]]+)\\]\\(([^)]*)\\)"
            );

    /**
     * \frac{3}{4}
     *
     * Corrected:
     * opening और closing दोनों braces escaped हैं।
     *
     * Android ICU Regex engine पर सुरक्षित pattern।
     */
    private static final Pattern LATEX_FRACTION_PATTERN =
            Pattern.compile(
                    "\\\\frac\\s*\\{(.+?)\\}\\s*\\{(.+?)\\}"
            );

    /**
     * \text{example}
     *
     * Corrected:
     * closing brace escaped है।
     */
    private static final Pattern LATEX_TEXT_PATTERN =
            Pattern.compile(
                    "\\\\text\\s*\\{(.+?)\\}"
            );

    /**
     * \mathrm{...}
     * \mathbf{...}
     * \mathit{...}
     * \operatorname{...}
     *
     * Corrected:
     * closing brace escaped है।
     */
    private static final Pattern LATEX_STYLE_PATTERN =
            Pattern.compile(
                    "\\\\(?:mathrm|mathbf|mathit|operatorname)"
                            + "\\s*\\{(.+?)\\}"
            );

    /**
     * setText() के अंदर super.setText() चलने पर recursive
     * formatting रोकता है।
     */
    private boolean markdownFormattingInProgress;

    /**
     * नया answer, Clear या screen close होने पर पुराने speech
     * callbacks को UI बदलने से रोकता है।
     */
    private boolean suppressSpeechCallback;

    /**
     * Firebase AI से प्राप्त original raw answer।
     */
    @NonNull
    private String currentAnswerText =
            "";

    @Nullable
    private StudyAnswerSpeaker answerSpeaker;

    @Nullable
    private View speechControlsLayout;

    @Nullable
    private TextView speechStatusText;

    @Nullable
    private MaterialButton speakAnswerButton;

    @Nullable
    private MaterialButton stopAnswerButton;

    @Nullable
    private LinearLayout answerActionsLayout;

    @Nullable
    private MaterialButton copyAnswerButton;

    @Nullable
    private MaterialButton shareAnswerButton;

    public StudyMarkdownTextView(
            @NonNull Context context
    ) {
        super(
                context
        );
    }

    public StudyMarkdownTextView(
            @NonNull Context context,
            @Nullable AttributeSet attributes
    ) {
        super(
                context,
                attributes
        );
    }

    public StudyMarkdownTextView(
            @NonNull Context context,
            @Nullable AttributeSet attributes,
            int defaultStyleAttribute
    ) {
        super(
                context,
                attributes,
                defaultStyleAttribute
        );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (answerSpeaker == null) {
            answerSpeaker =
                    new StudyAnswerSpeaker(
                            getContext()
                    );
        }

        post(
                this::connectAnswerControls
        );
    }

    /**
     * Answer Card के Speech, Copy और Share controls तैयार करता है।
     */
    private void connectAnswerControls() {
        if (!isAttachedToWindow()) {
            return;
        }

        View rootView =
                getRootView();

        speechControlsLayout =
                rootView.findViewById(
                        R.id.layoutAnswerSpeechControls
                );

        speechStatusText =
                rootView.findViewById(
                        R.id.textAnswerSpeechStatus
                );

        speakAnswerButton =
                rootView.findViewById(
                        R.id.buttonSpeakAnswer
                );

        stopAnswerButton =
                rootView.findViewById(
                        R.id.buttonStopAnswerSpeech
                );

        MaterialButton availableSpeakButton =
                speakAnswerButton;

        if (availableSpeakButton != null) {
            availableSpeakButton.setOnClickListener(
                    view -> startAnswerSpeech()
            );
        }

        MaterialButton availableStopButton =
                stopAnswerButton;

        if (availableStopButton != null) {
            availableStopButton.setOnClickListener(
                    view -> stopAnswerSpeechByUser()
            );
        }

        ensureAnswerActionBar();

        MaterialButton availableCopyButton =
                copyAnswerButton;

        if (availableCopyButton != null) {
            availableCopyButton.setOnClickListener(
                    view -> copyCurrentAnswer()
            );
        }

        MaterialButton availableShareButton =
                shareAnswerButton;

        if (availableShareButton != null) {
            availableShareButton.setOnClickListener(
                    view -> shareCurrentAnswer()
            );
        }

        updateAnswerControlsForCurrentText();
    }

    /**
     * Answer Card में Copy और Share buttons जोड़ता है।
     */
    private void ensureAnswerActionBar() {
        if (answerActionsLayout != null) {
            return;
        }

        if (!(getParent()
                instanceof LinearLayout)) {

            return;
        }

        LinearLayout answerContainer =
                (LinearLayout) getParent();

        View existingActionBar =
                answerContainer.findViewWithTag(
                        ANSWER_ACTION_BAR_TAG
                );

        if (existingActionBar
                instanceof LinearLayout) {

            answerActionsLayout =
                    (LinearLayout) existingActionBar;

            if (answerActionsLayout.getChildCount()
                    >= 2) {

                View firstChild =
                        answerActionsLayout.getChildAt(
                                0
                        );

                View secondChild =
                        answerActionsLayout.getChildAt(
                                1
                        );

                if (firstChild
                        instanceof MaterialButton) {

                    copyAnswerButton =
                            (MaterialButton) firstChild;
                }

                if (secondChild
                        instanceof MaterialButton) {

                    shareAnswerButton =
                            (MaterialButton) secondChild;
                }
            }

            return;
        }

        LinearLayout actionsLayout =
                new LinearLayout(
                        getContext()
                );

        actionsLayout.setTag(
                ANSWER_ACTION_BAR_TAG
        );

        actionsLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        actionsLayout.setVisibility(
                View.GONE
        );

        LinearLayout.LayoutParams actionsLayoutParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        actionsLayoutParams.topMargin =
                dpToPx(
                        13
                );

        actionsLayout.setLayoutParams(
                actionsLayoutParams
        );

        MaterialButton copyButton =
                createAnswerActionButton(
                        "📋 कॉपी करें",
                        R.color.ss_green_soft,
                        R.color.ss_green_border
                );

        MaterialButton shareButton =
                createAnswerActionButton(
                        "↗ शेयर करें",
                        R.color.ss_blue_soft,
                        R.color.ss_blue_border
                );

        LinearLayout.LayoutParams copyButtonParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        copyButtonParams.setMarginEnd(
                dpToPx(
                        6
                )
        );

        copyButton.setLayoutParams(
                copyButtonParams
        );

        LinearLayout.LayoutParams shareButtonParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        shareButtonParams.setMarginStart(
                dpToPx(
                        6
                )
        );

        shareButton.setLayoutParams(
                shareButtonParams
        );

        actionsLayout.addView(
                copyButton
        );

        actionsLayout.addView(
                shareButton
        );

        int insertionIndex =
                answerContainer.indexOfChild(
                        speechControlsLayout
                );

        if (insertionIndex >= 0) {
            insertionIndex++;

        } else {
            insertionIndex =
                    answerContainer.indexOfChild(
                            this
                    )
                            + 1;
        }

        insertionIndex =
                Math.max(
                        0,
                        Math.min(
                                insertionIndex,
                                answerContainer.getChildCount()
                        )
                );

        answerContainer.addView(
                actionsLayout,
                insertionIndex
        );

        answerActionsLayout =
                actionsLayout;

        copyAnswerButton =
                copyButton;

        shareAnswerButton =
                shareButton;
    }

    @NonNull
    private MaterialButton createAnswerActionButton(
            @NonNull String buttonText,
            int backgroundColorResource,
            int strokeColorResource
    ) {
        MaterialButton button =
                new MaterialButton(
                        getContext(),
                        null,
                        com.google.android.material.R.attr
                                .materialButtonOutlinedStyle
                );

        button.setText(
                buttonText
        );

        button.setAllCaps(
                false
        );

        button.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                13
        );

        button.setTextColor(
                ContextCompat.getColor(
                        getContext(),
                        R.color.ss_text_primary
                )
        );

        button.setMinHeight(
                dpToPx(
                        52
                )
        );

        button.setMinimumHeight(
                dpToPx(
                        52
                )
        );

        button.setCornerRadius(
                dpToPx(
                        16
                )
        );

        button.setStrokeWidth(
                dpToPx(
                        1
                )
        );

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                getContext(),
                                backgroundColorResource
                        )
                )
        );

        button.setStrokeColor(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                getContext(),
                                strokeColorResource
                        )
                )
        );

        button.setInsetTop(
                0
        );

        button.setInsetBottom(
                0
        );

        button.setContentDescription(
                buttonText
        );

        return button;
    }

    private int dpToPx(
            int dpValue
    ) {
        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dpValue
                        * density
        );
    }

    @Override
    public void setText(
            @Nullable CharSequence text,
            @NonNull BufferType type
    ) {
        if (markdownFormattingInProgress) {
            super.setText(
                    text,
                    type
            );

            return;
        }

        String incomingText =
                text == null
                        ? ""
                        : text.toString()
                        .trim();

        if (!incomingText.equals(
                currentAnswerText
        )) {
            stopAnswerSpeechSilently();
        }

        currentAnswerText =
                incomingText;

        if (incomingText.isEmpty()) {
            markdownFormattingInProgress =
                    true;

            try {
                super.setText(
                        "",
                        type
                );

            } finally {
                markdownFormattingInProgress =
                        false;
            }

            post(
                    this::hideAllAnswerControls
            );

            return;
        }

        markdownFormattingInProgress =
                true;

        try {
            CharSequence formattedText =
                    renderAdvancedAnswer(
                            incomingText
                    );

            super.setText(
                    formattedText,
                    BufferType.SPANNABLE
            );

        } finally {
            markdownFormattingInProgress =
                    false;
        }

        post(
                this::updateAnswerControlsForCurrentText
        );
    }

    /**
     * पूरे AI answer को Android Spannable में बदलता है।
     */
    @NonNull
    private CharSequence renderAdvancedAnswer(
            @NonNull String sourceText
    ) {
        String normalizedText =
                normalizeAnswerMarkup(
                        sourceText
                );

        SpannableStringBuilder formattedAnswer =
                new SpannableStringBuilder();

        String[] answerLines =
                normalizedText.split(
                        "\\n",
                        -1
                );

        for (int lineIndex = 0;
             lineIndex < answerLines.length;
             lineIndex++) {

            appendFormattedLine(
                    formattedAnswer,
                    answerLines[
                            lineIndex
                            ]
            );

            if (lineIndex
                    < answerLines.length - 1) {

                formattedAnswer.append(
                        '\n'
                );
            }
        }

        return formattedAnswer;
    }

    /**
     * Heading, divider, bullet या normal line render करता है।
     */
    private void appendFormattedLine(
            @NonNull SpannableStringBuilder destination,
            @NonNull String rawLine
    ) {
        String trimmedLine =
                rawLine.trim();

        if (trimmedLine.isEmpty()) {
            return;
        }

        if (HORIZONTAL_RULE_PATTERN
                .matcher(
                        trimmedLine
                )
                .matches()) {

            int dividerStart =
                    destination.length();

            destination.append(
                    DISPLAY_DIVIDER
            );

            int dividerEnd =
                    destination.length();

            destination.setSpan(
                    new RelativeSizeSpan(
                            0.85f
                    ),
                    dividerStart,
                    dividerEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            return;
        }

        Matcher headingMatcher =
                HEADING_PATTERN.matcher(
                        trimmedLine
                );

        if (headingMatcher.matches()) {
            String headingMarkers =
                    safeGroup(
                            headingMatcher,
                            1
                    );

            String headingText =
                    safeGroup(
                            headingMatcher,
                            2
                    );

            int headingLevel =
                    Math.max(
                            1,
                            headingMarkers.length()
                    );

            int headingStart =
                    destination.length();

            appendInlineMarkdown(
                    destination,
                    headingText
            );

            int headingEnd =
                    destination.length();

            if (headingEnd
                    > headingStart) {

                destination.setSpan(
                        new StyleSpan(
                                Typeface.BOLD
                        ),
                        headingStart,
                        headingEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                destination.setSpan(
                        new RelativeSizeSpan(
                                getHeadingScale(
                                        headingLevel
                                )
                        ),
                        headingStart,
                        headingEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            return;
        }

        Matcher bulletMatcher =
                BULLET_PATTERN.matcher(
                        trimmedLine
                );

        if (bulletMatcher.matches()) {
            destination.append(
                    "• "
            );

            appendInlineMarkdown(
                    destination,
                    safeGroup(
                            bulletMatcher,
                            1
                    )
            );

            return;
        }

        appendInlineMarkdown(
                destination,
                trimmedLine
        );
    }

    private float getHeadingScale(
            int headingLevel
    ) {
        switch (headingLevel) {
            case 1:
                return 1.30f;

            case 2:
                return 1.22f;

            case 3:
                return 1.16f;

            case 4:
                return 1.11f;

            case 5:
            case 6:
            default:
                return 1.07f;
        }
    }

    /**
     * Bold और inline-code formatting।
     */
    private void appendInlineMarkdown(
            @NonNull SpannableStringBuilder destination,
            @NonNull String sourceLine
    ) {
        int currentPosition =
                0;

        while (currentPosition
                < sourceLine.length()) {

            InlineMarker nextMarker =
                    findNextInlineMarker(
                            sourceLine,
                            currentPosition
                    );

            if (nextMarker == null) {
                destination.append(
                        sourceLine,
                        currentPosition,
                        sourceLine.length()
                );

                break;
            }

            if (nextMarker.position
                    > currentPosition) {

                destination.append(
                        sourceLine,
                        currentPosition,
                        nextMarker.position
                );
            }

            int markerContentStart =
                    nextMarker.position
                            + nextMarker.marker.length();

            int closingMarkerPosition =
                    sourceLine.indexOf(
                            nextMarker.marker,
                            markerContentStart
                    );

            if (closingMarkerPosition < 0) {
                destination.append(
                        nextMarker.marker
                );

                currentPosition =
                        markerContentStart;

                continue;
            }

            String markedContent =
                    sourceLine.substring(
                            markerContentStart,
                            closingMarkerPosition
                    );

            int spanStart =
                    destination.length();

            destination.append(
                    markedContent
            );

            int spanEnd =
                    destination.length();

            if (spanEnd
                    > spanStart) {

                if (nextMarker.type
                        == InlineMarkerType.BOLD) {

                    destination.setSpan(
                            new StyleSpan(
                                    Typeface.BOLD
                            ),
                            spanStart,
                            spanEnd,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );

                } else {
                    destination.setSpan(
                            new TypefaceSpan(
                                    "monospace"
                            ),
                            spanStart,
                            spanEnd,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }

            currentPosition =
                    closingMarkerPosition
                            + nextMarker.marker.length();
        }
    }

    @Nullable
    private InlineMarker findNextInlineMarker(
            @NonNull String sourceLine,
            int startPosition
    ) {
        InlineMarker bestMarker =
                null;

        bestMarker =
                chooseEarlierMarker(
                        bestMarker,
                        createMarker(
                                sourceLine,
                                BOLD_MARKER_A,
                                InlineMarkerType.BOLD,
                                startPosition
                        )
                );

        bestMarker =
                chooseEarlierMarker(
                        bestMarker,
                        createMarker(
                                sourceLine,
                                BOLD_MARKER_B,
                                InlineMarkerType.BOLD,
                                startPosition
                        )
                );

        bestMarker =
                chooseEarlierMarker(
                        bestMarker,
                        createMarker(
                                sourceLine,
                                CODE_MARKER,
                                InlineMarkerType.CODE,
                                startPosition
                        )
                );

        return bestMarker;
    }

    @Nullable
    private InlineMarker createMarker(
            @NonNull String sourceLine,
            @NonNull String marker,
            @NonNull InlineMarkerType markerType,
            int startPosition
    ) {
        int markerPosition =
                sourceLine.indexOf(
                        marker,
                        startPosition
                );

        if (markerPosition < 0) {
            return null;
        }

        return new InlineMarker(
                markerPosition,
                marker,
                markerType
        );
    }

    @Nullable
    private InlineMarker chooseEarlierMarker(
            @Nullable InlineMarker currentMarker,
            @Nullable InlineMarker candidateMarker
    ) {
        if (candidateMarker == null) {
            return currentMarker;
        }

        if (currentMarker == null) {
            return candidateMarker;
        }

        if (candidateMarker.position
                < currentMarker.position) {

            return candidateMarker;
        }

        if (candidateMarker.position
                == currentMarker.position
                && candidateMarker.marker.length()
                > currentMarker.marker.length()) {

            return candidateMarker;
        }

        return currentMarker;
    }

    /**
     * Markdown और सामान्य LaTeX को साफ readable text में बदलता है।
     */
    @NonNull
    private String normalizeAnswerMarkup(
            @NonNull String sourceText
    ) {
        String result =
                sourceText
                        .replace(
                                "\r\n",
                                "\n"
                        )
                        .replace(
                                '\r',
                                '\n'
                        );

        result =
                replacePattern(
                        MARKDOWN_LINK_PATTERN,
                        result,
                        "$1"
                );

        for (int pass = 0;
             pass < 5;
             pass++) {

            String updatedResult =
                    replacePattern(
                            LATEX_FRACTION_PATTERN,
                            result,
                            "$1/$2"
                    );

            updatedResult =
                    replacePattern(
                            LATEX_TEXT_PATTERN,
                            updatedResult,
                            "$1"
                    );

            updatedResult =
                    replacePattern(
                            LATEX_STYLE_PATTERN,
                            updatedResult,
                            "$1"
                    );

            if (updatedResult.equals(
                    result
            )) {
                break;
            }

            result =
                    updatedResult;
        }

        /*
         * Regex के बजाय direct replace उपयोग किया गया है,
         * जिससे Android ICU पर brace-related crash नहीं होगा।
         */
        result =
                result
                        .replace(
                                "^{2}",
                                "²"
                        )
                        .replace(
                                "^2",
                                "²"
                        )
                        .replace(
                                "^{3}",
                                "³"
                        )
                        .replace(
                                "^3",
                                "³"
                        );

        result =
                result
                        .replace(
                                "\\times",
                                "×"
                        )
                        .replace(
                                "\\cdot",
                                "·"
                        )
                        .replace(
                                "\\div",
                                "÷"
                        )
                        .replace(
                                "\\pm",
                                "±"
                        )
                        .replace(
                                "\\sqrt",
                                "√"
                        )
                        .replace(
                                "\\leq",
                                "≤"
                        )
                        .replace(
                                "\\geq",
                                "≥"
                        )
                        .replace(
                                "\\neq",
                                "≠"
                        )
                        .replace(
                                "\\approx",
                                "≈"
                        )
                        .replace(
                                "\\infty",
                                "∞"
                        )
                        .replace(
                                "\\pi",
                                "π"
                        )
                        .replace(
                                "\\degree",
                                "°"
                        )
                        .replace(
                                "\\left",
                                ""
                        )
                        .replace(
                                "\\right",
                                ""
                        )
                        .replace(
                                "\\,",
                                " "
                        )
                        .replace(
                                "\\;",
                                " "
                        )
                        .replace(
                                "\\:",
                                " "
                        )
                        .replace(
                                "\\!",
                                ""
                        )
                        .replace(
                                "\\(",
                                ""
                        )
                        .replace(
                                "\\)",
                                ""
                        )
                        .replace(
                                "\\[",
                                ""
                        )
                        .replace(
                                "\\]",
                                ""
                        )
                        .replace(
                                "$$",
                                ""
                        )
                        .replace(
                                "$",
                                ""
                        )
                        .replace(
                                "\\%",
                                "%"
                        )
                        .replace(
                                "\\_",
                                "_"
                        )
                        .replace(
                                "\\#",
                                "#"
                        )
                        .replaceAll(
                                "\\\\([A-Za-z]+)",
                                "$1"
                        )
                        .replace(
                                "{",
                                ""
                        )
                        .replace(
                                "}",
                                ""
                        )
                        .replaceAll(
                                "[ \\t]{2,}",
                                " "
                        )
                        .replaceAll(
                                "\\n{4,}",
                                "\n\n\n"
                        )
                        .trim();

        return result;
    }

    /**
     * Clipboard और Share के लिए plain-text answer।
     */
    @NonNull
    private String createPlainAnswerText(
            @NonNull String sourceText
    ) {
        return normalizeAnswerMarkup(
                sourceText
        )
                .replace(
                        "**",
                        ""
                )
                .replace(
                        "__",
                        ""
                )
                .replace(
                        "`",
                        ""
                )
                .replaceAll(
                        "(?m)^\\s*#{1,6}\\s*",
                        ""
                )
                .replaceAll(
                        "(?m)^\\s*(?:-{3,}|_{3,}|\\*{3,})\\s*$",
                        ""
                )
                .replaceAll(
                        "\\n{3,}",
                        "\n\n"
                )
                .trim();
    }

    /**
     * Answer clipboard में copy करता है।
     */
    private void copyCurrentAnswer() {
        if (!isFinalAnswerAvailable()) {
            Snackbar.make(
                    this,
                    "Copy करने के लिए final answer उपलब्ध नहीं है।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        String plainAnswer =
                createPlainAnswerText(
                        currentAnswerText
                );

        if (plainAnswer.isEmpty()) {
            Snackbar.make(
                    this,
                    "Copy करने के लिए answer खाली है।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        ClipboardManager clipboardManager =
                (ClipboardManager) getContext()
                        .getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );

        if (clipboardManager == null) {
            Snackbar.make(
                    this,
                    "Clipboard सेवा उपलब्ध नहीं है।",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        ClipData clipData =
                ClipData.newPlainText(
                        "Study Saathi Answer",
                        plainAnswer
                );

        clipboardManager.setPrimaryClip(
                clipData
        );

        Snackbar.make(
                this,
                "Answer clipboard में copy हो गया है।",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    /**
     * Question और Answer Android share sheet में भेजता है।
     */
    private void shareCurrentAnswer() {
        if (!isFinalAnswerAvailable()) {
            Snackbar.make(
                    this,
                    "Share करने के लिए final answer उपलब्ध नहीं है।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        String plainAnswer =
                createPlainAnswerText(
                        currentAnswerText
                );

        if (plainAnswer.isEmpty()) {
            Snackbar.make(
                    this,
                    "Share करने के लिए answer खाली है।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        String questionText =
                getDisplayedQuestionText();

        StringBuilder shareContent =
                new StringBuilder();

        shareContent.append(
                "Study Saathi"
        );

        if (!questionText.isEmpty()) {
            shareContent.append(
                    "\n\nQuestion:\n"
            );

            shareContent.append(
                    questionText
            );
        }

        shareContent.append(
                "\n\nAnswer:\n"
        );

        shareContent.append(
                plainAnswer
        );

        Intent shareIntent =
                new Intent(
                        Intent.ACTION_SEND
                );

        shareIntent.setType(
                "text/plain"
        );

        shareIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Study Saathi Answer"
        );

        shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                shareContent.toString()
        );

        Intent chooserIntent =
                Intent.createChooser(
                        shareIntent,
                        "Study Saathi answer शेयर करें"
                );

        if (!(getContext()
                instanceof Activity)) {

            chooserIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );
        }

        try {
            getContext().startActivity(
                    chooserIntent
            );

        } catch (ActivityNotFoundException exception) {
            Snackbar.make(
                    this,
                    "Answer share करने वाला कोई app उपलब्ध नहीं है।",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (RuntimeException exception) {
            Snackbar.make(
                    this,
                    "Answer share नहीं हो सका।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @NonNull
    private String getDisplayedQuestionText() {
        TextView displayedQuestion =
                getRootView()
                        .findViewById(
                                R.id.textUserQuestion
                        );

        if (displayedQuestion == null
                || displayedQuestion.getText() == null) {

            return "";
        }

        return displayedQuestion
                .getText()
                .toString()
                .trim();
    }

    @NonNull
    private String replacePattern(
            @NonNull Pattern pattern,
            @NonNull String source,
            @NonNull String replacement
    ) {
        return pattern
                .matcher(
                        source
                )
                .replaceAll(
                        replacement
                );
    }

    @NonNull
    private String safeGroup(
            @NonNull Matcher matcher,
            int groupIndex
    ) {
        String groupValue =
                matcher.group(
                        groupIndex
                );

        return groupValue == null
                ? ""
                : groupValue.trim();
    }

    /**
     * Final answer होने पर सभी actions दिखाता है।
     */
    private void updateAnswerControlsForCurrentText() {
        if (!isAttachedToWindow()) {
            return;
        }

        if (!isFinalAnswerAvailable()) {
            hideAllAnswerControls();
            return;
        }

        showAnswerActions();

        showSpeechReadyState(
                "Answer सुनने के लिए नीचे button दबाएँ।"
        );
    }

    private boolean isFinalAnswerAvailable() {
        if (currentAnswerText.isEmpty()) {
            return false;
        }

        View rootView =
                getRootView();

        View answerCard =
                rootView.findViewById(
                        R.id.cardAnswer
                );

        View askButton =
                rootView.findViewById(
                        R.id.buttonAskSaathi
                );

        boolean answerCardVisible =
                answerCard != null
                        && answerCard.getVisibility()
                        == View.VISIBLE;

        boolean aiRequestFinished =
                askButton == null
                        || askButton.isEnabled();

        return answerCardVisible
                && aiRequestFinished;
    }

    private void showAnswerActions() {
        LinearLayout availableActionsLayout =
                answerActionsLayout;

        if (availableActionsLayout != null) {
            availableActionsLayout.setVisibility(
                    View.VISIBLE
            );
        }

        MaterialButton availableCopyButton =
                copyAnswerButton;

        if (availableCopyButton != null) {
            availableCopyButton.setEnabled(
                    true
            );
        }

        MaterialButton availableShareButton =
                shareAnswerButton;

        if (availableShareButton != null) {
            availableShareButton.setEnabled(
                    true
            );
        }
    }

    private void hideAnswerActions() {
        LinearLayout availableActionsLayout =
                answerActionsLayout;

        if (availableActionsLayout != null) {
            availableActionsLayout.setVisibility(
                    View.GONE
            );
        }
    }

    private void startAnswerSpeech() {
        if (!isFinalAnswerAvailable()) {
            Snackbar.make(
                    this,
                    "पहले Smart AI answer पूरा होने दें।",
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        StudyAnswerSpeaker availableSpeaker =
                answerSpeaker;

        if (availableSpeaker == null) {
            availableSpeaker =
                    new StudyAnswerSpeaker(
                            getContext()
                    );

            answerSpeaker =
                    availableSpeaker;
        }

        MaterialButton availableSpeakButton =
                speakAnswerButton;

        if (availableSpeakButton != null) {
            availableSpeakButton.setEnabled(
                    false
            );
        }

        availableSpeaker.speak(
                currentAnswerText,
                resolvePreferredSpeechLanguage(
                        currentAnswerText
                ),
                createSpeechCallback()
        );
    }

    @NonNull
    private StudyAnswerSpeaker.SpeechCallback
    createSpeechCallback() {

        return new StudyAnswerSpeaker.SpeechCallback() {

            @Override
            public void onPreparing() {
                if (suppressSpeechCallback) {
                    return;
                }

                showSpeechActiveState(
                        "Voice तैयार की जा रही है..."
                );
            }

            @Override
            public void onStarted() {
                if (suppressSpeechCallback) {
                    return;
                }

                showSpeechActiveState(
                        "Study Saathi answer पढ़ रही है।"
                );
            }

            @Override
            public void onCompleted() {
                if (suppressSpeechCallback) {
                    return;
                }

                showSpeechReadyState(
                        "Answer पूरा पढ़ लिया गया है।"
                );
            }

            @Override
            public void onStopped() {
                if (suppressSpeechCallback) {
                    return;
                }

                showSpeechReadyState(
                        "Answer पढ़ना रोक दिया गया है।"
                );
            }

            @Override
            public void onError(
                    @NonNull String errorMessage
            ) {
                if (suppressSpeechCallback) {
                    return;
                }

                showSpeechReadyState(
                        errorMessage
                );

                Snackbar.make(
                        StudyMarkdownTextView.this,
                        errorMessage,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        };
    }

    private void showSpeechActiveState(
            @NonNull String statusMessage
    ) {
        showAnswerActions();

        if (speechControlsLayout != null) {
            speechControlsLayout.setVisibility(
                    View.VISIBLE
            );
        }

        if (speechStatusText != null) {
            speechStatusText.setText(
                    statusMessage
            );

            speechStatusText.setVisibility(
                    View.VISIBLE
            );
        }

        if (speakAnswerButton != null) {
            speakAnswerButton.setEnabled(
                    false
            );

            speakAnswerButton.setVisibility(
                    View.GONE
            );
        }

        if (stopAnswerButton != null) {
            stopAnswerButton.setEnabled(
                    true
            );

            stopAnswerButton.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void showSpeechReadyState(
            @NonNull String statusMessage
    ) {
        if (!isFinalAnswerAvailable()) {
            hideAllAnswerControls();
            return;
        }

        showAnswerActions();

        if (speechControlsLayout != null) {
            speechControlsLayout.setVisibility(
                    View.VISIBLE
            );
        }

        if (speechStatusText != null) {
            speechStatusText.setText(
                    statusMessage
            );

            speechStatusText.setVisibility(
                    View.VISIBLE
            );
        }

        if (speakAnswerButton != null) {
            speakAnswerButton.setEnabled(
                    true
            );

            speakAnswerButton.setVisibility(
                    View.VISIBLE
            );
        }

        if (stopAnswerButton != null) {
            stopAnswerButton.setEnabled(
                    false
            );

            stopAnswerButton.setVisibility(
                    View.GONE
            );
        }
    }

    private void stopAnswerSpeechByUser() {
        StudyAnswerSpeaker availableSpeaker =
                answerSpeaker;

        if (availableSpeaker == null
                || !availableSpeaker.isSpeaking()) {

            showSpeechReadyState(
                    "अभी कोई answer नहीं पढ़ा जा रहा है।"
            );

            return;
        }

        availableSpeaker.stop();
    }

    private void stopAnswerSpeechSilently() {
        StudyAnswerSpeaker availableSpeaker =
                answerSpeaker;

        if (availableSpeaker == null
                || !availableSpeaker.isSpeaking()) {

            return;
        }

        suppressSpeechCallback =
                true;

        try {
            availableSpeaker.stop();

        } finally {
            suppressSpeechCallback =
                    false;
        }
    }

    @NonNull
    private String resolvePreferredSpeechLanguage(
            @NonNull String answerText
    ) {
        boolean containsDevanagari =
                containsDevanagariCharacters(
                        answerText
                );

        boolean containsLatin =
                containsLatinCharacters(
                        answerText
                );

        if (containsDevanagari
                && containsLatin) {

            return "Hindi + English";
        }

        if (containsDevanagari) {
            return "Hindi";
        }

        return "English";
    }

    private boolean containsDevanagariCharacters(
            @NonNull String text
    ) {
        for (int index = 0;
             index < text.length();
             index++) {

            char character =
                    text.charAt(
                            index
                    );

            if ((character >= '\u0900'
                    && character <= '\u097F')
                    || (character >= '\uA8E0'
                    && character <= '\uA8FF')) {

                return true;
            }
        }

        return false;
    }

    private boolean containsLatinCharacters(
            @NonNull String text
    ) {
        for (int index = 0;
             index < text.length();
             index++) {

            char character =
                    text.charAt(
                            index
                    );

            if ((character >= 'A'
                    && character <= 'Z')
                    || (character >= 'a'
                    && character <= 'z')) {

                return true;
            }
        }

        return false;
    }

    private void hideAllAnswerControls() {
        hideAnswerActions();

        if (speechControlsLayout != null) {
            speechControlsLayout.setVisibility(
                    View.GONE
            );
        }

        if (speechStatusText != null) {
            speechStatusText.setText(
                    ""
            );

            speechStatusText.setVisibility(
                    View.GONE
            );
        }

        if (speakAnswerButton != null) {
            speakAnswerButton.setEnabled(
                    true
            );

            speakAnswerButton.setVisibility(
                    View.VISIBLE
            );
        }

        if (stopAnswerButton != null) {
            stopAnswerButton.setEnabled(
                    false
            );

            stopAnswerButton.setVisibility(
                    View.GONE
            );
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnswerSpeechSilently();

        if (speakAnswerButton != null) {
            speakAnswerButton.setOnClickListener(
                    null
            );
        }

        if (stopAnswerButton != null) {
            stopAnswerButton.setOnClickListener(
                    null
            );
        }

        if (copyAnswerButton != null) {
            copyAnswerButton.setOnClickListener(
                    null
            );
        }

        if (shareAnswerButton != null) {
            shareAnswerButton.setOnClickListener(
                    null
            );
        }

        StudyAnswerSpeaker availableSpeaker =
                answerSpeaker;

        answerSpeaker =
                null;

        if (availableSpeaker != null) {
            availableSpeaker.shutdown();
        }

        speechControlsLayout =
                null;

        speechStatusText =
                null;

        speakAnswerButton =
                null;

        stopAnswerButton =
                null;

        answerActionsLayout =
                null;

        copyAnswerButton =
                null;

        shareAnswerButton =
                null;

        super.onDetachedFromWindow();
    }

    private enum InlineMarkerType {

        BOLD,

        CODE
    }

    private static final class InlineMarker {

        private final int position;

        @NonNull
        private final String marker;

        @NonNull
        private final InlineMarkerType type;

        private InlineMarker(
                int position,
                @NonNull String marker,
                @NonNull InlineMarkerType type
        ) {
            this.position =
                    position;

            this.marker =
                    marker;

            this.type =
                    type;
        }
    }
}