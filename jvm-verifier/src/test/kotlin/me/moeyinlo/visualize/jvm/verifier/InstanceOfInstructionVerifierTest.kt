package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class InstanceOfInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(13))

    @Test
    fun `instanceof replaces an object reference with int on the operand stack`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 40,
            locals = emptyList(),
            stack = listOf(VerificationType.Float, objectType),
        )

        val nextFrame = InstanceOfInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `instanceof accepts null object references`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 40,
            locals = emptyList(),
            stack = listOf(VerificationType.Null),
        )

        val nextFrame = InstanceOfInstructionVerifier.verify(frame = frame, maxStack = 1)

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `instanceof rejects non reference operands`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InstanceOfInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 40,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }
}
