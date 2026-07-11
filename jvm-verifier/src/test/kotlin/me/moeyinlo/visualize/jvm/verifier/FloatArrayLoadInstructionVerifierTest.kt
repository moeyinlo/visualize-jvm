package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatArrayLoadInstructionVerifierTest {
    @Test
    fun `faload replaces float array reference and int index with float result`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val frame = VerificationFrameState(
            bytecodeOffset = 48,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Long, floatArrayType, VerificationType.Integer),
        )

        val nextFrame = FloatArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Long, VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `faload accepts null as a float array reference`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 48,
            locals = emptyList(),
            stack = listOf(VerificationType.Null, VerificationType.Integer),
        )

        val nextFrame = FloatArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Float)), nextFrame)
    }

    @Test
    fun `faload rejects a non int index`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(floatArrayType, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `faload rejects an array with a non float component`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected $floatArrayType",
            exception.message,
        )
    }

    @Test
    fun `faload rejects a missing array reference`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $floatArrayType",
            exception.message,
        )
    }

    @Test
    fun `faload rejects an incoming stack exceeding max stack`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val exception = assertFailsWith<MethodVerificationException> {
            FloatArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, floatArrayType, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 48, locals = emptyList(), stack = stack)
}
