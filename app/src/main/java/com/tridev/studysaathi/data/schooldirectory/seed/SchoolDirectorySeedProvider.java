package com.tridev.studysaathi.data.schooldirectory.seed;

import androidx.annotation.NonNull;

import com.tridev.studysaathi.data.schooldirectory.entity
        .DistrictDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity
        .SchoolDirectoryEntity;
import com.tridev.studysaathi.data.schooldirectory.entity
        .StateDirectoryEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SchoolDirectorySeedProvider {

    public static final String STATE_CODE_UTTAR_PRADESH =
            "UP";

    private SchoolDirectorySeedProvider() {
        /*
         * Utility class.
         */
    }

    /**
     * भारत के States और Union Territories की starter list.
     */
    @NonNull
    public static List<StateDirectoryEntity>
    createIndianStatesAndUnionTerritories() {
        long currentTime =
                System.currentTimeMillis();

        List<StateSeed> seeds =
                Arrays.asList(
                        new StateSeed(
                                "AN",
                                "Andaman and Nicobar Islands",
                                "अंडमान और निकोबार द्वीपसमूह"
                        ),
                        new StateSeed(
                                "AP",
                                "Andhra Pradesh",
                                "आंध्र प्रदेश"
                        ),
                        new StateSeed(
                                "AR",
                                "Arunachal Pradesh",
                                "अरुणाचल प्रदेश"
                        ),
                        new StateSeed(
                                "AS",
                                "Assam",
                                "असम"
                        ),
                        new StateSeed(
                                "BR",
                                "Bihar",
                                "बिहार"
                        ),
                        new StateSeed(
                                "CH",
                                "Chandigarh",
                                "चंडीगढ़"
                        ),
                        new StateSeed(
                                "CG",
                                "Chhattisgarh",
                                "छत्तीसगढ़"
                        ),
                        new StateSeed(
                                "DN",
                                "Dadra and Nagar Haveli and Daman and Diu",
                                "दादरा और नगर हवेली तथा दमन और दीव"
                        ),
                        new StateSeed(
                                "DL",
                                "Delhi",
                                "दिल्ली"
                        ),
                        new StateSeed(
                                "GA",
                                "Goa",
                                "गोवा"
                        ),
                        new StateSeed(
                                "GJ",
                                "Gujarat",
                                "गुजरात"
                        ),
                        new StateSeed(
                                "HR",
                                "Haryana",
                                "हरियाणा"
                        ),
                        new StateSeed(
                                "HP",
                                "Himachal Pradesh",
                                "हिमाचल प्रदेश"
                        ),
                        new StateSeed(
                                "JK",
                                "Jammu and Kashmir",
                                "जम्मू और कश्मीर"
                        ),
                        new StateSeed(
                                "JH",
                                "Jharkhand",
                                "झारखंड"
                        ),
                        new StateSeed(
                                "KA",
                                "Karnataka",
                                "कर्नाटक"
                        ),
                        new StateSeed(
                                "KL",
                                "Kerala",
                                "केरल"
                        ),
                        new StateSeed(
                                "LA",
                                "Ladakh",
                                "लद्दाख"
                        ),
                        new StateSeed(
                                "LD",
                                "Lakshadweep",
                                "लक्षद्वीप"
                        ),
                        new StateSeed(
                                "MP",
                                "Madhya Pradesh",
                                "मध्य प्रदेश"
                        ),
                        new StateSeed(
                                "MH",
                                "Maharashtra",
                                "महाराष्ट्र"
                        ),
                        new StateSeed(
                                "MN",
                                "Manipur",
                                "मणिपुर"
                        ),
                        new StateSeed(
                                "ML",
                                "Meghalaya",
                                "मेघालय"
                        ),
                        new StateSeed(
                                "MZ",
                                "Mizoram",
                                "मिजोरम"
                        ),
                        new StateSeed(
                                "NL",
                                "Nagaland",
                                "नागालैंड"
                        ),
                        new StateSeed(
                                "OD",
                                "Odisha",
                                "ओडिशा"
                        ),
                        new StateSeed(
                                "PY",
                                "Puducherry",
                                "पुदुचेरी"
                        ),
                        new StateSeed(
                                "PB",
                                "Punjab",
                                "पंजाब"
                        ),
                        new StateSeed(
                                "RJ",
                                "Rajasthan",
                                "राजस्थान"
                        ),
                        new StateSeed(
                                "SK",
                                "Sikkim",
                                "सिक्किम"
                        ),
                        new StateSeed(
                                "TN",
                                "Tamil Nadu",
                                "तमिलनाडु"
                        ),
                        new StateSeed(
                                "TS",
                                "Telangana",
                                "तेलंगाना"
                        ),
                        new StateSeed(
                                "TR",
                                "Tripura",
                                "त्रिपुरा"
                        ),
                        new StateSeed(
                                "UP",
                                "Uttar Pradesh",
                                "उत्तर प्रदेश"
                        ),
                        new StateSeed(
                                "UK",
                                "Uttarakhand",
                                "उत्तराखंड"
                        ),
                        new StateSeed(
                                "WB",
                                "West Bengal",
                                "पश्चिम बंगाल"
                        )
                );

        List<StateDirectoryEntity> states =
                new ArrayList<>();

        int sortOrder =
                1;

        for (StateSeed seed :
                seeds) {

            StateDirectoryEntity state =
                    new StateDirectoryEntity();

            state.setStateCode(
                    seed.code
            );

            state.setStateName(
                    seed.englishName
            );

            state.setStateNameHindi(
                    seed.hindiName
            );

            state.setActive(
                    true
            );

            state.setSortOrder(
                    sortOrder
            );

            state.setCreatedAt(
                    currentTime
            );

            state.setUpdatedAt(
                    currentTime
            );

            states.add(
                    state
            );

            sortOrder++;
        }

        return Collections.unmodifiableList(
                states
        );
    }

    /**
     * Uttar Pradesh के सभी 75 districts की starter list.
     *
     * District code app का stable internal code है।
     * इसे UDISE district code न माना जाए।
     */
    @NonNull
    public static List<DistrictDirectoryEntity>
    createUttarPradeshDistricts() {
        long currentTime =
                System.currentTimeMillis();

        List<DistrictSeed> seeds =
                Arrays.asList(
                        new DistrictSeed(
                                "UP-AGRA",
                                "Agra",
                                "आगरा"
                        ),
                        new DistrictSeed(
                                "UP-ALIGARH",
                                "Aligarh",
                                "अलीगढ़"
                        ),
                        new DistrictSeed(
                                "UP-AMBEDKAR-NAGAR",
                                "Ambedkar Nagar",
                                "अम्बेडकर नगर"
                        ),
                        new DistrictSeed(
                                "UP-AMETHI",
                                "Amethi",
                                "अमेठी"
                        ),
                        new DistrictSeed(
                                "UP-AMROHA",
                                "Amroha",
                                "अमरोहा"
                        ),
                        new DistrictSeed(
                                "UP-AURAIYA",
                                "Auraiya",
                                "औरैया"
                        ),
                        new DistrictSeed(
                                "UP-AYODHYA",
                                "Ayodhya",
                                "अयोध्या"
                        ),
                        new DistrictSeed(
                                "UP-AZAMGARH",
                                "Azamgarh",
                                "आजमगढ़"
                        ),
                        new DistrictSeed(
                                "UP-BAGHPAT",
                                "Baghpat",
                                "बागपत"
                        ),
                        new DistrictSeed(
                                "UP-BAHRAICH",
                                "Bahraich",
                                "बहराइच"
                        ),
                        new DistrictSeed(
                                "UP-BALLIA",
                                "Ballia",
                                "बलिया"
                        ),
                        new DistrictSeed(
                                "UP-BALRAMPUR",
                                "Balrampur",
                                "बलरामपुर"
                        ),
                        new DistrictSeed(
                                "UP-BANDA",
                                "Banda",
                                "बांदा"
                        ),
                        new DistrictSeed(
                                "UP-BARABANKI",
                                "Barabanki",
                                "बाराबंकी"
                        ),
                        new DistrictSeed(
                                "UP-BAREILLY",
                                "Bareilly",
                                "बरेली"
                        ),
                        new DistrictSeed(
                                "UP-BASTI",
                                "Basti",
                                "बस्ती"
                        ),
                        new DistrictSeed(
                                "UP-BHADOHI",
                                "Bhadohi",
                                "भदोही"
                        ),
                        new DistrictSeed(
                                "UP-BIJNOR",
                                "Bijnor",
                                "बिजनौर"
                        ),
                        new DistrictSeed(
                                "UP-BUDAUN",
                                "Budaun",
                                "बदायूं"
                        ),
                        new DistrictSeed(
                                "UP-BULANDSHAHR",
                                "Bulandshahr",
                                "बुलंदशहर"
                        ),
                        new DistrictSeed(
                                "UP-CHANDAULI",
                                "Chandauli",
                                "चंदौली"
                        ),
                        new DistrictSeed(
                                "UP-CHITRAKOOT",
                                "Chitrakoot",
                                "चित्रकूट"
                        ),
                        new DistrictSeed(
                                "UP-DEORIA",
                                "Deoria",
                                "देवरिया"
                        ),
                        new DistrictSeed(
                                "UP-ETAH",
                                "Etah",
                                "एटा"
                        ),
                        new DistrictSeed(
                                "UP-ETAWAH",
                                "Etawah",
                                "इटावा"
                        ),
                        new DistrictSeed(
                                "UP-FARRUKHABAD",
                                "Farrukhabad",
                                "फर्रुखाबाद"
                        ),
                        new DistrictSeed(
                                "UP-FATEHPUR",
                                "Fatehpur",
                                "फतेहपुर"
                        ),
                        new DistrictSeed(
                                "UP-FIROZABAD",
                                "Firozabad",
                                "फिरोजाबाद"
                        ),
                        new DistrictSeed(
                                "UP-GAUTAM-BUDDHA-NAGAR",
                                "Gautam Buddha Nagar",
                                "गौतम बुद्ध नगर"
                        ),
                        new DistrictSeed(
                                "UP-GHAZIABAD",
                                "Ghaziabad",
                                "गाजियाबाद"
                        ),
                        new DistrictSeed(
                                "UP-GHAZIPUR",
                                "Ghazipur",
                                "गाजीपुर"
                        ),
                        new DistrictSeed(
                                "UP-GONDA",
                                "Gonda",
                                "गोंडा"
                        ),
                        new DistrictSeed(
                                "UP-GORAKHPUR",
                                "Gorakhpur",
                                "गोरखपुर"
                        ),
                        new DistrictSeed(
                                "UP-HAMIRPUR",
                                "Hamirpur",
                                "हमीरपुर"
                        ),
                        new DistrictSeed(
                                "UP-HAPUR",
                                "Hapur",
                                "हापुड़"
                        ),
                        new DistrictSeed(
                                "UP-HARDOI",
                                "Hardoi",
                                "हरदोई"
                        ),
                        new DistrictSeed(
                                "UP-HATHRAS",
                                "Hathras",
                                "हाथरस"
                        ),
                        new DistrictSeed(
                                "UP-JALAUN",
                                "Jalaun",
                                "जालौन"
                        ),
                        new DistrictSeed(
                                "UP-JAUNPUR",
                                "Jaunpur",
                                "जौनपुर"
                        ),
                        new DistrictSeed(
                                "UP-JHANSI",
                                "Jhansi",
                                "झांसी"
                        ),
                        new DistrictSeed(
                                "UP-KANNAUJ",
                                "Kannauj",
                                "कन्नौज"
                        ),
                        new DistrictSeed(
                                "UP-KANPUR-DEHAT",
                                "Kanpur Dehat",
                                "कानपुर देहात"
                        ),
                        new DistrictSeed(
                                "UP-KANPUR-NAGAR",
                                "Kanpur Nagar",
                                "कानपुर नगर"
                        ),
                        new DistrictSeed(
                                "UP-KASGANJ",
                                "Kasganj",
                                "कासगंज"
                        ),
                        new DistrictSeed(
                                "UP-KAUSHAMBI",
                                "Kaushambi",
                                "कौशांबी"
                        ),
                        new DistrictSeed(
                                "UP-KHERI",
                                "Lakhimpur Kheri",
                                "लखीमपुर खीरी"
                        ),
                        new DistrictSeed(
                                "UP-KUSHINAGAR",
                                "Kushinagar",
                                "कुशीनगर"
                        ),
                        new DistrictSeed(
                                "UP-LALITPUR",
                                "Lalitpur",
                                "ललितपुर"
                        ),
                        new DistrictSeed(
                                "UP-LUCKNOW",
                                "Lucknow",
                                "लखनऊ"
                        ),
                        new DistrictSeed(
                                "UP-MAHARAJGANJ",
                                "Maharajganj",
                                "महराजगंज"
                        ),
                        new DistrictSeed(
                                "UP-MAHOBA",
                                "Mahoba",
                                "महोबा"
                        ),
                        new DistrictSeed(
                                "UP-MAINPURI",
                                "Mainpuri",
                                "मैनपुरी"
                        ),
                        new DistrictSeed(
                                "UP-MATHURA",
                                "Mathura",
                                "मथुरा"
                        ),
                        new DistrictSeed(
                                "UP-MAU",
                                "Mau",
                                "मऊ"
                        ),
                        new DistrictSeed(
                                "UP-MEERUT",
                                "Meerut",
                                "मेरठ"
                        ),
                        new DistrictSeed(
                                "UP-MIRZAPUR",
                                "Mirzapur",
                                "मिर्जापुर"
                        ),
                        new DistrictSeed(
                                "UP-MORADABAD",
                                "Moradabad",
                                "मुरादाबाद"
                        ),
                        new DistrictSeed(
                                "UP-MUZAFFARNAGAR",
                                "Muzaffarnagar",
                                "मुजफ्फरनगर"
                        ),
                        new DistrictSeed(
                                "UP-PILIBHIT",
                                "Pilibhit",
                                "पीलीभीत"
                        ),
                        new DistrictSeed(
                                "UP-PRATAPGARH",
                                "Pratapgarh",
                                "प्रतापगढ़"
                        ),
                        new DistrictSeed(
                                "UP-PRAYAGRAJ",
                                "Prayagraj",
                                "प्रयागराज"
                        ),
                        new DistrictSeed(
                                "UP-RAE-BARELI",
                                "Rae Bareli",
                                "रायबरेली"
                        ),
                        new DistrictSeed(
                                "UP-RAMPUR",
                                "Rampur",
                                "रामपुर"
                        ),
                        new DistrictSeed(
                                "UP-SAHARANPUR",
                                "Saharanpur",
                                "सहारनपुर"
                        ),
                        new DistrictSeed(
                                "UP-SAMBHAL",
                                "Sambhal",
                                "सम्भल"
                        ),
                        new DistrictSeed(
                                "UP-SANT-KABIR-NAGAR",
                                "Sant Kabir Nagar",
                                "संत कबीर नगर"
                        ),
                        new DistrictSeed(
                                "UP-SHAHJAHANPUR",
                                "Shahjahanpur",
                                "शाहजहांपुर"
                        ),
                        new DistrictSeed(
                                "UP-SHAMLI",
                                "Shamli",
                                "शामली"
                        ),
                        new DistrictSeed(
                                "UP-SHRAVASTI",
                                "Shravasti",
                                "श्रावस्ती"
                        ),
                        new DistrictSeed(
                                "UP-SIDDHARTHNAGAR",
                                "Siddharthnagar",
                                "सिद्धार्थनगर"
                        ),
                        new DistrictSeed(
                                "UP-SITAPUR",
                                "Sitapur",
                                "सीतापुर"
                        ),
                        new DistrictSeed(
                                "UP-SONBHADRA",
                                "Sonbhadra",
                                "सोनभद्र"
                        ),
                        new DistrictSeed(
                                "UP-SULTANPUR",
                                "Sultanpur",
                                "सुल्तानपुर"
                        ),
                        new DistrictSeed(
                                "UP-UNNAO",
                                "Unnao",
                                "उन्नाव"
                        ),
                        new DistrictSeed(
                                "UP-VARANASI",
                                "Varanasi",
                                "वाराणसी"
                        )
                );

        List<DistrictDirectoryEntity> districts =
                new ArrayList<>();

        int sortOrder =
                1;

        for (DistrictSeed seed :
                seeds) {

            DistrictDirectoryEntity district =
                    new DistrictDirectoryEntity();

            district.setDistrictCode(
                    seed.code
            );

            district.setStateCode(
                    STATE_CODE_UTTAR_PRADESH
            );

            district.setDistrictName(
                    seed.englishName
            );

            district.setDistrictNameHindi(
                    seed.hindiName
            );

            district.setActive(
                    true
            );

            district.setSortOrder(
                    sortOrder
            );

            district.setCreatedAt(
                    currentTime
            );

            district.setUpdatedAt(
                    currentTime
            );

            districts.add(
                    district
            );

            sortOrder++;
        }

        return Collections.unmodifiableList(
                districts
        );
    }

    /**
     * Curriculum setup में दिखाई जाने वाली standard Board list.
     *
     * School records उपलब्ध न होने पर भी यह list काम करेगी।
     */
    @NonNull
    public static List<String>
    createSupportedEducationBoards() {
        return Collections.unmodifiableList(
                Arrays.asList(
                        "CBSE",
                        "CISCE",
                        "STATE_BOARD",
                        "IB",
                        "CAMBRIDGE",
                        "NIOS",
                        "OTHER"
                )
        );
    }

    /**
     * Starter build में कोई fabricated school record नहीं रखा जाएगा।
     *
     * वास्तविक records CSV/JSON import या authorized directory
     * update से जोड़े जाएँगे।
     */
    @NonNull
    public static List<SchoolDirectoryEntity>
    createStarterSchools() {
        return Collections.emptyList();
    }

    @NonNull
    public static StarterDirectoryData
    createStarterDirectoryData() {
        return new StarterDirectoryData(
                createIndianStatesAndUnionTerritories(),
                // No partial State-only district list is activated. Existing
                // legacy directory rows are preserved but the UI does not use them.
                Collections.emptyList(),
                createStarterSchools(),
                createSupportedEducationBoards()
        );
    }

    public static final class StarterDirectoryData {

        @NonNull
        private final List<StateDirectoryEntity> states;

        @NonNull
        private final List<DistrictDirectoryEntity> districts;

        @NonNull
        private final List<SchoolDirectoryEntity> schools;

        @NonNull
        private final List<String> educationBoards;

        private StarterDirectoryData(
                @NonNull List<StateDirectoryEntity> states,
                @NonNull List<DistrictDirectoryEntity> districts,
                @NonNull List<SchoolDirectoryEntity> schools,
                @NonNull List<String> educationBoards
        ) {
            this.states =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    states
                            )
                    );

            this.districts =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    districts
                            )
                    );

            this.schools =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    schools
                            )
                    );

            this.educationBoards =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    educationBoards
                            )
                    );
        }

        @NonNull
        public List<StateDirectoryEntity> getStates() {
            return states;
        }

        @NonNull
        public List<DistrictDirectoryEntity> getDistricts() {
            return districts;
        }

        @NonNull
        public List<SchoolDirectoryEntity> getSchools() {
            return schools;
        }

        @NonNull
        public List<String> getEducationBoards() {
            return educationBoards;
        }

        public boolean hasSchoolRecords() {
            return !schools.isEmpty();
        }
    }

    private static final class StateSeed {

        @NonNull
        private final String code;

        @NonNull
        private final String englishName;

        @NonNull
        private final String hindiName;

        private StateSeed(
                @NonNull String code,
                @NonNull String englishName,
                @NonNull String hindiName
        ) {
            this.code =
                    code;

            this.englishName =
                    englishName;

            this.hindiName =
                    hindiName;
        }
    }

    private static final class DistrictSeed {

        @NonNull
        private final String code;

        @NonNull
        private final String englishName;

        @NonNull
        private final String hindiName;

        private DistrictSeed(
                @NonNull String code,
                @NonNull String englishName,
                @NonNull String hindiName
        ) {
            this.code =
                    code;

            this.englishName =
                    englishName;

            this.hindiName =
                    hindiName;
        }
    }
}
