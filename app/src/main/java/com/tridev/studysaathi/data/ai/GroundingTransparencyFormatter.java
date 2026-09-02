package com.tridev.studysaathi.data.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Set;

/** Builds concise student-facing disclosure for approved book evidence. */
public final class GroundingTransparencyFormatter {

    private GroundingTransparencyFormatter() { }

    @NonNull
    public static String format(
            @NonNull BookAnswerGroundingValidator.Result result,
            @Nullable String explanationLanguage
    ) {
        if (result.getStatus()
                == BookAnswerGroundingValidator.Status.NO_EXACT_EVIDENCE) {
            return "";
        }

        LanguageMode languageMode =
                resolveLanguageMode(explanationLanguage);

        String approved = joinPages(result.getApprovedPages());
        String coverage =
                result.getCitedPageCount()
                        + "/"
                        + result.getApprovedPageCount();

        if (result.isGrounded()) {
            switch (languageMode) {
                case ENGLISH:
                    return "🔎 Book evidence: page "
                            + approved
                            + " • Citation coverage "
                            + coverage;

                case HINDI:
                    return "🔎 पुस्तक प्रमाण: पृष्ठ "
                            + approved
                            + " • उद्धरण कवरेज "
                            + coverage;

                case HINGLISH:
                default:
                    return "🔎 Book evidence: page "
                            + approved
                            + " • Citation coverage "
                            + coverage;
            }
        }

        if (result.needsCitationCaution()) {
            switch (languageMode) {
                case ENGLISH:
                    return "ⓘ Book evidence was available on page "
                            + approved
                            + ", but the answer did not include an exact page citation"
                            + " • Coverage "
                            + coverage;

                case HINDI:
                    return "ⓘ पुस्तक प्रमाण पृष्ठ "
                            + approved
                            + " पर उपलब्ध था, लेकिन उत्तर में सटीक पृष्ठ उद्धरण नहीं दिया गया"
                            + " • कवरेज "
                            + coverage;

                case HINGLISH:
                default:
                    return "ⓘ Book evidence page "
                            + approved
                            + " पर available था, लेकिन answer में exact page citation नहीं आया"
                            + " • Coverage "
                            + coverage;
            }
        }

        switch (languageMode) {
            case ENGLISH:
                return "⚠ The cited page is outside the approved book evidence.";

            case HINDI:
                return "⚠ दिया गया पृष्ठ स्वीकृत पुस्तक प्रमाण में शामिल नहीं है।";

            case HINGLISH:
            default:
                return "⚠ Cited page approved book evidence के बाहर है।";
        }
    }

    @NonNull
    private static LanguageMode resolveLanguageMode(
            @Nullable String explanationLanguage
    ) {
        String language =
                explanationLanguage == null
                        ? ""
                        : explanationLanguage.trim()
                        .toLowerCase(Locale.ROOT);

        if (language.contains("hinglish")
                || language.contains("bilingual")
                || language.contains("hi,en")
                || language.contains("hindi + english")
                || language.contains("hindi+english")
                || (language.contains("hindi")
                && language.contains("english"))) {
            return LanguageMode.HINGLISH;
        }

        if ((language.contains("english") || language.equals("en"))
                && !language.contains("hindi")) {
            return LanguageMode.ENGLISH;
        }

        if (language.contains("hindi")
                || language.equals("hi")
                || language.contains("हिंदी")
                || language.contains("हिन्दी")) {
            return LanguageMode.HINDI;
        }

        return LanguageMode.HINGLISH;
    }

    @NonNull
    private static String joinPages(@NonNull Set<Integer> pages) {
        StringBuilder value = new StringBuilder();

        for (int page : pages) {
            if (value.length() > 0) {
                value.append(", ");
            }
            value.append(page);
        }

        return value.toString();
    }

    private enum LanguageMode {
        ENGLISH,
        HINDI,
        HINGLISH
    }
}
