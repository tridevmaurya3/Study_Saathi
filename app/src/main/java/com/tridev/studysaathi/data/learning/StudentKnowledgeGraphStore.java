package com.tridev.studysaathi.data.learning;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.ai.SmartTutorAnswerResult;
import com.tridev.studysaathi.data.local.entity.QuizAttemptEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Per-student concept graph foundation. It is intentionally isolated from Room/Firebase so
 * existing databases and cloud connections remain untouched while the graph evolves.
 */
public final class StudentKnowledgeGraphStore {
    private static final String PREFS = "student_knowledge_graph_v1";
    private static final String KEY_NODES = "nodes";
    private static final int MAX_NODES = 400;
    private static final Set<String> STOP_WORDS = createStopWords();
    private static final Object STORE_LOCK = new Object();

    @NonNull private final SharedPreferences preferences;

    public StudentKnowledgeGraphStore(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void recordAnswer(long profileId,
                             @Nullable String subject,
                             @Nullable String chapter,
                             @NonNull String question,
                             @NonNull SmartTutorAnswerResult result) {
        if (profileId <= 0 || question.trim().isEmpty()) return;
        synchronized (STORE_LOCK) {
            List<KnowledgeNode> nodes = readNodes();
            String conceptKey = extractConceptKey(question);
            KnowledgeNode node = findOrCreate(nodes, profileId, subject, chapter,
                    conceptKey, buildConceptLabel(question));
            node.answerInteractions++;
            node.confidenceTotal += result.getConfidenceScore();
            if (result.isVerified()) node.verifiedAnswers++;
            if (result.isRemoteAiAnswer()) node.remoteAiAnswers++;
            node.lastQuestion = compact(question, 180);
            node.lastUpdatedAt = System.currentTimeMillis();
            writeNodes(nodes);
        }
    }

    /** Quiz evidence carries more mastery weight than merely asking an AI question. */
    public void recordAssessment(@NonNull QuizAttemptEntity attempt) {
        if (attempt.getProfileId() <= 0 || attempt.getTotalQuestions() <= 0) return;
        synchronized (STORE_LOCK) {
            List<KnowledgeNode> nodes = readNodes();
            String conceptKey = normalize(attempt.getChapterTitle()).isEmpty()
                    ? "chapter-assessment" : normalize(attempt.getChapterTitle());
            KnowledgeNode node = findOrCreate(nodes, attempt.getProfileId(),
                    attempt.getSubjectName(), attempt.getChapterTitle(), conceptKey,
                    safe(attempt.getChapterTitle(), "Chapter assessment"));
            node.quizAttempts++;
            node.quizPercentageTotal += attempt.getPercentage();
            node.lastUpdatedAt = Math.max(System.currentTimeMillis(), attempt.getAttemptedAt());
            writeNodes(nodes);
        }
    }

    @NonNull
    public ProfileSummary getProfileSummary(long profileId) {
        synchronized (STORE_LOCK) {
            List<KnowledgeNode> profileNodes = new ArrayList<>();
            Set<String> subjects = new HashSet<>();
            int mastered = 0;
            int practicing = 0;
            int confidenceTotal = 0;
            for (KnowledgeNode node : readNodes()) {
                if (node.profileId != profileId) continue;
                profileNodes.add(node.copy());
                if (!node.subject.isEmpty()) subjects.add(node.subject);
                if (node.getStage() == LearningStage.MASTERED) mastered++;
                if (node.getStage() == LearningStage.PRACTICING) practicing++;
                confidenceTotal += node.getAverageConfidence();
            }
            Collections.sort(profileNodes,
                    (left, right) -> Long.compare(right.lastUpdatedAt, left.lastUpdatedAt));
            int averageConfidence = profileNodes.isEmpty()
                    ? 0 : confidenceTotal / profileNodes.size();
            return new ProfileSummary(subjects.size(), profileNodes.size(), mastered,
                    practicing, averageConfidence, profileNodes);
        }
    }

    @NonNull
    private KnowledgeNode findOrCreate(@NonNull List<KnowledgeNode> nodes,
                                       long profileId,
                                       @Nullable String subject,
                                       @Nullable String chapter,
                                       @NonNull String conceptKey,
                                       @NonNull String conceptLabel) {
        String safeSubject = safe(subject, "General Studies");
        String safeChapter = safe(chapter, "General");
        String key = profileId + "|" + normalize(safeSubject) + "|"
                + normalize(safeChapter) + "|" + conceptKey;
        for (KnowledgeNode node : nodes) {
            if (node.key.equals(key)) return node;
        }
        KnowledgeNode node = new KnowledgeNode();
        node.key = key;
        node.profileId = profileId;
        node.subject = safeSubject;
        node.chapter = safeChapter;
        node.conceptKey = conceptKey;
        node.conceptLabel = conceptLabel;
        node.firstSeenAt = System.currentTimeMillis();
        node.lastUpdatedAt = node.firstSeenAt;
        nodes.add(node);
        return node;
    }

    @NonNull
    private List<KnowledgeNode> readNodes() {
        List<KnowledgeNode> nodes = new ArrayList<>();
        String raw = preferences.getString(KEY_NODES, "");
        if (raw == null || raw.isEmpty()) return nodes;
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                KnowledgeNode node = KnowledgeNode.fromJson(array.optJSONObject(index));
                if (node != null) nodes.add(node);
            }
        } catch (JSONException ignored) {
            nodes.clear();
        }
        return nodes;
    }

    private void writeNodes(@NonNull List<KnowledgeNode> nodes) {
        nodes.sort(Comparator.comparingLong((KnowledgeNode node) -> node.lastUpdatedAt).reversed());
        JSONArray array = new JSONArray();
        int count = Math.min(MAX_NODES, nodes.size());
        for (int index = 0; index < count; index++) array.put(nodes.get(index).toJson());
        preferences.edit().putString(KEY_NODES, array.toString()).apply();
    }

    @NonNull
    private static String extractConceptKey(@NonNull String question) {
        String[] tokens = normalize(question).split("_");
        StringBuilder key = new StringBuilder();
        int accepted = 0;
        for (String token : tokens) {
            if (token.length() < 3 || STOP_WORDS.contains(token)) continue;
            if (key.length() > 0) key.append('_');
            key.append(token);
            if (++accepted == 5) break;
        }
        if (key.length() == 0) return "general-concept";
        return key.toString();
    }

    @NonNull
    private static String buildConceptLabel(@NonNull String question) {
        return compact(question.replace('\n', ' ').trim(), 72);
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    @NonNull
    private static String safe(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    @NonNull
    private static String compact(@NonNull String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit - 1) + "…";
    }

    @NonNull
    private static Set<String> createStopWords() {
        Set<String> words = new HashSet<>();
        Collections.addAll(words, "what", "why", "how", "explain", "tell", "about",
                "this", "that", "the", "and", "for", "with", "क्या", "क्यों", "कैसे",
                "बताओ", "समझाओ", "इसका", "इसके", "और", "एक", "को", "का", "की", "के");
        return words;
    }

    public enum LearningStage { DISCOVERED, LEARNING, PRACTICING, MASTERED }

    public static final class KnowledgeNode {
        private String key = "";
        private long profileId;
        private String subject = "";
        private String chapter = "";
        private String conceptKey = "";
        private String conceptLabel = "";
        private int answerInteractions;
        private int verifiedAnswers;
        private int remoteAiAnswers;
        private int confidenceTotal;
        private int quizAttempts;
        private int quizPercentageTotal;
        private String lastQuestion = "";
        private long firstSeenAt;
        private long lastUpdatedAt;

        public int getAverageConfidence() {
            return answerInteractions == 0 ? 0 : confidenceTotal / answerInteractions;
        }

        public int getAverageQuizPercentage() {
            return quizAttempts == 0 ? 0 : quizPercentageTotal / quizAttempts;
        }

        public int getReadinessScore() {
            int engagement = Math.min(100, answerInteractions * 12 + getAverageConfidence() / 3);
            if (quizAttempts == 0) return Math.min(45, engagement);
            return Math.min(100, Math.round(getAverageQuizPercentage() * .75f + engagement * .25f));
        }

        @NonNull public LearningStage getStage() {
            if (quizAttempts >= 2 && getAverageQuizPercentage() >= 80) return LearningStage.MASTERED;
            if (quizAttempts > 0) return LearningStage.PRACTICING;
            if (answerInteractions >= 3) return LearningStage.LEARNING;
            return LearningStage.DISCOVERED;
        }

        @NonNull public String getSubject() { return subject; }
        @NonNull public String getChapter() { return chapter; }
        @NonNull public String getConceptLabel() { return conceptLabel; }
        public int getAnswerInteractions() { return answerInteractions; }
        public int getQuizAttempts() { return quizAttempts; }
        public long getLastUpdatedAt() { return lastUpdatedAt; }

        private KnowledgeNode copy() {
            return fromJson(toJson());
        }

        private JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("key", key); json.put("profileId", profileId);
                json.put("subject", subject); json.put("chapter", chapter);
                json.put("conceptKey", conceptKey); json.put("conceptLabel", conceptLabel);
                json.put("answerInteractions", answerInteractions);
                json.put("verifiedAnswers", verifiedAnswers);
                json.put("remoteAiAnswers", remoteAiAnswers);
                json.put("confidenceTotal", confidenceTotal);
                json.put("quizAttempts", quizAttempts);
                json.put("quizPercentageTotal", quizPercentageTotal);
                json.put("lastQuestion", lastQuestion);
                json.put("firstSeenAt", firstSeenAt); json.put("lastUpdatedAt", lastUpdatedAt);
            } catch (JSONException ignored) { }
            return json;
        }

        @Nullable private static KnowledgeNode fromJson(@Nullable JSONObject json) {
            if (json == null) return null;
            KnowledgeNode node = new KnowledgeNode();
            node.key = json.optString("key");
            node.profileId = json.optLong("profileId");
            node.subject = json.optString("subject");
            node.chapter = json.optString("chapter");
            node.conceptKey = json.optString("conceptKey");
            node.conceptLabel = json.optString("conceptLabel");
            node.answerInteractions = json.optInt("answerInteractions");
            node.verifiedAnswers = json.optInt("verifiedAnswers");
            node.remoteAiAnswers = json.optInt("remoteAiAnswers");
            node.confidenceTotal = json.optInt("confidenceTotal");
            node.quizAttempts = json.optInt("quizAttempts");
            node.quizPercentageTotal = json.optInt("quizPercentageTotal");
            node.lastQuestion = json.optString("lastQuestion");
            node.firstSeenAt = json.optLong("firstSeenAt");
            node.lastUpdatedAt = json.optLong("lastUpdatedAt");
            return node.key.isEmpty() || node.profileId <= 0 ? null : node;
        }
    }

    public static final class ProfileSummary {
        private final int subjectCount;
        private final int conceptCount;
        private final int masteredCount;
        private final int practicingCount;
        private final int averageConfidence;
        @NonNull private final List<KnowledgeNode> recentNodes;

        ProfileSummary(int subjectCount, int conceptCount, int masteredCount,
                       int practicingCount, int averageConfidence,
                       @NonNull List<KnowledgeNode> recentNodes) {
            this.subjectCount = subjectCount; this.conceptCount = conceptCount;
            this.masteredCount = masteredCount; this.practicingCount = practicingCount;
            this.averageConfidence = averageConfidence;
            this.recentNodes = Collections.unmodifiableList(recentNodes);
        }

        public int getSubjectCount() { return subjectCount; }
        public int getConceptCount() { return conceptCount; }
        public int getMasteredCount() { return masteredCount; }
        public int getPracticingCount() { return practicingCount; }
        public int getAverageConfidence() { return averageConfidence; }
        @NonNull public List<KnowledgeNode> getRecentNodes() { return recentNodes; }
    }
}
