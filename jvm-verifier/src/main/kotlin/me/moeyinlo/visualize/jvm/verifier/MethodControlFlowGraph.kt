package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.CodeAttribute

data class MethodControlFlowGraph(
    val instructionOffsets: Set<Int>,
    val edges: Set<ControlFlowEdge>,
)

data class ControlFlowEdge(
    val sourceOffset: Int,
    val targetOffset: Int,
    val kind: ControlFlowEdgeKind,
)

enum class ControlFlowEdgeKind {
    FallThrough,
    Branch,
    ExceptionHandler,
}

class ControlFlowGraphException(message: String) : RuntimeException(message)

object MethodControlFlowGraphBuilder {
    private val conditionalBranches = (0x99..0xA6).toSet() + setOf(0xC6, 0xC7)
    private val terminalOpcodes = (0xAC..0xB1).toSet() + setOf(0xBF)

    fun build(code: CodeAttribute): MethodControlFlowGraph {
        val codeBytes = code.code
        val instructions = decodeInstructions(codeBytes)
        val instructionOffsets = instructions.mapTo(linkedSetOf()) { it.offset }
        validateExceptionHandlers(code, codeBytes.size, instructionOffsets)
        val edges = linkedSetOf<ControlFlowEdge>()

        instructions.forEachIndexed { index, instruction ->
            val next = instructions.getOrNull(index + 1)
            instruction.branchTargets.forEach { branchTarget ->
                if (branchTarget !in instructionOffsets) {
                    throw ControlFlowGraphException(
                        "Invalid branch target $branchTarget from instruction ${instruction.offset}",
                    )
                }
                edges += ControlFlowEdge(instruction.offset, branchTarget, ControlFlowEdgeKind.Branch)
            }
            if (next != null && instruction.hasFallThrough) {
                edges += ControlFlowEdge(instruction.offset, next.offset, ControlFlowEdgeKind.FallThrough)
            }
        }

        code.exceptionTable.forEach { handler ->
            instructionOffsets
                .filter { offset -> offset >= handler.startPc && offset < handler.endPc }
                .forEach { offset ->
                    edges += ControlFlowEdge(offset, handler.handlerPc, ControlFlowEdgeKind.ExceptionHandler)
                }
        }

        return MethodControlFlowGraph(
            instructionOffsets = instructionOffsets,
            edges = edges,
        )
    }

    private fun decodeInstructions(code: ByteArray): List<DecodedInstruction> {
        val instructions = mutableListOf<DecodedInstruction>()
        var offset = 0
        while (offset < code.size) {
            val opcode = code.u1(offset)
            val instruction = when (opcode) {
                0xAA -> code.decodeTableSwitch(offset)
                0xAB -> code.decodeLookupSwitch(offset)
                else -> code.decodeFixedInstruction(offset, opcode)
            }
            instructions += instruction
            offset += instruction.length
        }
        return instructions
    }

    private fun ByteArray.decodeFixedInstruction(
        offset: Int,
        opcode: Int,
    ): DecodedInstruction {
        val length = fixedLength(opcode)
        if (offset + length > size) {
            throw ControlFlowGraphException(
                "Instruction at $offset length $length exceeds code_length=$size",
            )
        }
        val branchTargets = when (opcode) {
            in conditionalBranches,
            0xA7,
            -> setOf(offset + s2(offset + 1))
            0xC8 -> setOf(offset + s4(offset + 1))
            else -> emptySet()
        }
        return DecodedInstruction(
            offset = offset,
            opcode = opcode,
            length = length,
            branchTargets = branchTargets,
        )
    }

    private fun ByteArray.decodeTableSwitch(offset: Int): DecodedInstruction {
        val padding = switchPadding(offset)
        val defaultOffset = offset + 1 + padding
        if (defaultOffset + 12 > size) {
            throw ControlFlowGraphException(
                "tableswitch at $offset header exceeds code_length=$size",
            )
        }

        val defaultTarget = offset + s4(defaultOffset)
        val low = s4(defaultOffset + 4)
        val high = s4(defaultOffset + 8)
        if (low > high) {
            throw ControlFlowGraphException(
                "tableswitch at $offset has low $low greater than high $high",
            )
        }

        val entryCount = high.toLong() - low.toLong() + 1L
        val length = 1L + padding.toLong() + 12L + entryCount * 4L
        if (length > Int.MAX_VALUE || offset.toLong() + length > size.toLong()) {
            throw ControlFlowGraphException(
                "tableswitch at $offset length $length exceeds code_length=$size",
            )
        }

        val jumpOffsets = defaultOffset + 12
        val targets = linkedSetOf(defaultTarget)
        repeat(entryCount.toInt()) { index ->
            targets += offset + s4(jumpOffsets + index * 4)
        }
        return DecodedInstruction(
            offset = offset,
            opcode = 0xAA,
            length = length.toInt(),
            branchTargets = targets,
        )
    }

    private fun ByteArray.decodeLookupSwitch(offset: Int): DecodedInstruction {
        val padding = switchPadding(offset)
        val defaultOffset = offset + 1 + padding
        if (defaultOffset + 8 > size) {
            throw ControlFlowGraphException(
                "lookupswitch at $offset header exceeds code_length=$size",
            )
        }

        val defaultTarget = offset + s4(defaultOffset)
        val pairCount = s4(defaultOffset + 4)
        if (pairCount < 0) {
            throw ControlFlowGraphException(
                "lookupswitch at $offset has negative npairs=$pairCount",
            )
        }

        val length = 1L + padding.toLong() + 8L + pairCount.toLong() * 8L
        if (length > Int.MAX_VALUE || offset.toLong() + length > size.toLong()) {
            throw ControlFlowGraphException(
                "lookupswitch at $offset length $length exceeds code_length=$size",
            )
        }

        val pairsOffset = defaultOffset + 8
        val targets = linkedSetOf(defaultTarget)
        repeat(pairCount) { index ->
            val pairOffset = pairsOffset + index * 8
            targets += offset + s4(pairOffset + 4)
        }
        return DecodedInstruction(
            offset = offset,
            opcode = 0xAB,
            length = length.toInt(),
            branchTargets = targets,
        )
    }

    private fun validateExceptionHandlers(
        code: CodeAttribute,
        codeLength: Int,
        instructionOffsets: Set<Int>,
    ) {
        code.exceptionTable.forEach { handler ->
            if (handler.startPc !in instructionOffsets) {
                throw ControlFlowGraphException("Exception handler start_pc ${handler.startPc} is not an instruction offset")
            }
            if (handler.endPc != codeLength && handler.endPc !in instructionOffsets) {
                throw ControlFlowGraphException("Exception handler end_pc ${handler.endPc} is not code_length or an instruction offset")
            }
            if (handler.startPc >= handler.endPc) {
                throw ControlFlowGraphException("Exception handler start_pc ${handler.startPc} must be less than end_pc ${handler.endPc}")
            }
            if (handler.handlerPc !in instructionOffsets) {
                throw ControlFlowGraphException("Exception handler handler_pc ${handler.handlerPc} is not an instruction offset")
            }
        }
    }

    private fun fixedLength(opcode: Int): Int =
        when {
            opcode in 0x00..0x0F -> 1
            opcode == 0x10 -> 2
            opcode == 0x11 -> 3
            opcode == 0x12 -> 2
            opcode in 0x13..0x14 -> 3
            opcode in 0x15..0x19 -> 2
            opcode in 0x1A..0x83 -> 1
            opcode == 0x84 -> 3
            opcode in 0x85..0x98 -> 1
            opcode in 0x99..0xA8 -> 3
            opcode == 0xA9 -> 2
            opcode in 0xAC..0xB1 -> 1
            opcode in 0xB2..0xB8 -> 3
            opcode in 0xB9..0xBA -> 5
            opcode == 0xBB -> 3
            opcode == 0xBC -> 2
            opcode == 0xBD -> 3
            opcode in 0xBE..0xBF -> 1
            opcode in 0xC0..0xC1 -> 3
            opcode in 0xC2..0xC3 -> 1
            opcode == 0xC5 -> 4
            opcode in 0xC6..0xC7 -> 3
            opcode in 0xC8..0xC9 -> 5
            opcode == 0xAA || opcode == 0xAB || opcode == 0xC4 -> throw ControlFlowGraphException(
                "Variable-length opcode 0x${opcode.toString(16)} is not supported by this CFG slice",
            )
            else -> throw ControlFlowGraphException("Invalid opcode 0x${opcode.toString(16)} at CFG decode")
        }

    private val DecodedInstruction.hasFallThrough: Boolean
        get() =
            opcode !in terminalOpcodes &&
                opcode != 0xA7 &&
                opcode != 0xAA &&
                opcode != 0xAB &&
                opcode != 0xC8

    private fun switchPadding(offset: Int): Int = (4 - ((offset + 1) % 4)) % 4

    private fun ByteArray.u1(offset: Int): Int = this[offset].toInt() and 0xFF

    private fun ByteArray.s2(offset: Int): Int =
        ((u1(offset) shl 8) or u1(offset + 1)).toShort().toInt()

    private fun ByteArray.s4(offset: Int): Int =
        (u1(offset) shl 24) or (u1(offset + 1) shl 16) or (u1(offset + 2) shl 8) or u1(offset + 3)

    private data class DecodedInstruction(
        val offset: Int,
        val opcode: Int,
        val length: Int,
        val branchTargets: Set<Int>,
    )
}
