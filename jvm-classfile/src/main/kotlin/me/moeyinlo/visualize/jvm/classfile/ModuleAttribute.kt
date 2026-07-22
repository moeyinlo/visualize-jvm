package me.moeyinlo.visualize.jvm.classfile

data class ModuleAttribute(
    override val nameIndex: ConstantPoolIndex,
    val moduleNameIndex: ConstantPoolIndex,
    val moduleFlags: Int,
    val moduleVersionIndex: ConstantPoolIndex?,
    val requires: List<ModuleRequires>,
    val exports: List<ModuleExports>,
    val opens: List<ModuleOpens>,
    val uses: List<ConstantPoolIndex>,
    val provides: List<ModuleProvides>,
) : AttributeInfo

data class ModuleRequires(
    val requiresIndex: ConstantPoolIndex,
    val requiresFlags: Int,
    val requiresVersionIndex: ConstantPoolIndex?,
)

data class ModuleExports(
    val exportsIndex: ConstantPoolIndex,
    val exportsFlags: Int,
    val exportsToIndexes: List<ConstantPoolIndex>,
)

data class ModuleOpens(
    val opensIndex: ConstantPoolIndex,
    val opensFlags: Int,
    val opensToIndexes: List<ConstantPoolIndex>,
)

data class ModuleProvides(
    val providesIndex: ConstantPoolIndex,
    val providesWithIndexes: List<ConstantPoolIndex>,
)

object ModuleAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val moduleNameIndex = readRequiredIndex<ConstantModuleEntry>(context, "${context.ownerPath}.module_name_index")
        val moduleFlags = context.reader.readU2()
        val moduleVersionIndex = readOptionalIndex<ConstantUtf8Entry>(
            context = context,
            role = "${context.ownerPath}.module_version_index",
            expected = "CONSTANT_Utf8_info",
        )
        return ModuleAttribute(
            nameIndex = context.nameIndex,
            moduleNameIndex = moduleNameIndex,
            moduleFlags = moduleFlags,
            moduleVersionIndex = moduleVersionIndex,
            requires = parseRequires(context),
            exports = parseExports(context),
            opens = parseOpens(context),
            uses = parseUses(context),
            provides = parseProvides(context),
        )
    }

    private fun parseRequires(context: AttributeParseContext): List<ModuleRequires> {
        val requires = List(context.reader.readU2()) { index ->
            val ownerPath = "${context.ownerPath}.requires[$index]"
            ModuleRequires(
                requiresIndex = readRequiredIndex<ConstantModuleEntry>(context, "$ownerPath.requires_index"),
                requiresFlags = context.reader.readU2(),
                requiresVersionIndex = readOptionalIndex<ConstantUtf8Entry>(
                    context = context,
                    role = "$ownerPath.requires_version_index",
                    expected = "CONSTANT_Utf8_info",
                ),
            )
        }
        requireUniqueConstantPoolIndexes(
            indexes = requires.map { it.requiresIndex },
            role = "${context.ownerPath}.requires",
            fieldName = "requires_index",
        )
        return requires
    }

    private fun parseExports(context: AttributeParseContext): List<ModuleExports> =
        List(context.reader.readU2()) { index ->
            val ownerPath = "${context.ownerPath}.exports[$index]"
            ModuleExports(
                exportsIndex = readRequiredIndex<ConstantPackageEntry>(context, "$ownerPath.exports_index"),
                exportsFlags = context.reader.readU2(),
                exportsToIndexes = parseModuleIndexList(context, "$ownerPath.exports_to_index", context.reader.readU2()),
            )
        }

    private fun parseOpens(context: AttributeParseContext): List<ModuleOpens> =
        List(context.reader.readU2()) { index ->
            val ownerPath = "${context.ownerPath}.opens[$index]"
            ModuleOpens(
                opensIndex = readRequiredIndex<ConstantPackageEntry>(context, "$ownerPath.opens_index"),
                opensFlags = context.reader.readU2(),
                opensToIndexes = parseModuleIndexList(context, "$ownerPath.opens_to_index", context.reader.readU2()),
            )
        }

    private fun parseUses(context: AttributeParseContext): List<ConstantPoolIndex> =
        List(context.reader.readU2()) { index ->
            readRequiredIndex<ConstantClassEntry>(context, "${context.ownerPath}.uses_index[$index]")
        }

    private fun parseProvides(context: AttributeParseContext): List<ModuleProvides> =
        List(context.reader.readU2()) { index ->
            val ownerPath = "${context.ownerPath}.provides[$index]"
            ModuleProvides(
                providesIndex = readRequiredIndex<ConstantClassEntry>(context, "$ownerPath.provides_index"),
                providesWithIndexes = List(context.reader.readU2()) { withIndex ->
                    readRequiredIndex<ConstantClassEntry>(context, "$ownerPath.provides_with_index[$withIndex]")
                },
            )
        }

    private fun parseModuleIndexList(
        context: AttributeParseContext,
        rolePrefix: String,
        count: Int,
    ): List<ConstantPoolIndex> =
        List(count) { index ->
            readRequiredIndex<ConstantModuleEntry>(context, "$rolePrefix[$index]")
        }

    private inline fun <reified T : ConstantPoolEntry> readRequiredIndex(
        context: AttributeParseContext,
        role: String,
    ): ConstantPoolIndex {
        val index = RawAttributeInfoParser.readNonZeroConstantPoolIndex(context.reader, role)
        expectEntry<T>(context, role, index, expectedConstantName<T>())
        return index
    }

    private inline fun <reified T : ConstantPoolEntry> readOptionalIndex(
        context: AttributeParseContext,
        role: String,
        expected: String,
    ): ConstantPoolIndex? {
        val rawIndex = context.reader.readU2()
        if (rawIndex == 0) {
            return null
        }
        val index = ConstantPoolIndex(rawIndex)
        expectEntry<T>(context, role, index, expected)
        return index
    }

    private inline fun <reified T : ConstantPoolEntry> expectEntry(
        context: AttributeParseContext,
        role: String,
        index: ConstantPoolIndex,
        expected: String,
    ) {
        val entry = try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
        if (entry !is T) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected $expected but found ${entry.javaClass.simpleName}",
            )
        }
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
            ConstantModuleEntry::class -> "CONSTANT_Module_info"
            ConstantPackageEntry::class -> "CONSTANT_Package_info"
            ConstantUtf8Entry::class -> "CONSTANT_Utf8_info"
            else -> T::class.java.simpleName
        }
}
