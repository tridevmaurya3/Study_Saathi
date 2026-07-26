package com.tridev.studysaathi.data.content.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin
        .TextRecognizerOptions;
import com.tridev.studysaathi.data.content.model
        .BookContentsScanResult;
import com.tridev.studysaathi.data.content.parser
        .BookContentsTextParser;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BookContentsOcrScanner {

    @NonNull
    private final Handler mainThreadHandler;

    @NonNull
    private final AtomicBoolean scanInProgress =
            new AtomicBoolean(
                    false
            );

    public BookContentsOcrScanner() {
        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    /**
     * Gallery, FileProvider या camera URI से Contents page scan करता है।
     */
    public void scanFromUri(
            @NonNull Context context,
            long bookRowId,
            @NonNull Uri imageUri,
            @Nullable String sourceImagePath,
            @NonNull ScanCallback callback
    ) {
        if (bookRowId <= 0L) {
            postResult(
                    callback,
                    BookContentsScanResult.failure(
                            bookRowId,
                            sourceImagePath,
                            "A valid school book is required."
                    )
            );

            return;
        }

        if (!beginScan()) {
            postResult(
                    callback,
                    BookContentsScanResult.failure(
                            bookRowId,
                            sourceImagePath,
                            "A contents-page scan is already running."
                    )
            );

            return;
        }

        try {
            InputImage inputImage =
                    InputImage.fromFilePath(
                            context.getApplicationContext(),
                            imageUri
                    );

            recognizeText(
                    inputImage,
                    bookRowId,
                    sourceImagePath,
                    callback
            );

        } catch (IOException
                 | RuntimeException exception) {

            finishWithFailure(
                    callback,
                    bookRowId,
                    sourceImagePath,
                    getErrorMessage(
                            exception,
                            "The selected contents-page image "
                                    + "could not be opened."
                    )
            );
        }
    }

    /**
     * Camera या पहले से decoded Bitmap से Contents page scan करता है।
     *
     * rotationDegrees सामान्यतः 0, 90, 180 या 270 होना चाहिए।
     */
    public void scanFromBitmap(
            long bookRowId,
            @NonNull Bitmap bitmap,
            int rotationDegrees,
            @Nullable String sourceImagePath,
            @NonNull ScanCallback callback
    ) {
        if (bookRowId <= 0L) {
            postResult(
                    callback,
                    BookContentsScanResult.failure(
                            bookRowId,
                            sourceImagePath,
                            "A valid school book is required."
                    )
            );

            return;
        }

        if (bitmap.isRecycled()) {
            postResult(
                    callback,
                    BookContentsScanResult.failure(
                            bookRowId,
                            sourceImagePath,
                            "The contents-page image is no longer available."
                    )
            );

            return;
        }

        if (!isValidRotation(
                rotationDegrees
        )) {
            postResult(
                    callback,
                    BookContentsScanResult.failure(
                            bookRowId,
                            sourceImagePath,
                            "The image rotation is invalid."
                    )
            );

            return;
        }

        if (!beginScan()) {
            postResult(
                    callback,
                    BookContentsScanResult.failure(
                            bookRowId,
                            sourceImagePath,
                            "A contents-page scan is already running."
                    )
            );

            return;
        }

        try {
            InputImage inputImage =
                    InputImage.fromBitmap(
                            bitmap,
                            rotationDegrees
                    );

            recognizeText(
                    inputImage,
                    bookRowId,
                    sourceImagePath,
                    callback
            );

        } catch (RuntimeException exception) {
            finishWithFailure(
                    callback,
                    bookRowId,
                    sourceImagePath,
                    getErrorMessage(
                            exception,
                            "The contents-page image "
                                    + "could not be processed."
                    )
            );
        }
    }

    private void recognizeText(
            @NonNull InputImage inputImage,
            long bookRowId,
            @Nullable String sourceImagePath,
            @NonNull ScanCallback callback
    ) {
        TextRecognizer textRecognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions
                                .DEFAULT_OPTIONS
                );

        textRecognizer
                .process(
                        inputImage
                )
                .addOnSuccessListener(text -> {
                    String detectedText =
                            text == null
                                    ? ""
                                    : text.getText();

                    BookContentsScanResult result =
                            BookContentsTextParser.parse(
                                    bookRowId,
                                    sourceImagePath,
                                    detectedText
                            );

                    completeScan(
                            callback,
                            result
                    );
                })
                .addOnFailureListener(exception ->
                        finishWithFailure(
                                callback,
                                bookRowId,
                                sourceImagePath,
                                getErrorMessage(
                                        exception,
                                        "Text could not be read from "
                                                + "the contents page."
                                )
                        )
                )
                .addOnCompleteListener(task ->
                        textRecognizer.close()
                );
    }

    private boolean beginScan() {
        return scanInProgress.compareAndSet(
                false,
                true
        );
    }

    private void completeScan(
            @NonNull ScanCallback callback,
            @NonNull BookContentsScanResult result
    ) {
        scanInProgress.set(
                false
        );

        postResult(
                callback,
                result
        );
    }

    private void finishWithFailure(
            @NonNull ScanCallback callback,
            long bookRowId,
            @Nullable String sourceImagePath,
            @NonNull String errorMessage
    ) {
        scanInProgress.set(
                false
        );

        postResult(
                callback,
                BookContentsScanResult.failure(
                        bookRowId,
                        sourceImagePath,
                        errorMessage
                )
        );
    }

    private void postResult(
            @NonNull ScanCallback callback,
            @NonNull BookContentsScanResult result
    ) {
        mainThreadHandler.post(() ->
                callback.onScanCompleted(
                        result
                )
        );
    }

    public boolean isScanInProgress() {
        return scanInProgress.get();
    }

    private boolean isValidRotation(
            int rotationDegrees
    ) {
        return rotationDegrees == 0
                || rotationDegrees == 90
                || rotationDegrees == 180
                || rotationDegrees == 270;
    }

    @NonNull
    private String getErrorMessage(
            @NonNull Exception exception,
            @NonNull String fallbackMessage
    ) {
        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return fallbackMessage;
        }

        return message.trim();
    }

    public interface ScanCallback {

        /**
         * Success और failure दोनों इसी result object से लौटेंगे।
         */
        void onScanCompleted(
                @NonNull BookContentsScanResult result
        );
    }
}