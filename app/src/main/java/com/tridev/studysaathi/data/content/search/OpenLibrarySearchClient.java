package com.tridev.studysaathi.data.content.search;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model.OnlineBookSearchResult;
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

public final class OpenLibrarySearchClient implements AutoCloseable {

    private static final String SEARCH_URL =
            "https://openlibrary.org/search.json";

    private static final String BASE_URL =
            "https://openlibrary.org";

    private static final String COVER_URL =
            "https://covers.openlibrary.org/b/id/";

    private static final String USER_AGENT =
            "StudySaathi-Android/1.0 (educational book discovery)";

    private static final String SEARCH_FIELDS =
            "key,title,subtitle,author_name,publisher,publish_date,"
                    + "first_publish_year,edition_name,isbn,cover_i,"
                    + "edition_key,language,number_of_pages_median,"
                    + "subject,has_fulltext,public_scan_b,ebook_access";

    private static final int DEFAULT_MAX_RESULTS =
            20;

    private static final int MIN_RESULTS =
            1;

    private static final int MAX_RESULTS =
            40;

    private static final int CONNECT_TIMEOUT_MS =
            15_000;

    private static final int READ_TIMEOUT_MS =
            25_000;

    private static final int MAX_ERROR_LENGTH =
            500;

    private static final int MAX_CATEGORIES =
            12;

    private static final long MIN_REQUEST_INTERVAL_MS =
            1_100L;

    private static final Pattern CLASS_PATTERN =
            Pattern.compile(
                    "(?i)\\b(?:class|grade|standard|std\\.?)"
                            + "\\s*[-:]?\\s*(1[0-2]|[1-9])\\b"
            );

    @NonNull
    private final ExecutorService networkExecutor =
            Executors.newSingleThreadExecutor();

    @NonNull
    private final Handler mainThreadHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final Object rateLimitLock =
            new Object();

    private long lastRequestStartedAt;

    private volatile boolean closed;

    public void searchBooks(
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedBook,
            @NonNull SearchCallback callback
    ) {
        searchBooks(
                detectedBook,
                DEFAULT_MAX_RESULTS,
                callback
        );
    }

    public void searchBooks(
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedBook,
            int maximumResults,
            @NonNull SearchCallback callback
    ) {
        if (closed) {
            dispatchFailure(
                    callback,
                    new OpenLibrarySearchException(
                            "Open Library search client has already been closed."
                    )
            );

            return;
        }

        final SearchRequest request;

        try {
            request =
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
                                            request
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
                    new OpenLibrarySearchException(
                            "Open Library search could not be started.",
                            exception
                    )
            );
        }
    }

    @NonNull
    private SearchRequest createSearchRequest(
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedBook,
            int maximumResults
    ) throws IOException, OpenLibrarySearchException {

        int safeMaximumResults =
                Math.max(
                        MIN_RESULTS,
                        Math.min(
                                MAX_RESULTS,
                                maximumResults
                        )
                );

        String preferredIsbn =
                normalizeIsbn(
                        detectedBook.getPreferredIsbn()
                );

        boolean exactIsbnSearch =
                !preferredIsbn.isEmpty();

        String query =
                exactIsbnSearch
                        ? "isbn:" + preferredIsbn
                        : createMetadataQuery(
                        detectedBook
                );

        if (query.isEmpty()) {
            throw new OpenLibrarySearchException(
                    "Book title or ISBN is required before starting Open Library search."
            );
        }

        StringBuilder requestUrl =
                new StringBuilder(
                        SEARCH_URL
                )
                        .append(
                                "?q="
                        )
                        .append(
                                encode(
                                        query
                                )
                        )
                        .append(
                                "&fields="
                        )
                        .append(
                                encode(
                                        SEARCH_FIELDS
                                )
                        )
                        .append(
                                "&limit="
                        )
                        .append(
                                safeMaximumResults
                        );

        String languageCode =
                convertMediumToLanguageCode(
                        detectedBook.getStudyMedium()
                );

        if (!languageCode.isEmpty()) {
            requestUrl.append(
                    "&lang="
            );

            requestUrl.append(
                    encode(
                            languageCode
                    )
            );
        }

        return new SearchRequest(
                query,
                requestUrl.toString(),
                preferredIsbn,
                exactIsbnSearch
        );
    }

    @NonNull
    private String createMetadataQuery(
            @NonNull BookCoverMetadataExtractor.DetectedBookMetadata detectedBook
    ) {
        StringBuilder query =
                new StringBuilder();

        appendFieldQuery(
                query,
                "title",
                detectedBook.getBookTitle()
        );

        appendFieldQuery(
                query,
                "author",
                detectedBook.getAuthorName()
        );

        appendFieldQuery(
                query,
                "publisher",
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
            @NonNull String field,
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
                field
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
            @NonNull SearchRequest request
    ) throws IOException,
            JSONException,
            OpenLibrarySearchException {

        waitForRateLimitWindow();

        HttpURLConnection connection =
                null;

        try {
            URL url =
                    new URL(
                            request.requestUrl
                    );

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT_MS
            );

            connection.setReadTimeout(
                    READ_TIMEOUT_MS
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
                    "X-OpenLibrary-Client",
                    "StudySaathi-Android"
            );

            int responseCode =
                    connection.getResponseCode();

            InputStream stream =
                    responseCode >= 200
                            && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            String body =
                    readBody(
                            stream
                    );

            if (responseCode < 200
                    || responseCode >= 300) {

                throw new OpenLibrarySearchException(
                        createHttpErrorMessage(
                                responseCode,
                                body
                        ),
                        responseCode
                );
            }

            return parseResponse(
                    request,
                    body
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void waitForRateLimitWindow()
            throws IOException {

        synchronized (rateLimitLock) {
            long now =
                    System.currentTimeMillis();

            long elapsed =
                    now - lastRequestStartedAt;

            long waitTime =
                    MIN_REQUEST_INTERVAL_MS
                            - elapsed;

            if (lastRequestStartedAt > 0L
                    && waitTime > 0L) {

                try {
                    Thread.sleep(
                            waitTime
                    );

                } catch (InterruptedException exception) {
                    Thread.currentThread()
                            .interrupt();

                    throw new IOException(
                            "Open Library request was interrupted.",
                            exception
                    );
                }
            }

            lastRequestStartedAt =
                    System.currentTimeMillis();
        }
    }

    @NonNull
    private SearchResponse parseResponse(
            @NonNull SearchRequest request,
            @NonNull String body
    ) throws JSONException,
            OpenLibrarySearchException {

        if (body.trim()
                .isEmpty()) {

            throw new OpenLibrarySearchException(
                    "Open Library returned an empty response."
            );
        }

        JSONObject root =
                new JSONObject(
                        body
                );

        int totalItems =
                Math.max(
                        0,
                        root.optInt(
                                "numFound",
                                root.optInt(
                                        "num_found",
                                        0
                                )
                        )
                );

        JSONArray docs =
                root.optJSONArray(
                        "docs"
                );

        List<OnlineBookSearchResult> parsedResults =
                new ArrayList<>();

        long searchedAt =
                System.currentTimeMillis();

        if (docs != null) {
            for (int index = 0;
                 index < docs.length();
                 index++) {

                JSONObject document =
                        docs.optJSONObject(
                                index
                        );

                if (document == null) {
                    continue;
                }

                OnlineBookSearchResult result =
                        parseDocument(
                                document,
                                request.preferredIsbn,
                                searchedAt
                        );

                if (result != null) {
                    parsedResults.add(
                            result
                    );
                }
            }
        }

        return new SearchResponse(
                request.query,
                totalItems,
                removeDuplicates(
                        parsedResults
                ),
                searchedAt,
                request.exactIsbnSearch
        );
    }

    @Nullable
    private OnlineBookSearchResult parseDocument(
            @NonNull JSONObject document,
            @NonNull String requestedIsbn,
            long searchedAt
    ) {
        String title =
                jsonText(
                        document,
                        "title"
                );

        if (title.isEmpty()) {
            return null;
        }

        String workKey =
                jsonText(
                        document,
                        "key"
                );

        String editionKey =
                firstArrayText(
                        document.optJSONArray(
                                "edition_key"
                        )
                );

        String providerBookId =
                editionKey.isEmpty()
                        ? workKey
                        : editionKey;

        String subtitle =
                jsonText(
                        document,
                        "subtitle"
                );

        List<String> authors =
                jsonTextList(
                        document.optJSONArray(
                                "author_name"
                        )
                );

        String publisher =
                firstArrayText(
                        document.optJSONArray(
                                "publisher"
                        )
                );

        String publicationDate =
                firstArrayText(
                        document.optJSONArray(
                                "publish_date"
                        )
                );

        String publicationYear =
                publicationYear(
                        document,
                        publicationDate
                );

        String editionName =
                firstArrayText(
                        document.optJSONArray(
                                "edition_name"
                        )
                );

        IsbnInformation isbn =
                isbnInformation(
                        document.optJSONArray(
                                "isbn"
                        ),
                        requestedIsbn
                );

        List<String> categories =
                jsonTextList(
                        document.optJSONArray(
                                "subject"
                        )
                );

        String subject =
                categories.isEmpty()
                        ? subjectFromTitle(
                        title
                )
                        : categories.get(
                        0
                );

        String className =
                classFromTitle(
                        title
                                + " "
                                + subtitle
                );

        String languageCode =
                normalizeLanguageCode(
                        firstArrayText(
                                document.optJSONArray(
                                        "language"
                                )
                        )
                );

        int pageCount =
                Math.max(
                        0,
                        document.optInt(
                                "number_of_pages_median",
                                0
                        )
                );

        int coverId =
                Math.max(
                        0,
                        document.optInt(
                                "cover_i",
                                0
                        )
                );

        String smallCoverUrl =
                coverId > 0
                        ? COVER_URL
                          + coverId
                          + "-M.jpg?default=false"
                        : "";

        String largeCoverUrl =
                coverId > 0
                        ? COVER_URL
                          + coverId
                          + "-L.jpg?default=false"
                        : "";

        String informationUrl =
                informationUrl(
                        workKey,
                        editionKey,
                        isbn.preferredIdentifier
                );

        boolean hasFullText =
                document.optBoolean(
                        "has_fulltext",
                        false
                );

        boolean publicScan =
                document.optBoolean(
                        "public_scan_b",
                        false
                );

        String ebookAccess =
                jsonText(
                        document,
                        "ebook_access"
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        boolean previewAllowed =
                !informationUrl.isEmpty()
                        && (
                        hasFullText
                                || publicScan
                                || "borrowable".equals(
                                ebookAccess
                        )
                );

        OnlineBookSearchResult.AccessType accessType;

        if (publicScan) {
            accessType =
                    OnlineBookSearchResult
                            .AccessType
                            .FULL_ONLINE;

        } else if (hasFullText
                || "borrowable".equals(
                ebookAccess
        )) {

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

        OnlineBookSearchResult.Builder builder =
                OnlineBookSearchResult.builder(
                                createResultId(
                                        providerBookId
                                ),
                                OnlineBookSearchResult
                                        .BookProvider
                                        .OPEN_LIBRARY,
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
                                publisher
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
                                subject
                        )
                        .setClassName(
                                className
                        )
                        .setStudyMedium(
                                mediumFromLanguage(
                                        languageCode
                                )
                        )
                        .setIsbn10(
                                isbn.isbn10
                        )
                        .setIsbn13(
                                isbn.isbn13
                        )
                        .setIndustryIdentifier(
                                isbn.preferredIdentifier
                        )
                        .setLanguageCode(
                                languageCode
                        )
                        .setPageCount(
                                pageCount
                        )
                        .setSmallCoverImageUrl(
                                smallCoverUrl
                        )
                        .setLargeCoverImageUrl(
                                largeCoverUrl
                        )
                        .setInformationUrl(
                                informationUrl
                        )
                        .setPreviewUrl(
                                previewAllowed
                                        ? informationUrl
                                        : ""
                        )
                        .setOfficialSourceUrl(
                                ""
                        )
                        .setAuthorizedDownloadUrl(
                                ""
                        )
                        .setDownloadMimeType(
                                ""
                        )
                        .setAccessType(
                                accessType
                        )
                        .setLicenseType(
                                OnlineBookSearchResult
                                        .LicenseType
                                        .UNKNOWN
                        )
                        .setPreviewAllowed(
                                previewAllowed
                        )
                        .setDownloadAllowed(
                                false
                        )
                        .setOfficialSource(
                                false
                        )
                        .setOfficialSourceVerified(
                                false
                        )
                        .setPublicDomain(
                                false
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
                     && index < MAX_CATEGORIES;
             index++) {

            builder.addCategory(
                    categories.get(
                            index
                    )
            );
        }

        if (isbn.preferredIdentifier
                .isEmpty()) {

            builder.addWarning(
                    "Open Library result does not contain a verified ISBN."
            );
        }

        if (publisher.isEmpty()) {
            builder.addWarning(
                    "Publisher information is not available in this Open Library record."
            );
        }

        if (!requestedIsbn.isEmpty()
                && requestedIsbn.equals(
                isbn.preferredIdentifier
        )) {

            builder.addMatchReason(
                    "Open Library ISBN matches the scanned ISBN."
            );

        } else {
            builder.addMatchReason(
                    "Result was found using available title and book metadata."
            );
        }

        return builder.build();
    }

    @NonNull
    private List<OnlineBookSearchResult> removeDuplicates(
            @NonNull List<OnlineBookSearchResult> results
    ) {
        Map<String, OnlineBookSearchResult> unique =
                new LinkedHashMap<>();

        for (OnlineBookSearchResult result :
                results) {

            String isbn =
                    normalizeIsbn(
                            result.getPreferredIsbn()
                    );

            String key;

            if (!isbn.isEmpty()) {
                key =
                        "isbn:"
                                + isbn;

            } else if (!result.getProviderBookId()
                    .isEmpty()) {

                key =
                        "provider:"
                                + result.getProviderBookId();

            } else {
                key =
                        "title:"
                                + comparisonText(
                                result.getBookTitle()
                        )
                                + "|publisher:"
                                + comparisonText(
                                result.getPublisherName()
                        );
            }

            if (!unique.containsKey(
                    key
            )) {
                unique.put(
                        key,
                        result
                );
            }
        }

        return new ArrayList<>(
                unique.values()
        );
    }

    @NonNull
    private IsbnInformation isbnInformation(
            @Nullable JSONArray isbnArray,
            @NonNull String requestedIsbn
    ) {
        String isbn10 =
                "";

        String isbn13 =
                "";

        String exact =
                "";

        if (isbnArray != null) {
            for (int index = 0;
                 index < isbnArray.length();
                 index++) {

                String value =
                        normalizeIsbn(
                                isbnArray.optString(
                                        index,
                                        ""
                                )
                        );

                if (value.length() == 10
                        && isbn10.isEmpty()) {

                    isbn10 =
                            value;
                }

                if (value.length() == 13
                        && isbn13.isEmpty()) {

                    isbn13 =
                            value;
                }

                if (!requestedIsbn.isEmpty()
                        && requestedIsbn.equals(
                        value
                )) {
                    exact =
                            value;
                }
            }
        }

        String preferred =
                !exact.isEmpty()
                        ? exact
                        : (
                        !isbn13.isEmpty()
                        ? isbn13
                        : isbn10
                );

        return new IsbnInformation(
                isbn10,
                isbn13,
                preferred
        );
    }

    @NonNull
    private String informationUrl(
            @Nullable String workKey,
            @Nullable String editionKey,
            @Nullable String isbn
    ) {
        String safeWorkKey =
                safeText(
                        workKey
                );

        if (!safeWorkKey.isEmpty()) {
            return safeWorkKey.startsWith(
                    "/"
            )
                    ? BASE_URL
                      + safeWorkKey
                    : BASE_URL
                      + "/"
                      + safeWorkKey;
        }

        String safeEditionKey =
                safeText(
                        editionKey
                );

        if (!safeEditionKey.isEmpty()) {
            return BASE_URL
                    + "/books/"
                    + safeEditionKey;
        }

        String safeIsbn =
                normalizeIsbn(
                        isbn
                );

        return safeIsbn.isEmpty()
                ? ""
                : BASE_URL
                  + "/isbn/"
                  + safeIsbn;
    }

    @NonNull
    private String publicationYear(
            @NonNull JSONObject document,
            @Nullable String publicationDate
    ) {
        int firstPublishYear =
                document.optInt(
                        "first_publish_year",
                        0
                );

        if (firstPublishYear > 0) {
            return String.valueOf(
                    firstPublishYear
            );
        }

        Matcher matcher =
                Pattern.compile(
                                "(?:18|19|20)\\d{2}"
                        )
                        .matcher(
                                safeText(
                                        publicationDate
                                )
                        );

        return matcher.find()
                ? matcher.group()
                : "";
    }

    @NonNull
    private String classFromTitle(
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
        String code =
                safeText(
                        value
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        switch (code) {
            case "eng":
                return "en";

            case "hin":
                return "hi";

            case "san":
                return "sa";

            default:
                return code;
        }
    }

    @NonNull
    private String mediumFromLanguage(
            @Nullable String value
    ) {
        switch (normalizeLanguageCode(
                value
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
    private String convertMediumToLanguageCode(
            @Nullable String value
    ) {
        String medium =
                comparisonText(
                        value
                );

        if (medium.contains(
                "english"
        )) {
            return "en";
        }

        if (medium.contains(
                "hindi"
        )
                || medium.contains(
                "हिंदी"
        )
                || medium.contains(
                "हिन्दी"
        )) {

            return "hi";
        }

        return "";
    }

    @NonNull
    private String createResultId(
            @Nullable String providerBookId
    ) {
        String id =
                safeText(
                        providerBookId
                );

        if (id.isEmpty()) {
            id =
                    UUID.randomUUID()
                            .toString();
        }

        return "open_library_"
                + id;
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
                    && !values.contains(
                    value
            )) {

                values.add(
                        value
                );
            }
        }

        return values;
    }

    @NonNull
    private String firstArrayText(
            @Nullable JSONArray array
    ) {
        if (array == null) {
            return "";
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

            if (!value.isEmpty()) {
                return value;
            }
        }

        return "";
    }

    @NonNull
    private String readBody(
            @Nullable InputStream stream
    ) throws IOException {

        if (stream == null) {
            return "";
        }

        StringBuilder body =
                new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     stream,
                                     StandardCharsets.UTF_8
                             )
                     )) {

            String line;

            while ((line = reader.readLine())
                    != null) {

                body.append(
                        line
                );
            }
        }

        return body.toString();
    }

    @NonNull
    private String createHttpErrorMessage(
            int responseCode,
            @Nullable String responseBody
    ) {
        String body =
                safeText(
                        responseBody
                );

        if (body.length()
                > MAX_ERROR_LENGTH) {

            body =
                    body.substring(
                            0,
                            MAX_ERROR_LENGTH
                    );
        }

        String reason;

        switch (responseCode) {
            case 403:
                reason =
                        "Open Library temporarily refused this request.";
                break;

            case 429:
                reason =
                        "Open Library rate limit was reached.";
                break;

            case 500:
            case 502:
            case 503:
            case 504:
                reason =
                        "Open Library service is temporarily unavailable.";
                break;

            default:
                reason =
                        "Open Library search failed.";
                break;
        }

        return body.isEmpty()
                ? reason
                  + " HTTP Status: "
                  + responseCode
                : reason
                  + " HTTP Status: "
                  + responseCode
                  + ". Response: "
                  + body;
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
            @Nullable String value
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
            @Nullable String value
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
        private final List<OnlineBookSearchResult> bookResults;

        private final long searchedAt;

        private final boolean exactIsbnSearch;

        private SearchResponse(
                @NonNull String searchQuery,
                int totalItems,
                @NonNull List<OnlineBookSearchResult> bookResults,
                long searchedAt,
                boolean exactIsbnSearch
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
        }

        @NonNull
        public String getSearchQuery() {
            return searchQuery;
        }

        public int getTotalItems() {
            return totalItems;
        }

        @NonNull
        public List<OnlineBookSearchResult> getBookResults() {
            return bookResults;
        }

        public long getSearchedAt() {
            return searchedAt;
        }

        public boolean isExactIsbnSearch() {
            return exactIsbnSearch;
        }

        public boolean hasResults() {
            return !bookResults.isEmpty();
        }

        public int getReturnedResultCount() {
            return bookResults.size();
        }
    }

    public static final class OpenLibrarySearchException
            extends Exception {

        private final int httpStatusCode;

        public OpenLibrarySearchException(
                @NonNull String message
        ) {
            this(
                    message,
                    0
            );
        }

        public OpenLibrarySearchException(
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

        public OpenLibrarySearchException(
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
            return httpStatusCode == 403
                    || httpStatusCode == 429;
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
        private final String query;

        @NonNull
        private final String requestUrl;

        @NonNull
        private final String preferredIsbn;

        private final boolean exactIsbnSearch;

        private SearchRequest(
                @NonNull String query,
                @NonNull String requestUrl,
                @NonNull String preferredIsbn,
                boolean exactIsbnSearch
        ) {
            this.query =
                    query;

            this.requestUrl =
                    requestUrl;

            this.preferredIsbn =
                    preferredIsbn;

            this.exactIsbnSearch =
                    exactIsbnSearch;
        }
    }

    private static final class IsbnInformation {

        @NonNull
        private final String isbn10;

        @NonNull
        private final String isbn13;

        @NonNull
        private final String preferredIdentifier;

        private IsbnInformation(
                @NonNull String isbn10,
                @NonNull String isbn13,
                @NonNull String preferredIdentifier
        ) {
            this.isbn10 =
                    isbn10;

            this.isbn13 =
                    isbn13;

            this.preferredIdentifier =
                    preferredIdentifier;
        }
    }
}