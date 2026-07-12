package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.BootstrapMethodIndex
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.CodeExceptionHandler
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.MethodHandleReferenceKind

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
    fun `type checking verifier applies aconst_null operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x01.toByte(),
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

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies iconst operand stack transitions`() {
        (0x02..0x08).forEach { opcode ->
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
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

            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 0,
                        code = byteArrayOf(
                            opcode.toByte(),
                            0xB1.toByte(),
                        ),
                    ),
                    frameStates = listOf(
                        VerificationFrameState(
                            bytecodeOffset = 0,
                            locals = emptyList(),
                            stack = listOf(VerificationType.Integer),
                        ),
                    ),
                )
            }

            assertEquals(
                "Operand stack depth 2 exceeds max_stack=1",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies lconst operand stack transitions`() {
        (0x09..0x0A).forEach { opcode ->
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
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

            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 2,
                        maxLocals = 0,
                        code = byteArrayOf(
                            opcode.toByte(),
                            0xB1.toByte(),
                        ),
                    ),
                    frameStates = listOf(
                        VerificationFrameState(
                            bytecodeOffset = 0,
                            locals = emptyList(),
                            stack = listOf(VerificationType.Integer),
                        ),
                    ),
                )
            }

            assertEquals(
                "Operand stack depth 3 exceeds max_stack=2",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies fconst operand stack transitions`() {
        (0x0B..0x0D).forEach { opcode ->
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
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

            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 0,
                        code = byteArrayOf(
                            opcode.toByte(),
                            0xB1.toByte(),
                        ),
                    ),
                    frameStates = listOf(
                        VerificationFrameState(
                            bytecodeOffset = 0,
                            locals = emptyList(),
                            stack = listOf(VerificationType.Integer),
                        ),
                    ),
                )
            }

            assertEquals(
                "Operand stack depth 2 exceeds max_stack=1",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies dconst operand stack transitions`() {
        (0x0E..0x0F).forEach { opcode ->
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
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

            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 2,
                        maxLocals = 0,
                        code = byteArrayOf(
                            opcode.toByte(),
                            0xB1.toByte(),
                        ),
                    ),
                    frameStates = listOf(
                        VerificationFrameState(
                            bytecodeOffset = 0,
                            locals = emptyList(),
                            stack = listOf(VerificationType.Integer),
                        ),
                    ),
                )
            }

            assertEquals(
                "Operand stack depth 3 exceeds max_stack=2",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies bipush operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x10.toByte(), 0x7F.toByte(),
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

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x10.toByte(), 0x80.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies sipush operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x11.toByte(), 0x7F.toByte(), 0xFF.toByte(),
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

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x11.toByte(), 0x80.toByte(), 0x00.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc integer operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(ConstantIntegerEntry(123)),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x01.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc_w integer operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(ConstantIntegerEntry(123)),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x13.toByte(), 0x00.toByte(), 0x01.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x13.toByte(), 0x00.toByte(), 0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc2_w long operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(ConstantLongEntry(123L)),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x14.toByte(), 0x00.toByte(), 0x01.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc2_w double operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(ConstantDoubleEntry(1.25)),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x14.toByte(), 0x00.toByte(), 0x01.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc2_w dynamic long operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("J", "J".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc2_w dynamic double operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("D", "D".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc float operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(ConstantFloatEntry(1.25f)),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x01.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc string operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("hello", "hello".encodeToByteArray()),
                ConstantStringEntry(ConstantPoolIndex(1)),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x02.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x02.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc class operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("java/lang/Object", "java/lang/Object".encodeToByteArray()),
                ConstantClassEntry(ConstantPoolIndex(1)),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x02.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x02.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc method type operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                ConstantMethodTypeEntry(ConstantPoolIndex(1)),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x02.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x02.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc method handle operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantMethodHandleEntry(MethodHandleReferenceKind.GetStatic, ConstantPoolIndex(1)),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x01.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic int operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic boolean operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("Z", "Z".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic byte operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("B", "B".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic char operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("C", "C".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic short operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("S", "S".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic float operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("F", "F".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic object operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val nextFrame = LdcInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 0,
                locals = emptyList(),
                stack = emptyList(),
            ),
            index = ConstantPoolIndex(4),
            constantPool = constantPool,
            maxStack = 1,
        )

        assertEquals(
            listOf(VerificationType.ClassType("java/lang/String")),
            nextFrame.stack,
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic int array operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("[I", "[I".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val nextFrame = LdcInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 0,
                locals = emptyList(),
                stack = emptyList(),
            ),
            index = ConstantPoolIndex(4),
            constantPool = constantPool,
            maxStack = 1,
        )

        assertEquals(
            listOf(VerificationType.ArrayOf(VerificationType.Integer)),
            nextFrame.stack,
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic object array operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("[Ljava/lang/String;", "[Ljava/lang/String;".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val nextFrame = LdcInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 0,
                locals = emptyList(),
                stack = emptyList(),
            ),
            index = ConstantPoolIndex(4),
            constantPool = constantPool,
            maxStack = 1,
        )

        assertEquals(
            listOf(VerificationType.ArrayOf(VerificationType.ClassType("java/lang/String"))),
            nextFrame.stack,
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldc dynamic nested int array operand stack transition`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("[[I", "[[I".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val expectedType = VerificationType.ArrayOf(
            VerificationType.ArrayOf(VerificationType.Integer),
        )

        val nextFrame = LdcInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 0,
                locals = emptyList(),
                stack = emptyList(),
            ),
            index = ConstantPoolIndex(4),
            constantPool = constantPool,
            maxStack = 1,
        )

        assertEquals(listOf(expectedType), nextFrame.stack)

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x12.toByte(), 0x04.toByte(),
                    0xB1.toByte(),
                ),
            ),
            constantPool = constantPool,
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )
    }

    @Test
    fun `type checking verifier rejects ldc dynamic long descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("J", "J".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
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
            "ldc CONSTANT_Dynamic descriptor 'J' is category 2; use ldc2_w",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects ldc dynamic double descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("D", "D".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
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
            "ldc CONSTANT_Dynamic descriptor 'D' is category 2; use ldc2_w",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects ldc dynamic void descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("V", "V".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x12.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
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
            "Invalid field descriptor 'V': unsupported field type 'V' at offset 0",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects ldc2w dynamic int descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
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
            "ldc2_w CONSTANT_Dynamic descriptor 'I' is category 1; use ldc",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects ldc2w dynamic float descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("F", "F".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
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
            "ldc2_w CONSTANT_Dynamic descriptor 'F' is category 1; use ldc",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects ldc2w dynamic object descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
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
            "ldc2_w CONSTANT_Dynamic descriptor 'Ljava/lang/String;' is category 1; use ldc",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects ldc2w dynamic array descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("[I", "[I".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
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
            "ldc2_w CONSTANT_Dynamic descriptor '[I' is category 1; use ldc",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier rejects ldc2w dynamic void descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("V", "V".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(1),
                    descriptorIndex = ConstantPoolIndex(2),
                ),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(3),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x14.toByte(), 0x00.toByte(), 0x04.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                constantPool = constantPool,
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
            "Invalid field descriptor 'V': unsupported field type 'V' at offset 0",
            exception.message,
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
    fun `type checking verifier applies jsr operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0xA8.toByte(), 0x00.toByte(), 0x03.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
                VerificationFrameState(
                    bytecodeOffset = 3,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xA8.toByte(), 0x00.toByte(), 0x03.toByte(),
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
                        bytecodeOffset = 3,
                        locals = emptyList(),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies jsr_w operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0xC9.toByte(),
                    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x05.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
                VerificationFrameState(
                    bytecodeOffset = 5,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xC9.toByte(),
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x05.toByte(),
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
                        bytecodeOffset = 5,
                        locals = emptyList(),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
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
    fun `type checking verifier applies int array load operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x2E.toByte(),
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
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies long array load operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x2F.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Long)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies float array load operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x30.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Float)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies double array load operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x31.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Double)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies reference array load operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x32.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected array reference with reference component",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies byte array load operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x33.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected byte boolean or null array reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies char array load operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x34.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected char or null array reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies short array load operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x35.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected short or null array reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies int array store operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x4F.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Integer)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies long array store operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x50.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Long),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Long)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies float array store operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x51.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Float)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies double array store operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x52.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Double),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Double)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies reference array store operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x53.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Reference),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected array reference with reference component",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies byte array store operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x54.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected byte boolean or null array reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies char array store operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x55.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Char)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies short array store operand stack transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x56.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack is empty, expected ArrayOf(component=Short)",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies arraylength operand stack transition at explicit source frames`() {
        val objectArrayType = VerificationType.ArrayOf(
            VerificationType.ObjectType(ConstantPoolIndex(7)),
        )

        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0xBE.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(objectArrayType),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xBE.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.ObjectType(ConstantPoolIndex(9))),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains ObjectType(constantPoolIndex=#9), expected array or null reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies monitorenter operand stack transition at explicit source frames`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0xC2.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.ObjectType(ConstantPoolIndex(11))),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xC2.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies monitorexit operand stack transition at explicit source frames`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0xC3.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Null),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xC3.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies checkcast operand stack transition at explicit source frames`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0xC0.toByte(), 0x00.toByte(), 0x1F.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Null),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xC0.toByte(), 0x00.toByte(), 0x1F.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies instanceof operand stack transition at explicit source frames`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0xC1.toByte(), 0x00.toByte(), 0x1F.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Null),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xC1.toByte(), 0x00.toByte(), 0x1F.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier accepts stack manipulation operand stack transitions`() {
        listOf(
            StackTransitionCase("pop", 0x57, 1, listOf(VerificationType.Integer)),
            StackTransitionCase("pop2", 0x58, 2, listOf(VerificationType.Long)),
            StackTransitionCase("dup", 0x59, 2, listOf(VerificationType.Integer)),
            StackTransitionCase("dup_x1", 0x5A, 3, listOf(VerificationType.Integer, VerificationType.Float)),
            StackTransitionCase("dup_x2", 0x5B, 4, listOf(VerificationType.Long, VerificationType.Integer)),
            StackTransitionCase("dup2", 0x5C, 4, listOf(VerificationType.Long)),
            StackTransitionCase("dup2_x1", 0x5D, 5, listOf(VerificationType.Integer, VerificationType.Long)),
            StackTransitionCase("dup2_x2", 0x5E, 6, listOf(VerificationType.Double, VerificationType.Long)),
            StackTransitionCase("swap", 0x5F, 2, listOf(VerificationType.Integer, VerificationType.Float)),
        ).forEach { case ->
            verifyStackTransition(case)
        }
    }

    @Test
    fun `type checking verifier rejects invalid stack manipulation operand stack transitions`() {
        listOf(
            StackTransitionRejection(
                name = "pop rejects category two top",
                opcode = 0x57,
                maxStack = 2,
                stack = listOf(VerificationType.Long),
                expectedError = "Operand stack top contains Long, expected category 1 value",
            ),
            StackTransitionRejection(
                name = "pop2 rejects category two below category one top",
                opcode = 0x58,
                maxStack = 3,
                stack = listOf(VerificationType.Long, VerificationType.Integer),
                expectedError = "Operand stack top contains Long, expected category 1 value",
            ),
            StackTransitionRejection(
                name = "dup rejects max stack overflow caused by duplicate",
                opcode = 0x59,
                maxStack = 1,
                stack = listOf(VerificationType.Integer),
                expectedError = "Operand stack depth 2 exceeds max_stack=1",
            ),
            StackTransitionRejection(
                name = "dup_x1 rejects category two below category one top",
                opcode = 0x5A,
                maxStack = 3,
                stack = listOf(VerificationType.Long, VerificationType.Integer),
                expectedError = "Operand stack top contains Long, expected category 1 value",
            ),
            StackTransitionRejection(
                name = "dup_x2 rejects max stack overflow in category two form",
                opcode = 0x5B,
                maxStack = 3,
                stack = listOf(VerificationType.Long, VerificationType.Integer),
                expectedError = "Operand stack depth 4 exceeds max_stack=3",
            ),
            StackTransitionRejection(
                name = "dup2 rejects category two below category one top",
                opcode = 0x5C,
                maxStack = 4,
                stack = listOf(VerificationType.Long, VerificationType.Integer),
                expectedError = "Operand stack top contains Long, expected category 1 value",
            ),
            StackTransitionRejection(
                name = "dup2_x1 rejects category two top with missing lower operand",
                opcode = 0x5D,
                maxStack = 4,
                stack = listOf(VerificationType.Long),
                expectedError = "Operand stack is empty, expected category 1 value",
            ),
            StackTransitionRejection(
                name = "dup2_x2 rejects category two top with missing lower operand",
                opcode = 0x5E,
                maxStack = 4,
                stack = listOf(VerificationType.Long),
                expectedError = "Operand stack is empty, expected category 1 or category 2 value",
            ),
            StackTransitionRejection(
                name = "swap rejects category two below category one top",
                opcode = 0x5F,
                maxStack = 3,
                stack = listOf(VerificationType.Long, VerificationType.Integer),
                expectedError = "Operand stack top contains Long, expected category 1 value",
            ),
        ).forEach { case ->
            val exception = assertFailsWith<MethodVerificationException>(case.name) {
                verifyStackTransition(case)
            }

            assertEquals(case.expectedError, exception.message, case.name)
        }
    }

    @Test
    fun `type checking verifier applies iadd operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x60.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x60.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ladd operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x61.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x61.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies fadd operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x62.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float, VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x62.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies dadd operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x63.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x63.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Double, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies isub operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x64.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x64.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lsub operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x65.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x65.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies fsub operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x66.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float, VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x66.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies dsub operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x67.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x67.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Double, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies imul operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x68.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x68.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lmul operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x69.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x69.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies fmul operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x6A.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float, VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x6A.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies dmul operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x6B.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x6B.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Double, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies idiv operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x6C.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x6C.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ldiv operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x6D.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x6D.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies fdiv operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x6E.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float, VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x6E.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ddiv operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x6F.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x6F.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Double, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies irem operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x70.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x70.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lrem operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x71.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x71.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies frem operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x72.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float, VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x72.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies drem operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x73.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x73.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Double, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ineg operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x74.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x74.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lneg operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x75.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x75.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies fneg operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x76.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x76.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies dneg operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x77.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x77.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ishl operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x78.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x78.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Long),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lshl operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 3,
                maxLocals = 0,
                code = byteArrayOf(
                    0x79.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x79.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Long),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ishr operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x7A.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x7A.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Long),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lshr operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 3,
                maxLocals = 0,
                code = byteArrayOf(
                    0x7B.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x7B.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Long),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies iushr operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x7C.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 3,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x7C.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Long),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lushr operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 3,
                maxLocals = 0,
                code = byteArrayOf(
                    0x7D.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x7D.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Long),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies iand operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x7E.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x7E.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies land operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x7F.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x7F.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ior operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x80.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x80.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lor operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x81.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x81.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ixor operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x82.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x82.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer, VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lxor operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x83.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x83.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies iinc local variable transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 1,
                code = byteArrayOf(
                    0x84.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = listOf(VerificationType.Integer),
                    stack = listOf(VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 0,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0x84.toByte(),
                        0x00.toByte(),
                        0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = listOf(VerificationType.Float),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Local variable 0 contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide iinc local variable transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 301,
                code = byteArrayOf(
                    0xC4.toByte(),
                    0x84.toByte(),
                    0x01.toByte(),
                    0x2C.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = List(300) { VerificationType.Top } + VerificationType.Integer,
                    stack = listOf(VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 0,
                    maxLocals = 301,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x84.toByte(),
                        0x01.toByte(),
                        0x2C.toByte(),
                        0x00.toByte(),
                        0x01.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = List(300) { VerificationType.Top } + VerificationType.Float,
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Local variable 300 contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ret local variable transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 1,
                code = byteArrayOf(
                    0xA9.toByte(),
                    0x00.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = listOf(VerificationType.ReturnAddress),
                    stack = listOf(VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 0,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0xA9.toByte(),
                        0x00.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = listOf(VerificationType.Float),
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Local variable 0 contains Float, expected ReturnAddress",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide ret local variable transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 301,
                code = byteArrayOf(
                    0xC4.toByte(),
                    0xA9.toByte(),
                    0x01.toByte(),
                    0x2C.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = List(300) { VerificationType.Top } + VerificationType.ReturnAddress,
                    stack = listOf(VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 0,
                    maxLocals = 301,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0xA9.toByte(),
                        0x01.toByte(),
                        0x2C.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = List(300) { VerificationType.Top } + VerificationType.Float,
                        stack = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(
            "Local variable 300 contains Float, expected ReturnAddress",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies return declared return type transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 0,
                maxLocals = 0,
                code = byteArrayOf(
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

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 0,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xB1.toByte(),
                    ),
                ),
                initialFrame = MethodInitialFrame(
                    locals = emptyList(),
                    stack = emptyList(),
                    flags = emptyList(),
                    returnType = VerificationType.Integer,
                ),
                frameStates = emptyList(),
            )
        }

        assertEquals(
            "Method return type is Integer, expected void",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies ireturn declared return type transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0xAC.toByte(),
                ),
            ),
            initialFrame = MethodInitialFrame(
                locals = emptyList(),
                stack = listOf(VerificationType.Integer),
                flags = emptyList(),
                returnType = VerificationType.Integer,
            ),
            frameStates = emptyList(),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xAC.toByte(),
                    ),
                ),
                initialFrame = MethodInitialFrame(
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                    flags = emptyList(),
                    returnType = VerificationType.Float,
                ),
                frameStates = emptyList(),
            )
        }

        assertEquals(
            "Method return type is Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lreturn declared return type transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0xAD.toByte(),
                ),
            ),
            initialFrame = MethodInitialFrame(
                locals = emptyList(),
                stack = listOf(VerificationType.Long),
                flags = emptyList(),
                returnType = VerificationType.Long,
            ),
            frameStates = emptyList(),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xAD.toByte(),
                    ),
                ),
                initialFrame = MethodInitialFrame(
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                    flags = emptyList(),
                    returnType = VerificationType.Double,
                ),
                frameStates = emptyList(),
            )
        }

        assertEquals(
            "Method return type is Double, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies freturn declared return type transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0xAE.toByte(),
                ),
            ),
            initialFrame = MethodInitialFrame(
                locals = emptyList(),
                stack = listOf(VerificationType.Float),
                flags = emptyList(),
                returnType = VerificationType.Float,
            ),
            frameStates = emptyList(),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xAE.toByte(),
                    ),
                ),
                initialFrame = MethodInitialFrame(
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                    flags = emptyList(),
                    returnType = VerificationType.Double,
                ),
                frameStates = emptyList(),
            )
        }

        assertEquals(
            "Method return type is Double, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies dreturn declared return type transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0xAF.toByte(),
                ),
            ),
            initialFrame = MethodInitialFrame(
                locals = emptyList(),
                stack = listOf(VerificationType.Double),
                flags = emptyList(),
                returnType = VerificationType.Double,
            ),
            frameStates = emptyList(),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xAF.toByte(),
                    ),
                ),
                initialFrame = MethodInitialFrame(
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double),
                    flags = emptyList(),
                    returnType = VerificationType.Float,
                ),
                frameStates = emptyList(),
            )
        }

        assertEquals(
            "Method return type is Float, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies areturn declared return type transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0xB0.toByte(),
                ),
            ),
            initialFrame = MethodInitialFrame(
                locals = emptyList(),
                stack = listOf(VerificationType.Reference),
                flags = emptyList(),
                returnType = VerificationType.Reference,
            ),
            frameStates = emptyList(),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0xB0.toByte(),
                    ),
                ),
                initialFrame = MethodInitialFrame(
                    locals = emptyList(),
                    stack = listOf(VerificationType.Reference),
                    flags = emptyList(),
                    returnType = VerificationType.Integer,
                ),
                frameStates = emptyList(),
            )
        }

        assertEquals(
            "Method return type is Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies i2l operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x85.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x85.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies i2f operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x86.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x86.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies i2d operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x87.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x87.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies l2i operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x88.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x88.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies l2f operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x89.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x89.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies l2d operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x8A.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x8A.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies f2i operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x8B.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x8B.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies f2l operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x8C.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x8C.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies f2d operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x8D.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x8D.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies d2i operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x8E.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x8E.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies d2l operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x8F.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x8F.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies d2f operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x90.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x90.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies i2b operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x91.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x91.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies i2c operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x92.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x92.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies i2s operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 1,
                maxLocals = 0,
                code = byteArrayOf(
                    0x93.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x93.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies lcmp operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x94.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x94.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Long, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies fcmpl operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x95.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float, VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x95.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies fcmpg operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 2,
                maxLocals = 0,
                code = byteArrayOf(
                    0x96.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float, VerificationType.Float),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x96.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Float, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies dcmpl operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x97.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x97.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Double, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies dcmpg operand stack transition`() {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = 4,
                maxLocals = 0,
                code = byteArrayOf(
                    0x98.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Double),
                ),
            ),
        )

        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 4,
                    maxLocals = 0,
                    code = byteArrayOf(
                        0x98.toByte(),
                        0xB1.toByte(),
                    ),
                ),
                frameStates = listOf(
                    VerificationFrameState(
                        bytecodeOffset = 0,
                        locals = emptyList(),
                        stack = listOf(VerificationType.Double, VerificationType.Integer),
                    ),
                ),
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Double",
            exception.message,
        )
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

    @Test
    fun `type checking verifier applies explicit float local load transitions`() {
        listOf(0, 1, 2, 3).forEach { index ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 4,
                        code = byteArrayOf(
                            0x17.toByte(),
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
                "Local variable $index contains Top, expected Float",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies explicit double local load transitions`() {
        listOf(0, 1, 2, 3).forEach { index ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 2,
                        maxLocals = 5,
                        code = byteArrayOf(
                            0x18.toByte(),
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
                "Local variable $index contains Top, expected Double",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies explicit reference local load transitions`() {
        listOf(0, 1, 2, 3).forEach { index ->
            val exception = assertFailsWith<MethodVerificationException> {
                MethodTypeCheckingVerifier.verify(
                    code = code(
                        maxStack = 1,
                        maxLocals = 4,
                        code = byteArrayOf(
                            0x19.toByte(),
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
                "Local variable $index contains Top, expected Reference",
                exception.message,
            )
        }
    }

    @Test
    fun `type checking verifier applies wide int local load transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 257,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x15.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Local variable 256 contains Top, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide long local load transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 258,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x16.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Local variable 256 contains Top, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide float local load transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 257,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x17.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Local variable 256 contains Top, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide double local load transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 258,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x18.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Local variable 256 contains Top, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide reference local load transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 257,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x19.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Local variable 256 contains Top, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide int local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 257,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x36.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide long local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 258,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x37.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide float local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 257,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x38.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide double local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 258,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x39.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies wide reference local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 257,
                    code = byteArrayOf(
                        0xC4.toByte(),
                        0x3A.toByte(),
                        0x01.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies explicit int local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0x36.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies explicit long local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 2,
                    code = byteArrayOf(
                        0x37.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies explicit float local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0x38.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies explicit double local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 2,
                    code = byteArrayOf(
                        0x39.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies explicit reference local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 1,
                    code = byteArrayOf(
                        0x3A.toByte(),
                        0x00.toByte(),
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
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies implicit int local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 4,
                    code = byteArrayOf(
                        0x3E.toByte(),
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
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies implicit long local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 5,
                    code = byteArrayOf(
                        0x42.toByte(),
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
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies implicit float local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 4,
                    code = byteArrayOf(
                        0x46.toByte(),
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
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies implicit double local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 2,
                    maxLocals = 5,
                    code = byteArrayOf(
                        0x4A.toByte(),
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
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `type checking verifier applies implicit reference local store transitions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MethodTypeCheckingVerifier.verify(
                code = code(
                    maxStack = 1,
                    maxLocals = 4,
                    code = byteArrayOf(
                        0x4E.toByte(),
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
            "Operand stack is empty, expected category 1 value",
            exception.message,
        )
    }

    private data class StackTransitionCase(
        val name: String,
        val opcode: Int,
        val maxStack: Int,
        val stack: List<VerificationType>,
    )

    private data class StackTransitionRejection(
        val name: String,
        val opcode: Int,
        val maxStack: Int,
        val stack: List<VerificationType>,
        val expectedError: String,
    )

    private fun verifyStackTransition(case: StackTransitionCase) {
        verifyStackTransition(
            opcode = case.opcode,
            maxStack = case.maxStack,
            stack = case.stack,
        )
    }

    private fun verifyStackTransition(case: StackTransitionRejection) {
        verifyStackTransition(
            opcode = case.opcode,
            maxStack = case.maxStack,
            stack = case.stack,
        )
    }

    private fun verifyStackTransition(
        opcode: Int,
        maxStack: Int,
        stack: List<VerificationType>,
    ) {
        MethodTypeCheckingVerifier.verify(
            code = code(
                maxStack = maxStack,
                maxLocals = 0,
                code = byteArrayOf(
                    opcode.toByte(),
                    0xB1.toByte(),
                ),
            ),
            frameStates = listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = emptyList(),
                    stack = stack,
                ),
            ),
        )
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
