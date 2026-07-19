package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmStaticFieldsTest {
    @Test
    fun `static fields store values by owner name and descriptor`() {
        val fields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "counter",
            descriptor = "I",
        )

        fields.put(field, JvmIntValue(7))

        assertEquals(JvmIntValue(7), fields.get(field))
    }
}
