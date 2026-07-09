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
        val fields = FieldInfoParser.parseFields(reader, constantPool, attributeParsers, accessFlags.kind)
        val methods = MethodInfoParser.parseMethods(reader, constantPool, attributeParsers, accessFlags.kind, version.major)
        val attributes = AttributeInfoParser.parseAttributes(
            reader = reader,
            constantPool = constantPool,
            registry = attributeParsers,
            ownerPath = "ClassFile",
        )
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
}
