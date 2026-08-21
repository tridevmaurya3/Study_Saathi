package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

/** Builds a concise student-facing disclosure for approved book evidence. */
public final class GroundingTransparencyFormatter {
    private GroundingTransparencyFormatter() { }

    @NonNull
    public static String format(@NonNull BookAnswerGroundingValidator.Result result,
                                @Nullable String explanationLanguage) {
        if (result.getStatus() == BookAnswerGroundingValidator.Status.NO_EXACT_EVIDENCE) {
            return "";
        }
        boolean english = explanationLanguage != null
                && explanationLanguage.trim().toLowerCase().startsWith("english");
        String approved = joinPages(result.getApprovedPages());
        String coverage = result.getCitedPageCount() + "/" + result.getApprovedPageCount();
        if (result.isGrounded()) {
            return english
                    ? "🔎 Book evidence: page " + approved + " • Citation coverage " + coverage
                    : "🔎 पुस्तक प्रमाण: पृष्ठ " + approved + " • Citation coverage " + coverage;
        }
        if (result.needsCitationCaution()) {
            return english
                    ? "ⓘ Book evidence was available on page " + approved
                            + ", but the answer did not include an exact citation • Coverage " + coverage
                    : "ⓘ पुस्तक प्रमाण पृष्ठ " + approved
                            + " पर उपलब्ध था, लेकिन उत्तर में exact citation नहीं मिला • Coverage " + coverage;
        }
        return english
                ? "⚠ The cited page is outside the approved book evidence."
                : "⚠ दिया गया पृष्ठ approved book evidence में नहीं है।";
    }

    @NonNull
    private static String joinPages(@NonNull Set<Integer> pages) {
        StringBuilder value = new StringBuilder();
        for (int page : pages) {
            if (value.length() > 0) value.append(", ");
            value.append(page);
        }
        return value.toString();
    }
}
