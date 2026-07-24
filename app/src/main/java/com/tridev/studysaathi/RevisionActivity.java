package com.tridev.studysaathi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.tridev.studysaathi.adapter.RevisionAdapter;
import com.tridev.studysaathi.data.catalog.ChapterCatalog;
import com.tridev.studysaathi.data.local.entity.LessonProgressEntity;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;
import com.tridev.studysaathi.data.repository.LessonProgressRepository;
import com.tridev.studysaathi.data.repository.StudentProfileRepository;
import com.tridev.studysaathi.databinding.ActivityRevisionBinding;
import com.tridev.studysaathi.model.ChapterItem;
import com.tridev.studysaathi.model.RevisionItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RevisionActivity extends AppCompatActivity {

    private static final int[] REVISION_INTERVALS = {
            1,
            3,
            7,
            14,
            30
    };

    private ActivityRevisionBinding binding;

    private StudentProfileRepository studentProfileRepository;
    private LessonProgressRepository lessonProgressRepository;
    private RevisionAdapter revisionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRevisionBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        studentProfileRepository =
                new StudentProfileRepository(this);

        lessonProgressRepository =
                new LessonProgressRepository(this);

        setupRecyclerView();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRevisionPlan();
    }

    private void setupRecyclerView() {
        revisionAdapter = new RevisionAdapter(
                new ArrayList<>(),
                this::openRevisionLesson
        );

        binding.recyclerRevision.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.recyclerRevision.setAdapter(
                revisionAdapter
        );

        binding.recyclerRevision.setHasFixedSize(false);
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.buttonBrowseSubjects.setOnClickListener(view ->
                openSubjects()
        );
    }

    private void loadRevisionPlan() {
        showLoadingState(true);

        studentProfileRepository.getActiveProfile(
                new StudentProfileRepository.SingleProfileCallback() {
                    @Override
                    public void onSuccess(
                            StudentProfileEntity studentProfile
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (studentProfile == null) {
                            showLoadingState(false);
                            showNoProfileState();
                            return;
                        }

                        showStudentInformation(
                                studentProfile
                        );

                        loadStudentProgress(
                                studentProfile
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);
                        showNoProfileState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.revision_profile_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showStudentInformation(
            @NonNull StudentProfileEntity studentProfile
    ) {
        binding.textRevisionStudent.setText(
                getString(
                        R.string.revision_for_student_format,
                        studentProfile.getStudentName(),
                        studentProfile.getEducationBoard(),
                        studentProfile.getStudentClass()
                )
        );
    }

    private void loadStudentProgress(
            @NonNull StudentProfileEntity studentProfile
    ) {
        lessonProgressRepository.getProgressForProfile(
                studentProfile.getProfileId(),
                new LessonProgressRepository.ProgressListCallback() {
                    @Override
                    public void onSuccess(
                            @NonNull List<LessonProgressEntity> progressList
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);

                        List<RevisionItem> revisionItems =
                                buildRevisionPlan(progressList);

                        showRevisionItems(revisionItems);
                    }

                    @Override
                    public void onError(
                            @NonNull Exception exception
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadingState(false);
                        showEmptyRevisionState();

                        Snackbar.make(
                                binding.getRoot(),
                                R.string.revision_progress_load_failed,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    @NonNull
    private List<RevisionItem> buildRevisionPlan(
            @NonNull List<LessonProgressEntity> progressList
    ) {
        List<RevisionItem> revisionItems =
                new ArrayList<>();

        LocalDate today =
                LocalDate.now(ZoneId.systemDefault());

        for (LessonProgressEntity progress : progressList) {
            if (!progress.isCompleted()) {
                continue;
            }

            LocalDate nextRevisionDate =
                    calculateNextRevisionDate(progress);

            RevisionItem.RevisionStatus status =
                    determineRevisionStatus(
                            nextRevisionDate,
                            today
                    );

            revisionItems.add(
                    new RevisionItem(
                            progress.getEducationBoard(),
                            progress.getStudentClass(),
                            progress.getSubjectName(),
                            progress.getChapterTitle(),
                            nextRevisionDate,
                            status
                    )
            );
        }

        sortRevisionItems(revisionItems);

        return revisionItems;
    }

    @NonNull
    private LocalDate calculateNextRevisionDate(
            @NonNull LessonProgressEntity progress
    ) {
        long completionTime =
                progress.getCompletedAt() > 0L
                        ? progress.getCompletedAt()
                        : progress.getLastStudiedAt();

        LocalDate completionDate =
                toLocalDate(completionTime);

        int revisionCount =
                Math.max(0, progress.getRevisionCount());

        if (revisionCount == 0) {
            return completionDate.plusDays(
                    REVISION_INTERVALS[0]
            );
        }

        long revisionTime =
                progress.getLastRevisedAt() > 0L
                        ? progress.getLastRevisedAt()
                        : completionTime;

        LocalDate lastRevisionDate =
                toLocalDate(revisionTime);

        int intervalIndex = Math.min(
                revisionCount,
                REVISION_INTERVALS.length - 1
        );

        return lastRevisionDate.plusDays(
                REVISION_INTERVALS[intervalIndex]
        );
    }

    @NonNull
    private LocalDate toLocalDate(long timeMillis) {
        if (timeMillis <= 0L) {
            return LocalDate.now(
                    ZoneId.systemDefault()
            );
        }

        return Instant.ofEpochMilli(timeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    @NonNull
    private RevisionItem.RevisionStatus determineRevisionStatus(
            @NonNull LocalDate revisionDate,
            @NonNull LocalDate today
    ) {
        if (revisionDate.isBefore(today)) {
            return RevisionItem.RevisionStatus.OVERDUE;
        }

        if (revisionDate.isEqual(today)) {
            return RevisionItem.RevisionStatus.DUE_TODAY;
        }

        return RevisionItem.RevisionStatus.UPCOMING;
    }

    private void sortRevisionItems(
            @NonNull List<RevisionItem> items
    ) {
        Collections.sort(
                items,
                new Comparator<RevisionItem>() {
                    @Override
                    public int compare(
                            RevisionItem first,
                            RevisionItem second
                    ) {
                        int firstPriority =
                                getStatusPriority(
                                        first.getRevisionStatus()
                                );

                        int secondPriority =
                                getStatusPriority(
                                        second.getRevisionStatus()
                                );

                        if (firstPriority != secondPriority) {
                            return Integer.compare(
                                    firstPriority,
                                    secondPriority
                            );
                        }

                        int dateComparison =
                                first.getNextRevisionDate()
                                        .compareTo(
                                                second.getNextRevisionDate()
                                        );

                        if (dateComparison != 0) {
                            return dateComparison;
                        }

                        return first.getSubjectName()
                                .compareToIgnoreCase(
                                        second.getSubjectName()
                                );
                    }
                }
        );
    }

    private int getStatusPriority(
            @NonNull RevisionItem.RevisionStatus status
    ) {
        switch (status) {
            case OVERDUE:
                return 0;

            case DUE_TODAY:
                return 1;

            case UPCOMING:
            default:
                return 2;
        }
    }

    private void showRevisionItems(
            @NonNull List<RevisionItem> revisionItems
    ) {
        revisionAdapter.submitList(revisionItems);

        int dueNowCount = 0;
        int upcomingCount = 0;

        for (RevisionItem item : revisionItems) {
            if (item.getRevisionStatus()
                    == RevisionItem.RevisionStatus.UPCOMING) {
                upcomingCount++;
            } else {
                dueNowCount++;
            }
        }

        binding.textDueNowValue.setText(
                String.valueOf(dueNowCount)
        );

        binding.textUpcomingValue.setText(
                String.valueOf(upcomingCount)
        );

        boolean itemsAvailable =
                !revisionItems.isEmpty();

        binding.recyclerRevision.setVisibility(
                itemsAvailable
                        ? View.VISIBLE
                        : View.GONE
        );

        binding.cardEmptyRevision.setVisibility(
                itemsAvailable
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private void showEmptyRevisionState() {
        revisionAdapter.submitList(
                new ArrayList<>()
        );

        binding.textDueNowValue.setText("0");
        binding.textUpcomingValue.setText("0");

        binding.recyclerRevision.setVisibility(
                View.GONE
        );

        binding.cardEmptyRevision.setVisibility(
                View.VISIBLE
        );
    }

    private void showNoProfileState() {
        binding.textRevisionStudent.setText(
                R.string.create_profile_to_continue
        );

        showEmptyRevisionState();
    }

    private void openRevisionLesson(
            @NonNull RevisionItem revisionItem
    ) {
        Intent lessonIntent = new Intent(
                RevisionActivity.this,
                LessonActivity.class
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_SUBJECT_NAME,
                revisionItem.getSubjectName()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_TITLE,
                revisionItem.getChapterTitle()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_CHAPTER_DESCRIPTION,
                findChapterDescription(revisionItem)
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_STUDENT_CLASS,
                revisionItem.getStudentClass()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_EDUCATION_BOARD,
                revisionItem.getEducationBoard()
        );

        lessonIntent.putExtra(
                LessonActivity.EXTRA_REVISION_MODE,
                true
        );

        startActivity(lessonIntent);
    }

    @NonNull
    private String findChapterDescription(
            @NonNull RevisionItem revisionItem
    ) {
        List<ChapterItem> chapters =
                ChapterCatalog.getChapters(
                        revisionItem.getEducationBoard(),
                        revisionItem.getStudentClass(),
                        revisionItem.getSubjectName()
                );

        String targetChapter =
                normalizeText(
                        revisionItem.getChapterTitle()
                );

        for (ChapterItem chapterItem : chapters) {
            if (normalizeText(
                    chapterItem.getChapterTitle()
            ).equals(targetChapter)) {

                return chapterItem
                        .getChapterDescription();
            }
        }

        return getString(
                R.string.revision_default_chapter_description
        );
    }

    @NonNull
    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void openSubjects() {
        Intent subjectsIntent = new Intent(
                RevisionActivity.this,
                SubjectsActivity.class
        );

        startActivity(subjectsIntent);
    }

    private void showLoadingState(boolean loading) {
        binding.progressRevision.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        binding.contentRevision.setVisibility(
                loading ? View.INVISIBLE : View.VISIBLE
        );
    }
}