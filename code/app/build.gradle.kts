plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.waitwell"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.waitwell"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// CRITICAL: Force protobuf to 3.21.12 for Firestore 25.0.0 compatibility
// Firestore 25.0.0 was compiled against protobuf 3.21.12.
// Version 3.25+ removed GeneratedMessageLite.registerDefaultInstance(),
// causing NoSuchMethodError at runtime. This must be enforced for ALL configs.
configurations.all {
    resolutionStrategy {
        force("com.google.protobuf:protobuf-javalite:3.25.3")
        force("com.google.protobuf:protobuf-java:3.25.3")
    }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Espresso Intents so we can assert navigation between activities
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    // Espresso contrib for RecyclerViewActions and stuff
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    // Firebase BOM first – all Firebase libs use compatible versions (no versions on Firebase deps)
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-firestore:24.10.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    testImplementation("org.mockito:mockito-core:5.11.0")
    // will force all transitive protobuf deps to 3.21.12, which is what Firestore needs.

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")


    // REHAAN'S ADDITION — US 02.02.02
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
// END REHAAN'S ADDITION
    // REHAAN'S ADDITION — Places autocomplete for location field
    implementation("com.google.android.libraries.places:places:3.4.0")
// END REHAAN'S ADDITION

    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

}