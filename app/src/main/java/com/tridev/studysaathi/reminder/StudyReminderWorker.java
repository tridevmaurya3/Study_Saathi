package com.tridev.studysaathi.reminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.SmartStudyPlanActivity;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class StudyReminderWorker extends Worker {

    public static final String CHANNEL_ID =
            "study_saathi_daily_reminder";

    private static final int DAILY_NOTIFICATION_ID =
            3101;

    private static final int TEST_NOTIFICATION_ID =
            3102;

    private static final int SNOOZED_NOTIFICATION_ID =
            3103;

    public StudyReminderWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters
    ) {
        super(appContext, workerParameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context =
                getApplicationContext();

        boolean testReminder =
                getInputData().getBoolean(
                        StudyReminderScheduler
                                .KEY_IS_TEST_REMINDER,
                        false
                );

        boolean snoozedReminder =
                getInputData().getBoolean(
                        StudyReminderScheduler
                                .KEY_IS_SNOOZED_REMINDER,
                        false
                );

        int selectedDaysMask =
                getInputData().getInt(
                        StudyReminderScheduler
                                .KEY_REMINDER_DAYS_MASK,
                        StudyReminderScheduler
                                .ALL_DAYS_MASK
                );

        if (!testReminder
                && !snoozedReminder
                && !isTodaySelected(
                selectedDaysMask
        )) {
            return Result.success();
        }

        createNotificationChannel(context);

        if (!hasNotificationPermission(context)) {
            return Result.success();
        }

        showReminderNotification(
                context,
                testReminder,
                snoozedReminder
        );

        return Result.success();
    }

    private boolean isTodaySelected(
            int selectedDaysMask
    ) {
        int safeDaysMask =
                StudyReminderScheduler
                        .sanitizeDaysMask(
                                selectedDaysMask
                        );

        DayOfWeek currentDay =
                LocalDate.now().getDayOfWeek();

        int currentDayBit =
                1 << (currentDay.getValue() - 1);

        return (safeDaysMask & currentDayBit) != 0;
    }

    private boolean hasNotificationPermission(
            @NonNull Context context
    ) {
        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }

        return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void showReminderNotification(
            @NonNull Context context,
            boolean testReminder,
            boolean snoozedReminder
    ) {
        int notificationId =
                getNotificationId(
                        testReminder,
                        snoozedReminder
                );

        int notificationTitle =
                getNotificationTitle(
                        testReminder,
                        snoozedReminder
                );

        int notificationMessage =
                getNotificationMessage(
                        testReminder,
                        snoozedReminder
                );

        PendingIntent smartPlanPendingIntent =
                createSmartPlanPendingIntent(
                        context,
                        notificationId
                );

        PendingIntent snoozePendingIntent =
                createSnoozePendingIntent(
                        context,
                        notificationId
                );

        String message =
                context.getString(
                        notificationMessage
                );

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(
                        context,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                R.drawable.ic_study_reminder
                        )
                        .setContentTitle(
                                context.getString(
                                        notificationTitle
                                )
                        )
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT
                        )
                        .setContentIntent(
                                smartPlanPendingIntent
                        )
                        .addAction(
                                R.drawable.ic_study_reminder,
                                context.getString(
                                        R.string.study_reminder_open_plan_action
                                ),
                                smartPlanPendingIntent
                        )
                        .addAction(
                                R.drawable.ic_study_reminder,
                                context.getString(
                                        R.string.study_reminder_snooze_action
                                ),
                                snoozePendingIntent
                        )
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(false)
                        .setCategory(
                                NotificationCompat.CATEGORY_REMINDER
                        );

        NotificationManagerCompat
                .from(context)
                .notify(
                        notificationId,
                        notificationBuilder.build()
                );
    }

    private int getNotificationId(
            boolean testReminder,
            boolean snoozedReminder
    ) {
        if (snoozedReminder) {
            return SNOOZED_NOTIFICATION_ID;
        }

        if (testReminder) {
            return TEST_NOTIFICATION_ID;
        }

        return DAILY_NOTIFICATION_ID;
    }

    private int getNotificationTitle(
            boolean testReminder,
            boolean snoozedReminder
    ) {
        if (snoozedReminder) {
            return R.string
                    .study_reminder_snoozed_notification_title;
        }

        if (testReminder) {
            return R.string
                    .study_reminder_test_notification_title;
        }

        return R.string.study_reminder_notification_title;
    }

    private int getNotificationMessage(
            boolean testReminder,
            boolean snoozedReminder
    ) {
        if (snoozedReminder) {
            return R.string
                    .study_reminder_snoozed_notification_message;
        }

        if (testReminder) {
            return R.string
                    .study_reminder_test_notification_message;
        }

        return R.string
                .study_reminder_notification_message;
    }

    @NonNull
    private PendingIntent createSmartPlanPendingIntent(
            @NonNull Context context,
            int notificationId
    ) {
        Intent smartPlanIntent = new Intent(
                context,
                SmartStudyPlanActivity.class
        );

        smartPlanIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        return PendingIntent.getActivity(
                context,
                notificationId,
                smartPlanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @NonNull
    private PendingIntent createSnoozePendingIntent(
            @NonNull Context context,
            int notificationId
    ) {
        Intent snoozeIntent = new Intent(
                context,
                StudyReminderActionReceiver.class
        );

        snoozeIntent.setAction(
                StudyReminderActionReceiver
                        .ACTION_SNOOZE_REMINDER
        );

        snoozeIntent.putExtra(
                StudyReminderActionReceiver
                        .EXTRA_NOTIFICATION_ID,
                notificationId
        );

        return PendingIntent.getBroadcast(
                context,
                notificationId + 1000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void createNotificationChannel(
            @NonNull Context context
    ) {
        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel notificationChannel =
                new NotificationChannel(
                        CHANNEL_ID,
                        context.getString(
                                R.string.study_reminder_channel_name
                        ),
                        NotificationManager.IMPORTANCE_DEFAULT
                );

        notificationChannel.setDescription(
                context.getString(
                        R.string.study_reminder_channel_description
                )
        );

        NotificationManager notificationManager =
                context.getSystemService(
                        NotificationManager.class
                );

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(
                    notificationChannel
            );
        }
    }
}