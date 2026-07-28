package com.tridev.studysaathi.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
import com.tridev.studysaathi.MainActivity;
import com.tridev.studysaathi.ManualSchoolBookActivity;
import com.tridev.studysaathi.ManualSchoolBookChapterActivity;
import com.tridev.studysaathi.ParentDashboardActivity;
import com.tridev.studysaathi.R;
import com.tridev.studysaathi.SchoolBookChapterContentEditorActivity;
import com.tridev.studysaathi.SchoolBookChapterPageReviewActivity;
import com.tridev.studysaathi.SchoolCurriculumSetupActivity;
import com.tridev.studysaathi.StudentProfileActivity;

/**
 * Login के बाद student-learning screens पर स्थिर Study Saathi AI entry point।
 *
 * असली AI, offline fallback, photo OCR, voice और conversation rendering
 * AskStudySaathiActivity में ही रहते हैं। यह controller केवल वर्तमान page का
 * सुरक्षित context उस established Hero flow तक पहुँचाता है।
 */
public final class SmartAiCompanionController {

    public static final String EXTRA_OPEN_INPUT_MODE =
            "extra_open_input_mode";
    public static final String INPUT_MODE_VOICE = "voice";
    public static final String INPUT_MODE_PHOTO = "photo";

    private static final int VIEW_TAG_KEY =
            R.id.smartAiCompanionRoot;

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
        if (!(content instanceof FrameLayout)
                || content.getTag(VIEW_TAG_KEY) != null) {
            return;
        }

        FrameLayout host = (FrameLayout) content;
        View companion = LayoutInflater.from(activity).inflate(
                R.layout.view_smart_ai_companion,
                host,
                false
        );

        int margin = dp(activity, 16);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dp(activity, 300),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.BOTTOM
        );
        params.setMargins(margin, margin, margin, margin);
        host.addView(companion, params);
        content.setTag(VIEW_TAG_KEY, companion);

        configure(activity, companion);
    }

    public static void detach(@NonNull Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) {
            return;
        }

        Object tagged = content.getTag(VIEW_TAG_KEY);
        if (tagged instanceof View) {
            View companion = (View) tagged;
            Object animator = companion.getTag();
            if (animator instanceof ObjectAnimator) {
                ((ObjectAnimator) animator).cancel();
            }
            ((FrameLayout) content).removeView(companion);
        }
        content.setTag(VIEW_TAG_KEY, null);
    }

    private static void configure(
            @NonNull Activity activity,
            @NonNull View companion
    ) {
        MaterialCardView card = companion.findViewById(
                R.id.cardSmartAiCompanion
        );
        TextView aiButton = companion.findViewById(
                R.id.buttonSmartAiCompanion
        );
        TextView contextText = companion.findViewById(
                R.id.textSmartAiContext
        );
        EditText question = companion.findViewById(
                R.id.editSmartAiQuestion
        );

        String pageContext = resolvePageContext(activity);
        contextText.setText(activity.getString(
                R.string.smart_companion_context_format,
                pageContext
        ));

        aiButton.setOnClickListener(view -> {
            boolean opening = card.getVisibility() != View.VISIBLE;
            if (opening) {
                card.setAlpha(0f);
                card.setScaleX(0.92f);
                card.setScaleY(0.92f);
                card.setVisibility(View.VISIBLE);
                card.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180L)
                        .start();
            } else {
                card.setVisibility(View.GONE);
            }
        });

        companion.findViewById(R.id.buttonSmartAiSend)
                .setOnClickListener(view ->
                        openHero(activity, question, pageContext, ""));
        companion.findViewById(R.id.buttonSmartAiVoice)
                .setOnClickListener(view ->
                        openHero(
                                activity,
                                question,
                                pageContext,
                                INPUT_MODE_VOICE
                        ));
        companion.findViewById(R.id.buttonSmartAiPhoto)
                .setOnClickListener(view ->
                        openHero(
                                activity,
                                question,
                                pageContext,
                                INPUT_MODE_PHOTO
                        ));

        question.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEND) {
                return false;
            }
            openHero(activity, question, pageContext, "");
            return true;
        });

        ObjectAnimator pulse = ObjectAnimator.ofFloat(
                aiButton,
                View.SCALE_X,
                1f,
                1.06f,
                1f
        );
        pulse.setDuration(2200L);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();
        companion.setTag(pulse);
    }

    private static void openHero(
            @NonNull Activity activity,
            @NonNull EditText questionView,
            @NonNull String pageContext,
            @NonNull String inputMode
    ) {
        String question = safe(questionView.getText());
        if (inputMode.isEmpty() && question.isEmpty()) {
            Snackbar.make(
                    questionView,
                    R.string.smart_companion_empty_question,
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent = new Intent(activity, AskStudySaathiActivity.class);
        intent.putExtra(
                AskStudySaathiActivity.EXTRA_PREFILL_SUBJECT,
                resolveExtra(
                        activity,
                        AskStudySaathiActivity.EXTRA_PREFILL_SUBJECT,
                        "extra_subject_name",
                        "subject_name"
                )
        );
        intent.putExtra(
                AskStudySaathiActivity.EXTRA_PREFILL_CHAPTER,
                resolveExtra(
                        activity,
                        AskStudySaathiActivity.EXTRA_PREFILL_CHAPTER,
                        "extra_chapter_title",
                        "chapter_title"
                )
        );
        intent.putExtra(
                AskStudySaathiActivity.EXTRA_PREFILL_QUESTION,
                question
        );
        intent.putExtra(EXTRA_OPEN_INPUT_MODE, inputMode);
        intent.putExtra("extra_source_page_context", pageContext);
        activity.startActivity(intent);
    }

    @NonNull
    private static String resolvePageContext(@NonNull Activity activity) {
        CharSequence title = activity.getTitle();
        if (!TextUtils.isEmpty(title)
                && !activity.getString(R.string.app_name)
                .contentEquals(title)) {
            return title.toString().trim();
        }

        String name = activity.getClass().getSimpleName()
                .replace("Activity", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2");
        return name.trim();
    }

    @NonNull
    private static String resolveExtra(
            @NonNull Activity activity,
            @NonNull String... keys
    ) {
        for (String key : keys) {
            String value = safe(activity.getIntent().getStringExtra(key));
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    @NonNull
    private static String safe(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(
                value * activity.getResources()
                        .getDisplayMetrics().density
        );
    }

    private static boolean isStudentLearningScreen(
            @NonNull Activity activity
    ) {
        return !(activity instanceof MainActivity)
                && !(activity instanceof StudentProfileActivity)
                && !(activity instanceof AskStudySaathiActivity)
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
