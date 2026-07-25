package com.tridev.studysaathi;

import android.app.Application;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.cloud.CloudAccountCacheSecurityMonitor;
import com.tridev.studysaathi.cloud.CloudRestoreActivitySecurityLifecycle;
import com.tridev.studysaathi.cloud.SensitiveScreenSecurityLifecycle;

public final class StudySaathiApplication
        extends Application {

    private CloudAccountCacheSecurityMonitor
            cloudAccountCacheSecurityMonitor;

    private CloudRestoreActivitySecurityLifecycle
            cloudRestoreActivitySecurityLifecycle;

    private SensitiveScreenSecurityLifecycle
            sensitiveScreenSecurityLifecycle;

    @Override
    public void onCreate() {
        super.onCreate();

        initializeCloudSecurity();
        initializeRestoreActivitySecurity();
        initializeSensitiveScreenSecurity();
    }

    private void initializeCloudSecurity() {
        cloudAccountCacheSecurityMonitor =
                CloudAccountCacheSecurityMonitor
                        .getInstance(this);

        cloudAccountCacheSecurityMonitor
                .startMonitoring();
    }

    private void initializeRestoreActivitySecurity() {
        cloudRestoreActivitySecurityLifecycle =
                new CloudRestoreActivitySecurityLifecycle(
                        this
                );

        registerActivityLifecycleCallbacks(
                cloudRestoreActivitySecurityLifecycle
        );
    }

    private void initializeSensitiveScreenSecurity() {
        sensitiveScreenSecurityLifecycle =
                new SensitiveScreenSecurityLifecycle();

        registerActivityLifecycleCallbacks(
                sensitiveScreenSecurityLifecycle
        );
    }

    /**
     * Returns the process-wide cloud cache security
     * monitor for app components that need an explicit
     * security cleanup.
     */
    @NonNull
    public CloudAccountCacheSecurityMonitor
    getCloudAccountCacheSecurityMonitor() {

        if (cloudAccountCacheSecurityMonitor == null) {
            cloudAccountCacheSecurityMonitor =
                    CloudAccountCacheSecurityMonitor
                            .getInstance(this);

            cloudAccountCacheSecurityMonitor
                    .startMonitoring();
        }

        return cloudAccountCacheSecurityMonitor;
    }

    /**
     * Android normally terminates an application
     * process without calling this method.
     *
     * It remains useful in emulated and controlled
     * development environments.
     */
    @Override
    public void onTerminate() {
        if (sensitiveScreenSecurityLifecycle != null) {
            unregisterActivityLifecycleCallbacks(
                    sensitiveScreenSecurityLifecycle
            );

            sensitiveScreenSecurityLifecycle = null;
        }

        if (cloudRestoreActivitySecurityLifecycle != null) {
            unregisterActivityLifecycleCallbacks(
                    cloudRestoreActivitySecurityLifecycle
            );

            cloudRestoreActivitySecurityLifecycle = null;
        }

        if (cloudAccountCacheSecurityMonitor != null) {
            cloudAccountCacheSecurityMonitor
                    .stopMonitoring();
        }

        super.onTerminate();
    }
}