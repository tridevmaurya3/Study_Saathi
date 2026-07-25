package com.tridev.studysaathi.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.dao.SchoolSubjectDao;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SchoolSubjectRepository {

    @NonNull
    private final SchoolSubjectDao schoolSubjectDao;

    @NonNull
    private final Handler mainThreadHandler;

    public SchoolSubjectRepository(
            @NonNull Context context
    ) {
        StudySaathiDatabase database =
                StudySaathiDatabase.getInstance(
                        context.getApplicationContext()
                );

        schoolSubjectDao =
                database.schoolSubjectDao();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * नई school subject database में insert करता है।
     */
    public void insertSubject(
            @NonNull SchoolSubjectEntity schoolSubject,
            @NonNull InsertSubjectCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectForInsert(
                                schoolSubject
                        );

                        long insertedSubjectRowId =
                                schoolSubjectDao.insertSubject(
                                        schoolSubject
                                );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        insertedSubjectRowId
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Existing subject की पूरी information update करता है।
     */
    public void updateSubject(
            @NonNull SchoolSubjectEntity schoolSubject,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        int updatedRows =
                                schoolSubjectDao.updateSubject(
                                        schoolSubject
                                );

                        requireUpdatedRow(
                                updatedRows
                        );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Subject row ID से subject प्राप्त करता है।
     */
    public void getSubjectByRowId(
            long subjectRowId,
            @NonNull SingleSubjectCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        SchoolSubjectEntity schoolSubject =
                                schoolSubjectDao
                                        .getSubjectByRowId(
                                                subjectRowId
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        schoolSubject
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Subject ID और profile ID से subject प्राप्त करता है।
     */
    public void getSubjectBySubjectId(
            long profileId,
            @NonNull String subjectId,
            @NonNull SingleSubjectCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        String safeSubjectId =
                                safeText(
                                        subjectId
                                );

                        if (safeSubjectId.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Subject ID is required."
                            );
                        }

                        SchoolSubjectEntity schoolSubject =
                                schoolSubjectDao
                                        .getSubjectBySubjectId(
                                                profileId,
                                                safeSubjectId
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        schoolSubject
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Curriculum profile के subjects प्राप्त करता है।
     */
    public void getSubjectsForProfile(
            long profileId,
            boolean enabledSubjectsOnly,
            @NonNull SubjectsCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        List<SchoolSubjectEntity> subjects;

                        if (enabledSubjectsOnly) {
                            subjects =
                                    schoolSubjectDao
                                            .getEnabledSubjectsForProfile(
                                                    profileId
                                            );

                        } else {
                            subjects =
                                    schoolSubjectDao
                                            .getSubjectsForProfile(
                                                    profileId
                                            );
                        }

                        List<SchoolSubjectEntity> safeSubjects =
                                subjects == null
                                        ? Collections.emptyList()
                                        : subjects;

                        postToMainThread(() ->
                                callback.onSuccess(
                                        safeSubjects
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Exact English या Hindi subject name से subject खोजता है।
     */
    public void findSubjectByName(
            long profileId,
            @NonNull String subjectName,
            boolean enabledSubjectOnly,
            @NonNull SingleSubjectCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        String safeSubjectName =
                                safeText(
                                        subjectName
                                );

                        if (safeSubjectName.isEmpty()) {
                            postToMainThread(() ->
                                    callback.onSuccess(
                                            null
                                    )
                            );

                            return;
                        }

                        SchoolSubjectEntity schoolSubject;

                        if (enabledSubjectOnly) {
                            schoolSubject =
                                    schoolSubjectDao
                                            .findEnabledSubjectByName(
                                                    profileId,
                                                    safeSubjectName
                                            );

                        } else {
                            schoolSubject =
                                    schoolSubjectDao
                                            .findSubjectByName(
                                                    profileId,
                                                    safeSubjectName
                                            );
                        }

                        postToMainThread(() ->
                                callback.onSuccess(
                                        schoolSubject
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Scanned book के subject name को curriculum subject से match करता है।
     *
     * उदाहरण:
     *
     * Mathematics → Maths → गणित
     * Science → General Science → विज्ञान
     * Social Science → Social Studies → SST
     * Computer → Computer Science → ICT
     */
    public void resolveSubjectForBook(
            long profileId,
            @Nullable String detectedSubjectName,
            @NonNull SubjectResolutionCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        List<String> subjectCandidates =
                                buildSubjectCandidates(
                                        detectedSubjectName
                                );

                        if (subjectCandidates.isEmpty()) {
                            postToMainThread(() ->
                                    callback.onResolved(
                                            null,
                                            ""
                                    )
                            );

                            return;
                        }

                        SchoolSubjectEntity resolvedSubject =
                                null;

                        String matchedCandidate =
                                "";

                        for (String candidate :
                                subjectCandidates) {

                            resolvedSubject =
                                    schoolSubjectDao
                                            .findEnabledSubjectByName(
                                                    profileId,
                                                    candidate
                                            );

                            if (resolvedSubject != null) {
                                matchedCandidate =
                                        candidate;

                                break;
                            }
                        }

                        SchoolSubjectEntity finalResolvedSubject =
                                resolvedSubject;

                        String finalMatchedCandidate =
                                matchedCandidate;

                        postToMainThread(() ->
                                callback.onResolved(
                                        finalResolvedSubject,
                                        finalMatchedCandidate
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Subject code से subject खोजता है।
     */
    public void findSubjectByCode(
            long profileId,
            @Nullable String subjectCode,
            @NonNull SingleSubjectCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        String safeSubjectCode =
                                safeText(
                                        subjectCode
                                );

                        if (safeSubjectCode.isEmpty()) {
                            postToMainThread(() ->
                                    callback.onSuccess(
                                            null
                                    )
                            );

                            return;
                        }

                        SchoolSubjectEntity schoolSubject =
                                schoolSubjectDao
                                        .findSubjectByCode(
                                                profileId,
                                                safeSubjectCode
                                        );

                        postToMainThread(() ->
                                callback.onSuccess(
                                        schoolSubject
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Subject की next available sort order देता है।
     */
    public void getNextSortOrder(
            long profileId,
            @NonNull SortOrderCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateProfileId(
                                profileId
                        );

                        int maximumSortOrder =
                                schoolSubjectDao
                                        .getMaximumSortOrder(
                                                profileId
                                        );

                        int nextSortOrder =
                                Math.max(
                                        0,
                                        maximumSortOrder
                                ) + 1;

                        postToMainThread(() ->
                                callback.onSuccess(
                                        nextSortOrder
                                )
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Subject को enable या disable करता है।
     */
    public void setSubjectEnabled(
            long subjectRowId,
            boolean enabled,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        int updatedRows =
                                schoolSubjectDao.setSubjectEnabled(
                                        subjectRowId,
                                        enabled,
                                        System.currentTimeMillis()
                                );

                        requireUpdatedRow(
                                updatedRows
                        );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Subject-level AI Tutor enable या disable करता है।
     */
    public void setAiTutorEnabled(
            long subjectRowId,
            boolean enabled,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        int updatedRows =
                                schoolSubjectDao.setAiTutorEnabled(
                                        subjectRowId,
                                        enabled,
                                        System.currentTimeMillis()
                                );

                        requireUpdatedRow(
                                updatedRows
                        );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Scan से मिली मुख्य book information subject row में update करता है।
     */
    public void updateSubjectBookInformation(
            long subjectRowId,
            @Nullable String bookName,
            @Nullable String bookCode,
            @Nullable String publisherName,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        String safeBookName =
                                safeText(
                                        bookName
                                );

                        if (safeBookName.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Book name is required."
                            );
                        }

                        int updatedRows =
                                schoolSubjectDao
                                        .updateSubjectBookInformation(
                                                subjectRowId,
                                                safeBookName,
                                                safeText(
                                                        bookCode
                                                ),
                                                safeText(
                                                        publisherName
                                                ),
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
                        );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    /**
     * Subject content counts update करता है।
     */
    public void updateContentCounts(
            long subjectRowId,
            int chapterCount,
            int lessonCount,
            int quizQuestionCount,
            @NonNull OperationCallback callback
    ) {
        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        validateSubjectRowId(
                                subjectRowId
                        );

                        int updatedRows =
                                schoolSubjectDao
                                        .updateContentCounts(
                                                subjectRowId,
                                                Math.max(
                                                        0,
                                                        chapterCount
                                                ),
                                                Math.max(
                                                        0,
                                                        lessonCount
                                                ),
                                                Math.max(
                                                        0,
                                                        quizQuestionCount
                                                ),
                                                System.currentTimeMillis()
                                        );

                        requireUpdatedRow(
                                updatedRows
                        );

                        postToMainThread(
                                callback::onSuccess
                        );

                    } catch (Exception exception) {
                        postError(
                                callback,
                                exception
                        );
                    }
                });
    }

    @NonNull
    private List<String> buildSubjectCandidates(
            @Nullable String detectedSubjectName
    ) {
        String safeDetectedSubjectName =
                safeText(
                        detectedSubjectName
                );

        if (safeDetectedSubjectName.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> candidates =
                new LinkedHashSet<>();

        addCandidate(
                candidates,
                safeDetectedSubjectName
        );

        String normalizedSubject =
                normalizeSubjectName(
                        safeDetectedSubjectName
                );

        switch (normalizedSubject) {
            case "math":
            case "maths":
            case "mathematics":
            case "गणित":
                addCandidate(
                        candidates,
                        "Mathematics"
                );

                addCandidate(
                        candidates,
                        "Maths"
                );

                addCandidate(
                        candidates,
                        "Math"
                );

                addCandidate(
                        candidates,
                        "गणित"
                );

                break;

            case "science":
            case "general science":
            case "विज्ञान":
                addCandidate(
                        candidates,
                        "Science"
                );

                addCandidate(
                        candidates,
                        "General Science"
                );

                addCandidate(
                        candidates,
                        "विज्ञान"
                );

                break;

            case "social science":
            case "social studies":
            case "sst":
            case "सामाजिक विज्ञान":
            case "सामाजिक अध्ययन":
                addCandidate(
                        candidates,
                        "Social Science"
                );

                addCandidate(
                        candidates,
                        "Social Studies"
                );

                addCandidate(
                        candidates,
                        "SST"
                );

                addCandidate(
                        candidates,
                        "सामाजिक विज्ञान"
                );

                addCandidate(
                        candidates,
                        "सामाजिक अध्ययन"
                );

                break;

            case "english":
            case "english language":
            case "अंग्रेजी":
                addCandidate(
                        candidates,
                        "English"
                );

                addCandidate(
                        candidates,
                        "English Language"
                );

                addCandidate(
                        candidates,
                        "अंग्रेजी"
                );

                break;

            case "hindi":
            case "हिंदी":
            case "हिन्दी":
                addCandidate(
                        candidates,
                        "Hindi"
                );

                addCandidate(
                        candidates,
                        "हिंदी"
                );

                addCandidate(
                        candidates,
                        "हिन्दी"
                );

                break;

            case "sanskrit":
            case "संस्कृत":
                addCandidate(
                        candidates,
                        "Sanskrit"
                );

                addCandidate(
                        candidates,
                        "संस्कृत"
                );

                break;

            case "computer":
            case "computer science":
            case "computer studies":
            case "ict":
            case "information technology":
            case "कंप्यूटर":
            case "कम्प्यूटर":
                addCandidate(
                        candidates,
                        "Computer"
                );

                addCandidate(
                        candidates,
                        "Computer Science"
                );

                addCandidate(
                        candidates,
                        "Computer Studies"
                );

                addCandidate(
                        candidates,
                        "ICT"
                );

                addCandidate(
                        candidates,
                        "Information Technology"
                );

                addCandidate(
                        candidates,
                        "कंप्यूटर"
                );

                break;

            case "general knowledge":
            case "gk":
            case "सामान्य ज्ञान":
                addCandidate(
                        candidates,
                        "General Knowledge"
                );

                addCandidate(
                        candidates,
                        "GK"
                );

                addCandidate(
                        candidates,
                        "सामान्य ज्ञान"
                );

                break;

            case "environmental studies":
            case "environmental science":
            case "evs":
            case "पर्यावरण अध्ययन":
                addCandidate(
                        candidates,
                        "Environmental Studies"
                );

                addCandidate(
                        candidates,
                        "Environmental Science"
                );

                addCandidate(
                        candidates,
                        "EVS"
                );

                addCandidate(
                        candidates,
                        "पर्यावरण अध्ययन"
                );

                break;

            case "moral science":
            case "value education":
            case "moral education":
            case "नैतिक शिक्षा":
                addCandidate(
                        candidates,
                        "Moral Science"
                );

                addCandidate(
                        candidates,
                        "Value Education"
                );

                addCandidate(
                        candidates,
                        "Moral Education"
                );

                addCandidate(
                        candidates,
                        "नैतिक शिक्षा"
                );

                break;

            case "art":
            case "art education":
            case "drawing":
            case "चित्रकला":
                addCandidate(
                        candidates,
                        "Art"
                );

                addCandidate(
                        candidates,
                        "Art Education"
                );

                addCandidate(
                        candidates,
                        "Drawing"
                );

                addCandidate(
                        candidates,
                        "चित्रकला"
                );

                break;

            case "physical education":
            case "physical training":
            case "pe":
            case "pt":
            case "शारीरिक शिक्षा":
                addCandidate(
                        candidates,
                        "Physical Education"
                );

                addCandidate(
                        candidates,
                        "Physical Training"
                );

                addCandidate(
                        candidates,
                        "PE"
                );

                addCandidate(
                        candidates,
                        "PT"
                );

                addCandidate(
                        candidates,
                        "शारीरिक शिक्षा"
                );

                break;

            default:
                /*
                 * Exact detected subject name पहले से
                 * candidate list में जोड़ा जा चुका है।
                 */
                break;
        }

        return new ArrayList<>(
                candidates
        );
    }

    private void addCandidate(
            @NonNull Set<String> candidates,
            @Nullable String candidate
    ) {
        String safeCandidate =
                safeText(
                        candidate
                );

        if (safeCandidate.isEmpty()) {
            return;
        }

        for (String existingCandidate :
                candidates) {

            if (existingCandidate.equalsIgnoreCase(
                    safeCandidate
            )) {
                return;
            }
        }

        candidates.add(
                safeCandidate
        );
    }

    @NonNull
    private String normalizeSubjectName(
            @Nullable String subjectName
    ) {
        return safeText(
                subjectName
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

    private void validateSubjectForInsert(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        validateProfileId(
                schoolSubject.getProfileId()
        );

        if (safeText(
                schoolSubject.getSubjectId()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Subject ID is required."
            );
        }

        if (safeText(
                schoolSubject.getSubjectNameEnglish()
        ).isEmpty()
                && safeText(
                schoolSubject.getSubjectNameHindi()
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Subject name is required."
            );
        }

        long currentTime =
                System.currentTimeMillis();

        if (schoolSubject.getCreatedAt()
                <= 0L) {

            schoolSubject.setCreatedAt(
                    currentTime
            );
        }

        schoolSubject.setUpdatedAt(
                currentTime
        );
    }

    private void validateProfileId(
            long profileId
    ) {
        if (profileId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid curriculum profile ID is required."
            );
        }
    }

    private void validateSubjectRowId(
            long subjectRowId
    ) {
        if (subjectRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school subject row ID is required."
            );
        }
    }

    private void requireUpdatedRow(
            int updatedRows
    ) {
        if (updatedRows <= 0) {
            throw new IllegalStateException(
                    "The selected school subject was not found."
            );
        }
    }

    @NonNull
    private String safeText(
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

    private void postError(
            @NonNull ErrorCallback callback,
            @NonNull Exception exception
    ) {
        postToMainThread(() ->
                callback.onError(
                        exception
                )
        );
    }

    public interface ErrorCallback {

        void onError(
                @NonNull Exception exception
        );
    }

    public interface InsertSubjectCallback
            extends ErrorCallback {

        void onSuccess(
                long insertedSubjectRowId
        );
    }

    public interface SingleSubjectCallback
            extends ErrorCallback {

        void onSuccess(
                @Nullable SchoolSubjectEntity schoolSubject
        );
    }

    public interface SubjectsCallback
            extends ErrorCallback {

        void onSuccess(
                @NonNull List<SchoolSubjectEntity> schoolSubjects
        );
    }

    public interface SubjectResolutionCallback
            extends ErrorCallback {

        void onResolved(
                @Nullable SchoolSubjectEntity schoolSubject,
                @NonNull String matchedSubjectName
        );
    }

    public interface SortOrderCallback
            extends ErrorCallback {

        void onSuccess(
                int nextSortOrder
        );
    }

    public interface OperationCallback
            extends ErrorCallback {

        void onSuccess();
    }
}