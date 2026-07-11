package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class PopInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(21))

    @Test
    fun `pop removes a category one operand stack top`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 87,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, VerificationType.Integer),
        )

        val nextFrame = PopInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `pop accepts a reference category one operand stack top`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 87,
            locals = emptyList(),
            stack = listOf(VerificationType.Integer, objectType),
        )

        val nextFrame = PopInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `pop rejects a category two operand stack top`() {
        val exception = assertFailsWith<MethodVerificationException> {
            PopInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 87,
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
    fun `pop rejects top because it may denote a category two upper half`() {
        val exception = assertFailsWith<MethodVerificationException> {
            PopInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 87,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Top),
                ),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Top, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `pop rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            PopInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 87, locals = emptyList(), stack = emptyList()),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `pop rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            PopInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 87,
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
