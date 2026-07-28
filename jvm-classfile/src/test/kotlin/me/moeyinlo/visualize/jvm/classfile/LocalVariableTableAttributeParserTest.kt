package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalVariableTableAttributeParserTest {
    @Test
    fun `parses LocalVariableTable entries`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTable", byteArrayOf()),
                ConstantUtf8Entry("arg", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 0, 0, 5, 0, 2, 0, 3, 0, 1),
                source = "local-variable-table.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("LocalVariableTable" to LocalVariableTableAttributeParser),
            ownerPath = "methods[0].attributes[0]",
        )

        val entry = assertIs<LocalVariableTableAttribute>(attributes.single()).entries.single()
        assertEquals(0, entry.startPc)
        assertEquals(5, entry.length)
        assertEquals(ConstantPoolIndex(2), entry.nameIndex)
        assertEquals(ConstantPoolIndex(3), entry.descriptorIndex)
        assertEquals(1, entry.index)
    }

    @Test
    fun `rejects LocalVariableTable descriptor index that is not UTF-8`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTable", byteArrayOf()),
                ConstantUtf8Entry("arg", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 0, 0, 5, 0, 2, 0, 3, 0, 1),
                    source = "bad-local-variable-table.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("LocalVariableTable" to LocalVariableTableAttributeParser),
                ownerPath = "methods[0].attributes[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("descriptor_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }

    @Test
    fun `rejects LocalVariableTable names that are not unqualified names`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTable", byteArrayOf()),
                ConstantUtf8Entry("bad/name", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 0, 0, 5, 0, 2, 0, 3, 0, 1),
                    source = "bad-local-variable-table-name.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("LocalVariableTable" to LocalVariableTableAttributeParser),
                ownerPath = "methods[0].attributes[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unqualified name"), failure.message)
    }

    @Test
    fun `rejects LocalVariableTable descriptors that are not field descriptors`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTable", byteArrayOf()),
                ConstantUtf8Entry("arg", byteArrayOf()),
                ConstantUtf8Entry("()V", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 0, 0, 5, 0, 2, 0, 3, 0, 1),
                    source = "bad-local-variable-table-descriptor.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("LocalVariableTable" to LocalVariableTableAttributeParser),
                ownerPath = "methods[0].attributes[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("descriptor_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field descriptor"), failure.message)
    }
}
