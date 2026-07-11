package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SwitchInstructionVerifierTest {
    @Test
    fun `tableswitch pops an int key from the operand stack`() {
        val frame = frame(
            bytecodeOffset = 170,
            stack = listOf(VerificationType.Float, VerificationType.Integer),
        )

        val nextFrame = SwitchInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(
            frame(bytecodeOffset = 170, stack = listOf(VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `lookupswitch uses the same int key rule`() {
        val frame = frame(bytecodeOffset = 171, stack = listOf(VerificationType.Integer))

        val nextFrame = SwitchInstructionVerifier.verify(frame = frame, maxStack = 1)

        assertEquals(frame(bytecodeOffset = 171, stack = emptyList()), nextFrame)
    }

    @Test
    fun `switch rejects a non int key`() {
        val exception = assertFailsWith<MethodVerificationException> {
            SwitchInstructionVerifier.verify(
                frame = frame(bytecodeOffset = 170, stack = listOf(VerificationType.Float)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `switch rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            SwitchInstructionVerifier.verify(
                frame = frame(bytecodeOffset = 171, stack = emptyList()),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `switch rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            SwitchInstructionVerifier.verify(
                frame = frame(
                    bytecodeOffset = 170,
                    stack = listOf(VerificationType.Long, VerificationType.Integer),
                ),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(bytecodeOffset: Int, stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = bytecodeOffset, locals = emptyList(), stack = stack)
}
