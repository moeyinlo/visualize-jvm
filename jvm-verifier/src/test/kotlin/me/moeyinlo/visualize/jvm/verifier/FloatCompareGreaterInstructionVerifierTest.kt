package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatCompareGreaterInstructionVerifierTest {
    @Test
    fun `fcmpg replaces two floats on top with int`() {
        val frame = frame(stack = listOf(VerificationType.Long, VerificationType.Float, VerificationType.Float))

        val nextFrame = FloatCompareGreaterInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(
            frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `fcmpg rejects a non float top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatCompareGreaterInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `fcmpg rejects a non float next operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatCompareGreaterInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `fcmpg rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatCompareGreaterInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `fcmpg rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatCompareGreaterInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Float)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 150, locals = emptyList(), stack = stack)
}
