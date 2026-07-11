package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ReferenceNullBranchInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(11))

    @Test
    fun `ifnull pops a reference value from the operand stack`() {
        val frame = frame(stack = listOf(VerificationType.Integer, objectType))

        val nextFrame = ReferenceNullBranchInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(
            frame(stack = listOf(VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `ifnonnull accepts null as a reference value`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Null))

        val nextFrame = ReferenceNullBranchInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(
            frame(stack = listOf(VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `ifnull rejects a non reference value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceNullBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `ifnull rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceNullBranchInstructionVerifier.verify(frame = frame(stack = emptyList()), maxStack = 1)
        }

        assertEquals(
            "Operand stack is empty, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `ifnull rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceNullBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, objectType)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 198, locals = emptyList(), stack = stack)
}
