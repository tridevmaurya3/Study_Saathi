package com.tridev.studysaathi.ui;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import com.tridev.studysaathi.R;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Scroll content में बने existing page header को वास्तविक fixed header बनाता है।
 *
 * Controller कोई नया Back/Hamburger control नहीं बनाता और किसी header को
 * translate/float नहीं करता। Existing header, title और click listener को scroll
 * container के बाहर सुरक्षित रूप से re-parent किया जाता है।
 */
public final class PersistentNavigationController {

    private static final int DEFAULT_HEADER_HEIGHT_DP = 64;
    private static final int HEADER_ELEVATION_DP = 8;

    @NonNull
    private static final Map<Activity, View> PINNED_HEADERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PersistentNavigationController() {
    }

    public static void attach(@NonNull Activity activity) {
        if (activity.isFinishing() || PINNED_HEADERS.containsKey(activity)) {
            return;
        }

        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        content.post(() -> pinAfterLayout(activity, (ViewGroup) content));
    }

    private static void pinAfterLayout(
            @NonNull Activity activity,
            @NonNull ViewGroup content
    ) {
        if (activity.isFinishing() || PINNED_HEADERS.containsKey(activity)) {
            return;
        }

        PinnedHeader candidate = findPinnedHeader(content);
        if (candidate == null) {
            return;
        }

        View header = candidate.header;
        ViewParent currentParent = header.getParent();
        ViewParent scrollParent = candidate.scrollContainer.getParent();
        if (!(currentParent instanceof ViewGroup)
                || !(scrollParent instanceof ViewGroup)) {
            return;
        }

        ViewGroup originalParent = (ViewGroup) currentParent;
        ViewGroup fixedHost = (ViewGroup) scrollParent;
        int originalIndex = originalParent.indexOfChild(header);
        if (originalIndex < 0 || fixedHost == originalParent) {
            return;
        }

        int headerHeight = header.getHeight() > 0
                ? header.getHeight()
                : dp(activity, DEFAULT_HEADER_HEIGHT_DP);
        ViewGroup.LayoutParams originalParams = header.getLayoutParams();

        originalParent.removeViewAt(originalIndex);
        Space reservedHeaderSpace = new Space(activity);
        reservedHeaderSpace.setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        );
        originalParent.addView(
                reservedHeaderSpace,
                originalIndex,
                createSpacerParams(originalParams, headerHeight)
        );

        header.setTranslationX(0F);
        header.setTranslationY(0F);
        header.setMinimumHeight(dp(activity, 56));
        header.setBackgroundColor(activity.getColor(R.color.ss_background));
        if (!(header instanceof Toolbar)) {
            header.setPaddingRelative(
                    Math.max(header.getPaddingStart(), dp(activity, 12)),
                    header.getPaddingTop(),
                    Math.max(header.getPaddingEnd(), dp(activity, 12)),
                    header.getPaddingBottom()
            );
        }
        ViewCompat.setElevation(
                header,
                Math.max(
                        ViewCompat.getElevation(header),
                        dp(activity, HEADER_ELEVATION_DP)
                )
        );
        fixedHost.addView(
                header,
                createFixedHeaderParams(fixedHost, headerHeight)
        );
        PINNED_HEADERS.put(activity, header);
    }

    public static void detach(@NonNull Activity activity) {
        /* Header Activity के जीवनभर fixed रहता है; केवल tracking हटती है। */
        PINNED_HEADERS.remove(activity);
    }

    @NonNull
    private static ViewGroup.LayoutParams createSpacerParams(
            @Nullable ViewGroup.LayoutParams original,
            int headerHeight
    ) {
        if (original instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams result =
                    new LinearLayout.LayoutParams(
                            (LinearLayout.LayoutParams) original
                    );
            result.height = headerHeight;
            return result;
        }
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                headerHeight
        );
    }

    @NonNull
    private static ViewGroup.LayoutParams createFixedHeaderParams(
            @NonNull ViewGroup host,
            int headerHeight
    ) {
        if (host instanceof CoordinatorLayout) {
            CoordinatorLayout.LayoutParams params =
                    new CoordinatorLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            headerHeight
                    );
            params.gravity = Gravity.TOP;
            return params;
        }
        if (host instanceof FrameLayout) {
            return new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    headerHeight,
                    Gravity.TOP
            );
        }
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                headerHeight
        );
    }

    @Nullable
    private static PinnedHeader findPinnedHeader(@NonNull View view) {
        if (view instanceof ScrollView || view instanceof NestedScrollView) {
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
                || (entryName.contains("back")
                        && entryName.endsWith("button"))
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

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(
                value
                        * activity.getResources()
                        .getDisplayMetrics()
                        .density
        );
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
