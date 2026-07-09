plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "visualize-jvm"

include(
    "jvm-classfile",
    "jvm-verifier",
    "jvm-runtime",
    "jvm-interpreter",
    "jvm-host",
    "jvm-native",
    "jvm-jni",
    "jvm-gui",
    "jvm-asm-oracle",
)
