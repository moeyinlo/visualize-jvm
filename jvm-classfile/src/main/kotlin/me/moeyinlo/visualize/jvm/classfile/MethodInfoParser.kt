package me.moeyinlo.visualize.jvm.classfile

data class MethodInfo(
    val accessFlags: Int,
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
    val attributes: List<RawAttributeInfo>,
)

object MethodInfoParser {
    fun parseMethods(reader: ClassFileByteReader): List<MethodInfo> {
        val methodsCount = reader.readU2()
        return List(methodsCount) { index ->
            parseMethod(reader, index)
        }
    }

    private fun parseMethod(reader: ClassFileByteReader, index: Int): MethodInfo {
        val ownerPath = "methods[$index]"
        return MethodInfo(
            accessFlags = reader.readU2(),
            nameIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.name_index"),
            descriptorIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(reader, "$ownerPath.descriptor_index"),
            attributes = RawAttributeInfoParser.parseAttributes(reader, ownerPath),
        )
    }
}
