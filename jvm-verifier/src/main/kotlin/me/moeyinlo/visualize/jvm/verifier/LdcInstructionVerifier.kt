package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry

object LdcInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: ConstantPoolIndex,
        constantPool: ConstantPool,
        maxStack: Int,
    ): VerificationFrameState {
        val entry = loadConstantPoolEntry(index = index, constantPool = constantPool)
        val pushedType = when (entry) {
            is ConstantIntegerEntry -> VerificationType.Integer
            is ConstantFloatEntry -> VerificationType.Float
            is ConstantStringEntry -> VerificationType.ClassType("java/lang/String")
            is ConstantClassEntry -> VerificationType.ClassType("java/lang/Class")
            is ConstantMethodHandleEntry -> VerificationType.ClassType("java/lang/invoke/MethodHandle")
            is ConstantMethodTypeEntry -> VerificationType.ClassType("java/lang/invoke/MethodType")
            is ConstantDynamicEntry -> dynamicCategory1ConstantType(entry = entry, constantPool = constantPool)
            else -> throw MethodVerificationException(
                "ldc constant_pool index $index references unsupported constant ${entry.javaClass.simpleName}",
            )
        }
        val nextStack = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .push(pushedType)
            .values
        return frame.copy(stack = nextStack)
    }

    fun verifyCategory2(
        frame: VerificationFrameState,
        index: ConstantPoolIndex,
        constantPool: ConstantPool,
        maxStack: Int,
    ): VerificationFrameState {
        val entry = loadConstantPoolEntry(index = index, constantPool = constantPool)
        val pushedType = when (entry) {
            is ConstantLongEntry -> VerificationType.Long
            is ConstantDoubleEntry -> VerificationType.Double
            is ConstantDynamicEntry -> dynamicCategory2ConstantType(entry = entry, constantPool = constantPool)
            else -> throw MethodVerificationException(
                "ldc2_w constant_pool index $index references unsupported constant ${entry.javaClass.simpleName}",
            )
        }
        val nextStack = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .push(pushedType)
            .values
        return frame.copy(stack = nextStack)
    }

    private fun loadConstantPoolEntry(
        index: ConstantPoolIndex,
        constantPool: ConstantPool,
    ) = try {
        constantPool[index]
    } catch (exception: ConstantPoolFormatException) {
        throw MethodVerificationException(
            "Invalid ldc constant_pool index $index: ${exception.message}",
        )
    }

    private fun dynamicCategory1ConstantType(
        entry: ConstantDynamicEntry,
        constantPool: ConstantPool,
    ): VerificationType {
        val descriptor = dynamicConstantDescriptor(entry = entry, constantPool = constantPool)
        return when {
            descriptor == "I" -> VerificationType.Integer
            descriptor == "Z" -> VerificationType.Integer
            descriptor == "B" -> VerificationType.Integer
            descriptor == "C" -> VerificationType.Integer
            descriptor == "S" -> VerificationType.Integer
            descriptor == "F" -> VerificationType.Float
            descriptor == "[I" -> VerificationType.ArrayOf(VerificationType.Integer)
            descriptor.startsWith("[L") && descriptor.endsWith(";") && descriptor.length > 3 ->
                VerificationType.ArrayOf(
                    VerificationType.ClassType(descriptor.substring(2, descriptor.length - 1)),
                )
            descriptor.startsWith("L") && descriptor.endsWith(";") && descriptor.length > 2 ->
                VerificationType.ClassType(descriptor.substring(1, descriptor.length - 1))
            else -> throw MethodVerificationException(
                "ldc CONSTANT_Dynamic descriptor '$descriptor' is unsupported",
            )
        }
    }

    private fun dynamicCategory2ConstantType(
        entry: ConstantDynamicEntry,
        constantPool: ConstantPool,
    ): VerificationType {
        return when (val descriptor = dynamicConstantDescriptor(entry = entry, constantPool = constantPool)) {
            "J" -> VerificationType.Long
            "D" -> VerificationType.Double
            else -> throw MethodVerificationException(
                "ldc2_w CONSTANT_Dynamic descriptor '$descriptor' is unsupported",
            )
        }
    }

    private fun dynamicConstantDescriptor(
        entry: ConstantDynamicEntry,
        constantPool: ConstantPool,
    ): String {
        val nameAndTypeEntry = loadConstantPoolEntry(
            index = entry.nameAndTypeIndex,
            constantPool = constantPool,
        )
        val nameAndType = nameAndTypeEntry as? ConstantNameAndTypeEntry
            ?: throw MethodVerificationException(
                "ldc CONSTANT_Dynamic name_and_type_index ${entry.nameAndTypeIndex} " +
                    "references ${nameAndTypeEntry.javaClass.simpleName}",
            )
        val descriptorEntry = loadConstantPoolEntry(
            index = nameAndType.descriptorIndex,
            constantPool = constantPool,
        )
        val descriptor = descriptorEntry as? ConstantUtf8Entry
            ?: throw MethodVerificationException(
                "ldc CONSTANT_Dynamic descriptor_index ${nameAndType.descriptorIndex} " +
                    "references ${descriptorEntry.javaClass.simpleName}",
            )
        return descriptor.value
    }
}
