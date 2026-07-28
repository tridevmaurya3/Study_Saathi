package com.tridev.studysaathi.data.ai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Study Saathi का persistent Smart Answer Cache।
 *
 * इसका उद्देश्य:
 *
 * 1. पहले से पूछे गए समान प्रश्न का saved answer लौटाना।
 * 2. Duplicate Gemini requests और quota usage कम करना।
 * 3. Board, Class, Language, Subject और Chapter के अनुसार
 *    answers को अलग रखना।
 * 4. App बंद होने और दोबारा खुलने के बाद भी answers सुरक्षित रखना।
 * 5. पुराने और expired answers को स्वतः हटाना।
 * 6. Cache का आकार सीमित रखना।
 *
 * यह cache केवल text questions के लिए बनाया गया है।
 * Image questions को साधारण text key से cache नहीं करना चाहिए।
 */
public final class SmartAnswerCache {

    public static final String SOURCE_FIREBASE_AI =
            "FIREBASE_AI";

    public static final String SOURCE_OFFLINE_BASIC_MATH =
            "OFFLINE_BASIC_MATH";

    private static final String PREFERENCES_NAME =
            "study_saathi_smart_answer_cache";

    private static final String KEY_CACHE_ENTRIES_JSON =
            "cache_entries_json";

    private static final int CACHE_FORMAT_VERSION =
            1;

    /**
     * अधिकतम saved answers।
     *
     * Limit पूरी होने पर सबसे कम recently-used entries हटेंगी।
     */
    private static final int MAXIMUM_CACHE_ENTRIES =
            120;

    private static final int MAXIMUM_ANSWER_LENGTH =
            30000;

    /**
     * Gemini से मिला answer 30 दिन तक valid रहेगा।
     */
    private static final long FIREBASE_AI_CACHE_DURATION_MILLIS =
            TimeUnit.DAYS.toMillis(
                    30
            );

    /**
     * Deterministic offline Mathematics answer लंबे समय तक valid रह सकता है।
     */
    private static final long OFFLINE_MATH_CACHE_DURATION_MILLIS =
            TimeUnit.DAYS.toMillis(
                    180
            );

    @NonNull
    private final SharedPreferences preferences;

    public SmartAnswerCache(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        preferences =
                applicationContext.getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE
                );

        clearExpiredEntries();
    }

    /**
     * Matching और non-expired answer खोजता है।
     *
     * Match इन values के आधार पर होगा:
     *
     * Education Board
     * Student Class
     * Explanation Language
     * Subject
     * Chapter
     * Question
     */
    @NonNull
    public synchronized CacheLookupResult findAnswer(
            @Nullable String educationBoard,
            @Nullable String studentClass,
            @Nullable String explanationLanguage,
            @Nullable String subjectName,
            @Nullable String chapterTitle,
            @Nullable String question
    ) {
        String cacheKey =
                createCacheKey(
                        educationBoard,
                        studentClass,
                        explanationLanguage,
                        subjectName,
                        chapterTitle,
                        question
                );

        if (cacheKey.isEmpty()) {
            return CacheLookupResult.miss(
                    "Cache key could not be created."
            );
        }

        long currentTimeMillis =
                System.currentTimeMillis();

        List<CacheEntry> cacheEntries =
                readValidEntries(
                        currentTimeMillis
                );

        CacheEntry matchingEntry =
                null;

        for (CacheEntry cacheEntry : cacheEntries) {
            if (cacheKey.equals(
                    cacheEntry.cacheKey
            )) {
                matchingEntry =
                        cacheEntry;

                break;
            }
        }

        if (matchingEntry == null) {
            writeEntries(
                    cacheEntries
            );

            return CacheLookupResult.miss(
                    "No matching cached answer was found."
            );
        }

        matchingEntry.lastAccessedAt =
                currentTimeMillis;

        matchingEntry.accessCount =
                matchingEntry.accessCount + 1L;

        sortByMostRecentlyUsed(
                cacheEntries
        );

        writeEntries(
                cacheEntries
        );

        return CacheLookupResult.hit(
                matchingEntry.answerText,
                matchingEntry.answerSource,
                matchingEntry.createdAt,
                matchingEntry.expiresAt,
                matchingEntry.accessCount
        );
    }

    /**
     * नया answer persistent cache में save करता है।
     *
     * समान key का पुराना answer उपलब्ध होने पर replace होगा।
     */
    public synchronized void saveAnswer(
            @Nullable String educationBoard,
            @Nullable String studentClass,
            @Nullable String explanationLanguage,
            @Nullable String subjectName,
            @Nullable String chapterTitle,
            @Nullable String question,
            @Nullable String answerText,
            @Nullable String answerSource
    ) {
        String normalizedAnswer =
                safeText(
                        answerText
                );

        if (normalizedAnswer.isEmpty()) {
            return;
        }

        if (normalizedAnswer.length()
                > MAXIMUM_ANSWER_LENGTH) {

            normalizedAnswer =
                    normalizedAnswer.substring(
                            0,
                            MAXIMUM_ANSWER_LENGTH
                    ).trim();
        }

        String cacheKey =
                createCacheKey(
                        educationBoard,
                        studentClass,
                        explanationLanguage,
                        subjectName,
                        chapterTitle,
                        question
                );

        if (cacheKey.isEmpty()) {
            return;
        }

        String normalizedAnswerSource =
                normalizeAnswerSource(
                        answerSource
                );

        long currentTimeMillis =
                System.currentTimeMillis();

        long expiresAt =
                currentTimeMillis
                        + getCacheDurationMillis(
                        normalizedAnswerSource
                );

        List<CacheEntry> cacheEntries =
                readValidEntries(
                        currentTimeMillis
                );

        removeEntryWithKey(
                cacheEntries,
                cacheKey
        );

        CacheEntry newEntry =
                new CacheEntry(
                        cacheKey,
                        normalizedAnswer,
                        normalizedAnswerSource,
                        currentTimeMillis,
                        expiresAt,
                        currentTimeMillis,
                        0L
                );

        cacheEntries.add(
                newEntry
        );

        sortByMostRecentlyUsed(
                cacheEntries
        );

        trimToMaximumSize(
                cacheEntries
        );

        writeEntries(
                cacheEntries
        );
    }

    /**
     * वर्तमान valid cache entries की संख्या।
     */
    public synchronized int getValidEntryCount() {
        List<CacheEntry> cacheEntries =
                readValidEntries(
                        System.currentTimeMillis()
                );

        writeEntries(
                cacheEntries
        );

        return cacheEntries.size();
    }

    /**
     * Expired और corrupted entries हटाता है।
     */
    public synchronized void clearExpiredEntries() {
        List<CacheEntry> validEntries =
                readValidEntries(
                        System.currentTimeMillis()
                );

        writeEntries(
                validEntries
        );
    }

    /**
     * पूरे Smart Answer Cache को साफ करता है।
     */
    public synchronized void clearAll() {
        preferences.edit()
                .remove(
                        KEY_CACHE_ENTRIES_JSON
                )
                .apply();
    }

    /**
     * Cache key बनाने से पहले सभी context values normalize करता है।
     */
    @NonNull
    private String createCacheKey(
            @Nullable String educationBoard,
            @Nullable String studentClass,
            @Nullable String explanationLanguage,
            @Nullable String subjectName,
            @Nullable String chapterTitle,
            @Nullable String question
    ) {
        String normalizedQuestion =
                normalizeForCache(
                        question
                );

        if (normalizedQuestion.isEmpty()) {
            return "";
        }

        StringBuilder canonicalKeyBuilder =
                new StringBuilder();

        appendCanonicalPart(
                canonicalKeyBuilder,
                educationBoard
        );

        appendCanonicalPart(
                canonicalKeyBuilder,
                studentClass
        );

        appendCanonicalPart(
                canonicalKeyBuilder,
                explanationLanguage
        );

        appendCanonicalPart(
                canonicalKeyBuilder,
                subjectName
        );

        appendCanonicalPart(
                canonicalKeyBuilder,
                chapterTitle
        );

        appendCanonicalPart(
                canonicalKeyBuilder,
                normalizedQuestion
        );

        return createSha256Hash(
                canonicalKeyBuilder.toString()
        );
    }

    private void appendCanonicalPart(
            @NonNull StringBuilder builder,
            @Nullable String value
    ) {
        String normalizedValue =
                normalizeForCache(
                        value
                );

        builder.append(
                normalizedValue.length()
        );

        builder.append(
                ':'
        );

        builder.append(
                normalizedValue
        );

        builder.append(
                '|'
        );
    }

    /**
     * अलग spacing और case वाले समान questions को एक key में बदलता है।
     */
    @NonNull
    private String normalizeForCache(
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return "";
        }

        String unicodeNormalizedValue =
                Normalizer.normalize(
                        safeValue,
                        Normalizer.Form.NFKC
                );

        return unicodeNormalizedValue
                .replace(
                        '−',
                        '-'
                )
                .replace(
                        '–',
                        '-'
                )
                .replace(
                        '—',
                        '-'
                )
                .replace(
                        '×',
                        'x'
                )
                .replace(
                        '÷',
                        '/'
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }

    @NonNull
    private String createSha256Hash(
            @NonNull String canonicalValue
    ) {
        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] digestBytes =
                    messageDigest.digest(
                            canonicalValue.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hashBuilder =
                    new StringBuilder();

            for (byte digestByte : digestBytes) {
                hashBuilder.append(
                        String.format(
                                Locale.US,
                                "%02x",
                                digestByte & 0xff
                        )
                );
            }

            return hashBuilder.toString();

        } catch (NoSuchAlgorithmException exception) {
            /*
             * SHA-256 Android/Java platform में उपलब्ध होता है।
             * फिर भी सुरक्षित fallback रखा गया है।
             */
            return Integer.toHexString(
                    canonicalValue.hashCode()
            );
        }
    }

    @NonNull
    private List<CacheEntry> readValidEntries(
            long currentTimeMillis
    ) {
        String savedJson =
                preferences.getString(
                        KEY_CACHE_ENTRIES_JSON,
                        ""
                );

        List<CacheEntry> validEntries =
                new ArrayList<>();

        if (savedJson == null
                || savedJson.trim().isEmpty()) {

            return validEntries;
        }

        try {
            JSONObject rootObject =
                    new JSONObject(
                            savedJson
                    );

            int formatVersion =
                    rootObject.optInt(
                            "format_version",
                            0
                    );

            if (formatVersion
                    != CACHE_FORMAT_VERSION) {

                return validEntries;
            }

            JSONArray entriesArray =
                    rootObject.optJSONArray(
                            "entries"
                    );

            if (entriesArray == null) {
                return validEntries;
            }

            for (int index = 0;
                 index < entriesArray.length();
                 index++) {

                JSONObject entryObject =
                        entriesArray.optJSONObject(
                                index
                        );

                if (entryObject == null) {
                    continue;
                }

                CacheEntry cacheEntry =
                        CacheEntry.fromJson(
                                entryObject
                        );

                if (cacheEntry == null) {
                    continue;
                }

                if (cacheEntry.expiresAt
                        <= currentTimeMillis) {

                    continue;
                }

                if (cacheEntry.answerText.isEmpty()
                        || cacheEntry.cacheKey.isEmpty()) {

                    continue;
                }

                validEntries.add(
                        cacheEntry
                );
            }

        } catch (JSONException ignored) {
            /*
             * Corrupted cache को ignore करके fresh empty cache बनाया जाएगा।
             */
            validEntries.clear();
        }

        sortByMostRecentlyUsed(
                validEntries
        );

        trimToMaximumSize(
                validEntries
        );

        return validEntries;
    }

    private void writeEntries(
            @NonNull List<CacheEntry> cacheEntries
    ) {
        try {
            JSONObject rootObject =
                    new JSONObject();

            rootObject.put(
                    "format_version",
                    CACHE_FORMAT_VERSION
            );

            rootObject.put(
                    "updated_at",
                    System.currentTimeMillis()
            );

            JSONArray entriesArray =
                    new JSONArray();

            for (CacheEntry cacheEntry : cacheEntries) {
                entriesArray.put(
                        cacheEntry.toJson()
                );
            }

            rootObject.put(
                    "entries",
                    entriesArray
            );

            preferences.edit()
                    .putString(
                            KEY_CACHE_ENTRIES_JSON,
                            rootObject.toString()
                    )
                    .apply();

        } catch (JSONException ignored) {
            /*
             * Cache write fail होने पर app answer flow को प्रभावित नहीं किया जाएगा।
             */
        }
    }

    private void removeEntryWithKey(
            @NonNull List<CacheEntry> cacheEntries,
            @NonNull String cacheKey
    ) {
        for (int index =
             cacheEntries.size() - 1;
             index >= 0;
             index--) {

            CacheEntry cacheEntry =
                    cacheEntries.get(
                            index
                    );

            if (cacheKey.equals(
                    cacheEntry.cacheKey
            )) {
                cacheEntries.remove(
                        index
                );
            }
        }
    }

    private void sortByMostRecentlyUsed(
            @NonNull List<CacheEntry> cacheEntries
    ) {
        Collections.sort(
                cacheEntries,
                new Comparator<CacheEntry>() {

                    @Override
                    public int compare(
                            CacheEntry first,
                            CacheEntry second
                    ) {
                        return Long.compare(
                                second.lastAccessedAt,
                                first.lastAccessedAt
                        );
                    }
                }
        );
    }

    private void trimToMaximumSize(
            @NonNull List<CacheEntry> cacheEntries
    ) {
        while (cacheEntries.size()
                > MAXIMUM_CACHE_ENTRIES) {

            cacheEntries.remove(
                    cacheEntries.size() - 1
            );
        }
    }

    private long getCacheDurationMillis(
            @NonNull String answerSource
    ) {
        if (SOURCE_OFFLINE_BASIC_MATH.equals(
                answerSource
        )) {
            return OFFLINE_MATH_CACHE_DURATION_MILLIS;
        }

        return FIREBASE_AI_CACHE_DURATION_MILLIS;
    }

    @NonNull
    private String normalizeAnswerSource(
            @Nullable String answerSource
    ) {
        String normalizedSource =
                safeText(
                        answerSource
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (SOURCE_OFFLINE_BASIC_MATH.equals(
                normalizedSource
        )) {
            return SOURCE_OFFLINE_BASIC_MATH;
        }

        return SOURCE_FIREBASE_AI;
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }

    /**
     * Internal persistent cache entry।
     */
    private static final class CacheEntry {

        @NonNull
        private final String cacheKey;

        @NonNull
        private final String answerText;

        @NonNull
        private final String answerSource;

        private final long createdAt;

        private final long expiresAt;

        private long lastAccessedAt;

        private long accessCount;

        private CacheEntry(
                @NonNull String cacheKey,
                @NonNull String answerText,
                @NonNull String answerSource,
                long createdAt,
                long expiresAt,
                long lastAccessedAt,
                long accessCount
        ) {
            this.cacheKey =
                    cacheKey;

            this.answerText =
                    answerText;

            this.answerSource =
                    answerSource;

            this.createdAt =
                    createdAt;

            this.expiresAt =
                    expiresAt;

            this.lastAccessedAt =
                    lastAccessedAt;

            this.accessCount =
                    accessCount;
        }

        @NonNull
        private JSONObject toJson()
                throws JSONException {

            JSONObject entryObject =
                    new JSONObject();

            entryObject.put(
                    "cache_key",
                    cacheKey
            );

            entryObject.put(
                    "answer_text",
                    answerText
            );

            entryObject.put(
                    "answer_source",
                    answerSource
            );

            entryObject.put(
                    "created_at",
                    createdAt
            );

            entryObject.put(
                    "expires_at",
                    expiresAt
            );

            entryObject.put(
                    "last_accessed_at",
                    lastAccessedAt
            );

            entryObject.put(
                    "access_count",
                    accessCount
            );

            return entryObject;
        }

        @Nullable
        private static CacheEntry fromJson(
                @NonNull JSONObject entryObject
        ) {
            String cacheKey =
                    safeText(
                            entryObject.optString(
                                    "cache_key",
                                    ""
                            )
                    );

            String answerText =
                    safeText(
                            entryObject.optString(
                                    "answer_text",
                                    ""
                            )
                    );

            String answerSource =
                    safeText(
                            entryObject.optString(
                                    "answer_source",
                                    SOURCE_FIREBASE_AI
                            )
                    );

            long createdAt =
                    entryObject.optLong(
                            "created_at",
                            0L
                    );

            long expiresAt =
                    entryObject.optLong(
                            "expires_at",
                            0L
                    );

            long lastAccessedAt =
                    entryObject.optLong(
                            "last_accessed_at",
                            createdAt
                    );

            long accessCount =
                    entryObject.optLong(
                            "access_count",
                            0L
                    );

            if (cacheKey.isEmpty()
                    || answerText.isEmpty()
                    || createdAt <= 0L
                    || expiresAt <= createdAt) {

                return null;
            }

            return new CacheEntry(
                    cacheKey,
                    answerText,
                    answerSource,
                    createdAt,
                    expiresAt,
                    lastAccessedAt,
                    Math.max(
                            0L,
                            accessCount
                    )
            );
        }
    }

    /**
     * Cache lookup का immutable result।
     */
    public static final class CacheLookupResult {

        private final boolean cacheHit;

        @NonNull
        private final String answerText;

        @NonNull
        private final String answerSource;

        private final long createdAt;

        private final long expiresAt;

        private final long accessCount;

        @NonNull
        private final String diagnosticReason;

        private CacheLookupResult(
                boolean cacheHit,
                @NonNull String answerText,
                @NonNull String answerSource,
                long createdAt,
                long expiresAt,
                long accessCount,
                @NonNull String diagnosticReason
        ) {
            this.cacheHit =
                    cacheHit;

            this.answerText =
                    answerText;

            this.answerSource =
                    answerSource;

            this.createdAt =
                    createdAt;

            this.expiresAt =
                    expiresAt;

            this.accessCount =
                    accessCount;

            this.diagnosticReason =
                    diagnosticReason;
        }

        @NonNull
        private static CacheLookupResult hit(
                @NonNull String answerText,
                @NonNull String answerSource,
                long createdAt,
                long expiresAt,
                long accessCount
        ) {
            return new CacheLookupResult(
                    true,
                    answerText,
                    answerSource,
                    createdAt,
                    expiresAt,
                    accessCount,
                    ""
            );
        }

        @NonNull
        private static CacheLookupResult miss(
                @NonNull String diagnosticReason
        ) {
            return new CacheLookupResult(
                    false,
                    "",
                    "",
                    0L,
                    0L,
                    0L,
                    diagnosticReason
            );
        }

        public boolean isCacheHit() {
            return cacheHit;
        }

        @NonNull
        public String getAnswerText() {
            return answerText;
        }

        @NonNull
        public String getAnswerSource() {
            return answerSource;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public long getAccessCount() {
            return accessCount;
        }

        @NonNull
        public String getDiagnosticReason() {
            return diagnosticReason;
        }
    }
}