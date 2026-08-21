package com.tridev.studysaathi;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.tridev.studysaathi.databinding.ActivityWhiteboardBinding;

/** Full-screen interactive practice whiteboard. */
public final class WhiteboardActivity extends AppCompatActivity {
    private ActivityWhiteboardBinding binding;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWhiteboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
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
        binding.buttonClear.setOnClickListener(v -> binding.studyWhiteboard.clearBoard());
    }
}
