package com.tridev.studysaathi.data.content.mapper;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.content.validation
        .ManualSchoolBookFormValidator;
import com.tridev.studysaathi.data.local.entity
        .SchoolBookEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolCurriculumProfileEntity;
import com.tridev.studysaathi.data.local.entity
        .SchoolSubjectEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class ManualSchoolBookEntityMapper {

    private static final String BOOK_SOURCE_PARENT_IMPORTED =
            "PARENT_IMPORTED";

    private static final String ACCESS_TYPE_USER_IMPORTED =
            "USER_IMPORTED";

    private static final String LICENSE_TYPE_PURCHASED_OR_OWNED =
            "PURCHASED_OR_OWNED";

    private static final String DOWNLOAD_STATUS_NOT_ALLOWED =
            "NOT_ALLOWED";

    private static final String CONTENT_STATUS_NOT_STARTED =
            "NOT_STARTED";

    private ManualSchoolBookEntityMapper() {
        /*
         * Utility class.
         */
    }

    /**
     * Parent द्वारा manually confirmed school book को
     * Room database entity में बदलता है।
     *
     * @param validationResult Validated manual book form.
     * @param schoolSubject    Selected actual school subject.
     * @param curriculumProfile Student का school curriculum profile.
     * @param sortOrder        Subject की book list में position.
     */
    @NonNull
    public static SchoolBookEntity createEntity(
            @NonNull ManualSchoolBookFormValidator
                    .ValidationResult validationResult,
            @NonNull SchoolSubjectEntity schoolSubject,
            @NonNull SchoolCurriculumProfileEntity
                    curriculumProfile,
            int sortOrder
    ) {
        validateRequiredInformation(
                validationResult,
                schoolSubject,
                curriculumProfile
        );

        long currentTime =
                System.currentTimeMillis();

        String preferredIsbn =
                validationResult.getPreferredIsbn();

        String bookCode =
                validationResult.getBookCode();

        if (bookCode.isEmpty()) {
            bookCode =
                    preferredIsbn;
        }

        String coverPath =
                validationResult.getSelectedCoverPath();

        SchoolBookEntity schoolBook =
                new SchoolBookEntity();

        schoolBook.setSubjectRowId(
                schoolSubject.getSubjectRowId()
        );

        schoolBook.setBookId(
                createManualBookId(
                        validationResult,
                        schoolSubject
                )
        );

        schoolBook.setBookTitle(
                validationResult.getBookTitle()
        );

        schoolBook.setBookSubtitle(
                validationResult.getBookSubtitle()
        );

        schoolBook.setAuthorName(
                validationResult.getAuthorName()
        );

        schoolBook.setPublisherName(
                validationResult.getPublisherName()
        );

        schoolBook.setEditionName(
                validationResult.getEditionName()
        );

        schoolBook.setPublicationYear(
                validationResult.getPublicationYear()
        );

        schoolBook.setAcademicSession(
                safeText(
                        curriculumProfile.getAcademicSession()
                )
        );

        schoolBook.setClassName(
                createClassName(
                        curriculumProfile.getClassNumber()
                )
        );

        schoolBook.setEducationBoard(
                safeText(
                        curriculumProfile.getEducationBoard()
                )
        );

        schoolBook.setStudyMedium(
                safeText(
                        curriculumProfile.getStudyMedium()
                )
        );

        schoolBook.setIsbn10(
                validationResult.getIsbn10()
        );

        schoolBook.setIsbn13(
                validationResult.getIsbn13()
        );

        schoolBook.setBookCode(
                bookCode
        );

        schoolBook.setBarcodeValue(
                ""
        );

        schoolBook.setBarcodeFormat(
                ""
        );

        /*
         * Manual book के लिए कोई online cover URL नहीं है।
         */
        schoolBook.setCoverImageUrl(
                ""
        );

        schoolBook.setLocalCoverImagePath(
                coverPath
        );

        schoolBook.setScannedCoverImagePath(
                coverPath
        );

        schoolBook.setDetectedCoverText(
                ""
        );

        /*
         * यह match किसी online algorithm से नहीं बल्कि
         * Parent confirmation से आया है।
         */
        schoolBook.setCoverMatchConfidence(
                1.0f
        );

        schoolBook.setBookSource(
                BOOK_SOURCE_PARENT_IMPORTED
        );

        schoolBook.setOnlineProvider(
                ""
        );

        schoolBook.setOnlineVolumeId(
                ""
        );

        schoolBook.setOnlineInformationUrl(
                ""
        );

        schoolBook.setOfficialSourceUrl(
                ""
        );

        schoolBook.setAuthorizedDownloadUrl(
                ""
        );

        schoolBook.setAccessType(
                ACCESS_TYPE_USER_IMPORTED
        );

        schoolBook.setLicenseType(
                LICENSE_TYPE_PURCHASED_OR_OWNED
        );

        /*
         * Manual entry का अर्थ यह नहीं है कि copyrighted
         * full book download की अनुमति है।
         */
        schoolBook.setDownloadAllowed(
                false
        );

        schoolBook.setPreviewAllowed(
                false
        );

        schoolBook.setParentConfirmedMatch(
                true
        );

        schoolBook.setOfficialSourceVerified(
                false
        );

        schoolBook.setDownloadStatus(
                DOWNLOAD_STATUS_NOT_ALLOWED
        );

        schoolBook.setDownloadProgress(
                0
        );

        /*
         * अभी केवल book metadata और optional cover save
         * हो रहा है। Full book file अभी import नहीं हुई।
         */
        schoolBook.setLocalBookFilePath(
                ""
        );

        schoolBook.setLocalContentFolderPath(
                ""
        );

        schoolBook.setDownloadedFileName(
                ""
        );

        schoolBook.setDownloadedFileMimeType(
                ""
        );

        schoolBook.setDownloadedFileSizeBytes(
                0L
        );

        schoolBook.setDownloadedFileChecksumSha256(
                ""
        );

        /*
         * Chapters/Contents pages अगले चरण में जोड़े जाएँगे।
         */
        schoolBook.setContentProcessingStatus(
                CONTENT_STATUS_NOT_STARTED
        );

        schoolBook.setChapterCount(
                0
        );

        schoolBook.setProcessedChapterCount(
                0
        );

        schoolBook.setGeneratedLessonCount(
                0
        );

        schoolBook.setGeneratedQuizQuestionCount(
                0
        );

        /*
         * केवल cover उपलब्ध होने से पूरी book offline
         * available नहीं मानी जाएगी।
         */
        schoolBook.setOfflineAvailable(
                false
        );

        schoolBook.setAiTutorEnabled(
                validationResult.isAiTutorEnabled()
        );

        schoolBook.setActive(
                true
        );

        schoolBook.setPrimaryBook(
                validationResult.isPrimaryBook()
        );

        schoolBook.setSortOrder(
                Math.max(
                        0,
                        sortOrder
                )
        );

        schoolBook.setLastOnlineSearchAt(
                0L
        );

        schoolBook.setLastDownloadAttemptAt(
                0L
        );

        schoolBook.setDownloadCompletedAt(
                0L
        );

        schoolBook.setLastContentProcessedAt(
                0L
        );

        schoolBook.setCreatedAt(
                currentTime
        );

        schoolBook.setUpdatedAt(
                currentTime
        );

        return schoolBook;
    }

    private static void validateRequiredInformation(
            @NonNull ManualSchoolBookFormValidator
                    .ValidationResult validationResult,
            @NonNull SchoolSubjectEntity schoolSubject,
            @NonNull SchoolCurriculumProfileEntity
                    curriculumProfile
    ) {
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(
                    "Manual school book form is not valid."
            );
        }

        if (schoolSubject.getSubjectRowId() <= 0L) {
            throw new IllegalArgumentException(
                    "A valid school subject is required."
            );
        }

        if (validationResult.getSubjectRowId()
                != schoolSubject.getSubjectRowId()) {

            throw new IllegalArgumentException(
                    "Selected subject does not match the validated book form."
            );
        }

        if (curriculumProfile.getProfileId() <= 0L) {
            throw new IllegalArgumentException(
                    "A valid curriculum profile is required."
            );
        }

        if (schoolSubject.getProfileId()
                != curriculumProfile.getProfileId()) {

            throw new IllegalArgumentException(
                    "The school subject does not belong to this curriculum profile."
            );
        }
    }

    @NonNull
    private static String createManualBookId(
            @NonNull ManualSchoolBookFormValidator
                    .ValidationResult validationResult,
            @NonNull SchoolSubjectEntity schoolSubject
    ) {
        String identitySeed =
                schoolSubject.getSubjectRowId()
                        + "|"
                        + normalizeIdentityText(
                        validationResult.getBookTitle()
                )
                        + "|"
                        + normalizeIdentityText(
                        validationResult.getPublisherName()
                )
                        + "|"
                        + normalizeIdentityText(
                        validationResult.getEditionName()
                )
                        + "|"
                        + normalizeIdentityText(
                        validationResult.getPreferredIsbn()
                )
                        + "|"
                        + normalizeIdentityText(
                        validationResult.getBookCode()
                );

        String identityHash =
                createSha256Hash(
                        identitySeed
                );

        return "manual_"
                + schoolSubject.getSubjectRowId()
                + "_"
                + identityHash.substring(
                0,
                Math.min(
                        20,
                        identityHash.length()
                )
        );
    }

    @NonNull
    private static String createSha256Hash(
            @NonNull String value
    ) {
        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] digest =
                    messageDigest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hashBuilder =
                    new StringBuilder();

            for (byte digestByte :
                    digest) {

                hashBuilder.append(
                        String.format(
                                Locale.ROOT,
                                "%02x",
                                digestByte & 0xff
                        )
                );
            }

            return hashBuilder.toString();

        } catch (NoSuchAlgorithmException exception) {
            /*
             * SHA-256 Android/Java में standard algorithm है।
             * Rare fallback में deterministic hashCode उपयोग होगा।
             */
            return String.format(
                    Locale.ROOT,
                    "%08x%08x",
                    value.hashCode(),
                    value.toLowerCase(
                            Locale.ROOT
                    ).hashCode()
            );
        }
    }

    @NonNull
    private static String createClassName(
            int classNumber
    ) {
        if (classNumber >= 1
                && classNumber <= 12) {

            return "Class "
                    + classNumber;
        }

        return "";
    }

    @NonNull
    private static String normalizeIdentityText(
            @NonNull String value
    ) {
        return safeText(
                value
        )
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "&",
                        " and "
                )
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    @NonNull
    private static String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
}