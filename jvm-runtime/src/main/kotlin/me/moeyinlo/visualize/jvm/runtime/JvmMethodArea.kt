package me.moeyinlo.visualize.jvm.runtime

data class JvmMethodAreaEntry(
    val definition: JvmClassDefinition,
    val staticFields: JvmStaticFields = JvmStaticFields(),
    val loadedClassKey: JvmLoadedClassKey? = null,
)

class JvmMethodArea {
    private val entriesByClassName = linkedMapOf<String, JvmMethodAreaEntry>()

    val classCount: Int
        get() = entriesByClassName.size

    fun defineClass(entry: JvmMethodAreaEntry) {
        val className = entry.definition.internalName
        require(className.isNotBlank()) { "class internal name must not be blank" }

        val previous = entriesByClassName.putIfAbsent(className, entry)
        if (previous != null) {
            throw JvmMethodAreaDefinitionException("Class $className is already defined in the method area")
        }
    }

    fun getClass(internalName: String): JvmMethodAreaEntry {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }
        return entriesByClassName[internalName]
            ?: throw JvmMethodAreaAccessException("Class $internalName is not defined in the method area")
    }

    fun hasClass(internalName: String): Boolean = internalName in entriesByClassName

    fun classHierarchy(strictClassResolution: Boolean = false): JvmClassHierarchy =
        JvmClassHierarchy(
            classes = entriesByClassName.values.map { entry -> entry.definition },
            strictClassResolution = strictClassResolution,
        )

    fun toList(): List<JvmMethodAreaEntry> = entriesByClassName.values.toList()
}

class JvmMethodAreaDefinitionException(message: String) : IllegalStateException(message)

class JvmMethodAreaAccessException(message: String) : IllegalStateException(message)
