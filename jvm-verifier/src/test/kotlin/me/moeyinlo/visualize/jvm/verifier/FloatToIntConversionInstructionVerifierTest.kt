package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatToIntConversionInstructionVerifierTest {
    @Test
    fun `f2i replaces float on top with int`() {
        val frame = frame(stack = listOf(VerificationType.Long, VerificationType.Float))

        val nextFrame = FloatToIntConversionInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `f2i rejects a non float operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToIntConversionInstructionVerifier.verify(
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
    fun `f2i rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToIntConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `f2i rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToIntConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Float)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 459, locals = emptyList(), stack = stack)
}
