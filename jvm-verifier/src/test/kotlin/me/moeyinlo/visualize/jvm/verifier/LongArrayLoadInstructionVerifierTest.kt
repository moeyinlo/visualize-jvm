package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongArrayLoadInstructionVerifierTest {
    @Test
    fun `laload replaces long array reference and int index with long result`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val frame = VerificationFrameState(
            bytecodeOffset = 47,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, longArrayType, VerificationType.Integer),
        )

        val nextFrame = LongArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `laload accepts null as a long array reference`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 47,
            locals = emptyList(),
            stack = listOf(VerificationType.Null, VerificationType.Integer),
        )

        val nextFrame = LongArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Long)), nextFrame)
    }

    @Test
    fun `laload rejects a non int index`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(longArrayType, VerificationType.Double)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Double, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `laload rejects an array with a non long component`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected $longArrayType",
            exception.message,
        )
    }

    @Test
    fun `laload rejects a missing array reference`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $longArrayType",
            exception.message,
        )
    }

    @Test
    fun `laload rejects an incoming stack exceeding max stack`() {
        val longArrayType = VerificationType.ArrayOf(VerificationType.Long)
        val exception = assertFailsWith<MethodVerificationException> {
            LongArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Double, longArrayType, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 47, locals = emptyList(), stack = stack)
}
