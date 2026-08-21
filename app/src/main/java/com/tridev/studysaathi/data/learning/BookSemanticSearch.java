package com.tridev.studysaathi.data.learning;

import androidx.annotation.NonNull;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lightweight offline semantic ranking for parent-approved book pages. */
public final class BookSemanticSearch {
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "and", "are", "explain", "for", "how", "in", "is", "of", "the",
            "this", "to", "what", "why", "with", "about", "tell", "me",
            "और", "का", "की", "के", "को", "क्या", "कैसे", "क्यों", "में", "से", "यह",
            "ये", "पर", "है", "हैं", "समझाओ", "बताओ", "मुझे"
    ));

    private BookSemanticSearch() { }

    @NonNull
    public static List<ExactBookPageCitationBuilder.PageReference> findRelevantPages(
            @NonNull String query,
            @NonNull List<ExactBookPageCitationBuilder.PageReference> pages,
            int limit) {
        if (limit <= 0 || pages.isEmpty()) return Collections.emptyList();
        List<String> queryTerms = meaningfulTerms(query);
        if (queryTerms.isEmpty()) return Collections.emptyList();

        String normalizedQuery = normalize(query);
        List<ScoredPage> scored = new ArrayList<>();
        for (int index = 0; index < pages.size(); index++) {
            ExactBookPageCitationBuilder.PageReference page = pages.get(index);
            if (page == null || page.getPageNumber() <= 0 || page.getContent().isEmpty()) continue;
            String title = normalize(page.getTitle());
            String content = normalize(page.getContent());
            Set<String> titleTerms = new HashSet<>(meaningfulTerms(title));
            Set<String> contentTerms = new HashSet<>(meaningfulTerms(content));
            int score = 0;
            int matchedTerms = 0;
            for (String term : queryTerms) {
                boolean inTitle = titleTerms.contains(term);
                boolean inContent = contentTerms.contains(term);
                if (inTitle || inContent) matchedTerms++;
                if (inTitle) score += 8;
                if (inContent) score += 3;
            }
            if (!normalizedQuery.isEmpty() && title.contains(normalizedQuery)) score += 12;
            if (!normalizedQuery.isEmpty() && content.contains(normalizedQuery)) score += 8;
            score += matchedTerms * matchedTerms;
            if (score > 0) scored.add(new ScoredPage(page, score, matchedTerms, index));
        }

        scored.sort(Comparator
                .comparingInt(ScoredPage::getScore).reversed()
                .thenComparing(Comparator.comparingInt(ScoredPage::getMatchedTerms).reversed())
                .thenComparingInt(ScoredPage::getOriginalIndex));
        List<ExactBookPageCitationBuilder.PageReference> result = new ArrayList<>();
        for (ScoredPage page : scored) {
            if (result.size() >= limit) break;
            result.add(page.reference);
        }
        return result;
    }

    @NonNull
    private static List<String> meaningfulTerms(@NonNull String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) return Collections.emptyList();
        Set<String> unique = new LinkedHashSet<>();
        for (String term : normalized.split("\\s+")) {
            if (term.length() >= 2 && !STOP_WORDS.contains(term)) unique.add(term);
        }
        return new ArrayList<>(unique);
    }

    @NonNull
    private static String normalize(@NonNull String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private static final class ScoredPage {
        private final ExactBookPageCitationBuilder.PageReference reference;
        private final int score;
        private final int matchedTerms;
        private final int originalIndex;

        private ScoredPage(ExactBookPageCitationBuilder.PageReference reference,
                           int score, int matchedTerms, int originalIndex) {
            this.reference = reference;
            this.score = score;
            this.matchedTerms = matchedTerms;
            this.originalIndex = originalIndex;
        }

        private int getScore() { return score; }
        private int getMatchedTerms() { return matchedTerms; }
        private int getOriginalIndex() { return originalIndex; }
    }
}
