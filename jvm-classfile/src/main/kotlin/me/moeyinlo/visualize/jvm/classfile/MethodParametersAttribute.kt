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
    private const val Java8MajorVersion = 52
    private const val AccFinal = 0x0010
    private const val AccSynthetic = 0x1000
    private const val AccMandated = 0x8000
    private const val AllowedAccessFlags = AccFinal or AccSynthetic or AccMandated

    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.majorVersion < Java8MajorVersion) {
            throw ClassFileFormatException(
                "Invalid MethodParameters attribute at ${context.ownerPath}: " +
                    "major_version=${context.majorVersion} must be at least $Java8MajorVersion",
            )
        }
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
    ): MethodParameter {
        val nameIndex = readOptionalUtf8Index(context, "$ownerPath.name_index")
        val accessFlags = context.reader.readU2()
        val invalidBits = accessFlags and AllowedAccessFlags.inv() and 0xFFFF
        if (invalidBits != 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.access_flags=0x${accessFlags.toU2Hex()}: " +
                    "unknown flag bits 0x${invalidBits.toU2Hex()}",
            )
        }
        return MethodParameter(
            nameIndex = nameIndex,
            accessFlags = accessFlags,
        )
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

    private fun Int.toU2Hex(): String = toString(16).uppercase().padStart(4, '0')
}
