package me.moeyinlo.visualize.jvm.runtime

enum class ClassLoadingCoverageStatus {
    Implemented,
    PartiallyImplemented,
    HostDelegationBoundary,
    NotYetImplemented,
}

data class ClassLoadingCoverageEntry(
    val rule: String,
    val specSection: String,
    val currentComponent: String,
    val status: ClassLoadingCoverageStatus,
    val coveringTestClass: String? = null,
)

object ClassLoadingCoverage {
    val entries: List<ClassLoadingCoverageEntry> = listOf(
        ClassLoadingCoverageEntry(
            rule = "JVM startup main entry point",
            specSection = "JVMS 5.2 Java Virtual Machine Startup",
            currentComponent = "JvmStartupResolver selects public static main(String[]) from the method area and maps startup lookup failures to guest throwable identities",
            status = ClassLoadingCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmStartupTest",
        ),
        ClassLoadingCoverageEntry(
            rule = "binary name to class bytes",
            specSection = "JVMS 5.3 Creation and Loading",
            currentComponent = "No runtime classpath loader yet; ClassFileParser parses bytes after external selection",
            status = ClassLoadingCoverageStatus.NotYetImplemented,
        ),
        ClassLoadingCoverageEntry(
            rule = "bootstrap and user-defined loader identity",
            specSection = "JVMS 5.3 Creation and Loading",
            currentComponent = "JvmClassLoaderIdentity and JvmLoadedClassKey model loader-qualified class identity",
            status = ClassLoadingCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmClassLoaderIdentityTest",
        ),
        ClassLoadingCoverageEntry(
            rule = "array class creation",
            specSection = "JVMS 5.3.3 Creating Array Classes",
            currentComponent = "JvmClassHierarchy models array assignability without defining array Class objects",
            status = ClassLoadingCoverageStatus.NotYetImplemented,
        ),
        ClassLoadingCoverageEntry(
            rule = "loading constraints",
            specSection = "JVMS 5.3.4 Loading Constraints",
            currentComponent = "JvmLoadingConstraintSet models initiating-loader constraint groups and LinkageError violations",
            status = ClassLoadingCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmLoadingConstraintSetTest",
        ),
        ClassLoadingCoverageEntry(
            rule = "module and layer metadata",
            specSection = "JVMS 5.3.6 Modules and Layers",
            currentComponent = "JvmModuleLayer models module descriptors, parent layers, readability, and same-layer package ownership",
            status = ClassLoadingCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmModuleLayerTest",
        ),
        ClassLoadingCoverageEntry(
            rule = "verification during linking boundary",
            specSection = "JVMS 5.4.1 Verification",
            currentComponent = "JvmClassLinker invokes a verifier boundary before recording linked classes and maps failures to guest VerifyError",
            status = ClassLoadingCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmClassLinkerTest",
        ),
        ClassLoadingCoverageEntry(
            rule = "erroneous class state",
            specSection = "JVMS 5.3.5 Deriving a Class from a class File Representation",
            currentComponent = "No class state machine yet, so failed derivation is not remembered per loader/name",
            status = ClassLoadingCoverageStatus.NotYetImplemented,
        ),
        ClassLoadingCoverageEntry(
            rule = "class loader consistency checks",
            specSection = "JVMS 5.3.5 Deriving a Class from a class File Representation",
            currentComponent = "No ClassLoader delegation or constraint consistency checker yet",
            status = ClassLoadingCoverageStatus.NotYetImplemented,
        ),
        ClassLoadingCoverageEntry(
            rule = "class hierarchy lookup for already loaded definitions",
            specSection = "JVMS 5.3 Creation and Loading",
            currentComponent = "JvmClassHierarchy",
            status = ClassLoadingCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmMethodResolutionTest",
        ),
        ClassLoadingCoverageEntry(
            rule = "missing class during symbolic reference lookup",
            specSection = "JVMS 5.3 Creation and Loading",
            currentComponent = "JvmClassHierarchy strict resolution errors",
            status = ClassLoadingCoverageStatus.Implemented,
            coveringTestClass = "JvmFieldResolutionTest",
        ),
        ClassLoadingCoverageEntry(
            rule = "class loading event visualization",
            specSection = "JVMS 5.3 Creation and Loading",
            currentComponent = "ClassLoadingEventsView",
            status = ClassLoadingCoverageStatus.PartiallyImplemented,
            coveringTestClass = "ClassLoadingEventsViewTest",
        ),
        ClassLoadingCoverageEntry(
            rule = "host delegated platform class boundary",
            specSection = "JVMS 5.3 Creation and Loading",
            currentComponent = "HostDelegationEventsView documents opaque JDK + whitelist delegation decisions",
            status = ClassLoadingCoverageStatus.HostDelegationBoundary,
            coveringTestClass = "HostDelegationEventsViewTest",
        ),
    )
}
