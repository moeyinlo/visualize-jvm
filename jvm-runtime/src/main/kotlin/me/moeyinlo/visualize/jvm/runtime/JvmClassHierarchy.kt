package me.moeyinlo.visualize.jvm.runtime

data class JvmClassDefinition(
    val internalName: String,
    val superclassName: String? = null,
    val interfaceNames: List<String> = emptyList(),
    val fields: List<JvmFieldDefinition> = emptyList(),
    val methods: List<JvmMethodDefinition> = emptyList(),
)

data class JvmFieldDefinition(
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
    val isPrivate: Boolean = false,
    val isPackagePrivate: Boolean = false,
    val isProtected: Boolean = false,
)

data class JvmResolvedField(
    val ownerClassName: String,
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
    val isPrivate: Boolean = false,
    val isPackagePrivate: Boolean = false,
    val isProtected: Boolean = false,
)

data class JvmMethodDefinition(
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
    val isPrivate: Boolean = false,
    val isPackagePrivate: Boolean = false,
    val isProtected: Boolean = false,
    val code: ByteArray? = null,
    val maxStack: Int = 0,
    val maxLocals: Int = 0,
)

data class JvmResolvedMethod(
    val ownerClassName: String,
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
    val isPrivate: Boolean = false,
    val isPackagePrivate: Boolean = false,
    val isProtected: Boolean = false,
    val code: ByteArray? = null,
    val maxStack: Int = 0,
    val maxLocals: Int = 0,
)

class JvmClassHierarchy(
    classes: Iterable<JvmClassDefinition> = emptyList(),
    private val strictClassResolution: Boolean = false,
) {
    private val classesByName: Map<String, JvmClassDefinition> =
        classes.associateBy { definition -> definition.internalName }

    fun requiresResolvedClasses(): Boolean = strictClassResolution

    fun hasClass(internalName: String): Boolean = internalName in classesByName

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
            ?: throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = ownerClassName,
            )
        return ownerClass.findDeclaredField(name, descriptor)
            ?: findInterfaceField(ownerClass.interfaceNames, name, descriptor)
            ?: findSuperclassField(ownerClass.superclassName, name, descriptor)
            ?: throw JvmNoSuchFieldError(
                guestClassName = "java/lang/NoSuchFieldError",
                message = "$ownerClassName.$name:$descriptor",
            )
    }

    fun resolveMethod(
        ownerClassName: String,
        name: String,
        descriptor: String,
    ): JvmResolvedMethod {
        val ownerClass = classesByName[ownerClassName]
            ?: throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = ownerClassName,
            )
        return ownerClass.findDeclaredMethod(name, descriptor)
            ?: findSuperclassMethod(ownerClass.superclassName, name, descriptor)
            ?: throw JvmNoSuchMethodError(
                guestClassName = "java/lang/NoSuchMethodError",
                message = "$ownerClassName.$name:$descriptor",
            )
    }

    private fun JvmClassDefinition.findDeclaredField(name: String, descriptor: String): JvmResolvedField? =
        fields.firstOrNull { field -> field.name == name && field.descriptor == descriptor }
            ?.let { field ->
                JvmResolvedField(
                    ownerClassName = internalName,
                    name = field.name,
                    descriptor = field.descriptor,
                    isStatic = field.isStatic,
                    isPrivate = field.isPrivate,
                    isPackagePrivate = field.isPackagePrivate,
                    isProtected = field.isProtected,
                )
            }

    private fun JvmClassDefinition.findDeclaredMethod(name: String, descriptor: String): JvmResolvedMethod? =
        methods.firstOrNull { method -> method.name == name && method.descriptor == descriptor }
            ?.let { method ->
                JvmResolvedMethod(
                    ownerClassName = internalName,
                    name = method.name,
                    descriptor = method.descriptor,
                    isStatic = method.isStatic,
                    isPrivate = method.isPrivate,
                    isPackagePrivate = method.isPackagePrivate,
                    isProtected = method.isProtected,
                    code = method.code,
                    maxStack = method.maxStack,
                    maxLocals = method.maxLocals,
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

    private fun findSuperclassMethod(
        superclassName: String?,
        name: String,
        descriptor: String,
    ): JvmResolvedMethod? {
        if (superclassName == null) {
            return null
        }
        val superclass = classesByName[superclassName] ?: return null
        return superclass.findDeclaredMethod(name, descriptor)
            ?: findSuperclassMethod(superclass.superclassName, name, descriptor)
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

class JvmNoClassDefFoundError(
    val guestClassName: String,
    message: String,
) : NoClassDefFoundError(message)

class JvmNoSuchFieldError(
    val guestClassName: String,
    message: String,
) : NoSuchFieldError(message)

class JvmNoSuchMethodError(
    val guestClassName: String,
    message: String,
) : NoSuchMethodError(message)
