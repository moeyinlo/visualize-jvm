package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShortArrayStoreInstructionVerifierTest {
    @Test
    fun `sastore pops short array reference int index and int value`() {
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val frame = VerificationFrameState(
            bytecodeOffset = 86,
            locals = listOf(VerificationType.Float),
            stack = listOf(VerificationType.Long, shortArrayType, VerificationType.Integer, VerificationType.Integer),
        )

        val nextFrame = ShortArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(frame.copy(stack = listOf(VerificationType.Long)), nextFrame)
    }

    @Test
    fun `sastore accepts null as a short array reference`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer, VerificationType.Integer))

        val nextFrame = ShortArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `sastore rejects a non int value`() {
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(shortArrayType, VerificationType.Integer, VerificationType.Long)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `sastore rejects a non int index`() {
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(shortArrayType, VerificationType.Float, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `sastore rejects an int array`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected $shortArrayType",
            exception.message,
        )
    }

    @Test
    fun `sastore rejects a char array`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(charArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $charArrayType, expected $shortArrayType",
            exception.message,
        )
    }

    @Test
    fun `sastore rejects a missing array reference`() {
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $shortArrayType",
            exception.message,
        )
    }

    @Test
    fun `sastore rejects an incoming stack exceeding max stack`() {
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, shortArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 86, locals = emptyList(), stack = stack)
}
