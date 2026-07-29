package com.tridev.studysaathi.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.tridev.studysaathi.backup.BackupDatabaseTablePolicy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public final class CloudBackupDownloader {

    private static final String COLLECTION_USERS =
            "users";

    private static final String COLLECTION_CLOUD_BACKUPS =
            "cloud_backups";

    private static final String COLLECTION_CHUNKS =
            "chunks";

    private static final String LATEST_BACKUP_DOCUMENT =
            "latest";

    private static final String STATUS_COMPLETE =
            "complete";

    private static final String BACKUP_TARGET =
            "cloud_firestore";

    private static final int MAX_RAW_JSON_BYTES =
            25 * 1024 * 1024;

    private static final int MAX_ENCODED_CHARACTERS =
            CloudBackupPayloadBuilder.MAX_CHUNK_COUNT
                    * CloudBackupPayloadBuilder
                    .CHUNK_CHARACTER_LIMIT;

    private static final ExecutorService
            payloadValidationExecutor =
            Executors.newSingleThreadExecutor();

    private final FirebaseFirestore firestore;

    private final Handler mainThreadHandler;

    private final String applicationPackageName;

    public CloudBackupDownloader(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        firestore =
                FirebaseFirestore.getInstance();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );

        applicationPackageName =
                applicationContext.getPackageName();
    }

    /**
     * Downloads and validates the latest cloud backup.
     *
     * The callback is always delivered on the main
     * application thread.
     */
    public void downloadLatestBackup(
            @NonNull FirebaseUser firebaseUser,
            @NonNull DownloadCallback callback
    ) {
        if (!firebaseUser.isEmailVerified()) {
            dispatchError(
                    callback,
                    new CloudBackupDownloadException(
                            "A verified email account is "
                                    + "required to download "
                                    + "the cloud backup."
                    )
            );

            return;
        }

        String userId =
                firebaseUser.getUid();

        if (userId.trim().isEmpty()) {
            dispatchError(
                    callback,
                    new CloudBackupDownloadException(
                            "Firebase user ID is unavailable."
                    )
            );

            return;
        }

        DocumentReference latestBackupReference =
                getLatestBackupReference(
                        userId
                );

        latestBackupReference
                .get(Source.SERVER)
                .addOnSuccessListener(
                        metadataSnapshot -> {
                            if (!metadataSnapshot.exists()) {
                                dispatchError(
                                        callback,
                                        new CloudBackupNotFoundException(
                                                "No cloud backup was "
                                                        + "found for this "
                                                        + "account."
                                        )
                                );

                                return;
                            }

                            BackupMetadata metadata;

                            try {
                                metadata =
                                        readAndValidateMetadata(
                                                metadataSnapshot,
                                                userId
                                        );

                            } catch (Exception exception) {
                                dispatchError(
                                        callback,
                                        exception
                                );

                                return;
                            }

                            downloadBackupChunks(
                                    firebaseUser,
                                    metadata,
                                    latestBackupReference,
                                    callback
                            );
                        }
                )
                .addOnFailureListener(
                        exception ->
                                dispatchError(
                                        callback,
                                        exception
                                )
                );
    }

    private void downloadBackupChunks(
            @NonNull FirebaseUser firebaseUser,
            @NonNull BackupMetadata metadata,
            @NonNull DocumentReference latestBackupReference,
            @NonNull DownloadCallback callback
    ) {
        CollectionReference chunksReference =
                latestBackupReference.collection(
                        COLLECTION_CHUNKS
                );

        chunksReference
                .orderBy(
                        "chunk_index",
                        Query.Direction.ASCENDING
                )
                .get(Source.SERVER)
                .addOnSuccessListener(
                        chunkSnapshot ->
                                validateDownloadedPayloadAsync(
                                        firebaseUser,
                                        metadata,
                                        chunkSnapshot,
                                        callback
                                )
                )
                .addOnFailureListener(
                        exception ->
                                dispatchError(
                                        callback,
                                        exception
                                )
                );
    }

    private void validateDownloadedPayloadAsync(
            @NonNull FirebaseUser firebaseUser,
            @NonNull BackupMetadata metadata,
            @NonNull QuerySnapshot chunkSnapshot,
            @NonNull DownloadCallback callback
    ) {
        payloadValidationExecutor.execute(() -> {
            try {
                FirebaseUser currentUser =
                        firebaseUser;

                if (!currentUser.isEmailVerified()) {
                    throw new CloudBackupDownloadException(
                            "The cloud account is no longer "
                                    + "verified."
                    );
                }

                CloudBackupDownloadResult result =
                        assembleAndValidatePayload(
                                currentUser.getUid(),
                                metadata,
                                chunkSnapshot
                        );

                dispatchSuccess(
                        callback,
                        result
                );

            } catch (Exception exception) {
                dispatchError(
                        callback,
                        exception
                );
            }
        });
    }

    @NonNull
    private BackupMetadata readAndValidateMetadata(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String expectedUserId
    ) throws CloudBackupDownloadException {

        String ownerUserId =
                getRequiredString(
                        snapshot,
                        "owner_uid"
                );

        String backupId =
                getRequiredString(
                        snapshot,
                        "backup_id"
                );

        String status =
                getRequiredString(
                        snapshot,
                        "status"
                );

        String backupFormat =
                getRequiredString(
                        snapshot,
                        "backup_format"
                );

        String compression =
                getRequiredString(
                        snapshot,
                        "compression"
                );

        String encoding =
                getRequiredString(
                        snapshot,
                        "encoding"
                );

        String checksum =
                getRequiredString(
                        snapshot,
                        "checksum_sha256"
                );

        String appPackage =
                getRequiredString(
                        snapshot,
                        "app_package"
                );

        int backupFormatVersion =
                getRequiredInt(
                        snapshot,
                        "backup_format_version"
                );

        int databaseSchemaVersion =
                getRequiredInt(
                        snapshot,
                        "database_schema_version"
                );

        int chunkCount =
                getRequiredInt(
                        snapshot,
                        "chunk_count"
                );

        int rawJsonBytes =
                getRequiredInt(
                        snapshot,
                        "raw_json_bytes"
                );

        int compressedBytes =
                getRequiredInt(
                        snapshot,
                        "compressed_bytes"
                );

        int encodedCharacters =
                getRequiredInt(
                        snapshot,
                        "encoded_characters"
                );

        long backupCreatedAt =
                getRequiredLong(
                        snapshot,
                        "backup_created_at"
                );

        long uploadedAt =
                getOptionalLong(
                        snapshot,
                        "uploaded_at_client"
                );

        if (uploadedAt <= 0L) {
            Timestamp serverUploadTimestamp =
                    snapshot.getTimestamp(
                            "uploaded_at"
                    );

            if (serverUploadTimestamp != null) {
                uploadedAt =
                        serverUploadTimestamp
                                .toDate()
                                .getTime();
            }
        }

        int profileCount =
                getOptionalInt(
                        snapshot,
                        "profile_count"
                );

        int lessonProgressCount =
                getOptionalInt(
                        snapshot,
                        "lesson_progress_count"
                );

        int quizAttemptCount =
                getOptionalInt(
                        snapshot,
                        "quiz_attempt_count"
                );

        int doubtCount =
                getOptionalInt(
                        snapshot,
                        "doubt_count"
                );

        int preferenceItemCount =
                getOptionalInt(
                        snapshot,
                        "preference_item_count"
                );

        if (!expectedUserId.equals(
                ownerUserId
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup owner does not "
                            + "match the signed-in account."
            );
        }

        if (!STATUS_COMPLETE.equals(
                status
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup is not marked "
                            + "as complete."
            );
        }

        if (!CloudBackupPayloadBuilder
                .BACKUP_FORMAT
                .equals(backupFormat)) {

            throw new CloudBackupDownloadException(
                    "Unsupported cloud backup format."
            );
        }

        if (backupFormatVersion
                != CloudBackupPayloadBuilder
                .BACKUP_FORMAT_VERSION) {

            throw new CloudBackupDownloadException(
                    "Unsupported cloud backup "
                            + "format version."
            );
        }

        if (!BackupDatabaseTablePolicy
                .isSupportedSchemaVersion(
                        databaseSchemaVersion
                )) {

            throw new CloudBackupDownloadException(
                    "Cloud backup database version "
                            + databaseSchemaVersion
                            + " is not compatible with "
                            + "database version "
                            + CloudBackupPayloadBuilder
                            .DATABASE_SCHEMA_VERSION
                            + "."
            );
        }

        if (!CloudBackupPayloadBuilder
                .COMPRESSION_TYPE
                .equals(compression)) {

            throw new CloudBackupDownloadException(
                    "Unsupported cloud backup "
                            + "compression type."
            );
        }

        if (!CloudBackupPayloadBuilder
                .ENCODING_TYPE
                .equals(encoding)) {

            throw new CloudBackupDownloadException(
                    "Unsupported cloud backup "
                            + "encoding type."
            );
        }

        if (!applicationPackageName.equals(
                appPackage
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup belongs to a "
                            + "different application package."
            );
        }

        if (backupId.trim().isEmpty()) {
            throw new CloudBackupDownloadException(
                    "Cloud backup ID is missing."
            );
        }

        if (!isValidSha256Checksum(
                checksum
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup checksum is invalid."
            );
        }

        if (chunkCount <= 0
                || chunkCount
                > CloudBackupPayloadBuilder
                .MAX_CHUNK_COUNT) {

            throw new CloudBackupDownloadException(
                    "Cloud backup chunk count is invalid."
            );
        }

        if (rawJsonBytes <= 0
                || rawJsonBytes
                > MAX_RAW_JSON_BYTES) {

            throw new CloudBackupDownloadException(
                    "Cloud backup raw data size "
                            + "is invalid."
            );
        }

        if (compressedBytes <= 0) {
            throw new CloudBackupDownloadException(
                    "Cloud backup compressed size "
                            + "is invalid."
            );
        }

        if (encodedCharacters <= 0
                || encodedCharacters
                > MAX_ENCODED_CHARACTERS) {

            throw new CloudBackupDownloadException(
                    "Cloud backup encoded size "
                            + "is invalid."
            );
        }

        if (backupCreatedAt <= 0L) {
            throw new CloudBackupDownloadException(
                    "Cloud backup creation time "
                            + "is invalid."
            );
        }

        if (profileCount < 0
                || lessonProgressCount < 0
                || quizAttemptCount < 0
                || doubtCount < 0
                || preferenceItemCount < 0) {

            throw new CloudBackupDownloadException(
                    "Cloud backup item counts "
                            + "are invalid."
            );
        }

        return new BackupMetadata(
                ownerUserId,
                backupId,
                status,
                backupFormat,
                backupFormatVersion,
                databaseSchemaVersion,
                compression,
                encoding,
                checksum,
                appPackage,
                backupCreatedAt,
                uploadedAt,
                chunkCount,
                rawJsonBytes,
                compressedBytes,
                encodedCharacters,
                profileCount,
                lessonProgressCount,
                quizAttemptCount,
                doubtCount,
                preferenceItemCount
        );
    }

    @NonNull
    private CloudBackupDownloadResult
    assembleAndValidatePayload(
            @NonNull String expectedUserId,
            @NonNull BackupMetadata metadata,
            @NonNull QuerySnapshot chunkSnapshot
    ) throws Exception {

        List<DocumentSnapshot> chunkDocuments =
                chunkSnapshot.getDocuments();

        if (chunkDocuments.size()
                != metadata.chunkCount) {

            throw new CloudBackupDownloadException(
                    "Cloud backup contains "
                            + chunkDocuments.size()
                            + " chunks, but "
                            + metadata.chunkCount
                            + " chunks were expected."
            );
        }

        StringBuilder encodedPayloadBuilder =
                new StringBuilder(
                        metadata.encodedCharacters
                );

        for (int expectedChunkIndex = 0;
             expectedChunkIndex
                     < chunkDocuments.size();
             expectedChunkIndex++) {

            DocumentSnapshot chunkDocument =
                    chunkDocuments.get(
                            expectedChunkIndex
                    );

            validateAndAppendChunk(
                    expectedUserId,
                    metadata,
                    expectedChunkIndex,
                    chunkDocument,
                    encodedPayloadBuilder
            );
        }

        String encodedPayload =
                encodedPayloadBuilder.toString();

        if (encodedPayload.length()
                != metadata.encodedCharacters) {

            throw new CloudBackupDownloadException(
                    "Cloud backup encoded length "
                            + "does not match its metadata."
            );
        }

        byte[] compressedPayload;

        try {
            compressedPayload =
                    Base64.decode(
                            encodedPayload,
                            Base64.NO_WRAP
                    );

        } catch (IllegalArgumentException exception) {
            throw new CloudBackupDownloadException(
                    "Cloud backup Base64 payload "
                            + "is invalid.",
                    exception
            );
        }

        if (compressedPayload.length
                != metadata.compressedBytes) {

            throw new CloudBackupDownloadException(
                    "Cloud backup compressed size "
                            + "does not match its metadata."
            );
        }

        String actualChecksum =
                createSha256Checksum(
                        compressedPayload
                );

        if (!metadata.checksumSha256
                .equalsIgnoreCase(
                        actualChecksum
                )) {

            throw new CloudBackupDownloadException(
                    "Cloud backup checksum failed. "
                            + "The backup may be corrupted."
            );
        }

        byte[] rawJsonPayload =
                decompressGzip(
                        compressedPayload
                );

        if (rawJsonPayload.length
                != metadata.rawJsonBytes) {

            throw new CloudBackupDownloadException(
                    "Cloud backup decompressed size "
                            + "does not match its metadata."
            );
        }

        String jsonText =
                new String(
                        rawJsonPayload,
                        StandardCharsets.UTF_8
                );

        JSONObject backupJson;

        try {
            backupJson =
                    new JSONObject(
                            jsonText
                    );

        } catch (JSONException exception) {
            throw new CloudBackupDownloadException(
                    "Cloud backup JSON is invalid.",
                    exception
            );
        }

        validateBackupJson(
                backupJson,
                metadata
        );

        return new CloudBackupDownloadResult(
                backupJson,
                metadata.backupId,
                metadata.backupCreatedAt,
                metadata.uploadedAt,
                metadata.checksumSha256,
                metadata.chunkCount,
                metadata.rawJsonBytes,
                metadata.compressedBytes,
                metadata.profileCount,
                metadata.lessonProgressCount,
                metadata.quizAttemptCount,
                metadata.doubtCount,
                metadata.preferenceItemCount
        );
    }

    private void validateAndAppendChunk(
            @NonNull String expectedUserId,
            @NonNull BackupMetadata metadata,
            int expectedChunkIndex,
            @NonNull DocumentSnapshot chunkDocument,
            @NonNull StringBuilder encodedPayloadBuilder
    ) throws CloudBackupDownloadException {

        String expectedDocumentId =
                String.format(
                        Locale.US,
                        "chunk_%03d",
                        expectedChunkIndex
                );

        if (!expectedDocumentId.equals(
                chunkDocument.getId()
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup chunk document "
                            + expectedDocumentId
                            + " is missing or out of order."
            );
        }

        String ownerUserId =
                getRequiredString(
                        chunkDocument,
                        "owner_uid"
                );

        String backupId =
                getRequiredString(
                        chunkDocument,
                        "backup_id"
                );

        String compression =
                getRequiredString(
                        chunkDocument,
                        "compression"
                );

        String encoding =
                getRequiredString(
                        chunkDocument,
                        "encoding"
                );

        String checksum =
                getRequiredString(
                        chunkDocument,
                        "checksum_sha256"
                );

        String payload =
                getRequiredString(
                        chunkDocument,
                        "payload"
                );

        int chunkIndex =
                getRequiredInt(
                        chunkDocument,
                        "chunk_index"
                );

        int chunkCount =
                getRequiredInt(
                        chunkDocument,
                        "chunk_count"
                );

        long backupCreatedAt =
                getRequiredLong(
                        chunkDocument,
                        "backup_created_at"
                );

        if (!expectedUserId.equals(
                ownerUserId
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup chunk owner "
                            + "does not match the account."
            );
        }

        if (!metadata.backupId.equals(
                backupId
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup chunk belongs "
                            + "to a different backup."
            );
        }

        if (chunkIndex
                != expectedChunkIndex) {

            throw new CloudBackupDownloadException(
                    "Cloud backup chunk sequence "
                            + "is invalid."
            );
        }

        if (chunkCount
                != metadata.chunkCount) {

            throw new CloudBackupDownloadException(
                    "Cloud backup chunk count "
                            + "does not match metadata."
            );
        }

        if (!metadata.compression.equals(
                compression
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup chunk compression "
                            + "does not match metadata."
            );
        }

        if (!metadata.encoding.equals(
                encoding
        )) {
            throw new CloudBackupDownloadException(
                    "Cloud backup chunk encoding "
                            + "does not match metadata."
            );
        }

        if (!metadata.checksumSha256
                .equalsIgnoreCase(
                        checksum
                )) {

            throw new CloudBackupDownloadException(
                    "Cloud backup chunk checksum "
                            + "does not match metadata."
            );
        }

        if (backupCreatedAt
                != metadata.backupCreatedAt) {

            throw new CloudBackupDownloadException(
                    "Cloud backup chunk creation "
                            + "time does not match metadata."
            );
        }

        if (payload.isEmpty()
                || payload.length()
                > CloudBackupPayloadBuilder
                .CHUNK_CHARACTER_LIMIT) {

            throw new CloudBackupDownloadException(
                    "Cloud backup chunk size "
                            + "is invalid."
            );
        }

        if (encodedPayloadBuilder.length()
                + payload.length()
                > MAX_ENCODED_CHARACTERS) {

            throw new CloudBackupDownloadException(
                    "Cloud backup encoded payload "
                            + "is too large."
            );
        }

        encodedPayloadBuilder.append(
                payload
        );
    }

    private void validateBackupJson(
            @NonNull JSONObject backupJson,
            @NonNull BackupMetadata metadata
    ) throws CloudBackupDownloadException {

        try {
            if (!CloudBackupPayloadBuilder
                    .BACKUP_FORMAT
                    .equals(
                            backupJson.getString(
                                    "backup_format"
                            )
                    )) {

                throw new CloudBackupDownloadException(
                        "Downloaded JSON backup format "
                                + "is invalid."
                );
            }

            if (backupJson.getInt(
                    "backup_format_version"
            ) != CloudBackupPayloadBuilder
                    .BACKUP_FORMAT_VERSION) {

                throw new CloudBackupDownloadException(
                        "Downloaded JSON backup version "
                                + "is unsupported."
                );
            }

            int backupSchemaVersion =
                    backupJson.getInt(
                            "database_schema_version"
                    );

            if (!BackupDatabaseTablePolicy
                    .isSupportedSchemaVersion(
                            backupSchemaVersion
                    )) {

                throw new CloudBackupDownloadException(
                        "Downloaded JSON database "
                                + "version is unsupported."
                );
            }

            if (!applicationPackageName.equals(
                    backupJson.getString(
                            "package_name"
                    )
            )) {
                throw new CloudBackupDownloadException(
                        "Downloaded JSON belongs to "
                                + "another application."
                );
            }

            if (!BACKUP_TARGET.equals(
                    backupJson.optString(
                            "backup_target",
                            ""
                    )
            )) {
                throw new CloudBackupDownloadException(
                        "Downloaded JSON is not a "
                                + "Study Saathi cloud backup."
                );
            }

            long jsonCreatedAt =
                    backupJson.getLong(
                            "created_at"
                    );

            if (jsonCreatedAt
                    != metadata.backupCreatedAt) {

                throw new CloudBackupDownloadException(
                        "Downloaded JSON creation time "
                                + "does not match metadata."
                );
            }

            JSONObject databaseObject =
                    backupJson.getJSONObject(
                            "database"
                    );

            int databaseSchemaVersion =
                    databaseObject.getInt(
                            "schema_version"
                    );

            if (databaseSchemaVersion
                    != backupSchemaVersion) {

                throw new CloudBackupDownloadException(
                        "Downloaded database schema "
                                + "is invalid."
                );
            }

            JSONArray tableArray =
                    databaseObject.getJSONArray(
                            "tables"
                    );

            validateRequiredTables(
                    databaseSchemaVersion,
                    tableArray
            );

            JSONArray preferencesArray =
                    backupJson.getJSONArray(
                            "shared_preferences"
                    );

            validatePreferencesStructure(
                    preferencesArray
            );

        } catch (JSONException exception) {
            throw new CloudBackupDownloadException(
                    "Downloaded cloud backup structure "
                            + "is invalid.",
                    exception
            );
        }
    }

    private void validateRequiredTables(
            int schemaVersion,
            @NonNull JSONArray tableArray
    ) throws JSONException,
            CloudBackupDownloadException {

        Set<String> foundTableNames =
                new HashSet<>();

        for (int tableIndex = 0;
             tableIndex < tableArray.length();
             tableIndex++) {

            JSONObject tableObject =
                    tableArray.getJSONObject(
                            tableIndex
                    );

            String tableName =
                    tableObject.getString(
                            "table_name"
                    ).trim();

            if (!BackupDatabaseTablePolicy
                    .isSupportedTable(
                            schemaVersion,
                            tableName
                    )) {

                throw new CloudBackupDownloadException(
                        "Downloaded backup contains "
                                + "unsupported table "
                                + tableName
                                + "."
                );
            }

            if (!foundTableNames.add(
                    tableName
            )) {
                throw new CloudBackupDownloadException(
                        "Downloaded backup contains "
                                + "duplicate table "
                                + tableName
                                + "."
                );
            }

            JSONArray rows =
                    tableObject.getJSONArray(
                            "rows"
                    );

            int declaredRowCount =
                    tableObject.getInt(
                            "row_count"
                    );

            if (declaredRowCount
                    != rows.length()) {

                throw new CloudBackupDownloadException(
                        "Downloaded backup table "
                                + tableName
                                + " has an invalid row count."
                );
            }
        }

        for (String requiredTable
                : BackupDatabaseTablePolicy
                .getRequiredTables(schemaVersion)) {

            if (!foundTableNames.contains(
                    requiredTable
            )) {
                throw new CloudBackupDownloadException(
                        "Downloaded backup is missing "
                                + "required table "
                                + requiredTable
                                + "."
                );
            }
        }
    }

    private void validatePreferencesStructure(
            @NonNull JSONArray preferencesArray
    ) throws JSONException,
            CloudBackupDownloadException {

        Set<String> foundPreferenceNames =
                new HashSet<>();

        for (int preferenceIndex = 0;
             preferenceIndex
                     < preferencesArray.length();
             preferenceIndex++) {

            JSONObject preferenceObject =
                    preferencesArray.getJSONObject(
                            preferenceIndex
                    );

            String preferenceName =
                    preferenceObject.getString(
                            "preference_name"
                    );

            if (!preferenceName.startsWith(
                    "study_saathi_"
            )) {
                throw new CloudBackupDownloadException(
                        "Downloaded backup contains "
                                + "an unsupported preference file."
                );
            }

            if (!foundPreferenceNames.add(
                    preferenceName
            )) {
                throw new CloudBackupDownloadException(
                        "Downloaded backup contains "
                                + "duplicate preference data."
                );
            }

            JSONObject entries =
                    preferenceObject.getJSONObject(
                            "entries"
                    );

            int declaredEntryCount =
                    preferenceObject.getInt(
                            "entry_count"
                    );

            if (declaredEntryCount
                    != entries.length()) {

                throw new CloudBackupDownloadException(
                        "Downloaded preference entry "
                                + "count is invalid."
                );
            }
        }
    }

    @NonNull
    private byte[] decompressGzip(
            @NonNull byte[] compressedPayload
    ) throws IOException {

        try (ByteArrayInputStream inputStream =
                     new ByteArrayInputStream(
                             compressedPayload
                     );

             GZIPInputStream gzipInputStream =
                     new GZIPInputStream(
                             inputStream
                     );

             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;
            long totalBytes = 0L;

            while ((bytesRead =
                    gzipInputStream.read(buffer)) != -1) {

                totalBytes += bytesRead;

                if (totalBytes
                        > MAX_RAW_JSON_BYTES) {

                    throw new IOException(
                            "Decompressed cloud backup "
                                    + "exceeds the supported "
                                    + "25 MB limit."
                    );
                }

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return outputStream.toByteArray();
        }
    }

    @NonNull
    private String createSha256Checksum(
            @NonNull byte[] sourceBytes
    ) throws IOException {

        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] checksumBytes =
                    messageDigest.digest(
                            sourceBytes
                    );

            StringBuilder checksumBuilder =
                    new StringBuilder(
                            checksumBytes.length * 2
                    );

            for (byte checksumByte
                    : checksumBytes) {

                checksumBuilder.append(
                        String.format(
                                Locale.US,
                                "%02x",
                                checksumByte & 0xff
                        )
                );
            }

            return checksumBuilder.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IOException(
                    "SHA-256 is unavailable.",
                    exception
            );
        }
    }

    private boolean isValidSha256Checksum(
            @NonNull String checksum
    ) {
        return checksum.matches(
                "[0-9a-fA-F]{64}"
        );
    }

    @NonNull
    private String getRequiredString(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) throws CloudBackupDownloadException {

        String value =
                snapshot.getString(
                        fieldName
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new CloudBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " is missing."
            );
        }

        return value;
    }

    private long getRequiredLong(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) throws CloudBackupDownloadException {

        Long value =
                snapshot.getLong(
                        fieldName
                );

        if (value == null) {
            throw new CloudBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " is missing."
            );
        }

        return value;
    }

    private int getRequiredInt(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) throws CloudBackupDownloadException {

        Long value =
                snapshot.getLong(
                        fieldName
                );

        if (value == null) {
            throw new CloudBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " is missing."
            );
        }

        if (value > Integer.MAX_VALUE
                || value < Integer.MIN_VALUE) {

            throw new CloudBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " exceeds supported limits."
            );
        }

        return value.intValue();
    }

    private long getOptionalLong(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) {
        Long value =
                snapshot.getLong(
                        fieldName
                );

        return value == null
                ? 0L
                : value;
    }

    private int getOptionalInt(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) throws CloudBackupDownloadException {

        Long value =
                snapshot.getLong(
                        fieldName
                );

        if (value == null) {
            return 0;
        }

        if (value > Integer.MAX_VALUE
                || value < Integer.MIN_VALUE) {

            throw new CloudBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " exceeds supported limits."
            );
        }

        return value.intValue();
    }

    @NonNull
    private DocumentReference getLatestBackupReference(
            @NonNull String userId
    ) {
        return firestore
                .collection(
                        COLLECTION_USERS
                )
                .document(
                        userId
                )
                .collection(
                        COLLECTION_CLOUD_BACKUPS
                )
                .document(
                        LATEST_BACKUP_DOCUMENT
                );
    }

    private void dispatchSuccess(
            @NonNull DownloadCallback callback,
            @NonNull CloudBackupDownloadResult result
    ) {
        mainThreadHandler.post(() ->
                callback.onSuccess(
                        result
                )
        );
    }

    private void dispatchError(
            @NonNull DownloadCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(() ->
                callback.onError(
                        exception
                )
        );
    }

    public interface DownloadCallback {

        void onSuccess(
                @NonNull CloudBackupDownloadResult result
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class
    CloudBackupDownloadResult {

        private final JSONObject backupJson;

        private final String backupId;

        private final long backupCreatedAt;

        private final long uploadedAt;

        private final String checksumSha256;

        private final int chunkCount;

        private final int rawJsonBytes;

        private final int compressedBytes;

        private final int profileCount;

        private final int lessonProgressCount;

        private final int quizAttemptCount;

        private final int doubtCount;

        private final int preferenceItemCount;

        private CloudBackupDownloadResult(
                @NonNull JSONObject backupJson,
                @NonNull String backupId,
                long backupCreatedAt,
                long uploadedAt,
                @NonNull String checksumSha256,
                int chunkCount,
                int rawJsonBytes,
                int compressedBytes,
                int profileCount,
                int lessonProgressCount,
                int quizAttemptCount,
                int doubtCount,
                int preferenceItemCount
        ) {
            this.backupJson =
                    backupJson;

            this.backupId =
                    backupId;

            this.backupCreatedAt =
                    backupCreatedAt;

            this.uploadedAt =
                    uploadedAt;

            this.checksumSha256 =
                    checksumSha256;

            this.chunkCount =
                    chunkCount;

            this.rawJsonBytes =
                    rawJsonBytes;

            this.compressedBytes =
                    compressedBytes;

            this.profileCount =
                    profileCount;

            this.lessonProgressCount =
                    lessonProgressCount;

            this.quizAttemptCount =
                    quizAttemptCount;

            this.doubtCount =
                    doubtCount;

            this.preferenceItemCount =
                    preferenceItemCount;
        }

        @NonNull
        public JSONObject getBackupJson() {
            return backupJson;
        }

        @NonNull
        public String getBackupId() {
            return backupId;
        }

        public long getBackupCreatedAt() {
            return backupCreatedAt;
        }

        public long getUploadedAt() {
            return uploadedAt;
        }

        @NonNull
        public String getChecksumSha256() {
            return checksumSha256;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public int getRawJsonBytes() {
            return rawJsonBytes;
        }

        public int getCompressedBytes() {
            return compressedBytes;
        }

        public int getProfileCount() {
            return profileCount;
        }

        public int getLessonProgressCount() {
            return lessonProgressCount;
        }

        public int getQuizAttemptCount() {
            return quizAttemptCount;
        }

        public int getDoubtCount() {
            return doubtCount;
        }

        public int getPreferenceItemCount() {
            return preferenceItemCount;
        }
    }

    public static class
    CloudBackupDownloadException
            extends Exception {

        public CloudBackupDownloadException(
                @NonNull String message
        ) {
            super(message);
        }

        public CloudBackupDownloadException(
                @NonNull String message,
                @NonNull Throwable cause
        ) {
            super(
                    message,
                    cause
            );
        }
    }

    public static final class
    CloudBackupNotFoundException
            extends CloudBackupDownloadException {

        public CloudBackupNotFoundException(
                @NonNull String message
        ) {
            super(message);
        }
    }

    private static final class BackupMetadata {

        private final String ownerUserId;

        private final String backupId;

        private final String status;

        private final String backupFormat;

        private final int backupFormatVersion;

        private final int databaseSchemaVersion;

        private final String compression;

        private final String encoding;

        private final String checksumSha256;

        private final String appPackage;

        private final long backupCreatedAt;

        private final long uploadedAt;

        private final int chunkCount;

        private final int rawJsonBytes;

        private final int compressedBytes;

        private final int encodedCharacters;

        private final int profileCount;

        private final int lessonProgressCount;

        private final int quizAttemptCount;

        private final int doubtCount;

        private final int preferenceItemCount;

        private BackupMetadata(
                @NonNull String ownerUserId,
                @NonNull String backupId,
                @NonNull String status,
                @NonNull String backupFormat,
                int backupFormatVersion,
                int databaseSchemaVersion,
                @NonNull String compression,
                @NonNull String encoding,
                @NonNull String checksumSha256,
                @NonNull String appPackage,
                long backupCreatedAt,
                long uploadedAt,
                int chunkCount,
                int rawJsonBytes,
                int compressedBytes,
                int encodedCharacters,
                int profileCount,
                int lessonProgressCount,
                int quizAttemptCount,
                int doubtCount,
                int preferenceItemCount
        ) {
            this.ownerUserId =
                    ownerUserId;

            this.backupId =
                    backupId;

            this.status =
                    status;

            this.backupFormat =
                    backupFormat;

            this.backupFormatVersion =
                    backupFormatVersion;

            this.databaseSchemaVersion =
                    databaseSchemaVersion;

            this.compression =
                    compression;

            this.encoding =
                    encoding;

            this.checksumSha256 =
                    checksumSha256;

            this.appPackage =
                    appPackage;

            this.backupCreatedAt =
                    backupCreatedAt;

            this.uploadedAt =
                    uploadedAt;

            this.chunkCount =
                    chunkCount;

            this.rawJsonBytes =
                    rawJsonBytes;

            this.compressedBytes =
                    compressedBytes;

            this.encodedCharacters =
                    encodedCharacters;

            this.profileCount =
                    profileCount;

            this.lessonProgressCount =
                    lessonProgressCount;

            this.quizAttemptCount =
                    quizAttemptCount;

            this.doubtCount =
                    doubtCount;

            this.preferenceItemCount =
                    preferenceItemCount;
        }
    }
}
