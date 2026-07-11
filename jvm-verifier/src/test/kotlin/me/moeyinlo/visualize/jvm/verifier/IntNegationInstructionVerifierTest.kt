package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntNegationInstructionVerifierTest {
    @Test
    fun `ineg requires int on top and leaves the type state unchanged`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer))

        val nextFrame = IntNegationInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `ineg rejects a non int operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntNegationInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `ineg rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntNegationInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `ineg rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntNegationInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 43, locals = emptyList(), stack = stack)
}
