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
        attributes.forEachIndexed { index, attribute ->
            when (attributeName(attribute, constantPool, source, index)) {
                "NestHost" -> nestHostPaths += "ClassFile.attributes[$index]"
                "NestMembers" -> nestMembersPaths += "ClassFile.attributes[$index]"
                "PermittedSubclasses" -> permittedSubclassesPaths += "ClassFile.attributes[$index]"
                "SourceFile" -> sourceFilePaths += "ClassFile.attributes[$index]"
                "SourceDebugExtension" -> sourceDebugExtensionPaths += "ClassFile.attributes[$index]"
            }
        }
        if (nestHostPaths.size > 1) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: at most one NestHost attribute is permitted " +
                    "but found ${nestHostPaths.size} at ${nestHostPaths.joinToString()}",
            )
        }
        if (nestMembersPaths.size > 1) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: at most one NestMembers attribute is permitted " +
                    "but found ${nestMembersPaths.size} at ${nestMembersPaths.joinToString()}",
            )
        }
        if (permittedSubclassesPaths.size > 1) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: at most one PermittedSubclasses attribute is permitted " +
                    "but found ${permittedSubclassesPaths.size} at ${permittedSubclassesPaths.joinToString()}",
            )
        }
        val permittedSubclassesPath = permittedSubclassesPaths.singleOrNull()
        if (permittedSubclassesPath != null && accessFlags.has(ClassAccessFlag.Final)) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: ACC_FINAL class must not declare " +
                    "PermittedSubclasses ($permittedSubclassesPath)",
            )
        }
        if (sourceFilePaths.size > 1) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: at most one SourceFile attribute is permitted " +
                    "but found ${sourceFilePaths.size} at ${sourceFilePaths.joinToString()}",
            )
        }
        if (sourceDebugExtensionPaths.size > 1) {
            throw ClassFileFormatException(
                "Invalid ClassFile attributes source=$source: at most one SourceDebugExtension attribute is permitted " +
                    "but found ${sourceDebugExtensionPaths.size} at ${sourceDebugExtensionPaths.joinToString()}",
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
