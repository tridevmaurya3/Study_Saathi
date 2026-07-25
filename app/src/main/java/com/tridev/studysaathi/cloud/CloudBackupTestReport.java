package com.tridev.studysaathi.cloud;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CloudBackupTestReport {

    public static final int REPORT_FORMAT_VERSION = 1;

    private static final String REPORT_FORMAT =
            "study_saathi_cloud_backup_test_report";

    private final String reportId;

    private final long startedAt;

    private long completedAt;

    private final List<TestResult> testResults =
            new ArrayList<>();

    private boolean completed;

    public CloudBackupTestReport() {
        reportId =
                "cloud_test_"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        startedAt =
                System.currentTimeMillis();
    }

    /**
     * Adds a completed test result.
     *
     * Duplicate test IDs are rejected to prevent an
     * unreliable or ambiguous report.
     */
    public synchronized void addResult(
            @NonNull TestResult testResult
    ) {
        ensureReportIsOpen();

        for (TestResult existingResult
                : testResults) {

            if (existingResult.getTestId()
                    .equals(
                            testResult.getTestId()
                    )) {

                throw new IllegalArgumentException(
                        "Duplicate cloud test ID: "
                                + testResult.getTestId()
                );
            }
        }

        testResults.add(
                testResult
        );
    }

    /**
     * Marks the report as complete.
     *
     * No additional test results can be added after
     * completion.
     */
    public synchronized void complete() {
        if (completed) {
            return;
        }

        completedAt =
                System.currentTimeMillis();

        if (completedAt < startedAt) {
            completedAt = startedAt;
        }

        completed = true;
    }

    @NonNull
    public String getReportId() {
        return reportId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public long getDurationMillis() {
        long effectiveEndTime =
                completed
                        ? completedAt
                        : System.currentTimeMillis();

        return Math.max(
                effectiveEndTime - startedAt,
                0L
        );
    }

    @NonNull
    public synchronized List<TestResult>
    getTestResults() {

        return Collections.unmodifiableList(
                new ArrayList<>(
                        testResults
                )
        );
    }

    public synchronized int getTotalTestCount() {
        return testResults.size();
    }

    public synchronized int getPassedTestCount() {
        return countStatus(
                TestStatus.PASSED
        );
    }

    public synchronized int getFailedTestCount() {
        return countStatus(
                TestStatus.FAILED
        );
    }

    public synchronized int getWarningTestCount() {
        return countStatus(
                TestStatus.WARNING
        );
    }

    public synchronized int getNotRunTestCount() {
        return countStatus(
                TestStatus.NOT_RUN
        );
    }

    /**
     * Overall report passes only when:
     *
     * 1. Report has been completed.
     * 2. At least one test exists.
     * 3. No FAILED result exists.
     * 4. No NOT_RUN result exists.
     *
     * WARNING results do not fail the complete report.
     */
    public synchronized boolean isOverallSuccessful() {
        return completed
                && !testResults.isEmpty()
                && getFailedTestCount() == 0
                && getNotRunTestCount() == 0;
    }

    @NonNull
    public synchronized ReportStatus
    getOverallStatus() {

        if (!completed) {
            return ReportStatus.IN_PROGRESS;
        }

        if (testResults.isEmpty()
                || getNotRunTestCount() > 0) {

            return ReportStatus.INCOMPLETE;
        }

        if (getFailedTestCount() > 0) {
            return ReportStatus.FAILED;
        }

        if (getWarningTestCount() > 0) {
            return ReportStatus.COMPLETED_WITH_WARNINGS;
        }

        return ReportStatus.PASSED;
    }

    /**
     * Converts the report to JSON without including
     * passphrases, tokens, decrypted backup contents or
     * unmasked Firebase identifiers.
     */
    @NonNull
    public synchronized JSONObject toJson()
            throws JSONException {

        JSONObject rootObject =
                new JSONObject();

        rootObject.put(
                "report_format",
                REPORT_FORMAT
        );

        rootObject.put(
                "report_format_version",
                REPORT_FORMAT_VERSION
        );

        rootObject.put(
                "report_id",
                reportId
        );

        rootObject.put(
                "started_at",
                startedAt
        );

        rootObject.put(
                "started_at_iso",
                Instant.ofEpochMilli(
                        startedAt
                ).toString()
        );

        rootObject.put(
                "completed",
                completed
        );

        rootObject.put(
                "completed_at",
                completedAt
        );

        if (completedAt > 0L) {
            rootObject.put(
                    "completed_at_iso",
                    Instant.ofEpochMilli(
                            completedAt
                    ).toString()
            );
        } else {
            rootObject.put(
                    "completed_at_iso",
                    JSONObject.NULL
            );
        }

        rootObject.put(
                "duration_millis",
                getDurationMillis()
        );

        rootObject.put(
                "overall_status",
                getOverallStatus().name()
        );

        rootObject.put(
                "overall_successful",
                isOverallSuccessful()
        );

        JSONObject summaryObject =
                new JSONObject();

        summaryObject.put(
                "total",
                getTotalTestCount()
        );

        summaryObject.put(
                "passed",
                getPassedTestCount()
        );

        summaryObject.put(
                "failed",
                getFailedTestCount()
        );

        summaryObject.put(
                "warnings",
                getWarningTestCount()
        );

        summaryObject.put(
                "not_run",
                getNotRunTestCount()
        );

        rootObject.put(
                "summary",
                summaryObject
        );

        JSONArray resultsArray =
                new JSONArray();

        for (TestResult testResult
                : testResults) {

            resultsArray.put(
                    testResult.toJson()
            );
        }

        rootObject.put(
                "tests",
                resultsArray
        );

        return rootObject;
    }

    @NonNull
    public synchronized String toFormattedJson()
            throws JSONException {

        return toJson().toString(2);
    }

    private int countStatus(
            @NonNull TestStatus expectedStatus
    ) {
        int count = 0;

        for (TestResult testResult
                : testResults) {

            if (testResult.getStatus()
                    == expectedStatus) {

                count++;
            }
        }

        return count;
    }

    private void ensureReportIsOpen() {
        if (completed) {
            throw new IllegalStateException(
                    "Cloud backup test report is already complete."
            );
        }
    }

    public enum TestStatus {
        PASSED,
        FAILED,
        WARNING,
        NOT_RUN
    }

    public enum ReportStatus {
        IN_PROGRESS,
        PASSED,
        COMPLETED_WITH_WARNINGS,
        FAILED,
        INCOMPLETE
    }

    public static final class TestResult {

        private final String testId;

        private final String testName;

        private final String testDescription;

        private final TestStatus status;

        private final long startedAt;

        private final long completedAt;

        private final String userMessage;

        private final String technicalMessage;

        private final String errorType;

        private final String maskedFirebaseUserId;

        private final String maskedBackupId;

        private TestResult(
                @NonNull Builder builder
        ) {
            testId =
                    requireText(
                            builder.testId,
                            "testId"
                    );

            testName =
                    requireText(
                            builder.testName,
                            "testName"
                    );

            testDescription =
                    safeText(
                            builder.testDescription
                    );

            status =
                    builder.status == null
                            ? TestStatus.NOT_RUN
                            : builder.status;

            startedAt =
                    Math.max(
                            builder.startedAt,
                            0L
                    );

            long safeCompletedAt =
                    Math.max(
                            builder.completedAt,
                            0L
                    );

            if (safeCompletedAt > 0L
                    && safeCompletedAt < startedAt) {

                safeCompletedAt =
                        startedAt;
            }

            completedAt =
                    safeCompletedAt;

            userMessage =
                    safeText(
                            builder.userMessage
                    );

            technicalMessage =
                    sanitizeTechnicalMessage(
                            builder.technicalMessage
                    );

            errorType =
                    sanitizeTechnicalMessage(
                            builder.errorType
                    );

            maskedFirebaseUserId =
                    maskIdentifier(
                            builder.firebaseUserId
                    );

            maskedBackupId =
                    maskIdentifier(
                            builder.backupId
                    );
        }

        @NonNull
        public String getTestId() {
            return testId;
        }

        @NonNull
        public String getTestName() {
            return testName;
        }

        @NonNull
        public String getTestDescription() {
            return testDescription;
        }

        @NonNull
        public TestStatus getStatus() {
            return status;
        }

        public long getStartedAt() {
            return startedAt;
        }

        public long getCompletedAt() {
            return completedAt;
        }

        public long getDurationMillis() {
            if (startedAt <= 0L
                    || completedAt <= 0L) {

                return 0L;
            }

            return Math.max(
                    completedAt - startedAt,
                    0L
            );
        }

        @NonNull
        public String getUserMessage() {
            return userMessage;
        }

        @NonNull
        public String getTechnicalMessage() {
            return technicalMessage;
        }

        @NonNull
        public String getErrorType() {
            return errorType;
        }

        @NonNull
        public String getMaskedFirebaseUserId() {
            return maskedFirebaseUserId;
        }

        @NonNull
        public String getMaskedBackupId() {
            return maskedBackupId;
        }

        @NonNull
        private JSONObject toJson()
                throws JSONException {

            JSONObject resultObject =
                    new JSONObject();

            resultObject.put(
                    "test_id",
                    testId
            );

            resultObject.put(
                    "test_name",
                    testName
            );

            resultObject.put(
                    "test_description",
                    testDescription
            );

            resultObject.put(
                    "status",
                    status.name()
            );

            resultObject.put(
                    "started_at",
                    startedAt
            );

            if (startedAt > 0L) {
                resultObject.put(
                        "started_at_iso",
                        Instant.ofEpochMilli(
                                startedAt
                        ).toString()
                );
            } else {
                resultObject.put(
                        "started_at_iso",
                        JSONObject.NULL
                );
            }

            resultObject.put(
                    "completed_at",
                    completedAt
            );

            if (completedAt > 0L) {
                resultObject.put(
                        "completed_at_iso",
                        Instant.ofEpochMilli(
                                completedAt
                        ).toString()
                );
            } else {
                resultObject.put(
                        "completed_at_iso",
                        JSONObject.NULL
                );
            }

            resultObject.put(
                    "duration_millis",
                    getDurationMillis()
            );

            resultObject.put(
                    "user_message",
                    userMessage
            );

            resultObject.put(
                    "technical_message",
                    technicalMessage
            );

            resultObject.put(
                    "error_type",
                    errorType
            );

            resultObject.put(
                    "firebase_uid_masked",
                    maskedFirebaseUserId
            );

            resultObject.put(
                    "backup_id_masked",
                    maskedBackupId
            );

            return resultObject;
        }

        public static final class Builder {

            private final String testId;

            private final String testName;

            private String testDescription = "";

            private TestStatus status =
                    TestStatus.NOT_RUN;

            private long startedAt;

            private long completedAt;

            private String userMessage = "";

            private String technicalMessage = "";

            private String errorType = "";

            private String firebaseUserId = "";

            private String backupId = "";

            public Builder(
                    @NonNull String testId,
                    @NonNull String testName
            ) {
                this.testId =
                        testId;

                this.testName =
                        testName;
            }

            @NonNull
            public Builder setTestDescription(
                    @Nullable String testDescription
            ) {
                this.testDescription =
                        safeText(
                                testDescription
                        );

                return this;
            }

            @NonNull
            public Builder setStatus(
                    @NonNull TestStatus status
            ) {
                this.status =
                        status;

                return this;
            }

            @NonNull
            public Builder setStartedAt(
                    long startedAt
            ) {
                this.startedAt =
                        startedAt;

                return this;
            }

            @NonNull
            public Builder setCompletedAt(
                    long completedAt
            ) {
                this.completedAt =
                        completedAt;

                return this;
            }

            @NonNull
            public Builder setUserMessage(
                    @Nullable String userMessage
            ) {
                this.userMessage =
                        safeText(
                                userMessage
                        );

                return this;
            }

            @NonNull
            public Builder setTechnicalMessage(
                    @Nullable String technicalMessage
            ) {
                this.technicalMessage =
                        sanitizeTechnicalMessage(
                                technicalMessage
                        );

                return this;
            }

            @NonNull
            public Builder setError(
                    @Nullable Throwable throwable
            ) {
                if (throwable == null) {
                    errorType = "";
                    technicalMessage = "";

                    return this;
                }

                errorType =
                        throwable.getClass()
                                .getSimpleName();

                technicalMessage =
                        sanitizeTechnicalMessage(
                                throwable.getLocalizedMessage()
                        );

                return this;
            }

            @NonNull
            public Builder setFirebaseUserId(
                    @Nullable String firebaseUserId
            ) {
                this.firebaseUserId =
                        safeText(
                                firebaseUserId
                        );

                return this;
            }

            @NonNull
            public Builder setBackupId(
                    @Nullable String backupId
            ) {
                this.backupId =
                        safeText(
                                backupId
                        );

                return this;
            }

            @NonNull
            public TestResult build() {
                return new TestResult(
                        this
                );
            }
        }
    }

    @NonNull
    private static String requireText(
            @Nullable String value,
            @NonNull String fieldName
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be empty."
            );
        }

        return safeValue;
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    /**
     * Removes line breaks and limits technical text to
     * avoid oversized or accidentally sensitive reports.
     */
    @NonNull
    private static String sanitizeTechnicalMessage(
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                )
                        .replace(
                                '\n',
                                ' '
                        )
                        .replace(
                                '\r',
                                ' '
                        );

        while (safeValue.contains(
                "  "
        )) {
            safeValue =
                    safeValue.replace(
                            "  ",
                            " "
                    );
        }

        /*
         * Passphrases and tokens are never intentionally
         * supplied here. Length limiting provides an
         * additional defensive boundary.
         */
        if (safeValue.length() > 500) {
            safeValue =
                    safeValue.substring(
                            0,
                            500
                    );
        }

        return safeValue;
    }

    /**
     * Masks Firebase UID and backup ID before they are
     * written into a diagnostic report.
     */
    @NonNull
    private static String maskIdentifier(
            @Nullable String identifier
    ) {
        String safeIdentifier =
                safeText(
                        identifier
                );

        if (safeIdentifier.isEmpty()) {
            return "";
        }

        if (safeIdentifier.length() <= 4) {
            return "****";
        }

        if (safeIdentifier.length() <= 8) {
            return safeIdentifier.substring(
                    0,
                    2
            )
                    + "****"
                    + safeIdentifier.substring(
                    safeIdentifier.length() - 2
            );
        }

        int visibleStartLength = 4;
        int visibleEndLength = 4;

        return String.format(
                Locale.US,
                "%s****%s",
                safeIdentifier.substring(
                        0,
                        visibleStartLength
                ),
                safeIdentifier.substring(
                        safeIdentifier.length()
                                - visibleEndLength
                )
        );
    }
}