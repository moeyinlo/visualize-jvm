import org.gradle.api.tasks.testing.Test

dependencies {
    implementation(project(":jvm-runtime"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
