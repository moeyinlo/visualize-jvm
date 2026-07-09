package me.moeyinlo.visualize.jvm.classfile

class CodeAttribute(
    override val nameIndex: ConstantPoolIndex,
    val maxStack: Int,
    val maxLocals: Int,
    code: ByteArray,
    val exceptionTable: List<CodeExceptionHandler> = emptyList(),
    val attributes: List<AttributeInfo> = emptyList(),
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
        val instructionLayout = CodeInstructionValidator.validate(code, context.ownerPath)
        val exceptionTable = parseExceptionTable(context, code.size, instructionLayout)
        val attributes = AttributeInfoParser.parseAttributes(
            reader = context.reader,
            constantPool = context.constantPool,
            registry = context.registry,
            ownerPath = context.ownerPath,
        )

        return CodeAttribute(
            nameIndex = context.nameIndex,
            maxStack = maxStack,
            maxLocals = maxLocals,
            code = code,
            exceptionTable = exceptionTable,
            attributes = attributes,
        )
    }

    private fun parseExceptionTable(
        context: AttributeParseContext,
        codeLength: Int,
        instructionLayout: CodeInstructionLayout,
    ): List<CodeExceptionHandler> {
        val exceptionTableLength = context.reader.readU2()
        return List(exceptionTableLength) { index ->
            parseExceptionHandler(context, codeLength, instructionLayout, index)
        }
    }

    private fun parseExceptionHandler(
        context: AttributeParseContext,
        codeLength: Int,
        instructionLayout: CodeInstructionLayout,
        index: Int,
    ): CodeExceptionHandler {
        val ownerPath = "${context.ownerPath}.exception_table[$index]"
        val startPc = context.reader.readU2()
        val endPc = context.reader.readU2()
        val handlerPc = context.reader.readU2()
        val catchTypeIndex = context.reader.readU2()
        validateHandlerRange(ownerPath, codeLength, instructionLayout, startPc, endPc, handlerPc)
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
        instructionLayout: CodeInstructionLayout,
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
        if (startPc !in instructionLayout.instructionOffsets) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.start_pc=$startPc: must point to the opcode of an instruction",
            )
        }
        if (endPc != codeLength && endPc !in instructionLayout.instructionOffsets) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.end_pc=$endPc: must be code_length=$codeLength " +
                    "or point to the opcode of an instruction",
            )
        }
        if (handlerPc >= codeLength) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.handler_pc=$handlerPc: must be less than code_length=$codeLength",
            )
        }
        if (handlerPc !in instructionLayout.instructionOffsets) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.handler_pc=$handlerPc: must point to the opcode of an instruction",
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

private object CodeInstructionValidator {
    private val fixedInstructionLengths: IntArray = IntArray(256) { -1 }.also { lengths ->
        fun length(opcode: Int, length: Int) {
            lengths[opcode] = length
        }

        for (opcode in 0x00..0x0F) length(opcode, 1)
        length(0x10, 2)
        length(0x11, 3)
        length(0x12, 2)
        length(0x13, 3)
        length(0x14, 3)
        for (opcode in 0x15..0x19) length(opcode, 2)
        for (opcode in 0x1A..0x35) length(opcode, 1)
        for (opcode in 0x36..0x3A) length(opcode, 2)
        for (opcode in 0x3B..0x83) length(opcode, 1)
        length(0x84, 3)
        for (opcode in 0x85..0x98) length(opcode, 1)
        for (opcode in 0x99..0xA8) length(opcode, 3)
        length(0xA9, 2)
        for (opcode in 0xAC..0xB1) length(opcode, 1)
        for (opcode in 0xB2..0xB8) length(opcode, 3)
        length(0xB9, 5)
        length(0xBA, 5)
        length(0xBB, 3)
        length(0xBC, 2)
        length(0xBD, 3)
        length(0xBE, 1)
        length(0xBF, 1)
        length(0xC0, 3)
        length(0xC1, 3)
        length(0xC2, 1)
        length(0xC3, 1)
        length(0xC5, 4)
        length(0xC6, 3)
        length(0xC7, 3)
        length(0xC8, 5)
        length(0xC9, 5)
    }

    private val wideTwoByteIndexOpcodes = setOf(0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3A, 0xA9)

    fun validate(
        code: ByteArray,
        ownerPath: String,
    ): CodeInstructionLayout {
        val instructionOffsets = mutableSetOf<Int>()
        val modifiedOpcodeOffsets = mutableSetOf<Int>()
        val branchTargets = mutableListOf<BranchTarget>()

        var pc = 0
        while (pc < code.size) {
            instructionOffsets += pc
            val opcode = code.u1(pc)
            val length = instructionLength(code, pc, ownerPath, branchTargets, modifiedOpcodeOffsets)
            if (pc + length > code.size) {
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[$pc] ${mnemonic(opcode)}: " +
                        "truncated instruction length=$length exceeds code_length=${code.size}",
                )
            }
            pc += length
        }

        branchTargets.forEach { target ->
            if (target.offset !in instructionOffsets) {
                val reason = if (target.offset in modifiedOpcodeOffsets) {
                    "points to the opcode operand modified by wide"
                } else {
                    "does not point to an instruction opcode"
                }
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[${target.sourcePc}] ${target.kind} branch target=${target.offset}: $reason",
                )
            }
        }
        return CodeInstructionLayout(instructionOffsets.toSet())
    }

    private fun instructionLength(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        branchTargets: MutableList<BranchTarget>,
        modifiedOpcodeOffsets: MutableSet<Int>,
    ): Int {
        val opcode = code.u1(pc)
        if (opcode in 0xCA..0xFF) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc]: reserved opcode 0x${opcode.toHex()} must not appear in code arrays",
            )
        }
        return when (opcode) {
            0xAA -> parseTableSwitch(code, pc, ownerPath, branchTargets)
            0xAB -> parseLookupSwitch(code, pc, ownerPath, branchTargets)
            0xC4 -> parseWide(code, pc, ownerPath, modifiedOpcodeOffsets)
            in 0x99..0xA8, 0xC6, 0xC7 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                branchTargets += BranchTarget(
                    sourcePc = pc,
                    offset = pc + code.s2(pc + 1),
                    kind = mnemonic(opcode),
                )
                3
            }
            0xC8, 0xC9 -> {
                ensureAvailable(code, pc, 5, ownerPath, mnemonic(opcode))
                branchTargets += BranchTarget(
                    sourcePc = pc,
                    offset = pc + code.s4(pc + 1),
                    kind = mnemonic(opcode),
                )
                5
            }
            else -> {
                val length = fixedInstructionLengths[opcode]
                if (length < 0) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc]: undocumented opcode 0x${opcode.toHex()} must not appear in code arrays",
                    )
                }
                ensureAvailable(code, pc, length, ownerPath, mnemonic(opcode))
                length
            }
        }
    }

    private fun parseTableSwitch(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        branchTargets: MutableList<BranchTarget>,
    ): Int {
        val padding = switchPadding(pc)
        val operands = pc + 1 + padding
        ensureAvailable(code, pc, 1 + padding + 12, ownerPath, "tableswitch")
        val defaultOffset = code.s4(operands)
        val low = code.s4(operands + 4)
        val high = code.s4(operands + 8)
        if (low > high) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] tableswitch: low=$low must be less than or equal to high=$high",
            )
        }
        val entryCount = high.toLong() - low.toLong() + 1L
        if (entryCount > Int.MAX_VALUE) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] tableswitch: jump table entry count $entryCount is too large",
            )
        }
        val length = switchLength(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = "tableswitch",
            value = 1L + padding + 12L + entryCount * 4L,
        )
        ensureAvailable(code, pc, length, ownerPath, "tableswitch")
        branchTargets += BranchTarget(pc, pc + defaultOffset, "tableswitch default")
        repeat(entryCount.toInt()) { index ->
            branchTargets += BranchTarget(
                sourcePc = pc,
                offset = pc + code.s4(operands + 12 + index * 4),
                kind = "tableswitch",
            )
        }
        return length
    }

    private fun parseLookupSwitch(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        branchTargets: MutableList<BranchTarget>,
    ): Int {
        val padding = switchPadding(pc)
        val operands = pc + 1 + padding
        ensureAvailable(code, pc, 1 + padding + 8, ownerPath, "lookupswitch")
        val defaultOffset = code.s4(operands)
        val pairs = code.s4(operands + 4)
        if (pairs < 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] lookupswitch: npairs=$pairs must be non-negative",
            )
        }
        val length = switchLength(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = "lookupswitch",
            value = 1L + padding + 8L + pairs.toLong() * 8L,
        )
        ensureAvailable(code, pc, length, ownerPath, "lookupswitch")
        var previousMatch: Int? = null
        branchTargets += BranchTarget(pc, pc + defaultOffset, "lookupswitch default")
        repeat(pairs) { index ->
            val pairOffset = operands + 8 + index * 8
            val match = code.s4(pairOffset)
            if (previousMatch?.let { match <= it } == true) {
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[$pc] lookupswitch: match-offset pairs must be sorted in increasing order",
                )
            }
            previousMatch = match
            branchTargets += BranchTarget(
                sourcePc = pc,
                offset = pc + code.s4(pairOffset + 4),
                kind = "lookupswitch",
            )
        }
        return length
    }

    private fun parseWide(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        modifiedOpcodeOffsets: MutableSet<Int>,
    ): Int {
        ensureAvailable(code, pc, 2, ownerPath, "wide")
        val modifiedOpcode = code.u1(pc + 1)
        modifiedOpcodeOffsets += pc + 1
        return when (modifiedOpcode) {
            in wideTwoByteIndexOpcodes -> {
                ensureAvailable(code, pc, 4, ownerPath, "wide")
                4
            }
            0x84 -> {
                ensureAvailable(code, pc, 6, ownerPath, "wide")
                6
            }
            else -> throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] wide: unsupported modified opcode 0x${modifiedOpcode.toHex()}",
            )
        }
    }

    private fun switchLength(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        value: Long,
    ): Int {
        if (value > Int.MAX_VALUE) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic: instruction length $value is too large",
            )
        }
        return value.toInt()
    }

    private fun ensureAvailable(
        code: ByteArray,
        pc: Int,
        length: Int,
        ownerPath: String,
        mnemonic: String,
    ) {
        if (pc + length > code.size) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic: " +
                    "truncated instruction length=$length exceeds code_length=${code.size}",
            )
        }
    }

    private fun switchPadding(pc: Int): Int =
        (4 - ((pc + 1) and 0x03)) and 0x03

    private fun mnemonic(opcode: Int): String =
        when (opcode) {
            0x11 -> "sipush"
            0xAA -> "tableswitch"
            0xAB -> "lookupswitch"
            0xC4 -> "wide"
            else -> "opcode 0x${opcode.toHex()}"
        }

    private fun ByteArray.u1(offset: Int): Int =
        this[offset].toInt() and 0xFF

    private fun ByteArray.s2(offset: Int): Int =
        (u1(offset).toShortish() shl 8) or u1(offset + 1)

    private fun ByteArray.s4(offset: Int): Int =
        (u1(offset) shl 24) or (u1(offset + 1) shl 16) or (u1(offset + 2) shl 8) or u1(offset + 3)

    private fun Int.toShortish(): Int =
        if (this and 0x80 != 0) this or 0xFFFFFF00.toInt() else this

    private fun Int.toHex(): String =
        toString(16).padStart(2, '0')

    private data class BranchTarget(
        val sourcePc: Int,
        val offset: Int,
        val kind: String,
    )
}

private data class CodeInstructionLayout(
    val instructionOffsets: Set<Int>,
)
