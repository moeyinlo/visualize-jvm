package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class MonitorInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))

    @Test
    fun `monitorenter pops an object reference from the operand stack`() {
        val frame = frame(stack = listOf(VerificationType.Integer, objectType))

        val nextFrame = MonitorInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `monitorexit accepts null as a reference type and pops it`() {
        val frame = frame(stack = listOf(VerificationType.Null))

        val nextFrame = MonitorInstructionVerifier.verify(frame = frame, maxStack = 1)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `monitor instructions reject non reference operands`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MonitorInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 12, locals = emptyList(), stack = stack)
}
