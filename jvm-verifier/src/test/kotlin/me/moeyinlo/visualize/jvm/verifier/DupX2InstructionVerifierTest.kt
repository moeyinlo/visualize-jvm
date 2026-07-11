package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class DupX2InstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(61))

    @Test
    fun `dup_x2 form 1 inserts a duplicate category one top beneath the next two category one values`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 91,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Null, VerificationType.Float, VerificationType.Integer, objectType),
        )

        val nextFrame = DupX2InstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Null,
                    objectType,
                    VerificationType.Float,
                    VerificationType.Integer,
                    objectType,
                ),
            ),
            nextFrame,
        )
    }

    @Test
    fun `dup_x2 form 2 inserts a duplicate category one top beneath a category two value`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 91,
            locals = emptyList(),
            stack = listOf(VerificationType.Float, VerificationType.Long, objectType),
        )

        val nextFrame = DupX2InstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, objectType, VerificationType.Long, objectType)),
            nextFrame,
        )
    }

    @Test
    fun `dup_x2 rejects a category two top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 91,
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
    fun `dup_x2 rejects top because it may denote a category two upper half`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 91,
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
    fun `dup_x2 rejects form 1 when the third operand is missing`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 91,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, objectType),
                ),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup_x2 rejects top as the second form 1 operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 91,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Top, objectType),
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
    fun `dup_x2 rejects max stack overflow caused by the duplicated value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DupX2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 91,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, objectType),
                ),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }
}
