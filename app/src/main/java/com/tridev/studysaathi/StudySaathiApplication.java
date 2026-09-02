package com.tridev.studysaathi;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.AppCheckProviderFactory;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.tridev.studysaathi.data.ai.FirebaseAiQuotaActivityObserver;
import com.tridev.studysaathi.data.ai.SmartTutorTextToSpeechActivityObserver;
import com.tridev.studysaathi.data.learning.ExactStudyContextResolver;
import com.tridev.studysaathi.data.learning.PhotoBookContextIndex;
import com.tridev.studysaathi.family.FamilyRealtimeSyncManager;
import com.tridev.studysaathi.overlay.StudyOverlayBubbleService;
import com.tridev.studysaathi.ui.ParentLearningTrustActivityObserver;
import com.tridev.studysaathi.ui.SmartAiCompanionController;
import com.tridev.studysaathi.ui.PersistentNavigationController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Main Study Saathi Application lifecycle owner. */
public final class StudySaathiApplication
        extends Application
        implements Application.ActivityLifecycleCallbacks {

    private static final String LOG_TAG = "StudySaathiApplication";
    private static final String DEBUG_PROVIDER_CLASS_NAME =
            "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory";

    @NonNull
    private final Set<Activity> insetConfiguredActivities =
            Collections.newSetFromMap(new WeakHashMap<>());

    private int visibleActivityCount;

    @Override
    public void onCreate() {
        super.onCreate();

        AppAppearancePreferences.applySavedAppearance(this);
        PhotoBookContextIndex.initialize(this);
        ExactStudyContextResolver.initialize(this);
        initializeFirebaseAppCheck();
        registerActivityLifecycleCallbacks(this);

        FirebaseAiQuotaActivityObserver.register(this);
        SmartTutorTextToSpeechActivityObserver.register(this);
        ParentLearningTrustActivityObserver.register(this);

        // No-op unless a verified parent has an active Family Workspace.
        FamilyRealtimeSyncManager.start(this);

        Log.i(LOG_TAG,
                "Study Saathi application observers registered successfully.");
    }

    private void initializeFirebaseAppCheck() {
        try {
            FirebaseApp firebaseApp = FirebaseApp.initializeApp(this);
            if (firebaseApp == null) {
                Log.e(LOG_TAG,
                        "Firebase initialize नहीं हो सका। google-services.json जाँचें।");
                return;
            }

            FirebaseAppCheck firebaseAppCheck =
                    FirebaseAppCheck.getInstance(firebaseApp);
            AppCheckProviderFactory providerFactory;

            if (BuildConfig.DEBUG) {
                providerFactory = createDebugAppCheckProviderFactory();
                if (providerFactory == null) {
                    Log.e(LOG_TAG, "Debug App Check provider load नहीं हुआ।");
                    return;
                }
                Log.i(LOG_TAG,
                        "Firebase App Check Debug Provider installed.");
            } else {
                providerFactory =
                        PlayIntegrityAppCheckProviderFactory.getInstance();
                Log.i(LOG_TAG,
                        "Firebase App Check Play Integrity Provider installed.");
            }

            firebaseAppCheck.installAppCheckProviderFactory(
                    providerFactory,
                    true
            );
        } catch (RuntimeException exception) {
            Log.e(LOG_TAG,
                    "Firebase App Check initialize नहीं हो सका।",
                    exception);
        }
    }

    @Nullable
    private AppCheckProviderFactory createDebugAppCheckProviderFactory() {
        try {
            Class<?> providerClass = Class.forName(DEBUG_PROVIDER_CLASS_NAME);
            Method getInstanceMethod = providerClass.getMethod("getInstance");
            Object providerFactory = getInstanceMethod.invoke(null);

            if (providerFactory instanceof AppCheckProviderFactory) {
                return (AppCheckProviderFactory) providerFactory;
            }

            Log.e(LOG_TAG,
                    "Debug provider सही AppCheckProviderFactory नहीं है।");
        } catch (ClassNotFoundException exception) {
            Log.e(LOG_TAG,
                    "firebase-appcheck-debug dependency उपलब्ध नहीं है।",
                    exception);
        } catch (NoSuchMethodException exception) {
            Log.e(LOG_TAG,
                    "Debug provider का getInstance method नहीं मिला।",
                    exception);
        } catch (IllegalAccessException exception) {
            Log.e(LOG_TAG,
                    "Debug provider access नहीं हो सका।",
                    exception);
        } catch (InvocationTargetException exception) {
            Log.e(LOG_TAG,
                    "Debug provider create करते समय error आया।",
                    exception);
        } catch (RuntimeException exception) {
            Log.e(LOG_TAG,
                    "Debug App Check provider load नहीं हो सका।",
                    exception);
        }
        return null;
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

    private void applySafeSystemBarInsets(@NonNull Activity activity) {
        if (!insetConfiguredActivities.add(activity)) return;

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        View contentRoot = activity.findViewById(android.R.id.content);
        if (contentRoot == null) {
            insetConfiguredActivities.remove(activity);
            window.getDecorView().post(
                    () -> applySafeSystemBarInsets(activity));
            return;
        }

        final int originalLeft = contentRoot.getPaddingLeft();
        final int originalTop = contentRoot.getPaddingTop();
        final int originalRight = contentRoot.getPaddingRight();
        final int originalBottom = contentRoot.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                contentRoot,
                (view, windowInsets) -> {
                    Insets safeInsets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(
                            originalLeft + safeInsets.left,
                            originalTop + safeInsets.top,
                            originalRight + safeInsets.right,
                            originalBottom + safeInsets.bottom);
                    return windowInsets;
                });
        ViewCompat.requestApplyInsets(contentRoot);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        visibleActivityCount++;
        StudyOverlayBubbleService.stop(this);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        SmartAiCompanionController.attach(activity);
        PersistentNavigationController.attach(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) { }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        visibleActivityCount = Math.max(0, visibleActivityCount - 1);
        if (visibleActivityCount == 0 && Settings.canDrawOverlays(this)) {
            StudyOverlayBubbleService.start(this);
        }
    }

    @Override
    public void onActivitySaveInstanceState(
            @NonNull Activity activity,
            @NonNull Bundle outState
    ) { }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        SmartAiCompanionController.detach(activity);
        PersistentNavigationController.detach(activity);
        insetConfiguredActivities.remove(activity);
    }
}
