package com.tridev.studysaathi.reminder;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public final class StudyReminderScheduler {

    public static final String PREFERENCES_NAME =
            "study_saathi_reminder_preferences";

    public static final String KEY_REMINDER_ENABLED =
            "study_reminder_enabled";

    public static final String KEY_REMINDER_HOUR =
            "study_reminder_hour";

    public static final String KEY_REMINDER_MINUTE =
            "study_reminder_minute";

    public static final String KEY_REMINDER_DAYS_MASK =
            "study_reminder_days_mask";

    public static final String KEY_IS_TEST_REMINDER =
            "is_test_reminder";

    public static final String KEY_IS_SNOOZED_REMINDER =
            "is_snoozed_reminder";

    public static final int DEFAULT_REMINDER_HOUR = 19;
    public static final int DEFAULT_REMINDER_MINUTE = 0;

    public static final int DAY_MONDAY =
            1;

    public static final int DAY_TUESDAY =
            1 << 1;

    public static final int DAY_WEDNESDAY =
            1 << 2;

    public static final int DAY_THURSDAY =
            1 << 3;

    public static final int DAY_FRIDAY =
            1 << 4;

    public static final int DAY_SATURDAY =
            1 << 5;

    public static final int DAY_SUNDAY =
            1 << 6;

    public static final int ALL_DAYS_MASK =
            DAY_MONDAY
                    | DAY_TUESDAY
                    | DAY_WEDNESDAY
                    | DAY_THURSDAY
                    | DAY_FRIDAY
                    | DAY_SATURDAY
                    | DAY_SUNDAY;

    private static final String UNIQUE_PERIODIC_WORK_NAME =
            "study_saathi_daily_reminder_work";

    private static final String UNIQUE_SNOOZED_WORK_NAME =
            "study_saathi_snoozed_reminder_work";

    private static final long SNOOZE_DELAY_MINUTES =
            15L;

    private StudyReminderScheduler() {
        // Utility class.
    }

    public static void saveSettings(
            @NonNull Context context,
            boolean enabled,
            int hour,
            int minute,
            int daysMask
    ) {
        getPreferences(context)
                .edit()
                .putBoolean(
                        KEY_REMINDER_ENABLED,
                        enabled
                )
                .putInt(
                        KEY_REMINDER_HOUR,
                        sanitizeHour(hour)
                )
                .putInt(
                        KEY_REMINDER_MINUTE,
                        sanitizeMinute(minute)
                )
                .putInt(
                        KEY_REMINDER_DAYS_MASK,
                        sanitizeDaysMask(daysMask)
                )
                .apply();
    }

    public static void saveSettings(
            @NonNull Context context,
            boolean enabled,
            int hour,
            int minute
    ) {
        saveSettings(
                context,
                enabled,
                hour,
                minute,
                ALL_DAYS_MASK
        );
    }

    public static boolean isReminderEnabled(
            @NonNull Context context
    ) {
        return getPreferences(context)
                .getBoolean(
                        KEY_REMINDER_ENABLED,
                        false
                );
    }

    public static int getReminderHour(
            @NonNull Context context
    ) {
        return sanitizeHour(
                getPreferences(context)
                        .getInt(
                                KEY_REMINDER_HOUR,
                                DEFAULT_REMINDER_HOUR
                        )
        );
    }

    public static int getReminderMinute(
            @NonNull Context context
    ) {
        return sanitizeMinute(
                getPreferences(context)
                        .getInt(
                                KEY_REMINDER_MINUTE,
                                DEFAULT_REMINDER_MINUTE
                        )
        );
    }

    public static int getReminderDaysMask(
            @NonNull Context context
    ) {
        return sanitizeDaysMask(
                getPreferences(context)
                        .getInt(
                                KEY_REMINDER_DAYS_MASK,
                                ALL_DAYS_MASK
                        )
        );
    }

    public static void scheduleReminder(
            @NonNull Context context,
            int hour,
            int minute,
            int daysMask
    ) {
        Context applicationContext =
                context.getApplicationContext();

        int safeHour =
                sanitizeHour(hour);

        int safeMinute =
                sanitizeMinute(minute);

        int safeDaysMask =
                sanitizeDaysMask(daysMask);

        long initialDelayMillis =
                calculateInitialDelayMillis(
                        safeHour,
                        safeMinute
                );

        Data periodicReminderData =
                new Data.Builder()
                        .putBoolean(
                                KEY_IS_TEST_REMINDER,
                                false
                        )
                        .putBoolean(
                                KEY_IS_SNOOZED_REMINDER,
                                false
                        )
                        .putInt(
                                KEY_REMINDER_DAYS_MASK,
                                safeDaysMask
                        )
                        .build();

        PeriodicWorkRequest reminderWorkRequest =
                new PeriodicWorkRequest.Builder(
                        StudyReminderWorker.class,
                        24,
                        TimeUnit.HOURS
                )
                        .setInitialDelay(
                                initialDelayMillis,
                                TimeUnit.MILLISECONDS
                        )
                        .setInputData(
                                periodicReminderData
                        )
                        .build();

        WorkManager workManager =
                WorkManager.getInstance(
                        applicationContext
                );

        workManager.cancelUniqueWork(
                UNIQUE_SNOOZED_WORK_NAME
        );

        workManager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderWorkRequest
        );
    }

    public static void scheduleReminder(
            @NonNull Context context,
            int hour,
            int minute
    ) {
        scheduleReminder(
                context,
                hour,
                minute,
                getReminderDaysMask(context)
        );
    }

    public static void sendTestReminder(
            @NonNull Context context
    ) {
        Data testReminderData =
                new Data.Builder()
                        .putBoolean(
                                KEY_IS_TEST_REMINDER,
                                true
                        )
                        .putBoolean(
                                KEY_IS_SNOOZED_REMINDER,
                                false
                        )
                        .putInt(
                                KEY_REMINDER_DAYS_MASK,
                                ALL_DAYS_MASK
                        )
                        .build();

        OneTimeWorkRequest testReminderRequest =
                new OneTimeWorkRequest.Builder(
                        StudyReminderWorker.class
                )
                        .setInputData(
                                testReminderData
                        )
                        .build();

        WorkManager.getInstance(
                context.getApplicationContext()
        ).enqueue(
                testReminderRequest
        );
    }

    public static void scheduleSnoozedReminder(
            @NonNull Context context
    ) {
        Data snoozedReminderData =
                new Data.Builder()
                        .putBoolean(
                                KEY_IS_TEST_REMINDER,
                                false
                        )
                        .putBoolean(
                                KEY_IS_SNOOZED_REMINDER,
                                true
                        )
                        .putInt(
                                KEY_REMINDER_DAYS_MASK,
                                ALL_DAYS_MASK
                        )
                        .build();

        OneTimeWorkRequest snoozedReminderRequest =
                new OneTimeWorkRequest.Builder(
                        StudyReminderWorker.class
                )
                        .setInitialDelay(
                                SNOOZE_DELAY_MINUTES,
                                TimeUnit.MINUTES
                        )
                        .setInputData(
                                snoozedReminderData
                        )
                        .build();

        WorkManager.getInstance(
                context.getApplicationContext()
        ).enqueueUniqueWork(
                UNIQUE_SNOOZED_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                snoozedReminderRequest
        );
    }

    public static void cancelReminder(
            @NonNull Context context
    ) {
        WorkManager workManager =
                WorkManager.getInstance(
                        context.getApplicationContext()
                );

        workManager.cancelUniqueWork(
                UNIQUE_PERIODIC_WORK_NAME
        );

        workManager.cancelUniqueWork(
                UNIQUE_SNOOZED_WORK_NAME
        );
    }

    public static int sanitizeDaysMask(
            int daysMask
    ) {
        int safeMask =
                daysMask & ALL_DAYS_MASK;

        if (safeMask == 0) {
            return ALL_DAYS_MASK;
        }

        return safeMask;
    }

    private static long calculateInitialDelayMillis(
            int hour,
            int minute
    ) {
        ZonedDateTime currentTime =
                ZonedDateTime.now();

        ZonedDateTime nextReminderTime =
                currentTime
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(0)
                        .withNano(0);

        if (!nextReminderTime.isAfter(currentTime)) {
            nextReminderTime =
                    nextReminderTime.plusDays(1);
        }

        long delayMillis =
                Duration.between(
                        currentTime,
                        nextReminderTime
                ).toMillis();

        return Math.max(
                1_000L,
                delayMillis
        );
    }

    @NonNull
    private static SharedPreferences getPreferences(
            @NonNull Context context
    ) {
        return context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
    }

    private static int sanitizeHour(int hour) {
        if (hour < 0 || hour > 23) {
            return DEFAULT_REMINDER_HOUR;
        }

        return hour;
    }

    private static int sanitizeMinute(int minute) {
        if (minute < 0 || minute > 59) {
            return DEFAULT_REMINDER_MINUTE;
        }

        return minute;
    }
}