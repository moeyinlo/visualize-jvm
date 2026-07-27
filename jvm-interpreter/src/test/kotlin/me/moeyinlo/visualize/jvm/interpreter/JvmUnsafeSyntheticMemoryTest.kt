package me.moeyinlo.visualize.jvm.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmUnsafeSyntheticMemoryTest {
    @Test
    fun `synthetic static long slots default to zero and compare and set atomically`() {
        val memory = JvmUnsafeSyntheticMemory()

        assertEquals(0L, memory.getStaticLong(offset = 7L))
        assertEquals(false, memory.compareAndSetStaticLong(offset = 7L, expected = 1L, replacement = 2L))
        assertEquals(true, memory.compareAndSetStaticLong(offset = 7L, expected = 0L, replacement = 2L))
        assertEquals(2L, memory.getStaticLong(offset = 7L))
    }

    @Test
    fun `synthetic static long slots can be written directly`() {
        val memory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 1L))

        memory.putStaticLong(offset = 7L, value = 2L)
        memory.putStaticLong(offset = 9L, value = 3L)

        assertEquals(2L, memory.getStaticLong(offset = 7L))
        assertEquals(3L, memory.getStaticLong(offset = 9L))
    }

    @Test
    fun `synthetic static long slots compare and exchange returns witness value`() {
        val memory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 42L))

        assertEquals(42L, memory.compareAndExchangeStaticLong(offset = 7L, expected = 1L, replacement = 2L))
        assertEquals(42L, memory.getStaticLong(offset = 7L))
        assertEquals(42L, memory.compareAndExchangeStaticLong(offset = 7L, expected = 42L, replacement = 43L))
        assertEquals(43L, memory.getStaticLong(offset = 7L))
    }

    @Test
    fun `synthetic static int slots default to zero can be written and compare and set atomically`() {
        val memory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 1))

        assertEquals(0, memory.getStaticInt(offset = 9L))
        assertEquals(1, memory.getStaticInt(offset = 7L))
        memory.putStaticInt(offset = 7L, value = 2)
        assertEquals(false, memory.compareAndSetStaticInt(offset = 7L, expected = 1, replacement = 3))
        assertEquals(true, memory.compareAndSetStaticInt(offset = 7L, expected = 2, replacement = 3))
        assertEquals(3, memory.getStaticInt(offset = 7L))
    }
}
