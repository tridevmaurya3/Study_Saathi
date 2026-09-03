package com.tridev.studysaathi.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Existing page header को scroll के दौरान स्थिर रखता है।
 *
 * यह controller कोई नया Back या Hamburger view नहीं बनाता। हर Activity का
 * layout-defined navigation control और उसका पुराना click listener canonical
 * navigation रहता है।
 */
public final class PersistentNavigationController {

    @NonNull
    private static final Map<Activity, Session> SESSIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PersistentNavigationController() {
    }

    public static void attach(@NonNull Activity activity) {
        if (activity.isFinishing() || SESSIONS.containsKey(activity)) {
            return;
        }

        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }

        content.post(() -> attachAfterLayout(activity, (ViewGroup) content));
    }

    private static void attachAfterLayout(
            @NonNull Activity activity,
            @NonNull ViewGroup content
    ) {
        if (activity.isFinishing() || SESSIONS.containsKey(activity)) {
            return;
        }

        PinnedHeader pinnedHeader = findPinnedHeader(content);
        if (pinnedHeader == null) {
            return;
        }

        Session session = new Session(content, pinnedHeader);
        SESSIONS.put(activity, session);
        content.getViewTreeObserver().addOnScrollChangedListener(session);
        session.onScrollChanged();
    }

    public static void detach(@NonNull Activity activity) {
        Session session = SESSIONS.remove(activity);
        if (session == null) {
            return;
        }

        ViewTreeObserver observer =
                session.observerHost.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnScrollChangedListener(session);
        }

        session.pinnedHeader.header.setTranslationY(0F);
    }

    @Nullable
    private static PinnedHeader findPinnedHeader(@NonNull View view) {
        if (view instanceof ScrollView
                || view instanceof NestedScrollView) {
            ViewGroup scrollContainer = (ViewGroup) view;
            View navigationView = findNavigationView(scrollContainer);
            View header = findTopLevelHeader(scrollContainer, navigationView);

            if (navigationView != null && header != null) {
                return new PinnedHeader(scrollContainer, header);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                PinnedHeader result = findPinnedHeader(group.getChildAt(index));
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    @Nullable
    private static View findNavigationView(@NonNull View view) {
        if (isDeclaredNavigationView(view)) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                View result = findNavigationView(group.getChildAt(index));
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private static boolean isDeclaredNavigationView(@NonNull View view) {
        if (view instanceof Toolbar
                && ((Toolbar) view).getNavigationIcon() != null) {
            return true;
        }

        int viewId = view.getId();
        if (viewId == View.NO_ID) {
            return false;
        }

        String entryName;
        try {
            entryName = view.getResources()
                    .getResourceEntryName(viewId)
                    .toLowerCase(Locale.ROOT);
        } catch (RuntimeException exception) {
            return false;
        }

        return entryName.equals("buttonback")
                || entryName.equals("backbutton")
                || entryName.endsWith("backbutton")
                || entryName.endsWith("helpback")
                || entryName.endsWith("familyback")
                || entryName.equals("carddashboardmenu")
                || entryName.equals("buttonparentmenu");
    }

    @Nullable
    private static View findTopLevelHeader(
            @NonNull ViewGroup scrollContainer,
            @Nullable View navigationView
    ) {
        if (navigationView == null || scrollContainer.getChildCount() == 0) {
            return null;
        }

        View contentRoot = scrollContainer.getChildAt(0);
        View current = navigationView;

        while (current != null) {
            ViewParent parent = current.getParent();
            if (parent == contentRoot) {
                return current;
            }
            if (!(parent instanceof View)) {
                return null;
            }
            current = (View) parent;
        }

        return null;
    }

    private static final class Session
            implements ViewTreeObserver.OnScrollChangedListener {

        @NonNull
        private final View observerHost;

        @NonNull
        private final PinnedHeader pinnedHeader;

        private Session(
                @NonNull View observerHost,
                @NonNull PinnedHeader pinnedHeader
        ) {
            this.observerHost = observerHost;
            this.pinnedHeader = pinnedHeader;
        }

        @Override
        public void onScrollChanged() {
            int scrollY = pinnedHeader.scrollContainer.getScrollY();
            pinnedHeader.header.setTranslationY(Math.max(0, scrollY));
            if (scrollY > 0) {
                pinnedHeader.header.bringToFront();
            }
        }
    }

    private static final class PinnedHeader {

        @NonNull
        private final ViewGroup scrollContainer;

        @NonNull
        private final View header;

        private PinnedHeader(
                @NonNull ViewGroup scrollContainer,
                @NonNull View header
        ) {
            this.scrollContainer = scrollContainer;
            this.header = header;
        }
    }
}
