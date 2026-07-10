package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CodeInstructionValidationTest {
    @Test
    fun `accepts branch target that points to an instruction opcode`() {
        val attribute = parseCodeAttribute(
            byteArrayOf(
                0xA7.toByte(),
                0,
                3,
                0xB1.toByte(),
            ),
        )

        assertIs<CodeAttribute>(attribute)
    }

    @Test
    fun `rejects fixed width instructions truncated before their operands`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(byteArrayOf(0x11, 0))
        }

        assertTrue(failure.message.orEmpty().contains("truncated"), failure.message)
        assertTrue(failure.message.orEmpty().contains("sipush"), failure.message)
    }

    @Test
    fun `rejects reserved opcodes in code arrays`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(byteArrayOf(0xCA.toByte()))
        }

        assertTrue(failure.message.orEmpty().contains("reserved opcode"), failure.message)
        assertTrue(failure.message.orEmpty().contains("0xca"), failure.message)
    }

    @Test
    fun `rejects branch targets that do not point to instruction opcodes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                byteArrayOf(
                    0xA7.toByte(),
                    0,
                    2,
                    0xB1.toByte(),
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("branch target"), failure.message)
        assertTrue(failure.message.orEmpty().contains("opcode"), failure.message)
    }

    @Test
    fun `rejects branch targets that point to the opcode modified by wide`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                byteArrayOf(
                    0xA7.toByte(),
                    0,
                    4,
                    0xC4.toByte(),
                    0x15,
                    0,
                    1,
                    0xB1.toByte(),
                ),
                maxLocals = 2,
            )
        }

        assertTrue(failure.message.orEmpty().contains("branch target=4"), failure.message)
        assertTrue(failure.message.orEmpty().contains("modified by wide"), failure.message)
    }

    @Test
    fun `rejects tableswitch instructions whose low value is greater than high`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                byteArrayOf(0xAA.toByte()) +
                    byteArrayOf(0, 0, 0) +
                    intBytes(16) +
                    intBytes(2) +
                    intBytes(1) +
                    byteArrayOf(0xB1.toByte()),
            )
        }

        assertTrue(failure.message.orEmpty().contains("tableswitch"), failure.message)
        assertTrue(failure.message.orEmpty().contains("low"), failure.message)
        assertTrue(failure.message.orEmpty().contains("high"), failure.message)
    }

    @Test
    fun `rejects lookupswitch match offset pairs that are not sorted`() {
        val returnOffset = 28
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                byteArrayOf(0xAB.toByte()) +
                    byteArrayOf(0, 0, 0) +
                    intBytes(returnOffset) +
                    intBytes(2) +
                    intBytes(2) +
                    intBytes(returnOffset) +
                    intBytes(1) +
                    intBytes(returnOffset) +
                    byteArrayOf(0xB1.toByte()),
            )
        }

        assertTrue(failure.message.orEmpty().contains("lookupswitch"), failure.message)
        assertTrue(failure.message.orEmpty().contains("increasing"), failure.message)
    }

    @Test
    fun `rejects wide instructions that modify unsupported opcodes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                byteArrayOf(
                    0xC4.toByte(),
                    0x00,
                    0,
                    1,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("wide"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unsupported"), failure.message)
    }

    @Test
    fun `accepts jsr jsr_w and ret instructions in legacy classfile versions`() {
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(0xA8.toByte(), 0, 3, 0xB1.toByte()),
                majorVersion = 50,
            ),
        )
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(0xC9.toByte(), 0, 0, 0, 5, 0xB1.toByte()),
                majorVersion = 50,
            ),
        )
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(0xA9.toByte(), 0, 0xB1.toByte()),
                majorVersion = 50,
            ),
        )
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(0xC4.toByte(), 0xA9.toByte(), 0, 1, 0xB1.toByte()),
                maxLocals = 2,
                majorVersion = 50,
            ),
        )
    }

    @Test
    fun `rejects jsr jsr_w and ret instructions in classfile version 51 or newer`() {
        val discontinuedInstructions = listOf(
            "jsr" to byteArrayOf(0xA8.toByte(), 0, 3, 0xB1.toByte()),
            "jsr_w" to byteArrayOf(0xC9.toByte(), 0, 0, 0, 5, 0xB1.toByte()),
            "ret" to byteArrayOf(0xA9.toByte(), 0, 0xB1.toByte()),
        )
        discontinuedInstructions.forEach { (mnemonic, code) ->
            val failure = assertFailsWith<ClassFileFormatException> {
                parseCodeAttribute(code = code, majorVersion = 51)
            }

            assertTrue(failure.message.orEmpty().contains(mnemonic), failure.message)
            assertTrue(failure.message.orEmpty().contains("major version 51"), failure.message)
        }

        val wideRetFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xC4.toByte(), 0xA9.toByte(), 0, 1, 0xB1.toByte()),
                majorVersion = 51,
            )
        }
        assertTrue(wideRetFailure.message.orEmpty().contains("wide"), wideRetFailure.message)
        assertTrue(wideRetFailure.message.orEmpty().contains("ret"), wideRetFailure.message)
        assertTrue(wideRetFailure.message.orEmpty().contains("major version 51"), wideRetFailure.message)
    }

    @Test
    fun `accepts category one local variable indexes within max locals`() {
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(
                    0x15,
                    1,
                    0x17,
                    1,
                    0x19,
                    1,
                    0x36,
                    1,
                    0x38,
                    1,
                    0x3A,
                    1,
                    0x84.toByte(),
                    1,
                    1,
                    0xA9.toByte(),
                    1,
                    0xC4.toByte(),
                    0x15,
                    0,
                    1,
                    0xC4.toByte(),
                    0x84.toByte(),
                    0,
                    1,
                    0,
                    1,
                    0xB1.toByte(),
                ),
                maxLocals = 2,
                majorVersion = 50,
            ),
        )
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(
                    0x1D,
                    0x25,
                    0x2D,
                    0x3E,
                    0x46,
                    0x4E,
                    0xB1.toByte(),
                ),
                maxLocals = 4,
            ),
        )
    }

    @Test
    fun `rejects category one local variable indexes outside max locals`() {
        val indexedInstructions = listOf(
            "iload" to byteArrayOf(0x15, 2, 0xB1.toByte()),
            "fload" to byteArrayOf(0x17, 2, 0xB1.toByte()),
            "aload" to byteArrayOf(0x19, 2, 0xB1.toByte()),
            "istore" to byteArrayOf(0x36, 2, 0xB1.toByte()),
            "fstore" to byteArrayOf(0x38, 2, 0xB1.toByte()),
            "astore" to byteArrayOf(0x3A, 2, 0xB1.toByte()),
            "iinc" to byteArrayOf(0x84.toByte(), 2, 1, 0xB1.toByte()),
            "ret" to byteArrayOf(0xA9.toByte(), 2, 0xB1.toByte()),
        )
        indexedInstructions.forEach { (mnemonic, code) ->
            val failure = assertFailsWith<ClassFileFormatException> {
                parseCodeAttribute(code = code, maxLocals = 2, majorVersion = 50)
            }

            assertTrue(failure.message.orEmpty().contains(mnemonic), failure.message)
            assertTrue(failure.message.orEmpty().contains("max_locals=2"), failure.message)
        }

        val implicitFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(code = byteArrayOf(0x1D, 0xB1.toByte()), maxLocals = 3)
        }
        assertTrue(implicitFailure.message.orEmpty().contains("iload_3"), implicitFailure.message)
        assertTrue(implicitFailure.message.orEmpty().contains("max_locals=3"), implicitFailure.message)

        val wideFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xC4.toByte(), 0x15, 0, 2, 0xB1.toByte()),
                maxLocals = 2,
            )
        }
        assertTrue(wideFailure.message.orEmpty().contains("wide iload"), wideFailure.message)
        assertTrue(wideFailure.message.orEmpty().contains("max_locals=2"), wideFailure.message)
    }

    @Test
    fun `accepts category two local variable indexes within max locals`() {
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(
                    0x16,
                    1,
                    0x18,
                    1,
                    0x37,
                    1,
                    0x39,
                    1,
                    0xC4.toByte(),
                    0x16,
                    0,
                    1,
                    0xB1.toByte(),
                ),
                maxLocals = 3,
            ),
        )
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(
                    0x1F,
                    0x27,
                    0x40,
                    0x48,
                    0xB1.toByte(),
                ),
                maxLocals = 3,
            ),
        )
    }

    @Test
    fun `rejects category two local variable indexes outside max locals`() {
        val indexedInstructions = listOf(
            "lload" to byteArrayOf(0x16, 1, 0xB1.toByte()),
            "dload" to byteArrayOf(0x18, 1, 0xB1.toByte()),
            "lstore" to byteArrayOf(0x37, 1, 0xB1.toByte()),
            "dstore" to byteArrayOf(0x39, 1, 0xB1.toByte()),
        )
        indexedInstructions.forEach { (mnemonic, code) ->
            val failure = assertFailsWith<ClassFileFormatException> {
                parseCodeAttribute(code = code, maxLocals = 2)
            }

            assertTrue(failure.message.orEmpty().contains(mnemonic), failure.message)
            assertTrue(failure.message.orEmpty().contains("max_locals=2"), failure.message)
        }

        val implicitFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(code = byteArrayOf(0x1F, 0xB1.toByte()), maxLocals = 2)
        }
        assertTrue(implicitFailure.message.orEmpty().contains("lload_1"), implicitFailure.message)
        assertTrue(implicitFailure.message.orEmpty().contains("max_locals=2"), implicitFailure.message)

        val wideFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xC4.toByte(), 0x16, 0, 1, 0xB1.toByte()),
                maxLocals = 2,
            )
        }
        assertTrue(wideFailure.message.orEmpty().contains("wide lload"), wideFailure.message)
        assertTrue(wideFailure.message.orEmpty().contains("max_locals=2"), wideFailure.message)
    }

    @Test
    fun `accepts class reference instruction operands that point to class constants`() {
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(
                    0xBB.toByte(),
                    0,
                    2,
                    0xBD.toByte(),
                    0,
                    2,
                    0xC0.toByte(),
                    0,
                    2,
                    0xC1.toByte(),
                    0,
                    2,
                    0xB1.toByte(),
                ),
                constantPool = constantPoolWithClass("java/lang/String"),
            ),
        )
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(
                    0xC5.toByte(),
                    0,
                    2,
                    2,
                    0xB1.toByte(),
                ),
                constantPool = constantPoolWithClass("[[I"),
            ),
        )
    }

    @Test
    fun `rejects class reference instruction operands that do not point to class constants`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xBB.toByte(), 0, 2, 0xB1.toByte()),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("Code", byteArrayOf()),
                        ConstantIntegerEntry(1),
                    ),
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("new"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class"), failure.message)
    }

    @Test
    fun `rejects new instructions that reference array classes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xBB.toByte(), 0, 2, 0xB1.toByte()),
                constantPool = constantPoolWithClass("[Ljava/lang/String;"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("new"), failure.message)
        assertTrue(failure.message.orEmpty().contains("array type"), failure.message)
    }

    @Test
    fun `rejects anewarray instructions that would create more than 255 dimensions`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xBD.toByte(), 0, 2, 0xB1.toByte()),
                constantPool = constantPoolWithClass("[".repeat(255) + "Ljava/lang/String;"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("anewarray"), failure.message)
        assertTrue(failure.message.orEmpty().contains("255"), failure.message)
    }

    @Test
    fun `rejects multianewarray instructions whose dimensions operand is zero`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xC5.toByte(), 0, 2, 0, 0xB1.toByte()),
                constantPool = constantPoolWithClass("[I"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("multianewarray"), failure.message)
        assertTrue(failure.message.orEmpty().contains("dimensions"), failure.message)
    }

    @Test
    fun `rejects multianewarray instructions that create more dimensions than the array class has`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xC5.toByte(), 0, 2, 2, 0xB1.toByte()),
                constantPool = constantPoolWithClass("[I"),
            )
        }

        assertTrue(failure.message.orEmpty().contains("multianewarray"), failure.message)
        assertTrue(failure.message.orEmpty().contains("array dimensions"), failure.message)
    }

    @Test
    fun `accepts newarray instructions with primitive array type codes`() {
        val code = (4..11).fold(byteArrayOf()) { bytes, atype ->
            bytes + byteArrayOf(0xBC.toByte(), atype.toByte())
        } + byteArrayOf(0xB1.toByte())

        assertIs<CodeAttribute>(parseCodeAttribute(code))
    }

    @Test
    fun `rejects newarray instructions with unsupported array type codes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(byteArrayOf(0xBC.toByte(), 3, 0xB1.toByte()))
        }

        assertTrue(failure.message.orEmpty().contains("newarray"), failure.message)
        assertTrue(failure.message.orEmpty().contains("atype"), failure.message)
    }

    @Test
    fun `accepts ldc and ldc_w operands that point to category one loadable constants`() {
        val attribute = parseCodeAttribute(
            code = byteArrayOf(
                0x12,
                2,
                0x12,
                3,
                0x12,
                4,
                0x12,
                5,
                0x12,
                6,
                0x12,
                7,
                0x12,
                8,
                0x13,
                0,
                8,
                0xB1.toByte(),
            ),
            constantPool = ldcCategoryOnePool(),
        )

        assertIs<CodeAttribute>(attribute)
    }

    @Test
    fun `rejects ldc operands that do not point to loadable constants`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0x12, 2, 0xB1.toByte()),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("Code", byteArrayOf()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(3), ConstantPoolIndex(4)),
                        ConstantUtf8Entry("notLoadable", byteArrayOf()),
                        ConstantUtf8Entry("I", byteArrayOf()),
                    ),
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ldc"), failure.message)
        assertTrue(failure.message.orEmpty().contains("loadable"), failure.message)
    }

    @Test
    fun `rejects ldc operands that point to category two constants`() {
        val longFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0x12, 2, 0xB1.toByte()),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("Code", byteArrayOf()),
                        ConstantLongEntry(1L),
                    ),
                ),
            )
        }
        assertTrue(longFailure.message.orEmpty().contains("ldc"), longFailure.message)
        assertTrue(longFailure.message.orEmpty().contains("long or double"), longFailure.message)

        val dynamicFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0x12, 2, 0xB1.toByte()),
                constantPool = constantDynamicPool("J"),
            )
        }
        assertTrue(dynamicFailure.message.orEmpty().contains("CONSTANT_Dynamic"), dynamicFailure.message)
        assertTrue(dynamicFailure.message.orEmpty().contains("J"), dynamicFailure.message)
    }

    @Test
    fun `accepts ldc2_w operands that point to category two constants`() {
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(0x14, 0, 2, 0xB1.toByte()),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("Code", byteArrayOf()),
                        ConstantLongEntry(1L),
                    ),
                ),
            ),
        )
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(0x14, 0, 2, 0xB1.toByte()),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("Code", byteArrayOf()),
                        ConstantDoubleEntry(1.0),
                    ),
                ),
            ),
        )
        assertIs<CodeAttribute>(
            parseCodeAttribute(
                code = byteArrayOf(0x14, 0, 2, 0xB1.toByte()),
                constantPool = constantDynamicPool("D"),
            ),
        )
    }

    @Test
    fun `rejects ldc2_w operands that point to category one constants`() {
        val integerFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0x14, 0, 2, 0xB1.toByte()),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("Code", byteArrayOf()),
                        ConstantIntegerEntry(1),
                    ),
                ),
            )
        }
        assertTrue(integerFailure.message.orEmpty().contains("ldc2_w"), integerFailure.message)
        assertTrue(integerFailure.message.orEmpty().contains("long or double"), integerFailure.message)

        val dynamicFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0x14, 0, 2, 0xB1.toByte()),
                constantPool = constantDynamicPool("I"),
            )
        }
        assertTrue(dynamicFailure.message.orEmpty().contains("CONSTANT_Dynamic"), dynamicFailure.message)
        assertTrue(dynamicFailure.message.orEmpty().contains("J or D"), dynamicFailure.message)
    }

    @Test
    fun `accepts field access instruction operands that point to field references`() {
        val attribute = parseCodeAttribute(
            code = byteArrayOf(
                0xB2.toByte(),
                0,
                2,
                0xB3.toByte(),
                0,
                2,
                0xB4.toByte(),
                0,
                2,
                0xB5.toByte(),
                0,
                2,
                0xB1.toByte(),
            ),
            constantPool = fieldReferencePool(),
        )

        assertIs<CodeAttribute>(attribute)
    }

    @Test
    fun `rejects field access instruction operands that do not point to field references`() {
        val opcodes = listOf(
            0xB2 to "getstatic",
            0xB3 to "putstatic",
            0xB4 to "getfield",
            0xB5 to "putfield",
        )
        opcodes.forEach { (opcode, mnemonic) ->
            val failure = assertFailsWith<ClassFileFormatException> {
                parseCodeAttribute(
                    code = byteArrayOf(opcode.toByte(), 0, 2, 0xB1.toByte()),
                    constantPool = methodReferencePool(),
                )
            }

            assertTrue(failure.message.orEmpty().contains(mnemonic), failure.message)
            assertTrue(failure.message.orEmpty().contains("CONSTANT_Fieldref"), failure.message)
        }
    }

    @Test
    fun `accepts invokevirtual operands that point to method references`() {
        val attribute = parseCodeAttribute(
            code = byteArrayOf(0xB6.toByte(), 0, 2, 0xB1.toByte()),
            constantPool = methodReferencePool(),
        )

        assertIs<CodeAttribute>(attribute)
    }

    @Test
    fun `rejects invokevirtual operands that do not point to method references`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xB6.toByte(), 0, 2, 0xB1.toByte()),
                constantPool = fieldReferencePool(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("invokevirtual"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Methodref"), failure.message)
    }

    @Test
    fun `accepts invokespecial and invokestatic method references in all supported classfile versions`() {
        val opcodes = listOf(0xB7 to "invokespecial", 0xB8 to "invokestatic")
        opcodes.forEach { (opcode, _) ->
            assertIs<CodeAttribute>(
                parseCodeAttribute(
                    code = byteArrayOf(opcode.toByte(), 0, 2, 0xB1.toByte()),
                    constantPool = methodReferencePool(),
                    majorVersion = 51,
                ),
            )
            assertIs<CodeAttribute>(
                parseCodeAttribute(
                    code = byteArrayOf(opcode.toByte(), 0, 2, 0xB1.toByte()),
                    constantPool = methodReferencePool(),
                    majorVersion = 52,
                ),
            )
        }
    }

    @Test
    fun `accepts invokespecial and invokestatic interface method references in modern classfile versions`() {
        val opcodes = listOf(0xB7 to "invokespecial", 0xB8 to "invokestatic")
        opcodes.forEach { (opcode, _) ->
            assertIs<CodeAttribute>(
                parseCodeAttribute(
                    code = byteArrayOf(opcode.toByte(), 0, 2, 0xB1.toByte()),
                    constantPool = interfaceMethodReferencePool(),
                    majorVersion = 52,
                ),
            )
        }
    }

    @Test
    fun `rejects invokespecial and invokestatic interface method references in legacy classfile versions`() {
        val opcodes = listOf(0xB7 to "invokespecial", 0xB8 to "invokestatic")
        opcodes.forEach { (opcode, mnemonic) ->
            val failure = assertFailsWith<ClassFileFormatException> {
                parseCodeAttribute(
                    code = byteArrayOf(opcode.toByte(), 0, 2, 0xB1.toByte()),
                    constantPool = interfaceMethodReferencePool(),
                    majorVersion = 51,
                )
            }

            assertTrue(failure.message.orEmpty().contains(mnemonic), failure.message)
            assertTrue(failure.message.orEmpty().contains("major version 51"), failure.message)
            assertTrue(failure.message.orEmpty().contains("CONSTANT_Methodref"), failure.message)
        }
    }

    @Test
    fun `rejects invokespecial and invokestatic operands that are not method references`() {
        val opcodes = listOf(0xB7 to "invokespecial", 0xB8 to "invokestatic")
        opcodes.forEach { (opcode, mnemonic) ->
            val failure = assertFailsWith<ClassFileFormatException> {
                parseCodeAttribute(
                    code = byteArrayOf(opcode.toByte(), 0, 2, 0xB1.toByte()),
                    constantPool = fieldReferencePool(),
                    majorVersion = 52,
                )
            }

            assertTrue(failure.message.orEmpty().contains(mnemonic), failure.message)
            assertTrue(failure.message.orEmpty().contains("CONSTANT_Methodref"), failure.message)
            assertTrue(failure.message.orEmpty().contains("CONSTANT_InterfaceMethodref"), failure.message)
        }
    }

    @Test
    fun `accepts invokeinterface operands that point to interface method references`() {
        val attribute = parseCodeAttribute(
            code = byteArrayOf(0xB9.toByte(), 0, 2, 1, 0, 0xB1.toByte()),
            constantPool = interfaceMethodReferencePool(),
        )

        assertIs<CodeAttribute>(attribute)
    }

    @Test
    fun `rejects invokeinterface operands that do not point to interface method references`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xB9.toByte(), 0, 2, 1, 0, 0xB1.toByte()),
                constantPool = methodReferencePool(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("invokeinterface"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_InterfaceMethodref"), failure.message)
    }

    @Test
    fun `rejects invokeinterface operands with zero count or nonzero fourth byte`() {
        val zeroCountFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xB9.toByte(), 0, 2, 0, 0, 0xB1.toByte()),
                constantPool = interfaceMethodReferencePool(),
            )
        }
        assertTrue(zeroCountFailure.message.orEmpty().contains("invokeinterface"), zeroCountFailure.message)
        assertTrue(zeroCountFailure.message.orEmpty().contains("count"), zeroCountFailure.message)
        assertTrue(zeroCountFailure.message.orEmpty().contains("zero"), zeroCountFailure.message)

        val nonzeroFourthByteFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xB9.toByte(), 0, 2, 1, 1, 0xB1.toByte()),
                constantPool = interfaceMethodReferencePool(),
            )
        }
        assertTrue(nonzeroFourthByteFailure.message.orEmpty().contains("invokeinterface"), nonzeroFourthByteFailure.message)
        assertTrue(nonzeroFourthByteFailure.message.orEmpty().contains("fourth"), nonzeroFourthByteFailure.message)
        assertTrue(nonzeroFourthByteFailure.message.orEmpty().contains("zero"), nonzeroFourthByteFailure.message)
    }

    @Test
    fun `accepts invokedynamic operands that point to dynamic call site specifiers`() {
        val attribute = parseCodeAttribute(
            code = byteArrayOf(0xBA.toByte(), 0, 2, 0, 0, 0xB1.toByte()),
            constantPool = invokeDynamicPool(),
        )

        assertIs<CodeAttribute>(attribute)
    }

    @Test
    fun `rejects invokedynamic operands that do not point to dynamic call site specifiers`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xBA.toByte(), 0, 2, 0, 0, 0xB1.toByte()),
                constantPool = methodReferencePool(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("invokedynamic"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_InvokeDynamic"), failure.message)
    }

    @Test
    fun `rejects invokedynamic operands with nonzero trailing bytes`() {
        val thirdByteFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xBA.toByte(), 0, 2, 1, 0, 0xB1.toByte()),
                constantPool = invokeDynamicPool(),
            )
        }
        assertTrue(thirdByteFailure.message.orEmpty().contains("invokedynamic"), thirdByteFailure.message)
        assertTrue(thirdByteFailure.message.orEmpty().contains("third"), thirdByteFailure.message)
        assertTrue(thirdByteFailure.message.orEmpty().contains("zero"), thirdByteFailure.message)

        val fourthByteFailure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xBA.toByte(), 0, 2, 0, 1, 0xB1.toByte()),
                constantPool = invokeDynamicPool(),
            )
        }
        assertTrue(fourthByteFailure.message.orEmpty().contains("invokedynamic"), fourthByteFailure.message)
        assertTrue(fourthByteFailure.message.orEmpty().contains("fourth"), fourthByteFailure.message)
        assertTrue(fourthByteFailure.message.orEmpty().contains("zero"), fourthByteFailure.message)
    }

    private fun parseCodeAttribute(
        code: ByteArray,
        constantPool: ConstantPool = ConstantPool.fromEntries(listOf(ConstantUtf8Entry("Code", byteArrayOf()))),
        majorVersion: Int = 70,
        maxLocals: Int = 1,
    ): AttributeInfo {
        return AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                codeAttributeBytes(code, maxLocals),
                source = "code-instruction-validation.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
            ownerPath = "methods[0]",
            majorVersion = majorVersion,
        ).single()
    }

    private fun constantPoolWithClass(name: String): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry(name, name.encodeToByteArray()),
            ),
        )

    private fun ldcCategoryOnePool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantIntegerEntry(1),
                ConstantFloatEntry(1.0f),
                ConstantStringEntry(ConstantPoolIndex(13)),
                ConstantClassEntry(ConstantPoolIndex(14)),
                ConstantMethodTypeEntry(ConstantPoolIndex(15)),
                ConstantMethodHandleEntry(MethodHandleReferenceKind.GetStatic, ConstantPoolIndex(2)),
                ConstantDynamicEntry(BootstrapMethodIndex(0), ConstantPoolIndex(10)),
                ConstantUtf8Entry("unused", byteArrayOf()),
                ConstantNameAndTypeEntry(ConstantPoolIndex(11), ConstantPoolIndex(12)),
                ConstantUtf8Entry("dyn", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("hello", byteArrayOf()),
                ConstantUtf8Entry("java/lang/String", byteArrayOf()),
                ConstantUtf8Entry("()V", byteArrayOf()),
            ),
        )

    private fun constantDynamicPool(descriptor: String): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantDynamicEntry(BootstrapMethodIndex(0), ConstantPoolIndex(3)),
                ConstantNameAndTypeEntry(ConstantPoolIndex(4), ConstantPoolIndex(5)),
                ConstantUtf8Entry("dyn", byteArrayOf()),
                ConstantUtf8Entry(descriptor, descriptor.encodeToByteArray()),
            ),
        )

    private fun invokeDynamicPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantInvokeDynamicEntry(BootstrapMethodIndex(0), ConstantPoolIndex(3)),
                ConstantNameAndTypeEntry(ConstantPoolIndex(4), ConstantPoolIndex(5)),
                ConstantUtf8Entry("run", byteArrayOf()),
                ConstantUtf8Entry("()V", byteArrayOf()),
            ),
        )

    private fun fieldReferencePool(): ConstantPool =
        ConstantPool.fromEntries(memberReferencePoolEntries(::ConstantFieldRefEntry, "value", "I"))

    private fun methodReferencePool(): ConstantPool =
        ConstantPool.fromEntries(memberReferencePoolEntries(::ConstantMethodRefEntry, "run", "()V"))

    private fun interfaceMethodReferencePool(): ConstantPool =
        ConstantPool.fromEntries(memberReferencePoolEntries(::ConstantInterfaceMethodRefEntry, "run", "()V"))

    private fun memberReferencePoolEntries(
        createMemberRef: (ConstantPoolIndex, ConstantPoolIndex) -> ConstantPoolEntry,
        memberName: String,
        descriptor: String,
    ): List<ConstantPoolEntry> =
        listOf(
            ConstantUtf8Entry("Code", byteArrayOf()),
            createMemberRef(ConstantPoolIndex(3), ConstantPoolIndex(4)),
            ConstantClassEntry(ConstantPoolIndex(5)),
            ConstantNameAndTypeEntry(ConstantPoolIndex(6), ConstantPoolIndex(7)),
            ConstantUtf8Entry("Example", byteArrayOf()),
            ConstantUtf8Entry(memberName, byteArrayOf()),
            ConstantUtf8Entry(descriptor, byteArrayOf()),
        )

    private fun codeAttributeBytes(code: ByteArray, maxLocals: Int): ByteArray {
        val attributeLength = 12 + code.size
        return byteArrayOf(
            0,
            1,
            0,
            1,
        ) + intBytes(attributeLength) +
            byteArrayOf(
                0,
                1,
                (maxLocals ushr 8).toByte(),
                maxLocals.toByte(),
            ) + intBytes(code.size) +
            code +
            byteArrayOf(
                0,
                0,
                0,
                0,
            )
    }

    private fun intBytes(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )
}
