package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongToIntConversionInstructionVerifierTest {
    @Test
    fun `l2i replaces long on top with int`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Long))

        val nextFrame = LongToIntConversionInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `l2i rejects a non long operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToIntConversionInstructionVerifier.verify(
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
    fun `l2i rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToIntConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `l2i rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToIntConversionInstructionVerifier.verify(
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
        VerificationFrameState(bytecodeOffset = 547, locals = emptyList(), stack = stack)
}
