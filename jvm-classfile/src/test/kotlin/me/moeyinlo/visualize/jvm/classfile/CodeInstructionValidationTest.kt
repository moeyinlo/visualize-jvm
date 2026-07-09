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

    private fun parseCodeAttribute(
        code: ByteArray,
        constantPool: ConstantPool = ConstantPool.fromEntries(listOf(ConstantUtf8Entry("Code", byteArrayOf()))),
    ): AttributeInfo {
        return AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                codeAttributeBytes(code),
                source = "code-instruction-validation.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
            ownerPath = "methods[0]",
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

    private fun codeAttributeBytes(code: ByteArray): ByteArray {
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
                0,
                1,
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
