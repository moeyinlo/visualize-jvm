package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class PutStaticInstructionVerifierTest {
    @Test
    fun `putstatic pops a value matching the declared field type`() {
        val frame = frame(stack = listOf(VerificationType.Float, VerificationType.Integer))

        val nextFrame = PutStaticInstructionVerifier.verify(
            frame = frame,
            fieldType = VerificationType.Integer,
            maxStack = 2,
        )

        assertEquals(frame(stack = listOf(VerificationType.Float)), nextFrame)
    }

    @Test
    fun `putstatic accepts a reference subtype value`() {
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(11))
        val frame = frame(stack = listOf(objectType))

        val nextFrame = PutStaticInstructionVerifier.verify(
            frame = frame,
            fieldType = VerificationType.Reference,
            maxStack = 1,
        )

        assertEquals(frame(stack = emptyList()), nextFrame)
    }

    @Test
    fun `putstatic rejects a non assignable field value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            PutStaticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                fieldType = VerificationType.Long,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `putstatic rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            PutStaticInstructionVerifier.verify(
                frame = frame(stack = emptyList()),
                fieldType = VerificationType.Integer,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `putstatic rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            PutStaticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Double, VerificationType.Integer)),
                fieldType = VerificationType.Integer,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 179, locals = emptyList(), stack = stack)
}
