package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntBinaryArithmeticInstructionVerifierTest {
    @Test
    fun `iadd replaces two int operands with one int result`() {
        val frame = frame(
            stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Integer),
        )

        val nextFrame = IntBinaryArithmeticInstructionVerifier.verify(
            frame = frame,
            kind = IntBinaryArithmeticKind.Add,
            maxStack = 3,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `ixor uses the same int binary type rule as iadd`() {
        val frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer))

        val nextFrame = IntBinaryArithmeticInstructionVerifier.verify(
            frame = frame,
            kind = IntBinaryArithmeticKind.Xor,
            maxStack = 2,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `int binary arithmetic rejects a non int top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
                kind = IntBinaryArithmeticKind.Multiply,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `int binary arithmetic rejects a non int next operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer)),
                kind = IntBinaryArithmeticKind.Divide,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `int binary arithmetic rejects missing second operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                kind = IntBinaryArithmeticKind.Remainder,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `int binary arithmetic rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer, VerificationType.Integer)),
                kind = IntBinaryArithmeticKind.And,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 24, locals = emptyList(), stack = stack)
}
