package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PermittedSubclassesAttributeParserTest {
    @Test
    fun `parses PermittedSubclasses attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("PermittedSubclasses", byteArrayOf()),
                ConstantUtf8Entry("pkg/AllowedOne", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("pkg/AllowedTwo", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(4)),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 6, 0, 2, 0, 3, 0, 5),
                source = "permitted-subclasses.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("PermittedSubclasses" to PermittedSubclassesAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<PermittedSubclassesAttribute>(attributes.single())
        assertEquals(listOf(ConstantPoolIndex(3), ConstantPoolIndex(5)), attribute.classes)
    }

    @Test
    fun `rejects permitted subclass entry that is not a class constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("PermittedSubclasses", byteArrayOf()),
                ConstantUtf8Entry("pkg/Allowed", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 4, 0, 1, 0, 2),
                    source = "bad-permitted-subclasses.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("PermittedSubclasses" to PermittedSubclassesAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("classes[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class"), failure.message)
    }
}
