package com.tridev.studysaathi.data.content.search;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model.BookCoverScanResult;
import com.tridev.studysaathi.data.content.model.OnlineBookSearchResult;
import com.tridev.studysaathi.data.content.network.NetworkAvailabilityChecker;
import com.tridev.studysaathi.data.content.scanner.BookCoverMetadataExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BookDiscoveryCoordinator
        implements AutoCloseable {

    private static final int DEFAULT_MAXIMUM_RESULTS =
            20;

    private static final int MINIMUM_RESULTS =
            1;

    private static final int MAXIMUM_RESULTS =
            40;

    @NonNull
    private final NetworkAvailabilityChecker
            networkAvailabilityChecker;

    @NonNull
    private final BookCoverMetadataExtractor
            metadataExtractor;

    @NonNull
    private final GoogleBooksSearchClient
            googleBooksSearchClient;

    @NonNull
    private final OnlineBookMatchEvaluator
            matchEvaluator;

    @NonNull
    private final AtomicBoolean searchInProgress;

    @NonNull
    private final AtomicBoolean closed;

    public BookDiscoveryCoordinator(
            @NonNull Context context
    ) {
        this(
                context,
                ""
        );
    }

    /**
     * The Google Books API key is optional.
     *
     * It must be supplied later through secure
     * application configuration and must never
     * be hard-coded directly inside this class.
     */
    public BookDiscoveryCoordinator(
            @NonNull Context context,
            @Nullable String googleBooksApiKey
    ) {
        Context applicationContext =
                context.getApplicationContext();

        networkAvailabilityChecker =
                new NetworkAvailabilityChecker(
                        applicationContext
                );

        metadataExtractor =
                new BookCoverMetadataExtractor();

        googleBooksSearchClient =
                new GoogleBooksSearchClient(
                        googleBooksApiKey
                );

        matchEvaluator =
                new OnlineBookMatchEvaluator();

        searchInProgress =
                new AtomicBoolean(
                        false
                );

        closed =
                new AtomicBoolean(
                        false
                );
    }

    public void discoverBooks(
            @NonNull BookCoverScanResult scanResult,
            @NonNull DiscoveryCallback callback
    ) {
        discoverBooks(
                scanResult,
                BookCoverMetadataExtractor
                        .ExtractionContext
                        .empty(),
                DEFAULT_MAXIMUM_RESULTS,
                callback
        );
    }

    public void discoverBooks(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor
                    .ExtractionContext extractionContext,
            @NonNull DiscoveryCallback callback
    ) {
        discoverBooks(
                scanResult,
                extractionContext,
                DEFAULT_MAXIMUM_RESULTS,
                callback
        );
    }

    /**
     * Performs the complete online book-discovery
     * process:
     *
     * 1. Validates the scan result.
     * 2. Extracts structured metadata.
     * 3. Checks validated internet connectivity.
     * 4. Searches Google Books.
     * 5. Evaluates and ranks all results.
     * 6. Returns the best match for parent review.
     */
    public void discoverBooks(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor
                    .ExtractionContext extractionContext,
            int maximumResults,
            @NonNull DiscoveryCallback callback
    ) {
        if (closed.get()) {
            callback.onDiscoveryFailed(
                    new BookDiscoveryException(
                            FailureReason.CLIENT_CLOSED,
                            "Book discovery is no longer available."
                    )
            );

            return;
        }

        if (!searchInProgress.compareAndSet(
                false,
                true
        )) {
            callback.onDiscoveryFailed(
                    new BookDiscoveryException(
                            FailureReason.SEARCH_ALREADY_RUNNING,
                            "Another book search is already running."
                    )
            );

            return;
        }

        if (!scanResult.isSuccessful()
                || !scanResult
                .isReadyForOnlineSearch()) {

            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.INVALID_SCAN_RESULT,
                            "The scanned cover does not contain "
                                    + "enough readable book information."
                    )
            );

            return;
        }

        BookCoverMetadataExtractor.DetectedBookMetadata
                detectedMetadata;

        try {
            detectedMetadata =
                    metadataExtractor.extract(
                            scanResult,
                            extractionContext
                    );

        } catch (RuntimeException exception) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.METADATA_EXTRACTION_FAILED,
                            "Book information could not be "
                                    + "extracted from the scanned cover.",
                            exception
                    )
            );

            return;
        }

        if (!detectedMetadata
                .isReadyForOnlineSearch()) {

            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason
                                    .NO_SEARCHABLE_METADATA,
                            "A valid book title or ISBN "
                                    + "could not be identified."
                    )
            );

            return;
        }

        NetworkAvailabilityChecker.NetworkState
                networkState =
                networkAvailabilityChecker
                        .getCurrentNetworkState();

        if (networkState.isPermissionMissing()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason
                                    .NETWORK_PERMISSION_MISSING,
                            networkState
                                    .getEnglishStatusMessage()
                    )
            );

            return;
        }

        if (!networkState
                .isSuitableForOnlineBookSearch()) {

            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.NETWORK_UNAVAILABLE,
                            networkState
                                    .getEnglishStatusMessage(),
                            networkState
                                    .getHindiStatusMessage()
                    )
            );

            return;
        }

        int safeMaximumResults =
                Math.max(
                        MINIMUM_RESULTS,
                        Math.min(
                                MAXIMUM_RESULTS,
                                maximumResults
                        )
                );

        googleBooksSearchClient.searchBooks(
                detectedMetadata,
                safeMaximumResults,
                new GoogleBooksSearchClient
                        .SearchCallback() {

                    @Override
                    public void onSearchCompleted(
                            @NonNull GoogleBooksSearchClient
                                    .SearchResponse response
                    ) {
                        handleSearchCompleted(
                                scanResult,
                                detectedMetadata,
                                networkState,
                                response,
                                callback
                        );
                    }

                    @Override
                    public void onSearchFailed(
                            @NonNull Exception exception
                    ) {
                        handleSearchFailed(
                                exception,
                                callback
                        );
                    }
                }
        );
    }

    private void handleSearchCompleted(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata detectedMetadata,
            @NonNull NetworkAvailabilityChecker
                    .NetworkState networkState,
            @NonNull GoogleBooksSearchClient
                    .SearchResponse searchResponse,
            @NonNull DiscoveryCallback callback
    ) {
        if (closed.get()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.CLIENT_CLOSED,
                            "Book discovery was closed before "
                                    + "the search completed."
                    )
            );

            return;
        }

        List<OnlineBookMatchEvaluator.RankedBookResult>
                rankedResults;

        try {
            rankedResults =
                    matchEvaluator.evaluateAndRank(
                            detectedMetadata,
                            searchResponse
                                    .getBookResults()
                    );

        } catch (RuntimeException exception) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason
                                    .MATCH_EVALUATION_FAILED,
                            "Online book results could not "
                                    + "be compared with the scan.",
                            exception
                    )
            );

            return;
        }

        OnlineBookMatchEvaluator.RankedBookResult
                bestMatch =
                rankedResults.isEmpty()
                        ? null
                        : rankedResults.get(
                        0
                );

        List<String> warnings =
                createDiscoveryWarnings(
                        detectedMetadata,
                        searchResponse,
                        rankedResults,
                        bestMatch
                );

        BookDiscoveryResult discoveryResult =
                new BookDiscoveryResult(
                        scanResult,
                        detectedMetadata,
                        networkState,
                        searchResponse.getSearchQuery(),
                        searchResponse.getTotalItems(),
                        searchResponse.getBookResults(),
                        rankedResults,
                        bestMatch,
                        warnings,
                        searchResponse.getSearchedAt()
                );

        searchInProgress.set(
                false
        );

        callback.onDiscoveryCompleted(
                discoveryResult
        );
    }

    private void handleSearchFailed(
            @NonNull Exception exception,
            @NonNull DiscoveryCallback callback
    ) {
        if (exception
                instanceof GoogleBooksSearchClient
                .GoogleBooksSearchException) {

            GoogleBooksSearchClient
                    .GoogleBooksSearchException
                    searchException =
                    (GoogleBooksSearchClient
                            .GoogleBooksSearchException)
                            exception;

            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason
                                    .SEARCH_PROVIDER_FAILED,
                            safeErrorMessage(
                                    searchException
                            ),
                            searchException
                                    .getHttpStatusCode(),
                            searchException
                    )
            );

            return;
        }

        finishWithFailure(
                callback,
                new BookDiscoveryException(
                        FailureReason
                                .SEARCH_PROVIDER_FAILED,
                        safeErrorMessage(
                                exception
                        ),
                        exception
                )
        );
    }

    @NonNull
    private List<String> createDiscoveryWarnings(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata detectedMetadata,
            @NonNull GoogleBooksSearchClient
                    .SearchResponse searchResponse,
            @NonNull List<OnlineBookMatchEvaluator
                    .RankedBookResult> rankedResults,
            @Nullable OnlineBookMatchEvaluator
                    .RankedBookResult bestMatch
    ) {
        List<String> warnings =
                new ArrayList<>(
                        detectedMetadata.getWarnings()
                );

        if (!searchResponse.hasResults()) {
            addUniqueWarning(
                    warnings,
                    "No online book results were found."
            );

            return Collections.unmodifiableList(
                    warnings
            );
        }

        if (rankedResults.isEmpty()) {
            addUniqueWarning(
                    warnings,
                    "Online results were returned, but "
                            + "none could be evaluated."
            );

            return Collections.unmodifiableList(
                    warnings
            );
        }

        if (bestMatch == null) {
            addUniqueWarning(
                    warnings,
                    "A best matching book could not be selected."
            );

            return Collections.unmodifiableList(
                    warnings
            );
        }

        OnlineBookMatchEvaluator.MatchEvaluation
                bestEvaluation =
                bestMatch.getEvaluation();

        for (String warning :
                bestEvaluation.getWarnings()) {

            addUniqueWarning(
                    warnings,
                    warning
            );
        }

        if (!bestEvaluation.isHighConfidence()) {
            addUniqueWarning(
                    warnings,
                    "The best result requires parent review."
            );
        }

        if (!bestEvaluation
                .isAutomaticSelectionRecommended()) {

            addUniqueWarning(
                    warnings,
                    "The book must not be added "
                            + "without parent confirmation."
            );
        }

        OnlineBookSearchResult bestBook =
                bestMatch.getBookResult();

        if (!bestBook
                .isOfficialSourceVerified()) {

            addUniqueWarning(
                    warnings,
                    "The source has not been verified "
                            + "as the official publisher source."
            );
        }

        if (!bestBook.hasAuthorizedDownload()) {
            addUniqueWarning(
                    warnings,
                    "Only metadata or preview may be "
                            + "available for this result."
            );
        }

        return Collections.unmodifiableList(
                warnings
        );
    }

    private void addUniqueWarning(
            @NonNull List<String> warnings,
            @Nullable String warning
    ) {
        if (warning == null) {
            return;
        }

        String safeWarning =
                warning.trim();

        if (safeWarning.isEmpty()) {
            return;
        }

        for (String existingWarning :
                warnings) {

            if (existingWarning
                    .equalsIgnoreCase(
                            safeWarning
                    )) {
                return;
            }
        }

        warnings.add(
                safeWarning
        );
    }

    private void finishWithFailure(
            @NonNull DiscoveryCallback callback,
            @NonNull BookDiscoveryException exception
    ) {
        searchInProgress.set(
                false
        );

        callback.onDiscoveryFailed(
                exception
        );
    }

    @NonNull
    private String safeErrorMessage(
            @NonNull Exception exception
    ) {
        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return "Online book search failed.";
        }

        return message.trim();
    }

    public boolean isSearchInProgress() {
        return searchInProgress.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @NonNull
    public NetworkAvailabilityChecker.NetworkState
    getCurrentNetworkState() {
        return networkAvailabilityChecker
                .getCurrentNetworkState();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(
                false,
                true
        )) {
            return;
        }

        searchInProgress.set(
                false
        );

        googleBooksSearchClient.close();

        networkAvailabilityChecker.close();
    }

    public interface DiscoveryCallback {

        void onDiscoveryCompleted(
                @NonNull BookDiscoveryResult result
        );

        void onDiscoveryFailed(
                @NonNull BookDiscoveryException exception
        );
    }

    public enum FailureReason {

        CLIENT_CLOSED,

        SEARCH_ALREADY_RUNNING,

        INVALID_SCAN_RESULT,

        METADATA_EXTRACTION_FAILED,

        NO_SEARCHABLE_METADATA,

        NETWORK_PERMISSION_MISSING,

        NETWORK_UNAVAILABLE,

        SEARCH_PROVIDER_FAILED,

        MATCH_EVALUATION_FAILED
    }

    public static final class BookDiscoveryResult {

        @NonNull
        private final BookCoverScanResult scanResult;

        @NonNull
        private final BookCoverMetadataExtractor
                .DetectedBookMetadata detectedMetadata;

        @NonNull
        private final NetworkAvailabilityChecker
                .NetworkState networkState;

        @NonNull
        private final String searchQuery;

        private final int totalOnlineItems;

        @NonNull
        private final List<OnlineBookSearchResult>
                rawBookResults;

        @NonNull
        private final List<OnlineBookMatchEvaluator
                .RankedBookResult> rankedBookResults;

        @Nullable
        private final OnlineBookMatchEvaluator
                .RankedBookResult bestMatch;

        @NonNull
        private final List<String> warnings;

        private final long searchedAt;

        private BookDiscoveryResult(
                @NonNull BookCoverScanResult scanResult,
                @NonNull BookCoverMetadataExtractor
                        .DetectedBookMetadata detectedMetadata,
                @NonNull NetworkAvailabilityChecker
                        .NetworkState networkState,
                @NonNull String searchQuery,
                int totalOnlineItems,
                @NonNull List<OnlineBookSearchResult>
                        rawBookResults,
                @NonNull List<OnlineBookMatchEvaluator
                        .RankedBookResult> rankedBookResults,
                @Nullable OnlineBookMatchEvaluator
                        .RankedBookResult bestMatch,
                @NonNull List<String> warnings,
                long searchedAt
        ) {
            this.scanResult =
                    scanResult;

            this.detectedMetadata =
                    detectedMetadata;

            this.networkState =
                    networkState;

            this.searchQuery =
                    searchQuery.trim();

            this.totalOnlineItems =
                    Math.max(
                            0,
                            totalOnlineItems
                    );

            this.rawBookResults =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    rawBookResults
                            )
                    );

            this.rankedBookResults =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    rankedBookResults
                            )
                    );

            this.bestMatch =
                    bestMatch;

            this.warnings =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    warnings
                            )
                    );

            this.searchedAt =
                    searchedAt > 0L
                            ? searchedAt
                            : System.currentTimeMillis();
        }

        @NonNull
        public BookCoverScanResult getScanResult() {
            return scanResult;
        }

        @NonNull
        public BookCoverMetadataExtractor
                .DetectedBookMetadata
        getDetectedMetadata() {
            return detectedMetadata;
        }

        @NonNull
        public NetworkAvailabilityChecker.NetworkState
        getNetworkState() {
            return networkState;
        }

        @NonNull
        public String getSearchQuery() {
            return searchQuery;
        }

        public int getTotalOnlineItems() {
            return totalOnlineItems;
        }

        @NonNull
        public List<OnlineBookSearchResult>
        getRawBookResults() {
            return rawBookResults;
        }

        @NonNull
        public List<OnlineBookMatchEvaluator
                .RankedBookResult>
        getRankedBookResults() {
            return rankedBookResults;
        }

        @Nullable
        public OnlineBookMatchEvaluator
                .RankedBookResult
        getBestMatch() {
            return bestMatch;
        }

        @NonNull
        public List<String> getWarnings() {
            return warnings;
        }

        public long getSearchedAt() {
            return searchedAt;
        }

        public boolean hasResults() {
            return !rankedBookResults.isEmpty();
        }

        public boolean hasBestMatch() {
            return bestMatch != null;
        }

        public boolean hasHighConfidenceMatch() {
            return bestMatch != null
                    && bestMatch.getEvaluation()
                    .isHighConfidence();
        }

        public boolean isAutomaticSelectionRecommended() {
            return bestMatch != null
                    && bestMatch.getEvaluation()
                    .isAutomaticSelectionRecommended();
        }

        public boolean requiresParentReview() {
            return bestMatch == null
                    || bestMatch.getEvaluation()
                    .requiresParentReview()
                    || !warnings.isEmpty();
        }

        public boolean shouldOfferManualEntry() {
            return bestMatch == null
                    || !hasHighConfidenceMatch();
        }

        public boolean bestMatchHasAuthorizedDownload() {
            return bestMatch != null
                    && bestMatch.getBookResult()
                    .hasAuthorizedDownload();
        }

        public boolean bestMatchCanBeAddedAsMetadata() {
            return bestMatch != null
                    && bestMatch.getBookResult()
                    .canBeAddedAsMetadataOnly();
        }
    }

    public static final class BookDiscoveryException
            extends Exception {

        @NonNull
        private final FailureReason failureReason;

        @NonNull
        private final String hindiMessage;

        private final int httpStatusCode;

        public BookDiscoveryException(
                @NonNull FailureReason failureReason,
                @NonNull String message
        ) {
            this(
                    failureReason,
                    message,
                    "",
                    0,
                    null
            );
        }

        public BookDiscoveryException(
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull String hindiMessage
        ) {
            this(
                    failureReason,
                    message,
                    hindiMessage,
                    0,
                    null
            );
        }

        public BookDiscoveryException(
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull Throwable cause
        ) {
            this(
                    failureReason,
                    message,
                    "",
                    0,
                    cause
            );
        }

        public BookDiscoveryException(
                @NonNull FailureReason failureReason,
                @NonNull String message,
                int httpStatusCode,
                @NonNull Throwable cause
        ) {
            this(
                    failureReason,
                    message,
                    "",
                    httpStatusCode,
                    cause
            );
        }

        private BookDiscoveryException(
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull String hindiMessage,
                int httpStatusCode,
                @Nullable Throwable cause
        ) {
            super(
                    message,
                    cause
            );

            this.failureReason =
                    failureReason;

            this.hindiMessage =
                    hindiMessage.trim();

            this.httpStatusCode =
                    Math.max(
                            0,
                            httpStatusCode
                    );
        }

        @NonNull
        public FailureReason getFailureReason() {
            return failureReason;
        }

        @NonNull
        public String getHindiMessage() {
            return hindiMessage;
        }

        public int getHttpStatusCode() {
            return httpStatusCode;
        }

        public boolean isNetworkFailure() {
            return failureReason
                    == FailureReason.NETWORK_UNAVAILABLE
                    || failureReason
                    == FailureReason
                    .NETWORK_PERMISSION_MISSING;
        }

        public boolean isProviderFailure() {
            return failureReason
                    == FailureReason
                    .SEARCH_PROVIDER_FAILED;
        }

        public boolean hasHttpStatusCode() {
            return httpStatusCode > 0;
        }
    }
}