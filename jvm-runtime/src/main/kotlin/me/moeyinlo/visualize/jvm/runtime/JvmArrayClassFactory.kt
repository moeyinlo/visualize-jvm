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

sealed interface JvmArrayComponent {
    val descriptor: String

    data class Primitive(
        val primitive: JvmPrimitiveArrayComponent,
    ) : JvmArrayComponent {
        override val descriptor: String = primitive.descriptor
    }

    data class Reference(
        val internalName: String,
        val definingLoader: JvmClassLoaderIdentity,
    ) : JvmArrayComponent {
        init {
            require(internalName.isNotBlank()) { "reference array component internal name must not be blank" }
        }

        override val descriptor: String = "L$internalName;"
    }

    data class Array(
        val loadedClassKey: JvmLoadedClassKey,
    ) : JvmArrayComponent {
        init {
            require(loadedClassKey.internalName.startsWith("[")) {
                "array component class must use an array internal name: ${loadedClassKey.internalName}"
            }
        }

        override val descriptor: String = loadedClassKey.internalName
    }
}

data class JvmArrayClassMetadata(
    val internalName: String,
    val component: JvmArrayComponent,
    val definingLoader: JvmClassLoaderIdentity,
)

class JvmArrayClassFactory(
    private val methodArea: JvmMethodArea,
) {
    private val metadataByInternalName = linkedMapOf<String, JvmArrayClassMetadata>()
    private val metadataByLoadedClassKey = linkedMapOf<JvmLoadedClassKey, JvmArrayClassMetadata>()

    fun createPrimitiveArrayClass(component: JvmPrimitiveArrayComponent): JvmMethodAreaEntry {
        val internalName = "[${component.descriptor}"
        val loadedClassKey = JvmLoadedClassKey(
            internalName = internalName,
            definingLoader = JvmClassLoaderIdentity.Bootstrap,
        )
        methodArea.getClass(loadedClassKey)?.let { existing ->
            rememberMetadataIfAbsent(
                loadedClassKey = loadedClassKey,
                metadata = arrayMetadata(
                    internalName = internalName,
                    component = JvmArrayComponent.Primitive(component),
                    definingLoader = JvmClassLoaderIdentity.Bootstrap,
                ),
            )
            return existing
        }
        return defineArrayClass(
            internalName = internalName,
            loadedClassKey = loadedClassKey,
            component = JvmArrayComponent.Primitive(component),
        )
    }

    fun createReferenceArrayClass(
        componentInternalName: String,
        componentDefiningLoader: JvmClassLoaderIdentity,
    ): JvmMethodAreaEntry {
        require(componentInternalName.isNotBlank()) { "reference array component internal name must not be blank" }
        val internalName = "[L$componentInternalName;"
        val loadedClassKey = JvmLoadedClassKey(
            internalName = internalName,
            definingLoader = componentDefiningLoader,
        )
        methodArea.getClass(loadedClassKey)?.let { existing ->
            rememberMetadataIfAbsent(
                loadedClassKey = loadedClassKey,
                metadata = arrayMetadata(
                    internalName = internalName,
                    component = JvmArrayComponent.Reference(componentInternalName, componentDefiningLoader),
                    definingLoader = componentDefiningLoader,
                ),
            )
            return existing
        }
        return defineArrayClass(
            internalName = internalName,
            loadedClassKey = loadedClassKey,
            component = JvmArrayComponent.Reference(componentInternalName, componentDefiningLoader),
        )
    }

    fun createArrayClassWithArrayComponent(componentArrayClass: JvmMethodAreaEntry): JvmMethodAreaEntry {
        val componentLoadedClassKey = requireNotNull(componentArrayClass.loadedClassKey) {
            "array component class must have a loaded class key"
        }
        val internalName = "[${componentLoadedClassKey.internalName}"
        val loadedClassKey = JvmLoadedClassKey(
            internalName = internalName,
            definingLoader = componentLoadedClassKey.definingLoader,
        )
        val component = JvmArrayComponent.Array(componentLoadedClassKey)
        methodArea.getClass(loadedClassKey)?.let { existing ->
            rememberMetadataIfAbsent(
                loadedClassKey = loadedClassKey,
                metadata = arrayMetadata(
                    internalName = internalName,
                    component = component,
                    definingLoader = loadedClassKey.definingLoader,
                ),
            )
            return existing
        }
        return defineArrayClass(
            internalName = internalName,
            loadedClassKey = loadedClassKey,
            component = component,
        )
    }

    fun createArrayClassFromDescriptor(
        descriptor: String,
        referenceComponentDefiningLoader: JvmClassLoaderIdentity,
    ): JvmMethodAreaEntry {
        require(descriptor.startsWith("[")) { "array descriptor must start with '[': $descriptor" }
        return createArrayClassFromComponentDescriptor(
            componentDescriptor = descriptor.substring(1),
            referenceComponentDefiningLoader = referenceComponentDefiningLoader,
        )
    }

    fun metadataFor(internalName: String): JvmArrayClassMetadata? =
        metadataByInternalName[internalName]

    fun metadataFor(loadedClassKey: JvmLoadedClassKey): JvmArrayClassMetadata? =
        metadataByLoadedClassKey[loadedClassKey]

    private fun defineArrayClass(
        internalName: String,
        loadedClassKey: JvmLoadedClassKey,
        component: JvmArrayComponent,
    ): JvmMethodAreaEntry {
        val entry = JvmMethodAreaEntry(
            definition = JvmClassDefinition(
                internalName = internalName,
                superclassName = "java/lang/Object",
                interfaceNames = listOf("java/lang/Cloneable", "java/io/Serializable"),
            ),
            loadedClassKey = loadedClassKey,
            initiatingLoaders = setOf(loadedClassKey.definingLoader),
        )
        rememberMetadata(
            loadedClassKey = loadedClassKey,
            metadata = arrayMetadata(
                internalName = internalName,
                component = component,
                definingLoader = loadedClassKey.definingLoader,
            ),
        )
        methodArea.defineClass(entry)
        return entry
    }

    private fun createArrayClassFromComponentDescriptor(
        componentDescriptor: String,
        referenceComponentDefiningLoader: JvmClassLoaderIdentity,
    ): JvmMethodAreaEntry =
        when {
            componentDescriptor.startsWith("[") -> {
                val componentArrayClass = createArrayClassFromDescriptor(
                    descriptor = componentDescriptor,
                    referenceComponentDefiningLoader = referenceComponentDefiningLoader,
                )
                createArrayClassWithArrayComponent(componentArrayClass)
            }

            componentDescriptor.startsWith("L") && componentDescriptor.endsWith(";") -> {
                createReferenceArrayClass(
                    componentInternalName = componentDescriptor.substring(1, componentDescriptor.length - 1),
                    componentDefiningLoader = referenceComponentDefiningLoader,
                )
            }

            else -> createPrimitiveArrayClass(componentDescriptor.toPrimitiveArrayComponent())
        }

    private fun rememberMetadata(
        loadedClassKey: JvmLoadedClassKey,
        metadata: JvmArrayClassMetadata,
    ) {
        metadataByInternalName[metadata.internalName] = metadata
        metadataByLoadedClassKey[loadedClassKey] = metadata
    }

    private fun rememberMetadataIfAbsent(
        loadedClassKey: JvmLoadedClassKey,
        metadata: JvmArrayClassMetadata,
    ) {
        metadataByInternalName.putIfAbsent(metadata.internalName, metadata)
        metadataByLoadedClassKey.putIfAbsent(loadedClassKey, metadata)
    }

    private fun arrayMetadata(
        internalName: String,
        component: JvmArrayComponent,
        definingLoader: JvmClassLoaderIdentity,
    ): JvmArrayClassMetadata =
        JvmArrayClassMetadata(
            internalName = internalName,
            component = component,
            definingLoader = definingLoader,
        )

    private fun String.toPrimitiveArrayComponent(): JvmPrimitiveArrayComponent =
        JvmPrimitiveArrayComponent.entries.firstOrNull { component -> component.descriptor == this }
            ?: throw IllegalArgumentException("unsupported array component descriptor: $this")
}
