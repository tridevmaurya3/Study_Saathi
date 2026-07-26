package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.content.scanner
        .BookPageOcrScanner;
import com.tridev.studysaathi.data.content.storage
        .BookPageImageStore;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterPageEntity;
import com.tridev.studysaathi.mapper
        .BookOcrPageEntityMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Parent-approved chapter ranges को page-wise Kinder reader drafts में
 * बदलकर सुरक्षित रूप से save करता है।
 */
public final class BookChapterPageImportCoordinator {

    private static final int MINIMUM_SAFE_MATCH_SCORE = 50;

    @NonNull
    private final SchoolBookChapterRepository
            chapterRepository;

    @NonNull
    private final SchoolBookChapterPageRepository
            pageRepository;

    @NonNull
    private final BookPageImageStore imageStore;

    @NonNull
    private final ExecutorService fileExecutor;

    @NonNull
    private final Handler mainThreadHandler;

    public BookChapterPageImportCoordinator(
            @NonNull Context context
    ) {
        Context appContext =
                context.getApplicationContext();

        chapterRepository =
                new SchoolBookChapterRepository(
                        appContext
                );

        pageRepository =
                new SchoolBookChapterPageRepository(
                        appContext
                );

        imageStore =
                new BookPageImageStore(
                        appContext
                );

        fileExecutor =
                Executors.newSingleThreadExecutor();

        mainThreadHandler =
                new Handler(Looper.getMainLooper());
    }

    public void importPageDrafts(
            long schoolBookRowId,
            @NonNull BookPageOcrScanner.BookOcrResult
                    ocrResult,
            @NonNull List<BoundaryInput> approvedBoundaries,
            @NonNull ImportCallback callback
    ) {
        if (schoolBookRowId <= 0L
                || ocrResult.getSchoolBookRowId()
                != schoolBookRowId) {
            callback.onError(
                    new IllegalArgumentException(
                            "Exact book and OCR result do not match."
                    )
            );
            return;
        }

        if (approvedBoundaries.isEmpty()) {
            callback.onError(
                    new IllegalArgumentException(
                            "At least one approved chapter "
                                    + "boundary is required."
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
                        List<MatchedChapter> matches =
                                matchChapters(
                                        chapters,
                                        approvedBoundaries
                                );

                        int unmatchedCount =
                                approvedBoundaries.size()
                                        - matches.size();

                        importNextChapter(
                                schoolBookRowId,
                                ocrResult,
                                matches,
                                0,
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

    public void close() {
        fileExecutor.shutdownNow();
    }

    private void importNextChapter(
            long schoolBookRowId,
            @NonNull BookPageOcrScanner.BookOcrResult ocrResult,
            @NonNull List<MatchedChapter> matches,
            int matchIndex,
            int importedChapterCount,
            int importedPageCount,
            int protectedChapterCount,
            int unmatchedChapterCount,
            @NonNull ImportCallback callback
    ) {
        if (matchIndex >= matches.size()) {
            callback.onSuccess(
                    new ImportResult(
                            importedChapterCount,
                            importedPageCount,
                            protectedChapterCount,
                            unmatchedChapterCount
                    )
            );
            return;
        }

        MatchedChapter match =
                matches.get(matchIndex);

        pageRepository.getPagesForChapter(
                match.chapter.getChapterRowId(),
                new SchoolBookChapterPageRepository
                        .PagesCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull List<SchoolBookChapterPageEntity>
                                    existingPages
                    ) {
                        if (!existingPages.isEmpty()) {
                            importNextChapter(
                                    schoolBookRowId,
                                    ocrResult,
                                    matches,
                                    matchIndex + 1,
                                    importedChapterCount,
                                    importedPageCount,
                                    protectedChapterCount + 1,
                                    unmatchedChapterCount,
                                    callback
                            );
                            return;
                        }

                        prepareChapterPages(
                                schoolBookRowId,
                                ocrResult,
                                matches,
                                matchIndex,
                                importedChapterCount,
                                importedPageCount,
                                protectedChapterCount,
                                unmatchedChapterCount,
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

    private void prepareChapterPages(
            long schoolBookRowId,
            @NonNull BookPageOcrScanner.BookOcrResult ocrResult,
            @NonNull List<MatchedChapter> matches,
            int matchIndex,
            int importedChapterCount,
            int importedPageCount,
            int protectedChapterCount,
            int unmatchedChapterCount,
            @NonNull ImportCallback callback
    ) {
        MatchedChapter match =
                matches.get(matchIndex);

        fileExecutor.execute(() -> {
            try {
                ArrayList<SchoolBookChapterPageEntity>
                        pageEntities =
                        createPageEntities(
                                schoolBookRowId,
                                ocrResult,
                                match
                        );

                if (pageEntities.isEmpty()) {
                    throw new IllegalStateException(
                            "No OCR pages were found inside "
                                    + match.boundary.title
                                    + "."
                    );
                }

                mainThreadHandler.post(() ->
                        savePreparedPages(
                                schoolBookRowId,
                                ocrResult,
                                matches,
                                matchIndex,
                                pageEntities,
                                importedChapterCount,
                                importedPageCount,
                                protectedChapterCount,
                                unmatchedChapterCount,
                                callback
                        )
                );
            } catch (Exception exception) {
                mainThreadHandler.post(
                        () -> callback.onError(exception)
                );
            }
        });
    }

    @NonNull
    private ArrayList<SchoolBookChapterPageEntity>
    createPageEntities(
            long schoolBookRowId,
            @NonNull BookPageOcrScanner.BookOcrResult ocrResult,
            @NonNull MatchedChapter match
    ) throws Exception {
        ArrayList<SchoolBookChapterPageEntity> entities =
                new ArrayList<>();

        int pageOrderInsideChapter = 1;

        for (BookPageOcrScanner.PageOcrText sourcePage
                : ocrResult.getPages()) {

            if (sourcePage.getPageNumber()
                    < match.boundary.startPage
                    || sourcePage.getPageNumber()
                    > match.boundary.endPage) {
                continue;
            }

            String persistentImagePath =
                    imageStore.persistPageImage(
                            schoolBookRowId,
                            match.chapter
                                    .getChapterRowId(),
                            ocrResult.getRequestId(),
                            pageOrderInsideChapter,
                            sourcePage.getPageNumber(),
                            sourcePage.getPageImagePath()
                    );

            SchoolBookChapterPageEntity entity =
                    BookOcrPageEntityMapper
                            .toPageEntity(
                                    ocrResult,
                                    sourcePage,
                                    match.boundary.order,
                                    match.boundary.title,
                                    pageOrderInsideChapter,
                                    persistentImagePath
                            );

            entities.add(entity);
            pageOrderInsideChapter++;
        }

        return entities;
    }

    private void savePreparedPages(
            long schoolBookRowId,
            @NonNull BookPageOcrScanner.BookOcrResult ocrResult,
            @NonNull List<MatchedChapter> matches,
            int matchIndex,
            @NonNull List<SchoolBookChapterPageEntity> pageEntities,
            int importedChapterCount,
            int importedPageCount,
            int protectedChapterCount,
            int unmatchedChapterCount,
            @NonNull ImportCallback callback
    ) {
        MatchedChapter match =
                matches.get(matchIndex);

        pageRepository.saveNewPagesForParentReview(
                match.chapter.getChapterRowId(),
                pageEntities,
                new SchoolBookChapterPageRepository
                        .SavePagesCallback() {

                    @Override
                    public void onSuccess(
                            int savedPageCount
                    ) {
                        callback.onProgress(
                                matchIndex + 1,
                                matches.size(),
                                match.boundary.title,
                                savedPageCount
                        );

                        importNextChapter(
                                schoolBookRowId,
                                ocrResult,
                                matches,
                                matchIndex + 1,
                                importedChapterCount + 1,
                                importedPageCount
                                        + savedPageCount,
                                protectedChapterCount,
                                unmatchedChapterCount,
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
    private static List<MatchedChapter> matchChapters(
            @NonNull List<SchoolBookChapterEntity> chapters,
            @NonNull List<BoundaryInput> boundaries
    ) {
        ArrayList<MatchedChapter> matches =
                new ArrayList<>();

        Set<Long> usedChapterIds =
                new HashSet<>();

        for (BoundaryInput boundary : boundaries) {
            SchoolBookChapterEntity bestChapter = null;
            int bestScore = 0;

            for (int chapterIndex = 0;
                 chapterIndex < chapters.size();
                 chapterIndex++) {

                SchoolBookChapterEntity chapter =
                        chapters.get(chapterIndex);

                if (chapter.getChapterRowId() <= 0L
                        || usedChapterIds.contains(
                        chapter.getChapterRowId()
                )) {
                    continue;
                }

                int score =
                        calculateMatchScore(
                                chapter,
                                chapterIndex,
                                boundary
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

            usedChapterIds.add(
                    bestChapter.getChapterRowId()
            );

            matches.add(
                    new MatchedChapter(
                            bestChapter,
                            boundary
                    )
            );
        }

        return matches;
    }

    private static int calculateMatchScore(
            @NonNull SchoolBookChapterEntity chapter,
            int chapterIndex,
            @NonNull BoundaryInput boundary
    ) {
        int score = 0;

        if (chapter.getStartPageNumber()
                == boundary.startPage
                && chapter.getEndPageNumber()
                == boundary.endPage) {
            score += 100;
        }

        String targetTitle =
                normalizeTitle(boundary.title);

        String englishTitle =
                normalizeTitle(
                        chapter.getChapterTitleEnglish()
                );

        String hindiTitle =
                normalizeTitle(
                        chapter.getChapterTitleHindi()
                );

        if (!targetTitle.isEmpty()
                && (targetTitle.equals(englishTitle)
                || targetTitle.equals(hindiTitle))) {
            score += 80;
        } else if (meaningfullyContains(
                targetTitle,
                englishTitle
        ) || meaningfullyContains(
                targetTitle,
                hindiTitle
        )) {
            score += 55;
        }

        if (chapterIndex + 1 == boundary.order) {
            score += 30;
        }

        return score;
    }

    @NonNull
    private static String normalizeTitle(
            String value
    ) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "")
                .trim();
    }

    private static boolean meaningfullyContains(
            @NonNull String first,
            @NonNull String second
    ) {
        return first.length() >= 5
                && second.length() >= 5
                && (first.contains(second)
                || second.contains(first));
    }

    public interface ImportCallback {

        void onProgress(
                int completedChapters,
                int totalChapters,
                @NonNull String chapterTitle,
                int savedPageCount
        );

        void onSuccess(
                @NonNull ImportResult result
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class BoundaryInput {

        private final int order;

        @NonNull
        private final String title;

        private final int startPage;
        private final int endPage;

        public BoundaryInput(
                int order,
                @NonNull String title,
                int startPage,
                int endPage
        ) {
            if (order <= 0
                    || title.trim().isEmpty()
                    || startPage <= 0
                    || endPage < startPage) {
                throw new IllegalArgumentException(
                        "Valid chapter boundary details are required."
                );
            }

            this.order = order;
            this.title = title.trim();
            this.startPage = startPage;
            this.endPage = endPage;
        }

        public int getOrder() {
            return order;
        }

        @NonNull
        public String getTitle() {
            return title;
        }

        public int getStartPage() {
            return startPage;
        }

        public int getEndPage() {
            return endPage;
        }
    }

    public static final class ImportResult {

        private final int importedChapterCount;
        private final int importedPageCount;
        private final int protectedChapterCount;
        private final int unmatchedChapterCount;

        private ImportResult(
                int importedChapterCount,
                int importedPageCount,
                int protectedChapterCount,
                int unmatchedChapterCount
        ) {
            this.importedChapterCount =
                    importedChapterCount;
            this.importedPageCount =
                    importedPageCount;
            this.protectedChapterCount =
                    protectedChapterCount;
            this.unmatchedChapterCount =
                    unmatchedChapterCount;
        }

        public int getImportedChapterCount() {
            return importedChapterCount;
        }

        public int getImportedPageCount() {
            return importedPageCount;
        }

        public int getProtectedChapterCount() {
            return protectedChapterCount;
        }

        public int getUnmatchedChapterCount() {
            return unmatchedChapterCount;
        }
    }

    private static final class MatchedChapter {

        @NonNull
        private final SchoolBookChapterEntity chapter;

        @NonNull
        private final BoundaryInput boundary;

        private MatchedChapter(
                @NonNull SchoolBookChapterEntity chapter,
                @NonNull BoundaryInput boundary
        ) {
            this.chapter = chapter;
            this.boundary = boundary;
        }
    }
}
