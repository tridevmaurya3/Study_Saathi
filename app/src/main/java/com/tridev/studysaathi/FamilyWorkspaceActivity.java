package com.tridev.studysaathi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.databinding.ActivityFamilyWorkspaceBinding;
import com.tridev.studysaathi.family.FamilySnapshotSyncRepository;
import com.tridev.studysaathi.family.FamilyRealtimeSyncManager;
import com.tridev.studysaathi.family.FamilyWorkspaceRepository;
import com.tridev.studysaathi.family.FamilyWorkspaceSession;

/** Parent-facing create/join and encrypted full-app family sync screen. */
public final class FamilyWorkspaceActivity extends AppCompatActivity {
    private ActivityFamilyWorkspaceBinding binding;
    private FamilyWorkspaceRepository workspaceRepository;
    private FamilySnapshotSyncRepository syncRepository;

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
                runOnUiThread(() -> { setBusy(false); renderSession(); message("Family Workspace तैयार है—realtime sync चालू है।"); });
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
        binding.buttonUploadFamilyData.setEnabled(active); binding.buttonDownloadFamilyData.setEnabled(active);
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
