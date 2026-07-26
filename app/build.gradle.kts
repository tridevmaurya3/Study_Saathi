import java.util.Properties

plugins {
    id("com.android.application")

    alias(
        libs.plugins.google.services
    )
}

/*
 * Google Books API key को project की local.properties file से पढ़ा जाता है।
 *
 * local.properties सामान्यतः Git repository में commit नहीं होती।
 * Key उपलब्ध न होने पर empty value उपयोग होगी, जिससे project build
 * होना बंद नहीं होगा और Open Library fallback काम कर सकेगी।
 */
val localProperties =
    Properties().apply {
        val localPropertiesFile =
            rootProject.file(
                "local.properties"
            )

        if (localPropertiesFile.exists()) {
            localPropertiesFile
                .inputStream()
                .use { inputStream ->
                    load(
                        inputStream
                    )
                }
        }
    }

val googleBooksApiKey =
    localProperties
        .getProperty(
            "GOOGLE_BOOKS_API_KEY",
            ""
        )
        .trim()

/*
 * BuildConfig String के लिए backslash और quotation marks को safely
 * escape किया जाता है।
 */
val escapedGoogleBooksApiKey =
    googleBooksApiKey
        .replace(
            "\\",
            "\\\\"
        )
        .replace(
            "\"",
            "\\\""
        )

android {
    namespace =
        "com.tridev.studysaathi"

    compileSdk =
        36

    defaultConfig {
        applicationId =
            "com.tridev.studysaathi"

        minSdk =
            26

        targetSdk =
            36

        versionCode =
            1

        versionName =
            "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        /*
         * Java code में यह value आगे ऐसे उपलब्ध होगी:
         *
         * BuildConfig.GOOGLE_BOOKS_API_KEY
         *
         * API key खाली होने पर Google Books anonymous request के बाद
         * Open Library fallback उपलब्ध रहेगी।
         */
        buildConfigField(
            "String",
            "GOOGLE_BOOKS_API_KEY",
            "\"$escapedGoogleBooksApiKey\""
        )
    }

    buildTypes {
        debug {
            /*
             * Debug build भी local.properties वाली key उपयोग करेगी।
             * API restriction में debug SHA-1 आगे जोड़ा जाएगा।
             */
            isMinifyEnabled =
                false
        }

        release {
            isMinifyEnabled =
                false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding =
            true

        /*
         * Custom GOOGLE_BOOKS_API_KEY field के लिए BuildConfig
         * generation explicitly enable होना जरूरी है।
         */
        buildConfig =
            true
    }
}

dependencies {
    implementation(
        "androidx.appcompat:appcompat:1.7.0"
    )

    implementation(
        "com.google.android.material:material:1.12.0"
    )

    implementation(
        "androidx.activity:activity:1.9.1"
    )

    implementation(
        "androidx.constraintlayout:constraintlayout:2.1.4"
    )

    implementation(
        "androidx.recyclerview:recyclerview:1.3.2"
    )

    implementation(
        "androidx.cardview:cardview:1.0.0"
    )

    implementation(
        "androidx.room:room-runtime:2.8.4"
    )

    annotationProcessor(
        "androidx.room:room-compiler:2.8.4"
    )

    implementation(
        "androidx.work:work-runtime:2.11.2"
    )

    /*
     * ML Kit Latin Text Recognition.
     *
     * यह bundled model English text, numbers, publisher names,
     * book titles, class information और ISBN text पढ़ता है।
     */
    implementation(
        "com.google.mlkit:text-recognition:16.0.1"
    )

    /*
     * ML Kit Devanagari Text Recognition.
     *
     * यह bundled model Hindi और Sanskrit text पढ़ता है।
     */
    implementation(
        "com.google.mlkit:text-recognition-devanagari:16.0.1"
    )

    /*
     * ML Kit Barcode Scanning.
     *
     * यह ISBN, EAN-13, EAN-8, UPC और दूसरे supported
     * book barcodes scan करता है।
     */
    implementation(
        "com.google.mlkit:barcode-scanning:17.3.0"
    )

    /*
     * Firebase BoM सभी Firebase Android libraries के compatible
     * versions manage करता है।
     */
    implementation(
        platform(
            libs.firebase.bom
        )
    )

    /*
     * Secure Study Saathi cloud accounts के लिए
     * Firebase Authentication।
     */
    implementation(
        libs.firebase.auth
    )

    /*
     * Cloud backup और synchronization data के लिए
     * Cloud Firestore।
     */
    implementation(
        libs.firebase.firestore
    )

    testImplementation(
        "junit:junit:4.13.2"
    )

    androidTestImplementation(
        "androidx.test.ext:junit:1.2.1"
    )

    androidTestImplementation(
        "androidx.test.espresso:espresso-core:3.6.1"
    )
}