package me.moeyinlo.visualize.jvm.classfile

data class PermittedSubclassesAttribute(
    override val nameIndex: ConstantPoolIndex,
    val classes: List<ConstantPoolIndex>,
) : AttributeInfo

object PermittedSubclassesAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val numberOfClasses = context.reader.readU2()
        return PermittedSubclassesAttribute(
            nameIndex = context.nameIndex,
            classes = List(numberOfClasses) { index ->
                readClassIndex(context, "${context.ownerPath}.classes[$index]")
            },
        )
    }

    private fun readClassIndex(
        context: AttributeParseContext,
        role: String,
    ): ConstantPoolIndex {
        val index = RawAttributeInfoParser.readNonZeroConstantPoolIndex(context.reader, role)
        val entry = try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
        if (entry !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected CONSTANT_Class_info but found ${entry.javaClass.simpleName}",
            )
        }
        return index
    }
}
