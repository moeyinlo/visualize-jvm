package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MethodInfoParserTest {
    @Test
    fun `parses method declarations including raw attributes`() {
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                2,
                0,
                0x01,
                0,
                3,
                0,
                4,
                0,
                0,
                0,
                0x09,
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
                2,
                20,
                21,
            ),
            source = "methods.class",
        )

        val methods = MethodInfoParser.parseMethods(reader)

        assertEquals(2, methods.size)
        assertEquals(0x0001, methods[0].accessFlags)
        assertEquals(ConstantPoolIndex(3), methods[0].nameIndex)
        assertEquals(ConstantPoolIndex(4), methods[0].descriptorIndex)
        assertEquals(emptyList(), methods[0].attributes)

        assertEquals(0x0009, methods[1].accessFlags)
        assertEquals(ConstantPoolIndex(5), methods[1].nameIndex)
        assertEquals(ConstantPoolIndex(6), methods[1].descriptorIndex)
        assertEquals(ConstantPoolIndex(7), methods[1].attributes.single().nameIndex)
        assertContentEquals(byteArrayOf(20, 21), methods[1].attributes.single().info)
        assertEquals(26, reader.position)
    }

    @Test
    fun `rejects zero method descriptor index`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 2, 0, 0, 0, 0),
                    source = "bad-method.class",
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0].descriptor_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }
}
