package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongToFloatConversionInstructionVerifierTest {
    @Test
    fun `l2f replaces long on top with float`() {
        val frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Long))

        val nextFrame = LongToFloatConversionInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `l2f rejects a non long operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToFloatConversionInstructionVerifier.verify(
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
    fun `l2f rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToFloatConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `l2f rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongToFloatConversionInstructionVerifier.verify(
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
        VerificationFrameState(bytecodeOffset = 546, locals = emptyList(), stack = stack)
}
