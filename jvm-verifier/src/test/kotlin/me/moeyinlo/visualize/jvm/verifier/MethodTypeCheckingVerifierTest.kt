package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class MethodTypeCheckingVerifierTest {
    @Test
    fun `type checking verifier accepts frames within limits and valid fixed length control flow`() {
        val code = code(
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0x1A.toByte(),
                0x99.toByte(), 0x00.toByte(), 0x03.toByte(),
                0xB1.toByte(),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = listOf(VerificationType.Integer),
                    stack = emptyList(),
                ),
                VerificationFrameState(
                    bytecodeOffset = 4,
                    locals = listOf(VerificationType.Integer),
                    stack = emptyList(),
                ),
            ),
        )
    }

    @Test
    fun `type checking verifier rejects frames exceeding code resource limits`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(maxStack = 1, maxLocals = 1),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 3,
                        locals = listOf(VerificationType.Integer, VerificationType.Integer),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Frame at bytecode offset 3 uses 2 local variable unit(s), exceeding max_locals=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects invalid fixed length control flow`() {
        val exception = assertFailsWith<ControlFlowGraphException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0xA7.toByte(), 0x00.toByte(), 0x02.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = emptyList(),
            )
        }

        assertEquals(
            "Invalid branch target 2 from instruction 0",
            exception.message,
        )
    }

    private fun code(
        maxStack: Int,
        maxLocals: Int,
        code: ByteArray = byteArrayOf(0xB1.toByte()),
    ): CodeAttribute =
        CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = maxStack,
            maxLocals = maxLocals,
            code = code,
        )
}
