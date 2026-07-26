package com.tridev.studysaathi.data.content.importer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model
        .BookLearningImportRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Selected full-book PDF/image को ordered local page images में बदलता है।
 * Heavy work background thread पर और callback main thread पर चलता है।
 */
public final class BookDocumentPageExtractor {

    private static final int MAXIMUM_PDF_PAGES = 600;
    private static final int MAX_RENDER_WIDTH_PX = 1800;
    private static final int JPEG_QUALITY = 88;

    @NonNull
    private final Context appContext;

    @NonNull
    private final ExecutorService executor;

    @NonNull
    private final Handler mainThreadHandler;

    @NonNull
    private final AtomicBoolean cancelled;

    public BookDocumentPageExtractor(
            @NonNull Context context
    ) {
        appContext =
                context.getApplicationContext();

        executor =
                Executors.newSingleThreadExecutor();

        mainThreadHandler =
                new Handler(Looper.getMainLooper());

        cancelled =
                new AtomicBoolean(false);
    }

    public void extractPages(
            @NonNull BookLearningImportRequest request,
            @NonNull ExtractionCallback callback
    ) {
        cancelled.set(false);

        executor.execute(() -> {
            try {
                request.markReadingDocument();

                ExtractionResult result =
                        request.isPdf()
                                ? extractPdfPages(
                                request,
                                callback
                        )
                                : extractImagePage(
                                request,
                                callback
                        );

                ensureNotCancelled();

                mainThreadHandler.post(
                        () -> callback.onSuccess(result)
                );
            } catch (Exception exception) {
                if (cancelled.get()) {
                    mainThreadHandler.post(
                            callback::onCancelled
                    );
                    return;
                }

                String message =
                        readableMessage(exception);

                request.markFailed(message);

                mainThreadHandler.post(
                        () -> callback.onError(
                                new IOException(
                                        message,
                                        exception
                                )
                        )
                );
            }
        });
    }

    public void cancel() {
        cancelled.set(true);
    }

    public void close() {
        cancel();
        executor.shutdownNow();
    }

    @NonNull
    private ExtractionResult extractPdfPages(
            @NonNull BookLearningImportRequest request,
            @NonNull ExtractionCallback callback
    ) throws IOException {
        Uri sourceUri =
                Uri.parse(request.getSourceUri());

        File outputDirectory =
                createOutputDirectory(request);

        ArrayList<String> pagePaths =
                new ArrayList<>();

        try (ParcelFileDescriptor descriptor =
                     appContext.getContentResolver()
                             .openFileDescriptor(
                                     sourceUri,
                                     "r"
                             )) {

            if (descriptor == null) {
                throw new IOException(
                        "The selected PDF could not be opened."
                );
            }

            try (PdfRenderer renderer =
                         new PdfRenderer(descriptor)) {

                int pageCount =
                        renderer.getPageCount();

                if (pageCount <= 0) {
                    throw new IOException(
                            "The selected PDF has no readable pages."
                    );
                }

                if (pageCount > MAXIMUM_PDF_PAGES) {
                    throw new IOException(
                            "This PDF has "
                                    + pageCount
                                    + " pages. The current limit is "
                                    + MAXIMUM_PDF_PAGES
                                    + " pages."
                    );
                }

                for (int pageIndex = 0;
                     pageIndex < pageCount;
                     pageIndex++) {

                    ensureNotCancelled();

                    File pageFile =
                            renderPdfPage(
                                    renderer,
                                    pageIndex,
                                    outputDirectory
                            );

                    pagePaths.add(
                            pageFile.getAbsolutePath()
                    );

                    postProgress(
                            callback,
                            pageIndex + 1,
                            pageCount
                    );
                }
            }
        } catch (SecurityException exception) {
            throw new IOException(
                    "Permission to read the selected PDF was lost.",
                    exception
            );
        }

        return new ExtractionResult(
                request.getRequestId(),
                request.getSchoolBookRowId(),
                request.getDisplayName(),
                outputDirectory.getAbsolutePath(),
                pagePaths
        );
    }

    @NonNull
    private File renderPdfPage(
            @NonNull PdfRenderer renderer,
            int pageIndex,
            @NonNull File outputDirectory
    ) throws IOException {
        try (PdfRenderer.Page page =
                     renderer.openPage(pageIndex)) {

            int sourceWidth =
                    Math.max(1, page.getWidth());

            int sourceHeight =
                    Math.max(1, page.getHeight());

            float scale =
                    MAX_RENDER_WIDTH_PX
                            / (float) sourceWidth;

            int targetWidth =
                    Math.max(
                            1,
                            Math.round(
                                    sourceWidth * scale
                            )
                    );

            int targetHeight =
                    Math.max(
                            1,
                            Math.round(
                                    sourceHeight * scale
                            )
                    );

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            targetWidth,
                            targetHeight,
                            Bitmap.Config.ARGB_8888
                    );

            bitmap.eraseColor(Color.WHITE);

            try {
                page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                );

                File pageFile =
                        createPageFile(
                                outputDirectory,
                                pageIndex + 1
                        );

                writeBitmap(
                        bitmap,
                        pageFile
                );

                return pageFile;
            } finally {
                bitmap.recycle();
            }
        }
    }

    @NonNull
    private ExtractionResult extractImagePage(
            @NonNull BookLearningImportRequest request,
            @NonNull ExtractionCallback callback
    ) throws IOException {
        Uri sourceUri =
                Uri.parse(request.getSourceUri());

        File outputDirectory =
                createOutputDirectory(request);

        Bitmap bitmap;

        try (InputStream inputStream =
                     appContext.getContentResolver()
                             .openInputStream(sourceUri)) {

            if (inputStream == null) {
                throw new IOException(
                        "The selected image could not be opened."
                );
            }

            bitmap =
                    BitmapFactory.decodeStream(
                            inputStream
                    );
        } catch (SecurityException exception) {
            throw new IOException(
                    "Permission to read the selected image was lost.",
                    exception
            );
        }

        if (bitmap == null) {
            throw new IOException(
                    "The selected image format could not be decoded."
            );
        }

        ensureNotCancelled();

        File pageFile =
                createPageFile(
                        outputDirectory,
                        1
                );

        try {
            writeBitmap(
                    bitmap,
                    pageFile
            );
        } finally {
            bitmap.recycle();
        }

        postProgress(
                callback,
                1,
                1
        );

        return new ExtractionResult(
                request.getRequestId(),
                request.getSchoolBookRowId(),
                request.getDisplayName(),
                outputDirectory.getAbsolutePath(),
                Collections.singletonList(
                        pageFile.getAbsolutePath()
                )
        );
    }

    @NonNull
    private File createOutputDirectory(
            @NonNull BookLearningImportRequest request
    ) throws IOException {
        File importRoot =
                new File(
                        appContext.getCacheDir(),
                        "book_learning_imports"
                );

        if (!importRoot.exists()
                && !importRoot.mkdirs()) {
            throw new IOException(
                    "Book import cache could not be created."
            );
        }

        File outputDirectory =
                new File(
                        importRoot,
                        request.getRequestId()
                );

        if (!outputDirectory.exists()
                && !outputDirectory.mkdirs()) {
            throw new IOException(
                    "Book page folder could not be created."
            );
        }

        return outputDirectory;
    }

    @NonNull
    private static File createPageFile(
            @NonNull File outputDirectory,
            int pageNumber
    ) {
        String fileName =
                String.format(
                        Locale.US,
                        "page_%04d.jpg",
                        pageNumber
                );

        return new File(
                outputDirectory,
                fileName
        );
    }

    private static void writeBitmap(
            @NonNull Bitmap bitmap,
            @NonNull File targetFile
    ) throws IOException {
        try (FileOutputStream outputStream =
                     new FileOutputStream(targetFile)) {

            boolean saved =
                    bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            JPEG_QUALITY,
                            outputStream
                    );

            outputStream.flush();

            if (!saved) {
                throw new IOException(
                        "A book page image could not be saved."
                );
            }
        }
    }

    private void postProgress(
            @NonNull ExtractionCallback callback,
            int completedPages,
            int totalPages
    ) {
        mainThreadHandler.post(
                () -> callback.onProgress(
                        completedPages,
                        totalPages
                )
        );
    }

    private void ensureNotCancelled()
            throws IOException {
        if (cancelled.get()
                || Thread.currentThread()
                .isInterrupted()) {
            throw new IOException(
                    "Book page extraction was cancelled."
            );
        }
    }

    @NonNull
    private static String readableMessage(
            @NonNull Exception exception
    ) {
        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {
            return "Book pages could not be extracted.";
        }

        return message.trim();
    }

    public interface ExtractionCallback {

        void onProgress(
                int completedPages,
                int totalPages
        );

        void onSuccess(
                @NonNull ExtractionResult result
        );

        void onCancelled();

        void onError(
                @NonNull Exception exception
        );
    }

    public static final class ExtractionResult {

        @NonNull
        private final String requestId;

        private final long schoolBookRowId;

        @NonNull
        private final String sourceDisplayName;

        @NonNull
        private final String outputDirectoryPath;

        @NonNull
        private final List<String> orderedPageImagePaths;

        private ExtractionResult(
                @NonNull String requestId,
                long schoolBookRowId,
                @NonNull String sourceDisplayName,
                @NonNull String outputDirectoryPath,
                @NonNull List<String> orderedPageImagePaths
        ) {
            this.requestId = requestId;
            this.schoolBookRowId = schoolBookRowId;
            this.sourceDisplayName =
                    sourceDisplayName;
            this.outputDirectoryPath =
                    outputDirectoryPath;
            this.orderedPageImagePaths =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    orderedPageImagePaths
                            )
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
        public String getSourceDisplayName() {
            return sourceDisplayName;
        }

        @NonNull
        public String getOutputDirectoryPath() {
            return outputDirectoryPath;
        }

        @NonNull
        public List<String> getOrderedPageImagePaths() {
            return orderedPageImagePaths;
        }

        public int getPageCount() {
            return orderedPageImagePaths.size();
        }
    }
}
