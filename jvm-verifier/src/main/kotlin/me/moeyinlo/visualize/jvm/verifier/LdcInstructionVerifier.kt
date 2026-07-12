package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry

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
            is ConstantMethodTypeEntry -> VerificationType.ClassType("java/lang/invoke/MethodType")
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
}
