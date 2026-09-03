package com.tridev.studysaathi.overlay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Explicit-consent, one-frame screen chooser for the floating Study Saathi. */
public final class OverlayScreenCaptureActivity extends ComponentActivity {
    private final ActivityResultLauncher<Intent> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != Activity.RESULT_OK
                                || result.getData() == null) {
                            cancelAndFinish();
                            return;
                        }
                        startCaptureService(result.getResultCode(), result.getData());
                    });

    private String pendingQuestion = "";
    @Nullable private Bitmap capture;
    @Nullable private CropImageView cropView;
    @Nullable private String sourcePath;
    private boolean handedOff;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pendingQuestion = safe(getIntent().getStringExtra(
                StudyOverlayBubbleService.EXTRA_PENDING_QUESTION));
        String action = getIntent().getAction();
        if (StudyLensCaptureService.ACTION_REVIEW.equals(action)) {
            sourcePath = getIntent().getStringExtra(StudyLensCaptureService.EXTRA_CAPTURE_PATH);
            showReview();
        } else if (StudyLensCaptureService.ACTION_CAPTURE_ERROR.equals(action)) {
            Toast.makeText(this,
                    "यह screen capture नहीं हो सकी। Secure screen या दोबारा अनुमति जाँचें।",
                    Toast.LENGTH_LONG).show();
            cancelAndFinish();
        } else if (savedInstanceState == null) {
            requestCapturePermission();
        }
    }

    @Override protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingQuestion = safe(intent.getStringExtra(
                StudyOverlayBubbleService.EXTRA_PENDING_QUESTION));
        if (StudyLensCaptureService.ACTION_REVIEW.equals(intent.getAction())) {
            sourcePath = intent.getStringExtra(StudyLensCaptureService.EXTRA_CAPTURE_PATH);
            showReview();
        } else if (StudyLensCaptureService.ACTION_CAPTURE_ERROR.equals(intent.getAction())) {
            handedOff = false;
            Toast.makeText(this,
                    "यह screen capture नहीं हो सकी। Secure screen या दोबारा अनुमति जाँचें।",
                    Toast.LENGTH_LONG).show();
            cancelAndFinish();
        }
    }

    private void requestCapturePermission() {
        try {
            MediaProjectionManager manager = (MediaProjectionManager)
                    getSystemService(MEDIA_PROJECTION_SERVICE);
            permissionLauncher.launch(manager.createScreenCaptureIntent());
        } catch (RuntimeException error) {
            Toast.makeText(this, "Screen share अभी उपलब्ध नहीं है।", Toast.LENGTH_LONG).show();
            cancelAndFinish();
        }
    }

    private void startCaptureService(int resultCode, @NonNull Intent resultData) {
        handedOff = true;
        Intent service = new Intent(this, StudyLensCaptureService.class)
                .putExtra(StudyLensCaptureService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(StudyLensCaptureService.EXTRA_RESULT_DATA, resultData)
                .putExtra(StudyOverlayBubbleService.EXTRA_PENDING_QUESTION, pendingQuestion);
        try {
            ContextCompat.startForegroundService(this, service);
            // Keep this transparent activity in front until the one-frame capture
            // returns. The application lifecycle therefore keeps the old floating
            // bubble stopped, so it cannot appear inside the captured image.
        } catch (RuntimeException error) {
            handedOff = false;
            Toast.makeText(this, "Screen capture शुरू नहीं हो सका।", Toast.LENGTH_LONG).show();
            cancelAndFinish();
        }
    }

    private void showReview() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (sourcePath == null) {
            cancelAndFinish();
            return;
        }
        capture = BitmapFactory.decodeFile(sourcePath);
        if (capture == null || isMostlyBlank(capture)) {
            Toast.makeText(this,
                    "इस screen ने capture को सुरक्षित रूप से block किया है।",
                    Toast.LENGTH_LONG).show();
            deleteSource();
            cancelAndFinish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(Color.rgb(10, 18, 36));

        TextView title = new TextView(this);
        title.setText("Study Lens • जिस हिस्से पर पूछना है उसे चुनें");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(4), dp(4), dp(4), dp(10));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cropView = new CropImageView(this, capture);
        root.addView(cropView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView hint = new TextView(this);
        hint.setText("नीले box को खींचकर बदलें। केवल चुनी image AI को भेजी जाएगी।");
        hint.setTextColor(Color.rgb(205, 216, 238));
        hint.setTextSize(12);
        hint.setPadding(dp(4), dp(8), dp(4), dp(6));
        root.addView(hint);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        Button cancel = button("रद्द", Color.rgb(62, 72, 94));
        Button full = button("पूरा screen", Color.rgb(38, 91, 190));
        Button crop = button("चुना भाग", Color.rgb(18, 132, 110));
        actions.addView(cancel, weighted());
        actions.addView(full, spacedWeighted());
        actions.addView(crop, spacedWeighted());
        root.addView(actions);

        cancel.setOnClickListener(view -> {
            deleteSource();
            cancelAndFinish();
        });
        full.setOnClickListener(view -> deliver(capture));
        crop.setOnClickListener(view -> deliver(cropView.createCrop()));
        setContentView(root);
    }

    private void deliver(@Nullable Bitmap selected) {
        if (selected == null) {
            Toast.makeText(this, "पहले screen का हिस्सा चुनें।", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File directory = new File(getCacheDir(), "book_cover_cache/study_lens");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("No cache directory");
            File output = File.createTempFile("study_lens_question_", ".jpg", directory);
            try (FileOutputStream stream = new FileOutputStream(output)) {
                if (!selected.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                    throw new IOException("Image compression failed");
                }
            }
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", output);
            Intent service = new Intent(this, StudyOverlayBubbleService.class)
                    .setAction(StudyOverlayBubbleService.ACTION_PHOTO_RESULT)
                    .putExtra(StudyOverlayBubbleService.EXTRA_PHOTO_URI, uri.toString())
                    .putExtra(StudyOverlayBubbleService.EXTRA_SCREEN_SOURCE, true)
                    .putExtra(StudyOverlayBubbleService.EXTRA_PENDING_QUESTION, pendingQuestion);
            handedOff = true;
            ContextCompat.startForegroundService(this, service);
            deleteSource();
            finish();
        } catch (IOException | RuntimeException error) {
            Toast.makeText(this, "चुना हुआ हिस्सा तैयार नहीं हो सका।", Toast.LENGTH_LONG).show();
        } finally {
            if (selected != capture && selected != null) selected.recycle();
        }
    }

    private void cancelAndFinish() {
        if (!handedOff) {
            try {
                ContextCompat.startForegroundService(this,
                        new Intent(this, StudyOverlayBubbleService.class)
                                .setAction(StudyOverlayBubbleService.ACTION_SCREEN_CANCELLED)
                                .putExtra(StudyOverlayBubbleService.EXTRA_PENDING_QUESTION,
                                        pendingQuestion));
            } catch (RuntimeException ignored) { }
        }
        handedOff = true;
        finish();
    }

    @Override public void onBackPressed() {
        deleteSource();
        cancelAndFinish();
    }

    @Override protected void onDestroy() {
        if (capture != null) capture.recycle();
        capture = null;
        super.onDestroy();
    }

    private void deleteSource() {
        if (sourcePath == null) return;
        try { new File(sourcePath).delete(); } catch (RuntimeException ignored) { }
        sourcePath = null;
    }

    private boolean isMostlyBlank(@NonNull Bitmap bitmap) {
        int darkOrTransparent = 0;
        int samples = 0;
        int xStep = Math.max(1, bitmap.getWidth() / 24);
        int yStep = Math.max(1, bitmap.getHeight() / 24);
        for (int y = 0; y < bitmap.getHeight(); y += yStep) {
            for (int x = 0; x < bitmap.getWidth(); x += xStep) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.alpha(pixel) < 20
                        || (Color.red(pixel) < 8 && Color.green(pixel) < 8
                        && Color.blue(pixel) < 8)) darkOrTransparent++;
                samples++;
            }
        }
        return samples > 0 && darkOrTransparent >= Math.round(samples * .97f);
    }

    @NonNull private Button button(@NonNull String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setAllCaps(false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(12));
        button.setBackground(background);
        return button;
    }

    @NonNull private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, dp(50), 1f);
    }

    @NonNull private LinearLayout.LayoutParams spacedWeighted() {
        LinearLayout.LayoutParams params = weighted();
        params.leftMargin = dp(7);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @NonNull private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static final class CropImageView extends View {
        private static final float MIN_SIZE = 90f;
        @NonNull private final Bitmap bitmap;
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint shadePaint = new Paint();
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF imageBounds = new RectF();
        private final RectF selection = new RectF();
        private float downX;
        private float downY;
        private int dragMode;

        CropImageView(@NonNull Context context, @NonNull Bitmap bitmap) {
            super(context);
            this.bitmap = bitmap;
            shadePaint.setColor(Color.argb(145, 0, 0, 0));
            borderPaint.setColor(Color.rgb(74, 153, 255));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 3f);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            float scale = Math.min(width / (float) bitmap.getWidth(),
                    height / (float) bitmap.getHeight());
            float shownWidth = bitmap.getWidth() * scale;
            float shownHeight = bitmap.getHeight() * scale;
            float left = (width - shownWidth) / 2f;
            float top = (height - shownHeight) / 2f;
            imageBounds.set(left, top, left + shownWidth, top + shownHeight);
            selection.set(imageBounds.left + shownWidth * .08f,
                    imageBounds.top + shownHeight * .2f,
                    imageBounds.right - shownWidth * .08f,
                    imageBounds.bottom - shownHeight * .2f);
        }

        @Override protected void onDraw(@NonNull Canvas canvas) {
            canvas.drawBitmap(bitmap, null, imageBounds, imagePaint);
            canvas.drawRect(imageBounds.left, imageBounds.top, imageBounds.right,
                    selection.top, shadePaint);
            canvas.drawRect(imageBounds.left, selection.bottom, imageBounds.right,
                    imageBounds.bottom, shadePaint);
            canvas.drawRect(imageBounds.left, selection.top, selection.left,
                    selection.bottom, shadePaint);
            canvas.drawRect(selection.right, selection.top, imageBounds.right,
                    selection.bottom, shadePaint);
            canvas.drawRect(selection, borderPaint);
        }

        @Override public boolean onTouchEvent(@NonNull MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                downX = x;
                downY = y;
                float edge = 42f * getResources().getDisplayMetrics().density;
                if (Math.abs(x - selection.left) < edge) dragMode |= 1;
                if (Math.abs(x - selection.right) < edge) dragMode |= 2;
                if (Math.abs(y - selection.top) < edge) dragMode |= 4;
                if (Math.abs(y - selection.bottom) < edge) dragMode |= 8;
                if (dragMode == 0 && selection.contains(x, y)) dragMode = 16;
                return dragMode != 0;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE && dragMode != 0) {
                float dx = x - downX;
                float dy = y - downY;
                if (dragMode == 16) {
                    dx = Math.max(imageBounds.left - selection.left,
                            Math.min(imageBounds.right - selection.right, dx));
                    dy = Math.max(imageBounds.top - selection.top,
                            Math.min(imageBounds.bottom - selection.bottom, dy));
                    selection.offset(dx, dy);
                } else {
                    if ((dragMode & 1) != 0) selection.left = Math.max(imageBounds.left,
                            Math.min(selection.right - MIN_SIZE, selection.left + dx));
                    if ((dragMode & 2) != 0) selection.right = Math.min(imageBounds.right,
                            Math.max(selection.left + MIN_SIZE, selection.right + dx));
                    if ((dragMode & 4) != 0) selection.top = Math.max(imageBounds.top,
                            Math.min(selection.bottom - MIN_SIZE, selection.top + dy));
                    if ((dragMode & 8) != 0) selection.bottom = Math.min(imageBounds.bottom,
                            Math.max(selection.top + MIN_SIZE, selection.bottom + dy));
                }
                downX = x;
                downY = y;
                invalidate();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                dragMode = 0;
                return true;
            }
            return super.onTouchEvent(event);
        }

        @Nullable Bitmap createCrop() {
            if (imageBounds.width() <= 0 || imageBounds.height() <= 0) return null;
            float scaleX = bitmap.getWidth() / imageBounds.width();
            float scaleY = bitmap.getHeight() / imageBounds.height();
            int left = Math.max(0, Math.round((selection.left - imageBounds.left) * scaleX));
            int top = Math.max(0, Math.round((selection.top - imageBounds.top) * scaleY));
            int right = Math.min(bitmap.getWidth(),
                    Math.round((selection.right - imageBounds.left) * scaleX));
            int bottom = Math.min(bitmap.getHeight(),
                    Math.round((selection.bottom - imageBounds.top) * scaleY));
            if (right <= left || bottom <= top) return null;
            return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
        }
    }
}
