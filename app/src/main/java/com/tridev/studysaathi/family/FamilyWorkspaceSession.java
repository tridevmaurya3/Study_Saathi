package com.tridev.studysaathi.family;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Device-local pointer and secret for the signed-in parent's active family. */
public final class FamilyWorkspaceSession {
    // Intentionally outside the study_saathi_* backup namespace: this device-bound
    // Keystore ciphertext must never overwrite another parent's local family secret.
    private static final String PREFS = "family_workspace_device_secret";
    private static final String FAMILY_ID = "family_id";
    private static final String FAMILY_NAME = "family_name";
    private static final String ROLE = "role";
    private static final String CODE = "invite_secret";
    private static final String KEY_ALIAS = "study_saathi_family_workspace_key";

    private FamilyWorkspaceSession() { }

    public static void save(@NonNull Context context, @NonNull String familyId,
                            @NonNull String familyName, @NonNull String role,
                            @NonNull String inviteCode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(FAMILY_ID, familyId).putString(FAMILY_NAME, familyName)
                .putString(ROLE, role).putString(CODE, protect(inviteCode)).apply();
    }

    public static void clear(@NonNull Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    @NonNull public static State load(@NonNull Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new State(p.getString(FAMILY_ID, ""), p.getString(FAMILY_NAME, ""),
                p.getString(ROLE, ""), unprotect(p.getString(CODE, "")));
    }

    public static final class State {
        public final String familyId, familyName, role, inviteCode;
        State(String id, String name, String role, String code) {
            this.familyId = id == null ? "" : id; this.familyName = name == null ? "" : name;
            this.role = role == null ? "" : role; this.inviteCode = code == null ? "" : code;
        }
        public boolean isActive() { return !familyId.isEmpty() && !inviteCode.isEmpty(); }
    }

    private static String protect(String clear) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] encrypted = cipher.doFinal(clear.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "."
                    + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception error) { throw new IllegalStateException("Family secret सुरक्षित नहीं हो सका।", error); }
    }

    private static String unprotect(String stored) {
        if (stored == null || stored.isEmpty()) return "";
        try {
            String[] parts = stored.split("\\.", 2); if (parts.length != 2) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128,
                    Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                    StandardCharsets.UTF_8);
        } catch (Exception error) { return ""; }
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        java.security.Key existing = store.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
