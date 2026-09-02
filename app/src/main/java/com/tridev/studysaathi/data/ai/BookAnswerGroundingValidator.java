package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.data.learning.PhotoBookContextIndex;

import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates answer page citations against approved book-page markers. */
public final class BookAnswerGroundingValidator {
    private static final Pattern APPROVED_PAGE = Pattern.compile(
            "\\[\\[VERIFIED_BOOK_PAGE\\s+page=(\\d+)(?:\\s|\\])");
    private static final Pattern ANSWER_CITATION = Pattern.compile(
            "(?:📖\\s*)?(?:पुस्तक\\s*पृष्ठ|book\\s*page|page)\\s*(?:number\\s*)?[:#-]?\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private BookAnswerGroundingValidator() { }

    @NonNull
    public static Result validate(@Nullable String answer, @Nullable String approvedReference) {
        String effectiveReference = approvedReference == null ? "" : approvedReference.trim();
        if (effectiveReference.isEmpty()) {
            effectiveReference = PhotoBookContextIndex.consumeLatestVerifiedReference();
        }

        Set<Integer> approvedPages = collect(APPROVED_PAGE, effectiveReference);
        if (approvedPages.isEmpty()) {
            return new Result(Status.NO_EXACT_EVIDENCE, approvedPages, Collections.emptySet());
        }

        Set<Integer> citedPages = collect(ANSWER_CITATION, answer);
        for (int citedPage : citedPages) {
            if (!approvedPages.contains(citedPage)) {
                return new Result(Status.UNSUPPORTED_PAGE_CITATION,
                        approvedPages, citedPages);
            }
        }
        if (citedPages.isEmpty()) {
            return new Result(Status.EVIDENCE_AVAILABLE_NOT_CITED,
                    approvedPages, citedPages);
        }
        return new Result(Status.GROUNDED, approvedPages, citedPages);
    }

    @NonNull
    private static Set<Integer> collect(@NonNull Pattern pattern, @Nullable String value) {
        Set<Integer> pages = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        while (matcher.find()) {
            try {
                int page = Integer.parseInt(matcher.group(1));
                if (page > 0) pages.add(page);
            } catch (NumberFormatException ignored) {
                // Invalid page values never become approved evidence.
            }
        }
        return pages;
    }

    public enum Status {
        NO_EXACT_EVIDENCE,
        EVIDENCE_AVAILABLE_NOT_CITED,
        GROUNDED,
        UNSUPPORTED_PAGE_CITATION
    }

    public static final class Result {
        @NonNull private final Status status;
        @NonNull private final Set<Integer> approvedPages;
        @NonNull private final Set<Integer> citedPages;

        private Result(@NonNull Status status, @NonNull Set<Integer> approvedPages,
                       @NonNull Set<Integer> citedPages) {
            this.status = status;
            this.approvedPages = Collections.unmodifiableSet(
                    new LinkedHashSet<>(approvedPages));
            this.citedPages = Collections.unmodifiableSet(new LinkedHashSet<>(citedPages));
        }

        @NonNull public Status getStatus() { return status; }
        public int getApprovedPageCount() { return approvedPages.size(); }
        public int getCitedPageCount() { return citedPages.size(); }
        @NonNull public Set<Integer> getApprovedPages() { return approvedPages; }
        @NonNull public Set<Integer> getCitedPages() { return citedPages; }
        public boolean isGrounded() { return status == Status.GROUNDED; }
        public boolean hasUnsupportedCitation() {
            return status == Status.UNSUPPORTED_PAGE_CITATION;
        }
        public boolean needsCitationCaution() {
            return status == Status.EVIDENCE_AVAILABLE_NOT_CITED;
        }
    }
}
