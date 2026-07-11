package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class DupInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(41))

    @Test
    fun `dup duplicates a category one operand stack top`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 89,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, VerificationType.Integer),
        )

        val nextFrame = DupInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `dup preserves the actual reference subtype it duplicates`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 89,
            locals = emptyList(),
            stack = listOf(VerificationType.Integer, objectType),
        )

        val nextFrame = DupInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Integer, objectType, objectType)),
            nextFrame,
        )
    }

    @Test
    fun `dup rejects a category two operand stack top`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 89,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                ),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup rejects top because it may denote a category two upper half`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 89,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Top),
                ),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Top, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 89, locals = emptyList(), stack = emptyList()),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup rejects max stack overflow caused by the duplicate`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 89,
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
