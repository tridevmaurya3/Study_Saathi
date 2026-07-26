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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BookDiscoveryCoordinator implements AutoCloseable {

    private static final int DEFAULT_MAXIMUM_RESULTS = 20;
    private static final int MINIMUM_RESULTS = 1;
    private static final int MAXIMUM_RESULTS = 40;

    @NonNull
    private final NetworkAvailabilityChecker networkAvailabilityChecker;

    @NonNull
    private final BookCoverMetadataExtractor metadataExtractor;

    @NonNull
    private final GoogleBooksSearchClient googleBooksSearchClient;

    @NonNull
    private final OpenLibrarySearchClient openLibrarySearchClient;

    @NonNull
    private final OnlineBookMatchEvaluator matchEvaluator;

    @NonNull
    private final AtomicBoolean searchInProgress = new AtomicBoolean(false);

    @NonNull
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public BookDiscoveryCoordinator(@NonNull Context context) {
        this(context, "");
    }

    /**
     * Google Books key optional है। Key उपलब्ध न होने, HTTP 429 आने,
     * no result मिलने या weak match मिलने पर Open Library fallback चलता है।
     */
    public BookDiscoveryCoordinator(
            @NonNull Context context,
            @Nullable String googleBooksApiKey
    ) {
        Context applicationContext = context.getApplicationContext();

        networkAvailabilityChecker =
                new NetworkAvailabilityChecker(applicationContext);

        metadataExtractor =
                new BookCoverMetadataExtractor();

        googleBooksSearchClient =
                new GoogleBooksSearchClient(googleBooksApiKey);

        openLibrarySearchClient =
                new OpenLibrarySearchClient();

        matchEvaluator =
                new OnlineBookMatchEvaluator();
    }

    public void discoverBooks(
            @NonNull BookCoverScanResult scanResult,
            @NonNull DiscoveryCallback callback
    ) {
        discoverBooks(
                scanResult,
                BookCoverMetadataExtractor.ExtractionContext.empty(),
                DEFAULT_MAXIMUM_RESULTS,
                callback
        );
    }

    public void discoverBooks(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor.ExtractionContext extractionContext,
            @NonNull DiscoveryCallback callback
    ) {
        discoverBooks(
                scanResult,
                extractionContext,
                DEFAULT_MAXIMUM_RESULTS,
                callback
        );
    }

    public void discoverBooks(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor.ExtractionContext extractionContext,
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

        if (!searchInProgress.compareAndSet(false, true)) {
            callback.onDiscoveryFailed(
                    new BookDiscoveryException(
                            FailureReason.SEARCH_ALREADY_RUNNING,
                            "Another book search is already running."
                    )
            );

            return;
        }

        if (!scanResult.isSuccessful()
                || !scanResult.isReadyForOnlineSearch()) {

            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.INVALID_SCAN_RESULT,
                            "The scanned cover does not contain enough readable book information."
                    )
            );

            return;
        }

        final BookCoverMetadataExtractor.DetectedBookMetadata
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
                            "Book information could not be extracted from the scanned cover.",
                            exception
                    )
            );

            return;
        }

        if (!detectedMetadata.isReadyForOnlineSearch()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.NO_SEARCHABLE_METADATA,
                            "A valid book title or ISBN could not be identified."
                    )
            );

            return;
        }

        NetworkAvailabilityChecker.NetworkState networkState =
                networkAvailabilityChecker.getCurrentNetworkState();

        if (networkState.isPermissionMissing()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.NETWORK_PERMISSION_MISSING,
                            networkState.getEnglishStatusMessage()
                    )
            );

            return;
        }

        if (!networkState.isSuitableForOnlineBookSearch()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.NETWORK_UNAVAILABLE,
                            networkState.getEnglishStatusMessage(),
                            networkState.getHindiStatusMessage()
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

        searchGoogleBooks(
                scanResult,
                detectedMetadata,
                networkState,
                safeMaximumResults,
                callback
        );
    }

    private void searchGoogleBooks(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
            @NonNull NetworkAvailabilityChecker.NetworkState networkState,
            int maximumResults,
            @NonNull DiscoveryCallback callback
    ) {
        googleBooksSearchClient.searchBooks(
                detectedMetadata,
                maximumResults,
                new GoogleBooksSearchClient.SearchCallback() {

                    @Override
                    public void onSearchCompleted(
                            @NonNull GoogleBooksSearchClient.SearchResponse response
                    ) {
                        handleGoogleSearchCompleted(
                                scanResult,
                                detectedMetadata,
                                networkState,
                                maximumResults,
                                response,
                                callback
                        );
                    }

                    @Override
                    public void onSearchFailed(
                            @NonNull Exception exception
                    ) {
                        searchOpenLibrary(
                                scanResult,
                                detectedMetadata,
                                networkState,
                                maximumResults,
                                null,
                                exception,
                                createGoogleFailureWarning(exception),
                                callback
                        );
                    }
                }
        );
    }

    private void handleGoogleSearchCompleted(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
            @NonNull NetworkAvailabilityChecker.NetworkState networkState,
            int maximumResults,
            @NonNull GoogleBooksSearchClient.SearchResponse googleResponse,
            @NonNull DiscoveryCallback callback
    ) {
        if (closed.get()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.CLIENT_CLOSED,
                            "Book discovery was closed before the search completed."
                    )
            );

            return;
        }

        if (hasStrongMatch(
                detectedMetadata,
                googleResponse.getBookResults()
        )) {
            completeDiscovery(
                    scanResult,
                    detectedMetadata,
                    networkState,
                    googleResponse.getSearchQuery(),
                    googleResponse.getTotalItems(),
                    googleResponse.getBookResults(),
                    new ArrayList<>(),
                    googleResponse.getSearchedAt(),
                    callback
            );

            return;
        }

        String fallbackWarning =
                googleResponse.hasResults()
                        ? "Google Books result exact या high-confidence match नहीं था। "
                          + "Open Library से अतिरिक्त मिलान खोजे गए।"
                        : "Google Books पर matching result नहीं मिला। "
                          + "Open Library fallback search उपयोग की गई।";

        searchOpenLibrary(
                scanResult,
                detectedMetadata,
                networkState,
                maximumResults,
                googleResponse,
                null,
                fallbackWarning,
                callback
        );
    }

    private void searchOpenLibrary(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
            @NonNull NetworkAvailabilityChecker.NetworkState networkState,
            int maximumResults,
            @Nullable GoogleBooksSearchClient.SearchResponse googleResponse,
            @Nullable Exception googleFailure,
            @Nullable String fallbackWarning,
            @NonNull DiscoveryCallback callback
    ) {
        if (closed.get()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.CLIENT_CLOSED,
                            "Book discovery was closed before the fallback search started."
                    )
            );

            return;
        }

        openLibrarySearchClient.searchBooks(
                detectedMetadata,
                maximumResults,
                new OpenLibrarySearchClient.SearchCallback() {

                    @Override
                    public void onSearchCompleted(
                            @NonNull OpenLibrarySearchClient.SearchResponse
                                    openLibraryResponse
                    ) {
                        handleOpenLibrarySearchCompleted(
                                scanResult,
                                detectedMetadata,
                                networkState,
                                googleResponse,
                                googleFailure,
                                fallbackWarning,
                                openLibraryResponse,
                                callback
                        );
                    }

                    @Override
                    public void onSearchFailed(
                            @NonNull Exception openLibraryFailure
                    ) {
                        handleOpenLibrarySearchFailed(
                                scanResult,
                                detectedMetadata,
                                networkState,
                                googleResponse,
                                googleFailure,
                                fallbackWarning,
                                openLibraryFailure,
                                callback
                        );
                    }
                }
        );
    }

    private void handleOpenLibrarySearchCompleted(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
            @NonNull NetworkAvailabilityChecker.NetworkState networkState,
            @Nullable GoogleBooksSearchClient.SearchResponse googleResponse,
            @Nullable Exception googleFailure,
            @Nullable String fallbackWarning,
            @NonNull OpenLibrarySearchClient.SearchResponse openLibraryResponse,
            @NonNull DiscoveryCallback callback
    ) {
        if (closed.get()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.CLIENT_CLOSED,
                            "Book discovery was closed before the fallback search completed."
                    )
            );

            return;
        }

        List<OnlineBookSearchResult> googleResults =
                googleResponse == null
                        ? new ArrayList<>()
                        : googleResponse.getBookResults();

        List<OnlineBookSearchResult> mergedResults =
                mergeResults(
                        googleResults,
                        openLibraryResponse.getBookResults()
                );

        List<String> providerWarnings =
                new ArrayList<>();

        addUniqueWarning(
                providerWarnings,
                fallbackWarning
        );

        if (googleFailure != null) {
            addUniqueWarning(
                    providerWarnings,
                    createGoogleFailureWarning(googleFailure)
            );
        }

        if (openLibraryResponse.hasResults()) {
            addUniqueWarning(
                    providerWarnings,
                    "Open Library fallback search पूरी हुई। "
                            + "Final result पर Parent confirmation आवश्यक है।"
            );

        } else {
            addUniqueWarning(
                    providerWarnings,
                    "Open Library पर भी matching result नहीं मिला।"
            );
        }

        String combinedQuery =
                createCombinedQuery(
                        googleResponse == null
                                ? ""
                                : googleResponse.getSearchQuery(),
                        openLibraryResponse.getSearchQuery()
                );

        int combinedTotalItems =
                safeAdd(
                        googleResponse == null
                                ? 0
                                : googleResponse.getTotalItems(),
                        openLibraryResponse.getTotalItems()
                );

        long searchedAt =
                Math.max(
                        googleResponse == null
                                ? 0L
                                : googleResponse.getSearchedAt(),
                        openLibraryResponse.getSearchedAt()
                );

        completeDiscovery(
                scanResult,
                detectedMetadata,
                networkState,
                combinedQuery,
                combinedTotalItems,
                mergedResults,
                providerWarnings,
                searchedAt,
                callback
        );
    }

    private void handleOpenLibrarySearchFailed(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
            @NonNull NetworkAvailabilityChecker.NetworkState networkState,
            @Nullable GoogleBooksSearchClient.SearchResponse googleResponse,
            @Nullable Exception googleFailure,
            @Nullable String fallbackWarning,
            @NonNull Exception openLibraryFailure,
            @NonNull DiscoveryCallback callback
    ) {
        if (closed.get()) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.CLIENT_CLOSED,
                            "Book discovery was closed before the fallback search completed."
                    )
            );

            return;
        }

        /*
         * Google Books ने response दिया था, लेकिन Open Library fail हुई।
         * इस स्थिति में Google का उपलब्ध result खोया नहीं जाएगा।
         */
        if (googleResponse != null) {
            List<String> providerWarnings =
                    new ArrayList<>();

            addUniqueWarning(
                    providerWarnings,
                    fallbackWarning
            );

            addUniqueWarning(
                    providerWarnings,
                    "Open Library fallback उपलब्ध नहीं हुई: "
                            + safeErrorMessage(openLibraryFailure)
            );

            completeDiscovery(
                    scanResult,
                    detectedMetadata,
                    networkState,
                    googleResponse.getSearchQuery(),
                    googleResponse.getTotalItems(),
                    googleResponse.getBookResults(),
                    providerWarnings,
                    googleResponse.getSearchedAt(),
                    callback
            );

            return;
        }

        /*
         * दोनों providers fail हुए हैं।
         */
        String googleMessage =
                googleFailure == null
                        ? "Google Books search failed."
                        : safeErrorMessage(googleFailure);

        String openLibraryMessage =
                safeErrorMessage(openLibraryFailure);

        int httpStatusCode =
                firstHttpStatusCode(
                        googleFailure,
                        openLibraryFailure
                );

        finishWithFailure(
                callback,
                new BookDiscoveryException(
                        FailureReason.SEARCH_PROVIDER_FAILED,
                        "All online book providers failed. Google Books: "
                                + googleMessage
                                + " Open Library: "
                                + openLibraryMessage,
                        "Google Books और Open Library दोनों अभी उपलब्ध नहीं हैं। "
                                + "थोड़ी देर बाद फिर प्रयास करें या Book की जानकारी manually डालें।",
                        httpStatusCode,
                        openLibraryFailure
                )
        );
    }

    private void completeDiscovery(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
            @NonNull NetworkAvailabilityChecker.NetworkState networkState,
            @Nullable String searchQuery,
            int totalOnlineItems,
            @NonNull List<OnlineBookSearchResult> rawResults,
            @NonNull List<String> providerWarnings,
            long searchedAt,
            @NonNull DiscoveryCallback callback
    ) {
        final List<OnlineBookMatchEvaluator.RankedBookResult>
                rankedResults;

        try {
            rankedResults =
                    matchEvaluator.evaluateAndRank(
                            detectedMetadata,
                            rawResults
                    );

        } catch (RuntimeException exception) {
            finishWithFailure(
                    callback,
                    new BookDiscoveryException(
                            FailureReason.MATCH_EVALUATION_FAILED,
                            "Online book results could not be compared with the scan.",
                            exception
                    )
            );

            return;
        }

        OnlineBookMatchEvaluator.RankedBookResult bestMatch =
                rankedResults.isEmpty()
                        ? null
                        : rankedResults.get(0);

        List<String> warnings =
                createDiscoveryWarnings(
                        detectedMetadata,
                        rawResults,
                        rankedResults,
                        bestMatch,
                        providerWarnings
                );

        BookDiscoveryResult discoveryResult =
                new BookDiscoveryResult(
                        scanResult,
                        detectedMetadata,
                        networkState,
                        safeText(searchQuery),
                        totalOnlineItems,
                        rawResults,
                        rankedResults,
                        bestMatch,
                        warnings,
                        searchedAt
                );

        searchInProgress.set(false);

        callback.onDiscoveryCompleted(
                discoveryResult
        );
    }

    private boolean hasStrongMatch(
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
            @NonNull List<OnlineBookSearchResult> results
    ) {
        if (results.isEmpty()) {
            return false;
        }

        String scannedIsbn =
                normalizeIsbn(
                        detectedMetadata.getPreferredIsbn()
                );

        /*
         * Exact ISBN मिलने पर result को strong match माना जाएगा।
         */
        if (!scannedIsbn.isEmpty()) {
            for (OnlineBookSearchResult result : results) {
                if (scannedIsbn.equals(
                        normalizeIsbn(
                                result.getPreferredIsbn()
                        )
                )) {
                    return true;
                }
            }
        }

        /*
         * ISBN न होने पर title, author, class, subject और publisher
         * matching evaluator से confidence जाँचा जाएगा।
         */
        try {
            List<OnlineBookMatchEvaluator.RankedBookResult>
                    rankedResults =
                    matchEvaluator.evaluateAndRank(
                            detectedMetadata,
                            results
                    );

            return !rankedResults.isEmpty()
                    && rankedResults
                    .get(0)
                    .getEvaluation()
                    .isHighConfidence();

        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @NonNull
    private List<OnlineBookSearchResult> mergeResults(
            @NonNull List<OnlineBookSearchResult> firstResults,
            @NonNull List<OnlineBookSearchResult> secondResults
    ) {
        Map<String, OnlineBookSearchResult> uniqueResults =
                new LinkedHashMap<>();

        addResultsToMap(
                uniqueResults,
                firstResults
        );

        addResultsToMap(
                uniqueResults,
                secondResults
        );

        return new ArrayList<>(
                uniqueResults.values()
        );
    }

    private void addResultsToMap(
            @NonNull Map<String, OnlineBookSearchResult> destination,
            @NonNull List<OnlineBookSearchResult> results
    ) {
        for (OnlineBookSearchResult result : results) {
            String key =
                    createResultKey(result);

            if (!destination.containsKey(key)) {
                destination.put(
                        key,
                        result
                );
            }
        }
    }

    @NonNull
    private String createResultKey(
            @NonNull OnlineBookSearchResult result
    ) {
        String isbn =
                normalizeIsbn(
                        result.getPreferredIsbn()
                );

        if (!isbn.isEmpty()) {
            return "isbn:" + isbn;
        }

        String providerBookId =
                safeText(
                        result.getProviderBookId()
                );

        if (!providerBookId.isEmpty()) {
            return "provider:"
                    + result.getProvider().name()
                    + ":"
                    + providerBookId;
        }

        return "metadata:"
                + comparisonText(result.getBookTitle())
                + "|"
                + comparisonText(result.getPublisherName())
                + "|"
                + comparisonText(result.getAuthorsDisplayText());
    }

    @NonNull
    private List<String> createDiscoveryWarnings(
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
            @NonNull List<OnlineBookSearchResult> rawResults,
            @NonNull List<OnlineBookMatchEvaluator.RankedBookResult> rankedResults,
            @Nullable OnlineBookMatchEvaluator.RankedBookResult bestMatch,
            @NonNull List<String> providerWarnings
    ) {
        List<String> warnings =
                new ArrayList<>(
                        detectedMetadata.getWarnings()
                );

        for (String providerWarning : providerWarnings) {
            addUniqueWarning(
                    warnings,
                    providerWarning
            );
        }

        if (rawResults.isEmpty()) {
            addUniqueWarning(
                    warnings,
                    "No online book results were found."
            );

            addUniqueWarning(
                    warnings,
                    "Book की जानकारी manually भरें या साफ cover/ISBN photo से फिर search करें।"
            );

            return Collections.unmodifiableList(
                    warnings
            );
        }

        if (rankedResults.isEmpty()) {
            addUniqueWarning(
                    warnings,
                    "Online results were returned, but none could be evaluated."
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

        OnlineBookMatchEvaluator.MatchEvaluation bestEvaluation =
                bestMatch.getEvaluation();

        for (String warning : bestEvaluation.getWarnings()) {
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

        if (!bestEvaluation.isAutomaticSelectionRecommended()) {
            addUniqueWarning(
                    warnings,
                    "The book must not be added without parent confirmation."
            );
        }

        OnlineBookSearchResult bestBook =
                bestMatch.getBookResult();

        if (!bestBook.isOfficialSourceVerified()) {
            addUniqueWarning(
                    warnings,
                    "The source has not been verified as the official publisher source."
            );
        }

        if (!bestBook.hasAuthorizedDownload()) {
            addUniqueWarning(
                    warnings,
                    "Only metadata or preview may be available for this result."
            );
        }

        return Collections.unmodifiableList(
                warnings
        );
    }

    @NonNull
    private String createGoogleFailureWarning(
            @NonNull Exception exception
    ) {
        return getHttpStatusCode(exception) == 429
                ? "Google Books quota समाप्त थी, इसलिए Open Library fallback search उपयोग की गई।"
                : "Google Books उपलब्ध नहीं था, इसलिए Open Library fallback search उपयोग की गई।";
    }

    private int firstHttpStatusCode(
            @Nullable Exception firstException,
            @Nullable Exception secondException
    ) {
        int firstCode =
                getHttpStatusCode(firstException);

        return firstCode > 0
                ? firstCode
                : getHttpStatusCode(secondException);
    }

    private int getHttpStatusCode(
            @Nullable Exception exception
    ) {
        if (exception
                instanceof GoogleBooksSearchClient.GoogleBooksSearchException) {

            return ((GoogleBooksSearchClient.GoogleBooksSearchException)
                    exception)
                    .getHttpStatusCode();
        }

        if (exception
                instanceof OpenLibrarySearchClient.OpenLibrarySearchException) {

            return ((OpenLibrarySearchClient.OpenLibrarySearchException)
                    exception)
                    .getHttpStatusCode();
        }

        return 0;
    }

    private int safeAdd(
            int firstValue,
            int secondValue
    ) {
        long sum =
                (long) Math.max(0, firstValue)
                        + Math.max(0, secondValue);

        return sum > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) sum;
    }

    @NonNull
    private String createCombinedQuery(
            @Nullable String googleQuery,
            @Nullable String openLibraryQuery
    ) {
        String safeGoogleQuery =
                safeText(googleQuery);

        String safeOpenLibraryQuery =
                safeText(openLibraryQuery);

        if (safeGoogleQuery.isEmpty()) {
            return safeOpenLibraryQuery;
        }

        if (safeOpenLibraryQuery.isEmpty()
                || safeGoogleQuery.equalsIgnoreCase(
                safeOpenLibraryQuery
        )) {
            return safeGoogleQuery;
        }

        return "Google Books: "
                + safeGoogleQuery
                + " | Open Library: "
                + safeOpenLibraryQuery;
    }

    @NonNull
    private String normalizeIsbn(
            @Nullable Object value
    ) {
        String isbn =
                safeText(value)
                        .replaceAll(
                                "[^0-9Xx]",
                                ""
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        return isbn.length() == 10
                || isbn.length() == 13
                ? isbn
                : "";
    }

    @NonNull
    private String comparisonText(
            @Nullable Object value
    ) {
        return safeText(value)
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    @NonNull
    private String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString().trim();
    }

    private void addUniqueWarning(
            @NonNull List<String> warnings,
            @Nullable String warning
    ) {
        String safeWarning =
                safeText(warning);

        if (safeWarning.isEmpty()) {
            return;
        }

        for (String existingWarning : warnings) {
            if (existingWarning.equalsIgnoreCase(
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
        searchInProgress.set(false);

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

        return message == null
                || message.trim().isEmpty()
                ? "Online book search failed."
                : message.trim();
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
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        searchInProgress.set(false);

        googleBooksSearchClient.close();
        openLibrarySearchClient.close();
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
        private final BookCoverMetadataExtractor.DetectedBookMetadata
                detectedMetadata;

        @NonNull
        private final NetworkAvailabilityChecker.NetworkState
                networkState;

        @NonNull
        private final String searchQuery;

        private final int totalOnlineItems;

        @NonNull
        private final List<OnlineBookSearchResult>
                rawBookResults;

        @NonNull
        private final List<OnlineBookMatchEvaluator.RankedBookResult>
                rankedBookResults;

        @Nullable
        private final OnlineBookMatchEvaluator.RankedBookResult
                bestMatch;

        @NonNull
        private final List<String> warnings;

        private final long searchedAt;

        private BookDiscoveryResult(
                @NonNull BookCoverScanResult scanResult,
                @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedMetadata,
                @NonNull NetworkAvailabilityChecker.NetworkState networkState,
                @NonNull String searchQuery,
                int totalOnlineItems,
                @NonNull List<OnlineBookSearchResult> rawBookResults,
                @NonNull List<OnlineBookMatchEvaluator.RankedBookResult>
                        rankedBookResults,
                @Nullable OnlineBookMatchEvaluator.RankedBookResult bestMatch,
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
        public BookCoverMetadataExtractor.DetectedBookMetadata
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
        public List<OnlineBookMatchEvaluator.RankedBookResult>
        getRankedBookResults() {
            return rankedBookResults;
        }

        @Nullable
        public OnlineBookMatchEvaluator.RankedBookResult
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
                    && bestMatch
                    .getEvaluation()
                    .isHighConfidence();
        }

        public boolean isAutomaticSelectionRecommended() {
            return bestMatch != null
                    && bestMatch
                    .getEvaluation()
                    .isAutomaticSelectionRecommended();
        }

        public boolean requiresParentReview() {
            return bestMatch == null
                    || bestMatch
                    .getEvaluation()
                    .requiresParentReview()
                    || !warnings.isEmpty();
        }

        public boolean shouldOfferManualEntry() {
            return bestMatch == null
                    || !hasHighConfidenceMatch();
        }

        public boolean bestMatchHasAuthorizedDownload() {
            return bestMatch != null
                    && bestMatch
                    .getBookResult()
                    .hasAuthorizedDownload();
        }

        public boolean bestMatchCanBeAddedAsMetadata() {
            return bestMatch != null
                    && bestMatch
                    .getBookResult()
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
                    == FailureReason.NETWORK_PERMISSION_MISSING;
        }

        public boolean isProviderFailure() {
            return failureReason
                    == FailureReason.SEARCH_PROVIDER_FAILED;
        }

        public boolean hasHttpStatusCode() {
            return httpStatusCode > 0;
        }
    }
}