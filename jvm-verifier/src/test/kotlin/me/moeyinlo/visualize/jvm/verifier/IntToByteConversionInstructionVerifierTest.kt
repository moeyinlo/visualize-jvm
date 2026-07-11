package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntToByteConversionInstructionVerifierTest {
    @Test
    fun `i2b requires int on top and leaves an int`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer))

        val nextFrame = IntToByteConversionInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `i2b rejects a non int operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToByteConversionInstructionVerifier.verify(
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
    fun `i2b rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToByteConversionInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `i2b rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntToByteConversionInstructionVerifier.verify(
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
        VerificationFrameState(bytecodeOffset = 145, locals = emptyList(), stack = stack)
}
