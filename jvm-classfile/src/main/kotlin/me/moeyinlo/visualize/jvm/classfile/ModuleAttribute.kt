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
    private const val AccOpen = 0x0020
    private const val AccTransitive = 0x0020
    private const val AccStaticPhase = 0x0040
    private const val AccSynthetic = 0x1000
    private const val AccMandated = 0x8000
    private const val JavaBaseModuleName = "java.base"
    private const val Java10MajorVersion = 54
    private const val AllowedModuleFlags = AccOpen or AccSynthetic or AccMandated
    private const val AllowedRequiresFlags = AccTransitive or AccStaticPhase or AccSynthetic or AccMandated
    private const val AllowedExportsFlags = AccSynthetic or AccMandated
    private const val AllowedOpensFlags = AccSynthetic or AccMandated

    override fun parse(context: AttributeParseContext): AttributeInfo {
        val moduleNameIndex = readRequiredIndex<ConstantModuleEntry>(context, "${context.ownerPath}.module_name_index")
        val moduleFlags = context.reader.readU2()
        requireAllowedFlags(
            flags = moduleFlags,
            allowedMask = AllowedModuleFlags,
            role = "${context.ownerPath}.module_flags",
        )
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
            requires = parseRequires(context, moduleNameIndex),
            exports = parseExports(context),
            opens = parseOpens(context, moduleFlags),
            uses = parseUses(context),
            provides = parseProvides(context),
        )
    }

    private fun parseRequires(
        context: AttributeParseContext,
        moduleNameIndex: ConstantPoolIndex,
    ): List<ModuleRequires> {
        val requires = List(context.reader.readU2()) { index ->
            val ownerPath = "${context.ownerPath}.requires[$index]"
            val requiresIndex = readRequiredIndex<ConstantModuleEntry>(context, "$ownerPath.requires_index")
            val requiresFlags = context.reader.readU2()
            requireAllowedFlags(
                flags = requiresFlags,
                allowedMask = AllowedRequiresFlags,
                role = "$ownerPath.requires_flags",
            )
            ModuleRequires(
                requiresIndex = requiresIndex,
                requiresFlags = requiresFlags,
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
        requireJavaBaseModuleHasNoRequires(context, moduleNameIndex, requires)
        requireJavaBaseRequiresAreNotSynthetic(context, requires)
        requireJavaBaseRequiresAreNotStaticPhase(context, requires)
        return requires
    }

    private fun requireAllowedFlags(
        flags: Int,
        allowedMask: Int,
        role: String,
    ) {
        val invalidBits = flags and allowedMask.inv() and 0xFFFF
        if (invalidBits != 0) {
            throw ClassFileFormatException(
                "Invalid $role=0x${flags.toU2Hex()}: unknown flag bits 0x${invalidBits.toU2Hex()}",
            )
        }
    }

    private fun requireJavaBaseModuleHasNoRequires(
        context: AttributeParseContext,
        moduleNameIndex: ConstantPoolIndex,
        requires: List<ModuleRequires>,
    ) {
        if (requires.isNotEmpty() &&
            moduleName(context, moduleNameIndex, "${context.ownerPath}.module_name_index") == JavaBaseModuleName
        ) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.requires_count=${requires.size}: " +
                    "java.base module must not declare requires entries",
            )
        }
    }

    private fun requireJavaBaseRequiresAreNotSynthetic(
        context: AttributeParseContext,
        requires: List<ModuleRequires>,
    ) {
        requires.forEachIndexed { index, entry ->
            if (moduleName(context, entry.requiresIndex, "${context.ownerPath}.requires[$index].requires_index") ==
                JavaBaseModuleName &&
                entry.requiresFlags and AccSynthetic != 0
            ) {
                throw ClassFileFormatException(
                    "Invalid ${context.ownerPath}.requires[$index].requires_flags: " +
                        "requires java.base must not set ACC_SYNTHETIC",
                )
            }
        }
    }

    private fun requireJavaBaseRequiresAreNotStaticPhase(
        context: AttributeParseContext,
        requires: List<ModuleRequires>,
    ) {
        if (context.majorVersion < Java10MajorVersion) {
            return
        }
        requires.forEachIndexed { index, entry ->
            if (moduleName(context, entry.requiresIndex, "${context.ownerPath}.requires[$index].requires_index") ==
                JavaBaseModuleName &&
                entry.requiresFlags and AccStaticPhase != 0
            ) {
                throw ClassFileFormatException(
                    "Invalid ${context.ownerPath}.requires[$index].requires_flags: " +
                        "requires java.base must not set ACC_STATIC_PHASE for classfile major >= $Java10MajorVersion",
                )
            }
        }
    }

    private fun parseExports(context: AttributeParseContext): List<ModuleExports> {
        val exports = List(context.reader.readU2()) { index ->
            val ownerPath = "${context.ownerPath}.exports[$index]"
            val exportsIndex = readRequiredIndex<ConstantPackageEntry>(context, "$ownerPath.exports_index")
            val exportsFlags = context.reader.readU2()
            requireAllowedFlags(
                flags = exportsFlags,
                allowedMask = AllowedExportsFlags,
                role = "$ownerPath.exports_flags",
            )
            val exportsToIndexes = parseModuleIndexList(context, "$ownerPath.exports_to_index", context.reader.readU2())
            requireUniqueConstantPoolIndexes(
                indexes = exportsToIndexes,
                role = "$ownerPath.exports_to",
                fieldName = "exports_to_index",
            )
            requireUniqueNames(
                names = exportsToIndexes.mapIndexed { targetIndex, target ->
                    moduleName(context, target, "$ownerPath.exports_to_index[$targetIndex]")
                },
                role = "$ownerPath.exports_to",
                fieldName = "exports_to_index module name",
            )
            ModuleExports(
                exportsIndex = exportsIndex,
                exportsFlags = exportsFlags,
                exportsToIndexes = exportsToIndexes,
            )
        }
        requireUniqueConstantPoolIndexes(
            indexes = exports.map { it.exportsIndex },
            role = "${context.ownerPath}.exports",
            fieldName = "exports_index",
        )
        requireUniqueNames(
            names = exports.mapIndexed { index, entry ->
                packageName(context, entry.exportsIndex, "${context.ownerPath}.exports[$index].exports_index")
            },
            role = "${context.ownerPath}.exports",
            fieldName = "exports_index package name",
        )
        return exports
    }

    private fun parseOpens(
        context: AttributeParseContext,
        moduleFlags: Int,
    ): List<ModuleOpens> {
        val opens = List(context.reader.readU2()) { index ->
            val ownerPath = "${context.ownerPath}.opens[$index]"
            val opensIndex = readRequiredIndex<ConstantPackageEntry>(context, "$ownerPath.opens_index")
            val opensFlags = context.reader.readU2()
            requireAllowedFlags(
                flags = opensFlags,
                allowedMask = AllowedOpensFlags,
                role = "$ownerPath.opens_flags",
            )
            val opensToIndexes = parseModuleIndexList(context, "$ownerPath.opens_to_index", context.reader.readU2())
            requireUniqueConstantPoolIndexes(
                indexes = opensToIndexes,
                role = "$ownerPath.opens_to",
                fieldName = "opens_to_index",
            )
            ModuleOpens(
                opensIndex = opensIndex,
                opensFlags = opensFlags,
                opensToIndexes = opensToIndexes,
            )
        }
        requireUniqueConstantPoolIndexes(
            indexes = opens.map { it.opensIndex },
            role = "${context.ownerPath}.opens",
            fieldName = "opens_index",
        )
        requireUniqueNames(
            names = opens.mapIndexed { index, entry ->
                packageName(context, entry.opensIndex, "${context.ownerPath}.opens[$index].opens_index")
            },
            role = "${context.ownerPath}.opens",
            fieldName = "opens_index package name",
        )
        if (opens.isNotEmpty() && moduleFlags and AccOpen != 0) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.opens_count=${opens.size}: " +
                    "open modules must not declare opens entries",
            )
        }
        return opens
    }

    private fun parseUses(context: AttributeParseContext): List<ConstantPoolIndex> {
        val uses = List(context.reader.readU2()) { index ->
            readRequiredIndex<ConstantClassEntry>(context, "${context.ownerPath}.uses_index[$index]")
        }
        requireUniqueConstantPoolIndexes(
            indexes = uses,
            role = "${context.ownerPath}.uses",
            fieldName = "uses_index",
        )
        return uses
    }

    private fun parseProvides(context: AttributeParseContext): List<ModuleProvides> {
        val provides = List(context.reader.readU2()) { index ->
            val ownerPath = "${context.ownerPath}.provides[$index]"
            val providesIndex = readRequiredIndex<ConstantClassEntry>(context, "$ownerPath.provides_index")
            val providesWithCount = context.reader.readU2()
            if (providesWithCount == 0) {
                throw ClassFileFormatException(
                    "Invalid $ownerPath.provides_with_count=0: provides_with_count must be nonzero",
                )
            }
            val providesWithIndexes = List(providesWithCount) { withIndex ->
                readRequiredIndex<ConstantClassEntry>(context, "$ownerPath.provides_with_index[$withIndex]")
            }
            requireUniqueConstantPoolIndexes(
                indexes = providesWithIndexes,
                role = "$ownerPath.provides_with",
                fieldName = "provides_with_index",
            )
            ModuleProvides(
                providesIndex = providesIndex,
                providesWithIndexes = providesWithIndexes,
            )
        }
        requireUniqueConstantPoolIndexes(
            indexes = provides.map { it.providesIndex },
            role = "${context.ownerPath}.provides",
            fieldName = "provides_index",
        )
        return provides
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
        expectEntryValue<T>(context, role, index, expected)
    }

    private inline fun <reified T : ConstantPoolEntry> expectEntryValue(
        context: AttributeParseContext,
        role: String,
        index: ConstantPoolIndex,
        expected: String,
    ): T {
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
        return entry
    }

    private fun moduleName(
        context: AttributeParseContext,
        index: ConstantPoolIndex,
        role: String,
    ): String {
        val moduleEntry = expectEntryValue<ConstantModuleEntry>(
            context = context,
            role = role,
            index = index,
            expected = "CONSTANT_Module_info",
        )
        return expectEntryValue<ConstantUtf8Entry>(
            context = context,
            role = "$role.name_index",
            index = moduleEntry.nameIndex,
            expected = "CONSTANT_Utf8_info",
        ).value
    }

    private fun packageName(
        context: AttributeParseContext,
        index: ConstantPoolIndex,
        role: String,
    ): String {
        val packageEntry = expectEntryValue<ConstantPackageEntry>(
            context = context,
            role = role,
            index = index,
            expected = "CONSTANT_Package_info",
        )
        return expectEntryValue<ConstantUtf8Entry>(
            context = context,
            role = "$role.name_index",
            index = packageEntry.nameIndex,
            expected = "CONSTANT_Utf8_info",
        ).value
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

    private fun requireUniqueNames(
        names: List<String>,
        role: String,
        fieldName: String,
    ) {
        val seen = mutableSetOf<String>()
        names.forEach { name ->
            if (!seen.add(name)) {
                throw ClassFileFormatException("Invalid $role: duplicate $fieldName '$name'")
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

    private fun Int.toU2Hex(): String = toString(16).uppercase().padStart(4, '0')
}
