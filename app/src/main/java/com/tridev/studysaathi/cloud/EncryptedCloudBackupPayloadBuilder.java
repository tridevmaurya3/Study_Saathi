package com.tridev.studysaathi.cloud;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class EncryptedCloudBackupPayloadBuilder {

    public static final String ENCRYPTED_PAYLOAD_FORMAT =
            "study_saathi_encrypted_cloud_backup";

    public static final int ENCRYPTED_PAYLOAD_FORMAT_VERSION =
            1;

    public static final String ASSOCIATED_DATA_SCHEME =
            "firebase_uid_and_backup_id_v1";

    private static final int MAX_ENCODED_CHARACTERS =
            CloudBackupPayloadBuilder.MAX_CHUNK_COUNT
                    * CloudBackupPayloadBuilder
                    .CHUNK_CHARACTER_LIMIT;

    private final Context applicationContext;

    public EncryptedCloudBackupPayloadBuilder(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();
    }

    /**
     * Creates an encrypted Firestore-ready payload.
     *
     * This method performs database, compression and
     * cryptographic operations. Always call it from a
     * background executor.
     *
     * The caller should also clear its original
     * passphrase char[] after this method returns.
     */
    @NonNull
    public EncryptedCloudBackupPayload build(
            @NonNull String firebaseUserId,
            @NonNull char[] passphrase
    ) throws EncryptedPayloadBuildException {

        String safeUserId =
                firebaseUserId.trim();

        if (safeUserId.isEmpty()) {
            throw new EncryptedPayloadBuildException(
                    "Firebase user ID is unavailable."
            );
        }

        char[] passphraseCopy =
                Arrays.copyOf(
                        passphrase,
                        passphrase.length
                );

        byte[] compressedBackupBytes = null;
        byte[] encryptedBackupBytes = null;

        try {
            CloudBackupPayloadBuilder
                    .CloudBackupPayload sourcePayload =
                    new CloudBackupPayloadBuilder(
                            applicationContext
                    ).build();

            String compressedBase64 =
                    joinSourceChunks(
                            sourcePayload.getChunks(),
                            sourcePayload
                                    .getEncodedCharacters()
                    );

            compressedBackupBytes =
                    decodeCompressedBackup(
                            compressedBase64
                    );

            validateSourceCompressedPayload(
                    sourcePayload,
                    compressedBackupBytes
            );

            String associatedData =
                    CloudBackupEncryption
                            .createAssociatedData(
                                    safeUserId,
                                    sourcePayload
                                            .getBackupId()
                            );

            CloudBackupEncryption.EncryptedPayload
                    encryptedPayload =
                    CloudBackupEncryption.encrypt(
                            compressedBackupBytes,
                            passphraseCopy,
                            associatedData
                    );

            encryptedBackupBytes =
                    encryptedPayload
                            .getEncryptedBytes();

            String encryptedBase64 =
                    Base64.encodeToString(
                            encryptedBackupBytes,
                            Base64.NO_WRAP
                    );

            if (encryptedBase64.isEmpty()) {
                throw new EncryptedPayloadBuildException(
                        "Encrypted cloud payload is empty."
                );
            }

            if (encryptedBase64.length()
                    > MAX_ENCODED_CHARACTERS) {

                throw new EncryptedPayloadBuildException(
                        "Encrypted cloud payload exceeds "
                                + "the supported Firestore size."
                );
            }

            List<String> encryptedChunks =
                    splitIntoChunks(
                            encryptedBase64
                    );

            if (encryptedChunks.size()
                    > CloudBackupPayloadBuilder
                    .MAX_CHUNK_COUNT) {

                throw new EncryptedPayloadBuildException(
                        "Encrypted cloud backup requires "
                                + encryptedChunks.size()
                                + " chunks. Maximum supported "
                                + "chunk count is "
                                + CloudBackupPayloadBuilder
                                .MAX_CHUNK_COUNT
                                + "."
                );
            }

            String encryptedChecksum =
                    createSha256Checksum(
                            encryptedBackupBytes
                    );

            CloudBackupPayloadBuilder.BackupOverview
                    overview =
                    sourcePayload.getOverview();

            return new EncryptedCloudBackupPayload(
                    safeUserId,
                    sourcePayload.getBackupId(),
                    sourcePayload.getCreatedAt(),
                    sourcePayload.getBackupFormat(),
                    sourcePayload.getBackupFormatVersion(),
                    sourcePayload.getDatabaseSchemaVersion(),
                    ENCRYPTED_PAYLOAD_FORMAT,
                    ENCRYPTED_PAYLOAD_FORMAT_VERSION,
                    sourcePayload.getCompressionType(),
                    sourcePayload.getEncodingType(),
                    ASSOCIATED_DATA_SCHEME,
                    encryptedPayload
                            .getEncryptionVersion(),
                    encryptedPayload
                            .getCipherTransformation(),
                    encryptedPayload
                            .getKdfAlgorithm(),
                    encryptedPayload
                            .getKdfIterations(),
                    encryptedPayload
                            .getKeyLengthBits(),
                    encryptedPayload
                            .getGcmTagLengthBits(),
                    encryptedPayload
                            .getSaltBase64(),
                    encryptedPayload
                            .getInitializationVectorBase64(),
                    sourcePayload
                            .getChecksumSha256(),
                    encryptedChecksum,
                    sourcePayload
                            .getRawJsonBytes(),
                    sourcePayload
                            .getCompressedBytes(),
                    encryptedPayload
                            .getEncryptedByteCount(),
                    encryptedBase64.length(),
                    encryptedChunks,
                    overview
            );

        } catch (EncryptedPayloadBuildException exception) {
            throw exception;

        } catch (CloudBackupEncryption
                         .CloudEncryptionException exception) {

            throw new EncryptedPayloadBuildException(
                    exception.getMessage() == null
                            ? "Cloud backup encryption failed."
                            : exception.getMessage(),
                    exception
            );

        } catch (Exception exception) {
            throw new EncryptedPayloadBuildException(
                    "Encrypted cloud backup payload "
                            + "could not be prepared.",
                    exception
            );

        } finally {
            clearCharArray(
                    passphraseCopy
            );

            clearByteArray(
                    compressedBackupBytes
            );

            clearByteArray(
                    encryptedBackupBytes
            );
        }
    }

    @NonNull
    private String joinSourceChunks(
            @NonNull List<String> sourceChunks,
            int expectedCharacterCount
    ) throws EncryptedPayloadBuildException {

        if (sourceChunks.isEmpty()) {
            throw new EncryptedPayloadBuildException(
                    "Compressed cloud backup chunks "
                            + "are unavailable."
            );
        }

        if (sourceChunks.size()
                > CloudBackupPayloadBuilder
                .MAX_CHUNK_COUNT) {

            throw new EncryptedPayloadBuildException(
                    "Compressed cloud backup has an "
                            + "invalid chunk count."
            );
        }

        StringBuilder payloadBuilder =
                new StringBuilder(
                        Math.max(
                                expectedCharacterCount,
                                0
                        )
                );

        for (String sourceChunk
                : sourceChunks) {

            if (sourceChunk == null
                    || sourceChunk.isEmpty()) {

                throw new EncryptedPayloadBuildException(
                        "Compressed cloud backup contains "
                                + "an empty chunk."
                );
            }

            if (sourceChunk.length()
                    > CloudBackupPayloadBuilder
                    .CHUNK_CHARACTER_LIMIT) {

                throw new EncryptedPayloadBuildException(
                        "Compressed cloud backup chunk "
                                + "exceeds the supported size."
                );
            }

            if (payloadBuilder.length()
                    + sourceChunk.length()
                    > MAX_ENCODED_CHARACTERS) {

                throw new EncryptedPayloadBuildException(
                        "Compressed cloud backup payload "
                                + "exceeds the supported size."
                );
            }

            payloadBuilder.append(
                    sourceChunk
            );
        }

        if (payloadBuilder.length()
                != expectedCharacterCount) {

            throw new EncryptedPayloadBuildException(
                    "Compressed cloud backup length "
                            + "does not match its metadata."
            );
        }

        return payloadBuilder.toString();
    }

    @NonNull
    private byte[] decodeCompressedBackup(
            @NonNull String compressedBase64
    ) throws EncryptedPayloadBuildException {

        try {
            return Base64.decode(
                    compressedBase64,
                    Base64.NO_WRAP
            );

        } catch (IllegalArgumentException exception) {
            throw new EncryptedPayloadBuildException(
                    "Compressed cloud backup contains "
                            + "invalid Base64 data.",
                    exception
            );
        }
    }

    private void validateSourceCompressedPayload(
            @NonNull CloudBackupPayloadBuilder
                    .CloudBackupPayload sourcePayload,
            @NonNull byte[] compressedBackupBytes
    ) throws EncryptedPayloadBuildException {

        if (compressedBackupBytes.length <= 0) {
            throw new EncryptedPayloadBuildException(
                    "Compressed cloud backup is empty."
            );
        }

        if (compressedBackupBytes.length
                != sourcePayload
                .getCompressedBytes()) {

            throw new EncryptedPayloadBuildException(
                    "Compressed cloud backup size "
                            + "does not match its metadata."
            );
        }

        String actualChecksum;

        try {
            actualChecksum =
                    createSha256Checksum(
                            compressedBackupBytes
                    );

        } catch (IOException exception) {
            throw new EncryptedPayloadBuildException(
                    "Compressed cloud backup checksum "
                            + "could not be calculated.",
                    exception
            );
        }

        if (!sourcePayload
                .getChecksumSha256()
                .equalsIgnoreCase(
                        actualChecksum
                )) {

            throw new EncryptedPayloadBuildException(
                    "Compressed cloud backup checksum "
                            + "validation failed."
            );
        }
    }

    @NonNull
    private List<String> splitIntoChunks(
            @NonNull String encryptedBase64
    ) {
        List<String> chunks =
                new ArrayList<>();

        int startIndex = 0;

        while (startIndex
                < encryptedBase64.length()) {

            int endIndex =
                    Math.min(
                            startIndex
                                    + CloudBackupPayloadBuilder
                                    .CHUNK_CHARACTER_LIMIT,
                            encryptedBase64.length()
                    );

            chunks.add(
                    encryptedBase64.substring(
                            startIndex,
                            endIndex
                    )
            );

            startIndex = endIndex;
        }

        return chunks;
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

            clearByteArray(
                    checksumBytes
            );

            return checksumBuilder.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IOException(
                    "SHA-256 is unavailable.",
                    exception
            );
        }
    }

    private void clearCharArray(
            char[] characterArray
    ) {
        if (characterArray == null) {
            return;
        }

        Arrays.fill(
                characterArray,
                '\0'
        );
    }

    private void clearByteArray(
            byte[] byteArray
    ) {
        if (byteArray == null) {
            return;
        }

        Arrays.fill(
                byteArray,
                (byte) 0
        );
    }

    public static final class
    EncryptedCloudBackupPayload {

        private final String ownerUserId;

        private final String backupId;

        private final long createdAt;

        private final String backupFormat;

        private final int backupFormatVersion;

        private final int databaseSchemaVersion;

        private final String encryptedPayloadFormat;

        private final int encryptedPayloadFormatVersion;

        private final String innerCompression;

        private final String outerEncoding;

        private final String associatedDataScheme;

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

        private final int rawJsonBytes;

        private final int compressedPlaintextBytes;

        private final int encryptedBytes;

        private final int encodedCharacters;

        private final List<String> chunks;

        private final CloudBackupPayloadBuilder
                .BackupOverview overview;

        private EncryptedCloudBackupPayload(
                @NonNull String ownerUserId,
                @NonNull String backupId,
                long createdAt,
                @NonNull String backupFormat,
                int backupFormatVersion,
                int databaseSchemaVersion,
                @NonNull String encryptedPayloadFormat,
                int encryptedPayloadFormatVersion,
                @NonNull String innerCompression,
                @NonNull String outerEncoding,
                @NonNull String associatedDataScheme,
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
                int rawJsonBytes,
                int compressedPlaintextBytes,
                int encryptedBytes,
                int encodedCharacters,
                @NonNull List<String> chunks,
                @NonNull CloudBackupPayloadBuilder
                        .BackupOverview overview
        ) {
            this.ownerUserId =
                    ownerUserId;

            this.backupId =
                    backupId;

            this.createdAt =
                    createdAt;

            this.backupFormat =
                    backupFormat;

            this.backupFormatVersion =
                    backupFormatVersion;

            this.databaseSchemaVersion =
                    databaseSchemaVersion;

            this.encryptedPayloadFormat =
                    encryptedPayloadFormat;

            this.encryptedPayloadFormatVersion =
                    encryptedPayloadFormatVersion;

            this.innerCompression =
                    innerCompression;

            this.outerEncoding =
                    outerEncoding;

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

            this.plaintextChecksumSha256 =
                    plaintextChecksumSha256;

            this.encryptedChecksumSha256 =
                    encryptedChecksumSha256;

            this.rawJsonBytes =
                    rawJsonBytes;

            this.compressedPlaintextBytes =
                    compressedPlaintextBytes;

            this.encryptedBytes =
                    encryptedBytes;

            this.encodedCharacters =
                    encodedCharacters;

            this.chunks =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    chunks
                            )
                    );

            this.overview =
                    overview;
        }

        @NonNull
        public String getOwnerUserId() {
            return ownerUserId;
        }

        @NonNull
        public String getBackupId() {
            return backupId;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        @NonNull
        public String getBackupFormat() {
            return backupFormat;
        }

        public int getBackupFormatVersion() {
            return backupFormatVersion;
        }

        public int getDatabaseSchemaVersion() {
            return databaseSchemaVersion;
        }

        @NonNull
        public String getEncryptedPayloadFormat() {
            return encryptedPayloadFormat;
        }

        public int getEncryptedPayloadFormatVersion() {
            return encryptedPayloadFormatVersion;
        }

        @NonNull
        public String getInnerCompression() {
            return innerCompression;
        }

        @NonNull
        public String getOuterEncoding() {
            return outerEncoding;
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

        @NonNull
        public String getPlaintextChecksumSha256() {
            return plaintextChecksumSha256;
        }

        @NonNull
        public String getEncryptedChecksumSha256() {
            return encryptedChecksumSha256;
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

        public int getEncodedCharacters() {
            return encodedCharacters;
        }

        public int getChunkCount() {
            return chunks.size();
        }

        @NonNull
        public List<String> getChunks() {
            return chunks;
        }

        @NonNull
        public CloudBackupPayloadBuilder
                .BackupOverview getOverview() {

            return overview;
        }
    }

    public static final class
    EncryptedPayloadBuildException
            extends Exception {

        public EncryptedPayloadBuildException(
                @NonNull String message
        ) {
            super(message);
        }

        public EncryptedPayloadBuildException(
                @NonNull String message,
                @NonNull Throwable cause
        ) {
            super(
                    message,
                    cause
            );
        }
    }
}