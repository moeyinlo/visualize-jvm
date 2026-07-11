package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongShiftInstructionVerifierTest {
    @Test
    fun `lshl replaces long value and int shift count with long result`() {
        val frame = frame(
            stack = listOf(VerificationType.Float, VerificationType.Long, VerificationType.Integer),
        )

        val nextFrame = LongShiftInstructionVerifier.verify(
            frame = frame,
            kind = LongShiftKind.Left,
            maxStack = 4,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `lushr uses the same long shift type rule as lshl`() {
        val frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer))

        val nextFrame = LongShiftInstructionVerifier.verify(
            frame = frame,
            kind = LongShiftKind.UnsignedRight,
            maxStack = 3,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Long)), nextFrame)
    }

    @Test
    fun `long shift rejects a non int shift count`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongShiftInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Long)),
                kind = LongShiftKind.Right,
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `long shift rejects a non long shifted value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongShiftInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer)),
                kind = LongShiftKind.Left,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `long shift rejects missing shifted value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongShiftInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                kind = LongShiftKind.Right,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `long shift rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongShiftInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Long, VerificationType.Integer)),
                kind = LongShiftKind.Left,
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 40, locals = emptyList(), stack = stack)
}
