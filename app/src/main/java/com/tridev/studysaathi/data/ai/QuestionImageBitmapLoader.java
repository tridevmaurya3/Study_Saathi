package com.tridev.studysaathi.data.ai;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Camera या Gallery से चुनी गई Question Image को Firebase AI Logic
 * के लिए memory-safe Bitmap में बदलता है।
 *
 * मुख्य जिम्मेदारियाँ:
 *
 * 1. Image decoding को background thread पर चलाना।
 * 2. बहुत बड़ी image को उचित size पर downsample करना।
 * 3. Camera image की EXIF orientation ठीक करना।
 * 4. Sanskrit, Hindi, English और Mathematics text के लिए
 *    पर्याप्त readable resolution बनाए रखना।
 * 5. Result को Android main thread पर callback करना।
 */
public final class QuestionImageBitmapLoader {

    /**
     * Firebase AI को भेजने वाली image की अधिकतम लंबी side।
     *
     * 2048 pixels सामान्य school-book questions, Sanskrit text,
     * Mathematics symbols और diagrams के लिए अच्छा संतुलन देती है।
     */
    private static final int MAXIMUM_IMAGE_SIDE_PX =
            2048;

    /**
     * बहुत छोटी या invalid image को AI request में जाने से रोकता है।
     */
    private static final int MINIMUM_IMAGE_SIDE_PX =
            40;

    @NonNull
    private final Context applicationContext;

    @NonNull
    private final ContentResolver contentResolver;

    @NonNull
    private final ExecutorService imageDecodeExecutor;

    @NonNull
    private final Executor mainExecutor;

    @NonNull
    private final AtomicInteger requestGeneration =
            new AtomicInteger();

    private volatile boolean closed;

    public QuestionImageBitmapLoader(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        contentResolver =
                applicationContext.getContentResolver();

        imageDecodeExecutor =
                Executors.newSingleThreadExecutor();

        mainExecutor =
                ContextCompat.getMainExecutor(
                        applicationContext
                );
    }

    /**
     * Question image को background thread पर decode करता है।
     *
     * नई request आने पर पुरानी request का result ignore कर दिया जाता है।
     */
    public void loadForAi(
            @NonNull Uri imageUri,
            @NonNull ImageLoadCallback callback
    ) {
        if (closed) {
            callback.onError(
                    new IllegalStateException(
                            "Question image loader बंद हो चुका है।"
                    )
            );

            return;
        }

        final int currentGeneration =
                requestGeneration.incrementAndGet();

        imageDecodeExecutor.execute(() -> {
            Bitmap loadedBitmap =
                    null;

            Throwable loadingError =
                    null;

            try {
                loadedBitmap =
                        decodeQuestionImage(
                                imageUri
                        );

                if (loadedBitmap == null) {
                    throw new IOException(
                            "Question image decode नहीं हो सकी।"
                    );
                }

                validateDecodedBitmap(
                        loadedBitmap
                );

            } catch (Throwable throwable) {
                loadingError =
                        throwable;
            }

            final Bitmap finalBitmap =
                    loadedBitmap;

            final Throwable finalError =
                    loadingError;

            mainExecutor.execute(() -> {
                if (closed
                        || currentGeneration
                        != requestGeneration.get()) {

                    recycleBitmap(
                            finalBitmap
                    );

                    return;
                }

                if (finalError != null) {
                    recycleBitmap(
                            finalBitmap
                    );

                    callback.onError(
                            finalError
                    );

                    return;
                }

                if (finalBitmap == null) {
                    callback.onError(
                            new IOException(
                                    "Question image उपलब्ध नहीं है।"
                            )
                    );

                    return;
                }

                callback.onSuccess(
                        finalBitmap
                );
            });
        });
    }

    /**
     * Android 9 और उसके बाद ImageDecoder उपयोग करता है।
     *
     * पुराने Android versions के लिए BitmapFactory तथा
     * EXIF rotation fallback उपयोग होता है।
     */
    @NonNull
    private Bitmap decodeQuestionImage(
            @NonNull Uri imageUri
    ) throws IOException {

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.P) {

            return decodeUsingImageDecoder(
                    imageUri
            );
        }

        return decodeUsingBitmapFactory(
                imageUri
        );
    }

    /**
     * Android 9+ के लिए modern image decoding।
     *
     * ImageDecoder सामान्यतः source image की stored orientation
     * को ध्यान में रखते हुए Bitmap तैयार करता है।
     */
    @NonNull
    private Bitmap decodeUsingImageDecoder(
            @NonNull Uri imageUri
    ) throws IOException {

        ImageDecoder.Source source =
                ImageDecoder.createSource(
                        contentResolver,
                        imageUri
                );

        Bitmap decodedBitmap =
                ImageDecoder.decodeBitmap(
                        source,
                        (decoder, imageInfo, imageSource) -> {
                            Size sourceSize =
                                    imageInfo.getSize();

                            int sourceWidth =
                                    sourceSize.getWidth();

                            int sourceHeight =
                                    sourceSize.getHeight();

                            int[] targetSize =
                                    calculateTargetSize(
                                            sourceWidth,
                                            sourceHeight
                                    );

                            decoder.setTargetSize(
                                    targetSize[0],
                                    targetSize[1]
                            );

                            /*
                             * Firebase AI Content.Builder.addImage()
                             * के लिए software Bitmap अधिक predictable है।
                             */
                            decoder.setAllocator(
                                    ImageDecoder.ALLOCATOR_SOFTWARE
                            );

                            decoder.setMemorySizePolicy(
                                    ImageDecoder.MEMORY_POLICY_LOW_RAM
                            );

                            decoder.setMutableRequired(
                                    false
                            );
                        }
                );

        return ensureMaximumSize(
                decodedBitmap
        );
    }

    /**
     * Android 8 और Android 8.1 fallback।
     */
    @NonNull
    private Bitmap decodeUsingBitmapFactory(
            @NonNull Uri imageUri
    ) throws IOException {

        BitmapFactory.Options boundsOptions =
                new BitmapFactory.Options();

        boundsOptions.inJustDecodeBounds =
                true;

        try (InputStream boundsInputStream =
                     contentResolver.openInputStream(
                             imageUri
                     )) {

            if (boundsInputStream == null) {
                throw new IOException(
                        "Question image stream नहीं खुली।"
                );
            }

            BitmapFactory.decodeStream(
                    boundsInputStream,
                    null,
                    boundsOptions
            );
        }

        if (boundsOptions.outWidth <= 0
                || boundsOptions.outHeight <= 0) {

            throw new IOException(
                    "Question image का size valid नहीं है।"
            );
        }

        int sampleSize =
                calculateSampleSize(
                        boundsOptions.outWidth,
                        boundsOptions.outHeight
                );

        BitmapFactory.Options bitmapOptions =
                new BitmapFactory.Options();

        bitmapOptions.inSampleSize =
                sampleSize;

        bitmapOptions.inPreferredConfig =
                Bitmap.Config.ARGB_8888;

        bitmapOptions.inJustDecodeBounds =
                false;

        Bitmap decodedBitmap;

        try (InputStream imageInputStream =
                     contentResolver.openInputStream(
                             imageUri
                     )) {

            if (imageInputStream == null) {
                throw new IOException(
                        "Question image stream दोबारा नहीं खुली।"
                );
            }

            decodedBitmap =
                    BitmapFactory.decodeStream(
                            imageInputStream,
                            null,
                            bitmapOptions
                    );
        }

        if (decodedBitmap == null) {
            throw new IOException(
                    "Question image Bitmap में decode नहीं हुई।"
            );
        }

        int imageOrientation =
                readImageOrientation(
                        imageUri
                );

        Bitmap orientedBitmap =
                applyExifOrientation(
                        decodedBitmap,
                        imageOrientation
                );

        return ensureMaximumSize(
                orientedBitmap
        );
    }

    /**
     * Image की लंबी side 2048 pixels से अधिक न रखते हुए
     * aspect ratio सुरक्षित रखता है।
     */
    @NonNull
    private int[] calculateTargetSize(
            int sourceWidth,
            int sourceHeight
    ) {
        if (sourceWidth <= 0
                || sourceHeight <= 0) {

            return new int[]{
                    MAXIMUM_IMAGE_SIDE_PX,
                    MAXIMUM_IMAGE_SIDE_PX
            };
        }

        int longestSide =
                Math.max(
                        sourceWidth,
                        sourceHeight
                );

        if (longestSide
                <= MAXIMUM_IMAGE_SIDE_PX) {

            return new int[]{
                    sourceWidth,
                    sourceHeight
            };
        }

        float scale =
                MAXIMUM_IMAGE_SIDE_PX
                        / (float) longestSide;

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

        return new int[]{
                targetWidth,
                targetHeight
        };
    }

    private int calculateSampleSize(
            int sourceWidth,
            int sourceHeight
    ) {
        int sampleSize =
                1;

        while ((sourceWidth / sampleSize)
                > MAXIMUM_IMAGE_SIDE_PX * 2
                || (sourceHeight / sampleSize)
                > MAXIMUM_IMAGE_SIDE_PX * 2) {

            sampleSize *=
                    2;
        }

        return Math.max(
                1,
                sampleSize
        );
    }

    /**
     * EXIF orientation पढ़ता है।
     */
    private int readImageOrientation(
            @NonNull Uri imageUri
    ) {
        try (InputStream exifInputStream =
                     contentResolver.openInputStream(
                             imageUri
                     )) {

            if (exifInputStream == null) {
                return ExifInterface.ORIENTATION_NORMAL;
            }

            ExifInterface exifInterface =
                    new ExifInterface(
                            exifInputStream
                    );

            return exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

        } catch (IOException
                 | RuntimeException exception) {

            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    /**
     * Camera द्वारा store की गई EXIF orientation के अनुसार
     * Bitmap को rotate या mirror करता है।
     */
    @NonNull
    private Bitmap applyExifOrientation(
            @NonNull Bitmap sourceBitmap,
            int orientation
    ) {
        Matrix transformationMatrix =
                new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                transformationMatrix.setScale(
                        -1f,
                        1f
                );
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                transformationMatrix.setRotate(
                        180f
                );
                break;

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                transformationMatrix.setRotate(
                        180f
                );

                transformationMatrix.postScale(
                        -1f,
                        1f
                );
                break;

            case ExifInterface.ORIENTATION_TRANSPOSE:
                transformationMatrix.setRotate(
                        90f
                );

                transformationMatrix.postScale(
                        -1f,
                        1f
                );
                break;

            case ExifInterface.ORIENTATION_ROTATE_90:
                transformationMatrix.setRotate(
                        90f
                );
                break;

            case ExifInterface.ORIENTATION_TRANSVERSE:
                transformationMatrix.setRotate(
                        -90f
                );

                transformationMatrix.postScale(
                        -1f,
                        1f
                );
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                transformationMatrix.setRotate(
                        -90f
                );
                break;

            case ExifInterface.ORIENTATION_UNDEFINED:
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return sourceBitmap;
        }

        try {
            Bitmap transformedBitmap =
                    Bitmap.createBitmap(
                            sourceBitmap,
                            0,
                            0,
                            sourceBitmap.getWidth(),
                            sourceBitmap.getHeight(),
                            transformationMatrix,
                            true
                    );

            if (transformedBitmap
                    != sourceBitmap) {

                recycleBitmap(
                        sourceBitmap
                );
            }

            return transformedBitmap;

        } catch (OutOfMemoryError error) {
            return sourceBitmap;
        }
    }

    /**
     * Final decoded Bitmap की लंबी side को सीमा के भीतर रखता है।
     */
    @NonNull
    private Bitmap ensureMaximumSize(
            @NonNull Bitmap sourceBitmap
    ) {
        int sourceWidth =
                sourceBitmap.getWidth();

        int sourceHeight =
                sourceBitmap.getHeight();

        int longestSide =
                Math.max(
                        sourceWidth,
                        sourceHeight
                );

        if (longestSide
                <= MAXIMUM_IMAGE_SIDE_PX) {

            return sourceBitmap;
        }

        int[] targetSize =
                calculateTargetSize(
                        sourceWidth,
                        sourceHeight
                );

        Bitmap scaledBitmap =
                Bitmap.createScaledBitmap(
                        sourceBitmap,
                        targetSize[0],
                        targetSize[1],
                        true
                );

        if (scaledBitmap
                != sourceBitmap) {

            recycleBitmap(
                    sourceBitmap
            );
        }

        return scaledBitmap;
    }

    private void validateDecodedBitmap(
            @NonNull Bitmap bitmap
    ) throws IOException {

        if (bitmap.isRecycled()) {
            throw new IOException(
                    "Question image Bitmap recycled है।"
            );
        }

        if (bitmap.getWidth()
                < MINIMUM_IMAGE_SIDE_PX
                || bitmap.getHeight()
                < MINIMUM_IMAGE_SIDE_PX) {

            throw new IOException(
                    "Question image बहुत छोटी है।"
            );
        }
    }

    /**
     * वर्तमान image loading request cancel करता है।
     */
    public void cancelCurrentLoad() {
        requestGeneration.incrementAndGet();
    }

    /**
     * Activity destroy होने पर worker को बंद करें।
     */
    public void close() {
        if (closed) {
            return;
        }

        closed =
                true;

        requestGeneration.incrementAndGet();

        imageDecodeExecutor.shutdownNow();
    }

    private static void recycleBitmap(
            @Nullable Bitmap bitmap
    ) {
        if (bitmap == null
                || bitmap.isRecycled()) {

            return;
        }

        bitmap.recycle();
    }

    public interface ImageLoadCallback {

        /**
         * Returned Bitmap की ownership callback receiver की होगी।
         *
         * Firebase AI request पूरी होने के बाद इसे recycle करना चाहिए।
         */
        void onSuccess(
                @NonNull Bitmap bitmap
        );

        void onError(
                @NonNull Throwable throwable
        );
    }
}