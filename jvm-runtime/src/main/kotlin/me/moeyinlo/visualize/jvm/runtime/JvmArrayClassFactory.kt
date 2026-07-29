package me.moeyinlo.visualize.jvm.runtime

enum class JvmPrimitiveArrayComponent(
    val descriptor: String,
) {
    Boolean("Z"),
    Byte("B"),
    Char("C"),
    Short("S"),
    Int("I"),
    Long("J"),
    Float("F"),
    Double("D"),
}

data class JvmArrayClassMetadata(
    val internalName: String,
    val component: JvmPrimitiveArrayComponent,
    val definingLoader: JvmClassLoaderIdentity,
)

class JvmArrayClassFactory(
    private val methodArea: JvmMethodArea,
) {
    private val metadataByInternalName = linkedMapOf<String, JvmArrayClassMetadata>()

    fun createPrimitiveArrayClass(component: JvmPrimitiveArrayComponent): JvmMethodAreaEntry {
        val internalName = "[${component.descriptor}"
        val loadedClassKey = JvmLoadedClassKey(
            internalName = internalName,
            definingLoader = JvmClassLoaderIdentity.Bootstrap,
        )
        methodArea.getClass(loadedClassKey)?.let { existing ->
            metadataByInternalName.putIfAbsent(
                internalName,
                primitiveArrayMetadata(internalName, component),
            )
            return existing
        }
        val entry = JvmMethodAreaEntry(
            definition = JvmClassDefinition(
                internalName = internalName,
                superclassName = "java/lang/Object",
                interfaceNames = listOf("java/lang/Cloneable", "java/io/Serializable"),
            ),
            loadedClassKey = loadedClassKey,
            initiatingLoaders = setOf(JvmClassLoaderIdentity.Bootstrap),
        )
        metadataByInternalName[internalName] = primitiveArrayMetadata(internalName, component)
        methodArea.defineClass(entry)
        return entry
    }

    fun metadataFor(internalName: String): JvmArrayClassMetadata? =
        metadataByInternalName[internalName]

    private fun primitiveArrayMetadata(
        internalName: String,
        component: JvmPrimitiveArrayComponent,
    ): JvmArrayClassMetadata =
        JvmArrayClassMetadata(
            internalName = internalName,
            component = component,
            definingLoader = JvmClassLoaderIdentity.Bootstrap,
        )
}
