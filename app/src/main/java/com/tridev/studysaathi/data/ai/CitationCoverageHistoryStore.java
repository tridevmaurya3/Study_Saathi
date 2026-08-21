package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Stores privacy-safe aggregate citation outcomes for parent insights. */
public final class CitationCoverageHistoryStore {
    private static final String PREFS = "citation_coverage_history";
    private static final String KEY_REVIEWED = "reviewed";
    private static final String KEY_GROUNDED = "grounded";
    private static final String KEY_MISSING = "missing";
    private static final String KEY_BLOCKED = "blocked";
    private static final String KEY_SCOPE_IDS = "scope_ids";
    private static final int MAX_SCOPES = 60;

    @NonNull private final SharedPreferences preferences;

    public CitationCoverageHistoryStore(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void record(@NonNull BookAnswerGroundingValidator.Result result) {
        record(result, "General Studies", "General");
    }

    public synchronized void record(@NonNull BookAnswerGroundingValidator.Result result,
                                    @NonNull String subjectName,
                                    @NonNull String chapterTitle) {
        if (result.getStatus() == BookAnswerGroundingValidator.Status.NO_EXACT_EVIDENCE) return;
        int reviewed = preferences.getInt(KEY_REVIEWED, 0) + 1;
        int grounded = preferences.getInt(KEY_GROUNDED, 0);
        int missing = preferences.getInt(KEY_MISSING, 0);
        int blocked = preferences.getInt(KEY_BLOCKED, 0);
        if (result.isGrounded()) grounded++;
        else if (result.needsCitationCaution()) missing++;
        else if (result.hasUnsupportedCitation()) blocked++;
        SharedPreferences.Editor editor = preferences.edit()
                .putInt(KEY_REVIEWED, reviewed)
                .putInt(KEY_GROUNDED, grounded)
                .putInt(KEY_MISSING, missing)
                .putInt(KEY_BLOCKED, blocked);

        String subject = safeLabel(subjectName, "General Studies");
        String chapter = safeLabel(chapterTitle, "General");
        String scopeId = scopeId(subject, chapter);
        Set<String> scopeIds = new HashSet<>(
                preferences.getStringSet(KEY_SCOPE_IDS, Collections.emptySet()));
        if (scopeIds.contains(scopeId) || scopeIds.size() < MAX_SCOPES) {
            scopeIds.add(scopeId);
            String prefix = "scope_" + scopeId + "_";
            editor.putString(prefix + "subject", subject)
                    .putString(prefix + "chapter", chapter)
                    .putInt(prefix + KEY_REVIEWED,
                            preferences.getInt(prefix + KEY_REVIEWED, 0) + 1)
                    .putInt(prefix + KEY_GROUNDED,
                            preferences.getInt(prefix + KEY_GROUNDED, 0)
                                    + (result.isGrounded() ? 1 : 0))
                    .putInt(prefix + KEY_MISSING,
                            preferences.getInt(prefix + KEY_MISSING, 0)
                                    + (result.needsCitationCaution() ? 1 : 0))
                    .putInt(prefix + KEY_BLOCKED,
                            preferences.getInt(prefix + KEY_BLOCKED, 0)
                                    + (result.hasUnsupportedCitation() ? 1 : 0))
                    .putStringSet(KEY_SCOPE_IDS, scopeIds);
        }
        editor.apply();
    }

    @NonNull
    public Summary getSummary() {
        return Summary.fromCounts(
                preferences.getInt(KEY_REVIEWED, 0),
                preferences.getInt(KEY_GROUNDED, 0),
                preferences.getInt(KEY_MISSING, 0),
                preferences.getInt(KEY_BLOCKED, 0));
    }

    @NonNull
    public List<ScopeSummary> getTopScopeSummaries(int limit) {
        if (limit <= 0) return Collections.emptyList();
        List<ScopeSummary> summaries = new ArrayList<>();
        Set<String> ids = preferences.getStringSet(KEY_SCOPE_IDS, Collections.emptySet());
        for (String id : ids) {
            String prefix = "scope_" + id + "_";
            summaries.add(ScopeSummary.fromCounts(
                    preferences.getString(prefix + "subject", "General Studies"),
                    preferences.getString(prefix + "chapter", "General"),
                    preferences.getInt(prefix + KEY_REVIEWED, 0),
                    preferences.getInt(prefix + KEY_GROUNDED, 0),
                    preferences.getInt(prefix + KEY_MISSING, 0),
                    preferences.getInt(prefix + KEY_BLOCKED, 0)));
        }
        summaries.sort(Comparator.comparingInt(ScopeSummary::getReviewed).reversed()
                .thenComparingInt(ScopeSummary::getCoveragePercent));
        return new ArrayList<>(summaries.subList(0, Math.min(limit, summaries.size())));
    }

    @NonNull
    private static String safeLabel(@NonNull String value, @NonNull String fallback) {
        String safe = value.trim().replaceAll("\\s+", " ");
        if (safe.isEmpty()) return fallback;
        return safe.length() <= 80 ? safe : safe.substring(0, 80).trim();
    }

    @NonNull
    private static String scopeId(@NonNull String subject, @NonNull String chapter) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (subject.toLowerCase(Locale.ROOT) + "\n"
                            + chapter.toLowerCase(Locale.ROOT))
                            .getBytes(StandardCharsets.UTF_8));
            StringBuilder id = new StringBuilder();
            for (int index = 0; index < 8; index++) {
                id.append(String.format(Locale.ROOT, "%02x", digest[index]));
            }
            return id.toString();
        } catch (NoSuchAlgorithmException impossible) {
            return Integer.toHexString((subject + "\n" + chapter).hashCode());
        }
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

    public static final class ScopeSummary {
        @NonNull private final String subject;
        @NonNull private final String chapter;
        @NonNull private final Summary summary;

        private ScopeSummary(@NonNull String subject, @NonNull String chapter,
                             @NonNull Summary summary) {
            this.subject = subject;
            this.chapter = chapter;
            this.summary = summary;
        }

        @NonNull
        public static ScopeSummary fromCounts(@NonNull String subject, @NonNull String chapter,
                                              int reviewed, int grounded,
                                              int missing, int blocked) {
            return new ScopeSummary(subject, chapter,
                    Summary.fromCounts(reviewed, grounded, missing, blocked));
        }

        @NonNull public String getSubject() { return subject; }
        @NonNull public String getChapter() { return chapter; }
        public int getReviewed() { return summary.getReviewed(); }
        public int getCoveragePercent() { return summary.getCoveragePercent(); }
        public int getAttentionNeeded() { return summary.getAttentionNeeded(); }
    }
}
