package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tridev.studysaathi.databinding.ActivityUserModeSelectionBinding;

public final class UserModeSelectionActivity extends AppCompatActivity {

    private ActivityUserModeSelectionBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            openAuthentication();
            return;
        }

        binding = ActivityUserModeSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.textSignedInAccount.setText(
                user.getEmail() == null ? "Verified account" : user.getEmail()
        );
        binding.buttonStudentMode.setOnClickListener(view ->
                openAsRoot(MainActivity.class)
        );
        binding.buttonParentMode.setOnClickListener(view ->
                openAsRoot(ParentDashboardActivity.class)
        );
        binding.buttonSignOut.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            openAuthentication();
        });
    }

    private void openAuthentication() {
        Intent intent = new Intent(this, CloudAccountActivity.class);
        intent.putExtra(
                CloudAccountActivity.EXTRA_REQUIRE_AUTHENTICATION,
                true
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void openAsRoot(Class<?> destination) {
        Intent intent = new Intent(this, destination);
        if (destination == MainActivity.class) {
            intent.putExtra(MainActivity.EXTRA_AUTH_GATE_PASSED, true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
