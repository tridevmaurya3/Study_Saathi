package com.tridev.studysaathi.ui;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.DashboardActivity;
import com.tridev.studysaathi.MainActivity;
import com.tridev.studysaathi.ParentDashboardActivity;
import com.tridev.studysaathi.R;
import com.tridev.studysaathi.UserModeSelectionActivity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class PersistentNavigationController {

    @NonNull
    private static final Map<Activity, Session> SESSIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PersistentNavigationController() {
    }

    public static void attach(@NonNull Activity activity) {
        if (activity instanceof MainActivity
                || activity instanceof UserModeSelectionActivity
                || activity.isFinishing()
                || SESSIONS.containsKey(activity)) {
            return;
        }

        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) {
            return;
        }

        FrameLayout host = (FrameLayout) content;
        boolean studentDashboard = activity instanceof DashboardActivity;
        boolean parentDashboard = activity instanceof ParentDashboardActivity;
        boolean dashboard = studentDashboard || parentDashboard;
        TextView back = new TextView(activity);
        back.setText(dashboard ? "☰" : "←");
        back.setTextSize(25);
        back.setTextColor(activity.getColor(R.color.ss_primary));
        back.setGravity(Gravity.CENTER);
        back.setBackgroundResource(R.drawable.bg_persistent_back);
        back.setElevation(dp(activity, 14));
        back.setContentDescription(dashboard ? "Open study menu" : "Back");
        back.setVisibility(View.GONE);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dp(activity, 52),
                dp(activity, 52),
                Gravity.TOP | Gravity.START
        );
        params.leftMargin = dp(activity, 8);
        params.topMargin = dp(activity, 8);
        host.addView(back, params);

        back.setOnClickListener(view -> {
            if (dashboard) {
                if (parentDashboard) {
                    ((ParentDashboardActivity) activity)
                            .openParentNavigationDrawer();
                    return;
                }
                View dashboardRoot = activity.findViewById(
                        R.id.dashboardRoot
                );
                if (dashboardRoot instanceof DrawerLayout) {
                    ((DrawerLayout) dashboardRoot)
                            .openDrawer(GravityCompat.START);
                }
                return;
            }
            if (activity instanceof AppCompatActivity) {
                ((AppCompatActivity) activity)
                        .getOnBackPressedDispatcher()
                        .onBackPressed();
            } else {
                activity.onBackPressed();
            }
        });

        Session session = new Session(host, back);
        SESSIONS.put(activity, session);
        host.getViewTreeObserver().addOnScrollChangedListener(session);
        host.post(session::onScrollChanged);
    }

    public static void detach(@NonNull Activity activity) {
        Session session = SESSIONS.remove(activity);
        if (session == null) {
            return;
        }
        ViewTreeObserver observer = session.host.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnScrollChangedListener(session);
        }
        session.host.removeView(session.back);
    }

    private static final class Session
            implements ViewTreeObserver.OnScrollChangedListener {

        @NonNull
        private final FrameLayout host;
        @NonNull
        private final TextView back;

        private Session(
                @NonNull FrameLayout host,
                @NonNull TextView back
        ) {
            this.host = host;
            this.back = back;
        }

        @Override
        public void onScrollChanged() {
            boolean scrolled = hasScrolledContent(host, back);
            back.setVisibility(scrolled ? View.VISIBLE : View.GONE);
        }
    }

    private static boolean hasScrolledContent(
            @NonNull View view,
            @NonNull View ignored
    ) {
        if (view == ignored) {
            return false;
        }
        if ((view instanceof ScrollView
                || view instanceof NestedScrollView)
                && view.getScrollY() > dp(view.getContext(), 24)) {
            return true;
        }
        if (view instanceof RecyclerView
                && view.canScrollVertically(-1)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                if (hasScrolledContent(group.getChildAt(index), ignored)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int dp(@NonNull android.content.Context context, int value) {
        return Math.round(
                value * context.getResources().getDisplayMetrics().density
        );
    }
}
