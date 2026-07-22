package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmIntegralValueTest {
    @Test
    fun `integral runtime values cover boolean byte char short int and long`() {
        val values = listOf(
            JvmBooleanValue(true),
            JvmByteValue(Byte.MIN_VALUE.toInt()),
            JvmByteValue(Byte.MAX_VALUE.toInt()),
            JvmCharValue(Char.MIN_VALUE.code),
            JvmCharValue(Char.MAX_VALUE.code),
            JvmShortValue(Short.MIN_VALUE.toInt()),
            JvmShortValue(Short.MAX_VALUE.toInt()),
            JvmIntValue(Int.MIN_VALUE),
            JvmIntValue(Int.MAX_VALUE),
            JvmLongValue(Long.MIN_VALUE),
            JvmLongValue(Long.MAX_VALUE),
        )

        values.dropLast(2).forEach { value ->
            assertEquals(JvmValueCategory.Category1, value.category)
        }
        values.takeLast(2).forEach { value ->
            assertEquals(JvmValueCategory.Category2, value.category)
        }
    }

    @Test
    fun `integral runtime values enforce JVMS narrowed type ranges`() {
        assertFailsWith<IllegalArgumentException> { JvmByteValue(Byte.MIN_VALUE - 1) }
        assertFailsWith<IllegalArgumentException> { JvmByteValue(Byte.MAX_VALUE + 1) }
        assertFailsWith<IllegalArgumentException> { JvmCharValue(Char.MIN_VALUE.code - 1) }
        assertFailsWith<IllegalArgumentException> { JvmCharValue(Char.MAX_VALUE.code + 1) }
        assertFailsWith<IllegalArgumentException> { JvmShortValue(Short.MIN_VALUE - 1) }
        assertFailsWith<IllegalArgumentException> { JvmShortValue(Short.MAX_VALUE + 1) }
    }
}
