package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.databinding.ItemChapterBookmarkBinding;
import com.tridev.studysaathi.model.ChapterBookmarkItem;

import java.util.ArrayList;
import java.util.List;

public class ChapterBookmarksAdapter
        extends RecyclerView.Adapter<
        ChapterBookmarksAdapter.BookmarkViewHolder> {

    public interface OnOpenBookmarkListener {
        void onOpenBookmark(
                @NonNull ChapterBookmarkItem bookmarkItem
        );
    }

    public interface OnRemoveBookmarkListener {
        void onRemoveBookmark(
                @NonNull ChapterBookmarkItem bookmarkItem
        );
    }

    private final List<ChapterBookmarkItem> bookmarkItems =
            new ArrayList<>();

    @NonNull
    private final OnOpenBookmarkListener
            openBookmarkListener;

    @NonNull
    private final OnRemoveBookmarkListener
            removeBookmarkListener;

    public ChapterBookmarksAdapter(
            @NonNull List<ChapterBookmarkItem> initialItems,
            @NonNull OnOpenBookmarkListener openBookmarkListener,
            @NonNull OnRemoveBookmarkListener removeBookmarkListener
    ) {
        bookmarkItems.addAll(initialItems);

        this.openBookmarkListener =
                openBookmarkListener;

        this.removeBookmarkListener =
                removeBookmarkListener;
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemChapterBookmarkBinding binding =
                ItemChapterBookmarkBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new BookmarkViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BookmarkViewHolder holder,
            int position
    ) {
        holder.bind(
                bookmarkItems.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return bookmarkItems.size();
    }

    public void submitList(
            @NonNull List<ChapterBookmarkItem> updatedItems
    ) {
        bookmarkItems.clear();
        bookmarkItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    class BookmarkViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemChapterBookmarkBinding
                binding;

        BookmarkViewHolder(
                @NonNull ItemChapterBookmarkBinding binding
        ) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(
                @NonNull ChapterBookmarkItem bookmarkItem
        ) {
            binding.textBookmarkSubject.setText(
                    bookmarkItem.getSubjectName()
            );

            binding.textBookmarkChapter.setText(
                    bookmarkItem.getChapterTitle()
            );

            String description =
                    bookmarkItem.getChapterDescription();

            if (description.trim().isEmpty()) {
                binding.textBookmarkDescription.setText(
                        binding.getRoot()
                                .getContext()
                                .getString(
                                        com.tridev.studysaathi.R.string
                                                .bookmarks_default_description
                                )
                );
            } else {
                binding.textBookmarkDescription.setText(
                        description
                );
            }

            binding.getRoot().setOnClickListener(view ->
                    openBookmarkListener.onOpenBookmark(
                            bookmarkItem
                    )
            );

            binding.buttonOpenBookmarkLesson
                    .setOnClickListener(view ->
                            openBookmarkListener.onOpenBookmark(
                                    bookmarkItem
                            )
                    );

            binding.buttonRemoveBookmark
                    .setOnClickListener(view ->
                            removeBookmarkListener.onRemoveBookmark(
                                    bookmarkItem
                            )
                    );
        }
    }
}