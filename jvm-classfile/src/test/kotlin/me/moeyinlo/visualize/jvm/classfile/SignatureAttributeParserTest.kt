package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignatureAttributeParserTest {
    @Test
    fun `parses Signature attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("Ljava/util/List<Ljava/lang/String;>;", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                source = "signature.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
            ownerPath = "fields[0]",
        )

        val attribute = assertIs<SignatureAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(2), attribute.signatureIndex)
        assertEquals("Ljava/util/List<Ljava/lang/String;>;", attribute.signature)
    }

    @Test
    fun `rejects Signature attributes before Java 5`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("Ljava/lang/String;", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "java4-signature.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
                ownerPath = "fields[0]",
                majorVersion = 48,
            )
        }

        assertTrue(failure.message.orEmpty().contains("Signature"), failure.message)
        assertTrue(failure.message.orEmpty().contains("major_version=48"), failure.message)
        assertTrue(failure.message.orEmpty().contains("49"), failure.message)
    }

    @Test
    fun `rejects Signature index that is not UTF-8`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "bad-signature.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
                ownerPath = "fields[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("signature_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Utf8"), failure.message)
    }

    @Test
    fun `rejects field Signature that is not a field signature`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "bad-field-signature.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
                ownerPath = "fields[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("field signature"), failure.message)
    }

    @Test
    fun `rejects primitive type argument in field Signature`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("Ljava/util/List<I>;", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "bad-field-signature-type-arg.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
                ownerPath = "fields[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("field signature"), failure.message)
    }

    @Test
    fun `parses method Signature grammar`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("<T:Ljava/lang/Object;>(Ljava/util/List<TT;>;)TT;^Ljava/lang/Exception;^TE;", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                source = "method-signature.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
            ownerPath = "methods[0]",
        )

        assertEquals("<T:Ljava/lang/Object;>(Ljava/util/List<TT;>;)TT;^Ljava/lang/Exception;^TE;", assertIs<SignatureAttribute>(attributes.single()).signature)
    }

    @Test
    fun `rejects method Signature that is not a method signature`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("Ljava/lang/String;", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "bad-method-signature.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
                ownerPath = "methods[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("method signature"), failure.message)
    }

    @Test
    fun `parses class Signature grammar`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/io/Serializable;", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                source = "class-signature.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
            ownerPath = "ClassFile",
        )

        assertEquals("<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/io/Serializable;", assertIs<SignatureAttribute>(attributes.single()).signature)
    }

    @Test
    fun `rejects class Signature that is not a class signature`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("Signature", byteArrayOf()),
                ConstantUtf8Entry("I", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "bad-class-signature.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("Signature" to SignatureAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("class signature"), failure.message)
    }
}
