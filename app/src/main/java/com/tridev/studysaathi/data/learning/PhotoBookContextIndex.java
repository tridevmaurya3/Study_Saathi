package com.tridev.studysaathi.data.learning;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookChapterPageEntity;
import com.tridev.studysaathi.data.local.entity.SchoolBookEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only in-memory index used to ground photo/OCR questions in the active
 * student's parent-approved school-book pages.
 *
 * The index never writes to Room and never invents a page number. A page is
 * exposed as verified evidence only when the OCR/question text has a strong,
 * unambiguous overlap with one approved stored page.
 */
public final class PhotoBookContextIndex {
    private static final long INDEX_REFRESH_INTERVAL_MS = 60_000L;
    private static final long MATCH_EXPIRY_MS = 45_000L;
    private static final int MAX_INDEXED_PAGES = 1500;
    private static final int MAX_INDEX_TEXT_CHARS = 7000;
    private static final int MIN_QUERY_TOKENS = 3;
    private static final int MIN_VERIFIED_OVERLAP = 4;
    private static final double MIN_VERIFIED_SCORE = 56.0;
    private static final double MIN_SCORE_MARGIN = 8.0;

    private static final Set<String> STOP_WORDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "the", "a", "an", "and", "or", "of", "to", "in", "on", "for",
                    "is", "are", "was", "were", "this", "that", "these", "those",
                    "what", "why", "how", "which", "who", "when", "where", "with",
                    "question", "answer", "solve", "explain", "please", "tell", "show",
                    "का", "की", "के", "को", "में", "से", "पर", "और", "या", "यह",
                    "ये", "वह", "क्या", "क्यों", "कैसे", "कौन", "कब", "कहाँ", "बताओ",
                    "समझाओ", "हल", "प्रश्न", "उत्तर", "करो", "करें", "है", "हैं", "था"
            ))
    );

    private static final AtomicBoolean LOAD_IN_PROGRESS = new AtomicBoolean(false);

    @Nullable
    private static volatile Context applicationContext;

    @NonNull
    private static volatile List<IndexedPage> indexedPages = Collections.emptyList();

    private static volatile long lastLoadedAt;

    @Nullable
    private static volatile MatchResult latestVerifiedMatch;

    private PhotoBookContextIndex() { }

    /** Start a background read-only index build as early as app startup. */
    public static void initialize(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
        refresh();
    }

    /** Rebuilds the index on Room's existing background executor. */
    public static void refresh() {
        Context context = applicationContext;
        if (context == null || !LOAD_IN_PROGRESS.compareAndSet(false, true)) {
            return;
        }

        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            List<IndexedPage> rebuilt = Collections.emptyList();
            try {
                rebuilt = buildIndex(context);
            } catch (RuntimeException ignored) {
                // Photo grounding is an enhancement; normal AI must remain usable.
            } finally {
                indexedPages = Collections.unmodifiableList(new ArrayList<>(rebuilt));
                lastLoadedAt = System.currentTimeMillis();
                LOAD_IN_PROGRESS.set(false);
            }
        });
    }

    private static void refreshIfStale() {
        if (applicationContext == null) {
            return;
        }
        long age = System.currentTimeMillis() - lastLoadedAt;
        if (lastLoadedAt <= 0L || age >= INDEX_REFRESH_INTERVAL_MS) {
            refresh();
        }
    }

    /**
     * Matches OCR/question text only for image requests. A weak or ambiguous
     * candidate returns no verified page and therefore cannot influence answer
     * grounding.
     */
    @NonNull
    public static MatchResult matchImageQuestion(@Nullable String questionText) {
        refreshIfStale();

        String normalizedQuestion = normalize(questionText);
        Set<String> queryTokens = tokenize(normalizedQuestion);
        DetectedLanguage language = detectLanguage(questionText);

        if (queryTokens.size() < MIN_QUERY_TOKENS || indexedPages.isEmpty()) {
            latestVerifiedMatch = null;
            return MatchResult.noMatch(language);
        }

        Candidate best = null;
        Candidate second = null;
        for (IndexedPage page : indexedPages) {
            Candidate candidate = score(page, normalizedQuestion, queryTokens);
            if (candidate.overlap <= 0) {
                continue;
            }
            if (best == null || candidate.score > best.score) {
                second = best;
                best = candidate;
            } else if (second == null || candidate.score > second.score) {
                second = candidate;
            }
        }

        if (best == null) {
            latestVerifiedMatch = null;
            return MatchResult.noMatch(language);
        }

        double margin = second == null ? best.score : best.score - second.score;
        boolean strongOverlap = best.overlap >= MIN_VERIFIED_OVERLAP;
        boolean strongScore = best.score >= MIN_VERIFIED_SCORE;
        boolean unambiguous = margin >= MIN_SCORE_MARGIN || best.score >= 86.0;

        if (!strongOverlap || !strongScore || !unambiguous) {
            latestVerifiedMatch = null;
            return MatchResult.possible(
                    language,
                    best.page.subjectName,
                    best.page.bookTitle,
                    best.page.chapterTitle,
                    best.page.pageNumber,
                    Math.min(79, (int) Math.round(best.score))
            );
        }

        int confidence = Math.max(80, Math.min(99,
                (int) Math.round(80 + Math.min(19, (best.score - MIN_VERIFIED_SCORE) * 0.45))));

        MatchResult verified = MatchResult.verified(best.page, language, confidence);
        latestVerifiedMatch = verified;
        return verified;
    }

    /** Clears any image match before a text-only request is prepared. */
    public static void clearLatestMatch() {
        latestVerifiedMatch = null;
    }

    /**
     * One-shot fallback used by the answer grounding validator. The reference
     * expires quickly and is consumed so it cannot leak into a later request.
     */
    @NonNull
    public static String consumeLatestVerifiedReference() {
        MatchResult match = latestVerifiedMatch;
        latestVerifiedMatch = null;
        if (match == null || !match.hasVerifiedPage()) {
            return "";
        }
        if (System.currentTimeMillis() - match.createdAt > MATCH_EXPIRY_MS) {
            return "";
        }
        return match.getVerifiedReference();
    }

    @NonNull
    private static List<IndexedPage> buildIndex(@NonNull Context context) {
        StudySaathiDatabase database = StudySaathiDatabase.getInstance(context);
        StudentProfileEntity profile = database.studentProfileDao().getActiveProfile();
        if (profile == null || profile.getProfileId() <= 0L) {
            return Collections.emptyList();
        }

        List<SchoolSubjectEntity> subjects =
                database.schoolSubjectDao().getEnabledSubjectsForProfile(profile.getProfileId());
        if (subjects == null || subjects.isEmpty()) {
            return Collections.emptyList();
        }

        List<IndexedPage> pages = new ArrayList<>();
        for (SchoolSubjectEntity subject : subjects) {
            if (subject == null || pages.size() >= MAX_INDEXED_PAGES) break;

            List<SchoolBookEntity> books =
                    database.schoolBookDao().getActiveBooksForSubject(subject.getSubjectRowId());
            if (books == null) continue;

            for (SchoolBookEntity book : books) {
                if (book == null || pages.size() >= MAX_INDEXED_PAGES) break;

                List<SchoolBookChapterEntity> chapters =
                        database.schoolBookChapterDao().getChildModeChaptersForBook(book.getBookRowId());
                if (chapters == null) continue;

                for (SchoolBookChapterEntity chapter : chapters) {
                    if (chapter == null || pages.size() >= MAX_INDEXED_PAGES) break;

                    List<SchoolBookChapterPageEntity> approvedPages =
                            database.schoolBookChapterPageDao()
                                    .getApprovedPagesForChapter(chapter.getChapterRowId());
                    if (approvedPages == null) continue;

                    for (SchoolBookChapterPageEntity page : approvedPages) {
                        if (page == null || pages.size() >= MAX_INDEXED_PAGES) break;
                        if (!page.isParentApproved()
                                || page.getSourceDocumentPageNumber() <= 0) {
                            continue;
                        }

                        String content = buildPageContent(page);
                        if (content.isEmpty()) continue;

                        pages.add(new IndexedPage(
                                firstNonEmpty(subject.getSubjectNameEnglish(),
                                        subject.getSubjectNameHindi()),
                                safe(book.getBookTitle()),
                                firstNonEmpty(chapter.getChapterTitleEnglish(),
                                        chapter.getChapterTitleHindi()),
                                page.getSourceDocumentPageNumber(),
                                safe(page.getPageTitle()),
                                content
                        ));
                    }
                }
            }
        }
        return pages;
    }

    @NonNull
    private static String buildPageContent(@NonNull SchoolBookChapterPageEntity page) {
        StringBuilder content = new StringBuilder();
        append(content, page.getRawOcrText());
        append(content, page.getPageTitle());
        append(content, page.getIntroductionEnglish());
        append(content, page.getIntroductionHindi());
        append(content, page.getExplanationEnglish());
        append(content, page.getExplanationHindi());
        append(content, page.getKeyPointsEnglish());
        append(content, page.getKeyPointsHindi());
        append(content, page.getExamplesEnglish());
        append(content, page.getExamplesHindi());
        append(content, page.getSummaryEnglish());
        append(content, page.getSummaryHindi());
        String value = content.toString().trim();
        return value.length() <= MAX_INDEX_TEXT_CHARS
                ? value
                : value.substring(0, MAX_INDEX_TEXT_CHARS).trim();
    }

    private static void append(@NonNull StringBuilder target, @Nullable String value) {
        String text = safe(value);
        if (text.isEmpty() || target.length() >= MAX_INDEX_TEXT_CHARS) return;
        if (target.length() > 0) target.append('\n');
        target.append(text);
    }

    @NonNull
    private static Candidate score(@NonNull IndexedPage page,
                                   @NonNull String normalizedQuestion,
                                   @NonNull Set<String> queryTokens) {
        int overlap = intersectionCount(queryTokens, page.contentTokens);
        if (overlap <= 0) return new Candidate(page, 0, 0.0);

        double coverage = overlap / (double) Math.max(1, Math.min(queryTokens.size(), 18));
        double score = overlap * 8.0 + coverage * 30.0;

        if (normalizedQuestion.length() >= 24
                && page.normalizedContent.contains(normalizedQuestion)) {
            score += 34.0;
        }

        score += intersectionCount(queryTokens, page.subjectTokens) * 4.0;
        score += intersectionCount(queryTokens, page.bookTokens) * 3.0;
        score += intersectionCount(queryTokens, page.chapterTokens) * 5.0;
        score += intersectionCount(queryTokens, page.titleTokens) * 5.0;

        return new Candidate(page, overlap, score);
    }

    private static int intersectionCount(@NonNull Set<String> left,
                                         @NonNull Set<String> right) {
        int count = 0;
        for (String token : left) if (right.contains(token)) count++;
        return count;
    }

    @NonNull
    private static Set<String> tokenize(@Nullable String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) return Collections.emptySet();
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 3 || STOP_WORDS.contains(token)) continue;
            tokens.add(token);
        }
        return tokens;
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        String text = safe(value).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) return "";
        return text.replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @NonNull
    private static DetectedLanguage detectLanguage(@Nullable String value) {
        String text = safe(value);
        int devanagari = 0;
        int latin = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(character);
            if (block == Character.UnicodeBlock.DEVANAGARI) {
                devanagari++;
            } else if ((character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')) {
                latin++;
            }
        }
        if (devanagari >= 3 && latin >= 3) return DetectedLanguage.HINGLISH;
        if (devanagari >= 3) return DetectedLanguage.HINDI;
        if (latin >= 3) return DetectedLanguage.ENGLISH;
        return DetectedLanguage.UNKNOWN;
    }

    @NonNull
    private static String firstNonEmpty(@Nullable String first, @Nullable String second) {
        String firstValue = safe(first);
        return firstValue.isEmpty() ? safe(second) : firstValue;
    }

    @NonNull
    private static String safe(@Nullable Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public enum DetectedLanguage {
        ENGLISH,
        HINDI,
        HINGLISH,
        UNKNOWN
    }

    public static final class MatchResult {
        @NonNull private final DetectedLanguage detectedLanguage;
        @NonNull private final String subjectName;
        @NonNull private final String bookTitle;
        @NonNull private final String chapterTitle;
        private final int pageNumber;
        private final int confidence;
        @NonNull private final String verifiedReference;
        private final boolean verified;
        private final long createdAt;

        private MatchResult(@NonNull DetectedLanguage detectedLanguage,
                            @NonNull String subjectName,
                            @NonNull String bookTitle,
                            @NonNull String chapterTitle,
                            int pageNumber,
                            int confidence,
                            @NonNull String verifiedReference,
                            boolean verified) {
            this.detectedLanguage = detectedLanguage;
            this.subjectName = subjectName;
            this.bookTitle = bookTitle;
            this.chapterTitle = chapterTitle;
            this.pageNumber = Math.max(0, pageNumber);
            this.confidence = Math.max(0, Math.min(100, confidence));
            this.verifiedReference = verifiedReference;
            this.verified = verified;
            this.createdAt = System.currentTimeMillis();
        }

        @NonNull
        static MatchResult noMatch(@NonNull DetectedLanguage language) {
            return new MatchResult(language, "", "", "", 0, 0, "", false);
        }

        @NonNull
        static MatchResult possible(@NonNull DetectedLanguage language,
                                    @NonNull String subjectName,
                                    @NonNull String bookTitle,
                                    @NonNull String chapterTitle,
                                    int pageNumber,
                                    int confidence) {
            return new MatchResult(language, subjectName, bookTitle, chapterTitle,
                    pageNumber, confidence, "", false);
        }

        @NonNull
        static MatchResult verified(@NonNull IndexedPage page,
                                    @NonNull DetectedLanguage language,
                                    int confidence) {
            String reference = ExactBookPageCitationBuilder.build(
                    Collections.singletonList(new ExactBookPageCitationBuilder.PageReference(
                            page.pageNumber, page.pageTitle, page.content)));
            return new MatchResult(language, page.subjectName, page.bookTitle,
                    page.chapterTitle, page.pageNumber, confidence, reference,
                    !reference.isEmpty());
        }

        public boolean hasVerifiedPage() {
            return verified && pageNumber > 0 && !verifiedReference.isEmpty();
        }

        @NonNull
        public String getVerifiedReference() {
            return verifiedReference;
        }

        @NonNull
        public String getPromptInstruction() {
            if (!hasVerifiedPage()) return "";
            return "PHOTO BOOK CONTEXT: local OCR text strongly matches the student's "
                    + "parent-approved book page. Subject=" + subjectName
                    + "; Book=" + bookTitle
                    + "; Chapter=" + chapterTitle
                    + "; Page=" + pageNumber
                    + "; Detected language=" + detectedLanguage.name()
                    + "; Match confidence=" + confidence + "% .\n"
                    + "Treat only the VERIFIED_BOOK_PAGE block below as exact book evidence. "
                    + "If the visible photo conflicts with it, say the photo needs verification; "
                    + "never invent a different page number. When using exact book facts, cite "
                    + "Page " + pageNumber + ".\n"
                    + verifiedReference;
        }
    }

    private static final class IndexedPage {
        @NonNull private final String subjectName;
        @NonNull private final String bookTitle;
        @NonNull private final String chapterTitle;
        private final int pageNumber;
        @NonNull private final String pageTitle;
        @NonNull private final String content;
        @NonNull private final String normalizedContent;
        @NonNull private final Set<String> contentTokens;
        @NonNull private final Set<String> subjectTokens;
        @NonNull private final Set<String> bookTokens;
        @NonNull private final Set<String> chapterTokens;
        @NonNull private final Set<String> titleTokens;

        private IndexedPage(@NonNull String subjectName,
                            @NonNull String bookTitle,
                            @NonNull String chapterTitle,
                            int pageNumber,
                            @NonNull String pageTitle,
                            @NonNull String content) {
            this.subjectName = subjectName;
            this.bookTitle = bookTitle;
            this.chapterTitle = chapterTitle;
            this.pageNumber = pageNumber;
            this.pageTitle = pageTitle;
            this.content = content;
            this.normalizedContent = normalize(content);
            this.contentTokens = tokenize(content);
            this.subjectTokens = tokenize(subjectName);
            this.bookTokens = tokenize(bookTitle);
            this.chapterTokens = tokenize(chapterTitle);
            this.titleTokens = tokenize(pageTitle);
        }
    }

    private static final class Candidate {
        @NonNull private final IndexedPage page;
        private final int overlap;
        private final double score;

        private Candidate(@NonNull IndexedPage page, int overlap, double score) {
            this.page = page;
            this.overlap = overlap;
            this.score = score;
        }
    }
}
