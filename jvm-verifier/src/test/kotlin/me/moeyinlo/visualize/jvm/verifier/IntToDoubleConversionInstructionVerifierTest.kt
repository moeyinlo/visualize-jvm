package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntToDoubleConversionInstructionVerifierTest {
    @Test
    fun `i2d replaces int on top with double`() {
        val frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer))

        val nextFrame = IntToDoubleConversionInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(
            frame(stack = listOf(VerificationType.Long, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `i2d rejects a non int operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToDoubleConversionInstructionVerifier.verify(
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
    fun `i2d rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToDoubleConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `i2d rejects a result stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToDoubleConversionInstructionVerifier.verify(
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
        VerificationFrameState(bytecodeOffset = 55, locals = emptyList(), stack = stack)
}
