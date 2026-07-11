package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class Dup2X1InstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(63))

    @Test
    fun `dup2_x1 form 1 inserts the duplicated top two category one values one value down`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 93,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Null, VerificationType.Float, VerificationType.Integer, objectType),
        )

        val nextFrame = Dup2X1InstructionVerifier.verify(frame = frame, maxStack = 6)

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Null,
                    VerificationType.Integer,
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
    fun `dup2_x1 form 2 inserts a duplicated category two top one category one value down`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 93,
            locals = emptyList(),
            stack = listOf(VerificationType.Float, VerificationType.Integer, VerificationType.Long),
        )

        val nextFrame = Dup2X1InstructionVerifier.verify(frame = frame, maxStack = 6)

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Float,
                    VerificationType.Long,
                    VerificationType.Integer,
                    VerificationType.Long,
                ),
            ),
            nextFrame,
        )
    }

    @Test
    fun `dup2_x1 rejects top because it may denote a category two upper half`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 93,
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
    fun `dup2_x1 rejects form 1 when the third operand is missing`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 93,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, objectType),
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
    fun `dup2_x1 rejects form 1 when the second operand is category two`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 93,
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
    fun `dup2_x1 rejects form 2 when the second operand is missing`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 93,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
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
    fun `dup2_x1 rejects max stack overflow caused by form 2 insertion`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Dup2X1InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 93,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Long),
                ),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }
}
