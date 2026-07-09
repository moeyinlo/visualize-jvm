package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LineNumberTableAttributeParserTest {
    @Test
    fun `parses LineNumberTable entries`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("LineNumberTable", byteArrayOf()),
            ),
        )

        val attributes = AttributeInfoParser.parseAttributes(
            reader = ClassFileByteReader(
                byteArrayOf(0, 1, 0, 1, 0, 0, 0, 10, 0, 2, 0, 0, 0, 10, 0, 5, 0, 20),
                source = "line-number-table.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("LineNumberTable" to LineNumberTableAttributeParser),
            ownerPath = "methods[0].attributes[0]",
        )

        val table = assertIs<LineNumberTableAttribute>(attributes.single())
        assertEquals(
            listOf(
                LineNumberTableEntry(startPc = 0, lineNumber = 10),
                LineNumberTableEntry(startPc = 5, lineNumber = 20),
            ),
            table.entries,
        )
    }
}
