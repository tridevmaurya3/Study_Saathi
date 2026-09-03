package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CustomCredential;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.tridev.studysaathi.cloud.CloudBackupPayloadBuilder;
import com.tridev.studysaathi.cloud.CloudBackupRestoreCoordinator;
import com.tridev.studysaathi.cloud.CloudBackupSecurityGuard;
import com.tridev.studysaathi.cloud.CloudBackupUploader;
import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.databinding.ActivityCloudAccountBinding;
import com.tridev.studysaathi.data.repository.LocalAccountDataRepository;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CloudAccountActivity
        extends AppCompatActivity {

    public static final String EXTRA_REQUIRE_AUTHENTICATION =
            "extra_require_authentication";

    private static final String CLOUD_STATE_PREFERENCES =
            "study_saathi_cloud_state";

    private static final String KEY_LAST_CLOUD_UPLOAD_AT =
            "last_cloud_upload_at";

    private static final String KEY_LAST_CLOUD_BACKUP_ID =
            "last_cloud_backup_id";

    private enum AccountFormMode {
        SIGN_IN,
        CREATE_ACCOUNT
    }

    private ActivityCloudAccountBinding binding;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private CredentialManager credentialManager;

    private CloudBackupUploader cloudBackupUploader;

    private CloudBackupRestoreCoordinator
            cloudBackupRestoreCoordinator;

    private CloudBackupSecurityGuard
            cloudBackupSecurityGuard;

    private CloudBackupUploader.CloudBackupMetadata
            currentCloudBackupMetadata;

    private AccountFormMode formMode =
            AccountFormMode.SIGN_IN;

    private boolean operationInProgress;
    private boolean cloudBackupOperationInProgress;
    private boolean updatingModeSelection;
    private boolean openingPreparedCloudRestore;
    private boolean activityInForeground;
    private boolean authenticationGate;
    private boolean openingUserMode;

    @NonNull
    private String observedFirebaseUserId =
            "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityCloudAccountBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(binding.getRoot());

        authenticationGate = getIntent().getBooleanExtra(
                EXTRA_REQUIRE_AUTHENTICATION,
                false
        );
        if (authenticationGate) {
            binding.buttonBack.setVisibility(View.GONE);
            getOnBackPressedDispatcher().addCallback(
                    this,
                    new OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            moveTaskToBack(true);
                        }
                    }
            );
        }

        firebaseAuth =
                FirebaseAuth.getInstance();
        credentialManager =
                CredentialManager.create(this);

        firestore =
                FirebaseFirestore.getInstance();

        cloudBackupUploader =
                new CloudBackupUploader();

        cloudBackupRestoreCoordinator =
                new CloudBackupRestoreCoordinator(this);

        cloudBackupSecurityGuard =
                new CloudBackupSecurityGuard();

        observedFirebaseUserId =
                getCurrentFirebaseUserId();

        setupClickListeners();
        setupModeSelection();

        setFormMode(
                AccountFormMode.SIGN_IN
        );

        showCurrentAccountState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        activityInForeground =
                true;

        openingPreparedCloudRestore =
                false;

        handlePossibleAccountChange();
        showCurrentAccountState();
        loadCloudBackupMetadataIfAvailable();
        continueAfterVerifiedAuthentication();
    }

    @Override
    protected void onPause() {
        activityInForeground =
                false;

        if (cloudBackupSecurityGuard != null) {
            cloudBackupSecurityGuard
                    .clearPassphraseMemory();
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (cloudBackupSecurityGuard != null
                && !isChangingConfigurations()
                && !openingPreparedCloudRestore) {

            cloudBackupSecurityGuard
                    .clearForAppBackground(this);
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (cloudBackupSecurityGuard != null) {
            cloudBackupSecurityGuard
                    .clearPassphraseMemory();

            if (isFinishing()
                    && !openingPreparedCloudRestore) {

                cloudBackupSecurityGuard
                        .clearForActivityClosed(this);
            }
        }

        super.onDestroy();
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher()
                        .onBackPressed()
        );

        binding.buttonCloudHelp.setOnClickListener(view -> {
            Intent helpIntent = new Intent(this, HelpAboutActivity.class);
            helpIntent.putExtra(
                    HelpAboutActivity.EXTRA_MODE,
                    HelpAboutActivity.MODE_AUTHENTICATION
            );
            startActivity(helpIntent);
        });

        binding.buttonCloudPrimaryAction
                .setOnClickListener(view ->
                        submitAccountForm()
                );

        binding.buttonCloudForgotPassword
                .setOnClickListener(view ->
                        sendPasswordResetEmail()
                );

        binding.buttonCloudResendVerification
                .setOnClickListener(view ->
                        resendVerificationEmail()
                );

        binding.buttonCloudRefreshAccount
                .setOnClickListener(view ->
                        refreshAccountStatus()
                );

        binding.buttonCloudSignOut
                .setOnClickListener(view ->
                        signOutAccount()
                );

        binding.buttonCloudUploadBackup
                .setOnClickListener(view ->
                        requestCloudBackupUpload()
                );

        binding.buttonCloudRestoreBackup
                .setOnClickListener(view ->
                        requestCloudBackupRestore()
                );

        binding.buttonCloudRefreshBackup
                .setOnClickListener(view ->
                        loadCloudBackupMetadataIfAvailable()
                );

        binding.buttonGoogleAccount.setOnClickListener(view ->
                startGoogleAccountFlow()
        );

        binding.buttonDeleteCloudBackup.setOnClickListener(view ->
                requestCloudBackupDeletion()
        );

        binding.buttonDeleteAccount.setOnClickListener(view ->
                requestPermanentAccountDeletion()
        );
    }

    private void setupModeSelection() {
        binding.toggleCloudAccountMode
                .addOnButtonCheckedListener(
                        (group, checkedId, isChecked) -> {
                            if (!isChecked
                                    || updatingModeSelection) {
                                return;
                            }

                            if (checkedId
                                    == R.id.buttonCloudCreateMode) {

                                setFormMode(
                                        AccountFormMode
                                                .CREATE_ACCOUNT
                                );

                            } else if (checkedId
                                    == R.id.buttonCloudSignInMode) {

                                setFormMode(
                                        AccountFormMode.SIGN_IN
                                );
                            }
                        }
                );
    }

    private void setFormMode(
            @NonNull AccountFormMode selectedMode
    ) {
        formMode = selectedMode;

        updatingModeSelection = true;

        binding.toggleCloudAccountMode.check(
                selectedMode
                        == AccountFormMode.CREATE_ACCOUNT
                        ? R.id.buttonCloudCreateMode
                        : R.id.buttonCloudSignInMode
        );

        updatingModeSelection = false;

        boolean creatingAccount =
                selectedMode
                        == AccountFormMode.CREATE_ACCOUNT;

        binding.layoutCloudDisplayName.setVisibility(
                creatingAccount
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.layoutCloudConfirmPassword.setVisibility(
                creatingAccount
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.buttonCloudForgotPassword.setVisibility(
                creatingAccount
                        ? View.GONE
                        : View.VISIBLE
        );

        binding.buttonCloudPrimaryAction.setText(
                creatingAccount
                        ? R.string.cloud_create_account_action
                        : R.string.cloud_sign_in_action
        );

        binding.textCloudFormTitle.setText(
                creatingAccount
                        ? R.string.cloud_create_account_title
                        : R.string.cloud_sign_in_title
        );

        binding.textCloudFormDescription.setText(
                creatingAccount
                        ? R.string.cloud_create_account_description
                        : R.string.cloud_sign_in_description
        );

        clearInputErrors();
    }
    private void submitAccountForm() {
        if (operationInProgress
                || cloudBackupOperationInProgress) {
            return;
        }

        clearInputErrors();
        hideKeyboard();

        String email =
                getInputText(
                        binding.inputCloudEmail.getText()
                );

        String password =
                getInputText(
                        binding.inputCloudPassword.getText()
                );

        if (!validateEmail(email)) {
            return;
        }

        if (!validatePassword(password)) {
            return;
        }

        if (formMode
                == AccountFormMode.CREATE_ACCOUNT) {

            String displayName =
                    getInputText(
                            binding.inputCloudDisplayName
                                    .getText()
                    );

            String confirmPassword =
                    getInputText(
                            binding.inputCloudConfirmPassword
                                    .getText()
                    );

            if (displayName.length() < 2) {
                binding.layoutCloudDisplayName.setError(
                        getString(
                                R.string.cloud_name_required
                        )
                );

                binding.inputCloudDisplayName
                        .requestFocus();

                return;
            }

            if (!password.equals(
                    confirmPassword
            )) {
                binding.layoutCloudConfirmPassword.setError(
                        getString(
                                R.string.cloud_password_mismatch
                        )
                );

                binding.inputCloudConfirmPassword
                        .requestFocus();

                return;
            }

            createCloudAccount(
                    displayName,
                    email,
                    password
            );

        } else {
            signInToCloudAccount(
                    email,
                    password
            );
        }
    }

    private boolean validateEmail(
            @NonNull String email
    ) {
        if (email.isEmpty()
                || !Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            binding.layoutCloudEmail.setError(
                    getString(
                            R.string.cloud_valid_email_required
                    )
            );

            binding.inputCloudEmail.requestFocus();

            return false;
        }

        return true;
    }

    private boolean validatePassword(
            @NonNull String password
    ) {
        if (password.length() < 8) {
            binding.layoutCloudPassword.setError(
                    getString(
                            R.string.cloud_password_length_required
                    )
            );

            binding.inputCloudPassword.requestFocus();

            return false;
        }

        return true;
    }

    private void createCloudAccount(
            @NonNull String displayName,
            @NonNull String email,
            @NonNull String password
    ) {
        showOperationState(
                true,
                R.string.cloud_creating_account
        );

        firebaseAuth
                .createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        showOperationState(
                                false,
                                R.string.cloud_create_account_action
                        );

                        showFirebaseError(
                                task.getException(),
                                R.string.cloud_account_creation_failed
                        );

                        return;
                    }

                    FirebaseUser firebaseUser =
                            firebaseAuth.getCurrentUser();

                    if (firebaseUser == null) {
                        showOperationState(
                                false,
                                R.string.cloud_create_account_action
                        );

                        showMessage(
                                R.string.cloud_account_creation_failed
                        );

                        return;
                    }

                    prepareCleanDeviceForNewAccount(
                            firebaseUser,
                            displayName
                    );
                });
    }

    private void startGoogleAccountFlow() {
        if (operationInProgress || cloudBackupOperationInProgress) {
            return;
        }
        int clientIdResource = getResources().getIdentifier(
                "default_web_client_id",
                "string",
                getPackageName()
        );
        if (clientIdResource == 0) {
            Snackbar.make(
                    binding.getRoot(),
                    "Google sign-in setup pending: Firebase Console में Google provider, SHA-1/SHA-256 और updated google-services.json जोड़ें।",
                    Snackbar.LENGTH_INDEFINITE
            ).show();
            return;
        }
        String serverClientId = getString(clientIdResource).trim();
        if (serverClientId.isEmpty()) {
            Snackbar.make(
                    binding.getRoot(),
                    "Google OAuth web client ID उपलब्ध नहीं है।",
                    Snackbar.LENGTH_LONG
            ).show();
            return;
        }

        showOperationState(true, R.string.cloud_signing_in);
        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(serverClientId)
                        .build();
        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleOption)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new androidx.credentials.CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleGoogleCredential(response.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException error) {
                        showOperationState(
                                false,
                                formMode == AccountFormMode.CREATE_ACCOUNT
                                        ? R.string.cloud_create_account_action
                                        : R.string.cloud_sign_in_action
                        );
                        Snackbar.make(
                                binding.getRoot(),
                                "Google account चयन पूरा नहीं हुआ। दोबारा कोशिश करें।",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void handleGoogleCredential(@NonNull Credential credential) {
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(credential.getType())) {
            showOperationState(false, R.string.cloud_sign_in_action);
            showMessage(R.string.cloud_account_creation_failed);
            return;
        }
        try {
            CustomCredential customCredential =
                    (CustomCredential) credential;
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(
                            customCredential.getData()
                    );
            AuthCredential firebaseCredential =
                    GoogleAuthProvider.getCredential(
                            googleCredential.getIdToken(),
                            null
                    );
            firebaseAuth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this, task -> {
                        if (!task.isSuccessful()
                                || task.getResult() == null
                                || task.getResult().getUser() == null) {
                            showOperationState(false, R.string.cloud_sign_in_action);
                            showFirebaseError(
                                    task.getException(),
                                    R.string.cloud_sign_in_failed
                            );
                            return;
                        }
                        FirebaseUser user = task.getResult().getUser();
                        boolean newAccount =
                                task.getResult().getAdditionalUserInfo() != null
                                        && task.getResult()
                                        .getAdditionalUserInfo()
                                        .isNewUser();
                        String displayName = getSafeDisplayName(user);
                        if (newAccount) {
                            prepareCleanGoogleAccount(
                                    user,
                                    displayName
                            );
                        } else {
                            saveCloudUserDocument(
                                    user,
                                    displayName,
                                    false,
                                    false
                            );
                        }
                    });
        } catch (RuntimeException exception) {
            showOperationState(false, R.string.cloud_sign_in_action);
            Snackbar.make(
                    binding.getRoot(),
                    "Google credential सुरक्षित रूप से पढ़ा नहीं जा सका।",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private void prepareCleanGoogleAccount(
            @NonNull FirebaseUser user,
            @NonNull String displayName
    ) {
        new LocalAccountDataRepository(this).permanentlyDeleteAll(
                new LocalAccountDataRepository.Callback() {
                    @Override
                    public void onSuccess() {
                        saveCloudUserDocument(
                                user,
                                displayName,
                                true,
                                false
                        );
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        firebaseAuth.signOut();
                        showOperationState(
                                false,
                                R.string.cloud_create_account_action
                        );
                        Snackbar.make(
                                binding.getRoot(),
                                "पुराना device data साफ नहीं हुआ, इसलिए नया Google account शुरू नहीं किया गया।",
                                Snackbar.LENGTH_INDEFINITE
                        ).show();
                    }
                }
        );
    }

    private void prepareCleanDeviceForNewAccount(
            @NonNull FirebaseUser firebaseUser,
            @NonNull String displayName
    ) {
        new LocalAccountDataRepository(this).permanentlyDeleteAll(
                new LocalAccountDataRepository.Callback() {
                    @Override
                    public void onSuccess() {
                        updateNewUserProfile(
                                firebaseUser,
                                displayName
                        );
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        firebaseAuth.signOut();
                        showOperationState(
                                false,
                                R.string.cloud_create_account_action
                        );
                        Snackbar.make(
                                binding.getRoot(),
                                "New account सुरक्षित रखने के लिए पुराने device data को साफ नहीं किया जा सका। दोबारा कोशिश करें।",
                                Snackbar.LENGTH_INDEFINITE
                        ).show();
                    }
                }
        );
    }

    private void updateNewUserProfile(
            @NonNull FirebaseUser firebaseUser,
            @NonNull String displayName
    ) {
        UserProfileChangeRequest profileChangeRequest =
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build();

        firebaseUser
                .updateProfile(profileChangeRequest)
                .addOnCompleteListener(profileTask -> {
                    if (!profileTask.isSuccessful()) {
                        clearSensitiveStateForAccountChange();
                        firebaseAuth.signOut();

                        observedFirebaseUserId =
                                "";

                        showOperationState(
                                false,
                                R.string.cloud_create_account_action
                        );

                        showFirebaseError(
                                profileTask.getException(),
                                R.string.cloud_profile_creation_failed
                        );

                        return;
                    }

                    sendInitialVerificationEmail(
                            firebaseUser,
                            displayName
                    );
                });
    }

    private void sendInitialVerificationEmail(
            @NonNull FirebaseUser firebaseUser,
            @NonNull String displayName
    ) {
        firebaseUser
                .sendEmailVerification()
                .addOnCompleteListener(
                        verificationTask ->
                                saveCloudUserDocument(
                                        firebaseUser,
                                        displayName,
                                        true,
                                        verificationTask
                                                .isSuccessful()
                                )
                );
    }

    private void signInToCloudAccount(
            @NonNull String email,
            @NonNull String password
    ) {
        showOperationState(
                true,
                R.string.cloud_signing_in
        );

        firebaseAuth
                .signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        showOperationState(
                                false,
                                R.string.cloud_sign_in_action
                        );

                        showFirebaseError(
                                task.getException(),
                                R.string.cloud_sign_in_failed
                        );

                        return;
                    }

                    FirebaseUser firebaseUser =
                            firebaseAuth.getCurrentUser();

                    if (firebaseUser == null) {
                        showOperationState(
                                false,
                                R.string.cloud_sign_in_action
                        );

                        showMessage(
                                R.string.cloud_sign_in_failed
                        );

                        return;
                    }

                    String displayName =
                            getSafeDisplayName(
                                    firebaseUser
                            );

                    saveCloudUserDocument(
                            firebaseUser,
                            displayName,
                            false,
                            false
                    );
                });
    }

    private void saveCloudUserDocument(
            @NonNull FirebaseUser firebaseUser,
            @NonNull String displayName,
            boolean newAccount,
            boolean verificationEmailSent
    ) {
        Map<String, Object> userData =
                new HashMap<>();

        userData.put(
                "uid",
                firebaseUser.getUid()
        );

        userData.put(
                "email",
                getSafeEmail(firebaseUser)
        );

        userData.put(
                "display_name",
                displayName
        );

        userData.put(
                "email_verified",
                firebaseUser.isEmailVerified()
        );

        userData.put(
                "updated_at",
                FieldValue.serverTimestamp()
        );

        userData.put(
                "app_package",
                getPackageName()
        );

        if (newAccount) {
            userData.put(
                    "created_at",
                    FieldValue.serverTimestamp()
            );
        }

        firestore
                .collection("users")
                .document(firebaseUser.getUid())
                .set(
                        userData,
                        SetOptions.merge()
                )
                .addOnCompleteListener(documentTask -> {
                    showOperationState(
                            false,
                            formMode
                                    == AccountFormMode.CREATE_ACCOUNT
                                    ? R.string.cloud_create_account_action
                                    : R.string.cloud_sign_in_action
                    );

                    showCurrentAccountState();
                    clearPasswordInputs();

                    if (newAccount) {
                        if (verificationEmailSent) {
                            showMessage(
                                    R.string.cloud_account_created_verify_email
                            );
                        } else {
                            showMessage(
                                    R.string.cloud_account_created_verification_failed
                            );
                        }

                    } else {
                        showMessage(
                                R.string.cloud_sign_in_success
                        );
                    }

                    if (!documentTask.isSuccessful()) {
                        showMessage(
                                R.string.cloud_profile_document_warning
                        );
                    }

                    loadCloudBackupMetadataIfAvailable();
                    continueAfterVerifiedAuthentication();
                });
    }

    private void sendPasswordResetEmail() {
        if (operationInProgress
                || cloudBackupOperationInProgress) {
            return;
        }

        clearInputErrors();
        hideKeyboard();

        String email =
                getInputText(
                        binding.inputCloudEmail.getText()
                );

        if (!validateEmail(email)) {
            return;
        }

        showOperationState(
                true,
                R.string.cloud_sending_reset_email
        );

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showOperationState(
                            false,
                            R.string.cloud_sign_in_action
                    );

                    if (task.isSuccessful()) {
                        showMessage(
                                R.string.cloud_reset_email_sent
                        );
                    } else {
                        showFirebaseError(
                                task.getException(),
                                R.string.cloud_reset_email_failed
                        );
                    }
                });
    }

    private void resendVerificationEmail() {
        if (operationInProgress
                || cloudBackupOperationInProgress) {
            return;
        }

        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            showMessage(
                    R.string.cloud_account_required
            );

            showCurrentAccountState();

            return;
        }

        if (firebaseUser.isEmailVerified()) {
            showMessage(
                    R.string.cloud_email_already_verified
            );

            return;
        }

        showSignedInOperationState(true);

        firebaseUser
                .sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showSignedInOperationState(false);

                    if (task.isSuccessful()) {
                        showMessage(
                                R.string.cloud_verification_email_sent
                        );
                    } else {
                        showFirebaseError(
                                task.getException(),
                                R.string.cloud_verification_email_failed
                        );
                    }
                });
    }

    private void refreshAccountStatus() {
        if (operationInProgress
                || cloudBackupOperationInProgress) {
            return;
        }

        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            showCurrentAccountState();

            return;
        }

        showSignedInOperationState(true);

        firebaseUser
                .reload()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showSignedInOperationState(false);

                        showFirebaseError(
                                task.getException(),
                                R.string.cloud_account_refresh_failed
                        );

                        return;
                    }

                    FirebaseUser refreshedUser =
                            firebaseAuth.getCurrentUser();

                    if (refreshedUser == null) {
                        showSignedInOperationState(false);
                        showCurrentAccountState();

                        return;
                    }

                    saveRefreshedVerificationStatus(
                            refreshedUser
                    );
                });
    }

    private void saveRefreshedVerificationStatus(
            @NonNull FirebaseUser firebaseUser
    ) {
        Map<String, Object> statusData =
                new HashMap<>();

        statusData.put(
                "email_verified",
                firebaseUser.isEmailVerified()
        );

        statusData.put(
                "updated_at",
                FieldValue.serverTimestamp()
        );

        firestore
                .collection("users")
                .document(firebaseUser.getUid())
                .set(
                        statusData,
                        SetOptions.merge()
                )
                .addOnCompleteListener(task -> {
                    showSignedInOperationState(false);
                    showCurrentAccountState();

                    showMessage(
                            firebaseUser.isEmailVerified()
                                    ? R.string.cloud_email_verified_success
                                    : R.string.cloud_email_not_verified_yet
                    );

                    loadCloudBackupMetadataIfAvailable();
                    continueAfterVerifiedAuthentication();
                });
    }

    private void continueAfterVerifiedAuthentication() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (!authenticationGate
                || openingUserMode
                || user == null
                || !user.isEmailVerified()) {
            return;
        }

        openingUserMode = true;
        Intent intent = new Intent(this, UserModeSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void requestCloudBackupDeletion() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || !user.isEmailVerified()
                || currentCloudBackupMetadata == null) {
            showMessage(R.string.cloud_backup_restore_unavailable);
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete cloud backup?")
                .setMessage("Cloud में सुरक्षित backup permanently मिट जाएगा। App का local data नहीं मिटेगा।")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Verify & Delete", (dialog, which) ->
                        requestPasswordVerification(
                                "Verify cloud backup deletion",
                                verifiedUser -> deleteVerifiedCloudBackup(verifiedUser, true)
                        ))
                .show();
    }

    private void deleteVerifiedCloudBackup(
            @NonNull FirebaseUser user,
            boolean showResult
    ) {
        setCloudBackupOperationState(true);
        cloudBackupUploader.deleteLatestBackup(
                user,
                new CloudBackupUploader.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            getSharedPreferences(CLOUD_STATE_PREFERENCES, MODE_PRIVATE)
                                    .edit().clear().apply();
                            currentCloudBackupMetadata = null;
                            setCloudBackupOperationState(false);
                            showNoCloudBackupState();
                            if (showResult) {
                                Snackbar.make(binding.getRoot(),
                                        "Cloud backup permanently deleted.",
                                        Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        runOnUiThread(() -> {
                            setCloudBackupOperationState(false);
                            showCloudBackupError(exception,
                                    R.string.cloud_backup_upload_failed);
                        });
                    }
                }
        );
    }

    private void requestPermanentAccountDeletion() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            showMessage(R.string.cloud_backup_verification_required);
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Permanently delete account?")
                .setMessage("Account, cloud backup, cloud profile और इस device का local study data हमेशा के लिए मिटेंगे। सभी student profiles, progress, doubts, books और settings delete होंगी। यह action वापस नहीं हो सकता।")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Verify & Continue", (dialog, which) ->
                        requestPasswordVerification(
                                "Verify permanent account deletion",
                                this::deleteVerifiedAccount
                        ))
                .show();
    }

    private void deleteVerifiedAccount(@NonNull FirebaseUser user) {
        setCloudBackupOperationState(true);
        cloudBackupUploader.deleteLatestBackup(
                user,
                new CloudBackupUploader.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        deleteCloudProfileAndAuthUser(user);
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        runOnUiThread(() -> {
                            setCloudBackupOperationState(false);
                            showCloudBackupError(exception,
                                    R.string.cloud_backup_upload_failed);
                        });
                    }
                }
        );
    }

    private void deleteCloudProfileAndAuthUser(@NonNull FirebaseUser user) {
        firestore.collection("users").document(user.getUid())
                .delete()
                .addOnSuccessListener(unused -> user.delete()
                        .addOnSuccessListener(deleted ->
                                permanentlyDeleteLocalAccountData())
                        .addOnFailureListener(error -> runOnUiThread(() -> {
                            setCloudBackupOperationState(false);
                            showFirebaseError(error,
                                    R.string.cloud_account_creation_failed);
                        })))
                .addOnFailureListener(error -> runOnUiThread(() -> {
                    setCloudBackupOperationState(false);
                    showFirebaseError(error,
                            R.string.cloud_account_creation_failed);
                }));
    }

    private void permanentlyDeleteLocalAccountData() {
        new LocalAccountDataRepository(this).permanentlyDeleteAll(
                new LocalAccountDataRepository.Callback() {
                    @Override
                    public void onSuccess() {
                        openCleanAuthenticationScreen();
                    }

                    @Override
                    public void onError(@NonNull Exception exception) {
                        setCloudBackupOperationState(false);
                        Snackbar.make(
                                binding.getRoot(),
                                "Account deleted, but device data cleanup failed. Clear app data before creating another account.",
                                Snackbar.LENGTH_INDEFINITE
                        ).show();
                    }
                }
        );
    }

    private void openCleanAuthenticationScreen() {
        clearSensitiveStateForAccountChange();
        firebaseAuth.signOut();
        Intent intent = new Intent(
                CloudAccountActivity.this,
                CloudAccountActivity.class
        );
        intent.putExtra(EXTRA_REQUIRE_AUTHENTICATION, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void requestPasswordVerification(
            @NonNull String title,
            @NonNull VerifiedUserAction action
    ) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        String email = user == null ? "" : getSafeEmail(user);
        if (user == null || email.isEmpty()) {
            showMessage(R.string.cloud_account_required);
            return;
        }

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Current password");
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        passwordInput.setPadding(padding, padding / 2, padding, padding / 2);

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(title)
                        .setMessage("सुरक्षा के लिए अपने account का current password लिखें।")
                        .setView(passwordInput)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Verify", null)
                        .create();

        dialog.setOnShowListener(unused ->
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(view -> {
                            String password = getInputText(passwordInput.getText());
                            if (password.length() < 8) {
                                passwordInput.setError("Valid current password required");
                                return;
                            }
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(false);
                            user.reauthenticate(
                                            EmailAuthProvider.getCredential(email, password))
                                    .addOnSuccessListener(done -> {
                                        dialog.dismiss();
                                        action.run(user);
                                    })
                                    .addOnFailureListener(error -> {
                                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                                                .setEnabled(true);
                                        passwordInput.setError("Password verification failed");
                                    });
                        }));
        dialog.show();
    }

    private interface VerifiedUserAction {
        void run(@NonNull FirebaseUser user);
    }

    private void signOutAccount() {
        if (operationInProgress
                || cloudBackupOperationInProgress) {
            return;
        }

        clearSensitiveStateForAccountChange();

        firebaseAuth.signOut();

        observedFirebaseUserId =
                "";

        currentCloudBackupMetadata = null;

        clearAccountInputs();

        setFormMode(
                AccountFormMode.SIGN_IN
        );

        showCurrentAccountState();

        showMessage(
                R.string.cloud_sign_out_success
        );
    }
    private void requestCloudBackupUpload() {
        if (operationInProgress
                || cloudBackupOperationInProgress) {
            return;
        }

        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            showMessage(
                    R.string.cloud_account_required
            );

            return;
        }

        if (!firebaseUser.isEmailVerified()) {
            showMessage(
                    R.string.cloud_backup_verification_required
            );

            return;
        }

        if (currentCloudBackupMetadata == null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(
                            R.string.cloud_backup_first_confirmation_title
                    )
                    .setMessage(
                            R.string.cloud_backup_first_confirmation_message
                    )
                    .setNegativeButton(
                            R.string.cloud_backup_cancel,
                            null
                    )
                    .setPositiveButton(
                            R.string.cloud_backup_upload_confirm,
                            (dialog, which) ->
                                    prepareAndUploadCloudBackup(
                                            firebaseUser.getUid()
                                    )
                    )
                    .show();

            return;
        }

        String previousUploadTime =
                formatDateTime(
                        currentCloudBackupMetadata
                                .getUploadedAt()
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.cloud_backup_replace_confirmation_title
                )
                .setMessage(
                        getString(
                                R.string.cloud_backup_replace_confirmation_message,
                                previousUploadTime
                        )
                )
                .setNegativeButton(
                        R.string.cloud_backup_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.cloud_backup_replace_confirm,
                        (dialog, which) ->
                                prepareAndUploadCloudBackup(
                                        firebaseUser.getUid()
                                )
                )
                .show();
    }

    private void requestCloudBackupRestore() {
        if (operationInProgress
                || cloudBackupOperationInProgress) {
            return;
        }

        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            showMessage(
                    R.string.cloud_account_required
            );

            return;
        }

        if (!firebaseUser.isEmailVerified()) {
            showMessage(
                    R.string.cloud_backup_verification_required
            );

            return;
        }

        CloudBackupUploader.CloudBackupMetadata metadata =
                currentCloudBackupMetadata;

        if (metadata == null
                || !metadata.isComplete()) {

            showMessage(
                    R.string.cloud_backup_restore_unavailable
            );

            return;
        }

        String backupTime =
                formatDateTime(
                        metadata.getUploadedAt() > 0L
                                ? metadata.getUploadedAt()
                                : metadata.getBackupCreatedAt()
                );

        String confirmationMessage =
                getString(
                        R.string.cloud_backup_restore_confirmation_message,
                        backupTime,
                        metadata.getProfileCount(),
                        metadata.getLessonProgressCount(),
                        metadata.getQuizAttemptCount(),
                        metadata.getDoubtCount(),
                        metadata.getPreferenceItemCount()
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.cloud_backup_restore_confirmation_title
                )
                .setMessage(
                        confirmationMessage
                )
                .setNegativeButton(
                        R.string.cloud_backup_cancel,
                        null
                )
                .setPositiveButton(
                        R.string.cloud_backup_restore_confirm,
                        (dialog, which) ->
                                downloadCloudBackupForRestore(
                                        firebaseUser.getUid()
                                )
                )
                .show();
    }

    private void downloadCloudBackupForRestore(
            @NonNull String expectedUserId
    ) {
        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        if (firebaseUser == null
                || !firebaseUser.isEmailVerified()
                || !expectedUserId.equals(
                firebaseUser.getUid()
        )) {
            clearSensitiveStateForAccountChange();

            showMessage(
                    R.string.cloud_backup_account_changed
            );

            return;
        }

        setCloudBackupOperationState(
                true
        );

        binding.textCloudBackupStatus.setText(
                R.string.cloud_backup_downloading_status
        );

        binding.textCloudBackupDescription.setText(
                R.string.cloud_backup_downloading_description
        );

        cloudBackupRestoreCoordinator
                .downloadLatestBackupToCache(
                        firebaseUser,
                        new CloudBackupRestoreCoordinator
                                .RestorePreparationCallback() {

                            @Override
                            public void onPrepared(
                                    @NonNull CloudBackupRestoreCoordinator
                                            .CloudRestorePreparationResult result
                            ) {
                                if (isFinishing()
                                        || isDestroyed()) {
                                    cloudBackupRestoreCoordinator
                                            .deletePreparedBackup(
                                                    result.getAbsoluteFilePath()
                                            );

                                    return;
                                }

                                if (!activityInForeground) {
                                    cloudBackupRestoreCoordinator
                                            .deletePreparedBackup(
                                                    result.getAbsoluteFilePath()
                                            );

                                    cloudBackupOperationInProgress =
                                            false;

                                    return;
                                }

                                FirebaseUser currentUser =
                                        firebaseAuth.getCurrentUser();

                                if (currentUser == null
                                        || !currentUser.isEmailVerified()
                                        || !expectedUserId.equals(
                                        currentUser.getUid()
                                )) {

                                    cloudBackupRestoreCoordinator
                                            .deletePreparedBackup(
                                                    result.getAbsoluteFilePath()
                                            );

                                    clearSensitiveStateForAccountChange();

                                    setCloudBackupOperationState(
                                            false
                                    );

                                    showMessage(
                                            R.string.cloud_backup_account_changed
                                    );

                                    showCurrentAccountState();

                                    return;
                                }

                                setCloudBackupOperationState(
                                        false
                                );

                                openPreparedCloudRestore(
                                        result
                                );
                            }

                            @Override
                            public void onError(
                                    @NonNull Exception exception
                            ) {
                                if (isFinishing()
                                        || isDestroyed()) {
                                    return;
                                }

                                if (!activityInForeground) {
                                    cloudBackupOperationInProgress =
                                            false;

                                    return;
                                }

                                setCloudBackupOperationState(
                                        false
                                );

                                if (currentCloudBackupMetadata != null) {
                                    showCloudBackupMetadata(
                                            currentCloudBackupMetadata
                                    );
                                }

                                showCloudBackupError(
                                        exception,
                                        R.string.cloud_backup_restore_prepare_failed
                                );
                            }
                        }
                );
    }

    private void openPreparedCloudRestore(
            @NonNull CloudBackupRestoreCoordinator
                    .CloudRestorePreparationResult result
    ) {
        File preparedRestoreFile =
                new File(
                        result.getAbsoluteFilePath()
                );

        try {
            cloudBackupSecurityGuard
                    .registerSensitiveFile(
                            this,
                            preparedRestoreFile
                    );

        } catch (IllegalArgumentException exception) {
            cloudBackupRestoreCoordinator
                    .deletePreparedBackup(
                            result.getAbsoluteFilePath()
                    );

            showCloudBackupError(
                    exception,
                    R.string.cloud_backup_restore_prepare_failed
            );

            return;
        }

        Intent restoreIntent =
                new Intent(
                        CloudAccountActivity.this,
                        BackupRestoreActivity.class
                );

        restoreIntent.putExtra(
                BackupRestoreActivity.EXTRA_INTERNAL_BACKUP_PATH,
                result.getAbsoluteFilePath()
        );

        restoreIntent.putExtra(
                BackupRestoreActivity.EXTRA_INTERNAL_BACKUP_DISPLAY_NAME,
                result.getDisplayFileName()
        );

        openingPreparedCloudRestore =
                true;

        try {
            startActivity(
                    restoreIntent
            );

        } catch (RuntimeException exception) {
            openingPreparedCloudRestore =
                    false;

            cloudBackupSecurityGuard
                    .unregisterSensitiveFile(
                            preparedRestoreFile
                    );

            cloudBackupRestoreCoordinator
                    .deletePreparedBackup(
                            result.getAbsoluteFilePath()
                    );

            showCloudBackupError(
                    exception,
                    R.string.cloud_backup_restore_prepare_failed
            );
        }
    }

    private void prepareAndUploadCloudBackup(
            @NonNull String expectedUserId
    ) {
        setCloudBackupOperationState(
                true
        );

        binding.textCloudBackupStatus.setText(
                R.string.cloud_backup_preparing_status
        );

        binding.textCloudBackupDescription.setText(
                R.string.cloud_backup_preparing_description
        );

        StudySaathiDatabase
                .databaseWriteExecutor
                .execute(() -> {
                    try {
                        CloudBackupPayloadBuilder
                                .CloudBackupPayload payload =
                                new CloudBackupPayloadBuilder(
                                        CloudAccountActivity.this
                                ).build();

                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            uploadPreparedCloudBackup(
                                    expectedUserId,
                                    payload
                            );
                        });

                    } catch (Exception exception) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            setCloudBackupOperationState(
                                    false
                            );

                            showCloudBackupError(
                                    exception,
                                    R.string.cloud_backup_prepare_failed
                            );
                        });
                    }
                });
    }

    private void uploadPreparedCloudBackup(
            @NonNull String expectedUserId,
            @NonNull CloudBackupPayloadBuilder
                    .CloudBackupPayload payload
    ) {
        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        if (firebaseUser == null
                || !firebaseUser.isEmailVerified()
                || !expectedUserId.equals(
                firebaseUser.getUid()
        )) {
            clearSensitiveStateForAccountChange();

            setCloudBackupOperationState(
                    false
            );

            showMessage(
                    R.string.cloud_backup_account_changed
            );

            showCurrentAccountState();

            return;
        }

        binding.textCloudBackupStatus.setText(
                R.string.cloud_backup_uploading_status
        );

        binding.textCloudBackupDescription.setText(
                getString(
                        R.string.cloud_backup_uploading_description_format,
                        payload.getChunkCount()
                )
        );

        cloudBackupUploader.uploadLatestBackup(
                firebaseUser,
                payload,
                new CloudBackupUploader.UploadCallback() {

                    @Override
                    public void onSuccess(
                            @NonNull CloudBackupUploader
                                    .UploadResult uploadResult
                    ) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            saveSuccessfulCloudUpload(
                                    uploadResult
                            );

                            setCloudBackupOperationState(
                                    false
                            );

                            showMessage(
                                    R.string.cloud_backup_upload_success
                            );

                            loadCloudBackupMetadataIfAvailable();
                        });
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            setCloudBackupOperationState(
                                    false
                            );

                            showCloudBackupError(
                                    exception,
                                    R.string.cloud_backup_upload_failed
                            );
                        });
                    }
                }
        );
    }

    private void saveSuccessfulCloudUpload(
            @NonNull CloudBackupUploader
                    .UploadResult uploadResult
    ) {
        getSharedPreferences(
                CLOUD_STATE_PREFERENCES,
                MODE_PRIVATE
        )
                .edit()
                .putLong(
                        KEY_LAST_CLOUD_UPLOAD_AT,
                        uploadResult.getUploadedAt()
                )
                .putString(
                        KEY_LAST_CLOUD_BACKUP_ID,
                        uploadResult.getBackupId()
                )
                .apply();
    }

    private void loadCloudBackupMetadataIfAvailable() {
        if (operationInProgress
                || cloudBackupOperationInProgress) {
            return;
        }

        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            currentCloudBackupMetadata = null;
            showCloudBackupSignedOutState();

            return;
        }

        if (!firebaseUser.isEmailVerified()) {
            currentCloudBackupMetadata = null;
            showCloudBackupVerificationRequiredState();

            return;
        }

        setCloudBackupOperationState(
                true
        );

        showCloudBackupLoadingState();

        cloudBackupUploader.loadLatestBackupMetadata(
                firebaseUser,
                new CloudBackupUploader.MetadataCallback() {

                    @Override
                    public void onLoaded(
                            CloudBackupUploader
                                    .CloudBackupMetadata metadata
                    ) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            currentCloudBackupMetadata =
                                    metadata;

                            setCloudBackupOperationState(
                                    false
                            );

                            if (metadata == null) {
                                showNoCloudBackupState();
                            } else {
                                showCloudBackupMetadata(
                                        metadata
                                );
                            }
                        });
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        runOnUiThread(() -> {
                            if (isFinishing()
                                    || isDestroyed()) {
                                return;
                            }

                            setCloudBackupOperationState(
                                    false
                            );

                            showCloudBackupLoadErrorState();

                            showCloudBackupError(
                                    exception,
                                    R.string.cloud_backup_status_load_failed
                            );
                        });
                    }
                }
        );
    }

    private void showCurrentAccountState() {
        handlePossibleAccountChange();

        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        boolean signedIn =
                firebaseUser != null;

        binding.cardCloudSignedOut.setVisibility(
                signedIn
                        ? View.GONE
                        : View.VISIBLE
        );

        binding.cardCloudSignedIn.setVisibility(
                signedIn
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardCloudBackup.setVisibility(
                signedIn
                        ? View.VISIBLE
                        : View.GONE
        );

        if (!signedIn) {
            currentCloudBackupMetadata = null;

            binding.textCloudHeaderStatus.setText(
                    R.string.cloud_status_signed_out
            );

            binding.textCloudHeaderDescription.setText(
                    R.string.cloud_status_signed_out_description
            );

            showCloudBackupSignedOutState();

            return;
        }

        boolean emailVerified =
                firebaseUser.isEmailVerified();

        binding.textCloudHeaderStatus.setText(
                emailVerified
                        ? R.string.cloud_status_verified
                        : R.string.cloud_status_verification_required
        );

        binding.textCloudHeaderDescription.setText(
                emailVerified
                        ? R.string.cloud_status_verified_description
                        : R.string.cloud_status_verification_description
        );

        binding.textCloudAccountName.setText(
                getSafeDisplayName(firebaseUser)
        );

        binding.textCloudAccountEmail.setText(
                getSafeEmail(firebaseUser)
        );

        binding.textCloudAccountUid.setText(
                getString(
                        R.string.cloud_uid_format,
                        firebaseUser.getUid()
                )
        );

        binding.textCloudVerificationStatus.setText(
                emailVerified
                        ? R.string.cloud_verified_badge
                        : R.string.cloud_unverified_badge
        );

        binding.textCloudVerificationStatus
                .setTextColor(
                        getColor(
                                emailVerified
                                        ? R.color.ss_success
                                        : R.color.ss_warning
                        )
                );

        binding.buttonCloudResendVerification
                .setVisibility(
                        emailVerified
                                ? View.GONE
                                : View.VISIBLE
                );

        updateCloudBackupButtonsEnabled();

        if (!emailVerified) {
            currentCloudBackupMetadata = null;
            showCloudBackupVerificationRequiredState();
        }
    }

    private void showCloudBackupLoadingState() {
        binding.textCloudBackupStatus.setText(
                R.string.cloud_backup_checking_status
        );

        binding.textCloudBackupDescription.setText(
                R.string.cloud_backup_checking_description
        );

        clearCloudBackupMetadataViews();
    }

    private void showNoCloudBackupState() {
        binding.textCloudBackupStatus.setText(
                R.string.cloud_backup_none_status
        );

        binding.textCloudBackupDescription.setText(
                R.string.cloud_backup_none_description
        );

        clearCloudBackupMetadataViews();

        binding.buttonCloudUploadBackup.setText(
                R.string.cloud_backup_upload_action
        );

        updateCloudBackupButtonsEnabled();
    }

    private void showCloudBackupMetadata(
            @NonNull CloudBackupUploader
                    .CloudBackupMetadata metadata
    ) {
        binding.textCloudBackupStatus.setText(
                metadata.isComplete()
                        ? R.string.cloud_backup_available_status
                        : R.string.cloud_backup_incomplete_status
        );

        binding.textCloudBackupDescription.setText(
                metadata.isComplete()
                        ? R.string.cloud_backup_available_description
                        : R.string.cloud_backup_incomplete_description
        );

        long displayTimestamp =
                metadata.getUploadedAt() > 0L
                        ? metadata.getUploadedAt()
                        : metadata.getBackupCreatedAt();

        binding.textCloudBackupUploadedAt.setText(
                getString(
                        R.string.cloud_backup_uploaded_at_format,
                        formatDateTime(
                                displayTimestamp
                        )
                )
        );

        binding.textCloudBackupId.setText(
                getString(
                        R.string.cloud_backup_id_format,
                        metadata.getBackupId()
                )
        );

        binding.textCloudBackupProfiles.setText(
                String.valueOf(
                        metadata.getProfileCount()
                )
        );

        binding.textCloudBackupLessons.setText(
                String.valueOf(
                        metadata.getLessonProgressCount()
                )
        );

        binding.textCloudBackupQuizzes.setText(
                String.valueOf(
                        metadata.getQuizAttemptCount()
                )
        );

        binding.textCloudBackupDoubts.setText(
                String.valueOf(
                        metadata.getDoubtCount()
                )
        );

        binding.textCloudBackupPreferences.setText(
                String.valueOf(
                        metadata.getPreferenceItemCount()
                )
        );

        binding.textCloudBackupStorage.setText(
                getString(
                        R.string.cloud_backup_storage_format,
                        formatFileSize(
                                metadata.getCompressedBytes()
                        ),
                        metadata.getChunkCount()
                )
        );

        binding.buttonCloudUploadBackup.setText(
                R.string.cloud_backup_replace_action
        );

        updateCloudBackupButtonsEnabled();
    }

    private void showCloudBackupVerificationRequiredState() {
        binding.textCloudBackupStatus.setText(
                R.string.cloud_backup_blocked_status
        );

        binding.textCloudBackupDescription.setText(
                R.string.cloud_backup_blocked_description
        );

        clearCloudBackupMetadataViews();

        updateCloudBackupButtonsEnabled();
    }

    private void showCloudBackupSignedOutState() {
        binding.textCloudBackupStatus.setText(
                R.string.cloud_backup_signed_out_status
        );

        binding.textCloudBackupDescription.setText(
                R.string.cloud_backup_signed_out_description
        );

        clearCloudBackupMetadataViews();

        updateCloudBackupButtonsEnabled();
    }

    private void showCloudBackupLoadErrorState() {
        binding.textCloudBackupStatus.setText(
                R.string.cloud_backup_status_error
        );

        long localLastUploadAt =
                getSharedPreferences(
                        CLOUD_STATE_PREFERENCES,
                        MODE_PRIVATE
                )
                        .getLong(
                                KEY_LAST_CLOUD_UPLOAD_AT,
                                0L
                        );

        if (localLastUploadAt > 0L) {
            binding.textCloudBackupDescription.setText(
                    getString(
                            R.string.cloud_backup_status_error_with_local_format,
                            formatDateTime(
                                    localLastUploadAt
                            )
                    )
            );
        } else {
            binding.textCloudBackupDescription.setText(
                    R.string.cloud_backup_status_error_description
            );
        }

        updateCloudBackupButtonsEnabled();
    }
    private void clearCloudBackupMetadataViews() {
        binding.textCloudBackupUploadedAt.setText(
                R.string.cloud_backup_uploaded_at_none
        );

        binding.textCloudBackupId.setText(
                R.string.cloud_backup_id_none
        );

        binding.textCloudBackupProfiles.setText("0");
        binding.textCloudBackupLessons.setText("0");
        binding.textCloudBackupQuizzes.setText("0");
        binding.textCloudBackupDoubts.setText("0");
        binding.textCloudBackupPreferences.setText("0");

        binding.textCloudBackupStorage.setText(
                R.string.cloud_backup_storage_none
        );

        binding.buttonCloudUploadBackup.setText(
                R.string.cloud_backup_upload_action
        );
    }

    private void showOperationState(
            boolean inProgress,
            int primaryButtonText
    ) {
        operationInProgress =
                inProgress;

        binding.progressCloudAccount.setVisibility(
                inProgress
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.buttonCloudPrimaryAction.setEnabled(
                !inProgress
        );

        binding.buttonCloudPrimaryAction.setText(
                primaryButtonText
        );

        binding.buttonCloudForgotPassword.setEnabled(
                !inProgress
        );

        binding.buttonGoogleAccount.setEnabled(
                !inProgress
        );

        binding.buttonCloudSignInMode.setEnabled(
                !inProgress
        );

        binding.buttonCloudCreateMode.setEnabled(
                !inProgress
        );

        binding.inputCloudDisplayName.setEnabled(
                !inProgress
        );

        binding.inputCloudEmail.setEnabled(
                !inProgress
        );

        binding.inputCloudPassword.setEnabled(
                !inProgress
        );

        binding.inputCloudConfirmPassword.setEnabled(
                !inProgress
        );

        binding.contentCloudAccount.setAlpha(
                inProgress
                        ? 0.70f
                        : 1f
        );

        updateCloudBackupButtonsEnabled();
    }

    private void showSignedInOperationState(
            boolean inProgress
    ) {
        operationInProgress =
                inProgress;

        binding.progressCloudAccount.setVisibility(
                inProgress
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.buttonCloudRefreshAccount.setEnabled(
                !inProgress
        );

        binding.buttonCloudResendVerification.setEnabled(
                !inProgress
        );

        binding.buttonCloudSignOut.setEnabled(
                !inProgress
        );

        binding.contentCloudAccount.setAlpha(
                inProgress
                        ? 0.70f
                        : 1f
        );

        updateCloudBackupButtonsEnabled();
    }

    private void setCloudBackupOperationState(
            boolean inProgress
    ) {
        cloudBackupOperationInProgress =
                inProgress;

        binding.progressCloudBackup.setVisibility(
                inProgress
                        ? View.VISIBLE
                        : View.GONE
        );

        updateCloudBackupButtonsEnabled();

        if (inProgress) {
            binding.buttonCloudUploadBackup.setText(
                    R.string.cloud_backup_uploading_action
            );
        } else if (currentCloudBackupMetadata == null) {
            binding.buttonCloudUploadBackup.setText(
                    R.string.cloud_backup_upload_action
            );
        } else {
            binding.buttonCloudUploadBackup.setText(
                    R.string.cloud_backup_replace_action
            );
        }
    }

    private void updateCloudBackupButtonsEnabled() {
        FirebaseUser firebaseUser =
                firebaseAuth == null
                        ? null
                        : firebaseAuth.getCurrentUser();

        boolean cloudActionsAvailable =
                firebaseUser != null
                        && firebaseUser.isEmailVerified()
                        && !operationInProgress
                        && !cloudBackupOperationInProgress;

        boolean restoreAvailable =
                cloudActionsAvailable
                        && currentCloudBackupMetadata != null
                        && currentCloudBackupMetadata.isComplete();

        binding.buttonCloudUploadBackup.setEnabled(
                cloudActionsAvailable
        );

        binding.buttonCloudRefreshBackup.setEnabled(
                cloudActionsAvailable
        );

        binding.buttonCloudRestoreBackup.setEnabled(
                restoreAvailable
        );

        binding.buttonDeleteCloudBackup.setEnabled(
                restoreAvailable
        );

        binding.buttonCloudUploadBackup.setAlpha(
                cloudActionsAvailable
                        ? 1f
                        : 0.55f
        );

        binding.buttonCloudRefreshBackup.setAlpha(
                cloudActionsAvailable
                        ? 1f
                        : 0.55f
        );

        binding.buttonCloudRestoreBackup.setAlpha(
                restoreAvailable
                        ? 1f
                        : 0.55f
        );

        binding.buttonDeleteCloudBackup.setAlpha(
                restoreAvailable
                        ? 1f
                        : 0.55f
        );
    }

    private void handlePossibleAccountChange() {
        String currentFirebaseUserId =
                getCurrentFirebaseUserId();

        if (observedFirebaseUserId.equals(
                currentFirebaseUserId
        )) {
            return;
        }

        clearSensitiveStateForAccountChange();

        observedFirebaseUserId =
                currentFirebaseUserId;

        currentCloudBackupMetadata =
                null;
    }

    private void clearSensitiveStateForAccountChange() {
        openingPreparedCloudRestore =
                false;

        if (cloudBackupSecurityGuard != null) {
            cloudBackupSecurityGuard
                    .clearForAccountChange(this);
        }
    }

    @NonNull
    private String getCurrentFirebaseUserId() {
        if (firebaseAuth == null) {
            return "";
        }

        FirebaseUser firebaseUser =
                firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            return "";
        }

        String firebaseUserId =
                firebaseUser.getUid();

        return firebaseUserId == null
                ? ""
                : firebaseUserId.trim();
    }

    private void clearInputErrors() {
        binding.layoutCloudDisplayName.setError(null);
        binding.layoutCloudEmail.setError(null);
        binding.layoutCloudPassword.setError(null);
        binding.layoutCloudConfirmPassword.setError(null);
    }

    private void clearPasswordInputs() {
        binding.inputCloudPassword.setText("");
        binding.inputCloudConfirmPassword.setText("");
    }

    private void clearAccountInputs() {
        binding.inputCloudDisplayName.setText("");
        binding.inputCloudEmail.setText("");

        clearPasswordInputs();
        clearInputErrors();
    }

    @NonNull
    private String getInputText(
            CharSequence charSequence
    ) {
        if (charSequence == null) {
            return "";
        }

        return charSequence
                .toString()
                .trim();
    }

    @NonNull
    private String getSafeDisplayName(
            @NonNull FirebaseUser firebaseUser
    ) {
        String displayName =
                firebaseUser.getDisplayName();

        if (!TextUtils.isEmpty(displayName)) {
            return displayName.trim();
        }

        String email =
                firebaseUser.getEmail();

        if (!TextUtils.isEmpty(email)
                && email.contains("@")) {

            return email.substring(
                    0,
                    email.indexOf('@')
            );
        }

        return getString(
                R.string.cloud_account_default_name
        );
    }

    @NonNull
    private String getSafeEmail(
            @NonNull FirebaseUser firebaseUser
    ) {
        String email =
                firebaseUser.getEmail();

        return TextUtils.isEmpty(email)
                ? getString(
                R.string.cloud_email_unavailable
        )
                : email.trim();
    }

    @NonNull
    private String formatDateTime(
            long timestamp
    ) {
        if (timestamp <= 0L) {
            return getString(
                    R.string.cloud_backup_time_unknown
            );
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                );

        return Instant.ofEpochMilli(timestamp)
                .atZone(
                        ZoneId.systemDefault()
                )
                .format(formatter);
    }

    @NonNull
    private String formatFileSize(
            int byteCount
    ) {
        if (byteCount <= 0) {
            return "0 KB";
        }

        double kilobytes =
                byteCount / 1024.0;

        if (kilobytes < 1024.0) {
            return String.format(
                    Locale.getDefault(),
                    "%.1f KB",
                    kilobytes
            );
        }

        double megabytes =
                kilobytes / 1024.0;

        return String.format(
                Locale.getDefault(),
                "%.2f MB",
                megabytes
        );
    }

    private void showFirebaseError(
            Exception exception,
            int fallbackMessage
    ) {
        String errorMessage =
                exception == null
                        ? ""
                        : exception.getLocalizedMessage();

        if (TextUtils.isEmpty(errorMessage)) {
            showMessage(fallbackMessage);

            return;
        }

        Snackbar.make(
                binding.getRoot(),
                getString(
                        R.string.cloud_error_format,
                        getString(fallbackMessage),
                        errorMessage
                ),
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showCloudBackupError(
            Exception exception,
            int fallbackMessage
    ) {
        String errorMessage =
                exception == null
                        ? ""
                        : exception.getLocalizedMessage();

        if (TextUtils.isEmpty(errorMessage)) {
            showMessage(fallbackMessage);

            return;
        }

        Snackbar.make(
                binding.getRoot(),
                getString(
                        R.string.cloud_backup_error_format,
                        getString(fallbackMessage),
                        errorMessage
                ),
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void showMessage(int messageResource) {
        Snackbar.make(
                binding.getRoot(),
                messageResource,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void hideKeyboard() {
        View focusedView =
                getCurrentFocus();

        if (focusedView == null) {
            return;
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(
                        INPUT_METHOD_SERVICE
                );

        inputMethodManager.hideSoftInputFromWindow(
                focusedView.getWindowToken(),
                0
        );

        focusedView.clearFocus();
    }
}
