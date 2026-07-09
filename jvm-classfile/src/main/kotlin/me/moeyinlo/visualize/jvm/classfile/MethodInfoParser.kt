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
    ): List<MethodInfo> =
        parseMethods(reader) { attributeReader, ownerPath ->
            AttributeInfoParser.parseAttributes(
                reader = attributeReader,
                constantPool = constantPool,
                registry = attributeParsers,
                ownerPath = ownerPath,
            )
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
}
