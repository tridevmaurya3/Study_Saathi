package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.databinding.ItemBookmarkBinding;
import com.tridev.studysaathi.model.BookmarkItem;

import java.util.ArrayList;
import java.util.List;

public class BookmarksAdapter
        extends RecyclerView.Adapter<
        BookmarksAdapter.BookmarkViewHolder> {

    public interface OnBookmarkOpenListener {
        void onBookmarkOpen(
                @NonNull BookmarkItem bookmarkItem
        );
    }

    public interface OnBookmarkRemoveListener {
        void onBookmarkRemove(
                @NonNull BookmarkItem bookmarkItem
        );
    }

    private final List<BookmarkItem> bookmarkItems =
            new ArrayList<>();

    @NonNull
    private final OnBookmarkOpenListener
            bookmarkOpenListener;

    @NonNull
    private final OnBookmarkRemoveListener
            bookmarkRemoveListener;

    public BookmarksAdapter(
            @NonNull List<BookmarkItem> initialItems,
            @NonNull OnBookmarkOpenListener bookmarkOpenListener,
            @NonNull OnBookmarkRemoveListener bookmarkRemoveListener
    ) {
        bookmarkItems.addAll(initialItems);

        this.bookmarkOpenListener =
                bookmarkOpenListener;

        this.bookmarkRemoveListener =
                bookmarkRemoveListener;
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemBookmarkBinding binding =
                ItemBookmarkBinding.inflate(
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
            @NonNull List<BookmarkItem> updatedItems
    ) {
        bookmarkItems.clear();
        bookmarkItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    class BookmarkViewHolder
            extends RecyclerView.ViewHolder {

        private static final int MAX_DESCRIPTION_LENGTH =
                190;

        private final ItemBookmarkBinding binding;

        BookmarkViewHolder(
                @NonNull ItemBookmarkBinding binding
        ) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(
                @NonNull BookmarkItem bookmarkItem
        ) {
            binding.textBookmarkSubject.setText(
                    bookmarkItem.getSubjectName()
            );

            binding.textBookmarkChapter.setText(
                    bookmarkItem.getChapterTitle()
            );

            binding.textBookmarkDescription.setText(
                    createDescriptionPreview(
                            bookmarkItem
                                    .getChapterDescription()
                    )
            );

            binding.getRoot().setOnClickListener(view ->
                    bookmarkOpenListener.onBookmarkOpen(
                            bookmarkItem
                    )
            );

            binding.buttonOpenBookmark.setOnClickListener(view ->
                    bookmarkOpenListener.onBookmarkOpen(
                            bookmarkItem
                    )
            );

            binding.buttonRemoveBookmark.setOnClickListener(view ->
                    bookmarkRemoveListener.onBookmarkRemove(
                            bookmarkItem
                    )
            );
        }

        @NonNull
        private String createDescriptionPreview(
                @NonNull String chapterDescription
        ) {
            String cleanDescription =
                    chapterDescription
                            .replaceAll("\\s+", " ")
                            .trim();

            if (cleanDescription.isEmpty()) {
                return "Open this chapter to continue learning.";
            }

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