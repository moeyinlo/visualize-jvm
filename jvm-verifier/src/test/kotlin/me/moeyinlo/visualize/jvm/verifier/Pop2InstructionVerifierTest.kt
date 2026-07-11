package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class Pop2InstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(31))

    @Test
    fun `pop2 removes two category one operand stack values`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 88,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, VerificationType.Integer, objectType),
        )

        val nextFrame = Pop2InstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `pop2 removes one category two operand stack top`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 88,
            locals = emptyList(),
            stack = listOf(VerificationType.Integer, VerificationType.Long),
        )

        val nextFrame = Pop2InstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `pop2 rejects a single category one operand stack value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Pop2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 88,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `pop2 rejects a category two value below a category one top`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Pop2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 88,
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
    fun `pop2 rejects top as a category one value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Pop2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 88,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Top),
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
    fun `pop2 rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Pop2InstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 88, locals = emptyList(), stack = emptyList()),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected category 1 or category 2 value",
            exception.message,
        )
    }

    @Test
    fun `pop2 rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            Pop2InstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 88,
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
