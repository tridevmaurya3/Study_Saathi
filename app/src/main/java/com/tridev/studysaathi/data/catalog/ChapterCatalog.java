package com.tridev.studysaathi.data.catalog;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.model.ChapterItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChapterCatalog {

    private static final int DEFAULT_CLASS_NUMBER = 6;

    private ChapterCatalog() {
        // Utility class.
    }

    @NonNull
    public static List<ChapterItem> getChapters(
            String educationBoard,
            String studentClass,
            String subjectName
    ) {
        int classNumber = extractClassNumber(studentClass);
        String normalizedSubject = normalizeSubjectName(subjectName);

        /*
         * अभी detailed starter content Class 6 के लिए रखा गया है।
         * दूसरे Boards और Classes का official session-based content
         * आगे cloud curriculum से load होगा।
         */
        if (classNumber == 6) {
            switch (normalizedSubject) {
                case "mathematics":
                    return getMathematicsChapters();

                case "science":
                    return getScienceChapters();

                case "english":
                    return getEnglishChapters();

                case "hindi":
                    return getHindiChapters();

                case "social science":
                    return getSocialScienceChapters();

                case "sanskrit":
                    return getSanskritChapters();

                case "computer":
                    return getComputerChapters();

                case "general knowledge":
                    return getGeneralKnowledgeChapters();

                default:
                    return getGenericChapters(
                            subjectName,
                            classNumber
                    );
            }
        }

        return getGenericChapters(subjectName, classNumber);
    }

    private static int extractClassNumber(String studentClass) {
        if (studentClass == null || studentClass.trim().isEmpty()) {
            return DEFAULT_CLASS_NUMBER;
        }

        Pattern numberPattern = Pattern.compile("(\\d{1,2})");
        Matcher matcher = numberPattern.matcher(studentClass);

        if (!matcher.find()) {
            return DEFAULT_CLASS_NUMBER;
        }

        try {
            int classNumber = Integer.parseInt(matcher.group(1));

            if (classNumber < 1 || classNumber > 12) {
                return DEFAULT_CLASS_NUMBER;
            }

            return classNumber;
        } catch (NumberFormatException exception) {
            return DEFAULT_CLASS_NUMBER;
        }
    }

    @NonNull
    private static String normalizeSubjectName(String subjectName) {
        if (subjectName == null) {
            return "";
        }

        return subjectName
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    @NonNull
    private static List<ChapterItem> getMathematicsChapters() {
        List<ChapterItem> chapters = new ArrayList<>();

        addChapter(
                chapters,
                "Numbers Around Us",
                "Numbers को पढ़ना, लिखना और compare करना।",
                6
        );

        addChapter(
                chapters,
                "Whole Numbers",
                "Number line, properties और basic operations।",
                6
        );

        addChapter(
                chapters,
                "Factors and Multiples",
                "Factors, multiples, prime numbers, HCF और LCM।",
                7
        );

        addChapter(
                chapters,
                "Integers",
                "Positive और negative numbers को सरल तरीके से समझें।",
                6
        );

        addChapter(
                chapters,
                "Fractions",
                "Fractions की तुलना, addition और subtraction।",
                8
        );

        addChapter(
                chapters,
                "Decimals",
                "Decimal numbers और दैनिक जीवन में उनका उपयोग।",
                6
        );

        addChapter(
                chapters,
                "Basic Geometry",
                "Points, lines, angles और shapes की पहचान।",
                7
        );

        addChapter(
                chapters,
                "Data Handling",
                "Tables, pictographs और bar graphs समझें।",
                6
        );

        return chapters;
    }

    @NonNull
    private static List<ChapterItem> getScienceChapters() {
        List<ChapterItem> chapters = new ArrayList<>();

        addChapter(
                chapters,
                "The World of Science",
                "Observation, questions और scientific thinking।",
                5
        );

        addChapter(
                chapters,
                "Living World Around Us",
                "Living organisms और उनकी विशेषताएँ।",
                7
        );

        addChapter(
                chapters,
                "Food and Nutrition",
                "Food sources, nutrients और healthy diet।",
                7
        );

        addChapter(
                chapters,
                "Materials Around Us",
                "Materials की properties और classification।",
                6
        );

        addChapter(
                chapters,
                "Measurement and Motion",
                "Length, units और motion के प्रकार।",
                7
        );

        addChapter(
                chapters,
                "Light and Shadows",
                "Light sources, shadows और reflection basics।",
                6
        );

        addChapter(
                chapters,
                "Electricity and Circuits",
                "Cells, bulbs, switches और simple circuits।",
                7
        );

        addChapter(
                chapters,
                "Water and Environment",
                "Water cycle, conservation और surroundings।",
                6
        );

        return chapters;
    }

    @NonNull
    private static List<ChapterItem> getEnglishChapters() {
        List<ChapterItem> chapters = new ArrayList<>();

        addChapter(
                chapters,
                "Reading Skills",
                "Paragraph को समझना और सही answers ढूँढना।",
                6
        );

        addChapter(
                chapters,
                "Grammar Foundations",
                "Noun, pronoun, verb और sentence structure।",
                8
        );

        addChapter(
                chapters,
                "Vocabulary Builder",
                "New words, meanings, synonyms और antonyms।",
                6
        );

        addChapter(
                chapters,
                "Writing Skills",
                "Paragraph, letter और short composition writing।",
                7
        );

        addChapter(
                chapters,
                "Prose and Stories",
                "Stories को सरल explanation के साथ पढ़ें।",
                8
        );

        addChapter(
                chapters,
                "Poetry",
                "Poem meaning, rhyme और central idea।",
                6
        );

        return chapters;
    }

    @NonNull
    private static List<ChapterItem> getHindiChapters() {
        List<ChapterItem> chapters = new ArrayList<>();

        addChapter(
                chapters,
                "पठन कौशल",
                "गद्यांश पढ़कर उसके अर्थ और उत्तर समझना।",
                6
        );

        addChapter(
                chapters,
                "हिंदी व्याकरण",
                "संज्ञा, सर्वनाम, विशेषण और क्रिया।",
                8
        );

        addChapter(
                chapters,
                "शब्द भंडार",
                "पर्यायवाची, विलोम और अनेक शब्दों के लिए एक शब्द।",
                6
        );

        addChapter(
                chapters,
                "लेखन कौशल",
                "अनुच्छेद, पत्र और संवाद लेखन।",
                7
        );

        addChapter(
                chapters,
                "गद्य पाठ",
                "कहानी और पाठ को सरल भाषा में समझना।",
                8
        );

        addChapter(
                chapters,
                "पद्य पाठ",
                "कविता का भावार्थ और मुख्य संदेश।",
                6
        );

        return chapters;
    }

    @NonNull
    private static List<ChapterItem> getSocialScienceChapters() {
        List<ChapterItem> chapters = new ArrayList<>();

        addChapter(
                chapters,
                "Understanding History",
                "Past, sources और timelines को समझें।",
                6
        );

        addChapter(
                chapters,
                "Early Civilisations",
                "प्रारम्भिक समाज और सभ्यताओं का परिचय।",
                7
        );

        addChapter(
                chapters,
                "The Earth and Maps",
                "Globe, directions, maps और coordinates।",
                7
        );

        addChapter(
                chapters,
                "Our Environment",
                "Land, water, climate और natural resources।",
                6
        );

        addChapter(
                chapters,
                "Diversity and Society",
                "भारत की विविधता और सामाजिक जीवन।",
                6
        );

        addChapter(
                chapters,
                "Government and Citizenship",
                "Government, rules, rights और responsibilities।",
                7
        );

        return chapters;
    }

    @NonNull
    private static List<ChapterItem> getSanskritChapters() {
        List<ChapterItem> chapters = new ArrayList<>();

        addChapter(
                chapters,
                "वर्ण एवं उच्चारण",
                "संस्कृत वर्णों और सही उच्चारण का अभ्यास।",
                5
        );

        addChapter(
                chapters,
                "शब्द परिचय",
                "सरल संस्कृत शब्द और उनके अर्थ।",
                6
        );

        addChapter(
                chapters,
                "सरल वाक्य",
                "छोटे संस्कृत वाक्य पढ़ना और बनाना।",
                6
        );

        addChapter(
                chapters,
                "व्याकरण आधार",
                "लिंग, वचन और सरल धातु रूप।",
                7
        );

        addChapter(
                chapters,
                "पाठ एवं अभ्यास",
                "सरल पाठ, अनुवाद और प्रश्नोत्तर।",
                7
        );

        return chapters;
    }

    @NonNull
    private static List<ChapterItem> getComputerChapters() {
        List<ChapterItem> chapters = new ArrayList<>();

        addChapter(
                chapters,
                "Computer Fundamentals",
                "Computer के मुख्य parts और उनका उपयोग।",
                6
        );

        addChapter(
                chapters,
                "Operating System",
                "Files, folders और basic computer operations।",
                6
        );

        addChapter(
                chapters,
                "Word Processing",
                "Documents बनाना और formatting करना।",
                7
        );

        addChapter(
                chapters,
                "Internet Basics",
                "Web, search और safe internet use।",
                6
        );

        addChapter(
                chapters,
                "Digital Safety",
                "Passwords, privacy और online safety।",
                6
        );

        addChapter(
                chapters,
                "Introduction to Coding",
                "Logic, sequence और basic programming ideas।",
                7
        );

        return chapters;
    }

    @NonNull
    private static List<ChapterItem> getGeneralKnowledgeChapters() {
        List<ChapterItem> chapters = new ArrayList<>();

        addChapter(
                chapters,
                "India",
                "States, capitals, culture और important places।",
                6
        );

        addChapter(
                chapters,
                "World Around Us",
                "Countries, continents और major landmarks।",
                6
        );

        addChapter(
                chapters,
                "Science and Inventions",
                "Scientists, discoveries और useful inventions।",
                6
        );

        addChapter(
                chapters,
                "Nature and Wildlife",
                "Plants, animals और environmental awareness।",
                6
        );

        addChapter(
                chapters,
                "Sports and Awards",
                "Major sports, players और awards।",
                6
        );

        addChapter(
                chapters,
                "Brain Booster",
                "Puzzles, reasoning और interesting facts।",
                7
        );

        return chapters;
    }

    @NonNull
    private static List<ChapterItem> getGenericChapters(
            String subjectName,
            int classNumber
    ) {
        List<ChapterItem> chapters = new ArrayList<>();
        String normalized = normalizeSubjectName(subjectName);
        String[] topics;

        if (normalized.contains("math")) {
            topics = classNumber <= 5
                    ? new String[]{"Numbers", "Basic Operations", "Fractions", "Shapes & Geometry", "Measurement", "Patterns & Data"}
                    : classNumber <= 10
                    ? new String[]{"Number Systems", "Algebra", "Geometry", "Mensuration", "Data Handling", "Probability"}
                    : new String[]{"Sets & Functions", "Algebra", "Coordinate Geometry", "Calculus", "Vectors & 3D", "Statistics & Probability"};
        } else if (normalized.contains("science")
                || normalized.contains("physics")
                || normalized.contains("chemistry")
                || normalized.contains("biology")) {
            topics = classNumber <= 5
                    ? new String[]{"Living World", "Food & Health", "Materials", "Motion & Energy", "Earth & Environment", "Activities & Revision"}
                    : new String[]{"Matter & Materials", "Living Organisms", "Force & Energy", "Earth & Environment", "Health & Technology", "Experiments & Revision"};
        } else if (normalized.contains("english")
                || normalized.contains("hindi")
                || normalized.contains("sanskrit")
                || normalized.contains("language")) {
            topics = new String[]{"Reading", "Literature", "Grammar", "Vocabulary", "Writing Skills", "Speaking & Revision"};
        } else if (normalized.contains("social")
                || normalized.contains("history")
                || normalized.contains("geography")
                || normalized.contains("civics")
                || normalized.contains("political")) {
            topics = new String[]{"History", "Geography", "Civics", "Economics", "Maps & Projects", "Revision"};
        } else if (normalized.contains("computer")
                || normalized.contains("information technology")) {
            topics = new String[]{"Computer Fundamentals", "Digital Safety", "Productivity Tools", "Coding & Logic", "Data & Internet", "Practical Project"};
        } else if (normalized.contains("account")
                || normalized.contains("business")
                || normalized.contains("economic")) {
            topics = new String[]{"Core Concepts", "Records & Data", "Business Environment", "Applied Problems", "Case Studies", "Revision"};
        } else {
            topics = new String[]{"Core Concepts", "Key Topics", "Learning Skills", "Applications", "Activity & Project", "Revision & Practice"};
        }

        for (String topic : topics) {
            addChapter(
                    chapters,
                    topic,
                    "Class "
                            + classNumber
                            + " के लिए editable starter topic। Parent इसे school book के अनुसार बदल सकते हैं।",
                    5
            );
        }

        return chapters;
    }

    private static void addChapter(
            @NonNull List<ChapterItem> chapters,
            @NonNull String title,
            @NonNull String description,
            int lessonCount
    ) {
        chapters.add(
                new ChapterItem(
                        chapters.size() + 1,
                        title,
                        description,
                        lessonCount,
                        0
                )
        );
    }
}
