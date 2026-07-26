package me.moeyinlo.visualize.jvm.runtime

enum class InitializationCoverageStatus {
    Implemented,
    PartiallyImplemented,
    NotYetImplemented,
}

data class InitializationCoverageEntry(
    val rule: String,
    val specSection: String,
    val currentComponent: String,
    val status: InitializationCoverageStatus,
    val coveringTestClass: String? = null,
)

object InitializationCoverage {
    val entries: List<InitializationCoverageEntry> = listOf(
        InitializationCoverageEntry(
            rule = "preparation default values",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmStaticFields.defaultFieldValue",
            status = InitializationCoverageStatus.Implemented,
            coveringTestClass = "JvmStaticFieldsTest",
        ),
        InitializationCoverageEntry(
            rule = "ConstantValue attribute assignment",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "ClassFileParser parses ConstantValue and JvmStaticFields.prepare assigns modeled ConstantValue metadata; classfile-to-runtime metadata wiring remains pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmStaticFieldsTest",
        ),
        InitializationCoverageEntry(
            rule = "static field active-use operations",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmInterpreter getstatic and putstatic execute against prepared JvmStaticFields",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "static method active-use operations",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmInterpreter invokestatic resolves and executes methods without class initialization scheduling",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "active use triggers initialization",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "No class initialization scheduler before new/getstatic/putstatic/invokestatic yet",
            status = InitializationCoverageStatus.NotYetImplemented,
        ),
        InitializationCoverageEntry(
            rule = "class initialization state machine",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "No verified/prepared/initializing/initialized/erroneous class state table yet",
            status = InitializationCoverageStatus.NotYetImplemented,
        ),
        InitializationCoverageEntry(
            rule = "superclass and superinterface initialization",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "No recursive superclass/default-method superinterface initialization ordering yet",
            status = InitializationCoverageStatus.NotYetImplemented,
        ),
        InitializationCoverageEntry(
            rule = "initialization error handling",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "No ExceptionInInitializerError or NoClassDefFoundError transition for failed <clinit> yet",
            status = InitializationCoverageStatus.NotYetImplemented,
        ),
        InitializationCoverageEntry(
            rule = "native class initialization boundary",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "Host delegated classes are visualized as an opaque boundary, not a guest initialization state",
            status = InitializationCoverageStatus.NotYetImplemented,
        ),
    )
}
