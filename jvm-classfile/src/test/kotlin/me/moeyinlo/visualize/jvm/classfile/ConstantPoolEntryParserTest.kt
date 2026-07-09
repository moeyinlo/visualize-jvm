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

    @Test
    fun `parses integer and float constants from raw bits`() {
        val integer = assertIs<ConstantIntegerEntry>(
            parse(byteArrayOf(3, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xD6.toByte())),
        )
        val float = assertIs<ConstantFloatEntry>(
            parse(byteArrayOf(4, 0x40, 0x60, 0x00, 0x00)),
        )

        assertEquals(-42, integer.value)
        assertEquals(3.5f, float.value)
    }

    @Test
    fun `parses long and double constants as two slot entries`() {
        val long = assertIs<ConstantLongEntry>(
            parse(
                byteArrayOf(
                    5,
                    0xFF.toByte(),
                    0xFF.toByte(),
                    0xFF.toByte(),
                    0xFF.toByte(),
                    0xFF.toByte(),
                    0xFF.toByte(),
                    0xFF.toByte(),
                    0xD6.toByte(),
                ),
            ),
        )
        val double = assertIs<ConstantDoubleEntry>(
            parse(
                byteArrayOf(
                    6,
                    0x3F,
                    0xF8.toByte(),
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                ),
            ),
        )

        assertEquals(-42L, long.value)
        assertTrue(long.occupiesTwoSlots)
        assertEquals(1.5, double.value)
        assertTrue(double.occupiesTwoSlots)
    }

    private fun parse(bytes: ByteArray): ConstantPoolEntry =
        ConstantPoolEntryParser.parseEntry(ClassFileByteReader(bytes, source = "numeric.class"))
}
