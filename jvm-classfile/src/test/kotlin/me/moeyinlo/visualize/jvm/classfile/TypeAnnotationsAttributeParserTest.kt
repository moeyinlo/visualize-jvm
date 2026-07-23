package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TypeAnnotationsAttributeParserTest {
    @Test
    fun `parses RuntimeVisibleTypeAnnotations target info variants`() {
        val constantPool = typeAnnotationConstantPool("RuntimeVisibleTypeAnnotations")
        val annotationsInfo = bytes(
            0x00,
            22,
            *typeAnnotation(0x00, 0),
            *typeAnnotation(0x01, 1),
            *typeAnnotation(0x10, 0, 5),
            *typeAnnotation(0x11, 1, 2),
            *typeAnnotation(0x12, 3, 4),
            *typeAnnotation(0x13),
            *typeAnnotation(0x14),
            *typeAnnotation(0x15),
            *typeAnnotation(0x16, 6),
            *typeAnnotation(0x17, 0, 7),
            *typeAnnotation(0x40, 0, 0),
            *typeAnnotation(0x41, 0, 0),
            *typeAnnotation(0x42, 0, 8),
            *typeAnnotation(0x43, 0, 9),
            *typeAnnotation(0x44, 0, 10),
            *typeAnnotation(0x45, 0, 11),
            *typeAnnotation(0x46, 0, 12),
            *typeAnnotation(0x47, 0, 13, 1),
            *typeAnnotation(0x48, 0, 14, 2),
            *typeAnnotation(0x49, 0, 15, 3),
            *typeAnnotation(0x4A, 0, 16, 4),
            *typeAnnotation(0x4B, 0, 17, 5),
        )

        val attribute = parseTypeAttribute(
            attributeName = "RuntimeVisibleTypeAnnotations",
            parser = RuntimeVisibleTypeAnnotationsAttributeParser,
            constantPool = constantPool,
            info = annotationsInfo,
        )

        val visible = assertIs<RuntimeVisibleTypeAnnotationsAttribute>(attribute)
        assertEquals(22, visible.annotations.size)
        assertIs<TypeAnnotationTargetInfo.TypeParameterTarget>(visible.annotations[0].targetInfo)
        assertIs<TypeAnnotationTargetInfo.TypeParameterTarget>(visible.annotations[1].targetInfo)
        assertIs<TypeAnnotationTargetInfo.SupertypeTarget>(visible.annotations[2].targetInfo)
        assertIs<TypeAnnotationTargetInfo.TypeParameterBoundTarget>(visible.annotations[3].targetInfo)
        assertIs<TypeAnnotationTargetInfo.TypeParameterBoundTarget>(visible.annotations[4].targetInfo)
        assertIs<TypeAnnotationTargetInfo.EmptyTarget>(visible.annotations[5].targetInfo)
        assertIs<TypeAnnotationTargetInfo.EmptyTarget>(visible.annotations[6].targetInfo)
        assertIs<TypeAnnotationTargetInfo.EmptyTarget>(visible.annotations[7].targetInfo)
        assertIs<TypeAnnotationTargetInfo.FormalParameterTarget>(visible.annotations[8].targetInfo)
        assertIs<TypeAnnotationTargetInfo.ThrowsTarget>(visible.annotations[9].targetInfo)
        assertIs<TypeAnnotationTargetInfo.LocalVarTarget>(visible.annotations[10].targetInfo)
        assertIs<TypeAnnotationTargetInfo.LocalVarTarget>(visible.annotations[11].targetInfo)
        assertIs<TypeAnnotationTargetInfo.CatchTarget>(visible.annotations[12].targetInfo)
        assertIs<TypeAnnotationTargetInfo.OffsetTarget>(visible.annotations[13].targetInfo)
        assertIs<TypeAnnotationTargetInfo.OffsetTarget>(visible.annotations[16].targetInfo)
        assertIs<TypeAnnotationTargetInfo.TypeArgumentTarget>(visible.annotations[17].targetInfo)
        assertEquals(5, assertIs<TypeAnnotationTargetInfo.TypeArgumentTarget>(visible.annotations[21].targetInfo).typeArgumentIndex)
    }

    @Test
    fun `parses type path and annotation element value`() {
        val constantPool = typeAnnotationConstantPool("RuntimeInvisibleTypeAnnotations")
        val info = bytes(
            0,
            1,
            0x13,
            2,
            0,
            0,
            3,
            2,
            0,
            2,
            0,
            1,
            0,
            3,
            'I'.code,
            0,
            4,
        )

        val attribute = parseTypeAttribute(
            attributeName = "RuntimeInvisibleTypeAnnotations",
            parser = RuntimeInvisibleTypeAnnotationsAttributeParser,
            constantPool = constantPool,
            info = info,
        )

        val invisible = assertIs<RuntimeInvisibleTypeAnnotationsAttribute>(attribute)
        val annotation = invisible.annotations.single()
        assertEquals(
            listOf(
                TypePathEntry(typePathKind = 0, typeArgumentIndex = 0),
                TypePathEntry(typePathKind = 3, typeArgumentIndex = 2),
            ),
            annotation.targetPath.entries,
        )
        assertEquals(ConstantPoolIndex(2), annotation.annotation.typeIndex)
        assertEquals(ConstantPoolIndex(4), assertIs<ElementValue.Const>(annotation.annotation.elementValuePairs.single().value).constValueIndex)
    }

    @Test
    fun `rejects invalid type annotation target type`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseTypeAttribute(
                attributeName = "RuntimeVisibleTypeAnnotations",
                parser = RuntimeVisibleTypeAnnotationsAttributeParser,
                constantPool = typeAnnotationConstantPool("RuntimeVisibleTypeAnnotations"),
                info = bytes(0, 1, 0x02),
            )
        }

        assertTrue(failure.message.orEmpty().contains("target_type"), failure.message)
        assertTrue(failure.message.orEmpty().contains("0x02"), failure.message)
    }

    @Test
    fun `rejects code target type in ClassFile type annotations`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseTypeAttribute(
                attributeName = "RuntimeVisibleTypeAnnotations",
                parser = RuntimeVisibleTypeAnnotationsAttributeParser,
                constantPool = typeAnnotationConstantPool("RuntimeVisibleTypeAnnotations"),
                info = bytes(0, 1, *typeAnnotation(0x40, 0, 0)),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("target_type"), failure.message)
        assertTrue(failure.message.orEmpty().contains("0x40"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects method target type in field type annotations`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseTypeAttribute(
                attributeName = "RuntimeVisibleTypeAnnotations",
                parser = RuntimeVisibleTypeAnnotationsAttributeParser,
                constantPool = typeAnnotationConstantPool("RuntimeVisibleTypeAnnotations"),
                info = bytes(0, 1, *typeAnnotation(0x14)),
                ownerPath = "fields[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("target_type"), failure.message)
        assertTrue(failure.message.orEmpty().contains("0x14"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field_info"), failure.message)
    }

    @Test
    fun `parses record component field target type annotations`() {
        val attribute = parseTypeAttribute(
            attributeName = "RuntimeVisibleTypeAnnotations",
            parser = RuntimeVisibleTypeAnnotationsAttributeParser,
            constantPool = typeAnnotationConstantPool("RuntimeVisibleTypeAnnotations"),
            info = bytes(0, 1, *typeAnnotation(0x13)),
            ownerPath = "ClassFile.attributes[0].components[0]",
        )

        val visible = assertIs<RuntimeVisibleTypeAnnotationsAttribute>(attribute)
        assertEquals(0x13, visible.annotations.single().targetType)
    }

    @Test
    fun `rejects method target type in Code type annotations`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseTypeAttribute(
                attributeName = "RuntimeVisibleTypeAnnotations",
                parser = RuntimeVisibleTypeAnnotationsAttributeParser,
                constantPool = typeAnnotationConstantPool("RuntimeVisibleTypeAnnotations"),
                info = bytes(0, 1, *typeAnnotation(0x14)),
                ownerPath = "methods[0].attributes[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("target_type"), failure.message)
        assertTrue(failure.message.orEmpty().contains("0x14"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Code"), failure.message)
    }

    @Test
    fun `rejects invalid type path entry`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseTypeAttribute(
                attributeName = "RuntimeVisibleTypeAnnotations",
                parser = RuntimeVisibleTypeAnnotationsAttributeParser,
                constantPool = typeAnnotationConstantPool("RuntimeVisibleTypeAnnotations"),
                info = bytes(0, 1, 0x13, 1, 0, 1, 0, 2, 0, 0),
            )
        }

        assertTrue(failure.message.orEmpty().contains("type_argument_index"), failure.message)
    }

    private fun parseTypeAttribute(
        attributeName: String,
        parser: AttributeBodyParser,
        constantPool: ConstantPool,
        info: ByteArray,
        ownerPath: String = "methods[0]",
    ): AttributeInfo =
        AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                bytes(0, 1, 0, 1, *u4(info.size), *info.map { it.toInt() and 0xFF }.toIntArray()),
                source = "$attributeName.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of(attributeName to parser),
            ownerPath = ownerPath,
        ).single()

    private fun typeAnnotationConstantPool(attributeName: String): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry(attributeName, byteArrayOf()),
                ConstantUtf8Entry("Lpkg/TypeUse;", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

    private fun typeAnnotation(
        targetType: Int,
        vararg targetInfo: Int,
    ): IntArray =
        intArrayOf(
            targetType,
            *targetInfo,
            0,
            0,
            2,
            0,
            0,
        )

    private fun u4(value: Int): IntArray =
        intArrayOf(
            (value ushr 24) and 0xFF,
            (value ushr 16) and 0xFF,
            (value ushr 8) and 0xFF,
            value and 0xFF,
        )

    private fun bytes(vararg values: Int): ByteArray =
        values.map { it.toByte() }.toByteArray()
}
