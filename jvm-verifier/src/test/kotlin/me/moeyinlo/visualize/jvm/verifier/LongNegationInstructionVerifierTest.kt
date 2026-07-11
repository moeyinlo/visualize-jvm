package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongNegationInstructionVerifierTest {
    @Test
    fun `lneg requires long on top and leaves the type state unchanged`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Long))

        val nextFrame = LongNegationInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `lneg rejects a non long operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongNegationInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lneg rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongNegationInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lneg rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongNegationInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Long)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 46, locals = emptyList(), stack = stack)
}
