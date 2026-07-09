package me.moeyinlo.visualize.jvm.classfile

data class RecordAttribute(
    override val nameIndex: ConstantPoolIndex,
    val components: List<RecordComponentInfo>,
) : AttributeInfo

data class RecordComponentInfo(
    val nameIndex: ConstantPoolIndex,
    val descriptorIndex: ConstantPoolIndex,
    val attributes: List<AttributeInfo>,
)

object RecordAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val componentsCount = context.reader.readU2()
        return RecordAttribute(
            nameIndex = context.nameIndex,
            components = List(componentsCount) { index ->
                parseComponent(context, "${context.ownerPath}.components[$index]")
            },
        )
    }

    private fun parseComponent(
        context: AttributeParseContext,
        ownerPath: String,
    ): RecordComponentInfo =
        RecordComponentInfo(
            nameIndex = readUtf8Index(context, "$ownerPath.name_index"),
            descriptorIndex = readUtf8Index(context, "$ownerPath.descriptor_index"),
            attributes = AttributeInfoParser.parseAttributes(
                reader = context.reader,
                constantPool = context.constantPool,
                registry = context.registry,
                ownerPath = ownerPath,
            ),
        )

    private fun readUtf8Index(
        context: AttributeParseContext,
        role: String,
    ): ConstantPoolIndex {
        val index = RawAttributeInfoParser.readNonZeroConstantPoolIndex(context.reader, role)
        val entry = try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return index
    }
}
