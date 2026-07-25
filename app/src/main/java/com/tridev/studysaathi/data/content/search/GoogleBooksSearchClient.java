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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public final class GoogleBooksSearchClient
        implements AutoCloseable {

    private static final String GOOGLE_BOOKS_SEARCH_URL =
            "https://www.googleapis.com/books/v1/volumes";

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

    private static final int MAXIMUM_ERROR_MESSAGE_LENGTH =
            500;

    @NonNull
    private final ExecutorService networkExecutor;

    @NonNull
    private final Handler mainThreadHandler;

    @NonNull
    private final String apiKey;

    private volatile boolean closed;

    public GoogleBooksSearchClient() {
        this("");
    }

    /**
     * API key must not be hard-coded directly inside
     * this class. It will later be supplied through
     * secure application configuration.
     */
    public GoogleBooksSearchClient(
            @Nullable String apiKey
    ) {
        this.apiKey =
                normalizeOptionalText(
                        apiKey
                );

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
                            "Google Books search client "
                                    + "has already been closed."
                    )
            );

            return;
        }

        String searchQuery =
                createSearchQuery(
                        detectedBook
                );

        if (searchQuery.isEmpty()) {
            dispatchFailure(
                    callback,
                    new GoogleBooksSearchException(
                            "Book title or ISBN is required "
                                    + "before starting online search."
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

        try {
            networkExecutor.execute(
                    () -> {
                        try {
                            SearchResponse response =
                                    executeSearch(
                                            searchQuery,
                                            safeMaximumResults
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
                            "Book search could not be started.",
                            exception
                    )
            );
        }
    }

    @NonNull
    private SearchResponse executeSearch(
            @NonNull String searchQuery,
            int maximumResults
    ) throws IOException,
            JSONException,
            GoogleBooksSearchException {

        String requestUrl =
                createRequestUrl(
                        searchQuery,
                        maximumResults
                );

        HttpURLConnection connection =
                null;

        try {
            URL url =
                    new URL(
                            requestUrl
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
                    false
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
                    "StudySaathi-Android/1.0"
            );

            int responseCode =
                    connection.getResponseCode();

            InputStream responseStream;

            if (responseCode >= 200
                    && responseCode < 300) {

                responseStream =
                        connection.getInputStream();

            } else {
                responseStream =
                        connection.getErrorStream();
            }

            String responseBody =
                    readResponseBody(
                            responseStream
                    );

            if (responseCode < 200
                    || responseCode >= 300) {

                throw new GoogleBooksSearchException(
                        createHttpErrorMessage(
                                responseCode,
                                responseBody
                        ),
                        responseCode
                );
            }

            return parseSearchResponse(
                    searchQuery,
                    responseBody
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private String createRequestUrl(
            @NonNull String searchQuery,
            int maximumResults
    ) throws IOException {

        String encodedQuery =
                URLEncoder.encode(
                        searchQuery,
                        StandardCharsets.UTF_8.name()
                );

        StringBuilder urlBuilder =
                new StringBuilder(
                        GOOGLE_BOOKS_SEARCH_URL
                );

        urlBuilder.append(
                "?q="
        );

        urlBuilder.append(
                encodedQuery
        );

        urlBuilder.append(
                "&maxResults="
        );

        urlBuilder.append(
                maximumResults
        );

        urlBuilder.append(
                "&printType=books"
        );

        urlBuilder.append(
                "&projection=full"
        );

        urlBuilder.append(
                "&orderBy=relevance"
        );

        if (!apiKey.isEmpty()) {
            urlBuilder.append(
                    "&key="
            );

            urlBuilder.append(
                    URLEncoder.encode(
                            apiKey,
                            StandardCharsets.UTF_8.name()
                    )
            );
        }

        return urlBuilder.toString();
    }

    @NonNull
    private String createSearchQuery(
            @NonNull BookCoverMetadataExtractor
                    .DetectedBookMetadata detectedBook
    ) {
        String preferredIsbn =
                normalizeIsbn(
                        detectedBook.getPreferredIsbn()
                );

        if (!preferredIsbn.isEmpty()) {
            return "isbn:"
                    + preferredIsbn;
        }

        StringBuilder queryBuilder =
                new StringBuilder();

        String bookTitle =
                normalizeOptionalText(
                        detectedBook.getBookTitle()
                );

        if (!bookTitle.isEmpty()) {
            queryBuilder.append(
                    "intitle:\""
            );

            queryBuilder.append(
                    removeQuotationMarks(
                            bookTitle
                    )
            );

            queryBuilder.append(
                    "\""
            );
        }

        String subjectName =
                normalizeOptionalText(
                        detectedBook.getSubjectName()
                );

        if (!subjectName.isEmpty()) {
            appendQueryPart(
                    queryBuilder,
                    subjectName
            );
        }

        String className =
                normalizeOptionalText(
                        detectedBook.getClassName()
                );

        if (!className.isEmpty()) {
            appendQueryPart(
                    queryBuilder,
                    className
            );
        }

        if (queryBuilder.length() == 0) {
            return normalizeOptionalText(
                    detectedBook.getOnlineSearchQuery()
            );
        }

        return queryBuilder
                .toString()
                .trim();
    }

    private void appendQueryPart(
            @NonNull StringBuilder queryBuilder,
            @Nullable String value
    ) {
        String safeValue =
                normalizeOptionalText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (queryBuilder.length() > 0) {
            queryBuilder.append(
                    ' '
            );
        }

        queryBuilder.append(
                safeValue
        );
    }

    @NonNull
    private SearchResponse parseSearchResponse(
            @NonNull String searchQuery,
            @NonNull String responseBody
    ) throws JSONException,
            GoogleBooksSearchException {

        if (responseBody.trim().isEmpty()) {
            throw new GoogleBooksSearchException(
                    "Google Books returned an empty response."
            );
        }

        JSONObject rootObject =
                new JSONObject(
                        responseBody
                );

        int totalItems =
                Math.max(
                        0,
                        rootObject.optInt(
                                "totalItems",
                                0
                        )
                );

        JSONArray itemArray =
                rootObject.optJSONArray(
                        "items"
                );

        List<OnlineBookSearchResult> results =
                new ArrayList<>();

        long searchedAt =
                System.currentTimeMillis();

        if (itemArray != null) {
            for (int index = 0;
                 index < itemArray.length();
                 index++) {

                JSONObject itemObject =
                        itemArray.optJSONObject(
                                index
                        );

                if (itemObject == null) {
                    continue;
                }

                OnlineBookSearchResult result =
                        parseVolumeResult(
                                itemObject,
                                searchedAt
                        );

                if (result != null) {
                    results.add(
                            result
                    );
                }
            }
        }

        return new SearchResponse(
                searchQuery,
                totalItems,
                results,
                searchedAt
        );
    }

    @Nullable
    private OnlineBookSearchResult parseVolumeResult(
            @NonNull JSONObject itemObject,
            long searchedAt
    ) {
        JSONObject volumeInfo =
                itemObject.optJSONObject(
                        "volumeInfo"
                );

        if (volumeInfo == null) {
            return null;
        }

        String bookTitle =
                getJsonText(
                        volumeInfo,
                        "title"
                );

        if (bookTitle.isEmpty()) {
            return null;
        }

        String providerBookId =
                getJsonText(
                        itemObject,
                        "id"
                );

        String resultId =
                createResultId(
                        providerBookId
                );

        String subtitle =
                getJsonText(
                        volumeInfo,
                        "subtitle"
                );

        String publisher =
                getJsonText(
                        volumeInfo,
                        "publisher"
                );

        String publicationDate =
                getJsonText(
                        volumeInfo,
                        "publishedDate"
                );

        String publicationYear =
                extractPublicationYear(
                        publicationDate
                );

        String description =
                getJsonText(
                        volumeInfo,
                        "description"
                );

        String languageCode =
                getJsonText(
                        volumeInfo,
                        "language"
                );

        int pageCount =
                Math.max(
                        0,
                        volumeInfo.optInt(
                                "pageCount",
                                0
                        )
                );

        String informationUrl =
                normalizeWebUrl(
                        getJsonText(
                                volumeInfo,
                                "infoLink"
                        )
                );

        String previewUrl =
                normalizeWebUrl(
                        getJsonText(
                                volumeInfo,
                                "previewLink"
                        )
                );

        String editionName =
                getJsonText(
                        volumeInfo,
                        "edition"
                );

        JSONArray authorArray =
                volumeInfo.optJSONArray(
                        "authors"
                );

        JSONArray categoryArray =
                volumeInfo.optJSONArray(
                        "categories"
                );

        String subjectName =
                getFirstArrayText(
                        categoryArray
                );

        IsbnInformation isbnInformation =
                extractIsbnInformation(
                        volumeInfo.optJSONArray(
                                "industryIdentifiers"
                        )
                );

        ImageInformation imageInformation =
                extractImageInformation(
                        volumeInfo.optJSONObject(
                                "imageLinks"
                        )
                );

        AccessInformation accessInformation =
                extractAccessInformation(
                        itemObject.optJSONObject(
                                "accessInfo"
                        ),
                        previewUrl
                );

        OnlineBookSearchResult.Builder builder =
                OnlineBookSearchResult.builder(
                                resultId,
                                OnlineBookSearchResult
                                        .BookProvider
                                        .GOOGLE_BOOKS,
                                bookTitle
                        )
                        .setProviderBookId(
                                providerBookId
                        )
                        .setBookSubtitle(
                                subtitle
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
                        .setDescription(
                                description
                        )
                        .setSubjectName(
                                subjectName
                        )
                        .setStudyMedium(
                                convertLanguageToMedium(
                                        languageCode
                                )
                        )
                        .setIsbn10(
                                isbnInformation.isbn10
                        )
                        .setIsbn13(
                                isbnInformation.isbn13
                        )
                        .setIndustryIdentifier(
                                isbnInformation
                                        .preferredIdentifier
                        )
                        .setLanguageCode(
                                languageCode
                        )
                        .setPageCount(
                                pageCount
                        )
                        .setSmallCoverImageUrl(
                                imageInformation
                                        .smallImageUrl
                        )
                        .setLargeCoverImageUrl(
                                imageInformation
                                        .largeImageUrl
                        )
                        .setInformationUrl(
                                informationUrl
                        )
                        .setPreviewUrl(
                                accessInformation
                                        .preferredPreviewUrl
                        )
                        .setOfficialSourceUrl(
                                ""
                        )
                        .setAuthorizedDownloadUrl(
                                accessInformation
                                        .authorizedDownloadUrl
                        )
                        .setDownloadMimeType(
                                accessInformation
                                        .downloadMimeType
                        )
                        .setAccessType(
                                accessInformation
                                        .accessType
                        )
                        .setLicenseType(
                                accessInformation
                                        .licenseType
                        )
                        .setPreviewAllowed(
                                accessInformation
                                        .previewAllowed
                        )
                        .setDownloadAllowed(
                                accessInformation
                                        .downloadAllowed
                        )
                        .setOfficialSource(
                                false
                        )
                        .setOfficialSourceVerified(
                                false
                        )
                        .setPublicDomain(
                                accessInformation
                                        .publicDomain
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

        if (authorArray != null) {
            for (int index = 0;
                 index < authorArray.length();
                 index++) {

                builder.addAuthor(
                        authorArray.optString(
                                index,
                                ""
                        )
                );
            }
        }

        if (categoryArray != null) {
            for (int index = 0;
                 index < categoryArray.length();
                 index++) {

                builder.addCategory(
                        categoryArray.optString(
                                index,
                                ""
                        )
                );
            }
        }

        if (isbnInformation.preferredIdentifier
                .isEmpty()) {

            builder.addWarning(
                    "Google Books result does not "
                            + "contain an ISBN."
            );
        }

        if (accessInformation
                .unverifiedDownloadAvailable) {

            builder.addWarning(
                    "A download link was reported, "
                            + "but its usage rights still "
                            + "require separate verification."
            );
        }

        if (accessInformation.publicDomain) {
            builder.addMatchReason(
                    "Google Books marks this volume "
                            + "as public domain."
            );
        }

        return builder.build();
    }

    @NonNull
    private IsbnInformation extractIsbnInformation(
            @Nullable JSONArray identifierArray
    ) {
        String isbn10 =
                "";

        String isbn13 =
                "";

        String preferredIdentifier =
                "";

        if (identifierArray != null) {
            for (int index = 0;
                 index < identifierArray.length();
                 index++) {

                JSONObject identifierObject =
                        identifierArray.optJSONObject(
                                index
                        );

                if (identifierObject == null) {
                    continue;
                }

                String type =
                        getJsonText(
                                identifierObject,
                                "type"
                        )
                                .toUpperCase(
                                        Locale.ROOT
                                );

                String identifier =
                        normalizeIsbn(
                                getJsonText(
                                        identifierObject,
                                        "identifier"
                                )
                        );

                if (identifier.isEmpty()) {
                    continue;
                }

                if ("ISBN_13".equals(type)
                        || identifier.length() == 13) {

                    if (isbn13.isEmpty()) {
                        isbn13 =
                                identifier;
                    }

                } else if ("ISBN_10".equals(type)
                        || identifier.length() == 10) {

                    if (isbn10.isEmpty()) {
                        isbn10 =
                                identifier;
                    }
                }

                if (preferredIdentifier.isEmpty()) {
                    preferredIdentifier =
                            identifier;
                }
            }
        }

        if (!isbn13.isEmpty()) {
            preferredIdentifier =
                    isbn13;

        } else if (!isbn10.isEmpty()) {
            preferredIdentifier =
                    isbn10;
        }

        return new IsbnInformation(
                isbn10,
                isbn13,
                preferredIdentifier
        );
    }

    @NonNull
    private ImageInformation extractImageInformation(
            @Nullable JSONObject imageLinks
    ) {
        if (imageLinks == null) {
            return new ImageInformation(
                    "",
                    ""
            );
        }

        String smallImageUrl =
                firstNonEmpty(
                        normalizeImageUrl(
                                getJsonText(
                                        imageLinks,
                                        "thumbnail"
                                )
                        ),
                        normalizeImageUrl(
                                getJsonText(
                                        imageLinks,
                                        "smallThumbnail"
                                )
                        )
                );

        String largeImageUrl =
                firstNonEmpty(
                        normalizeImageUrl(
                                getJsonText(
                                        imageLinks,
                                        "extraLarge"
                                )
                        ),
                        normalizeImageUrl(
                                getJsonText(
                                        imageLinks,
                                        "large"
                                )
                        ),
                        normalizeImageUrl(
                                getJsonText(
                                        imageLinks,
                                        "medium"
                                )
                        ),
                        smallImageUrl
                );

        return new ImageInformation(
                smallImageUrl,
                largeImageUrl
        );
    }

    @NonNull
    private AccessInformation extractAccessInformation(
            @Nullable JSONObject accessInfo,
            @NonNull String volumePreviewUrl
    ) {
        if (accessInfo == null) {
            return AccessInformation.metadataOnly(
                    volumePreviewUrl
            );
        }

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

        String viewability =
                getJsonText(
                        accessInfo,
                        "viewability"
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        String webReaderLink =
                normalizeWebUrl(
                        getJsonText(
                                accessInfo,
                                "webReaderLink"
                        )
                );

        DownloadInformation pdfDownload =
                extractFormatDownload(
                        accessInfo.optJSONObject(
                                "pdf"
                        ),
                        "application/pdf"
                );

        DownloadInformation epubDownload =
                extractFormatDownload(
                        accessInfo.optJSONObject(
                                "epub"
                        ),
                        "application/epub+zip"
                );

        DownloadInformation preferredDownload;

        if (pdfDownload.available
                && !pdfDownload.downloadUrl.isEmpty()) {

            preferredDownload =
                    pdfDownload;

        } else {
            preferredDownload =
                    epubDownload;
        }

        String preferredPreviewUrl =
                firstNonEmpty(
                        webReaderLink,
                        volumePreviewUrl
                );

        boolean previewAllowed =
                embeddable
                        || !preferredPreviewUrl.isEmpty()
                        || "PARTIAL".equals(viewability)
                        || "ALL_PAGES".equals(viewability);

        boolean downloadLinkReported =
                preferredDownload.available
                        && !preferredDownload
                        .downloadUrl
                        .isEmpty();

        /*
         * A reported link is not treated as an
         * authorized automatic download unless
         * the API explicitly marks the volume
         * as public domain.
         */
        boolean downloadAllowed =
                publicDomain
                        && downloadLinkReported;

        String authorizedDownloadUrl =
                downloadAllowed
                        ? preferredDownload.downloadUrl
                        : "";

        String downloadMimeType =
                downloadAllowed
                        ? preferredDownload.mimeType
                        : "";

        OnlineBookSearchResult.AccessType accessType;

        if (downloadAllowed) {
            accessType =
                    OnlineBookSearchResult
                            .AccessType
                            .FULL_DOWNLOAD;

        } else if ("ALL_PAGES".equals(
                viewability
        )
                && previewAllowed) {

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

        OnlineBookSearchResult.LicenseType licenseType;

        if (publicDomain) {
            licenseType =
                    OnlineBookSearchResult
                            .LicenseType
                            .PUBLIC_DOMAIN;

        } else {
            licenseType =
                    OnlineBookSearchResult
                            .LicenseType
                            .UNKNOWN;
        }

        return new AccessInformation(
                preferredPreviewUrl,
                authorizedDownloadUrl,
                downloadMimeType,
                accessType,
                licenseType,
                previewAllowed,
                downloadAllowed,
                publicDomain,
                downloadLinkReported
                        && !downloadAllowed
        );
    }

    @NonNull
    private DownloadInformation extractFormatDownload(
            @Nullable JSONObject formatObject,
            @NonNull String mimeType
    ) {
        if (formatObject == null) {
            return DownloadInformation.unavailable(
                    mimeType
            );
        }

        boolean available =
                formatObject.optBoolean(
                        "isAvailable",
                        false
                );

        String downloadUrl =
                normalizeWebUrl(
                        getJsonText(
                                formatObject,
                                "downloadLink"
                        )
                );

        return new DownloadInformation(
                available,
                downloadUrl,
                mimeType
        );
    }

    @NonNull
    private String readResponseBody(
            @Nullable InputStream inputStream
    ) throws IOException {

        if (inputStream == null) {
            return "";
        }

        StringBuilder responseBuilder =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {
            String line;

            while ((line = reader.readLine())
                    != null) {

                responseBuilder.append(
                        line
                );
            }
        }

        return responseBuilder.toString();
    }

    @NonNull
    private String createHttpErrorMessage(
            int responseCode,
            @Nullable String responseBody
    ) {
        String serverMessage =
                extractServerErrorMessage(
                        responseBody
                );

        StringBuilder messageBuilder =
                new StringBuilder();

        messageBuilder.append(
                "Google Books search failed"
        );

        messageBuilder.append(
                " (HTTP "
        );

        messageBuilder.append(
                responseCode
        );

        messageBuilder.append(
                ")."
        );

        if (!serverMessage.isEmpty()) {
            messageBuilder.append(
                    " "
            );

            messageBuilder.append(
                    serverMessage
            );
        }

        return messageBuilder.toString();
    }

    @NonNull
    private String extractServerErrorMessage(
            @Nullable String responseBody
    ) {
        String safeBody =
                normalizeOptionalText(
                        responseBody
                );

        if (safeBody.isEmpty()) {
            return "";
        }

        try {
            JSONObject rootObject =
                    new JSONObject(
                            safeBody
                    );

            JSONObject errorObject =
                    rootObject.optJSONObject(
                            "error"
                    );

            if (errorObject != null) {
                String message =
                        getJsonText(
                                errorObject,
                                "message"
                        );

                if (!message.isEmpty()) {
                    return limitTextLength(
                            message,
                            MAXIMUM_ERROR_MESSAGE_LENGTH
                    );
                }
            }

        } catch (JSONException ignored) {
            // Fall back to limited plain response text.
        }

        return limitTextLength(
                safeBody,
                MAXIMUM_ERROR_MESSAGE_LENGTH
        );
    }

    @NonNull
    private String limitTextLength(
            @NonNull String value,
            int maximumLength
    ) {
        if (value.length()
                <= maximumLength) {

            return value;
        }

        return value.substring(
                        0,
                        maximumLength
                )
                .trim();
    }

    @NonNull
    private String getJsonText(
            @NonNull JSONObject object,
            @NonNull String fieldName
    ) {
        Object value =
                object.opt(
                        fieldName
                );

        if (value == null
                || value == JSONObject.NULL) {

            return "";
        }

        return String.valueOf(
                        value
                )
                .trim();
    }

    @NonNull
    private String getFirstArrayText(
            @Nullable JSONArray array
    ) {
        if (array == null
                || array.length() == 0) {

            return "";
        }

        return normalizeOptionalText(
                array.optString(
                        0,
                        ""
                )
        );
    }

    @NonNull
    private String extractPublicationYear(
            @Nullable String publicationDate
    ) {
        String safePublicationDate =
                normalizeOptionalText(
                        publicationDate
                );

        if (safePublicationDate.length() < 4) {
            return "";
        }

        String possibleYear =
                safePublicationDate.substring(
                        0,
                        4
                );

        if (possibleYear.matches(
                "\\d{4}"
        )) {
            return possibleYear;
        }

        return "";
    }

    @NonNull
    private String convertLanguageToMedium(
            @Nullable String languageCode
    ) {
        String safeLanguageCode =
                normalizeOptionalText(
                        languageCode
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        switch (safeLanguageCode) {
            case "hi":
                return "Hindi";

            case "en":
                return "English";

            case "sa":
                return "Sanskrit";

            default:
                return "";
        }
    }

    @NonNull
    private String createResultId(
            @Nullable String providerBookId
    ) {
        String safeProviderBookId =
                normalizeOptionalText(
                        providerBookId
                )
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9_-]",
                                "_"
                        )
                        .replaceAll(
                                "_+",
                                "_"
                        );

        while (safeProviderBookId.startsWith("_")) {
            safeProviderBookId =
                    safeProviderBookId.substring(
                            1
                    );
        }

        while (safeProviderBookId.endsWith("_")) {
            safeProviderBookId =
                    safeProviderBookId.substring(
                            0,
                            safeProviderBookId.length() - 1
                    );
        }

        if (safeProviderBookId.isEmpty()) {
            safeProviderBookId =
                    UUID.randomUUID()
                            .toString()
                            .replace(
                                    "-",
                                    ""
                            );
        }

        return "google_books_"
                + safeProviderBookId;
    }

    @NonNull
    private String normalizeIsbn(
            @Nullable String value
    ) {
        return normalizeOptionalText(
                value
        )
                .replaceAll(
                        "[^0-9Xx]",
                        ""
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }

    @NonNull
    private String normalizeWebUrl(
            @Nullable String value
    ) {
        String safeUrl =
                normalizeOptionalText(
                        value
                );

        if (safeUrl.startsWith(
                "http://"
        )) {
            return "https://"
                    + safeUrl.substring(
                    "http://".length()
            );
        }

        return safeUrl;
    }

    @NonNull
    private String normalizeImageUrl(
            @Nullable String value
    ) {
        return normalizeWebUrl(
                value
        );
    }

    @NonNull
    private String removeQuotationMarks(
            @NonNull String value
    ) {
        return value.replace(
                        "\"",
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
        for (String value : values) {
            String safeValue =
                    normalizeOptionalText(
                            value
                    );

            if (!safeValue.isEmpty()) {
                return safeValue;
            }
        }

        return "";
    }

    @NonNull
    private String normalizeOptionalText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
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
        private final List<OnlineBookSearchResult>
                bookResults;

        private final long searchedAt;

        private SearchResponse(
                @NonNull String searchQuery,
                int totalItems,
                @NonNull List<OnlineBookSearchResult>
                        bookResults,
                long searchedAt
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
                    searchedAt;
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

        public boolean hasResults() {
            return !bookResults.isEmpty();
        }

        public int getReturnedResultCount() {
            return bookResults.size();
        }
    }

    public static final class
    GoogleBooksSearchException
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

        public boolean isHttpError() {
            return httpStatusCode > 0;
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

    private static final class ImageInformation {

        @NonNull
        private final String smallImageUrl;

        @NonNull
        private final String largeImageUrl;

        private ImageInformation(
                @NonNull String smallImageUrl,
                @NonNull String largeImageUrl
        ) {
            this.smallImageUrl =
                    smallImageUrl;

            this.largeImageUrl =
                    largeImageUrl;
        }
    }

    private static final class DownloadInformation {

        private final boolean available;

        @NonNull
        private final String downloadUrl;

        @NonNull
        private final String mimeType;

        private DownloadInformation(
                boolean available,
                @NonNull String downloadUrl,
                @NonNull String mimeType
        ) {
            this.available =
                    available;

            this.downloadUrl =
                    downloadUrl;

            this.mimeType =
                    mimeType;
        }

        @NonNull
        private static DownloadInformation unavailable(
                @NonNull String mimeType
        ) {
            return new DownloadInformation(
                    false,
                    "",
                    mimeType
            );
        }
    }

    private static final class AccessInformation {

        @NonNull
        private final String preferredPreviewUrl;

        @NonNull
        private final String authorizedDownloadUrl;

        @NonNull
        private final String downloadMimeType;

        @NonNull
        private final OnlineBookSearchResult.AccessType
                accessType;

        @NonNull
        private final OnlineBookSearchResult.LicenseType
                licenseType;

        private final boolean previewAllowed;

        private final boolean downloadAllowed;

        private final boolean publicDomain;

        private final boolean unverifiedDownloadAvailable;

        private AccessInformation(
                @NonNull String preferredPreviewUrl,
                @NonNull String authorizedDownloadUrl,
                @NonNull String downloadMimeType,
                @NonNull OnlineBookSearchResult.AccessType
                        accessType,
                @NonNull OnlineBookSearchResult.LicenseType
                        licenseType,
                boolean previewAllowed,
                boolean downloadAllowed,
                boolean publicDomain,
                boolean unverifiedDownloadAvailable
        ) {
            this.preferredPreviewUrl =
                    preferredPreviewUrl;

            this.authorizedDownloadUrl =
                    authorizedDownloadUrl;

            this.downloadMimeType =
                    downloadMimeType;

            this.accessType =
                    accessType;

            this.licenseType =
                    licenseType;

            this.previewAllowed =
                    previewAllowed;

            this.downloadAllowed =
                    downloadAllowed;

            this.publicDomain =
                    publicDomain;

            this.unverifiedDownloadAvailable =
                    unverifiedDownloadAvailable;
        }

        @NonNull
        private static AccessInformation metadataOnly(
                @NonNull String previewUrl
        ) {
            boolean hasPreview =
                    !previewUrl.isEmpty();

            return new AccessInformation(
                    previewUrl,
                    "",
                    "",
                    hasPreview
                            ? OnlineBookSearchResult
                              .AccessType
                              .PARTIAL_PREVIEW
                            : OnlineBookSearchResult
                              .AccessType
                              .METADATA_ONLY,
                    OnlineBookSearchResult
                            .LicenseType
                            .UNKNOWN,
                    hasPreview,
                    false,
                    false,
                    false
            );
        }
    }
}