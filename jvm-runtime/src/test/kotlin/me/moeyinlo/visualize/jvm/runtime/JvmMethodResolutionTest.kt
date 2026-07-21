package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmMethodResolutionTest {
    @Test
    fun `method resolution finds a static method declared directly by the referenced class`() {
        val method = JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = true)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    methods = listOf(method),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
                isStatic = true,
            ),
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `method resolution searches the superclass chain after the referenced class`() {
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
                    methods = listOf(JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = true)),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Grandparent",
                name = "answer",
                descriptor = "()I",
                isStatic = true,
            ),
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `method resolution throws guest NoSuchMethodError when lookup misses`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Example"),
            ),
        )

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "missing",
                descriptor = "()V",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Example.missing:()V", exception.message)
    }
}
