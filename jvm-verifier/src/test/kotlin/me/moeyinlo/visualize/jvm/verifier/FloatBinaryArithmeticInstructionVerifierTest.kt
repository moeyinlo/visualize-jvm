package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatBinaryArithmeticInstructionVerifierTest {
    @Test
    fun `fadd replaces two float operands with one float result`() {
        val frame = frame(
            stack = listOf(VerificationType.Integer, VerificationType.Float, VerificationType.Float),
        )

        val nextFrame = FloatBinaryArithmeticInstructionVerifier.verify(
            frame = frame,
            kind = FloatBinaryArithmeticKind.Add,
            maxStack = 3,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Integer, VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `fsub uses the same float binary type rule as fadd`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Float))

        val nextFrame = FloatBinaryArithmeticInstructionVerifier.verify(
            frame = frame,
            kind = FloatBinaryArithmeticKind.Subtract,
            maxStack = 2,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Float)), nextFrame)
    }

    @Test
    fun `float binary arithmetic rejects a non float top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer)),
                kind = FloatBinaryArithmeticKind.Multiply,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `float binary arithmetic rejects a non float next operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
                kind = FloatBinaryArithmeticKind.Divide,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `float binary arithmetic rejects missing second operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float)),
                kind = FloatBinaryArithmeticKind.Remainder,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `float binary arithmetic rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float, VerificationType.Float)),
                kind = FloatBinaryArithmeticKind.Add,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 31, locals = emptyList(), stack = stack)
}
