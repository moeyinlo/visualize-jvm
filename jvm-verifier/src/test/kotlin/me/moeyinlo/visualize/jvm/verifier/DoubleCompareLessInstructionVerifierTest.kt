package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleCompareLessInstructionVerifierTest {
    @Test
    fun `dcmpl replaces two doubles on top with int`() {
        val frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Double, VerificationType.Double))

        val nextFrame = DoubleCompareLessInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(
            frame(stack = listOf(VerificationType.Integer, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `dcmpl rejects a non double top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleCompareLessInstructionVerifier.verify(
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
    fun `dcmpl rejects a non double next operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleCompareLessInstructionVerifier.verify(
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
    fun `dcmpl rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleCompareLessInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dcmpl rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleCompareLessInstructionVerifier.verify(
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
        VerificationFrameState(bytecodeOffset = 151, locals = emptyList(), stack = stack)
}
