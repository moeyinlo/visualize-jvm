package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleBinaryArithmeticInstructionVerifierTest {
    @Test
    fun `dadd replaces two double operands with one double result`() {
        val frame = frame(
            stack = listOf(VerificationType.Integer, VerificationType.Double, VerificationType.Double),
        )

        val nextFrame = DoubleBinaryArithmeticInstructionVerifier.verify(
            frame = frame,
            kind = DoubleBinaryArithmeticKind.Add,
            maxStack = 5,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Integer, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `dsub uses the same double binary type rule as dadd`() {
        val frame = frame(stack = listOf(VerificationType.Double, VerificationType.Double))

        val nextFrame = DoubleBinaryArithmeticInstructionVerifier.verify(
            frame = frame,
            kind = DoubleBinaryArithmeticKind.Subtract,
            maxStack = 4,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Double)), nextFrame)
    }

    @Test
    fun `double binary arithmetic rejects a non double top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Double, VerificationType.Integer)),
                kind = DoubleBinaryArithmeticKind.Multiply,
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `double binary arithmetic rejects a non double next operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Double)),
                kind = DoubleBinaryArithmeticKind.Divide,
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `double binary arithmetic rejects missing second operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Double)),
                kind = DoubleBinaryArithmeticKind.Remainder,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `double binary arithmetic rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Double, VerificationType.Double)),
                kind = DoubleBinaryArithmeticKind.Add,
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 34, locals = emptyList(), stack = stack)
}
