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
    fun `rejects ClassFile with duplicate SourceFile attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateSourceFileBytes(),
                source = "DuplicateSourceFile.class",
                attributeParsers = AttributeParserRegistry.of("SourceFile" to SourceFileAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("SourceFile"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate SourceDebugExtension attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateSourceDebugExtensionBytes(),
                source = "DuplicateSourceDebugExtension.class",
                attributeParsers = AttributeParserRegistry.of(
                    "SourceDebugExtension" to SourceDebugExtensionAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("SourceDebugExtension"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate EnclosingMethod attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateEnclosingMethodBytes(),
                source = "DuplicateEnclosingMethod.class",
                attributeParsers = AttributeParserRegistry.of("EnclosingMethod" to EnclosingMethodAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("EnclosingMethod"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate InnerClasses attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateInnerClassesBytes(),
                source = "DuplicateInnerClasses.class",
                attributeParsers = AttributeParserRegistry.of("InnerClasses" to InnerClassesAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("InnerClasses"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate Record attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateRecordBytes(),
                source = "DuplicateRecord.class",
                attributeParsers = AttributeParserRegistry.of("Record" to RecordAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Record"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate ModuleMainClass attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateModuleMainClassBytes(),
                source = "DuplicateModuleMainClass.class",
                attributeParsers = AttributeParserRegistry.of(
                    "ModuleMainClass" to ModuleMainClassAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ModuleMainClass"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate ModulePackages attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateModulePackagesBytes(),
                source = "DuplicateModulePackages.class",
                attributeParsers = AttributeParserRegistry.of("ModulePackages" to ModulePackagesAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ModulePackages"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate Module attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateModuleBytes(),
                source = "DuplicateModule.class",
                attributeParsers = AttributeParserRegistry.of("Module" to ModuleAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Module"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate Signature attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateSignatureBytes(),
                source = "DuplicateSignature.class",
                attributeParsers = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
            )
        }

        assertTrue(failure.message.orEmpty().contains("Signature"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ClassFile with duplicate RuntimeVisibleAnnotations attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicateRuntimeVisibleAnnotationsBytes(),
                source = "DuplicateRuntimeVisibleAnnotations.class",
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
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

    @Test
    fun `rejects ClassFile with duplicate PermittedSubclasses attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = classFileWithDuplicatePermittedSubclassesBytes(),
                source = "DuplicatePermittedSubclasses.class",
                attributeParsers = AttributeParserRegistry.of(
                    "PermittedSubclasses" to PermittedSubclassesAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("PermittedSubclasses"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects final ClassFile with PermittedSubclasses attribute`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ClassFileParser.parse(
                bytes = finalClassFileWithPermittedSubclassesBytes(),
                source = "FinalPermittedSubclasses.class",
                attributeParsers = AttributeParserRegistry.of(
                    "PermittedSubclasses" to PermittedSubclassesAttributeParser,
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_FINAL"), failure.message)
        assertTrue(failure.message.orEmpty().contains("PermittedSubclasses"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not declare"), failure.message)
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

    private fun classFileWithDuplicateSourceDebugExtensionBytes(): ByteArray =
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
            1, 0, 20,
            'S'.code, 'o'.code, 'u'.code, 'r'.code, 'c'.code, 'e'.code, 'D'.code, 'e'.code, 'b'.code, 'u'.code,
            'g'.code, 'E'.code, 'x'.code, 't'.code, 'e'.code, 'n'.code, 's'.code, 'i'.code, 'o'.code, 'n'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 1,
            'A'.code,
            0, 5,
            0, 0, 0, 1,
            'B'.code,
        )

    private fun classFileWithDuplicateEnclosingMethodBytes(): ByteArray =
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
            1, 0, 15,
            'E'.code, 'n'.code, 'c'.code, 'l'.code, 'o'.code, 's'.code, 'i'.code, 'n'.code, 'g'.code,
            'M'.code, 'e'.code, 't'.code, 'h'.code, 'o'.code, 'd'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 4,
            0, 4,
            0, 0,
            0, 5,
            0, 0, 0, 4,
            0, 4,
            0, 0,
        )

    private fun classFileWithDuplicateInnerClassesBytes(): ByteArray =
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
            1, 0, 12,
            'I'.code, 'n'.code, 'n'.code, 'e'.code, 'r'.code, 'C'.code, 'l'.code, 'a'.code, 's'.code, 's'.code,
            'e'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateRecordBytes(): ByteArray =
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
            1, 0, 6, 'R'.code, 'e'.code, 'c'.code, 'o'.code, 'r'.code, 'd'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateModuleMainClassBytes(): ByteArray =
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
            1, 0, 15,
            'M'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code, 'M'.code, 'a'.code, 'i'.code, 'n'.code,
            'C'.code, 'l'.code, 'a'.code, 's'.code, 's'.code,
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

    private fun classFileWithDuplicateModulePackagesBytes(): ByteArray =
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
            1, 0, 14,
            'M'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code, 'P'.code, 'a'.code, 'c'.code, 'k'.code,
            'a'.code, 'g'.code, 'e'.code, 's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
        )

    private fun classFileWithDuplicateModuleBytes(): ByteArray =
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
            1, 0, 6, 'M'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code, 'e'.code,
            1, 0, 11,
            't'.code, 'e'.code, 's'.code, 't'.code, '.'.code, 'm'.code, 'o'.code, 'd'.code, 'u'.code, 'l'.code,
            'e'.code,
            19, 0, 6,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 16,
            0, 7,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 5,
            0, 0, 0, 16,
            0, 7,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        )

    private fun classFileWithDuplicateSignatureBytes(): ByteArray =
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
            1, 0, 9, 'S'.code, 'i'.code, 'g'.code, 'n'.code, 'a'.code, 't'.code, 'u'.code, 'r'.code, 'e'.code,
            1, 0, 18,
            'L'.code, 'j'.code, 'a'.code, 'v'.code, 'a'.code, '/'.code, 'l'.code, 'a'.code, 'n'.code, 'g'.code,
            '/'.code, 'O'.code, 'b'.code, 'j'.code, 'e'.code, 'c'.code, 't'.code, ';'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 6,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )

    private fun classFileWithDuplicateSourceFileBytes(): ByteArray =
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
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 6,
            0, 5,
            0, 0, 0, 2,
            0, 6,
        )

    private fun classFileWithDuplicateRuntimeVisibleAnnotationsBytes(): ByteArray =
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
            1, 0, 25,
            'R'.code, 'u'.code, 'n'.code, 't'.code, 'i'.code, 'm'.code, 'e'.code,
            'V'.code, 'i'.code, 's'.code, 'i'.code, 'b'.code, 'l'.code, 'e'.code,
            'A'.code, 'n'.code, 'n'.code, 'o'.code, 't'.code, 'a'.code, 't'.code, 'i'.code, 'o'.code, 'n'.code,
            's'.code,
            0, 0x21,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 2,
            0, 5,
            0, 0, 0, 2,
            0, 0,
            0, 5,
            0, 0, 0, 2,
            0, 0,
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

    private fun classFileWithDuplicatePermittedSubclassesBytes(): ByteArray =
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
            1, 0, 19,
            'P'.code, 'e'.code, 'r'.code, 'm'.code, 'i'.code, 't'.code, 't'.code, 'e'.code, 'd'.code,
            'S'.code, 'u'.code, 'b'.code, 'c'.code, 'l'.code, 'a'.code, 's'.code, 's'.code, 'e'.code, 's'.code,
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

    private fun finalClassFileWithPermittedSubclassesBytes(): ByteArray =
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
            1, 0, 19,
            'P'.code, 'e'.code, 'r'.code, 'm'.code, 'i'.code, 't'.code, 't'.code, 'e'.code, 'd'.code,
            'S'.code, 'u'.code, 'b'.code, 'c'.code, 'l'.code, 'a'.code, 's'.code, 's'.code, 'e'.code, 's'.code,
            1, 0, 10,
            'p'.code, 'k'.code, 'g'.code, '/'.code, 'M'.code, 'e'.code, 'm'.code, 'b'.code, 'e'.code, 'r'.code,
            7, 0, 6,
            0, 0x31,
            0, 2,
            0, 4,
            0, 0,
            0, 0,
            0, 0,
            0, 1,
            0, 5,
            0, 0, 0, 4,
            0, 1,
            0, 7,
        )

    private fun bytes(vararg values: Int): ByteArray =
        values.map { it.toByte() }.toByteArray()
}
