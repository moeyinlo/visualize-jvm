package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MethodInfoParserTest {
    @Test
    fun `parses method declarations including raw attributes`() {
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                2,
                0,
                0x01,
                0,
                3,
                0,
                4,
                0,
                0,
                0,
                0x09,
                0,
                5,
                0,
                6,
                0,
                1,
                0,
                7,
                0,
                0,
                0,
                2,
                20,
                21,
            ),
            source = "methods.class",
        )

        val methods = MethodInfoParser.parseMethods(reader)

        assertEquals(2, methods.size)
        assertEquals(0x0001, methods[0].accessFlags)
        assertEquals(ConstantPoolIndex(3), methods[0].nameIndex)
        assertEquals(ConstantPoolIndex(4), methods[0].descriptorIndex)
        assertEquals(emptyList(), methods[0].attributes)

        assertEquals(0x0009, methods[1].accessFlags)
        assertEquals(ConstantPoolIndex(5), methods[1].nameIndex)
        assertEquals(ConstantPoolIndex(6), methods[1].descriptorIndex)
        val attribute = assertIs<RawAttributeInfo>(methods[1].attributes.single())
        assertEquals(ConstantPoolIndex(7), attribute.nameIndex)
        assertContentEquals(byteArrayOf(20, 21), attribute.info)
        assertEquals(26, reader.position)
    }

    @Test
    fun `rejects zero method descriptor index`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 2, 0, 0, 0, 0),
                    source = "bad-method.class",
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0].descriptor_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("zero"), failure.message)
    }

    @Test
    fun `validates method names and descriptors when constant pool is available`() {
        val methods = parseValidatedMethods(
            methodName = "run",
            descriptor = "()V",
            accessFlags = 0x0001,
        )

        assertEquals(ConstantPoolIndex(1), methods.single().nameIndex)
        assertEquals(ConstantPoolIndex(2), methods.single().descriptorIndex)
    }

    @Test
    fun `rejects concrete methods without exactly one Code attribute`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "run",
                descriptor = "()V",
                accessFlags = 0x0001,
                attributes = emptyList(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("exactly one Code attribute"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 0"), failure.message)
    }

    @Test
    fun `rejects concrete methods with duplicate Code attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "run",
                descriptor = "()V",
                accessFlags = 0x0001,
                attributes = listOf(codeAttribute(), codeAttribute()),
            )
        }

        assertTrue(failure.message.orEmpty().contains("exactly one Code attribute"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects native methods with Code attributes`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "run",
                descriptor = "()V",
                accessFlags = 0x0101,
            )
        }

        assertTrue(failure.message.orEmpty().contains("ACC_NATIVE"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not have a Code attribute"), failure.message)
    }

    @Test
    fun `accepts native methods without Code attributes`() {
        val methods = parseValidatedMethods(
            methodName = "run",
            descriptor = "()V",
            accessFlags = 0x0101,
            attributes = emptyList(),
        )

        assertEquals(0x0101, methods.single().accessFlags)
    }

    @Test
    fun `accepts abstract methods without Code attributes`() {
        val methods = parseValidatedMethods(
            methodName = "run",
            descriptor = "()V",
            accessFlags = 0x0401,
            attributes = emptyList(),
        )

        assertEquals(0x0401, methods.single().accessFlags)
    }

    @Test
    fun `requires Code attributes for class initialization methods`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "<clinit>",
                descriptor = "()V",
                accessFlags = 0x0108,
                attributes = emptyList(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("<clinit>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("exactly one Code attribute"), failure.message)
    }

    @Test
    fun `rejects non special method names containing angle brackets`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "bad<name>", descriptor = "()V")
        }

        assertTrue(failure.message.orEmpty().contains("methods[0].name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("method name"), failure.message)
    }

    @Test
    fun `rejects duplicate method name and descriptor pairs`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(accessFlags = 0x0001, attributes = listOf(codeAttribute())),
                        methodEntry(accessFlags = 0x0002, attributes = listOf(codeAttribute())),
                    ),
                    source = "bad-method.class",
                ),
                constantPool = methodValidationPool("run", "()V"),
                attributeParsers = AttributeParserRegistry.Empty,
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("Duplicate method_info"), failure.message)
        assertTrue(failure.message.orEmpty().contains("methods[1]"), failure.message)
    }

    @Test
    fun `rejects duplicate method Signature attributes`() {
        val signatureAttribute = byteArrayOf(0, 3) + intBytes(2) + byteArrayOf(0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(accessFlags = 0x0101, attributes = listOf(signatureAttribute, signatureAttribute)),
                    ),
                    source = "bad-method-signature.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry("Signature", "Signature".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Signature"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate method Exceptions attributes`() {
        val exceptionsAttribute = byteArrayOf(0, 3) + intBytes(4) + byteArrayOf(0, 1, 0, 5)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(accessFlags = 0x0101, attributes = listOf(exceptionsAttribute, exceptionsAttribute)),
                    ),
                    source = "bad-method-exceptions.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry("Exceptions", "Exceptions".encodeToByteArray()),
                        ConstantUtf8Entry("java/lang/Exception", "java/lang/Exception".encodeToByteArray()),
                        ConstantClassEntry(ConstantPoolIndex(4)),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of("Exceptions" to ExceptionsAttributeParser),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("Exceptions"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }


    @Test
    fun `rejects duplicate method RuntimeVisibleAnnotations attributes`() {
        val annotationsAttribute = byteArrayOf(0, 3) + intBytes(2) + byteArrayOf(0, 0)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(accessFlags = 0x0101, attributes = listOf(annotationsAttribute, annotationsAttribute)),
                    ),
                    source = "bad-method-runtime-visible-annotations.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry("RuntimeVisibleAnnotations", "RuntimeVisibleAnnotations".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser,
                ),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate method RuntimeInvisibleAnnotations attributes`() {
        val annotationsAttribute = byteArrayOf(0, 3) + intBytes(2) + byteArrayOf(0, 0)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(accessFlags = 0x0101, attributes = listOf(annotationsAttribute, annotationsAttribute)),
                    ),
                    source = "bad-method-runtime-invisible-annotations.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry("RuntimeInvisibleAnnotations", "RuntimeInvisibleAnnotations".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeInvisibleAnnotations" to RuntimeInvisibleAnnotationsAttributeParser,
                ),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeInvisibleAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate method RuntimeVisibleTypeAnnotations attributes`() {
        val annotationsAttribute = byteArrayOf(0, 3) + intBytes(2) + byteArrayOf(0, 0)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(accessFlags = 0x0101, attributes = listOf(annotationsAttribute, annotationsAttribute)),
                    ),
                    source = "bad-method-runtime-visible-type-annotations.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry("RuntimeVisibleTypeAnnotations", "RuntimeVisibleTypeAnnotations".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeVisibleTypeAnnotations" to RuntimeVisibleTypeAnnotationsAttributeParser,
                ),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleTypeAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate method RuntimeInvisibleTypeAnnotations attributes`() {
        val annotationsAttribute = byteArrayOf(0, 3) + intBytes(2) + byteArrayOf(0, 0)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(accessFlags = 0x0101, attributes = listOf(annotationsAttribute, annotationsAttribute)),
                    ),
                    source = "bad-method-runtime-invisible-type-annotations.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry("RuntimeInvisibleTypeAnnotations", "RuntimeInvisibleTypeAnnotations".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeInvisibleTypeAnnotations" to RuntimeInvisibleTypeAnnotationsAttributeParser,
                ),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeInvisibleTypeAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate method RuntimeVisibleParameterAnnotations attributes`() {
        val parameterAnnotationsAttribute = byteArrayOf(0, 3) + intBytes(1) + byteArrayOf(0)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0101,
                            attributes = listOf(parameterAnnotationsAttribute, parameterAnnotationsAttribute),
                        ),
                    ),
                    source = "bad-method-runtime-visible-parameter-annotations.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry(
                            "RuntimeVisibleParameterAnnotations",
                            "RuntimeVisibleParameterAnnotations".encodeToByteArray(),
                        ),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeVisibleParameterAnnotations" to RuntimeVisibleParameterAnnotationsAttributeParser,
                ),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeVisibleParameterAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate method RuntimeInvisibleParameterAnnotations attributes`() {
        val parameterAnnotationsAttribute = byteArrayOf(0, 3) + intBytes(1) + byteArrayOf(0)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0101,
                            attributes = listOf(parameterAnnotationsAttribute, parameterAnnotationsAttribute),
                        ),
                    ),
                    source = "bad-method-runtime-invisible-parameter-annotations.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry(
                            "RuntimeInvisibleParameterAnnotations",
                            "RuntimeInvisibleParameterAnnotations".encodeToByteArray(),
                        ),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "RuntimeInvisibleParameterAnnotations" to RuntimeInvisibleParameterAnnotationsAttributeParser,
                ),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("RuntimeInvisibleParameterAnnotations"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate method MethodParameters attributes`() {
        val methodParametersAttribute = byteArrayOf(0, 3) + intBytes(1) + byteArrayOf(0)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0101,
                            attributes = listOf(methodParametersAttribute, methodParametersAttribute),
                        ),
                    ),
                    source = "bad-method-parameters.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("run", "run".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry("MethodParameters", "MethodParameters".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "MethodParameters" to MethodParametersAttributeParser,
                ),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("MethodParameters"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects duplicate method AnnotationDefault attributes`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('I'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute, annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-method-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantIntegerEntry(1),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("at most one"), failure.message)
        assertTrue(failure.message.orEmpty().contains("found 2"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault attributes on non annotation interface methods`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('I'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-non-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantIntegerEntry(1),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.Class,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("annotation interface element"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault attributes on annotation interface methods with parameters`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('I'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-parameterized-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("(I)I", "(I)I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantIntegerEntry(1),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not declare parameters"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault attributes on annotation interface methods returning void`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('I'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-void-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantIntegerEntry(1),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not return void"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault attributes on annotation interface methods with Exceptions`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('I'.code.toByte(), 0, 4)
        val exceptionsAttribute = byteArrayOf(0, 5) + intBytes(4) + byteArrayOf(0, 1, 0, 7)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute, exceptionsAttribute),
                        ),
                    ),
                    source = "bad-throws-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantIntegerEntry(1),
                        ConstantUtf8Entry("Exceptions", "Exceptions".encodeToByteArray()),
                        ConstantUtf8Entry("java/lang/Exception", "java/lang/Exception".encodeToByteArray()),
                        ConstantClassEntry(ConstantPoolIndex(6)),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                    "Exceptions" to ExceptionsAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not declare Exceptions"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault attributes on concrete annotation interface methods`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('I'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0001,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute, codeAttribute(nameIndex = 5)),
                        ),
                    ),
                    source = "bad-concrete-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantIntegerEntry(1),
                        ConstantUtf8Entry("Code", "Code".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                    "Code" to CodeAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_PUBLIC and ACC_ABSTRACT"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault attributes on annotation interface methods returning nested arrays`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('['.code.toByte(), 0, 0)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-nested-array-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()[[I", "()[[I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("must not return nested arrays"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault string value for int annotation interface methods`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('s'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-int-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantUtf8Entry("not an int", "not an int".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 'I'"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 's'"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault int value for String annotation interface methods`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('I'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-string-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()Ljava/lang/String;", "()Ljava/lang/String;".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantIntegerEntry(1),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 's'"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 'I'"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault string value for Class annotation interface methods`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('s'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-class-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()Ljava/lang/Class;", "()Ljava/lang/Class;".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantUtf8Entry("not a class literal", "not a class literal".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 'c'"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 's'"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault int value for array annotation interface methods`() {
        val annotationDefaultAttribute = byteArrayOf(0, 3) + intBytes(3) + byteArrayOf('I'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-array-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()[I", "()[I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantIntegerEntry(1),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag '['"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 'I'"), failure.message)
    }

    @Test
    fun `rejects AnnotationDefault string array element for int array annotation interface methods`() {
        val annotationDefaultAttribute = byteArrayOf(
            0,
            3,
        ) + intBytes(6) + byteArrayOf('['.code.toByte(), 0, 1, 's'.code.toByte(), 0, 4)

        val failure = assertFailsWith<ClassFileFormatException> {
            MethodInfoParser.parseMethods(
                reader = ClassFileByteReader(
                    methodTable(
                        methodEntry(
                            accessFlags = 0x0401,
                            descriptorIndex = 2,
                            attributes = listOf(annotationDefaultAttribute),
                        ),
                    ),
                    source = "bad-int-array-element-annotation-default.class",
                ),
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()[I", "()[I".encodeToByteArray()),
                        ConstantUtf8Entry("AnnotationDefault", "AnnotationDefault".encodeToByteArray()),
                        ConstantUtf8Entry("not an int", "not an int".encodeToByteArray()),
                    ),
                ),
                attributeParsers = AttributeParserRegistry.of(
                    "AnnotationDefault" to AnnotationDefaultAttributeParser,
                ),
                classKind = ClassFileKind.AnnotationInterface,
                majorVersion = 70,
            )
        }

        assertTrue(failure.message.orEmpty().contains("methods[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("AnnotationDefault"), failure.message)
        assertTrue(failure.message.orEmpty().contains("array element"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 'I'"), failure.message)
        assertTrue(failure.message.orEmpty().contains("tag 's'"), failure.message)
    }
    @Test
    fun `rejects instance initialization methods in interfaces`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "<init>",
                descriptor = "()V",
                accessFlags = 0x0001,
                classKind = ClassFileKind.Interface,
            )
        }

        assertTrue(failure.message.orEmpty().contains("<init>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("not permitted"), failure.message)
    }

    @Test
    fun `rejects instance initialization methods returning values`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "<init>", descriptor = "()I", accessFlags = 0x0001)
        }

        assertTrue(failure.message.orEmpty().contains("<init>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("void"), failure.message)
    }

    @Test
    fun `accepts unassigned strict bit on modern instance initialization methods`() {
        val methods = parseValidatedMethods(methodName = "<init>", descriptor = "()V", accessFlags = 0x0801)

        assertEquals(0x0801, methods.single().accessFlags)
    }

    @Test
    fun `rejects class initialization methods with parameters on modern class files`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "<clinit>", descriptor = "(I)V", accessFlags = 0x0008)
        }

        assertTrue(failure.message.orEmpty().contains("<clinit>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("()V"), failure.message)
    }

    @Test
    fun `rejects class initialization methods without static flag`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "<clinit>", descriptor = "()V", accessFlags = 0x0000)
        }

        assertTrue(failure.message.orEmpty().contains("<clinit>"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_STATIC"), failure.message)
    }

    @Test
    fun `rejects class methods with multiple access visibility flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "run", descriptor = "()V", accessFlags = 0x0003)
        }

        assertTrue(failure.message.orEmpty().contains("ACC_PUBLIC"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_PRIVATE"), failure.message)
    }

    @Test
    fun `rejects abstract methods with static flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(methodName = "run", descriptor = "()V", accessFlags = 0x0408)
        }

        assertTrue(failure.message.orEmpty().contains("ACC_ABSTRACT"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_STATIC"), failure.message)
    }

    @Test
    fun `rejects interface methods with protected flags`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "run",
                descriptor = "()V",
                accessFlags = 0x0004,
                classKind = ClassFileKind.Interface,
            )
        }

        assertTrue(failure.message.orEmpty().contains("interface methods"), failure.message)
        assertTrue(failure.message.orEmpty().contains("ACC_PROTECTED"), failure.message)
    }

    @Test
    fun `rejects modern interface methods without exactly one of public or private`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            parseValidatedMethods(
                methodName = "run",
                descriptor = "()V",
                accessFlags = 0x0400,
                classKind = ClassFileKind.Interface,
            )
        }

        assertTrue(failure.message.orEmpty().contains("exactly one of ACC_PUBLIC and ACC_PRIVATE"), failure.message)
    }

    private fun parseValidatedMethods(
        methodName: String,
        descriptor: String,
        accessFlags: Int = 0x0001,
        classKind: ClassFileKind = ClassFileKind.Class,
        majorVersion: Int = 70,
        attributes: List<ByteArray> = listOf(codeAttribute()),
    ): List<MethodInfo> =
        MethodInfoParser.parseMethods(
            reader = ClassFileByteReader(
                methodTable(methodEntry(accessFlags = accessFlags, attributes = attributes)),
                source = "validated-methods.class",
            ),
            constantPool = methodValidationPool(methodName, descriptor),
            attributeParsers = AttributeParserRegistry.Empty,
            classKind = classKind,
            majorVersion = majorVersion,
        )

    private fun methodValidationPool(
        name: String,
        descriptor: String,
    ): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry(name, name.encodeToByteArray()),
                ConstantUtf8Entry(descriptor, descriptor.encodeToByteArray()),
                ConstantUtf8Entry("Code", "Code".encodeToByteArray()),
            ),
        )

    private fun methodTable(vararg methods: ByteArray): ByteArray =
        byteArrayOf(0, methods.size.toByte()) + methods.fold(byteArrayOf()) { bytes, method -> bytes + method }

    private fun methodEntry(
        accessFlags: Int,
        nameIndex: Int = 1,
        descriptorIndex: Int = 2,
        attributes: List<ByteArray> = emptyList(),
    ): ByteArray =
        byteArrayOf(
            (accessFlags ushr 8).toByte(),
            accessFlags.toByte(),
            (nameIndex ushr 8).toByte(),
            nameIndex.toByte(),
            (descriptorIndex ushr 8).toByte(),
            descriptorIndex.toByte(),
            0,
            attributes.size.toByte(),
        ) + attributes.fold(byteArrayOf()) { bytes, attribute -> bytes + attribute }

    private fun codeAttribute(nameIndex: Int = 3): ByteArray {
        val body = byteArrayOf(
            0,
            0,
            0,
            1,
        ) + intBytes(1) +
            byteArrayOf(
                0xB1.toByte(),
                0,
                0,
                0,
                0,
            )
        return byteArrayOf(
            (nameIndex ushr 8).toByte(),
            nameIndex.toByte(),
        ) + intBytes(body.size) + body
    }

    private fun intBytes(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )
}
