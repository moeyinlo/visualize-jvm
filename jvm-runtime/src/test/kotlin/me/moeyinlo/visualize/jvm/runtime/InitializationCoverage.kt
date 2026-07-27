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
            currentComponent = "new, getstatic, putstatic, invokestatic, and cached invokedynamic GetStatic/PutStatic/InvokeStatic targets execute target <clinit>:()V before object allocation, static field access, or static method access; direct superclass recursion and recursive default-method superinterface initialization are covered for modeled classes, including duplicate shared-superinterface ordering through the runtime hierarchy enumeration; successful and failed terminal outcomes release and resume scheduler-tracked waiters, and resumed waiters can retry active use after successful or failed owner completion",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "class initialization state machine",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmClassInitializationStates models prepared, initializing, initialized, and erroneous states; getstatic drives Prepared -> Initializing -> Initialized around successful <clinit> execution, the scheduled thread loop can run frames against a shared caller-supplied ledger, records cross-thread active-use waiters, suspends them on the class mirror monitor, parks scheduled waiter frames as non-runnable while the owner is still initializing, JvmClassInitializationStates releases waiter ids on terminal outcomes, and successful and failed initialization resume scheduler-tracked waiters, and resumed waiters can retry active use after successful or failed owner completion",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmClassInitializationStateTest",
        ),
        InitializationCoverageEntry(
            rule = "superclass and superinterface initialization",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "Modeled classes initialize their direct superclass and recursive superinterfaces that declare default methods before executing the target class initializer; runtime hierarchy enumeration visits duplicate shared default-method superinterface ancestors once before child interfaces",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "initialization error handling",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "Active use of a class already marked erroneous throws guest NoClassDefFoundError; interpreted <clinit> abrupt completion marks the class erroneous, dependency initialization failure marks the current class erroneous too, preserves modeled and athrow-propagated Error subclasses as-is, and wraps non-Error guest VM exceptions as guest ExceptionInInitializerError objects whose throwable payload cause and ExceptionInInitializerError exception field point at the original guest failure while detailMessage remains null per the Throwable constructor shape, and direct static active-use callers seed an initial active-use stack-trace frame with caller class and method names, and new, getstatic, putstatic, invokestatic, and cached invokedynamic GetStatic/PutStatic/InvokeStatic callers can resolve optional SourceFile plus LineNumberTable line metadata into that frame, including source metadata carried from interpreted invokestatic static callee frames; failure terminal outcomes release and resume scheduler-tracked waiters, while host delegation and broader native initialization boundary handling remain pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "native class initialization boundary",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "Simulated JNI static upcalls initialize interpreted static target classes before invoking the upcall target and share the native caller or interpreter-backed JNI dispatcher initialization-state ledger",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        InitializationCoverageEntry(
            rule = "host delegated native initialization boundary",
            specSection = "JVMS 5.5 Initialization",
            currentComponent = "JvmHostMethodInvoker and JvmHostFieldAccessor static accessors record host-delegated static method active use and static field reads/writes as opaque boundaries before reflective access without mutating guest initialization state, JvmHostActiveUseHandler exposes the runtime contract needed by interpreter wiring, and JvmInterpreter getstatic and putstatic can route active use through that handler to skip guest class initialization; invokestatic, new, invokedynamic, and full host-delegation invocation/access wiring remain pending",
            status = InitializationCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmJdkHostDelegationTest",
        ),
    )
}
