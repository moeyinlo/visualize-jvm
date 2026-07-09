package me.moeyinlo.visualize.jvm.classfile

class RawAttributeInfo(
    override val nameIndex: ConstantPoolIndex,
    info: ByteArray,
) : AttributeInfo {
    private val infoBytes: ByteArray = info.copyOf()

    val info: ByteArray
        get() = infoBytes.copyOf()
}

object RawAttributeInfoParser {
    fun parseAttributes(reader: ClassFileByteReader, ownerPath: String): List<RawAttributeInfo> {
        val attributesCount = reader.readU2()
        return List(attributesCount) { index ->
            parseAttribute(reader, "$ownerPath.attributes[$index]")
        }
    }

    private fun parseAttribute(reader: ClassFileByteReader, ownerPath: String): RawAttributeInfo {
        val nameIndex = readNonZeroConstantPoolIndex(reader, "$ownerPath.attribute_name_index")
        val attributeLength = reader.readU4()
        if (attributeLength > Int.MAX_VALUE) {
            throw ClassFileFormatException(
                "Attribute too large source=${reader.source} $ownerPath.attribute_length=$attributeLength",
            )
        }
        return RawAttributeInfo(
            nameIndex = nameIndex,
            info = reader.readSlice(attributeLength.toInt()),
        )
    }

    internal fun readNonZeroConstantPoolIndex(
        reader: ClassFileByteReader,
        role: String,
    ): ConstantPoolIndex {
        val offset = reader.position
        val rawIndex = reader.readU2()
        if (rawIndex == 0) {
            throw ClassFileFormatException("Invalid $role source=${reader.source} offset=$offset: zero is not allowed")
        }
        return ConstantPoolIndex(rawIndex)
    }
}
