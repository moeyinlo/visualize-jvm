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
            rule = "JNIEnv function table upcalls",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmSimulatedJniFunctionTable binds Call<Type>Method, CallNonvirtual<Type>Method, and CallStatic<Type>Method helpers to one simulated JNI environment whose upcall dispatcher re-enters guest interpreter execution",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniFunctionTableTest",
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
            currentComponent = "NewString/NewStringUTF/GetStringLength/GetStringChars/GetStringCritical/GetStringRegion/GetStringUTFChars/GetStringUTFRegion/ReleaseString*",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "primitive and reference arrays",
            specSection = "JNI array operations",
            currentComponent = "New<Type>Array/Get<Type>ArrayElements/Region/Release, GetPrimitiveArrayCritical/ReleasePrimitiveArrayCritical, plus object array helpers",
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
            currentComponent = "JvmSimulatedJniEnvironment pending exception state covers Throw/ThrowNew/ExceptionOccurred/ExceptionDescribe without clearing/ExceptionCheck/ExceptionClear/FatalError with Throwable assignability checks",
            status = SimulatedJniCoverageStatus.Implemented,
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
            currentComponent = "JvmSimulatedJniEnvironment.newLocalRef duplicates nullable object local references, isSameObject compares nullable local references by guest identity, deleteLocalRef deletes nullable local references, ensureLocalCapacity records guaranteed local reference capacity, pushLocalFrame records nested frame depth/capacity, and popLocalFrame pops frame depth, deletes scoped handles, and rebinds non-null object results",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "global references",
            specSection = "JNI global references",
            currentComponent = "JvmSimulatedJniEnvironment.newGlobalRef creates object handles outside local frames, deleteGlobalRef releases only global references, and local/global delete helpers reject the wrong reference scope",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "weak global references",
            specSection = "JNI weak global references",
            currentComponent = "JvmSimulatedJniEnvironment.newWeakGlobalRef creates weak-global object handles outside local frames, deleteWeakGlobalRef releases only weak-global references, and object ref type reports weak-global scope",
            status = SimulatedJniCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "critical array and string sections",
            specSection = "JNI critical access operations",
            currentComponent = "GetStringCritical/ReleaseStringCritical and GetPrimitiveArrayCritical/ReleasePrimitiveArrayCritical use simulated copied buffers while keeping pin/no-block implementation obligations explicit",
            status = SimulatedJniCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
        SimulatedJniCoverageEntry(
            rule = "direct byte buffers",
            specSection = "JNI NIO direct buffer operations",
            currentComponent = "NewDirectByteBuffer/GetDirectBufferAddress/GetDirectBufferCapacity map simulated native address and capacity payloads to guest java/nio/DirectByteBuffer handles",
            status = SimulatedJniCoverageStatus.Implemented,
            coveringTestClass = "JvmSimulatedJniEnvironmentTest",
        ),
    )
}
