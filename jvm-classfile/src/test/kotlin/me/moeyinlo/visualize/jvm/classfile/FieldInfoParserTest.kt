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
    fun `copies raw attribute bytes defensively`() {
        val attribute = RawAttributeInfo(ConstantPoolIndex(1), byteArrayOf(1, 2, 3))

        attribute.info[0] = 99

        assertContentEquals(byteArrayOf(1, 2, 3), attribute.info)
    }
}
