package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class NopInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))

    @Test
    fun `nop leaves the verification frame unchanged`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 0,
            locals = listOf(VerificationType.Integer, objectType),
            stack = listOf(VerificationType.Float, VerificationType.Null),
        )

        val nextFrame = NopInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame, nextFrame)
    }

    @Test
    fun `nop rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            NopInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
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
}
