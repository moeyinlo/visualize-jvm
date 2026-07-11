package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsrWideInstructionVerifierTest {
    @Test
    fun `jsr_w pushes a returnAddress and preserves locals`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 201,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float),
        )

        val nextFrame = JsrWideInstructionVerifier.verify(
            frame = frame,
            maxStack = 2,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.ReturnAddress)),
            nextFrame,
        )
    }

    @Test
    fun `jsr_w rejects operand stack overflow`() {
        val exception = assertFailsWith<MethodVerificationException> {
            JsrWideInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 201,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }
}
