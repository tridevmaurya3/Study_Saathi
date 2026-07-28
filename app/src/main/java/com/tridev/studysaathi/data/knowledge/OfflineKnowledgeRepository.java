package com.tridev.studysaathi.data.knowledge;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Study Saathi का general verified offline knowledge repository।
 *
 * यह repository:
 *
 * 1. assets/offline_knowledge directory की JSON files पढ़ता है।
 * 2. सभी valid OfflineKnowledgeEntry objects memory में load करता है।
 * 3. Board, Class, Language, Subject और Chapter context जाँचता है।
 * 4. Question patterns, keywords, topic और title के आधार पर score बनाता है।
 * 5. केवल पर्याप्त confidence वाला verified answer लौटाता है।
 *
 * यह repository calculation नहीं करता।
 *
 * Mathematics calculations के लिए:
 *
 * - OfflineBasicMathSolver
 * - OfflineDivisibilitySolver
 *
 * जैसे deterministic solvers उपयोग किए जाते हैं।
 */
public final class OfflineKnowledgeRepository {

    private static final String ASSET_DIRECTORY =
            "offline_knowledge";

    private static final String JSON_FILE_EXTENSION =
            ".json";

    /**
     * इससे कम score वाला answer student को नहीं दिया जाएगा।
     *
     * इससे generic keyword मिलने पर गलत answer लौटने का खतरा कम होता है।
     */
    private static final int MINIMUM_ACCEPTED_SCORE =
            35;

    private static final int SCORE_EXACT_QUESTION_PATTERN =
            130;

    private static final int SCORE_CONTAINED_QUESTION_PATTERN =
            85;

    private static final int SCORE_TOPIC_MATCH =
            40;

    private static final int SCORE_TITLE_MATCH =
            35;

    private static final int SCORE_PER_KEYWORD =
            18;

    private static final int MAXIMUM_KEYWORD_SCORE =
            108;

    private static final int SCORE_SPECIFIC_CHAPTER =
            12;

    private static final int SCORE_SPECIFIC_BOARD =
            8;

    private static final int SCORE_SPECIFIC_CLASS =
            8;

    private static final int MAXIMUM_PRIORITY_SCORE =
            20;

    @NonNull
    private final Context applicationContext;

    @NonNull
    private final Object repositoryLock =
            new Object();

    @NonNull
    private List<OfflineKnowledgeEntry> loadedEntries =
            Collections.emptyList();

    private boolean knowledgeLoaded;

    private int ignoredEntryCount;

    @NonNull
    private String lastLoadDiagnostic =
            "Offline knowledge has not been loaded yet.";

    public OfflineKnowledgeRepository(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();
    }

    /**
     * Current student question के लिए सबसे सही verified
     * offline knowledge answer खोजता है।
     *
     * कमजोर या ambiguous match मिलने पर found=false result
     * लौटाया जाएगा और question आगे cache/Gemini को भेजा जा सकेगा।
     */
    @NonNull
    public SearchResult findBestAnswer(
            @Nullable String requestedBoard,
            @Nullable String requestedClass,
            @Nullable String requestedLanguage,
            @Nullable String requestedSubject,
            @Nullable String requestedChapter,
            @Nullable String question
    ) {
        String normalizedQuestion =
                normalizeText(
                        question
                );

        if (normalizedQuestion.isEmpty()) {
            return SearchResult.notFound(
                    "Question is empty."
            );
        }

        ensureKnowledgeLoaded();

        List<OfflineKnowledgeEntry> entriesSnapshot;

        synchronized (repositoryLock) {
            entriesSnapshot =
                    new ArrayList<>(
                            loadedEntries
                    );
        }

        if (entriesSnapshot.isEmpty()) {
            return SearchResult.notFound(
                    "No offline knowledge entries are loaded."
            );
        }

        ScoredEntry bestScoredEntry =
                null;

        for (OfflineKnowledgeEntry entry : entriesSnapshot) {
            if (entry == null
                    || !entry.isValid()
                    || !entry.isVerified()) {

                continue;
            }

            if (!entry.matchesContext(
                    requestedBoard,
                    requestedClass,
                    requestedLanguage,
                    requestedSubject,
                    requestedChapter
            )) {
                continue;
            }

            ScoredEntry scoredEntry =
                    calculateScore(
                            entry,
                            normalizedQuestion
                    );

            if (!scoredEntry.hasMeaningfulMatch()) {
                continue;
            }

            if (bestScoredEntry == null
                    || scoredEntry.getScore()
                    > bestScoredEntry.getScore()) {

                bestScoredEntry =
                        scoredEntry;
            }
        }

        if (bestScoredEntry == null) {
            return SearchResult.notFound(
                    "No verified entry matched the question and context."
            );
        }

        if (bestScoredEntry.getScore()
                < MINIMUM_ACCEPTED_SCORE) {

            return SearchResult.notFound(
                    "Best knowledge match score was below the safety threshold: "
                            + bestScoredEntry.getScore()
            );
        }

        OfflineKnowledgeEntry bestEntry =
                bestScoredEntry.getEntry();

        String answerText =
                safeText(
                        bestEntry.buildAnswerText()
                );

        if (answerText.isEmpty()) {
            return SearchResult.notFound(
                    "Matched knowledge entry produced an empty answer."
            );
        }

        return SearchResult.found(
                bestEntry,
                answerText,
                bestScoredEntry.getScore(),
                bestScoredEntry.getMatchedKeywordCount(),
                bestScoredEntry.isQuestionPatternMatched()
        );
    }

    /**
     * Repository को पहली बार आवश्यकता पड़ने पर load करता है।
     */
    private void ensureKnowledgeLoaded() {
        synchronized (repositoryLock) {
            if (knowledgeLoaded) {
                return;
            }

            loadKnowledgeLocked();
        }
    }

    /**
     * Assets से सभी knowledge files दोबारा load करता है।
     *
     * Development, testing अथवा future content-update system में
     * इसका उपयोग किया जा सकता है।
     */
    public void reload() {
        synchronized (repositoryLock) {
            knowledgeLoaded =
                    false;

            loadedEntries =
                    Collections.emptyList();

            ignoredEntryCount =
                    0;

            lastLoadDiagnostic =
                    "Reload requested.";

            loadKnowledgeLocked();
        }
    }

    /**
     * assets/offline_knowledge की सभी JSON files पढ़ता है।
     *
     * Supported root formats:
     *
     * 1. Direct JSON array:
     *
     * [
     *   { entry },
     *   { entry }
     * ]
     *
     * 2. JSON object:
     *
     * {
     *   "entries": [
     *     { entry },
     *     { entry }
     *   ]
     * }
     */
    private void loadKnowledgeLocked() {
        AssetManager assetManager =
                applicationContext.getAssets();

        List<OfflineKnowledgeEntry> newEntries =
                new ArrayList<>();

        int ignoredEntries =
                0;

        int parsedFileCount =
                0;

        try {
            String[] assetFiles =
                    assetManager.list(
                            ASSET_DIRECTORY
                    );

            if (assetFiles == null
                    || assetFiles.length == 0) {

                loadedEntries =
                        Collections.emptyList();

                ignoredEntryCount =
                        0;

                knowledgeLoaded =
                        true;

                lastLoadDiagnostic =
                        "No JSON files found in assets/"
                                + ASSET_DIRECTORY
                                + ".";

                return;
            }

            for (String assetFileName : assetFiles) {
                if (assetFileName == null
                        || !assetFileName
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .endsWith(
                                JSON_FILE_EXTENSION
                        )) {

                    continue;
                }

                String completeAssetPath =
                        ASSET_DIRECTORY
                                + "/"
                                + assetFileName;

                try {
                    String jsonText =
                            readEntireAsset(
                                    assetManager,
                                    completeAssetPath
                            );

                    ParseResult parseResult =
                            parseKnowledgeJson(
                                    jsonText
                            );

                    newEntries.addAll(
                            parseResult.getEntries()
                    );

                    ignoredEntries +=
                            parseResult.getIgnoredEntryCount();

                    parsedFileCount++;

                } catch (IOException
                         | JSONException exception) {

                    ignoredEntries++;
                }
            }

        } catch (IOException exception) {
            loadedEntries =
                    Collections.emptyList();

            ignoredEntryCount =
                    0;

            knowledgeLoaded =
                    true;

            lastLoadDiagnostic =
                    "Unable to list offline knowledge assets: "
                            + safeText(
                            exception.getMessage()
                    );

            return;
        }

        /*
         * Higher-priority entries पहले रखी जाती हैं।
         *
         * Search score फिर भी final selection तय करेगा।
         */
        Collections.sort(
                newEntries,
                new Comparator<OfflineKnowledgeEntry>() {

                    @Override
                    public int compare(
                            OfflineKnowledgeEntry firstEntry,
                            OfflineKnowledgeEntry secondEntry
                    ) {
                        return Integer.compare(
                                secondEntry.getPriority(),
                                firstEntry.getPriority()
                        );
                    }
                }
        );

        loadedEntries =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                newEntries
                        )
                );

        ignoredEntryCount =
                ignoredEntries;

        knowledgeLoaded =
                true;

        lastLoadDiagnostic =
                "Loaded "
                        + loadedEntries.size()
                        + " verified-capable entries from "
                        + parsedFileCount
                        + " JSON files. Ignored entries/files: "
                        + ignoredEntryCount
                        + ".";
    }

    /**
     * एक JSON file का content parse करता है।
     */
    @NonNull
    private ParseResult parseKnowledgeJson(
            @Nullable String jsonText
    ) throws JSONException {

        String safeJsonText =
                safeText(
                        jsonText
                );

        if (safeJsonText.isEmpty()) {
            return new ParseResult(
                    Collections.emptyList(),
                    1
            );
        }

        JSONArray entriesArray;

        if (safeJsonText.startsWith("[")) {
            entriesArray =
                    new JSONArray(
                            safeJsonText
                    );

        } else {
            JSONObject rootObject =
                    new JSONObject(
                            safeJsonText
                    );

            entriesArray =
                    rootObject.optJSONArray(
                            "entries"
                    );

            if (entriesArray == null) {
                throw new JSONException(
                        "Knowledge JSON object does not contain an entries array."
                );
            }
        }

        List<OfflineKnowledgeEntry> parsedEntries =
                new ArrayList<>();

        int ignoredEntries =
                0;

        for (int index = 0;
             index < entriesArray.length();
             index++) {

            JSONObject entryObject =
                    entriesArray.optJSONObject(
                            index
                    );

            if (entryObject == null) {
                ignoredEntries++;

                continue;
            }

            try {
                OfflineKnowledgeEntry entry =
                        OfflineKnowledgeEntry.fromJson(
                                entryObject
                        );

                parsedEntries.add(
                        entry
                );

            } catch (JSONException exception) {
                ignoredEntries++;
            }
        }

        return new ParseResult(
                parsedEntries,
                ignoredEntries
        );
    }

    /**
     * एक entry का question match score बनाता है।
     */
    @NonNull
    private ScoredEntry calculateScore(
            @NonNull OfflineKnowledgeEntry entry,
            @NonNull String normalizedQuestion
    ) {
        int totalScore =
                0;

        int matchedKeywordCount =
                0;

        boolean questionPatternMatched =
                false;

        /*
         * Question patterns सबसे मजबूत matching signal हैं।
         */
        for (String questionPattern :
                entry.getQuestionPatterns()) {

            String normalizedPattern =
                    normalizeText(
                            questionPattern
                    );

            if (normalizedPattern.isEmpty()) {
                continue;
            }

            if (normalizedQuestion.equals(
                    normalizedPattern
            )) {
                totalScore +=
                        SCORE_EXACT_QUESTION_PATTERN;

                questionPatternMatched =
                        true;

                break;
            }

            if (normalizedQuestion.contains(
                    normalizedPattern
            )) {
                totalScore +=
                        SCORE_CONTAINED_QUESTION_PATTERN;

                questionPatternMatched =
                        true;
            }
        }

        /*
         * Topic और title match strong signals हैं।
         */
        String normalizedTopic =
                normalizeText(
                        entry.getTopic()
                );

        boolean topicMatched =
                !normalizedTopic.isEmpty()
                        && normalizedQuestion.contains(
                        normalizedTopic
                );

        if (topicMatched) {
            totalScore +=
                    SCORE_TOPIC_MATCH;
        }

        String normalizedTitle =
                normalizeText(
                        entry.getTitle()
                );

        boolean titleMatched =
                !normalizedTitle.isEmpty()
                        && normalizedQuestion.contains(
                        normalizedTitle
                );

        if (titleMatched) {
            totalScore +=
                    SCORE_TITLE_MATCH;
        }

        /*
         * Multiple matched keywords confidence बढ़ाते हैं।
         */
        int keywordScore =
                0;

        for (String keyword :
                entry.getKeywords()) {

            String normalizedKeyword =
                    normalizeText(
                            keyword
                    );

            if (normalizedKeyword.length() < 2) {
                continue;
            }

            if (normalizedQuestion.contains(
                    normalizedKeyword
            )) {
                matchedKeywordCount++;

                keywordScore +=
                        SCORE_PER_KEYWORD;
            }
        }

        totalScore +=
                Math.min(
                        keywordScore,
                        MAXIMUM_KEYWORD_SCORE
                );

        /*
         * अधिक specific entries को wildcard entries से
         * थोड़ी preference दी जाती है।
         */
        if (!OfflineKnowledgeEntry.ANY_VALUE.equals(
                normalizeText(
                        entry.getChapter()
                )
        )) {
            totalScore +=
                    SCORE_SPECIFIC_CHAPTER;
        }

        if (!OfflineKnowledgeEntry.ANY_VALUE.equals(
                normalizeText(
                        entry.getEducationBoard()
                )
        )) {
            totalScore +=
                    SCORE_SPECIFIC_BOARD;
        }

        if (!OfflineKnowledgeEntry.ANY_VALUE.equals(
                normalizeText(
                        entry.getStudentClass()
                )
        )) {
            totalScore +=
                    SCORE_SPECIFIC_CLASS;
        }

        totalScore +=
                Math.min(
                        entry.getPriority(),
                        MAXIMUM_PRIORITY_SCORE
                );

        return new ScoredEntry(
                entry,
                totalScore,
                matchedKeywordCount,
                questionPatternMatched,
                topicMatched,
                titleMatched
        );
    }

    /**
     * Asset file को UTF-8 text में पढ़ता है।
     */
    @NonNull
    private String readEntireAsset(
            @NonNull AssetManager assetManager,
            @NonNull String assetPath
    ) throws IOException {

        StringBuilder contentBuilder =
                new StringBuilder();

        try (
                InputStream inputStream =
                        assetManager.open(
                                assetPath
                        );

                InputStreamReader inputStreamReader =
                        new InputStreamReader(
                                inputStream,
                                StandardCharsets.UTF_8
                        );

                BufferedReader bufferedReader =
                        new BufferedReader(
                                inputStreamReader
                        )
        ) {
            String currentLine;

            while (
                    (
                            currentLine =
                                    bufferedReader.readLine()
                    ) != null
            ) {
                contentBuilder.append(
                        currentLine
                );

                contentBuilder.append(
                        '\n'
                );
            }
        }

        return contentBuilder.toString()
                .trim();
    }

    /**
     * वर्तमान loaded entries की संख्या।
     */
    public int getLoadedEntryCount() {
        ensureKnowledgeLoaded();

        synchronized (repositoryLock) {
            return loadedEntries.size();
        }
    }

    /**
     * Invalid entries अथवा unreadable files की संख्या।
     */
    public int getIgnoredEntryCount() {
        ensureKnowledgeLoaded();

        synchronized (repositoryLock) {
            return ignoredEntryCount;
        }
    }

    /**
     * Development और debugging के लिए last load status।
     */
    @NonNull
    public String getLastLoadDiagnostic() {
        ensureKnowledgeLoaded();

        synchronized (repositoryLock) {
            return lastLoadDiagnostic;
        }
    }

    @NonNull
    private static String normalizeText(
            @Nullable Object value
    ) {
        return safeText(
                value
        )
                .replace(
                        '\u00A0',
                        ' '
                )
                .replaceAll(
                        "[\\p{Punct}।॥]+",
                        " "
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
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }

    /**
     * JSON parsing का internal immutable result।
     */
    private static final class ParseResult {

        @NonNull
        private final List<OfflineKnowledgeEntry> entries;

        private final int ignoredEntryCount;

        private ParseResult(
                @NonNull List<OfflineKnowledgeEntry> entries,
                int ignoredEntryCount
        ) {
            this.entries =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    entries
                            )
                    );

            this.ignoredEntryCount =
                    Math.max(
                            0,
                            ignoredEntryCount
                    );
        }

        @NonNull
        private List<OfflineKnowledgeEntry> getEntries() {
            return entries;
        }

        private int getIgnoredEntryCount() {
            return ignoredEntryCount;
        }
    }

    /**
     * एक entry और उसके matching score का internal result।
     */
    private static final class ScoredEntry {

        @NonNull
        private final OfflineKnowledgeEntry entry;

        private final int score;

        private final int matchedKeywordCount;

        private final boolean questionPatternMatched;

        private final boolean topicMatched;

        private final boolean titleMatched;

        private ScoredEntry(
                @NonNull OfflineKnowledgeEntry entry,
                int score,
                int matchedKeywordCount,
                boolean questionPatternMatched,
                boolean topicMatched,
                boolean titleMatched
        ) {
            this.entry =
                    entry;

            this.score =
                    Math.max(
                            0,
                            score
                    );

            this.matchedKeywordCount =
                    Math.max(
                            0,
                            matchedKeywordCount
                    );

            this.questionPatternMatched =
                    questionPatternMatched;

            this.topicMatched =
                    topicMatched;

            this.titleMatched =
                    titleMatched;
        }

        /**
         * कम-से-कम एक वास्तविक question signal होना चाहिए।
         *
         * केवल context और priority के आधार पर answer नहीं चुना जाएगा।
         */
        private boolean hasMeaningfulMatch() {
            return questionPatternMatched
                    || topicMatched
                    || titleMatched
                    || matchedKeywordCount > 0;
        }

        @NonNull
        private OfflineKnowledgeEntry getEntry() {
            return entry;
        }

        private int getScore() {
            return score;
        }

        private int getMatchedKeywordCount() {
            return matchedKeywordCount;
        }

        private boolean isQuestionPatternMatched() {
            return questionPatternMatched;
        }
    }

    /**
     * Offline knowledge search का immutable public result।
     */
    public static final class SearchResult {

        private final boolean found;

        @Nullable
        private final OfflineKnowledgeEntry entry;

        @NonNull
        private final String answerText;

        private final int matchScore;

        private final int matchedKeywordCount;

        private final boolean questionPatternMatched;

        @NonNull
        private final String diagnosticReason;

        private SearchResult(
                boolean found,
                @Nullable OfflineKnowledgeEntry entry,
                @NonNull String answerText,
                int matchScore,
                int matchedKeywordCount,
                boolean questionPatternMatched,
                @NonNull String diagnosticReason
        ) {
            this.found =
                    found;

            this.entry =
                    entry;

            this.answerText =
                    answerText;

            this.matchScore =
                    Math.max(
                            0,
                            matchScore
                    );

            this.matchedKeywordCount =
                    Math.max(
                            0,
                            matchedKeywordCount
                    );

            this.questionPatternMatched =
                    questionPatternMatched;

            this.diagnosticReason =
                    diagnosticReason;
        }

        @NonNull
        private static SearchResult found(
                @NonNull OfflineKnowledgeEntry entry,
                @NonNull String answerText,
                int matchScore,
                int matchedKeywordCount,
                boolean questionPatternMatched
        ) {
            return new SearchResult(
                    true,
                    entry,
                    answerText,
                    matchScore,
                    matchedKeywordCount,
                    questionPatternMatched,
                    ""
            );
        }

        @NonNull
        private static SearchResult notFound(
                @NonNull String diagnosticReason
        ) {
            return new SearchResult(
                    false,
                    null,
                    "",
                    0,
                    0,
                    false,
                    diagnosticReason
            );
        }

        public boolean isFound() {
            return found;
        }

        @Nullable
        public OfflineKnowledgeEntry getEntry() {
            return entry;
        }

        @NonNull
        public String getAnswerText() {
            return answerText;
        }

        public int getMatchScore() {
            return matchScore;
        }

        public int getMatchedKeywordCount() {
            return matchedKeywordCount;
        }

        public boolean isQuestionPatternMatched() {
            return questionPatternMatched;
        }

        @NonNull
        public String getDiagnosticReason() {
            return diagnosticReason;
        }

        @NonNull
        public String getEntryId() {
            return entry == null
                    ? ""
                    : entry.getEntryId();
        }

        @NonNull
        public String getSourceLabel() {
            return entry == null
                    ? ""
                    : entry.getSourceLabel();
        }

        @NonNull
        public String getSourceVersion() {
            return entry == null
                    ? ""
                    : entry.getSourceVersion();
        }

        public boolean isVerified() {
            return entry != null
                    && entry.isVerified();
        }
    }
}