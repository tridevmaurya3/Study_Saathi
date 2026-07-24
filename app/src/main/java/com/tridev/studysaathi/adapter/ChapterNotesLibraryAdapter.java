package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.databinding.ItemChapterNoteLibraryBinding;
import com.tridev.studysaathi.model.ChapterNoteItem;

import java.util.ArrayList;
import java.util.List;

public class ChapterNotesLibraryAdapter
        extends RecyclerView.Adapter<
        ChapterNotesLibraryAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(
                @NonNull ChapterNoteItem noteItem
        );
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(
                @NonNull ChapterNoteItem noteItem
        );
    }

    private final List<ChapterNoteItem> noteItems =
            new ArrayList<>();

    @NonNull
    private final OnNoteClickListener
            noteClickListener;

    @NonNull
    private final OnDeleteClickListener
            deleteClickListener;

    public ChapterNotesLibraryAdapter(
            @NonNull List<ChapterNoteItem> initialItems,
            @NonNull OnNoteClickListener noteClickListener,
            @NonNull OnDeleteClickListener deleteClickListener
    ) {
        noteItems.addAll(initialItems);

        this.noteClickListener =
                noteClickListener;

        this.deleteClickListener =
                deleteClickListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemChapterNoteLibraryBinding binding =
                ItemChapterNoteLibraryBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull NoteViewHolder holder,
            int position
    ) {
        holder.bind(
                noteItems.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return noteItems.size();
    }

    public void submitList(
            @NonNull List<ChapterNoteItem> updatedItems
    ) {
        noteItems.clear();
        noteItems.addAll(updatedItems);
        notifyDataSetChanged();
    }

    class NoteViewHolder
            extends RecyclerView.ViewHolder {

        private static final int PREVIEW_LENGTH =
                180;

        private final ItemChapterNoteLibraryBinding
                binding;

        NoteViewHolder(
                @NonNull ItemChapterNoteLibraryBinding binding
        ) {
            super(binding.getRoot());

            this.binding = binding;
        }

        void bind(
                @NonNull ChapterNoteItem noteItem
        ) {
            binding.textLibraryNoteSubject.setText(
                    noteItem.getSubjectName()
            );

            binding.textLibraryNoteChapter.setText(
                    noteItem.getChapterTitle()
            );

            binding.textLibraryNotePreview.setText(
                    createPreview(
                            noteItem.getNoteText()
                    )
            );

            binding.getRoot().setOnClickListener(view ->
                    noteClickListener.onNoteClick(
                            noteItem
                    )
            );

            binding.buttonOpenLibraryNote
                    .setOnClickListener(view ->
                            noteClickListener.onNoteClick(
                                    noteItem
                            )
                    );

            binding.buttonDeleteLibraryNote
                    .setOnClickListener(view ->
                            deleteClickListener.onDeleteClick(
                                    noteItem
                            )
                    );
        }

        @NonNull
        private String createPreview(
                @NonNull String noteText
        ) {
            String cleanText =
                    noteText
                            .replaceAll("\\s+", " ")
                            .trim();

            if (cleanText.length()
                    <= PREVIEW_LENGTH) {
                return cleanText;
            }

            return cleanText
                    .substring(
                            0,
                            PREVIEW_LENGTH
                    )
                    .trim()
                    + "…";
        }
    }
}