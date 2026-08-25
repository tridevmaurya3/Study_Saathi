package com.tridev.studysaathi.family;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Client-side encryption dedicated to shared Family Workspace snapshots. */
public final class FamilyWorkspaceCrypto {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private FamilyWorkspaceCrypto() { }

    @NonNull
    public static String inviteHash(@NonNull String familyId, @NonNull String code)
            throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] value = digest.digest((familyId + "|" + normalizeCode(code))
                .getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(value, Base64.NO_WRAP | Base64.URL_SAFE);
    }

    @NonNull
    public static String encrypt(@NonNull String clearText, @NonNull String familyId,
                                 @NonNull String code) throws Exception {
        byte[] iv = new byte[IV_BYTES];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(familyId, code),
                new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(familyId.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8));
        return "fw1." + Base64.encodeToString(iv, Base64.NO_WRAP | Base64.URL_SAFE)
                + "." + Base64.encodeToString(encrypted, Base64.NO_WRAP | Base64.URL_SAFE);
    }

    @NonNull
    public static String decrypt(@NonNull String encryptedText, @NonNull String familyId,
                                 @NonNull String code) throws Exception {
        String[] parts = encryptedText.split("\\.", 3);
        if (parts.length != 3 || !"fw1".equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported family snapshot format.");
        }
        byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP | Base64.URL_SAFE);
        byte[] encrypted = Base64.decode(parts[2], Base64.NO_WRAP | Base64.URL_SAFE);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(familyId, code),
                new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(familyId.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    @NonNull
    public static String normalizeCode(@NonNull String code) {
        return code.replaceAll("[^A-Za-z0-9]", "").toUpperCase(java.util.Locale.US);
    }

    private static SecretKeySpec key(String familyId, String code) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(normalizeCode(code).toCharArray(),
                ("StudySaathiFamily|" + familyId).getBytes(StandardCharsets.UTF_8),
                ITERATIONS, KEY_BITS);
        byte[] bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        spec.clearPassword();
        return new SecretKeySpec(bytes, "AES");
    }
}
