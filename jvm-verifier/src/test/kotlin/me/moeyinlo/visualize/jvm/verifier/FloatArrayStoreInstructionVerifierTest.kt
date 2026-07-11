package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatArrayStoreInstructionVerifierTest {
    @Test
    fun `fastore pops float array reference int index and float value`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val frame = VerificationFrameState(
            bytecodeOffset = 81,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Long, floatArrayType, VerificationType.Integer, VerificationType.Float),
        )

        val nextFrame = FloatArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(frame.copy(stack = listOf(VerificationType.Long)), nextFrame)
    }

    @Test
    fun `fastore accepts null as a float array reference`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer, VerificationType.Float))

        val nextFrame = FloatArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `fastore rejects a non float value`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(floatArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `fastore rejects a non int index`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(floatArrayType, VerificationType.Long, VerificationType.Float)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `fastore rejects an array with a non float component`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer, VerificationType.Float)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected $floatArrayType",
            exception.message,
        )
    }

    @Test
    fun `fastore rejects a missing array reference`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $floatArrayType",
            exception.message,
        )
    }

    @Test
    fun `fastore rejects an incoming stack exceeding max stack`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, floatArrayType, VerificationType.Integer, VerificationType.Float)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 81, locals = emptyList(), stack = stack)
}
