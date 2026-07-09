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
}
