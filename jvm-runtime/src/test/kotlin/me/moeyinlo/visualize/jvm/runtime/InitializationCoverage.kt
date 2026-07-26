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
            currentComponent = "ClassFileParser parses ConstantValue, JvmClassfileRuntimeAdapter maps numeric and String ConstantValue entries into runtime metadata, and heap-aware JvmStaticFields.prepare assigns modeled ConstantValue metadata to descriptor values or interned guest strings",
            status = InitializationCoverageStatus.Implemented,
            coveringTestClass = "JvmStaticFieldsTest",
        ),
        InitializationCoverageEntry(
            rule = "static field active-use operations",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmInterpreter getstatic and putstatic trigger active-use initialization for resolved static field owners before executing against prepared JvmStaticFields; actual <clinit> bytecode scheduling remains pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "static method active-use operations",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmInterpreter invokestatic triggers active-use initialization for the resolved static method owner and shares the initialization state ledger with interpreted static callee frames; actual <clinit> bytecode scheduling remains pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "active use triggers initialization",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "new, getstatic, putstatic, and invokestatic mark prepared target classes initialized when no class initializer is present; real <clinit> execution, recursive ordering, waiting, and error transitions remain pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "class initialization state machine",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmClassInitializationStates models prepared, initializing, initialized, and erroneous states; interpreter active-use scheduling and waiting semantics remain pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmClassInitializationStateTest",
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
