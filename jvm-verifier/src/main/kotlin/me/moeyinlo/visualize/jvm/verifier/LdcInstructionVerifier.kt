package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

object LdcInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        index: ConstantPoolIndex,
        constantPool: ConstantPool,
        maxStack: Int,
    ): VerificationFrameState {
        val entry = loadConstantPoolEntry(index = index, constantPool = constantPool)
        val kind = when (entry) {
            is ConstantIntegerEntry -> ConstantPushKind.Int
            is ConstantFloatEntry -> ConstantPushKind.Float
            else -> throw MethodVerificationException(
                "ldc constant_pool index $index references unsupported constant ${entry.javaClass.simpleName}",
            )
        }
        return ConstantInstructionVerifier.verify(
            frame = frame,
            kind = kind,
            maxStack = maxStack,
        )
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
