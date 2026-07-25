package com.tridev.studysaathi;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.cloud.CloudBackupDiagnosticRunner;
import com.tridev.studysaathi.cloud.CloudBackupTestReport;
import com.tridev.studysaathi.cloud.CloudBackupTestReportRepository;
import com.tridev.studysaathi.databinding.ActivityCloudBackupDiagnosticBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import android.os.Build;

import androidx.core.content.pm.PackageInfoCompat;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public final class CloudBackupDiagnosticActivity
        extends AppCompatActivity {

    private static final int REQUEST_EXPORT_DIAGNOSTIC_REPORT = 4608;

    private ActivityCloudBackupDiagnosticBinding binding;

    private CloudBackupDiagnosticRunner diagnosticRunner;

    private CloudBackupTestReportRepository reportRepository;

    private boolean operationInProgress;

    private boolean reportCurrentlyVisible;

    @Nullable
    private JSONObject currentReportJson;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityCloudBackupDiagnosticBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        diagnosticRunner =
                new CloudBackupDiagnosticRunner(
                        this
                );

        reportRepository =
                new CloudBackupTestReportRepository(
                        this
                );

        setupToolbar();
        setupClickListeners();

        showEmptyReportState(
                R.string.cloud_diagnostic_loading_latest
        );

        loadLatestSavedReport();
    }

    private void setupToolbar() {
        binding.toolbarCloudDiagnostic
                .setNavigationOnClickListener(view ->
                        getOnBackPressedDispatcher()
                                .onBackPressed()
                );
    }

    private void setupClickListeners() {
        binding.buttonRunCloudDiagnostic
                .setOnClickListener(view ->
                        runCloudDiagnostic()
                );

        binding.buttonRefreshCloudDiagnosticReport
                .setOnClickListener(view ->
                        loadLatestSavedReport()
                );

        binding.buttonExportCloudDiagnosticReport
                .setOnClickListener(view ->
                        exportCurrentDiagnosticReport()
                );

        binding.buttonShareCloudDiagnosticReport
                .setOnClickListener(view ->
                        shareCurrentDiagnosticReport()
                );

        binding.buttonClearCloudDiagnosticReports
                .setOnClickListener(view ->
                        showClearReportsConfirmation()
                );
    }

    private void runCloudDiagnostic() {
        if (operationInProgress) {
            return;
        }

        if (diagnosticRunner.isTestRunning()) {
            Snackbar.make(
                    binding.getRoot(),
                    R.string.cloud_diagnostic_already_running,
                    Snackbar.LENGTH_SHORT
            ).show();

            return;
        }

        showOperationState(
                true,
                true
        );

        diagnosticRunner.runDiagnostics(
                new CloudBackupDiagnosticRunner
                        .DiagnosticCallback() {

                    @Override
                    public void onCompleted(
                            @NonNull CloudBackupTestReport report,
                            @NonNull CloudBackupTestReportRepository
                                    .SavedReport savedReport
                    ) {
                        showOperationState(
                                false,
                                false
                        );

                        try {
                            renderReport(
                                    report.toJson()
                            );

                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.cloud_diagnostic_test_completed,
                                    Snackbar.LENGTH_LONG
                            ).show();

                        } catch (JSONException exception) {
                            Snackbar.make(
                                    binding.getRoot(),
                                    createErrorMessage(
                                            getString(
                                                    R.string.cloud_diagnostic_report_load_failed
                                            ),
                                            exception
                                    ),
                                    Snackbar.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        showOperationState(
                                false,
                                false
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                createErrorMessage(
                                        getString(
                                                R.string.cloud_diagnostic_test_failed
                                        ),
                                        exception
                                ),
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void loadLatestSavedReport() {
        if (operationInProgress) {
            return;
        }

        showOperationState(
                true,
                false
        );

        if (!reportCurrentlyVisible) {
            showEmptyReportState(
                    R.string.cloud_diagnostic_loading_latest
            );
        }

        reportRepository.loadLatestReportAsync(
                new CloudBackupTestReportRepository
                        .LoadCallback() {

                    @Override
                    public void onLoaded(
                            @Nullable CloudBackupTestReportRepository
                                    .StoredReport storedReport
                    ) {
                        showOperationState(
                                false,
                                false
                        );

                        if (storedReport == null) {
                            showEmptyReportState(
                                    R.string.cloud_diagnostic_empty_description
                            );

                            return;
                        }

                        try {
                            renderReport(
                                    storedReport.getReportJson()
                            );

                        } catch (JSONException exception) {
                            showEmptyReportState(
                                    R.string.cloud_diagnostic_empty_description
                            );

                            Snackbar.make(
                                    binding.getRoot(),
                                    createErrorMessage(
                                            getString(
                                                    R.string.cloud_diagnostic_report_load_failed
                                            ),
                                            exception
                                    ),
                                    Snackbar.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        showOperationState(
                                false,
                                false
                        );

                        if (!reportCurrentlyVisible) {
                            showEmptyReportState(
                                    R.string.cloud_diagnostic_empty_description
                            );
                        }

                        Snackbar.make(
                                binding.getRoot(),
                                createErrorMessage(
                                        getString(
                                                R.string.cloud_diagnostic_report_load_failed
                                        ),
                                        exception
                                ),
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showClearReportsConfirmation() {
        if (operationInProgress) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.cloud_diagnostic_clear_title
                )
                .setMessage(
                        R.string.cloud_diagnostic_clear_message
                )
                .setNegativeButton(
                        R.string.cloud_diagnostic_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.cloud_diagnostic_clear_confirm,
                        (dialog, which) ->
                                clearSavedReports()
                )
                .show();
    }

    private void clearSavedReports() {
        showOperationState(
                true,
                false
        );

        reportRepository.clearReportsAsync(
                new CloudBackupTestReportRepository
                        .ClearCallback() {

                    @Override
                    public void onCleared(
                            int deletedReportCount
                    ) {
                        showOperationState(
                                false,
                                false
                        );

                        showEmptyReportState(
                                R.string.cloud_diagnostic_empty_description
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.cloud_diagnostic_reports_cleared,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        showOperationState(
                                false,
                                false
                        );

                        Snackbar.make(
                                binding.getRoot(),
                                createErrorMessage(
                                        getString(
                                                R.string.cloud_diagnostic_reports_clear_failed
                                        ),
                                        exception
                                ),
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void renderReport(
            @NonNull JSONObject reportJson
    ) throws JSONException {

        currentReportJson =
                new JSONObject(
                        reportJson.toString()
                );

        reportCurrentlyVisible = true;

        binding.cardCloudDiagnosticEmpty.setVisibility(
                View.GONE
        );

        binding.cardCloudDiagnosticSummary.setVisibility(
                View.VISIBLE
        );

        binding.textCloudDiagnosticResultsTitle.setVisibility(
                View.VISIBLE
        );

        binding.layoutCloudDiagnosticExportActions.setVisibility(
                View.VISIBLE
        );

        binding.buttonClearCloudDiagnosticReports.setVisibility(
                View.VISIBLE
        );

        JSONObject summaryObject =
                reportJson.getJSONObject(
                        "summary"
                );

        int passedCount =
                summaryObject.optInt(
                        "passed",
                        0
                );

        int failedCount =
                summaryObject.optInt(
                        "failed",
                        0
                );

        int warningCount =
                summaryObject.optInt(
                        "warnings",
                        0
                );

        int notRunCount =
                summaryObject.optInt(
                        "not_run",
                        0
                );

        binding.textCloudDiagnosticPassedCount.setText(
                String.valueOf(
                        passedCount
                )
        );

        binding.textCloudDiagnosticFailedCount.setText(
                String.valueOf(
                        failedCount
                )
        );

        binding.textCloudDiagnosticWarningCount.setText(
                String.valueOf(
                        warningCount
                )
        );

        binding.textCloudDiagnosticPendingCount.setText(
                String.valueOf(
                        notRunCount
                )
        );

        String overallStatus =
                reportJson.optString(
                        "overall_status",
                        "INCOMPLETE"
                );

        binding.textCloudDiagnosticOverallStatus.setText(
                getOverallStatusText(
                        overallStatus
                )
        );

        binding.textCloudDiagnosticOverallStatus.setTextColor(
                getStatusColor(
                        binding.textCloudDiagnosticOverallStatus,
                        overallStatus
                )
        );

        long completedAt =
                reportJson.optLong(
                        "completed_at",
                        0L
                );

        binding.textCloudDiagnosticCompletedAt.setText(
                createCompletedAtText(
                        completedAt
                )
        );

        String reportId =
                reportJson.optString(
                        "report_id",
                        ""
                );

        binding.textCloudDiagnosticReportId.setText(
                reportId.isEmpty()
                        ? getString(
                        R.string.cloud_diagnostic_report_id_none
                )
                        : getString(
                        R.string.cloud_diagnostic_report_id_format,
                        reportId
                )
        );

        JSONArray testResults =
                reportJson.getJSONArray(
                        "tests"
                );

        renderTestResults(
                testResults
        );
    }

    private void renderTestResults(
            @NonNull JSONArray testResults
    ) throws JSONException {

        binding.containerCloudDiagnosticResults
                .removeAllViews();

        for (int index = 0;
             index < testResults.length();
             index++) {

            JSONObject testObject =
                    testResults.getJSONObject(
                            index
                    );

            View resultView =
                    LayoutInflater.from(this)
                            .inflate(
                                    R.layout.item_cloud_diagnostic_result,
                                    binding.containerCloudDiagnosticResults,
                                    false
                            );

            TextView testName =
                    resultView.findViewById(
                            R.id.textCloudDiagnosticTestName
                    );

            TextView testStatus =
                    resultView.findViewById(
                            R.id.textCloudDiagnosticTestStatus
                    );

            TextView testDescription =
                    resultView.findViewById(
                            R.id.textCloudDiagnosticTestDescription
                    );

            TextView testMessage =
                    resultView.findViewById(
                            R.id.textCloudDiagnosticTestMessage
                    );

            TextView technicalMessage =
                    resultView.findViewById(
                            R.id.textCloudDiagnosticTechnicalMessage
                    );

            String status =
                    testObject.optString(
                            "status",
                            "NOT_RUN"
                    );

            testName.setText(
                    testObject.optString(
                            "test_name",
                            getString(
                                    R.string.cloud_diagnostic_test_name_placeholder
                            )
                    )
            );

            testStatus.setText(
                    getTestStatusText(
                            status
                    )
            );

            applyStatusBadgeStyle(
                    testStatus,
                    status
            );

            testDescription.setText(
                    testObject.optString(
                            "test_description",
                            getString(
                                    R.string.cloud_diagnostic_test_description_placeholder
                            )
                    )
            );

            String userMessage =
                    testObject.optString(
                            "user_message",
                            ""
                    );

            if (userMessage.isEmpty()) {
                userMessage =
                        getString(
                                R.string.cloud_diagnostic_test_message_placeholder
                        );
            }

            testMessage.setText(
                    userMessage
            );

            String technicalDetails =
                    createTechnicalDetails(
                            testObject
                    );

            if (technicalDetails.isEmpty()) {
                technicalMessage.setVisibility(
                        View.GONE
                );

            } else {
                technicalMessage.setVisibility(
                        View.VISIBLE
                );

                technicalMessage.setText(
                        technicalDetails
                );
            }

            binding.containerCloudDiagnosticResults
                    .addView(
                            resultView
                    );
        }
    }

    @NonNull
    private String createTechnicalDetails(
            @NonNull JSONObject testObject
    ) {
        String technicalMessage =
                testObject.optString(
                        "technical_message",
                        ""
                );

        String errorType =
                testObject.optString(
                        "error_type",
                        ""
                );

        String maskedUserId =
                testObject.optString(
                        "firebase_uid_masked",
                        ""
                );

        String maskedBackupId =
                testObject.optString(
                        "backup_id_masked",
                        ""
                );

        StringBuilder detailsBuilder =
                new StringBuilder();

        if (!technicalMessage.isEmpty()) {
            detailsBuilder.append(
                    technicalMessage
            );
        }

        if (!errorType.isEmpty()) {
            appendTechnicalLine(
                    detailsBuilder,
                    "Error: " + errorType
            );
        }

        if (!maskedUserId.isEmpty()) {
            appendTechnicalLine(
                    detailsBuilder,
                    "Firebase UID: " + maskedUserId
            );
        }

        if (!maskedBackupId.isEmpty()) {
            appendTechnicalLine(
                    detailsBuilder,
                    "Backup ID: " + maskedBackupId
            );
        }

        return detailsBuilder.toString();
    }

    private void appendTechnicalLine(
            @NonNull StringBuilder builder,
            @NonNull String value
    ) {
        if (builder.length() > 0) {
            builder.append(
                    '\n'
            );
        }

        builder.append(
                value
        );
    }

    private void applyStatusBadgeStyle(
            @NonNull TextView statusView,
            @NonNull String status
    ) {
        int statusColor =
                getStatusColor(
                        statusView,
                        status
                );

        statusView.setTextColor(
                statusColor
        );

        int backgroundColor =
                ColorUtils.setAlphaComponent(
                        statusColor,
                        28
                );

        statusView.setBackgroundTintList(
                ColorStateList.valueOf(
                        backgroundColor
                )
        );
    }

    private int getStatusColor(
            @NonNull View referenceView,
            @NonNull String status
    ) {
        String normalizedStatus =
                status.trim()
                        .toUpperCase(
                                Locale.US
                        );

        switch (normalizedStatus) {
            case "PASSED":
                return ContextCompat.getColor(
                        this,
                        R.color.ss_success
                );

            case "FAILED":
                return ContextCompat.getColor(
                        this,
                        R.color.ss_error
                );

            case "WARNING":
            case "COMPLETED_WITH_WARNINGS":
                return ContextCompat.getColor(
                        this,
                        R.color.ss_warning
                );

            case "NOT_RUN":
            case "INCOMPLETE":
            case "IN_PROGRESS":
            default:
                return MaterialColors.getColor(
                        referenceView,
                        com.google.android.material.R.attr
                                .colorOnSurfaceVariant
                );
        }
    }

    @NonNull
    private String getTestStatusText(
            @NonNull String status
    ) {
        String normalizedStatus =
                status.trim()
                        .toUpperCase(
                                Locale.US
                        );

        switch (normalizedStatus) {
            case "PASSED":
                return getString(
                        R.string.cloud_diagnostic_status_passed
                );

            case "FAILED":
                return getString(
                        R.string.cloud_diagnostic_status_failed
                );

            case "WARNING":
                return getString(
                        R.string.cloud_diagnostic_status_warning
                );

            case "NOT_RUN":
            default:
                return getString(
                        R.string.cloud_diagnostic_status_not_run
                );
        }
    }

    @NonNull
    private String getOverallStatusText(
            @NonNull String overallStatus
    ) {
        String normalizedStatus =
                overallStatus.trim()
                        .toUpperCase(
                                Locale.US
                        );

        switch (normalizedStatus) {
            case "PASSED":
                return getString(
                        R.string.cloud_diagnostic_status_passed
                );

            case "FAILED":
                return getString(
                        R.string.cloud_diagnostic_status_failed
                );

            case "COMPLETED_WITH_WARNINGS":
                return getString(
                        R.string
                                .cloud_diagnostic_status_completed_with_warnings
                );

            case "IN_PROGRESS":
                return getString(
                        R.string.cloud_diagnostic_status_in_progress
                );

            case "INCOMPLETE":
            default:
                return getString(
                        R.string.cloud_diagnostic_status_incomplete
                );
        }
    }

    @NonNull
    private String createCompletedAtText(
            long completedAt
    ) {
        if (completedAt <= 0L) {
            return getString(
                    R.string.cloud_diagnostic_completed_none
            );
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                );

        String formattedTime =
                Instant.ofEpochMilli(
                                completedAt
                        )
                        .atZone(
                                ZoneId.systemDefault()
                        )
                        .format(
                                formatter
                        );

        return getString(
                R.string.cloud_diagnostic_completed_format,
                formattedTime
        );
    }

    private void showEmptyReportState(
            int descriptionStringResource
    ) {

        currentReportJson = null;

        reportCurrentlyVisible = false;

        binding.cardCloudDiagnosticSummary.setVisibility(
                View.GONE
        );

        binding.textCloudDiagnosticResultsTitle.setVisibility(
                View.GONE
        );

        binding.containerCloudDiagnosticResults
                .removeAllViews();

        binding.layoutCloudDiagnosticExportActions.setVisibility(
                View.GONE
        );



        binding.buttonClearCloudDiagnosticReports.setVisibility(
                View.GONE
        );

        binding.cardCloudDiagnosticEmpty.setVisibility(
                View.VISIBLE
        );

        binding.textCloudDiagnosticEmptyDescription.setText(
                descriptionStringResource
        );
    }

    private void showOperationState(
            boolean inProgress,
            boolean diagnosticRunning
    ) {
        operationInProgress =
                inProgress;

        binding.progressCloudDiagnostic.setVisibility(
                inProgress
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.buttonRunCloudDiagnostic.setEnabled(
                !inProgress
        );

        binding.buttonRefreshCloudDiagnosticReport.setEnabled(
                !inProgress
        );

        binding.buttonClearCloudDiagnosticReports.setEnabled(
                !inProgress
        );

        binding.buttonRunCloudDiagnostic.setText(
                inProgress && diagnosticRunning
                        ? R.string.cloud_diagnostic_running_action
                        : R.string.cloud_diagnostic_run_action
        );

        binding.contentCloudDiagnostic.setAlpha(
                inProgress
                        ? 0.72f
                        : 1f
        );
    }

    private void exportCurrentDiagnosticReport() {
        if (currentReportJson == null) {
            Snackbar.make(
                    binding.getRoot(),
                    "No diagnostic report is available to export.",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        String reportId =
                currentReportJson.optString(
                        "report_id",
                        String.valueOf(
                                System.currentTimeMillis()
                        )
                );

        String safeReportId =
                reportId.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );

        Intent exportIntent =
                new Intent(
                        Intent.ACTION_CREATE_DOCUMENT
                );

        exportIntent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        exportIntent.setType(
                "application/json"
        );

        exportIntent.putExtra(
                Intent.EXTRA_TITLE,
                "study_saathi_cloud_diagnostic_"
                        + safeReportId
                        + ".json"
        );

        startActivityForResult(
                exportIntent,
                REQUEST_EXPORT_DIAGNOSTIC_REPORT
        );
    }

    @NonNull
    private JSONObject createExportReport()
            throws JSONException {

        JSONObject exportReport =
                new JSONObject(
                        currentReportJson.toString()
                );

        JSONObject device =
                new JSONObject();

        device.put(
                "manufacturer",
                Build.MANUFACTURER
        );

        device.put(
                "model",
                Build.MODEL
        );

        device.put(
                "android_version",
                Build.VERSION.RELEASE
        );

        device.put(
                "sdk_int",
                Build.VERSION.SDK_INT
        );

        PackageInfo packageInfo;

        try {

            packageInfo =
                    getPackageManager()
                            .getPackageInfo(
                                    getPackageName(),
                                    0
                            );

            device.put(
                    "app_version",
                    packageInfo.versionName
            );

            device.put(
                    "version_code",
                    PackageInfoCompat.getLongVersionCode(
                            packageInfo
                    )
            );

        } catch (PackageManager.NameNotFoundException ignored) {

            device.put(
                    "app_version",
                    "Unknown"
            );
        }

        device.put(
                "generated_at",
                System.currentTimeMillis()
        );

        exportReport.put(
                "device_information",
                device
        );

        int healthScore =
                calculateHealthScore(
                        exportReport
                );

        exportReport.put(
                "health_score",
                healthScore
        );

        exportReport.put(
                "backup_readiness",
                getBackupReadinessStatus(
                        healthScore
                )
        );

        exportReport.put(
                "security_status",
                getSecurityStatus(
                        exportReport
                )
        );

        exportReport.put(
                "recommended_fixes",
                createRecommendedFixes(
                        exportReport
                )
        );

        exportReport.put(
                "human_readable_summary",
                createHumanReadableSummary(
                        exportReport
                )
        );

        exportReport.put(
                "categorized_checks",
                createCategorizedChecks(
                        exportReport
                )
        );

        boolean reportComplete =
                exportReport.optJSONArray("tests") != null
                        && exportReport.optJSONObject("summary") != null
                        && exportReport.has("overall_status")
                        && exportReport.has("health_score")
                        && exportReport.has("backup_readiness")
                        && exportReport.has("security_status");

        exportReport.put(
                "report_complete",
                reportComplete
        );

        exportReport.put(
                "report_format_version",
                1
        );

        return exportReport;
    }

    private int calculateHealthScore(
            @NonNull JSONObject reportJson
    ) {

        JSONObject summary =
                reportJson.optJSONObject(
                        "summary"
                );

        if (summary == null) {
            return 0;
        }

        int passed =
                summary.optInt(
                        "passed",
                        0
                );

        int failed =
                summary.optInt(
                        "failed",
                        0
                );

        int warnings =
                summary.optInt(
                        "warnings",
                        0
                );

        int notRun =
                summary.optInt(
                        "not_run",
                        0
                );

        int total =
                passed
                        + failed
                        + warnings
                        + notRun;

        if (total == 0) {
            return 0;
        }

        double score =
                (
                        (passed * 1.0)
                                + (warnings * 0.5)
                ) / total;

        return Math.max(
                0,
                Math.min(
                        100,
                        (int) Math.round(
                                score * 100
                        )
                )
        );
    }

    @NonNull
    private String getBackupReadinessStatus(
            int healthScore
    ) {

        if (healthScore >= 95) {
            return "Production Ready";
        }

        if (healthScore >= 80) {
            return "Ready";
        }

        if (healthScore >= 60) {
            return "Needs Attention";
        }

        if (healthScore >= 40) {
            return "High Risk";
        }

        return "Critical";
    }

    @NonNull
    private String getSecurityStatus(
            @NonNull JSONObject reportJson
    ) {

        int healthScore =
                calculateHealthScore(
                        reportJson
                );

        String overallStatus =
                reportJson.optString(
                        "overall_status",
                        "INCOMPLETE"
                );

        if ("FAILED".equalsIgnoreCase(overallStatus)) {
            return "Security Issues Detected";
        }

        if (healthScore >= 95) {
            return "Secure";
        }

        if (healthScore >= 80) {
            return "Mostly Secure";
        }

        if (healthScore >= 60) {
            return "Needs Review";
        }

        return "Security Risk";
    }

    @NonNull
    private JSONArray createRecommendedFixes(
            @NonNull JSONObject reportJson
    ) throws JSONException {

        JSONArray fixes = new JSONArray();

        JSONArray tests =
                reportJson.optJSONArray("tests");

        if (tests == null) {
            return fixes;
        }

        for (int i = 0; i < tests.length(); i++) {

            JSONObject test =
                    tests.getJSONObject(i);

            String status =
                    test.optString("status", "");

            if (!"FAILED".equalsIgnoreCase(status)
                    && !"WARNING".equalsIgnoreCase(status)) {
                continue;
            }

            JSONObject suggestion =
                    new JSONObject();

            suggestion.put(
                    "test",
                    test.optString("test_name")
            );

            suggestion.put(
                    "status",
                    status
            );

            String message =
                    test.optString(
                            "user_message",
                            ""
                    );

            suggestion.put(
                    "issue",
                    message
            );

            suggestion.put(
                    "recommended_action",
                    getSuggestedAction(
                            test.optString("test_name", "")
                    )
            );

            fixes.put(
                    suggestion
            );
        }

        return fixes;
    }

    @NonNull
    private String getSuggestedAction(
            @NonNull String testName
    ) {

        String value =
                testName.toLowerCase(Locale.US);

        if (value.contains("firebase")) {
            return "Verify Firebase authentication and internet connectivity.";
        }

        if (value.contains("backup")) {
            return "Create a fresh encrypted cloud backup and verify synchronization.";
        }

        if (value.contains("encryption")) {
            return "Verify encryption key and passphrase configuration.";
        }

        if (value.contains("restore")) {
            return "Perform a complete restore test using the latest backup.";
        }

        if (value.contains("storage")) {
            return "Check available storage space and cloud permissions.";
        }

        return "Review diagnostic details and repeat the test after resolving the issue.";
    }

    @NonNull
    private JSONObject createHumanReadableSummary(
            @NonNull JSONObject reportJson
    ) throws JSONException {

        JSONObject summary =
                reportJson.optJSONObject(
                        "summary"
                );

        int passed = 0;
        int failed = 0;
        int warnings = 0;
        int notRun = 0;

        if (summary != null) {
            passed =
                    summary.optInt(
                            "passed",
                            0
                    );

            failed =
                    summary.optInt(
                            "failed",
                            0
                    );

            warnings =
                    summary.optInt(
                            "warnings",
                            0
                    );

            notRun =
                    summary.optInt(
                            "not_run",
                            0
                    );
        }

        int healthScore =
                calculateHealthScore(
                        reportJson
                );

        JSONObject readableSummary =
                new JSONObject();

        readableSummary.put(
                "title",
                "Study Saathi Cloud Backup Diagnostic Summary"
        );

        readableSummary.put(
                "health",
                "Cloud backup health score is "
                        + healthScore
                        + " out of 100."
        );

        readableSummary.put(
                "result",
                passed
                        + " checks passed, "
                        + failed
                        + " failed, "
                        + warnings
                        + " have warnings, and "
                        + notRun
                        + " were not completed."
        );

        readableSummary.put(
                "readiness",
                "Backup readiness status: "
                        + getBackupReadinessStatus(
                        healthScore
                )
                        + "."
        );

        readableSummary.put(
                "security",
                "Security status: "
                        + getSecurityStatus(
                        reportJson
                )
                        + "."
        );

        if (failed > 0) {
            readableSummary.put(
                    "next_step",
                    "Resolve failed checks before relying on cloud backup for important data."
            );

        } else if (warnings > 0) {
            readableSummary.put(
                    "next_step",
                    "Review warning checks and repeat diagnostics after applying the recommended actions."
            );

        } else if (notRun > 0) {
            readableSummary.put(
                    "next_step",
                    "Run all remaining diagnostic checks before confirming production readiness."
            );

        } else {
            readableSummary.put(
                    "next_step",
                    "All diagnostic checks completed successfully. Continue periodic backup and restore testing."
            );
        }

        return readableSummary;
    }

    @NonNull
    private JSONObject createCategorizedChecks(
            @NonNull JSONObject reportJson
    ) throws JSONException {

        JSONObject categorizedChecks =
                new JSONObject();

        JSONArray passedChecks =
                new JSONArray();

        JSONArray warningChecks =
                new JSONArray();

        JSONArray failedChecks =
                new JSONArray();

        JSONArray notRunChecks =
                new JSONArray();

        JSONArray tests =
                reportJson.optJSONArray(
                        "tests"
                );

        if (tests != null) {

            for (int index = 0;
                 index < tests.length();
                 index++) {

                JSONObject test =
                        tests.getJSONObject(
                                index
                        );

                JSONObject checkSummary =
                        new JSONObject();

                checkSummary.put(
                        "test_name",
                        test.optString(
                                "test_name",
                                "Unknown test"
                        )
                );

                checkSummary.put(
                        "user_message",
                        test.optString(
                                "user_message",
                                ""
                        )
                );

                String status =
                        test.optString(
                                "status",
                                "NOT_RUN"
                        ).trim().toUpperCase(
                                Locale.US
                        );

                switch (status) {

                    case "PASSED":
                        passedChecks.put(
                                checkSummary
                        );
                        break;

                    case "WARNING":
                        warningChecks.put(
                                checkSummary
                        );
                        break;

                    case "FAILED":
                        failedChecks.put(
                                checkSummary
                        );
                        break;

                    case "NOT_RUN":
                    default:
                        notRunChecks.put(
                                checkSummary
                        );
                        break;
                }
            }
        }

        categorizedChecks.put(
                "passed_checks",
                passedChecks
        );

        categorizedChecks.put(
                "warning_checks",
                warningChecks
        );

        categorizedChecks.put(
                "failed_checks",
                failedChecks
        );

        categorizedChecks.put(
                "not_run_checks",
                notRunChecks
        );

        return categorizedChecks;
    }
    private void shareCurrentDiagnosticReport() {
        if (currentReportJson == null) {
            Snackbar.make(
                    binding.getRoot(),
                    "No diagnostic report is available to share.",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        try {
            Intent shareIntent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            shareIntent.setType(
                    "application/json"
            );

            shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Study Saathi Cloud Backup Diagnostic Report"
            );

            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    createExportReport().toString(
                            2
                    )
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share diagnostic report"
                    )
            );

        } catch (JSONException exception) {
            Snackbar.make(
                    binding.getRoot(),
                    createErrorMessage(
                            "Unable to prepare diagnostic report.",
                            exception
                    ),
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode
                != REQUEST_EXPORT_DIAGNOSTIC_REPORT) {

            return;
        }

        if (resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {

            return;
        }

        if (currentReportJson == null) {
            Snackbar.make(
                    binding.getRoot(),
                    "Diagnostic report is no longer available.",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        Uri destinationUri =
                data.getData();

        try (
                OutputStream outputStream =
                        getContentResolver()
                                .openOutputStream(
                                        destinationUri
                                )
        ) {
            if (outputStream == null) {
                throw new IOException(
                        "Unable to open the selected file."
                );
            }

            byte[] reportBytes =
                    createExportReport()
                            .toString(2)
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            outputStream.write(
                    reportBytes
            );

            outputStream.flush();

            Snackbar.make(
                    binding.getRoot(),
                    "Diagnostic report exported successfully.",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (IOException | JSONException exception) {
            Snackbar.make(
                    binding.getRoot(),
                    createErrorMessage(
                            "Diagnostic report export failed.",
                            exception
                    ),
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }
    @NonNull
    private String createErrorMessage(
            @NonNull String userMessage,
            @Nullable Exception exception
    ) {
        if (exception == null) {
            return userMessage;
        }

        String technicalMessage =
                exception.getLocalizedMessage();

        if (technicalMessage == null
                || technicalMessage.trim().isEmpty()) {

            technicalMessage =
                    exception.getClass()
                            .getSimpleName();
        }

        return getString(
                R.string.cloud_diagnostic_error_format,
                userMessage,
                technicalMessage.trim()
        );
    }
}