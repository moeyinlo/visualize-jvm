package me.moeyinlo.visualize.jvm.classfile

data class InnerClassesAttribute(
    override val nameIndex: ConstantPoolIndex,
    val classes: List<InnerClassEntry>,
) : AttributeInfo

data class InnerClassEntry(
    val innerClassInfoIndex: ConstantPoolIndex,
    val outerClassInfoIndex: ConstantPoolIndex?,
    val innerNameIndex: ConstantPoolIndex?,
    val innerClassAccessFlags: Int,
)

data class EnclosingMethodAttribute(
    override val nameIndex: ConstantPoolIndex,
    val classIndex: ConstantPoolIndex,
    val methodIndex: ConstantPoolIndex?,
) : AttributeInfo

object InnerClassesAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val numberOfClasses = context.reader.readU2()
        val classes = List(numberOfClasses) { index ->
            parseClassEntry(context, "${context.ownerPath}.classes[$index]")
        }
        return InnerClassesAttribute(
            nameIndex = context.nameIndex,
            classes = classes,
        )
    }

    private fun parseClassEntry(
        context: AttributeParseContext,
        ownerPath: String,
    ): InnerClassEntry {
        val innerClassInfoIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "$ownerPath.inner_class_info_index",
        )
        expectConstantClass(context, "$ownerPath.inner_class_info_index", innerClassInfoIndex)
        val outerClassInfoIndex = readOptionalClassIndex(context, "$ownerPath.outer_class_info_index")
        val innerNameIndex = readOptionalUtf8Index(context, "$ownerPath.inner_name_index")
        return InnerClassEntry(
            innerClassInfoIndex = innerClassInfoIndex,
            outerClassInfoIndex = outerClassInfoIndex,
            innerNameIndex = innerNameIndex,
            innerClassAccessFlags = context.reader.readU2(),
        )
    }
}

object EnclosingMethodAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.length != 4) {
            throw ClassFileFormatException(
                "Invalid EnclosingMethod attribute_length=${context.length} " +
                    "at ${context.ownerPath}: expected 4",
            )
        }
        val classIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "${context.ownerPath}.class_index",
        )
        expectConstantClass(context, "${context.ownerPath}.class_index", classIndex)
        return EnclosingMethodAttribute(
            nameIndex = context.nameIndex,
            classIndex = classIndex,
            methodIndex = readOptionalNameAndTypeIndex(context, "${context.ownerPath}.method_index"),
        )
    }
}

private fun readOptionalClassIndex(
    context: AttributeParseContext,
    role: String,
): ConstantPoolIndex? {
    val rawIndex = context.reader.readU2()
    if (rawIndex == 0) {
        return null
    }
    val index = ConstantPoolIndex(rawIndex)
    expectConstantClass(context, role, index)
    return index
}

private fun readOptionalUtf8Index(
    context: AttributeParseContext,
    role: String,
): ConstantPoolIndex? {
    val rawIndex = context.reader.readU2()
    if (rawIndex == 0) {
        return null
    }
    val index = ConstantPoolIndex(rawIndex)
    val entry = constantPoolEntry(context, role, index)
    if (entry !is ConstantUtf8Entry) {
        throw ClassFileFormatException(
            "Invalid $role=$index: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
        )
    }
    ClassNameValidator.validateUnqualifiedName(index, role, entry.value)
    return index
}

private fun readOptionalNameAndTypeIndex(
    context: AttributeParseContext,
    role: String,
): ConstantPoolIndex? {
    val rawIndex = context.reader.readU2()
    if (rawIndex == 0) {
        return null
    }
    val index = ConstantPoolIndex(rawIndex)
    val entry = constantPoolEntry(context, role, index)
    if (entry !is ConstantNameAndTypeEntry) {
        throw ClassFileFormatException(
            "Invalid $role=$index: expected CONSTANT_NameAndType_info but found ${entry.javaClass.simpleName}",
        )
    }
    return index
}

private fun expectConstantClass(
    context: AttributeParseContext,
    role: String,
    index: ConstantPoolIndex,
) {
    val entry = constantPoolEntry(context, role, index)
    if (entry !is ConstantClassEntry) {
        throw ClassFileFormatException(
            "Invalid $role=$index: expected CONSTANT_Class_info but found ${entry.javaClass.simpleName}",
        )
    }
}

private fun constantPoolEntry(
    context: AttributeParseContext,
    role: String,
    index: ConstantPoolIndex,
): ConstantPoolEntry = try {
    context.constantPool[index]
} catch (exception: ConstantPoolFormatException) {
    throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
}
