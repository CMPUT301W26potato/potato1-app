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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Prevents the test APK from pulling the wrong protobuf (NoSuchMethodError: registerDefaultInstance).
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
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    testImplementation("org.mockito:mockito-core:5.11.0")
    // Explicit protobuf-javalite so Firestore and tests share a compatible version
    implementation("com.google.protobuf:protobuf-javalite:3.25.3")
    // Force test APK to use the same protobuf (fixes NoSuchMethodError: registerDefaultInstance in instrumentation)
    androidTestImplementation("com.google.protobuf:protobuf-javalite:3.25.3")
    //
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")


}