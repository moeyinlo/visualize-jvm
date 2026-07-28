package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CodeExceptionTableParserTest {
    @Test
    fun `parses Code exception table entries`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("java/lang/Throwable", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(
                    0,
                    1,
                    0,
                    1,
                    0,
                    0,
                    0,
                    22,
                    0,
                    1,
                    0,
                    1,
                    0,
                    0,
                    0,
                    2,
                    0x00,
                    0xBF.toByte(),
                    0,
                    1,
                    0,
                    0,
                    0,
                    1,
                    0,
                    1,
                    0,
                    2,
                    0,
                    0,
                ),
                source = "code-exception-table.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
            ownerPath = "methods[0]",
        )

        val handler = assertIs<CodeAttribute>(attributes.single()).exceptionTable.single()
        assertEquals(0, handler.startPc)
        assertEquals(1, handler.endPc)
        assertEquals(1, handler.handlerPc)
        assertEquals(ConstantPoolIndex(2), handler.catchType)
    }

    @Test
    fun `parses finally handler catch type zero`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(
                    0,
                    1,
                    0,
                    1,
                    0,
                    0,
                    0,
                    21,
                    0,
                    1,
                    0,
                    1,
                    0,
                    0,
                    0,
                    1,
                    0xBF.toByte(),
                    0,
                    1,
                    0,
                    0,
                    0,
                    1,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                ),
                source = "finally-handler.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
            ownerPath = "methods[0]",
        )

        assertNull(assertIs<CodeAttribute>(attributes.single()).exceptionTable.single().catchType)
    }

    @Test
    fun `rejects exception table catch type that is not a class constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        21,
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        1,
                        0xBF.toByte(),
                        0,
                        1,
                        0,
                        0,
                        0,
                        1,
                        0,
                        0,
                        0,
                        2,
                        0,
                        0,
                    ),
                    source = "bad-catch-type.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("catch_type"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class"), failure.message)
    }

    @Test
    fun `rejects exception table catch type with invalid class name`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("bad.name", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xBF.toByte()),
                exceptionTable = listOf(ExceptionHandlerBytes(startPc = 0, endPc = 1, handlerPc = 0, catchType = 2)),
                constantPool = constantPool,
            )
        }

        assertTrue(failure.message.orEmpty().contains("catch_type"), failure.message)
        assertTrue(failure.message.orEmpty().contains("internal form"), failure.message)
    }

    @Test
    fun `rejects exception table start pc that does not point to an instruction opcode`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xA7.toByte(), 0, 3, 0xB1.toByte()),
                exceptionTable = listOf(ExceptionHandlerBytes(startPc = 1, endPc = 3, handlerPc = 3)),
            )
        }

        assertTrue(failure.message.orEmpty().contains("start_pc"), failure.message)
        assertTrue(failure.message.orEmpty().contains("opcode"), failure.message)
    }

    @Test
    fun `rejects exception table end pc that is neither code length nor an instruction opcode`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xA7.toByte(), 0, 3, 0xB1.toByte()),
                exceptionTable = listOf(ExceptionHandlerBytes(startPc = 0, endPc = 2, handlerPc = 3)),
            )
        }

        assertTrue(failure.message.orEmpty().contains("end_pc"), failure.message)
        assertTrue(failure.message.orEmpty().contains("opcode"), failure.message)
    }

    @Test
    fun `rejects exception table handler pc that does not point to an instruction opcode`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseCodeAttribute(
                code = byteArrayOf(0xA7.toByte(), 0, 3, 0xB1.toByte()),
                exceptionTable = listOf(ExceptionHandlerBytes(startPc = 0, endPc = 3, handlerPc = 2)),
            )
        }

        assertTrue(failure.message.orEmpty().contains("handler_pc"), failure.message)
        assertTrue(failure.message.orEmpty().contains("opcode"), failure.message)
    }

    private fun parseCodeAttribute(
        code: ByteArray,
        exceptionTable: List<ExceptionHandlerBytes>,
        constantPool: ConstantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
            ),
        ),
    ): AttributeInfo {
        return AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                codeAttributeBytes(code, exceptionTable),
                source = "code-exception-table-boundary.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
            ownerPath = "methods[0]",
        ).single()
    }

    private fun codeAttributeBytes(
        code: ByteArray,
        exceptionTable: List<ExceptionHandlerBytes>,
    ): ByteArray {
        val attributeLength = 12 + code.size + exceptionTable.size * 8
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
            byteArrayOf(0, exceptionTable.size.toByte()) +
            exceptionTable.fold(byteArrayOf()) { bytes, handler -> bytes + exceptionHandlerBytes(handler) } +
            byteArrayOf(0, 0)
    }

    private fun intBytes(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )

    private fun shortBytes(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 8).toByte(),
            value.toByte(),
        )

    private data class ExceptionHandlerBytes(
        val startPc: Int,
        val endPc: Int,
        val handlerPc: Int,
        val catchType: Int = 0,
    )

    private fun exceptionHandlerBytes(handler: ExceptionHandlerBytes): ByteArray =
        shortBytes(handler.startPc) +
            shortBytes(handler.endPc) +
            shortBytes(handler.handlerPc) +
            shortBytes(handler.catchType)
}
