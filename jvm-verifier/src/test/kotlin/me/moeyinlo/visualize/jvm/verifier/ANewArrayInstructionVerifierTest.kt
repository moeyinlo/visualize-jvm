package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ANewArrayInstructionVerifierTest {
    @Test
    fun `anewarray replaces int count with object array reference`() {
        val componentType = VerificationType.ObjectType(ConstantPoolIndex(7))
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer))

        val nextFrame = ANewArrayInstructionVerifier.verify(
            frame = frame,
            componentType = componentType,
            maxStack = 2,
        )

        assertEquals(
            frame(stack = listOf(VerificationType.Float, VerificationType.ArrayOf(componentType))),
            nextFrame,
        )
    }

    @Test
    fun `anewarray accepts array component targets`() {
        val componentType = VerificationType.ArrayOf(VerificationType.Integer)

        val nextFrame = ANewArrayInstructionVerifier.verify(
            frame = frame(stack = listOf(VerificationType.Integer)),
            componentType = componentType,
            maxStack = 1,
        )

        assertEquals(
            frame(stack = listOf(VerificationType.ArrayOf(componentType))),
            nextFrame,
        )
    }

    @Test
    fun `anewarray rejects primitive component targets`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ANewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                componentType = VerificationType.Integer,
                maxStack = 1,
            )
        }

        assertEquals(
            "anewarray component Integer is not a class, interface, or array type",
            exception.message,
        )
    }

    @Test
    fun `anewarray rejects non int count`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ANewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float)),
                componentType = VerificationType.ObjectType(ConstantPoolIndex(7)),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `anewarray rejects incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ANewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
                componentType = VerificationType.ObjectType(ConstantPoolIndex(7)),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 189, locals = emptyList(), stack = stack)
}
