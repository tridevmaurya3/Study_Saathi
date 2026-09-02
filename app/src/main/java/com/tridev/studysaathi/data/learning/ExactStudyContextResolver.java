package com.tridev.studysaathi.data.learning;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;
import com.tridev.studysaathi.data.local.entity.StudentProfileEntity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Builds the exact active-study graph used by Smart Tutor grounding:
 * Student -> Board -> Class -> School Medium -> Subject -> Book -> Chapter -> Page.
 *
 * It is intentionally read-only. Student metadata is cached on the existing
 * Room background executor, while exact page evidence continues to come only
 * from parent-approved pages indexed by {@link PhotoBookContextIndex}.
 */
public final class ExactStudyContextResolver {

    private static final long PROFILE_REFRESH_INTERVAL_MS = 60_000L;
    private static final int MIN_TEXT_CONTEXT_LENGTH = 18;
    private static final int MIN_TEXT_CONTEXT_WORDS = 5;

    private static final AtomicBoolean PROFILE_LOAD_IN_PROGRESS =
            new AtomicBoolean(false);

    @Nullable
    private static volatile Context applicationContext;

    @NonNull
    private static volatile StudentContext studentContext =
            StudentContext.empty();

    private static volatile long lastProfileLoadedAt;

    private ExactStudyContextResolver() { }

    public static void initialize(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
        refreshStudentContext();
    }

    public static void refreshStudentContext() {
        Context context = applicationContext;
        if (context == null
                || !PROFILE_LOAD_IN_PROGRESS.compareAndSet(false, true)) {
            return;
        }

        StudySaathiDatabase.databaseWriteExecutor.execute(() -> {
            StudentContext next = StudentContext.empty();
            try {
                StudentProfileEntity profile = StudySaathiDatabase
                        .getInstance(context)
                        .studentProfileDao()
                        .getActiveProfile();

                if (profile != null && profile.getProfileId() > 0L) {
                    next = new StudentContext(
                            safe(profile.getStudentName()),
                            safe(profile.getEducationBoard()),
                            safe(profile.getStudentClass()),
                            safe(profile.getStudyMedium())
                    );
                }
            } catch (RuntimeException ignored) {
                // Exact context is an enhancement; normal tutoring must continue.
            } finally {
                studentContext = next;
                lastProfileLoadedAt = System.currentTimeMillis();
                PROFILE_LOAD_IN_PROGRESS.set(false);
            }
        });
    }

    @NonNull
    public static Decision resolve(
            @Nullable String question,
            boolean imageAttached
    ) {
        refreshIfStale();

        String cleanQuestion = safe(question);
        if (!imageAttached
                && (cleanQuestion.length() < MIN_TEXT_CONTEXT_LENGTH
                || countWords(cleanQuestion) < MIN_TEXT_CONTEXT_WORDS)) {
            PhotoBookContextIndex.clearLatestMatch();
            return Decision.none();
        }

        PhotoBookContextIndex.MatchResult match =
                PhotoBookContextIndex.matchImageQuestion(cleanQuestion);

        if (!match.hasVerifiedPage()) {
            if (!imageAttached) {
                PhotoBookContextIndex.clearLatestMatch();
            }
            return Decision.none();
        }

        StudentContext context = studentContext;
        StringBuilder graph = new StringBuilder();
        graph.append("EXACT STUDY CONTEXT GRAPH: ");
        appendGraphPart(graph, "Student", context.studentName);
        appendGraphPart(graph, "Board", context.educationBoard);
        appendGraphPart(graph, "Class", context.studentClass);
        appendGraphPart(graph, "School Medium", context.studyMedium);

        if (graph.charAt(graph.length() - 1) == ' ') {
            graph.append("active approved school book context");
        }

        graph.append(".\n")
                .append("Use the exact graph only when verified page evidence exists. ")
                .append("School Medium controls book context; explanation language remains independent.\n")
                .append(match.getPromptInstruction());

        return new Decision(true, graph.toString().trim());
    }

    private static void refreshIfStale() {
        if (applicationContext == null) {
            return;
        }
        long age = System.currentTimeMillis() - lastProfileLoadedAt;
        if (lastProfileLoadedAt <= 0L || age >= PROFILE_REFRESH_INTERVAL_MS) {
            refreshStudentContext();
        }
    }

    private static void appendGraphPart(
            @NonNull StringBuilder graph,
            @NonNull String label,
            @Nullable String value
    ) {
        String clean = safe(value);
        if (clean.isEmpty()) {
            return;
        }
        if (graph.charAt(graph.length() - 1) != ' ') {
            graph.append(" -> ");
        }
        graph.append(label).append('=').append(clean);
    }

    private static int countWords(@NonNull String text) {
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return 0;
        return normalized.split(" ").length;
    }

    @NonNull
    private static String safe(@Nullable Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public static final class Decision {
        private final boolean verifiedExactContext;
        @NonNull private final String promptInstruction;

        private Decision(
                boolean verifiedExactContext,
                @NonNull String promptInstruction
        ) {
            this.verifiedExactContext = verifiedExactContext;
            this.promptInstruction = promptInstruction;
        }

        @NonNull
        static Decision none() {
            return new Decision(false, "");
        }

        public boolean hasVerifiedExactContext() {
            return verifiedExactContext && !promptInstruction.isEmpty();
        }

        @NonNull
        public String getPromptInstruction() {
            return promptInstruction;
        }
    }

    private static final class StudentContext {
        @NonNull private final String studentName;
        @NonNull private final String educationBoard;
        @NonNull private final String studentClass;
        @NonNull private final String studyMedium;

        private StudentContext(
                @NonNull String studentName,
                @NonNull String educationBoard,
                @NonNull String studentClass,
                @NonNull String studyMedium
        ) {
            this.studentName = studentName;
            this.educationBoard = educationBoard;
            this.studentClass = studentClass;
            this.studyMedium = studyMedium;
        }

        @NonNull
        static StudentContext empty() {
            return new StudentContext("", "", "", "");
        }
    }
}
