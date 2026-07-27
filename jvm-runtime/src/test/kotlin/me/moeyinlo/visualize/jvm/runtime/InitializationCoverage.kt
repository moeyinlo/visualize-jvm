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
            currentComponent = "new, getstatic, putstatic, invokestatic, and cached invokedynamic GetStatic/PutStatic/InvokeStatic targets execute target <clinit>:()V before object allocation, static field access, or static method access; direct superclass recursion and recursive default-method superinterface initialization are covered for modeled classes, while duplicate superinterface ordering edge cases and failure-path scheduler notification/retry integration remain pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "class initialization state machine",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmClassInitializationStates models prepared, initializing, initialized, and erroneous states; getstatic drives Prepared -> Initializing -> Initialized around successful <clinit> execution, records cross-thread active-use waiters, suspends them on the class mirror monitor, JvmClassInitializationStates releases waiter ids on terminal outcomes, and successful initialization resumes scheduler-tracked waiters; failure-path scheduler notification/retry integration remains pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmClassInitializationStateTest",
        ),
        InitializationCoverageEntry(
            rule = "superclass and superinterface initialization",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "Modeled classes initialize their direct superclass and recursive superinterfaces that declare default methods before executing the target class initializer; duplicate superinterface ordering edge cases remain pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "initialization error handling",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "Active use of a class already marked erroneous throws guest NoClassDefFoundError; interpreted <clinit> abrupt completion marks the class erroneous, dependency initialization failure marks the current class erroneous too, preserves modeled and athrow-propagated Error subclasses as-is, and wraps non-Error guest VM exceptions as guest ExceptionInInitializerError objects whose throwable payload cause points at the original guest failure; Throwable constructor message and stack-trace initialization remain pending",
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
