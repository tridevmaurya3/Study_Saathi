package com.tridev.studysaathi.cloud;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CloudBackupUploader {

    private static final String COLLECTION_USERS =
            "users";

    private static final String COLLECTION_CLOUD_BACKUPS =
            "cloud_backups";

    private static final String COLLECTION_CHUNKS =
            "chunks";

    private static final String LATEST_BACKUP_DOCUMENT =
            "latest";

    private static final String APP_PACKAGE =
            "com.tridev.studysaathi";

    private static final String STATUS_COMPLETE =
            "complete";

    private static final String LEGACY_PAYLOAD_FORMAT =
            "study_saathi_compressed_cloud_backup";

    private static final int LEGACY_PAYLOAD_FORMAT_VERSION =
            1;

    /*
     * Firestore allows up to 500 writes in a batch.
     * This lower limit leaves room for field transforms
     * such as FieldValue.serverTimestamp().
     */
    private static final int MAX_SAFE_BATCH_OPERATIONS =
            450;

    private final FirebaseFirestore firestore;

    public CloudBackupUploader() {
        firestore =
                FirebaseFirestore.getInstance();
    }

    /**
     * Keeps support for the existing compressed but
     * unencrypted cloud payload.
     *
     * This method remains available so the current
     * CloudAccountActivity continues compiling until
     * encrypted upload is activated in the next step.
     */
    public void uploadLatestBackup(
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupPayloadBuilder
                    .CloudBackupPayload payload,
            @NonNull UploadCallback callback
    ) {
        Exception accountError =
                validateVerifiedUser(
                        firebaseUser
                );

        if (accountError != null) {
            callback.onError(
                    accountError
            );

            return;
        }

        PreparedUpload preparedUpload;

        try {
            preparedUpload =
                    createLegacyPreparedUpload(
                            firebaseUser,
                            payload
                    );

        } catch (Exception exception) {
            callback.onError(
                    exception
            );

            return;
        }

        loadExistingChunksAndUpload(
                firebaseUser,
                preparedUpload,
                callback
        );
    }

    /**
     * Uploads an AES-256-GCM encrypted cloud payload.
     *
     * The passphrase itself is never passed to this
     * uploader. It receives only ciphertext, salt, IV
     * and non-secret encryption metadata.
     */
    public void uploadLatestEncryptedBackup(
            @NonNull FirebaseUser firebaseUser,
            @NonNull EncryptedCloudBackupPayloadBuilder
                    .EncryptedCloudBackupPayload payload,
            @NonNull UploadCallback callback
    ) {
        Exception accountError =
                validateVerifiedUser(
                        firebaseUser
                );

        if (accountError != null) {
            callback.onError(
                    accountError
            );

            return;
        }

        if (!firebaseUser.getUid().equals(
                payload.getOwnerUserId()
        )) {
            callback.onError(
                    new IllegalStateException(
                            "Encrypted cloud backup owner "
                                    + "does not match the "
                                    + "signed-in account."
                    )
            );

            return;
        }

        PreparedUpload preparedUpload;

        try {
            preparedUpload =
                    createEncryptedPreparedUpload(
                            firebaseUser,
                            payload
                    );

        } catch (Exception exception) {
            callback.onError(
                    exception
            );

            return;
        }

        loadExistingChunksAndUpload(
                firebaseUser,
                preparedUpload,
                callback
        );
    }

    /**
     * Reads metadata for the currently stored latest
     * backup directly from the Firestore server.
     *
     * Both older unencrypted backups and future
     * encrypted backups are supported.
     */
    public void loadLatestBackupMetadata(
            @NonNull FirebaseUser firebaseUser,
            @NonNull MetadataCallback callback
    ) {
        String userId =
                firebaseUser.getUid();

        if (userId.trim().isEmpty()) {
            callback.onError(
                    new IllegalStateException(
                            "Firebase user ID is unavailable."
                    )
            );

            return;
        }

        getLatestBackupReference(
                userId
        )
                .get(Source.SERVER)
                .addOnSuccessListener(
                        documentSnapshot -> {
                            if (!documentSnapshot.exists()) {
                                callback.onLoaded(null);

                                return;
                            }

                            try {
                                callback.onLoaded(
                                        createMetadata(
                                                documentSnapshot
                                        )
                                );

                            } catch (Exception exception) {
                                callback.onError(
                                        exception
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        callback::onError
                );
    }

    private void loadExistingChunksAndUpload(
            @NonNull FirebaseUser firebaseUser,
            @NonNull PreparedUpload preparedUpload,
            @NonNull UploadCallback callback
    ) {
        String userId =
                firebaseUser.getUid();

        DocumentReference latestBackupReference =
                getLatestBackupReference(
                        userId
                );

        CollectionReference chunksReference =
                latestBackupReference.collection(
                        COLLECTION_CHUNKS
                );

        /*
         * Always inspect the server copy so abandoned
         * or old chunk documents can be removed safely.
         */
        chunksReference
                .get(Source.SERVER)
                .addOnSuccessListener(
                        existingChunkSnapshot ->
                                replaceCloudBackup(
                                        preparedUpload,
                                        latestBackupReference,
                                        chunksReference,
                                        existingChunkSnapshot,
                                        callback
                                )
                )
                .addOnFailureListener(
                        callback::onError
                );
    }

    private void replaceCloudBackup(
            @NonNull PreparedUpload preparedUpload,
            @NonNull DocumentReference latestBackupReference,
            @NonNull CollectionReference chunksReference,
            @NonNull QuerySnapshot existingChunkSnapshot,
            @NonNull UploadCallback callback
    ) {
        Set<String> newChunkDocumentIds =
                new HashSet<>();

        for (int chunkIndex = 0;
             chunkIndex
                     < preparedUpload
                     .chunkDocuments
                     .size();
             chunkIndex++) {

            newChunkDocumentIds.add(
                    createChunkDocumentId(
                            chunkIndex
                    )
            );
        }

        int staleChunkDeleteCount = 0;

        for (DocumentSnapshot oldChunkDocument
                : existingChunkSnapshot
                .getDocuments()) {

            if (!newChunkDocumentIds.contains(
                    oldChunkDocument.getId()
            )) {
                staleChunkDeleteCount++;
            }
        }

        /*
         * Operations:
         *
         * 1. One set for every new chunk.
         * 2. One delete for every stale old chunk.
         * 3. One metadata document set.
         * 4. One additional operation allowance for
         *    the server timestamp field transform.
         */
        int requiredBatchOperations =
                preparedUpload
                        .chunkDocuments
                        .size()
                        + staleChunkDeleteCount
                        + 2;

        if (requiredBatchOperations
                > MAX_SAFE_BATCH_OPERATIONS) {

            callback.onError(
                    new IllegalStateException(
                            "Cloud backup replacement needs "
                                    + requiredBatchOperations
                                    + " batch operations, which "
                                    + "exceeds the safe limit."
                    )
            );

            return;
        }

        WriteBatch writeBatch =
                firestore.batch();

        /*
         * A chunk document that also exists in the new
         * payload is overwritten with set().
         *
         * Only documents that do not belong to the new
         * payload are deleted.
         */
        for (DocumentSnapshot oldChunkDocument
                : existingChunkSnapshot
                .getDocuments()) {

            if (!newChunkDocumentIds.contains(
                    oldChunkDocument.getId()
            )) {
                writeBatch.delete(
                        oldChunkDocument
                                .getReference()
                );
            }
        }

        for (int chunkIndex = 0;
             chunkIndex
                     < preparedUpload
                     .chunkDocuments
                     .size();
             chunkIndex++) {

            String chunkDocumentId =
                    createChunkDocumentId(
                            chunkIndex
                    );

            DocumentReference chunkReference =
                    chunksReference.document(
                            chunkDocumentId
                    );

            writeBatch.set(
                    chunkReference,
                    preparedUpload
                            .chunkDocuments
                            .get(chunkIndex)
            );
        }

        long clientUploadTime =
                System.currentTimeMillis();

        Map<String, Object> finalMetadata =
                new HashMap<>(
                        preparedUpload.metadata
                );

        finalMetadata.put(
                "uploaded_at",
                FieldValue.serverTimestamp()
        );

        finalMetadata.put(
                "uploaded_at_client",
                clientUploadTime
        );

        writeBatch.set(
                latestBackupReference,
                finalMetadata
        );

        writeBatch.commit()
                .addOnSuccessListener(
                        unused ->
                                callback.onSuccess(
                                        new UploadResult(
                                                preparedUpload
                                                        .backupId,
                                                clientUploadTime,
                                                preparedUpload
                                                        .chunkDocuments
                                                        .size(),
                                                preparedUpload
                                                        .compressedBytes,
                                                preparedUpload
                                                        .encryptedBytes,
                                                preparedUpload
                                                        .checksumSha256,
                                                preparedUpload
                                                        .clientSideEncrypted
                                        )
                                )
                )
                .addOnFailureListener(
                        callback::onError
                );
    }

    @NonNull
    private PreparedUpload createLegacyPreparedUpload(
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupPayloadBuilder
                    .CloudBackupPayload payload
    ) {
        validateBackupIdentity(
                payload.getBackupId(),
                payload.getCreatedAt()
        );

        validateChunks(
                payload.getChunks(),
                payload.getEncodedCharacters()
        );

        List<Map<String, Object>> chunkDocuments =
                new ArrayList<>();

        for (int chunkIndex = 0;
             chunkIndex
                     < payload.getChunks().size();
             chunkIndex++) {

            String chunkContent =
                    payload.getChunks()
                            .get(chunkIndex);

            chunkDocuments.add(
                    createLegacyChunkDocument(
                            firebaseUser,
                            payload,
                            chunkIndex,
                            chunkContent
                    )
            );
        }

        Map<String, Object> metadata =
                createLegacyMetadataDocument(
                        firebaseUser,
                        payload
                );

        return new PreparedUpload(
                payload.getBackupId(),
                false,
                payload.getCompressedBytes(),
                0,
                payload.getChecksumSha256(),
                chunkDocuments,
                metadata
        );
    }

    @NonNull
    private PreparedUpload createEncryptedPreparedUpload(
            @NonNull FirebaseUser firebaseUser,
            @NonNull EncryptedCloudBackupPayloadBuilder
                    .EncryptedCloudBackupPayload payload
    ) {
        validateBackupIdentity(
                payload.getBackupId(),
                payload.getCreatedAt()
        );

        validateChunks(
                payload.getChunks(),
                payload.getEncodedCharacters()
        );

        if (payload.getEncryptedBytes() <= 0) {
            throw new IllegalArgumentException(
                    "Encrypted cloud backup size "
                            + "is invalid."
            );
        }

        if (payload.getCompressedPlaintextBytes()
                <= 0) {

            throw new IllegalArgumentException(
                    "Compressed plaintext size "
                            + "is invalid."
            );
        }

        validateSha256Checksum(
                payload.getPlaintextChecksumSha256(),
                "plaintext checksum"
        );

        validateSha256Checksum(
                payload.getEncryptedChecksumSha256(),
                "encrypted checksum"
        );

        List<Map<String, Object>> chunkDocuments =
                new ArrayList<>();

        for (int chunkIndex = 0;
             chunkIndex
                     < payload.getChunks().size();
             chunkIndex++) {

            String chunkContent =
                    payload.getChunks()
                            .get(chunkIndex);

            chunkDocuments.add(
                    createEncryptedChunkDocument(
                            firebaseUser,
                            payload,
                            chunkIndex,
                            chunkContent
                    )
            );
        }

        Map<String, Object> metadata =
                createEncryptedMetadataDocument(
                        firebaseUser,
                        payload
                );

        return new PreparedUpload(
                payload.getBackupId(),
                true,
                payload.getCompressedPlaintextBytes(),
                payload.getEncryptedBytes(),
                payload.getEncryptedChecksumSha256(),
                chunkDocuments,
                metadata
        );
    }

    @NonNull
    private Map<String, Object> createLegacyChunkDocument(
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupPayloadBuilder
                    .CloudBackupPayload payload,
            int chunkIndex,
            @NonNull String chunkContent
    ) {
        Map<String, Object> chunkData =
                new HashMap<>();

        chunkData.put(
                "owner_uid",
                firebaseUser.getUid()
        );

        chunkData.put(
                "backup_id",
                payload.getBackupId()
        );

        chunkData.put(
                "chunk_index",
                chunkIndex
        );

        chunkData.put(
                "chunk_count",
                payload.getChunkCount()
        );

        chunkData.put(
                "payload",
                chunkContent
        );

        chunkData.put(
                "payload_format",
                LEGACY_PAYLOAD_FORMAT
        );

        chunkData.put(
                "payload_format_version",
                LEGACY_PAYLOAD_FORMAT_VERSION
        );

        chunkData.put(
                "is_client_side_encrypted",
                false
        );

        chunkData.put(
                "encoding",
                payload.getEncodingType()
        );

        chunkData.put(
                "compression",
                payload.getCompressionType()
        );

        chunkData.put(
                "checksum_sha256",
                payload.getChecksumSha256()
        );

        chunkData.put(
                "backup_created_at",
                payload.getCreatedAt()
        );

        return chunkData;
    }

    @NonNull
    private Map<String, Object> createEncryptedChunkDocument(
            @NonNull FirebaseUser firebaseUser,
            @NonNull EncryptedCloudBackupPayloadBuilder
                    .EncryptedCloudBackupPayload payload,
            int chunkIndex,
            @NonNull String chunkContent
    ) {
        Map<String, Object> chunkData =
                new HashMap<>();

        chunkData.put(
                "owner_uid",
                firebaseUser.getUid()
        );

        chunkData.put(
                "backup_id",
                payload.getBackupId()
        );

        chunkData.put(
                "chunk_index",
                chunkIndex
        );

        chunkData.put(
                "chunk_count",
                payload.getChunkCount()
        );

        chunkData.put(
                "payload",
                chunkContent
        );

        chunkData.put(
                "payload_format",
                payload.getEncryptedPayloadFormat()
        );

        chunkData.put(
                "payload_format_version",
                payload.getEncryptedPayloadFormatVersion()
        );

        chunkData.put(
                "is_client_side_encrypted",
                true
        );

        chunkData.put(
                "encoding",
                payload.getOuterEncoding()
        );

        chunkData.put(
                "compression",
                payload.getInnerCompression()
        );

        chunkData.put(
                "encryption_version",
                payload.getEncryptionVersion()
        );

        chunkData.put(
                "encrypted_checksum_sha256",
                payload.getEncryptedChecksumSha256()
        );

        /*
         * checksum_sha256 remains available as a common
         * integrity field. For encrypted backups it
         * represents the ciphertext checksum.
         */
        chunkData.put(
                "checksum_sha256",
                payload.getEncryptedChecksumSha256()
        );

        chunkData.put(
                "backup_created_at",
                payload.getCreatedAt()
        );

        return chunkData;
    }

    @NonNull
    private Map<String, Object> createLegacyMetadataDocument(
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupPayloadBuilder
                    .CloudBackupPayload payload
    ) {
        CloudBackupPayloadBuilder.BackupOverview overview =
                payload.getOverview();

        Map<String, Object> metadata =
                createCommonMetadata(
                        firebaseUser,
                        payload.getBackupId(),
                        payload.getCreatedAt(),
                        payload.getBackupFormat(),
                        payload.getBackupFormatVersion(),
                        payload.getDatabaseSchemaVersion(),
                        payload.getChunkCount(),
                        payload.getRawJsonBytes(),
                        payload.getCompressedBytes(),
                        payload.getEncodedCharacters(),
                        overview
                );

        metadata.put(
                "payload_format",
                LEGACY_PAYLOAD_FORMAT
        );

        metadata.put(
                "payload_format_version",
                LEGACY_PAYLOAD_FORMAT_VERSION
        );

        metadata.put(
                "is_client_side_encrypted",
                false
        );

        metadata.put(
                "compression",
                payload.getCompressionType()
        );

        metadata.put(
                "encoding",
                payload.getEncodingType()
        );

        metadata.put(
                "checksum_sha256",
                payload.getChecksumSha256()
        );

        metadata.put(
                "plaintext_checksum_sha256",
                payload.getChecksumSha256()
        );

        metadata.put(
                "encrypted_bytes",
                0
        );

        return metadata;
    }

    @NonNull
    private Map<String, Object> createEncryptedMetadataDocument(
            @NonNull FirebaseUser firebaseUser,
            @NonNull EncryptedCloudBackupPayloadBuilder
                    .EncryptedCloudBackupPayload payload
    ) {
        CloudBackupPayloadBuilder.BackupOverview overview =
                payload.getOverview();

        Map<String, Object> metadata =
                createCommonMetadata(
                        firebaseUser,
                        payload.getBackupId(),
                        payload.getCreatedAt(),
                        payload.getBackupFormat(),
                        payload.getBackupFormatVersion(),
                        payload.getDatabaseSchemaVersion(),
                        payload.getChunkCount(),
                        payload.getRawJsonBytes(),
                        payload.getCompressedPlaintextBytes(),
                        payload.getEncodedCharacters(),
                        overview
                );

        metadata.put(
                "payload_format",
                payload.getEncryptedPayloadFormat()
        );

        metadata.put(
                "payload_format_version",
                payload.getEncryptedPayloadFormatVersion()
        );

        metadata.put(
                "is_client_side_encrypted",
                true
        );

        metadata.put(
                "compression",
                payload.getInnerCompression()
        );

        metadata.put(
                "encoding",
                payload.getOuterEncoding()
        );

        metadata.put(
                "associated_data_scheme",
                payload.getAssociatedDataScheme()
        );

        metadata.put(
                "encryption_version",
                payload.getEncryptionVersion()
        );

        metadata.put(
                "cipher_transformation",
                payload.getCipherTransformation()
        );

        metadata.put(
                "kdf_algorithm",
                payload.getKdfAlgorithm()
        );

        metadata.put(
                "kdf_iterations",
                payload.getKdfIterations()
        );

        metadata.put(
                "key_length_bits",
                payload.getKeyLengthBits()
        );

        metadata.put(
                "gcm_tag_length_bits",
                payload.getGcmTagLengthBits()
        );

        metadata.put(
                "salt_base64",
                payload.getSaltBase64()
        );

        metadata.put(
                "initialization_vector_base64",
                payload.getInitializationVectorBase64()
        );

        metadata.put(
                "plaintext_checksum_sha256",
                payload.getPlaintextChecksumSha256()
        );

        metadata.put(
                "encrypted_checksum_sha256",
                payload.getEncryptedChecksumSha256()
        );

        metadata.put(
                "checksum_sha256",
                payload.getEncryptedChecksumSha256()
        );

        metadata.put(
                "compressed_plaintext_bytes",
                payload.getCompressedPlaintextBytes()
        );

        metadata.put(
                "encrypted_bytes",
                payload.getEncryptedBytes()
        );

        return metadata;
    }

    @NonNull
    private Map<String, Object> createCommonMetadata(
            @NonNull FirebaseUser firebaseUser,
            @NonNull String backupId,
            long backupCreatedAt,
            @NonNull String backupFormat,
            int backupFormatVersion,
            int databaseSchemaVersion,
            int chunkCount,
            int rawJsonBytes,
            int compressedBytes,
            int encodedCharacters,
            @NonNull CloudBackupPayloadBuilder
                    .BackupOverview overview
    ) {
        Map<String, Object> metadata =
                new HashMap<>();

        metadata.put(
                "owner_uid",
                firebaseUser.getUid()
        );

        metadata.put(
                "backup_id",
                backupId
        );

        metadata.put(
                "status",
                STATUS_COMPLETE
        );

        metadata.put(
                "backup_format",
                backupFormat
        );

        metadata.put(
                "backup_format_version",
                backupFormatVersion
        );

        metadata.put(
                "database_schema_version",
                databaseSchemaVersion
        );

        metadata.put(
                "backup_created_at",
                backupCreatedAt
        );

        metadata.put(
                "chunk_count",
                chunkCount
        );

        metadata.put(
                "raw_json_bytes",
                rawJsonBytes
        );

        metadata.put(
                "compressed_bytes",
                compressedBytes
        );

        metadata.put(
                "encoded_characters",
                encodedCharacters
        );

        metadata.put(
                "profile_count",
                overview.getProfileCount()
        );

        metadata.put(
                "lesson_progress_count",
                overview.getLessonProgressCount()
        );

        metadata.put(
                "quiz_attempt_count",
                overview.getQuizAttemptCount()
        );

        metadata.put(
                "doubt_count",
                overview.getDoubtCount()
        );

        metadata.put(
                "preference_item_count",
                overview.getPreferenceItemCount()
        );

        metadata.put(
                "total_database_rows",
                overview.getTotalDatabaseRows()
        );

        metadata.put(
                "app_package",
                APP_PACKAGE
        );

        return metadata;
    }

    private void validateChunks(
            @NonNull List<String> chunks,
            int expectedEncodedCharacters
    ) {
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cloud backup contains no chunks."
            );
        }

        if (chunks.size()
                > CloudBackupPayloadBuilder
                .MAX_CHUNK_COUNT) {

            throw new IllegalArgumentException(
                    "Cloud backup chunk count exceeds "
                            + "the supported limit."
            );
        }

        long actualCharacterCount = 0L;

        for (String chunk : chunks) {
            if (chunk == null
                    || chunk.isEmpty()) {

                throw new IllegalArgumentException(
                        "Cloud backup contains an "
                                + "empty chunk."
                );
            }

            if (chunk.length()
                    > CloudBackupPayloadBuilder
                    .CHUNK_CHARACTER_LIMIT) {

                throw new IllegalArgumentException(
                        "Cloud backup chunk exceeds "
                                + "the supported size."
                );
            }

            actualCharacterCount +=
                    chunk.length();

            if (actualCharacterCount
                    > Integer.MAX_VALUE) {

                throw new IllegalArgumentException(
                        "Cloud backup encoded payload "
                                + "is too large."
                );
            }
        }

        if (actualCharacterCount
                != expectedEncodedCharacters) {

            throw new IllegalArgumentException(
                    "Cloud backup encoded size does "
                            + "not match its metadata."
            );
        }
    }

    private void validateBackupIdentity(
            @NonNull String backupId,
            long createdAt
    ) {
        if (backupId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cloud backup ID is unavailable."
            );
        }

        if (createdAt <= 0L) {
            throw new IllegalArgumentException(
                    "Cloud backup creation time "
                            + "is invalid."
            );
        }
    }

    private void validateSha256Checksum(
            @NonNull String checksum,
            @NonNull String fieldDescription
    ) {
        if (!checksum.matches(
                "[0-9a-fA-F]{64}"
        )) {
            throw new IllegalArgumentException(
                    "Cloud backup "
                            + fieldDescription
                            + " is invalid."
            );
        }
    }

    private Exception validateVerifiedUser(
            @NonNull FirebaseUser firebaseUser
    ) {
        if (!firebaseUser.isEmailVerified()) {
            return new IllegalStateException(
                    "Cloud backup requires a "
                            + "verified email account."
            );
        }

        if (firebaseUser.getUid()
                .trim()
                .isEmpty()) {

            return new IllegalStateException(
                    "Firebase user ID is unavailable."
            );
        }

        return null;
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

    @NonNull
    private String createChunkDocumentId(
            int chunkIndex
    ) {
        return String.format(
                Locale.US,
                "chunk_%03d",
                chunkIndex
        );
    }

    @NonNull
    private CloudBackupMetadata createMetadata(
            @NonNull DocumentSnapshot documentSnapshot
    ) {
        boolean clientSideEncrypted =
                getBooleanValue(
                        documentSnapshot,
                        "is_client_side_encrypted"
                );

        String commonChecksum =
                getStringValue(
                        documentSnapshot,
                        "checksum_sha256"
                );

        String plaintextChecksum =
                getStringValue(
                        documentSnapshot,
                        "plaintext_checksum_sha256"
                );

        String encryptedChecksum =
                getStringValue(
                        documentSnapshot,
                        "encrypted_checksum_sha256"
                );

        if (plaintextChecksum.isEmpty()
                && !clientSideEncrypted) {

            plaintextChecksum =
                    commonChecksum;
        }

        if (encryptedChecksum.isEmpty()
                && clientSideEncrypted) {

            encryptedChecksum =
                    commonChecksum;
        }

        String effectiveChecksum =
                clientSideEncrypted
                        ? encryptedChecksum
                        : plaintextChecksum;

        if (effectiveChecksum.isEmpty()) {
            effectiveChecksum =
                    commonChecksum;
        }

        return new CloudBackupMetadata(
                getStringValue(
                        documentSnapshot,
                        "backup_id"
                ),
                getStringValue(
                        documentSnapshot,
                        "status"
                ),
                effectiveChecksum,
                plaintextChecksum,
                encryptedChecksum,
                getStringValue(
                        documentSnapshot,
                        "payload_format"
                ),
                getIntValue(
                        documentSnapshot,
                        "payload_format_version"
                ),
                clientSideEncrypted,
                getLongValue(
                        documentSnapshot,
                        "backup_created_at"
                ),
                getLongValue(
                        documentSnapshot,
                        "uploaded_at_client"
                ),
                getIntValue(
                        documentSnapshot,
                        "chunk_count"
                ),
                getIntValue(
                        documentSnapshot,
                        "compressed_bytes"
                ),
                getIntValue(
                        documentSnapshot,
                        "encrypted_bytes"
                ),
                getIntValue(
                        documentSnapshot,
                        "profile_count"
                ),
                getIntValue(
                        documentSnapshot,
                        "lesson_progress_count"
                ),
                getIntValue(
                        documentSnapshot,
                        "quiz_attempt_count"
                ),
                getIntValue(
                        documentSnapshot,
                        "doubt_count"
                ),
                getIntValue(
                        documentSnapshot,
                        "preference_item_count"
                ),
                getStringValue(
                        documentSnapshot,
                        "compression"
                ),
                getStringValue(
                        documentSnapshot,
                        "encoding"
                ),
                getStringValue(
                        documentSnapshot,
                        "associated_data_scheme"
                ),
                getIntValue(
                        documentSnapshot,
                        "encryption_version"
                ),
                getStringValue(
                        documentSnapshot,
                        "cipher_transformation"
                ),
                getStringValue(
                        documentSnapshot,
                        "kdf_algorithm"
                ),
                getIntValue(
                        documentSnapshot,
                        "kdf_iterations"
                ),
                getIntValue(
                        documentSnapshot,
                        "key_length_bits"
                ),
                getIntValue(
                        documentSnapshot,
                        "gcm_tag_length_bits"
                ),
                getStringValue(
                        documentSnapshot,
                        "salt_base64"
                ),
                getStringValue(
                        documentSnapshot,
                        "initialization_vector_base64"
                )
        );
    }

    @NonNull
    private String getStringValue(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) {
        String value =
                snapshot.getString(
                        fieldName
                );

        return value == null
                ? ""
                : value;
    }

    private long getLongValue(
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

    private int getIntValue(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) {
        Long value =
                snapshot.getLong(
                        fieldName
                );

        if (value == null) {
            return 0;
        }

        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        return value.intValue();
    }

    private boolean getBooleanValue(
            @NonNull DocumentSnapshot snapshot,
            @NonNull String fieldName
    ) {
        Boolean value =
                snapshot.getBoolean(
                        fieldName
                );

        return value != null
                && value;
    }

    public interface UploadCallback {

        void onSuccess(
                @NonNull UploadResult uploadResult
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public interface MetadataCallback {

        void onLoaded(
                CloudBackupMetadata cloudBackupMetadata
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class UploadResult {

        private final String backupId;

        private final long uploadedAt;

        private final int chunkCount;

        private final int compressedBytes;

        private final int encryptedBytes;

        private final String checksumSha256;

        private final boolean clientSideEncrypted;

        private UploadResult(
                @NonNull String backupId,
                long uploadedAt,
                int chunkCount,
                int compressedBytes,
                int encryptedBytes,
                @NonNull String checksumSha256,
                boolean clientSideEncrypted
        ) {
            this.backupId =
                    backupId;

            this.uploadedAt =
                    uploadedAt;

            this.chunkCount =
                    chunkCount;

            this.compressedBytes =
                    compressedBytes;

            this.encryptedBytes =
                    encryptedBytes;

            this.checksumSha256 =
                    checksumSha256;

            this.clientSideEncrypted =
                    clientSideEncrypted;
        }

        @NonNull
        public String getBackupId() {
            return backupId;
        }

        public long getUploadedAt() {
            return uploadedAt;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public int getCompressedBytes() {
            return compressedBytes;
        }

        public int getEncryptedBytes() {
            return encryptedBytes;
        }

        @NonNull
        public String getChecksumSha256() {
            return checksumSha256;
        }

        public boolean isClientSideEncrypted() {
            return clientSideEncrypted;
        }

        public int getStoredPayloadBytes() {
            return clientSideEncrypted
                    && encryptedBytes > 0
                    ? encryptedBytes
                    : compressedBytes;
        }
    }

    public static final class CloudBackupMetadata {

        private final String backupId;

        private final String status;

        private final String checksumSha256;

        private final String plaintextChecksumSha256;

        private final String encryptedChecksumSha256;

        private final String payloadFormat;

        private final int payloadFormatVersion;

        private final boolean clientSideEncrypted;

        private final long backupCreatedAt;

        private final long uploadedAt;

        private final int chunkCount;

        private final int compressedBytes;

        private final int encryptedBytes;

        private final int profileCount;

        private final int lessonProgressCount;

        private final int quizAttemptCount;

        private final int doubtCount;

        private final int preferenceItemCount;

        private final String compression;

        private final String encoding;

        private final String associatedDataScheme;

        private final int encryptionVersion;

        private final String cipherTransformation;

        private final String kdfAlgorithm;

        private final int kdfIterations;

        private final int keyLengthBits;

        private final int gcmTagLengthBits;

        private final String saltBase64;

        private final String initializationVectorBase64;

        private CloudBackupMetadata(
                @NonNull String backupId,
                @NonNull String status,
                @NonNull String checksumSha256,
                @NonNull String plaintextChecksumSha256,
                @NonNull String encryptedChecksumSha256,
                @NonNull String payloadFormat,
                int payloadFormatVersion,
                boolean clientSideEncrypted,
                long backupCreatedAt,
                long uploadedAt,
                int chunkCount,
                int compressedBytes,
                int encryptedBytes,
                int profileCount,
                int lessonProgressCount,
                int quizAttemptCount,
                int doubtCount,
                int preferenceItemCount,
                @NonNull String compression,
                @NonNull String encoding,
                @NonNull String associatedDataScheme,
                int encryptionVersion,
                @NonNull String cipherTransformation,
                @NonNull String kdfAlgorithm,
                int kdfIterations,
                int keyLengthBits,
                int gcmTagLengthBits,
                @NonNull String saltBase64,
                @NonNull String initializationVectorBase64
        ) {
            this.backupId =
                    backupId;

            this.status =
                    status;

            this.checksumSha256 =
                    checksumSha256;

            this.plaintextChecksumSha256 =
                    plaintextChecksumSha256;

            this.encryptedChecksumSha256 =
                    encryptedChecksumSha256;

            this.payloadFormat =
                    payloadFormat;

            this.payloadFormatVersion =
                    payloadFormatVersion;

            this.clientSideEncrypted =
                    clientSideEncrypted;

            this.backupCreatedAt =
                    backupCreatedAt;

            this.uploadedAt =
                    uploadedAt;

            this.chunkCount =
                    chunkCount;

            this.compressedBytes =
                    compressedBytes;

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

            this.compression =
                    compression;

            this.encoding =
                    encoding;

            this.associatedDataScheme =
                    associatedDataScheme;

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
        }

        @NonNull
        public String getBackupId() {
            return backupId;
        }

        @NonNull
        public String getStatus() {
            return status;
        }

        @NonNull
        public String getChecksumSha256() {
            return checksumSha256;
        }

        @NonNull
        public String getPlaintextChecksumSha256() {
            return plaintextChecksumSha256;
        }

        @NonNull
        public String getEncryptedChecksumSha256() {
            return encryptedChecksumSha256;
        }

        @NonNull
        public String getPayloadFormat() {
            return payloadFormat;
        }

        public int getPayloadFormatVersion() {
            return payloadFormatVersion;
        }

        public boolean isClientSideEncrypted() {
            return clientSideEncrypted;
        }

        public long getBackupCreatedAt() {
            return backupCreatedAt;
        }

        public long getUploadedAt() {
            return uploadedAt;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public int getCompressedBytes() {
            return compressedBytes;
        }

        public int getEncryptedBytes() {
            return encryptedBytes;
        }

        public int getStoredPayloadBytes() {
            return clientSideEncrypted
                    && encryptedBytes > 0
                    ? encryptedBytes
                    : compressedBytes;
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

        @NonNull
        public String getCompression() {
            return compression;
        }

        @NonNull
        public String getEncoding() {
            return encoding;
        }

        @NonNull
        public String getAssociatedDataScheme() {
            return associatedDataScheme;
        }

        public int getEncryptionVersion() {
            return encryptionVersion;
        }

        @NonNull
        public String getCipherTransformation() {
            return cipherTransformation;
        }

        @NonNull
        public String getKdfAlgorithm() {
            return kdfAlgorithm;
        }

        public int getKdfIterations() {
            return kdfIterations;
        }

        public int getKeyLengthBits() {
            return keyLengthBits;
        }

        public int getGcmTagLengthBits() {
            return gcmTagLengthBits;
        }

        @NonNull
        public String getSaltBase64() {
            return saltBase64;
        }

        @NonNull
        public String getInitializationVectorBase64() {
            return initializationVectorBase64;
        }

        public boolean isComplete() {
            return STATUS_COMPLETE.equals(
                    status
            );
        }
    }

    private static final class PreparedUpload {

        private final String backupId;

        private final boolean clientSideEncrypted;

        private final int compressedBytes;

        private final int encryptedBytes;

        private final String checksumSha256;

        private final List<Map<String, Object>>
                chunkDocuments;

        private final Map<String, Object> metadata;

        private PreparedUpload(
                @NonNull String backupId,
                boolean clientSideEncrypted,
                int compressedBytes,
                int encryptedBytes,
                @NonNull String checksumSha256,
                @NonNull List<Map<String, Object>>
                        chunkDocuments,
                @NonNull Map<String, Object> metadata
        ) {
            this.backupId =
                    backupId;

            this.clientSideEncrypted =
                    clientSideEncrypted;

            this.compressedBytes =
                    compressedBytes;

            this.encryptedBytes =
                    encryptedBytes;

            this.checksumSha256 =
                    checksumSha256;

            this.chunkDocuments =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    chunkDocuments
                            )
                    );

            this.metadata =
                    Collections.unmodifiableMap(
                            new HashMap<>(
                                    metadata
                            )
                    );
        }
    }
}