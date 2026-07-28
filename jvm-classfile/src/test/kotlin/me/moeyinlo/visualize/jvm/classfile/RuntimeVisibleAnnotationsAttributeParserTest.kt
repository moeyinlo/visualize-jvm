package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RuntimeVisibleAnnotationsAttributeParserTest {
    @Test
    fun `parses RuntimeVisibleAnnotations with every element value variant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()), // #1
                ConstantUtf8Entry("Lpkg/Example;", byteArrayOf()), // #2
                ConstantUtf8Entry("intValue", byteArrayOf()), // #3
                ConstantIntegerEntry(7), // #4
                ConstantUtf8Entry("stringValue", byteArrayOf()), // #5
                ConstantUtf8Entry("hello", byteArrayOf()), // #6
                ConstantUtf8Entry("enumValue", byteArrayOf()), // #7
                ConstantUtf8Entry("Lpkg/Mode;", byteArrayOf()), // #8
                ConstantUtf8Entry("FAST", byteArrayOf()), // #9
                ConstantUtf8Entry("classValue", byteArrayOf()), // #10
                ConstantUtf8Entry("Ljava/lang/String;", byteArrayOf()), // #11
                ConstantUtf8Entry("nestedValue", byteArrayOf()), // #12
                ConstantUtf8Entry("Lpkg/Nested;", byteArrayOf()), // #13
                ConstantUtf8Entry("arrayValue", byteArrayOf()), // #14
                ConstantLongEntry(9L), // #15
                ConstantDoubleEntry(1.5), // #17
                ConstantFloatEntry(2.5f), // #19
                ConstantUtf8Entry("boolValue", byteArrayOf()), // #20
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(
                    0,
                    1,
                    0,
                    1,
                    0,
                    0,
                    0,
                    54,
                    0,
                    1,
                    0,
                    2,
                    0,
                    7,
                    0,
                    3,
                    'I'.code.toByte(),
                    0,
                    4,
                    0,
                    5,
                    's'.code.toByte(),
                    0,
                    6,
                    0,
                    7,
                    'e'.code.toByte(),
                    0,
                    8,
                    0,
                    9,
                    0,
                    10,
                    'c'.code.toByte(),
                    0,
                    11,
                    0,
                    12,
                    '@'.code.toByte(),
                    0,
                    13,
                    0,
                    0,
                    0,
                    14,
                    '['.code.toByte(),
                    0,
                    3,
                    'J'.code.toByte(),
                    0,
                    15,
                    'D'.code.toByte(),
                    0,
                    17,
                    'F'.code.toByte(),
                    0,
                    19,
                    0,
                    20,
                    'Z'.code.toByte(),
                    0,
                    4,
                ),
                source = "runtime-visible-annotations.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<RuntimeVisibleAnnotationsAttribute>(attributes.single())
        val annotation = attribute.annotations.single()
        assertEquals(ConstantPoolIndex(2), annotation.typeIndex)
        assertEquals(7, annotation.elementValuePairs.size)

        assertEquals(
            ConstantPoolIndex(4),
            assertIs<ElementValue.Const>(annotation.elementValuePairs[0].value).constValueIndex,
        )
        assertEquals(
            ConstantPoolIndex(6),
            assertIs<ElementValue.Const>(annotation.elementValuePairs[1].value).constValueIndex,
        )
        val enumValue = assertIs<ElementValue.EnumConst>(annotation.elementValuePairs[2].value)
        assertEquals(ConstantPoolIndex(8), enumValue.typeNameIndex)
        assertEquals(ConstantPoolIndex(9), enumValue.constNameIndex)
        assertEquals(
            ConstantPoolIndex(11),
            assertIs<ElementValue.ClassInfo>(annotation.elementValuePairs[3].value).classInfoIndex,
        )
        assertEquals(
            ConstantPoolIndex(13),
            assertIs<ElementValue.NestedAnnotation>(annotation.elementValuePairs[4].value).annotation.typeIndex,
        )
        val arrayValue = assertIs<ElementValue.ArrayValue>(annotation.elementValuePairs[5].value)
        assertEquals(3, arrayValue.values.size)
        assertEquals(ConstantPoolIndex(15), assertIs<ElementValue.Const>(arrayValue.values[0]).constValueIndex)
        assertEquals(ConstantPoolIndex(17), assertIs<ElementValue.Const>(arrayValue.values[1]).constValueIndex)
        assertEquals(ConstantPoolIndex(19), assertIs<ElementValue.Const>(arrayValue.values[2]).constValueIndex)
        assertEquals('Z', assertIs<ElementValue.Const>(annotation.elementValuePairs[6].value).tag)
    }

    @Test
    fun `rejects RuntimeVisibleAnnotations attributes before Java 5`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 0),
                    source = "java4-runtime-visible-annotations.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
                majorVersion = 48,
            )
        }

        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version=48"), failure.message)
        assertTrue(failure.message.orEmpty().contains("49"), failure.message)
    }

    @Test
    fun `rejects annotation type index that is not UTF-8`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 6, 0, 1, 0, 2, 0, 0),
                    source = "bad-runtime-visible-annotations.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("type_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }

    @Test
    fun `rejects annotation type index with invalid field descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
                ConstantUtf8Entry("not-a-descriptor", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 6, 0, 1, 0, 2, 0, 0),
                    source = "bad-runtime-visible-annotation-type.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("type_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field descriptor"), failure.message)
    }

    @Test
    fun `rejects annotation element name index with invalid simple name`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Example;", byteArrayOf()),
                ConstantUtf8Entry("bad/name", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        11,
                        0,
                        1,
                        0,
                        2,
                        0,
                        1,
                        0,
                        3,
                        'I'.code.toByte(),
                        0,
                        4,
                    ),
                    source = "bad-annotation-element-name.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("element_name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unqualified name"), failure.message)
    }

    @Test
    fun `rejects const element value with wrong constant type`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Example;", byteArrayOf()),
                ConstantUtf8Entry("intValue", byteArrayOf()),
                ConstantUtf8Entry("not-an-int", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        11,
                        0,
                        1,
                        0,
                        2,
                        0,
                        1,
                        0,
                        3,
                        'I'.code.toByte(),
                        0,
                        4,
                    ),
                    source = "bad-annotation-element.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("const_value_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Integer"), failure.message)
    }

    @Test
    fun `rejects enum element value with invalid type descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Example;", byteArrayOf()),
                ConstantUtf8Entry("enumValue", byteArrayOf()),
                ConstantUtf8Entry("not-a-descriptor", byteArrayOf()),
                ConstantUtf8Entry("FAST", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        13,
                        0,
                        1,
                        0,
                        2,
                        0,
                        1,
                        0,
                        3,
                        'e'.code.toByte(),
                        0,
                        4,
                        0,
                        5,
                    ),
                    source = "bad-enum-annotation-element.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("enum_const_value.type_name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field descriptor"), failure.message)
    }

    @Test
    fun `rejects enum element value with invalid const simple name`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Example;", byteArrayOf()),
                ConstantUtf8Entry("enumValue", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Mode;", byteArrayOf()),
                ConstantUtf8Entry("BAD/NAME", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        13,
                        0,
                        1,
                        0,
                        2,
                        0,
                        1,
                        0,
                        3,
                        'e'.code.toByte(),
                        0,
                        4,
                        0,
                        5,
                    ),
                    source = "bad-enum-const-name-annotation-element.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("enum_const_value.const_name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unqualified name"), failure.message)
    }

    @Test
    fun `rejects class element value with invalid return descriptor`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
                ConstantUtf8Entry("Lpkg/Example;", byteArrayOf()),
                ConstantUtf8Entry("classValue", byteArrayOf()),
                ConstantUtf8Entry("not-a-descriptor", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0,
                        1,
                        0,
                        1,
                        0,
                        0,
                        0,
                        11,
                        0,
                        1,
                        0,
                        2,
                        0,
                        1,
                        0,
                        3,
                        'c'.code.toByte(),
                        0,
                        4,
                    ),
                    source = "bad-class-annotation-element.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("class_info_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("return descriptor"), failure.message)
    }
}
