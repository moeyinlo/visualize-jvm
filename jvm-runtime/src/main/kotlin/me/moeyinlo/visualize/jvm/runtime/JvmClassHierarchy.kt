package me.moeyinlo.visualize.jvm.runtime

data class JvmClassDefinition(
    val internalName: String,
    val superclassName: String? = null,
    val interfaceNames: List<String> = emptyList(),
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
