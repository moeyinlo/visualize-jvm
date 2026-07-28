package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecordAttributeParserTest {
    @Test
    fun `parses Record attribute components and nested attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("VendorRecordMetadata", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(
                    0, 1,
                    0, 1,
                    0, 0, 0, 16,
                    0, 1,
                    0, 2, 0, 3,
                    0, 1,
                    0, 4, 0, 0, 0, 2, 7, 8,
                ),
                source = "record.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
            ownerPath = "ClassFile",
        )

        val component = assertIs<RecordAttribute>(attributes.single()).components.single()
        assertEquals(ConstantPoolIndex(2), component.nameIndex)
        assertEquals(ConstantPoolIndex(3), component.descriptorIndex)
        val nested = assertIs<UnknownAttributeInfo>(component.attributes.single())
        assertEquals("VendorRecordMetadata", nested.name)
        assertContentEquals(byteArrayOf(7, 8), nested.info)
    }

    @Test
    fun `rejects component with duplicate Signature attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("Ljava/lang/Object;", byteArrayOf()),
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("Ljava/lang/String;", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 24,
                        0, 1,
                        0, 2, 0, 3,
                        0, 2,
                        0, 4, 0, 0, 0, 2, 0, 5,
                        0, 4, 0, 0, 0, 2, 0, 5,
                    ),
                    source = "bad-record-signature.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Record" to RecordAttributeParser,
                    "Signature" to SignatureAttributeParser,
                ),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Signature"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }


    @Test
    fun `rejects component with duplicate RuntimeVisibleAnnotations attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("RuntimeVisibleAnnotations", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 24,
                        0, 1,
                        0, 2, 0, 3,
                        0, 2,
                        0, 4, 0, 0, 0, 2, 0, 0,
                        0, 4, 0, 0, 0, 2, 0, 0,
                    ),
                    source = "bad-record-runtime-visible-annotations.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Record" to RecordAttributeParser,
                    "RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser,
                ),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects component with duplicate RuntimeInvisibleAnnotations attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("RuntimeInvisibleAnnotations", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 24,
                        0, 1,
                        0, 2, 0, 3,
                        0, 2,
                        0, 4, 0, 0, 0, 2, 0, 0,
                        0, 4, 0, 0, 0, 2, 0, 0,
                    ),
                    source = "bad-record-runtime-invisible-annotations.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Record" to RecordAttributeParser,
                    "RuntimeInvisibleAnnotations" to RuntimeInvisibleAnnotationsAttributeParser,
                ),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeInvisibleAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects component with duplicate RuntimeVisibleTypeAnnotations attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("RuntimeVisibleTypeAnnotations", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 24,
                        0, 1,
                        0, 2, 0, 3,
                        0, 2,
                        0, 4, 0, 0, 0, 2, 0, 0,
                        0, 4, 0, 0, 0, 2, 0, 0,
                    ),
                    source = "bad-record-runtime-visible-type-annotations.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Record" to RecordAttributeParser,
                    "RuntimeVisibleTypeAnnotations" to RuntimeVisibleTypeAnnotationsAttributeParser,
                ),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleTypeAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects component with duplicate RuntimeInvisibleTypeAnnotations attributes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("RuntimeInvisibleTypeAnnotations", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 24,
                        0, 1,
                        0, 2, 0, 3,
                        0, 2,
                        0, 4, 0, 0, 0, 2, 0, 0,
                        0, 4, 0, 0, 0, 2, 0, 0,
                    ),
                    source = "bad-record-runtime-invisible-type-annotations.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of(
                    "Record" to RecordAttributeParser,
                    "RuntimeInvisibleTypeAnnotations" to RuntimeInvisibleTypeAnnotationsAttributeParser,
                ),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeInvisibleTypeAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects ConstantValue attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("ConstantValue", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-constant-value.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ConstantValue"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field_info"), failure.message)
    }

    @Test
    fun `rejects Exceptions attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("Exceptions", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-exceptions.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Exceptions"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method_info"), failure.message)
    }

    @Test
    fun `rejects Code attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("Code", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-code.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Code"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method_info"), failure.message)
    }

    @Test
    fun `rejects Module attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("Module", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-module.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Module"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects EnclosingMethod attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("EnclosingMethod", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-enclosing-method.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("EnclosingMethod"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects InnerClasses attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("InnerClasses", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-inner-classes.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("InnerClasses"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects SourceDebugExtension attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("SourceDebugExtension", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-source-debug-extension.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("SourceDebugExtension"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects SourceFile attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("SourceFile", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-source-file.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("SourceFile"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects BootstrapMethods attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("BootstrapMethods", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-bootstrap-methods.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("BootstrapMethods"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ClassFile"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("AnnotationDefault", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-annotation-default.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method_info"), failure.message)
    }

    @Test
    fun `rejects MethodParameters attributes in record components`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
                ConstantUtf8Entry("MethodParameters", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(
                        0, 1,
                        0, 1,
                        0, 0, 0, 14,
                        0, 1,
                        0, 2, 0, 3,
                        0, 1,
                        0, 4, 0, 0, 0, 0,
                    ),
                    source = "bad-record-method-parameters.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("MethodParameters"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method_info"), failure.message)
    }

    @Test
    fun `rejects parameter annotation attributes in record components`() {
        listOf(
            "RuntimeVisibleParameterAnnotations",
            "RuntimeInvisibleParameterAnnotations",
        ).forEach { attributeName ->
            val constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("Record", byteArrayOf()),
                    ConstantUtf8Entry("value", byteArrayOf()),
                    ConstantUtf8Entry("I", byteArrayOf()),
                    ConstantUtf8Entry(attributeName, byteArrayOf()),
                ),
            )

            val failure = assertFailsWith<ClassFileFormatException> {
                AttributeInfoParser.parseAttributes(
                    reader = ClassFileByteReader(
                        byteArrayOf(
                            0, 1,
                            0, 1,
                            0, 0, 0, 14,
                            0, 1,
                            0, 2, 0, 3,
                            0, 1,
                            0, 4, 0, 0, 0, 0,
                        ),
                        source = "bad-record-parameter-annotations.class",
                    ),
                    constantPool = constantPool,
                    registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                    ownerPath = "ClassFile",
                )
            }

            assertTrue(failure.message.orEmpty().contains("components[0]"), failure.message)
            assertTrue(failure.message.orEmpty().contains(attributeName), failure.message)
            assertTrue(failure.message.orEmpty().contains("method_info"), failure.message)
        }
    }

    @Test
    fun `rejects component name index that is not UTF-8`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 8, 0, 1, 0, 2, 0, 2, 0, 0),
                    source = "bad-record.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0].name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }

    @Test
    fun `rejects component names that are not valid unqualified names`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("bad/name", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 8, 0, 1, 0, 2, 0, 3, 0, 0),
                    source = "bad-record.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0].name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unqualified name"), failure.message)
    }

    @Test
    fun `rejects component descriptors that are not field descriptors`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Record", byteArrayOf()),
                ConstantUtf8Entry("value", byteArrayOf()),
                ConstantUtf8Entry("()V", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 8, 0, 1, 0, 2, 0, 3, 0, 0),
                    source = "bad-record.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Record" to RecordAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("components[0].descriptor_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("field descriptor"), failure.message)
    }
}
