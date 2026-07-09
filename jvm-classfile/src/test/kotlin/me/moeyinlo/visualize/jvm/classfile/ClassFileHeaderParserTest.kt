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

    @Test
    fun `parses minor and major version after magic`() {
        val reader = ClassFileByteReader(
            byteArrayOf(
                0xCA.toByte(),
                0xFE.toByte(),
                0xBA.toByte(),
                0xBE.toByte(),
                0,
                1,
                0,
                70,
            ),
            source = "version.class",
        )

        ClassFileHeaderParser.parseMagic(reader)
        val version = ClassFileHeaderParser.parseVersion(reader)

        assertEquals(4, version.offset)
        assertEquals(1, version.minor)
        assertEquals(70, version.major)
        assertEquals(8, reader.position)
    }

    @Test
    fun `accepts Java SE 26 supported classfile versions`() {
        val supportedVersions = listOf(
            ClassFileVersion(offset = 0, minor = 0, major = 45),
            ClassFileVersion(offset = 0, minor = 65535, major = 45),
            ClassFileVersion(offset = 0, minor = 123, major = 55),
            ClassFileVersion(offset = 0, minor = 0, major = 56),
            ClassFileVersion(offset = 0, minor = 0, major = 70),
            ClassFileVersion(offset = 0, minor = 65535, major = 70),
        )

        supportedVersions.forEach { version ->
            assertEquals(version, ClassFileHeaderParser.validateJava26Version(version))
        }
    }

    @Test
    fun `rejects major versions outside Java SE 26 range`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileHeaderParser.validateJava26Version(ClassFileVersion(offset = 4, minor = 0, major = 71))
        }

        assertTrue(failure.message.orEmpty().contains("offset=4"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major=71"), failure.message)
        assertTrue(failure.message.orEmpty().contains("supported=45..70"), failure.message)
    }

    @Test
    fun `rejects non preview minor versions for major 56 and above`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileHeaderParser.validateJava26Version(ClassFileVersion(offset = 4, minor = 1, major = 56))
        }

        assertTrue(failure.message.orEmpty().contains("major=56"), failure.message)
        assertTrue(failure.message.orEmpty().contains("minor=1"), failure.message)
        assertTrue(failure.message.orEmpty().contains("minor must be 0 or 65535"), failure.message)
    }

    @Test
    fun `rejects older preview versions for Java SE 26`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileHeaderParser.validateJava26Version(ClassFileVersion(offset = 4, minor = 65535, major = 69))
        }

        assertTrue(failure.message.orEmpty().contains("major=69"), failure.message)
        assertTrue(failure.message.orEmpty().contains("minor=65535"), failure.message)
        assertTrue(failure.message.orEmpty().contains("older preview"), failure.message)
    }
}
