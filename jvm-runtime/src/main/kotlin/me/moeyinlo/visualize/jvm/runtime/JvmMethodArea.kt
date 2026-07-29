package me.moeyinlo.visualize.jvm.runtime

data class JvmMethodAreaEntry(
    val definition: JvmClassDefinition,
    val staticFields: JvmStaticFields = JvmStaticFields(),
    val loadedClassKey: JvmLoadedClassKey? = null,
)

class JvmMethodArea {
    private val entriesByLoadedClassKey = linkedMapOf<JvmLoadedClassKey, JvmMethodAreaEntry>()

    val classCount: Int
        get() = entriesByLoadedClassKey.size

    fun defineClass(entry: JvmMethodAreaEntry) {
        val className = entry.definition.internalName
        require(className.isNotBlank()) { "class internal name must not be blank" }

        val loadedClassKey = entry.loadedClassKey ?: JvmLoadedClassKey(
            internalName = className,
            definingLoader = JvmClassLoaderIdentity.Bootstrap,
        )
        require(loadedClassKey.internalName == className) {
            "loaded class key ${loadedClassKey.diagnosticName} must match class internal name $className"
        }
        val previous = entriesByLoadedClassKey.putIfAbsent(loadedClassKey, entry)
        if (previous != null) {
            throw JvmMethodAreaDefinitionException("Class $className is already defined in the method area")
        }
    }

    fun getClass(internalName: String): JvmMethodAreaEntry {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }
        return getClass(JvmLoadedClassKey(internalName, JvmClassLoaderIdentity.Bootstrap))
            ?: throw JvmMethodAreaAccessException("Class $internalName is not defined in the method area")
    }

    fun getClass(loadedClassKey: JvmLoadedClassKey): JvmMethodAreaEntry? =
        entriesByLoadedClassKey[loadedClassKey]

    fun hasClass(internalName: String): Boolean =
        hasClass(JvmLoadedClassKey(internalName, JvmClassLoaderIdentity.Bootstrap))

    fun hasClass(loadedClassKey: JvmLoadedClassKey): Boolean =
        loadedClassKey in entriesByLoadedClassKey

    fun classHierarchy(strictClassResolution: Boolean = false): JvmClassHierarchy =
        JvmClassHierarchy(
            classes = entriesByLoadedClassKey.values.map { entry -> entry.definition },
            strictClassResolution = strictClassResolution,
        )

    fun toList(): List<JvmMethodAreaEntry> = entriesByLoadedClassKey.values.toList()
}

class JvmMethodAreaDefinitionException(message: String) : IllegalStateException(message)

class JvmMethodAreaAccessException(message: String) : IllegalStateException(message)
