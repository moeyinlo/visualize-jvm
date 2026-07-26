package me.moeyinlo.visualize.jvm.jni

data class JvmLoadedNativeLibrary(
    val library: JvmNativeLibraryDescriptor,
    val binding: JvmNativeLibraryBinding,
    val onLoadVersion: Int?,
)

data class JvmNativeLibraryUnloadRequest(
    val loadedLibrary: JvmLoadedNativeLibrary,
    val onUnloadInvocation: JvmNativeDowncallInvocation?,
)

class JvmNativeLibraryRegistry {
    private val loadedByLogicalName = linkedMapOf<String, JvmLoadedNativeLibrary>()

    fun markLoaded(
        binding: JvmNativeLibraryBinding,
        onLoadVersion: Int?,
    ): JvmLoadedNativeLibrary {
        val logicalName = binding.library.logicalName
        if (logicalName in loadedByLogicalName) {
            throw JvmNativeLibraryLoadException("native library $logicalName is already loaded")
        }
        val loaded = JvmLoadedNativeLibrary(
            library = binding.library,
            binding = binding,
            onLoadVersion = onLoadVersion,
        )
        loadedByLogicalName[logicalName] = loaded
        return loaded
    }

    fun loadedLibrary(logicalName: String): JvmLoadedNativeLibrary? =
        loadedByLogicalName[logicalName]

    fun markUnloaded(logicalName: String): JvmLoadedNativeLibrary? =
        loadedByLogicalName.remove(logicalName)

    fun prepareOnUnloadInvocation(
        logicalName: String,
        javaVm: JvmSimulatedJavaVm,
    ): JvmNativeDowncallInvocation? =
        loadedByLogicalName[logicalName]
            ?.binding
            ?.onUnloadTarget
            ?.prepareOnUnloadInvocation(javaVm)

    fun unload(
        logicalName: String,
        javaVm: JvmSimulatedJavaVm,
    ): JvmNativeLibraryUnloadRequest? {
        val loaded = loadedByLogicalName.remove(logicalName) ?: return null
        return JvmNativeLibraryUnloadRequest(
            loadedLibrary = loaded,
            onUnloadInvocation = loaded.binding.onUnloadTarget?.prepareOnUnloadInvocation(javaVm),
        )
    }

    fun unloadOrThrow(
        logicalName: String,
        javaVm: JvmSimulatedJavaVm,
    ): JvmNativeLibraryUnloadRequest =
        unload(logicalName, javaVm)
            ?: throw JvmNativeLibraryLoadException("native library $logicalName is not loaded")

    fun resolveExport(signature: JvmNativeGuestMethodSignature): JvmNativeDowncallTarget? =
        loadedByLogicalName.values
            .asSequence()
            .mapNotNull { loaded -> loaded.binding.exportTargets[signature] }
            .firstOrNull()

    fun loadedLibraries(): List<JvmLoadedNativeLibrary> =
        loadedByLogicalName.values.toList()
}

class JvmNativeLibraryLoadException(message: String) : UnsatisfiedLinkError(message)
