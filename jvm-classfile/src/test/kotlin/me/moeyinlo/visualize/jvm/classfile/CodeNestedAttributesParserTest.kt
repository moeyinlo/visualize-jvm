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
    fun `accepts StackMapTable uninitialized variable offsets that point to new instructions`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("StackMapTable", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(4)),
                ConstantUtf8Entry("pkg/Foo", byteArrayOf()),
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
                    28,
                    0,
                    1,
                    0,
                    1,
                    0,
                    0,
                    0,
                    4,
                    0xBB.toByte(),
                    0,
                    3,
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
                    6,
                    0,
                    1,
                    64,
                    8,
                    0,
                    0,
                ),
                source = "stack-map-uninitialized-offset.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of(
                "Code" to CodeAttributeParser,
                "StackMapTable" to StackMapTableAttributeParser,
            ),
            ownerPath = "methods[0]",
        )

        val code = assertIs<CodeAttribute>(attributes.single())
        val stackMapTable = assertIs<StackMapTableAttribute>(code.attributes.single())
        val frame = assertIs<SameLocalsOneStackItemFrame>(stackMapTable.entries.single())
        assertEquals(0, assertIs<VerificationTypeInfo.UninitializedVariable>(frame.stack).offset)
    }

    @Test
    fun `rejects StackMapTable uninitialized variable offsets that do not point to new instructions`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("StackMapTable", byteArrayOf()),
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
                        27,
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        3,
                        0x10,
                        0,
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
                        6,
                        0,
                        1,
                        64,
                        8,
                        0,
                        1,
                    ),
                    source = "bad-stack-map-uninitialized-offset.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "StackMapTable" to StackMapTableAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("Uninitialized_variable_info"), message)
        assertTrue(message.contains("offset=1"), message)
        assertTrue(message.contains("new"), message)
    }

    @Test
    fun `rejects StackMapTable frame offsets that do not point to instruction opcodes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("StackMapTable", byteArrayOf()),
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
                        24,
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        3,
                        0x10,
                        0,
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
                        3,
                        0,
                        1,
                        1,
                    ),
                    source = "bad-stack-map-frame-offset.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "StackMapTable" to StackMapTableAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("StackMapTable"), message)
        assertTrue(message.contains("frame offset=1"), message)
        assertTrue(message.contains("opcode of an instruction"), message)
    }

    @Test
    fun `rejects LineNumberTable entries whose start pc does not point to an instruction opcode`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("LineNumberTable", byteArrayOf()),
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
                        27,
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        3,
                        0x10,
                        0,
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
                        6,
                        0,
                        1,
                        0,
                        1,
                        0,
                        123,
                    ),
                    source = "bad-line-number-table-start-pc.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "LineNumberTable" to LineNumberTableAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("LineNumberTable"), message)
        assertTrue(message.contains("start_pc=1"), message)
        assertTrue(message.contains("opcode of an instruction"), message)
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

    @Test
    fun `rejects Code type annotation local variable target range outside code array`() {
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
                        35,
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
                        16,
                        0,
                        1,
                        0x40,
                        0,
                        1,
                        0,
                        0,
                        0,
                        2,
                        0,
                        0,
                        0,
                        0,
                        3,
                        0,
                        0,
                    ),
                    source = "bad-code-type-annotation-localvar.class",
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
        assertTrue(message.contains("localvar_target.table[0]"), message)
        assertTrue(message.contains("start_pc=0"), message)
        assertTrue(message.contains("length=2"), message)
        assertTrue(message.contains("code_length=1"), message)
    }

    @Test
    fun `rejects Code type annotation local variable target start pc inside instruction operands`() {
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
                        37,
                        0,
                        0,
                        0,
                        1,
                        0,
                        0,
                        0,
                        3,
                        0x10,
                        0,
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
                        16,
                        0,
                        1,
                        0x40,
                        0,
                        1,
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        0,
                        3,
                        0,
                        0,
                    ),
                    source = "bad-code-type-annotation-localvar-start.class",
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
        assertTrue(message.contains("localvar_target.table[0].start_pc=1"), message)
        assertTrue(message.contains("opcode of an instruction"), message)
    }

    @Test
    fun `rejects Code type annotation local variable target end pc inside instruction operands`() {
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
                        37,
                        0,
                        0,
                        0,
                        1,
                        0,
                        0,
                        0,
                        3,
                        0x10,
                        0,
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
                        16,
                        0,
                        1,
                        0x40,
                        0,
                        1,
                        0,
                        0,
                        0,
                        1,
                        0,
                        0,
                        0,
                        0,
                        3,
                        0,
                        0,
                    ),
                    source = "bad-code-type-annotation-localvar-end.class",
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
        assertTrue(message.contains("localvar_target.table[0].end_pc=1"), message)
        assertTrue(message.contains("code_length=3"), message)
        assertTrue(message.contains("opcode of an instruction"), message)
    }

    @Test
    fun `rejects Code type annotation local variable target index outside max locals`() {
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
                        35,
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
                        16,
                        0,
                        1,
                        0x40,
                        0,
                        1,
                        0,
                        0,
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        3,
                        0,
                        0,
                    ),
                    source = "bad-code-type-annotation-localvar-index.class",
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
        assertTrue(message.contains("localvar_target.table[0].index=1"), message)
        assertTrue(message.contains("max_locals=1"), message)
    }
}
