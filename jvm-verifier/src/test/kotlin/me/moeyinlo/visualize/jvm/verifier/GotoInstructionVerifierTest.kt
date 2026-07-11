package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class GotoInstructionVerifierTest {
    @Test
    fun `goto preserves locals and operand stack`() {
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(17))
        val frame = VerificationFrameState(
            bytecodeOffset = 167,
            locals = listOf(VerificationType.Integer, VerificationType.Long),
            stack = listOf(objectType, VerificationType.Long),
        )

        val nextFrame = GotoInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `goto_w uses the same no operand stack rule as goto`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 200,
            locals = listOf(VerificationType.Float),
            stack = emptyList(),
        )

        val nextFrame = GotoInstructionVerifier.verify(frame = frame, maxStack = 0)

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `goto rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            GotoInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 167,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Integer),
                ),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }
}
