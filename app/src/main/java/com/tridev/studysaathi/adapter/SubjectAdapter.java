package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.databinding.ItemSubjectBinding;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<
        SubjectAdapter.SubjectViewHolder> {

    private final List<SubjectItem> subjectItems;
    private final OnSubjectClickListener onSubjectClickListener;

    public SubjectAdapter(
            @NonNull List<SubjectItem> subjectItems,
            @NonNull OnSubjectClickListener onSubjectClickListener
    ) {
        this.subjectItems = new ArrayList<>(subjectItems);
        this.onSubjectClickListener = onSubjectClickListener;
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSubjectBinding binding =
                ItemSubjectBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new SubjectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SubjectViewHolder holder,
            int position
    ) {
        holder.bind(subjectItems.get(position));
    }

    @Override
    public int getItemCount() {
        return subjectItems.size();
    }

    public void submitList(
            @NonNull List<SubjectItem> updatedSubjects
    ) {
        subjectItems.clear();
        subjectItems.addAll(updatedSubjects);
        notifyDataSetChanged();
    }

    public interface OnSubjectClickListener {

        void onSubjectClicked(
                @NonNull SubjectItem subjectItem
        );
    }

    class SubjectViewHolder extends RecyclerView.ViewHolder {

        private final ItemSubjectBinding binding;

        SubjectViewHolder(
                @NonNull ItemSubjectBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull SubjectItem subjectItem) {
            binding.textSubjectName.setText(
                    subjectItem.getSubjectName()
            );

            binding.textSubjectDescription.setText(
                    subjectItem.getSubjectDescription()
            );

            binding.textSubjectIcon.setText(
                    subjectItem.getIconText()
            );

            int backgroundColor = ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    subjectItem.getBackgroundColorRes()
            );

            int borderColor = ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    subjectItem.getBorderColorRes()
            );

            int accentColor = ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    subjectItem.getAccentColorRes()
            );

            binding.cardSubject.setCardBackgroundColor(
                    backgroundColor
            );

            binding.cardSubject.setStrokeColor(
                    borderColor
            );

            binding.cardSubjectIcon.setCardBackgroundColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            android.R.color.white
                    )
            );

            binding.cardSubjectIcon.setStrokeColor(
                    borderColor
            );

            binding.textSubjectIcon.setTextColor(accentColor);
            binding.textSubjectArrow.setTextColor(accentColor);

            binding.cardSubject.setOnClickListener(view ->
                    onSubjectClickListener.onSubjectClicked(
                            subjectItem
                    )
            );
        }
    }
}