package com.tridev.studysaathi.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CloudBackupTestReportRepository {

    private static final String REPORT_DIRECTORY_NAME =
            "cloud_backup_test_reports";

    private static final String REPORT_FILE_PREFIX =
            "StudySaathi_Cloud_Test_";

    private static final String REPORT_FILE_EXTENSION =
            ".json";

    private static final String TEMP_FILE_EXTENSION =
            ".tmp";

    private static final String EXPECTED_REPORT_FORMAT =
            "study_saathi_cloud_backup_test_report";

    private static final int EXPECTED_REPORT_FORMAT_VERSION =
            CloudBackupTestReport.REPORT_FORMAT_VERSION;

    private static final int MAX_STORED_REPORTS =
            10;

    private static final long MAX_REPORT_FILE_SIZE_BYTES =
            2L * 1024L * 1024L;

    private static final ExecutorService repositoryExecutor =
            Executors.newSingleThreadExecutor();

    private final Context applicationContext;

    private final Handler mainThreadHandler;

    public CloudBackupTestReportRepository(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Saves a completed cloud backup test report in
     * Study Saathi's private app storage.
     */
    public void saveReportAsync(
            @NonNull CloudBackupTestReport report,
            @NonNull SaveCallback callback
    ) {
        repositoryExecutor.execute(() -> {
            try {
                if (!report.isCompleted()) {
                    throw new ReportRepositoryException(
                            "Only completed cloud backup "
                                    + "test reports can be saved."
                    );
                }

                String formattedReport =
                        report.toFormattedJson();

                SavedReport savedReport =
                        saveReport(
                                report.getReportId(),
                                formattedReport
                        );

                dispatchSaveSuccess(
                        callback,
                        savedReport
                );

            } catch (Exception exception) {
                dispatchSaveError(
                        callback,
                        exception
                );
            }
        });
    }

    /**
     * Reads the newest valid test report.
     *
     * Invalid or corrupted reports are skipped and
     * removed from private storage.
     */
    public void loadLatestReportAsync(
            @NonNull LoadCallback callback
    ) {
        repositoryExecutor.execute(() -> {
            try {
                StoredReport latestReport =
                        loadLatestReport();

                dispatchLoadSuccess(
                        callback,
                        latestReport
                );

            } catch (Exception exception) {
                dispatchLoadError(
                        callback,
                        exception
                );
            }
        });
    }

    /**
     * Returns summaries of all currently valid stored
     * reports, newest first.
     */
    public void loadReportSummariesAsync(
            @NonNull SummaryCallback callback
    ) {
        repositoryExecutor.execute(() -> {
            try {
                List<ReportSummary> summaries =
                        loadReportSummaries();

                dispatchSummarySuccess(
                        callback,
                        summaries
                );

            } catch (Exception exception) {
                dispatchSummaryError(
                        callback,
                        exception
                );
            }
        });
    }

    /**
     * Deletes every managed cloud test report.
     */
    public void clearReportsAsync(
            @Nullable ClearCallback callback
    ) {
        repositoryExecutor.execute(() -> {
            try {
                int deletedCount =
                        clearAllReports();

                dispatchClearSuccess(
                        callback,
                        deletedCount
                );

            } catch (Exception exception) {
                dispatchClearError(
                        callback,
                        exception
                );
            }
        });
    }

    @NonNull
    private SavedReport saveReport(
            @NonNull String reportId,
            @NonNull String formattedReport
    ) throws IOException,
            JSONException,
            ReportRepositoryException {

        validateReportId(
                reportId
        );

        byte[] reportBytes =
                formattedReport.getBytes(
                        StandardCharsets.UTF_8
                );

        if (reportBytes.length == 0) {
            throw new ReportRepositoryException(
                    "Cloud backup test report is empty."
            );
        }

        if (reportBytes.length
                > MAX_REPORT_FILE_SIZE_BYTES) {

            throw new ReportRepositoryException(
                    "Cloud backup test report exceeds "
                            + "the supported size."
            );
        }

        JSONObject reportJson =
                new JSONObject(
                        formattedReport
                );

        validateReportJson(
                reportJson
        );

        File reportDirectory =
                prepareReportDirectory();

        String safeReportId =
                createSafeReportId(
                        reportId
                );

        String reportFileName =
                REPORT_FILE_PREFIX
                        + safeReportId
                        + REPORT_FILE_EXTENSION;

        File finalReportFile =
                new File(
                        reportDirectory,
                        reportFileName
                );

        File temporaryReportFile =
                new File(
                        reportDirectory,
                        reportFileName
                                + TEMP_FILE_EXTENSION
                );

        deleteManagedFile(
                temporaryReportFile
        );

        writeTemporaryReport(
                temporaryReportFile,
                formattedReport
        );

        if (finalReportFile.exists()
                && !deleteManagedFile(
                finalReportFile
        )) {

            deleteManagedFile(
                    temporaryReportFile
            );

            throw new IOException(
                    "Existing cloud test report "
                            + "could not be replaced."
            );
        }

        if (!temporaryReportFile.renameTo(
                finalReportFile
        )) {
            deleteManagedFile(
                    temporaryReportFile
            );

            throw new IOException(
                    "Cloud test report could not "
                            + "be finalized."
            );
        }

        if (!isManagedReportFile(
                finalReportFile
        )) {
            deleteManagedFile(
                    finalReportFile
            );

            throw new ReportRepositoryException(
                    "Cloud test report path validation failed."
            );
        }

        if (finalReportFile.length()
                != reportBytes.length) {

            deleteManagedFile(
                    finalReportFile
            );

            throw new ReportRepositoryException(
                    "Stored cloud test report size "
                            + "does not match source data."
            );
        }

        pruneOldReports(
                reportDirectory
        );

        return new SavedReport(
                finalReportFile.getAbsolutePath(),
                finalReportFile.getName(),
                reportJson.optString(
                        "report_id",
                        reportId
                ),
                reportJson.optLong(
                        "completed_at",
                        0L
                ),
                reportJson.optString(
                        "overall_status",
                        ""
                ),
                finalReportFile.length()
        );
    }

    @Nullable
    private StoredReport loadLatestReport()
            throws IOException {

        File reportDirectory =
                getReportDirectory();

        if (!reportDirectory.exists()) {
            return null;
        }

        if (!reportDirectory.isDirectory()) {
            throw new IOException(
                    "Cloud test report path is "
                            + "not a directory."
            );
        }

        File[] reportFiles =
                getManagedReportFiles(
                        reportDirectory
                );

        if (reportFiles.length == 0) {
            return null;
        }

        sortNewestFirst(
                reportFiles
        );

        for (File reportFile : reportFiles) {
            try {
                return readStoredReport(
                        reportFile
                );

            } catch (Exception exception) {
                /*
                 * Corrupted managed diagnostic reports
                 * are removed and the next valid report
                 * is attempted.
                 */
                deleteManagedFile(
                        reportFile
                );
            }
        }

        return null;
    }

    @NonNull
    private List<ReportSummary> loadReportSummaries()
            throws IOException {

        File reportDirectory =
                getReportDirectory();

        if (!reportDirectory.exists()) {
            return Collections.emptyList();
        }

        if (!reportDirectory.isDirectory()) {
            throw new IOException(
                    "Cloud test report path is "
                            + "not a directory."
            );
        }

        File[] reportFiles =
                getManagedReportFiles(
                        reportDirectory
                );

        sortNewestFirst(
                reportFiles
        );

        List<ReportSummary> summaries =
                new ArrayList<>();

        for (File reportFile : reportFiles) {
            try {
                StoredReport storedReport =
                        readStoredReport(
                                reportFile
                        );

                JSONObject reportJson =
                        storedReport.getReportJson();

                JSONObject summaryJson =
                        reportJson.getJSONObject(
                                "summary"
                        );

                summaries.add(
                        new ReportSummary(
                                reportJson.getString(
                                        "report_id"
                                ),
                                reportJson.optLong(
                                        "started_at",
                                        0L
                                ),
                                reportJson.optLong(
                                        "completed_at",
                                        0L
                                ),
                                reportJson.optString(
                                        "overall_status",
                                        ""
                                ),
                                summaryJson.optInt(
                                        "total",
                                        0
                                ),
                                summaryJson.optInt(
                                        "passed",
                                        0
                                ),
                                summaryJson.optInt(
                                        "failed",
                                        0
                                ),
                                summaryJson.optInt(
                                        "warnings",
                                        0
                                ),
                                summaryJson.optInt(
                                        "not_run",
                                        0
                                ),
                                reportFile.getAbsolutePath(),
                                reportFile.length()
                        )
                );

            } catch (Exception exception) {
                deleteManagedFile(
                        reportFile
                );
            }
        }

        return Collections.unmodifiableList(
                summaries
        );
    }

    @NonNull
    private StoredReport readStoredReport(
            @NonNull File reportFile
    ) throws IOException,
            JSONException,
            ReportRepositoryException {

        if (!isManagedReportFile(
                reportFile
        )) {
            throw new ReportRepositoryException(
                    "Cloud test report file is invalid."
            );
        }

        if (reportFile.length() <= 0L
                || reportFile.length()
                > MAX_REPORT_FILE_SIZE_BYTES) {

            throw new ReportRepositoryException(
                    "Cloud test report size is invalid."
            );
        }

        String reportText =
                readTextFile(
                        reportFile
                );

        JSONObject reportJson =
                new JSONObject(
                        reportText
                );

        validateReportJson(
                reportJson
        );

        return new StoredReport(
                reportJson,
                reportFile.getAbsolutePath(),
                reportFile.getName(),
                reportFile.length()
        );
    }

    private void validateReportJson(
            @NonNull JSONObject reportJson
    ) throws JSONException,
            ReportRepositoryException {

        String reportFormat =
                reportJson.optString(
                        "report_format",
                        ""
                );

        if (!EXPECTED_REPORT_FORMAT.equals(
                reportFormat
        )) {
            throw new ReportRepositoryException(
                    "Unsupported cloud test report format."
            );
        }

        int reportFormatVersion =
                reportJson.optInt(
                        "report_format_version",
                        -1
                );

        if (reportFormatVersion
                != EXPECTED_REPORT_FORMAT_VERSION) {

            throw new ReportRepositoryException(
                    "Unsupported cloud test report version."
            );
        }

        String reportId =
                reportJson.optString(
                        "report_id",
                        ""
                );

        validateReportId(
                reportId
        );

        if (!reportJson.has(
                "started_at"
        )
                || !reportJson.has(
                "completed_at"
        )
                || !reportJson.has(
                "overall_status"
        )
                || !reportJson.has(
                "summary"
        )
                || !reportJson.has(
                "tests"
        )) {

            throw new ReportRepositoryException(
                    "Cloud test report structure is incomplete."
            );
        }

        if (!reportJson.optBoolean(
                "completed",
                false
        )) {
            throw new ReportRepositoryException(
                    "Incomplete cloud test report cannot "
                            + "be stored."
            );
        }

        JSONObject summaryObject =
                reportJson.getJSONObject(
                        "summary"
                );

        int total =
                summaryObject.optInt(
                        "total",
                        -1
                );

        int passed =
                summaryObject.optInt(
                        "passed",
                        -1
                );

        int failed =
                summaryObject.optInt(
                        "failed",
                        -1
                );

        int warnings =
                summaryObject.optInt(
                        "warnings",
                        -1
                );

        int notRun =
                summaryObject.optInt(
                        "not_run",
                        -1
                );

        if (total < 0
                || passed < 0
                || failed < 0
                || warnings < 0
                || notRun < 0) {

            throw new ReportRepositoryException(
                    "Cloud test report summary "
                            + "contains invalid counts."
            );
        }

        int calculatedTotal =
                passed
                        + failed
                        + warnings
                        + notRun;

        if (total != calculatedTotal) {
            throw new ReportRepositoryException(
                    "Cloud test report summary "
                            + "counts are inconsistent."
            );
        }

        if (reportJson.getJSONArray(
                "tests"
        ).length() != total) {

            throw new ReportRepositoryException(
                    "Cloud test report result count "
                            + "does not match summary."
            );
        }
    }

    private void validateReportId(
            @NonNull String reportId
    ) throws ReportRepositoryException {

        String trimmedReportId =
                reportId.trim();

        if (trimmedReportId.isEmpty()) {
            throw new ReportRepositoryException(
                    "Cloud test report ID is unavailable."
            );
        }

        if (trimmedReportId.length() > 150) {
            throw new ReportRepositoryException(
                    "Cloud test report ID is too long."
            );
        }

        if (!trimmedReportId.matches(
                "[a-zA-Z0-9_-]+"
        )) {
            throw new ReportRepositoryException(
                    "Cloud test report ID contains "
                            + "unsupported characters."
            );
        }
    }

    @NonNull
    private String readTextFile(
            @NonNull File reportFile
    ) throws IOException {

        try (InputStream inputStream =
                     new FileInputStream(
                             reportFile
                     );

             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;
            long totalBytes = 0L;

            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {

                totalBytes += bytesRead;

                if (totalBytes
                        > MAX_REPORT_FILE_SIZE_BYTES) {

                    throw new IOException(
                            "Cloud test report exceeds "
                                    + "the supported size."
                    );
                }

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return outputStream.toString(
                    StandardCharsets.UTF_8.name()
            );
        }
    }

    private void writeTemporaryReport(
            @NonNull File temporaryFile,
            @NonNull String reportText
    ) throws IOException {

        try (FileOutputStream fileOutputStream =
                     new FileOutputStream(
                             temporaryFile,
                             false
                     );

             OutputStreamWriter writer =
                     new OutputStreamWriter(
                             fileOutputStream,
                             StandardCharsets.UTF_8
                     )) {

            writer.write(
                    reportText
            );

            writer.flush();

            fileOutputStream
                    .getFD()
                    .sync();
        }

        if (!temporaryFile.exists()
                || temporaryFile.length() <= 0L) {

            throw new IOException(
                    "Temporary cloud test report "
                            + "was not created correctly."
            );
        }

        if (temporaryFile.length()
                > MAX_REPORT_FILE_SIZE_BYTES) {

            deleteManagedFile(
                    temporaryFile
            );

            throw new IOException(
                    "Temporary cloud test report "
                            + "exceeds the supported size."
            );
        }
    }

    @NonNull
    private File prepareReportDirectory()
            throws IOException {

        File reportDirectory =
                getReportDirectory();

        if (!reportDirectory.exists()
                && !reportDirectory.mkdirs()) {

            throw new IOException(
                    "Unable to create private cloud "
                            + "test report directory."
            );
        }

        if (!reportDirectory.isDirectory()) {
            throw new IOException(
                    "Cloud test report path is "
                            + "not a directory."
            );
        }

        return reportDirectory;
    }

    private void pruneOldReports(
            @NonNull File reportDirectory
    ) {
        File[] reportFiles =
                getManagedReportFiles(
                        reportDirectory
                );

        if (reportFiles.length
                <= MAX_STORED_REPORTS) {
            return;
        }

        sortNewestFirst(
                reportFiles
        );

        for (int index = MAX_STORED_REPORTS;
             index < reportFiles.length;
             index++) {

            deleteManagedFile(
                    reportFiles[index]
            );
        }
    }

    private int clearAllReports()
            throws IOException {

        File reportDirectory =
                getReportDirectory();

        if (!reportDirectory.exists()) {
            return 0;
        }

        if (!reportDirectory.isDirectory()) {
            throw new IOException(
                    "Cloud test report path is "
                            + "not a directory."
            );
        }

        File[] files =
                reportDirectory.listFiles();

        if (files == null) {
            throw new IOException(
                    "Cloud test report directory "
                            + "could not be inspected."
            );
        }

        int deletedCount = 0;

        for (File file : files) {
            if (!isManagedReportPath(
                    file
            )) {
                continue;
            }

            if (deleteManagedFile(
                    file
            )) {
                deletedCount++;
            }
        }

        File[] remainingFiles =
                reportDirectory.listFiles();

        if (remainingFiles != null
                && remainingFiles.length == 0) {

            reportDirectory.delete();
        }

        return deletedCount;
    }

    @NonNull
    private File[] getManagedReportFiles(
            @NonNull File reportDirectory
    ) {
        File[] files =
                reportDirectory.listFiles(
                        file ->
                                file != null
                                        && isManagedReportFile(
                                        file
                                )
                );

        return files == null
                ? new File[0]
                : files;
    }

    private void sortNewestFirst(
            @NonNull File[] reportFiles
    ) {
        Arrays.sort(
                reportFiles,
                Comparator.comparingLong(
                        File::lastModified
                ).reversed()
        );
    }

    private boolean isManagedReportFile(
            @NonNull File file
    ) {
        return file.isFile()
                && file.getName().startsWith(
                REPORT_FILE_PREFIX
        )
                && file.getName().endsWith(
                REPORT_FILE_EXTENSION
        )
                && isManagedReportPath(
                file
        );
    }

    private boolean isManagedReportPath(
            @NonNull File file
    ) {
        try {
            File reportDirectory =
                    getReportDirectory()
                            .getCanonicalFile();

            File canonicalFile =
                    file.getCanonicalFile();

            String directoryPath =
                    reportDirectory.getPath()
                            + File.separator;

            String filePath =
                    canonicalFile.getPath();

            String fileName =
                    canonicalFile.getName();

            boolean validName =
                    fileName.startsWith(
                            REPORT_FILE_PREFIX
                    )
                            && (
                            fileName.endsWith(
                                    REPORT_FILE_EXTENSION
                            )
                                    || fileName.endsWith(
                                    TEMP_FILE_EXTENSION
                            )
                    );

            return filePath.startsWith(
                    directoryPath
            )
                    && validName;

        } catch (IOException exception) {
            return false;
        }
    }

    private boolean deleteManagedFile(
            @NonNull File file
    ) {
        if (!file.exists()) {
            return true;
        }

        if (!file.isFile()
                || !isManagedReportPath(
                file
        )) {
            return false;
        }

        return file.delete();
    }

    @NonNull
    private String createSafeReportId(
            @NonNull String reportId
    ) {
        String safeReportId =
                reportId.trim()
                        .replaceAll(
                                "[^a-zA-Z0-9_-]",
                                "_"
                        );

        if (safeReportId.length() > 150) {
            safeReportId =
                    safeReportId.substring(
                            0,
                            150
                    );
        }

        return safeReportId;
    }

    @NonNull
    private File getReportDirectory() {
        return new File(
                applicationContext.getFilesDir(),
                REPORT_DIRECTORY_NAME
        );
    }

    private void dispatchSaveSuccess(
            @NonNull SaveCallback callback,
            @NonNull SavedReport savedReport
    ) {
        mainThreadHandler.post(() ->
                callback.onSaved(
                        savedReport
                )
        );
    }

    private void dispatchSaveError(
            @NonNull SaveCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(() ->
                callback.onError(
                        exception
                )
        );
    }

    private void dispatchLoadSuccess(
            @NonNull LoadCallback callback,
            @Nullable StoredReport storedReport
    ) {
        mainThreadHandler.post(() ->
                callback.onLoaded(
                        storedReport
                )
        );
    }

    private void dispatchLoadError(
            @NonNull LoadCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(() ->
                callback.onError(
                        exception
                )
        );
    }

    private void dispatchSummarySuccess(
            @NonNull SummaryCallback callback,
            @NonNull List<ReportSummary> summaries
    ) {
        mainThreadHandler.post(() ->
                callback.onLoaded(
                        summaries
                )
        );
    }

    private void dispatchSummaryError(
            @NonNull SummaryCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(() ->
                callback.onError(
                        exception
                )
        );
    }

    private void dispatchClearSuccess(
            @Nullable ClearCallback callback,
            int deletedCount
    ) {
        if (callback == null) {
            return;
        }

        mainThreadHandler.post(() ->
                callback.onCleared(
                        deletedCount
                )
        );
    }

    private void dispatchClearError(
            @Nullable ClearCallback callback,
            @NonNull Exception exception
    ) {
        if (callback == null) {
            return;
        }

        mainThreadHandler.post(() ->
                callback.onError(
                        exception
                )
        );
    }

    public interface SaveCallback {

        void onSaved(
                @NonNull SavedReport savedReport
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public interface LoadCallback {

        void onLoaded(
                @Nullable StoredReport storedReport
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public interface SummaryCallback {

        void onLoaded(
                @NonNull List<ReportSummary> summaries
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public interface ClearCallback {

        void onCleared(
                int deletedReportCount
        );

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class SavedReport {

        private final String absoluteFilePath;

        private final String displayFileName;

        private final String reportId;

        private final long completedAt;

        private final String overallStatus;

        private final long fileSizeBytes;

        private SavedReport(
                @NonNull String absoluteFilePath,
                @NonNull String displayFileName,
                @NonNull String reportId,
                long completedAt,
                @NonNull String overallStatus,
                long fileSizeBytes
        ) {
            this.absoluteFilePath =
                    absoluteFilePath;

            this.displayFileName =
                    displayFileName;

            this.reportId =
                    reportId;

            this.completedAt =
                    completedAt;

            this.overallStatus =
                    overallStatus;

            this.fileSizeBytes =
                    fileSizeBytes;
        }

        @NonNull
        public String getAbsoluteFilePath() {
            return absoluteFilePath;
        }

        @NonNull
        public String getDisplayFileName() {
            return displayFileName;
        }

        @NonNull
        public String getReportId() {
            return reportId;
        }

        public long getCompletedAt() {
            return completedAt;
        }

        @NonNull
        public String getOverallStatus() {
            return overallStatus;
        }

        public long getFileSizeBytes() {
            return fileSizeBytes;
        }
    }

    public static final class StoredReport {

        private final JSONObject reportJson;

        private final String absoluteFilePath;

        private final String displayFileName;

        private final long fileSizeBytes;

        private StoredReport(
                @NonNull JSONObject reportJson,
                @NonNull String absoluteFilePath,
                @NonNull String displayFileName,
                long fileSizeBytes
        ) {
            this.reportJson =
                    reportJson;

            this.absoluteFilePath =
                    absoluteFilePath;

            this.displayFileName =
                    displayFileName;

            this.fileSizeBytes =
                    fileSizeBytes;
        }

        @NonNull
        public JSONObject getReportJson() {
            return reportJson;
        }

        @NonNull
        public String getAbsoluteFilePath() {
            return absoluteFilePath;
        }

        @NonNull
        public String getDisplayFileName() {
            return displayFileName;
        }

        public long getFileSizeBytes() {
            return fileSizeBytes;
        }
    }

    public static final class ReportSummary {

        private final String reportId;

        private final long startedAt;

        private final long completedAt;

        private final String overallStatus;

        private final int totalCount;

        private final int passedCount;

        private final int failedCount;

        private final int warningCount;

        private final int notRunCount;

        private final String absoluteFilePath;

        private final long fileSizeBytes;

        private ReportSummary(
                @NonNull String reportId,
                long startedAt,
                long completedAt,
                @NonNull String overallStatus,
                int totalCount,
                int passedCount,
                int failedCount,
                int warningCount,
                int notRunCount,
                @NonNull String absoluteFilePath,
                long fileSizeBytes
        ) {
            this.reportId =
                    reportId;

            this.startedAt =
                    startedAt;

            this.completedAt =
                    completedAt;

            this.overallStatus =
                    overallStatus;

            this.totalCount =
                    totalCount;

            this.passedCount =
                    passedCount;

            this.failedCount =
                    failedCount;

            this.warningCount =
                    warningCount;

            this.notRunCount =
                    notRunCount;

            this.absoluteFilePath =
                    absoluteFilePath;

            this.fileSizeBytes =
                    fileSizeBytes;
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

        @NonNull
        public String getOverallStatus() {
            return overallStatus;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getPassedCount() {
            return passedCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public int getNotRunCount() {
            return notRunCount;
        }

        @NonNull
        public String getAbsoluteFilePath() {
            return absoluteFilePath;
        }

        public long getFileSizeBytes() {
            return fileSizeBytes;
        }
    }

    public static final class ReportRepositoryException
            extends Exception {

        public ReportRepositoryException(
                @NonNull String message
        ) {
            super(message);
        }

        public ReportRepositoryException(
                @NonNull String message,
                @NonNull Throwable cause
        ) {
            super(
                    message,
                    cause
            );
        }
    }
}