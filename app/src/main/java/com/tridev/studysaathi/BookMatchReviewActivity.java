package com.tridev.studysaathi;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.data.content.model.BookMatchReviewData;
import com.tridev.studysaathi.databinding.ActivityBookMatchReviewBinding;

import java.util.List;
import java.util.Locale;

public final class BookMatchReviewActivity
        extends AppCompatActivity {

    public static final String EXTRA_BOOK_MATCH_REVIEW_DATA =
            "extra_book_match_review_data";

    private ActivityBookMatchReviewBinding binding;

    @Nullable
    private BookMatchReviewData reviewData;

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            @NonNull BookMatchReviewData reviewData
    ) {
        Intent intent =
                new Intent(
                        context,
                        BookMatchReviewActivity.class
                );

        intent.putExtra(
                EXTRA_BOOK_MATCH_REVIEW_DATA,
                reviewData
        );

        return intent;
    }

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        binding =
                ActivityBookMatchReviewBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        setupToolbar();
        setupClickListeners();

        reviewData =
                readReviewDataFromIntent();

        if (reviewData == null) {
            showMissingReviewDataError();

            return;
        }

        displayReviewData(
                reviewData
        );
    }

    private void setupToolbar() {
        binding.toolbarBookMatchReview
                .setNavigationOnClickListener(
                        view ->
                                getOnBackPressedDispatcher()
                                        .onBackPressed()
                );
    }

    private void setupClickListeners() {
        binding.buttonPreviewOnlineBook
                .setOnClickListener(
                        view ->
                                openOnlineBookPreview()
                );

        binding.buttonConfirmAndAddBook
                .setOnClickListener(
                        view ->
                                confirmAndAddBook()
                );

        binding.buttonEditBookInformation
                .setOnClickListener(
                        view ->
                                showBookInformationEditorMessage()
                );

        binding.buttonScanAnotherCover
                .setOnClickListener(
                        view ->
                                finish()
                );
    }

    @Nullable
    private BookMatchReviewData
    readReviewDataFromIntent() {
        Intent intent =
                getIntent();

        if (intent == null) {
            return null;
        }

        try {
            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                return intent.getSerializableExtra(
                        EXTRA_BOOK_MATCH_REVIEW_DATA,
                        BookMatchReviewData.class
                );
            }

            Object serializableData =
                    intent.getSerializableExtra(
                            EXTRA_BOOK_MATCH_REVIEW_DATA
                    );

            if (serializableData
                    instanceof BookMatchReviewData) {

                return (BookMatchReviewData)
                        serializableData;
            }

        } catch (RuntimeException ignored) {
            /*
             * Invalid या incomplete Intent data के
             * कारण Activity crash नहीं होनी चाहिए।
             */
        }

        return null;
    }

    private void displayReviewData(
            @NonNull BookMatchReviewData data
    ) {
        displayReviewStatus(
                data
        );

        displayBookCover(
                data
        );

        displayDetectedInformation(
                data
        );

        displayOnlineMatch(
                data
        );

        displayReviewWarnings(
                data.getReviewWarnings()
        );

        binding.buttonConfirmAndAddBook
                .setEnabled(
                        data.canConfirmBook()
                );
    }

    private void displayReviewStatus(
            @NonNull BookMatchReviewData data
    ) {
        if (!data.isBestMatchAvailable()) {
            binding.textReviewStatusIcon.setText(
                    "!"
            );

            binding.textReviewStatusTitle.setText(
                    "सही online match नहीं मिला"
            );

            binding.textReviewStatusDescription.setText(
                    "Detected information जाँचें या book details manually सुधारें।"
            );

            return;
        }

        if (data.isHighConfidenceMatch()) {
            binding.textReviewStatusIcon.setText(
                    "✓"
            );

            binding.textReviewStatusTitle.setText(
                    "Book match मिल गया"
            );

            binding.textReviewStatusDescription.setText(
                    "Details जाँचकर parent confirmation दें।"
            );

            return;
        }

        binding.textReviewStatusIcon.setText(
                "?"
        );

        binding.textReviewStatusTitle.setText(
                "Parent review आवश्यक है"
        );

        binding.textReviewStatusDescription.setText(
                "Online result मिला है, लेकिन इसकी जानकारी ध्यान से जाँचें।"
        );
    }

    private void displayBookCover(
            @NonNull BookMatchReviewData data
    ) {
        binding.textReviewedBookTitle.setText(
                data.getPreferredBookTitle()
        );

        binding.textReviewedBookSubtitle.setText(
                createBookSubtitle(
                        data
                )
        );

        if (data.isBestMatchAvailable()) {
            binding.textMatchConfidenceChip.setText(
                    String.format(
                            Locale.getDefault(),
                            "Match: %.1f%%",
                            data.getOverallMatchScore()
                    )
            );

        } else {
            binding.textMatchConfidenceChip.setText(
                    "Match: Not available"
            );
        }

        if (!data.hasSelectedImageUri()) {
            showCoverPlaceholder();

            return;
        }

        try {
            Uri selectedImageUri =
                    Uri.parse(
                            data.getSelectedImageUri()
                    );

            binding.imageReviewedBookCover
                    .setImageURI(
                            null
                    );

            binding.imageReviewedBookCover
                    .setImageURI(
                            selectedImageUri
                    );

            binding.imageReviewedBookCover
                    .setVisibility(
                            View.VISIBLE
                    );

            binding.textReviewedCoverPlaceholder
                    .setVisibility(
                            View.GONE
                    );

        } catch (RuntimeException exception) {
            showCoverPlaceholder();
        }
    }

    private void showCoverPlaceholder() {
        binding.imageReviewedBookCover
                .setImageDrawable(
                        null
                );

        binding.imageReviewedBookCover
                .setVisibility(
                        View.GONE
                );

        binding.textReviewedCoverPlaceholder
                .setVisibility(
                        View.VISIBLE
                );
    }

    @NonNull
    private String createBookSubtitle(
            @NonNull BookMatchReviewData data
    ) {
        StringBuilder subtitleBuilder =
                new StringBuilder();

        appendSubtitlePart(
                subtitleBuilder,
                data.getPreferredSubjectName()
        );

        appendSubtitlePart(
                subtitleBuilder,
                data.getPreferredClassName()
        );

        appendSubtitlePart(
                subtitleBuilder,
                data.getPreferredPublisherName()
        );

        if (subtitleBuilder.length() == 0) {
            return "Book information requires review";
        }

        return subtitleBuilder.toString();
    }

    private void appendSubtitlePart(
            @NonNull StringBuilder builder,
            @Nullable String value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(
                    "  •  "
            );
        }

        builder.append(
                safeValue
        );
    }

    private void displayDetectedInformation(
            @NonNull BookMatchReviewData data
    ) {
        binding.textDetectedBookName.setText(
                createLabelValue(
                        "Book Name",
                        data.getDetectedBookTitle()
                )
        );

        binding.textDetectedSubject.setText(
                createLabelValue(
                        "Subject",
                        data.getDetectedSubjectName()
                )
        );

        binding.textDetectedClass.setText(
                createLabelValue(
                        "Class",
                        data.getDetectedClassName()
                )
        );

        binding.textDetectedBoard.setText(
                createLabelValue(
                        "Board",
                        data.getDetectedEducationBoard()
                )
        );

        binding.textDetectedPublisher.setText(
                createLabelValue(
                        "Publisher",
                        data.getDetectedPublisherName()
                )
        );

        binding.textDetectedIsbn.setText(
                createLabelValue(
                        "ISBN",
                        data.getDetectedIsbn()
                )
        );
    }

    private void displayOnlineMatch(
            @NonNull BookMatchReviewData data
    ) {
        if (!data.isBestMatchAvailable()) {
            binding.textOnlineBookTitle.setText(
                    "No reliable online match available"
            );

            binding.textOnlineBookAuthor.setText(
                    "Author: Not available"
            );

            binding.textOnlineBookPublisher.setText(
                    "Publisher: Not available"
            );

            binding.textOnlineBookIsbn.setText(
                    "ISBN: Not available"
            );

            binding.textOnlineBookSource.setText(
                    "Source: Not available"
            );

            binding.textOnlineBookAccess.setText(
                    "Access: Not available"
            );

            binding.buttonPreviewOnlineBook
                    .setVisibility(
                            View.GONE
                    );

            return;
        }

        binding.textOnlineBookTitle.setText(
                valueOrFallback(
                        data.getOnlineBookTitle(),
                        "Online book title not available"
                )
        );

        binding.textOnlineBookAuthor.setText(
                createLabelValue(
                        "Author",
                        data.getOnlineBookAuthors()
                )
        );

        binding.textOnlineBookPublisher.setText(
                createLabelValue(
                        "Publisher",
                        data.getOnlinePublisherName()
                )
        );

        binding.textOnlineBookIsbn.setText(
                createLabelValue(
                        "ISBN",
                        data.getOnlineIsbn()
                )
        );

        binding.textOnlineBookSource.setText(
                createLabelValue(
                        "Source",
                        data.getOnlineProviderName()
                )
        );

        binding.textOnlineBookAccess.setText(
                createOnlineAccessText(
                        data
                )
        );

        binding.buttonPreviewOnlineBook
                .setVisibility(
                        data.hasOnlinePreviewUrl()
                                || data.hasOnlineInformationUrl()
                                || data.hasOfficialSourceUrl()
                                ? View.VISIBLE
                                : View.GONE
                );
    }

    @NonNull
    private String createOnlineAccessText(
            @NonNull BookMatchReviewData data
    ) {
        String accessType =
                valueOrFallback(
                        data.getOnlineAccessType(),
                        "Metadata only"
                );

        if (data.hasAuthorizedDownloadUrl()) {
            return "Access: "
                    + accessType
                    + " • Authorized download available";
        }

        if (data.hasOnlinePreviewUrl()) {
            return "Access: "
                    + accessType
                    + " • Preview available";
        }

        return "Access: "
                + accessType;
    }

    private void displayReviewWarnings(
            @NonNull List<String> warnings
    ) {
        if (warnings.isEmpty()) {
            binding.cardBookReviewWarnings
                    .setVisibility(
                            View.GONE
                    );

            binding.textBookReviewWarnings.setText(
                    ""
            );

            return;
        }

        StringBuilder warningBuilder =
                new StringBuilder();

        for (String warning : warnings) {
            String safeWarning =
                    safeText(
                            warning
                    );

            if (safeWarning.isEmpty()) {
                continue;
            }

            if (warningBuilder.length() > 0) {
                warningBuilder.append(
                        '\n'
                );
            }

            warningBuilder.append(
                    "• "
            );

            warningBuilder.append(
                    safeWarning
            );
        }

        if (warningBuilder.length() == 0) {
            binding.cardBookReviewWarnings
                    .setVisibility(
                            View.GONE
                    );

            return;
        }

        binding.textBookReviewWarnings.setText(
                warningBuilder.toString()
        );

        binding.cardBookReviewWarnings
                .setVisibility(
                        View.VISIBLE
                );
    }

    private void openOnlineBookPreview() {
        BookMatchReviewData data =
                reviewData;

        if (data == null) {
            return;
        }

        String previewUrl =
                findPreferredOnlineUrl(
                        data
                );

        if (previewUrl.isEmpty()) {
            Snackbar.make(
                    binding.getRoot(),
                    "इस book का online preview उपलब्ध नहीं है।",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        try {
            Intent browserIntent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    previewUrl
                            )
                    );

            startActivity(
                    browserIntent
            );

        } catch (RuntimeException exception) {
            Snackbar.make(
                    binding.getRoot(),
                    "Online book link खोला नहीं जा सका।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @NonNull
    private String findPreferredOnlineUrl(
            @NonNull BookMatchReviewData data
    ) {
        if (data.hasOnlinePreviewUrl()) {
            return data.getOnlinePreviewUrl();
        }

        if (data.hasOfficialSourceUrl()) {
            return data.getOnlineOfficialSourceUrl();
        }

        if (data.hasOnlineInformationUrl()) {
            return data.getOnlineInformationUrl();
        }

        return "";
    }

    private void confirmAndAddBook() {
        BookMatchReviewData data =
                reviewData;

        if (data == null
                || !data.canConfirmBook()) {

            Snackbar.make(
                    binding.getRoot(),
                    "इस result को अभी confirm नहीं किया जा सकता।",
                    Snackbar.LENGTH_LONG
            ).show();

            return;
        }

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "Book जोड़ने की पुष्टि"
                )
                .setMessage(
                        "क्या आप “"
                                + data.getPreferredBookTitle()
                                + "” को student curriculum में जोड़ना चाहते हैं?"
                )
                .setNegativeButton(
                        "अभी नहीं",
                        null
                )
                .setPositiveButton(
                        "Confirm",
                        (dialog, which) ->
                                showDatabaseConnectionPendingMessage()
                )
                .show();
    }

    private void showDatabaseConnectionPendingMessage() {
        Snackbar.make(
                binding.getRoot(),
                "Review complete है। Database save अगले step में connect होगा।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showBookInformationEditorMessage() {
        Snackbar.make(
                binding.getRoot(),
                "Book Information Editor अगले step में जोड़ा जाएगा।",
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showMissingReviewDataError() {
        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        "Review data उपलब्ध नहीं है"
                )
                .setMessage(
                        "Book scan result नहीं मिला। कृपया cover दोबारा scan करें।"
                )
                .setCancelable(
                        false
                )
                .setPositiveButton(
                        "वापस जाएँ",
                        (dialog, which) ->
                                finish()
                )
                .show();
    }

    @NonNull
    private String createLabelValue(
            @NonNull String label,
            @Nullable String value
    ) {
        return label
                + ": "
                + valueOrFallback(
                value,
                "Not detected"
        );
    }

    @NonNull
    private String valueOrFallback(
            @Nullable String value,
            @NonNull String fallback
    ) {
        String safeValue =
                safeText(
                        value
                );

        return safeValue.isEmpty()
                ? fallback
                : safeValue;
    }

    @NonNull
    private String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    @Override
    protected void onDestroy() {
        binding =
                null;

        super.onDestroy();
    }
}