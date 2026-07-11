package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharArrayLoadInstructionVerifierTest {
    @Test
    fun `caload replaces char array reference and int index with int value`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val frame = VerificationFrameState(
            bytecodeOffset = 52,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, charArrayType, VerificationType.Integer),
        )

        val nextFrame = CharArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `caload accepts null array reference and pushes int value`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer))

        val nextFrame = CharArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `caload rejects a non int index`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(charArrayType, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `caload rejects an int array`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected char or null array reference",
            exception.message,
        )
    }

    @Test
    fun `caload rejects a byte array`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(byteArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $byteArrayType, expected char or null array reference",
            exception.message,
        )
    }

    @Test
    fun `caload rejects a missing array reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected char or null array reference",
            exception.message,
        )
    }

    @Test
    fun `caload rejects an incoming stack exceeding max stack`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, charArrayType, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 52, locals = emptyList(), stack = stack)
}
