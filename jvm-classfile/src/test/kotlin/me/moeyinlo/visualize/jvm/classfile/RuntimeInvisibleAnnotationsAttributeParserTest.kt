package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RuntimeInvisibleAnnotationsAttributeParserTest {
    @Test
    fun `parses RuntimeInvisibleAnnotations`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeInvisibleAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Invisible;", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("hidden", byteArrayOf()),
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
                    11,
                    0,
                    1,
                    0,
                    2,
                    0,
                    1,
                    0,
                    3,
                    's'.code.toByte(),
                    0,
                    4,
                ),
                source = "runtime-invisible-annotations.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("RuntimeInvisibleAnnotations" to RuntimeInvisibleAnnotationsAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<RuntimeInvisibleAnnotationsAttribute>(attributes.single())
        val annotation = attribute.annotations.single()
        assertEquals(ConstantPoolIndex(2), annotation.typeIndex)
        assertEquals(ConstantPoolIndex(3), annotation.elementValuePairs.single().elementNameIndex)
        assertEquals(
            ConstantPoolIndex(4),
            assertIs<ElementValue.Const>(annotation.elementValuePairs.single().value).constValueIndex,
        )
    }

    @Test
    fun `rejects invalid RuntimeInvisibleAnnotations element value`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeInvisibleAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Invisible;", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
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
                        9,
                        0,
                        1,
                        0,
                        2,
                        0,
                        1,
                        0,
                        3,
                        '?'.code.toByte(),
                    ),
                    source = "bad-runtime-invisible-annotations.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeInvisibleAnnotations" to RuntimeInvisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("element_value tag"), failure.message)
    }
}
