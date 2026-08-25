package com.tridev.studysaathi.family;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Additive family membership layer. Existing users/{uid} data remains untouched. */
public final class FamilyWorkspaceRepository {
    public static final String ROLE_OWNER_PARENT = "OWNER_PARENT";
    public static final String ROLE_PARENT = "PARENT";

    private final Context context;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public FamilyWorkspaceRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void create(@NonNull String requestedName, @NonNull Callback callback) {
        FirebaseUser user = verifiedUser(callback); if (user == null) return;
        String name = requestedName.trim();
        if (name.length() < 2) { callback.onError(new IllegalArgumentException(
                "Family name कम से कम 2 अक्षर का रखें।")); return; }
        String familyId = UUID.randomUUID().toString();
        String code = generateInviteCode();
        try {
            String hash = FamilyWorkspaceCrypto.inviteHash(familyId, code);
            DocumentReference family = firestore.collection("families").document(familyId);
            DocumentReference member = family.collection("members").document(user.getUid());
            DocumentReference invite = firestore.collection("family_invites").document(code);
            DocumentReference link = firestore.collection("users").document(user.getUid())
                    .collection("family_memberships").document(familyId);
            Map<String,Object> familyData = new HashMap<>();
            familyData.put("family_id", familyId); familyData.put("name", name);
            familyData.put("owner_uid", user.getUid()); familyData.put("invite_hash", hash);
            familyData.put("schema_version", 1); familyData.put("created_at", FieldValue.serverTimestamp());
            familyData.put("updated_at", FieldValue.serverTimestamp());
            Map<String,Object> memberData = memberData(user, ROLE_OWNER_PARENT, code);
            Map<String,Object> inviteData = new HashMap<>();
            inviteData.put("family_id", familyId); inviteData.put("invite_hash", hash);
            inviteData.put("family_name", name);
            inviteData.put("active", true); inviteData.put("created_at", FieldValue.serverTimestamp());
            Map<String,Object> linkData = membershipData(familyId, name, ROLE_OWNER_PARENT);
            WriteBatch batch = firestore.batch();
            batch.set(family, familyData); batch.set(member, memberData);
            batch.set(invite, inviteData); batch.set(link, linkData);
            batch.commit().addOnSuccessListener(v -> {
                FamilyWorkspaceSession.save(context, familyId, name, ROLE_OWNER_PARENT, code);
                callback.onSuccess(new Result(familyId, name, ROLE_OWNER_PARENT, code));
            }).addOnFailureListener(callback::onError);
        } catch (Exception error) { callback.onError(error); }
    }

    public void join(@NonNull String enteredCode, @NonNull Callback callback) {
        FirebaseUser user = verifiedUser(callback); if (user == null) return;
        String code = FamilyWorkspaceCrypto.normalizeCode(enteredCode);
        if (code.length() != 10) { callback.onError(new IllegalArgumentException(
                "10-character family invite code दर्ज करें।")); return; }
        firestore.collection("family_invites").document(code).get()
                .addOnSuccessListener(invite -> {
                    if (!invite.exists() || !Boolean.TRUE.equals(invite.getBoolean("active"))) {
                        callback.onError(new IllegalArgumentException("Invite code मान्य नहीं है।")); return;
                    }
                    String familyId = safe(invite.getString("family_id"));
                    String expectedHash = safe(invite.getString("invite_hash"));
                    try {
                        if (!expectedHash.equals(FamilyWorkspaceCrypto.inviteHash(familyId, code))) {
                            callback.onError(new SecurityException("Invite verification failed.")); return;
                        }
                    } catch (Exception error) { callback.onError(error); return; }
                    String name = safe(invite.getString("family_name"));
                    DocumentReference family = firestore.collection("families").document(familyId);
                    DocumentReference member = family.collection("members").document(user.getUid());
                                DocumentReference link = firestore.collection("users")
                                        .document(user.getUid()).collection("family_memberships")
                                        .document(familyId);
                                WriteBatch batch = firestore.batch();
                                batch.set(member, memberData(user, ROLE_PARENT, code), SetOptions.merge());
                                batch.set(link, membershipData(familyId, name, ROLE_PARENT), SetOptions.merge());
                                batch.commit().addOnSuccessListener(v -> {
                                    FamilyWorkspaceSession.save(context, familyId, name, ROLE_PARENT, code);
                                    callback.onSuccess(new Result(familyId, name, ROLE_PARENT, code));
                                }).addOnFailureListener(callback::onError);
                }).addOnFailureListener(callback::onError);
    }

    public void refresh(@NonNull Callback callback) {
        FamilyWorkspaceSession.State state = FamilyWorkspaceSession.load(context);
        FirebaseUser user = verifiedUser(callback);
        if (user == null) return;
        if (!state.isActive()) { callback.onError(new IllegalStateException(
                "इस device पर कोई Family Workspace active नहीं है।")); return; }
        firestore.collection("families").document(state.familyId).collection("members")
                .document(user.getUid()).get().addOnSuccessListener(member -> {
                    if (!member.exists()) { FamilyWorkspaceSession.clear(context);
                        callback.onError(new SecurityException("Family membership उपलब्ध नहीं है।")); return; }
                    callback.onSuccess(new Result(state.familyId, state.familyName,
                            safe(member.getString("role")), state.inviteCode));
                }).addOnFailureListener(callback::onError);
    }

    private FirebaseUser verifiedUser(Callback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            callback.onError(new SecurityException("Verified parent cloud account से sign in करें।"));
            return null;
        }
        return user;
    }

    private static Map<String,Object> memberData(FirebaseUser user, String role, String inviteCode) {
        Map<String,Object> data = new HashMap<>(); data.put("uid", user.getUid());
        data.put("email", safe(user.getEmail())); data.put("display_name", safe(user.getDisplayName()));
        data.put("role", role); data.put("active", true);
        data.put("invite_code", inviteCode);
        data.put("joined_at", FieldValue.serverTimestamp()); data.put("updated_at", FieldValue.serverTimestamp());
        return data;
    }

    private static Map<String,Object> membershipData(String familyId, String name, String role) {
        Map<String,Object> data = new HashMap<>(); data.put("family_id", familyId);
        data.put("family_name", name); data.put("role", role); data.put("active", true);
        data.put("updated_at", FieldValue.serverTimestamp()); return data;
    }

    private static String generateInviteCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom(); StringBuilder value = new StringBuilder(10);
        for (int i=0;i<10;i++) value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return value.toString().toUpperCase(Locale.US);
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }

    public interface Callback { void onSuccess(@NonNull Result result); void onError(@NonNull Exception error); }
    public static final class Result {
        public final String familyId, familyName, role, inviteCode;
        Result(String id, String name, String role, String code) {
            familyId=id; familyName=name; this.role=role; inviteCode=code;
        }
    }
}
