package com.tridev.studysaathi.data.content.storage;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Temporary rendered PDF pages को app के persistent internal storage में
 * सुरक्षित करता है। Database में cache path save नहीं होना चाहिए।
 */
public final class BookPageImageStore {

    private static final String ROOT_DIRECTORY_NAME =
            "book_learning_pages";

    @NonNull
    private final File rootDirectory;

    public BookPageImageStore(
            @NonNull Context context
    ) {
        rootDirectory =
                new File(
                        context.getApplicationContext()
                                .getFilesDir(),
                        ROOT_DIRECTORY_NAME
                );
    }

    @NonNull
    public String persistPageImage(
            long schoolBookRowId,
            long chapterRowId,
            @NonNull String importRequestId,
            int pageOrder,
            int sourceDocumentPageNumber,
            @NonNull String temporaryImagePath
    ) throws IOException {
        validateIdentifiers(
                schoolBookRowId,
                chapterRowId,
                importRequestId,
                pageOrder,
                sourceDocumentPageNumber
        );

        File sourceFile =
                new File(temporaryImagePath);

        if (!sourceFile.isFile()
                || !sourceFile.canRead()) {
            throw new IOException(
                    "Temporary source page image is missing."
            );
        }

        File requestDirectory =
                createRequestDirectory(
                        schoolBookRowId,
                        chapterRowId,
                        importRequestId
                );

        String fileName =
                String.format(
                        Locale.US,
                        "reader_page_%04d_source_%04d.jpg",
                        pageOrder,
                        sourceDocumentPageNumber
                );

        File targetFile =
                new File(
                        requestDirectory,
                        fileName
                );

        ensureInsideRoot(targetFile);

        if (targetFile.isFile()
                && targetFile.length() > 0L) {
            return targetFile.getAbsolutePath();
        }

        copyFile(
                sourceFile,
                targetFile
        );

        return targetFile.getAbsolutePath();
    }

    @NonNull
    private File createRequestDirectory(
            long schoolBookRowId,
            long chapterRowId,
            @NonNull String importRequestId
    ) throws IOException {
        File bookDirectory =
                new File(
                        rootDirectory,
                        "book_" + schoolBookRowId
                );

        File chapterDirectory =
                new File(
                        bookDirectory,
                        "chapter_" + chapterRowId
                );

        File requestDirectory =
                new File(
                        chapterDirectory,
                        "import_"
                                + sanitizeIdentifier(
                                importRequestId
                        )
                );

        ensureInsideRoot(requestDirectory);

        if (!requestDirectory.exists()
                && !requestDirectory.mkdirs()) {
            throw new IOException(
                    "Persistent chapter page folder "
                            + "could not be created."
            );
        }

        return requestDirectory;
    }

    private void copyFile(
            @NonNull File sourceFile,
            @NonNull File targetFile
    ) throws IOException {
        File temporaryTarget =
                new File(
                        targetFile.getParentFile(),
                        targetFile.getName() + ".part"
                );

        ensureInsideRoot(temporaryTarget);

        try (FileInputStream inputStream =
                     new FileInputStream(sourceFile);
             FileOutputStream outputStream =
                     new FileOutputStream(
                             temporaryTarget,
                             false
                     )) {

            byte[] buffer =
                    new byte[32 * 1024];

            int bytesRead;

            while ((bytesRead =
                    inputStream.read(buffer)) >= 0) {

                if (bytesRead == 0) {
                    continue;
                }

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            outputStream.flush();
            outputStream.getFD().sync();
        } catch (IOException exception) {
            if (temporaryTarget.isFile()) {
                //noinspection ResultOfMethodCallIgnored
                temporaryTarget.delete();
            }

            throw exception;
        }

        if (temporaryTarget.length() <= 0L) {
            //noinspection ResultOfMethodCallIgnored
            temporaryTarget.delete();

            throw new IOException(
                    "Persistent page image copy is empty."
            );
        }

        if (targetFile.exists()
                && !targetFile.delete()) {
            //noinspection ResultOfMethodCallIgnored
            temporaryTarget.delete();

            throw new IOException(
                    "Existing page image could not be replaced."
            );
        }

        if (!temporaryTarget.renameTo(targetFile)) {
            //noinspection ResultOfMethodCallIgnored
            temporaryTarget.delete();

            throw new IOException(
                    "Persistent page image could not be finalized."
            );
        }
    }

    private void ensureInsideRoot(
            @NonNull File target
    ) throws IOException {
        String rootPath =
                rootDirectory.getCanonicalPath();

        String targetPath =
                target.getCanonicalPath();

        if (!targetPath.equals(rootPath)
                && !targetPath.startsWith(
                rootPath + File.separator
        )) {
            throw new IOException(
                    "Invalid persistent page image path."
            );
        }
    }

    private static void validateIdentifiers(
            long schoolBookRowId,
            long chapterRowId,
            @NonNull String importRequestId,
            int pageOrder,
            int sourceDocumentPageNumber
    ) {
        if (schoolBookRowId <= 0L
                || chapterRowId <= 0L) {
            throw new IllegalArgumentException(
                    "Valid exact book and chapter IDs are required."
            );
        }

        if (importRequestId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Import request ID is required."
            );
        }

        if (pageOrder <= 0
                || sourceDocumentPageNumber <= 0) {
            throw new IllegalArgumentException(
                    "Valid page numbers are required."
            );
        }
    }

    @NonNull
    private static String sanitizeIdentifier(
            @NonNull String value
    ) {
        String sanitized =
                value.trim()
                        .replaceAll(
                                "[^A-Za-z0-9_-]",
                                "_"
                        );

        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Import request ID is invalid."
            );
        }

        return sanitized;
    }
}
