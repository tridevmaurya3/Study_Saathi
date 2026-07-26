package com.tridev.studysaathi.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.parser
        .BookChapterSectionExtractor;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterContentEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.mapper
        .BookExtractedContentEntityMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Extracted chapter content को existing exact chapter rows से सुरक्षित रूप से
 * match करके नए Parent-review drafts save करता है।
 */
public final class BookExtractedContentSaveCoordinator {

    private static final int MINIMUM_SAFE_MATCH_SCORE = 55;

    @NonNull
    private final SchoolBookChapterRepository
            chapterRepository;

    @NonNull
    private final SchoolBookChapterContentRepository
            contentRepository;

    public BookExtractedContentSaveCoordinator(
            @NonNull Context context
    ) {
        Context appContext =
                context.getApplicationContext();

        chapterRepository =
                new SchoolBookChapterRepository(
                        appContext
                );

        contentRepository =
                new SchoolBookChapterContentRepository(
                        appContext
                );
    }

    public void saveDrafts(
            long schoolBookRowId,
            @NonNull List<BookChapterSectionExtractor
                    .ExtractedChapterContent> extractedContents,
            @NonNull SaveCallback callback
    ) {
        if (schoolBookRowId <= 0L) {
            callback.onError(
                    new IllegalArgumentException(
                            "A valid exact school book is required."
                    )
            );
            return;
        }

        if (extractedContents.isEmpty()) {
            callback.onError(
                    new IllegalArgumentException(
                            "No extracted chapter content is available."
                    )
            );
            return;
        }

        chapterRepository.getChaptersForBook(
                schoolBookRowId,
                new SchoolBookChapterRepository
                        .ChaptersCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolBookChapterEntity>
                                    chapters
                    ) {
                        if (chapters.isEmpty()) {
                            callback.onError(
                                    new IllegalStateException(
                                            "This exact book has no "
                                                    + "existing chapters."
                                    )
                            );
                            return;
                        }

                        List<MatchedDraft> matchedDrafts =
                                matchDrafts(
                                        chapters,
                                        extractedContents
                                );

                        int unmatchedCount =
                                extractedContents.size()
                                        - matchedDrafts.size();

                        saveNextDraft(
                                matchedDrafts,
                                0,
                                0,
                                0,
                                unmatchedCount,
                                callback
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        callback.onError(exception);
                    }
                }
        );
    }

    @NonNull
    private static List<MatchedDraft> matchDrafts(
            @NonNull List<SchoolBookChapterEntity> chapters,
            @NonNull List<BookChapterSectionExtractor
                    .ExtractedChapterContent> extractedContents
    ) {
        ArrayList<MatchedDraft> matches =
                new ArrayList<>();

        Set<Long> usedChapterRowIds =
                new HashSet<>();

        for (BookChapterSectionExtractor
                .ExtractedChapterContent extracted
                : extractedContents) {

            SchoolBookChapterEntity bestChapter =
                    null;

            int bestScore = 0;

            for (int chapterIndex = 0;
                 chapterIndex < chapters.size();
                 chapterIndex++) {

                SchoolBookChapterEntity chapter =
                        chapters.get(chapterIndex);

                if (chapter.getChapterRowId() <= 0L
                        || usedChapterRowIds.contains(
                        chapter.getChapterRowId()
                )) {
                    continue;
                }

                int score =
                        calculateMatchScore(
                                chapter,
                                chapterIndex,
                                extracted
                        );

                if (score > bestScore) {
                    bestScore = score;
                    bestChapter = chapter;
                }
            }

            if (bestChapter == null
                    || bestScore
                    < MINIMUM_SAFE_MATCH_SCORE) {
                continue;
            }

            usedChapterRowIds.add(
                    bestChapter.getChapterRowId()
            );

            matches.add(
                    new MatchedDraft(
                            bestChapter.getChapterRowId(),
                            extracted,
                            bestScore
                    )
            );
        }

        return matches;
    }

    private static int calculateMatchScore(
            @NonNull SchoolBookChapterEntity chapter,
            int chapterIndex,
            @NonNull BookChapterSectionExtractor
                    .ExtractedChapterContent extracted
    ) {
        int score = 0;

        String extractedTitle =
                normalizeTitle(
                        extracted.getChapterTitle()
                );

        String englishTitle =
                normalizeTitle(
                        chapter.getChapterTitleEnglish()
                );

        String hindiTitle =
                normalizeTitle(
                        chapter.getChapterTitleHindi()
                );

        if (!extractedTitle.isEmpty()
                && (extractedTitle.equals(englishTitle)
                || extractedTitle.equals(hindiTitle))) {
            score += 100;
        } else if (hasMeaningfulContainment(
                extractedTitle,
                englishTitle
        ) || hasMeaningfulContainment(
                extractedTitle,
                hindiTitle
        )) {
            score += 70;
        }

        if (chapterIndex + 1
                == extracted.getChapterOrder()) {
            score += 30;
        }

        if (chapter.getStartPageNumber() > 0
                && chapter.getStartPageNumber()
                == extracted.getStartPage()) {
            score += 25;
        }

        if (chapter.getEndPageNumber() > 0
                && chapter.getEndPageNumber()
                == extracted.getEndPage()) {
            score += 20;
        }

        String chapterNumber =
                normalizeNumber(
                        chapter.getChapterNumber()
                );

        if (!chapterNumber.isEmpty()
                && chapterNumber.equals(
                String.valueOf(
                        extracted.getChapterOrder()
                )
        )) {
            score += 20;
        }

        return score;
    }

    private void saveNextDraft(
            @NonNull List<MatchedDraft> matchedDrafts,
            int index,
            int savedCount,
            int existingContentSkippedCount,
            int unmatchedCount,
            @NonNull SaveCallback callback
    ) {
        if (index >= matchedDrafts.size()) {
            callback.onSuccess(
                    new SaveResult(
                            savedCount,
                            existingContentSkippedCount,
                            unmatchedCount
                    )
            );
            return;
        }

        MatchedDraft matchedDraft =
                matchedDrafts.get(index);

        contentRepository.getContentForChapter(
                matchedDraft.chapterRowId,
                new SchoolBookChapterContentRepository
                        .SingleContentCallback() {

                    @Override
                    public void onSuccess(
                            @Nullable SchoolBookChapterContentEntity
                                    existingContent
                    ) {
                        if (existingContent != null) {
                            saveNextDraft(
                                    matchedDrafts,
                                    index + 1,
                                    savedCount,
                                    existingContentSkippedCount + 1,
                                    unmatchedCount,
                                    callback
                            );
                            return;
                        }

                        saveNewDraft(
                                matchedDrafts,
                                index,
                                savedCount,
                                existingContentSkippedCount,
                                unmatchedCount,
                                callback
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        callback.onError(exception);
                    }
                }
        );
    }

    private void saveNewDraft(
            @NonNull List<MatchedDraft> matchedDrafts,
            int index,
            int savedCount,
            int existingContentSkippedCount,
            int unmatchedCount,
            @NonNull SaveCallback callback
    ) {
        MatchedDraft matchedDraft =
                matchedDrafts.get(index);

        SchoolBookChapterContentEntity draft =
                BookExtractedContentEntityMapper
                        .toDraftEntity(
                                matchedDraft.chapterRowId,
                                matchedDraft.extractedContent
                        );

        contentRepository.saveDraft(
                matchedDraft.chapterRowId,
                draft,
                new SchoolBookChapterContentRepository
                        .SaveContentCallback() {

                    @Override
                    public void onSuccess(
                            long contentRowId,
                            boolean created
                    ) {
                        submitSavedDraftForReview(
                                matchedDrafts,
                                index,
                                savedCount,
                                existingContentSkippedCount,
                                unmatchedCount,
                                callback
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        callback.onError(exception);
                    }
                }
        );
    }

    private void submitSavedDraftForReview(
            @NonNull List<MatchedDraft> matchedDrafts,
            int index,
            int savedCount,
            int existingContentSkippedCount,
            int unmatchedCount,
            @NonNull SaveCallback callback
    ) {
        long chapterRowId =
                matchedDrafts.get(index)
                        .chapterRowId;

        contentRepository.submitForParentReview(
                chapterRowId,
                new SchoolBookChapterContentRepository
                        .OperationCallback() {

                    @Override
                    public void onSuccess() {
                        saveNextDraft(
                                matchedDrafts,
                                index + 1,
                                savedCount + 1,
                                existingContentSkippedCount,
                                unmatchedCount,
                                callback
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        callback.onError(exception);
                    }
                }
        );
    }

    @NonNull
    private static String normalizeTitle(
            String value
    ) {
        String safeValue =
                value == null
                        ? ""
                        : value.toLowerCase(Locale.ROOT)
                        .trim();

        return safeValue
                .replaceFirst(
                        "^(chapter|unit|lesson|"
                                + "अध्याय|पाठ|इकाई)"
                                + "\\s*[0-9०-९ivxlcdm]*"
                                + "\\s*[:.\\-–—]?",
                        ""
                )
                .replaceAll("[^\\p{L}\\p{N}]+", "")
                .trim();
    }

    private static boolean hasMeaningfulContainment(
            @NonNull String first,
            @NonNull String second
    ) {
        if (first.length() < 5
                || second.length() < 5) {
            return false;
        }

        return first.contains(second)
                || second.contains(first);
    }

    @NonNull
    private static String normalizeNumber(
            String value
    ) {
        if (value == null) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int index = 0;
             index < value.length();
             index++) {

            char character =
                    value.charAt(index);

            if (character >= '0'
                    && character <= '9') {
                builder.append(character);
            } else if (character >= '\u0966'
                    && character <= '\u096F') {
                builder.append(
                        (char) ('0'
                                + character
                                - '\u0966')
                );
            }
        }

        return builder.toString();
    }

    public interface SaveCallback {

        void onSuccess(
                @NonNull SaveResult result
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class SaveResult {

        private final int savedDraftCount;
        private final int existingContentSkippedCount;
        private final int unmatchedChapterCount;

        private SaveResult(
                int savedDraftCount,
                int existingContentSkippedCount,
                int unmatchedChapterCount
        ) {
            this.savedDraftCount = savedDraftCount;
            this.existingContentSkippedCount =
                    existingContentSkippedCount;
            this.unmatchedChapterCount =
                    unmatchedChapterCount;
        }

        public int getSavedDraftCount() {
            return savedDraftCount;
        }

        public int getExistingContentSkippedCount() {
            return existingContentSkippedCount;
        }

        public int getUnmatchedChapterCount() {
            return unmatchedChapterCount;
        }

        public int getHandledCount() {
            return savedDraftCount
                    + existingContentSkippedCount
                    + unmatchedChapterCount;
        }
    }

    private static final class MatchedDraft {

        private final long chapterRowId;

        @NonNull
        private final BookChapterSectionExtractor
                .ExtractedChapterContent extractedContent;

        @SuppressWarnings("unused")
        private final int matchScore;

        private MatchedDraft(
                long chapterRowId,
                @NonNull BookChapterSectionExtractor
                        .ExtractedChapterContent extractedContent,
                int matchScore
        ) {
            this.chapterRowId = chapterRowId;
            this.extractedContent = extractedContent;
            this.matchScore = matchScore;
        }
    }
}
