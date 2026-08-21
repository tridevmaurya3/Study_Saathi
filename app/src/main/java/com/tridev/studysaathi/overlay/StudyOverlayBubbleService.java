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
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.tridev.studysaathi.DashboardActivity;
import com.tridev.studysaathi.R;
import com.tridev.studysaathi.ui.SmartAiCompanionController;

public final class StudyOverlayBubbleService extends Service {
    private static final String CHANNEL = "study_ai_overlay";
    private WindowManager manager;
    private TextView bubble;
    private WindowManager.LayoutParams params;
    private ObjectAnimator pulse;

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) return;
        try {
            ContextCompat.startForegroundService(context,
                    new Intent(context, StudyOverlayBubbleService.class));
        } catch (RuntimeException ignored) { }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, StudyOverlayBubbleService.class));
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        Intent open = openIntent();
        PendingIntent pending = PendingIntent.getActivity(this, 71, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        startForeground(47021, new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("AI Study Assistant")
                .setContentText("Floating study access is active")
                .setContentIntent(pending).setOngoing(true).build());
        if (!signedIn() || (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this))) {
            stopSelf(); return;
        }
        showBubble();
    }

    private void showBubble() {
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubble = new TextView(this);
        bubble.setText("🎓 AI");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(13f);
        bubble.setGravity(Gravity.CENTER);
        bubble.setElevation(dp(12));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(48, 80, 205), Color.rgb(13, 148, 158)});
        bg.setShape(GradientDrawable.OVAL);
        bg.setStroke(dp(2), Color.argb(170, 220, 235, 255));
        bubble.setBackground(bg);
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(dp(68), dp(68), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = getResources().getDisplayMetrics().widthPixels - dp(82);
        params.y = Math.round(getResources().getDisplayMetrics().heightPixels * .62f);
        bubble.setOnTouchListener(new DragTouch());
        manager.addView(bubble, params);
        pulse = ObjectAnimator.ofFloat(bubble, View.SCALE_X, 1f, 1.08f, 1f);
        pulse.setDuration(1700L);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();
    }

    private Intent openIntent() {
        return new Intent(this, DashboardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SmartAiCompanionController.EXTRA_OPEN_COMPANION, true);
    }

    private final class DragTouch implements View.OnTouchListener {
        int sx, sy; float dx, dy; boolean moved;
        @Override public boolean onTouch(View v, MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                sx=params.x; sy=params.y; dx=e.getRawX(); dy=e.getRawY(); moved=false; return true;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                int mx=Math.round(e.getRawX()-dx), my=Math.round(e.getRawY()-dy);
                moved |= Math.abs(mx)>dp(5) || Math.abs(my)>dp(5);
                params.x=Math.max(0,sx+mx); params.y=Math.max(dp(24),sy+my);
                manager.updateViewLayout(bubble,params); return true;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                if (!moved) startActivity(openIntent()); return true;
            }
            return false;
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm=getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"AI Study Overlay",
                NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(channel);
    }

    private boolean signedIn() {
        try { return FirebaseAuth.getInstance().getCurrentUser()!=null; }
        catch (RuntimeException e) { return false; }
    }
    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
    @Override public void onDestroy(){
        if(pulse!=null)pulse.cancel();
        if(bubble!=null&&manager!=null)try{manager.removeView(bubble);}catch(RuntimeException ignored){}
        super.onDestroy();
    }
    @Nullable @Override public IBinder onBind(Intent intent){return null;}
}
