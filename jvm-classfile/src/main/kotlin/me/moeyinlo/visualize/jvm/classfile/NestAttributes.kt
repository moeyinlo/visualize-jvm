package me.moeyinlo.visualize.jvm.classfile

data class NestHostAttribute(
    override val nameIndex: ConstantPoolIndex,
    val hostClassIndex: ConstantPoolIndex,
) : AttributeInfo

data class NestMembersAttribute(
    override val nameIndex: ConstantPoolIndex,
    val classes: List<ConstantPoolIndex>,
) : AttributeInfo

object NestHostAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.length != 2) {
            throw ClassFileFormatException(
                "Invalid NestHost attribute_length=${context.length} at ${context.ownerPath}: expected 2",
            )
        }
        return NestHostAttribute(
            nameIndex = context.nameIndex,
            hostClassIndex = readClassIndex(context, "${context.ownerPath}.host_class_index"),
        )
    }
}

object NestMembersAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val numberOfClasses = context.reader.readU2()
        return NestMembersAttribute(
            nameIndex = context.nameIndex,
            classes = List(numberOfClasses) { index ->
                readClassIndex(context, "${context.ownerPath}.classes[$index]")
            },
        )
    }
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
    val nameEntry = try {
        context.constantPool[entry.nameIndex]
    } catch (exception: ConstantPoolFormatException) {
        throw ClassFileFormatException("Invalid $role=$index name_index=${entry.nameIndex}: ${exception.message}")
    }
    if (nameEntry !is ConstantUtf8Entry) {
        throw ClassFileFormatException(
            "Invalid $role=$index name_index=${entry.nameIndex}: expected CONSTANT_Utf8_info but found ${nameEntry.javaClass.simpleName}",
        )
    }
    try {
        ClassNameValidator.validateInternalBinaryName(entry.nameIndex, "name_index", nameEntry.value)
    } catch (exception: ClassFileFormatException) {
        throw ClassFileFormatException("Invalid $role=$index name_index=${entry.nameIndex}: ${exception.message}")
    }
    return index
}
