package com.tridev.studysaathi.overlay;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.tridev.studysaathi.R;
import com.tridev.studysaathi.data.ai.FirebaseStudyTutorClient;
import com.tridev.studysaathi.data.ai.SmartTutorAnswerResult;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;

/** Floating Study AI that stays above the current app without opening Dashboard. */
public final class StudyOverlayBubbleService extends Service {
    private static final String CHANNEL = "study_ai_overlay";
    private static final String ACTION_TOGGLE = "com.tridev.studysaathi.overlay.TOGGLE";
    private static final String PREFS = "study_ai_overlay_preferences";
    private static final String KEY_ALPHA = "bubble_alpha";

    private WindowManager windowManager;
    private TextView bubble;
    private WindowManager.LayoutParams bubbleParams;
    private FrameLayout panel;
    private WindowManager.LayoutParams panelParams;
    private TextView transcript;
    private TextView status;
    private EditText question;
    private Button send;
    private ObjectAnimator pulse;
    private FirebaseStudyTutorClient tutorClient;
    private boolean asking;
    private int resizeCorner;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private float resizeDownX;
    private float resizeDownY;

    public static void start(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) return;
        try {
            ContextCompat.startForegroundService(context,
                    new Intent(context, StudyOverlayBubbleService.class));
        } catch (RuntimeException ignored) { }
    }

    public static void stop(@NonNull Context context) {
        context.stopService(new Intent(context, StudyOverlayBubbleService.class));
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        PendingIntent toggle = PendingIntent.getService(this, 71,
                new Intent(this, StudyOverlayBubbleService.class).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        startForeground(47021, new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("AI Study Assistant")
                .setContentText("Tap to open the floating study panel")
                .setContentIntent(toggle).setOngoing(true).build());
        if (!signedIn() || (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this))) {
            stopSelf();
            return;
        }
        tutorClient = new FirebaseStudyTutorClient(this);
        showBubble();
    }

    private void showBubble() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubble = text("AI", 12, Color.WHITE);
        bubble.setGravity(Gravity.CENTER);
        bubble.setElevation(dp(9));
        bubble.setAlpha(savedAlpha());
        GradientDrawable background = rounded(Color.rgb(42, 82, 200),
                Color.argb(170, 220, 237, 255), 30);
        background.setShape(GradientDrawable.OVAL);
        bubble.setBackground(background);
        bubbleParams = new WindowManager.LayoutParams(dp(52), dp(52), overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = getResources().getDisplayMetrics().widthPixels - dp(64);
        bubbleParams.y = Math.round(getResources().getDisplayMetrics().heightPixels * .62f);
        bubble.setOnTouchListener(new BubbleTouch());
        bubble.setOnLongClickListener(v -> {
            setBubbleAlpha(bubble.getAlpha() > .55f ? .3f : .8f);
            return true;
        });
        windowManager.addView(bubble, bubbleParams);
        pulse = ObjectAnimator.ofFloat(bubble, View.SCALE_X, 1f, 1.05f, 1f);
        pulse.setDuration(1900L);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();
    }

    private void togglePanel() {
        if (panel == null) showPanel(); else removePanel();
    }

    private void showPanel() {
        panel = new FrameLayout(this);
        panel.setBackground(rounded(Color.rgb(248, 250, 255),
                Color.rgb(136, 158, 211), 24));
        panel.setElevation(dp(16));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(15), dp(10), dp(15), dp(12));
        panel.addView(body, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("✦ Study Saathi AI", 17, Color.rgb(25, 47, 91));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView minimize = headerAction("—");
        TextView close = headerAction("×");
        header.addView(minimize);
        header.addView(close);
        body.addView(header);

        status = text("General Studies • Active profile", 12, Color.rgb(94, 105, 128));
        body.addView(status, matchWrap());
        ScrollView scroll = new ScrollView(this);
        transcript = text("नमस्ते! अपना सवाल लिखें। उत्तर इसी screen पर मिलेगा।",
                14, Color.rgb(30, 42, 67));
        transcript.setPadding(dp(12), dp(10), dp(12), dp(10));
        transcript.setBackground(rounded(Color.WHITE, Color.rgb(215, 224, 242), 16));
        scroll.addView(transcript);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollLp.topMargin = dp(8);
        body.addView(scroll, scrollLp);

        question = new EditText(this);
        question.setHint("अपना सवाल लिखें…");
        question.setTextSize(14);
        question.setMinLines(2);
        question.setMaxLines(4);
        question.setPadding(dp(12), dp(8), dp(12), dp(8));
        question.setBackground(rounded(Color.WHITE, Color.rgb(96, 128, 205), 15));
        LinearLayout.LayoutParams inputLp = matchWrap();
        inputLp.topMargin = dp(8);
        body.addView(question, inputLp);

        send = new Button(this);
        send.setText("Ask AI");
        send.setTextColor(Color.WHITE);
        send.setTextSize(13);
        send.setAllCaps(false);
        send.setBackground(rounded(Color.rgb(43, 91, 201), Color.rgb(43, 91, 201), 14));
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(-1, dp(48));
        sendLp.topMargin = dp(7);
        body.addView(send, sendLp);

        LinearLayout transparency = new LinearLayout(this);
        transparency.setGravity(Gravity.CENTER_VERTICAL);
        transparency.addView(text("Bubble transparency", 11, Color.rgb(78, 91, 118)));
        SeekBar alpha = new SeekBar(this);
        alpha.setMax(100);
        alpha.setProgress(Math.round(savedAlpha() * 100));
        transparency.addView(alpha, new LinearLayout.LayoutParams(0, dp(40), 1));
        body.addView(transparency, matchWrap());

        addCornerHint("⌜", Gravity.START | Gravity.TOP);
        addCornerHint("⌝", Gravity.END | Gravity.TOP);
        addCornerHint("⌞", Gravity.START | Gravity.BOTTOM);
        addCornerHint("⌟", Gravity.END | Gravity.BOTTOM);

        minimize.setOnClickListener(v -> removePanel());
        close.setOnClickListener(v -> removePanel());
        title.setOnTouchListener(new PanelDragTouch());
        send.setOnClickListener(v -> askQuestion());
        alpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (user) setBubbleAlpha(Math.max(.2f, value / 100f));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        panel.setOnTouchListener(this::resizeFromCorners);

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        panelParams = new WindowManager.LayoutParams(Math.min(dp(390), width - dp(24)),
                Math.min(dp(560), height - dp(90)), overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = Math.max(dp(12), width - panelParams.width - dp(12));
        panelParams.y = dp(70);
        panelParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        windowManager.addView(panel, panelParams);
        question.requestFocus();
        question.postDelayed(() -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .showSoftInput(question, InputMethodManager.SHOW_IMPLICIT), 180);
    }

    private boolean resizeFromCorners(View view, MotionEvent event) {
        int edge = dp(32);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            boolean left = event.getX() <= edge;
            boolean right = event.getX() >= panel.getWidth() - edge;
            boolean top = event.getY() <= edge;
            boolean bottom = event.getY() >= panel.getHeight() - edge;
            if (!(left || right) || !(top || bottom)) return false;
            resizeCorner = (left ? 1 : 2) | (top ? 4 : 8);
            resizeStartWidth = panelParams.width;
            resizeStartHeight = panelParams.height;
            resizeDownX = event.getRawX();
            resizeDownY = event.getRawY();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && resizeCorner != 0) {
            float dx = event.getRawX() - resizeDownX;
            float dy = event.getRawY() - resizeDownY;
            int width = resizeStartWidth
                    + ((resizeCorner & 1) != 0 ? Math.round(-dx) : Math.round(dx));
            int height = resizeStartHeight
                    + ((resizeCorner & 4) != 0 ? Math.round(-dy) : Math.round(dy));
            panelParams.width = Math.max(dp(285), Math.min(
                    getResources().getDisplayMetrics().widthPixels - dp(16), width));
            panelParams.height = Math.max(dp(360), Math.min(
                    getResources().getDisplayMetrics().heightPixels - dp(50), height));
            windowManager.updateViewLayout(panel, panelParams);
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            boolean handled = resizeCorner != 0;
            resizeCorner = 0;
            return handled;
        }
        return resizeCorner != 0;
    }

    private void askQuestion() {
        String prompt = question.getText().toString().trim();
        if (prompt.isEmpty() || asking) return;
        asking = true;
        send.setEnabled(false);
        status.setText("उत्तर तैयार किया जा रहा है…");
        transcript.append("\n\nआप: " + prompt);
        question.setText("");
        new StudentProfileRepository(this).getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override public void onSuccess(@Nullable StudentProfileEntity profile) {
                        if (profile == null) {
                            finishError("Active student profile उपलब्ध नहीं है।");
                            return;
                        }
                        FirebaseStudyTutorClient.TutorRequest request =
                                new FirebaseStudyTutorClient.TutorRequest(
                                        profile.getStudentName(), profile.getEducationBoard(),
                                        profile.getStudentClass(), profile.getExplanationLanguage(),
                                        "General Studies", "", prompt);
                        tutorClient.askTextQuestionWithResult(request,
                                new FirebaseStudyTutorClient.TutorResultCallback() {
                                    @Override public void onSuccess(@NonNull SmartTutorAnswerResult result) {
                                        asking = false;
                                        if (panel == null) return;
                                        send.setEnabled(true);
                                        status.setText("उत्तर तैयार है");
                                        transcript.append("\n\nStudy Saathi: " + result.getRawAnswerText());
                                    }
                                    @Override public void onError(@NonNull Throwable error) {
                                        finishError("अभी उत्तर नहीं मिल पाया। Internet जाँचकर दोबारा प्रयास करें।");
                                    }
                                });
                    }
                    @Override public void onError(@NonNull Exception error) {
                        finishError("Student profile load नहीं हो पाया।");
                    }
                });
    }

    private void finishError(String message) {
        asking = false;
        if (panel == null) return;
        send.setEnabled(true);
        status.setText(message);
    }

    private void addCornerHint(String symbol, int gravity) {
        TextView hint = text(symbol, 16, Color.rgb(50, 85, 170));
        hint.setAlpha(.25f);
        hint.setGravity(Gravity.CENTER);
        hint.setClickable(false);
        panel.addView(hint, new FrameLayout.LayoutParams(dp(28), dp(28), gravity));
    }

    private final class BubbleTouch implements View.OnTouchListener {
        int startX, startY; float downX, downY; boolean moved;
        @Override public boolean onTouch(View view, MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startX = bubbleParams.x; startY = bubbleParams.y;
                downX = event.getRawX(); downY = event.getRawY(); moved = false;
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                int dx = Math.round(event.getRawX() - downX);
                int dy = Math.round(event.getRawY() - downY);
                moved |= Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5);
                bubbleParams.x = Math.max(0, startX + dx);
                bubbleParams.y = Math.max(dp(24), startY + dy);
                windowManager.updateViewLayout(bubble, bubbleParams);
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                if (!moved) togglePanel();
                return true;
            }
            return false;
        }
    }

    private final class PanelDragTouch implements View.OnTouchListener {
        int startX, startY; float downX, downY;
        @Override public boolean onTouch(View view, MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startX = panelParams.x; startY = panelParams.y;
                downX = event.getRawX(); downY = event.getRawY(); return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                panelParams.x = Math.max(0, startX + Math.round(event.getRawX() - downX));
                panelParams.y = Math.max(dp(24), startY + Math.round(event.getRawY() - downY));
                windowManager.updateViewLayout(panel, panelParams); return true;
            }
            return event.getActionMasked() == MotionEvent.ACTION_UP;
        }
    }

    private void removePanel() {
        if (panel != null && windowManager != null) {
            try { windowManager.removeView(panel); } catch (RuntimeException ignored) { }
        }
        panel = null; panelParams = null; transcript = null;
        status = null; question = null; send = null;
    }

    private void setBubbleAlpha(float alpha) {
        float safe = Math.max(.2f, Math.min(1f, alpha));
        if (bubble != null) bubble.setAlpha(safe);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putFloat(KEY_ALPHA, safe).apply();
    }

    private float savedAlpha() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getFloat(KEY_ALPHA, .78f);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        return view;
    }

    private TextView headerAction(String label) {
        TextView view = text(label, 22, Color.rgb(42, 67, 122));
        view.setGravity(Gravity.CENTER);
        view.setBackground(rounded(Color.rgb(235, 241, 253), Color.rgb(200, 214, 240), 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
        lp.leftMargin = dp(6); view.setLayoutParams(lp); return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill); drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke); return drawable;
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "AI Study Overlay", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private boolean signedIn() {
        try { return FirebaseAuth.getInstance().getCurrentUser() != null; }
        catch (RuntimeException error) { return false; }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction()) && bubble != null) {
            togglePanel();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (pulse != null) pulse.cancel();
        removePanel();
        if (bubble != null && windowManager != null) {
            try { windowManager.removeView(bubble); } catch (RuntimeException ignored) { }
        }
        bubble = null;
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
