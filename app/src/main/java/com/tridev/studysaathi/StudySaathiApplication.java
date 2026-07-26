package com.tridev.studysaathi;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * App की सभी Activities को Android 15/16 edge-to-edge system bars से सुरक्षित रखता है.
 */
public final class StudySaathiApplication
        extends Application
        implements Application.ActivityLifecycleCallbacks {

    @NonNull
    private final Set<Activity> insetConfiguredActivities =
            Collections.newSetFromMap(
                    new WeakHashMap<>()
            );

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState
    ) {
        applySafeSystemBarInsets(activity);
    }

    @Override
    public void onActivityPostCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState
    ) {
        applySafeSystemBarInsets(activity);
    }

    private void applySafeSystemBarInsets(
            @NonNull Activity activity
    ) {
        if (!insetConfiguredActivities.add(activity)) {
            return;
        }

        Window window = activity.getWindow();

        /*
         * Android 15/16 edge-to-edge को support करते हुए content root पर
         * वास्तविक status/navigation/cutout insets लगाए जाते हैं.
         */
        WindowCompat.setDecorFitsSystemWindows(
                window,
                false
        );

        View contentRoot =
                activity.findViewById(
                        android.R.id.content
                );

        if (contentRoot == null) {
            insetConfiguredActivities.remove(activity);
            window.getDecorView().post(
                    () -> applySafeSystemBarInsets(
                            activity
                    )
            );
            return;
        }

        final int originalLeft =
                contentRoot.getPaddingLeft();
        final int originalTop =
                contentRoot.getPaddingTop();
        final int originalRight =
                contentRoot.getPaddingRight();
        final int originalBottom =
                contentRoot.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                contentRoot,
                (view, windowInsets) -> {
                    Insets safeInsets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type
                                            .systemBars()
                                            | WindowInsetsCompat.Type
                                            .displayCutout()
                            );

                    view.setPadding(
                            originalLeft
                                    + safeInsets.left,
                            originalTop
                                    + safeInsets.top,
                            originalRight
                                    + safeInsets.right,
                            originalBottom
                                    + safeInsets.bottom
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(
                contentRoot
        );
    }

    @Override
    public void onActivityStarted(
            @NonNull Activity activity
    ) {
        // No action required.
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
        // No action required.
    }

    @Override
    public void onActivityStopped(
            @NonNull Activity activity
    ) {
        // No action required.
    }

    @Override
    public void onActivitySaveInstanceState(
            @NonNull Activity activity,
            @NonNull Bundle outState
    ) {
        // No action required.
    }

    @Override
    public void onActivityDestroyed(
            @NonNull Activity activity
    ) {
        // No action required.
    }
}
