package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.databinding.ItemGlobalSearchBinding;
import com.tridev.studysaathi.model.GlobalSearchItem;

import java.util.ArrayList;
import java.util.List;

public class GlobalSearchAdapter
        extends RecyclerView.Adapter<
        GlobalSearchAdapter.SearchViewHolder> {

    public interface OnSearchResultClickListener {
        void onSearchResultClick(
                @NonNull GlobalSearchItem searchItem
        );
    }

    private final List<GlobalSearchItem> searchItems =
            new ArrayList<>();

    @NonNull
    private final OnSearchResultClickListener
            clickListener;

    public GlobalSearchAdapter(
            @NonNull List<GlobalSearchItem> initialItems,
            @NonNull OnSearchResultClickListener clickListener
    ) {
        searchItems.addAll(initialItems);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemGlobalSearchBinding binding =
                ItemGlobalSearchBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new SearchViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SearchViewHolder holder,
            int position
    ) {
        holder.bind(
                searchItems.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return searchItems.size();
    }

    public void submitList(
            @NonNull List<GlobalSearchItem> updatedItems
    ) {
        searchItems.clear();
        searchItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    class SearchViewHolder
            extends RecyclerView.ViewHolder {

        private static final int MAX_DESCRIPTION_LENGTH =
                190;

        private final ItemGlobalSearchBinding binding;

        SearchViewHolder(
                @NonNull ItemGlobalSearchBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull GlobalSearchItem searchItem
        ) {
            binding.textGlobalResultTitle.setText(
                    searchItem.getTitle()
            );

            binding.textGlobalResultSubtitle.setText(
                    searchItem.getSubtitle()
            );

            binding.textGlobalResultDescription.setText(
                    createPreview(
                            searchItem.getDescription()
                    )
            );

            switch (searchItem.getResultType()) {
                case SUBJECT:
                    binding.textGlobalResultIcon.setText("S");

                    binding.textGlobalResultType.setText(
                            R.string.global_search_type_subject
                    );

                    binding.cardGlobalResultIcon.setCardBackgroundColor(
                            binding.getRoot()
                                    .getContext()
                                    .getColor(
                                            R.color.ss_blue_soft
                                    )
                    );

                    binding.textGlobalResultIcon.setTextColor(
                            binding.getRoot()
                                    .getContext()
                                    .getColor(
                                            R.color.ss_primary
                                    )
                    );
                    break;

                case CHAPTER:
                    binding.textGlobalResultIcon.setText("C");

                    binding.textGlobalResultType.setText(
                            R.string.global_search_type_chapter
                    );

                    binding.cardGlobalResultIcon.setCardBackgroundColor(
                            binding.getRoot()
                                    .getContext()
                                    .getColor(
                                            R.color.ss_green_soft
                                    )
                    );

                    binding.textGlobalResultIcon.setTextColor(
                            binding.getRoot()
                                    .getContext()
                                    .getColor(
                                            R.color.ss_success
                                    )
                    );
                    break;

                case NOTE:
                default:
                    binding.textGlobalResultIcon.setText("N");

                    binding.textGlobalResultType.setText(
                            R.string.global_search_type_note
                    );

                    binding.cardGlobalResultIcon.setCardBackgroundColor(
                            binding.getRoot()
                                    .getContext()
                                    .getColor(
                                            R.color.ss_yellow_soft
                                    )
                    );

                    binding.textGlobalResultIcon.setTextColor(
                            binding.getRoot()
                                    .getContext()
                                    .getColor(
                                            R.color.ss_warning
                                    )
                    );
                    break;
            }

            binding.getRoot().setOnClickListener(view ->
                    clickListener.onSearchResultClick(
                            searchItem
                    )
            );

            binding.buttonOpenGlobalResult
                    .setOnClickListener(view ->
                            clickListener.onSearchResultClick(
                                    searchItem
                            )
                    );
        }

        @NonNull
        private String createPreview(
                @NonNull String description
        ) {
            String cleanDescription =
                    description
                            .replaceAll("\\s+", " ")
                            .trim();

            if (cleanDescription.length()
                    <= MAX_DESCRIPTION_LENGTH) {
                return cleanDescription;
            }

            return cleanDescription
                    .substring(
                            0,
                            MAX_DESCRIPTION_LENGTH
                    )
                    .trim()
                    + "…";
        }
    }
}