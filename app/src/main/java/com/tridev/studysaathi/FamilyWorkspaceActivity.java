package com.tridev.studysaathi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.tridev.studysaathi.databinding.ActivityFamilyWorkspaceBinding;
import com.tridev.studysaathi.family.FamilySnapshotSyncRepository;
import com.tridev.studysaathi.family.FamilyRealtimeSyncManager;
import com.tridev.studysaathi.family.FamilyWorkspaceRepository;
import com.tridev.studysaathi.family.FamilyWorkspaceSession;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Parent-facing create/join and encrypted full-app family sync screen. */
public final class FamilyWorkspaceActivity extends AppCompatActivity {
    private ActivityFamilyWorkspaceBinding binding;
    private FamilyWorkspaceRepository workspaceRepository;
    private FamilySnapshotSyncRepository syncRepository;
    private ListenerRegistration memberRegistration;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); binding = ActivityFamilyWorkspaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        workspaceRepository = new FamilyWorkspaceRepository(this);
        syncRepository = new FamilySnapshotSyncRepository(this);
        binding.buttonFamilyBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.buttonCreateFamily.setOnClickListener(v -> createFamily());
        binding.buttonJoinFamily.setOnClickListener(v -> joinFamily());
        binding.buttonCopyInvite.setOnClickListener(v -> copyInvite());
        binding.buttonUploadFamilyData.setOnClickListener(v -> upload());
        binding.buttonDownloadFamilyData.setOnClickListener(v -> download());
        renderSession();
    }

    @Override protected void onStart() {
        super.onStart();
        startMemberObserver();
    }

    @Override protected void onStop() {
        stopMemberObserver();
        super.onStop();
    }

    private void createFamily() {
        setBusy(true); workspaceRepository.create(text(binding.inputFamilyName), workspaceCallback());
    }
    private void joinFamily() {
        setBusy(true); workspaceRepository.join(text(binding.inputFamilyInviteCode), workspaceCallback());
    }
    private FamilyWorkspaceRepository.Callback workspaceCallback() {
        return new FamilyWorkspaceRepository.Callback() {
            @Override public void onSuccess(@NonNull FamilyWorkspaceRepository.Result result) {
                FamilyRealtimeSyncManager.refresh(FamilyWorkspaceActivity.this);
                runOnUiThread(() -> { setBusy(false); renderSession(); startMemberObserver(); message("Family Workspace तैयार है—realtime sync चालू है।"); });
            }
            @Override public void onError(@NonNull Exception error) { showError(error); }
        };
    }
    private void upload() {
        setBusy(true); syncRepository.upload(new FamilySnapshotSyncRepository.Callback() {
            @Override public void onSuccess(@NonNull String value) { runOnUiThread(() -> { setBusy(false); message(value); }); }
            @Override public void onError(@NonNull Exception error) { showError(error); }
        });
    }
    private void download() {
        setBusy(true); syncRepository.download(new FamilySnapshotSyncRepository.DownloadCallback() {
            @Override public void onDownloaded(@NonNull java.io.File file, @NonNull String displayName) {
                runOnUiThread(() -> { setBusy(false); Intent restore = new Intent(FamilyWorkspaceActivity.this, BackupRestoreActivity.class);
                    restore.putExtra(BackupRestoreActivity.EXTRA_INTERNAL_BACKUP_PATH, file.getAbsolutePath());
                    restore.putExtra(BackupRestoreActivity.EXTRA_INTERNAL_BACKUP_DISPLAY_NAME, displayName);
                    startActivity(restore); });
            }
            @Override public void onError(@NonNull Exception error) { showError(error); }
        });
    }
    private void renderSession() {
        FamilyWorkspaceSession.State state = FamilyWorkspaceSession.load(this);
        boolean active = state.isActive(); binding.cardActiveFamily.setVisibility(active ? View.VISIBLE : View.GONE);
        binding.cardFamilySetup.setVisibility(active ? View.GONE : View.VISIBLE);
        binding.textFamilyName.setText(active ? state.familyName : "");
        binding.textFamilyRole.setText(active ? roleLabel(state.role) : "");
        binding.textFamilyInvite.setText(active ? state.inviteCode : "");
        binding.cardFamilyMembers.setVisibility(active ? View.VISIBLE : View.GONE);
        binding.buttonUploadFamilyData.setEnabled(active); binding.buttonDownloadFamilyData.setEnabled(active);
    }

    private void startMemberObserver() {
        stopMemberObserver();
        if (!FamilyWorkspaceSession.load(this).isActive()) return;
        binding.textFamilyMembersStatus.setText("Members load हो रहे हैं…");
        memberRegistration = workspaceRepository.observeMembers(
                new FamilyWorkspaceRepository.MemberCallback() {
                    @Override public void onChanged(
                            @NonNull List<FamilyWorkspaceRepository.Member> members) {
                        runOnUiThread(() -> renderMembers(members));
                    }

                    @Override public void onError(@NonNull Exception error) {
                        runOnUiThread(() -> binding.textFamilyMembersStatus.setText(
                                "Members load नहीं हुए। Page दोबारा खोलें।"));
                    }
                });
    }

    private void stopMemberObserver() {
        if (memberRegistration != null) {
            memberRegistration.remove();
            memberRegistration = null;
        }
    }

    private void renderMembers(
            @NonNull List<FamilyWorkspaceRepository.Member> members) {
        binding.containerFamilyMembers.removeAllViews();
        binding.textFamilyMemberCount.setText(members.size()
                + (members.size() == 1 ? " Member" : " Members"));
        binding.textFamilyMembersStatus.setText(members.isEmpty()
                ? "अभी कोई member उपलब्ध नहीं है।"
                : "नया member जुड़ते ही यह list अपने-आप update होगी।");
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid();
        for (FamilyWorkspaceRepository.Member member : members) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(7);
            card.setLayoutParams(cardParams);
            card.setRadius(dp(14));
            card.setCardElevation(0f);
            card.setCardBackgroundColor(getColor(R.color.ss_surface_soft));
            card.setStrokeColor(getColor(R.color.ss_outline));
            card.setStrokeWidth(dp(1));

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView name = memberText(14, R.color.ss_text_primary, true);
            String displayName = member.displayName.isEmpty()
                    ? "Family Member" : member.displayName;
            name.setText(displayName
                    + (member.uid.equals(currentUid) ? " (You)" : ""));

            TextView email = memberText(12, R.color.ss_text_secondary, false);
            email.setText(member.email.isEmpty()
                    ? "Email unavailable" : member.email);

            TextView details = memberText(11,
                    member.active ? R.color.ss_success : R.color.ss_text_muted,
                    true);
            details.setText(roleLabel(member.role) + "   •   "
                    + (member.active ? "Active" : "Inactive"));

            TextView joined = memberText(10, R.color.ss_text_muted, false);
            joined.setText(member.joinedAt <= 0L ? "Joining date pending"
                    : "Joined " + DateTimeFormatter.ofPattern(
                            "dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(member.joinedAt)));

            content.addView(name);
            content.addView(email);
            content.addView(details);
            content.addView(joined);
            card.addView(content);
            binding.containerFamilyMembers.addView(card);
        }
    }

    private TextView memberText(int sizeSp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setTextSize(sizeSp);
        text.setTextColor(getColor(color));
        if (bold) text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
        return text;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void copyInvite() {
        String code = FamilyWorkspaceSession.load(this).inviteCode; if (code.isEmpty()) return;
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText("Study Saathi Family Invite", code));
        Toast.makeText(this, "Invite code copy हो गया।", Toast.LENGTH_SHORT).show();
    }
    private void setBusy(boolean busy) {
        binding.familyProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.buttonCreateFamily.setEnabled(!busy); binding.buttonJoinFamily.setEnabled(!busy);
        binding.buttonUploadFamilyData.setEnabled(!busy); binding.buttonDownloadFamilyData.setEnabled(!busy);
    }
    private void showError(Exception error) { runOnUiThread(() -> { setBusy(false); message(error.getMessage() == null ? "Operation पूरी नहीं हुई।" : error.getMessage()); }); }
    private void message(String value) { Snackbar.make(binding.getRoot(), value, Snackbar.LENGTH_LONG).show(); }
    private static String text(android.widget.TextView view) { return view.getText() == null ? "" : view.getText().toString().trim(); }
    private static String roleLabel(String role) { return FamilyWorkspaceRepository.ROLE_OWNER_PARENT.equals(role) ? "Owner Parent" : "Parent"; }
}
