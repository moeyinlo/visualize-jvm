package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleToFloatConversionInstructionVerifierTest {
    @Test
    fun `d2f replaces double on top with float`() {
        val frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Double))

        val nextFrame = DoubleToFloatConversionInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `d2f rejects a non double operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleToFloatConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `d2f rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleToFloatConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `d2f rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleToFloatConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Double)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 429, locals = emptyList(), stack = stack)
}
