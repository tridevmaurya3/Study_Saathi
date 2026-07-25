package com.tridev.studysaathi.data.content.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StudentSchoolCurriculumProfile {

    private static final int MINIMUM_CLASS_NUMBER = 1;

    private static final int MAXIMUM_CLASS_NUMBER = 12;

    private final long studentProfileId;

    @NonNull
    private final String curriculumId;

    @NonNull
    private final String schoolName;

    @NonNull
    private final String schoolCode;

    @NonNull
    private final String educationBoard;

    @NonNull
    private final String schoolPattern;

    private final int classNumber;

    @NonNull
    private final String section;

    @NonNull
    private final String academicSession;

    @NonNull
    private final String studyMedium;

    @NonNull
    private final AiTutorPreferences aiTutorPreferences;

    @NonNull
    private final List<SchoolSubject> subjects;

    private final long createdAt;

    private final long updatedAt;

    public StudentSchoolCurriculumProfile(
            long studentProfileId,
            @NonNull String curriculumId,
            @NonNull String schoolName,
            @Nullable String schoolCode,
            @NonNull String educationBoard,
            @Nullable String schoolPattern,
            int classNumber,
            @Nullable String section,
            @NonNull String academicSession,
            @NonNull String studyMedium,
            @NonNull AiTutorPreferences aiTutorPreferences,
            @NonNull List<SchoolSubject> subjects,
            long createdAt,
            long updatedAt
    ) {
        if (studentProfileId < 1L) {
            throw new IllegalArgumentException(
                    "Student profile ID must be greater than zero."
            );
        }

        this.studentProfileId =
                studentProfileId;

        this.curriculumId =
                normalizeIdentifier(
                        curriculumId,
                        "Curriculum ID"
                );

        this.schoolName =
                requireText(
                        schoolName,
                        "School name"
                );

        this.schoolCode =
                safeOptionalText(
                        schoolCode
                );

        this.educationBoard =
                requireText(
                        educationBoard,
                        "Education board"
                );

        this.schoolPattern =
                safeOptionalText(
                        schoolPattern
                );

        if (classNumber < MINIMUM_CLASS_NUMBER
                || classNumber > MAXIMUM_CLASS_NUMBER) {

            throw new IllegalArgumentException(
                    "Class number must be between 1 and 12."
            );
        }

        this.classNumber =
                classNumber;

        this.section =
                safeOptionalText(
                        section
                );

        this.academicSession =
                requireText(
                        academicSession,
                        "Academic session"
                );

        this.studyMedium =
                requireText(
                        studyMedium,
                        "Study medium"
                );

        this.aiTutorPreferences =
                aiTutorPreferences;

        this.subjects =
                prepareSubjectList(
                        subjects
                );

        long safeCreatedAt =
                createdAt > 0L
                        ? createdAt
                        : System.currentTimeMillis();

        this.createdAt =
                safeCreatedAt;

        this.updatedAt =
                updatedAt >= safeCreatedAt
                        ? updatedAt
                        : safeCreatedAt;
    }

    public long getStudentProfileId() {
        return studentProfileId;
    }

    @NonNull
    public String getCurriculumId() {
        return curriculumId;
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

    @NonNull
    public String getSchoolPattern() {
        return schoolPattern;
    }

    public int getClassNumber() {
        return classNumber;
    }

    @NonNull
    public String getClassDisplayName() {
        return "Class " + classNumber;
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
    public AiTutorPreferences getAiTutorPreferences() {
        return aiTutorPreferences;
    }

    @NonNull
    public List<SchoolSubject> getSubjects() {
        return subjects;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public int getTotalSubjectCount() {
        return subjects.size();
    }

    public int getEnabledSubjectCount() {
        int enabledCount = 0;

        for (SchoolSubject subject : subjects) {
            if (subject.isEnabled()) {
                enabledCount++;
            }
        }

        return enabledCount;
    }

    public int getCoreSubjectCount() {
        int coreSubjectCount = 0;

        for (SchoolSubject subject : subjects) {
            if (subject.getSubjectCategory()
                    == SubjectCategory.CORE_ACADEMIC) {

                coreSubjectCount++;
            }
        }

        return coreSubjectCount;
    }

    public int getSchoolSpecificSubjectCount() {
        int schoolSpecificCount = 0;

        for (SchoolSubject subject : subjects) {
            if (subject.getSubjectCategory()
                    == SubjectCategory.SCHOOL_SPECIFIC) {

                schoolSpecificCount++;
            }
        }

        return schoolSpecificCount;
    }

    @NonNull
    public List<SchoolSubject> getEnabledSubjects() {
        List<SchoolSubject> enabledSubjects =
                new ArrayList<>();

        for (SchoolSubject subject : subjects) {
            if (subject.isEnabled()) {
                enabledSubjects.add(
                        subject
                );
            }
        }

        return Collections.unmodifiableList(
                enabledSubjects
        );
    }

    @Nullable
    public SchoolSubject findSubjectById(
            @NonNull String subjectId
    ) {
        String requiredSubjectId =
                normalizeIdentifier(
                        subjectId,
                        "Subject ID"
                );

        for (SchoolSubject subject : subjects) {
            if (subject.getSubjectId()
                    .equals(requiredSubjectId)) {

                return subject;
            }
        }

        return null;
    }

    public boolean containsSubject(
            @NonNull String subjectId
    ) {
        return findSubjectById(
                subjectId
        ) != null;
    }

    public boolean isAiTutorAvailableForSubject(
            @NonNull String subjectId
    ) {
        if (!aiTutorPreferences.isAiTutorEnabled()) {
            return false;
        }

        SchoolSubject subject =
                findSubjectById(
                        subjectId
                );

        return subject != null
                && subject.isEnabled()
                && subject.isAiTutorEnabled();
    }

    @NonNull
    private static List<SchoolSubject>
    prepareSubjectList(
            @NonNull List<SchoolSubject> sourceSubjects
    ) {
        List<SchoolSubject> preparedSubjects =
                new ArrayList<>();

        for (SchoolSubject subject : sourceSubjects) {
            if (subject == null) {
                throw new IllegalArgumentException(
                        "Subject list cannot contain null items."
                );
            }

            for (SchoolSubject existingSubject :
                    preparedSubjects) {

                if (existingSubject.getSubjectId()
                        .equals(subject.getSubjectId())) {

                    throw new IllegalArgumentException(
                            "Duplicate subject ID: "
                                    + subject.getSubjectId()
                    );
                }
            }

            preparedSubjects.add(
                    subject
            );
        }

        preparedSubjects.sort(
                Comparator.comparingInt(
                        SchoolSubject::getSortOrder
                )
        );

        return Collections.unmodifiableList(
                preparedSubjects
        );
    }

    public static final class SchoolSubject {

        @NonNull
        private final String subjectId;

        @NonNull
        private final String subjectNameEnglish;

        @NonNull
        private final String subjectNameHindi;

        @NonNull
        private final String subjectCode;

        @NonNull
        private final String bookName;

        @NonNull
        private final String publisherName;

        @NonNull
        private final SubjectCategory subjectCategory;

        @NonNull
        private final ContentSource contentSource;

        private final boolean enabled;

        private final boolean aiTutorEnabled;

        private final int sortOrder;

        public SchoolSubject(
                @NonNull String subjectId,
                @NonNull String subjectNameEnglish,
                @NonNull String subjectNameHindi,
                @Nullable String subjectCode,
                @Nullable String bookName,
                @Nullable String publisherName,
                @NonNull SubjectCategory subjectCategory,
                @NonNull ContentSource contentSource,
                boolean enabled,
                boolean aiTutorEnabled,
                int sortOrder
        ) {
            this.subjectId =
                    normalizeIdentifier(
                            subjectId,
                            "Subject ID"
                    );

            this.subjectNameEnglish =
                    requireText(
                            subjectNameEnglish,
                            "English subject name"
                    );

            this.subjectNameHindi =
                    requireText(
                            subjectNameHindi,
                            "Hindi subject name"
                    );

            this.subjectCode =
                    safeOptionalText(
                            subjectCode
                    );

            this.bookName =
                    safeOptionalText(
                            bookName
                    );

            this.publisherName =
                    safeOptionalText(
                            publisherName
                    );

            this.subjectCategory =
                    subjectCategory;

            this.contentSource =
                    contentSource;

            this.enabled =
                    enabled;

            this.aiTutorEnabled =
                    aiTutorEnabled;

            this.sortOrder =
                    Math.max(
                            0,
                            sortOrder
                    );
        }

        @NonNull
        public String getSubjectId() {
            return subjectId;
        }

        @NonNull
        public String getSubjectNameEnglish() {
            return subjectNameEnglish;
        }

        @NonNull
        public String getSubjectNameHindi() {
            return subjectNameHindi;
        }

        @NonNull
        public String getSubjectCode() {
            return subjectCode;
        }

        @NonNull
        public String getBookName() {
            return bookName;
        }

        @NonNull
        public String getPublisherName() {
            return publisherName;
        }

        @NonNull
        public SubjectCategory getSubjectCategory() {
            return subjectCategory;
        }

        @NonNull
        public ContentSource getContentSource() {
            return contentSource;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isAiTutorEnabled() {
            return aiTutorEnabled;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        @NonNull
        public String getDisplayName(
                @NonNull TutorLanguage tutorLanguage
        ) {
            switch (tutorLanguage) {
                case HINDI:
                    return subjectNameHindi;

                case ENGLISH:
                    return subjectNameEnglish;

                case BILINGUAL:
                default:
                    return subjectNameEnglish
                            + " / "
                            + subjectNameHindi;
            }
        }

        public boolean hasBookInformation() {
            return !bookName.isEmpty()
                    || !publisherName.isEmpty();
        }

        public boolean isSchoolSpecific() {
            return subjectCategory
                    == SubjectCategory.SCHOOL_SPECIFIC;
        }

        public boolean isOfficialCoreSubject() {
            return subjectCategory
                    == SubjectCategory.CORE_ACADEMIC;
        }
    }

    public static final class AiTutorPreferences {

        private final boolean aiTutorEnabled;

        @NonNull
        private final TutorLanguage defaultLanguage;

        @NonNull
        private final TutorAnswerMode defaultAnswerMode;

        private final boolean voiceQuestionEnabled;

        private final boolean imageQuestionEnabled;

        private final boolean readAnswerAloudEnabled;

        private final boolean childSafeAnswersEnabled;

        private final boolean saveDoubtHistoryEnabled;

        private final int preferredMaximumAnswerWords;

        public AiTutorPreferences(
                boolean aiTutorEnabled,
                @NonNull TutorLanguage defaultLanguage,
                @NonNull TutorAnswerMode defaultAnswerMode,
                boolean voiceQuestionEnabled,
                boolean imageQuestionEnabled,
                boolean readAnswerAloudEnabled,
                boolean childSafeAnswersEnabled,
                boolean saveDoubtHistoryEnabled,
                int preferredMaximumAnswerWords
        ) {
            this.aiTutorEnabled =
                    aiTutorEnabled;

            this.defaultLanguage =
                    defaultLanguage;

            this.defaultAnswerMode =
                    defaultAnswerMode;

            this.voiceQuestionEnabled =
                    voiceQuestionEnabled;

            this.imageQuestionEnabled =
                    imageQuestionEnabled;

            this.readAnswerAloudEnabled =
                    readAnswerAloudEnabled;

            this.childSafeAnswersEnabled =
                    childSafeAnswersEnabled;

            this.saveDoubtHistoryEnabled =
                    saveDoubtHistoryEnabled;

            this.preferredMaximumAnswerWords =
                    Math.max(
                            50,
                            Math.min(
                                    preferredMaximumAnswerWords,
                                    1000
                            )
                    );
        }

        @NonNull
        public static AiTutorPreferences
        createGradeSixDefaults() {

            return new AiTutorPreferences(
                    true,
                    TutorLanguage.BILINGUAL,
                    TutorAnswerMode.SIMPLE,
                    true,
                    true,
                    true,
                    true,
                    true,
                    300
            );
        }

        public boolean isAiTutorEnabled() {
            return aiTutorEnabled;
        }

        @NonNull
        public TutorLanguage getDefaultLanguage() {
            return defaultLanguage;
        }

        @NonNull
        public TutorAnswerMode getDefaultAnswerMode() {
            return defaultAnswerMode;
        }

        public boolean isVoiceQuestionEnabled() {
            return voiceQuestionEnabled;
        }

        public boolean isImageQuestionEnabled() {
            return imageQuestionEnabled;
        }

        public boolean isReadAnswerAloudEnabled() {
            return readAnswerAloudEnabled;
        }

        public boolean isChildSafeAnswersEnabled() {
            return childSafeAnswersEnabled;
        }

        public boolean isSaveDoubtHistoryEnabled() {
            return saveDoubtHistoryEnabled;
        }

        public int getPreferredMaximumAnswerWords() {
            return preferredMaximumAnswerWords;
        }
    }

    public enum SubjectCategory {

        CORE_ACADEMIC(
                "Core Academic",
                "मुख्य शैक्षणिक विषय"
        ),

        LANGUAGE(
                "Language",
                "भाषा विषय"
        ),

        SCHOOL_SPECIFIC(
                "School Specific",
                "स्कूल का अतिरिक्त विषय"
        ),

        SKILL_BASED(
                "Skill Based",
                "कौशल आधारित विषय"
        ),

        ACTIVITY_BASED(
                "Activity Based",
                "गतिविधि आधारित विषय"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        SubjectCategory(
                @NonNull String englishLabel,
                @NonNull String hindiLabel
        ) {
            this.englishLabel =
                    englishLabel;

            this.hindiLabel =
                    hindiLabel;
        }

        @NonNull
        public String getEnglishLabel() {
            return englishLabel;
        }

        @NonNull
        public String getHindiLabel() {
            return hindiLabel;
        }
    }

    public enum ContentSource {

        NCERT(
                "NCERT",
                "एनसीईआरटी"
        ),

        SCHOOL_BOOK(
                "School Book",
                "स्कूल की पुस्तक"
        ),

        PRIVATE_PUBLISHER(
                "Private Publisher",
                "निजी प्रकाशक"
        ),

        TEACHER_NOTES(
                "Teacher Notes",
                "शिक्षक के नोट्स"
        ),

        PARENT_CREATED(
                "Parent Created",
                "अभिभावक द्वारा बनाया गया"
        ),

        AI_ASSISTED(
                "AI Assisted",
                "एआई की सहायता से बनाया गया"
        ),

        CUSTOM(
                "Custom",
                "कस्टम"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        ContentSource(
                @NonNull String englishLabel,
                @NonNull String hindiLabel
        ) {
            this.englishLabel =
                    englishLabel;

            this.hindiLabel =
                    hindiLabel;
        }

        @NonNull
        public String getEnglishLabel() {
            return englishLabel;
        }

        @NonNull
        public String getHindiLabel() {
            return hindiLabel;
        }
    }

    public enum TutorLanguage {

        HINDI(
                "Hindi",
                "हिंदी"
        ),

        ENGLISH(
                "English",
                "अंग्रेजी"
        ),

        BILINGUAL(
                "Hindi + English",
                "हिंदी + अंग्रेजी"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        TutorLanguage(
                @NonNull String englishLabel,
                @NonNull String hindiLabel
        ) {
            this.englishLabel =
                    englishLabel;

            this.hindiLabel =
                    hindiLabel;
        }

        @NonNull
        public String getEnglishLabel() {
            return englishLabel;
        }

        @NonNull
        public String getHindiLabel() {
            return hindiLabel;
        }

        @NonNull
        public static TutorLanguage fromText(
                @Nullable String value
        ) {
            if (value == null
                    || value.trim().isEmpty()) {

                return BILINGUAL;
            }

            String normalizedValue =
                    value.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (normalizedValue.contains("hindi")
                    && !normalizedValue.contains("english")) {

                return HINDI;
            }

            if (normalizedValue.contains("english")
                    && !normalizedValue.contains("hindi")) {

                return ENGLISH;
            }

            return BILINGUAL;
        }
    }

    public enum TutorAnswerMode {

        SIMPLE(
                "Simple Explanation",
                "आसान व्याख्या"
        ),

        DETAILED(
                "Detailed Explanation",
                "विस्तृत व्याख्या"
        ),

        EXAM_ANSWER(
                "Exam Answer",
                "परीक्षा उत्तर"
        ),

        STEP_BY_STEP(
                "Step-by-Step",
                "स्टेप-बाय-स्टेप"
        ),

        EXAMPLE_BASED(
                "Example Based",
                "उदाहरण आधारित"
        ),

        PRACTICE_MODE(
                "Practice Mode",
                "अभ्यास मोड"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        TutorAnswerMode(
                @NonNull String englishLabel,
                @NonNull String hindiLabel
        ) {
            this.englishLabel =
                    englishLabel;

            this.hindiLabel =
                    hindiLabel;
        }

        @NonNull
        public String getEnglishLabel() {
            return englishLabel;
        }

        @NonNull
        public String getHindiLabel() {
            return hindiLabel;
        }
    }

    @NonNull
    private static String requireText(
            @Nullable String value,
            @NonNull String fieldName
    ) {
        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be empty."
            );
        }

        return value.trim();
    }

    @NonNull
    private static String safeOptionalText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    @NonNull
    private static String normalizeIdentifier(
            @Nullable String value,
            @NonNull String fieldName
    ) {
        String normalizedValue =
                requireText(
                        value,
                        fieldName
                )
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9_-]",
                                "_"
                        )
                        .replaceAll(
                                "_+",
                                "_"
                        );

        while (normalizedValue.startsWith("_")) {
            normalizedValue =
                    normalizedValue.substring(1);
        }

        while (normalizedValue.endsWith("_")) {
            normalizedValue =
                    normalizedValue.substring(
                            0,
                            normalizedValue.length() - 1
                    );
        }

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName
                            + " contains no supported characters."
            );
        }

        return normalizedValue;
    }
}