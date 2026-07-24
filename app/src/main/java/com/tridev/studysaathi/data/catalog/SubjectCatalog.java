package com.tridev.studysaathi.data.catalog;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.model.SubjectItem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SubjectCatalog {

    private static final int DEFAULT_CLASS_NUMBER = 6;

    private SubjectCatalog() {
        // Utility class.
    }

    @NonNull
    public static List<SubjectItem> getSubjectsForClass(
            String studentClass
    ) {
        int classNumber = extractClassNumber(studentClass);

        if (classNumber <= 2) {
            return getClassOneToTwoSubjects();
        }

        if (classNumber <= 5) {
            return getClassThreeToFiveSubjects();
        }

        if (classNumber <= 8) {
            return getClassSixToEightSubjects();
        }

        if (classNumber <= 10) {
            return getClassNineToTenSubjects();
        }

        return getClassElevenToTwelveSubjects();
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
            int parsedClass = Integer.parseInt(matcher.group(1));

            if (parsedClass < 1 || parsedClass > 12) {
                return DEFAULT_CLASS_NUMBER;
            }

            return parsedClass;
        } catch (NumberFormatException exception) {
            return DEFAULT_CLASS_NUMBER;
        }
    }

    @NonNull
    private static List<SubjectItem> getClassOneToTwoSubjects() {
        List<SubjectItem> subjects = new ArrayList<>();

        subjects.add(createEnglish());
        subjects.add(createHindi());
        subjects.add(createMathematics());
        subjects.add(createEnvironmentalStudies());
        subjects.add(createComputer());
        subjects.add(createArtAndActivity());

        return subjects;
    }

    @NonNull
    private static List<SubjectItem> getClassThreeToFiveSubjects() {
        List<SubjectItem> subjects = new ArrayList<>();

        subjects.add(createEnglish());
        subjects.add(createHindi());
        subjects.add(createMathematics());
        subjects.add(createEnvironmentalStudies());
        subjects.add(createComputer());
        subjects.add(createGeneralKnowledge());

        return subjects;
    }

    @NonNull
    private static List<SubjectItem> getClassSixToEightSubjects() {
        List<SubjectItem> subjects = new ArrayList<>();

        subjects.add(createEnglish());
        subjects.add(createHindi());
        subjects.add(createMathematics());
        subjects.add(createScience());
        subjects.add(createSocialScience());
        subjects.add(createSanskrit());
        subjects.add(createComputer());
        subjects.add(createGeneralKnowledge());

        return subjects;
    }

    @NonNull
    private static List<SubjectItem> getClassNineToTenSubjects() {
        List<SubjectItem> subjects = new ArrayList<>();

        subjects.add(createEnglish());
        subjects.add(createHindi());
        subjects.add(createMathematics());
        subjects.add(createScience());
        subjects.add(createSocialScience());
        subjects.add(createInformationTechnology());
        subjects.add(createSanskrit());

        return subjects;
    }

    @NonNull
    private static List<SubjectItem> getClassElevenToTwelveSubjects() {
        List<SubjectItem> subjects = new ArrayList<>();

        subjects.add(createEnglish());
        subjects.add(createHindi());
        subjects.add(createMathematics());
        subjects.add(createPhysics());
        subjects.add(createChemistry());
        subjects.add(createBiology());
        subjects.add(createAccountancy());
        subjects.add(createBusinessStudies());
        subjects.add(createEconomics());
        subjects.add(createComputerScience());
        subjects.add(createHistory());
        subjects.add(createGeography());
        subjects.add(createPoliticalScience());

        return subjects;
    }

    private static SubjectItem createEnglish() {
        return new SubjectItem(
                "English",
                "Reading, grammar, writing and literature",
                "A",
                R.color.ss_blue_soft,
                R.color.ss_blue_border,
                R.color.ss_primary
        );
    }

    private static SubjectItem createHindi() {
        return new SubjectItem(
                "Hindi",
                "पठन, व्याकरण, लेखन और साहित्य",
                "अ",
                R.color.ss_yellow_soft,
                R.color.ss_yellow_border,
                R.color.ss_warning
        );
    }

    private static SubjectItem createMathematics() {
        return new SubjectItem(
                "Mathematics",
                "Concepts, examples and step-by-step practice",
                "∑",
                R.color.ss_green_soft,
                R.color.ss_green_border,
                R.color.ss_success
        );
    }

    private static SubjectItem createEnvironmentalStudies() {
        return new SubjectItem(
                "Environmental Studies",
                "Our surroundings, nature and daily life",
                "🌿",
                R.color.ss_teal_soft,
                R.color.ss_teal_border,
                R.color.ss_secondary
        );
    }

    private static SubjectItem createScience() {
        return new SubjectItem(
                "Science",
                "Physics, chemistry, biology and experiments",
                "⚗",
                R.color.ss_teal_soft,
                R.color.ss_teal_border,
                R.color.ss_secondary
        );
    }

    private static SubjectItem createSocialScience() {
        return new SubjectItem(
                "Social Science",
                "History, geography and civics",
                "◎",
                R.color.ss_red_soft,
                R.color.ss_red_border,
                R.color.ss_error
        );
    }

    private static SubjectItem createSanskrit() {
        return new SubjectItem(
                "Sanskrit",
                "संस्कृत भाषा, व्याकरण और पाठ",
                "सं",
                R.color.ss_purple_soft,
                R.color.ss_purple_border,
                R.color.ss_primary_dark
        );
    }

    private static SubjectItem createComputer() {
        return new SubjectItem(
                "Computer",
                "Digital skills, computer basics and technology",
                "</>",
                R.color.ss_blue_soft,
                R.color.ss_blue_border,
                R.color.ss_info
        );
    }

    private static SubjectItem createGeneralKnowledge() {
        return new SubjectItem(
                "General Knowledge",
                "World, India, science and current awareness",
                "?",
                R.color.ss_yellow_soft,
                R.color.ss_yellow_border,
                R.color.ss_warning
        );
    }

    private static SubjectItem createArtAndActivity() {
        return new SubjectItem(
                "Art & Activity",
                "Creative drawing, colouring and activities",
                "✦",
                R.color.ss_red_soft,
                R.color.ss_red_border,
                R.color.ss_error
        );
    }

    private static SubjectItem createInformationTechnology() {
        return new SubjectItem(
                "Information Technology",
                "Digital tools, communication and IT skills",
                "IT",
                R.color.ss_blue_soft,
                R.color.ss_blue_border,
                R.color.ss_info
        );
    }

    private static SubjectItem createPhysics() {
        return new SubjectItem(
                "Physics",
                "Motion, energy, electricity and natural laws",
                "P",
                R.color.ss_blue_soft,
                R.color.ss_blue_border,
                R.color.ss_primary
        );
    }

    private static SubjectItem createChemistry() {
        return new SubjectItem(
                "Chemistry",
                "Matter, reactions, elements and compounds",
                "C",
                R.color.ss_purple_soft,
                R.color.ss_purple_border,
                R.color.ss_primary_dark
        );
    }

    private static SubjectItem createBiology() {
        return new SubjectItem(
                "Biology",
                "Living organisms, cells and life processes",
                "B",
                R.color.ss_green_soft,
                R.color.ss_green_border,
                R.color.ss_success
        );
    }

    private static SubjectItem createAccountancy() {
        return new SubjectItem(
                "Accountancy",
                "Financial records, journals and accounts",
                "₹",
                R.color.ss_teal_soft,
                R.color.ss_teal_border,
                R.color.ss_secondary
        );
    }

    private static SubjectItem createBusinessStudies() {
        return new SubjectItem(
                "Business Studies",
                "Business organisation, management and markets",
                "BS",
                R.color.ss_yellow_soft,
                R.color.ss_yellow_border,
                R.color.ss_warning
        );
    }

    private static SubjectItem createEconomics() {
        return new SubjectItem(
                "Economics",
                "Resources, markets, development and economy",
                "E",
                R.color.ss_red_soft,
                R.color.ss_red_border,
                R.color.ss_error
        );
    }

    private static SubjectItem createComputerScience() {
        return new SubjectItem(
                "Computer Science",
                "Programming, data and computational thinking",
                "{ }",
                R.color.ss_blue_soft,
                R.color.ss_blue_border,
                R.color.ss_info
        );
    }

    private static SubjectItem createHistory() {
        return new SubjectItem(
                "History",
                "Events, societies, cultures and civilisations",
                "H",
                R.color.ss_yellow_soft,
                R.color.ss_yellow_border,
                R.color.ss_warning
        );
    }

    private static SubjectItem createGeography() {
        return new SubjectItem(
                "Geography",
                "Earth, environment, resources and maps",
                "G",
                R.color.ss_green_soft,
                R.color.ss_green_border,
                R.color.ss_success
        );
    }

    private static SubjectItem createPoliticalScience() {
        return new SubjectItem(
                "Political Science",
                "Government, constitution and political systems",
                "PS",
                R.color.ss_purple_soft,
                R.color.ss_purple_border,
                R.color.ss_primary_dark
        );
    }
}