package com.tridev.studysaathi.data.content.search;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.BuildConfig;
import com.tridev.studysaathi.data.content.model.BookCoverScanResult;
import com.tridev.studysaathi.data.content.scanner.BookCoverMetadataExtractor;
import com.tridev.studysaathi.data.content.scanner.BookCoverScanner;

import java.util.concurrent.atomic.AtomicBoolean;

public final class BookScanDiscoveryCoordinator
        implements AutoCloseable {

    private static final int DEFAULT_MAXIMUM_RESULTS =
            20;

    private static final int MINIMUM_RESULTS =
            1;

    private static final int MAXIMUM_RESULTS =
            40;

    @NonNull
    private final BookCoverScanner bookCoverScanner;

    @NonNull
    private final BookDiscoveryCoordinator
            bookDiscoveryCoordinator;

    @NonNull
    private final Handler mainThreadHandler;

    @NonNull
    private final AtomicBoolean operationInProgress;

    @NonNull
    private final AtomicBoolean closed;

    private volatile long operationStartedAt;

    public BookScanDiscoveryCoordinator(
            @NonNull Context context
    ) {
        this(
                context,
                BuildConfig.GOOGLE_BOOKS_API_KEY
        );
    }

    /**
     * Google Books API key is optional.
     *
     * Key को source code में hard-code नहीं किया जाता।
     * Default constructor BuildConfig से local.properties
     * में सुरक्षित रखी गई key प्राप्त करता है।
     *
     * Key खाली होने या Google Books unavailable होने पर
     * BookDiscoveryCoordinator Open Library fallback उपयोग करेगा।
     */
    public BookScanDiscoveryCoordinator(
            @NonNull Context context,
            @Nullable String googleBooksApiKey
    ) {
        Context applicationContext =
                context.getApplicationContext();

        bookCoverScanner =
                new BookCoverScanner(
                        applicationContext
                );

        bookDiscoveryCoordinator =
                new BookDiscoveryCoordinator(
                        applicationContext,
                        googleBooksApiKey
                );

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );

        operationInProgress =
                new AtomicBoolean(
                        false
                );

        closed =
                new AtomicBoolean(
                        false
                );
    }

    public void scanAndDiscover(
            @NonNull Uri imageUri,
            @Nullable String privateImagePath,
            @NonNull BookCoverScanResult.ScanSource
                    scanSource,
            @NonNull ScanDiscoveryCallback callback
    ) {
        scanAndDiscover(
                imageUri,
                privateImagePath,
                scanSource,
                BookCoverMetadataExtractor
                        .ExtractionContext
                        .empty(),
                DEFAULT_MAXIMUM_RESULTS,
                callback
        );
    }

    public void scanAndDiscover(
            @NonNull Uri imageUri,
            @Nullable String privateImagePath,
            @NonNull BookCoverScanResult.ScanSource
                    scanSource,
            @NonNull BookCoverMetadataExtractor
                    .ExtractionContext extractionContext,
            @NonNull ScanDiscoveryCallback callback
    ) {
        scanAndDiscover(
                imageUri,
                privateImagePath,
                scanSource,
                extractionContext,
                DEFAULT_MAXIMUM_RESULTS,
                callback
        );
    }

    /**
     * Runs the complete book-cover discovery flow.
     *
     * Only one operation can run at a time.
     */
    public void scanAndDiscover(
            @NonNull Uri imageUri,
            @Nullable String privateImagePath,
            @NonNull BookCoverScanResult.ScanSource
                    scanSource,
            @NonNull BookCoverMetadataExtractor
                    .ExtractionContext extractionContext,
            int maximumResults,
            @NonNull ScanDiscoveryCallback callback
    ) {
        if (closed.get()) {
            dispatchFailure(
                    callback,
                    new BookScanDiscoveryException(
                            FailureStage.COORDINATOR,
                            FailureReason.CLIENT_CLOSED,
                            "Book cover discovery is no longer available.",
                            "पुस्तक खोज सेवा बंद हो चुकी है।"
                    )
            );

            return;
        }

        if (!operationInProgress.compareAndSet(
                false,
                true
        )) {
            dispatchFailure(
                    callback,
                    new BookScanDiscoveryException(
                            FailureStage.COORDINATOR,
                            FailureReason.OPERATION_ALREADY_RUNNING,
                            "Another book discovery operation is already running.",
                            "एक अन्य पुस्तक खोज प्रक्रिया पहले से चल रही है।"
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

        operationStartedAt =
                System.currentTimeMillis();

        dispatchStageChanged(
                callback,
                DiscoveryStage.PREPARING_IMAGE
        );

        dispatchStageChanged(
                callback,
                DiscoveryStage.SCANNING_COVER
        );

        try {
            bookCoverScanner.scanBookCover(
                    imageUri,
                    privateImagePath,
                    scanSource,
                    new BookCoverScanner.ScanCallback() {

                        @Override
                        public void onScanCompleted(
                                @NonNull BookCoverScanResult
                                        scanResult
                        ) {
                            handleScanCompleted(
                                    scanResult,
                                    extractionContext,
                                    safeMaximumResults,
                                    callback
                            );
                        }

                        @Override
                        public void onScanFailed(
                                @NonNull Exception exception
                        ) {
                            finishWithFailure(
                                    callback,
                                    new BookScanDiscoveryException(
                                            FailureStage.COVER_SCAN,
                                            FailureReason
                                                    .COVER_SCAN_FAILED,
                                            safeExceptionMessage(
                                                    exception,
                                                    "Book cover could not be scanned."
                                            ),
                                            "पुस्तक का कवर स्कैन नहीं किया जा सका।",
                                            exception
                                    )
                            );
                        }
                    }
            );

        } catch (RuntimeException exception) {
            finishWithFailure(
                    callback,
                    new BookScanDiscoveryException(
                            FailureStage.COVER_SCAN,
                            FailureReason.COVER_SCAN_FAILED,
                            safeExceptionMessage(
                                    exception,
                                    "Book cover scanning could not be started."
                            ),
                            "पुस्तक का कवर स्कैन शुरू नहीं किया जा सका।",
                            exception
                    )
            );
        }
    }

    private void handleScanCompleted(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookCoverMetadataExtractor
                    .ExtractionContext extractionContext,
            int maximumResults,
            @NonNull ScanDiscoveryCallback callback
    ) {
        if (closed.get()) {
            finishWithFailure(
                    callback,
                    new BookScanDiscoveryException(
                            FailureStage.COORDINATOR,
                            FailureReason.CLIENT_CLOSED,
                            "Book discovery was closed before scanning completed.",
                            "स्कैन पूरा होने से पहले पुस्तक खोज सेवा बंद हो गई।"
                    )
            );

            return;
        }

        dispatchScanCompleted(
                callback,
                scanResult
        );

        if (!scanResult.isSuccessful()) {
            finishWithFailure(
                    callback,
                    new BookScanDiscoveryException(
                            FailureStage.COVER_SCAN,
                            FailureReason.INVALID_SCAN_RESULT,
                            "The scanned image did not contain usable book information.",
                            "स्कैन की गई तस्वीर में उपयोगी पुस्तक जानकारी नहीं मिली।",
                            scanResult
                    )
            );

            return;
        }

        if (!scanResult.isReadyForOnlineSearch()) {
            finishWithFailure(
                    callback,
                    new BookScanDiscoveryException(
                            FailureStage.METADATA_EXTRACTION,
                            FailureReason
                                    .NO_SEARCHABLE_INFORMATION,
                            "The scan did not contain a title, ISBN or searchable text.",
                            "स्कैन में पुस्तक का नाम, ISBN या खोज योग्य टेक्स्ट नहीं मिला।",
                            scanResult
                    )
            );

            return;
        }

        dispatchStageChanged(
                callback,
                DiscoveryStage.EXTRACTING_METADATA
        );

        dispatchStageChanged(
                callback,
                DiscoveryStage.CHECKING_INTERNET
        );

        dispatchStageChanged(
                callback,
                DiscoveryStage.SEARCHING_ONLINE
        );

        try {
            bookDiscoveryCoordinator.discoverBooks(
                    scanResult,
                    extractionContext,
                    maximumResults,
                    new BookDiscoveryCoordinator
                            .DiscoveryCallback() {

                        @Override
                        public void onDiscoveryCompleted(
                                @NonNull BookDiscoveryCoordinator
                                        .BookDiscoveryResult
                                        discoveryResult
                        ) {
                            handleDiscoveryCompleted(
                                    scanResult,
                                    discoveryResult,
                                    callback
                            );
                        }

                        @Override
                        public void onDiscoveryFailed(
                                @NonNull BookDiscoveryCoordinator
                                        .BookDiscoveryException
                                        exception
                        ) {
                            finishWithFailure(
                                    callback,
                                    convertDiscoveryException(
                                            scanResult,
                                            exception
                                    )
                            );
                        }
                    }
            );

        } catch (RuntimeException exception) {
            finishWithFailure(
                    callback,
                    new BookScanDiscoveryException(
                            FailureStage.ONLINE_SEARCH,
                            FailureReason.ONLINE_SEARCH_FAILED,
                            safeExceptionMessage(
                                    exception,
                                    "Online book search could not be started."
                            ),
                            "ऑनलाइन पुस्तक खोज शुरू नहीं की जा सकी।",
                            exception,
                            scanResult
                    )
            );
        }
    }

    private void handleDiscoveryCompleted(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookDiscoveryCoordinator
                    .BookDiscoveryResult discoveryResult,
            @NonNull ScanDiscoveryCallback callback
    ) {
        if (closed.get()) {
            finishWithFailure(
                    callback,
                    new BookScanDiscoveryException(
                            FailureStage.COORDINATOR,
                            FailureReason.CLIENT_CLOSED,
                            "Book discovery was closed before the results were delivered.",
                            "परिणाम मिलने से पहले पुस्तक खोज सेवा बंद हो गई।",
                            scanResult
                    )
            );

            return;
        }

        dispatchStageChanged(
                callback,
                DiscoveryStage.RANKING_RESULTS
        );

        long completedAt =
                System.currentTimeMillis();

        CompleteDiscoveryResult completeResult =
                new CompleteDiscoveryResult(
                        scanResult,
                        discoveryResult,
                        operationStartedAt,
                        completedAt
                );

        operationInProgress.set(
                false
        );

        dispatchStageChanged(
                callback,
                DiscoveryStage.COMPLETED
        );

        dispatchCompleted(
                callback,
                completeResult
        );
    }

    @NonNull
    private BookScanDiscoveryException
    convertDiscoveryException(
            @NonNull BookCoverScanResult scanResult,
            @NonNull BookDiscoveryCoordinator
                    .BookDiscoveryException exception
    ) {
        FailureStage failureStage;

        FailureReason failureReason;

        switch (exception.getFailureReason()) {
            case METADATA_EXTRACTION_FAILED:
            case NO_SEARCHABLE_METADATA:
                failureStage =
                        FailureStage.METADATA_EXTRACTION;

                failureReason =
                        FailureReason
                                .METADATA_EXTRACTION_FAILED;

                break;

            case NETWORK_PERMISSION_MISSING:
                failureStage =
                        FailureStage.NETWORK_CHECK;

                failureReason =
                        FailureReason
                                .NETWORK_PERMISSION_MISSING;

                break;

            case NETWORK_UNAVAILABLE:
                failureStage =
                        FailureStage.NETWORK_CHECK;

                failureReason =
                        FailureReason.NETWORK_UNAVAILABLE;

                break;

            case SEARCH_PROVIDER_FAILED:
                failureStage =
                        FailureStage.ONLINE_SEARCH;

                failureReason =
                        FailureReason.ONLINE_SEARCH_FAILED;

                break;

            case MATCH_EVALUATION_FAILED:
                failureStage =
                        FailureStage.RESULT_RANKING;

                failureReason =
                        FailureReason.RESULT_RANKING_FAILED;

                break;

            case SEARCH_ALREADY_RUNNING:
                failureStage =
                        FailureStage.COORDINATOR;

                failureReason =
                        FailureReason.OPERATION_ALREADY_RUNNING;

                break;

            case CLIENT_CLOSED:
                failureStage =
                        FailureStage.COORDINATOR;

                failureReason =
                        FailureReason.CLIENT_CLOSED;

                break;

            case INVALID_SCAN_RESULT:
            default:
                failureStage =
                        FailureStage.COVER_SCAN;

                failureReason =
                        FailureReason.INVALID_SCAN_RESULT;

                break;
        }

        String hindiMessage =
                exception.getHindiMessage();

        if (hindiMessage.isEmpty()) {
            hindiMessage =
                    createDefaultHindiMessage(
                            failureReason
                    );
        }

        return new BookScanDiscoveryException(
                failureStage,
                failureReason,
                safeExceptionMessage(
                        exception,
                        "Book discovery failed."
                ),
                hindiMessage,
                exception.getHttpStatusCode(),
                exception,
                scanResult
        );
    }

    @NonNull
    private String createDefaultHindiMessage(
            @NonNull FailureReason failureReason
    ) {
        switch (failureReason) {
            case COVER_SCAN_FAILED:
                return "पुस्तक का कवर स्कैन नहीं किया जा सका।";

            case INVALID_SCAN_RESULT:
                return "स्कैन में उपयोगी पुस्तक जानकारी नहीं मिली।";

            case NO_SEARCHABLE_INFORMATION:
                return "ऑनलाइन खोज के लिए पर्याप्त जानकारी नहीं मिली।";

            case METADATA_EXTRACTION_FAILED:
                return "पुस्तक की जानकारी पहचानी नहीं जा सकी।";

            case NETWORK_PERMISSION_MISSING:
                return "नेटवर्क की स्थिति जाँचने की अनुमति उपलब्ध नहीं है।";

            case NETWORK_UNAVAILABLE:
                return "इंटरनेट उपलब्ध नहीं है।";

            case ONLINE_SEARCH_FAILED:
                return "ऑनलाइन पुस्तक खोज सफल नहीं हुई।";

            case RESULT_RANKING_FAILED:
                return "खोज परिणामों की तुलना नहीं की जा सकी।";

            case OPERATION_ALREADY_RUNNING:
                return "एक पुस्तक खोज प्रक्रिया पहले से चल रही है।";

            case CLIENT_CLOSED:
                return "पुस्तक खोज सेवा बंद हो चुकी है।";

            case UNKNOWN:
            default:
                return "पुस्तक खोज के दौरान समस्या हुई।";
        }
    }

    private void finishWithFailure(
            @NonNull ScanDiscoveryCallback callback,
            @NonNull BookScanDiscoveryException exception
    ) {
        operationInProgress.set(
                false
        );

        dispatchStageChanged(
                callback,
                DiscoveryStage.FAILED
        );

        dispatchFailure(
                callback,
                exception
        );
    }

    private void dispatchStageChanged(
            @NonNull ScanDiscoveryCallback callback,
            @NonNull DiscoveryStage discoveryStage
    ) {
        mainThreadHandler.post(
                () -> {
                    try {
                        callback.onStageChanged(
                                discoveryStage
                        );

                    } catch (RuntimeException ignored) {
                        /*
                         * A UI callback error must not
                         * stop the discovery process.
                         */
                    }
                }
        );
    }

    private void dispatchScanCompleted(
            @NonNull ScanDiscoveryCallback callback,
            @NonNull BookCoverScanResult scanResult
    ) {
        mainThreadHandler.post(
                () -> {
                    try {
                        callback.onCoverScanCompleted(
                                scanResult
                        );

                    } catch (RuntimeException ignored) {
                        /*
                         * The final discovery process
                         * continues even if the optional
                         * scan callback fails.
                         */
                    }
                }
        );
    }

    private void dispatchCompleted(
            @NonNull ScanDiscoveryCallback callback,
            @NonNull CompleteDiscoveryResult result
    ) {
        mainThreadHandler.post(
                () -> callback.onDiscoveryCompleted(
                        result
                )
        );
    }

    private void dispatchFailure(
            @NonNull ScanDiscoveryCallback callback,
            @NonNull BookScanDiscoveryException exception
    ) {
        mainThreadHandler.post(
                () -> callback.onDiscoveryFailed(
                        exception
                )
        );
    }

    @NonNull
    private String safeExceptionMessage(
            @NonNull Exception exception,
            @NonNull String fallbackMessage
    ) {
        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return fallbackMessage;
        }

        return message.trim();
    }

    public boolean isOperationInProgress() {
        return operationInProgress.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @NonNull
    public BookDiscoveryCoordinator
    getBookDiscoveryCoordinator() {
        return bookDiscoveryCoordinator;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(
                false,
                true
        )) {
            return;
        }

        operationInProgress.set(
                false
        );

        bookDiscoveryCoordinator.close();
    }

    public interface ScanDiscoveryCallback {

        /**
         * Called whenever the current operation
         * moves to another processing stage.
         */
        void onStageChanged(
                @NonNull DiscoveryStage stage
        );

        /**
         * Called after OCR and barcode scanning.
         *
         * The online search may still be running.
         */
        void onCoverScanCompleted(
                @NonNull BookCoverScanResult scanResult
        );

        /**
         * Called after scanning, searching and
         * result ranking are complete.
         */
        void onDiscoveryCompleted(
                @NonNull CompleteDiscoveryResult result
        );

        void onDiscoveryFailed(
                @NonNull BookScanDiscoveryException exception
        );
    }

    public enum DiscoveryStage {

        PREPARING_IMAGE(
                "Preparing image",
                "चित्र तैयार किया जा रहा है"
        ),

        SCANNING_COVER(
                "Scanning book cover",
                "पुस्तक का कवर स्कैन किया जा रहा है"
        ),

        EXTRACTING_METADATA(
                "Reading book information",
                "पुस्तक की जानकारी पढ़ी जा रही है"
        ),

        CHECKING_INTERNET(
                "Checking internet connection",
                "इंटरनेट कनेक्शन जाँचा जा रहा है"
        ),

        SEARCHING_ONLINE(
                "Searching for the book online",
                "पुस्तक को ऑनलाइन खोजा जा रहा है"
        ),

        RANKING_RESULTS(
                "Comparing search results",
                "खोज परिणामों की तुलना की जा रही है"
        ),

        COMPLETED(
                "Book discovery completed",
                "पुस्तक खोज पूरी हुई"
        ),

        FAILED(
                "Book discovery failed",
                "पुस्तक खोज असफल हुई"
        );

        @NonNull
        private final String englishMessage;

        @NonNull
        private final String hindiMessage;

        DiscoveryStage(
                @NonNull String englishMessage,
                @NonNull String hindiMessage
        ) {
            this.englishMessage =
                    englishMessage;

            this.hindiMessage =
                    hindiMessage;
        }

        @NonNull
        public String getEnglishMessage() {
            return englishMessage;
        }

        @NonNull
        public String getHindiMessage() {
            return hindiMessage;
        }
    }

    public enum FailureStage {

        COORDINATOR,

        COVER_SCAN,

        METADATA_EXTRACTION,

        NETWORK_CHECK,

        ONLINE_SEARCH,

        RESULT_RANKING
    }

    public enum FailureReason {

        CLIENT_CLOSED,

        OPERATION_ALREADY_RUNNING,

        COVER_SCAN_FAILED,

        INVALID_SCAN_RESULT,

        NO_SEARCHABLE_INFORMATION,

        METADATA_EXTRACTION_FAILED,

        NETWORK_PERMISSION_MISSING,

        NETWORK_UNAVAILABLE,

        ONLINE_SEARCH_FAILED,

        RESULT_RANKING_FAILED,

        UNKNOWN
    }

    public static final class CompleteDiscoveryResult {

        @NonNull
        private final BookCoverScanResult scanResult;

        @NonNull
        private final BookDiscoveryCoordinator
                .BookDiscoveryResult discoveryResult;

        private final long operationStartedAt;

        private final long operationCompletedAt;

        private final long totalDurationMilliseconds;

        private CompleteDiscoveryResult(
                @NonNull BookCoverScanResult scanResult,
                @NonNull BookDiscoveryCoordinator
                        .BookDiscoveryResult discoveryResult,
                long operationStartedAt,
                long operationCompletedAt
        ) {
            this.scanResult =
                    scanResult;

            this.discoveryResult =
                    discoveryResult;

            this.operationStartedAt =
                    Math.max(
                            0L,
                            operationStartedAt
                    );

            this.operationCompletedAt =
                    Math.max(
                            this.operationStartedAt,
                            operationCompletedAt
                    );

            totalDurationMilliseconds =
                    Math.max(
                            0L,
                            this.operationCompletedAt
                                    - this.operationStartedAt
                    );
        }

        @NonNull
        public BookCoverScanResult getScanResult() {
            return scanResult;
        }

        @NonNull
        public BookDiscoveryCoordinator
                .BookDiscoveryResult
        getDiscoveryResult() {
            return discoveryResult;
        }

        public long getOperationStartedAt() {
            return operationStartedAt;
        }

        public long getOperationCompletedAt() {
            return operationCompletedAt;
        }

        public long getTotalDurationMilliseconds() {
            return totalDurationMilliseconds;
        }

        public boolean hasOnlineResults() {
            return discoveryResult.hasResults();
        }

        public boolean hasBestMatch() {
            return discoveryResult.hasBestMatch();
        }

        public boolean hasHighConfidenceMatch() {
            return discoveryResult
                    .hasHighConfidenceMatch();
        }

        public boolean requiresParentReview() {
            return discoveryResult
                    .requiresParentReview();
        }

        public boolean canAddBookMetadata() {
            return discoveryResult
                    .bestMatchCanBeAddedAsMetadata();
        }

        public boolean canDownloadAuthorizedBook() {
            return discoveryResult
                    .bestMatchHasAuthorizedDownload();
        }

        public boolean shouldOfferManualEntry() {
            return discoveryResult
                    .shouldOfferManualEntry();
        }
    }

    public static final class
    BookScanDiscoveryException
            extends Exception {

        @NonNull
        private final FailureStage failureStage;

        @NonNull
        private final FailureReason failureReason;

        @NonNull
        private final String hindiMessage;

        private final int httpStatusCode;

        @Nullable
        private final BookCoverScanResult scanResult;

        public BookScanDiscoveryException(
                @NonNull FailureStage failureStage,
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull String hindiMessage
        ) {
            this(
                    failureStage,
                    failureReason,
                    message,
                    hindiMessage,
                    0,
                    null,
                    null
            );
        }

        public BookScanDiscoveryException(
                @NonNull FailureStage failureStage,
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull String hindiMessage,
                @NonNull Throwable cause
        ) {
            this(
                    failureStage,
                    failureReason,
                    message,
                    hindiMessage,
                    0,
                    cause,
                    null
            );
        }

        public BookScanDiscoveryException(
                @NonNull FailureStage failureStage,
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull String hindiMessage,
                @NonNull BookCoverScanResult scanResult
        ) {
            this(
                    failureStage,
                    failureReason,
                    message,
                    hindiMessage,
                    0,
                    null,
                    scanResult
            );
        }

        public BookScanDiscoveryException(
                @NonNull FailureStage failureStage,
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull String hindiMessage,
                @NonNull Throwable cause,
                @NonNull BookCoverScanResult scanResult
        ) {
            this(
                    failureStage,
                    failureReason,
                    message,
                    hindiMessage,
                    0,
                    cause,
                    scanResult
            );
        }

        /*
         * Single canonical constructor.
         *
         * The previous version accidentally contained
         * another constructor with this exact signature.
         * Java does not treat @NonNull and @Nullable as
         * different parameter types, so it caused the
         * duplicate-constructor compilation error.
         */
        private BookScanDiscoveryException(
                @NonNull FailureStage failureStage,
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull String hindiMessage,
                int httpStatusCode,
                @Nullable Throwable cause,
                @Nullable BookCoverScanResult scanResult
        ) {
            super(
                    message,
                    cause
            );

            this.failureStage =
                    failureStage;

            this.failureReason =
                    failureReason;

            this.hindiMessage =
                    hindiMessage.trim();

            this.httpStatusCode =
                    Math.max(
                            0,
                            httpStatusCode
                    );

            this.scanResult =
                    scanResult;
        }

        @NonNull
        public FailureStage getFailureStage() {
            return failureStage;
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

        @Nullable
        public BookCoverScanResult getScanResult() {
            return scanResult;
        }

        public boolean hasScanResult() {
            return scanResult != null;
        }

        public boolean hasHttpStatusCode() {
            return httpStatusCode > 0;
        }

        public boolean isNetworkFailure() {
            return failureStage
                    == FailureStage.NETWORK_CHECK;
        }

        public boolean canRetry() {
            return failureReason
                    == FailureReason.NETWORK_UNAVAILABLE
                    || failureReason
                    == FailureReason.ONLINE_SEARCH_FAILED
                    || failureReason
                    == FailureReason.COVER_SCAN_FAILED;
        }

        public boolean shouldOfferManualEntry() {
            return failureReason
                    == FailureReason.INVALID_SCAN_RESULT
                    || failureReason
                    == FailureReason.NO_SEARCHABLE_INFORMATION
                    || failureReason
                    == FailureReason
                    .METADATA_EXTRACTION_FAILED;
        }
    }
}