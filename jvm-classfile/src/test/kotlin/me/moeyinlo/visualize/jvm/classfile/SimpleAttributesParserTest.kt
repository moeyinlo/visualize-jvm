package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SimpleAttributesParserTest {
    @Test
    fun `parses zero length marker attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Synthetic", byteArrayOf()),
                ConstantUtf8Entry("Deprecated", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 2, 0, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0),
                source = "marker-attributes.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of(
                "Synthetic" to SyntheticAttributeParser,
                "Deprecated" to DeprecatedAttributeParser,
            ),
            ownerPath = "ClassFile",
        )

        assertIs<SyntheticAttribute>(attributes[0])
        assertIs<DeprecatedAttribute>(attributes[1])
    }

    @Test
    fun `parses SourceFile attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("SourceFile", byteArrayOf()),
                ConstantUtf8Entry("Main.java", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                source = "source-file.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<SourceFileAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(2), attribute.sourceFileIndex)
    }

    @Test
    fun `rejects marker attribute with nonzero length`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Synthetic", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 1, 0),
                    source = "bad-synthetic.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Synthetic" to SyntheticAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("Synthetic"), failure.message)
        assertTrue(failure.message.orEmpty().contains("attribute_length"), failure.message)
    }

    @Test
    fun `rejects SourceFile index that is not a UTF-8 constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("SourceFile", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "bad-source-file.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("sourcefile_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }
}
