package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WideRetInstructionVerifierTest {
    @Test
    fun `wide ret accepts a widened local index and preserves the type state`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 196,
            locals = List(300) { VerificationType.Top } + VerificationType.ReturnAddress,
            stack = listOf(VerificationType.Float),
        )

        val nextFrame = WideRetInstructionVerifier.verify(
            frame = frame,
            index = 300,
            maxLocals = 301,
        )

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `wide ret uses the widened instruction type rule`() {
        val exception = assertFailsWith<MethodVerificationException> {
            WideRetInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 196,
                    locals = List(300) { VerificationType.Top } + VerificationType.Float,
                    stack = emptyList(),
                ),
                index = 300,
                maxLocals = 301,
            )
        }

        assertEquals(
            "Local variable 300 contains Float, expected ReturnAddress",
            exception.message,
        )
    }
}
