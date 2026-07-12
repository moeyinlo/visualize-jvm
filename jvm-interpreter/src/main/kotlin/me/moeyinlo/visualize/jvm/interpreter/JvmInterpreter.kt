package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmOperandStack

data class JvmExecutionResult(
    val operandStack: JvmOperandStack,
)

class JvmUnsupportedInstructionException(message: String) : IllegalStateException(message)

object JvmInterpreter {
    fun execute(
        code: ByteArray,
        maxStack: Int,
        constantPool: ConstantPool = ConstantPool.fromEntries(emptyList()),
        heap: JvmHeap = JvmHeap(),
    ): JvmExecutionResult {
        val operandStack = JvmOperandStack(maxStack = maxStack)
        BytecodeDecoder.decode(code).forEach { instruction ->
            executeInstruction(instruction, operandStack, constantPool, heap)
        }
        return JvmExecutionResult(operandStack = operandStack)
    }

    private fun executeInstruction(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
    ) {
        when (instruction.metadata.opcode) {
            0x00 -> Unit
            0x01 -> operandStack.push(JvmNullValue)
            in 0x02..0x08 -> operandStack.push(JvmIntValue(instruction.metadata.opcode - 0x03))
            in 0x09..0x0A -> operandStack.push(JvmLongValue((instruction.metadata.opcode - 0x09).toLong()))
            in 0x0B..0x0D -> operandStack.push(JvmFloatValue((instruction.metadata.opcode - 0x0B).toFloat()))
            in 0x0E..0x0F -> operandStack.push(JvmDoubleValue((instruction.metadata.opcode - 0x0E).toDouble()))
            0x10 -> operandStack.push(JvmIntValue(instruction.operands[0].toByte().toInt()))
            0x11 -> operandStack.push(
                JvmIntValue(((instruction.operands[0] shl 8) or instruction.operands[1]).toShort().toInt()),
            )
            0x12,
            0x13,
            -> executeLdc(instruction, operandStack, constantPool, heap)
            0x14 -> executeLdc2(instruction, operandStack, constantPool)
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported instruction ${instruction.metadata.mnemonic} " +
                    "(${instruction.metadata.opcode.hexByte()}) at offset ${instruction.offset}",
            )
        }
    }

    private fun executeLdc(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
    ) {
        val index = instruction.constantPoolIndex()
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw JvmUnsupportedInstructionException(
                "Invalid ldc constant_pool index $index at offset ${instruction.offset}: ${exception.message}",
            )
        }
        when (entry) {
            is ConstantFloatEntry -> operandStack.push(JvmFloatValue(entry.value))
            is ConstantIntegerEntry -> operandStack.push(JvmIntValue(entry.value))
            is ConstantStringEntry -> {
                val stringEntry = try {
                    constantPool[entry.stringIndex]
                } catch (exception: ConstantPoolFormatException) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_String string_index ${entry.stringIndex} " +
                            "at offset ${instruction.offset}: ${exception.message}",
                    )
                }
                if (stringEntry !is ConstantUtf8Entry) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_String string_index ${entry.stringIndex} at offset " +
                            "${instruction.offset}: expected ConstantUtf8Entry but was " +
                            stringEntry.javaClass.simpleName,
                    )
                }
                operandStack.push(heap.internString(stringEntry.value))
            }
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported ldc constant ${entry.javaClass.simpleName} at offset ${instruction.offset}",
            )
        }
    }

    private fun executeLdc2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
    ) {
        val index = instruction.constantPoolIndex()
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw JvmUnsupportedInstructionException(
                "Invalid ldc2_w constant_pool index $index at offset ${instruction.offset}: ${exception.message}",
            )
        }
        when (entry) {
            is ConstantDoubleEntry -> operandStack.push(JvmDoubleValue(entry.value))
            is ConstantLongEntry -> operandStack.push(JvmLongValue(entry.value))
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported ldc2_w constant ${entry.javaClass.simpleName} at offset ${instruction.offset}",
            )
        }
    }

    private fun DecodedInstruction.constantPoolIndex(): ConstantPoolIndex =
        when (metadata.opcode) {
            0x12 -> ConstantPoolIndex(operands[0])
            0x13,
            0x14,
            -> ConstantPoolIndex((operands[0] shl 8) or operands[1])
            else -> error("Instruction ${metadata.mnemonic} does not use a constant_pool index")
        }

    private fun Int.hexByte(): String = "0x${toString(16).padStart(2, '0')}"
}
