package com.tridev.studysaathi.family;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tridev.studysaathi.cloud.CloudBackupPayloadBuilder;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/** Full-app encrypted family snapshot transport; old UID backup is not modified. */
public final class FamilySnapshotSyncRepository {
    private static final int CHUNK_SIZE = 400_000;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Context context;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public FamilySnapshotSyncRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void upload(@NonNull Callback callback) {
        FamilyWorkspaceSession.State state = validState(callback); if (state == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        EXECUTOR.execute(() -> {
            try {
                CloudBackupPayloadBuilder.CloudBackupPayload payload =
                        new CloudBackupPayloadBuilder(context).build();
                StringBuilder encoded = new StringBuilder(payload.getEncodedCharacters());
                for (String chunk : payload.getChunks()) encoded.append(chunk);
                String encrypted = FamilyWorkspaceCrypto.encrypt(encoded.toString(),
                        state.familyId, state.inviteCode);
                List<String> chunks = split(encrypted);
                String version = payload.getBackupId();
                DocumentReference latest = firestore.collection("families")
                        .document(state.familyId).collection("shared_snapshots").document("latest");
                Map<String,Object> metadata = new HashMap<>();
                metadata.put("snapshot_id", version); metadata.put("owner_uid", user.getUid());
                metadata.put("format", "family_workspace_encrypted_v1");
                metadata.put("chunk_count", chunks.size()); metadata.put("checksum", sha256(encrypted));
                metadata.put("database_schema_version", payload.getDatabaseSchemaVersion());
                metadata.put("created_at_ms", System.currentTimeMillis());
                metadata.put("server_updated_at", FieldValue.serverTimestamp());
                latest.set(metadata).addOnSuccessListener(v -> uploadChunks(latest, version,
                        chunks, 0, callback)).addOnFailureListener(callback::onError);
            } catch (Exception error) { callback.onError(error); }
        });
    }

    private void uploadChunks(DocumentReference latest, String version, List<String> chunks,
                              int index, Callback callback) {
        if (index >= chunks.size()) { callback.onSuccess("Family data सुरक्षित रूप से sync हो गया।"); return; }
        Map<String,Object> value = new HashMap<>(); value.put("snapshot_id", version);
        value.put("index", index); value.put("payload", chunks.get(index));
        latest.collection("chunks").document(String.format(java.util.Locale.US, "chunk_%03d", index))
                .set(value).addOnSuccessListener(v -> uploadChunks(latest, version, chunks,
                        index + 1, callback)).addOnFailureListener(callback::onError);
    }

    public void download(@NonNull DownloadCallback callback) {
        FamilyWorkspaceSession.State state = validState(callback); if (state == null) return;
        DocumentReference latest = firestore.collection("families").document(state.familyId)
                .collection("shared_snapshots").document("latest");
        latest.get().addOnSuccessListener(meta -> {
            if (!meta.exists()) { callback.onError(new IllegalStateException(
                    "Family का shared data अभी upload नहीं हुआ है।")); return; }
            String snapshotId = safe(meta.getString("snapshot_id"));
            String expectedChecksum = safe(meta.getString("checksum"));
            Long countValue = meta.getLong("chunk_count"); int expectedCount = countValue == null ? 0 : countValue.intValue();
            latest.collection("chunks").orderBy("index", Query.Direction.ASCENDING).get()
                    .addOnSuccessListener(result -> EXECUTOR.execute(() -> {
                        try {
                            StringBuilder encrypted = new StringBuilder(); int accepted = 0;
                            for (com.google.firebase.firestore.DocumentSnapshot doc : result.getDocuments()) {
                                if (!snapshotId.equals(safe(doc.getString("snapshot_id")))) continue;
                                encrypted.append(safe(doc.getString("payload"))); accepted++;
                            }
                            if (accepted != expectedCount || !expectedChecksum.equals(sha256(encrypted.toString())))
                                throw new SecurityException("Family snapshot अधूरा या बदला हुआ है।");
                            String encoded = FamilyWorkspaceCrypto.decrypt(encrypted.toString(),
                                    state.familyId, state.inviteCode);
                            byte[] compressed = Base64.decode(encoded, Base64.NO_WRAP);
                            byte[] jsonBytes = gunzip(compressed);
                            JSONObject json = new JSONObject(new String(jsonBytes, StandardCharsets.UTF_8));
                            if (!CloudBackupPayloadBuilder.BACKUP_FORMAT.equals(json.optString("backup_format")))
                                throw new SecurityException("Family snapshot format मान्य नहीं है।");
                            File dir = new File(context.getCacheDir(), "family_workspace_restore");
                            if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("Restore cache unavailable.");
                            File file = new File(dir, "family-" + snapshotId + ".json");
                            try (FileOutputStream out = new FileOutputStream(file)) { out.write(jsonBytes); }
                            callback.onDownloaded(file, "Study-Saathi-Family-" + snapshotId + ".json");
                        } catch (Exception error) { callback.onError(error); }
                    })).addOnFailureListener(callback::onError);
        }).addOnFailureListener(callback::onError);
    }

    private FamilyWorkspaceSession.State validState(ErrorCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FamilyWorkspaceSession.State state = FamilyWorkspaceSession.load(context);
        if (user == null || !user.isEmailVerified()) { callback.onError(new SecurityException(
                "Verified parent account से sign in करें।")); return null; }
        if (!state.isActive()) { callback.onError(new IllegalStateException(
                "पहले Family Workspace बनाएँ या join करें।")); return null; }
        return state;
    }

    private static List<String> split(String value) { List<String> out = new ArrayList<>();
        for (int i=0;i<value.length();i+=CHUNK_SIZE) out.add(value.substring(i, Math.min(value.length(),i+CHUNK_SIZE)));
        return out; }
    private static byte[] gunzip(byte[] value) throws Exception { try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(value)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] buffer=new byte[8192]; int read; while((read=in.read(buffer))!=-1) out.write(buffer,0,read); return out.toByteArray(); } }
    private static String sha256(String value) throws Exception { byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE); }
    private static String safe(String value) { return value == null ? "" : value; }

    public interface ErrorCallback { void onError(@NonNull Exception error); }
    public interface Callback extends ErrorCallback { void onSuccess(@NonNull String message); }
    public interface DownloadCallback extends ErrorCallback { void onDownloaded(@NonNull File file, @NonNull String displayName); }
}
