package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClassFileHeaderParserTest {
    @Test
    fun `parses classfile magic and advances reader`() {
        val reader = ClassFileByteReader(
            byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte(), 0),
            source = "valid.class",
        )

        val magic = ClassFileHeaderParser.parseMagic(reader)

        assertEquals(0, magic.offset)
        assertEquals(0xCAFEBABEL, magic.value)
        assertEquals(4, reader.position)
        assertEquals(1, reader.remaining)
    }

    @Test
    fun `rejects invalid magic with byte offset and actual value`() {
        val reader = ClassFileByteReader(
            byteArrayOf(0, 0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBF.toByte()),
            source = "invalid.class",
        )
        reader.readU1()

        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileHeaderParser.parseMagic(reader)
        }

        assertTrue(failure.message.orEmpty().contains("invalid.class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("offset=1"), failure.message)
        assertTrue(failure.message.orEmpty().contains("expected=0xCAFEBABE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("actual=0xCAFEBABF"), failure.message)
    }
}
