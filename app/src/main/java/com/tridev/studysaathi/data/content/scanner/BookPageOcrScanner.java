package com.tridev.studysaathi.data.content.scanner;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari
        .DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.latin
        .TextRecognizerOptions;
import com.tridev.studysaathi.data.content.importer
        .BookDocumentPageExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Extracted book page images से क्रमवार English/Hindi OCR text निकालता है।
 */
public final class BookPageOcrScanner {

    @NonNull
    private final Context appContext;

    @NonNull
    private final Handler mainThreadHandler;

    @NonNull
    private final AtomicBoolean cancelled;

    private TextRecognizer latinRecognizer;
    private TextRecognizer devanagariRecognizer;

    public BookPageOcrScanner(
            @NonNull Context context
    ) {
        appContext =
                context.getApplicationContext();

        mainThreadHandler =
                new Handler(Looper.getMainLooper());

        cancelled =
                new AtomicBoolean(false);
    }

    public void scanPages(
            @NonNull BookDocumentPageExtractor
                    .ExtractionResult extractionResult,
            @NonNull ScanCallback callback
    ) {
        List<String> pagePaths =
                extractionResult
                        .getOrderedPageImagePaths();

        if (pagePaths.isEmpty()) {
            postError(
                    callback,
                    new IOException(
                            "No prepared book pages were found."
                    )
            );
            return;
        }

        cancel();
        cancelled.set(false);
        closeRecognizers();

        latinRecognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions
                                .DEFAULT_OPTIONS
                );

        devanagariRecognizer =
                TextRecognition.getClient(
                        new DevanagariTextRecognizerOptions
                                .Builder()
                                .build()
                );

        ArrayList<PageOcrText> pageResults =
                new ArrayList<>();

        scanNextPage(
                extractionResult,
                pagePaths,
                0,
                pageResults,
                callback
        );
    }

    public void cancel() {
        cancelled.set(true);
    }

    public void close() {
        cancel();
        closeRecognizers();
    }

    private void scanNextPage(
            @NonNull BookDocumentPageExtractor
                    .ExtractionResult extractionResult,
            @NonNull List<String> pagePaths,
            int pageIndex,
            @NonNull ArrayList<PageOcrText> pageResults,
            @NonNull ScanCallback callback
    ) {
        if (cancelled.get()) {
            closeRecognizers();
            postCancelled(callback);
            return;
        }

        if (pageIndex >= pagePaths.size()) {
            closeRecognizers();

            BookOcrResult result =
                    new BookOcrResult(
                            extractionResult.getRequestId(),
                            extractionResult
                                    .getSchoolBookRowId(),
                            pageResults
                    );

            mainThreadHandler.post(
                    () -> callback.onSuccess(result)
            );
            return;
        }

        String pagePath =
                pagePaths.get(pageIndex);

        InputImage inputImage;

        try {
            File pageFile =
                    new File(pagePath);

            if (!pageFile.isFile()
                    || !pageFile.canRead()) {
                throw new IOException(
                        "Prepared book page "
                                + (pageIndex + 1)
                                + " is missing."
                );
            }

            inputImage =
                    InputImage.fromFilePath(
                            appContext,
                            Uri.fromFile(pageFile)
                    );
        } catch (IOException
                 | RuntimeException exception) {
            failScan(
                    callback,
                    readableException(
                            exception,
                            "Book page "
                                    + (pageIndex + 1)
                                    + " could not be opened."
                    )
            );
            return;
        }

        recognizeLatin(
                inputImage,
                extractionResult,
                pagePaths,
                pageIndex,
                pageResults,
                callback
        );
    }

    private void recognizeLatin(
            @NonNull InputImage inputImage,
            @NonNull BookDocumentPageExtractor
                    .ExtractionResult extractionResult,
            @NonNull List<String> pagePaths,
            int pageIndex,
            @NonNull ArrayList<PageOcrText> pageResults,
            @NonNull ScanCallback callback
    ) {
        TextRecognizer recognizer =
                latinRecognizer;

        if (recognizer == null) {
            failScan(
                    callback,
                    "English OCR engine is unavailable."
            );
            return;
        }

        recognizer.process(inputImage)
                .addOnSuccessListener(text ->
                        recognizeDevanagari(
                                inputImage,
                                safeText(
                                        text == null
                                                ? null
                                                : text.getText()
                                ),
                                null,
                                extractionResult,
                                pagePaths,
                                pageIndex,
                                pageResults,
                                callback
                        )
                )
                .addOnFailureListener(exception ->
                        recognizeDevanagari(
                                inputImage,
                                "",
                                exception,
                                extractionResult,
                                pagePaths,
                                pageIndex,
                                pageResults,
                                callback
                        )
                );
    }

    private void recognizeDevanagari(
            @NonNull InputImage inputImage,
            @NonNull String latinText,
            Exception latinFailure,
            @NonNull BookDocumentPageExtractor
                    .ExtractionResult extractionResult,
            @NonNull List<String> pagePaths,
            int pageIndex,
            @NonNull ArrayList<PageOcrText> pageResults,
            @NonNull ScanCallback callback
    ) {
        TextRecognizer recognizer =
                devanagariRecognizer;

        if (recognizer == null) {
            failScan(
                    callback,
                    "Hindi OCR engine is unavailable."
            );
            return;
        }

        recognizer.process(inputImage)
                .addOnSuccessListener(text -> {
                    String devanagariText =
                            safeText(
                                    text == null
                                            ? null
                                            : text.getText()
                            );

                    completePage(
                            latinText,
                            devanagariText,
                            extractionResult,
                            pagePaths,
                            pageIndex,
                            pageResults,
                            callback
                    );
                })
                .addOnFailureListener(exception -> {
                    if (latinFailure != null) {
                        failScan(
                                callback,
                                "OCR failed on book page "
                                        + (pageIndex + 1)
                                        + "."
                        );
                        return;
                    }

                    completePage(
                            latinText,
                            "",
                            extractionResult,
                            pagePaths,
                            pageIndex,
                            pageResults,
                            callback
                    );
                });
    }

    private void completePage(
            @NonNull String latinText,
            @NonNull String devanagariText,
            @NonNull BookDocumentPageExtractor
                    .ExtractionResult extractionResult,
            @NonNull List<String> pagePaths,
            int pageIndex,
            @NonNull ArrayList<PageOcrText> pageResults,
            @NonNull ScanCallback callback
    ) {
        if (cancelled.get()) {
            closeRecognizers();
            postCancelled(callback);
            return;
        }

        String combinedText =
                mergeRecognizedText(
                        latinText,
                        devanagariText
                );

        pageResults.add(
                new PageOcrText(
                        pageIndex + 1,
                        pagePaths.get(pageIndex),
                        latinText,
                        devanagariText,
                        combinedText
                )
        );

        int completedPages =
                pageIndex + 1;

        mainThreadHandler.post(
                () -> callback.onProgress(
                        completedPages,
                        pagePaths.size()
                )
        );

        scanNextPage(
                extractionResult,
                pagePaths,
                pageIndex + 1,
                pageResults,
                callback
        );
    }

    @NonNull
    private static String mergeRecognizedText(
            @NonNull String latinText,
            @NonNull String devanagariText
    ) {
        Set<String> orderedLines =
                new LinkedHashSet<>();

        addLines(
                orderedLines,
                latinText
        );

        addLines(
                orderedLines,
                devanagariText
        );

        StringBuilder builder =
                new StringBuilder();

        for (String line : orderedLines) {
            if (builder.length() > 0) {
                builder.append('\n');
            }

            builder.append(line);
        }

        return builder.toString();
    }

    private static void addLines(
            @NonNull Set<String> target,
            @NonNull String text
    ) {
        String[] lines =
                text.split("\\R");

        for (String line : lines) {
            String normalized =
                    safeText(line);

            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    private void failScan(
            @NonNull ScanCallback callback,
            @NonNull String message
    ) {
        closeRecognizers();

        postError(
                callback,
                new IOException(message)
        );
    }

    private void closeRecognizers() {
        if (latinRecognizer != null) {
            latinRecognizer.close();
            latinRecognizer = null;
        }

        if (devanagariRecognizer != null) {
            devanagariRecognizer.close();
            devanagariRecognizer = null;
        }
    }

    private void postCancelled(
            @NonNull ScanCallback callback
    ) {
        mainThreadHandler.post(
                callback::onCancelled
        );
    }

    private void postError(
            @NonNull ScanCallback callback,
            @NonNull Exception exception
    ) {
        mainThreadHandler.post(
                () -> callback.onError(exception)
        );
    }

    @NonNull
    private static String readableException(
            @NonNull Exception exception,
            @NonNull String fallback
    ) {
        String message =
                safeText(exception.getMessage());

        return message.isEmpty()
                ? fallback
                : message;
    }

    @NonNull
    private static String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    public interface ScanCallback {

        void onProgress(
                int completedPages,
                int totalPages
        );

        void onSuccess(
                @NonNull BookOcrResult result
        );

        void onCancelled();

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class BookOcrResult {

        @NonNull
        private final String requestId;

        private final long schoolBookRowId;

        @NonNull
        private final List<PageOcrText> pages;

        private BookOcrResult(
                @NonNull String requestId,
                long schoolBookRowId,
                @NonNull List<PageOcrText> pages
        ) {
            this.requestId = requestId;
            this.schoolBookRowId = schoolBookRowId;
            this.pages =
                    Collections.unmodifiableList(
                            new ArrayList<>(pages)
                    );
        }

        @NonNull
        public String getRequestId() {
            return requestId;
        }

        public long getSchoolBookRowId() {
            return schoolBookRowId;
        }

        @NonNull
        public List<PageOcrText> getPages() {
            return pages;
        }

        public int getPageCount() {
            return pages.size();
        }

        public int getPagesWithTextCount() {
            int count = 0;

            for (PageOcrText page : pages) {
                if (!page.getCombinedText().isEmpty()) {
                    count++;
                }
            }

            return count;
        }
    }

    public static final class PageOcrText {

        private final int pageNumber;

        @NonNull
        private final String pageImagePath;

        @NonNull
        private final String latinText;

        @NonNull
        private final String devanagariText;

        @NonNull
        private final String combinedText;

        private PageOcrText(
                int pageNumber,
                @NonNull String pageImagePath,
                @NonNull String latinText,
                @NonNull String devanagariText,
                @NonNull String combinedText
        ) {
            this.pageNumber = pageNumber;
            this.pageImagePath = pageImagePath;
            this.latinText = latinText;
            this.devanagariText = devanagariText;
            this.combinedText = combinedText;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        @NonNull
        public String getPageImagePath() {
            return pageImagePath;
        }

        @NonNull
        public String getLatinText() {
            return latinText;
        }

        @NonNull
        public String getDevanagariText() {
            return devanagariText;
        }

        @NonNull
        public String getCombinedText() {
            return combinedText;
        }
    }
}
