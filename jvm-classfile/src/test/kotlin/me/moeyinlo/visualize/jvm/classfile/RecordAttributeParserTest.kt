package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecordAttributeParserTest {
    @Test
    fun `parses Record attribute components and nested attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("VendorRecordMetadata", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(
                    0, 1,
                    0, 1,
                    0, 0, 0, 16,
                    0, 1,
                    0, 2, 0, 3,
                    0, 1,
                    0, 4, 0, 0, 0, 2, 7, 8,
                ),
                source = "record.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
            ownerPath = "ClassFile",
        )

        val component = assertIs<RecordAttribute>(attributes.single()).components.single()
        assertEquals(ConstantPoolIndex(2), component.nameIndex)
        assertEquals(ConstantPoolIndex(3), component.descriptorIndex)
        val nested = assertIs<UnknownAttributeInfo>(component.attributes.single())
        assertEquals("VendorRecordMetadata", nested.name)
        assertContentEquals(byteArrayOf(7, 8), nested.info)
    }

    @Test
    fun `rejects component name index that is not UTF-8`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 8, 0, 1, 0, 2, 0, 2, 0, 0),
                    source = "bad-record.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0].name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }
}
