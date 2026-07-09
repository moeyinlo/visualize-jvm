package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParameterAnnotationsAttributeParserTest {
    @Test
    fun `parses RuntimeVisibleParameterAnnotations`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleParameterAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Param;", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantIntegerEntry(1),
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
                    14,
                    2,
                    0,
                    1,
                    0,
                    2,
                    0,
                    1,
                    0,
                    3,
                    'Z'.code.toByte(),
                    0,
                    4,
                    0,
                    0,
                ),
                source = "visible-parameter-annotations.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of(
                "RuntimeVisibleParameterAnnotations" to RuntimeVisibleParameterAnnotationsAttributeParser,
            ),
            ownerPath = "methods[0]",
        )

        val attribute = assertIs<RuntimeVisibleParameterAnnotationsAttribute>(attributes.single())
        assertEquals(2, attribute.parameterAnnotations.size)
        val annotation = attribute.parameterAnnotations[0].annotations.single()
        assertEquals(ConstantPoolIndex(2), annotation.typeIndex)
        assertEquals(
            ConstantPoolIndex(4),
            assertIs<ElementValue.Const>(annotation.elementValuePairs.single().value).constValueIndex,
        )
        assertEquals(emptyList(), attribute.parameterAnnotations[1].annotations)
    }

    @Test
    fun `parses RuntimeInvisibleParameterAnnotations`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeInvisibleParameterAnnotations", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 3, 1, 0, 0),
                source = "invisible-parameter-annotations.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of(
                "RuntimeInvisibleParameterAnnotations" to RuntimeInvisibleParameterAnnotationsAttributeParser,
            ),
            ownerPath = "methods[0]",
        )

        val attribute = assertIs<RuntimeInvisibleParameterAnnotationsAttribute>(attributes.single())
        assertEquals(1, attribute.parameterAnnotations.size)
        assertEquals(emptyList(), attribute.parameterAnnotations.single().annotations)
    }

    @Test
    fun `rejects invalid parameter annotation`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleParameterAnnotations", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 7, 1, 0, 1, 0, 2, 0, 0),
                    source = "bad-visible-parameter-annotations.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "RuntimeVisibleParameterAnnotations" to RuntimeVisibleParameterAnnotationsAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("type_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }
}
