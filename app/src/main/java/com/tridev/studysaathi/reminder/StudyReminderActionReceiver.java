package com.tridev.studysaathi.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.tridev.studysaathi.R;

public class StudyReminderActionReceiver
        extends BroadcastReceiver {

    public static final String ACTION_SNOOZE_REMINDER =
            "com.tridev.studysaathi.action.SNOOZE_STUDY_REMINDER";

    public static final String EXTRA_NOTIFICATION_ID =
            "extra_study_reminder_notification_id";

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {
        if (context == null
                || intent == null
                || !ACTION_SNOOZE_REMINDER.equals(
                intent.getAction()
        )) {
            return;
        }

        int notificationId =
                intent.getIntExtra(
                        EXTRA_NOTIFICATION_ID,
                        -1
                );

        if (notificationId > 0) {
            NotificationManagerCompat
                    .from(context)
                    .cancel(notificationId);
        }

        StudyReminderScheduler
                .scheduleSnoozedReminder(
                        context
                );

        showSnoozeConfirmation(context);
    }

    private void showSnoozeConfirmation(
            @NonNull Context context
    ) {
        Toast.makeText(
                context.getApplicationContext(),
                R.string.study_reminder_snoozed_confirmation,
                Toast.LENGTH_SHORT
        ).show();
    }
}