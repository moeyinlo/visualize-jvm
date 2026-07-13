package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFieldRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInterfaceMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.MethodHandleReferenceKind
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLocalVariables
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMethodHandleReferenceKind
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmOperandStack
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue

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
        localVariables: JvmLocalVariables = JvmLocalVariables(maxLocals = 0),
    ): JvmExecutionResult {
        val operandStack = JvmOperandStack(maxStack = maxStack)
        BytecodeDecoder.decode(code).forEach { instruction ->
            executeInstruction(instruction, operandStack, constantPool, heap, localVariables)
        }
        return JvmExecutionResult(operandStack = operandStack)
    }

    private fun executeInstruction(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        localVariables: JvmLocalVariables,
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
            0x15,
            in 0x1A..0x1D,
            -> executeIntLoad(instruction, operandStack, localVariables)
            0x16,
            in 0x1E..0x21,
            -> executeLongLoad(instruction, operandStack, localVariables)
            0x17,
            in 0x22..0x25,
            -> executeFloatLoad(instruction, operandStack, localVariables)
            0x18,
            in 0x26..0x29,
            -> executeDoubleLoad(instruction, operandStack, localVariables)
            0x19,
            in 0x2A..0x2D,
            -> executeReferenceLoad(instruction, operandStack, localVariables)
            0xC4 -> executeWide(instruction, operandStack, localVariables)
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported instruction ${instruction.metadata.mnemonic} " +
                    "(${instruction.metadata.opcode.hexByte()}) at offset ${instruction.offset}",
            )
        }
    }

    private fun executeIntLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeLongLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeFloatLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeDoubleLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeReferenceLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeWide(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        when (val modifiedOpcode = instruction.modifiedWideOpcode()) {
            0x15 -> executeIntLoad(instruction, operandStack, localVariables)
            0x16 -> executeLongLoad(instruction, operandStack, localVariables)
            0x17 -> executeFloatLoad(instruction, operandStack, localVariables)
            0x18 -> executeDoubleLoad(instruction, operandStack, localVariables)
            0x19 -> executeReferenceLoad(instruction, operandStack, localVariables)
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported wide-modified instruction ${OpcodeTable.metadata(modifiedOpcode).mnemonic} " +
                    "(${modifiedOpcode.hexByte()}) at offset ${instruction.offset}",
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
            is ConstantClassEntry -> {
                val nameEntry = try {
                    constantPool[entry.nameIndex]
                } catch (exception: ConstantPoolFormatException) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_Class name_index ${entry.nameIndex} " +
                            "at offset ${instruction.offset}: ${exception.message}",
                    )
                }
                if (nameEntry !is ConstantUtf8Entry) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_Class name_index ${entry.nameIndex} at offset " +
                            "${instruction.offset}: expected ConstantUtf8Entry but was " +
                            nameEntry.javaClass.simpleName,
                    )
                }
                operandStack.push(heap.internClassMirror(nameEntry.value))
            }
            is ConstantFloatEntry -> operandStack.push(JvmFloatValue(entry.value))
            is ConstantIntegerEntry -> operandStack.push(JvmIntValue(entry.value))
            is ConstantMethodHandleEntry -> {
                val referencedEntry = try {
                    constantPool[entry.referenceIndex]
                } catch (exception: ConstantPoolFormatException) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_MethodHandle reference_index ${entry.referenceIndex} " +
                            "at offset ${instruction.offset}: ${exception.message}",
                    )
                }
                if (!entry.referenceKind.matches(referencedEntry)) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_MethodHandle reference_index ${entry.referenceIndex} at offset " +
                            "${instruction.offset}: reference_kind ${entry.referenceKind} cannot target " +
                            referencedEntry.javaClass.simpleName,
                    )
                }
                operandStack.push(
                    heap.internMethodHandle(
                        referenceKind = entry.referenceKind.toRuntimeReferenceKind(),
                        referenceIndex = entry.referenceIndex.value,
                    ),
                )
            }
            is ConstantMethodTypeEntry -> {
                val descriptorEntry = try {
                    constantPool[entry.descriptorIndex]
                } catch (exception: ConstantPoolFormatException) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_MethodType descriptor_index ${entry.descriptorIndex} " +
                            "at offset ${instruction.offset}: ${exception.message}",
                    )
                }
                if (descriptorEntry !is ConstantUtf8Entry) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_MethodType descriptor_index ${entry.descriptorIndex} at offset " +
                            "${instruction.offset}: expected ConstantUtf8Entry but was " +
                            descriptorEntry.javaClass.simpleName,
                    )
                }
                operandStack.push(heap.internMethodType(descriptorEntry.value))
            }
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

    private fun DecodedInstruction.localVariableIndex(): Int =
        when (metadata.opcode) {
            0x15,
            0x16,
            0x17,
            0x18,
            0x19,
            -> operands[0]
            0xC4 -> (operands[1] shl 8) or operands[2]
            in 0x1A..0x1D -> metadata.opcode - 0x1A
            in 0x1E..0x21 -> metadata.opcode - 0x1E
            in 0x22..0x25 -> metadata.opcode - 0x22
            in 0x26..0x29 -> metadata.opcode - 0x26
            in 0x2A..0x2D -> metadata.opcode - 0x2A
            else -> error("Instruction ${metadata.mnemonic} does not use a local variable index")
        }

    private fun DecodedInstruction.modifiedWideOpcode(): Int = operands[0]

    private fun Int.hexByte(): String = "0x${toString(16).padStart(2, '0')}"

    private fun MethodHandleReferenceKind.matches(entry: ConstantPoolEntry): Boolean =
        when (this) {
            MethodHandleReferenceKind.GetField,
            MethodHandleReferenceKind.GetStatic,
            MethodHandleReferenceKind.PutField,
            MethodHandleReferenceKind.PutStatic,
            -> entry is ConstantFieldRefEntry

            MethodHandleReferenceKind.InvokeVirtual,
            MethodHandleReferenceKind.NewInvokeSpecial,
            -> entry is ConstantMethodRefEntry

            MethodHandleReferenceKind.InvokeStatic,
            MethodHandleReferenceKind.InvokeSpecial,
            -> entry is ConstantMethodRefEntry || entry is ConstantInterfaceMethodRefEntry

            MethodHandleReferenceKind.InvokeInterface -> entry is ConstantInterfaceMethodRefEntry
        }

    private fun MethodHandleReferenceKind.toRuntimeReferenceKind(): JvmMethodHandleReferenceKind =
        when (this) {
            MethodHandleReferenceKind.GetField -> JvmMethodHandleReferenceKind.GetField
            MethodHandleReferenceKind.GetStatic -> JvmMethodHandleReferenceKind.GetStatic
            MethodHandleReferenceKind.PutField -> JvmMethodHandleReferenceKind.PutField
            MethodHandleReferenceKind.PutStatic -> JvmMethodHandleReferenceKind.PutStatic
            MethodHandleReferenceKind.InvokeVirtual -> JvmMethodHandleReferenceKind.InvokeVirtual
            MethodHandleReferenceKind.InvokeStatic -> JvmMethodHandleReferenceKind.InvokeStatic
            MethodHandleReferenceKind.InvokeSpecial -> JvmMethodHandleReferenceKind.InvokeSpecial
            MethodHandleReferenceKind.NewInvokeSpecial -> JvmMethodHandleReferenceKind.NewInvokeSpecial
            MethodHandleReferenceKind.InvokeInterface -> JvmMethodHandleReferenceKind.InvokeInterface
        }
}
