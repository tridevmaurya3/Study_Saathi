package com.tridev.studysaathi.cloud;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CloudBackupSecurityGuard {

    public static final String RESTORE_CACHE_FOLDER_NAME =
            "cloud_restore_cache";

    public static final String RESTORE_CACHE_FILE_PREFIX =
            "StudySaathi_Cloud_Backup_";

    private static final String JSON_FILE_EXTENSION =
            ".json";

    private static final String TEMP_FILE_EXTENSION =
            ".tmp";

    private static final String DECRYPTED_FILE_EXTENSION =
            ".decrypted";

    private static final String SHORT_DECRYPTED_FILE_EXTENSION =
            ".dec";

    private static final int FILE_OVERWRITE_BUFFER_SIZE =
            8 * 1024;

    private final Object securityLock =
            new Object();

    private final Set<String>
            registeredSensitiveFilePaths =
            new HashSet<>();

    private char[] activePassphrase;

    /**
     * Stores a temporary private copy of the supplied
     * passphrase.
     *
     * The supplied caller array is cleared immediately
     * after its contents have been copied.
     */
    public void captureAndClearCallerPassphrase(
            @NonNull char[] passphrase
    ) {
        if (passphrase.length == 0) {
            clearCharacters(
                    passphrase
            );

            throw new IllegalArgumentException(
                    "Cloud backup passphrase cannot be empty."
            );
        }

        char[] privateCopy =
                Arrays.copyOf(
                        passphrase,
                        passphrase.length
                );

        clearCharacters(
                passphrase
        );

        synchronized (securityLock) {
            clearActivePassphraseLocked();

            activePassphrase =
                    privateCopy;
        }
    }

    /**
     * Returns a new copy of the active passphrase and
     * immediately clears the internally stored copy.
     *
     * The caller must clear the returned array after
     * completing encryption or decryption.
     */
    @NonNull
    public char[] consumePassphrase() {
        synchronized (securityLock) {
            if (activePassphrase == null
                    || activePassphrase.length == 0) {

                throw new IllegalStateException(
                        "No active cloud backup passphrase "
                                + "is available."
                );
            }

            char[] passphraseCopy =
                    Arrays.copyOf(
                            activePassphrase,
                            activePassphrase.length
                    );

            clearActivePassphraseLocked();

            return passphraseCopy;
        }
    }

    public boolean hasActivePassphrase() {
        synchronized (securityLock) {
            return activePassphrase != null
                    && activePassphrase.length > 0;
        }
    }

    /**
     * Clears only passphrase memory.
     */
    public void clearPassphraseMemory() {
        synchronized (securityLock) {
            clearActivePassphraseLocked();
        }
    }

    /**
     * Registers a temporary decrypted or restore-cache
     * file so it can be deleted during security cleanup.
     */
    public void registerSensitiveFile(
            @NonNull Context context,
            @NonNull File sensitiveFile
    ) {
        Context applicationContext =
                context.getApplicationContext();

        if (!isManagedSensitiveFile(
                applicationContext,
                sensitiveFile
        )) {
            throw new IllegalArgumentException(
                    "The file is outside the managed "
                            + "cloud restore cache."
            );
        }

        try {
            String canonicalPath =
                    sensitiveFile.getCanonicalPath();

            synchronized (securityLock) {
                registeredSensitiveFilePaths.add(
                        canonicalPath
                );
            }

        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Unable to validate the sensitive "
                            + "cloud restore file.",
                    exception
            );
        }
    }

    /**
     * Removes a file from active tracking without
     * deleting it.
     *
     * This should only be used when another trusted
     * component has already securely removed the file.
     */
    public void unregisterSensitiveFile(
            @NonNull File sensitiveFile
    ) {
        try {
            String canonicalPath =
                    sensitiveFile.getCanonicalPath();

            synchronized (securityLock) {
                registeredSensitiveFilePaths.remove(
                        canonicalPath
                );
            }

        } catch (IOException ignored) {
            /*
             * The file cannot be safely identified.
             * A later full managed-cache cleanup will
             * still attempt to remove it.
             */
        }
    }

    /**
     * Call when the cloud activity goes into the
     * background.
     */
    @NonNull
    public CleanupResult clearForAppBackground(
            @NonNull Context context
    ) {
        return clearSensitiveState(
                context
        );
    }

    /**
     * Call when the cloud activity is permanently
     * closed or destroyed.
     */
    @NonNull
    public CleanupResult clearForActivityClosed(
            @NonNull Context context
    ) {
        return clearSensitiveState(
                context
        );
    }

    /**
     * Call before sign-out or whenever the active
     * Firebase account changes.
     */
    @NonNull
    public CleanupResult clearForAccountChange(
            @NonNull Context context
    ) {
        return clearSensitiveState(
                context
        );
    }

    /**
     * Clears temporary passphrase memory, registered
     * sensitive files and all managed cloud restore
     * cache files.
     */
    @NonNull
    public CleanupResult clearSensitiveState(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        boolean passphraseWasCleared;

        List<String> registeredPaths;

        synchronized (securityLock) {
            passphraseWasCleared =
                    activePassphrase != null;

            clearActivePassphraseLocked();

            registeredPaths =
                    new ArrayList<>(
                            registeredSensitiveFilePaths
                    );

            registeredSensitiveFilePaths.clear();
        }

        int deletedFileCount = 0;
        int failedFileCount = 0;

        Set<String> processedPaths =
                new HashSet<>();

        for (String registeredPath :
                registeredPaths) {

            File registeredFile =
                    new File(
                            registeredPath
                    );

            FileDeleteResult deleteResult =
                    securelyDeleteManagedFile(
                            applicationContext,
                            registeredFile
                    );

            if (deleteResult.fileWasProcessed) {
                processedPaths.add(
                        registeredPath
                );
            }

            if (deleteResult.deleted) {
                deletedFileCount++;

            } else if (deleteResult.failed) {
                failedFileCount++;
            }
        }

        File cacheDirectory =
                getManagedRestoreCacheDirectory(
                        applicationContext
                );

        File[] cacheFiles =
                cacheDirectory.listFiles();

        if (cacheFiles != null) {
            for (File cacheFile : cacheFiles) {
                String canonicalPath;

                try {
                    canonicalPath =
                            cacheFile.getCanonicalPath();

                } catch (IOException exception) {
                    failedFileCount++;
                    continue;
                }

                if (processedPaths.contains(
                        canonicalPath
                )) {
                    continue;
                }

                FileDeleteResult deleteResult =
                        securelyDeleteManagedFile(
                                applicationContext,
                                cacheFile
                        );

                if (deleteResult.deleted) {
                    deletedFileCount++;

                } else if (deleteResult.failed) {
                    failedFileCount++;
                }
            }
        }

        removeEmptyManagedCacheDirectory(
                cacheDirectory
        );

        return new CleanupResult(
                passphraseWasCleared,
                deletedFileCount,
                failedFileCount
        );
    }

    /**
     * Clears a caller-owned character array.
     */
    public static void clearCharacters(
            char[] characters
    ) {
        if (characters == null) {
            return;
        }

        Arrays.fill(
                characters,
                '\0'
        );
    }

    /**
     * Clears a caller-owned byte array.
     */
    public static void clearBytes(
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

    @NonNull
    public static File getManagedRestoreCacheDirectory(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        return new File(
                applicationContext.getCacheDir(),
                RESTORE_CACHE_FOLDER_NAME
        );
    }

    public static boolean isManagedSensitiveFile(
            @NonNull Context context,
            @NonNull File file
    ) {
        if (file.isDirectory()) {
            return false;
        }

        String fileName =
                file.getName();

        if (!fileName.startsWith(
                RESTORE_CACHE_FILE_PREFIX
        )) {
            return false;
        }

        if (!hasSupportedSensitiveExtension(
                fileName
        )) {
            return false;
        }

        try {
            File managedDirectory =
                    getManagedRestoreCacheDirectory(
                            context
                    );

            String managedDirectoryPath =
                    managedDirectory.getCanonicalPath();

            String filePath =
                    file.getCanonicalPath();

            return filePath.startsWith(
                    managedDirectoryPath
                            + File.separator
            );

        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean
    hasSupportedSensitiveExtension(
            @NonNull String fileName
    ) {
        String normalizedFileName =
                fileName.toLowerCase();

        return normalizedFileName.endsWith(
                JSON_FILE_EXTENSION
        )
                || normalizedFileName.endsWith(
                JSON_FILE_EXTENSION
                        + TEMP_FILE_EXTENSION
        )
                || normalizedFileName.endsWith(
                TEMP_FILE_EXTENSION
        )
                || normalizedFileName.endsWith(
                DECRYPTED_FILE_EXTENSION
        )
                || normalizedFileName.endsWith(
                SHORT_DECRYPTED_FILE_EXTENSION
        );
    }

    @NonNull
    private static FileDeleteResult
    securelyDeleteManagedFile(
            @NonNull Context context,
            @NonNull File file
    ) {
        if (!file.exists()) {
            return FileDeleteResult.notRequired();
        }

        if (!file.isFile()) {
            return FileDeleteResult.notManaged();
        }

        if (!isManagedSensitiveFile(
                context,
                file
        )) {
            return FileDeleteResult.notManaged();
        }

        boolean overwriteSucceeded =
                overwriteFileWithZeros(
                        file
                );

        boolean deleteSucceeded =
                file.delete();

        if (deleteSucceeded) {
            return FileDeleteResult.deleted();
        }

        /*
         * Some filesystems may refuse deletion while
         * another process still holds the file.
         * Mark it for deletion when possible.
         */
        file.deleteOnExit();

        return FileDeleteResult.failed(
                overwriteSucceeded
        );
    }

    private static boolean overwriteFileWithZeros(
            @NonNull File file
    ) {
        long originalLength =
                file.length();

        if (originalLength <= 0L) {
            return true;
        }

        byte[] zeroBuffer =
                new byte[
                        FILE_OVERWRITE_BUFFER_SIZE
                        ];

        RandomAccessFile randomAccessFile =
                null;

        try {
            randomAccessFile =
                    new RandomAccessFile(
                            file,
                            "rws"
                    );

            randomAccessFile.seek(
                    0L
            );

            long remainingBytes =
                    originalLength;

            while (remainingBytes > 0L) {
                int writeSize =
                        (int) Math.min(
                                zeroBuffer.length,
                                remainingBytes
                        );

                randomAccessFile.write(
                        zeroBuffer,
                        0,
                        writeSize
                );

                remainingBytes -=
                        writeSize;
            }

            randomAccessFile.setLength(
                    originalLength
            );

            randomAccessFile.getFD()
                    .sync();

            return true;

        } catch (IOException exception) {
            return false;

        } finally {
            Arrays.fill(
                    zeroBuffer,
                    (byte) 0
            );

            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();

                } catch (IOException ignored) {
                    /*
                     * Best-effort cleanup.
                     */
                }
            }
        }
    }

    private static void removeEmptyManagedCacheDirectory(
            @NonNull File cacheDirectory
    ) {
        if (!cacheDirectory.exists()
                || !cacheDirectory.isDirectory()) {

            return;
        }

        File[] remainingFiles =
                cacheDirectory.listFiles();

        if (remainingFiles != null
                && remainingFiles.length == 0) {

            cacheDirectory.delete();
        }
    }

    private void clearActivePassphraseLocked() {
        if (activePassphrase == null) {
            return;
        }

        clearCharacters(
                activePassphrase
        );

        activePassphrase =
                null;
    }

    public static final class CleanupResult {

        private final boolean passphraseCleared;

        private final int deletedFileCount;

        private final int failedFileCount;

        private CleanupResult(
                boolean passphraseCleared,
                int deletedFileCount,
                int failedFileCount
        ) {
            this.passphraseCleared =
                    passphraseCleared;

            this.deletedFileCount =
                    deletedFileCount;

            this.failedFileCount =
                    failedFileCount;
        }

        public boolean wasPassphraseCleared() {
            return passphraseCleared;
        }

        public int getDeletedFileCount() {
            return deletedFileCount;
        }

        public int getFailedFileCount() {
            return failedFileCount;
        }

        public boolean isFullySuccessful() {
            return failedFileCount == 0;
        }
    }

    private static final class FileDeleteResult {

        private final boolean fileWasProcessed;

        private final boolean deleted;

        private final boolean failed;

        private FileDeleteResult(
                boolean fileWasProcessed,
                boolean deleted,
                boolean failed
        ) {
            this.fileWasProcessed =
                    fileWasProcessed;

            this.deleted =
                    deleted;

            this.failed =
                    failed;
        }

        @NonNull
        private static FileDeleteResult deleted() {
            return new FileDeleteResult(
                    true,
                    true,
                    false
            );
        }

        @NonNull
        private static FileDeleteResult failed(
                boolean overwriteSucceeded
        ) {
            /*
             * overwriteSucceeded is intentionally
             * accepted so the result creation remains
             * explicit during security review.
             */
            return new FileDeleteResult(
                    true,
                    false,
                    true
            );
        }

        @NonNull
        private static FileDeleteResult notRequired() {
            return new FileDeleteResult(
                    true,
                    false,
                    false
            );
        }

        @NonNull
        private static FileDeleteResult notManaged() {
            return new FileDeleteResult(
                    false,
                    false,
                    false
            );
        }
    }
}