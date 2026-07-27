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
    fun `synthetic static boolean slots default to false can be written and compare and set atomically`() {
        val memory = JvmUnsafeSyntheticMemory(staticBooleanSlots = mapOf(7L to true))

        assertEquals(false, memory.getStaticBoolean(offset = 9L))
        assertEquals(true, memory.getStaticBoolean(offset = 7L))
        memory.putStaticBoolean(offset = 7L, value = false)
        assertEquals(false, memory.compareAndSetStaticBoolean(offset = 7L, expected = true, replacement = true))
        assertEquals(true, memory.compareAndSetStaticBoolean(offset = 7L, expected = false, replacement = true))
        assertEquals(true, memory.getStaticBoolean(offset = 7L))
    }

    @Test
    fun `synthetic static byte slots default to zero can be written and compare and set atomically`() {
        val memory = JvmUnsafeSyntheticMemory(staticByteSlots = mapOf(7L to 1.toByte()))

        assertEquals(0.toByte(), memory.getStaticByte(offset = 9L))
        assertEquals(1.toByte(), memory.getStaticByte(offset = 7L))
        memory.putStaticByte(offset = 7L, value = 2.toByte())
        assertEquals(false, memory.compareAndSetStaticByte(offset = 7L, expected = 1.toByte(), replacement = 3.toByte()))
        assertEquals(true, memory.compareAndSetStaticByte(offset = 7L, expected = 2.toByte(), replacement = 3.toByte()))
        assertEquals(3.toByte(), memory.getStaticByte(offset = 7L))
    }

    @Test
    fun `synthetic static short slots default to zero can be written and compare and set atomically`() {
        val memory = JvmUnsafeSyntheticMemory(staticShortSlots = mapOf(7L to 1.toShort()))

        assertEquals(0.toShort(), memory.getStaticShort(offset = 9L))
        assertEquals(1.toShort(), memory.getStaticShort(offset = 7L))
        memory.putStaticShort(offset = 7L, value = 2.toShort())
        assertEquals(false, memory.compareAndSetStaticShort(offset = 7L, expected = 1.toShort(), replacement = 3.toShort()))
        assertEquals(true, memory.compareAndSetStaticShort(offset = 7L, expected = 2.toShort(), replacement = 3.toShort()))
        assertEquals(3.toShort(), memory.getStaticShort(offset = 7L))
    }

    @Test
    fun `synthetic static char slots default to nul can be written and compare and set atomically`() {
        val memory = JvmUnsafeSyntheticMemory(staticCharSlots = mapOf(7L to 'A'))

        assertEquals('\u0000', memory.getStaticChar(offset = 9L))
        assertEquals('A', memory.getStaticChar(offset = 7L))
        memory.putStaticChar(offset = 7L, value = 'B')
        assertEquals(false, memory.compareAndSetStaticChar(offset = 7L, expected = 'A', replacement = 'C'))
        assertEquals(true, memory.compareAndSetStaticChar(offset = 7L, expected = 'B', replacement = 'C'))
        assertEquals('C', memory.getStaticChar(offset = 7L))
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

    @Test
    fun `synthetic static reference slots compare and exchange returns witness value`() {
        val first = JvmObjectReferenceValue(JvmReferenceId(1))
        val second = JvmObjectReferenceValue(JvmReferenceId(2))
        val third = JvmObjectReferenceValue(JvmReferenceId(3))
        val memory = JvmUnsafeSyntheticMemory(staticReferenceSlots = mapOf(7L to first))

        assertEquals(first, memory.compareAndExchangeStaticReference(offset = 7L, expected = second, replacement = third))
        assertEquals(first, memory.getStaticReference(offset = 7L))
        assertEquals(first, memory.compareAndExchangeStaticReference(offset = 7L, expected = first, replacement = third))
        assertEquals(third, memory.getStaticReference(offset = 7L))
    }
}
