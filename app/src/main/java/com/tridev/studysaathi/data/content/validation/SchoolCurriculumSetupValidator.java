package com.tridev.studysaathi.data.content.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SchoolCurriculumSetupValidator {

    private static final int MINIMUM_CLASS_NUMBER =
            1;

    private static final int MAXIMUM_CLASS_NUMBER =
            12;

    private SchoolCurriculumSetupValidator() {
        /*
         * Utility class.
         */
    }

    /**
     * Parent Curriculum Setup screen की current values
     * validate करता है।
     *
     * School details और subjects valid होने पर setup
     * database में save किया जा सकता है।
     *
     * Child Mode तभी ready माना जाएगा जब हर enabled
     * subject की exact school book जुड़ी हो।
     */
    @NonNull
    public static ValidationResult validate(
            @Nullable CharSequence schoolName,
            @Nullable CharSequence schoolCode,
            @Nullable CharSequence educationBoard,
            @Nullable CharSequence classNumberText,
            @Nullable CharSequence section,
            @Nullable CharSequence academicSession,
            @Nullable CharSequence studyMedium,
            @Nullable List<SchoolSubjectEntity> subjects
    ) {
        String safeSchoolName =
                safeText(
                        schoolName
                );

        String safeSchoolCode =
                safeText(
                        schoolCode
                );

        String safeEducationBoard =
                safeText(
                        educationBoard
                );

        String safeClassNumberText =
                safeText(
                        classNumberText
                );

        String safeSection =
                safeText(
                        section
                );

        String safeAcademicSession =
                safeText(
                        academicSession
                );

        String safeStudyMedium =
                safeText(
                        studyMedium
                );

        List<SchoolSubjectEntity> safeSubjects =
                subjects == null
                        ? Collections.emptyList()
                        : subjects;

        String schoolNameError =
                "";

        String educationBoardError =
                "";

        String classNumberError =
                "";

        String academicSessionError =
                "";

        String studyMediumError =
                "";

        List<String> generalErrors =
                new ArrayList<>();

        if (safeSchoolName.isEmpty()) {
            schoolNameError =
                    "School name आवश्यक है।";
        }

        if (safeEducationBoard.isEmpty()) {
            educationBoardError =
                    "Education board या school pattern आवश्यक है।";
        }

        int classNumber =
                parseClassNumber(
                        safeClassNumberText
                );

        if (safeClassNumberText.isEmpty()) {
            classNumberError =
                    "Class number आवश्यक है।";

        } else if (classNumber
                < MINIMUM_CLASS_NUMBER
                || classNumber
                > MAXIMUM_CLASS_NUMBER) {

            classNumberError =
                    "Class number 1 से 12 के बीच होना चाहिए।";
        }

        if (safeAcademicSession.isEmpty()) {
            academicSessionError =
                    "Academic session आवश्यक है।";
        }

        if (safeStudyMedium.isEmpty()) {
            studyMediumError =
                    "Study medium आवश्यक है।";
        }

        int enabledSubjectCount =
                0;

        int confirmedBookCount =
                0;

        int pendingBookCount =
                0;

        int hiddenSubjectCount =
                0;

        int duplicateSubjectCount =
                0;

        Set<String> usedSubjectNames =
                new HashSet<>();

        Set<String> usedSubjectCodes =
                new HashSet<>();

        List<SchoolSubjectStatus> subjectStatuses =
                new ArrayList<>();

        for (SchoolSubjectEntity subject :
                safeSubjects) {

            if (subject == null) {
                continue;
            }

            String englishSubjectName =
                    safeText(
                            subject.getSubjectNameEnglish()
                    );

            String hindiSubjectName =
                    safeText(
                            subject.getSubjectNameHindi()
                    );

            String displaySubjectName =
                    firstNonBlank(
                            englishSubjectName,
                            hindiSubjectName,
                            "Unnamed Subject"
                    );

            String subjectCode =
                    safeText(
                            subject.getSubjectCode()
                    );

            String bookName =
                    safeText(
                            subject.getBookName()
                    );

            boolean subjectEnabled =
                    subject.isEnabled();

            boolean exactBookConfirmed =
                    !bookName.isEmpty();

            boolean duplicateSubject =
                    false;

            if (subjectEnabled) {
                enabledSubjectCount++;

                String normalizedEnglishName =
                        normalizeText(
                                englishSubjectName
                        );

                String normalizedHindiName =
                        normalizeText(
                                hindiSubjectName
                        );

                String normalizedSubjectCode =
                        normalizeText(
                                subjectCode
                        );

                if (!normalizedEnglishName.isEmpty()) {
                    if (!usedSubjectNames.add(
                            normalizedEnglishName
                    )) {
                        duplicateSubject =
                                true;
                    }
                }

                if (!normalizedHindiName.isEmpty()) {
                    if (!usedSubjectNames.add(
                            normalizedHindiName
                    )) {
                        duplicateSubject =
                                true;
                    }
                }

                if (!normalizedSubjectCode.isEmpty()) {
                    if (!usedSubjectCodes.add(
                            normalizedSubjectCode
                    )) {
                        duplicateSubject =
                                true;
                    }
                }

                if (exactBookConfirmed) {
                    confirmedBookCount++;

                } else {
                    pendingBookCount++;
                }

            } else {
                hiddenSubjectCount++;
            }

            if (duplicateSubject) {
                duplicateSubjectCount++;
            }

            subjectStatuses.add(
                    new SchoolSubjectStatus(
                            subject.getSubjectRowId(),
                            displaySubjectName,
                            subjectCode,
                            subjectEnabled,
                            exactBookConfirmed,
                            duplicateSubject,
                            bookName
                    )
            );
        }

        if (enabledSubjectCount <= 0) {
            generalErrors.add(
                    "कम-से-कम एक actual school subject जोड़ना आवश्यक है।"
            );
        }

        if (duplicateSubjectCount > 0) {
            generalErrors.add(
                    "Curriculum में duplicate subjects मौजूद हैं।"
            );
        }

        boolean schoolDetailsValid =
                schoolNameError.isEmpty()
                        && educationBoardError.isEmpty()
                        && classNumberError.isEmpty()
                        && academicSessionError.isEmpty()
                        && studyMediumError.isEmpty();

        boolean subjectSelectionValid =
                enabledSubjectCount > 0
                        && duplicateSubjectCount == 0;

        boolean canSaveSchoolSetup =
                schoolDetailsValid
                        && subjectSelectionValid;

        boolean allEnabledSubjectsHaveBooks =
                enabledSubjectCount > 0
                        && pendingBookCount == 0
                        && confirmedBookCount
                        == enabledSubjectCount;

        boolean readyForChildMode =
                canSaveSchoolSetup
                        && allEnabledSubjectsHaveBooks;

        String setupStatusMessage =
                createSetupStatusMessage(
                        canSaveSchoolSetup,
                        readyForChildMode,
                        enabledSubjectCount,
                        confirmedBookCount,
                        pendingBookCount
                );

        return new ValidationResult(
                safeSchoolName,
                safeSchoolCode,
                safeEducationBoard,
                classNumber,
                safeSection,
                safeAcademicSession,
                safeStudyMedium,
                schoolNameError,
                educationBoardError,
                classNumberError,
                academicSessionError,
                studyMediumError,
                schoolDetailsValid,
                subjectSelectionValid,
                canSaveSchoolSetup,
                readyForChildMode,
                enabledSubjectCount,
                confirmedBookCount,
                pendingBookCount,
                hiddenSubjectCount,
                duplicateSubjectCount,
                setupStatusMessage,
                generalErrors,
                subjectStatuses
        );
    }

    private static int parseClassNumber(
            @Nullable String classNumberText
    ) {
        String normalizedClassNumber =
                safeText(
                        classNumberText
                )
                        .replaceAll(
                                "[^0-9]",
                                ""
                        );

        if (normalizedClassNumber.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(
                    normalizedClassNumber
            );

        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @NonNull
    private static String createSetupStatusMessage(
            boolean canSaveSchoolSetup,
            boolean readyForChildMode,
            int enabledSubjectCount,
            int confirmedBookCount,
            int pendingBookCount
    ) {
        if (!canSaveSchoolSetup) {
            return "School details और actual subjects पूरा करें।";
        }

        if (readyForChildMode) {
            return enabledSubjectCount
                    + " subjects और उनकी exact books confirmed हैं। "
                    + "Curriculum बच्चे के लिए तैयार है।";
        }

        if (pendingBookCount > 0) {
            return confirmedBookCount
                    + " books confirmed हैं और "
                    + pendingBookCount
                    + " subjects की exact books अभी बाकी हैं।";
        }

        return "School curriculum details save किए जा सकते हैं।";
    }

    @NonNull
    private static String firstNonBlank(
            @Nullable String firstValue,
            @Nullable String secondValue,
            @NonNull String fallback
    ) {
        String safeFirstValue =
                safeText(
                        firstValue
                );

        if (!safeFirstValue.isEmpty()) {
            return safeFirstValue;
        }

        String safeSecondValue =
                safeText(
                        secondValue
                );

        return safeSecondValue.isEmpty()
                ? fallback
                : safeSecondValue;
    }

    @NonNull
    private static String normalizeText(
            @Nullable String value
    ) {
        return safeText(
                value
        )
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "&",
                        " and "
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
                : value.toString().trim();
    }

    public static final class ValidationResult {

        @NonNull
        private final String schoolName;

        @NonNull
        private final String schoolCode;

        @NonNull
        private final String educationBoard;

        private final int classNumber;

        @NonNull
        private final String section;

        @NonNull
        private final String academicSession;

        @NonNull
        private final String studyMedium;

        @NonNull
        private final String schoolNameError;

        @NonNull
        private final String educationBoardError;

        @NonNull
        private final String classNumberError;

        @NonNull
        private final String academicSessionError;

        @NonNull
        private final String studyMediumError;

        private final boolean schoolDetailsValid;

        private final boolean subjectSelectionValid;

        private final boolean canSaveSchoolSetup;

        private final boolean readyForChildMode;

        private final int enabledSubjectCount;

        private final int confirmedBookCount;

        private final int pendingBookCount;

        private final int hiddenSubjectCount;

        private final int duplicateSubjectCount;

        @NonNull
        private final String setupStatusMessage;

        @NonNull
        private final List<String> generalErrors;

        @NonNull
        private final List<SchoolSubjectStatus>
                subjectStatuses;

        private ValidationResult(
                @NonNull String schoolName,
                @NonNull String schoolCode,
                @NonNull String educationBoard,
                int classNumber,
                @NonNull String section,
                @NonNull String academicSession,
                @NonNull String studyMedium,
                @NonNull String schoolNameError,
                @NonNull String educationBoardError,
                @NonNull String classNumberError,
                @NonNull String academicSessionError,
                @NonNull String studyMediumError,
                boolean schoolDetailsValid,
                boolean subjectSelectionValid,
                boolean canSaveSchoolSetup,
                boolean readyForChildMode,
                int enabledSubjectCount,
                int confirmedBookCount,
                int pendingBookCount,
                int hiddenSubjectCount,
                int duplicateSubjectCount,
                @NonNull String setupStatusMessage,
                @NonNull List<String> generalErrors,
                @NonNull List<SchoolSubjectStatus>
                        subjectStatuses
        ) {
            this.schoolName =
                    schoolName;

            this.schoolCode =
                    schoolCode;

            this.educationBoard =
                    educationBoard;

            this.classNumber =
                    classNumber;

            this.section =
                    section;

            this.academicSession =
                    academicSession;

            this.studyMedium =
                    studyMedium;

            this.schoolNameError =
                    schoolNameError;

            this.educationBoardError =
                    educationBoardError;

            this.classNumberError =
                    classNumberError;

            this.academicSessionError =
                    academicSessionError;

            this.studyMediumError =
                    studyMediumError;

            this.schoolDetailsValid =
                    schoolDetailsValid;

            this.subjectSelectionValid =
                    subjectSelectionValid;

            this.canSaveSchoolSetup =
                    canSaveSchoolSetup;

            this.readyForChildMode =
                    readyForChildMode;

            this.enabledSubjectCount =
                    Math.max(
                            0,
                            enabledSubjectCount
                    );

            this.confirmedBookCount =
                    Math.max(
                            0,
                            confirmedBookCount
                    );

            this.pendingBookCount =
                    Math.max(
                            0,
                            pendingBookCount
                    );

            this.hiddenSubjectCount =
                    Math.max(
                            0,
                            hiddenSubjectCount
                    );

            this.duplicateSubjectCount =
                    Math.max(
                            0,
                            duplicateSubjectCount
                    );

            this.setupStatusMessage =
                    setupStatusMessage;

            this.generalErrors =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    generalErrors
                            )
                    );

            this.subjectStatuses =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    subjectStatuses
                            )
                    );
        }

        @NonNull
        public String getSchoolName() {
            return schoolName;
        }

        @NonNull
        public String getSchoolCode() {
            return schoolCode;
        }

        @NonNull
        public String getEducationBoard() {
            return educationBoard;
        }

        public int getClassNumber() {
            return classNumber;
        }

        @NonNull
        public String getSection() {
            return section;
        }

        @NonNull
        public String getAcademicSession() {
            return academicSession;
        }

        @NonNull
        public String getStudyMedium() {
            return studyMedium;
        }

        @NonNull
        public String getSchoolNameError() {
            return schoolNameError;
        }

        @NonNull
        public String getEducationBoardError() {
            return educationBoardError;
        }

        @NonNull
        public String getClassNumberError() {
            return classNumberError;
        }

        @NonNull
        public String getAcademicSessionError() {
            return academicSessionError;
        }

        @NonNull
        public String getStudyMediumError() {
            return studyMediumError;
        }

        public boolean isSchoolDetailsValid() {
            return schoolDetailsValid;
        }

        public boolean isSubjectSelectionValid() {
            return subjectSelectionValid;
        }

        public boolean canSaveSchoolSetup() {
            return canSaveSchoolSetup;
        }

        public boolean isReadyForChildMode() {
            return readyForChildMode;
        }

        public int getEnabledSubjectCount() {
            return enabledSubjectCount;
        }

        public int getConfirmedBookCount() {
            return confirmedBookCount;
        }

        public int getPendingBookCount() {
            return pendingBookCount;
        }

        public int getHiddenSubjectCount() {
            return hiddenSubjectCount;
        }

        public int getDuplicateSubjectCount() {
            return duplicateSubjectCount;
        }

        @NonNull
        public String getSetupStatusMessage() {
            return setupStatusMessage;
        }

        @NonNull
        public List<String> getGeneralErrors() {
            return generalErrors;
        }

        @NonNull
        public List<SchoolSubjectStatus>
        getSubjectStatuses() {
            return subjectStatuses;
        }

        public boolean hasGeneralErrors() {
            return !generalErrors.isEmpty();
        }

        public boolean hasPendingBooks() {
            return pendingBookCount > 0;
        }

        public boolean hasConfirmedBooks() {
            return confirmedBookCount > 0;
        }
    }

    public static final class SchoolSubjectStatus {

        private final long subjectRowId;

        @NonNull
        private final String subjectName;

        @NonNull
        private final String subjectCode;

        private final boolean enabled;

        private final boolean exactBookConfirmed;

        private final boolean duplicate;

        @NonNull
        private final String bookName;

        private SchoolSubjectStatus(
                long subjectRowId,
                @NonNull String subjectName,
                @NonNull String subjectCode,
                boolean enabled,
                boolean exactBookConfirmed,
                boolean duplicate,
                @NonNull String bookName
        ) {
            this.subjectRowId =
                    Math.max(
                            0L,
                            subjectRowId
                    );

            this.subjectName =
                    subjectName;

            this.subjectCode =
                    subjectCode;

            this.enabled =
                    enabled;

            this.exactBookConfirmed =
                    exactBookConfirmed;

            this.duplicate =
                    duplicate;

            this.bookName =
                    bookName;
        }

        public long getSubjectRowId() {
            return subjectRowId;
        }

        @NonNull
        public String getSubjectName() {
            return subjectName;
        }

        @NonNull
        public String getSubjectCode() {
            return subjectCode;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isExactBookConfirmed() {
            return exactBookConfirmed;
        }

        public boolean isDuplicate() {
            return duplicate;
        }

        @NonNull
        public String getBookName() {
            return bookName;
        }
    }
}