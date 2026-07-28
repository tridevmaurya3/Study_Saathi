package com.tridev.studysaathi.data.ai;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Firebase AI quota cooldown को Android UI पर live countdown
 * के रूप में दिखाने वाला lifecycle-controlled controller।
 *
 * यह class:
 *
 * 1. Cooldown समाप्त होने का exact timestamp स्वीकार करती है।
 * 2. हर displayed second पर remaining time callback देती है।
 * 3. Activity को countdown message और button state update करने देती है।
 * 4. Cooldown पूरा होते ही completion callback देती है।
 * 5. पुराने countdown को हटाकर नया countdown शुरू कर सकती है।
 * 6. Activity destroy होने पर cancel करके memory leak रोकती है।
 *
 * Activity को onDestroy() में cancel() अवश्य call करना चाहिए।
 */
public final class FirebaseAiQuotaCountdownController {

    private static final long MINIMUM_CALLBACK_DELAY_MILLIS =
            100L;

    private static final long MAXIMUM_CALLBACK_DELAY_MILLIS =
            1000L;

    @NonNull
    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    @Nullable
    private CountdownListener countdownListener;

    private long cooldownUntilEpochMillis;

    private long lastDeliveredRemainingSeconds =
            -1L;

    private boolean countdownRunning;

    private int countdownGeneration;

    @NonNull
    private final Runnable countdownRunnable =
            this::dispatchCountdownUpdate;

    /**
     * नया countdown शुरू करता है।
     *
     * इस method को किसी भी thread से call किया जा सकता है।
     * वास्तविक UI countdown main thread पर चलेगा।
     */
    public void start(
            long cooldownUntilEpochMillis,
            @NonNull CountdownListener countdownListener
    ) {
        int requestedGeneration =
                ++countdownGeneration;

        mainHandler.post(
                () -> startOnMainThread(
                        cooldownUntilEpochMillis,
                        countdownListener,
                        requestedGeneration
                )
        );
    }

    /**
     * चालू countdown को रोकता है।
     *
     * Activity के onDestroy() में इसे call करें।
     */
    public void cancel() {
        int cancelledGeneration =
                ++countdownGeneration;

        mainHandler.post(
                () -> cancelOnMainThread(
                        cancelledGeneration
                )
        );
    }

    /**
     * Current countdown active है या नहीं।
     *
     * यह value UI/main-thread usage के लिए है।
     */
    public boolean isCountdownRunning() {
        return countdownRunning;
    }

    /**
     * Cooldown समाप्त होने का वर्तमान timestamp।
     */
    public long getCooldownUntilEpochMillis() {
        return cooldownUntilEpochMillis;
    }

    /**
     * वर्तमान शेष seconds calculate करता है।
     */
    public long calculateRemainingSeconds() {
        return calculateRemainingSeconds(
                cooldownUntilEpochMillis,
                System.currentTimeMillis()
        );
    }

    private void startOnMainThread(
            long requestedCooldownUntilEpochMillis,
            @NonNull CountdownListener requestedListener,
            int requestedGeneration
    ) {
        if (requestedGeneration
                != countdownGeneration) {

            return;
        }

        mainHandler.removeCallbacks(
                countdownRunnable
        );

        countdownListener =
                requestedListener;

        cooldownUntilEpochMillis =
                requestedCooldownUntilEpochMillis;

        lastDeliveredRemainingSeconds =
                -1L;

        long remainingSeconds =
                calculateRemainingSeconds();

        if (remainingSeconds <= 0L) {
            countdownRunning =
                    false;

            CountdownListener completionListener =
                    countdownListener;

            countdownListener =
                    null;

            cooldownUntilEpochMillis =
                    0L;

            if (completionListener != null) {
                completionListener
                        .onCooldownFinished();
            }

            return;
        }

        countdownRunning =
                true;

        dispatchCountdownUpdate();
    }

    private void cancelOnMainThread(
            int cancelledGeneration
    ) {
        if (cancelledGeneration
                != countdownGeneration) {

            return;
        }

        mainHandler.removeCallbacks(
                countdownRunnable
        );

        countdownRunning =
                false;

        countdownListener =
                null;

        cooldownUntilEpochMillis =
                0L;

        lastDeliveredRemainingSeconds =
                -1L;
    }

    private void dispatchCountdownUpdate() {
        if (!countdownRunning) {
            return;
        }

        CountdownListener currentListener =
                countdownListener;

        if (currentListener == null) {
            stopWithoutCompletionCallback();
            return;
        }

        long currentTimeMillis =
                System.currentTimeMillis();

        long remainingMilliseconds =
                cooldownUntilEpochMillis
                        - currentTimeMillis;

        long remainingSeconds =
                calculateRemainingSeconds(
                        cooldownUntilEpochMillis,
                        currentTimeMillis
                );

        if (remainingSeconds <= 0L) {
            finishCountdown(
                    currentListener
            );

            return;
        }

        if (remainingSeconds
                != lastDeliveredRemainingSeconds) {

            lastDeliveredRemainingSeconds =
                    remainingSeconds;

            currentListener.onCooldownTick(
                    remainingSeconds,
                    cooldownUntilEpochMillis
            );
        }

        long callbackDelayMillis =
                calculateNextCallbackDelay(
                        remainingMilliseconds,
                        remainingSeconds
                );

        mainHandler.postDelayed(
                countdownRunnable,
                callbackDelayMillis
        );
    }

    private void finishCountdown(
            @NonNull CountdownListener completionListener
    ) {
        mainHandler.removeCallbacks(
                countdownRunnable
        );

        countdownRunning =
                false;

        countdownListener =
                null;

        cooldownUntilEpochMillis =
                0L;

        lastDeliveredRemainingSeconds =
                -1L;

        completionListener
                .onCooldownFinished();
    }

    private void stopWithoutCompletionCallback() {
        mainHandler.removeCallbacks(
                countdownRunnable
        );

        countdownRunning =
                false;

        countdownListener =
                null;

        cooldownUntilEpochMillis =
                0L;

        lastDeliveredRemainingSeconds =
                -1L;
    }

    private long calculateNextCallbackDelay(
            long remainingMilliseconds,
            long remainingSeconds
    ) {
        long millisecondsUntilNextDisplayedSecond =
                remainingMilliseconds
                        - (
                        remainingSeconds - 1L
                ) * 1000L;

        return Math.max(
                MINIMUM_CALLBACK_DELAY_MILLIS,
                Math.min(
                        MAXIMUM_CALLBACK_DELAY_MILLIS,
                        millisecondsUntilNextDisplayedSecond
                )
        );
    }

    private long calculateRemainingSeconds(
            long cooldownUntilEpochMillis,
            long currentTimeMillis
    ) {
        long remainingMilliseconds =
                cooldownUntilEpochMillis
                        - currentTimeMillis;

        if (remainingMilliseconds <= 0L) {
            return 0L;
        }

        return Math.max(
                1L,
                (long) Math.ceil(
                        remainingMilliseconds
                                / 1000.0d
                )
        );
    }

    /**
     * Activity या Fragment को countdown events देने वाला callback।
     */
    public interface CountdownListener {

        /**
         * Countdown के प्रत्येक displayed second पर call होगा।
         */
        void onCooldownTick(
                long remainingSeconds,
                long cooldownUntilEpochMillis
        );

        /**
         * Cooldown पूर्ण होने पर केवल एक बार call होगा।
         */
        void onCooldownFinished();
    }
}