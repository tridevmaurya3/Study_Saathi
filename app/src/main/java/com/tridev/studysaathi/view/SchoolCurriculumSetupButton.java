package com.tridev.studysaathi.view;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.tridev.studysaathi.SchoolCurriculumSetupActivity;

/**
 * Parent Dashboard से exact school curriculum setup खोलने वाला
 * reusable navigation button.
 */
public final class SchoolCurriculumSetupButton
        extends MaterialButton {

    public SchoolCurriculumSetupButton(
            @NonNull Context context
    ) {
        super(context);
        initializeNavigation();
    }

    public SchoolCurriculumSetupButton(
            @NonNull Context context,
            @Nullable AttributeSet attrs
    ) {
        super(context, attrs);
        initializeNavigation();
    }

    public SchoolCurriculumSetupButton(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        initializeNavigation();
    }

    private void initializeNavigation() {
        setOnClickListener(view -> {
            Context context =
                    view.getContext();

            Intent intent =
                    new Intent(
                            context,
                            SchoolCurriculumSetupActivity.class
                    );

            context.startActivity(
                    intent
            );
        });
    }
}