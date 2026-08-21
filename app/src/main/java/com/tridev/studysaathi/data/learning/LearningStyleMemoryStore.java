package com.tridev.studysaathi.data.learning;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/** Isolated per-profile style memory; it does not alter Room, Firebase, or the knowledge graph. */
public final class LearningStyleMemoryStore {
    private static final String PREFS = "student_learning_style_memory_v1";
    private static final int MIN_SIGNALS_FOR_MEMORY = 2;
    private static final Object LOCK = new Object();
    @NonNull private final SharedPreferences preferences;

    public LearningStyleMemoryStore(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public LearningStylePreference.Style recordAndResolve(long profileId,
                                                           @NonNull String question) {
        LearningStylePreference.Style current = LearningStylePreference.detect(question);
        if (profileId <= 0) return current;
        synchronized (LOCK) {
            if (current != LearningStylePreference.Style.BALANCED) {
                String key = key(profileId, current);
                preferences.edit().putInt(key, preferences.getInt(key, 0) + 1).apply();
                return current;
            }
            LearningStylePreference.Style best = LearningStylePreference.Style.BALANCED;
            int bestCount = 0;
            for (LearningStylePreference.Style style : LearningStylePreference.Style.values()) {
                if (style == LearningStylePreference.Style.BALANCED) continue;
                int count = preferences.getInt(key(profileId, style), 0);
                if (count > bestCount) {
                    best = style;
                    bestCount = count;
                }
            }
            return bestCount >= MIN_SIGNALS_FOR_MEMORY
                    ? best : LearningStylePreference.Style.BALANCED;
        }
    }

    private static String key(long profileId, LearningStylePreference.Style style) {
        return "profile_" + profileId + "_" + style.name();
    }
}
