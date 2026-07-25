package com.tridev.studysaathi.data.content.scanner;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tridev.studysaathi.data.content.model.BookCoverScanResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BookCoverScanner {

    private static final Pattern ISBN_PATTERN =
            Pattern.compile(
                    "(?i)(?:ISBN(?:-1[03])?\\s*[:\\-]?\\s*)?"
                            + "((?:97[89][\\s\\-]?)?"
                            + "[0-9][0-9Xx\\s\\-]{8,20})"
            );

    private static final int MINIMUM_OCR_TEXT_LENGTH =
            3;

    private final Context applicationContext;

    private final Handler mainThreadHandler;

    public BookCoverScanner(
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
     * Scans a book-cover image using:
     *
     * 1. Latin text recognition
     * 2. Devanagari text recognition
     * 3. Book-compatible barcode recognition
     *
     * The callback is always returned on the main
     * application thread.
     */
    public void scanBookCover(
            @NonNull Uri imageUri,
            @Nullable String privateImagePath,
            @NonNull BookCoverScanResult.ScanSource scanSource,
            @NonNull ScanCallback callback
    ) {
        long scanStartedAt =
                System.currentTimeMillis();

        final InputImage inputImage;

        try {
            inputImage =
                    InputImage.fromFilePath(
                            applicationContext,
                            imageUri
                    );

        } catch (IOException exception) {
            dispatchError(
                    callback,
                    new BookCoverScanException(
                            "Unable to open the selected "
                                    + "book-cover image.",
                            exception
                    )
            );

            return;
        }

        TextRecognizer latinRecognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions
                                .DEFAULT_OPTIONS
                );

        TextRecognizer devanagariRecognizer =
                TextRecognition.getClient(
                        new DevanagariTextRecognizerOptions
                                .Builder()
                                .build()
                );

        BarcodeScannerOptions barcodeOptions =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E,
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_QR_CODE
                        )
                        .build();

        BarcodeScanner barcodeScanner =
                BarcodeScanning.getClient(
                        barcodeOptions
                );

        Task<Text> latinTextTask =
                latinRecognizer.process(
                        inputImage
                );

        Task<Text> devanagariTextTask =
                devanagariRecognizer.process(
                        inputImage
                );

        Task<List<Barcode>> barcodeTask =
                barcodeScanner.process(
                        inputImage
                );

        Tasks.whenAllComplete(
                        latinTextTask,
                        devanagariTextTask,
                        barcodeTask
                )
                .addOnCompleteListener(
                        ignored -> {
                            try {
                                ScanProcessingData
                                        processingData =
                                        collectProcessingData(
                                                latinTextTask,
                                                devanagariTextTask,
                                                barcodeTask
                                        );

                                BookCoverScanResult result =
                                        createScanResult(
                                                privateImagePath,
                                                scanSource,
                                                scanStartedAt,
                                                processingData
                                        );

                                dispatchSuccess(
                                        callback,
                                        result
                                );

                            } catch (Exception exception) {
                                dispatchError(
                                        callback,
                                        exception
                                );

                            } finally {
                                latinRecognizer.close();
                                devanagariRecognizer.close();
                                barcodeScanner.close();
                            }
                        }
                );
    }

    @NonNull
    private ScanProcessingData collectProcessingData(
            @NonNull Task<Text> latinTextTask,
            @NonNull Task<Text> devanagariTextTask,
            @NonNull Task<List<Barcode>> barcodeTask
    ) throws BookCoverScanException {

        ScanProcessingData processingData =
                new ScanProcessingData();

        if (latinTextTask.isSuccessful()
                && latinTextTask.getResult() != null) {

            processingData.latinRecognitionSucceeded =
                    true;

            collectTextResult(
                    latinTextTask.getResult(),
                    processingData
            );

        } else {
            processingData.warnings.add(
                    "English text recognition was "
                            + "not completed successfully."
            );
        }

        if (devanagariTextTask.isSuccessful()
                && devanagariTextTask.getResult() != null) {

            processingData
                    .devanagariRecognitionSucceeded =
                    true;

            collectTextResult(
                    devanagariTextTask.getResult(),
                    processingData
            );

        } else {
            processingData.warnings.add(
                    "Hindi/Devanagari text recognition "
                            + "was not completed successfully."
            );
        }

        if (barcodeTask.isSuccessful()
                && barcodeTask.getResult() != null) {

            processingData.barcodeScanSucceeded =
                    true;

            collectBarcodeResults(
                    barcodeTask.getResult(),
                    processingData
            );

        } else {
            processingData.warnings.add(
                    "Barcode recognition was not "
                            + "completed successfully."
            );
        }

        boolean hasText =
                !processingData.uniqueTextLines
                        .isEmpty();

        boolean hasBarcode =
                !processingData.detectedBarcodes
                        .isEmpty();

        if (!hasText
                && !hasBarcode) {

            throw new BookCoverScanException(
                    "No readable book information was "
                            + "found on the selected image."
            );
        }

        return processingData;
    }

    private void collectTextResult(
            @NonNull Text textResult,
            @NonNull ScanProcessingData processingData
    ) {
        List<Text.TextBlock> textBlocks =
                textResult.getTextBlocks();

        processingData.detectedTextBlockCount +=
                textBlocks.size();

        for (Text.TextBlock textBlock :
                textBlocks) {

            for (Text.Line textLine :
                    textBlock.getLines()) {

                addUniqueTextLine(
                        processingData.uniqueTextLines,
                        textLine.getText()
                );
            }
        }

        if (textBlocks.isEmpty()) {
            addTextLinesFromFullText(
                    processingData.uniqueTextLines,
                    textResult.getText()
            );
        }
    }

    private void addTextLinesFromFullText(
            @NonNull Map<String, String> uniqueTextLines,
            @Nullable String fullText
    ) {
        if (fullText == null
                || fullText.trim().isEmpty()) {

            return;
        }

        String[] lines =
                fullText.split(
                        "\\r?\\n"
                );

        for (String line : lines) {
            addUniqueTextLine(
                    uniqueTextLines,
                    line
            );
        }
    }

    private void addUniqueTextLine(
            @NonNull Map<String, String> uniqueTextLines,
            @Nullable String textLine
    ) {
        if (textLine == null) {
            return;
        }

        String normalizedText =
                normalizeWhitespace(
                        textLine
                );

        if (normalizedText.length()
                < MINIMUM_OCR_TEXT_LENGTH) {

            return;
        }

        String comparisonKey =
                normalizedText
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!uniqueTextLines.containsKey(
                comparisonKey
        )) {
            uniqueTextLines.put(
                    comparisonKey,
                    normalizedText
            );
        }
    }

    private void collectBarcodeResults(
            @NonNull List<Barcode> barcodes,
            @NonNull ScanProcessingData processingData
    ) {
        for (Barcode barcode : barcodes) {
            String rawValue =
                    safeText(
                            barcode.getRawValue()
                    );

            if (rawValue.isEmpty()) {
                continue;
            }

            String displayValue =
                    safeText(
                            barcode.getDisplayValue()
                    );

            String barcodeFormat =
                    getBarcodeFormatName(
                            barcode.getFormat()
                    );

            String valueType =
                    getBarcodeValueTypeName(
                            barcode.getValueType()
                    );

            BookCoverScanResult.DetectedBarcode
                    detectedBarcode =
                    new BookCoverScanResult
                            .DetectedBarcode(
                            rawValue,
                            displayValue,
                            barcodeFormat,
                            valueType,
                            95f
                    );

            processingData.detectedBarcodes.add(
                    detectedBarcode
            );

            if (processingData.primaryBarcodeValue
                    .isEmpty()) {

                processingData.primaryBarcodeValue =
                        rawValue;

                processingData.primaryBarcodeFormat =
                        barcodeFormat;
            }

            String possibleIsbn =
                    extractValidIsbn(
                            rawValue
                    );

            if (!possibleIsbn.isEmpty()) {
                assignDetectedIsbn(
                        possibleIsbn,
                        true,
                        processingData
                );
            }
        }
    }

    @NonNull
    private BookCoverScanResult createScanResult(
            @Nullable String privateImagePath,
            @NonNull BookCoverScanResult.ScanSource scanSource,
            long scanStartedAt,
            @NonNull ScanProcessingData processingData
    ) {
        List<String> textLines =
                new ArrayList<>(
                        processingData.uniqueTextLines
                                .values()
                );

        String fullDetectedText =
                joinTextLines(
                        textLines
                );

        if (processingData.detectedIsbn10.isEmpty()
                && processingData.detectedIsbn13
                .isEmpty()) {

            String isbnFromText =
                    extractValidIsbn(
                            fullDetectedText
                    );

            if (!isbnFromText.isEmpty()) {
                assignDetectedIsbn(
                        isbnFromText,
                        false,
                        processingData
                );
            }
        }

        boolean hasText =
                !fullDetectedText.isEmpty();

        boolean hasBarcode =
                !processingData.detectedBarcodes
                        .isEmpty();

        BookCoverScanResult.ScanStatus scanStatus;

        if (hasText
                && processingData.latinRecognitionSucceeded
                && processingData
                .devanagariRecognitionSucceeded
                && processingData.barcodeScanSucceeded) {

            scanStatus =
                    BookCoverScanResult.ScanStatus.SUCCESS;

        } else {
            scanStatus =
                    BookCoverScanResult
                            .ScanStatus.PARTIAL_SUCCESS;
        }

        float overallConfidence =
                calculateOverallConfidence(
                        hasText,
                        hasBarcode,
                        processingData
                );

        BookCoverScanResult.Builder resultBuilder =
                BookCoverScanResult.builder(
                                createScanId()
                        )
                        .setLocalImagePath(
                                privateImagePath
                        )
                        .setScanSource(
                                scanSource
                        )
                        .setDetectedFullText(
                                fullDetectedText
                        )
                        .setDetectedIsbn10(
                                processingData.detectedIsbn10
                        )
                        .setDetectedIsbn13(
                                processingData.detectedIsbn13
                        )
                        .setDetectedBarcodeValue(
                                processingData.primaryBarcodeValue
                        )
                        .setDetectedBarcodeFormat(
                                processingData.primaryBarcodeFormat
                        )
                        .setDetectedTextBlockCount(
                                processingData
                                        .detectedTextBlockCount
                        )
                        .setOverallConfidence(
                                overallConfidence
                        )
                        .setIsbnConfidence(
                                processingData.isbnConfidence
                        )
                        .setScanStatus(
                                scanStatus
                        )
                        .setScannedAt(
                                System.currentTimeMillis()
                        )
                        .setProcessingDurationMilliseconds(
                                System.currentTimeMillis()
                                        - scanStartedAt
                        );

        for (String textLine : textLines) {
            resultBuilder.addDetectedTextLine(
                    textLine
            );
        }

        for (String warning :
                processingData.warnings) {

            resultBuilder.addWarning(
                    warning
            );
        }

        if (!hasText) {
            resultBuilder.addWarning(
                    "No readable title or cover text "
                            + "was detected."
            );
        }

        if (!hasBarcode) {
            resultBuilder.addWarning(
                    "No barcode was detected. The app "
                            + "can still search using cover text."
            );
        }

        if (processingData.detectedIsbn10.isEmpty()
                && processingData.detectedIsbn13
                .isEmpty()) {

            resultBuilder.addWarning(
                    "No valid ISBN was found. Manual "
                            + "book confirmation may be required."
            );
        }

        for (BookCoverScanResult.DetectedBarcode
                detectedBarcode :
                processingData.detectedBarcodes) {

            resultBuilder.addDetectedBarcode(
                    detectedBarcode
            );
        }

        return resultBuilder.build();
    }

    private float calculateOverallConfidence(
            boolean hasText,
            boolean hasBarcode,
            @NonNull ScanProcessingData processingData
    ) {
        float confidence =
                0f;

        if (hasText) {
            confidence +=
                    45f;
        }

        if (hasBarcode) {
            confidence +=
                    25f;
        }

        if (!processingData.detectedIsbn10.isEmpty()
                || !processingData.detectedIsbn13
                .isEmpty()) {

            confidence +=
                    20f;
        }

        if (processingData.latinRecognitionSucceeded
                || processingData
                .devanagariRecognitionSucceeded) {

            confidence +=
                    5f;
        }

        if (processingData.barcodeScanSucceeded) {
            confidence +=
                    5f;
        }

        return Math.max(
                0f,
                Math.min(
                        100f,
                        confidence
                )
        );
    }

    private void assignDetectedIsbn(
            @NonNull String isbn,
            boolean detectedFromBarcode,
            @NonNull ScanProcessingData processingData
    ) {
        if (isbn.length() == 13
                && processingData.detectedIsbn13
                .isEmpty()) {

            processingData.detectedIsbn13 =
                    isbn;

        } else if (isbn.length() == 10
                && processingData.detectedIsbn10
                .isEmpty()) {

            processingData.detectedIsbn10 =
                    isbn;
        }

        processingData.isbnConfidence =
                detectedFromBarcode
                        ? 98f
                        : 82f;
    }

    @NonNull
    private String extractValidIsbn(
            @Nullable String sourceText
    ) {
        if (sourceText == null
                || sourceText.trim().isEmpty()) {

            return "";
        }

        Matcher matcher =
                ISBN_PATTERN.matcher(
                        sourceText
                );

        while (matcher.find()) {
            String possibleIsbn =
                    matcher.group(1);

            if (possibleIsbn == null) {
                continue;
            }

            String normalizedIsbn =
                    possibleIsbn.replaceAll(
                                    "[^0-9Xx]",
                                    ""
                            )
                            .toUpperCase(
                                    Locale.ROOT
                            );

            if (normalizedIsbn.length() == 13
                    && isValidIsbn13(
                    normalizedIsbn
            )) {
                return normalizedIsbn;
            }

            if (normalizedIsbn.length() == 10
                    && isValidIsbn10(
                    normalizedIsbn
            )) {
                return normalizedIsbn;
            }
        }

        String compactText =
                sourceText.replaceAll(
                                "[^0-9Xx]",
                                ""
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (compactText.length() == 13
                && isValidIsbn13(
                compactText
        )) {
            return compactText;
        }

        if (compactText.length() == 10
                && isValidIsbn10(
                compactText
        )) {
            return compactText;
        }

        return "";
    }

    private boolean isValidIsbn13(
            @NonNull String isbn
    ) {
        if (isbn.length() != 13
                || !isbn.matches("\\d{13}")) {

            return false;
        }

        int checksumTotal =
                0;

        for (int index = 0;
             index < 12;
             index++) {

            int digit =
                    isbn.charAt(index)
                            - '0';

            checksumTotal +=
                    index % 2 == 0
                            ? digit
                            : digit * 3;
        }

        int expectedCheckDigit =
                (10 - checksumTotal % 10) % 10;

        int actualCheckDigit =
                isbn.charAt(12)
                        - '0';

        return expectedCheckDigit
                == actualCheckDigit;
    }

    private boolean isValidIsbn10(
            @NonNull String isbn
    ) {
        if (isbn.length() != 10) {
            return false;
        }

        int checksumTotal =
                0;

        for (int index = 0;
             index < 10;
             index++) {

            char character =
                    isbn.charAt(index);

            int digit;

            if (index == 9
                    && character == 'X') {

                digit =
                        10;

            } else if (Character.isDigit(
                    character
            )) {
                digit =
                        character - '0';

            } else {
                return false;
            }

            checksumTotal +=
                    (10 - index) * digit;
        }

        return checksumTotal % 11
                == 0;
    }

    @NonNull
    private String joinTextLines(
            @NonNull List<String> textLines
    ) {
        StringBuilder textBuilder =
                new StringBuilder();

        for (String textLine : textLines) {
            if (textBuilder.length() > 0) {
                textBuilder.append(
                        '\n'
                );
            }

            textBuilder.append(
                    textLine
            );
        }

        return textBuilder.toString()
                .trim();
    }

    @NonNull
    private String normalizeWhitespace(
            @NonNull String value
    ) {
        return value.trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    private String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    @NonNull
    private String createScanId() {
        return "scan_"
                + System.currentTimeMillis()
                + "_"
                + UUID.randomUUID()
                .toString()
                .replace(
                        "-",
                        ""
                );
    }

    @NonNull
    private String getBarcodeFormatName(
            int barcodeFormat
    ) {
        switch (barcodeFormat) {
            case Barcode.FORMAT_EAN_13:
                return "EAN_13";

            case Barcode.FORMAT_EAN_8:
                return "EAN_8";

            case Barcode.FORMAT_UPC_A:
                return "UPC_A";

            case Barcode.FORMAT_UPC_E:
                return "UPC_E";

            case Barcode.FORMAT_CODE_128:
                return "CODE_128";

            case Barcode.FORMAT_QR_CODE:
                return "QR_CODE";

            default:
                return "UNKNOWN";
        }
    }

    @NonNull
    private String getBarcodeValueTypeName(
            int valueType
    ) {
        switch (valueType) {
            case Barcode.TYPE_ISBN:
                return "ISBN";

            case Barcode.TYPE_PRODUCT:
                return "PRODUCT";

            case Barcode.TYPE_TEXT:
                return "TEXT";

            case Barcode.TYPE_URL:
                return "URL";

            default:
                return "UNKNOWN";
        }
    }

    private void dispatchSuccess(
            @NonNull ScanCallback callback,
            @NonNull BookCoverScanResult result
    ) {
        mainThreadHandler.post(() ->
                callback.onScanCompleted(
                        result
                )
        );
    }

    private void dispatchError(
            @NonNull ScanCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(() ->
                callback.onScanFailed(
                        exception
                )
        );
    }

    public interface ScanCallback {

        void onScanCompleted(
                @NonNull BookCoverScanResult result
        );

        void onScanFailed(
                @NonNull Exception exception
        );
    }

    public static final class
    BookCoverScanException
            extends Exception {

        public BookCoverScanException(
                @NonNull String message
        ) {
            super(
                    message
            );
        }

        public BookCoverScanException(
                @NonNull String message,
                @NonNull Throwable cause
        ) {
            super(
                    message,
                    cause
            );
        }
    }

    private static final class
    ScanProcessingData {

        private boolean latinRecognitionSucceeded;

        private boolean devanagariRecognitionSucceeded;

        private boolean barcodeScanSucceeded;

        private int detectedTextBlockCount;

        private float isbnConfidence;

        @NonNull
        private String detectedIsbn10 =
                "";

        @NonNull
        private String detectedIsbn13 =
                "";

        @NonNull
        private String primaryBarcodeValue =
                "";

        @NonNull
        private String primaryBarcodeFormat =
                "";

        @NonNull
        private final Map<String, String>
                uniqueTextLines =
                new LinkedHashMap<>();

        @NonNull
        private final List<String> warnings =
                new ArrayList<>();

        @NonNull
        private final List<BookCoverScanResult
                .DetectedBarcode> detectedBarcodes =
                new ArrayList<>();
    }
}