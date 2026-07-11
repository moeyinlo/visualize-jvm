package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntArrayLoadInstructionVerifierTest {
    @Test
    fun `iaload replaces int array reference and int index with int result`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val frame = VerificationFrameState(
            bytecodeOffset = 46,
            locals = listOf(VerificationType.Float),
            stack = listOf(VerificationType.Long, intArrayType, VerificationType.Integer),
        )

        val nextFrame = IntArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Long, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `iaload accepts null as an int array reference`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 46,
            locals = emptyList(),
            stack = listOf(VerificationType.Null, VerificationType.Integer),
        )

        val nextFrame = IntArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `iaload rejects a non int index`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `iaload rejects an array with a non int component`() {
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(floatArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $floatArrayType, expected $intArrayType",
            exception.message,
        )
    }

    @Test
    fun `iaload rejects a missing array reference`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected $intArrayType",
            exception.message,
        )
    }

    @Test
    fun `iaload rejects an incoming stack exceeding max stack`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            IntArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, intArrayType, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 46, locals = emptyList(), stack = stack)
}
