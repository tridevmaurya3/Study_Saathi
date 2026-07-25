package com.tridev.studysaathi.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tridev.studysaathi.data.local.entity.SchoolSubjectEntity;
import com.tridev.studysaathi.databinding.ItemSchoolCurriculumSubjectBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SchoolCurriculumSubjectAdapter
        extends RecyclerView.Adapter<
        SchoolCurriculumSubjectAdapter.SubjectViewHolder> {

    @NonNull
    private final List<SchoolSubjectEntity> subjects;

    @NonNull
    private final SubjectActionListener actionListener;

    public SchoolCurriculumSubjectAdapter(
            @NonNull SubjectActionListener actionListener
    ) {
        this.subjects =
                new ArrayList<>();

        this.actionListener =
                actionListener;

        setHasStableIds(
                true
        );
    }

    /**
     * Adapter की पूरी subject list safely replace करता है।
     */
    public void submitList(
            @NonNull List<SchoolSubjectEntity> newSubjects
    ) {
        subjects.clear();

        subjects.addAll(
                newSubjects
        );

        notifyDataSetChanged();
    }

    /**
     * नई subject को list के अंत में जोड़ता है।
     */
    public void addSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        int existingPosition =
                findSubjectPosition(
                        schoolSubject.getSubjectRowId()
                );

        if (existingPosition >= 0) {
            subjects.set(
                    existingPosition,
                    schoolSubject
            );

            notifyItemChanged(
                    existingPosition
            );

            return;
        }

        subjects.add(
                schoolSubject
        );

        notifyItemInserted(
                subjects.size() - 1
        );
    }

    /**
     * Existing subject item update करता है।
     */
    public void updateSubject(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        int subjectPosition =
                findSubjectPosition(
                        schoolSubject.getSubjectRowId()
                );

        if (subjectPosition < 0) {
            addSubject(
                    schoolSubject
            );

            return;
        }

        subjects.set(
                subjectPosition,
                schoolSubject
        );

        notifyItemChanged(
                subjectPosition
        );
    }

    /**
     * Subject को list से हटाता है।
     */
    public void removeSubject(
            long subjectRowId
    ) {
        int subjectPosition =
                findSubjectPosition(
                        subjectRowId
                );

        if (subjectPosition < 0) {
            return;
        }

        subjects.remove(
                subjectPosition
        );

        notifyItemRemoved(
                subjectPosition
        );
    }

    @NonNull
    public List<SchoolSubjectEntity> getCurrentSubjects() {
        return new ArrayList<>(
                subjects
        );
    }

    public int getSelectedSubjectCount() {
        int selectedCount =
                0;

        for (SchoolSubjectEntity schoolSubject :
                subjects) {

            if (schoolSubject.isEnabled()) {
                selectedCount++;
            }
        }

        return selectedCount;
    }

    public boolean hasSubjects() {
        return !subjects.isEmpty();
    }

    @Override
    public long getItemId(
            int position
    ) {
        SchoolSubjectEntity schoolSubject =
                subjects.get(
                        position
                );

        long subjectRowId =
                schoolSubject.getSubjectRowId();

        if (subjectRowId > 0L) {
            return subjectRowId;
        }

        String subjectId =
                safeText(
                        schoolSubject.getSubjectId()
                );

        return subjectId.isEmpty()
                ? position
                : subjectId.hashCode();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSchoolCurriculumSubjectBinding binding =
                ItemSchoolCurriculumSubjectBinding.inflate(
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
                subjects.get(
                        position
                )
        );
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    private int findSubjectPosition(
            long subjectRowId
    ) {
        if (subjectRowId <= 0L) {
            return -1;
        }

        for (int index = 0;
             index < subjects.size();
             index++) {

            SchoolSubjectEntity schoolSubject =
                    subjects.get(
                            index
                    );

            if (schoolSubject.getSubjectRowId()
                    == subjectRowId) {

                return index;
            }
        }

        return -1;
    }

    @NonNull
    private static String createSubjectSecondaryText(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String hindiName =
                safeText(
                        schoolSubject.getSubjectNameHindi()
                );

        String subjectCode =
                safeText(
                        schoolSubject.getSubjectCode()
                );

        StringBuilder secondaryText =
                new StringBuilder();

        if (!hindiName.isEmpty()
                && !hindiName.equalsIgnoreCase(
                safeText(
                        schoolSubject
                                .getSubjectNameEnglish()
                )
        )) {
            secondaryText.append(
                    hindiName
            );
        }

        if (!subjectCode.isEmpty()) {
            if (secondaryText.length() > 0) {
                secondaryText.append(
                        "  •  "
                );
            }

            secondaryText.append(
                    subjectCode
            );
        }

        if (secondaryText.length() == 0) {
            secondaryText.append(
                    "School Subject"
            );
        }

        return secondaryText.toString();
    }

    @NonNull
    private static String createSubjectIconText(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String englishName =
                safeText(
                        schoolSubject.getSubjectNameEnglish()
                );

        String hindiName =
                safeText(
                        schoolSubject.getSubjectNameHindi()
                );

        String preferredName =
                !englishName.isEmpty()
                        ? englishName
                        : hindiName;

        if (preferredName.isEmpty()) {
            return "S";
        }

        int firstCodePoint =
                preferredName.codePointAt(
                        0
                );

        return new String(
                Character.toChars(
                        firstCodePoint
                )
        ).toUpperCase(
                Locale.getDefault()
        );
    }

    @NonNull
    private static String createBookDetailsText(
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String publisherName =
                safeText(
                        schoolSubject.getPublisherName()
                );

        String bookCode =
                safeText(
                        schoolSubject.getBookCode()
                );

        StringBuilder bookDetails =
                new StringBuilder();

        if (!publisherName.isEmpty()) {
            bookDetails.append(
                    publisherName
            );
        }

        if (!bookCode.isEmpty()) {
            if (bookDetails.length() > 0) {
                bookDetails.append(
                        "  •  "
                );
            }

            bookDetails.append(
                    bookCode
            );
        }

        if (bookDetails.length() == 0) {
            return "Cover या ISBN scan करके actual book जोड़ें";
        }

        return bookDetails.toString();
    }

    @NonNull
    private static String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    public interface SubjectActionListener {

        void onSubjectSelected(
                @NonNull SchoolSubjectEntity schoolSubject
        );

        void onScanBookClicked(
                @NonNull SchoolSubjectEntity schoolSubject
        );

        void onEditSubjectClicked(
                @NonNull SchoolSubjectEntity schoolSubject
        );

        void onRemoveSubjectClicked(
                @NonNull SchoolSubjectEntity schoolSubject
        );
    }

    final class SubjectViewHolder
            extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemSchoolCurriculumSubjectBinding
                binding;

        private SubjectViewHolder(
                @NonNull ItemSchoolCurriculumSubjectBinding
                        binding
        ) {
            super(
                    binding.getRoot()
            );

            this.binding =
                    binding;
        }

        private void bind(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            String englishSubjectName =
                    safeText(
                            schoolSubject
                                    .getSubjectNameEnglish()
                    );

            String hindiSubjectName =
                    safeText(
                            schoolSubject
                                    .getSubjectNameHindi()
                    );

            String displaySubjectName =
                    !englishSubjectName.isEmpty()
                            ? englishSubjectName
                            : hindiSubjectName;

            if (displaySubjectName.isEmpty()) {
                displaySubjectName =
                        "Unnamed Subject";
            }

            binding.textCurriculumSubjectIcon
                    .setText(
                            createSubjectIconText(
                                    schoolSubject
                            )
                    );

            binding.textCurriculumSubjectName
                    .setText(
                            displaySubjectName
                    );

            binding.textCurriculumSubjectSecondaryName
                    .setText(
                            createSubjectSecondaryText(
                                    schoolSubject
                            )
                    );

            bindSubjectStatus(
                    schoolSubject
            );

            bindBookStatus(
                    schoolSubject
            );

            binding.cardCurriculumSubjectItem
                    .setOnClickListener(view ->
                            actionListener
                                    .onSubjectSelected(
                                            schoolSubject
                                    )
                    );

            binding.buttonScanCurriculumSubjectBook
                    .setOnClickListener(view ->
                            actionListener
                                    .onScanBookClicked(
                                            schoolSubject
                                    )
                    );

            binding.buttonEditCurriculumSubject
                    .setOnClickListener(view ->
                            actionListener
                                    .onEditSubjectClicked(
                                            schoolSubject
                                    )
                    );

            binding.buttonRemoveCurriculumSubject
                    .setOnClickListener(view ->
                            actionListener
                                    .onRemoveSubjectClicked(
                                            schoolSubject
                                    )
                    );
        }

        private void bindSubjectStatus(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            boolean subjectEnabled =
                    schoolSubject.isEnabled();

            binding.textCurriculumSubjectStatus
                    .setText(
                            subjectEnabled
                                    ? "Active"
                                    : "Hidden"
                    );

            binding.cardCurriculumSubjectItem
                    .setAlpha(
                            subjectEnabled
                                    ? 1.0f
                                    : 0.65f
                    );
        }

        private void bindBookStatus(
                @NonNull SchoolSubjectEntity schoolSubject
        ) {
            String bookName =
                    safeText(
                            schoolSubject.getBookName()
                    );

            boolean exactBookAvailable =
                    !bookName.isEmpty();

            if (!exactBookAvailable) {
                binding.textCurriculumSubjectBookName
                        .setText(
                                "Exact school book अभी नहीं जोड़ी गई"
                        );

                binding.textCurriculumSubjectBookDetails
                        .setText(
                                "Cover या ISBN scan करके actual book जोड़ें"
                        );

                binding.textCurriculumSubjectBookStatus
                        .setText(
                                "Pending"
                        );

                binding.buttonScanCurriculumSubjectBook
                        .setText(
                                "Scan Book"
                        );

                return;
            }

            binding.textCurriculumSubjectBookName
                    .setText(
                            bookName
                    );

            binding.textCurriculumSubjectBookDetails
                    .setText(
                            createBookDetailsText(
                                    schoolSubject
                            )
                    );

            binding.textCurriculumSubjectBookStatus
                    .setText(
                            "Confirmed"
                    );

            binding.buttonScanCurriculumSubjectBook
                    .setText(
                            "Change Book"
                    );
        }
    }
}