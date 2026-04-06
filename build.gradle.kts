plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

subprojects {
    afterEvaluate {
        configurations.all {
            resolutionStrategy {
                force("com.google.protobuf:protobuf-javalite:3.25.3")
                eachDependency {
                    if (requested.group == "com.google.protobuf" && requested.name == "protobuf-lite") {
                        useTarget("com.google.protobuf:protobuf-javalite:3.25.3")
                        because("protobuf-lite duplicates protobuf-javalite / protobuf-java on the classpath")
                    }
                }
            }
        }
    }
}
