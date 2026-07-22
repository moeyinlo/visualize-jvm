package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClassFileParserTest {
    @Test
    fun `parses complete ClassFile structure`() {
        val classFile = ClassFileParser.parse(
            bytes = minimalClassFileBytes(),
            source = "Minimal.class",
            attributeParsers = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
        )

        assertEquals(ClassFileHeaderParser.ExpectedMagic, classFile.magic.value)
        assertEquals(70, classFile.version.major)
        assertEquals(7, classFile.constantPool.constantPoolCount)
        assertEquals(0x0021, classFile.accessFlags.raw)
        assertEquals(ClassFileKind.Class, classFile.accessFlags.kind)
        assertEquals(ConstantPoolIndex(2), classFile.identity.thisClassIndex)
        assertEquals(ConstantPoolIndex(4), classFile.identity.superClassIndex)
        assertEquals(emptyList(), classFile.identity.interfaceIndexes)
        assertEquals(emptyList(), classFile.fields)
        assertEquals(emptyList(), classFile.methods)
        assertEquals(ConstantPoolIndex(6), assertIs<SourceFileAttribute>(classFile.attributes.single()).sourceFileIndex)
    }

    @Test
    fun `rejects trailing bytes after ClassFile`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = minimalClassFileBytes() + 0,
                source = "Trailing.class",
                attributeParsers = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Trailing bytes"), failure.message)
        assertTrue(failure.message.orEmpty().contains("remaining=1"), failure.message)
    }

    @Test
    fun `rejects ClassFile with both NestHost and NestMembers attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithNestHostAndNestMembersBytes(),
                source = "ConflictingNest.class",
                attributeParsers = AttributeParserRegistry.of(
                    "NestHost" to NestHostAttributeParser,
                    "NestMembers" to NestMembersAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("NestHost"), failure.message)
        assertTrue(failure.message.orEmpty().contains("NestMembers"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not contain both"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate NestHost attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateNestHostBytes(),
                source = "DuplicateNestHost.class",
                attributeParsers = AttributeParserRegistry.of("NestHost" to NestHostAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("NestHost"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate NestMembers attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateNestMembersBytes(),
                source = "DuplicateNestMembers.class",
                attributeParsers = AttributeParserRegistry.of("NestMembers" to NestMembersAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("NestMembers"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
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

    private fun classFileWithNestHostAndNestMembersBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 9,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 8,
            'N'.code, 'e'.code, 's'.code, 't'.code, 'H'.code, 'o'.code, 's'.code, 't'.code,
            1, 0, 11,
            'N'.code, 'e'.code, 's'.code, 't'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            's'.code,
            1, 0, 10,
            'p'.code, 'k'.code, 'g'.code, '/'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            7, 0, 9,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 2,
            0, 6,
            0, 0, 0, 4,
            0, 1,
            0, 8,
        )

    private fun classFileWithDuplicateNestHostBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 6,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 8,
            'N'.code, 'e'.code, 's'.code, 't'.code, 'H'.code, 'o'.code, 's'.code, 't'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 2,
        )

    private fun classFileWithDuplicateNestMembersBytes(): ByteArray =
        bytes(
            0xCA, 0xFE, 0xBA, 0xBE,
            0, 0,
            0, 70,
            0, 8,
            1, 0, 4, 'T'.code, 'e'.code, 's'.code, 't'.code,
            7, 0, 1,
            1, 0, 16,
            'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code,
            'l'.code, 'a'.code, 'n'.code, 'g'.code, '/'.code,
            'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code,
            7, 0, 3,
            1, 0, 11,
            'N'.code, 'e'.code, 's'.code, 't'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            's'.code,
            1, 0, 10,
            'p'.code, 'k'.code, 'g'.code, '/'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            7, 0, 6,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 4,
            0, 1,
            0, 7,
            0, 5,
            0, 0, 0, 4,
            0, 1,
            0, 7,
        )

    private fun bytes(vararg values: Int): ByteArray =
        values.map { it.toByte() }.toByteArray()
}
