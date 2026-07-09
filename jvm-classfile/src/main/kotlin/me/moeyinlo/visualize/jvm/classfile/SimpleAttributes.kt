package me.moeyinlo.visualize.jvm.classfile

data class SyntheticAttribute(
    override val nameIndex: ConstantPoolIndex,
) : AttributeInfo

data class DeprecatedAttribute(
    override val nameIndex: ConstantPoolIndex,
) : AttributeInfo

data class SourceFileAttribute(
    override val nameIndex: ConstantPoolIndex,
    val sourceFileIndex: ConstantPoolIndex,
) : AttributeInfo

object SyntheticAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        requireZeroLength(context, "Synthetic")
        return SyntheticAttribute(context.nameIndex)
    }
}

object DeprecatedAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        requireZeroLength(context, "Deprecated")
        return DeprecatedAttribute(context.nameIndex)
    }
}

object SourceFileAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.length != 2) {
            throw ClassFileFormatException(
                "Invalid SourceFile attribute_length=${context.length} at ${context.ownerPath}: expected 2",
            )
        }
        val sourceFileIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "${context.ownerPath}.sourcefile_index",
        )
        val entry = try {
            context.constantPool[sourceFileIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.sourcefile_index=$sourceFileIndex: ${exception.message}",
            )
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.sourcefile_index=$sourceFileIndex: " +
                    "expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return SourceFileAttribute(
            nameIndex = context.nameIndex,
            sourceFileIndex = sourceFileIndex,
        )
    }
}

private fun requireZeroLength(
    context: AttributeParseContext,
    attributeName: String,
) {
    if (context.length != 0) {
        throw ClassFileFormatException(
            "Invalid $attributeName attribute_length=${context.length} at ${context.ownerPath}: expected 0",
        )
    }
}
