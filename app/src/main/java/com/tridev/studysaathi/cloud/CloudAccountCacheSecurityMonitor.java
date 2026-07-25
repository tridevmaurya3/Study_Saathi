package com.tridev.studysaathi.cloud;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class CloudAccountCacheSecurityMonitor {

    private static final Object MONITOR_LOCK =
            new Object();

    private static CloudAccountCacheSecurityMonitor
            instance;

    private final FirebaseAuth firebaseAuth;

    private final CloudRestoreCacheSecurityManager
            cacheSecurityManager;

    private final FirebaseAuth.AuthStateListener
            authStateListener;

    @Nullable
    private String activeFirebaseUserId;

    private boolean monitoring;

    private CloudAccountCacheSecurityMonitor(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        firebaseAuth =
                FirebaseAuth.getInstance();

        cacheSecurityManager =
                new CloudRestoreCacheSecurityManager(
                        applicationContext
                );

        authStateListener =
                this::handleAuthenticationStateChanged;
    }

    /**
     * Returns the process-wide security monitor.
     */
    @NonNull
    public static CloudAccountCacheSecurityMonitor
    getInstance(
            @NonNull Context context
    ) {
        synchronized (MONITOR_LOCK) {
            if (instance == null) {
                instance =
                        new CloudAccountCacheSecurityMonitor(
                                context
                        );
            }

            return instance;
        }
    }

    /**
     * Starts monitoring Firebase account changes.
     *
     * Calling this method repeatedly does not register
     * duplicate FirebaseAuth listeners.
     */
    public void startMonitoring() {
        synchronized (MONITOR_LOCK) {
            if (monitoring) {
                return;
            }

            FirebaseUser currentUser =
                    firebaseAuth.getCurrentUser();

            activeFirebaseUserId =
                    getSafeUserId(
                            currentUser
                    );

            /*
             * Perform normal age-based cleanup whenever
             * the security monitor starts.
             */
            cacheSecurityManager
                    .cleanupExpiredFilesAsync(
                            null,
                            null
                    );

            firebaseAuth.addAuthStateListener(
                    authStateListener
            );

            monitoring = true;
        }
    }

    /**
     * Stops monitoring Firebase authentication.
     *
     * This normally is not needed for the app-wide
     * singleton, but it is available for controlled
     * shutdown and testing.
     */
    public void stopMonitoring() {
        synchronized (MONITOR_LOCK) {
            if (!monitoring) {
                return;
            }

            firebaseAuth.removeAuthStateListener(
                    authStateListener
            );

            activeFirebaseUserId = null;
            monitoring = false;
        }
    }

    /**
     * Immediately clears every managed decrypted cloud
     * restore file.
     *
     * This can be called before an explicit sign-out or
     * account-deletion operation.
     */
    public void clearPreparedBackupsImmediately() {
        cacheSecurityManager
                .clearAllManagedFilesAsync(
                        null
                );
    }

    /**
     * Runs normal expiry cleanup while preserving one
     * currently active restore file.
     */
    public void cleanupExpiredBackups(
            @Nullable String activeBackupPath
    ) {
        cacheSecurityManager
                .cleanupExpiredFilesAsync(
                        activeBackupPath,
                        null
                );
    }

    public boolean isMonitoring() {
        synchronized (MONITOR_LOCK) {
            return monitoring;
        }
    }

    private void handleAuthenticationStateChanged(
            @NonNull FirebaseAuth updatedFirebaseAuth
    ) {
        FirebaseUser currentUser =
                updatedFirebaseAuth.getCurrentUser();

        String currentUserId =
                getSafeUserId(
                        currentUser
                );

        String previousUserId;

        synchronized (MONITOR_LOCK) {
            previousUserId =
                    activeFirebaseUserId;

            activeFirebaseUserId =
                    currentUserId;
        }

        /*
         * No user before and no user now:
         * nothing account-specific exists to compare.
         */
        if (previousUserId == null
                && currentUserId == null) {

            return;
        }

        /*
         * The same Firebase account may trigger this
         * listener again after token refresh or profile
         * reload. That does not require cache removal.
         */
        if (previousUserId != null
                && previousUserId.equals(
                currentUserId
        )) {

            cacheSecurityManager
                    .cleanupExpiredFilesAsync(
                            null,
                            null
                    );

            return;
        }

        /*
         * Security-sensitive transitions:
         *
         * 1. Signed-in account -> signed out.
         * 2. Signed-in account A -> account B.
         * 3. Signed-out state -> newly signed-in account.
         *
         * Any decrypted restore file left by the
         * previous authentication state is removed.
         */
        cacheSecurityManager
                .clearAllManagedFilesAsync(
                        null
                );
    }

    @Nullable
    private String getSafeUserId(
            @Nullable FirebaseUser firebaseUser
    ) {
        if (firebaseUser == null) {
            return null;
        }

        String userId =
                firebaseUser.getUid();

        if (userId == null) {
            return null;
        }

        String trimmedUserId =
                userId.trim();

        return trimmedUserId.isEmpty()
                ? null
                : trimmedUserId;
    }
}