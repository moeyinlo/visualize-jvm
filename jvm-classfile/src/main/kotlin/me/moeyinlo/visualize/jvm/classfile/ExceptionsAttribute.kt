package me.moeyinlo.visualize.jvm.classfile

data class ExceptionsAttribute(
    override val nameIndex: ConstantPoolIndex,
    val exceptionIndexTable: List<ConstantPoolIndex>,
) : AttributeInfo

object ExceptionsAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val numberOfExceptions = context.reader.readU2()
        val exceptions = List(numberOfExceptions) { index ->
            readExceptionIndex(context, index)
        }
        return ExceptionsAttribute(
            nameIndex = context.nameIndex,
            exceptionIndexTable = exceptions,
        )
    }

    private fun readExceptionIndex(
        context: AttributeParseContext,
        index: Int,
    ): ConstantPoolIndex {
        val exceptionIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "${context.ownerPath}.exception_index_table[$index]",
        )
        val entry = try {
            context.constantPool[exceptionIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.exception_index_table[$index]=$exceptionIndex: ${exception.message}",
            )
        }
        if (entry !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.exception_index_table[$index]=$exceptionIndex: " +
                    "expected CONSTANT_Class_info but found ${entry.javaClass.simpleName}",
            )
        }
        return exceptionIndex
    }
}
