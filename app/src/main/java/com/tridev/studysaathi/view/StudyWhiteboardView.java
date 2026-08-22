package com.tridev.studysaathi.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Lightweight touch whiteboard with undo, clear and pen/eraser modes. */
public final class StudyWhiteboardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Stroke> strokes = new ArrayList<>();
    private Path activePath;
    private boolean eraser;
    @Nullable private OnStrokeCompleteListener strokeCompleteListener;

    public interface OnStrokeCompleteListener {
        void onStrokeComplete();
    }

    public StudyWhiteboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setBackgroundColor(Color.WHITE);
    }

    public void setEraser(boolean enabled) { eraser = enabled; }
    public void setOnStrokeCompleteListener(@Nullable OnStrokeCompleteListener listener) {
        strokeCompleteListener = listener;
    }
    public void undo() { if (!strokes.isEmpty()) { strokes.remove(strokes.size() - 1); invalidate(); } }
    public void clearBoard() { strokes.clear(); activePath = null; invalidate(); }
    public boolean isBlank() { return strokes.isEmpty(); }

    public Bitmap createBitmap() {
        int safeWidth = Math.max(1, getWidth());
        int safeHeight = Math.max(1, getHeight());
        Bitmap bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Stroke stroke : strokes) {
            paint.setColor(stroke.erase ? Color.WHITE : Color.rgb(31, 54, 96));
            paint.setStrokeWidth(stroke.erase ? 34f : 7f);
            canvas.drawPath(stroke.path, paint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            activePath = new Path();
            activePath.moveTo(x, y);
            strokes.add(new Stroke(activePath, eraser));
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE && activePath != null) {
            activePath.lineTo(x, y);
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            activePath = null;
            performClick();
            if (strokeCompleteListener != null) strokeCompleteListener.onStrokeComplete();
            return true;
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    private static final class Stroke {
        final Path path; final boolean erase;
        Stroke(Path path, boolean erase) { this.path = path; this.erase = erase; }
    }
}
