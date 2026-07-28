package me.moeyinlo.visualize.jvm.classfile

data class SignatureAttribute(
    override val nameIndex: ConstantPoolIndex,
    val signatureIndex: ConstantPoolIndex,
    val signature: String,
) : AttributeInfo

object SignatureAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        if (context.majorVersion < Java5MajorVersion) {
            throw ClassFileFormatException(
                "Invalid Signature attribute at ${context.ownerPath}: " +
                    "major_version=${context.majorVersion} must be at least $Java5MajorVersion",
            )
        }
        if (context.length != 2) {
            throw ClassFileFormatException(
                "Invalid Signature attribute_length=${context.length} at ${context.ownerPath}: expected 2",
            )
        }
        val signatureIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "${context.ownerPath}.signature_index",
        )
        val entry = try {
            context.constantPool[signatureIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.signature_index=$signatureIndex: ${exception.message}",
            )
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid ${context.ownerPath}.signature_index=$signatureIndex: " +
                    "expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        SignatureGrammarValidator.validateForOwner(context.ownerPath, entry.value)
        return SignatureAttribute(
            nameIndex = context.nameIndex,
            signatureIndex = signatureIndex,
            signature = entry.value,
        )
    }
}

private const val Java5MajorVersion = 49
