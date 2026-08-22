package com.tridev.studysaathi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tridev.studysaathi.databinding.ActivityWhiteboardBinding;
import com.tridev.studysaathi.data.learning.WhiteboardMathEvaluator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

/** Smart handwriting whiteboard: dual OCR preview plus verified Study Saathi solving. */
public final class WhiteboardActivity extends AppCompatActivity {
    private ActivityWhiteboardBinding binding;
    private TextRecognizer latinRecognizer;
    private TextRecognizer devanagariRecognizer;
    private boolean recognitionRunning;
    private final Handler handwritingHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRecognize = () -> recognizeBoard(false);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWhiteboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        devanagariRecognizer = TextRecognition.getClient(
                new DevanagariTextRecognizerOptions.Builder().build());

        binding.buttonBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.buttonPen.setOnClickListener(v -> {
            binding.studyWhiteboard.setEraser(false);
            binding.textWhiteboardMode.setText("Pen mode • लिखना शुरू करें");
        });
        binding.buttonEraser.setOnClickListener(v -> {
            binding.studyWhiteboard.setEraser(true);
            binding.textWhiteboardMode.setText("Eraser mode • हटाने के लिए draw करें");
        });
        binding.buttonUndo.setOnClickListener(v -> binding.studyWhiteboard.undo());
        binding.buttonClear.setOnClickListener(v -> {
            handwritingHandler.removeCallbacks(autoRecognize);
            binding.studyWhiteboard.clearBoard();
            binding.editDetectedQuestion.setText("");
            binding.textWhiteboardCanvasAnswer.setVisibility(View.GONE);
            binding.textRecognitionStatus.setText("Whiteboard पर लिखें • Auto recognition चालू है");
        });
        binding.buttonRecognize.setOnClickListener(v -> recognizeBoard(false));
        binding.buttonSolve.setOnClickListener(v -> solveWithStudySaathi());
        binding.studyWhiteboard.setOnStrokeCompleteListener(() -> {
            handwritingHandler.removeCallbacks(autoRecognize);
            binding.textRecognitionStatus.setText("लिखना पूरा करें • Auto recognition तैयार है…");
            handwritingHandler.postDelayed(autoRecognize, 950L);
        });
    }

    private void recognizeBoard(boolean solveAfter) {
        if (recognitionRunning) return;
        if (binding.studyWhiteboard.isBlank()) {
            Snackbar.make(binding.getRoot(), "पहले whiteboard पर सवाल लिखें।", Snackbar.LENGTH_SHORT).show();
            return;
        }
        recognitionRunning = true;
        setBusy(true, "Latin + Devanagari handwriting पढ़ी जा रही है…");
        Bitmap bitmap = binding.studyWhiteboard.createBitmap();
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        latinRecognizer.process(image)
                .addOnSuccessListener(latin -> recognizeDevanagari(
                        image, safe(latin.getText()), bitmap, solveAfter))
                .addOnFailureListener(error -> recognizeDevanagari(
                        image, "", bitmap, solveAfter));
    }

    private void recognizeDevanagari(InputImage image, String latinText,
                                     Bitmap bitmap, boolean solveAfter) {
        devanagariRecognizer.process(image)
                .addOnSuccessListener(devanagari -> finishRecognition(
                        chooseBestText(latinText, devanagari.getText()), bitmap, solveAfter))
                .addOnFailureListener(error -> finishRecognition(latinText, bitmap, solveAfter));
    }

    private void finishRecognition(String detected, Bitmap bitmap, boolean solveAfter) {
        recognitionRunning = false;
        String quickAnswer = WhiteboardMathEvaluator.evaluate(detected);
        setBusy(false, detected.isEmpty()
                ? "Handwriting साफ नहीं पढ़ी गई। थोड़ा बड़ा और स्पष्ट लिखकर फिर कोशिश करें।"
                : quickAnswer.isEmpty()
                ? "OCR preview • Text जाँचें और जरूरत हो तो edit करें।"
                : "Smart Calculator: " + detected + " = " + quickAnswer
                        + " • Step explanation के लिए Solve with AI दबाएँ।");
        if (!detected.isEmpty()) binding.editDetectedQuestion.setText(detected);
        if (!quickAnswer.isEmpty()) {
            binding.textWhiteboardCanvasAnswer.setText(detected + "  =  " + quickAnswer);
            binding.textWhiteboardCanvasAnswer.setVisibility(View.VISIBLE);
        } else if (!detected.isEmpty()) {
            binding.textWhiteboardCanvasAnswer.setText(
                    "पहचाना: " + detected + "\n✦ से smart answer खोलें");
            binding.textWhiteboardCanvasAnswer.setVisibility(View.VISIBLE);
        }
        if (solveAfter && !detected.isEmpty()) openTutor(bitmap, detected);
        else bitmap.recycle();
    }

    private void solveWithStudySaathi() {
        if (binding.studyWhiteboard.isBlank()) {
            Snackbar.make(binding.getRoot(), "पहले whiteboard पर सवाल लिखें।", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String edited = safe(binding.editDetectedQuestion.getText() == null
                ? "" : binding.editDetectedQuestion.getText().toString());
        if (edited.isEmpty()) {
            recognizeBoard(true);
            return;
        }
        openTutor(binding.studyWhiteboard.createBitmap(), edited);
    }

    private void openTutor(@NonNull Bitmap bitmap, @NonNull String detectedText) {
        try {
            File directory = new File(getCacheDir(), "book_cover_cache/question_images");
            if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Image folder unavailable");
            File file = File.createTempFile("whiteboard_", ".png", directory);
            try (FileOutputStream output = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                    throw new IllegalStateException("Whiteboard image could not be saved");
            }
            bitmap.recycle();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(this, AskStudySaathiActivity.class);
            intent.putExtra(AskStudySaathiActivity.EXTRA_PREFILL_QUESTION,
                    "Whiteboard पर लिखा प्रश्न: " + detectedText
                            + "\nOriginal handwriting image attached है। Subject और intent पहचानकर answer verify करें। "
                            + "Maths हो तो calculation step-by-step दिखाएँ; अन्य subject हो तो class-level explanation दें।");
            intent.putExtra(AskStudySaathiActivity.EXTRA_PREFILL_IMAGE_URI, uri.toString());
            intent.putExtra(AskStudySaathiActivity.EXTRA_PREFILL_IMAGE_PRIVATE_PATH, file.getAbsolutePath());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception error) {
            if (!bitmap.isRecycled()) bitmap.recycle();
            Snackbar.make(binding.getRoot(), "Whiteboard question तैयार नहीं हो सका। दोबारा कोशिश करें।",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void setBusy(boolean busy, String message) {
        binding.progressWhiteboardRecognition.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.buttonRecognize.setEnabled(!busy);
        binding.buttonSolve.setEnabled(!busy);
        binding.textRecognitionStatus.setText(message);
    }

    private static String chooseBestText(String latin, String devanagari) {
        String first = safe(latin), second = safe(devanagari);
        if (first.isEmpty()) return second;
        if (second.isEmpty()) return first;
        boolean secondHasDevanagari = second.matches(".*[\\u0900-\\u097F].*");
        boolean firstHasDevanagari = first.matches(".*[\\u0900-\\u097F].*");
        if (secondHasDevanagari && !firstHasDevanagari) return second;
        return second.length() > first.length() ? second : first;
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim().replaceAll("[\\t ]+", " ");
    }

    @Override protected void onDestroy() {
        handwritingHandler.removeCallbacksAndMessages(null);
        if (latinRecognizer != null) latinRecognizer.close();
        if (devanagariRecognizer != null) devanagariRecognizer.close();
        super.onDestroy();
    }
}
