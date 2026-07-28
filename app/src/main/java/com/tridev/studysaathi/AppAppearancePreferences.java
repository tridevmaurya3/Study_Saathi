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
    public static final String LANGUAGE_BILINGUAL = "hi,en";

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
        String language =
                preferences(context).getString(
                        KEY_LANGUAGE,
                        LANGUAGE_BILINGUAL
                );

        return language == null
                ? LANGUAGE_BILINGUAL
                : language;
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
        preferences(context).edit()
                .putString(KEY_LANGUAGE, language)
                .apply();

        applyLanguage(language);
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
        String languageTags;

        if (LANGUAGE_HINDI.equals(language)) {
            languageTags = "hi";
        } else if (LANGUAGE_ENGLISH.equals(language)) {
            languageTags = "en";
        } else {
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.getEmptyLocaleList()
            );
            return;
        }

        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                        languageTags
                )
        );
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
