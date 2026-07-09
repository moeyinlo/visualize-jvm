package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CodeNestedAttributesParserTest {
    @Test
    fun `parses attributes nested inside Code attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("VendorCodeMetadata", byteArrayOf()),
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
                    0,
                    0,
                    1,
                    0,
                    0,
                    0,
                    1,
                    0xB1.toByte(),
                    0,
                    0,
                    0,
                    1,
                    0,
                    2,
                    0,
                    0,
                    0,
                    2,
                    9,
                    10,
                ),
                source = "nested-code-attributes.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
            ownerPath = "methods[0]",
        )

        val code = assertIs<CodeAttribute>(attributes.single())
        val nested = assertIs<UnknownAttributeInfo>(code.attributes.single())
        assertEquals(ConstantPoolIndex(2), nested.nameIndex)
        assertEquals("VendorCodeMetadata", nested.name)
        assertContentEquals(byteArrayOf(9, 10), nested.info)
    }
}
