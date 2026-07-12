package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class JvmReferenceValueTest {
    @Test
    fun `null and non null references are category one values`() {
        assertEquals(JvmValueCategory.Category1, JvmNullValue.category)
        assertEquals(JvmValueCategory.Category1, JvmObjectReferenceValue(JvmReferenceId(1)).category)
    }

    @Test
    fun `non null references preserve opaque guest identity`() {
        val first = JvmObjectReferenceValue(JvmReferenceId(7))
        val same = JvmObjectReferenceValue(JvmReferenceId(7))
        val second = JvmObjectReferenceValue(JvmReferenceId(8))

        assertEquals(first, same)
        assertNotEquals(first, second)
    }

    @Test
    fun `reference ids are positive opaque guest ids`() {
        assertFailsWith<IllegalArgumentException> { JvmReferenceId(0) }
        assertFailsWith<IllegalArgumentException> { JvmReferenceId(-1) }
    }
}
