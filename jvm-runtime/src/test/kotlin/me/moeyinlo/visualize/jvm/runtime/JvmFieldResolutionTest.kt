package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

    @Test
    fun `field resolution recursively searches indirect superinterfaces`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    interfaceNames = listOf("ChildInterface"),
                ),
                JvmClassDefinition(
                    internalName = "ChildInterface",
                    interfaceNames = listOf("ParentInterface"),
                ),
                JvmClassDefinition(
                    internalName = "ParentInterface",
                    fields = listOf(JvmFieldDefinition(name = "name", descriptor = "Ljava/lang/String;", isStatic = true)),
                ),
            ),
        )

        assertEquals(
            JvmResolvedField(
                ownerClassName = "ParentInterface",
                name = "name",
                descriptor = "Ljava/lang/String;",
                isStatic = true,
            ),
            hierarchy.resolveField(
                ownerClassName = "Example",
                name = "name",
                descriptor = "Ljava/lang/String;",
            ),
        )
    }

    @Test
    fun `field resolution searches the superclass chain after superinterfaces`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                ),
                JvmClassDefinition(
                    internalName = "Parent",
                    superclassName = "Grandparent",
                ),
                JvmClassDefinition(
                    internalName = "Grandparent",
                    fields = listOf(JvmFieldDefinition(name = "answer", descriptor = "I", isStatic = false)),
                ),
            ),
        )

        assertEquals(
            JvmResolvedField(
                ownerClassName = "Grandparent",
                name = "answer",
                descriptor = "I",
                isStatic = false,
            ),
            hierarchy.resolveField(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "I",
            ),
        )
    }

    @Test
    fun `field resolution throws guest NoSuchFieldError when lookup misses`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Example"),
            ),
        )

        val exception = assertFailsWith<JvmNoSuchFieldError> {
            hierarchy.resolveField(
                ownerClassName = "Example",
                name = "missing",
                descriptor = "I",
            )
        }

        assertEquals("java/lang/NoSuchFieldError", exception.guestClassName)
        assertEquals("Example.missing:I", exception.message)
    }
}
