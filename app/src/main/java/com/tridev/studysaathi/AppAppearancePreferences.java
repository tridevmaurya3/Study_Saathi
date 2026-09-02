package com.tridev.studysaathi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class AppAppearancePreferences {

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final String LANGUAGE_HINDI = "hi";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_HINGLISH = "hinglish";

    /**
     * Legacy value used by older Study Saathi builds.
     *
     * Keep this constant so old saved preferences remain readable while the
     * public product language is now called Hinglish.
     */
    @Deprecated
    public static final String LANGUAGE_BILINGUAL = "hi,en";

    private static final String LOCALE_TAG_HINDI =
            "hi";

    private static final String LOCALE_TAG_ENGLISH =
            "en";

    /**
     * Hinglish is a Study Saathi product mode, not a separate natural
     * language code. We use the India-specific English resource variant so
     * Android can select a dedicated values-en-rIN pack while still falling
     * back safely to pure English resources for keys that are not translated
     * yet.
     */
    private static final String LOCALE_TAG_HINGLISH =
            "en-IN";

    private static final String PREFERENCES_NAME =
            "study_saathi_appearance";

    private static final String KEY_THEME =
            "theme_mode";

    private static final String KEY_LANGUAGE =
            "language_mode";

    private AppAppearancePreferences() {
    }

    public static void applySavedAppearance(
            @NonNull Context context
    ) {
        applyTheme(
                getTheme(context)
        );

        applyLanguage(
                getLanguage(context)
        );
    }

    @NonNull
    public static String getTheme(
            @NonNull Context context
    ) {
        String theme =
                preferences(context).getString(
                        KEY_THEME,
                        THEME_SYSTEM
                );

        return theme == null
                ? THEME_SYSTEM
                : theme;
    }

    @NonNull
    public static String getLanguage(
            @NonNull Context context
    ) {
        String storedLanguage =
                preferences(context).getString(
                        KEY_LANGUAGE,
                        LANGUAGE_HINGLISH
                );

        String normalizedLanguage =
                normalizeLanguage(
                        storedLanguage
                );

        if (storedLanguage == null
                || !normalizedLanguage.equals(
                storedLanguage
        )) {
            preferences(context).edit()
                    .putString(
                            KEY_LANGUAGE,
                            normalizedLanguage
                    )
                    .apply();
        }

        return normalizedLanguage;
    }

    public static void saveAndApplyTheme(
            @NonNull Context context,
            @NonNull String theme
    ) {
        preferences(context).edit()
                .putString(KEY_THEME, theme)
                .apply();

        applyTheme(theme);
    }

    public static void saveAndApplyLanguage(
            @NonNull Context context,
            @NonNull String language
    ) {
        String normalizedLanguage =
                normalizeLanguage(
                        language
                );

        preferences(context).edit()
                .putString(
                        KEY_LANGUAGE,
                        normalizedLanguage
                )
                .apply();

        applyLanguage(
                normalizedLanguage
        );
    }

    private static void applyTheme(
            @NonNull String theme
    ) {
        int nightMode;

        if (THEME_LIGHT.equals(theme)) {
            nightMode =
                    AppCompatDelegate.MODE_NIGHT_NO;
        } else if (THEME_DARK.equals(theme)) {
            nightMode =
                    AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            nightMode =
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }

        AppCompatDelegate.setDefaultNightMode(
                nightMode
        );
    }

    private static void applyLanguage(
            @NonNull String language
    ) {
        String normalizedLanguage =
                normalizeLanguage(
                        language
                );

        String languageTags;

        if (LANGUAGE_HINDI.equals(
                normalizedLanguage
        )) {
            languageTags =
                    LOCALE_TAG_HINDI;
        } else if (LANGUAGE_ENGLISH.equals(
                normalizedLanguage
        )) {
            languageTags =
                    LOCALE_TAG_ENGLISH;
        } else {
            languageTags =
                    LOCALE_TAG_HINGLISH;
        }

        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                        languageTags
                )
        );
    }

    @NonNull
    private static String normalizeLanguage(
            String language
    ) {
        if (language == null) {
            return LANGUAGE_HINGLISH;
        }

        String normalized =
                language.trim();

        if (LANGUAGE_HINDI.equalsIgnoreCase(
                normalized
        )) {
            return LANGUAGE_HINDI;
        }

        if (LANGUAGE_ENGLISH.equalsIgnoreCase(
                normalized
        )) {
            return LANGUAGE_ENGLISH;
        }

        if (LANGUAGE_HINGLISH.equalsIgnoreCase(
                normalized
        )
                || LANGUAGE_BILINGUAL.equalsIgnoreCase(
                normalized
        )
                || "bilingual".equalsIgnoreCase(
                normalized
        )
                || "hindi + english".equalsIgnoreCase(
                normalized
        )) {
            return LANGUAGE_HINGLISH;
        }

        return LANGUAGE_HINGLISH;
    }

    @NonNull
    private static SharedPreferences preferences(
            @NonNull Context context
    ) {
        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE
                );
    }
}
