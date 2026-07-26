package com.tridev.studysaathi.data.content.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BookContentsScanResult
        implements Serializable {

    private static final long serialVersionUID =
            1L;

    private final long bookRowId;

    @NonNull
    private final String sourceImagePath;

    @NonNull
    private final String detectedFullText;

    @NonNull
    private final List<ChapterCandidate> chapterCandidates;

    private final long scannedAt;

    private final boolean successful;

    @NonNull
    private final String errorMessage;

    private BookContentsScanResult(
            long bookRowId,
            @NonNull String sourceImagePath,
            @NonNull String detectedFullText,
            @NonNull List<ChapterCandidate> chapterCandidates,
            long scannedAt,
            boolean successful,
            @NonNull String errorMessage
    ) {
        this.bookRowId =
                Math.max(
                        0L,
                        bookRowId
                );

        this.sourceImagePath =
                safeText(
                        sourceImagePath
                );

        this.detectedFullText =
                normalizeMultilineText(
                        detectedFullText
                );

        this.chapterCandidates =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                chapterCandidates
                        )
                );

        this.scannedAt =
                Math.max(
                        0L,
                        scannedAt
                );

        this.successful =
                successful;

        this.errorMessage =
                safeText(
                        errorMessage
                );
    }

    @NonNull
    public static BookContentsScanResult success(
            long bookRowId,
            @Nullable String sourceImagePath,
            @Nullable String detectedFullText,
            @Nullable List<ChapterCandidate> chapterCandidates
    ) {
        List<ChapterCandidate> safeCandidates =
                chapterCandidates == null
                        ? Collections.emptyList()
                        : chapterCandidates;

        return new BookContentsScanResult(
                bookRowId,
                safeText(
                        sourceImagePath
                ),
                normalizeMultilineText(
                        detectedFullText
                ),
                safeCandidates,
                System.currentTimeMillis(),
                true,
                ""
        );
    }

    @NonNull
    public static BookContentsScanResult failure(
            long bookRowId,
            @Nullable String sourceImagePath,
            @Nullable String errorMessage
    ) {
        String safeErrorMessage =
                safeText(
                        errorMessage
                );

        if (safeErrorMessage.isEmpty()) {
            safeErrorMessage =
                    "The contents page could not be scanned.";
        }

        return new BookContentsScanResult(
                bookRowId,
                safeText(
                        sourceImagePath
                ),
                "",
                Collections.emptyList(),
                System.currentTimeMillis(),
                false,
                safeErrorMessage
        );
    }

    public long getBookRowId() {
        return bookRowId;
    }

    @NonNull
    public String getSourceImagePath() {
        return sourceImagePath;
    }

    @NonNull
    public String getDetectedFullText() {
        return detectedFullText;
    }

    @NonNull
    public List<ChapterCandidate> getChapterCandidates() {
        return chapterCandidates;
    }

    public long getScannedAt() {
        return scannedAt;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @NonNull
    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasDetectedText() {
        return !detectedFullText.isEmpty();
    }

    public boolean hasChapterCandidates() {
        return !chapterCandidates.isEmpty();
    }

    public int getCandidateCount() {
        return chapterCandidates.size();
    }

    public boolean isUsable() {
        return successful
                && bookRowId > 0L
                && hasDetectedText()
                && hasChapterCandidates();
    }

    public static final class ChapterCandidate
            implements Serializable {

        private static final long serialVersionUID =
                1L;

        @NonNull
        private final String chapterNumber;

        @NonNull
        private final String chapterTitle;

        private final int startPageNumber;

        private final int sourceLineNumber;

        @NonNull
        private final String rawDetectedLine;

        private final float confidence;

        private boolean selected;

        private ChapterCandidate(
                @NonNull String chapterNumber,
                @NonNull String chapterTitle,
                int startPageNumber,
                int sourceLineNumber,
                @NonNull String rawDetectedLine,
                float confidence,
                boolean selected
        ) {
            this.chapterNumber =
                    safeText(
                            chapterNumber
                    );

            this.chapterTitle =
                    safeText(
                            chapterTitle
                    );

            this.startPageNumber =
                    Math.max(
                            0,
                            startPageNumber
                    );

            this.sourceLineNumber =
                    Math.max(
                            0,
                            sourceLineNumber
                    );

            this.rawDetectedLine =
                    safeText(
                            rawDetectedLine
                    );

            this.confidence =
                    normalizeConfidence(
                            confidence
                    );

            this.selected =
                    selected;
        }

        @NonNull
        public static ChapterCandidate create(
                @Nullable String chapterNumber,
                @Nullable String chapterTitle,
                int startPageNumber,
                int sourceLineNumber,
                @Nullable String rawDetectedLine,
                float confidence
        ) {
            return new ChapterCandidate(
                    safeText(
                            chapterNumber
                    ),
                    safeText(
                            chapterTitle
                    ),
                    startPageNumber,
                    sourceLineNumber,
                    safeText(
                            rawDetectedLine
                    ),
                    confidence,
                    true
            );
        }

        @NonNull
        public String getChapterNumber() {
            return chapterNumber;
        }

        @NonNull
        public String getChapterTitle() {
            return chapterTitle;
        }

        public int getStartPageNumber() {
            return startPageNumber;
        }

        public int getSourceLineNumber() {
            return sourceLineNumber;
        }

        @NonNull
        public String getRawDetectedLine() {
            return rawDetectedLine;
        }

        public float getConfidence() {
            return confidence;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(
                boolean selected
        ) {
            this.selected =
                    selected;
        }

        public boolean hasChapterNumber() {
            return !chapterNumber.isEmpty();
        }

        public boolean hasValidTitle() {
            return !chapterTitle.isEmpty();
        }

        public boolean hasPageNumber() {
            return startPageNumber > 0;
        }

        public boolean isUsable() {
            return hasValidTitle()
                    && confidence > 0F;
        }

        @NonNull
        public String getDisplayLabel() {
            if (!chapterNumber.isEmpty()) {
                return "Chapter "
                        + chapterNumber
                        + " — "
                        + chapterTitle;
            }

            return chapterTitle;
        }

        @NonNull
        public String getPageLabel() {
            if (startPageNumber <= 0) {
                return "";
            }

            return "Page "
                    + startPageNumber;
        }
    }

    private static float normalizeConfidence(
            float confidence
    ) {
        if (Float.isNaN(
                confidence
        )
                || Float.isInfinite(
                confidence
        )) {

            return 0F;
        }

        return Math.max(
                0F,
                Math.min(
                        1F,
                        confidence
                )
        );
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    private static String normalizeMultilineText(
            @Nullable Object value
    ) {
        if (value == null) {
            return "";
        }

        return value.toString()
                .replace(
                        "\r\n",
                        "\n"
                )
                .replace(
                        "\r",
                        "\n"
                )
                .trim();
    }
}