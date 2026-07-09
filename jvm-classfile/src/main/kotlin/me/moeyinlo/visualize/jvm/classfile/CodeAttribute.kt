package me.moeyinlo.visualize.jvm.classfile

class CodeAttribute(
    override val nameIndex: ConstantPoolIndex,
    val maxStack: Int,
    val maxLocals: Int,
    code: ByteArray,
) : AttributeInfo {
    private val codeBytes = code.copyOf()

    val code: ByteArray
        get() = codeBytes.copyOf()
}

object CodeAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val maxStack = context.reader.readU2()
        val maxLocals = context.reader.readU2()
        val codeLength = context.reader.readU4()
        if (codeLength == 0L) {
            throw ClassFileFormatException(
                "Invalid Code code_length=0 at ${context.ownerPath}: must be greater than zero",
            )
        }
        if (codeLength >= 65_536L) {
            throw ClassFileFormatException(
                "Invalid Code code_length=$codeLength at ${context.ownerPath}: must be less than 65536",
            )
        }

        val code = context.reader.readSlice(codeLength.toInt())
        val exceptionTableLength = context.reader.readU2()
        if (exceptionTableLength != 0) {
            throw ClassFileFormatException(
                "Unsupported Code exception_table_length=$exceptionTableLength at ${context.ownerPath}",
            )
        }
        val attributesCount = context.reader.readU2()
        if (attributesCount != 0) {
            throw ClassFileFormatException(
                "Unsupported Code attributes_count=$attributesCount at ${context.ownerPath}",
            )
        }

        return CodeAttribute(
            nameIndex = context.nameIndex,
            maxStack = maxStack,
            maxLocals = maxLocals,
            code = code,
        )
    }
}
