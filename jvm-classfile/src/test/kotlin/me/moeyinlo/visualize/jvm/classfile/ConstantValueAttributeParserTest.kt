package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConstantValueAttributeParserTest {
    @Test
    fun `parses ConstantValue field attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ConstantValue", byteArrayOf()),
                ConstantUtf8Entry("VALUE", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantIntegerEntry(42),
            ),
        )
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                1,
                0,
                0x19,
                0,
                2,
                0,
                3,
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                2,
                0,
                4,
            ),
            source = "constant-value-field.class",
        )

        val fields = FieldInfoParser.parseFields(
            reader = reader,
            constantPool = constantPool,
            attributeParsers = AttributeParserRegistry.of("ConstantValue" to ConstantValueAttributeParser),
        )

        val attribute = assertIs<ConstantValueAttribute>(fields.single().attributes.single())
        assertEquals(ConstantPoolIndex(1), attribute.nameIndex)
        assertEquals(ConstantPoolIndex(4), attribute.constantValueIndex)
        assertEquals(18, reader.position)
    }

    @Test
    fun `rejects ConstantValue attribute length other than two`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ConstantValue", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 3, 0, 1, 0),
                    source = "bad-constant-value.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("ConstantValue" to ConstantValueAttributeParser),
                ownerPath = "fields[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ConstantValue"), failure.message)
        assertTrue(failure.message.orEmpty().contains("attribute_length"), failure.message)
        assertTrue(failure.message.orEmpty().contains("2"), failure.message)
    }

    @Test
    fun `rejects ConstantValue index that does not reference a constant value entry`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("ConstantValue", byteArrayOf()),
                ConstantUtf8Entry("java/lang/Object", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 3),
                    source = "bad-constant-value-index.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("ConstantValue" to ConstantValueAttributeParser),
                ownerPath = "fields[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("constantvalue_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Integer"), failure.message)
    }
}
