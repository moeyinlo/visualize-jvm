package me.moeyinlo.visualize.jvm.classfile

data class ClassIdentity(
    val thisClassIndex: ConstantPoolIndex,
    val superClassIndex: ConstantPoolIndex?,
    val interfaceIndexes: List<ConstantPoolIndex>,
)

object ClassIdentityParser {
    fun parse(reader: ClassFileByteReader): ClassIdentity {
        val thisClassOffset = reader.position
        val thisClassIndex = reader.readU2()
        if (thisClassIndex == 0) {
            throw ClassFileFormatException(
                "Invalid this_class index source=${reader.source} offset=$thisClassOffset: zero is not allowed",
            )
        }

        val superClassRaw = reader.readU2()
        val interfacesCount = reader.readU2()
        val interfaceIndexes = List(interfacesCount) { interfaceIndex ->
            val offset = reader.position
            val rawIndex = reader.readU2()
            if (rawIndex == 0) {
                throw ClassFileFormatException(
                    "Invalid interfaces[$interfaceIndex] index source=${reader.source} offset=$offset: zero is not allowed",
                )
            }
            ConstantPoolIndex(rawIndex)
        }

        return ClassIdentity(
            thisClassIndex = ConstantPoolIndex(thisClassIndex),
            superClassIndex = if (superClassRaw == 0) null else ConstantPoolIndex(superClassRaw),
            interfaceIndexes = interfaceIndexes,
        )
    }
}
