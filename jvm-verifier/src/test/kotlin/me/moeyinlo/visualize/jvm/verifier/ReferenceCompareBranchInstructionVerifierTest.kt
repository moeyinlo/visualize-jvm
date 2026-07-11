package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ReferenceCompareBranchInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(7))

    @Test
    fun `if acmp condition pops two reference values from the operand stack`() {
        val frame = frame(stack = listOf(VerificationType.Integer, objectType, VerificationType.Null))

        val nextFrame = ReferenceCompareBranchInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame(stack = listOf(VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `if acmp condition rejects a non reference top value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceCompareBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(objectType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `if acmp condition rejects a non reference next value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceCompareBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, objectType)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `if acmp condition rejects a missing next value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceCompareBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(objectType)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `if acmp condition rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceCompareBranchInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, objectType, VerificationType.Null)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 165, locals = emptyList(), stack = stack)
}
