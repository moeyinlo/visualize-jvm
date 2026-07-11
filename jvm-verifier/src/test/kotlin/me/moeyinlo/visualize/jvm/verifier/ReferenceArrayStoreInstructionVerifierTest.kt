package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ReferenceArrayStoreInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))
    private val stringType = VerificationType.ObjectType(ConstantPoolIndex(2))

    @Test
    fun `aastore pops reference array int index and reference value`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val frame = VerificationFrameState(
            bytecodeOffset = 83,
            locals = listOf(VerificationType.Integer),
            stack = listOf(VerificationType.Float, objectArrayType, VerificationType.Integer, stringType),
        )

        val nextFrame = ReferenceArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 4)

        assertEquals(frame.copy(stack = listOf(VerificationType.Float)), nextFrame)
    }

    @Test
    fun `aastore accepts null as the value`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val frame = frame(stack = listOf(objectArrayType, VerificationType.Integer, VerificationType.Null))

        val nextFrame = ReferenceArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `aastore accepts null as a reference array reference`() {
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer, objectType))

        val nextFrame = ReferenceArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `aastore accepts array values because arrays are references`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val nestedObjectArrayType = VerificationType.ArrayOf(objectArrayType)
        val frame = frame(stack = listOf(objectArrayType, VerificationType.Integer, nestedObjectArrayType))

        val nextFrame = ReferenceArrayStoreInstructionVerifier.verify(frame = frame, maxStack = 3)

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `aastore rejects a non reference value`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(objectArrayType, VerificationType.Integer, VerificationType.Integer)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `aastore rejects a non int index`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(objectArrayType, VerificationType.Long, objectType)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `aastore rejects an array with a primitive component`() {
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(intArrayType, VerificationType.Integer, objectType)),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains $intArrayType, expected array reference with reference component",
            exception.message,
        )
    }

    @Test
    fun `aastore rejects a missing array reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, objectType)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected array reference with reference component",
            exception.message,
        )
    }

    @Test
    fun `aastore rejects an incoming stack exceeding max stack`() {
        val objectArrayType = VerificationType.ArrayOf(objectType)
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceArrayStoreInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, objectArrayType, VerificationType.Integer, objectType)),
                maxStack = 4,
            )
        }

        assertEquals(
            "Operand stack depth 5 exceeds max_stack=4",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 83, locals = emptyList(), stack = stack)
}
