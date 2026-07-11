package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntArrayStoreInstructionVerifierTest {
    @Test
    fun `iastore pops int array reference int index and int value`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val frame = VerificationFrameState(
            bytecodeOffset = 79,
            locals = listOf(VerificationType.Float),
            stack = listOf(VerificationType.Long, intArrayType, VerificationType.Integer, VerificationType.Integer),
        )

        val nextFrame = IntArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(frame.copy(stack = listOf(VerificationType.Long)), nextFrame)
    }

    @Test
    fun `iastore accepts null as an int array reference`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer, VerificationType.Integer))

        val nextFrame = IntArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `iastore rejects a non int value`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer, VerificationType.Long)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `iastore rejects a non int index`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Float, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `iastore rejects an array with a non int component`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(floatArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $floatArrayType, expected $intArrayType",
            exception.message,
        )
    }

    @Test
    fun `iastore rejects a missing array reference`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $intArrayType",
            exception.message,
        )
    }

    @Test
    fun `iastore rejects an incoming stack exceeding max stack`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, intArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 79, locals = emptyList(), stack = stack)
}
