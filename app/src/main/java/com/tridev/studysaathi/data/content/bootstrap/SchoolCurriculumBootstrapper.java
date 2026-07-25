package com.tridev.studysaathi.data.content.bootstrap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.catalog.SubjectCatalog;
import com.tridev.studysaathi.data.local.dao.SchoolSubjectDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.SchoolCurriculumProfileRepository;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SchoolCurriculumBootstrapper
        implements AutoCloseable {

    @NonNull
    private final StudySaathiDatabase database;

    @NonNull
    private final SchoolSubjectDao schoolSubjectDao;

    @NonNull
    private final SchoolCurriculumProfileRepository
            curriculumProfileRepository;

    @NonNull
    private final Handler mainThreadHandler;

    @NonNull
    private final AtomicBoolean operationInProgress;

    @NonNull
    private final AtomicBoolean closed;

    public SchoolCurriculumBootstrapper(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        database =
                StudySaathiDatabase.getInstance(
                        applicationContext
                );

        schoolSubjectDao =
                database.schoolSubjectDao();

        curriculumProfileRepository =
                new SchoolCurriculumProfileRepository(
                        applicationContext
                );

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );

        operationInProgress =
                new AtomicBoolean(
                        false
                );

        closed =
                new AtomicBoolean(
                        false
                );
    }

    /**
     * Student के लिए curriculum profile और default
     * subject rows सुनिश्चित करता है।
     *
     * Existing database subjects कभी overwrite नहीं होंगे।
     */
    public void ensureCurriculumReady(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull BootstrapCallback callback
    ) {
        if (closed.get()) {
            callback.onError(
                    new BootstrapException(
                            FailureReason.BOOTSTRAPPER_CLOSED,
                            "Curriculum setup service is closed."
                    )
            );

            return;
        }

        if (!operationInProgress.compareAndSet(
                false,
                true
        )) {
            callback.onError(
                    new BootstrapException(
                            FailureReason.OPERATION_ALREADY_RUNNING,
                            "Curriculum setup is already running."
                    )
            );

            return;
        }

        if (studentProfile.getProfileId() <= 0L) {
            finishWithError(
                    callback,
                    new BootstrapException(
                            FailureReason.INVALID_STUDENT_PROFILE,
                            "A valid active student profile is required."
                    )
            );

            return;
        }

        curriculumProfileRepository
                .ensureBasicCurriculumProfile(
                        studentProfile,
                        new SchoolCurriculumProfileRepository
                                .EnsureProfileCallback() {

                            @Override
                            public void onReady(
                                    @NonNull SchoolCurriculumProfileEntity
                                            profile,
                                    boolean newlyCreated
                            ) {
                                seedMissingSubjects(
                                        studentProfile,
                                        profile,
                                        newlyCreated,
                                        callback
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                finishWithError(
                                        callback,
                                        new BootstrapException(
                                                FailureReason
                                                        .CURRICULUM_PROFILE_FAILED,
                                                safeErrorMessage(
                                                        exception,
                                                        "Curriculum profile could not be prepared."
                                                ),
                                                exception
                                        )
                                );
                            }
                        }
                );
    }

    private void seedMissingSubjects(
            @NonNull StudentProfileEntity studentProfile,
            @NonNull SchoolCurriculumProfileEntity
                    curriculumProfile,
            boolean curriculumProfileCreated,
            @NonNull BootstrapCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        long profileId =
                                curriculumProfile.getProfileId();

                        if (profileId <= 0L) {
                            throw new BootstrapException(
                                    FailureReason
                                            .INVALID_CURRICULUM_PROFILE,
                                    "A valid curriculum profile is required."
                            );
                        }

                        List<SubjectItem> catalogSubjects =
                                SubjectCatalog
                                        .getSubjectsForClass(
                                                studentProfile
                                                        .getStudentClass()
                                        );

                        if (catalogSubjects == null) {
                            catalogSubjects =
                                    Collections.emptyList();
                        }

                        List<SubjectItem> finalCatalogSubjects =
                                catalogSubjects;

                        int[] insertedCount =
                                new int[]{
                                        0
                                };

                        int[] existingCount =
                                new int[]{
                                        0
                                };

                        List<SchoolSubjectEntity>
                                resultingSubjects =
                                new ArrayList<>();

                        database.runInTransaction(() -> {
                            List<SchoolSubjectEntity>
                                    existingSubjects =
                                    schoolSubjectDao
                                            .getSubjectsForProfile(
                                                    profileId
                                            );

                            Set<String> existingSubjectIds =
                                    new HashSet<>();

                            if (existingSubjects != null) {
                                resultingSubjects.addAll(
                                        existingSubjects
                                );

                                for (SchoolSubjectEntity subject :
                                        existingSubjects) {

                                    existingSubjectIds.add(
                                            safeText(
                                                    subject.getSubjectId()
                                            )
                                                    .toLowerCase(
                                                            Locale.ROOT
                                                    )
                                    );
                                }
                            }

                            long currentTime =
                                    System.currentTimeMillis();

                            for (int index = 0;
                                 index < finalCatalogSubjects.size();
                                 index++) {

                                SubjectItem subjectItem =
                                        finalCatalogSubjects.get(
                                                index
                                        );

                                if (subjectItem == null) {
                                    continue;
                                }

                                String subjectName =
                                        safeText(
                                                subjectItem
                                                        .getSubjectName()
                                        );

                                if (subjectName.isEmpty()) {
                                    continue;
                                }

                                String subjectId =
                                        SchoolSubjectEntity
                                                .createSubjectId(
                                                        subjectName
                                                );

                                if (existingSubjectIds.contains(
                                        subjectId.toLowerCase(
                                                Locale.ROOT
                                        )
                                )) {
                                    existingCount[0]++;

                                    continue;
                                }

                                SchoolSubjectEntity
                                        schoolSubject =
                                        createSchoolSubject(
                                                profileId,
                                                subjectName,
                                                index + 1,
                                                currentTime
                                        );

                                long insertedRowId =
                                        schoolSubjectDao
                                                .insertSubject(
                                                        schoolSubject
                                                );

                                schoolSubject.setSubjectRowId(
                                        insertedRowId
                                );

                                resultingSubjects.add(
                                        schoolSubject
                                );

                                existingSubjectIds.add(
                                        subjectId.toLowerCase(
                                                Locale.ROOT
                                        )
                                );

                                insertedCount[0]++;
                            }
                        });

                        BootstrapResult result =
                                new BootstrapResult(
                                        curriculumProfile,
                                        curriculumProfileCreated,
                                        insertedCount[0],
                                        existingCount[0],
                                        resultingSubjects
                                );

                        operationInProgress.set(
                                false
                        );

                        postToMainThread(() ->
                                callback.onReady(
                                        result
                                )
                        );

                    } catch (Exception exception) {
                        finishWithError(
                                callback,
                                exception
                                        instanceof BootstrapException
                                        ? (BootstrapException) exception
                                        : new BootstrapException(
                                        FailureReason
                                        .SUBJECT_SEEDING_FAILED,
                                        safeErrorMessage(
                                                exception,
                                                "School subjects could not be prepared."
                                        ),
                                        exception
                                )
                        );
                    }
                });
    }

    @NonNull
    private SchoolSubjectEntity createSchoolSubject(
            long profileId,
            @NonNull String subjectName,
            int sortOrder,
            long currentTime
    ) {
        SchoolSubjectEntity schoolSubject =
                new SchoolSubjectEntity();

        String category =
                determineSubjectCategory(
                        subjectName
                );

        boolean officialCoreSubject =
                isOfficialCoreSubject(
                        subjectName
                );

        schoolSubject.setProfileId(
                profileId
        );

        schoolSubject.setSubjectId(
                SchoolSubjectEntity.createSubjectId(
                        subjectName
                )
        );

        schoolSubject.setSubjectNameEnglish(
                subjectName
        );

        schoolSubject.setSubjectNameHindi(
                getHindiSubjectName(
                        subjectName
                )
        );

        schoolSubject.setSubjectCode(
                getSubjectCode(
                        subjectName
                )
        );

        schoolSubject.setBookName(
                ""
        );

        schoolSubject.setBookCode(
                ""
        );

        schoolSubject.setPublisherName(
                ""
        );

        schoolSubject.setSubjectCategory(
                category
        );

        /*
         * शुरुआत में किसी specific NCERT/private book की
         * पुष्टि नहीं हुई है, इसलिए SCHOOL_BOOK रखा गया है।
         */
        schoolSubject.setContentSource(
                "SCHOOL_BOOK"
        );

        schoolSubject.setContentPackId(
                ""
        );

        schoolSubject.setEnabled(
                true
        );

        schoolSubject.setAiTutorEnabled(
                true
        );

        schoolSubject.setOfficialCoreSubject(
                officialCoreSubject
        );

        /*
         * Parent बाद में actual school book, publisher
         * और subject information बदल सकेगा।
         */
        schoolSubject.setAllowParentContentEditing(
                true
        );

        schoolSubject.setSortOrder(
                Math.max(
                        1,
                        sortOrder
                )
        );

        schoolSubject.setChapterCount(
                0
        );

        schoolSubject.setLessonCount(
                0
        );

        schoolSubject.setQuizQuestionCount(
                0
        );

        schoolSubject.setCreatedAt(
                currentTime
        );

        schoolSubject.setUpdatedAt(
                currentTime
        );

        return schoolSubject;
    }

    @NonNull
    private String determineSubjectCategory(
            @NonNull String subjectName
    ) {
        String normalizedName =
                normalizeSubjectName(
                        subjectName
                );

        switch (normalizedName) {
            case "english":
            case "hindi":
            case "sanskrit":
                return "LANGUAGE";

            case "computer":
            case "computer science":
            case "information technology":
                return "SKILL_BASED";

            case "art and activity":
            case "art activity":
                return "ACTIVITY_BASED";

            case "general knowledge":
                return "SCHOOL_SPECIFIC";

            default:
                return "CORE_ACADEMIC";
        }
    }

    private boolean isOfficialCoreSubject(
            @NonNull String subjectName
    ) {
        String normalizedName =
                normalizeSubjectName(
                        subjectName
                );

        switch (normalizedName) {
            case "general knowledge":
            case "art and activity":
            case "art activity":
            case "computer":
                return false;

            default:
                return true;
        }
    }

    @NonNull
    private String getHindiSubjectName(
            @NonNull String subjectName
    ) {
        String normalizedName =
                normalizeSubjectName(
                        subjectName
                );

        switch (normalizedName) {
            case "english":
                return "अंग्रेज़ी";

            case "hindi":
                return "हिंदी";

            case "mathematics":
                return "गणित";

            case "environmental studies":
                return "पर्यावरण अध्ययन";

            case "science":
                return "विज्ञान";

            case "social science":
                return "सामाजिक विज्ञान";

            case "sanskrit":
                return "संस्कृत";

            case "computer":
                return "कंप्यूटर";

            case "general knowledge":
                return "सामान्य ज्ञान";

            case "art and activity":
            case "art activity":
                return "कला एवं गतिविधि";

            case "information technology":
                return "सूचना प्रौद्योगिकी";

            case "physics":
                return "भौतिक विज्ञान";

            case "chemistry":
                return "रसायन विज्ञान";

            case "biology":
                return "जीव विज्ञान";

            case "accountancy":
                return "लेखाशास्त्र";

            case "business studies":
                return "व्यवसाय अध्ययन";

            case "economics":
                return "अर्थशास्त्र";

            case "computer science":
                return "कंप्यूटर विज्ञान";

            case "history":
                return "इतिहास";

            case "geography":
                return "भूगोल";

            case "political science":
                return "राजनीति विज्ञान";

            default:
                return subjectName;
        }
    }

    @NonNull
    private String getSubjectCode(
            @NonNull String subjectName
    ) {
        String normalizedName =
                normalizeSubjectName(
                        subjectName
                );

        switch (normalizedName) {
            case "english":
                return "ENG";

            case "hindi":
                return "HIN";

            case "mathematics":
                return "MATH";

            case "environmental studies":
                return "EVS";

            case "science":
                return "SCI";

            case "social science":
                return "SST";

            case "sanskrit":
                return "SAN";

            case "computer":
                return "COMP";

            case "general knowledge":
                return "GK";

            case "art and activity":
            case "art activity":
                return "ART";

            case "information technology":
                return "IT";

            case "physics":
                return "PHY";

            case "chemistry":
                return "CHEM";

            case "biology":
                return "BIO";

            case "accountancy":
                return "ACC";

            case "business studies":
                return "BST";

            case "economics":
                return "ECO";

            case "computer science":
                return "CS";

            case "history":
                return "HIS";

            case "geography":
                return "GEO";

            case "political science":
                return "POL";

            default:
                return createFallbackSubjectCode(
                        subjectName
                );
        }
    }

    @NonNull
    private String createFallbackSubjectCode(
            @NonNull String subjectName
    ) {
        String normalizedName =
                subjectName
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^A-Z0-9]",
                                ""
                        );

        if (normalizedName.isEmpty()) {
            return "SUB";
        }

        return normalizedName.substring(
                0,
                Math.min(
                        6,
                        normalizedName.length()
                )
        );
    }

    @NonNull
    private String normalizeSubjectName(
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

    private void finishWithError(
            @NonNull BootstrapCallback callback,
            @NonNull BootstrapException exception
    ) {
        operationInProgress.set(
                false
        );

        postToMainThread(() ->
                callback.onError(
                        exception
                )
        );
    }

    @NonNull
    private String safeErrorMessage(
            @NonNull Exception exception,
            @NonNull String fallback
    ) {
        String message =
                safeText(
                        exception.getMessage()
                );

        return message.isEmpty()
                ? fallback
                : message;
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private void postToMainThread(
            @NonNull Runnable runnable
    ) {
        mainThreadHandler.post(
                runnable
        );
    }

    public boolean isOperationInProgress() {
        return operationInProgress.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        closed.set(
                true
        );

        operationInProgress.set(
                false
        );
    }

    public interface BootstrapCallback {

        void onReady(
                @NonNull BootstrapResult result
        );

        void onError(
                @NonNull BootstrapException exception
        );
    }

    public enum FailureReason {

        BOOTSTRAPPER_CLOSED,

        OPERATION_ALREADY_RUNNING,

        INVALID_STUDENT_PROFILE,

        INVALID_CURRICULUM_PROFILE,

        CURRICULUM_PROFILE_FAILED,

        SUBJECT_SEEDING_FAILED
    }

    public static final class BootstrapResult {

        @NonNull
        private final SchoolCurriculumProfileEntity
                curriculumProfile;

        private final boolean curriculumProfileCreated;

        private final int insertedSubjectCount;

        private final int existingSubjectCount;

        @NonNull
        private final List<SchoolSubjectEntity>
                availableSubjects;

        private BootstrapResult(
                @NonNull SchoolCurriculumProfileEntity
                        curriculumProfile,
                boolean curriculumProfileCreated,
                int insertedSubjectCount,
                int existingSubjectCount,
                @NonNull List<SchoolSubjectEntity>
                        availableSubjects
        ) {
            this.curriculumProfile =
                    curriculumProfile;

            this.curriculumProfileCreated =
                    curriculumProfileCreated;

            this.insertedSubjectCount =
                    Math.max(
                            0,
                            insertedSubjectCount
                    );

            this.existingSubjectCount =
                    Math.max(
                            0,
                            existingSubjectCount
                    );

            this.availableSubjects =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    availableSubjects
                            )
                    );
        }

        @NonNull
        public SchoolCurriculumProfileEntity
        getCurriculumProfile() {
            return curriculumProfile;
        }

        public boolean isCurriculumProfileCreated() {
            return curriculumProfileCreated;
        }

        public int getInsertedSubjectCount() {
            return insertedSubjectCount;
        }

        public int getExistingSubjectCount() {
            return existingSubjectCount;
        }

        public int getTotalSubjectCount() {
            return availableSubjects.size();
        }

        @NonNull
        public List<SchoolSubjectEntity>
        getAvailableSubjects() {
            return availableSubjects;
        }

        public boolean hasSubjects() {
            return !availableSubjects.isEmpty();
        }

        public boolean wereNewSubjectsInserted() {
            return insertedSubjectCount > 0;
        }
    }

    public static final class BootstrapException
            extends Exception {

        @NonNull
        private final FailureReason failureReason;

        private BootstrapException(
                @NonNull FailureReason failureReason,
                @NonNull String message
        ) {
            super(
                    message
            );

            this.failureReason =
                    failureReason;
        }

        private BootstrapException(
                @NonNull FailureReason failureReason,
                @NonNull String message,
                @NonNull Throwable cause
        ) {
            super(
                    message,
                    cause
            );

            this.failureReason =
                    failureReason;
        }

        @NonNull
        public FailureReason getFailureReason() {
            return failureReason;
        }

        public boolean canRetry() {
            return failureReason
                    == FailureReason.CURRICULUM_PROFILE_FAILED
                    || failureReason
                    == FailureReason.SUBJECT_SEEDING_FAILED;
        }
    }
}