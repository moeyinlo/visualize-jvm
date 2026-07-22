package me.moeyinlo.visualize.jvm.classfile

data class ModulePackagesAttribute(
    override val nameIndex: ConstantPoolIndex,
    val packageIndexes: List<ConstantPoolIndex>,
) : AttributeInfo

data class ModuleMainClassAttribute(
    override val nameIndex: ConstantPoolIndex,
    val mainClassIndex: ConstantPoolIndex,
) : AttributeInfo

object ModulePackagesAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val packageCount = context.reader.readU2()
        val packageIndexes = List(packageCount) { index ->
            readRequiredIndex<ConstantPackageEntry>(context, "${context.ownerPath}.package_index[$index]")
        }
        requireUniqueConstantPoolIndexes(
            indexes = packageIndexes,
            role = "${context.ownerPath}.packages",
            fieldName = "package_index",
        )
        return ModulePackagesAttribute(
            nameIndex = context.nameIndex,
            packageIndexes = packageIndexes,
        )
    }
}

object ModuleMainClassAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.length != 2) {
            throw ClassFileFormatException(
                "Invalid ModuleMainClass attribute_length=${context.length} at ${context.ownerPath}: expected 2",
            )
        }
        return ModuleMainClassAttribute(
            nameIndex = context.nameIndex,
            mainClassIndex = readRequiredIndex<ConstantClassEntry>(context, "${context.ownerPath}.main_class_index"),
        )
    }
}

private inline fun <reified T : ConstantPoolEntry> readRequiredIndex(
    context: AttributeParseContext,
    role: String,
): ConstantPoolIndex {
    val index = RawAttributeInfoParser.readNonZeroConstantPoolIndex(context.reader, role)
    val entry = try {
        context.constantPool[index]
    } catch (exception: ConstantPoolFormatException) {
        throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
    }
    if (entry !is T) {
        throw ClassFileFormatException(
            "Invalid $role=$index: expected ${expectedConstantName<T>()} but found ${entry.javaClass.simpleName}",
        )
    }
    return index
}

private fun requireUniqueConstantPoolIndexes(
    indexes: List<ConstantPoolIndex>,
    role: String,
    fieldName: String,
) {
    val seen = mutableSetOf<ConstantPoolIndex>()
    indexes.forEach { index ->
        if (!seen.add(index)) {
            throw ClassFileFormatException("Invalid $role: duplicate $fieldName $index")
        }
    }
}

private inline fun <reified T : ConstantPoolEntry> expectedConstantName(): String =
    when (T::class) {
        ConstantClassEntry::class -> "CONSTANT_Class_info"
        ConstantPackageEntry::class -> "CONSTANT_Package_info"
        else -> T::class.java.simpleName
    }
