package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NestAttributesParserTest {
    @Test
    fun `parses NestHost attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("NestHost", byteArrayOf()),
                ConstantUtf8Entry("pkg/Host", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 3),
                source = "nest-host.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("NestHost" to NestHostAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<NestHostAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(3), attribute.hostClassIndex)
    }

    @Test
    fun `parses NestMembers attribute`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("NestMembers", byteArrayOf()),
                ConstantUtf8Entry("pkg/MemberOne", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(2)),
                ConstantUtf8Entry("pkg/MemberTwo", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(4)),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 6, 0, 2, 0, 3, 0, 5),
                source = "nest-members.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("NestMembers" to NestMembersAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<NestMembersAttribute>(attributes.single())
        assertEquals(listOf(ConstantPoolIndex(3), ConstantPoolIndex(5)), attribute.classes)
    }

    @Test
    fun `rejects NestHost index that is not a class constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("NestHost", byteArrayOf()),
                ConstantUtf8Entry("pkg/Host", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 2, 0, 2),
                    source = "bad-nest-host.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("NestHost" to NestHostAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("host_class_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class"), failure.message)
    }

    @Test
    fun `rejects NestMembers entry that is not a class constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("NestMembers", byteArrayOf()),
                ConstantUtf8Entry("pkg/Member", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 4, 0, 1, 0, 2),
                    source = "bad-nest-members.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("NestMembers" to NestMembersAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("classes[0]"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class"), failure.message)
    }
}
