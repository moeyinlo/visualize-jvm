package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ReferenceArrayLoadInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(17))

    @Test
    fun `aaload replaces object array reference and int index with component type`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val frame = VerificationFrameState(
            bytecodeOffset = 50,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Long, objectArrayType, VerificationType.Integer),
        )

        val nextFrame = ReferenceArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Long, objectType)),
            nextFrame,
        )
    }

    @Test
    fun `aaload preserves nested array component type`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val nestedArrayType = VerificationType.ArrayOf(intArrayType)
        val frame = frame(stack = listOf(nestedArrayType, VerificationType.Integer))

        val nextFrame = ReferenceArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(intArrayType)), nextFrame)
    }

    @Test
    fun `aaload accepts null array reference and pushes null`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer))

        val nextFrame = ReferenceArrayLoadInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(frame.copy(stack = listOf(VerificationType.Null)), nextFrame)
    }

    @Test
    fun `aaload rejects a non int index`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(objectArrayType, VerificationType.Long)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `aaload rejects an array with a primitive component`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected array reference with reference component",
            exception.message,
        )
    }

    @Test
    fun `aaload rejects a non array reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(objectType, VerificationType.Integer)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains $objectType, expected array reference with reference component",
            exception.message,
        )
    }

    @Test
    fun `aaload rejects a missing array reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected array reference with reference component",
            exception.message,
        )
    }

    @Test
    fun `aaload rejects an incoming stack exceeding max stack`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayLoadInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, objectArrayType, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 50, locals = emptyList(), stack = stack)
}
