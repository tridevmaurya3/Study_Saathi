package com.tridev.studysaathi.data.ai;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ask Study Saathi Hero Feature की current conversation memory।
 *
 * यह class:
 *
 * 1. पिछले Question और Answer turns को सुरक्षित रखती है।
 * 2. Follow-up question के लिए AI context तैयार करती है।
 * 3. बहुत लंबी conversation को सीमित रखती है।
 * 4. केवल नवीनतम उपयोगी turns को AI prompt में भेजती है।
 * 5. Screen rotation के बाद conversation restore कर सकती है।
 * 6. Regenerate, Simplify और More Detail जैसे actions के लिए
 *    अंतिम Question और Answer उपलब्ध कराती है।
 */
public final class StudyConversationMemory {

    /**
     * AI prompt में अधिकतम इतने पुराने Question-Answer turns जाएँगे।
     *
     * इससे conversation useful भी रहेगी और prompt अनावश्यक रूप से
     * बहुत बड़ा भी नहीं होगा।
     */
    private static final int MAXIMUM_CONVERSATION_TURNS =
            5;

    /**
     * Conversation context की अधिकतम स्थानीय character सीमा।
     */
    private static final int MAXIMUM_CONTEXT_CHARACTERS =
            8000;

    /**
     * एक Question की अधिकतम सुरक्षित सीमा।
     */
    private static final int MAXIMUM_QUESTION_CHARACTERS =
            2500;

    /**
     * एक Answer की अधिकतम सुरक्षित सीमा।
     */
    private static final int MAXIMUM_ANSWER_CHARACTERS =
            5000;

    private static final String STATE_QUESTIONS =
            "study_conversation_questions";

    private static final String STATE_ANSWERS =
            "study_conversation_answers";

    @NonNull
    private final List<ConversationTurn> conversationTurns =
            new ArrayList<>();

    /**
     * सफल AI Question और Answer को conversation में जोड़ता है।
     *
     * Failed, loading या खाली answer को conversation में नहीं जोड़ना चाहिए।
     */
    public synchronized void addTurn(
            @Nullable String question,
            @Nullable String answer
    ) {
        String safeQuestion =
                limitText(
                        question,
                        MAXIMUM_QUESTION_CHARACTERS
                );

        String safeAnswer =
                limitText(
                        answer,
                        MAXIMUM_ANSWER_CHARACTERS
                );

        if (safeQuestion.isEmpty()
                || safeAnswer.isEmpty()) {

            return;
        }

        conversationTurns.add(
                new ConversationTurn(
                        safeQuestion,
                        safeAnswer
                )
        );

        removeOldTurnsIfRequired();
    }

    /**
     * Regenerated answer आने पर अंतिम Question का पुराना Answer बदलता है।
     *
     * इससे एक ही Question के दो अलग answers conversation context में
     * duplicate होकर नहीं जाएँगे।
     */
    public synchronized void replaceLastAnswer(
            @Nullable String question,
            @Nullable String replacementAnswer
    ) {
        String safeQuestion =
                limitText(
                        question,
                        MAXIMUM_QUESTION_CHARACTERS
                );

        String safeAnswer =
                limitText(
                        replacementAnswer,
                        MAXIMUM_ANSWER_CHARACTERS
                );

        if (safeQuestion.isEmpty()
                || safeAnswer.isEmpty()) {

            return;
        }

        if (conversationTurns.isEmpty()) {
            addTurn(
                    safeQuestion,
                    safeAnswer
            );

            return;
        }

        int lastIndex =
                conversationTurns.size() - 1;

        ConversationTurn lastTurn =
                conversationTurns.get(
                        lastIndex
                );

        if (normalizeForComparison(
                lastTurn.getQuestion()
        ).equals(
                normalizeForComparison(
                        safeQuestion
                )
        )) {

            conversationTurns.set(
                    lastIndex,
                    new ConversationTurn(
                            safeQuestion,
                            safeAnswer
                    )
            );

            return;
        }

        addTurn(
                safeQuestion,
                safeAnswer
        );
    }

    /**
     * Follow-up AI request के लिए previous conversation block बनाता है।
     */
    @NonNull
    public synchronized String buildContextForNextQuestion() {
        if (conversationTurns.isEmpty()) {
            return "";
        }

        StringBuilder contextBuilder =
                new StringBuilder();

        contextBuilder.append(
                "PREVIOUS STUDY CONVERSATION\n"
        );

        contextBuilder.append(
                "Use this only as context for the student's follow-up question. "
        );

        contextBuilder.append(
                "Do not repeat the whole previous answer unless necessary.\n\n"
        );

        int startingIndex =
                Math.max(
                        0,
                        conversationTurns.size()
                                - MAXIMUM_CONVERSATION_TURNS
                );

        for (int index = startingIndex;
             index < conversationTurns.size();
             index++) {

            ConversationTurn turn =
                    conversationTurns.get(
                            index
                    );

            int visibleTurnNumber =
                    index - startingIndex + 1;

            String turnBlock =
                    createTurnBlock(
                            visibleTurnNumber,
                            turn
                    );

            if (contextBuilder.length()
                    + turnBlock.length()
                    > MAXIMUM_CONTEXT_CHARACTERS) {

                break;
            }

            contextBuilder.append(
                    turnBlock
            );
        }

        return contextBuilder.toString()
                .trim();
    }

    @NonNull
    private String createTurnBlock(
            int turnNumber,
            @NonNull ConversationTurn turn
    ) {
        StringBuilder turnBuilder =
                new StringBuilder();

        turnBuilder.append(
                "Conversation Turn "
        );

        turnBuilder.append(
                turnNumber
        );

        turnBuilder.append(
                "\nStudent Question:\n"
        );

        turnBuilder.append(
                turn.getQuestion()
        );

        turnBuilder.append(
                "\nStudy Saathi Answer:\n"
        );

        turnBuilder.append(
                turn.getAnswer()
        );

        turnBuilder.append(
                "\n\n"
        );

        return turnBuilder.toString();
    }

    /**
     * पिछला Question उपलब्ध है या नहीं।
     */
    public synchronized boolean hasHistory() {
        return !conversationTurns.isEmpty();
    }

    /**
     * Current conversation में कुल turns।
     */
    public synchronized int getTurnCount() {
        return conversationTurns.size();
    }

    /**
     * सबसे अंतिम Question।
     */
    @NonNull
    public synchronized String getLastQuestion() {
        if (conversationTurns.isEmpty()) {
            return "";
        }

        return conversationTurns
                .get(
                        conversationTurns.size() - 1
                )
                .getQuestion();
    }

    /**
     * सबसे अंतिम Answer।
     */
    @NonNull
    public synchronized String getLastAnswer() {
        if (conversationTurns.isEmpty()) {
            return "";
        }

        return conversationTurns
                .get(
                        conversationTurns.size() - 1
                )
                .getAnswer();
    }

    /**
     * सबसे अंतिम conversation turn हटाता है।
     *
     * Regenerate request failed होने जैसी स्थिति में उपयोगी हो सकता है।
     */
    public synchronized void removeLastTurn() {
        if (conversationTurns.isEmpty()) {
            return;
        }

        conversationTurns.remove(
                conversationTurns.size() - 1
        );
    }

    /**
     * नई independent conversation शुरू करता है।
     */
    public synchronized void clear() {
        conversationTurns.clear();
    }

    /**
     * Screen recreation के लिए Question और Answer lists Bundle में रखता है।
     */
    public synchronized void saveToBundle(
            @NonNull Bundle outState
    ) {
        ArrayList<String> questions =
                new ArrayList<>();

        ArrayList<String> answers =
                new ArrayList<>();

        for (ConversationTurn turn :
                conversationTurns) {

            questions.add(
                    turn.getQuestion()
            );

            answers.add(
                    turn.getAnswer()
            );
        }

        outState.putStringArrayList(
                STATE_QUESTIONS,
                questions
        );

        outState.putStringArrayList(
                STATE_ANSWERS,
                answers
        );
    }

    /**
     * Screen recreation के बाद saved conversation restore करता है।
     */
    public synchronized void restoreFromBundle(
            @Nullable Bundle savedInstanceState
    ) {
        conversationTurns.clear();

        if (savedInstanceState == null) {
            return;
        }

        ArrayList<String> savedQuestions =
                savedInstanceState.getStringArrayList(
                        STATE_QUESTIONS
                );

        ArrayList<String> savedAnswers =
                savedInstanceState.getStringArrayList(
                        STATE_ANSWERS
                );

        if (savedQuestions == null
                || savedAnswers == null
                || savedQuestions.isEmpty()
                || savedAnswers.isEmpty()) {

            return;
        }

        int availableTurnCount =
                Math.min(
                        savedQuestions.size(),
                        savedAnswers.size()
                );

        int startingIndex =
                Math.max(
                        0,
                        availableTurnCount
                                - MAXIMUM_CONVERSATION_TURNS
                );

        for (int index = startingIndex;
             index < availableTurnCount;
             index++) {

            String question =
                    limitText(
                            savedQuestions.get(
                                    index
                            ),
                            MAXIMUM_QUESTION_CHARACTERS
                    );

            String answer =
                    limitText(
                            savedAnswers.get(
                                    index
                            ),
                            MAXIMUM_ANSWER_CHARACTERS
                    );

            if (question.isEmpty()
                    || answer.isEmpty()) {

                continue;
            }

            conversationTurns.add(
                    new ConversationTurn(
                            question,
                            answer
                    )
            );
        }

        removeOldTurnsIfRequired();
    }

    /**
     * अधिकतम allowed turns से पुराने turns हटाता है।
     */
    private void removeOldTurnsIfRequired() {
        while (conversationTurns.size()
                > MAXIMUM_CONVERSATION_TURNS) {

            conversationTurns.remove(
                    0
            );
        }
    }

    @NonNull
    private static String limitText(
            @Nullable Object value,
            int maximumLength
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.length()
                <= maximumLength) {

            return safeValue;
        }

        return safeValue.substring(
                        0,
                        maximumLength
                )
                .trim();
    }

    @NonNull
    private static String normalizeForComparison(
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
                : value.toString()
                .trim();
    }

    /**
     * एक सफल Question-Answer pair।
     */
    private static final class ConversationTurn {

        @NonNull
        private final String question;

        @NonNull
        private final String answer;

        private ConversationTurn(
                @NonNull String question,
                @NonNull String answer
        ) {
            this.question =
                    question;

            this.answer =
                    answer;
        }

        @NonNull
        private String getQuestion() {
            return question;
        }

        @NonNull
        private String getAnswer() {
            return answer;
        }
    }
}