package me.moeyinlo.visualize.jvm.runtime

data class JvmClassDefinition(
    val internalName: String,
    val superclassName: String? = null,
    val interfaceNames: List<String> = emptyList(),
    val fields: List<JvmFieldDefinition> = emptyList(),
)

data class JvmFieldDefinition(
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
)

data class JvmResolvedField(
    val ownerClassName: String,
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
)

class JvmClassHierarchy(
    classes: Iterable<JvmClassDefinition> = emptyList(),
) {
    private val classesByName: Map<String, JvmClassDefinition> =
        classes.associateBy { definition -> definition.internalName }

    fun isAssignable(sourceClassName: String, targetClassName: String): Boolean {
        if (sourceClassName == targetClassName || targetClassName == "java/lang/Object") {
            return true
        }
        if (sourceClassName.isArrayClassName() && targetClassName == "java/lang/Cloneable") {
            return true
        }
        if (sourceClassName.isArrayClassName() && targetClassName == "java/io/Serializable") {
            return true
        }
        if (sourceClassName.isReferenceArrayClassName() && targetClassName.isReferenceArrayClassName()) {
            return isAssignable(
                sourceClassName.referenceArrayComponentClassName(),
                targetClassName.referenceArrayComponentClassName(),
            )
        }

        val sourceClass = classesByName[sourceClassName] ?: return false
        val superclassName = sourceClass.superclassName
        if (superclassName != null && isAssignable(superclassName, targetClassName)) {
            return true
        }
        return sourceClass.interfaceNames.any { interfaceName ->
            isAssignable(interfaceName, targetClassName)
        }
    }

    fun resolveField(
        ownerClassName: String,
        name: String,
        descriptor: String,
    ): JvmResolvedField {
        val ownerClass = classesByName[ownerClassName]
            ?: throw JvmFieldResolutionException("Cannot resolve field $ownerClassName.$name:$descriptor: class not found")
        return ownerClass.findDeclaredField(name, descriptor)
            ?: findInterfaceField(ownerClass.interfaceNames, name, descriptor)
            ?: findSuperclassField(ownerClass.superclassName, name, descriptor)
            ?: throw JvmFieldResolutionException("Cannot resolve field $ownerClassName.$name:$descriptor")
    }

    private fun JvmClassDefinition.findDeclaredField(name: String, descriptor: String): JvmResolvedField? =
        fields.firstOrNull { field -> field.name == name && field.descriptor == descriptor }
            ?.let { field ->
                JvmResolvedField(
                    ownerClassName = internalName,
                    name = field.name,
                    descriptor = field.descriptor,
                    isStatic = field.isStatic,
                )
            }

    private fun findInterfaceField(
        interfaceNames: List<String>,
        name: String,
        descriptor: String,
        visited: MutableSet<String> = linkedSetOf(),
    ): JvmResolvedField? {
        for (interfaceName in interfaceNames) {
            if (!visited.add(interfaceName)) {
                continue
            }
            val interfaceClass = classesByName[interfaceName] ?: continue
            interfaceClass.findDeclaredField(name, descriptor)?.let { resolved -> return resolved }
            findInterfaceField(interfaceClass.interfaceNames, name, descriptor, visited)?.let { resolved -> return resolved }
        }
        return null
    }

    private fun findSuperclassField(
        superclassName: String?,
        name: String,
        descriptor: String,
    ): JvmResolvedField? {
        if (superclassName == null) {
            return null
        }
        val superclass = classesByName[superclassName] ?: return null
        return superclass.findDeclaredField(name, descriptor)
            ?: findInterfaceField(superclass.interfaceNames, name, descriptor)
            ?: findSuperclassField(superclass.superclassName, name, descriptor)
    }

    private fun String.isArrayClassName(): Boolean = startsWith("[")

    private fun String.isReferenceArrayClassName(): Boolean =
        startsWith("[L") && endsWith(";") || startsWith("[[")

    private fun String.referenceArrayComponentClassName(): String =
        if (startsWith("[L") && endsWith(";")) {
            substring(2, length - 1)
        } else {
            substring(1)
        }

    companion object {
        val Empty: JvmClassHierarchy = JvmClassHierarchy()
    }
}

class JvmFieldResolutionException(message: String) : IllegalStateException(message)
