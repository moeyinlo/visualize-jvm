package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntCompareBranchInstructionVerifierTest {
    @Test
    fun `if icmp condition pops two int values from the operand stack`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Integer))

        val nextFrame = IntCompareBranchInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `if icmp condition rejects a non int top value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntCompareBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `if icmp condition rejects a non int next value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntCompareBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `if icmp condition rejects a missing next value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntCompareBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `if icmp condition rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntCompareBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 159, locals = emptyList(), stack = stack)
}
