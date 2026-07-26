package me.moeyinlo.visualize.jvm.jni

data class JvmLoadedNativeLibrary(
    val library: JvmNativeLibraryDescriptor,
    val binding: JvmNativeLibraryBinding,
    val onLoadVersion: Int?,
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

    fun resolveExport(signature: JvmNativeGuestMethodSignature): JvmNativeDowncallTarget? =
        loadedByLogicalName.values
            .asSequence()
            .mapNotNull { loaded -> loaded.binding.exportTargets[signature] }
            .firstOrNull()

    fun loadedLibraries(): List<JvmLoadedNativeLibrary> =
        loadedByLogicalName.values.toList()
}

class JvmNativeLibraryLoadException(message: String) : UnsatisfiedLinkError(message)
