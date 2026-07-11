package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleArrayLoadInstructionVerifierTest {
    @Test
    fun `daload replaces double array reference and int index with double result`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val frame = VerificationFrameState(
            bytecodeOffset = 49,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, doubleArrayType, VerificationType.Integer),
        )

        val nextFrame = DoubleArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `daload accepts null as a double array reference`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 49,
            locals = emptyList(),
            stack = listOf(VerificationType.Null, VerificationType.Integer),
        )

        val nextFrame = DoubleArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Double)), nextFrame)
    }

    @Test
    fun `daload rejects a non int index`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(doubleArrayType, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `daload rejects an array with a non double component`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected $doubleArrayType",
            exception.message,
        )
    }

    @Test
    fun `daload rejects a missing array reference`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $doubleArrayType",
            exception.message,
        )
    }

    @Test
    fun `daload rejects an incoming stack exceeding max stack`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, doubleArrayType, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 49, locals = emptyList(), stack = stack)
}
