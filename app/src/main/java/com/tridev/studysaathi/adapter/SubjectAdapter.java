package com.tridev.studysaathi.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.AskStudySaathiActivity;
import com.tridev.studysaathi.databinding.ItemSubjectBinding;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<
        SubjectAdapter.SubjectViewHolder> {

    @NonNull
    private final List<SubjectItem> subjectItems;

    @NonNull
    private final OnSubjectClickListener onSubjectClickListener;

    public SubjectAdapter(
            @NonNull List<SubjectItem> subjectItems,
            @NonNull OnSubjectClickListener onSubjectClickListener
    ) {
        this.subjectItems =
                new ArrayList<>(
                        subjectItems
                );

        this.onSubjectClickListener =
                onSubjectClickListener;
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSubjectBinding binding =
                ItemSubjectBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new SubjectViewHolder(
                binding
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull SubjectViewHolder holder,
            int position
    ) {
        holder.bind(
                subjectItems.get(
                        position
                )
        );
    }

    @Override
    public int getItemCount() {
        return subjectItems.size();
    }

    public void submitList(
            @NonNull List<SubjectItem> updatedSubjects
    ) {
        subjectItems.clear();

        subjectItems.addAll(
                updatedSubjects
        );

        notifyDataSetChanged();
    }

    public interface OnSubjectClickListener {

        /**
         * Subject card के दाईं ओर मौजूद arrow दबाने पर
         * existing chapter screen खोलने के लिए callback।
         */
        void onSubjectClicked(
                @NonNull SubjectItem subjectItem
        );
    }

    class SubjectViewHolder
            extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemSubjectBinding binding;

        SubjectViewHolder(
                @NonNull ItemSubjectBinding binding
        ) {
            super(
                    binding.getRoot()
            );

            this.binding =
                    binding;
        }

        void bind(
                @NonNull SubjectItem subjectItem
        ) {
            binding.textSubjectName.setText(
                    subjectItem.getSubjectName()
            );

            binding.textSubjectDescription.setText(
                    subjectItem.getSubjectDescription()
            );

            binding.textSubjectIcon.setText(
                    subjectItem.getIconText()
            );

            int backgroundColor =
                    ContextCompat.getColor(
                            binding.getRoot()
                                    .getContext(),
                            subjectItem
                                    .getBackgroundColorRes()
                    );

            int borderColor =
                    ContextCompat.getColor(
                            binding.getRoot()
                                    .getContext(),
                            subjectItem
                                    .getBorderColorRes()
                    );

            int accentColor =
                    ContextCompat.getColor(
                            binding.getRoot()
                                    .getContext(),
                            subjectItem
                                    .getAccentColorRes()
                    );

            binding.cardSubject
                    .setCardBackgroundColor(
                            backgroundColor
                    );

            binding.cardSubject
                    .setStrokeColor(
                            borderColor
                    );

            binding.cardSubjectIcon
                    .setCardBackgroundColor(
                            ContextCompat.getColor(
                                    binding.getRoot()
                                            .getContext(),
                                    android.R.color.white
                            )
                    );

            binding.cardSubjectIcon
                    .setStrokeColor(
                            borderColor
                    );

            binding.textSubjectIcon.setTextColor(
                    accentColor
            );

            binding.textSubjectArrow.setTextColor(
                    accentColor
            );

            /*
             * HERO ACTION
             *
             * पूरे Subject Card पर tap करने से बच्ची सीधे
             * Ask Study Saathi screen पर जाएगी।
             *
             * Selected subject अपने-आप prefill हो जाएगा।
             */
            binding.cardSubject.setOnClickListener(view ->
                    openSubjectTutor(
                            view,
                            subjectItem
                    )
            );

            /*
             * ADVANCED / CHAPTER ACTION
             *
             * दाईं ओर के arrow पर tap करने से पुराना exact
             * school-book chapter flow सुरक्षित रूप से खुलेगा।
             */
            binding.textSubjectArrow.setOnClickListener(view ->
                    onSubjectClickListener.onSubjectClicked(
                            subjectItem
                    )
            );

            binding.textSubjectArrow.setClickable(
                    true
            );

            binding.textSubjectArrow.setFocusable(
                    true
            );

            binding.textSubjectArrow.setContentDescription(
                    subjectItem.getSubjectName()
                            + " chapters खोलें"
            );
        }

        private void openSubjectTutor(
                @NonNull View clickedView,
                @NonNull SubjectItem subjectItem
        ) {
            Intent tutorIntent =
                    new Intent(
                            clickedView.getContext(),
                            AskStudySaathiActivity.class
                    );

            tutorIntent.putExtra(
                    AskStudySaathiActivity
                            .EXTRA_PREFILL_SUBJECT,
                    subjectItem.getSubjectName()
            );

            /*
             * अभी chapter अनिवार्य रूप से pass नहीं किया जा रहा।
             *
             * अगले Hero step में Ask Study Saathi screen को
             * subject-first और chapter-optional बनाया जाएगा।
             */
            clickedView.getContext()
                    .startActivity(
                            tutorIntent
                    );
        }
    }
}