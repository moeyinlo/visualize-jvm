package me.moeyinlo.visualize.jvm.classfile

data class LocalVariableTypeTableAttribute(
    override val nameIndex: ConstantPoolIndex,
    val entries: List<LocalVariableTypeTableEntry>,
) : AttributeInfo

data class LocalVariableTypeTableEntry(
    val startPc: Int,
    val length: Int,
    val nameIndex: ConstantPoolIndex,
    val signatureIndex: ConstantPoolIndex,
    val index: Int,
)

object LocalVariableTypeTableAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val localVariableTypeTableLength = context.reader.readU2()
        val entries = List(localVariableTypeTableLength) { index ->
            parseEntry(context, "${context.ownerPath}.local_variable_type_table[$index]")
        }
        return LocalVariableTypeTableAttribute(
            nameIndex = context.nameIndex,
            entries = entries,
        )
    }

    private fun parseEntry(
        context: AttributeParseContext,
        ownerPath: String,
    ): LocalVariableTypeTableEntry {
        val startPc = context.reader.readU2()
        val length = context.reader.readU2()
        val nameIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(context.reader, "$ownerPath.name_index")
        expectUtf8(context, "$ownerPath.name_index", nameIndex)
        val signatureIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            context.reader,
            "$ownerPath.signature_index",
        )
        expectUtf8(context, "$ownerPath.signature_index", signatureIndex)
        return LocalVariableTypeTableEntry(
            startPc = startPc,
            length = length,
            nameIndex = nameIndex,
            signatureIndex = signatureIndex,
            index = context.reader.readU2(),
        )
    }

    private fun expectUtf8(
        context: AttributeParseContext,
        role: String,
        index: ConstantPoolIndex,
    ) {
        val entry = try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
    }
}
