package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConstantInstructionVerifierTest {
    @Test
    fun `integer constants push int onto the operand stack`() {
        val frame = frame(stack = listOf(VerificationType.Float))

        val nextFrame = ConstantInstructionVerifier.verify(
            frame = frame,
            kind = ConstantPushKind.Int,
            maxStack = 2,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `aconst_null pushes null onto the operand stack`() {
        val frame = frame(stack = emptyList())

        val nextFrame = ConstantInstructionVerifier.verify(
            frame = frame,
            kind = ConstantPushKind.Null,
            maxStack = 1,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Null)), nextFrame)
    }

    @Test
    fun `long and double constants account for category two stack depth`() {
        val frame = frame(stack = listOf(VerificationType.Integer))

        val longFrame = ConstantInstructionVerifier.verify(
            frame = frame,
            kind = ConstantPushKind.Long,
            maxStack = 3,
        )
        val doubleFrame = ConstantInstructionVerifier.verify(
            frame = frame,
            kind = ConstantPushKind.Double,
            maxStack = 3,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer, VerificationType.Long)), longFrame)
        assertEquals(frame.copy(stack = listOf(VerificationType.Integer, VerificationType.Double)), doubleFrame)
    }

    @Test
    fun `constant push rejects operand stack overflow`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ConstantInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                kind = ConstantPushKind.Long,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 12, locals = emptyList(), stack = stack)
}
