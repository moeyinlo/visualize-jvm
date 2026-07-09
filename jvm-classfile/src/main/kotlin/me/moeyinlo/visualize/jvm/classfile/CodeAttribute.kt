package me.moeyinlo.visualize.jvm.classfile

class CodeAttribute(
    override val nameIndex: ConstantPoolIndex,
    val maxStack: Int,
    val maxLocals: Int,
    code: ByteArray,
    val exceptionTable: List<CodeExceptionHandler> = emptyList(),
) : AttributeInfo {
    private val codeBytes = code.copyOf()

    val code: ByteArray
        get() = codeBytes.copyOf()
}

data class CodeExceptionHandler(
    val startPc: Int,
    val endPc: Int,
    val handlerPc: Int,
    val catchType: ConstantPoolIndex?,
)

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
        val exceptionTable = parseExceptionTable(context, code.size)
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
            exceptionTable = exceptionTable,
        )
    }

    private fun parseExceptionTable(
        context: AttributeParseContext,
        codeLength: Int,
    ): List<CodeExceptionHandler> {
        val exceptionTableLength = context.reader.readU2()
        return List(exceptionTableLength) { index ->
            parseExceptionHandler(context, codeLength, index)
        }
    }

    private fun parseExceptionHandler(
        context: AttributeParseContext,
        codeLength: Int,
        index: Int,
    ): CodeExceptionHandler {
        val ownerPath = "${context.ownerPath}.exception_table[$index]"
        val startPc = context.reader.readU2()
        val endPc = context.reader.readU2()
        val handlerPc = context.reader.readU2()
        val catchTypeIndex = context.reader.readU2()
        validateHandlerRange(ownerPath, codeLength, startPc, endPc, handlerPc)
        val catchType = validateCatchType(context, ownerPath, catchTypeIndex)
        return CodeExceptionHandler(
            startPc = startPc,
            endPc = endPc,
            handlerPc = handlerPc,
            catchType = catchType,
        )
    }

    private fun validateHandlerRange(
        ownerPath: String,
        codeLength: Int,
        startPc: Int,
        endPc: Int,
        handlerPc: Int,
    ) {
        if (startPc >= codeLength) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.start_pc=$startPc: must be less than code_length=$codeLength",
            )
        }
        if (endPc > codeLength) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.end_pc=$endPc: must be at most code_length=$codeLength",
            )
        }
        if (startPc >= endPc) {
            throw ClassFileFormatException(
                "Invalid $ownerPath range: start_pc=$startPc must be less than end_pc=$endPc",
            )
        }
        if (handlerPc >= codeLength) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.handler_pc=$handlerPc: must be less than code_length=$codeLength",
            )
        }
    }

    private fun validateCatchType(
        context: AttributeParseContext,
        ownerPath: String,
        catchTypeIndex: Int,
    ): ConstantPoolIndex? {
        if (catchTypeIndex == 0) {
            return null
        }
        val index = ConstantPoolIndex(catchTypeIndex)
        val entry = try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.catch_type=$index: ${exception.message}",
            )
        }
        if (entry !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.catch_type=$index: expected CONSTANT_Class_info " +
                    "but found ${entry.javaClass.simpleName}",
            )
        }
        return index
    }
}
