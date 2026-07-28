package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalVariableTypeTableAttributeParserTest {
    @Test
    fun `parses LocalVariableTypeTable entries`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTypeTable", byteArrayOf()),
                ConstantUtf8Entry("list", byteArrayOf()),
                ConstantUtf8Entry("Ljava/util/List<Ljava/lang/String;>;", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 0, 0, 7, 0, 2, 0, 3, 0, 2),
                source = "local-variable-type-table.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("LocalVariableTypeTable" to LocalVariableTypeTableAttributeParser),
            ownerPath = "methods[0].attributes[0]",
        )

        val entry = assertIs<LocalVariableTypeTableAttribute>(attributes.single()).entries.single()
        assertEquals(0, entry.startPc)
        assertEquals(7, entry.length)
        assertEquals(ConstantPoolIndex(2), entry.nameIndex)
        assertEquals(ConstantPoolIndex(3), entry.signatureIndex)
        assertEquals(2, entry.index)
    }

    @Test
    fun `rejects LocalVariableTypeTable attributes before Java 5`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTypeTable", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 0),
                    source = "java4-local-variable-type-table.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("LocalVariableTypeTable" to LocalVariableTypeTableAttributeParser),
                ownerPath = "methods[0].attributes[0]",
                majorVersion = 48,
            )
        }

        assertTrue(failure.message.orEmpty().contains("LocalVariableTypeTable"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version=48"), failure.message)
        assertTrue(failure.message.orEmpty().contains("49"), failure.message)
    }

    @Test
    fun `rejects LocalVariableTypeTable signature index that is not UTF-8`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTypeTable", byteArrayOf()),
                ConstantUtf8Entry("list", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 0, 0, 7, 0, 2, 0, 3, 0, 2),
                    source = "bad-local-variable-type-table.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("LocalVariableTypeTable" to LocalVariableTypeTableAttributeParser),
                ownerPath = "methods[0].attributes[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("signature_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }

    @Test
    fun `rejects LocalVariableTypeTable names that are not unqualified names`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTypeTable", byteArrayOf()),
                ConstantUtf8Entry("bad/name", byteArrayOf()),
                ConstantUtf8Entry("Ljava/util/List<Ljava/lang/String;>;", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 0, 0, 7, 0, 2, 0, 3, 0, 2),
                    source = "bad-local-variable-type-table-name.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("LocalVariableTypeTable" to LocalVariableTypeTableAttributeParser),
                ownerPath = "methods[0].attributes[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unqualified name"), failure.message)
    }

    @Test
    fun `rejects LocalVariableTypeTable signatures that are not field signatures`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LocalVariableTypeTable", byteArrayOf()),
                ConstantUtf8Entry("list", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 12, 0, 1, 0, 0, 0, 7, 0, 2, 0, 3, 0, 2),
                    source = "bad-local-variable-type-table-signature.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("LocalVariableTypeTable" to LocalVariableTypeTableAttributeParser),
                ownerPath = "methods[0].attributes[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("signature_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field signature"), failure.message)
    }
}
