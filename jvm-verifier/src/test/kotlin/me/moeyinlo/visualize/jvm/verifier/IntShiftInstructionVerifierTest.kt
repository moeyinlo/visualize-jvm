package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntShiftInstructionVerifierTest {
    @Test
    fun `ishl replaces int value and int shift count with int result`() {
        val frame = frame(
            stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Integer),
        )

        val nextFrame = IntShiftInstructionVerifier.verify(
            frame = frame,
            kind = IntShiftKind.Left,
            maxStack = 3,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `iushr uses the same int shift type rule as ishl`() {
        val frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer))

        val nextFrame = IntShiftInstructionVerifier.verify(
            frame = frame,
            kind = IntShiftKind.UnsignedRight,
            maxStack = 2,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `int shift rejects a non int shift count`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntShiftInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Long)),
                kind = IntShiftKind.Right,
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `int shift rejects a non int shifted value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntShiftInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer)),
                kind = IntShiftKind.Left,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `int shift rejects missing shifted value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntShiftInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                kind = IntShiftKind.Right,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `int shift rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntShiftInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Integer)),
                kind = IntShiftKind.Left,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 37, locals = emptyList(), stack = stack)
}
