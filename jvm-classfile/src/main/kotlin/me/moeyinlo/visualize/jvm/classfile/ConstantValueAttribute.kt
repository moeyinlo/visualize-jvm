package me.moeyinlo.visualize.jvm.classfile

data class ConstantValueAttribute(
    override val nameIndex: ConstantPoolIndex,
    val constantValueIndex: ConstantPoolIndex,
) : AttributeInfo

object ConstantValueAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.length != 2) {
            throw ClassFileFormatException(
                "Invalid ConstantValue attribute_length=${context.length} " +
                    "at ${context.ownerPath}: expected 2",
            )
        }

        val constantValueIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "${context.ownerPath}.constantvalue_index",
        )
        validateConstantValueEntry(context, constantValueIndex)
        return ConstantValueAttribute(
            nameIndex = context.nameIndex,
            constantValueIndex = constantValueIndex,
        )
    }

    private fun validateConstantValueEntry(
        context: AttributeParseContext,
        constantValueIndex: ConstantPoolIndex,
    ) {
        val entry = try {
            context.constantPool[constantValueIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.constantvalue_index=$constantValueIndex: ${exception.message}",
            )
        }
        if (!entry.isConstantValueEntry()) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.constantvalue_index=$constantValueIndex: " +
                    "expected CONSTANT_Integer, CONSTANT_Float, CONSTANT_Long, CONSTANT_Double, " +
                    "or CONSTANT_String but found ${entry.javaClass.simpleName}",
            )
        }
    }

    private fun ConstantPoolEntry.isConstantValueEntry(): Boolean =
        this is ConstantIntegerEntry ||
            this is ConstantFloatEntry ||
            this is ConstantLongEntry ||
            this is ConstantDoubleEntry ||
            this is ConstantStringEntry
}
