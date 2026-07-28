package me.moeyinlo.visualize.jvm.classfile

data class PermittedSubclassesAttribute(
    override val nameIndex: ConstantPoolIndex,
    val classes: List<ConstantPoolIndex>,
) : AttributeInfo

object PermittedSubclassesAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.majorVersion < Java17MajorVersion) {
            throw ClassFileFormatException(
                "Invalid PermittedSubclasses attribute at ${context.ownerPath}: " +
                    "major_version=${context.majorVersion} must be at least $Java17MajorVersion",
            )
        }
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
}

private const val Java17MajorVersion = 61
