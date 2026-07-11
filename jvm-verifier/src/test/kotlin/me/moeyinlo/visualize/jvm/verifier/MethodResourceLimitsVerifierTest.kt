package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class MethodResourceLimitsVerifierTest {
    @Test
    fun `accepts frames within declared local and stack limits`() {
        val code = code(maxStack = 3, maxLocals = 3)

        MethodResourceLimitsVerifier.verify(
            code = code,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = listOf(VerificationType.Integer, VerificationType.Long),
                    stack = listOf(VerificationType.Null, VerificationType.Double),
                ),
            ),
        )
    }

    @Test
    fun `rejects frame locals exceeding max locals`() {
        val code = code(maxStack = 1, maxLocals = 2)

        val exception = assertFailsWith<MethodVerificationException> {
            MethodResourceLimitsVerifier.verify(
                code = code,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 7,
                        locals = listOf(VerificationType.Integer, VerificationType.Long),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Frame at bytecode offset 7 uses 3 local variable unit(s), exceeding max_locals=2",
            exception.message,
        )
    }

    @Test
    fun `rejects operand stack exceeding max stack`() {
        val code = code(maxStack = 2, maxLocals = 1)

        val exception = assertFailsWith<MethodVerificationException> {
            MethodResourceLimitsVerifier.verify(
                code = code,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 12,
                        locals = listOf(VerificationType.Integer),
                        stack = listOf(VerificationType.ObjectType(ConstantPoolIndex(2)), VerificationType.Double),
                    ),
                ),
            )
        }

        assertEquals(
            "Frame at bytecode offset 12 uses 3 operand stack unit(s), exceeding max_stack=2",
            exception.message,
        )
    }

    private fun code(maxStack: Int, maxLocals: Int): CodeAttribute =
        CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = maxStack,
            maxLocals = maxLocals,
            code = byteArrayOf(0xB1.toByte()),
        )
}
