package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntZeroBranchInstructionVerifierTest {
    @Test
    fun `if condition pops an int value from the operand stack`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer))

        val nextFrame = IntZeroBranchInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(
            frame(stack = listOf(VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `if condition rejects a non int value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntZeroBranchInstructionVerifier.verify(
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
    fun `if condition rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntZeroBranchInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `if condition rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntZeroBranchInstructionVerifier.verify(
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
        VerificationFrameState(bytecodeOffset = 153, locals = emptyList(), stack = stack)
}
