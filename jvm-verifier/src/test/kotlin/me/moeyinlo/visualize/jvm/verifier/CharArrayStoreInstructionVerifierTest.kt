package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharArrayStoreInstructionVerifierTest {
    @Test
    fun `castore pops char array reference int index and int value`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val frame = VerificationFrameState(
            bytecodeOffset = 85,
            locals = listOf(VerificationType.Float),
            stack = listOf(VerificationType.Long, charArrayType, VerificationType.Integer, VerificationType.Integer),
        )

        val nextFrame = CharArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(frame.copy(stack = listOf(VerificationType.Long)), nextFrame)
    }

    @Test
    fun `castore accepts null as a char array reference`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer, VerificationType.Integer))

        val nextFrame = CharArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `castore rejects a non int value`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(charArrayType, VerificationType.Integer, VerificationType.Long)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `castore rejects a non int index`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(charArrayType, VerificationType.Float, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `castore rejects an int array`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected $charArrayType",
            exception.message,
        )
    }

    @Test
    fun `castore rejects a byte array`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(byteArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $byteArrayType, expected $charArrayType",
            exception.message,
        )
    }

    @Test
    fun `castore rejects a missing array reference`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $charArrayType",
            exception.message,
        )
    }

    @Test
    fun `castore rejects an incoming stack exceeding max stack`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            CharArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, charArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 85, locals = emptyList(), stack = stack)
}
