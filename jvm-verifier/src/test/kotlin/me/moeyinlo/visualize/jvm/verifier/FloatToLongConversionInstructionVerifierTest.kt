package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatToLongConversionInstructionVerifierTest {
    @Test
    fun `f2l replaces float on top with long`() {
        val frame = frame(stack = listOf(VerificationType.Long, VerificationType.Float))

        val nextFrame = FloatToLongConversionInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(
            frame(stack = listOf(VerificationType.Long, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `f2l rejects a non float operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToLongConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `f2l rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToLongConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `f2l rejects a result stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToLongConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Float)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 460, locals = emptyList(), stack = stack)
}
