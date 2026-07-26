package me.moeyinlo.visualize.jvm.jni

class JvmNativeLibraryLoader(
    private val catalog: JvmNativeLibraryCatalog,
    private val lifecycle: JvmNativeLibraryLifecycle,
) {
    fun loadLibrary(
        logicalName: String,
        javaVm: JvmSimulatedJavaVm,
    ): JvmLoadedNativeLibrary =
        lifecycle.load(
            library = catalog.resolveOrThrow(logicalName),
            javaVm = javaVm,
        )
}
