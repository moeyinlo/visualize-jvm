package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmFloatingPointValueTest {
    @Test
    fun `floating point runtime values have JVMS slot categories`() {
        assertEquals(JvmValueCategory.Category1, JvmFloatValue(1.0f).category)
        assertEquals(1, JvmFloatValue(1.0f).category.slotWidth)

        assertEquals(JvmValueCategory.Category2, JvmDoubleValue(1.0).category)
        assertEquals(2, JvmDoubleValue(1.0).category.slotWidth)
    }

    @Test
    fun `floating point runtime values preserve IEEE 754 float bit patterns`() {
        val bitPatterns = listOf(
            0x0000_0000,
            0x8000_0000.toInt(),
            0x7F80_0000,
            0xFF80_0000.toInt(),
            0x7FA1_2345,
            0xFFA1_2345.toInt(),
        )

        bitPatterns.forEach { bits ->
            val value = JvmFloatValue(java.lang.Float.intBitsToFloat(bits))

            assertEquals(bits, java.lang.Float.floatToRawIntBits(value.value))
        }
    }

    @Test
    fun `floating point runtime values preserve IEEE 754 double bit patterns`() {
        val bitPatterns = listOf(
            0x0000_0000_0000_0000L,
            Long.MIN_VALUE,
            0x7FF0_0000_0000_0000L,
            -0x0010_0000_0000_0000L,
            0x7FF8_0000_DEAD_BE77L,
            -0x0007_FFFF_2152_4189L,
        )

        bitPatterns.forEach { bits ->
            val value = JvmDoubleValue(java.lang.Double.longBitsToDouble(bits))

            assertEquals(bits, java.lang.Double.doubleToRawLongBits(value.value))
        }
    }
}
