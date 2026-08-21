package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/** Stores privacy-safe aggregate citation outcomes for parent insights. */
public final class CitationCoverageHistoryStore {
    private static final String PREFS = "citation_coverage_history";
    private static final String KEY_REVIEWED = "reviewed";
    private static final String KEY_GROUNDED = "grounded";
    private static final String KEY_MISSING = "missing";
    private static final String KEY_BLOCKED = "blocked";

    @NonNull private final SharedPreferences preferences;

    public CitationCoverageHistoryStore(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void record(@NonNull BookAnswerGroundingValidator.Result result) {
        if (result.getStatus() == BookAnswerGroundingValidator.Status.NO_EXACT_EVIDENCE) return;
        int reviewed = preferences.getInt(KEY_REVIEWED, 0) + 1;
        int grounded = preferences.getInt(KEY_GROUNDED, 0);
        int missing = preferences.getInt(KEY_MISSING, 0);
        int blocked = preferences.getInt(KEY_BLOCKED, 0);
        if (result.isGrounded()) grounded++;
        else if (result.needsCitationCaution()) missing++;
        else if (result.hasUnsupportedCitation()) blocked++;
        preferences.edit()
                .putInt(KEY_REVIEWED, reviewed)
                .putInt(KEY_GROUNDED, grounded)
                .putInt(KEY_MISSING, missing)
                .putInt(KEY_BLOCKED, blocked)
                .apply();
    }

    @NonNull
    public Summary getSummary() {
        return Summary.fromCounts(
                preferences.getInt(KEY_REVIEWED, 0),
                preferences.getInt(KEY_GROUNDED, 0),
                preferences.getInt(KEY_MISSING, 0),
                preferences.getInt(KEY_BLOCKED, 0));
    }

    public static final class Summary {
        private final int reviewed;
        private final int grounded;
        private final int missing;
        private final int blocked;

        private Summary(int reviewed, int grounded, int missing, int blocked) {
            this.reviewed = Math.max(0, reviewed);
            this.grounded = Math.max(0, Math.min(grounded, this.reviewed));
            this.missing = Math.max(0, missing);
            this.blocked = Math.max(0, blocked);
        }

        @NonNull
        public static Summary fromCounts(int reviewed, int grounded, int missing, int blocked) {
            return new Summary(reviewed, grounded, missing, blocked);
        }

        public int getReviewed() { return reviewed; }
        public int getGrounded() { return grounded; }
        public int getMissing() { return missing; }
        public int getBlocked() { return blocked; }
        public int getAttentionNeeded() { return missing + blocked; }
        public int getCoveragePercent() {
            return reviewed == 0 ? 0 : Math.round((grounded * 100f) / reviewed);
        }
    }
}
