package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteArrayStoreInstructionVerifierTest {
    @Test
    fun `bastore pops byte array reference int index and int value`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val frame = VerificationFrameState(
            bytecodeOffset = 84,
            locals = listOf(VerificationType.Float),
            stack = listOf(VerificationType.Long, byteArrayType, VerificationType.Integer, VerificationType.Integer),
        )

        val nextFrame = ByteArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(frame.copy(stack = listOf(VerificationType.Long)), nextFrame)
    }

    @Test
    fun `bastore accepts boolean arrays`() {
        val booleanArrayType = VerificationType.ArrayOf(VerificationType.Boolean)
        val frame = frame(stack = listOf(booleanArrayType, VerificationType.Integer, VerificationType.Integer))

        val nextFrame = ByteArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `bastore accepts null as a byte or boolean array reference`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer, VerificationType.Integer))

        val nextFrame = ByteArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `bastore rejects a non int value`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(byteArrayType, VerificationType.Integer, VerificationType.Long)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `bastore rejects a non int index`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(byteArrayType, VerificationType.Float, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `bastore rejects an int array`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected byte boolean or null array reference",
            exception.message,
        )
    }

    @Test
    fun `bastore rejects a char array`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(charArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $charArrayType, expected byte boolean or null array reference",
            exception.message,
        )
    }

    @Test
    fun `bastore rejects a missing array reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected byte boolean or null array reference",
            exception.message,
        )
    }

    @Test
    fun `bastore rejects an incoming stack exceeding max stack`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, byteArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 84, locals = emptyList(), stack = stack)
}
