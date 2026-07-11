package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntToFloatConversionInstructionVerifierTest {
    @Test
    fun `i2f replaces int on top with float`() {
        val frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer))

        val nextFrame = IntToFloatConversionInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Long, VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `i2f rejects a non int operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToFloatConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `i2f rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToFloatConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `i2f rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToFloatConversionInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 56, locals = emptyList(), stack = stack)
}
