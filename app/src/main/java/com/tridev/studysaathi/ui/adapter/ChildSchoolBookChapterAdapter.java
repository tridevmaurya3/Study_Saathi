package com.tridev.studysaathi.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.data.local.entity
        .SchoolBookChapterEntity;
import com.tridev.studysaathi.databinding
        .ItemChildSchoolBookChapterBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChildSchoolBookChapterAdapter
        extends RecyclerView.Adapter<
        ChildSchoolBookChapterAdapter.ChapterViewHolder> {

    @NonNull
    private final List<SchoolBookChapterEntity> chapters =
            new ArrayList<>();

    @NonNull
    private final ChapterClickListener clickListener;

    public ChildSchoolBookChapterAdapter(
            @NonNull ChapterClickListener clickListener
    ) {
        this.clickListener =
                clickListener;

        setHasStableIds(
                true
        );
    }

    public void submitChapters(
            @NonNull List<SchoolBookChapterEntity> newChapters
    ) {
        chapters.clear();

        for (SchoolBookChapterEntity chapter : newChapters) {
            if (chapter == null
                    || !chapter.isReadyForChildMode()) {

                continue;
            }

            chapters.add(
                    chapter
            );
        }

        notifyDataSetChanged();
    }

    public void clearChapters() {
        if (chapters.isEmpty()) {
            return;
        }

        chapters.clear();

        notifyDataSetChanged();
    }

    @NonNull
    public List<SchoolBookChapterEntity> getCurrentChapters() {
        return Collections.unmodifiableList(
                new ArrayList<>(
                        chapters
                )
        );
    }

    @Override
    public long getItemId(
            int position
    ) {
        SchoolBookChapterEntity chapter =
                chapters.get(
                        position
                );

        if (chapter.getChapterRowId()
                > 0L) {

            return chapter.getChapterRowId();
        }

        return chapter.getChapterId()
                .hashCode();
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemChildSchoolBookChapterBinding binding =
                ItemChildSchoolBookChapterBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new ChapterViewHolder(
                binding
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull ChapterViewHolder holder,
            int position
    ) {
        holder.bind(
                chapters.get(
                        position
                )
        );
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    public final class ChapterViewHolder
            extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemChildSchoolBookChapterBinding binding;

        private ChapterViewHolder(
                @NonNull ItemChildSchoolBookChapterBinding binding
        ) {
            super(
                    binding.getRoot()
            );

            this.binding =
                    binding;
        }

        private void bind(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            bindNumberAndType(
                    chapter
            );

            bindTitles(
                    chapter
            );

            bindPageRange(
                    chapter
            );

            bindProgress(
                    chapter
            );

            bindSpecialStatus(
                    chapter
            );

            binding.childChapterCard
                    .setOnClickListener(view ->
                            clickListener.onChapterClicked(
                                    chapter
                            )
                    );
        }

        private void bindNumberAndType(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            String chapterNumber =
                    chapter.getChapterNumber()
                            .trim();

            if (chapterNumber.isEmpty()) {
                chapterNumber =
                        String.valueOf(
                                Math.max(
                                        1,
                                        chapter.getSortOrder()
                                )
                        );
            }

            binding.childChapterNumberTextView
                    .setText(
                            chapterNumber
                    );

            binding.childChapterTypeTextView
                    .setText(
                            chapter.getChapterLabel()
                    );
        }

        private void bindTitles(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            binding.childChapterTitleTextView
                    .setText(
                            chapter.getDisplayTitle()
                    );

            String secondaryTitle =
                    chapter.getSecondaryTitle()
                            .trim();

            setTextOrHide(
                    binding.childChapterSecondaryTitleTextView,
                    secondaryTitle
            );
        }

        private void bindPageRange(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            setTextOrHide(
                    binding.childChapterPageRangeTextView,
                    chapter.getPageRangeLabel()
            );
        }

        private void bindProgress(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            int progressPercent =
                    Math.max(
                            0,
                            Math.min(
                                    100,
                                    chapter.getProgressPercent()
                            )
                    );

            binding.childChapterProgressTextView
                    .setText(
                            progressPercent
                                    + "%"
                    );

            binding.childChapterProgressBar
                    .setProgress(
                            progressPercent
                    );
        }

        private void bindSpecialStatus(
                @NonNull SchoolBookChapterEntity chapter
        ) {
            String status =
                    "";

            if (chapter.isOptionalChapter()
                    && chapter.isRevisionChapter()) {

                status =
                        "Optional • Revision chapter";

            } else if (chapter.isOptionalChapter()) {
                status =
                        "Optional chapter";

            } else if (chapter.isRevisionChapter()) {
                status =
                        "Revision chapter";
            }

            setTextOrHide(
                    binding.childChapterSpecialStatusTextView,
                    status
            );
        }

        private void setTextOrHide(
                @NonNull android.widget.TextView textView,
                @NonNull String text
        ) {
            String safeText =
                    text.trim();

            if (safeText.isEmpty()) {
                textView.setText(
                        ""
                );

                textView.setVisibility(
                        View.GONE
                );

            } else {
                textView.setText(
                        safeText
                );

                textView.setVisibility(
                        View.VISIBLE
                );
            }
        }
    }

    public interface ChapterClickListener {

        void onChapterClicked(
                @NonNull SchoolBookChapterEntity chapter
        );
    }
}