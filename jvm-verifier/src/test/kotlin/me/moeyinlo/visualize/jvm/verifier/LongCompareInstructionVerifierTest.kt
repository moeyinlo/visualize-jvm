package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongCompareInstructionVerifierTest {
    @Test
    fun `lcmp replaces two longs on top with int`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Long, VerificationType.Long))

        val nextFrame = LongCompareInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(
            frame(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `lcmp rejects a non long top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongCompareInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lcmp rejects a non long next operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongCompareInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lcmp rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongCompareInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lcmp rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongCompareInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 148, locals = emptyList(), stack = stack)
}
