package com.tridev.studysaathi;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.View;
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
    private String helpSearchQuery = "";

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
        binding.editHelpSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                helpSearchQuery = text == null ? "" : text.toString().trim();
                renderGuide();
            }
            @Override public void afterTextChanged(Editable editable) { }
        });
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
        binding.textHelpMode.setText(modeTitle(parent));
        binding.textHelpMotto.setText(motto());
        binding.textHelpIntro.setText(parent ? parentHeading() : studentHeading());
        binding.textHelpProfileContext.setText(profileContext);
        binding.textHelpSafety.setText(parent ? parentSafety() : studentSafety());
        binding.editHelpSearch.setHint(searchHint());

        binding.containerHelpSteps.removeAllViews();
        String[][] steps = parent ? parentSteps() : studentSteps();
        int visibleCount = 0;
        for (int index = 0; index < steps.length; index++) {
            if (matchesSearch(steps[index][0], steps[index][1])) {
                addStepCard(index + 1, steps[index][0], steps[index][1]);
                visibleCount++;
            }
        }
        binding.textHelpNoResults.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
        binding.textHelpNoResults.setText(noResultsText());
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

    private String modeTitle(boolean parent) {
        if (LANGUAGE_HINDI.equals(language)) {
            return parent ? "सुरक्षित अभिभावक मोड गाइड" : "विद्यार्थी सहायता गाइड";
        }
        if (LANGUAGE_ENGLISH.equals(language)) {
            return parent ? "Secure Parent Mode Guide" : "Student Learning Guide";
        }
        return parent ? "अभिभावक गाइड • Parent Guide" : "विद्यार्थी गाइड • Student Guide";
    }

    private String searchHint() {
        if (LANGUAGE_HINDI.equals(language)) return "फीचर खोजें—जैसे AI, backup, revision";
        if (LANGUAGE_ENGLISH.equals(language)) return "Search features—AI, backup, revision…";
        return "फीचर खोजें • Search features";
    }

    private String noResultsText() {
        if (LANGUAGE_HINDI.equals(language)) return "इस खोज से कोई help step नहीं मिला। दूसरा शब्द लिखें।";
        if (LANGUAGE_ENGLISH.equals(language)) return "No help step matches this search. Try another word.";
        return "कोई step नहीं मिला • No matching help step";
    }

    private boolean matchesSearch(@NonNull String title, @NonNull String detail) {
        if (helpSearchQuery.isEmpty()) return true;
        String query = helpSearchQuery.toLowerCase(java.util.Locale.ROOT);
        return title.toLowerCase(java.util.Locale.ROOT).contains(query)
                || detail.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    private String[][] studentSteps() {
        String[][] hi = {
                {"1. प्रोफाइल और भाषा तैयार करें", "Dashboard पर सही student profile चुनें। Profile में Class, Board, study medium और explanation language सही रखें। Settings → Theme & Language से पूरे app को हिन्दी, English या दोनों में दिखाएँ।"},
                {"2. विषय, किताब और chapter चुनें", "Subjects खोलें → विषय चुनें → chapter खोलें। यदि school book उपलब्ध नहीं है तो parent से Curriculum Setup में सही किताब scan या manually add करवाएँ। Tutor इसी saved class, board, subject और chapter को प्राथमिकता देता है।"},
                {"3. Lesson पढ़ें और वहीं से जारी रखें", "Lesson में content पढ़ें, font छोटा-बड़ा करें और पूरा होने पर lesson complete करें। Dashboard का Continue Learning पिछला अधूरा lesson खोलता है। Chapter Page Reader में चुने हुए book pages पढ़े जा सकते हैं।"},
                {"4. Ask Study Saathi में प्रश्न पूछें", "Ask Study Saathi खोलें → subject/chapter जाँचें → प्रश्न लिखें और Send दबाएँ। Mic से बोलकर, camera से photo लेकर या gallery image लगाकर भी प्रश्न भेज सकते हैं। साफ photo और पूरा सवाल बेहतर उत्तर देते हैं।"},
                {"5. AI उत्तर की विश्वसनीयता समझें", "उत्तर में confidence/verification और उपलब्ध होने पर book-page citation देखें। Approved chapter evidence से बाहर का page AI नहीं बताना चाहिए। Caution या retry संदेश मिले तो प्रश्न स्पष्ट करें अथवा सही chapter/page चुनें।"},
                {"6. आसान, गहरा या Socratic उत्तर लें", "‘आसान भाषा में समझाओ’, ‘और गहराई से बताओ’ या ‘मुझे hint देकर सिखाओ’ लिखें। Tutor आपकी learning level, पुरानी गलतियों और पसंदीदा learning style के अनुसार explanation बदलता है।"},
                {"7. Diagram, table और handwritten answer जाँचें", "पूरी और साफ image जोड़ें। ‘इस diagram/table को समझाओ’ या ‘मेरे handwritten steps check करो’ लिखें। AI readable labels और हर step को जाँचता है; धुँधले या कटे हिस्से का अनुमान नहीं लगाता।"},
                {"8. Summary, notes और visual revision बनाएँ", "‘इस chapter का summary बनाओ’, ‘flowchart बनाओ’ या ‘important points बताओ’ लिखें। उपयोगी उत्तर को Chapter Notes में रखें। All Chapter Notes से सभी notes खोजें और दोबारा खोलें।"},
                {"9. Practice questions और adaptive difficulty", "‘5 practice questions बनाओ’ लिखें। एक-एक उत्तर दें और कठिनाई बढ़ाने/घटाने को कहें। Tutor सही उत्तरों पर level बढ़ाता और गलती पर छोटा hint तथा सरल अगला प्रश्न देता है।"},
                {"10. Mock exam और step-wise evaluation", "‘20 marks का mock test लो’ लिखें। पहले बिना answers के paper लें, समय में हल करें, फिर उत्तर जमा करके marks और step-wise feedback माँगें। पहली गलत step, कारण और corrected continuation देखें।"},
                {"11. Re-test और Mistake Notebook", "‘मेरे गलत concepts का दोबारा test लो’ लिखें। Tutor बदले हुए numbers/wording के साथ re-test करता है। कमजोर या low-confidence concept Mistake Notebook और revision priority में वापस आता है।"},
                {"12. Flashcards और spaced repetition", "‘इस chapter के flashcards बनाओ’ लिखें। Front देखकर उत्तर याद करें, फिर Back जाँचें। Revision Planner गलतियों, confidence, बीते दिनों और correct streak से अगली revision तथा forgetting risk तय करता है।"},
                {"13. Smart Study Plan और daily queue", "Dashboard → Smart Study Plan खोलें। आज के lesson, practice और revision actions क्रम से पूरे करें। Daily Revision Queue पहले overdue, high-risk और कमजोर topics दिखाती है।"},
                {"14. Voice Tutor, Read-Along और pronunciation", "Mic से प्रश्न बोलें और speaker button से उत्तर सुनें। ‘साथ पढ़ो’ कहने पर छोटे हिस्सों में read-along करें। ‘इस शब्द का उच्चारण बताओ’ कहने पर syllable, sound hint और repeat-after-me अभ्यास मिलता है।"},
                {"15. AI Companion से लगातार follow-up", "किसी supported screen पर floating AI icon दबाएँ। उसी panel में ‘क्यों?’, ‘दूसरा example’ या ‘पिछला step समझाओ’ पूछें। Conversation context follow-up समझने में मदद करता है।"},
                {"16. Search, bookmarks और history", "Global Search से lessons, chapters और उपलब्ध content खोजें। जरूरी page/bookmark save करें। Doubt History में पुराने AI प्रश्न और उत्तर देखें; गलत profile/subject हो तो नया सही प्रश्न पूछें।"},
                {"17. Progress और weekly study देखें", "Learning Progress में completed lessons, quiz performance और progress देखें। Weekly Study में streak/activity जाँचें। Parent द्वारा तय daily goal के actions पूरे होने पर dashboard progress बदलती है।"},
                {"18. Offline उपयोग और sync", "Downloaded/local lessons, verified offline knowledge और cached answers सीमित network में मदद करते हैं। नया AI answer या online book search इंटरनेट माँग सकता है। Sync/restore parent की देखरेख में और सही account पर करें।"},
                {"19. Reminder और notification", "Settings → Daily Study Reminder में enable करें, समय और दिन चुनें, फिर Save Reminder दबाएँ। Test Notification से जाँचें। Notification का Snooze action reminder को थोड़ी देर बाद फिर दिखाता है।"},
                {"20. समस्या आने पर क्या करें", "पहले सही profile, subject, chapter, भाषा, internet और permissions जाँचें। धुँधली photo दोबारा लें। Sync के बाद Build/ऐप restart करें। फिर भी समस्या हो तो error का पूरा screenshot और उस समय किए गए steps parent/developer को दें।"}
        };
        String[][] en = {
                {"1. Set up profile and language", "Select the correct student profile on Dashboard. Keep Class, Board, study medium and explanation language accurate. Use Settings → Theme & Language to display the app in Hindi, English or Bilingual mode."},
                {"2. Choose subject, book and chapter", "Open Subjects → choose a subject → open a chapter. If the school book is missing, ask a parent to scan or add it in Curriculum Setup. The tutor prioritises the saved class, board, subject and chapter."},
                {"3. Read lessons and resume", "Read lesson content, adjust font size and mark the lesson complete. Continue Learning opens the last unfinished lesson. Chapter Page Reader can display selected pages from an added school book."},
                {"4. Ask a question", "Open Ask Study Saathi → verify subject/chapter → type the question → tap Send. You may also speak, take a camera photo or attach a gallery image. A clear full image and complete question produce better results."},
                {"5. Understand answer reliability", "Check confidence/verification and book-page citation when available. AI should not claim a page outside approved chapter evidence. If a caution or retry message appears, clarify the question or select the correct chapter/page."},
                {"6. Request easy, deep or Socratic teaching", "Ask ‘explain simply’, ‘go deeper’ or ‘teach me with hints’. The tutor adapts to learning level, earlier misconceptions and preferred learning style."},
                {"7. Check diagrams, tables and handwriting", "Attach a complete clear image and ask to explain the diagram/table or check handwritten steps. AI inspects readable labels and each step, and must not guess blurred or cropped content."},
                {"8. Create summaries, notes and visual revision", "Ask for a chapter summary, flowchart or important points. Keep useful material in Chapter Notes. Use All Chapter Notes to search and reopen saved notes."},
                {"9. Generate adaptive practice", "Ask for practice questions, answer one at a time and request higher or lower difficulty. The tutor can raise difficulty after success or provide a small hint and easier next item after an error."},
                {"10. Run a mock exam and evaluate steps", "Ask for a timed mock test with marks. Solve the paper before requesting answers, then submit responses for marks and step-wise feedback. Review the first wrong step, its reason and corrected continuation."},
                {"11. Re-test mistakes", "Ask to retest weak concepts. The tutor changes numbers or wording instead of repeating a memorised answer. Weak or low-confidence concepts return to the Mistake Notebook and revision priority."},
                {"12. Use flashcards and spaced repetition", "Ask for chapter flashcards, recall the Front, then verify the Back. Revision scheduling uses mistakes, confidence, elapsed days and correct streak to estimate the next review and forgetting risk."},
                {"13. Follow Smart Study Plan", "Open Dashboard → Smart Study Plan and complete today’s lesson, practice and revision actions in order. Daily Revision Queue prioritises overdue, high-risk and weak topics."},
                {"14. Use Voice Tutor, Read-Along and pronunciation", "Speak through the microphone and listen with the speaker button. Ask for read-along to work through short sections. Ask for pronunciation to receive syllable breaks, sound guidance and repeat-after-me practice."},
                {"15. Continue with AI Companion", "Tap the floating AI icon on a supported screen. Ask follow-ups such as ‘why?’, ‘another example’ or ‘explain the previous step’. Conversation context helps the tutor understand references."},
                {"16. Search, bookmark and review history", "Use Global Search for lessons, chapters and available content. Save important bookmarks. Open Doubt History for earlier AI questions and answers; ask again if the saved profile or subject was wrong."},
                {"17. Review learning progress", "Learning Progress shows completed lessons, quiz performance and progress. Weekly Study shows streak/activity. Dashboard progress changes as actions in the parent-set daily goal are completed."},
                {"18. Learn offline and sync safely", "Downloaded/local lessons, verified offline knowledge and cached answers help with limited connectivity. New AI answers or online book search may require internet. Sync or restore only under parent supervision and on the correct account."},
                {"19. Configure reminders", "Open Settings → Daily Study Reminder, enable it, choose time and days, then tap Save Reminder. Use Test Notification to verify it. Snooze shows the reminder again after a short delay."},
                {"20. Troubleshoot", "Check profile, subject, chapter, language, internet and permissions first. Retake blurred photos. Restart the app after sync/build. If the issue continues, share the complete error screenshot and exact steps with a parent or developer."}
        };
        return combine(hi, en);
    }

    private String[][] parentSteps() {
        String[][] hi = {
                {"1. Parent Mode में सुरक्षित प्रवेश", "Verified account से Parent Mode चुनें और phone PIN, pattern या password से device-lock check पूरा करें। Parent controls बच्चे को unlocked न दें।"},
                {"2. अलग student profiles बनाएँ", "हर बच्चे के लिए अलग profile बनाएँ। सही नाम, Class 1–12, Board, medium और explanation language चुनें। Profile बदलने पर progress, books, doubts और plans भी उसी बच्चे के अनुसार दिखते हैं।"},
                {"3. Curriculum और school details सेट करें", "State, district, board और school चुनें। Suggested Subjects की सूची जाँचें, केवल पढ़ाए जाने वाले subjects जोड़ें और गलत subject हटाएँ।"},
                {"4. असली school books जोड़ें", "Book cover scan करें या title/author manually भरें। सही edition और class जाँचें, chapters import/add करें और chapter boundaries/pages review करें। गलत book AI grounding को प्रभावित कर सकती है।"},
                {"5. Content और citations सत्यापित करें", "Chapter content editor/page reader में text और page range जाँचें। AI citation, semantic search और grounded answers इसी approved evidence पर निर्भर हैं। अस्पष्ट scan को approve न करें।"},
                {"6. Daily goal और Smart Study Plan", "Settings में 1, 3 या 5 daily actions तय करें। Smart Study Plan में lesson, practice और revision balance देखें। बच्चे की class और उपलब्ध समय के अनुसार realistic goal रखें।"},
                {"7. Reminder और smart notifications", "Daily Study Reminder enable करें → सही दिन/समय चुनें → Save करें → Test Notification भेजें। Battery restrictions के कारण थोड़ी देरी हो सकती है; Snooze action भी जाँचें।"},
                {"8. Parent Dashboard पढ़ें", "Student-wise lessons, completed activity, quiz scores और daily progress देखें। Citation coverage और recommended revision cards से कमजोर/कम-grounded chapter पहचानें। Revision started/completed प्रतिशत भी देखें।"},
                {"9. Doubts और AI answers review करें", "Doubts & AI Answers में बच्चे के प्रश्न, subject और answer देखें। Retry/caution, गलत citation या बार-बार misconception दिखे तो सही book chapter जाँचें और बच्चे/teacher के साथ concept दोहराएँ।"},
                {"10. Revision और forgetting risk संभालें", "Low score, repeated mistake, low confidence और overdue review को प्राथमिकता दें। Recommended Revision खोलें और completion track करें। केवल total screen time नहीं, concept mastery देखें।"},
                {"11. Notes, bookmarks और search", "Chapter Notes और bookmarks review करें। Global Search से गलत/duplicate content पहचानें। बच्चे की personal note हटाने से पहले उसकी जरूरत पूछें।"},
                {"12. Local backup बनाएँ", "Settings/Backup में export शुरू करें, सुरक्षित location चुनें और file पूर्ण होने दें। Backup में profiles, progress, quizzes, doubts, notes, bookmarks, goals और reminder preferences हो सकते हैं।"},
                {"13. Encrypted cloud backup और restore", "केवल verified cloud account उपयोग करें। Backup encryption enabled रखें। Restore से पहले account, backup date और target device जाँचें—restore मौजूदा data बदल सकता है। प्रक्रिया के बीच app बंद न करें।"},
                {"14. Privacy और account सुरक्षा", "Password, OTP, address, phone number या बच्चे की निजी पहचान AI prompt में न लिखें। Device lock और verified email सक्रिय रखें। Cloud backup/account delete के लिए current-password verification पूरा करें।"},
                {"15. Offline और network व्यवहार", "Local lessons/cache offline चल सकते हैं; नया Gemini answer, book search और cloud sync इंटरनेट माँग सकते हैं। Failure पर बार-बार duplicate submit न करें—network जाँचकर retry करें।"},
                {"16. भाषा और accessibility", "Settings में हिन्दी, English या Bilingual interface चुनें। Student profile की explanation language अलग setting है। बड़े font, speaker, voice input और read-along को बच्चे की जरूरत के अनुसार उपयोग करें।"},
                {"17. Student Mode में सुरक्षित लौटें", "Switch to Student Mode दबाएँ और learning-only interface बच्चे को दें। Parent Mode दोबारा खोलते समय verified account तथा device security check सुनिश्चित करें।"},
                {"18. समस्या रिपोर्ट करने का सही तरीका", "Profile नाम, screen, किए गए exact steps और पूरा error screenshot लिखें। Backup/restore समस्या में file name/date दें, लेकिन password/OTP कभी साझा न करें। Developer fix के बाद Git Pull, Gradle Sync और Rebuild करें।"}
        };
        String[][] en = {
                {"1. Enter Parent Mode securely", "Choose Parent Mode with a verified account and complete the device-lock check using PIN, pattern or password. Do not hand unlocked parent controls to a child."},
                {"2. Create separate student profiles", "Create one profile per child with the correct name, Class 1–12, Board, medium and explanation language. Progress, books, doubts and plans follow the selected profile."},
                {"3. Configure curriculum and school", "Choose state, district, board and school. Review Suggested Subjects, add only subjects actually taught and remove incorrect entries."},
                {"4. Add the actual school books", "Scan the cover or enter title/author manually. Verify edition and class, add/import chapters and review chapter boundaries/pages. A wrong book can weaken AI grounding."},
                {"5. Verify content and citations", "Review text and page ranges in chapter content/page reader. AI citation, semantic search and grounded answers depend on approved evidence. Do not approve an unreadable scan."},
                {"6. Set daily goal and study plan", "Choose 1, 3 or 5 daily actions in Settings. Review the balance of lessons, practice and revision in Smart Study Plan. Keep goals realistic for the child’s class and available time."},
                {"7. Configure smart reminders", "Enable Daily Study Reminder, select days/time, save and send a Test Notification. Battery restrictions may cause small delays; also test the Snooze action."},
                {"8. Read Parent Dashboard", "Review lessons, completed activity, quiz scores and daily progress per student. Use citation coverage and recommended revision cards to identify weak or poorly grounded chapters and track completion percentage."},
                {"9. Review doubts and AI answers", "Check each question, subject and answer in Doubts & AI Answers. If caution/retry, bad citations or repeated misconceptions appear, verify the book chapter and revise the concept with the child or teacher."},
                {"10. Manage revision and forgetting risk", "Prioritise low scores, repeated mistakes, low confidence and overdue reviews. Open Recommended Revision and track completion. Focus on concept mastery, not only total screen time."},
                {"11. Review notes, bookmarks and search", "Review Chapter Notes and bookmarks. Use Global Search to find incorrect or duplicate content. Ask the child before removing personal notes."},
                {"12. Create a local backup", "Start export from Backup, select a safe location and allow completion. A backup may include profiles, progress, quizzes, doubts, notes, bookmarks, goals and reminder preferences."},
                {"13. Use encrypted cloud backup and restore", "Use only a verified cloud account and keep encryption enabled. Before restoring, verify account, backup date and target device because restore may replace current data. Do not close the app mid-process."},
                {"14. Protect privacy and account", "Never put passwords, OTPs, address, phone number or a child’s private identity in an AI prompt. Keep device lock and verified email active. Complete current-password verification for cloud backup/account deletion."},
                {"15. Understand offline and network behaviour", "Local lessons/cache may work offline; new Gemini answers, online book search and cloud sync can require internet. Do not create duplicate requests after failure—check the connection, then retry."},
                {"16. Configure language and accessibility", "Select Hindi, English or Bilingual interface in Settings. The student profile’s explanation language is separate. Use larger text, speaker, voice input and read-along according to the child’s needs."},
                {"17. Return safely to Student Mode", "Tap Switch to Student Mode and hand over the learning-only interface. Require the verified account and device security check whenever Parent Mode is opened again."},
                {"18. Report a problem properly", "Record profile name, screen, exact steps and the complete error screenshot. For backup/restore issues, include file name/date but never a password or OTP. After a developer fix, Git Pull, Gradle Sync and Rebuild."}
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
