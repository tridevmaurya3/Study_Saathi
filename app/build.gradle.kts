plugins {
    id("com.android.application")

    alias(
        libs.plugins.google.services
    )
}

android {
    namespace = "com.tridev.studysaathi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tridev.studysaathi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

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
        viewBinding = true
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
     * Firebase BoM manages compatible versions
     * of all Firebase Android libraries.
     */
    implementation(
        platform(
            libs.firebase.bom
        )
    )

    /*
     * Firebase Authentication will be used
     * for secure Study Saathi cloud accounts.
     */
    implementation(
        libs.firebase.auth
    )

    /*
     * Cloud Firestore will store cloud backup
     * and synchronization data.
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