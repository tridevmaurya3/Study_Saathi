package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Locale;

/** Stores privacy-safe start/completion counts for recommended revisions. */
public final class RecommendedRevisionProgressStore {
    private static final String PREFS = "recommended_revision_progress";
    @NonNull private final SharedPreferences preferences;

    public RecommendedRevisionProgressStore(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void recordStarted(@NonNull String subject, @NonNull String chapter) {
        increment(key(subject, chapter, "started"));
    }

    public void recordCompleted(@NonNull String subject, @NonNull String chapter) {
        increment(key(subject, chapter, "completed"));
    }

    @NonNull
    public Summary getSummary(@NonNull String subject, @NonNull String chapter) {
        return Summary.fromCounts(
                preferences.getInt(key(subject, chapter, "started"), 0),
                preferences.getInt(key(subject, chapter, "completed"), 0));
    }

    private synchronized void increment(@NonNull String key) {
        preferences.edit().putInt(key, preferences.getInt(key, 0) + 1).apply();
    }

    @NonNull
    private static String key(@NonNull String subject, @NonNull String chapter,
                              @NonNull String suffix) {
        String scope = subject.trim().toLowerCase(Locale.ROOT) + "\n"
                + chapter.trim().toLowerCase(Locale.ROOT);
        return "revision_" + Integer.toHexString(scope.hashCode()) + "_" + suffix;
    }

    public static final class Summary {
        private final int started;
        private final int completed;

        private Summary(int started, int completed) {
            this.started = Math.max(0, started);
            this.completed = Math.max(0, Math.min(completed, this.started));
        }

        @NonNull
        public static Summary fromCounts(int started, int completed) {
            return new Summary(started, completed);
        }

        public int getStarted() { return started; }
        public int getCompleted() { return completed; }
        public int getPending() { return Math.max(0, started - completed); }
        public int getCompletionPercent() {
            return started == 0 ? 0 : Math.round((completed * 100f) / started);
        }
    }
}
