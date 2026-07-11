package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class DupX1InstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(51))

    @Test
    fun `dup_x1 inserts a duplicate category one top value beneath the next category one value`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 90,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, VerificationType.Integer, objectType),
        )

        val nextFrame = DupX1InstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, objectType, VerificationType.Integer, objectType)),
            nextFrame,
        )
    }

    @Test
    fun `dup_x1 preserves primitive category one values`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 90,
            locals = emptyList(),
            stack = listOf(VerificationType.Integer, VerificationType.Float),
        )

        val nextFrame = DupX1InstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `dup_x1 rejects a category two top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 90,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Long),
                ),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup_x1 rejects a category two value below the top`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 90,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Integer),
                ),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup_x1 rejects top because it may denote a category two upper half`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 90,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Top),
                ),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Top, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup_x1 rejects a missing second operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 90,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup_x1 rejects max stack overflow caused by the duplicated value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 90,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, objectType),
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
