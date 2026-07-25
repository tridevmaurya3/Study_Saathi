package com.tridev.studysaathi.cloud;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.BackupRestoreActivity;

import java.io.File;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class CloudRestoreActivitySecurityLifecycle
        implements Application.ActivityLifecycleCallbacks {

    private final CloudRestoreCacheSecurityManager
            cacheSecurityManager;

    /*
     * IdentityHashMap ensures each Activity instance is
     * tracked independently, including configuration
     * changes.
     */
    private final Map<Activity, String>
            activeManagedBackupPaths =
            Collections.synchronizedMap(
                    new IdentityHashMap<>()
            );

    public CloudRestoreActivitySecurityLifecycle(
            @NonNull Application application
    ) {
        cacheSecurityManager =
                new CloudRestoreCacheSecurityManager(
                        application
                );
    }

    @Override
    public void onActivityCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState
    ) {
        if (!(activity
                instanceof BackupRestoreActivity)) {
            return;
        }

        String managedBackupPath =
                readManagedBackupPath(
                        activity
                );

        if (managedBackupPath == null) {
            /*
             * No internal cloud backup is active.
             * Normal expiry cleanup can still run.
             */
            cacheSecurityManager
                    .cleanupExpiredFilesAsync(
                            null,
                            null
                    );

            return;
        }

        activeManagedBackupPaths.put(
                activity,
                managedBackupPath
        );

        /*
         * Remove expired files while preserving the
         * backup currently being previewed.
         */
        cacheSecurityManager
                .cleanupExpiredFilesAsync(
                        managedBackupPath,
                        null
                );
    }

    @Override
    public void onActivityStarted(
            @NonNull Activity activity
    ) {
        if (!(activity
                instanceof BackupRestoreActivity)) {
            return;
        }

        String activeBackupPath =
                activeManagedBackupPaths.get(
                        activity
                );

        cacheSecurityManager
                .cleanupExpiredFilesAsync(
                        activeBackupPath,
                        null
                );
    }

    @Override
    public void onActivityResumed(
            @NonNull Activity activity
    ) {
        // No action required.
    }

    @Override
    public void onActivityPaused(
            @NonNull Activity activity
    ) {
        // Do not delete while a system dialog is shown.
    }

    @Override
    public void onActivityStopped(
            @NonNull Activity activity
    ) {
        /*
         * Do not delete merely because the app entered
         * the background. The user may return to the
         * restore confirmation immediately.
         */
    }

    @Override
    public void onActivitySaveInstanceState(
            @NonNull Activity activity,
            @NonNull Bundle outState
    ) {
        // Passphrase and backup JSON are not saved here.
    }

    @Override
    public void onActivityDestroyed(
            @NonNull Activity activity
    ) {
        if (!(activity
                instanceof BackupRestoreActivity)) {
            return;
        }

        String managedBackupPath =
                activeManagedBackupPaths.remove(
                        activity
                );

        if (managedBackupPath == null
                || managedBackupPath
                .trim()
                .isEmpty()) {
            return;
        }

        /*
         * During rotation Android destroys the old
         * Activity and creates a replacement. The active
         * restore file must survive that transition.
         */
        if (activity.isChangingConfigurations()) {
            return;
        }

        /*
         * A finishing Activity means the user backed out,
         * restore completed, or the screen was otherwise
         * explicitly closed. The decrypted cache copy is
         * no longer needed.
         */
        if (activity.isFinishing()) {
            cacheSecurityManager
                    .deleteManagedBackup(
                            managedBackupPath
                    );

            return;
        }

        /*
         * Defensive cleanup for an unexpected Activity
         * destruction that is not a configuration change.
         */
        cacheSecurityManager
                .deleteManagedBackup(
                        managedBackupPath
                );
    }

    @Nullable
    private String readManagedBackupPath(
            @NonNull Activity activity
    ) {
        if (activity.getIntent() == null) {
            return null;
        }

        String incomingPath =
                activity.getIntent()
                        .getStringExtra(
                                BackupRestoreActivity
                                        .EXTRA_INTERNAL_BACKUP_PATH
                        );

        if (incomingPath == null
                || incomingPath
                .trim()
                .isEmpty()) {
            return null;
        }

        File incomingFile =
                new File(
                        incomingPath.trim()
                );

        if (!cacheSecurityManager
                .isManagedBackupPath(
                        incomingFile
                )) {
            return null;
        }

        return incomingFile
                .getAbsolutePath();
    }
}