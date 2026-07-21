package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmReturnAddressValueTest {
    @Test
    fun `returnAddress values are category one JVM values`() {
        assertEquals(JvmValueCategory.Category1, JvmReturnAddressValue(0).category)
        assertEquals(JvmValueCategory.Category1, JvmReturnAddressValue(Int.MAX_VALUE).category)
    }

    @Test
    fun `returnAddress values preserve non negative bytecode targets`() {
        assertEquals(123, JvmReturnAddressValue(123).address)
        assertFailsWith<IllegalArgumentException> { JvmReturnAddressValue(-1) }
    }
}
