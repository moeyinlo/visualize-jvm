package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.CodeExceptionHandler
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
    fun `type checking verifier treats initial frame as stack map frame at offset zero`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x03.toByte(),
                    0x99.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                    0xB1.toByte(),
                ),
            ),
            initialFrame = MethodInitialFrame(
                locals = emptyList(),
                stack = emptyList(),
                flags = emptyList(),
                returnType = null,
            ),
            frameStates = emptyList(),
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

    @Test
    fun `type checking verifier rejects a branch target without a stack map frame`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0x03.toByte(),
                        0x99.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Branch target 5 has no stack map frame",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects an exception handler target without a stack map frame`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0xB1.toByte(),
                        0xB1.toByte(),
                    ),
                    exceptionTable = listOf(
                        CodeExceptionHandler(
                            startPc = 0,
                            endPc = 1,
                            handlerPc = 1,
                            catchType = null,
                        ),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Exception handler target 1 has no stack map frame",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects a frame offset without a matching instruction`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0x1A.toByte(),
                        0x99.toByte(), 0x00.toByte(), 0x03.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 2,
                        locals = listOf(VerificationType.Integer),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Frame at bytecode offset 2 does not correspond to an instruction offset",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects non increasing frame offsets`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0x1A.toByte(),
                        0x99.toByte(), 0x00.toByte(), 0x03.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 4,
                        locals = listOf(VerificationType.Integer),
                        stack = emptyList(),
                    ),
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = listOf(VerificationType.Integer),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Frame at bytecode offset 0 is not after previous frame offset 4",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ifeq operand stack transition`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x99.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = emptyList(),
                    ),
                    VerificationFrameState(
                        bytecodeOffset = 4,
                        locals = emptyList(),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies all int zero branch operand stack transitions`() {
        listOf(0x9A, 0x9B, 0x9C, 0x9D, 0x9E).forEach { opcode ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 0,
                        code = byteArrayOf(
                            opcode.toByte(), 0x00.toByte(), 0x04.toByte(),
                            0xB1.toByte(),
                            0xB1.toByte(),
                        ),
                    ),
                    frameStates = listOf(
                        VerificationFrameState(
                            bytecodeOffset = 0,
                            locals = emptyList(),
                            stack = emptyList(),
                        ),
                        VerificationFrameState(
                            bytecodeOffset = 4,
                            locals = emptyList(),
                            stack = emptyList(),
                        ),
                    ),
                )
            }

            assertEquals(
                "Operand stack is empty, expected Integer",
                exception.message,
            )
        }
    }

    private fun code(
        maxStack: Int,
        maxLocals: Int,
        code: ByteArray = byteArrayOf(0xB1.toByte()),
        exceptionTable: List<CodeExceptionHandler> = emptyList(),
    ): CodeAttribute =
        CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = maxStack,
            maxLocals = maxLocals,
            code = code,
            exceptionTable = exceptionTable,
        )
}
