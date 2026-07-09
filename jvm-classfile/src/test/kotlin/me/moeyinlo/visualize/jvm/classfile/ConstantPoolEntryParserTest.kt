package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConstantPoolEntryParserTest {
    @Test
    fun `parses CONSTANT_Utf8 with modified UTF-8 characters`() {
        val bytes = byteArrayOf(
            1,
            0,
            10,
            'H'.code.toByte(),
            'i'.code.toByte(),
            0xC0.toByte(),
            0x80.toByte(),
            0xC2.toByte(),
            0x80.toByte(),
            0xE2.toByte(),
            0x82.toByte(),
            0xAC.toByte(),
            '!'.code.toByte(),
        )
        val reader = ClassFileByteReader(bytes, source = "utf8.class")

        val entry = ConstantPoolEntryParser.parseEntry(reader)
        val utf8 = assertIs<ConstantUtf8Entry>(entry)

        assertEquals("Hi\u0000\u0080€!", utf8.value)
        assertContentEquals(bytes.copyOfRange(3, bytes.size), utf8.encodedBytes)
        assertEquals(bytes.size, reader.position)
    }

    @Test
    fun `rejects zero bytes in CONSTANT_Utf8 payload`() {
        val reader = ClassFileByteReader(byteArrayOf(1, 0, 1, 0), source = "zero.class")

        val failure = assertFailsWith<ClassFileFormatException> {
            ConstantPoolEntryParser.parseEntry(reader)
        }

        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
        assertTrue(failure.message.orEmpty().contains("offset=3"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero byte"), failure.message)
    }

    @Test
    fun `rejects invalid modified UTF-8 continuation bytes`() {
        val reader = ClassFileByteReader(
            byteArrayOf(1, 0, 2, 0xC2.toByte(), 'A'.code.toByte()),
            source = "invalid-continuation.class",
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            ConstantPoolEntryParser.parseEntry(reader)
        }

        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
        assertTrue(failure.message.orEmpty().contains("offset=4"), failure.message)
        assertTrue(failure.message.orEmpty().contains("continuation"), failure.message)
    }

    @Test
    fun `rejects unsupported constant pool tags with entry offset`() {
        val reader = ClassFileByteReader(byteArrayOf(99), source = "unsupported.class")

        val failure = assertFailsWith<ClassFileFormatException> {
            ConstantPoolEntryParser.parseEntry(reader)
        }

        assertTrue(failure.message.orEmpty().contains("tag=99"), failure.message)
        assertTrue(failure.message.orEmpty().contains("offset=0"), failure.message)
    }
}
