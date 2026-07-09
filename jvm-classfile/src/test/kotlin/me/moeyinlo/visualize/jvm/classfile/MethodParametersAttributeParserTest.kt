package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MethodParametersAttributeParserTest {
    @Test
    fun `parses MethodParameters attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("MethodParameters", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 9, 2, 0, 2, 0x90.toByte(), 0x10, 0, 0, 0, 0),
                source = "method-parameters.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("MethodParameters" to MethodParametersAttributeParser),
            ownerPath = "methods[0]",
        )

        val attribute = assertIs<MethodParametersAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(2), attribute.parameters[0].nameIndex)
        assertEquals(0x9010, attribute.parameters[0].accessFlags)
        assertNull(attribute.parameters[1].nameIndex)
        assertEquals(0, attribute.parameters[1].accessFlags)
    }

    @Test
    fun `rejects nonzero parameter name index that is not UTF-8`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("MethodParameters", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 5, 1, 0, 2, 0, 0),
                    source = "bad-method-parameters.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("MethodParameters" to MethodParametersAttributeParser),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("parameters[0].name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }
}
