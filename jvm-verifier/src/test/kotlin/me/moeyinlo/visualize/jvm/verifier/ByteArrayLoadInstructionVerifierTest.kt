package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ByteArrayLoadInstructionVerifierTest {
    @Test
    fun `baload replaces byte array reference and int index with int value`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val frame = VerificationFrameState(
            bytecodeOffset = 51,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, byteArrayType, VerificationType.Integer),
        )

        val nextFrame = ByteArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `baload accepts boolean arrays and pushes int value`() {
        val booleanArrayType = VerificationType.ArrayOf(VerificationType.Boolean)
        val frame = frame(stack = listOf(booleanArrayType, VerificationType.Integer))

        val nextFrame = ByteArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `baload accepts null array reference and pushes int value`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer))

        val nextFrame = ByteArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `baload rejects a non int index`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(byteArrayType, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `baload rejects an int array`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected byte boolean or null array reference",
            exception.message,
        )
    }

    @Test
    fun `baload rejects a reference array`() {
        val objectArrayType = VerificationType.ArrayOf(
            VerificationType.ObjectType(ConstantPoolIndex(17)),
        )
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(objectArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $objectArrayType, expected byte boolean or null array reference",
            exception.message,
        )
    }

    @Test
    fun `baload rejects a missing array reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected byte boolean or null array reference",
            exception.message,
        )
    }

    @Test
    fun `baload rejects an incoming stack exceeding max stack`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val exception = assertFailsWith<MethodVerificationException> {
            ByteArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, byteArrayType, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 51, locals = emptyList(), stack = stack)
}
