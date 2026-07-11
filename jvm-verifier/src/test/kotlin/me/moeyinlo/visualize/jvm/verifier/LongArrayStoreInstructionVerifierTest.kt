package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongArrayStoreInstructionVerifierTest {
    @Test
    fun `lastore pops long array reference int index and long value`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val frame = VerificationFrameState(
            bytecodeOffset = 80,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, longArrayType, VerificationType.Integer, VerificationType.Long),
        )

        val nextFrame = LongArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 5)

        assertEquals(frame.copy(stack = listOf(VerificationType.Float)), nextFrame)
    }

    @Test
    fun `lastore accepts null as a long array reference`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer, VerificationType.Long))

        val nextFrame = LongArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `lastore rejects a non long value`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(longArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lastore rejects a non int index`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(longArrayType, VerificationType.Float, VerificationType.Long)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `lastore rejects an array with a non long component`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer, VerificationType.Long)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected $longArrayType",
            exception.message,
        )
    }

    @Test
    fun `lastore rejects a missing array reference`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $longArrayType",
            exception.message,
        )
    }

    @Test
    fun `lastore rejects an incoming stack exceeding max stack`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, longArrayType, VerificationType.Integer, VerificationType.Long)),
                maxStack = 5,
            )
        }

        assertEquals(
            "Operand stack depth 6 exceeds max_stack=5",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 80, locals = emptyList(), stack = stack)
}
