package com.tridev.studysaathi.cloud;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;

public final class CloudBackupPassphraseDialog {

    private static final int
            MINIMUM_PASSPHRASE_LENGTH = 8;

    private static final int
            MAXIMUM_PASSPHRASE_LENGTH = 128;

    private CloudBackupPassphraseDialog() {
        /*
         * Utility class.
         * Object creation is not required.
         */
    }

    public interface PassphraseCallback {

        void onPassphraseAccepted(
                @NonNull char[] passphrase
        );
    }

    /**
     * Shows the secure passphrase dialog before
     * creating or replacing an encrypted cloud backup.
     */
    public static void showForEncryptedUpload(
            @NonNull Activity activity,
            @NonNull PassphraseCallback callback
    ) {
        showDialog(
                activity,
                DialogMode.ENCRYPTED_UPLOAD,
                callback
        );
    }

    /**
     * Shows the secure passphrase dialog before
     * restoring an encrypted cloud backup.
     */
    public static void showForEncryptedRestore(
            @NonNull Activity activity,
            @NonNull PassphraseCallback callback
    ) {
        showDialog(
                activity,
                DialogMode.ENCRYPTED_RESTORE,
                callback
        );
    }

    private static void showDialog(
            @NonNull Activity activity,
            @NonNull DialogMode dialogMode,
            @NonNull PassphraseCallback callback
    ) {
        if (activity.isFinishing()
                || activity.isDestroyed()) {

            return;
        }

        Context context =
                activity;

        LinearLayout contentContainer =
                new LinearLayout(
                        context
                );

        contentContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        int horizontalPadding =
                dpToPixels(
                        context,
                        24
                );

        int topPadding =
                dpToPixels(
                        context,
                        8
                );

        int bottomPadding =
                dpToPixels(
                        context,
                        4
                );

        contentContainer.setPadding(
                horizontalPadding,
                topPadding,
                horizontalPadding,
                bottomPadding
        );

        TextView informationText =
                new TextView(
                        context
                );

        if (dialogMode
                == DialogMode.ENCRYPTED_UPLOAD) {

            informationText.setText(
                    "कम से कम 8 characters का passphrase बनाएँ। "
                            + "इसे सुरक्षित रखें—passphrase भूलने पर "
                            + "encrypted backup restore नहीं किया जा सकेगा।"
            );

        } else {
            informationText.setText(
                    "इस backup को बनाते समय उपयोग किया गया "
                            + "passphrase दर्ज करें। गलत passphrase "
                            + "से backup decrypt नहीं होगा।"
            );
        }

        informationText.setTextSize(
                14f
        );

        informationText.setLineSpacing(
                0f,
                1.12f
        );

        contentContainer.addView(
                informationText,
                createMatchWidthWrapHeightParams()
        );

        TextInputLayout passphraseLayout =
                createPasswordInputLayout(
                        context,
                        "Backup passphrase"
                );

        TextInputEditText passphraseInput =
                createPasswordInput(
                        context
                );

        passphraseLayout.addView(
                passphraseInput,
                createMatchWidthWrapHeightParams()
        );

        LinearLayout.LayoutParams
                passphraseLayoutParams =
                createMatchWidthWrapHeightParams();

        passphraseLayoutParams.topMargin =
                dpToPixels(
                        context,
                        18
                );

        contentContainer.addView(
                passphraseLayout,
                passphraseLayoutParams
        );

        TextInputLayout confirmationLayout =
                null;

        TextInputEditText confirmationInput =
                null;

        if (dialogMode
                == DialogMode.ENCRYPTED_UPLOAD) {

            confirmationLayout =
                    createPasswordInputLayout(
                            context,
                            "Confirm backup passphrase"
                    );

            confirmationInput =
                    createPasswordInput(
                            context
                    );

            confirmationLayout.addView(
                    confirmationInput,
                    createMatchWidthWrapHeightParams()
            );

            LinearLayout.LayoutParams
                    confirmationLayoutParams =
                    createMatchWidthWrapHeightParams();

            confirmationLayoutParams.topMargin =
                    dpToPixels(
                            context,
                            12
                    );

            contentContainer.addView(
                    confirmationLayout,
                    confirmationLayoutParams
            );
        }

        String dialogTitle;

        String positiveButtonText;

        if (dialogMode
                == DialogMode.ENCRYPTED_UPLOAD) {

            dialogTitle =
                    "Secure cloud backup";

            positiveButtonText =
                    "Encrypt & Upload";

        } else {
            dialogTitle =
                    "Unlock encrypted backup";

            positiveButtonText =
                    "Unlock & Restore";
        }

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(
                        context
                )
                        .setTitle(
                                dialogTitle
                        )
                        .setView(
                                contentContainer
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                positiveButtonText,
                                null
                        )
                        .create();

        TextInputLayout
                finalConfirmationLayout =
                confirmationLayout;

        TextInputEditText
                finalConfirmationInput =
                confirmationInput;

        dialog.setOnShowListener(
                ignored -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            view -> {

                                clearInputErrors(
                                        passphraseLayout,
                                        finalConfirmationLayout
                                );

                                Editable passphraseEditable =
                                        passphraseInput
                                                .getText();

                                int passphraseLength =
                                        passphraseEditable == null
                                                ? 0
                                                : passphraseEditable
                                                .length();

                                if (passphraseLength
                                        < MINIMUM_PASSPHRASE_LENGTH) {

                                    passphraseLayout.setError(
                                            "Passphrase कम से कम "
                                                    + "8 characters का होना चाहिए।"
                                    );

                                    passphraseInput
                                            .requestFocus();

                                    showKeyboard(
                                            context,
                                            passphraseInput
                                    );

                                    return;
                                }

                                if (passphraseLength
                                        > MAXIMUM_PASSPHRASE_LENGTH) {

                                    passphraseLayout.setError(
                                            "Passphrase 128 characters "
                                                    + "से अधिक नहीं हो सकता।"
                                    );

                                    passphraseInput
                                            .requestFocus();

                                    return;
                                }

                                char[] passphrase =
                                        copyCharacters(
                                                passphraseEditable
                                        );

                                if (dialogMode
                                        == DialogMode
                                        .ENCRYPTED_UPLOAD) {

                                    Editable
                                            confirmationEditable =
                                            finalConfirmationInput
                                                    == null
                                                    ? null
                                                    : finalConfirmationInput
                                                    .getText();

                                    int confirmationLength =
                                            confirmationEditable
                                                    == null
                                                    ? 0
                                                    : confirmationEditable
                                                    .length();

                                    if (confirmationLength == 0) {

                                        clearCharacters(
                                                passphrase
                                        );

                                        if (finalConfirmationLayout
                                                != null) {

                                            finalConfirmationLayout
                                                    .setError(
                                                            "Passphrase "
                                                                    + "दोबारा दर्ज करें।"
                                                    );
                                        }

                                        if (finalConfirmationInput
                                                != null) {

                                            finalConfirmationInput
                                                    .requestFocus();

                                            showKeyboard(
                                                    context,
                                                    finalConfirmationInput
                                            );
                                        }

                                        return;
                                    }

                                    if (!charactersMatch(
                                            passphraseEditable,
                                            confirmationEditable
                                    )) {

                                        clearCharacters(
                                                passphrase
                                        );

                                        if (finalConfirmationLayout
                                                != null) {

                                            finalConfirmationLayout
                                                    .setError(
                                                            "दोनों passphrase "
                                                                    + "एक जैसे नहीं हैं।"
                                                    );
                                        }

                                        if (finalConfirmationInput
                                                != null) {

                                            finalConfirmationInput
                                                    .requestFocus();
                                        }

                                        return;
                                    }
                                }

                                hideKeyboard(
                                        context,
                                        passphraseInput
                                );

                                clearInputText(
                                        passphraseInput,
                                        finalConfirmationInput
                                );

                                dialog.dismiss();

                                callback
                                        .onPassphraseAccepted(
                                                passphrase
                                        );
                            }
                    );

                    passphraseInput
                            .requestFocus();

                    passphraseInput.postDelayed(
                            () -> showKeyboard(
                                    context,
                                    passphraseInput
                            ),
                            180L
                    );
                }
        );

        dialog.setOnDismissListener(
                ignored -> clearInputText(
                        passphraseInput,
                        finalConfirmationInput
                )
        );

        dialog.show();
    }

    @NonNull
    private static TextInputLayout
    createPasswordInputLayout(
            @NonNull Context context,
            @NonNull String hint
    ) {
        TextInputLayout inputLayout =
                new TextInputLayout(
                        context
                );

        inputLayout.setHint(
                hint
        );

        inputLayout.setBoxBackgroundMode(
                TextInputLayout
                        .BOX_BACKGROUND_OUTLINE
        );

        inputLayout.setEndIconMode(
                TextInputLayout
                        .END_ICON_PASSWORD_TOGGLE
        );

        inputLayout.setErrorEnabled(
                true
        );

        return inputLayout;
    }

    @NonNull
    private static TextInputEditText
    createPasswordInput(
            @NonNull Context context
    ) {
        TextInputEditText input =
                new TextInputEditText(
                        context
                );

        input.setSingleLine(
                true
        );

        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType
                        .TYPE_TEXT_VARIATION_PASSWORD
        );

        input.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(
                                MAXIMUM_PASSPHRASE_LENGTH
                        )
                }
        );

        input.setAutofillHints(
                View.AUTOFILL_HINT_PASSWORD
        );

        return input;
    }

    @NonNull
    private static LinearLayout.LayoutParams
    createMatchWidthWrapHeightParams() {

        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private static int dpToPixels(
            @NonNull Context context,
            int dpValue
    ) {
        float density =
                context.getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dpValue * density
        );
    }

    private static void clearInputErrors(
            @NonNull TextInputLayout passphraseLayout,
            TextInputLayout confirmationLayout
    ) {
        passphraseLayout.setError(
                null
        );

        if (confirmationLayout != null) {
            confirmationLayout.setError(
                    null
            );
        }
    }

    @NonNull
    private static char[] copyCharacters(
            @NonNull CharSequence characters
    ) {
        char[] copiedCharacters =
                new char[
                        characters.length()
                        ];

        for (int index = 0;
             index < characters.length();
             index++) {

            copiedCharacters[index] =
                    characters.charAt(
                            index
                    );
        }

        return copiedCharacters;
    }

    private static boolean charactersMatch(
            @NonNull CharSequence first,
            @NonNull CharSequence second
    ) {
        if (first.length()
                != second.length()) {

            return false;
        }

        int difference =
                0;

        for (int index = 0;
             index < first.length();
             index++) {

            difference |=
                    first.charAt(index)
                            ^ second.charAt(index);
        }

        return difference == 0;
    }

    private static void clearInputText(
            @NonNull TextInputEditText passphraseInput,
            TextInputEditText confirmationInput
    ) {
        Editable passphraseText =
                passphraseInput.getText();

        if (passphraseText != null) {
            passphraseText.clear();
        }

        if (confirmationInput == null) {
            return;
        }

        Editable confirmationText =
                confirmationInput.getText();

        if (confirmationText != null) {
            confirmationText.clear();
        }
    }

    private static void clearCharacters(
            char[] characters
    ) {
        if (characters == null) {
            return;
        }

        Arrays.fill(
                characters,
                '\0'
        );
    }

    private static void showKeyboard(
            @NonNull Context context,
            @NonNull View targetView
    ) {
        InputMethodManager
                inputMethodManager =
                (InputMethodManager)
                        context.getSystemService(
                                Context
                                        .INPUT_METHOD_SERVICE
                        );

        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(
                    targetView,
                    InputMethodManager
                            .SHOW_IMPLICIT
            );
        }
    }

    private static void hideKeyboard(
            @NonNull Context context,
            @NonNull View targetView
    ) {
        InputMethodManager
                inputMethodManager =
                (InputMethodManager)
                        context.getSystemService(
                                Context
                                        .INPUT_METHOD_SERVICE
                        );

        if (inputMethodManager != null) {
            inputMethodManager
                    .hideSoftInputFromWindow(
                            targetView
                                    .getWindowToken(),
                            0
                    );
        }
    }

    private enum DialogMode {
        ENCRYPTED_UPLOAD,
        ENCRYPTED_RESTORE
    }
}