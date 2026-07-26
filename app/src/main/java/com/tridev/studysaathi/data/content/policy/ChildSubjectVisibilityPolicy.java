package com.tridev.studysaathi.data.content.policy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ChildSubjectVisibilityPolicy {

    public static final String CONTENT_SOURCE_SCHOOL_BOOK =
            "SCHOOL_BOOK";

    public static final String REASON_VISIBLE =
            "VISIBLE";

    public static final String REASON_INVALID_SUBJECT =
            "INVALID_SUBJECT";

    public static final String REASON_SUBJECT_DISABLED =
            "SUBJECT_DISABLED";

    public static final String REASON_SUBJECT_NAME_MISSING =
            "SUBJECT_NAME_MISSING";

    public static final String REASON_WRONG_CONTENT_SOURCE =
            "WRONG_CONTENT_SOURCE";

    public static final String REASON_EXACT_BOOK_PENDING =
            "EXACT_BOOK_PENDING";

    private ChildSubjectVisibilityPolicy() {
        /*
         * Utility class.
         */
    }

    /**
     * Child Mode में दिखाए जाने योग्य subjects की सुरक्षित list बनाता है।
     *
     * Original list में कोई बदलाव नहीं किया जाता।
     */
    @NonNull
    public static List<SchoolSubjectEntity> filterVisibleSubjects(
            @Nullable List<SchoolSubjectEntity> schoolSubjects
    ) {
        List<SchoolSubjectEntity> visibleSubjects =
                new ArrayList<>();

        if (schoolSubjects == null
                || schoolSubjects.isEmpty()) {

            return Collections.unmodifiableList(
                    visibleSubjects
            );
        }

        for (SchoolSubjectEntity schoolSubject :
                schoolSubjects) {

            if (isVisibleInChildMode(
                    schoolSubject
            )) {
                visibleSubjects.add(
                        schoolSubject
                );
            }
        }

        visibleSubjects.sort(
                createSubjectComparator()
        );

        return Collections.unmodifiableList(
                visibleSubjects
        );
    }

    /**
     * जाँचता है कि कोई subject Child Mode में दिखाई देना चाहिए या नहीं।
     */
    public static boolean isVisibleInChildMode(
            @Nullable SchoolSubjectEntity schoolSubject
    ) {
        return evaluateVisibility(
                schoolSubject
        ).isVisible();
    }

    /**
     * Subject visibility का पूरा decision और उसका reason देता है।
     */
    @NonNull
    public static VisibilityDecision evaluateVisibility(
            @Nullable SchoolSubjectEntity schoolSubject
    ) {
        if (schoolSubject == null
                || schoolSubject.getSubjectRowId() <= 0L
                || schoolSubject.getProfileId() <= 0L) {

            return VisibilityDecision.hidden(
                    REASON_INVALID_SUBJECT,
                    "Valid school subject record उपलब्ध नहीं है।"
            );
        }

        if (!schoolSubject.isEnabled()) {
            return VisibilityDecision.hidden(
                    REASON_SUBJECT_DISABLED,
                    "Parent ने इस subject को Child Mode से hide किया है।"
            );
        }

        if (!hasValidSubjectName(
                schoolSubject
        )) {
            return VisibilityDecision.hidden(
                    REASON_SUBJECT_NAME_MISSING,
                    "Subject का valid name उपलब्ध नहीं है।"
            );
        }

        if (!isExactSchoolBookContentSource(
                schoolSubject
        )) {
            return VisibilityDecision.hidden(
                    REASON_WRONG_CONTENT_SOURCE,
                    "यह subject exact school-book curriculum source से जुड़ा नहीं है।"
            );
        }

        if (!hasConfirmedExactBook(
                schoolSubject
        )) {
            return VisibilityDecision.hidden(
                    REASON_EXACT_BOOK_PENDING,
                    "इस subject की exact school book अभी confirm नहीं हुई है।"
            );
        }

        return VisibilityDecision.visible(
                "Subject और exact school book Child Mode के लिए तैयार हैं।"
        );
    }

    /**
     * जाँचता है कि subject की exact school book confirm है।
     *
     * Manual book में ISBN या Book Code optional हो सकता है,
     * इसलिए confirmed book पहचानने के लिए Book Name आवश्यक रखा गया है।
     */
    public static boolean hasConfirmedExactBook(
            @Nullable SchoolSubjectEntity schoolSubject
    ) {
        if (schoolSubject == null) {
            return false;
        }

        String bookName =
                safeText(
                        schoolSubject.getBookName()
                );

        return !bookName.isEmpty();
    }

    /**
     * जाँचता है कि subject का content source exact school book है।
     */
    public static boolean isExactSchoolBookContentSource(
            @Nullable SchoolSubjectEntity schoolSubject
    ) {
        if (schoolSubject == null) {
            return false;
        }

        String contentSource =
                safeText(
                        schoolSubject.getContentSource()
                );

        return CONTENT_SOURCE_SCHOOL_BOOK.equalsIgnoreCase(
                contentSource
        );
    }

    /**
     * Subject का display name देता है।
     */
    @NonNull
    public static String getSubjectDisplayName(
            @Nullable SchoolSubjectEntity schoolSubject
    ) {
        if (schoolSubject == null) {
            return "School Subject";
        }

        String englishName =
                safeText(
                        schoolSubject.getSubjectNameEnglish()
                );

        if (!englishName.isEmpty()) {
            return englishName;
        }

        String hindiName =
                safeText(
                        schoolSubject.getSubjectNameHindi()
                );

        if (!hindiName.isEmpty()) {
            return hindiName;
        }

        return "School Subject";
    }

    /**
     * पूरी subject list की Child Mode readiness summary बनाता है।
     */
    @NonNull
    public static VisibilitySummary createVisibilitySummary(
            @Nullable List<SchoolSubjectEntity> schoolSubjects
    ) {
        int totalSubjectCount =
                0;

        int visibleSubjectCount =
                0;

        int disabledSubjectCount =
                0;

        int pendingBookCount =
                0;

        int invalidSubjectCount =
                0;

        int wrongContentSourceCount =
                0;

        if (schoolSubjects != null) {
            for (SchoolSubjectEntity schoolSubject :
                    schoolSubjects) {

                totalSubjectCount++;

                VisibilityDecision decision =
                        evaluateVisibility(
                                schoolSubject
                        );

                if (decision.isVisible()) {
                    visibleSubjectCount++;
                    continue;
                }

                switch (decision.getReasonCode()) {
                    case REASON_SUBJECT_DISABLED:
                        disabledSubjectCount++;
                        break;

                    case REASON_EXACT_BOOK_PENDING:
                        pendingBookCount++;
                        break;

                    case REASON_WRONG_CONTENT_SOURCE:
                        wrongContentSourceCount++;
                        break;

                    case REASON_INVALID_SUBJECT:
                    case REASON_SUBJECT_NAME_MISSING:
                    default:
                        invalidSubjectCount++;
                        break;
                }
            }
        }

        return new VisibilitySummary(
                totalSubjectCount,
                visibleSubjectCount,
                disabledSubjectCount,
                pendingBookCount,
                invalidSubjectCount,
                wrongContentSourceCount
        );
    }

    private static boolean hasValidSubjectName(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        return !safeText(
                schoolSubject.getSubjectNameEnglish()
        ).isEmpty()
                || !safeText(
                schoolSubject.getSubjectNameHindi()
        ).isEmpty();
    }

    @NonNull
    private static Comparator<SchoolSubjectEntity>
    createSubjectComparator() {
        return (firstSubject, secondSubject) -> {
            int firstSortOrder =
                    Math.max(
                            0,
                            firstSubject.getSortOrder()
                    );

            int secondSortOrder =
                    Math.max(
                            0,
                            secondSubject.getSortOrder()
                    );

            int sortOrderComparison =
                    Integer.compare(
                            firstSortOrder,
                            secondSortOrder
                    );

            if (sortOrderComparison != 0) {
                return sortOrderComparison;
            }

            String firstName =
                    normalizeText(
                            getSubjectDisplayName(
                                    firstSubject
                            )
                    );

            String secondName =
                    normalizeText(
                            getSubjectDisplayName(
                                    secondSubject
                            )
                    );

            return firstName.compareTo(
                    secondName
            );
        };
    }

    @NonNull
    private static String normalizeText(
            @Nullable Object value
    ) {
        return safeText(
                value
        )
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    public static final class VisibilityDecision {

        private final boolean visible;

        @NonNull
        private final String reasonCode;

        @NonNull
        private final String message;

        private VisibilityDecision(
                boolean visible,
                @NonNull String reasonCode,
                @NonNull String message
        ) {
            this.visible =
                    visible;

            this.reasonCode =
                    reasonCode;

            this.message =
                    message;
        }

        @NonNull
        private static VisibilityDecision visible(
                @NonNull String message
        ) {
            return new VisibilityDecision(
                    true,
                    REASON_VISIBLE,
                    message
            );
        }

        @NonNull
        private static VisibilityDecision hidden(
                @NonNull String reasonCode,
                @NonNull String message
        ) {
            return new VisibilityDecision(
                    false,
                    reasonCode,
                    message
            );
        }

        public boolean isVisible() {
            return visible;
        }

        @NonNull
        public String getReasonCode() {
            return reasonCode;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        public boolean isBookPending() {
            return REASON_EXACT_BOOK_PENDING.equals(
                    reasonCode
            );
        }

        public boolean isParentHidden() {
            return REASON_SUBJECT_DISABLED.equals(
                    reasonCode
            );
        }
    }

    public static final class VisibilitySummary {

        private final int totalSubjectCount;

        private final int visibleSubjectCount;

        private final int disabledSubjectCount;

        private final int pendingBookCount;

        private final int invalidSubjectCount;

        private final int wrongContentSourceCount;

        private VisibilitySummary(
                int totalSubjectCount,
                int visibleSubjectCount,
                int disabledSubjectCount,
                int pendingBookCount,
                int invalidSubjectCount,
                int wrongContentSourceCount
        ) {
            this.totalSubjectCount =
                    Math.max(
                            0,
                            totalSubjectCount
                    );

            this.visibleSubjectCount =
                    Math.max(
                            0,
                            visibleSubjectCount
                    );

            this.disabledSubjectCount =
                    Math.max(
                            0,
                            disabledSubjectCount
                    );

            this.pendingBookCount =
                    Math.max(
                            0,
                            pendingBookCount
                    );

            this.invalidSubjectCount =
                    Math.max(
                            0,
                            invalidSubjectCount
                    );

            this.wrongContentSourceCount =
                    Math.max(
                            0,
                            wrongContentSourceCount
                    );
        }

        public int getTotalSubjectCount() {
            return totalSubjectCount;
        }

        public int getVisibleSubjectCount() {
            return visibleSubjectCount;
        }

        public int getDisabledSubjectCount() {
            return disabledSubjectCount;
        }

        public int getPendingBookCount() {
            return pendingBookCount;
        }

        public int getInvalidSubjectCount() {
            return invalidSubjectCount;
        }

        public int getWrongContentSourceCount() {
            return wrongContentSourceCount;
        }

        public int getHiddenSubjectCount() {
            return Math.max(
                    0,
                    totalSubjectCount
                            - visibleSubjectCount
            );
        }

        public boolean hasVisibleSubjects() {
            return visibleSubjectCount > 0;
        }

        public boolean isChildCurriculumReady() {
            return visibleSubjectCount > 0
                    && pendingBookCount == 0
                    && invalidSubjectCount == 0
                    && wrongContentSourceCount == 0;
        }

        @NonNull
        public String createStatusMessage() {
            if (totalSubjectCount <= 0) {
                return "School curriculum में अभी कोई subject नहीं जोड़ा गया है।";
            }

            if (visibleSubjectCount <= 0
                    && pendingBookCount > 0) {

                return pendingBookCount
                        + (pendingBookCount == 1
                        ? " subject की exact school book pending है।"
                        : " subjects की exact school books pending हैं।");
            }

            if (visibleSubjectCount <= 0) {
                return "Child Mode के लिए अभी कोई visible subject तैयार नहीं है।";
            }

            if (pendingBookCount > 0) {
                return visibleSubjectCount
                        + (visibleSubjectCount == 1
                        ? " subject तैयार है और "
                        : " subjects तैयार हैं और ")
                        + pendingBookCount
                        + (pendingBookCount == 1
                        ? " book pending है।"
                        : " books pending हैं।");
            }

            return visibleSubjectCount
                    + (visibleSubjectCount == 1
                    ? " confirmed subject Child Mode के लिए तैयार है।"
                    : " confirmed subjects Child Mode के लिए तैयार हैं।");
        }
    }
}