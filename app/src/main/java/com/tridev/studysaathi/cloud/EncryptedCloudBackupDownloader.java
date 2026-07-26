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
import com.tridev.studysaathi.backup.BackupDatabaseTablePolicy;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

public final class EncryptedCloudBackupDownloader {

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

    private static final String PREFERENCE_PREFIX =
            "study_saathi_";

    private static final String BACKUP_STATE_PREFERENCES =
            "study_saathi_backup_state";

    private static final String CLOUD_STATE_PREFERENCES =
            "study_saathi_cloud_state";

    private static final int MAX_RAW_JSON_BYTES =
            25 * 1024 * 1024;

    private static final int MAX_ENCODED_CHARACTERS =
            CloudBackupPayloadBuilder.MAX_CHUNK_COUNT
                    * CloudBackupPayloadBuilder
                    .CHUNK_CHARACTER_LIMIT;

    private static final String[] REQUIRED_TABLES = {
            "student_profiles",
            "lesson_progress",
            "quiz_attempts",
            "doubt_history"
    };

    private static final ExecutorService
            decryptionExecutor =
            Executors.newSingleThreadExecutor();

    private final FirebaseFirestore firestore;

    private final Handler mainThreadHandler;

    private final String applicationPackageName;

    public EncryptedCloudBackupDownloader(
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
     * Downloads and decrypts the latest encrypted cloud
     * backup.
     *
     * The supplied passphrase is copied immediately.
     * The internal copy is erased when the operation
     * finishes.
     *
     * The caller should also erase its original char[].
     */
    public void downloadLatestEncryptedBackup(
            @NonNull FirebaseUser firebaseUser,
            @NonNull char[] passphrase,
            @NonNull DownloadCallback callback
    ) {
        char[] passphraseCopy =
                Arrays.copyOf(
                        passphrase,
                        passphrase.length
                );

        DownloadSession session =
                new DownloadSession(
                        firebaseUser.getUid(),
                        passphraseCopy,
                        callback
                );

        if (!firebaseUser.isEmailVerified()) {
            session.fail(
                    new EncryptedBackupDownloadException(
                            "Encrypted cloud restore requires "
                                    + "a verified email account."
                    )
            );

            return;
        }

        if (firebaseUser.getUid()
                .trim()
                .isEmpty()) {

            session.fail(
                    new EncryptedBackupDownloadException(
                            "Firebase user ID is unavailable."
                    )
            );

            return;
        }

        DocumentReference latestBackupReference =
                getLatestBackupReference(
                        firebaseUser.getUid()
                );

        latestBackupReference
                .get(Source.SERVER)
                .addOnSuccessListener(
                        metadataSnapshot -> {
                            if (!metadataSnapshot.exists()) {
                                session.fail(
                                        new EncryptedBackupNotFoundException(
                                                "No cloud backup was "
                                                        + "found for this "
                                                        + "account."
                                        )
                                );

                                return;
                            }

                            EncryptedBackupMetadata metadata;

                            try {
                                metadata =
                                        readAndValidateMetadata(
                                                metadataSnapshot,
                                                session.expectedUserId
                                        );

                            } catch (Exception exception) {
                                session.fail(
                                        exception
                                );

                                return;
                            }

                            downloadEncryptedChunks(
                                    latestBackupReference,
                                    metadata,
                                    session
                            );
                        }
                )
                .addOnFailureListener(
                        session::fail
                );
    }

    private void downloadEncryptedChunks(
            @NonNull DocumentReference latestBackupReference,
            @NonNull EncryptedBackupMetadata metadata,
            @NonNull DownloadSession session
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
                        querySnapshot ->
                                decryptAndValidateAsync(
                                        metadata,
                                        querySnapshot,
                                        session
                                )
                )
                .addOnFailureListener(
                        session::fail
                );
    }

    private void decryptAndValidateAsync(
            @NonNull EncryptedBackupMetadata metadata,
            @NonNull QuerySnapshot chunkSnapshot,
            @NonNull DownloadSession session
    ) {
        decryptionExecutor.execute(() -> {
            byte[] encryptedBytes = null;
            byte[] compressedPlaintextBytes = null;
            byte[] rawJsonBytes = null;

            try {
                if (session.isCompleted()) {
                    return;
                }

                String encodedEncryptedPayload =
                        assembleEncryptedPayload(
                                session.expectedUserId,
                                metadata,
                                chunkSnapshot
                        );

                try {
                    encryptedBytes =
                            Base64.decode(
                                    encodedEncryptedPayload,
                                    Base64.NO_WRAP
                            );

                } catch (IllegalArgumentException exception) {
                    throw new EncryptedBackupDownloadException(
                            "Encrypted cloud payload contains "
                                    + "invalid Base64 data.",
                            exception
                    );
                }

                if (encryptedBytes.length
                        != metadata.encryptedBytes) {

                    throw new EncryptedBackupDownloadException(
                            "Encrypted payload size does not "
                                    + "match its metadata."
                    );
                }

                String actualEncryptedChecksum =
                        createSha256Checksum(
                                encryptedBytes
                        );

                if (!metadata.encryptedChecksumSha256
                        .equalsIgnoreCase(
                                actualEncryptedChecksum
                        )) {

                    throw new EncryptedBackupDownloadException(
                            "Encrypted cloud backup checksum "
                                    + "validation failed."
                    );
                }

                String associatedData =
                        CloudBackupEncryption
                                .createAssociatedData(
                                        session.expectedUserId,
                                        metadata.backupId
                                );

                CloudBackupEncryption.EncryptedPayload
                        encryptedPayload =
                        CloudBackupEncryption.fromBase64(
                                metadata.encryptionVersion,
                                metadata.cipherTransformation,
                                metadata.kdfAlgorithm,
                                metadata.kdfIterations,
                                metadata.keyLengthBits,
                                metadata.gcmTagLengthBits,
                                metadata.saltBase64,
                                metadata.initializationVectorBase64,
                                encodedEncryptedPayload,
                                metadata.compressedPlaintextBytes
                        );

                compressedPlaintextBytes =
                        CloudBackupEncryption.decrypt(
                                encryptedPayload,
                                session.passphraseCopy,
                                associatedData
                        );

                if (compressedPlaintextBytes.length
                        != metadata
                        .compressedPlaintextBytes) {

                    throw new EncryptedBackupDownloadException(
                            "Decrypted compressed payload size "
                                    + "does not match metadata."
                    );
                }

                String actualPlaintextChecksum =
                        createSha256Checksum(
                                compressedPlaintextBytes
                        );

                if (!metadata.plaintextChecksumSha256
                        .equalsIgnoreCase(
                                actualPlaintextChecksum
                        )) {

                    throw new EncryptedBackupDownloadException(
                            "Decrypted cloud backup checksum "
                                    + "validation failed."
                    );
                }

                rawJsonBytes =
                        decompressGzip(
                                compressedPlaintextBytes
                        );

                if (rawJsonBytes.length
                        != metadata.rawJsonBytes) {

                    throw new EncryptedBackupDownloadException(
                            "Decompressed JSON size does not "
                                    + "match cloud metadata."
                    );
                }

                String backupJsonText =
                        new String(
                                rawJsonBytes,
                                StandardCharsets.UTF_8
                        );

                JSONObject backupJson;

                try {
                    backupJson =
                            new JSONObject(
                                    backupJsonText
                            );

                } catch (JSONException exception) {
                    throw new EncryptedBackupDownloadException(
                            "Decrypted cloud backup JSON "
                                    + "is invalid.",
                            exception
                    );
                }

                BackupContentCounts contentCounts =
                        validateBackupJson(
                                backupJson,
                                metadata
                        );

                EncryptedBackupDownloadResult result =
                        new EncryptedBackupDownloadResult(
                                backupJson,
                                metadata.backupId,
                                metadata.backupCreatedAt,
                                metadata.uploadedAt,
                                metadata.encryptedChecksumSha256,
                                metadata.plaintextChecksumSha256,
                                metadata.chunkCount,
                                metadata.rawJsonBytes,
                                metadata.compressedPlaintextBytes,
                                metadata.encryptedBytes,
                                contentCounts.profileCount,
                                contentCounts.lessonProgressCount,
                                contentCounts.quizAttemptCount,
                                contentCounts.doubtCount,
                                contentCounts.preferenceItemCount
                        );

                session.succeed(
                        result
                );

            } catch (CloudBackupEncryption
                             .InvalidPassphraseException exception) {

                session.fail(
                        new InvalidCloudBackupPassphraseException(
                                "Cloud backup could not be "
                                        + "unlocked. The passphrase "
                                        + "is incorrect or the backup "
                                        + "has been modified.",
                                exception
                        )
                );

            } catch (Exception exception) {
                session.fail(
                        exception
                );

            } finally {
                clearByteArray(
                        encryptedBytes
                );

                clearByteArray(
                        compressedPlaintextBytes
                );

                clearByteArray(
                        rawJsonBytes
                );
            }
        });
    }

    @NonNull
    private EncryptedBackupMetadata
    readAndValidateMetadata(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String expectedUserId
    ) throws EncryptedBackupDownloadException {

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

        String payloadFormat =
                getRequiredString(
                        snapshot,
                        "payload_format"
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

        String associatedDataScheme =
                getRequiredString(
                        snapshot,
                        "associated_data_scheme"
                );

        String cipherTransformation =
                getRequiredString(
                        snapshot,
                        "cipher_transformation"
                );

        String kdfAlgorithm =
                getRequiredString(
                        snapshot,
                        "kdf_algorithm"
                );

        String saltBase64 =
                getRequiredString(
                        snapshot,
                        "salt_base64"
                );

        String initializationVectorBase64 =
                getRequiredString(
                        snapshot,
                        "initialization_vector_base64"
                );

        String plaintextChecksumSha256 =
                getRequiredString(
                        snapshot,
                        "plaintext_checksum_sha256"
                );

        String encryptedChecksumSha256 =
                getRequiredString(
                        snapshot,
                        "encrypted_checksum_sha256"
                );

        String appPackage =
                getRequiredString(
                        snapshot,
                        "app_package"
                );

        boolean clientSideEncrypted =
                getRequiredBoolean(
                        snapshot,
                        "is_client_side_encrypted"
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

        int payloadFormatVersion =
                getRequiredInt(
                        snapshot,
                        "payload_format_version"
                );

        int encryptionVersion =
                getRequiredInt(
                        snapshot,
                        "encryption_version"
                );

        int kdfIterations =
                getRequiredInt(
                        snapshot,
                        "kdf_iterations"
                );

        int keyLengthBits =
                getRequiredInt(
                        snapshot,
                        "key_length_bits"
                );

        int gcmTagLengthBits =
                getRequiredInt(
                        snapshot,
                        "gcm_tag_length_bits"
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

        int compressedPlaintextBytes =
                getRequiredInt(
                        snapshot,
                        "compressed_plaintext_bytes"
                );

        int encryptedBytes =
                getRequiredInt(
                        snapshot,
                        "encrypted_bytes"
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
            Timestamp serverTimestamp =
                    snapshot.getTimestamp(
                            "uploaded_at"
                    );

            if (serverTimestamp != null) {
                uploadedAt =
                        serverTimestamp.toDate()
                                .getTime();
            }
        }

        int profileCount =
                getRequiredNonNegativeInt(
                        snapshot,
                        "profile_count"
                );

        int lessonProgressCount =
                getRequiredNonNegativeInt(
                        snapshot,
                        "lesson_progress_count"
                );

        int quizAttemptCount =
                getRequiredNonNegativeInt(
                        snapshot,
                        "quiz_attempt_count"
                );

        int doubtCount =
                getRequiredNonNegativeInt(
                        snapshot,
                        "doubt_count"
                );

        int preferenceItemCount =
                getRequiredNonNegativeInt(
                        snapshot,
                        "preference_item_count"
                );

        if (!expectedUserId.equals(
                ownerUserId
        )) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup owner does not "
                            + "match the signed-in account."
            );
        }

        if (!STATUS_COMPLETE.equals(
                status
        )) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup is not marked "
                            + "as complete."
            );
        }

        if (!clientSideEncrypted) {
            throw new EncryptedBackupDownloadException(
                    "The selected cloud backup is "
                            + "not client-side encrypted."
            );
        }

        if (!CloudBackupPayloadBuilder
                .BACKUP_FORMAT.equals(
                        backupFormat
                )) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported cloud backup format."
            );
        }

        if (backupFormatVersion
                != CloudBackupPayloadBuilder
                .BACKUP_FORMAT_VERSION) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported cloud backup "
                            + "format version."
            );
        }

        if (!BackupDatabaseTablePolicy.isSupportedSchemaVersion(
                databaseSchemaVersion
        )) {

            throw new EncryptedBackupDownloadException(
                    "Cloud backup database version "
                            + databaseSchemaVersion
                            + " is incompatible with "
                            + CloudBackupPayloadBuilder
                            .DATABASE_SCHEMA_VERSION
                            + "."
            );
        }

        if (!EncryptedCloudBackupPayloadBuilder
                .ENCRYPTED_PAYLOAD_FORMAT
                .equals(payloadFormat)) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported encrypted payload format."
            );
        }

        if (payloadFormatVersion
                != EncryptedCloudBackupPayloadBuilder
                .ENCRYPTED_PAYLOAD_FORMAT_VERSION) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported encrypted payload "
                            + "format version."
            );
        }

        if (!CloudBackupPayloadBuilder
                .COMPRESSION_TYPE.equals(
                        compression
                )) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported cloud backup "
                            + "compression type."
            );
        }

        if (!CloudBackupPayloadBuilder
                .ENCODING_TYPE.equals(
                        encoding
                )) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported cloud backup "
                            + "encoding type."
            );
        }

        if (!EncryptedCloudBackupPayloadBuilder
                .ASSOCIATED_DATA_SCHEME
                .equals(associatedDataScheme)) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported associated-data scheme."
            );
        }

        if (encryptionVersion
                != CloudBackupEncryption
                .ENCRYPTION_VERSION) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported encryption version."
            );
        }

        if (!CloudBackupEncryption
                .CIPHER_TRANSFORMATION
                .equals(cipherTransformation)) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported cloud backup cipher."
            );
        }

        if (!CloudBackupEncryption
                .KDF_ALGORITHM.equals(
                        kdfAlgorithm
                )) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported key derivation algorithm."
            );
        }

        if (kdfIterations
                != CloudBackupEncryption
                .KDF_ITERATIONS) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported key derivation settings."
            );
        }

        if (keyLengthBits
                != CloudBackupEncryption
                .KEY_LENGTH_BITS) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported encryption key size."
            );
        }

        if (gcmTagLengthBits
                != CloudBackupEncryption
                .GCM_TAG_LENGTH_BITS) {

            throw new EncryptedBackupDownloadException(
                    "Unsupported authentication tag size."
            );
        }

        if (!applicationPackageName.equals(
                appPackage
        )) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup belongs to a "
                            + "different application."
            );
        }

        validateSha256Checksum(
                plaintextChecksumSha256,
                "plaintext checksum"
        );

        validateSha256Checksum(
                encryptedChecksumSha256,
                "encrypted checksum"
        );

        validateEncryptionBase64(
                saltBase64,
                CloudBackupEncryption
                        .SALT_LENGTH_BYTES,
                "encryption salt"
        );

        validateEncryptionBase64(
                initializationVectorBase64,
                CloudBackupEncryption
                        .IV_LENGTH_BYTES,
                "initialization vector"
        );

        if (chunkCount <= 0
                || chunkCount
                > CloudBackupPayloadBuilder
                .MAX_CHUNK_COUNT) {

            throw new EncryptedBackupDownloadException(
                    "Cloud backup chunk count is invalid."
            );
        }

        if (rawJsonBytes <= 0
                || rawJsonBytes
                > MAX_RAW_JSON_BYTES) {

            throw new EncryptedBackupDownloadException(
                    "Cloud backup JSON size is invalid."
            );
        }

        if (compressedPlaintextBytes <= 0
                || compressedPlaintextBytes
                > MAX_RAW_JSON_BYTES) {

            throw new EncryptedBackupDownloadException(
                    "Compressed plaintext size "
                            + "is invalid."
            );
        }

        if (compressedBytes
                != compressedPlaintextBytes) {

            throw new EncryptedBackupDownloadException(
                    "Compressed payload metadata "
                            + "is inconsistent."
            );
        }

        if (encryptedBytes <= 0
                || encryptedBytes
                > MAX_RAW_JSON_BYTES + 1024) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted payload size is invalid."
            );
        }

        if (encodedCharacters <= 0
                || encodedCharacters
                > MAX_ENCODED_CHARACTERS) {

            throw new EncryptedBackupDownloadException(
                    "Encoded encrypted payload "
                            + "size is invalid."
            );
        }

        if (backupCreatedAt <= 0L) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup creation time "
                            + "is invalid."
            );
        }

        return new EncryptedBackupMetadata(
                ownerUserId,
                backupId,
                backupCreatedAt,
                uploadedAt,
                chunkCount,
                rawJsonBytes,
                compressedPlaintextBytes,
                encryptedBytes,
                encodedCharacters,
                encryptionVersion,
                cipherTransformation,
                kdfAlgorithm,
                kdfIterations,
                keyLengthBits,
                gcmTagLengthBits,
                saltBase64,
                initializationVectorBase64,
                plaintextChecksumSha256,
                encryptedChecksumSha256,
                profileCount,
                lessonProgressCount,
                quizAttemptCount,
                doubtCount,
                preferenceItemCount
        );
    }

    @NonNull
    private String assembleEncryptedPayload(
            @NonNull String expectedUserId,
            @NonNull EncryptedBackupMetadata metadata,
            @NonNull QuerySnapshot querySnapshot
    ) throws EncryptedBackupDownloadException {

        List<DocumentSnapshot> chunkDocuments =
                querySnapshot.getDocuments();

        if (chunkDocuments.size()
                != metadata.chunkCount) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted cloud backup contains "
                            + chunkDocuments.size()
                            + " chunks, but "
                            + metadata.chunkCount
                            + " were expected."
            );
        }

        StringBuilder payloadBuilder =
                new StringBuilder(
                        metadata.encodedCharacters
                );

        for (int expectedIndex = 0;
             expectedIndex < chunkDocuments.size();
             expectedIndex++) {

            DocumentSnapshot chunkDocument =
                    chunkDocuments.get(
                            expectedIndex
                    );

            validateAndAppendChunk(
                    expectedUserId,
                    metadata,
                    expectedIndex,
                    chunkDocument,
                    payloadBuilder
            );
        }

        if (payloadBuilder.length()
                != metadata.encodedCharacters) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted payload length does not "
                            + "match metadata."
            );
        }

        return payloadBuilder.toString();
    }

    private void validateAndAppendChunk(
            @NonNull String expectedUserId,
            @NonNull EncryptedBackupMetadata metadata,
            int expectedChunkIndex,
            @NonNull DocumentSnapshot chunkDocument,
            @NonNull StringBuilder payloadBuilder
    ) throws EncryptedBackupDownloadException {

        String expectedDocumentId =
                String.format(
                        Locale.US,
                        "chunk_%03d",
                        expectedChunkIndex
                );

        if (!expectedDocumentId.equals(
                chunkDocument.getId()
        )) {
            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk "
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

        String payloadFormat =
                getRequiredString(
                        chunkDocument,
                        "payload_format"
                );

        String encoding =
                getRequiredString(
                        chunkDocument,
                        "encoding"
                );

        String compression =
                getRequiredString(
                        chunkDocument,
                        "compression"
                );

        String encryptedChecksum =
                getRequiredString(
                        chunkDocument,
                        "encrypted_checksum_sha256"
                );

        String payload =
                getRequiredString(
                        chunkDocument,
                        "payload"
                );

        boolean encrypted =
                getRequiredBoolean(
                        chunkDocument,
                        "is_client_side_encrypted"
                );

        int payloadFormatVersion =
                getRequiredInt(
                        chunkDocument,
                        "payload_format_version"
                );

        int encryptionVersion =
                getRequiredInt(
                        chunkDocument,
                        "encryption_version"
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
            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk owner does not "
                            + "match the account."
            );
        }

        if (!metadata.backupId.equals(
                backupId
        )) {
            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk belongs to "
                            + "a different backup."
            );
        }

        if (!encrypted) {
            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk is not marked "
                            + "as encrypted."
            );
        }

        if (!EncryptedCloudBackupPayloadBuilder
                .ENCRYPTED_PAYLOAD_FORMAT
                .equals(payloadFormat)) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk payload format "
                            + "is invalid."
            );
        }

        if (payloadFormatVersion
                != EncryptedCloudBackupPayloadBuilder
                .ENCRYPTED_PAYLOAD_FORMAT_VERSION) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk payload version "
                            + "is invalid."
            );
        }

        if (encryptionVersion
                != CloudBackupEncryption
                .ENCRYPTION_VERSION) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk encryption version "
                            + "is invalid."
            );
        }

        if (!CloudBackupPayloadBuilder
                .ENCODING_TYPE.equals(
                        encoding
                )) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk encoding is invalid."
            );
        }

        if (!CloudBackupPayloadBuilder
                .COMPRESSION_TYPE.equals(
                        compression
                )) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk compression "
                            + "metadata is invalid."
            );
        }

        if (!metadata.encryptedChecksumSha256
                .equalsIgnoreCase(
                        encryptedChecksum
                )) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk checksum does not "
                            + "match metadata."
            );
        }

        if (chunkIndex != expectedChunkIndex) {
            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk sequence is invalid."
            );
        }

        if (chunkCount != metadata.chunkCount) {
            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk count does not "
                            + "match metadata."
            );
        }

        if (backupCreatedAt
                != metadata.backupCreatedAt) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk creation time "
                            + "does not match metadata."
            );
        }

        if (payload.isEmpty()
                || payload.length()
                > CloudBackupPayloadBuilder
                .CHUNK_CHARACTER_LIMIT) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted chunk size is invalid."
            );
        }

        if (payloadBuilder.length()
                + payload.length()
                > MAX_ENCODED_CHARACTERS) {

            throw new EncryptedBackupDownloadException(
                    "Encrypted payload exceeds the "
                            + "supported size."
            );
        }

        payloadBuilder.append(
                payload
        );
    }

    @NonNull
    private BackupContentCounts validateBackupJson(
            @NonNull JSONObject backupJson,
            @NonNull EncryptedBackupMetadata metadata
    ) throws EncryptedBackupDownloadException {

        try {
            if (!CloudBackupPayloadBuilder
                    .BACKUP_FORMAT.equals(
                            backupJson.getString(
                                    "backup_format"
                            )
                    )) {

                throw new EncryptedBackupDownloadException(
                        "Decrypted backup format is invalid."
                );
            }

            if (backupJson.getInt(
                    "backup_format_version"
            ) != CloudBackupPayloadBuilder
                    .BACKUP_FORMAT_VERSION) {

                throw new EncryptedBackupDownloadException(
                        "Decrypted backup format version "
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

                throw new EncryptedBackupDownloadException(
                        "Decrypted backup database "
                                + "version is unsupported."
                );
            }

            if (!applicationPackageName.equals(
                    backupJson.getString(
                            "package_name"
                    )
            )) {
                throw new EncryptedBackupDownloadException(
                        "Decrypted backup belongs to "
                                + "another application."
                );
            }

            if (!BACKUP_TARGET.equals(
                    backupJson.optString(
                            "backup_target",
                            ""
                    )
            )) {
                throw new EncryptedBackupDownloadException(
                        "Decrypted data is not a "
                                + "Study Saathi cloud backup."
                );
            }

            if (backupJson.getLong(
                    "created_at"
            ) != metadata.backupCreatedAt) {

                throw new EncryptedBackupDownloadException(
                        "Decrypted backup creation time "
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

                throw new EncryptedBackupDownloadException(
                        "Decrypted database schema "
                                + "is invalid."
                );
            }

            JSONArray tableArray =
                    databaseObject.getJSONArray(
                            "tables"
                    );

            Map<String, JSONArray> tableRows;

            try {
                tableRows =
                        EncryptedBackupDatabaseValidator.validate(
                                databaseSchemaVersion,
                                tableArray
                        );

            } catch (EncryptedBackupDatabaseValidator
                             .ValidationException exception) {

                throw new EncryptedBackupDownloadException(
                        exception.getMessage(),
                        exception
                );
            }

            JSONArray preferencesArray =
                    backupJson.getJSONArray(
                            "shared_preferences"
                    );

            int preferenceItemCount =
                    validatePreferences(
                            preferencesArray
                    );

            int profileCount =
                    tableRows.get(
                            "student_profiles"
                    ).length();

            int lessonProgressCount =
                    tableRows.get(
                            "lesson_progress"
                    ).length();

            int quizAttemptCount =
                    tableRows.get(
                            "quiz_attempts"
                    ).length();

            int doubtCount =
                    tableRows.get(
                            "doubt_history"
                    ).length();

            if (profileCount
                    != metadata.profileCount
                    || lessonProgressCount
                    != metadata.lessonProgressCount
                    || quizAttemptCount
                    != metadata.quizAttemptCount
                    || doubtCount
                    != metadata.doubtCount
                    || preferenceItemCount
                    != metadata.preferenceItemCount) {

                throw new EncryptedBackupDownloadException(
                        "Decrypted backup item counts do "
                                + "not match cloud metadata."
                );
            }

            validateActiveProfiles(
                    tableRows.get(
                            "student_profiles"
                    )
            );

            return new BackupContentCounts(
                    profileCount,
                    lessonProgressCount,
                    quizAttemptCount,
                    doubtCount,
                    preferenceItemCount
            );

        } catch (JSONException exception) {
            throw new EncryptedBackupDownloadException(
                    "Decrypted cloud backup structure "
                            + "is invalid.",
                    exception
            );
        }
    }

    @NonNull
    private Map<String, JSONArray> validateDatabaseTables(
            @NonNull JSONArray tableArray
    ) throws JSONException,
            EncryptedBackupDownloadException {

        Map<String, JSONArray> tableRows =
                new HashMap<>();

        Set<String> supportedTables =
                new HashSet<>(
                        Arrays.asList(
                                REQUIRED_TABLES
                        )
                );

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
                    );

            if (!supportedTables.contains(
                    tableName
            )) {
                throw new EncryptedBackupDownloadException(
                        "Decrypted backup contains "
                                + "unsupported table "
                                + tableName
                                + "."
                );
            }

            if (tableRows.containsKey(
                    tableName
            )) {
                throw new EncryptedBackupDownloadException(
                        "Decrypted backup contains "
                                + "duplicate table "
                                + tableName
                                + "."
                );
            }

            JSONArray rows =
                    tableObject.getJSONArray(
                            "rows"
                    );

            if (tableObject.getInt(
                    "row_count"
            ) != rows.length()) {

                throw new EncryptedBackupDownloadException(
                        "Table "
                                + tableName
                                + " has an invalid row count."
                );
            }

            validateRequiredColumns(
                    tableName,
                    rows
            );

            tableRows.put(
                    tableName,
                    rows
            );
        }

        for (String requiredTable
                : REQUIRED_TABLES) {

            if (!tableRows.containsKey(
                    requiredTable
            )) {
                throw new EncryptedBackupDownloadException(
                        "Decrypted backup is missing "
                                + requiredTable
                                + "."
                );
            }
        }

        return tableRows;
    }

    private void validateRequiredColumns(
            @NonNull String tableName,
            @NonNull JSONArray rows
    ) throws JSONException,
            EncryptedBackupDownloadException {

        String[] requiredColumns;

        switch (tableName) {
            case "student_profiles":
                requiredColumns =
                        new String[]{
                                "profile_id",
                                "student_name",
                                "education_board",
                                "student_class",
                                "study_medium",
                                "explanation_language",
                                "is_active",
                                "created_at",
                                "updated_at"
                        };
                break;

            case "lesson_progress":
                requiredColumns =
                        new String[]{
                                "progress_key",
                                "profile_id",
                                "education_board",
                                "student_class",
                                "subject_name",
                                "chapter_title",
                                "progress_percent",
                                "is_completed",
                                "last_studied_at",
                                "completed_at",
                                "revision_count",
                                "last_revised_at"
                        };
                break;

            case "quiz_attempts":
                requiredColumns =
                        new String[]{
                                "attempt_id",
                                "profile_id",
                                "education_board",
                                "student_class",
                                "subject_name",
                                "chapter_title",
                                "correct_answers",
                                "total_questions",
                                "percentage",
                                "attempted_at"
                        };
                break;

            case "doubt_history":
                requiredColumns =
                        new String[]{
                                "history_id",
                                "profile_id",
                                "education_board",
                                "student_class",
                                "subject_name",
                                "chapter_title",
                                "question_text",
                                "answer_text",
                                "explanation_language",
                                "created_at"
                        };
                break;

            default:
                throw new EncryptedBackupDownloadException(
                        "Unsupported table "
                                + tableName
                                + "."
                );
        }

        for (int rowIndex = 0;
             rowIndex < rows.length();
             rowIndex++) {

            JSONObject row =
                    rows.getJSONObject(
                            rowIndex
                    );

            for (String requiredColumn
                    : requiredColumns) {

                if (!row.has(requiredColumn)
                        || row.isNull(
                        requiredColumn
                )) {

                    throw new EncryptedBackupDownloadException(
                            "Table "
                                    + tableName
                                    + " is missing column "
                                    + requiredColumn
                                    + "."
                    );
                }
            }
        }
    }

    private int validatePreferences(
            @NonNull JSONArray preferencesArray
    ) throws JSONException,
            EncryptedBackupDownloadException {

        Set<String> preferenceNames =
                new HashSet<>();

        int totalEntryCount = 0;

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
                    PREFERENCE_PREFIX
            )) {
                throw new EncryptedBackupDownloadException(
                        "Decrypted backup contains an "
                                + "unsupported preference file."
                );
            }

            if (BACKUP_STATE_PREFERENCES.equals(
                    preferenceName
            )
                    || CLOUD_STATE_PREFERENCES.equals(
                    preferenceName
            )) {

                throw new EncryptedBackupDownloadException(
                        "Decrypted backup contains "
                                + "internal backup state."
                );
            }

            if (!preferenceNames.add(
                    preferenceName
            )) {
                throw new EncryptedBackupDownloadException(
                        "Decrypted backup contains "
                                + "duplicate preference data."
                );
            }

            JSONObject entries =
                    preferenceObject.getJSONObject(
                            "entries"
                    );

            if (preferenceObject.getInt(
                    "entry_count"
            ) != entries.length()) {

                throw new EncryptedBackupDownloadException(
                        "Preference entry count is invalid."
                );
            }

            JSONArray keys =
                    entries.names();

            if (keys != null) {
                for (int keyIndex = 0;
                     keyIndex < keys.length();
                     keyIndex++) {

                    String key =
                            keys.getString(
                                    keyIndex
                            );

                    JSONObject valueObject =
                            entries.getJSONObject(
                                    key
                            );

                    String valueType =
                            valueObject.getString(
                                    "type"
                            );

                    if (!valueObject.has(
                            "value"
                    )) {
                        throw new EncryptedBackupDownloadException(
                                "Preference value is missing."
                        );
                    }

                    if (!isSupportedPreferenceType(
                            valueType
                    )) {
                        throw new EncryptedBackupDownloadException(
                                "Unsupported preference type "
                                        + valueType
                                        + "."
                        );
                    }
                }
            }

            totalEntryCount +=
                    entries.length();
        }

        return totalEntryCount;
    }

    private boolean isSupportedPreferenceType(
            @NonNull String valueType
    ) {
        switch (valueType) {
            case "string":
            case "integer":
            case "long":
            case "float":
            case "boolean":
            case "string_set":
            case "null":
                return true;

            default:
                return false;
        }
    }

    private void validateActiveProfiles(
            @NonNull JSONArray profileRows
    ) throws JSONException,
            EncryptedBackupDownloadException {

        int activeProfileCount = 0;

        for (int index = 0;
             index < profileRows.length();
             index++) {

            if (profileRows
                    .getJSONObject(index)
                    .optInt(
                            "is_active",
                            0
                    ) == 1) {

                activeProfileCount++;
            }
        }

        if (activeProfileCount > 1) {
            throw new EncryptedBackupDownloadException(
                    "Decrypted backup contains more "
                            + "than one active profile."
            );
        }
    }

    @NonNull
    private byte[] decompressGzip(
            @NonNull byte[] compressedBytes
    ) throws IOException {

        try (ByteArrayInputStream inputStream =
                     new ByteArrayInputStream(
                             compressedBytes
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

            byte[] digest =
                    messageDigest.digest(
                            sourceBytes
                    );

            StringBuilder builder =
                    new StringBuilder(
                            digest.length * 2
                    );

            for (byte digestByte : digest) {
                builder.append(
                        String.format(
                                Locale.US,
                                "%02x",
                                digestByte & 0xff
                        )
                );
            }

            clearByteArray(
                    digest
            );

            return builder.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IOException(
                    "SHA-256 is unavailable.",
                    exception
            );
        }
    }

    private void validateSha256Checksum(
            @NonNull String checksum,
            @NonNull String description
    ) throws EncryptedBackupDownloadException {

        if (!checksum.matches(
                "[0-9a-fA-F]{64}"
        )) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup "
                            + description
                            + " is invalid."
            );
        }
    }

    private void validateEncryptionBase64(
            @NonNull String encodedValue,
            int expectedByteCount,
            @NonNull String description
    ) throws EncryptedBackupDownloadException {

        byte[] decodedValue = null;

        try {
            decodedValue =
                    Base64.decode(
                            encodedValue,
                            Base64.NO_WRAP
                    );

            if (decodedValue.length
                    != expectedByteCount) {

                throw new EncryptedBackupDownloadException(
                        "Cloud backup "
                                + description
                                + " is invalid."
                );
            }

        } catch (IllegalArgumentException exception) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup "
                            + description
                            + " contains invalid Base64 data.",
                    exception
            );

        } finally {
            clearByteArray(
                    decodedValue
            );
        }
    }

    @NonNull
    private String getRequiredString(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) throws EncryptedBackupDownloadException {

        String value =
                snapshot.getString(
                        fieldName
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new EncryptedBackupDownloadException(
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
    ) throws EncryptedBackupDownloadException {

        Long value =
                snapshot.getLong(
                        fieldName
                );

        if (value == null) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " is missing."
            );
        }

        if (value > Integer.MAX_VALUE
                || value < Integer.MIN_VALUE) {

            throw new EncryptedBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " exceeds supported limits."
            );
        }

        return value.intValue();
    }

    private int getRequiredNonNegativeInt(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) throws EncryptedBackupDownloadException {

        int value =
                getRequiredInt(
                        snapshot,
                        fieldName
                );

        if (value < 0) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " cannot be negative."
            );
        }

        return value;
    }

    private long getRequiredLong(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) throws EncryptedBackupDownloadException {

        Long value =
                snapshot.getLong(
                        fieldName
                );

        if (value == null) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " is missing."
            );
        }

        return value;
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

    private boolean getRequiredBoolean(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) throws EncryptedBackupDownloadException {

        Boolean value =
                snapshot.getBoolean(
                        fieldName
                );

        if (value == null) {
            throw new EncryptedBackupDownloadException(
                    "Cloud backup field "
                            + fieldName
                            + " is missing."
            );
        }

        return value;
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

    private void clearByteArray(
            byte[] bytes
    ) {
        if (bytes == null) {
            return;
        }

        Arrays.fill(
                bytes,
                (byte) 0
        );
    }

    public interface DownloadCallback {

        void onSuccess(
                @NonNull EncryptedBackupDownloadResult result
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class
    EncryptedBackupDownloadResult {

        private final JSONObject backupJson;

        private final String backupId;

        private final long backupCreatedAt;

        private final long uploadedAt;

        private final String encryptedChecksumSha256;

        private final String plaintextChecksumSha256;

        private final int chunkCount;

        private final int rawJsonBytes;

        private final int compressedPlaintextBytes;

        private final int encryptedBytes;

        private final int profileCount;

        private final int lessonProgressCount;

        private final int quizAttemptCount;

        private final int doubtCount;

        private final int preferenceItemCount;

        private EncryptedBackupDownloadResult(
                @NonNull JSONObject backupJson,
                @NonNull String backupId,
                long backupCreatedAt,
                long uploadedAt,
                @NonNull String encryptedChecksumSha256,
                @NonNull String plaintextChecksumSha256,
                int chunkCount,
                int rawJsonBytes,
                int compressedPlaintextBytes,
                int encryptedBytes,
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

            this.encryptedChecksumSha256 =
                    encryptedChecksumSha256;

            this.plaintextChecksumSha256 =
                    plaintextChecksumSha256;

            this.chunkCount =
                    chunkCount;

            this.rawJsonBytes =
                    rawJsonBytes;

            this.compressedPlaintextBytes =
                    compressedPlaintextBytes;

            this.encryptedBytes =
                    encryptedBytes;

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
        public String getEncryptedChecksumSha256() {
            return encryptedChecksumSha256;
        }

        @NonNull
        public String getPlaintextChecksumSha256() {
            return plaintextChecksumSha256;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public int getRawJsonBytes() {
            return rawJsonBytes;
        }

        public int getCompressedPlaintextBytes() {
            return compressedPlaintextBytes;
        }

        public int getEncryptedBytes() {
            return encryptedBytes;
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
    EncryptedBackupDownloadException
            extends Exception {

        public EncryptedBackupDownloadException(
                @NonNull String message
        ) {
            super(message);
        }

        public EncryptedBackupDownloadException(
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
    InvalidCloudBackupPassphraseException
            extends EncryptedBackupDownloadException {

        public InvalidCloudBackupPassphraseException(
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
    EncryptedBackupNotFoundException
            extends EncryptedBackupDownloadException {

        public EncryptedBackupNotFoundException(
                @NonNull String message
        ) {
            super(message);
        }
    }

    private final class DownloadSession {

        private final String expectedUserId;

        private final char[] passphraseCopy;

        private final DownloadCallback callback;

        private final AtomicBoolean completed =
                new AtomicBoolean(false);

        private DownloadSession(
                @NonNull String expectedUserId,
                @NonNull char[] passphraseCopy,
                @NonNull DownloadCallback callback
        ) {
            this.expectedUserId =
                    expectedUserId;

            this.passphraseCopy =
                    passphraseCopy;

            this.callback =
                    callback;
        }

        private boolean isCompleted() {
            return completed.get();
        }

        private void succeed(
                @NonNull EncryptedBackupDownloadResult result
        ) {
            if (!completed.compareAndSet(
                    false,
                    true
            )) {
                return;
            }

            clearPassphrase();

            mainThreadHandler.post(() ->
                    callback.onSuccess(
                            result
                    )
            );
        }

        private void fail(
                @NonNull Exception exception
        ) {
            if (!completed.compareAndSet(
                    false,
                    true
            )) {
                return;
            }

            clearPassphrase();

            mainThreadHandler.post(() ->
                    callback.onError(
                            exception
                    )
            );
        }

        private void clearPassphrase() {
            Arrays.fill(
                    passphraseCopy,
                    '\0'
            );
        }
    }

    private static final class
    EncryptedBackupMetadata {

        private final String ownerUserId;

        private final String backupId;

        private final long backupCreatedAt;

        private final long uploadedAt;

        private final int chunkCount;

        private final int rawJsonBytes;

        private final int compressedPlaintextBytes;

        private final int encryptedBytes;

        private final int encodedCharacters;

        private final int encryptionVersion;

        private final String cipherTransformation;

        private final String kdfAlgorithm;

        private final int kdfIterations;

        private final int keyLengthBits;

        private final int gcmTagLengthBits;

        private final String saltBase64;

        private final String initializationVectorBase64;

        private final String plaintextChecksumSha256;

        private final String encryptedChecksumSha256;

        private final int profileCount;

        private final int lessonProgressCount;

        private final int quizAttemptCount;

        private final int doubtCount;

        private final int preferenceItemCount;

        private EncryptedBackupMetadata(
                @NonNull String ownerUserId,
                @NonNull String backupId,
                long backupCreatedAt,
                long uploadedAt,
                int chunkCount,
                int rawJsonBytes,
                int compressedPlaintextBytes,
                int encryptedBytes,
                int encodedCharacters,
                int encryptionVersion,
                @NonNull String cipherTransformation,
                @NonNull String kdfAlgorithm,
                int kdfIterations,
                int keyLengthBits,
                int gcmTagLengthBits,
                @NonNull String saltBase64,
                @NonNull String initializationVectorBase64,
                @NonNull String plaintextChecksumSha256,
                @NonNull String encryptedChecksumSha256,
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

            this.backupCreatedAt =
                    backupCreatedAt;

            this.uploadedAt =
                    uploadedAt;

            this.chunkCount =
                    chunkCount;

            this.rawJsonBytes =
                    rawJsonBytes;

            this.compressedPlaintextBytes =
                    compressedPlaintextBytes;

            this.encryptedBytes =
                    encryptedBytes;

            this.encodedCharacters =
                    encodedCharacters;

            this.encryptionVersion =
                    encryptionVersion;

            this.cipherTransformation =
                    cipherTransformation;

            this.kdfAlgorithm =
                    kdfAlgorithm;

            this.kdfIterations =
                    kdfIterations;

            this.keyLengthBits =
                    keyLengthBits;

            this.gcmTagLengthBits =
                    gcmTagLengthBits;

            this.saltBase64 =
                    saltBase64;

            this.initializationVectorBase64 =
                    initializationVectorBase64;

            this.plaintextChecksumSha256 =
                    plaintextChecksumSha256;

            this.encryptedChecksumSha256 =
                    encryptedChecksumSha256;

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

    private static final class
    BackupContentCounts {

        private final int profileCount;

        private final int lessonProgressCount;

        private final int quizAttemptCount;

        private final int doubtCount;

        private final int preferenceItemCount;

        private BackupContentCounts(
                int profileCount,
                int lessonProgressCount,
                int quizAttemptCount,
                int doubtCount,
                int preferenceItemCount
        ) {
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

