package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntToLongConversionInstructionVerifierTest {
    @Test
    fun `i2l replaces int on top with long`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer))

        val nextFrame = IntToLongConversionInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Float, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `i2l rejects a non int operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToLongConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `i2l rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToLongConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `i2l rejects a result stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToLongConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 53, locals = emptyList(), stack = stack)
}
