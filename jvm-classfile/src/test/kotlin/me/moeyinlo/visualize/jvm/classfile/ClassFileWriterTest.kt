package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClassFileWriterTest {
    @Test
    fun `writes classfile magic and version header in JVMS order`() {
        val bytes = ClassFileWriter.writeHeader(
            ClassFileVersion(offset = 4, minor = 0, major = ClassFileHeaderParser.Java26MajorVersion),
        )

        assertContentEquals(
            byteArrayOf(
                0xCA.toByte(),
                0xFE.toByte(),
                0xBA.toByte(),
                0xBE.toByte(),
                0,
                0,
                0,
                70,
            ),
            bytes,
        )

        val reader = ClassFileByteReader(bytes, source = "writer-header.class")
        assertEquals(ClassFileHeaderParser.ExpectedMagic, ClassFileHeaderParser.parseMagic(reader).value)
        val version = ClassFileHeaderParser.parseVersion(reader)
        assertEquals(0, version.minor)
        assertEquals(ClassFileHeaderParser.Java26MajorVersion, version.major)
    }

    @Test
    fun `rejects unsupported classfile versions before writing header`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileWriter.writeHeader(ClassFileVersion(offset = 4, minor = 0, major = 71))
        }

        assertTrue(failure.message.orEmpty().contains("major=71"), failure.message)
    }

    @Test
    fun `writes unsigned values in big endian order`() {
        val writer = ClassFileByteWriter()

        writer.writeU1(0x12)
        writer.writeU2(0x3456)
        writer.writeU4(0x789ABCDEL)

        assertEquals(7, writer.position)
        assertContentEquals(
            byteArrayOf(
                0x12,
                0x34,
                0x56,
                0x78,
                0x9A.toByte(),
                0xBC.toByte(),
                0xDE.toByte(),
            ),
            writer.toByteArray(),
        )
    }

    @Test
    fun `rejects unsigned values outside their width`() {
        assertFailsWith<IllegalArgumentException> { ClassFileByteWriter().writeU1(0x100) }
        assertFailsWith<IllegalArgumentException> { ClassFileByteWriter().writeU2(0x1_0000) }
        assertFailsWith<IllegalArgumentException> { ClassFileByteWriter().writeU4(0x1_0000_0000L) }
    }

    @Test
    fun `copies raw byte payloads defensively`() {
        val payload = byteArrayOf(1, 2, 3)
        val writer = ClassFileByteWriter()

        writer.writeBytes(payload)
        payload[0] = 99
        val output = writer.toByteArray()
        output[1] = 88

        assertContentEquals(byteArrayOf(1, 2, 3), writer.toByteArray())
    }

    @Test
    fun `writes complete ClassFile structure`() {
        val originalBytes = minimalClassFileBytes()
        val registry = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser)
        val classFile = ClassFileParser.parse(
            bytes = originalBytes,
            source = "Minimal.class",
            attributeParsers = registry,
        )

        val writtenBytes = ClassFileWriter.writeClassFile(classFile)

        assertContentEquals(originalBytes, writtenBytes)

        val parsedAgain = ClassFileParser.parse(
            bytes = writtenBytes,
            source = "Minimal-written.class",
            attributeParsers = registry,
        )
        assertEquals(ConstantPoolIndex(2), parsedAgain.identity.thisClassIndex)
        assertEquals(ConstantPoolIndex(4), parsedAgain.identity.superClassIndex)
        assertEquals(ConstantPoolIndex(6), assertIs<SourceFileAttribute>(parsedAgain.attributes.single()).sourceFileIndex)
    }

    private fun minimalClassFileBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 7,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 10,
            'S'.code, 'o'.code, 'u'.code, 'r'.code, 'c'.code, 'e'.code, 'F'.code, 'i'.code, 'l'.code, 'e'.code,
            1, 0, 9,
            'T'.code, 'e'.code, 's'.code, 't'.code, '.'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )

    private fun bytes(vararg values: Int): ByteArray =
        values.map { it.toByte() }.toByteArray()
}
