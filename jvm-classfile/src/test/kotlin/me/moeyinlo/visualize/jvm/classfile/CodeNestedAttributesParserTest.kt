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

    @Test
    fun `rejects Code type annotation catch target outside exception table`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("RuntimeVisibleTypeAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/TypeUse;", byteArrayOf()),
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
                        29,
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
                        10,
                        0,
                        1,
                        0x42,
                        0,
                        0,
                        0,
                        0,
                        3,
                        0,
                        0,
                    ),
                    source = "bad-code-type-annotation-catch.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "RuntimeVisibleTypeAnnotations" to RuntimeVisibleTypeAnnotationsAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("exception_table_index"), message)
        assertTrue(message.contains("0"), message)
        assertTrue(message.contains("exception_table_length=0"), message)
    }

    @Test
    fun `rejects Code type annotation offset target outside code array`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("RuntimeVisibleTypeAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/TypeUse;", byteArrayOf()),
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
                        29,
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
                        10,
                        0,
                        1,
                        0x43,
                        0,
                        1,
                        0,
                        0,
                        3,
                        0,
                        0,
                    ),
                    source = "bad-code-type-annotation-offset.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "RuntimeVisibleTypeAnnotations" to RuntimeVisibleTypeAnnotationsAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("target_info.offset"), message)
        assertTrue(message.contains("1"), message)
        assertTrue(message.contains("opcode of an instruction"), message)
    }

    @Test
    fun `rejects Code type annotation type argument target outside code array`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("RuntimeVisibleTypeAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/TypeUse;", byteArrayOf()),
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
                        30,
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
                        11,
                        0,
                        1,
                        0x47,
                        0,
                        1,
                        0,
                        0,
                        0,
                        3,
                        0,
                        0,
                    ),
                    source = "bad-code-type-annotation-type-argument.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "RuntimeVisibleTypeAnnotations" to RuntimeVisibleTypeAnnotationsAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("target_info.offset"), message)
        assertTrue(message.contains("1"), message)
        assertTrue(message.contains("opcode of an instruction"), message)
    }
}
