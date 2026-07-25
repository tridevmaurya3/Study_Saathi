package com.tridev.studysaathi.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CloudBackupRestoreCoordinator {

    private static final String CACHE_FOLDER_NAME =
            CloudBackupSecurityGuard
                    .RESTORE_CACHE_FOLDER_NAME;

    private static final String CACHE_FILE_PREFIX =
            CloudBackupSecurityGuard
                    .RESTORE_CACHE_FILE_PREFIX;

    private static final String CACHE_FILE_EXTENSION =
            ".json";

    private static final String TEMP_FILE_EXTENSION =
            ".tmp";

    private static final int MAX_BACKUP_BYTES =
            25 * 1024 * 1024;

    private static final int FILE_OVERWRITE_BUFFER_SIZE =
            8 * 1024;

    private static final ExecutorService
            cacheFileExecutor =
            Executors.newSingleThreadExecutor();

    private final Context applicationContext;

    private final Handler mainThreadHandler;

    private final CloudBackupDownloader
            cloudBackupDownloader;

    private final CloudBackupSecurityGuard
            cloudBackupSecurityGuard;

    public CloudBackupRestoreCoordinator(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );

        cloudBackupDownloader =
                new CloudBackupDownloader(
                        applicationContext
                );

        cloudBackupSecurityGuard =
                new CloudBackupSecurityGuard();
    }

    /**
     * Downloads the latest cloud backup, validates it
     * through CloudBackupDownloader and stores it in the
     * app's private cache directory.
     *
     * The callback is always delivered on the main
     * application thread.
     */
    public void downloadLatestBackupToCache(
            @NonNull FirebaseUser firebaseUser,
            @NonNull RestorePreparationCallback callback
    ) {
        if (!firebaseUser.isEmailVerified()) {
            dispatchError(
                    callback,
                    new CloudRestorePreparationException(
                            "Cloud restore requires a "
                                    + "verified email account."
                    )
            );

            return;
        }

        String expectedUserId =
                firebaseUser.getUid();

        if (expectedUserId.trim().isEmpty()) {
            dispatchError(
                    callback,
                    new CloudRestorePreparationException(
                            "Firebase user ID is unavailable."
                    )
            );

            return;
        }

        cloudBackupDownloader.downloadLatestBackup(
                firebaseUser,
                new CloudBackupDownloader
                        .DownloadCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull CloudBackupDownloader
                                    .CloudBackupDownloadResult result
                    ) {
                        writeValidatedBackupToCache(
                                expectedUserId,
                                result,
                                callback
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        dispatchError(
                                callback,
                                exception
                        );
                    }
                }
        );
    }

    private void writeValidatedBackupToCache(
            @NonNull String expectedUserId,
            @NonNull CloudBackupDownloader
                    .CloudBackupDownloadResult downloadResult,
            @NonNull RestorePreparationCallback callback
    ) {
        cacheFileExecutor.execute(() -> {
            File temporaryFile = null;
            File finalBackupFile = null;
            byte[] backupJsonBytes = null;

            try {
                verifyCurrentAccount(
                        expectedUserId
                );

                clearManagedCacheBeforePreparation();

                String backupJsonText =
                        downloadResult
                                .getBackupJson()
                                .toString();

                backupJsonBytes =
                        backupJsonText.getBytes(
                                StandardCharsets.UTF_8
                        );

                if (backupJsonBytes.length == 0) {
                    throw new CloudRestorePreparationException(
                            "Downloaded cloud backup is empty."
                    );
                }

                if (backupJsonBytes.length
                        > MAX_BACKUP_BYTES) {

                    throw new CloudRestorePreparationException(
                            "Downloaded cloud backup exceeds "
                                    + "the supported 25 MB limit."
                    );
                }

                File cacheDirectory =
                        prepareCacheDirectory();

                String safeBackupId =
                        createSafeBackupId(
                                downloadResult.getBackupId()
                        );

                String finalFileName =
                        CACHE_FILE_PREFIX
                                + safeBackupId
                                + CACHE_FILE_EXTENSION;

                finalBackupFile =
                        new File(
                                cacheDirectory,
                                finalFileName
                        );

                temporaryFile =
                        new File(
                                cacheDirectory,
                                finalFileName
                                        + TEMP_FILE_EXTENSION
                        );

                securelyDeleteManagedFileIfPresent(
                        temporaryFile
                );

                writeTemporaryBackupFile(
                        temporaryFile,
                        backupJsonText
                );

                verifyCurrentAccount(
                        expectedUserId
                );

                if (finalBackupFile.exists()
                        && !securelyDeleteManagedFileIfPresent(
                        finalBackupFile
                )) {

                    throw new IOException(
                            "Unable to securely replace the previous "
                                    + "cached cloud backup."
                    );
                }

                if (!temporaryFile.renameTo(
                        finalBackupFile
                )) {
                    throw new IOException(
                            "Unable to finalize the cached "
                                    + "cloud backup."
                    );
                }

                temporaryFile = null;

                if (!isManagedCacheFile(
                        finalBackupFile
                )) {
                    securelyDeleteManagedFileIfPresent(
                            finalBackupFile
                    );

                    throw new CloudRestorePreparationException(
                            "Cloud backup cache path "
                                    + "validation failed."
                    );
                }

                deleteOtherCacheFiles(
                        cacheDirectory,
                        finalBackupFile
                );

                verifyCurrentAccount(
                        expectedUserId
                );

                CloudRestorePreparationResult result =
                        new CloudRestorePreparationResult(
                                finalBackupFile
                                        .getAbsolutePath(),
                                finalBackupFile
                                        .getName(),
                                downloadResult
                                        .getBackupId(),
                                downloadResult
                                        .getBackupCreatedAt(),
                                downloadResult
                                        .getUploadedAt(),
                                downloadResult
                                        .getChecksumSha256(),
                                downloadResult
                                        .getChunkCount(),
                                downloadResult
                                        .getRawJsonBytes(),
                                downloadResult
                                        .getCompressedBytes(),
                                downloadResult
                                        .getProfileCount(),
                                downloadResult
                                        .getLessonProgressCount(),
                                downloadResult
                                        .getQuizAttemptCount(),
                                downloadResult
                                        .getDoubtCount(),
                                downloadResult
                                        .getPreferenceItemCount()
                        );

                dispatchSuccess(
                        callback,
                        result
                );

                finalBackupFile = null;

            } catch (Exception exception) {
                if (temporaryFile != null) {
                    securelyDeleteManagedFileIfPresent(
                            temporaryFile
                    );
                }

                if (finalBackupFile != null) {
                    securelyDeleteManagedFileIfPresent(
                            finalBackupFile
                    );
                }

                dispatchError(
                        callback,
                        exception
                );

            } finally {
                CloudBackupSecurityGuard.clearBytes(
                        backupJsonBytes
                );
            }
        });
    }

    private void verifyCurrentAccount(
            @NonNull String expectedUserId
    ) throws CloudRestorePreparationException {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (currentUser == null) {
            throw new CloudRestorePreparationException(
                    "Cloud account was signed out "
                            + "during restore preparation."
            );
        }

        if (!currentUser.isEmailVerified()) {
            throw new CloudRestorePreparationException(
                    "Cloud account is no longer verified."
            );
        }

        if (!expectedUserId.equals(
                currentUser.getUid()
        )) {
            throw new CloudRestorePreparationException(
                    "Cloud account changed during "
                            + "restore preparation."
            );
        }
    }

    /**
     * Clears any previous temporary restore files before
     * a new cloud backup is prepared.
     */
    private void clearManagedCacheBeforePreparation()
            throws IOException {

        CloudBackupSecurityGuard.CleanupResult
                cleanupResult =
                cloudBackupSecurityGuard
                        .clearSensitiveState(
                                applicationContext
                        );

        if (!cleanupResult.isFullySuccessful()) {
            throw new IOException(
                    "Unable to securely clear "
                            + cleanupResult
                            .getFailedFileCount()
                            + " previous cloud restore "
                            + "cache file(s)."
            );
        }
    }

    @NonNull
    private File prepareCacheDirectory()
            throws IOException {

        File cacheDirectory =
                getManagedCacheDirectory();

        if (!cacheDirectory.exists()
                && !cacheDirectory.mkdirs()) {

            throw new IOException(
                    "Unable to create the private "
                            + "cloud restore cache."
            );
        }

        if (!cacheDirectory.isDirectory()) {
            throw new IOException(
                    "Cloud restore cache path is "
                            + "not a directory."
            );
        }

        return cacheDirectory;
    }

    private void writeTemporaryBackupFile(
            @NonNull File temporaryFile,
            @NonNull String backupJsonText
    ) throws IOException {

        if (!isManagedCachePath(
                temporaryFile
        )) {
            throw new IOException(
                    "Temporary cloud backup path "
                            + "validation failed."
            );
        }

        try (FileOutputStream fileOutputStream =
                     new FileOutputStream(
                             temporaryFile,
                             false
                     );

             OutputStreamWriter writer =
                     new OutputStreamWriter(
                             fileOutputStream,
                             StandardCharsets.UTF_8
                     )) {

            writer.write(
                    backupJsonText
            );

            writer.flush();

            fileOutputStream
                    .getFD()
                    .sync();
        }

        if (!temporaryFile.exists()
                || temporaryFile.length() <= 0L) {

            throw new IOException(
                    "Temporary cloud backup file "
                            + "was not created correctly."
            );
        }

        if (temporaryFile.length()
                > MAX_BACKUP_BYTES) {

            securelyDeleteManagedFileIfPresent(
                    temporaryFile
            );

            throw new IOException(
                    "Temporary cloud backup file "
                            + "exceeds the supported limit."
            );
        }
    }

    @NonNull
    private String createSafeBackupId(
            @NonNull String backupId
    ) {
        String safeBackupId =
                backupId.trim()
                        .replaceAll(
                                "[^a-zA-Z0-9_-]",
                                "_"
                        );

        if (safeBackupId.isEmpty()) {
            safeBackupId =
                    String.format(
                            Locale.US,
                            "backup_%d",
                            System.currentTimeMillis()
                    );
        }

        if (safeBackupId.length() > 100) {
            safeBackupId =
                    safeBackupId.substring(
                            0,
                            100
                    );
        }

        return safeBackupId;
    }

    private void deleteOtherCacheFiles(
            @NonNull File cacheDirectory,
            @NonNull File fileToKeep
    ) throws IOException {

        File[] cachedFiles =
                cacheDirectory.listFiles();

        if (cachedFiles == null) {
            return;
        }

        String fileToKeepPath =
                fileToKeep.getCanonicalPath();

        for (File cachedFile : cachedFiles) {
            String cachedFilePath;

            try {
                cachedFilePath =
                        cachedFile.getCanonicalPath();

            } catch (IOException exception) {
                throw new IOException(
                        "Unable to validate a previous "
                                + "cloud restore cache file.",
                        exception
                );
            }

            if (fileToKeepPath.equals(
                    cachedFilePath
            )) {
                continue;
            }

            if (!cachedFile.isFile()) {
                continue;
            }

            if (!hasManagedCacheFileName(
                    cachedFile.getName()
            )) {
                continue;
            }

            if (!securelyDeleteManagedFileIfPresent(
                    cachedFile
            )) {
                throw new IOException(
                        "Unable to securely remove an old "
                                + "cloud restore cache file."
                );
            }
        }
    }

    /**
     * Securely deletes a previously prepared cloud
     * backup file.
     *
     * Files outside Study Saathi's private managed
     * restore-cache directory are rejected.
     */
    public boolean deletePreparedBackup(
            @NonNull String absoluteFilePath
    ) {
        if (absoluteFilePath.trim().isEmpty()) {
            return false;
        }

        File backupFile =
                new File(
                        absoluteFilePath
                );

        return securelyDeleteManagedFileIfPresent(
                backupFile
        );
    }

    /**
     * Confirms that an existing file belongs to Study
     * Saathi's private cloud-restore cache directory.
     */
    public boolean isManagedCacheFile(
            @NonNull File backupFile
    ) {
        return backupFile.exists()
                && backupFile.isFile()
                && isManagedCachePath(
                backupFile
        );
    }

    /**
     * Validates the canonical location and permitted
     * filename even when the target file does not exist.
     */
    private boolean isManagedCachePath(
            @NonNull File backupFile
    ) {
        try {
            File managedDirectory =
                    getManagedCacheDirectory()
                            .getCanonicalFile();

            File canonicalBackupFile =
                    backupFile.getCanonicalFile();

            String managedDirectoryPath =
                    managedDirectory.getPath()
                            + File.separator;

            String backupFilePath =
                    canonicalBackupFile.getPath();

            return backupFilePath.startsWith(
                    managedDirectoryPath
            )
                    && hasManagedCacheFileName(
                    canonicalBackupFile.getName()
            );

        } catch (IOException exception) {
            return false;
        }
    }

    private boolean hasManagedCacheFileName(
            @NonNull String fileName
    ) {
        if (!fileName.startsWith(
                CACHE_FILE_PREFIX
        )) {
            return false;
        }

        return fileName.endsWith(
                CACHE_FILE_EXTENSION
        )
                || fileName.endsWith(
                CACHE_FILE_EXTENSION
                        + TEMP_FILE_EXTENSION
        );
    }

    @NonNull
    private File getManagedCacheDirectory() {
        return CloudBackupSecurityGuard
                .getManagedRestoreCacheDirectory(
                        applicationContext
                );
    }

    /**
     * Best-effort zero overwrite followed by deletion.
     */
    private boolean securelyDeleteManagedFileIfPresent(
            @NonNull File file
    ) {
        if (!file.exists()) {
            return true;
        }

        if (!file.isFile()
                || !isManagedCachePath(file)) {

            return false;
        }

        overwriteFileWithZeros(
                file
        );

        if (file.delete()) {
            return true;
        }

        file.deleteOnExit();

        return !file.exists();
    }

    private boolean overwriteFileWithZeros(
            @NonNull File file
    ) {
        long fileLength =
                file.length();

        if (fileLength <= 0L) {
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
                    fileLength;

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
                    fileLength
            );

            randomAccessFile
                    .getFD()
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
                     * Best-effort resource cleanup.
                     */
                }
            }
        }
    }

    private void dispatchSuccess(
            @NonNull RestorePreparationCallback callback,
            @NonNull CloudRestorePreparationResult result
    ) {
        mainThreadHandler.post(() ->
                callback.onPrepared(
                        result
                )
        );
    }

    private void dispatchError(
            @NonNull RestorePreparationCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(() ->
                callback.onError(
                        exception
                )
        );
    }

    public interface RestorePreparationCallback {

        void onPrepared(
                @NonNull CloudRestorePreparationResult result
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class
    CloudRestorePreparationResult {

        private final String absoluteFilePath;

        private final String displayFileName;

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

        private CloudRestorePreparationResult(
                @NonNull String absoluteFilePath,
                @NonNull String displayFileName,
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
            this.absoluteFilePath =
                    absoluteFilePath;

            this.displayFileName =
                    displayFileName;

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
        public String getAbsoluteFilePath() {
            return absoluteFilePath;
        }

        @NonNull
        public String getDisplayFileName() {
            return displayFileName;
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

    public static final class
    CloudRestorePreparationException
            extends Exception {

        public CloudRestorePreparationException(
                @NonNull String message
        ) {
            super(
                    message
            );
        }

        public CloudRestorePreparationException(
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