package com.tridev.studysaathi.data.content.search;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model.OnlineBookSearchResult;
import com.tridev.studysaathi.data.content.network.AndroidApiRequestIdentity;
import com.tridev.studysaathi.data.content.scanner.BookCoverMetadataExtractor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoogleBooksSearchClient
        implements AutoCloseable {

    private static final String GOOGLE_BOOKS_SEARCH_URL =
            "https://www.googleapis.com/books/v1/volumes";

    private static final String USER_AGENT =
            "StudySaathi-Android/1.0";

    private static final int DEFAULT_MAXIMUM_RESULTS =
            20;

    private static final int MINIMUM_RESULTS =
            1;

    private static final int MAXIMUM_RESULTS =
            40;

    private static final int CONNECT_TIMEOUT_MILLISECONDS =
            15_000;

    private static final int READ_TIMEOUT_MILLISECONDS =
            25_000;

    private static final int MAXIMUM_ERROR_BODY_LENGTH =
            700;

    private static final int MAXIMUM_CATEGORIES =
            12;

    private static final Pattern CLASS_PATTERN =
            Pattern.compile(
                    "(?i)\\b(?:class|grade|standard|std\\.?)"
                            + "\\s*[-:]?\\s*(1[0-2]|[1-9])\\b"
            );

    private static final Pattern PUBLICATION_YEAR_PATTERN =
            Pattern.compile(
                    "(?:18|19|20)\\d{2}"
            );

    @NonNull
    private final String googleBooksApiKey;

    @Nullable
    private final AndroidApiRequestIdentity
            androidApiRequestIdentity;

    @NonNull
    private final ExecutorService networkExecutor;

    @NonNull
    private final Handler mainThreadHandler;

    private volatile boolean closed;

    /**
     * पुराना constructor compatibility के लिए रखा गया है।
     *
     * इस constructor से API key request में जाएगी, लेकिन Android
     * restriction headers उपलब्ध नहीं होंगे।
     */
    public GoogleBooksSearchClient(
            @Nullable String googleBooksApiKey
    ) {
        this(
                googleBooksApiKey,
                null
        );
    }

    /**
     * Android-aware constructor।
     *
     * यह current installed APK से package name और signing certificate
     * SHA-1 तैयार करता है।
     */
    public GoogleBooksSearchClient(
            @NonNull Context context,
            @Nullable String googleBooksApiKey
    ) {
        this(
                googleBooksApiKey,
                AndroidApiRequestIdentity.create(
                        context.getApplicationContext()
                )
        );
    }

    /**
     * Test और dependency injection के लिए constructor।
     */
    public GoogleBooksSearchClient(
            @Nullable String googleBooksApiKey,
            @Nullable AndroidApiRequestIdentity
                    androidApiRequestIdentity
    ) {
        this.googleBooksApiKey =
                safeText(
                        googleBooksApiKey
                );

        this.androidApiRequestIdentity =
                androidApiRequestIdentity;

        networkExecutor =
                Executors.newSingleThreadExecutor();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    public void searchBooks(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata detectedBook,
            @NonNull SearchCallback callback
    ) {
        searchBooks(
                detectedBook,
                DEFAULT_MAXIMUM_RESULTS,
                callback
        );
    }

    public void searchBooks(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata detectedBook,
            int maximumResults,
            @NonNull SearchCallback callback
    ) {
        if (closed) {
            dispatchFailure(
                    callback,
                    new GoogleBooksSearchException(
                            "Google Books search client has already been closed."
                    )
            );

            return;
        }

        final SearchRequest searchRequest;

        try {
            searchRequest =
                    createSearchRequest(
                            detectedBook,
                            maximumResults
                    );

        } catch (Exception exception) {
            dispatchFailure(
                    callback,
                    exception
            );

            return;
        }

        try {
            networkExecutor.execute(
                    () -> {
                        try {
                            SearchResponse response =
                                    executeSearch(
                                            searchRequest
                                    );

                            dispatchSuccess(
                                    callback,
                                    response
                            );

                        } catch (Exception exception) {
                            dispatchFailure(
                                    callback,
                                    exception
                            );
                        }
                    }
            );

        } catch (RejectedExecutionException exception) {
            dispatchFailure(
                    callback,
                    new GoogleBooksSearchException(
                            "Google Books search could not be started.",
                            exception
                    )
            );
        }
    }

    @NonNull
    private SearchRequest createSearchRequest(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata detectedBook,
            int maximumResults
    ) throws IOException,
            GoogleBooksSearchException {

        int safeMaximumResults =
                Math.max(
                        MINIMUM_RESULTS,
                        Math.min(
                                MAXIMUM_RESULTS,
                                maximumResults
                        )
                );

        String preferredIsbn =
                normalizeIsbn(
                        detectedBook.getPreferredIsbn()
                );

        boolean exactIsbnSearch =
                !preferredIsbn.isEmpty();

        String searchQuery =
                exactIsbnSearch
                        ? "isbn:" + preferredIsbn
                        : createMetadataSearchQuery(
                        detectedBook
                );

        if (searchQuery.isEmpty()) {
            throw new GoogleBooksSearchException(
                    "Book title or ISBN is required before starting Google Books search."
            );
        }

        StringBuilder requestUrl =
                new StringBuilder(
                        GOOGLE_BOOKS_SEARCH_URL
                )
                        .append(
                                "?q="
                        )
                        .append(
                                encode(
                                        searchQuery
                                )
                        )
                        .append(
                                "&maxResults="
                        )
                        .append(
                                safeMaximumResults
                        )
                        .append(
                                "&startIndex=0"
                        )
                        .append(
                                "&printType=books"
                        )
                        .append(
                                "&projection=full"
                        );

        if (!googleBooksApiKey.isEmpty()) {
            requestUrl.append(
                    "&key="
            );

            requestUrl.append(
                    encode(
                            googleBooksApiKey
                    )
            );
        }

        return new SearchRequest(
                searchQuery,
                requestUrl.toString(),
                preferredIsbn,
                exactIsbnSearch,
                safeMaximumResults
        );
    }

    @NonNull
    private String createMetadataSearchQuery(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata detectedBook
    ) {
        StringBuilder query =
                new StringBuilder();

        appendFieldQuery(
                query,
                "intitle",
                detectedBook.getBookTitle()
        );

        appendFieldQuery(
                query,
                "inauthor",
                detectedBook.getAuthorName()
        );

        appendFieldQuery(
                query,
                "inpublisher",
                detectedBook.getPublisherName()
        );

        if (query.length() == 0) {
            appendFreeText(
                    query,
                    detectedBook.getOnlineSearchQuery()
            );
        }

        if (query.length() == 0) {
            appendFreeText(
                    query,
                    detectedBook.getSubjectName()
            );

            appendFreeText(
                    query,
                    detectedBook.getClassName()
            );
        }

        return query.toString()
                .trim();
    }

    private void appendFieldQuery(
            @NonNull StringBuilder query,
            @NonNull String fieldName,
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (query.length() > 0) {
            query.append(
                    ' '
            );
        }

        query.append(
                fieldName
        );

        query.append(
                ":\""
        );

        query.append(
                safeValue.replace(
                        "\"",
                        " "
                )
        );

        query.append(
                '"'
        );
    }

    private void appendFreeText(
            @NonNull StringBuilder query,
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (query.length() > 0) {
            query.append(
                    ' '
            );
        }

        query.append(
                safeValue
        );
    }

    @NonNull
    private SearchResponse executeSearch(
            @NonNull SearchRequest searchRequest
    ) throws IOException,
            JSONException,
            GoogleBooksSearchException {

        HttpURLConnection connection =
                null;

        try {
            URL url =
                    new URL(
                            searchRequest.requestUrl
                    );

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT_MILLISECONDS
            );

            connection.setReadTimeout(
                    READ_TIMEOUT_MILLISECONDS
            );

            connection.setUseCaches(
                    true
            );

            connection.setDoInput(
                    true
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Accept-Charset",
                    StandardCharsets.UTF_8.name()
            );

            connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
            );

            connection.setRequestProperty(
                    "X-Study-Saathi-Provider",
                    "Google-Books"
            );

            boolean androidIdentityApplied =
                    applyAndroidRequestIdentity(
                            connection
                    );

            int responseCode =
                    connection.getResponseCode();

            InputStream responseStream =
                    responseCode >= 200
                            && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            String responseBody =
                    readResponseBody(
                            responseStream
                    );

            if (responseCode < 200
                    || responseCode >= 300) {

                throw new GoogleBooksSearchException(
                        createHttpErrorMessage(
                                responseCode,
                                responseBody,
                                androidIdentityApplied
                        ),
                        responseCode
                );
            }

            return parseSearchResponse(
                    searchRequest,
                    responseBody,
                    androidIdentityApplied
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean applyAndroidRequestIdentity(
            @NonNull HttpURLConnection connection
    ) {
        if (googleBooksApiKey.isEmpty()
                || androidApiRequestIdentity == null) {

            return false;
        }

        return androidApiRequestIdentity.applyTo(
                connection
        );
    }

    @NonNull
    private SearchResponse parseSearchResponse(
            @NonNull SearchRequest searchRequest,
            @NonNull String responseBody,
            boolean androidIdentityApplied
    ) throws JSONException,
            GoogleBooksSearchException {

        if (responseBody.trim().isEmpty()) {
            throw new GoogleBooksSearchException(
                    "Google Books returned an empty response."
            );
        }

        JSONObject root =
                new JSONObject(
                        responseBody
                );

        int totalItems =
                Math.max(
                        0,
                        root.optInt(
                                "totalItems",
                                0
                        )
                );

        JSONArray items =
                root.optJSONArray(
                        "items"
                );

        List<OnlineBookSearchResult> parsedResults =
                new ArrayList<>();

        long searchedAt =
                System.currentTimeMillis();

        if (items != null) {
            for (int index = 0;
                 index < items.length();
                 index++) {

                JSONObject item =
                        items.optJSONObject(
                                index
                        );

                if (item == null) {
                    continue;
                }

                OnlineBookSearchResult parsedResult =
                        parseBookItem(
                                item,
                                searchRequest.preferredIsbn,
                                searchedAt
                        );

                if (parsedResult != null) {
                    parsedResults.add(
                            parsedResult
                    );
                }
            }
        }

        return new SearchResponse(
                searchRequest.searchQuery,
                totalItems,
                removeDuplicateResults(
                        parsedResults
                ),
                searchedAt,
                searchRequest.exactIsbnSearch,
                !googleBooksApiKey.isEmpty(),
                androidIdentityApplied
        );
    }

    @Nullable
    private OnlineBookSearchResult parseBookItem(
            @NonNull JSONObject item,
            @NonNull String requestedIsbn,
            long searchedAt
    ) {
        String providerBookId =
                jsonText(
                        item,
                        "id"
                );

        JSONObject volumeInfo =
                item.optJSONObject(
                        "volumeInfo"
                );

        if (volumeInfo == null) {
            return null;
        }

        String title =
                jsonText(
                        volumeInfo,
                        "title"
                );

        if (title.isEmpty()) {
            return null;
        }

        String subtitle =
                jsonText(
                        volumeInfo,
                        "subtitle"
                );

        List<String> authors =
                jsonTextList(
                        volumeInfo.optJSONArray(
                                "authors"
                        )
                );

        String publisherName =
                jsonText(
                        volumeInfo,
                        "publisher"
                );

        String publicationDate =
                jsonText(
                        volumeInfo,
                        "publishedDate"
                );

        String publicationYear =
                extractPublicationYear(
                        publicationDate
                );

        String description =
                jsonText(
                        volumeInfo,
                        "description"
                );

        String languageCode =
                normalizeLanguageCode(
                        jsonText(
                                volumeInfo,
                                "language"
                        )
                );

        int pageCount =
                Math.max(
                        0,
                        volumeInfo.optInt(
                                "pageCount",
                                0
                        )
                );

        List<String> categories =
                jsonTextList(
                        volumeInfo.optJSONArray(
                                "categories"
                        )
                );

        IndustryIdentifiers identifiers =
                parseIndustryIdentifiers(
                        volumeInfo.optJSONArray(
                                "industryIdentifiers"
                        ),
                        requestedIsbn
                );

        JSONObject imageLinks =
                volumeInfo.optJSONObject(
                        "imageLinks"
                );

        String smallCoverImageUrl =
                firstNonEmpty(
                        imageText(
                                imageLinks,
                                "small"
                        ),
                        imageText(
                                imageLinks,
                                "thumbnail"
                        ),
                        imageText(
                                imageLinks,
                                "smallThumbnail"
                        )
                );

        String largeCoverImageUrl =
                firstNonEmpty(
                        imageText(
                                imageLinks,
                                "extraLarge"
                        ),
                        imageText(
                                imageLinks,
                                "large"
                        ),
                        imageText(
                                imageLinks,
                                "medium"
                        ),
                        smallCoverImageUrl
                );

        String informationUrl =
                firstNonEmpty(
                        jsonText(
                                volumeInfo,
                                "infoLink"
                        ),
                        jsonText(
                                item,
                                "selfLink"
                        ),
                        createGoogleBooksVolumeUrl(
                                providerBookId
                        )
                );

        String previewUrl =
                jsonText(
                        volumeInfo,
                        "previewLink"
                );

        String canonicalVolumeUrl =
                jsonText(
                        volumeInfo,
                        "canonicalVolumeLink"
                );

        JSONObject accessInfo =
                item.optJSONObject(
                        "accessInfo"
                );

        AccessInformation accessInformation =
                parseAccessInformation(
                        accessInfo,
                        previewUrl,
                        informationUrl
                );

        String subjectName =
                categories.isEmpty()
                        ? subjectFromTitle(
                        title
                )
                        : categories.get(
                        0
                );

        String className =
                classFromBookText(
                        title
                                + " "
                                + subtitle
                                + " "
                                + description
                );

        String studyMedium =
                mediumFromLanguageCode(
                        languageCode
                );

        String editionName =
                extractEditionName(
                        title,
                        subtitle,
                        description
                );

        String resultId =
                createResultId(
                        providerBookId
                );

        OnlineBookSearchResult.Builder builder =
                OnlineBookSearchResult.builder(
                                resultId,
                                OnlineBookSearchResult
                                        .BookProvider
                                        .GOOGLE_BOOKS,
                                title
                        )
                        .setProviderBookId(
                                providerBookId
                        )
                        .setBookSubtitle(
                                subtitle
                        )
                        .addAuthors(
                                authors
                        )
                        .setPublisherName(
                                publisherName
                        )
                        .setPublicationDate(
                                publicationDate
                        )
                        .setPublicationYear(
                                publicationYear
                        )
                        .setEditionName(
                                editionName
                        )
                        .setSubjectName(
                                subjectName
                        )
                        .setClassName(
                                className
                        )
                        .setStudyMedium(
                                studyMedium
                        )
                        .setIsbn10(
                                identifiers.isbn10
                        )
                        .setIsbn13(
                                identifiers.isbn13
                        )
                        .setIndustryIdentifier(
                                identifiers.preferredIdentifier
                        )
                        .setLanguageCode(
                                languageCode
                        )
                        .setPageCount(
                                pageCount
                        )
                        .setSmallCoverImageUrl(
                                smallCoverImageUrl
                        )
                        .setLargeCoverImageUrl(
                                largeCoverImageUrl
                        )
                        .setInformationUrl(
                                firstNonEmpty(
                                        canonicalVolumeUrl,
                                        informationUrl
                                )
                        )
                        .setPreviewUrl(
                                accessInformation.previewUrl
                        )
                        .setOfficialSourceUrl(
                                ""
                        )
                        .setAuthorizedDownloadUrl(
                                accessInformation.authorizedDownloadUrl
                        )
                        .setDownloadMimeType(
                                accessInformation.downloadMimeType
                        )
                        .setAccessType(
                                accessInformation.accessType
                        )
                        .setLicenseType(
                                OnlineBookSearchResult
                                        .LicenseType
                                        .UNKNOWN
                        )
                        .setPreviewAllowed(
                                accessInformation.previewAllowed
                        )
                        .setDownloadAllowed(
                                accessInformation.downloadAllowed
                        )
                        .setOfficialSource(
                                false
                        )
                        .setOfficialSourceVerified(
                                false
                        )
                        .setPublicDomain(
                                accessInformation.publicDomain
                        )
                        .setOpenEducationalResource(
                                false
                        )
                        .setMatchStatus(
                                OnlineBookSearchResult
                                        .MatchStatus
                                        .MANUAL_REVIEW_REQUIRED
                        )
                        .setSearchedAt(
                                searchedAt
                        )
                        .setParentConfirmed(
                                false
                        );

        for (int index = 0;
             index < categories.size()
                     && index < MAXIMUM_CATEGORIES;
             index++) {

            builder.addCategory(
                    categories.get(
                            index
                    )
            );
        }

        if (identifiers.preferredIdentifier.isEmpty()) {
            builder.addWarning(
                    "Google Books result में verified ISBN उपलब्ध नहीं है।"
            );
        }

        if (publisherName.isEmpty()) {
            builder.addWarning(
                    "Google Books record में publisher उपलब्ध नहीं है।"
            );
        }

        if (!requestedIsbn.isEmpty()
                && requestedIsbn.equals(
                identifiers.preferredIdentifier
        )) {

            builder.addMatchReason(
                    "Google Books ISBN scanned ISBN से exact match करता है।"
            );

        } else if (!requestedIsbn.isEmpty()
                && identifiers.contains(
                requestedIsbn
        )) {

            builder.addMatchReason(
                    "Scanned ISBN Google Books identifiers में मौजूद है।"
            );

        } else {
            builder.addMatchReason(
                    "Result उपलब्ध title, author और publisher metadata से मिला।"
            );
        }

        if (accessInformation.publicDomain) {
            builder.addMatchReason(
                    "Google Books ने इस edition को public-domain access के रूप में बताया है।"
            );
        }

        return builder.build();
    }

    @NonNull
    private AccessInformation parseAccessInformation(
            @Nullable JSONObject accessInfo,
            @Nullable String volumePreviewUrl,
            @Nullable String informationUrl
    ) {
        if (accessInfo == null) {
            return new AccessInformation(
                    OnlineBookSearchResult
                            .AccessType
                            .METADATA_ONLY,
                    false,
                    false,
                    false,
                    "",
                    "",
                    ""
            );
        }

        String viewability =
                jsonText(
                        accessInfo,
                        "viewability"
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        boolean publicDomain =
                accessInfo.optBoolean(
                        "publicDomain",
                        false
                );

        boolean embeddable =
                accessInfo.optBoolean(
                        "embeddable",
                        false
                );

        String webReaderLink =
                jsonText(
                        accessInfo,
                        "webReaderLink"
                );

        String accessViewStatus =
                jsonText(
                        accessInfo,
                        "accessViewStatus"
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        boolean previewAllowed =
                !"NO_PAGES".equals(
                        viewability
                )
                        && (
                        "PARTIAL".equals(
                                viewability
                        )
                                || "ALL_PAGES".equals(
                                viewability
                        )
                                || "FULL_PUBLIC_DOMAIN".equals(
                                viewability
                        )
                                || "SAMPLE".equals(
                                accessViewStatus
                        )
                                || embeddable
                );

        String previewUrl =
                previewAllowed
                        ? firstNonEmpty(
                        webReaderLink,
                        safeText(
                                volumePreviewUrl
                        ),
                        safeText(
                                informationUrl
                        )
                )
                        : "";

        DownloadInformation downloadInformation =
                parseDownloadInformation(
                        accessInfo,
                        publicDomain
                );

        OnlineBookSearchResult.AccessType accessType;

        if (publicDomain
                || "FULL_PUBLIC_DOMAIN".equals(
                viewability
        )
                || "ALL_PAGES".equals(
                viewability
        )) {

            accessType =
                    OnlineBookSearchResult
                            .AccessType
                            .FULL_ONLINE;

        } else if (previewAllowed) {
            accessType =
                    OnlineBookSearchResult
                            .AccessType
                            .PARTIAL_PREVIEW;

        } else {
            accessType =
                    OnlineBookSearchResult
                            .AccessType
                            .METADATA_ONLY;
        }

        return new AccessInformation(
                accessType,
                previewAllowed,
                downloadInformation.downloadAllowed,
                publicDomain,
                previewUrl,
                downloadInformation.downloadUrl,
                downloadInformation.mimeType
        );
    }

    @NonNull
    private DownloadInformation parseDownloadInformation(
            @NonNull JSONObject accessInfo,
            boolean publicDomain
    ) {
        JSONObject pdf =
                accessInfo.optJSONObject(
                        "pdf"
                );

        JSONObject epub =
                accessInfo.optJSONObject(
                        "epub"
                );

        boolean pdfAvailable =
                pdf != null
                        && pdf.optBoolean(
                        "isAvailable",
                        false
                );

        boolean epubAvailable =
                epub != null
                        && epub.optBoolean(
                        "isAvailable",
                        false
                );

        String pdfDownloadUrl =
                pdf == null
                        ? ""
                        : firstNonEmpty(
                        jsonText(
                                pdf,
                                "downloadLink"
                        ),
                        jsonText(
                                pdf,
                                "acsTokenLink"
                        )
                );

        String epubDownloadUrl =
                epub == null
                        ? ""
                        : firstNonEmpty(
                        jsonText(
                                epub,
                                "downloadLink"
                        ),
                        jsonText(
                                epub,
                                "acsTokenLink"
                        )
                );

        /*
         * Metadata में file उपलब्ध लिखी होने के बावजूद direct authorized
         * link न हो तो app अपने-आप download नहीं करेगी।
         */
        if (publicDomain
                && pdfAvailable
                && !pdfDownloadUrl.isEmpty()) {

            return new DownloadInformation(
                    true,
                    pdfDownloadUrl,
                    "application/pdf"
            );
        }

        if (publicDomain
                && epubAvailable
                && !epubDownloadUrl.isEmpty()) {

            return new DownloadInformation(
                    true,
                    epubDownloadUrl,
                    "application/epub+zip"
            );
        }

        return new DownloadInformation(
                false,
                "",
                ""
        );
    }

    @NonNull
    private IndustryIdentifiers parseIndustryIdentifiers(
            @Nullable JSONArray industryIdentifiers,
            @NonNull String requestedIsbn
    ) {
        String isbn10 =
                "";

        String isbn13 =
                "";

        String exactIdentifier =
                "";

        List<String> allIdentifiers =
                new ArrayList<>();

        if (industryIdentifiers != null) {
            for (int index = 0;
                 index < industryIdentifiers.length();
                 index++) {

                JSONObject identifierObject =
                        industryIdentifiers.optJSONObject(
                                index
                        );

                if (identifierObject == null) {
                    continue;
                }

                String type =
                        jsonText(
                                identifierObject,
                                "type"
                        )
                                .toUpperCase(
                                        Locale.ROOT
                                );

                String identifier =
                        normalizeIsbn(
                                jsonText(
                                        identifierObject,
                                        "identifier"
                                )
                        );

                if (identifier.isEmpty()) {
                    continue;
                }

                if (!allIdentifiers.contains(
                        identifier
                )) {
                    allIdentifiers.add(
                            identifier
                    );
                }

                if ("ISBN_10".equals(
                        type
                )
                        || identifier.length() == 10) {

                    if (isbn10.isEmpty()) {
                        isbn10 =
                                identifier;
                    }
                }

                if ("ISBN_13".equals(
                        type
                )
                        || identifier.length() == 13) {

                    if (isbn13.isEmpty()) {
                        isbn13 =
                                identifier;
                    }
                }

                if (!requestedIsbn.isEmpty()
                        && requestedIsbn.equals(
                        identifier
                )) {

                    exactIdentifier =
                            identifier;
                }
            }
        }

        String preferredIdentifier =
                !exactIdentifier.isEmpty()
                        ? exactIdentifier
                        : (
                        !isbn13.isEmpty()
                        ? isbn13
                        : isbn10
                );

        return new IndustryIdentifiers(
                isbn10,
                isbn13,
                preferredIdentifier,
                allIdentifiers
        );
    }

    @NonNull
    private List<OnlineBookSearchResult>
    removeDuplicateResults(
            @NonNull List<OnlineBookSearchResult> results
    ) {
        Map<String, OnlineBookSearchResult> uniqueResults =
                new LinkedHashMap<>();

        for (OnlineBookSearchResult result :
                results) {

            String isbn =
                    normalizeIsbn(
                            result.getPreferredIsbn()
                    );

            String resultKey;

            if (!isbn.isEmpty()) {
                resultKey =
                        "isbn:"
                                + isbn;

            } else if (!result.getProviderBookId()
                    .isEmpty()) {

                resultKey =
                        "provider:"
                                + result.getProviderBookId();

            } else {
                resultKey =
                        "metadata:"
                                + comparisonText(
                                result.getBookTitle()
                        )
                                + "|"
                                + comparisonText(
                                result.getPublisherName()
                        )
                                + "|"
                                + comparisonText(
                                result.getAuthorsDisplayText()
                        );
            }

            if (!uniqueResults.containsKey(
                    resultKey
            )) {
                uniqueResults.put(
                        resultKey,
                        result
                );
            }
        }

        return new ArrayList<>(
                uniqueResults.values()
        );
    }

    @NonNull
    private String extractPublicationYear(
            @Nullable String publicationDate
    ) {
        Matcher matcher =
                PUBLICATION_YEAR_PATTERN.matcher(
                        safeText(
                                publicationDate
                        )
                );

        return matcher.find()
                ? matcher.group()
                : "";
    }

    @NonNull
    private String extractEditionName(
            @Nullable String title,
            @Nullable String subtitle,
            @Nullable String description
    ) {
        String combinedText =
                safeText(
                        title
                )
                        + " "
                        + safeText(
                        subtitle
                )
                        + " "
                        + safeText(
                        description
                );

        Matcher matcher =
                Pattern.compile(
                                "(?i)\\b(?:revised|updated|new|latest|"
                                        + "\\d+(?:st|nd|rd|th))\\s+edition\\b"
                        )
                        .matcher(
                                combinedText
                        );

        return matcher.find()
                ? matcher.group()
                .trim()
                : "";
    }

    @NonNull
    private String classFromBookText(
            @Nullable String value
    ) {
        Matcher matcher =
                CLASS_PATTERN.matcher(
                        safeText(
                                value
                        )
                );

        return matcher.find()
                ? "Class "
                  + matcher.group(
                1
        )
                : "";
    }

    @NonNull
    private String subjectFromTitle(
            @Nullable String title
    ) {
        String subject =
                safeText(
                        title
                )
                        .replaceAll(
                                "(?i)\\s+(?:for\\s+)?"
                                        + "(?:class|grade|standard|std\\.?)"
                                        + "\\s*[-:]?\\s*(?:1[0-2]|[1-9]).*$",
                                ""
                        )
                        .replaceAll(
                                "(?i)\\b(?:textbook|workbook|coursebook)\\b",
                                ""
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        return subject.length() <= 80
                ? subject
                : "";
    }

    @NonNull
    private String normalizeLanguageCode(
            @Nullable String value
    ) {
        String languageCode =
                safeText(
                        value
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        switch (languageCode) {
            case "eng":
                return "en";

            case "hin":
                return "hi";

            case "san":
                return "sa";

            default:
                return languageCode;
        }
    }

    @NonNull
    private String mediumFromLanguageCode(
            @Nullable String languageCode
    ) {
        switch (normalizeLanguageCode(
                languageCode
        )) {
            case "en":
                return "English";

            case "hi":
                return "Hindi";

            case "sa":
                return "Sanskrit";

            default:
                return "";
        }
    }

    @NonNull
    private String createGoogleBooksVolumeUrl(
            @Nullable String providerBookId
    ) {
        String safeProviderBookId =
                safeText(
                        providerBookId
                );

        return safeProviderBookId.isEmpty()
                ? ""
                : "https://books.google.com/books?id="
                  + safeProviderBookId;
    }

    @NonNull
    private String createResultId(
            @Nullable String providerBookId
    ) {
        String safeProviderBookId =
                safeText(
                        providerBookId
                );

        if (safeProviderBookId.isEmpty()) {
            safeProviderBookId =
                    UUID.randomUUID()
                            .toString();
        }

        return "google_books_"
                + safeProviderBookId;
    }

    @NonNull
    private String imageText(
            @Nullable JSONObject imageLinks,
            @NonNull String key
    ) {
        if (imageLinks == null) {
            return "";
        }

        String imageUrl =
                jsonText(
                        imageLinks,
                        key
                );

        /*
         * कुछ Google Books records HTTP image URL देते हैं।
         * Android cleartext block से बचने के लिए HTTPS उपयोग करें।
         */
        if (imageUrl.startsWith(
                "http://"
        )) {
            imageUrl =
                    "https://"
                            + imageUrl.substring(
                            "http://".length()
                    );
        }

        return imageUrl;
    }

    @NonNull
    private String jsonText(
            @NonNull JSONObject object,
            @NonNull String key
    ) {
        return safeText(
                object.optString(
                        key,
                        ""
                )
        );
    }

    @NonNull
    private List<String> jsonTextList(
            @Nullable JSONArray array
    ) {
        List<String> values =
                new ArrayList<>();

        if (array == null) {
            return values;
        }

        for (int index = 0;
             index < array.length();
             index++) {

            String value =
                    safeText(
                            array.optString(
                                    index,
                                    ""
                            )
                    );

            if (!value.isEmpty()
                    && !containsIgnoreCase(
                    values,
                    value
            )) {
                values.add(
                        value
                );
            }
        }

        return values;
    }

    private boolean containsIgnoreCase(
            @NonNull List<String> values,
            @NonNull String target
    ) {
        for (String value : values) {
            if (value.equalsIgnoreCase(
                    target
            )) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private String readResponseBody(
            @Nullable InputStream inputStream
    ) throws IOException {

        if (inputStream == null) {
            return "";
        }

        StringBuilder responseBody =
                new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     inputStream,
                                     StandardCharsets.UTF_8
                             )
                     )) {

            String line;

            while ((line = reader.readLine())
                    != null) {

                responseBody.append(
                        line
                );
            }
        }

        return responseBody.toString();
    }

    @NonNull
    private String createHttpErrorMessage(
            int responseCode,
            @Nullable String responseBody,
            boolean androidIdentityApplied
    ) {
        String safeResponseBody =
                safeText(
                        responseBody
                );

        if (safeResponseBody.length()
                > MAXIMUM_ERROR_BODY_LENGTH) {

            safeResponseBody =
                    safeResponseBody.substring(
                            0,
                            MAXIMUM_ERROR_BODY_LENGTH
                    );
        }

        String reason;

        switch (responseCode) {
            case 400:
                reason =
                        "Google Books request invalid थी।";
                break;

            case 401:
                reason =
                        "Google Books API key authentication सफल नहीं हुई।";
                break;

            case 403:
                reason =
                        androidIdentityApplied
                                ? "Google Books API key restriction या permission ने request अस्वीकार की।"
                                : "Google Books API key restriction के लिए Android identity उपलब्ध नहीं थी।";
                break;

            case 404:
                reason =
                        "Google Books endpoint या requested resource उपलब्ध नहीं है।";
                break;

            case 429:
                reason =
                        "Google Books request quota या rate limit समाप्त हो गई।";
                break;

            case 500:
            case 502:
            case 503:
            case 504:
                reason =
                        "Google Books service अस्थायी रूप से उपलब्ध नहीं है।";
                break;

            default:
                reason =
                        "Google Books search सफल नहीं हुई।";
                break;
        }

        return safeResponseBody.isEmpty()
                ? reason
                  + " HTTP Status: "
                  + responseCode
                : reason
                  + " HTTP Status: "
                  + responseCode
                  + ". Response: "
                  + safeResponseBody;
    }

    @NonNull
    private String encode(
            @NonNull String value
    ) throws IOException {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8.name()
        );
    }

    @NonNull
    private String normalizeIsbn(
            @Nullable Object value
    ) {
        String isbn =
                safeText(
                        value
                )
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
        return safeText(
                value
        )
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
    private String firstNonEmpty(
            @Nullable String... values
    ) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            String safeValue =
                    safeText(
                            value
                    );

            if (!safeValue.isEmpty()) {
                return safeValue;
            }
        }

        return "";
    }

    @NonNull
    private String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }

    private void dispatchSuccess(
            @NonNull SearchCallback callback,
            @NonNull SearchResponse response
    ) {
        mainThreadHandler.post(
                () -> callback.onSearchCompleted(
                        response
                )
        );
    }

    private void dispatchFailure(
            @NonNull SearchCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(
                () -> callback.onSearchFailed(
                        exception
                )
        );
    }

    public boolean hasApiKey() {
        return !googleBooksApiKey.isEmpty();
    }

    public boolean hasCompleteAndroidIdentity() {
        return androidApiRequestIdentity != null
                && androidApiRequestIdentity.isComplete();
    }

    @NonNull
    public String getAndroidIdentityDiagnosticSummary() {
        if (androidApiRequestIdentity == null) {
            return "Android API request identity has not been supplied.";
        }

        return androidApiRequestIdentity
                .createDiagnosticSummary();
    }

    @Override
    public void close() {
        closed =
                true;

        networkExecutor.shutdownNow();
    }

    public interface SearchCallback {

        void onSearchCompleted(
                @NonNull SearchResponse response
        );

        void onSearchFailed(
                @NonNull Exception exception
        );
    }

    public static final class SearchResponse {

        @NonNull
        private final String searchQuery;

        private final int totalItems;

        @NonNull
        private final List<OnlineBookSearchResult>
                bookResults;

        private final long searchedAt;

        private final boolean exactIsbnSearch;

        private final boolean apiKeyUsed;

        private final boolean androidIdentityApplied;

        private SearchResponse(
                @NonNull String searchQuery,
                int totalItems,
                @NonNull List<OnlineBookSearchResult>
                        bookResults,
                long searchedAt,
                boolean exactIsbnSearch,
                boolean apiKeyUsed,
                boolean androidIdentityApplied
        ) {
            this.searchQuery =
                    searchQuery;

            this.totalItems =
                    Math.max(
                            0,
                            totalItems
                    );

            this.bookResults =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    bookResults
                            )
                    );

            this.searchedAt =
                    Math.max(
                            0L,
                            searchedAt
                    );

            this.exactIsbnSearch =
                    exactIsbnSearch;

            this.apiKeyUsed =
                    apiKeyUsed;

            this.androidIdentityApplied =
                    androidIdentityApplied;
        }

        @NonNull
        public String getSearchQuery() {
            return searchQuery;
        }

        public int getTotalItems() {
            return totalItems;
        }

        @NonNull
        public List<OnlineBookSearchResult>
        getBookResults() {
            return bookResults;
        }

        public long getSearchedAt() {
            return searchedAt;
        }

        public boolean isExactIsbnSearch() {
            return exactIsbnSearch;
        }

        public boolean isApiKeyUsed() {
            return apiKeyUsed;
        }

        public boolean isAndroidIdentityApplied() {
            return androidIdentityApplied;
        }

        public boolean hasResults() {
            return !bookResults.isEmpty();
        }

        public int getReturnedResultCount() {
            return bookResults.size();
        }
    }

    public static final class GoogleBooksSearchException
            extends Exception {

        private final int httpStatusCode;

        public GoogleBooksSearchException(
                @NonNull String message
        ) {
            this(
                    message,
                    0
            );
        }

        public GoogleBooksSearchException(
                @NonNull String message,
                int httpStatusCode
        ) {
            super(
                    message
            );

            this.httpStatusCode =
                    Math.max(
                            0,
                            httpStatusCode
                    );
        }

        public GoogleBooksSearchException(
                @NonNull String message,
                @NonNull Throwable cause
        ) {
            super(
                    message,
                    cause
            );

            httpStatusCode =
                    0;
        }

        public int getHttpStatusCode() {
            return httpStatusCode;
        }

        public boolean isRateLimited() {
            return httpStatusCode == 429;
        }

        public boolean isAuthenticationFailure() {
            return httpStatusCode == 401
                    || httpStatusCode == 403;
        }

        public boolean isTemporaryServiceFailure() {
            return httpStatusCode == 429
                    || httpStatusCode == 500
                    || httpStatusCode == 502
                    || httpStatusCode == 503
                    || httpStatusCode == 504;
        }
    }

    private static final class SearchRequest {

        @NonNull
        private final String searchQuery;

        @NonNull
        private final String requestUrl;

        @NonNull
        private final String preferredIsbn;

        private final boolean exactIsbnSearch;

        private final int maximumResults;

        private SearchRequest(
                @NonNull String searchQuery,
                @NonNull String requestUrl,
                @NonNull String preferredIsbn,
                boolean exactIsbnSearch,
                int maximumResults
        ) {
            this.searchQuery =
                    searchQuery;

            this.requestUrl =
                    requestUrl;

            this.preferredIsbn =
                    preferredIsbn;

            this.exactIsbnSearch =
                    exactIsbnSearch;

            this.maximumResults =
                    Math.max(
                            MINIMUM_RESULTS,
                            maximumResults
                    );
        }
    }

    private static final class IndustryIdentifiers {

        @NonNull
        private final String isbn10;

        @NonNull
        private final String isbn13;

        @NonNull
        private final String preferredIdentifier;

        @NonNull
        private final List<String> allIdentifiers;

        private IndustryIdentifiers(
                @NonNull String isbn10,
                @NonNull String isbn13,
                @NonNull String preferredIdentifier,
                @NonNull List<String> allIdentifiers
        ) {
            this.isbn10 =
                    isbn10;

            this.isbn13 =
                    isbn13;

            this.preferredIdentifier =
                    preferredIdentifier;

            this.allIdentifiers =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    allIdentifiers
                            )
                    );
        }

        private boolean contains(
                @Nullable String identifier
        ) {
            String safeIdentifier =
                    safeStaticIsbn(
                            identifier
                    );

            if (safeIdentifier.isEmpty()) {
                return false;
            }

            for (String existingIdentifier :
                    allIdentifiers) {

                if (safeIdentifier.equals(
                        existingIdentifier
                )) {
                    return true;
                }
            }

            return false;
        }

        @NonNull
        private static String safeStaticIsbn(
                @Nullable Object value
        ) {
            if (value == null) {
                return "";
            }

            String isbn =
                    value.toString()
                            .trim()
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
    }

    private static final class AccessInformation {

        @NonNull
        private final OnlineBookSearchResult.AccessType
                accessType;

        private final boolean previewAllowed;

        private final boolean downloadAllowed;

        private final boolean publicDomain;

        @NonNull
        private final String previewUrl;

        @NonNull
        private final String authorizedDownloadUrl;

        @NonNull
        private final String downloadMimeType;

        private AccessInformation(
                @NonNull OnlineBookSearchResult.AccessType
                        accessType,
                boolean previewAllowed,
                boolean downloadAllowed,
                boolean publicDomain,
                @NonNull String previewUrl,
                @NonNull String authorizedDownloadUrl,
                @NonNull String downloadMimeType
        ) {
            this.accessType =
                    accessType;

            this.previewAllowed =
                    previewAllowed;

            this.downloadAllowed =
                    downloadAllowed;

            this.publicDomain =
                    publicDomain;

            this.previewUrl =
                    previewUrl;

            this.authorizedDownloadUrl =
                    authorizedDownloadUrl;

            this.downloadMimeType =
                    downloadMimeType;
        }
    }

    private static final class DownloadInformation {

        private final boolean downloadAllowed;

        @NonNull
        private final String downloadUrl;

        @NonNull
        private final String mimeType;

        private DownloadInformation(
                boolean downloadAllowed,
                @NonNull String downloadUrl,
                @NonNull String mimeType
        ) {
            this.downloadAllowed =
                    downloadAllowed;

            this.downloadUrl =
                    downloadUrl;

            this.mimeType =
                    mimeType;
        }
    }
}