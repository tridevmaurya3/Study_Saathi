package com.tridev.studysaathi.cloud;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.BackupRestoreActivity;
import com.tridev.studysaathi.CloudAccountActivity;

public final class SensitiveScreenSecurityLifecycle
        implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState
    ) {
        applySensitiveScreenProtection(
                activity
        );
    }

    @Override
    public void onActivityStarted(
            @NonNull Activity activity
    ) {
        /*
         * Apply again in case the Activity window was
         * recreated or modified after onCreate().
         */
        applySensitiveScreenProtection(
                activity
        );
    }

    @Override
    public void onActivityResumed(
            @NonNull Activity activity
    ) {
        /*
         * Defensive reapplication when returning from
         * another screen or system dialog.
         */
        applySensitiveScreenProtection(
                activity
        );
    }

    @Override
    public void onActivityPaused(
            @NonNull Activity activity
    ) {
        // Protection remains enabled while paused.
    }

    @Override
    public void onActivityStopped(
            @NonNull Activity activity
    ) {
        // Protection remains attached to the window.
    }

    @Override
    public void onActivitySaveInstanceState(
            @NonNull Activity activity,
            @NonNull Bundle outState
    ) {
        /*
         * No passphrase, decrypted JSON or cloud account
         * information is written into this Bundle.
         */
    }

    @Override
    public void onActivityDestroyed(
            @NonNull Activity activity
    ) {
        /*
         * The Activity window is destroyed by Android.
         * No explicit flag removal is required.
         */
    }

    private void applySensitiveScreenProtection(
            @NonNull Activity activity
    ) {
        if (!isSensitiveActivity(
                activity
        )) {
            return;
        }

        Window window =
                activity.getWindow();

        if (window == null) {
            return;
        }

        window.addFlags(
                WindowManager.LayoutParams.FLAG_SECURE
        );
    }

    private boolean isSensitiveActivity(
            @NonNull Activity activity
    ) {
        return activity
                instanceof CloudAccountActivity
                || activity
                instanceof BackupRestoreActivity;
    }
}