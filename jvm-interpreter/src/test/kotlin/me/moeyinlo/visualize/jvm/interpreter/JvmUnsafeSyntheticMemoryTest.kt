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
}
