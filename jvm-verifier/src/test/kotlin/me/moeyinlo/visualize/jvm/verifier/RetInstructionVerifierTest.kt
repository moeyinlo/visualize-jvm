package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetInstructionVerifierTest {
    @Test
    fun `ret requires a returnAddress local and preserves the type state`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 169,
            locals = listOf(VerificationType.Float, VerificationType.ReturnAddress),
            stack = listOf(VerificationType.Integer),
        )

        val nextFrame = RetInstructionVerifier.verify(
            frame = frame,
            index = 1,
            maxLocals = 2,
        )

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `ret rejects locals that are not returnAddress`() {
        val exception = assertFailsWith<MethodVerificationException> {
            RetInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 169,
                    locals = listOf(VerificationType.Float),
                    stack = emptyList(),
                ),
                index = 0,
                maxLocals = 1,
            )
        }

        assertEquals(
            "Local variable 0 contains Float, expected ReturnAddress",
            exception.message,
        )
    }
}
