package com.tridev.studysaathi.data.learning;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Small local, profile-device mistake notebook populated by practice answers. */
public final class MistakeNotebookStore {
    private static final String PREFS = "study_saathi_mistake_notebook";
    private static final String KEY_ITEMS = "items";
    private static final int LIMIT = 60;
    private final SharedPreferences preferences;

    public MistakeNotebookStore(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void record(String subject, String chapter, String question, String explanation) {
        JSONArray old = readArray();
        JSONArray next = new JSONArray();
        JSONObject item = new JSONObject();
        try {
            item.put("subject", safe(subject)); item.put("chapter", safe(chapter));
            item.put("question", safe(question)); item.put("explanation", safe(explanation));
            item.put("time", System.currentTimeMillis()); next.put(item);
            for (int i = 0; i < old.length() && next.length() < LIMIT; i++) next.put(old.optJSONObject(i));
            preferences.edit().putString(KEY_ITEMS, next.toString()).apply();
        } catch (Exception ignored) { }
    }

    @NonNull public List<Entry> getEntries() {
        JSONArray array = readArray(); List<Entry> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i); if (item == null) continue;
            result.add(new Entry(item.optString("subject"), item.optString("chapter"),
                    item.optString("question"), item.optString("explanation")));
        }
        return result;
    }

    public void clear() { preferences.edit().remove(KEY_ITEMS).apply(); }
    private JSONArray readArray() { try { return new JSONArray(preferences.getString(KEY_ITEMS, "[]")); } catch (Exception e) { return new JSONArray(); } }
    private static String safe(String text) { return text == null ? "" : text.trim(); }

    public static final class Entry {
        public final String subject, chapter, question, explanation;
        Entry(String subject, String chapter, String question, String explanation) {
            this.subject = subject; this.chapter = chapter; this.question = question; this.explanation = explanation;
        }
    }
}
