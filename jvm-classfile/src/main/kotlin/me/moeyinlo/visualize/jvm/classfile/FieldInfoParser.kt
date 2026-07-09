package me.moeyinlo.visualize.jvm.classfile

data class FieldInfo(
    val accessFlags: Int,
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
    val attributes: List<RawAttributeInfo>,
)

object FieldInfoParser {
    fun parseFields(reader: ClassFileByteReader): List<FieldInfo> {
        val fieldsCount = reader.readU2()
        return List(fieldsCount) { index ->
            parseField(reader, index)
        }
    }

    private fun parseField(reader: ClassFileByteReader, index: Int): FieldInfo {
        val ownerPath = "fields[$index]"
        return FieldInfo(
            accessFlags = reader.readU2(),
            nameIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.name_index"),
            descriptorIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.descriptor_index"),
            attributes = RawAttributeInfoParser.parseAttributes(reader, ownerPath),
        )
    }
}
