package com.tridev.studysaathi.data.content.importer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.content.model
        .BookLearningImportRequest;

import java.io.IOException;
import java.util.Locale;

/**
 * Full-book learning content के लिए PDF/image चुनने और उसकी metadata पढ़ने
 * की shared utility.
 */
public final class BookLearningSourcePicker {

    private static final String MIME_TYPE_PDF =
            "application/pdf";

    private static final String MIME_TYPE_ANY_IMAGE =
            "image/*";

    private BookLearningSourcePicker() {
        throw new AssertionError(
                "BookLearningSourcePicker cannot be instantiated."
        );
    }

    /**
     * Activity Result launcher के साथ launch करने योग्य document picker Intent.
     */
    @NonNull
    public static Intent createPickerIntent() {
        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        MIME_TYPE_PDF,
                        MIME_TYPE_ANY_IMAGE
                }
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        return intent;
    }

    /**
     * चुनी गई Uri की read permission सुरक्षित रखता है और validated request
     * बनाता है। Unsupported या unreadable document पर IOException देता है।
     */
    @NonNull
    public static BookLearningImportRequest createImportRequest(
            @NonNull Context context,
            long schoolBookRowId,
            @NonNull Uri sourceUri
    ) throws IOException {
        if (schoolBookRowId <= 0L) {
            throw new IllegalArgumentException(
                    "A valid exact school book row ID is required."
            );
        }

        ContentResolver resolver =
                context.getApplicationContext()
                        .getContentResolver();

        takePersistentReadPermission(
                resolver,
                sourceUri
        );

        SourceMetadata metadata =
                readSourceMetadata(
                        resolver,
                        sourceUri
                );

        return BookLearningImportRequest.create(
                schoolBookRowId,
                sourceUri.toString(),
                metadata.displayName,
                metadata.mimeType,
                metadata.sizeBytes
        );
    }

    private static void takePersistentReadPermission(
            @NonNull ContentResolver resolver,
            @NonNull Uri sourceUri
    ) throws IOException {
        try {
            resolver.takePersistableUriPermission(
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException exception) {
            throw new IOException(
                    "The selected book file permission "
                            + "could not be saved.",
                    exception
            );
        }
    }

    @NonNull
    private static SourceMetadata readSourceMetadata(
            @NonNull ContentResolver resolver,
            @NonNull Uri sourceUri
    ) throws IOException {
        String displayName = "";
        long sizeBytes = 0L;

        String[] projection =
                new String[]{
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE
                };

        try (Cursor cursor =
                     resolver.query(
                             sourceUri,
                             projection,
                             null,
                             null,
                             null
                     )) {

            if (cursor != null
                    && cursor.moveToFirst()) {
                int displayNameColumn =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );

                int sizeColumn =
                        cursor.getColumnIndex(
                                OpenableColumns.SIZE
                        );

                if (displayNameColumn >= 0
                        && !cursor.isNull(
                        displayNameColumn
                )) {
                    displayName =
                            safeText(
                                    cursor.getString(
                                            displayNameColumn
                                    )
                            );
                }

                if (sizeColumn >= 0
                        && !cursor.isNull(sizeColumn)) {
                    sizeBytes =
                            Math.max(
                                    0L,
                                    cursor.getLong(
                                            sizeColumn
                                    )
                            );
                }
            }
        } catch (RuntimeException exception) {
            throw new IOException(
                    "The selected book file details "
                            + "could not be read.",
                    exception
            );
        }

        String mimeType =
                normalizeMimeType(
                        resolver.getType(sourceUri),
                        displayName
                );

        if (displayName.isEmpty()) {
            displayName =
                    createFallbackDisplayName(
                            mimeType
                    );
        }

        ensureReadable(
                resolver,
                sourceUri
        );

        return new SourceMetadata(
                displayName,
                mimeType,
                sizeBytes
        );
    }

    private static void ensureReadable(
            @NonNull ContentResolver resolver,
            @NonNull Uri sourceUri
    ) throws IOException {
        try (android.os.ParcelFileDescriptor ignored =
                     resolver.openFileDescriptor(
                             sourceUri,
                             "r"
                     )) {

            if (ignored == null) {
                throw new IOException(
                        "The selected book file cannot be opened."
                );
            }
        } catch (SecurityException exception) {
            throw new IOException(
                    "The selected book file cannot be read.",
                    exception
            );
        }
    }

    @NonNull
    private static String normalizeMimeType(
            @Nullable String resolverMimeType,
            @NonNull String displayName
    ) throws IOException {
        String mimeType =
                safeText(resolverMimeType)
                        .toLowerCase(Locale.US);

        if (MIME_TYPE_PDF.equals(mimeType)
                || mimeType.startsWith("image/")) {
            return mimeType;
        }

        String lowerName =
                displayName.toLowerCase(Locale.US);

        if (lowerName.endsWith(".pdf")) {
            return MIME_TYPE_PDF;
        }

        if (lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (lowerName.endsWith(".png")) {
            return "image/png";
        }

        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }

        if (lowerName.endsWith(".heic")
                || lowerName.endsWith(".heif")) {
            return "image/heic";
        }

        throw new IOException(
                "Please select a PDF or supported image book file."
        );
    }

    @NonNull
    private static String createFallbackDisplayName(
            @NonNull String mimeType
    ) {
        if (MIME_TYPE_PDF.equals(mimeType)) {
            return "Selected book.pdf";
        }

        return "Selected book image";
    }

    @NonNull
    private static String safeText(
            @Nullable String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private static final class SourceMetadata {

        @NonNull
        private final String displayName;

        @NonNull
        private final String mimeType;

        private final long sizeBytes;

        private SourceMetadata(
                @NonNull String displayName,
                @NonNull String mimeType,
                long sizeBytes
        ) {
            this.displayName = displayName;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
        }
    }
}
