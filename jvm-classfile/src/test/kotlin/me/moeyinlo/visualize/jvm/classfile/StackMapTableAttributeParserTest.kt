package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StackMapTableAttributeParserTest {
    @Test
    fun `parses all StackMapTable frame variants`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("StackMapTable", byteArrayOf()),
                ConstantClassEntry(ConstantPoolIndex(3)),
                ConstantUtf8Entry("java/lang/Object", byteArrayOf()),
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
                    37,
                    0,
                    7,
                    10,
                    64,
                    1,
                    247.toByte(),
                    0x01,
                    0x2C,
                    7,
                    0,
                    2,
                    249.toByte(),
                    0,
                    5,
                    251.toByte(),
                    0,
                    6,
                    253.toByte(),
                    0,
                    7,
                    4,
                    8,
                    0,
                    9,
                    255.toByte(),
                    0,
                    8,
                    0,
                    2,
                    0,
                    7,
                    0,
                    2,
                    0,
                    2,
                    3,
                    5,
                ),
                source = "stack-map-table.class",
            ),
            constantPool = constantPool,
            registry = AttributeParserRegistry.of("StackMapTable" to StackMapTableAttributeParser),
            ownerPath = "methods[0].attributes[0]",
        )

        val table = assertIs<StackMapTableAttribute>(attributes.single())
        assertEquals(7, table.entries.size)
        assertEquals(10, assertIs<SameStackMapFrame>(table.entries[0]).offsetDelta)
        assertIs<VerificationTypeInfo.Integer>(assertIs<SameLocalsOneStackItemFrame>(table.entries[1]).stack)
        assertEquals(
            ConstantPoolIndex(2),
            assertIs<VerificationTypeInfo.ObjectVariable>(
                assertIs<SameLocalsOneStackItemFrameExtended>(table.entries[2]).stack,
            ).cpoolIndex,
        )
        assertEquals(2, assertIs<ChopStackMapFrame>(table.entries[3]).choppedLocals)
        assertEquals(6, assertIs<SameStackMapFrameExtended>(table.entries[4]).offsetDelta)
        val appendFrame = assertIs<AppendStackMapFrame>(table.entries[5])
        assertEquals(2, appendFrame.locals.size)
        assertIs<VerificationTypeInfo.Long>(appendFrame.locals[0])
        assertEquals(9, assertIs<VerificationTypeInfo.UninitializedVariable>(appendFrame.locals[1]).offset)
        val fullFrame = assertIs<FullStackMapFrame>(table.entries[6])
        assertIs<VerificationTypeInfo.Top>(fullFrame.locals[0])
        assertEquals(ConstantPoolIndex(2), assertIs<VerificationTypeInfo.ObjectVariable>(fullFrame.locals[1]).cpoolIndex)
        assertIs<VerificationTypeInfo.Double>(fullFrame.stack[0])
        assertIs<VerificationTypeInfo.Null>(fullFrame.stack[1])
    }

    @Test
    fun `rejects reserved StackMapTable frame type`() {
        val constantPool = ConstantPool.fromEntries(
            listOf(
                ConstantUtf8Entry("StackMapTable", byteArrayOf()),
            ),
        )

        val failure = assertFailsWith<ClassFileFormatException> {
            AttributeInfoParser.parseAttributes(
                reader = ClassFileByteReader(
                    byteArrayOf(0, 1, 0, 1, 0, 0, 0, 3, 0, 1, 128.toByte()),
                    source = "reserved-stack-map-frame.class",
                ),
                constantPool = constantPool,
                registry = AttributeParserRegistry.of("StackMapTable" to StackMapTableAttributeParser),
                ownerPath = "methods[0].attributes[0]",
            )
        }

        assertTrue(failure.message.orEmpty().contains("reserved"), failure.message)
        assertTrue(failure.message.orEmpty().contains("128"), failure.message)
    }
}
