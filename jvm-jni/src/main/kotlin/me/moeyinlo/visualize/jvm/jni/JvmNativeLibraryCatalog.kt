package me.moeyinlo.visualize.jvm.jni

class JvmNativeLibraryCatalog(
    descriptors: Iterable<JvmNativeLibraryDescriptor>,
) {
    private val descriptorsByLogicalName: Map<String, JvmNativeLibraryDescriptor> =
        descriptors.associateByUniqueLogicalName()

    fun resolve(logicalName: String): JvmNativeLibraryDescriptor? =
        descriptorsByLogicalName[logicalName]

    fun resolveOrThrow(logicalName: String): JvmNativeLibraryDescriptor =
        resolve(logicalName)
            ?: throw JvmNativeLibraryLoadException("native library descriptor $logicalName is not configured")

    private fun Iterable<JvmNativeLibraryDescriptor>.associateByUniqueLogicalName():
        Map<String, JvmNativeLibraryDescriptor> {
        val result = linkedMapOf<String, JvmNativeLibraryDescriptor>()
        for (descriptor in this) {
            val previous = result.putIfAbsent(descriptor.logicalName, descriptor)
            require(previous == null) {
                "duplicate native library descriptor for ${descriptor.logicalName}"
            }
        }
        return result
    }
}
