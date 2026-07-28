package me.moeyinlo.visualize.jvm.classfile

data class ClassFile(
    val magic: ClassFileMagic,
    val version: ClassFileVersion,
    val constantPool: ConstantPool,
    val accessFlags: ClassAccessFlags,
    val identity: ClassIdentity,
    val fields: List<FieldInfo>,
    val methods: List<MethodInfo>,
    val attributes: List<AttributeInfo>,
)

object ClassFileParser {
    fun parse(
        bytes: ByteArray,
        source: String = "<memory>",
        attributeParsers: AttributeParserRegistry = AttributeParserRegistry.Empty,
    ): ClassFile =
        parse(
            reader = ClassFileByteReader(bytes, source = source),
            attributeParsers = attributeParsers,
        )

    fun parse(
        reader: ClassFileByteReader,
        attributeParsers: AttributeParserRegistry = AttributeParserRegistry.Empty,
    ): ClassFile {
        val magic = ClassFileHeaderParser.parseMagic(reader)
        val version = ClassFileHeaderParser.validateJava26Version(ClassFileHeaderParser.parseVersion(reader))
        val constantPool = ConstantPoolParser.parse(reader)
        val accessFlags = ClassAccessFlagsParser.parse(reader)
        val identity = ClassIdentityParser.parse(reader)
        validateClassIdentity(identity, constantPool, reader.source)
        val fields = FieldInfoParser.parseFields(reader, constantPool, attributeParsers, accessFlags.kind, version.major)
        val methods = MethodInfoParser.parseMethods(reader, constantPool, attributeParsers, accessFlags.kind, version.major)
        val attributes = AttributeInfoParser.parseAttributes(
            reader = reader,
            constantPool = constantPool,
            registry = attributeParsers,
            ownerPath = "ClassFile",
            majorVersion = version.major,
        )
        validateClassAttributes(attributes, constantPool, accessFlags, reader.source)
        if (reader.remaining != 0) {
            throw ClassFileFormatException(
                "Trailing bytes after ClassFile source=${reader.source} offset=${reader.currentOffset} " +
                    "remaining=${reader.remaining}",
            )
        }
        return ClassFile(
            magic = magic,
            version = version,
            constantPool = constantPool,
            accessFlags = accessFlags,
            identity = identity,
            fields = fields,
            methods = methods,
            attributes = attributes,
        )
    }

    private fun validateClassIdentity(
        identity: ClassIdentity,
        constantPool: ConstantPool,
        source: String,
    ) {
        expectClassIdentityReference(
            constantPool = constantPool,
            index = identity.thisClassIndex,
            role = "this_class",
            source = source,
        )
        identity.superClassIndex?.let { superClassIndex ->
            expectClassIdentityReference(
                constantPool = constantPool,
                index = superClassIndex,
                role = "super_class",
                source = source,
            )
        }
        identity.interfaceIndexes.forEachIndexed { interfaceIndex, constantPoolIndex ->
            expectClassIdentityReference(
                constantPool = constantPool,
                index = constantPoolIndex,
                role = "interfaces[$interfaceIndex]",
                source = source,
            )
        }
    }

    private fun expectClassIdentityReference(
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        role: String,
        source: String,
    ): ConstantClassEntry {
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid ClassFile $role=$index source=$source: ${exception.message}",
            )
        }
        if (entry !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid ClassFile $role=$index source=$source: expected CONSTANT_Class_info " +
                    "but found ${entry.javaClass.simpleName}",
            )
        }
        return entry
    }

    private fun validateClassAttributes(
        attributes: List<AttributeInfo>,
        constantPool: ConstantPool,
        accessFlags: ClassAccessFlags,
        source: String,
    ) {
        val nestHostPaths = mutableListOf<String>()
        val nestMembersPaths = mutableListOf<String>()
        val permittedSubclassesPaths = mutableListOf<String>()
        val sourceFilePaths = mutableListOf<String>()
        val sourceDebugExtensionPaths = mutableListOf<String>()
        val innerClassesPaths = mutableListOf<String>()
        val enclosingMethodPaths = mutableListOf<String>()
        val recordPaths = mutableListOf<String>()
        val modulePaths = mutableListOf<String>()
        val moduleMainClassPaths = mutableListOf<String>()
        val signaturePaths = mutableListOf<String>()
        val runtimeVisibleAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleAnnotationsPaths = mutableListOf<String>()
        val runtimeVisibleTypeAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleTypeAnnotationsPaths = mutableListOf<String>()
        val modulePackagesPaths = mutableListOf<String>()
        val bootstrapMethodsPaths = mutableListOf<String>()
        val codePaths = mutableListOf<String>()
        val constantValuePaths = mutableListOf<String>()
        val methodParametersPaths = mutableListOf<String>()
        attributes.forEachIndexed { index, attribute ->
            when (attributeName(attribute, constantPool, source, index)) {
                "NestHost" -> nestHostPaths += "ClassFile.attributes[$index]"
                "NestMembers" -> nestMembersPaths += "ClassFile.attributes[$index]"
                "PermittedSubclasses" -> permittedSubclassesPaths += "ClassFile.attributes[$index]"
                "SourceFile" -> sourceFilePaths += "ClassFile.attributes[$index]"
                "SourceDebugExtension" -> sourceDebugExtensionPaths += "ClassFile.attributes[$index]"
                "InnerClasses" -> innerClassesPaths += "ClassFile.attributes[$index]"
                "EnclosingMethod" -> enclosingMethodPaths += "ClassFile.attributes[$index]"
                "Record" -> recordPaths += "ClassFile.attributes[$index]"
                "Module" -> modulePaths += "ClassFile.attributes[$index]"
                "ModuleMainClass" -> moduleMainClassPaths += "ClassFile.attributes[$index]"
                "Signature" -> signaturePaths += "ClassFile.attributes[$index]"
                "RuntimeVisibleAnnotations" -> runtimeVisibleAnnotationsPaths += "ClassFile.attributes[$index]"
                "RuntimeInvisibleAnnotations" -> runtimeInvisibleAnnotationsPaths += "ClassFile.attributes[$index]"
                "RuntimeVisibleTypeAnnotations" -> runtimeVisibleTypeAnnotationsPaths += "ClassFile.attributes[$index]"
                "RuntimeInvisibleTypeAnnotations" -> runtimeInvisibleTypeAnnotationsPaths += "ClassFile.attributes[$index]"
                "ModulePackages" -> modulePackagesPaths += "ClassFile.attributes[$index]"
                "BootstrapMethods" -> bootstrapMethodsPaths += "ClassFile.attributes[$index]"
                "Code" -> codePaths += "ClassFile.attributes[$index]"
                "ConstantValue" -> constantValuePaths += "ClassFile.attributes[$index]"
                "MethodParameters" -> methodParametersPaths += "ClassFile.attributes[$index]"
            }
        }
        requireAbsentAttribute(codePaths, "Code", "method_info", source)
        requireAbsentAttribute(constantValuePaths, "ConstantValue", "field_info", source)
        requireAbsentAttribute(methodParametersPaths, "MethodParameters", "method_info", source)
        requireAtMostOneAttribute(nestHostPaths, "NestHost", source)
        requireAtMostOneAttribute(nestMembersPaths, "NestMembers", source)
        requireAtMostOneAttribute(permittedSubclassesPaths, "PermittedSubclasses", source)
        val permittedSubclassesPath = permittedSubclassesPaths.singleOrNull()
        if (permittedSubclassesPath != null && accessFlags.has(ClassAccessFlag.Final)) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: ACC_FINAL class must not declare " +
                    "PermittedSubclasses ($permittedSubclassesPath)",
            )
        }
        requireAtMostOneAttribute(sourceFilePaths, "SourceFile", source)
        requireAtMostOneAttribute(sourceDebugExtensionPaths, "SourceDebugExtension", source)
        requireAtMostOneAttribute(innerClassesPaths, "InnerClasses", source)
        requireAtMostOneAttribute(enclosingMethodPaths, "EnclosingMethod", source)
        requireAtMostOneAttribute(recordPaths, "Record", source)
        requireAtMostOneAttribute(modulePaths, "Module", source)
        requireAtMostOneAttribute(moduleMainClassPaths, "ModuleMainClass", source)
        requireAtMostOneAttribute(signaturePaths, "Signature", source)
        requireAtMostOneAttribute(runtimeVisibleAnnotationsPaths, "RuntimeVisibleAnnotations", source)
        requireAtMostOneAttribute(runtimeInvisibleAnnotationsPaths, "RuntimeInvisibleAnnotations", source)
        requireAtMostOneAttribute(runtimeVisibleTypeAnnotationsPaths, "RuntimeVisibleTypeAnnotations", source)
        requireAtMostOneAttribute(runtimeInvisibleTypeAnnotationsPaths, "RuntimeInvisibleTypeAnnotations", source)
        requireAtMostOneAttribute(modulePackagesPaths, "ModulePackages", source)
        requireAtMostOneAttribute(bootstrapMethodsPaths, "BootstrapMethods", source)
        if (bootstrapMethodsPaths.isEmpty() && requiresBootstrapMethodsAttribute(constantPool)) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: exactly one BootstrapMethods attribute is required " +
                    "when constant_pool contains a CONSTANT_Dynamic_info or CONSTANT_InvokeDynamic_info entry",
            )
        }
        val nestHostPath = nestHostPaths.singleOrNull()
        val nestMembersPath = nestMembersPaths.singleOrNull()
        if (nestHostPath != null && nestMembersPath != null) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: must not contain both " +
                    "NestHost ($nestHostPath) and NestMembers ($nestMembersPath)",
            )
        }
    }

    private fun requiresBootstrapMethodsAttribute(constantPool: ConstantPool): Boolean =
        (1 until constantPool.constantPoolCount).any { rawIndex ->
            when (val slot = constantPool.slotAt(ConstantPoolIndex(rawIndex))) {
                is ConstantPoolSlot.Entry ->
                    slot.value is ConstantDynamicEntry || slot.value is ConstantInvokeDynamicEntry
                ConstantPoolSlot.Unusable -> false
        }
    }

    private fun requireAbsentAttribute(
        paths: List<String>,
        attributeName: String,
        allowedLocation: String,
        source: String,
    ) {
        if (paths.isNotEmpty()) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: $attributeName is permitted only in " +
                    "$allowedLocation attributes but found at ${paths.joinToString()}",
            )
        }
    }

    private fun requireAtMostOneAttribute(
        paths: List<String>,
        attributeName: String,
        source: String,
    ) {
        if (paths.size > 1) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: at most one $attributeName attribute is permitted " +
                    "but found ${paths.size} at ${paths.joinToString()}",
            )
        }
    }

    private fun attributeName(
        attribute: AttributeInfo,
        constantPool: ConstantPool,
        source: String,
        attributeIndex: Int,
    ): String {
        val entry = try {
            constantPool[attribute.nameIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid ClassFile.attributes[$attributeIndex].attribute_name_index=${attribute.nameIndex} " +
                    "source=$source: ${exception.message}",
            )
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid ClassFile.attributes[$attributeIndex].attribute_name_index=${attribute.nameIndex} " +
                    "source=$source: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return entry.value
    }
}
