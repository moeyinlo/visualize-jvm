package me.moeyinlo.visualize.jvm.classfile

data class FieldInfo(
    val accessFlags: Int,
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
    val attributes: List<AttributeInfo>,
)

object FieldInfoParser {
    fun parseFields(reader: ClassFileByteReader): List<FieldInfo> =
        parseFields(reader) { attributeReader, ownerPath ->
            RawAttributeInfoParser.parseAttributes(attributeReader, ownerPath)
        }

    fun parseFields(
        reader: ClassFileByteReader,
        constantPool: ConstantPool,
        attributeParsers: AttributeParserRegistry,
    ): List<FieldInfo> =
        parseFields(reader) { attributeReader, ownerPath ->
            AttributeInfoParser.parseAttributes(
                reader = attributeReader,
                constantPool = constantPool,
                registry = attributeParsers,
                ownerPath = ownerPath,
            )
        }

    private fun parseFields(
        reader: ClassFileByteReader,
        parseAttributes: (reader: ClassFileByteReader, ownerPath: String) -> List<AttributeInfo>,
    ): List<FieldInfo> {
        val fieldsCount = reader.readU2()
        return List(fieldsCount) { index ->
            parseField(reader, index, parseAttributes)
        }
    }

    private fun parseField(
        reader: ClassFileByteReader,
        index: Int,
        parseAttributes: (reader: ClassFileByteReader, ownerPath: String) -> List<AttributeInfo>,
    ): FieldInfo {
        val ownerPath = "fields[$index]"
        return FieldInfo(
            accessFlags = reader.readU2(),
            nameIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.name_index"),
            descriptorIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.descriptor_index"),
            attributes = parseAttributes(reader, ownerPath),
        )
    }
}
