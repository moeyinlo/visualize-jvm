package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClassFileByteReaderTest {
    @Test
    fun `reads unsigned values in big endian order and advances position`() {
        val reader = ClassFileByteReader(
            bytes = byteArrayOf(
                0x12,
                0x34,
                0x56,
                0x78,
                0x9A.toByte(),
                0xBC.toByte(),
                0xDE.toByte(),
            ),
            source = "sample/Hello.class",
        )

        assertEquals(0, reader.position)
        assertEquals(0x12, reader.readU1())
        assertEquals(1, reader.position)
        assertEquals(0x3456, reader.readU2())
        assertEquals(3, reader.position)
        assertEquals(0x789ABCDE, reader.readU4())
        assertEquals(7, reader.position)
        assertEquals(0, reader.remaining)
    }

    @Test
    fun `reads byte slices as copies and advances position`() {
        val input = byteArrayOf(1, 2, 3, 4, 5)
        val reader = ClassFileByteReader(input, source = "slice.class")

        input[1] = 99
        val slice = reader.readSlice(3)

        assertContentEquals(byteArrayOf(1, 2, 3), slice)
        assertEquals(3, reader.position)
        assertEquals(2, reader.remaining)
        assertEquals(4, reader.readU1())
    }

    @Test
    fun `reports EOF diagnostics without advancing position`() {
        val reader = ClassFileByteReader(byteArrayOf(1), source = "truncated.class")

        val failure = assertFailsWith<ClassFileReadException> {
            reader.readU2()
        }

        assertEquals(0, reader.position)
        assertTrue(failure.message.orEmpty().contains("truncated.class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("offset=0"), failure.message)
        assertTrue(failure.message.orEmpty().contains("need=2"), failure.message)
        assertTrue(failure.message.orEmpty().contains("remaining=1"), failure.message)
    }

    @Test
    fun `reports EOF diagnostics with absolute base offset`() {
        val reader = ClassFileByteReader(byteArrayOf(1), source = "nested.class", baseOffset = 27)

        val failure = assertFailsWith<ClassFileReadException> {
            reader.readU2()
        }

        assertEquals(0, reader.position)
        assertEquals(27, reader.currentOffset)
        assertTrue(failure.message.orEmpty().contains("nested.class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("offset=27"), failure.message)
    }
}
