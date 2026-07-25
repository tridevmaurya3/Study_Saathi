package com.tridev.studysaathi.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CloudRestoreCacheSecurityManager {

    private static final String CACHE_DIRECTORY_NAME =
            "cloud_restore_cache";

    private static final String MANAGED_FILE_PREFIX =
            "StudySaathi_Cloud_Backup_";

    private static final String BACKUP_FILE_EXTENSION =
            ".json";

    private static final String TEMP_FILE_EXTENSION =
            ".tmp";

    /*
     * A prepared decrypted restore file should not
     * remain indefinitely in app cache.
     *
     * Six hours gives enough time to review and restore
     * while reducing long-term plaintext exposure.
     */
    private static final long MAX_BACKUP_FILE_AGE_MILLIS =
            6L * 60L * 60L * 1000L;

    /*
     * Temporary files are expected to exist only while
     * a backup is being written. Anything older than
     * fifteen minutes is treated as abandoned.
     */
    private static final long MAX_TEMP_FILE_AGE_MILLIS =
            15L * 60L * 1000L;

    private static final ExecutorService cleanupExecutor =
            Executors.newSingleThreadExecutor();

    private final Context applicationContext;

    private final Handler mainThreadHandler;

    public CloudRestoreCacheSecurityManager(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Removes abandoned temporary files and expired
     * decrypted restore files.
     *
     * The active file, when supplied, is preserved.
     */
    public void cleanupExpiredFilesAsync(
            @Nullable String activeBackupPath,
            @Nullable CleanupCallback callback
    ) {
        cleanupExecutor.execute(() -> {
            CleanupResult cleanupResult;

            try {
                cleanupResult =
                        cleanupExpiredFiles(
                                activeBackupPath
                        );

            } catch (Exception exception) {
                dispatchError(
                        callback,
                        exception
                );

                return;
            }

            dispatchSuccess(
                    callback,
                    cleanupResult
            );
        });
    }

    /**
     * Deletes every managed cloud restore cache file.
     *
     * Use this after sign-out, account change or an
     * explicit security cleanup.
     */
    public void clearAllManagedFilesAsync(
            @Nullable CleanupCallback callback
    ) {
        cleanupExecutor.execute(() -> {
            CleanupResult cleanupResult;

            try {
                cleanupResult =
                        clearAllManagedFiles();

            } catch (Exception exception) {
                dispatchError(
                        callback,
                        exception
                );

                return;
            }

            dispatchSuccess(
                    callback,
                    cleanupResult
            );
        });
    }

    /**
     * Deletes one managed prepared backup.
     *
     * A path outside Study Saathi's private managed
     * cache is rejected.
     */
    public boolean deleteManagedBackup(
            @NonNull String absoluteFilePath
    ) {
        File targetFile =
                new File(
                        absoluteFilePath
                );

        if (!isManagedBackupPath(
                targetFile
        )) {
            return false;
        }

        return secureDeleteFile(
                targetFile
        );
    }

    /**
     * Confirms that an existing JSON file belongs to
     * the managed private cloud restore cache.
     */
    public boolean isManagedBackupFile(
            @NonNull File file
    ) {
        return file.isFile()
                && file.getName().endsWith(
                BACKUP_FILE_EXTENSION
        )
                && isManagedBackupPath(
                file
        );
    }

    /**
     * Confirms that a path belongs to Study Saathi's
     * managed cloud restore directory.
     *
     * The file does not need to exist.
     */
    public boolean isManagedBackupPath(
            @NonNull File file
    ) {
        try {
            File managedDirectory =
                    getManagedCacheDirectory()
                            .getCanonicalFile();

            File canonicalFile =
                    file.getCanonicalFile();

            String managedDirectoryPath =
                    managedDirectory.getPath()
                            + File.separator;

            String filePath =
                    canonicalFile.getPath();

            String fileName =
                    canonicalFile.getName();

            boolean validManagedName =
                    fileName.startsWith(
                            MANAGED_FILE_PREFIX
                    )
                            && (
                            fileName.endsWith(
                                    BACKUP_FILE_EXTENSION
                            )
                                    || fileName.endsWith(
                                    TEMP_FILE_EXTENSION
                            )
                    );

            return filePath.startsWith(
                    managedDirectoryPath
            )
                    && validManagedName;

        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Returns true only for an existing managed
     * temporary file.
     */
    public boolean isManagedTemporaryFile(
            @NonNull File file
    ) {
        return file.isFile()
                && file.getName().endsWith(
                TEMP_FILE_EXTENSION
        )
                && isManagedBackupPath(
                file
        );
    }

    @NonNull
    private CleanupResult cleanupExpiredFiles(
            @Nullable String activeBackupPath
    ) throws IOException {

        File cacheDirectory =
                getManagedCacheDirectory();

        if (!cacheDirectory.exists()) {
            return new CleanupResult(
                    0,
                    0,
                    0,
                    0L
            );
        }

        if (!cacheDirectory.isDirectory()) {
            throw new IOException(
                    "Cloud restore cache path is "
                            + "not a directory."
            );
        }

        File activeBackupFile =
                resolveActiveManagedFile(
                        activeBackupPath
                );

        File[] cachedFiles =
                cacheDirectory.listFiles();

        if (cachedFiles == null) {
            throw new IOException(
                    "Cloud restore cache could not "
                            + "be inspected."
            );
        }

        long currentTime =
                System.currentTimeMillis();

        int scannedFileCount = 0;
        int deletedBackupCount = 0;
        int deletedTempCount = 0;
        long releasedBytes = 0L;

        for (File cachedFile : cachedFiles) {
            if (!cachedFile.isFile()) {
                continue;
            }

            if (!isManagedBackupPath(
                    cachedFile
            )) {
                continue;
            }

            scannedFileCount++;

            if (activeBackupFile != null
                    && sameCanonicalFile(
                    cachedFile,
                    activeBackupFile
            )) {
                continue;
            }

            long fileAge =
                    calculateFileAge(
                            cachedFile,
                            currentTime
                    );

            boolean shouldDeleteBackup =
                    isManagedBackupFile(
                            cachedFile
                    )
                            && fileAge
                            > MAX_BACKUP_FILE_AGE_MILLIS;

            boolean shouldDeleteTemporary =
                    isManagedTemporaryFile(
                            cachedFile
                    )
                            && fileAge
                            > MAX_TEMP_FILE_AGE_MILLIS;

            if (!shouldDeleteBackup
                    && !shouldDeleteTemporary) {
                continue;
            }

            long fileSize =
                    Math.max(
                            cachedFile.length(),
                            0L
                    );

            if (secureDeleteFile(
                    cachedFile
            )) {
                releasedBytes +=
                        fileSize;

                if (shouldDeleteTemporary) {
                    deletedTempCount++;
                } else {
                    deletedBackupCount++;
                }
            }
        }

        removeDirectoryWhenEmpty(
                cacheDirectory
        );

        return new CleanupResult(
                scannedFileCount,
                deletedBackupCount,
                deletedTempCount,
                releasedBytes
        );
    }

    @NonNull
    private CleanupResult clearAllManagedFiles()
            throws IOException {

        File cacheDirectory =
                getManagedCacheDirectory();

        if (!cacheDirectory.exists()) {
            return new CleanupResult(
                    0,
                    0,
                    0,
                    0L
            );
        }

        if (!cacheDirectory.isDirectory()) {
            throw new IOException(
                    "Cloud restore cache path is "
                            + "not a directory."
            );
        }

        File[] cachedFiles =
                cacheDirectory.listFiles();

        if (cachedFiles == null) {
            throw new IOException(
                    "Cloud restore cache could not "
                            + "be inspected."
            );
        }

        int scannedFileCount = 0;
        int deletedBackupCount = 0;
        int deletedTempCount = 0;
        long releasedBytes = 0L;

        for (File cachedFile : cachedFiles) {
            if (!cachedFile.isFile()) {
                continue;
            }

            if (!isManagedBackupPath(
                    cachedFile
            )) {
                continue;
            }

            scannedFileCount++;

            boolean backupFile =
                    isManagedBackupFile(
                            cachedFile
                    );

            boolean temporaryFile =
                    isManagedTemporaryFile(
                            cachedFile
                    );

            if (!backupFile
                    && !temporaryFile) {
                continue;
            }

            long fileSize =
                    Math.max(
                            cachedFile.length(),
                            0L
                    );

            if (secureDeleteFile(
                    cachedFile
            )) {
                releasedBytes +=
                        fileSize;

                if (temporaryFile) {
                    deletedTempCount++;
                } else {
                    deletedBackupCount++;
                }
            }
        }

        removeDirectoryWhenEmpty(
                cacheDirectory
        );

        return new CleanupResult(
                scannedFileCount,
                deletedBackupCount,
                deletedTempCount,
                releasedBytes
        );
    }

    @Nullable
    private File resolveActiveManagedFile(
            @Nullable String activeBackupPath
    ) {
        if (activeBackupPath == null
                || activeBackupPath
                .trim()
                .isEmpty()) {

            return null;
        }

        File activeFile =
                new File(
                        activeBackupPath
                );

        if (!isManagedBackupPath(
                activeFile
        )) {
            return null;
        }

        return activeFile;
    }

    private long calculateFileAge(
            @NonNull File file,
            long currentTime
    ) {
        long lastModified =
                file.lastModified();

        if (lastModified <= 0L
                || lastModified > currentTime) {

            return Long.MAX_VALUE;
        }

        return currentTime
                - lastModified;
    }

    private boolean sameCanonicalFile(
            @NonNull File firstFile,
            @NonNull File secondFile
    ) {
        try {
            return firstFile
                    .getCanonicalFile()
                    .equals(
                            secondFile
                                    .getCanonicalFile()
                    );

        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Java and Android do not guarantee physical
     * overwrite behavior on flash storage.
     *
     * The file is therefore truncated first and then
     * deleted as a best-effort reduction of residual
     * plaintext exposure.
     */
    private boolean secureDeleteFile(
            @NonNull File file
    ) {
        if (!file.exists()) {
            return true;
        }

        if (!file.isFile()
                || !isManagedBackupPath(
                file
        )) {
            return false;
        }

        try {
            java.io.FileOutputStream outputStream =
                    new java.io.FileOutputStream(
                            file,
                            false
                    );

            try {
                outputStream.getChannel()
                        .truncate(0L);

                outputStream.flush();

                outputStream.getFD()
                        .sync();

            } finally {
                outputStream.close();
            }

        } catch (IOException ignored) {
            /*
             * Continue with normal deletion. Cache files
             * may still be deletable even when truncation
             * cannot be completed.
             */
        }

        return !file.exists()
                || file.delete();
    }

    private void removeDirectoryWhenEmpty(
            @NonNull File directory
    ) {
        File[] remainingFiles =
                directory.listFiles();

        if (remainingFiles != null
                && remainingFiles.length == 0) {

            directory.delete();
        }
    }

    @NonNull
    private File getManagedCacheDirectory() {
        return new File(
                applicationContext.getCacheDir(),
                CACHE_DIRECTORY_NAME
        );
    }

    private void dispatchSuccess(
            @Nullable CleanupCallback callback,
            @NonNull CleanupResult result
    ) {
        if (callback == null) {
            return;
        }

        mainThreadHandler.post(() ->
                callback.onCleanupCompleted(
                        result
                )
        );
    }

    private void dispatchError(
            @Nullable CleanupCallback callback,
            @NonNull Exception exception
    ) {
        if (callback == null) {
            return;
        }

        mainThreadHandler.post(() ->
                callback.onCleanupFailed(
                        exception
                )
        );
    }

    public interface CleanupCallback {

        void onCleanupCompleted(
                @NonNull CleanupResult result
        );

        void onCleanupFailed(
                @NonNull Exception exception
        );
    }

    public static final class CleanupResult {

        private final int scannedFileCount;

        private final int deletedBackupCount;

        private final int deletedTemporaryCount;

        private final long releasedBytes;

        private CleanupResult(
                int scannedFileCount,
                int deletedBackupCount,
                int deletedTemporaryCount,
                long releasedBytes
        ) {
            this.scannedFileCount =
                    scannedFileCount;

            this.deletedBackupCount =
                    deletedBackupCount;

            this.deletedTemporaryCount =
                    deletedTemporaryCount;

            this.releasedBytes =
                    releasedBytes;
        }

        public int getScannedFileCount() {
            return scannedFileCount;
        }

        public int getDeletedBackupCount() {
            return deletedBackupCount;
        }

        public int getDeletedTemporaryCount() {
            return deletedTemporaryCount;
        }

        public int getTotalDeletedCount() {
            return deletedBackupCount
                    + deletedTemporaryCount;
        }

        public long getReleasedBytes() {
            return releasedBytes;
        }
    }
}