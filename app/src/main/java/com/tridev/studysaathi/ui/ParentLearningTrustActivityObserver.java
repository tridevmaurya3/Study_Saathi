package com.tridev.studysaathi.ui;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.ParentDashboardActivity;
import com.tridev.studysaathi.R;
import com.tridev.studysaathi.data.learning.ParentLearningTrustSummaryStore;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;

/** Adds privacy-safe learning-memory insight to Parent Dashboard only. */
public final class ParentLearningTrustActivityObserver
        implements Application.ActivityLifecycleCallbacks {

    private static final String TRUST_MARKER = "\n\nLearning Trust •";

    private ParentLearningTrustActivityObserver() { }

    public static void register(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(
                new ParentLearningTrustActivityObserver()
        );
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (!(activity instanceof ParentDashboardActivity)) return;
        renderForActiveStudent(activity);
    }

    private void renderForActiveStudent(@NonNull Activity activity) {
        TextView insight = activity.findViewById(R.id.textParentCitationInsight);
        if (insight == null) return;

        new StudentProfileRepository(activity).getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(@Nullable StudentProfileEntity profile) {
                        if (profile == null || activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }

                        ParentLearningTrustSummaryStore.Summary summary =
                                new ParentLearningTrustSummaryStore(activity)
                                        .getSummary(profile.getProfileId());

                        CharSequence existing = insight.getText();
                        String base = existing == null ? "" : existing.toString();
                        int markerAt = base.indexOf(TRUST_MARKER);
                        if (markerAt >= 0) {
                            base = base.substring(0, markerAt);
                        }

                        insight.setText(
                                base.trim()
                                        + TRUST_MARKER
                                        + summary.buildParentDisplayText()
                                                .substring("Learning Trust •".length())
                        );
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        // Parent Dashboard remains usable when learning memory is unavailable.
                    }
                }
        );
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) { }
    @Override public void onActivityStarted(@NonNull Activity activity) { }
    @Override public void onActivityPaused(@NonNull Activity activity) { }
    @Override public void onActivityStopped(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity,
                                                      @NonNull Bundle outState) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
