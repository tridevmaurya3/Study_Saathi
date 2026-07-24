package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.databinding.ItemRevisionBinding;
import com.tridev.studysaathi.model.RevisionItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RevisionAdapter extends RecyclerView.Adapter<
        RevisionAdapter.RevisionViewHolder> {

    private final List<RevisionItem> revisionItems;
    private final OnRevisionClickListener onRevisionClickListener;

    public RevisionAdapter(
            @NonNull List<RevisionItem> revisionItems,
            @NonNull OnRevisionClickListener onRevisionClickListener
    ) {
        this.revisionItems =
                new ArrayList<>(revisionItems);

        this.onRevisionClickListener =
                onRevisionClickListener;
    }

    @NonNull
    @Override
    public RevisionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemRevisionBinding binding =
                ItemRevisionBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new RevisionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RevisionViewHolder holder,
            int position
    ) {
        holder.bind(revisionItems.get(position));
    }

    @Override
    public int getItemCount() {
        return revisionItems.size();
    }

    public void submitList(
            @NonNull List<RevisionItem> updatedItems
    ) {
        revisionItems.clear();
        revisionItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    public interface OnRevisionClickListener {

        void onRevisionClicked(
                @NonNull RevisionItem revisionItem
        );
    }

    class RevisionViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemRevisionBinding binding;

        RevisionViewHolder(
                @NonNull ItemRevisionBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull RevisionItem revisionItem
        ) {
            binding.textSubjectName.setText(
                    revisionItem.getSubjectName()
            );

            binding.textChapterTitle.setText(
                    revisionItem.getChapterTitle()
            );

            binding.textRevisionDate.setText(
                    getRevisionDateText(revisionItem)
            );

            binding.textRevisionIcon.setText(
                    getSubjectInitial(
                            revisionItem.getSubjectName()
                    )
            );

            applyRevisionStatus(
                    revisionItem.getRevisionStatus()
            );

            binding.cardRevision.setOnClickListener(
                    view ->
                            onRevisionClickListener
                                    .onRevisionClicked(
                                            revisionItem
                                    )
            );
        }

        @NonNull
        private String getRevisionDateText(
                @NonNull RevisionItem revisionItem
        ) {
            LocalDate revisionDate =
                    revisionItem.getNextRevisionDate();

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy",
                            Locale.getDefault()
                    );

            switch (revisionItem.getRevisionStatus()) {
                case DUE_TODAY:
                    return binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.revision_due_today_detail
                            );

                case OVERDUE:
                    return binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.revision_overdue_date_format,
                                    revisionDate.format(formatter)
                            );

                case UPCOMING:
                default:
                    return binding.getRoot()
                            .getContext()
                            .getString(
                                    R.string.revision_upcoming_date_format,
                                    revisionDate.format(formatter)
                            );
            }
        }

        private void applyRevisionStatus(
                @NonNull RevisionItem.RevisionStatus status
        ) {
            int statusTextRes;
            int backgroundColorRes;
            int textColorRes;
            int borderColorRes;

            switch (status) {
                case DUE_TODAY:
                    statusTextRes =
                            R.string.revision_status_due_today;

                    backgroundColorRes =
                            R.color.ss_yellow_soft;

                    textColorRes =
                            R.color.ss_warning;

                    borderColorRes =
                            R.color.ss_yellow_border;
                    break;

                case OVERDUE:
                    statusTextRes =
                            R.string.revision_status_overdue;

                    backgroundColorRes =
                            R.color.ss_red_soft;

                    textColorRes =
                            R.color.ss_error;

                    borderColorRes =
                            R.color.ss_red_border;
                    break;

                case UPCOMING:
                default:
                    statusTextRes =
                            R.string.revision_status_upcoming;

                    backgroundColorRes =
                            R.color.ss_blue_soft;

                    textColorRes =
                            R.color.ss_primary;

                    borderColorRes =
                            R.color.ss_blue_border;
                    break;
            }

            binding.textRevisionStatus.setText(
                    statusTextRes
            );

            binding.textRevisionStatus.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            textColorRes
                    )
            );

            binding.cardRevisionStatus
                    .setCardBackgroundColor(
                            ContextCompat.getColor(
                                    binding.getRoot().getContext(),
                                    backgroundColorRes
                            )
                    );

            binding.cardRevisionStatus.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            borderColorRes
                    )
            );

            binding.cardRevisionIcon
                    .setCardBackgroundColor(
                            ContextCompat.getColor(
                                    binding.getRoot().getContext(),
                                    backgroundColorRes
                            )
                    );

            binding.cardRevisionIcon.setStrokeColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            borderColorRes
                    )
            );

            binding.textRevisionIcon.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            textColorRes
                    )
            );

            binding.textRevisionArrow.setTextColor(
                    ContextCompat.getColor(
                            binding.getRoot().getContext(),
                            textColorRes
                    )
            );
        }

        @NonNull
        private String getSubjectInitial(
                String subjectName
        ) {
            if (subjectName == null
                    || subjectName.trim().isEmpty()) {
                return "R";
            }

            String trimmedName = subjectName.trim();

            if (trimmedName.length() <= 2) {
                return trimmedName.toUpperCase(
                        Locale.getDefault()
                );
            }

            return trimmedName
                    .substring(0, 1)
                    .toUpperCase(Locale.getDefault());
        }
    }
}