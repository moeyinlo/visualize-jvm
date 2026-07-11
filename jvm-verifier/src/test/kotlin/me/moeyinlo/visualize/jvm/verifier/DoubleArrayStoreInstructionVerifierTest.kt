package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleArrayStoreInstructionVerifierTest {
    @Test
    fun `dastore pops double array reference int index and double value`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val frame = VerificationFrameState(
            bytecodeOffset = 82,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, doubleArrayType, VerificationType.Integer, VerificationType.Double),
        )

        val nextFrame = DoubleArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(frame.copy(stack = listOf(VerificationType.Float)), nextFrame)
    }

    @Test
    fun `dastore accepts null as a double array reference`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer, VerificationType.Double))

        val nextFrame = DoubleArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `dastore rejects a non double value`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(doubleArrayType, VerificationType.Integer, VerificationType.Float)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dastore rejects a non int index`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(doubleArrayType, VerificationType.Long, VerificationType.Double)),
                maxStack = 5,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `dastore rejects an array with a non double component`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(floatArrayType, VerificationType.Integer, VerificationType.Double)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains $floatArrayType, expected $doubleArrayType",
            exception.message,
        )
    }

    @Test
    fun `dastore rejects a missing array reference`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Double)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $doubleArrayType",
            exception.message,
        )
    }

    @Test
    fun `dastore rejects an incoming stack exceeding max stack`() {
        val doubleArrayType = VerificationType.ArrayOf(VerificationType.Double)
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, doubleArrayType, VerificationType.Integer, VerificationType.Double)),
                maxStack = 5,
            )
        }

        assertEquals(
            "Operand stack depth 6 exceeds max_stack=5",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 82, locals = emptyList(), stack = stack)
}
