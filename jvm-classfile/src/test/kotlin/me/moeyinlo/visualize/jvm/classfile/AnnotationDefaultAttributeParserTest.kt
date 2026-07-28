package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AnnotationDefaultAttributeParserTest {
    @Test
    fun `parses AnnotationDefault element value`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("AnnotationDefault", byteArrayOf()),
                ConstantUtf8Entry("default-value", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 3, 's'.code.toByte(), 0, 2),
                source = "annotation-default.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("AnnotationDefault" to AnnotationDefaultAttributeParser),
            ownerPath = "methods[0]",
        )

        val attribute = assertIs<AnnotationDefaultAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(2), assertIs<ElementValue.Const>(attribute.defaultValue).constValueIndex)
    }

    @Test
    fun `rejects AnnotationDefault attributes before Java 5`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("AnnotationDefault", byteArrayOf()),
                ConstantUtf8Entry("default-value", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 3, 's'.code.toByte(), 0, 2),
                    source = "java4-annotation-default.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("AnnotationDefault" to AnnotationDefaultAttributeParser),
                ownerPath = "methods[0]",
                majorVersion = 48,
            )
        }

        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version=48"), failure.message)
        assertTrue(failure.message.orEmpty().contains("49"), failure.message)
    }

    @Test
    fun `rejects invalid AnnotationDefault element value`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("AnnotationDefault", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 1, '?'.code.toByte()),
                    source = "bad-annotation-default.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("AnnotationDefault" to AnnotationDefaultAttributeParser),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("element_value tag"), failure.message)
    }
}
