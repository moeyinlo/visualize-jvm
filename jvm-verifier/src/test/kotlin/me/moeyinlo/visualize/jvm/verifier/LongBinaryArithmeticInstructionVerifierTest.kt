package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongBinaryArithmeticInstructionVerifierTest {
    @Test
    fun `ladd replaces two long operands with one long result`() {
        val frame = frame(
            stack = listOf(VerificationType.Integer, VerificationType.Long, VerificationType.Long),
        )

        val nextFrame = LongBinaryArithmeticInstructionVerifier.verify(
            frame = frame,
            kind = LongBinaryArithmeticKind.Add,
            maxStack = 5,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Integer, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `lxor uses the same long binary type rule as ladd`() {
        val frame = frame(stack = listOf(VerificationType.Long, VerificationType.Long))

        val nextFrame = LongBinaryArithmeticInstructionVerifier.verify(
            frame = frame,
            kind = LongBinaryArithmeticKind.Xor,
            maxStack = 4,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Long)), nextFrame)
    }

    @Test
    fun `long binary arithmetic rejects a non long top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
                kind = LongBinaryArithmeticKind.Multiply,
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `long binary arithmetic rejects a non long next operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Long)),
                kind = LongBinaryArithmeticKind.Divide,
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `long binary arithmetic rejects missing second operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long)),
                kind = LongBinaryArithmeticKind.Remainder,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `long binary arithmetic rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongBinaryArithmeticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Long, VerificationType.Long)),
                kind = LongBinaryArithmeticKind.And,
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 28, locals = emptyList(), stack = stack)
}
