package com.tridev.studysaathi;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityHelpAboutBinding;

public final class HelpAboutActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "extra_help_mode";
    public static final String MODE_STUDENT = "STUDENT";
    public static final String MODE_PARENT = "PARENT";

    private static final String LANGUAGE_HINDI = "HINDI";
    private static final String LANGUAGE_ENGLISH = "ENGLISH";
    private static final String LANGUAGE_BILINGUAL = "BILINGUAL";

    private ActivityHelpAboutBinding binding;
    private String mode = MODE_STUDENT;
    private String language = LANGUAGE_BILINGUAL;
    private String profileContext = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode = MODE_PARENT.equals(getIntent().getStringExtra(EXTRA_MODE))
                ? MODE_PARENT : MODE_STUDENT;

        binding.buttonHelpBack.setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );
        binding.textHelpVersion.setText("Version " + BuildConfig.VERSION_NAME);
        binding.toggleHelpLanguage.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) {
                        return;
                    }
                    if (checkedId == R.id.buttonHelpHindi) {
                        language = LANGUAGE_HINDI;
                    } else if (checkedId == R.id.buttonHelpEnglish) {
                        language = LANGUAGE_ENGLISH;
                    } else {
                        language = LANGUAGE_BILINGUAL;
                    }
                    renderGuide();
                }
        );

        String savedLanguage = AppAppearancePreferences.getLanguage(this);
        if (AppAppearancePreferences.LANGUAGE_HINDI.equals(savedLanguage)) {
            binding.toggleHelpLanguage.check(R.id.buttonHelpHindi);
        } else if (AppAppearancePreferences.LANGUAGE_ENGLISH.equals(savedLanguage)) {
            binding.toggleHelpLanguage.check(R.id.buttonHelpEnglish);
        } else {
            binding.toggleHelpLanguage.check(R.id.buttonHelpBilingual);
        }

        if (MODE_STUDENT.equals(mode)) {
            loadStudentContext();
        } else {
            profileContext = "Parent Mode • verified account और device-lock protection";
            renderGuide();
        }
    }

    private void loadStudentContext() {
        new StudentProfileRepository(this).getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(StudentProfileEntity profile) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (profile == null) {
                            profileContext = "Student Mode • profile setup pending";
                        } else {
                            profileContext = profile.getStudentName()
                                    + " • " + profile.getStudentClass()
                                    + " • " + profile.getEducationBoard()
                                    + " • " + profile.getStudyMedium();
                        }
                        renderGuide();
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        profileContext = "Student Mode • learning guide";
                        renderGuide();
                    }
                }
        );
    }

    private void renderGuide() {
        boolean parent = MODE_PARENT.equals(mode);
        binding.textHelpMode.setText(
                parent ? "Secure Parent Mode Guide" : "Student Learning Guide"
        );
        binding.textHelpMotto.setText(motto());
        binding.textHelpIntro.setText(parent ? parentHeading() : studentHeading());
        binding.textHelpProfileContext.setText(profileContext);
        binding.textHelpSafety.setText(parent ? parentSafety() : studentSafety());

        binding.containerHelpSteps.removeAllViews();
        String[][] steps = parent ? parentSteps() : studentSteps();
        for (int index = 0; index < steps.length; index++) {
            addStepCard(index + 1, steps[index][0], steps[index][1]);
        }
    }

    private void addStepCard(int number, @NonNull String title, @NonNull String detail) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(20));
        card.setCardElevation(0f);
        card.setCardBackgroundColor(getColor(
                number % 3 == 1 ? R.color.ss_blue_soft
                        : number % 3 == 2 ? R.color.ss_green_soft
                        : R.color.ss_purple_soft
        ));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(
                number % 3 == 1 ? R.color.ss_blue_border
                        : number % 3 == 2 ? R.color.ss_green_border
                        : R.color.ss_purple_border
        ));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        if (number > 1) {
            cardParams.topMargin = dp(8);
        }
        card.setLayoutParams(cardParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(15), dp(15), dp(15), dp(15));

        TextView badge = new TextView(this);
        badge.setText(String.valueOf(number));
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setTextColor(getColor(R.color.ss_on_primary));
        badge.setTextSize(15);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setBackgroundResource(R.drawable.bg_help_step_badge);
        row.addView(badge, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        copyParams.leftMargin = dp(13);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getColor(R.color.ss_text_primary));
        titleView.setTextSize(16);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        copy.addView(titleView);

        TextView detailView = new TextView(this);
        detailView.setText(detail);
        detailView.setTextColor(getColor(R.color.ss_text_secondary));
        detailView.setTextSize(13);
        detailView.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = dp(5);
        copy.addView(detailView, detailParams);
        row.addView(copy, copyParams);
        card.addView(row);
        binding.containerHelpSteps.addView(card);
    }

    private String motto() {
        if (LANGUAGE_HINDI.equals(language)) return "समझो • अभ्यास करो • आगे बढ़ो";
        if (LANGUAGE_ENGLISH.equals(language)) return "Understand • Practise • Grow";
        return "समझो • Practise • आगे बढ़ो";
    }

    private String studentHeading() {
        if (LANGUAGE_HINDI.equals(language)) return "Study Saathi से पढ़ाई कैसे करें";
        if (LANGUAGE_ENGLISH.equals(language)) return "How to learn with Study Saathi";
        return "Study Saathi से पढ़ाई कैसे करें • How to learn";
    }

    private String parentHeading() {
        if (LANGUAGE_HINDI.equals(language)) return "सुरक्षित Parent Mode का पूरा उपयोग";
        if (LANGUAGE_ENGLISH.equals(language)) return "Complete guide to secure Parent Mode";
        return "सुरक्षित Parent Mode • Complete parent guide";
    }

    private String[][] studentSteps() {
        String[][] hi = {
                {"अपना विषय चुनें", "Subjects खोलें, विषय और वर्तमान chapter चुनें। AI इसी class, board और chapter को प्राथमिकता देगा।"},
                {"सवाल पूछें", "Ask Study Saathi में सवाल लिखें, बोलें, camera से photo लें या gallery image चुनें।"},
                {"उत्तर को समझें", "सरल उत्तर, उदाहरण और practice question पढ़ें। Speaker से उत्तर सुनें और quick buttons से आसान explanation माँगें।"},
                {"पढ़ाई जारी रखें", "Continue Learning, Chapter Notes और Smart Study Plan से वहीं से आगे बढ़ें जहाँ पिछली बार छोड़ा था।"},
                {"Revision और अभ्यास", "Revision Planner तथा practice questions से दोहराएँ। गलतियों को सीखने का हिस्सा मानें।"},
                {"अपनी progress देखें", "Weekly Study और Learning Progress में streak, completed lessons और parent द्वारा तय daily goal देखें।"},
                {"AI साथी का उपयोग", "किसी भी page पर नीचे AI icon दबाएँ। उसी panel में लगातार सवाल-जवाब करें; पुरानी conversation सुरक्षित रहती है।"}
        };
        String[][] en = {
                {"Choose your subject", "Open Subjects, then select the subject and current chapter. AI prioritises the saved class, board and chapter."},
                {"Ask a question", "In Ask Study Saathi, type, speak, take a camera photo or choose an image from the gallery."},
                {"Understand the answer", "Read the simple answer, example and practice question. Use speaker and quick actions for easier explanations."},
                {"Continue learning", "Use Continue Learning, Chapter Notes and Smart Study Plan to resume from where you stopped."},
                {"Revise and practise", "Use Revision Planner and practice questions. Treat mistakes as part of learning."},
                {"Check your progress", "Weekly Study and Learning Progress show streaks, completed lessons and the daily goal set by a parent."},
                {"Use the AI companion", "Tap the floating AI icon on any page. Ask follow-up questions in the same panel; the conversation is saved."}
        };
        return combine(hi, en);
    }

    private String[][] parentSteps() {
        String[][] hi = {
                {"सुरक्षित प्रवेश", "Verified account से Parent Mode चुनें और phone PIN, pattern या password से दूसरा security check पूरा करें।"},
                {"Student profile बनाएँ", "हर बच्चे के लिए अलग profile, Class 1–12, board, medium और explanation language चुनें।"},
                {"Curriculum तैयार करें", "State, district, board और school चुनें। Suggested Subjects जोड़ें, फिर actual school books scan या manually add करें।"},
                {"Daily goal और reminders", "App Settings में 1, 3 या 5 daily actions तय करें और Manage Study Reminders में सही दिन तथा समय चुनें।"},
                {"Progress और कमजोर topics", "Parent Dashboard में lessons, scores, activity और student-wise progress देखें। Doubts & AI Answers में पूछे गए सवाल review करें।"},
                {"Backup और restore", "समय-समय पर local backup बनाएँ। Verified cloud account में encrypted backup रखें; restore से पहले सही file/account जाँचें।"},
                {"Account और data safety", "Cloud backup या account delete करने के लिए current password verification जरूरी है। बच्चे के local data को सोच-समझकर manage करें।"},
                {"Student Mode में लौटें", "Switch to Student Mode दबाएँ। बच्चे को केवल learning interface दें; parent controls device-lock के पीछे सुरक्षित रहें।"}
        };
        String[][] en = {
                {"Enter securely", "Choose Parent Mode with a verified account, then complete the second check using the phone PIN, pattern or password."},
                {"Create student profiles", "Create a separate profile for each child and select Class 1–12, board, medium and explanation language."},
                {"Prepare the curriculum", "Choose state, district, board and school. Add suggested subjects, then scan or manually add the actual school books."},
                {"Set goals and reminders", "Choose 1, 3 or 5 daily actions in App Settings, then select the correct reminder days and time."},
                {"Review progress and weak areas", "Use Parent Dashboard for lessons, scores, activity and student progress. Review questions in Doubts & AI Answers."},
                {"Backup and restore", "Create regular local backups. Keep an encrypted backup in the verified cloud account and verify the file/account before restoring."},
                {"Protect account and data", "Current-password verification is required to delete a cloud backup or account. Manage the child’s local data carefully."},
                {"Return to Student Mode", "Tap Switch to Student Mode. Give the child the learning-only interface while parent controls remain protected by device lock."}
        };
        return combine(hi, en);
    }

    private String[][] combine(String[][] hindi, String[][] english) {
        if (LANGUAGE_HINDI.equals(language)) return hindi;
        if (LANGUAGE_ENGLISH.equals(language)) return english;
        String[][] result = new String[hindi.length][2];
        for (int index = 0; index < hindi.length; index++) {
            result[index][0] = hindi[index][0] + " • " + english[index][0];
            result[index][1] = hindi[index][1] + "\n\n" + english[index][1];
        }
        return result;
    }

    private String studentSafety() {
        if (LANGUAGE_HINDI.equals(language)) return "सुरक्षित सीखना: AI से निजी जानकारी साझा न करें। संदेह वाले तथ्य teacher या parent से जाँचें। Emergency या health समस्या में किसी भरोसेमंद बड़े से तुरंत बात करें।";
        if (LANGUAGE_ENGLISH.equals(language)) return "Safe learning: Never share private information with AI. Verify uncertain facts with a teacher or parent. For an emergency or health concern, speak to a trusted adult immediately.";
        return "सुरक्षित सीखना • Safe learning\nनिजी जानकारी साझा न करें। Verify uncertain facts with a teacher or parent.";
    }

    private String parentSafety() {
        if (LANGUAGE_HINDI.equals(language)) return "Parent checklist: Email verified रखें, device lock चालू रखें, books/curriculum जाँचें, doubts review करें और नियमित encrypted backup बनाएँ।";
        if (LANGUAGE_ENGLISH.equals(language)) return "Parent checklist: Keep email verified and device lock enabled, review books/curriculum and doubts, and create regular encrypted backups.";
        return "Parent checklist • अभिभावक जाँच\nVerified email, device lock, reviewed curriculum और regular encrypted backup बनाए रखें।";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
