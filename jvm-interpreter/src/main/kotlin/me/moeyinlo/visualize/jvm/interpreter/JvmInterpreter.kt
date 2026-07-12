package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmOperandStack

data class JvmExecutionResult(
    val operandStack: JvmOperandStack,
)

class JvmUnsupportedInstructionException(message: String) : IllegalStateException(message)

object JvmInterpreter {
    fun execute(code: ByteArray, maxStack: Int): JvmExecutionResult {
        val operandStack = JvmOperandStack(maxStack = maxStack)
        BytecodeDecoder.decode(code).forEach { instruction ->
            executeInstruction(instruction, operandStack)
        }
        return JvmExecutionResult(operandStack = operandStack)
    }

    private fun executeInstruction(instruction: DecodedInstruction, operandStack: JvmOperandStack) {
        when (instruction.metadata.opcode) {
            0x00 -> Unit
            0x01 -> operandStack.push(JvmNullValue)
            in 0x02..0x08 -> operandStack.push(JvmIntValue(instruction.metadata.opcode - 0x03))
            in 0x09..0x0A -> operandStack.push(JvmLongValue((instruction.metadata.opcode - 0x09).toLong()))
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported instruction ${instruction.metadata.mnemonic} " +
                    "(${instruction.metadata.opcode.hexByte()}) at offset ${instruction.offset}",
            )
        }
    }

    private fun Int.hexByte(): String = "0x${toString(16).padStart(2, '0')}"
}
