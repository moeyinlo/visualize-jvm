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
    fun `synthetic static long slots get and add returns witness value`() {
        val memory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 42L))

        assertEquals(42L, memory.getAndAddStaticLong(offset = 7L, delta = 5L))
        assertEquals(47L, memory.getStaticLong(offset = 7L))
        assertEquals(0L, memory.getAndAddStaticLong(offset = 9L, delta = -3L))
        assertEquals(-3L, memory.getStaticLong(offset = 9L))
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
    fun `synthetic static int slots get and add returns witness value`() {
        val memory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 42))

        assertEquals(42, memory.getAndAddStaticInt(offset = 7L, delta = 5))
        assertEquals(47, memory.getStaticInt(offset = 7L))
        assertEquals(0, memory.getAndAddStaticInt(offset = 9L, delta = -3))
        assertEquals(-3, memory.getStaticInt(offset = 9L))
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
    fun `synthetic static boolean slots compare and exchange returns witness value`() {
        val memory = JvmUnsafeSyntheticMemory(staticBooleanSlots = mapOf(7L to true))

        assertEquals(true, memory.compareAndExchangeStaticBoolean(offset = 7L, expected = false, replacement = false))
        assertEquals(true, memory.getStaticBoolean(offset = 7L))
        assertEquals(true, memory.compareAndExchangeStaticBoolean(offset = 7L, expected = true, replacement = false))
        assertEquals(false, memory.getStaticBoolean(offset = 7L))
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
    fun `synthetic static byte slots compare and exchange returns witness value`() {
        val memory = JvmUnsafeSyntheticMemory(staticByteSlots = mapOf(7L to 42.toByte()))

        assertEquals(42.toByte(), memory.compareAndExchangeStaticByte(offset = 7L, expected = 1.toByte(), replacement = 2.toByte()))
        assertEquals(42.toByte(), memory.getStaticByte(offset = 7L))
        assertEquals(42.toByte(), memory.compareAndExchangeStaticByte(offset = 7L, expected = 42.toByte(), replacement = 43.toByte()))
        assertEquals(43.toByte(), memory.getStaticByte(offset = 7L))
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
    fun `synthetic static short slots compare and exchange returns witness value`() {
        val memory = JvmUnsafeSyntheticMemory(staticShortSlots = mapOf(7L to 42.toShort()))

        assertEquals(42.toShort(), memory.compareAndExchangeStaticShort(offset = 7L, expected = 1.toShort(), replacement = 2.toShort()))
        assertEquals(42.toShort(), memory.getStaticShort(offset = 7L))
        assertEquals(42.toShort(), memory.compareAndExchangeStaticShort(offset = 7L, expected = 42.toShort(), replacement = 43.toShort()))
        assertEquals(43.toShort(), memory.getStaticShort(offset = 7L))
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
    fun `synthetic static char slots compare and exchange returns witness value`() {
        val memory = JvmUnsafeSyntheticMemory(staticCharSlots = mapOf(7L to 'A'))

        assertEquals('A', memory.compareAndExchangeStaticChar(offset = 7L, expected = 'B', replacement = 'C'))
        assertEquals('A', memory.getStaticChar(offset = 7L))
        assertEquals('A', memory.compareAndExchangeStaticChar(offset = 7L, expected = 'A', replacement = 'C'))
        assertEquals('C', memory.getStaticChar(offset = 7L))
    }

    @Test
    fun `synthetic static float slots default to zero can be written and compare and set atomically`() {
        val memory = JvmUnsafeSyntheticMemory(staticFloatSlots = mapOf(7L to 1.25f))

        assertEquals(0.0f, memory.getStaticFloat(offset = 9L))
        assertEquals(1.25f, memory.getStaticFloat(offset = 7L))
        memory.putStaticFloat(offset = 7L, value = 2.5f)
        assertEquals(false, memory.compareAndSetStaticFloat(offset = 7L, expected = 1.25f, replacement = 3.75f))
        assertEquals(true, memory.compareAndSetStaticFloat(offset = 7L, expected = 2.5f, replacement = 3.75f))
        assertEquals(3.75f, memory.getStaticFloat(offset = 7L))
    }

    @Test
    fun `synthetic static float slots compare and set uses raw bits`() {
        val storedNaN = java.lang.Float.intBitsToFloat(0x7fc00001)
        val otherNaN = java.lang.Float.intBitsToFloat(0x7fc00002)
        val memory = JvmUnsafeSyntheticMemory(staticFloatSlots = mapOf(7L to storedNaN, 9L to 0.0f))

        assertEquals(false, memory.compareAndSetStaticFloat(offset = 7L, expected = otherNaN, replacement = 1.0f))
        assertEquals(storedNaN.toRawBits(), memory.getStaticFloat(offset = 7L).toRawBits())
        assertEquals(true, memory.compareAndSetStaticFloat(offset = 7L, expected = storedNaN, replacement = 1.0f))
        assertEquals(1.0f, memory.getStaticFloat(offset = 7L))
        assertEquals(false, memory.compareAndSetStaticFloat(offset = 9L, expected = -0.0f, replacement = 2.0f))
        assertEquals(0.0f.toRawBits(), memory.getStaticFloat(offset = 9L).toRawBits())
    }

    @Test
    fun `synthetic static float slots compare and exchange returns witness by raw bits`() {
        val storedNaN = java.lang.Float.intBitsToFloat(0x7fc00001)
        val otherNaN = java.lang.Float.intBitsToFloat(0x7fc00002)
        val memory = JvmUnsafeSyntheticMemory(staticFloatSlots = mapOf(7L to storedNaN, 9L to 0.0f))

        assertEquals(
            storedNaN.toRawBits(),
            memory.compareAndExchangeStaticFloat(offset = 7L, expected = otherNaN, replacement = 1.0f).toRawBits(),
        )
        assertEquals(storedNaN.toRawBits(), memory.getStaticFloat(offset = 7L).toRawBits())
        assertEquals(
            storedNaN.toRawBits(),
            memory.compareAndExchangeStaticFloat(offset = 7L, expected = storedNaN, replacement = 1.0f).toRawBits(),
        )
        assertEquals(1.0f, memory.getStaticFloat(offset = 7L))
        assertEquals(
            0.0f.toRawBits(),
            memory.compareAndExchangeStaticFloat(offset = 9L, expected = -0.0f, replacement = 2.0f).toRawBits(),
        )
        assertEquals(0.0f.toRawBits(), memory.getStaticFloat(offset = 9L).toRawBits())
    }

    @Test
    fun `synthetic static double slots default to zero can be written and compare and set atomically`() {
        val memory = JvmUnsafeSyntheticMemory(staticDoubleSlots = mapOf(7L to 1.25))

        assertEquals(0.0, memory.getStaticDouble(offset = 9L))
        assertEquals(1.25, memory.getStaticDouble(offset = 7L))
        memory.putStaticDouble(offset = 7L, value = 2.5)
        assertEquals(false, memory.compareAndSetStaticDouble(offset = 7L, expected = 1.25, replacement = 3.75))
        assertEquals(true, memory.compareAndSetStaticDouble(offset = 7L, expected = 2.5, replacement = 3.75))
        assertEquals(3.75, memory.getStaticDouble(offset = 7L))
    }

    @Test
    fun `synthetic static double slots compare and set uses raw bits`() {
        val storedNaN = java.lang.Double.longBitsToDouble(0x7ff8000000000001L)
        val otherNaN = java.lang.Double.longBitsToDouble(0x7ff8000000000002L)
        val memory = JvmUnsafeSyntheticMemory(staticDoubleSlots = mapOf(7L to storedNaN, 9L to 0.0))

        assertEquals(false, memory.compareAndSetStaticDouble(offset = 7L, expected = otherNaN, replacement = 1.0))
        assertEquals(storedNaN.toRawBits(), memory.getStaticDouble(offset = 7L).toRawBits())
        assertEquals(true, memory.compareAndSetStaticDouble(offset = 7L, expected = storedNaN, replacement = 1.0))
        assertEquals(1.0, memory.getStaticDouble(offset = 7L))
        assertEquals(false, memory.compareAndSetStaticDouble(offset = 9L, expected = -0.0, replacement = 2.0))
        assertEquals(0.0.toRawBits(), memory.getStaticDouble(offset = 9L).toRawBits())
    }

    @Test
    fun `synthetic static double slots compare and exchange returns witness by raw bits`() {
        val storedNaN = java.lang.Double.longBitsToDouble(0x7ff8000000000001L)
        val otherNaN = java.lang.Double.longBitsToDouble(0x7ff8000000000002L)
        val memory = JvmUnsafeSyntheticMemory(staticDoubleSlots = mapOf(7L to storedNaN, 9L to 0.0))

        assertEquals(
            storedNaN.toRawBits(),
            memory.compareAndExchangeStaticDouble(offset = 7L, expected = otherNaN, replacement = 1.0).toRawBits(),
        )
        assertEquals(storedNaN.toRawBits(), memory.getStaticDouble(offset = 7L).toRawBits())
        assertEquals(
            storedNaN.toRawBits(),
            memory.compareAndExchangeStaticDouble(offset = 7L, expected = storedNaN, replacement = 1.0).toRawBits(),
        )
        assertEquals(1.0, memory.getStaticDouble(offset = 7L))
        assertEquals(
            0.0.toRawBits(),
            memory.compareAndExchangeStaticDouble(offset = 9L, expected = -0.0, replacement = 2.0).toRawBits(),
        )
        assertEquals(0.0.toRawBits(), memory.getStaticDouble(offset = 9L).toRawBits())
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
