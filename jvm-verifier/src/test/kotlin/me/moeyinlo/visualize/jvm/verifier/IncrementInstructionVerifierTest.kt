package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IncrementInstructionVerifierTest {
    @Test
    fun `iinc requires an int local and does not change the type state`() {
        val frame = frame(
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float),
        )

        val nextFrame = IncrementInstructionVerifier.verify(
            frame = frame,
            index = 0,
            maxLocals = 1,
        )

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `iinc rejects locals that are not int`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IncrementInstructionVerifier.verify(
                frame = frame(
                    locals = listOf(VerificationType.Float),
                    stack = emptyList(),
                ),
                index = 0,
                maxLocals = 1,
            )
        }

        assertEquals(
            "Local variable 0 contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `iinc rejects local indexes outside max_locals`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IncrementInstructionVerifier.verify(
                frame = frame(
                    locals = listOf(VerificationType.Integer),
                    stack = emptyList(),
                ),
                index = 1,
                maxLocals = 1,
            )
        }

        assertEquals(
            "Local variable index 1 with width 1 exceeds max_locals=1",
            exception.message,
        )
    }

    private fun frame(
        locals: List<VerificationType>,
        stack: List<VerificationType>,
    ): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 12, locals = locals, stack = stack)
}
