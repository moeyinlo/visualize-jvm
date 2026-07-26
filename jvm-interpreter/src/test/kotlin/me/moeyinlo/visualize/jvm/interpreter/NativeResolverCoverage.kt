package me.moeyinlo.visualize.jvm.interpreter

enum class NativeResolverCoverageStatus {
    Implemented,
    PartiallyImplemented,
    NotYetImplemented,
}

data class NativeResolverCoverageEntry(
    val rule: String,
    val specSection: String,
    val currentComponent: String,
    val status: NativeResolverCoverageStatus,
    val coveringTestClass: String? = null,
)

object NativeResolverCoverage {
    val entries: List<NativeResolverCoverageEntry> = listOf(
        NativeResolverCoverageEntry(
            rule = "native method key identity",
            specSection = "JVMS 5.4.3 Resolution",
            currentComponent = "JvmNativeMethodKey.from includes owner, name, descriptor, and staticness",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        NativeResolverCoverageEntry(
            rule = "VM intrinsic lookup before simulated JNI",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeMethodRegistry.resolve checks intrinsics before simulatedJni",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        NativeResolverCoverageEntry(
            rule = "simulated JNI fallback lookup",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeMethodRegistry.resolve falls back to simulatedJni when no intrinsic is bound",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        NativeResolverCoverageEntry(
            rule = "unresolved native method error",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmInterpreter throws guest UnsatisfiedLinkError for unbound native invocation",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        NativeResolverCoverageEntry(
            rule = "JNI short and long symbol candidates",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeSymbolNameResolver",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmNativeSymbolNameResolverTest",
        ),
        NativeResolverCoverageEntry(
            rule = "native library export lookup",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeLibraryDescriptor.exportFor",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmNativeLibraryDescriptorTest",
        ),
        NativeResolverCoverageEntry(
            rule = "native library loading lifecycle",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeLibraryLifecycle binds, loads, unloads, and calls optional JNI_OnLoad/JNI_OnUnload; JDK NativeLibraries initialization wiring remains pending",
            status = NativeResolverCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmNativeLibraryLifecycleTest",
        ),
        NativeResolverCoverageEntry(
            rule = "JNI_OnLoad registration",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeLibraryLifecycle invokes optional JNI_OnLoad and registers the accepted JNI version in JvmNativeLibraryRegistry",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmNativeLibraryLifecycleTest",
        ),
        NativeResolverCoverageEntry(
            rule = "automatic descriptor-to-library binding",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeLibraryLoader resolves logical loadLibrary names through JvmNativeLibraryCatalog before lifecycle binding",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmNativeLibraryLoaderTest",
        ),
        NativeResolverCoverageEntry(
            rule = "System and Runtime loadLibrary VM intrinsic hooks",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmVmIntrinsics resolves java/lang/System.loadLibrary(Ljava/lang/String;)V and java/lang/Runtime.loadLibrary0(Ljava/lang/Class;Ljava/lang/String;)V and delegates logical library names to the VM native-library load hook",
            status = NativeResolverCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmVmIntrinsicsTest",
        ),        NativeResolverCoverageEntry(
            rule = "interpreter native library load hook dispatch",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmInterpreter threads the VM native-library load hook into invokestatic native dispatch so java/lang/System.loadLibrary can trigger VM-owned loading from interpreted bytecode",
            status = NativeResolverCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),        NativeResolverCoverageEntry(
            rule = "System mapLibraryName VM intrinsic",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmVmIntrinsics resolves java/lang/System.mapLibraryName(Ljava/lang/String;)Ljava/lang/String; through the platform library-name mapper and returns a guest String",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmVmIntrinsicsTest",
        ),
        NativeResolverCoverageEntry(
            rule = "loaded native library export dispatch",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeMethodRegistry resolves loaded library Java_ static and instance exports into simulated JNI downcalls",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        NativeResolverCoverageEntry(
            rule = "registered native dispatch",
            specSection = "JVMS 5.6 Binding Native Method Implementations",
            currentComponent = "JvmNativeMethodRegistry resolves JNI RegisterNatives static and instance entries through loaded library ownership into simulated JNI downcalls",
            status = NativeResolverCoverageStatus.Implemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
    )
}
