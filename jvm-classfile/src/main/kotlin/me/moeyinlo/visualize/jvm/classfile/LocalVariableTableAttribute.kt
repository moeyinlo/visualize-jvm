package me.moeyinlo.visualize.jvm.classfile

data class LocalVariableTableAttribute(
    override val nameIndex: ConstantPoolIndex,
    val entries: List<LocalVariableTableEntry>,
) : AttributeInfo

data class LocalVariableTableEntry(
    val startPc: Int,
    val length: Int,
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
    val index: Int,
)

object LocalVariableTableAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val localVariableTableLength = context.reader.readU2()
        val entries = List(localVariableTableLength) { index ->
            parseEntry(context, "${context.ownerPath}.local_variable_table[$index]")
        }
        return LocalVariableTableAttribute(
            nameIndex = context.nameIndex,
            entries = entries,
        )
    }

    private fun parseEntry(
        context: AttributeParseContext,
        ownerPath: String,
    ): LocalVariableTableEntry {
        val startPc = context.reader.readU2()
        val length = context.reader.readU2()
        val nameIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(context.reader, "$ownerPath.name_index")
        val name = expectUtf8(context, "$ownerPath.name_index", nameIndex)
        ClassNameValidator.validateUnqualifiedName(nameIndex, "$ownerPath.name_index", name.value)
        val descriptorIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            context.reader,
            "$ownerPath.descriptor_index",
        )
        val descriptor = expectUtf8(context, "$ownerPath.descriptor_index", descriptorIndex)
        DescriptorValidator.validateFieldDescriptor(descriptorIndex, "$ownerPath.descriptor_index", descriptor.value)
        return LocalVariableTableEntry(
            startPc = startPc,
            length = length,
            nameIndex = nameIndex,
            descriptorIndex = descriptorIndex,
            index = context.reader.readU2(),
        )
    }

    private fun expectUtf8(
        context: AttributeParseContext,
        role: String,
        index: ConstantPoolIndex,
    ): ConstantUtf8Entry {
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
        return entry
    }
}
