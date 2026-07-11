package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatToDoubleConversionInstructionVerifierTest {
    @Test
    fun `f2d replaces float on top with double`() {
        val frame = frame(stack = listOf(VerificationType.Long, VerificationType.Float))

        val nextFrame = FloatToDoubleConversionInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(
            frame(stack = listOf(VerificationType.Long, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `f2d rejects a non float operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToDoubleConversionInstructionVerifier.verify(
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
    fun `f2d rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToDoubleConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `f2d rejects a result stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatToDoubleConversionInstructionVerifier.verify(
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
        VerificationFrameState(bytecodeOffset = 458, locals = emptyList(), stack = stack)
}
