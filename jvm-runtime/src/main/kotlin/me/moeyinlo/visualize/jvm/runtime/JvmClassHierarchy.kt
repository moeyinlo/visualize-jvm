package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.ConstantPool

data class JvmClassDefinition(
    val internalName: String,
    val superclassName: String? = null,
    val interfaceNames: List<String> = emptyList(),
    val fields: List<JvmFieldDefinition> = emptyList(),
    val methods: List<JvmMethodDefinition> = emptyList(),
    val isInterface: Boolean = false,
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

data class JvmExceptionHandler(
    val startPc: Int,
    val endPc: Int,
    val handlerPc: Int,
    val catchClassName: String?,
) {
    init {
        require(startPc >= 0) { "exception handler start_pc must be non-negative: $startPc" }
        require(endPc >= startPc) { "exception handler end_pc must be >= start_pc: $endPc < $startPc" }
        require(handlerPc >= 0) { "exception handler handler_pc must be non-negative: $handlerPc" }
        require(catchClassName == null || catchClassName.isNotBlank()) {
            "exception handler catch class name must not be blank"
        }
    }
}

data class JvmMethodDefinition(
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
    val isPrivate: Boolean = false,
    val isPackagePrivate: Boolean = false,
    val isProtected: Boolean = false,
    val isAbstract: Boolean = false,
    val isNative: Boolean = false,
    val isVarargs: Boolean = false,
    val code: ByteArray? = null,
    val constantPool: ConstantPool? = null,
    val maxStack: Int = 0,
    val maxLocals: Int = 0,
    val exceptionHandlers: List<JvmExceptionHandler> = emptyList(),
)

data class JvmResolvedMethod(
    val ownerClassName: String,
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
    val isPrivate: Boolean = false,
    val isPackagePrivate: Boolean = false,
    val isProtected: Boolean = false,
    val isAbstract: Boolean = false,
    val isNative: Boolean = false,
    val isVarargs: Boolean = false,
    val signaturePolymorphicDeclarationDescriptor: String? = null,
    val code: ByteArray? = null,
    val constantPool: ConstantPool? = null,
    val maxStack: Int = 0,
    val maxLocals: Int = 0,
    val exceptionHandlers: List<JvmExceptionHandler> = emptyList(),
) {
    val isSignaturePolymorphic: Boolean
        get() = ownerClassName == "java/lang/invoke/MethodHandle" &&
            (name == "invoke" || name == "invokeExact") &&
            (signaturePolymorphicDeclarationDescriptor ?: descriptor) == "([Ljava/lang/Object;)Ljava/lang/Object;" &&
            isNative &&
            isVarargs
}

class JvmClassHierarchy(
    classes: Iterable<JvmClassDefinition> = emptyList(),
    private val strictClassResolution: Boolean = false,
) {
    private val classesByName: Map<String, JvmClassDefinition> =
        classes.associateBy { definition -> definition.internalName }

    fun requiresResolvedClasses(): Boolean = strictClassResolution

    fun hasClass(internalName: String): Boolean = internalName in classesByName

    fun directSuperclassName(internalName: String): String? =
        classesByName[internalName]?.superclassName

    fun isInterface(internalName: String): Boolean =
        classesByName[internalName]?.isInterface == true

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

        val sourceClass = classesByName[sourceClassName]
            ?: return standardJavaLangSuperclassName(sourceClassName)
                ?.let { superclassName -> isAssignable(superclassName, targetClassName) }
                ?: false
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
        if (ownerClass.isInterface) {
            throw JvmIncompatibleClassChangeError(
                guestClassName = "java/lang/IncompatibleClassChangeError",
                message = "$ownerClassName.$name:$descriptor",
            )
        }
        ownerClass.findDeclaredMethod(name, descriptor)?.let { method -> return method }
        ownerClass.findSignaturePolymorphicDeclaration(name, descriptor)?.let { method -> return method }
        if (name != "<init>") {
            findSuperclassMethodForMethodResolution(ownerClass.superclassName, name, descriptor)?.let { method ->
                return method
            }
            selectMaximallySpecificInterfaceMethodOrNull(ownerClass.interfaceNames, name, descriptor)?.let { method ->
                return method
            }
            collectInterfaceMethods(ownerClass.interfaceNames, name, descriptor).firstOrNull()?.let { method ->
                return method
            }
        }
        throw JvmNoSuchMethodError(
            guestClassName = "java/lang/NoSuchMethodError",
            message = "$ownerClassName.$name:$descriptor",
        )
    }

    fun resolveInterfaceMethod(
        ownerClassName: String,
        name: String,
        descriptor: String,
    ): JvmResolvedMethod {
        val ownerClass = classesByName[ownerClassName]
            ?: throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = ownerClassName,
            )
        if (!ownerClass.isInterface) {
            throw JvmIncompatibleClassChangeError(
                guestClassName = "java/lang/IncompatibleClassChangeError",
                message = "$ownerClassName.$name:$descriptor",
            )
        }
        ownerClass.findDeclaredMethod(name, descriptor)?.let { method -> return method }
        findPublicObjectMethod(name, descriptor)?.let { method -> return method }
        selectMaximallySpecificInterfaceMethodOrNull(ownerClass.interfaceNames, name, descriptor)?.let { method ->
            return method
        }
        collectInterfaceMethods(ownerClass.interfaceNames, name, descriptor).firstOrNull()?.let { method -> return method }
        throw JvmNoSuchMethodError(
            guestClassName = "java/lang/NoSuchMethodError",
            message = "$ownerClassName.$name:$descriptor",
        )
    }

    fun resolveVirtualMethod(
        receiverClassName: String,
        name: String,
        descriptor: String,
    ): JvmResolvedMethod {
        val receiverClass = classesByName[receiverClassName]
            ?: throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = receiverClassName,
            )
        return receiverClass.findDeclaredMethod(name, descriptor)
            ?: receiverClass.findSignaturePolymorphicDeclaration(name, descriptor)
            ?: findSuperclassMethod(receiverClass.superclassName, name, descriptor)
            ?: throw JvmNoSuchMethodError(
                guestClassName = "java/lang/NoSuchMethodError",
                message = "$receiverClassName.$name:$descriptor",
            )
    }

    fun resolveInterfaceMethodTarget(
        receiverClassName: String,
        name: String,
        descriptor: String,
    ): JvmResolvedMethod {
        val receiverClass = classesByName[receiverClassName]
            ?: throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = receiverClassName,
            )
        return receiverClass.findDeclaredMethod(name, descriptor)
            ?: receiverClass.findSignaturePolymorphicDeclaration(name, descriptor)
            ?: findSuperclassMethod(receiverClass.superclassName, name, descriptor)
            ?: selectMaximallySpecificInterfaceMethod(receiverClassName, receiverClass.interfaceNames, name, descriptor)
    }

    fun classInitializationMethod(ownerClassName: String): JvmResolvedMethod? {
        val ownerClass = classesByName[ownerClassName]
            ?: throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = ownerClassName,
            )
        return ownerClass.findDeclaredMethod(name = "<clinit>", descriptor = "()V")
            ?.takeIf { method -> method.isStatic }
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
                    isAbstract = method.isAbstract,
                    isNative = method.isNative,
                    isVarargs = method.isVarargs,
                    code = method.code,
                    constantPool = method.constantPool,
                    maxStack = method.maxStack,
                    maxLocals = method.maxLocals,
                    exceptionHandlers = method.exceptionHandlers,
                )
            }

    private fun JvmClassDefinition.findSignaturePolymorphicDeclaration(
        name: String,
        callSiteDescriptor: String,
    ): JvmResolvedMethod? {
        if (internalName != "java/lang/invoke/MethodHandle" || name !in setOf("invoke", "invokeExact")) {
            return null
        }
        return findDeclaredMethod(name, "([Ljava/lang/Object;)Ljava/lang/Object;")
            ?.takeIf { method -> method.isSignaturePolymorphic }
            ?.copy(
                descriptor = callSiteDescriptor,
                signaturePolymorphicDeclarationDescriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
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

    private fun selectMaximallySpecificInterfaceMethod(
        receiverClassName: String,
        interfaceNames: List<String>,
        name: String,
        descriptor: String,
    ): JvmResolvedMethod {
        val candidates = collectInterfaceMethods(interfaceNames, name, descriptor)
        val maximallySpecificMethods = candidates.filter { candidate ->
            candidates.none { other ->
                other.ownerClassName != candidate.ownerClassName &&
                    isAssignable(other.ownerClassName, candidate.ownerClassName)
            }
        }
        val concreteMethods = maximallySpecificMethods.filterNot { method -> method.isAbstract }
        return when (concreteMethods.size) {
            1 -> concreteMethods.single()
            0 -> throw JvmAbstractMethodError(
                guestClassName = "java/lang/AbstractMethodError",
                message = "$receiverClassName.$name:$descriptor",
            )
            else -> throw JvmIncompatibleClassChangeError(
                guestClassName = "java/lang/IncompatibleClassChangeError",
                message = "$receiverClassName.$name:$descriptor",
            )
        }
    }

    private fun selectMaximallySpecificInterfaceMethodOrNull(
        interfaceNames: List<String>,
        name: String,
        descriptor: String,
    ): JvmResolvedMethod? {
        val candidates = collectInterfaceMethods(interfaceNames, name, descriptor)
        val maximallySpecificMethods = candidates.filter { candidate ->
            candidates.none { other ->
                other.ownerClassName != candidate.ownerClassName &&
                    isAssignable(other.ownerClassName, candidate.ownerClassName)
            }
        }
        return maximallySpecificMethods.singleOrNull { method -> !method.isAbstract }
    }

    private fun collectInterfaceMethods(
        interfaceNames: List<String>,
        name: String,
        descriptor: String,
        visited: MutableSet<String> = linkedSetOf(),
    ): List<JvmResolvedMethod> {
        val methods = mutableListOf<JvmResolvedMethod>()
        for (interfaceName in interfaceNames) {
            if (!visited.add(interfaceName)) {
                continue
            }
            val interfaceClass = classesByName[interfaceName] ?: continue
            interfaceClass.findDeclaredMethod(name, descriptor)
                ?.takeIf { method -> !method.isPrivate && !method.isStatic }
                ?.let { resolved -> methods += resolved }
            methods += collectInterfaceMethods(interfaceClass.interfaceNames, name, descriptor, visited)
        }
        return methods
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

    private fun findSuperclassMethodForMethodResolution(
        superclassName: String?,
        name: String,
        descriptor: String,
    ): JvmResolvedMethod? {
        if (superclassName == null) {
            return null
        }
        val superclass = classesByName[superclassName] ?: return null
        return superclass.findDeclaredMethod(name, descriptor)
            ?: superclass.findSignaturePolymorphicDeclaration(name, descriptor)
            ?: findSuperclassMethodForMethodResolution(superclass.superclassName, name, descriptor)
            ?: selectMaximallySpecificInterfaceMethodOrNull(superclass.interfaceNames, name, descriptor)
            ?: collectInterfaceMethods(superclass.interfaceNames, name, descriptor).firstOrNull()
    }

    private fun findPublicObjectMethod(name: String, descriptor: String): JvmResolvedMethod? =
        classesByName["java/lang/Object"]
            ?.findDeclaredMethod(name, descriptor)
            ?.takeIf { method -> !method.isStatic && !method.isPrivate && !method.isPackagePrivate && !method.isProtected }

    private fun String.isArrayClassName(): Boolean = startsWith("[")

    private fun String.isReferenceArrayClassName(): Boolean =
        startsWith("[L") && endsWith(";") || startsWith("[[")

    private fun String.referenceArrayComponentClassName(): String =
        if (startsWith("[L") && endsWith(";")) {
            substring(2, length - 1)
        } else {
            substring(1)
        }

    private fun standardJavaLangSuperclassName(className: String): String? =
        standardJavaLangSuperclasses[className]

    companion object {
        private val standardJavaLangSuperclasses = mapOf(
            "java/lang/Throwable" to "java/lang/Object",
            "java/lang/Exception" to "java/lang/Throwable",
            "java/lang/RuntimeException" to "java/lang/Exception",
            "java/lang/Error" to "java/lang/Throwable",
            "java/lang/LinkageError" to "java/lang/Error",
            "java/lang/IllegalArgumentException" to "java/lang/RuntimeException",
            "java/lang/IllegalStateException" to "java/lang/RuntimeException",
            "java/lang/IndexOutOfBoundsException" to "java/lang/RuntimeException",
            "java/lang/ArithmeticException" to "java/lang/RuntimeException",
            "java/lang/ArrayIndexOutOfBoundsException" to "java/lang/IndexOutOfBoundsException",
            "java/lang/ArrayStoreException" to "java/lang/RuntimeException",
            "java/lang/ClassCastException" to "java/lang/RuntimeException",
            "java/lang/IllegalMonitorStateException" to "java/lang/RuntimeException",
            "java/lang/NegativeArraySizeException" to "java/lang/RuntimeException",
            "java/lang/NullPointerException" to "java/lang/RuntimeException",
            "java/lang/AbstractMethodError" to "java/lang/IncompatibleClassChangeError",
            "java/lang/IllegalAccessError" to "java/lang/IncompatibleClassChangeError",
            "java/lang/IncompatibleClassChangeError" to "java/lang/LinkageError",
            "java/lang/NoClassDefFoundError" to "java/lang/LinkageError",
            "java/lang/NoSuchFieldError" to "java/lang/IncompatibleClassChangeError",
            "java/lang/NoSuchMethodError" to "java/lang/IncompatibleClassChangeError",
            "java/lang/UnsatisfiedLinkError" to "java/lang/LinkageError",
        )

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

class JvmIncompatibleClassChangeError(
    val guestClassName: String,
    message: String,
) : IncompatibleClassChangeError(message)

class JvmAbstractMethodError(
    val guestClassName: String,
    message: String,
) : AbstractMethodError(message)
