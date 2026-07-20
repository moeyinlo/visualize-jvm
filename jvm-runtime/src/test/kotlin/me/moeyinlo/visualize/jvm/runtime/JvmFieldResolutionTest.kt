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

    @Test
    fun `field resolution finds a field declared by a direct superinterface`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    interfaceNames = listOf("ExampleInterface"),
                ),
                JvmClassDefinition(
                    internalName = "ExampleInterface",
                    fields = listOf(JvmFieldDefinition(name = "flag", descriptor = "Z", isStatic = true)),
                ),
            ),
        )

        assertEquals(
            JvmResolvedField(
                ownerClassName = "ExampleInterface",
                name = "flag",
                descriptor = "Z",
                isStatic = true,
            ),
            hierarchy.resolveField(
                ownerClassName = "Example",
                name = "flag",
                descriptor = "Z",
            ),
        )
    }
}
