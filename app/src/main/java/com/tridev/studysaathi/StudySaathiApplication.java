package com.tridev.studysaathi;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
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
import com.tridev.studysaathi.ui.SmartAiCompanionController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Study Saathi का मुख्य Application class।
 *
 * यह class:
 *
 * 1. Firebase App Check को app के शुरू होते ही initialize करती है।
 * 2. Debug build में Firebase App Check Debug Provider उपयोग करती है।
 * 3. Release build में Play Integrity Provider उपयोग करती है।
 * 4. सभी Activities को Android edge-to-edge system bars से सुरक्षित रखती है।
 * 5. Ask Study Saathi screen के Firebase AI quota cooldown को observe करती है।
 * 6. Active quota cooldown में live countdown UI दिखाती है।
 * 7. Hero Answer Card के Text-to-Speech observer को register करती है।
 * 8. Smart Answer के लिए Listen और Stop controls की foundation सक्रिय करती है।
 */
public final class StudySaathiApplication
        extends Application
        implements Application.ActivityLifecycleCallbacks {

    private static final String LOG_TAG =
            "StudySaathiApplication";

    /*
     * Debug provider class को reflection से load किया जाता है।
     *
     * इससे आगे release build में debug dependency को केवल
     * debugImplementation करने पर भी main source code को
     * बदलना नहीं पड़ेगा।
     */
    private static final String DEBUG_PROVIDER_CLASS_NAME =
            "com.google.firebase.appcheck.debug."
                    + "DebugAppCheckProviderFactory";

    @NonNull
    private final Set<Activity> insetConfiguredActivities =
            Collections.newSetFromMap(
                    new WeakHashMap<>()
            );

    @Override
    public void onCreate() {
        super.onCreate();

        AppAppearancePreferences.applySavedAppearance(
                this
        );

        /*
         * Firebase App Check को किसी भी Firebase Auth,
         * Firestore या Firebase AI request से पहले initialize करें।
         */
        initializeFirebaseAppCheck();

        /*
         * Application का अपना lifecycle callback सभी Activities
         * पर safe system-bar insets लागू करता है।
         */
        registerActivityLifecycleCallbacks(
                this
        );

        /*
         * Firebase AI quota observer को केवल एक बार register करें।
         *
         * यह observer:
         *
         * - केवल AskStudySaathiActivity को observe करेगा।
         * - Saved Firebase AI cooldown पहचानेगा।
         * - Ask button पर live MM:SS countdown दिखाएगा।
         * - Cooldown के दौरान Ask और Quick Action buttons रोकेगा।
         * - Cooldown पूरा होने पर controls फिर सक्रिय करेगा।
         */
        FirebaseAiQuotaActivityObserver.register(
                this
        );

        /*
         * Hero Smart Answer Text-to-Speech observer को
         * केवल एक बार register करें।
         *
         * यह observer:
         *
         * - केवल AskStudySaathiActivity को observe करेगा।
         * - Structured source label वाला answer पहचानेगा।
         * - Answer के नीचे "उत्तर सुनें" button जोड़ेगा।
         * - Hindi और English answer को आवाज में पढ़ेगा।
         * - Speech के दौरान Stop action देगा।
         * - Activity pause/destroy होने पर speech और
         *   Text-to-Speech resources सुरक्षित रूप से release करेगा।
         */
        SmartTutorTextToSpeechActivityObserver.register(
                this
        );

        Log.i(
                LOG_TAG,
                "Study Saathi application observers registered successfully."
        );
    }

    /**
     * Firebase App Check provider install करता है।
     *
     * Debug build:
     * DebugAppCheckProviderFactory
     *
     * Release build:
     * PlayIntegrityAppCheckProviderFactory
     */
    private void initializeFirebaseAppCheck() {
        try {
            FirebaseApp firebaseApp =
                    FirebaseApp.initializeApp(
                            this
                    );

            if (firebaseApp == null) {
                Log.e(
                        LOG_TAG,
                        "Firebase initialize नहीं हो सका। "
                                + "google-services.json जाँचें।"
                );

                return;
            }

            FirebaseAppCheck firebaseAppCheck =
                    FirebaseAppCheck.getInstance(
                            firebaseApp
                    );

            AppCheckProviderFactory providerFactory;

            if (BuildConfig.DEBUG) {
                providerFactory =
                        createDebugAppCheckProviderFactory();

                if (providerFactory == null) {
                    Log.e(
                            LOG_TAG,
                            "Debug App Check provider load नहीं हुआ।"
                    );

                    return;
                }

                Log.i(
                        LOG_TAG,
                        "Firebase App Check Debug Provider installed."
                );

            } else {
                providerFactory =
                        PlayIntegrityAppCheckProviderFactory
                                .getInstance();

                Log.i(
                        LOG_TAG,
                        "Firebase App Check Play Integrity Provider installed."
                );
            }

            /*
             * दूसरा argument true रखने से App Check token
             * अपने-आप समय पर refresh होता रहेगा।
             */
            firebaseAppCheck.installAppCheckProviderFactory(
                    providerFactory,
                    true
            );

        } catch (RuntimeException exception) {
            Log.e(
                    LOG_TAG,
                    "Firebase App Check initialize नहीं हो सका।",
                    exception
            );
        }
    }

    /**
     * Debug provider को reflection के माध्यम से प्राप्त करता है।
     *
     * Debug token source code में hardcode नहीं किया जाता।
     * पहली बार app run होने पर Firebase SDK Logcat में token बनाएगा।
     */
    @Nullable
    private AppCheckProviderFactory
    createDebugAppCheckProviderFactory() {

        try {
            Class<?> providerClass =
                    Class.forName(
                            DEBUG_PROVIDER_CLASS_NAME
                    );

            Method getInstanceMethod =
                    providerClass.getMethod(
                            "getInstance"
                    );

            Object providerFactory =
                    getInstanceMethod.invoke(
                            null
                    );

            if (providerFactory
                    instanceof AppCheckProviderFactory) {

                return (AppCheckProviderFactory)
                        providerFactory;
            }

            Log.e(
                    LOG_TAG,
                    "Debug provider सही AppCheckProviderFactory नहीं है।"
            );

        } catch (ClassNotFoundException exception) {
            Log.e(
                    LOG_TAG,
                    "firebase-appcheck-debug dependency उपलब्ध नहीं है।",
                    exception
            );

        } catch (NoSuchMethodException exception) {
            Log.e(
                    LOG_TAG,
                    "Debug provider का getInstance method नहीं मिला।",
                    exception
            );

        } catch (IllegalAccessException exception) {
            Log.e(
                    LOG_TAG,
                    "Debug provider access नहीं हो सका।",
                    exception
            );

        } catch (InvocationTargetException exception) {
            Log.e(
                    LOG_TAG,
                    "Debug provider create करते समय error आया।",
                    exception
            );

        } catch (RuntimeException exception) {
            Log.e(
                    LOG_TAG,
                    "Debug App Check provider load नहीं हो सका।",
                    exception
            );
        }

        return null;
    }

    @Override
    public void onActivityCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState
    ) {
        applySafeSystemBarInsets(
                activity
        );
    }

    @Override
    public void onActivityPostCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState
    ) {
        applySafeSystemBarInsets(
                activity
        );
    }

    /**
     * Android 15 और Android 16 edge-to-edge behavior के लिए
     * हर Activity के root content पर सुरक्षित system-bar padding लगाता है।
     */
    private void applySafeSystemBarInsets(
            @NonNull Activity activity
    ) {
        if (!insetConfiguredActivities.add(
                activity
        )) {
            return;
        }

        Window window =
                activity.getWindow();

        /*
         * Edge-to-edge support चालू रखते हुए वास्तविक
         * system bar insets content root पर लगाए जाते हैं।
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
            insetConfiguredActivities.remove(
                    activity
            );

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
        SmartAiCompanionController.attach(
                activity
        );

        /*
         * FirebaseAiQuotaActivityObserver और
         * SmartTutorTextToSpeechActivityObserver अपने lifecycle
         * callbacks independently handle करते हैं।
         */
    }

    @Override
    public void onActivityPaused(
            @NonNull Activity activity
    ) {
        /*
         * TTS observer Ask Study Saathi screen pause होने पर
         * active speech अपने-आप रोकता है।
         */
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
        /*
         * Firebase AI cooldown SharedPreferences में save रहता है।
         * TTS speech state को जानबूझकर restore नहीं किया जाता।
         */
    }

    @Override
    public void onActivityDestroyed(
            @NonNull Activity activity
    ) {
        SmartAiCompanionController.detach(
                activity
        );

        insetConfiguredActivities.remove(
                activity
        );

        /*
         * TTS और quota observers अपने Activity sessions
         * independently release करते हैं।
         */
    }
}
