package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceId
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

    @Test
    fun `synthetic static int slots compare and exchange returns witness value`() {
        val memory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 42))

        assertEquals(42, memory.compareAndExchangeStaticInt(offset = 7L, expected = 1, replacement = 2))
        assertEquals(42, memory.getStaticInt(offset = 7L))
        assertEquals(42, memory.compareAndExchangeStaticInt(offset = 7L, expected = 42, replacement = 43))
        assertEquals(43, memory.getStaticInt(offset = 7L))
    }

    @Test
    fun `synthetic static reference slots default to null can be written and compare and set atomically`() {
        val first = JvmObjectReferenceValue(JvmReferenceId(1))
        val second = JvmObjectReferenceValue(JvmReferenceId(2))
        val third = JvmObjectReferenceValue(JvmReferenceId(3))
        val memory = JvmUnsafeSyntheticMemory(staticReferenceSlots = mapOf(7L to first))

        assertEquals(JvmNullValue, memory.getStaticReference(offset = 9L))
        assertEquals(first, memory.getStaticReference(offset = 7L))
        memory.putStaticReference(offset = 7L, value = second)
        assertEquals(false, memory.compareAndSetStaticReference(offset = 7L, expected = first, replacement = third))
        assertEquals(true, memory.compareAndSetStaticReference(offset = 7L, expected = second, replacement = third))
        assertEquals(third, memory.getStaticReference(offset = 7L))
    }
}
