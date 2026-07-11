package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ArrayLengthInstructionVerifierTest {
    private val objectArrayType = VerificationType.ArrayOf(
        VerificationType.ObjectType(ConstantPoolIndex(7)),
    )

    @Test
    fun `arraylength replaces an array reference with int on the operand stack`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 33,
            locals = emptyList(),
            stack = listOf(VerificationType.Float, objectArrayType),
        )

        val nextFrame = ArrayLengthInstructionVerifier.verify(frame = frame, maxStack = 2)

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `arraylength accepts null as an array reference`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 33,
            locals = emptyList(),
            stack = listOf(VerificationType.Null),
        )

        val nextFrame = ArrayLengthInstructionVerifier.verify(frame = frame, maxStack = 1)

        assertEquals(frame.copy(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `arraylength rejects a non array reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ArrayLengthInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 33,
                    locals = emptyList(),
                    stack = listOf(VerificationType.ObjectType(ConstantPoolIndex(9))),
                ),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains ObjectType(constantPoolIndex=#9), expected array or null reference",
            exception.message,
        )
    }
}
