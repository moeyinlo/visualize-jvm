package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class Dup2InstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(62))

    @Test
    fun `dup2 form 1 duplicates the top two category one values in original order`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 92,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Null, VerificationType.Float, objectType),
        )

        val nextFrame = Dup2InstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Null,
                    VerificationType.Float,
                    objectType,
                    VerificationType.Float,
                    objectType,
                ),
            ),
            nextFrame,
        )
    }

    @Test
    fun `dup2 form 2 duplicates a category two top value`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 92,
            locals = emptyList(),
            stack = listOf(VerificationType.Integer, VerificationType.Double),
        )

        val nextFrame = Dup2InstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Integer, VerificationType.Double, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `dup2 rejects top because it may denote a category two upper half`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 92,
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
    fun `dup2 rejects form 1 when the second operand is missing`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 92,
                    locals = emptyList(),
                    stack = listOf(objectType),
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
    fun `dup2 rejects form 1 when the second operand is category two`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 92,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, objectType),
                ),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup2 rejects max stack overflow caused by duplicating form 1 values`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 92,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, objectType),
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
