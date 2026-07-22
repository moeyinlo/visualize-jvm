package me.moeyinlo.visualize.jvm.classfile

data class MethodInfo(
    val accessFlags: Int,
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
    val attributes: List<AttributeInfo>,
)

object MethodInfoParser {
    fun parseMethods(reader: ClassFileByteReader): List<MethodInfo> =
        parseMethods(reader) { attributeReader, ownerPath ->
            RawAttributeInfoParser.parseAttributes(attributeReader, ownerPath)
        }

    fun parseMethods(
        reader: ClassFileByteReader,
        constantPool: ConstantPool,
        attributeParsers: AttributeParserRegistry,
        classKind: ClassFileKind = ClassFileKind.Class,
        majorVersion: Int = 70,
    ): List<MethodInfo> {
        val methods = parseMethods(reader) { attributeReader, ownerPath ->
            AttributeInfoParser.parseAttributes(
                reader = attributeReader,
                constantPool = constantPool,
                registry = attributeParsers,
                ownerPath = ownerPath,
                majorVersion = majorVersion,
            )
        }
        validateMethods(methods, constantPool, classKind, majorVersion)
        return methods
    }

    private fun parseMethods(
        reader: ClassFileByteReader,
        parseAttributes: (reader: ClassFileByteReader, ownerPath: String) -> List<AttributeInfo>,
    ): List<MethodInfo> {
        val methodsCount = reader.readU2()
        return List(methodsCount) { index ->
            parseMethod(reader, index, parseAttributes)
        }
    }

    private fun parseMethod(
        reader: ClassFileByteReader,
        index: Int,
        parseAttributes: (reader: ClassFileByteReader, ownerPath: String) -> List<AttributeInfo>,
    ): MethodInfo {
        val ownerPath = "methods[$index]"
        return MethodInfo(
            accessFlags = reader.readU2(),
            nameIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.name_index"),
            descriptorIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.descriptor_index"),
            attributes = parseAttributes(reader, ownerPath),
        )
    }

    private fun validateMethods(
        methods: List<MethodInfo>,
        constantPool: ConstantPool,
        classKind: ClassFileKind,
        majorVersion: Int,
    ) {
        val seenMethods = mutableMapOf<Pair<String, String>, Int>()
        methods.forEachIndexed { index, method ->
            val ownerPath = "methods[$index]"
            val name = expectUtf8(constantPool, method.nameIndex, "$ownerPath.name_index")
            val descriptor = expectUtf8(constantPool, method.descriptorIndex, "$ownerPath.descriptor_index")

            ClassNameValidator.validateMethodName(
                owner = method.nameIndex,
                role = "$ownerPath.name_index",
                value = name.value,
                allowInit = classKind != ClassFileKind.Interface && classKind != ClassFileKind.AnnotationInterface,
                allowClinit = true,
            )
            DescriptorValidator.validateMethodDescriptor(method.descriptorIndex, "$ownerPath.descriptor_index", descriptor.value)
            validateSpecialMethodDescriptor(name.value, descriptor.value, method.descriptorIndex, ownerPath, majorVersion)
            validateAccessFlags(method.accessFlags, name.value, classKind, ownerPath, majorVersion)
            validateCodeAttributeCardinality(method, constantPool, name.value, ownerPath)
            validateMethodAttributeCardinality(method, constantPool, classKind, descriptor.value, ownerPath)

            val duplicateOf = seenMethods.putIfAbsent(name.value to descriptor.value, index)
            if (duplicateOf != null) {
                throw ClassFileFormatException(
                    "Duplicate method_info at $ownerPath: " +
                        "name='${name.value}' descriptor='${descriptor.value}' already used by methods[$duplicateOf]",
                )
            }
        }
    }

    private fun validateCodeAttributeCardinality(
        method: MethodInfo,
        constantPool: ConstantPool,
        methodName: String,
        ownerPath: String,
    ) {
        val codeAttributeCount = method.attributes.countIndexed { attributeIndex, attribute ->
            val name = expectUtf8(
                constantPool = constantPool,
                index = attribute.nameIndex,
                role = "$ownerPath.attributes[$attributeIndex].attribute_name_index",
            )
            name.value == "Code"
        }
        val isClassOrInterfaceInitializationMethod = methodName == "<clinit>"
        val mayNotHaveCode = !isClassOrInterfaceInitializationMethod &&
            (has(method.accessFlags, MethodAccessFlag.Native) || has(method.accessFlags, MethodAccessFlag.Abstract))

        if (mayNotHaveCode) {
            if (codeAttributeCount != 0) {
                val reason = when {
                    has(method.accessFlags, MethodAccessFlag.Native) -> "ACC_NATIVE"
                    else -> "ACC_ABSTRACT"
                }
                throw ClassFileFormatException(
                    "Invalid $ownerPath.attributes: $reason method '$methodName' must not have a Code attribute",
                )
            }
            return
        }

        if (codeAttributeCount != 1) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.attributes: method '$methodName' must have exactly one Code attribute " +
                    "but found $codeAttributeCount",
            )
        }
    }

    private fun validateMethodAttributeCardinality(
        method: MethodInfo,
        constantPool: ConstantPool,
        classKind: ClassFileKind,
        descriptor: String,
        ownerPath: String,
    ) {
        val exceptionsPaths = mutableListOf<String>()
        val signaturePaths = mutableListOf<String>()
        val runtimeVisibleAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleAnnotationsPaths = mutableListOf<String>()
        val runtimeVisibleTypeAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleTypeAnnotationsPaths = mutableListOf<String>()
        val runtimeVisibleParameterAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleParameterAnnotationsPaths = mutableListOf<String>()
        val methodParametersPaths = mutableListOf<String>()
        val annotationDefaultPaths = mutableListOf<String>()
        method.attributes.forEachIndexed { attributeIndex, attribute ->
            val name = expectUtf8(
                constantPool = constantPool,
                index = attribute.nameIndex,
                role = "$ownerPath.attributes[$attributeIndex].attribute_name_index",
            )
            when (name.value) {
                "Exceptions" -> exceptionsPaths += "$ownerPath.attributes[$attributeIndex]"
                "Signature" -> signaturePaths += "$ownerPath.attributes[$attributeIndex]"
                "RuntimeVisibleAnnotations" -> runtimeVisibleAnnotationsPaths += "$ownerPath.attributes[$attributeIndex]"
                "RuntimeInvisibleAnnotations" -> runtimeInvisibleAnnotationsPaths += "$ownerPath.attributes[$attributeIndex]"
                "RuntimeVisibleTypeAnnotations" -> runtimeVisibleTypeAnnotationsPaths += "$ownerPath.attributes[$attributeIndex]"
                "RuntimeInvisibleTypeAnnotations" -> runtimeInvisibleTypeAnnotationsPaths += "$ownerPath.attributes[$attributeIndex]"
                "RuntimeVisibleParameterAnnotations" -> runtimeVisibleParameterAnnotationsPaths += "$ownerPath.attributes[$attributeIndex]"
                "RuntimeInvisibleParameterAnnotations" -> runtimeInvisibleParameterAnnotationsPaths += "$ownerPath.attributes[$attributeIndex]"
                "MethodParameters" -> methodParametersPaths += "$ownerPath.attributes[$attributeIndex]"
                "AnnotationDefault" -> annotationDefaultPaths += "$ownerPath.attributes[$attributeIndex]"
            }
        }
        requireAtMostOneAttribute(exceptionsPaths, "Exceptions", ownerPath)
        requireAtMostOneAttribute(signaturePaths, "Signature", ownerPath)
        requireAtMostOneAttribute(runtimeVisibleAnnotationsPaths, "RuntimeVisibleAnnotations", ownerPath)
        requireAtMostOneAttribute(runtimeInvisibleAnnotationsPaths, "RuntimeInvisibleAnnotations", ownerPath)
        requireAtMostOneAttribute(runtimeVisibleTypeAnnotationsPaths, "RuntimeVisibleTypeAnnotations", ownerPath)
        requireAtMostOneAttribute(runtimeInvisibleTypeAnnotationsPaths, "RuntimeInvisibleTypeAnnotations", ownerPath)
        requireAtMostOneAttribute(
            runtimeVisibleParameterAnnotationsPaths,
            "RuntimeVisibleParameterAnnotations",
            ownerPath,
        )
        requireAtMostOneAttribute(
            runtimeInvisibleParameterAnnotationsPaths,
            "RuntimeInvisibleParameterAnnotations",
            ownerPath,
        )
        requireAtMostOneAttribute(methodParametersPaths, "MethodParameters", ownerPath)
        requireAtMostOneAttribute(annotationDefaultPaths, "AnnotationDefault", ownerPath)
        val annotationDefaultPath = annotationDefaultPaths.singleOrNull()
        if (annotationDefaultPath != null && classKind != ClassFileKind.AnnotationInterface) {
            throw ClassFileFormatException(
                "Invalid $ownerPath attributes: AnnotationDefault is permitted only on an annotation " +
                    "interface element but found $annotationDefaultPath",
            )
        }
        if (
            annotationDefaultPath != null &&
            DescriptorValidator.methodParameterUnits(method.descriptorIndex, "$ownerPath.descriptor_index", descriptor) != 0
        ) {
            throw ClassFileFormatException(
                "Invalid $ownerPath attributes: AnnotationDefault annotation interface elements " +
                    "must not declare parameters but found $annotationDefaultPath",
            )
        }
        if (annotationDefaultPath != null && descriptor.endsWith("V")) {
            throw ClassFileFormatException(
                "Invalid $ownerPath attributes: AnnotationDefault annotation interface elements " +
                    "must not return void but found $annotationDefaultPath",
            )
        }
    }

    private fun requireAtMostOneAttribute(
        paths: List<String>,
        attributeName: String,
        ownerPath: String,
    ) {
        if (paths.size <= 1) {
            return
        }
        throw ClassFileFormatException(
            "Invalid $ownerPath attributes: at most one $attributeName attribute is permitted " +
                "but found ${paths.size} at ${paths.joinToString()}",
        )
    }

    private fun validateSpecialMethodDescriptor(
        name: String,
        descriptor: String,
        descriptorIndex: ConstantPoolIndex,
        ownerPath: String,
        majorVersion: Int,
    ) {
        if (name == "<init>" && !descriptor.endsWith("V")) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.descriptor_index=$descriptorIndex: method name <init> must have a void method descriptor",
            )
        }
        if (name == "<clinit>") {
            val validDescriptor = if (majorVersion >= 51) {
                descriptor == "()V"
            } else {
                descriptor.endsWith("V")
            }
            if (!validDescriptor) {
                throw ClassFileFormatException(
                    "Invalid $ownerPath.descriptor_index=$descriptorIndex: " +
                        "method name <clinit> must have descriptor ()V in class file major version $majorVersion",
                )
            }
        }
    }

    private fun validateAccessFlags(
        accessFlags: Int,
        methodName: String,
        classKind: ClassFileKind,
        ownerPath: String,
        majorVersion: Int,
    ) {
        if (methodName == "<clinit>") {
            if (majorVersion >= 51 && !has(accessFlags, MethodAccessFlag.Static)) {
                failAccess(ownerPath, "method <clinit> must set ACC_STATIC")
            }
            return
        }

        if (methodName == "<init>") {
            validateVisibility(accessFlags, ownerPath)
            val allowed = MethodAccessFlag.Public.mask or
                MethodAccessFlag.Private.mask or
                MethodAccessFlag.Protected.mask or
                MethodAccessFlag.Varargs.mask or
                MethodAccessFlag.Synthetic.mask or
                strictMaskWhenInterpreted(majorVersion)
            val disallowed = accessFlags and assignedMaskWhenInterpreted(majorVersion) and allowed.inv()
            if (disallowed != 0) {
                failAccess(ownerPath, "instance initialization methods must not set ${MethodAccessFlag.namesFor(disallowed)}")
            }
            return
        }

        validateVisibility(accessFlags, ownerPath)

        if (classKind == ClassFileKind.Interface || classKind == ClassFileKind.AnnotationInterface) {
            val disallowed = accessFlags and (
                MethodAccessFlag.Protected.mask or
                    MethodAccessFlag.Final.mask or
                    MethodAccessFlag.Synchronized.mask or
                    MethodAccessFlag.Native.mask
                )
            if (disallowed != 0) {
                failAccess(ownerPath, "interface methods must not set ${MethodAccessFlag.namesFor(disallowed)}")
            }
            if (majorVersion >= 52) {
                val publicOrPrivate = listOf(MethodAccessFlag.Public, MethodAccessFlag.Private)
                    .count { has(accessFlags, it) }
                if (publicOrPrivate != 1) {
                    failAccess(ownerPath, "modern interface methods must set exactly one of ACC_PUBLIC and ACC_PRIVATE")
                }
            } else if (!has(accessFlags, MethodAccessFlag.Public) || !has(accessFlags, MethodAccessFlag.Abstract)) {
                failAccess(ownerPath, "legacy interface methods must set ACC_PUBLIC and ACC_ABSTRACT")
            }
        }

        if (has(accessFlags, MethodAccessFlag.Abstract)) {
            val disallowed = accessFlags and (
                MethodAccessFlag.Private.mask or
                    MethodAccessFlag.Static.mask or
                    MethodAccessFlag.Final.mask or
                    MethodAccessFlag.Synchronized.mask or
                    MethodAccessFlag.Native.mask or
                    strictMaskWhenInterpreted(majorVersion)
                )
            if (disallowed != 0) {
                failAccess(ownerPath, "ACC_ABSTRACT methods must not set ${MethodAccessFlag.namesFor(disallowed)}")
            }
        }
    }

    private fun validateVisibility(
        accessFlags: Int,
        ownerPath: String,
    ) {
        val visibilityCount = listOf(MethodAccessFlag.Public, MethodAccessFlag.Private, MethodAccessFlag.Protected)
            .count { has(accessFlags, it) }
        if (visibilityCount > 1) {
            failAccess(ownerPath, "must not set more than one of ACC_PUBLIC, ACC_PRIVATE, and ACC_PROTECTED")
        }
    }

    private fun strictMaskWhenInterpreted(majorVersion: Int): Int =
        if (majorVersion in 46..60) MethodAccessFlag.Strict.mask else 0

    private fun assignedMaskWhenInterpreted(majorVersion: Int): Int =
        if (majorVersion in 46..60) {
            MethodAccessFlag.assignedMask
        } else {
            MethodAccessFlag.assignedMask and MethodAccessFlag.Strict.mask.inv()
        }

    private fun expectUtf8(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        role: String,
    ): ConstantUtf8Entry {
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return entry
    }

    private inline fun <T> Iterable<T>.countIndexed(predicate: (Int, T) -> Boolean): Int {
        var count = 0
        forEachIndexed { index, value ->
            if (predicate(index, value)) {
                count += 1
            }
        }
        return count
    }

    private fun has(accessFlags: Int, flag: MethodAccessFlag): Boolean =
        accessFlags and flag.mask != 0

    private fun failAccess(
        ownerPath: String,
        reason: String,
    ): Nothing =
        throw ClassFileFormatException("Invalid $ownerPath.access_flags: $reason")

    private enum class MethodAccessFlag(
        val mask: Int,
        val specName: String,
    ) {
        Public(0x0001, "ACC_PUBLIC"),
        Private(0x0002, "ACC_PRIVATE"),
        Protected(0x0004, "ACC_PROTECTED"),
        Static(0x0008, "ACC_STATIC"),
        Final(0x0010, "ACC_FINAL"),
        Synchronized(0x0020, "ACC_SYNCHRONIZED"),
        Bridge(0x0040, "ACC_BRIDGE"),
        Varargs(0x0080, "ACC_VARARGS"),
        Native(0x0100, "ACC_NATIVE"),
        Abstract(0x0400, "ACC_ABSTRACT"),
        Strict(0x0800, "ACC_STRICT"),
        Synthetic(0x1000, "ACC_SYNTHETIC"),
        ;

        companion object {
            val assignedMask: Int = entries.fold(0) { mask, flag -> mask or flag.mask }

            fun namesFor(raw: Int): String =
                entries
                    .filter { raw and it.mask != 0 }
                    .joinToString(", ") { it.specName }
        }
    }
}
