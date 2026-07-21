package me.moeyinlo.visualize.jvm.runtime

enum class LinkingResolutionCoverageStatus {
    Implemented,
    PartiallyImplemented,
    NotYetImplemented,
}

data class LinkingResolutionCoverageEntry(
    val rule: String,
    val specSection: String,
    val currentComponent: String,
    val status: LinkingResolutionCoverageStatus,
    val coveringTestClass: String? = null,
)

object LinkingResolutionCoverage {
    val entries: List<LinkingResolutionCoverageEntry> = listOf(
        LinkingResolutionCoverageEntry(
            rule = "class and interface resolution",
            specSection = "JVMS 5.4.3.1 Class and Interface Resolution",
            currentComponent = "No runtime symbolic class resolver or initiating-loader lookup yet",
            status = LinkingResolutionCoverageStatus.NotYetImplemented,
        ),
        LinkingResolutionCoverageEntry(
            rule = "field resolution",
            specSection = "JVMS 5.4.3.2 Field Resolution",
            currentComponent = "JvmClassHierarchy.resolveField",
            status = LinkingResolutionCoverageStatus.Implemented,
            coveringTestClass = "JvmFieldResolutionTest",
        ),
        LinkingResolutionCoverageEntry(
            rule = "method resolution",
            specSection = "JVMS 5.4.3.3 Method Resolution",
            currentComponent = "JvmClassHierarchy.resolveMethod",
            status = LinkingResolutionCoverageStatus.Implemented,
            coveringTestClass = "JvmMethodResolutionTest",
        ),
        LinkingResolutionCoverageEntry(
            rule = "interface method resolution",
            specSection = "JVMS 5.4.3.4 Interface Method Resolution",
            currentComponent = "JvmInterpreter accepts CONSTANT_InterfaceMethodref for static interface method invocation",
            status = LinkingResolutionCoverageStatus.PartiallyImplemented,
            coveringTestClass = "JvmInterpreterTest",
        ),
        LinkingResolutionCoverageEntry(
            rule = "method type and method handle resolution",
            specSection = "JVMS 5.4.3.5 Method Type and Method Handle Resolution",
            currentComponent = "ClassFileParser parses constant pool forms but runtime resolution is not implemented",
            status = LinkingResolutionCoverageStatus.NotYetImplemented,
        ),
        LinkingResolutionCoverageEntry(
            rule = "dynamically-computed constants and call sites",
            specSection = "JVMS 5.4.3.6 Dynamically-Computed Constant and Call Site Resolution",
            currentComponent = "GUI event model exists for invokedynamic and condy, but runtime resolution is not implemented",
            status = LinkingResolutionCoverageStatus.NotYetImplemented,
        ),
        LinkingResolutionCoverageEntry(
            rule = "access control during resolution",
            specSection = "JVMS 5.4.4 Access Control",
            currentComponent = "Interpreter checks selected field and method accesses at execution sites, not a complete linking access-control pass",
            status = LinkingResolutionCoverageStatus.NotYetImplemented,
        ),
        LinkingResolutionCoverageEntry(
            rule = "resolution error memoization",
            specSection = "JVMS 5.4.3 Resolution",
            currentComponent = "No per-constant-pool resolution cache or sticky resolution failure table yet",
            status = LinkingResolutionCoverageStatus.NotYetImplemented,
        ),
    )
}
