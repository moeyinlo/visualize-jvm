package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NestedClassAttributesParserTest {
    @Test
    fun `parses InnerClasses attribute entries`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("InnerClasses", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("pkg/Outer\$Inner", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(5)),
                ConstantUtf8Entry("pkg/Outer", byteArrayOf()),
                ConstantUtf8Entry("Inner", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 10, 0, 1, 0, 2, 0, 4, 0, 6, 0, 1),
                source = "inner-classes.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("InnerClasses" to InnerClassesAttributeParser),
            ownerPath = "ClassFile",
        )

        val entry = assertIs<InnerClassesAttribute>(attributes.single()).classes.single()
        assertEquals(ConstantPoolIndex(2), entry.innerClassInfoIndex)
        assertEquals(ConstantPoolIndex(4), entry.outerClassInfoIndex)
        assertEquals(ConstantPoolIndex(6), entry.innerNameIndex)
        assertEquals(0x0001, entry.innerClassAccessFlags)
    }

    @Test
    fun `parses anonymous InnerClasses entry with nullable outer and name indexes`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("InnerClasses", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("pkg/Outer\$1", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 10, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0),
                source = "anonymous-inner.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("InnerClasses" to InnerClassesAttributeParser),
            ownerPath = "ClassFile",
        )

        val entry = assertIs<InnerClassesAttribute>(attributes.single()).classes.single()
        assertNull(entry.outerClassInfoIndex)
        assertNull(entry.innerNameIndex)
    }

    @Test
    fun `rejects InnerClasses inner names that are not unqualified names`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("InnerClasses", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("pkg/Outer\$Inner", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(5)),
                ConstantUtf8Entry("pkg/Outer", byteArrayOf()),
                ConstantUtf8Entry("bad/name", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 10, 0, 1, 0, 2, 0, 4, 0, 6, 0, 1),
                    source = "bad-inner-name.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("InnerClasses" to InnerClassesAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("inner_name_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("unqualified name"), failure.message)
    }

    @Test
    fun `parses EnclosingMethod attribute with optional method index`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("EnclosingMethod", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("pkg/Outer", byteArrayOf()),
                ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                ConstantUtf8Entry("run", byteArrayOf()),
                ConstantUtf8Entry("()V", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 4, 0, 2, 0, 4),
                source = "enclosing-method.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("EnclosingMethod" to EnclosingMethodAttributeParser),
            ownerPath = "ClassFile",
        )

        val attribute = assertIs<EnclosingMethodAttribute>(attributes.single())
        assertEquals(ConstantPoolIndex(2), attribute.classIndex)
        assertEquals(ConstantPoolIndex(4), attribute.methodIndex)
    }

    @Test
    fun `rejects EnclosingMethod class index that is not a class constant`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("EnclosingMethod", byteArrayOf()),
                ConstantIntegerEntry(1),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 4, 0, 2, 0, 0),
                    source = "bad-enclosing-method.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("EnclosingMethod" to EnclosingMethodAttributeParser),
                ownerPath = "ClassFile",
            )
        }

        assertTrue(failure.message.orEmpty().contains("class_index"), failure.message)
        assertTrue(failure.message.orEmpty().contains("CONSTANT_Class"), failure.message)
    }
}
