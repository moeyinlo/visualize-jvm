package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExceptionsAttributeParserTest {
    @Test
    fun `parses Exceptions attribute exception index table`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Exceptions", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("java/io/IOException", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(5)),
                ConstantUtf8Entry("java/lang/ReflectiveOperationException", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 6, 0, 2, 0, 2, 0, 4),
                source = "exceptions.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Exceptions" to ExceptionsAttributeParser),
            ownerPath = "methods[0]",
        )

        val attribute = kotlin.test.assertIs<ExceptionsAttribute>(attributes.single())
        assertEquals(listOf(ConstantPoolIndex(2), ConstantPoolIndex(4)), attribute.exceptionIndexTable)
    }

    @Test
    fun `rejects Exceptions table entry that is not a class constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Exceptions", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 4, 0, 1, 0, 2),
                    source = "bad-exceptions.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Exceptions" to ExceptionsAttributeParser),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("exception_index_table"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class"), failure.message)
    }

    @Test
    fun `rejects Exceptions table entry that names an array type`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Exceptions", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("[Ljava/lang/Exception;", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 4, 0, 1, 0, 2),
                    source = "bad-exceptions-array.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Exceptions" to ExceptionsAttributeParser),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("exception_index_table"), failure.message)
        assertTrue(failure.message.orEmpty().contains("class type"), failure.message)
    }
}
