package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class CheckCastInstructionVerifierTest {
    private val objectRef = VerificationType.ObjectType(ConstantPoolIndex(31))
    private val stringType = VerificationType.ClassType("java/lang/String")
    private val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)

    @Test
    fun `checkcast replaces an object reference with the target class type`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 48,
            locals = emptyList(),
            stack = listOf(VerificationType.Float, objectRef),
        )

        val nextFrame = CheckCastInstructionVerifier.verify(
            frame = frame,
            targetType = stringType,
            maxStack = 2,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, stringType)),
            nextFrame,
        )
    }

    @Test
    fun `checkcast replaces null with the target array type in the verifier state`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 48,
            locals = emptyList(),
            stack = listOf(VerificationType.Null),
        )

        val nextFrame = CheckCastInstructionVerifier.verify(
            frame = frame,
            targetType = intArrayType,
            maxStack = 1,
        )

        assertEquals(frame.copy(stack = listOf(intArrayType)), nextFrame)
    }

    @Test
    fun `checkcast rejects non reference operands`() {
        val exception = assertFailsWith<MethodVerificationException> {
            CheckCastInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 48,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                targetType = stringType,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `checkcast rejects non class and non array targets`() {
        val exception = assertFailsWith<MethodVerificationException> {
            CheckCastInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 48,
                    locals = emptyList(),
                    stack = listOf(objectRef),
                ),
                targetType = VerificationType.Integer,
                maxStack = 1,
            )
        }

        assertEquals(
            "checkcast target Integer is not a class or array type",
            exception.message,
        )
    }
}
