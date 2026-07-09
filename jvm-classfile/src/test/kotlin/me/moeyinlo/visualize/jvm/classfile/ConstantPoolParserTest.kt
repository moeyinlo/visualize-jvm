package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConstantPoolParserTest {
    @Test
    fun `parses constant pool table and inserts unusable wide slots`() {
        val reader = ClassFileByteReader(
            byteArrayOf(
                0,
                5,
                1,
                0,
                1,
                'A'.code.toByte(),
                5,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                7,
                0,
                1,
            ),
            source = "pool.class",
        )

        val pool = ConstantPoolParser.parse(reader)

        assertEquals(5, pool.constantPoolCount)
        assertEquals(4, pool.slotCount)
        assertEquals("A", assertIs<ConstantUtf8Entry>(pool[ConstantPoolIndex(1)]).value)
        assertEquals(1L, assertIs<ConstantLongEntry>(pool[ConstantPoolIndex(2)]).value)
        assertIs<ConstantPoolSlot.Unusable>(pool.slotAt(ConstantPoolIndex(3)))
        assertEquals(ConstantPoolIndex(1), assertIs<ConstantClassEntry>(pool[ConstantPoolIndex(4)]).nameIndex)
        assertEquals(18, reader.position)
    }

    @Test
    fun `rejects zero constant pool count`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ConstantPoolParser.parse(ClassFileByteReader(byteArrayOf(0, 0), source = "zero-count.class"))
        }

        assertTrue(failure.message.orEmpty().contains("constant_pool_count=0"), failure.message)
    }

    @Test
    fun `rejects wide constants that overflow declared constant pool count`() {
        val failure = assertFailsWith<ClassFileFormatException> {
            ConstantPoolParser.parse(
                ClassFileByteReader(
                    byteArrayOf(0, 2, 5, 0, 0, 0, 0, 0, 0, 0, 1),
                    source = "wide-overflow.class",
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("wide-overflow.class"), failure.message)
        assertTrue(failure.message.orEmpty().contains("#1"), failure.message)
        assertTrue(failure.message.orEmpty().contains("two slots"), failure.message)
    }
}
