package com.tridev.studysaathi.overlay;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.tridev.studysaathi.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Captures exactly one user-approved frame and immediately ends projection. */
public final class StudyLensCaptureService extends Service {
    static final String EXTRA_RESULT_CODE = "study_lens_result_code";
    static final String EXTRA_RESULT_DATA = "study_lens_result_data";
    static final String EXTRA_CAPTURE_PATH = "study_lens_capture_path";
    static final String ACTION_REVIEW = "com.tridev.studysaathi.overlay.REVIEW_CAPTURE";
    static final String ACTION_CAPTURE_ERROR = "com.tridev.studysaathi.overlay.CAPTURE_ERROR";
    private static final String CHANNEL = "study_lens_capture";
    private static final int NOTIFICATION_ID = 47022;

    @Nullable private MediaProjection projection;
    @Nullable private VirtualDisplay virtualDisplay;
    @Nullable private ImageReader imageReader;
    @Nullable private HandlerThread captureThread;
    private boolean frameHandled;
    private String pendingQuestion = "";

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Study Lens")
                .setContentText("एक screen frame तैयार किया जा रहा है")
                .setOngoing(true)
                .build());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        pendingQuestion = safe(intent.getStringExtra(
                StudyOverlayBubbleService.EXTRA_PENDING_QUESTION));
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData = parcelableIntent(intent, EXTRA_RESULT_DATA);
        if (resultCode == 0 || resultData == null) {
            finishWithError();
            return START_NOT_STICKY;
        }
        try {
            MediaProjectionManager manager = (MediaProjectionManager)
                    getSystemService(MEDIA_PROJECTION_SERVICE);
            projection = manager.getMediaProjection(resultCode, resultData);
            if (projection == null) {
                finishWithError();
                return START_NOT_STICKY;
            }
            captureOneFrame();
        } catch (RuntimeException error) {
            finishWithError();
        }
        return START_NOT_STICKY;
    }

    private void captureOneFrame() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;
        if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Rect bounds = ((WindowManager) getSystemService(WINDOW_SERVICE))
                    .getMaximumWindowMetrics().getBounds();
            width = bounds.width();
            height = bounds.height();
        }
        captureThread = new HandlerThread("StudyLensOneFrame");
        captureThread.start();
        Handler handler = new Handler(captureThread.getLooper());
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { releaseCapture(); }
        }, handler);
        final int captureWidth = width;
        final int captureHeight = height;
        imageReader.setOnImageAvailableListener(reader -> {
            if (frameHandled) return;
            Image image = reader.acquireLatestImage();
            if (image == null) return;
            frameHandled = true;
            try {
                File file = writeImage(image, captureWidth, captureHeight);
                launchReview(file);
            } catch (IOException | RuntimeException error) {
                finishWithError();
            } finally {
                image.close();
                stopProjectionAndSelf();
            }
        }, handler);
        virtualDisplay = projection.createVirtualDisplay(
                "StudyLensOneFrame", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, handler);
        handler.postDelayed(() -> {
            if (!frameHandled) {
                frameHandled = true;
                finishWithError();
                stopProjectionAndSelf();
            }
        }, 3500L);
    }

    @NonNull private File writeImage(@NonNull Image image, int width, int height)
            throws IOException {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int paddedWidth = width + (rowStride - pixelStride * width) / pixelStride;
        Bitmap padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        Bitmap exact = Bitmap.createBitmap(padded, 0, 0, width, height);
        if (exact != padded) padded.recycle();
        File directory = new File(getCacheDir(), "book_cover_cache/study_lens");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("No cache directory");
        File file = File.createTempFile("study_lens_", ".png", directory);
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!exact.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("Capture compression failed");
            }
        } finally {
            exact.recycle();
        }
        return file;
    }

    private void launchReview(@NonNull File file) {
        startActivity(new Intent(this, OverlayScreenCaptureActivity.class)
                .setAction(ACTION_REVIEW)
                .putExtra(EXTRA_CAPTURE_PATH, file.getAbsolutePath())
                .putExtra(StudyOverlayBubbleService.EXTRA_PENDING_QUESTION, pendingQuestion)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    private void finishWithError() {
        try {
            startActivity(new Intent(this, OverlayScreenCaptureActivity.class)
                    .setAction(ACTION_CAPTURE_ERROR)
                    .putExtra(StudyOverlayBubbleService.EXTRA_PENDING_QUESTION, pendingQuestion)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (RuntimeException ignored) { }
    }

    private void stopProjectionAndSelf() {
        MediaProjection active = projection;
        projection = null;
        if (active != null) active.stop();
        releaseCapture();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void releaseCapture() {
        if (virtualDisplay != null) virtualDisplay.release();
        virtualDisplay = null;
        if (imageReader != null) imageReader.close();
        imageReader = null;
        if (captureThread != null) captureThread.quitSafely();
        captureThread = null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Study Lens capture", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public void onDestroy() {
        MediaProjection active = projection;
        projection = null;
        if (active != null) {
            try { active.stop(); } catch (RuntimeException ignored) { }
        }
        releaseCapture();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Nullable private static Intent parcelableIntent(@NonNull Intent source,
                                                     @NonNull String key) {
        if (Build.VERSION.SDK_INT >= 33) return source.getParcelableExtra(key, Intent.class);
        return source.getParcelableExtra(key);
    }

    @NonNull private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }
}
