package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShortArrayLoadInstructionVerifierTest {
    @Test
    fun `saload replaces short array reference and int index with int value`() {
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val frame = VerificationFrameState(
            bytecodeOffset = 53,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, shortArrayType, VerificationType.Integer),
        )

        val nextFrame = ShortArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `saload accepts null array reference and pushes int value`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer))

        val nextFrame = ShortArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `saload rejects a non int index`() {
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(shortArrayType, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `saload rejects an int array`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected short or null array reference",
            exception.message,
        )
    }

    @Test
    fun `saload rejects a char array`() {
        val charArrayType = VerificationType.ArrayOf(VerificationType.Char)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(charArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $charArrayType, expected short or null array reference",
            exception.message,
        )
    }

    @Test
    fun `saload rejects a missing array reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected short or null array reference",
            exception.message,
        )
    }

    @Test
    fun `saload rejects an incoming stack exceeding max stack`() {
        val shortArrayType = VerificationType.ArrayOf(VerificationType.Short)
        val exception = assertFailsWith<MethodVerificationException> {
            ShortArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, shortArrayType, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 53, locals = emptyList(), stack = stack)
}
