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
            currentComponent = "JvmInterpreter getstatic, putstatic, and cached invokedynamic GetStatic/PutStatic targets execute target <clinit>:()V before reading or storing prepared JvmStaticFields",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "static method active-use operations",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmInterpreter invokestatic and cached invokedynamic InvokeStatic targets execute target <clinit>:()V before invoking the resolved static method and share the initialization state ledger with class initializer and callee frames",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "active use triggers initialization",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "new, getstatic, putstatic, invokestatic, and cached invokedynamic GetStatic/PutStatic/InvokeStatic targets execute target <clinit>:()V before object allocation, static field access, or static method access; recursive ordering, waiting, and error transitions remain pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "class initialization state machine",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmClassInitializationStates models prepared, initializing, initialized, and erroneous states; getstatic drives Prepared -> Initializing -> Initialized around successful <clinit> execution, while waiting semantics remain pending",
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
            currentComponent = "Active use of a class already marked erroneous throws guest NoClassDefFoundError; interpreted <clinit> abrupt completion marks the class erroneous, preserves modeled and athrow-propagated Error subclasses as-is, and wraps non-Error guest VM exceptions as ExceptionInInitializerError; deeper Throwable object cause/linkage modeling remains pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "native class initialization boundary",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "Host delegated classes are visualized as an opaque boundary, not a guest initialization state",
            status = InitializationCoverageStatus.NotYetImplemented,
        ),
    )
}
