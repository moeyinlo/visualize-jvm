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
        val field = ownerClass.fields.firstOrNull { field ->
            field.name == name && field.descriptor == descriptor
        } ?: throw JvmFieldResolutionException("Cannot resolve field $ownerClassName.$name:$descriptor")
        return JvmResolvedField(
            ownerClassName = ownerClass.internalName,
            name = field.name,
            descriptor = field.descriptor,
            isStatic = field.isStatic,
        )
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
