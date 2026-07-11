package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatNegationInstructionVerifierTest {
    @Test
    fun `fneg requires float on top and leaves the type state unchanged`() {
        val frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float))

        val nextFrame = FloatNegationInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `fneg rejects a non float operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatNegationInstructionVerifier.verify(
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
    fun `fneg rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatNegationInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `fneg rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatNegationInstructionVerifier.verify(
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
        VerificationFrameState(bytecodeOffset = 34, locals = emptyList(), stack = stack)
}
