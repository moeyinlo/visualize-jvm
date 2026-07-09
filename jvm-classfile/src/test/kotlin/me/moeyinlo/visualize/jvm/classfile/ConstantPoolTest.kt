package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConstantPoolTest {
    @Test
    fun `constant pool indexes are one based`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            ConstantPoolIndex(0)
        }

        assertTrue(failure.message.orEmpty().contains("one-based"), failure.message)
    }

    @Test
    fun `models long and double two slot constants with unusable following slots`() {
        val first = TestConstant("first")
        val wide = TestConstant("wide", occupiesTwoSlots = true)
        val last = TestConstant("last")

        val pool = ConstantPool.fromEntries(listOf(first, wide, last))

        assertEquals(5, pool.constantPoolCount)
        assertEquals(4, pool.slotCount)
        assertSame(first, pool[ConstantPoolIndex(1)])
        assertSame(wide, pool[ConstantPoolIndex(2)])
        assertIs<ConstantPoolSlot.Unusable>(pool.slotAt(ConstantPoolIndex(3)))
        assertSame(last, pool[ConstantPoolIndex(4)])
    }

    @Test
    fun `rejects dereferencing unusable and out of range indexes`() {
        val pool = ConstantPool.fromEntries(
            listOf(
                TestConstant("wide", occupiesTwoSlots = true),
            ),
        )

        val unusable = assertFailsWith<ConstantPoolFormatException> {
            pool[ConstantPoolIndex(2)]
        }
        assertTrue(unusable.message.orEmpty().contains("#2"), unusable.message)
        assertTrue(unusable.message.orEmpty().contains("unusable"), unusable.message)

        val outOfRange = assertFailsWith<ConstantPoolFormatException> {
            pool[ConstantPoolIndex(3)]
        }
        assertTrue(outOfRange.message.orEmpty().contains("#3"), outOfRange.message)
        assertTrue(outOfRange.message.orEmpty().contains("constant_pool_count=3"), outOfRange.message)
    }

    private data class TestConstant(
        val name: String,
        override val occupiesTwoSlots: Boolean = false,
    ) : ConstantPoolEntry
}
