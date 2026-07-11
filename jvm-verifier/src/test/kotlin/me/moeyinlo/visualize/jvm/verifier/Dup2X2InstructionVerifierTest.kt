package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class Dup2X2InstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(64))

    @Test
    fun `dup2_x2 form 1 inserts the duplicated top two category one values beneath two category one values`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 94,
            locals = emptyList(),
            stack = listOf(VerificationType.Null, VerificationType.Float, VerificationType.Integer, objectType),
        )

        val nextFrame = Dup2X2InstructionVerifier.verify(frame = frame, maxStack = 6)

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Integer,
                    objectType,
                    VerificationType.Null,
                    VerificationType.Float,
                    VerificationType.Integer,
                    objectType,
                ),
            ),
            nextFrame,
        )
    }

    @Test
    fun `dup2_x2 form 2 inserts a duplicated category two top beneath two category one values`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 94,
            locals = emptyList(),
            stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Long),
        )

        val nextFrame = Dup2X2InstructionVerifier.verify(frame = frame, maxStack = 6)

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Long,
                    VerificationType.Float,
                    VerificationType.Integer,
                    VerificationType.Long,
                ),
            ),
            nextFrame,
        )
    }

    @Test
    fun `dup2_x2 form 3 inserts the duplicated top two category one values beneath a category two value`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 94,
            locals = emptyList(),
            stack = listOf(VerificationType.Long, VerificationType.Integer, objectType),
        )

        val nextFrame = Dup2X2InstructionVerifier.verify(frame = frame, maxStack = 6)

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Integer,
                    objectType,
                    VerificationType.Long,
                    VerificationType.Integer,
                    objectType,
                ),
            ),
            nextFrame,
        )
    }

    @Test
    fun `dup2_x2 form 4 inserts a duplicated category two top beneath a category two value`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 94,
            locals = emptyList(),
            stack = listOf(VerificationType.Double, VerificationType.Long),
        )

        val nextFrame = Dup2X2InstructionVerifier.verify(frame = frame, maxStack = 6)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Long, VerificationType.Double, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `dup2_x2 rejects top because it may denote a category two upper half`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 94,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Top),
                ),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Top, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup2_x2 rejects a category one top when the second operand is missing`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 94,
                    locals = emptyList(),
                    stack = listOf(objectType),
                ),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `dup2_x2 rejects a category two top when the second operand is missing`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 94,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                ),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack is empty, expected category 1 or category 2 value",
            exception.message,
        )
    }

    @Test
    fun `dup2_x2 rejects a category one top when the second operand is category two`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 94,
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
    fun `dup2_x2 rejects max stack overflow caused by form 4 insertion`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 94,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Long),
                ),
                maxStack = 5,
            )
        }

        assertEquals(
            "Operand stack depth 6 exceeds max_stack=5",
            exception.message,
        )
    }
}
