package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class SwapInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(65))

    @Test
    fun `swap exchanges the top two category one values`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 95,
            locals = emptyList(),
            stack = listOf(VerificationType.Null, VerificationType.Integer, objectType),
        )

        val nextFrame = SwapInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Null, objectType, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `swap rejects a category two top operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            SwapInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 95,
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
    fun `swap rejects top because it may denote a category two upper half`() {
        val exception = assertFailsWith<MethodVerificationException> {
            SwapInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 95,
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
    fun `swap rejects when the second operand is missing`() {
        val exception = assertFailsWith<MethodVerificationException> {
            SwapInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 95,
                    locals = emptyList(),
                    stack = listOf(objectType),
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
    fun `swap rejects a category two second operand`() {
        val exception = assertFailsWith<MethodVerificationException> {
            SwapInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 95,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, objectType),
                ),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected category 1 value",
            exception.message,
        )
    }
}
