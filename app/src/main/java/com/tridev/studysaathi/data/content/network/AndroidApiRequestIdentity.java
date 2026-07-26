package com.tridev.studysaathi.data.content.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AndroidApiRequestIdentity {

    public static final String HEADER_ANDROID_PACKAGE =
            "X-Android-Package";

    public static final String HEADER_ANDROID_CERT =
            "X-Android-Cert";

    private static final String SHA_1_ALGORITHM =
            "SHA-1";

    @NonNull
    private final String packageName;

    @NonNull
    private final String certificateSha1;

    @NonNull
    private final List<String> availableCertificateSha1Values;

    @NonNull
    private final String errorMessage;

    private AndroidApiRequestIdentity(
            @NonNull String packageName,
            @NonNull String certificateSha1,
            @NonNull List<String> availableCertificateSha1Values,
            @NonNull String errorMessage
    ) {
        this.packageName =
                normalizeText(
                        packageName
                );

        this.certificateSha1 =
                normalizeFingerprint(
                        certificateSha1
                );

        this.availableCertificateSha1Values =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                availableCertificateSha1Values
                        )
                );

        this.errorMessage =
                normalizeText(
                        errorMessage
                );
    }

    /**
     * Current installed app की Android API request identity तैयार करता है।
     *
     * Package name:
     *     com.tridev.studysaathi
     *
     * Certificate:
     *     Current APK को sign करने वाले certificate का SHA-1 fingerprint
     */
    @NonNull
    public static AndroidApiRequestIdentity create(
            @NonNull Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        String packageName =
                normalizeText(
                        applicationContext.getPackageName()
                );

        if (packageName.isEmpty()) {
            return failed(
                    "",
                    "Application package name is not available."
            );
        }

        try {
            List<String> fingerprints =
                    readSigningCertificateFingerprints(
                            applicationContext,
                            packageName
                    );

            if (fingerprints.isEmpty()) {
                return failed(
                        packageName,
                        "Application signing certificate could not be read."
                );
            }

            /*
             * पहली fingerprint current APK signer की होगी।
             *
             * Multiple signers या certificate rotation की स्थिति में
             * बाकी fingerprints diagnostic list में सुरक्षित रहती हैं।
             */
            String primaryFingerprint =
                    fingerprints.get(
                            0
                    );

            return new AndroidApiRequestIdentity(
                    packageName,
                    primaryFingerprint,
                    fingerprints,
                    ""
            );

        } catch (PackageManager.NameNotFoundException exception) {
            return failed(
                    packageName,
                    "Installed application package information was not found."
            );

        } catch (NoSuchAlgorithmException exception) {
            return failed(
                    packageName,
                    "SHA-1 certificate algorithm is not available."
            );

        } catch (SecurityException exception) {
            return failed(
                    packageName,
                    "Application signing certificate access was denied."
            );

        } catch (RuntimeException exception) {
            return failed(
                    packageName,
                    "Application signing identity could not be prepared."
            );
        }
    }

    @NonNull
    private static AndroidApiRequestIdentity failed(
            @Nullable String packageName,
            @NonNull String errorMessage
    ) {
        return new AndroidApiRequestIdentity(
                normalizeText(
                        packageName
                ),
                "",
                new ArrayList<>(),
                errorMessage
        );
    }

    /**
     * Google REST API request में Android restriction headers लगाता है।
     *
     * API key URL query parameter या दूसरे request mechanism से अलग
     * भेजी जाती है। यह method केवल Android app identity headers लगाता है।
     */
    public boolean applyTo(
            @NonNull HttpURLConnection connection
    ) {
        if (!isComplete()) {
            return false;
        }

        connection.setRequestProperty(
                HEADER_ANDROID_PACKAGE,
                packageName
        );

        connection.setRequestProperty(
                HEADER_ANDROID_CERT,
                certificateSha1
        );

        return true;
    }

    /**
     * Identity complete होने के लिए package और SHA-1 दोनों आवश्यक हैं।
     */
    public boolean isComplete() {
        return !packageName.isEmpty()
                && !certificateSha1.isEmpty();
    }

    public boolean hasError() {
        return !errorMessage.isEmpty();
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    /**
     * Fingerprint uppercase hexadecimal format में return होती है।
     *
     * उदाहरण:
     * A1B2C3D4E5F60718293A4B5C6D7E8F9012345678
     */
    @NonNull
    public String getCertificateSha1() {
        return certificateSha1;
    }

    /**
     * Certificate rotation या multiple signer होने पर सभी प्राप्त
     * SHA-1 fingerprints की immutable list देता है।
     */
    @NonNull
    public List<String> getAvailableCertificateSha1Values() {
        return availableCertificateSha1Values;
    }

    @NonNull
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Google Cloud Console में डालने योग्य colon-separated SHA-1 देता है।
     *
     * उदाहरण:
     * A1:B2:C3:D4:E5:F6:07:18:29:3A:4B:5C:6D:7E:8F:90:12:34:56:78
     */
    @NonNull
    public String getCertificateSha1WithColons() {
        return addFingerprintSeparators(
                certificateSha1
        );
    }

    @NonNull
    public String createDiagnosticSummary() {
        if (!isComplete()) {
            return errorMessage.isEmpty()
                    ? "Android API request identity is incomplete."
                    : errorMessage;
        }

        return "Package: "
                + packageName
                + "\nSHA-1: "
                + getCertificateSha1WithColons();
    }

    @NonNull
    private static List<String> readSigningCertificateFingerprints(
            @NonNull Context context,
            @NonNull String packageName
    ) throws PackageManager.NameNotFoundException,
            NoSuchAlgorithmException {

        PackageManager packageManager =
                context.getPackageManager();

        Signature[] signatures;

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.P) {

            PackageInfo packageInfo =
                    packageManager.getPackageInfo(
                            packageName,
                            PackageManager
                                    .GET_SIGNING_CERTIFICATES
                    );

            SigningInfo signingInfo =
                    packageInfo.signingInfo;

            if (signingInfo == null) {
                return new ArrayList<>();
            }

            /*
             * getApkContentsSigners() current APK को sign करने वाले
             * certificate या certificates देता है।
             *
             * Certificate rotation होने पर भी request में current signer
             * की fingerprint उपयोग करना आवश्यक है।
             */
            signatures =
                    signingInfo.getApkContentsSigners();

            if (signatures == null
                    || signatures.length == 0) {

                signatures =
                        signingInfo
                                .getSigningCertificateHistory();
            }

        } else {
            @SuppressWarnings("deprecation")
            PackageInfo packageInfo =
                    packageManager.getPackageInfo(
                            packageName,
                            PackageManager.GET_SIGNATURES
                    );

            @SuppressWarnings("deprecation")
            Signature[] legacySignatures =
                    packageInfo.signatures;

            signatures =
                    legacySignatures;
        }

        return createUniqueFingerprints(
                signatures
        );
    }

    @NonNull
    private static List<String> createUniqueFingerprints(
            @Nullable Signature[] signatures
    ) throws NoSuchAlgorithmException {

        List<String> fingerprints =
                new ArrayList<>();

        if (signatures == null
                || signatures.length == 0) {

            return fingerprints;
        }

        for (Signature signature :
                signatures) {

            if (signature == null) {
                continue;
            }

            String fingerprint =
                    createSha1Fingerprint(
                            signature.toByteArray()
                    );

            if (fingerprint.isEmpty()) {
                continue;
            }

            boolean alreadyAdded =
                    false;

            for (String existingFingerprint :
                    fingerprints) {

                if (existingFingerprint.equalsIgnoreCase(
                        fingerprint
                )) {
                    alreadyAdded =
                            true;

                    break;
                }
            }

            if (!alreadyAdded) {
                fingerprints.add(
                        fingerprint
                );
            }
        }

        return fingerprints;
    }

    @NonNull
    private static String createSha1Fingerprint(
            @Nullable byte[] certificateBytes
    ) throws NoSuchAlgorithmException {

        if (certificateBytes == null
                || certificateBytes.length == 0) {

            return "";
        }

        MessageDigest messageDigest =
                MessageDigest.getInstance(
                        SHA_1_ALGORITHM
                );

        byte[] digest =
                messageDigest.digest(
                        certificateBytes
                );

        StringBuilder fingerprintBuilder =
                new StringBuilder(
                        digest.length * 2
                );

        for (byte digestByte :
                digest) {

            fingerprintBuilder.append(
                    String.format(
                            Locale.ROOT,
                            "%02X",
                            digestByte & 0xFF
                    )
            );
        }

        return fingerprintBuilder.toString();
    }

    @NonNull
    private static String addFingerprintSeparators(
            @Nullable String fingerprint
    ) {
        String normalizedFingerprint =
                normalizeFingerprint(
                        fingerprint
                );

        if (normalizedFingerprint.isEmpty()) {
            return "";
        }

        StringBuilder formattedFingerprint =
                new StringBuilder(
                        normalizedFingerprint.length()
                                + normalizedFingerprint.length() / 2
                );

        for (int index = 0;
             index < normalizedFingerprint.length();
             index += 2) {

            if (formattedFingerprint.length() > 0) {
                formattedFingerprint.append(
                        ':'
                );
            }

            int endIndex =
                    Math.min(
                            normalizedFingerprint.length(),
                            index + 2
                    );

            formattedFingerprint.append(
                    normalizedFingerprint,
                    index,
                    endIndex
            );
        }

        return formattedFingerprint.toString();
    }

    @NonNull
    private static String normalizeFingerprint(
            @Nullable Object value
    ) {
        return normalizeText(
                value
        )
                .replaceAll(
                        "[^0-9A-Fa-f]",
                        ""
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }

    @NonNull
    private static String normalizeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim();
    }
}