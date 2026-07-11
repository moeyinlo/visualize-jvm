package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleNegationInstructionVerifierTest {
    @Test
    fun `dneg requires double on top and leaves the type state unchanged`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Double))

        val nextFrame = DoubleNegationInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `dneg rejects a non double operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleNegationInstructionVerifier.verify(
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
    fun `dneg rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleNegationInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dneg rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleNegationInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Double)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 28, locals = emptyList(), stack = stack)
}
