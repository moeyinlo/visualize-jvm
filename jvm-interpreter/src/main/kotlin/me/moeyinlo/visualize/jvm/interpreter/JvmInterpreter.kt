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

class JvmArithmeticException(
    val guestClassName: String,
    message: String,
) : ArithmeticException(message)

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
            0x36,
            in 0x3B..0x3E,
            -> executeIntStore(instruction, operandStack, localVariables)
            0x37,
            in 0x3F..0x42,
            -> executeLongStore(instruction, operandStack, localVariables)
            0x38,
            in 0x43..0x46,
            -> executeFloatStore(instruction, operandStack, localVariables)
            0x39,
            in 0x47..0x4A,
            -> executeDoubleStore(instruction, operandStack, localVariables)
            0x3A,
            in 0x4B..0x4E,
            -> executeReferenceStore(instruction, operandStack, localVariables)
            0x57 -> executePop(instruction, operandStack)
            0x58 -> executePop2(instruction, operandStack)
            0x59 -> executeDup(instruction, operandStack)
            0x5A -> executeDupX1(instruction, operandStack)
            0x5B -> executeDupX2(instruction, operandStack)
            0x5C -> executeDup2(instruction, operandStack)
            0x5D -> executeDup2X1(instruction, operandStack)
            0x5E -> executeDup2X2(instruction, operandStack)
            0x5F -> executeSwap(instruction, operandStack)
            0x60 -> executeIntAdd(instruction, operandStack)
            0x61 -> executeLongAdd(instruction, operandStack)
            0x62 -> executeFloatAdd(instruction, operandStack)
            0x63 -> executeDoubleAdd(instruction, operandStack)
            0x64 -> executeIntSub(instruction, operandStack)
            0x65 -> executeLongSub(instruction, operandStack)
            0x66 -> executeFloatSub(instruction, operandStack)
            0x67 -> executeDoubleSub(instruction, operandStack)
            0x68 -> executeIntMul(instruction, operandStack)
            0x69 -> executeLongMul(instruction, operandStack)
            0x6A -> executeFloatMul(instruction, operandStack)
            0x6B -> executeDoubleMul(instruction, operandStack)
            0x6C -> executeIntDiv(instruction, operandStack)
            0x6D -> executeLongDiv(instruction, operandStack)
            0x6E -> executeFloatDiv(instruction, operandStack)
            0x6F -> executeDoubleDiv(instruction, operandStack)
            0x70 -> executeIntRem(instruction, operandStack)
            0x71 -> executeLongRem(instruction, operandStack)
            0x72 -> executeFloatRem(instruction, operandStack)
            0x73 -> executeDoubleRem(instruction, operandStack)
            0x74 -> executeIntNeg(instruction, operandStack)
            0x75 -> executeLongNeg(instruction, operandStack)
            0x76 -> executeFloatNeg(instruction, operandStack)
            0x77 -> executeDoubleNeg(instruction, operandStack)
            0x78 -> executeIntShiftLeft(instruction, operandStack)
            0x79 -> executeLongShiftLeft(instruction, operandStack)
            0x7A -> executeIntArithmeticShiftRight(instruction, operandStack)
            0x7B -> executeLongArithmeticShiftRight(instruction, operandStack)
            0x7C -> executeIntLogicalShiftRight(instruction, operandStack)
            0x7D -> executeLongLogicalShiftRight(instruction, operandStack)
            0x7E -> executeIntAnd(instruction, operandStack)
            0x7F -> executeLongAnd(instruction, operandStack)
            0x80 -> executeIntOr(instruction, operandStack)
            0x81 -> executeLongOr(instruction, operandStack)
            0x82 -> executeIntXor(instruction, operandStack)
            0x83 -> executeLongXor(instruction, operandStack)
            0x84 -> executeIncrement(instruction, localVariables)
            0x85 -> executeIntToLong(instruction, operandStack)
            0x86 -> executeIntToFloat(instruction, operandStack)
            0x87 -> executeIntToDouble(instruction, operandStack)
            0x88 -> executeLongToInt(instruction, operandStack)
            0x89 -> executeLongToFloat(instruction, operandStack)
            0x8A -> executeLongToDouble(instruction, operandStack)
            0x8B -> executeFloatToInt(instruction, operandStack)
            0x8C -> executeFloatToLong(instruction, operandStack)
            0x8D -> executeFloatToDouble(instruction, operandStack)
            0x8E -> executeDoubleToInt(instruction, operandStack)
            0x8F -> executeDoubleToLong(instruction, operandStack)
            0x90 -> executeDoubleToFloat(instruction, operandStack)
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

    private fun executeIntStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeLongStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeFloatStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeDoubleStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeReferenceStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executePop(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.peek()
        if (value.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 1 value but was category ${value.category.slotWidth}",
            )
        }
        operandStack.pop()
    }

    private fun executePop2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val top = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        if (top.category.slotWidth == 2) {
            operandStack.pop()
            return
        }

        val next = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values but found one",
            )
        if (next.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values or one category 2 value",
            )
        }
        operandStack.pop()
        operandStack.pop()
    }

    private fun executeDup(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.peek()
        if (value.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 1 value but was category ${value.category.slotWidth}",
            )
        }
        operandStack.push(value)
    }

    private fun executeDupX1(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values but found one",
            )
        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.push(value1)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeDupX2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        if (value1.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected top category 1 value",
            )
        }

        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 1 value over category 2 value " +
                    "or three category 1 values",
            )
        if (value2.category.slotWidth == 2) {
            operandStack.pop()
            operandStack.pop()
            operandStack.push(value1)
            operandStack.push(value2)
            operandStack.push(value1)
            return
        }

        val value3 = values.dropLast(2).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected three category 1 values",
            )
        if (value2.category.slotWidth != 1 || value3.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 1 value over category 2 value " +
                    "or three category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.pop()
        operandStack.push(value1)
        operandStack.push(value3)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeDup2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        if (value1.category.slotWidth == 2) {
            operandStack.pop()
            operandStack.push(value1)
            operandStack.push(value1)
            return
        }

        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values or one category 2 value",
            )
        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values or one category 2 value",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.push(value2)
        operandStack.push(value1)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeDup2X1(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 2 value over category 1 value " +
                    "or three category 1 values",
            )

        if (value1.category.slotWidth == 2) {
            if (value2.category.slotWidth != 1) {
                throw JvmUnsupportedInstructionException(
                    "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                        "${instruction.offset}: expected category 2 value over category 1 value",
                )
            }
            operandStack.pop()
            operandStack.pop()
            operandStack.push(value1)
            operandStack.push(value2)
            operandStack.push(value1)
            return
        }

        val value3 = values.dropLast(2).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected three category 1 values",
            )
        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1 || value3.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 2 value over category 1 value " +
                    "or three category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.pop()
        operandStack.push(value2)
        operandStack.push(value1)
        operandStack.push(value3)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeDup2X2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected a valid dup2_x2 operand form",
            )

        if (value1.category.slotWidth == 2) {
            if (value2.category.slotWidth == 2) {
                operandStack.pop()
                operandStack.pop()
                operandStack.push(value1)
                operandStack.push(value2)
                operandStack.push(value1)
                return
            }

            val value3 = values.dropLast(2).lastOrNull()
                ?: throw JvmUnsupportedInstructionException(
                    "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                        "${instruction.offset}: expected two category 1 values below a category 2 value",
                )
            if (value2.category.slotWidth != 1 || value3.category.slotWidth != 1) {
                throw JvmUnsupportedInstructionException(
                    "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                        "${instruction.offset}: expected two category 1 values below a category 2 value",
                )
            }

            operandStack.pop()
            operandStack.pop()
            operandStack.pop()
            operandStack.push(value1)
            operandStack.push(value3)
            operandStack.push(value2)
            operandStack.push(value1)
            return
        }

        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected top two category 1 values or top category 2 value",
            )
        }

        val value3 = values.dropLast(2).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected a third value below two category 1 values",
            )
        if (value3.category.slotWidth == 2) {
            operandStack.pop()
            operandStack.pop()
            operandStack.pop()
            operandStack.push(value2)
            operandStack.push(value1)
            operandStack.push(value3)
            operandStack.push(value2)
            operandStack.push(value1)
            return
        }

        val value4 = values.dropLast(3).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected four category 1 values",
            )
        if (value3.category.slotWidth != 1 || value4.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected four category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.pop()
        operandStack.pop()
        operandStack.push(value2)
        operandStack.push(value1)
        operandStack.push(value4)
        operandStack.push(value3)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeSwap(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values",
            )
        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.push(value1)
        operandStack.push(value2)
    }

    private fun executeIntAdd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value + value2.value))
    }

    private fun executeIntSub(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value - value2.value))
    }

    private fun executeIntMul(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value * value2.value))
    }

    private fun executeIntDiv(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }
        if (value2.value == 0) {
            throw JvmArithmeticException(
                guestClassName = "java/lang/ArithmeticException",
                message = "${instruction.metadata.mnemonic} at offset ${instruction.offset}: division by zero",
            )
        }

        operandStack.push(JvmIntValue(value1.value / value2.value))
    }

    private fun executeIntRem(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }
        if (value2.value == 0) {
            throw JvmArithmeticException(
                guestClassName = "java/lang/ArithmeticException",
                message = "${instruction.metadata.mnemonic} at offset ${instruction.offset}: division by zero",
            )
        }

        operandStack.push(JvmIntValue(value1.value % value2.value))
    }

    private fun executeIntNeg(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(-value.value))
    }

    private fun executeIntShiftLeft(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value shl (value2.value and 0x1F)))
    }

    private fun executeIntArithmeticShiftRight(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value shr (value2.value and 0x1F)))
    }

    private fun executeIntLogicalShiftRight(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value ushr (value2.value and 0x1F)))
    }

    private fun executeIntAnd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value and value2.value))
    }

    private fun executeIntOr(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value or value2.value))
    }

    private fun executeIntXor(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value xor value2.value))
    }

    private fun executeLongAdd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value + value2.value))
    }

    private fun executeLongSub(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value - value2.value))
    }

    private fun executeLongMul(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value * value2.value))
    }

    private fun executeLongDiv(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }
        if (value2.value == 0L) {
            throw JvmArithmeticException(
                guestClassName = "java/lang/ArithmeticException",
                message = "${instruction.metadata.mnemonic} at offset ${instruction.offset}: division by zero",
            )
        }

        operandStack.push(JvmLongValue(value1.value / value2.value))
    }

    private fun executeLongRem(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }
        if (value2.value == 0L) {
            throw JvmArithmeticException(
                guestClassName = "java/lang/ArithmeticException",
                message = "${instruction.metadata.mnemonic} at offset ${instruction.offset}: division by zero",
            )
        }

        operandStack.push(JvmLongValue(value1.value % value2.value))
    }

    private fun executeLongNeg(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(-value.value))
    }

    private fun executeLongShiftLeft(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value shl (value2.value and 0x3F)))
    }

    private fun executeLongArithmeticShiftRight(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value shr (value2.value and 0x3F)))
    }

    private fun executeLongLogicalShiftRight(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value ushr (value2.value and 0x3F)))
    }

    private fun executeLongAnd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value and value2.value))
    }

    private fun executeLongOr(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value or value2.value))
    }

    private fun executeLongXor(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value xor value2.value))
    }

    private fun executeFloatAdd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value + value2.value))
    }

    private fun executeFloatSub(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value - value2.value))
    }

    private fun executeFloatMul(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value * value2.value))
    }

    private fun executeFloatDiv(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value / value2.value))
    }

    private fun executeFloatRem(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value % value2.value))
    }

    private fun executeFloatNeg(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(-value.value))
    }

    private fun executeDoubleAdd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value + value2.value))
    }

    private fun executeDoubleSub(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value - value2.value))
    }

    private fun executeDoubleMul(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value * value2.value))
    }

    private fun executeDoubleDiv(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value / value2.value))
    }

    private fun executeDoubleRem(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value % value2.value))
    }

    private fun executeDoubleNeg(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(-value.value))
    }

    private fun executeIncrement(
        instruction: DecodedInstruction,
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
        localVariables.store(index, JvmIntValue(value.value + instruction.incrementConstant()))
    }

    private fun executeIntToLong(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value.value.toLong()))
    }

    private fun executeIntToFloat(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value.value.toFloat()))
    }

    private fun executeIntToDouble(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value.value.toDouble()))
    }

    private fun executeLongToInt(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value.toInt()))
    }

    private fun executeLongToFloat(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value.value.toFloat()))
    }

    private fun executeLongToDouble(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value.value.toDouble()))
    }

    private fun executeFloatToInt(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value.toInt()))
    }

    private fun executeFloatToLong(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value.value.toLong()))
    }

    private fun executeFloatToDouble(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value.value.toDouble()))
    }

    private fun executeDoubleToInt(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value.toInt()))
    }

    private fun executeDoubleToLong(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value.value.toLong()))
    }

    private fun executeDoubleToFloat(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value.value.toFloat()))
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
            0x36 -> executeIntStore(instruction, operandStack, localVariables)
            0x37 -> executeLongStore(instruction, operandStack, localVariables)
            0x38 -> executeFloatStore(instruction, operandStack, localVariables)
            0x39 -> executeDoubleStore(instruction, operandStack, localVariables)
            0x3A -> executeReferenceStore(instruction, operandStack, localVariables)
            0x84 -> executeIncrement(instruction, localVariables)
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
            0x36,
            0x37,
            0x38,
            0x39,
            0x3A,
            0x84,
            -> operands[0]
            0xC4 -> (operands[1] shl 8) or operands[2]
            in 0x1A..0x1D -> metadata.opcode - 0x1A
            in 0x1E..0x21 -> metadata.opcode - 0x1E
            in 0x22..0x25 -> metadata.opcode - 0x22
            in 0x26..0x29 -> metadata.opcode - 0x26
            in 0x2A..0x2D -> metadata.opcode - 0x2A
            in 0x3B..0x3E -> metadata.opcode - 0x3B
            in 0x3F..0x42 -> metadata.opcode - 0x3F
            in 0x43..0x46 -> metadata.opcode - 0x43
            in 0x47..0x4A -> metadata.opcode - 0x47
            in 0x4B..0x4E -> metadata.opcode - 0x4B
            else -> error("Instruction ${metadata.mnemonic} does not use a local variable index")
        }

    private fun DecodedInstruction.incrementConstant(): Int =
        when (metadata.opcode) {
            0x84 -> operands[1].toByte().toInt()
            0xC4 -> ((operands[3] shl 8) or operands[4]).toShort().toInt()
            else -> error("Instruction ${metadata.mnemonic} does not use an increment constant")
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
