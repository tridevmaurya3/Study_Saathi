package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.databinding.ItemStudentProfileBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentProfileAdapter extends RecyclerView.Adapter<
        StudentProfileAdapter.StudentProfileViewHolder> {

    private final List<StudentProfileEntity> studentProfiles;
    private final OnProfileClickListener onProfileClickListener;

    public StudentProfileAdapter(
            @NonNull List<StudentProfileEntity> studentProfiles,
            @NonNull OnProfileClickListener onProfileClickListener
    ) {
        this.studentProfiles = new ArrayList<>(studentProfiles);
        this.onProfileClickListener = onProfileClickListener;
    }

    @NonNull
    @Override
    public StudentProfileViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemStudentProfileBinding binding =
                ItemStudentProfileBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new StudentProfileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull StudentProfileViewHolder holder,
            int position
    ) {
        holder.bind(studentProfiles.get(position));
    }

    @Override
    public int getItemCount() {
        return studentProfiles.size();
    }

    public void submitList(
            @NonNull List<StudentProfileEntity> updatedProfiles
    ) {
        studentProfiles.clear();
        studentProfiles.addAll(updatedProfiles);
        notifyDataSetChanged();
    }

    public interface OnProfileClickListener {

        void onProfileClicked(
                @NonNull StudentProfileEntity studentProfile
        );
    }

    class StudentProfileViewHolder extends RecyclerView.ViewHolder {

        private final ItemStudentProfileBinding binding;

        StudentProfileViewHolder(
                @NonNull ItemStudentProfileBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull StudentProfileEntity studentProfile
        ) {
            binding.textStudentName.setText(
                    studentProfile.getStudentName()
            );

            String profileDetails =
                    studentProfile.getEducationBoard()
                            + "  •  "
                            + studentProfile.getStudentClass()
                            + "  •  "
                            + studentProfile.getStudyMedium();

            binding.textStudentDetails.setText(profileDetails);

            binding.textProfileLanguage.setText(
                    binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.student_profile_language_format,
                                    studentProfile.getExplanationLanguage()
                            )
            );

            binding.textProfileInitial.setText(
                    getStudentInitial(
                            studentProfile.getStudentName()
                    )
            );

            applyActiveState(studentProfile.isActive());

            binding.cardProfile.setOnClickListener(view ->
                    onProfileClickListener.onProfileClicked(
                            studentProfile
                    )
            );
        }

        private void applyActiveState(boolean active) {
            int backgroundColor = ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    active
                            ? R.color.ss_blue_soft
                            : R.color.ss_surface
            );

            int strokeColor = ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    active
                            ? R.color.ss_primary
                            : R.color.ss_outline
            );

            binding.cardProfile.setCardBackgroundColor(
                    backgroundColor
            );

            binding.cardProfile.setStrokeColor(strokeColor);

            binding.cardProfile.setStrokeWidth(
                    active ? 2 : 1
            );

            binding.cardActiveBadge.setVisibility(
                    active
                            ? android.view.View.VISIBLE
                            : android.view.View.GONE
            );

            binding.textSelectArrow.setText(
                    active ? "✓" : "›"
            );

            binding.textSelectArrow.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            active
                                    ? R.color.ss_primary
                                    : R.color.ss_text_muted
                    )
            );
        }

        private String getStudentInitial(String studentName) {
            if (studentName == null
                    || studentName.trim().isEmpty()) {
                return "S";
            }

            return studentName
                    .trim()
                    .substring(0, 1)
                    .toUpperCase(Locale.getDefault());
        }
    }
}