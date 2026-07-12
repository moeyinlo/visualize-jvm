package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmPrimitiveValueTest {
    @Test
    fun `primitive values expose their JVM slot categories`() {
        assertEquals(JvmValueCategory.Category1, JvmBooleanValue(true).category)
        assertEquals(JvmValueCategory.Category1, JvmByteValue(-128).category)
        assertEquals(JvmValueCategory.Category1, JvmCharValue(0xFFFF).category)
        assertEquals(JvmValueCategory.Category1, JvmShortValue(-32768).category)
        assertEquals(JvmValueCategory.Category1, JvmIntValue(-1).category)
        assertEquals(JvmValueCategory.Category1, JvmFloatValue(-0.0f).category)
        assertEquals(JvmValueCategory.Category2, JvmLongValue(1L).category)
        assertEquals(JvmValueCategory.Category2, JvmDoubleValue(1.0).category)
    }

    @Test
    fun `narrow primitive wrappers validate their JVMS ranges`() {
        assertFailsWith<IllegalArgumentException> { JvmByteValue(-129) }
        assertFailsWith<IllegalArgumentException> { JvmByteValue(128) }
        assertFailsWith<IllegalArgumentException> { JvmCharValue(-1) }
        assertFailsWith<IllegalArgumentException> { JvmCharValue(0x1_0000) }
        assertFailsWith<IllegalArgumentException> { JvmShortValue(-32769) }
        assertFailsWith<IllegalArgumentException> { JvmShortValue(32768) }
    }
}
