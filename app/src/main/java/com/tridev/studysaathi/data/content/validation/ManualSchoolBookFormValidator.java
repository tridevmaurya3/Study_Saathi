package com.tridev.studysaathi.data.content.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ManualSchoolBookFormValidator {

    private static final int MINIMUM_PUBLICATION_YEAR =
            1900;

    private ManualSchoolBookFormValidator() {
        /*
         * Utility class.
         */
    }

    /**
     * Manual school book form को validate और normalize करता है।
     *
     * Cover photo, author, publisher, edition, ISBN और
     * book code optional हैं।
     *
     * केवल valid subject और book title आवश्यक हैं।
     */
    @NonNull
    public static ValidationResult validate(
            long subjectRowId,
            @Nullable CharSequence bookTitle,
            @Nullable CharSequence bookSubtitle,
            @Nullable CharSequence authorName,
            @Nullable CharSequence publisherName,
            @Nullable CharSequence editionName,
            @Nullable CharSequence publicationYear,
            @Nullable CharSequence isbn,
            @Nullable CharSequence bookCode,
            @Nullable String selectedCoverPath,
            boolean primaryBook,
            boolean aiTutorEnabled
    ) {
        String safeBookTitle =
                normalizeText(
                        bookTitle
                );

        String safeBookSubtitle =
                normalizeText(
                        bookSubtitle
                );

        String safeAuthorName =
                normalizeText(
                        authorName
                );

        String safePublisherName =
                normalizeText(
                        publisherName
                );

        String safeEditionName =
                normalizeText(
                        editionName
                );

        String safePublicationYear =
                normalizeText(
                        publicationYear
                );

        String safeIsbn =
                normalizeIsbn(
                        isbn
                );

        String safeBookCode =
                normalizeBookCode(
                        bookCode
                );

        String safeSelectedCoverPath =
                normalizeText(
                        selectedCoverPath
                );

        String bookTitleError =
                "";

        String publicationYearError =
                "";

        String isbnError =
                "";

        List<String> generalErrors =
                new ArrayList<>();

        if (subjectRowId <= 0L) {
            generalErrors.add(
                    "एक valid school subject चुनना आवश्यक है।"
            );
        }

        if (safeBookTitle.isEmpty()) {
            bookTitleError =
                    "Book title आवश्यक है।";
        }

        if (!safePublicationYear.isEmpty()
                && !isValidPublicationYear(
                safePublicationYear
        )) {

            publicationYearError =
                    createPublicationYearError();
        }

        String isbn10 =
                "";

        String isbn13 =
                "";

        if (!safeIsbn.isEmpty()) {
            if (safeIsbn.length() == 10) {
                if (isValidIsbn10(
                        safeIsbn
                )) {
                    isbn10 =
                            safeIsbn;

                } else {
                    isbnError =
                            "यह ISBN-10 valid नहीं है।";
                }

            } else if (safeIsbn.length() == 13) {
                if (isValidIsbn13(
                        safeIsbn
                )) {
                    isbn13 =
                            safeIsbn;

                } else {
                    isbnError =
                            "यह ISBN-13 valid नहीं है।";
                }

            } else {
                isbnError =
                        "ISBN में 10 या 13 characters होने चाहिए।";
            }
        }

        boolean valid =
                generalErrors.isEmpty()
                        && bookTitleError.isEmpty()
                        && publicationYearError.isEmpty()
                        && isbnError.isEmpty();

        return new ValidationResult(
                subjectRowId,
                safeBookTitle,
                safeBookSubtitle,
                safeAuthorName,
                safePublisherName,
                safeEditionName,
                safePublicationYear,
                isbn10,
                isbn13,
                safeBookCode,
                safeSelectedCoverPath,
                primaryBook,
                aiTutorEnabled,
                bookTitleError,
                publicationYearError,
                isbnError,
                generalErrors,
                valid
        );
    }

    private static boolean isValidPublicationYear(
            @NonNull String publicationYear
    ) {
        if (!publicationYear.matches(
                "\\d{4}"
        )) {
            return false;
        }

        try {
            int year =
                    Integer.parseInt(
                            publicationYear
                    );

            int maximumAllowedYear =
                    Calendar.getInstance()
                            .get(
                                    Calendar.YEAR
                            ) + 1;

            return year >= MINIMUM_PUBLICATION_YEAR
                    && year <= maximumAllowedYear;

        } catch (NumberFormatException exception) {
            return false;
        }
    }

    @NonNull
    private static String createPublicationYearError() {
        int maximumAllowedYear =
                Calendar.getInstance()
                        .get(
                                Calendar.YEAR
                        ) + 1;

        return "Publication year "
                + MINIMUM_PUBLICATION_YEAR
                + " से "
                + maximumAllowedYear
                + " के बीच होना चाहिए।";
    }

    /**
     * ISBN-10 checksum validate करता है।
     *
     * अंतिम character X हो सकता है।
     */
    private static boolean isValidIsbn10(
            @NonNull String isbn
    ) {
        if (isbn.length() != 10) {
            return false;
        }

        int weightedSum =
                0;

        for (int index = 0;
             index < 10;
             index++) {

            char character =
                    isbn.charAt(
                            index
                    );

            int digit;

            if (index == 9
                    && character == 'X') {

                digit =
                        10;

            } else if (Character.isDigit(
                    character
            )) {
                digit =
                        character - '0';

            } else {
                return false;
            }

            weightedSum +=
                    digit
                            * (10 - index);
        }

        return weightedSum % 11
                == 0;
    }

    /**
     * ISBN-13 checksum validate करता है।
     */
    private static boolean isValidIsbn13(
            @NonNull String isbn
    ) {
        if (isbn.length() != 13
                || !isbn.matches(
                "\\d{13}"
        )) {
            return false;
        }

        int weightedSum =
                0;

        for (int index = 0;
             index < 12;
             index++) {

            int digit =
                    isbn.charAt(
                            index
                    ) - '0';

            weightedSum +=
                    index % 2 == 0
                            ? digit
                            : digit * 3;
        }

        int expectedCheckDigit =
                (10 - weightedSum % 10)
                        % 10;

        int actualCheckDigit =
                isbn.charAt(
                        12
                ) - '0';

        return expectedCheckDigit
                == actualCheckDigit;
    }

    @NonNull
    private static String normalizeIsbn(
            @Nullable Object value
    ) {
        return safeText(
                value
        )
                .replaceAll(
                        "[^0-9Xx]",
                        ""
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }

    @NonNull
    private static String normalizeBookCode(
            @Nullable Object value
    ) {
        return safeText(
                value
        )
                .toUpperCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    private static String normalizeText(
            @Nullable Object value
    ) {
        return safeText(
                value
        )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    @NonNull
    private static String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString().trim();
    }

    public static final class ValidationResult {

        private final long subjectRowId;

        @NonNull
        private final String bookTitle;

        @NonNull
        private final String bookSubtitle;

        @NonNull
        private final String authorName;

        @NonNull
        private final String publisherName;

        @NonNull
        private final String editionName;

        @NonNull
        private final String publicationYear;

        @NonNull
        private final String isbn10;

        @NonNull
        private final String isbn13;

        @NonNull
        private final String bookCode;

        @NonNull
        private final String selectedCoverPath;

        private final boolean primaryBook;

        private final boolean aiTutorEnabled;

        @NonNull
        private final String bookTitleError;

        @NonNull
        private final String publicationYearError;

        @NonNull
        private final String isbnError;

        @NonNull
        private final List<String> generalErrors;

        private final boolean valid;

        private ValidationResult(
                long subjectRowId,
                @NonNull String bookTitle,
                @NonNull String bookSubtitle,
                @NonNull String authorName,
                @NonNull String publisherName,
                @NonNull String editionName,
                @NonNull String publicationYear,
                @NonNull String isbn10,
                @NonNull String isbn13,
                @NonNull String bookCode,
                @NonNull String selectedCoverPath,
                boolean primaryBook,
                boolean aiTutorEnabled,
                @NonNull String bookTitleError,
                @NonNull String publicationYearError,
                @NonNull String isbnError,
                @NonNull List<String> generalErrors,
                boolean valid
        ) {
            this.subjectRowId =
                    Math.max(
                            0L,
                            subjectRowId
                    );

            this.bookTitle =
                    bookTitle;

            this.bookSubtitle =
                    bookSubtitle;

            this.authorName =
                    authorName;

            this.publisherName =
                    publisherName;

            this.editionName =
                    editionName;

            this.publicationYear =
                    publicationYear;

            this.isbn10 =
                    isbn10;

            this.isbn13 =
                    isbn13;

            this.bookCode =
                    bookCode;

            this.selectedCoverPath =
                    selectedCoverPath;

            this.primaryBook =
                    primaryBook;

            this.aiTutorEnabled =
                    aiTutorEnabled;

            this.bookTitleError =
                    bookTitleError;

            this.publicationYearError =
                    publicationYearError;

            this.isbnError =
                    isbnError;

            this.generalErrors =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    generalErrors
                            )
                    );

            this.valid =
                    valid;
        }

        public long getSubjectRowId() {
            return subjectRowId;
        }

        @NonNull
        public String getBookTitle() {
            return bookTitle;
        }

        @NonNull
        public String getBookSubtitle() {
            return bookSubtitle;
        }

        @NonNull
        public String getAuthorName() {
            return authorName;
        }

        @NonNull
        public String getPublisherName() {
            return publisherName;
        }

        @NonNull
        public String getEditionName() {
            return editionName;
        }

        @NonNull
        public String getPublicationYear() {
            return publicationYear;
        }

        @NonNull
        public String getIsbn10() {
            return isbn10;
        }

        @NonNull
        public String getIsbn13() {
            return isbn13;
        }

        @NonNull
        public String getPreferredIsbn() {
            if (!isbn13.isEmpty()) {
                return isbn13;
            }

            return isbn10;
        }

        @NonNull
        public String getBookCode() {
            return bookCode;
        }

        @NonNull
        public String getSelectedCoverPath() {
            return selectedCoverPath;
        }

        public boolean isPrimaryBook() {
            return primaryBook;
        }

        public boolean isAiTutorEnabled() {
            return aiTutorEnabled;
        }

        @NonNull
        public String getBookTitleError() {
            return bookTitleError;
        }

        @NonNull
        public String getPublicationYearError() {
            return publicationYearError;
        }

        @NonNull
        public String getIsbnError() {
            return isbnError;
        }

        @NonNull
        public List<String> getGeneralErrors() {
            return generalErrors;
        }

        public boolean isValid() {
            return valid;
        }

        public boolean hasGeneralErrors() {
            return !generalErrors.isEmpty();
        }

        public boolean hasIsbn() {
            return !isbn10.isEmpty()
                    || !isbn13.isEmpty();
        }

        public boolean hasCoverImage() {
            return !selectedCoverPath.isEmpty();
        }
    }
}