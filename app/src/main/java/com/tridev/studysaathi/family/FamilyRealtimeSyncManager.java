package com.tridev.studysaathi.family;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Automatic encrypted, record-by-record Family Workspace synchronization.
 * Existing repositories keep writing to Room normally; additive SQLite triggers
 * capture changes into an outbox, so old write paths do not need modification.
 */
public final class FamilyRealtimeSyncManager {
    private static final String TAG = "FamilyRealtimeSync";
    private static final int CHUNK_SIZE = 360_000;
    private static final long POLL_SECONDS = 2L;
    private static volatile FamilyRealtimeSyncManager instance;

    private final Context context;
    private final FirebaseFirestore firestore;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();
    private ListenerRegistration listener;
    private String runningFamilyId = "";
    private final Map<String, DocumentSnapshot> deferredChildren = new HashMap<>();

    private static final List<TableSpec> TABLES = Collections.unmodifiableList(Arrays.asList(
            new TableSpec("student_profiles", "profile_id", 0, null, null),
            new TableSpec("school_curriculum_profiles", "profile_id", 1,
                    "student_profiles", "profile_id"),
            new TableSpec("lesson_progress", "progress_key", 1,
                    "student_profiles", "profile_id"),
            new TableSpec("quiz_attempts", "attempt_id", 1,
                    "student_profiles", "profile_id"),
            new TableSpec("doubt_history", "history_id", 1,
                    "student_profiles", "profile_id"),
            new TableSpec("school_subjects", "subject_row_id", 2,
                    "school_curriculum_profiles", "profile_id"),
            new TableSpec("school_books", "book_row_id", 3,
                    "school_subjects", "subject_row_id"),
            new TableSpec("school_book_chapters", "chapter_row_id", 4,
                    "school_books", "book_row_id"),
            new TableSpec("school_book_chapter_contents", "content_row_id", 5,
                    "school_book_chapters", "chapter_row_id"),
            new TableSpec("school_book_chapter_pages", "chapter_page_row_id", 5,
                    "school_book_chapters", "chapter_row_id")
    ));

    private FamilyRealtimeSyncManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        FirebaseAuth.getInstance().addAuthStateListener(auth -> refreshSession());
    }

    public static void start(@NonNull Context context) {
        synchronized (FamilyRealtimeSyncManager.class) {
            if (instance == null) instance = new FamilyRealtimeSyncManager(context);
            instance.refreshSession();
        }
    }

    public static void refresh(@NonNull Context context) {
        start(context);
    }

    private void refreshSession() {
        executor.execute(() -> {
            FamilyWorkspaceSession.State state = FamilyWorkspaceSession.load(context);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            boolean valid = state.isActive() && user != null && user.isEmailVerified();
            if (!valid) { stopListener(); return; }
            if (state.familyId.equals(runningFamilyId) && listener != null) return;
            stopListener();
            runningFamilyId = state.familyId;
            bootstrapExistingRecords(database());
            attachListener(state);
            executor.scheduleWithFixedDelay(this::flushSafely, 0L,
                    POLL_SECONDS, TimeUnit.SECONDS);
        });
    }

    private void attachListener(@NonNull FamilyWorkspaceSession.State state) {
        listener = records(state.familyId).orderBy("rank", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { Log.w(TAG, "Realtime listener failed.", error); return; }
                    if (snapshot == null) return;
                    List<DocumentChange> changes = new ArrayList<>(snapshot.getDocumentChanges());
                    changes.sort(Comparator.comparingInt(change ->
                            intValue(change.getDocument().getLong("rank"))));
                    executor.execute(() -> applyChanges(state, changes));
                });
    }

    private void stopListener() {
        if (listener != null) listener.remove();
        listener = null;
        runningFamilyId = "";
        deferredChildren.clear();
    }

    private void flushSafely() {
        try {
            FamilyWorkspaceSession.State state = FamilyWorkspaceSession.load(context);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (!state.isActive() || user == null || !user.isEmailVerified()
                    || !state.familyId.equals(runningFamilyId)) return;
            flushOutbox(state, user);
        } catch (Exception error) {
            Log.w(TAG, "Realtime outbox will retry.", error);
        }
    }

    private void flushOutbox(FamilyWorkspaceSession.State state, FirebaseUser user)
            throws Exception {
        SupportSQLiteDatabase db = database();
        try (Cursor cursor = db.query("SELECT table_name,local_id,sync_id,operation "
                + "FROM family_sync_outbox ORDER BY changed_at ASC LIMIT 25")) {
            while (cursor.moveToNext()) {
                String table = cursor.getString(0), localId = cursor.getString(1);
                String syncId = cursor.getString(2), operation = cursor.getString(3);
                TableSpec spec = spec(table); if (spec == null) { removeOutbox(db, table, syncId); continue; }
                DocumentReference record = records(state.familyId).document(syncId);
                if ("DELETE".equals(operation)) {
                    Map<String,Object> tombstone = baseMetadata(spec, syncId, user.getUid());
                    tombstone.put("deleted", true); tombstone.put("chunk_count", 0);
                    Tasks.await(record.set(tombstone));
                    cleanupOldChunks(record, "");
                } else {
                    JSONObject payload = readRecord(db, spec, localId);
                    if (payload == null) { // It was deleted after this outbox row was read.
                        continue;
                    }
                    addReference(db, spec, payload);
                    String encrypted = FamilyWorkspaceCrypto.encrypt(
                            compress(payload.toString()), state.familyId, state.inviteCode);
                    String version = UUID.randomUUID().toString();
                    List<String> chunks = split(encrypted);
                    for (int index = 0; index < chunks.size(); index++) {
                        Map<String,Object> part = new HashMap<>();
                        part.put("version", version); part.put("index", index);
                        part.put("payload", chunks.get(index));
                        Tasks.await(record.collection("payload_chunks")
                                .document(String.format(Locale.US, "%s_%04d", version, index))
                                .set(part));
                    }
                    Map<String,Object> metadata = baseMetadata(spec, syncId, user.getUid());
                    metadata.put("deleted", false); metadata.put("version", version);
                    metadata.put("chunk_count", chunks.size());
                    metadata.put("checksum", checksum(encrypted));
                    Tasks.await(record.set(metadata)); // Published only after all chunks exist.
                    cleanupOldChunks(record, version);
                }
                removeOutbox(db, table, syncId);
            }
        }
    }

    private void applyChanges(FamilyWorkspaceSession.State state,
                              List<DocumentChange> changes) {
        SupportSQLiteDatabase db = database();
        for (DocumentChange change : changes) {
            DocumentSnapshot document = change.getDocument();
            if (change.getType() == DocumentChange.Type.REMOVED) continue;
            try {
                applyDocument(db, state, document);
            } catch (MissingParentException deferred) {
                deferredChildren.put(document.getId(), document);
                Log.d(TAG, "Child record waits for its parent: " + document.getId());
            } catch (Exception error) {
                Log.w(TAG, "Remote record could not be applied: " + document.getId(), error);
            }
        }
        retryDeferredChildren(db, state);
    }

    private void retryDeferredChildren(SupportSQLiteDatabase db,
                                       FamilyWorkspaceSession.State state) {
        List<DocumentSnapshot> pending = new ArrayList<>(deferredChildren.values());
        pending.sort(Comparator.comparingInt(document -> intValue(document.getLong("rank"))));
        for (DocumentSnapshot document : pending) {
            try {
                applyDocument(db, state, document);
                deferredChildren.remove(document.getId());
            } catch (MissingParentException stillWaiting) {
                // A later parent snapshot will retry it.
            } catch (Exception error) {
                deferredChildren.remove(document.getId());
                Log.w(TAG, "Deferred record failed: " + document.getId(), error);
            }
        }
    }

    private void applyDocument(SupportSQLiteDatabase db,
                               FamilyWorkspaceSession.State state,
                               DocumentSnapshot document) throws Exception {
        TableSpec spec = spec(string(document.getString("table_name")));
        if (spec == null) return;
        String syncId = document.getId();
        // A local offline edit waiting in the outbox is newer from this device's
        // point of view. Do not let Firestore's cached older value overwrite it.
        if (hasPendingOutbox(db, spec.table, syncId)) return;
        String localId = localIdForSync(db, spec.table, syncId);
        boolean deleted = Boolean.TRUE.equals(document.getBoolean("deleted"));
        db.beginTransaction();
        try {
            setSuppressed(db, true);
            if (deleted) {
                if (localId != null) {
                    db.delete(spec.table, "`" + spec.primaryKey + "`=?", new Object[]{localId});
                    db.delete("family_sync_record_map", "table_name=? AND sync_id=?",
                            new Object[]{spec.table, syncId});
                }
            } else {
                int count = intValue(document.getLong("chunk_count"));
                String version = string(document.getString("version"));
                QuerySnapshot allParts = Tasks.await(document.getReference()
                        .collection("payload_chunks").get());
                List<DocumentSnapshot> parts = new ArrayList<>();
                for (DocumentSnapshot part : allParts.getDocuments())
                    if (version.equals(string(part.getString("version")))) parts.add(part);
                parts.sort(Comparator.comparingInt(part -> intValue(part.getLong("index"))));
                if (parts.size() != count) throw new SecurityException("Incomplete realtime record.");
                StringBuilder encrypted = new StringBuilder();
                for (DocumentSnapshot part : parts)
                    encrypted.append(string(part.getString("payload")));
                if (!checksum(encrypted.toString()).equals(string(document.getString("checksum"))))
                    throw new SecurityException("Realtime record checksum mismatch.");
                String clear = decompress(FamilyWorkspaceCrypto.decrypt(encrypted.toString(),
                        state.familyId, state.inviteCode));
                JSONObject payload = new JSONObject(clear);
                upsertLocal(db, spec, syncId, localId, payload);
            }
            setSuppressed(db, false);
            db.setTransactionSuccessful();
        } finally {
            if (db.inTransaction()) {
                try { setSuppressed(db, false); } catch (Exception ignored) { }
                db.endTransaction();
            }
        }
    }

    private void upsertLocal(SupportSQLiteDatabase db, TableSpec spec, String syncId,
                             @Nullable String localId, JSONObject payload) throws Exception {
        ContentValues values = jsonValues(payload.getJSONObject("data"));
        if (spec.parentTable != null) {
            String parentSyncId = payload.optString("parent_sync_id", "");
            String parentLocalId = localIdForSync(db, spec.parentTable, parentSyncId);
            if (parentLocalId == null) throw new MissingParentException();
            putTypedId(values, spec.parentColumn, parentLocalId);
        }
        if (localId != null) {
            putTypedId(values, spec.primaryKey, localId);
            db.update(spec.table, 0, values, "`" + spec.primaryKey + "`=?",
                    new Object[]{localId});
            return;
        }

        if ("school_curriculum_profiles".equals(spec.table)) {
            // Its primary key is also its student-profile foreign key.
            String parentLocalId = valueAsString(values.get("profile_id"));
            putTypedId(values, spec.primaryKey, parentLocalId);
        } else if ("lesson_progress".equals(spec.table)) {
            String remoteKey = values.getAsString(spec.primaryKey);
            String profileId = valueAsString(values.get("profile_id"));
            int separator = remoteKey == null ? -1 : remoteKey.indexOf('|');
            values.put(spec.primaryKey, profileId + (separator >= 0
                    ? remoteKey.substring(separator) : "|" + syncId));
        } else {
            values.remove(spec.primaryKey); // Let this device allocate its own row id.
        }

        long inserted = db.insert(spec.table, 0, values);
        if (inserted < 0) throw new IllegalStateException("Local record insert failed: " + spec.table);
        String allocated = "lesson_progress".equals(spec.table)
                ? values.getAsString(spec.primaryKey)
                : ("school_curriculum_profiles".equals(spec.table)
                ? valueAsString(values.get(spec.primaryKey)) : Long.toString(inserted));
        ContentValues map = new ContentValues(); map.put("table_name", spec.table);
        map.put("local_id", allocated); map.put("sync_id", syncId);
        db.insert("family_sync_record_map", 0, map);
    }

    private void bootstrapExistingRecords(SupportSQLiteDatabase db) {
        db.beginTransaction();
        try {
            for (TableSpec spec : TABLES) {
                try (Cursor cursor = db.query("SELECT `" + spec.primaryKey + "` FROM `"
                        + spec.table + "`")) {
                    while (cursor.moveToNext()) {
                        String localId = cursor.getString(0);
                        if (mappingExists(db, spec.table, localId)) continue;
                        String syncId = UUID.randomUUID().toString().replace("-", "");
                        ContentValues map = new ContentValues(); map.put("table_name", spec.table);
                        map.put("local_id", localId); map.put("sync_id", syncId);
                        db.insert("family_sync_record_map", 0, map);
                        ContentValues outbox = new ContentValues(); outbox.put("table_name", spec.table);
                        outbox.put("local_id", localId); outbox.put("sync_id", syncId);
                        outbox.put("operation", "UPSERT"); outbox.put("changed_at", System.currentTimeMillis());
                        db.insert("family_sync_outbox", 0, outbox);
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception error) {
            Log.w(TAG, "Existing records could not be queued.", error);
        } finally { db.endTransaction(); }
    }

    private static void cleanupOldChunks(DocumentReference record, String currentVersion) {
        record.collection("payload_chunks").get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot part : snapshot.getDocuments()) {
                if (!currentVersion.equals(string(part.getString("version"))))
                    part.getReference().delete();
            }
        });
    }

    private JSONObject readRecord(SupportSQLiteDatabase db, TableSpec spec, String localId)
            throws Exception {
        try (Cursor cursor = db.query("SELECT * FROM `" + spec.table + "` WHERE `"
                + spec.primaryKey + "`=? LIMIT 1", new Object[]{localId})) {
            if (!cursor.moveToFirst()) return null;
            JSONObject data = new JSONObject();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                String name = cursor.getColumnName(i);
                switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_NULL: data.put(name, JSONObject.NULL); break;
                    case Cursor.FIELD_TYPE_INTEGER: data.put(name, cursor.getLong(i)); break;
                    case Cursor.FIELD_TYPE_FLOAT: data.put(name, cursor.getDouble(i)); break;
                    case Cursor.FIELD_TYPE_BLOB:
                        data.put(name, "blob:" + Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP)); break;
                    default: data.put(name, cursor.getString(i));
                }
            }
            JSONObject payload = new JSONObject(); payload.put("data", data); return payload;
        }
    }

    private void addReference(SupportSQLiteDatabase db, TableSpec spec, JSONObject payload)
            throws Exception {
        if (spec.parentTable == null) return;
        JSONObject data = payload.getJSONObject("data");
        String parentLocal = data.optString(spec.parentColumn, "");
        String parentSync = syncIdForLocal(db, spec.parentTable, parentLocal);
        if (parentSync == null) throw new MissingParentException();
        payload.put("parent_sync_id", parentSync);
    }

    private static ContentValues jsonValues(JSONObject json) throws Exception {
        ContentValues values = new ContentValues();
        java.util.Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next(); Object value = json.get(key);
            if (value == JSONObject.NULL) values.putNull(key);
            else if (value instanceof Integer) values.put(key, (Integer)value);
            else if (value instanceof Long) values.put(key, (Long)value);
            else if (value instanceof Double) values.put(key, (Double)value);
            else if (value instanceof Boolean) values.put(key, (Boolean)value);
            else { String text = String.valueOf(value);
                if (text.startsWith("blob:")) values.put(key, Base64.decode(text.substring(5), Base64.NO_WRAP));
                else values.put(key, text); }
        }
        return values;
    }

    private SupportSQLiteDatabase database() {
        return StudySaathiDatabase.getInstance(context).getOpenHelper().getWritableDatabase();
    }
    private com.google.firebase.firestore.CollectionReference records(String familyId) {
        return firestore.collection("families").document(familyId).collection("realtime_records");
    }
    private static Map<String,Object> baseMetadata(TableSpec spec, String syncId, String uid) {
        Map<String,Object> value = new HashMap<>(); value.put("sync_id", syncId);
        value.put("table_name", spec.table); value.put("rank", spec.rank);
        value.put("updated_by_uid", uid); value.put("updated_at", FieldValue.serverTimestamp());
        value.put("format", "encrypted_record_v1"); return value;
    }
    private static List<String> split(String value) { List<String> out = new ArrayList<>();
        for (int i=0;i<value.length();i+=CHUNK_SIZE) out.add(value.substring(i,Math.min(value.length(),i+CHUNK_SIZE)));
        return out; }
    private static String compress(String clear) throws Exception { ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) { gzip.write(clear.getBytes(StandardCharsets.UTF_8)); }
        return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP); }
    private static String decompress(String encoded) throws Exception { byte[] compressed=Base64.decode(encoded,Base64.NO_WRAP);
        try (java.util.zip.GZIPInputStream input=new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(compressed)); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int read; while((read=input.read(buffer))!=-1) out.write(buffer,0,read);
            return out.toString(StandardCharsets.UTF_8.name()); } }
    private static String checksum(String value) throws Exception { byte[] hash=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(hash,Base64.NO_WRAP|Base64.URL_SAFE); }
    private static void setSuppressed(SupportSQLiteDatabase db, boolean value) { db.execSQL("UPDATE family_sync_runtime SET suppress_triggers=? WHERE singleton_id=1",new Object[]{value?1:0}); }
    private static void removeOutbox(SupportSQLiteDatabase db,String table,String syncId) { db.delete("family_sync_outbox","table_name=? AND sync_id=?",new Object[]{table,syncId}); }
    private static boolean mappingExists(SupportSQLiteDatabase db,String table,String local) { return syncIdForLocal(db,table,local)!=null; }
    private static boolean hasPendingOutbox(SupportSQLiteDatabase db,String table,String sync) { try(Cursor c=db.query("SELECT 1 FROM family_sync_outbox WHERE table_name=? AND sync_id=? LIMIT 1",new Object[]{table,sync})){return c.moveToFirst();} }
    @Nullable private static String syncIdForLocal(SupportSQLiteDatabase db,String table,String local) { try(Cursor c=db.query("SELECT sync_id FROM family_sync_record_map WHERE table_name=? AND local_id=? LIMIT 1",new Object[]{table,local})){return c.moveToFirst()?c.getString(0):null;} }
    @Nullable private static String localIdForSync(SupportSQLiteDatabase db,String table,String sync) { if(sync==null||sync.isEmpty())return null; try(Cursor c=db.query("SELECT local_id FROM family_sync_record_map WHERE table_name=? AND sync_id=? LIMIT 1",new Object[]{table,sync})){return c.moveToFirst()?c.getString(0):null;} }
    private static void putTypedId(ContentValues values,String key,String id) { try { values.put(key,Long.parseLong(id)); } catch(NumberFormatException error){ values.put(key,id); } }
    private static String valueAsString(Object value){return value==null?"":String.valueOf(value);}
    private static int intValue(Long value){return value==null?0:value.intValue();}
    private static String string(String value){return value==null?"":value;}
    @Nullable private static TableSpec spec(String table){for(TableSpec spec:TABLES)if(spec.table.equals(table))return spec;return null;}

    private static final class TableSpec {
        final String table, primaryKey, parentTable, parentColumn; final int rank;
        TableSpec(String table,String primaryKey,int rank,String parentTable,String parentColumn){this.table=table;this.primaryKey=primaryKey;this.rank=rank;this.parentTable=parentTable;this.parentColumn=parentColumn;}
    }
    private static final class MissingParentException extends Exception { }
}
