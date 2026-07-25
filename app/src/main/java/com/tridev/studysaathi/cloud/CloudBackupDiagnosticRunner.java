package com.tridev.studysaathi.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public final class CloudBackupDiagnosticRunner {

    private static final String TEST_ACCOUNT_SIGNED_IN =
            "account_signed_in";

    private static final String TEST_EMAIL_VERIFIED =
            "email_verified";

    private static final String TEST_FIREBASE_UID =
            "firebase_uid";

    private static final String TEST_METADATA_CONNECTION =
            "metadata_connection";

    private static final String TEST_BACKUP_AVAILABILITY =
            "backup_availability";

    private static final String TEST_BACKUP_COMPLETENESS =
            "backup_completeness";

    private static final String TEST_BACKUP_ID =
            "backup_id";

    private static final String TEST_BACKUP_TIMESTAMP =
            "backup_timestamp";

    private static final String TEST_BACKUP_STORAGE =
            "backup_storage";

    private static final String TEST_BACKUP_ENCRYPTION =
            "backup_encryption";

    private static final String TEST_BACKUP_COUNTS =
            "backup_counts";

    private static final String TEST_CACHE_PATH_SECURITY =
            "cache_path_security";

    private final Context applicationContext;

    private final FirebaseAuth firebaseAuth;

    private final CloudBackupUploader cloudBackupUploader;

    private final CloudBackupTestReportRepository
            reportRepository;

    private final CloudRestoreCacheSecurityManager
            cacheSecurityManager;

    private final Handler mainThreadHandler;

    private boolean testRunning;

    public CloudBackupDiagnosticRunner(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        firebaseAuth =
                FirebaseAuth.getInstance();

        cloudBackupUploader =
                new CloudBackupUploader();

        reportRepository =
                new CloudBackupTestReportRepository(
                        applicationContext
                );

        cacheSecurityManager =
                new CloudRestoreCacheSecurityManager(
                        applicationContext
                );

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Runs non-destructive cloud backup diagnostics.
     *
     * This method does not upload, replace, restore or
     * delete any Firebase backup document.
     */
    public synchronized void runDiagnostics(
            @NonNull DiagnosticCallback callback
    ) {
        if (testRunning) {
            dispatchError(
                    callback,
                    new DiagnosticException(
                            "Cloud backup diagnostic test "
                                    + "is already running."
                    )
            );

            return;
        }

        testRunning = true;

        CloudBackupTestReport report =
                new CloudBackupTestReport();

        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        addAccountSignInResult(
                report,
                firebaseUser
        );

        if (firebaseUser == null) {
            addNotRunResult(
                    report,
                    TEST_EMAIL_VERIFIED,
                    "Email verification",
                    "Verified Firebase email account की जाँच।",
                    "Cloud account signed in नहीं है।",
                    null,
                    null
            );

            addNotRunResult(
                    report,
                    TEST_FIREBASE_UID,
                    "Firebase UID",
                    "Firebase account UID की उपलब्धता की जाँच।",
                    "Cloud account signed in नहीं है।",
                    null,
                    null
            );

            addCloudDependentNotRunResults(
                    report,
                    "Cloud account signed in नहीं है।",
                    null,
                    null
            );

            addCachePathSecurityResult(
                    report,
                    null
            );

            completeAndSaveReport(
                    report,
                    callback
            );

            return;
        }

        addEmailVerificationResult(
                report,
                firebaseUser
        );

        addFirebaseUidResult(
                report,
                firebaseUser
        );

        addCachePathSecurityResult(
                report,
                firebaseUser.getUid()
        );

        if (!firebaseUser.isEmailVerified()) {
            addCloudDependentNotRunResults(
                    report,
                    "Email verification required है।",
                    firebaseUser.getUid(),
                    null
            );

            completeAndSaveReport(
                    report,
                    callback
            );

            return;
        }

        loadAndValidateCloudMetadata(
                firebaseUser,
                report,
                callback
        );
    }

    public synchronized boolean isTestRunning() {
        return testRunning;
    }

    private void loadAndValidateCloudMetadata(
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupTestReport report,
            @NonNull DiagnosticCallback callback
    ) {
        long metadataTestStartedAt =
                System.currentTimeMillis();

        cloudBackupUploader
                .loadLatestBackupMetadata(
                        firebaseUser,
                        new CloudBackupUploader
                                .MetadataCallback() {

                            @Override
                            public void onLoaded(
                                    @Nullable CloudBackupUploader
                                            .CloudBackupMetadata metadata
                            ) {
                                long completedAt =
                                        System.currentTimeMillis();

                                report.addResult(
                                        new CloudBackupTestReport
                                                .TestResult.Builder(
                                                TEST_METADATA_CONNECTION,
                                                "Cloud metadata connection"
                                        )
                                                .setTestDescription(
                                                        "Firebase server से latest "
                                                                + "cloud backup metadata "
                                                                + "पढ़ने की जाँच।"
                                                )
                                                .setStatus(
                                                        CloudBackupTestReport
                                                                .TestStatus.PASSED
                                                )
                                                .setStartedAt(
                                                        metadataTestStartedAt
                                                )
                                                .setCompletedAt(
                                                        completedAt
                                                )
                                                .setUserMessage(
                                                        "Cloud metadata server "
                                                                + "connection सफल है।"
                                                )
                                                .setTechnicalMessage(
                                                        "Firestore latest backup "
                                                                + "metadata request completed."
                                                )
                                                .setFirebaseUserId(
                                                        firebaseUser.getUid()
                                                )
                                                .build()
                                );

                                if (metadata == null) {
                                    addNoBackupResults(
                                            report,
                                            firebaseUser.getUid()
                                    );

                                    completeAndSaveReport(
                                            report,
                                            callback
                                    );

                                    return;
                                }

                                addBackupAvailabilityResult(
                                        report,
                                        firebaseUser,
                                        metadata
                                );

                                addBackupCompletenessResult(
                                        report,
                                        firebaseUser,
                                        metadata
                                );

                                addBackupIdResult(
                                        report,
                                        firebaseUser,
                                        metadata
                                );

                                addBackupTimestampResult(
                                        report,
                                        firebaseUser,
                                        metadata
                                );

                                addBackupStorageResult(
                                        report,
                                        firebaseUser,
                                        metadata
                                );

                                addBackupEncryptionResult(
                                        report,
                                        firebaseUser,
                                        metadata
                                );

                                addBackupCountsResult(
                                        report,
                                        firebaseUser,
                                        metadata
                                );

                                completeAndSaveReport(
                                        report,
                                        callback
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                long completedAt =
                                        System.currentTimeMillis();

                                report.addResult(
                                        new CloudBackupTestReport
                                                .TestResult.Builder(
                                                TEST_METADATA_CONNECTION,
                                                "Cloud metadata connection"
                                        )
                                                .setTestDescription(
                                                        "Firebase server से latest "
                                                                + "cloud backup metadata "
                                                                + "पढ़ने की जाँच।"
                                                )
                                                .setStatus(
                                                        CloudBackupTestReport
                                                                .TestStatus.FAILED
                                                )
                                                .setStartedAt(
                                                        metadataTestStartedAt
                                                )
                                                .setCompletedAt(
                                                        completedAt
                                                )
                                                .setUserMessage(
                                                        "Cloud backup metadata "
                                                                + "load नहीं हो सकी।"
                                                )
                                                .setError(
                                                        exception
                                                )
                                                .setFirebaseUserId(
                                                        firebaseUser.getUid()
                                                )
                                                .build()
                                );

                                addCloudMetadataDependentNotRunResults(
                                        report,
                                        "Cloud metadata request failed।",
                                        firebaseUser.getUid()
                                );

                                completeAndSaveReport(
                                        report,
                                        callback
                                );
                            }
                        }
                );
    }

    private void addAccountSignInResult(
            @NonNull CloudBackupTestReport report,
            @Nullable FirebaseUser firebaseUser
    ) {
        long currentTime =
                System.currentTimeMillis();

        boolean signedIn =
                firebaseUser != null;

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_ACCOUNT_SIGNED_IN,
                        "Cloud account sign-in"
                )
                        .setTestDescription(
                                "Firebase cloud account sign-in "
                                        + "स्थिति की जाँच।"
                        )
                        .setStatus(
                                signedIn
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.FAILED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                signedIn
                                        ? "Cloud account signed in है।"
                                        : "Cloud account signed in नहीं है।"
                        )
                        .setTechnicalMessage(
                                signedIn
                                        ? "FirebaseAuth current user is available."
                                        : "FirebaseAuth current user is null."
                        )
                        .setFirebaseUserId(
                                signedIn
                                        ? firebaseUser.getUid()
                                        : null
                        )
                        .build()
        );
    }

    private void addEmailVerificationResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser
    ) {
        long currentTime =
                System.currentTimeMillis();

        boolean verified =
                firebaseUser.isEmailVerified();

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_EMAIL_VERIFIED,
                        "Email verification"
                )
                        .setTestDescription(
                                "Cloud backup account email "
                                        + "verification की जाँच।"
                        )
                        .setStatus(
                                verified
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.FAILED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                verified
                                        ? "Cloud account email verified है।"
                                        : "Cloud account email verify करना आवश्यक है।"
                        )
                        .setTechnicalMessage(
                                "FirebaseUser.isEmailVerified="
                                        + verified
                        )
                        .setFirebaseUserId(
                                firebaseUser.getUid()
                        )
                        .build()
        );
    }

    private void addFirebaseUidResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser
    ) {
        long currentTime =
                System.currentTimeMillis();

        String userId =
                firebaseUser.getUid();

        boolean validUserId =
                userId != null
                        && !userId.trim().isEmpty();

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_FIREBASE_UID,
                        "Firebase UID"
                )
                        .setTestDescription(
                                "Cloud backup ownership के लिए "
                                        + "Firebase UID की जाँच।"
                        )
                        .setStatus(
                                validUserId
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.FAILED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                validUserId
                                        ? "Firebase account UID उपलब्ध है।"
                                        : "Firebase account UID उपलब्ध नहीं है।"
                        )
                        .setTechnicalMessage(
                                validUserId
                                        ? "Firebase UID is non-empty."
                                        : "Firebase UID is empty."
                        )
                        .setFirebaseUserId(
                                userId
                        )
                        .build()
        );
    }

    private void addBackupAvailabilityResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupUploader
                    .CloudBackupMetadata metadata
    ) {
        long currentTime =
                System.currentTimeMillis();

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_BACKUP_AVAILABILITY,
                        "Cloud backup availability"
                )
                        .setTestDescription(
                                "Latest cloud backup metadata "
                                        + "उपलब्ध होने की जाँच।"
                        )
                        .setStatus(
                                CloudBackupTestReport
                                        .TestStatus.PASSED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                "Cloud backup उपलब्ध है।"
                        )
                        .setTechnicalMessage(
                                "Latest cloud backup metadata exists."
                        )
                        .setFirebaseUserId(
                                firebaseUser.getUid()
                        )
                        .setBackupId(
                                metadata.getBackupId()
                        )
                        .build()
        );
    }

    private void addBackupCompletenessResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupUploader
                    .CloudBackupMetadata metadata
    ) {
        long currentTime =
                System.currentTimeMillis();

        boolean complete =
                metadata.isComplete();

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_BACKUP_COMPLETENESS,
                        "Backup completeness"
                )
                        .setTestDescription(
                                "Cloud backup complete status की जाँच।"
                        )
                        .setStatus(
                                complete
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.FAILED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                complete
                                        ? "Cloud backup complete है।"
                                        : "Cloud backup incomplete या invalid status में है।"
                        )
                        .setTechnicalMessage(
                                "metadata.isComplete="
                                        + complete
                        )
                        .setFirebaseUserId(
                                firebaseUser.getUid()
                        )
                        .setBackupId(
                                metadata.getBackupId()
                        )
                        .build()
        );
    }

    private void addBackupIdResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupUploader
                    .CloudBackupMetadata metadata
    ) {
        long currentTime =
                System.currentTimeMillis();

        String backupId =
                metadata.getBackupId();

        boolean validBackupId =
                backupId != null
                        && !backupId.trim().isEmpty();

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_BACKUP_ID,
                        "Backup identifier"
                )
                        .setTestDescription(
                                "Cloud backup ID की validity जाँच।"
                        )
                        .setStatus(
                                validBackupId
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.FAILED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                validBackupId
                                        ? "Cloud backup ID valid है।"
                                        : "Cloud backup ID missing है।"
                        )
                        .setTechnicalMessage(
                                validBackupId
                                        ? "Backup ID is non-empty."
                                        : "Backup ID is empty."
                        )
                        .setFirebaseUserId(
                                firebaseUser.getUid()
                        )
                        .setBackupId(
                                backupId
                        )
                        .build()
        );
    }

    private void addBackupTimestampResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupUploader
                    .CloudBackupMetadata metadata
    ) {
        long currentTime =
                System.currentTimeMillis();

        long backupCreatedAt =
                metadata.getBackupCreatedAt();

        long uploadedAt =
                metadata.getUploadedAt();

        boolean validTimestamp =
                backupCreatedAt > 0L
                        && (
                        uploadedAt <= 0L
                                || uploadedAt >= backupCreatedAt
                );

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_BACKUP_TIMESTAMP,
                        "Backup timestamps"
                )
                        .setTestDescription(
                                "Backup creation और upload time "
                                        + "metadata की consistency जाँच।"
                        )
                        .setStatus(
                                validTimestamp
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.WARNING
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                validTimestamp
                                        ? "Backup timestamps valid हैं।"
                                        : "Backup timestamp metadata की समीक्षा आवश्यक है।"
                        )
                        .setTechnicalMessage(
                                "createdAt="
                                        + backupCreatedAt
                                        + ", uploadedAt="
                                        + uploadedAt
                        )
                        .setFirebaseUserId(
                                firebaseUser.getUid()
                        )
                        .setBackupId(
                                metadata.getBackupId()
                        )
                        .build()
        );
    }

    private void addBackupStorageResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupUploader
                    .CloudBackupMetadata metadata
    ) {
        long currentTime =
                System.currentTimeMillis();

        int chunkCount =
                metadata.getChunkCount();

        int storedPayloadBytes =
                metadata.getStoredPayloadBytes();

        boolean validStorage =
                chunkCount > 0
                        && chunkCount
                        <= CloudBackupPayloadBuilder
                        .MAX_CHUNK_COUNT
                        && storedPayloadBytes > 0;

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_BACKUP_STORAGE,
                        "Backup storage metadata"
                )
                        .setTestDescription(
                                "Stored payload size और Firestore "
                                        + "chunk count की जाँच।"
                        )
                        .setStatus(
                                validStorage
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.FAILED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                validStorage
                                        ? "Backup storage metadata valid है।"
                                        : "Backup size या chunk count invalid है।"
                        )
                        .setTechnicalMessage(
                                "chunkCount="
                                        + chunkCount
                                        + ", storedPayloadBytes="
                                        + storedPayloadBytes
                        )
                        .setFirebaseUserId(
                                firebaseUser.getUid()
                        )
                        .setBackupId(
                                metadata.getBackupId()
                        )
                        .build()
        );
    }

    private void addBackupEncryptionResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupUploader
                    .CloudBackupMetadata metadata
    ) {
        long currentTime =
                System.currentTimeMillis();

        boolean encrypted =
                metadata.isClientSideEncrypted();

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_BACKUP_ENCRYPTION,
                        "Client-side encryption"
                )
                        .setTestDescription(
                                "Cloud backup client-side encrypted "
                                        + "format में होने की जाँच।"
                        )
                        .setStatus(
                                encrypted
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.WARNING
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                encrypted
                                        ? "Backup client-side encrypted है।"
                                        : "यह legacy unencrypted cloud backup है।"
                        )
                        .setTechnicalMessage(
                                "isClientSideEncrypted="
                                        + encrypted
                        )
                        .setFirebaseUserId(
                                firebaseUser.getUid()
                        )
                        .setBackupId(
                                metadata.getBackupId()
                        )
                        .build()
        );
    }

    private void addBackupCountsResult(
            @NonNull CloudBackupTestReport report,
            @NonNull FirebaseUser firebaseUser,
            @NonNull CloudBackupUploader
                    .CloudBackupMetadata metadata
    ) {
        long currentTime =
                System.currentTimeMillis();

        boolean validCounts =
                metadata.getProfileCount() >= 0
                        && metadata.getLessonProgressCount() >= 0
                        && metadata.getQuizAttemptCount() >= 0
                        && metadata.getDoubtCount() >= 0
                        && metadata.getPreferenceItemCount() >= 0;

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_BACKUP_COUNTS,
                        "Backup data counts"
                )
                        .setTestDescription(
                                "Profiles, lessons, quizzes, doubts "
                                        + "और preference counts की जाँच।"
                        )
                        .setStatus(
                                validCounts
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.FAILED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                validCounts
                                        ? "Backup data counts valid हैं।"
                                        : "Backup data counts invalid हैं।"
                        )
                        .setTechnicalMessage(
                                "profiles="
                                        + metadata.getProfileCount()
                                        + ", lessons="
                                        + metadata.getLessonProgressCount()
                                        + ", quizzes="
                                        + metadata.getQuizAttemptCount()
                                        + ", doubts="
                                        + metadata.getDoubtCount()
                                        + ", preferences="
                                        + metadata.getPreferenceItemCount()
                        )
                        .setFirebaseUserId(
                                firebaseUser.getUid()
                        )
                        .setBackupId(
                                metadata.getBackupId()
                        )
                        .build()
        );
    }

    private void addCachePathSecurityResult(
            @NonNull CloudBackupTestReport report,
            @Nullable String firebaseUserId
    ) {
        long currentTime =
                System.currentTimeMillis();

        File outsideManagedDirectory =
                new File(
                        applicationContext.getFilesDir(),
                        "outside_cloud_restore_test.json"
                );

        boolean outsidePathRejected =
                !cacheSecurityManager
                        .isManagedBackupPath(
                                outsideManagedDirectory
                        );

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_CACHE_PATH_SECURITY,
                        "Restore cache path security"
                )
                        .setTestDescription(
                                "Managed restore cache से बाहर की "
                                        + "file path rejection की जाँच।"
                        )
                        .setStatus(
                                outsidePathRejected
                                        ? CloudBackupTestReport
                                          .TestStatus.PASSED
                                        : CloudBackupTestReport
                                          .TestStatus.FAILED
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                outsidePathRejected
                                        ? "Restore cache path protection सक्रिय है।"
                                        : "Restore cache path protection failed है।"
                        )
                        .setTechnicalMessage(
                                outsidePathRejected
                                        ? "External private-files path was rejected."
                                        : "External private-files path was accepted unexpectedly."
                        )
                        .setFirebaseUserId(
                                firebaseUserId
                        )
                        .build()
        );
    }

    private void addNoBackupResults(
            @NonNull CloudBackupTestReport report,
            @NonNull String firebaseUserId
    ) {
        long currentTime =
                System.currentTimeMillis();

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        TEST_BACKUP_AVAILABILITY,
                        "Cloud backup availability"
                )
                        .setTestDescription(
                                "Latest cloud backup metadata "
                                        + "उपलब्ध होने की जाँच।"
                        )
                        .setStatus(
                                CloudBackupTestReport
                                        .TestStatus.WARNING
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                "इस account पर cloud backup उपलब्ध नहीं है।"
                        )
                        .setTechnicalMessage(
                                "Latest backup metadata returned null."
                        )
                        .setFirebaseUserId(
                                firebaseUserId
                        )
                        .build()
        );

        addBackupSpecificNotRunResults(
                report,
                "Cloud backup उपलब्ध नहीं है।",
                firebaseUserId,
                null
        );
    }

    private void addCloudDependentNotRunResults(
            @NonNull CloudBackupTestReport report,
            @NonNull String reason,
            @Nullable String firebaseUserId,
            @Nullable String backupId
    ) {
        addNotRunResult(
                report,
                TEST_METADATA_CONNECTION,
                "Cloud metadata connection",
                "Firebase server metadata connection की जाँच।",
                reason,
                firebaseUserId,
                backupId
        );

        addNotRunResult(
                report,
                TEST_BACKUP_AVAILABILITY,
                "Cloud backup availability",
                "Latest cloud backup availability की जाँच।",
                reason,
                firebaseUserId,
                backupId
        );

        addBackupSpecificNotRunResults(
                report,
                reason,
                firebaseUserId,
                backupId
        );
    }

    private void addCloudMetadataDependentNotRunResults(
            @NonNull CloudBackupTestReport report,
            @NonNull String reason,
            @Nullable String firebaseUserId
    ) {
        addNotRunResult(
                report,
                TEST_BACKUP_AVAILABILITY,
                "Cloud backup availability",
                "Latest cloud backup availability की जाँच।",
                reason,
                firebaseUserId,
                null
        );

        addBackupSpecificNotRunResults(
                report,
                reason,
                firebaseUserId,
                null
        );
    }

    private void addBackupSpecificNotRunResults(
            @NonNull CloudBackupTestReport report,
            @NonNull String reason,
            @Nullable String firebaseUserId,
            @Nullable String backupId
    ) {
        addNotRunResult(
                report,
                TEST_BACKUP_COMPLETENESS,
                "Backup completeness",
                "Cloud backup complete status की जाँच।",
                reason,
                firebaseUserId,
                backupId
        );

        addNotRunResult(
                report,
                TEST_BACKUP_ID,
                "Backup identifier",
                "Cloud backup ID validity की जाँच।",
                reason,
                firebaseUserId,
                backupId
        );

        addNotRunResult(
                report,
                TEST_BACKUP_TIMESTAMP,
                "Backup timestamps",
                "Backup timestamps consistency की जाँच।",
                reason,
                firebaseUserId,
                backupId
        );

        addNotRunResult(
                report,
                TEST_BACKUP_STORAGE,
                "Backup storage metadata",
                "Cloud backup size और chunks की जाँच।",
                reason,
                firebaseUserId,
                backupId
        );

        addNotRunResult(
                report,
                TEST_BACKUP_ENCRYPTION,
                "Client-side encryption",
                "Cloud backup encryption status की जाँच।",
                reason,
                firebaseUserId,
                backupId
        );

        addNotRunResult(
                report,
                TEST_BACKUP_COUNTS,
                "Backup data counts",
                "Cloud backup item counts की जाँच।",
                reason,
                firebaseUserId,
                backupId
        );
    }

    private void addNotRunResult(
            @NonNull CloudBackupTestReport report,
            @NonNull String testId,
            @NonNull String testName,
            @NonNull String testDescription,
            @NonNull String reason,
            @Nullable String firebaseUserId,
            @Nullable String backupId
    ) {
        long currentTime =
                System.currentTimeMillis();

        report.addResult(
                new CloudBackupTestReport
                        .TestResult.Builder(
                        testId,
                        testName
                )
                        .setTestDescription(
                                testDescription
                        )
                        .setStatus(
                                CloudBackupTestReport
                                        .TestStatus.NOT_RUN
                        )
                        .setStartedAt(
                                currentTime
                        )
                        .setCompletedAt(
                                currentTime
                        )
                        .setUserMessage(
                                reason
                        )
                        .setTechnicalMessage(
                                "Test was not executed because "
                                        + reason
                        )
                        .setFirebaseUserId(
                                firebaseUserId
                        )
                        .setBackupId(
                                backupId
                        )
                        .build()
        );
    }

    private void completeAndSaveReport(
            @NonNull CloudBackupTestReport report,
            @NonNull DiagnosticCallback callback
    ) {
        report.complete();

        reportRepository.saveReportAsync(
                report,
                new CloudBackupTestReportRepository
                        .SaveCallback() {

                    @Override
                    public void onSaved(
                            @NonNull CloudBackupTestReportRepository
                                    .SavedReport savedReport
                    ) {
                        synchronized (
                                CloudBackupDiagnosticRunner.this
                        ) {
                            testRunning = false;
                        }

                        callback.onCompleted(
                                report,
                                savedReport
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        synchronized (
                                CloudBackupDiagnosticRunner.this
                        ) {
                            testRunning = false;
                        }

                        callback.onError(
                                exception
                        );
                    }
                }
        );
    }

    private void dispatchError(
            @NonNull DiagnosticCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(() ->
                callback.onError(
                        exception
                )
        );
    }

    public interface DiagnosticCallback {

        void onCompleted(
                @NonNull CloudBackupTestReport report,
                @NonNull CloudBackupTestReportRepository
                        .SavedReport savedReport
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class DiagnosticException
            extends Exception {

        public DiagnosticException(
                @NonNull String message
        ) {
            super(message);
        }

        public DiagnosticException(
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