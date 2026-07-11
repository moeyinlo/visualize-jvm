package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleCompareGreaterInstructionVerifierTest {
    @Test
    fun `dcmpg replaces two doubles on top with int`() {
        val frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Double, VerificationType.Double))

        val nextFrame = DoubleCompareGreaterInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(
            frame(stack = listOf(VerificationType.Integer, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `dcmpg rejects a non double top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleCompareGreaterInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Double, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dcmpg rejects a non double next operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleCompareGreaterInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Double)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dcmpg rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleCompareGreaterInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dcmpg rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleCompareGreaterInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Double, VerificationType.Double)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 152, locals = emptyList(), stack = stack)
}
