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

    fun loadHook(javaVm: JvmSimulatedJavaVm): (logicalName: String) -> Unit = { logicalName ->
        loadLibrary(logicalName, javaVm)
    }

    fun unloadHook(javaVm: JvmSimulatedJavaVm): (logicalName: String) -> Unit = { logicalName ->
        unloadLibrary(logicalName, javaVm)
    }

    fun unloadLibrary(
        logicalName: String,
        javaVm: JvmSimulatedJavaVm,
    ): JvmLoadedNativeLibrary =
        lifecycle.unload(
            logicalName = logicalName,
            javaVm = javaVm,
        )
}
