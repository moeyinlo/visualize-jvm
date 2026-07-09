package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FieldInfoParserTest {
    @Test
    fun `parses field declarations including raw attributes`() {
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                2,
                0,
                0x19,
                0,
                3,
                0,
                4,
                0,
                0,
                0,
                0x02,
                0,
                5,
                0,
                6,
                0,
                1,
                0,
                7,
                0,
                0,
                0,
                3,
                10,
                11,
                12,
            ),
            source = "fields.class",
        )

        val fields = FieldInfoParser.parseFields(reader)

        assertEquals(2, fields.size)
        assertEquals(0x0019, fields[0].accessFlags)
        assertEquals(ConstantPoolIndex(3), fields[0].nameIndex)
        assertEquals(ConstantPoolIndex(4), fields[0].descriptorIndex)
        assertEquals(emptyList(), fields[0].attributes)

        assertEquals(0x0002, fields[1].accessFlags)
        assertEquals(ConstantPoolIndex(5), fields[1].nameIndex)
        assertEquals(ConstantPoolIndex(6), fields[1].descriptorIndex)
        val attribute = assertIs<RawAttributeInfo>(fields[1].attributes.single())
        assertEquals(ConstantPoolIndex(7), attribute.nameIndex)
        assertContentEquals(byteArrayOf(10, 11, 12), attribute.info)
        assertEquals(27, reader.position)
    }

    @Test
    fun `rejects zero field name index`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            FieldInfoParser.parseFields(
                ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 0),
                    source = "bad-field.class",
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("fields[0].name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `validates field names and descriptors when constant pool is available`() {
        val fields = FieldInfoParser.parseFields(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 1, 0, 2, 0, 0),
                source = "validated-fields.class",
            ),
            constantPool = fieldValidationPool("value", "I"),
            attributeParsers = AttributeParserRegistry.Empty,
            classKind = ClassFileKind.Class,
        )

        assertEquals(ConstantPoolIndex(1), fields.single().nameIndex)
        assertEquals(ConstantPoolIndex(2), fields.single().descriptorIndex)
    }

    @Test
    fun `rejects field names that are not valid unqualified names`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            FieldInfoParser.parseFields(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 1, 0, 2, 0, 0),
                    source = "bad-field.class",
                ),
                constantPool = fieldValidationPool("bad/name", "I"),
                attributeParsers = AttributeParserRegistry.Empty,
                classKind = ClassFileKind.Class,
            )
        }

        assertTrue(failure.message.orEmpty().contains("fields[0].name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unqualified name"), failure.message)
    }

    @Test
    fun `rejects field descriptors that are not field descriptors`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            FieldInfoParser.parseFields(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 1, 0, 2, 0, 0),
                    source = "bad-field.class",
                ),
                constantPool = fieldValidationPool("value", "()V"),
                attributeParsers = AttributeParserRegistry.Empty,
                classKind = ClassFileKind.Class,
            )
        }

        assertTrue(failure.message.orEmpty().contains("fields[0].descriptor_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field descriptor"), failure.message)
    }

    @Test
    fun `rejects duplicate field name and descriptor pairs`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            FieldInfoParser.parseFields(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 2,
                        0, 1, 0, 1, 0, 2, 0, 0,
                        0, 2, 0, 1, 0, 2, 0, 0,
                    ),
                    source = "bad-field.class",
                ),
                constantPool = fieldValidationPool("value", "I"),
                attributeParsers = AttributeParserRegistry.Empty,
                classKind = ClassFileKind.Class,
            )
        }

        assertTrue(failure.message.orEmpty().contains("Duplicate field_info"), failure.message)
        assertTrue(failure.message.orEmpty().contains("fields[1]"), failure.message)
    }

    @Test
    fun `rejects class fields with multiple access visibility flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            FieldInfoParser.parseFields(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 0x03, 0, 1, 0, 2, 0, 0),
                    source = "bad-field.class",
                ),
                constantPool = fieldValidationPool("value", "I"),
                attributeParsers = AttributeParserRegistry.Empty,
                classKind = ClassFileKind.Class,
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_PUBLIC"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_PRIVATE"), failure.message)
    }

    @Test
    fun `rejects class fields that are both final and volatile`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            FieldInfoParser.parseFields(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 0x50, 0, 1, 0, 2, 0, 0),
                    source = "bad-field.class",
                ),
                constantPool = fieldValidationPool("value", "I"),
                attributeParsers = AttributeParserRegistry.Empty,
                classKind = ClassFileKind.Class,
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_FINAL"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_VOLATILE"), failure.message)
    }

    @Test
    fun `rejects interface fields without public static final`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            FieldInfoParser.parseFields(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 0x09, 0, 1, 0, 2, 0, 0),
                    source = "bad-field.class",
                ),
                constantPool = fieldValidationPool("value", "I"),
                attributeParsers = AttributeParserRegistry.Empty,
                classKind = ClassFileKind.Interface,
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_PUBLIC"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_STATIC"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_FINAL"), failure.message)
    }

    @Test
    fun `copies raw attribute bytes defensively`() {
        val attribute = RawAttributeInfo(ConstantPoolIndex(1), byteArrayOf(1, 2, 3))

        attribute.info[0] = 99

        assertContentEquals(byteArrayOf(1, 2, 3), attribute.info)
    }

    private fun fieldValidationPool(
        name: String,
        descriptor: String,
    ): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry(name, name.encodeToByteArray()),
                ConstantUtf8Entry(descriptor, descriptor.encodeToByteArray()),
            ),
        )
}
