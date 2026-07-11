package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class GetStaticInstructionVerifierTest {
    @Test
    fun `getstatic pushes the declared field type onto the operand stack`() {
        val frame = frame(stack = listOf(VerificationType.Float))

        val nextFrame = GetStaticInstructionVerifier.verify(
            frame = frame,
            fieldType = VerificationType.Long,
            maxStack = 3,
        )

        assertEquals(
            frame(stack = listOf(VerificationType.Float, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `getstatic preserves reference field precision`() {
        val fieldType = VerificationType.ObjectType(ConstantPoolIndex(7))
        val frame = frame(stack = emptyList())

        val nextFrame = GetStaticInstructionVerifier.verify(
            frame = frame,
            fieldType = fieldType,
            maxStack = 1,
        )

        assertEquals(frame(stack = listOf(fieldType)), nextFrame)
    }

    @Test
    fun `getstatic rejects operand stack overflow from the field type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            GetStaticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                fieldType = VerificationType.Double,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    @Test
    fun `getstatic rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            GetStaticInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
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
        VerificationFrameState(bytecodeOffset = 178, locals = emptyList(), stack = stack)
}
