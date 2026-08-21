package com.tridev.studysaathi.data.learning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/** Builds bounded, machine-readable references from parent-approved exact book pages. */
public final class ExactBookPageCitationBuilder {
    private static final int MAX_REFERENCE_CHARS = 11500;
    private static final int MAX_PAGE_CONTENT_CHARS = 3500;

    private ExactBookPageCitationBuilder() { }

    @NonNull
    public static String build(@NonNull List<PageReference> pages) {
        StringBuilder result = new StringBuilder();
        for (PageReference page : pages) {
            if (page == null || page.pageNumber <= 0 || page.content.isEmpty()) continue;
            String title = sanitizeTitle(page.title);
            String content = compact(page.content, MAX_PAGE_CONTENT_CHARS);
            String block = "[[VERIFIED_BOOK_PAGE page=" + page.pageNumber
                    + (title.isEmpty() ? "" : " title=\"" + title + "\"")
                    + "]]\n" + content + "\n[[END_VERIFIED_BOOK_PAGE]]\n\n";
            if (result.length() + block.length() > MAX_REFERENCE_CHARS) break;
            result.append(block);
        }
        return result.toString().trim();
    }

    @NonNull
    private static String sanitizeTitle(@Nullable String value) {
        return safe(value).replace('"', '\'').replaceAll("\\s+", " ");
    }

    @NonNull
    private static String compact(@Nullable String value, int maxLength) {
        String text = safe(value).replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n");
        return text.length() <= maxLength ? text : text.substring(0, maxLength).trim();
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    public static final class PageReference {
        private final int pageNumber;
        @NonNull private final String title;
        @NonNull private final String content;

        public PageReference(int pageNumber, @Nullable String title,
                             @Nullable String content) {
            this.pageNumber = Math.max(0, pageNumber);
            this.title = safe(title);
            this.content = safe(content);
        }
    }
}
