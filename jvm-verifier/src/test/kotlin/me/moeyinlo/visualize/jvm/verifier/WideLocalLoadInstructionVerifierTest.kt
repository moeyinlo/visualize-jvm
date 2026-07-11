package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WideLocalLoadInstructionVerifierTest {
    @Test
    fun `wide iload accepts a widened local index`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 196,
            locals = List(300) { VerificationType.Top } + VerificationType.Integer,
            stack = emptyList(),
        )

        val nextFrame = WideLocalLoadInstructionVerifier.verify(
            frame = frame,
            index = 300,
            kind = LocalLoadKind.Int,
            maxLocals = 301,
            maxStack = 1,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `wide local load uses the widened instruction type rule`() {
        val exception = assertFailsWith<MethodVerificationException> {
            WideLocalLoadInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 196,
                    locals = List(300) { VerificationType.Top } + VerificationType.Float,
                    stack = emptyList(),
                ),
                index = 300,
                kind = LocalLoadKind.Int,
                maxLocals = 301,
                maxStack = 1,
            )
        }

        assertEquals(
            "Local variable 300 contains Float, expected Integer",
            exception.message,
        )
    }
}
