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
}
