package me.moeyinlo.visualize.jvm.classfile

data class MethodParametersAttribute(
    override val nameIndex: ConstantPoolIndex,
    val parameters: List<MethodParameter>,
) : AttributeInfo

data class MethodParameter(
    val nameIndex: ConstantPoolIndex?,
    val accessFlags: Int,
)

object MethodParametersAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val parametersCount = context.reader.readU1()
        return MethodParametersAttribute(
            nameIndex = context.nameIndex,
            parameters = List(parametersCount) { index ->
                parseParameter(context, "${context.ownerPath}.parameters[$index]")
            },
        )
    }

    private fun parseParameter(
        context: AttributeParseContext,
        ownerPath: String,
    ): MethodParameter =
        MethodParameter(
            nameIndex = readOptionalUtf8Index(context, "$ownerPath.name_index"),
            accessFlags = context.reader.readU2(),
        )

    private fun readOptionalUtf8Index(
        context: AttributeParseContext,
        role: String,
    ): ConstantPoolIndex? {
        val rawIndex = context.reader.readU2()
        if (rawIndex == 0) {
            return null
        }
        val index = ConstantPoolIndex(rawIndex)
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
        ClassNameValidator.validateUnqualifiedName(index, role, entry.value)
        return index
    }
}
