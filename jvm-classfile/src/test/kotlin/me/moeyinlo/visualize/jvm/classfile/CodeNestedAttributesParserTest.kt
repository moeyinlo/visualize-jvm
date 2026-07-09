package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    @Test
    fun `reports original source nested path and absolute byte offset for malformed nested Code attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("NeedsTwoBytes", byteArrayOf()),
            ),
        )
        val registry = AttributeParserRegistry.of(
            "Code" to CodeAttributeParser,
            "NeedsTwoBytes" to AttributeBodyParser { context ->
                context.reader.readU2()
                UnknownAttributeInfo(
                    nameIndex = context.nameIndex,
                    name = context.name,
                    info = context.info,
                )
            },
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
                        20,
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
                        1,
                        99,
                    ),
                    source = "nested-code-diagnostic.class",
                ),
                constantPool = constantPool,
                registry = registry,
                ownerPath = "methods[0]",
            )
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("source=nested-code-diagnostic.class"), message)
        assertTrue(message.contains("path=methods[0].attributes[0].attributes[0]"), message)
        assertTrue(message.contains("offset=27"), message)
        assertTrue(message.contains("Unexpected end of classfile"), message)
    }
}
