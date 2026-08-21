package com.tridev.studysaathi;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.tridev.studysaathi.data.learning.MistakeNotebookStore;
import com.tridev.studysaathi.databinding.ActivityMistakeNotebookBinding;

import java.util.List;

public final class MistakeNotebookActivity extends AppCompatActivity {
    private ActivityMistakeNotebookBinding binding;
    private MistakeNotebookStore store;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMistakeNotebookBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        store = new MistakeNotebookStore(this);
        binding.buttonBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.buttonClearMistakes.setOnClickListener(v -> { store.clear(); render(); });
    }

    @Override protected void onResume() { super.onResume(); render(); }

    private void render() {
        List<MistakeNotebookStore.Entry> entries = store.getEntries();
        binding.containerMistakes.removeAllViews();
        binding.textMistakeEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        binding.buttonClearMistakes.setEnabled(!entries.isEmpty());
        for (MistakeNotebookStore.Entry entry : entries) {
            MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(18)); card.setCardElevation(0);
            card.setCardBackgroundColor(getColor(R.color.ss_red_soft)); card.setStrokeColor(getColor(R.color.ss_red_border)); card.setStrokeWidth(dp(1));
            TextView text = new TextView(this); text.setPadding(dp(16), dp(14), dp(16), dp(14)); text.setTextColor(getColor(R.color.ss_text_primary));
            text.setTextSize(14); text.setText(entry.subject + " • " + entry.chapter + "\n\n" + entry.question + "\n\nसही समझ: " + entry.explanation);
            card.addView(text); android.widget.LinearLayout.LayoutParams p = new android.widget.LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(7); card.setLayoutParams(p);
            binding.containerMistakes.addView(card);
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
