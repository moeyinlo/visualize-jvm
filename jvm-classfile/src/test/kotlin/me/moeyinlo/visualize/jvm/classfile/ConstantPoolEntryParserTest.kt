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

    @Test
    fun `parses class and string constants as constant pool indexes`() {
        val classEntry = assertIs<ConstantClassEntry>(
            parse(byteArrayOf(7, 0, 9)),
        )
        val stringEntry = assertIs<ConstantStringEntry>(
            parse(byteArrayOf(8, 0, 10)),
        )

        assertEquals(ConstantPoolIndex(9), classEntry.nameIndex)
        assertEquals(ConstantPoolIndex(10), stringEntry.stringIndex)
    }

    @Test
    fun `parses name and type constants as name and descriptor indexes`() {
        val entry = assertIs<ConstantNameAndTypeEntry>(
            parse(byteArrayOf(12, 0, 3, 0, 4)),
        )

        assertEquals(ConstantPoolIndex(3), entry.nameIndex)
        assertEquals(ConstantPoolIndex(4), entry.descriptorIndex)
    }

    @Test
    fun `parses field method and interface method references`() {
        val field = assertIs<ConstantFieldRefEntry>(
            parse(byteArrayOf(9, 0, 2, 0, 3)),
        )
        val method = assertIs<ConstantMethodRefEntry>(
            parse(byteArrayOf(10, 0, 4, 0, 5)),
        )
        val interfaceMethod = assertIs<ConstantInterfaceMethodRefEntry>(
            parse(byteArrayOf(11, 0, 6, 0, 7)),
        )

        assertEquals(ConstantPoolIndex(2), field.classIndex)
        assertEquals(ConstantPoolIndex(3), field.nameAndTypeIndex)
        assertEquals(ConstantPoolIndex(4), method.classIndex)
        assertEquals(ConstantPoolIndex(5), method.nameAndTypeIndex)
        assertEquals(ConstantPoolIndex(6), interfaceMethod.classIndex)
        assertEquals(ConstantPoolIndex(7), interfaceMethod.nameAndTypeIndex)
    }

    @Test
    fun `parses method handle constants with valid reference kinds`() {
        val entry = assertIs<ConstantMethodHandleEntry>(
            parse(byteArrayOf(15, 6, 0, 12)),
        )

        assertEquals(MethodHandleReferenceKind.InvokeStatic, entry.referenceKind)
        assertEquals(ConstantPoolIndex(12), entry.referenceIndex)
    }

    @Test
    fun `rejects method handle constants with invalid reference kinds`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parse(byteArrayOf(15, 10, 0, 12))
        }

        assertTrue(failure.message.orEmpty().contains("reference_kind=10"), failure.message)
        assertTrue(failure.message.orEmpty().contains("offset=1"), failure.message)
        assertTrue(failure.message.orEmpty().contains("1..9"), failure.message)
    }

    @Test
    fun `parses method type constants as descriptor indexes`() {
        val entry = assertIs<ConstantMethodTypeEntry>(
            parse(byteArrayOf(16, 0, 13)),
        )

        assertEquals(ConstantPoolIndex(13), entry.descriptorIndex)
    }

    private fun parse(bytes: ByteArray): ConstantPoolEntry =
        ConstantPoolEntryParser.parseEntry(ClassFileByteReader(bytes, source = "numeric.class"))
}
