// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

// Force one protobuf version for app and test APKs (fixes NoSuchMethodError: registerDefaultInstance).
subprojects {
    afterEvaluate {
        configurations.all {
            resolutionStrategy {
                force("com.google.protobuf:protobuf-javalite:3.25.3")
                force("com.google.protobuf:protobuf-java:3.25.3")
            }
        }
    }
}