package com.tridev.studysaathi.cloud;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class CloudBackupEncryption {

    public static final int ENCRYPTION_VERSION = 1;

    public static final String CIPHER_TRANSFORMATION =
            "AES/GCM/NoPadding";

    public static final String KEY_ALGORITHM =
            "AES";

    public static final String KDF_ALGORITHM =
            "PBKDF2WithHmacSHA256";

    public static final int KEY_LENGTH_BITS =
            256;

    public static final int GCM_TAG_LENGTH_BITS =
            128;

    public static final int KDF_ITERATIONS =
            210_000;

    public static final int SALT_LENGTH_BYTES =
            16;

    public static final int IV_LENGTH_BYTES =
            12;

    private static final int MINIMUM_PASSPHRASE_LENGTH =
            8;

    private static final int MAXIMUM_PASSPHRASE_LENGTH =
            128;

    private static final int MAX_PLAINTEXT_BYTES =
            25 * 1024 * 1024;

    private static final int MAX_ENCRYPTED_BYTES =
            MAX_PLAINTEXT_BYTES + 1024;

    private static final int MAX_ASSOCIATED_DATA_LENGTH =
            1024;

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private CloudBackupEncryption() {
        // Utility class.
    }

    /**
     * Encrypts cloud backup bytes with a passphrase.
     *
     * The caller should clear its own passphrase char[]
     * after this method returns.
     */
    @NonNull
    public static EncryptedPayload encrypt(
            @NonNull byte[] plaintextBytes,
            @NonNull char[] passphrase,
            @NonNull String associatedData
    ) throws CloudEncryptionException {

        validatePlaintext(
                plaintextBytes
        );

        validatePassphrase(
                passphrase
        );

        validateAssociatedData(
                associatedData
        );

        byte[] salt =
                createRandomBytes(
                        SALT_LENGTH_BYTES
                );

        byte[] initializationVector =
                createRandomBytes(
                        IV_LENGTH_BYTES
                );

        byte[] derivedKeyBytes = null;

        try {
            derivedKeyBytes =
                    deriveKeyBytes(
                            passphrase,
                            salt,
                            KDF_ITERATIONS
                    );

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            derivedKeyBytes,
                            KEY_ALGORITHM
                    );

            Cipher cipher =
                    Cipher.getInstance(
                            CIPHER_TRANSFORMATION
                    );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH_BITS,
                            initializationVector
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            cipher.updateAAD(
                    associatedData.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            byte[] encryptedBytes =
                    cipher.doFinal(
                            plaintextBytes
                    );

            if (encryptedBytes.length <= 0
                    || encryptedBytes.length
                    > MAX_ENCRYPTED_BYTES) {

                throw new CloudEncryptionException(
                        "Encrypted cloud backup size "
                                + "is invalid."
                );
            }

            return new EncryptedPayload(
                    ENCRYPTION_VERSION,
                    CIPHER_TRANSFORMATION,
                    KDF_ALGORITHM,
                    KDF_ITERATIONS,
                    KEY_LENGTH_BITS,
                    GCM_TAG_LENGTH_BITS,
                    salt,
                    initializationVector,
                    encryptedBytes,
                    plaintextBytes.length
            );

        } catch (CloudEncryptionException exception) {
            throw exception;

        } catch (GeneralSecurityException exception) {
            throw new CloudEncryptionException(
                    "Cloud backup encryption failed.",
                    exception
            );

        } finally {
            clearByteArray(
                    derivedKeyBytes
            );
        }
    }

    /**
     * Decrypts an encrypted cloud backup.
     *
     * A wrong passphrase, changed associated data or
     * modified ciphertext will fail authentication.
     */
    @NonNull
    public static byte[] decrypt(
            @NonNull EncryptedPayload encryptedPayload,
            @NonNull char[] passphrase,
            @NonNull String associatedData
    ) throws CloudEncryptionException {

        validatePassphrase(
                passphrase
        );

        validateAssociatedData(
                associatedData
        );

        validateEncryptedPayload(
                encryptedPayload
        );

        byte[] derivedKeyBytes = null;

        try {
            derivedKeyBytes =
                    deriveKeyBytes(
                            passphrase,
                            encryptedPayload.getSalt(),
                            encryptedPayload
                                    .getKdfIterations()
                    );

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            derivedKeyBytes,
                            KEY_ALGORITHM
                    );

            Cipher cipher =
                    Cipher.getInstance(
                            encryptedPayload
                                    .getCipherTransformation()
                    );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            encryptedPayload
                                    .getGcmTagLengthBits(),
                            encryptedPayload
                                    .getInitializationVector()
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            cipher.updateAAD(
                    associatedData.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            byte[] decryptedBytes =
                    cipher.doFinal(
                            encryptedPayload
                                    .getEncryptedBytes()
                    );

            if (decryptedBytes.length <= 0
                    || decryptedBytes.length
                    > MAX_PLAINTEXT_BYTES) {

                clearByteArray(
                        decryptedBytes
                );

                throw new CloudEncryptionException(
                        "Decrypted cloud backup size "
                                + "is invalid."
                );
            }

            int expectedPlaintextBytes =
                    encryptedPayload
                            .getOriginalPlaintextBytes();

            if (expectedPlaintextBytes > 0
                    && decryptedBytes.length
                    != expectedPlaintextBytes) {

                clearByteArray(
                        decryptedBytes
                );

                throw new CloudEncryptionException(
                        "Decrypted cloud backup size "
                                + "does not match its metadata."
                );
            }

            return decryptedBytes;

        } catch (AEADBadTagException exception) {
            throw new InvalidPassphraseException(
                    "Cloud backup could not be unlocked. "
                            + "The passphrase may be incorrect "
                            + "or the backup may be corrupted.",
                    exception
            );

        } catch (CloudEncryptionException exception) {
            throw exception;

        } catch (GeneralSecurityException exception) {
            throw new CloudEncryptionException(
                    "Cloud backup decryption failed.",
                    exception
            );

        } finally {
            clearByteArray(
                    derivedKeyBytes
            );
        }
    }

    /**
     * Recreates encrypted payload metadata after reading
     * it from Firestore.
     */
    @NonNull
    public static EncryptedPayload fromBase64(
            int encryptionVersion,
            @NonNull String cipherTransformation,
            @NonNull String kdfAlgorithm,
            int kdfIterations,
            int keyLengthBits,
            int gcmTagLengthBits,
            @NonNull String saltBase64,
            @NonNull String initializationVectorBase64,
            @NonNull String encryptedPayloadBase64,
            int originalPlaintextBytes
    ) throws CloudEncryptionException {

        byte[] salt;
        byte[] initializationVector;
        byte[] encryptedBytes;

        try {
            salt =
                    Base64.decode(
                            saltBase64,
                            Base64.NO_WRAP
                    );

            initializationVector =
                    Base64.decode(
                            initializationVectorBase64,
                            Base64.NO_WRAP
                    );

            encryptedBytes =
                    Base64.decode(
                            encryptedPayloadBase64,
                            Base64.NO_WRAP
                    );

        } catch (IllegalArgumentException exception) {
            throw new CloudEncryptionException(
                    "Encrypted cloud backup contains "
                            + "invalid Base64 data.",
                    exception
            );
        }

        EncryptedPayload encryptedPayload =
                new EncryptedPayload(
                        encryptionVersion,
                        cipherTransformation,
                        kdfAlgorithm,
                        kdfIterations,
                        keyLengthBits,
                        gcmTagLengthBits,
                        salt,
                        initializationVector,
                        encryptedBytes,
                        originalPlaintextBytes
                );

        validateEncryptedPayload(
                encryptedPayload
        );

        return encryptedPayload;
    }

    /**
     * Creates deterministic associated data that binds
     * an encrypted backup to one Firebase account and
     * one backup ID.
     */
    @NonNull
    public static String createAssociatedData(
            @NonNull String userId,
            @NonNull String backupId
    ) throws CloudEncryptionException {

        String safeUserId =
                userId.trim();

        String safeBackupId =
                backupId.trim();

        if (safeUserId.isEmpty()) {
            throw new CloudEncryptionException(
                    "Cloud account UID is unavailable."
            );
        }

        if (safeBackupId.isEmpty()) {
            throw new CloudEncryptionException(
                    "Cloud backup ID is unavailable."
            );
        }

        String associatedData =
                "study_saathi"
                        + "|cloud_backup"
                        + "|encryption_v"
                        + ENCRYPTION_VERSION
                        + "|uid="
                        + safeUserId
                        + "|backup_id="
                        + safeBackupId;

        validateAssociatedData(
                associatedData
        );

        return associatedData;
    }

    @NonNull
    private static byte[] deriveKeyBytes(
            @NonNull char[] passphrase,
            @NonNull byte[] salt,
            int iterationCount
    ) throws GeneralSecurityException {

        char[] passphraseCopy =
                Arrays.copyOf(
                        passphrase,
                        passphrase.length
                );

        PBEKeySpec keySpec =
                new PBEKeySpec(
                        passphraseCopy,
                        salt,
                        iterationCount,
                        KEY_LENGTH_BITS
                );

        try {
            SecretKeyFactory secretKeyFactory =
                    SecretKeyFactory.getInstance(
                            KDF_ALGORITHM
                    );

            return secretKeyFactory
                    .generateSecret(
                            keySpec
                    )
                    .getEncoded();

        } finally {
            keySpec.clearPassword();

            Arrays.fill(
                    passphraseCopy,
                    '\0'
            );
        }
    }

    @NonNull
    private static byte[] createRandomBytes(
            int byteCount
    ) {
        byte[] randomBytes =
                new byte[byteCount];

        SECURE_RANDOM.nextBytes(
                randomBytes
        );

        return randomBytes;
    }

    private static void validatePlaintext(
            @NonNull byte[] plaintextBytes
    ) throws CloudEncryptionException {

        if (plaintextBytes.length <= 0) {
            throw new CloudEncryptionException(
                    "Cloud backup data is empty."
            );
        }

        if (plaintextBytes.length
                > MAX_PLAINTEXT_BYTES) {

            throw new CloudEncryptionException(
                    "Cloud backup data exceeds "
                            + "the supported 25 MB limit."
            );
        }
    }

    private static void validatePassphrase(
            @NonNull char[] passphrase
    ) throws CloudEncryptionException {

        if (passphrase.length
                < MINIMUM_PASSPHRASE_LENGTH) {

            throw new CloudEncryptionException(
                    "Cloud backup passphrase must "
                            + "contain at least "
                            + MINIMUM_PASSPHRASE_LENGTH
                            + " characters."
            );
        }

        if (passphrase.length
                > MAXIMUM_PASSPHRASE_LENGTH) {

            throw new CloudEncryptionException(
                    "Cloud backup passphrase exceeds "
                            + "the supported length."
            );
        }

        boolean containsVisibleCharacter =
                false;

        for (char character : passphrase) {
            if (!Character.isWhitespace(
                    character
            )) {
                containsVisibleCharacter =
                        true;

                break;
            }
        }

        if (!containsVisibleCharacter) {
            throw new CloudEncryptionException(
                    "Cloud backup passphrase cannot "
                            + "contain only spaces."
            );
        }
    }

    private static void validateAssociatedData(
            @NonNull String associatedData
    ) throws CloudEncryptionException {

        if (associatedData.trim().isEmpty()) {
            throw new CloudEncryptionException(
                    "Cloud backup associated data "
                            + "is unavailable."
            );
        }

        byte[] associatedDataBytes =
                associatedData.getBytes(
                        StandardCharsets.UTF_8
                );

        if (associatedDataBytes.length
                > MAX_ASSOCIATED_DATA_LENGTH) {

            throw new CloudEncryptionException(
                    "Cloud backup associated data "
                            + "exceeds the supported size."
            );
        }
    }

    private static void validateEncryptedPayload(
            @NonNull EncryptedPayload encryptedPayload
    ) throws CloudEncryptionException {

        if (encryptedPayload.getEncryptionVersion()
                != ENCRYPTION_VERSION) {

            throw new CloudEncryptionException(
                    "Unsupported cloud backup "
                            + "encryption version."
            );
        }

        if (!CIPHER_TRANSFORMATION.equals(
                encryptedPayload
                        .getCipherTransformation()
        )) {
            throw new CloudEncryptionException(
                    "Unsupported cloud backup cipher."
            );
        }

        if (!KDF_ALGORITHM.equals(
                encryptedPayload
                        .getKdfAlgorithm()
        )) {
            throw new CloudEncryptionException(
                    "Unsupported cloud backup "
                            + "key derivation algorithm."
            );
        }

        if (encryptedPayload.getKdfIterations()
                != KDF_ITERATIONS) {

            throw new CloudEncryptionException(
                    "Unsupported cloud backup "
                            + "key derivation settings."
            );
        }

        if (encryptedPayload.getKeyLengthBits()
                != KEY_LENGTH_BITS) {

            throw new CloudEncryptionException(
                    "Unsupported cloud backup key size."
            );
        }

        if (encryptedPayload.getGcmTagLengthBits()
                != GCM_TAG_LENGTH_BITS) {

            throw new CloudEncryptionException(
                    "Unsupported cloud backup "
                            + "authentication tag size."
            );
        }

        if (encryptedPayload.getSalt().length
                != SALT_LENGTH_BYTES) {

            throw new CloudEncryptionException(
                    "Cloud backup encryption salt "
                            + "is invalid."
            );
        }

        if (encryptedPayload
                .getInitializationVector()
                .length != IV_LENGTH_BYTES) {

            throw new CloudEncryptionException(
                    "Cloud backup initialization "
                            + "vector is invalid."
            );
        }

        int encryptedByteCount =
                encryptedPayload
                        .getEncryptedBytes()
                        .length;

        if (encryptedByteCount <= 0
                || encryptedByteCount
                > MAX_ENCRYPTED_BYTES) {

            throw new CloudEncryptionException(
                    "Encrypted cloud backup size "
                            + "is invalid."
            );
        }

        int originalByteCount =
                encryptedPayload
                        .getOriginalPlaintextBytes();

        if (originalByteCount <= 0
                || originalByteCount
                > MAX_PLAINTEXT_BYTES) {

            throw new CloudEncryptionException(
                    "Original cloud backup size "
                            + "is invalid."
            );
        }
    }

    private static void clearByteArray(
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

    public static final class EncryptedPayload {

        private final int encryptionVersion;

        private final String cipherTransformation;

        private final String kdfAlgorithm;

        private final int kdfIterations;

        private final int keyLengthBits;

        private final int gcmTagLengthBits;

        private final byte[] salt;

        private final byte[] initializationVector;

        private final byte[] encryptedBytes;

        private final int originalPlaintextBytes;

        private EncryptedPayload(
                int encryptionVersion,
                @NonNull String cipherTransformation,
                @NonNull String kdfAlgorithm,
                int kdfIterations,
                int keyLengthBits,
                int gcmTagLengthBits,
                @NonNull byte[] salt,
                @NonNull byte[] initializationVector,
                @NonNull byte[] encryptedBytes,
                int originalPlaintextBytes
        ) {
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

            this.salt =
                    Arrays.copyOf(
                            salt,
                            salt.length
                    );

            this.initializationVector =
                    Arrays.copyOf(
                            initializationVector,
                            initializationVector.length
                    );

            this.encryptedBytes =
                    Arrays.copyOf(
                            encryptedBytes,
                            encryptedBytes.length
                    );

            this.originalPlaintextBytes =
                    originalPlaintextBytes;
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
        public byte[] getSalt() {
            return Arrays.copyOf(
                    salt,
                    salt.length
            );
        }

        @NonNull
        public String getSaltBase64() {
            return Base64.encodeToString(
                    salt,
                    Base64.NO_WRAP
            );
        }

        @NonNull
        public byte[] getInitializationVector() {
            return Arrays.copyOf(
                    initializationVector,
                    initializationVector.length
            );
        }

        @NonNull
        public String getInitializationVectorBase64() {
            return Base64.encodeToString(
                    initializationVector,
                    Base64.NO_WRAP
            );
        }

        @NonNull
        public byte[] getEncryptedBytes() {
            return Arrays.copyOf(
                    encryptedBytes,
                    encryptedBytes.length
            );
        }

        @NonNull
        public String getEncryptedPayloadBase64() {
            return Base64.encodeToString(
                    encryptedBytes,
                    Base64.NO_WRAP
            );
        }

        public int getEncryptedByteCount() {
            return encryptedBytes.length;
        }

        public int getOriginalPlaintextBytes() {
            return originalPlaintextBytes;
        }
    }

    public static class CloudEncryptionException
            extends Exception {

        public CloudEncryptionException(
                @NonNull String message
        ) {
            super(message);
        }

        public CloudEncryptionException(
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
    InvalidPassphraseException
            extends CloudEncryptionException {

        public InvalidPassphraseException(
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