package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WideLocalStoreInstructionVerifierTest {
    @Test
    fun `wide lstore accepts a widened local index`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 196,
            locals = emptyList(),
            stack = listOf(VerificationType.Long),
        )

        val nextFrame = WideLocalStoreInstructionVerifier.verify(
            frame = frame,
            index = 300,
            kind = LocalStoreKind.Long,
            maxLocals = 302,
            maxStack = 2,
        )

        assertEquals(
            frame.copy(
                locals = List(300) { VerificationType.Top } + listOf(VerificationType.Long, VerificationType.Top),
                stack = emptyList(),
            ),
            nextFrame,
        )
    }

    @Test
    fun `wide local store uses the widened instruction type rule`() {
        val exception = assertFailsWith<MethodVerificationException> {
            WideLocalStoreInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 196,
                    locals = List(301) { VerificationType.Top },
                    stack = listOf(VerificationType.Float),
                ),
                index = 300,
                kind = LocalStoreKind.Int,
                maxLocals = 301,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }
}
