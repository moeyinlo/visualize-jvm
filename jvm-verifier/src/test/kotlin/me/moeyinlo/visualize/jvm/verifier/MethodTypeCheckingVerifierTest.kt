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

    @Test
    fun `type checking verifier applies int compare branch operand stack transitions`() {
        listOf(0x9F, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4).forEach { opcode ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 2,
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
                            stack = listOf(VerificationType.Integer),
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

    @Test
    fun `type checking verifier applies reference compare branch operand stack transitions`() {
        listOf(0xA5, 0xA6).forEach { opcode ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 2,
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
                            stack = listOf(VerificationType.Reference),
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
                "Operand stack is empty, expected Reference",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies reference null branch operand stack transitions`() {
        listOf(0xC6, 0xC7).forEach { opcode ->
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
                "Operand stack is empty, expected Reference",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies implicit int local load transitions`() {
        listOf(0x1A to 0, 0x1B to 1, 0x1C to 2, 0x1D to 3).forEach { (opcode, index) ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 4,
                        code = byteArrayOf(
                            opcode.toByte(),
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
                "Local variable $index contains Top, expected Integer",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies implicit long local load transitions`() {
        listOf(0x1E to 0, 0x1F to 1, 0x20 to 2, 0x21 to 3).forEach { (opcode, index) ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 2,
                        maxLocals = 5,
                        code = byteArrayOf(
                            opcode.toByte(),
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
                "Local variable $index contains Top, expected Long",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies implicit float local load transitions`() {
        listOf(0x22 to 0, 0x23 to 1, 0x24 to 2, 0x25 to 3).forEach { (opcode, index) ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 4,
                        code = byteArrayOf(
                            opcode.toByte(),
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
                "Local variable $index contains Top, expected Float",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies implicit double local load transitions`() {
        listOf(0x26 to 0, 0x27 to 1, 0x28 to 2, 0x29 to 3).forEach { (opcode, index) ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 2,
                        maxLocals = 5,
                        code = byteArrayOf(
                            opcode.toByte(),
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
                "Local variable $index contains Top, expected Double",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies implicit reference local load transitions`() {
        listOf(0x2A to 0, 0x2B to 1, 0x2C to 2, 0x2D to 3).forEach { (opcode, index) ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 4,
                        code = byteArrayOf(
                            opcode.toByte(),
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
                "Local variable $index contains Top, expected Reference",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies explicit int local load transitions`() {
        listOf(0, 1, 2, 3).forEach { index ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 4,
                        code = byteArrayOf(
                            0x15.toByte(),
                            index.toByte(),
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
                "Local variable $index contains Top, expected Integer",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies explicit long local load transitions`() {
        listOf(0, 1, 2, 3).forEach { index ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 2,
                        maxLocals = 5,
                        code = byteArrayOf(
                            0x16.toByte(),
                            index.toByte(),
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
                "Local variable $index contains Top, expected Long",
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
