package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongToDoubleConversionInstructionVerifierTest {
    @Test
    fun `l2d replaces long on top with double`() {
        val frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Long))

        val nextFrame = LongToDoubleConversionInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Integer, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `l2d rejects a non long operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToDoubleConversionInstructionVerifier.verify(
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
    fun `l2d rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToDoubleConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `l2d rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToDoubleConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Long)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 545, locals = emptyList(), stack = stack)
}
