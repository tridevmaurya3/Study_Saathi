package com.tridev.studysaathi.data.knowledge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Study Saathi के verified offline educational content की
 * एक सामान्य immutable entry।
 *
 * इस model का उपयोग सभी non-calculation subjects के लिए होगा:
 *
 * 1. Science
 * 2. Social Science
 * 3. EVS
 * 4. English
 * 5. Hindi
 * 6. Sanskrit
 * 7. Computer
 * 8. General Knowledge
 *
 * हर छोटे question या topic के लिए Java class बनाने की जगह
 * content JSON अथवा database entry के रूप में रखा जाएगा।
 */
public final class OfflineKnowledgeEntry {

    public static final String ANY_VALUE =
            "*";

    public static final String LANGUAGE_HINDI =
            "hindi";

    public static final String LANGUAGE_ENGLISH =
            "english";

    public static final String LANGUAGE_BILINGUAL =
            "bilingual";

    @NonNull
    private final String entryId;

    @NonNull
    private final String educationBoard;

    @NonNull
    private final String studentClass;

    @NonNull
    private final String language;

    @NonNull
    private final String subject;

    @NonNull
    private final String chapter;

    @NonNull
    private final String topic;

    @NonNull
    private final String title;

    @NonNull
    private final String directAnswer;

    @NonNull
    private final String detailedExplanation;

    @NonNull
    private final List<String> keyPoints;

    @NonNull
    private final List<String> examples;

    @NonNull
    private final List<String> keywords;

    @NonNull
    private final List<String> questionPatterns;

    @NonNull
    private final String sourceLabel;

    @NonNull
    private final String sourceVersion;

    private final int priority;

    private final boolean verified;

    private OfflineKnowledgeEntry(
            @NonNull String entryId,
            @NonNull String educationBoard,
            @NonNull String studentClass,
            @NonNull String language,
            @NonNull String subject,
            @NonNull String chapter,
            @NonNull String topic,
            @NonNull String title,
            @NonNull String directAnswer,
            @NonNull String detailedExplanation,
            @NonNull List<String> keyPoints,
            @NonNull List<String> examples,
            @NonNull List<String> keywords,
            @NonNull List<String> questionPatterns,
            @NonNull String sourceLabel,
            @NonNull String sourceVersion,
            int priority,
            boolean verified
    ) {
        this.entryId =
                safeText(entryId);

        this.educationBoard =
                defaultToAny(educationBoard);

        this.studentClass =
                defaultToAny(studentClass);

        this.language =
                defaultToAny(language);

        this.subject =
                defaultToAny(subject);

        this.chapter =
                defaultToAny(chapter);

        this.topic =
                safeText(topic);

        this.title =
                safeText(title);

        this.directAnswer =
                safeText(directAnswer);

        this.detailedExplanation =
                safeText(detailedExplanation);

        this.keyPoints =
                createImmutableList(keyPoints);

        this.examples =
                createImmutableList(examples);

        this.keywords =
                createImmutableList(keywords);

        this.questionPatterns =
                createImmutableList(questionPatterns);

        this.sourceLabel =
                safeText(sourceLabel);

        this.sourceVersion =
                safeText(sourceVersion);

        this.priority =
                Math.max(0, priority);

        this.verified =
                verified;
    }

    /**
     * JSON object से OfflineKnowledgeEntry तैयार करता है।
     *
     * Supported JSON fields:
     *
     * id
     * board
     * class
     * language
     * subject
     * chapter
     * topic
     * title
     * directAnswer
     * explanation
     * keyPoints
     * examples
     * keywords
     * questionPatterns
     * source
     * sourceVersion
     * priority
     * verified
     */
    @NonNull
    public static OfflineKnowledgeEntry fromJson(
            @NonNull JSONObject jsonObject
    ) throws JSONException {

        String entryId =
                jsonObject.optString(
                        "id",
                        ""
                );

        String educationBoard =
                jsonObject.optString(
                        "board",
                        ANY_VALUE
                );

        String studentClass =
                jsonObject.optString(
                        "class",
                        ANY_VALUE
                );

        String language =
                jsonObject.optString(
                        "language",
                        ANY_VALUE
                );

        String subject =
                jsonObject.optString(
                        "subject",
                        ANY_VALUE
                );

        String chapter =
                jsonObject.optString(
                        "chapter",
                        ANY_VALUE
                );

        String topic =
                jsonObject.optString(
                        "topic",
                        ""
                );

        String title =
                jsonObject.optString(
                        "title",
                        ""
                );

        String directAnswer =
                jsonObject.optString(
                        "directAnswer",
                        ""
                );

        String detailedExplanation =
                jsonObject.optString(
                        "explanation",
                        ""
                );

        List<String> keyPoints =
                readStringArray(
                        jsonObject.optJSONArray(
                                "keyPoints"
                        )
                );

        List<String> examples =
                readStringArray(
                        jsonObject.optJSONArray(
                                "examples"
                        )
                );

        List<String> keywords =
                readStringArray(
                        jsonObject.optJSONArray(
                                "keywords"
                        )
                );

        List<String> questionPatterns =
                readStringArray(
                        jsonObject.optJSONArray(
                                "questionPatterns"
                        )
                );

        String sourceLabel =
                jsonObject.optString(
                        "source",
                        ""
                );

        String sourceVersion =
                jsonObject.optString(
                        "sourceVersion",
                        ""
                );

        int priority =
                jsonObject.optInt(
                        "priority",
                        0
                );

        boolean verified =
                jsonObject.optBoolean(
                        "verified",
                        false
                );

        OfflineKnowledgeEntry entry =
                new OfflineKnowledgeEntry(
                        entryId,
                        educationBoard,
                        studentClass,
                        language,
                        subject,
                        chapter,
                        topic,
                        title,
                        directAnswer,
                        detailedExplanation,
                        keyPoints,
                        examples,
                        keywords,
                        questionPatterns,
                        sourceLabel,
                        sourceVersion,
                        priority,
                        verified
                );

        if (!entry.isValid()) {
            throw new JSONException(
                    "Offline knowledge entry में जरूरी fields उपलब्ध नहीं हैं: "
                            + entryId
            );
        }

        return entry;
    }

    /**
     * यह जाँचता है कि entry basic offline use के लिए valid है।
     */
    public boolean isValid() {
        return !entryId.isEmpty()
                && !subject.isEmpty()
                && !directAnswer.isEmpty()
                && !keywords.isEmpty();
    }

    /**
     * Entry current Board, Class, Language, Subject और Chapter
     * context से मेल खाती है या नहीं।
     *
     * "*" value का अर्थ है कि entry सभी values के लिए लागू है।
     */
    public boolean matchesContext(
            @Nullable String requestedBoard,
            @Nullable String requestedClass,
            @Nullable String requestedLanguage,
            @Nullable String requestedSubject,
            @Nullable String requestedChapter
    ) {
        return matchesField(
                educationBoard,
                requestedBoard
        )
                && matchesField(
                studentClass,
                requestedClass
        )
                && matchesLanguage(
                language,
                requestedLanguage
        )
                && matchesField(
                subject,
                requestedSubject
        )
                && matchesField(
                chapter,
                requestedChapter
        );
    }

    /**
     * Answer card में दिखाने योग्य text बनाता है।
     */
    @NonNull
    public String buildAnswerText() {
        StringBuilder answerBuilder =
                new StringBuilder();

        if (!title.isEmpty()) {
            answerBuilder.append(title);
            answerBuilder.append("\n\n");
        }

        answerBuilder.append(directAnswer);

        if (!detailedExplanation.isEmpty()) {
            answerBuilder.append("\n\nसमझिए:\n");
            answerBuilder.append(detailedExplanation);
        }

        if (!keyPoints.isEmpty()) {
            answerBuilder.append("\n\nमुख्य बिंदु:");

            for (String keyPoint : keyPoints) {
                answerBuilder.append("\n• ");
                answerBuilder.append(keyPoint);
            }
        }

        if (!examples.isEmpty()) {
            answerBuilder.append("\n\nउदाहरण:");

            for (String example : examples) {
                answerBuilder.append("\n• ");
                answerBuilder.append(example);
            }
        }

        return answerBuilder.toString()
                .trim();
    }

    private static boolean matchesField(
            @Nullable String storedValue,
            @Nullable String requestedValue
    ) {
        String normalizedStoredValue =
                normalize(storedValue);

        String normalizedRequestedValue =
                normalize(requestedValue);

        if (normalizedStoredValue.equals(
                ANY_VALUE
        )) {
            return true;
        }

        if (normalizedRequestedValue.isEmpty()) {
            return false;
        }

        return normalizedStoredValue.equals(
                normalizedRequestedValue
        );
    }

    private static boolean matchesLanguage(
            @Nullable String storedLanguage,
            @Nullable String requestedLanguage
    ) {
        String normalizedStoredLanguage =
                normalize(storedLanguage);

        String normalizedRequestedLanguage =
                normalize(requestedLanguage);

        if (normalizedStoredLanguage.equals(
                ANY_VALUE
        )) {
            return true;
        }

        if (normalizedStoredLanguage.equals(
                LANGUAGE_BILINGUAL
        )) {
            return true;
        }

        if (normalizedRequestedLanguage.contains(
                normalizedStoredLanguage
        )) {
            return true;
        }

        if (normalizedStoredLanguage.equals(
                LANGUAGE_HINDI
        )) {
            return normalizedRequestedLanguage.contains(
                    "हिंदी"
            );
        }

        if (normalizedStoredLanguage.equals(
                LANGUAGE_ENGLISH
        )) {
            return normalizedRequestedLanguage.contains(
                    "अंग्रेज"
            );
        }

        return false;
    }

    @NonNull
    private static List<String> readStringArray(
            @Nullable JSONArray jsonArray
    ) {
        if (jsonArray == null
                || jsonArray.length() == 0) {

            return Collections.emptyList();
        }

        List<String> values =
                new ArrayList<>();

        for (int index = 0;
             index < jsonArray.length();
             index++) {

            String value =
                    safeText(
                            jsonArray.optString(
                                    index,
                                    ""
                            )
                    );

            if (!value.isEmpty()) {
                values.add(value);
            }
        }

        return values;
    }

    @NonNull
    private static List<String> createImmutableList(
            @Nullable List<String> sourceList
    ) {
        if (sourceList == null
                || sourceList.isEmpty()) {

            return Collections.emptyList();
        }

        List<String> cleanList =
                new ArrayList<>();

        for (String value : sourceList) {
            String safeValue =
                    safeText(value);

            if (!safeValue.isEmpty()) {
                cleanList.add(safeValue);
            }
        }

        return Collections.unmodifiableList(
                cleanList
        );
    }

    @NonNull
    private static String defaultToAny(
            @Nullable String value
    ) {
        String safeValue =
                safeText(value);

        return safeValue.isEmpty()
                ? ANY_VALUE
                : safeValue;
    }

    @NonNull
    private static String normalize(
            @Nullable String value
    ) {
        return safeText(value)
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toLowerCase(
                        Locale.ROOT
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

    @NonNull
    public String getEntryId() {
        return entryId;
    }

    @NonNull
    public String getEducationBoard() {
        return educationBoard;
    }

    @NonNull
    public String getStudentClass() {
        return studentClass;
    }

    @NonNull
    public String getLanguage() {
        return language;
    }

    @NonNull
    public String getSubject() {
        return subject;
    }

    @NonNull
    public String getChapter() {
        return chapter;
    }

    @NonNull
    public String getTopic() {
        return topic;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getDirectAnswer() {
        return directAnswer;
    }

    @NonNull
    public String getDetailedExplanation() {
        return detailedExplanation;
    }

    @NonNull
    public List<String> getKeyPoints() {
        return keyPoints;
    }

    @NonNull
    public List<String> getExamples() {
        return examples;
    }

    @NonNull
    public List<String> getKeywords() {
        return keywords;
    }

    @NonNull
    public List<String> getQuestionPatterns() {
        return questionPatterns;
    }

    @NonNull
    public String getSourceLabel() {
        return sourceLabel;
    }

    @NonNull
    public String getSourceVersion() {
        return sourceVersion;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isVerified() {
        return verified;
    }
}