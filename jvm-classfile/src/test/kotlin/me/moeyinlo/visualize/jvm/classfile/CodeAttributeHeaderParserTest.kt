package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CodeAttributeHeaderParserTest {
    @Test
    fun `parses Code attribute header and bytecode array`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("run", byteArrayOf()),
                ConstantUtf8Entry("()V", byteArrayOf()),
            ),
        )
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                1,
                0,
                0x01,
                0,
                2,
                0,
                3,
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                13,
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
                0,
            ),
            source = "code-method.class",
        )

        val methods = MethodInfoParser.parseMethods(
            reader = reader,
            constantPool = constantPool,
            attributeParsers = AttributeParserRegistry.of("Code" to CodeAttributeParser),
        )

        val attribute = assertIs<CodeAttribute>(methods.single().attributes.single())
        assertEquals(ConstantPoolIndex(1), attribute.nameIndex)
        assertEquals(0, attribute.maxStack)
        assertEquals(1, attribute.maxLocals)
        assertContentEquals(byteArrayOf(0xB1.toByte()), attribute.code)
        assertEquals(29, reader.position)
    }

    @Test
    fun `copies Code bytecode array defensively`() {
        val attribute = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(0x2A, 0xB0.toByte()),
        )

        attribute.code[0] = 0

        assertContentEquals(byteArrayOf(0x2A, 0xB0.toByte()), attribute.code)
    }

    @Test
    fun `rejects empty Code bytecode array`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
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
                        12,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                    ),
                    source = "empty-code.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("code_length"), failure.message)
        assertTrue(failure.message.orEmpty().contains("greater than zero"), failure.message)
    }

    @Test
    fun `rejects duplicate StackMapTable attributes in Code`() {
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
                        0, 1,
                        0, 1,
                        0, 0, 0, 29,
                        0, 0,
                        0, 1,
                        0, 0, 0, 1,
                        0xB1.toByte(),
                        0, 0,
                        0, 2,
                        0, 2, 0, 0, 0, 2, 0, 0,
                        0, 2, 0, 0, 0, 2, 0, 0,
                    ),
                    source = "duplicate-stack-map-code.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "StackMapTable" to StackMapTableAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("StackMapTable"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects nested Code attributes in Code`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 32,
                        0, 0,
                        0, 0,
                        0, 0, 0, 1,
                        0xB1.toByte(),
                        0, 0,
                        0, 1,
                        0, 1,
                        0, 0, 0, 13,
                        0, 0,
                        0, 0,
                        0, 0, 0, 1,
                        0xB1.toByte(),
                        0, 0,
                        0, 0,
                    ),
                    source = "nested-code.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Code" to CodeAttributeParser),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("Code"), failure.message)
        assertTrue(failure.message.orEmpty().contains("methods[0].attributes[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method_info"), failure.message)
    }

    @Test
    fun `rejects ConstantValue attributes in Code`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("ConstantValue", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 21,
                        0, 0,
                        0, 0,
                        0, 0, 0, 1,
                        0xB1.toByte(),
                        0, 0,
                        0, 1,
                        0, 2,
                        0, 0, 0, 2,
                        0, 3,
                    ),
                    source = "code-constant-value.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "ConstantValue" to ConstantValueAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("ConstantValue"), failure.message)
        assertTrue(failure.message.orEmpty().contains("methods[0].attributes[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field_info"), failure.message)
    }

    @Test
    fun `rejects SourceFile attributes in Code`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("SourceFile", byteArrayOf()),
                ConstantUtf8Entry("Test.java", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 21,
                        0, 0,
                        0, 0,
                        0, 0, 0, 1,
                        0xB1.toByte(),
                        0, 0,
                        0, 1,
                        0, 2,
                        0, 0, 0, 2,
                        0, 3,
                    ),
                    source = "code-sourcefile.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "SourceFile" to SourceFileAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("SourceFile"), failure.message)
        assertTrue(failure.message.orEmpty().contains("methods[0].attributes[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects SourceDebugExtension attributes in Code`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("SourceDebugExtension", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 23,
                        0, 0,
                        0, 0,
                        0, 0, 0, 1,
                        0xB1.toByte(),
                        0, 0,
                        0, 1,
                        0, 2,
                        0, 0, 0, 4,
                        83, 77, 65, 80,
                    ),
                    source = "code-source-debug-extension.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "SourceDebugExtension" to SourceDebugExtensionAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("SourceDebugExtension"), failure.message)
        assertTrue(failure.message.orEmpty().contains("methods[0].attributes[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects duplicate RuntimeVisibleTypeAnnotations attributes in Code`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("RuntimeVisibleTypeAnnotations", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 29,
                        0, 0,
                        0, 1,
                        0, 0, 0, 1,
                        0xB1.toByte(),
                        0, 0,
                        0, 2,
                        0, 2, 0, 0, 0, 2, 0, 0,
                        0, 2, 0, 0, 0, 2, 0, 0,
                    ),
                    source = "duplicate-runtime-visible-type-annotations-code.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "RuntimeVisibleTypeAnnotations" to RuntimeVisibleTypeAnnotationsAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleTypeAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate RuntimeInvisibleTypeAnnotations attributes in Code`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Code", byteArrayOf()),
                ConstantUtf8Entry("RuntimeInvisibleTypeAnnotations", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 29,
                        0, 0,
                        0, 1,
                        0, 0, 0, 1,
                        0xB1.toByte(),
                        0, 0,
                        0, 2,
                        0, 2, 0, 0, 0, 2, 0, 0,
                        0, 2, 0, 0, 0, 2, 0, 0,
                    ),
                    source = "duplicate-runtime-invisible-type-annotations-code.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Code" to CodeAttributeParser,
                    "RuntimeInvisibleTypeAnnotations" to RuntimeInvisibleTypeAnnotationsAttributeParser,
                ),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("RuntimeInvisibleTypeAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }
}
