package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Floating Study Saathi conversation का छोटा, device-local persistent store।
 * केवल अंतिम 24 सफल turns रखे जाते हैं ताकि storage और AI context सीमित रहें।
 */
public final class SmartCompanionConversationStore {

    private static final String PREFERENCES_NAME =
            "smart_companion_conversations";
    private static final String KEY_PREFIX = "profile_";
    private static final int MAXIMUM_TURNS = 24;
    private static final int MAXIMUM_CONTEXT_CHARACTERS = 8000;

    @NonNull
    private final SharedPreferences preferences;

    public SmartCompanionConversationStore(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE
                );
    }

    @NonNull
    public List<Turn> load(long profileId) {
        String serialized = preferences.getString(key(profileId), "");
        if (serialized == null || serialized.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Turn> turns = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(serialized);
            int start = Math.max(0, array.length() - MAXIMUM_TURNS);
            for (int index = start; index < array.length(); index++) {
                JSONObject object = array.optJSONObject(index);
                if (object == null) {
                    continue;
                }
                Turn turn = new Turn(
                        object.optString("question"),
                        object.optString("answer"),
                        object.optString("source"),
                        object.optBoolean("verified"),
                        object.optLong("createdAt")
                );
                if (turn.isValid()) {
                    turns.add(turn);
                }
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(key(profileId)).apply();
        }
        return turns;
    }

    public void append(long profileId, @NonNull Turn turn) {
        if (!turn.isValid()) {
            return;
        }

        List<Turn> turns = new ArrayList<>(load(profileId));
        turns.add(turn);
        if (turns.size() > MAXIMUM_TURNS) {
            turns = new ArrayList<>(
                    turns.subList(
                            turns.size() - MAXIMUM_TURNS,
                            turns.size()
                    )
            );
        }

        JSONArray array = new JSONArray();
        for (Turn saved : turns) {
            JSONObject object = new JSONObject();
            try {
                object.put("question", saved.getQuestion());
                object.put("answer", saved.getAnswer());
                object.put("source", saved.getSource());
                object.put("verified", saved.isVerified());
                object.put("createdAt", saved.getCreatedAt());
                array.put(object);
            } catch (JSONException ignored) {
                // A malformed individual turn must not destroy older turns.
            }
        }
        preferences.edit().putString(key(profileId), array.toString()).apply();
    }

    public void clear(long profileId) {
        preferences.edit().remove(key(profileId)).apply();
    }

    @NonNull
    public String buildConversationContext(@NonNull List<Turn> turns) {
        StringBuilder context = new StringBuilder();
        int start = Math.max(0, turns.size() - 8);
        for (int index = start; index < turns.size(); index++) {
            Turn turn = turns.get(index);
            if (!turn.isValid()) {
                continue;
            }
            context.append("Student: ")
                    .append(turn.getQuestion())
                    .append("\nTutor: ")
                    .append(turn.getAnswer())
                    .append("\n\n");
        }

        if (context.length() <= MAXIMUM_CONTEXT_CHARACTERS) {
            return context.toString().trim();
        }
        return context.substring(
                context.length() - MAXIMUM_CONTEXT_CHARACTERS
        ).trim();
    }

    @NonNull
    private String key(long profileId) {
        return KEY_PREFIX + Math.max(0L, profileId);
    }

    public static final class Turn {

        @NonNull
        private final String question;
        @NonNull
        private final String answer;
        @NonNull
        private final String source;
        private final boolean verified;
        private final long createdAt;

        public Turn(
                @Nullable String question,
                @Nullable String answer,
                @Nullable String source,
                boolean verified,
                long createdAt
        ) {
            this.question = safe(question);
            this.answer = safe(answer);
            this.source = safe(source);
            this.verified = verified;
            this.createdAt = createdAt > 0L
                    ? createdAt
                    : System.currentTimeMillis();
        }

        public boolean isValid() {
            return !question.isEmpty() && !answer.isEmpty();
        }

        @NonNull
        public String getQuestion() {
            return question;
        }

        @NonNull
        public String getAnswer() {
            return answer;
        }

        @NonNull
        public String getSource() {
            return source;
        }

        public boolean isVerified() {
            return verified;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        @NonNull
        private static String safe(@Nullable String value) {
            return value == null ? "" : value.trim();
        }
    }
}
