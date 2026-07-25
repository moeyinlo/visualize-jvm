package me.moeyinlo.visualize.jvm.jni

enum class SimulatedJniCoverageStatus {
    Implemented,
    PartiallyImplemented,
    NotYetImplemented,
}

data class SimulatedJniCoverageEntry(
    val rule: String,
    val specSection: String,
    val currentComponent: String,
    val status: SimulatedJniCoverageStatus,
    val coveringTestClass: String? = null,
)

object SimulatedJniCoverage {
    val entries: List<SimulatedJniCoverageEntry> = listOf(
        SimulatedJniCoverageEntry(
            rule = "handle table local references",
            specSection = "JNI local references",
            currentComponent = "JvmJniHandleTable object/class/methodID/fieldID handles",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmJniHandleTableTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "FindClass and class handles",
            specSection = "JNI class operations",
            currentComponent = "JvmSimulatedJniEnvironment.findClass and getObjectClass",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "method and field ID lookup",
            specSection = "JNI method and field operations",
            currentComponent = "GetMethodID/GetStaticMethodID/GetFieldID/GetStaticFieldID backed by JvmClassHierarchy",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "guest method upcalls",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeMethodContext callStaticMethodHandler and callInstanceMethodHandler re-enter guest interpreter",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "instance and static field helpers",
            specSection = "JNI field operations",
            currentComponent = "JvmSimulatedJniEnvironment get/set instance and static primitive/object fields",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "string helpers",
            specSection = "JNI string operations",
            currentComponent = "NewString/NewStringUTF/GetStringLength/GetStringChars/GetStringUTFChars",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "primitive and reference arrays",
            specSection = "JNI array operations",
            currentComponent = "New<Type>Array/Get<Type>ArrayElements/Region/Release plus object array helpers",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "monitor helpers",
            specSection = "JNI monitor operations",
            currentComponent = "JvmSimulatedJniEnvironment.monitorEnter and monitorExit delegate to JvmMonitorState",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "exception helpers",
            specSection = "JNI exception operations",
            currentComponent = "JvmSimulatedJniEnvironment pending exception state covers Throw/ThrowNew/ExceptionOccurred/ExceptionDescribe/ExceptionCheck/ExceptionClear/FatalError with Throwable assignability checks",
            status = SimulatedJniCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "pending exception native-return boundary",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeDowncallReturn.toGuestValue consumes pending JNI exceptions and raises guest VM boundary exceptions",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmPanamaDowncallBackendTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "JNI local frame capacity management",
            specSection = "JNI local reference management",
            currentComponent = "JvmSimulatedJniEnvironment.ensureLocalCapacity records guaranteed local reference capacity, pushLocalFrame records nested frame depth/capacity, and popLocalFrame pops frame depth and rebinds non-null object results; PopLocalFrame scoped reference deletion is not implemented yet",
            status = SimulatedJniCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "weak global references",
            specSection = "JNI weak global references",
            currentComponent = "No weak global handle table or GC-sensitive semantics yet",
            status = SimulatedJniCoverageStatus.NotYetImplemented,
        ),
        SimulatedJniCoverageEntry(
            rule = "critical array and string sections",
            specSection = "JNI critical access operations",
            currentComponent = "No GetPrimitiveArrayCritical/GetStringCritical pinning semantics yet",
            status = SimulatedJniCoverageStatus.NotYetImplemented,
        ),
        SimulatedJniCoverageEntry(
            rule = "direct byte buffers",
            specSection = "JNI NIO direct buffer operations",
            currentComponent = "No NewDirectByteBuffer/GetDirectBufferAddress/GetDirectBufferCapacity model yet",
            status = SimulatedJniCoverageStatus.NotYetImplemented,
        ),
    )
}
