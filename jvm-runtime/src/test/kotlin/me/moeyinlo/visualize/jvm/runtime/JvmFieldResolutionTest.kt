package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmFieldResolutionTest {
    @Test
    fun `field resolution finds a field declared directly by the referenced class`() {
        val field = JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = true)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    fields = listOf(field),
                ),
            ),
        )

        assertEquals(
            JvmResolvedField(
                ownerClassName = "Example",
                name = "counter",
                descriptor = "I",
                isStatic = true,
            ),
            hierarchy.resolveField(
                ownerClassName = "Example",
                name = "counter",
                descriptor = "I",
            ),
        )
    }
}
